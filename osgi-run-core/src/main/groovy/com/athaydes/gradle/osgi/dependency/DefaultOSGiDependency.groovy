package com.athaydes.gradle.osgi.dependency

import groovy.transform.ToString
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

@ToString( includeFields = true, includeNames = true )
class DefaultOSGiDependency extends DefaultExternalModuleDependency implements OSGiDependency {

    Integer startLevel

    // TODO support providing single String coordinates
    DefaultOSGiDependency( String group, String name, String version,
                           String configuration = null, Integer startLevel = null ) {
        super( group, name, version, configuration )
        this.startLevel = startLevel
    }

    @Override
    DefaultOSGiDependency copy() {
        DefaultOSGiDependency copiedModuleDependency =
                new DefaultOSGiDependency( group, name, version, targetConfiguration, startLevel )
        copyTo( copiedModuleDependency )
        return copiedModuleDependency
    }
}
