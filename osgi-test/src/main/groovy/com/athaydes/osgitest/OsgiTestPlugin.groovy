package com.athaydes.osgitest

import org.gradle.api.Plugin
import org.gradle.api.Project

class OsgiTestPlugin implements Plugin<Project> {

    @Override
    void apply( Project project ) {
        project.task(
                type: OsgiTestTask,
                group: 'Verification',
                description: 'Runs JUnit tests inside OSGi',
                'testOsgi' )
    }

}