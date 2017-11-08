package com.athaydes.osgi.gradle.testrun.comm;

/**
 * Remote OSGi test runner interface.
 * <p>
 * The implementation of this interface will run within the OSGi test environment
 * and will start, monitor and stop the test service (i.e. the OSGi test that needs
 * to run within the OSGi environment).
 */
public interface RemoteOsgiTestRunner {

    /**
     * Start a test.
     *
     * @param testClass name of the test class
     * @return empty String if the remote test service was started successfully,
     * otherwise an error message.
     */
    String startTest(String testClass);

    /**
     * Stop a test.
     *
     * @param testClass name of the test class
     */
    void stopTest(String testClass);
}
