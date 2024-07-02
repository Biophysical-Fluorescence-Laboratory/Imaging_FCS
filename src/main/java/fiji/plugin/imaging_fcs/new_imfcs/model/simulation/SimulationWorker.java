package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.io.File;
import java.util.function.Consumer;

/**
 * SimulationWorker is a SwingWorker subclass designed to handle long-running simulation tasks for imaging FCS in a
 * separate thread.
 * This class encapsulates the execution of simulations either in 2D or 3D, based on the specified models, and manages
 * the saving or loading of the resulting simulated images. It supports both batch and interactive modes, facilitating
 * the flexible integration with GUI components and batch processing workflows.
 */
public final class SimulationWorker extends SwingWorker<Void, Void> {
    private final Consumer<ImagePlus> loadImage;
    private final Runnable onSimulationComplete;
    private final Runnable incrementSimulationErrorsNumber;
    private final SimulationBase simulation;
    private final String path;

    /**
     * Constructs a Worker instance for running imaging FCS simulations.
     *
     * @param model                           The simulation model containing parameters and settings for the simulation.
     * @param settingsModel                   The experimental settings model with additional configuration for the simulation.
     * @param path                            The file path where the simulated image will be saved if in batch mode, otherwise null.
     * @param loadImage                       Callback to load the simulated image into the GUI in interactive mode.
     * @param onSimulationComplete            Callback invoked upon completion of the simulation to update UI or handle post-processing.
     * @param incrementSimulationErrorsNumber Callback to increment the error counter in batch mode on failure.
     */
    public SimulationWorker(SimulationModel model, ExpSettingsModel settingsModel, File path,
                            Consumer<ImagePlus> loadImage, Runnable onSimulationComplete,
                            Runnable incrementSimulationErrorsNumber) {
        if (model.getIs2D()) {
            simulation = new Simulation2D(model, settingsModel);
        } else {
            simulation = new Simulation3D(model, settingsModel);
        }

        this.loadImage = loadImage;
        this.onSimulationComplete = onSimulationComplete;
        this.incrementSimulationErrorsNumber = incrementSimulationErrorsNumber;

        if (path != null) {
            this.path = String.format("%s/sim-D1=%.2f-D2=%.2f-F2=%.2f.tif", path.getAbsolutePath(),
                    model.getD1Interface(), model.getD2Interface(), model.getF2());
        } else {
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

            if (loadImage != null) {
                loadImage.accept(image);
            } else {
                IJ.saveAsTiff(image, path);
            }
        } catch (RuntimeException e) {
            IJ.showProgress(1);
            if (incrementSimulationErrorsNumber != null) {
                incrementSimulationErrorsNumber.run();
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
        onSimulationComplete.run();
    }
}
