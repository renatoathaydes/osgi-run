package com.athaydes.gradle.osgi

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

class OsgiRunPluginTest {

    @Test
    void allTasksAdded() {
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.athaydes.osgi-run'
        println "Running the test"
        assert project.tasks.runOsgi
        assert project.tasks.createOsgiRuntime
    }

    @Test
    void allConfigurationsAdded() {
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.athaydes.osgi-run'
        assert project.configurations.findByName( 'osgiRuntime' )
        assert project.configurations.findByName( 'osgiMain' )
        assert project.configurations.findByName( 'systemLib' )
    }
}
