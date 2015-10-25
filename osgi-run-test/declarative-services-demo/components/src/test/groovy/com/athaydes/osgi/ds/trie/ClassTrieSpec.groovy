package com.athaydes.osgi.ds.trie

import spock.lang.Specification
import spock.lang.Subject
import spock.lang.Unroll

import javax.swing.*
import java.awt.*

@Unroll
class ClassTrieSpec extends Specification {

    @Subject
    def trie = new ClassTrie<Integer>()

    def "Can put and retrieve items on a ClassTrie"() {
        given: 'A Trie with some items'
        items.each { type, values -> values.each { v -> trie.put( type, v ) } }

        when: 'We try to get the items using example queries'
        def result = queries.collect { trie.get( it ) }

        then: 'The correct items are returned'
        result == expected

        where:
        items                                                       | queries                                   | expected
        [ : ]                                                       | [ Object ]                                | [ [ ] ]
        [ : ]                                                       | [ Integer ]                               | [ [ ] ]
        [ ( Object ): [ 0 ] ]                                       | [ Object, Integer, int ]                  | [ [ 0 ], [ ], [ ] ]
        [ ( Integer ): [ 4 ] ]                                      | [ Object, Integer, int ]                  | [ [ 4 ], [ 4 ], [ ] ]
        [ ( char ): [ 2, 3 ] ]                                      | [ Object, char, int ]                     | [ [ ], [ 2, 3 ], [ ] ]
        [ ( char ): [ 1 ], ( byte ): [ 2 ], ( Byte ): [ 3 ] ]       | [ char, int, Object, byte, Byte ]         | [ [ 1 ], [ ], [ 3 ], [ 2 ], [ 3 ] ]
        [ ( JComponent ): [ 1 ], ( String ): [ 2 ] ]                | [ JComponent, Container, String, JPanel ] | [ [ 1 ], [ 1 ], [ 2 ], [ ] ]
        [ ( Number ): [ 1 ], ( Integer ): [ 2 ], ( Float ): [ 3 ] ] | [ Object, Number, Float, Integer ]        | [ [ 1, 2, 3 ], [ 1, 2, 3 ], [ 3 ], [ 2 ] ]
    }

}
