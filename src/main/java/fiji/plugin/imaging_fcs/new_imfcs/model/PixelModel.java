package fiji.plugin.imaging_fcs.new_imfcs.model;

import fiji.plugin.imaging_fcs.new_imfcs.utils.Pair;

import java.util.function.Function;
import java.util.stream.IntStream;

import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.DIFFUSION_COEFFICIENT_BASE;
import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;

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
    private double[] acf;
    private double[] varianceAcf;
    private double[] standardDeviationAcf;
    private double[] fittedAcf;
    private double[] residuals;
    private double[] MSD;
    private double chi2 = 0;
    private boolean fitted = false;
    private int blocked;
    private FitParameters fitParams;


    /**
     * Constructs a new PixelModel instance.
     */
    public PixelModel() {
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
            case "Sqrt(vx^2+vy^2)":
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
     * Adds the values of another PixelModel to this one using a sliding window approach.
     *
     * @param other The other PixelModel whose values are to be added.
     */
    public void addPixelModelSlidingWindow(PixelModel other) {
        if (acf == null || standardDeviationAcf == null || varianceAcf == null) {
            acf = other.acf;
            varianceAcf = other.varianceAcf;
            standardDeviationAcf = other.standardDeviationAcf;
        } else {
            for (int i = 0; i < acf.length; i++) {
                acf[i] += other.acf[i];
                varianceAcf[i] += other.varianceAcf[i];
                standardDeviationAcf[i] += other.standardDeviationAcf[i];
            }
        }
    }

    /**
     * Averages the values in the sliding window over a given number of windows.
     *
     * @param numSlidingWindow The number of sliding windows to average over.
     */
    public void averageSlidingWindow(int numSlidingWindow) {
        for (int i = 0; i < acf.length; i++) {
            acf[i] /= numSlidingWindow;
            varianceAcf[i] /= numSlidingWindow;
            standardDeviationAcf[i] /= numSlidingWindow;
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

    public double[] getAcf() {
        return acf;
    }

    public void setAcf(double[] acf) {
        this.acf = acf;
    }

    public double[] getStandardDeviationAcf() {
        return standardDeviationAcf;
    }

    public void setStandardDeviationAcf(double[] standardDeviationAcf) {
        this.standardDeviationAcf = standardDeviationAcf;
    }

    public double[] getFittedAcf() {
        return fittedAcf;
    }

    public void setFittedAcf(double[] fittedAcf) {
        this.fittedAcf = fittedAcf;
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
        this.fitParams = fitParams;
    }

    public double[] getVarianceAcf() {
        return varianceAcf;
    }

    public void setVarianceAcf(double[] varianceAcf) {
        this.varianceAcf = varianceAcf;
    }

    public double getChi2() {
        return chi2;
    }

    public void setChi2(double chi2) {
        this.chi2 = chi2;
    }

    public boolean isFitted() {
        return fitted;
    }

    public void setFitted(boolean fitted) {
        this.fitted = fitted;
    }

    /**
     * The FitParameters class encapsulates the fitting parameters for a pixel.
     */
    public static class FitParameters {
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
