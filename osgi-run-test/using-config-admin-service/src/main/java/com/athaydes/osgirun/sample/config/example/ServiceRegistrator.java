package com.athaydes.osgirun.sample.config.example;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;
import java.util.Hashtable;

/**
 * BundleActivator that registers this bundle's service.
 */
public class ServiceRegistrator implements BundleActivator {

    private volatile ServiceRegistration<?> registrator;

    @Override
    public void start( BundleContext context ) throws Exception {
        Dictionary<String, String> props = new Hashtable<>();
        props.put( "service.pid", "example.config" );
        System.out.println( "Registering managed service with properties: " + props );
        registrator = context.registerService( ManagedService.class.getName(),
                new ConfigedService(), props );
    }

    @Override
    public void stop( BundleContext context ) throws Exception {
        System.out.println( "Un-registering managed service" );
        if ( registrator != null ) {
            registrator.unregister();
        }
    }
}
