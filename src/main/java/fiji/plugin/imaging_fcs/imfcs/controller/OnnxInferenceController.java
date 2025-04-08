
package fiji.plugin.imaging_fcs.imfcs.controller;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import ai.onnxruntime.OrtException;
import fiji.plugin.imaging_fcs.imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.imfcs.model.OnnxInferenceModel;
import fiji.plugin.imaging_fcs.imfcs.model.onnx.OnnxRuntimeStatus;
import fiji.plugin.imaging_fcs.imfcs.view.OnnxInferenceView;
import ij.IJ;
import ij.ImagePlus;

/**
 * The OnnxInferenceController class handles the interactions between the
 * OnnxInferneceModel and the OnnxInferenceView,
 * managing the fitting process and updating the view based on user actions.
 */
public class OnnxInferenceController {
    private final OnnxInferenceModel model;
    private final OnnxInferenceView view;
    private final ImageModel imageModel;
    private ExpSettingsModel expSettingsModel;

    /**
     * Constructs a new OnnxInferenceController with the given OnnxInferenceModel.
     *
     * @param model The OnnxInferenceModel instance.
     */
    public OnnxInferenceController(OnnxInferenceModel model, ImageModel imageModel, ExpSettingsModel expSettingsModel) {
        this.model = model;
        this.view = new OnnxInferenceView(this, model);

        // Initialize stride values based on the view defaults.
        this.model.setStrideX(this.view.getStrideX());
        this.model.setStrideY(this.view.getStrideY());
        this.model.setStrideFrames(this.view.getStrideFrames());

        this.imageModel = imageModel;
        this.expSettingsModel = expSettingsModel;
    }

    // Load an ONNX Model.
    public void btnLoadPressed() throws OrtException {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("ONNX Protobuf Models", "onnx");
        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            String filePath = selectedFile.getAbsolutePath();

            System.out.println("Selected ONNX model file: " + filePath);
            this.updateModelPath(filePath);
        }

        // Load the model
        this.model.loadOnnxModel(this.view.getUseGPU());

        // Populate the Model metadata
        this.view.setModelInputMetadata(this.model.getInputMetadata());
        this.view.updateStatus(this.model.getCurrentStatus().getDisplayLabel());
    }

    private void updateModelPath(String filePath) {
        this.view.updateModelPath(filePath);
        this.model.updateModelPath(filePath);
    }

    /**
     * Performs ONNX inference using the loaded model and provided settings.
     * Always returns a map, which will be empty if inference cannot be run
     * (due to state checks) or if an error occurs during processing.
     *
     * @param expSettingsModel The experiment settings (adjust type as needed).
     * @return A Map where keys are output names and values are the resulting
     *         ImagePlus stacks.
     *         Returns an empty map if inference is skipped or fails.
     */
    public Map<String, ImagePlus> infer() {
        this.view.updateStatus(OnnxRuntimeStatus.PROCESSING.getDisplayLabel());
        // 1. Initialize the result map - Default to empty
        Map<String, ImagePlus> imagePlusResultsMap = new HashMap<>();// Use HashMap for mutability

        // 2. Check preconditions BEFORE the try-catch block
        if (!this.imageModel.isImageLoaded()) {
            System.out.println("Inference skipped: No image loaded.");
            IJ.error("No image loaded.");
            return imagePlusResultsMap; // Return empty map
        }
        if (!isActivated()) {
            System.out.println("Inference skipped: System is not activated.");
            IJ.error("Inference skipped: System is not activated."); // Optional ImageJ log
            return imagePlusResultsMap; // Return empty map
        }
        if (!model.canFit()) { // Assuming model.canFit() exists and checks readiness
            System.out.println("Inference skipped: Model cannot fit (e.g., not loaded or dimensions mismatch?).");
            IJ.error("Inference skipped: Model cannot fit."); // Optional ImageJ log
            return imagePlusResultsMap; // Return empty map
        }

        // 3. Perform inference and conversion within a try-catch block
        try {
            // --- Run Inference ---
            // This might throw OrtException or potentially others
            Map<String, float[][][]> modelOutsArray = model.runInference(this.imageModel, this.expSettingsModel);

            // --- Optional: Debug Printing (Consider removing or making conditional for
            // production) ---
            if (modelOutsArray != null) {
                System.out
                        .println("Raw inference output received. Processing " + modelOutsArray.size() + " output(s).");
                for (Map.Entry<String, float[][][]> entry : modelOutsArray.entrySet()) {
                    String outputName = entry.getKey();
                    float[][][] resultArray = entry.getValue();

                    System.out.println("\n=== Raw Output Details: " + outputName + " ===");
                    if (resultArray == null || resultArray.length == 0 || resultArray[0] == null
                            || resultArray[0].length == 0 || resultArray[0][0] == null
                            || resultArray[0][0].length == 0) {
                        System.out.println("  (Result array is null or empty for this output)");
                    } else {
                        System.out.println("  Result Array Dimensions: [" + resultArray.length + "]["
                                + resultArray[0].length + "][" + resultArray[0][0].length + "]");
                        // Consider limiting the full printout for large arrays in production
                        // printArrayContents(resultArray); // Maybe extract printing if complex
                    }
                }
            } else {
                System.out.println("Warning: model.runInference returned a null map.");
                // Continue, castArrayToImagePlus should handle null input map
            }

            // --- Convert array map to ImagePlus map ---
            // This method should internally handle null/empty input map and invalid arrays
            // Assign the result directly to our return variable
            imagePlusResultsMap = model.castArrayToImagePlus(modelOutsArray);

            System.out.println("Successfully converted inference results to ImagePlus map.");
        } catch (OrtException e) {
            // Handle ONNX specific errors during runInference or potentially during setup
            // called within it
            System.err.println("!!! ONNX Runtime Exception during inference: " + e.getMessage());
            e.printStackTrace(); // Print stack trace for detailed debugging
            IJ.handleException(e); // Use ImageJ's exception handler for user feedback / logging
            // imagePlusResultsMap remains empty (its initial state)
        } catch (RuntimeException e) {
            // Handle unexpected runtime errors during inference or conversion
            System.err.println("!!! Runtime Exception during inference/conversion: " + e.getMessage());
            e.printStackTrace();
            // IJ.log(String.format("%s at pixel x=%d, y=%d", ...)); // Original log format
            // needs context (pixel info) which isn't available here easily.
            IJ.handleException(e); // Use generic ImageJ handler
            // imagePlusResultsMap remains empty
        } catch (Exception e) {
            // Catch any other possible checked exceptions as a safety net
            System.err.println("!!! Unexpected Exception during inference/conversion: " + e.getMessage());
            e.printStackTrace();
            IJ.handleException(e);
            // imagePlusResultsMap remains empty
        }

        // 4. Always return the map (populated if successful, empty otherwise)
        return imagePlusResultsMap;
    }

    public void teardownOnnxSession() {
        this.model.closeOnnxSession();
        this.view.updateStatus(this.model.getCurrentStatus().getDisplayLabel());
    }

    /**
     * Sets the visibility of the view.
     *
     * @param b The visibility status.
     */
    public void setVisible(boolean b) {
        // Perform teardown when closing.
        if (!b) {
            this.teardownOnnxSession();
        }
        view.setVisible(b);
    }

    /**
     * Dispose the view
     */
    public void dispose() {
        this.view.dispose();
    }

    /**
     * Bring the view to front
     */
    public void toFront() {
        this.view.toFront();
    }

    /**
     * Checks if the view is currently visible (activated).
     *
     * @return true if the view is visible, false otherwise.
     */
    public boolean isActivated() {
        return view.isVisible();
    }

    public OnnxInferenceModel getModel() {
        return model;
    }

    public OnnxInferenceView getView() {
        return view;
    }

    // TODO: Add functionality to display windows.
    public void btnRunInferencePressed() {
        Map<String, ImagePlus> outputMaps = this.infer();

    }
}
