package com.athaydes.gradle.osgi

import org.gradle.api.GradleException
import org.gradle.api.Project

import java.util.concurrent.atomic.AtomicBoolean
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
                        it ==~ /^(?i)main-class\s*:.*/
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
            def java = javaCmd()
            log.info "Java to be used to run OSGi: $java"

            def process = "$java ${config.javaArgs} -jar ${runnableJar.absolutePath} ${config.programArgs}"
                    .execute( [ ], config.outDirFile )

            delegateProcessTo( process )
        } else {
            throw new GradleException( 'OsgiRuntime does not contain any runnable jar! Cannot start the OSGi environment' )
        }

    }

    private void delegateProcessTo( Process process ) {
        def scanner = new Scanner( System.in )
        def exit = new AtomicBoolean( false )
        def line = null;

        consume process.in, exit, System.out
        consume process.err, exit, System.err

        while ( !exit.get() && ( line = scanner.nextLine()?.trim() ) != null ) {
            if ( line in [ 'exit', 'stop 0', 'shutdown', 'quit' ] ) {
                exit.set true
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
            exit.set true
        }
    }

    void consume( InputStream stream, AtomicBoolean exit, PrintStream writer ) {
        // REALLY low-level code necessary here to fix issue #1 (no output in Windows)
        Thread.startDaemon {
            byte[] bytes = new byte[64]
            while ( !exit.get() ) {
                def len = stream.read( bytes )
                if ( len > 0 ) writer.write bytes, 0, len
                else exit.set( true )
            }
        }
    }

    static String javaCmd() {
        def javaHome = System.getenv( 'JAVA_HOME' )
        if ( javaHome ) {
            def potentialJavas = [ "$javaHome/bin/java", "$javaHome/jre/bin/java" ]
                    .collect { it.replace( '//', '/' ).replace( '\\\\', '/' ) }
            for ( potentialJava in potentialJavas ) {
                if ( new File( potentialJava ).exists() ) {
                    return potentialJava
                }
            }
        }
        return 'java'
    }

}
