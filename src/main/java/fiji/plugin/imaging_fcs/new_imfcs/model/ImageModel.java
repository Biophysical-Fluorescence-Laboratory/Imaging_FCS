package fiji.plugin.imaging_fcs.new_imfcs.model;

import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;

/**
 * The ImageModel class encapsulates data and operations for an image.
 */
public class ImageModel {
    public ImagePlus image;
    private String imagePath;
    private String fileName;
    private boolean isSimulation;

    /**
     * Constructs an ImageModel instance with no image loaded.
     */
    public ImageModel() {
        this.image = null;
    }

    /**
     * Loads an image into the model, checking its type and extracting its path and file name.
     * Only images of type GRAY16 are supported.
     *
     * @param image The ImagePlus object to load into the model.
     * @throws RuntimeException if the image is not of type GRAY16.
     */
    public void loadImage(ImagePlus image) {
        checkImage(image);

        this.image = image;
        getImagePath();

        this.isSimulation = false;
    }

    /**
     * Validates that the provided ImagePlus object is of the correct type (GRAY16).
     *
     * @param image The ImagePlus object to check.
     * @throws RuntimeException if the image type is not GRAY16.
     */
    private void checkImage(ImagePlus image) {
        if (image.getType() != ImagePlus.GRAY16) {
            throw new RuntimeException("Only GRAY16 Tiff stacks supported");
        }
    }

    /**
     * Extracts and stores the path and file name from the ImagePlus object's FileInfo.
     */
    private void getImagePath() {
        imagePath = image.getOriginalFileInfo().directory;
        fileName = image.getOriginalFileInfo().fileName;
    }

    /**
     * Displays the image in a new window.
     */
    public void show() {
        image.show();
    }

    // List of getters
    public ImageWindow getWindow() {
        return image.getWindow();
    }

    public ImageCanvas getCanvas() {
        return image.getCanvas();
    }

    public int getHeight() {
        return image.getHeight();
    }

    public int getWidth() {
        return image.getWidth();
    }

    public ImagePlus getImage() {
        return image;
    }
}
