# Gradle IPojo Plugin

The Gradle IPojo Plugin IPojoizes your jars, making it really easy to benefit from [IPojo](http://felix.apache.org/documentation/subprojects/apache-felix-ipojo.html)
in any Gradle project.

## How to use

Add to your ``build.gradle`` file:

```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath "com.athaydes.gradle.osgi:ipojo-plugin:1.0"
    }
}

apply plugin: 'ipojo'
```

Now, when the ``jar`` task runs, the IPojo Plugin will add IPojo metadata to the jars automatically.

> Gradle 2.0+ is required to use this plugin (due to the asm-all dependency, which is shared with Gradle)

## Configuring

IPojo annotations and XML configuration are supported.

### outDir

If you do not want the plugin to overwrite your common jar,
you may set the ``ipojo.outDir`` property to let the IPojo plugin
know where it should save your ipojoized bundle:

*build.gradle*

```groovy
ipojo {
  outDir = 'your-ipojo-directory/location'
}
```

### ignoreAnnotations

If set to true, the IPojo manipulator skips annotations processing
(can reduce significantly the processing time on large bundles).

*build.gradle*

```groovy
ipojo {
  ignoreAnnotations = true
}
```

### useLocalXSD

If set to true, the IPojo manipulator will not look at remote XSD resources to validate metadata.

*build.gradle*

```groovy
ipojo {
  useLocalXSD = true
}
```

### metadata

The location of the XML metadata file.

If not set, the IPojo plugin will look inside the bundle, in the following locations (the first existing one will be used):

* metadata.xml
* META-INF/metadata.xml

This property can be set using the following types:

* ``String``: path to the metadata file (relative to the project root).
* ``File``: location of the metadata file.
* ``Iterable<String | File>``: the first existing path will be used.

Examples:

* Use the file called ``ipojo.xml`` located at the project root folder.

```groovy
ipojo {
  metadata = 'ipojo.xml'
}
```

* Try to use ``config/metadata.xml`` (using Gradle's ``file`` method).
  If it does not exist, use ``WEB-INF/metadata.xml``.

```groovy
ipojo {
  metadata = [file('config/metadata.xml'), 'WEB-INF/metadata.xml']
}
```

### failIfNoIPojoComponents

If set to true, the build will fail if no IPojo components have been detected in the input bundle.
If not set (or set to false), only a INFO-level message will be displayed.

This option is useful if you want to ensure your metadata has been used as expected, but notice that many API bundles
can be used with IPojo without any metadata at all.

*build.gradle*

```groovy
ipojo {
  failIfNoIPojoComponents = true
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

