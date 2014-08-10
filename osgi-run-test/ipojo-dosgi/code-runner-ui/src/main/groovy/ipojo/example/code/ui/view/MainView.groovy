package ipojo.example.code.ui.view

import groovy.beans.Bindable
import groovy.swing.SwingBuilder
import ipojo.example.code.CompositeCodeRunner

import javax.swing.*
import java.awt.*

class MainView {

    @Bindable
    class Model {
        String language
    }

    private final builder = new SwingBuilder()
    private final CompositeCodeRunner codeRunner
    private final Model model = new Model()

    private JFrame theFrame
    private JComboBox langsCombo = null


    MainView( CompositeCodeRunner codeRunner ) {
        this.codeRunner = codeRunner
    }

    void create() {
        destroy()
        JTextArea resultText = null

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
                                enabled: bind { model.language != null },
                                actionPerformed: {
                                    resultText.text = codeRunner.runScript( model.language, sourceArea.text )
                                } )
                        hstrut 5
                        label 'Language:'
                        hstrut 5
                        langsCombo = comboBox( actionPerformed: { event ->
                            model.language = event.source.selectedItem
                        } )
                        hstrut 5
                        button( text: 'Update Languages',
                                actionPerformed: { updateLanguages() } )
                        hstrut 5
                    }
                    vstrut 10
                    resultText = textArea( editable: false, background: new Color( 240, 230, 140 ) )
                }
            }
        }

        updateLanguages()
    }

    void destroy() {
        theFrame?.dispose()
    }

    private void updateLanguages() {
        if ( langsCombo ) {
            def item = langsCombo.selectedItem
            langsCombo.removeAllItems()
            codeRunner.languages.toList().sort().each { String lang ->
                langsCombo.addItem( lang )
            }
            if ( item )
                langsCombo.selectedItem = item
        }
    }

}
