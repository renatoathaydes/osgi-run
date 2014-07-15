package com.athaydes.gradle.osgi

import groovy.transform.ToString

/**
 * The configuration used by the {@code osgi-run} plugin to create and run OSGi environments.
 */
@ToString( includeFields = true, includeNames = true )
class OsgiConfig {
    protected File outDirFile
    def outDir = "osgi"
    def bundles = [ ] + FELIX_GOGO_BUNDLES
    def osgiMain = FELIX
    String javaArgs = ""
    String bundlesPath = 'bundle'
    String configSettings = 'felix'

    static final String FELIX = 'org.apache.felix:org.apache.felix.main:4.4.0'

    static final String EQUINOX = 'org.eclipse.osgi:org.eclipse.osgi:3.7.1'

    static final FELIX_GOGO_BUNDLES = [
            'org.apache.felix:org.apache.felix.gogo.runtime:0.12.1',
            'org.apache.felix:org.apache.felix.gogo.shell:0.10.0',
            'org.apache.felix:org.apache.felix.gogo.command:0.14.0',
    ].asImmutable()

}
