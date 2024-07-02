package fiji.plugin.imaging_fcs.new_imfcs.model;

import fiji.plugin.imaging_fcs.new_imfcs.model.simulation.SimulationWorker;
import ij.IJ;
import ij.ImagePlus;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.DIFFUSION_COEFFICIENT_BASE;
import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;

/**
 * Represents the data model for FCS simulation, encapsulating all simulation parameters
 * and providing methods to execute and control simulations.
 */
public final class SimulationModel {
    private static final double DOMAIN_MESH_CONVERSION = Math.pow(10, 9);
    private final ExpSettingsModel expSettingsModel;
    private List<SimulationWorker> simulationWorkers;

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

    // attributes to count the numbers of simulation running and the numbers of errors
    private int simulationsRunning = 0;
    private int numSimulationsErrors = 0;

    /**
     * Constructs a simulation model with references to the experimental settings.
     *
     * @param expSettingsModel the experimental settings model.
     */
    public SimulationModel(ExpSettingsModel expSettingsModel) {
        this.expSettingsModel = expSettingsModel;

        is2D = true;
        isDomain = false;
        isMesh = false;
        blinkFlag = false;
    }

    /**
     * Starts the simulation process in a background thread, updating UI upon completion.
     */
    public void runSimulation(Runnable onSimulationComplete, Consumer<ImagePlus> loadImage) {
        simulationWorkers = new ArrayList<>();
        try {
            // Here we will load the image directly in the plugin so the path is null.
            // Only one simulation is running, so we don't need a callback to increment the errors numbers
            simulationWorkers.add(new SimulationWorker(this, expSettingsModel, null, loadImage,
                    onSimulationComplete, null));
            simulationWorkers.get(0).execute();
        } catch (RuntimeException e) {
            IJ.showStatus("Instantiation error");
            IJ.showMessage(e.getMessage());
            onSimulationComplete.run();
        }
    }

    /**
     * Resets the diffusion coefficients and fraction values for particles to specified initial values.
     *
     * @param initialD1 Initial diffusion coefficient for particle in group 1.
     * @param initialD2 Initial diffusion coefficient for particle in group 2.
     * @param initialF2 Initial fraction value for particle in group 2.
     */
    private void resetValues(double initialD1, double initialD2, double initialF2) {
        this.D1 = initialD1;
        this.D2 = initialD2;
        this.F2 = initialF2;
    }

    /**
     * Initiates batch simulations based on provided parameter ranges, handling each simulation in separate threads.
     *
     * @param path    Directory for saving batch simulation results.
     * @param rangeD1 Range for D1 parameter.
     * @param rangeD2 Range for D2 parameter.
     * @param rangeF2 Range for F2 parameter.
     */
    public void runBatch(File path, double[] rangeD1, double[] rangeD2, double[] rangeF2,
                         Runnable onBatchSimulationComplete) {
        // reset the variables to count the number of simulations running and errors
        simulationsRunning = 0;
        numSimulationsErrors = 0;

        simulationWorkers = new ArrayList<>();

        // Store the initial values to restore them later
        double initialD1 = this.D1;
        double initialD2 = this.D2;
        double initialF2 = this.F2;

        for (double D1 = rangeD1[0]; D1 <= rangeD1[1]; D1 += rangeD1[2]) {
            this.D1 = D1 / DIFFUSION_COEFFICIENT_BASE;

            for (double D2 = rangeD2[0]; D2 <= rangeD2[1]; D2 += rangeD2[2]) {
                this.D2 = D2 / DIFFUSION_COEFFICIENT_BASE;

                for (double F2 = rangeF2[0]; F2 <= rangeF2[1]; F2 += rangeF2[2]) {
                    this.F2 = F2;

                    try {
                        // In batch mode, we will not load the image in the plugin but save it directly.
                        // Here loadImage callback is null.
                        SimulationWorker simulationWorker =
                                new SimulationWorker(this, expSettingsModel, path, null,
                                        onBatchSimulationComplete, this::incrementSimulationErrorsNumber);
                        simulationWorkers.add(simulationWorker);

                        incrementSimulationsRunningNumber();
                        simulationWorker.execute();
                    } catch (RuntimeException e) {
                        IJ.showStatus("Instantiation error");
                        IJ.showMessage(e.getMessage());

                        // reset the values to make the UI consistent with the model
                        resetValues(initialD1, initialD2, initialF2);
                        incrementSimulationErrorsNumber();
                        onBatchSimulationComplete.run();
                        return;
                    }
                }
            }
        }

        // reset the values to make the UI consistent with the model
        resetValues(initialD1, initialD2, initialF2);
    }

    /**
     * Cancels the currently running simulations.
     *
     * @param mayInterruptIfRunning true if the thread executing this task should be interrupted;
     *                              otherwise, in-progress tasks are allowed to complete.
     */
    public void cancel(boolean mayInterruptIfRunning) {
        for (SimulationWorker simulationWorker : simulationWorkers) {
            simulationWorker.cancel(mayInterruptIfRunning);
        }
    }

    /**
     * Increments the count of simulations currently running. Used to track batch simulation progress.
     */
    public void incrementSimulationsRunningNumber() {
        simulationsRunning++;
    }

    /**
     * Decrements the count of simulations currently running. Used to track batch simulation progress.
     */
    public void decrementSimulationsRunningNumber() {
        simulationsRunning--;
    }

    /**
     * Get the count of simulations currently running.
     *
     * @return the number of running simulations.
     */
    public int getSimulationsRunningNumber() {
        return simulationsRunning;
    }

    /**
     * Increments the count of encountered errors during simulation. Used for error tracking in batch simulations.
     */
    public void incrementSimulationErrorsNumber() {
        numSimulationsErrors++;
    }

    /**
     * Get the count of encountered errors during simulation.
     *
     * @return the number of errors
     */
    public int getNumSimulationsErrors() {
        return numSimulationsErrors;
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
        return Math.round(bleachRadius * PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR);
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
        return Math.round(domainRadius * DOMAIN_MESH_CONVERSION);
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
