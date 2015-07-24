package com.athaydes.myimpl;

import com.athaydes.myapi.MyService;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

/**
 * Bundle Activator - used to register the service we want to provide.
 */
public class MyImplActivator implements BundleActivator {

    @Override
    public void start( BundleContext context ) throws Exception {
        MyService service = new MyServiceImpl();
        context.registerService( MyService.class, service, null );
        System.out.println("Exported MyService implementation");
    }

    @Override
    public void stop( BundleContext context ) throws Exception {

    }
}
