package fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace;

import fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace.parametric_univariate_functions.DoubleExponential;
import fiji.plugin.imaging_fcs.new_imfcs.utils.Pair;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;

import java.util.Collection;

/**
 * Implements curve fitting for intensity data using a double exponential decay model.
 */
public class DoubleExponentialFit extends IntensityTraceFit {
    /**
     * Initializes a new instance with time data for intensity measurements.
     *
     * @param intensityTime Time data array.
     */
    public DoubleExponentialFit(double[] intensityTime) {
        super(intensityTime);
    }

    /**
     * Sets up a least squares problem for fitting a double exponential model to observed data points.
     *
     * @param points Collection of weighted observed points for fitting.
     * @return Configured least squares problem suitable for optimization.
     */
    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {
        Pair<double[], double[]> targetAndWeights = createTargetAndWeights(points);
        double[] target = targetAndWeights.getLeft();
        double[] weights = targetAndWeights.getRight();

        final double[] initialGuess = new double[5];

        // initial guesses
        initialGuess[0] = target[1] / 2; // amplitude for first and second decay are set equal; estimated from half
        // of the first point
        initialGuess[1] = intensityTime[target.length / 10]; // use a tenth of the intensity trace time as first
        // estimate for the first exponential decay
        initialGuess[2] = target[1] / 2; // amplitude estimated from half of the first point
        initialGuess[3] = intensityTime[target.length / 2]; // use half the intensity trace time as first estimate
        // for the second exponential decay
        initialGuess[4] = 0;

        ParametricUnivariateFunction function = new DoubleExponential();

        return getLeastSquaresProblem(points, function, initialGuess, target, weights);
    }
}
