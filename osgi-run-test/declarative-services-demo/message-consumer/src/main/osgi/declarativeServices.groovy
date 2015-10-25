component( name: 'consumer' ) {
    implementation( 'class': 'com.athaydes.osgi.consumer.MessageConsumer' )
    reference( name: 'message-bus',
            'interface': 'com.athaydes.osgi.messaging.MessageBus',
            'cardinality': '1..1',
            'policy': 'static',
            'bind': 'addMessageBus',
            'unbind': 'removeMessageBus' )
}
