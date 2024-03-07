package fiji.plugin.imaging_fcs.new_imfcs.model;

public class SimulationModel {
    private static final double DIFFUSION_COEFFICIENT_BASE = Math.pow(10, 12);
    private final ExpSettingsModel expSettingsModel;
    private boolean is2D;
    private boolean isDomain;
    private boolean isMesh;
    private boolean blinkFlag;
    private int seed = 1;
    private int numParticles = 1000; // number of simulated particles
    private int CPS = 10000; // average count rate per particle per second
    private double tauBleach = 0; // bleach time in seconds
    private int pixelNum = 21; // width of image in pixels
    private double extFactor = 1.5; // factor by which the simulated area is bigger than the observed area
    private int numFrames = 50000; // number of frames to be simulated
    private double frameTime = 0.001; // time resolution of the camera in second
    private int stepsPerFrame = 10; // simulation steps per frame
    private double D1 = 1.0 / DIFFUSION_COEFFICIENT_BASE; // particle 1 diffusion coefficient
    private double doutDinRatio = 1.0; // ratio of diffusion coefficients outside over inside of domains
    private double D2 = 0.1 / DIFFUSION_COEFFICIENT_BASE; // particle 2 diffusion coefficient
    private double D3 = 0.01 / DIFFUSION_COEFFICIENT_BASE; // particle 3 diffusion coefficient
    private double F2 = 0.0; // fraction of particle 2
    private double F3 = 0.0; // fraction of particle 3
    private double kon = 300.0; // on-rate for triplet
    private double koff = 700.0; // off-rate for triplet
    private int cameraOffset = 100; // offset of CCD camera
    private double cameraNoiseFactor = 3.0; // noise of CCD camera
    private double bleachRadius = 3.0; // bleach radius
    private int bleachFrame = 10000000; // frame at which bleach happens
    private double domainRadius = 30.0; // Radius of domains
    private double domainDensity = 30.0; // Density of domains in number/um2
    private double pin = 1.0; // Probability to enter domain
    private double pout = 0.6; // Probability to exit domain
    private double meshWorkSize = 100.0; // Size of meshes
    private double hopProbability = 1.0; // hop probability over meshwork barriers

    public SimulationModel(ExpSettingsModel expSettingsModel) {
        this.expSettingsModel = expSettingsModel;

        is2D = true;
        isDomain = false;
        isMesh = false;
        blinkFlag = false;
    }

    public void setIs2D(boolean is2D) {
        this.is2D = is2D;
    }

    public void setIsDomain(boolean isDomain) {
        this.isDomain = isDomain;
    }

    public void setIsMesh(boolean isMesh) {
        this.isMesh = isMesh;
    }

    public void setBlinkFlag(boolean blinkFlag) {
        this.blinkFlag = blinkFlag;
    }

    public int getSeed() {
        return seed;
    }

    public void setSeed(String seed) {
        this.seed = Integer.parseInt(seed);
    }

    public int getNumParticles() {
        return numParticles;
    }

    public void setNumParticles(String numParticles) {
        this.numParticles = Integer.parseInt(numParticles);
    }

    public int getCPS() {
        return CPS;
    }

    public void setCPS(String CPS) {
        this.CPS = Integer.parseInt(CPS);
    }

    public double getTauBleach() {
        return tauBleach;
    }

    public void setTauBleach(String tauBleach) {
        this.tauBleach = Double.parseDouble(tauBleach);
    }

    public int getPixelNum() {
        return pixelNum;
    }

    public void setPixelNum(String pixelNum) {
        this.pixelNum = Integer.parseInt(pixelNum);
    }

    public double getExtFactor() {
        return extFactor;
    }

    public void setExtFactor(String extFactor) {
        this.extFactor = Double.parseDouble(extFactor);
    }

    public int getNumFrames() {
        return numFrames;
    }

    public void setNumFrames(String numFrames) {
        this.numFrames = Integer.parseInt(numFrames);
    }

    public double getFrameTime() {
        return frameTime;
    }

    public void setFrameTime(String frameTime) {
        this.frameTime = Double.parseDouble(frameTime);
    }

    public int getStepsPerFrame() {
        return stepsPerFrame;
    }

    public void setStepsPerFrame(String stepsPerFrame) {
        this.stepsPerFrame = Integer.parseInt(stepsPerFrame);
    }

    public double getD1() {
        return D1 * DIFFUSION_COEFFICIENT_BASE;
    }

    public void setD1(String D1) {
        this.D1 = Double.parseDouble(D1) / DIFFUSION_COEFFICIENT_BASE;
    }

    public double getDoutDinRatio() {
        return doutDinRatio;
    }

    public void setDoutDinRatio(String doutDinRatio) {
        this.doutDinRatio = Double.parseDouble(doutDinRatio);
    }

    public double getD2() {
        return D2 * DIFFUSION_COEFFICIENT_BASE;
    }

    public void setD2(String D2) {
        this.D2 = Double.parseDouble(D2) / DIFFUSION_COEFFICIENT_BASE;
    }

    public double getD3() {
        return D3 * DIFFUSION_COEFFICIENT_BASE;
    }

    public void setD3(String D3) {
        this.D3 = Double.parseDouble(D3) / DIFFUSION_COEFFICIENT_BASE;
    }

    public double getF2() {
        return F2;
    }

    public void setF2(String F2) {
        this.F2 = Double.parseDouble(F2);
    }

    public double getF3() {
        return F3;
    }

    public void setF3(String F3) {
        this.F3 = Double.parseDouble(F3);
    }

    public double getKon() {
        return kon;
    }

    public void setKon(String kon) {
        this.kon = Double.parseDouble(kon);
    }

    public double getKoff() {
        return koff;
    }

    public void setKoff(String koff) {
        this.koff = Double.parseDouble(koff);
    }

    public int getCameraOffset() {
        return cameraOffset;
    }

    public void setCameraOffset(String cameraOffset) {
        this.cameraOffset = Integer.parseInt(cameraOffset);
    }

    public double getCameraNoiseFactor() {
        return cameraNoiseFactor;
    }

    public void setCameraNoiseFactor(String cameraNoiseFactor) {
        this.cameraNoiseFactor = Double.parseDouble(cameraNoiseFactor);
    }

    public double getBleachRadius() {
        return bleachRadius;
    }

    public void setBleachRadius(String bleachRadius) {
        this.bleachRadius = Double.parseDouble(bleachRadius);
    }

    public int getBleachFrame() {
        return bleachFrame;
    }

    public void setBleachFrame(String bleachFrame) {
        this.bleachFrame = Integer.parseInt(bleachFrame);
    }

    public double getDomainRadius() {
        return domainRadius;
    }

    public void setDomainRadius(String domainRadius) {
        this.domainRadius = Double.parseDouble(domainRadius);
    }

    public double getDomainDensity() {
        return domainDensity;
    }

    public void setDomainDensity(String domainDensity) {
        this.domainDensity = Double.parseDouble(domainDensity);
    }

    public double getPin() {
        return pin;
    }

    public void setPin(String pin) {
        this.pin = Double.parseDouble(pin);
    }

    public double getPout() {
        return pout;
    }

    public void setPout(String pout) {
        this.pout = Double.parseDouble(pout);
    }

    public double getMeshWorkSize() {
        return meshWorkSize;
    }

    public void setMeshWorkSize(String meshWorkSize) {
        this.meshWorkSize = Double.parseDouble(meshWorkSize);
    }

    public double getHopProbability() {
        return hopProbability;
    }

    public void setHopProbability(String hopProbability) {
        this.hopProbability = Double.parseDouble(hopProbability);
    }
}
