package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.HardwareModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.ImageModel;
import fiji.plugin.imaging_fcs.new_imfcs.model.OptionsModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.ImageView;
import fiji.plugin.imaging_fcs.new_imfcs.view.MainPanelView;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;

import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

public class MainPanelController {
    private final MainPanelView view;
    private final HardwareModel hardwareModel;
    private final OptionsModel optionsModel;
    private final ImageModel imageModel;

    public MainPanelController(HardwareModel hardwareModel) {
        this.view = new MainPanelView(this);
        this.hardwareModel = hardwareModel;
        this.optionsModel = new OptionsModel(hardwareModel.isCuda());
        this.imageModel = new ImageModel();
    }

    public DocumentListener expSettingsChanged() {
        // TODO: FIXME
        return null;
    }

    public DocumentListener tfLastFrameChanged() {
        // TODO: FIXME
        return null;
    }

    public DocumentListener tfFirstFrameChanged() {
        // TODO: FIXME
        return null;
    }

    public ActionListener cbBleachCorChanged() {
        // TODO: FIXME
        return null;
    }

    public ActionListener cbFilterChanged() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnExitPressed() {
        return (ActionEvent ev) -> view.dispose();
    }

    public ActionListener btnBtfPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnUseExistingPressed() {
        return (ActionEvent ev) -> {
            if (WindowManager.getImageCount() > 0) {
                try {
                    imageModel.loadImage(IJ.getImage());
                    ImageView imageView = new ImageView();
                    imageView.showImage(imageModel);
                } catch (RuntimeException e) {
                    IJ.showMessage("Wrong image format", e.getMessage());
                }
            } else {
                IJ.showMessage("No image open.");
            }
        };
    }

    public ActionListener btnLoadNewPressed() {
        return (ActionEvent ev) -> {
            ImagePlus image = IJ.openImage();
            if (image != null) {
                try {
                    imageModel.loadImage(image);
                    ImageView imageView = new ImageView();
                    imageView.showImage(imageModel);
                } catch (RuntimeException e) {
                    IJ.showMessage("Wrong image format", e.getMessage());
                }
            }
        };
    }

    public ActionListener btnSavePressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnLoadPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnBatchPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnPSFPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbDLPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnOptionsPressed() {
        return (ActionEvent ev) -> new OptionsController(optionsModel);
    }

    public ItemListener tbNBPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener tbFilteringPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnAvePressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnParaCorPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnDCCFPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnRTPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnWriteConfigPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnDCRPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnParamVideoPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnDebugPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnROIPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener btnAllPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbFCCSDisplayPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbExpSettingsPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbBleachCorStridePressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbFitPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbSimPressed() {
        // TODO: FIXME
        return null;
    }

    public ActionListener tbMSDPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbOverlapPressed() {
        // TODO: FIXME
        return null;
    }

    public ItemListener tbBackgroundPressed() {
        // TODO: FIXME
        return null;
    }
}