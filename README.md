osgi-run
========

Osgi-Run - A Gradle plugin to make the development of modular applications using OSGi completely painless

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

#### Use Gradle sub-projects as bundles

```groovy
runOsgi {
  bundles = FELIX_GOGO_BUNDLES + subprojects
}
```

#### Use dependencies as runtime bundles

```groovy
dependencies {
  // add all the Apache Felix Gogo bundles to the OSGi runtime
  osgiRuntime 'org.apache.felix:org.apache.felix.gogo.runtime:0.12.1'
  osgiRuntime 'org.apache.felix:org.apache.felix.gogo.shell:0.10.0'
  osgiRuntime 'org.apache.felix:org.apache.felix.gogo.command:0.14.0'
}
```

```groovy
dependencies {
  osgiRuntime 'org.apache.commons:commons-lang3:3.3.2'
}
```

Non-bundle jars will be wrapped into OSGi bundles automatically by Felix, with their version set to ``0.0.0``.

#### Using Equinox as the OSGi container

Simplest possible Equinox setup:

```groovy
runOsgi {
  osgiMain = EQUINOX
  bundles = [] // do not use the Gogo bundles
  javaArgs = '-console'
  configSettings = 'equinox'
}
```

Notice that this will only start the Equinox Framework with the console enabled but no bundles deployed.

If you want to **deploy some bundles automatically** (your subprojects, for example) to your OSGi environment,
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

##### Using an older version of Apache Felix

```groovy
runOsgi {
  osgiMain = "org.apache.felix:org.apache.felix.main:3.2.1"
}
```

##### Using an older version of Equinox

```groovy
runOsgi {
  osgiMain = "org.eclipse.osgi:org.eclipse.osgi:3.6.0.v20100517"
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

