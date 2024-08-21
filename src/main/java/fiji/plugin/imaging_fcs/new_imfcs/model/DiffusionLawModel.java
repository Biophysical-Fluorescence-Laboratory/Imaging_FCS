package fiji.plugin.imaging_fcs.new_imfcs.model;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.controller.InvalidUserInputException;
import fiji.plugin.imaging_fcs.new_imfcs.model.correlations.Correlator;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.BleachCorrectionModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.LineFit;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions.FCSFit;
import fiji.plugin.imaging_fcs.new_imfcs.utils.Range;

import java.awt.*;
import java.util.Arrays;

/**
 * The {@code DiffusionLawModel} class represents the data model for diffusion law analysis.
 * It handles data preparation, fitting, and calculation of diffusion law parameters.
 */
public class DiffusionLawModel {
    private static final int MAX_POINTS = 30;
    private static final int DIMENSION_ROI = 7;
    private final ExpSettingsModel interfaceSettings;
    private final FitModel interfaceFitModel;
    private final ImageModel imageModel;
    private final Runnable resetCallback;
    private String mode = "All";
    private int binningStart = 1;
    private int binningEnd = 5;
    private int calculatedBinningStart = -1;
    private int calculatedBinningEnd = -1;
    private int fitStart = 1;
    private int fitEnd = 5;
    private double[] effectiveArea;
    private double[] time;
    private double[] standardDeviation;
    private double minValueDiffusionLaw = Double.MAX_VALUE;
    private double maxValueDiffusionLaw = -Double.MAX_VALUE;
    private double intercept = -1;
    private double slope = -1;

    /**
     * Initializes the model with the provided experimental settings, image data, and fitting model.
     *
     * @param settings      Experimental settings model.
     * @param imageModel    Image model containing the data.
     * @param fitModel      Fitting model used for the correlation data.
     * @param resetCallback Callback to handle resetting results.
     */
    public DiffusionLawModel(ExpSettingsModel settings, ImageModel imageModel, FitModel fitModel,
                             Runnable resetCallback) {
        this.interfaceSettings = settings;
        this.interfaceFitModel = fitModel;
        this.imageModel = imageModel;
        this.resetCallback = resetCallback;
    }

    /**
     * Initializes a {@code Correlator} object for computing correlations in the provided image data.
     *
     * @param settings   the experimental settings model used for the correlation computation.
     * @param fitModel   the fitting model containing parameters for fitting the correlation data.
     * @param imageModel the image model containing the image data to be analyzed.
     * @return a {@code Correlator} initialized with the provided settings and image data.
     */
    private Correlator initCorrelator(ExpSettingsModel settings, FitModel fitModel, ImageModel imageModel) {
        BleachCorrectionModel bleachCorrectionModel = new BleachCorrectionModel(settings, imageModel);
        bleachCorrectionModel.computeNumPointsIntensityTrace(settings.getLastFrame() - settings.getFirstFrame() + 1);
        return new Correlator(settings, bleachCorrectionModel, fitModel);
    }

    /**
     * Fits a pixel at the specified coordinates and updates the diffusion coefficient statistics
     * for the current binning setting.
     *
     * @param settings   the experimental settings model used for fitting.
     * @param fitModel   the fitting model containing parameters for fitting the pixel data.
     * @param correlator the correlator used to compute correlation data for the pixel.
     * @param averageD   Array for average diffusion coefficients.
     * @param varianceD  Array for variances.
     * @param pixelModel the model representing the pixel's data and fit parameters.
     * @param x          the x-coordinate of the pixel to be fitted.
     * @param y          the y-coordinate of the pixel to be fitted.
     * @param index      the index corresponding to the current binning setting.
     * @return 1 if the pixel was successfully fitted, 0 otherwise.
     */
    private int fitPixelAndAddD(ExpSettingsModel settings, FitModel fitModel, Correlator correlator, double[] averageD,
                                double[] varianceD, PixelModel pixelModel, int x, int y, int index) {
        correlator.correlatePixelModel(pixelModel, imageModel.getImage(), x, y, x, y, settings.getFirstFrame(),
                settings.getLastFrame());
        fitModel.standardFit(pixelModel, correlator.getLagTimes());

        if (pixelModel.isFitted()) {
            averageD[index] += pixelModel.getFitParams().getD();
            varianceD[index] += Math.pow(pixelModel.getFitParams().getD(), 2);

            // Return 1 to count this element in the total elements.
            return 1;
        } else {
            return 0;
        }
    }

    /**
     * Calculates the diffusion law by fitting across the specified binning range.
     * Computes observation volumes, average diffusion coefficients, and their variances for each binning setting.
     */
    public void calculateDiffusionLaw() {
        // update the binning calculated
        calculatedBinningStart = binningStart;
        calculatedBinningEnd = binningEnd;

        // create a new settings model to be able to update the binning size separately from the interface.
        ExpSettingsModel settings = new ExpSettingsModel(this.interfaceSettings);

        // init a new fit model based on the interface parameters and fix the parameters
        FitModel fitModel = new FitModel(settings, interfaceFitModel);
        fitModel.setFix(true);

        Correlator correlator = initCorrelator(settings, fitModel, this.imageModel);
        PixelModel pixelModel = new PixelModel();

        double[] observationVolumes = new double[binningEnd - binningStart + 1];
        double[] averageD = new double[binningEnd - binningStart + 1];
        double[] varianceD = new double[binningEnd - binningStart + 1];

        for (int currentBinning = binningStart; currentBinning <= binningEnd; currentBinning++) {
            settings.setBinning(new Point(currentBinning, currentBinning));
            settings.updateSettings();

            Range[] ranges = settings.getAllArea(imageModel.getDimension());
            Range xRange = ranges[0];
            Range yRange = ranges[1];

            int index = currentBinning - binningStart;

            observationVolumes[index] =
                    FCSFit.getFitObservationVolume(settings.getParamAx(), settings.getParamAy(), settings.getParamW()) *
                            Constants.DIFFUSION_COEFFICIENT_BASE;

            int numElements = xRange.stream()
                    .mapToInt(x -> yRange.stream()
                            .mapToInt(y -> fitPixelAndAddD(settings, fitModel, correlator, averageD, varianceD,
                                    pixelModel, x, y, index))
                            .sum())
                    .sum();

            // Do the average and convert to um^2/s
            averageD[index] *= Constants.DIFFUSION_COEFFICIENT_BASE / numElements;
            // normalize the average of the square
            varianceD[index] *= Math.pow(Constants.DIFFUSION_COEFFICIENT_BASE, 2) / numElements;
            varianceD[index] = varianceD[index] - Math.pow(averageD[index], 2);
        }

        computeDiffusionLawParameters(observationVolumes, averageD, varianceD);
    }

    /**
     * Computes diffusion law parameters.
     *
     * @param observationVolumes Array of observation volumes.
     * @param averageD           Array of average diffusion coefficients.
     * @param varianceD          Array of variances.
     */
    private void computeDiffusionLawParameters(double[] observationVolumes, double[] averageD, double[] varianceD) {
        effectiveArea = observationVolumes;
        time = new double[observationVolumes.length];
        standardDeviation = new double[observationVolumes.length];

        minValueDiffusionLaw = Double.MAX_VALUE;
        maxValueDiffusionLaw = -Double.MAX_VALUE;

        for (int currentBinning = binningStart; currentBinning <= binningEnd; currentBinning++) {
            int index = currentBinning - binningStart;
            time[index] = observationVolumes[index] / averageD[index];
            standardDeviation[index] =
                    observationVolumes[index] / Math.pow(averageD[index], 2) * Math.sqrt(varianceD[index]);

            minValueDiffusionLaw = Math.min(minValueDiffusionLaw, averageD[index] - varianceD[index]);
            maxValueDiffusionLaw = Math.max(maxValueDiffusionLaw, averageD[index] + varianceD[index]);
        }
    }

    /**
     * Returns a subarray corresponding to the current fitting range.
     *
     * @param array The source array.
     * @return The subarray within the fitting range.
     */
    private double[] getFitSegment(double[] array) {
        return Arrays.copyOfRange(array, fitStart - calculatedBinningStart, fitEnd - calculatedBinningStart + 1);
    }

    /**
     * Performs a linear fit on the calculated diffusion law data.
     *
     * @return The fitted line data.
     */
    public double[][] fit() {
        if (effectiveArea == null) {
            throw new RuntimeException("Please run the diffusion law calculation before");
        } else if (fitStart < calculatedBinningStart || fitEnd > calculatedBinningEnd) {
            throw new RuntimeException("Fit start/end not are out of ranges");
        }

        double[] segmentEffectiveArea = getFitSegment(effectiveArea);
        double[] segmentTime = getFitSegment(time);
        double[] segmentStandardDeviation = getFitSegment(standardDeviation);

        LineFit lineFit = new LineFit();
        double[] result =
                lineFit.doFit(segmentEffectiveArea, segmentTime, segmentStandardDeviation, fitEnd - fitStart + 1);

        intercept = result[0];
        slope = result[1];

        double[][] fitFunction = new double[2][segmentEffectiveArea.length];
        fitFunction[0] = segmentEffectiveArea;

        for (int i = 0; i < segmentEffectiveArea.length; i++) {
            fitFunction[1][i] = intercept + slope * segmentEffectiveArea[i];
        }

        return fitFunction;
    }

    /**
     * Resets the binning and fitting range values to their default states.
     * This method is typically called when the user switches to ROI (Region of Interest) mode.
     *
     * @param reset if true, resets the range values and sets the mode to "ROI"; otherwise, sets the mode to "All".
     * @return the updated mode string, either "ROI" or "All".
     */
    public String resetRangeValues(boolean reset) {
        if (reset) {
            this.binningEnd = 5;
            this.fitStart = 1;
            this.fitEnd = 5;

            this.mode = "ROI";
        } else {
            this.mode = "All";
        }

        return this.mode;
    }

    /**
     * Resets the calculated results of the diffusion law analysis.
     * <p>
     * This method clears the stored data and resets the calculated binning ranges to their initial states.
     * It is typically called when the user changes the analysis parameters or when a new calculation is initiated.
     */
    public void resetResults() {
        this.calculatedBinningStart = -1;
        this.calculatedBinningEnd = -1;

        minValueDiffusionLaw = Double.MAX_VALUE;
        maxValueDiffusionLaw = -Double.MAX_VALUE;

        effectiveArea = null;
        time = null;
        standardDeviation = null;
    }

    // Getter and setter methods with input validation for binning and fitting ranges.

    public int getBinningStart() {
        return binningStart;
    }

    public void setBinningStart(String binningStart) {
        int start = Integer.parseInt(binningStart);
        if (start <= 0 || start >= this.binningEnd) {
            throw new InvalidUserInputException("Binning start out of range.");
        } else if (calculatedBinningStart != -1 && calculatedBinningStart != start) {
            resetCallback.run();
        }
        this.binningStart = start;
    }

    public int getBinningEnd() {
        return binningEnd;
    }

    public void setBinningEnd(String binningEnd) {
        int end = Integer.parseInt(binningEnd);
        if (end >= MAX_POINTS || end <= this.binningStart) {
            throw new InvalidUserInputException("Binning end out of range");
        } else if (calculatedBinningEnd != -1 && calculatedBinningEnd != end) {
            resetCallback.run();
        }
        this.binningEnd = end;
    }

    public int getFitStart() {
        return fitStart;
    }

    public void setFitStart(String fitStart) {
        int start = Integer.parseInt(fitStart);
        if (start <= 0 || start >= this.fitEnd || start < this.binningStart || start > this.binningEnd) {
            throw new InvalidUserInputException("Fit start out of range.");
        }
        this.fitStart = start;
    }

    public int getFitEnd() {
        return fitEnd;
    }

    public void setFitEnd(String fitEnd) {
        int end = Integer.parseInt(fitEnd);
        if (end <= this.fitStart || end > this.binningEnd) {
            throw new InvalidUserInputException("Fit end out of range.");
        }
        this.fitEnd = end;
    }

    public int getDimensionRoi() {
        return DIMENSION_ROI;
    }

    public String getMode() {
        return mode;
    }

    public double[] getEffectiveArea() {
        return effectiveArea;
    }

    public double[] getTime() {
        return time;
    }

    public double[] getStandardDeviation() {
        return standardDeviation;
    }

    public double getIntercept() {
        return intercept;
    }

    public double getSlope() {
        return slope;
    }

    public double getMinValueDiffusionLaw() {
        return minValueDiffusionLaw;
    }

    public double getMaxValueDiffusionLaw() {
        return maxValueDiffusionLaw;
    }
}
