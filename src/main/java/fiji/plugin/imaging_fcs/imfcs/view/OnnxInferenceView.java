package fiji.plugin.imaging_fcs.imfcs.view;

import fiji.plugin.imaging_fcs.imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.imfcs.controller.MainPanelController;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.util.function.Function;

import static fiji.plugin.imaging_fcs.imfcs.view.ButtonFactory.createJButton;
import static fiji.plugin.imaging_fcs.imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.imfcs.view.UIUtils.createJLabel;

/**
 * Provides the user interface for configuring and running ONNX model inference
 * within the imaging FCS plugin. Users can specify the model path, input/stride dimensions,
 * frame ranges, and trigger the inference process.
 */
public final class OnnxInferenceView extends BaseView {

    // --- Constants for Layout and Appearance (Adjust as needed) ---
    // Increased rows to accommodate more fields + button + status
    private static final GridLayout VIEW_LAYOUT = new GridLayout(10, 4, 5, 5); // Rows, Cols, Hgap, Vgap
    // Adjust location and size based on where you want it relative to other plugin windows
    private static final Point VIEW_LOCATION =
            new Point(Constants.MAIN_PANEL_POS.x, Constants.MAIN_PANEL_POS.y + Constants.MAIN_PANEL_DIM.height + 10);
    private static final Dimension VIEW_DIMENSION = new Dimension(480, 350); // Increased size

    // --- Model and Controller References (Placeholders) ---
    // private final OnnxInferenceModel model;
    // private final OnnxInferenceController controller;

    // --- UI Component Fields ---
    private JTextField tfOnnxModelPath;
    private JButton btnBrowseOnnx;
    private JTextField tfInputX;
    private JTextField tfInputY;
    private JTextField tfInputFrames;
    private JTextField tfStrideX;
    private JTextField tfStrideY;
    private JTextField tfStrideFrames;
    private JTextField tfInitialFrame;
    private JTextField tfFinalFrame;
    private JCheckBox cbUseGpu;
    private JButton btnRunInference;
    private JLabel lblStatus; // To display messages like "Processing...", "Done", errors


    /**
     * Constructs an OnnxInferenceView with references to its controller and model.
     * Initializes the UI components for the ONNX inference settings window.
     *
     * @param controller The controller managing interactions for ONNX inference.
     * @param model      The model holding ONNX inference parameters and state.
     */
    public OnnxInferenceView(
        // OnnxInferenceController controller, 
        // OnnxInferenceModel model
    ) {
        // Title for the window
        super("ONNX Inference Settings");
        // this.model = model;
        // this.controller = controller;
        initializeUI(); // This calls configureWindow, initializeTextFields, addComponentsToFrame
    }

    /**
     * Sets up the basic window properties (layout, location, size).
     */
    @Override
    protected void configureWindow() {
        super.configureWindow(); // Call parent setup

        setLayout(VIEW_LAYOUT);
        setLocation(VIEW_LOCATION);
        setSize(VIEW_DIMENSION);
        // setResizable(false); // Optional: prevent resizing

        setVisible(false); // Keep it hidden until explicitly shown
    }

    /**
     * Initialize text fields, buttons, and checkbox with default values and tooltips.
     * No listeners are attached at this stage.
     */
    @Override
    protected void initializeTextFields() { // Renaming to initializeComponents might be better later
        // ONNX Model Path
        tfOnnxModelPath = createTextField("", "Path to the ONNX model file (.onnx)");
        tfOnnxModelPath.setEnabled(false); // Path often set via browser button
        btnBrowseOnnx = new JButton("Browse...");
        btnBrowseOnnx.setToolTipText("Select the ONNX model file");

        // Input Dimensions
        tfInputX = createTextField("", "Model input X dimension (pixels)");
        tfInputY = createTextField("", "Model input Y dimension (pixels)");
        tfInputFrames = createTextField("", "Model input Frames dimension");
        // Disable these fields, as these are read from the model file.
        tfInputX.setEnabled(false);
        tfInputY.setEnabled(false);
        tfInputFrames.setEnabled(false);

        // Strides
        tfStrideX = createTextField("1", "Stride in X dimension (pixels)"); // Default based on example
        tfStrideY = createTextField("1", "Stride in Y dimension (pixels)"); // Default based on example
        tfStrideFrames = createTextField("2500", "Stride in Frames dimension"); // Default based on example

        // Frame Range
        tfInitialFrame = createTextField("1", "Initial frame to process (1-based index)"); // Default based on example (adjust if 0-based needed)
        tfFinalFrame = createTextField("-1", "Final frame to process (-1 for end of stack)"); // Default based on example

        // GPU Option
        cbUseGpu = new JCheckBox("Use GPU (if available)");
        cbUseGpu.setToolTipText("Attempt to use CUDA for inference if supported");

        // Action Button
        btnRunInference = createJButton("Run Inference", "Process the current image using the specified settings", null, (ActionListener) e -> {});
        btnRunInference.setForeground(Color.RED);

        // Status Label
        lblStatus = createJLabel("Status: Ready", "Displays current operation status");
    }

    /**
     * Adds the initialized UI components (labels, text fields, buttons, checkbox)
     * to the view's frame according to the defined layout.
     */
    @Override
    protected void addComponentsToFrame() {
        // Row 1: ONNX Model Path
        add(createJLabel("ONNX Model:", "Path to the ONNX model file (.onnx)"));
        add(tfOnnxModelPath);
        add(btnBrowseOnnx);
        add(createJLabel("", "")); // Spacer

        // Row 2: Input Dimensions (X, Y)
        add(createJLabel("Input X:", "Model input X dimension (pixels)"));
        add(tfInputX);
        add(createJLabel("Input Y:", "Model input Y dimension (pixels)"));
        add(tfInputY);

        // Row 3: Input Dimensions (Frames)
        add(createJLabel("Input Frames:", "Model input Frames dimension"));
        add(tfInputFrames);
        add(createJLabel("", "")); // Spacer
        add(createJLabel("", "")); // Spacer

        // Row 4: Stride (X, Y)
        add(createJLabel("Stride X:", "Stride in X dimension (pixels)"));
        add(tfStrideX);
        add(createJLabel("Stride Y:", "Stride in Y dimension (pixels)"));
        add(tfStrideY);

        // Row 5: Stride (Frames)
        add(createJLabel("Stride Frames:", "Stride in Frames dimension"));
        add(tfStrideFrames);
        add(createJLabel("", "")); // Spacer
        add(createJLabel("", "")); // Spacer

        // Row 6: Frame Range
        add(createJLabel("Initial Frame:", "First frame to process (1-based index)"));
        add(tfInitialFrame);
        add(createJLabel("Final Frame:", "Last frame to process (-1 for end)"));
        add(tfFinalFrame);

        // Row 7: GPU Option
        add(cbUseGpu);
        add(createJLabel("", "")); // Spacer
        add(createJLabel("", "")); // Spacer
        add(createJLabel("", "")); // Spacer

        // Row 8: Spacer Row (Optional)
        add(createJLabel("", ""));
        add(createJLabel("", ""));
        add(createJLabel("", ""));
        add(createJLabel("", ""));

        // Row 9: Run Button
        add(btnRunInference);
        add(createJLabel("", "")); // Spacer
        add(createJLabel("", "")); // Spacer
        add(createJLabel("", "")); // Spacer

        // Row 10: Status Label
        add(createJLabel("Status:", "Current operation status"));
        // Make status label span multiple columns for longer messages
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 3; // Span 3 columns
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(lblStatus, gbc);
        // Note: Adding components with GridBagConstraints directly might conflict
        // if BaseView strictly uses the GridLayout set earlier.
        // A simpler approach if GridBag isn't needed is just:
        // add(lblStatus);
        // add(createJLabel("", "")); // Spacer
        // add(createJLabel("", "")); // Spacer
        // Let's stick to the simpler grid layout adding for now:
        // Remove the GBC code above and uncomment below:
        // add(lblStatus);
        // add(createJLabel("", "")); // Spacer
        // add(createJLabel("", "")); // Spacer
        // --- Re-evaluate layout for status ---
        // Let's just put status label in col 2
        // add(createJLabel("Status:", "Current operation status")); // Already added in prev row
        // add(lblStatus);
        // add(createJLabel("", "")); // Spacer
        // add(createJLabel("", "")); // Spacer
        // Let's try putting Status label and text on the last row:
        add(lblStatus); // Occupies the last cell

    }

    // --- Placeholder methods for Controller interaction ---

    /**
     * Updates the status message displayed to the user.
     * (To be called by the Controller).
     * @param message The status message to display.
     */
    public void updateStatus(String message) {
        // Ensure UI updates happen on the Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(message);
        });
    }

    /**
     * Enables or disables UI components based on whether processing is running.
     * (To be called by the Controller).
     * @param running True if processing is starting, false if it has finished or errored.
     */
    public void setRunningState(boolean running) {
         SwingUtilities.invokeLater(() -> {
             // Disable input fields and buttons while running
             tfOnnxModelPath.setEnabled(!running);
             btnBrowseOnnx.setEnabled(!running);
             tfInputX.setEnabled(!running);
             tfInputY.setEnabled(!running);
             tfInputFrames.setEnabled(!running);
             tfStrideX.setEnabled(!running);
             tfStrideY.setEnabled(!running);
             tfStrideFrames.setEnabled(!running);
             tfInitialFrame.setEnabled(!running);
             tfFinalFrame.setEnabled(!running);
             cbUseGpu.setEnabled(!running);
             btnRunInference.setEnabled(!running);

             // Optionally change Run button text
             btnRunInference.setText(running ? "Processing..." : "Run Inference");
         });
    }

    // Add getter methods for components if the controller needs to directly access them
    // (Though typically controller modifies model, and view updates from model changes)
    // Example:
    // public JButton getRunButton() { return btnRunInference; }
    // public JCheckBox getGpuCheckbox() { return cbUseGpu; }
    // public String getOnnxPath() { return tfOnnxModelPath.getText(); } // Controller might read directly on action
    // ... etc for other fields
}
