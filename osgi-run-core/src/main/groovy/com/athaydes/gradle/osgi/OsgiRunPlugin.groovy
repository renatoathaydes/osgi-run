package com.athaydes.gradle.osgi

import groovy.transform.ToString
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
        Task createOsgiRuntimeTask = project.task( 'createOsgiRuntime' ) <<
                runtimeCreator.createOsgiRuntimeTask( project, osgiConfig )
        project.task( dependsOn: 'createOsgiRuntime', 'runOsgi' ) <<
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

@ToString( includeFields = true )
class OsgiConfig {
    protected File outDirFile
    def outDir = "osgi"
    def bundles = [ ] + FELIX_GOGO_BUNDLES
    def osgiMain = FELIX
    String javaArgs = ""
    String bundlesPath = "bundle"

    static final String FELIX = 'org.apache.felix:org.apache.felix.main:4.4.0'

    static final String EQUINOX = 'org.eclipse.osgi:org.eclipse.osgi:3.7.1'

    static final FELIX_GOGO_BUNDLES = [
            'org.apache.felix:org.apache.felix.gogo.runtime:0.12.1',
            'org.apache.felix:org.apache.felix.gogo.shell:0.10.0',
            'org.apache.felix:org.apache.felix.gogo.command:0.14.0',
    ].asImmutable()

}
