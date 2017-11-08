package com.athaydes.osgi.gradle.testrun;

import com.athaydes.osgi.gradle.testrun.comm.OsgiRunTestRunnerSettings;
import com.athaydes.osgi.gradle.testrun.comm.RemoteOsgiTestRunner;
import com.athaydes.osgi.rsa.provider.protobuf.api.RemoteServices;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.InitializationError;

import java.util.List;

/**
 * The OSGi-RUN JUnit4 test runner.
 * <p>
 * Annotating a test class with the {@link org.junit.runner.RunWith} annotation
 * causes it to be tested within the OSGi test environment created by the OSGi-RUN
 * Gradle plugin.
 * <p>
 * The test class may take any OSGi services it requires as parameters in its constructor.
 * Only one constructor is allowed.
 */
public class OsgiRunTestRunner extends BlockJUnit4ClassRunner {

    private RemoteOsgiTestRunner osgiTestRunner;

    public OsgiRunTestRunner(Class<?> testType) throws InitializationError {
        super(testType);
        osgiTestRunner = RemoteServices.createClient(RemoteOsgiTestRunner.class,
                OsgiRunTestRunnerSettings.getRemoteTestRunnerHost(),
                OsgiRunTestRunnerSettings.getRemoteTestRunnerPort());
    }

    @Override
    protected void validateConstructor(List<Throwable> errors) {
        String error = osgiTestRunner.startTest(getTestClass().getName());
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
        osgiTestRunner.stopTest(getTestClass().getName());
    }
}
