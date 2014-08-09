package ipojo.example.code.ui.view

import ipojo.example.code.CodeRunner

/**
 *
 */
class MainViewTest {

    public static void main( String[] args ) {
        final view = new MainView()
        view.create()

        sleep 2000
        println "Adding new code Runner"
        def run
        view.addCodeRunner( run = new CodeRunner() {
            @Override
            String runScript( String script ) {
                'hi ' + script
            }

            @Override
            String getLanguage() {
                return "Java 1.7"
            }
        } )
        sleep 5000
        println "Removing it now"
        view.removeCodeRunner( run )
    }

}
