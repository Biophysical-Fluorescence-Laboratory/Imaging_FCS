package fiji.plugin.imaging_fcs.imfcs.model;

import fiji.plugin.imaging_fcs.imfcs.utils.Pair;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.util.Arrays;
import java.util.function.Function;
import java.util.stream.IntStream;

import static fiji.plugin.imaging_fcs.imfcs.constants.Constants.DIFFUSION_COEFFICIENT_BASE;
import static fiji.plugin.imaging_fcs.imfcs.constants.Constants.PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;

/**
 * The PixelModel class represents a model for handling pixel data in fluorescence correlation spectroscopy (FCS)
 * analysis.
 * It includes methods for setting and getting various attributes related to autocorrelation function (ACF), variance,
 * standard deviation, and fitting parameters.
 */
public class PixelModel {
    public static final String[] paramsName = {
            "N",
            "D",
            "vx",
            "vy",
            "G",
            "F2",
            "D2",
            "F3",
            "D3",
            "FTrip",
            "TTrip",
            "reduced Chi2",
            "blocked",
            "valid pixels"
    };
    private double[] CorrelationFunction;
    private double[] varianceCF;
    private double[] standardDeviationCF;
    private double[] fittedCF;
    private double[] residuals;
    private double[] MSD;
    private double chi2 = 0;
    private boolean fitted = false;
    // blocked is 1 if blocking is successful and 0 if maximum blocking is used
    private int blocked;
    private FitParameters fitParams;
    private PixelModel acf1PixelModel = null;
    private PixelModel acf2PixelModel = null;


    /**
     * Constructs a new PixelModel instance.
     */
    public PixelModel() {
    }

    /**
     * Copy constructor that creates a new PixelModel by copying only the non-fit parameters
     * (CorrelationFunction, varianceCF, standardDeviationCF, and ACF models) from the specified model.
     * This is used for performing theoretical fit.
     *
     * @param other The PixelModel to copy from.
     */
    public PixelModel(PixelModel other) {
        this.CorrelationFunction = other.CorrelationFunction;
        this.varianceCF = other.varianceCF;
        this.standardDeviationCF = other.standardDeviationCF;

        acf1PixelModel = other.acf1PixelModel;
        acf2PixelModel = other.acf2PixelModel;
    }

    /**
     * Returns a function that retrieves a specific parameter from a FitParameters instance.
     *
     * @param param The name of the parameter to retrieve.
     * @return A function that takes a FitParameters instance and returns the value of the specified parameter.
     */
    private static Function<FitParameters, Double> getParamFromString(String param) {
        switch (param) {
            case "N":
                return FitParameters::getN;
            case "D":
                return FitParameters::getDInterface;
            case "F2":
                return FitParameters::getF2;
            case "D2":
                return FitParameters::getD2Interface;
            case "N*(1-F2)":
                return fp -> fp.getN() * (1 - fp.getF2());
            case "N*F2":
                return fp -> fp.getN() * fp.getF2();
            case "Sqrt(vx²+vy²)":
                return fp -> Math.sqrt(Math.pow(fp.getVxInterface(), 2) + Math.pow(fp.getVyInterface(), 2));
            default:
                throw new RuntimeException("The scatter method doesn't exist for this param " + param);
        }
    }

    /**
     * Generates a scatter plot array based on the specified mode and pixel data.
     *
     * @param pixels A 2D array of PixelModel instances.
     * @param mode   The mode specifying which parameters to plot. Should be in the format "param1 vs param2".
     * @return A pair containing the scatter plot data and the parameter names used for the plot.
     */
    public static Pair<double[][], String[]> getScatterPlotArray(PixelModel[][] pixels, String mode) {
        String[] params = mode.split(" vs ");
        Function<FitParameters, Double> getter1 = getParamFromString(params[0]);
        Function<FitParameters, Double> getter2 = getParamFromString(params[1]);

        int rows = pixels.length;
        int cols = pixels[0].length;
        double[][] scPlot = new double[2][rows * cols];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                int index = i * cols + j;
                if (pixels[i][j] != null && pixels[i][j].getFitParams() != null) {
                    scPlot[0][index] = getter1.apply(pixels[i][j].getFitParams());
                    scPlot[1][index] = getter2.apply(pixels[i][j].getFitParams());
                } else {
                    scPlot[0][index] = Double.NaN;
                    scPlot[1][index] = Double.NaN;
                }
            }
        }

        return new Pair<>(scPlot, params);
    }

    /**
     * Extracts a 2D array of `PixelModel` objects by applying a getter function to each element.
     *
     * @param pixelModels the original 2D array of `PixelModel` instances
     * @param getter      a function that retrieves a `PixelModel` from a given `PixelModel`
     * @return a new 2D array containing the result of applying the getter to each non-null element
     */
    public static PixelModel[][] extractAcfPixelModels(PixelModel[][] pixelModels,
                                                       Function<PixelModel, PixelModel> getter) {
        return Arrays.stream(pixelModels)
                .map(row -> Arrays.stream(row)
                        .map(pixel -> pixel != null ? getter.apply(pixel) : null)
                        .toArray(PixelModel[]::new))
                .toArray(PixelModel[][]::new);
    }

    /**
     * Checks if any PixelModel in the given 2D array has been fitted.
     * Iterates through the array and returns true if at least one PixelModel is not null and is fitted.
     *
     * @param pixelModels a 2D array of PixelModel objects to be checked
     * @return true if any PixelModel in the array is fitted, false otherwise
     */
    public static boolean anyPixelFit(PixelModel[][] pixelModels) {
        for (PixelModel[] pixelModelsRow : pixelModels) {
            for (PixelModel pixelModel : pixelModelsRow) {
                if (pixelModel != null && pixelModel.isAtLeastOneFitted()) {
                    return true;
                }
            }
        }

        return false;
    }

    /**
     * Determines whether the given pixel model should be filtered based on the thresholds
     * defined in the {@code FitModel}. This method applies filtering criteria to each parameter
     * using the provided threshold transformation function.
     *
     * @param pixelModel   The pixel model to be checked for filtering.
     * @param model        The fit model containing the parameters and their thresholds.
     * @param getThreshold A function that applies a transformation to the thresholds (e.g., switching between
     *                     the default threshold and an ACF threshold).
     * @return {@code true} if the pixel model should be filtered, {@code false} otherwise.
     */
    private static boolean toFilterPixelModel(PixelModel pixelModel, FitModel model,
                                              Function<FilteringModel, FilteringModel> getThreshold) {
        return pixelModel.fitParams != null &&
                (getThreshold.apply(model.getN().getThreshold()).toFilter(pixelModel.fitParams.getN()) ||
                        getThreshold.apply(model.getD().getThreshold()).toFilter(pixelModel.fitParams.getD()) ||
                        getThreshold.apply(model.getF2().getThreshold()).toFilter(pixelModel.fitParams.getF2()) ||
                        getThreshold.apply(model.getD2().getThreshold()).toFilter(pixelModel.fitParams.getD2()) ||
                        getThreshold.apply(model.getF3().getThreshold()).toFilter(pixelModel.fitParams.getF3()) ||
                        getThreshold.apply(model.getD3().getThreshold()).toFilter(pixelModel.fitParams.getD3()) ||
                        getThreshold.apply(model.getG().getThreshold()).toFilter(pixelModel.fitParams.getG()) ||
                        getThreshold.apply(model.getVx().getThreshold()).toFilter(pixelModel.fitParams.getVx()) ||
                        getThreshold.apply(model.getVy().getThreshold()).toFilter(pixelModel.fitParams.getVy()) ||
                        getThreshold.apply(model.getFTrip().getThreshold()).toFilter(pixelModel.fitParams.getFTrip()) ||
                        getThreshold.apply(model.getTTrip().getThreshold()).toFilter(pixelModel.fitParams.getTTrip()) ||
                        getThreshold.apply(model.getChi2Threshold()).toFilter(pixelModel.chi2));
    }

    /**
     * Adds the values of another PixelModel to this one using a sliding window approach.
     *
     * @param other The other PixelModel whose values are to be added.
     */
    public void addPixelModelSlidingWindow(PixelModel other) {
        if (CorrelationFunction == null || standardDeviationCF == null || varianceCF == null) {
            CorrelationFunction = other.CorrelationFunction;
            varianceCF = other.varianceCF;
            standardDeviationCF = other.standardDeviationCF;
        } else {
            for (int i = 0; i < CorrelationFunction.length; i++) {
                CorrelationFunction[i] += other.CorrelationFunction[i];
                varianceCF[i] += other.varianceCF[i];
                standardDeviationCF[i] += other.standardDeviationCF[i];
            }
        }
    }

    /**
     * Averages the values in the sliding window over a given number of windows.
     *
     * @param numSlidingWindow The number of sliding windows to average over.
     */
    public void averageSlidingWindow(int numSlidingWindow) {
        for (int i = 0; i < CorrelationFunction.length; i++) {
            CorrelationFunction[i] /= numSlidingWindow;
            varianceCF[i] /= numSlidingWindow;
            standardDeviationCF[i] /= numSlidingWindow;
        }
    }

    /**
     * Retrieves an array of parameter pairs.
     * This method constructs an array of {@link Pair} objects, where each pair consists of a parameter name (as a
     * {@link String}) and its corresponding value (as a {@link Double}). The parameter names are predefined in the
     * `paramsName` array, and the corresponding values are retrieved from the `fitParams` object.
     *
     * @return An array of {@link Pair} objects, each containing a parameter name and its corresponding value.
     */
    public Pair<String, Double>[] getParams() {
        double[] params = {
                fitParams.N,
                fitParams.D,
                fitParams.vx,
                fitParams.vy,
                fitParams.G,
                fitParams.F2,
                fitParams.D2,
                fitParams.F3,
                fitParams.D3,
                fitParams.fTrip,
                fitParams.tTrip,
                chi2,
                blocked,
                fitted ? 1.0 : 0.0,
                };

        @SuppressWarnings("unchecked") Pair<String, Double>[] pairs = IntStream.range(0, paramsName.length)
                .mapToObj(i -> new Pair<>(paramsName[i], params[i]))
                .toArray(Pair[]::new);

        return pairs;
    }

    /**
     * Determines whether the current pixel model should be filtered based on the provided {@code FitModel} settings.
     * <p>
     * This method evaluates the pixel model using the following criteria:
     * <ul>
     *     <li>If a binary filtering image is present in the {@code FitModel}, the pixel is filtered based on the pixel
     *     value at its coordinates in the binary image. If the pixel value is 0, the pixel is flagged for filtering
     *     .</li>
     *     <li>If no binary image is used, the method checks if any fit parameters exceed their respective thresholds
     *     defined in the {@code FitModel}. The pixel model and its related ACF pixel models (if present) are checked
     *     against these thresholds. If any threshold is exceeded, the pixel model is flagged for filtering.</li>
     * </ul>
     * </p>
     *
     * @param model The {@code FitModel} containing threshold settings and optional binary filtering image.
     * @param x     The x-coordinate of the pixel.
     * @param y     The y-coordinate of the pixel.
     * @return {@code true} if the pixel model should be filtered, {@code false} otherwise.
     */
    public boolean toFilter(FitModel model, int x, int y) {
        ImagePlus filteringImage = FilteringModel.getFilteringBinaryImage();
        if (filteringImage != null) {
            // filter only based on the binary image
            ImageProcessor ip = filteringImage.getImageStack().getProcessor(1);
            int pixelValue = filteringImage.getBitDepth() == 16 ? ip.getPixel(x, y) : (int) ip.getf(x, y);

            // if the pixel value is 0, we need to filter it
            return pixelValue == 0;
        }

        if (acf1PixelModel != null && acf2PixelModel != null) {
            return toFilterPixelModel(this, model, Function.identity()) ||
                    toFilterPixelModel(acf1PixelModel, model, FilteringModel::getAcfThreshold) ||
                    toFilterPixelModel(acf2PixelModel, model, FilteringModel::getAcfThreshold);
        } else {
            return toFilterPixelModel(this, model, Function.identity());
        }
    }

    public double[] getCorrelationFunction() {
        return CorrelationFunction;
    }

    public void setCorrelationFunction(double[] correlationFunction) {
        this.CorrelationFunction = correlationFunction;
    }

    public double[] getStandardDeviationCF() {
        return standardDeviationCF;
    }

    public void setStandardDeviationCF(double[] standardDeviationCF) {
        this.standardDeviationCF = standardDeviationCF;
    }

    public double[] getFittedCF() {
        return fittedCF;
    }

    public void setFittedCF(double[] fittedCF) {
        this.fittedCF = fittedCF;
    }

    public double[] getResiduals() {
        return residuals;
    }

    public void setResiduals(double[] residuals) {
        this.residuals = residuals;
    }

    public double[] getMSD() {
        return MSD;
    }

    public void setMSD(double[] MSD) {
        this.MSD = MSD;
    }

    public FitParameters getFitParams() {
        return fitParams;
    }

    public void setFitParams(FitParameters fitParams) {
        this.fitted = true;
        this.fitParams = fitParams;
    }

    public double[] getVarianceCF() {
        return varianceCF;
    }

    public void setVarianceCF(double[] varianceCF) {
        this.varianceCF = varianceCF;
    }

    public double getChi2() {
        return chi2;
    }

    public void setChi2(double chi2) {
        this.chi2 = chi2;
    }

    public int getBlocked() {
        return blocked;
    }

    public void setBlocked(int blocked) {
        this.blocked = blocked;
    }

    public boolean isFitted() {
        return fitted;
    }

    /**
     * Check if at least one of the pixel model (main pixel model, acf1 pixel model,
     * acf2 pixel model) is fitted
     * @return true if at least one is fitted
     */
    public boolean isAtLeastOneFitted() {
        boolean acf1fitted = acf1PixelModel != null ? acf1PixelModel.fitted : false;
        boolean acf2fitted = acf2PixelModel != null ? acf2PixelModel.fitted : false;
        return fitted || acf1fitted || acf2fitted;
    }

    public void setFitted(boolean fitted) {
        this.fitted = fitted;
    }

    public PixelModel getAcf1PixelModel() {
        return acf1PixelModel;
    }

    public void setAcf1PixelModel(PixelModel acf1PixelModel) {
        this.acf1PixelModel = acf1PixelModel;
    }

    public PixelModel getAcf2PixelModel() {
        return acf2PixelModel;
    }

    public void setAcf2PixelModel(PixelModel acf2PixelModel) {
        this.acf2PixelModel = acf2PixelModel;
    }

    /**
     * The FitParameters class encapsulates the fitting parameters for a pixel.
     */
    public static class FitParameters {
        public static final int NUM_PARAMETERS = 11;
        private final double N, D, vx, vy, G, F2, D2, F3, D3, fTrip, tTrip;

        /**
         * Constructs a new FitParameters instance with the given parameters and fit model.
         *
         * @param params   The array of parameter values.
         * @param fitModel The fit model.
         */
        public FitParameters(double[] params, FitModel fitModel) {
            N = selectValue(fitModel.getN(), params[0]);
            D = selectValue(fitModel.getD(), params[1]);
            vx = selectValue(fitModel.getVx(), params[2]);
            vy = selectValue(fitModel.getVy(), params[3]);
            G = selectValue(fitModel.getG(), params[4]);
            F2 = selectValue(fitModel.getF2(), params[5]);
            D2 = selectValue(fitModel.getD2(), params[6]);
            F3 = selectValue(fitModel.getF3(), params[7]);
            D3 = selectValue(fitModel.getD3(), params[8]);
            fTrip = selectValue(fitModel.getFTrip(), params[9]);
            tTrip = selectValue(fitModel.getTTrip(), params[10]);
        }

        /**
         * Constructs a new FitParameters instance with the given array of parameter values.
         *
         * @param params The array of parameter values.
         */
        public FitParameters(double[] params) {
            N = params[0];
            D = params[1];
            vx = params[2];
            vy = params[3];
            G = params[4];
            F2 = params[5];
            D2 = params[6];
            F3 = params[7];
            D3 = params[8];
            fTrip = params[9];
            tTrip = params[10];
        }

        /**
         * Selects the value of the parameter, preferring the held value from the fit model if it exists.
         *
         * @param parameter The parameter from the fit model.
         * @param value     The value to select.
         * @return The selected value.
         */
        private double selectValue(FitModel.Parameter parameter, double value) {
            return parameter.isHeld() ? parameter.getValue() : value;
        }

        public double getN() {
            return N;
        }

        public double getD() {
            return D;
        }

        public double getDInterface() {
            return D * DIFFUSION_COEFFICIENT_BASE;
        }

        public double getVx() {
            return vx;
        }

        public double getVxInterface() {
            return vx * PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
        }

        public double getVy() {
            return vy;
        }

        public double getVyInterface() {
            return vy * PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
        }

        public double getG() {
            return G;
        }

        public double getF2() {
            return F2;
        }

        public double getD2() {
            return D2;
        }

        public double getD2Interface() {
            return D2 * DIFFUSION_COEFFICIENT_BASE;
        }

        public double getF3() {
            return F3;
        }

        public double getD3() {
            return D3;
        }

        public double getD3Interface() {
            return D3 * DIFFUSION_COEFFICIENT_BASE;
        }

        public double getFTrip() {
            return fTrip;
        }

        public double getTTrip() {
            return tTrip;
        }

        public double getTTripInterface() {
            return tTrip * PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
        }
    }
}
