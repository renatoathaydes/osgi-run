package com.athaydes.gradle.osgi

import com.athaydes.gradle.osgi.util.JarUtils
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Jar

import java.util.zip.ZipFile

/**
 * A Gradle plugin that helps create and execute OSGi runtime environments.
 */
class OsgiRunPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( OsgiRunPlugin )
    static final WRAP_EXTENSION = 'wrapInstructions'

    @Override
    void apply( Project project ) {
        createConfigurations( project )

        OsgiConfig osgiConfig = createExtensions( project )

        String target = OsgiRuntimeTaskCreator.getTarget( project, osgiConfig )
        osgiConfig.outDirFile = target as File

        updateConfigWithSystemLibs( project, osgiConfig, target )
        configMainDeps( project, osgiConfig )

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
                type: OsgiRunner,
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
                def output = OsgiRuntimeTaskCreator.getTarget( project, osgiConfig )
                log.debug( "cleanOsgiRuntime will delete $output" )
                output
            }
            delete target
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

    private static void configMainDeps( Project project, OsgiConfig osgiConfig ) {
        def hasOsgiMainDeps = !project.configurations.osgiMain.dependencies.empty
        if ( !hasOsgiMainDeps ) {
            assert osgiConfig.osgiMain, 'No osgiMain provided, cannot create OSGi runtime'
            project.dependencies.add( 'osgiMain', osgiConfig.osgiMain ) {
                transitive = false
            }
        }
    }

    private static void updateConfigWithSystemLibs( Project project, OsgiConfig osgiConfig, String target ) {
        def systemLibsDir = project.file "${target}/${OsgiRuntimeTaskCreator.SYSTEM_LIBS}"

        systemLibsDir.listFiles()?.findAll { it.name.endsWith( '.jar' ) }?.each { File jar ->
            Set packages = [ ]
            final version = JarUtils.versionOf( new aQute.bnd.osgi.Jar( jar ) )

            for ( entry in new ZipFile( jar ).entries() ) {

                if ( entry.name.endsWith( '.class' ) ) {
                    def lastSlashIndex = entry.toString().findLastIndexOf { it == '/' }
                    def entryName = lastSlashIndex > 0 ?
                            entry.toString().substring( 0, lastSlashIndex ) :
                            entry.toString()

                    packages << ( entryName.replace( '/', '.' ) + ';version=' + version )
                }
            }

            def extrasKey = 'org.osgi.framework.system.packages.extra'

            def extras = osgiConfig.config.get( extrasKey, '' )
            if ( extras && packages ) {
                extras = extras + ','
            }
            osgiConfig.config[ extrasKey ] = extras + packages.join( ',' )
        }

    }


}
