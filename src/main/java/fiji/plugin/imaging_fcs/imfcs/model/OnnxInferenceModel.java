package fiji.plugin.imaging_fcs.imfcs.model;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;

import ij.IJ;  // Import ImageJ
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.FloatProcessor;
import ai.onnxruntime.OrtException;

import java.util.HashMap;
import java.util.Map;
import fiji.plugin.imaging_fcs.imfcs.model.onnx.DeepLearningProcessor;
import fiji.plugin.imaging_fcs.imfcs.model.onnx.InputMetadata;
import fiji.plugin.imaging_fcs.imfcs.model.onnx.OnnxRuntimeStatus;

public class OnnxInferenceModel {
    private int strideX;
    private int strideY;
    private int strideFrames;
    private DeepLearningProcessor deepLearningProcessor;
    private BleachCorrectionModel bleachCorrectionModel;

    // Properties dependent on the loaded model, updated from view using listeners.
    // Required for communication between the controller and the model.
    private int modelInputX;
    private int modelInputY;
    private int modelInputFrames;
    private String modelPath;
    private boolean useGpu;
    private OnnxRuntimeStatus currentStatus = OnnxRuntimeStatus.NO_MODEL_LOADED; 
    
    /**
     * Constructs a ONNX inference model with references to the experimental settings.
     *
     * @param expSettingsModel the experimental settings model.
     */
    public OnnxInferenceModel(BleachCorrectionModel bleachCorrectionModel, boolean UseGpu) {
        this.bleachCorrectionModel = bleachCorrectionModel;
        this.useGpu = UseGpu;
    }

    private boolean validateModelPath(String onnxModelPath) {
        try {
            if (!Files.exists(Paths.get(onnxModelPath))) {
                IJ.error("Error: ONNX model does not exist: " + onnxModelPath);
                return false;
            }
            if (!onnxModelPath.toLowerCase().endsWith(".onnx")) {
                IJ.error("Error: ONNX file must have .onnx extension: " + onnxModelPath);
                return false;
            }
        } catch (InvalidPathException e) {
            IJ.error("Error: Invalid file path: " + e.getMessage());
            return false;
        }
        return true;
    }

    public boolean loadOnnxModel(boolean useGpu) throws OrtException {
        // File validation
        if (!validateModelPath(this.modelPath)) {
            this.currentStatus = OnnxRuntimeStatus.ERROR_LOADING;
            IJ.error("Error loading ONNX model.");
            throw new Error("Invalid paths provided for the ONNX model.");
        }

        // Model selected, but not yet loaded.
        this.currentStatus = OnnxRuntimeStatus.MODEL_SELECTED;

        this.deepLearningProcessor = new DeepLearningProcessor(this.bleachCorrectionModel, useGpu);
        this.deepLearningProcessor.loadOnnxModel(this.modelPath);

        // Update internal state.
        this.useGpu = useGpu;
        this.currentStatus = OnnxRuntimeStatus.READY;

        return true;
    }

    public Map<String, float[][][]> runInference(ImageModel imageModel, ExpSettingsModel expSettingsModel) throws OrtException {
        if (this.currentStatus != OnnxRuntimeStatus.READY) {
            IJ.error("ONNX Model needs to be loaded for inference.");
            throw new Error("ONNX Model is not loaded.");
        }
        try {
            this.currentStatus = OnnxRuntimeStatus.PROCESSING;
            ImagePlus imp = imageModel.getImage();

            // Create the DeepLearningProcessor
            this.deepLearningProcessor.loadImage(imp);

            // Process the image - This now returns a Map
            Map<String, float[][][]> resultsMap = this.deepLearningProcessor.processImage(
                    strideX, strideY, strideFrames,
                    expSettingsModel.getFirstFrame(), expSettingsModel.getLastFrame());

            System.out.println("Processing Complete. Results:");
            for (Map.Entry<String, float[][][]> entry : resultsMap.entrySet()) {
                String outputName = entry.getKey();
                float[][][] resultArray = entry.getValue(); // Get the specific result array for this output

                System.out.println("\n=== Output Name: " + outputName + " ===");

                // Defensive check if the array dimensions are valid before trying to access elements
                if (resultArray == null || resultArray.length == 0 || resultArray[0].length == 0 || resultArray[0][0].length == 0) {
                    System.out.println(" (Result array is null or empty for this output)");
                    continue; // Skip to the next output name
                }

                System.out.println("Result Array Dimensions: [" + resultArray.length + "][" + resultArray[0].length + "][" + resultArray[0][0].length + "]");
                System.out.println("--------------------------");

                // Print the contents of this specific resultArray
                // (Using the original printing logic, now applied per output)
                for (int x = 0; x < resultArray.length; x++) {
                    for (int y = 0; y < resultArray[0].length; y++) {
                        System.out.print("  ["); // Indent slightly for readability
                        for (int frame = 0; frame < resultArray[0][0].length; frame++) {
                            System.out.print(resultArray[x][y][frame] + (frame < resultArray[0][0].length - 1 ? ", " : ""));
                        }
                        System.out.print("] ");
                    }
                    System.out.println(); // Newline after each row (all y values for a given x)
                }
            }
            this.currentStatus = OnnxRuntimeStatus.READY;
            return resultsMap;
        } catch (OrtException e) {
            System.err.println("Error during ONNX processing: " + e.getMessage());
            this.currentStatus = OnnxRuntimeStatus.ERROR_PROCESSING;
            e.printStackTrace();
            // Instead of System.exit, re-throw the exception (or wrap it)
            throw e; // Let the caller handle the OrtException
        } finally {
             // Ensure processor resources are always closed if initialized
            if (this.deepLearningProcessor != null) {
                try {
                    this.deepLearningProcessor.close();
                    System.out.println("\nProcessor resources closed.");
                } catch (OrtException e) {
                    System.err.println("Error closing processor resources: " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
    }
    
    /**
     * Checks if inference should be done.
     *
     * @return true if model is loaded and ready for inference.
     */
    public boolean canFit() {
        if (this.currentStatus == OnnxRuntimeStatus.READY && this.deepLearningProcessor != null) {
            if (this.deepLearningProcessor.isOnnxSessionStarted()) {
                return true;
            } else {
                IJ.error("ONNX Session is not started.");
                return false;
            }
        }
        return false;
    }

    public void updateModelPath(String filePath) {
        this.modelPath = filePath;
    }

    public InputMetadata getInputMetadata() {
        return this.deepLearningProcessor.getInputMetadata();
    }

    public Map<String, ImagePlus> castArrayToImagePlus(Map<String, float[][][]> inferenceResults) {

        // 1. Initialize the map to store the resulting ImagePlus objects.
        Map<String, ImagePlus> imagePlusMap = new HashMap<>();

        // 2. Handle null or empty input map immediately.
        if (inferenceResults == null || inferenceResults.isEmpty()) {
            System.out.println("Input map for castArrayToImagePlus is null or empty. Returning empty map.");
            return imagePlusMap;
        }

        // 3. Iterate through each entry (output name -> 3D float array) in the input map.
        for (Map.Entry<String, float[][][]> entry : inferenceResults.entrySet()) {
            String outputName = entry.getKey();       // The identifier/name for this image stack.
            float[][][] dataArray = entry.getValue(); // The 3D array [WIDTH][HEIGHT][FRAMES].

            // 4. --- Input Validation for the current dataArray ---
            // Check if the array itself is null.
            if (dataArray == null) {
                 System.err.println("Skipping conversion for output '" + outputName + "': Input data array is null.");
                 continue; // Move to the next entry in the map.
            }
            // Check if the first dimension (WIDTH) is valid.
            if (dataArray.length == 0) {
                 System.err.println("Skipping conversion for output '" + outputName + "': Width dimension (dataArray.length) is 0.");
                 continue;
            }
            // Check if the second dimension (HEIGHT) is valid. Assumes rectangular structure.
            // Need to check if dataArray[0] is null before accessing its length.
            if (dataArray[0] == null || dataArray[0].length == 0) {
                System.err.println("Skipping conversion for output '" + outputName + "': Height dimension (dataArray[0].length) is 0 or dataArray[0] is null.");
                continue;
            }
            // Check if the third dimension (FRAMES) is valid. Assumes consistent depth.
            // Need to check if dataArray[0][0] is null before accessing its length.
            if (dataArray[0][0] == null || dataArray[0][0].length == 0) {
                System.err.println("Skipping conversion for output '" + outputName + "': Frames dimension (dataArray[0][0].length) is 0 or dataArray[0][0] is null.");
                continue;
            }
            // --- End Input Validation ---

            // 5. Determine the dimensions from the validated array.
            int width = dataArray.length;          // Width from the first dimension.
            int height = dataArray[0].length;       // Height from the second dimension.
            int nFrames = dataArray[0][0].length;   // Number of frames/slices from the third dimension.

            // 6. Create a new ImageJ ImageStack to hold the individual 2D slices (frames).
            // The stack is defined by the width and height of each slice.
            ImageStack imageStack = new ImageStack(width, height);

            // 7. Iterate through each frame (which corresponds to a slice in the ImageStack).
            for (int frameIndex = 0; frameIndex < nFrames; frameIndex++) {

                // 8. Create a 1D float array to hold the pixel data for the *current* frame/slice.
                // ImageJ processors expect pixel data as a flat 1D array. Size is width * height.
                float[] slicePixels = new float[width * height];

                // 9. Populate the 1D slicePixels array from the 3D dataArray for the current frameIndex.
                // ImageJ's standard pixel order in 1D arrays is row-by-row.
                // So, the pixel at (x, y) should be at index (y * width + x) in the 1D array.
                for (int y = 0; y < height; y++) {
                    for (int x = 0; x < width; x++) {
                        // Calculate the index in the 1D target array.
                        int targetIndex = y * width + x;
                        // Get the pixel value from the source 3D array: dataArray[WIDTH][HEIGHT][FRAME]
                        float pixelValue = dataArray[x][y][frameIndex];
                        // Assign the value to the correct position in the 1D slice array.
                        slicePixels[targetIndex] = pixelValue;
                    }
                } // End of loops for x and y (pixel population for one slice)

                // 10. Create an ImageJ FloatProcessor for this single slice.
                // This represents one 2D grayscale image with 32-bit float data.
                FloatProcessor floatProcessor = new FloatProcessor(width, height, slicePixels);

                // 11. Add the created FloatProcessor (slice) to the ImageStack.
                // It's good practice to give each slice a label, typically its 1-based index.
                imageStack.addSlice(String.valueOf(frameIndex + 1), floatProcessor);

            } // End of loop through frames (all slices added to imageStack)

            // 12. Create the final ImagePlus object.
            // It uses the original outputName as its title and contains the populated ImageStack.
            ImagePlus imagePlus = new ImagePlus(outputName, imageStack);

            // 13. Add the newly created ImagePlus object to the result map, using the
            // original output name as the key.
            imagePlusMap.put(outputName, imagePlus);

            System.out.println("Successfully converted output '" + outputName + "' to ImagePlus (" + width + "x" + height + "x" + nFrames + ").");

        } // End of loop through the input map entries

        // 14. Return the map containing the generated ImagePlus objects.
        return imagePlusMap;
    }

    public OnnxRuntimeStatus getCurrentStatus() {
        return this.currentStatus;
    }
}

