package com.athaydes.gradle.osgi

import com.athaydes.gradle.osgi.bnd.BndWrapper
import com.athaydes.gradle.osgi.util.JarUtils
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.file.FileTreeElement
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.bundling.Jar

import java.util.regex.Pattern

import static com.athaydes.gradle.osgi.OsgiRunPlugin.WRAP_EXTENSION

/**
 * Creates the osgiRun task
 */
class OsgiRuntimeTaskCreator {

    static final Logger log = Logging.getLogger( OsgiRuntimeTaskCreator )
    static final String OSGI_DEP_PREFIX = '__osgiRuntime'

    Closure createOsgiRuntimeTask( Project project, OsgiConfig osgiConfig, Task task ) {
        String target = getTarget( project, osgiConfig )
        setTaskInsAndOuts( project, task, target, osgiConfig )
        osgiConfig.outDirFile = target as File

        return {
            log.info( "Will copy osgi runtime resources into $target" )
            configBundles( project, osgiConfig )
            copyBundles( project, osgiConfig, target )
            configMainDeps( project, osgiConfig )
            copyMainDeps( project, target )
            copyConfigFiles( target, osgiConfig )
            osgiConfig.javaArgs = osgiConfig.javaArgs.replaceAll( /\r|\n/, ' ' )
            createOSScriptFiles( target, osgiConfig )
        }
    }

    private static setTaskInsAndOuts( Project project, Task task, String target, OsgiConfig osgiConfig ) {
        project.afterEvaluate {
            def allProjectDeps = allRuntimeDependencies( project, osgiConfig ).findAll { it instanceof Project }
            log.info "Adding build file of the following projects to the inputs of the Jar task: {}",
                    allProjectDeps*.name

            // inputs
            if ( project.buildFile ) task.inputs.file( project.buildFile )

            ( allProjectDeps + project ).each { dep ->
                dep.tasks.withType( Jar ) { Jar jar ->
                    // we need to run the jar task if the build file changes
                    if ( dep.buildFile ) jar.inputs.file( dep.buildFile )
                    // run our task if the jar of any dependency changes
                    task.inputs.files( jar.outputs.files )
                }
            }

            // outputs
            task.outputs.dir( target )
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

    private static List allRuntimeDependencies( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.bundles as List ).flatten() +
                project.configurations.osgiRuntime.allDependencies.asList()
    }

    private void configBundles( Project project, OsgiConfig osgiConfig ) {
        def allBundles = allRuntimeDependencies( project, osgiConfig )
        project.configurations { c ->
            // create individual configurations for each dependency so that version conflicts need not be resolved
            allBundles.size().times { int i -> c[ OSGI_DEP_PREFIX + i ] }
        }

        //noinspection GroovyAssignabilityCheck
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

    private void copyBundles( Project project, OsgiConfig osgiConfig, String target ) {
        def bundlesDir = "${target}/${osgiConfig.bundlesPath}"
        def wrapInstructions = osgiConfig[ WRAP_EXTENSION ] as WrapInstructionsConfig

        def nonBundles = [ ] as Set
        //noinspection GroovyAssignabilityCheck
        def allDeps = project.configurations.findAll { it.name.startsWith( OSGI_DEP_PREFIX ) }

        project.copy {
            from allDeps
            into bundlesDir
            exclude { FileTreeElement element ->
                def excluded = osgiConfig.excludedBundles.any { element.name ==~ it }
                if ( excluded ) {
                    log.info( 'Excluding bundle from runtime: {}', element.name )
                    return excluded
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
        if ( !configFile ) return;
        if ( !configFile.exists() ) {
            configFile.parentFile.mkdirs()
        }
        configFile.write( scapeSlashes( textForConfigFile( target, osgiConfig ) ), 'UTF-8' )
    }

    private static File getConfigFile( String target, OsgiConfig osgiConfig ) {
        switch ( osgiConfig.configSettings ) {
            case 'felix': return new File( "${target}/conf/config.properties" )
            case 'equinox': return new File( "${target}/configuration/config.ini" )
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
        bundleJar.replace( target, '.' ) + ( JarUtils.isFragment( bundleJar ) ? '' : '@start' )
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

    private static void createOSScriptFiles( String target, OsgiConfig osgiConfig ) {
        def jars = ( target as File ).listFiles()?.findAll { it.name.endsWith( 'jar' ) }
        assert jars, 'No main Jar found! Cannot create OSGi runtime.'

        def mainJar = jars.find { it.name.contains( 'main' ) } ?: jars.first()

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
        |"\$JAVA" ${osgiConfig.javaArgs} -jar ${mainJar.name} ${osgiConfig.programArgs} "\$@"
        |""".stripMargin().replaceAll( Pattern.quote( '\r\n' ), '\n' )

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
        |%JAVA% ${osgiConfig.javaArgs} -jar ${mainJar} ${osgiConfig.programArgs} %*
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
