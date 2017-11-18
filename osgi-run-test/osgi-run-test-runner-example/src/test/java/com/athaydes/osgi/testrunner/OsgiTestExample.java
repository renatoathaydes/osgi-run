package com.athaydes.osgi.testrunner;

import com.athaydes.gradle.osgi.testrun.OsgiRunJUnit4TestRunner;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith( OsgiRunJUnit4TestRunner.class )
public class OsgiTestExample {

    @Test
    public void canRunWithinOsgiContainer() {
        System.out.println( "I am in OSGi" );
        Assert.assertEquals( 2 + 2, 5 );
    }
}
