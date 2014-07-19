package com.athaydes.gradle.osgi

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import java.util.zip.ZipFile

/**
 *
 */
class IPojoPluginTest {

    def plugin = new IPojoPlugin()
    File inputBundle
    File outBundle
    Project mockProject

    void setupWith( IPojoConfig config, String inputBundlePath ) {
        mockProject = [ getBuildDir: { config.outDir } ] as Project
        inputBundle = inputBundlePath as File
        assert inputBundle.isFile()
        outBundle = new File( config.outDir as File, inputBundle.name )
        if ( outBundle.exists() ) {
            assert outBundle.delete()
        }
    }

    @Test
    void pluginCreatesIPojoExtension() {
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'ipojo'
        assert project.extensions.ipojo instanceof IPojoConfig
    }

    @Test
    void ipojoTaskCorrectlyModifiesOutputBundle_Annotations() {
        def config = new IPojoConfig( outDir: new File( 'build' ) )
        setupWith config, 'src/test/resources/no-ipojo.spell.checker.jar.1'

        plugin.ipojoTask( mockProject, inputBundle, config ).run()

        assert outBundle.exists()
        assertJarEntriesAreTheSame inputBundle, outBundle
        assertManifestRemainsTheSameExceptForIPojoEntry inputBundle, outBundle
    }

    @Test
    void ipojoTaskCorrectlyModifiesOutputBundle_XML() {
        def config = new IPojoConfig( outDir: new File( 'build' ), ignoreAnnotations: true )
        setupWith config, 'src/test/resources/no-ipojo.hello-client.jar.1'

        plugin.ipojoTask( mockProject, inputBundle, config ).run()

        assert outBundle.exists()
        assertJarEntriesAreTheSame inputBundle, outBundle
        assertManifestRemainsTheSameExceptForIPojoEntry inputBundle, outBundle
    }

    @Test( expected = GradleException )
    void failIfNoXMLAndAnnotationsIgnored() {
        def config = new IPojoConfig(outDir: new File( 'build' ),
                ignoreAnnotations: true, failIfNoIPojoComponents: true )

        // providing only annotations in the source but ignoring annotations
        setupWith config, 'src/test/resources/no-ipojo.spell.checker.jar.1'

        plugin.ipojoTask( mockProject, inputBundle, config ).run()
    }



    def assertJarEntriesAreTheSame( File input, File output ) {
        assert zipEntryNames( input ) == zipEntryNames( output )
    }

    def assertManifestRemainsTheSameExceptForIPojoEntry( File input, File output ) {
        def inputMap = ManifestReader.manifestAsMap( input )
        def outputMap = ManifestReader.manifestAsMap( output )
        assert inputMap[ 'iPOJO-Components' ] == null
        assert outputMap[ 'iPOJO-Components' ]
        outputMap.remove( 'iPOJO-Components' )
        assert inputMap.keySet() == outputMap.keySet() - 'iPOJO-Components'
    }

    def zipEntryNames( File file ) {
        def zip = new ZipFile( file )
        try {
            zip.entries().collect { it.name }.toSet()
        } finally {
            zip.close()
        }
    }

}
