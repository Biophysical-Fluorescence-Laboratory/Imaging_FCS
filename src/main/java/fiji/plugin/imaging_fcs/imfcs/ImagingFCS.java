package fiji.plugin.imaging_fcs.imfcs;

import fiji.plugin.imaging_fcs.imfcs.controller.SplashScreenController;
import fiji.plugin.imaging_fcs.imfcs.view.UIUtils;
import ij.ImageJ;
import ij.plugin.PlugIn;

public final class ImagingFCS implements PlugIn {
    public static void main(final String[] args) {
        ImageJ.main(args);
        new ImagingFCS().run("");
    }

    @Override
    public void run(String _arg) {
        UIUtils.setUIFont();
        new SplashScreenController();
    }
}
