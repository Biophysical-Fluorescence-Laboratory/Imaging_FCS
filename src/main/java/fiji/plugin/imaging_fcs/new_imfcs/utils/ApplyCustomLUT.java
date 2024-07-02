package fiji.plugin.imaging_fcs.new_imfcs.utils;

import ij.ImagePlus;
import ij.plugin.LutLoader;
import ij.process.ImageProcessor;
import ij.process.LUT;

import java.net.URL;

/**
 * A utility class for applying custom Look-Up Tables (LUTs) to images in ImageJ.
 */
public final class ApplyCustomLUT {
    // Private constructor to prevent instantiation
    private ApplyCustomLUT() {
    }

    /**
     * Applies a custom LUT to the given image.
     *
     * @param img      the ImagePlus object representing the image to which the LUT will be applied
     * @param lutColor the name of the LUT file (without the ".lut" extension) to be applied
     * @throws RuntimeException if the image is null, if the LUT folder is not found, or if the LUT fails to load
     */
    public static void applyCustomLUT(ImagePlus img, String lutColor) {
        if (img == null) {
            throw new RuntimeException("No image open.");
        }

        // Load the custom LUT from the classpath
        URL lutUrl = ApplyCustomLUT.class.getResource("/luts/");

        if (lutUrl == null) {
            throw new RuntimeException("LUTS folder not found");
        }

        // Load the custom LUT using the specified lutColor
        LUT lut = LutLoader.openLut(lutUrl.getPath() + lutColor + ".lut");

        if (lut == null) {
            throw new RuntimeException("Failed to load LUT:" + lutColor);
        }

        // Apply the LUT to the image processor
        ImageProcessor ip = img.getProcessor();
        ip.setLut(lut);

        // Update the image to reflect the LUT change
        img.updateAndDraw();
    }
}
