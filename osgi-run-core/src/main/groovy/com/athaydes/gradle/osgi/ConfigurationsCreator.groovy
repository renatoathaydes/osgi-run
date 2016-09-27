package com.athaydes.gradle.osgi

import org.gradle.api.Project
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency

public class ConfigurationsCreator {

    static final String OSGI_DEP_PREFIX = '__osgiRuntime__'

    static List allRuntimeDependencies( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.bundles as List ).flatten() +
                project.configurations.osgiRuntime.allDependencies.asList()
    }

    static void configBundles( Project project, OsgiConfig osgiConfig ) {
        def allBundles = allRuntimeDependencies( project, osgiConfig )
        project.configurations { c ->
            // create individual configurations for each dependency so that version conflicts need not be resolved
            allBundles.size().times { int i -> //noinspection UnnecessaryQualifiedReference
                c.create( ConfigurationsCreator.OSGI_DEP_PREFIX + i )
            }
        }

        //noinspection GroovyAssignabilityCheck
        allBundles.eachWithIndex { Object bundle, int i ->

            // by default, all project dependencies are transitive
            boolean transitiveDep = bundle instanceof Project
            def exclusions = [ ] as Set
            if ( bundle instanceof ModuleDependency ) {
                transitiveDep = bundle.transitive
                exclusions = bundle.excludeRules
            }
            def depConfig = {
                transitive = transitiveDep
                exclusions.each { ExcludeRule rule ->
                    def excludeMap = [ : ]
                    if ( rule.group ) excludeMap.group = rule.group
                    if ( rule.module ) excludeMap.module = rule.module
                    exclude excludeMap
                }
            }
            project.dependencies.add( OSGI_DEP_PREFIX + i, bundle, depConfig )
        }
    }

}
