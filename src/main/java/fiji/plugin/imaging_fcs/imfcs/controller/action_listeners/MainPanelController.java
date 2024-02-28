package fiji.plugin.imaging_fcs.imfcs.controller.action_listeners;

import fiji.plugin.imaging_fcs.imfcs.Imaging_FCS;
import fiji.plugin.imaging_fcs.imfcs.TimerFit;
import ij.IJ;
import ij.WindowManager;
import ij.gui.GenericDialog;
import ij.gui.Overlay;
import ij.gui.Roi;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.io.File;

public class MainPanelController {
    private JFrame frame;

    public MainPanelController(JFrame frame) {
        this.frame = frame;
    }

    public ActionListener btnExitPressed = (ActionEvent ev) -> {
        exitImFCS();
    };

    public ActionListener btnBtfPressed = (ActionEvent ev) -> {
        bringToFront();
    };

    public ActionListener btnUseExistingPressed = (ActionEvent ev) -> {
        if (WindowManager.getImageCount() > 0) {
            closeWindows();
            imp = IJ.getImage();
            obtainImage();
        } else {
            IJ.showMessage("No image open.");
        }
    };

    public ActionListener btnLoadNewPressed = (ActionEvent ev) -> {
        imp = IJ.openImage();
        if (imp != null) {
            imp.show();
            obtainImage();
            closeWindows();
        }
    };

    public ActionListener btnSavePressed = (ActionEvent ev) -> {
        if (setImp) {
            String xlsxFN = $impTitle;
            int dotind = xlsxFN.lastIndexOf('.');
            if (dotind != -1) {
                xlsxFN = xlsxFN.substring(0, dotind);
            }
            xlsxFN += ".xlsx";
            JFileChooser fc = new JFileChooser($imagePath);
            fc.setSelectedFile(new File(xlsxFN));
            fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
            int returnVal = fc.showSaveDialog(btnSave);
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                writeExperiment(fc.getSelectedFile(), "Failed to write data files", true);
            }
        } else {
            JOptionPane.showMessageDialog(null, "No Image loaded or assigned.");
        }
    };

    public ActionListener btnLoadPressed = (ActionEvent ev) -> {

        // Locate excel file while automatically loads accompanied image stacks
        JFileChooser fc = new JFileChooser($imagePath);
        fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
        int returnVal = fc.showOpenDialog(btnLoad);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            readExperiment(fc.getSelectedFile(), "Failed to read data files");
        }
        // if (setImp) {
        // }
        // } else {
        // JOptionPane.showMessageDialog(null, "No Image loaded or assigned.");
        // }
    };

    public ActionListener btnBatchPressed = (ActionEvent ev) -> {
        setImp = true;
        if (lastframe < firstframe) {
            firstframe = 1;
            lastframe = 2;
            tfFirstFrame.setText(Integer.toString(firstframe));
            tfLastFrame.setText(Integer.toString(lastframe));
        }
        Imaging_FCS.batchWorker batchInstant = new Imaging_FCS.batchWorker();
        batchInstant.execute();
    };

    public ActionListener btnPSFPressed = (ActionEvent ev) -> {
        if (setParameters()) {
            Imaging_FCS.correlatePSFWorker correlatePSFInstant = new Imaging_FCS.correlatePSFWorker();
            correlatePSFInstant.execute();
        }
    };

    public ItemListener tbDLPressed = (ItemEvent ev) -> {
        if (ev.getStateChange() == ev.SELECTED) {
            if (setParameters()) {
                setDLparameters();
                tbDL.setSelected(true);
                difflawframe.setVisible(true);
            }
        } else {
            difflawframe.setVisible(false);
        }
    };

    public ActionListener btnOptionsPressed = (ActionEvent ev) -> {
        GenericDialog gd = new GenericDialog("Options");
        gd.addCheckbox("ACF", plotACFCurves);
        gd.addCheckbox("SD", plotSDCurves);
        gd.addCheckbox("Intensity", plotIntensityCurves);
        gd.addCheckbox("Residuals", plotResCurves);
        gd.addCheckbox("Histogram", plotParaHist);
        gd.addCheckbox("Blocking", plotBlockingCurve);
        gd.addCheckbox("Covariance Matrix", plotCovmats);
        gd.addCheckbox("GPU", useGpu);
        gd.hideCancelButton();
        gd.showDialog();
        if (gd.wasOKed()) {
            plotACFCurves = gd.getNextBoolean();
            plotSDCurves = gd.getNextBoolean();
            plotIntensityCurves = gd.getNextBoolean();
            plotResCurves = gd.getNextBoolean();
            plotParaHist = gd.getNextBoolean();
            plotBlockingCurve = gd.getNextBoolean();
            plotCovmats = gd.getNextBoolean();
            boolean gpu = gd.getNextBoolean();
            if (!plotACFCurves && (plotSDCurves || plotResCurves)) {
                plotSDCurves = false;
                plotResCurves = false;
                IJ.showMessage("Plotting of SD and/or Residuals without the ACF is not supported.");
            }
            if (!plotACFCurves) {
                if (acfWindow != null && !acfWindow.isClosed()) { // close ACF window
                    acfWindow.close();
                }
            }
            if (!plotSDCurves) {
                if (sdWindow != null && !sdWindow.isClosed()) { // close SD window
                    sdWindow.close();
                }
            }
            if (!plotIntensityCurves) {
                if (intWindow != null && !intWindow.isClosed()) { // close intensity trace window
                    intWindow.close();
                }
            }
            if (!plotResCurves) {
                if (resWindow != null && !resWindow.isClosed()) { // close fit residuals window
                    resWindow.close();
                }
            }
            if (!plotParaHist) {
                if (histWin != null && !histWin.isClosed()) {
                    histWin.close();
                }
            }
            if (!plotBlockingCurve) {
                if (blockingWindow != null && !blockingWindow.isClosed()) { // close blocking window
                    blockingWindow.close();
                }
            }
            if (!plotCovmats) {
                if (impCovWin != null && !impCovWin.isClosed()) { // close covariance window
                    impCovWin.close();
                }
            }

            useGpu = gpu && isCuda;
        }
    };

    public ItemListener tbNBPressed = (ItemEvent ev) -> {
        if (ev.getStateChange() == ItemEvent.SELECTED) {
            tbNB.setText("N&B On");
            NBframe.setVisible(true);
        } else {
            tbNB.setText("N&B Off");
            NBframe.setVisible(false);
        }
    };

    public ActionListener tbFilteringPressed = (ActionEvent ev) -> {
        if (tbFiltering.isSelected()) {
            doFiltering = true;
            tbFiltering.setSelected(true);
            SetThresholds();
            filteringframe.setVisible(true);
        } else {
            doFiltering = false;
            tbFiltering.setSelected(false);
            if (filteringframe != null) {
                filteringframe.setVisible(false);
            }
        }

    };

    public ActionListener btnAvePressed = (ActionEvent ev) -> {

        if (impPara1 != null && impPara1.getRoi() != null) {
            printlog("btnAvePressed consider all pixel within selected rectangular ROI drawn on parameter maps");
            calcAveCF(impPara1.getRoi(), true); // Consider all pixel within selected rectangular ROI drawn on parameter
            // maps
        } else {
            printlog("btnAvePressed consider all pixel processed");
            calcAveCF(null, true); // Consider all pixel processed
        }

    };

    public ActionListener btnParaCorPressed = (ActionEvent ev) -> {
        if (setParameters()) {
            plotScatterPlot();
        }
    };

    public ActionListener btnDCCFPressed = (ActionEvent ev) -> {
        if (setParameters()) {
            String $dccf = (String) cbDCCF.getSelectedItem();
            int mode;
            int wx = pixelWidthX;
            int hy = pixelHeightY;

            if ("y direction".equals($dccf)) {
                mode = 2;
            } else if ("diagonal /".equals($dccf)) {
                mode = 3;
            } else if ("diagonal \\".equals($dccf)) {
                mode = 4;
            } else {
                mode = 1;
            }
            Imaging_FCS.correlateDCCFWorker correlateDCCFInstant = new Imaging_FCS.correlateDCCFWorker(mode, wx, hy);
            correlateDCCFInstant.execute();
        }
    };

    public ActionListener btnRTPressed = (ActionEvent ev) -> {
        createImFCSResultsTable();
    };

    public ActionListener btnWriteConfigPressed = (ActionEvent ev) -> {
        writeConfigFile();
    };

    public ActionListener btnDCRPressed = (ActionEvent ev) -> {
        if (dcrobj == null) {
            return;
        }
        // check if computer running Windows OS
        boolean proceed = true;
        String osname = System.getProperty("os.name");
        String osnamelc = osname.toLowerCase();
        if (!osnamelc.contains("win")) {
            proceed = false;
        }
        if (proceed) {
            dcrobj.check();
        } else {
            IJ.showMessage("Direct Capture only supported in Windows");
        }
    };

    public ActionListener btnParamVideoPressed = (ActionEvent ev) -> {
        if (PVideoDialog()) {
            Imaging_FCS.PVideoWorker PVideoInstant = new Imaging_FCS.PVideoWorker(); // TODO: terminate worker so routine can be stopped at wish
            PVideoInstant.execute();
        }
    };

    public ActionListener btnDebugPressed = (ActionEvent ev) -> {
        isTimeProcesses = !isTimeProcesses;
        IJ.log("isTimeProcesses: " + isTimeProcesses);
    };

    public ActionListener btnROIPressed = (ActionEvent ev) -> {

        boolean proceed = true;
        if (!overlap && useGpu) { // prevent non-overlapping ROI running on GPU
            proceed = false;
            IJ.log("ROI non-overlapping mode in GPU is disabled. Either run in CPU or activate toggle overlap on");
        }

        if (setParameters() && proceed) {
            if (impPara1 != null) { // set the parameter window to the front to avaid a "imsPVideoNDave required"
                // error from ImageJ
                WindowManager.setCurrentWindow(impPara1Win);
            }
            Roi improi = imp.getRoi();
            Rectangle rect = improi.getBounds();
            roi1StartX = (int) rect.getX();
            roi1StartY = (int) rect.getY();
            roi1WidthX = (int) rect.getWidth();
            roi1HeightY = (int) rect.getHeight();

            if (improi != null) {
                if (imp.getOverlay() != null) {
                    imp.getOverlay().clear();
                    imp.setOverlay(imp.getOverlay());
                }
                if (cfXDistance != 0 || cfYDistance != 0) {
                    Roi impRoiCCF = (Roi) improi.clone();
                    improi.setLocation(roi1StartX, roi1StartY);
                    improi.setStrokeColor(java.awt.Color.GREEN);
                    imp.setRoi(improi);
                    impRoiCCF.setLocation(roi1StartX + cfXDistance, roi1StartY + cfYDistance);
                    impRoiCCF.setStrokeColor(java.awt.Color.RED);
                    Overlay ccfov = new Overlay(impRoiCCF);
                    imp.setOverlay(ccfov);
                }
                Imaging_FCS.correlateRoiWorker correlateRoiInstant = new Imaging_FCS.correlateRoiWorker(improi);
                correlateRoiInstant.execute();
            } else {
                JOptionPane.showMessageDialog(null, "No ROI chosen.");
            }

        }
    };

    public ActionListener btnAllPressed = (ActionEvent ev) -> {

        // Reset
        // Timing 2D and 3D fitting (CPU and GPU)
        timerObj = new TimerFit();
        timerObj2 = new TimerFit();
        timerObj3 = new TimerFit();
        timerObj4 = new TimerFit();
        timerObj5 = new TimerFit();
        timerObj6 = new TimerFit();
        timerObj7 = new TimerFit();

        if (isTimeProcesses) {
            timerObj7.tic();
        }

        if (setParameters()) {
            if (imp.getOverlay() != null) {
                imp.getOverlay().clear();
                imp.setOverlay(imp.getOverlay());
            }

            if (cfXDistance > 0) {
                roi1StartX = 0;
                roi2StartX = cfXDistance;
            } else {
                roi1StartX = -cfXDistance;
                roi2StartX = 0;
            }
            if (cfYDistance > 0) {
                roi1StartY = 0;
                roi2StartY = cfYDistance;
            } else {
                roi1StartY = -cfYDistance;
                roi2StartY = 0;
            }
            if (overlap) {
                roi1WidthX = width - Math.abs(cfXDistance);
                roi1HeightY = height - Math.abs(cfYDistance);
            } else {
                roi1WidthX = (int) Math.floor((width - Math.abs(cfXDistance)) / binningX) * binningX;
                roi1HeightY = (int) Math.floor((height - Math.abs(cfYDistance)) / binningY) * binningY;
            }

            roi2WidthX = roi1WidthX;
            roi2HeightY = roi1HeightY;
            Roi impRoi1 = new Roi(roi1StartX, roi1StartY, roi1WidthX, roi1HeightY);
            impRoi1.setStrokeColor(java.awt.Color.GREEN);
            imp.setRoi(impRoi1);
            Roi impRoi2 = new Roi(roi2StartX, roi2StartY, roi2WidthX, roi2HeightY);
            if (cfXDistance != 0 || cfYDistance != 0) {
                impRoi2.setStrokeColor(java.awt.Color.RED);
                Overlay cfov = new Overlay(impRoi2);
                imp.setOverlay(cfov);
            }

            checkroi = true;
            if (setParameters()) {

                Imaging_FCS.correlateRoiWorker correlateRoiInstant = new Imaging_FCS.correlateRoiWorker(impRoi1);
                correlateRoiInstant.execute();

                // TODO: perform dimensionality checl e.g., matching correlator scheme as
                // trained
                // This loop executes the ACF CNN fitting on a btnAll press
                if (rbtnCNNACF.isSelected() && cnnACFLoaded) {
                    correlateRoiInstant.addPropertyChangeListener((PropertyChangeEvent event) -> {
                        if (event.getPropertyName().equals("state")) {
                            if (event.getNewValue() == SwingWorker.StateValue.DONE) {
                                executeCnnAcf(impRoi1);
                            }
                        }
                    });
                }

                // This loop executes the Image CNN fitting on a btnAll press
                if (!useGpu) {
                    if (rbtnCNNImage.isSelected() && cnnImageLoaded) {
                        correlateRoiInstant.addPropertyChangeListener((PropertyChangeEvent event) -> {
                            if (event.getPropertyName().equals("state")) {
                                if (event.getNewValue() == SwingWorker.StateValue.DONE) {
                                    executeCnnImage(impRoi1);
                                }
                            }
                        });
                    }
                }

            } else {
                imp.getOverlay().clear();
                imp.deleteRoi();
            }

        }

    };

    public ItemListener tbFCCSDisplayPressed = (ItemEvent ev) -> {
        if (ev.getStateChange() == ItemEvent.SELECTED && cbFitModel.getSelectedItem() == "DC-FCCS (2D)") {
            tbFCCSDisplay.setText("FCCS Disp On");
        } else {
            tbFCCSDisplay.setSelected(false);
            tbFCCSDisplay.setText("FCCS Disp Off");
        }
    };

    public ItemListener tbExpSettingsPressed = (ItemEvent ev) -> {
        if (ev.getStateChange() == ItemEvent.SELECTED) {
            UpdateExpSettings();
            expSettingsFrame.setVisible(true);
        } else {
            expSettingsFrame.setVisible(false);
        }
    };

    public ItemListener tbBleachCorStridePressed = (ItemEvent ev) -> {
        if (ev.getStateChange() == ItemEvent.SELECTED) {
            if (setImp) {
                bleachCorStrideSetFrame.setVisible(true);
            } else {
                tbBleachCorStride.setSelected(false);
                JOptionPane.showMessageDialog(null, "No image stack loaded.");
            }
        } else {
            bleachCorStrideSetFrame.setVisible(false);
        }
    };

    public ItemListener tbFitPressed = (ItemEvent ev) -> {
        if (ev.getStateChange() == ItemEvent.SELECTED) {
            doFit = true;
            tbFit.setText("Fit On");
            fitframe.setVisible(true);
        } else {
            doFit = false;
            tbFit.setText("Fit Off");
            fitframe.setVisible(false);
        }
    };

    public ItemListener tbSimPressed = (ItemEvent ev) -> {
        if (ev.getStateChange() == ItemEvent.SELECTED) {
            tbSim.setText("Sim On");
            simframe.setVisible(true);
        } else {
            tbSim.setText("Sim Off");
            simframe.setVisible(false);
        }
    };

    public ActionListener tbMSDPressed = (ActionEvent ev) -> {
        if (tbMSD.isSelected()) {
            doMSD = true;
            tbMSD.setText("MSD On");
            tbMSD.setSelected(true);
            MSDDialogue();
        } else {
            doMSD = false;
            tbMSD.setText("MSD Off");
            tbMSD.setSelected(false);
        }
    };

    public ItemListener tbOverlapPressed = (ItemEvent ev) -> {
        if (ev.getStateChange() == ItemEvent.SELECTED) {
            overlap = true;
            tbOverlap.setText("Overlap On");
        } else {
            overlap = false;
            tbOverlap.setText("Overlap Off");
        }
    };
}
