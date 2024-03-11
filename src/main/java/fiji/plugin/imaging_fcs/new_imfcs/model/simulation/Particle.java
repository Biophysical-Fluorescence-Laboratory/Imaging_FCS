package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

public class Particle {
    public double x;
    public double y;

    private boolean bleached;
    private boolean on;
    private int domainIndex;

    public Particle(double x, double y) {
        this.x = x;
        this.y = y;

        bleached = false;
        on = true;
        domainIndex = -1;
    }

    public boolean isBleached() {
        return bleached;
    }

    public void setBleached() {
        this.bleached = true;
    }

    public boolean isOn() {
        return on;
    }

    public void setOn() {
        this.on = true;
    }

    public void setOff() {
        this.on = false;
    }

    public int getDomainIndex() {
        return domainIndex;
    }

    public void setDomainIndex(int domainIndex) {
        this.domainIndex = domainIndex;
    }
}
