package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.version.VERSION;

import javax.swing.*;
import java.awt.*;

/**
 * A class representing the splash screen of the Imaging FCS application.
 * This splash screen displays the current version and indicates whether the application
 * is running in GPU or CPU mode, along with a custom message related to CUDA if applicable.
 */
public class SplashScreenView extends JFrame {
    // Define the layout and dimensions of the splash screen.
    private static final GridLayout SPLASH_SCREEN_LAYOUT = new GridLayout(2, 1);
    private static final Dimension SPLASH_SCREEN_DIMENSION = new Dimension(300, 200);

    /**
     * Constructor for the splash screen.
     *
     * @param isCuda      Indicates if the application is running in CUDA (GPU) mode.
     * @param cudaMessage A custom message to display regarding CUDA, if applicable.
     */
    public SplashScreenView(boolean isCuda, String cudaMessage) {
        super("Loading Imaging FCS " + VERSION.IMFCS_VERSION);
        initializeUI(isCuda, cudaMessage);
    }

    /**
     * Initializes the user interface of the splash screen.
     *
     * @param isCuda      Indicates if CUDA mode is enabled.
     * @param cudaMessage The custom CUDA message to display.
     */
    private void initializeUI(boolean isCuda, String cudaMessage) {
        configureWindow();
        addComponents(isCuda, cudaMessage);
    }

    /**
     * Configures the basic settings of the splash screen window.
     */
    private void configureWindow() {
        setFocusable(true);
        setResizable(false);
        setLayout(SPLASH_SCREEN_LAYOUT);
        setSize(SPLASH_SCREEN_DIMENSION);
        centerWindow();

        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    }

    /**
     * Adds UI components to the splash screen.
     *
     * @param isCuda      Indicates if CUDA mode is enabled.
     * @param cudaMessage The custom CUDA message to display.
     */
    private void addComponents(boolean isCuda, String cudaMessage) {
        String mode = isCuda ? "GPU mode" : "CPU mode";
        JLabel modeLabel = createCenterAlignedLabel(mode);
        JLabel messageLabel = createCenterAlignedLabel(String.format("<html><p>%s</p></html>", cudaMessage));

        add(modeLabel);
        add(messageLabel);
    }

    /**
     * Creates a JLabel with its text centered.
     *
     * @param text The text to display in the label.
     * @return A JLabel with centered text.
     */
    private JLabel createCenterAlignedLabel(String text) {
        JLabel label = new JLabel(text);
        label.setVerticalAlignment(JLabel.CENTER);
        label.setHorizontalAlignment(JLabel.CENTER);
        return label;
    }

    /**
     * Centers the splash screen window on the screen.
     */
    private void centerWindow() {
        setLocationRelativeTo(null); // Center the window on the screen
    }
}
