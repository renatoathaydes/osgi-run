package com.athaydes.gradle.osgi

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.bundling.Jar

/**
 * A Gradle plugin that helps create and execute OSGi runtime environments.
 */
class OsgiRunPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( OsgiRunPlugin )
    final osgiRunner = new OsgiRunner()

    @Override
    void apply( Project project ) {
        project.apply( plugin: 'osgi' )
        project.configurations.create( 'osgiRuntime' )
        project.configurations.create( 'osgiMain' )
        OsgiConfig osgiConfig = project.extensions.create( 'runOsgi', OsgiConfig )
        Task createOsgiRuntimeTask= project.task( 'createOsgiRuntime' ) << createOsgiRuntimeTask( project, osgiConfig )
        project.task( dependsOn: 'createOsgiRuntime', 'runOsgi' ) << runOsgiTask( project, osgiConfig )
        addTaskDependencies( project, createOsgiRuntimeTask )
    }

    private Closure<File> createOsgiRuntimeTask( Project project, OsgiConfig osgiConfig ) {
        return {
            String target = ( osgiConfig.outDir instanceof File ) ?
                    osgiConfig.outDir.absolutePath :
                    "${project.buildDir}/${osgiConfig.outDir}"
            log.info( "Will copy osgi runtime resources into $target" )
            project.copy {
                from asCopySources( osgiConfig.bundles )
                from project.configurations.osgiRuntime
                into target + "/bundle"
            }
            project.copy {
                from project.configurations.osgiMain
                into target
            }
            def configFile = new File( "${target}/conf/config.properties" )
            if ( !configFile.exists() ) {
                configFile.parentFile.mkdirs()
            }
            configFile << this.class.getResource( '/conf/config.properties' ).text
            osgiConfig.outDirFile = target as File
        }
    }

    void addTaskDependencies( Project project, createOsgiRuntimeTask ) {
        project.allprojects {
            it.tasks.withType( Jar ) { jarTask ->
                createOsgiRuntimeTask.dependsOn jarTask
            }
        }
    }

    private Closure runOsgiTask( Project project, OsgiConfig osgiConfig ) {
        return {
            osgiRunner.run( project, osgiConfig )
        }
    }

    def asCopySources( resources ) {
        resources.collect { resource ->
            println "As CopySource: $resource"
            switch ( resource ) {
                case Project:
                    Project p = resource
                    return p.configurations.archives.artifacts.files.files.asList() + p.configurations.runtime
                default:
                    return resource
            }
        }
    }

}

class OsgiConfig {
    protected File outDirFile
    def outDir = "osgi"
    def bundles = [ ]
}
