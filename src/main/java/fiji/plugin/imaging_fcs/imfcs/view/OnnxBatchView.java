package fiji.plugin.imaging_fcs.imfcs.view;

import fiji.plugin.imaging_fcs.imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.imfcs.controller.OnnxBatchController;
import fiji.plugin.imaging_fcs.imfcs.model.onnx.OnnxBatchModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ItemListener;

import static fiji.plugin.imaging_fcs.imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.imfcs.view.UIUtils.createJLabel;
import static fiji.plugin.imaging_fcs.imfcs.view.TextFieldFactory.setText;
import static fiji.plugin.imaging_fcs.imfcs.controller.FieldListenerFactory.createFocusListener;

/**
 * Provides the user interface for configuring advanced ONNX batching
 * parameters,
 * such as maximum batch size and pre-fetch settings.
 */
public final class OnnxBatchView extends BaseView {

    // --- Constants for Layout and Appearance ---
    private static final GridLayout VIEW_LAYOUT = new GridLayout(5, 2, 5, 5); // Simple layout for settings
    private static final Point VIEW_LOCATION = new Point(Constants.MAIN_PANEL_POS.x + 500,
            Constants.MAIN_PANEL_POS.y + 10);
    private static final Dimension VIEW_DIMENSION = new Dimension(350, 200);

    private final OnnxBatchModel model;
    private final OnnxBatchController controller;

    // --- UI Component Fields ---
    private JTextField tfBatchSize;
    private JCheckBox cbEnableBatching;
    private JTextField tfPrefetchWorkers;
    private JLabel lblStatus;

    /**
     * Constructs the OnnxBatchView.
     * (Model and Controller references omitted as per instruction to focus only on
     * the view structure).
     */
    // If a controller/model were used:
    // public OnnxBatchView(OnnxBatchController controller, OnnxBatchModel model) {
    // ... }
    public OnnxBatchView(OnnxBatchController controller, OnnxBatchModel model) {
        super("ONNX Batch Settings");
        this.model = model;
        this.controller = controller;
        initializeUI();
    }

    /**
     * Sets up the basic window properties.
     */
    @Override
    protected void configureWindow() {
        super.configureWindow(); // Call parent setup

        setLayout(VIEW_LAYOUT);
        setLocation(VIEW_LOCATION);
        setSize(VIEW_DIMENSION);
        setResizable(false);

        setVisible(false); // Keep it hidden until explicitly shown
    }

    /**
     * Initialize UI components with default values and tooltips.
     */
    @Override
    protected void initializeTextFields() {
        // tfStrideX = createTextField("1", "Stride in X dimension (pixels)",
        // createFocusListener(model::setStrideX));
        // Batch Size
        tfBatchSize = createTextField("1", "Maximum number of input windows to process simultaneously in a batch.",
                createFocusListener(model::setBatchSize));

        // Prefetch Workers (Optional setting for async loading)
        tfPrefetchWorkers = createTextField("0",
                "Number of background threads dedicated to prefetching data (0 = disabled).",
                createFocusListener(model::setNumPrefetchWorkers));

        // Checkbox
        cbEnableBatching = new JCheckBox("Enable Batching");
        cbEnableBatching
                .setToolTipText("If enabled, inference will attempt to process multiple input windows per ONNX call.");

        // Status Label
        lblStatus = createJLabel("Status: Disabled", "Current status of the batching pipeline");
    }

    /**
     * No specialized buttons needed for this simple view skeleton.
     */
    @Override
    protected void initializeButtons() {
        // No custom buttons currently defined for this view.
    }

    /**
     * Adds the initialized UI components to the frame.
     */
    @Override
    protected void addComponentsToFrame() {
        // Row 1: Enable Checkbox
        add(cbEnableBatching);
        add(createJLabel("", ""));

        // Row 2: Batch Size
        add(createJLabel("Max Batch Size:", "Maximum number of inputs bundled per ONNX session call."));
        add(tfBatchSize);

        // Row 3: Prefetch Workers
        add(createJLabel("Prefetch Workers:", "Number of threads to load data in background."));
        add(tfPrefetchWorkers);

        // Row 4: Spacer
        add(createJLabel("", ""));
        add(createJLabel("", ""));

        // Row 5: Status Label
        add(createJLabel("Current Status:", ""));
        add(lblStatus);
    }

    // --- Public methods for Controller interaction (Skeletons) ---

    public void updateStatus(String message) {
        SwingUtilities.invokeLater(() -> {
            lblStatus.setText(message);
        });
    }

    // Example getter for controller
    public int getBatchSize() {
        try {
            return Integer.parseInt(tfBatchSize.getText());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    public void setBatchEnabledListener(ItemListener batchEnabledListener) {
        this.cbEnableBatching.addItemListener(batchEnabledListener);
    }

    public void setBatchSizeText(String text) {
        setText(tfBatchSize, text);
    }

    public void setPrefetchWorkersText(String text) {
        setText(tfPrefetchWorkers, text);
    }

    public void setBatchEnabledStatus(boolean selected) {
        this.cbEnableBatching.setSelected(selected);
    }

    public void setInputsEnabled(boolean enabled) {
        tfBatchSize.setEnabled(enabled);
        tfPrefetchWorkers.setEnabled(enabled);
    }

    public boolean getBatchEnabledStatus() {
        return this.cbEnableBatching.isSelected();
    }
}
