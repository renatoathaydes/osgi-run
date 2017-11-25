package com.athaydes.osgi.testrunner.exampletests;

import com.athaydes.gradle.osgi.testrun.OsgiRunJUnit4TestRunner;
import com.athaydes.osgi.testrunner.example.ExampleForTestRunner;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

@RunWith( OsgiRunJUnit4TestRunner.class )
public class OsgiServiceExampleTest {

    private final ExampleForTestRunner exampleForTestRunnerService;

    /**
     * Constructor can require any number of OSGi services.
     *
     * @param exampleForTestRunnerService OSGi service exported by the bundle being tested
     */
    public OsgiServiceExampleTest( ExampleForTestRunner exampleForTestRunnerService ) {
        this.exampleForTestRunnerService = exampleForTestRunnerService;
    }

    @Test
    public void testOsgiService() {
        String helloMessage = exampleForTestRunnerService.sayHiFromBundle( "Joe" );
        assertThat( helloMessage, equalTo( "Hi Joe from bundle com.athaydes.gradle.osgi.run-test-runner-example" ) );
    }

}
