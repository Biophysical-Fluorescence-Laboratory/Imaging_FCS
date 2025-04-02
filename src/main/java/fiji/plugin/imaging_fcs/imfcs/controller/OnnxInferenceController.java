
package fiji.plugin.imaging_fcs.imfcs.controller;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.filechooser.FileNameExtensionFilter;

import ai.onnxruntime.OrtException;
import fiji.plugin.imaging_fcs.imfcs.model.ExpSettingsModel;
import fiji.plugin.imaging_fcs.imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.imfcs.model.OnnxInferenceModel;
import fiji.plugin.imaging_fcs.imfcs.view.OnnxInferenceView;
import ij.IJ;

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
        this.view = new OnnxInferenceView(this, model);
    }
    
    // Load an ONNX Model.
    public void btnLoadPressed() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        FileNameExtensionFilter filter = new FileNameExtensionFilter("ONNX Protobuf Models", "onnx");
        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
             // Get the selected file object
            File selectedFile = fileChooser.getSelectedFile();

            // Get the absolute path of the selected file as a String
            String filePath = selectedFile.getAbsolutePath();

            // Print the file path
            System.out.println("Selected ONNX model file: " + filePath);
        }
    }
    
    // TODO: This should return an ImagePlus (?)
    public void infer(ImageModel imageModel, ExpSettingsModel expSettingsModel) throws OrtException {
        if (isActivated() && model.canFit()) {
            try {
                model.runInference(imageModel, expSettingsModel);
                // double[] modProbs = model.fit(pixelModel, modelName, lagTimes, correlationMatrix);
                // // update view
                // view.updateFitParams(pixelModel.getFitParams());
                // if (model.isBayes()) {
                //     view.updateModProbs(modProbs);
                //     view.updateHoldStatus();
                // }
            } catch (RuntimeException e) {
                IJ.log(String.format("%s at pixel x=%d, y=%d"));
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

    public OnnxInferenceView getView() {
        return view;
    }
}
