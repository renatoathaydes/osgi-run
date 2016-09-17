package com.athaydes.gradle.osgi.util

import aQute.bnd.osgi.Jar

import java.util.concurrent.Callable
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Helper functions to work with jars.
 */
class JarUtils {

    /**
     * Attempt to consume the Manifest Jar entry.
     *
     * If the Manifest Jar entry is found, the consumeManifest closure is called with
     * ZipFile and ZipEntry as its arguments.
     *
     * If the Manifest Jar entry does not exist, the manifestMissing Callable is invoked.
     *
     * @param file an object representing the Jar which can be coerced to a File
     * @param consumeManifest closure taking a ZipFile and a ZipEntry as arguments
     * @param manifestMissing callable to run in case the entry is not found
     * @return whatever the function that ran returned.
     */
    static withManifestEntry( file, Closure consumeManifest, Callable manifestMissing = { -> } ) {
        withJarEntry( file, 'META-INF/MANIFEST.MF', consumeManifest, manifestMissing )
    }

    /**
     * Attempt to consume a Jar entry.
     *
     * If the Jar entry is found, the consumeEntry closure is called with
     * ZipFile and ZipEntry as its arguments.
     *
     * If the Jar entry does not exist, the entryMissing Callable is invoked.
     *
     * @param file an object representing the Jar which can be coerced to a File
     * @param entryName name of the Jar entry to consume
     * @param consumeEntry closure taking a ZipFile and a ZipEntry as arguments
     * @param entryMissing callable to run in case the entry is not found
     * @return whatever the function that ran returned.
     */
    static withJarEntry( file, String entryName,
                         Closure consumeEntry,
                         Callable entryMissing ) {
        def zip = new ZipFile( file as File )
        try {
            ZipEntry entry = zip.getEntry( entryName )
            if ( !entry ) return entryMissing()
            else return consumeEntry( zip, entry )
        } finally {
            zip.close()
        }
    }

    static void copyJar( File source, File destination,
                         Closure copyFunction,
                         Closure afterFunction = { _ -> } ) {
        def destinationStream = new ZipOutputStream( destination.newOutputStream() )
        def input = new ZipFile( source )
        try {
            for ( entry in input.entries() ) {
                copyFunction( input, destinationStream, entry )
            }
            afterFunction( destinationStream )
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

    static boolean hasManifest( File file ) {
        withManifestEntry( file, { ZipFile zip, ZipEntry entry -> true }, { false } )
    }

    static boolean notBundle( File file ) {
        withManifestEntry( file, { ZipFile zip, ZipEntry entry ->
            def lines = zip.getInputStream( entry ).readLines()
            !lines.any { it.trim().startsWith( 'Bundle' ) }
        }, { true } ) // no manifest, so it's not a bundle
    }

    static boolean isBundle( File file ) {
        !notBundle( file )
    }

    static boolean isFragment( file ) {
        withManifestEntry( file, { ZipFile zip, ZipEntry entry ->
            def lines = zip.getInputStream( entry ).readLines()
            lines.any { it.trim().startsWith( 'Fragment-Host' ) }
        }, { false } ) // no manifest, so we can't tell whether this is a fragment or not
    }

    static String versionOf( Jar jarFile ) {
        def attributes = jarFile.manifest.mainAttributes
        attributes.getValue( 'Bundle-Version' ) ?:
                        attributes.getValue( 'Implementation-Version' ) ?:
                                FileNameUtils.versionFrom( jarFile.name )
    }

    static String titleOf( Jar jarFile ) {
        def attributes = jarFile.manifest.mainAttributes
        attributes.getValue( 'Bundle-SymbolicName' ) ?:
                        attributes.getValue( 'Implementation-Title' ) ?:
                                FileNameUtils.titleFrom( jarFile.name )
    }

}
