package com.athaydes.gradle.osgi.dependency;

import org.gradle.api.artifacts.ExternalModuleDependency

interface OSGiDependency extends ExternalModuleDependency {

    Integer getStartLevel()

    void setStartLevel( Integer startLevel )

    OSGiDependency copy()

}
