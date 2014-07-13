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

#### Using Equinox as the OSGi container

```groovy
runOsgi {
  osgiMain = equinox
  bundles = [] // do not use the Gogo bundles
}
```

#### Include dependencies as runtime bundles

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

Non-bundle jars will be wrapped into OSGi bundles automatically, with their version set to ``0.0.0``.

### External Links

* [Apache Felix](http://felix.apache.org/)
* [Felix Gogo](http://felix.apache.org/documentation/subprojects/apache-felix-gogo.html)
* [Equinox Framework](http://www.eclipse.org/equinox)
* [Equinox Quickstart](http://www.eclipse.org/equinox/documents/quickstart-framework.php)

