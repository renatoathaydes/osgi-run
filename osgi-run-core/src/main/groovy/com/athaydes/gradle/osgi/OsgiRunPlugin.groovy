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
    static final WRAP_EXTENSION = 'wrapInstructions'

    def osgiRunner = new OsgiRunner()
    def runtimeCreator = new OsgiRuntimeTaskCreator()

    @Override
    void apply( Project project ) {
        createConfigurations( project )
        OsgiConfig osgiConfig = createExtensions( project )
        createTasks( project, osgiConfig )
    }

    def void createTasks( Project project, OsgiConfig osgiConfig ) {
        Task createOsgiRuntimeTask = project.task(
                group: 'Build',
                description:
                        'Creates an OSGi environment which can then be started with generated scripts or with task runOsgi',
                'createOsgiRuntime' )
        createOsgiRuntimeTask <<
                runtimeCreator.createOsgiRuntimeTask( project, osgiConfig, createOsgiRuntimeTask )

        createOsgiRuntimeTask.doLast { ManifestFileCopier.run( project, osgiConfig ) }

        project.task(
                dependsOn: createOsgiRuntimeTask,
                group: 'Run',
                description:
                        'Runs the OSGi environment, installing and starting the configured bundles',
                'runOsgi' ) <<
                runOsgiTask( project, osgiConfig )

        addTaskDependencies( project, createOsgiRuntimeTask )
    }

    static OsgiConfig createExtensions( Project project ) {
        def osgiConfig = project.extensions.create( 'runOsgi', OsgiConfig )
        osgiConfig.extensions.create(
                WRAP_EXTENSION, WrapInstructionsConfig )
        return osgiConfig
    }

    static void createConfigurations( Project project ) {
        project.configurations.create( 'osgiRuntime' )
        project.configurations.create( 'osgiMain' )
    }

    static void addTaskDependencies( Project project,
                                     Task createOsgiRuntimeTask ) {
        project.allprojects {
            it.tasks.withType( Jar ) { jarTask ->
                createOsgiRuntimeTask.dependsOn jarTask
            }
        }
    }

    private Closure runOsgiTask( Project project, OsgiConfig osgiConfig ) {
        return {
            log.info( "Jar wrap instructions: {}", osgiConfig[ WRAP_EXTENSION ] )
            osgiRunner.run( project, osgiConfig )
        }
    }

}
