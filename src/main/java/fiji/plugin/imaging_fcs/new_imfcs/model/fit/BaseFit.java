package fiji.plugin.imaging_fcs.new_imfcs.model.fit;

import fiji.plugin.imaging_fcs.new_imfcs.utils.Pair;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.AbstractCurveFitter;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresBuilder;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.DiagonalMatrix;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Abstract class for curve fitting.
 */
public abstract class BaseFit extends AbstractCurveFitter {
    protected int maxEvaluations = Integer.MAX_VALUE;
    protected int maxIterations = Integer.MAX_VALUE;

    /**
     * Sets the maximum number of evaluations for the least squares problem.
     *
     * @param maxEvaluations The maximum number of evaluations.
     */
    protected void setMaxEvaluations(int maxEvaluations) {
        this.maxEvaluations = maxEvaluations;
    }

    /**
     * Sets the maximum number of iterations for the least squares problem.
     *
     * @param maxIterations The maximum number of iterations.
     */
    protected void setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
    }

    /**
     * Creates and fills the target and weights arrays with the Y values and weights from the given collection of
     * {@link WeightedObservedPoint} objects.
     *
     * @param points the collection of {@link WeightedObservedPoint} objects to process
     * @return a two-dimensional array where the first element is the target array filled with Y values and the second
     * element is the weights array filled with the weights of the points
     */
    protected Pair<double[], double[]> createTargetAndWeights(Collection<WeightedObservedPoint> points) {
        int len = points.size();
        double[] target = new double[len];
        double[] weights = new double[len];

        AtomicInteger index = new AtomicInteger(0);
        points.forEach(point -> {
            int i = index.getAndIncrement();
            target[i] = point.getY();
            weights[i] = point.getWeight();
        });

        return new Pair<>(target, weights);
    }

    /**
     * Configures the least squares problem for curve fitting.
     *
     * @param points       Collection of weighted observed points for fitting.
     * @param function     The parametric univariate function to fit.
     * @param initialGuess The initial guess array for the fitting parameters.
     * @param target       The target array of observed values.
     * @param weights      The weights array.
     * @return Configured least squares problem for the optimizer.
     */
    protected LeastSquaresProblem getLeastSquaresProblem(Collection<WeightedObservedPoint> points,
                                                         ParametricUnivariateFunction function,
                                                         double[] initialGuess,
                                                         double[] target,
                                                         double[] weights) {
        final AbstractCurveFitter.TheoreticalValuesFunction model = new AbstractCurveFitter.TheoreticalValuesFunction(
                function, points);

        return new LeastSquaresBuilder()
                .maxEvaluations(maxEvaluations)
                .maxIterations(maxIterations)
                .start(initialGuess)
                .target(target)
                .weight(new DiagonalMatrix(weights))
                .model(model.getModelFunction(), model.getModelFunctionJacobian())
                .build();
    }
}
