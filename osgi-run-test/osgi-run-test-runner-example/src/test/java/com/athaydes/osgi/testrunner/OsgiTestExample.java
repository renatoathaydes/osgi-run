package com.athaydes.osgi.testrunner;

import com.athaydes.gradle.osgi.testrun.OsgiRunJUnit4TestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith( OsgiRunJUnit4TestRunner.class )
public class OsgiTestExample {

    @Test
    public void canRunWithinOsgiContainer() {
        System.err.println( "I am in OSGi" );
        assertEquals( 2 + 2, 4 );
    }

    @Test
    public void anotherOsgiTest() {
        System.err.println( "I am also in OSGi" );
        assertEquals( "Hello".toUpperCase(), "HELLO" );
    }

}
