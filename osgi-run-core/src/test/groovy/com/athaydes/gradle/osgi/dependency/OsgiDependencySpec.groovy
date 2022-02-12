package com.athaydes.gradle.osgi.dependency

import spock.lang.Specification
import spock.lang.Unroll

class OsgiDependencySpec extends Specification {

    @Unroll
    def "OSGiDependency can be specified via String"() {
        when: 'A dependency is created via a single String'
        def dep = new DefaultOSGiDependency( spec )

        then: 'The dependency has the correct properties'
        dep.group == group
        dep.name == name
        dep.version == version
        dep.targetConfiguration == config
        dep.startLevel == startLevel

        where:
        spec              | group | name | version | config    | startLevel
        'a:b:c'           | 'a'   | 'b'  | 'c'     | null      | null
        'a:b:1.0'         | 'a'   | 'b'  | '1.0'   | null      | null
        'a:b:1.0:compile' | 'a'   | 'b'  | '1.0'   | 'compile' | null
        'a:b:1.0:test:2'  | 'a'   | 'b'  | '1.0'   | 'test'    | 2
        'a:b:1.0:2'       | 'a'   | 'b'  | '1.0'   | null      | 2
        'a:b:1.0:2:3'     | 'a'   | 'b'  | '1.0'   | '2'       | 3

    }

}
