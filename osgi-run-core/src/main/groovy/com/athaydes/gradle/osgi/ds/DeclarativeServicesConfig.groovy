package com.athaydes.gradle.osgi.ds

import groovy.xml.MarkupBuilder
import org.codehaus.groovy.control.CompilerConfiguration
import org.gradle.api.GradleException

/**
 * Config for Declarative Services.
 */
class DeclarativeServicesConfig {

    private final groovyConfigBase = new GroovyConfigBaseClass()

    def declarations = null
    String xmlFileName = 'OSGI-INF/ds.xml'

    void declarations( Closure config ) {
        config.delegate = groovyConfigBase
        config.run()
    }

    String getXmlFileContents() {
        groovyConfigBase.run() ?: readDeclarations()
    }

    private String readDeclarations() {
        readDeclarations declarations
    }

    private String readDeclarations( source ) {
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
            case ~/.*\.groovy$/: return runGroovyConfig( file )
            case ~/.*\.xml$/: return file.text
            default: throw new GradleException( 'Declarative Services declarations file' +
                    ' must have extension .groovy or .xml' )
        }
    }

    private static String runGroovyConfig( File groovyScript ) {
        def compilerConfig = new CompilerConfiguration()
        compilerConfig.scriptBaseClass = GroovyConfigBaseClass.name
        GroovyShell shell = new GroovyShell( GroovyConfigBaseClass.classLoader, new Binding(), compilerConfig )
        def result = shell.parse( groovyScript )
        result.run()
        result.toString()
    }

    void show() {
        println getXmlFileContents()
    }

}

class GroovyConfigBaseClass extends Script {

    private final _writer = new StringWriter()
    private final _xmlBuilder = new MarkupBuilder( _writer )

    def methodMissing( String name, args ) {
        _xmlBuilder."$name"( *args )
    }

    @Override
    Object run() {
        _writer.toString()
    }

    @Override
    String toString() {
        _writer.toString()
    }

}
