package com.athaydes.osgi.testrunner.example;

import org.osgi.framework.BundleContext;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.Deactivate;

@Component(service = ExampleForTestRunner.class)
public class ExampleForTestRunner {

    private String symName;

    @Activate
    public void start(BundleContext context) throws Exception {
        symName = context.getBundle().getSymbolicName();
    }

    @Deactivate
    public void stop(BundleContext context) throws Exception {
        symName = null;
    }

    public String sayHiFromBundle(String name) {
        return "Hi " + name + " from bundle " + symName;
    }

}
