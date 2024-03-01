package fiji.plugin.imaging_fcs.new_imfcs.model;

/**
 * The OptionsModel class represents the configuration options for imaging FCS analysis.
 * It stores user preferences for plotting various curves and histograms, as well as
 * the option to use GPU acceleration if CUDA is available.
 */
public class OptionsModel {
    private final boolean isCuda;
    private boolean plotACFCurves;
    private boolean plotSDCurves;
    private boolean plotIntensityCurves;
    private boolean plotResCurves;
    private boolean plotParaHist;
    private boolean plotBlockingCurve;
    private boolean plotCovMats;
    private boolean useGpu;

    /**
     * Constructs an OptionsModel with CUDA availability.
     * Initializes plotting options to default values and sets GPU usage based on CUDA availability.
     *
     * @param isCuda Indicates whether CUDA is available for GPU acceleration.
     */
    public OptionsModel(boolean isCuda) {
        this.isCuda = isCuda;

        // By default, we use Cuda if a GPU is detected
        this.useGpu = isCuda;

        // set default values
        this.plotACFCurves = true;
        this.plotSDCurves = true;
        this.plotIntensityCurves = true;
        this.plotResCurves = true;
        this.plotParaHist = true;
        this.plotBlockingCurve = false;
        this.plotCovMats = false;
    }

    // Getters and setters follows, setUseGpu is the only one with a specific behavior
    public boolean isPlotACFCurves() {
        return plotACFCurves;
    }

    public void setPlotACFCurves(boolean plotACFCurves) {
        this.plotACFCurves = plotACFCurves;
    }

    public boolean isPlotSDCurves() {
        return plotSDCurves;
    }

    public void setPlotSDCurves(boolean plotSDCurves) {
        this.plotSDCurves = plotSDCurves;
    }

    public boolean isPlotIntensityCurves() {
        return plotIntensityCurves;
    }

    public void setPlotIntensityCurves(boolean plotIntensityCurves) {
        this.plotIntensityCurves = plotIntensityCurves;
    }

    public boolean isPlotResCurves() {
        return plotResCurves;
    }

    public void setPlotResCurves(boolean plotResCurves) {
        this.plotResCurves = plotResCurves;
    }

    public boolean isPlotParaHist() {
        return plotParaHist;
    }

    public void setPlotParaHist(boolean plotParaHist) {
        this.plotParaHist = plotParaHist;
    }

    public boolean isPlotBlockingCurve() {
        return plotBlockingCurve;
    }

    public void setPlotBlockingCurve(boolean plotBlockingCurve) {
        this.plotBlockingCurve = plotBlockingCurve;
    }

    public boolean isPlotCovMats() {
        return plotCovMats;
    }

    public void setPlotCovMats(boolean plotCovMats) {
        this.plotCovMats = plotCovMats;
    }

    public boolean isUseGpu() {
        return useGpu;
    }

    /**
     * Sets whether to use GPU for computations, considering CUDA availability.
     * The GPU will only be used if CUDA was detected at initialization.
     *
     * @param useGpu true to enable GPU usage, false to disable.
     */
    public void setUseGpu(boolean useGpu) {
        // useGpu can only be set to true if Cuda was detected
        this.useGpu = useGpu && isCuda;
    }

    public boolean isCuda() {
        return isCuda;
    }
}
