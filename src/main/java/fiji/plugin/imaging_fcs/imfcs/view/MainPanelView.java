package fiji.plugin.imaging_fcs.imfcs.view;

import fiji.plugin.imaging_fcs.imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.imfcs.controller.MainPanelController;
import fiji.plugin.imaging_fcs.imfcs.enums.BleachCorrectionMethod;
import fiji.plugin.imaging_fcs.imfcs.enums.DccfDirection;
import fiji.plugin.imaging_fcs.imfcs.enums.FilterMode;
import fiji.plugin.imaging_fcs.imfcs.model.ExpSettingsModel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

import static fiji.plugin.imaging_fcs.imfcs.controller.ControllerUtils.updateComboBoxValue;
import static fiji.plugin.imaging_fcs.imfcs.controller.FieldListenerFactory.createComboBoxListener;
import static fiji.plugin.imaging_fcs.imfcs.controller.FieldListenerFactory.createFocusListener;
import static fiji.plugin.imaging_fcs.imfcs.view.ButtonFactory.createJButton;
import static fiji.plugin.imaging_fcs.imfcs.view.ButtonFactory.createJToggleButton;
import static fiji.plugin.imaging_fcs.imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.imfcs.view.UIUtils.createJLabel;
import static fiji.plugin.imaging_fcs.version.VERSION.IMFCS_VERSION;

/**
 * The MainPanelView class defines the GUI for the Imaging FCS plugin in Fiji.
 * It extends BaseView and uses a MainPanelController to handle events.
 */
public final class MainPanelView extends BaseView {
    // Font for bold labels
    private static final Font BOLD_FONT = new Font(Constants.PANEL_FONT, Font.BOLD, 12);
    // Colors for buttons
    private static final Color SAVE_BUTTON_COLOR = Color.BLUE;
    private static final Color LOAD_BUTTON_COLOR = Color.BLUE;
    private static final Color EXIT_BUTTON_COLOR = Color.RED;
    // Animation parameters
    private static final int ANIMATION_DELAY = 15; // milliseconds between animation updates
    private static final int ANIMATION_STEP = 20;  // pixels to expand/shrink per step
    // UI Constants
    private static final int COLUMN_NUMBER = 4;
    private static final int EMPTY_ROW_HEIGHT = 15;
    // Controller for handling user actions
    private final MainPanelController controller;

    // Settings model for default values
    private final ExpSettingsModel settings;

    // Text Fields for user input
    private JTextField tfFirstFrame, tfLastFrame, tfFrameTime, tfBinning, tfCCFDistance, tfCorrelatorQ;

    // Combo boxes for selecting options
    private JComboBox<FilterMode> cbFilter;
    private JComboBox<BleachCorrectionMethod> cbBleachCor;
    private JComboBox<DccfDirection> cbDCCF;
    private JComboBox<String> cbCorrelatorP, cbParaCor;

    // Buttons
    private JButton btnSave, btnRead, btnExit, btnLoad, btnBatch, btnDCCF, btnWriteConfig, btnUseExisting, btnDCR,
            btnParamVideo, btnOptions, btnAve, btnParaCor, btnPSF, btnAll, btnROI, btnMore, btnCancel, btnBringToFront,
            btnNB;
    private JToggleButton tbExpSettings, tbFit, tbOverlap, tbBackground, tbFiltering, tbBleachCorStride, tbDL, tbSim,
            tbMSD, tbOnnxInference;

    // Extended panel for additional features
    private JPanel extendedPanel;
    // Variables for animation
    private Timer animationTimer;
    private boolean isAnimating;
    private boolean isExpanding;
    private int extendedPanelHeight;

    /**
     * Constructs the MainPanelView with a specified controller.
     *
     * @param controller The controller to handle actions performed on this panel.
     */
    public MainPanelView(MainPanelController controller, ExpSettingsModel settings) {
        super("ImagingFCS " + IMFCS_VERSION); // items for ImFCS control panel;
        this.controller = controller;
        this.settings = settings;
        initializeUI();
    }

    /**
     * Creates a toggle button with an "On" or "Off" label based on the given state.
     *
     * @param label        The base label for the button, with " On" or " Off" appended.
     * @param isOn         The initial state; true for "On" (selected), false for "Off" (deselected).
     * @param toolTipText  Tooltip text for the button.
     * @param font         Font for the button text, or null for default.
     * @param itemListener Listener for state changes when toggled.
     * @return A {@link JToggleButton} initialized with the specified settings.
     */
    private static JToggleButton createOnOffToggleButton(String label, boolean isOn, String toolTipText, Font font,
                                                         ItemListener itemListener) {
        JToggleButton button = createJToggleButton(label + (isOn ? " On" : " Off"), toolTipText, font);
        button.setSelected(isOn);
        button.addItemListener(itemListener);

        return button;
    }

    /**
     * Configures basic window properties.
     */
    @Override
    protected void configureWindow() {
        super.configureWindow();

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
                createTextField(settings.getFirstFrame(), "", controller.updateStrideParam(settings::setFirstFrame));

        tfFrameTime = createTextField(settings.getFrameTime(),
                "Time per this. NOTE: Changing this value will reinitialize all arrays.",
                createFocusListener(settings::setFrameTime));

        tfLastFrame =
                createTextField(settings.getLastFrame(), "", controller.updateStrideParam(settings::setLastFrame));

        tfBinning = createTextField(settings.getBinningString(),
                "Pixel binning used in the evaluations. NOTE: Changing this value will reinitialize all arrays.",
                controller.updateSettings(settings::setBinning));

        tfCCFDistance = createTextField(settings.getCCFString(), "Distance in x- and y-direction for spatial " +
                        "cross-correlation. NOTE: Changing this value will reinitialize all arrays.",
                controller.updateSettings(settings::setCCF));

        tfCorrelatorQ = createTextField(settings.getCorrelatorQ(), "",
                createFocusListener(controller.updateFitEnd(settings::setCorrelatorQ)));
    }

    /**
     * Initializes combo boxes with options for various parameters and settings.
     * This method sets up combo boxes used for selecting options in the main panel. It also
     * attaches action listeners to some combo boxes to handle user selections.
     */
    @Override
    protected void initializeComboBoxes() {
        cbCorrelatorP = new JComboBox<>(new String[]{"16", "32"});
        cbCorrelatorP.setSelectedItem(String.valueOf(settings.getCorrelatorP()));

        cbFilter = new JComboBox<>(FilterMode.values());
        cbFilter.setSelectedItem(settings.getFilter());

        cbBleachCor = new JComboBox<>(BleachCorrectionMethod.values());
        cbBleachCor.setSelectedItem(settings.getBleachCorrection());

        cbParaCor = new JComboBox<>(new String[]{
                "N vs D",
                "N vs F2",
                "D vs F2",
                "N*(1-F2) vs D",
                "N*F2 vs D2",
                "D vs Sqrt(vx²+vy²)",
                "D2 vs Sqrt(vx²+vy²)"
        });

        cbDCCF = new JComboBox<>(DccfDirection.values());

        // add listeners
        cbCorrelatorP.addActionListener(
                createComboBoxListener(cbCorrelatorP, controller.updateFitEnd(settings::setCorrelatorP)));
        cbParaCor.addActionListener(updateComboBoxValue(settings::setParaCor));
        cbBleachCor.addActionListener(controller.cbBleachCorChanged(cbBleachCor));
        cbFilter.addActionListener(controller.cbFilterChanged(cbFilter));
        cbDCCF.addActionListener(updateComboBoxValue(settings::setdCCF));
    }

    /**
     * Initializes all buttons used in the main panel.
     * This method calls sub-methods to create regular buttons and toggle buttons.
     */
    @Override
    protected void initializeButtons() {
        createJButtons();
        createJToggleButtons();
    }

    private void createJButtons() {
        // create the IO colored buttons
        btnSave = createJButton("Save",
                "Save the evaluation of the data as binary files. Which data to save can be selected in a dialog.",
                null, controller.btnSavePressed());
        btnSave.setForeground(SAVE_BUTTON_COLOR);

        btnRead = createJButton("Read", "Load a previously saved experiment. Note that the original image is not " +
                "automatically loaded along.", null, controller.btnLoadPressed());
        btnRead.setForeground(LOAD_BUTTON_COLOR);

        btnExit = createJButton("Exit", "", null, controller.btnExitPressed());
        btnExit.setForeground(EXIT_BUTTON_COLOR);

        // Buttons
        btnUseExisting = createJButton("Use", "Uses the active existing image in ImageJ.", null,
                controller.btnUseExistingPressed());
        btnLoad = createJButton("Load", "Opens a dialog to open a new image.", null, controller.btnLoadNewPressed());
        btnBatch = createJButton("Batch", "Allow to select a list of evaluations to be performed on a range of images.",
                null, controller.btnBatchPressed());
        btnWriteConfig = createJButton("Write Conf",
                "Writes a configuration file int user.home that will be read at next ImFCS start",
                new Font(Constants.PANEL_FONT, Font.BOLD, 11), controller.btnWriteConfigPressed());
        btnDCR = createJButton("LiveReadout", "", new Font(Constants.PANEL_FONT, Font.BOLD, 10),
                controller.btnDirectCameraReadoutPressed());
        btnParamVideo =
                createJButton("PVideo", "Creates videos of parameter maps", null, controller.btnParamVideoPressed());
        btnOptions = createJButton("Options", "Select various options regarding the display of results.", null,
                controller.btnOptionsPressed());
        btnAve = createJButton("Average", "Calculate the average ACF from all valid ACFs and fit if fit is switched " +
                "on; this does not calculate residuals or sd.", null, controller.btnAveragePressed());
        btnParaCor = createJButton("Scatter",
                "Calculates a scatter plot for a pair of two parameters from the scroll down menu.", null,
                controller.btnParaCorPressed());
        btnDCCF = createJButton("dCCF", "Create a dCCF image to see differences between forward and backward " +
                "correlation in a direction (see scroll down menu).", null, controller.btnDCCFPressed());
        btnPSF = createJButton("PSF", "Calculates the calibration for the PSF.", null, controller.btnPSFPressed());
        btnAll = createJButton("All", "Calculates all ACFs.", null, controller.btnAllPressed());
        btnROI = createJButton("ROI", "Calculates ACFs only in the currently chose ROI.", null,
                controller.btnROIPressed());
        btnNB = createJButton("N&B", "", null, controller.btnNBPressed());
        btnBringToFront = createJButton("To Front", "Bring all windows of this plugin instance to the front.", null,
                controller.btnBringToFrontPressed());
        btnCancel = createJButton("Cancel correlation", "Cancel the current correlation run.", null,
                controller.btnCancelCorrelationPressed());
        btnCancel.setForeground(EXIT_BUTTON_COLOR);

        btnMore = createJButton("More \u25BC", "Show more options", null, controller.btnMorePressed());
    }

    private void createJToggleButtons() {
        tbExpSettings = createJToggleButton("Exp Set", "Opens a dialog with experimental settings.", null,
                controller.tbExpSettingsPressed());

        // Set the button overlap based on the settings value
        tbOverlap = createOnOffToggleButton("Overlap", settings.isOverlap(), "",
                new Font(Constants.PANEL_FONT, Font.BOLD, 11), controller.tbOverlapPressed());

        tbBackground =
                createJToggleButton("Background", "Panel for different methods to perform background subtraction.",
                        new Font(Constants.PANEL_FONT, Font.BOLD, 10), controller.tbBackgroundPressed());
        tbFiltering =
                createJToggleButton("Threshold", "Filters the values in parameters maps using user-defined thresholds",
                        null, controller.tbFilteringPressed());
        tbBleachCorStride = createJToggleButton("Bleach Cor",
                "Set number of intensity points to be averaged before bleach correction is performed.", null,
                controller.tbBleachCorStridePressed());
        tbDL = createJToggleButton("Diff. Law", "Calculates the Diffusion Law.", null,
                controller.tbDiffusionLawPressed());
        tbFit = createJToggleButton("Fit Off", "Switches Fit on/off; opens/closes Fit panel.", null,
                controller.tbFitPressed());
        tbSim = createJToggleButton("Sim Off", "Opens/closes Simulation panel.", null, controller.tbSimPressed());

        // set the button MSD based on the settings value
        tbMSD = createOnOffToggleButton("MSD", settings.isMSD(),
                "Switches Mean Square Displacement calculation and " + "plot on/off.", null, controller.tbMSDPressed());

        tbOnnxInference = createJToggleButton("ONNX Inference", "Show window for ONNX Model Inference", null, controller.tbOnnxInferencePressed());
    }

    /**
     * Adds a row of components to the main panel.
     * Each component is added to a row panel with a GridLayout to ensure uniform sizes.
     *
     * @param mainPanel  The main panel to which the row is added.
     * @param components The components to add in the row.
     */
    private void addRow(JPanel mainPanel, Component... components) {
        JPanel rowPanel = new JPanel(new GridLayout(1, COLUMN_NUMBER, 1, 1));
        for (Component comp : components) {
            if (comp != null) {
                rowPanel.add(comp);
            } else {
                rowPanel.add(Box.createGlue()); // Add an empty space if component is null
            }
        }
        mainPanel.add(rowPanel);
    }

    /**
     * Recursively sets the preferred and maximum size for all components in a container.
     * This ensures that all components have a uniform size, contributing to a consistent UI appearance.
     *
     * @param component The component to set the size for.
     * @param size      The dimension to set as both preferred and maximum size.
     */
    private void setUniformSize(Component component, Dimension size) {
        if (component instanceof JButton || component instanceof JTextField || component instanceof JComboBox ||
                component instanceof JToggleButton) {
            component.setPreferredSize(size);
            component.setMaximumSize(size);
        }
        if (component instanceof Container) {
            for (Component child : ((Container) component).getComponents()) {
                setUniformSize(child, size);
            }
        }
    }


    /**
     * Adds components to the frame, organizing them according to the specified layout.
     * This method meticulously places labels, text fields, combo boxes, and buttons into the
     * frame, utilizing a box layout. It also configures action listeners for buttons to
     * interact with the controller for executing actions based on user input.
     */
    @Override
    protected void addComponentsToFrame() {
        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));

        // FILE label
        addRow(mainPanel, createJLabel("FILE", "", BOLD_FONT));

        // FILE buttons
        addRow(mainPanel, btnUseExisting, btnLoad, btnBatch, btnDCR);

        // Save, Read, Background
        addRow(mainPanel, btnSave, btnRead, tbBackground, btnExit);

        // Empty row
        addRow(mainPanel, Box.createVerticalStrut(EMPTY_ROW_HEIGHT));
        // SETTINGS label
        addRow(mainPanel, createJLabel("SETTINGS", "", BOLD_FONT));

        // Settings buttons
        addRow(mainPanel, tbExpSettings, btnOptions, tbOverlap, btnWriteConfig);

        // Frame Time
        addRow(mainPanel, createJLabel("Frame time", ""), tfFrameTime, null, null);

        // First Frame, Last Frame
        addRow(mainPanel, createJLabel("First Frame", ""), tfFirstFrame, createJLabel("Last Frame", ""), tfLastFrame);

        // CCF Distance, Binning
        addRow(mainPanel, createJLabel("CCF Distance", ""), tfCCFDistance, createJLabel("Binning", ""), tfBinning);

        // Correlator P, Correlator Q
        addRow(mainPanel, createJLabel("Correlator P", ""), cbCorrelatorP, createJLabel("Correlator Q", ""),
                tfCorrelatorQ);

        // Empty row
        addRow(mainPanel, Box.createVerticalStrut(EMPTY_ROW_HEIGHT));
        // EVALUATION label
        addRow(mainPanel, createJLabel("EVALUATION", "", BOLD_FONT));

        // Bleach Cor, cbBleachCor, Fit, All
        addRow(mainPanel, tbBleachCorStride, cbBleachCor, tbFit, btnAll);

        // Filter (All), cbFilter, Average, ROI
        addRow(mainPanel, createJLabel("Filter (All)", ""), cbFilter, btnAve, btnROI);

        // Threshold, PVideo, NB, More
        addRow(mainPanel, tbFiltering, btnParamVideo, btnNB, btnMore);

        // Initialize extended panel and add to main panel
        initializeExtendedPanel();

        // Add extended panel to main panel
        mainPanel.add(extendedPanel);
        extendedPanel.setVisible(false);

        Dimension uniformSize = new Dimension(100, 25);
        setUniformSize(mainPanel, uniformSize);
        setUniformSize(extendedPanel, uniformSize);

        // Add main panel to frame
        getContentPane().add(mainPanel);

        // Adjust window size
        pack();
    }

    /**
     * Initializes the extended panel, which contains additional controls that can be toggled visible or hidden.
     * This panel is added to the main panel and starts as hidden.
     */
    private void initializeExtendedPanel() {
        extendedPanel = new JPanel();
        extendedPanel.setLayout(new BoxLayout(extendedPanel, BoxLayout.Y_AXIS));

        // Empty row
        addRow(extendedPanel, Box.createVerticalStrut(EMPTY_ROW_HEIGHT));
        // ADDITIONAL label
        addRow(extendedPanel, createJLabel("ADDITIONAL", "", BOLD_FONT));

        addRow(extendedPanel, btnDCCF, cbDCCF, btnParaCor, cbParaCor);
        addRow(extendedPanel, btnPSF, tbDL, tbMSD, tbSim);
        addRow(extendedPanel, btnBringToFront);
        addRow(extendedPanel, btnCancel);

        // ONNX Model section
        addRow(extendedPanel, createJLabel("ONNX MODELS", "", BOLD_FONT), tbOnnxInference);
    }

    /**
     * Update tfLastFrame with the text given.
     *
     * @param text The text used to update the last frame text field
     */
    public void setTfLastFrame(String text) {
        tfLastFrame.setText(text);
    }

    /**
     * Toggles the visibility of the extended panel with animation and updates the "More" button label.
     * This method is called from the controller when the "More" button is pressed.
     */
    public void toggleExtendedPanel() {
        if (isAnimating) {
            return; // Ignore if an animation is already running
        }

        isAnimating = true;
        isExpanding = !extendedPanel.isVisible();

        if (isExpanding) {
            extendedPanel.setVisible(true);
            btnMore.setText("Less \u25B2"); // Up arrow
        } else {
            btnMore.setText("More \u25BC"); // Down arrow
        }

        extendedPanelHeight = isExpanding ? 0 : extendedPanel.getPreferredSize().height;

        animationTimer = new Timer(ANIMATION_DELAY, new AnimationListener());
        animationTimer.start();
    }

    /**
     * Inner class to handle the animation steps.
     */
    private class AnimationListener implements ActionListener {
        @Override
        public void actionPerformed(ActionEvent e) {
            int panelHeight = extendedPanel.getPreferredSize().height;
            int targetHeight = getExtendedPanelTargetHeight();

            if (isExpanding) {
                panelHeight += ANIMATION_STEP;
                if (panelHeight >= targetHeight) {
                    panelHeight = targetHeight;
                    animationTimer.stop();
                    isAnimating = false;
                }
            } else {
                panelHeight -= ANIMATION_STEP;
                if (panelHeight <= 0) {
                    panelHeight = 0;
                    animationTimer.stop();
                    extendedPanel.setVisible(false);
                    isAnimating = false;
                }
            }

            extendedPanel.setPreferredSize(new Dimension(extendedPanel.getPreferredSize().width, panelHeight));
            extendedPanel.revalidate();
            pack();
        }

        private int getExtendedPanelTargetHeight() {
            if (isExpanding) {
                // Calculate the preferred height when fully expanded
                extendedPanel.setPreferredSize(null);
                extendedPanelHeight = extendedPanel.getPreferredSize().height;
                return extendedPanelHeight;
            } else {
                return 0;
            }
        }
    }
}
