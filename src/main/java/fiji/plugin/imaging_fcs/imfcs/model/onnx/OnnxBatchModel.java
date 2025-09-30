package fiji.plugin.imaging_fcs.imfcs.model.onnx;

/**
 * Model class to store configuration settings for advanced ONNX inference
 * batching and prefetching.
 */
public class OnnxBatchModel {

    // Default configuration values
    private static final int DEFAULT_BATCH_SIZE = 1;
    private static final int DEFAULT_PREFETCH_WORKERS = 0; // 0 or 1 usually implies synchronous/no prefetching
    private static final boolean DEFAULT_ENABLE_BATCHING = false;

    private int batchSize;
    private int numPrefetchWorkers;
    private boolean batchingEnabled;

    public OnnxBatchModel() {
        this.batchSize = DEFAULT_BATCH_SIZE;
        this.numPrefetchWorkers = DEFAULT_PREFETCH_WORKERS;
        this.batchingEnabled = DEFAULT_ENABLE_BATCHING;
    }

    public int getBatchSize() {
        return batchSize;
    }

    public int getNumPrefetchWorkers() {
        return numPrefetchWorkers;
    }

    public boolean isBatchingEnabled() {
        return batchingEnabled;
    }

    public void setBatchSize(String batchSize) {
        Integer batchSizeInt = Integer.parseInt(batchSize);
        if (batchSizeInt < 1) {
            throw new IllegalArgumentException("Batch size must be at least 1.");
        }
        this.batchSize = batchSizeInt;
    }

    public void setNumPrefetchWorkers(String numPrefetchWorkers) {
        Integer numPrefetchWorkersInt = Integer.parseInt(numPrefetchWorkers);
        if (numPrefetchWorkersInt < 0) {
            throw new IllegalArgumentException("Number of prefetch workers cannot be negative.");
        }
        this.numPrefetchWorkers = numPrefetchWorkersInt;
    }

    public void setBatchingEnabled(boolean batchingEnabled) {
        this.batchingEnabled = batchingEnabled;
        // Optional: If batching is disabled, reset workers to 0/1 and batch size to 1
        // to simplify the runInference decision logic.
        if (!batchingEnabled) {
            this.batchSize = DEFAULT_BATCH_SIZE;
            this.numPrefetchWorkers = DEFAULT_PREFETCH_WORKERS;
        }
    }
}
