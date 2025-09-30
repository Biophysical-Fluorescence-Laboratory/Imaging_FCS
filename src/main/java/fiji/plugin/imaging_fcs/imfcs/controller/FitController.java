package fiji.plugin.imaging_fcs.imfcs.controller;

import fiji.plugin.imaging_fcs.imfcs.enums.FitFunctions;
import fiji.plugin.imaging_fcs.imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.imfcs.model.FitModel;
import fiji.plugin.imaging_fcs.imfcs.model.OptionsModel;
import fiji.plugin.imaging_fcs.imfcs.model.PixelModel;
import fiji.plugin.imaging_fcs.imfcs.model.correlations.Correlator;
import fiji.plugin.imaging_fcs.imfcs.utils.Pair;
import fiji.plugin.imaging_fcs.imfcs.view.FitView;
import fiji.plugin.imaging_fcs.imfcs.view.Plots;
import ij.IJ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Collections;
import java.util.function.Consumer;

/**
 * The FitController class handles the interactions between the FitModel and the FitView,
 * managing the fitting process and updating the view based on user actions.
 */
public class FitController {
    private final FitModel model;
    private final FitView view;
    private final Correlator correlator;
    private final ExpSettingsModel settings;
    private final OptionsModel options;
    private final Runnable updateSettingsField;

    /**
     * Constructs a new FitController with the given FitModel.
     *
     * @param model The FitModel instance.
     */
    public FitController(FitModel model, Correlator correlator, ExpSettingsModel settings,
                         OptionsModel options, Runnable updateSettingsField) {
        this.model = model;
        this.correlator = correlator;
        this.settings = settings;
        this.options = options;
        this.updateSettingsField = updateSettingsField;

        this.view = new FitView(this, model);
    }

    /**
     * Initiates the fitting process using the given pixel model, lag times, and correlation matrix.
     * Updates the view with the fit parameters if the fitting process can proceed.
     *
     * @param pixelModel        The pixel model to fit.
     * @param modelName         The name of the model to use for fitting.
     * @param lagTimes          The lag times for fitting.
     * @param correlationMatrix The correlation matrix used for fitting.
     * @param x                 The x coordinate of the pixel.
     * @param y                 The y coordinate of the pixel.
     */
    public void fit(PixelModel pixelModel, FitFunctions modelName, double[] lagTimes, double[][] correlationMatrix,
                    int x, int y) {
        if (isActivated() && model.canFit()) {
            try {
                double[] modProbs = model.fit(pixelModel, modelName, lagTimes, correlationMatrix);
                // update view
                view.updateFitParams(pixelModel.getFitParams());
                if (model.isBayes()) {
                    view.updateModProbs(modProbs);
                    view.updateHoldStatus();
                }
            } catch (RuntimeException e) {
                IJ.log(String.format("%s at pixel x=%d, y=%d", e.getClass().getName(), x, y));
                pixelModel.setFitted(false);
            }
        }
    }

    /**
     * Initiates the standard fitting process using the given pixel model and lag times.
     * Updates the view with the fit parameters if the fitting process can proceed.
     *
     * @param pixelModel The pixel model to fit.
     * @param modelName  The name of the model to use for fitting.
     * @param lagTimes   The lag times for fitting.
     */
    public void fit(PixelModel pixelModel, FitFunctions modelName, double[] lagTimes) {
        if (isActivated() && model.canFit()) {
            try {
                model.standardFit(pixelModel, modelName, lagTimes);
                view.updateFitParams(pixelModel.getFitParams());
            } catch (RuntimeException e) {
                IJ.log(String.format("%s on average ACF", e.getClass().getName()));
                pixelModel.setFitted(false);
            }
        }
    }

    /**
     * Updates the thresholds in the underlying model based on the provided {@code PixelModel}.
     *
     * @param pixelModel The {@code PixelModel} containing the data used to update the thresholds.
     */
    public void updateThresholds(PixelModel pixelModel) {
        model.updateThresholds(pixelModel);
    }

    /**
     * Determines if the pixel model needs to be filtered based on the current model settings.
     *
     * @param pixelModel The pixel model to check.
     * @param x          The x-coordinate of the pixel.
     * @param y          The y-coordinate of the pixel.
     * @return true if the pixel model should be filtered, false otherwise.
     */
    public boolean needToFilter(PixelModel pixelModel, int x, int y) {
        return pixelModel.toFilter(model, x, y);
    }

    /**
     * Resets the filters in the FitModel.
     */
    public void resetFilters() {
        model.resetFilters();
    }

    /**
     * Sets the active state of all ACF thresholds in the model.
     * This method delegates the activation or deactivation of ACF thresholds
     * to the underlying model.
     *
     * @param acfsActive {@code true} to activate ACF thresholds, {@code false} to deactivate them.
     */
    public void setAllAcfsThreshold(boolean acfsActive) {
        model.setAllAcfsThreshold(acfsActive);
    }

    /**
     * Sets the visibility of the view.
     *
     * @param b The visibility status.
     */
    public void setVisible(boolean b) {
        view.setVisible(b);
    }

    /**
     * Creates an ActionListener for the reset parameters button.
     * Resets the model and view to their default values.
     *
     * @return The ActionListener for the reset parameters button.
     */
    public ActionListener btnResetParametersPressed() {
        return (ActionEvent ev) -> {
            model.setDefaultValues();
            view.setDefaultValues();
        };
    }

    /**
     * Creates an ItemListener for the toggle button that fixes or frees parameters.
     * Updates the model and button text based on the button state.
     *
     * @return The ItemListener for the toggle button.
     */
    public ItemListener tbFixParPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setText(selected ? "Free" : "Fix");
            model.setFix(!selected);
        };
    }

    /**
     * Creates an ItemListener for toggle buttons that handle different options.
     * Updates the button color and calls the provided setter with the button state.
     *
     * @param setter The setter function to update the model.
     * @return The ItemListener for the toggle button.
     */
    public ItemListener tbOptionPressed(Consumer<Boolean> setter) {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setForeground(selected ? Color.BLACK : Color.LIGHT_GRAY);
            setter.accept(selected);
        };
    }

    /**
     * Creates an ActionListener for the test button to perform a theoretical fit
     * on the last correlated pixel model. If no correlation has been run, displays
     * a message to the user. Otherwise, fits and plots the correlation function.
     *
     * @return The ActionListener for the test button.
     */
    public ActionListener btnTestPressed() {
        return (ActionEvent ev) -> {
            view.refreshModel();
            Pair<Point[], PixelModel> lastUsedPixelModel = correlator.getLastUsedPixelModel();
            if (lastUsedPixelModel == null) {
                IJ.showMessage("You need to run at least one correlation to do a theoretical fit.");
            } else {
                PixelModel pixelModel = new PixelModel(lastUsedPixelModel.getRight());
                model.theoreticalFit(pixelModel, correlator.getLagTimes());
                Plots.plotCorrelationFunction(Collections.singletonList(pixelModel), settings.isFCCSDisp(),
                        correlator.getLagTimes(), lastUsedPixelModel.getLeft(), settings.getBinning(),
                        settings.getCCF(), model.getFitStart(), model.getFitEnd());
            }
        };
    }

    /**
     * Creates an ActionListener that updates the fit model selection in the experimental settings model.
     * This listener is triggered when the fit model combo box selection changes.
     *
     * @return an ActionListener that processes the action event
     */
    public ActionListener cbFitModelChanged(JComboBox<FitFunctions> comboBox) {
        return new ActionListener() {
            private FitFunctions previousSelection = (FitFunctions) comboBox.getSelectedItem();

            @Override
            public void actionPerformed(ActionEvent ev) {
                FitFunctions fitModel = (FitFunctions) comboBox.getSelectedItem();
                try {
                    if (!previousSelection.equals(fitModel)) {
                        settings.setFitModel(fitModel);
                        // Update previous selection to current if successful
                        previousSelection = fitModel;

                        // If the model is not DC_FCCS_2D, deactivate FCCSDisp
                        boolean isDCFCCS = fitModel == FitFunctions.DC_FCCS_2D;

                        if (fitModel == FitFunctions.DC_FCCS_2D && options.isUseGpu()) {
                            IJ.showMessage("Warning",
                                "DC-FCCS is not supported on GPU, fitting will run on CPU.");
                        }

                        settings.setFCCSDisp(isDCFCCS);
                        // update the threshold fields depending on the model used.
                        setAllAcfsThreshold(isDCFCCS);

                        updateSettingsField.run();
                    }
                } catch (RejectResetException e) {
                    comboBox.setSelectedItem(previousSelection);
                }
            }
        };
    }

    /**
     * Updates the fit end position based on the current experimental settings.
     * This method is invoked later on the event dispatch thread.
     *
     * @param settings The experimental settings model.
     */
    public void updateFitEnd(ExpSettingsModel settings) {
        Runnable doUpdateFitEnd = () -> {
            settings.updateChannelNumber();
            model.resetFitEnd();
            view.updateFitEnd();
        };

        SwingUtilities.invokeLater(doUpdateFitEnd);
    }

    /**
     * Updates the fit parameters in the view using the provided fit parameters.
     *
     * @param fitParams The fit parameters from the pixel model.
     */
    public void updateFitParams(PixelModel.FitParameters fitParams) {
        view.updateFitParams(fitParams);
    }

    /**
     * Dispose the view
     */
    public void dispose() {
        this.view.dispose();
    }

    /**
     * Bring the view to front
     */
    public void toFront() {
        this.view.toFront();
    }

    /**
     * Checks if the view is currently visible (activated).
     *
     * @return true if the view is visible, false otherwise.
     */
    public boolean isActivated() {
        return view.isVisible();
    }

    /**
     * Checks if the Generalized Least Squares (GLS) fitting method is selected.
     *
     * @return true if GLS is selected, false otherwise.
     */
    public boolean isGLS() {
        return model.isGLS();
    }

    public int getFitStart() {
        return model.getFitStart();
    }

    public int getFitEnd() {
        return model.getFitEnd();
    }

    public FitFunctions getFitModel() {
        return settings.getFitModel();
    }

    public void setFitModel(FitFunctions fitModel) {
        settings.setFitModel(fitModel);
        updateSettingsField.run();
    }

    public FitModel getModel() {
        return model;
    }

    public void setFitModelField(FitFunctions fitModel) {
        view.setFitModel(fitModel);
    }
}
