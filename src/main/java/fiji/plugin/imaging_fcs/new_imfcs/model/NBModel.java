package fiji.plugin.imaging_fcs.new_imfcs.model;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.BleachCorrectionModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.util.Arrays;
import java.util.function.BiConsumer;

/**
 * The NBModel class represents the model for performing number and brightness (N&B) analysis.
 */
public final class NBModel {
    private final ImageModel imageModel;
    private final ExpSettingsModel settings;
    private final OptionsModel options;
    private final BleachCorrectionModel bleachCorrectionModel;

    private double[][] filterArray, NBB, NBN, NBNum, NBEpsilon;

    private String mode = "G1";
    private int calibrationRatio = 2;
    private double sValue = 0.0;

    private int frameCount;

    /**
     * Constructs a new NBModel instance with the specified parameters.
     *
     * @param imageModel            The image model containing image data.
     * @param settings              The experimental settings model.
     * @param options               The options model containing analysis options.
     * @param bleachCorrectionModel The bleach correction model.
     */
    public NBModel(ImageModel imageModel, ExpSettingsModel settings, OptionsModel options,
                   BleachCorrectionModel bleachCorrectionModel) {
        this.imageModel = imageModel;
        this.bleachCorrectionModel = bleachCorrectionModel;
        this.settings = settings;
        this.options = options;
    }

    /**
     * Performs number and brightness (N&B) analysis on the specified image.
     *
     * @param img        The ImagePlus object representing the input image.
     * @param evaluation A boolean indicating whether to perform evaluation.
     * @param showImage  A consumer to display the resulting image.
     */
    public void performNB(ImagePlus img, boolean evaluation, BiConsumer<ImagePlus, String> showImage) {
        int width = img.getWidth();
        int height = img.getHeight();

        filterArray = new double[width][height];
        NBB = new double[width][height];
        NBN = new double[width][height];

        if (evaluation) {
            NBEpsilon = new double[width][height];
            NBNum = new double[width][height];
        }

        final double filterLL = settings.getFilterLowerLimit();
        final double filterUL = settings.getFilterUpperLimit();

        frameCount = settings.getLastFrame() - settings.getFirstFrame() + 1;

        // If mean is selected we consider all the frames, else just the first frame
        final int framesToConsider = Constants.FILTER_MEAN.equals(settings.getFilter()) ? frameCount : 1;

        if ("none".equals(settings.getFilter())) {
            for (int i = 0; i < width; i++) {
                Arrays.fill(filterArray[i], 1.0);
            }
        } else {
            for (int i = 0; i < width; i++) {
                for (int j = 0; j < height; j++) {
                    // This is used when "Mean" is selected, if "Intensity" is selected, we will just the current value
                    double sum = 0;

                    for (int k = settings.getFirstFrame(); k < settings.getFirstFrame() + framesToConsider; k++) {
                        sum += img.getStack().getProcessor(k).getPixelValue(i, j);
                    }

                    // Here we do the mean of the sum. If Intensity is selected, it doesn't change the value (sum / 1)
                    double value = sum / framesToConsider;

                    filterArray[i][j] = (value > filterLL && value < filterUL) ? value : Double.NaN;
                }
            }
        }

        if (options.isUseGpu()) {
            performGpuAnalysis(img);
        } else {
            performCpuAnalysis(img, evaluation);
        }

        if (evaluation) {
            createNBImages(img, showImage);
        }
    }

    private void performGpuAnalysis(ImagePlus img) {
        // TODO: implement me
    }

    /**
     * Performs CPU-based analysis of the fluorescence image.
     * If the mode is "G1", it calculates the mean and covariance.
     * If the mode is "Calibrated", it calculates the mean and variance.
     *
     * @param img        The ImagePlus object representing the input image.
     * @param evaluation A boolean indicating whether to perform evaluation.
     */
    private void performCpuAnalysis(ImagePlus img, boolean evaluation) {
        if ("G1".equals(mode)) {
            calculateMeanAndCovariance(img);
        } else { // Calibrated
            calculateMeanAndVariance(img, evaluation);
        }
    }

    /**
     * Calculates the mean and covariance for the fluorescence image.
     * Used when the mode is "G1".
     *
     * @param img The ImagePlus object representing the input image.
     */
    private void calculateMeanAndCovariance(ImagePlus img) {
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                bleachCorrectionModel.calcIntensityTrace(img, i, j, settings.getFirstFrame(), settings.getLastFrame());
                double[] data =
                        bleachCorrectionModel.getIntensity(img, i, j, 1, settings.getFirstFrame(),
                                settings.getLastFrame());

                double mean = 0.0;
                double meanNextElement = 0.0;
                double covariance = 0.0;

                for (int k = 0; k < frameCount - 1; k++) {
                    mean += data[k];
                    meanNextElement += data[k + 1];
                    covariance += data[k] * data[k + 1];
                }

                mean /= frameCount - 1;
                meanNextElement /= frameCount - 1;
                covariance = covariance / (frameCount - 1) - mean * meanNextElement;

                // note that offset has already been corrected for in getIntensity()
                if (Double.isNaN(filterArray[i][j])) {
                    NBB[i][j] = Double.NaN;
                    NBN[i][j] = Double.NaN;
                } else {
                    NBB[i][j] = (covariance - imageModel.getBackgroundCovariance()[i][j]) / mean;
                    NBN[i][j] = mean / NBB[i][j];
                }
            }
        }
    }

    /**
     * Calculates the mean and variance for the fluorescence image.
     * Used when the mode is "Calibrated".
     *
     * @param img        The ImagePlus object representing the input image.
     * @param evaluation A boolean indicating whether to perform evaluation.
     */
    private void calculateMeanAndVariance(ImagePlus img, boolean evaluation) {
        for (int i = 0; i < img.getWidth(); i++) {
            for (int j = 0; j < img.getHeight(); j++) {
                bleachCorrectionModel.calcIntensityTrace(img, i, j, 1, img.getStackSize());
                double[] data = bleachCorrectionModel.getIntensity(img, i, j, 1, 1, img.getStackSize());

                double mean = 0;
                double variance = 0;

                for (double element : data) {
                    mean += element;
                    variance += element * element;
                }

                mean /= data.length;
                variance = variance / data.length - mean * mean;

                // note that offset has already been corrected for in getIntensity()
                if (Double.isNaN(filterArray[i][j])) {
                    NBB[i][j] = Double.NaN;
                    NBN[i][j] = Double.NaN;

                    if (evaluation) {
                        NBNum[i][j] = Double.NaN;
                        NBEpsilon[i][j] = Double.NaN;
                    }
                } else {
                    NBB[i][j] = (variance - imageModel.getBackgroundVariance()[i][j]) / mean;
                    NBN[i][j] = (mean * mean) / (variance - imageModel.getBackgroundVariance()[i][j]);

                    if (evaluation) {
                        NBNum[i][j] = mean / (NBB[i][j] - sValue); // n = (mean - offset) / (B - S)
                        NBEpsilon[i][j] = NBB[i][j] / sValue - 1; // epsilon = B / S - 1
                    }
                }
            }
        }
    }

    /**
     * Creates and fills an ImagePlus object with the specified title, width, height, and values.
     *
     * @param title  The title of the image.
     * @param width  The width of the image.
     * @param height The height of the image.
     * @param values The values to fill the image with.
     * @return The created ImagePlus object.
     */
    private ImagePlus createAndFillImage(String title, int width, int height, double[][] values) {
        ImagePlus image = IJ.createImage(title, "GRAY32", width, height, 1);
        ImageProcessor ip = image.getStack().getProcessor(1);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                ip.putPixelValue(i, j, values[i][j]);
            }
        }

        return image;
    }

    /**
     * Creates and displays the number and brightness (N&B) images based on the analysis results.
     *
     * @param img       The ImagePlus object representing the input image.
     * @param showImage A consumer to display the resulting image.
     */
    private void createNBImages(ImagePlus img, BiConsumer<ImagePlus, String> showImage) {
        int height = img.getHeight();
        int width = img.getWidth();

        ImagePlus imageN = createAndFillImage("Num N&B", width, height, NBN);
        ImagePlus imageB = createAndFillImage("Brightness N&B", width, height, NBB);

        showImage.accept(imageN, "Cyan Hot");
        showImage.accept(imageB, "Yellow Hot");

        if ("Calibrated".equals(mode)) {
            ImagePlus imageNum = createAndFillImage("Num", width, height, NBNum);
            ImagePlus imageEpsilon = createAndFillImage("Epsilon", width, height, NBEpsilon);

            showImage.accept(imageNum, "Cyan Hot");
            showImage.accept(imageEpsilon, "Yellow Hot");
        }
    }

    // Getter and setter.
    public String getMode() {
        return mode;
    }

    public void setMode(String mode) {
        this.mode = mode;
    }

    public int getCalibrationRatio() {
        return calibrationRatio;
    }

    public void setCalibrationRatio(String calibrationRatio) {
        this.calibrationRatio = Integer.parseInt(calibrationRatio);
    }

    public double getsValue() {
        return sValue;
    }

    public void setsValue(String sValue) {
        this.sValue = Double.parseDouble(sValue);
    }
}
