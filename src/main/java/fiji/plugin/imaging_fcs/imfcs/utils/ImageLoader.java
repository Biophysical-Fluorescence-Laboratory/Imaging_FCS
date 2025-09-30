package fiji.plugin.imaging_fcs.imfcs.utils;

import ij.ImagePlus;
import ij.io.Opener;

import javax.swing.*;
import java.io.File;

/**
 * Utility class to load ImagePlus files
 */
public class ImageLoader {

    // Static attribute to store the last directory visited
    private static String lastDirectory = System.getProperty("user.home");

    // private constructor to prevent instantiation
    private ImageLoader() {
    }

    /**
     * Shows a file-open dialog starting from the last directory (if any),
     * then opens the selected image and returns the ImagePlus.
     *
     * @return ImagePlus object if a file is successfully chosen and opened;
     * null if cancelled or failed to open.
     */
    public static ImagePlus openImagePlusWithDialog() {
        // Create a Swing file chooser, starting in lastDirectory
        JFileChooser fileChooser = new JFileChooser(lastDirectory);
        // Only allow single-file selection
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null && file.isFile()) {
                // Update lastDirectory so next time we start from here
                lastDirectory = file.getParent();

                // Return the opened ImagePlus
                return new Opener().openImage(file.getAbsolutePath());
            }
        }
        // If the user cancels or no valid file is chosen, return null
        return null;
    }
}
