package fiji.plugin.imaging_fcs.new_imfcs.view;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.controller.SimulationController;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;

import javax.swing.*;
import java.awt.*;

import static fiji.plugin.imaging_fcs.new_imfcs.controller.FocusListenerFactory.createFocusListener;
import static fiji.plugin.imaging_fcs.new_imfcs.view.ButtonFactory.createJButton;
import static fiji.plugin.imaging_fcs.new_imfcs.view.ButtonFactory.createJToggleButton;
import static fiji.plugin.imaging_fcs.new_imfcs.view.TextFieldFactory.createTextField;
import static fiji.plugin.imaging_fcs.new_imfcs.view.UIUtils.createJLabel;

/**
 * The SimulationView class extends JFrame and is responsible for creating the GUI for the simulation panel.
 * It initializes and displays various UI components like buttons, text fields, and combo boxes that
 * allow users to input simulation parameters and control the simulation process.
 */
public class SimulationView extends JFrame {
    // Constants
    private static final GridLayout SIMULATION_LAYOUT = new GridLayout(16, 4);
    private static final Point SIMULATION_LOCATION = new Point(
            Constants.MAIN_PANEL_POS.x + Constants.MAIN_PANEL_DIM.width + 10, 125);
    private static final Dimension SIMULATION_DIM = new Dimension(370, 320);

    // References to the controller and the model
    private final SimulationController controller;
    private final SimulationModel model;

    // ComboBoxes
    public JComboBox<String> cbMode;
    // TextFields
    private JTextField tfSeed, tfNumParticle, tfCPS, tfTauBleach, tfPixelNum, tfExtensionFactor, tfNumFrames,
            tfFrameTime, tfStepsPerFrame, tfCurrentStepSize, tfD1, tfDoutDinRatio, tfD2, tfF2, tfD3, tfF3, tfKon,
            tfKoff, tfCameraOffset, tfCameraNoiseFactor, tfBleachRadius, tfBleachFrame, tfDomainRadius, tfDomainDensity,
            tfPin, tfPout, tfMeshworkSize, tfHopProbability;

    // buttons
    private JButton btnSimulate, btnBatchSim, btnStopSimulation;
    private JToggleButton tbSimTrip;

    /**
     * Constructor for SimulationView. Initializes the UI components and sets up the controller and model.
     *
     * @param controller The SimulationController that handles actions performed on the UI.
     * @param model      The SimulationModel that holds the data and state of the simulation.
     */
    public SimulationView(SimulationController controller, SimulationModel model) {
        super("Simulation Panel");
        this.controller = controller;
        this.model = model;
        initializeUI();
    }

    /**
     * Initializes the UI components and configures the layout of the simulation window.
     */
    private void initializeUI() {
        configureWindow();
        initializeComboBoxes();
        initializeTextFields();
        try {
            initializeButtons();
        } catch (Exception e) {
            IJ.log(e.getMessage());
        }

        disable_fields();

        addComponentsToFrame();
    }

    /**
     * Configures basic window properties such as size, location, and behavior on close.
     */
    private void configureWindow() {
        setFocusable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(SIMULATION_LAYOUT);
        setLocation(SIMULATION_LOCATION);
        setSize(SIMULATION_DIM);
        setResizable(false);
    }

    /**
     * Initializes the combo boxes and sets up their action listeners.
     */
    private void initializeComboBoxes() {
        cbMode = new JComboBox<>(new String[]{
                "2D (free)", "2D (domains)", "2D (mesh)", "3D (free)"});
        cbMode.addActionListener(controller.cbModeChanged());
    }

    /**
     * Initializes text fields with model data and focus listeners for updating model values.
     */
    private void initializeTextFields() {
        tfSeed = createTextField(model.getSeed(), "", createFocusListener(model::setSeed));
        tfNumParticle = createTextField(model.getNumParticles(), "", createFocusListener(model::setNumParticles));
        tfCPS = createTextField(model.getCPS(), "", createFocusListener(model::setCPS));
        tfTauBleach = createTextField(model.getTauBleach(), "", createFocusListener(model::setTauBleach));
        tfPixelNum = createTextField(model.getPixelNum(), "", createFocusListener(model::setPixelNum));
        tfExtensionFactor = createTextField(model.getExtFactor(), "", createFocusListener(model::setExtFactor));
        tfNumFrames = createTextField(model.getNumFrames(), "", createFocusListener(model::setNumFrames));
        tfFrameTime = createTextField(model.getFrameTime(), "", createFocusListener(model::setFrameTime));
        tfStepsPerFrame = createTextField(model.getStepsPerFrame(), "", createFocusListener(model::setStepsPerFrame));
        tfCurrentStepSize = createTextField("20", "");
        tfD1 = createTextField(model.getD1Interface(), "", createFocusListener(model::setD1));
        tfDoutDinRatio = createTextField(model.getDoutDinRatio(), "", createFocusListener(model::setDoutDinRatio));
        tfD2 = createTextField(model.getD2Interface(), "", createFocusListener(model::setD2));
        tfF2 = createTextField(model.getF2(), "", createFocusListener(model::setF2));
        tfD3 = createTextField(model.getD3Interface(), "", createFocusListener(model::setD3));
        tfF3 = createTextField(model.getF3(), "", createFocusListener(model::setF3));
        tfKon = createTextField(model.getKon(), "", createFocusListener(model::setKon));
        tfKoff = createTextField(model.getKoff(), "", createFocusListener(model::setKoff));
        tfCameraOffset = createTextField(model.getCameraOffset(), "", createFocusListener(model::setCameraOffset));
        tfCameraNoiseFactor = createTextField(model.getCameraNoiseFactor(), "", createFocusListener(model::setCameraNoiseFactor));
        tfBleachRadius = createTextField(model.getBleachRadiusInterface(), "", createFocusListener(model::setBleachRadius));
        tfBleachFrame = createTextField(model.getBleachFrame(), "", createFocusListener(model::setBleachFrame));
        tfDomainRadius = createTextField(model.getDomainRadiusInterface(), "", createFocusListener(model::setDomainRadius));
        tfDomainDensity = createTextField(model.getDomainDensity(), "", createFocusListener(model::setDomainDensity));
        tfPin = createTextField(model.getPin(), "", createFocusListener(model::setPin));
        tfPout = createTextField(model.getPout(), "", createFocusListener(model::setPout));
        tfMeshworkSize = createTextField(model.getMeshWorkSizeInterface(), "", createFocusListener(model::setMeshWorkSize));
        tfHopProbability = createTextField(model.getHopProbability(), "", createFocusListener(model::setHopProbability));
    }

    /**
     * Initialize buttons and set their listeners.
     */
    private void initializeButtons() throws Exception {
        btnSimulate = createJButton("Simulate", "", null, controller.btnSimulatePressed());
        btnBatchSim = createJButton("Batch", "Run multiple simulations", null, controller.btnBatchSimPressed());
        btnStopSimulation = createJButton("Stop", "Stops running simulations", null,
                controller.btnStopSimulationPressed());
        tbSimTrip = createJToggleButton("Triplet off", "", null, controller.tbSimTripPressed());
    }

    /**
     * Disables specific fields and buttons upon initialization
     * This method is crucial for ensuring that users do not interact with parts of the UI that should be
     * inaccessible due to the default configuration.
     */
    private void disable_fields() {
        tfCurrentStepSize.setEnabled(false);
        btnStopSimulation.setEnabled(false);

        tripletSetEnable(false);
        meshSetEnable(false);
        domainSetEnable(false);
    }

    /**
     * Toggles the enable state of triplet-related input fields.
     *
     * @param b true to enable, false to disable.
     */
    public void tripletSetEnable(boolean b) {
        tfKon.setEnabled(b);
        tfKoff.setEnabled(b);
    }

    /**
     * Toggles the enable state of bleaching-related input fields.
     *
     * @param b true to enable, false to disable.
     */
    public void bleachSetEnable(boolean b) {
        tfBleachRadius.setEnabled(b);
        tfBleachFrame.setEnabled(b);
    }

    /**
     * Toggles the enable state of meshwork-related input fields.
     *
     * @param b true to enable, false to disable.
     */
    public void meshSetEnable(boolean b) {
        tfMeshworkSize.setEnabled(b);
        tfHopProbability.setEnabled(b);
    }

    /**
     * Toggles the enable state of domain-related input fields.
     *
     * @param b true to enable, false to disable.
     */
    public void domainSetEnable(boolean b) {
        tfDoutDinRatio.setEnabled(b);
        tfDomainRadius.setEnabled(b);
        tfDomainDensity.setEnabled(b);
        tfPin.setEnabled(b);
        tfPout.setEnabled(b);
    }

    /**
     * Adds UI components to the frame, organizing them into rows and setting their labels and tooltips.
     * This method systematically arranges all the interactive and informational elements of the simulation panel,
     * facilitating user interaction and providing necessary details about each simulation parameter.
     */
    private void addComponentsToFrame() {
        // row 1
        add(createJLabel("Mode", ""));
        add(cbMode);
        add(createJLabel("", ""));
        add(tbSimTrip);

        // row 2
        add(createJLabel("Seed",
                "Integer: Seed for the random number generator. Using the same seed (>0) leads to reproducible simulations."));
        add(tfSeed);
        add(createJLabel("Particle #", "Integer: Number of particles"));
        add(tfNumParticle);

        // row 3
        add(createJLabel("CPS", "Integer: counts per particle per second; brightness of the molecules"));
        add(tfCPS);
        add(createJLabel("Bleach time",
                "Integer: Characteristic bleach time in seconds (based on an exponential). Set to 0 for no bleaching"));
        add(tfTauBleach);

        // row 4
        add(createJLabel("Pixel #",
                "Integer: number of pixels in x and y direction to be simulated. Only square areas are used."));
        add(tfPixelNum);
        add(createJLabel("Extension", "Double: ratio of simulated to observed region."));
        add(tfExtensionFactor);

        // row 5
        add(createJLabel("Frame #", "Integer: Numbers of frames to be simulated."));
        add(tfNumFrames);
        add(createJLabel("Time res", "Double: time per frame in seconds.  Press enter to calculate new step size."));
        add(tfFrameTime);

        // row 6
        add(createJLabel("Steps per Frame",
                "Double: number of simulation steps per frame. Press enter to calculate new step size."));
        add(tfStepsPerFrame);
        add(createJLabel("Step Size [nm]",
                "Double: Shows the current step size in the simulations based on D1 and time per simulation step"));
        add(tfCurrentStepSize);

        // row 7
        add(createJLabel("D1 [um2/s]",
                "Double: Diffusion coefficient of first species to be simulated.  Press enter to calculate new step size."));
        add(tfD1);
        add(createJLabel("Dout/Din",
                "Double: Ratio of diffusion coefficients of particles outside and inside domains."));
        add(tfDoutDinRatio);

        // row 8
        add(createJLabel("D2 [um2/s]", "Double: Diffusion coefficient of second species to be simulated (if any)."));
        add(tfD2);
        add(createJLabel("F2", "Double (0 < F2 < 1): Fraction of particles of the total for the second species."));
        add(tfF2);

        // row 9
        add(createJLabel("D3 [um2s]", "Double: Diffusion coefficient of third species to be simulated (if any)."));
        add(tfD3);
        add(createJLabel("F3",
                "Double (0 < F3 < 1 AND F2 + F3 < 1): Fraction of particles of the total for the second species."));
        add(tfF3);

        // row 10
        add(createJLabel("kon (triplet)", "Double: on rate for transition to triplet"));
        add(tfKon);
        add(createJLabel("koff (triplet)", "Double: off rate for transition from triplet."));
        add(tfKoff);

        // row 11
        add(createJLabel("Cam Offset", "Integer: Offset of the CCD camera."));
        add(tfCameraOffset);
        add(createJLabel("Cam Noise", "Integer: noise factor of the camera."));
        add(tfCameraNoiseFactor);

        // row 12
        add(createJLabel("FRAP Radius [um]",
                "Double: Radius in um within which the particles will be bleached. Only available in 2D."));
        add(tfBleachRadius);
        add(createJLabel("FRAP Frame",
                "Integer: Frame at which the assumed bleach pulse happens. Bleaching is assumed to be instantaneous."));
        add(tfBleachFrame);

        // rox 13
        add(createJLabel("Dom Rad [nm]", "Radius of domains in nm"));
        add(tfDomainRadius);
        add(createJLabel("Dom Density", "Domain Density in numbers/um2"));
        add(tfDomainDensity);

        // rox 14
        add(createJLabel("Pin", "Probability to enter a domain"));
        add(tfPin);
        add(createJLabel("Pout", "Probability to exit a domain"));
        add(tfPout);

        // rox 15
        add(createJLabel("Mesh Size [nm]", "Mesh size in nm"));
        add(tfMeshworkSize);
        add(createJLabel("Hop Prob", "Probability to hop over a barrier in the meshwork."));
        add(tfHopProbability);

        // rox 16
        add(createJLabel("", ""));
        add(btnBatchSim);
        add(btnStopSimulation);
        add(btnSimulate);
    }

    /**
     * Toggles the enable state of the simulate button.
     *
     * @param b true to enable, false to disable.
     */
    public void enableBtnSimulate(boolean b) {
        btnSimulate.setEnabled(b);
    }

    /**
     * Toggles the enable state of the stop simulation button.
     *
     * @param b true to enable, false to disable.
     */
    public void enableBtnStopSimulation(boolean b) {
        btnStopSimulation.setEnabled(b);
    }
}
