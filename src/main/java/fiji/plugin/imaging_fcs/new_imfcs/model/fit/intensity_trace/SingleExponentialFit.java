package fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace;

import fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace.parametric_univariate_functions.SingleExponential;
import fiji.plugin.imaging_fcs.new_imfcs.utils.Pair;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;

import java.util.Collection;

/**
 * Provides methods to fit single exponential decay models to intensity data over time.
 */
public class SingleExponentialFit extends IntensityTraceFit {
    /**
     * Constructs a SingleExponentialFit with given time data for intensity measurements.
     *
     * @param intensityTime Time data array.
     */
    public SingleExponentialFit(double[] intensityTime) {
        super(intensityTime);
    }

    /**
     * Configures the least squares problem for fitting a single exponential decay model.
     *
     * @param points Collection of weighted observed points for fitting.
     * @return Configured least squares problem for the optimizer.
     */
    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {
        Pair<double[], double[]> targetAndWeights = createTargetAndWeights(points);
        double[] target = targetAndWeights.getLeft();
        double[] weights = targetAndWeights.getRight();

        double[] initialGuess = new double[3];

        // initial guesses
        initialGuess[0] = target[1]; // use first point as intercept estimate
        initialGuess[1] = intensityTime[target.length / 2]; // use half the intensity trace time as first estimate
        // for the exponential decay
        initialGuess[2] = 0;

        ParametricUnivariateFunction function = new SingleExponential();

        return getLeastSquaresProblem(points, function, initialGuess, target, weights);
    }
}
