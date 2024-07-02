package fiji.plugin.imaging_fcs.new_imfcs.view.dialogs;

import ij.gui.GenericDialog;

import java.util.function.Consumer;

/**
 * A dialog view for selecting the sliding window size.
 * This dialog allows the user to input a window size for an operation, which is then passed to the provided listener when the dialog is OKed.
 */
public class SlidingWindowSelectionView extends GenericDialog {
    private final Consumer<Integer> listener;

    /**
     * Constructs a SlidingWindowSelectionView dialog.
     *
     * @param listener            The consumer that handles the action when the OK button is pressed,
     *                            receiving the selected window size as an integer.
     * @param slidingWindowLength The initial sliding window length to be set in the numeric field.
     */
    public SlidingWindowSelectionView(Consumer<Integer> listener, int slidingWindowLength) {
        super("Window Size");
        this.listener = listener;

        displayDialog(slidingWindowLength);
    }

    /**
     * Displays the dialog with a message and numeric field to enter the sliding window size.
     *
     * @param slidingWindowLength The sliding window length to be displayed as the default value in the numeric field.
     */
    private void displayDialog(int slidingWindowLength) {
        addMessage("Only positive Integers allowed.");
        addNumericField("Size: ", slidingWindowLength, 0);

        hideCancelButton();
        showDialog();

        if (wasOKed()) {
            listener.accept((int) getNextNumber());
        }
    }
}
