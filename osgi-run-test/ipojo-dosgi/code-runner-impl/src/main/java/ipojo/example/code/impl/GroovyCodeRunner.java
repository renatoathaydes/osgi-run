package ipojo.example.code.impl;

import ipojo.example.code.CodeRunner;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Provides;

@Component(propagation = true, name = "GroovyCodeRunner")
@Provides
public class GroovyCodeRunner implements CodeRunner {

    @Override
    public Object runScript(String script) {
        return "Hello from Groovy! " + script;
    }

}
