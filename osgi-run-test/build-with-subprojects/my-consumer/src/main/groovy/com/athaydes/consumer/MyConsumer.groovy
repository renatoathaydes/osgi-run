package com.athaydes.consumer

import com.athaydes.myapi.MyService
import org.osgi.framework.BundleActivator
import org.osgi.framework.BundleContext


/**
 *
 */
class MyConsumer implements BundleActivator {

    @Override
    void start( BundleContext context ) throws Exception {
        println "Started MyConsumer"
        def ref = context.getServiceReference( MyService )
        println "Got service ref $ref"
        if ( ref ) {
            def service = context.getService( ref )
            println "Got service with message: ${service.message()}"
        }
    }

    @Override
    void stop( BundleContext context ) throws Exception {
        println "Stopped MyConsumer"
    }
}
