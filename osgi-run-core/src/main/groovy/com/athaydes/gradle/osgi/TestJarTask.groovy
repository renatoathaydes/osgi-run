package com.athaydes.gradle.osgi

import org.gradle.api.GradleException
import org.gradle.api.plugins.JavaPlugin
import org.gradle.jvm.tasks.Jar

class TestJarTask extends Jar {

    boolean configured = false

    TestJarTask() {
        // only configure the jar task if the Java Plugin is configured
        project.plugins.withType( JavaPlugin ) {
            configured = true
            configure {
                from project.sourceSets.test.allSource
                classifier = "tests"
                extension = "jar"
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
    }
}
