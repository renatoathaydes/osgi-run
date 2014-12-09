package ipojo.example.code.ui;

import ipojo.example.code.CompositeCodeRunner;
import ipojo.example.code.LanguageNotAvailableException;
import ipojo.example.code.RemoteException;
import ipojo.example.code.ui.view.MainView;
import org.apache.felix.ipojo.annotations.*;

import java.util.Set;

@Component(name = "CodeRunnerUI")
@Instantiate
public class CodeRunnerUI {

    private MainView mainView;

    public CodeRunnerUI(@Requires CompositeCodeRunner codeRunner) {
        mainView = new MainView(new ResilientCodeRunner(codeRunner));
    }

    @Validate
    public void start() {
        mainView.create();
    }

    @Invalidate
    public void stop() {
        if (mainView != null)
            mainView.destroy();
    }

    private static class ResilientCodeRunner implements CompositeCodeRunner {

        private final CompositeCodeRunner delegate;

        private ResilientCodeRunner(CompositeCodeRunner delegate) {
            this.delegate = delegate;
        }

        @Override
        public String runScript(String language, String script) {
            try {
                return delegate.runScript(language, script);
            } catch (LanguageNotAvailableException | RemoteException e) {
                return e.getMessage();
            } catch (Exception e) {
                return e.toString();
            }
        }

        @Override
        public Set<String> getLanguages() {
            return delegate.getLanguages();
        }

    }

}
