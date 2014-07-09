package com.athaydes.gradle.osgi

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * A Gradle plugin that helps create and execute OSGi runtime environments.
 */
class OsgiRunPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( OsgiRunPlugin )
    final osgiRunner = new OsgiRunner()

    @Override
    void apply( Project project ) {
        project.apply( plugin: 'osgi' )
        project.task( dependsOn: 'jar', 'createOsgiRuntime' ) << {
            OsgiConfig osgi = project.extensions.getByName( 'runOsgi' )
            String target = ( osgi.outDir instanceof File ) ? osgi.outDir.absolutePath : "${project.buildDir}/${osgi.outDir}"
            log.info( "Will copy osgi runtime resources into $target" )
            project.copy {
                from asCopySources( osgi.bundles )
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
            osgi.outDirFile = target as File
        }
        project.task( dependsOn: 'createOsgiRuntime', 'runOsgi' ) << {
            OsgiConfig osgi = project.extensions.getByName( 'runOsgi' )
            osgiRunner.run( project, osgi )
        }
        project.configurations.create( 'osgiRuntime' )
        project.configurations.create( 'osgiMain' )
        project.extensions.create( 'runOsgi', OsgiConfig )
    }

    def asCopySources( resources ) {
        resources.collect { resource ->
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
    String outDir = "osgi"
    def bundles = [ ]
}
