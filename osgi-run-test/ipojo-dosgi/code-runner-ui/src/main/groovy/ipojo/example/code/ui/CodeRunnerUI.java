package ipojo.example.code.ui;

import ipojo.example.code.CodeRunner;
import ipojo.example.code.ui.view.MainView;
import org.apache.felix.ipojo.annotations.*;

@Component(name = "CodeRunnerUI")
@Instantiate
public class CodeRunnerUI {

    @Requires(optional = true)
    private CodeRunner codeRunner;

    private MainView mainView;

    @Validate
    public void start() {
        CodeRunner localRunner = new CodeRunner() {
            @Override
            public Object runScript(String script) {
                try {
                    return codeRunner.runScript(script);
                } catch (Exception e) {
                    return e;
                }
            }
        };
        mainView = new MainView(localRunner);
        mainView.create();
    }

    @Invalidate
    public void stop() {
        if (mainView != null)
            mainView.destroy();
    }

}
