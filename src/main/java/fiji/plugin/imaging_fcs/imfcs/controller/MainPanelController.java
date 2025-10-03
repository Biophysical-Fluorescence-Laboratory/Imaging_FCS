package fiji.plugin.imaging_fcs.imfcs.controller;

import fiji.plugin.imaging_fcs.directCameraReadout.DirectCapture;
import fiji.plugin.imaging_fcs.imfcs.enums.BleachCorrectionMethod;
import fiji.plugin.imaging_fcs.imfcs.enums.DccfDirection;
import fiji.plugin.imaging_fcs.imfcs.enums.FilterMode;
import fiji.plugin.imaging_fcs.imfcs.model.*;
import fiji.plugin.imaging_fcs.imfcs.model.correlations.AverageCorrelation;
import fiji.plugin.imaging_fcs.imfcs.model.correlations.Correlator;
import fiji.plugin.imaging_fcs.imfcs.model.correlations.DeltaCCFWorker;
import fiji.plugin.imaging_fcs.imfcs.model.correlations.MeanSquareDisplacement;
import fiji.plugin.imaging_fcs.imfcs.utils.*;
import fiji.plugin.imaging_fcs.imfcs.view.BleachCorrectionView;
import fiji.plugin.imaging_fcs.imfcs.view.ExpSettingsView;
import fiji.plugin.imaging_fcs.imfcs.view.MainPanelView;
import fiji.plugin.imaging_fcs.imfcs.view.Plots;
import fiji.plugin.imaging_fcs.imfcs.view.dialogs.*;
import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.Overlay;
import ij.gui.Roi;
import org.apache.poi.ss.usermodel.Workbook;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.FileSystems;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import static fiji.plugin.imaging_fcs.imfcs.controller.FieldListenerFactory.createFocusListener;

/**
 * Controller class for the main panel of the FCS plugin. It handles user interactions and coordinates
 * the update of models based on user input, as well as the update of views to reflect the current state
 * of the models.
 */
public final class MainPanelController {
    // Field declarations for the views and models that this controller will manage
    private final MainPanelView view;
    private final ExpSettingsView expSettingsView;
    private final BleachCorrectionView bleachCorrectionView;
    private final OptionsModel optionsModel;
    private final ImageController imageController;
    private final ExpSettingsModel settings;
    private final BleachCorrectionModel bleachCorrectionModel;
    private final SimulationController simulationController;
    private final BackgroundSubtractionController backgroundSubtractionController;
    private final NBController nbController;
    private final FitController fitController;
    private final DiffusionLawController diffusionLawController;
    private final FilteringController filteringController;
    private final ParameterVideoController parameterVideoController;
    private final Correlator correlator;
    private final OnnxInferenceModel onnxInferenceModel;
    private final OnnxInferenceController onnxInferenceController;

    /**
     * Constructor that initializes models, views, and other controllers needed for the main panel.
     * This constructor initializes the MainPanelController with the given hardware settings model.
     * It calls another constructor to initialize the options model based on whether CUDA is enabled.
     *
     * @param hardwareModel The model containing hardware settings for the imaging FCS analysis.
     */
    public MainPanelController(HardwareModel hardwareModel) {
        this(new OptionsModel(hardwareModel.isCuda()), null);
    }

    /**
     * Constructor that initializes models, views, and other controllers needed for the main panel.
     * This constructor initializes the MainPanelController with the given options model and an Excel workbook.
     * If a workbook is provided, it loads settings from the workbook; otherwise, it loads previously saved
     * configurations.
     *
     * @param optionsModel The model containing options settings for the imaging FCS analysis.
     * @param workbook     The Excel workbook containing previously saved configuration, or null to load saved config.
     */
    public MainPanelController(OptionsModel optionsModel, Workbook workbook) {
        this.optionsModel = optionsModel;

        this.settings = new ExpSettingsModel(this::askResetResults);

        FitModel fitModel = new FitModel(settings);

        ImageModel imageModel = new ImageModel(settings, this::askResetResults);
        this.backgroundSubtractionController = new BackgroundSubtractionController(imageModel, this::askResetResults);
        this.bleachCorrectionModel = new BleachCorrectionModel(settings, imageModel);
        this.correlator = new Correlator(settings, bleachCorrectionModel, fitModel);
        this.fitController = new FitController(fitModel, correlator, settings, optionsModel, this::updateSettingsField);
        this.imageController =
                new ImageController(imageModel, backgroundSubtractionController, fitController, bleachCorrectionModel,
                        correlator, settings, optionsModel, this::setLastFrame);

        this.simulationController = new SimulationController(imageController, settings);

        this.nbController = new NBController(imageModel, settings, optionsModel, bleachCorrectionModel, correlator);

        this.diffusionLawController =
                new DiffusionLawController(settings, optionsModel, imageModel, fitModel, bleachCorrectionModel);
        imageController.setSetDiffusionLawRange(diffusionLawController::setDefaultRange);

        this.filteringController =
                new FilteringController(settings, optionsModel, imageController, fitController, fitModel, correlator);
        imageController.setRefreshThresholdView(filteringController::refreshFilteringView);

        this.parameterVideoController = new ParameterVideoController(settings, imageModel, fitModel);

        if (workbook == null) {
            // load previously saved configuration
            loadConfig();
        } else {
            // load parameters from the excel file
            try {
                loadExcelSettings(workbook, imageController);
            } catch (Exception e) {
                // if we fail to read the excel file, we just setup the class the normal way
                IJ.showMessage("Error", "Excel format is incorrect.");
                loadConfig();
                workbook = null;
            }
        }

        // set the different views
        this.fitController.setFitModelField(settings.getFitModel());
        this.bleachCorrectionView = new BleachCorrectionView(this, bleachCorrectionModel);
        this.expSettingsView = new ExpSettingsView(this, settings);
        updateSettingsField();
        this.view = new MainPanelView(this, this.settings);

        // ONNX Inference-specific views.
        this.onnxInferenceModel = new OnnxInferenceModel(bleachCorrectionModel, optionsModel.isCuda());
        this.onnxInferenceController = new OnnxInferenceController(this.onnxInferenceModel, imageModel, settings);

        if (workbook != null) {
            // read the Excel file to restore parameters
            correlator.loadResultsFromWorkbook(workbook, imageModel.getDimension());

            // Plot the restored pixel models
            imageController.plotAll();
        }
    }

    /**
     * Constructs the file path to the configuration file in the user's home directory.
     *
     * @return the file path to the configuration file.
     */
    private String getConfigPath() {
        String userHomeDir = System.getProperty("user.home");
        String configFileName = ".ImFCS_config.yaml";
        return userHomeDir + FileSystems.getDefault().getSeparator() + configFileName;
    }

    /**
     * Loads the configuration from the YAML file located at the path specified by getConfigPath.
     * The method reads the file, parses the YAML content, and updates the settings and options models.
     * If an error occurs during reading or parsing, a message is logged.
     */
    private void loadConfig() {
        Yaml yaml = new Yaml();
        try (FileInputStream inputStream = new FileInputStream(getConfigPath())) {
            Map<String, Map<String, Object>> data = yaml.load(inputStream);
            settings.fromMap(data.get("Settings"));
            optionsModel.fromMap(data.get("Options"));
        } catch (Exception e) {
            IJ.log("Can't read configuration file.");
        }
    }

    /**
     * Loads settings from the provided Excel workbook and applies them to the image model through the ImageController.
     * This method reads the experimental settings from the workbook, attempts to reload the image
     * from the specified path, and updates the settings and image model with the retrieved data.
     *
     * @param workbook        the Excel workbook containing the settings to be loaded
     * @param imageController the ImageController instance to be updated with the loaded settings
     */
    private void loadExcelSettings(Workbook workbook, ImageController imageController) {
        Map<String, Object> expSettingsMap = ExcelReader.readSheetToMap(workbook, "Experimental settings");

        ImagePlus reloadImage = IJ.openImage(expSettingsMap.get("Image path").toString());
        if (reloadImage == null) {
            IJ.log(String.format("Fail to load '%s' from saved Excel file",
                    expSettingsMap.get("Image path").toString()));
            reloadImage = ImageLoader.openImagePlusWithDialog();
        }

        if (reloadImage != null) {
            imageController.loadImage(reloadImage, null);
        }

        settings.fromMapExcelLoading(expSettingsMap);
        bleachCorrectionModel.setPolynomialOrder(Integer.parseInt(expSettingsMap.get("Polynomial Order").toString()));
        imageController.fromMap(expSettingsMap);
        imageController.setFilterArray();
    }

    /**
     * Prompts the user to confirm whether to reset the current results due to changes
     * in parameter settings. If the user confirms, it resets the correlator's results
     * and closes any open plots. If the user declines, it throws a {@link RejectResetException}.
     * <p>
     * This method checks if there are existing results in the correlator's pixel model,
     * and if so, it displays a confirmation dialog. This ensures that users are aware of
     * the consequences of changing certain settings, which could lead to the deletion of
     * current results.
     * </p>
     *
     * @throws RejectResetException if the user chooses not to proceed with the reset.
     */
    private void askResetResults() {
        if (correlator.getPixelModels() != null) {
            int response = JOptionPane.showConfirmDialog(null,
                    "Some of the parameter settings in the main panel have changed. \n" +
                            "Continuing will result in deleting some Results", "Delete the Results and start new?",
                    JOptionPane.YES_NO_OPTION);

            if (response == JOptionPane.YES_OPTION) {
                correlator.resetResults();
                fitController.resetFilters();
                filteringController.refreshFilteringView();
                Plots.closePlots();
            } else {
                throw new RejectResetException();
            }
        }
    }

    /**
     * Sets the last frame for the analysis and updates related fields.
     *
     * @param lastFrame The last frame number to be set.
     */
    public void setLastFrame(int lastFrame) {
        if (view != null) {
            view.setTfLastFrame(String.valueOf(lastFrame));
        }

        settings.setLastFrame(String.valueOf(lastFrame));
        updateStrideParamFields();
    }

    /**
     * Handles different bleach correction modes and initializes appropriate views for further user inputs
     * depending on the selected mode.
     *
     * @return ActionListener to handle bleach correction mode changes.
     */
    public ActionListener cbBleachCorChanged(JComboBox<BleachCorrectionMethod> comboBox) {
        Consumer<Integer> onBleachCorrectionSlidingWindowAccepted = this::onBleachCorrectionSlidingWindowAccepted;
        Consumer<Integer> onBleachCorrectionOrderAccepted = this::onBleachCorrectionOrderAccepted;
        return new ActionListener() {
            private BleachCorrectionMethod previousSelection = (BleachCorrectionMethod) comboBox.getSelectedItem();

            @Override
            public void actionPerformed(ActionEvent ev) {
                BleachCorrectionMethod currentSelection = (BleachCorrectionMethod) comboBox.getSelectedItem();

                if ((currentSelection == BleachCorrectionMethod.SLIDING_WINDOW ||
                        currentSelection == BleachCorrectionMethod.LINEAR_SEGMENT) &&
                        !imageController.isImageLoaded()) {
                    IJ.showMessage("No image open. Please open an image first.");
                    // reset the combo box to default in no image is loaded.
                    comboBox.setSelectedItem(previousSelection);
                    return;
                }

                try {
                    if (!previousSelection.equals(currentSelection)) {
                        settings.setBleachCorrection(currentSelection);
                        // Update previous selection to current if successful
                        previousSelection = currentSelection;
                    }
                } catch (RejectResetException e) {
                    comboBox.setSelectedItem(previousSelection);
                    return;
                }

                fitController.updateFitEnd(settings);

                if (currentSelection == BleachCorrectionMethod.SLIDING_WINDOW ||
                        currentSelection == BleachCorrectionMethod.LINEAR_SEGMENT) {
                    new SlidingWindowSelectionView(onBleachCorrectionSlidingWindowAccepted,
                            settings.getSlidingWindowLength());
                } else if (currentSelection == BleachCorrectionMethod.POLYNOMIAL) {
                    new PolynomialOrderSelectionView(onBleachCorrectionOrderAccepted,
                            bleachCorrectionModel.getPolynomialOrder());
                }
            }
        };
    }

    /**
     * Callback method for accepting sliding window length.
     * It validates the input and sets it in the model or prompts re-entry if the input is invalid.
     *
     * @param slidingWindowLength The length of the sliding window chosen by the user.
     */
    private void onBleachCorrectionSlidingWindowAccepted(int slidingWindowLength) {
        int maxWindowSize = settings.getLastFrame() - settings.getFirstFrame();

        if (slidingWindowLength <= 0 || slidingWindowLength > maxWindowSize) {
            IJ.showMessage(
                    String.format("Invalid sliding window size. It must be inside 0 < order < %d", maxWindowSize));
            new SlidingWindowSelectionView(this::onBleachCorrectionSlidingWindowAccepted,
                    settings.getSlidingWindowLength());
        } else {
            settings.setSlidingWindowLength(slidingWindowLength);
            fitController.updateFitEnd(settings);
        }
    }

    /**
     * Callback method for accepting polynomial order.
     * It validates the polynomial order and sets it in the model or prompts re-entry if the input is invalid.
     *
     * @param order The polynomial order chosen by the user.
     */
    private void onBleachCorrectionOrderAccepted(int order) {
        if (order <= 0 || order > BleachCorrectionModel.MAX_POLYNOMIAL_ORDER) {
            IJ.showMessage(String.format("Invalid polynomial order. It must be inside 0 < order <= %d",
                    BleachCorrectionModel.MAX_POLYNOMIAL_ORDER));
            new PolynomialOrderSelectionView(this::onBleachCorrectionOrderAccepted,
                    bleachCorrectionModel.getPolynomialOrder());
        } else {
            bleachCorrectionModel.setPolynomialOrder(order);
        }
    }

    /**
     * Returns an action listener for changes in the filter selection combo box.
     * This method creates a listener that updates the filter settings and may
     * open a dialog for setting filter limits based on the selected filter mode.
     *
     * @return an ActionListener that processes filter selection changes
     */
    public ActionListener cbFilterChanged(JComboBox<FilterMode> comboBox) {
        BiConsumer<Integer, Integer> listener = this::onFilterSelectionAccepted;

        return new ActionListener() {
            private FilterMode previousSelection = (FilterMode) comboBox.getSelectedItem();

            @Override
            public void actionPerformed(ActionEvent ev) {
                FilterMode filterMode = (FilterMode) comboBox.getSelectedItem();

                try {
                    if (!previousSelection.equals(filterMode)) {
                        settings.setFilter(filterMode);
                        // Update previous selection to current if successful
                        previousSelection = filterMode;
                    }
                } catch (RejectResetException e) {
                    comboBox.setSelectedItem(previousSelection);
                    return;
                }

                // If a filter mode other than "none" is selected, show the filter limits dialog.
                if (filterMode != FilterMode.NO_FILTER) {
                    new FilterLimitsSelectionView(listener, settings.getFilterLowerLimit(),
                            settings.getFilterUpperLimit());
                }

                imageController.setFilterArray();
            }
        };
    }

    /**
     * Callback method for accepting filter limits.
     * It validates the filter limits and sets them in the model or prompts re-entry if the input is invalid.
     *
     * @param lowerLimit The lower limit of the filter chosen by the user.
     * @param upperLimit The upper limit of the filter chosen by the user.
     */
    private void onFilterSelectionAccepted(int lowerLimit, int upperLimit) {
        if (lowerLimit < 0 || lowerLimit > upperLimit) {
            IJ.showMessage("Illegal filter limits");
            new FilterLimitsSelectionView(this::onFilterSelectionAccepted, settings.getFilterLowerLimit(),
                    settings.getFilterUpperLimit());
        } else {
            settings.setFilterLowerLimit(lowerLimit);
            settings.setFilterUpperLimit(upperLimit);
        }
    }

    /**
     * Returns an action listener to handle the exit button press event.
     * This listener disposes of views.
     *
     * @return an ActionListener that processes the exit button press event
     */
    public ActionListener btnExitPressed() {
        return (ActionEvent ev) -> {
            view.dispose();
            expSettingsView.dispose();
            filteringController.dispose();
            bleachCorrectionView.dispose();
            simulationController.dispose();
            fitController.dispose();
            backgroundSubtractionController.dispose();
            diffusionLawController.dispose();

            imageController.unloadImage();

            Plots.closePlots();
        };
    }

    /**
     * Returns an ActionListener for the "More" button to toggle the extended panel.
     *
     * @return An ActionListener that toggles the extended panel in the view.
     */
    public ActionListener btnMorePressed() {
        return (ActionEvent ev) -> view.toggleExtendedPanel();
    }

    /**
     * Returns an {@link ActionListener} that brings all key application windows to the front.
     *
     * @return an {@link ActionListener} to bring all windows to the front.
     */
    public ActionListener btnBringToFrontPressed() {
        return (ActionEvent ev) -> {
            bleachCorrectionView.toFront();
            filteringController.toFront();
            simulationController.toFront();
            backgroundSubtractionController.toFront();
            imageController.toFront();
            diffusionLawController.toFront();
            fitController.toFront();
            Plots.toFront();
            expSettingsView.toFront();
            view.toFront();
        };
    }

    /**
     * Returns an {@link ActionListener} that cancels the current correlate ROI method if it's running.
     *
     * @return an {@link ActionListener} to cancel the current correlation.
     */
    public ActionListener btnCancelCorrelationPressed() {
        return (ActionEvent ev) -> CorrelationWorker.cancelPreviousInstance();
    }

    /**
     * Returns an action listener to handle the exit button press event.
     * This listener disposes of the main panel view.
     *
     * @return an ActionListener that processes the exit button press event
     */
    public ActionListener btnUseExistingPressed() {
        return (ActionEvent ev) -> {
            if (WindowManager.getImageCount() > 0) {
                try {
                    imageController.loadImage(IJ.getImage(), null);
                    fitController.resetFilters();
                    filteringController.refreshFilteringView();
                } catch (RuntimeException e) {
                    IJ.showMessage("Wrong image format", e.getMessage());
                }
            } else {
                IJ.showMessage("No image open.");
            }
        };
    }

    /**
     * Returns an action listener to handle the exit button press event.
     * This listener disposes of the main panel view.
     *
     * @return an ActionListener that processes the exit button press event
     */
    public ActionListener btnLoadNewPressed() {
        return (ActionEvent ev) -> {
            ImagePlus image = ImageLoader.openImagePlusWithDialog();
            if (image != null) {
                try {
                    imageController.loadImage(image, null);
                    fitController.resetFilters();
                    filteringController.refreshFilteringView();
                } catch (RuntimeException e) {
                    IJ.showMessage("Wrong image format", e.getMessage());
                }
            }
        };
    }

    /**
     * Exports all analysis results and settings to an Excel file.
     * Saves the current settings, polynomial order, image data, pixel models, diffusion law data,
     * N&B data, and DCCF sheets into the specified Excel file.
     *
     * @param filePath the path where the Excel file will be saved
     */
    private void exportAllToExcel(String filePath) {
        Map<String, Object> settingsMap = settings.toMap();
        settingsMap.put("Polynomial Order", bleachCorrectionModel.getPolynomialOrder());
        settingsMap.putAll(imageController.toMap());

        ExcelExporter.saveExcelFile(filePath, settingsMap, (workbook) -> {
            ExcelExporter.saveExcelPixelModels(workbook, correlator.getPixelModels(), settings, correlator);
            diffusionLawController.saveExcelSheets(workbook);
            nbController.saveExcelSheet(workbook);
            ExcelExporter.savedCCFSheets(workbook, correlator.getDccf());
        });
    }

    /**
     * Creates an ActionListener for the save button, allowing the user to select a file path and save parameters and
     * results to an Excel file.
     *
     * @return an ActionListener that handles the save button press event
     */
    public ActionListener btnSavePressed() {
        return (ActionEvent ev) -> {
            if (!imageController.isImageLoaded()) {
                IJ.showMessage("No image open.");
                return;
            }

            String filePath = ExcelExporter.selectExcelFileToSave(imageController.getFileName() + ".xlsx",
                    imageController.getDirectory());
            if (filePath == null) {
                return;
            }

            new BackgroundTaskWorker<Void, Void>(() -> exportAllToExcel(filePath)).execute();
        };
    }

    /**
     * Creates an ActionListener for the "Load" button press event.
     * This method handles the event when the "Load" button is pressed, allowing the user to select an Excel file
     * to load. If a file is selected, the current view is disposed of, and a new MainPanelController is initialized
     * with the selected workbook.
     *
     * @return an ActionListener that handles the "Load" button press event
     */
    public ActionListener btnLoadPressed() {
        return (ActionEvent ev) -> {
            Workbook workbook = null;
            try {
                workbook = ExcelReader.selectExcelFileToLoad(
                        imageController.isImageLoaded() ? imageController.getDirectory() : "");
            } catch (Exception e) {
                IJ.showMessage(e.getMessage());
            }

            // In this case, the user didn't select a file, we can just leave the method
            if (workbook == null) {
                return;
            }

            this.view.dispose();
            btnExitPressed().actionPerformed(ev);

            new MainPanelController(this.optionsModel, workbook);
        };
    }

    /**
     * Creates an ActionListener for the "Batch" button.
     * When triggered, it opens the BatchView to configure and execute batch processing of images.
     *
     * @return an ActionListener that handles the "Batch" button press event
     */
    public ActionListener btnBatchPressed() {
        return (ActionEvent ev) -> new BatchView(this::runBatch);
    }

    /**
     * Executes batch processing of images based on user-selected options.
     * Processes multiple images selected by the user, performing operations such as correlation, PSF calculation,
     * diffusion law analysis, DCCF computation, saving results, and plotting, depending on the options specified in
     * the run parameter.
     *
     * @param run a Map containing options for batch processing, specifying which operations to perform on each image
     */
    private void runBatch(Map<String, Object> run) {
        // Early cast to boolean, as this value will be reused.
        boolean doOnnxInference = (boolean) run.get("ONNX Inference");
        
        // If ONNX Inference is requested, check to see if model is loaded.
        if (doOnnxInference) {
            if (!onnxInferenceController.canDoInference()) {
                IJ.error("ONNX Inference was requested, but ONNX model is not loaded.");
                return;
            }
        }

        fitController.setVisible((boolean) run.get("Fit"));

        JFileChooser fileChooser = new JFileChooser(imageController.getDirectory());
        fileChooser.setMultiSelectionEnabled(true);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fileChooser.showOpenDialog(null);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File[] files = fileChooser.getSelectedFiles();

            new BackgroundTaskWorker<Void, Void>(() -> {
                for (File file : files) {
                    // Initializing dummy value for ONNX Inference Output maps.
                    // Mainly to silence compiler errors.
                    Map<String, ImagePlus> outputMaps = null; 

                    try {
                        imageController.loadImage(IJ.openImage(file.getAbsolutePath()), null);
                    } catch (Exception e) {
                        IJ.log(String.format("The file %s is not an image file. Skipping it.", file.getAbsolutePath()));
                        continue;
                    }

                    if ((boolean) run.get("Correlate All")) {
                        // Perform the correlation synchronously
                        Range[] ranges = settings.getAllArea(imageController.getImageDimension());
                        Range xRange = ranges[0];
                        Range yRange = ranges[1];

                        Roi imgRoi = new Roi(xRange.getStart(), yRange.getStart(), xRange.getEnd(), yRange.getEnd());

                        imageController.correlateROI(imgRoi);
                    }

                    if ((boolean) run.get("PSF Calculation")) {
                        Dimension imageDimension = imageController.getImageDimension();
                        if (imageDimension.width < 20 || imageDimension.height < 20) {
                            IJ.log("Image is too small to provide good PSF statistics. At least 20x20 pixels are " +
                                    "required");
                        } else {
                            // Every condition is validated (image loaded and size >= 20), so we can run the PSF.
                            diffusionLawController.displayPSFDialog();
                        }
                    }

                    if ((boolean) run.get("Diffusion Law")) {
                        diffusionLawController.runCalculate();
                        diffusionLawController.btnFitPressed().actionPerformed(null);
                    }

                    if (doOnnxInference) {
                        // Produce output maps for Onnx inference, used for saving later on.
                        outputMaps = onnxInferenceController.infer();
                    }

                    if ((boolean) run.get("Vertical DCCF")) {
                        new DeltaCCFWorker(settings, correlator, imageController.getImage(), DccfDirection.X_DIRECTION,
                                (dccfArray, direction) -> {
                                    IJ.showStatus("Done");
                                    Plots.plotDCCFWindow(dccfArray, direction.getDisplayName());
                                }).executeAndWait();
                    }

                    if ((boolean) run.get("Horizontal DCCF")) {
                        new DeltaCCFWorker(settings, correlator, imageController.getImage(), DccfDirection.Y_DIRECTION,
                                (dccfArray, direction) -> {
                                    IJ.showStatus("Done");
                                    Plots.plotDCCFWindow(dccfArray, direction.getDisplayName());
                                }).executeAndWait();
                    }

                    if ((boolean) run.get("Diagonal Up DCCF")) {
                        new DeltaCCFWorker(settings, correlator, imageController.getImage(),
                                DccfDirection.DIAGONAL_UP_DIRECTION, (dccfArray, direction) -> {
                            IJ.showStatus("Done");
                            Plots.plotDCCFWindow(dccfArray, direction.getDisplayName());
                        }).executeAndWait();
                    }

                    if ((boolean) run.get("Diagonal Down DCCF")) {
                        new DeltaCCFWorker(settings, correlator, imageController.getImage(),
                                DccfDirection.DIAGONAL_DOWN_DIRECTION, (dccfArray, direction) -> {
                            IJ.showStatus("Done");
                            Plots.plotDCCFWindow(dccfArray, direction.getDisplayName());
                        }).executeAndWait();
                    }

                    // Get the suffix
                    String suffix = run.get("File suffix").toString();
                    if (suffix.isEmpty()) {
                        suffix = new SimpleDateFormat("yyyy_MM_dd-HH_mm_ss").format(new Date());
                    }

                    // Get the absolute path without the extension
                    String absolutePathNoExt = file.getAbsolutePath().replaceFirst("[.][^.]+$", "");

                    if ((boolean) run.get("Save excel")) {
                        exportAllToExcel(absolutePathNoExt + suffix + ".xlsx");
                    }

                    if ((boolean) run.get("Save plot windows")) {
                        Plots.saveWindows(absolutePathNoExt + suffix);
                    }
                    
                    // NOTE: ONNX inference relies on a slightly different save format
                    // Hence the separated logic.
                    if (doOnnxInference && outputMaps != null) {
                        OnnxInferenceController.saveOnnxOutputMaps(outputMaps, absolutePathNoExt + suffix);
                    } else if (doOnnxInference && outputMaps == null) {
                        IJ.log("Warning: ONNX Inference was run for " + file.getName() + " but resulted in null output maps. Skipping save.");
                    }

                    Plots.closePlots();
                    imageController.unloadImage();
                    fitController.btnResetParametersPressed().actionPerformed(null);
                }
            }).execute();
        }
    }

    /**
     * Creates an ActionListener for the "PSF" button that initiates the Point Spread Function (PSF) calculation
     * process.
     * This listener checks whether an image is loaded and validates its dimensions. If the image is loaded and
     * its dimensions are sufficient (at least 20x20 pixels), the PSF dialog is displayed to configure and start
     * the PSF calculation. If the image is not loaded or its dimensions are too small, an appropriate message is shown.
     *
     * @return An ActionListener that handles the PSF button press event.
     */
    public ActionListener btnPSFPressed() {
        return (ActionEvent ev) -> {
            if (imageController.isImageLoaded()) {
                Dimension imageDimension = imageController.getImageDimension();
                if (imageDimension.width < 20 || imageDimension.height < 20) {
                    IJ.showMessage("Image is too small to provide good PSF statistics. At least 20x20 pixels are " +
                            "required");
                } else {
                    // Every condition is validated (image loaded and size >= 20), so we can run the PSF.
                    diffusionLawController.displayPSFDialog();
                }
            } else {
                IJ.showMessage("No image stack loaded.");
            }
        };
    }

    /**
     * Creates an {@link ItemListener} for the "Diffusion Law" toggle button.
     * If an image stack is loaded, it toggles the visibility of the diffusion law view.
     * If no image is loaded and the button is selected, it shows a warning and deselects the button.
     *
     * @return An {@link ItemListener} that manages the diffusion law view visibility and warns if no image is loaded.
     */
    public ItemListener tbDiffusionLawPressed() {
        return (ItemEvent ev) -> {
            if (imageController.isImageLoaded()) {
                diffusionLawController.setVisible(ev.getStateChange() == ItemEvent.SELECTED);
            } else if (ev.getStateChange() == ItemEvent.SELECTED) {
                // Don't show the message if the button is unselected.
                IJ.showMessage("No image stack loaded.");
                diffusionLawController.setVisible(false);
                ((JToggleButton) ev.getSource()).setSelected(false);
            }
        };
    }

    /**
     * Returns an action listener to handle the "Options" button press event.
     * This listener opens the options controller.
     *
     * @return an ActionListener that processes the "Options" button press event
     */
    public ActionListener btnOptionsPressed() {
        return (ActionEvent ev) -> new OptionsController(optionsModel);
    }

    /**
     * Creates an ActionListener to handle the NB analysis button press.
     *
     * @return the ActionListener for the NB analysis button
     */
    public ActionListener btnNBPressed() {
        return (ActionEvent ev) -> {
            nbController.btnNBPressed().actionPerformed(ev);
        };
    }

    public ItemListener tbFilteringPressed() {
        return (ItemEvent ev) -> filteringController.setVisible(ev.getStateChange() == ItemEvent.SELECTED);
    }

    /**
     * Creates an ActionListener for the "Average" button.
     * This listener handles the calculation and plotting of the average correlation function
     * for the selected region of interest (ROI) in the image.
     *
     * @return The ActionListener instance.
     */
    public ActionListener btnAveragePressed() {
        return (ActionEvent ev) -> {
            if (correlator.getPixelModels() == null) {
                IJ.showMessage("Nothing to plot, please run the correlation on at least one pixel before");
            } else {
                // if the ROI is null, we consider all correlated pixels.
                Roi roi = null;
                if (Plots.imgParam != null) {
                    roi = Plots.imgParam.getRoi();
                }

                try {
                    PixelModel averagePixelModel =
                            AverageCorrelation.calculateAverageCorrelationFunction(correlator.getPixelModels(), roi,
                                    settings::convertPointToBinning, settings.getPixelBinning(),
                                    settings.getMinCursorPosition(), fitController);
                    fitController.fit(averagePixelModel, settings.getFitModel(), correlator.getLagTimes());

                    if (optionsModel.isPlotACFCurves()) {
                        Plots.plotCorrelationFunction(Collections.singletonList(averagePixelModel),
                                settings.isFCCSDisp(), correlator.getLagTimes(), null, settings.getBinning(),
                                settings.getCCF(), fitController.getFitStart(), fitController.getFitEnd());
                    }

                    if (settings.isMSD()) {
                        averagePixelModel.setMSD(
                                MeanSquareDisplacement.correlationToMSD(averagePixelModel.getCorrelationFunction(),
                                        settings.getParamAx(), settings.getParamAy(), settings.getParamW(),
                                        settings.getSigmaZ(), settings.isMSD3d()));
                        Plots.plotMSD(Collections.singletonList(averagePixelModel), correlator.getLagTimes(), null,
                                settings.getBinning(), settings.isFCCSDisp());
                    }
                } catch (RuntimeException e) {
                    // This happen if the ROI selected doesn't contain any correlated pixel
                    IJ.showMessage(e.getMessage());
                }
            }
        };
    }

    /**
     * Creates an ActionListener for the button press event that generates a scatter plot.
     *
     * @return An ActionListener that handles the button press event.
     */
    public ActionListener btnParaCorPressed() {
        return (ActionEvent ev) -> {
            if (!imageController.isImageLoaded()) {
                IJ.showMessage("No image open.");
            } else if (correlator.getPixelModels() == null) {
                IJ.showMessage("Nothing to plot, please run the fit on at least one pixel before.");
            } else {
                Pair<double[][], String[]> scatterArrayAndLabels =
                        PixelModel.getScatterPlotArray(correlator.getPixelModels(), settings.getParaCor());
                double[][] scPlot = scatterArrayAndLabels.getLeft();
                String[] labels = scatterArrayAndLabels.getRight();

                Plots.scatterPlot(scPlot, labels[1], labels[0]);
            }
        };
    }

    /**
     * Creates an ActionListener that handles the DCCF button press event.
     *
     * @return An ActionListener that initiates the DCCF computation when the button is pressed.
     */
    public ActionListener btnDCCFPressed() {
        return (ActionEvent ev) -> {
            if (!imageController.isImageLoaded()) {
                IJ.showMessage("No image open.");
            } else {
                DccfDirection direction = settings.getdCCF();
                IJ.showStatus("Correlating all pixels");
                DeltaCCFWorker dccfWorker =
                        new DeltaCCFWorker(settings, correlator, imageController.getImage(), direction,
                                (dccfArray, d) -> {
                                    IJ.showStatus("Done");
                                    Plots.plotDCCFWindow(dccfArray, d.getDisplayName());
                                });
                dccfWorker.execute();
            }
        };
    }

    public ActionListener btnResultTablePressed() {
        // FIXME
        return null;
    }

    /**
     * Returns an ActionListener that writes the current configuration to a YAML file.
     * The configuration includes settings and options which are serialized and saved to the file path
     * specified by getConfigPath. If an error occurs during writing, a message is displayed.
     *
     * @return an ActionListener that writes the configuration to a file when triggered.
     */
    public ActionListener btnWriteConfigPressed() {
        return (ActionEvent ev) -> {
            Map<String, Object> data = new HashMap<>();
            data.put("Settings", settings.toMapConfig());
            data.put("Options", optionsModel.toMap());

            DumperOptions optionsYaml = new DumperOptions();
            optionsYaml.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
            optionsYaml.setPrettyFlow(true);

            Yaml yaml = new Yaml(optionsYaml);
            try (FileWriter writer = new FileWriter(getConfigPath())) {
                yaml.dump(data, writer);
            } catch (IOException e) {
                IJ.showMessage("Error", "Can't write configuration.");
            }
        };
    }

    /**
     * Creates an ActionListener for the button to perform direct camera readout.
     * This method returns an ActionListener that, when triggered, checks the operating system.
     * If the OS is Windows, it proceeds with the camera readout operation (to be implemented).
     * If the OS is not Windows, it shows a message indicating that direct capture is only supported on Windows.
     *
     * @return an ActionListener that performs OS-specific camera readout operations
     */
    public ActionListener btnDirectCameraReadoutPressed() {
        return (ActionEvent ev) -> {
            if (CheckOS.getCurrentOS() == CheckOS.OperatingSystem.WINDOWS) {
                DirectCapture directCapture = new DirectCapture(optionsModel.isUseGpu());
                directCapture.check();
            } else {
                IJ.showMessage("Direct capture is only supported on Windows.");
            }
        };
    }

    /**
     * Creates an ActionListener for handling the "Parameter Video" button press.
     * When triggered, it checks if an image is loaded. If not, it displays an error message.
     * If an image is loaded, it opens the parameter video configuration dialog.
     *
     * @return an ActionListener that handles the button press event
     */
    public ActionListener btnParamVideoPressed() {
        return (ActionEvent ev) -> {
            if (!imageController.isImageLoaded()) {
                IJ.showMessage("No image open.");
            } else {
                parameterVideoController.showParameterVideoView(settings.getFirstFrame(), settings.getLastFrame());
            }
        };
    }

    /**
     * Creates an ActionListener that handles the event when the ROI button is pressed.
     * It checks if an image is loaded and if an ROI is selected. If a CCF distance is set, it creates
     * an additional ROI for correlation, validates it, and performs correlation.
     *
     * @return An ActionListener that processes the ROI selection and performs correlation.
     */
    public ActionListener btnROIPressed() {
        return (ActionEvent ev) -> {
            if (!imageController.isImageLoaded()) {
                IJ.showMessage("No image open.");
                return;
            }

            Roi imgRoi = imageController.getImage().getRoi();

            if (imgRoi == null) {
                IJ.showMessage("No ROI chosen.");
            } else {
                // create another ROI if the distance is not null
                if (settings.getCCF().width != 0 || settings.getCCF().height != 0) {
                    Roi CCFRoi = (Roi) imgRoi.clone();
                    CCFRoi.setLocation(imgRoi.getBounds().getX() + settings.getCCF().width,
                            imgRoi.getBounds().getY() + settings.getCCF().height);
                    CCFRoi.setStrokeColor(Color.RED);
                    if (!imageController.isROIValid(CCFRoi)) {
                        IJ.showMessage("Correlation points are not within image.");
                        return;
                    }
                    if (imageController.isROIOverlapInDCFCCS(CCFRoi)) {
                        IJ.showMessage("Cross-correlation areas overlap.");
                        return;
                    }
                    imageController.getImage().setOverlay(new Overlay(CCFRoi));
                }

                // Perform ROI
                IJ.showStatus("Correlating pixels");
                new CorrelationWorker(imageController, imgRoi).execute();
            }
        };
    }

    /**
     * Creates an ActionListener that handles the event when the "All" button is pressed.
     * It checks if an image is loaded and then sets the ROI to cover the entire image,
     * adjusted for CCF and binning settings, before performing correlation.
     *
     * @return An ActionListener that sets the ROI and performs correlation.
     */
    public ActionListener btnAllPressed() {
        return (ActionEvent ev) -> {
            if (!imageController.isImageLoaded()) {
                IJ.showMessage("No image open.");
                return;
            }

            Overlay overlay = imageController.getImage().getOverlay();
            if (overlay != null) {
                overlay.clear();
            }

            Range[] ranges = settings.getAllArea(imageController.getImageDimension());
            Range xRange = ranges[0];
            Range yRange = ranges[1];

            Roi imgRoi = new Roi(xRange.getStart(), yRange.getStart(), xRange.getEnd(), yRange.getEnd());
            imageController.getImage().setRoi(imgRoi);

            btnROIPressed().actionPerformed(ev);
        };
    }

    /**
     * Returns an item listener to handle the "Exp Settings" toggle button press event.
     * This listener toggles the visibility of the experimental settings view.
     *
     * @return an ItemListener that processes the "Exp Settings" toggle button press event
     */
    public ItemListener tbExpSettingsPressed() {
        return (ItemEvent ev) -> expSettingsView.setVisible(ev.getStateChange() == ItemEvent.SELECTED);
    }

    /**
     * Returns an item listener to handle the "Bleach Correction" toggle button press event.
     * This listener toggles the visibility of the bleach correction view, ensuring an image is loaded first.
     *
     * @return an ItemListener that processes the "Bleach Correction" toggle button press event
     */
    public ItemListener tbBleachCorStridePressed() {
        return (ItemEvent ev) -> {
            if (imageController.isImageLoaded()) {
                bleachCorrectionView.setVisible(ev.getStateChange() == ItemEvent.SELECTED);
            } else if (ev.getStateChange() == ItemEvent.SELECTED) {
                // Don't show the message if the button is unselected.
                IJ.showMessage("No image stack loaded.");
                ((JToggleButton) ev.getSource()).setSelected(false);
            }
        };
    }

    /**
     * Returns an item listener to handle the "Fit" toggle button press event.
     * This listener toggles the visibility of the FitController.
     *
     * @return an ItemListener that processes the "Fit" toggle button press event
     */
    public ItemListener tbFitPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setText(selected ? "Fit On" : "Fit Off");
            fitController.setVisible(selected);
        };
    }

    /**
     * Returns an item listener to handle the "Sim" toggle button press event.
     * This listener toggles the visibility of the SimulationController.
     *
     * @return an ItemListener that processes the "Sim" toggle button press event
     */
    public ItemListener tbSimPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            button.setText(selected ? "Sim On" : "Sim Off");
            simulationController.setVisible(selected);
        };
    }

    /**
     * Creates an ItemListener for the MSD toggle button.
     * When the button is pressed, it toggles the MSD (Mean Squared Displacement) analysis on or off
     * and updates the button text accordingly. If MSD is enabled, it opens an MSDView dialog to
     * configure the MSD settings.
     *
     * @return an ItemListener for the MSD toggle button.
     */
    public ItemListener tbMSDPressed() {
        return (ItemEvent ev) -> {
            JToggleButton button = (JToggleButton) ev.getItemSelectable();

            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);

            if (selected) {
                settings.setMSD(true);
                button.setText("MSD On");
                SwingUtilities.invokeLater(() -> new MSDView(settings.isMSD3d(), settings::setMSD3d));
            } else {
                settings.setMSD(false);
                button.setText("MSD Off");
            }
        };
    }

    /**
     * Returns an item listener to handle the "Overlap" toggle button press event.
     * This listener toggles the overlap setting in the experimental settings model.
     *
     * @return an ItemListener that processes the "Overlap" toggle button press event
     */
    public ItemListener tbOverlapPressed() {
        return new ItemListener() {
            private boolean ignoreEvent = false;

            @Override
            public void itemStateChanged(ItemEvent ev) {
                // If we're updating the button programmatically, ignore the event.
                if (ignoreEvent) {
                    return;
                }

                JToggleButton button = (JToggleButton) ev.getItemSelectable();
                boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);

                try {
                    // Check if we need to reset results due to parameter changes
                    askResetResults();
                } catch (RejectResetException e) {
                    // ignore the event for the next modification
                    ignoreEvent = true;
                    // revert to previous stage
                    button.setSelected(!selected);
                    ignoreEvent = false;
                    return;
                }

                button.setText(selected ? "Overlap On" : "Overlap Off");
                settings.setOverlap(selected);
            }
        };
    }

    /**
     * Returns an item listener to handle the "Background" toggle button press event.
     * This listener toggles the visibility of the BackgroundSubtractionController.
     *
     * @return an ItemListener that processes the "Background" toggle button press event
     */
    public ItemListener tbBackgroundPressed() {
        return (ItemEvent ev) -> {
            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            backgroundSubtractionController.setVisible(selected);
        };
    }

    /**
     * Updates the display fields in the experimental settings view based on the current values in the model.
     * This method ensures that the UI reflects the most up-to-date settings.
     */
    private void updateSettingsField() {
        Runnable doUpdateSettingsField = () -> {
            settings.updateSettings();
            expSettingsView.setNonUserSettings();
            expSettingsView.setPSFZEnabled();
            filteringController.enableButtonSameAsCCF(settings.isFCCSDisp());
            filteringController.refreshFilteringView();
        };

        // Execute the update in the Swing event dispatch thread to ensure thread safety
        SwingUtilities.invokeLater(doUpdateSettingsField);
    }

    /**
     * Decorates a setter from the model to automatically update the settings view
     * after the model value has been changed.
     *
     * @param setter A Consumer that sets a value in the model.
     * @return A FocusListener that updates the model and then the view when focus is lost.
     */
    public FocusListener updateSettings(Consumer<String> setter) {
        // decorate the setter to call updateSettingsfield after changing the value
        Consumer<String> decoratedSetter = (String value) -> {
            setter.accept(value);
            updateSettingsField();
        };

        // Create the focus listener with the method from the factory with the decorated setter
        return createFocusListener(decoratedSetter);
    }

    /**
     * Updates the stride parameter fields based on the current model values.
     * This method ensures that the stride parameter fields are accurately set
     * based on the number of frames and the average stride.
     */
    private void updateStrideParamFields() {
        Runnable doUpdateStrideParam = () -> {
            int numberOfFrames = settings.getLastFrame() - settings.getFirstFrame() + 1;
            bleachCorrectionModel.computeNumPointsIntensityTrace(numberOfFrames);
            bleachCorrectionView.updateNumPointsIntensityTrace();
        };

        SwingUtilities.invokeLater(doUpdateStrideParam);
    }

    /**
     * Decorates a setter from the model to automatically update the stride parameter fields
     * after the model value has been changed.
     *
     * @param setter A Consumer that sets a value in the model.
     * @return A FocusListener that updates the model and then the view when focus is lost.
     */
    public FocusListener updateStrideParam(Consumer<String> setter) {
        Consumer<String> decoratedSetter = (String value) -> {
            setter.accept(value);
            updateStrideParamFields();

            // recompute backgrounds if first frame / last frame changed
            imageController.computeBackground();
            backgroundSubtractionController.setTfBackgrounds();
        };

        return createFocusListener(decoratedSetter);
    }

    /**
     * Returns a Consumer that decorates the given setter to also update the fit end.
     *
     * @param setter the original setter Consumer
     * @return a decorated Consumer that updates the fit end and calls the given setter
     */
    public Consumer<String> updateFitEnd(Consumer<String> setter) {
        return (String value) -> {
            setter.accept(value);
            fitController.updateFitEnd(settings);
        };
    }

    // ONNX-related controller options.
    /**
     * Returns an item listener to handle the "ONNX Models" toggle button press event.
     * This listener toggles the visibility of the ONNX Models view.
     *
     * @return an ItemListener that processes the "ONNX Models Infernece" toggle button press event
     */
    public ItemListener tbOnnxInferencePressed() {
        return (ItemEvent ev) -> {
            boolean selected = (ev.getStateChange() == ItemEvent.SELECTED);
            onnxInferenceController.setVisible(selected);
        };
    }
}
