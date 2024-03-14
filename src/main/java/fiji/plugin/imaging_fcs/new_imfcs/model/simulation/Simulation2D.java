package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public class Simulation2D {
    private static final double OBSERVATION_WAVELENGTH_CONVERSION_FACTOR = Math.pow(10, 9);
    // Refractive index of water
    private final SimulationModel model;
    private final ExpSettingsModel settingsModel;
    private final RandomCustom random;
    private double tStep;
    private double darkF;
    private double pixelSize;
    private double PSFSize;
    private double midPos;
    private double sizeLowerLimit;
    private double sizeUpperLimit;
    private double bleachFactor;
    private boolean bleachFlag;
    private long particleGroup1;
    private long particleGroup2;
    private double blinkOnFactor;
    private double blinkOffFactor;
    private double[][] domains;
    private Particle2D[] particles;
    private ImagePlus impSim; // Assuming this is your image stack for the simulation
    private int width; // Width of the simulation area in pixels
    private int height; // Height of the simulation area in pixels

    public Simulation2D(SimulationModel model, ExpSettingsModel settingsModel) {
        this.model = model;
        this.settingsModel = settingsModel;

        // Set random with the seed if it's different to 0 to make results reproducible
        // Use a custom random class to add a poisson generator
        if (model.getSeed() == 0) {
            random = new RandomCustom();
        } else {
            random = new RandomCustom(model.getSeed());
        }
    }

    public ImagePlus simulateACF2D() {
        if (!validateSimulationConditions()) {
            return null;
        }

        prepareSimulation();

        if (model.getIsDomain()) {
            initializeDomains();
        }

        initializeParticles();

        // create the stack of images
        impSim = IJ.createImage("2D Simulation", "GRAY16", width, height, model.getNumFrames());

        runSimulation();

        return impSim;
    }

    private boolean validateSimulationConditions() {
        if (model.getDoutDinRatio() <= 0) {
            IJ.showMessage("Dout / Din <= 0 is not allowed");
            return false;
        }
        return true;
    }

    private void prepareSimulation() {
        // Calculate the time step based on frame time and steps per frame
        tStep = model.getFrameTime() / model.getStepsPerFrame();

        // Calculate the fraction of molecules in the dark state
        darkF = model.getKoff() / (model.getKoff() + model.getKon());

        // Calculate real pixel size based on settings and a conversion factor
        double pixelSizeRealSize = settingsModel.getPixelSize() / SimulationModel.PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;

        // Calculate the wavelength based on settings and a conversion factor
        double wavelength = settingsModel.getEmLambda() / OBSERVATION_WAVELENGTH_CONVERSION_FACTOR;

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

    private void initializeDomains() {
        double gridLength = sizeUpperLimit - sizeLowerLimit; // Length of the full simulation grid
        int numberOfDomains = (int) Math.ceil(
                Math.pow(gridLength * SimulationModel.PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR, 2) * model.getDomainDensity());

        domains = new double[numberOfDomains][3]; // Initialize domains array

        int attempts = 0;
        int createdDomains = 0;
        while (createdDomains < numberOfDomains && attempts < numberOfDomains * 10) {
            double x = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double y = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double radius = model.getDomainRadius() + random.nextGaussian() * (model.getDomainRadius() / 10);

            boolean overlap = false;
            for (int i = 0; i < createdDomains; i++) {
                double dx = x - domains[i][0];
                double dy = y - domains[i][1];
                if (Math.sqrt(dx * dx + dy * dy) < (radius + domains[i][2])) {
                    overlap = true;
                    break;
                }
            }

            if (!overlap) {
                domains[createdDomains][0] = x;
                domains[createdDomains][1] = y;
                domains[createdDomains][2] = radius;
                createdDomains++;
            } else {
                attempts++;
            }
        }

        if (attempts >= numberOfDomains * 10) {
            IJ.showMessage("Domains too dense, cannot place them without overlap.");
            IJ.showStatus("Simulation Error");
            IJ.showProgress(1);
            // TODO: Stop simulation execution
        }
    }

    private void initializeParticles() {
        particles = new Particle2D[model.getNumParticles()]; // Initialize particles array

        for (int i = 0; i < model.getNumParticles(); i++) {
            // Randomly position particles within the simulation area
            double x = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double y = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            particles[i] = new Particle2D(x, y);

            // Determine if the particle is in a dark state based on the dark fraction (darkF)
            if (model.getBlinkFlag() && (int) ((i + 1) * darkF) > (int) (i * darkF)) {
                particles[i].setOff();
            }

            // Determine particle diffusion coefficient based on group
            if (i < particleGroup1) {
                particles[i].setDiffusionCoefficient(model.getD1());
            } else if (i < particleGroup1 + particleGroup2) {
                particles[i].setDiffusionCoefficient(model.getD2());
            } else {
                particles[i].setDiffusionCoefficient(model.getD3());
            }
        }

        // If domains are used, determine each particle's domain
        if (model.getIsDomain()) {
            assignParticlesToDomains();
        }
    }

    private void assignParticlesToDomains() {
        for (Particle2D particle : particles) {
            for (int j = 0; j < domains.length; j++) {
                double domainX = domains[j][0];
                double domainY = domains[j][1];
                double radius = domains[j][2];

                double distance = Math.sqrt(Math.pow(particle.x - domainX, 2) + Math.pow(particle.y - domainY, 2));
                if (distance < radius) {
                    particle.setDomainIndex(j); // Assign particle to the domain j
                    // Update diffusion coefficient if it's in domain
                    particle.setDiffusionCoefficient(particle.getDiffusionCoefficient() / model.getDoutDinRatio());
                    break; // Break the loop once a domain is assigned
                }
            }
        }
    }

    private void runSimulation() {
        IJ.showStatus("Simulating ...");
        for (int n = 0; n < model.getNumFrames(); n++) {
            if (Thread.currentThread().isInterrupted()) {
                IJ.showStatus("Simulation Interrupted");
                IJ.showProgress(1);
                return;
            }
            processFrame(n);
            IJ.showProgress(n, model.getNumFrames());
        }
    }

    private void processFrame(int frameNumber) {
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

    private ImageProcessor initializeFrameProcessor(int frameNumber) {
        // Get the ImageProcessor for the current frame. Frames in ImageJ are 1-based.
        ImageProcessor ipSim = impSim.getStack().getProcessor(frameNumber + 1);

        // add the camera offset and a noise term to each pixel
        for (int dx = 0; dx < model.getPixelNum(); dx++) {
            for (int dy = 0; dy < model.getPixelNum(); dy++) {
                double random_noise = random.nextGaussian() * Math.sqrt(model.getCameraNoiseFactor());
                ipSim.putPixelValue(dx, dy, model.getCameraOffset() + random_noise);
            }
        }

        return ipSim; // Return the initialized ImageProcessor for the current frame
    }

    private void applyBleaching() {
        // Assuming a bleach radius and a center for the bleach spot. Adjust as needed.
        double bleachRadius = model.getBleachRadius();

        for (Particle2D particle : particles) {
            // Calculate the distance of the particle from the bleach center (0, 0)
            double dx = particle.x - 0;
            double dy = particle.y - 0;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // If the particle is within the bleach radius, mark it as bleached
            if (distance <= bleachRadius) {
                particle.setBleached();
            }
        }
    }

    private void updateParticlePosition(Particle2D particle) {
        // Calculate step size based on diffusion coefficient
        double stepSizeX = Math.sqrt(2 * particle.getDiffusionCoefficient() * tStep) * random.nextGaussian();
        double stepSizeY = Math.sqrt(2 * particle.getDiffusionCoefficient() * tStep) * random.nextGaussian();

        // Update particle position
        particle.x += stepSizeX;
        particle.y += stepSizeY;
    }

    private void handleBleachingAndBlinking(Particle2D particle) {
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

    private void resetParticleIfOutOfBounds(Particle2D particle) {
        if (particle.isOutOfBound(sizeLowerLimit, sizeUpperLimit)) {
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

            particle.resetBleached();
        }
    }

    private void emitPhotonsForFrame(ImageProcessor ipSim, Particle2D particle) {
        // If the particle is off or bleached, do nothing
        if (!particle.isOn() || particle.isBleached()) {
            return;
        }

        int numPhotons = random.nextPoisson(tStep * model.getCPS());
        for (int i = 0; i < numPhotons; i++) {
            double photonX = particle.x + random.nextGaussian() * PSFSize;
            double photonY = particle.y + random.nextGaussian() * PSFSize;

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
