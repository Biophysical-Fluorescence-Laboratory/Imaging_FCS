package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

public class Particle3D extends Particle2D {
    public double z;

    public Particle3D(double x, double y, double z) {
        super(x, y);
        this.z = z;
    }

    public boolean isOutOfBound(double lowerLimit, double upperLimit, double zLowerLimit, double zUpperLimit) {
        return x > upperLimit || x < lowerLimit ||
                y > upperLimit || y < lowerLimit ||
                z > zUpperLimit || z < zLowerLimit;
    }
}
