package com.athaydes.gradle.osgi.util

import java.util.zip.ZipEntry
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

    static boolean notBundle( File file ) {
        def zip = new ZipFile( file )
        try {
            ZipEntry entry = zip.getEntry( 'META-INF/MANIFEST.MF' )
            if ( !entry ) return true
            def lines = zip.getInputStream( entry ).readLines()
            return !lines.any { it.trim().startsWith( 'Bundle' ) }
        } finally {
            zip.close()
        }
    }

    static boolean isFragment( file ) {
        def zip = new ZipFile( file as File )
        try {
            ZipEntry entry = zip.getEntry( 'META-INF/MANIFEST.MF' )
            if ( !entry ) return true
            def lines = zip.getInputStream( entry ).readLines()
            return lines.any { it.trim().startsWith( 'Fragment-Host' ) }
        } finally {
            zip.close()
        }
    }

}
