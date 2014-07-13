package com.athaydes.gradle.osgi

import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

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
            copyConfigFiles( target )
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
        osgiConfig.bundles.flatten().each {
            project.dependencies.add( 'osgiRuntime', it )
        }
    }

    private void copyBundles( Project project, String bundlesDir ) {
        project.copy {
            from project.configurations.osgiRuntime
            into bundlesDir
        }
    }

    private void copyConfigFiles( String target ) {
        def configFile = new File( "${target}/conf/config.properties" )
        if ( !configFile.exists() ) {
            configFile.parentFile.mkdirs()
        }
        configFile << this.class.getResource( '/conf/config.properties' ).text
    }

    private String getTarget( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.outDir instanceof File ) ?
                osgiConfig.outDir.absolutePath :
                "${project.buildDir}/${osgiConfig.outDir}"
    }

}
