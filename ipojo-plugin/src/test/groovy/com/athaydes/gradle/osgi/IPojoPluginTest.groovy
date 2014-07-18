package com.athaydes.gradle.osgi

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Test

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

/**
 *
 */
class IPojoPluginTest {

    @Test
    void pluginCreatesIPojoExtension() {
        def project = ProjectBuilder.builder().build()
        project.apply plugin: 'ipojo'
        assert project.extensions.ipojo instanceof IPojoConfig
    }

    @Test
    void ipojoTaskCorrectlyModifiesOutputBundle() {
        def plugin = new IPojoPlugin()
        def mockProject = [ getBuildDir: { new File( 'build' ) } ] as Project
        def config = new IPojoConfig( outDir: mockProject.buildDir )
        def inputBundle = 'src/test/resources/no-ipojo.spell.checker.jar.1' as File
        assert inputBundle.isFile()
        def outBundle = new File( mockProject.buildDir, 'no-ipojo.spell.checker.jar.1' )
        if ( outBundle.exists() ) {
            assert outBundle.delete()
        }

        plugin.ipojoTask( mockProject, inputBundle, config ).run()

        assert outBundle.exists()
        assertJarEntriesAreTheSame inputBundle, outBundle
        assertManifestRemainsTheSameExceptForIPojoEntry inputBundle, outBundle
    }

    def assertJarEntriesAreTheSame( File input, File output ) {
        assert zipEntryNames( input ) == zipEntryNames( output )
    }

    def assertManifestRemainsTheSameExceptForIPojoEntry( File input, File output ) {
        def inputMap = manifestAsMap( input )
        def outputMap = manifestAsMap( output )
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

    def manifestAsMap( File file ) {
        def extractKey = { String line -> line[ 0..<line.indexOf( ':' ) ].trim() }
        def extractValue = { String line -> line[ ( line.indexOf( ':' ) + 1 )..-1 ].trim() }
        def zip = new ZipFile( file )
        try {
            ZipEntry entry = zip.entries().find { 'META-INF/MANIFEST.MF' }
            def lines = zip.getInputStream( entry ).readLines()
            def map = [ : ]
            def prevKey = null
            for ( line in lines ) {
                if ( line.startsWith( ' ' ) ) {
                    def currentValue = map[ prevKey ]
                    map[ prevKey ] = currentValue + line.trim()
                } else if ( !line.trim().empty ) {
                    map[ prevKey = extractKey( line ) ] = extractValue( line )
                }
            }
            return map
        } finally {
            zip.close()
        }
    }

}
