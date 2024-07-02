package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.OptionsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.PixelModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.correlations.Correlator;
import fiji.plugin.imaging_fcs.new_imfcs.model.correlations.MeanSquareDisplacement;
import fiji.plugin.imaging_fcs.new_imfcs.model.correlations.SelectedPixel;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.BleachCorrectionModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.ImageView;
import fiji.plugin.imaging_fcs.new_imfcs.view.Plots;
import ij.IJ;
import ij.ImagePlus;
import ij.gui.Roi;

import java.awt.*;
import java.awt.event.*;

/**
 * The ImageController class handles the interactions with the image data,
 * managing the loading, processing, and analysis of images for FCS data.
 */
public final class ImageController {
    private final ImageModel imageModel;
    private final MainPanelController mainPanelController;
    private final BackgroundSubtractionController backgroundSubtractionController;
    private final BleachCorrectionModel bleachCorrectionModel;
    private final Correlator correlator;
    private final FitController fitController;
    private final ExpSettingsModel settings;
    private final OptionsModel options;
    private ImageView imageView;
    private int previousX = -1;
    private int previousY = -1;

    /**
     * Constructs a new ImageController with the given dependencies.
     *
     * @param mainPanelController             The main panel controller.
     * @param imageModel                      The image model.
     * @param backgroundSubtractionController The background subtraction controller.
     * @param fitController                   The fit controller.
     * @param bleachCorrectionModel           The bleach correction model.
     * @param correlator                      The correlator.
     * @param settings                        The experimental settings model.
     * @param options                         The options model.
     */
    public ImageController(MainPanelController mainPanelController, ImageModel imageModel,
                           BackgroundSubtractionController backgroundSubtractionController,
                           FitController fitController, BleachCorrectionModel bleachCorrectionModel,
                           Correlator correlator, ExpSettingsModel settings, OptionsModel options) {
        this.mainPanelController = mainPanelController;
        this.imageModel = imageModel;
        this.backgroundSubtractionController = backgroundSubtractionController;
        this.fitController = fitController;
        this.bleachCorrectionModel = bleachCorrectionModel;
        this.correlator = correlator;
        this.settings = settings;
        this.options = options;
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
     * Loads the given image into the model and sets up the view and event listeners.
     *
     * @param image The ImagePlus instance to load.
     */
    public void loadImage(ImagePlus image) {
        imageModel.loadImage(image);

        imageView = new ImageView();
        imageView.showImage(imageModel);

        image.getCanvas().addMouseListener(imageMouseClicked());
        image.getCanvas().addKeyListener(imageKeyPressed());

        ImageModel.adaptImageScale(image);

        mainPanelController.setLastFrame(imageModel.getStackSize());
        backgroundSubtractionController.setTfBackground(imageModel.getBackground());
        backgroundSubtractionController.setTfBackground2(imageModel.getBackground2());
    }

    /**
     * Performs the correlation and fitting for a pixel at the given coordinates.
     *
     * @param x The x-coordinate of the pixel.
     * @param y The y-coordinate of the pixel.
     */
    private void correlatePixel(int x, int y) {
        SelectedPixel selectedPixel = new SelectedPixel(imageModel, correlator, settings);
        try {
            Point[] cursorPositions = selectedPixel.performCorrelationFunctionEvaluation(x, y);
            Point pixel = cursorPositions[0];
            fitController.fit(correlator.getPixelModel(pixel.x, pixel.y), correlator.getLagTimes(),
                    correlator.getRegularizedCovarianceMatrix(), x, y);

            plotResuts(cursorPositions);
        } catch (RuntimeException e) {
            IJ.showMessage("Error", e.getMessage());
        }
    }

    /**
     * Plots the results for a pixel at the given coordinates.
     *
     * @param cursorPositions The array of cursor positions where the two first elements represent the pixel
     *                        coordinates.
     */
    private void plotResuts(Point[] cursorPositions) {
        Point p = cursorPositions[0];
        PixelModel pixelModel = correlator.getPixelModel(p.x, p.y);

        if (options.isPlotACFCurves()) {
            Plots.plotCorrelationFunction(pixelModel, correlator.getLagTimes(), cursorPositions,
                    settings.getBinning(), fitController.getFitStart(), fitController.getFitEnd());
        }

        if (options.isPlotSDCurves()) {
            Plots.plotStandardDeviation(pixelModel.getStandardDeviationAcf(), correlator.getLagTimes(), p);
        }

        if (options.isPlotIntensityCurves()) {
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
            pixelModel.setMSD(MeanSquareDisplacement.correlationToMSD(pixelModel.getAcf(), settings.getParamAx(),
                    settings.getParamAy(), settings.getParamW(), settings.getSigmaZ(), settings.isMSD3d()));
            Plots.plotMSD(pixelModel.getMSD(), correlator.getLagTimes(), p);
        }

        if (options.isPlotResCurves() && pixelModel.isFitted()) {
            Plots.plotResiduals(pixelModel.getResiduals(), correlator.getLagTimes(), p);
        }

        if (pixelModel.isFitted()) {
            ImagePlus imgParams =
                    Plots.plotParameterMaps(pixelModel, p, imageModel.getDimension(), settings.getBinning());

            if (options.isPlotParaHist()) {
                Plots.plotHistogramWindow(imgParams);
            }
        }
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

                correlatePixel(x, y);
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

                        // Here we multiply by the binning again to make sure that X and Y are factor of the binning
                        // It is only useful if the binning was changed by the user in the meantime
                        previousX = x * settings.getBinning().x;
                        previousY = y * settings.getBinning().y;
                    }

                    correlatePixel(x, y);
                }
            }
        };
    }
}
