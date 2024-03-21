package fiji.plugin.imaging_fcs.new_imfcs.controller;

import ij.IJ;

import javax.swing.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.util.function.Consumer;

/**
 * A factory class for creating {@link FocusListener} objects that are tailored to handle focus events,
 * specifically focusing on the action to take when a component loses focus. This class is designed
 * to simplify the process of attaching a focus lost behavior to text fields, where the new text value
 * is captured and processed through a provided {@link Consumer<String>}.
 */
public class FocusListenerFactory {
    // Private constructor to prevent instantiation
    private FocusListenerFactory() {
    }

    /**
     * Creates a {@link FocusListener} that performs an action with the text of a {@link JTextField}
     * when the component loses focus. The action is specified by the {@link Consumer<String>} passed
     * to the method.
     *
     * @param setter A {@link Consumer<String>} that defines the action to be performed with the
     *               text value of the {@link JTextField} upon losing focus. This consumer receives
     *               the text value of the text field as its input. If the format is not valid, it restores
     *               the initial value that was in the field on gaining focus.
     * @return A {@link FocusListener} that captures the text of a {@link JTextField} when focus is lost
     * and performs the specified action with it.
     */
    public static FocusListener createFocusListener(Consumer<String> setter) {
        return new FocusListener() {
            // This field will be used to store the current value of the text field
            String memory;

            // Store the value on focus gained
            @Override
            public void focusGained(FocusEvent ev) {
                JTextField textField = (JTextField) ev.getComponent();
                memory = textField.getText();
            }

            @Override
            public void focusLost(FocusEvent ev) {
                JTextField textField = (JTextField) ev.getComponent();
                try {
                    setter.accept(textField.getText());
                } catch (Exception e) {
                    // The text field was not successfully parsed, a message is shown and the value is restored
                    IJ.showMessage("Error", "Incorrect format for this field");
                    textField.setText(memory);
                }
            }
        };
    }
}