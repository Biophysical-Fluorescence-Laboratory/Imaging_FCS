package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.controller.MainPanelController;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.BleachCorrectionModel;

import javax.swing.*;
import java.awt.*;

import static fiji.plugin.imaging_fcs.new_imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.new_imfcs.view.UIUtils.createJLabel;

/**
 * Provides a graphical user interface for configuring bleach correction settings in imaging FCS.
 * This view displays controls for setting stride and the number of points in the intensity trace,
 * which are based on experimental settings loaded into the model.
 * <p>
 * This class extends {@link BaseView} and uses a grid layout to organize text fields and labels.
 */
public class BleachCorrectionView extends BaseView {
    private static final GridLayout BLEACH_CORRELATION_LAYOUT = new GridLayout(2, 2);
    private static final Point BLEACH_CORRELATION_LOCATION =
            new Point(Constants.MAIN_PANEL_POS.x + Constants.MAIN_PANEL_DIM.width + 10, 125);
    private static final Dimension BLEACH_CORRELATION_DIM = new Dimension(125, 75);
    private final MainPanelController controller;
    private final BleachCorrectionModel model;
    private JTextField tfIntAveStride, tfNumPointsIntensityTrace;

    /**
     * Constructs a new view for managing bleach correlation settings.
     *
     * @param controller the main panel controller that handles user interactions
     * @param model      the experimental settings model containing data to be displayed and edited
     */
    public BleachCorrectionView(MainPanelController controller, BleachCorrectionModel model) {
        super("Bleach correction settings.");

        this.controller = controller;
        this.model = model;

        initializeUI();
    }

    /**
     * Configures the window's properties including its layout, location, and size.
     * The window is initially set to not be visible.
     */
    @Override
    protected void configureWindow() {
        super.configureWindow();

        setLayout(BLEACH_CORRELATION_LAYOUT);
        setLocation(BLEACH_CORRELATION_LOCATION);
        setSize(BLEACH_CORRELATION_DIM);

        setVisible(false);
    }

    /**
     * Initializes text fields for entering stride and viewing the number of points in the intensity trace.
     * The stride field is editable and changes are propagated to the model.
     * The number of points field is read-only.
     */
    @Override
    protected void initializeTextFields() {
        tfIntAveStride = createTextField(model.getAverageStride(), "",
                controller.updateStrideParam(model::setAverageStride));
        tfNumPointsIntensityTrace = createTextField(model.getNumPointsIntensityTrace(), "");
        tfNumPointsIntensityTrace.setEditable(false);
    }

    /**
     * Adds components to the frame, arranging them into two rows.
     * The first row contains the label and text field for stride.
     * The second row contains the label and text field for the number of points in the intensity trace.
     */
    @Override
    protected void addComponentsToFrame() {
        // row 1
        add(createJLabel("Stride", ""));
        add(tfIntAveStride);

        // row 2
        add(createJLabel("Points", ""));
        add(tfNumPointsIntensityTrace);
    }

    /**
     * Sets the text in the number of points intensity trace text field.
     *
     * @param text the new text to display in the number of points field
     */
    public void setTextNumPointsIntensityTrace(String text) {
        tfNumPointsIntensityTrace.setText(text);
    }
}
