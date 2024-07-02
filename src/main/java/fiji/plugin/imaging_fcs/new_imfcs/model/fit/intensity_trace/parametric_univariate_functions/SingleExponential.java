package fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace.parametric_univariate_functions;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;

/**
 * Implements a parametric univariate function for modeling single exponential decay.
 * This class is typically used for curve fitting in systems where decay follows a single exponential function.
 */
public class SingleExponential implements ParametricUnivariateFunction {

    /**
     * Calculates the gradient of the function at a given point x, using the specified parameters.
     * This method provides the necessary derivatives for optimization algorithms used in curve fitting.
     *
     * @param x      The point at which the gradient is computed.
     * @param params Array of parameters: [amplitude, timeConstant, offset]
     *               where 'amplitude' is the scale factor of the exponential decay,
     *               'timeConstant' is the decay constant of the exponential function,
     *               and 'offset' is the constant offset of the decay curve.
     * @return An array representing the gradient of the function at point x with respect to each parameter.
     */
    @Override
    public double[] gradient(double x, double[] params) {
        double amplitude = params[0];
        double timeConstant = params[1];

        double exp = Math.exp(-x / timeConstant);

        return new double[]{
                exp, // Derivative with respect to amplitude
                amplitude * x * exp / (timeConstant * timeConstant), // Derivative with respect to time constant
                1 // Derivative with respect to offset
        };
    }

    /**
     * Calculates the value of the single exponential function at a given point x.
     * This value is computed based on the exponential model with an offset.
     *
     * @param x      The point at which the function value is calculated.
     * @param params Array of parameters as described in the gradient method.
     * @return Value of the single exponential function at point x, accounting for amplitude, decay, and offset.
     */
    @Override
    public double value(double x, double[] params) {
        double amplitude = params[0];
        double timeConstant = params[1];
        double offset = params[2];
        return amplitude * Math.exp(-x / timeConstant) + offset;
    }
}
