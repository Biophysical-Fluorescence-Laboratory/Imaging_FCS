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

/**
 * Controls interactions between the simulation model and view, handling user input
 * and simulation execution feedback.
 */
public class SimulationController {
    private final ImageController imageController;
    private final SimulationView simulationView;
    private final SimulationModel simulationModel;

    /**
     * Constructs a controller with references to the image controller and experimental settings model.
     * Initializes the simulation view and model.
     *
     * @param imageController  the controller for image manipulation and display.
     * @param expSettingsModel the experimental settings model.
     */
    public SimulationController(ImageController imageController, ExpSettingsModel expSettingsModel) {
        this.imageController = imageController;
        simulationModel = new SimulationModel(this, expSettingsModel);
        simulationView = new SimulationView(this, simulationModel);
    }

    /**
     * Callback for simulation completion, updating UI elements accordingly.
     */
    public void onSimulationComplete() {
        simulationView.enableBtnStopSimulation(false);
        simulationView.enableBtnSimulate(true);
    }

    /**
     * Sets the visibility of the simulation view.
     *
     * @param b true to make the view visible, false otherwise.
     */
    public void setVisible(boolean b) {
        simulationView.setVisible(b);
    }

    /**
     * Generates an ActionListener for changes in simulation mode.
     *
     * @return an ActionListener that updates simulation settings based on mode selection.
     */
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

    /**
     * Generates an ActionListener for the "Simulate" button press.
     *
     * @return an ActionListener that initiates the simulation.
     */
    public ActionListener btnSimulatePressed() {
        return (ActionEvent ev) -> {
            simulationView.enableBtnStopSimulation(true);
            simulationView.enableBtnSimulate(false);
            simulationModel.runSimulation();
        };
    }

    /**
     * Generates an ActionListener for the "Stop Simulation" button press.
     *
     * @return an ActionListener that stops the ongoing simulation.
     */
    public ActionListener btnStopSimulationPressed() {
        return (ActionEvent ev) -> simulationModel.cancel(true);
    }

    public ActionListener btnBatchSimPressed() {
        // FIXME
        return null;
    }

    /**
     * Generates an ItemListener for the toggle button press to enable/disable triplet simulation.
     *
     * @return an ItemListener that updates the UI and model based on the triplet state.
     */
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

    /**
     * Loads a given image into the image controller for display.
     *
     * @param image the image to display.
     */
    public void loadImage(ImagePlus image) {
        imageController.loadImage(image, true);
    }
}
