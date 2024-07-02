package fiji.plugin.imaging_fcs.new_imfcs.model.correlations;

import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.solvers.BrentSolver;
import org.apache.commons.math3.analysis.solvers.UnivariateSolver;

import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.PI;
import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.SQRT_PI;
import static fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions.FCSFit.getFitObservationVolume;

/**
 * Class for calculating mean square displacement (MSD) from correlation functions.
 */
public class MeanSquareDisplacement {
    private static final double FACTOR_A = -17.0 * 1260.0 / 29.0 / 180.0;
    private static final double FACTOR_B = 1260.0 / 29.0 / 3.0;
    private static final double FACTOR_C = -1260.0 / 29.0;

    /**
     * Private constructor to prevent instantiation of this utility class.
     * This class is intended to provide only static methods and should not be instantiated.
     */
    private MeanSquareDisplacement() {
    }

    /**
     * Calculates the MSD from the given correlation function.
     *
     * @param correlationFunction The correlation function to be inverted.
     * @param pixelSizeX          The pixel size in the X dimension (in micrometers).
     * @param pixelSizeY          The pixel size in the Y dimension (in micrometers).
     * @param psfWidth            The point spread function (PSF) width (in micrometers).
     * @param thickness           The thickness of the PSF in the Z dimension (in micrometers).
     * @return The calculated MSD array.
     */
    public static double[] correlationToMSD(double[] correlationFunction, double pixelSizeX, double pixelSizeY,
                                            double psfWidth, double thickness, boolean is3d) {
        if (is3d) {
            return correlationToMSD3d(correlationFunction, pixelSizeX, pixelSizeY, psfWidth, thickness);
        } else {
            return correlationToMSD2d(correlationFunction, pixelSizeX, pixelSizeY, psfWidth);
        }
    }

    /**
     * Finds the cutoff index in the correlation function where the value drops below 10% of its initial value.
     *
     * @param correlationFunction The correlation function.
     * @return The cutoff index.
     */
    private static int findCutoffIndex(double[] correlationFunction) {
        // Determine cutoff index
        int cutoffIndex = correlationFunction.length - 1;
        for (int i = 2; i < correlationFunction.length; i++) {
            if (correlationFunction[i] / correlationFunction[1] < 0.1) {
                cutoffIndex = i;
                break;
            }
        }

        return cutoffIndex;
    }

    /**
     * Replaces NaN values in the array with zeros.
     *
     * @param array The array to process.
     */
    private static void handleNaNValues(double[] array) {
        for (int i = 0; i < array.length; i++) {
            if (Double.isNaN(array[i])) {
                array[i] = 0;
            }
        }
    }

    /**
     * Calculates the 2D MSD from the given correlation function.
     *
     * @param correlationFunction The correlation function to be inverted.
     * @param pixelSizeX          The pixel size in the X dimension (in micrometers).
     * @param pixelSizeY          The pixel size in the Y dimension (in micrometers).
     * @param psfWidth            The point spread function (PSF) width (in micrometers).
     * @return The calculated MSD array.
     */
    private static double[] correlationToMSD2d(double[] correlationFunction, double pixelSizeX, double pixelSizeY,
                                               double psfWidth) {
        int cutoffIndex = findCutoffIndex(correlationFunction);

        double[] msdArray = new double[cutoffIndex];
        // Calculate MSD for each valid channel
        for (int i = 1; i < cutoffIndex; i++) {
            double d = Math.PI * correlationFunction[i] / correlationFunction[1] /
                    (getFitObservationVolume(pixelSizeX, pixelSizeY, psfWidth) * 1e12) * Math.pow(pixelSizeX, 2) *
                    1260.0 / 29.0;
            double[] roots = solveQuartic(FACTOR_A, FACTOR_B, FACTOR_C, d);
            msdArray[i] = (Math.pow(pixelSizeX, 2) / roots[1]) - Math.pow(psfWidth, 2);
        }

        handleNaNValues(msdArray);

        return msdArray;
    }

    /**
     * Calculates the 3D MSD from the given correlation function.
     *
     * @param correlationFunction The correlation function to be inverted.
     * @param pixelSizeX          The pixel size in the X dimension (in micrometers).
     * @param pixelSizeY          The pixel size in the Y dimension (in micrometers).
     * @param psfWidth            The point spread function (PSF) width (in micrometers).
     * @param thickness           The thickness of the PSF in the Z dimension (in micrometers).
     * @return The calculated MSD array.
     */
    private static double[] correlationToMSD3d(double[] correlationFunction, double pixelSizeX, double pixelSizeY,
                                               double psfWidth, double thickness) {
        int cutoffIndex = findCutoffIndex(correlationFunction);

        double[] msdArray = new double[cutoffIndex];
        msdArray[0] = 0.0;
        msdArray[1] = 0.0;

        final double initialCorrelation = correlationFunction[1];

        for (int i = 2; i < cutoffIndex; i++) {
            final double currentCorrelation = correlationFunction[i];

            UnivariateFunction function = (double x) -> {
                double p = 1 - Math.pow(psfWidth, 2.0) / Math.pow(thickness, 2.0);
                double q = Math.pow(pixelSizeX, 2.0) / Math.pow(thickness, 2.0);
                double p1 = -15.0 * Math.pow(p, 2.0) + 20.0 * p * q + 2.0 * Math.pow(q, 2.0);
                double p2 = 945.0 * Math.pow(p, 3.0) - 630.0 * Math.pow(p, 2.0) * q;

                double coef0 = -1.0;
                double coef2 = 1 / 63.0 / q * (p2 - 420.0 * p * Math.pow(q, 2.0) - 64.0 * Math.pow(q, 3.0)) / p1;
                double coef3 = -1 / PI / Math.sqrt(q);
                double coef4 = 1 / 7560.0 / Math.pow(q, 2.0) / p1 *
                        (14175.0 * Math.pow(p, 4.0) + 37800.0 * Math.pow(p, 3.0) * q -
                                30240.0 * Math.pow(p, 2.0) * Math.pow(q, 2.0) - 3840.0 * p * Math.pow(q, 3.0) -
                                1132.0 * Math.pow(q, 4.0));
                double coef5 = 1 / 126.0 / PI / Math.pow(q, 1.5) *
                        (p2 + 126.0 * p * Math.pow(q, 2.0) - 44.0 * Math.pow(q, 3.0)) / p1;

                double corrP = currentCorrelation * SQRT_PI * pixelSizeX * pixelSizeX * thickness /
                        (getFitObservationVolume(pixelSizeX, pixelSizeY, psfWidth) * SQRT_PI * thickness *
                                Math.pow(10, 12)) / initialCorrelation;

                return coef5 * Math.pow(x, 5.0) - coef4 * corrP * Math.pow(x, 4.0) + coef3 * Math.pow(x, 3.0) -
                        coef2 * corrP * Math.pow(x, 2.0) - coef0 * corrP;
            };

            UnivariateSolver solver = new BrentSolver();
            double result = solver.solve(100, function, 0, pixelSizeX / psfWidth);
            msdArray[i] = 1.5 * (Math.pow(pixelSizeX, 2.0) / Math.pow(result, 2.0) - Math.pow(psfWidth, 2.0));
        }

        handleNaNValues(msdArray);

        return msdArray;
    }

    /**
     * Solves a fourth-order polynomial equation.
     *
     * @param a Coefficient of the x^4 term.
     * @param b Coefficient of the x^3 term.
     * @param c Coefficient of the x^2 term.
     * @param d Constant term.
     * @return The roots of the polynomial.
     */
    private static double[] solveQuartic(double a, double b, double c, double d) {
        double AA = -b;
        double BB = a * c - 4.0 * d;
        double CC = -a * a * d + 4.0 * b * d - c * c;
        double y1 = cubicRootFunction(AA, BB, CC);
        double halfA = a / 4.0;
        double RR = Math.sqrt(a * a / 4.0 - b + y1);
        double DD, EE, x1, x2, x3, x4;

        if (RR == 0) {
            double sqrtTerm = Math.sqrt(y1 * y1 - 4.0 * d);
            DD = Math.sqrt(3.0 * a * a / 4.0 - 2.0 * b + 2.0 * sqrtTerm);
            EE = Math.sqrt(3.0 * a * a / 4.0 - 2.0 * b - 2.0 * sqrtTerm);
        } else {
            double commonTerm = (4.0 * a * b - 8.0 * c - a * a * a) / (4.0 * RR);
            DD = Math.sqrt(3.0 * a * a / 4.0 - RR * RR - 2.0 * b + commonTerm);
            EE = Math.sqrt(3.0 * a * a / 4.0 - RR * RR - 2.0 * b - commonTerm);
        }

        RR /= 2.0;
        DD /= 2.0;
        EE /= 2.0;
        x1 = -halfA + RR + DD;
        x2 = -halfA + RR - DD;
        x3 = -halfA - RR + EE;
        x4 = -halfA - RR - EE;

        return new double[]{x1, x2, x3, x4};
    }

    /**
     * Solves a cubic equation to find one real root.
     *
     * @param a Coefficient of the x^2 term.
     * @param b Coefficient of the x term.
     * @param c Constant term.
     * @return The real root of the cubic equation.
     */
    private static double cubicRootFunction(double a, double b, double c) {
        double pp = b - (a * a) / 3.0;
        double qq = c + 2.0 * Math.pow(a / 3.0, 3) - a * b / 3.0;
        double Qa = Math.pow(pp / 3.0, 3) + Math.pow(qq / 2.0, 2);
        a /= 3.0;

        if (Qa >= 0) {
            double sQ = Math.sqrt(Qa);
            double AA = sQ > qq / 2.0 ? Math.pow(sQ - qq / 2.0, 1.0 / 3.0) : -Math.pow(qq / 2.0 - sQ, 1.0 / 3.0);
            double BB =
                    -sQ - qq / 2.0 > 0.0 ? Math.pow(-sQ - qq / 2.0, 1.0 / 3.0) : -Math.pow(sQ + qq / 2.0, 1.0 / 3.0);
            return AA + BB - a;
        } else {
            pp /= 3.0;
            double cosA = -qq / (2.0 * Math.sqrt(-Math.pow(pp, 3)));
            double alpha = Math.acos(cosA) / 3.0;
            if (alpha == 0) {
                alpha = 2.0 * PI / 3.0;
            }
            return 2.0 * Math.sqrt(-pp) * Math.cos(alpha) - a;
        }
    }
}