package fiji.plugin.imaging_fcs.imfcs.view;

import fiji.plugin.imaging_fcs.imfcs.constants.Constants;

import javax.swing.*;
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
     * Creates a {@link JLabel} with specified text and an optional tooltip. The tooltip is only set
     * if the provided tooltip string is not empty. This method streamlines the creation of labels with
     * consistent tooltip handling.
     *
     * @param text    The text to be displayed by the {@link JLabel}.
     * @param toolTip The tooltip text to be displayed when hovering over the label. If this parameter
     *                is an empty string, no tooltip is set.
     * @return A {@link JLabel} instance configured with the specified text and an optional tooltip.
     */
    public static JLabel createJLabel(String text, String toolTip) {
        return createJLabel(text, toolTip, null);
    }

    /**
     * Creates a {@link JLabel} with specified text, an optional tooltip, and an optional font.
     * This method streamlines the creation of labels with consistent tooltip handling and font styling.
     *
     * @param text    The text to be displayed by the {@link JLabel}.
     * @param toolTip The tooltip text to be displayed when hovering over the label. If this parameter
     *                is an empty string, no tooltip is set.
     * @param font    The {@link Font} to be used for the label's text. If null, the default font is used.
     * @return A {@link JLabel} instance configured with the specified text, optional tooltip, and optional font.
     */
    public static JLabel createJLabel(String text, String toolTip, Font font) {
        JLabel label = new JLabel(text);

        if (!toolTip.isEmpty()) {
            label.setToolTipText(toolTip);
        }

        if (font != null) {
            label.setFont(font);
        }

        return label;
    }
}