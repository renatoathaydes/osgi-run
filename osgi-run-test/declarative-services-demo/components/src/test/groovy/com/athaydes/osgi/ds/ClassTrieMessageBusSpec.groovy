package com.athaydes.osgi.ds

import com.athaydes.osgi.messaging.MessageBus
import com.athaydes.osgi.messaging.message.ErrorMessage
import com.athaydes.osgi.messaging.message.StartMessage
import com.athaydes.osgi.messaging.message.StopMessage
import spock.lang.Specification
import spock.lang.Subject

class ClassTrieMessageBusSpec extends Specification {

    @Subject
    MessageBus messageBus = new ClassTrieMessageBus()

    def "Events are published and received by listeners following type hierarchy"() {
        given: 'A number of listeners are added to a ClassTrieMessageBus'
        def receivedMessages = [ : ]
        listenerTypes.each { type ->
            messageBus.listenTo( type ) { message ->
                receivedMessages.get( type, [ ] ) << message
            }
        }

        when: 'Some message(s) are published through the MessageBus'
        messages.each { messageBus.publish( it.newInstance() ) }

        then: 'Each listener receives the messages it subscribed to'
        receivedMessages.collectEntries { type, list ->
            [ ( type ): list.size() ]
        } == expectedMessageCountByType

        where:
        listenerTypes                  | messages                      | expectedMessageCountByType
        [ StartMessage ]               | [ StartMessage ]              | [ ( StartMessage ): 1 ]
        [ StartMessage, StopMessage ]  | [ StartMessage ]              | [ ( StartMessage ): 1 ]
        [ StartMessage, StartMessage ] | [ StartMessage ]              | [ ( StartMessage ): 2 ]
        [ StartMessage, StopMessage,
          StartMessage, StopMessage,
          ErrorMessage, ErrorMessage ] | [ StartMessage, StopMessage ] |
                [ ( StartMessage ): 2, ( StopMessage ): 2 ]
    }

}
