package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.OptionsModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.OptionsView;

/**
 * The OptionsController class manages the interactions between the OptionsModel and the OptionsView.
 * It initializes the view with the current state of the model and updates the model based on user inputs
 * through the view's dialog. This class acts as a part of the MVC (Model-View-Controller) architectural pattern,
 * focusing on the user interaction logic for the options dialog.
 */
public class OptionsController {
    private final OptionsModel optionsModel;
    private final OptionsView optionsView;

    /**
     * Constructs an OptionsController with a given OptionsModel.
     * It also initializes the OptionsView and displays the options dialog with the current settings from the model.
     *
     * @param optionsModel The OptionsModel instance containing the settings to be displayed and modified.
     */
    public OptionsController(OptionsModel optionsModel) {
        this.optionsModel = optionsModel;
        this.optionsView = new OptionsView(this::onDialogOk);
        this.optionsView.displayOptionsDialog(optionsModel);
    }

    /**
     * Called when the OK button in the options dialog is pressed.
     * This method updates the OptionsModel with the new settings based on the user's input.
     * If CUDA is detected, it also updates the model with the user's preference for using GPU.
     */
    public void onDialogOk() {
        // with the implementation of GenericDialog you can only get the boolean in order
        optionsModel.setPlotACFCurves(optionsView.getNextBoolean());
        optionsModel.setPlotSDCurves(optionsView.getNextBoolean());
        optionsModel.setPlotIntensityCurves(optionsView.getNextBoolean());
        optionsModel.setPlotResCurves(optionsView.getNextBoolean());
        optionsModel.setPlotParaHist(optionsView.getNextBoolean());
        optionsModel.setPlotBlockingCurve(optionsView.getNextBoolean());
        optionsModel.setPlotCovMats(optionsView.getNextBoolean());

        // This box only exist if Cuda was detected
        if (optionsModel.isCuda()) {
            optionsModel.setUseGpu(optionsView.getNextBoolean());
        }
    }
}
