package fiji.plugin.imaging_fcs.new_imfcs.controller;

import fiji.plugin.imaging_fcs.new_imfcs.model.HardwareModel;
import fiji.plugin.imaging_fcs.new_imfcs.view.MainPanelView;

import javax.swing.event.DocumentListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemListener;

public class MainPanelController {
    private final MainPanelView view;
    private final HardwareModel hardwareModel;

    public MainPanelController(HardwareModel hardwareModel) {
        this.view = new MainPanelView(this);
        this.hardwareModel = hardwareModel;
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
        // TODO: FIXME
        return null;
    }

    public ActionListener btnLoadNewPressed() {
        // TODO: FIXME
        return null;
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
        // TODO: FIXME
        return null;
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