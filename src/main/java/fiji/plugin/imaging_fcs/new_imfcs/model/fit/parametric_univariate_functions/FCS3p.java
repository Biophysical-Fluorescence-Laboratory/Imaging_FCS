package fiji.plugin.imaging_fcs.new_imfcs.model.fit.parametric_univariate_functions;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.FitModel;

/**
 * The FCS_3p class represents a specific parametric univariate function for fitting
 * fluorescence correlation spectroscopy (FCS) data with three parameters.
 * It extends the FCSFit class, inheriting its methods and properties.
 */
public class FCS_3p extends FCSFit {
    /**
     * Constructs a new FCS_3p instance with the given settings, fit model, and mode.
     *
     * @param settings The experimental settings model.
     * @param fitModel The fit model.
     * @param mode     The mode for the PSF size and light sheet thickness.
     */
    public FCS_3p(ExpSettingsModel settings, FitModel fitModel, int mode) {
        super(settings, fitModel, mode);
    }
}
