package com.athaydes.gradle.osgi.testrun.bundle;

import com.athaydes.gradle.osgi.testrun.comm.OsgiRunTestRunnerSettings;
import com.athaydes.gradle.osgi.testrun.comm.RemoteOsgiTestRunner;
import com.athaydes.protobuf.tcp.api.RemoteServices;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

/**
 * OSGi test environment TestRunner implementation.
 * <p>
 * A client running on the JUnit test side is expected to send tests to be run within the OSGi
 * environment to this test runner.
 */
public class OsgiRunRemoteTestRunner implements RemoteOsgiTestRunner, BundleActivator {

    private static final Logger log = LoggerFactory.getLogger( OsgiRunRemoteTestRunner.class );

    private final AtomicReference<Closeable> testRunner = new AtomicReference<>();
    private final AtomicReference<BundleContext> bundleContext = new AtomicReference<>();
    private final Map<String, TestRunData> testServices = new ConcurrentHashMap<>( 2 );

    @Override
    public void start( BundleContext context ) throws Exception {
        log.debug( "Starting {}", getClass().getName() );
        bundleContext.set( context );
        int testRunnerPort = OsgiRunTestRunnerSettings.getRemoteTestRunnerPort();
        log.info( "Starting OSGi test runner service on port {}", testRunnerPort );
        testRunner.set( RemoteServices.provideService( this,
                testRunnerPort,
                RemoteOsgiTestRunner.class ) );
    }

    @Override
    public void stop( BundleContext context ) throws Exception {
        log.debug( "Stopping {}", getClass().getName() );
        bundleContext.set( null );
        closeService( testRunner.get() );
        for ( TestRunData testRunData : testServices.values() ) {
            testRunData.discard( context );
        }
        testServices.clear();
    }

    @Override
    public String startTest( String testClass ) {
        log.info( "Starting test service: {}", testClass );
        try {
            Class<?> testType = getClass().getClassLoader().loadClass( testClass );
            startServiceFor( testType );
        } catch ( ClassNotFoundException | NoClassDefFoundError e ) {
            log.warn( "Attempted to run non-existing test class", e );
            return e.toString();
        } catch ( IllegalAccessException | InstantiationException e ) {
            log.warn( "Unable to instantiate test class", e );
            return e.toString();
        } catch ( InvocationTargetException e ) {
            log.warn( "Unable to call constructor", e );
            return e.toString();
        }

        return "";
    }

    private void startServiceFor( Class<?> testType )
            throws IllegalAccessException, InstantiationException, InvocationTargetException {
        Constructor<?>[] constructors = testType.getConstructors();
        if ( constructors.length != 1 ) {
            throw new InstantiationException( "Test class does not contain exactly one public constructor" );
        }
        Class<?>[] parameterTypes = constructors[ 0 ].getParameterTypes();

        Object[] parameters = new Object[ parameterTypes.length ];
        ServiceReference<?>[] requiredServices = new ServiceReference[ parameterTypes.length ];

        for ( int i = 0; i < parameterTypes.length; i++ ) {
            ServiceReference<?> serviceRef = bundleContext.get().getServiceReference( parameterTypes[ i ] );
            if ( serviceRef == null ) {
                throw new InstantiationException( "Cannot create test instance, service not found: " + parameterTypes[ i ] );
            } else {
                parameters[ i ] = bundleContext.get().getService( serviceRef );
                requiredServices[ i ] = serviceRef;
            }
        }

        int servicePort = OsgiRunTestRunnerSettings.getRemoteTestServicePort();
        Object testInstance = constructors[ 0 ].newInstance( parameters );

        log.info( "Starting test service {} on port {}", testInstance.getClass().getName(), servicePort );
        Closeable testService = RemoteServices.provideService( testInstance, servicePort );

        testServices.put( testInstance.getClass().getName(), new TestRunData( testService, requiredServices ) );
    }

    @Override
    public void stopTest( String testClass ) {
        log.debug( "Stopping test service: {}", testClass );

        try {
            Optional.ofNullable( testServices.remove( testClass ) )
                    .ifPresent( it -> it.discard( bundleContext.get() ) );
        } finally {
            // stop the framework
            try {
                bundleContext.get().getBundle( 0L ).stop();
            } catch ( BundleException e ) {
                log.warn( "Error stopping framework", e );
            }
        }
    }

    private static void closeService( Closeable service ) {
        Optional.ofNullable( service ).ifPresent( server -> {
            try {
                server.close();
                log.debug( "Closed service: {}", server.getClass().getName() );
            } catch ( IOException e ) {
                log.warn( "Problem closing service of type " +
                        server.getClass().getName(), e );
            }
        } );
    }


    private static class TestRunData {
        final Closeable testService;
        final ServiceReference<?>[] requiredServices;

        TestRunData( Closeable testService, ServiceReference<?>[] requiredServices ) {
            this.testService = testService;
            this.requiredServices = requiredServices;
        }

        void discard( BundleContext bundleContext ) {
            for ( ServiceReference<?> requiredService : requiredServices ) {
                bundleContext.ungetService( requiredService );
            }
            closeService( testService );
        }

    }

}
