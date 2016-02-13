package com.athaydes.gradle.osgi

import com.athaydes.gradle.osgi.util.JarUtils
import org.gradle.api.Project
import org.gradle.api.tasks.bundling.Jar

import java.util.zip.ZipEntry
import java.util.zip.ZipFile

class ManifestFileCopier {

    static log = OsgiRunPlugin.log

    static void run( Project project, OsgiConfig config ) {
        if ( config.copyManifestTo ) {
            log.info( "Copying manifests to {}", config.copyManifestTo )
            def jarsByBundle = [ : ] as Map<Project, Set>
            config.bundles.each { bundle ->
                log.debug "Checking if bundle '{}' is a project", bundle
                if ( bundle instanceof Project ) {
                    bundle.tasks.withType( Jar ) { jarTask ->
                        if ( !jarsByBundle.containsKey( bundle ) ) {
                            jarsByBundle[ bundle as Project ] = [ ] as Set
                        }
                        jarsByBundle[ bundle as Project ].addAll jarTask.outputs.files.toSet()
                    }
                }
            }

            log.info( "Will write manifests for the following bundles: {}", jarsByBundle )

            jarsByBundle.each { Project bundle, Set jars ->
                if ( jars.size() == 1 ) {
                    copyManifest jars.first(), bundle.file( config.copyManifestTo )
                } else jars.indexed().each { index, jar ->
                    def indexedOutput = bundle.file( "${config.copyManifestTo}_$index" )
                    copyManifest jar, indexedOutput
                }
            }
        } else {
            log.info( "Not copying any manifest as 'copyManifestTo' property was not set" )
        }
    }

    private static void copyManifest( jar, File output ) {
        output.parentFile?.mkdirs()
        JarUtils.withManifestEntry( jar, { ZipFile zip, ZipEntry entry ->
            def manifestStream = zip.getInputStream( entry )
            output.withOutputStream { writer ->
                writer.write manifestStream.bytes
            }
        } )
    }

}
