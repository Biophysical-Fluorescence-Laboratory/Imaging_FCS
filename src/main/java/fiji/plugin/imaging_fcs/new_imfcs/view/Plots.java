package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.controller.FitController;
import fiji.plugin.imaging_fcs.new_imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.PixelModel;
import fiji.plugin.imaging_fcs.new_imfcs.utils.ApplyCustomLUT;
import fiji.plugin.imaging_fcs.new_imfcs.utils.Pair;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;
import ij.process.ImageProcessor;
import ij.process.ImageStatistics;

import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

/**
 * The Plots class provides static methods for generating various plots used in fluorescence correlation spectroscopy
 * (FCS) analysis.
 * This includes plotting autocorrelation functions (ACF), blocking curves, covariance matrices, standard deviations,
 * intensity traces, and mean square displacements (MSD).
 */
public class Plots {
    private static final Point ACF_POSITION =
            new Point(Constants.MAIN_PANEL_POS.x + Constants.MAIN_PANEL_DIM.width + 10,
                    Constants.MAIN_PANEL_POS.y + 100);
    private static final Dimension ACF_DIMENSION = new Dimension(200, 200);
    private static final Dimension BLOCKING_CURVE_DIMENSION = new Dimension(200, 100);
    private static final Point STANDARD_DEVIATION_POSITION =
            new Point(ACF_POSITION.x + ACF_DIMENSION.width + 115, ACF_POSITION.y);
    private static final Dimension STANDARD_DEVIATION_DIMENSION = new Dimension(ACF_DIMENSION.width, 50);
    private static final Point BLOCKING_CURVE_POSITION =
            new Point(STANDARD_DEVIATION_POSITION.x + STANDARD_DEVIATION_DIMENSION.width + 110,
                    STANDARD_DEVIATION_POSITION.y);
    private static final Point COVARIANCE_POSITION =
            new Point(BLOCKING_CURVE_POSITION.x, BLOCKING_CURVE_POSITION.y + BLOCKING_CURVE_DIMENSION.height + 150);
    private static final Point MSD_POSITION =
            new Point(STANDARD_DEVIATION_POSITION.x + STANDARD_DEVIATION_DIMENSION.width + 165, ACF_POSITION.y);
    private static final Point RESIDUALS_POSITION = new Point(STANDARD_DEVIATION_POSITION.x,
            STANDARD_DEVIATION_POSITION.y + STANDARD_DEVIATION_DIMENSION.height + 145);
    private static final Point PARAMETER_POSITION =
            new Point(ACF_POSITION.x + ACF_DIMENSION.width + 80, Constants.MAIN_PANEL_POS.y);
    private static final Point PARAM_HISTOGRAM_POSITION = new Point(PARAMETER_POSITION.x + 280, PARAMETER_POSITION.y);
    private static final Point INTENSITY_POSITION =
            new Point(ACF_POSITION.x, ACF_POSITION.y + ACF_DIMENSION.height + 145);
    private static final Dimension INTENSITY_DIMENSION = new Dimension(ACF_DIMENSION.width, 50);
    private static final Dimension MSD_DIMENSION = new Dimension(ACF_DIMENSION);
    private static final Dimension RESIDUALS_DIMENSION = new Dimension(ACF_DIMENSION.width, 50);
    private static final Dimension PARAM_HISTOGRAM_DIMENSION = new Dimension(350, 250);
    private static final Dimension SCATTER_DIMENSION = new Dimension(200, 200);
    private static final Point SCATTER_POSITION = new Point(ACF_POSITION.x + 30, ACF_POSITION.y + 30);
    private static final Point DCCF_POSITION =
            new Point(ImageView.IMAGE_POSITION.x + 50, ImageView.IMAGE_POSITION.y + 50);
    private static final Dimension DCCF_HISTOGRAM_DIMENSION = new Dimension(350, 250);
    private static final Point DCCF_HISTOGRAM_POSITION = new Point(DCCF_POSITION.x + 280, DCCF_POSITION.y);
    private static final Dimension DIFFUSION_LAW_DIMENSION = new Dimension(200, 200);
    private static final Point DIFFUSION_LAW_POSITION = new Point(ACF_POSITION.x + 30, ACF_POSITION.y + 30);
    private static final Dimension PSF_DIMENSION = new Dimension(200, 200);
    private static final Point PSF_POSITION = new Point(ACF_POSITION.x + 30, ACF_POSITION.y + 30);
    private static final Map<String, ImageWindow> dccfWindows = new HashMap<>();
    private static final Map<String, HistogramWindow> dccfHistogramWindows = new HashMap<>();
    public static ImagePlus imgParam;
    private static PlotWindow blockingCurveWindow, acfWindow, standardDeviationWindow, intensityTraceWindow, msdWindow,
            residualsWindow, scatterWindow, diffusionLawWindow, psfWindow;
    private static ImageWindow imgCovarianceWindow;
    private static HistogramWindow paramHistogramWindow;

    // Prevent instantiation
    private Plots() {
    }

    /**
     * Finds the adjusted minimum and maximum values of an array, with a 10% margin.
     *
     * @param array The array of values.
     * @return A Pair containing the adjusted minimum and maximum values.
     */
    private static Pair<Double, Double> findAdjustedMinMax(double[] array) {
        if (array.length == 0) {
            throw new IllegalArgumentException("findAdjustedMinMax: array is empty");
        }
        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (int i = 1; i < array.length; i++) {
            min = Math.min(min, array[i]);
            max = Math.max(max, array[i]);
        }

        // maximum scales need to be 10% larger than maximum value and 10% smaller than minimum value
        min -= min * 0.1;
        max += max * 0.1;

        return new Pair<>(min, max);
    }

    /**
     * Selects the minimum and maximum values between two pairs of min and max values.
     *
     * @param minMax1 The first pair containing min and max values.
     * @param minMax2 The second pair containing min and max values.
     * @return A new pair where the left value is the minimum of the two mins, and the right value is the maximum of
     * the two maxes.
     */
    private static Pair<Double, Double> selectMinMax(Pair<Double, Double> minMax1, Pair<Double, Double> minMax2) {
        return new Pair<>(Math.min(minMax1.getLeft(), minMax2.getLeft()),
                Math.max(minMax1.getRight(), minMax2.getRight()));
    }

    /**
     * Displays the plot in a new window or updates the existing window.
     *
     * @param plot     The Plot to display.
     * @param window   The existing PlotWindow, if any.
     * @param position The position to display the window.
     * @return The updated PlotWindow.
     */
    private static PlotWindow plotWindow(Plot plot, PlotWindow window, Point position) {
        // Display the plot in a new window or update the existing one
        if (window == null || window.isClosed()) {
            window = plot.show();
            window.setLocation(position);
        } else {
            window.drawPlot(plot);
        }

        return window;
    }

    /**
     * Displays the image in a new window or updates the existing window.
     *
     * @param img      The ImagePlus to display.
     * @param window   The existing ImageWindow, if any.
     * @param position The position to display the window.
     * @return The updated ImageWindow.
     */
    private static ImageWindow plotImageWindow(ImagePlus img, ImageWindow window, Point position) {
        if (window == null || window.isClosed()) {
            img.show();
            window = img.getWindow();
            window.setLocation(position);
        } else {
            window.setImage(img);
        }

        return window;
    }

    /**
     * Plots the histogram window for the given image, creating a new window or updating an existing one.
     *
     * @param img       The ImagePlus object representing the image for which the histogram is plotted.
     * @param window    The existing HistogramWindow object, if any.
     * @param title     The title of the histogram window.
     * @param numBins   The number of bins for the histogram.
     * @param position  The position of the histogram window on the screen.
     * @param dimension The dimensions of the histogram window.
     * @return The HistogramWindow object representing the histogram window.
     */
    private static HistogramWindow plotHistogramWindow(ImagePlus img, HistogramWindow window, String title, int numBins,
                                                       Point position, Dimension dimension) {
        ImageStatistics statistics = img.getStatistics();

        if (window == null || window.isClosed()) {
            window = new HistogramWindow(title, img, numBins, statistics.histMin, statistics.histMax,
                    statistics.histYMax);
            window.setLocationAndSize(position.x, position.y, dimension.width, dimension.height);
        } else {
            window.showHistogram(img, numBins, statistics.histMin, statistics.histMax);
            window.setTitle(title);
        }

        return window;
    }

    /**
     * Plots the blocking curve with variance blocks and index highlighting.
     *
     * @param varianceBlocks The variance blocks.
     * @param index          The index to highlight.
     */
    public static void plotBlockingCurve(double[][] varianceBlocks, int index) {
        Plot plot = getBlockingCurvePlot(varianceBlocks);
        plot.setColor(Color.BLUE);
        plot.setJustification(Plot.CENTER);
        plot.addPoints(varianceBlocks[0], varianceBlocks[1], varianceBlocks[2], Plot.CIRCLE);
        plot.draw();

        // Highlight specific points if index is not zero
        if (index != 0) {
            double[][] blockPoints = new double[3][3];
            for (int i = -1; i <= 1; i++) {
                blockPoints[0][i + 1] = varianceBlocks[0][index + i];
                blockPoints[1][i + 1] = varianceBlocks[1][index + i];
                blockPoints[2][i + 1] = varianceBlocks[2][index + i];
            }
            plot.setColor(Color.RED);
            plot.addPoints(blockPoints[0], blockPoints[1], blockPoints[2], Plot.CIRCLE);
            plot.draw();
        }

        blockingCurveWindow = plotWindow(plot, blockingCurveWindow, BLOCKING_CURVE_POSITION);
    }

    /**
     * Creates a Plot for the blocking curve.
     *
     * @param varianceBlocks The variance blocks.
     * @return The created Plot.
     */
    private static Plot getBlockingCurvePlot(double[][] varianceBlocks) {
        Pair<Double, Double> minMax = findAdjustedMinMax(varianceBlocks[1]);
        double minBlock = minMax.getLeft();
        double maxBlock = minMax.getRight();

        Plot plot = new Plot("blocking", "x", "SD");
        plot.add("line", varianceBlocks[0], varianceBlocks[1]);

        plot.setFrameSize(BLOCKING_CURVE_DIMENSION.width, BLOCKING_CURVE_DIMENSION.height);
        plot.setLogScaleX();
        plot.setLimits(varianceBlocks[0][0] / 2, 2 * varianceBlocks[0][varianceBlocks[0].length - 1], minBlock,
                maxBlock);
        return plot;
    }

    /**
     * Plots the covariance matrix.
     *
     * @param regularizedCovarianceMatrix The covariance matrix to plot.
     */
    public static void plotCovarianceMatrix(double[][] regularizedCovarianceMatrix) {
        int len = regularizedCovarianceMatrix.length;
        ImagePlus imgCovariance = IJ.createImage("Covariance", "GRAY32", len, len, 1);

        ImageProcessor ip = imgCovariance.getProcessor();
        for (int x = 0; x < len; x++) {
            for (int y = 0; y < len; y++) {
                ip.putPixelValue(x, y, regularizedCovarianceMatrix[x][y]);
            }
        }

        imgCovarianceWindow = plotImageWindow(imgCovariance, imgCovarianceWindow, COVARIANCE_POSITION);

        // apply "Spectrum" LUT
        IJ.run(imgCovariance, "Spectrum", "");
        IJ.run(imgCovariance, "Enhance Contrast", "saturated=0.35");

        IJ.run(imgCovariance, "Set... ", "zoom=" + 200 + " x=" + 0 + " y=" + 0);
        // This needs to be used since ImageJ 1.48v to set the window to the right size;
        IJ.run(imgCovariance, "In [+]", "");
    }

    /**
     * Plots the Correlation Function (CF) for the given pixel models, and optionally FCCS models. It sets up the
     * plot with the corresponding lag times, fitted CF values, and descriptions, and adjusts the scale dynamically.
     *
     * @param pixelModels The list of PixelModel objects containing CF and fitted CF values.
     * @param lagTimes    Array of lag times corresponding to the CF values.
     * @param pixels      Array of Point objects representing the pixels (or null if using an ROI).
     * @param binning     Point object representing the binning factor.
     * @param distance    Dimension object representing the separation distance for cross-correlation.
     * @param fitStart    The starting index for the fitted CF range.
     * @param fitEnd      The ending index for the fitted CF range.
     */
    public static void plotCorrelationFunction(List<PixelModel> pixelModels, boolean FCCSDisp, double[] lagTimes,
                                               Point[] pixels, Point binning, Dimension distance, int fitStart,
                                               int fitEnd) {
        Plot plot = new Plot("CF plot", "tau [s]", "G (tau)");
        plot.setFrameSize(ACF_DIMENSION.width, ACF_DIMENSION.height);
        plot.setLogScaleX();
        plot.setJustification(Plot.CENTER);

        boolean fccsPixelsFound = false;

        double minScale = Double.MAX_VALUE;
        double maxScale = -Double.MAX_VALUE;
        for (PixelModel pixelModel : pixelModels) {
            // check if at least one pixel has an auto-correlation computed
            if (!fccsPixelsFound && pixelModel.getAcf1PixelModel() != null) {
                fccsPixelsFound = true;
            }

            Pair<Double, Double> minMax =
                    plotPixelModelCF(plot, pixelModel, lagTimes, fitStart, fitEnd, FCCSDisp, Color.BLUE,
                            FCCSDisp ? Color.BLACK : Color.RED);
            minScale = Math.min(minScale, minMax.getLeft());
            maxScale = Math.max(maxScale, minMax.getRight());
        }

        String description = getDescription(pixels, binning, distance, fccsPixelsFound && FCCSDisp);

        plot.setColor(Color.BLUE);
        plot.addLabel(0.5, 0, description);

        plot.setLimits(lagTimes[1], 2 * lagTimes[lagTimes.length - 1], minScale, maxScale);
        plot.draw();

        acfWindow = plotWindow(plot, acfWindow, ACF_POSITION);
    }

    /**
     * Generates a description string for the correlation based on pixel points, binning, and distance.
     * The description includes the correlation type (ACF or CFF), the points or ROIs, and binning dimensions.
     *
     * @param pixels   Array of Point objects representing pixel coordinates (should contain two points if not null).
     * @param binning  Point representing binning dimensions (x and y).
     * @param distance Dimension representing separation distance between ROIs.
     * @param fccs     Boolean indicating if FCCS correlation is used.
     * @return A formatted string describing the correlation type, points or ROIs, and binning.
     */
    private static String getDescription(Point[] pixels, Point binning, Dimension distance, boolean fccs) {
        String correlationType = "ACF";
        String points = "ROI";
        if (distance.width != 0 || distance.height != 0) {
            correlationType = fccs ? "ACF1, ACF2, CCF" : "CFF";
            points = String.format("ROIs with %dx%d separation", distance.width, distance.height);
        }

        if (pixels != null) {
            Point p1 = pixels[0];
            Point p2 = pixels[1];

            if (p1.equals(p2)) {
                points = String.format("(%d, %d)", p1.x, p1.y);
            } else {
                points = String.format("(%d, %d) and (%d, %d)", p1.x, p1.y, p2.x, p2.y);
            }
        }

        return String.format(" %s of %s at %dx%d binning.", correlationType, points, binning.x, binning.y);
    }

    /**
     * Plots the Correlation Function for the given PixelModel and optionally its FCCS (ACF1 and ACF2) components.
     *
     * @param plot       The Plot object for displaying the CF.
     * @param pixelModel The PixelModel with CF values.
     * @param lagTimes   Array of lag times for the CF values.
     * @param fitStart   Start index for fitted CF.
     * @param fitEnd     End index for fitted CF.
     * @param FCCSDisp   Boolean indicating if FCCS models are displayed.
     * @param color      Color for the CF plot.
     * @param fitColor   Color for the fitted CF plot.
     * @return Pair of min and max values for the CF scale.
     */
    private static Pair<Double, Double> plotPixelModelCF(Plot plot, PixelModel pixelModel, double[] lagTimes,
                                                         int fitStart, int fitEnd, boolean FCCSDisp, Color color,
                                                         Color fitColor) {

        Pair<Double, Double> minMax = findAdjustedMinMax(pixelModel.getCorrelationFunction());

        plot.setColor(color);
        plot.addPoints(lagTimes, pixelModel.getCorrelationFunction(), Plot.LINE);

        // Plot the fitted ACF
        plotFittedCF(plot, pixelModel, lagTimes, fitStart, fitEnd, fitColor);

        if (FCCSDisp && pixelModel.getAcf1PixelModel() != null) {
            minMax = selectMinMax(minMax,
                    plotPixelModelCF(plot, pixelModel.getAcf1PixelModel(), lagTimes, fitStart, fitEnd, false,
                            Color.GREEN, Color.BLACK));
            minMax = selectMinMax(minMax,
                    plotPixelModelCF(plot, pixelModel.getAcf2PixelModel(), lagTimes, fitStart, fitEnd, false, Color.RED,
                            Color.BLACK));
        }

        return minMax;
    }

    /**
     * Plots the fitted CF (Correlation Function) for the given pixel model.
     *
     * @param plot       The Plot object to add points to.
     * @param pixelModel The PixelModel containing the fitted ACF.
     * @param lagTimes   Array of lag times for the plot.
     * @param fitStart   The starting index for fitting.
     * @param fitEnd     The ending index for fitting.
     * @param color      The color to use for plotting.
     */
    private static void plotFittedCF(Plot plot, PixelModel pixelModel, double[] lagTimes, int fitStart, int fitEnd,
                                     Color color) {
        if (pixelModel.isFitted()) {
            plot.setColor(color);
            plot.addPoints(Arrays.copyOfRange(lagTimes, fitStart, fitEnd + 1),
                    Arrays.copyOfRange(pixelModel.getFittedCF(), fitStart, fitEnd + 1), Plot.LINE);
        }
    }

    /**
     * Plots the standard deviation for a given pixel model, optionally including FCCS models.
     *
     * @param pixelModel The PixelModel containing standard deviation values.
     * @param lagTimes   Array of lag times for the standard deviation values.
     * @param p          The Point representing the pixel.
     * @param FCCSDisp   Boolean indicating if FCCS models are displayed.
     */
    public static void plotStandardDeviation(PixelModel pixelModel, double[] lagTimes, Point p, boolean FCCSDisp) {
        Plot plot = new Plot("StdDev", "time [s]", "SD");
        plot.setColor(Color.BLUE);
        plot.setJustification(Plot.CENTER);
        plot.addLabel(0.5, 0, String.format(" StdDev (%d, %d)", p.x, p.y));

        Pair<Double, Double> minMax =
                plotLineStandardDeviation(plot, pixelModel.getStandardDeviationCF(), lagTimes, Color.BLUE);

        // plot the acf1 and acf2 pixel models if they are defined
        if (FCCSDisp && pixelModel.getAcf1PixelModel() != null) {
            minMax = selectMinMax(minMax,
                    plotLineStandardDeviation(plot, pixelModel.getAcf1PixelModel().getStandardDeviationCF(), lagTimes,
                            Color.GREEN));
            minMax = selectMinMax(minMax,
                    plotLineStandardDeviation(plot, pixelModel.getAcf2PixelModel().getStandardDeviationCF(), lagTimes,
                            Color.RED));
        }

        double min = minMax.getLeft();
        double max = minMax.getRight();

        plot.setFrameSize(STANDARD_DEVIATION_DIMENSION.width, STANDARD_DEVIATION_DIMENSION.height);
        plot.setLogScaleX();
        plot.setLimits(lagTimes[1], lagTimes[lagTimes.length - 1], min, max);
        plot.draw();

        standardDeviationWindow = plotWindow(plot, standardDeviationWindow, STANDARD_DEVIATION_POSITION);
    }

    /**
     * Plots a line representing standard deviation for a pixel model.
     *
     * @param plot                   The Plot object for displaying the standard deviation.
     * @param blockStandardDeviation Array of standard deviation values.
     * @param lagTimes               Array of lag times corresponding to the values.
     * @param color                  Color for plotting the line.
     * @return Pair of min and max values for the standard deviation scale.
     */
    private static Pair<Double, Double> plotLineStandardDeviation(Plot plot, double[] blockStandardDeviation,
                                                                  double[] lagTimes, Color color) {
        plot.setColor(color);
        plot.addPoints(lagTimes, blockStandardDeviation, Plot.LINE);

        return findAdjustedMinMax(blockStandardDeviation);
    }

    /**
     * Plots the intensity trace for given pixels.
     *
     * @param intensityTrace  The intensity trace values.
     * @param intensityTrace2 The second set of intensity trace values for comparison.
     * @param intensityTime   The time points corresponding to the intensity trace values.
     * @param pixels          The points representing the pixels.
     */
    public static void plotIntensityTrace(double[] intensityTrace, double[] intensityTrace2, double[] intensityTime,
                                          Point[] pixels) {
        Point p1 = pixels[0];
        Point p2 = pixels[1];

        Pair<Double, Double> minMax = findAdjustedMinMax(intensityTrace);
        double min = minMax.getLeft();
        double max = minMax.getRight();

        Plot plot = new Plot("Intensity Trace", "time [s]", "Intensity");
        plot.setFrameSize(INTENSITY_DIMENSION.width, INTENSITY_DIMENSION.height);
        plot.setLimits(intensityTime[1], intensityTime[intensityTime.length - 1], min, max);
        plot.setColor(Color.BLUE);
        plot.addPoints(intensityTime, intensityTrace, Plot.LINE);

        String description = String.format(" Intensity Trace (%d, %d)", p1.x, p1.y);
        if (!p1.equals(p2)) {
            description = String.format(" Intensity Trace (%d, %d) and (%d, %d)", p1.x, p1.y, p2.x, p2.y);
            plot.setColor(Color.RED);
            plot.addPoints(intensityTime, intensityTrace2, Plot.LINE);
            plot.setColor(Color.BLUE);
        }

        plot.setJustification(Plot.CENTER);
        plot.addLabel(0.5, 0, description);
        plot.draw();

        intensityTraceWindow = plotWindow(plot, intensityTraceWindow, INTENSITY_POSITION);
    }

    /**
     * Plots the mean square displacement (MSD) for the given pixel models.
     *
     * @param pixelModels List of PixelModel objects with MSD values.
     * @param lagTimes    Array of lag times for the MSD values.
     * @param p           The Point representing the pixel, or null for ROI.
     * @param binning     Binning factor as a Point.
     * @param FCCSDisp    Boolean indicating if FCCS models are displayed.
     */
    public static void plotMSD(List<PixelModel> pixelModels, double[] lagTimes, Point p, Point binning,
                               boolean FCCSDisp) {
        Plot plot = new Plot("MSD", "time [s]", "MSD (um^2)");
        plot.setFrameSize(MSD_DIMENSION.width, MSD_DIMENSION.height);
        plot.setJustification(Plot.CENTER);

        double minScale = Double.MAX_VALUE;
        double maxScale = -Double.MAX_VALUE;
        int msdMinLen = Integer.MAX_VALUE;
        boolean msdFound = false;

        // if FCCSDisp is activated then we consider the CCF, the ACF1 and the ACF2.
        // Otherwise, we just consider the CCF.
        int lenToConsider = FCCSDisp ? 3 : 1;

        Color[] colors = {Color.BLUE, Color.GREEN, Color.RED};

        for (PixelModel pixelModel : pixelModels) {
            PixelModel[] currentPixelModels = {
                    pixelModel, pixelModel.getAcf1PixelModel(), pixelModel.getAcf2PixelModel()
            };

            for (int i = 0; i < lenToConsider; i++) {
                if (currentPixelModels[i] != null && currentPixelModels[i].getMSD() != null) {
                    double[] msd = currentPixelModels[i].getMSD();
                    msdFound = true;

                    Pair<Double, Double> minMax = plotMSDLine(plot, msd, lagTimes, colors[i]);
                    minScale = Math.min(minScale, minMax.getLeft());
                    maxScale = Math.max(maxScale, minMax.getRight());
                    msdMinLen = Math.min(msdMinLen, msd.length);
                }
            }
        }

        // if no msd calculation was found then there is nothing to plot.
        if (!msdFound) {
            return;
        }

        plot.setLimits(lagTimes[1], lagTimes[msdMinLen - 1], minScale, maxScale);

        plot.setColor(Color.BLUE);
        String label = String.format("the ROI at %dx%d binning.", binning.x, binning.y);
        if (p != null) {
            label = String.format("(%d, %d).", p.x, p.y);
        }

        plot.addLabel(0.5, 0, "MSD of " + label);
        plot.draw();

        msdWindow = plotWindow(plot, msdWindow, MSD_POSITION);
    }

    /**
     * Plots the MSD line for a given pixel model.
     *
     * @param plot     The Plot object for displaying the MSD.
     * @param msd      Array of MSD values.
     * @param lagTimes Array of lag times corresponding to the MSD values.
     * @param color    Color for the MSD line.
     * @return Pair of min and max values for the MSD scale.
     */
    private static Pair<Double, Double> plotMSDLine(Plot plot, double[] msd, double[] lagTimes, Color color) {
        double[] msdTime = Arrays.copyOfRange(lagTimes, 0, msd.length);

        plot.setColor(color);
        plot.addPoints(msdTime, msd, Plot.LINE);

        return findAdjustedMinMax(msd);
    }

    /**
     * Plots the residuals for a given pixel.
     *
     * @param residuals The residual values.
     * @param lagTimes  The lag times corresponding to the residual values.
     * @param p         The point representing the pixel.
     */
    public static void plotResiduals(double[] residuals, double[] lagTimes, Point p) {
        Pair<Double, Double> minMax = findAdjustedMinMax(residuals);
        double min = minMax.getLeft();
        double max = minMax.getRight();

        Plot plot = new Plot("Residuals", "time [s]", "Res");
        plot.setFrameSize(RESIDUALS_DIMENSION.width, RESIDUALS_DIMENSION.height);
        plot.setLimits(lagTimes[1], lagTimes[lagTimes.length - 1], min, max);
        plot.setLogScaleX();
        plot.setColor(Color.BLUE);
        plot.addPoints(lagTimes, residuals, Plot.LINE);
        plot.setJustification(Plot.CENTER);
        plot.addLabel(0.5, 0, String.format(" Residuals (%d, %d)", p.x, p.y));
        plot.draw();

        residualsWindow = plotWindow(plot, residualsWindow, RESIDUALS_POSITION);
    }

    /**
     * Sets all pixel values in the given image to NaN within the specified dimensions.
     *
     * @param img       The ImagePlus object representing the image.
     * @param dimension The dimensions of the image in which to set pixel values to NaN.
     */
    public static void setAllPixelToNaN(ImagePlus img, Dimension dimension) {
        IntStream.range(1, img.getStackSize() + 1).forEach(slice -> {
            ImageProcessor ip = img.getStack().getProcessor(slice);
            for (int x = 0; x < dimension.width; x++) {
                for (int y = 0; y < dimension.height; y++) {
                    ip.putPixelValue(x, y, Double.NaN);
                }
            }
        });
    }

    /**
     * Sets the pixel at (x, y) to NaN across all slices in the image stack,
     * and updates the parameter histogram window.
     *
     * @param x the x-coordinate of the pixel to reset
     * @param y the y-coordinate of the pixel to reset
     */
    public static void resetImgParamPixel(int x, int y) {
        IntStream.range(1, imgParam.getStackSize() + 1).forEach(slice -> {
            ImageProcessor ip = imgParam.getStack().getProcessor(slice);
            ip.putPixelValue(x, y, Double.NaN);
        });

        IJ.run(imgParam, "Enhance Contrast", "satured=0.35");
        plotParamHistogramWindow();
    }

    /**
     * Displays the parameter maps in a window, adapts the image scale, applies a custom LUT,
     * and adds key and mouse listeners for user interaction.
     *
     * @param img           ImagePlus object representing the parameter maps to display.
     * @param mouseListener MouseListener to handle mouse events on the image canvas.
     */
    private static void showParameterMaps(ImagePlus img, MouseListener mouseListener) {
        // create the window and adapt the image scale
        img.show();
        ImageWindow window = img.getWindow();
        window.setLocation(PARAMETER_POSITION);
        ImageModel.adaptImageScale(img);
        ApplyCustomLUT.applyCustomLUT(img, "Red Hot");

        // add key listener on both the window on the canvas to support if the user uses its keyboard after clicking
        // on the window only or after clicking on the image.
        window.addKeyListener(keyAdjustmentListener());
        img.getCanvas().addKeyListener(keyAdjustmentListener());

        // Add listener to switch the histogram if the slice is changed
        for (Component component : window.getComponents()) {
            if (component instanceof ScrollbarWithLabel) {
                ScrollbarWithLabel scrollbar = (ScrollbarWithLabel) component;
                scrollbar.addAdjustmentListener(imageAdjusted());
            }
        }

        // Add a mouse listener to plot the correlations functions
        img.getCanvas().addMouseListener(mouseListener);
    }

    /**
     * Initializes an ImagePlus object for parameter maps, setting pixels to NaN and adding listeners.
     *
     * @param dimension     Dimensions of the image.
     * @param paramsLength  Number of slices (parameters to map).
     * @param mouseListener Listener for mouse events on the image.
     * @return Initialized ImagePlus object with parameter maps.
     */
    private static ImagePlus initParameterMaps(Dimension dimension, int paramsLength, MouseListener mouseListener) {
        ImagePlus img = IJ.createImage("Maps", "GRAY32", dimension.width, dimension.height, paramsLength);

        // Set all pixel values to NaN
        setAllPixelToNaN(img, dimension);

        if (mouseListener != null) {
            showParameterMaps(img, mouseListener);
        }

        return img;
    }

    /**
     * Updates parameter map values at a given point in the image.
     *
     * @param img     ImagePlus object representing the parameter maps.
     * @param p       Point in the image to update.
     * @param params  Array of parameter names and values.
     * @param initImg If true, initializes the image display.
     */
    private static void updateParameterMapsValue(ImagePlus img, Point p, Pair<String, Double>[] params,
                                                 boolean initImg) {
        // Enter value from the end to be on the first slice on output
        for (int i = params.length - 1; i >= 0; i--) {
            ImageProcessor ip = img.getStack().getProcessor(i + 1);
            ip.putPixelValue(p.x, p.y, params[i].getRight());
            if (initImg) {
                img.setSlice(i + 1);
                IJ.run("Set Label...", "label=" + params[i].getLeft());
            }
        }
    }

    /**
     * Plots parameter maps for a given pixel model at a specific point within the image dimensions.
     *
     * @param pixelModel    Model containing the parameters to plot.
     * @param p             Point in the image to plot parameters.
     * @param dimension     Image dimensions.
     * @param mouseListener Listener for mouse events on the image.
     */
    public static void plotParameterMaps(PixelModel pixelModel, Point p, Dimension dimension,
                                         MouseListener mouseListener) {
        Pair<String, Double>[] params = pixelModel.getParams();

        boolean initImg = false;
        if (imgParam == null || !imgParam.isVisible()) {
            initImg = true;
            imgParam = initParameterMaps(dimension, params.length, mouseListener);
        }

        updateParameterMapsValue(imgParam, p, params, initImg);
        IJ.run(imgParam, "Enhance Contrast", "saturated=0.35");
    }

    /**
     * Set parameter maps for a given pixel model at a specific point.
     *
     * @param img        ImagePlus object to update, or null to create a new one.
     * @param pixelModel Model containing parameters to map.
     * @param p          Point in the image to update.
     * @param dimension  Image dimensions.
     * @return ImagePlus object with updated parameter maps.
     */
    public static ImagePlus setParameterMaps(ImagePlus img, PixelModel pixelModel, Point p, Dimension dimension) {
        Pair<String, Double>[] params = pixelModel.getParams();

        if (img == null) {
            img = initParameterMaps(dimension, params.length, null);
        }

        updateParameterMapsValue(img, p, params, false);

        return img;
    }

    /**
     * Updates the parameter maps for a set of pixel models across the image dimensions,
     * creating or updating an ImagePlus object. Initializes the image if not already visible,
     * and sets pixel values for each parameter slice.
     *
     * @param pixelModels           The 2D array of PixelModel objects to be plotted.
     * @param dimension             The dimensions of the image.
     * @param convertPointToBinning Function to convert pixel coordinates to their corresponding binning points.
     * @param mouseListener         The MouseListener to handle mouse events on the plotted image.
     * @param fitController         The FitController used to determine if a pixel should be filtered.
     * @param plotParaHist          Flag indicating whether to plot the parameter histogram.
     */
    public static void updateParameterMaps(PixelModel[][] pixelModels, Dimension dimension,
                                           Function<Point, Point> convertPointToBinning, MouseListener mouseListener,
                                           FitController fitController, boolean plotParaHist) {
        int paramsLength = PixelModel.paramsName.length;

        boolean initImg = false;
        if (imgParam == null || !imgParam.isVisible()) {
            imgParam = initParameterMaps(dimension, paramsLength, mouseListener);
            initImg = true;
        } else {
            // Set all pixel values to NaN
            setAllPixelToNaN(imgParam, dimension);
        }

        for (int x = 0; x < pixelModels.length; x++) {
            for (int y = 0; y < pixelModels[0].length; y++) {
                PixelModel currentPixelModel = pixelModels[x][y];
                Point binningPoint = convertPointToBinning.apply(new Point(x, y));

                if (currentPixelModel != null && currentPixelModel.isFitted() &&
                        !fitController.needToFilter(currentPixelModel, binningPoint.x, binningPoint.y)) {
                    Pair<String, Double>[] params = currentPixelModel.getParams();
                    for (int i = paramsLength - 1; i >= 0; i--) {
                        ImageProcessor ip = imgParam.getStack().getProcessor(i + 1);

                        ip.putPixelValue(binningPoint.x, binningPoint.y, params[i].getRight());
                        if (initImg) {
                            imgParam.setSlice(i + 1);
                            IJ.run("Set Label...", "label=" + params[i].getLeft());
                        }
                    }
                }
            }
        }

        IJ.run(imgParam, "Enhance Contrast", "satured=0.35");

        if (plotParaHist) {
            plotParamHistogramWindow();
        }
    }

    /**
     * Plots a histogram window for the parameter image.
     */
    public static void plotParamHistogramWindow() {
        String title = PixelModel.paramsName[imgParam.getSlice() - 1];

        int numBins = getNumBins(imgParam.getStatistics());

        paramHistogramWindow =
                plotHistogramWindow(imgParam, paramHistogramWindow, title, numBins, PARAM_HISTOGRAM_POSITION,
                        PARAM_HISTOGRAM_DIMENSION);
    }

    /**
     * Calculates the number of bins for a histogram based on image statistics.
     *
     * @param statistics the image statistics.
     * @return the number of bins for the histogram.
     */
    private static int getNumBins(ImageStatistics statistics) {
        int firstQuartile = 0;
        long countQuartile = 0;

        while (countQuartile < Math.ceil(statistics.pixelCount / 4.0)) {
            countQuartile += statistics.getHistogram()[firstQuartile++];
        }

        int thirdQuartile = firstQuartile;
        while (countQuartile < Math.ceil(3.0 * statistics.pixelCount / 4.0)) {
            countQuartile += statistics.getHistogram()[thirdQuartile++];
        }

        double interQuartileDistance = (thirdQuartile - firstQuartile) * statistics.binSize;

        return interQuartileDistance > 0 ? (int) Math.ceil(
                Math.cbrt(statistics.pixelCount) * (statistics.histMax - statistics.histMin) /
                        (2.0 * interQuartileDistance)) : 10;
    }

    /**
     * Creates an AdjustmentListener that adjusts the image and plots the histogram when the image is adjusted.
     *
     * @return the AdjustmentListener.
     */
    private static AdjustmentListener imageAdjusted() {
        return (AdjustmentEvent ev) -> {
            IJ.run(imgParam, "Enhance Contrast", "saturated=0.35");
            plotParamHistogramWindow();
        };
    }

    /**
     * Creates a KeyListener that updates the histogram when a key is pressed, released, or typed.
     *
     * @return the KeyListener.
     */
    private static KeyListener keyAdjustmentListener() {
        return new KeyAdapter() {
            private int currentSlice = 0;

            @Override
            public void keyReleased(KeyEvent e) {
                updateHistogram();
            }

            @Override
            public void keyTyped(KeyEvent e) {
                updateHistogram();
            }

            @Override
            public void keyPressed(KeyEvent e) {
                updateHistogram();
            }

            private void updateHistogram() {
                if (currentSlice != imgParam.getSlice()) {
                    IJ.run(imgParam, "Enhance Contrast", "saturated=0.35");
                    plotParamHistogramWindow();
                    currentSlice = imgParam.getSlice();
                }
            }
        };
    }

    /**
     * Plots a scatter plot using the given data and labels.
     *
     * @param scPlot A 2D array containing the scatter plot data. The first row (scPlot[0])
     *               represents the y-values, and the second row (scPlot[1]) represents the x-values.
     * @param labelX The label for the x-axis.
     * @param labelY The label for the y-axis.
     */
    public static void scatterPlot(double[][] scPlot, String labelX, String labelY) {
        Pair<Double, Double> minMax = findAdjustedMinMax(scPlot[1]);
        double minX = minMax.getLeft();
        double maxX = minMax.getRight();

        minMax = findAdjustedMinMax(scPlot[0]);
        double minY = minMax.getLeft();
        double maxY = minMax.getRight();

        Plot plot = new Plot("Scatter plot", labelX, labelY);
        plot.setFrameSize(SCATTER_DIMENSION.width, SCATTER_DIMENSION.height);
        plot.setLimits(minX, maxX, minY, maxY);
        plot.setColor(Color.BLUE);
        plot.addPoints(scPlot[1], scPlot[0], Plot.CIRCLE);
        plot.setJustification(Plot.CENTER);
        plot.addLabel(0.5, 0, labelX + " vs " + labelY);

        plot.draw();

        scatterWindow = plotWindow(plot, scatterWindow, SCATTER_POSITION);
    }

    /**
     * Plots the DCCF window using the given DCCF data and direction name.
     *
     * @param dcff          A 2D array representing the computed DCCF values.
     * @param directionName The name of the direction for DCCF computation.
     */
    public static void plotDCCFWindow(double[][] dcff, String directionName) {
        int width = dcff.length;
        int height = dcff[0].length;

        ImagePlus img = IJ.createImage("DCCF - " + directionName, "GRAY32", width, height, 1);
        ImageProcessor ip = img.getStack().getProcessor(1);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                ip.putPixelValue(x, y, dcff[x][y]);
            }
        }

        // Plot the image in a new window or update the existing window
        dccfWindows.put(directionName, plotImageWindow(img, dccfWindows.get(directionName), DCCF_POSITION));

        IJ.run(img, "Enhance Contrast", "saturated=0.35");
        ImageModel.adaptImageScale(img);

        plotDCCFHistogram(img, directionName);
    }

    /**
     * Plots the histogram of the given DCCF image using the specified direction name.
     *
     * @param dcffImg       The ImagePlus object representing the DCCF image.
     * @param directionName The name of the direction for DCCF computation.
     */
    private static void plotDCCFHistogram(ImagePlus dcffImg, String directionName) {
        ImageStatistics stats = dcffImg.getStatistics();
        int numBins = (int) (Math.cbrt(stats.pixelCount) * (stats.histMax - stats.histMin) / (4 * stats.stdDev)) + 1;

        dccfHistogramWindows.put(directionName,
                plotHistogramWindow(dcffImg, dccfHistogramWindows.get(directionName), "Histogram - " + directionName,
                        numBins, DCCF_HISTOGRAM_POSITION, DCCF_HISTOGRAM_DIMENSION));
    }

    /**
     * Plots the diffusion law using the provided data arrays and value limits.
     * Visualizes the relationship between the effective area (Aeff) and the ratio Aeff/D with customized plot settings.
     *
     * @param effectiveArea     Array of effective area (Aeff) values in um^2.
     * @param time              Array of Aeff/D values in seconds.
     * @param standardDeviation Array of standard deviations for Aeff/D values.
     * @param minValue          Minimum value of Aeff/D for setting the Y-axis lower limit.
     * @param maxValue          Maximum value of Aeff/D for setting the Y-axis upper limit.
     */
    public static void plotDiffLaw(double[] effectiveArea, double[] time, double[] standardDeviation, double minValue,
                                   double maxValue) {
        Plot plot = new Plot("Diffusion Law", "Aeff [um^2]", "Aeff/D [s]");
        plot.setFrameSize(DIFFUSION_LAW_DIMENSION.width, DIFFUSION_LAW_DIMENSION.height);
        plot.setLimits(0, effectiveArea[effectiveArea.length - 1] * 1.1, minValue * 0.9, maxValue * 0.9);
        plot.setJustification(Plot.CENTER);
        plot.setColor(Color.BLUE);
        plot.addPoints(effectiveArea, time, standardDeviation, Plot.CIRCLE);
        plot.draw();

        plot.setLimitsToFit(true);

        diffusionLawWindow = plotWindow(plot, diffusionLawWindow, DIFFUSION_LAW_POSITION);
    }

    public static void plotFitDiffLaw(double intercept, double slope, double[][] fitFunctions) {
        Plot plot = diffusionLawWindow.getPlot();
        plot.addLabel(0.3, 0, String.format("%f + %f * Aeff", Math.floor(intercept * 100 + 0.5) / 100,
                Math.floor(slope * 100 + 0.5) / 100));
        plot.setColor(Color.RED);
        plot.addPoints(fitFunctions[0], fitFunctions[1], Plot.LINE);
        plot.draw();

        diffusionLawWindow = plotWindow(plot, diffusionLawWindow, DIFFUSION_LAW_POSITION);
    }

    public static boolean isPlotDiffLawOpen() {
        return diffusionLawWindow != null && !diffusionLawWindow.isClosed();
    }

    public static void closeDiffLawWindow() {
        closeWindow(diffusionLawWindow);
    }

    /**
     * Plots the Point Spread Function (PSF) results on a graph. This method visualizes the diffusion coefficients
     * across different binning values for various PSF values, along with error bars indicating the uncertainty
     * in the measurements. The plot includes labels for each PSF value and color codes the curves for clarity.
     *
     * @param minValue     The minimum diffusion coefficient value across all PSF results, used to set plot limits.
     * @param maxValue     The maximum diffusion coefficient value across all PSF results, used to set plot limits.
     * @param psfResults   A map containing the PSF values as keys and their corresponding binning values,
     *                     diffusion coefficients, and errors as the values in a 2D array.
     * @param binningStart The starting value of the binning range.
     * @param binningEnd   The ending value of the binning range.
     */
    public static void plotPSF(double minValue, double maxValue, Map<Double, double[][]> psfResults, int binningStart,
                               int binningEnd) {
        double[] labelPosX = {0.1, 0.25, 0.4, 0.55, 0.7, 0.1, 0.25, 0.4, 0.55, 0.7};
        double[] labelPosY = {0.8, 0.8, 0.8, 0.8, 0.8, 0.95, 0.95, 0.95, 0.95, 0.95};
        java.awt.Color[] colors = {
                Color.BLUE,
                Color.CYAN,
                Color.GREEN,
                Color.ORANGE,
                Color.PINK,
                Color.MAGENTA,
                Color.RED,
                Color.LIGHT_GRAY,
                Color.GRAY,
                Color.BLACK
        };

        Plot plot = new Plot("PSF", "binning", "D [um^2/s]");
        plot.setFrameSize(PSF_DIMENSION.width, PSF_DIMENSION.height);

        // make margin for plot label
        minValue /= 2;

        plot.setLimits(binningStart * 0.9, binningEnd * 1.1, minValue * 0.9, maxValue * 1.1);
        plot.setJustification(Plot.CENTER);

        int index = 0;
        for (Map.Entry<Double, double[][]> entry : psfResults.entrySet()) {
            double[] binning = entry.getValue()[0];
            double[] diffusionCoefficients = entry.getValue()[1];
            double[] errors = entry.getValue()[2];

            plot.setColor(colors[index % colors.length]);
            plot.addPoints(binning, diffusionCoefficients, errors, Plot.LINE);
            plot.addLabel(labelPosX[index], labelPosY[index], IJ.d2s(entry.getKey(), 2));
            index++;
        }

        plot.draw();

        psfWindow = plotWindow(plot, psfWindow, PSF_POSITION);
    }

    /**
     * Retrieves all relevant {@link ImageWindow} instances managed by the application.
     * <p>
     * This method dynamically collects various types of plot and analysis windows
     * into a list, and includes additional windows if available. The list is then
     * converted to an array and returned for further processing.
     * </p>
     *
     * @return an array of {@link ImageWindow} instances.
     */
    private static ImageWindow[] getImageWindows() {
        // Use an ArrayList to dynamically manage the ImageWindow elements
        List<ImageWindow> windowsList = new ArrayList<>();

        windowsList.add(blockingCurveWindow);
        windowsList.add(acfWindow);
        windowsList.add(standardDeviationWindow);
        windowsList.add(intensityTraceWindow);
        windowsList.add(msdWindow);
        windowsList.add(residualsWindow);
        windowsList.add(scatterWindow);
        windowsList.add(imgCovarianceWindow);
        windowsList.add(paramHistogramWindow);
        windowsList.add(diffusionLawWindow);
        windowsList.add(psfWindow);

        if (imgParam != null) {
            windowsList.add(imgParam.getWindow());
        }

        // Convert the list to an array and return
        return windowsList.toArray(new ImageWindow[0]);
    }

    /**
     * Closes the specified {@link ImageWindow} if it is not already closed.
     * <p>
     * This utility method checks if the provided window is non-null and open,
     * and if so, it closes the window. This ensures that any windows passed to
     * this method are properly disposed of, freeing up resources.
     * </p>
     *
     * @param window the {@link ImageWindow} to be closed. If the window is
     *               already closed or null, no action is taken.
     */
    private static void closeWindow(ImageWindow window) {
        if (window != null && !window.isClosed()) {
            window.close();
        }
    }

    /**
     * Closes all open windows managed by the application.
     * <p>
     * Iterates through a list of {@link ImageWindow} instances,
     * and closes each one if it is open, ensuring proper resource management.
     * </p>
     */
    public static void closePlots() {
        for (ImageWindow window : getImageWindows()) {
            closeWindow(window);
        }
    }

    /**
     * Brings all managed {@link ImageWindow} instances to the front.
     * <p>
     * Iterates through all open windows and brings each one to the front
     * if it is not null, ensuring they are visible to the user.
     * </p>
     */
    public static void toFront() {
        for (ImageWindow window : getImageWindows()) {
            if (window != null) {
                window.toFront();
            }
        }
    }
}
