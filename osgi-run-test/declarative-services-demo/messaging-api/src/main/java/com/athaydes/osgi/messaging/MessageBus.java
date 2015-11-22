package com.athaydes.osgi.messaging;

public interface MessageBus {

    void publish( Message message );

    <M extends Message> void listenTo( Class<M> messageType,
                                       MessageListener<? extends M> listener );

}
