package com.athaydes.gradle.osgi.util

import groovy.transform.CompileStatic

@CompileStatic
class FileNameUtils {


    static String versionFrom( String name ) {
        name = removeExtensionFrom( name )
        def digitsAfterDash = name.find( /\-\d+.*/ )
        if ( digitsAfterDash ) {
            return digitsAfterDash[ 1..-1 ] // without the dash
        }
        int digit = name.findIndexOf { String s -> s.isNumber() }
        if ( digit > 0 ) {
            return name[ digit..-1 ]
        }
        '1.0.0'
    }

    static String titleFrom( String name ) {
        name = removeExtensionFrom( name )
        def digitsAfterDash = name.find( /\-\d+.*/ )
        if ( digitsAfterDash ) {
            return name - digitsAfterDash
        }
        int digit = name.findIndexOf { String s -> s.isNumber() }
        if ( digit > 0 ) {
            return name[ 0..<digit ]
        }
        name
    }

    static String removeExtensionFrom( String name ) {
        def dot = name.lastIndexOf( '.' )
        if ( dot > 0 ) { // exclude extension
            return name[ 0..<dot ]
        }
        return name
    }

}
