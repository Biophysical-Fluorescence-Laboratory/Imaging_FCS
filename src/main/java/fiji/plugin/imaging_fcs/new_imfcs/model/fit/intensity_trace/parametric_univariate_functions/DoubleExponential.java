package fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace.parametric_univariate_functions;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;

/**
 * Implements a parametric univariate function for modeling double exponential decay. This class can be used
 * to perform curve fitting for systems exhibiting bi-exponential decay behavior.
 */
public class DoubleExponential implements ParametricUnivariateFunction {
    /**
     * Calculates the gradient of the function at a given point x, using specified parameters.
     * The gradient is calculated for each parameter, providing the necessary derivatives for optimization algorithms.
     *
     * @param x      The point at which the gradient is computed.
     * @param params Array of parameters: [amplitude1, timeConstant1, amplitude2, timeConstant2, offset]
     *               where 'amplitude1' and 'amplitude2' are the amplitudes of the first and second exponential components,
     *               'timeConstant1' and 'timeConstant2' are the time constants,
     *               and 'offset' is the baseline offset of the decay function.
     * @return An array representing the gradient of the function at point x with respect to each parameter.
     */
    @Override
    public double[] gradient(double x, double[] params) {
        double amplitude1 = params[0];
        double timeConstant1 = params[1];
        double amplitude2 = params[2];
        double timeConstant2 = params[3];

        double exp1 = Math.exp(-x / timeConstant1);
        double exp2 = Math.exp(-x / timeConstant2);

        return new double[]{
                exp1,
                amplitude1 * x * exp1 / (timeConstant1 * timeConstant1),
                exp2,
                amplitude2 * x * exp2 / (timeConstant2 * timeConstant2),
                1
        };
    }

    /**
     * Calculates the value of the double exponential function at a given point x.
     * This method computes the sum of two exponential terms and a constant offset, allowing for the modeling
     * of systems with two distinct decay rates.
     *
     * @param x      The point at which the function value is calculated.
     * @param params Array of parameters as described in the gradient method.
     * @return Value of the double exponential function at point x, incorporating both exponential components and offset.
     */
    @Override
    public double value(double x, double[] params) {
        double amplitude1 = params[0];
        double timeConstant1 = params[1];
        double amplitude2 = params[2];
        double timeConstant2 = params[3];
        double offset = params[4];
        return amplitude1 * Math.exp(-x / timeConstant1) + amplitude2 * Math.exp(-x / timeConstant2) + offset;
    }
}
