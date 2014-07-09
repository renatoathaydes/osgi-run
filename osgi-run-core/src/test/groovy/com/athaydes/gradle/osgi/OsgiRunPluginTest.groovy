package com.athaydes.gradle.osgi

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class OsgiRunPluginTest {

    @Test
    void allTasksAdded() {
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'osgi-run'
        println "Running the test"
        assert project.tasks.runOsgi
        assert project.tasks.createOsgiRuntime
    }
}
