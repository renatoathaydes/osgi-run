package com.athaydes.gradle.osgi.dependency

import groovy.transform.ToString
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

@ToString( includeFields = true, includeNames = true )
class DefaultOSGiDependency extends DefaultExternalModuleDependency implements OSGiDependency {

    private Integer startLevel

    DefaultOSGiDependency(String group, String name, String version) {
        this(group, name, version, null, null)
    }

    DefaultOSGiDependency(String group, String name, String version, String configuration) {
        this(group, name, version, configuration, null)
    }

    DefaultOSGiDependency(String group, String name, String version, String configuration, Integer startLevel) {
        super(group, name, version, configuration)
        this.startLevel = startLevel
    }

    Integer getStartLevel() {
        startLevel
    }

    DefaultOSGiDependency setStartLevel(Integer startLevel) {
        this.startLevel = startLevel
        return this
    }

    @Override
    DefaultOSGiDependency copy() {
        DefaultOSGiDependency copiedModuleDependency =
                new DefaultOSGiDependency(getGroup(), getName(), getVersion(), getTargetConfiguration(), getStartLevel())
        copyTo(copiedModuleDependency)
        return copiedModuleDependency
    }
}
