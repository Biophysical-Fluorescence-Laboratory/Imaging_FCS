package fiji.plugin.imaging_fcs.new_imfcs.model;

import fiji.plugin.imaging_fcs.gpufitImFCS.GpufitImFCS;

public class HardwareModel {
    private boolean cuda;
    private String cudaMessage;

    public HardwareModel() {
        this.cuda = GpufitImFCS.isCudaAvailable();
        this.cudaMessage = GpufitImFCS.ALERT;
    }

    public boolean isCuda() {
        return cuda;
    }

    public String getCudaMessage() {
        return cudaMessage;
    }
}
