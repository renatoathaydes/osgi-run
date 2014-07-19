package com.athaydes.gradle.osgi

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 *
 */
class ManifestReader {

    static Map manifestAsMap( File bundle ) {
        def extractKey = { String line -> line[ 0..<line.indexOf( ':' ) ].trim() }
        def extractValue = { String line -> line[ ( line.indexOf( ':' ) + 1 )..-1 ].trim() }
        def zip = new ZipFile( bundle )
        try {
            ZipEntry entry = zip.getEntry( 'META-INF/MANIFEST.MF' )
            assert entry
            def lines = zip.getInputStream( entry ).readLines()
            def map = [ : ]
            def prevKey = null
            for ( line in lines ) {
                if ( line.startsWith( ' ' ) ) {
                    def currentValue = map[ prevKey ]
                    map[ prevKey ] = currentValue + line.trim()
                } else if ( !line.trim().empty ) {
                    map[ prevKey = extractKey( line ) ] = extractValue( line )
                }
            }
            return map
        } finally {
            zip.close()
        }
    }

}
