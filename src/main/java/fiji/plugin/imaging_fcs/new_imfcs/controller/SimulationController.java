package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.BatchSimulationView;
import fiji.plugin.imaging_fcs.new_imfcs.view.SimulationView;
import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.File;

/**
 * Controls interactions between the simulation model and view, handling user input
 * and simulation execution feedback.
 */
public class SimulationController {
    private final ImageController imageController;
    private final SimulationView simulationView;
    private final SimulationModel simulationModel;

    private int simulationsRunning = 0;
    private int numSimulationsErrors = 0;

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
        simulationView.enableBtnBatch(true);
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
     * Initiates the process of running batch simulations based on parameters defined in a separate view.
     *
     * @param view The BatchSimulationView containing user-defined parameters for batch simulation.
     */
    public void runBatchSimulation(BatchSimulationView view) {
        double[] batchD1 = new double[]{view.getNextNumber(), view.getNextNumber(), view.getNextNumber()};
        double[] batchD2 = new double[]{view.getNextNumber(), view.getNextNumber(), view.getNextNumber()};
        double[] batchF2 = new double[]{view.getNextNumber(), view.getNextNumber(), view.getNextNumber()};

        JFileChooser fc = new JFileChooser();
        fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fc.setMultiSelectionEnabled(false);

        if (fc.showDialog(null, "Choose directory") == JFileChooser.APPROVE_OPTION) {
            File path = fc.getSelectedFile();
            if (!path.exists()) {
                IJ.showMessage("Directory does not exist.");
                return;
            }

            // reset the variables to count the number of simulations running and errors
            simulationsRunning = 0;
            numSimulationsErrors = 0;

            simulationView.enableBtnStopSimulation(true);
            simulationView.enableBtnSimulate(false);
            simulationView.enableBtnBatch(false);

            simulationModel.runBatch(path, batchD1, batchD2, batchF2);
        }
    }

    /**
     * Handles the completion of all batch simulations, updating the UI and displaying the final status.
     */
    public void onBatchSimulationComplete() {
        simulationsRunning--;
        if (simulationsRunning <= 0) {
            simulationView.enableBtnStopSimulation(false);
            simulationView.enableBtnSimulate(true);
            simulationView.enableBtnBatch(true);

            String message = String.format("Batch simulation finished with %d errors", numSimulationsErrors);
            IJ.showStatus("Simulation ended.");
            IJ.showMessage(message);
            IJ.showProgress(1);
        }
    }

    /**
     * Increments the count of simulations currently running. Used to track batch simulation progress.
     */
    public void incrementSimulationsRunningNumber() {
        simulationsRunning++;
    }

    /**
     * Increments the count of encountered errors during simulation. Used for error tracking in batch simulations.
     */
    public void incrementSimulationErrorsNumber() {
        numSimulationsErrors++;
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
            simulationView.enableBtnBatch(false);
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
        return (ActionEvent ev) -> {
            BatchSimulationView view = new BatchSimulationView();
            view.display(this::runBatchSimulation);
        };
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
