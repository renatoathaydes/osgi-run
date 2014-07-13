osgi-run
========

Osgi-Run - A Gradle plugin to make the development of modular applications using OSGi completely painless

## Tasks

  * ``createOsgiRuntime``: create the OSGi runtime based on configuration provided (or the defaults).
      This task depends on the ``jar`` task of the project and its sub-projects.
  * ``runOsgi``: starts the OSGi runtime (depends on ``createOsgiRuntime``).

## Extensions

  * ``runOsgi``: allows configuration of the plugin using the following properties:
    * ``outDir``: output directory (defaut: ``osgi``).
        Can be a String (relative to the project ``buildDir``) or a File (used as-is).
    * ``bundles``: Extra resources to include in the OSGi ``bundle`` folder (default: ``[]``).
        Each item can be anything accepted by ``Project.files(Object... paths)``.
    * ``osgiMain``: Main OSGi run-time (default: ``org.apache.felix:org.apache.felix.main:4.4.0``).
        Accepts anything accepted by ``Project.files(Object... paths)``.

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

By default, the resulting OSGi runtime will use [Apache Felix](http://felix.apache.org/) as a container
and the [Felix Gogo](http://felix.apache.org/documentation/subprojects/apache-felix-gogo.html) bundles will be installed.

To configure the behavior of this plugin, you have two options:

### Using the ``runOsgi`` extension

For example, if you want to use Equinox rather than Felix as your OSGi container, you may add this to your build file:

```groovy
runOsgi {
  osgiMain = equinox
  bundles = []
}
```

