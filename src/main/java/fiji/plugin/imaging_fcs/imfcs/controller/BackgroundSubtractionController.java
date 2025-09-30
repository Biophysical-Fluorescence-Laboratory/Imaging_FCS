package fiji.plugin.imaging_fcs.imfcs.controller;

import fiji.plugin.imaging_fcs.imfcs.enums.BackgroundMode;
import fiji.plugin.imaging_fcs.imfcs.model.BackgroundModel;
import fiji.plugin.imaging_fcs.imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.imfcs.utils.ImageLoader;
import fiji.plugin.imaging_fcs.imfcs.view.BackgroundSubtractionView;
import ij.IJ;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Controller for handling background subtraction logic in the ImageModel.
 * It connects the ImageModel and the BackgroundSubtractionView.
 */
public class BackgroundSubtractionController {
    private final ImageModel imageModel;
    private final BackgroundModel backgroundModel;
    private final Runnable resetCallback;
    private final BackgroundSubtractionView view;

    /**
     * Constructs a BackgroundSubtractionController with the specified ImageModel.
     * Initializes the view associated with this controller.
     *
     * @param imageModel The ImageModel to be controlled.
     */
    public BackgroundSubtractionController(ImageModel imageModel, Runnable resetCallback) {
        this.imageModel = imageModel;
        this.backgroundModel = imageModel.getBackgroundModel();
        view = new BackgroundSubtractionView(this, backgroundModel);

        this.resetCallback = resetCallback;
    }

    /**
     * Sets the visibility of the view.
     *
     * @param b true to make the view visible, false to hide it.
     */
    public void setVisible(boolean b) {
        view.setVisible(b);
    }

    /**
     * Creates an ActionListener that handles changes to the background subtraction method.
     * The listener updates the ImageModel and the view based on the selected method.
     *
     * @return An ActionListener for handling background subtraction method changes.
     */
    public ActionListener cbBackgroundSubtractionMethodChanged(JComboBox<BackgroundMode> comboBox) {
        return new ActionListener() {
            private BackgroundMode previousSelection = (BackgroundMode) comboBox.getSelectedItem();

            @Override
            public void actionPerformed(ActionEvent ev) {
                BackgroundMode mode = (BackgroundMode) comboBox.getSelectedItem();

                try {
                    if (!previousSelection.equals(mode)) {
                        resetCallback.run();
                        // Update previous selection to current if successful
                        previousSelection = mode;
                    }
                } catch (RejectResetException e) {
                    comboBox.setSelectedItem(previousSelection);
                    return;
                }

                view.setEnableBackgroundTextField(false);
                backgroundModel.resetBackgroundImage();

                if (mode == BackgroundMode.CONSTANT_BACKGROUND) {
                    view.setEnableBackgroundTextField(true);
                } else if (mode == BackgroundMode.LOAD_BGR_IMAGE) {
                    // Try to load image, revert to other background subtraction method if no background file is
                    // loaded
                    boolean loaded = false;
                    try {
                        loaded = imageModel.loadBackgroundImage(ImageLoader.openImagePlusWithDialog());
                    } catch (RuntimeException e) {
                        IJ.showMessage(e.getMessage());
                    }

                    if (loaded) {
                        view.updateStatusOnImageLoad(true);
                    } else {
                        mode = BackgroundMode.CONSTANT_BACKGROUND;
                        comboBox.setSelectedItem(mode);
                        ((JComboBox<?>) ev.getSource()).setSelectedIndex(0);
                        view.setEnableBackgroundTextField(true);
                        view.updateStatusOnImageLoad(false);
                    }
                }

                backgroundModel.setMode(mode);
                backgroundModel.computeBackground(imageModel.getImage());
                setTfBackgrounds();
            }
        };
    }

    /**
     * Dispose the view
     */
    public void dispose() {
        this.view.dispose();
    }

    /**
     * Bring the view to front
     */
    public void toFront() {
        this.view.toFront();
    }

    public void setTfBackgrounds() {
        view.setTfBackground(String.valueOf(backgroundModel.getConstantBackground1()));
        view.setTfBackground2(String.valueOf(backgroundModel.getConstantBackground2()));
    }
}
