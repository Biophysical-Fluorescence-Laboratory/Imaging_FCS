package fiji.plugin.imaging_fcs.imfcs.model;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.InvalidPathException;

import ij.IJ;  // Import ImageJ
import ij.ImagePlus;
import ai.onnxruntime.OrtException;
import java.util.Map;
import fiji.plugin.imaging_fcs.imfcs.model.onnx.DeepLearningProcessor;

public class OnnxInferenceModel {
    private int strideX;
    private int strideY;
    private int strideFrames;
    private DeepLearningProcessor deepLearningProcessor;
    private BleachCorrectionModel bleachCorrectionModel;
    private boolean onnxModelLoaded = false;
    
    /**
     * Constructs a ONNX inference model with references to the experimental settings.
     *
     * @param expSettingsModel the experimental settings model.
     */
    public OnnxInferenceModel(BleachCorrectionModel bleachCorrectionModel) {
        this.bleachCorrectionModel = bleachCorrectionModel;
        this.onnxModelLoaded = false;
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

    public boolean loadOnnxModel(String onnxModelPath, boolean useGpu) throws OrtException {
        // File validation
        if (!validateModelPath(onnxModelPath)) {
            IJ.error("Error loading ONNX model.");
            throw new Error("Invalid paths provided for the ONNX model.");
        }

        this.deepLearningProcessor = new DeepLearningProcessor(this.bleachCorrectionModel, useGpu);
        this.deepLearningProcessor.loadOnnxModel(onnxModelPath);
        this.onnxModelLoaded = true;
        return true;
    }

    public Map<String, float[][][]> runInference(ImageModel imageModel, ExpSettingsModel expSettingsModel) throws OrtException {
        if (!this.onnxModelLoaded) {
            IJ.error("ONNX Model needs to be loaded for inference.");
            throw new Error("ONNX Model is not loaded.");
        }
        try {
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
            return resultsMap;
        } catch (OrtException e) {
            System.err.println("Error during ONNX processing: " + e.getMessage());
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
        if (this.onnxModelLoaded && this.deepLearningProcessor != null) {
            if (this.deepLearningProcessor.isOnnxSessionStarted()) {
                return true;
            } else {
                IJ.error("ONNX Session is not started.");
                return false;
            }
        }
        return false;
    }

}

