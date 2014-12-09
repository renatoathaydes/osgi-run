package ipojo.example.code.javascript;

import ipojo.example.code.CodeRunner;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

@Component( propagation = true, name = "javascript-runner" )
@Instantiate
@Provides
public class JavaScriptRunner implements CodeRunner {

    private final ScriptEngine engine;

    public JavaScriptRunner() {
        ScriptEngineManager factory = new ScriptEngineManager();
        engine = factory.getEngineByName( "JavaScript" );
    }

    @Override
    public Object runScript( String script ) {
        System.out.println( getLanguage() + " interpreting script" );
        try {
            return engine.eval( script );
        } catch ( ScriptException se ) {
            return se.toString();
        }
    }

    @Override
    public String getLanguage() {
        return "JavaScript";
    }
}
