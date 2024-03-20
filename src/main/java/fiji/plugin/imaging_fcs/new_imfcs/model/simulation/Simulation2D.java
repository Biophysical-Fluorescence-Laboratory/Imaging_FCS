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
    private static final int DOMAIN_MAX_ATTEMPTS = 10;

    // Define the hashmap of domains
    private DomainHashMap domains;
    private double domainDensity, domainRadius, DoutDinRatio, pIn, pOut;
    private double meshWorkSize, hopProbability;
    private double bleachRadius;
    private boolean isDomain, isMesh;

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
    protected void validateSimulationConditions(SimulationModel model, ExpSettingsModel settingsModel) {
        if (model.getDoutDinRatio() <= 0) {
            throw new RuntimeException("Dout / Din <= 0 is not allowed");
        }
    }

    /**
     * Prepares the simulation by calculating additional 2D-specific parameters and initializing the environment.
     */
    @Override
    protected void prepareSimulation(SimulationModel model, ExpSettingsModel settingsModel) {
        super.prepareSimulation(model, settingsModel);

        isDomain = model.getIsDomain();
        domainDensity = model.getDomainDensity();
        domainRadius = model.getDomainRadius();
        DoutDinRatio = model.getDoutDinRatio();
        pIn = model.getPin();
        pOut = model.getPout();

        isMesh = model.getIsMesh();
        meshWorkSize = model.getMeshWorkSize();
        hopProbability = model.getHopProbability();

        bleachRadius = model.getBleachRadius();
    }

    /**
     * Runs the 2D simulation and returns an ImagePlus object containing the simulated image stack.
     *
     * @return An ImagePlus object containing the results of the 2D simulation.
     */
    @Override
    public ImagePlus simulate() {
        if (isDomain) {
            initializeDomains();
        }

        initializeParticles();

        // create the stack of images
        image = IJ.createImage("2D Simulation", "GRAY16", width, height, numFrames);

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
                Math.pow(gridLength * SimulationModel.PIXEL_SIZE_REAL_SPACE_CONVERSION_FACTOR, 2) * domainDensity);

        double cellSize = domainRadius * 2;
        domains = new DomainHashMap(cellSize);

        int attempts = 0;
        int createdDomains = 0;
        while (createdDomains < numberOfDomains && attempts < numberOfDomains * DOMAIN_MAX_ATTEMPTS) {
            double x = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double y = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double radius = domainRadius + random.nextGaussian() * (domainRadius / 10);

            Domain domain = new Domain(x, y, radius);

            if (domains.hasOverlap(domain)) {
                attempts++;
            } else {
                domains.insert(domain);
                createdDomains++;
            }
        }

        if (attempts >= numberOfDomains * DOMAIN_MAX_ATTEMPTS) {
            throw new RuntimeException("Domains too dense, cannot place them without overlap.");
        }
    }

    /**
     * Initializes particles within the simulation area. Particles are positioned randomly.
     */
    private void initializeParticles() {
        particles = new Particle2D[numParticles]; // Initialize particles array

        for (int i = 0; i < numParticles; i++) {
            // Randomly position particles within the simulation area
            double x = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double y = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            particles[i] = new Particle2D(x, y);

            setParticleState(particles[i], i);
        }
    }

    /**
     * Updates the position of a particle based on simple meshWork grid diffusion simulation.
     * The movement is constrained by the meshWork size, with a certain probability to hop across mesh cells.
     *
     * @param particle the particle to update.
     */
    private void updateParticlePositionWithMesh(Particle2D particle) {
        // simulate diffusion on a simple meshWork grid
        double randomRange = Math.sqrt(2 * particle.getDiffusionCoefficient() * tStep);
        // Calculate step size based on diffusion coefficient
        double stepSizeX = randomRange * random.nextGaussian();
        double stepSizeY = randomRange * random.nextGaussian();

        // if hop is not true, step inside the mesh only
        if (!(hopProbability > random.nextDouble())) {
            while (Math.floor(particle.x / meshWorkSize) !=
                    Math.floor((particle.x + stepSizeX) / meshWorkSize)) {
                stepSizeX = randomRange * random.nextGaussian();
            }

            while (Math.floor(particle.y / meshWorkSize) !=
                    Math.floor((particle.y + stepSizeY) / meshWorkSize)) {
                stepSizeY = randomRange * random.nextGaussian();
            }
        }

        // Update particle position
        particle.x += stepSizeX;
        particle.y += stepSizeY;
    }

    /**
     * Calculates the intersection point of a particle's motion with the boundary of a domain.
     *
     * @param px     The x-coordinate of the particle's initial position.
     * @param py     The y-coordinate of the particle's initial position.
     * @param dx     The x-component of the particle's motion vector.
     * @param dy     The y-component of the particle's motion vector.
     * @param radius The radius of the circular domain centered at the origin.
     * @return The scalar distance along the motion vector to the domain's boundary intersection.
     */
    private double computeIntersection(double px, double py, double dx, double dy, double radius) {
        double a = dx * dx + dy * dy;
        double b = -(px * dx + py * dy);
        double c = Math.sqrt(-Math.pow(py * dx - px * dy, 2) + Math.pow(dx * radius, 2) + Math.pow(dy * radius, 2));

        return (b + c) / a;
    }

    /**
     * Updates the position of a particle considering domain boundaries. This includes adjusting the diffusion coefficient
     * if the particle is within a domain and handling crossing between domains and the open simulation area.
     *
     * @param particle the particle to update.
     */
    private void updateParticlePositionWithDomain(Particle2D particle) {
        Domain domain = domains.findDomainForParticle(particle);
        double diffusionCoefficient = particle.getDiffusionCoefficient();

        // update the diffusion coefficient if the particle is in a domain
        if (domain != null) {
            diffusionCoefficient /= DoutDinRatio;
        }

        double randomRange = Math.sqrt(2 * diffusionCoefficient * tStep);
        // Calculate step size based on diffusion coefficient
        double stepSizeX = randomRange * random.nextGaussian();
        double stepSizeY = randomRange * random.nextGaussian();

        boolean crossInOut = pOut > random.nextDouble();
        boolean crossOutIn = pIn > random.nextDouble();

        Particle2D particleAfterMove = new Particle2D(particle.x + stepSizeX, particle.y + stepSizeY);
        Domain domainAfterMove = domains.findDomainForParticle(particleAfterMove);

        if (domain != null && !domain.equals(domainAfterMove)) {
            // Particle is attempting to move out of the domain, which is possible.
            if (crossInOut) {
                // Compute the position of the particle relative to the domain center
                double relativePositionX = particle.x - domain.x;
                double relativePositionY = particle.y - domain.y;

                double intersection = computeIntersection(relativePositionX, relativePositionY, stepSizeX, stepSizeY,
                        domain.radius);

                stepSizeX *= (intersection + (1 - intersection) * Math.sqrt(DoutDinRatio));
                stepSizeY *= (intersection + (1 - intersection) * Math.sqrt(DoutDinRatio));
            } else {
                // Generate a new position that stays inside the domain
                while (!domain.equals(domainAfterMove)) {
                    stepSizeX = randomRange * random.nextGaussian();
                    stepSizeY = randomRange * random.nextGaussian();
                    particleAfterMove.x = particle.x + stepSizeX;
                    particleAfterMove.y = particle.y + stepSizeY;

                    domainAfterMove = domains.findDomainForParticle(particleAfterMove);
                }
            }
        } else if (domain == null && domainAfterMove != null) {
            // Particle is attempting to enter a domain, which is possible
            if (crossOutIn) {
                // Compute the position of the particle relative to the domain
                double relativePositionX = particle.x - domainAfterMove.x;
                double relativePositionY = particle.y - domainAfterMove.y;

                double intersection = computeIntersection(relativePositionX, relativePositionY, stepSizeX, stepSizeY,
                        domainAfterMove.radius);

                stepSizeX *= (intersection + (1 - intersection) / Math.sqrt(DoutDinRatio));
                stepSizeY *= (intersection + (1 - intersection) / Math.sqrt(DoutDinRatio));
            } else {
                // Generate a new position that stays outside a domain
                while (domainAfterMove != null) {
                    stepSizeX = randomRange * random.nextGaussian();
                    stepSizeY = randomRange * random.nextGaussian();
                    particleAfterMove.x = particle.x + stepSizeX;
                    particleAfterMove.y = particle.y + stepSizeY;

                    domainAfterMove = domains.findDomainForParticle(particleAfterMove);
                }
            }
        }

        particle.x += stepSizeX;
        particle.y += stepSizeY;
    }

    /**
     * Updates the position of a particle. This method decides whether to use the base class method,
     * mesh-based movement or domain-based movement
     *
     * @param particle the particle to update.
     */
    protected void updateParticlePosition(Particle2D particle) {
        if (!isDomain && isMesh) {
            updateParticlePositionWithMesh(particle);
        } else if (isDomain && !isMesh) {
            updateParticlePositionWithDomain(particle);
        } else {
            super.updateParticlePosition(particle);
        }
    }

    /**
     * Applies bleaching effects to the simulation based on a predefined bleach radius. Particles within the bleach radius
     * are marked as bleached.
     */
    @Override
    protected void applyBleaching() {
        for (Particle2D particle : particles) {
            // Calculate the distance of the particle from the bleach center (0, 0)
            double dx = particle.x - 0;
            double dy = particle.y - 0;
            double distanceSq = dx * dx + dy * dy;

            // If the particle is within the bleach radius, mark it as bleached
            if (distanceSq <= bleachRadius * bleachRadius) {
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

        int numPhotons = random.nextPoisson(tStep * CPS);
        emitPhotons(ipSim, particle, numPhotons, PSFSize);
    }
}
