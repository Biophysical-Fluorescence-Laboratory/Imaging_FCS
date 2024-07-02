package fiji.plugin.imaging_fcs.new_imfcs.model.fit.intensity_trace.parametric_univariate_functions;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;

/**
 * Represents a polynomial function of a given order. This class implements methods to calculate
 * the value and gradient of the polynomial at a given point x, for curve fitting and analysis purposes.
 */
public class Polynomial implements ParametricUnivariateFunction {
    private final int polynomialOrder;

    /**
     * Constructs a polynomial function of the specified order.
     *
     * @param polynomialOrder the order of the polynomial (degree)
     */
    public Polynomial(int polynomialOrder) {
        super();
        this.polynomialOrder = polynomialOrder;
    }

    /**
     * Calculates the gradient of the polynomial at a given point x.
     *
     * @param x      the point at which to calculate the gradient
     * @param params coefficients of the polynomial
     * @return an array of gradients at point x for each polynomial coefficient
     */
    @Override
    public double[] gradient(double x, double[] params) {
        double[] gradient = new double[polynomialOrder + 1];
        for (int i = 0; i <= polynomialOrder; i++) {
            gradient[i] = Math.pow(x, i);
        }

        return gradient;
    }

    /**
     * Evaluates the polynomial at a given point x using the specified coefficients.
     *
     * @param x      the point at which to evaluate the polynomial
     * @param params coefficients of the polynomial
     * @return the value of the polynomial at point x
     */
    @Override
    public double value(double x, double[] params) {
        if (params.length < polynomialOrder + 1) {
            throw new IllegalArgumentException("Parameter array length does not match polynomial order.");
        }

        double value = 0;
        for (int i = 0; i <= polynomialOrder; i++) {
            value += params[i] * Math.pow(x, i);
        }

        return value;
    }
}
