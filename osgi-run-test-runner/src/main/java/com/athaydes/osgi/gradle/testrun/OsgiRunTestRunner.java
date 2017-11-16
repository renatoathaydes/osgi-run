package com.athaydes.osgi.gradle.testrun;

import com.athaydes.osgi.gradle.testrun.comm.OsgiRunTestRunnerSettings;
import com.athaydes.osgi.gradle.testrun.comm.RemoteOsgiTestRunner;
import com.athaydes.protobuf.tcp.api.CommunicationException;
import com.athaydes.protobuf.tcp.api.RemoteServices;
import java.io.Closeable;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.DynamicType;
import org.junit.Test;
import org.junit.internal.runners.statements.InvokeMethod;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The OSGi-RUN JUnit4 test runner.
 * <p>
 * Annotating a test class with the {@link org.junit.runner.RunWith} annotation
 * causes it to be tested within the OSGi test environment created by the OSGi-RUN
 * Gradle plugin.
 * <p>
 * The test class may take any OSGi services it requires as parameters in its constructor.
 * Only one constructor is allowed.
 * <p>
 * This class is meant to be used by JUnit to run the tests outside OSGi. Do not use this class within OSGi.
 */
public class OsgiRunTestRunner extends BlockJUnit4ClassRunner {

    private static final Logger log = LoggerFactory.getLogger( OsgiRunTestRunner.class );

    private static final AtomicInteger testInterfaceId = new AtomicInteger( 0 );
    private RemoteOsgiTestRunner osgiTestRunner;

    public OsgiRunTestRunner( Class<?> testType ) throws InitializationError {
        super( testType );
    }

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
    protected void validateConstructor( List<Throwable> errors ) {
        initializeRunner();
        String error;
        try {
            error = osgiTestRunner.startTest( getTestClass().getName() );
        } catch ( CommunicationException e ) {
            error = "Problem accessing the osgi-run remote test runner - make sure to start the server before " +
                    "running tests outside of a Gradle build. Cause: " + e.getCause();
        }
        if ( !error.isEmpty() ) {
            errors.add( new Exception( error ) );
        }
    }

    @Override
    protected void validateZeroArgConstructor( List<Throwable> errors ) {
        throw new UnsupportedOperationException( "OsgiRunTestRunner supports tests with a non-zero args constructor" );
    }

    @Override
    protected Object createTest() throws Exception {
        DynamicType.Builder<?> interfaceDefinition = new ByteBuddy().makeInterface()
                .name( getTestClass().getName() + "TestInterface" + testInterfaceId.incrementAndGet() );

        for ( FrameworkMethod m : getTestClass().getAnnotatedMethods( Test.class ) ) {
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
    public void run( RunNotifier notifier ) {
        super.run( notifier );
        Optional.of( osgiTestRunner ).ifPresent( ( o ) -> o.stopTest( getTestClass().getName() ) );
    }

    @Override
    protected Statement methodInvoker( FrameworkMethod method, Object test ) {
        Class<?> testInterface = findTestInterface( test );
        log.trace( "TestInterface found: {}", testInterface );
        FrameworkMethod actualMethod = findMethodIn( testInterface, method );
        return new InvokeMethod( actualMethod, test );
    }

    private Class<?> findTestInterface( Object test ) {
        // the client test instance should implement the test interface and Closeable
        return Arrays.stream( test.getClass().getInterfaces() )
                .filter( definition -> !( definition.equals( Closeable.class ) ) )
                .findFirst().orElseThrow( () -> new IllegalStateException(
                        "Tried to call non-test method" ) );
    }

    private FrameworkMethod findMethodIn( Class<?> testClass, FrameworkMethod method ) {
        return new FrameworkMethod( Arrays.stream( testClass.getDeclaredMethods() )
                .filter( m -> m.getName().equals( method.getName() ) )
                .findFirst().orElseThrow( () -> new IllegalStateException(
                        "Tried to call non-test method" ) ) );
    }

}
