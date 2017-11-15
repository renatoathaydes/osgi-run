package com.athaydes.gradle.osgi

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CreateTestOsgiRuntimeTask extends DefaultTask {

    @TaskAction
    void run() {
        def config = project.extensions.getByName( 'runOsgi' ) as OsgiConfig

        def target = getTarget( project, config )

        def createTask = project.tasks.getByName( 'createOsgiRuntime' ) as CreateOsgiRuntimeTask
        def osgiDir = createTask.outputDir

        project.copy {
            from osgiDir
            into project.file( target )
        }
        project.copy {
            from project.configurations.osgiRunTest
            into project.file("${target}/${config.bundlesPath}")
        }
    }

    static String getTarget( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.outTestDir instanceof File ) ?
                osgiConfig.outTestDir.absolutePath :
                "${project.buildDir}/${osgiConfig.outTestDir}"
    }

}
