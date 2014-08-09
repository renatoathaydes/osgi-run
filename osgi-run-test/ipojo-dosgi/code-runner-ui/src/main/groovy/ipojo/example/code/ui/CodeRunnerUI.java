package ipojo.example.code.ui;

import ipojo.example.code.CodeRunner;
import ipojo.example.code.ui.view.MainView;
import org.apache.felix.ipojo.annotations.*;

@Component(name = "CodeRunnerUI")
@Instantiate
public class CodeRunnerUI {

    private MainView mainView;

    @Validate
    public void start() {
        mainView = new MainView();
        mainView.create();
    }

    @Invalidate
    public void stop() {
        if ( mainView != null )
            mainView.destroy();
    }

    @Bind( aggregate = true )
    public void bindCodeRunner( CodeRunner codeRunner ) {
        mainView.addCodeRunner( new ResilientCodeRunner( codeRunner ) );
    }


    @Unbind
    public void unbindCodeRunner( CodeRunner codeRunner ) {
        mainView.removeCodeRunner( codeRunner );
    }

    private static class ResilientCodeRunner implements CodeRunner {

        private final CodeRunner delegate;

        private ResilientCodeRunner( CodeRunner delegate ) {
            this.delegate = delegate;
        }

        @Override
        public Object runScript( String script ) {
            try {
                return delegate.runScript( script );
            } catch ( Exception e ) {
                return e;
            }
        }

        @Override
        public String getLanguage() {
            return delegate.getLanguage();
        }

    }

}
