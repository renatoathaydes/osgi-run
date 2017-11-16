package com.athaydes.osgi.gradle.testrun.comm;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * OSGi-RUN test runner settings.
 */
public final class OsgiRunTestRunnerSettings {

    public static final class SystemProperties {
        public static final String REMOTE_TEST_RUNNER_HOST = "com.athaydes.osgi-run.test-runner.host";
        public static final String REMOTE_TEST_RUNNER_PORT = "com.athaydes.osgi-run.test-runner.port";
        public static final String REMOTE_TEST_SERVICE_FIRST_PORT = "com.athaydes.osgi-run.test-service.first-port";
    }

    public static final class Defaults {
        public static final String REMOTE_TEST_RUNNER_HOST = "127.0.0.1";
        public static final int REMOTE_TEST_RUNNER_PORT = 5880;
        public static final int REMOTE_TEST_SERVICE_FIRST_PORT = 5881;
    }

    private static final AtomicInteger remoteServicePortOffset = new AtomicInteger(0);

    /**
     * @return the host where the remote test runner is running.
     */
    public static String getRemoteTestRunnerHost() {
        return System.getProperty(SystemProperties.REMOTE_TEST_RUNNER_HOST, Defaults.REMOTE_TEST_RUNNER_HOST);
    }

    /**
     * @return the port where the remote test runner is running.
     */
    public static int getRemoteTestRunnerPort() {
        return getIntProperty(SystemProperties.REMOTE_TEST_RUNNER_PORT, Defaults.REMOTE_TEST_RUNNER_PORT);
    }

    /**
     * @return the port where the next test class service should be running.
     * <b>
     * This method returns a different port every time it is called!
     * </b>
     * This allows both server and client to keep track of which port should be used by each
     * service being tested. The first port returned is given by {@link Defaults#REMOTE_TEST_SERVICE_FIRST_PORT}
     * or {@link SystemProperties#REMOTE_TEST_SERVICE_FIRST_PORT}, and each time this method is called,
     * the port is incremented by 1.
     */
    public static int getNextRemoteTestServicePort() {
        return remoteServicePortOffset.getAndIncrement() +
                getIntProperty(SystemProperties.REMOTE_TEST_SERVICE_FIRST_PORT, Defaults.REMOTE_TEST_SERVICE_FIRST_PORT);
    }

    private static int getIntProperty(String key, int defaultValue) {
        String value = System.getProperty(key);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return defaultValue;
    }
}
