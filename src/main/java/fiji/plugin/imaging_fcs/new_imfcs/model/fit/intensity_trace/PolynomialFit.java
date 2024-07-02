package fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace;

import fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace.parametric_univariate_functions.Polynomial;
import fiji.plugin.imaging_fcs.new_imfcs.utils.Pair;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;

import java.util.Collection;

/**
 * Implements curve fitting for intensity data using a polynomial model of a specified order.
 */
public class PolynomialFit extends IntensityTraceFit {
    private final int polynomialOrder;

    /**
     * Initializes a new instance for polynomial curve fitting with specified time data and polynomial order.
     *
     * @param intensityTime   Time data array.
     * @param polynomialOrder The order of the polynomial used for fitting.
     */
    public PolynomialFit(double[] intensityTime, int polynomialOrder) {
        super(intensityTime);
        this.polynomialOrder = polynomialOrder;
    }

    /**
     * Sets up a least squares problem for fitting a polynomial model to the observed data points.
     *
     * @param points Collection of weighted observed points for fitting.
     * @return Configured least squares problem suitable for optimization.
     */
    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {
        Pair<double[], double[]> targetAndWeights = createTargetAndWeights(points);
        double[] target = targetAndWeights.getLeft();
        double[] weights = targetAndWeights.getRight();

        final double[] initialGuess = new double[polynomialOrder + 1];

        initialGuess[0] = target[target.length - 1]; // use the last point as offset estimate
        for (int j = 1; j <= polynomialOrder; j++) { // use a straight line as the first estimate
            initialGuess[j] = 0;
        }

        ParametricUnivariateFunction function = new Polynomial(polynomialOrder);

        return getLeastSquaresProblem(points, function, initialGuess, target, weights);
    }
}
