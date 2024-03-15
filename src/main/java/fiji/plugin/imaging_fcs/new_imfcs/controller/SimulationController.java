package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.SimulationView;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SimulationController {
    private final ImageController imageController;
    private final SimulationView simulationView;
    private final SimulationModel simulationModel;

    public SimulationController(ImageController imageController, ExpSettingsModel expSettingsModel) {
        this.imageController = imageController;
        simulationModel = new SimulationModel(this, expSettingsModel);
        simulationView = new SimulationView(this, simulationModel);
    }

    public void onSimulationComplete() {
        simulationView.enableBtnStopSimulation(false);
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
        return (ActionEvent ev) -> {
            simulationView.enableBtnStopSimulation(true);
            simulationModel.runSimulation();
        };
    }

    public ActionListener btnStopSimulationPressed() {
        return (ActionEvent ev) -> simulationModel.cancel(true);
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
            simulationModel.setBlinkFlag(selected);
        };
    }

    public void loadImage(ImagePlus image) {
        simulationView.enableBtnStopSimulation(false);
        imageController.loadImage(image, true);
    }
}
