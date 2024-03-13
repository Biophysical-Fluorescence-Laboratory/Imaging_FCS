package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.HardwareModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.OptionsModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.ExpSettingsView;
import fiji.plugin.imaging_fcs.new_imfcs.view.MainPanelView;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.event.*;
import java.util.function.Consumer;
import java.util.function.Function;

import static fiji.plugin.imaging_fcs.new_imfcs.controller.FocusListenerFactory.createFocusListener;

/**
 * Controller class for the main panel of the FCS plugin. It handles user interactions and coordinates
 * the update of models based on user input, as well as the update of views to reflect the current state
 * of the models.
 */
public class MainPanelController {
    // Field declarations for the views and models that this controller will manage
    private final MainPanelView view;
    private final ExpSettingsView expSettingsView;
    private final HardwareModel hardwareModel;
    private final OptionsModel optionsModel;
    private final ImageController imageController;
    private final ExpSettingsModel expSettingsModel;
    private final SimulationController simulationController;

    /**
     * Constructor that initializes models, views, and other controllers needed for the main panel.
     *
     * @param hardwareModel The model containing hardware settings for the imaging FCS analysis.
     */
    public MainPanelController(HardwareModel hardwareModel) {
        this.hardwareModel = hardwareModel;
        this.optionsModel = new OptionsModel(hardwareModel.isCuda());

        ImageModel imageModel = new ImageModel();
        this.imageController = new ImageController(imageModel);

        this.expSettingsModel = new ExpSettingsModel();
        this.expSettingsView = new ExpSettingsView(this, expSettingsModel);
        updateSettingsField();

        this.simulationController = new SimulationController(imageController, expSettingsModel);

        this.view = new MainPanelView(this, this.expSettingsModel);
    }

    public DocumentListener tfLastFrameChanged() {
        // TODO: FIXME
        return null;
    }

    public DocumentListener tfFirstFrameChanged() {
        // TODO: FIXME
        return null;
    }

    public ActionListener cbBleachCorChanged() {
        // TODO: FIXME
        return null;
    }

    public ActionListener cbFilterChanged() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnExitPressed() {
        return (ActionEvent ev) -> view.dispose();
    }

    public ActionListener btnBtfPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnUseExistingPressed() {
        return (ActionEvent ev) -> {
            if (WindowManager.getImageCount() > 0) {
                try {
                    imageController.loadImage(IJ.getImage(), false);
                } catch (RuntimeException e) {
                    IJ.showMessage("Wrong image format", e.getMessage());
                }
            } else {
                IJ.showMessage("No image open.");
            }
        };
    }

    public ActionListener btnLoadNewPressed() {
        return (ActionEvent ev) -> {
            ImagePlus image = IJ.openImage();
            if (image != null) {
                try {
                    imageController.loadImage(image, false);
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

    public ActionListener btnOptionsPressed() {
        return (ActionEvent ev) -> new OptionsController(optionsModel);
    }

    public ItemListener tbNBPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener tbFilteringPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnAvePressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnParaCorPressed() {
        // TODO: FIXME
        return null;
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

    public ItemListener tbExpSettingsPressed() {
        return (ItemEvent ev) -> expSettingsView.setVisible(ev.getStateChange() == ItemEvent.SELECTED);
    }

    public ItemListener tbBleachCorStridePressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbFitPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbSimPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setText(selected ? "Sim on" : "Sim off");
            simulationController.setVisible(selected);
        };
    }

    public ActionListener tbMSDPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbOverlapPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbBackgroundPressed() {
        // TODO: FIXME
        return null;
    }

    /**
     * Updates the display fields in the experimental settings view based on the current values in the model.
     * This method ensures that the UI reflects the most up-to-date settings.
     */
    private void updateSettingsField() {
        Runnable doUpdateSettingsField = new Runnable() {
            @Override
            public void run() {
                expSettingsModel.updateSettings();

                // Use scientific notation for these fields
                Function<Double, String> formatter = value -> String.format("%6.2e", value);

                expSettingsView.setTextParamA(formatter.apply(expSettingsModel.getParamA()));
                expSettingsView.setTextParamW(formatter.apply(expSettingsModel.getParamW()));
                expSettingsView.setTextParamW2(formatter.apply(expSettingsModel.getParamW2()));
                expSettingsView.setTextParamZ(formatter.apply(expSettingsModel.getParamZ()));
                expSettingsView.setTextParamZ2(formatter.apply(expSettingsModel.getParamZ2()));
                expSettingsView.setTextParamRx(formatter.apply(expSettingsModel.getParamRx()));
                expSettingsView.setTextParamRy(formatter.apply(expSettingsModel.getParamRy()));
            }
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
}