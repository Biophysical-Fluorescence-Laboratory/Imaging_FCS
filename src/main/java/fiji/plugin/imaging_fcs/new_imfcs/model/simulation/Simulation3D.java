package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.SimulationModel;
import ij.IJ;
import ij.ImagePlus;
import ij.process.ImageProcessor;

public final class Simulation3D extends SimulationBase {
    private static final double REFRACTIVE_INDEX = 1.333; // refractive index of water;
    private static final double Z_EXT_FACTOR = 10;
    private double lightSheetThickness;
    private double sizeZLowerLimit;
    private double sizeZUpperLimit;
    private double zFactor;

    public Simulation3D(SimulationModel model, ExpSettingsModel settingsModel) {
        super(model, settingsModel);
    }

    @Override
    protected void validateSimulationConditions() {
        if (settingsModel.getSigmaZ() <= 0) {
            throw new RuntimeException("SigmaZ (LightSheetThickness) can't be <= 0 (3D only)");
        } else if (settingsModel.getSigmaZ() > 100) {
            throw new RuntimeException("SigmaZ (LightSheetThickness) can't be > 100 (3D only)");
        }

        if (settingsModel.getNA() >= 1.33) {
            throw new RuntimeException("For 3D simulations NA has to be smaller than 1.33");
        }
    }

    public ImagePlus SimulateACF3D() {
        prepareSimulation();

        initializeParticles();

        image = IJ.createImage("3D Simulation", "GRAY16", width, height, model.getNumFrames());

        runSimulation();
        return image;
    }

    @Override
    protected void prepareSimulation() {
        super.prepareSimulation();

        lightSheetThickness = settingsModel.getSigmaZ() * wavelength / settingsModel.getNA() / 2.0;
        sizeZLowerLimit = -Z_EXT_FACTOR * lightSheetThickness;
        sizeZUpperLimit = Z_EXT_FACTOR * lightSheetThickness;
        zFactor = settingsModel.getNA() / Math.sqrt(Math.pow(REFRACTIVE_INDEX, 2) - Math.pow(settingsModel.getNA(), 2));
    }

    private void initializeParticles() {
        particles = new Particle3D[model.getNumParticles()];

        for (int i = 0; i < model.getNumParticles(); i++) {
            double x = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double y = sizeLowerLimit + random.nextDouble() * (sizeUpperLimit - sizeLowerLimit);
            double z = sizeZLowerLimit + random.nextDouble() * (sizeZUpperLimit - sizeZLowerLimit);
            particles[i] = new Particle3D(x, y, z);

            setParticleState(particles[i], i);
        }
    }

    @Override
    protected void applyBleaching() {
        return; // No implementation using radius for bleaching in 3D
    }

    @Override
    protected void updateParticlePosition(Particle2D particle) {
        // update particle position for x and y
        super.updateParticlePosition(particle);

        double stepSizeZ = Math.sqrt(2 * particle.getDiffusionCoefficient() * tStep) * random.nextGaussian();
        ((Particle3D) particle).z += stepSizeZ;
    }

    @Override
    protected void resetParticleIfOutOfBounds(Particle2D particle2D) {
        Particle3D particle = (Particle3D) particle2D;

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

    @Override
    protected void emitPhotonsForFrame(ImageProcessor ipSim, Particle2D particle2D) {
        // If the particle is off or bleached, do nothing
        if (!particle2D.isOn() || particle2D.isBleached()) {
            return;
        }
        Particle3D particle = (Particle3D) particle2D;

        double zCor = (PSFSize + (Math.abs(particle.z) * (zFactor / 2)));
        int randomPoisson = random.nextPoisson(tStep * model.getCPS());
        int numPhotons = (int) Math.round(Math.abs(
                randomPoisson * Math.exp(-0.5 * Math.pow(particle.z / lightSheetThickness, 2))));

        emitPhotons(ipSim, particle, numPhotons, zCor);
    }
}
