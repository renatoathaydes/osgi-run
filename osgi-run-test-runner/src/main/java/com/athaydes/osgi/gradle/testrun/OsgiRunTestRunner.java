package com.athaydes.osgi.gradle.testrun;

import com.athaydes.osgi.gradle.testrun.comm.OsgiRunTestRunnerSettings;
import com.athaydes.osgi.gradle.testrun.comm.RemoteOsgiTestRunner;
import com.athaydes.protobuf.tcp.api.CommunicationException;
import com.athaydes.protobuf.tcp.api.RemoteServices;
import java.util.List;
import java.util.Optional;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

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

    private RemoteOsgiTestRunner osgiTestRunner;

    public OsgiRunTestRunner(Class<?> testType) throws InitializationError {
        super(testType);
    }

    private void initializeRunner() {
        if (osgiTestRunner == null) {
            osgiTestRunner = RemoteServices.createClient(RemoteOsgiTestRunner.class,
                    OsgiRunTestRunnerSettings.getRemoteTestRunnerHost(),
                    OsgiRunTestRunnerSettings.getRemoteTestRunnerPort());
        }
    }

    @Override
    protected void validateConstructor(List<Throwable> errors) {
        initializeRunner();
        String error;
        try {
            error = osgiTestRunner.startTest(getTestClass().getName());
        } catch (CommunicationException e) {
            error = "Problem accessing the osgi-run remote test runner - make sure to start the server before " +
                    "running tests outside of a Gradle build. Cause: " + e.getCause();
        }
        if (!error.isEmpty()) {
            errors.add(new Exception(error));
        }
    }

    @Override
    protected void validateZeroArgConstructor(List<Throwable> errors) {
        throw new UnsupportedOperationException("OsgiRunTestRunner supports tests with a non-zero args constructor");
    }

    @Override
    protected Object createTest() throws Exception {
        return RemoteServices.createClient(getTestClass().getJavaClass(),
                OsgiRunTestRunnerSettings.getRemoteTestRunnerHost(),
                OsgiRunTestRunnerSettings.getNextRemoteTestServicePort());
    }

    @Override
    public void run(RunNotifier notifier) {
        super.run(notifier);
        Optional.of(osgiTestRunner).ifPresent((o) -> o.stopTest(getTestClass().getName()));
    }
}
