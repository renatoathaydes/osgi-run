package com.athaydes.gradle.osgi

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.regex.Pattern
import java.util.zip.ZipEntry
import java.util.zip.ZipFile

import static java.util.Collections.emptyList

/**
 *
 */
class OsgiRuntimeTaskCreator {

    static final Logger log = Logging.getLogger( OsgiRuntimeTaskCreator )

    Closure createOsgiRuntimeTask( Project project, OsgiConfig osgiConfig ) {
        return {
            String target = getTarget( project, osgiConfig )
            osgiConfig.outDirFile = target as File
            log.info( "Will copy osgi runtime resources into $target" )
            configBundles( project, osgiConfig )
            copyBundles( project, "${target}/${osgiConfig.bundlesPath}" )
            configMainDeps( project, osgiConfig )
            copyMainDeps( project, target )
            copyConfigFiles( target, osgiConfig )
            createOSScriptFiles( target )
        }
    }

    private void configMainDeps( Project project, OsgiConfig osgiConfig ) {
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
            allBundles.size().times { i -> c."__osgiRuntime$i" }
        }
        allBundles.eachWithIndex { Object bundle, i ->
            def depConfig = ( bundle instanceof Dependency ) ? {} : { transitive = bundle instanceof Project }
            project.dependencies.add( "__osgiRuntime$i", bundle, depConfig )
        }
    }

    private void copyBundles( Project project, String bundlesDir ) {
        project.copy {
            from project.configurations.findAll { it.name.startsWith( '__osgiRuntime' ) }
            into bundlesDir
        }
        nonBundles( new File( bundlesDir ).listFiles() ).each {
            log.info "Removing non-bundle from classpath: ${it.name}"
            assert it.delete()
        }
    }

    private Collection<File> nonBundles( File[] files ) {
        if ( !files ) return emptyList()
        def notBundle = { File file ->
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
        files.findAll( notBundle )
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
                [ 'osgi.bundles': bundleJars.collect { it + '@start' }.join( ',' ) ] )
    }

    private String map2properties( Map map ) {
        map.inject( '' ) { acc, key, value ->
            "${acc}${key} = ${value}\n"
        }
    }

    private void createOSScriptFiles( String target ) {
        def jars = ( target as File ).listFiles()?.findAll { it.name.endsWith( 'jar' ) }
        assert jars, 'No main Jar found! Cannot create OSGi runtime.'

        def mainJar = jars.find { it.name.contains( 'main' ) } ?: jars.first()

        def linuxScript = """
        |#!/bin/sh
        |
        |JAVA="java"
        |
        |# if JAVA_HOME exists, use it
        |if [ "x\$JAVA_HOME" = "x" ]
        |then
        |  JAVA="\$JAVA_HOME/bin/java"
        |fi
        |
        |"\$JAVA" -jar ${mainJar} "\$@"
        |""".stripMargin().replaceAll( Pattern.quote( '\r\n' ), '\n' )

        def windowsScript = """
        |set JAVA="java"
        |
        |# if JAVA_HOME exists, use it
        |if exists "%JAVA_HOME%" (
        |  set JAVA="%JAVA_HOME%/bin/java"
        |)
        |
        |"%JAVA%" -jar ${mainJar} %*
        |""".stripMargin().replaceAll( Pattern.quote( '\n' ), '\r\n' )

        def writeToExecutable = { String fileName, String scriptText ->
            def file = new File( "$target/$fileName" )
            def ok = file.setExecutable( true )
            if ( !ok ) log.warn( "No permission to make file $file executable. Please do it manually." )
            file.write( scriptText, 'utf-8' )
        }

        writeToExecutable( "run.sh", linuxScript )
        writeToExecutable( "run.command", linuxScript )
        writeToExecutable( "run.bat", windowsScript )
    }

}
