package fiji.plugin.imaging_fcs.new_imfcs.model;

import fiji.plugin.imaging_fcs.new_imfcs.controller.SimulationController;
import fiji.plugin.imaging_fcs.new_imfcs.model.simulation.Simulation2D;
import fiji.plugin.imaging_fcs.new_imfcs.model.simulation.Simulation3D;
import ij.IJ;
import ij.ImagePlus;

import javax.swing.*;

/**
 * Represents the data model for FCS simulation, encapsulating all simulation parameters
 * and providing methods to execute and control simulations.
 */
public class SimulationModel {
    public static final double PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR = Math.pow(10, 6);
    private static final double DIFFUSION_COEFFICIENT_BASE = Math.pow(10, 12);
    private static final double DOMAIN_MESH_CONVERSION = Math.pow(10, 9);
    private final ExpSettingsModel expSettingsModel;
    private final SimulationController controller;
    private SwingWorker<Void, Void> worker;
    private boolean is2D, isDomain, isMesh, blinkFlag;
    private int seed = 1;
    private int numParticles = 1000; // number of simulated particles
    private int CPS = 10000; // average count rate per particle per second
    private double tauBleach = 0; // bleach time in seconds
    private int pixelNum = 21; // width of image in pixels
    private double extFactor = 1.5; // factor by which the simulated area is bigger than the observed area
    private int numFrames = 50000; // number of frames to be simulated
    private double frameTime = 0.001; // time resolution of the camera in second
    private int stepsPerFrame = 10; // simulation steps per frame
    private double D1 = 1.0 / DIFFUSION_COEFFICIENT_BASE; // particle 1 diffusion coefficient
    private double doutDinRatio = 1.0; // ratio of diffusion coefficients outside over inside of domains
    private double D2 = 0.1 / DIFFUSION_COEFFICIENT_BASE; // particle 2 diffusion coefficient
    private double D3 = 0.01 / DIFFUSION_COEFFICIENT_BASE; // particle 3 diffusion coefficient
    private double F2 = 0.0; // fraction of particle 2
    private double F3 = 0.0; // fraction of particle 3
    private double kon = 300.0; // on-rate for triplet
    private double koff = 700.0; // off-rate for triplet
    private int cameraOffset = 100; // offset of CCD camera
    private double cameraNoiseFactor = 3.0; // noise of CCD camera
    private double bleachRadius = 3.0 / PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR; // bleach radius
    private int bleachFrame = 10000000; // frame at which bleach happens
    private double domainRadius = 30.0 / DOMAIN_MESH_CONVERSION; // Radius of domains
    private double domainDensity = 30.0; // Density of domains in number/um2
    private double pin = 1.0; // Probability to enter domain
    private double pout = 0.6; // Probability to exit domain
    private double meshWorkSize = 100.0 / DOMAIN_MESH_CONVERSION; // Size of meshes
    private double hopProbability = 1.0; // hop probability over meshwork barriers

    /**
     * Constructs a simulation model with references to the controller and experimental settings.
     *
     * @param controller       the controller for simulation actions.
     * @param expSettingsModel the experimental settings model.
     */
    public SimulationModel(SimulationController controller, ExpSettingsModel expSettingsModel) {
        this.controller = controller;
        this.expSettingsModel = expSettingsModel;

        is2D = true;
        isDomain = false;
        isMesh = false;
        blinkFlag = false;
    }

    /**
     * Starts the simulation process in a background thread, updating UI upon completion.
     */
    public void runSimulation() {
        SimulationModel model = this;
        worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                ImagePlus image = null;
                try {
                    if (is2D) {
                        Simulation2D simulation = new Simulation2D(model, expSettingsModel);
                        image = simulation.simulateACF2D();
                    } else {
                        Simulation3D simulation = new Simulation3D(model, expSettingsModel);
                        image = simulation.SimulateACF3D();
                    }

                    IJ.run(image, "Enhance Contrast", "saturated=0.35");
                    controller.loadImage(image);
                } catch (RuntimeException e) {
                    IJ.showProgress(1);
                    IJ.showStatus("Simulation Interrupted");
                    IJ.showMessage(e.getMessage());
                }
                return null;
            }

            @Override
            protected void done() {
                controller.onSimulationComplete();
            }
        };
        worker.execute();
    }

    /**
     * Cancels the currently running simulation, if possible.
     *
     * @param mayInterruptIfRunning true if the thread executing this task should be interrupted; otherwise, in-progress tasks are allowed to complete.
     */
    public void cancel(boolean mayInterruptIfRunning) {
        worker.cancel(mayInterruptIfRunning);
    }

    // Getter and setter methods follow, providing access to all simulation parameters.
    // Each setter method parses its input to the expected data type, applying necessary conversions where applicable.
    public boolean getIs2D() {
        return is2D;
    }

    public void setIs2D(boolean is2D) {
        this.is2D = is2D;
    }

    public boolean getIsDomain() {
        return isDomain;
    }

    public void setIsDomain(boolean isDomain) {
        this.isDomain = isDomain;
    }

    public boolean getIsMesh() {
        return isMesh;
    }

    public void setIsMesh(boolean isMesh) {
        this.isMesh = isMesh;
    }

    public boolean getBlinkFlag() {
        return blinkFlag;
    }

    public void setBlinkFlag(boolean blinkFlag) {
        this.blinkFlag = blinkFlag;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = Integer.parseInt(seed);
    }

    public int getNumParticles() {
        return numParticles;
    }

    public void setNumParticles(String numParticles) {
        this.numParticles = Integer.parseInt(numParticles);
    }

    public int getCPS() {
        return CPS;
    }

    public void setCPS(String CPS) {
        this.CPS = Integer.parseInt(CPS);
    }

    public double getTauBleach() {
        return tauBleach;
    }

    public void setTauBleach(String tauBleach) {
        this.tauBleach = Double.parseDouble(tauBleach);
    }

    public int getPixelNum() {
        return pixelNum;
    }

    public void setPixelNum(String pixelNum) {
        this.pixelNum = Integer.parseInt(pixelNum);
    }

    public double getExtFactor() {
        return extFactor;
    }

    public void setExtFactor(String extFactor) {
        this.extFactor = Double.parseDouble(extFactor);
    }

    public int getNumFrames() {
        return numFrames;
    }

    public void setNumFrames(String numFrames) {
        this.numFrames = Integer.parseInt(numFrames);
    }

    public double getFrameTime() {
        return frameTime;
    }

    public void setFrameTime(String frameTime) {
        this.frameTime = Double.parseDouble(frameTime);
    }

    public int getStepsPerFrame() {
        return stepsPerFrame;
    }

    public void setStepsPerFrame(String stepsPerFrame) {
        this.stepsPerFrame = Integer.parseInt(stepsPerFrame);
    }

    public double getD1Interface() {
        return D1 * DIFFUSION_COEFFICIENT_BASE;
    }

    public double getD1() {
        return D1;
    }

    public void setD1(String D1) {
        this.D1 = Double.parseDouble(D1) / DIFFUSION_COEFFICIENT_BASE;
    }

    public double getDoutDinRatio() {
        return doutDinRatio;
    }

    public void setDoutDinRatio(String doutDinRatio) {
        this.doutDinRatio = Double.parseDouble(doutDinRatio);
    }

    public double getD2() {
        return D2;
    }

    public void setD2(String D2) {
        this.D2 = Double.parseDouble(D2) / DIFFUSION_COEFFICIENT_BASE;
    }

    public double getD2Interface() {
        return D2 * DIFFUSION_COEFFICIENT_BASE;
    }

    public double getD3() {
        return D3;
    }

    public void setD3(String D3) {
        this.D3 = Double.parseDouble(D3) / DIFFUSION_COEFFICIENT_BASE;
    }

    public double getD3Interface() {
        return D3 * DIFFUSION_COEFFICIENT_BASE;
    }

    public double getF2() {
        return F2;
    }

    public void setF2(String F2) {
        this.F2 = Double.parseDouble(F2);
    }

    public double getF3() {
        return F3;
    }

    public void setF3(String F3) {
        this.F3 = Double.parseDouble(F3);
    }

    public double getKon() {
        return kon;
    }

    public void setKon(String kon) {
        this.kon = Double.parseDouble(kon);
    }

    public double getKoff() {
        return koff;
    }

    public void setKoff(String koff) {
        this.koff = Double.parseDouble(koff);
    }

    public int getCameraOffset() {
        return cameraOffset;
    }

    public void setCameraOffset(String cameraOffset) {
        this.cameraOffset = Integer.parseInt(cameraOffset);
    }

    public double getCameraNoiseFactor() {
        return cameraNoiseFactor;
    }

    public void setCameraNoiseFactor(String cameraNoiseFactor) {
        this.cameraNoiseFactor = Double.parseDouble(cameraNoiseFactor);
    }

    public double getBleachRadius() {
        return bleachRadius;
    }

    public void setBleachRadius(String bleachRadius) {
        this.bleachRadius = Double.parseDouble(bleachRadius) / PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
    }

    public double getBleachRadiusInterface() {
        return bleachRadius * PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;
    }

    public int getBleachFrame() {
        return bleachFrame;
    }

    public void setBleachFrame(String bleachFrame) {
        this.bleachFrame = Integer.parseInt(bleachFrame);
    }

    public double getDomainRadius() {
        return domainRadius;
    }

    public void setDomainRadius(String domainRadius) {
        this.domainRadius = Double.parseDouble(domainRadius) / DOMAIN_MESH_CONVERSION;
    }

    public double getDomainRadiusInterface() {
        return domainRadius * DOMAIN_MESH_CONVERSION;
    }

    public double getDomainDensity() {
        return domainDensity;
    }

    public void setDomainDensity(String domainDensity) {
        this.domainDensity = Double.parseDouble(domainDensity);
    }

    public double getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = Double.parseDouble(pin);
    }

    public double getPout() {
        return pout;
    }

    public void setPout(String pout) {
        this.pout = Double.parseDouble(pout);
    }

    public double getMeshWorkSize() {
        return meshWorkSize;
    }

    public void setMeshWorkSize(String meshWorkSize) {
        this.meshWorkSize = Double.parseDouble(meshWorkSize) / DOMAIN_MESH_CONVERSION;
    }

    public double getMeshWorkSizeInterface() {
        return meshWorkSize * DOMAIN_MESH_CONVERSION;
    }

    public double getHopProbability() {
        return hopProbability;
    }

    public void setHopProbability(String hopProbability) {
        this.hopProbability = Double.parseDouble(hopProbability);
    }
}
