package fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.FitModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.PixelModel;
import org.apache.commons.math3.special.Erf;

import java.util.stream.IntStream;

import static fiji.plugin.imaging_fcs.new_imfcs.constants.Constants.*;

/**
 * This class represents the 3D Single Plane Illumination Microscopy (SPIM) model for Fluorescence Correlation
 * Spectroscopy (FCS).
 * It extends the FCSFit class and provides implementations for calculating the value and gradient of the model.
 */
public class FCS3pSPIM extends FCSFit {
    // general parameters
    private static final int LOOP_ITERATIONS = 6401;
    private static final double INVERSE_SQRT_PI = Math.sqrt(1 / PI);
    private double srn, NA, modifiedObservationVolume;

    /**
     * Constructor for the FCS3pSPIM class.
     *
     * @param settings The experimental settings model.
     * @param fitModel The fit model.
     */
    public FCS3pSPIM(ExpSettingsModel settings, FitModel fitModel) {
        super(settings, fitModel, 0);
    }

    @Override
    protected void initParameters(ExpSettingsModel settings, int mode) {
        super.initParameters(settings, mode);

        NA = settings.getNA();
        srn = Math.sqrt(Math.pow(REFRACTIVE_INDEX, 2) - Math.pow(NA, 2));
        modifiedObservationVolume = getModifiedObservationVolume(settings);
    }

    /**
     * Calculates the modified observation volume.
     *
     * @param settings The experimental settings model.
     * @return The modified observation volume.
     */
    private double getModifiedObservationVolume(ExpSettingsModel settings) {
        double fitObservationVolume = getFitObservationVolume(ax, ay, s);

        // Additional calculations for 3D volume
        // size of PSF in axial direction convolution of two Gaussians depending on illumination profile and
        // detection PSF
        double psfZ = 2 * settings.getEmLambda() / NANO_CONVERSION_FACTOR * REFRACTIVE_INDEX / Math.pow(NA, 2.0);
        double szEff = Math.sqrt(1 / (Math.pow(sz, -2.0) + Math.pow(psfZ, -2.0)));

        double volume3d = SQRT_PI * szEff * fitObservationVolume;
        return (volume3d / (SQRT_PI * sz));
    }

    /**
     * Calculates the z coordinate based on the index.
     *
     * @param index The index.
     * @return The z coordinate.
     */
    private double calculateZ(int index) {
        return (sz * (index - 40)) / 20;
    }

    /**
     * Calculates the Point Spread Function (PSF) in the z direction.
     *
     * @param z The z coordinate.
     * @return The PSF in the z direction.
     */
    private double calculatePSFz(double z) {
        return s + (NA * Math.abs(z)) / srn;
    }

    /**
     * Calculates a component used in the model.
     *
     * @param x                 The x coordinate.
     * @param a                 Parameter a.
     * @param r                 Parameter r.
     * @param sdt               Standard deviation of t.
     * @param sp0t              Square root of p0t.
     * @param tsp0t             Cube of sp0t.
     * @param qp0t              Square of p0t.
     * @param computeDerivative Whether to compute the derivative.
     * @return An array containing the part and the derivative.
     */
    private double[] calculateComponent(double x, double a, double r, double sdt, double sp0t, double tsp0t,
                                        double qp0t, boolean computeDerivative) {
        double p1 = a + r;
        double p2 = a - r;

        double p10t = p1 / sp0t;
        double p20t = p2 / sp0t;
        double p30t = r / sp0t;
        double p1exp = Math.exp(-Math.pow(p10t, 2));
        double p2exp = Math.exp(-Math.pow(p20t, 2));
        double p3exp = Math.exp(-Math.pow(p30t, 2));
        double pexp = p1exp + p2exp - (2 * p3exp);
        double perf = (p1 * Erf.erf(p10t)) + (p2 * Erf.erf(p20t)) - (2 * r * Erf.erf(p30t));
        double part = ((pexp * sp0t) / SQRT_PI) + perf;

        double derivative = 0;

        if (computeDerivative) {
            double d1exp = 4 * x * (p1exp * Math.pow(p1, 2));
            double d2exp = 4 * x * (p2exp * Math.pow(p2, 2));
            double d3exp = 4 * x * (p3exp * Math.pow(r, 2));
            double d0exp = 2 * x * (pexp);
            double dexp = d1exp + d2exp - (2 * d3exp);

            derivative = (1 / sdt) *
                    (INVERSE_SQRT_PI * ((d0exp / sp0t) - (dexp / tsp0t)) + (dexp / SQRT_PI) * (sp0t / qp0t));
        }

        return new double[]{part, derivative};
    }

    /**
     * Calculates the z components used in the model.
     *
     * @param z1 The first z coordinate.
     * @param z2 The second z coordinate.
     * @param D  The diffusion coefficient.
     * @param x  The x coordinate.
     * @param sz The size in the z direction.
     * @return An array containing the z difference and the z exponent.
     */
    private double[] calculateZComponents(double z1, double z2, double D, double x, double sz) {
        double zdiff = (z1 - z2);
        double z1exp = (2 / Math.pow(sz, 2)) * (Math.pow(z1, 2) + Math.pow(z2, 2));
        double z2exp = (Math.pow(zdiff, 2)) / (4 * D * x);
        double zexp = Math.exp(-(z1exp + z2exp));
        return new double[]{zdiff, zexp};
    }

    @Override
    public double[] gradient(double x, double[] params) {
        PixelModel.FitParameters p = new PixelModel.FitParameters(fitModel.fillParamsArray(params));

        double sdt = Math.sqrt(p.getD() * x);

        // numerical integration for z and z' components in the ACF. This is required as no analytical solutions were
        // found
        double[] sums = IntStream.range(0, LOOP_ITERATIONS).parallel().mapToObj(i -> {
            double z1 = calculateZ(i / 80);
            double z2 = calculateZ(i % 80);

            double psfxz1 = calculatePSFz(z1);
            double psfxz2 = calculatePSFz(z2);

            // help variables, which are dependent on time, to write the full function
            double p0t = ((8 * p.getD() * x) + Math.pow(psfxz1, 2) + Math.pow(psfxz2, 2)) / 2;
            double sp0t = Math.sqrt(p0t);
            double tsp0t = Math.pow(sp0t, 3);
            double qp0t = Math.pow(p0t, 2);

            double[] xComponents = calculateComponent(x, ax, rx, sdt, sp0t, tsp0t, qp0t, true);
            double[] yComponents = calculateComponent(x, ay, ry, sdt, sp0t, tsp0t, qp0t, true);

            double xPart = xComponents[0];
            double xDerivative = xComponents[1];

            double yPart = yComponents[0];
            double yDerivative = yComponents[1];

            double[] zComponents = calculateZComponents(z1, z2, p.getD(), x, sz);
            double zDiff = zComponents[0];
            double zExp = zComponents[1];

            double dt1 = -(0.5 * x) / (Math.pow(sdt, 3));
            double dt2 = (0.25 * (Math.pow((zDiff), 2))) / (x * sdt * Math.pow(p.getD(), 2));

            // TODO: double check that the two values are correct
            double sum = ((zExp * xPart * yPart) * ((sz * sz) / 400)) / sdt;
            double sumDerivative =
                    zExp * ((dt1 + dt2) * xPart * yPart + xPart * yDerivative + yPart * xDerivative) * (sz * sz / 400);
            return new double[]{sum, sumDerivative};
        }).reduce(new double[]{0, 0}, (a, b) -> new double[]{a[0] + b[0], a[1] + b[1]});

        double sum = sums[0];
        double sumDerivative = sums[1];

        double acf1 = (sum * 1000000) / (4 * Math.pow(ax * ay, 2) / (modifiedObservationVolume));
        double Dpspim = (sumDerivative * 1000000) / (4 * Math.pow(ax * ay, 2) / (modifiedObservationVolume));
        // TRIPLET
        double triplet = 1 + p.getFTrip() / (1 - p.getFTrip()) * Math.exp(-x / p.getTTrip());
        double dtripletFtrip =
                Math.exp(-x / p.getTTrip()) * (1 / (1 - p.getFTrip()) + p.getFTrip() / Math.pow(1 - p.getFTrip(), 2));
        double dtripletTtrip =
                Math.exp(-x / p.getTTrip()) * (p.getFTrip() * x) / ((1 - p.getFTrip()) * Math.pow(p.getTTrip(), 2));

        double pacf = ((1 / p.getN()) * acf1) * triplet + p.getG();

        double[] results = new double[]{
                (-1 / Math.pow(p.getN(), 2)) * acf1 * triplet,
                (1 / p.getN()) * (Dpspim),
                0,
                0,
                1,
                0,
                0,
                0,
                0,
                dtripletFtrip * pacf,
                dtripletTtrip * pacf
        };

        return fitModel.filterFitArray(results);
    }


    @Override
    public double value(double x, double[] params) {
        PixelModel.FitParameters p = new PixelModel.FitParameters(fitModel.fillParamsArray(params));

        double sdt = Math.sqrt(p.getD() * x);

        double sum = IntStream.range(0, LOOP_ITERATIONS).parallel().mapToDouble(i -> {
            double z1 = calculateZ(i / 80);
            double z2 = calculateZ(i % 80);

            double psfxz1 = calculatePSFz(z1);
            double psfxz2 = calculatePSFz(z2);

            // help variables, which are dependent on time, to write the full function
            double p0t = ((8 * p.getD() * x) + Math.pow(psfxz1, 2) + Math.pow(psfxz2, 2)) / 2;
            double sp0t = Math.sqrt(p0t);

            double[] xComponents = calculateComponent(x, ax, rx, sdt, sp0t, 0, 0, false);
            double[] yComponents = calculateComponent(x, ay, ry, sdt, sp0t, 0, 0, false);

            double xPart = xComponents[0];
            double yPart = yComponents[0];

            double[] zComponents = calculateZComponents(z1, z2, p.getD(), x, sz);
            double zExp = zComponents[1];

            return ((zExp * xPart * yPart) * ((sz * sz) / 400)) / sdt;
        }).sum();

        double acf1 = (sum * 1000000) / (4 * Math.pow(ax * ay, 2) / (modifiedObservationVolume));
        double triplet = 1 + p.getFTrip() / (1 - p.getFTrip()) * Math.exp(-x / p.getTTrip());

        return ((1 / p.getN()) * acf1) * triplet + p.getG();
    }
}