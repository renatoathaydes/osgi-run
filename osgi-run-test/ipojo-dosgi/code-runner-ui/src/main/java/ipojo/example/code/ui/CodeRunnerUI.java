package ipojo.example.code.ui;

import ipojo.example.code.CodeRunner;
import org.apache.felix.ipojo.annotations.Component;
import org.apache.felix.ipojo.annotations.Instantiate;
import org.apache.felix.ipojo.annotations.Requires;

@Component(name = "CodeRunnerUI")
@Instantiate
public class CodeRunnerUI {

    @Requires(optional = true, defaultimplementation = NoServiceAvailable.class)
    private CodeRunner codeRunner;

    //TODO implement

}
