## Declarative Services demo

This sample project shows how to configure declarative services components using the different methods
available:

* embedded in the Gradle build script: See [message-producer](message-producer/build.gradle) sample. 
* as a separate Groovy script: See [message-consumer](message-consumer/build.gradle) sample. 
* as a separate, standard DS XML file: See [components](components/build.gradle) sample. 


## Config embedded in the Gradle build

Example:

**build.gradle**

```groovy
declarativeServices {
    declarations {
        component( name: 'classTrieMessageBus' ) {
            implementation( 'class': 'com.athaydes.osgi.ds.ClassTrieMessageBus' )
            service {
                provide( 'interface': 'com.athaydes.osgi.messaging.MessageBus' )
            }
        }
    }
    show()
}
```

## Config in a separate Groovy script

Example:

**build.gradle**

```groovy
declarativeServices {
    declarations = project.file( 'src/main/osgi/declarativeServices.groovy' )
}
```

**declarativeServices.groovy**

```groovy
component( name: 'consumer' ) {
    implementation( 'class': 'com.athaydes.osgi.consumer.MessageConsumer' )
    reference( name: 'message-bus',
            'interface': 'com.athaydes.osgi.messaging.MessageBus',
            'cardinality': '1..1',
            'policy': 'static',
            'bind': 'addMessageBus',
            'unbind': 'removeMessageBus' )
}
```

## Config in a standard Declarative Services XML file

Example:

**build.gradle**

```groovy
declarativeServices {
    declarations = project.file( 'src/main/osgi/components.xml' )
}
```

**components.xml**

```xml
<?xml version="1.0" encoding="UTF-8"?>

<component name='producer' immediate="true">
    <implementation class='com.athaydes.osgi.producer.MessageProducer'/>
    <reference name="message-bus" interface="com.athaydes.osgi.messaging.MessageBus"
               cardinality="0..1" policy="dynamic"
               bind="setMessageBus" unbind="removeMessageBus"/>
</component>
```
