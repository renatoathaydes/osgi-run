package com.athaydes.osgitest

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class OsgiTestTask extends DefaultTask {

    @TaskAction
    void run() {
        println "Hello OSGi TEST"
    }

}