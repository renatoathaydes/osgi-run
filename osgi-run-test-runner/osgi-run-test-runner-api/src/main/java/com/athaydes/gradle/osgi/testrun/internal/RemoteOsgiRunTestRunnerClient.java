package com.athaydes.gradle.osgi.testrun.internal;

import java.util.Optional;

public interface RemoteOsgiRunTestRunnerClient {

    Object createTest( String testClass ) throws Exception;

    Optional<String> startTest( String testClass );

    void stopTest( String testClass );

    void initialize();
}
