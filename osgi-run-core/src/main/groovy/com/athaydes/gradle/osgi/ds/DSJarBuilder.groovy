package com.athaydes.gradle.osgi.ds

import com.athaydes.gradle.osgi.util.JarUtils
import org.gradle.api.GradleException

import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream

/**
 * Adds declarative services data to a jar.
 */
class DSJarBuilder {

    final ant = new AntBuilder()

    void addDeclarativeServices( File jar, DeclarativeServicesConfig config ) {
        def dsXml = config.xmlFileContents

        if ( dsXml && JarUtils.isBundle( jar ) ) {
            def tempJar = new File( File.createTempDir(), jar.name )
            JarUtils.copyJar( jar, tempJar, { ZipFile input, ZipOutputStream out, ZipEntry entry ->
                out.putNextEntry( new ZipEntry( entry.name ) )
                if ( entry.name == 'META-INF/MANIFEST.MF' ) {
                    def lines = input.getInputStream( entry ).readLines()
                    if ( !lines.any { it ==~ /^Service-Component.*:.+/ } ) {
                        lines = lines + "Service-Component: ${config.xmlFileName}"
                    }
                    lines.each { line ->
                        if ( line ) out.write( "$line\n".bytes )
                    }
                } else {
                    out.write( input.getInputStream( entry ).bytes )
                }
            }, { ZipOutputStream out ->
                out.putNextEntry( new ZipEntry( config.xmlFileName ) )
                if ( !dsXml.startsWith( '<?' ) ) {
                    out.write( '<?xml version="1.0" encoding="UTF-8"?>\n'.bytes )
                }
                out.write( dsXml.bytes )
            } )

            if ( !jar.delete() ) {
                throw new GradleException( "Could not delete jar to add Declarative Services Meta-data: $jar" )
            }

            ant.move( file: tempJar, tofile: jar )
        }
    }

}
