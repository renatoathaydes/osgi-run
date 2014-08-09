package ipojo.example.code.ui.view

import groovy.beans.Bindable
import groovy.swing.SwingBuilder
import ipojo.example.code.CodeRunner

import javax.swing.*
import java.awt.*
import java.beans.PropertyChangeEvent
import java.beans.PropertyChangeListener

class MainView {

    private final builder = new SwingBuilder()
    private JFrame theFrame

    class Model {
        @Bindable
        CodeRunner codeRunner

        final Map<String, CodeRunner> codeRunners = [ : ] as ObservableMap
    }

    private final model = new Model()

    void create() {
        JTextArea resultText = null
        JComboBox langsCombo = null

        builder.edt {
            theFrame = frame( title: 'D-OSGi IPojo Demo', show: true,
                    size: [ 400, 500 ], locationRelativeTo: null ) {
                vbox {
                    vstrut 10
                    def sourceArea = textArea( editable: true )
                    vstrut 10
                    hbox {
                        hstrut 5
                        button( text: 'Run',
                                enabled: bind { model.codeRunner != null },
                                actionPerformed: {
                                    def result = model.codeRunner?.runScript( sourceArea.text )
                                    resultText.text = result as String
                                } )
                        hstrut 5
                        label 'Language:'
                        hstrut 5
                        langsCombo = comboBox( actionPerformed: { event ->
                            model.codeRunner = model.codeRunners[ event.source.selectedItem ]
                        } )
                        hstrut 5
                    }
                    vstrut 10
                    resultText = textArea( editable: false, background: new Color( 240, 230, 140 ) )
                    vstrut 10
                }
            }
        }

        model.codeRunners.addPropertyChangeListener { PropertyChangeEvent event ->
            switch ( event.newValue ) {
                case CodeRunner: langsCombo.addItem( event.propertyName ); break
                case null: langsCombo.removeItem( event.propertyName ); break
            }
        } as PropertyChangeListener
    }

    void destroy() {
        theFrame?.dispose()
    }

    void addCodeRunner( CodeRunner codeRunner ) {
        model.codeRunners[ codeRunner.language ] = codeRunner
    }

    void removeCodeRunner( CodeRunner codeRunner ) {
        model.codeRunners.remove( codeRunner.language )
    }

}
