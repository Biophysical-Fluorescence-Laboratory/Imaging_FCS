package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.HardwareModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.SplashScreenView;

import javax.swing.*;

public class SplashScreenController {
    private static final int duration = 3000; // milliseconds
    private final SplashScreenView splashScreenView;
    private final HardwareModel hardwareModel;

    public SplashScreenController() {
        this.hardwareModel = new HardwareModel();

        splashScreenView = new SplashScreenView(hardwareModel.isCuda(), hardwareModel.getCudaMessage());
        showSplashScreen();
    }

    private void showSplashScreen() {
        splashScreenView.setVisible(true);

        Timer timer = new Timer(duration, e -> {
            splashScreenView.dispose(); // Close the splash screen
            new MainPanelController(hardwareModel);
        });
        timer.setRepeats(false);
        timer.start();
    }
}
