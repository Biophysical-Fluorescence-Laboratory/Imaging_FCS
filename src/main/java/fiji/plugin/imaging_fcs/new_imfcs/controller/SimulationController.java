package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.view.SimulationView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SimulationController {
    private final SimulationView simulationView;

    public SimulationController() {
        simulationView = new SimulationView(this);
    }

    public void setVisible(boolean b) {
        simulationView.setVisible(b);
    }

    public ActionListener cbSimModeChanged() {
        return (ActionEvent ev) -> {
            String simMode = ControllerUtils.getComboBoxSelectionFromEvent(ev);

            simulationView.bleachSetEnable(simMode.contains("2D"));
            simulationView.domainSetEnable(simMode.contains("dom"));
            simulationView.meshSetEnable(simMode.contains("mesh"));
        };
    }

    public ActionListener btnSimulatePressed() {
        // FIXME
        return null;
    }

    public ActionListener btnStopSimulationPressed() {
        // FIXME
        return null;
    }

    public ActionListener btnBatchSimPressed() {
        // FIXME
        return null;
    }

    public ItemListener tbSimTripPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();
            if (ev.getStateChange() == ItemEvent.SELECTED) {
                button.setText("Triplet On");
                simulationView.tripletSetEnable(true);
            } else {
                button.setText("Triplet Off");
                simulationView.tripletSetEnable(false);
            }
        };
    }
}
