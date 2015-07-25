package com.athaydes.osgi.host;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * Example that shows how we can get a resource that should be provided by
 * a fragment.
 */
public class HostBundleActivator implements BundleActivator {

    @Override
    public void start( BundleContext context ) throws Exception {
        URL resourceURL = getClass().getResource( "/fragment/example.properties" );
        if ( resourceURL == null ) {
            System.out.println( "Host Bundle could not get the fragment resource!" );
        } else {
            System.out.println( "Host Bundle found fragment resource! " + resourceURL );
            describePropertiesResource( resourceURL );
        }
    }

    @Override
    public void stop( BundleContext context ) throws Exception {

    }

    private void describePropertiesResource( URL resourceUrl ) {
        try ( InputStream stream = resourceUrl.openStream() ) {
            Properties properties = new Properties();
            properties.load( stream );
            System.out.println( "Got the following properties: " + properties );
        } catch ( IOException e ) {
            e.printStackTrace();
        }

    }

}
