package fiji.plugin.imaging_fcs.new_imfcs.model.simulation;

import java.util.Random;

/**
 * Extends the java.util.Random class to include a method for generating Poisson-distributed random numbers.
 */
public final class RandomCustom extends Random {

    /**
     * Creates a new random number generator. This constructor sets the seed of the random number generator to
     * a value very likely to be distinct from any other invocation of this constructor.
     */
    public RandomCustom() {
        super();
    }

    /**
     * Creates a new random number generator using a single {@code long} seed.
     *
     * @param seed the initial seed
     */
    public RandomCustom(int seed) {
        super(seed);
    }

    /**
     * Generates a random number based on the Poisson distribution with the given mean.
     *
     * @param mean the mean of the Poisson distribution
     * @return a random integer following the Poisson distribution.
     */
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
