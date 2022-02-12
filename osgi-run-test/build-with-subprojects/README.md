### build-with-subprojects

This demo project shows how to build Gradle projects with subprojects where each subproject is a OSGi bundle.

The subprojects are made OSGi bundles by applying the `biz.aQute.bnd.builder` plugin.

To run the top-project with an environment containing all subprojects:

```
../gradlew clean :build-with-subprojects:runOsgi
```

This will start a Felix container with all subprojects installed and started.

To use Equinox instead:

```
../gradlew clean :build-with-subprojects:runOsgi -Pequinox
```

Example interaction (from this directory):

```shell
▶ ../../gradlew createOsgi    # create the OSGi environment
...
▶ chmod +x dist/osgi/run.sh   # on Linux/MacOS, make the run script executable
▶ dist/osgi/run.sh            # start the OSGi container
Started MyConsumer
Got service ref null
Detected registration of a Service!
Got service with message: This is a MyService implementation of class com.athaydes.myimpl.MyServiceImpl
Exported MyService implementation
____________________________
Welcome to Apache Felix Gogo

g! lb                                                                                                                                                                                             21:41:10
START LEVEL 10
   ID|State      |Level|Name
    0|Active     |    0|System Bundle (7.0.3)|7.0.3
    1|Active     |    2|Apache Commons Lang (3.3.2)|3.3.2
    2|Active     |    4|my-consumer (2.0.0)|2.0.0
    3|Active     |    4|Apache Felix Gogo Runtime (1.1.4)|1.1.4
    4|Active     |    4|my-api (2.0.0)|2.0.0
    5|Active     |    4|Apache Felix Gogo Command (1.1.2)|1.1.2
    6|Active     |    4|JLine Bundle (3.21.0)|3.21.0
    7|Active     |    4|Groovy Runtime (4.0.0)|4.0.0
    8|Active     |    4|Apache Felix Gogo JLine Shell (1.1.8)|1.1.8
    9|Active     |    4|my-impl (2.0.0)|2.0.0

g! stop 9                                                                                                                                                                                         21:41:24

g! start 9                                                                                                                                                                                        21:41:35
Detected registration of a Service!
Got service with message: This is a MyService implementation of class com.athaydes.myimpl.MyServiceImpl
Exported MyService implementation

g!
```
