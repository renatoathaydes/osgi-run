package com.athaydes.osgitest

import org.gradle.testkit.runner.GradleRunner
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class OsgiTestTest {

    @Rule
    public TemporaryFolder testProjectDir = new TemporaryFolder()
    File buildFile
    List<File> pluginClasspath

    @Before
    void setup() {
        buildFile = testProjectDir.newFile( 'build.gradle' )

        def pluginClasspathResource = getClass().classLoader.findResource( "plugin-classpath.txt" )
        if ( pluginClasspathResource == null ) {
            throw new IllegalStateException( "Did not find plugin classpath resource, run `testClasses` build task." )
        }

        pluginClasspath = pluginClasspathResource.readLines().collect { new File( it ) }
    }

    @Test
    void "can run the test task"() {
        buildFile << """
            plugins {
                id 'com.athaydes.osgi-test'
            }
        """

        when:
        def result = GradleRunner.create()
                .withProjectDir( testProjectDir.root )
                .withArguments( 'testOsgi' )
                .withPluginClasspath( pluginClasspath )
                .build()

        then:
        assert result.output.contains( "TEST" )
        result.task( ":testOsgi" ).outcome == SUCCESS
    }

}
