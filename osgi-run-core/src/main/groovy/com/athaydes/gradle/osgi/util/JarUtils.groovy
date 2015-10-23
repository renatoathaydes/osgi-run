package com.athaydes.gradle.osgi.util

import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Helper functions to work with jars.
 */
class JarUtils {

    static void copyJar( File source, File destination, Closure copyFunction ) {
        def destinationStream = new ZipOutputStream( destination.newOutputStream() )
        def input = new ZipFile( source )
        try {
            for ( entry in input.entries() ) {
                copyFunction( input, destinationStream, entry )
            }
        } finally {
            try {
                destinationStream.close()
            } catch ( ignored ) {
            }
            try {
                input.close()
            } catch ( ignored ) {
            }
        }
    }


}
