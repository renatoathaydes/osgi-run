package com.athaydes.gradle.osgi

import com.athaydes.gradle.osgi.dependency.DefaultOSGiDependency
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import static com.athaydes.gradle.osgi.OsgiRunConstants.GROUP
import static com.athaydes.gradle.osgi.OsgiRunConstants.TEST_RUNNER_VERSION

class ConfigurationsCreator {

    static final Logger log = Logging.getLogger( ConfigurationsCreator )

    static final String OSGI_DEP_PREFIX = '__osgiRuntime__'

    static List allRuntimeDependencies( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.bundles as List ).flatten() +
                project.configurations.osgiRuntime.allDependencies.asList()
    }

    static void configTestRunnerDependencies( Project project ) {
        if ( needsOsgiTestRuntime( project ) ) {
            log.debug( "osgi-run TestRunner detected, setting up osgi-test-runner runtime dependencies" )

            def osgiRunProtobufTestRunner = "${GROUP}:osgi-run-protobuf-test-runner:${TEST_RUNNER_VERSION}"
            project.dependencies.add( 'testRuntime', osgiRunProtobufTestRunner )

            def osgiRunTestRunnerBundle = "${GROUP}:osgi-run-test-runner-bundle:${TEST_RUNNER_VERSION}"
            project.dependencies.add( 'osgiTestRuntime', osgiRunTestRunnerBundle )
        } else {
            log.debug( "No osgi-run TestRunner was detected" )
        }
    }

    static void configOsgiRuntimeBundles( Project project, OsgiConfig osgiConfig ) {
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
            ( bundle, depConfig ) = configureDependency( bundle, transitiveDep, exclusions )

            project.dependencies.add( OSGI_DEP_PREFIX + i, bundle, depConfig )
        }

    }

    private static List configureDependency( bundle, boolean transitiveDep, exclusions ) {
        Closure depConfig

        switch ( bundle ) {
            case Dependency:
            case String:
                depConfig = {
                    transitive = transitiveDep
                    exclusions.each { ExcludeRule rule ->
                        def excludeMap = [ : ]
                        if ( rule.group ) excludeMap.group = rule.group
                        if ( rule.module ) excludeMap.module = rule.module
                        exclude excludeMap
                    }
                }
                break
            case Map:
                assert bundle instanceof Map
                if ( !bundle.containsKey( 'dependency' ) ) {
                    throw new GradleException( "Bundle declaration does not contain 'dependency': $bundle" )
                }
                if ( bundle.containsKey( 'transitive' ) ) {
                    transitiveDep = bundle[ 'transitive' ]
                }

                if ( bundle.containsKey( 'exclusions' ) ) {
                    exclusions = bundle.exclusions
                }
                depConfig = {
                    transitive = transitiveDep
                    exclusions.each { ex ->
                        exclude ex
                    }
                }
                if ( bundle.dependency instanceof String || bundle.dependency instanceof Map ) {
                    def startLevel = bundle[ 'startLevel' ]
                    //noinspection GroovyAssignabilityCheck
                    bundle = new DefaultOSGiDependency( bundle.dependency )
                    if ( startLevel ) {
                        if ( startLevel instanceof Integer || startLevel.toString().isInteger() ) {
                            bundle.startLevel = startLevel as int
                        } else {
                            throw new GradleException( "startLevel has invalid type or format (should be integer): $startLevel" )
                        }
                    }
                } else {
                    bundle = bundle.dependency
                }

                break
            default:
                depConfig = { -> }
        }

        [ bundle, depConfig ]
    }

    static boolean needsOsgiTestRuntime( Project project ) {
        def testCompileConfig = project.configurations.findByName( 'testCompile' )

        testCompileConfig && testCompileConfig.files*.name.any {
            it ==~ /osgi-run-test-runner-api-.*\.jar/
        }
    }

}