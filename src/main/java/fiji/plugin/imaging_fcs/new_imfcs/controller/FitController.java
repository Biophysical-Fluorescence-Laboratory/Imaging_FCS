package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.FitModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.PixelModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.FitView;
import ij.IJ;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.function.Consumer;

/**
 * The FitController class handles the interactions between the FitModel and the FitView,
 * managing the fitting process and updating the view based on user actions.
 */
public class FitController {
    private final FitModel model;
    private final FitView view;

    /**
     * Constructs a new FitController with the given FitModel.
     *
     * @param model The FitModel instance.
     */
    public FitController(FitModel model) {
        this.model = model;
        this.view = new FitView(this, model);
    }

    /**
     * Initiates the fitting process using the given pixel model and lag times.
     * Updates the view with the fit parameters if the fitting process can proceed.
     *
     * @param pixelModel The pixel model to fit.
     * @param lagTimes   The lag times for fitting.
     * @param x          The x coordinate of the pixel.
     * @param y          The y coordinate of the pixel.
     */
    public void fit(PixelModel pixelModel, double[] lagTimes, double[][] correlationMatrix, int x, int y) {
        if (isActivated() && model.canFit()) {
            try {
                double[] modProbs = model.fit(pixelModel, lagTimes, correlationMatrix);
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
            button.setText(selected ? "Fix" : "Free");
            model.setFix(selected);
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
}
