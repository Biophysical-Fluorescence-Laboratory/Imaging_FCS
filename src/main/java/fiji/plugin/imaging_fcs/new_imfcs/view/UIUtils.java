package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for UI-related functionalities in the Imaging FCS application.
 * This class includes methods to customize the UI appearance, such as setting default fonts.
 */
public final class UIUtils {

    // Private constructor to prevent instantiation
    private UIUtils() {
    }

    /**
     * Sets a custom font for UI components globally within the application.
     * This method defines default, bold, and italic fonts based on application constants
     * and applies them to specific component types for a consistent look and feel.
     */
    public static void setUIFont() {
        // Define default font settings
        Font defaultFont = new Font(Constants.PANEL_FONT, Font.PLAIN, Constants.PANEL_FONT_SIZE);
        Font boldFont = new Font(Constants.PANEL_FONT, Font.BOLD, Constants.PANEL_FONT_SIZE);
        Font italicFont = new Font(Constants.PANEL_FONT, Font.ITALIC, Constants.PANEL_FONT_SIZE);

        // Setting the default font for all components
        UIManager.getLookAndFeelDefaults().put("defaultFont", defaultFont);

        // A map to hold component-specific fonts
        Map<String, Font> componentFonts = new HashMap<>();
        componentFonts.put("Button.font", boldFont);
        componentFonts.put("ToggleButton.font", boldFont);
        componentFonts.put("RadioButton.font", boldFont);
        componentFonts.put("Label.font", italicFont);
        // For components using the default font, no need to specify again unless different

        // Apply fonts to components
        componentFonts.forEach(UIManager::put);
    }

    /**
     * Creates a text field with specified initial text, tooltip, and document listener.
     *
     * @param text     The initial text for the text field.
     * @param toolTip  The tooltip to display when hovering over the text field.
     * @param listener The document listener to attach to the text field, or null if no listener is needed.
     * @return A new JTextField instance configured with the specified parameters.
     */
    public static JTextField createTextField(String text, String toolTip, DocumentListener listener) {
        JTextField textField = new JTextField(text, Constants.TEXT_FIELD_COLUMNS);
        if (!toolTip.isEmpty()) {
            textField.setToolTipText(toolTip);
        }

        if (listener != null) {
            textField.getDocument().addDocumentListener(listener);
        }

        return textField;
    }

    public static JLabel createJLabel(String text, String toolTip) {
        JLabel label = new JLabel(text);

        if (!toolTip.isEmpty()) {
            label.setToolTipText(toolTip);
        }

        return label;
    }
}