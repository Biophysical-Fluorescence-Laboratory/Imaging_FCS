package fiji.plugin.imaging_fcs.new_imfcs;

import fiji.plugin.imaging_fcs.new_imfcs.controller.listeners.MainPanelController;
import ij.ImageJ;
import ij.plugin.PlugIn;

public class ImagingFCS implements PlugIn {

    public static void main(final String[] args) {
        ImageJ.main(args);
        new ImagingFCS().run("");
    }

    @Override
    public void run(String _arg) {
        MainPanelController mainController = new MainPanelController();
    }
}
