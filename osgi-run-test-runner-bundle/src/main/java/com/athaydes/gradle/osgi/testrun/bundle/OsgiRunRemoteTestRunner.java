package com.athaydes.gradle.osgi.testrun.bundle;

import com.athaydes.osgi.gradle.testrun.comm.OsgiRunTestRunnerSettings;
import com.athaydes.osgi.gradle.testrun.comm.RemoteOsgiTestRunner;
import com.athaydes.osgi.rsa.provider.protobuf.api.RemoteServices;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;

public class OsgiRunRemoteTestRunner implements RemoteOsgiTestRunner, BundleActivator {

    private static final Logger log = LoggerFactory.getLogger(OsgiRunRemoteTestRunner.class);

    private final AtomicReference<Closeable> testRunner = new AtomicReference<>();
    private final AtomicReference<BundleContext> bundleContext = new AtomicReference<>();
    private final Map<String, Closeable> testServices = new ConcurrentHashMap<>(2);

    @Override
    public void start(BundleContext context) throws Exception {
        log.debug("Starting {}", getClass().getName());
        bundleContext.set(context);
        testRunner.set(RemoteServices.provideService(this,
                OsgiRunTestRunnerSettings.getRemoteTestRunnerPort(),
                RemoteOsgiTestRunner.class));
    }

    @Override
    public void stop(BundleContext context) throws Exception {
        log.debug("Stopping {}", getClass().getName());
        bundleContext.set(null);
        closeService(testRunner.get());
        testServices.values().forEach(OsgiRunRemoteTestRunner::closeService);
        testServices.clear();
    }

    @Override
    public String startTest(String testClass) {
        log.debug("Starting test service: {}", testClass);
        try {
            Class<?> testType = getClass().getClassLoader().loadClass(testClass);
            // TODO allow service to get services by injection
            startServiceFor(testType.newInstance());
        } catch (ClassNotFoundException e) {
            log.warn("Attempted to run non-existing test class", e);
            return e.toString();
        } catch (IllegalAccessException | InstantiationException e) {
            log.warn("Unable to instantiate test class", e);
            return e.toString();
        }

        return "";
    }

    private void startServiceFor(Object testInstance) {
        Closeable testService = RemoteServices.provideService(testInstance,
                OsgiRunTestRunnerSettings.getNextRemoteTestServicePort());
        testServices.put(testInstance.getClass().getName(), testService);
    }

    @Override
    public void stopTest(String testClass) {
        log.debug("Stopping test service: {}", testClass);
        closeService(testServices.remove(testClass));

        // stop the framework
        try {
            bundleContext.get().getBundle(0L).stop();
        } catch (BundleException e) {
            log.warn("Error stopping framework", e);
        }
    }

    private static void closeService(Closeable service) {
        Optional.ofNullable(service).ifPresent(server -> {
            try {
                server.close();
                log.debug("Closed service: {}", server.getClass().getName());
            } catch (IOException e) {
                log.warn("Problem closing service of type " +
                        server.getClass().getName(), e);
            }
        });
    }

}
