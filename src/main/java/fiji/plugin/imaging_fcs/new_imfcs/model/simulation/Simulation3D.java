package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

/**
 * A final class for simulating 3D fluorescence microscopy experiments.
 * It extends the SimulationBase class to include 3D-specific functionalities such as handling
 * light sheet thickness and z-axis constraints in particle movement.
 */
public final class Simulation3D extends SimulationBase {
    // Constants
    private static final double REFRACTIVE_INDEX = 1.333; // refractive index of water;
    private static final double Z_EXT_FACTOR = 10;

    // 3D simulation parameters
    private double lightSheetThickness;
    private double sizeZLowerLimit;
    private double sizeZUpperLimit;
    private double zFactor;

    /**
     * Constructs a new Simulation3D instance with specified simulation and experimental settings models.
     *
     * @param model         The simulation model containing parameters for the simulation.
     * @param settingsModel The experimental settings model containing settings such as pixel size and magnification.
     */
    public Simulation3D(SimulationModel model, ExpSettingsModel settingsModel) {
        super(model, settingsModel);
    }

    /**
     * Validates the simulation conditions specific to 3D simulations, such as acceptable light sheet thickness
     * and numerical aperture values. Throws RuntimeException if conditions are not met.
     */
    @Override
    protected void validateSimulationConditions(SimulationModel model, ExpSettingsModel settingsModel) {
        if (settingsModel.getSigmaZ() <= 0) {
            throw new RuntimeException("SigmaZ (LightSheetThickness) can't be <= 0 (3D only)");
        } else if (settingsModel.getSigmaZ() > 100) {
            throw new RuntimeException("SigmaZ (LightSheetThickness) can't be > 100 (3D only)");
        }

        if (settingsModel.getNA() >= 1.33) {
            throw new RuntimeException("For 3D simulations NA has to be smaller than 1.33");
        }
    }

    /**
     * Prepares the simulation by calculating additional 3D-specific parameters and initializing the environment.
     */
    @Override
    protected void prepareSimulation(SimulationModel model, ExpSettingsModel settingsModel) {
        super.prepareSimulation(model, settingsModel);

        lightSheetThickness = settingsModel.getSigmaZ() * wavelength / settingsModel.getNA() / 2.0;
        sizeZLowerLimit = -Z_EXT_FACTOR * lightSheetThickness;
        sizeZUpperLimit = Z_EXT_FACTOR * lightSheetThickness;
        zFactor = settingsModel.getNA() / Math.sqrt(Math.pow(REFRACTIVE_INDEX, 2) - Math.pow(settingsModel.getNA(), 2));
    }

    /**
     * Runs the 3D simulation and returns an ImagePlus object containing the simulated image stack.
     *
     * @return An ImagePlus object containing the results of the 3D simulation.
     */
    public ImagePlus SimulateACF3D() {
        initializeParticles();

        image = IJ.createImage("3D Simulation", "GRAY16", width, height, numFrames);

        runSimulation();
        return image;
    }

    /**
     * Initializes particles with 3D positions within the simulation bounds.
     */
    private void initializeParticles() {
        particles = new Particle3D[numParticles];

        for (int i = 0; i < numParticles; i++) {
            double x = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double y = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double z = sizeZLowerLimit + random.nextDouble() * (sizeZUpperLimit - sizeZLowerLimit);
            particles[i] = new Particle3D(x, y, z);

            setParticleState(particles[i], i);
        }
    }

    /**
     * Applies bleaching effects to the simulation. This method is overridden with no implementation for 3D simulations.
     */
    @Override
    protected void applyBleaching() {
        // No implementation using radius for bleaching in 3D
    }

    /**
     * Updates the position of a particle based on its diffusion coefficient and the time step, including z-axis movement.
     *
     * @param particle The particle to update.
     */
    @Override
    protected void updateParticlePosition(Particle2D particle) {
        // update particle position for x and y
        super.updateParticlePosition(particle);

        double stepSizeZ = Math.sqrt(2 * particle.getDiffusionCoefficient() * tStep) * random.nextGaussian();
        ((Particle3D) particle).z += stepSizeZ;
    }

    /**
     * Resets a particle's position if it moves out of bounds, considering the z-axis.
     *
     * @param particleToCast The particle to reset if necessary.
     */
    @Override
    protected void resetParticleIfOutOfBounds(Particle2D particleToCast) {
        Particle3D particle = (Particle3D) particleToCast;

        if (particle.isOutOfBound(sizeLowerLimit, sizeUpperLimit, sizeZLowerLimit, sizeZUpperLimit)) {
            if (random.nextBoolean()) {
                // resample at random z position
                resetParticle2D(particle);
                particle.z = sizeZLowerLimit + random.nextDouble() * (sizeZUpperLimit - sizeZLowerLimit);
            } else {
                // resample at z-boundary
                particle.x = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
                particle.y = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
                particle.z = random.nextBoolean() ? sizeZLowerLimit : sizeZUpperLimit;
            }
        }

        particle.resetBleached();
    }

    /**
     * Emits photons for a frame based on the particle's position and state, adjusted for 3D simulations.
     *
     * @param ipSim      The ImageProcessor for the current frame.
     * @param particle2D The particle to emit photons from.
     */
    @Override
    protected void emitPhotonsForFrame(ImageProcessor ipSim, Particle2D particle2D) {
        // If the particle is off or bleached, do nothing
        if (!particle2D.isOn() || particle2D.isBleached()) {
            return;
        }
        Particle3D particle = (Particle3D) particle2D;

        double zCor = (PSFSize + (Math.abs(particle.z) * (zFactor / 2)));
        int randomPoisson = random.nextPoisson(tStep * CPS);
        int numPhotons = (int) Math.round(Math.abs(
                randomPoisson * Math.exp(-0.5 * Math.pow(particle.z / lightSheetThickness, 2))));

        emitPhotons(ipSim, particle, numPhotons, zCor);
    }
}
