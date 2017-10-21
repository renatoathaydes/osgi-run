package com.athaydes.gradle.osgi

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

class ConfigurationsCreator {

    static final Logger log = Logging.getLogger( ConfigurationsCreator )

    static final String OSGI_DEP_PREFIX = '__osgiRuntime__'

    static List allRuntimeDependencies( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.bundles as List ).flatten() +
                project.configurations.osgiRuntime.allDependencies.asList()
    }

    static void configBundles( Project project, OsgiConfig osgiConfig ) {
        def allBundles = allRuntimeDependencies( project, osgiConfig )

        log.debug( "Creating individual configurations for each OSGi runtime dependency:\n{}", allBundles )

        project.configurations { c ->
            // create individual configurations for each dependency so that version conflicts need not be resolved
            allBundles.size().times { int i -> //noinspection UnnecessaryQualifiedReference
                c.create( ConfigurationsCreator.OSGI_DEP_PREFIX + i )
            }
        }

        //noinspection GroovyAssignabilityCheck
        allBundles.eachWithIndex { Object bundle, int i ->

            // by default, all dependencies are transitive
            boolean transitiveDep = true
            def exclusions = [ ] as Set
            if ( bundle instanceof ModuleDependency ) {
                transitiveDep = bundle.transitive
                exclusions = bundle.excludeRules
            }

            Closure depConfig

            if ( bundle instanceof Dependency || bundle instanceof String ) {
                depConfig = {
                    transitive = transitiveDep
                    exclusions.each { ExcludeRule rule ->
                        def excludeMap = [ : ]
                        if ( rule.group ) excludeMap.group = rule.group
                        if ( rule.module ) excludeMap.module = rule.module
                        exclude excludeMap
                    }
                }

            } else {
                depConfig = { -> }
            }

            project.dependencies.add( OSGI_DEP_PREFIX + i, bundle, depConfig )
        }

    }

}