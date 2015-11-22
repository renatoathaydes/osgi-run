package com.athaydes.osgi.ds;

import com.athaydes.osgi.ds.trie.ClassTrie;
import com.athaydes.osgi.messaging.Message;
import com.athaydes.osgi.messaging.MessageBus;
import com.athaydes.osgi.messaging.MessageListener;

/**
 * Event bus.
 */
public class ClassTrieMessageBus implements MessageBus {

    private final ClassTrie<MessageListener> listeners = new ClassTrie<>();

    public ClassTrieMessageBus() {
        System.out.println( "Hello Event Bus" );
    }

    @Override
    public void publish( Message message ) {
        for (MessageListener listener : listeners.get( message.getClass() )) {
            //noinspection unchecked
            listener.onMessage( message );
        }
    }

    @Override
    public <M extends Message> void listenTo( Class<M> messageType,
                                              MessageListener<? extends M> listener ) {
        listeners.put( messageType, listener );
    }
}
