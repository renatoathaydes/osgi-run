package com.athaydes.gradle.osgi.dependency;

import org.gradle.api.artifacts.ExternalModuleDependency;

public interface OSGiDependency extends ExternalModuleDependency {

    Integer getStartLevel();

    OSGiDependency setStartLevel(Integer startLevel);

    OSGiDependency copy();

}
