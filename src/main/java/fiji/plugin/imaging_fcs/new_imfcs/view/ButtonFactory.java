package fiji.plugin.imaging_fcs.new_imfcs.view;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

/**
 * Factory class for creating various types of buttons with common properties set,
 * such as the button label, tooltip text, font, and event listeners.
 */
public final class ButtonFactory {

    /**
     * Configures common properties of a button.
     *
     * @param button      The button to configure.
     * @param toolTipText Tooltip text for the button.
     * @param font        Font for the button's label.
     * @return The configured button.
     */
    private static AbstractButton decorateButton(AbstractButton button, String toolTipText, Font font) {
        if (!toolTipText.isEmpty()) {
            button.setToolTipText(toolTipText);
        }

        if (font != null) {
            button.setFont(font);
        }

        return button;
    }

    /**
     * Creates a {@link JButton} with specified label, tooltip text, font, and an
     * item listener.
     *
     * @param label        the text of the button
     * @param toolTipText  the tooltip text for the button
     * @param font         the font to be used for the button's label
     * @param itemListener the item listener to be attached to the button
     * @return a configured {@link JButton} instance
     */
    public static JButton createJButton(String label, String toolTipText, Font font, ItemListener itemListener) {
        JButton button = (JButton) decorateButton(new JButton(label), toolTipText, font);
        button.addItemListener(itemListener);
        return button;
    }

    /**
     * Creates a {@link JButton} with specified label, tooltip text, font, and an
     * action listener.
     *
     * @param label          the text of the button
     * @param toolTipText    the tooltip text for the button
     * @param font           the font to be used for the button's label
     * @param actionListener the action listener to be attached to the button
     * @return a configured {@link JButton} instance
     */
    public static JButton createJButton(String label, String toolTipText, Font font, ActionListener actionListener) {
        JButton button = (JButton) decorateButton(new JButton(label), toolTipText, font);
        button.addActionListener(actionListener);
        return button;
    }

    /**
     * Creates a {@link JToggleButton} with specified label, tooltip text, font, and
     * an item listener.
     *
     * @param label        the text of the toggle button
     * @param toolTipText  the tooltip text for the toggle button
     * @param font         the font to be used for the toggle button's label
     * @param itemListener the item listener to be attached to the toggle button
     * @return a configured {@link JToggleButton} instance
     */
    public static JToggleButton createJToggleButton(String label, String toolTipText, Font font,
                                                    ItemListener itemListener) {
        JToggleButton button = (JToggleButton) decorateButton(new JToggleButton(label), toolTipText, font);
        button.addItemListener(itemListener);
        return button;
    }

    /**
     * Creates a {@link JToggleButton} with specified label, tooltip text, font, and
     * an action listener.
     *
     * @param label          the text of the toggle button
     * @param toolTipText    the tooltip text for the toggle button
     * @param font           the font to be used for the toggle button's label
     * @param actionListener the action listener to be attached to the toggle button
     * @return a configured {@link JToggleButton} instance
     */
    public static JToggleButton createJToggleButton(String label, String toolTipText, Font font,
                                                    ActionListener actionListener) {
        JToggleButton button = (JToggleButton) decorateButton(new JToggleButton(label), toolTipText, font);
        button.addActionListener(actionListener);
        return button;
    }
}