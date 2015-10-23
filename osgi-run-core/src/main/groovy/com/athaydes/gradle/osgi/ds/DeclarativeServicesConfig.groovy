package com.athaydes.gradle.osgi.ds

import groovy.xml.MarkupBuilder

/**
 * Config for Declarative Services.
 */
class DeclarativeServicesConfig {

    final _writer = new StringWriter()
    final _xmlBuilder = new MarkupBuilder( _writer )

    String dsFile = 'OSGI-INF/ds.xml'

    def methodMissing( String name, args ) {
        _xmlBuilder."$name"( *args )
    }

    void show() {
        println _writer.toString()
    }

}
