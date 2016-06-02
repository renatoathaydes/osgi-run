package com.athaydes.frege.osgi;

import frege.repl.FregeRepl;
import frege.runtime.Concurrent;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

public class FregeMain implements BundleActivator {

    public FregeMain() {

    }

    @Override
    public void start( BundleContext context ) throws Exception {
        System.out.println( "Frege Bundle activated" );
        new Thread( () -> FregeRepl.main( new String[ 0 ] ) ).start();
    }

    @Override
    public void stop( BundleContext context ) throws Exception {
        System.out.println( "Frege stopping. Goodbye!" );
        Concurrent.shutDownIfExists();
    }

}
