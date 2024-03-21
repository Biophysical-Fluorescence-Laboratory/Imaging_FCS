package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.HardwareModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.SplashScreenView;

import javax.swing.*;

/**
 * Controls the behavior and timing of the splash screen for the Imaging FCS application.
 * It initializes the splash screen with hardware information and schedules its automatic closure.
 */
public class SplashScreenController {
    private static final int duration = 3000; // milliseconds
    private final SplashScreenView splashScreenView;
    private final HardwareModel hardwareModel;

    /**
     * Constructor for the SplashScreenController.
     * It initializes the hardware model and the splash screen view, and then displays the splash screen.
     */
    public SplashScreenController() {
        this.hardwareModel = new HardwareModel();

        splashScreenView = new SplashScreenView(hardwareModel.isCuda(), hardwareModel.getCudaMessage());
        showSplashScreen();
    }

    /**
     * Displays the splash screen for a fixed duration and then disposes of it,
     * proceeding to initialize the main panel controller.
     */
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
