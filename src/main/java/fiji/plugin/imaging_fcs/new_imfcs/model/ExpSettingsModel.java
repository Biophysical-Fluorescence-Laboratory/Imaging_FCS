package fiji.plugin.imaging_fcs.new_imfcs.model;

import fiji.plugin.imaging_fcs.new_imfcs.constants.Constants;
import fiji.plugin.imaging_fcs.new_imfcs.controller.InvalidUserInputException;

import java.awt.*;

/**
 * Represents the experimental settings for imaging FCS.
 * This model includes various parameters related to imaging,
 * such as pixel size, magnification, numerical aperture (NA),
 * and others relevant for fluorescence correlation spectroscopy (FCS) analysis.
 */
public final class ExpSettingsModel {
    // User parameters with default values

    // minimum number of frames required for the sliding windows; this is used to calculate a useful correlatorq
    private final int SLIDING_WINDOW_MIN_FRAME = 20;

    //// Parameter that updates the non-user parameters
    private final Point binning = new Point(1, 1);
    private final Dimension CCF = new Dimension(0, 0);
    private double pixelSize = 24;
    private double magnification = 100;
    private double NA = 1.49;
    private double sigma = 0.8;
    private double emLambda = 515;
    private double sigma2 = 0.8;
    private double emLamdba2 = 600;
    private double sigmaZ = 1000000;
    private double sigmaZ2 = 1000000;

    //// Other user parameters
    private int firstFrame = 1;
    private int lastFrame = 0;
    private double frameTime = 0.001;
    private int correlatorP = 16;
    private int correlatorQ = 8;
    private String fitModel = Constants.ITIR_FCS_2D;
    private String paraCor = "N vs D";
    private String dCCF = "x direction";
    private String bleachCorrection = "none";
    private String filter = "none";
    private int filterLowerLimit = 0;
    private int filterUpperLimit = 65536;
    private boolean FCSSDisp = false;
    private boolean overlap = false;
    private boolean MSD3d = false;
    private boolean MSD = false;
    private int slidingWindowLength = 0;
    private int channelNumber = 0;
    private int lagGroupNumber = 0;

    // Non-user parameters (compute using user parameters)
    private double paramAx;
    private double paramAy;
    private double paramW;
    private double paramW2;
    private double paramZ;
    private double paramZ2;
    private double paramRx;
    private double paramRy;

    /**
     * Constructs an ExpSettingsModel and initializes settings by calling updateSettings.
     */
    public ExpSettingsModel() {
        updateSettings();
        updateChannelNumber();
    }

    /**
     * Updates the derived parameters based on the current settings.
     * This includes calculations for resolution and displacement adjustments.
     */
    public void updateSettings() {
        // Calculation of the axial resolution adjustment parameter based on pixel size and magnification.
        paramAx = pixelSize * 1000 / magnification * binning.x;
        paramAy = pixelSize * 1000 / magnification * binning.y;

        // Calculation of lateral and axial resolutions for both emission wavelengths.
        paramW = sigma * emLambda / NA;
        paramW2 = sigma2 * emLamdba2 / NA;
        paramZ = sigmaZ * emLambda / NA;
        paramZ2 = sigmaZ2 * emLamdba2 / NA;

        // Adjustments for lateral displacements.
        int cfXShift = 0;
        int cfYShift = 0;
        if (fitModel.equals(Constants.ITIR_FCS_2D) || fitModel.equals(Constants.SPIM_FCS_3D)) {
            cfXShift = CCF.width;
            cfYShift = CCF.height;
        }

        paramRx = pixelSize * 1000 / magnification * cfXShift;
        paramRy = pixelSize * 1000 / magnification * cfYShift;
    }

    /**
     * Updates the channel number and lag group number based on the bleach correction method.
     */
    public void updateChannelNumber() {
        if (bleachCorrection.equals(Constants.BLEACH_CORRECTION_SLIDING_WINDOW)) {
            lagGroupNumber = (int) Math.floor(
                    (Math.log((double) slidingWindowLength / (SLIDING_WINDOW_MIN_FRAME + correlatorP)) + 1) /
                            Math.log(2));
            channelNumber = correlatorP + (lagGroupNumber - 1) * correlatorP / 2 + 1;
        } else {
            lagGroupNumber = correlatorQ;
            channelNumber = correlatorP + (correlatorQ - 1) * correlatorP / 2 + 1;
        }
    }

    // Getters and setters for various parameters follow, allowing external modification and access to the settings.
    // These include straightforward implementations to set and get values for pixel size, magnification, NA, etc.
    // Some setters parse strings to double values, enabling easy handling of text input.
    // Additionally, there are methods to get and set the binning and CCF dimensions as strings for user interfaces.
    public double getPixelSize() {
        return pixelSize;
    }

    public void setPixelSize(String pixelSize) {
        this.pixelSize = Double.parseDouble(pixelSize);
    }

    public double getMagnification() {
        return magnification;
    }

    public void setMagnification(String magnification) {
        this.magnification = Double.parseDouble(magnification);
    }

    public double getNA() {
        return NA;
    }

    public void setNA(String NA) {
        this.NA = Double.parseDouble(NA);
    }

    public double getSigma() {
        return sigma;
    }

    public void setSigma(String sigma) {
        this.sigma = Double.parseDouble(sigma);
    }

    public double getEmLambda() {
        return emLambda;
    }

    public void setEmLambda(String emLambda) {
        this.emLambda = Double.parseDouble(emLambda);
    }

    public double getSigma2() {
        return sigma2;
    }

    public void setSigma2(String sigma2) {
        this.sigma2 = Double.parseDouble(sigma2);
    }

    public double getEmLamdba2() {
        return emLamdba2;
    }

    public void setEmLamdba2(String emLamdba2) {
        this.emLamdba2 = Double.parseDouble(emLamdba2);
    }

    public double getSigmaZ() {
        return sigmaZ;
    }

    public void setSigmaZ(String sigmaZ) {
        this.sigmaZ = Double.parseDouble(sigmaZ);
    }

    public double getSigmaZ2() {
        return sigmaZ2;
    }

    public void setSigmaZ2(String sigmaZ2) {
        this.sigmaZ2 = Double.parseDouble(sigmaZ2);
    }

    public double getParamAx() {
        return paramAx;
    }

    public double getParamAy() {
        return paramAy;
    }

    public double getParamW() {
        return paramW;
    }

    public double getParamZ() {
        return paramZ;
    }

    public double getParamZ2() {
        return paramZ2;
    }

    public double getParamRx() {
        return paramRx;
    }

    public double getParamRy() {
        return paramRy;
    }

    public Point getBinning() {
        return binning;
    }

    public void setBinning(String binning) {
        String[] parts = binning.replace(" ", "").split("x");
        int binningX = Integer.parseInt(parts[0]);
        int binningY = Integer.parseInt(parts[1]);

        if (binningX < 1 || binningY < 1) {
            throw new InvalidUserInputException("Binning can't be smaller than 1.");
        }

        this.binning.x = binningX;
        this.binning.y = binningY;
    }

    public String getBinningString() {
        return String.format("%d x %d", binning.x, binning.y);
    }

    public Dimension getCCF() {
        return CCF;
    }

    public void setCCF(String CCF) {
        String[] parts = CCF.replace(" ", "").split("x");
        this.CCF.width = Integer.parseInt(parts[0]);
        this.CCF.height = Integer.parseInt(parts[1]);
    }

    public String getCCFString() {
        return String.format("%d x %d", CCF.width, CCF.height);
    }

    public double getParamW2() {
        return paramW2;
    }

    public int getFirstFrame() {
        return firstFrame;
    }

    public void setFirstFrame(String firstFrame) {
        int intFirstFrame = Integer.parseInt(firstFrame);
        if (intFirstFrame >= lastFrame || intFirstFrame < 1) {
            throw new InvalidUserInputException(
                    "First frame set incorrectly, it needs to be between 1 and last frame" + ".");
        }

        this.firstFrame = intFirstFrame;
    }

    public int getLastFrame() {
        return lastFrame;
    }

    public void setLastFrame(String lastFrame) {
        int intLastFrame = Integer.parseInt(lastFrame);
        if (intLastFrame <= firstFrame || intLastFrame < 1) {
            throw new InvalidUserInputException("Last frame set incorrectly, it needs to be larger than first frame.");
        }

        this.lastFrame = intLastFrame;
    }

    public double getFrameTime() {
        return frameTime;
    }

    public void setFrameTime(String frameTime) {
        this.frameTime = Double.parseDouble(frameTime);
    }

    public int getCorrelatorP() {
        return correlatorP;
    }

    public void setCorrelatorP(String correlatorP) {
        this.correlatorP = Integer.parseInt(correlatorP);
    }

    public int getCorrelatorQ() {
        return correlatorQ;
    }

    public void setCorrelatorQ(String correlatorQ) {
        this.correlatorQ = Integer.parseInt(correlatorQ);
    }

    public String getFitModel() {
        return fitModel;
    }

    public void setFitModel(String fitModel) {
        this.fitModel = fitModel;
    }

    public String getParaCor() {
        return paraCor;
    }

    public void setParaCor(String paraCor) {
        this.paraCor = paraCor;
    }

    public String getdCCF() {
        return dCCF;
    }

    public void setdCCF(String dCCF) {
        this.dCCF = dCCF;
    }

    public String getBleachCorrection() {
        return bleachCorrection;
    }

    public void setBleachCorrection(String bleachCorrection) {
        this.bleachCorrection = bleachCorrection;
    }

    public String getFilter() {
        return filter;
    }

    public void setFilter(String filter) {
        this.filter = filter;
    }

    public boolean isFCSSDisp() {
        return FCSSDisp;
    }

    public void setFCSSDisp(String FCSSDisp) {
        this.FCSSDisp = Boolean.parseBoolean(FCSSDisp);
    }

    public int getFilterLowerLimit() {
        return filterLowerLimit;
    }

    public void setFilterLowerLimit(int filterLowerLimit) {
        this.filterLowerLimit = filterLowerLimit;
    }

    public int getFilterUpperLimit() {
        return filterUpperLimit;
    }

    public void setFilterUpperLimit(int filterUpperLimit) {
        this.filterUpperLimit = filterUpperLimit;
    }

    public boolean isOverlap() {
        return overlap;
    }

    public void setOverlap(boolean overlap) {
        this.overlap = overlap;
    }

    public boolean isMSD3d() {
        return MSD3d;
    }

    public void setMSD3d(boolean isMSD3d) {
        this.MSD3d = isMSD3d;
    }

    public boolean isMSD() {
        return MSD;
    }

    public void setMSD(boolean MSD) {
        this.MSD = MSD;
    }

    public int getChannelNumber() {
        return channelNumber;
    }

    public int getLagGroupNumber() {
        return lagGroupNumber;
    }

    public int getSlidingWindowLength() {
        return slidingWindowLength;
    }

    public void setSlidingWindowLength(int slidingWindowLength) {
        this.slidingWindowLength = slidingWindowLength;
    }
}
