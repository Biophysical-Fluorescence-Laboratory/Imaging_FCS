package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public abstract class SimulationBase {
    protected static final double OBSERVATION_WAVELENGTH_CONVERSION_FACTOR = Math.pow(10, 9);

    protected final SimulationModel model;
    protected final ExpSettingsModel settingsModel;
    protected final RandomCustom random;
    protected double tStep;
    protected double darkF;
    protected double pixelSize;
    protected double wavelength;
    protected double PSFSize;
    protected double midPos;
    protected double sizeLowerLimit;
    protected double sizeUpperLimit;
    protected double bleachFactor;
    protected boolean bleachFlag;
    protected long particleGroup1;
    protected long particleGroup2;
    protected double blinkOnFactor;
    protected double blinkOffFactor;

    protected ImagePlus image; // Assuming this is your image stack for the simulation
    protected int width; // Width of the simulation area in pixels
    protected int height; // Height of the simulation area in pixels

    protected Particle2D[] particles;

    protected SimulationBase(SimulationModel model, ExpSettingsModel settingsModel) {
        this.model = model;
        this.settingsModel = settingsModel;

        // Validate that we can run the simulation, otherwise throw a runtime exception
        validateSimulationConditions();

        // Set random with the seed if it's different to 0 to make results reproducible
        // Use a custom random class to add a poisson generator
        if (model.getSeed() < 0) {
            random = new RandomCustom();
        } else {
            random = new RandomCustom(model.getSeed());
        }
    }

    protected abstract void validateSimulationConditions();

    protected void prepareSimulation() {
        // Calculate the time step based on frame time and steps per frame
        tStep = model.getFrameTime() / model.getStepsPerFrame();

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

        // Initialize the blinking factors for on and off states
        blinkOnFactor = Math.exp(-tStep * model.getKon());
        blinkOffFactor = Math.exp(-tStep * model.getKoff());

        // Initialize image dimension
        width = model.getPixelNum();
        height = model.getPixelNum();

        double f1 = 1.0 - model.getF2() - model.getF3();

        // define the groups size for particles
        particleGroup1 = Math.round(model.getNumParticles() * f1);
        particleGroup2 = Math.round(model.getNumParticles() * model.getF2());
    }

    protected void setParticleState(Particle2D particle, int i) {
        // Determine if the particle is in a dark state based on the dark fraction (darkF)
        if (model.getBlinkFlag() && (int) ((i + 1) * darkF) > (int) (i * darkF)) {
            particle.setOff();
        }

        // Determine particle diffusion coefficient based on group
        if (i < particleGroup1) {
            particle.setDiffusionCoefficient(model.getD1());
        } else if (i < particleGroup1 + particleGroup2) {
            particle.setDiffusionCoefficient(model.getD2());
        } else {
            particle.setDiffusionCoefficient(model.getD3());
        }
    }

    protected void runSimulation() {
        IJ.showStatus("Simulating ...");
        for (int n = 0; n < model.getNumFrames(); n++) {
            if (Thread.currentThread().isInterrupted()) {
                throw new RuntimeException("Simulation interrupted");
            }
            processFrame(n);
            IJ.showProgress(n, model.getNumFrames());
        }
    }

    protected void processFrame(int frameNumber) {
        // Initialize the frame in your visualization or data structure
        // For example, reset the image processor for the current frame if visualizing
        ImageProcessor ipSim = initializeFrameProcessor(frameNumber);

        // Handle bleaching at the specified frame
        if (frameNumber == model.getBleachFrame()) {
            applyBleaching();
        }

        // Iterate through each simulation step within the current frame
        for (int step = 0; step < model.getStepsPerFrame(); step++) {
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
        IJ.showProgress(frameNumber + 1, model.getNumFrames());
    }

    protected ImageProcessor initializeFrameProcessor(int frameNumber) {
        // Get the ImageProcessor for the current frame. Frames in ImageJ are 1-based.
        ImageProcessor ipSim = image.getStack().getProcessor(frameNumber + 1);

        // add the camera offset and a noise term to each pixel
        for (int dx = 0; dx < model.getPixelNum(); dx++) {
            for (int dy = 0; dy < model.getPixelNum(); dy++) {
                double random_noise = random.nextGaussian() * Math.sqrt(model.getCameraNoiseFactor());
                ipSim.putPixelValue(dx, dy, model.getCameraOffset() + random_noise);
            }
        }

        return ipSim; // Return the initialized ImageProcessor for the current frame
    }

    protected abstract void applyBleaching();

    protected void updateParticlePosition(Particle2D particle) {
        // Calculate step size based on diffusion coefficient
        double stepSizeX = Math.sqrt(2 * particle.getDiffusionCoefficient() * tStep) * random.nextGaussian();
        double stepSizeY = Math.sqrt(2 * particle.getDiffusionCoefficient() * tStep) * random.nextGaussian();

        // Update particle position
        particle.x += stepSizeX;
        particle.y += stepSizeY;
    }

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
        if (model.getBlinkFlag() && !particle.isBleached()) { // Only consider blinking if the particle is not bleached
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

    protected abstract void emitPhotonsForFrame(ImageProcessor ipSim, Particle2D particle);

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
