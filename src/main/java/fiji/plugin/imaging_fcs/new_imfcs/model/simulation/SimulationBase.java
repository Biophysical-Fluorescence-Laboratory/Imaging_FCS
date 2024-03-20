package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * An abstract base class for simulating fluorescence microscopy experiments.
 * This class provides foundational functionality for simulating the diffusion,
 * bleaching, and blinking of fluorescent particles within a specified simulation area.
 */
public abstract class SimulationBase {
    protected static final double OBSERVATION_WAVELENGTH_CONVERSION_FACTOR = Math.pow(10, 9);

    protected final RandomCustom random;

    private int stepsPerFrame, bleachFrame;
    protected double tStep, darkF, pixelSize, wavelength, PSFSize, midPos, sizeLowerLimit, sizeUpperLimit, bleachFactor,
            blinkOnFactor, blinkOffFactor, sqrtCameraNoiseFactor, D1, D2, D3;

    private int cameraOffset;
    protected boolean bleachFlag, blinkFlag;
    protected long particleGroup1;
    protected long particleGroup2;

    protected ImagePlus image; // Assuming this is your image stack for the simulation
    protected int width, height; // Width and height of the simulation area in pixels
    protected int numFrames, numParticles, CPS;

    protected Particle2D[] particles;

    /**
     * Constructs a SimulationBase instance with specified simulation and experimental settings models.
     *
     * @param model         The simulation model containing the simulation parameters.
     * @param settingsModel The experimental settings model containing settings like pixel size and magnification.
     */
    protected SimulationBase(SimulationModel model, ExpSettingsModel settingsModel) {
        // Validate that we can run the simulation, otherwise throw a runtime exception
        validateSimulationConditions(model, settingsModel);

        // Set random with the seed if it's different to 0 to make results reproducible
        // Use a custom random class to add a poisson generator
        if (model.getSeed() < 0) {
            random = new RandomCustom();
        } else {
            random = new RandomCustom(model.getSeed());
        }

        // Initialize the parameters
        prepareSimulation(model, settingsModel);
    }

    /**
     * Validates the simulation conditions. Implementations should check for any conditions
     * that would prevent the simulation from running and throw an exception if such conditions are found.
     */
    protected abstract void validateSimulationConditions(SimulationModel model, ExpSettingsModel settingsModel);

    /**
     * Prepares the simulation by calculating various parameters and initializing the simulation environment.
     */
    protected void prepareSimulation(SimulationModel model, ExpSettingsModel settingsModel) {
        numFrames = model.getNumFrames();
        stepsPerFrame = model.getStepsPerFrame();
        bleachFrame = model.getBleachFrame();

        // Calculate the time step based on frame time and steps per frame
        tStep = model.getFrameTime() / stepsPerFrame;

        // Calculate the fraction of molecules in the dark state
        darkF = model.getKoff() / (model.getKoff() + model.getKon());

        // Calculate real pixel size based on settings and a conversion factor
        double pixelSizeRealSize =
                settingsModel.getPixelSize() / SimulationModel.PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;

        // Calculate the wavelength based on settings and a conversion factor
        wavelength = settingsModel.getEmLambda() / OBSERVATION_WAVELENGTH_CONVERSION_FACTOR;

        // Sigma0 from settings
        double sigma0 = settingsModel.getSigma();

        // Calculate the pixel size in object space
        pixelSize = pixelSizeRealSize / settingsModel.getMagnification();

        // Calculate the PSF (Point Spread Function) size
        PSFSize = 0.5 * sigma0 * wavelength / settingsModel.getNA();

        // Calculate the size of the grid (i.e., the size of the pixel area of the detector)
        double gridSize = model.getPixelNum() * pixelSize;

        // Calculate the middle position of the grid
        midPos = gridSize / 2.0;

        // Calculate the lower and upper limits of the simulation area
        sizeLowerLimit = -model.getExtFactor() * gridSize;
        sizeUpperLimit = model.getExtFactor() * gridSize;

        // Calculate the bleaching factor
        if (model.getTauBleach() == 0) {
            bleachFlag = false;
            bleachFactor = -1;
        } else {
            bleachFlag = true;
            bleachFactor = Math.exp(-tStep / model.getTauBleach());
        }

        blinkFlag = model.getBlinkFlag();

        // Initialize the blinking factors for on and off states
        blinkOnFactor = Math.exp(-tStep * model.getKon());
        blinkOffFactor = Math.exp(-tStep * model.getKoff());

        sqrtCameraNoiseFactor = Math.sqrt(model.getCameraNoiseFactor());
        cameraOffset = model.getCameraOffset();

        // Initialize image dimension
        width = model.getPixelNum();
        height = model.getPixelNum();

        double f1 = 1.0 - model.getF2() - model.getF3();

        D1 = model.getD1();
        D2 = model.getD2();
        D3 = model.getD3();

        // define the groups size for particles
        numParticles = model.getNumParticles();
        particleGroup1 = Math.round(numParticles * f1);
        particleGroup2 = Math.round(numParticles * model.getF2());

        CPS = model.getCPS();
    }

    /**
     * Runs the 2D/3D simulation and returns an ImagePlus object containing the simulated image stack.
     *
     * @return An ImagePlus object containing the results of the simulation.
     */
    public abstract ImagePlus simulate();

    /**
     * Sets the initial state of a particle based on its index and the simulation parameters.
     *
     * @param particle The particle to set the state for.
     * @param i        The index of the particle in the particles array.
     */
    protected void setParticleState(Particle2D particle, int i) {
        // Determine if the particle is in a dark state based on the dark fraction (darkF)
        if (blinkFlag && (int) ((i + 1) * darkF) > (int) (i * darkF)) {
            particle.setOff();
        }

        // Determine particle diffusion coefficient based on group
        if (i < particleGroup1) {
            particle.setDiffusionCoefficient(D1);
        } else if (i < particleGroup1 + particleGroup2) {
            particle.setDiffusionCoefficient(D2);
        } else {
            particle.setDiffusionCoefficient(D3);
        }
    }

    /**
     * Runs the simulation, processing each frame according to the simulation model.
     */
    protected void runSimulation() {
        IJ.showStatus("Simulating ...");
        for (int n = 0; n < numFrames; n++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeException("Simulation interrupted");
            }
            processFrame(n);
            IJ.showProgress(n, numFrames);
        }
    }

    /**
     * Processes a single frame of the simulation.
     *
     * @param frameNumber The frame number to process.
     */
    protected void processFrame(int frameNumber) {
        // Initialize the frame in your visualization or data structure
        // For example, reset the image processor for the current frame if visualizing
        ImageProcessor ipSim = initializeFrameProcessor(frameNumber);

        // Handle bleaching at the specified frame
        if (frameNumber == bleachFrame) {
            applyBleaching();
        }

        // Iterate through each simulation step within the current frame
        for (int step = 0; step < stepsPerFrame; step++) {
            for (Particle2D particle : particles) {
                // Update particle positions based on diffusion and potential domain constraints
                updateParticlePosition(particle);
                // Handle bleaching and blinking effects for the particle
                handleBleachingAndBlinking(particle);
                // Reset particle position if it moves out of bounds
                resetParticleIfOutOfBounds(particle);
                // Emit photons for the current frame and update the visualization
                emitPhotonsForFrame(ipSim, particle);
            }
        }

        // Update progress display if applicable
        IJ.showProgress(frameNumber + 1, numFrames);
    }

    /**
     * Initializes the ImageProcessor for a specific frame, applying camera noise and offset.
     *
     * @param frameNumber The frame number to initialize the processor for.
     * @return The initialized ImageProcessor for the specified frame.
     */
    protected ImageProcessor initializeFrameProcessor(int frameNumber) {
        // Get the ImageProcessor for the current frame. Frames in ImageJ are 1-based.
        ImageProcessor ipSim = image.getStack().getProcessor(frameNumber + 1);

        // add the camera offset and a noise term to each pixel
        for (int dx = 0; dx < height; dx++) {
            for (int dy = 0; dy < width; dy++) {
                double random_noise = random.nextGaussian() * sqrtCameraNoiseFactor;
                ipSim.putPixelValue(dx, dy, cameraOffset + random_noise);
            }
        }

        return ipSim; // Return the initialized ImageProcessor for the current frame
    }

    /**
     * Applies bleaching effects to the simulation. This method should be overridden by subclasses
     * to implement specific bleaching behavior.
     */
    protected abstract void applyBleaching();

    /**
     * Updates the position of a particle based on its diffusion coefficient and the time step.
     *
     * @param particle The particle to update.
     */
    protected void updateParticlePosition(Particle2D particle) {
        // Calculate step size based on diffusion coefficient
        double randomRange = Math.sqrt(2 * particle.getDiffusionCoefficient() * tStep);

        double stepSizeX = randomRange * random.nextGaussian();
        double stepSizeY = randomRange * random.nextGaussian();

        // Update particle position
        particle.x += stepSizeX;
        particle.y += stepSizeY;
    }

    /**
     * Handles bleaching and blinking effects for a particle.
     *
     * @param particle The particle to handle effects for.
     */
    protected void handleBleachingAndBlinking(Particle2D particle) {
        // Handle bleaching
        if (bleachFlag && !particle.isBleached()) { // If the particle is not already bleached
            // Assuming bleachFactor is the probability of not bleaching, adjust as necessary
            double bleachChance = random.nextDouble();
            if (bleachChance > bleachFactor) {
                particle.setBleached();
            }
        }

        // Handle blinking
        if (blinkFlag && !particle.isBleached()) { // Only consider blinking if the particle is not bleached
            double blinkChance = random.nextDouble();
            if (particle.isOn() && blinkChance > blinkOffFactor) {
                // If the particle is on and decides to turn off
                particle.setOff();
            } else if (!particle.isOn() && blinkChance > blinkOnFactor) {
                // If the particle is off and decides to turn on
                particle.setOn();
            }
        }
    }

    /**
     * Resets a particle's position if it moves out of bounds. This method should be overridden
     * by subclasses to implement specific out-of-bounds behavior.
     *
     * @param particle The particle to reset if necessary.
     */
    protected abstract void resetParticleIfOutOfBounds(Particle2D particle);

    protected void resetParticle2D(Particle2D particle) {
        double random_position = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
        double random_border = random.nextBoolean() ? sizeLowerLimit : sizeUpperLimit;
        // Randomly choose whether to reset x or y
        if (random.nextBoolean()) {
            // Reset x to either the lower or upper limit
            particle.x = random_border;
            particle.y = random_position;
        } else {
            // Reset y to either the lower or upper limit
            particle.x = random_position;
            particle.y = random_border;
        }
    }

    /**
     * Emits photons for a frame based on the particle's position and state.
     *
     * @param ipSim    The ImageProcessor for the current frame.
     * @param particle The particle to emit photons from.
     */
    protected abstract void emitPhotonsForFrame(ImageProcessor ipSim, Particle2D particle);

    /**
     * Simulates the emission of photons from a particle and updates the image processor accordingly.
     *
     * @param ipSim      The ImageProcessor for the current frame.
     * @param particle   The particle emitting photons.
     * @param numPhotons The number of photons to emit.
     * @param mean       The mean displacement of the photons from the particle's position.
     */
    protected void emitPhotons(ImageProcessor ipSim, Particle2D particle, int numPhotons, double mean) {
        for (int i = 0; i < numPhotons; i++) {
            double photonX = particle.x + random.nextGaussian() * mean;
            double photonY = particle.y + random.nextGaussian() * mean;

            if (Math.abs(photonX) < midPos && Math.abs(photonY) < midPos) {
                int xPixel = (int) ((photonX + midPos) / pixelSize);
                int yPixel = (int) ((photonY + midPos) / pixelSize);

                // Increment the pixel value at the photon's position to simulate photon emission
                double currentValue = ipSim.getPixelValue(xPixel, yPixel);
                ipSim.putPixelValue(xPixel, yPixel, currentValue + 1);
            }
        }
    }
}
