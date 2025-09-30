package fiji.plugin.imaging_fcs.imfcs.model.correlations;

import fiji.plugin.imaging_fcs.imfcs.enums.BleachCorrectionMethod;
import fiji.plugin.imaging_fcs.imfcs.enums.DccfDirection;
import fiji.plugin.imaging_fcs.imfcs.enums.FitFunctions;
import fiji.plugin.imaging_fcs.imfcs.model.*;
import fiji.plugin.imaging_fcs.imfcs.utils.ExcelReader;
import fiji.plugin.imaging_fcs.imfcs.utils.Pair;
import ij.ImagePlus;
import org.apache.poi.ss.usermodel.Workbook;

import java.awt.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

import static fiji.plugin.imaging_fcs.imfcs.utils.MatrixDeepCopy.deepCopy;

/**
 * The Correlator class is responsible for performing correlation calculations
 * on pixel intensity data from image frames. It utilizes various settings and
 * models to compute correlation functions and covariance matrices.
 */
public class Correlator {
    private final int BLOCK_LAG = 1;
    private final ExpSettingsModel settings;
    private final BleachCorrectionModel bleachCorrectionModel;
    private final FitModel fitModel;
    private final Map<DccfDirection, double[][]> dccf = new HashMap<>();
    private int correlatorQ, blockIndex;
    private double median;
    private int[] numSamples, lags, sampleTimes;
    private double[] lagTimes;
    private double[][] regularizedCovarianceMatrix, varianceBlocks;
    private PixelModel[][] pixelModels;
    private Pair<Point[], PixelModel> lastUsedPixelModel;

    /**
     * Constructs a Correlator with the specified settings, bleach correction model, and fit model.
     *
     * @param settings              The experimental settings model.
     * @param bleachCorrectionModel The bleach correction model.
     * @param fitModel              The fit model.
     */
    public Correlator(ExpSettingsModel settings, BleachCorrectionModel bleachCorrectionModel, FitModel fitModel) {
        this.settings = settings;
        this.bleachCorrectionModel = bleachCorrectionModel;
        this.fitModel = fitModel;
    }

    /**
     * Loads results from an Excel workbook and restores parameters for the pixel models.
     * This method reads lag times, sample times, and attributes such as ACF, standard deviation,
     * fitted functions, residuals, and MSD. It also restores the ACF1 and ACF2 models if applicable.
     *
     * @param workbook  the Excel workbook containing the saved results
     * @param dimension the dimension of the image (width and height)
     */
    public void loadResultsFromWorkbook(Workbook workbook, Dimension dimension) {
        // read the Excel file to restore parameters
        ExcelReader.readLagTimesAndSampleTimes(workbook, "Lag time", this::setLagTimes, this::setSampleTimes);
        pixelModels = new PixelModel[dimension.width][dimension.height];

        loadPixelModelsSheets(workbook, "CF", pixelModels);

        if (settings.getFitModel() == FitFunctions.DC_FCCS_2D) {
            PixelModel[][] acf1PixelModels = new PixelModel[dimension.width][dimension.height];
            loadPixelModelsSheets(workbook, "ACF1", acf1PixelModels);
            loadAcfPixelModels(acf1PixelModels, PixelModel::setAcf1PixelModel);

            PixelModel[][] acf2PixelModels = new PixelModel[dimension.width][dimension.height];
            loadPixelModelsSheets(workbook, "ACF2", acf2PixelModels);
            loadAcfPixelModels(acf2PixelModels, PixelModel::setAcf2PixelModel);
        }
    }

    /**
     * Loads data from various sheets in the workbook and updates the corresponding pixel model attributes.
     * This includes correlation functions, standard deviation, fitted functions, residuals, and MSD data.
     *
     * @param workbook    the Excel workbook containing the sheets
     * @param name        the base name of the sheet (e.g., "CF")
     * @param pixelModels a 2D array of PixelModel objects to update
     */
    private void loadPixelModelsSheets(Workbook workbook, String name, PixelModel[][] pixelModels) {
        ExcelReader.readSheetToPixelModels(workbook, name, pixelModels, PixelModel::setCorrelationFunction);
        ExcelReader.readSheetToPixelModels(workbook, name + " - Standard Deviation", pixelModels,
                PixelModel::setStandardDeviationCF);
        ExcelReader.readSheetToPixelModels(workbook, name + " - Fit Functions", pixelModels, PixelModel::setFittedCF);
        ExcelReader.readSheetToPixelModels(workbook, name + " - Residuals", pixelModels, PixelModel::setResiduals);
        ExcelReader.readSheetToPixelModels(workbook, name + " - MSD", pixelModels, PixelModel::setMSD);
        ExcelReader.readSheetToPixelModels(workbook, name + " - Fit Parameters", pixelModels,
                (pixelModel, values) -> pixelModel.setFitParams(new PixelModel.FitParameters(values)));
    }

    /**
     * Loads ACF pixel models (ACF1 or ACF2) and sets them in the corresponding positions in the main pixel models.
     *
     * @param acfPixelModels a 2D array of ACF PixelModel objects to set
     * @param acfSetter      a BiConsumer to set the ACF1 or ACF2 models in the main PixelModel
     */
    private void loadAcfPixelModels(PixelModel[][] acfPixelModels, BiConsumer<PixelModel, PixelModel> acfSetter) {
        for (int i = 0; i < pixelModels.length; i++) {
            for (int j = 0; j < pixelModels[0].length; j++) {
                if (acfPixelModels[i][j] != null && pixelModels[i][j] != null) {
                    acfSetter.accept(pixelModels[i][j], acfPixelModels[i][j]);
                }
            }
        }
    }

    /**
     * Retrieves a block of intensity data from the specified image and coordinates.
     *
     * @param img          The image.
     * @param x            The x-coordinate of the first pixel.
     * @param y            The y-coordinate of the first pixel.
     * @param x2           The x-coordinate of the second pixel.
     * @param y2           The y-coordinate of the second pixel.
     * @param initialFrame The initial frame number.
     * @param finalFrame   The final frame number.
     * @param mode         The mode of intensity retrieval.
     * @return A 2D array containing the intensity data.
     */
    private double[][] getIntensityBlock(ImagePlus img, int x, int y, int x2, int y2, int initialFrame, int finalFrame,
                                         int mode) {
        double[] intensityData = bleachCorrectionModel.getIntensity(img, x, y, mode, initialFrame, finalFrame);

        double[][] intensityBlock = new double[2][intensityData.length];
        intensityBlock[0] = intensityData;

        if (x != x2 || y != y2) {
            // get intensity for second pixel
            intensityBlock[1] = bleachCorrectionModel.getIntensity(img, x2, y2, 2, initialFrame, finalFrame);
        } else {
            // otherwise perform an auto correlation
            intensityBlock[1] = intensityData;
        }

        return intensityBlock;
    }

    /**
     * Performs correlation on the specified pixel within the given image and frame range.
     *
     * @param img          The image.
     * @param x            The x-coordinate of the pixel.
     * @param y            The y-coordinate of the pixel.
     * @param initialFrame The initial frame number.
     * @param finalFrame   The final frame number.
     */
    public void correlate(ImagePlus img, int x, int y, int initialFrame, int finalFrame) {
        correlate(img, x, y, x, y, initialFrame, finalFrame);
    }

    /**
     * Performs correlation between two pixels in the given image within the specified frame range.
     * If FCCS (Fluorescence Cross-Correlation Spectroscopy) is enabled, it calls the FCCS correlation method.
     * Otherwise, it creates or retrieves the PixelModel for the first pixel and performs correlation.
     *
     * @param img          The image in which the pixels are located.
     * @param x            The x-coordinate of the first pixel.
     * @param y            The y-coordinate of the first pixel.
     * @param x2           The x-coordinate of the second pixel.
     * @param y2           The y-coordinate of the second pixel.
     * @param initialFrame The initial frame to start the correlation.
     * @param finalFrame   The final frame to end the correlation.
     */
    public void correlate(ImagePlus img, int x, int y, int x2, int y2, int initialFrame, int finalFrame) {
        // Instantiate the pixelModels array if it hasn't been created yet
        if (pixelModels == null) {
            pixelModels = new PixelModel[img.getWidth()][img.getHeight()];
        }

        // Create or update the PixelModel for the first pixel and perform correlation
        pixelModels[x][y] = new PixelModel();
        PixelModel pixelModel = pixelModels[x][y];
        lastUsedPixelModel = new Pair<>(new Point[]{new Point(x, y), new Point(x2, y2)}, pixelModel);

        if (settings.isFCCSDisp()) {
            correlateFCCS(img, pixelModel, x, y, x2, y2, initialFrame, finalFrame);
        }

        correlatePixelModel(pixelModel, img, x, y, x2, y2, initialFrame, finalFrame, false);
    }

    /**
     * Performs FCCS correlation between two pixels in the given image within the specified frame range. It creates
     * or retrieves the two PixelModel instances for the two pixel positions and performs correlation on both.
     *
     * @param img          The image in which the pixels are located.
     * @param x            The x-coordinate of the first pixel.
     * @param y            The y-coordinate of the first pixel.
     * @param x2           The x-coordinate of the second pixel.
     * @param y2           The y-coordinate of the second pixel.
     * @param initialFrame The initial frame to start the correlation.
     * @param finalFrame   The final frame to end the correlation.
     */
    private void correlateFCCS(ImagePlus img, PixelModel pixelModel, int x, int y, int x2, int y2, int initialFrame,
                               int finalFrame) {
        // Create PixelModels for both ACF and perform correlation
        pixelModel.setAcf1PixelModel(new PixelModel());
        pixelModel.setAcf2PixelModel(new PixelModel());

        // Perform correlation for both FCCS pixel models
        correlatePixelModel(pixelModel.getAcf1PixelModel(), img, x, y, x, y, initialFrame, finalFrame, false);
        correlatePixelModel(pixelModel.getAcf2PixelModel(), img, x2, y2, x2, y2, initialFrame, finalFrame, true);
    }

    /**
     * Correlates the specified PixelModel using the given image, pixel coordinates, and frame range.
     *
     * @param pixelModel   The PixelModel to be correlated.
     * @param img          The image.
     * @param x            The x-coordinate of the first pixel.
     * @param y            The y-coordinate of the first pixel.
     * @param x2           The x-coordinate of the second pixel.
     * @param y2           The y-coordinate of the second pixel.
     * @param initialFrame The initial frame number.
     * @param finalFrame   The final frame number.
     */
    public void correlatePixelModel(PixelModel pixelModel, ImagePlus img, int x, int y, int x2, int y2,
                                    int initialFrame, int finalFrame) {
       correlatePixelModel(pixelModel, img, x, y, x2, y2, initialFrame, finalFrame, false);
    }

    /**
     * Correlates the specified PixelModel using the given image, pixel coordinates, and frame range.
     *
     * @param pixelModel   The PixelModel to be correlated.
     * @param img          The image.
     * @param x            The x-coordinate of the first pixel.
     * @param y            The y-coordinate of the first pixel.
     * @param x2           The x-coordinate of the second pixel.
     * @param y2           The y-coordinate of the second pixel.
     * @param initialFrame The initial frame number.
     * @param finalFrame   The final frame number.
     * @param secondPixel  Indicates whether this is the second pixel in a FCCS correlation.
     */
    private void correlatePixelModel(PixelModel pixelModel, ImagePlus img, int x, int y, int x2, int y2,
                                    int initialFrame, int finalFrame, boolean secondPixel) {
        // calculate the intensity trace beforehand since it will be needed to perform the correlation
        bleachCorrectionModel.calcIntensityTrace(img, x, y, x2, y2, initialFrame, finalFrame);

        correlatorQ = settings.getCorrelatorQ();

        if (settings.getBleachCorrection() == BleachCorrectionMethod.SLIDING_WINDOW) {
            handleSlidingWindowCorrelation(img, pixelModel, x, y, x2, y2, initialFrame, finalFrame, secondPixel);
        } else {
            // if sliding window is not selected, correlate the full intensity trace
            handleFullTraceCorrelation(img, pixelModel, x, y, x2, y2, initialFrame, finalFrame, secondPixel);
        }
    }

    /**
     * Handles correlation using a sliding window approach.
     *
     * @param img          The image.
     * @param pixelModel   The pixel model.
     * @param x            The x-coordinate of the first pixel.
     * @param y            The y-coordinate of the first pixel.
     * @param x2           The x-coordinate of the second pixel.
     * @param y2           The y-coordinate of the second pixel.
     * @param initialFrame The initial frame number.
     * @param finalFrame   The final frame number.
     * @param secondPixel  Indicates whether this is the second pixel in a FCCS correlation.
     */
    private void handleSlidingWindowCorrelation(ImagePlus img, PixelModel pixelModel, int x, int y, int x2, int y2,
                                                int initialFrame, int finalFrame, boolean secondPixel) {
        int numFrames = finalFrame - initialFrame + 1;
        int numSlidingWindow = numFrames / settings.getSlidingWindowLength();

        int mode = secondPixel ? 2 : 1;

        // allow smaller correlator Q value as minimum but not larger
        correlatorQ = Math.min(correlatorQ, settings.getLagGroupNumber());

        for (int i = 0; i < numSlidingWindow; i++) {
            PixelModel tmpSlidingWindowModel = new PixelModel();

            int slidingWindowInitialFrame = i * settings.getSlidingWindowLength() + initialFrame;
            int slidingWindowFinalFrame = (i + 1) * settings.getSlidingWindowLength() + initialFrame - 1;

            double[][] intensityBlock =
                    getIntensityBlock(img, x, y, x2, y2, slidingWindowInitialFrame, slidingWindowFinalFrame, mode);

            blockTransform(pixelModel, deepCopy(intensityBlock), settings.getSlidingWindowLength());
            calculateCorrelationFunction(tmpSlidingWindowModel, deepCopy(intensityBlock),
                    settings.getSlidingWindowLength());
            pixelModel.addPixelModelSlidingWindow(tmpSlidingWindowModel);
        }
        pixelModel.averageSlidingWindow(numSlidingWindow);
    }

    /**
     * Handles correlation on the full intensity trace without using a sliding window.
     *
     * @param img          The image.
     * @param pixelModel   The pixel model.
     * @param x            The x-coordinate of the first pixel.
     * @param y            The y-coordinate of the first pixel.
     * @param x2           The x-coordinate of the second pixel.
     * @param y2           The y-coordinate of the second pixel.
     * @param initialFrame The initial frame number.
     * @param finalFrame   The final frame number.
     * @param secondPixel  Indicates whether this is the second pixel in a FCCS correlation.
     */
    private void handleFullTraceCorrelation(ImagePlus img, PixelModel pixelModel, int x, int y, int x2, int y2,
                                            int initialFrame, int finalFrame, boolean secondPixel) {
        int numFrames = finalFrame - initialFrame + 1;

        int mode = secondPixel ? 2 : 1;
        double[][] intensityBlock = getIntensityBlock(img, x, y, x2, y2, initialFrame, finalFrame, mode);

        blockTransform(pixelModel, deepCopy(intensityBlock), numFrames);
        calculateCorrelationFunction(pixelModel, deepCopy(intensityBlock), numFrames);
    }

    /**
     * Transforms the intensity correlation data using block transformation.
     *
     * @param pixelModel           The pixel model.
     * @param intensityCorrelation The intensity correlation data.
     * @param numFrames            The number of frames.
     */
    private void blockTransform(PixelModel pixelModel, double[][] intensityCorrelation, int numFrames) {
        int blockCount = calculateBlockCount(numFrames);

        varianceBlocks = new double[3][blockCount];
        double[] lowerQuartile = new double[blockCount];
        double[] upperQuartile = new double[blockCount];

        processBlocks(intensityCorrelation, blockCount, numFrames, varianceBlocks, lowerQuartile, upperQuartile);
        blockIndex =
                determineLastIndexMeetingCriteria(pixelModel, blockCount, varianceBlocks, lowerQuartile, upperQuartile);
    }

    /**
     * Calculates the number of blocks for the given number of frames.
     *
     * @param numFrames The number of frames.
     * @return The number of blocks.
     */
    private int calculateBlockCount(int numFrames) {
        // if the parameters are not instantiated then we compute them
        if (lags == null) {
            calculateParameters(numFrames);
        }

        return (int) Math.floor(Math.log(numSamples[BLOCK_LAG]) / Math.log(2)) - 2;
    }

    /**
     * Calculates the parameters required for correlation calculations.
     *
     * @param numFrames The number of frames.
     */
    public void calculateParameters(int numFrames) {
        lags = new int[settings.getChannelNumber()];
        lagTimes = new double[settings.getChannelNumber()];
        sampleTimes = new int[settings.getChannelNumber()];
        numSamples = new int[settings.getChannelNumber()];

        calculateLags(settings.getCorrelatorP(), settings.getCorrelatorP() / 2);
        calculateSampleTimes(settings.getCorrelatorP(), settings.getCorrelatorP() / 2);
        calculateNumberOfSamples(numFrames);
    }

    /**
     * Calculates the lag times for the given number of channels in the first and higher groups.
     *
     * @param numChannelsFirstGroup   The number of channels in the first group.
     * @param numChannelsHigherGroups The number of channels in the higher groups.
     */
    private void calculateLags(int numChannelsFirstGroup, int numChannelsHigherGroups) {
        for (int i = 0; i <= numChannelsHigherGroups; i++) {
            lags[i] = i;
            lagTimes[i] = i * settings.getFrameTime();
        }

        for (int group = 1; group <= settings.getLagGroupNumber(); group++) {
            for (int channel = 1; channel <= numChannelsHigherGroups; channel++) {
                int index = group * numChannelsHigherGroups + channel;
                lags[index] =
                        (int) (Math.pow(2, group - 1) * channel + (numChannelsFirstGroup / 4) * Math.pow(2, group));
                lagTimes[index] = lags[index] * settings.getFrameTime();
            }
        }
    }

    /**
     * Calculates the sample times (bin width) for the 0 lag time kcf.
     *
     * @param numChannelsFirstGroup   The number of channels in the first group.
     * @param numChannelsHigherGroups The number of channels in the higher groups.
     */
    private void calculateSampleTimes(int numChannelsFirstGroup, int numChannelsHigherGroups) {
        Arrays.fill(sampleTimes, 0, numChannelsFirstGroup + 1, 1);

        for (int group = 2; group <= settings.getLagGroupNumber(); group++) {
            for (int channel = 1; channel <= numChannelsHigherGroups; channel++) {
                sampleTimes[group * numChannelsHigherGroups + channel] = (int) Math.pow(2, group - 1);
            }
        }
    }

    /**
     * Calculates the number of samples for the given number of frames.
     *
     * @param numFrames The number of frames.
     */
    private void calculateNumberOfSamples(int numFrames) {
        for (int i = 0; i < settings.getChannelNumber(); i++) {
            numSamples[i] = (numFrames - lags[i]) / sampleTimes[i];
        }
    }

    /**
     * Bins the data by combining pairs of data points.
     *
     * @param numBinnedDataPoints The number of binned data points.
     * @param intensityBlock      The intensity data block.
     */
    private void binData(int numBinnedDataPoints, double[][] intensityBlock) {
        for (int i = 0; i < numBinnedDataPoints; i++) {
            intensityBlock[0][i] = intensityBlock[0][2 * i] + intensityBlock[0][2 * i + 1];
            intensityBlock[1][i] = intensityBlock[1][2 * i] + intensityBlock[1][2 * i + 1];
        }
    }

    /**
     * Processes blocks of intensity data for correlation calculations.
     *
     * @param intensityBlock The intensity data block.
     * @param blockCount     The number of blocks.
     * @param numFrames      The number of frames.
     * @param varianceBlocks The variance blocks.
     * @param lowerQuartile  The lower quartile values.
     * @param upperQuartile  The upper quartile values.
     */
    private void processBlocks(double[][] intensityBlock, int blockCount, int numFrames, double[][] varianceBlocks,
                               double[] lowerQuartile, double[] upperQuartile) {
        int currentIncrement = BLOCK_LAG;
        int numBinnedDataPoints = numFrames;
        int[] numProducts = new int[blockCount];

        for (int i = 0; i < settings.getChannelNumber(); i++) {
            // check whether the kcf width has changed
            if (currentIncrement != sampleTimes[i]) {
                // set the current increment accordingly
                currentIncrement = sampleTimes[i];
                // Correct the number of actual data points accordingly
                numBinnedDataPoints /= 2;
                binData(numBinnedDataPoints, intensityBlock);
            }

            if (i == BLOCK_LAG) {
                processCorrelationData(i, blockCount, numFrames, numBinnedDataPoints, intensityBlock, varianceBlocks,
                        numProducts, currentIncrement);
            }
        }

        for (int i = 0; i < blockCount; i++) {
            varianceBlocks[1][i] = Math.sqrt(varianceBlocks[1][i]);
            varianceBlocks[2][i] = varianceBlocks[1][i] / Math.sqrt(2 * (numProducts[i] - 1));
            upperQuartile[i] = varianceBlocks[1][i] + varianceBlocks[2][i];
            lowerQuartile[i] = varianceBlocks[1][i] - varianceBlocks[2][i];
        }
    }

    /**
     * Calculates the monitors for correlation data.
     *
     * @param numProducts    The number of products.
     * @param intensityBlock The intensity data block.
     * @param delay          The delay.
     * @return An array containing the direct and delayed monitors.
     */
    private double[] calculateMonitors(double numProducts, double[][] intensityBlock, int delay) {
        double directMonitor = 0.0;
        double delayedMonitor = 0.0;

        for (int i = 0; i < numProducts; i++) {
            directMonitor += intensityBlock[0][i];
            delayedMonitor += intensityBlock[1][i + delay];
        }
        directMonitor /= numProducts;
        delayedMonitor /= numProducts;

        return new double[]{directMonitor, delayedMonitor};
    }

    /**
     * Calculates the correlation products for the given intensity data.
     *
     * @param numProducts    The number of products.
     * @param intensityBlock The intensity data block.
     * @param directMonitor  The direct monitor value.
     * @param delayedMonitor The delayed monitor value.
     * @param products       The array to store the products.
     * @param delay          The delay value.
     * @return An array containing the sum of products and the sum of squared products.
     */
    private double[] calculateCorrelations(double numProducts, double[][] intensityBlock, double directMonitor,
                                           double delayedMonitor, double[] products, int delay) {
        double sumProd = 0.0;
        double sumProdSquared = 0.0;

        for (int i = 0; i < numProducts; i++) {
            products[i] = intensityBlock[0][i] * intensityBlock[1][i + delay] - delayedMonitor * intensityBlock[0][i] -
                    directMonitor * intensityBlock[1][i + delay] + delayedMonitor * directMonitor;
            sumProd += products[i];
            sumProdSquared += Math.pow(products[i], 2);
        }

        return new double[]{sumProd, sumProdSquared};
    }

    /**
     * Performs blocking operations on the intensity data.
     *
     * @param blockCount       The number of blocks.
     * @param currentIncrement The current increment value.
     * @param varianceBlocks   The variance blocks.
     * @param directMonitor    The direct monitor value.
     * @param delayedMonitor   The delayed monitor value.
     * @param products         The array of products.
     * @param numProducts      The number of products.
     */
    private void performBlockingOperations(int blockCount, int currentIncrement, double[][] varianceBlocks,
                                           double directMonitor, double delayedMonitor, double[] products,
                                           int[] numProducts) {
        double sumProd, sumProdSquared;

        for (int i = 1; i < blockCount; i++) {
            numProducts[i] = numProducts[i - 1] / 2;
            sumProd = sumProdSquared = 0.0;
            for (int j = 0; j < numProducts[i]; j++) {
                products[j] = (products[2 * j] + products[2 * j + 1]) / 2;
                sumProd += products[j];
                sumProdSquared += products[j] * products[j];
            }

            // the time of the block curve
            varianceBlocks[0][i] = (currentIncrement * Math.pow(2, i)) * settings.getFrameTime();

            // value of the block curve
            varianceBlocks[1][i] = (sumProdSquared / numProducts[i] - Math.pow(sumProd / numProducts[i], 2)) /
                    (numProducts[i] * Math.pow(directMonitor * delayedMonitor, 2));
        }
    }

    /**
     * Processes the correlation data for a specific lag time.
     *
     * @param i                   The index of the lag time.
     * @param blockCount          The number of blocks.
     * @param numFrames           The number of frames.
     * @param numBinnedDataPoints The number of binned data points.
     * @param intensityBlock      The intensity data block.
     * @param varianceBlocks      The variance blocks.
     * @param numProducts         The number of products.
     * @param currentIncrement    The current increment value.
     */
    private void processCorrelationData(int i, int blockCount, int numFrames, int numBinnedDataPoints,
                                        double[][] intensityBlock, double[][] varianceBlocks, int[] numProducts,
                                        int currentIncrement) {
        int delay = lags[i] / currentIncrement;
        numProducts[0] = numBinnedDataPoints - delay;

        double[] monitors = calculateMonitors(numProducts[0], intensityBlock, delay);
        double directMonitor = monitors[0];
        double delayedMonitor = monitors[1];

        double[] products = new double[numFrames];
        double[] sumProds =
                calculateCorrelations(numProducts[0], intensityBlock, directMonitor, delayedMonitor, products, delay);
        double sumProd = sumProds[0];
        double sumProdSquared = sumProds[1];

        varianceBlocks[0][0] = currentIncrement * settings.getFrameTime();
        varianceBlocks[1][0] =
                (sumProdSquared / (numBinnedDataPoints - delay) - Math.pow(sumProd / numProducts[0], 2)) /
                        (numProducts[0] * Math.pow(directMonitor * delayedMonitor, 2));

        performBlockingOperations(blockCount, currentIncrement, varianceBlocks, directMonitor, delayedMonitor, products,
                numProducts);
    }

    /**
     * Determines the last index meeting the criteria for blocking.
     *
     * @param pixelModel     The pixel model.
     * @param blockCount     The number of blocks.
     * @param varianceBlocks The variance blocks.
     * @param lowerQuartile  The lower quartile values.
     * @param upperQuartile  The upper quartile values.
     * @return The last index meeting the criteria.
     */
    private int determineLastIndexMeetingCriteria(PixelModel pixelModel, int blockCount, double[][] varianceBlocks,
                                                  double[] lowerQuartile, double[] upperQuartile) {
        int lastIndexMeetingCriteria = -1;
        int index = 0;

        for (int i = 0; i < blockCount - 2; i++) {
            // Check if neighboring points have overlapping error bars
            boolean overlap = haveOverlappingErrorBars(i, upperQuartile, lowerQuartile) &&
                    haveOverlappingErrorBars(i + 1, upperQuartile, lowerQuartile);
            // Check if these three points are the last triple with increasing differences
            boolean isIncreasing = isIncreasing(i, varianceBlocks) && isIncreasing(i + 1, varianceBlocks);

            if (!overlap && isIncreasing) {
                lastIndexMeetingCriteria = i;
            }
        }

        if (lastIndexMeetingCriteria != -1) {
            for (int i = lastIndexMeetingCriteria + 1; i < blockCount - 4; i++) {
                // Check if neighboring points have overlapping error bars
                boolean overlap = haveOverlappingErrorBars(i, upperQuartile, lowerQuartile) &&
                        haveOverlappingErrorBars(i + 1, upperQuartile, lowerQuartile);

                if (overlap) {
                    index = i + 1;
                    break;
                }
            }
        }

        if (index == 0) {
            // optimal blocking is not possible, use maximal blocking
            pixelModel.setBlocked(0);
            index = (blockCount > 3) ? blockCount - 3 : blockCount - 1;
        } else {
            pixelModel.setBlocked(1);
        }

        return Math.max(index, correlatorQ - 1);
    }

    /**
     * Checks if the error bars of neighboring points overlap.
     *
     * @param index         The index of the point.
     * @param upperQuartile The upper quartile values.
     * @param lowerQuartile The lower quartile values.
     * @return True if the error bars overlap, false otherwise.
     */
    private boolean haveOverlappingErrorBars(int index, double[] upperQuartile, double[] lowerQuartile) {
        return upperQuartile[index] > lowerQuartile[index + 1] && upperQuartile[index + 1] > lowerQuartile[index];
    }

    /**
     * Checks if the variance values are increasing.
     *
     * @param index          The index of the point.
     * @param varianceBlocks The variance blocks.
     * @return True if the variance values are increasing, false otherwise.
     */
    private boolean isIncreasing(int index, double[][] varianceBlocks) {
        return varianceBlocks[1][index + 1] - varianceBlocks[1][index] > 0;
    }

    /**
     * Calculates the block variance.
     *
     * @param products       The array of products.
     * @param directMonitor  The direct monitor value.
     * @param delayedMonitor The delayed monitor value.
     * @param numProducts    The number of products.
     * @return The block variance.
     */
    private double calculateBlockVariance(double[] products, double directMonitor, double delayedMonitor,
                                          double numProducts) {
        double sumProd = 0.0;
        double sumProdSquared = 0.0;

        for (int i = 0; i < numProducts; i++) {
            // calculate the sum of prod, i.e. the raw correlation value
            sumProd += products[i];
            sumProdSquared += Math.pow(products[i], 2);
        }

        // variance after blocking; extra division by numProduct to obtain SEM
        return (sumProdSquared / numProducts - Math.pow(sumProd / numProducts, 2)) /
                ((numProducts - 1) * Math.pow(directMonitor * delayedMonitor, 2));
    }

    /**
     * Calculates the mean covariance for the given products and monitors.
     *
     * @param products        The array of products.
     * @param directMonitors  The direct monitor values.
     * @param delayedMonitors The delayed monitor values.
     * @param minProducts     The minimum number of products.
     * @return The mean covariance.
     */
    private double[] calculateMeanCovariance(double[][] products, double[] directMonitors, double[] delayedMonitors,
                                             int minProducts) {
        double[] meanCovariance = new double[settings.getChannelNumber()];
        for (int i = 1; i < settings.getChannelNumber(); i++) {
            for (int j = 0; j < minProducts; j++) {
                meanCovariance[i] += products[i][j] / (directMonitors[i] * delayedMonitors[i]);
            }
            // normalize by the number of products
            meanCovariance[i] /= minProducts;
        }

        return meanCovariance;
    }

    /**
     * Calculates the covariance matrix for the given products and monitors.
     *
     * @param covarianceMatrix The covariance matrix.
     * @param products         The array of products.
     * @param meanCovariance   The mean covariance values.
     * @param directMonitors   The direct monitor values.
     * @param delayedMonitors  The delayed monitor values.
     * @param minProducts      The minimum number of products.
     */
    private void calculateCovarianceMatrix(double[][] covarianceMatrix, double[][] products, double[] meanCovariance,
                                           double[] directMonitors, double[] delayedMonitors, int minProducts) {
        for (int i = 1; i < settings.getChannelNumber(); i++) {
            for (int j = 1; j <= i; j++) {
                for (int k = 0; k < minProducts; k++) {
                    covarianceMatrix[i][j] += (products[i][k] / (directMonitors[i] * delayedMonitors[i]) *
                            (products[j][k] / (directMonitors[j] * delayedMonitors[j]) - meanCovariance[j]));
                }
                covarianceMatrix[i][j] /= (minProducts - 1);
                // lower triangular part is equal to upper triangular part
                covarianceMatrix[j][i] = covarianceMatrix[i][j];
            }
        }
    }

    /**
     * Calculates the variance shrinkage weight for the given covariance matrix and products.
     *
     * @param covarianceMatrix The covariance matrix.
     * @param products         The array of products.
     * @param meanCovariance   The mean covariance values.
     * @param directMonitors   The direct monitor values.
     * @param delayedMonitors  The delayed monitor values.
     * @param minProducts      The minimum number of products.
     * @return The variance shrinkage weight.
     */
    private double calculateVarianceShrinkageWeight(double[][] covarianceMatrix, double[][] products,
                                                    double[] meanCovariance, double[] directMonitors,
                                                    double[] delayedMonitors, int minProducts) {
        double[] diagonalCovarianceMatrix = new double[settings.getChannelNumber()];

        for (int i = 1; i < settings.getChannelNumber(); i++) {
            diagonalCovarianceMatrix[i] = covarianceMatrix[i][i];
        }

        Arrays.sort(diagonalCovarianceMatrix);
        double pos1 = Math.floor((diagonalCovarianceMatrix.length - 1.0) / 2.0);
        double pos2 = Math.ceil((diagonalCovarianceMatrix.length - 1.0) / 2.0);

        if (pos1 == pos2) {
            median = diagonalCovarianceMatrix[(int) pos1];
        } else {
            median = (diagonalCovarianceMatrix[(int) pos1] + diagonalCovarianceMatrix[(int) pos2]) / 2.0;
        }

        double numerator = 0;
        double denominator = 0;
        for (int i = 1; i < settings.getChannelNumber(); i++) {
            double tmp = 0;
            for (int j = 0; j < minProducts; j++) {
                tmp += Math.pow(
                        (Math.pow(products[i][j] / (directMonitors[i] * delayedMonitors[i]) - meanCovariance[i], 2) -
                                covarianceMatrix[i][i]), 2);
            }
            tmp *= minProducts / Math.pow(minProducts - 1, 3);
            numerator += tmp;
            denominator += Math.pow(covarianceMatrix[i][i] - median, 2);
        }

        return Math.max(Math.min(1, numerator / denominator), 0);
    }

    /**
     * Calculates the covariance shrinkage weight for the given products and covariance matrix.
     *
     * @param products          The array of products.
     * @param meanCovariance    The mean covariance values.
     * @param directMonitors    The direct monitor values.
     * @param delayedMonitors   The delayed monitor values.
     * @param covarianceMatrix  The covariance matrix.
     * @param correlationMatrix The correlation matrix.
     * @param minProducts       The minimum number of products.
     * @return The covariance shrinkage weight.
     */
    private double calculateCovarianceShrinkageWeight(double[][] products, double[] meanCovariance,
                                                      double[] directMonitors, double[] delayedMonitors,
                                                      double[][] covarianceMatrix, double[][] correlationMatrix,
                                                      int minProducts) {
        for (int i = 1; i < settings.getChannelNumber(); i++) {
            for (int j = 1; j < settings.getChannelNumber(); j++) {
                correlationMatrix[i][j] =
                        covarianceMatrix[i][j] / Math.sqrt(covarianceMatrix[i][i] * covarianceMatrix[j][j]);
            }
        }

        double numerator = 0.0;
        double denominator = 0.0;

        double cmx, cmy, tmp;

        // determine the variance of the covariance
        for (int i = 1; i < settings.getChannelNumber(); i++) {
            tmp = 0.0;
            // sum only the upper triangle as the matrix is symmetric
            for (int j = 1; j < i; j++) {
                for (int k = 0; k < minProducts; k++) {
                    cmx = (products[i][k] / (directMonitors[i] * delayedMonitors[i]) - meanCovariance[i]) /
                            Math.sqrt(covarianceMatrix[i][i]);
                    cmy = (products[j][k] / (directMonitors[j] * delayedMonitors[j]) - meanCovariance[j]) /
                            Math.sqrt(covarianceMatrix[j][j]);
                    tmp += Math.pow(cmx * cmy - correlationMatrix[i][j], 2);
                }
                tmp *= minProducts / Math.pow(minProducts - 1, 3);
                numerator += tmp;
                // sum of squares of off-diagonal elements of correlation matrix
                denominator += Math.pow(correlationMatrix[i][j], 2);
            }
        }

        return Math.max(Math.min(1, numerator / denominator), 0);
    }

    /**
     * Regularizes the covariance matrix using the given weights and correlation matrix.
     *
     * @param covarianceMatrix          The covariance matrix.
     * @param correlationMatrix         The correlation matrix.
     * @param varianceShrinkageWeight   The variance shrinkage weight.
     * @param covarianceShrinkageWeight The covariance shrinkage weight.
     * @param minProducts               The minimum number of products.
     */
    private void regularizeCovarianceMatrix(double[][] covarianceMatrix, double[][] correlationMatrix,
                                            double varianceShrinkageWeight, double covarianceShrinkageWeight,
                                            int minProducts) {
        double cmx, cmy;

        // calculate the off-diagonal elements of the regularized variance-covariance matrix
        for (int i = 1; i < settings.getChannelNumber(); i++) {
            for (int j = 1; j < i; j++) {
                cmx = varianceShrinkageWeight * median + (1 - varianceShrinkageWeight) * covarianceMatrix[i][i];
                cmy = varianceShrinkageWeight * median + (1 - varianceShrinkageWeight) * covarianceMatrix[j][j];
                regularizedCovarianceMatrix[i - 1][j - 1] =
                        (1 - covarianceShrinkageWeight) * correlationMatrix[i][j] * Math.sqrt(cmx * cmy) / minProducts;
                regularizedCovarianceMatrix[j - 1][i - 1] = regularizedCovarianceMatrix[i - 1][j - 1];
            }
        }

        // diagonal elements of the regularized variance-covariance matrix
        for (int i = 1; i < settings.getChannelNumber(); i++) {
            regularizedCovarianceMatrix[i - 1][i - 1] =
                    (varianceShrinkageWeight * median + (1 - varianceShrinkageWeight) * covarianceMatrix[i][i]) /
                            minProducts;
        }
    }

    /**
     * Calculates the correlation function (CF) for the given pixel model and intensity blocks.
     *
     * @param pixelModel      The pixel model to store the results.
     * @param intensityBlocks The array of intensity values for the two traces which are correlated.
     * @param numFrames       The number of frames.
     */
    private void calculateCorrelationFunction(PixelModel pixelModel, double[][] intensityBlocks, int numFrames) {
        // intensityBlocks is the array of intensity values for the two traces witch are correlated
        pixelModel.setStandardDeviationCF(new double[settings.getChannelNumber()]);
        pixelModel.setVarianceCF(new double[settings.getChannelNumber()]);

        double[][] covarianceMatrix = new double[settings.getChannelNumber()][settings.getChannelNumber()];
        // the final results does not contain information about the zero lagtime kcf
        regularizedCovarianceMatrix = new double[settings.getChannelNumber() - 1][settings.getChannelNumber() - 1];

        int[] numProducts = new int[settings.getChannelNumber()];
        double[][] products = new double[settings.getChannelNumber()][numFrames];

        double[] correlationMean = new double[settings.getChannelNumber()];

        // direct and delayed monitors required for ACF normalization
        double[] directMonitors = new double[settings.getChannelNumber()];
        double[] delayedMonitors = new double[settings.getChannelNumber()];

        int numBinnedDataPoints = numFrames;
        int currentIncrement = BLOCK_LAG;
        int minProducts = (int) (numSamples[settings.getChannelNumber() - 1] / Math.pow(2,
                Math.max(blockIndex - Math.log(sampleTimes[settings.getChannelNumber() - 1]) / Math.log(2), 0)));

        // count how often the data was binned
        int binCount = 0;

        for (int i = 0; i < settings.getChannelNumber(); i++) {
            if (currentIncrement != sampleTimes[i]) {
                currentIncrement = sampleTimes[i];
                numBinnedDataPoints /= 2;
                binCount++;

                binData(numBinnedDataPoints, intensityBlocks);
            }

            int delay = lags[i] / currentIncrement;
            numProducts[i] = numBinnedDataPoints - delay;

            double[] monitors = calculateMonitors(numProducts[i], intensityBlocks, delay);
            directMonitors[i] = monitors[0];
            delayedMonitors[i] = monitors[1];

            double[] sumProds =
                    calculateCorrelations(numProducts[i], intensityBlocks, directMonitors[i], delayedMonitors[i],
                            products[i], delay);

            correlationMean[i] = sumProds[0] / (numProducts[i] * directMonitors[i] * delayedMonitors[i]);

            int binTimes = blockIndex - binCount;
            // bin the data until block time is reached
            for (int j = 1; j <= binTimes; j++) {
                // for each binning the number of data point is halfed
                numProducts[i] /= 2;
                for (int k = 0; k < numProducts[i]; k++) {
                    // do the binning and divide by 2 so that the average value does not change
                    products[i][k] = (products[i][2 * k] + products[i][2 * k + 1]) / 2;
                }
            }

            // use only the minimal number of products to achieve a symmetric variance matrix
            numProducts[i] = minProducts;

            pixelModel.getVarianceCF()[i] =
                    calculateBlockVariance(products[i], directMonitors[i], delayedMonitors[i], numProducts[i]);
            pixelModel.getStandardDeviationCF()[i] = Math.sqrt(pixelModel.getVarianceCF()[i]);
        }

        // if GLS is selected, then calculate the regularized covariance matrix
        if (fitModel.isGLS()) {
            pixelModel.setCorrelationFunction(
                    calculateMeanCovariance(products, directMonitors, delayedMonitors, minProducts));
            calculateCovarianceMatrix(covarianceMatrix, products, pixelModel.getCorrelationFunction(), directMonitors,
                    delayedMonitors, minProducts);
            double varianceShrinkageWeight =
                    calculateVarianceShrinkageWeight(covarianceMatrix, products, pixelModel.getCorrelationFunction(),
                            directMonitors, delayedMonitors, minProducts);

            double[][] correlationMatrix = new double[settings.getChannelNumber()][settings.getChannelNumber()];
            double covarianceShrinkageWeight =
                    calculateCovarianceShrinkageWeight(products, pixelModel.getCorrelationFunction(), directMonitors,
                            delayedMonitors, covarianceMatrix, correlationMatrix, minProducts);

            regularizeCovarianceMatrix(covarianceMatrix, correlationMatrix, varianceShrinkageWeight,
                    covarianceShrinkageWeight, minProducts);
        } else {
            // hand over the correlation function CorrelationMean; they differ only slightly
            pixelModel.setCorrelationFunction(correlationMean);
        }
    }

    /**
     * Reset the saved results.
     */
    public void resetResults() {
        CorrelationWorker.cancelPreviousInstance();

        pixelModels = null;
        lastUsedPixelModel = null;
        lags = null;
        lagTimes = null;
        sampleTimes = null;
        numSamples = null;
    }

    public Map<DccfDirection, double[][]> getDccf() {
        return dccf;
    }

    public double[][] getDccf(DccfDirection direction) {
        return dccf.get(direction);
    }

    public void setDccf(DccfDirection direction, double[][] dccf) {
        this.dccf.put(direction, dccf);
    }

    public double[][] getRegularizedCovarianceMatrix() {
        return regularizedCovarianceMatrix;
    }

    public PixelModel getPixelModel(int x, int y) {
        return pixelModels[x][y];
    }

    public void resetPixelModel(int x, int y) {
        pixelModels[x][y] = null;
    }

    public PixelModel[][] getPixelModels() {
        return pixelModels;
    }

    public void setPixelModels(PixelModel[][] pixelModels) {
        this.pixelModels = pixelModels;
    }

    public int[] getLags() {
        return lags;
    }

    public double[] getLagTimes() {
        return lagTimes;
    }

    public void setLagTimes(double[] lagTimes) {
        this.lagTimes = lagTimes;
    }

    public int[] getNumSamples() {
        return numSamples;
    }

    public int[] getSampleTimes() {
        return sampleTimes;
    }

    public void setSampleTimes(int[] sampleTimes) {
        this.sampleTimes = sampleTimes;
    }

    public double[][] getVarianceBlocks() {
        return varianceBlocks;
    }

    public int getBlockIndex() {
        return blockIndex;
    }

    public Pair<Point[], PixelModel> getLastUsedPixelModel() {
        return lastUsedPixelModel;
    }

    public void setLastUsedPixelModel(Pair<Point[], PixelModel> lastUsedPixelModel) {
        this.lastUsedPixelModel = lastUsedPixelModel;
    }
}
