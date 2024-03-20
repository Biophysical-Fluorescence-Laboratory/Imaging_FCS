package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.model.OptionsModel;
import ij.gui.GenericDialog;

/**
 * The OptionsView class provides a user interface for configuring various plotting and processing options
 * for imaging FCS analysis. It extends the GenericDialog class, offering a customized dialog tailored to the
 * specific needs of the imaging FCS plugin. The view reads its initial state from the OptionsModel and updates
 * the model through the OptionsController upon user confirmation.
 */
public class OptionsView extends GenericDialog {
    private final Runnable listener;

    /**
     * Constructs an OptionsView dialog with a specified OptionsController.
     *
     * @param listener The listener to the dialog actions.
     */
    public OptionsView(Runnable listener) {
        super("Options");
        this.listener = listener;
    }

    /**
     * Displays the options dialog to the user, initializing checkboxes for each configuration option based on the
     * current state of the OptionsModel. If CUDA is detected, an additional checkbox for GPU usage is also displayed.
     *
     * @param model The OptionsModel containing the current configuration to be displayed in the dialog.
     */
    public void displayOptionsDialog(OptionsModel model) {
        addCheckbox("ACF", model.isPlotACFCurves());
        addCheckbox("SD", model.isPlotSDCurves());
        addCheckbox("Intensity", model.isPlotIntensityCurves());
        addCheckbox("Residuals", model.isPlotResCurves());
        addCheckbox("Histogram", model.isPlotParaHist());
        addCheckbox("Blocking", model.isPlotBlockingCurve());
        addCheckbox("Covariance Matrix", model.isPlotCovMats());

        // this box only exists if Cuda is detected
        if (model.isCuda()) {
            addCheckbox("GPU", model.isUseGpu());
        }

        hideCancelButton();
        showDialog();

        // If the OK button was pressed, inform the listener to update the model
        if (wasOKed()) {
            listener.run();
        }
    }
}
