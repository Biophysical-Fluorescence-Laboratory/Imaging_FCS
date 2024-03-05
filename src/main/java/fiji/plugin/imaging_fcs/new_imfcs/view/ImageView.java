package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.model.ImageModel;
import ij.IJ;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

import java.awt.*;
import java.awt.event.KeyListener;
import java.awt.event.MouseListener;

/**
 * The ImageView class is responsible for displaying images in a window with custom settings.
 * It provides functionality to adjust the zoom level and window position based on predefined constants.
 */
public class ImageView {
    // Constants defining the zoom factor and the initial position of the image window
    private static final double ZOOM_FACTOR = 250;
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
     * Adjusts the zoom and position of the image window according to predefined settings.
     *
     * @param imageModel The ImageModel object representing the image to be displayed.
     */
    public void showImage(ImageModel imageModel, MouseListener mouseListener, KeyListener keyListener) {
        imageModel.show();
        ImageWindow window = imageModel.getWindow();
        window.setLocation(IMAGE_POSITION);

        ImageCanvas canvas = imageModel.getCanvas();
        canvas.setFocusable(true);
        // add listeners
        canvas.addMouseListener(mouseListener);
        canvas.addKeyListener(keyListener);

        int width = imageModel.getWidth();
        int height = imageModel.getHeight();

        double scimp;
        if (width >= height) {
            scimp = ZOOM_FACTOR / width;
        } else {
            scimp = ZOOM_FACTOR / height;
        }

        if (scimp < 1.0) {
            scimp = 1.0;
        }
        // Convert scale factor to percentage for the ImageJ command
        scimp *= 100;

        IJ.run(imageModel.getImage(), "Original Scale", "");
        String options = String.format("zoom=%f x=%d y=%d", scimp, width / 2, height / 2);
        IJ.run(imageModel.getImage(), "Set... ", options);
        // This workaround addresses a potential bug in ImageJ versions 1.48v and later
        IJ.run("In [+]", "");
    }
}
