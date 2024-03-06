package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;

import javax.swing.*;
import java.awt.*;

import static fiji.plugin.imaging_fcs.new_imfcs.view.UIUtils.createJLabel;

public class ExpSettingsView extends JFrame {
    private static final GridLayout SETTINGS_LAYOUT = new GridLayout(11, 4);
    private static final Point SETTINGS_LOCATION = new Point(
            Constants.MAIN_PANEL_POS.x + Constants.MAIN_PANEL_DIM.width + 10, 125);
    private static final Dimension SETTINGS_DIMENSION = new Dimension(370, 280);
    public JTextField tfParamA;
    public JTextField tfParamW;
    public JTextField tfParamZ;
    public JTextField tfParamW2;
    public JTextField tfParamZ2;
    public JTextField tfParamRx;
    public JTextField tfParamRy;
    public JTextField tfParamRz;
    private JTextField tfPixelSize;
    private JTextField tfMagnification;
    private JTextField tfNA;
    private JTextField tfEmLambda;
    private JTextField tfEmLambda2;
    private JTextField tfSigma;
    private JTextField tfSigma2;
    private JTextField tfSigmaZ;
    private JTextField tfSigmaZ2;

    public ExpSettingsView() {
        initializeUI();
    }

    private void initializeUI() {
        configureWindow();
        initializeTextFields();
        addComponentsToFrame();
    }

    private void configureWindow() {
        setFocusable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(SETTINGS_LAYOUT);
        setLocation(SETTINGS_LOCATION);
        setSize(SETTINGS_DIMENSION);
        setResizable(false);
    }

    private void initializeTextFields() {
        // TODO
        setNonEditable();
    }

    private void setNonEditable() {
        JTextField[] nonEditFields = {
                tfParamA, tfParamW, tfParamZ, tfParamW2, tfParamZ2, tfParamRx, tfParamRy, tfParamRz
        };

        for (JTextField textField : nonEditFields) {
            textField.setEditable(false);
        }
    }

    private void addComponentsToFrame() {
        // row 1
        add(createJLabel("Pixel size [um]:", ""));
        add(tfPixelSize);
        add(createJLabel("", ""));
        add(createJLabel("", ""));

        // row 2
        add(createJLabel("Magnification:", ""));
        add(tfMagnification);
        add(createJLabel("NA", ""));
        add(tfNA);

        // row 3
        add(createJLabel("λ₁ [nm]:", ""));
        add(tfEmLambda);
        add(createJLabel("λ₂ [nm]:", ""));
        add(tfEmLambda2);

        // row 4
        add(createJLabel("PSF (xy):", ""));
        add(tfSigma);
        add(createJLabel("PSF (z)", ""));
        add(tfSigmaZ);

        // row 5
        add(createJLabel("PSF2 (xy):", ""));
        add(tfSigma2);
        add(createJLabel("PSF2 (z):", ""));
        add(tfSigmaZ2);

        // row 6
        add(createJLabel("", ""));
        add(createJLabel("", ""));
        add(createJLabel("", ""));
        add(createJLabel("", ""));

        // row 7
        add(createJLabel("Resulting Fit", ""));
        add(createJLabel("Parameters", ""));
        add(createJLabel("", ""));
        add(createJLabel("", ""));

        // row 8
        add(createJLabel("a [nm]: ", ""));
        add(tfParamA);
        add(createJLabel("w [nm]: ", ""));
        add(tfParamW);

        // row 9
        add(createJLabel("z [nm]: ", ""));
        add(tfParamZ);
        add(createJLabel("w2 [nm]: ", ""));
        add(tfParamW2);

        // row 10
        add(createJLabel("z2 [nm]: ", ""));
        add(tfParamZ2);
        add(createJLabel("rx [nm]: ", ""));
        add(tfParamRx);

        // row 11
        add(createJLabel("ry [nm]: ", ""));
        add(tfParamRy);
        add(createJLabel("rz [nm]: ", ""));
        add(tfParamRz);
    }
}
