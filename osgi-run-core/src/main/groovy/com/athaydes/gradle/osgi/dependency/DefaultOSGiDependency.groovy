package com.athaydes.gradle.osgi.dependency

import groovy.transform.ToString
import org.gradle.api.GradleException
import org.gradle.api.internal.artifacts.dependencies.DefaultExternalModuleDependency

@ToString( includeFields = true, includeNames = true )
class DefaultOSGiDependency extends DefaultExternalModuleDependency implements OSGiDependency {

    Integer startLevel

    DefaultOSGiDependency( String spec ) {
        this( *fromSpec( spec ) )
    }

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

    private static fromSpec = { String spec ->
        String[] parts = spec.split( ':' )
        if ( !( parts.size() in [ 3, 4, 5 ] ) ) {
            throw new GradleException( "Invalid OSGi dependency spec (must have format group:name:version[:configuration][:startLevel]): $spec" )
        }

        def args = [ ]

        parts.eachWithIndex { String entry, int i ->
            if ( i == 3 && parts.size() == 4 && entry.isInteger() ) {
                args << null << entry.toInteger()
            } else if ( i == 4 && entry.isInteger() ) {
                args << entry.toInteger()
            } else {
                args << entry
            }
        }

        args
    }
}
