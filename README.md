osgi-run
========

Osgi-Run - A Gradle plugin to make the development of modular applications using OSGi completely painless

## Quick Start

Given a Gradle project whose sub-projects are OSGi bundles:

*build.gradle*
```groovy
apply 'osgi-run'

runOsgi {
  bundles += subprojects
}
```

From the project's root directory, type:

```
gradle runOsgi
```

Once the framework starts, type ``lb`` to see all bundles installed and running.
Stop the OSGi framework by typing ``exit``.

The OSGi environment built by ``osgi-run`` will be located in the default ``outDir`` (see below).

For complete examples, see the [osgi-run-test](osgi-run-test/) projects.

### IPojo Plugin

If you use [IPojo](http://felix.apache.org/documentation/subprojects/apache-felix-ipojo.html),
you should definitely check out the [IPojo Plugin](ipojo-plugin), which complements the ``osgi-run`` plugin
to make it really easy to develop and run your OSGi/IPojo projects.

For examples of using IPojo and Gradle, see the test projects:

* [ipojo-example](osgi-run-test/ipojo-example) - annotation-based IPojo project
* [ipojo-xml-example](osgi-run-test/ipojo-xml-example) - XML-configured IPojo project

## Tasks

  * ``createOsgiRuntime``: create the OSGi runtime based on configuration provided (or the defaults).
      This task depends on the ``jar`` task of the project and its sub-projects.
  * ``runOsgi``: starts the OSGi runtime (depends on ``createOsgiRuntime``).

## Extensions

  * ``runOsgi``: allows configuration of the plugin.
    It contains the following mutable properties:
    
    * ``outDir``: output directory (defaut: ``"osgi"``).
        Can be a String (relative to the project ``buildDir``) or a File (used as-is).
    * ``bundles``: Extra resources to include in the OSGi ``bundle`` folder (default: ``runOsgi.FELIX_GOGO_BUNDLES``).
        Each item can be anything accepted by ``Project.files(Object... paths)``.
    * ``osgiMain``: Main OSGi run-time (default: ``runOsgi.FELIX``).
        Accepts anything accepted by ``Project.files(Object... paths)``.
    * ``javaArgs``: String with arguments to be passed to the java process (default: ``""``).
    * ``bundlesPath``: String with path where the bundles should be copied to (default ``"bundle"``).
    * ``configSettings``: String, one of ``['equinox', 'felix', 'none']`` (default ``"felix"``).
        This is used to generate a default config file for the OSGi container selected.
        Set to ``none`` if you want to provide your own config file.
    
    The following final properties can be used to provide values for the above properties:
    
    * ``FELIX``: the Apache Felix main jar. Can be used to set ``osgiMain``.
    * ``FELIX_GOGO_BUNDLES``: the Felix Gogo bundles. Can be used with ``bundles``.
    * ``EQUINOX``: The Eclipse Equinox main jar. Can be used to set ``osgiMain``.

## Configurations

  * ``osgiMain``: same as the ``runOsgi.osgiMain`` property, but declaring this configuration in a project ``dependencies``
      overrides that property. It is preferrable to use that property over this configuration.
  * ``osgiRuntime``: same as the extension ``runOsgi.bundles`` property.
      Both the property and the configuration are applied.

## Implicitly applied plugins

The ``osgi-run`` plugin applies the following plugins:

  * [OSGi plugin](http://www.gradle.org/docs/current/userguide/osgi_plugin.html)

## Using the osgi-run plugin

To use the ``osgi-run`` plugin you only need to apply it to your Gradle build:

```groovy
apply plugin: 'osgi-run'
```

You can immediately run your OSGi container using:

```groovy
gradle runOsgi
```

To see a list of installed bundles, type ``lb`` (or ``ss`` if using Equinox).


### Configuring the ``runOsgi`` extension

The best way to understand how you can configure your OSGi runtime is through examples.

Let's have a look at some common use-cases:

#### Use the Gradle project itself as a bundle

```groovy
runOsgi {
  bundles += project
}
```

As ``FELIX_GOGO_BUNDLES`` is the default value of ``bundles``, the above is equivalent to:

```groovy
runOsgi {
  bundles = FELIX_GOGO_BUNDLES + project
}
```

If you don't want the Gogo bundles installed, just use:

```groovy
runOsgi {
  bundles = [ project ]
}
```

#### Use Gradle sub-projects as bundles

```groovy
runOsgi {
  bundles += subprojects
}
```

#### Use artifacts as runtime bundles

```groovy
dependencies {
  // add all the Apache Felix Gogo bundles to the OSGi runtime
  osgiRuntime 'org.apache.felix:org.apache.felix.gogo.runtime:0.12.1'
  osgiRuntime 'org.apache.felix:org.apache.felix.gogo.shell:0.10.0'
  osgiRuntime 'org.apache.felix:org.apache.felix.gogo.command:0.14.0'
}
```

Notice that the above configuration is equivalent to setting ``osgiConfig.bundles`` to ``FELIX_GOGO_BUNDLES`` (which is the default).

##### Solving *unresolved constraint* errors

As another example, suppose you want to run the  [PDF Box library](http://pdfbox.apache.org) in an OSGi environment.
That seems pretty easy, as PDF Box jars are already OSGi bundles!
So you might expect that it should just work if you declare a dependency to it:

```groovy
dependencies {
    compile 'org.apache.pdfbox:pdfbox:1.8.6' // won't work
}
```

However, when you do ``gradle clean runOsgi``, you will find out it requires Apache Commons Logging at run-time:

```
(org.osgi.framework.BundleException: Unresolved constraint in bundle org.apache.pdfbox.fontbox [3]:
  Unable to resolve 3.0: missing requirement [3.0] osgi.wiring.package; (osgi.wiring.package=org.apache.commons.logging))
```

Luckily, that's easy to fix! Just add Commons Logging to the OSGi runtime:

```groovy
dependencies {
    compile 'org.apache.pdfbox:pdfbox:1.8.6'
    osgiRuntime 'commons-logging:commons-logging:1.2'
}
```

You might notice that Commongs Logging is NOT an OSGi bundle.

Still, this works just fine (and you can actually try yourself in the [Installing non-bundles demo](osgi-run-test/installing-non-bundles))
because non-bundle jars will be wrapped into OSGi bundles automatically.

If you have experience with OSGi you might have thought that it could be difficult to use Commons Logging in OSGi.
Well, no more!

#### Using Equinox as the OSGi container

Simplest possible Equinox setup:

```groovy
runOsgi {
  osgiMain = EQUINOX
  bundles = [] // do not use the Gogo bundles, just run the system bundle
  javaArgs = '-console'
  configSettings = 'equinox'
}
```

Notice that this will only start the Equinox Framework with the console enabled but no bundles deployed.
You can install bundles manually using the Equinox console.

But if you want to **deploy some bundles automatically** (your subprojects, for example) to your OSGi environment,
try something like this:

```groovy
runOsgi {
  osgiMain = EQUINOX
  bundles = subprojects
  javaArgs = '-console'
  configSettings = 'equinox'
  bundlesPath = 'plugins'
}
```

This will deploy and start all your bundles when you run ``gradle runOsgi``.
This is done through the ``configuration/config.ini`` file which is generated automatically by ``osgi-run``.
If you do not wish to use this behavior, just set ``configSettings`` to ``"none"`` and copy your own config file
to ``"${runOsgi.outDir}/<configFileLocation>"``.

#### Using a different version of Felix/Equinox or another OSGi container

If you want to declare exactly which version of Felix or Equinox (or you want to use some other OSGi container) you want
to use, you can set ``runOsgi.osgiMain`` to the artifact coordinates of the container.

##### Using an older/newer version of Apache Felix

```groovy
def felixVersion = '3.2.1' // or some other version

runOsgi {
  osgiMain = "org.apache.felix:org.apache.felix.main:$felixVersion"
}
```

##### Using an older/newer version of Equinox

```groovy
def equinoxVersion = '3.6.0.v20100517'

runOsgi {
  osgiMain = "org.eclipse.osgi:org.eclipse.osgi:$equinoxVersion"
  javaArgs = '-console'
  configSettings = 'equinox'
  bundlesPath = 'plugins'
}
```

##### Using another OSGi framework implementation

Just point to a runnable artifact which can start up your framework of choice, Knopflerfish, for example:

```groovy
repositories {
  maven {
    url 'http://www.knopflerfish.org/maven2'
  }
}

runOsgi {
  osgiMain = "your.knopflerfish:starter:7.1.2"
  bundles = [ "org.knopflerfish:framework:7.1.2" ]
  configSettings = 'none'
}
```

### External Links

* [Apache Felix](http://felix.apache.org/)
* [Felix Gogo](http://felix.apache.org/documentation/subprojects/apache-felix-gogo.html)
* [Equinox Framework](http://www.eclipse.org/equinox)
* [Equinox Quickstart](http://www.eclipse.org/equinox/documents/quickstart-framework.php)
* [Equinox runtime options](http://help.eclipse.org/indigo/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)
* [Knopflerfish](http://www.knopflerfish.org/index.html)

