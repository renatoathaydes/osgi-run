package com.athaydes.gradle.osgi.dependency;

import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency;

public class DefaultOSGiDependency extends DefaultExternalModuleDependency implements OSGiDependency {

    private Integer startLevel;

    public DefaultOSGiDependency(String group, String name, String version) {
        this(group, name, version, null, null);
    }

    public DefaultOSGiDependency(String group, String name, String version, String configuration) {
        this(group, name, version, configuration, null);
    }
    public DefaultOSGiDependency(String group, String name, String version, String configuration, Integer startLevel) {
        super(group, name, version, configuration);
        this.startLevel = startLevel;
    }

    public Integer getStartLevel() {
        return startLevel;
    }

    public DefaultOSGiDependency setStartLevel(Integer startLevel) {
        this.startLevel = startLevel;
        return this;
    }

    @Override
    public DefaultOSGiDependency copy() {
        DefaultOSGiDependency copiedModuleDependency =
                new DefaultOSGiDependency(getGroup(), getName(), getVersion(), getTargetConfiguration(), getStartLevel());
        copyTo(copiedModuleDependency);
        return copiedModuleDependency;
    }

    @Override
    public String toString() {
        return String.format("DefaultOSGiDependency{group='%s', name='%s', version='%s', configuration='%s', startLevel=%s}",
                getGroup(), getName(), getVersion(),
                getTargetConfiguration() != null ? getTargetConfiguration() : Dependency.DEFAULT_CONFIGURATION,
                startLevel == null ? "N/A" : startLevel);
    }
}
