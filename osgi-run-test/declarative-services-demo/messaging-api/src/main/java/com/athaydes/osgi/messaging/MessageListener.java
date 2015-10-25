package com.athaydes.osgi.messaging;

public interface MessageListener<M extends Message> {

    void onMessage( M message );

}
