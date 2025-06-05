package fiji.plugin.imaging_fcs.imfcs.controller;

import fiji.plugin.imaging_fcs.imfcs.enums.BackgroundMode;
import fiji.plugin.imaging_fcs.imfcs.enums.FitFunctions;
import fiji.plugin.imaging_fcs.imfcs.gpu.GpuCorrelator;
import fiji.plugin.imaging_fcs.imfcs.model.*;
import fiji.plugin.imaging_fcs.imfcs.model.correlations.Correlator;
import fiji.plugin.imaging_fcs.imfcs.model.correlations.SelectedPixel;
import fiji.plugin.imaging_fcs.imfcs.utils.Pair;
import fiji.plugin.imaging_fcs.imfcs.utils.Range;
import fiji.plugin.imaging_fcs.imfcs.view.ImageView;
import fiji.plugin.imaging_fcs.imfcs.view.Plots;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Roi;

import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.swing.SwingUtilities;

import static fiji.plugin.imaging_fcs.imfcs.model.correlations.MeanSquareDisplacement.correlationToMSD;

/**
 * The ImageController class handles the interactions with the image data,
 * managing the loading, processing, and analysis of images for FCS data.
 */
public final class ImageController {
    private final ImageModel imageModel;
    private final BackgroundSubtractionController backgroundSubtractionController;
    private final BleachCorrectionModel bleachCorrectionModel;
    private final Correlator correlator;
    private final FitController fitController;
    private final ExpSettingsModel settings;
    private final OptionsModel options;
    private final Consumer<Integer> setLastFrame;
    private Runnable refreshThresholdView = () -> {};
    private Runnable setDiffusionLawRange = () -> {};
    private ImageView imageView;
    private int previousX = -1;
    private int previousY = -1;

    /**
     * Constructs a new ImageController with the given dependencies.
     *
     * @param imageModel                      The image model.
     * @param backgroundSubtractionController The background subtraction controller.
     * @param fitController                   The fit controller.
     * @param bleachCorrectionModel           The bleach correction model.
     * @param correlator                      The correlator.
     * @param settings                        The experimental settings model.
     * @param options                         The options model.
     * @param setLastFrame                    A {@code Consumer<Integer>} to set the
     *                                        last frame index.
     */
    public ImageController(ImageModel imageModel, BackgroundSubtractionController backgroundSubtractionController,
            FitController fitController, BleachCorrectionModel bleachCorrectionModel,
            Correlator correlator, ExpSettingsModel settings, OptionsModel options,
            Consumer<Integer> setLastFrame) {
        this.imageModel = imageModel;
        this.backgroundSubtractionController = backgroundSubtractionController;
        this.fitController = fitController;
        this.bleachCorrectionModel = bleachCorrectionModel;
        this.correlator = correlator;
        this.settings = settings;
        this.options = options;
        this.setLastFrame = setLastFrame;
        imageView = null;
    }

    /**
     * Checks if an image is currently loaded in the model.
     *
     * @return true if an image is loaded, false otherwise.
     */
    public boolean isImageLoaded() {
        return imageModel.isImageLoaded();
    }

    /**
     * Retrieves the currently loaded image from the model.
     *
     * @return The ImagePlus object representing the currently loaded image.
     */
    public ImagePlus getImage() {
        return imageModel.getImage();
    }

    /**
     * Retrieves the dimension of the currently loaded image from the model.
     *
     * @return The Dimension object representing the dimension of the currently
     *         loaded image.
     */
    public Dimension getImageDimension() {
        return imageModel.getDimension();
    }

    /**
     * Retrieves the directory of the currently loaded image.
     *
     * @return The directory path as a String.
     */
    public String getDirectory() {
        return imageModel.getDirectory();
    }

    /**
     * Retrieves the full path of the currently loaded image.
     *
     * @return The image path as a String.
     */
    public String getImagePath() {
        return imageModel.getImagePath();
    }

    /**
     * Retrieves the file name of the currently loaded image.
     *
     * @return The file name as a String.
     */
    public String getFileName() {
        return imageModel.getFileName();
    }

    /**
     * Converts the model's state to a map representation.
     *
     * @return A map containing key-value pairs representing the model's state.
     */
    public Map<String, Object> toMap() {
        return imageModel.toMap();
    }

    /**
     * Populates the model's state from a map representation.
     *
     * @param data A map containing key-value pairs used to set the model's state.
     */
    public void fromMap(Map<String, Object> data) {
        imageModel.fromMap(data);
    }

    /**
     * Updates the image model's filter array using the current filter settings.
     */
    public void setFilterArray() {
        imageModel.setFilterArray(settings.getFilter(), settings.getFilterLowerLimit(), settings.getFilterUpperLimit(),
                settings.getFirstFrame(), settings.getLastFrame());
    }

    /**
     * Loads the given image into the model and sets up the view and event
     * listeners.
     *
     * @param image The ImagePlus instance to load.
     */
    public void loadImage(ImagePlus image, String simulationName) {
        // reset the results if we load a new image
        correlator.resetResults();
        Plots.closePlots();

        imageModel.loadImage(image, simulationName);

        initializeAndDisplayImage();

        // set the default end for diffusion law
        setDiffusionLawRange.run();

        // set the filter array
        setFilterArray();
    }

    /**
     * Initializes the image view, sets up the display, and attaches event
     * listeners.
     */
    public void initializeAndDisplayImage() {
        imageView = new ImageView();
        imageView.showImage(imageModel);

        ImagePlus image = imageModel.getImage();
        image.getCanvas().addMouseListener(imageMouseClicked());
        image.getCanvas().addKeyListener(imageKeyPressed());

        ImageModel.adaptImageScale(image);

        setLastFrame.accept(imageModel.getStackSize());

        imageModel.getBackgroundModel().computeBackground(image);
        backgroundSubtractionController.setTfBackgrounds();
    }

    /**
     * Computes background values according to the current {@link BackgroundMode}.
     * Depending on the mode, it calculates minimum values frame-by-frame, pixel-wise,
     * or uses a constant value. If a separate background image is loaded
     */
    public void computeBackground() {
        imageModel.getBackgroundModel().computeBackground(imageModel.getImage());
    }

    /**
     * Unloads the current image from the model.
     */
    public void unloadImage() {
        imageModel.unloadImage();
    }

    /**
     * Bring the loaded image to front
     */
    public void toFront() {
        if (isImageLoaded()) {
            imageModel.getWindow().toFront();
        }
    }

    /**
     * Performs the correlation and fitting for a pixel at the given coordinates.
     * If FCCS display is enabled, it additionally fits the FCCS model. It also
     * optionally computes
     * the Mean Squared Displacement (MSD) if enabled in the settings.
     *
     * @param x                      The x-coordinate of the pixel.
     * @param y                      The y-coordinate of the pixel.
     * @param singlePixelCorrelation A boolean indicating if it is a single pixel
     *                               correlation.
     * @return An array of Points representing the cursor positions.
     */
    private Point[] correlatePixel(int x, int y, boolean singlePixelCorrelation) {
        SelectedPixel selectedPixel = new SelectedPixel(imageModel, correlator, settings);

        Point[] cursorPositions = selectedPixel.performCorrelationFunctionEvaluation(x, y, singlePixelCorrelation);
        Point pixel = cursorPositions[0];

        PixelModel pixelModel = correlator.getPixelModel(pixel.x, pixel.y);

        if (settings.isFCCSDisp()) {
            fitFCCS(pixelModel.getAcf1PixelModel(), pixelModel.getAcf2PixelModel(), x, y);
        }

        fitController.fit(pixelModel, settings.getFitModel(), correlator.getLagTimes(),
                correlator.getRegularizedCovarianceMatrix(), x, y);

        if (pixelModel.isFitted()) {
            fitController.updateThresholds(pixelModel);
            refreshThresholdView.run();
        }

        if (settings.isMSD()) {
            pixelModel.setMSD(
                    correlationToMSD(pixelModel.getCorrelationFunction(), settings.getParamAx(), settings.getParamAy(),
                            settings.getParamW(), settings.getSigmaZ(), settings.isMSD3d()));
        }

        return cursorPositions;
    }

    /**
     * Performs fitting for the given pixel models at the specified coordinates.
     * This method fits two pixel models (acf1Model and acf2Model) using different
     * fit settings and lag times.
     * If MSD (Mean Squared Displacement) is enabled in the settings, it calculates
     * and sets the MSD for both models.
     *
     * @param acf1Model The first pixel model to be fitted (ACF1).
     * @param acf2Model The second pixel model to be fitted (ACF2).
     * @param x         The x-coordinate of the pixel.
     * @param y         The y-coordinate of the pixel.
     */
    private void fitFCCS(PixelModel acf1Model, PixelModel acf2Model, int x, int y) {
        fitController.fit(acf1Model, FitFunctions.ITIR_FCS_2D, correlator.getLagTimes(),
                correlator.getRegularizedCovarianceMatrix(), x, y);
        fitController.fit(acf2Model, FitFunctions.ITIR_FCS_2D_2, correlator.getLagTimes(),
                correlator.getRegularizedCovarianceMatrix(), x, y);

        if (settings.isMSD()) {
            acf1Model.setMSD(
                    correlationToMSD(acf1Model.getCorrelationFunction(), settings.getParamAx(), settings.getParamAy(),
                            settings.getParamW(), settings.getSigmaZ(), settings.isMSD3d()));
            acf2Model.setMSD(
                    correlationToMSD(acf2Model.getCorrelationFunction(), settings.getParamAx(), settings.getParamAy(),
                            settings.getParamW(), settings.getSigmaZ(), settings.isMSD3d()));
        }
    }

    /**
     * Correlates a single pixel at the specified coordinates and plots the results.
     *
     * @param x The x-coordinate of the pixel.
     * @param y The y-coordinate of the pixel.
     */
    private void correlateSinglePixelAndPlot(int x, int y) {
        try {
            Point[] cursorPositions = correlatePixel(x, y, true);
            plotResuts(cursorPositions);
            plotFittedParams(cursorPositions);
        } catch (Exception e) {
            IJ.showMessage("Error",
                    String.format("Fail to correlate points for x=%d, y=%d with error: %s", x, y, e.getMessage()));
        }
    }

    /**
     * Plots multiple PixelModels.
     *
     * @param pixelModels A list of PixelModels to plot.
     */
    public void plotMultiplePixelsModels(List<PixelModel> pixelModels) {
        Plots.plotCorrelationFunction(pixelModels, settings.isFCCSDisp(), correlator.getLagTimes(), null,
                settings.getBinning(), settings.getCCF(), fitController.getFitStart(), fitController.getFitEnd());
        if (settings.isMSD()) {
            Plots.plotMSD(pixelModels, correlator.getLagTimes(), null, settings.getBinning(), settings.isFCCSDisp());
        }
    }

    /**
     * Checks if the specified pixel is fully contained within the given Region of
     * Interest (ROI).
     * A pixel is considered fully contained if all four corners, defined by the
     * top-left corner (x, y)
     * and the binning dimensions, are within the ROI.
     *
     * @param imgRoi The Region of Interest (ROI) to check against.
     * @param x      The x-coordinate of the top-left corner of the pixel in the
     *               image.
     * @param y      The y-coordinate of the top-left corner of the pixel in the
     *               image.
     * @return {@code true} if the pixel defined by (x, y) and its binning
     *         dimensions
     *         is fully within the ROI; {@code false} otherwise.
     */
    private boolean isPixelInRoi(Roi imgRoi, int x, int y) {
        return imgRoi.contains(x, y) && imgRoi.contains(x + settings.getBinning().x - 1, y) &&
                imgRoi.contains(x, y + settings.getBinning().y - 1) &&
                imgRoi.contains(x + settings.getBinning().x - 1, y + settings.getBinning().y - 1);
    }

    /**
     * Correlates the given ROI while periodically checking for cancellation.
     * Iterates over binned pixels and stops processing if cancelChecker returns true
     * or the thread is interrupted.
     *
     * @param imgRoi        the ROI to correlate
     * @param cancelChecker a Supplier that returns true when cancellation is requested
     */
    public void correlateROI(Roi imgRoi, Supplier<Boolean> cancelChecker) {
        Point pixelBinning = settings.getPixelBinning();
        Rectangle rect = imgRoi.getBounds();

        Range xRange, yRange;

        try {
            xRange = new Range((int) Math.ceil(rect.x / (double) pixelBinning.x),
                    (int) Math.ceil((rect.x + rect.width - settings.getBinning().x) / (double) pixelBinning.x), 1);
            yRange = new Range((int) Math.ceil(rect.y / (double) pixelBinning.y),
                    (int) Math.ceil((rect.y + rect.height - settings.getBinning().y) / (double) pixelBinning.y), 1);
        } catch (IllegalArgumentException e) {
            // catch if the range isn't valid
            IJ.showMessage("ROI does not cover a whole single pixel in the binned image.");
            return;
        }

        if (options.isUseGpu()) {
            GpuCorrelator gpuCorrelator =
                    new GpuCorrelator(settings, bleachCorrectionModel, imageModel, fitController.getModel(), false,
                            correlator, xRange, yRange);
            gpuCorrelator.correlateAndFit(xRange, yRange, fitController.isActivated(), true);
            SwingUtilities.invokeLater(this::plotAll);
        } else {
            List<PixelModel> correlatedPixels = new ArrayList<>();

            for (int x = xRange.getStart(); x <= xRange.getEnd(); x += xRange.getStep()) {
                // Check for interruption
                if (cancelChecker.get() || Thread.currentThread().isInterrupted()) {
                    return;
                }

                for (int y = yRange.getStart(); y <= yRange.getEnd(); y += yRange.getStep()) {
                    // Check for interruption again to respond quickly
                    if (cancelChecker.get() || Thread.currentThread().isInterrupted()) {
                        return;
                    }

                    try {
                        if ((isPixelInRoi(imgRoi, x * pixelBinning.x, y * pixelBinning.y)) &&
                            !imageModel.isPixelFiltered(x * pixelBinning.x, y * pixelBinning.y)) {
                            Point[] points = correlatePixel(x, y, false);

                            PixelModel pixelModel = correlator.getPixelModel(points[0].x, points[0].y);
                            correlatedPixels.add(pixelModel);

                            SwingUtilities.invokeLater(() -> plotFittedParams(points));
                        }
                    } catch (Exception e) {
                        IJ.log(String.format("Fail to correlate points for x=%d, y=%d with error: %s", x, y,
                                e.getMessage()));
                    }
                }
                    IJ.showProgress(x - xRange.getStart(), xRange.length());
            }

            plotMultiplePixelsModels(correlatedPixels);
        }
    }

    /**
     * Correlates the given ROI without cancellation support.
     *
     * @param imgRoi the ROI to correlate
     */
    public void correlateROI(Roi imgRoi) {
        correlateROI(imgRoi, () -> false);
    }

    /**
     * Validates if the given ROI is within the image bounds.
     *
     * @param roi The Region of Interest to check.
     * @return {@code true} if the ROI is within bounds, {@code false} otherwise.
     */
    public boolean isROIValid(Roi roi) {
        return imageModel.isROIValid(roi);
    }

    /**
     * Checks if the given ROI overlaps when using the DC-FCCS model.
     *
     * @param roi The Region of Interest to check.
     * @return True if the ROI is larger than the DCFCCS region, false otherwise.
     */
    public boolean isROIOverlapInDCFCCS(Roi roi) {
        if (settings.getFitModel() == FitFunctions.DC_FCCS_2D) {
            Rectangle rect = roi.getBounds();

            return Math.abs(settings.getCCF().width) < rect.width && Math.abs(settings.getCCF().height) < rect.height;
        }

        return false;
    }

    /**
     * Plots the results for a pixel at the given coordinates.
     *
     * @param cursorPositions The array of cursor positions where the two first
     *                        elements represent the pixel
     *                        coordinates.
     */
    private void plotResuts(Point[] cursorPositions) {
        Point p = cursorPositions[0];
        PixelModel pixelModel = correlator.getPixelModel(p.x, p.y);

        if (options.isPlotACFCurves()) {
            Plots.plotCorrelationFunction(Collections.singletonList(pixelModel), settings.isFCCSDisp(),
                    correlator.getLagTimes(), cursorPositions, settings.getBinning(), settings.getCCF(),
                    fitController.getFitStart(), fitController.getFitEnd());
        }

        if (options.isPlotSDCurves()) {
            Plots.plotStandardDeviation(pixelModel, correlator.getLagTimes(), p, settings.isFCCSDisp());
        }

        if (options.isPlotIntensityCurves() && isImageLoaded()) {
            Point p2 = cursorPositions[1];
            if (bleachCorrectionModel.getIntensityTrace1() == null) {
                correlator.correlatePixelModel(pixelModel, imageModel.getImage(), p.x, p.y, p2.x, p2.y,
                        settings.getFirstFrame(), settings.getLastFrame());
            }
            Plots.plotIntensityTrace(bleachCorrectionModel.getIntensityTrace1(),
                    bleachCorrectionModel.getIntensityTrace2(), bleachCorrectionModel.getIntensityTime(),
                    cursorPositions);

        }

        if (options.isPlotBlockingCurve()) {
            Plots.plotBlockingCurve(correlator.getVarianceBlocks(), correlator.getBlockIndex());
        }

        if (options.isPlotCovMats() && fitController.isGLS()) {
            Plots.plotCovarianceMatrix(correlator.getRegularizedCovarianceMatrix());
        }

        if (settings.isMSD()) {
            Plots.plotMSD(Collections.singletonList(pixelModel), correlator.getLagTimes(), p, settings.getBinning(),
                    settings.isFCCSDisp());
        }

        if (options.isPlotResCurves() && pixelModel.isFitted() && pixelModel.getResiduals() != null) {
            Plots.plotResiduals(pixelModel.getResiduals(), correlator.getLagTimes(), p);
        }
    }

    /**
     * Plots the fitted parameters at the specified cursor positions.
     * This method retrieves the pixel model at the given position, checks if it has
     * been fitted,
     * and if so, plots the parameter maps. Additionally, if the option is enabled,
     * it plots the
     * parameter histograms.
     *
     * @param cursorPositions An array of Points representing the cursor positions.
     */
    public void plotFittedParams(Point[] cursorPositions) {
        Point p = cursorPositions[0];
        PixelModel pixelModel = correlator.getPixelModel(p.x, p.y);

        Point binningPoint = settings.convertPointToBinning(p);

        if (pixelModel.isFitted() && !fitController.needToFilter(pixelModel, binningPoint.x, binningPoint.y)) {

            Plots.plotParameterMaps(pixelModel, binningPoint, settings.getConvertedImageDimension(getImageDimension()),
                    imageParamClicked(), settings.isFCCSDisp());

            if (options.isPlotParaHist()) {
                Plots.plotParamHistogramWindow();
            }
        }
    }

    /**
     * Plots all the pixel models that have valid ACF (Autocorrelation Function)
     * data.
     * This method iterates through all the pixel models, identifies those with
     * non-null ACF data,
     * computes the intensity trace for each valid pixel model, and plots the fitted
     * parameters.
     * Finally, it plots multiple pixel models together (ACF and MSD).
     */
    public void plotAll() {
        List<PixelModel> pixelModelList = new ArrayList<>();
        PixelModel[][] pixelModels = correlator.getPixelModels();

        Plots.updateParameterMaps(pixelModels, settings.getConvertedImageDimension(getImageDimension()),
                settings::convertPointToBinning, imageParamClicked(), fitController, options.isPlotParaHist(),
                settings.isFCCSDisp());

        for (PixelModel[] pixelModelsRow : pixelModels) {
            for (PixelModel currentPixelModel : pixelModelsRow) {
                if (currentPixelModel != null && currentPixelModel.getCorrelationFunction() != null) {
                    pixelModelList.add(currentPixelModel);
                }
            }
        }

        plotMultiplePixelsModels(pixelModelList);
    }

    /**
     * Creates a MouseListener to handle mouse clicks on the image canvas.
     *
     * @return The MouseListener instance.
     */
    public MouseListener imageMouseClicked() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                int x = imageModel.getCanvas().offScreenX(event.getX());
                int y = imageModel.getCanvas().offScreenY(event.getY());

                if (!settings.isOverlap()) {
                    x /= settings.getBinning().x;
                    y /= settings.getBinning().y;

                    previousX = x * settings.getBinning().x;
                    previousY = y * settings.getBinning().y;
                }

                correlateSinglePixelAndPlot(x, y);
            }
        };
    }

    /**
     * Creates a KeyListener to handle key presses on the image canvas.
     *
     * @return The KeyListener instance.
     */
    public KeyListener imageKeyPressed() {
        return new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent event) {
                Roi roi = imageModel.getRoi();
                if (roi != null) {
                    int x = (int) roi.getXBase();
                    int y = (int) roi.getYBase();

                    if (!settings.isOverlap()) {
                        // Move the roi by the size of the binning
                        x = previousX + (x - previousX) * settings.getBinning().x;
                        y = previousY + (y - previousY) * settings.getBinning().y;

                        x /= settings.getBinning().x;
                        y /= settings.getBinning().y;

                        // Here we multiply by the binning again to make sure that X and Y are factor of
                        // the binning
                        // It is only useful if the binning was changed by the user in the meantime
                        previousX = x * settings.getBinning().x;
                        previousY = y * settings.getBinning().y;
                    }

                    correlateSinglePixelAndPlot(x, y);
                }
            }
        };
    }

    /**
     * Creates and returns a MouseListener that responds to mouse clicks on an
     * ImageCanvas.
     * This listener retrieves pixel coordinates from the clicked location, checks
     * if the pixel
     * model is correlated, updates fit parameters on the view, and plots the
     * results.
     * In case of a double click, it resets this pixel.
     *
     * @return a MouseListener that handles mouse click events on an ImageCanvas.
     */
    public MouseListener imageParamClicked() {
        return new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent event) {
                Object source = event.getSource();

                if (source instanceof ImageCanvas) {
                    ImageCanvas canvas = (ImageCanvas) source;
                    ImageWindow window = (ImageWindow) canvas.getParent();
                    ImagePlus img = window.getImagePlus();

                    // reset ROI
                    img.deleteRoi();

                    int x = canvas.offScreenX(event.getX());
                    int y = canvas.offScreenY(event.getY());

                    // The pixel is not correlated on this pixel
                    if (Double.isNaN(img.getStack().getProcessor(1).getPixelValue(x, y))) {
                        return;
                    }

                    Point pixelBinning = settings.getPixelBinning();
                    Point minimumPosition = settings.getMinCursorPosition();

                    x = (x + minimumPosition.x) * pixelBinning.x;
                    y = (y + minimumPosition.y) * pixelBinning.y;
                    PixelModel pixelModel = correlator.getPixelModel(x, y);

                    // if the pixel model is not correlated we do not plot
                    if (pixelModel == null || pixelModel.getCorrelationFunction() == null) {
                        return;
                    }

                    // if it's a double click, then we reset this pixel model
                    if (event.getClickCount() == 2) {
                        correlator.resetPixelModel(x, y);
                        Plots.resetImgParamPixel(x, y);
                        return;
                    }

                    if (pixelModel.isFitted()) {
                        fitController.updateFitParams(pixelModel.getFitParams());
                    }

                    int x2 = x + settings.getCCF().width;
                    int y2 = y + settings.getCCF().height;

                    Point[] points = new Point[] { new Point(x, y), new Point(x2, y2) };

                    correlator.setLastUsedPixelModel(new Pair<>(points, pixelModel));
                    plotResuts(points);
                }
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                Object source = event.getSource();

                if (source instanceof ImageCanvas) {
                    ImageCanvas canvas = (ImageCanvas) source;
                    ImageWindow window = (ImageWindow) canvas.getParent();
                    ImagePlus img = window.getImagePlus();

                    Roi roi = img.getRoi();

                    if (roi != null && roi.getFeretsDiameter() > 1) {
                        roi.setStrokeColor(Color.BLUE);

                        Point pixelBinning = settings.getPixelBinning();
                        Point minimumPosition = settings.getMinCursorPosition();

                        List<PixelModel> pixelModels = SelectedPixel.getPixelModelsInRoi(roi, pixelBinning,
                                minimumPosition,
                                settings::convertPointToBinning, correlator.getPixelModels(), fitController);

                        plotMultiplePixelsModels(pixelModels);
                    }
                }
            }
        };
    }

    public void setRefreshThresholdView(Runnable refreshThresholdView) {
        this.refreshThresholdView = refreshThresholdView;
    }

    public void setSetDiffusionLawRange(Runnable setDiffusionLawRange) {
        this.setDiffusionLawRange = setDiffusionLawRange;
    }
}
