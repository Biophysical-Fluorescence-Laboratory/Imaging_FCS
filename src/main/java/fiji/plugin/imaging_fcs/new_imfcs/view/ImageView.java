package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.model.ImageModel;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

import java.awt.*;

/**
 * The ImageView class is responsible for displaying images in a window with custom settings.
 * It provides functionality to adjust the zoom level and window position based on predefined constants.
 */
public class ImageView {
    // Constants defining the zoom factor and the initial position of the image window
    private static final Point IMAGE_POSITION = new Point(
            Constants.MAIN_PANEL_POS.x + Constants.MAIN_PANEL_DIM.width + 10,
            Constants.MAIN_PANEL_POS.y);

    /**
     * Default constructor for ImageView.
     */
    public ImageView() {
    }

    /**
     * Displays an image based on the provided ImageModel instance.
     * Adjusts position of the image window according to predefined settings.
     *
     * @param imageModel The ImageModel object representing the image to be displayed.
     */
    public void showImage(ImageModel imageModel) {
        imageModel.show();

        ImageWindow window = imageModel.getWindow();
        window.setLocation(IMAGE_POSITION);

        ImageCanvas canvas = imageModel.getCanvas();
        canvas.setFocusable(true);
    }
}
