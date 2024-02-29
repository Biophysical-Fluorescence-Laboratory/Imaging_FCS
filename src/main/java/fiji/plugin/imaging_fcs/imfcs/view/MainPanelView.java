package fiji.plugin.imaging_fcs.imfcs.view;

import fiji.plugin.imaging_fcs.imfcs.controller.listeners.MainPanelController;
import ij.IJ;

import javax.swing.*;
import javax.swing.event.DocumentListener;
import java.awt.*;

import static fiji.plugin.imaging_fcs.version.VERSION.IMFCS_VERSION;

/**
 * The MainPanelView class defines the GUI for the Imaging FCS plugin in Fiji.
 * It extends JFrame and uses a MainPanelController to handle events.
 */
public class MainPanelView extends JFrame {
    // Constants for UI design
    private static final String PANEL_FONT = "SansSerif";
    private static final Dimension PANEL_DIM = new Dimension(410, 370);
    private static final Point PANEL_POS = new Point(10, 125);
    private static final GridLayout PANEL_LAYOUT = new GridLayout(14, 4);
    private static final String CORREL_Q = "8";
    private static final int TEXT_FIELD_COLUMNS = 8;

    // Colors for buttons
    private static final Color SAVE_BUTTON_COLOR = Color.BLUE;
    private static final Color LOAD_BUTTON_COLOR = Color.BLUE;
    private static final Color EXIT_BUTTON_COLOR = Color.RED;

    // controller for handling user actions
    private final MainPanelController controller;

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
    public MainPanelView(MainPanelController controller) {
        super("ImFCS " + IMFCS_VERSION); // items for ImFCS control panel;
        this.controller = controller;
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
        setLocation(PANEL_POS);
        setSize(PANEL_DIM);
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
                "0.001", "Time per this. NOTE: Changing this value will reinitialize all arrays.", null);

        tfLastFrame = createTextField("0", "", controller.tfLastFrameChanged());

        tfBinning = createTextField("1 x 1",
                "Pixel binning used in the evaluations. NOTE: Changing this value will reinitialize all arrays.",
                controller.expSettingsChanged());

        tfCCFDistance = createTextField("0 x 0",
                "Distance in x- and y-direction for spatial cross-correlation. NOTE: Changing this value will reinitialize all arrays.",
                controller.expSettingsChanged());

        tfCorrelatorQ = createTextField(CORREL_Q, "", null);
    }

    /**
     * Creates a text field with specified initial text, tooltip, and document listener.
     *
     * @param text     The initial text for the text field.
     * @param toolTip  The tooltip to display when hovering over the text field.
     * @param listener The document listener to attach to the text field, or null if no listener is needed.
     * @return A new JTextField instance configured with the specified parameters.
     */
    private JTextField createTextField(String text, String toolTip, DocumentListener listener) {
        JTextField textField = new JTextField(text, TEXT_FIELD_COLUMNS);
        if (!toolTip.isEmpty()) {
            textField.setToolTipText(toolTip);
        }

        if (listener != null) {
            textField.getDocument().addDocumentListener(listener);
        }

        return textField;
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
        // create the button factory
        ButtonFactory btnFact = new ButtonFactory();

        // create the IO colored buttons
        JButton btnSave = btnFact.createJButton("Save",
                "Save the evaluation of the data as binary files. Which data to save can be selected in a dialog.",
                null, controller.btnSavePressed());
        btnSave.setForeground(SAVE_BUTTON_COLOR);

        JButton btnRead = btnFact.createJButton("Read",
                "Load a previously saved experiment. Note that the original image is not automatically loaded along.",
                null, controller.btnLoadPressed());
        btnRead.setForeground(LOAD_BUTTON_COLOR);

        JButton btnExit = btnFact.createJButton("Exit", "", null, controller.btnExitPressed());
        btnExit.setForeground(EXIT_BUTTON_COLOR);

        // row 1
        add(new JLabel("Image"));
        add(btnFact.createJButton("Use", "Uses the active existing image in ImageJ.", null,
                controller.btnUseExistingPressed()));
        add(btnFact.createJButton("Load", "Opens a dialog to open a new image.", null, controller.btnLoadNewPressed()));
        add(btnFact.createJButton("Batch",
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
        add(btnFact.createJToggleButton("Exp Set", "Opens a dialog with experimental settings.", null,
                controller.tbExpSettingsPressed()));
        add(btnFact.createJButton("Write Conf",
                "Writes a configuration file int user.home that will be read at next ImFCS start",
                new Font(PANEL_FONT, Font.BOLD, 11), controller.btnWriteConfigPressed()));

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
        add(btnFact.createJToggleButton("FCCS Disp Off", "", new Font(PANEL_FONT, Font.BOLD, 9),
                controller.tbFCCSDisplayPressed()));
        add(btnFact.createJToggleButton("Overlap Off", "", new Font(PANEL_FONT, Font.BOLD, 11),
                controller.tbOverlapPressed()));

        // row 7
        add(new JLabel(""));
        add(new JLabel(""));
        add(new JLabel(""));
        add(new JLabel(""));

        // row 8
        add(btnFact.createJButton("LiveReadout", "", new Font(PANEL_FONT, Font.BOLD, 10), controller.btnDCRPressed()));
        add(btnFact.createJButton("PVideo", "Creates videos of parameter maps", null, controller.btnParamVideoPressed()));
        add(btnFact.createJToggleButton("Background", "Panel for different methods to perform background subtraction.",
                new Font(PANEL_FONT, Font.BOLD, 10), controller.tbBackgroundPressed()));
        add(btnFact.createJButton("", "", null, controller.btnDebugPressed()));

        // row 9
        add(btnFact.createJButton("Options", "Select various options regarding the display of results.", null,
                controller.btnOptionsPressed()));
        add(btnFact.createJToggleButton("N&B Off", "", null, controller.tbNBPressed()));
        add(btnFact.createJToggleButton("Threshold",
                "Filters the values in parameters maps using user-defined thresholds", null,
                controller.tbFilteringPressed()));
        add(btnFact.createJButton("Average",
                "Calculate the average ACF from all valid ACFs and fit if fit is switched on; this does not calculate residuals or sd.",
                null, controller.btnAvePressed()));

        // row 10
        add(btnFact.createJButton("Scatter",
                "Calculates a scatter plot for a pair of two parameters from the scroll down menu.", null,
                controller.btnParaCorPressed()));
        add(cbParaCor);
        add(btnFact.createJToggleButton("Bleach Cor",
                "Set number of intensity points to be averaged before bleach correction is performed.", null,
                controller.tbBleachCorStridePressed()));
        add(cbBleachCor);

        // row 11
        add(btnFact.createJButton("dCCF",
                "Create a dCCF image to see differences between forward and backward correlation in a direction (see scroll down menu).",
                null, controller.btnDCCFPressed()));
        add(cbDCCF);
        add(new JLabel("Filter (All):"));
        add(cbFilter);

        // row 12
        add(btnFact.createJButton("PSF", "Calculates the calibration for the PSF.", null, controller.btnPSFPressed()));
        add(btnFact.createJToggleButton("Diff. Law", "Calculates the Diffusion Law.", null, controller.tbDLPressed()));
        add(btnFact.createJToggleButton("Fit off", "Switches Fit on/off; opens/closes Fit panel.", null,
                controller.tbFitPressed()));
        add(btnFact.createJButton("All", "Calculates all ACFs.", null, controller.btnAllPressed()));

        // row 13
        add(btnFact.createJToggleButton("Sim off", "Opens/closes Simulation panel.", null, controller.tbSimPressed()));
        add(btnFact.createJButton("Res. Table", "Create a results table.", null, controller.btnRTPressed()));
        add(btnFact.createJToggleButton("MSD Off", "Switches Mean Square Displacement calculation and plot on/off.",
                null, controller.tbMSDPressed()));
        add(btnFact.createJButton("ROI", "Calculates ACFs only in the currently chose ROI.", null,
                controller.btnROIPressed()));

        // row 14
        add(btnFact.createJButton("To Front", "Bring all windows of this plugin instance to the front.", null,
                controller.btnBtfPressed()));
        add(btnSave);
        add(btnRead);
        add(btnExit);
    }
}
