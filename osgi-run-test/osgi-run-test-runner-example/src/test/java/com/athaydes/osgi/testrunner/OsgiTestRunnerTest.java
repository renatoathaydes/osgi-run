package com.athaydes.osgi.testrunner;

import com.athaydes.osgi.gradle.testrun.comm.OsgiRunTestRunnerSettings;
import com.athaydes.osgi.gradle.testrun.comm.RemoteOsgiTestRunner;
import com.athaydes.protobuf.tcp.api.RemoteServices;
import org.junit.Test;

public class OsgiTestRunnerTest {

    @Test
    public void canContactTestRunner() throws Exception {
        RemoteOsgiTestRunner client = RemoteServices.createClient(RemoteOsgiTestRunner.class, "127.0.0.1",
                OsgiRunTestRunnerSettings.getRemoteTestRunnerPort());

        client.startTest("hello");
    }
}
