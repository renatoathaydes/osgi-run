package com.athaydes.gradle.osgi

import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

import static com.athaydes.gradle.osgi.ConfigurationsCreator.OSGI_DEP_PREFIX

class OsgiRunPluginTest extends Specification {

    def allTasksAdded() {
        when:
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.athaydes.osgi-run'

        then:
        project.tasks.runOsgi
        project.tasks.createBundlesDir
        project.tasks.createOsgiRuntime
    }

    def allStaticConfigurationsAdded() {
        when:
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.athaydes.osgi-run'

        then:
        project.configurations.findByName( 'osgiRuntime' )
         project.configurations.findByName( 'osgiMain' )
         project.configurations.findByName( 'systemLib' )
    }

    def allDynamicConfigurationsAdded() {
        when:
        // using a default config
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'com.athaydes.osgi-run'

        def osgiConfig = project.extensions.getByName( 'runOsgi' ) as OsgiConfig

        and: 'run the config'
        ConfigurationsCreator.configBundles( project, osgiConfig )

        def defaultBundleCount = osgiConfig.bundles.size() as int

        then:
        defaultBundleCount > 0

        and: 'one config for each default dependency is created'
        defaultBundleCount.times { i ->
            assert project.configurations.findByName( OSGI_DEP_PREFIX + i )
        }

        and: 'no more configs are added'
        assert project.configurations.findByName( OSGI_DEP_PREFIX + ( defaultBundleCount + 1 ) ) == null
    }
}
