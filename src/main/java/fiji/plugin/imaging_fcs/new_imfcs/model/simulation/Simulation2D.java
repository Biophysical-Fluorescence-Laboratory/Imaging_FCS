package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

import java.io.File;
import java.util.Random;

public class Simulation2D {
    private static final double PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR = Math.pow(10, 6);
    private static final double OBSERVATION_WAVELENGTH_CONVERSION_FACTOR = Math.pow(10, 9);
    // Refractive index of water
    private static final double REFRACTIVE_INDEX = Math.pow(1.333, 2); // 1.333 is the refractive index of water;
    private final SimulationModel model;
    private final ExpSettingsModel settingsModel;
    private final Random random;
    private double tStep;
    private double darkF;
    private double pixelSizeRealSize;
    private double wavelength;
    private double sigma0;
    private double pixelSize;
    private double pSFSize;
    private double gridSize;
    private double midPos;
    private double sizeLowerLimit;
    private double sizeUpperLimit;
    private double detectorSize;
    private double bleachFactor;
    private double F1;
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
        if (model.getSeed() == 0) {
            random = new Random();
        } else {
            random = new Random(model.getSeed());
        }
    }

    public void simulateACF2D() {
        if (!validateSimulationConditions()) {
            return;
        }

        prepareSimulation();

        if (model.getIsDomain()) {
            initializeDomains();
        }

        initializeParticles();

        runSimulation();

        finalizeSimulation();
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
        pixelSizeRealSize = settingsModel.getPixelSize() / PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR;

        // Calculate the wavelength based on settings and a conversion factor
        wavelength = settingsModel.getEmLambda() / OBSERVATION_WAVELENGTH_CONVERSION_FACTOR;

        // Sigma0 from settings
        sigma0 = settingsModel.getSigma();

        // Calculate the pixel size in object space
        pixelSize = pixelSizeRealSize / settingsModel.getMagnification();

        // Calculate the PSF (Point Spread Function) size
        pSFSize = 0.5 * sigma0 * wavelength / settingsModel.getNA();

        // Calculate the size of the grid (i.e., the size of the pixel area of the detector)
        gridSize = model.getPixelNum() * pixelSize;

        // Calculate the middle position of the grid
        midPos = gridSize / 2.0;

        // Calculate the lower and upper limits of the simulation area
        sizeLowerLimit = -model.getExtFactor() * gridSize;
        sizeUpperLimit = model.getExtFactor() * gridSize;

        // The detector size, effectively half the grid size
        detectorSize = gridSize / 2.0;

        // Calculate the bleaching factor
        // 2.0 to ensure no bleaching if tauBleach is 0
        bleachFactor = model.getTauBleach() != 0 ? Math.exp(-tStep / model.getTauBleach()) : 2.0;

        // Initialize the blinking factors for on and off states
        blinkOnFactor = Math.exp(-tStep * model.getKon());
        blinkOffFactor = Math.exp(-tStep * model.getKoff());

        // Initialize image dimension
        width = model.getPixelNum();
        height = model.getPixelNum();

        F1 = 1.0 - model.getF2() - model.getF3();

        particleGroup1 = Math.round(model.getNumParticles() * F1);
        particleGroup2 = Math.round(model.getNumParticles() * model.getF2());
    }

    private void initializeDomains() {
        double gridLength = sizeUpperLimit - sizeLowerLimit; // Length of the full simulation grid
        int numberOfDomains = (int) Math.ceil(Math.pow(gridLength * Math.pow(10, 6), 2) * model.getDomainDensity());

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
        int numParticles = model.getNumParticles(); // Total number of particles
        particles = new Particle2D[numParticles]; // Initialize particles array

        for (int i = 0; i < numParticles; i++) {
            // Randomly position particles within the simulation area
            double x = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double y = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            particles[i] = new Particle2D(x, y);

            // Determine if the particle is in a dark state based on the dark fraction (darkF)
            if (model.getBlinkFlag() && (int) ((i + 1) * darkF) > (int) (i * darkF)) {
                particles[i].setOff();
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
            for (int particleIndex = 0; particleIndex < particles.length; particleIndex++) {
                // Update particle positions based on diffusion and potential domain constraints
                updateParticlePosition(particles[particleIndex], particleIndex);
                // Handle bleaching and blinking effects for the particle
                handleBleachingAndBlinking(particles[particleIndex]);
                // Reset particle position if it moves out of bounds
                resetParticleIfOutOfBounds(particles[particleIndex]);
            }
        }

        // Emit photons from particles for the current frame and update the visualization or data structure accordingly
        emitPhotonsForFrame(ipSim);

        // Update progress display if applicable
        IJ.showProgress(frameNumber + 1, model.getNumFrames());
    }

    private ImageProcessor initializeFrameProcessor(int frameNumber) {
        // Check if the image stack is initialized; if not, create it
        if (impSim == null) {
            impSim = IJ.createImage("2D Simulation", "GRAY16", width, height, model.getNumFrames());
        }

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
        double bleachCenterX = midPos; // For simplicity, assuming the bleach center is at the middle. Adjust as needed.
        double bleachCenterY = midPos; // For simplicity, assuming the bleach center is at the middle. Adjust as needed.

        for (Particle2D particle : particles) {
            // Calculate the distance of the particle from the bleach center
            double dx = particle.x - bleachCenterX;
            double dy = particle.y - bleachCenterY;
            double distance = Math.sqrt(dx * dx + dy * dy);

            // If the particle is within the bleach radius, mark it as bleached
            if (distance <= bleachRadius) {
                particle.setBleached();
            }
        }
    }

    private double getDiffusionCoefficient(Particle2D particle, int particleIndex) {
        double diffusionCoefficient;
        if (particleIndex < particleGroup1) {
            diffusionCoefficient = model.getD1();
        } else if (particleIndex < particleGroup1 + particleGroup2) {
            diffusionCoefficient = model.getD2();
        } else {
            diffusionCoefficient = model.getD3();
        }

        // Update diffusion coefficient if we have domains
        if (model.getIsDomain() && particle.getDomainIndex() != -1) {
            diffusionCoefficient /= model.getDoutDinRatio();
        }

        return diffusionCoefficient;
    }

    private void updateParticlePosition(Particle2D particle, int particleIndex) {
        // Determine the diffusion coefficient based on the particle index
        double diffusionCoefficient = getDiffusionCoefficient(particle, particleIndex);

        // Calculate step size based on diffusion coefficient
        double stepSizeX = Math.sqrt(2 * diffusionCoefficient * tStep) * random.nextGaussian();
        double stepSizeY = Math.sqrt(2 * diffusionCoefficient * tStep) * random.nextGaussian();

        // Update particle position
        particle.x += stepSizeX;
        particle.y += stepSizeY;
    }

    private void handleBleachingAndBlinking(Particle2D particle) {
        // Handle bleaching
        if (!particle.isBleached()) { // If the particle is not already bleached
            // Assuming bleachFactor is the probability of not bleaching, adjust as necessary
            double bleachChance = random.nextDouble();
            if (bleachChance > bleachFactor) {
                particle.setBleached();
            }
        }

        // Handle blinking
        if (!particle.isBleached()) { // Only consider blinking if the particle is not bleached
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
            // Randomly choose whether to reset x or y
            if (random.nextBoolean()) {
                // Reset x to either the lower or upper limit
                particle.x = random.nextBoolean() ? sizeLowerLimit : sizeUpperLimit;
                particle.y = random_position;
            } else {
                // Reset y to either the lower or upper limit
                particle.y = random.nextBoolean() ? sizeLowerLimit : sizeUpperLimit;
                particle.x = random_position;
            }

            particle.resetBleached();
        }
    }

    private void emitPhotonsForFrame(ImageProcessor ipSim) {
        for (Particle2D particle : particles) {
            if (particle.isOn() && !particle.isBleached()) {
                // Convert particle's continuous position to discrete pixel coordinates
                int xPixel = (int) Math.round((particle.x - sizeLowerLimit) / (sizeUpperLimit - sizeLowerLimit) * (width - 1));
                int yPixel = (int) Math.round((particle.y - sizeLowerLimit) / (sizeUpperLimit - sizeLowerLimit) * (height - 1));

                // Ensure the pixel coordinates are within the bounds of the image
                xPixel = Math.max(0, Math.min(xPixel, width - 1));
                yPixel = Math.max(0, Math.min(yPixel, height - 1));

                // Increment the pixel value at the particle's position to simulate photon emission
                int currentValue = ipSim.getPixel(xPixel, yPixel);
                ipSim.putPixel(xPixel, yPixel, currentValue + 1); // Assuming each particle contributes a single photon's worth of intensity
            }
        }
    }

    private void finalizeSimulation() {
        // Show the simulation results
        if (impSim != null) {
            impSim.show();
            IJ.run(impSim, "Enhance Contrast", "saturated=0.35");
        } else {
            System.out.println("Simulation image stack is null.");
        }

        // Optional: Save the simulation results to a file
        boolean saveResults = true; // Set this based on user input or your requirements
        if (saveResults) {
            saveSimulationResults();
        }
    }

    private void saveSimulationResults() {
        // Define the file path and name for saving the results
        String resultsDirectory = "/path/to/your/results/directory"; // Update this path
        String fileName = "simulationResults.tif"; // Customize the file name as needed

        // Ensure the directory exists
        File dir = new File(resultsDirectory);
        if (!dir.exists()) {
            dir.mkdirs(); // Create the directory if it doesn't exist
        }

        // Construct the full file path
        String filePath = resultsDirectory + File.separator + fileName;

        // Save the image stack as a TIFF file
        if (impSim != null) {
            IJ.saveAsTiff(impSim, filePath);
            System.out.println("Simulation results saved to: " + filePath);
        } else {
            System.out.println("Cannot save simulation results: Image stack is null.");
        }
    }
}
