package fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions;

import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.linear.*;

/**
 * The GLSFitFunction class implements the ParametricUnivariateFunction interface to perform generalized least
 * squares fitting.
 * It uses a parametric univariate function and applies transformations to account for correlated noise.
 */
public class GLSFitFunction implements ParametricUnivariateFunction {
    private final ParametricUnivariateFunction function;
    private final double[] lagTimes;
    private final RealMatrix lowerDiagonalCholeskyDecomposition;
    private final double[][] theoreticalGradientACF;
    private double[] theoreticalACF;

    /**
     * Constructs a new GLSFitFunction instance with the given parameters.
     *
     * @param function                           The parametric univariate function to be used.
     * @param lagTimes                           The lag times for fitting.
     * @param lowerDiagonalCholeskyDecomposition The lower triangular matrix from the Cholesky decomposition.
     * @param numFreeParameters                  The number of free parameters in the fitting model.
     */
    public GLSFitFunction(ParametricUnivariateFunction function, double[] lagTimes,
                          RealMatrix lowerDiagonalCholeskyDecomposition, int numFreeParameters) {
        this.function = function;
        this.lagTimes = lagTimes;
        this.lowerDiagonalCholeskyDecomposition = lowerDiagonalCholeskyDecomposition;

        theoreticalGradientACF = new double[numFreeParameters][lagTimes.length - 1];
    }

    /**
     * Finds the index of the solution for the given lag time.
     *
     * @param x The lag time.
     * @return The index of the solution.
     */
    private int findSolutionIndex(double x) {
        for (int i = lagTimes.length - 1; i > 0; i--) {
            if (lagTimes[i] == x) {
                return i - 1;
            }
        }
        return 0; // Default to the first index
    }

    /**
     * Calculates the gradient with respect to tau for the given parameters.
     *
     * @param params The parameters for the function.
     * @return The gradient with respect to tau.
     */
    private double[][] calculateGradTau(double[] params) {
        int numParams = params.length;
        double[][] gradTau = new double[numParams][lagTimes.length - 1];

        for (int i = 1; i < lagTimes.length; i++) {
            double[] gradient = function.gradient(lagTimes[i], params);
            for (int j = 0; j < numParams; j++) {
                gradTau[j][i - 1] = gradient[j];
            }
        }

        return gradTau;
    }

    /**
     * Solves the linear system for the gradients using the LU decomposition.
     *
     * @param tau The array of tau values.
     * @return The solved gradients as a RealVector.
     */
    private RealVector solveGradients(double[] tau) {
        DecompositionSolver solver = new LUDecomposition(lowerDiagonalCholeskyDecomposition).getSolver();
        RealVector constants = new ArrayRealVector(tau);
        return solver.solve(constants);
    }

    @Override
    public double[] gradient(double x, double[] params) {
        int numParams = params.length;
        double[] finalGrad = new double[numParams];

        if (x == lagTimes[1]) {
            double[][] gradTau = calculateGradTau(params);

            // Assign the appropriate gradient for the given tau
            for (int i = 0; i < numParams; i++) {
                // Solve the linear system for each parameter's gradient
                theoreticalGradientACF[i] = solveGradients(gradTau[i]).toArray();
                finalGrad[i] = theoreticalGradientACF[i][0];
            }
        } else {
            // Find the index of the solution for the particular tau
            int solutionIndex = findSolutionIndex(x);

            // Directly use the precomputed gradient for the given tau
            for (int i = 0; i < numParams; i++) {
                finalGrad[i] = theoreticalGradientACF[i][solutionIndex];
            }
        }

        return finalGrad;
    }

    @Override
    public double value(double x, double[] params) {
        if (x == lagTimes[1]) {
            double[] valueTau = new double[lagTimes.length - 1];
            // calculate the correlation function for this particular set of parameters
            for (int i = 1; i < lagTimes.length; i++) {
                valueTau[i - 1] = function.value(lagTimes[i], params);
            }

            theoreticalACF = solveGradients(valueTau).toArray();
            return theoreticalACF[0];
        } else {
            int solutionIndex = findSolutionIndex(x);
            return theoreticalACF[solutionIndex];
        }
    }
}
