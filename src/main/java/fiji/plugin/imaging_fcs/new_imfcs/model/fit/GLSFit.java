package fiji.plugin.imaging_fcs.new_imfcs.model.fit;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.FitModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.PixelModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions.GLSFitFunction;
import fiji.plugin.imaging_fcs.new_imfcs.utils.Pair;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;
import org.apache.commons.math3.linear.*;

import java.util.Arrays;
import java.util.Collection;

/**
 * The GLSFit class extends the StandardFit class to perform generalized least squares fitting for fluorescence
 * correlation spectroscopy (FCS) data.
 * It applies a transformation to the data using the Cholesky decomposition to handle correlated noise.
 */
public class GLSFit extends StandardFit {
    private final GLSFitFunction glsFitFunction;
    private RealMatrix lowerTriangularMatrix;
    private RealVector transformedACF;

    /**
     * Constructs a new GLSFit instance with the given model, settings, lag times, correlation function, and
     * covariance matrix.
     *
     * @param model               The FitModel instance containing the fitting parameters.
     * @param settings            The experimental settings model.
     * @param lagTimes            The lag times for fitting.
     * @param correlationFunction The correlation function data.
     * @param covarianceMatrix    The covariance matrix for the data.
     */
    public GLSFit(FitModel model, ExpSettingsModel settings, double[] lagTimes, double[] correlationFunction,
                  double[][] covarianceMatrix) {
        super(model, settings);

        dataTransform(correlationFunction, covarianceMatrix);
        glsFitFunction = new GLSFitFunction(function, lagTimes, lowerTriangularMatrix, numFreeParameters);
    }

    /**
     * Transforms the correlation function data using the Cholesky decomposition of the covariance matrix.
     *
     * @param correlationFunction The correlation function data.
     * @param covarianceMatrix    The covariance matrix for the data.
     */
    private void dataTransform(double[] correlationFunction, double[][] covarianceMatrix) {
        // Perform the Cholesky decomposition to get the lower triangular matrix
        lowerTriangularMatrix = new CholeskyDecomposition(MatrixUtils.createRealMatrix(covarianceMatrix)).getL();

        // Remove zero lagtime from the correlation function as it is not used in the fit
        double[] correlationTmp = Arrays.copyOfRange(correlationFunction, 1, correlationFunction.length);

        // Solve for a new correlation vector with independent elements using LU decomposition
        DecompositionSolver solver = new LUDecomposition(lowerTriangularMatrix).getSolver();
        transformedACF = solver.solve(new ArrayRealVector(correlationTmp));
    }

    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {
        Pair<double[], double[]> targetAndWeights = createTargetAndWeights(points);
        double[] target = targetAndWeights.getLeft();
        double[] weights = targetAndWeights.getRight();

        double[] initialGuess = model.getNonHeldParameterValues();

        return getLeastSquaresProblem(points, glsFitFunction, initialGuess, target, weights);
    }

    @Override
    protected WeightedObservedPoint createPoint(PixelModel pixelModel, double[] lagTimes, int i) {
        return new WeightedObservedPoint(1, lagTimes[i], transformedACF.getEntry(i - 1));
    }
}
