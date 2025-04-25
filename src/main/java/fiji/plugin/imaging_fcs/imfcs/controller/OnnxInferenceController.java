
package fiji.plugin.imaging_fcs.imfcs.controller;

import java.awt.Dimension;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import ai.onnxruntime.OrtException;
import fiji.plugin.imaging_fcs.imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.imfcs.model.OnnxInferenceModel;
import fiji.plugin.imaging_fcs.imfcs.model.onnx.OnnxRuntimeStatus;
import fiji.plugin.imaging_fcs.imfcs.utils.ApplyCustomLUT;
import fiji.plugin.imaging_fcs.imfcs.view.OnnxInferenceView;
import fiji.plugin.imaging_fcs.imfcs.model.OnnxInferenceWorker;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.ImageWindow;

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

    // Keep track of window positions globally or pass state if needed
    private static int nextX = 50;
    private static int nextY = 50;
    private static final int X_OFFSET = 30;
    private static final int Y_OFFSET = 30;
    private static int lastRowHeight = 200; // Keep track of height in the current 'row'

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

        // Disable the RunInference button.
        this.view.disableRunInferenceButton();

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

            this.view.updateStatus(OnnxRuntimeStatus.READY.getDisplayLabel());
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

        // Re-enable the run inference button on completion.
        this.view.enableRunInferenceButton();

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

    public void btnRunInferencePressed() {
        IJ.showStatus("Executing ONNX Inference");
        new OnnxInferenceWorker(this).execute();
    }

    /**
     * Displays each ImagePlus from the map. If a window with the same title
     * (map key) already exists, its content is updated. Otherwise, a new
     * window is created and positioned.
     *
     * @param outputMaps A map where keys are desired titles and values are
     *                   ImagePlus objects.
     */
    public static void showOnnxOutputMaps(Map<String, ImagePlus> outputMaps) {
        if (outputMaps == null || outputMaps.isEmpty()) {
            System.out.println("No output maps to display.");
            return;
        }

        // Reset starting position for each fresh call if desired, or maintain state
        // For this example, let's reset positioning each time for simplicity,
        // although maintaining state across calls might be useful in some plugins.
        nextX = 50;
        nextY = 50;
        lastRowHeight = 200; // Reset estimated height

        // Get screen dimensions
        Dimension screenSize = ij.IJ.getScreenSize();
        int screenWidth = screenSize.width - 50; // Leave margin
        int screenHeight = screenSize.height - 100;// Leave margin

        System.out.println("Displaying/Updating windows...");
        for (Map.Entry<String, ImagePlus> entry : outputMaps.entrySet()) {
            String name = entry.getKey();
            ImagePlus newImp = entry.getValue(); // The new image data

            if (newImp == null) {
                System.out.println("Skipping null ImagePlus associated with key: " + name);
                continue; // Skip to the next entry
            }

            // Set the title on the new ImagePlus regardless, needed for comparison/display
            newImp.setTitle(name);

            // --- Check if a window with this title already exists ---
            ImagePlus existingImp = WindowManager.getImage(name);

            if (existingImp != null && existingImp.getWindow() != null) {
                // --- Update existing window ---
                System.out.println("Updating existing window: " + name);

                // 1. Replace the image stack
                // Important: Use the stack from the *new* ImagePlus
                existingImp.setStack(newImp.getStack());

                // 2. Ensure title is correct (might be redundant but safe)
                existingImp.setTitle(name);

                // 3. Refresh the display
                existingImp.updateAndDraw();
                ImageModel.adaptImageScale(existingImp);
                ApplyCustomLUT.applyCustomLUT(existingImp, "Red Hot");

                // 4. Optional: Bring the updated window to the front
                existingImp.getWindow().toFront();

                // Skip positioning for existing windows.

            } else {
                // --- Show as a new window ---
                System.out.println("Creating new window: " + name);

                // 1. Show the new ImagePlus
                newImp.show(); // Creates the window
                ImageWindow win = newImp.getWindow();
                ImageModel.adaptImageScale(newImp);
                ApplyCustomLUT.applyCustomLUT(newImp, "Red Hot");

                // 2. Position the *new* window
                if (win != null) {
                    int actualWindowWidth = win.getWidth();
                    int actualWindowHeight = win.getHeight();
                    lastRowHeight = Math.max(lastRowHeight, actualWindowHeight); // Track max height in row

                    // Check if placing at nextX overflows screen width
                    if (nextX + actualWindowWidth > screenWidth) {
                        nextX = 50; // Reset X to initial
                        nextY += Y_OFFSET + lastRowHeight; // Move down by offset + max height of previous row
                        lastRowHeight = actualWindowHeight; // Reset max height for the new row
                    }
                    // Check if placing at nextY overflows screen height
                    if (nextY + actualWindowHeight > screenHeight) {
                        nextY = 50; // Reset Y (wrap around)
                        nextX = 50; // Reset X too for wrap
                        lastRowHeight = actualWindowHeight; // Reset height track
                    }

                    win.setLocation(nextX, nextY);

                    // 3. Calculate position for the *next* potential new window
                    nextX += X_OFFSET; // Prepare for next window slightly offset right

                } else {
                    System.err.println(
                            "Warning: Could not get window reference immediately for '" + name + "' to set location.");
                    // Fallback positioning increment if window ref fails immediately
                    nextX += X_OFFSET;
                    if (nextX + 200 > screenWidth) { // Use estimate
                        nextX = 50;
                        nextY += Y_OFFSET + lastRowHeight;
                    }
                    if (nextY + 200 > screenHeight) {
                        nextY = 50;
                    }
                }
            }
        }
        System.out.println("Finished displaying/updating windows.");
    }

    public void startOnnxSession() {
        this.model.startOnnxSession();
    }

    public boolean canDoInference() {
        return this.model.canFit();
    }

    // Function for saving during the Batch mode inference.
    // Designed to interface well with the existing Batch processing.
    // NOTE: The reason this is separate is because the existing batch processing is
    // a bit constrained.
    // i.e. windows are only ever saved as PNG files, and only saved when "Save
    // Plots" is toggled.
    // This function guarantees that ONNX inference outputs are saved.
    public static void saveOnnxOutputMaps(Map<String, ImagePlus> outputMaps, String path) {
        for (Map.Entry<String, ImagePlus> entry : outputMaps.entrySet()) {
            String title = entry.getKey();
            ImagePlus imp = entry.getValue(); // The new image data

            if (imp == null) {
                System.out.println("Skipping null ImagePlus associated with key: " + title);
                continue; // Skip to the next entry
            }

            // Set the title on the new ImagePlus regardless, needed for comparison/display
            imp.setTitle(title);

            // Remove the last part with parentheses, if present
            title = title.replaceAll("\\s*\\([^)]*\\)", "");

            // Trim any trailing whitespace after removing parentheses
            title = title.trim();

            // Replace spaces with underscores
            title = title.replace(" ", "_");

            // Replace slashes with "up" (this prevents error since / is used to separate
            // folders)
            title = title.replace("/", "up");
            // Same here since \ is used for Windows
            title = title.replace("\\", "down");

            IJ.saveAsTiff(imp, path + "_onnx" + title + ".tiff");
        }
    }
}
