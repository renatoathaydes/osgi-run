package com.athaydes.gradle.osgi

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import static com.athaydes.gradle.osgi.ConfigurationsCreator.OSGI_DEP_PREFIX

class OsgiRunPluginTest {

    @Test
    void allTasksAdded() {
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.athaydes.osgi-run'
        println "Running the test"
        assert project.tasks.runOsgi
        assert project.tasks.createBundlesDir
        assert project.tasks.createOsgiRuntime
    }

    @Test
    void allStaticConfigurationsAdded() {
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.athaydes.osgi-run'
        assert project.configurations.findByName( 'osgiRuntime' )
        assert project.configurations.findByName( 'osgiMain' )
        assert project.configurations.findByName( 'systemLib' )
    }

    @Test
    void allDynamicConfigurationsAdded() {
        // using a default config
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.athaydes.osgi-run'

        def osgiConfig = project.extensions.getByName( 'runOsgi' ) as OsgiConfig

        // run the config
        ConfigurationsCreator.configOsgiRuntimeBundles( project, osgiConfig )

        def defaultBundleCount = osgiConfig.bundles.size() as int

        assert defaultBundleCount > 0

        // one config for each default dependency is created
        defaultBundleCount.times { i ->
            assert project.configurations.findByName( OSGI_DEP_PREFIX + i )
        }

        // no more configs are added
        assert project.configurations.findByName( OSGI_DEP_PREFIX + ( defaultBundleCount + 1 ) ) == null
    }
}
