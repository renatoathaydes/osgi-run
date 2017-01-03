package com.athaydes.gradle.osgi

import com.athaydes.gradle.osgi.bnd.BndWrapper
import com.athaydes.gradle.osgi.util.JarUtils
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.FileTreeElement
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.bundling.Jar

import java.util.jar.Manifest
import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static com.athaydes.gradle.osgi.OsgiRunPlugin.WRAP_EXTENSION

/**
 * Creates the osgiRun task
 */
class OsgiRuntimeTaskCreator extends DefaultTask {

    static final Logger log = Logging.getLogger( OsgiRuntimeTaskCreator )
    static final String SYSTEM_LIBS = 'system-libs'

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

        log.debug "Adding build file and jars of the following projects to the inputs of the createOsgiRuntime task: {}",
                allProjectDeps*.name

        Set<File> projectDependencies = [ ]

        ( allProjectDeps + project ).collectMany { dep ->
            dep.tasks.withType( Jar ) { Jar jar ->
                // we need to run the jar task if the build file changes
                if ( dep.buildFile ) projectDependencies += dep.buildFile
                // run our task if the jar of any dependency changes
                projectDependencies += jar.outputs.files
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

        log.info( "Will copy osgi runtime resources into $target" )
        copyBundles( project, osgiConfig, target )
        copySystemLibs( project, osgiConfig, target )
        copyMainDeps( project, target )
        copyConfigFiles( target, osgiConfig )
        osgiConfig.javaArgs = osgiConfig.javaArgs.replaceAll( /\r|\n/, ' ' )
        def mainClass = selectMainClass( project )
        createOSScriptFiles( target, osgiConfig, mainClass )
    }

    private void copyMainDeps( Project project, String target ) {
        project.copy {
            from project.configurations.osgiMain
            into "${target}/${SYSTEM_LIBS}"
        }
    }

    static String selectMainClass( Project project ) {
        String mainClass = null
        def mainJars = project.configurations.osgiMain.resolvedConfiguration.resolvedArtifacts
        for ( artifact in mainJars ) {
            mainClass = JarUtils.withManifestEntry( artifact.file ) { ZipFile file, ZipEntry manifestEntry ->
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

    private void copySystemLibs( Project project, OsgiConfig osgiConfig, String target ) {
        def systemLibsDir = "${target}/${SYSTEM_LIBS}"
        project.copy {
            from project.configurations.systemLib
            into systemLibsDir
        }
    }

    private void copyBundles( Project project, OsgiConfig osgiConfig, String target ) {
        def bundlesDir = "${target}/${osgiConfig.bundlesPath}"
        def wrapInstructions = osgiConfig[ WRAP_EXTENSION ] as WrapInstructionsConfig

        def nonBundles = [ ] as Set
        //noinspection GroovyAssignabilityCheck
        def allDeps = project.configurations.findAll { it.name.startsWith( ConfigurationsCreator.OSGI_DEP_PREFIX ) }

        def systemLibs = project.configurations.systemLib.resolvedConfiguration
                .resolvedArtifacts.collect { it.file.name } as Set

        project.copy {
            from allDeps
            into bundlesDir
            exclude { FileTreeElement element ->
                def inSystemLibs = element.file.name in systemLibs
                def explicityExcluded = osgiConfig.excludedBundles.any { element.name ==~ it }
                if ( inSystemLibs || explicityExcluded ) {
                    def reason = ( inSystemLibs && explicityExcluded ) ?
                            'both explicitly excluded and in system libs' : ( inSystemLibs ?
                            'in system libs' : 'explicitly excluded' )
                    log.info( 'Excluding bundle from bundles directory ({}): {}', reason, element.name )
                    return true
                }
                def nonBundle = JarUtils.notBundle( element.file )
                if ( nonBundle ) nonBundles << element.file
                return nonBundle
            }
        }

        if ( wrapInstructions.enabled ) {
            nonBundles.each { File file ->
                if ( JarUtils.hasManifest( file ) ) {
                    try {
                        BndWrapper.wrapNonBundle( file, bundlesDir, wrapInstructions )
                    } catch ( e ) {
                        log.warn( "Unable to wrap ${file.name}", e )
                    }
                } else {
                    log.warn( 'Jar without manifest found, unable to wrap it into a bundle: {}', file.name )
                }
            }
        } else if ( nonBundles ) {
            log.info "The following jars were kept out of the classpath " +
                    "as they are not bundles (enable wrapping if they are needed): {}", nonBundles
        }
    }

    private static void copyConfigFiles( String target, OsgiConfig osgiConfig ) {
        def configFile = getConfigFile( target, osgiConfig )
        if ( !configFile ) return
        if ( !configFile.exists() ) {
            configFile.parentFile.mkdirs()
        }
        configFile.write( scapeSlashes( textForConfigFile( target, osgiConfig ) ), 'UTF-8' )
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

    static String getTarget( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.outDir instanceof File ) ?
                osgiConfig.outDir.absolutePath :
                "${project.buildDir}/${osgiConfig.outDir}"
    }

    private static String scapeSlashes( String string ) {
        string.replace( '\\', '\\\\' )
    }

    private static String textForConfigFile( String target, OsgiConfig osgiConfig ) {
        switch ( osgiConfig.configSettings ) {
            case 'felix': return generateFelixConfigFile( osgiConfig )
            case 'equinox': return generateEquinoxConfigFile( target, osgiConfig )
            case 'knopflerfish': return generateKnopflerfishConfigFile( target, osgiConfig )
            default: throw new GradleException( 'Internal Plugin Error! Unknown configSettings. Please report bug at ' +
                    'https://github.com/renatoathaydes/osgi-run/issues\nInclude the following in your message:\n' +
                    osgiConfig )
        }
    }

    private static String generateFelixConfigFile( OsgiConfig osgiConfig ) {
        map2properties osgiConfig.config
    }

    private static String generateEquinoxConfigFile( String target, OsgiConfig osgiConfig ) {
        def bundlesDir = "${target}/${osgiConfig.bundlesPath}" as File
        if ( !bundlesDir.exists() ) {
            bundlesDir.mkdirs()
        }
        def bundleJars = new FileNameByRegexFinder().getFileNames(
                bundlesDir.absolutePath, /.+\.jar/ )
        map2properties( osgiConfig.config +
                [ 'osgi.bundles': bundleJars.collect { equinoxBundleDirective( it, target ) }.join( ',' ) ] )
    }

    private static String equinoxBundleDirective( String bundleJar, String target ) {
        bundleJar.replace( target, '..' ) + ( JarUtils.isFragment( bundleJar ) ? '' : '@start' )
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
