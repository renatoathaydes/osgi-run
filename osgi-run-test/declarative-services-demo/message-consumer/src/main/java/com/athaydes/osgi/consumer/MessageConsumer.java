package com.athaydes.osgi.consumer;

import com.athaydes.osgi.messaging.MessageBus;
import com.athaydes.osgi.messaging.MessageListener;
import com.athaydes.osgi.messaging.message.StartMessage;

public class MessageConsumer {

    public void addMessageBus( MessageBus messageBus ) {
        System.out.println( "MessageConsumer got a messageBus" );
        messageBus.listenTo( StartMessage.class, new MessageListener<StartMessage>() {
            @Override
            public void onMessage( StartMessage message ) {
                System.out.println( "MessageConsumer: received " + message );
            }
        } );
    }

    public void removeMessageBus( MessageBus messageBus ) {
        System.out.println( "MessageConsumer lost EventBus" );
    }

}
