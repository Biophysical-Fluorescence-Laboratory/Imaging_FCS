package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import java.util.Random;

public final class RandomCustom extends Random {

    public RandomCustom() {
        super();
    }

    public RandomCustom(int seed) {
        super(seed);
    }

    public int nextPoisson(double mean) {
        double L = Math.exp(-mean);
        int k = 0;
        double p = 1.0;
        do {
            k++;
            double u = nextDouble(); // Generate a uniform random number u in [0,1]
            p *= u;
        } while (p > L);
        return k - 1;
    }
}
