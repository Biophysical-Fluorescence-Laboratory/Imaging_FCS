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
     * Creates an instance of a button given the button class type, label, tooltip
     * text, and font.
     * This is a generic method used internally to avoid code duplication.
     *
     * @param buttonClass the class of the button to create, must extend
     *                    {@link AbstractButton}
     * @param label       the text of the button
     * @param toolTipText the tooltip text for the button
     * @param font        the font to be used for the button's label
     * @return an instance of {@link AbstractButton} with the specified properties
     * @throws Exception if there is an error creating the button instance
     */
    private static AbstractButton createButton(Class<? extends AbstractButton> buttonClass, String label,
                                               String toolTipText, Font font) throws Exception {
        AbstractButton button = buttonClass.getDeclaredConstructor(String.class).newInstance(label);

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
     * @throws Exception if there is an error creating the button
     */
    public static JButton createJButton(String label, String toolTipText, Font font, ItemListener itemListener)
            throws Exception {
        JButton button = (JButton) createButton(JButton.class, label, toolTipText, font);
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
     * @throws Exception if there is an error creating the button
     */
    public static JButton createJButton(String label, String toolTipText, Font font, ActionListener actionListener)
            throws Exception {
        JButton button = (JButton) createButton(JButton.class, label, toolTipText, font);
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
     * @throws Exception if there is an error creating the toggle button
     */
    public static JToggleButton createJToggleButton(String label, String toolTipText, Font font,
                                                    ItemListener itemListener) throws Exception {
        JToggleButton button = (JToggleButton) createButton(JToggleButton.class, label, toolTipText, font);
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
     * @throws Exception if there is an error creating the toggle button
     */
    public static JToggleButton createJToggleButton(String label, String toolTipText, Font font,
                                                    ActionListener actionListener) throws Exception {
        JToggleButton button = (JToggleButton) createButton(JToggleButton.class, label, toolTipText, font);
        button.addActionListener(actionListener);
        return button;
    }
}