package com.athaydes.gradle.osgi

import com.athaydes.gradle.osgi.bnd.BndWrapper
import org.gradle.api.GradleException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.jvm.tasks.Jar

import static com.athaydes.gradle.osgi.OsgiRunPlugin.WRAP_EXTENSION

class TestJarTask extends Jar {

    boolean configured = false

    TestJarTask() {
        // only configure the jar task if the Java Plugin is configured
        project.plugins.withType( JavaPlugin ) {
            configured = true
            configure {
                from project.sourceSets.test.output
                classifier = "tests"
                extension = "jar"
            }
            manifest {
                it.attributes(
                        'Bundle-SymbolicName': project.name + '-tests',
                        'Bundle-Name': project.name + '-tests',
                        'Bundle-Description': project.description + ' (Tests)',
                )
            }
        }
    }

    @Override
    protected void copy() {
        if ( !configured ) {
            throw new GradleException( 'Cannot run the createTestJarTask because the Java plugin was not applied ' +
                    'on the project! Please apply the Java plugin or other language plugin.' )
        }
        super.copy()

        wrapJarIntoBundle()
    }

    private void wrapJarIntoBundle() {
        def osgiConfig = project.extensions.getByName( 'runOsgi' ) as OsgiConfig
        def wrapInstructions = osgiConfig[ WRAP_EXTENSION ] as WrapInstructionsConfig

        outputs.files.each { jar ->
            def jarFile = jar as File
            if ( jarFile.isFile() ) {
                BndWrapper.wrapNonBundle( jarFile, jarFile.parentFile.absolutePath, wrapInstructions )
            }
        }
    }


}
