package fiji.plugin.imaging_fcs.directCameraReadout.cameras;

import java.io.File;

public abstract class Camera {
    public Camera() {
        initialize();
    }

    protected abstract void initialize();

    public abstract void shutdown();

    // Temperature and cooling control
    public abstract void setTemperature(int temp);

    public abstract void setCooling(boolean isCool);

    public abstract void setFan(String fan);

    public abstract int[] getMinMaxTemperature();

    public abstract void updateTemperature(); // For runThread_UpdateTemp

    // Detector information
    public abstract int[] getDetectorDimension();

    // Shutter control (optional for some cameras)
    public abstract void shutterControl(boolean isShutterOn);

    // Capture methods
    public abstract void singleCapture(boolean isFF); // For runThread_SingleCapture

    public abstract void liveVideoCapture(); // For runThread_LiveVideo

    public abstract void nonCumulativeCapture(); // For runThread_nonCumulative

    public abstract void cumulativeCapture(); // For runThread_Cumulative

    public abstract void iccsCalibrationCapture(); // For runThread_ICCScalibration

    // Stop mechanism
    public abstract void setStopMechanism(boolean isStopPressed);

    // File operations
    public abstract void writeExcel(File file, String exception, boolean showLog);
}
