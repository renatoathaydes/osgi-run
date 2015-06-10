osgi-run
========

Osgi-Run - A Gradle plugin to make the development of modular applications using OSGi completely painless

## Quick Start

Given a Gradle project whose sub-projects are OSGi bundles:

*build.gradle*
```groovy
buildscript {
    repositories {
        jcenter()
    }
    dependencies {
        classpath "com.athaydes.gradle.osgi:osgi-run-core:1.2"
    }
}

apply plugin: 'osgi-run'

runOsgi {
  bundles += subprojects
}
```

From the project's root directory, type:

```
gradle runOsgi
```

This will create and run the OSGi environment during the Gradle build.

Alternatively, you can just create the OSGi environment, then run it later using the run scripts created by osgi-run:

```
gradle createOsgiRuntime
```

This will create an OSGi environment in the output directory, which by default is `build/osgi`.

To run it:

```
cd build/osgi
chmod +x run.sh  # may be necessary in Linux
./run.sh # In Windows, use run.bat, in Mac, use run.command
```

Once the framework starts, type ``lb`` (or ``ps``) to see all bundles installed and running.
To see a list of commands available, type ``help``.
Stop the OSGi framework by typing ``exit``, ``stop 0`` (stops the system bundle) or pressing `Ctrl+C`.

Notice that you can include any artifact, such as Maven dependencies, in your bundle environment.

For complete examples, see several examples further below or go straight to the samples in [osgi-run-test](osgi-run-test/).

### IPojo Plugin

If you use [IPojo](http://felix.apache.org/documentation/subprojects/apache-felix-ipojo.html),
you should definitely check out the [IPojo Plugin](ipojo-plugin), which complements the ``osgi-run`` plugin
to make it really easy to develop and run your OSGi/IPojo projects.

For examples of using IPojo and Gradle, see the test projects:

* [ipojo-example](osgi-run-test/ipojo-example) - annotation-based IPojo project
* [ipojo-xml-example](osgi-run-test/ipojo-xml-example) - XML-configured IPojo project
* [ipojo-dosgi](osgi-run-test/ipojo-dosgi) - Distributed OSGi with IPojo

## Tasks

  * ``createOsgiRuntime``: create the OSGi runtime based on configuration provided (or the defaults).
      This task depends on the ``jar`` task of the project and its sub-projects.
  * ``runOsgi``: starts the OSGi runtime (depends on ``createOsgiRuntime``).

## Extensions

  * ``runOsgi``: allows configuration of the plugin.
    It contains the following settable properties (all properties are optional):
    
    * ``configSettings``: String, one of ``['equinox', 'felix', 'none']`` (default ``"felix"``).
        This is used to generate a default config file for the OSGi container selected and affects the
        defaults used for most other properties. Always make this the first property you declare otherwise
        it may overwrite other properties with the default values for the container selected.
        Set to ``none`` if you want to provide your own config file.
    * ``outDir``: output directory (defaut: ``"osgi"``).
        Can be a String (relative to the project ``buildDir``) or a File (used as-is).
    * ``bundles``: Extra resources to include in the OSGi ``bundle`` folder 
        (defaults: in Felix: ``runOsgi.FELIX_GOGO_BUNDLES``, in Equinox: ``[]``).
        Each item can be anything accepted by ``Project.files(Object... paths)``.
    * ``osgiMain``: Main OSGi run-time 
        (defaults: in Felix: ``runOsgi.FELIX``, in Equinox: ``runOsgi.EQUINOX``).
        Accepts anything accepted by ``Project.files(Object... paths)``.
    * ``javaArgs``: String with arguments to be passed to the java process (default: ``""``).
    * ``bundlesPath``: String with path where the bundles should be copied to (default ``"bundle"``).
    * ``config``: Map of properties that should be added to the container's config file.
        This property is ignored if `configSettings` is set to 'none'.

The default `config` for Felix is:
        
```groovy
'felix.auto.deploy.action'  : 'install,start',
'felix.log.level'           : 1,
'org.osgi.service.http.port': 8080,
'obr.repository.url'        : 'http://felix.apache.org/obr/releases.xml'
```

The default `config` for Equinox is (notice `osgi.bundles` is set dynamically based on the `bundles` property:

```groovy
eclipse.ignoreApp : true,
osgi.noShutdown   : true,
osgi.bundles      : [bundle1-location@start,bundle2-location@start,...]
```
    
The following constants can be used to provide values for the above properties:
    
* ``FELIX``: the Apache Felix main jar. Can be used to set ``osgiMain``.
* ``FELIX_GOGO_BUNDLES``: the Felix Gogo bundles. Can be used with ``bundles``.
* ``EQUINOX``: The Eclipse Equinox main jar. Can be used to set ``osgiMain``.
* ``IPOJO_BUNDLE``: The IPojo bundle. Can be used with ``bundles``.
* ``IPOJO_ALL_BUNDLES``: The IPojo bundle plus IPojo Arch and command-line support bundles. Can be used with ``bundles``.

## Configurations

  * ``osgiMain``: same as the ``runOsgi.osgiMain`` property, but declaring this configuration in a project ``dependencies``
      overrides that property. It is preferrable to use that property over this configuration.
  * ``osgiRuntime``: same as the extension ``runOsgi.bundles`` property.
      Both the property and the configuration are applied.
      Notice that properties and configurations, by default, consider all transitive dependencies of the bundles/jars.
      However, any non-bundle (simple jar) transitive dependency is discarded from the OSGi runtime.
      If you do not want any transitive dependency of an artifact to be included in the OSGi runtime, you can do:
      
```groovy
dependencies {
    // all your usual dependencies
    ...
    
    osgiRuntime( "your:dependency:1.0" ) {
        transitive = false // transitive dependencies not included in OSGi runtime, even the ones that are OSGi bundles
    }
}
```

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

###### Including Apache Commons Logging into your OSGi environment

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

You might notice that Commons Logging is NOT an OSGi bundle.

Still, this works just fine (and you can actually try yourself in the [Installing non-bundles demo](osgi-run-test/installing-non-bundles))
because non-bundle jars will be wrapped into OSGi bundles automatically.

If you have experience with OSGi you might have thought that it could be difficult to use Commons Logging in OSGi.
Well, no more!

###### Using Groovy's SwingBuilder inside an OSGi environment

For yet another example, let's consider a bundle which uses Groovy to create a Swing UI.
Using Groovy's `SwingBuilder`, writing UIs is pretty easy! However, if you try to start your bundle, you will be greeted by
a really horrible error at runtime:

```
... 42 more    (too long to show the rest)
Caused by: java.lang.ClassNotFoundException: sun.reflect.ConstructorAccessorImpl
 not found by groovy-all [6]
        at org.apache.felix.framework.BundleWiringImpl.findClassOrResourceByDele
gation(BundleWiringImpl.java:1550)
        at org.apache.felix.framework.BundleWiringImpl.access$400(BundleWiringIm
pl.java:77)
...
```

Nothing is more annoying than these runtime ClassNotFoundException's you get in OSGi, especially when
the offending class is clearly part of the JRE!

For cases like this, there's an easy fix... Just add the package of the class that cannot be found to OSGi's
**extra system packages**:

```groovy
runOsgi {
    config += [ 'org.osgi.framework.system.packages.extra': 'sun.reflect' ]
}
```

Done! Now you can use the `SwingBuilder` without any concern.
And you can see an actual working demo in the [IPojo-DOSGi Demo](osgi-run-test/ipojo-dosgi), which includes a
`SwingBuilder`-created UI in bundle `code-runner-ui`.

#### Using Equinox as the OSGi container

Simplest possible Equinox setup:

```groovy
runOsgi {
  configSettings = 'equinox'
  javaArgs = '-console'
}
```

Notice that this will only start the Equinox Framework with the console enabled but no bundles deployed.
You can install bundles manually using the Equinox console.

But if you want to **deploy some bundles automatically** (your subprojects, for example) to your OSGi environment,
try something like this:

```groovy
runOsgi {
  configSettings = 'equinox'
  javaArgs = '-console'
  bundles = subprojects
}
```

This will deploy and start all your bundles (subprojects) when you run ``gradle runOsgi``.
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
  configSettings = 'equinox'
  javaArgs = '-console'
  osgiMain = "org.eclipse.osgi:org.eclipse.osgi:$equinoxVersion"
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
  configSettings = 'none'
  osgiMain = "your.knopflerfish:starter:7.1.2"
  bundles = [ "org.knopflerfish:framework:7.1.2" ]
}
```

### External Links

* [Apache Felix](http://felix.apache.org/)
* [Felix Gogo](http://felix.apache.org/documentation/subprojects/apache-felix-gogo.html)
* [Equinox Framework](http://www.eclipse.org/equinox)
* [Equinox Quickstart](http://www.eclipse.org/equinox/documents/quickstart-framework.php)
* [Equinox runtime options](http://help.eclipse.org/indigo/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)
* [Knopflerfish](http://www.knopflerfish.org/index.html)
* [D-OSGi Demo Walkthrough](http://cxf.apache.org/distributed-osgi-greeter-demo-walkthrough.html)
