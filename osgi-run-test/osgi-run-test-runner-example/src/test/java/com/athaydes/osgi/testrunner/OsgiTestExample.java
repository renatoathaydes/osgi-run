package com.athaydes.osgi.testrunner;

import com.athaydes.osgi.gradle.testrun.OsgiRunTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(OsgiRunTestRunner.class)
public class OsgiTestExample {

    @Test
    public void canRunWithinOsgiContainer() {
        System.out.println("I am in OSGi");
    }
}
