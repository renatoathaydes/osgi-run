package com.athaydes.gradle.osgi

import org.apache.tools.ant.taskdefs.condition.Os
import org.gradle.api.Project

import java.util.concurrent.atomic.AtomicBoolean

import static com.athaydes.gradle.osgi.OsgiRuntimeTaskCreator.createJavaRunArgs
import static com.athaydes.gradle.osgi.OsgiRuntimeTaskCreator.getTarget
import static com.athaydes.gradle.osgi.OsgiRuntimeTaskCreator.selectMainClass

class OsgiRunner {

    static log = OsgiRunPlugin.log

    void run( Project project, OsgiConfig config ) {
        log.info "Running project ${project.name}"

        def mainClass = selectMainClass( project )
        String target = getTarget( project, config )

        def separator = Os.isFamily( Os.FAMILY_WINDOWS ) ? ';' : ':'

        def javaArgs = createJavaRunArgs( target, config, mainClass, separator )

        def command = "${javaCmd()} ${javaArgs}"

        log.info "Running command: ${command}"

        def process = command.execute( ( List ) null, config.outDirFile )

        delegateProcessTo( process )
    }

    private void delegateProcessTo( Process process ) {
        def exit = new AtomicBoolean( false )
        def line = null;

        consume process.in, exit, System.out
        consume process.err, exit, System.err

        def input = System.in.newReader()

        def readAtLeastOneLine = false

        while ( !exit.get() && ( line = input.readLine()?.trim() ) != null ) {
            if ( line in [ 'exit', 'stop 0', 'shutdown', 'quit' ] ) {
                exit.set true
                line = 'stop 0'
            }
            readAtLeastOneLine = true

            process.outputStream.write( ( line + '\n' ).bytes )
            process.outputStream.flush()
        }

        if ( readAtLeastOneLine ) {
            try {
                process.waitForOrKill( 5000 )
            } catch ( e ) {
                log.warn "OSGi process did not die gracefully. $e"
            } finally {
                exit.set true
            }
        } else {
            def stars = '*' * 50
            println "$stars\n" +
                    "The osgi-run process does not have access to the\n" +
                    "JVM console (may happen when running from an IDE).\n" +
                    "For this reason, the command-line will not work.\n" +
                    "Blocking until the OSGi process is killed.\n" +
                    "$stars"

            process.waitFor()
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
        def javaHome = System.getenv( 'JAVA_HOME' ) ?: System.getProperty( 'java.home' )
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
