package fiji.plugin.imaging_fcs.imfcs.model;

import fiji.plugin.imaging_fcs.imfcs.controller.InvalidUserInputException;
import fiji.plugin.imaging_fcs.imfcs.enums.FitFunctions;
import fiji.plugin.imaging_fcs.imfcs.model.fit.BayesFit;
import fiji.plugin.imaging_fcs.imfcs.model.fit.GLSFit;
import fiji.plugin.imaging_fcs.imfcs.model.fit.StandardFit;

import java.util.Arrays;
import java.util.function.Function;

import java.util.stream.IntStream;

import static fiji.plugin.imaging_fcs.imfcs.constants.Constants.DIFFUSION_COEFFICIENT_BASE;
import static fiji.plugin.imaging_fcs.imfcs.constants.Constants.PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;

/**
 * The FitModel class encapsulates the parameters and functionality required for
 * performing fits in fluorescence correlation spectroscopy (FCS) data analysis.
 */
public class FitModel {
    private final ExpSettingsModel settings;
    private final FilteringModel chi2Threshold;
    private Parameter D, N, F2, F3, D2, D3, G, vx, vy, fTrip, tTrip;
    private double modProb1, modProb2, modProb3, Q2, Q3;
    private int fitStart, fitEnd;
    private boolean fix = true;
    private boolean GLS = false;
    private boolean bayes = false;

    /**
     * Constructs a new FitModel with the given experimental settings.
     *
     * @param settings The experimental settings.
     */
    public FitModel(ExpSettingsModel settings) {
        this.settings = settings;

        this.chi2Threshold = new FilteringModel(1);

        initValues();
    }

    /**
     * Copy constructor for FitModel. Use a different instance of
     * settings to prevent side effects.
     *
     * @param settings The experimental settings.
     * @param other    The FitModel instance to copy.
     */
    public FitModel(ExpSettingsModel settings, FitModel other) {
        this.settings = settings;

        this.chi2Threshold = new FilteringModel(1);

        this.D = new Parameter(other.D);
        this.N = new Parameter(other.N);
        this.F2 = new Parameter(other.F2);
        this.F3 = new Parameter(other.F3);
        this.D2 = new Parameter(other.D2);
        this.D3 = new Parameter(other.D3);
        this.G = new Parameter(other.G);
        this.vx = new Parameter(other.vx);
        this.vy = new Parameter(other.vy);
        this.fTrip = new Parameter(other.fTrip);
        this.tTrip = new Parameter(other.tTrip);

        this.modProb1 = other.modProb1;
        this.modProb2 = other.modProb2;
        this.modProb3 = other.modProb3;
        this.Q2 = other.Q2;
        this.Q3 = other.Q3;

        this.fitStart = other.fitStart;
        this.fitEnd = other.fitEnd;

        this.fix = other.fix;
        this.GLS = other.GLS;
        this.bayes = other.bayes;
    }

    /**
     * Initializes the parameter values to their default settings.
     */
    private void initValues() {
        D = new Parameter(1 / DIFFUSION_COEFFICIENT_BASE, false, DIFFUSION_COEFFICIENT_BASE);
        N = new Parameter(1, false, 1);
        F2 = new Parameter(0, true, 1);
        F3 = new Parameter(0, true, 1);
        D2 = new Parameter(0, true, DIFFUSION_COEFFICIENT_BASE);
        D3 = new Parameter(0, true, DIFFUSION_COEFFICIENT_BASE);
        vx = new Parameter(0, true, PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR);
        vy = new Parameter(0, true, PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR);
        G = new Parameter(0, false, 1);
        fTrip = new Parameter(0, true, 1);
        tTrip = new Parameter(0, true, PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR);

        Q2 = 1;
        Q3 = 1;
        modProb1 = 0;
        modProb2 = 0;
        modProb3 = 0;

        fitStart = 1;
        fitEnd = settings.getChannelNumber() - 1;
    }

    /**
     * Resets the parameter values to their default settings.
     */
    public void setDefaultValues() {
        D.value = 1 / DIFFUSION_COEFFICIENT_BASE;
        N.value = 1;
        F2.value = 0;
        F3.value = 0;
        D2.value = 0;
        D3.value = 0;
        vx.value = 0;
        vy.value = 0;
        G.value = 0;
        fTrip.value = 0;
        tTrip.value = 0;

        Q2 = 1;
        Q3 = 1;
        modProb1 = 0;
        modProb2 = 0;
        modProb3 = 0;

        fitStart = 1;
        fitEnd = settings.getChannelNumber() - 1;
    }

    /**
     * Resets the fit end position to the maximum channel number.
     */
    public void resetFitEnd() {
        fitEnd = settings.getChannelNumber() - 1;
    }

    /**
     * Retrieves the values of parameters that are not held (fixed).
     *
     * @return An array of parameter values that are not held.
     */
    public double[] getNonHeldParameterValues() {
        Parameter[] parameters = {N, D, vx, vy, G, F2, D2, F3, D3, fTrip, tTrip};

        return Arrays.stream(parameters)
                .filter(parameter -> !parameter.isHeld())
                .mapToDouble(Parameter::getValue)
                .toArray();
    }


    /**
     * Retrieves the values of parameters that are not held (fixed) from a given PixelModel.
     *
     * @param fitParameters The fit parameters from a PixelModel instance.
     * @return An array of parameter values that are not held.
     */
    public double[] getNonHeldParameterValuesFromPixelModel(PixelModel.FitParameters fitParameters) {
        Parameter[] paramArray = {N, D, vx, vy, G, F2, D2, F3, D3, fTrip, tTrip};

        // Corresponding FitParameters "get" methods in the same order
        double[] paramValues = {
                fitParameters.getN(),
                fitParameters.getD(),
                fitParameters.getVx(),
                fitParameters.getVy(),
                fitParameters.getG(),
                fitParameters.getF2(),
                fitParameters.getD2(),
                fitParameters.getF3(),
                fitParameters.getD3(),
                fitParameters.getFTrip(),
                fitParameters.getTTrip()
        };

        // Filter out all parameters that are held
        return IntStream.range(0, paramArray.length)
                        .filter(i -> !paramArray[i].isHeld())
                        .mapToDouble(i -> paramValues[i])
                        .toArray();
    }

    /**
     * Retrieves the values of all parameters as an array of floats.
     *
     * @return A float array containing the values of all parameters.
     */
    public float[] getFloatParameterValues() {
        Parameter[] parameters = {N, D, vx, vy, G, F2, D2, F3, D3, fTrip, tTrip};

        float[] values = new float[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            values[i] = (float) parameters[i].getValue();
        }

        return values;

    }

    /**
     * Retrieves a boolean array indicating which parameters should be fitted.
     *
     * @return A boolean array representing the fit state of each parameter.
     */
    public boolean[] getParametersToFit() {
        Parameter[] parameters = {N, D, vx, vy, G, F2, D2, F3, D3, fTrip, tTrip};

        boolean[] parametersToFit = new boolean[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            parametersToFit[i] = !parameters[i].isHeld();
        }

        return parametersToFit;
    }

    /**
     * Fills an array of parameters with given values, considering whether each parameter is held.
     *
     * @param params The array of parameter values to fill.
     * @return An array of filled parameter values.
     */
    public double[] fillParamsArray(double[] params) {
        Parameter[] parameters = {N, D, vx, vy, G, F2, D2, F3, D3, fTrip, tTrip};
        double[] paramsFilled = new double[parameters.length];

        int paramIndex = 0;
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].isHeld()) {
                paramsFilled[i] = parameters[i].value;
            } else {
                paramsFilled[i] = params[paramIndex++];
            }
        }

        return paramsFilled;
    }

    /**
     * Filters an array of fit parameters to include only those that are not held.
     *
     * @param fitParams The array of fit parameters.
     * @return An array of filtered fit parameters.
     */
    public double[] filterFitArray(double[] fitParams) {
        Parameter[] parameters = {N, D, vx, vy, G, F2, D2, F3, D3, fTrip, tTrip};

        return Arrays.stream(parameters)
                .filter(parameter -> !parameter.isHeld())
                .mapToDouble(parameter -> fitParams[Arrays.asList(parameters).indexOf(parameter)])
                .toArray();
    }

    /**
     * Resets all parameter thresholds in the model to their default values.
     * <p>
     * This method iterates through a predefined set of parameters, resetting each one's
     * threshold to its default state, and also resets the chi-squared threshold.
     * </p>
     */
    public void resetFilters() {
        Parameter[] parameters = {N, D, vx, vy, G, F2, D2, F3, D3, fTrip, tTrip};

        Arrays.stream(parameters).forEach(parameter -> parameter.getThreshold().setDefault());
        chi2Threshold.setDefault();
        FilteringModel.setFilteringBinaryImage(null);
    }

    /**
     * Activates or deactivates the ACF thresholds for all parameters and chi2.
     * This method sets the active state of ACF-related thresholds across all parameters,
     * enabling or disabling them based on the provided boolean value.
     *
     * @param acfsActive {@code true} to activate ACF thresholds, {@code false} to deactivate them.
     */
    public void setAllAcfsThreshold(boolean acfsActive) {
        Parameter[] parameters = {N, D, vx, vy, G, F2, D2, F3, D3, fTrip, tTrip};

        Arrays.stream(parameters).forEach(parameter -> parameter.getThreshold().setAcfActive(acfsActive));
        chi2Threshold.setAcfActive(acfsActive);
    }

    /**
     * Checks if there are any parameters that can be fit.
     *
     * @return true if there are parameters to fit, false otherwise.
     */
    public boolean canFit() {
        return getNonHeldParameterValues().length > 0;
    }

    /**
     * Updates the parameter values based on the given fit parameters.
     *
     * @param parameters The fit parameters.
     */
    public void updateParameterValues(PixelModel.FitParameters parameters) {
        D.value = parameters.getD();
        N.value = parameters.getN();
        F2.value = parameters.getF2();
        F3.value = parameters.getF3();
        D2.value = parameters.getD2();
        D3.value = parameters.getD3();
        vx.value = parameters.getVx();
        vy.value = parameters.getVy();
        G.value = parameters.getG();
        fTrip.value = parameters.getFTrip();
        tTrip.value = parameters.getTTrip();
    }

    /**
     * Performs the fitting operation on the given pixel model, lag times, and covariance matrix.
     * Returns the model probabilities if Bayesian fitting is used, otherwise returns null.
     *
     * @param pixelModel       The pixel model to fit.
     * @param modelName        The name of the model to use for fitting.
     * @param lagTimes         The lag times for fitting.
     * @param covarianceMatrix The covariance matrix used for fitting.
     * @return A double array of model probabilities if Bayesian fitting is used, otherwise null.
     */
    public double[] fit(PixelModel pixelModel, FitFunctions modelName, double[] lagTimes, double[][] covarianceMatrix) {
        if (bayes) {
            BayesFit fitter = new BayesFit(this, settings);
            return fitter.bayesFit(pixelModel, modelName, lagTimes, covarianceMatrix);
        } else if (GLS) {
            GLSFit fitter = new GLSFit(this, settings, modelName, lagTimes, pixelModel.getCorrelationFunction(),
                    covarianceMatrix);
            fitter.fitPixel(pixelModel, lagTimes);
        } else {
            StandardFit fitter = new StandardFit(this, settings, modelName);
            fitter.fitPixel(pixelModel, lagTimes);
        }

        return null;
    }

    /**
     * Performs the standard fitting operation on the given pixel model and lag times.
     *
     * @param pixelModel The pixel model to fit.
     * @param modelName  The name of the model to use for fitting.
     * @param lagTimes   The lag times for fitting.
     */
    public void standardFit(PixelModel pixelModel, FitFunctions modelName, double[] lagTimes) {
        StandardFit fitter = new StandardFit(this, settings, modelName);
        fitter.fitPixel(pixelModel, lagTimes);
    }

    /**
     * Computes theoretical fit values for the given pixel model using current parameters
     * and specified lag times, storing the results within the pixel model.
     *
     * @param pixelModel The pixel model to fit.
     * @param lagTimes   The lag times for calculating fit values.
     */
    public void theoreticalFit(PixelModel pixelModel, double[] lagTimes) {
        StandardFit fitter = new StandardFit(this, settings, settings.getFitModel());
        fitter.theoreticalFit(pixelModel, lagTimes);
    }

    /**
     * Computes the fitted correlation function for a given pixel model.
     *
     * @param pixelModel The pixel model for which to compute the fitted correlation function.
     * @param lagTimes   The array of lag times used in the computation.
     */
    public void calculateFittedCorrelationFunction(PixelModel pixelModel, double[] lagTimes) {
        StandardFit fitter = new StandardFit(this, settings, settings.getFitModel());
        fitter.calculateFittedCorrelationFunction(pixelModel, lagTimes);
    }

    /**
     * Updates the minimum and maximum values of the thresholds for various fit parameters
     * based on the provided {@code PixelModel}'s fit parameters.
     *
     * @param pixelModel   The {@code PixelModel} containing the fit parameters to update thresholds with.
     * @param getThreshold A function that takes a {@code FilteringModel} and returns the appropriate threshold model.
     */
    private void updateParameterThreshold(PixelModel pixelModel,
                                          Function<FilteringModel, FilteringModel> getThreshold) {
        PixelModel.FitParameters fitParameters = pixelModel.getFitParams();

        // Update thresholds for all relevant parameters
        getThreshold.apply(D.getThreshold()).updateMinMax(fitParameters.getD());
        getThreshold.apply(N.getThreshold()).updateMinMax(fitParameters.getN());
        getThreshold.apply(F2.getThreshold()).updateMinMax(fitParameters.getF2());
        getThreshold.apply(F3.getThreshold()).updateMinMax(fitParameters.getF3());
        getThreshold.apply(D2.getThreshold()).updateMinMax(fitParameters.getD2());
        getThreshold.apply(D3.getThreshold()).updateMinMax(fitParameters.getD3());
        getThreshold.apply(G.getThreshold()).updateMinMax(fitParameters.getG());
        getThreshold.apply(vx.getThreshold()).updateMinMax(fitParameters.getVx());
        getThreshold.apply(vy.getThreshold()).updateMinMax(fitParameters.getVy());
        getThreshold.apply(fTrip.getThreshold()).updateMinMax(fitParameters.getFTrip());
        getThreshold.apply(tTrip.getThreshold()).updateMinMax(fitParameters.getTTrip());

        getThreshold.apply(chi2Threshold).updateMinMax(pixelModel.getChi2());
    }

    /**
     * Updates the thresholds for the given {@code PixelModel} and its associated autocorrelation pixel models.
     * If the pixel model has been fitted, it updates the thresholds for its parameters and for the parameters
     * of its autocorrelation pixel models (ACF1 and ACF2) if they exist.
     *
     * @param pixelModel The {@code PixelModel} whose thresholds are to be updated.
     */
    public void updateThresholds(PixelModel pixelModel) {
        if (pixelModel.isFitted()) {
            // Update thresholds for the main PixelModel
            updateParameterThreshold(pixelModel, Function.identity());

            // Update thresholds for ACF1 and ACF2 if they exist
            if (pixelModel.getAcf1PixelModel() != null) {
                updateParameterThreshold(pixelModel.getAcf1PixelModel(), FilteringModel::getAcfThreshold);
                updateParameterThreshold(pixelModel.getAcf2PixelModel(), FilteringModel::getAcfThreshold);
            }
        }
    }

    public Parameter getD() {
        return D;
    }

    public void setD(String D) {
        double newValue = Double.parseDouble(D);
        if (newValue < 0) {
            throw new InvalidUserInputException("D must be positive.");
        }
        this.D.value = newValue / DIFFUSION_COEFFICIENT_BASE;
    }

    public double getDInterface() {
        return D.value * DIFFUSION_COEFFICIENT_BASE;
    }

    public Parameter getN() {
        return N;
    }

    public void setN(String N) {
        this.N.value = Double.parseDouble(N);
    }

    public Parameter getVx() {
        return vx;
    }

    public void setVx(String vx) {
        this.vx.value = Double.parseDouble(vx) / PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
    }

    public double getVxInterface() {
        return vx.value * PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
    }

    public Parameter getVy() {
        return vy;
    }

    public void setVy(String vy) {
        this.vy.value = Double.parseDouble(vy) / PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
    }

    public double getVyInterface() {
        return vy.value * PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
    }

    public Parameter getG() {
        return G;
    }

    public void setG(String G) {
        this.G.value = Double.parseDouble(G);
    }

    public Parameter getF2() {
        return F2;
    }

    public void setF2(String F2) {
        double newValue = Double.parseDouble(F2);
        if (newValue < 0) {
            throw new InvalidUserInputException("F2 must be positive.");
        }
        this.F2.value = newValue;
    }

    public double getQ2() {
        return Q2;
    }

    public void setQ2(String Q2) {
        double newValue = Double.parseDouble(Q2);
        if (newValue < 0) {
            throw new InvalidUserInputException("Q2 must be superior to 0");
        }
        this.Q2 = newValue;
    }

    public Parameter getF3() {
        return F3;
    }

    public void setF3(String F3) {
        double newValue = Double.parseDouble(F3);
        if (newValue < 0) {
            throw new InvalidUserInputException("F3 must be positive.");
        }
        this.F3.value = newValue;
    }

    public Parameter getD2() {
        return D2;
    }

    public void setD2(String D2) {
        double newValue = Double.parseDouble(D2);
        if (newValue < 0) {
            throw new InvalidUserInputException("D2 must be positive.");
        }
        this.D2.value = newValue / DIFFUSION_COEFFICIENT_BASE;
    }

    public double getD2Interface() {
        return D2.value * DIFFUSION_COEFFICIENT_BASE;
    }

    public Parameter getD3() {
        return D3;
    }

    public void setD3(String D3) {
        double newValue = Double.parseDouble(D3);
        if (newValue < 0) {
            throw new InvalidUserInputException("D3 must be positive.");
        }
        this.D3.value = newValue / DIFFUSION_COEFFICIENT_BASE;
    }

    public double getD3Interface() {
        return D3.value * DIFFUSION_COEFFICIENT_BASE;
    }

    public Parameter getFTrip() {
        return fTrip;
    }

    public void setFTrip(String fTrip) {
        this.fTrip.value = Double.parseDouble(fTrip);
    }

    public Parameter getTTrip() {
        return tTrip;
    }

    public void setTTrip(String tTrip) {
        this.tTrip.value = Double.parseDouble(tTrip);
    }

    public double getTTripInterface() {
        return tTrip.value * PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
    }

    public double getModProb1() {
        return modProb1;
    }

    public void setModProb1(String modProb1) {
        this.modProb1 = Double.parseDouble(modProb1);
    }

    public double getModProb2() {
        return modProb2;
    }

    public void setModProb2(String modProb2) {
        this.modProb2 = Double.parseDouble(modProb2);
    }

    public double getModProb3() {
        return modProb3;
    }

    public void setModProb3(String modProb3) {
        this.modProb3 = Double.parseDouble(modProb3);
    }

    public int getFitStart() {
        return fitStart;
    }

    public void setFitStart(String fitStart) {
        int newValue = Integer.parseInt(fitStart);
        if (newValue < 1 || newValue >= settings.getChannelNumber()) {
            throw new InvalidUserInputException(String.format("Fit Start must be between 0 and the channel number (%d)",
                    settings.getChannelNumber()));
        }

        this.fitStart = newValue;
    }

    public int getFitEnd() {
        return fitEnd;
    }

    public void setFitEnd(String fitEnd) {
        int newValue = Integer.parseInt(fitEnd);
        if (newValue < fitStart || newValue >= settings.getChannelNumber()) {
            throw new InvalidUserInputException(
                    String.format("Fit End must be between Fit Start and the channel number (%d)",
                            settings.getChannelNumber()));
        }

        this.fitEnd = newValue;
    }

    public double getQ3() {
        return Q3;
    }

    public void setQ3(String Q3) {
        double newValue = Double.parseDouble(Q3);
        if (newValue < 0) {
            throw new InvalidUserInputException("Q3 must be superior to 0");
        }
        this.Q3 = newValue;
    }

    public boolean isFix() {
        return fix;
    }

    public void setFix(boolean fix) {
        this.fix = fix;
    }

    public boolean isGLS() {
        return GLS;
    }

    public void setGLS(boolean GLS) {
        this.GLS = GLS;
    }

    public boolean isBayes() {
        return bayes;
    }

    public void setBayes(boolean bayes) {
        this.bayes = bayes;
    }

    public FilteringModel getChi2Threshold() {
        return chi2Threshold;
    }

    /**
     * The {@code Parameter} class encapsulates a parameter with a value, a hold state,
     * and an associated {@code Threshold} object that defines the constraints (min, max)
     * and active state for the parameter during fitting operations.
     */
    public static class Parameter {
        private final FilteringModel threshold;
        private double value;
        private boolean hold;

        /**
         * Constructs a new Parameter with the given value and hold state.
         *
         * @param value       The value of the parameter.
         * @param hold        The hold state of the parameter.
         * @param fieldFactor The factor by which min and max values are adjusted.
         */
        public Parameter(double value, boolean hold, double fieldFactor) {
            this.value = value;
            this.hold = hold;
            this.threshold = new FilteringModel(fieldFactor);
        }

        /**
         * Copy constructor for {@code Parameter}.
         *
         * @param other The {@code Parameter} instance to copy.
         */
        public Parameter(Parameter other) {
            this.value = other.value;
            this.hold = other.hold;
            this.threshold = other.threshold;
        }

        public double getValue() {
            return value;
        }

        public void setValue(double value) {
            this.value = value;
        }

        public boolean isHeld() {
            return hold;
        }

        public void setHold(boolean hold) {
            this.hold = hold;
        }

        public FilteringModel getThreshold() {
            return threshold;
        }

        @Override
        public String toString() {
            return String.valueOf(value);
        }
    }
}
