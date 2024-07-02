package fiji.plugin.imaging_fcs.new_imfcs.model.fit;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace.DoubleExponentialFit;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace.PolynomialFit;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace.SingleExponentialFit;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * This class provides models for correcting photobleaching effects in fluorescence
 * correlation spectroscopy (FCS) data. Different correction methods include single and double
 * exponential decays, polynomial fitting, and linear segment fitting.
 */
public class BleachCorrectionModel {
    public static final int MAX_POLYNOMIAL_ORDER = 8;
    private final ExpSettingsModel settings;
    private final ImageModel imageModel;
    private int polynomialOrder = 0;
    private int numPointsIntensityTrace = 1;
    private int averageStride = 50;

    private double[] intensityTrace1, intensityTrace2, intensityTime;

    /**
     * Constructs a new BleachCorrectionModel with specified experimental settings and image model.
     *
     * @param settings   the experimental settings
     * @param imageModel the image model containing image data and background information
     */
    public BleachCorrectionModel(ExpSettingsModel settings, ImageModel imageModel) {
        this.settings = settings;
        this.imageModel = imageModel;
    }

    /**
     * Calculates the intensity trace for a specific pixel location in an image.
     *
     * @param img the image from which intensity data is extracted
     * @param x   the x-coordinate of the pixel
     * @param y   the y-coordinate of the pixel
     */
    public void calcIntensityTrace(ImagePlus img, int x, int y, int initialFrame, int finalFrame) {
        calcIntensityTrace(img, x, y, x, y, initialFrame, finalFrame);
    }

    /**
     * Calculates the intensity trace for two points in an image.
     *
     * @param img the image from which intensity data is extracted
     * @param x1  the x-coordinate of the first pixel
     * @param y1  the y-coordinate of the first pixel
     * @param x2  the x-coordinate of the second pixel
     * @param y2  the y-coordinate of the second pixel
     */
    public void calcIntensityTrace(ImagePlus img, int x1, int y1, int x2, int y2, int initialFrame, int finalFrame) {
        int average = (finalFrame - initialFrame + 1) / numPointsIntensityTrace;

        int background1 = imageModel.getBackground();
        int background2 =
                Constants.DC_FCCS_2D.equals(settings.getFitModel()) ? imageModel.getBackground2() : background1;

        intensityTrace1 = new double[numPointsIntensityTrace];
        intensityTrace2 = new double[numPointsIntensityTrace];
        intensityTime = new double[numPointsIntensityTrace];

        for (int i = 0; i < numPointsIntensityTrace; i++) {
            double sum1 = 0;
            double sum2 = 0;

            for (int x = 0; x < settings.getBinning().x; x++) {
                for (int y = 0; y < settings.getBinning().y; y++) {
                    for (int z = initialFrame + i * average; z < initialFrame + (i + 1) * average; z++) {
                        if (imageModel.isBackgroundLoaded()) {
                            background1 = (int) imageModel.getBackgroundMean()[x1 + x][y1 + y];
                            background2 = (int) imageModel.getBackgroundMean()[x2 + x][y2 + y];
                        }
                        sum1 += img.getStack().getProcessor(z).get(x1 + x, y1 + y) - background1;
                        sum2 += img.getStack().getProcessor(z).get(x2 + x, y2 + y) - background2;
                    }
                }
            }
            intensityTrace1[i] = sum1 / average;
            intensityTrace2[i] = sum2 / average;
            intensityTime[i] = settings.getFrameTime() * (i + 0.5) * average;
        }
    }

    /**
     * Retrieves corrected intensity data from an image based on the selected bleach correction method.
     *
     * @param img  the image from which to extract intensity
     * @param x    the x-coordinate of the pixel
     * @param y    the y-coordinate of the pixel
     * @param mode specifies which intensity trace to use (1 or 2)
     * @return the array of corrected intensity values
     */
    public double[] getIntensity(ImagePlus img, int x, int y, int mode, int initialFrame, int finalFrame) {
        int numFrames = finalFrame - initialFrame + 1;
        double[] intensityData = new double[numFrames];

        fillIntensityData(img, mode, intensityData, x, y, initialFrame);

        double[] intensityTrace = mode == 1 ? intensityTrace1 : intensityTrace2;

        switch (settings.getBleachCorrection()) {
            case Constants.BLEACH_CORRECTION_SINGLE_EXP:
                handleSingleExponential(intensityData, intensityTrace);
                break;
            case Constants.BLEACH_CORRECTION_DOUBLE_EXP:
                handleDoubleExponential(intensityData, intensityTrace);
                break;
            case Constants.BLEACH_CORRECTION_POLYNOMIAL:
                handlePolynomial(intensityData, intensityTrace);
                break;
            case Constants.BLEACH_CORRECTION_LINEAR_SEGMENT:
                handleLinearSegment(intensityData, intensityTrace);
                break;
        }

        return intensityData;
    }

    /**
     * Fills the provided intensity data array with calculated intensity values from the specified image.
     * This method takes into account the selected background model based on the mode and fit model settings,
     * and corrects for background in each pixel within the binning area specified in the settings.
     *
     * @param img           The image from which to extract intensity values.
     * @param mode          Determines which background model to use. If mode is 2 and the fit model is DC-FCCS (2D),
     *                      a different background value (background2) is used; otherwise, the default background is
     *                      used.
     * @param intensityData An array to be filled with the corrected intensity values.
     * @param x             The x-coordinate of the top left corner of the binning area.
     * @param y             The y-coordinate of the top left corner of the binning area.
     */
    private void fillIntensityData(ImagePlus img, int mode, double[] intensityData, int x, int y, int initialFrame) {
        int background = (mode == 2 &&
                Constants.DC_FCCS_2D.equals(settings.getFitModel())) ? imageModel.getBackground2() :
                imageModel.getBackground();

        for (int i = 0; i < intensityData.length; i++) {
            final ImageProcessor ip = img.getStack().getProcessor(initialFrame + i);
            for (int bx = 0; bx < settings.getBinning().x; bx++) {
                for (int by = 0; by < settings.getBinning().y; by++) {
                    if (imageModel.isBackgroundLoaded()) {
                        background = (int) Math.round(imageModel.getBackgroundMean()[x + bx][y + by]);
                    }

                    intensityData[i] += ip.get(x + bx, y + by) - background;
                }
            }
        }
    }

    /**
     * Applies a single exponential bleach correction to the intensity data based on a fitted exponential decay model.
     * This method modifies the intensity data array to account for photobleaching effects observed in the intensity
     * trace.
     *
     * @param intensityData  The array containing the raw intensity data to be corrected.
     * @param intensityTrace The array containing sampled intensity data used to fit the exponential model.
     */
    private void handleSingleExponential(double[] intensityData, double[] intensityTrace) {
        SingleExponentialFit exponentialFit = new SingleExponentialFit(intensityTime);
        double[] result = exponentialFit.fitIntensityTrace(intensityTrace);

        // Correct the full intensity trace if the fit is successful.
        if (result[0] * result[1] != 0) {
            for (int i = 0; i < intensityData.length; i++) {
                intensityData[i] = intensityData[i] / Math.sqrt(
                        (result[0] * Math.exp(-settings.getFrameTime() * (i + 0.5) / result[1]) + result[2]) /
                                (result[0] + result[2])) + (result[0] + result[2]) * (1 - Math.sqrt(
                        (result[0] * Math.exp(-settings.getFrameTime() * (i + 0.5) / result[1]) + result[2]) /
                                (result[0] + result[2])));
            }

            for (int i = 0; i < numPointsIntensityTrace; i++) {
                intensityTrace[i] = intensityTrace[i] / Math.sqrt(
                        (result[0] * Math.exp(-intensityTime[i] / result[1]) + result[2]) / (result[0] + result[2])) +
                        (result[0] + result[2]) * (1 - Math.sqrt(
                                (result[0] * Math.exp(-intensityTime[i] / result[1]) + result[2]) /
                                        (result[0] + result[2])));
            }
        }
    }

    /**
     * Applies a double exponential bleach correction to the intensity data based on a fitted double exponential
     * decay model.
     * This method is intended for cases where a single exponential model is insufficient to describe the
     * photobleaching dynamics.
     *
     * @param intensityData  The array containing the raw intensity data to be corrected.
     * @param intensityTrace The array containing sampled intensity data used to fit the double exponential model.
     */
    private void handleDoubleExponential(double[] intensityData, double[] intensityTrace) {
        DoubleExponentialFit doubleExponentialFit = new DoubleExponentialFit(intensityTime);
        double[] result = doubleExponentialFit.fitIntensityTrace(intensityTrace);

        if (result[0] * result[1] * result[2] * result[3] != 0) {
            for (int i = 0; i < intensityData.length; i++) {
                intensityData[i] = intensityData[i] / Math.sqrt(
                        (result[0] * Math.exp(-settings.getFrameTime() * (i + 0.5) / result[1]) +
                                result[2] * Math.exp(-settings.getFrameTime() * (i + 0.5) / result[3]) + result[4]) /
                                (result[0] + result[2] + result[4])) + (result[0] + result[2] + result[4]) * (1 -
                        Math.sqrt((result[0] * Math.exp(-settings.getFrameTime() * (i + 0.5) / result[1]) +
                                result[2] * Math.exp(-settings.getFrameTime() * (i + 0.5) / result[3]) + result[4]) /
                                (result[0] + result[2] + result[4])));
            }

            for (int i = 0; i < numPointsIntensityTrace; i++) {
                intensityTrace[i] = intensityTrace[i] / Math.sqrt((result[0] * Math.exp(-intensityTime[i] / result[1]) +
                        result[2] * Math.exp(-intensityTime[i] / result[3]) + result[4]) /
                        (result[0] + result[2] + result[4])) + (result[0] + result[2] + result[4]) * (1 - Math.sqrt(
                        (result[0] * Math.exp(-intensityTime[i] / result[1]) +
                                result[2] * Math.exp(-intensityTime[i] / result[3]) + result[4]) /
                                (result[0] + result[2] + result[4])));
            }
        }
    }

    /**
     * Applies a polynomial bleach correction based on a fitted polynomial model.
     * This method fits a polynomial to the intensity trace and uses it to correct the raw intensity data.
     *
     * @param intensityData  The array containing the raw intensity data to be corrected.
     * @param intensityTrace The array containing sampled intensity data used to fit the polynomial model.
     */
    private void handlePolynomial(double[] intensityData, double[] intensityTrace) {
        PolynomialFit polynomialFit = new PolynomialFit(intensityTime, polynomialOrder);
        double[] result = polynomialFit.fitIntensityTrace(intensityTrace);

        for (int i = 0; i < intensityData.length; i++) {
            double correlationFunction = 0;
            for (int j = 0; j <= polynomialOrder; j++) {
                correlationFunction += result[j] * Math.pow(settings.getFrameTime() * (i + 0.5), j);
            }
            intensityData[i] = intensityData[i] / Math.sqrt(correlationFunction / result[0]) +
                    result[0] * (1 - Math.sqrt(correlationFunction / result[0]));
        }

        for (int i = 0; i < numPointsIntensityTrace; i++) {
            double correlationFunction = 0;
            for (int j = 0; j <= polynomialOrder; j++) {
                correlationFunction += result[j] * Math.pow(intensityTime[i], j);
            }
            intensityTrace[i] = intensityTrace[i] / Math.sqrt(correlationFunction / result[0]) +
                    result[0] * (1 - Math.sqrt(correlationFunction / result[0]));
        }
    }

    /**
     * Applies a piecewise linear segment bleach correction to the intensity data.
     * This method divides the intensity data into linear segments and fits a linear correction to each segment.
     *
     * @param intensityData  The array containing the raw intensity data to be corrected.
     * @param intensityTrace The array containing sampled intensity data used to create the intensity correction model.
     */
    private void handleLinearSegment(double[] intensityData, double[] intensityTrace) {
        int numFrames = intensityData.length;

        int numLinearSegments = numFrames / settings.getSlidingWindowLength();
        int linearSegmentsAverage = numFrames / numLinearSegments;

        double[] bleachCorrectionTrace = new double[numLinearSegments];

        // Calculating the average intensity in each segment
        for (int i = 0; i < numLinearSegments; i++) {
            double sum = 0;
            for (int j = i * linearSegmentsAverage; j < (i + 1) * linearSegmentsAverage; j++) {
                sum += intensityData[j];
            }
            bleachCorrectionTrace[i] = sum / linearSegmentsAverage;
        }

        double initialIntensity = bleachCorrectionTrace[0] + (bleachCorrectionTrace[0] - bleachCorrectionTrace[1]) / 2;

        for (int i = 1; i < linearSegmentsAverage / 2; i++) {
            double correctionFactor = Math.sqrt((bleachCorrectionTrace[0] +
                    (bleachCorrectionTrace[0] - bleachCorrectionTrace[1]) / linearSegmentsAverage *
                            ((double) linearSegmentsAverage / 2 - i)) / initialIntensity);
            applyCorrection(intensityData, i, correctionFactor, initialIntensity);

        }

        for (int i = 1; i < numLinearSegments; i++) {
            for (int j = 0; j < linearSegmentsAverage; j++) {
                int nf = (i - 1) * linearSegmentsAverage + linearSegmentsAverage / 2 + j;
                double correctionFactor = Math.sqrt((bleachCorrectionTrace[i - 1] +
                        (bleachCorrectionTrace[i] - bleachCorrectionTrace[i - 1]) * j / linearSegmentsAverage) /
                        initialIntensity);
                applyCorrection(intensityData, nf, correctionFactor, initialIntensity);
            }
        }

        for (int i = (numLinearSegments - 1) * linearSegmentsAverage + linearSegmentsAverage / 2; i < numFrames; i++) {
            int nf = i - (numLinearSegments - 1) * linearSegmentsAverage + linearSegmentsAverage / 2 +
                    linearSegmentsAverage;
            double correctionFactor = Math.sqrt((bleachCorrectionTrace[numLinearSegments - 2] +
                    (bleachCorrectionTrace[numLinearSegments - 1] - bleachCorrectionTrace[numLinearSegments - 2]) * nf /
                            linearSegmentsAverage) / initialIntensity);
            applyCorrection(intensityData, i, correctionFactor, initialIntensity);
        }

        // Fill Intensity Trace
        int numPointsAverage = numFrames / numPointsIntensityTrace;
        for (int i = 0; i < numPointsIntensityTrace; i++) {
            intensityTrace[i] = intensityData[(int) (i + 0.5) * numPointsAverage + 1];
        }
    }

    /**
     * Applies a correction factor to an individual intensity data point.
     * This method modifies the intensity value at a specific index based on a correction factor and the initial
     * intensity,
     * aiming to adjust the intensity data to account for photobleaching or other systematic variations.
     *
     * @param intensityData    The array of intensity data that needs correction.
     * @param index            The index of the intensity data point to correct.
     * @param correctionFactor The factor by which the original intensity data is adjusted.
     * @param initialIntensity The initial or reference intensity value used for baseline correction.
     */
    private void applyCorrection(double[] intensityData, int index, double correctionFactor, double initialIntensity) {
        intensityData[index] = intensityData[index] / correctionFactor + initialIntensity * (1 - correctionFactor);
    }

    public int getPolynomialOrder() {
        return polynomialOrder;
    }

    public void setPolynomialOrder(int polynomialOrder) {
        this.polynomialOrder = polynomialOrder;
    }

    public int getNumPointsIntensityTrace() {
        return numPointsIntensityTrace;
    }

    public void setNumPointsIntensityTrace(int numPointsIntensityTrace) {
        this.numPointsIntensityTrace = numPointsIntensityTrace;
    }

    public int getAverageStride() {
        return averageStride;
    }

    public void setAverageStride(String averageStride) {
        this.averageStride = Integer.parseInt(averageStride);
    }

    public double[] getIntensityTrace1() {
        return intensityTrace1;
    }

    public double[] getIntensityTrace2() {
        return intensityTrace2;
    }

    public double[] getIntensityTime() {
        return intensityTime;
    }
}
