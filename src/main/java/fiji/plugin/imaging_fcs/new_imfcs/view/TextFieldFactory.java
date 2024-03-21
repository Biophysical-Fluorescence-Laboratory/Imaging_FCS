package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.event.ActionListener;
import java.awt.event.FocusListener;

/**
 * A factory class for creating {@link JTextField} objects with common configurations.
 * This class simplifies the creation of text fields by providing methods that configure
 * text fields with tooltips, and attach various listeners such as {@link DocumentListener},
 * {@link FocusListener}, and {@link ActionListener}.
 */
public final class TextFieldFactory {
    // Private constructor to prevent instantiation
    private TextFieldFactory() {
    }

    /**
     * Creates a {@link JTextField} with specified text and tooltip.
     *
     * @param text    The initial text to display in the text field.
     * @param toolTip The tooltip text to be displayed when hovering over the text field.
     * @return A {@link JTextField} instance with the specified text and tooltip.
     */
    public static JTextField createTextField(Object text, String toolTip) {
        JTextField textField = new JTextField(String.valueOf(text), Constants.TEXT_FIELD_COLUMNS);
        if (!toolTip.isEmpty()) {
            textField.setToolTipText(toolTip);
        }

        return textField;
    }

    /**
     * Creates a {@link JTextField} with specified text, tooltip, and a {@link DocumentListener}.
     *
     * @param text     The initial text to display in the text field.
     * @param toolTip  The tooltip text to be displayed when hovering over the text field.
     * @param listener The {@link DocumentListener} to be added to the text field.
     * @return A {@link JTextField} instance with the specified text, tooltip, and attached {@link DocumentListener}.
     */
    public static JTextField createTextField(Object text, String toolTip, DocumentListener listener) {
        JTextField textField = createTextField(text, toolTip);
        textField.getDocument().addDocumentListener(listener);

        return textField;
    }

    /**
     * Creates a {@link JTextField} with specified text, tooltip, and a {@link FocusListener}.
     *
     * @param text     The initial text to display in the text field.
     * @param toolTip  The tooltip text to be displayed when hovering over the text field.
     * @param listener The {@link FocusListener} to be added to the text field.
     * @return A {@link JTextField} instance with the specified text, tooltip, and attached {@link FocusListener}.
     */
    public static JTextField createTextField(Object text, String toolTip, FocusListener listener) {
        JTextField textField = createTextField(text, toolTip);
        textField.addFocusListener(listener);

        return textField;
    }

    /**
     * Creates a {@link JTextField} with specified text, tooltip, and an {@link ActionListener}.
     *
     * @param text     The initial text to display in the text field.
     * @param toolTip  The tooltip text to be displayed when hovering over the text field.
     * @param listener The {@link ActionListener} to be added to the text field.
     * @return A {@link JTextField} instance with the specified text, tooltip, and attached {@link ActionListener}.
     */
    public static JTextField createTextField(Object text, String toolTip, ActionListener listener) {
        JTextField textField = createTextField(text, toolTip);
        textField.addActionListener(listener);

        return textField;
    }
}
