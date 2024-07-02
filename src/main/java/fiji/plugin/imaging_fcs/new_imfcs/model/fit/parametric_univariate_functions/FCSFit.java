package fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.FitModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.PixelModel;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.special.Erf;

import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.NANO_CONVERSION_FACTOR;
import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.SQRT_PI;

/**
 * The FCSFit class provides the base functionality for fitting fluorescence correlation spectroscopy (FCS) data.
 * It implements the ParametricUnivariateFunction interface from the Apache Commons Math library.
 */
public abstract class FCSFit implements ParametricUnivariateFunction {
    protected final FitModel fitModel;
    protected double ax, ay, s, sz, rx, ry, fitObservationVolume, q2, q3;

    /**
     * Constructs a new FCSFit instance with the given settings and fit model.
     *
     * @param settings The experimental settings model.
     * @param fitModel The fit model.
     * @param mode     The mode for the PSF size and light sheet thickness.
     */
    public FCSFit(ExpSettingsModel settings, FitModel fitModel, int mode) {
        this.fitModel = fitModel;

        initParameters(settings, mode);
    }

    /**
     * Computes the observation volume for the fit.
     *
     * @param ax The axial size.
     * @param ay The lateral size.
     * @param s  The PSF size.
     * @return The calculated fit observation volume.
     */
    public static double getFitObservationVolume(double ax, double ay, double s) {
        // Compute helper values
        double expAxSqr = 2 * Math.exp(-Math.pow(ax / s, 2)) - 2;
        double expAySqr = 2 * Math.exp(-Math.pow(ay / s, 2)) - 2;
        double erfAxS = 2 * ax * Erf.erf(ax / s);
        double erfAyS = 2 * ay * Erf.erf(ay / s);

        // calculate terms
        double termX = (s / SQRT_PI) * expAxSqr + erfAxS;
        double termY = (s / SQRT_PI) * expAySqr + erfAyS;

        // Return the calculated value for 2D fit, DC-FCCS fit
        return 4 * Math.pow(ax * ay, 2) / (termX * termY);
    }

    /**
     * Initializes the parameters for the FCS fit.
     *
     * @param settings The experimental settings model.
     * @param mode     The mode for the PSF size and light sheet thickness.
     */
    protected void initParameters(ExpSettingsModel settings, int mode) {
        ax = settings.getParamAx() / NANO_CONVERSION_FACTOR;
        ay = settings.getParamAy() / NANO_CONVERSION_FACTOR;

        double psfSize = settings.getParamW() / NANO_CONVERSION_FACTOR;
        double psfSize2 = settings.getParamW2() / NANO_CONVERSION_FACTOR;

        double lsThickness = settings.getParamZ() / NANO_CONVERSION_FACTOR;
        double lsThickness2 = settings.getParamZ2() / NANO_CONVERSION_FACTOR;

        if (mode == 0) {
            s = psfSize;
            sz = lsThickness;
        } else if (mode == 1) {
            s = psfSize2;
            sz = lsThickness2;
        } else {
            s = Math.sqrt(Math.pow(psfSize, 2) / 2 + Math.pow(psfSize2, 2) / 2);
            sz = Math.sqrt(Math.pow(lsThickness, 2) / 2 + Math.pow(lsThickness2, 2) / 2);
        }

        rx = settings.getParamRx() / NANO_CONVERSION_FACTOR;
        ry = settings.getParamRy() / NANO_CONVERSION_FACTOR;

        fitObservationVolume = getFitObservationVolume(ax, ay, s);

        q2 = fitModel.getQ2();
        q3 = fitModel.getQ3();
    }

    /**
     * Calculates the component values for the FCS fit.
     *
     * @param x  The lag time.
     * @param D  The diffusion coefficient.
     * @param vx The flow velocity in the x direction.
     * @param vy The flow velocity in the y direction.
     * @return An array of calculated component values.
     */
    private double[] calculateComponent(double x, double D, double vx, double vy) {
        double sqrtTerm = Math.sqrt(4 * D * x + Math.pow(s, 2));
        PerfTerms perfTermX = calculatePerfTerms(x, vx, ax, rx, sqrtTerm);
        PerfTerms perfTermY = calculatePerfTerms(x, vy, ay, ry, sqrtTerm);

        double plat = calculatePlat(perfTermX, perfTermY, sqrtTerm);
        double dDplat = calculateDPlat(perfTermX, perfTermY, sqrtTerm, x);

        double pspim = 1 / Math.sqrt(1 + (4 * D * x) / Math.pow(sz, 2));
        double dDpspim = -4 * x / (2 * Math.pow(sz, 2) * Math.pow(Math.sqrt(1 + (4 * D * x) / Math.pow(sz, 2)), 3));

        double acf = plat * pspim;

        return new double[]{plat, dDplat, perfTermX.dPerf, perfTermY.dPerf, pspim, dDpspim, acf};
    }

    /**
     * Calculates the plat value for the FCS fit.
     *
     * @param perfTermX The perf terms for the x direction.
     * @param perfTermY The perf terms for the y direction.
     * @param sqrtTerm  The square root term.
     * @return The calculated plat value.
     */
    private double calculatePlat(PerfTerms perfTermX, PerfTerms perfTermY, double sqrtTerm) {
        return (sqrtTerm / SQRT_PI * perfTermX.exp + perfTermX.perf) *
                (sqrtTerm / SQRT_PI * perfTermY.exp + perfTermY.perf) /
                (4 * Math.pow(ax * ay, 2) / fitObservationVolume);
    }

    /**
     * Calculates the dPlat value for the FCS fit.
     *
     * @param perfTermX The perf terms for the x direction.
     * @param perfTermY The perf terms for the y direction.
     * @param sqrtTerm  The square root term.
     * @param x         The lag time.
     * @return The calculated dPlat value.
     */
    private double calculateDPlat(PerfTerms perfTermX, PerfTerms perfTermY, double sqrtTerm, double x) {
        return (1 / (SQRT_PI * sqrtTerm)) *
                (perfTermY.dExp * x * (sqrtTerm / SQRT_PI * perfTermX.exp + perfTermX.perf) +
                        perfTermX.dExp * x * (sqrtTerm / SQRT_PI * perfTermY.exp + perfTermY.perf)) /
                (4 * Math.pow(ax * ay, 2) / fitObservationVolume);
    }

    /**
     * Calculates the perf terms for the FCS fit.
     *
     * @param x        The lag time.
     * @param v        The flow velocity.
     * @param a        The axial size.
     * @param r        The radial size.
     * @param sqrtTerm The square root term.
     * @return The calculated perf terms.
     */
    private PerfTerms calculatePerfTerms(double x, double v, double a, double r, double sqrtTerm) {
        double term1 = a + r - v * x;
        double term2 = a - r + v * x;
        double term3 = r - v * x;
        double term4 = 2 * Math.pow(a, 2) + 3 * Math.pow(r, 2) - 6 * x * r * v + 3 * Math.pow(x * v, 2);
        double term5 = Math.pow(term3, 2) + Math.pow(term1, 2);
        double term6 = Math.pow(term3, 2) + Math.pow(term2, 2);
        double term7 = 2 * (Math.pow(a, 2) + Math.pow(r, 2) - 2 * x * r * v + Math.pow(x * v, 2));

        double expTerm = Math.exp(-Math.pow(term1 / sqrtTerm, 2)) + Math.exp(-Math.pow(term2 / sqrtTerm, 2)) -
                2 * Math.exp(-Math.pow(term3 / sqrtTerm, 2));
        double erfTerm = term1 * Erf.erf(term1 / sqrtTerm) + term2 * Erf.erf(term2 / sqrtTerm) -
                2 * term3 * Erf.erf(term3 / sqrtTerm);
        double dExpTerm = 2 * Math.exp(-term4 / Math.pow(sqrtTerm, 2)) *
                (Math.exp(term5 / Math.pow(sqrtTerm, 2)) + Math.exp(term6 / Math.pow(sqrtTerm, 2)) -
                        2 * Math.exp(term7 / Math.pow(sqrtTerm, 2)));
        double dPerf = (Erf.erf(term2 / sqrtTerm) + 2 * Erf.erf(term3 / sqrtTerm) - Erf.erf(term1 / sqrtTerm)) * x;

        return new PerfTerms(expTerm, erfTerm, dExpTerm, dPerf);
    }

    @Override
    public double[] gradient(double x, double[] params) {
        PixelModel.FitParameters p = new PixelModel.FitParameters(fitModel.fillParamsArray(params));

        double[] component1 = calculateComponent(x, p.getD(), p.getVx(), p.getVy());
        double[] component2 = calculateComponent(x, p.getD2(), p.getVx(), p.getVy());
        double[] component3 = calculateComponent(x, p.getD3(), p.getVx(), p.getVy());

        double plat1 = component1[0], dDplat1 = component1[1], dvxPerfXt1 = component1[2], dvyPerfYt1 = component1[3];
        double pspim1 = component1[4], dDpspim1 = component1[5], acf1 = component1[6];

        double plat2 = component2[0], dDplat2 = component2[1], dvxPerfXt2 = component2[2], dvyPerfYt2 = component2[3];
        double pspim2 = component2[4], dDpspim2 = component2[5], acf2 = component2[6];

        double plat3 = component3[0], dDplat3 = component3[1], dvxPerfXt3 = component3[2], dvyPerfYt3 = component3[3];
        double pspim3 = component3[4], dDpspim3 = component3[5], acf3 = component3[6];

        // Triplet Correction
        double triplet = calculateTriplet(x, p.getFTrip(), p.getTTrip());
        double dTripletFtrip = calculateDTripletFtrip(x, p.getFTrip(), p.getTTrip());
        double dTripletTtrip = calculateDTripletTtrip(x, p.getFTrip(), p.getTTrip());

        // Correction Factors
        double[] pf = calculateCorrectionFactors(p.getF2(), p.getF3(), q2, q3);
        double pf1 = pf[0], pf2 = pf[1], pf3 = pf[2];
        double[] df = calculateDNomFactors(p.getF2(), p.getF3(), q2, q3);
        double dfNom = df[0], df21 = df[1], df22 = df[2], df23 = df[3], df31 = df[4], df32 = df[5], df33 = df[6];

        double pacf = (1 / p.getN()) * ((1 - p.getF2() - p.getF3()) * acf1 + Math.pow(q2, 2) * p.getF2() * acf2 +
                Math.pow(q3, 2) * p.getF3() * acf3) /
                Math.pow(1 - p.getF2() - p.getF3() + q2 * p.getF2() + q3 * p.getF3(), 2) * triplet + p.getG();

        double[] results = new double[]{
                (-1 / Math.pow(p.getN(), 2)) * (pf1 * acf1 + pf2 * acf2 + pf3 * acf3) * triplet,
                (1 / p.getN()) * pf1 * (plat1 * dDpspim1 + pspim1 * dDplat1),
                (1 / p.getN()) * (pf1 * dvxPerfXt1 * plat1 * pspim1 + pf2 * dvxPerfXt2 * plat2 * pspim2 +
                        pf3 * dvxPerfXt3 * plat3 * pspim3) * triplet,
                (1 / p.getN()) * (pf1 * dvyPerfYt1 * plat1 * pspim1 + pf2 * dvyPerfYt2 * plat2 * pspim2 +
                        pf3 * dvyPerfYt3 * plat3 * pspim3) * triplet,
                1,
                (1 / p.getN()) * (1 / dfNom) * (df21 * acf1 + df22 * acf2 + df23 * acf3) * triplet,
                (1 / p.getN()) * pf2 * (plat2 * dDpspim2 + pspim2 * dDplat2) * triplet,
                (1 / p.getN()) * (1 / dfNom) * (df31 * acf1 + df32 * acf2 + df33 * acf3) * triplet,
                (1 / p.getN()) * pf3 * (plat3 * dDpspim3 + pspim3 * dDplat3) * triplet,
                dTripletFtrip * pacf,
                dTripletTtrip * pacf
        };

        // TODO: check if the filtering actually makes sense
        return fitModel.filterFitArray(results);
    }

    /**
     * Calculates the triplet correction for the FCS fit.
     *
     * @param x     The lag time.
     * @param fTrip The triplet fraction.
     * @param tTrip The triplet time.
     * @return The calculated triplet correction.
     */
    private double calculateTriplet(double x, double fTrip, double tTrip) {
        return 1 + fTrip / (1 - fTrip) * Math.exp(-x / tTrip);
    }

    /**
     * Calculates the derivative of the triplet correction with respect to the triplet fraction.
     *
     * @param x     The lag time.
     * @param fTrip The triplet fraction.
     * @param tTrip The triplet time.
     * @return The calculated derivative.
     */
    private double calculateDTripletFtrip(double x, double fTrip, double tTrip) {
        return Math.exp(-x / tTrip) * (1 / (1 - fTrip) + fTrip / Math.pow(1 - fTrip, 2));
    }

    /**
     * Calculates the derivative of the triplet correction with respect to the triplet time.
     *
     * @param x     The lag time.
     * @param fTrip The triplet fraction.
     * @param tTrip The triplet time.
     * @return The calculated derivative.
     */
    private double calculateDTripletTtrip(double x, double fTrip, double tTrip) {
        return Math.exp(-x / tTrip) * (fTrip * x) / ((1 - fTrip) * Math.pow(tTrip, 2));
    }

    /**
     * Calculates the correction factors for the FCS fit.
     *
     * @param F2 The fraction of the second component.
     * @param F3 The fraction of the third component.
     * @param q2 The relative amplitude of the second component.
     * @param q3 The relative amplitude of the third component.
     * @return An array of calculated correction factors.
     */
    private double[] calculateCorrectionFactors(double F2, double F3, double q2, double q3) {
        double denominator = 1 - F2 - F3 + q2 * F2 + q3 * F3;
        double pf1 = (1 - F2 - F3) / denominator;
        double pf2 = (q2 * q2 * F2) / denominator;
        double pf3 = (q3 * q3 * F3) / denominator;
        return new double[]{pf1, pf2, pf3};
    }

    /**
     * Calculates the derivatives of the normalization factors for the FCS fit.
     *
     * @param F2 The fraction of the second component.
     * @param F3 The fraction of the third component.
     * @param q2 The relative amplitude of the second component.
     * @param q3 The relative amplitude of the third component.
     * @return An array of calculated derivatives.
     */
    private double[] calculateDNomFactors(double F2, double F3, double q2, double q3) {
        double dfNom = Math.pow(1 - F2 - F3 + q2 * F2 + q3 * F3, 3);
        double df21 = 1 - F2 - F3 + q2 * F2 - q3 * F3 + 2 * q2 * F3 - 2 * q2;
        double df22 = Math.pow(q2, 2) * (1 + F2 - F3 - q2 * F2 + q3 * F3);
        double df23 = 2 * F3 * Math.pow(q3, 2) * (1 - q2);
        double df31 = 1 - F2 - F3 - q2 * F2 + 2 * q3 * F2 - 2 * q3 + q3 * F3;
        double df32 = 2 * F2 * Math.pow(q2, 2) * (1 - q3);
        double df33 = Math.pow(q3, 2) * (1 - F2 + F3 + q2 * F2 - q3 * F3);
        return new double[]{dfNom, df21, df22, df23, df31, df32, df33};
    }

    @Override
    public double value(double x, double[] params) {
        PixelModel.FitParameters p = new PixelModel.FitParameters(fitModel.fillParamsArray(params));

        double acf1 = calculateComponent(x, p.getD(), p.getVx(), p.getVy())[6];
        double acf2 = calculateComponent(x, p.getD2(), p.getVx(), p.getVy())[6];
        double acf3 = calculateComponent(x, p.getD3(), p.getVx(), p.getVy())[6];

        double triplet = calculateTriplet(x, p.getFTrip(), p.getTTrip());

        double weightFactor = 1 - p.getF2() - p.getF3() + q2 * p.getF2() + q3 * p.getF3();
        double weightedAcf = (1 - p.getF2() - p.getF3()) * acf1 + Math.pow(q2, 2) * p.getF2() * acf2 +
                Math.pow(q3, 2) * p.getF3() * acf3;

        return (1 / p.getN()) * (weightedAcf / Math.pow(weightFactor, 2)) * triplet + p.getG();
    }

    /**
     * The PerfTerms class encapsulates the performance terms for the FCS fit.
     */
    private static class PerfTerms {
        private final double exp;
        private final double perf;
        private final double dExp;
        private final double dPerf;

        PerfTerms(double exp, double perf, double dExp, double dPerf) {
            this.exp = exp;
            this.perf = perf;
            this.dExp = dExp;
            this.dPerf = dPerf;
        }
    }
}
