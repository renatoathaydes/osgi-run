package com.athaydes.osgi.producer;

import com.athaydes.osgi.messaging.MessageBus;
import com.athaydes.osgi.messaging.message.StartMessage;

import java.util.Timer;
import java.util.TimerTask;

public class MessageProducer {

    private volatile MessageBus messageBus;

    public void setMessageBus( MessageBus messageBus ) {
        System.out.println( "Got a messageBus" );
        this.messageBus = messageBus;
        produceMessages();
    }

    public void removeMessageBus( MessageBus messageBus ) {
        System.out.println( "Lost message bus" );
        this.messageBus = null;
    }

    private void produceMessages() {
        final Timer timer = new Timer( "message-producer-timer", true );

        timer.scheduleAtFixedRate( new TimerTask() {
            @Override
            public void run() {
                MessageBus bus = messageBus;
                if ( bus != null ) {
                    bus.publish( new StartMessage() );
                } else {
                    timer.cancel();
                }
            }
        }, 0L, 5000L );
    }

}
