package fiji.plugin.imaging_fcs.new_imfcs.model.fit;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.FitModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.PixelModel;
import org.apache.commons.math3.linear.*;

import java.util.Arrays;

import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.PI;

/**
 * This class performs Bayesian fitting for fluorescence correlation spectroscopy (FCS) data.
 * It consists of two fit, one with one particle and one with two particles with the same density.
 */
public class BayesFit {
    private static final int BOX_SIZE = 200;
    private final FitModel fitModel;
    private final ExpSettingsModel settings;

    /**
     * Constructor for BayesFit.
     *
     * @param fitModel the model to be used for fitting.
     * @param settings the experimental settings.
     */
    public BayesFit(FitModel fitModel, ExpSettingsModel settings) {
        this.fitModel = fitModel;
        this.settings = settings;
    }

    /**
     * Sets the value and hold status of a parameter in the FitModel.
     *
     * @param parameter the parameter to set.
     * @param value     the value to set.
     * @param hold      the hold status.
     */
    private void setParameterField(FitModel.Parameter parameter, double value, boolean hold) {
        parameter.setValue(value);
        parameter.setHold(hold);
    }

    /**
     * Fits the model to the pixel data using either GLS or standard fitting methods.
     *
     * @param fitModel         the model to be fitted.
     * @param pixelModel       the pixel data.
     * @param lagTimes         the lag times.
     * @param covarianceMatrix the covariance matrix.
     * @return the computed model probability.
     */
    private double fit(FitModel fitModel, PixelModel pixelModel, double[] lagTimes, double[][] covarianceMatrix) {
        double logResiduals;
        StandardFit.FitOutput output;

        if (fitModel.isGLS()) {
            GLSFit glsFit = new GLSFit(fitModel, settings, lagTimes, pixelModel.getAcf(), covarianceMatrix);
            output = glsFit.fitPixel(pixelModel, lagTimes);

            RealMatrix matT = MatrixUtils.createRealMatrix(covarianceMatrix).transpose();
            DecompositionSolver solver = new LUDecomposition(matT).getSolver();
            RealVector residualsVector = new ArrayRealVector(output.getResiduals());
            RealVector solution = solver.solve(residualsVector);

            logResiduals = -0.5 * solution.dotProduct(residualsVector);
        } else {
            StandardFit standardFit = new StandardFit(fitModel, settings);
            output = standardFit.fitPixel(pixelModel, lagTimes);
            double[] residuals = output.getResiduals();

            double residualSum = 0.0;
            for (int i = 1; i < residuals.length; i++) {
                residualSum += Math.pow(residuals[i], 2);
            }

            logResiduals = -0.5 * residualSum;
        }

        return computeModelProbability(logResiduals, output);
    }

    /**
     * Computes the model probability based on the log residuals and the fit output.
     *
     * @param logResiduals the log residuals from the fit.
     * @param output       the output of the fitting process.
     * @return the model probability.
     */
    private double computeModelProbability(double logResiduals, StandardFit.FitOutput output) {
        double[][] covariance = output.getCovariance();
        int len = covariance.length;
        double det = determinant(covariance, len);

        double prodSigma = Arrays.stream(output.getSigma()).reduce(1.0, (a, b) -> a * b);

        return Math.exp(0.5 * len * Math.log(2 * PI) + 0.5 * Math.log(det) + logResiduals -
                Math.log(prodSigma * Math.pow(2 * BOX_SIZE, len)));
    }

    /**
     * Computes the determinant of a matrix recursively.
     *
     * @param matrix the matrix.
     * @param size   the size of the matrix.
     * @return the determinant.
     */
    private double determinant(double[][] matrix, int size) {
        // Base case for 1x1 matrix
        if (size == 1) {
            return matrix[0][0];
        }
        // Base case for 2x2 matrix
        if (size == 2) {
            return matrix[0][0] * matrix[1][1] - matrix[1][0] * matrix[0][1];
        }

        double result = 0;
        for (int j = 0; j < size; j++) {
            double[][] subMatrix = generateSubMatrix(matrix, size, j);
            result += Math.pow(-1, j) * matrix[0][j] * determinant(subMatrix, size - 1);
        }

        return result;
    }

    /**
     * Generates a submatrix excluding the specified column.
     *
     * @param matrix        the original matrix.
     * @param size          the size of the original matrix.
     * @param excludeColumn the column to exclude.
     * @return the submatrix.
     */
    private double[][] generateSubMatrix(double[][] matrix, int size, int excludeColumn) {
        double[][] subMatrix = new double[size - 1][size - 1];
        for (int i = 0; i < size - 1; i++) {
            int subMatrixCol = 0;
            for (int j = 0; j < size; j++) {
                if (j == excludeColumn) {
                    continue;
                }
                subMatrix[i][subMatrixCol++] = matrix[i + 1][j];
            }
        }
        return subMatrix;
    }

    /**
     * Performs Bayesian fitting on a pixel model.
     *
     * @param pixelModel       the pixel model.
     * @param lagTimes         the lag times.
     * @param covarianceMatrix the covariance matrix.
     * @return an array containing the model probabilities for one-component and two-component fits.
     */
    public double[] bayesFit(PixelModel pixelModel, double[] lagTimes, double[][] covarianceMatrix) {
        // One-component fit
        FitModel currentFitModel = new FitModel(fitModel);
        setParameterField(currentFitModel.getD2(), 0.0, true);
        setParameterField(currentFitModel.getF2(), 0.0, true);

        double modProb1 = fit(currentFitModel, pixelModel, lagTimes, covarianceMatrix);

        // Two-component fit
        setParameterField(fitModel.getD2(), fitModel.getD().getValue() / 10, false);
        setParameterField(fitModel.getF2(), 0.5, false);

        double modProb2 = fit(fitModel, pixelModel, lagTimes, covarianceMatrix);

        // calculate the normalization for the model probabilities
        double normProb = modProb1 + modProb2;

        return new double[]{modProb1 / normProb, modProb2 / normProb};
    }
}
