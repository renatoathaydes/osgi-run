### build-with-subprojects

This demo project shows how to build Gradle projects with subprojects where each subproject is a OSGi bundle.

The subprojects are made OSGi bundles by applying the `org.dm.bundle` plugin.

To run the top-project with an environment containing all subprojects:

```
../gradlew clean :build-with-subprojects:runOsgi
```

This will start a Felix container with all subprojects installed and started.

To use Equinox instead:

```
../gradlew clean :build-with-subprojects:runOsgi -Pequinox
```

You can also start an OSGi environment with each subproject individually.

For example, to start only the `my-api` bundle:

```
gradlew clean :build:with-subprojects:my-api:runOsgi
```
