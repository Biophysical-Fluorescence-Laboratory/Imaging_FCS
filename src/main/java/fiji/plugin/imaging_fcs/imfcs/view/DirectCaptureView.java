package fiji.plugin.imaging_fcs.imfcs.view;

import fiji.plugin.imaging_fcs.imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.imfcs.enums.CameraMode;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import static fiji.plugin.imaging_fcs.imfcs.view.ButtonFactory.createJButton;
import static fiji.plugin.imaging_fcs.imfcs.view.ButtonFactory.createJToggleButton;
import static fiji.plugin.imaging_fcs.imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.imfcs.view.UIUtils.createJLabel;

public class DirectCaptureView extends BaseView {
    private static final GridLayout DCP_LAYOUT = new GridLayout(7, 2);
    private static final Dimension DCP_DIMENSION = new Dimension(270, 200);
    private static final Point DCP_POSITION = new Point(425, 125);

    private JToggleButton tbStartStop, tbSettings, tbPixelDimension;
    private JButton btnSave, btnExit;
    private JTextField tfPixelDimension, tfExposureTime, tfTotalFrame, tfTemperature;
    private JComboBox<CameraMode> cbMode;

    public DirectCaptureView() {
        super("Direct Camera Capture");
        initializeUI();
    }

    @Override
    protected void configureWindow() {
        super.configureWindow();

        setLayout(DCP_LAYOUT);
        setSize(DCP_DIMENSION);
        setLocation(DCP_POSITION);
        setVisible(true);
    }

    @Override
    protected void initializeTextFields() {
        tfPixelDimension = createTextField("128 x 128", "");
        tfPixelDimension.setEditable(false);

        tfTotalFrame = createTextField("1000", "Set total number of frame. Only applicable in acquisition mode");
        tfTotalFrame.setEditable(false);

        tfExposureTime = createTextField("0.00106", "Set exposure time per frame. NOTE: Upon pressing 'Start' time " +
                "per frame will update accordingly depending on camera settings.");

        tfTemperature = createTextField("2", "");
        tfTemperature.setFont(tfTemperature.getFont().deriveFont(Font.BOLD, Constants.PANEL_FONT_SIZE + 2));
        tfTemperature.setEditable(false);
    }

    @Override
    protected void initializeComboBoxes() {
        cbMode = new JComboBox<>(CameraMode.values());
    }

    @Override
    protected void initializeButtons() {
        tbPixelDimension =
                createJToggleButton("Live", "Open / Close a dialog for ROI selection.", null, (ItemListener) null);
        tbStartStop = createJToggleButton("Start", "Start / Stop recording.", null, (ItemListener) null);
        tbSettings = createJToggleButton("Settings", "Open / Close a dialog with camera settings.", null,
                (ItemListener) null);
        btnSave = createJButton("Save", "Opens acquired image stack for analysis and saving.", null,
                (ActionListener) null);
        btnExit = createJButton("Exit", "Turning off camera would switch off its fan and cooler.", null,
                (ActionListener) null);
        btnExit.setForeground(Color.RED);
    }

    @Override
    protected void addComponentsToFrame() {
        // row 1
        add(tbPixelDimension);
        add(tfPixelDimension);

        // row 2
        add(createJLabel("Total Frame:", ""));
        add(tfTotalFrame);

        // row 3
        add(createJLabel("Exposure Time [s]:", ""));
        add(tfExposureTime);

        // row 4
        add(createJLabel("Mode:", ""));
        add(cbMode);

        // row 5
        add(tbStartStop);
        add(btnSave);

        // row 6
        add(tbSettings);
        add(btnExit);

        // row 7
        add(createJLabel("Temperature:", ""));
        add(tfTemperature);
    }
}
