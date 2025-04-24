package fiji.plugin.imaging_fcs.imfcs.model.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ij.IJ;
import ij.ImagePlus;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.List;
import java.nio.FloatBuffer;

import fiji.plugin.imaging_fcs.imfcs.model.BleachCorrectionModel;
import fiji.plugin.imaging_fcs.imfcs.utils.Pair;

public class DeepLearningProcessor {
    private final BleachCorrectionModel bleachCorrectionModel;
    private ImagePlus loadedImage;
    private int imageDimX;
    private int imageDimY;
    private int imageDimFrames;
    private float[][][] imageArr;
    private OnnxPredictor onnxModel;
    private ChunkGenerator chunker;
    private boolean useGpu;

    public DeepLearningProcessor(BleachCorrectionModel bleachCorrectionModel, boolean useGpu) {
        this.useGpu = useGpu;
        this.bleachCorrectionModel = bleachCorrectionModel;
    }

    // Initializes the arrays based on a loaded image.
    public void loadImage(ImagePlus img) {
        this.loadedImage = img;

        // Initialize the array store.
        this.imageDimX = img.getWidth();
        this.imageDimY = img.getHeight();
        this.imageDimFrames = img.getNSlices();

        this.imageArr = new float[this.imageDimX][this.imageDimY][this.imageDimFrames];
    }

    // Prepare the pixels by extracting values and doing bleach correction if
    // requested.
    private void preparePixels(int initialFrame, int finalFrame) {
        for (int x = 0; x < this.imageDimX; x++) {
            for (int y = 0; y < this.imageDimY; y++) {
                // NOTE: This step is technically not needed for no bleach correction.
                bleachCorrectionModel.calcIntensityTrace(this.loadedImage, x, y, initialFrame, finalFrame);
                double[] intensityData = bleachCorrectionModel.getIntensity(this.loadedImage, x, y, 1, initialFrame,
                        finalFrame);

                // Iterate through the frames within the specified range
                for (int t = initialFrame; t < finalFrame; t++) {
                    // Calculate the correct index into intensityData.
                    int intensityIndex = t - initialFrame;

                    // Store the bleach-corrected intensity.
                    // - Cast the double to a float.
                    this.imageArr[x][y][t] = (float) intensityData[intensityIndex];
                }
            }
        }
    }

    public void loadOnnxModel(String modelPath) throws OrtException {
        this.onnxModel = new OnnxPredictor(modelPath, this.useGpu);
    }

    /**
     * Processes the loaded image by iterating through chunks, running inference,
     * and aggregating results for each named output of the ONNX model.
     *
     * @param modelInputX      Model input X dimension.
     * @param modelInputY      Model input Y dimension.
     * @param modelInputFrames Model input frame dimension.
     * @param strideX          Stride in X dimension.
     * @param strideY          Stride in Y dimension.
     * @param strideFrames     Stride in frame dimension.
     * @param initialFrame     Initial frame to process.
     * @param finalFrame       Final frame to process.
     * @return A Map where keys are the ONNX model output names and values are
     *         3D float arrays (float[][][]) containing the aggregated results for
     *         that output.
     * @throws OrtException If there is an error during ONNX processing.
     */
    public Map<String, float[][][]> processImage(int strideX, int strideY, int strideFrames,
            int initialFrame, int finalFrame) throws OrtException {
        // Extract model input properties from the metadata.
        InputMetadata inputMetadata = onnxModel.getInputMetadata();
        int modelInputX = (int) inputMetadata.modelInputX;
        int modelInputY = (int) inputMetadata.modelInputY;
        int modelInputFrames = (int) inputMetadata.modelInputFrames;

        // Prepare pixels (bleach correction, etc.)
        preparePixels(initialFrame, finalFrame);

        // Create the ChunkGenerator
        this.chunker = new ChunkGenerator(imageDimX, imageDimY, imageDimFrames,
                modelInputX, modelInputY, modelInputFrames,
                strideX, strideY, strideFrames);

        // Initialize the map to hold result arrays for each output name
        Map<String, float[][][]> resultsMap = new HashMap<>();
        List<String> outputNames = onnxModel.getOutputNames(); // Get expected output names from the model
        for (String name : outputNames) {
            // Create a result array structure matching the chunking scheme for each output
            resultsMap.put(name, chunker.generateResultArray());
        }

        // Get the chunk iterator
        Iterator<Pair<float[][][], ResultIndices>> chunkIterator = chunker.getChunkIterator(imageArr);
        
        int currentChunkInd = 0;
        int totalChunks = chunker.getTotalChunks();

        // Process each chunk
        while (chunkIterator.hasNext()) {
            ++currentChunkInd;
            Pair<float[][][], ResultIndices> pair = chunkIterator.next();
            float[][][] chunk = pair.getLeft();
            ResultIndices resultIndices = pair.getRight();

            // Add channel dimension for ONNX model (Batch=1, Channel=1)
            // The shape should be [Batch, Channel, Frames, X, Y]
            // chunk dimensions are [Frames][X][Y]
            // Shape for createTensor should match model input spec: [1, 1,
            // modelInputFrames, modelInputX, modelInputY]
            long[] inputShape = new long[] { 1, 1, modelInputFrames, modelInputX, modelInputY };

            // Prepare input tensor within a try-with-resources block for auto-closing
            try (OnnxTensor onnxTensor = OnnxTensor.createTensor(
                    onnxModel.getEnvironment(),
                    FloatBuffer.wrap(flattenChunk(chunk)), // Use a specific flatten for the chunk
                    inputShape)) {
                Map<String, OnnxTensor> inputMap = new HashMap<>();
                inputMap.put(onnxModel.getInputNames(), onnxTensor); // Assuming only one input

                // Run inference - This now returns Map<String, Float>
                // ONNX resources related to output are managed within runInference
                Map<String, Float> chunkOutputData = onnxModel.runInference(inputMap);

                // Process results and fill in the appropriate result array in the map
                for (Map.Entry<String, Float> entry : chunkOutputData.entrySet()) {
                    String outputName = entry.getKey();
                    float predictionValue = entry.getValue();

                    // Get the correct result array from the map based on the output name
                    float[][][] targetArray = resultsMap.get(outputName);

                    // Defensive check (should not happen if initialized correctly based on model
                    // metadata)
                    if (targetArray == null) {
                        System.err.println("Warning: No result array found in resultsMap for output name: " + outputName
                                + ". Skipping.");
                        continue; // Skip this output if structure wasn't pre-allocated
                    }

                    // Fill the specific result array at the calculated indices
                    targetArray[resultIndices.resultX][resultIndices.resultY][resultIndices.resultFrame] = predictionValue;
                }
                // input 'onnxTensor' is closed automatically by try-with-resources

            } // End try-with-resources for input onnxTensor
            // Update ImageJ progress bar
            IJ.showProgress((double) currentChunkInd / (double) totalChunks);
        } // End while loop over chunks

        // Return the map containing all aggregated result arrays
        return resultsMap;
    }

    // Helper function specifically to flatten a 3D float chunk [Frames][X][Y]
    // into the order expected by the FloatBuffer for shape [1, 1, Frames, X, Y]
    private static float[] flattenChunk(float[][][] chunk) {
        int dimFrames = chunk.length;
        int dimX = chunk[0].length;
        int dimY = chunk[0][0].length;
        float[] flattened = new float[dimFrames * dimX * dimY];
        int index = 0;
        // Order: Frames, X, Y (matches typical memory layout for this structure)
        for (int f = 0; f < dimFrames; f++) {
            for (int x = 0; x < dimX; x++) {
                for (int y = 0; y < dimY; y++) {
                    flattened[index++] = chunk[f][x][y];
                }
            }
        }
        return flattened;
    }

    // Close methods to prevent resource leaks
    public void close() throws OrtException {
        if (onnxModel != null)
            onnxModel.close();
    }

    public boolean isOnnxSessionStarted() {
        if (onnxModel != null && onnxModel.getEnvironment() != null) {
            return true;
        }
        return false;
    }

    public InputMetadata getInputMetadata() {
        return this.onnxModel.getInputMetadata();
    }
    
    public OnnxPredictor getOnnxPredictor() {
        return this.onnxModel;
    }
}
