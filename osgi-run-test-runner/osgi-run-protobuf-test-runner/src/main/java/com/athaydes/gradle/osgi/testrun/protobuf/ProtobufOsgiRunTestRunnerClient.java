package com.athaydes.gradle.osgi.testrun.protobuf;

import com.athaydes.gradle.osgi.testrun.comm.OsgiRunTestRunnerSettings;
import com.athaydes.gradle.osgi.testrun.comm.RemoteOsgiTestRunner;
import com.athaydes.gradle.osgi.testrun.internal.RemoteOsgiRunTestRunnerClient;
import com.athaydes.protobuf.tcp.api.CommunicationException;
import com.athaydes.protobuf.tcp.api.RemoteServices;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.ConnectException;
import java.net.Socket;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * OSGi Test runner client implementation.
 */
public class ProtobufOsgiRunTestRunnerClient implements RemoteOsgiRunTestRunnerClient {

    private static final Logger log = LoggerFactory.getLogger( ProtobufOsgiRunTestRunnerClient.class );

    private static final AtomicInteger testInterfaceId = new AtomicInteger( 0 );
    private RemoteOsgiTestRunner osgiTestRunner;
    private Process osgiTestEnvironmentProcess;

    private void initializeRunner() {
        if ( osgiTestRunner == null ) {
            String runScriptLocation = System.getProperty( "osgirun.runscript" );
            if ( runScriptLocation == null ) {
                throw new RuntimeException( "Please set the 'osgirun.runscript' system property to point to the " +
                        "OSGi run script in order to be able to run OSGi tests" );
            }

            File runScript = new File( runScriptLocation );
            log.debug( "Attempting to run OSGi test script at {}", runScript );

            if ( !runScript.isFile() ) {
                throw new RuntimeException( "The 'osgirun.runscript' system property is not pointing to an " +
                        "existing file, cannot start the test OSGi runtime" );
            }

            if ( !runScript.canExecute() ) {
                boolean canExecute = runScript.setExecutable( true );
                if ( !canExecute ) {
                    throw new RuntimeException( "Cannot make script to start the test OSGi environment executable, " +
                            "please do it manually: " + runScript );
                }
            }

            String host = OsgiRunTestRunnerSettings.getRemoteTestRunnerHost();
            int port = OsgiRunTestRunnerSettings.getRemoteTestRunnerPort();

            try {
                osgiTestEnvironmentProcess = new ProcessBuilder( runScript.getAbsolutePath() )
                        .inheritIO()
                        .start();

                waitForServerToStart( host, port );
            } catch ( IOException | InterruptedException e ) {
                throw new RuntimeException( "Could not start test OSGi environment", e );
            }

            log.info( "Starting OSGi test runner, connecting to remote OSGi test server at {}:{}",
                    host, port );
            osgiTestRunner = RemoteServices.createClient( RemoteOsgiTestRunner.class,
                    host, port );
        }
    }

    @Override
    public Object createTest( String testClassName ) throws Exception {
        DynamicType.Builder<?> interfaceDefinition = new ByteBuddy().makeInterface()
                .name( testClassName + "TestInterface" + testInterfaceId.incrementAndGet() );

        TestClass testClass = new TestClass( Class.forName( testClassName ) );

        for ( FrameworkMethod m : testClass.getAnnotatedMethods( Test.class ) ) {
            log.debug( "Adding test method to generated interface: {}", m );
            interfaceDefinition = interfaceDefinition
                    .defineMethod( m.getName(), m.getReturnType(), Modifier.PUBLIC )
                    .throwing( Throwable.class )
                    .withoutCode();
        }

        Class<?> testInterface = interfaceDefinition.make()
                .load( ClassLoader.getSystemClassLoader() )
                .getLoaded();

        log.debug( "Created test interface '{}' with {} methods",
                testInterface.getName(), testInterface.getDeclaredMethods().length );

        return RemoteServices.createClient( testInterface,
                OsgiRunTestRunnerSettings.getRemoteTestRunnerHost(),
                OsgiRunTestRunnerSettings.getNextRemoteTestServicePort() );
    }

    @Override
    public Optional<String> startTest( String testClass ) {
        initializeRunner();

        String error;
        try {
            error = osgiTestRunner.startTest( testClass );
        } catch ( CommunicationException e ) {
            error = "Problem accessing the osgi-run remote test runner - make sure to start the server before " +
                    "running tests outside of a Gradle build. Cause: " + e.getCause();
        }
        if ( !error.isEmpty() ) {
            return Optional.of( error );
        } else {
            return Optional.empty();
        }
    }

    @Override
    public void stopTest( String testClass ) {
        // stopping the test should cause the test OSGi environment to die
        osgiTestRunner.stopTest( testClass );

        boolean processDead = false;
        try {
            processDead = osgiTestEnvironmentProcess.waitFor( 2, TimeUnit.SECONDS );
        } catch ( InterruptedException e ) {
            log.warn( "Interrupted while waiting for Test OSGi environment to stop" );
        }

        if ( !processDead ) {
            throw new RuntimeException( "The test OSGi process did not die within the timeout." );
        }
    }

    private void waitForServerToStart( String host, int port ) throws InterruptedException, IOException {
        log.debug( "Waiting for OSGi TestRunner to start" );
        long timeout = System.currentTimeMillis() + 15_000L;
        while ( System.currentTimeMillis() < timeout ) {
            try {
                Socket socket = new Socket( host, port );
                socket.getOutputStream();
                socket.close();
                log.debug( "OSGi TestRunner has started!" );
                return;
            } catch ( ConnectException e ) {
                // as expected before the server starts
                Thread.sleep( 250L );
            }
        }

        // timeout
        throw new RuntimeException( "Test OSGi server has not started within the timeout" );
    }

}
