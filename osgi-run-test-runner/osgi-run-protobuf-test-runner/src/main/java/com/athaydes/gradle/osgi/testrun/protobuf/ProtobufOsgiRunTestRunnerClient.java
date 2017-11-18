package com.athaydes.gradle.osgi.testrun.protobuf;

import com.athaydes.gradle.osgi.testrun.comm.OsgiRunTestRunnerSettings;
import com.athaydes.gradle.osgi.testrun.comm.RemoteOsgiTestRunner;
import com.athaydes.gradle.osgi.testrun.internal.RemoteOsgiRunTestRunnerClient;
import com.athaydes.protobuf.tcp.api.CommunicationException;
import com.athaydes.protobuf.tcp.api.RemoteServices;
import java.lang.reflect.Modifier;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.Test;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProtobufOsgiRunTestRunnerClient implements RemoteOsgiRunTestRunnerClient {

    private static final Logger log = LoggerFactory.getLogger( ProtobufOsgiRunTestRunnerClient.class );

    private static final AtomicInteger testInterfaceId = new AtomicInteger( 0 );
    private RemoteOsgiTestRunner osgiTestRunner;

    private void initializeRunner() {
        if ( osgiTestRunner == null ) {
            String host = OsgiRunTestRunnerSettings.getRemoteTestRunnerHost();
            int port = OsgiRunTestRunnerSettings.getRemoteTestRunnerPort();
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
        osgiTestRunner.stopTest( testClass );
    }

}
