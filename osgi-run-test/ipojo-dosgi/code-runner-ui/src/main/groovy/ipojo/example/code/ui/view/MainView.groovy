package ipojo.example.code.ui.view

import groovy.swing.SwingBuilder
import ipojo.example.code.CodeRunner

import javax.swing.*

class MainView {

    final CodeRunner codeRunner
    final builder = new SwingBuilder()
    JFrame theFrame

    MainView( CodeRunner codeRunner ) {
        this.codeRunner = codeRunner
    }

    void create() {
        JTextArea resultText = null
        builder.edt {
            theFrame = frame( title: 'D-OSGi IPojo Demo', show: true, size: [ 400, 500 ] ) {
                vbox {
                    vstrut 10
                    def sourceArea = textArea( editable: true )
                    vstrut 10
                    hbox {
                        button( text: 'Run', actionPerformed: {
                            def result = codeRunner.runScript( sourceArea.text )
                            resultText.text = result as String
                        } )
                    }
                    vstrut 10
                    resultText = textArea( editable: false )
                    vstrut 10
                }
            }
        }
    }

    void destroy() {
        theFrame?.dispose()
    }

}
