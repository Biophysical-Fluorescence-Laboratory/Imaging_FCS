package fiji.plugin.imaging_fcs.imfcs.model;

import fiji.plugin.imaging_fcs.imfcs.controller.OnnxInferenceController;
import ij.IJ;
import ij.ImagePlus;

import java.util.Map;
import java.util.concurrent.ExecutionException;

import javax.swing.*;

/**
 * CorrelationWorker performs correlation on a given ROI using an
 * ImageController.
 * It ensures that only one correlation task is active at a time by canceling
 * any
 * previously running instance. The worker passes its cancellation status to the
 * correlation process so that it can exit early if needed.
 */
public class OnnxInferenceWorker extends SwingWorker<Map<String, ImagePlus>, Void> {
    // Track the current running instance.
    private static volatile OnnxInferenceWorker currentInstance;

    // References to the objects needed for correlation.
    private final OnnxInferenceController onnxInferenceController;

    /**
     * Creates a new OnnxInferenceWorker
     *
     * @param imageController the controller performing correlation
     */
    public OnnxInferenceWorker(OnnxInferenceController onnxInferenceController) {
        this.onnxInferenceController = onnxInferenceController;
        currentInstance = this;
    }

    /**
     * Performs the inference in a background thread.
     *
     * @return null upon completion
     * @throws Exception if an error occurs during correlation
     */
    @Override
    protected Map<String, ImagePlus> doInBackground() throws Exception {
        return this.onnxInferenceController.infer();
    }

    /**
     * Updates the UI when the task is finished.
     * If the task was cancelled, the status is updated accordingly.
     * The static reference to the current instance is cleared.
     */
    @Override
    protected void done() {
        try {
            // Plot the output maps.
            Map<String, ImagePlus> outputMaps = get();
            OnnxInferenceController.showOnnxOutputMaps(outputMaps);
        } catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
            synchronized (OnnxInferenceWorker.class) {
                if (currentInstance == this) {
                    currentInstance = null; // Clear reference when done
                }
            }
        }
    }
}
