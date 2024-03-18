package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * A final class for simulating 2D fluorescence microscopy experiments. It extends the SimulationBase class,
 * incorporating domain-specific functionalities that affect particle diffusion. Domains are areas within the simulation
 * environment where particles may have different diffusion behaviors.
 */
public final class Simulation2D extends SimulationBase {
    private double[][] domains;

    /**
     * Constructs a Simulation2D instance with specified simulation and experimental settings models.
     *
     * @param model         The simulation model containing the parameters for the simulation.
     * @param settingsModel The experimental settings model containing settings such as pixel size and magnification.
     */
    public Simulation2D(SimulationModel model, ExpSettingsModel settingsModel) {
        super(model, settingsModel);
    }

    /**
     * Validates the simulation conditions specific to 2D simulations, such as the ratio of diffusion coefficients outside
     * and inside domains. Throws RuntimeException if conditions are not met.
     */
    @Override
    protected void validateSimulationConditions() {
        if (model.getDoutDinRatio() <= 0) {
            throw new RuntimeException("Dout / Din <= 0 is not allowed");
        }
    }

    /**
     * Runs the 2D simulation and returns an ImagePlus object containing the simulated image stack.
     *
     * @return An ImagePlus object containing the results of the 2D simulation.
     */
    public ImagePlus simulateACF2D() {
        prepareSimulation();

        if (model.getIsDomain()) {
            initializeDomains();
        }

        initializeParticles();

        // create the stack of images
        image = IJ.createImage("2D Simulation", "GRAY16", width, height, model.getNumFrames());

        runSimulation();

        return image;
    }

    /**
     * Initializes domains within the simulation area. Domains are circular areas where particles can have different
     * diffusion behaviors. This method randomly places domains without overlap and assigns them a radius.
     */
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
            throw new RuntimeException("Domains too dense, cannot place them without overlap.");
        }
    }

    /**
     * Initializes particles within the simulation area. Particles are positioned randomly. If domains are used,
     * each particle is also assigned to a domain if it falls within one.
     */
    private void initializeParticles() {
        particles = new Particle2D[model.getNumParticles()]; // Initialize particles array

        for (int i = 0; i < model.getNumParticles(); i++) {
            // Randomly position particles within the simulation area
            double x = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double y = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            particles[i] = new Particle2D(x, y);

            setParticleState(particles[i], i);
        }

        // If domains are used, determine each particle's domain
        if (model.getIsDomain()) {
            assignParticlesToDomains();
        }
    }

    /**
     * Assigns particles to domains based on their positions. Particles within a domain have their diffusion
     * coefficient adjusted according to the domain's properties.
     */
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

    /**
     * Applies bleaching effects to the simulation based on a predefined bleach radius. Particles within the bleach radius
     * are marked as bleached.
     */
    @Override
    protected void applyBleaching() {
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

    /**
     * Resets a particle's position if it moves out of bounds. This method also resets the bleached state of the particle.
     *
     * @param particle The particle to reset if necessary.
     */
    @Override
    protected void resetParticleIfOutOfBounds(Particle2D particle) {
        if (particle.isOutOfBound(sizeLowerLimit, sizeUpperLimit)) {
            resetParticle2D(particle);
            particle.resetBleached();
        }
    }

    /**
     * Emits photons for a frame based on the particle's position and state. This method is adjusted for 2D simulations,
     * considering whether the particle is in an on or off state, and whether it has been bleached.
     *
     * @param ipSim    The ImageProcessor for the current frame.
     * @param particle The particle to emit photons from.
     */
    @Override
    protected void emitPhotonsForFrame(ImageProcessor ipSim, Particle2D particle) {
        // If the particle is off or bleached, do nothing
        if (!particle.isOn() || particle.isBleached()) {
            return;
        }

        int numPhotons = random.nextPoisson(tStep * model.getCPS());
        emitPhotons(ipSim, particle, numPhotons, PSFSize);
    }
}
