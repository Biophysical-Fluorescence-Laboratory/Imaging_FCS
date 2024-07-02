package fiji.plugin.imaging_fcs.new_imfcs.view.dialogs;

import ij.gui.GenericDialog;

import java.util.function.Consumer;

/**
 * A dialog for inputting parameters for batch simulations.
 * This class provides a user interface for entering various simulation parameters
 * such as diffusion rates (D) and fraction (F2) ranges, and executes a specified action
 * upon user confirmation.
 */
public final class BatchSimulationView extends GenericDialog {
    public BatchSimulationView() {
        super("Batch Simulation");
    }

    /**
     * Displays the dialog with input fields for simulation parameters and registers a callback
     * to be invoked if the user clicks OK.
     * <p>
     * This method sets up numeric fields for entering start, end, and step values for both
     * diffusion rates (D, D2) and fraction (F2), then shows the dialog. If the user confirms,
     * the specified listener is invoked with this view as its argument, allowing access to the
     * inputted values.
     * </p>
     *
     * @param listener A {@link Consumer} that accepts this view to process the inputted parameters upon confirmation.
     */
    public void display(Consumer<BatchSimulationView> listener) {
        addNumericField("D start ", 1, 1);
        addNumericField("D end ", 10, 1);
        addNumericField("D step ", 1, 1);
        addNumericField("D2 start ", 1, 1);
        addNumericField("D2 end ", 10, 1);
        addNumericField("D2 step ", 1, 1);
        addNumericField("F2 start ", 0, 0);
        addNumericField("F2 end ", 1, 0);
        addNumericField("F2 step ", 0.1, 2);
        showDialog();

        if (wasOKed()) {
            listener.accept(this);
        }
    }
}
