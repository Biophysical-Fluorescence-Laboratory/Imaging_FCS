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

public class SimulationView extends JFrame {
    private static final GridLayout SIMULATION_LAYOUT = new GridLayout(16, 4);
    private static final Point SIMULATION_LOCATION = new Point(
            Constants.MAIN_PANEL_POS.x + Constants.MAIN_PANEL_DIM.width + 10, 125);
    private static final Dimension SIMULATION_DIM = new Dimension(370, 320);
    // Controller
    private final SimulationController controller;
    // model
    private final SimulationModel model;

    // ComboBoxes
    public JComboBox<String> cbSimMode;
    // TextFields
    private JTextField tfSimSeed;
    private JTextField tfSimParticleNum;
    private JTextField tfSimCPS;
    private JTextField tfSimTauBleach;
    private JTextField tfSimPixelNum;
    private JTextField tfSimExtensionFactor;
    private JTextField tfSimTimeStepNum;
    private JTextField tfSimFrameTime;
    private JTextField tfSimStepsPerFrame;
    private JTextField tfSimCurrentStepSize;
    private JTextField tfSimD1;
    private JTextField tfSimDoutDinRatio;
    private JTextField tfSimD2;
    private JTextField tfSimF2;
    private JTextField tfSimD3;
    private JTextField tfSimF3;
    private JTextField tfSimKon;
    private JTextField tfSimKoff;
    private JTextField tfSimCameraOffset;
    private JTextField tfSimCameraNoiseFactor;
    private JTextField tfSimBleachRadius;
    private JTextField tfSimBleachFrame;
    private JTextField tfDomainRadius;
    private JTextField tfDomainDensity;
    private JTextField tfPin;
    private JTextField tfPout;
    private JTextField tfMeshworkSize;
    private JTextField tfHopProbability;

    // buttons
    private JButton btnSimulate;
    private JButton btnBatchSim;
    private JButton btnStopSimulation;
    private JToggleButton tbSimTrip;

    public SimulationView(SimulationController controller, SimulationModel model) {
        super("Simulation Panel");
        this.controller = controller;
        this.model = model;
        initializeUI();
    }

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

    private void configureWindow() {
        setFocusable(true);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setLayout(SIMULATION_LAYOUT);
        setLocation(SIMULATION_LOCATION);
        setSize(SIMULATION_DIM);
        setResizable(false);
    }

    private void initializeComboBoxes() {
        cbSimMode = new JComboBox<>(new String[]{
                "2D (free)", "2D (domains)", "2D (mesh)", "2D (dom+mesh)", "3D (free)"});
        cbSimMode.addActionListener(controller.cbSimModeChanged());
    }

    private void initializeTextFields() {
        tfSimSeed = createTextField(model.getSeed(), "", createFocusListener(model::setSeed));
        tfSimParticleNum = createTextField("1000", "");
        tfSimCPS = createTextField("10000", "");
        tfSimTauBleach = createTextField("0", "");
        tfSimPixelNum = createTextField("21", "");
        tfSimExtensionFactor = createTextField("1.5", "");
        tfSimTimeStepNum = createTextField("50000", "");
        tfSimFrameTime = createTextField("0.001", "");
        tfSimStepsPerFrame = createTextField("10", "");
        tfSimCurrentStepSize = createTextField("20", "");
        tfSimD1 = createTextField("1.0", "");
        tfSimDoutDinRatio = createTextField("1.0", "");
        tfSimD2 = createTextField("0.1", "");
        tfSimF2 = createTextField("0.0", "");
        tfSimD3 = createTextField("0.01", "");
        tfSimF3 = createTextField("0.0", "");
        tfSimKon = createTextField("300", "");
        tfSimKoff = createTextField("700", "");
        tfSimCameraOffset = createTextField("100", "");
        tfSimCameraNoiseFactor = createTextField("3", "");
        tfSimBleachRadius = createTextField("3.0", "");
        tfSimBleachFrame = createTextField("1000000", "");
        tfDomainRadius = createTextField("30", "");
        tfDomainDensity = createTextField("30", "");
        tfPin = createTextField("1.0", "");
        tfPout = createTextField("0.6", "");
        tfMeshworkSize = createTextField("100", "");
        tfHopProbability = createTextField("1", "");
    }

    private void initializeButtons() throws Exception {
        btnSimulate = createJButton("Simulate", "", null, controller.btnSimulatePressed());
        btnBatchSim = createJButton("Batch", "Run multiple simulations", null, controller.btnBatchSimPressed());
        btnStopSimulation = createJButton("Stop", "Stops running simulations", null,
                controller.btnStopSimulationPressed());
        tbSimTrip = createJToggleButton("Triplet off", "", null, controller.tbSimTripPressed());
    }

    private void disable_fields() {
        tfSimCurrentStepSize.setEnabled(false);
        btnStopSimulation.setEnabled(false);

        tripletSetEnable(false);
        meshSetEnable(false);
        domainSetEnable(false);
    }

    public void tripletSetEnable(boolean b) {
        tfSimKon.setEnabled(b);
        tfSimKoff.setEnabled(b);
    }

    public void bleachSetEnable(boolean b) {
        tfSimBleachRadius.setEnabled(b);
        tfSimBleachFrame.setEnabled(b);
    }

    public void meshSetEnable(boolean b) {
        tfMeshworkSize.setEnabled(b);
        tfHopProbability.setEnabled(b);
    }

    public void domainSetEnable(boolean b) {
        tfSimDoutDinRatio.setEnabled(b);
        tfDomainRadius.setEnabled(b);
        tfDomainDensity.setEnabled(b);
        tfPin.setEnabled(b);
        tfPout.setEnabled(b);
    }

    private void addComponentsToFrame() {
        // row 1
        add(createJLabel("Mode", ""));
        add(cbSimMode);
        add(createJLabel("", ""));
        add(tbSimTrip);

        // row 2
        add(createJLabel("Seed",
                "Integer: Seed for the random number generator. Using the same seed (>0) leads to reproducible simulations."));
        add(tfSimSeed);
        add(createJLabel("Particle #", "Integer: Number of particles"));
        add(tfSimParticleNum);

        // row 3
        add(createJLabel("CPS", "Integer: counts per particle per second; brightness of the molecules"));
        add(tfSimCPS);
        add(createJLabel("Bleach time",
                "Integer: Characteristic bleach time in seconds (based on an exponential). Set to 0 for no bleaching"));
        add(tfSimTauBleach);

        // row 4
        add(createJLabel("Pixel #",
                "Integer: number of pixels in x and y direction to be simulated. Only square areas are used."));
        add(tfSimPixelNum);
        add(createJLabel("Extension", "Double: ratio of simulated to observed region."));
        add(tfSimExtensionFactor);

        // row 5
        add(createJLabel("Frame #", "Integer: Numbers of frames to be simulated."));
        add(tfSimTimeStepNum);
        add(createJLabel("Time res", "Double: time per frame in seconds.  Press enter to calculate new step size."));
        add(tfSimFrameTime);

        // row 6
        add(createJLabel("Steps per Frame",
                "Double: number of simulation steps per frame. Press enter to calculate new step size."));
        add(tfSimStepsPerFrame);
        add(createJLabel("Step Size [nm]",
                "Double: Shows the current step size in the simulations based on D1 and time per simulation step"));
        add(tfSimCurrentStepSize);

        // row 7
        add(createJLabel("D1 [um2/s]",
                "Double: Diffusion coefficient of first species to be simulated.  Press enter to calculate new step size."));
        add(tfSimD1);
        add(createJLabel("Dout/Din",
                "Double: Ratio of diffusion coefficients of particles outside and inside domains."));
        add(tfSimDoutDinRatio);

        // row 8
        add(createJLabel("D2 [um2/s]", "Double: Diffusion coefficient of second species to be simulated (if any)."));
        add(tfSimD2);
        add(createJLabel("F2", "Double (0 < F2 < 1): Fraction of particles of the total for the second species."));
        add(tfSimF2);

        // row 9
        add(createJLabel("D3 [um2s]", "Double: Diffusion coefficient of third species to be simulated (if any)."));
        add(tfSimD3);
        add(createJLabel("F3",
                "Double (0 < F3 < 1 AND F2 + F3 < 1): Fraction of particles of the total for the second species."));
        add(tfSimF3);

        // row 10
        add(createJLabel("kon (triplet)", "Double: on rate for transition to triplet"));
        add(tfSimKon);
        add(createJLabel("koff (triplet)", "Double: off rate for transition from triplet."));
        add(tfSimKoff);

        // row 11
        add(createJLabel("Cam Offset", "Integer: Offset of the CCD camera."));
        add(tfSimCameraOffset);
        add(createJLabel("Cam Noise", "Integer: noise factor of the camera."));
        add(tfSimCameraNoiseFactor);

        // row 12
        add(createJLabel("FRAP Radius [um]",
                "Double: Radius in um within which the particles will be bleached. Only available in 2D."));
        add(tfSimBleachRadius);
        add(createJLabel("FRAP Frame",
                "Integer: Frame at which the assumed bleach pulse happens. Bleaching is assumed to be instantaneous."));
        add(tfSimBleachFrame);

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
}
