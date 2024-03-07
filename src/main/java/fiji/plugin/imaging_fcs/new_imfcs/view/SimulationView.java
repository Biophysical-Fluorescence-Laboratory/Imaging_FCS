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
    public JComboBox<String> cbMode;
    // TextFields
    private JTextField tfSeed;
    private JTextField tfNumParticle;
    private JTextField tfCPS;
    private JTextField tfTauBleach;
    private JTextField tfPixelNum;
    private JTextField tfExtensionFactor;
    private JTextField tfNumFrames;
    private JTextField tfFrameTime;
    private JTextField tfStepsPerFrame;
    private JTextField tfCurrentStepSize;
    private JTextField tfD1;
    private JTextField tfDoutDinRatio;
    private JTextField tfD2;
    private JTextField tfF2;
    private JTextField tfD3;
    private JTextField tfF3;
    private JTextField tfKon;
    private JTextField tfKoff;
    private JTextField tfCameraOffset;
    private JTextField tfCameraNoiseFactor;
    private JTextField tfBleachRadius;
    private JTextField tfBleachFrame;
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
        cbMode = new JComboBox<>(new String[]{
                "2D (free)", "2D (domains)", "2D (mesh)", "2D (dom+mesh)", "3D (free)"});
        cbMode.addActionListener(controller.cbModeChanged());
    }

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
        tfD1 = createTextField(model.getD1(), "", createFocusListener(model::setD1));
        tfDoutDinRatio = createTextField(model.getDoutDinRatio(), "", createFocusListener(model::setDoutDinRatio));
        tfD2 = createTextField(model.getD2(), "", createFocusListener(model::setD2));
        tfF2 = createTextField(model.getF2(), "", createFocusListener(model::setF2));
        tfD3 = createTextField(model.getD3(), "", createFocusListener(model::setD3));
        tfF3 = createTextField(model.getF3(), "", createFocusListener(model::setF3));
        tfKon = createTextField(model.getKon(), "", createFocusListener(model::setKon));
        tfKoff = createTextField(model.getKoff(), "", createFocusListener(model::setKoff));
        tfCameraOffset = createTextField(model.getCameraOffset(), "", createFocusListener(model::setCameraOffset));
        tfCameraNoiseFactor = createTextField(model.getCameraNoiseFactor(), "", createFocusListener(model::setCameraNoiseFactor));
        tfBleachRadius = createTextField(model.getBleachRadius(), "", createFocusListener(model::setBleachRadius));
        tfBleachFrame = createTextField(model.getBleachFrame(), "", createFocusListener(model::setBleachFrame));
        tfDomainRadius = createTextField(model.getDomainRadius(), "", createFocusListener(model::setDomainRadius));
        tfDomainDensity = createTextField(model.getDomainDensity(), "", createFocusListener(model::setDomainDensity));
        tfPin = createTextField(model.getPin(), "", createFocusListener(model::setPin));
        tfPout = createTextField(model.getPout(), "", createFocusListener(model::setPout));
        tfMeshworkSize = createTextField(model.getMeshWorkSize(), "", createFocusListener(model::setMeshWorkSize));
        tfHopProbability = createTextField(model.getHopProbability(), "", createFocusListener(model::setHopProbability));
    }

    private void initializeButtons() throws Exception {
        btnSimulate = createJButton("Simulate", "", null, controller.btnSimulatePressed());
        btnBatchSim = createJButton("Batch", "Run multiple simulations", null, controller.btnBatchSimPressed());
        btnStopSimulation = createJButton("Stop", "Stops running simulations", null,
                controller.btnStopSimulationPressed());
        tbSimTrip = createJToggleButton("Triplet off", "", null, controller.tbSimTripPressed());
    }

    private void disable_fields() {
        tfCurrentStepSize.setEnabled(false);
        btnStopSimulation.setEnabled(false);

        tripletSetEnable(false);
        meshSetEnable(false);
        domainSetEnable(false);
    }

    public void tripletSetEnable(boolean b) {
        tfKon.setEnabled(b);
        tfKoff.setEnabled(b);
    }

    public void bleachSetEnable(boolean b) {
        tfBleachRadius.setEnabled(b);
        tfBleachFrame.setEnabled(b);
    }

    public void meshSetEnable(boolean b) {
        tfMeshworkSize.setEnabled(b);
        tfHopProbability.setEnabled(b);
    }

    public void domainSetEnable(boolean b) {
        tfDoutDinRatio.setEnabled(b);
        tfDomainRadius.setEnabled(b);
        tfDomainDensity.setEnabled(b);
        tfPin.setEnabled(b);
        tfPout.setEnabled(b);
    }

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
}
