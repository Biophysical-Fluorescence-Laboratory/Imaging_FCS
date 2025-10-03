package fiji.plugin.imaging_fcs.imfcs.model.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtException;
import ij.IJ;
import ij.ImagePlus;

import java.util.ArrayList;
import java.util.Arrays;
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
        this.imageDimFrames = img.getImageStackSize();

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

    public float[] extractAndStackBatch(List<Pair<ChunkIndices, ResultIndices>> batch, int frames, int width,
            int height) {
        int batchSize = batch.size();
        // Default to 1 channel, might need to make this explicit if Dual Color used in
        // future
        int channels = 1;

        // Here, flatten batch to match ONNX expected semantics
        int totalSize = batchSize * channels * frames * width * height;
        float[] flattenedBatch = new float[totalSize];

        // Populate in loop
        int index = 0;
        for (Pair<ChunkIndices, ResultIndices> indexPair : batch) {
            ChunkIndices chunkIndices = indexPair.getLeft();

            int startX = chunkIndices.startX;
            int startY = chunkIndices.startY;
            int startFrame = chunkIndices.startFrame;

            for (int f = 0; f < frames; f++) {
                for (int x = 0; x < width; x++) {
                    for (int y = 0; y < height; y++) {
                        flattenedBatch[index++] = imageArr[startX + x][startY + y][startFrame + f];
                    }
                }
            }
        }
        return flattenedBatch;
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
            int initialFrame, int finalFrame, int batchSize) throws OrtException {
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

        // Initialize chunk index generator.
        Iterator<Pair<ChunkIndices, ResultIndices>> indexIterator = chunker.getChunkIterator();

        // Batch processing loop
        int currentChunkInd = 0;
        int totalChunks = chunker.getTotalChunks();
        List<Pair<ChunkIndices, ResultIndices>> batch = new ArrayList<>(batchSize);

        while (indexIterator.hasNext() || !batch.isEmpty()) {
            while (indexIterator.hasNext() && batch.size() < batchSize) {
                batch.add(indexIterator.next());
            }

            if (batch.isEmpty()) {
                break;
            }

            int actualBatchSize = batch.size();

            long[] inputShape = new long[] {
                actualBatchSize,
                1, // channel dimension, assumed 1 for now, might change.
                modelInputFrames,
                modelInputX,
                modelInputY,
            };

            float[] flattenedBatchData = extractAndStackBatch(batch, modelInputFrames, modelInputX, modelInputY);

            Map<String, float[]> batchedOutputData;

            // Prepare input tensor within a try-with-resources block for auto-closing
            try (OnnxTensor onnxTensor = OnnxTensor.createTensor(
                    onnxModel.getEnvironment(),
                    FloatBuffer.wrap(flattenedBatchData),
                    inputShape)) {
                    
                Map<String, OnnxTensor> inputMap = new HashMap<>();
                inputMap.put(onnxModel.getInputNames(), onnxTensor);
                
                // ONNX inference.
                batchedOutputData = onnxModel.runInferenceBatched(inputMap);

                // Result processing and populating result map
                for (int i = 0; i < actualBatchSize; i++) {
                    ResultIndices resultIndices = batch.get(i).getRight();

                    for (Map.Entry<String, float[]> entry : batchedOutputData.entrySet()) {
                        String outputName = entry.getKey();
                        float[] predictionBatch = entry.getValue();

                        float predictionValue = predictionBatch[i];
                        float[][][] targetArray = resultsMap.get(outputName);

                        if (targetArray == null) {
                            continue;
                        }

                        targetArray[resultIndices.resultX][resultIndices.resultY][resultIndices.resultFrame] = predictionValue;
                    }
                    currentChunkInd++;
                }
            } catch (OrtException e) {
                IJ.error("ONNX Runtime Error during batch inference: " + e.getMessage());
                throw e; 
            }
            
            batch.clear();
            IJ.showProgress((double) currentChunkInd / (double) totalChunks);
        }

        // Return the map containing all aggregated result arrays
        return resultsMap;
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
