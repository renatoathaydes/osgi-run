package com.athaydes.gradle.osgi

import groovy.transform.ToString
import org.gradle.api.logging.Logger
import org.gradle.api.logging.Logging

/**
 * Object representing the wrapInstructions block.
 */
@ToString( includeFields = true, includePackage = false )
class WrapInstructionsConfig {

    static final Logger log = Logging.getLogger( WrapInstructionsConfig )

    boolean enabled = true
    Map<Object, Map> manifests = [ : ]

    def manifest( regx, Closure config ) {
        manifests[ regx ] = [ : ]
        config.metaClass {
            instruction { String name, Object... args ->
                manifests[ regx ] << [ ( name ): args ]
            }
        }
        try {
            config.run()
        } catch ( e ) {
            log.warn "Problem running wrapInstructions config {}", e.toString()
        }
    }

}
