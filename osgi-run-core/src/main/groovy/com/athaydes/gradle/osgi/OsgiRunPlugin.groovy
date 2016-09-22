package com.athaydes.gradle.osgi

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Jar

/**
 * A Gradle plugin that helps create and execute OSGi runtime environments.
 */
class OsgiRunPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( OsgiRunPlugin )
    static final WRAP_EXTENSION = 'wrapInstructions'

    def osgiRunner = new OsgiRunner()

    @Override
    void apply( Project project ) {
        createConfigurations( project )
        OsgiConfig osgiConfig = createExtensions( project )
        createTasks( project, osgiConfig )
    }

    def void createTasks( Project project, OsgiConfig osgiConfig ) {
        project.afterEvaluate { ConfigurationsCreator.configBundles( project, osgiConfig ) }

        Task createOsgiRuntimeTask = project.task(
                type: OsgiRuntimeTaskCreator,
                group: 'Build',
                description:
                        'Creates an OSGi environment which can then be started with generated scripts or with task runOsgi',
                'createOsgiRuntime' )

        createOsgiRuntimeTask.doLast { ManifestFileCopier.run( project, osgiConfig ) }

        project.task(
                dependsOn: createOsgiRuntimeTask,
                group: 'Run',
                description:
                        'Runs the OSGi environment, installing and starting the configured bundles',
                'runOsgi' ) <<
                runOsgiTask( project, osgiConfig )

        Task cleanTask = project.task(
                type: Delete,
                group: 'Build',
                description: 'Cleans the OSGi environment created by the createOsgiRuntime task',
                'cleanOsgiRuntime' ) {
            delete OsgiRuntimeTaskCreator.getTarget( project, osgiConfig )
        }

        addTaskDependencies( project, createOsgiRuntimeTask, cleanTask )
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
        project.configurations.create( 'systemLib' ) {
            def compile = project.configurations.findByName( 'compile' )
            if ( compile ) {
                compile.extendsFrom it
            }
        }
    }

    static void addTaskDependencies( Project project,
                                     Task createOsgiRuntimeTask,
                                     Task cleanTask ) {
        project.allprojects {
            it.tasks.withType( Jar ) { jarTask ->
                createOsgiRuntimeTask.dependsOn jarTask
            }
            it.tasks.withType( Delete ) { delTask ->
                if ( delTask.name == 'clean' ) {
                    delTask.dependsOn cleanTask
                }
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
