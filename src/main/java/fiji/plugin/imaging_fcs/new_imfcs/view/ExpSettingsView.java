package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.controller.MainPanelController;
import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;

import javax.swing.*;
import java.awt.*;
import java.util.function.Function;

import static fiji.plugin.imaging_fcs.new_imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.new_imfcs.view.UIUtils.createJLabel;

/**
 * Provides the user interface for adjusting and displaying experimental settings in the imaging FCS application.
 * This class extends BaseView to create a window where users can input and view settings such as pixel size,
 * magnification, numerical aperture, and more. It also displays calculated parameters based on these settings.
 */
public final class ExpSettingsView extends BaseView {
    private static final GridLayout SETTINGS_LAYOUT = new GridLayout(11, 4);
    private static final Point SETTINGS_LOCATION = new Point(
            Constants.MAIN_PANEL_POS.x + Constants.MAIN_PANEL_DIM.width + 10, 125);
    private static final Dimension SETTINGS_DIMENSION = new Dimension(370, 280);
    private final ExpSettingsModel model;
    private final MainPanelController controller;
    private JTextField tfParamAx, tfParamAy, tfParamW, tfParamZ, tfParamW2, tfParamZ2, tfParamRx, tfParamRy,
            tfPixelSize, tfMagnification, tfNA, tfEmLambda, tfEmLambda2, tfSigma, tfSigma2, tfSigmaZ, tfSigmaZ2;

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
     * Sets up the basic window properties such as size, layout, and default close operation.
     */
    @Override
    protected void configureWindow() {
        super.configureWindow();

        setLayout(SETTINGS_LAYOUT);
        setLocation(SETTINGS_LOCATION);
        setSize(SETTINGS_DIMENSION);

        setVisible(false);
    }

    /**
     * Initialize text fields with callbacks that will update the model values if modified by the user.
     */
    @Override
    protected void initializeTextFields() {
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
        tfParamAx = createTextField("", "");
        tfParamAy = createTextField("", "");
        tfParamW = createTextField("", "");
        tfParamZ = createTextField("", "");
        tfParamW2 = createTextField("", "");
        tfParamZ2 = createTextField("", "");
        tfParamRx = createTextField("", "");
        tfParamRy = createTextField("", "");
        setNonEditable();
    }

    /**
     * Sets the non-user-editable settings fields in the user interface using scientific notation.
     * This method formats several parameters from the model and updates the corresponding text fields.
     */
    public void setNonUserSettings() {
        // Use scientific notation for these fields
        Function<Double, String> formatter = value -> String.format("%6.2e", value);

        tfParamAx.setText(formatter.apply(model.getParamAx()));
        tfParamAy.setText(formatter.apply(model.getParamAy()));
        tfParamW.setText(formatter.apply(model.getParamW()));
        tfParamW2.setText(formatter.apply(model.getParamW2()));
        tfParamZ.setText(formatter.apply(model.getParamZ()));
        tfParamZ2.setText(formatter.apply(model.getParamZ2()));
        tfParamRx.setText(formatter.apply(model.getParamRx()));
        tfParamRy.setText(formatter.apply(model.getParamRy()));
    }

    /**
     * Sets certain text fields to be non-editable, indicating they are for display only.
     */
    private void setNonEditable() {
        JTextField[] nonEditFields =
                {tfParamAx, tfParamAy, tfParamW, tfParamZ, tfParamW2, tfParamZ2, tfParamRx, tfParamRy};

        for (JTextField textField : nonEditFields) {
            textField.setEditable(false);
        }
    }

    /**
     * Adds UI components to the frame, organizing them according to the layout for the settings panel.
     */
    @Override
    protected void addComponentsToFrame() {
        // row 1
        add(createJLabel("Pixel size [μm]:", ""));
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
        add(createJLabel("ax [nm]: ", ""));
        add(tfParamAx);
        add(createJLabel("ay [nm]: ", ""));
        add(tfParamAy);

        // row 9
        add(createJLabel("z [nm]: ", ""));
        add(tfParamZ);
        add(createJLabel("w [nm]: ", ""));
        add(tfParamW);

        // row 10
        add(createJLabel("z2 [nm]: ", ""));
        add(tfParamZ2);
        add(createJLabel("w2 [nm]: ", ""));
        add(tfParamW2);

        // row 11
        add(createJLabel("rx [nm]: ", ""));
        add(tfParamRx);
        add(createJLabel("ry [nm]: ", ""));
        add(tfParamRy);
    }
}
