package fiji.plugin.imaging_fcs.imfcs.enums;

public enum CameraMode implements DisplayNameEnum {

    SINGLE_CAPTURE("Single Capture"),
    LIVE_VIDEO("Live Video"),
    CALIBRATION("Calibration"),
    ACQUISITION("Acquisition"),
    ICCS("ICCS");

    private final String displayName;

    CameraMode(String displayName) {
        this.displayName = displayName;
    }

    /**
     * Returns the human-readable label for this Camera mode.
     */
    @Override
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Returns the display name as the default string representation.
     */
    @Override
    public String toString() {
        return displayName;
    }
}
