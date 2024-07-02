package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.controller.MainPanelController;
import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;

import javax.swing.*;
import java.awt.*;

import static fiji.plugin.imaging_fcs.new_imfcs.controller.ControllerUtils.updateComboBoxValue;
import static fiji.plugin.imaging_fcs.new_imfcs.controller.FocusListenerFactory.createFocusListener;
import static fiji.plugin.imaging_fcs.new_imfcs.view.ButtonFactory.createJButton;
import static fiji.plugin.imaging_fcs.new_imfcs.view.ButtonFactory.createJToggleButton;
import static fiji.plugin.imaging_fcs.new_imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.new_imfcs.view.UIUtils.createJLabel;
import static fiji.plugin.imaging_fcs.version.VERSION.IMFCS_VERSION;

/**
 * The MainPanelView class defines the GUI for the Imaging FCS plugin in Fiji.
 * It extends BaseView and uses a MainPanelController to handle events.
 */
public final class MainPanelView extends BaseView {
    // Constants for UI design
    private static final GridLayout PANEL_LAYOUT = new GridLayout(14, 4);

    // Colors for buttons
    private static final Color SAVE_BUTTON_COLOR = Color.BLUE;
    private static final Color LOAD_BUTTON_COLOR = Color.BLUE;
    private static final Color EXIT_BUTTON_COLOR = Color.RED;

    // Controller for handling user actions
    private final MainPanelController controller;

    // Settings model for default values
    private final ExpSettingsModel expSettingsModel;

    // Text Fields for user input
    private JTextField tfFirstFrame, tfLastFrame, tfFrameTime, tfBinning, tfCCFDistance, tfCorrelatorQ;

    // Combo boxes for selecting options
    private JComboBox<String> cbCorrelatorP, cbBleachCor, cbFilter, cbParaCor, cbDCCF, cbFitModel;

    // Buttons
    private JButton btnSave, btnRead, btnExit, btnLoad, btnBatch, btnDCCF, btnWriteConfig, btnUseExisting, btnDCR,
            btnParamVideo, btnDebug, btnOptions, btnAve, btnParaCor, btnPSF, btnAll, btnRT, btnROI, btnBtf;
    private JToggleButton tbExpSettings, tbFCCSDisplay, tbFit, tbOverlap, tbBackground, tbNB, tbFiltering,
            tbBleachCorStride, tbDL, tbSim, tbMSD;

    /**
     * Constructs the MainPanelView with a specified controller.
     *
     * @param controller The controller to handle actions performed on this panel.
     */
    public MainPanelView(MainPanelController controller, ExpSettingsModel expSettingsModel) {
        super("ImagingFCS " + IMFCS_VERSION); // items for ImFCS control panel;
        this.controller = controller;
        this.expSettingsModel = expSettingsModel;
        initializeUI();
    }

    /**
     * Configures basic window properties.
     */
    @Override
    protected void configureWindow() {
        super.configureWindow();

        setLayout(PANEL_LAYOUT);
        setLocation(Constants.MAIN_PANEL_POS);
        setSize(Constants.MAIN_PANEL_DIM);

        setVisible(true);
    }

    /**
     * Initializes text fields with default values and adds document listeners where applicable.
     * This method sets up the text fields used for user input in the main panel, including setting
     * initial values and tooltips to guide the user. Listeners are attached to fields to handle
     * changes in input, triggering appropriate actions in the application.
     */
    @Override
    protected void initializeTextFields() {
        tfFirstFrame =
                createTextField(expSettingsModel.getFirstFrame(), "",
                        controller.updateStrideParam(expSettingsModel::setFirstFrame));

        tfFrameTime = createTextField(expSettingsModel.getFrameTime(),
                "Time per this. NOTE: Changing this value will " +
                        "reinitialize all arrays.", createFocusListener(expSettingsModel::setFrameTime));

        tfLastFrame =
                createTextField(expSettingsModel.getLastFrame(), "",
                        controller.updateStrideParam(expSettingsModel::setLastFrame));

        tfBinning = createTextField(expSettingsModel.getBinningString(),
                "Pixel binning used in the evaluations. NOTE: " +
                        "Changing this value will reinitialize all arrays.",
                controller.updateSettings(expSettingsModel::setBinning));

        tfCCFDistance = createTextField(expSettingsModel.getCCFString(), "Distance in x- and y-direction for spatial " +
                "cross-correlation. NOTE: Changing this value will reinitialize all arrays.",
                controller.updateSettings(expSettingsModel::setCCF));

        tfCorrelatorQ =
                createTextField(expSettingsModel.getCorrelatorQ(), "",
                        createFocusListener(controller.updateFitEnd(expSettingsModel::setCorrelatorQ)));
    }

    /**
     * Initializes combo boxes with options for various parameters and settings.
     * This method sets up combo boxes used for selecting options in the main panel. It also
     * attaches action listeners to some combo boxes to handle user selections.
     */
    @Override
    protected void initializeComboBoxes() {
        cbFitModel = new JComboBox<>(new String[]{Constants.ITIR_FCS_2D, Constants.SPIM_FCS_3D, Constants.DC_FCCS_2D});
        cbCorrelatorP = new JComboBox<>(new String[]{"16", "32"});
        cbFilter = new JComboBox<>(new String[]{"none", Constants.FILTER_INTENSITY, Constants.FILTER_MEAN});

        cbBleachCor = new JComboBox<>(new String[]{
                "none",
                Constants.BLEACH_CORRECTION_SLIDING_WINDOW,
                Constants.BLEACH_CORRECTION_SINGLE_EXP,
                Constants.BLEACH_CORRECTION_DOUBLE_EXP,
                Constants.BLEACH_CORRECTION_POLYNOMIAL,
                Constants.BLEACH_CORRECTION_LINEAR_SEGMENT
        });

        cbParaCor = new JComboBox<>(new String[]{
                "N vs D",
                "N vs F2",
                "D vs F2",
                "N*(1-F2) vs D",
                "N*F2 vs D2",
                "D vs Sqrt(vx^2+vy^2)",
                "D2 vs Sqrt(vx^2+vy^2)"
        });

        cbDCCF = new JComboBox<>(new String[]{"x direction", "y direction", "diagonal /", "diagonal \\"});

        // add listeners
        cbFitModel.addActionListener(controller.cbFitModelChanged());
        cbCorrelatorP.addActionListener(updateComboBoxValue(controller.updateFitEnd(expSettingsModel::setCorrelatorP)));
        cbParaCor.addActionListener(updateComboBoxValue(expSettingsModel::setParaCor));
        cbBleachCor.addActionListener(controller.cbBleachCorChanged());
        cbFilter.addActionListener(controller.cbFilterChanged());
    }

    @Override
    protected void initializeButtons() {
        createJButtons();
        createJToggleButtons();
    }

    private void createJButtons() {
        // create the IO colored buttons
        btnSave = createJButton("Save", "Save the evaluation of the data as binary files. Which data to save can be " +
                "selected in a dialog.", null, controller.btnSavePressed());
        btnSave.setForeground(SAVE_BUTTON_COLOR);

        btnRead = createJButton("Read", "Load a previously saved experiment. Note that the original image is not " +
                "automatically loaded along.", null, controller.btnLoadPressed());
        btnRead.setForeground(LOAD_BUTTON_COLOR);

        btnExit = createJButton("Exit", "", null, controller.btnExitPressed());
        btnExit.setForeground(EXIT_BUTTON_COLOR);

        // Buttons
        btnUseExisting =
                createJButton("Use", "Uses the active existing image in ImageJ.", null,
                        controller.btnUseExistingPressed());
        btnLoad = createJButton("Load", "Opens a dialog to open a new image.", null, controller.btnLoadNewPressed());
        btnBatch =
                createJButton("Batch", "Allow to select a list of evaluations to be performed on a range of images.",
                        null, controller.btnBatchPressed());
        btnWriteConfig = createJButton("Write Conf",
                "Writes a configuration file int user.home that will be read at next " +
                        "ImFCS start", new Font(Constants.PANEL_FONT, Font.BOLD, 11),
                controller.btnWriteConfigPressed());
        btnDCR =
                createJButton("LiveReadout", "", new Font(Constants.PANEL_FONT, Font.BOLD, 10),
                        controller.btnDCRPressed());
        btnParamVideo =
                createJButton("PVideo", "Creates videos of parameter maps", null, controller.btnParamVideoPressed());
        btnDebug = createJButton("", "", null, controller.btnDebugPressed());
        btnOptions =
                createJButton("Options", "Select various options regarding the display of results.", null,
                        controller.btnOptionsPressed());
        btnAve = createJButton("Average", "Calculate the average ACF from all valid ACFs and fit if fit is switched " +
                "on; this does not calculate residuals or sd.", null, controller.btnAvePressed());
        btnParaCor = createJButton("Scatter",
                "Calculates a scatter plot for a pair of two parameters from the scroll down" +
                        " menu.", null, controller.btnParaCorPressed());
        btnDCCF = createJButton("dCCF", "Create a dCCF image to see differences between forward and backward " +
                "correlation in a direction (see scroll down menu).", null, controller.btnDCCFPressed());
        btnPSF = createJButton("PSF", "Calculates the calibration for the PSF.", null, controller.btnPSFPressed());
        btnAll = createJButton("All", "Calculates all ACFs.", null, controller.btnAllPressed());
        btnRT = createJButton("Res. Table", "Create a results table.", null, controller.btnRTPressed());
        btnROI =
                createJButton("ROI", "Calculates ACFs only in the currently chose ROI.", null,
                        controller.btnROIPressed());
        btnBtf =
                createJButton("To Front", "Bring all windows of this plugin instance to the front.", null,
                        controller.btnBtfPressed());
    }

    private void createJToggleButtons() {
        tbExpSettings =
                createJToggleButton("Exp Set", "Opens a dialog with experimental settings.", null,
                        controller.tbExpSettingsPressed());
        tbFCCSDisplay =
                createJToggleButton("FCCS Disp Off", "", new Font(Constants.PANEL_FONT, Font.BOLD, 9),
                        controller.tbFCCSDisplayPressed());
        tbOverlap =
                createJToggleButton("Overlap Off", "", new Font(Constants.PANEL_FONT, Font.BOLD, 11),
                        controller.tbOverlapPressed());
        tbBackground =
                createJToggleButton("Background", "Panel for different methods to perform background subtraction.",
                        new Font(Constants.PANEL_FONT, Font.BOLD, 10), controller.tbBackgroundPressed());
        tbNB = createJToggleButton("N&B Off", "", null, controller.tbNBPressed());
        tbFiltering = createJToggleButton("Threshold", "Filters the values in parameters maps using user-defined " +
                "thresholds", null, controller.tbFilteringPressed());
        tbBleachCorStride = createJToggleButton("Bleach Cor",
                "Set number of intensity points to be averaged before bleach " +
                        "correction is performed.", null, controller.tbBleachCorStridePressed());
        tbDL = createJToggleButton("Diff. Law", "Calculates the Diffusion Law.", null, controller.tbDLPressed());
        tbFit =
                createJToggleButton("Fit Off", "Switches Fit on/off; opens/closes Fit panel.", null,
                        controller.tbFitPressed());
        tbSim = createJToggleButton("Sim Off", "Opens/closes Simulation panel.", null, controller.tbSimPressed());
        tbMSD =
                createJToggleButton("MSD Off", "Switches Mean Square Displacement calculation and plot on/off.", null
                        , controller.tbMSDPressed());
    }

    /**
     * Adds components to the frame, organizing them according to the specified layout.
     * This method meticulously places labels, text fields, combo boxes, and buttons into the
     * frame, utilizing a grid layout. It also configures action listeners for buttons to
     * interact with the controller for executing actions based on user input.
     */
    @Override
    protected void addComponentsToFrame() {
        // row 1
        add(createJLabel("Image", ""));
        add(btnUseExisting);
        add(btnLoad);
        add(btnBatch);

        // row 2
        add(createJLabel("First frame: ", ""));
        add(tfFirstFrame);
        add(createJLabel("Last frame: ", ""));
        add(tfLastFrame);

        // row 3
        add(createJLabel("Frame time: ", ""));
        add(tfFrameTime);
        add(tbExpSettings);
        add(btnWriteConfig);

        // row 4
        add(createJLabel("CCF distance: ", ""));
        add(tfCCFDistance);
        add(createJLabel("Binning", ""));
        add(tfBinning);

        // row 5
        add(createJLabel("Correlator P: ", ""));
        add(cbCorrelatorP);
        add(createJLabel("Correlator Q: ", ""));
        add(tfCorrelatorQ);

        // row 6
        add(createJLabel("Fit Model: ", ""));
        add(cbFitModel);
        add(tbFCCSDisplay);
        add(tbOverlap);

        // row 7
        add(createJLabel("", ""));
        add(createJLabel("", ""));
        add(createJLabel("", ""));
        add(createJLabel("", ""));

        // row 8
        add(btnDCR);
        add(btnParamVideo);
        add(tbBackground);
        add(btnDebug);

        // row 9
        add(btnOptions);
        add(tbNB);
        add(tbFiltering);
        add(btnAve);

        // row 10
        add(btnParaCor);
        add(cbParaCor);
        add(tbBleachCorStride);
        add(cbBleachCor);

        // row 11
        add(btnDCCF);
        add(cbDCCF);
        add(createJLabel("Filter (All):", ""));
        add(cbFilter);

        // row 12
        add(btnPSF);
        add(tbDL);
        add(tbFit);
        add(btnAll);

        // row 13
        add(tbSim);
        add(btnRT);
        add(tbMSD);
        add(btnROI);

        // row 14
        add(btnBtf);
        add(btnSave);
        add(btnRead);
        add(btnExit);
    }

    /**
     * Update tfLastFrame with the text given.
     *
     * @param text The text used to update the last frame text field
     */
    public void setTfLastFrame(String text) {
        tfLastFrame.setText(text);
    }
}
