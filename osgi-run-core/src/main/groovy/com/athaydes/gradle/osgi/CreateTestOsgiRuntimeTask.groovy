package com.athaydes.gradle.osgi

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CreateTestOsgiRuntimeTask extends DefaultTask {

    @TaskAction
    void run() {
        def config = project.extensions.getByName( 'runOsgi' ) as OsgiConfig

        // never include the osgi-run-protobuf-test-runner jar in the test runtime
        config.excludedBundles.add( 'osgi-run-protobuf-test-runner.*' )

        def target = getTarget( project, config )

        def createTask = project.tasks.getByName( 'createOsgiRuntime' ) as CreateOsgiRuntimeTask
        def osgiDir = createTask.outputDir

        // copy the standard OSGi environment to the test environment without changes
        project.copy {
            from osgiDir
            into project.file( target )
        }

        // now, copy the test resources to the test-bundles directory, wrapping non-bundles
        def testBundlesDir = "${target}/${config.bundlesPath}"

        CreateBundlesDir.copyJarsWrappingIfNeeded( project, config,
                project.configurations.osgiRunTest, testBundlesDir )

        project.tasks.withType( TestJarTask ) { testJarTask ->
            CreateBundlesDir.copyJarsWrappingIfNeeded( project, config, testJarTask, testBundlesDir )
        }
    }

    static String getTarget( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.outTestDir instanceof File ) ?
                osgiConfig.outTestDir.absolutePath :
                "${project.buildDir}/${osgiConfig.outTestDir}"
    }

}
