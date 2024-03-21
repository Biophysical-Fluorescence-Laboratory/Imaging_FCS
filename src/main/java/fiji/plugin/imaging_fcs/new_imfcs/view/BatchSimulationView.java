package fiji.plugin.imaging_fcs.new_imfcs.view;

import ij.gui.GenericDialog;

import java.util.function.Consumer;

public class BatchSimulationView extends GenericDialog {
    public BatchSimulationView() {
        super("Batch Simulation");
    }

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
