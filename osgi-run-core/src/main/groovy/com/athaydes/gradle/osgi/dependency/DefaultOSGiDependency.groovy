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

    DefaultOSGiDependency( Map declaredConfig ) {
        this( *fromMap( declaredConfig ) )
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

    private static List fromSpec( String spec ) {
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

    private static List fromMap( Map declaredConfig ) {
        def errors = [ ]
        def conf = declaredConfig.clone() as Map

        def verifyProperty = { String name, boolean mandatory, Class type = String ->
            def value = conf.remove( name )
            if ( value == null ) {
                if ( mandatory ) {
                    errors << "'$name' is missing"
                }
            } else if ( !type.isInstance( value ) ) {
                errors << "'$name' has invalid type (must be ${type.name}, was ${value.class.name}"
            }
            value
        }

        def group = verifyProperty 'group', true
        def name = verifyProperty 'name', true
        def version = verifyProperty 'version', true
        def config = verifyProperty 'configuration', false
        def startLevel = verifyProperty 'startLevel', false, Integer

        if ( !conf.isEmpty() ) {
            def unknownProperties = conf.keySet().collect { "'$it'" }.join( ', ' )
            errors << "Could not set unknown properties $unknownProperties"
        }

        if ( errors ) {
            throw new GradleException( "Invalid OSGi dependency $declaredConfig:\n  ${errors.join( '\n  ' )}" )
        }

        [ group, name, version, config, startLevel ]
    }
}
