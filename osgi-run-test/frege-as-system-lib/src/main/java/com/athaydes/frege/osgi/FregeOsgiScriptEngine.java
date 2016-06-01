package com.athaydes.frege.osgi;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

public class FregeOsgiScriptEngine {

    private final ScriptEngine engine;

    public FregeOsgiScriptEngine() {
        this.engine = new ScriptEngineManager().getEngineByName( "frege" );
        if ( engine == null ) {
            throw new RuntimeException( "The Frege script engine could not be found" );
        }
    }

    public Object run( String script ) throws ScriptException {
        return engine.eval( script );
    }

}
