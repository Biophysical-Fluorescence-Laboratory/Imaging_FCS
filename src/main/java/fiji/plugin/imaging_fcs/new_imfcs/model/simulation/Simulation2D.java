package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public final class Simulation2D extends SimulationBase {
    private double[][] domains;

    public Simulation2D(SimulationModel model, ExpSettingsModel settingsModel) {
        super(model, settingsModel);
    }

    @Override
    protected void validateSimulationConditions() {
        if (model.getDoutDinRatio() <= 0) {
            throw new RuntimeException("Dout / Din <= 0 is not allowed");
        }
    }

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

    @Override
    protected void resetParticleIfOutOfBounds(Particle2D particle) {
        if (particle.isOutOfBound(sizeLowerLimit, sizeUpperLimit)) {
            resetParticle2D(particle);
            particle.resetBleached();
        }
    }

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
