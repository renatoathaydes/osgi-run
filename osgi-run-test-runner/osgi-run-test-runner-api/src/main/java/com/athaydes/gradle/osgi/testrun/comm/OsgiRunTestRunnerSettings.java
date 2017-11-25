package com.athaydes.gradle.osgi.testrun.comm;

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

    /**
     * @return the host where the remote test runner is running.
     */
    public static String getRemoteTestRunnerHost() {
        return System.getProperty( SystemProperties.REMOTE_TEST_RUNNER_HOST, Defaults.REMOTE_TEST_RUNNER_HOST );
    }

    /**
     * @return the port where the remote test runner is running.
     */
    public static int getRemoteTestRunnerPort() {
        return getIntProperty( SystemProperties.REMOTE_TEST_RUNNER_PORT, Defaults.REMOTE_TEST_RUNNER_PORT );
    }

    /**
     * @return the port where the test class service is running.
     */
    public static int getRemoteTestServicePort() {
        return getIntProperty( SystemProperties.REMOTE_TEST_SERVICE_FIRST_PORT, Defaults.REMOTE_TEST_SERVICE_FIRST_PORT );
    }

    private static int getIntProperty( String key, int defaultValue ) {
        String value = System.getProperty( key );
        if ( value != null ) {
            try {
                return Integer.parseInt( value );
            } catch ( NumberFormatException e ) {
                // ignore
            }
        }
        return defaultValue;
    }

}
