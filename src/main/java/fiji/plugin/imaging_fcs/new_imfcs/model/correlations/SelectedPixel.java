package fiji.plugin.imaging_fcs.new_imfcs.model.correlations;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.ImageModel;
import ij.gui.Overlay;
import ij.gui.Roi;

import java.awt.*;

/**
 * The SelectedPixel class handles the operations related to pixel selection and correlation for image processing.
 */
public class SelectedPixel {
    private final ImageModel imageModel;
    private final Correlator correlator;
    private final ExpSettingsModel settings;

    /**
     * Constructs a SelectedPixel instance with the given models and settings.
     *
     * @param imageModel the image model
     * @param correlator the correlator
     * @param settings   the experimental settings model
     */
    public SelectedPixel(ImageModel imageModel, Correlator correlator, ExpSettingsModel settings) {
        this.imageModel = imageModel;
        this.correlator = correlator;
        this.settings = settings;
    }

    /**
     * Performs the correlation function evaluation (CFE) on the selected pixel.
     *
     * @param x the x-coordinate of the pixel
     * @param y the y-coordinate of the pixel
     * @return the position of the pixels after conversion to image space
     */
    public Point[] performCorrelationFunctionEvaluation(int x, int y, boolean singlePixelCorrelation) {
        Point[] cursorPositions = convertToImageSpace(x, y);
        Point cursorPosition1 = cursorPositions[0];
        Point cursorPosition2 = cursorPositions[1];

        if (!isPixelWithinImage(x, y)) {
            throw new RuntimeException("Pixel coordinates are out of the image");
        }

        if (isOverlapInDCFCCS()) {
            throw new RuntimeException("Cross-correlation areas overlap.");
        }

        processPixels(cursorPosition1, cursorPosition2, singlePixelCorrelation);
        return cursorPositions;
    }

    /**
     * Converts the given pixel coordinates to image space coordinates.
     *
     * @param x the x-coordinate of the pixel
     * @param y the y-coordinate of the pixel
     * @return an array containing the converted coordinates
     */
    private Point[] convertToImageSpace(int x, int y) {
        Point pixelBinning = settings.getPixelBinning();

        Point cursorPosition1 = new Point(x * pixelBinning.x, y * pixelBinning.y);
        Point cursorPosition2 =
                new Point(x * pixelBinning.x + settings.getCCF().width, y * pixelBinning.y + settings.getCCF().height);

        return new Point[]{cursorPosition1, cursorPosition2};
    }

    /**
     * Checks if the given pixel coordinates are within the image boundaries.
     *
     * @param x the x-coordinate of the pixel
     * @param y the y-coordinate of the pixel
     * @return true if the pixel is within the image, false otherwise
     */
    private boolean isPixelWithinImage(int x, int y) {
        Point minCursorPosition = settings.getMinCursorPosition();
        Point maxCursorPosition = settings.getMaxCursorPosition(imageModel.getImage());

        return x >= minCursorPosition.x && x <= maxCursorPosition.x && y >= minCursorPosition.y &&
                y <= maxCursorPosition.y;
    }

    /**
     * Checks if there is an overlap in DC-FCCS (Dual-Color Fluorescence Cross-Correlation Spectroscopy) areas.
     *
     * @return true if there is an overlap, false otherwise
     */
    private boolean isOverlapInDCFCCS() {
        return settings.getFitModel().equals(Constants.DC_FCCS_2D) &&
                Math.abs(settings.getCCF().width) < settings.getBinning().x &&
                Math.abs(settings.getCCF().height) < settings.getBinning().y;
    }

    /**
     * Processes the pixels at the given cursor positions.
     *
     * @param cursorPosition1 the first cursor position
     * @param cursorPosition2 the second cursor position
     */
    private void processPixels(Point cursorPosition1, Point cursorPosition2, boolean singlePixelCorrelation) {
        if (singlePixelCorrelation) {
            setupROIs(cursorPosition1, cursorPosition2);
        }

        if (settings.getFitModel().equals(Constants.ITIR_FCS_2D) ||
                settings.getFitModel().equals(Constants.SPIM_FCS_3D)) {
            // TODO: in correlate we need to divide by pixelBinning to set the auto-correlation function
            correlator.correlate(imageModel.getImage(), cursorPosition1.x, cursorPosition1.y, cursorPosition2.x,
                    cursorPosition2.y, settings.getFirstFrame(), settings.getLastFrame());
        }
    }

    /**
     * Sets up the Regions of Interest (ROIs) at the given cursor positions.
     *
     * @param cursorPosition1 the first cursor position
     * @param cursorPosition2 the second cursor position
     */
    private void setupROIs(Point cursorPosition1, Point cursorPosition2) {
        if (imageModel.getOverlay() != null) {
            imageModel.getOverlay().clear();
        }

        Roi roi1 = new Roi(cursorPosition1.x, cursorPosition1.y, settings.getBinning().x, settings.getBinning().y);
        roi1.setStrokeColor(Color.BLUE);
        imageModel.setRoi(roi1);

        if (settings.getCCF().width != 0 || settings.getCCF().height != 0 ||
                settings.getFitModel().equals(Constants.DC_FCCS_2D)) {
            Roi roi2 = new Roi(cursorPosition2.x, cursorPosition2.y, settings.getBinning().x, settings.getBinning().y);
            roi2.setStrokeColor(Color.RED);
            imageModel.setOverlay(new Overlay(roi2));
        }
    }
}
