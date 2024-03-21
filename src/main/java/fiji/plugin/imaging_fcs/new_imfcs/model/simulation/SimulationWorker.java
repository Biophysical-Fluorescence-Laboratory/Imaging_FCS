package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.controller.SimulationController;
import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.io.File;

/**
 * SimulationWorker is a SwingWorker subclass designed to handle long-running simulation tasks for imaging FCS in a
 * separate thread.
 * This class encapsulates the execution of simulations either in 2D or 3D, based on the specified models, and manages
 * the saving or loading of the resulting simulated images. It supports both batch and interactive modes, facilitating
 * the flexible integration with GUI components and batch processing workflows.
 */
public class SimulationWorker extends SwingWorker<Void, Void> {
    private final SimulationController controller;
    private final SimulationBase simulation;
    private final String path;
    private final boolean batchMode;

    /**
     * Constructs a Worker instance for running imaging FCS simulations.
     *
     * @param model         The simulation model containing parameters and settings for the simulation.
     * @param settingsModel The experimental settings model with additional configuration for the simulation.
     * @param controller    The controller that manages interactions between the simulation model and the UI.
     * @param path          The file path where the simulated image will be saved if in batch mode, otherwise null.
     */
    public SimulationWorker(SimulationModel model, ExpSettingsModel settingsModel, SimulationController controller,
                            File path) {
        if (model.getIs2D()) {
            simulation = new Simulation2D(model, settingsModel);
        } else {
            simulation = new Simulation3D(model, settingsModel);
        }

        this.controller = controller;

        if (path != null) {
            this.batchMode = true;
            this.path = String.format("%s/sim-D1=%.2f-D2=%.2f-F2=%.2f.tif", path.getAbsolutePath(),
                    model.getD1Interface(), model.getD2Interface(), model.getF2());
        } else {
            this.batchMode = false;
            this.path = null;
        }
    }

    /**
     * The main task performed in a background thread. This method initiates the simulation process, adjusts the
     * contrast of the resulting image for better visibility, and either saves the image to disk in batch mode or
     * loads it into the GUI for interactive use.
     *
     * @return null
     */
    @Override
    protected Void doInBackground() {
        try {
            ImagePlus image = simulation.simulate();

            IJ.run(image, "Enhance Contrast", "saturated=0.35");

            if (batchMode) {
                IJ.saveAsTiff(image, path);
            } else {
                controller.loadImage(image);
            }
        } catch (RuntimeException e) {
            IJ.showProgress(1);
            if (batchMode) {
                controller.incrementSimulationErrorsNumber();
            } else {
                IJ.showProgress(1);
                IJ.showStatus("Simulation Interrupted");
                IJ.showMessage(e.getMessage());
            }
        }
        return null;
    }

    /**
     * Called when the background task is completed. It notifies the controller that the batch or single simulation
     * process is complete, allowing for any necessary UI updates or post-processing actions.
     */
    @Override
    protected void done() {
        if (batchMode) {
            controller.onBatchSimulationComplete();
        } else {
            controller.onSimulationComplete();
        }
    }
}
