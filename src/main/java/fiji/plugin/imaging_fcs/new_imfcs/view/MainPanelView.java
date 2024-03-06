package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.controller.MainPanelController;
import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import ij.IJ;

import javax.swing.*;
import java.awt.*;

import static fiji.plugin.imaging_fcs.new_imfcs.controller.FocusListenerFactory.createFocusListener;
import static fiji.plugin.imaging_fcs.new_imfcs.view.ButtonFactory.createJButton;
import static fiji.plugin.imaging_fcs.new_imfcs.view.ButtonFactory.createJToggleButton;
import static fiji.plugin.imaging_fcs.new_imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.version.VERSION.IMFCS_VERSION;

/**
 * The MainPanelView class defines the GUI for the Imaging FCS plugin in Fiji.
 * It extends JFrame and uses a MainPanelController to handle events.
 */
public class MainPanelView extends JFrame {
    // Constants for UI design
    private static final GridLayout PANEL_LAYOUT = new GridLayout(14, 4);
    private static final String CORREL_Q = "8";

    // Colors for buttons
    private static final Color SAVE_BUTTON_COLOR = Color.BLUE;
    private static final Color LOAD_BUTTON_COLOR = Color.BLUE;
    private static final Color EXIT_BUTTON_COLOR = Color.RED;

    // Controller for handling user actions
    private final MainPanelController controller;

    // Settings model for default values
    private final ExpSettingsModel expSettingsModel;

    // Text Fields for user input
    public JTextField tfFirstFrame; // a detailed description is given in the accompanying documentation
    public JTextField tfLastFrame;
    public JTextField tfFrameTime;
    public JTextField tfBinning;
    public JTextField tfCCFDistance;
    public JTextField tfCorrelatorQ;

    // Combo boxes for selecting options
    public JComboBox<String> cbCorrelatorP;
    public JComboBox<String> cbBleachCor;
    public JComboBox<String> cbFilter;
    public JComboBox<String> cbParaCor;
    public JComboBox<String> cbDCCF;
    public JComboBox<String> cbFitModel;

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
     * Initializes the user interface components and layout.
     */
    private void initializeUI() {
        configureWindow();
        initializeTextFields();
        initializeComboBoxes();

        try {
            addComponentsToFrame();
        } catch (Exception e) {
            IJ.log(e.getMessage());
        }

        setVisible(true);
    }

    /**
     * Configures basic window properties.
     */
    private void configureWindow() {
        setFocusable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(PANEL_LAYOUT);
        setLocation(Constants.MAIN_PANEL_POS);
        setSize(Constants.MAIN_PANEL_DIM);
        setResizable(false);
    }

    /**
     * Initializes text fields with default values and adds document listeners where applicable.
     * This method sets up the text fields used for user input in the main panel, including setting
     * initial values and tooltips to guide the user. Listeners are attached to fields to handle
     * changes in input, triggering appropriate actions in the application.
     */
    private void initializeTextFields() {
        tfFirstFrame = createTextField("1", "", controller.tfFirstFrameChanged());

        tfFrameTime = createTextField(
                "0.001", "Time per this. NOTE: Changing this value will reinitialize all arrays.");

        tfLastFrame = createTextField("0", "", controller.tfLastFrameChanged());

        tfBinning = createTextField(expSettingsModel.getBinningString(),
                "Pixel binning used in the evaluations. NOTE: Changing this value will reinitialize all arrays.",
                createFocusListener(expSettingsModel::setBinning)
        );

        tfCCFDistance = createTextField(expSettingsModel.getCCFString(),
                "Distance in x- and y-direction for spatial cross-correlation. NOTE: Changing this value will reinitialize all arrays.",
                createFocusListener(expSettingsModel::setCCF));

        tfCorrelatorQ = createTextField(CORREL_Q, "");
    }

    /**
     * Initializes combo boxes with options for various parameters and settings.
     * This method sets up combo boxes used for selecting options in the main panel. It also
     * attaches action listeners to some combo boxes to handle user selections.
     */
    private void initializeComboBoxes() {
        cbFitModel = new JComboBox<>(new String[]{"ITIR-FCS (2D)", "SPIM-FCS (3D)", "DC-FCCS (2D)"});
        cbCorrelatorP = new JComboBox<>(new String[]{"16", "32"});
        cbFilter = new JComboBox<>(new String[]{"none", "Intensity", "Mean"});

        cbBleachCor = new JComboBox<>(
                new String[]{"none", "Sliding Window", "Single Exp", "Double Exp", "Polynomial", "Lin Segment"});

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
        cbBleachCor.addActionListener(controller.cbBleachCorChanged());
        cbFilter.addActionListener(controller.cbFilterChanged());
    }

    /**
     * Adds components to the frame, organizing them according to the specified layout.
     * This method meticulously places labels, text fields, combo boxes, and buttons into the
     * frame, utilizing a grid layout. It also configures action listeners for buttons to
     * interact with the controller for executing actions based on user input.
     *
     * @throws Exception if there is an error adding components to the frame.
     */
    private void addComponentsToFrame() throws Exception {
        // create the IO colored buttons
        JButton btnSave = createJButton("Save",
                "Save the evaluation of the data as binary files. Which data to save can be selected in a dialog.",
                null, controller.btnSavePressed());
        btnSave.setForeground(SAVE_BUTTON_COLOR);

        JButton btnRead = createJButton("Read",
                "Load a previously saved experiment. Note that the original image is not automatically loaded along.",
                null, controller.btnLoadPressed());
        btnRead.setForeground(LOAD_BUTTON_COLOR);

        JButton btnExit = createJButton("Exit", "", null, controller.btnExitPressed());
        btnExit.setForeground(EXIT_BUTTON_COLOR);

        // row 1
        add(new JLabel("Image"));
        add(createJButton("Use", "Uses the active existing image in ImageJ.", null,
                controller.btnUseExistingPressed()));
        add(createJButton("Load", "Opens a dialog to open a new image.", null, controller.btnLoadNewPressed()));
        add(createJButton("Batch",
                "Allow to select a list of evaluations to be performed on a range of images.", null,
                controller.btnBatchPressed()));

        // row 2
        add(new JLabel("First frame: "));
        add(tfFirstFrame);
        add(new JLabel("Last frame: "));
        add(tfLastFrame);

        // row 3
        add(new JLabel("Frame time: "));
        add(tfFrameTime);
        add(createJToggleButton("Exp Set", "Opens a dialog with experimental settings.", null,
                controller.tbExpSettingsPressed()));
        add(createJButton("Write Conf",
                "Writes a configuration file int user.home that will be read at next ImFCS start",
                new Font(Constants.PANEL_FONT, Font.BOLD, 11), controller.btnWriteConfigPressed()));

        // row 4
        add(new JLabel("CCF distance: "));
        add(tfCCFDistance);
        add(new JLabel("Binning"));
        add(tfBinning);

        // row 5
        add(new JLabel("Correlator P: "));
        add(cbCorrelatorP);
        add(new JLabel("Correlator Q: "));
        add(tfCorrelatorQ);

        // row 6
        add(new JLabel("Fit Model: "));
        add(cbFitModel);
        add(createJToggleButton("FCCS Disp Off", "", new Font(Constants.PANEL_FONT, Font.BOLD, 9),
                controller.tbFCCSDisplayPressed()));
        add(createJToggleButton("Overlap Off", "", new Font(Constants.PANEL_FONT, Font.BOLD, 11),
                controller.tbOverlapPressed()));

        // row 7
        add(new JLabel(""));
        add(new JLabel(""));
        add(new JLabel(""));
        add(new JLabel(""));

        // row 8
        add(createJButton("LiveReadout", "", new Font(Constants.PANEL_FONT, Font.BOLD, 10), controller.btnDCRPressed()));
        add(createJButton("PVideo", "Creates videos of parameter maps", null, controller.btnParamVideoPressed()));
        add(createJToggleButton("Background", "Panel for different methods to perform background subtraction.",
                new Font(Constants.PANEL_FONT, Font.BOLD, 10), controller.tbBackgroundPressed()));
        add(createJButton("", "", null, controller.btnDebugPressed()));

        // row 9
        add(createJButton("Options", "Select various options regarding the display of results.", null,
                controller.btnOptionsPressed()));
        add(createJToggleButton("N&B Off", "", null, controller.tbNBPressed()));
        add(createJToggleButton("Threshold",
                "Filters the values in parameters maps using user-defined thresholds", null,
                controller.tbFilteringPressed()));
        add(createJButton("Average",
                "Calculate the average ACF from all valid ACFs and fit if fit is switched on; this does not calculate residuals or sd.",
                null, controller.btnAvePressed()));

        // row 10
        add(createJButton("Scatter",
                "Calculates a scatter plot for a pair of two parameters from the scroll down menu.", null,
                controller.btnParaCorPressed()));
        add(cbParaCor);
        add(createJToggleButton("Bleach Cor",
                "Set number of intensity points to be averaged before bleach correction is performed.", null,
                controller.tbBleachCorStridePressed()));
        add(cbBleachCor);

        // row 11
        add(createJButton("dCCF",
                "Create a dCCF image to see differences between forward and backward correlation in a direction (see scroll down menu).",
                null, controller.btnDCCFPressed()));
        add(cbDCCF);
        add(new JLabel("Filter (All):"));
        add(cbFilter);

        // row 12
        add(createJButton("PSF", "Calculates the calibration for the PSF.", null, controller.btnPSFPressed()));
        add(createJToggleButton("Diff. Law", "Calculates the Diffusion Law.", null, controller.tbDLPressed()));
        add(createJToggleButton("Fit off", "Switches Fit on/off; opens/closes Fit panel.", null,
                controller.tbFitPressed()));
        add(createJButton("All", "Calculates all ACFs.", null, controller.btnAllPressed()));

        // row 13
        add(createJToggleButton("Sim off", "Opens/closes Simulation panel.", null, controller.tbSimPressed()));
        add(createJButton("Res. Table", "Create a results table.", null, controller.btnRTPressed()));
        add(createJToggleButton("MSD Off", "Switches Mean Square Displacement calculation and plot on/off.",
                null, controller.tbMSDPressed()));
        add(createJButton("ROI", "Calculates ACFs only in the currently chose ROI.", null,
                controller.btnROIPressed()));

        // row 14
        add(createJButton("To Front", "Bring all windows of this plugin instance to the front.", null,
                controller.btnBtfPressed()));
        add(btnSave);
        add(btnRead);
        add(btnExit);
    }
}
