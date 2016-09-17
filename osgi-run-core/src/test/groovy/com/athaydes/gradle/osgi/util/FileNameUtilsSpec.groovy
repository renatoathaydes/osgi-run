package com.athaydes.gradle.osgi.util

import spock.lang.Specification
import spock.lang.Unroll

class FileNameUtilsSpec extends Specification {

    @Unroll
    def "Can extract version from file name"() {
        when: 'Extracting a version from a file name'
        def version = FileNameUtils.versionFrom( name )

        then: 'The correct version is extracted'
        version == expectedVersion

        where:
        name                          | expectedVersion
        'hi.jar'                      | '1.0.0'
        'badFile'                     | '1.0.0'
        'lib-1.0.jar'                 | '1.0'
        'aopalliance-1.0.jar'         | '1.0'
        'lib-5.2.3.jar'               | '5.2.3'
        'aop-6.12.jar'                | '6.12'
        'commons-logging-1.1.1.jar'   | '1.1.1'
        'crazy-lib-name-0.1-SNAP.jar' | '0.1-SNAP'
    }

    @Unroll
    def "Can extract title from file name"() {
        when: 'Extracting a title from a file name'
        def title = FileNameUtils.titleFrom( name )

        then: 'The title version is extracted'
        title == expectedTitle

        where:
        name                          | expectedTitle
        'hi.jar'                      | 'hi'
        'badFile'                     | 'badFile'
        'lib-1.0.jar'                 | 'lib'
        'aopalliance-1.0.jar'         | 'aopalliance'
        'lib-5.2.3.jar'               | 'lib'
        'aop-6.12.jar'                | 'aop'
        'commons-logging-1.1.1.jar'   | 'commons-logging'
        'crazy-lib-name-0.1-SNAP.jar' | 'crazy-lib-name'
    }

}
