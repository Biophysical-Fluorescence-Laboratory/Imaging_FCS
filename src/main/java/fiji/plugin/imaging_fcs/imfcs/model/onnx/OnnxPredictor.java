package fiji.plugin.imaging_fcs.imfcs.model.onnx;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.SessionOptions;
import ai.onnxruntime.TensorInfo;
import ai.onnxruntime.OrtSession.SessionOptions.OptLevel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OnnxPredictor implements AutoCloseable {

    private final OrtEnvironment env;
    private OrtSession session;
    private final List<String> inputNames;
    private InputMetadata inputMetadata;
    private final List<String> outputNames;
    private final Map<String, long[]> outputShapes;

    // Define the expected name of the input tensor containing the shape info
    private static final String TARGET_INPUT_NAME = "input";
    // Define the expected indices based on [BATCH, CHANNELS, FRAMES, WIDTH, HEIGHT]
    private static final int FRAMES_INDEX = 2;
    private static final int WIDTH_INDEX = 3;  // Assuming width is 3rd dim (index 3)
    private static final int HEIGHT_INDEX = 4; // Assuming height is 4th dim (index 4)
    private static final int EXPECTED_DIMS = 5; // Expected number of dimensions

    /**
     * Creates an ONNX predictor model, optionally configuring GPU execution,
     * with graph optimizations DISABLED for maximum numerical reproducibility.
     *
     * @param modelPath The file path to the .onnx model.
     * @param useGpu    If true, attempts to configure the session with the CUDA execution provider.
     *                  If false, or if CUDA configuration fails, uses the CPU provider.
     * @throws OrtException If the model file cannot be loaded or the session cannot be created.
     */
    public OnnxPredictor(String modelPath, boolean useGpu) throws OrtException {
        this.env = OrtEnvironment.getEnvironment();
        this.inputNames = new ArrayList<>();
        this.outputNames = new ArrayList<>();
        this.outputShapes = new HashMap<>();

        // Use try-with-resources for SessionOptions to ensure it's closed automatically
        try (SessionOptions options = new SessionOptions()) {

            // --- DISABLE OPTIMIZATIONS ---
            // Set the optimization level to NONE before configuring providers.
            // This ensures no graph optimizations are applied, prioritizing
            // numerical reproducibility over potential speed gains.
            System.out.println("Setting ONNX Runtime Optimization Level to: NO_OPTIMIZATION");
            options.setOptimizationLevel(OptLevel.NO_OPT);
            // --- END DISABLE OPTIMIZATIONS ---

            if (useGpu) {
                System.out.println("Attempting to configure ONNX Runtime session for GPU (CUDA).");
                try {
                    // Add the CUDA execution provider (device 0 is typically the default GPU)
                    options.addCUDA(0);
                    System.out.println("CUDA Execution Provider added to session options.");
                    // No need to set OptLevel again here, it's already set to NO_OPTIMIZATION
                } catch (OrtException e) {
                    // Handle failure to add CUDA provider
                    System.err.println("WARNING: Failed to add CUDA Execution Provider. " +
                                       "Ensure Nvidia drivers and CUDA toolkit are compatible and installed. " +
                                       "Falling back to CPU. Error: " + e.getMessage());
                    // The OptLevel is already set to NO_OPTIMIZATION for the CPU fallback
                }
            } else {
                System.out.println("Configuring ONNX Runtime session for CPU.");
                // OptLevel is already set to NO_OPTIMIZATION
            }

            // Create the session using the configured options
            this.session = env.createSession(modelPath, options);
        } // SessionOptions 'options' is automatically closed here

        // Extract metadata (input/output names and shapes) - same as before
        extractMetadata();
    }

    // Helper method to keep constructor cleaner
    private void extractMetadata() throws OrtException {
        if (this.session == null) {
            throw new IllegalStateException("Session is not initialized.");
        }
        // Extract input metadata
        for (Map.Entry<String, NodeInfo> entry : session.getInputInfo().entrySet()) {
            String name = entry.getKey();
            TensorInfo info = (TensorInfo) entry.getValue().getInfo();
            // Check if this is the specific input we want metadata from
            if (name.equals(TARGET_INPUT_NAME)) {
                 // Ensure the NodeInfo is actually TensorInfo
                if (info instanceof TensorInfo) {
                    long[] shape = info.getShape();

                    // Validate shape - Ensure it has enough dimensions
                    if (shape.length == EXPECTED_DIMS) {
                        // Populate the metadata object
                        // Note the cast from long to int - potential overflow if dims > Integer.MAX_VALUE
                        long modelInputFrames = shape[FRAMES_INDEX];
                        long modelInputX = shape[WIDTH_INDEX];
                        long modelInputY = shape[HEIGHT_INDEX];
                        this.inputMetadata = new InputMetadata(modelInputX, modelInputY, modelInputFrames);
                    } else {
                        // Handle error: Shape does not have the expected number of dimensions
                        throw new IllegalStateException(
                            "Input tensor '" + TARGET_INPUT_NAME + "' has shape " + Arrays.toString(shape) +
                            ", but expected " + EXPECTED_DIMS + " dimensions.");
                    }
                } else {
                     // Handle error: The node with the target name is not a Tensor
                     throw new IllegalStateException(
                        "Input node '" + TARGET_INPUT_NAME + "' is not of type TensorInfo.");
                }
            }
        }
        // Extract output metadata
        for (Map.Entry<String, NodeInfo> entry : session.getOutputInfo().entrySet()) {
            String name = entry.getKey();
            TensorInfo info = (TensorInfo) entry.getValue().getInfo();
            outputNames.add(name);
            outputShapes.put(name, info.getShape());
        }
    }

    public OrtEnvironment getEnvironment() {
        return env;
    }

    public List<String> getInputNames() {
        return inputNames;
    }

    public InputMetadata getInputMetadata() {
        return inputMetadata;
    }

    public List<String> getOutputNames() {
        return outputNames;
    }

    public Map<String, long[]> getOutputShapes() {
        return outputShapes;
    }

 /**
     * Runs inference using the provided input tensor map.
     * Extracts the first float value from each output tensor.
     * Manages the lifecycle of the session result and output tensors internally.
     *
     * @param input Map where keys are input tensor names and values are the OnnxTensor inputs.
     * @return A Map where keys are the output tensor names and values are the single float result extracted from each.
     * @throws OrtException If there is an error during inference or tensor processing.
     */
    public Map<String, Float> runInference(Map<String, OnnxTensor> input) throws OrtException {
        Map<String, Float> outputDataMap = new HashMap<>();

        // Use try-with-resources for OrtSession.Result
        // This ensures that 'results' and all the OnnxTensor objects
        // it contains are closed automatically when the block exits.
        try (OrtSession.Result results = session.run(input)) {

            // Iterate through the known output names obtained during model loading
            for (Map.Entry<String, NodeInfo> entry : session.getOutputInfo().entrySet()) {
                String outputName = entry.getKey();

                // Get the output tensor from the results.
                // The .get() retrieves the OnnxValue (which is an OnnxTensor here).
                // The cast is safe based on typical ONNX model outputs.
                OnnxTensor tensor = (OnnxTensor) results.get(outputName).get(); // .get() unwraps the internal value

                // Extract the float value.
                // Assuming output tensors for this model are shape [1] (batch size 1)
                // or that we only need the very first float value.
                float value = tensor.getFloatBuffer().get(0);

                // Store the extracted primitive float value in the map
                outputDataMap.put(outputName, value);

                // No need to manually close 'tensor' here -
                // the try-with-resources on 'results' handles it.
            }

        } // 'results' and all contained tensors are closed here.

        return outputDataMap;
    }

    @Override
    public void close() throws OrtException {
        if (session != null) {
            session.close();
        }
    }

    //Inner class to contain input shapes
    public class InputShape {
      public List<String> shape;
      public String type;

      public InputShape() {}
    }
}
