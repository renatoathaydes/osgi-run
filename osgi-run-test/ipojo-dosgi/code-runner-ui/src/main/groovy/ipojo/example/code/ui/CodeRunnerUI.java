package ipojo.example.code.ui;

import ipojo.example.code.CodeRunner;
import ipojo.example.code.ui.view.MainView;
import org.apache.felix.ipojo.annotations.*;

@Component(name = "CodeRunnerUI")
@Instantiate
public class CodeRunnerUI {

    @Requires(optional = true, defaultimplementation = NoServiceAvailable.class)
    private CodeRunner codeRunner;

    private MainView mainView;

    @Validate
    public void start() {
        mainView = new MainView(codeRunner);
        mainView.create();
    }

    @Invalidate
    public void stop() {
        if (mainView != null)
            mainView.destroy();
    }

}
