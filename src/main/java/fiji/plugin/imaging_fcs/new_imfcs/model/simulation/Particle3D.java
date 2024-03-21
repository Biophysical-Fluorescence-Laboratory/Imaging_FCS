package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

/**
 * Extends the Particle2D class to represent a 3-dimensional particle for use in fluorescence correlation spectroscopy (FCS) simulations.
 */
public class Particle3D extends Particle2D {
    public double z;

    /**
     * Constructs a Particle3D with specified initial position.
     *
     * @param x Initial x-coordinate of the particle.
     * @param y Initial y-coordinate of the particle.
     * @param z Initial z-coordinate of the particle.
     */
    public Particle3D(double x, double y, double z) {
        super(x, y);
        this.z = z;
    }

    /**
     * Checks if the particle is out of the specified bounds in 3D space.
     *
     * @param lowerLimit  Lower limit of the simulation area in the x and y axes.
     * @param upperLimit  Upper limit of the simulation area in the x and y axes.
     * @param zLowerLimit Lower limit of the simulation area in the z-axis.
     * @param zUpperLimit Upper limit of the simulation area in the z-axis.
     * @return True if the particle is outside the bounds, false otherwise.
     */
    public boolean isOutOfBound(double lowerLimit, double upperLimit, double zLowerLimit, double zUpperLimit) {
        return x > upperLimit || x < lowerLimit ||
                y > upperLimit || y < lowerLimit ||
                z > zUpperLimit || z < zLowerLimit;
    }
}
