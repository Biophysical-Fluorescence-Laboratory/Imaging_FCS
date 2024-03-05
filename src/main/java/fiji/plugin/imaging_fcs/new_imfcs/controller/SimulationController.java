package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.view.SimulationView;

import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

public class SimulationController {
    private final SimulationView simulationView;

    public SimulationController() {
        simulationView = new SimulationView(this);
    }

    public void setVisible(boolean b) {
        simulationView.setVisible(b);
    }

    public ActionListener btnSimulatePressed() {
        // FIXME
        return null;
    }

    public ActionListener btnStopSimulationPressed() {
        // FIXME
        return null;
    }

    public ActionListener btnBatchSimPressed() {
        // FIXME
        return null;
    }

    public ItemListener tbSimTripPressed() {
        // FIXME
        return null;
    }
}
