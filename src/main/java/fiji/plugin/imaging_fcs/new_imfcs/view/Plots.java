package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
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
    private static final Map<String, ImageWindow> dccfWindows = new HashMap<>();
    private static final Map<String, HistogramWindow> dccfHistogramWindows = new HashMap<>();
    public static ImagePlus imgParam;
    private static PlotWindow blockingCurveWindow, acfWindow, standardDeviationWindow, intensityTraceWindow, msdWindow,
            residualsWindow, scatterWindow;
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
     * Plots the Correlation Function for given pixels.
     *
     * @param pixelModels The models containing the CF and fitted CF values.
     * @param lagTimes    The lag times corresponding to the CF values.
     * @param pixels      The points representing the pixels (or null if we are using an ROI).
     * @param binning     The binning factor.
     * @param distance    The distance used for cross-correlation function.
     * @param fitStart    The starting index for the fitted CF range.
     * @param fitEnd      The ending index for the fitted CF range.
     */
    public static void plotCorrelationFunction(List<PixelModel> pixelModels, double[] lagTimes, Point[] pixels,
                                               Point binning, Dimension distance, int fitStart, int fitEnd) {
        double minScale = Double.MAX_VALUE;
        double maxScale = -Double.MAX_VALUE;

        Plot plot = new Plot("CF plot", "tau [s]", "G (tau)");
        plot.setFrameSize(ACF_DIMENSION.width, ACF_DIMENSION.height);
        plot.setLogScaleX();
        plot.setJustification(Plot.CENTER);
        String description = getDescription(pixels, binning, distance);

        plot.setColor(Color.BLUE);
        plot.addLabel(0.5, 0, description);

        for (PixelModel pixelModel : pixelModels) {
            Pair<Double, Double> minMax = findAdjustedMinMax(pixelModel.getAcf());
            minScale = Math.min(minScale, minMax.getLeft());
            maxScale = Math.max(maxScale, minMax.getRight());

            plot.setColor(Color.BLUE);
            plot.addPoints(lagTimes, pixelModel.getAcf(), Plot.LINE);

            // Plot the fitted ACF
            if (pixelModel.isFitted()) {
                plot.setColor(Color.RED);
                plot.addPoints(Arrays.copyOfRange(lagTimes, fitStart, fitEnd + 1),
                        Arrays.copyOfRange(pixelModel.getFittedAcf(), fitStart, fitEnd + 1), Plot.LINE);
            }
        }

        plot.setLimits(lagTimes[1], 2 * lagTimes[lagTimes.length - 1], minScale, maxScale);
        plot.draw();

        acfWindow = plotWindow(plot, acfWindow, ACF_POSITION);
    }

    /**
     * Generates a description string based on the given pixel points, binning, and distance.
     * The description indicates the type of correlation (ACF or CFF) and the coordinates or region of interest (ROI).
     *
     * @param pixels   An array of Point objects representing pixel coordinates. If not null, should contain exactly
     *                 two points.
     * @param binning  A Point object representing the binning dimensions (x and y).
     * @param distance A Dimension object representing the separation distance between regions of interest (width and
     *                 height).
     * @return A formatted string describing the correlation type, points or ROIs, and binning dimensions.
     */
    private static String getDescription(Point[] pixels, Point binning, Dimension distance) {
        String correlationType = "ACF";
        String points = "ROI";
        if (distance.width != 0 || distance.height != 0) {
            correlationType = "CFF";
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
     * Plots the standard deviation for a given pixel.
     *
     * @param blockStandardDeviation The standard deviation values.
     * @param lagTimes               The lag times corresponding to the standard deviation values.
     * @param p                      The point representing the pixel.
     */
    public static void plotStandardDeviation(double[] blockStandardDeviation, double[] lagTimes, Point p) {
        Pair<Double, Double> minMax = findAdjustedMinMax(blockStandardDeviation);
        double min = minMax.getLeft();
        double max = minMax.getRight();

        Plot plot = new Plot("StdDev", "time [s]", "SD");
        plot.setColor(Color.BLUE);
        plot.addPoints(lagTimes, blockStandardDeviation, Plot.LINE);
        plot.setFrameSize(STANDARD_DEVIATION_DIMENSION.width, STANDARD_DEVIATION_DIMENSION.height);
        plot.setLogScaleX();
        plot.setLimits(lagTimes[1], lagTimes[lagTimes.length - 1], min, max);
        plot.setJustification(Plot.CENTER);
        plot.addLabel(0.5, 0, String.format(" StdDev (%d, %d)", p.x, p.y));
        plot.draw();

        // TODO: Add other lines if DC-FCCS(2D) and FCCSDisplay is selected
        standardDeviationWindow = plotWindow(plot, standardDeviationWindow, STANDARD_DEVIATION_POSITION);
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
     * Plots the mean square displacement (MSD) for a list of pixel models.
     *
     * @param pixelModels The list of PixelModel objects containing MSD values.
     * @param lagTimes    The lag times corresponding to the MSD values.
     * @param p           The point representing the pixel (can be null for the entire ROI).
     * @param binning     The binning point representing the binning factor in x and y directions.
     */
    public static void plotMSD(List<PixelModel> pixelModels, double[] lagTimes, Point p, Point binning) {
        Plot plot = new Plot("MSD", "time [s]", "MSD (um^2)");
        plot.setFrameSize(MSD_DIMENSION.width, MSD_DIMENSION.height);
        plot.setColor(Color.BLUE);
        plot.setJustification(Plot.CENTER);

        double minScale = Double.MAX_VALUE;
        double maxScale = -Double.MAX_VALUE;

        int msdMinLen = Integer.MAX_VALUE;

        for (PixelModel pixelModel : pixelModels) {
            double[] msd = pixelModel.getMSD();
            Pair<Double, Double> minMax = findAdjustedMinMax(msd);
            minScale = Math.min(minScale, minMax.getLeft());
            maxScale = Math.max(maxScale, minMax.getRight());

            double[] msdTime = Arrays.copyOfRange(lagTimes, 0, msd.length);
            msdMinLen = Math.min(msdMinLen, msdTime.length);

            plot.addPoints(msdTime, msd, Plot.LINE);
        }

        plot.setLimits(lagTimes[1], lagTimes[msdMinLen - 1], minScale, maxScale);
        String label = String.format("the ROI at %dx%d binning.", binning.x, binning.y);
        if (p != null) {
            label = String.format("(%d, %d).", p.x, p.y);
        }

        plot.addLabel(0.5, 0, "MSD of " + label);
        plot.draw();

        msdWindow = plotWindow(plot, msdWindow, MSD_POSITION);
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
     * Converts a point to a different scale based on binning factors and adjusts based on the minimum position.
     *
     * @param p               the original point.
     * @param minimumPosition the minimum position to adjust the point.
     * @param binning         the binning factors to be applied.
     * @return the converted point with binning factors applied.
     */
    private static Point convertPointToBinning(Point p, Point minimumPosition, Point binning) {
        return new Point(p.x / binning.x - minimumPosition.x, p.y / binning.y - minimumPosition.y);
    }

    /**
     * Calculates the dimensions of an image based on the minimum and maximum positions.
     *
     * @param minimumPosition the minimum position in the image.
     * @param maximumPosition the maximum position in the image.
     * @return the dimension of the image calculated from the positions.
     */
    private static Dimension getConvertedImageDimension(Point minimumPosition, Point maximumPosition) {
        return new Dimension(maximumPosition.x - minimumPosition.x + 1, maximumPosition.y - minimumPosition.y + 1);
    }

    /**
     * Plots parameter maps based on the pixel model and given image dimensions, creating an ImagePlus object.
     *
     * @param pixelModel      the model containing pixel parameters.
     * @param p               the point to be plotted.
     * @param minimumPosition the minimum position in the image.
     * @param maximumPosition the maximum position in the image.
     * @param binning         the binning factors to be applied.
     * @param mouseListener   the mouse listener to handle mouse events on the plotted image.
     * @return the ImagePlus object containing the plotted parameter maps.
     */
    public static ImagePlus plotParameterMaps(PixelModel pixelModel, Point p, Point minimumPosition,
                                              Point maximumPosition, Point binning, MouseListener mouseListener) {
        Pair<String, Double>[] params = pixelModel.getParams();

        // convert dimension using binning
        Point binningPoint = convertPointToBinning(p, minimumPosition, binning);
        Dimension convertedDimension = getConvertedImageDimension(minimumPosition, maximumPosition);

        boolean initImg = false;
        if (imgParam == null || !imgParam.isVisible()) {
            initImg = true;
            imgParam = IJ.createImage("Maps", "GRAY32", convertedDimension.width, convertedDimension.height,
                    params.length);

            // Set all pixel values to NaN
            IntStream.range(1, imgParam.getStackSize() + 1).forEach(slice -> {
                ImageProcessor ip = imgParam.getStack().getProcessor(slice);
                for (int x = 0; x < convertedDimension.width; x++) {
                    for (int y = 0; y < convertedDimension.height; y++) {
                        ip.putPixelValue(x, y, Double.NaN);
                    }
                }
            });

            // create the window and adapt the image scale
            imgParam.show();
            ImageWindow window = imgParam.getWindow();
            window.setLocation(PARAMETER_POSITION);
            ImageModel.adaptImageScale(imgParam);
            ApplyCustomLUT.applyCustomLUT(imgParam, "Red Hot");

            // add key listener on both the window on the canvas to support if the user uses its keyboard after clicking
            // on the window only or after clicking on the image.
            window.addKeyListener(keyAdjustmentListener());
            imgParam.getCanvas().addKeyListener(keyAdjustmentListener());

            // Add listener to switch the histogram if the slice is changed
            for (Component component : window.getComponents()) {
                if (component instanceof ScrollbarWithLabel) {
                    ScrollbarWithLabel scrollbar = (ScrollbarWithLabel) component;
                    scrollbar.addAdjustmentListener(imageAdjusted());
                }
            }

            // Add a mouse listener to plot the correlations functions
            imgParam.getCanvas().addMouseListener(mouseListener);
        }

        // Enter value from the end to be on the first slice on output
        for (int i = params.length - 1; i >= 0; i--) {
            ImageProcessor ip = imgParam.getStack().getProcessor(i + 1);
            ip.putPixelValue(binningPoint.x, binningPoint.y, params[i].getRight());
            if (initImg) {
                imgParam.setSlice(i + 1);
                IJ.run("Set Label...", "label=" + params[i].getLeft());
            }
        }

        IJ.run(imgParam, "Enhance Contrast", "saturated=0.35");

        return imgParam;
    }

    /**
     * Plots a histogram window for the given parameter image.
     *
     * @param imgParam the ImagePlus object for which the histogram is to be plotted.
     */
    public static void plotParamHistogramWindow(ImagePlus imgParam) {
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
            plotParamHistogramWindow(imgParam);
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
                    plotParamHistogramWindow(imgParam);
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
