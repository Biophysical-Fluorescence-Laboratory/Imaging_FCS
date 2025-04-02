
package fiji.plugin.imaging_fcs.imfcs.controller;

import fiji.plugin.imaging_fcs.imfcs.model.OnnxInferenceModel;
import fiji.plugin.imaging_fcs.imfcs.view.OnnxInferenceView;

/**
 * The OnnxInferenceController class handles the interactions between the OnnxInferneceModel and the OnnxInferenceView,
 * managing the fitting process and updating the view based on user actions.
 */
public class OnnxInferenceController {
    private final OnnxInferenceModel model;
    private final OnnxInferenceView view;

    /**
     * Constructs a new OnnxInferenceController with the given OnnxInferenceModel.
     *
     * @param model The OnnxInferenceModel instance.
     */
    public OnnxInferenceController(OnnxInferenceModel model) {
        this.model = model;
        this.view = new OnnxInferenceView(model);
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

    public OnnxInferenceModel getModel() {
        return model;
    }
}
