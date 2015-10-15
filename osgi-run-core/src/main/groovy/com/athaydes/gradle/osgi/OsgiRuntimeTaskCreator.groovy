package com.athaydes.gradle.osgi

import com.athaydes.gradle.osgi.bnd.BndWrapper
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.FileTreeElement
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 * Creates the osgiRun task
 */
class OsgiRuntimeTaskCreator {

    static final Logger log = Logging.getLogger( OsgiRuntimeTaskCreator )
    static final String OSGI_DEP_PREFIX = '__osgiRuntime'

    Closure createOsgiRuntimeTask( Project project, OsgiConfig osgiConfig ) {
        return {
            String target = getTarget( project, osgiConfig )
            osgiConfig.outDirFile = target as File
            log.info( "Will copy osgi runtime resources into $target" )
            configBundles( project, osgiConfig )
            copyBundles( project, "${target}/${osgiConfig.bundlesPath}",
                    osgiConfig.wrapInstructions as WrapInstructionsConfig )
            configMainDeps( project, osgiConfig )
            copyMainDeps( project, target )
            copyConfigFiles( target, osgiConfig )
            createOSScriptFiles( target, osgiConfig )
        }
    }

    private static void configMainDeps( Project project, OsgiConfig osgiConfig ) {
        def hasOsgiMainDeps = !project.configurations.osgiMain.dependencies.empty
        if ( !hasOsgiMainDeps ) {
            assert osgiConfig.osgiMain, 'No osgiMain provided, cannot create OSGi runtime'
            project.dependencies.add( 'osgiMain', osgiConfig.osgiMain )
        }
    }

    private void copyMainDeps( Project project, String target ) {
        project.copy {
            from project.configurations.osgiMain
            into target
        }
    }

    private void configBundles( Project project, OsgiConfig osgiConfig ) {
        def allBundles = osgiConfig.bundles.flatten() + project.configurations.osgiRuntime.allDependencies.asList()
        project.configurations { c ->
            // create individual configurations for each dependency so that version conflicts need not be resolved
            allBundles.size().times { int i -> c[ OSGI_DEP_PREFIX + i ] }
        }
        allBundles.eachWithIndex { Object bundle, int i ->

            // by default, all project dependencies are transitive
            boolean transitiveDep = bundle instanceof Project
            def exclusions = [ ] as Set
            if ( bundle instanceof ModuleDependency ) {
                transitiveDep = bundle.transitive
                exclusions = bundle.excludeRules
            }
            def depConfig = {
                transitive = transitiveDep
                exclusions.each { ExcludeRule rule ->
                    def excludeMap = [ : ]
                    if ( rule.group ) excludeMap.group = rule.group
                    if ( rule.module ) excludeMap.module = rule.module
                    exclude excludeMap
                }
            }
            project.dependencies.add( OSGI_DEP_PREFIX + i, bundle, depConfig )
        }
    }

    private void copyBundles( Project project, String bundlesDir,
                              WrapInstructionsConfig wrapInstructions ) {
        def nonBundles = [ ] as Set
        //noinspection GroovyAssignabilityCheck
        def allDeps = project.configurations.findAll { it.name.startsWith( OSGI_DEP_PREFIX ) }

        project.copy {
            from allDeps
            into bundlesDir
            exclude { FileTreeElement element ->
                def nonBundle = notBundle( element.file )
                if ( nonBundle ) nonBundles << element.file
                return nonBundle
            }
        }

        if ( wrapInstructions.enabled ) {
            nonBundles.each { File file ->
                try {
                    BndWrapper.wrapNonBundle( file, bundlesDir, wrapInstructions )
                } catch ( e ) {
                    log.warn( "Unable to wrap ${file.name}", e )
                }
            }
        } else if ( nonBundles ) {
            log.info "The following jars were kept out of the classpath " +
                    "as they are not bundles (enable wrapping if they are needed): {}", nonBundles
        }
    }

    private static boolean notBundle( File file ) {
        def zip = new ZipFile( file )
        try {
            ZipEntry entry = zip.getEntry( 'META-INF/MANIFEST.MF' )
            if ( !entry ) return true
            def lines = zip.getInputStream( entry ).readLines()
            return !lines.any { it.trim().startsWith( 'Bundle' ) }
        } finally {
            zip.close()
        }
    }


    private static boolean isFragment( file ) {
        def zip = new ZipFile( file as File )
        try {
            ZipEntry entry = zip.getEntry( 'META-INF/MANIFEST.MF' )
            if ( !entry ) return true
            def lines = zip.getInputStream( entry ).readLines()
            return lines.any { it.trim().startsWith( 'Fragment-Host' ) }
        } finally {
            zip.close()
        }
    }

    private void copyConfigFiles( String target, OsgiConfig osgiConfig ) {
        def configFile = getConfigFile( target, osgiConfig )
        if ( !configFile ) return;
        if ( !configFile.exists() ) {
            configFile.parentFile.mkdirs()
        }
        configFile.write( scapeSlashes( textForConfigFile( target, osgiConfig ) ), 'UTF-8' )
    }

    private File getConfigFile( String target, OsgiConfig osgiConfig ) {
        switch ( osgiConfig.configSettings ) {
            case 'felix': return new File( "${target}/conf/config.properties" )
            case 'equinox': return new File( "${target}/configuration/config.ini" )
            case 'none': return null
        }
        throw new GradleException( "Unknown OSGi configSettings: ${osgiConfig.configSettings}" )
    }

    private String getTarget( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.outDir instanceof File ) ?
                osgiConfig.outDir.absolutePath :
                "${project.buildDir}/${osgiConfig.outDir}"
    }

    private String scapeSlashes( String string ) {
        string.replace( '\\', '\\\\' )
    }

    private String textForConfigFile( String target, OsgiConfig osgiConfig ) {
        switch ( osgiConfig.configSettings ) {
            case 'felix': return generateFelixConfigFile( osgiConfig )
            case 'equinox': return generateEquinoxConfigFile( target, osgiConfig )
            default: throw new GradleException( 'Internal Plugin Error! Unknown configSettings. Please report bug at ' +
                    'https://github.com/renatoathaydes/osgi-run/issues\nInclude the following in your message:\n' +
                    osgiConfig )
        }
    }

    private String generateFelixConfigFile( OsgiConfig osgiConfig ) {
        map2properties osgiConfig.config
    }

    private String generateEquinoxConfigFile( String target, OsgiConfig osgiConfig ) {
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
        bundleJar.replace( target, '.' ) + ( isFragment( bundleJar ) ? '' : '@start' )
    }

    @SuppressWarnings( "GroovyAssignabilityCheck" )
    private String map2properties( Map map ) {
        map.inject( '' ) { acc, key, value ->
            "${acc}${key} = ${value}\n"
        }
    }

    private void createOSScriptFiles( String target, OsgiConfig osgiConfig ) {
        def jars = ( target as File ).listFiles()?.findAll { it.name.endsWith( 'jar' ) }
        assert jars, 'No main Jar found! Cannot create OSGi runtime.'

        def mainJar = jars.find { it.name.contains( 'main' ) } ?: jars.first()

        def linuxScript = """|#!/bin/sh
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
        |"\$JAVA" -jar ${mainJar.name} ${osgiConfig.javaArgs} "\$@"
        |""".stripMargin().replaceAll( Pattern.quote( '\r\n' ), '\n' )

        def windowsScript = """
        |@ECHO OFF
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
        |%JAVA% -jar ${mainJar} ${osgiConfig.javaArgs} %*
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
