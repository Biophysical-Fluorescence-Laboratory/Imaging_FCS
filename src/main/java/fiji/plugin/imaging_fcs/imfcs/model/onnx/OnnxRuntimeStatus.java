package fiji.plugin.imaging_fcs.imfcs.model.onnx;

/**
 * Represents the different operational states of the ONNX Runtime environment.
 */
public enum OnnxRuntimeStatus {

    NO_MODEL_LOADED("No Model Loaded"),           // Initial state
    MODEL_SELECTED("Model Selected, Initializing..."), // File chosen, setup pending
    READY("Ready for Inference"),           // ONNX Session created and ready
    PROCESSING("Processing Inference..."),        // Actively running the model
    ERROR_LOADING("Error Loading Model"),       // State if model loading/parsing failed
    ERROR_PROCESSING("Error During Processing");  // State if inference failed

    private final String displayLabel;

    OnnxRuntimeStatus(String label) {
        this.displayLabel = label;
    }

    /**
     * Gets the user-friendly label associated with this status.
     * @return The display label string.
     */
    public String getDisplayLabel() {
        return displayLabel;
    }

    // Optional: Override toString() if you want System.out.println(status)
    // to show the label directly, though getDisplayLabel() is more explicit.
    @Override
    public String toString() {
        return displayLabel;
    }
}
