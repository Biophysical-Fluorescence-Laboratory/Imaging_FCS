package fiji.plugin.imaging_fcs.new_imfcs.model;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.ImageCanvas;
import ij.gui.ImageWindow;
import ij.gui.Overlay;
import ij.gui.Roi;
import ij.process.ImageProcessor;

import java.awt.*;
import java.util.EventListener;
import java.util.function.Consumer;

/**
 * The ImageModel class encapsulates data and operations for an image.
 */
public final class ImageModel {
    private static final double ZOOM_FACTOR = 250;
    private ImagePlus image;
    private ImagePlus backgroundImage;
    private double[][] backgroundMean, backgroundVariance, backgroundCovariance;

    private int background = 0;
    private int background2 = 0;
    private int minBackgroundValue = 0;

    /**
     * Constructs an ImageModel instance with no image loaded.
     */
    public ImageModel() {
        this.image = null;
        this.backgroundImage = null;
    }

    /**
     * Adjusts the scale of the provided ImagePlus object.
     * The image is scaled to fit within a specified zoom factor while maintaining its aspect ratio.
     *
     * @param image The ImagePlus object to be scaled.
     */
    public static void adaptImageScale(ImagePlus image) {
        double scimp;
        if (image.getWidth() >= image.getHeight()) {
            scimp = ZOOM_FACTOR / image.getWidth();
        } else {
            scimp = ZOOM_FACTOR / image.getHeight();
        }

        if (scimp < 1.0) {
            scimp = 1.0;
        }
        // Convert scale factor to percentage for the ImageJ command
        scimp *= 100;

        IJ.run(image, "Original Scale", "");
        String options = String.format("zoom=%f x=%d y=%d", scimp, image.getWidth() / 2, image.getHeight() / 2);
        IJ.run(image, "Set... ", options);
        // This workaround addresses a potential bug in ImageJ versions 1.48v and later
        IJ.run(image, "In [+]", "");
    }

    /**
     * Checks if an image is currently loaded in the model.
     *
     * @return True if an image is loaded, false otherwise.
     */
    public boolean isImageLoaded() {
        return image != null && image.getWindow() != null;
    }

    /**
     * Checks if a background image is currently loaded in the model.
     *
     * @return True if a background image is loaded, false otherwise.
     */
    public boolean isBackgroundLoaded() {
        return backgroundImage != null;
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
     * Checks if two ImagePlus objects have different sizes.
     *
     * @param img1 The first ImagePlus object.
     * @param img2 The second ImagePlus object.
     * @return True if the images are not the same size, false otherwise.
     */
    private boolean areNotSameSize(ImagePlus img1, ImagePlus img2) {
        return img1.getWidth() != img2.getWidth() || img1.getHeight() != img2.getHeight() ||
                img1.getStackSize() != img2.getStackSize();
    }

    /**
     * Loads an image into the model, checking its type and extracting its path and file name.
     * Only images of type GRAY16 are supported.
     *
     * @param image The ImagePlus object to load into the model.
     * @throws RuntimeException if the image is not of type GRAY16 or if the size doesn't match with the background.
     */
    public void loadImage(ImagePlus image) {
        checkImage(image);

        minBackgroundValue = 0;

        // if a background image is loaded, check that there are the same format
        if (backgroundImage != null && areNotSameSize(image, backgroundImage)) {
            throw new RuntimeException("Image is not the same size as the background image");
        } else {
            // calculate the minimum background value, this will be used if no background is loaded
            minBackgroundValue = minDetermination(image);
        }

        if (backgroundImage == null) {
            background = minBackgroundValue;
            background2 = minBackgroundValue;
        } else {
            background = 0;
            background2 = 0;
        }

        // If an image is already loaded, unload it
        if (isImageLoaded()) {
            unloadImage();
        }

        this.image = image;
    }

    /**
     * Unloads the current image from the model.
     */
    private void unloadImage() {
        if (image.getOverlay() != null) {
            image.deleteRoi();
            image.getOverlay().clear();
            image.setOverlay(null);
        }

        ImageCanvas canvas = image.getCanvas();
        removeListeners(canvas.getMouseListeners(), canvas::removeMouseListener);
        removeListeners(canvas.getKeyListeners(), canvas::removeKeyListener);

        image = null;
    }

    /**
     * Removes listeners from the provided array using the specified removal function.
     *
     * @param listeners              The array of listeners to remove.
     * @param removeListenerFunction The function to remove each listener.
     * @param <T>                    The type of listener.
     */
    private <T extends EventListener> void removeListeners(T[] listeners, Consumer<T> removeListenerFunction) {
        for (T listener : listeners) {
            removeListenerFunction.accept(listener);
        }
    }

    /**
     * Determines the minimum pixel value in the provided ImagePlus object.
     *
     * @param img The ImagePlus object to analyze.
     * @return The minimum pixel value in the image.
     */
    private int minDetermination(ImagePlus img) {
        int min = Integer.MAX_VALUE;

        for (int z = 1; z <= img.getStackSize(); z++) {
            final ImageProcessor imageProcessor = img.getStack().getProcessor(z);
            for (int x = 0; x < img.getWidth(); x++) {
                for (int y = 0; y < img.getHeight(); y++) {
                    int pixelValue = imageProcessor.get(x, y);
                    if (pixelValue < min) {
                        min = imageProcessor.get(x, y);
                    }
                }
            }
        }

        return min;
    }

    /**
     * Loads a background image into the model, checking its type and computing statistics.
     *
     * @param backgroundImage The ImagePlus object to load as the background.
     * @return True if the background image was successfully loaded, false otherwise.
     */
    public boolean loadBackgroundImage(ImagePlus backgroundImage) {
        if (backgroundImage == null) {
            IJ.showMessage("Selected file does not exist or it is not an image.");
            return false;
        }

        checkImage(backgroundImage);

        if (isImageLoaded() && areNotSameSize(image, backgroundImage)) {
            throw new RuntimeException("Background image is not the same size as Image. Background image not loaded");
        }

        this.backgroundImage = backgroundImage;

        // Compute Mean, Variance and Covariance of the background
        computeBackgroundStats();

        // set background values to 0 if a background is loaded
        background = 0;
        background2 = 0;
        return true;
    }

    /**
     * Computes the mean, variance, and covariance of the background image.
     */
    private void computeBackgroundStats() {
        int width = backgroundImage.getWidth();
        int height = backgroundImage.getHeight();
        int stackSize = backgroundImage.getStackSize();

        backgroundMean = new double[width][height];

        // This is the mean of the current frame without the last frame
        double[][] meanCurrentNoLastFrame = new double[width][height];
        double[][] meanNextFrame = new double[width][height];

        backgroundCovariance = new double[width][height];
        backgroundVariance = new double[width][height];

        for (int stackIndex = 1; stackIndex <= stackSize; stackIndex++) {
            ImageProcessor ip = backgroundImage.getStack().getProcessor(stackIndex);
            ImageProcessor ipNextFrame =
                    (stackIndex < stackSize) ? backgroundImage.getStack().getProcessor(stackIndex + 1) : null;

            // compute the means, variance and covariance
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    double currentPixelValue = ip.getPixelValue(x, y);

                    // Compute covariance if the next frame exists
                    if (ipNextFrame != null) {
                        double nextPixelValue = ipNextFrame.getPixelValue(x, y);
                        meanCurrentNoLastFrame[x][y] += currentPixelValue;
                        meanNextFrame[x][y] += nextPixelValue;
                        backgroundCovariance[x][y] += currentPixelValue * nextPixelValue;
                    }

                    backgroundMean[x][y] += currentPixelValue;
                    backgroundVariance[x][y] += Math.pow(currentPixelValue, 2.0);
                }
            }
        }

        // Compute final values for mean, variance and covariance
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                backgroundMean[x][y] /= stackSize;
                backgroundVariance[x][y] = backgroundVariance[x][y] / stackSize - Math.pow(backgroundMean[x][y], 2);
                meanNextFrame[x][y] /= (stackSize - 1);
                backgroundCovariance[x][y] = backgroundCovariance[x][y] / (stackSize - 1) -
                        (meanCurrentNoLastFrame[x][y] / (stackSize - 1)) * meanNextFrame[x][y];
            }
        }
    }

    /**
     * Displays the image in a new window.
     */
    public void show() {
        image.show();
    }

    /**
     * Resets the background image, removing it from the model.
     */
    public void resetBackgroundImage() {
        backgroundImage = null;
        background = minBackgroundValue;
        background2 = minBackgroundValue;
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

    public Dimension getDimension() {
        return new Dimension(image.getWidth(), image.getHeight());
    }

    public int getStackSize() {
        return image.getStackSize();
    }

    public Overlay getOverlay() {
        return image.getOverlay();
    }

    public void setOverlay(Overlay overlay) {
        image.setOverlay(overlay);
    }

    public Roi getRoi() {
        return image.getRoi();
    }

    public void setRoi(Roi roi) {
        image.setRoi(roi);
    }

    public ImagePlus getImage() {
        return image;
    }

    public ImagePlus getBackgroundImage() {
        return backgroundImage;
    }

    public double[][] getBackgroundMean() {
        return backgroundMean;
    }

    public double[][] getBackgroundVariance() {
        return backgroundVariance;
    }

    public double[][] getBackgroundCovariance() {
        return backgroundCovariance;
    }

    public int getBackground() {
        return background;
    }

    public void setBackground(String background) {
        this.background = Integer.parseInt(background);
    }

    public int getBackground2() {
        return background2;
    }

    public void setBackground2(String background2) {
        this.background2 = Integer.parseInt(background2);
        System.out.println(this.background2);
    }
}
