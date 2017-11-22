package com.athaydes.gradle.osgi

import com.athaydes.gradle.osgi.dependency.DefaultOSGiDependency
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.testing.Test

/**
 * A Gradle plugin that helps create and execute OSGi runtime environments.
 */
class OsgiRunPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( OsgiRunPlugin )
    static final WRAP_EXTENSION = 'wrapInstructions'

    @Override
    void apply( Project project ) {
        createConfigurations( project )

        addOsgiDependency( project )

        OsgiConfig osgiConfig = createExtensions( project )

        createTasks( project, osgiConfig )

        updateConfigurations( project, osgiConfig )
    }

    static void updateConfigurations( Project project, OsgiConfig osgiConfig ) {
        project.afterEvaluate {
            ConfigurationsCreator.configOsgiRuntimeBundles( project, osgiConfig )
            ConfigurationsCreator.configTestRunnerDependencies( project )

            String target = CreateOsgiRuntimeTask.getTarget( project, osgiConfig )
            osgiConfig.outDirFile = target as File

            configMainDeps( project, osgiConfig )
        }
    }

    static void createTasks( Project project, OsgiConfig osgiConfig ) {
        Task createBundlesDir = project.task(
                type: CreateBundlesDir,
                group: 'Build',
                description:
                        'Copies all configured OSGi bundles into the bundles directory',
                'createBundlesDir' )

        Task createOsgiRuntimeTask = project.task(
                type: CreateOsgiRuntimeTask,
                dependsOn: createBundlesDir,
                group: 'Build',
                description:
                        'Creates an OSGi environment which can then be started with generated scripts or with task runOsgi',
                'createOsgiRuntime' )

        createOsgiRuntimeTask.doLast { ManifestFileCopier.run( project, osgiConfig ) }

        project.task(
                type: RunOsgiTask,
                dependsOn: createOsgiRuntimeTask,
                group: 'Run',
                description:
                        'Runs the OSGi environment, installing and starting the configured bundles',
                'runOsgi' )

        Task cleanTask = project.task(
                type: Delete,
                group: 'Build',
                description: 'Cleans the OSGi environment created by the createOsgiRuntime task',
                'cleanOsgiRuntime' ) {
            // delay resolving the target to until after the project is resolved
            def target = {
                def output = CreateOsgiRuntimeTask.getTarget( project, osgiConfig )
                log.debug( "cleanOsgiRuntime will delete $output" )
                output
            }
            delete target
        }

        Task createTestJarTask = project.task(
                type: TestJarTask,
                group: 'Build',
                description: 'Creates a test Jar for this project',
                'createTestJarTask' )

        Task createTestRuntimeTask = project.task(
                type: CreateTestOsgiRuntimeTask,
                dependsOn: [ createTestJarTask, createOsgiRuntimeTask ],
                group: 'Build',
                description: 'Creates an OSGi environment within which to run OSGi tests',
                'createTestOsgiRuntime' )

        addTaskDependencies( project, createBundlesDir, cleanTask, createTestRuntimeTask, createTestJarTask )
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
        project.configurations.create( 'osgiTestRuntime' ) {
            def testRuntime = project.configurations.findByName( 'testRuntime' )
            if ( testRuntime ) {
                it.extendsFrom( testRuntime )
            }
        }
    }

    static void addTaskDependencies( Project project,
                                     Task createBundlesdir,
                                     Task cleanTask,
                                     Task createTestRuntimeTask,
                                     Task createTestJarTask ) {
        project.allprojects {
            it.tasks.withType( Jar ) { jarTask ->
                createBundlesdir.dependsOn jarTask
            }

            it.tasks.all { task ->
                if ( task.name.startsWith( 'compileTest' ) ) {
                    createTestJarTask.dependsOn task
                }
            }

            it.tasks.withType( Delete ) { delTask ->
                if ( delTask.name == 'clean' ) {
                    delTask.dependsOn cleanTask
                }
            }
        }
        project.tasks.withType( Test ) {
            it.dependsOn( createTestRuntimeTask )
        }
    }

    private static void configMainDeps( Project project, OsgiConfig osgiConfig ) {
        def hasOsgiMainDeps = !project.configurations.osgiMain.dependencies.empty
        if ( !hasOsgiMainDeps ) {
            assert osgiConfig.osgiMain, 'No osgiMain provided, cannot create OSGi runtime'
            project.dependencies.add( 'osgiMain', osgiConfig.osgiMain ) {
                transitive = false
            }
        }
    }

    private static void addOsgiDependency( Project project ) {
        project.dependencies.ext.osgi = { conf ->
            if ( conf instanceof Map || conf instanceof String ) {
                //noinspection GroovyAssignabilityCheck
                return new DefaultOSGiDependency( conf )
            } else {
                throw new IllegalArgumentException( "Invalid argument type to 'osgi': must be a String or Map, was ${conf?.class?.name}" )
            }
        }
    }

}
