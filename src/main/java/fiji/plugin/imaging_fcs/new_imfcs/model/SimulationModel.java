package fiji.plugin.imaging_fcs.new_imfcs.model;

import ij.IJ;

public class SimulationModel {
    private boolean is2D;
    private boolean isDomain;
    private boolean isMesh;
    private boolean simBlinkFlag;

    private int seed = 1;

    public SimulationModel() {
        is2D = true;
        isDomain = false;
        isMesh = false;
        simBlinkFlag = false;
    }

    public void setIs2D(boolean is2D) {
        this.is2D = is2D;
    }

    public void setIsDomain(boolean isDomain) {
        this.isDomain = isDomain;
    }

    public void setIsMesh(boolean isMesh) {
        this.isMesh = isMesh;
    }

    public void setSimBlinkFlag(boolean simBlinkFlag) {
        this.simBlinkFlag = simBlinkFlag;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        try {
            this.seed = Integer.parseInt(seed);
        } catch (NumberFormatException e) {
            IJ.showMessage("Error", "Seed must be an int (the value wasn't change)");
        }
        System.out.println(this.seed);
    }
}
