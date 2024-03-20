package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

/**
 * Represents a circular domain in a two-dimensional space.
 * This class is used to define a circular area and determine
 * whether a given particle is within this domain.
 */
public class Domain {
    public final double x;
    public final double y;
    public final double radius;

    /**
     * Constructs a new {@code Domain} instance with specified center coordinates and radius.
     *
     * @param x      the x-coordinate of the domain's center.
     * @param y      the y-coordinate of the domain's center.
     * @param radius the radius of the domain.
     */
    public Domain(double x, double y, double radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    /**
     * Determines if a given {@code Particle2D} is inside the domain.
     * This method checks if the distance between the particle and the domain's center
     * is less than the domain's radius. It uses the squared distance for efficiency,
     * avoiding the computational cost of square root operations.
     *
     * @param particle the {@code Particle2D} to check.
     * @return {@code true} if the particle is inside the domain; {@code false} otherwise.
     */
    public boolean isParticleInsideDomain(Particle2D particle) {
        double dx = particle.x - x;
        double dy = particle.y - y;
        double distanceSq = dx * dx + dy * dy;

        // Do not use square root for fastest computation
        return distanceSq < radius * radius;
    }
}
