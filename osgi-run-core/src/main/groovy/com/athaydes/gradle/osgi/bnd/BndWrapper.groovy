package com.athaydes.gradle.osgi.bnd

import aQute.bnd.osgi.Analyzer
import aQute.bnd.osgi.Jar
import com.athaydes.gradle.osgi.WrapInstructionsConfig
import com.athaydes.gradle.osgi.util.JarUtils
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

        // make a copy of the Map so that if more than one Jar matches, all of them get the same instructions
        Map<String, Object[]> config = new LinkedHashMap<>( getWrapConfig( wrapInstructions, jarFile ) )

        def consumeValue = { String key ->
            Object[] items = config.get( key )
            if ( items ) items.join( ',' )
            else null
        }

        if ( !config ) {
            log.info "No instructions provided to wrap bundle {}, will use defaults", jarFile.name
        }

        def newJar = new Jar( jarFile )

        String implVersion = consumeValue( 'Bundle-Version' ) ?: JarUtils.versionOf( newJar )

        String implTitle = consumeValue( 'Bundle-SymbolicName' ) ?: JarUtils.titleOf( newJar )

        String imports = consumeValue( 'Import-Package' ) ?: '*'
        String exports = consumeValue( 'Export-Package' ) ?: "*;version=$implVersion"

        def analyzer = new Analyzer().with {
            jar = newJar
            bundleVersion = implVersion
            bundleSymbolicName = implTitle
            importPackage = imports
            exportPackage = exports
            config.each { k, v -> setProperty( k, v.join( ',' ) ) }
            return it
        }

        Manifest manifest = analyzer.calcManifest()

        if ( wrapInstructions.printManifests ) {
            println " Manifest for ${jarFile.name} ".center( 100, '-' )
            manifest.write( System.out )
            println '-' * 100
        }

        def bundle = new File( "$bundlesDir/${jarFile.name}" )

        JarUtils.copyJar( jarFile, bundle ) {
            ZipFile input, ZipOutputStream out, ZipEntry entry ->
                if ( entry.name == 'META-INF/MANIFEST.MF' ) {
                    out.putNextEntry( new ZipEntry( entry.name ) )
                    manifest.write( out )
                } else {
                    out.putNextEntry( entry )
                    out.write( input.getInputStream( entry ).bytes )
                }
        }
    }

    private static Map<String, Object[]> getWrapConfig(
            WrapInstructionsConfig wrapInstructions, File jarFile ) {
        Map<String, Object[]> config = wrapInstructions.manifests.find { regx, _ ->
            try {
                def match = jarFile.name ==~ regx
                if ( match ) log.debug( 'Regex {} matched jar file {}', regx, jarFile.name )
                return match
            } catch ( e ) {
                log.warn( 'Invalid regex in wrapInstructions Map: {}', e as String )
                return false
            }
        }?.value

        config ?: [ : ]
    }


}
