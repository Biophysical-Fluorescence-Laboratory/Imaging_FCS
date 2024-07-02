package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.BackgroundSubtractionView;
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
    private final BackgroundSubtractionView view;

    /**
     * Constructs a BackgroundSubtractionController with the specified ImageModel.
     * Initializes the view associated with this controller.
     *
     * @param imageModel The ImageModel to be controlled.
     */
    public BackgroundSubtractionController(ImageModel imageModel) {
        this.imageModel = imageModel;
        view = new BackgroundSubtractionView(this, imageModel);
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
    public ActionListener cbBackgroundSubtractionMethodChanged() {
        return (ActionEvent ev) -> {
            String method = ControllerUtils.getComboBoxSelectionFromEvent(ev);
            view.setEnableBackgroundTextField(false);
            imageModel.resetBackgroundImage();
            setTfBackground(imageModel.getBackground());
            setTfBackground2(imageModel.getBackground2());

            switch (method) {
                case "Constant Background":
                    view.setEnableBackgroundTextField(true);
                    break;
                case "Min frame by frame":
                    break;
                case "Min per image stack":
                    break;
                case "Min Pixel wise per image stack":
                    break;
                case "Load BGR image":
                    // Only allows background subtraction before performing bleach correction
                    view.unselectSubtractionAfterBleachCorrection();

                    // Try to load image, revert to other background subtraction method if no background file is loaded
                    boolean loaded = false;
                    try {
                        loaded = imageModel.loadBackgroundImage(IJ.openImage());
                        setTfBackground(imageModel.getBackground());
                        setTfBackground2(imageModel.getBackground2());
                    } catch (RuntimeException e) {
                        IJ.showMessage(e.getMessage());
                    }

                    if (loaded) {
                        view.updateStatusOnImageLoad(true);
                    } else {
                        ((JComboBox<?>) ev.getSource()).setSelectedIndex(0);
                        view.setEnableBackgroundTextField(true);
                        view.updateStatusOnImageLoad(false);
                    }
                    break;
            }
        };
    }

    public void setTfBackground(int background) {
        view.setTfBackground(String.valueOf(background));
    }

    public void setTfBackground2(int background2) {
        view.setTfBackground2(String.valueOf(background2));
    }
}
