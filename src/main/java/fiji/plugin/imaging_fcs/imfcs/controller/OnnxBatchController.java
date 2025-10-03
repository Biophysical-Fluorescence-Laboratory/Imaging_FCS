package fiji.plugin.imaging_fcs.imfcs.controller;

import fiji.plugin.imaging_fcs.imfcs.model.onnx.OnnxBatchModel;
import fiji.plugin.imaging_fcs.imfcs.view.OnnxBatchView;
import ij.IJ;

import java.awt.event.ItemListener;

/**
 * Controller for the OnnxBatchView. Manages user input related to batch sizing
 * and prefetching, updating the OnnxBatchModel.
 */
public class OnnxBatchController {
    private final OnnxBatchModel model;
    private final OnnxBatchView view;

    // Assuming the setBatchSize/setNumPrefetchWorkers methods in the model 
    // now take Integer/int as arguments to match the View's getters:
    // NOTE: If the model uses String setters (as in your last provided model), 
    // the update methods below are obsolete, and only initialization is needed.

    public OnnxBatchController(OnnxBatchModel model) {
        this.model = model;
        this.view = new OnnxBatchView(this, model); 
        
        // Initialize the view with current model state
        this.updateViewFromModel();
        
        // Add listeners ONLY for components not using direct FocusListeners (i.e., the Checkbox)
        this.initializeListeners();
    }

    /**
     * Initializes listeners for the Batch View components.
     */
    private void initializeListeners() {
        // Listener for the Enable Batching CheckBox (ItemEvent)
        this.view.setBatchEnabledListener(this.createBatchEnabledListener());
        
        // REMOVED: Listener for Batch Size (Handled by FocusListener in View)
        // REMOVED: Listener for Prefetch Workers (Handled by FocusListener in View)
    }

    /**
     * Creates an ItemListener for the enable batching checkbox.
     */
    private ItemListener createBatchEnabledListener() {
        return e -> {
            boolean enabled = this.view.getBatchEnabledStatus();
            
            // 1. Update the Model based on the toggle state.
            this.model.setBatchingEnabled(enabled);
            
            // 2. Perform necessary UI and status updates.
            this.updateModelAndViewAfterToggle(enabled);
            IJ.log("ONNX Batching enabled: " + enabled);
            
            // Note: Since setBatchingEnabled in the model resets values if disabled, 
            // the updateViewFromModel call inside updateModelAndViewAfterToggle 
            // will automatically sync the text fields if the user disables batching.
        };
    }

    // Since text fields use FocusListeners writing directly to the Model 
    // (model::setBatchSize and model::setNumPrefetchWorkers), 
    // we should remove the manual update methods from the controller.
    /*
    private void updateBatchSize() { ... } // REMOVED
    private void updatePrefetchWorkers() { ... } // REMOVED
    */

    /**
     * Ensures the view reflects the model's state (used for initialization and
     * resetting defaults).
     */
    private void updateViewFromModel() {
        // Set text fields based on model values
        this.view.setBatchSizeText(String.valueOf(this.model.getBatchSize()));
        this.view.setPrefetchWorkersText(String.valueOf(this.model.getNumPrefetchWorkers()));

        // Set checkbox status
        this.view.setBatchEnabledStatus(this.model.isBatchingEnabled());
        
        // Set input fields enabled/disabled state
        this.view.setInputsEnabled(this.model.isBatchingEnabled());

        // Update status label on initialization
        String status = this.model.isBatchingEnabled() ? "Batching enabled." : "Status: Disabled";
        this.view.updateStatus(status);
    }
    
    /**
     * Updates the model's state in the view after the batch toggle is pressed.
     * 
     * @param enabled The new state of batching.
     */
    private void updateModelAndViewAfterToggle(boolean enabled) {
        // When batching is disabled, the model resets batchSize and workers to defaults.
        if (!enabled) {
            // Revert the view text fields to reflect the model's reset defaults (1 and 0)
            this.updateViewFromModel(); 
            this.view.updateStatus("Batching disabled. Default settings applied.");
        } else {
            this.view.updateStatus("Batching enabled.");
        }
        
        // Enable/Disable the text fields
        this.view.setInputsEnabled(enabled); 
    }

    public OnnxBatchView getView() {
        return view;
    }

    public OnnxBatchModel getModel() {
        return model;
    }
}
