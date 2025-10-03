package fiji.plugin.imaging_fcs.imfcs.view;

import fiji.plugin.imaging_fcs.imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.imfcs.controller.OnnxInferenceController;
import fiji.plugin.imaging_fcs.imfcs.model.OnnxInferenceModel;
import fiji.plugin.imaging_fcs.imfcs.model.onnx.InputMetadata;
import ij.IJ;

import javax.swing.*;

import ai.onnxruntime.OrtException;

import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import static fiji.plugin.imaging_fcs.imfcs.controller.FieldListenerFactory.createFocusListener;
import static fiji.plugin.imaging_fcs.imfcs.view.ButtonFactory.createJButton;
import static fiji.plugin.imaging_fcs.imfcs.view.ButtonFactory.createJToggleButton;
import static fiji.plugin.imaging_fcs.imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.imfcs.view.UIUtils.createJLabel;
import static fiji.plugin.imaging_fcs.imfcs.view.TextFieldFactory.setText;

/**
 * Provides the user interface for configuring and running ONNX model inference
 * within the imaging FCS plugin. Users can specify the model path, input/stride
 * dimensions,
 * frame ranges, and trigger the inference process.
 */
public final class OnnxInferenceView extends BaseView {

    // --- Constants for Layout and Appearance (Adjust as needed) ---
    // Increased rows to accommodate more fields + button + status
    // private static final GridLayout VIEW_LAYOUT = new GridLayout(9, 4, 5, 5); // Rows, Cols, Hgap, Vgap
    // Adjust location and size based on where you want it relative to other plugin
    // windows
    private static final Point VIEW_LOCATION = new Point(Constants.MAIN_PANEL_POS.x,
            Constants.MAIN_PANEL_POS.y + Constants.MAIN_PANEL_DIM.height + 10);
    private static final Dimension VIEW_DIMENSION = new Dimension(480, 350); // Increased size

    // --- Model and Controller References ---
    private final OnnxInferenceModel model;
    private final OnnxInferenceController controller;

    // --- UI Component Fields ---
    private JTextField tfOnnxModelPath;
    private JButton btnBrowseOnnx;
    private JButton btnInitOnnx;
    private JTextField tfInputX;
    private JTextField tfInputY;
    private JTextField tfInputFrames;
    private JTextField tfStrideX;
    private JTextField tfStrideY;
    private JTextField tfStrideFrames;
    private JCheckBox cbUseGpu;
    private JButton btnRunInference;
    private JLabel lblStatus;
    private JToggleButton tbBatchSettings; 
    private JLabel lblDevice;

    enum Status {
        NO_MODEL_LOADED,
        READY,
        PROCESSING,
    }

    /**
     * Constructs an OnnxInferenceView with references to its controller and model.
     * Initializes the UI components for the ONNX inference settings window.
     *
     * @param controller The controller managing interactions for ONNX inference.
     * @param model      The model holding ONNX inference parameters and state.
     */
    public OnnxInferenceView(
            OnnxInferenceController controller,
            OnnxInferenceModel model) {
        // Title for the window
        super("ONNX Inference Settings");
        this.model = model;
        this.controller = controller;
        initializeUI(); // This calls configureWindow, initializeTextFields, addComponentsToFrame
    }

    /**
     * Sets up the basic window properties (layout, location, size).
     */
    @Override
    protected void configureWindow() {
        super.configureWindow(); // Call parent setup

        // setLayout(VIEW_LAYOUT);
        setLayout(new GridBagLayout());
        setLocation(VIEW_LOCATION);
        setSize(VIEW_DIMENSION);
        setResizable(true);
        setVisible(false); // Keep it hidden until explicitly shown
    }

    /**
     * Initialize text fields, buttons, and checkbox with default values and
     * tooltips.
     * No listeners are attached at this stage.
     */
    @Override
    protected void initializeTextFields() { // Renaming to initializeComponents might be better later
        // ONNX Model Path
        tfOnnxModelPath = createTextField("", "Path to the ONNX model file (.onnx)");
        tfOnnxModelPath.setEnabled(false); // Path often set via browser button

        tfInputX = createTextField("", "Model expected input width (loaded from model)");
        tfInputX.setEditable(false); // Loaded from model metadata
        tfInputY = createTextField("", "Model expected input height (loaded from model)");
        tfInputY.setEditable(false); // Loaded from model metadata
        tfInputFrames = createTextField("", "Model expected input frames (loaded from model)");
        tfInputFrames.setEditable(false); // Loaded from model metadata

        // Strides
        tfStrideX = createTextField("1", "Stride in X dimension (pixels)", createFocusListener(model::setStrideX));
        tfStrideY = createTextField("1", "Stride in Y dimension (pixels)", createFocusListener(model::setStrideY));
        // Default from original ImFCSNet model, but will autofill to the loaded model.
        tfStrideFrames = createTextField("2500", "Stride in Frames dimension",
                createFocusListener(model::setStrideFrames));

        // GPU Option
        cbUseGpu = new JCheckBox("Use GPU (if available)");
        cbUseGpu.setToolTipText("Attempt to use CUDA for inference if supported");
        cbUseGpu.addItemListener(controller.cbUseGpuToggled());

        // Status Label
        lblStatus = createJLabel("Status: No Model Loaded", "Displays current operation status");
        lblDevice = createJLabel("Device: CPU", "Displays current ONNX device.");
    }

    @Override
    protected void initializeButtons() {
        btnBrowseOnnx = createJButton("Load Model", "Select the ONNX model file", null, (ActionListener) e -> {
            try {
                controller.btnLoadPressed();
            } catch (OrtException e1) {
                IJ.log(e1.getStackTrace().toString());
                IJ.error(e1.getMessage());
            }
        });
        btnInitOnnx = createJButton("Init Model", "Start the ONNX environment for inference", null, 
            (ActionListener) e ->  {
            try {
                controller.startOnnxSession();
            } catch (OrtException e1) {
                IJ.log(e1.getStackTrace().toString());
                IJ.error(e1.getMessage());
            }
        });
        tbBatchSettings = createJToggleButton("Batch Settings", "Configure advanced batching parameters.", null,
                controller.tbBatchSettingsPressed()); 

        // Inference Button
        btnRunInference = createJButton("Run Inference", "Process the current image using the specified settings", null,
            (ActionListener) e -> {
                controller.btnRunInferencePressed();
            });
        btnRunInference.setForeground(Color.RED);

        // Start the inference button as disabled. Only enable explicitly once model is loaded.
        this.disableRunInferenceButton();

    }

    @Override
    protected void addComponentsToFrame() {
        // 1. Initialize GridBagConstraints
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL; // Components fill their cell width
        gbc.insets = new Insets(5, 5, 5, 5); // Use the original hgap/vgap (5, 5) as padding
        
        // We want input fields to stretch more than labels or buttons
        gbc.weightx = 1.0; // Default weight for components that span 1 column

        int row = 0; // Start row counter

        // --- Row 1: ONNX Model Path ---
        
        // Col 0: Label
        gbc.gridx = 0;
        gbc.gridy = row;
        gbc.weightx = 0; // Labels typically don't stretch
        gbc.gridwidth = 1;
        add(createJLabel("ONNX Model:", "Path to the ONNX model file (.onnx)"), gbc);

        // Col 1: Text Field (Give it more stretch capacity)
        gbc.gridx = 1;
        gbc.weightx = 1.0; 
        add(tfOnnxModelPath, gbc);

        // Col 2: Browse Button
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        add(btnBrowseOnnx, gbc);

        // Col 3: Init Button
        gbc.gridx = 3;
        gbc.weightx = 1.0;
        add(btnInitOnnx, gbc);

        row++;
        // Reset weightx for standard fields
        gbc.weightx = 0.5; 

        // --- Row 2: Input Dimensions (X, Y) ---
        gbc.gridy = row;
        gbc.gridx = 0; add(createJLabel("Input X:", "..."), gbc);
        gbc.gridx = 1; add(tfInputX, gbc);
        gbc.gridx = 2; add(createJLabel("Input Y:", "..."), gbc);
        gbc.gridx = 3; add(tfInputY, gbc);

        row++;

        // --- Row 3: Input Dimensions (Frames) ---
        gbc.gridy = row;
        gbc.gridx = 0; add(createJLabel("Input Frames:", "..."), gbc);
        gbc.gridx = 1; add(tfInputFrames, gbc);
        gbc.gridx = 2; add(createJLabel("", ""), gbc); // Spacer
        gbc.gridx = 3; add(createJLabel("", ""), gbc); // Spacer

        row++;

        // --- Row 4: Stride (X, Y) ---
        gbc.gridy = row;
        gbc.gridx = 0; add(createJLabel("Stride X:", "..."), gbc);
        gbc.gridx = 1; add(tfStrideX, gbc);
        gbc.gridx = 2; add(createJLabel("Stride Y:", "..."), gbc);
        gbc.gridx = 3; add(tfStrideY, gbc);

        row++;

        // --- Row 5: Stride (Frames) ---
        gbc.gridy = row;
        gbc.gridx = 0; add(createJLabel("Stride Frames:", "..."), gbc);
        gbc.gridx = 1; add(tfStrideFrames, gbc);
        gbc.gridx = 2; add(createJLabel("", ""), gbc); // Spacer
        gbc.gridx = 3; add(createJLabel("", ""), gbc); // Spacer

        row++;

        // --- Row 6: GPU Option / Batch Settings ---
        gbc.gridy = row;
        gbc.gridx = 0; add(cbUseGpu, gbc);
        gbc.gridx = 1; add(tbBatchSettings, gbc);
        gbc.gridx = 2; add(createJLabel("", ""), gbc);
        gbc.gridx = 3; add(createJLabel("", ""), gbc);

        row++;
        
        // --- Row 7: DEVICE LABEL (The Target) ---
        // Make the label span all 4 columns.
        gbc.gridy = row;
        gbc.gridx = 0; // Start at column 0
        gbc.gridwidth = 4; // Span four columns
        gbc.weightx = 1.0; // Ensure it stretches fully across the row
        
        add(lblDevice, gbc);
        
        // Reset constraints for subsequent rows
        gbc.gridwidth = 1;
        gbc.weightx = 0.5; 
        
        row++;

        // --- Row 8: Run Button ---
        gbc.gridy = row;
        gbc.gridx = 0; add(btnRunInference, gbc);
        gbc.gridx = 1; add(createJLabel("", ""), gbc); // Spacer
        gbc.gridx = 2; add(createJLabel("", ""), gbc); // Spacer
        gbc.gridx = 3; add(createJLabel("", ""), gbc); // Spacer

        row++;

        // --- Row 9: Status Label ---
        gbc.gridy = row;
        
        // Col 0: Label "Status:"
        gbc.gridx = 0;
        gbc.weightx = 0; // Don't let this label stretch
        add(createJLabel("Status:", "Current operation status"), gbc);
        
        // Col 1-3: Status Message Field (Span 3 columns)
        gbc.gridx = 1; 
        gbc.gridwidth = 3; // Span the remaining 3 columns
        gbc.weightx = 1.0; // Let the status message stretch fully
        add(lblStatus, gbc);
    }

    /**
     * Adds the initialized UI components (labels, text fields, buttons, checkbox)
     * to the view's frame according to the defined layout.
     */
    protected void _addComponentsToFrame() {
        // Row 1: ONNX Model Path
        add(createJLabel("ONNX Model:", "Path to the ONNX model file (.onnx)"));
        add(tfOnnxModelPath);
        add(btnBrowseOnnx);
        add(btnInitOnnx);
        
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
        add(createJLabel("Stride X:",
                "Stride in X dimension (pixels). Set equal to Model Input X for non-overlapping inference."));
        add(tfStrideX);
        add(createJLabel("Stride Y:",
                "Stride in Y dimension (pixels). Set equal to Model Input X for non-overlapping inference."));
        add(tfStrideY);

        // Row 5: Stride (Frames)
        add(createJLabel("Stride Frames:", "Stride in Frames dimension"));
        add(tfStrideFrames);
        add(createJLabel("", "")); // Spacer
        add(createJLabel("", "")); // Spacer

        // Row 6: GPU Option
        add(cbUseGpu);
        add(tbBatchSettings);
        add(createJLabel("", ""));
        add(createJLabel("", ""));

        // Row 7: Spacer Row (Optional)
        add(lblDevice); // Here, replace spacer with the lblDevice.
        // add(createJLabel("", ""));
        add(createJLabel("", ""));
        add(createJLabel("", ""));
        add(createJLabel("", ""));

        // Row 8: Run Button
        add(btnRunInference);
        add(createJLabel("", "")); // Spacer
        add(createJLabel("", "")); // Spacer
        add(createJLabel("", "")); // Spacer

        // Row 9: Status Label
        add(createJLabel("Status:", "Current operation status"));
        // Make status label span multiple columns for longer messages
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridwidth = 3; // Span 3 columns
        gbc.fill = GridBagConstraints.HORIZONTAL;
        add(lblStatus, gbc);
        add(lblStatus); // Occupies the last cell
    }

    /**
     * Updates the status message displayed to the user.
     * 
     * @param message The status message to display.
     */
    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(message);
        });
    }

    /**
     * Updates the device displayed to the user.
     * 
     * @param message The status message to display.
     */
    public void updateDevice(String message) {
        SwingUtilities.invokeLater(() -> {
            lblDevice.setText(message);
        });
    }

    /**
     * Enables or disables UI components based on whether processing is running.
     * (To be called by the Controller).
     * 
     * @param running True if processing is starting, false if it has finished or
     *                errored.
     */
    public void setRunningState(boolean running) {
        SwingUtilities.invokeLater(() -> {
            // Disable input fields and buttons while running
            tfOnnxModelPath.setEnabled(!running);
            btnBrowseOnnx.setEnabled(!running);
            btnInitOnnx.setEnabled(!running);
            tfInputX.setEnabled(!running);
            tfInputY.setEnabled(!running);
            tfInputFrames.setEnabled(!running);
            tfStrideX.setEnabled(!running);
            tfStrideY.setEnabled(!running);
            tfStrideFrames.setEnabled(!running);
            cbUseGpu.setEnabled(!running);
            btnRunInference.setEnabled(!running);

            // Optionally change Run button text
            btnRunInference.setText(running ? "Processing..." : "Run Inference");
        });
    }

    public void updateModelPath(String modelPath) {
        setText(tfOnnxModelPath, modelPath);
    }

    public void updateModelInputMetadata(int x, int y, int frames) {
        setText(tfInputX, x);
        setText(tfInputY, y);
        setText(tfInputFrames, frames);
    }

    public void setModelInputMetadata(InputMetadata inputMetadata) {
        tfInputX.setText(inputMetadata.getX());
        tfInputY.setText(inputMetadata.getY());
        tfInputFrames.setText(inputMetadata.getFrames());

        // Also set the stride in the frames dimension to be equivalent.
        this.setStrideFrames(inputMetadata.getFrames());
    }

    private void setStrideFrames(String frames) {
        this.tfStrideFrames.setText(frames);
    }

    public boolean getUseGPU() {
        return cbUseGpu.isSelected();
    }

    public String getStrideX() {
        return this.tfStrideX.getText();
    }

    public String getStrideY() {
        return this.tfStrideY.getText();
    }

    public String getStrideFrames() {
        return this.tfStrideFrames.getText();
    }

    public void disableRunInferenceButton() {
        this.btnRunInference.setEnabled(false);
    }

    public void enableRunInferenceButton() {
        this.btnRunInference.setEnabled(true);
    }
}
