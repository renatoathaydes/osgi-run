package com.athaydes.consumer

import com.athaydes.myapi.MyService
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceEvent
import org.osgi.framework.ServiceListener
import org.osgi.framework.ServiceReference


/**
 * Simple example of a service consumer using the low-level OSGI API.
 */
class MyConsumer implements BundleActivator {

    protected volatile ServiceReference _serviceReference

    @Override
    void start( BundleContext context ) throws Exception {
        println "Started MyConsumer"
        def ref = context.getServiceReference( MyService )
        println "Got service ref $ref"
        if ( ref ) {
            printServiceMessage context, ref
        } else {
            // if the service is not there yet, let's listen for when it gets registered
            def serviceFilter = "(objectclass=${MyService.name})"
            context.addServiceListener( { ServiceEvent event ->
                switch ( event.type ) {
                    case ServiceEvent.REGISTERED:
                        println "Detected registration of a Service!"
                        printServiceMessage context, event.serviceReference
                }
            } as ServiceListener, serviceFilter )
        }
    }

    @Override
    void stop( BundleContext context ) throws Exception {
        println "Stopped MyConsumer"
        final ref = _serviceReference // protect against another Thread modifying the variable
        if ( ref ) {
            context.ungetService( ref )
        }
    }

    void printServiceMessage( BundleContext context, ServiceReference<MyService> serviceReference ) {
        def service = context.getService( serviceReference )
        println "Got service with message: ${service.message()}"

        // remember service ref so we can unget it later
        _serviceReference = serviceReference
    }
}
