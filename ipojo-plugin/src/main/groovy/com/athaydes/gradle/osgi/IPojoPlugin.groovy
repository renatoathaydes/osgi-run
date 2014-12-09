package com.athaydes.gradle.osgi

import org.apache.felix.ipojo.manipulator.Pojoization
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

import java.util.zip.ZipFile

/**
 * A Gradle plugin that adds IPojo metadata to OSGi bundles.
 */
class IPojoPlugin implements Plugin<Project> {

    static final Logger log = Logging.getLogger( IPojoPlugin )

    static final defaultMetadataFiles = [ 'metadata.xml', 'META-INF/metadata.xml' ]

    @Override
    void apply( Project project ) {
        project.apply( plugin: 'java' )
        project.apply( plugin: 'osgi' )
        IPojoConfig config = project.extensions.create( 'ipojo', IPojoConfig )
        project.tasks.jar.doLast ipojoTask( project, project.tasks.jar.archivePath, config )
    }

    def ipojoTask( Project project, jarPath, IPojoConfig config ) {
        return {
            def pojo = createIPojo( config )
            File srcBundle = jarPath as File
            assert srcBundle.isFile()
            File outBundle = resolveOutBundle( srcBundle, project, config )
            def metadata = resolveMetadata( config.metadata, srcBundle )

            log.info "IPojo input bundle:  ${srcBundle.absolutePath}"
            log.info "IPojo output bundle: ${outBundle.absolutePath}"

            try {
                pojo.pojoization( srcBundle, outBundle, metadata as File, project.class.classLoader )
            } finally {
                metadata?.delete()
            }

            def pojoizedOk = confirmIPojoization( srcBundle, outBundle, config )

            if ( !config.outDir ) {
                log.info "Replacing OSGi bundle with IPojoized bundle at ${srcBundle.absolutePath}"
                srcBundle.newOutputStream() << outBundle.newInputStream()
            }
            if ( pojoizedOk ) {
                log.info "IPojoization - SUCCESS"
            }
        }
    }

    Pojoization createIPojo( IPojoConfig config ) {
        def pojo = new Pojoization( new LoggingReporter( log ) )
        if ( config.useLocalXSD ) {
            pojo.setUseLocalXSD()
        }
        if ( config.ignoreAnnotations ) {
            pojo.disableAnnotationProcessing()
        }
        return pojo
    }

    File resolveOutBundle( File srcBundle, Project project, IPojoConfig config ) {
        def outBundle = new File( config.outDir ?: project.buildDir, srcBundle.name )
        if ( !config.outDir ) {
            outBundle.deleteOnExit()
        }
        if ( !outBundle.parentFile.isDirectory() ) {
            assert outBundle.parentFile.mkdirs()
        }
        if ( outBundle.exists() ) {
            assert outBundle.delete()
        }
        assert outBundle.createNewFile()
        return outBundle
    }

    File resolveMetadata( metadata, File inputBundle ) {
        switch ( metadata ) {
            case null: return findMetadataIn( inputBundle )
            case String: return fileOrNull( metadata as File )
            case File: return fileOrNull( metadata as File )
            case Iterable:
                for ( candidate in metadata ) {
                    def file = fileOrNull( resolveMetadata( candidate, inputBundle ) )
                    if ( file ) return file
                }
        }
        return null
    }

    File findMetadataIn( File bundle ) {
        def zip = new ZipFile( bundle )
        try {
            for ( path in defaultMetadataFiles ) {
                def entry = zip.getEntry( path )
                if ( entry ) {
                    def tempFile = new File( "tmp-ipojo-${System.nanoTime()}.xml" )
                    tempFile.deleteOnExit()
                    tempFile << zip.getInputStream( entry )
                    return tempFile
                }
            }
        } finally {
            zip.close()
        }
        return null
    }

    boolean confirmIPojoization( File inputBundle, File outBundle, IPojoConfig config ) {
        def containsIPojoComponents = ManifestReader.manifestAsMap( outBundle ).containsKey( 'iPOJO-Components' )
        if ( !containsIPojoComponents ) {
            if ( config.failIfNoIPojoComponents ) {
                throw new GradleException( noIPojoComponentsError( inputBundle, config ) )
            } else {
                log.info( "The IPojo plugin has not detected any IPojo components in bundle [$inputBundle.absolutePath]" )
            }
        }
        return containsIPojoComponents
    }

    static final String noIPojoComponentsError( File inputBundle, IPojoConfig config ) {
        final suggestion = config.ignoreAnnotations ?
                '' : 'you either have IPojo annotations in your source or '
        """The IPojo Plugin could not find any IPojo metadata in the input bundle [${inputBundle.absolutePath}].
           |Please make sure that ${suggestion}there is an IPojo metadata file in one of these locations:
           |${config.metadata ?: "Bundle: ${defaultMetadataFiles}"}""".stripMargin()
    }

    File fileOrNull( File file ) {
        file?.exists() ? file : null
    }

}


class IPojoConfig {

    def outDir = null
    boolean useLocalXSD = false
    boolean ignoreAnnotations = false
    boolean failIfNoIPojoComponents = false
    def metadata = null

}