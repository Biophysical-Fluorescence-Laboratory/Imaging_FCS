package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.SimulationView;
import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class SimulationController {
    private final ImageController imageController;
    private final SimulationView simulationView;
    private final SimulationModel simulationModel;
    private final ExpSettingsModel expSettingsModel;

    public SimulationController(ImageController imageController, ExpSettingsModel expSettingsModel) {
        this.imageController = imageController;
        this.expSettingsModel = expSettingsModel;
        simulationModel = new SimulationModel(this, expSettingsModel);
        simulationView = new SimulationView(this, simulationModel);
    }

    public void setVisible(boolean b) {
        simulationView.setVisible(b);
    }

    public ActionListener cbModeChanged() {
        return (ActionEvent ev) -> {
            String simMode = ControllerUtils.getComboBoxSelectionFromEvent(ev);
            boolean is2D = simMode.contains("2D");
            boolean isDomain = simMode.contains("dom");
            boolean isMesh = simMode.contains("mesh");

            simulationView.bleachSetEnable(is2D);
            simulationModel.setIs2D(is2D);

            simulationView.domainSetEnable(isDomain);
            simulationModel.setIsDomain(isDomain);

            simulationView.meshSetEnable(isMesh);
            simulationModel.setIsMesh(isMesh);
        };
    }

    public ActionListener btnSimulatePressed() {
        return (ActionEvent ev) -> {
            if (!simulationModel.getIs2D()) {
                // 3D
                if (expSettingsModel.getSigmaZ() <= 0) {
                    IJ.showMessage("SigmaZ (LightSheetThickness) can't be <= 0 (3D only)");
                    return;
                } else if (expSettingsModel.getSigmaZ() > 100) {
                    IJ.showMessage("SigmaZ (LightSheetThickness) can't be > 100 (3D only)");
                    return;
                }

                if (expSettingsModel.getNA() >= 1.33) {
                    IJ.showMessage("For 3D simulations NA has to be smaller than 1.33");
                    return;
                }
            }

            simulationView.enableBtnStopSimulation(true);
            simulationModel.execute();
        };
    }

    public ActionListener btnStopSimulationPressed() {
        return (ActionEvent ev) -> simulationModel.cancel(true);
    }

    public ActionListener btnBatchSimPressed() {
        // FIXME
        return null;
    }

    public ItemListener tbSimTripPressed() {
        return (ItemEvent ev) -> {
            // Get the button
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setText(selected ? "Triplet On" : "Triplet Off");
            simulationView.tripletSetEnable(selected);
            simulationModel.setBlinkFlag(selected);
        };
    }

    public void loadImage(ImagePlus image) {
        simulationView.enableBtnStopSimulation(false);
        imageController.loadImage(image, true);
    }
}
