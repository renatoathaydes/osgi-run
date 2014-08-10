package ipojo.example.code.ui.view

import ipojo.example.code.CompositeCodeRunner

/**
 *
 */
class MainViewTest {

    public static void main( String[] args ) {
        final view = new MainView( new CompositeCodeRunner() {
            @Override
            String runScript( String language, String script ) {
                return "$language -> $script"
            }

            @Override
            Set<String> getLanguages() {
                [ "Java", "Scala", "Ceylon" ] as Set
            }
        } )
        view.create()
    }

}
