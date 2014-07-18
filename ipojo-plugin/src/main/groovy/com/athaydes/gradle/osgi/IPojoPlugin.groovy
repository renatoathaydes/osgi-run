package com.athaydes.gradle.osgi

import org.apache.felix.ipojo.manipulator.Pojoization
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * A Gradle plugin that adds IPojo metadata to OSGi bundles.
 */
class IPojoPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( IPojoPlugin )

    @Override
    void apply( Project project ) {
        project.apply( plugin: 'java' )
        project.apply( plugin: 'osgi' )
        IPojoConfig config = project.extensions.create( 'ipojo', IPojoConfig )
        project.tasks.jar.doLast ipojoTask( project, project.tasks.jar.archivePath, config )
    }

    def ipojoTask( Project project, jarPath, IPojoConfig config ) {
        return {
            def pojo = new Pojoization( new LoggingReporter( log ) )
            def srcBundle = jarPath as File
            assert srcBundle.isFile()
            File outBundle = resolveOutBundle( srcBundle, project, config )

            pojo.pojoization( srcBundle, outBundle, ( File ) null, project.class.classLoader )

            log.debug "IPojo output bundle: ${outBundle.absolutePath}"
            if ( !config.outDir ) {
                log.debug "Replacing OSGi bundle with IPojoized bundle at ${srcBundle.absolutePath}"
                srcBundle.newOutputStream() << outBundle.newInputStream()
            }
            log.info "IPojoization - SUCCESS"
        }
    }

    def File resolveOutBundle( File srcBundle, Project project, IPojoConfig config ) {
        def outBundle = new File( config.outDir ?: project.buildDir, srcBundle.name )
        if ( !config.outDir ) {
            outBundle.deleteOnExit()
        }
        if ( !outBundle.parentFile.isDirectory() ) {
            assert outBundle.parentFile.mkdirs()
        }
        assert outBundle.createNewFile()
        return outBundle
    }


}


class IPojoConfig {

    File outDir = null

}