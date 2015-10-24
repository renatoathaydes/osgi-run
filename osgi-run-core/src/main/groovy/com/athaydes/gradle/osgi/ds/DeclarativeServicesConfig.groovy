package com.athaydes.gradle.osgi.ds

import groovy.xml.MarkupBuilder
import org.gradle.api.GradleException

/**
 * Config for Declarative Services.
 */
class DeclarativeServicesConfig {

    private final _writer = new StringWriter()
    private final _xmlBuilder = new MarkupBuilder( _writer )

    def declarations = null
    String xmlFileName = 'OSGI-INF/ds.xml'

    void declarations( Closure config ) {
        config.delegate = _xmlBuilder
        config.run()
    }

//    def methodMissing( String name, args ) {
//        _xmlBuilder."$name"( *args )
//    }

    String getXmlFileContents() {
        _writer.toString() ?: readDeclarations()
    }

    private String readDeclarations() {
        readDeclarations declarations
    }

    private static String readDeclarations( source ) {
        switch ( source ) {
            case String: // fall-through
            case File: return readFileDeclarations( source as File )
            case Collection: return source.inject( '' ) { acc, item ->
                "$acc${readDeclarations( item )}\n"
            }
            default: throw new GradleException( 'Cannot read Declarative Services declarations from ' + source )
        }
    }

    private static String readFileDeclarations( File file ) {
        switch ( file.name ) {
            case ~/.*\.groovy$/: return ""
            case ~/.*\.xml$/: return file.text
            default: throw new GradleException( 'Declarative Services declarations file' +
                    ' must have extension .groovy or .xml' )
        }
    }

    void show() {
        println getXmlFileContents()
    }

}
