package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.*;
import fiji.plugin.imaging_fcs.new_imfcs.model.correlations.Correlator;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.BleachCorrectionModel;
import fiji.plugin.imaging_fcs.new_imfcs.utils.Pair;
import fiji.plugin.imaging_fcs.new_imfcs.view.BleachCorrectionView;
import fiji.plugin.imaging_fcs.new_imfcs.view.ExpSettingsView;
import fiji.plugin.imaging_fcs.new_imfcs.view.MainPanelView;
import fiji.plugin.imaging_fcs.new_imfcs.view.Plots;
import fiji.plugin.imaging_fcs.new_imfcs.view.dialogs.FilterLimitsSelectionView;
import fiji.plugin.imaging_fcs.new_imfcs.view.dialogs.MSDView;
import fiji.plugin.imaging_fcs.new_imfcs.view.dialogs.PolynomialOrderSelectionView;
import fiji.plugin.imaging_fcs.new_imfcs.view.dialogs.SlidingWindowSelectionView;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import javax.swing.*;
import java.awt.event.*;
import java.util.function.Consumer;

import static fiji.plugin.imaging_fcs.new_imfcs.controller.ControllerUtils.getComboBoxSelectionFromEvent;
import static fiji.plugin.imaging_fcs.new_imfcs.controller.FocusListenerFactory.createFocusListener;

/**
 * Controller class for the main panel of the FCS plugin. It handles user interactions and coordinates
 * the update of models based on user input, as well as the update of views to reflect the current state
 * of the models.
 */
public final class MainPanelController {
    // Field declarations for the views and models that this controller will manage
    private final MainPanelView view;
    private final ExpSettingsView expSettingsView;
    private final BleachCorrectionView bleachCorrectionView;
    private final HardwareModel hardwareModel;
    private final OptionsModel optionsModel;
    private final ImageController imageController;
    private final ExpSettingsModel expSettingsModel;
    private final BleachCorrectionModel bleachCorrectionModel;
    private final SimulationController simulationController;
    private final BackgroundSubtractionController backgroundSubtractionController;
    private final NBController nbController;
    private final FitController fitController;
    private final Correlator correlator;

    /**
     * Constructor that initializes models, views, and other controllers needed for the main panel.
     *
     * @param hardwareModel The model containing hardware settings for the imaging FCS analysis.
     */
    public MainPanelController(HardwareModel hardwareModel) {
        this.hardwareModel = hardwareModel;
        this.optionsModel = new OptionsModel(hardwareModel.isCuda());

        this.expSettingsModel = new ExpSettingsModel();
        this.expSettingsView = new ExpSettingsView(this, expSettingsModel);
        updateSettingsField();

        FitModel fitModel = new FitModel(expSettingsModel);
        this.fitController = new FitController(fitModel);

        ImageModel imageModel = new ImageModel();
        this.backgroundSubtractionController = new BackgroundSubtractionController(imageModel);
        this.bleachCorrectionModel = new BleachCorrectionModel(expSettingsModel, imageModel);
        this.correlator = new Correlator(expSettingsModel, bleachCorrectionModel, fitModel);
        this.imageController =
                new ImageController(this, imageModel, backgroundSubtractionController, fitController,
                        bleachCorrectionModel, correlator, expSettingsModel, optionsModel);

        this.bleachCorrectionView = new BleachCorrectionView(this, bleachCorrectionModel);

        this.simulationController = new SimulationController(imageController, expSettingsModel);

        this.nbController = new NBController(imageModel, expSettingsModel, optionsModel, bleachCorrectionModel);


        this.view = new MainPanelView(this, this.expSettingsModel);
    }

    /**
     * Sets the last frame for the analysis and updates related fields.
     *
     * @param lastFrame The last frame number to be set.
     */
    public void setLastFrame(int lastFrame) {
        view.setTfLastFrame(String.valueOf(lastFrame));
        expSettingsModel.setLastFrame(String.valueOf(lastFrame));
        expSettingsModel.setSlidingWindowLength(lastFrame / 20);
        updateStrideParamFields();
    }

    /**
     * Handles different bleach correction modes and initializes appropriate views for further user inputs
     * depending on the selected mode.
     *
     * @return ActionListener to handle bleach correction mode changes.
     */
    public ActionListener cbBleachCorChanged() {
        return (ActionEvent ev) -> {
            String bleachCorrectionMode = getComboBoxSelectionFromEvent(ev);
            expSettingsModel.setBleachCorrection(bleachCorrectionMode);
            fitController.updateFitEnd(expSettingsModel);

            if ("Sliding Window".equals(bleachCorrectionMode) || "Lin Segment".equals(bleachCorrectionMode)) {
                if (!imageController.isImageLoaded()) {
                    IJ.showMessage("No image open. Please open an image first.");
                    // reset the combo box to default in no image is loaded.
                    ((JComboBox<?>) ev.getSource()).setSelectedIndex(0);
                } else {
                    new SlidingWindowSelectionView(this::onBleachCorrectionSlidingWindowAccepted,
                            expSettingsModel.getSlidingWindowLength());
                }
            } else if ("Polynomial".equals(bleachCorrectionMode)) {
                new PolynomialOrderSelectionView(this::onBleachCorrectionOrderAccepted,
                        bleachCorrectionModel.getPolynomialOrder());
            }
        };
    }

    /**
     * Callback method for accepting sliding window length.
     * It validates the input and sets it in the model or prompts re-entry if the input is invalid.
     *
     * @param slidingWindowLength The length of the sliding window chosen by the user.
     */
    private void onBleachCorrectionSlidingWindowAccepted(int slidingWindowLength) {
        int maxWindowSize = expSettingsModel.getLastFrame() - expSettingsModel.getFirstFrame();

        if (slidingWindowLength <= 0 || slidingWindowLength > maxWindowSize) {
            IJ.showMessage(String.format("Invalid sliding window size. It must be inside 0 < order < %d",
                    maxWindowSize));
            new SlidingWindowSelectionView(this::onBleachCorrectionSlidingWindowAccepted,
                    expSettingsModel.getSlidingWindowLength());
        } else {
            expSettingsModel.setSlidingWindowLength(slidingWindowLength);
            fitController.updateFitEnd(expSettingsModel);
        }
    }

    /**
     * Callback method for accepting polynomial order.
     * It validates the polynomial order and sets it in the model or prompts re-entry if the input is invalid.
     *
     * @param order The polynomial order chosen by the user.
     */
    private void onBleachCorrectionOrderAccepted(int order) {
        if (order <= 0 || order > BleachCorrectionModel.MAX_POLYNOMIAL_ORDER) {
            IJ.showMessage(String.format("Invalid polynomial order. It must be inside 0 < order <= %d",
                    BleachCorrectionModel.MAX_POLYNOMIAL_ORDER));
            new PolynomialOrderSelectionView(this::onBleachCorrectionOrderAccepted,
                    bleachCorrectionModel.getPolynomialOrder());
        } else {
            bleachCorrectionModel.setPolynomialOrder(order);
        }
    }

    /**
     * Returns an action listener for changes in the filter selection combo box.
     * This method creates a listener that updates the filter settings and may
     * open a dialog for setting filter limits based on the selected filter mode.
     *
     * @return an ActionListener that processes filter selection changes
     */
    public ActionListener cbFilterChanged() {
        return (ActionEvent ev) -> {
            String filterMode = getComboBoxSelectionFromEvent(ev);
            expSettingsModel.setFilter(filterMode);

            // If a filter mode other than "none" is selected, show the filter limits dialog.
            if (!filterMode.equals("none")) {
                new FilterLimitsSelectionView(this::onFilterSelectionAccepted, expSettingsModel.getFilterLowerLimit()
                        , expSettingsModel.getFilterUpperLimit());
            }
        };
    }

    /**
     * Creates an ActionListener that updates the fit model selection in the experimental settings model.
     * This listener is triggered when the fit model combo box selection changes.
     *
     * @return an ActionListener that processes the action event
     */
    public ActionListener cbFitModelChanged() {
        return (ActionEvent ev) -> {
            String fitModel = getComboBoxSelectionFromEvent(ev);
            expSettingsModel.setFitModel(fitModel);
            updateSettingsField();
        };
    }

    /**
     * Callback method for accepting filter limits.
     * It validates the filter limits and sets them in the model or prompts re-entry if the input is invalid.
     *
     * @param lowerLimit The lower limit of the filter chosen by the user.
     * @param upperLimit The upper limit of the filter chosen by the user.
     */
    private void onFilterSelectionAccepted(int lowerLimit, int upperLimit) {
        if (lowerLimit < 0 || lowerLimit > upperLimit) {
            IJ.showMessage("Illegal filter limits");
            new FilterLimitsSelectionView(this::onFilterSelectionAccepted, expSettingsModel.getFilterLowerLimit(),
                    expSettingsModel.getFilterUpperLimit());
        } else {
            expSettingsModel.setFilterLowerLimit(lowerLimit);
            expSettingsModel.setFilterUpperLimit(upperLimit);
        }
    }

    /**
     * Returns an action listener to handle the exit button press event.
     * This listener disposes of the main panel view.
     *
     * @return an ActionListener that processes the exit button press event
     */
    public ActionListener btnExitPressed() {
        return (ActionEvent ev) -> view.dispose();
    }

    public ActionListener btnBtfPressed() {
        // TODO: FIXME
        return null;
    }

    /**
     * Returns an action listener to handle the exit button press event.
     * This listener disposes of the main panel view.
     *
     * @return an ActionListener that processes the exit button press event
     */
    public ActionListener btnUseExistingPressed() {
        return (ActionEvent ev) -> {
            if (WindowManager.getImageCount() > 0) {
                try {
                    imageController.loadImage(IJ.getImage());
                } catch (RuntimeException e) {
                    IJ.showMessage("Wrong image format", e.getMessage());
                }
            } else {
                IJ.showMessage("No image open.");
            }
        };
    }

    /**
     * Returns an action listener to handle the exit button press event.
     * This listener disposes of the main panel view.
     *
     * @return an ActionListener that processes the exit button press event
     */
    public ActionListener btnLoadNewPressed() {
        return (ActionEvent ev) -> {
            ImagePlus image = IJ.openImage();
            if (image != null) {
                try {
                    imageController.loadImage(image);
                } catch (RuntimeException e) {
                    IJ.showMessage("Wrong image format", e.getMessage());
                }
            }
        };
    }

    public ActionListener btnSavePressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnLoadPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnBatchPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnPSFPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbDLPressed() {
        // TODO: FIXME
        return null;
    }

    /**
     * Returns an action listener to handle the "Options" button press event.
     * This listener opens the options controller.
     *
     * @return an ActionListener that processes the "Options" button press event
     */
    public ActionListener btnOptionsPressed() {
        return (ActionEvent ev) -> new OptionsController(optionsModel);
    }

    /**
     * Returns an action listener to handle the "Options" button press event.
     * This listener opens the options controller.
     *
     * @return an ActionListener that processes the "Options" button press event
     */
    public ItemListener tbNBPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setText(selected ? "N&B On" : "N&B Off");
            nbController.setVisible(selected);
        };
    }

    public ActionListener tbFilteringPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnAvePressed() {
        // TODO: FIXME
        return null;
    }

    /**
     * Creates an ActionListener for the button press event that generates a scatter plot.
     *
     * @return An ActionListener that handles the button press event.
     */
    public ActionListener btnParaCorPressed() {
        return (ActionEvent ev) -> {
            if (!imageController.isImageLoaded()) {
                IJ.showMessage("No image open.");
            } else if (correlator.getPixelsModel() == null) {
                IJ.showMessage("Nothing to plot, please run the fit on at least one pixel before.");
            } else {
                Pair<double[][], String[]> scatterArrayAndLabels =
                        PixelModel.getScatterPlotArray(correlator.getPixelsModel(), expSettingsModel.getParaCor());
                double[][] scPlot = scatterArrayAndLabels.getLeft();
                String[] labels = scatterArrayAndLabels.getRight();

                Plots.scatterPlot(scPlot, labels[0], labels[1]);
            }
        };
    }

    public ActionListener btnDCCFPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnRTPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnWriteConfigPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnDCRPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnParamVideoPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnDebugPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnROIPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnAllPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbFCCSDisplayPressed() {
        // TODO: FIXME
        return null;
    }

    /**
     * Returns an item listener to handle the "Exp Settings" toggle button press event.
     * This listener toggles the visibility of the experimental settings view.
     *
     * @return an ItemListener that processes the "Exp Settings" toggle button press event
     */
    public ItemListener tbExpSettingsPressed() {
        return (ItemEvent ev) -> expSettingsView.setVisible(ev.getStateChange() == ItemEvent.SELECTED);
    }

    /**
     * Returns an item listener to handle the "Bleach Correction" toggle button press event.
     * This listener toggles the visibility of the bleach correction view, ensuring an image is loaded first.
     *
     * @return an ItemListener that processes the "Bleach Correction" toggle button press event
     */
    public ItemListener tbBleachCorStridePressed() {
        return (ItemEvent ev) -> {
            if (imageController.isImageLoaded()) {
                bleachCorrectionView.setVisible(ev.getStateChange() == ItemEvent.SELECTED);
            } else if (ev.getStateChange() == ItemEvent.SELECTED) {
                // Don't show the message if the button is unselected.
                IJ.showMessage("No image stack loaded.");
                ((JToggleButton) ev.getSource()).setSelected(false);
            }
        };
    }

    /**
     * Returns an item listener to handle the "Fit" toggle button press event.
     * This listener toggles the visibility of the FitController.
     *
     * @return an ItemListener that processes the "Fit" toggle button press event
     */
    public ItemListener tbFitPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setText(selected ? "Fit On" : "Fit Off");
            fitController.setVisible(selected);
        };
    }

    /**
     * Returns an item listener to handle the "Sim" toggle button press event.
     * This listener toggles the visibility of the SimulationController.
     *
     * @return an ItemListener that processes the "Sim" toggle button press event
     */
    public ItemListener tbSimPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setText(selected ? "Sim On" : "Sim Off");
            simulationController.setVisible(selected);
        };
    }

    /**
     * Creates an ItemListener for the MSD toggle button.
     * When the button is pressed, it toggles the MSD (Mean Squared Displacement) analysis on or off
     * and updates the button text accordingly. If MSD is enabled, it opens an MSDView dialog to
     * configure the MSD settings.
     *
     * @return an ItemListener for the MSD toggle button.
     */
    public ItemListener tbMSDPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);

            if (selected) {
                expSettingsModel.setMSD(true);
                button.setText("MSD On");
                new MSDView(expSettingsModel.isMSD3d(), expSettingsModel::setMSD3d);
            } else {
                expSettingsModel.setMSD(false);
                button.setText("MSD Off");
            }
        };
    }

    /**
     * Returns an item listener to handle the "Overlap" toggle button press event.
     * This listener toggles the overlap setting in the experimental settings model.
     *
     * @return an ItemListener that processes the "Overlap" toggle button press event
     */
    public ItemListener tbOverlapPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();
            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);

            button.setText(selected ? "Overlap On" : "Overlap Off");
            expSettingsModel.setOverlap(selected);
        };
    }

    /**
     * Returns an item listener to handle the "Background" toggle button press event.
     * This listener toggles the visibility of the BackgroundSubtractionController.
     *
     * @return an ItemListener that processes the "Background" toggle button press event
     */
    public ItemListener tbBackgroundPressed() {
        return (ItemEvent ev) -> {
            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            backgroundSubtractionController.setVisible(selected);
        };
    }

    /**
     * Updates the display fields in the experimental settings view based on the current values in the model.
     * This method ensures that the UI reflects the most up-to-date settings.
     */
    private void updateSettingsField() {
        Runnable doUpdateSettingsField = () -> {
            expSettingsModel.updateSettings();
            expSettingsView.setNonUserSettings();
        };

        // Execute the update in the Swing event dispatch thread to ensure thread safety
        SwingUtilities.invokeLater(doUpdateSettingsField);
    }

    /**
     * Decorates a setter from the model to automatically update the settings view
     * after the model value has been changed.
     *
     * @param setter A Consumer that sets a value in the model.
     * @return A FocusListener that updates the model and then the view when focus is lost.
     */
    public FocusListener updateSettings(Consumer<String> setter) {
        // decorate the setter to call updateSettingsfield after changing the value
        Consumer<String> decoratedSetter = (String value) -> {
            setter.accept(value);
            updateSettingsField();
        };

        // Create the focus listener with the method from the factory with the decorated setter
        return createFocusListener(decoratedSetter);
    }

    /**
     * Updates the stride parameter fields based on the current model values.
     * This method ensures that the stride parameter fields are accurately set
     * based on the number of frames and the average stride.
     */
    private void updateStrideParamFields() {
        Runnable doUpdateStrideParam = () -> {
            int numberOfFrames = expSettingsModel.getLastFrame() - expSettingsModel.getFirstFrame() + 1;

            // Use variable points for the intensity, except when less than 1000 frames are present.
            int numPointsIntensityTrace = numberOfFrames;
            if (numberOfFrames >= 1000) {
                numPointsIntensityTrace = numberOfFrames / bleachCorrectionModel.getAverageStride();
            }

            bleachCorrectionView.setTextNumPointsIntensityTrace(String.valueOf(numPointsIntensityTrace));
            bleachCorrectionModel.setNumPointsIntensityTrace(numPointsIntensityTrace);
        };

        SwingUtilities.invokeLater(doUpdateStrideParam);
    }

    /**
     * Decorates a setter from the model to automatically update the stride parameter fields
     * after the model value has been changed.
     *
     * @param setter A Consumer that sets a value in the model.
     * @return A FocusListener that updates the model and then the view when focus is lost.
     */
    public FocusListener updateStrideParam(Consumer<String> setter) {
        Consumer<String> decoratedSetter = (String value) -> {
            setter.accept(value);
            updateStrideParamFields();
        };

        return createFocusListener(decoratedSetter);
    }

    /**
     * Returns a Consumer that decorates the given setter to also update the fit end.
     *
     * @param setter the original setter Consumer
     * @return a decorated Consumer that updates the fit end and calls the given setter
     */
    public Consumer<String> updateFitEnd(Consumer<String> setter) {
        return (String value) -> {
            setter.accept(value);
            fitController.updateFitEnd(expSettingsModel);
        };
    }
}