package ipojo.example.code.groovy_1_8_6;

import groovy.util.Eval;
import ipojo.example.code.CodeRunner;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Provides;

@Component(propagation = true, name = "groovy-code-runner-1.8.6")
@Instantiate
@Provides
public class GroovyCodeRunner_1_8_6 implements CodeRunner {

    @Override
    public Object runScript(String script) {
        System.out.println(getLanguage() + " interpreting script");
        return Eval.me(script);
    }

    @Override
    public String getLanguage() {
        return "Groovy 1.8.6";
    }
}
