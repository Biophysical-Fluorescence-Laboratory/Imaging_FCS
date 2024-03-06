package fiji.plugin.imaging_fcs.new_imfcs.model;

import java.awt.*;

public class ExpSettingsModel {
    private Point binning = new Point(1, 1);
    private Dimension CCF = new Dimension(0, 0);
    private double pixelSize = 24;
    private double magnification = 100;
    private double NA = 1.49;
    private double sigma = 0.8;
    private double emLambda = 515;
    private double sigma2 = 0.8;
    private double emLamdba2 = 600;
    private double sigmaZ = 1000000;
    private double sigmaZ2 = 1000000;
    private double paramA = 0;
    private double paramW = 0;
    private double paramW2 = 0;
    private double paramZ = 0;
    private double paramZ2 = 0;
    private double paramRx = 0;
    private double paramRy = 0;

    public ExpSettingsModel() {
    }

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

    public double getParamA() {
        return paramA;
    }

    public void setParamA(String paramA) {
        this.paramA = Double.parseDouble(paramA);
    }

    public double getParamW() {
        return paramW;
    }

    public void setParamW(String paramW) {
        this.paramW = Double.parseDouble(paramW);
    }

    public double getParamZ() {
        return paramZ;
    }

    public void setParamZ(String paramZ) {
        this.paramZ = Double.parseDouble(paramZ);
    }

    public double getParamZ2() {
        return paramZ2;
    }

    public void setParamZ2(String paramZ2) {
        this.paramZ2 = Double.parseDouble(paramZ2);
    }

    public double getParamRx() {
        return paramRx;
    }

    public void setParamRx(String paramRx) {
        this.paramRx = Double.parseDouble(paramRx);
    }

    public double getParamRy() {
        return paramRy;
    }

    public void setParamRy(String paramRy) {
        this.paramRy = Double.parseDouble(paramRy);
    }

    public Point getBinning() {
        return binning;
    }

    public void setBinning(String binning) {
        String[] parts = binning.split(" x ");
        this.binning.x = Integer.parseInt(parts[0]);
        this.binning.y = Integer.parseInt(parts[1]);
    }

    public String getBinningString() {
        return String.format("%d x %d", binning.x, binning.y);
    }

    public Dimension getCCF() {
        return CCF;
    }

    public void setCCF(String CCF) {
        String[] parts = CCF.split(" x ");
        this.CCF.width = Integer.parseInt(parts[0]);
        this.CCF.height = Integer.parseInt(parts[1]);
    }

    public String getCCFString() {
        return String.format("%d x %d", CCF.width, CCF.height);
    }

    public double getParamW2() {
        return paramW2;
    }

    public void setParamW2(double paramW2) {
        this.paramW2 = paramW2;
    }
}
