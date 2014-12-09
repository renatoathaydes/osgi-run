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
    def osgiRunner = new OsgiRunner()
    def runtimeCreator = new OsgiRuntimeTaskCreator()

    @Override
    void apply( Project project ) {
        project.apply( plugin: 'osgi' )
        createConfigurations( project )
        OsgiConfig osgiConfig = createExtensions( project )
        createTasks( project, osgiConfig )
    }

    def void createTasks( Project project, OsgiConfig osgiConfig ) {
        Task createOsgiRuntimeTask = project.task(
                group: 'Build',
                description:
                'Creates an OSGi environment which can then be started manually or with task runOsgi',
                'createOsgiRuntime' ) <<
                runtimeCreator.createOsgiRuntimeTask( project, osgiConfig )
        project.task(
                dependsOn: 'createOsgiRuntime',
                group: 'Run',
                description:
                'Runs the OSGi environment, installing and starting the configured bundles',
                'runOsgi' ) <<
                runOsgiTask( project, osgiConfig )
        addTaskDependencies( project, createOsgiRuntimeTask )
    }

    def OsgiConfig createExtensions( Project project ) {
        project.extensions.create( 'runOsgi', OsgiConfig )
    }

    void createConfigurations( Project project ) {
        project.configurations.create( 'osgiRuntime' )
        project.configurations.create( 'osgiMain' )
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

}
