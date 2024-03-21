package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.controller.MainPanelController;
import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;

import javax.swing.*;
import java.awt.*;

import static fiji.plugin.imaging_fcs.new_imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.new_imfcs.view.UIUtils.createJLabel;

/**
 * Provides the user interface for adjusting and displaying experimental settings in the imaging FCS application.
 * This class extends JFrame to create a window where users can input and view settings such as pixel size,
 * magnification, numerical aperture, and more. It also displays calculated parameters based on these settings.
 */
public class ExpSettingsView extends JFrame {
    private static final GridLayout SETTINGS_LAYOUT = new GridLayout(11, 4);
    private static final Point SETTINGS_LOCATION = new Point(
            Constants.MAIN_PANEL_POS.x + Constants.MAIN_PANEL_DIM.width + 10, 125);
    private static final Dimension SETTINGS_DIMENSION = new Dimension(370, 280);
    private final ExpSettingsModel model;
    private final MainPanelController controller;
    private JTextField tfParamA;
    private JTextField tfParamW;
    private JTextField tfParamZ;
    private JTextField tfParamW2;
    private JTextField tfParamZ2;
    private JTextField tfParamRx;
    private JTextField tfParamRy;
    private JTextField tfPixelSize;
    private JTextField tfMagnification;
    private JTextField tfNA;
    private JTextField tfEmLambda;
    private JTextField tfEmLambda2;
    private JTextField tfSigma;
    private JTextField tfSigma2;
    private JTextField tfSigmaZ;
    private JTextField tfSigmaZ2;

    /**
     * Constructs an ExpSettingsView with references to a controller and a model.
     * Initializes the UI components of the settings window.
     *
     * @param controller The controller that manages interactions between the view and the model.
     * @param model      The model containing the experimental settings data.
     */
    public ExpSettingsView(MainPanelController controller, ExpSettingsModel model) {
        super("Experimental Settings");
        this.model = model;
        this.controller = controller;
        initializeUI();
    }

    /**
     * Configures the main window settings, initializes text fields for user input, and adds components to the frame.
     */
    private void initializeUI() {
        configureWindow();
        initializeTextFields();
        addComponentsToFrame();
    }

    /**
     * Sets up the basic window properties such as size, layout, and default close operation.
     */
    private void configureWindow() {
        setFocusable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(SETTINGS_LAYOUT);
        setLocation(SETTINGS_LOCATION);
        setSize(SETTINGS_DIMENSION);
        setResizable(false);
    }

    private void initializeTextFields() {
        // Initialize editable fields with model data and controller actions
        tfPixelSize = createTextField(model.getPixelSize(), "", controller.updateSettings(model::setPixelSize));
        tfMagnification = createTextField(model.getMagnification(), "", controller.updateSettings(model::setMagnification));
        tfNA = createTextField(model.getNA(), "", controller.updateSettings(model::setNA));
        tfEmLambda = createTextField(model.getEmLambda(), "", controller.updateSettings(model::setEmLambda));
        tfEmLambda2 = createTextField(model.getEmLamdba2(), "", controller.updateSettings(model::setEmLamdba2));
        tfSigma = createTextField(model.getSigma(), "", controller.updateSettings(model::setSigma));
        tfSigma2 = createTextField(model.getSigma2(), "", controller.updateSettings(model::setSigma2));
        tfSigmaZ = createTextField(model.getSigmaZ(), "", controller.updateSettings(model::setSigmaZ));
        tfSigmaZ2 = createTextField(model.getSigmaZ2(), "", controller.updateSettings(model::setSigmaZ2));

        // Initialize non-editable fields for displaying calculated parameters
        tfParamA = createTextField("", "");
        tfParamW = createTextField("", "");
        tfParamZ = createTextField("", "");
        tfParamW2 = createTextField("", "");
        tfParamZ2 = createTextField("", "");
        tfParamRx = createTextField("", "");
        tfParamRy = createTextField("", "");
        setNonEditable();
    }

    /**
     * Sets certain text fields to be non-editable, indicating they are for display only.
     */
    private void setNonEditable() {
        JTextField[] nonEditFields = {tfParamA, tfParamW, tfParamZ, tfParamW2, tfParamZ2, tfParamRx, tfParamRy};

        for (JTextField textField : nonEditFields) {
            textField.setEditable(false);
        }
    }

    /**
     * Adds UI components to the frame, organizing them according to the layout for the settings panel.
     */
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
        add(createJLabel("", ""));
        add(createJLabel("", ""));
    }

    public void setTextParamA(String text) {
        tfParamA.setText(text);
    }

    public void setTextParamW(String text) {

        tfParamW.setText(text);
    }

    public void setTextParamW2(String text) {
        tfParamW2.setText(text);
    }

    public void setTextParamZ(String text) {
        tfParamZ.setText(text);
    }

    public void setTextParamZ2(String text) {
        tfParamZ2.setText(text);
    }

    public void setTextParamRx(String text) {
        tfParamRx.setText(text);
    }

    public void setTextParamRy(String text) {
        tfParamRy.setText(text);
    }
}
