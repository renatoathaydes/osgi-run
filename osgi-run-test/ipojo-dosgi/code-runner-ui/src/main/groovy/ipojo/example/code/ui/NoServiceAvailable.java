package ipojo.example.code.ui;

import ipojo.example.code.CodeRunner;

/**
 *
 */
public class NoServiceAvailable implements CodeRunner {

    @Override
    public Object runScript(String script) {
        return "<No Service Available>";
    }
}
