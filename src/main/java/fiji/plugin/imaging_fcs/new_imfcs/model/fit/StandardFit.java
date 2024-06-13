package fiji.plugin.imaging_fcs.new_imfcs.model.fit;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.FitModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.PixelModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions.FCCS2p;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions.FCS3p;
import fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions.FCS3pSPIM;
import fiji.plugin.imaging_fcs.new_imfcs.utils.Pair;
import org.apache.commons.math3.analysis.ParametricUnivariateFunction;
import org.apache.commons.math3.fitting.WeightedObservedPoint;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresOptimizer;
import org.apache.commons.math3.fitting.leastsquares.LeastSquaresProblem;

import java.util.ArrayList;
import java.util.Collection;

/**
 * The StandardFit class performs the standard fitting procedure for fluorescence correlation spectroscopy (FCS) data.
 * It extends the BaseFit class and uses a specific parametric univariate function for the fitting process.
 */
public class StandardFit extends BaseFit {
    private final int MAX_EVALUATIONS = 2000;
    private final int MAX_ITERATIONS = 2000;
    private final FitModel model;
    private final ParametricUnivariateFunction function;
    private int numFreeParameters;

    /**
     * Constructs a new StandardFit instance with the given model and settings.
     *
     * @param model    The FitModel instance containing the fitting parameters.
     * @param settings The experimental settings model.
     */
    public StandardFit(FitModel model, ExpSettingsModel settings) {
        this.model = model;
        function = selectFitFunction(settings);

        setMaxEvaluations(MAX_EVALUATIONS);
        setMaxIterations(MAX_ITERATIONS);
    }

    /**
     * Selects the appropriate parametric univariate function based on the experimental settings.
     *
     * @param settings The experimental settings model.
     * @return The selected parametric univariate function.
     */
    private ParametricUnivariateFunction selectFitFunction(ExpSettingsModel settings) {
        switch (settings.getFitModel()) {
            case Constants.ITIR_FCS_2D:
                return new FCS3p(settings, model, 0); // TODO: Set mode
            case Constants.SPIM_FCS_3D:
                return new FCS3pSPIM(settings, model);
            case Constants.DC_FCCS_2D:
                return new FCCS2p(settings, model);
            default:
                throw new IllegalArgumentException("Unknown fit model: " + settings.getFitModel());
        }
    }

    @Override
    protected LeastSquaresProblem getProblem(Collection<WeightedObservedPoint> points) {
        Pair<double[], double[]> targetAndWeights = createTargetAndWeights(points);
        double[] target = targetAndWeights.getLeft();
        double[] weights = targetAndWeights.getRight();

        double[] initialGuess = model.getNonHeldParameterValues();
        numFreeParameters = initialGuess.length;

        return getLeastSquaresProblem(points, function, initialGuess, target, weights);
    }

    /**
     * Fits the data for a pixel model using the given lag times.
     *
     * @param pixelModel The pixel model to fit.
     * @param lagTimes   The lag times for fitting.
     * @return An array of residuals.
     */
    public double[] fitPixel(PixelModel pixelModel, double[] lagTimes) {
        ArrayList<WeightedObservedPoint> points = new ArrayList<>();
        int channelNumber = pixelModel.getAcf().length;

        for (int i = model.getFitStart(); i <= model.getFitEnd(); i++) {
            points.add(new WeightedObservedPoint(
                    1 / pixelModel.getVarianceAcf()[i], lagTimes[i], pixelModel.getAcf()[i]));
        }

        LeastSquaresOptimizer.Optimum optimum = getOptimizer().optimize(getProblem(points));

        double[] result = optimum.getPoint().toArray();
        double[] tmpResiduals = optimum.getResiduals().toArray();
        double[] tres = new double[channelNumber - 1];

        double[] fitAcf = new double[channelNumber];
        double[] residuals = new double[channelNumber];

        for (int i = 1; i < channelNumber; i++) {
            if (model.getFitStart() <= i && i <= model.getFitEnd()) {
                fitAcf[i] = function.value(lagTimes[i], result);
                residuals[i] = tmpResiduals[i - model.getFitStart()];
                tres[i - 1] = pixelModel.getAcf()[i] - fitAcf[i];
            } else {
                fitAcf[i] = 0;
                residuals[i] = 0;
                tres[i - 1] = 0;
            }
        }

        double chi2 = 0;
        for (int i = model.getFitStart(); i <= model.getFitEnd(); i++) {
            // calculate chi2 value; do not include the 0 lagtime kcf which contains shot noise
            chi2 += Math.pow(residuals[i], 2) / ((model.getFitEnd() - model.getFitStart()) - numFreeParameters - 1);
        }
        pixelModel.setChi2(chi2);
        pixelModel.setFitted(true);

        pixelModel.setFittedAcf(fitAcf);
        pixelModel.setResiduals(residuals);

        pixelModel.setFitParams(new PixelModel.FitParameters(model.fillParamsArray(result)));
        if (!model.isFix()) {
            model.updateParameterValues(pixelModel.getFitParams());
        }

        return tres;
    }
}