osgi-run
========

Osgi-Run - A Gradle plugin to make the development of modular applications using OSGi completely painless.

[ ![Download](https://api.bintray.com/packages/renatoathaydes/maven/osgi-run-core/images/download.svg) ](https://bintray.com/renatoathaydes/maven/osgi-run-core/_latestVersion)

### Features

* Create and run an OSGi environment using any container.
* Deploy the Gradle project's or sub-projects' bundles to the container easily. 
* Fetch dependencies using the standard Gradle way, from any
  [repository](https://docs.gradle.org/current/userguide/dependency_management.html#sec:repositories)
  supported by Gradle.
* Wrap into bundles any non-OSGi-ready dependencies automatically<sup>[1](#footnote-1)</sup>.
* Use dependencies as system libs, ie. let them live in the system classpath<sup>[2](#footnote-2)</sup>.

### What can I use osgi-run for?

Basically, to get your Gradle project (and sub-projects) bundles into an OSGi container
as part of the Gradle build, or just run existing bundles sourced from Gradle-compatible repositories.

Here's how it works:

#### If you already have your bundle(s) in a Gradle-compatible repository:

1. create a `build.gradle` file and apply the `osgi-run` plugin.
2. add your bundles to the project dependencies with the `osgiRuntime` type.
3. run `gradle createOsgi`.
4. Find your OSGi environment ready to run in the `build/osgi` directory.
   Start the OSGi container with `run.sh` or `run.bat`.

#### If you want to start a Gradle project from scratch:

1. create a Java [Gradle build](https://gradle.org/getting-started-gradle-java/).
2. specify `compile` dependencies (plain Java or OSGi libs) as usual,
   plus some `osgiRuntime` deps if you need some OSGi bundles at runtime.
3. add a Gradle plugin<sup>[3](#footnote-3)</sup> such as
   [org.dm.bundle](https://github.com/TomDmitriev/gradle-bundle-plugin) to turn your
   jar into a bundle.
4. add the `osgi-run` plugin to your build.
5. run `gradle createOsgi`.
6. Find your OSGi environment ready to run in the `build/osgi` directory.
   Start the OSGi container with `run.sh` or `run.bat`.

<sub>
<a name="footnote-1">1</a>: osgi-run uses [Bnd](http://www.aqute.biz/Bnd/Bnd) to wrap Gradle dependencies as
bundles if necessary before adding them to the OSGi runtime, including transitive dependencies, so using normal
flat jars becomes as easy as possible.
</sub>

<sub>
<a name="footnote-2">2</a>: If some of your dependencies assume a flat classpath like in regular Java and won't work any other way
(eg. loads classes at runtime, scans the classpath, uses JVM internals),
you can use still them as **system libs**, which are just jars added to the system classpath and visible from all bundles
(see the system libs section below).
</sub>

<sub>
<a name="footnote-3">3</a>: Other Gradle plugins that can be used to turn jars into bundles:

* [osgi plugin](https://docs.gradle.org/current/userguide/osgi_plugin.html)
* [biz.aQute.bnd plugin](https://github.com/bndtools/bnd/blob/master/biz.aQute.bnd.gradle)
</sub>

## osgi-run Tutorial and learning resources

To get started quickly, see the [Quick Start](#quick-start) section further below.

For a more advanced guide, check the [osgi-run tutorial](https://sites.google.com/a/athaydes.com/renato-athaydes/posts/osgi-runtutorial-runyourjavakotlinfregecodeinosgi).

Plenty of examples are available in the [osgi-run-test](osgi-run-test/) directory (all examples use the 'osgi' plugin,
except [build-with-subprojects](osgi-run-test/build-with-subprojects) which uses 'org.dm.bundle').

## Applying the osgi-run plugin

### Gradle 2.1+ and 3.0+

```groovy
plugins {
    id "com.athaydes.osgi-run" version "1.6.0"
}
```

### Older Gradle versions

```groovy
buildscript {
    repositories {
        mavenCentral()
    }
    dependencies {
        classpath "com.athaydes.gradle.osgi:osgi-run-core:1.6.0"
    }
}

apply plugin: 'com.athaydes.osgi-run'
```

## Quick Start

Given a Gradle project whose sub-projects are OSGi bundles, create an OSGi environment
containing the sub-projects' bundles, running it with Apache Felix and the default bundles:

*build.gradle*
```groovy
runOsgi {
  bundles += subprojects
}
```

Or if your OSGi environment consists of the Gradle project itself,
its compile-time dependencies, plus some existing bundle such as the
[Felix implementation](http://felix.apache.org/documentation/subprojects/apache-felix-config-admin.html) 
of the OSGi Config Admin Service:

```groovy
dependencies {
    compile group: 'org.osgi', name: 'org.osgi.enterprise', version: '5.0.0'
    osgiRuntime group: 'org.apache.felix', name: 'org.apache.felix.configadmin', version: '1.8.8'
}

runOsgi {
    javaArgs = "-Dexample.configFile=${file( 'config-example.properties' ).absolutePath}"
    bundles += project
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
chmod +x run.sh  # may be necessary in Linux/Mac
./run.sh # In Windows, use run.bat
```

Once the framework starts, type ``lb`` (or ``ps``) to see all bundles installed and running.
To see a list of commands available, type ``help``.
Stop the OSGi framework by typing ``exit``, ``stop 0`` (stops the system bundle) or pressing `Ctrl+C`.

Notice that you can include any artifact, such as Maven dependencies, in your bundle environment.

The default OSGi container is Apache Felix, but you can easily use Equinox and Knopflerfish as well.

For complete examples, continue reading the next sections or go straight to the samples in [osgi-run-test](osgi-run-test/).

### Declarative Services Plugin

If you use OSGi Declarative Services, you should have a look at the `osgi-ds` plugin, which is part of the
`osgi-run` core distribution.

Here's an example of how you can use it:

```groovy
apply plugin: 'com.athaydes.osgi-ds'

declarativeServices {
    declarations {
        component( name: 'classTrieMessageBus' ) {
            implementation( 'class': 'com.athaydes.osgi.ds.ClassTrieMessageBus' )
            service {
                provide( 'interface': 'com.athaydes.osgi.messaging.MessageBus' )
            }
        }
    }
}
```

For more information, have a look at the [DS Plugin Demo](osgi-run-test/declarative-services-demo).

### Handling start levels

If your OSGi bundles for any reason need to start in a defined order you can benefit from Start Levels
defined by OSGi Core specification. Currently, the implementation works for Equinox and Felix only.

There are 2 ways to define specific start levels for your bundles:

#### Start level for a dependency

Inside the `dependencies` block, you can use the `osgi` method as follows

```groovy
dependencies {
    osgiRuntime osgi(group: "some.group.id", name: "some-artifact-id", version: "version", startLevel: 3)
    
    // or using the shorter notation
    osgiRuntime osgi('some.group.id:some-artifact-id:version:3')
}
```

#### Start level for a bundle

If you specify your bundles using the `runOsgi.budles` property, you can use the following syntax:

```groovy
runOsgi {
    bundles = [
        [dependency: 'group:artifact:version', startLevel: 3],
        
        // or using the shorter notation
        [dependency: 'group:artifact:version:3'],
    ]
}
```

## Tasks

  * ``createBundlesDir``: create the bundles directory with all the configured bundles.
    This task depends on the ``jar`` task of the project and its sub-projects.
  * ``createOsgiRuntime``: create the OSGi runtime based on configuration provided (or the defaults).
    **This task depends on ``createBundlesDir`` and is the main task of this plugin.**  
  * ``runOsgi``: starts the OSGi runtime (depends on ``createOsgiRuntime``). Useful for debugging purposes.
  * ``cleanOsgiRuntime``: deletes the `outputDir` directory.
  
Notice that Gradle lets you write the shortest unambiguous task name possible, so instead of using the full name of
a task, say `createOsgiRuntime`, you can just do `gradle crOsgi` and Gradle will get it.

The `cleanOsgiRuntime` task will make any existing `clean` task (normally added by the Java plugin)
depend on itself, so you just need to type `gradle clean` to obliterate the OSGi runtime.

## Configuring osgi-run

`osgi-run` accepts the following configuration:

  * ``runOsgi``: allows configuration of the OSGi runtime.
    It contains the following settable properties (all properties are optional):
    
    * ``configSettings``: String, one of ``['equinox', 'felix', 'knopflerfish', 'none']`` (default ``"felix"``).
        This is used to generate a default config file for the OSGi container selected and affects the
        defaults used for most other properties. **Always make this the first property you declare** otherwise
        it will overwrite other properties with the default values for the container selected.
        Set to ``none`` if you want to provide your own config file.  
        You can configure several environments and select which to use by passing a Gradle property, e.g. `gradle runOsgi -Pequinox`.
        See the [build-with-subprojects](osgi-run-test/build-with-subprojects) example.
    * ``outDir``: output directory (default: ``"osgi"``).
        Can be a String (relative to the project ``buildDir``) or a File (used as-is).
    * ``bundles``: Bundles to include in the OSGi environment 
        (defaults: in Felix and Equinox: ``runOsgi.OSGIAAS_CLI_BUNDLES``, in Knopflerfish: ``[]``).
        Each item can be anything accepted by ``Project.files(Object... paths)``.
    * ``osgiMain``: Main OSGi run-time 
        (default: ``FELIX``, set to ``EQUINOX``, or ``KNOPFLERFISH`` depending on `configSettings`).
        Accepts anything accepted by ``Project.files(Object... paths)``.
    * ``javaArgs``: String with arguments to be passed to the java process (default: ``""``).
    * ``programArgs``: String with arguments to be passed to the main Java class (main args).
    * ``bundlesPath``: String with path where the bundles should be copied to 
      (default for Felix: ``"bundle"``, Equinox: ``"plugins"``, Knopflerfish: ``"jars""``).
    * ``config``: Map of properties that should be added to the container's config file.
        This property is ignored if `configSettings` is set to 'none'.
    * ``wrapInstructions``: instructions for wrapping non-bundles. See the relevant section below.
    * ``excludedBundles``: List of regular expressions to match against bundle file names
        which must not be added to the OSGi runtime. Defaults to `[ 'osgi\\..*', 'org\\.osgi\\..*' ]`.
    * ``copyManifestTo``: Copies the bundle's Manifest to the given location.
        This is useful to keep an up-to-date, auto-generated version of the Manifest in a location
        where the IDE can use it to provide OSGi support.

The default `config` for Felix is:

> If a `startLevel` is defined for any bundle, then the config file will
  list all bundles in the environment specifying the start-level as applicable, similarly to the Equinox config file.
     
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

The default `config` for Knopflerfish is (notice `bundle-1` and `fragment-1` are actually derived from
the `bundles` property):

```groovy
-Dorg.knopflerfish.framework.main.verbosity  =  0
-Forg.knopflerfish.framework.debug.resolver  =  false
-Forg.knopflerfish.framework.debug.errors  =  true
-Forg.knopflerfish.framework.debug.classloader  =  false
-Forg.osgi.framework.system.packages.extra  =  
-Forg.knopflerfish.startlevel.use  =  true
-init   
-launch   

-istart $bundle-1
-install $fragment-1
```

> Notice that to use Knopflerfish, you need to add its Maven Repository to your build file.
    
The following constants can be used to provide values for the above properties:
    
* ``FELIX``: the Apache Felix main jar. Can be used to set ``osgiMain``.
* ``FELIX_GOGO_BUNDLES``: the Felix Gogo bundles. Can be used with ``bundles``.
* ``EQUINOX``: The Eclipse Equinox main jar. Can be used to set ``osgiMain``.
* ``KNOPFLERFISH``: The Knopflerfish Framework jar. Can be used to set ``osgiMain``.
* ``OSGIAAS_CLI_BUNDLE``: The OSGiaaS-CLI bundle. Can be used with ``bundles``.
* ``OSGIAAS_CLI_BUNDLES``: The OSGiaaS-CLI bundle as well as the Felix SCR bundle, required for the CLI to start. Can be used with ``bundles``.

Here's an example setting most properties (notice that normally you won't need to set nearly as many):

```groovy
runOsgi {
    configSettings = 'equinox'            // use Equinox's config file instead of Felix's
    osgiMain = 'org.eclipse.osgi:org.eclipse.osgi:3.7.1' // use a specific version of Equinox
    javaArgs = '-DmyProp=someValue'       // pass some args to the Java process
    programArgs = '-console'              // pass some arguments to the Equinox starter
    bundles += allprojects.toList() + 'my:custom-bundle:1.0' // bundles are: this project + subprojects + custom bundle
    config += [ 'osgi.clean': true ]      // add properties to the Equinox config
    outDir = 'runtime'                    // the environment will be built at "${project.buildDir}/runtime"
    copyManifestTo file( 'auto-generated/MANIFEST.MF' ) // make the manifest visible to the IDE for OSGi support
}
```

### The syntax of the `bundles` property

The `bundles` property takes a List with items having the following formats:

* a `String` with the format of any Gradle dependency declaration.
* a `Project` as in `bundles = [project]` or `bundles = subProjects`.
* a `Map` with the following entries:
    * `dependency` (mandatory): String or Map dependency declaration.
    * `transitive` (optional, defaults to `true`): whether or not to include transitive dependencies.
    * `exclusions` (optional): transitive dependencies to exclude, specified as Map: `[group: 'xxx', module: 'xxx']`. 
    * `startLevel` (optional): start level for the bundle.

## Gradle configurations additions

`osgi-run` adds the following Gradle configurations to the project:

  * ``osgiMain``: same as the ``runOsgi.osgiMain`` property, but declaring this configuration in a project's
      ``dependencies`` overrides that property. 
      It is preferrable to use that property over this configuration.
  * ``osgiRuntime``: has the same purpose as the ``runOsgi.bundles`` property.
      Both the property and the configuration are applied.
      Notice that properties and configurations, by default, consider all transitive dependencies of the bundles/jars.
      Non-bundles (simple jar) are wrapped into OSGi bundles automatically by default.
      If you do not want any transitive dependency of an artifact to be included in the OSGi runtime, you can do:
  * ``systemLib``: system libs which should be added to the runtime not as bundles, but as simple jars in the system
      classpath. All system libs are excluded automatically from the bundle directory and export all their packages
      as system packages (using the `org.osgi.framework.system.packages.extra` config property).
      
```groovy
dependencies {
    // all your usual dependencies
    ...
    
    osgiRuntime( "your:dependency:1.0" ) {
        transitive = false // transitive dependencies not included in OSGi runtime
    }
}
```

### Wrapping non-bundles (flat jars)

If any of the artifacts you include in the OSGi environment are not OSGi bundles
(ie. they are flat jars which do not contain OSGi meta-data), they will be automatically
wrapped by `osgi-run` into OSGi bundles which export all of their contents.

This allows you to use any Java artifact whatsoever, so you are not limited to only OSGi bundles.

The actual wrapping is done by [Bnd](http://www.aqute.biz/Bnd/Bnd).

If you want to provide extra meta-data for ``Bnd`` to improve the wrapping results, you can use
`wrapInstructions` as follows:

```groovy
runOsgi {
    bundles += project

    wrapInstructions {
        // use regex to match file name of dependency
        manifest( "c3p0.*" ) {
            // import everything except the log4j package - should not be needed
            instruction 'Import-Package', '!org.apache.log4j', '*'
            instruction 'Bundle-Description', 'c3p0 is an easy-to-use library for making traditional ' +
                    'JDBC drivers "enterprise-ready" by augmenting them with functionality defined by ' +
                    'the jdbc3 spec and the optional extensions to jdbc2.'
        }
    }
}
```

The example above is used in the [quartz-sample](osgi-run-test/quartz-sample) 
to provide extra meta-data for wrapping the `c3p0` jar, which is required by the `Quartz` bundle.

### System Libs

If the library you want to use cannot work within the OSGi environment even as a wrapped bundle (as discussed above),
then you have only one option: add your jar to the system classpath by making it a system lib.

In short, system libs allow you to run some parts of your application in a regular Java environment (single classpath),
while others run inside the OSGi container, allowing you to benefit from both worlds.

You get the modular design and awesome features of OSGi where you want it, but can still benefit from any JVM component
whatsoever.

For example, if you want to add [Frege](https://github.com/Frege/frege) (Haskell in the JVM) scripting to your OSGi 
application, you'll find that [it's basically impossible](https://github.com/Frege/frege-interpreter/issues/31) 
because it uses its own complex classloader to load modules.

However, by turning the Frege REPL into a system lib, it will start just as if it were in a regular Java application:

```groovy
dependencies {
    systemLib 'org.frege-lang:frege-repl-core:1.2'
}
```

All your bundles will be able to import and use the packages provided by Frege's regular jars. 

See the [frege-as-system-lib](osgi-run-test/frege-as-system-lib) sample for a working example.

## More usage examples

The best way to understand how you can configure your OSGi runtime is through examples.

Let's have a look at some common use-cases:

#### Use your Gradle project itself as a bundle

```groovy
runOsgi {
  bundles += project
}
```

As ``OSGIAAS_CLI_BUNDLES`` is the default value of ``bundles``, the above is equivalent to:

```groovy
runOsgi {
  bundles = OSGIAAS_CLI_BUNDLES + project
}
```

If you don't want the default bundles installed, just use:

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

#### Use Maven artifacts as runtime bundles

```groovy
dependencies {
  // add all the Apache Felix Gogo bundles to the OSGi runtime
  osgiRuntime 'org.apache.felix:org.apache.felix.gogo.runtime:0.12.1'
  osgiRuntime 'org.apache.felix:org.apache.felix.gogo.shell:0.10.0'
  osgiRuntime 'org.apache.felix:org.apache.felix.gogo.command:0.14.0'
}
```

##### Solving *unresolved constraint* errors

###### Including Apache Commons Logging 1.1.1 into your OSGi environment

As another example, suppose you want to run the  [PDF Box library](http://pdfbox.apache.org) in an OSGi environment.
That seems pretty easy, as PDF Box jars are already OSGi bundles!
So you might expect that it should just work if you declare a dependency to it:

```groovy
dependencies {
    compile 'org.apache.pdfbox:pdfbox:1.8.6' // won't work
}
```

However, when you do ``gradle clean runOsgi``, you will find out it requires Apache Commons Logging at run-time
(which will be automatically wrapped as a bundle by `run-osgi`), but this jar itself requires other things:

```
(org.osgi.framework.BundleException: Unresolved constraint in bundle org.apache.pdfbox.fontbox [2]: 
  Unable to resolve 2.0: missing requirement [2.0] osgi.wiring.package; 
  (osgi.wiring.package=org.apache.commons.logging) 
  [caused by: Unable to resolve 1.0: missing requirement [1.0] osgi.wiring.package;
  (osgi.wiring.package=javax.servlet)])
```

To understand why `osgi-run` could not figure out we needed not only commons-logging, but also some bundle to
provide `javax.servlet`, let's ask Gradle to show us the dependencies of our module:

```
compile - Compile classpath for source set 'main'.
+--- org.osgi:org.osgi.core:4.3.1
\--- org.apache.pdfbox:pdfbox:1.8.6
     +--- org.apache.pdfbox:fontbox:1.8.6
     |    \--- commons-logging:commons-logging:1.1.1
     +--- org.apache.pdfbox:jempbox:1.8.6
     \--- commons-logging:commons-logging:1.1.1
```

As you can see, it looks as if the commons-logging dependency had no dependencies at all. So `osgi-run`
has no way of knowing there are other needs for commons-logging to work.

Inspecting the POM file of commons-logging reveals what's going on... it declares several optional dependencies:

```xml
<dependency>
  <groupId>log4j</groupId>
  <artifactId>log4j</artifactId>
  <version>1.2.12</version>
  <optional>true</optional>
</dependency>
<dependency>
  <groupId>logkit</groupId>
  <artifactId>logkit</artifactId>
  <version>1.0.1</version>
  <optional>true</optional>
</dependency>
<dependency>
  <groupId>avalon-framework</groupId>
  <artifactId>avalon-framework</artifactId>
  <version>4.1.3</version>
  <optional>true</optional>
</dependency>
<dependency>
  <groupId>javax.servlet</groupId>
  <artifactId>servlet-api</artifactId>
  <version>2.3</version>
  <scope>provided</scope>
  <optional>true</optional>
</dependency>
```

So this is one case where we need to add a little meta-data by hand.

But that's easy, knowing that we probably won't be needing servlets or the Avalon Framework and the other Apache
dependencies, we can simply tell `osgi-run` that these packages should not be imported at all:

```groovy
runOsgi {
    bundles += project

    wrapInstructions {
        manifest( /commons-logging.*/ ) {
            instruction 'Import-Package', '!javax.servlet,!org.apache.*,*'
        }
    }
}
```

The wrap instruction tells `run-osgi` that the commons-logging bundle should not import the packages 
`javax.servlet` and `org.apache.*` (notice that you may use wildcards), but should import everything else that is
 required when wrapped into an OSGi bundle. 

A working project demonstrating this can be found in the [Installing non-bundles demo](osgi-run-test/installing-non-bundles)).

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

TODO rewrite `IPojo-DOSGi Demo` without IPojo as IPojo has not been maintained for many years.

Done! Now you can use the `SwingBuilder` without any concern.
And you can see an actual working demo in the [IPojo-DOSGi Demo](osgi-run-test/ipojo-dosgi), which includes a
`SwingBuilder`-created UI in bundle `code-runner-ui`.

#### Using Equinox as the OSGi container

Simplest possible Equinox setup:

```groovy
runOsgi {
  configSettings = 'equinox'
}
```

Notice that this will only start the Equinox Framework with the default bundles deployed.
You can install bundles manually using the [OSGiaaS-CLI](https://github.com/renatoathaydes/osgiaas/blob/master/docs/cli/README.md)
(which is currently used by both Felix and Equinox).

But if you want to **deploy some bundles automatically** (your subprojects, for example) to your OSGi environment,
try something like this:

```groovy
runOsgi {
  configSettings = 'equinox'
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
  programArgs = '-console'
  osgiMain = "org.eclipse.osgi:org.eclipse.osgi:$equinoxVersion"
  bundles = [] // do not use the default bundles, older Equinox has its own console
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
  osgiMain = "org.knopflerfish:framework:7.1.2"
  bundles = subprojects // your bundles 
}
```

### External Links

* [Apache Felix](http://felix.apache.org/)
* [OSGiaaS-CLI](https://github.com/renatoathaydes/osgiaas/blob/master/docs/cli/README.md)
* [Felix Gogo](http://felix.apache.org/documentation/subprojects/apache-felix-gogo.html)
* [Equinox Framework](http://www.eclipse.org/equinox)
* [Equinox Quickstart](http://www.eclipse.org/equinox/documents/quickstart-framework.php)
* [Equinox runtime options](http://help.eclipse.org/indigo/index.jsp?topic=/org.eclipse.platform.doc.isv/reference/misc/runtime-options.html)
* [Knopflerfish](http://www.knopflerfish.org/index.html)
* [D-OSGi Demo Walkthrough](http://cxf.apache.org/distributed-osgi-greeter-demo-walkthrough.html)
