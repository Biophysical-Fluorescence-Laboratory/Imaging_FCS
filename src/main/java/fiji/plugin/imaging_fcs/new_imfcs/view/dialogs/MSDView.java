package fiji.plugin.imaging_fcs.new_imfcs.view.dialogs;

import ij.gui.GenericDialog;

import java.util.function.Consumer;

/**
 * MSDView class represents a dialog for selecting the model dimension (2D or 3D)
 * in the Mean Squared Displacement (MSD) analysis. It extends the GenericDialog
 * class from the ImageJ library.
 */
public class MSDView extends GenericDialog {
    /**
     * Constructs an MSDView dialog.
     *
     * @param is3d     a boolean indicating whether the 3D model is selected by default.
     * @param listener a Consumer that takes a Boolean and handles the user's selection.
     */
    public MSDView(boolean is3d, Consumer<Boolean> listener) {
        super("MSD model");

        displayDialog(is3d, listener);
    }

    /**
     * Displays the dialog for selecting the model dimension.
     *
     * @param is3d     a boolean indicating whether the 3D model is selected by default.
     * @param listener a Consumer that takes a Boolean and handles the user's selection.
     */
    private void displayDialog(boolean is3d, Consumer<Boolean> listener) {
        addMessage("2D is default (ITIR-FCS)");
        addCheckbox("3D (SPIM-FCS)", is3d);

        hideCancelButton();
        showDialog();

        if (wasOKed()) {
            listener.accept(getNextBoolean());
        }
    }
}
