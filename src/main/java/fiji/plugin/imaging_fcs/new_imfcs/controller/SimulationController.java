package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.SimulationView;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SimulationController {
    private final SimulationView simulationView;
    private final SimulationModel simulationModel;

    public SimulationController() {
        simulationModel = new SimulationModel();
        simulationView = new SimulationView(this, simulationModel);
    }

    public void setVisible(boolean b) {
        simulationView.setVisible(b);
    }

    public ActionListener cbModeChanged() {
        return (ActionEvent ev) -> {
            String simMode = ControllerUtils.getComboBoxSelectionFromEvent(ev);
            boolean is2D = simMode.contains("2D");
            boolean isDomain = simMode.contains("dom");
            boolean isMesh = simMode.contains("mesh");

            simulationView.bleachSetEnable(is2D);
            simulationModel.setIs2D(is2D);

            simulationView.domainSetEnable(isDomain);
            simulationModel.setIsDomain(isDomain);

            simulationView.meshSetEnable(isMesh);
            simulationModel.setIsMesh(isMesh);
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
            // Get the button
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setText(selected ? "Triplet On" : "Triplet Off");
            simulationView.tripletSetEnable(selected);
            simulationModel.setSimBlinkFlag(selected);
        };
    }
}
