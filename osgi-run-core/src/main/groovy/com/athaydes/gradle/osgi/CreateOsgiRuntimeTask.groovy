package com.athaydes.gradle.osgi

import aQute.bnd.version.MavenVersion
import com.athaydes.gradle.osgi.dependency.DefaultOSGiDependency
import com.athaydes.gradle.osgi.util.JarUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

import java.nio.file.Files
import java.util.jar.Manifest
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * The createOsgiRuntime task.
 */
class CreateOsgiRuntimeTask extends DefaultTask {

    static final Logger log = Logging.getLogger( CreateOsgiRuntimeTask )
    static final String SYSTEM_LIBS = 'system-libs'
    static final Integer DEFAULT_START_LEVEL = 4

    @InputFile
    File getBuildFile() {
        log.debug( "Adding project build file to createOsgiRuntime task inputs: {}", project.buildFile )
        project.buildFile
    }

    @InputFiles
    Set<File> getAllFileInputsFromProjectDependencies() {
        def osgiConfig = project.extensions.getByName( 'runOsgi' ) as OsgiConfig

        def allProjectDeps = ConfigurationsCreator.allRuntimeDependencies( project, osgiConfig ).findAll {
            it instanceof Project
        } as List<Project>

        log.debug "Adding build file of the following projects to the inputs of the createOsgiRuntime task: {}",
                allProjectDeps*.name

        Set<File> projectDependencies = [ ]

        ( allProjectDeps + project ).collectMany { dep ->
            dep.tasks.withType( Jar ) { Jar jar ->
                // we need to run the jar task if the build file changes
                if ( dep.buildFile ) projectDependencies += dep.buildFile
            }
        }

        return projectDependencies
    }

    @OutputDirectory
    File getOutputDir() {
        def osgiConfig = project.extensions.getByName( 'runOsgi' ) as OsgiConfig
        project.file( getTarget( project, osgiConfig ) )
    }

    @TaskAction
    def createOsgiRuntime() {
        def osgiConfig = project.extensions.getByName( 'runOsgi' ) as OsgiConfig

        String target = getTarget( project, osgiConfig )
        def systemLibs = getSystemLibs( target )
        systemLibs.mkdirs()

        log.info( "Will copy osgi runtime resources into $target" )
        copySystemLibs( project, systemLibs )
        updateConfigWithSystemLibs( osgiConfig, systemLibs )
        copyMainDeps( project, systemLibs, osgiConfig )
        copyConfigFiles( target, osgiConfig, project )
        osgiConfig.javaArgs = osgiConfig.javaArgs.replaceAll( /[\r\n]/, ' ' )
        def mainClass = selectMainClass( project, systemLibs )
        createOSScriptFiles( target, osgiConfig, mainClass )
    }

    private void copyMainDeps( Project project, File systemLibs, OsgiConfig osgiConfig ) {
        if ( osgiConfig.osgiMain instanceof URI ) {
            URI uri = osgiConfig.osgiMain
            def fileName = new File( uri.path ).name
            Files.write( new File( systemLibs, fileName ).toPath(), uri.toURL().openStream().bytes )
        } else {
            project.copy {
                from project.configurations.osgiMain
                into systemLibs
            }
        }
    }

    static String selectMainClass( Project project, File systemLibs ) {
        String mainClass = null
        def mainJars = project.configurations.osgiMain.resolvedConfiguration.resolvedArtifacts*.file
        def systemJars = systemLibs.listFiles( { dir, name ->
            name.endsWith( '.jar' )
        } as FilenameFilter )?.toList() ?: [ ]
        for ( artifact in ( mainJars + systemJars ) ) {
            mainClass = JarUtils.withManifestEntry( artifact ) { ZipFile file, ZipEntry manifestEntry ->
                def manifest = new Manifest( file.getInputStream( manifestEntry ) )
                manifest.mainAttributes.getValue( 'Main-Class' )
            }
            if ( mainClass ) {
                break
            }
        }

        if ( !mainClass ) {
            throw new GradleException( "None of the osgiMain jars [${mainJars.collect { it.file }}] contain a " +
                    "Main-Class in its Manifest.\nPlease specify a runnable jar as a osgiMain dependency " +
                    "or the osgiMain property." )
        }

        return mainClass
    }

    private void copySystemLibs( Project project, File systemLibs ) {
        project.copy {
            from project.configurations.systemLib
            into systemLibs
        }
    }

    private static void updateConfigWithSystemLibs( OsgiConfig osgiConfig, File systemLibs ) {
        systemLibs.listFiles()?.findAll { it.name.endsWith( '.jar' ) }?.each { File jar ->
            Set packages = [ ]
            final version = MavenVersion.parseString( JarUtils.versionOf( new aQute.bnd.osgi.Jar( jar ) ) )
                    .getOSGiVersion()

            for ( entry in new ZipFile( jar ).entries() ) {

                if ( entry.name.endsWith( '.class' ) ) {
                    def lastSlashIndex = entry.toString().findLastIndexOf { it == '/' }
                    def entryName = lastSlashIndex > 0 ?
                            entry.toString().substring( 0, lastSlashIndex ) :
                            entry.toString()

                    packages << ( entryName.replace( '/', '.' ) + ';version=' + version )
                }
            }

            def extrasKey = 'org.osgi.framework.system.packages.extra'

            def extras = osgiConfig.config.get( extrasKey, '' )
            if ( extras && packages ) {
                extras = extras + ','
            }
            osgiConfig.config[ extrasKey ] = extras + packages.join( ',' )
        }

    }

    private static void copyConfigFiles( String target, OsgiConfig osgiConfig, Project project ) {
        def configFile = getConfigFile( target, osgiConfig )
        if ( !configFile ) return
        if ( !configFile.exists() ) {
            configFile.parentFile.mkdirs()
        }
        configFile.write( scapeSlashes( textForConfigFile( target, osgiConfig, project ) ), 'UTF-8' )
    }

    private static File getConfigFile( String target, OsgiConfig osgiConfig ) {
        switch ( osgiConfig.configSettings ) {
            case 'felix': return new File( "${target}/conf/config.properties" )
            case 'equinox': return new File( "${target}/$SYSTEM_LIBS/configuration/config.ini" )
            case 'knopflerfish': return new File( "${target}/init.xargs" )
            case 'none': return null
        }
        throw new GradleException( "Unknown OSGi configSettings: ${osgiConfig.configSettings}" )
    }

    static File getSystemLibs( String target ) {
        new File( "${target}/${SYSTEM_LIBS}" )
    }

    static String getTarget( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.outDir instanceof File ) ?
                osgiConfig.outDir.absolutePath :
                "${project.buildDir}/${osgiConfig.outDir}"
    }

    private static String scapeSlashes( String string ) {
        string.replace( '\\', '\\\\' )
    }

    private static String textForConfigFile( String target, OsgiConfig osgiConfig, Project project ) {
        switch ( osgiConfig.configSettings ) {
            case 'felix': return generateFelixConfigFile( target, osgiConfig, project )
            case 'equinox': return generateEquinoxConfigFile( target, osgiConfig, project )
            case 'knopflerfish': return generateKnopflerfishConfigFile( target, osgiConfig )
            default: throw new GradleException( 'Internal Plugin Error! Unknown configSettings. Please report bug at ' +
                    'https://github.com/renatoathaydes/osgi-run/issues\nInclude the following in your message:\n' +
                    osgiConfig )
        }
    }

    private static String generateFelixConfigFile( String target, OsgiConfig osgiConfig, Project project ) {
        def bundlesDir = "${target}/${osgiConfig.bundlesPath}" as File

        def bundleJars = bundlesDir.listFiles( { dir, name -> name ==~ /.+\.jar/ } as FilenameFilter )

        if ( !bundleJars ) {
            log.info( "Could not find any bundles in $target" )
            return map2properties( osgiConfig.config )
        }

        def startLevelMap = buildStartLevelMap( project )
        log.debug( "StartLevel map: {}", startLevelMap )

        if ( startLevelMap.values().every { it == null } ) {
            log.debug( "No StartLevels specified" )
            return map2properties( osgiConfig.config )
        }

        Map<Integer, List<File>> bundlesByStartLevel = [ : ]
        Map<Integer, List<File>> fragmentBundlesByStartLevel = [ : ]

        bundleJars.each { jar ->
            Map<Integer, List<File>> map
            if ( JarUtils.isFragment( jar ) ) {
                map = fragmentBundlesByStartLevel
            } else {
                map = bundlesByStartLevel
            }

            def startLevel = startLevelMap[ jar.name ] ?: DEFAULT_START_LEVEL
            map.merge( startLevel, [ jar ], { a, b -> a + b } )
        }

        def fragmentInstallEntries = fragmentBundlesByStartLevel.collectEntries(
                felixBundleDirective( 'felix.auto.install', target ) )

        def bundleStartEntries = bundlesByStartLevel.collectEntries(
                felixBundleDirective( 'felix.auto.start', target ) )

        map2properties( osgiConfig.config + fragmentInstallEntries + bundleStartEntries )
    }

    private static String generateEquinoxConfigFile( String target, OsgiConfig osgiConfig, Project project ) {
        def bundlesDir = "${target}/${osgiConfig.bundlesPath}" as File

        def bundleJars = bundlesDir.listFiles( { dir, name -> name ==~ /.+\.jar/ } as FilenameFilter )

        if ( !bundleJars ) {
            log.info( "Could not find any bundles in $target" )
            return map2properties( osgiConfig.config )
        }

        def startLevelMap = buildStartLevelMap( project )
        log.debug( "StartLevel map: {}", startLevelMap )

        def bundleStartEntries = [ 'osgi.bundles': bundleJars.collect { file ->
            def startLevel = startLevelMap[ file.name ]
            equinoxBundleDirective( file, target, startLevel )
        }.join( ',' ) ]

        map2properties( osgiConfig.config + bundleStartEntries )
    }

    private static Map<String, Integer> buildStartLevelMap( Project project ) {
        def osgiConfigs = project.configurations.findAll { it.name.startsWith( ConfigurationsCreator.OSGI_DEP_PREFIX ) }

        osgiConfigs.collectEntries { conf ->
            def files = conf.resolvedConfiguration.files
            conf.allDependencies.collectEntries { dep ->
                def startLevel = null
                if ( dep instanceof DefaultOSGiDependency ) {
                    startLevel = dep.startLevel
                }

                files.collectEntries { f -> [ ( f.name ): startLevel ] }
            }
        }
    }

    private static felixBundleDirective( String propPrefix, String target ) {
        target = "$target/"
        return { startLevel, jars ->
            String propKey = "$propPrefix.$startLevel"
            def url = { File bundleJar ->
                'file:' + bundleJar.absolutePath.replace( target, '' )
            }
            [ ( propKey ): jars.collect( url ).join( ' ' ) ]
        }
    }

    private static String equinoxBundleDirective( File bundleJar, String target, Integer startLevel ) {
        bundleJar.absolutePath.replace( target, '..' ) + (
                JarUtils.isFragment( bundleJar ) ? '' : (
                        startLevel == null ? '@start' : "@$startLevel:start"
                ) )
    }

    private static String generateKnopflerfishConfigFile( String target, OsgiConfig osgiConfig ) {
        def bundlesDir = "${target}/${osgiConfig.bundlesPath}" as File
        if ( !bundlesDir.exists() ) {
            bundlesDir.mkdirs()
        }
        def bundleJars = new FileNameByRegexFinder().getFileNames(
                bundlesDir.absolutePath, /.+\.jar/ )

        knopflerfishEntries( osgiConfig.config ) + knopflerfishBundleInstructions( bundleJars )
    }

    static String knopflerfishBundleInstructions( List<String> bundleJars ) {
        bundleJars.inject( '\n' ) { acc, bundle ->
            acc + ( JarUtils.isFragment( bundle ) ? "-install ${bundle}\n" : "-istart ${bundle}\n" )
        }
    }

    @SuppressWarnings( "GroovyAssignabilityCheck" )
    private static String map2properties( Map map ) {
        map.inject( '' ) { acc, key, value ->
            "${acc}${key} = ${value}\n"
        }
    }

    @SuppressWarnings( "GroovyAssignabilityCheck" )
    private static String knopflerfishEntries( Map map ) {
        map.inject( '' ) { acc, key, value ->
            def separator = key ==~ /\s*-[DF].*/ ? '=' : ''
            "${acc}${key} ${separator} ${value}\n"
        }
    }

    static String createJavaRunArgs( String target,
                                     OsgiConfig osgiConfig,
                                     String mainClass,
                                     String classpathSeparator ) {
        def systemLibs = ( "${target}/${SYSTEM_LIBS}" as File ).listFiles()?.findAll { it.name.endsWith( 'jar' ) }

        def classPath = {
            systemLibs ?
                    '-cp ' + systemLibs.collect { "${SYSTEM_LIBS}/${it.name}" }.join( classpathSeparator ) :
                    ''
        }
        "${osgiConfig.javaArgs} ${classPath()} ${mainClass} ${osgiConfig.programArgs}"
    }

    private static void createOSScriptFiles( String target, OsgiConfig osgiConfig, String mainClass ) {
        def linuxJavaArgs = createJavaRunArgs( target, osgiConfig, mainClass, ':' )

        def linuxScript = """|#!/bin/bash
        |
        |cd "\$( dirname "\${BASH_SOURCE[ 0 ]}" )"
        |
        |JAVA="java"
        |
        |# if JAVA_HOME exists, use it
        |if [ -x "\$JAVA_HOME/bin/java" ]
        |then
        |  JAVA="\$JAVA_HOME/bin/java"
        |else
        |  if [ -x "\$JAVA_HOME/jre/bin/java" ]
        |  then
        |    JAVA="\$JAVA_HOME/jre/bin/java"
        |  fi
        |fi
        |
        |"\$JAVA" ${linuxJavaArgs} "\$@"
        |""".stripMargin().replaceAll( Pattern.quote( '\r\n' ), '\n' )

        def windowsJavaArgs = createJavaRunArgs( target, osgiConfig, mainClass, ';' )

        def windowsScript = """
        |@ECHO OFF
        |
        |cd /d %~dp0
        |
        |set JAVA="java"
        |
        |REM if JAVA_HOME exists, use it
        |if exist "%JAVA_HOME%/bin/java" (
        |  set JAVA="%JAVA_HOME%/bin/java"
        |) else (
        |  if exist "%JAVA_HOME%/jre/bin/java" (
        |    set JAVA="%JAVA_HOME%/jre/bin/java"
        |  )
        |)
        |
        |%JAVA% ${windowsJavaArgs} %*
        |""".stripMargin().replaceAll( Pattern.quote( '\n' ), '\r\n' )

        def writeToExecutable = { String fileName, String scriptText ->
            def file = new File( "$target/$fileName" )
            def ok = file.setExecutable( true )
            if ( !ok ) log.warn( "No permission to make file $file executable. Please do it manually." )
            file.write( scriptText, 'utf-8' )
        }

        writeToExecutable( "run.sh", linuxScript )
        writeToExecutable( "run.bat", windowsScript )
    }

}
