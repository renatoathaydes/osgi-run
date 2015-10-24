package com.athaydes.gradle.osgi.ds

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

/**
 * Plugin that adds OSGi Declarative Services support to a Project.
 */
class DeclarativeServicesPlugin implements Plugin<Project> {

    final dsJarBuilder = new DSJarBuilder()

    @Override
    void apply( Project project ) {
        def dsConfig = project.extensions.create( 'declarativeServices', DeclarativeServicesConfig )
        project.tasks.withType( Jar ) { jarTask ->
            jarTask.doLast {
                if ( jarTask.didWork ) {
                    jarTask.outputs.files.each { File output ->
                        dsJarBuilder.addDeclarativeServices( output, dsConfig )
                    }
                }
            }
        }
    }

}
