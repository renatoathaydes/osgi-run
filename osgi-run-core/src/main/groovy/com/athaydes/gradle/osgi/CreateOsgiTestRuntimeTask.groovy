package com.athaydes.gradle.osgi

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class CreateOsgiTestRuntimeTask extends DefaultTask {

    @TaskAction
    void run() {
        def config = project.extensions.getByName( 'runOsgi' ) as OsgiConfig

        def target = getTarget( project, config )

        def createTask = project.tasks.getByName( 'createOsgiRuntime' ) as CreateOsgiRuntimeTask
        def bundlesDir = createTask.outputDir

        project.copy {
            from project.configurations.osgiRunTest
            from bundlesDir
            into project.file( target )
        }
    }

    static String getTarget( Project project, OsgiConfig osgiConfig ) {
        ( osgiConfig.outTestDir instanceof File ) ?
                osgiConfig.outTestDir.absolutePath :
                "${project.buildDir}/${osgiConfig.outTestDir}"
    }

}
