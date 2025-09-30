package fiji.plugin.imaging_fcs.imfcs.model;

import fiji.plugin.imaging_fcs.imfcs.model.correlations.Correlator;
import fiji.plugin.imaging_fcs.imfcs.utils.ApplyCustomLUT;
import fiji.plugin.imaging_fcs.imfcs.utils.ExcelExporter;
import fiji.plugin.imaging_fcs.imfcs.utils.Range;
import fiji.plugin.imaging_fcs.imfcs.view.Plots;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;

import java.awt.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

/**
 * Model for handling video parameters and generating parameter videos from image and fit data.
 */
public class ParameterVideoModel {
    private final ExpSettingsModel settings;
    private final ImageModel imageModel;
    private final FitModel interfaceFitModel;
    private int start, end, length, step;
    private boolean saveCFAndFitPVideo = false;
    private String videoName;
    private String excelDirectory;

    /**
     * Initializes the model with the necessary experiment settings, image data, and fit model.
     *
     * @param settings   the experiment settings model
     * @param imageModel the image model
     * @param fitModel   the fit model
     */
    public ParameterVideoModel(ExpSettingsModel settings, ImageModel imageModel, FitModel fitModel) {
        this.settings = settings;
        this.imageModel = imageModel;
        this.interfaceFitModel = fitModel;
    }

    /**
     * Removes the file extension from a given file path.
     *
     * @param filePath the file path
     * @return the file path without extension
     */
    private static String removeExtension(String filePath) {
        int dotIndex = filePath.lastIndexOf('.');
        if (dotIndex != -1) {
            return filePath.substring(0, dotIndex);
        }
        // Return the original string if no extension is found
        return filePath;
    }

    /**
     * Validates the parameters for video creation.
     */
    private void validateParameters() {
        if (start <= 0 || end <= 0 || end <= start || length <= 0 || step <= 0 || step > (end - start + 1) ||
                length > (end - start + 1)) {
            throw new IllegalArgumentException(
                    "Number error; either negative numbers or length or step size larger than length of frame.");
        }

        videoName = videoName.replaceAll("\\W", "");
        if (videoName.isEmpty()) {
            throw new IllegalArgumentException("Video name is empty or only contains non-word characters.");
        }
    }

    /**
     * Displays the image stack.
     *
     * @param title the title of the image
     * @param stack the image stack to display
     */
    private void displayImageStack(String title, ImageStack stack) {
        ImagePlus img = new ImagePlus(title, stack);
        img.show();
        ImageModel.adaptImageScale(img);

        ApplyCustomLUT.applyCustomLUT(img, "Red Hot");

        IJ.run(img, "Enhance Contrast", "saturated=0.35");
    }

    /**
     * Creates the parameter video based on the current model settings and displays it.
     */
    public void createParameterVideo() {
        // check the parameters and throw exception in case of incorrect parameter
        validateParameters();

        FitModel fitModel = new FitModel(settings, interfaceFitModel);
        fitModel.setFix(true);

        BleachCorrectionModel bleachCorrectionModel = new BleachCorrectionModel(settings, imageModel);
        Correlator correlator = new Correlator(settings, bleachCorrectionModel, fitModel);

        Dimension convertedDimension = settings.getConvertedImageDimension(imageModel.getDimension());

        Range[] rangesArea = settings.getAllArea(imageModel.getDimension());
        Range xRange = rangesArea[0];
        Range yRange = rangesArea[1];

        ImageStack stackN = new ImageStack(xRange.length() - 1, yRange.length() - 1);
        ImageStack stackD = new ImageStack(xRange.length() - 1, yRange.length() - 1);

        int numSteps = (end - start + 1 - length) / step + 1;
        int currentStep = 0;

        for (int startFrame = start; startFrame < end; startFrame += step) {
            int endFrame = startFrame + length - 1;

            PixelModel[][] pixelModels = new PixelModel[imageModel.getWidth()][imageModel.getHeight()];
            ImagePlus img = null;

            for (int x = xRange.getStart(); x < xRange.getEnd(); x += xRange.getStep()) {
                for (int y = yRange.getStart(); y < yRange.getEnd(); y += yRange.getStep()) {
                    PixelModel currentPixelModel = new PixelModel();
                    correlator.correlatePixelModel(currentPixelModel, imageModel.getImage(), x, y,
                            x + settings.getCCF().width, y + settings.getCCF().height, startFrame, endFrame);
                    fitModel.fit(currentPixelModel, settings.getFitModel(), correlator.getLagTimes(),
                            correlator.getRegularizedCovarianceMatrix());

                    Point binningPoint = settings.convertPointToBinning(new Point(x, y));
                    if (currentPixelModel.isAtLeastOneFitted() &&
                            !currentPixelModel.toFilter(fitModel, binningPoint.x, binningPoint.y)) {
                        img = Plots.setParameterMaps(img, currentPixelModel, binningPoint, convertedDimension,
                                settings.isFCCSDisp());
                    }

                    pixelModels[x][y] = currentPixelModel;
                }
            }

            if (isSaveCFAndFitPVideo()) {
                saveExcelFile(pixelModels, correlator, bleachCorrectionModel, startFrame, endFrame);
            }

            if (img != null) {
                stackN.addSlice(img.getStack().getProcessor(1));
                stackD.addSlice(img.getStack().getProcessor(2));
                IJ.showProgress(currentStep++, numSteps);
            } else {
                throw new RuntimeException("All pixels in the image were filtered.");
            }
        }

        displayImageStack(videoName + " - N", stackN);
        displayImageStack(videoName + " - D", stackD);
    }

    /**
     * Saves pixel model data and related settings to an Excel file.
     *
     * @param pixelModels           pixel model data
     * @param correlator            the correlator object
     * @param bleachCorrectionModel bleach correction model
     * @param start                 starting frame
     * @param end                   ending frame
     */
    private void saveExcelFile(PixelModel[][] pixelModels, Correlator correlator,
                               BleachCorrectionModel bleachCorrectionModel, int start, int end) {
        Path filePath = Paths.get(excelDirectory, String.format("%d_%d.xlsx", start, end));

        Map<String, Object> settingsMap = settings.toMap();
        settingsMap.put("First frame", start);
        settingsMap.put("Last frame", end);
        settingsMap.putAll(imageModel.toMap());
        settingsMap.put("Polynomial Order", bleachCorrectionModel.getPolynomialOrder());

        ExcelExporter.saveExcelFile(filePath.toAbsolutePath().toString(), settingsMap,
                (workbook) -> ExcelExporter.saveExcelPixelModels(workbook, pixelModels, settings, correlator));
    }

    // Getter and setter methods for parameters

    public void setStart(int start) {
        this.start = start;
    }

    public void setEnd(int end) {
        this.end = end;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public void setStep(int step) {
        this.step = step;
    }

    public boolean isSaveCFAndFitPVideo() {
        return saveCFAndFitPVideo;
    }

    public void setSaveCFAndFitPVideo(boolean saveCFAndFitPVideo) {
        this.saveCFAndFitPVideo = saveCFAndFitPVideo;
    }

    public String getVideoName() {
        return videoName;
    }

    public void setVideoName(String videoName) {
        this.videoName = videoName;
    }

    public void setExcelDirectory(String excelDirectory) {
        this.excelDirectory = removeExtension(excelDirectory);
    }
}
