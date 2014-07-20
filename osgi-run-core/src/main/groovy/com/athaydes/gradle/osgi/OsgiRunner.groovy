package com.athaydes.gradle.osgi

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.zip.ZipFile

class OsgiRunner {

    static log = OsgiRunPlugin.log

    def isRunnableJar = { File file ->
        if ( file.name.endsWith( '.jar' ) ) {
            def zip = new ZipFile( file )
            try {
                def manifest = zip.getEntry( 'META-INF/MANIFEST.MF' )
                if ( manifest ) {
                    return zip.getInputStream( manifest ).readLines().any {
                        it.trim().startsWith( 'Main-Class' )
                    }
                }
            } finally {
                zip.close()
            }
        }
        return false
    }

    void run( Project project, OsgiConfig config ) {
        log.info "Running project ${project.name}"
        def runnableJar = config.outDirFile.listFiles().find( isRunnableJar )
        if ( runnableJar ) {
            log.debug "Running executable jar: ${runnableJar}"
            def process = "java -jar ${runnableJar.absolutePath} ${config.javaArgs}".execute( [ ], config.outDirFile )
            delegateProcessTo( process )
        } else {
            throw new GradleException( 'OsgiRuntime does not contain any runnable jar! Cannot start the OSGi environment' )
        }

    }

    private void delegateProcessTo( Process process ) {
        def scanner = new Scanner( System.in )
        def exit = false
        def line = null;

        // REALLY low-level code necessary here to fix issue #1 (no output in Windows)
        Thread.startDaemon {
            byte[] bytes = new byte[64]
            while (!exit) {
                def len= process.in.read( bytes )
                if (len > 0) print new String(bytes[0..<len] as byte[])
                else exit = true
            }
        }

        while ( !exit && ( line = scanner.nextLine()?.trim() ) != null ) {
            if ( line in [ 'exit', 'stop 0', 'shutdown', 'quit' ] ) {
                exit = true
                line = 'stop 0'
            }
            process.outputStream.write( ( line + '\n' ).bytes )
            process.outputStream.flush()
        }

        try {
            process.waitForOrKill( 5000 )
        } catch ( e ) {
            log.warn "OSGi process did not die gracefully. $e"
        } finally {
            exit = true
        }
    }

}
