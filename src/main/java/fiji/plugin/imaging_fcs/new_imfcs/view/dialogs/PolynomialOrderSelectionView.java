package fiji.plugin.imaging_fcs.new_imfcs.view.dialogs;

import fiji.plugin.imaging_fcs.new_imfcs.model.fit.BleachCorrectionModel;
import ij.gui.GenericDialog;

import java.util.function.Consumer;

/**
 * A dialog view for selecting the polynomial order for bleach correction.
 */
public class PolynomialOrderSelectionView extends GenericDialog {
    private final Consumer<Integer> listener;

    /**
     * Constructs a PolynomialOrderSelectionView dialog.
     *
     * @param listener        The consumer that handles the action when OK button is pressed,
     *                        receiving the selected polynomial order as an integer.
     * @param polynomialOrder The initial polynomial order to be set in the numeric field.
     */
    public PolynomialOrderSelectionView(Consumer<Integer> listener, int polynomialOrder) {
        super("Polynomial Order");
        this.listener = listener;

        displayDialog(polynomialOrder);
    }

    /**
     * Displays the dialog with a message and numeric field to enter the polynomial order.
     *
     * @param polynomialOrder The polynomial order to be displayed as the default value in the numeric field.
     */
    private void displayDialog(int polynomialOrder) {
        addMessage(String.format("Only positive integers <= %d allowed.", BleachCorrectionModel.MAX_POLYNOMIAL_ORDER));
        addNumericField("Order: ", polynomialOrder, 0);

        hideCancelButton();
        showDialog();

        if (wasOKed()) {
            listener.accept((int) getNextNumber());
        }
    }
}
