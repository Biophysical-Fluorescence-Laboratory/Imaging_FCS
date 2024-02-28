package fiji.plugin.imaging_fcs.imfcs.view;

import fiji.plugin.imaging_fcs.imfcs.controller.action_listeners.MainPanelController;

import javax.swing.*;
import java.awt.*;

import static fiji.plugin.imaging_fcs.version.VERSION.IMFCS_VERSION;

public class MainPanelView extends JFrame {
    // Constants
    private static final String PANEL_FONT = "SansSerif";
    private static final int PANEL_FONT_SIZE = 12;
    private static final Dimension PANEL_DIM = new Dimension(410, 370);
    private static final Point PANEL_POS = new Point(10, 125);
    private static final GridLayout PANEL_LAYOUT = new GridLayout(14, 4);
    private static final String CORREL_Q = "8";
    private static final String PIXEL_SIZE = "24";
    private static final String MAGNIFICATION = "100";
    private static final String NA = "1.49";
    private static final String WAVELENGTH_1 = "515";
    private static final String WAVELENGTH_2 = "600";
    private static final String LATERAL_PSF_1 = "0.8";
    private static final String LATERAL_PSF_2 = "0.8";
    private static final String AXIAL_PSF_1 = "1000000";
    private static final String AXIAL_PSF_2 = "1000000";

    // Colors
    private static final Color SAVE_BUTTON_COLOR = Color.BLUE;
    private static final Color LOAD_BUTTON_COLOR = Color.BLUE;
    private static final Color EXIT_BUTTON_COLOR = Color.RED;
    private final int frames = 0;
    private final MainPanelController controller;
    // Buttons
    private JTextField tfFirstFrame; // a detailed description is given in the accompanying documentation
    private JTextField tfLastFrame;
    private JTextField tfFrameTime;
    private JTextField tfBinning;
    private JTextField tfCCFDistance;
    private JTextField tfCorrelatorQ;
    private JTextField tfPixelSize;
    private JTextField tfMagnification;
    private JTextField tfNA;
    private JTextField tfEmLambda;
    private JTextField tfEmLambda2;
    private JTextField tfSigma;
    private JTextField tfSigmaZ;
    private JTextField tfSigma2;
    private JTextField tfSigmaZ2;
    private JTextField tfIntAveStride;
    private JTextField tfNopit;
    private JComboBox<String> cbCorrelatorP;
    private JComboBox<String> cbBleachCor;
    private JComboBox<String> cbFilter;
    private JComboBox<String> cbParaCor;
    private JComboBox<String> cbDCCF;
    private JComboBox<String> cbFitModel;
    // Background Subtraction Class, Bleach Correction Class
    private Imaging_FCS.JBackgroundSubtractionComponent JBackgroundSubtractionComponentObj;
    private JFrame frame;

    public MainPanelView() {
        super("ImFCS " + IMFCS_VERSION); // items for ImFCS control panel;
        controller = new MainPanelController(this);
        initializeUI();
    }

    private void initializeUI() {
        setFocusable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(PANEL_LAYOUT);
        setLocation(PANEL_POS);
        setSize(PANEL_DIM);
        setResizable(false);

        try {
            addComponentsToFrame();
        } catch (Exception e) {
            // TODO: log in ImageJ
        }
        setVisible(true);
    }

    private void addComponentsToFrame() throws Exception {
        // create the button factory
        ButtonFactory btnFact = new ButtonFactory();

        // create the IO colored buttons
        JButton btnSave = btnFact.createJButton("Save", "Save the evaluation of the data as binary files. Which data to save can be selected in a dialog.", null, controller.btnSavePressed);
        btnSave.setForeground(SAVE_BUTTON_COLOR);

        JButton btnRead = btnFact.createJButton("Read", "Load a previously saved experiment. Note that the original image is not automatically loaded along.", null, controller.btnLoadPressed);
        btnRead.setForeground(LOAD_BUTTON_COLOR);

        JButton btnExit = btnFact.createJButton("Exit", "", null, controller.btnExitPressed);
        btnExit.setForeground(EXIT_BUTTON_COLOR);

        tfFirstFrame = new JTextField("1", 8);
        tfFrameTime = new JTextField("0.001", 8);
        tfLastFrame = new JTextField(Integer.toString(frames), 8);
        tfBinning = new JTextField("1 x 1", 8);
        tfCCFDistance = new JTextField("0 x 0", 8);
        tfPixelSize = new JTextField(PIXEL_SIZE, 8);
        tfMagnification = new JTextField(MAGNIFICATION, 8);
        tfNA = new JTextField(NA, 8);
        tfEmLambda = new JTextField(WAVELENGTH_1, 8);
        tfEmLambda2 = new JTextField(WAVELENGTH_2, 8);
        tfSigma = new JTextField(LATERAL_PSF_1, 8);
        tfSigmaZ = new JTextField(AXIAL_PSF_1, 8);
        tfSigma2 = new JTextField(LATERAL_PSF_2, 8);
        tfSigmaZ2 = new JTextField(AXIAL_PSF_2, 8);
        tfCorrelatorQ = new JTextField(CORREL_Q, 8);

        cbFitModel = new JComboBox<>(new String[]{"ITIR-FCS (2D)", "SPIM-FCS (3D)", "DC-FCCS (2D)"});
        cbCorrelatorP = new JComboBox<>(new String[]{"16", "32"});
        cbBleachCor = new JComboBox<>(new String[]{"none", "Sliding Window", "Single Exp", "Double Exp", "Polynomial", "Lin Segment"});
        cbFilter = new JComboBox<>(new String[]{"none", "Intensity", "Mean"});
        cbParaCor = new JComboBox<>(new String[]{"N vs D", "N vs F2", "D vs F2", "N*(1-F2) vs D", "N*F2 vs D2", "D vs Sqrt(vx^2+vy^2)", "D2 vs Sqrt(vx^2+vy^2)"});
        cbDCCF = new JComboBox<>(new String[]{"x direction", "y direction", "diagonal /", "diagonal \\"});

        tfFrameTime.setToolTipText("Time per this. NOTE: Changing this value will reinitialize all arrays.");
        tfBinning.setToolTipText("Pixel binning used in the evlauations. NOTE: Changing this value will reinitialize all arrays.");
        tfCCFDistance.setToolTipText("Distance in x- and y-direction for spatial cross-correlation. NOTE: Changing this value will reinitialize all arrays.");

        JBackgroundSubtractionComponentObj.tbBGR.setToolTipText("Panel for different methods to perform background subtraction.");

        // row 1
        add(new JLabel("Image"));
        add(btnFact.createJButton("Use", "Uses the active existing image in ImageJ.", null, controller.btnUseExistingPressed));
        add(btnFact.createJButton("Load", "Opens a dialog to open a new image.", null, controller.btnLoadNewPressed));
        add(btnFact.createJButton("Batch", "Allow to select a list of evaluations to be performed on a range of images.", null, controller.btnBatchPressed));

        // row 2
        add(new JLabel("First frame: "));
        add(tfFirstFrame);
        add(new JLabel("Last frame: "));
        add(tfLastFrame);

        // row 3
        add(new JLabel("Frame time: "));
        add(tfFrameTime);
        add(btnFact.createJToggleButton("Exp Set", "Opens a dialog with experimental settings.", null, controller.tbExpSettingsPressed));
        add(btnFact.createJButton("Write Conf", "Writes a configuration file int user.home that will be read at next ImFCS start", new Font(PANEL_FONT, Font.BOLD, 11), controller.btnWriteConfigPressed));

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
        add(btnFact.createJToggleButton("FCCS Disp Off", "", new Font(PANEL_FONT, Font.BOLD, 9), controller.tbFCCSDisplayPressed));
        add(btnFact.createJToggleButton("Overlap Off", "", new Font(PANEL_FONT, Font.BOLD, 11), controller.tbOverlapPressed));

        // row 7
        add(new JLabel(""));
        add(new JLabel(""));
        add(new JLabel(""));
        add(new JLabel(""));

        // row 8
        add(btnFact.createJButton("LiveReadout", "", new Font(PANEL_FONT, Font.BOLD, 10), controller.btnDCRPressed));
        add(btnFact.createJButton("PVideo", "Creates videos of parameter maps", null, controller.btnParamVideoPressed));
        add(JBackgroundSubtractionComponentObj.tbBGR);
        add(btnFact.createJButton("", "", null, controller.btnDebugPressed));

        // row 9
        add(btnFact.createJButton("Options", "Select various options regarding the display of results.", null, controller.btnOptionsPressed));
        add(btnFact.createJToggleButton("N&B Off", "", null, controller.tbNBPressed));
        add(btnFact.createJToggleButton("Threshold", "Filters the values in parameters maps using user-defined thresholds", null, controller.tbFilteringPressed));
        add(btnFact.createJButton("Average", "Calculate the average ACF from all valid ACFs and fit if fit is switched on; this does not calculate residuals or sd.", null, controller.btnAvePressed));

        // row 10
        add(btnFact.createJButton("Scatter", "Calculates a scatter plot for a pair of two parameters from the scroll down menu.", null, controller.btnParaCorPressed));
        add(cbParaCor);
        add(btnFact.createJToggleButton("Bleach Cor", "Set number of intensity points to be averaged before bleach correction is performed.", null, controller.tbBleachCorStridePressed));
        add(cbBleachCor);

        // row 11
        add(btnFact.createJButton("dCCF", "Create a dCCF image to see differences between forward and backward correlation in a direction (see scroll down menu).", null, controller.btnDCCFPressed));
        add(cbDCCF);
        add(new JLabel("Filter (All):"));
        add(cbFilter);

        // row 12
        add(btnFact.createJButton("PSF", "Calculates the calibration for the PSF.", null, controller.btnPSFPressed));
        add(btnFact.createJToggleButton("Diff. Law", "Calculates the Diffusion Law.", null, controller.tbDLPressed));
        add(btnFact.createJToggleButton("Fit off", "Switches Fit on/off; opens/closes Fit panel.", null, controller.tbFitPressed));
        add(btnFact.createJButton("All", "Calculates all ACFs.", null, controller.btnAllPressed));

        // row 13
        add(btnFact.createJToggleButton("Sim off", "Opens/closes Simulation panel.", null, controller.tbSimPressed));
        add(btnFact.createJButton("Res. Table", "Create a results table.", null, controller.btnRTPressed));
        add(btnFact.createJToggleButton("MSD Off", "Switches Mean Square Displacement calculation and plot on/off.", null, controller.tbMSDPressed));
        add(btnFact.createJButton("ROI", "Calculates ACFs only in the currently chose ROI.", null, controller.btnROIPressed));

        // row 14
        add(btnFact.createJButton("To Front", "Bring all windows of this plugin instance to the front.", null, controller.btnBtfPressed));
        add(btnSave);
        add(btnRead);
        add(btnExit);

        // add listeners
        JBackgroundSubtractionComponentObj.tbBGR.addItemListener(JBackgroundSubtractionComponentObj.tbBackgroundPressed);
        cbBleachCor.addActionListener(cbBleachCorChanged);
        cbFilter.addActionListener(cbFilterChanged);

        tfFirstgetDocument().addDocumentListener(tfFirstFrameChanged);
        tfLastgetDocument().addDocumentListener(tfLastFrameChanged);
        tfBinning.getDocument().addDocumentListener(expSettingsChanged);
        tfCCFDistance.getDocument().addDocumentListener(expSettingsChanged);
    }
}
