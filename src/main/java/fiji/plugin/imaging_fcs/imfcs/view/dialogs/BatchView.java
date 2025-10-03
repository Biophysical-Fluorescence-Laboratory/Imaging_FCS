package fiji.plugin.imaging_fcs.imfcs.view.dialogs;

import ij.gui.GenericDialog;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;

/**
 * A dialog window for configuring batch processing options in the imaging FCS plugin.
 * <p>
 * The {@code BatchView} class extends {@link GenericDialog} to present various
 * options related to batch processing tasks such as correlation, fitting, and DCCF
 * calculations. The user selections are collected and passed to a listener for further
 * processing.
 * </p>
 */
public class BatchView extends GenericDialog {
    /**
     * Constructs a {@code BatchView} dialog and immediately displays it.
     *
     * @param listener a {@link Consumer} that accepts a {@link Map} containing
     *                 the user's selections from the dialog.
     */
    public BatchView(Consumer<Map<String, Object>> listener) {
        super("Batch Processing");
        displayDialog(listener);
    }

    /**
     * Collects user input from the dialog and returns it as a map.
     * <p>
     * The map contains the user's selections for various batch processing options,
     * including whether to correlate data, perform fitting, calculate PSF, and more.
     * Each option is mapped to a corresponding boolean or string value.
     * </p>
     *
     * @return a {@link Map} with the user's selections.
     */
    private Map<String, Object> getUserInputAsDict() {
        Map<String, Object> out = new LinkedHashMap<>();
        out.put("Correlate All", getNextBoolean());
        out.put("Fit", getNextBoolean());
        out.put("PSF Calculation", getNextBoolean());
        out.put("Diffusion Law", getNextBoolean());
        out.put("Vertical DCCF", getNextBoolean());
        out.put("Horizontal DCCF", getNextBoolean());
        out.put("Diagonal Up DCCF", getNextBoolean());
        out.put("Diagonal Down DCCF", getNextBoolean());

        out.put("File suffix", getNextString());

        out.put("Save excel", getNextBoolean());
        out.put("Save plot windows", getNextBoolean());

        out.put("ONNX Inference", getNextBoolean());

        return out;
    }

    /**
     * Displays the batch processing dialog and passes the user input to the listener.
     * <p>
     * This method adds checkboxes and a text field to the dialog for various
     * batch processing options. Once the dialog is closed, the user's selections
     * are sent to the provided listener if the dialog was not canceled.
     * </p>
     *
     * @param listener a {@link Consumer} that handles the user input map.
     */
    private void displayDialog(Consumer<Map<String, Object>> listener) {
        addCheckbox("Correlate All", true);
        addCheckbox("Fit", false);
        addCheckbox("PSF Calculation", false);
        addCheckbox("Diffusion Law", false);
        addCheckbox("Vertical DCCF", false);
        addCheckbox("Horizontal DCCF", false);
        addCheckbox("Diagonal Up DCCF", false);
        addCheckbox("Diagonal Down DCCF", false);

        addStringField("File suffix", "");
        addMessage("If empty, the date will be used as suffix.");

        addCheckbox("Save excel", true);
        addCheckbox("Save plot windows", false);
        addCheckbox("ONNX Inference", false);

        showDialog();

        if (wasCanceled()) {
            return;
        }

        if (wasOKed()) {
            listener.accept(getUserInputAsDict());
        }
    }
}
