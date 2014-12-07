# IPojo D-OSGi Demo

## What it is

This demo has two parts: a server (code-runner-server) which exposes a OSGi remote service
( [CompositeCodeRunner](code-runner-api/src/main/java/ipojo/example/code/CompositeCodeRunner.java) ),
 and a client User Interface (code-runner-ui) which consumes the service.

Both the service consumer and implementation's Java code have no idea that the service is remote (ie.
not running locally in the same JVM process). The differentiation is only made in the IPojo
[server config file](code-runner-server/src/main/resources/metadata.xml) and the
[Client UI's remote-services file](code-runner-ui/src/main/resources/OSGI-INF/remote-service/remote-services.xml).

The server-side makes use of [CodeRunner](code-runner-api/src/main/java/ipojo/example/code/CodeRunner.java)
implementation services provided by local bundles (like groovy-2-3-3-runner) to provide particular language
interpreters.

## Getting started

To run this sample, clone the code with git:

```
git clone git@github.com:renatoathaydes/osgi-run.git
```

Enter this demo directory:

```
cd osgi-run-test/ipojo-dosgi
```

Follow the instructions below.

### Running the server

In the project root folder, type:

`../gradlew clean runOsgi`

You can verify the server is running by checking the automatically-generated WSDL file with your browser at:

[http://localhost:8080/coderunner?wsdl](http://localhost:8080/coderunner?wsdl)

### Running the client

In the project root folder, type:

`../gradlew runOsgi -Pclient`

> Do not run the `clean` task if the server is already running in the same directory

The UI should appear in a few seconds. You can then run scripts in the server, in whatever languages are available
in the server.

**Notice this is a toy project to show how to use IPojo and D-OSGi work. It is not safe to run code in your server
without sandboxing it first**.

## Testing the server with SoapUI

1. Open [SoapUI](http://soapui.org/downloads).
2. Select *File > New SOAP Project*.
3. Enter **http://localhost:8080/coderunner?wsdl** in *Initial WSDL*.
4. Select both *Create Requests* and *Create TestSuite* checkboxes and hit "OK".

Now you just need to add new test steps to the auto-generated TestSuites/TestCases to make assertions
to ensure the results you get from the server are correct.

## Changing or checking the OSGi environment at run-time

Apache Felix's Gogo bundles are installed in the OSGi environment so you can control/inspect the OSGi
runtime from the command-line.

To see the installed bundles, after starting the OSGi runtime, type:

``ps``

To see more commands, type:

``help``
