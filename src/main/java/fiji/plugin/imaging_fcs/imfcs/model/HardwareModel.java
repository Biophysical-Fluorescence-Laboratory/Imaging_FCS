package fiji.plugin.imaging_fcs.imfcs.model;

import fiji.plugin.imaging_fcs.imfcs.utils.CheckOS;
import fiji.plugin.imaging_fcs.imfcs.utils.LibraryLoader;

import java.io.IOException;

import static fiji.plugin.imaging_fcs.gpufit.Gpufit.isCudaAvailable;

/**
 * Represents the hardware configuration model for the Imaging FCS application.
 * This class is responsible for determining if CUDA (GPU computing) is available
 * and fetching any relevant messages regarding the CUDA setup.
 */
public final class HardwareModel {
    private final boolean cuda;
    private String cudaMessage = "CUDA and libs loaded.";

    /**
     * Initializes the hardware model and checks if CUDA is available.
     * Sets the CUDA message accordingly.
     */
    public HardwareModel() {
        this.cuda = loadIfCudaAvailable();
    }

    /**
     * Loads the required GPU libraries based on the operating system.
     * Throws an exception if the OS is not supported or if the libraries cannot be loaded.
     *
     * @throws IOException if loading GPU libraries fails.
     */
    private static void loadGpuLibraries() throws IOException {
        CheckOS.OperatingSystem osType = CheckOS.getCurrentOS();
        if (osType == CheckOS.OperatingSystem.OTHER || osType == CheckOS.OperatingSystem.MAC) {
            throw new RuntimeException("GPU mode is currently supported only on Windows and Linux with NVIDIA GPU.");
        }

        String libName = (osType == CheckOS.OperatingSystem.WINDOWS) ? "agpufit.dll" : "libagpufit.so";
        LibraryLoader.loadNativeLibraries("/libs/gpufit/", libName);
    }

    /**
     * Checks if CUDA is available by loading GPU libraries and calling the native method.
     *
     * @return true if CUDA is available, false otherwise.
     */
    private boolean loadIfCudaAvailable() {
        try {
            loadGpuLibraries();
            boolean available = isCudaAvailable();
            if (!available) {
                cudaMessage = "CUDA is not available.";
            }
            return available;
        } catch (UnsatisfiedLinkError | Exception e) {
            cudaMessage = e.getMessage();
        }

        return false;
    }

    /**
     * Returns the CUDA availability status.
     *
     * @return true if CUDA is available, false otherwise.
     */
    public boolean isCuda() {
        return cuda;
    }

    /**
     * Returns the CUDA related message or alert.
     *
     * @return A string containing the CUDA message or alert.
     */
    public String getCudaMessage() {
        return cudaMessage;
    }
}