package com.athaydes.gradle.osgi.bnd

import aQute.bnd.osgi.Analyzer
import aQute.bnd.osgi.Jar
import com.athaydes.gradle.osgi.WrapInstructionsConfig
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.jar.Manifest
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Uses Bnd to wrap a jar.
 */
class BndWrapper {

    static final Logger log = Logging.getLogger( BndWrapper )

    static void wrapNonBundle( File jarFile, String bundlesDir,
                               WrapInstructionsConfig wrapInstructions ) {
        log.info "Wrapping non-bundle: {}", jarFile.name

        def newJar = new Jar( jarFile )
        def currentManifest = newJar.manifest

        Map<Object, Object[]> config = getWrapConfig( wrapInstructions, jarFile )

        if ( !config ) {
            log.info "No instructions provided to wrap bundle {}, will use defaults", jarFile.name
        }

        String implVersion = config.remove( 'Bundle-Version' ) ?:
                currentManifest.mainAttributes.getValue( 'Implementation-Version' ) ?:
                        versionFromFileName( jarFile.name )

        String implTitle = config.remove( 'Bundle-SymbolicName' ) ?:
                currentManifest.mainAttributes.getValue( 'Implementation-Title' ) ?:
                        titleFromFileName( jarFile.name )

        String imports = config.remove( 'Import-Package' )?.join( ',' ) ?: '*'
        String exports = config.remove( 'Export-Package' )?.join( ',' ) ?: '*'

        def analyzer = new Analyzer().with {
            jar = newJar
            bundleVersion = implVersion
            bundleSymbolicName = implTitle
            importPackage = imports
            exportPackage = exports
            config.each { k, v -> it.setProperty( k as String, v.join( ',' ) ) }
            return it
        }

        Manifest manifest = analyzer.calcManifest()

        def bundle = new ZipOutputStream( new File( "$bundlesDir/${jarFile.name}" ).newOutputStream() )
        def input = new ZipFile( jarFile )
        try {
            for ( entry in input.entries() ) {
                if ( entry.name == 'META-INF/MANIFEST.MF' ) {
                    bundle.putNextEntry( new ZipEntry( entry.name ) )
                    manifest.write( bundle )
                } else {
                    bundle.putNextEntry( entry )
                    bundle.write( input.getInputStream( entry ).bytes )
                }
            }
        } finally {
            bundle.close()
            input.close()
        }
    }

    private static Map getWrapConfig( WrapInstructionsConfig wrapInstructions, File jarFile ) {
        Map config = wrapInstructions.manifests.find { regx, _ ->
            try {
                jarFile.name ==~ regx
            } catch ( e ) {
                log.warn( 'Invalid regex in wrapInstructions Map: {}', e as String )
                return false
            }
        }?.value

        config ?: [ : ]
    }

    static String removeExtensionFrom( String name ) {
        def dot = name.lastIndexOf( '.' )
        if ( dot > 0 ) { // exclude extension
            return name[ 0..<dot ]
        }
        return name
    }

    static String versionFromFileName( String name ) {
        name = removeExtensionFrom( name )
        def digitsAfterDash = name.find( /\-\d+.*/ )
        if ( digitsAfterDash ) {
            return digitsAfterDash[ 1..-1 ] // without the dash
        }
        int digit = name.findIndexOf { it.number }
        if ( digit > 0 ) {
            return name[ digit..-1 ]
        }
        '1.0.0'
    }

    static String titleFromFileName( String name ) {
        name = removeExtensionFrom( name )
        def digitsAfterDash = name.find( /\-\d+.*/ )
        if ( digitsAfterDash ) {
            return name - digitsAfterDash
        }
        int digit = name.findIndexOf { it.number }
        if ( digit > 0 ) {
            return name[ 0..<digit ]
        }
        name
    }


}
