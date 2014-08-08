package ipojo.example.code.ui.view

import ipojo.example.code.CodeRunner

/**
 *
 */
class MainViewTest {

    public static void main( String[] args ) {
        final view = new MainView( new CodeRunner() {
            @Override
            String runScript( String script ) {
                'hi ' + script
            }
        } )
        view.create()
    }

}
