package fiji.plugin.imaging_fcs.imfcs.controller;

import fiji.plugin.imaging_fcs.imfcs.enums.FitFunctions;
import fiji.plugin.imaging_fcs.imfcs.model.*;
import fiji.plugin.imaging_fcs.imfcs.model.correlations.Correlator;
import fiji.plugin.imaging_fcs.imfcs.model.correlations.SelectedPixel;
import fiji.plugin.imaging_fcs.imfcs.utils.ImageLoader;
import fiji.plugin.imaging_fcs.imfcs.view.FilteringView;
import fiji.plugin.imaging_fcs.imfcs.view.Plots;
import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

/**
 * Controls filtering operations, connecting the view, models, and other controllers
 * for fluorescence correlation spectroscopy (FCS) processing within the Fiji plugin.
 */
public class FilteringController {
    private final FilteringView view;
    private final ExpSettingsModel settings;
    private final OptionsModel optionsModel;
    private final ImageController imageController;
    private final FitController fitController;
    private final Correlator correlator;

    /**
     * Initializes the FilteringController with the provided models and controllers.
     *
     * @param settings        Experiment settings model.
     * @param optionsModel    Filtering options model.
     * @param imageController Image-related operations controller.
     * @param fitController   Data fitting controller.
     * @param fitModel        Data fitting model.
     * @param correlator      Correlation data processor.
     */
    public FilteringController(ExpSettingsModel settings, OptionsModel optionsModel, ImageController imageController,
                               FitController fitController, FitModel fitModel, Correlator correlator) {
        this.settings = settings;
        this.optionsModel = optionsModel;
        this.imageController = imageController;
        this.fitController = fitController;
        this.correlator = correlator;

        this.view = new FilteringView(this, fitModel);
    }

    /**
     * Filters pixel data and updates the plots based on the current settings and fit criteria.
     */
    private void filterAndPlot() {
        Plots.updateParameterMaps(correlator.getPixelModels(),
                settings.getConvertedImageDimension(imageController.getImageDimension()),
                settings::convertPointToBinning, imageController.imageParamClicked(), fitController,
                optionsModel.isPlotParaHist(), settings.isFCCSDisp());

        List<PixelModel> pixelModelList =
                SelectedPixel.getPixelModelsInRoi(null, settings.getPixelBinning(), settings.getMinCursorPosition(),
                        settings::convertPointToBinning, correlator.getPixelModels(), fitController);

        imageController.plotMultiplePixelsModels(pixelModelList);
    }

    /**
     * Loads a binary filtering image to be applied to parameter maps.
     * Ensures the loaded image matches the dimensions of the parameter maps.
     *
     * @param width  The width of the parameter map.
     * @param height The height of the parameter map.
     * @return The loaded binary image.
     * @throws IllegalArgumentException if the filtering image does not match the size of the parameter map.
     */
    private ImagePlus loadBinaryFilteringImage(int width, int height) {
        ImagePlus filteringImg = ImageLoader.openImagePlusWithDialog();
        if (filteringImg != null && (filteringImg.getWidth() != width || filteringImg.getHeight() != height)) {
            throw new IllegalArgumentException("Filtering image must be the same size as the parameter map.");
        }
        FilteringModel.setFilteringBinaryImage(filteringImg);
        return filteringImg;
    }

    /**
     * Enables or disables the "Same as CCF" button and sets the ACFsSameAsCCF state.
     *
     * @param b True to enable the button, false to disable it.
     */
    public void enableButtonSameAsCCF(boolean b) {
        if (!b) {
            // unselect the option if it was set when we deactivate the button.
            FilteringModel.setAcfsSameAsCCF(false);
        }
        view.enableButtonSameAsCCF(b);
    }

    /**
     * Refreshes the filtering-related UI components in the view.
     */
    public void refreshFilteringView() {
        SwingUtilities.invokeLater(view::refreshFields);
    }

    /**
     * Creates an ActionListener for the filtering button, updating parameter maps
     * or showing an error if unavailable.
     *
     * @return ActionListener for filtering button.
     */
    public ActionListener btnFilteringPressed() {
        return (ActionEvent) -> {
            if (Plots.imgParam != null && Plots.imgParam.isVisible()) {
                filterAndPlot();
            } else {
                IJ.showMessage("No parameter map available. Perform a correlation or load an experiment.");
            }
        };
    }

    /**
     * Creates an ActionListener for the reset button, resetting filters and clearing fields.
     *
     * @return ActionListener for reset button.
     */
    public ActionListener btnResetPressed() {
        return (ActionEvent) -> {
            fitController.resetFilters();
            refreshFilteringView();

            if (Plots.imgParam != null && Plots.imgParam.isVisible()) {
                filterAndPlot();
            }
        };
    }

    /**
     * Creates an ActionListener for loading a binary filter image, updating the button label upon success.
     *
     * @return An ActionListener for the load binary filter button.
     */
    public ActionListener btnLoadBinaryFilterPressed() {
        return (ActionEvent ev) -> {
            if (Plots.imgParam != null && Plots.imgParam.isVisible()) {
                JButton button = (JButton) ev.getSource();
                ImagePlus filterImage = null;
                try {
                    filterImage = loadBinaryFilteringImage(Plots.imgParam.getWidth(), Plots.imgParam.getHeight());
                } catch (IllegalArgumentException e) {
                    IJ.showMessage("Error", e.getMessage());
                }

                if (filterImage != null) {
                    button.setText("Loaded");

                    filterAndPlot();
                } else {
                    button.setText("Binary");
                }
            } else {
                IJ.showMessage("No parameter map available. Perform a correlation or load an experiment.");
            }
        };
    }

    /**
     * Creates an ActionListener for enabling or disabling a threshold, updating
     * the active state of the associated filter field and refreshing the UI.
     *
     * @param filterFields The filter fields associated with the threshold.
     * @return ActionListener for enabling or disabling a threshold.
     */
    public ActionListener enabledThresholdPressed(FilteringView.FilterFields filterFields) {
        return (ActionEvent ev) -> {
            JRadioButton radioButton = (JRadioButton) ev.getSource();

            boolean acfsEnabled = settings.getFitModel() == FitFunctions.DC_FCCS_2D;

            FilteringModel threshold = filterFields.getThreshold();
            threshold.setActive(radioButton.isSelected(), acfsEnabled);

            filterFields.refreshEnabled();
        };
    }

    /**
     * Creates an ActionListener for toggling the "Same as CCF" setting,
     * updating the ACF thresholds accordingly and refreshing the view.
     *
     * @return ActionListener for toggling the "Same as CCF" button.
     */
    public ActionListener sameAsCCFPressed() {
        return (ActionEvent ev) -> {
            JRadioButton radioButton = (JRadioButton) ev.getSource();
            FilteringModel.setAcfsSameAsCCF(radioButton.isSelected());
            // Deactivate or activate the acfs thresholds depending on the value of the button
            fitController.setAllAcfsThreshold(!radioButton.isSelected());
            view.refreshFields();
        };
    }

    /**
     * Sets the visibility of the filtering view.
     *
     * @param b True to make the view visible, false to hide.
     */
    public void setVisible(boolean b) {
        view.setVisible(b);
    }

    /**
     * Disposes of the filtering view.
     */
    public void dispose() {
        view.dispose();
    }

    /**
     * Brings the filtering view to the front.
     */
    public void toFront() {
        view.toFront();
    }
}
