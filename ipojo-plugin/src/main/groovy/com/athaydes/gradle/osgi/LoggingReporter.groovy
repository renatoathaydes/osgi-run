package com.athaydes.gradle.osgi

import org.apache.felix.ipojo.manipulator.Reporter
import org.slf4j.Logger

/**
 *
 */
class LoggingReporter implements Reporter {

    final Logger log

    final List<String> warnings = [ ]
    final List<String> errors = [ ]

    LoggingReporter( Logger log ) {
        this.log = log
    }

    private message( String s, Object... objects ) {
        s + ( objects ? Arrays.toString( objects ) : '' )
    }

    @Override
    void trace( String s, Object... objects ) {
        log.trace( message( s, objects ) )
    }

    @Override
    void info( String s, Object... objects ) {
        log.info( message( s, objects ) )
    }

    @Override
    void warn( String s, Object... objects ) {
        def msg = message( s, objects )
        log.warn( msg )
        warnings.add( msg )
    }

    @Override
    void error( String s, Object... objects ) {
        def msg = message( s, objects )
        log.error( msg )
        errors.add( msg )
    }

}
