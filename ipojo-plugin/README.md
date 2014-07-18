# Gradle IPojo Plugin

The Gradle IPojo Plugin IPojoizes your jars, making it really easy to use [IPojo](http://felix.apache.org/documentation/subprojects/apache-felix-ipojo.html).

## How to use

Add to your ``build.gradle`` file:

```groovy
buildscript {
  repositories {
    // this will be available in JCenter shortly!
    //jcenter() 
    // if you build this project locally and install it using the Maven plugin
    mavenLocal()
  }
  dependencies {
    classpath group: 'com.athaydes.gradle.osgi', name: 'ipojo-plugin', version: '1.0-SNAPSHOT'
  }
}

apply plugin: 'ipojo'
```

Now, when the ``jar`` task runs, the IPojo Plugin will add IPojo metadata to the jars automatically.


> Currently, it only works with annotation-based configuration (XML is not yet supported).


## Configuring

If you do not want to overwrite your common jar, you may set where you want the IPojo plugin
to save your ipojoized bundle by adding this to your ``build.gradle`` file:

```groovy
ipojo {
  outDir = 'your-ipojo-directory/location'
}
```

## Implicitly applied plugins

The IPojo Plugin applies the following plugins:

  * [OSGi plugin](http://www.gradle.org/docs/current/userguide/osgi_plugin.html)
  * [Java plugin](http://www.gradle.org/docs/current/userguide/tutorial_java_projects.html)
  
## Running your IPojo bundles

Use the [Osgi-run Plugin](https://github.com/renatoathaydes/osgi-run) to create an OSGi environment and automatically install and start all your bundles,
including the IPojo bundles.

## Usage Examples

See the [ipojo-example](https://github.com/renatoathaydes/osgi-run/tree/master/osgi-run-test/ipojo-example),
which contains the code shown on the [IPojo Maven Tutorial](http://felix.apache.org/documentation/subprojects/apache-felix-ipojo/apache-felix-ipojo-gettingstarted/ipojo-hello-word-maven-based-tutorial.html).

