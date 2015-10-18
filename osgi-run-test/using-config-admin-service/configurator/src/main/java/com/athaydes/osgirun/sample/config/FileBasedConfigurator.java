package com.athaydes.osgirun.sample.config;

import org.osgi.framework.*;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.*;
import java.util.Hashtable;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

public class FileBasedConfigurator implements BundleActivator {

    private volatile WatchKey configFileWatchKey = null;
    private final AtomicBoolean serviceRegistered = new AtomicBoolean( false );

    @Override
    public void start( final BundleContext context ) throws Exception {
        System.out.println( "Started configurator bundle" );

        context.addServiceListener( new ServiceListener() {
            @Override
            public void serviceChanged( ServiceEvent event ) {
                if ( event.getType() == ServiceEvent.REGISTERED ) {
                    if ( serviceRegistered.compareAndSet( false, true ) ) {
                        System.out.println( "Got ConfigAdmin service by listener notification" );
                        ServiceReference configAdminRef = event.getServiceReference();
                        ConfigurationAdmin admin = ( ConfigurationAdmin ) context.getService( configAdminRef );
                        notifyServicesOnConfigChange( admin );
                    }
                }
            }
        }, String.format( "(objectClass=%s)", ConfigurationAdmin.class.getName() ) );

        ServiceReference<ConfigurationAdmin> configAdminRef = context.getServiceReference( ConfigurationAdmin.class );
        if ( configAdminRef != null && serviceRegistered.compareAndSet( false, true ) ) {
            System.out.println( "Got ConfigAdmin service directly from the context" );
            ConfigurationAdmin admin = context.getService( configAdminRef );
            notifyServicesOnConfigChange( admin );
        }
    }

    private void notifyServicesOnConfigChange( ConfigurationAdmin admin ) {
        final Configuration config;
        try {
            config = admin.getConfiguration( "example.config", null );
        } catch ( IOException e ) {
            e.printStackTrace();
            return;
        }

        String configFilePath = System.getProperty( "example.configFile" );
        if ( configFilePath != null ) {
            File configFile = new File( configFilePath );
            if ( configFile.exists() ) {
                onConfigFileChange( configFile.toPath(), config );
            } else {
                System.out.println( "Config file does not exist: " + configFilePath );
            }
            watchConfigFile( configFile, new OnFileChange() {
                @Override
                public void onChange( Path path ) {
                    onConfigFileChange( path, config );
                }
            } );
        } else {
            System.out.println( "example.configFile property not set" );
        }
    }

    private void onConfigFileChange( Path configFile, Configuration config ) {
        Properties properties = new Properties();
        try {
            properties.load( new FileInputStream( configFile.toFile() ) );
            System.out.println( "Loaded properties: " + properties );
            config.update( stringToObjectHashtable( properties ) );
        } catch ( IOException e ) {
            e.printStackTrace();
        }
    }

    private void watchConfigFile( final File configFile, final OnFileChange onFileChange ) {
        System.out.println( "Watching file " + configFile );
        final Path toWatch = Paths.get( configFile.getParent() );

        final WatchService watcher;
        try {
            watcher = FileSystems.getDefault().newWatchService();
        } catch ( IOException e ) {
            e.printStackTrace();
            return;
        }

        if ( configFileWatchKey != null ) {
            System.out.println( "WatchKey already set! Can't start fileWatcher" );
            return;
        }

        try {
            configFileWatchKey = toWatch.register( watcher,
                    ENTRY_MODIFY, ENTRY_CREATE );
        } catch ( IOException e ) {
            e.printStackTrace();
            return;
        }

        // start the file watcher thread below
        Runnable fileWatcher = new Runnable() {
            @Override
            public void run() {

                try {
                    for (; ; ) {
                        System.out.println( "Taking key" );
                        configFileWatchKey = watcher.take();
                        System.out.println( "Got watch key " + configFileWatchKey );

                        // we have a polled event, now we traverse it and
                        // receive all the states from it
                        for (WatchEvent event : configFileWatchKey.pollEvents()) {
                            System.out.printf( "Received %s event for file: %s\n",
                                    event.kind(), event.context() );

                            if ( event.kind() == StandardWatchEventKinds.OVERFLOW ) {
                                continue; // overflow may happen even if we did not subscribe to it
                            }

                            Path fileName = ( Path ) event.context();
                            if ( fileName.toFile().getName().equals( configFile.getName() ) ) {
                                System.out.println( "Config file changed" );
                                onFileChange.onChange( toWatch.resolve( fileName ) );
                            }
                        }
                        boolean stillValid = configFileWatchKey.reset();
                        if ( !stillValid ) {
                            System.out.println( "Breaking out of fileWatcher loop - key no longer valid" );
                            break;
                        }
                    }
                } catch ( InterruptedException e ) {
                    e.printStackTrace();
                }
            }
        };
        Thread th = new Thread( fileWatcher, "FileWatcher" );
        th.start();
    }

    private static Hashtable<String, ?> stringToObjectHashtable( Properties properties ) {
        Hashtable<String, Object> result = new Hashtable<>();
        for (Map.Entry<?, Object> property : properties.entrySet()) {
            result.put( property.getKey().toString(), property.getValue() );
        }
        return result;
    }

    @Override
    public void stop( BundleContext context ) throws Exception {
        System.out.println( "Stopping file watcher" );
        if ( configFileWatchKey != null ) {
            configFileWatchKey.cancel();
            configFileWatchKey = null;
        }
    }


    private interface OnFileChange {
        void onChange( Path path );
    }
}
