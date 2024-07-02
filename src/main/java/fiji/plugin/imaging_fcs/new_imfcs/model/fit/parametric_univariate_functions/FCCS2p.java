package fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.FitModel;

/**
 * The FCCS2p class represents a specific parametric univariate function for fitting
 * fluorescence cross-correlation spectroscopy (FCCS) data with two parameters.
 * It extends the FCSFit class, inheriting its methods and properties.
 */
public class FCCS2p extends FCSFit {
    /**
     * Constructs a new FCCS2p instance with the given settings and fit model.
     *
     * @param settings The experimental settings model.
     * @param fitModel The fit model.
     */
    public FCCS2p(ExpSettingsModel settings, FitModel fitModel) {
        super(settings, fitModel, 2);
    }
}