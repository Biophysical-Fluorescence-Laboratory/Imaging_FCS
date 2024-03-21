package fiji.plugin.imaging_fcs.new_imfcs.controller;

import javax.swing.*;
import java.awt.event.ActionEvent;

public final class ControllerUtils {
    private ControllerUtils() {
    }

    @SuppressWarnings("unchecked")
    public static String getComboBoxSelectionFromEvent(ActionEvent ev) {
        JComboBox<String> comboBox = (JComboBox<String>) ev.getSource();
        Object selectedItem = comboBox.getSelectedItem();
        if (selectedItem == null) {
            return "";
        } else {
            return selectedItem.toString();
        }
    }
}
