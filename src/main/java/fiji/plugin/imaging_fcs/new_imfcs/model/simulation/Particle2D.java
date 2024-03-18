package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

/**
 * Represents a 2-dimensional particle with properties relevant to fluorescence correlation spectroscopy (FCS) simulation.
 */
public class Particle2D {
    public double x;
    public double y;

    private boolean bleached;
    private boolean on;
    private int domainIndex;
    private double diffusionCoefficient;

    /**
     * Constructs a Particle2D with specified initial position.
     *
     * @param x Initial x-coordinate of the particle.
     * @param y Initial y-coordinate of the particle.
     */
    public Particle2D(double x, double y) {
        this.x = x;
        this.y = y;

        bleached = false;
        on = true;
        domainIndex = -1;
        diffusionCoefficient = 0.0;
    }

    /**
     * Checks if the particle is out of the specified bounds.
     *
     * @param lowerLimit Lower limit of the simulation area.
     * @param upperLimit Upper limit of the simulation area.
     * @return True if the particle is outside the bounds, false otherwise.
     */
    public boolean isOutOfBound(double lowerLimit, double upperLimit) {
        return x > upperLimit || x < lowerLimit ||
                y > upperLimit || y < lowerLimit;
    }

    /**
     * Checks if the particle is bleached.
     *
     * @return True if the particle is bleached, false otherwise.
     */
    public boolean isBleached() {
        return bleached;
    }

    /**
     * Resets the particle's bleached state to false.
     */
    public void resetBleached() {
        this.bleached = false;
    }

    /**
     * Sets the particle's bleached state to true.
     */
    public void setBleached() {
        this.bleached = true;
    }

    /**
     * Checks if the particle is in the "on" state.
     *
     * @return True if the particle is "on", false otherwise.
     */
    public boolean isOn() {
        return on;
    }

    /**
     * Sets the particle's state to "on".
     */
    public void setOn() {
        this.on = true;
    }

    /**
     * Sets the particle's state to "off".
     */
    public void setOff() {
        this.on = false;
    }

    /**
     * Gets the index of the domain the particle is in.
     *
     * @return The index of the domain, or -1 if not in any domain.
     */
    public int getDomainIndex() {
        return domainIndex;
    }

    /**
     * Sets the index of the domain the particle is in.
     *
     * @param domainIndex The index of the domain.
     */
    public void setDomainIndex(int domainIndex) {
        this.domainIndex = domainIndex;
    }

    /**
     * Gets the particle's diffusion coefficient.
     *
     * @return The diffusion coefficient.
     */
    public double getDiffusionCoefficient() {
        return diffusionCoefficient;
    }

    /**
     * Sets the particle's diffusion coefficient.
     *
     * @param diffusionCoefficient The diffusion coefficient.
     */
    public void setDiffusionCoefficient(double diffusionCoefficient) {
        this.diffusionCoefficient = diffusionCoefficient;
    }
}
