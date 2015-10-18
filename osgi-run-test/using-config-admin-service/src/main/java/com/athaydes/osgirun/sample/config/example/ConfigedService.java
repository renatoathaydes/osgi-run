package com.athaydes.osgirun.sample.config.example;

import org.osgi.service.cm.ConfigurationException;
import org.osgi.service.cm.ManagedService;

import java.util.Dictionary;

public class ConfigedService implements ManagedService {

    @Override
    public void updated( Dictionary<String, ?> properties ) throws ConfigurationException {
        System.out.println( "Updating service with " + properties );
    }
}
