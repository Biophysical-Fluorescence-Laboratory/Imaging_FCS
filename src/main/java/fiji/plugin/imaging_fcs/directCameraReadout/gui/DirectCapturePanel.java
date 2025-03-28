package fiji.plugin.imaging_fcs.directCameraReadout.gui;

import fiji.plugin.imaging_fcs.directCameraReadout.andorsdk2v3.AndorSDK2v3;
import fiji.plugin.imaging_fcs.directCameraReadout.andorsdk3v2.AndorSDK3v2;
import fiji.plugin.imaging_fcs.directCameraReadout.control.FrameCounter;
import fiji.plugin.imaging_fcs.directCameraReadout.control.FrameCounterX;
import fiji.plugin.imaging_fcs.directCameraReadout.fcs.ImFCSCorrelator;
import fiji.plugin.imaging_fcs.directCameraReadout.gui.parameterName.liveVideoBinMode;
import fiji.plugin.imaging_fcs.directCameraReadout.gui.parameterName.liveVideoBinMode.liveVideoBinModeEnum;
import fiji.plugin.imaging_fcs.directCameraReadout.gui.parameterName.mode;
import fiji.plugin.imaging_fcs.directCameraReadout.gui.parameterName.mode.modeEnum;
import fiji.plugin.imaging_fcs.directCameraReadout.hamadcamsdk4.Hamamatsu_DCAM_SDK4;
import fiji.plugin.imaging_fcs.directCameraReadout.iccs.ICCS;
import fiji.plugin.imaging_fcs.directCameraReadout.image.DisplayImage;
import fiji.plugin.imaging_fcs.directCameraReadout.pvcamsdk.Photometrics_PVCAM_SDK;
import fiji.plugin.imaging_fcs.directCameraReadout.system.SystemInfo;
import fiji.plugin.imaging_fcs.directCameraReadout.util.TimeTaggedStorage;
import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.gui.*;
import ij.process.ShortProcessor;
import org.apache.commons.math3.special.Erf;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Document;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Level;
import java.util.logging.Logger;

import static fiji.plugin.imaging_fcs.directCameraReadout.gui.parameterName.calibrationType.calibrationTypeList;
import static fiji.plugin.imaging_fcs.directCameraReadout.gui.parameterName.modeType.$amode;
import static fiji.plugin.imaging_fcs.directCameraReadout.updater.Updater.UpdateDimTextField;
import static fiji.plugin.imaging_fcs.directCameraReadout.util.Utilities.*;
import static fiji.plugin.imaging_fcs.version.VERSION.DCR_VERSION;

public class DirectCapturePanel {

    private final static boolean IsSaveExcel = true;
    private final static boolean IsSaveJson = true;
    public static String $camera;
    //Currently programmed "DU860_BV" "DU888_BV" "DU897_BV" "SONA-4BV11" "C11440-22CU" "C11440-22C"
    // "C13440-20CU" "EVOLVE- 512" "GS144BSI" "C13440-20C" "C15550-20UP" "TMP-Kinetix"
    public static int cameraint;
    //Main panel
    public static JDirectCaptureComponent JDirectCapturepanelComponentPanel; //extend JFrame
    public static JToggleButton tbStartStop;
    public static JTextField tfPixelDimension;
    public static JTextField tfExposureTime;
    public static JTextField tfTotalFrame;
    public static JTextField tfTemperature;
    //Dimension panel
    public static JDimensionpanelComponent JDimensionpanelComponentPanel; // extend JFrame
    public static JTextField tfoWidth;
    public static JTextField tfoHeight;
    public static JTextField tfoLeft;
    public static JTextField tfoRight;
    public static JTextField tfoTop;
    public static JTextField tfoBottom;
    public static JTextField tfPlotInterval;
    public static JTextField tfCumulativeFitStart;
    public static JTextField tfICCSRoi1Coord; //width, height, left, top // index start from 1
    public static ICCS iccsObj1; //root
    //JLiveVideo Pane
    public static JLiveVideoPanelComponent JLiveVideoPanelComponentPanel;
    public static JButton btnTest;
    //DisplayImage
    public static DisplayImage DisplayImageObj;
    static boolean DEBUG_TRUE = false; //Debug pane
    private static boolean isHamamatsu;
    //Settings panel
    private static JSettingspanelComponent JSettingspanelComponentPanel; //extend JFrame
    //JCropMode Pane
    private static JCropModePanelComponent JCropModePanelComponentPanel;
    //JCalibration Pane
    private static JCalibrationPanelComponent JCalibrationPanelComponentPanel;
    private static JTextField tfNoPtsCalib;
    //JCumulative Pane
    private static JCumulativeCFPanelComponent JCumulativeCFPanelComponentPanel;
    //JICCSMaps Pane
    private static JICCSPanelComponent JICCSPanelComponentPanel;
    private static JTextField tfICCSParam; //shiftX, shiftY
    //JAnalysisModeSelector Pane
    private static JAcquisitionModePanelComponent JAcquisitionModePanelComponentPanel;
    //Debugging Pane
    private static JTESTPanelComponent JTESTPanelComponentPanel;
    private final String $panelFont = "SansSerif";                            // font and font size of the Panels
    private final int panelFontSize = 12;
    //ImageJ window
    Window imjWindow;
    WindowListener imjWindowListener;
    private JComboBox<String> cbMode;
    private JButton btnSave;
    private JButton btnExit;
    private JToggleButton tbPixelDimension;
    private JToggleButton tbSettings;
    private JTextField tfPixelBinningSoftware;
    private JTextField tfCCFdist;
    private JRadioButton rbCustomROI;
    private JButton btnFullFrame;
    private JComboBox<String> cbPixelEncoding;
    private JComboBox<String> cbInCameraBinning;
    private JRadioButton rbCropMode; //is crop mode?
    private JComboBox<String> cbBleachCorrection;
    private JComboBox<String> cbCorrelator_p;
    private JTextField tfCorrelator_q;
    private JButton btnCorrelator_q;
    private JRadioButton rbGPUon;
    private JButton btnEmGain;
    private JTextField tfEmGain;
    private JButton btnTemperature;
    private JButton btnFan;
    private JButton btnOption; //acf,trace,liveVideo
    private JComboBox<String> cbVspeed;
    private JComboBox<String> cbVSAmp;
    private JComboBox<String> cbHspeed;
    private JComboBox<String> cbPreAmpGain;
    private JToggleButton tbMechanicalShutter;
    private JComboBox<String> cbOutputTrigger_ham;//Hamamatsu Orca
    private JComboBox<String> cbOutputTrigger_sona;//Sona
    private JToggleButton tbOverlap_sona; //Sona
    private JComboBox<String> cbReadoutSpeed_ham;//Hamamatsu Orca
    private JComboBox<String> cbSensorMode_ham;//Hamamatsu Orca
    private JButton btnClearWindow;
    // close CF (non-cumulative or cumulative), Intensity, Calibration (intensity), Calibration (amplitude),
    // Calibration (diffusion) graph
    private JComboBox<String> cbReadoutSpeed_photometric;//Photometrics
    private JComboBox<String> cbCropMode;
    private JTextField tfcWidth;
    private JTextField tfcHeight;
    private JTextField tfcLeft;
    private JTextField tfcRight;
    private JTextField tfcTop;
    private JTextField tfcBottom;
    private JRadioButton rbPlotCalibrationAmplitude;
    private JRadioButton rbPlotCalibrationDiffusion;
    private JRadioButton rbPlotCalibrationIntensity;
    private JButton btnPlotInterval;
    private JToggleButton tbCalibFixScale;
    private JToggleButton tbIsNonCumulCF;//if selected, perfrom non-cumulative CF calculation
    private JToggleButton tbRecordTimePoint;
    //Store calibration time points to json. Post-process software will exclude these time points during the
    // analyssis. Toggle on before making changes to the microscope; toggle off after making changes. Json
    // saved in a folder along with metadata and tiff stacks
    private String currentCalibrationType;
    private String otherRemarks;

    public DirectCapturePanel() {
        addImageJWindowListener();
    }

    private static void printlog(String msg) {
        if (false) {
            IJ.log(msg);
        }
    }

    private static void updateTfFrameCounterV2() {
        // TO be used with syncrhonizer worker

        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {

                while (!Common.isPrematureTermination) {
                    if (Common.isPrematureTermination == true) {
                        break;
                    }
                    Thread.sleep(1);
                    Common.cIsDisplayGUIcounter = false;
                    synchronized (Common.locker2) {
                        while (!Common.cIsDisplayGUIcounter) {
                            Common.locker2.wait();
                            Common.cIsDisplayGUIcounter = true;
                        }
                    }
                    /*
                    Start work
                     */
                    publish(Common.tempGUIcounter);
                    /*
                    End work
                     */
                }

                //                while (Common.isAcquisitionRunning) {
                //                    if (Common.framecounter != null) {
                //                        publish(Common.framecounter.getCounter());
                //                    }
                //                    Thread.sleep(100);
                //                }
                //
                //                tfTotalFrame.setText(Integer.toString(Common.totalFrame));
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {

                if (Common.isAcquisitionRunning) {
                    Integer count = chunks.get(chunks.size() - 1);
                    if (Common.selectedMode == modeEnum.ACQUISITION) {//"Acquisition"
                        tfTotalFrame.setText(Integer.toString(count) + " / " + Common.totalFrame);
                    } else {
                        tfTotalFrame.setText(Integer.toString(count));
                    }
                }

            }

            @Override
            protected void done() {
                tfTotalFrame.setText(Integer.toString(Common.totalFrame));
            }

        };

        worker.execute();
    }

    private static void updateTfFrameCounterV3() {

        // TODO update for new FrameCounter
        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {

                while (Common.isAcquisitionRunning) {
                    if (Common.framecounter != null) {
                        publish(Common.framecounter.getCounter());
                    }
                    Thread.sleep(40);
                }

                tfTotalFrame.setText(Integer.toString(Common.totalFrame));
                return null;
            }

            @Override
            protected void process(List<Integer> chunks) {

                if (Common.isAcquisitionRunning) {
                    Integer count = chunks.get(chunks.size() - 1);
                    if (Common.selectedMode == modeEnum.ACQUISITION) {//"Acquisition"
                        tfTotalFrame.setText(Integer.toString(count) + " / " + Common.totalFrame);
                    } else {
                        tfTotalFrame.setText(Integer.toString(count));
                    }
                }

            }
        };
        worker.execute();
    }

    private static void makeDefaultPanelSetting() {

    }

    /*
     * Utilities
     * checkerBinAndLiveROI:
     * CCFselectorChecker:
     * getCenterCoordinate:
     * getRoiCoordinateFromCorner
     * getRoiCoordinateFromCenter
     * istfWHLTValid

     */
    public static boolean CCFselectorChecker(int oW, int oH, int ShiftX, int ShiftY, int binX, int binY, int px, int py,
                                             int roiW, int roiH) {
        int px2 = px + roiW - 1;
        int py2 = py + roiH - 1;

        //px,py --> coordinate of left top corner
        //px2,py2 --> coordinate of bottom right corner
        // px and py index start at 0; left top corner coordinate
        int mincposx, maxcposx, mincposy, maxcposy, pixelWidthX, pixelHeightY;

        //        pixelWidthX = (int) Math.floor(oW / binX) - 1;
        //        pixelHeightY = (int) Math.floor(oH / binY) - 1;
        pixelWidthX = oW - 1;
        pixelHeightY = oH - 1;

        // set initial, maximum, and minimum cursor positions possible in the image
        if (ShiftX >= 0) {
            //            maxcposx = pixelWidthX - (int) Math.ceil(((double) ShiftX - (oW - (pixelWidthX * binX +
            //            binX))) / binX);
            maxcposx = pixelWidthX - (int) Math.ceil(((double) ShiftX - (oW - (pixelWidthX * 1 + 1))) / 1);
            mincposx = 0;
        } else {
            maxcposx = pixelWidthX;
            mincposx = -(int) Math.floor((double) ShiftX / binX);
        }

        if (ShiftY >= 0) {
            //            maxcposy = pixelHeightY - (int) Math.ceil(((double) ShiftY - (oH - (pixelHeightY * binY +
            //            binY))) / binY);
            maxcposy = pixelHeightY - (int) Math.ceil(((double) ShiftY - (oH - (pixelHeightY * 1 + 1))) / 1);
            mincposy = 0;
        } else {
            maxcposy = pixelHeightY;
            mincposy = -(int) Math.floor((double) ShiftY / binY);
        }

        if (px <= maxcposx && px >= mincposx && py <= maxcposy && py >= mincposy && px2 <= maxcposx &&
                px2 >= mincposx && py2 <= maxcposy && py2 >= mincposy) {
            return true;
        } else {
            IJ.log("CCFselectorChecker: Pixel Out Of Range!");
            return false;

        }

    }

    public static int[] getRoiCoordinateFromCorner(int leftpx, int toppy, int width, int height, int widthMAX,
                                                   int heightMAX) {
        // leftpx and toppy index start from 1
        int[] result = new int[4];
        result[0] = leftpx;
        result[1] = leftpx + width - 1;
        result[2] = toppy;
        result[3] = toppy + height - 1;

        if (result[1] > widthMAX) {
            result[0] = widthMAX - width + 1;
        }
        if (result[3] > heightMAX) {
            result[2] = heightMAX - height + 1;
        }
        return result;

    }

    public static int[] getRoiCoordinateFromCenter(int centerpx, int centerpy, int width, int height, int widthMAX,
                                                   int heightMAX) {


        /*
               widthMAX
            -----------------------------
            -                           -
            -           width           -
            -         * * * *           -   heightMAX
            -         * X   * height    -
            -         * * * *           -
            -                           -
            -                           -
            -                           -   X = (centerpx, centerpy)
            -                           -   // all index start from 1. for example 128 x 128 dim. index start from 1
            and end at 128
            -                           -
            -----------------------------


         */
        int[] result = new int[4];

        int shiftx = width / 2;
        int shifty = height / 2;
        result[0] = centerpx - shiftx;
        result[1] = centerpx - shiftx + width - 1;
        result[2] = centerpy - shifty;
        result[3] = centerpy - shifty + height - 1;

        if (result[0] < 1) {
            result[0] = 1;
            result[1] = 1 + width - 1;
        }

        if (result[1] > widthMAX) {
            result[1] = widthMAX;
            result[0] = widthMAX - width + 1;
        }

        if (result[2] < 1) {
            result[2] = 1;
            result[3] = 1 + height - 1;
        }

        if (result[3] > heightMAX) {
            result[3] = heightMAX;
            result[2] = heightMAX - height + 1;
        }
        //            IJ.log("fromGetCoordinate; left: " + result[0] + ", right: " + result[1] + ", top: " +
        //            result[2] + ", bottom: " + result[3]);
        return result;

    }

    /*
     * Utilities (ImagePlus/Stack related)
     * clearImageStackPlus
     * InitStack
     * fillImagePlusNonCumul
     * fillImagePlusCumul
     * getimp

     */
    public static void clearImageStackPlus(int mode) {
        switch (mode) {
            case 2:
                Common.ims_cum = null;
                Common.imp_cum = null;
                break;

        }
    }

    public static void fillImagePlusCumul() {
        Common.imp_cum = new ImagePlus("imp acquisition", Common.ims_cum);
    }

    /*
        * Utilities (Calcualtor)
        // calculation of the observation area; this is used in the Diffusion Law Plot as the y-axis
        // the calculation of the observation area/volume is provided on our website in CDF files (http://www.dbs.nus
        * .edu.sg/lab/BFL/index.html)

     */
    public static double obsvolFCS_ST2D1p(int dim) {

        double pixeldimx = 240 / Math.pow(10, 9);
        double pixeldimy = 240 / Math.pow(10, 9);
        double sigma = 0.8;
        double emlambda = 515;
        double NA = 1.49;
        double psfsize = (sigma * emlambda / NA) / Math.pow(10, 9);
        int cfXshift = 0;
        int cfYshift = 0;
        int binningX = 1;
        int binningY = 1;

        // general parameters
        double pi = Math.PI;
        double sqrpi = Math.sqrt(pi);
        double ax = pixeldimx;
        //        IJ.log("pixeldimx: " + pixeldimx);
        double ay = pixeldimy;
        //        IJ.log("pixeldimy: " + pixeldimy);
        double s = psfsize;
        //        IJ.log("psfsize: " + psfsize);
        double psfz = 2 * emlambda / Math.pow(10, 9.0) * 1.33 / Math.pow(NA, 2.0); // size of PSF in axial direction
        //        IJ.log("emlambda: " + emlambda + ", NA: " + NA);
        double rx = ax * cfXshift / binningX;
        double ry = ay * cfYshift / binningY;
        //        IJ.log("cfXshift: " + cfXshift);
        //        IJ.log("cfYshift: " + cfYshift);
        //        IJ.log("binningX: " + binningX);
        //        IJ.log("binningY: " + binningY);

        // help variables, for t = 0, to write the full fit function
        double p00 = s;
        double p1x0 = ax;
        double p2x0 = ax;
        double p1y0 = ay;
        double p2y0 = ay;
        double pexpx0 = 2 * Math.exp(-Math.pow(p1x0 / p00, 2)) - 2;
        double perfx0 = 2 * p1x0 * Erf.erf(p1x0 / p00);
        double pexpy0 = 2 * Math.exp(-Math.pow(p1y0 / p00, 2)) - 2;
        double perfy0 = 2 * p1y0 * Erf.erf(p1y0 / p00);

        //return (p00/sqrpi * pexpx0 + perfx0) * (p00/sqrpi * pexpy0 + perfy0) * Math.pow(sz, 2);
        if (dim == 2) {
            return 4 * Math.pow(ax * ay, 2) / ((p00 / sqrpi * pexpx0 + perfx0) * (p00 / sqrpi * pexpy0 + perfy0));
        } else {
            //return sqrpi * szeff * 4 * Math.pow(ax*ay, 2)/( (p00/sqrpi * pexpx0 + perfx0) * (p00/sqrpi * pexpy0 +
            // perfy0) );
            return 4 * Math.pow(ax * ay, 2) / ((p00 / sqrpi * pexpx0 + perfx0) * (p00 / sqrpi * pexpy0 + perfy0));

        }

    }

    /*
    Mode
     */
    private void start_single_capture(boolean isFF) {
        if (Common.isCropMode == 1) {
            IJ.showMessage("Switch off Crop Mode to capture a single frame.");
        } else {
            clearImageStackPlus(2);
            APIcall.runThread_SingleCapture(isFF);
            tbStartStop.setSelected(false);
        }
    }

    private void start_live_video() { //deprecated; CALIBRATION mode essentially do the same with additional CF analysis
        tfExposureTime.setEditable(false);
        Common.setAutoAdjustImageDynamicRange(true);
        clearImageStackPlus(2);
        if (Common.isAcquisitionRunning) {
            JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to complete");
        } else {
            APIcall.runThread_LiveVideo();//13/7/21 use version V2)
            updateTfFrameCounterV2();
        }
    }

    private void start_calibration() {
        tfExposureTime.setEditable(false);
        Common.setAutoAdjustImageDynamicRange(true);
        clearImageStackPlus(2);
        if (Common.isAcquisitionRunning) {
            JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to complete");
        } else {
            APIcall.runThread_nonCumulative(); //13/7/21 use version V3
            updateTfFrameCounterV3();
        }
    }

    private void start_acquisition() {
        tfExposureTime.setEditable(false);
        tfTotalFrame.setEditable(false);
        Common.setAutoAdjustImageDynamicRange(false);
        clearImageStackPlus(2);
        if (Common.isAcquisitionRunning) {
            JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to complete");
        } else {
            APIcall.runThread_Cumulative(); //13/7/21 use version V3
            updateTfFrameCounterV3();
        }
    }

    private void start_iccs_routine() {
        tfExposureTime.setEditable(false);
        Common.setAutoAdjustImageDynamicRange(false);
        clearImageStackPlus(2);

        if (Common.isAcquisitionRunning) {
            JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to complete");
        } else {
            Common.isICCSValid = false;
            APIcall.runThread_ICCScalibration();
            updateTfFrameCounterV3(); //update framecounter
        }
    }

    /*
     * Methods needed when changes are made in GUI
     * invoke changes to document from the Event Dispatched Thread
     * UpdateExpSettings(); Update binning(software) or CFdistance
     * UpdateDimTextField(); Update ROI textfield (shifted to updater package)
     * makeDefaultPanelSetting(); not implemented
     * UpdateCalibParam(): update average point to average for plotting G(0) calibration graph
     * UpdateICCSSettings(); Make changes to green and red roi, parameter storing green ROI and parameter describing
     * red ROI
     */
    private void UpdateExpSettings(String mode) {

        Runnable doUpdateExpSettings = new Runnable() {
            @Override
            public void run() {

                int[] val = new int[2];
                int memx, memy, tempx, tempy;
                switch (mode) {
                    case "SoftBinning":
                        boolean changesmade = false;
                        boolean needChanges = false;
                        boolean invalid = false;

                        memx = Common.BinXSoft;
                        memy = Common.BinYSoft;

                        val = parserTf(tfPixelBinningSoftware);// parse the binning textfiled
                        tempx = val[0];
                        tempy = val[1];

                        //first test
                        if (tempx < 1 || tempx > Common.oWidth) {
                            tempx = memx;
                            invalid = true;
                        }
                        if (tempy < 1 || tempy > Common.oHeight) {
                            tempy = memy;
                            invalid = true;
                        }

                        //second test
                        if (Common.isAcquisitionRunning) {
                            if (Common.lWidth < tempx) {
                                needChanges = true;
                            }
                            if (Common.lHeight < tempy) {
                                needChanges = true;
                            }
                            if (needChanges) {
                                changesmade =
                                        checkerBinAndLiveROI(Common.lWidth, Common.lHeight, Common.lLeft, Common.lTop,
                                                Common.oWidth, Common.oHeight, Common.CCFdistX, Common.CCFdistY, tempx,
                                                tempy, Common.isCCFmode);
                                if (changesmade) {
                                    Common.lWidth = tempx;
                                    Common.lHeight = tempy;

                                    if (Common.isCCFmode) {
                                        Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth,
                                                Common.lHeight);
                                        Common.impRoiLive.setStrokeColor(Color.GREEN);
                                        Common.imp.setRoi(Common.impRoiLive);

                                        Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1,
                                                Common.lTop + Common.CCFdistY - 1, Common.lWidth, Common.lHeight);
                                        Common.impRoiLive2.setStrokeColor(Color.RED);
                                        Overlay impov = new Overlay(Common.impRoiLive2);
                                        Common.imp.setOverlay(impov);
                                    } else {
                                        Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth,
                                                Common.lHeight);
                                        Common.impRoiLive.setStrokeColor(Color.GREEN);
                                        Common.imp.setRoi(Common.impRoiLive);

                                    }

                                } else {
                                    tempx = memx;
                                    tempy = memy;
                                }
                            }
                        }

                        Common.BinXSoft = tempx;
                        Common.BinYSoft = tempy;

                        // Reset textField under these conditions
                        if (tfPixelBinningSoftware.getText().equals("") || tempx != memx || tempy != memy || invalid) {
                            tfPixelBinningSoftware.setText(
                                    Integer.toString(Common.BinXSoft) + " x " + Integer.toString(Common.BinYSoft));
                        }

                        break;
                    case "CFDistance":

                        boolean isChangesMade = false;
                        boolean isCCFselectionOK;
                        memx = Common.CCFdistX;
                        memy = Common.CCFdistY;

                        val = parserTf(tfCCFdist);// parse the binning textfiled

                        tempx = val[0];
                        tempy = val[1];

                        isCCFselectionOK =
                                CCFselectorChecker(Common.oWidth, Common.oHeight, tempx, tempy, Common.BinXSoft,
                                        Common.BinYSoft, Common.lLeft - 1, Common.lTop - 1, Common.lWidth,
                                        Common.lHeight);

                        // checker if user input parameter is valid
                        if (Common.analysisMode.equals($amode[3])) {//Iccs
                            if ((Common.lLeft - 1 + tempx + Common.ICCSShiftX + Common.lWidth) > Common.oWidth ||
                                    (Common.lLeft - 1 + tempx - Common.ICCSShiftX) < 0 ||
                                    (Common.lTop - 1 + tempy + Common.ICCSShiftY + Common.lHeight) > Common.oHeight ||
                                    (Common.lTop - 1 + tempy - Common.ICCSShiftY) < 0 ||
                                    Common.lLeft - 1 + Common.lWidth > Common.oWidth ||
                                    Common.lTop - 1 + Common.lHeight > Common.oHeight) {
                                isCCFselectionOK = false;
                            }
                        }

                        if (isCCFselectionOK) {
                            Common.CCFdistX = tempx;
                            Common.CCFdistY = tempy;
                            if (memx != Common.CCFdistX || memy != Common.CCFdistY) {
                                isChangesMade = true;
                            }
                        } else {
                            Common.CCFdistX = memx;
                            Common.CCFdistY = memy;
                        }

                        if (Common.analysisMode.equals($amode[3])) {//Iccs
                            if (Common.CCFdistX != 0 || Common.CCFdistY != 0) {
                                Common.isCCFmode = true;
                                if (isChangesMade && Common.isAcquisitionRunning && isCCFselectionOK) {

                                    Common.impRoiLive =
                                            new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                    Common.impRoiLive.setStrokeColor(Color.GREEN);
                                    Common.imp.setRoi(Common.impRoiLive);

                                    Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1 - Common.ICCSShiftX,
                                            Common.lTop + Common.CCFdistY - 1 - Common.ICCSShiftY,
                                            Common.lWidth + (2 * Common.ICCSShiftX),
                                            Common.lHeight + (2 * Common.ICCSShiftY));
                                    Common.impRoiLive2.setStrokeColor(Color.RED);
                                    Overlay impov = new Overlay(Common.impRoiLive2);
                                    Common.imp.setOverlay(impov);

                                }
                            } else {
                                Common.isCCFmode = false;
                                if (isChangesMade && Common.isAcquisitionRunning) {

                                    Common.impRoiLive =
                                            new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                    Common.impRoiLive.setStrokeColor(Color.GREEN);
                                    Common.imp.setRoi(Common.impRoiLive);

                                    if (Common.imp.getOverlay() != null) {
                                        Common.imp.getOverlay().clear();
                                        Common.imp.setOverlay(Common.imp.getOverlay());
                                    }

                                }
                            }
                        }

                        if (!Common.analysisMode.equals($amode[3])) {//not Iccs
                            if (Common.CCFdistX != 0 || Common.CCFdistY != 0) {
                                Common.isCCFmode = true;
                                if (isChangesMade && Common.isAcquisitionRunning && isCCFselectionOK) {

                                    Common.impRoiLive =
                                            new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                    Common.impRoiLive.setStrokeColor(Color.GREEN);
                                    Common.imp.setRoi(Common.impRoiLive);

                                    Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1,
                                            Common.lTop + Common.CCFdistY - 1, Common.lWidth, Common.lHeight);
                                    Common.impRoiLive2.setStrokeColor(Color.RED);
                                    Overlay impov = new Overlay(Common.impRoiLive2);
                                    Common.imp.setOverlay(impov);

                                }
                            } else {
                                Common.isCCFmode = false;
                                if (isChangesMade && Common.isAcquisitionRunning) {
                                    Common.impRoiLive =
                                            new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                    Common.impRoiLive.setStrokeColor(Color.GREEN);
                                    Common.imp.setRoi(Common.impRoiLive);

                                    if (Common.imp.getOverlay() != null) {
                                        Common.imp.getOverlay().clear();
                                        Common.imp.setOverlay(Common.imp.getOverlay());
                                    }

                                }
                            }
                        }

                        // Reset textField under these conditions
                        if (tfCCFdist.getText().equals("") || tempx != memx || tempy != memy) {
                            tfCCFdist.setText(
                                    Integer.toString(Common.CCFdistX) + " x " + Integer.toString(Common.CCFdistY));
                        }

                        break;
                    default:
                        throw new java.lang.Error("UpdateExpSettings unmatched case.");
                }

            }

        };
        SwingUtilities.invokeLater(doUpdateExpSettings);
    }

    private void UpdateCalibParam() {

        Runnable doUpdateCalibparam = new Runnable() {

            @Override
            public void run() {
                boolean proceed = false;
                int memint;
                try {
                    memint = Integer.parseInt(tfNoPtsCalib.getText());
                    proceed = true;
                } catch (NumberFormatException nfe) {
                    tfNoPtsCalib.setText(Integer.toString(Common.noptsavr));
                    return;
                }
                if (proceed) {
                    Common.noptsavr = memint;
                }
            }

        };
        SwingUtilities.invokeLater(doUpdateCalibparam);
    }

    private void UpdateICCSSettigs(String mode) {

        Runnable doUpdateICCSSettings = new Runnable() {
            @Override
            public void run() {

                int[] val;
                boolean changesmade = false;//for ROI draw
                boolean needChanges = false; //for ROI draw
                boolean invalid = false;

                switch (mode) {
                    case "CoordinateGreen":
                        // read changes, check if valid, update global parameter and draw ROI if changes is valid
                        int memW, memH, memX, memY, tempW, tempH, tempX, tempY;

                        memW = Common.lWidth;
                        memH = Common.lHeight;
                        memX = Common.lLeft; //index start from 1
                        memY = Common.lTop;

                        val = parserTfICCS(tfICCSRoi1Coord);// parse tf

                        tempW = val[0];
                        tempH = val[1];
                        tempX = val[2];
                        tempY = val[3];

                        if ((tempX - 1 + Common.CCFdistX + Common.ICCSShiftX + tempW) > Common.oWidth ||
                                (tempX - 1 + Common.CCFdistX - Common.ICCSShiftX) < 0 ||
                                (tempY - 1 + Common.CCFdistY + Common.ICCSShiftY + tempH) > Common.oHeight ||
                                (tempY - 1 + Common.CCFdistY - Common.ICCSShiftY) < 0 ||
                                tempX - 1 + tempW > Common.oWidth || tempY - 1 + tempH > Common.oHeight) {
                            invalid = true;//invalid parameter
                        }

                        //                        //check if self-channel correlation
                        //                        if (tempY + Common.CCFdistY - Common.ICCSShiftY < (Common.oHeight /
                        //                        2)) {
                        //                            IJ.log("Warning: self-channel correlation");
                        //                        }
                        if (!invalid) {
                            Common.lWidth = tempW;
                            Common.lHeight = tempH;
                            Common.lLeft = tempX;
                            Common.lTop = tempY;
                        }

                        if (Common.isAcquisitionRunning && !invalid) {

                            if (Common.isCCFmode) {
                                Common.impRoiLive =
                                        new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);

                                Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1 - Common.ICCSShiftX,
                                        Common.lTop + Common.CCFdistY - 1 - Common.ICCSShiftY,
                                        Common.lWidth + (2 * Common.ICCSShiftX),
                                        Common.lHeight + (2 * Common.ICCSShiftY));
                                Common.impRoiLive2.setStrokeColor(Color.RED);
                                Overlay impov = new Overlay(Common.impRoiLive2);
                                Common.imp.setOverlay(impov);
                            } else {
                                Common.impRoiLive =
                                        new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);
                            }
                        }

                        // Reset textField under these conditions
                        if (tfICCSRoi1Coord.getText().equals("") || tempW != memW || tempH != memH || tempX != memX ||
                                tempY != memY || invalid) {
                            tfICCSRoi1Coord.setText(
                                    Integer.toString(Common.lWidth) + " / " + Integer.toString(Common.lHeight) + " / " +
                                            Integer.toString(Common.lLeft) + " / " + Integer.toString(Common.lTop));
                        }

                        break;
                    case "ParamRed":
                        int memShiftX, memShiftY, tempShiftX, tempShiftY;

                        memShiftX = Common.ICCSShiftX;
                        memShiftY = Common.ICCSShiftY;

                        val = parserTfICCS(tfICCSParam);// parse tf

                        tempShiftX = val[0];
                        tempShiftY = val[1];

                        if ((Common.lLeft - 1 + Common.CCFdistX + tempShiftX + Common.lWidth) > Common.oWidth ||
                                (Common.lLeft - 1 + Common.CCFdistX - tempShiftX) < 0 ||
                                (Common.lTop - 1 + Common.CCFdistY + tempShiftY + Common.lHeight) > Common.oHeight ||
                                (Common.lTop - 1 + Common.CCFdistY - tempShiftY) < 0 ||
                                Common.lLeft - 1 + Common.lWidth > Common.oWidth ||
                                Common.lTop - 1 + Common.lHeight > Common.oHeight) {
                            invalid = true;//invalid parameter
                        }

                        //                        //check if self-channel correlation
                        //                        if (Common.lTop + Common.CCFdistY - memShiftY < (Common.oHeight /
                        //                        2)) {
                        //                            IJ.log("Warning: self-channel correlation");
                        //                        }
                        if (!invalid) {

                            if ((tempShiftX != memShiftX) || (tempShiftY != memShiftY)) { // changes made
                                if (tempShiftX == memShiftX) {
                                    Common.ICCSShiftX = tempShiftY;
                                    Common.ICCSShiftY = tempShiftY;
                                } else {
                                    Common.ICCSShiftX = tempShiftX;
                                    Common.ICCSShiftY = tempShiftX;
                                }

                            }

                        }

                        if (Common.isAcquisitionRunning && !invalid) {

                            if (Common.isCCFmode) {
                                Common.impRoiLive =
                                        new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);

                                Common.impRoiLive2 = new Roi(Common.lLeft + Common.CCFdistX - 1 - Common.ICCSShiftX,
                                        Common.lTop + Common.CCFdistY - 1 - Common.ICCSShiftY,
                                        Common.lWidth + (2 * Common.ICCSShiftX),
                                        Common.lHeight + (2 * Common.ICCSShiftY));
                                Common.impRoiLive2.setStrokeColor(Color.RED);
                                Overlay impov = new Overlay(Common.impRoiLive2);
                                Common.imp.setOverlay(impov);
                            } else {
                                Common.impRoiLive =
                                        new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);
                            }
                        }

                        // Reset textField under these conditions
                        if (tfICCSParam.getText().equals("") || tempShiftX != memShiftX || tempShiftY != memShiftY ||
                                invalid) {
                            tfICCSParam.setText(
                                    Integer.toString(Common.ICCSShiftX) + " / " + Integer.toString(Common.ICCSShiftY));
                        }

                        break;

                }

            }

        };
        SwingUtilities.invokeLater(doUpdateICCSSettings);
    }

    private boolean UpdateROIwh(boolean isHam) {

        boolean proceed = false;
        int tempw = 0, temph = 0;
        int[] oCoordinate;

        try {
            tempw = Integer.parseInt(tfoWidth.getText());
            temph = Integer.parseInt(tfoHeight.getText());
            proceed = true;
        } catch (NumberFormatException nfe) {
            return false;
        }

        if (proceed) {

            if (tempw == Common.oWidth && temph == Common.oHeight) {
                return false;
            }

            if (isHam) {
                boolean isWidthValid = false, isHeightValid = false;

                int w, h;

                // check if width and coordinate (left) is valid
                for (w = tempw; w >= Common.minHeight; w--) {
                    oCoordinate = getCenterCoordinate(w, temph, Common.MAXpixelwidth / Common.inCameraBinning,
                            Common.MAXpixelheight / Common.inCameraBinning);
                    isWidthValid = istfWHLTValid(oCoordinate[2], oCoordinate[0], Common.inCameraBinning);
                    if (isWidthValid) {
                        break;
                    }
                }

                // check if height and coordinate (top) is valid
                for (h = temph; h >= Common.minHeight; h--) {
                    oCoordinate = getCenterCoordinate(w, h, Common.MAXpixelwidth / Common.inCameraBinning,
                            Common.MAXpixelheight / Common.inCameraBinning);
                    isHeightValid = istfWHLTValid(oCoordinate[3], oCoordinate[1], Common.inCameraBinning);
                    if (isHeightValid) {
                        break;
                    }
                }

                oCoordinate = getCenterCoordinate(w, h, Common.MAXpixelwidth / Common.inCameraBinning,
                        Common.MAXpixelheight / Common.inCameraBinning);

                Common.oWidth = oCoordinate[0];
                Common.oHeight = oCoordinate[1];
                Common.oLeft = oCoordinate[2];
                Common.oTop = oCoordinate[3];
                Common.oRight = Common.oLeft + Common.oWidth - 1;
                Common.oBottom = Common.oTop + Common.oHeight - 1;

            } else {
                oCoordinate = getCenterCoordinate(tempw, temph, Common.MAXpixelwidth / Common.inCameraBinning,
                        Common.MAXpixelheight / Common.inCameraBinning);

                Common.oWidth = oCoordinate[0];
                Common.oHeight = oCoordinate[1];
                Common.oLeft = oCoordinate[2];
                Common.oTop = oCoordinate[3];
                Common.oRight = Common.oLeft + Common.oWidth - 1;
                Common.oBottom = Common.oTop + Common.oHeight - 1;
            }

            tfoLeft.setText(Integer.toString(Common.oLeft));
            tfoRight.setText(Integer.toString(Common.oRight));
            tfoTop.setText(Integer.toString(Common.oTop));
            tfoBottom.setText(Integer.toString(Common.oBottom));
            tfPixelDimension.setText(Integer.toString(Common.oWidth) + " x " + Integer.toString(Common.oHeight));
            setSizeAandSizeB(Common.oWidth, Common.oHeight, Common.maxE, Common.minPI, Common.maxPI);
            if (Common.plotInterval > retMaxAllowablePlotInterval(Common.size_a, Common.size_b)) {
                if (retMaxAllowablePlotInterval(Common.size_a, Common.size_b) > 500) {
                    Common.plotInterval = 500;
                } else {
                    Common.plotInterval = retMaxAllowablePlotInterval(Common.size_a, Common.size_b);
                }
                tfPlotInterval.setText(Integer.toString(Common.plotInterval));
            }
            return true;
        } else {
            return false;
        }

        //        Runnable doUpdateROI = new Runnable() {
        //            @Override
        //            public void run() {
        //
        //            }
        //
        //        };
        //        SwingUtilities.invokeLater(doUpdateROI);
        //            SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
        //                @Override
        //                protected Void doInBackground() throws Exception {
        //
        //                    return null;
        //                }
        //            };
        //            worker.execute();
    }

    private boolean UpdateROIlrtb(String type, boolean isHam) {
        boolean proceed = false;
        int temp = 0;
        try {
            switch (type) {
                case "l":
                    temp = Integer.parseInt(tfoLeft.getText());
                    break;
                case "r":
                    temp = Integer.parseInt(tfoRight.getText());
                    break;
                case "t":
                    temp = Integer.parseInt(tfoTop.getText());
                    break;
                case "b":
                    temp = Integer.parseInt(tfoBottom.getText());
                    break;
            }
            proceed = true;

        } catch (NumberFormatException nfe) {
            return false;
        }

        if (proceed) {
            switch (type) {
                case "l":
                    if (temp == Common.oLeft) {
                        return false;
                    }
                case "r":
                    if (temp == Common.oRight) {
                        return false;
                    }
                case "t":
                    if (temp == Common.oTop) {
                        return false;
                    }
                case "b":
                    if (temp == Common.oBottom) {
                        return false;
                    }
            }

            if (temp < 1) {
                temp = 1;
            }
            switch (type) {
                case "l":
                    if (temp > (Common.MAXpixelwidth / Common.inCameraBinning)) {
                        temp = Common.MAXpixelwidth / Common.inCameraBinning;
                    }
                    if (temp > Common.oRight) {
                        temp = Common.oRight;
                    }

                    //check if Left coordinate is valid
                    if (isHam) {
                        boolean isValid;
                        int validValue;

                        for (validValue = temp; validValue >= 1; validValue--) {
                            isValid = istfWHLTValid(validValue, Common.oRight - validValue + 1, Common.inCameraBinning);
                            if (isValid) {
                                break;
                            }
                        }
                        Common.oLeft = validValue;
                    } else {
                        Common.oLeft = temp;
                    }

                    Common.oWidth = Common.oRight - Common.oLeft + 1;
                    tfoWidth.setText(Integer.toString(Common.oWidth));
                    break;
                case "r":
                    if (temp > (Common.MAXpixelwidth / Common.inCameraBinning)) {
                        temp = Common.MAXpixelwidth / Common.inCameraBinning;
                    }
                    if (temp < Common.oLeft) {
                        temp = Common.oLeft;
                    }

                    //check if Right coordinate is valid
                    if (isHam) {
                        boolean isValid;
                        int validValue;

                        for (validValue = temp; validValue <= Common.MAXpixelwidth; validValue++) {
                            isValid =
                                    istfWHLTValid(Common.oLeft, validValue - Common.oLeft + 1, Common.inCameraBinning);
                            if (isValid) {
                                break;
                            }
                        }
                        Common.oRight = validValue;
                    } else {
                        Common.oRight = temp;
                    }

                    Common.oWidth = Common.oRight - Common.oLeft + 1;
                    tfoWidth.setText(Integer.toString(Common.oWidth));
                    break;
                case "t":
                    if (temp > (Common.MAXpixelheight / Common.inCameraBinning)) {
                        temp = Common.MAXpixelheight / Common.inCameraBinning;
                    }
                    if (temp > Common.oBottom) {
                        temp = Common.oBottom;
                    }

                    //check if Top coordinate is valid
                    if (isHam) {
                        boolean isValid;
                        int validValue;

                        for (validValue = temp; validValue >= 1; validValue--) {
                            isValid =
                                    istfWHLTValid(validValue, Common.oBottom - validValue + 1, Common.inCameraBinning);
                            if (isValid) {
                                break;
                            }
                        }
                        Common.oTop = validValue;
                    } else {
                        Common.oTop = temp;
                    }

                    Common.oHeight = Common.oBottom - Common.oTop + 1;
                    tfoHeight.setText(Integer.toString(Common.oWidth));
                    break;
                case "b":
                    if (temp > (Common.MAXpixelheight / Common.inCameraBinning)) {
                        temp = Common.MAXpixelheight / Common.inCameraBinning;
                    }
                    if (temp < Common.oTop) {
                        temp = Common.oTop;
                    }

                    //check if Bottom coordinate is valid
                    if (isHam) {
                        boolean isValid;
                        int validValue;

                        for (validValue = temp; validValue <= Common.MAXpixelheight; validValue++) {
                            isValid = istfWHLTValid(Common.oTop, validValue - Common.oTop + 1, Common.inCameraBinning);
                            if (isValid) {
                                break;
                            }
                        }
                        Common.oBottom = validValue;
                    } else {
                        Common.oBottom = temp;
                    }

                    Common.oHeight = Common.oBottom - Common.oTop + 1;
                    tfoHeight.setText(Integer.toString(Common.oWidth));
                    break;
            }
            tfPixelDimension.setText(Integer.toString(Common.oWidth) + " x " + Integer.toString(Common.oHeight));
            setSizeAandSizeB(Common.oWidth, Common.oHeight, Common.maxE, Common.minPI, Common.maxPI);
            if (Common.plotInterval > retMaxAllowablePlotInterval(Common.size_a, Common.size_b)) {
                if (retMaxAllowablePlotInterval(Common.size_a, Common.size_b) > 500) {
                    Common.plotInterval = 500;
                } else {
                    Common.plotInterval = retMaxAllowablePlotInterval(Common.size_a, Common.size_b);
                }
                tfPlotInterval.setText(Integer.toString(Common.plotInterval));
            }
            return true;
        } else {
            return false;
        }

    }

    /*
     * Parser function for:
     * tfBinning, tfCFDistance
     * ICCS calibration: ShiftX, ShiftY
     */
    private int[] parseSetting(JTextField tfset) {

        int m1;
        int m2;
        if (tfset.getDocument() == tfPixelBinningSoftware.getDocument()) {
            m1 = Common.BinXSoft;
            m2 = Common.BinYSoft;
        } else {
            m1 = Common.CCFdistX;
            m2 = Common.CCFdistY;
        }

        int[] val = new int[2];
        val = parserTf(tfset);
        // update text field under certain condition only. THis prevent infinite loop
        if (m1 != val[0] || m2 != val[1] || tfset.getText().equals("")) {
            tfset.setText(val[0] + " x " + val[1]);
        }

        return val;
    }

    private int[] parserTf(JTextField tfset) {
        // return parsed value: CCFdistance and Binning
        int m1, m2;
        if (tfset.getDocument() == tfPixelBinningSoftware.getDocument()) {
            m1 = Common.BinXSoft;
            m2 = Common.BinYSoft;
        } else {
            m1 = Common.CCFdistX;
            m2 = Common.CCFdistY;
        }

        int[] val = new int[2];
        String str = tfset.getText();
        String[] strA;

        if (tfset.getDocument() == tfPixelBinningSoftware.getDocument()) {
            strA = str.replaceAll("[^0-9]+", " ").trim().split(" ");
        } else {
            strA = str.replaceAll("[^-?0-9]+", " ").trim().split(" ");
        }

        try {
            val[0] = Integer.parseInt(strA[0]);
            val[1] = Integer.parseInt(strA[1]);
        } catch (NumberFormatException e) {
            IJ.log("Binning or CF Distance value incorrect.");
            val[0] = m1;
            val[1] = m2;
        } catch (ArrayIndexOutOfBoundsException aob) {
            val[0] = m1;
            val[1] = m2;
        }
        return val;
    }

    private int[] parserTfICCS(JTextField tfset) {
        // return parsed value: tfICCSRoi1Coord and tfICCSParam

        int[] val = null;
        String str = tfset.getText();
        String[] strA;

        int m1, m2, m3, m4;

        if (tfset.getDocument() == tfICCSRoi1Coord.getDocument()) {
            val = new int[4];
            m1 = Common.lWidth;
            m2 = Common.lHeight;
            m3 = Common.lLeft;
            m4 = Common.lTop;
            strA = str.replaceAll("[^0-9]+", " ").trim().split(" ");
            try {
                val[0] = Integer.parseInt(strA[0]);
                val[1] = Integer.parseInt(strA[1]);
                val[2] = Integer.parseInt(strA[2]);
                val[3] = Integer.parseInt(strA[3]);
            } catch (NumberFormatException e) {
                val[0] = m1;
                val[1] = m2;
                val[2] = m3;
                val[3] = m4;
            } catch (ArrayIndexOutOfBoundsException aob) {
                val[0] = m1;
                val[1] = m2;
                val[2] = m3;
                val[3] = m4;
            }

        } else {//tfset.getDocument() == tfICCSParam.getDocument()
            val = new int[2];
            String[] strB;
            m1 = Common.ICCSShiftX;
            m2 = Common.ICCSShiftY;
            strB = str.replaceAll("[^0-9]++", " ").trim().split(" ");
            try {
                val[0] = Integer.parseInt(strB[0]);
                val[1] = Integer.parseInt(strB[1]);
            } catch (NumberFormatException e) {
                val[0] = m1;
                val[1] = m2;
            } catch (ArrayIndexOutOfBoundsException aob) {
                val[0] = m1;
                val[1] = m2;
            }
        }

        return val;

    }

    private boolean checkerBinAndLiveROI(int lW, int lH, int lL, int lT, int oW, int oH, int CCFx, int CCFy, int BinX,
                                         int BinY, boolean isCCF) {

        boolean isNewBinOK = false;

        if (isCCF) {
            isNewBinOK = CCFselectorChecker(oW, oH, CCFx, CCFy, BinX, BinY, lL - 1, lT - 1, lW, lH);

        } else {
            if (oW >= (lL + BinX - 1) && oH >= (lT + BinY - 1)) {
                isNewBinOK = true;
            }
        }
        if (isNewBinOK) {
            return true;
        } else {
            return false;
        }
    }

    private int[] getCenterCoordinate(int w, int h, int wmax, int hmax) {
        //return 0=width, 1=height, 2=left, 3=top
        int[] res = new int[4];
        int tempw = w, temph = h;
        if (tempw < 1) {
            tempw = 1;
        }
        if (tempw > wmax) {
            tempw = wmax;
        }
        if (temph < 1) {
            temph = 1;
        }
        if (temph > hmax) {
            temph = hmax;
        }
        res[0] = tempw;
        res[1] = temph;
        res[2] = (wmax - tempw) / 2 + 1;
        res[3] = (hmax - temph) / 2 + 1;
        //            for (int i = 0; i < res.length; i++) {
        //                IJ.log("res[" + i + "]: " + res[i]);
        //            }
        return res;
    }

    private boolean istfWHLTValid(int left, int width, int bin) {
        // applies to top/height
        // decide if user entered W and H parameters are valid
        boolean isValidL, isValidW;

        //check width valid
        int right = left + width - 1;
        isValidW = ((right - left + 1) * bin) % 4 == 0;

        //check left valid
        int scaledleft = (left * bin) - (bin - 1);
        isValidL = (scaledleft - 1) % 4 == 0;

        return (isValidL && isValidW);

    }

    /*
     * GUI looks and feel
     */
    public void setUIFont(int panelFontSize, String $panelFont) {
        UIManager.getLookAndFeelDefaults()
                .put("defaultFont", new java.awt.Font($panelFont, java.awt.Font.PLAIN, panelFontSize));
        UIManager.put("Button.font", new java.awt.Font($panelFont, java.awt.Font.BOLD, panelFontSize));
        UIManager.put("ToggleButton.font", new java.awt.Font($panelFont, java.awt.Font.BOLD, panelFontSize));
        UIManager.put("RadioButton.font", new java.awt.Font($panelFont, java.awt.Font.BOLD, panelFontSize));
        UIManager.put("Label.font", new java.awt.Font($panelFont, java.awt.Font.ITALIC, panelFontSize));
        UIManager.put("ComboBox.font", new java.awt.Font($panelFont, java.awt.Font.PLAIN, panelFontSize));
        UIManager.put("TextField.font", new java.awt.Font($panelFont, java.awt.Font.PLAIN, panelFontSize));
        UIManager.put("ToolTip.font", new java.awt.Font($panelFont, java.awt.Font.PLAIN, panelFontSize));
    }

    /*
     * Update experimental settings:
     * pixelSize (before in-camera binning), objMag, NA, emlambda, sigmaxy
     */
    public boolean GetExpSettingsDialogue() {

        GenericDialog gd = new GenericDialog("Experimental Settings");
        gd.addNumericField("Pixel size", Common.pixelSize, 1, 4, "\u03BCm");
        gd.addNumericField("Magnification", Common.objMag, 0, 4, "\u00D7");
        gd.addNumericField("NA", Common.NA, 1, 4, "");
        gd.addNumericField("\u03BB (emission)", Common.emlambda, 0, 4, "nm");
        gd.addNumericField("PSF (xy)", Common.sigmaxy, 1, 4, "");
        gd.hideCancelButton();
        gd.setOKLabel​("Set");
        gd.showDialog();

        if (gd.wasOKed()) {
            double ps = (double) gd.getNextNumber();
            int objmag = (int) gd.getNextNumber();
            double na = (double) gd.getNextNumber();
            int em = (int) gd.getNextNumber();
            double sigma = (double) gd.getNextNumber();

            if (!Double.isNaN(ps) && ps > 0) {
                Common.pixelSize = ps;
            } else {
                IJ.log("Invalid Pixel size");
                return false;
            }

            if (objmag > 0) {
                Common.objMag = objmag;
            } else {
                IJ.log("Invalid Magnification");
                return false;
            }

            if (!Double.isNaN(na) && na > 0) {
                Common.NA = na;
            } else {
                IJ.log("Invalid NA");
                return false;
            }

            if (em > 0) {
                Common.emlambda = em;
            } else {
                IJ.log("Invalid Lambda emission");
                return false;
            }

            if (!Double.isNaN(sigma) && sigma > 0) {
                Common.sigmaxy = sigma;
            } else {
                IJ.log("Invalid PSF");
                return false;
            }

        }

        return true;
    }

    /*
    Window Listener
    // addImageJWindowListener(): Safely turn off camera if user happen to exit Fiji before pressing "Exit" button.
    More relevant expecially for DU860 without physical off button at the back of the camera.
     */
    private void addImageJWindowListener() {

        imjWindowListener = new WindowListener() {
            @Override
            public void windowOpened(WindowEvent e) {
            }

            @Override
            public void windowClosing(WindowEvent e) {
                //Check if camera is running; if not stop camera
                if (Common.isAcquisitionRunning) {
                    //do something to stop calibration, acquisiton, live, ICCS
                    Common.isStopPressed = true;
                    APIcall.setStopMechanism(Common.isStopPressed);
                }

                //Check if camera is off; if not call exit camera
                if (Common.isShutSystemPressed == false) {
                    APIcall.exitDirectCaptureProgram();
                }
            }

            @Override
            public void windowClosed(WindowEvent e) {
            }

            @Override
            public void windowIconified(WindowEvent e) {
            }

            @Override
            public void windowDeiconified(WindowEvent e) {
            }

            @Override
            public void windowActivated(WindowEvent e) {
            }

            @Override
            public void windowDeactivated(WindowEvent e) {
            }
        };

        imjWindow = ImageJ.getWindows()[0];
        imjWindow.addWindowListener(imjWindowListener);
    }

    public static class Common {

        /*
        Evaluated start
         */
        public final static int WarningCounts = 16000; //Above which shows warning to user
        public final static int upperlimitExposureTime = 1;

        public final static Object locker1 = new Object(); //for LiveVideo
        public final static Object locker2 = new Object(); //for GUI counter
        //4/200 works except sona 2048x2048
        public final static int maxE = (4 * 2048 * 2048 * 20);
        public final static int maxPI = 20000;
        public final static int minPI = 5000;
        public final static int minAllowableFrame = 100; // minimum frame for ACF
        public final static int transferFrameInterval = 1; //calibration //live //cumualtive
        public final static int fps = 25;
        public final static int dataPtsLastChannel = 100;
        // for acquisition mode. With 5000 frames, setting 16,8 will calculate 16,8
        public final static int dataPtsLastChannel_calibration = 1;
        public final static int zoomFactor = 300; // single capture //live
        public volatile static boolean cIsDisplayLatestFrame;
        public volatile static boolean cIsDisplayGUIcounter;
        public volatile static int tempGUIcounter;
        public static int tempWidth;
        //true width in pixel eg. 128x128 bin 1 = 128x128; 128x128 bin2 = 64x64; 128x128 bin3 = 42x42
        public static int tempHeight; //true width in pixel
        //ixon seris
        public static int isCropMode;//0 false, 1 true
        public static int cWidth;
        public static int cHeight;
        public static int cLeft;
        public static int cRight;
        public static int cTop;
        public static int cBottom;
        //Only iXon series END
        //indexes of selected settings Vertical shift speed(usecs), Horizontal speed/Readout speed (MHz), Vertical
        // Clock Amp, Pre-amp
        public static int iVSpeed;
        public static int iHSpeed;
        public static int iVSamp;
        public static int iPreamp;
        public static int size_a; //calibration //live //cumualtive
        public static int size_b; //calibration //live //cumualtive //2000 works but slow
        public static int size_r;
        public static int size_c;
        public static double maximumBytePerStack = 8000000000f;
        //max bytes per stacks. lets set this to 4 billion bytes/4GB// assume 16-bit images 32x32 maxframe =
        // 4000000000/(2*32*32)
        public static boolean isShutSystemPressed;
        public volatile static boolean isAcquisitionRunning;
        public volatile static boolean isPrematureTermination;
        public volatile static boolean isStopPressed;
        public static boolean isPlotACFdone;
        public static boolean isImageReady;
        public volatile static boolean isResetCalibPlot = false; //TODO:volatile neede?
        public static String $cameraHeadModel;
        public static String $serialNumber;
        //        public volatile static String selectedMode; //TODO:replaced by enum
        public volatile static modeEnum selectedMode;
        public volatile static String analysisMode;//0-None, 1-NonCumulative, 2-Cumulative, 3-Iccs
        //camera acquisition parameters
        public static double exposureTime;
        public static double kineticCycleTime;
        public static int totalFrame;
        public static int plotInterval;
        public static int cumulativePlotInterval = 10;
        public static int BinXSoft;
        public static int BinYSoft;
        public static int inCameraBinning;// 1x1, 2x2, 3x3, 4x4, 8x8 for sona11 and DU860
        public static int CCFdistX; //index start from 0
        public static int CCFdistY; //index start from 0
        public static boolean isCCFmode;
        //setting to 1 will essentially perform calculation back-to back. Increase this number with larger
        // ROI and frame rate
        public static int chipSizeX;
        public static int chipSizeY;
        public static int MAXpixelwidth;
        public static int MAXpixelheight;
        public static int oWidth; //index start from 1
        public static int oHeight; //index start from 1
        public static int oLeft; //index start from 1
        public static int oTop; //index start from 1
        public static int oRight; //index start from 1
        public static int oBottom; //index start from 1
        public static int mem_oWidth;
        public static int mem_oHeight;
        public static int mem_oLeft;
        public static int mem_oTop;
        public static int minHeight; //6 pixels for ixon860
        public static int EMgain;
        public static int temperature;
        public static int mintemp;
        public static int maxtemp;
        public static boolean isCooling;
        public static int[] tempStatus = {
                0, 0
        }; // 0=detector temp; 1=errorCode
        public static String[] FanList;
        public static String FanStatus;
        public static String bleachCor;
        public static int polynomDegree;
        public static int correlator_p;
        public static int correlator_q;
        //for non-cumulative. Make sure setting at 5000 frames setting 16,8 will calculate 16,8
        public static int background = 1_000_000;
        public static boolean RunLiveReadOutOnGPU;
        public static boolean useGpu;
        // Calibration plot
        public static boolean isCalibFixScale = false;
        //Live video display setting
        public static int livevideo_displayFramesMode;
        //0-display all frames; 1-display odd frames; 2-display even frames
        public volatile static liveVideoBinModeEnum selected_livevideo_binningMode;
        public static int livevideo_binningNo;
        //PlotCurve display
        public static boolean plotACFCurves;
        public static boolean plotTrace;
        //by default is 0; setting background >= 1_000_000 will set min counts as bacgkround. see
        // minDetermination(ImagePlus imp);    //V2
        public static boolean plotAverage; // plot average correlation functions
        public static boolean plotJustCCF; //overlay ACFs when plotting CCF in a single graph
        public static boolean showLiveVideoCumul;   //V2
        //Setting this to truew ill remove the auto scaling for 3 calibration plot: diffusion, intensity and
        // amplitude   //V2
        public static boolean plotCalibAmplitude;// Average first few points of correlation for focus finder
        public static boolean plotCalibDiffusion; //Average fit of D for focus finder
        public static boolean plotCalibIntensity;  //Average Intensity for focus finder
        //number of binned frames for display (either a sum or average operation is done to the binned images
        // before displaying on the screen)
        public static ShortProcessor ip;// live
        public static short[] arraysingleS;//singlescan
        public static ImagePlus imp; //singlescan //live
        public static ImageWindow impwin; //singlescan //live
        public static ImageCanvas impcan; //singlescan //live
        public static Roi impRoiLive; //live
        public static Roi impRoiLive2; //live //for CCF mode
        public static double scimp; //live
        public static ImageStack ims_nonCumGreen;
        public static ImageStack ims_nonCumRed;
        public static ImageStack ims_cum;
        public static ImagePlus imp_cum;
        public static int arraysize; //calibration //live //cumualtive
        public volatile static short[] bufferArray1D;
        public static FrameCounterX framecounterIMSX; //cumulative
        public static FrameCounter framecounter;
        public static int lWidth = 6; //index start from 1
        public static int lHeight = 6; //index start from 1
        public static int lLeft = 1; //index start from 1
        public static int lTop = 1; //index start from 1
        // focus-finder
        public static int noptsavr = 3;
        // Cumulative CF
        public static int fitStartCumulative = 1;
        //ICCS //TODO (initizlie param at the start when starting camera)
        public static int ICCSShiftX = 0; // x-span = ICCSShiftX*2 +1
        //live //rseplacing array1Ds  [size_b * size_a * w * h] //V2 //in Java short is signed max value is
        // 32767 instead of 65534. Any counts above 32767 register as 0. Solution is to replace with int[] at
        // expense of doubling RAM usage
        public static int ICCSShiftY = 0;  // y-span = ICCSShiftY*2 +1 // currently only allows shift X = shift Y
        public volatile static boolean isICCSValid;
        public static ImFCSCorrelator fromImFCSobj1; // for non-cumul CF display
        public static ImFCSCorrelator fromImFCSobj2; // for cumulative CF display
        //Experimental parameter for CF data fitting on the fly
        public static double pixelSize = 24; // pixel size in micrometer before in camera pixel binning (if any)
        public static int objMag = 100;
        public static double NA = 1.50;
        // no of correlation points to be averaged (excluding zero time lag) for amplitude & diffusion focus
        // finder analysis
        public static int emlambda = 583;
        public static double sigmaxy = 0.8;
        //autoadjust graph scale
        public static boolean isAutoAdjustACFintensityTraceScale = true; //reset scale by default
        private static int[][] CornerTetherCropROI;//w, h, l, r, t, b
        // evaluate to true once user make selection on the screen//TODO: volatile neede
        //Only iXon series START
        private static ArrayList<String> VspeedArr;
        private static ArrayList<String> HspeedArr;
        private static ArrayList<String> VSAmpArr;
        private static ArrayList<String> PreAmpGainArr;
        private static String $impSavedOR = null;//filename
        private static String $impPathOR = null;
        private static String $impSavingFolderPath = null;
        private static boolean isSaveDone = true;
        //json time tagged
        private static TimeTaggedStorage ttsObj;
        //whether to reset dynamic range every single frame; Setting to true might causes suddent flash when there is
        // sudden change in max or min counts; setting to false allow user to use built in Fiji brightness tool
        private static boolean isAutoAdjustLiveImagesDynamicRange = true;
        //Default settings are as follows; Live video/Calibration mode: true; CF acquisition mode: false;
        // ICCS mode: false

        public static boolean getAutoAdjustImageDynamicRange() {
            return isAutoAdjustLiveImagesDynamicRange;
        }

        /*
        Evaluated end
         */
 /*
        Getter and setter for:
        1) private static boolean isAutoAdjustLiveImagesDynamicRange
         */
        public static void setAutoAdjustImageDynamicRange(boolean isAutoAdjust) {
            isAutoAdjustLiveImagesDynamicRange = isAutoAdjust;
        }
    }

    public static class APIcall {

        private static void setTemperature(int temp) {
            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.SetTemperatureSDK2(temp);
                    break;
                case "DU888_BV":
                    AndorSDK2v3.SetTemperatureSDK2(temp);
                    break;
                case "DU897_BV":
                    AndorSDK2v3.SetTemperatureSDK2(temp);
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.SetDoubleValueSDK3("TargetSensorTemperature", (float) temp);
                    break;
            }
        }

        public static void setCooling(int iscool) {
            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.SetCoolingSDK2(iscool);
                    break;
                case "DU888_BV":
                    AndorSDK2v3.SetCoolingSDK2(iscool);
                    break;
                case "DU897_BV":
                    AndorSDK2v3.SetCoolingSDK2(iscool);
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.SetBooleanValueSDK3("SensorCooling", iscool);
                    break;
            }
        }

        public static void setFan(String fan) {

            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                int iFan = Arrays.asList(Common.FanList).indexOf(fan);
                int iRet = AndorSDK2v3.SetFanModeSDK2(iFan);
                if (iRet != 0) {
                    IJ.log("Error setting fan mode error: " + iRet);
                }
            }

            switch ($camera) {
                case "SONA-4BV11":
                    AndorSDK3v2.SetEnumeratedStringSDK3("FanSpeed", fan);
                    break;
            }
        }

        public static int[] getDetectorDim() {
            int[] res = new int[2];
            switch ($camera) {
                case "DU860_BV":
                    res = AndorSDK2v3.getDetectorDimensionSDK2();
                    break;
                case "DU888_BV":
                    res = AndorSDK2v3.getDetectorDimensionSDK2();
                    break;
                case "DU897_BV":
                    res = AndorSDK2v3.getDetectorDimensionSDK2();
                    break;
                case "SONA-4BV11":
                    res[0] = AndorSDK3v2.GetIntegerValueSDK3("SensorWidth");
                    res[1] = AndorSDK3v2.GetIntegerValueSDK3("SensorHeight");
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    res = Hamamatsu_DCAM_SDK4.getDetectorDimensionSDK4();
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    res = Photometrics_PVCAM_SDK.GetDetectorDimPVCAM();
                    break;
            }
            return res;
        }

        public static int[] getMinMaxTemperature() {
            int[] res = new int[2];
            switch ($camera) {
                case "DU860_BV":
                    res = AndorSDK2v3.getMinMaxTemperatureSDK2();
                    break;
                case "DU888_BV":
                    res = AndorSDK2v3.getMinMaxTemperatureSDK2();
                    break;
                case "DU897_BV":
                    res = AndorSDK2v3.getMinMaxTemperatureSDK2();
                    break;
                case "SONA-4BV11":
                    res[0] = (int) AndorSDK3v2.GetFloatMinSDK3("TargetSensorTemperature");
                    res[1] = (int) AndorSDK3v2.GetFloatMaxSDK3("TargetSensorTemperature");
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    //Orca NA
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    //Photometrics NA
                    break;

            }
            //            IJ.log("Temperature min: " + res[0] + ",max: " + res[1]);
            return res;

        }

        private static void ShutterControl(boolean isShutterOn) {
            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                AndorSDK2v3.ShutterControlSDK2(isShutterOn);
            }
        }

        private static void runThread_UpdateTemp() {
            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.runThread_updatetemp();
                    break;
                case "DU888_BV":
                    AndorSDK2v3.runThread_updatetemp();
                    break;
                case "DU897_BV":
                    AndorSDK2v3.runThread_updatetemp();
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.runThread_updatetemp();
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    //Orca NA
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    //Photometrics NA
                    break;
            }
        }

        private static void runThread_SingleCapture(boolean isFF) {
            // boolean isFF: flag to prevent additional display of single capture image; ROI window selection
            try {
                double exps;
                try {
                    exps = Double.parseDouble(tfExposureTime.getText());
                } catch (NumberFormatException nfe) {
                    IJ.showMessage("Exposure time does not have the right float format");
                    throw new NumberFormatException("Number formal error");
                }

                if (exps > Common.upperlimitExposureTime) {
                    exps = 0.001f;
                }

                Common.exposureTime = exps; // update UI->variable

            } catch (Exception e) {
                IJ.log("something went wrong in setting up parameter for single scan error 26");
            }

            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.runThread_singlecapture(isFF);
                    break;
                case "DU888_BV":
                    AndorSDK2v3.runThread_singlecapture(isFF);
                    break;
                case "DU897_BV":
                    AndorSDK2v3.runThread_singlecapture(isFF);
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.runThread_singlecapture(isFF);
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    Hamamatsu_DCAM_SDK4.runThread_singlecapture(isFF);
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    Photometrics_PVCAM_SDK.runThread_singlecapture(isFF);
                    break;
            }

        }

        private static boolean runThread_LiveVideo() {

            //control flow
            Common.isPrematureTermination = false;
            Common.isStopPressed = false;
            Common.isAcquisitionRunning = true;
            Common.cIsDisplayLatestFrame = false;

            try {
                double exps;
                try {
                    exps = Double.parseDouble(tfExposureTime.getText());
                } catch (NumberFormatException nfe) {
                    IJ.showMessage("Exposure time does not have the right float format");
                    throw new NumberFormatException("Number formal error");
                }

                if (exps > Common.upperlimitExposureTime) {
                    exps = 0.001f;
                }

                Common.exposureTime = exps; // update UI->variable

            } catch (NumberFormatException e) {
                IJ.log("something went wrong in setting up parameter for single scan error 26");
            }

            // Pixel dimension to be passed to Camera(s)
            if (Common.isCropMode == 1) {
                Common.tempWidth = (int) Math.floor(Common.cWidth / Common.inCameraBinning);
                Common.tempHeight = (int) Math.floor(Common.cHeight / Common.inCameraBinning);
            } else {
                Common.tempWidth = Common.oWidth;
                Common.tempHeight = Common.oHeight;
            }

            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.runThread_livevideoV2();
                    break;
                case "DU888_BV":
                    AndorSDK2v3.runThread_livevideoV2();
                    break;
                case "DU897_BV":
                    AndorSDK2v3.runThread_livevideoV2();
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.runThread_livevideoV2();
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    Hamamatsu_DCAM_SDK4.runThread_livevideoV2();
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    Photometrics_PVCAM_SDK.runThread_livevideoV2();
                    break;
            }

            return true;

        }

        private static void runThread_nonCumulative() {

            //control flow
            Common.isPrematureTermination = false;
            Common.isStopPressed = false;
            Common.isAcquisitionRunning = true;
            Common.cIsDisplayLatestFrame = false;

            try {
                double exps;
                try {
                    exps = Double.parseDouble(tfExposureTime.getText());
                } catch (NumberFormatException nfe) {
                    IJ.showMessage("Exposure time does not have the right float format");
                    throw new NumberFormatException("Number formal error");
                }

                if (exps > Common.upperlimitExposureTime) {
                    exps = 0.001f;
                }

                Common.exposureTime = exps; // update UI->variable

            } catch (NumberFormatException e) {
                IJ.log("something went wrong in setting up parameter for single scan error 26");
            }

            try {
                Common.plotInterval = Integer.parseInt(tfPlotInterval.getText());
            } catch (NumberFormatException nfe) {
                IJ.showMessage("set frames to integer " + nfe);
                throw new NumberFormatException("Number format error");
            }

            // Pixel dimension to be passed to Camera(s)
            if (Common.isCropMode == 1) {
                Common.tempWidth = (int) Math.floor(Common.cWidth / Common.inCameraBinning);
                Common.tempHeight = (int) Math.floor(Common.cHeight / Common.inCameraBinning);
            } else {
                Common.tempWidth = Common.oWidth;
                Common.tempHeight = Common.oHeight;
            }

            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.runThread_noncumulativeV3();
                    break;
                case "DU888_BV":
                    AndorSDK2v3.runThread_noncumulativeV3();
                    break;
                case "DU897_BV":
                    AndorSDK2v3.runThread_noncumulativeV3();
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.runThread_noncumulativeV3();
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    Hamamatsu_DCAM_SDK4.runThread_noncumulativeV3();
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    Photometrics_PVCAM_SDK.runThread_noncumulativeV3();
                    break;
            }

        }

        private static void runThread_Cumulative() {
            //control flow
            Common.isPrematureTermination = false;
            Common.isStopPressed = false;
            Common.isAcquisitionRunning = true;
            Common.cIsDisplayLatestFrame = false;
            Common.ttsObj = new TimeTaggedStorage(calibrationTypeList);

            try {
                double exps;
                try {
                    exps = Double.parseDouble(tfExposureTime.getText());
                } catch (NumberFormatException nfe) {
                    IJ.showMessage("Exposure time does not have the right float format");
                    throw new NumberFormatException("Number formal error");
                }

                if (exps > Common.upperlimitExposureTime) {
                    exps = 0.001f;
                }

                Common.exposureTime = exps; // update UI->variable

            } catch (NumberFormatException e) {
                IJ.log("something went wrong in setting up parameter for single scan error 26");
            }

            try {
                int tot;
                tot = Integer.parseInt(tfTotalFrame.getText());
                Common.totalFrame = tot;
            } catch (NumberFormatException nfe) {
                tfTotalFrame.setText(Integer.toString(Common.totalFrame));
                IJ.showMessage("wrong exposureTime format");
                throw new NumberFormatException("Number format error");
            }

            try {
                Common.plotInterval = Integer.parseInt(tfPlotInterval.getText());
            } catch (NumberFormatException nfe) {
                IJ.showMessage("set frames to integer " + nfe);
                throw new NumberFormatException("Number format error");
            }

            // Pixel dimension to be passed to Camera(s)
            if (Common.isCropMode == 1) {
                Common.tempWidth = (int) Math.floor(Common.cWidth / Common.inCameraBinning);
                Common.tempHeight = (int) Math.floor(Common.cHeight / Common.inCameraBinning);
            } else {
                Common.tempWidth = Common.oWidth;
                Common.tempHeight = Common.oHeight;
            }

            if (!setSizeRandSizeC(Common.totalFrame, Common.tempWidth, Common.tempHeight)) {
                IJ.showMessage("Warning: insufficient memory. Reduce ROI or Total frame");
                DirectCapturePanel.tbStartStop.setSelected(false);
                DirectCapturePanel.tfTotalFrame.setEditable(true);
                DirectCapturePanel.tfExposureTime.setEditable(true);
                Common.isPrematureTermination = true;
                Common.isStopPressed = true;
                Common.isAcquisitionRunning = false;
                return;
            }

            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.runThread_cumulativeV3();
                    break;
                case "DU888_BV":
                    AndorSDK2v3.runThread_cumulativeV3();
                    break;
                case "DU897_BV":
                    AndorSDK2v3.runThread_cumulativeV3();
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.runThread_cumulativeV3();
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    Hamamatsu_DCAM_SDK4.runThread_cumulativeV3();
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    Photometrics_PVCAM_SDK.runThread_cumulativeV3();
                    break;
            }

        }

        private static void runThread_ICCScalibration() {
            //control flow
            Common.isPrematureTermination = false;
            Common.isStopPressed = false;
            Common.isAcquisitionRunning = true;

            try {
                double exps;
                exps = Double.parseDouble(tfExposureTime.getText());
                Common.exposureTime = exps; // update UI->variable
            } catch (NumberFormatException nfe) {
                IJ.showMessage("Exposure time does not have the right float format");
                throw new NumberFormatException("Number formal error");
            }

            // Pixel dimension to be passed to Camera(s)
            if (Common.isCropMode == 1) {
                Common.tempWidth = (int) Math.floor(Common.cWidth / Common.inCameraBinning);
                Common.tempHeight = (int) Math.floor(Common.cHeight / Common.inCameraBinning);
            } else {
                Common.tempWidth = Common.oWidth;
                Common.tempHeight = Common.oHeight;
            }

            //            IJ.log("ICCS cropMode: " + Common.isCropMode + ", tempWidth: " + Common.tempWidth + ",
            //            tempHeight: " + Common.tempHeight + ", oWidth: " + Common.oWidth + ", oHeight: " + Common
            //            .oHeight + ", incameraBin: " + Common.inCameraBinning);
            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.runThread_ICCS();
                    break;
                case "DU888_BV":
                    AndorSDK2v3.runThread_ICCS();
                    break;
                case "DU897_BV":
                    AndorSDK2v3.runThread_ICCS();
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.runThread_ICCS();
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    Hamamatsu_DCAM_SDK4.runThread_ICCS();
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    Photometrics_PVCAM_SDK.runThread_ICCS();
                    break;
            }

        }

        private static void setStopMechanism(boolean isstoppressed) {
            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.setStopMechanismSDK2(isstoppressed);//start cpp termination procedure
                    break;
                case "DU888_BV":
                    AndorSDK2v3.setStopMechanismSDK2(isstoppressed);//start cpp termination procedure
                    break;
                case "DU897_BV":
                    AndorSDK2v3.setStopMechanismSDK2(isstoppressed);//start cpp termination procedure
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.setStopMechanismSDK3(isstoppressed);
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    Hamamatsu_DCAM_SDK4.setStopMechanismSDK4(isstoppressed);
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    Photometrics_PVCAM_SDK.setStopMechanismPVCAM(isstoppressed);
                    break;
            }

        }

        private static int exitDirectCaptureProgram() {

            Common.isShutSystemPressed = true;
            int err;
            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.SystemShutDownSDK2();
                    break;
                case "DU888_BV":
                    AndorSDK2v3.SystemShutDownSDK2();
                    break;
                case "DU897_BV":
                    AndorSDK2v3.SystemShutDownSDK2();
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.SystemShutDownSDK3();
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    err = Hamamatsu_DCAM_SDK4.SystemShutDownSDK4();
                    if (err != 0) {
                        IJ.log("unsucessfull uninit Hamamtsu");
                    }
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    err = Photometrics_PVCAM_SDK.SystemShutDownPVCAM();
                    if (err != 0) {
                        IJ.log("unsucessfull uninit Photometrics");
                    }
            }

            JDirectCapturepanelComponentPanel.setVisible(false);
            JDirectCapturepanelComponentPanel.dispose();
            JDimensionpanelComponentPanel.setVisible(false);
            JDimensionpanelComponentPanel.dispose();
            JSettingspanelComponentPanel.setVisible(false);
            JSettingspanelComponentPanel.dispose();
            if (JCropModePanelComponentPanel != null) {
                JCropModePanelComponentPanel.setVisible(false);
                JCropModePanelComponentPanel.dispose();
            }
            JCalibrationPanelComponentPanel.setVisible(false);
            JCalibrationPanelComponentPanel.dispose();
            JICCSPanelComponentPanel.setVisible(false);
            JICCSPanelComponentPanel.dispose();
            JAcquisitionModePanelComponentPanel.setVisible(false);
            JAcquisitionModePanelComponentPanel.dispose();
            JCumulativeCFPanelComponentPanel.setVisible(false);
            JCumulativeCFPanelComponentPanel.dispose();
            JLiveVideoPanelComponentPanel.setVisible(false);
            JLiveVideoPanelComponentPanel.dispose();

            if (DEBUG_TRUE) {
                JTESTPanelComponentPanel.setVisible(false);
                JTESTPanelComponentPanel.dispose();
            }

            if (Common.fromImFCSobj1 != null) {
                Common.fromImFCSobj1.closeWindowsAll();
            }

            if (Common.fromImFCSobj2 != null) {
                Common.fromImFCSobj2.closeWindowsAll();
            }

            if (Common.impwin != null && Common.impwin.isClosed() == false) {
                Common.impwin.close();
            }
            if (DisplayImageObj.impwin != null && DisplayImageObj.impwin.isClosed() == false) {
                DisplayImageObj.impwin.close();
            }

            makeDefaultPanelSetting();

            return 0;
        }

        private static void writeExcel(File sfile, String $exception, boolean showlog) {
            switch ($camera) {
                case "DU860_BV":
                    AndorSDK2v3.writeExcel(sfile, $exception, showlog);
                    break;
                case "DU888_BV":
                    AndorSDK2v3.writeExcel(sfile, $exception, showlog);
                    break;
                case "DU897_BV":
                    AndorSDK2v3.writeExcel(sfile, $exception, showlog);
                    break;
                case "SONA-4BV11":
                    AndorSDK3v2.writeExcel(sfile, $exception, showlog);
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    Hamamatsu_DCAM_SDK4.writeExcel(sfile, $exception, showlog);
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    Photometrics_PVCAM_SDK.writeExcel(sfile, $exception, showlog);
                    break;
            }
        }

    }

    /*
    Snap: single capture

     */
    /*
     * Utilities (Control flow)
     * checkCumulativeReady

     */
    public static class checkCumulativeReady {

        private static int previousFC;

        public static void resetPreviousFC() {
            previousFC = 0;
        }

        public static boolean isImageReady(int frameInterval, int plotInterval, int frameCounterStack) {
            if (!Common.analysisMode.equals($amode[2])) {//not cumulative
                return false;
            }
            if (frameCounterStack == 0) {
                return false;
            }
            if (frameCounterStack < 100) {
                return false;
            }
            double test = (double) frameCounterStack / (double) plotInterval;
            if ((test % 1) == 0) {
                if (previousFC == frameCounterStack) {
                    return false;
                } else {
                    previousFC = frameCounterStack;
                    return true;
                }
            }
            return false;
            //            //alternative, step-by-step
            //            double divisor = (double) plotInterval / (double) frameInterval;
            //            double runner = (double) frameCounterStack / (double) frameInterval;
            //            return ((runner / divisor) % 1 == 0);
        }
    }

    public static class multiTauCorrelatorCalculator {

        // get timelag of last correlation channel
        public static double getTimeLag(double frametime, int corr_p, int corr_q) {
            //            double p1 = (double) (corr_p - 1); // include zero timelag in first p correlator
            double p1 = (double) (corr_p); // exclude zero timelag
            double p2 = 0;
            for (int i = 1; i < corr_q; i++) {
                p2 += corr_p * Math.pow(2, i) / 2;
            }
            //IJ.log("getTimeLag: " + frametime * (p1 + p2));
            return (frametime * (p1 + p2));
        }

        // get minimum frame (independent of frametime)
        // last = min data points in last channel
        public static int getMinFrame(double frametime, int corr_p, int corr_q, int last) {
            double p = frametime * last * Math.pow(2, corr_q -
                    1); // given min point of last channel= 1 & include zero timelag, (16,2) = 33 not 31; (16,8) =
            // 2175 not 2047
            //IJ.log("getMinFrame: " + (int) Math.ceil((p + getTimeLag(frametime, corr_p, corr_q)) / frametime));
            return (int) Math.ceil((p + getTimeLag(frametime, corr_p, corr_q)) / frametime);
        }

        //tauD = Aeff/(4D)
        public static double getTauD(double D) { // D in um2/s
            //            IJ.log("getTauD: " + obsvolFCS_ST2D1p(2) / (4 * D / Math.pow(10, 12)));
            return obsvolFCS_ST2D1p(2) / (4 * D / Math.pow(10, 12));
        }

        // get upper tauD (Fix: var = 4Dt)
        public static double getTauDupper(double D, int cl) { //cl = confidence itnerval cl 3 = 99.7% coverage
            double tdmean = getTauD(D);
            double tdupper = Math.pow((cl * Math.sqrt(4 * D * tdmean)), 2) / (4 * D);
            //            IJ.log("getTauDupper(correct): " + tdupper);
            return tdupper;
        }

        //find minimum q (Fix: var = 4Dt)
        public static int find_q(double D, double frametime, int p, int cl) {
            double upperTauD = getTauDupper(D, cl);
            int tempQ = 1;
            while (getTimeLag(frametime, p, tempQ) < upperTauD) {
                tempQ++;
            }
            return tempQ;
        }

        // find D
        public static double find_D(double D, int p, int q, double frametime, int cl) {
            double maxlag = getTimeLag(frametime, p, q);
            double tauD = maxlag / Math.pow(cl, 2); // tdmean
            //            IJ.log("tauD: " + tauD);
            //            IJ.log("obsvolFCS_ST2D1p(2) * Math.pow(10, 12): " + obsvolFCS_ST2D1p(2) * Math.pow(10, 12));

            return obsvolFCS_ST2D1p(2) * Math.pow(10, 12) / (4 * tauD);
        }

        public static int getMinFrame(int frameTime, int corr_p, int corr_q, int dataPtsLastCorChannel) {
            double p = frameTime * dataPtsLastCorChannel * Math.pow(2, (corr_q - 1));
            return (int) ((p + getTimeLag(frameTime, corr_p, corr_q)) / frameTime);
        }

        //return q value given number of frame available
        public static int getQgivenFrame(int p, int q, int noframe, int dataPtsLastCorChannel) {
            if (getMinFrame(0.001, p, 1, dataPtsLastCorChannel) > noframe) {
                return 1;
            }
            int TempQ = q;
            while (getMinFrame(0.001, p, TempQ, dataPtsLastCorChannel) > noframe) {
                TempQ = TempQ - 1;
            }
            //            if (TempQ == 0){
            //                return 1;
            //            }
            return TempQ;
        }

    }

    public class ORpanel {

        public ORpanel(String cameramodel) {

            // set Fonts
            setUIFont(panelFontSize, $panelFont);

            $camera = cameramodel;
            switch ($camera) {
                case "DU860_BV":
                    cameraint = 860;
                    break;
                case "DU888_BV":
                    cameraint = 888;
                    break;
                case "DU897_BV":
                    cameraint = 897;
                    break;
            }
            resetUIparameter();
            //DisplayImage object creation
            isHamamatsu = DirectCapturePanel.$camera.equals("C11440-22CU") ||
                    DirectCapturePanel.$camera.equals("C11440-22C") ||
                    DirectCapturePanel.$camera.equals("C13440-20CU") ||
                    DirectCapturePanel.$camera.equals("C13440-20C") || DirectCapturePanel.$camera.equals("C15550-20UP");
            DisplayImageObj = new DisplayImage(isHamamatsu);

            createDirectCapturePanel();
            createDimensionPanel();
            createSettingsPanel();
            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                createCropModePanel();
            }
            createCalibrationPanel();
            createICCSPanel();
            if (DEBUG_TRUE) {
                createTESTPanel();
            }

            createAnalysisSelectorPanel();

            createCumulativeCFPanel();

            createLiveVideoOptionPanel();

            APIcall.runThread_UpdateTemp();

            /*
            Print camera model onto log
             */
            IJ.log("Detected camera model: " + Common.$cameraHeadModel + ", camera ID: " + Common.$serialNumber);

        }

        private void resetUIparameter() {
            //TODO: read data from config file

            //Setting crop mode
            Common.isCropMode = 0; // setting false by default
            switch ($camera) {
                case "DU860_BV":
                    Common.CornerTetherCropROI = cameraConstant.Common_iXon860.RecommendedCentralCrop;
                    break;
                case "DU888_BV":
                    Common.CornerTetherCropROI = cameraConstant.Common_iXon888.RecommendedCentralCrop;
                    break;
                case "DU897_BV":
                    Common.CornerTetherCropROI = cameraConstant.Common_iXon897.RecommendedCentralCrop;
                    break;
            }

            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                Common.cWidth = Common.CornerTetherCropROI[0][0];
                Common.cHeight = Common.CornerTetherCropROI[0][1];
                Common.cLeft = Common.CornerTetherCropROI[0][2];
                Common.cRight = Common.CornerTetherCropROI[0][3];
                Common.cTop = Common.CornerTetherCropROI[0][4];
                Common.cBottom = Common.CornerTetherCropROI[0][5];
            }

            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                Common.FanList = new String[3];
                Common.FanList[0] = "Full";
                Common.FanList[1] = "Low";
                Common.FanList[2] = "Off";
                Common.FanStatus = "Full"; //Default is set to full fan mode
            }

            switch ($camera) {
                case "DU860_BV":
                    Common.temperature = cameraConstant.Common_iXon860.defaultTemp;
                    Common.minHeight = cameraConstant.Common_iXon860.minHeight;
                    break;
                case "DU888_BV":
                    Common.temperature = cameraConstant.Common_iXon888.defaultTemp;
                    Common.minHeight = cameraConstant.Common_iXon888.minHeight;
                    break;
                case "DU897_BV":
                    Common.temperature = cameraConstant.Common_iXon897.defaultTemp;
                    Common.minHeight = cameraConstant.Common_iXon897.minHeight;
                    break;
                case "SONA-4BV11":
                    Common.temperature = cameraConstant.Common_SONA.defaultTemp;
                    Common.minHeight = cameraConstant.Common_SONA.minHeight;
                    Common.FanStatus = AndorSDK3v2.GetEnumeratedStringSDK3("FanSpeed");
                    int count = AndorSDK3v2.GetEnumCountSDK3("FanSpeed");
                    Common.FanList = new String[count];
                    for (int i = 0; i < count; i++) {
                        Common.FanList[i] = AndorSDK3v2.GetEnumStringByIndexSDK3("FanSpeed", i);
                    }
                    count = 3;
                    cameraConstant.Common_SONA.listPixelEncoding = new String[count];//exclude Mono32
                    for (int i = 0; i < count; i++) {
                        cameraConstant.Common_SONA.listPixelEncoding[i] =
                                AndorSDK3v2.GetEnumStringByIndexSDK3("PixelEncoding", i);
                    }
                    cameraConstant.Common_SONA.PixelEncoding = 1; //Monopacked12 by default
                    cameraConstant.Common_SONA.isOverlap = 1;
                    cameraConstant.Common_SONA.OutputTriggerKindArr = new String[4];
                    cameraConstant.Common_SONA.OutputTriggerKindArr[0] = "FireRow1";
                    cameraConstant.Common_SONA.OutputTriggerKindArr[1] = "FireRowN";
                    cameraConstant.Common_SONA.OutputTriggerKindArr[2] = "FireAll";
                    cameraConstant.Common_SONA.OutputTriggerKindArr[3] = "FireAny";
                    cameraConstant.Common_SONA.OutputTriggerKind = 2;
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                    Common.minHeight = cameraConstant.Common_Orca.minHeight;
                    cameraConstant.Common_Orca.outTriggerDelay = 0; //0 us
                    cameraConstant.Common_Orca.outTriggerPeriod = 0.0001; //100 us
                    cameraConstant.Common_Orca.OutputTriggerKindArr = new String[3];
                    cameraConstant.Common_Orca.OutputTriggerKindArr[0] = "Disabled";
                    cameraConstant.Common_Orca.OutputTriggerKindArr[1] = "Programmable";
                    cameraConstant.Common_Orca.OutputTriggerKindArr[2] = "Global";
                    cameraConstant.Common_Orca.OutputTriggerKind = 0;
                    cameraConstant.Common_Orca.readoutSpeedArr = new String[2];
                    cameraConstant.Common_Orca.readoutSpeedArr[0] = "Ultra-quiet";
                    cameraConstant.Common_Orca.readoutSpeedArr[1] = "Standard scan";
                    cameraConstant.Common_Orca.readoutSpeed =
                            1; //0-DCAMPROP_READOUTSPEED__SLOWEST; 1-DCAMPROP_READOUTSPEED__FASTEST (default for both
                    // orca flash and orca quest)
                    cameraConstant.Common_Orca.sensorModeArr = new String[1];
                    cameraConstant.Common_Orca.sensorModeArr[0] = "Area";
                    cameraConstant.Common_Orca.sensorMode =
                            0;//0-DCAMPROP_SENSORMODE__AREA (default and only mode for flash)
                    break;
                case "C15550-20UP":
                    Common.minHeight = cameraConstant.Common_Orca.minHeight;
                    cameraConstant.Common_Orca.outTriggerDelay = 0; //0 us
                    cameraConstant.Common_Orca.outTriggerPeriod = 0.0001; //100 us
                    cameraConstant.Common_Orca.OutputTriggerKindArr = new String[3];
                    cameraConstant.Common_Orca.OutputTriggerKindArr[0] = "Disabled";
                    cameraConstant.Common_Orca.OutputTriggerKindArr[1] = "Programmable";
                    cameraConstant.Common_Orca.OutputTriggerKindArr[2] = "Global";
                    cameraConstant.Common_Orca.OutputTriggerKind = 0;
                    cameraConstant.Common_Orca.readoutSpeedArr = new String[2];
                    cameraConstant.Common_Orca.readoutSpeedArr[0] = "Ultra-quiet";
                    cameraConstant.Common_Orca.readoutSpeedArr[1] = "Standard scan";
                    cameraConstant.Common_Orca.readoutSpeed =
                            1; //0-DCAMPROP_READOUTSPEED__SLOWEST; 1-DCAMPROP_READOUTSPEED__FASTEST (default for both
                    // orca flash and orca quest)
                    cameraConstant.Common_Orca.sensorModeArr = new String[2];
                    cameraConstant.Common_Orca.sensorModeArr[0] = "Area";
                    cameraConstant.Common_Orca.sensorModeArr[1] = "Photon counting";
                    cameraConstant.Common_Orca.sensorMode =
                            0;//0-DCAMPROP_SENSORMODE__AREA; 1-DCAMPROP_SENSORMODE__PHOTONNUMBERRESOLVING
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    Common.minHeight = cameraConstant.Common_Photometrics.minHeight;

                    //call API to check available aspect regarding available: port and speed combination
                    int noPortSpeedCombination = 0;
                    int portSize = Photometrics_PVCAM_SDK.getPortSize();// call API get total port
                    //assert portSize != 0
                    for (int i = 0; i < portSize; i++) {
                        // call API get total speedcount for each port index
                        int speedCount = Photometrics_PVCAM_SDK.getSpeedCount(i);
                        //assert speedCount != 0
                        noPortSpeedCombination += speedCount;
                    }

                    cameraConstant.Common_Photometrics.readoutSpeedDescription = new String[noPortSpeedCombination][5];
                    int counter = 0;
                    for (int i = 0; i < portSize; i++) {
                        // call API get total speedcount for each port index
                        int speedCount = Photometrics_PVCAM_SDK.getSpeedCount(i);
                        for (int j = 0; j < speedCount; j++) {
                            Photometrics_PVCAM_SDK.setPortAndSpeedPair(i, j);
                            cameraConstant.Common_Photometrics.readoutSpeedDescription[counter][0] =
                                    Integer.toString(i); //port index
                            cameraConstant.Common_Photometrics.readoutSpeedDescription[counter][1] =
                                    Integer.toString(j); //speed index
                            cameraConstant.Common_Photometrics.readoutSpeedDescription[counter][2] = Double.toString(
                                    Photometrics_PVCAM_SDK.getDoubleValuePVCAM(
                                            "readoutFrequency")); //readout speed (MHz)
                            cameraConstant.Common_Photometrics.readoutSpeedDescription[counter][3] = Double.toString(
                                    Photometrics_PVCAM_SDK.getDoubleValuePVCAM("BIT_DEPTH")); //bit depth
                            cameraConstant.Common_Photometrics.readoutSpeedDescription[counter][4] =
                                    cameraConstant.Common_Photometrics.readoutSpeedDescription[counter][2] + " MHz " +
                                            cameraConstant.Common_Photometrics.readoutSpeedDescription[counter][3] +
                                            "-bit"; //descriptin

                            counter++;
                        }
                    }
            }

            int[] tempmaxdim = APIcall.getDetectorDim();
            Common.chipSizeX = tempmaxdim[0];
            Common.chipSizeY = tempmaxdim[1];
            Common.MAXpixelwidth = Common.chipSizeX;
            Common.MAXpixelheight = Common.chipSizeY;

            Common.inCameraBinning = 1;

            int[] temp = getCenterCoordinate(Common.minHeight, Common.minHeight,
                    Common.MAXpixelwidth / Common.inCameraBinning, Common.MAXpixelheight / Common.inCameraBinning);
            Common.oWidth = temp[0];
            Common.oHeight = temp[1];
            Common.oLeft = temp[2];
            Common.oTop = temp[3];
            Common.oRight = Common.oLeft + Common.oWidth - 1;
            Common.oBottom = Common.oTop + Common.oHeight - 1;

            setSizeAandSizeB(Common.oWidth, Common.oHeight, Common.maxE, Common.minPI, Common.maxPI);
            if (retMaxAllowablePlotInterval(Common.size_a, Common.size_b) > 500) {
                Common.plotInterval = 500;
            } else {
                Common.plotInterval = retMaxAllowablePlotInterval(Common.size_a, Common.size_b);
            }

            Common.exposureTime = 0.001;
            Common.totalFrame = 50000;

            Common.BinXSoft = 1;
            Common.BinYSoft = 1;

            Common.CCFdistX = 0;
            Common.CCFdistY = 0;
            Common.isCCFmode = false;

            Common.EMgain = 300;

            Common.bleachCor = "none";
            Common.polynomDegree = 4;
            Common.correlator_p = 16;
            Common.correlator_q = 8;
            Common.RunLiveReadOutOnGPU = false;
            Common.useGpu = false; //TODO remove this // main ImFCS panel will assigne

            Common.plotACFCurves = true;
            Common.plotTrace = true;
            Common.showLiveVideoCumul = true;
            Common.plotJustCCF = true;
            Common.plotAverage = false;
            Common.plotCalibAmplitude = false;
            Common.plotCalibDiffusion = false;
            Common.plotCalibIntensity = false;

            //Live video option
            Common.livevideo_displayFramesMode = 0; //by default display all frames
            Common.livevideo_binningNo = 1;         //by default no binning

            Common.isCooling = true;
            int[] tempminmax = APIcall.getMinMaxTemperature();
            Common.mintemp = tempminmax[0];
            Common.maxtemp = tempminmax[1];

            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                String[] tempArray = AndorSDK2v3.GetAvailableVSSpeedsSDK2();
                Common.VspeedArr = new ArrayList<String>(Arrays.asList(tempArray));
                tempArray = AndorSDK2v3.GetAvailableVSAmplitudeSDK2();
                Common.VSAmpArr = new ArrayList<String>(Arrays.asList(tempArray));
                tempArray = AndorSDK2v3.GetAvailableHSSpeedsSDK2();
                Common.HspeedArr = new ArrayList<String>(Arrays.asList(tempArray));
                tempArray = AndorSDK2v3.GetAvailablePreAmpGainSDK2();
                Common.PreAmpGainArr = new ArrayList<String>(Arrays.asList(tempArray));

                //setdefault
                Common.iVSpeed = Common.VspeedArr.size() - 1;
                Common.iVSamp = 0;
                Common.iHSpeed = 0;
                Common.iPreamp = Common.PreAmpGainArr.size() - 1;

                printlog("print Vspeed");
                for (int i = 0; i < Common.VspeedArr.size(); i++) {
                    printlog("i " + Common.VspeedArr.get(i));
                }
                printlog("print Vsamp");
                for (int i = 0; i < Common.VSAmpArr.size(); i++) {
                    printlog("i " + Common.VSAmpArr.get(i));
                }
                printlog("print HspeedArr");
                for (int i = 0; i < Common.HspeedArr.size(); i++) {
                    printlog("i " + Common.HspeedArr.get(i));
                }
                printlog("print PreAmpGainArr");
                for (int i = 0; i < Common.PreAmpGainArr.size(); i++) {
                    printlog("i " + Common.PreAmpGainArr.get(i));
                }
            }

            // Setting Experimental parameters
            boolean proceed = false;
            while (!proceed) {
                proceed = GetExpSettingsDialogue();
            }

        }

        private void createDirectCapturePanel() {
            JDirectCapturepanelComponentPanel = new JDirectCaptureComponent();
        }

        private void createDimensionPanel() {
            JDimensionpanelComponentPanel = new JDimensionpanelComponent();
        }

        private void createSettingsPanel() {
            JSettingspanelComponentPanel = new JSettingspanelComponent();
        }

        private void createCropModePanel() {
            JCropModePanelComponentPanel = new JCropModePanelComponent();
        }

        private void createCalibrationPanel() {
            JCalibrationPanelComponentPanel = new JCalibrationPanelComponent();
        }

        private void createICCSPanel() {
            JICCSPanelComponentPanel = new JICCSPanelComponent();
        }

        private void createTESTPanel() {
            JTESTPanelComponentPanel = new JTESTPanelComponent(this);
        }

        private void createAnalysisSelectorPanel() {
            JAcquisitionModePanelComponentPanel = new JAcquisitionModePanelComponent();
        }

        private void createCumulativeCFPanel() {
            JCumulativeCFPanelComponentPanel = new JCumulativeCFPanelComponent();
        }

        private void createLiveVideoOptionPanel() {
            JLiveVideoPanelComponentPanel = new JLiveVideoPanelComponent();
        }

    }

    public class JDirectCaptureComponent extends JFrame {

        final int DCpanelPosX = 425;
        // control panel, "ImFCS", position and dimensions
        final int DCpanelPosY = 125;
        final int DCpanelDimX = 270;
        final int DCpanelDimY = 280;
        // DocumentLsitener to act on textfield changes
        DocumentListener tfBinChanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                UpdateExpSettings("SoftBinning");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                UpdateExpSettings("SoftBinning");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                UpdateExpSettings("SoftBinning");
            }
        };
        // DocumentLsitener to act on textfield changes
        DocumentListener tfCCFdistChanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                UpdateExpSettings("CFDistance");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                UpdateExpSettings("CFDistance");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                UpdateExpSettings("CFDistance");
            }
        };
        ActionListener btnSavePressed = (ActionEvent event) -> {

            if (Common.isAcquisitionRunning) {
                JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to finish");
            } else {
                if (Common.ims_cum != null) {
                    if (Common.isSaveDone) {
                        int ret = DisplaySavedImageDialogue();
                        if (ret == 1 || ret == 2) {
                            ImagePlus tempPlus = new ImagePlus("Unsaved images", Common.ims_cum); //pass-by-reference
                            if (tempPlus.getSizeInBytes() > Common.maximumBytePerStack) {
                                // Dialog box whether user would like to split the images? tend to very slow
                                boolean check = false;
                                while (!check) {
                                    check = SavingSizePerStackDialog(tempPlus);
                                }
                            }
                            IJ.log("Saving in progress..." + Common.maximumBytePerStack);

                            runSaveMechanism(Common.isSaveDone, tempPlus, ret);
                        }

                    } else {
                        IJ.showMessage("Be patient... (let us know for file saving issue)");
                    }
                } else {
                    IJ.showMessage("No images available, start acquisition mode");
                }
            }
        };
        ActionListener btnExitPressed = (ActionEvent event) -> {
            JDirectCapturepanelComponentPanel.setVisible(true);
            if (Common.isAcquisitionRunning) {
                JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to finish");
            } else {
                int someval = ExitDialogue();
                if (someval == 1 || someval == 2) {
                    if (someval == 1) {
                        IJ.log("saving acquisition settings to config file...");
                    }
                    if (Common.isShutSystemPressed == false) {
                        APIcall.exitDirectCaptureProgram();
                    }
                }
            }

        };
        ActionListener btnPixelBinningSoftwarePressed = (ActionEvent event) -> {
            SoftwarePixelBinningDialogue();
        };
        ActionListener btnCCFdistPressed = (ActionEvent event) -> {
            CCFdistDialogue();
        };
        ActionListener cbModeChanged = (ActionEvent event) -> {

            if (cbMode.getSelectedItem().toString().equals(mode.getStringValue(modeEnum.SINGLECAPTURE.getValue()))) {
                Common.selectedMode = modeEnum.SINGLECAPTURE;//single capture
                Common.analysisMode = $amode[0];//None
                tfTotalFrame.setEditable(false);
                JCalibrationPanelComponentPanel.setVisible(false);
                JICCSPanelComponentPanel.setVisible(false);
                JAcquisitionModePanelComponentPanel.setVisible(false);
                JCumulativeCFPanelComponentPanel.setVisible(false);
                JLiveVideoPanelComponentPanel.setVisible(false);
            }

            if (cbMode.getSelectedItem().toString().equals(mode.getStringValue(modeEnum.LIVEVIDEO.getValue()))) {
                Common.selectedMode = modeEnum.LIVEVIDEO;//live video
                Common.analysisMode = $amode[0];//None
                tfTotalFrame.setEditable(false);
                JCalibrationPanelComponentPanel.setVisible(false);
                JICCSPanelComponentPanel.setVisible(false);
                JAcquisitionModePanelComponentPanel.setVisible(false);
                JCumulativeCFPanelComponentPanel.setVisible(false);
                JLiveVideoPanelComponentPanel.setVisible(false);
            }

            if (cbMode.getSelectedItem().toString().equals(mode.getStringValue(modeEnum.CALIBRATION.getValue()))) {
                Common.selectedMode = modeEnum.CALIBRATION;//calibration
                Common.analysisMode = $amode[1];//Non-cumulative
                tfTotalFrame.setEditable(false);
                if (Common.plotACFCurves) {
                    JCalibrationPanelComponentPanel.setVisible(true);
                }
                JICCSPanelComponentPanel.setVisible(false);
                JAcquisitionModePanelComponentPanel.setVisible(false);
                JCumulativeCFPanelComponentPanel.setVisible(false);
                JLiveVideoPanelComponentPanel.setVisible(true);
            }

            if (cbMode.getSelectedItem().toString().equals(mode.getStringValue(modeEnum.ACQUISITION.getValue()))) {
                Common.selectedMode = modeEnum.ACQUISITION;//acquisition
                Common.analysisMode = tbIsNonCumulCF.getText();
                tfTotalFrame.setEditable(true);

                // make sure reset cumulative mode
                Common.analysisMode = $amode[2];//cumulative
                tbIsNonCumulCF.setText($amode[2]);
                tbIsNonCumulCF.setSelected(false);
                JCalibrationPanelComponentPanel.setVisible(false);
                if (Common.plotACFCurves) {
                    JCumulativeCFPanelComponentPanel.setVisible(true);
                }

                JICCSPanelComponentPanel.setVisible(false);
                if (Common.plotACFCurves) {
                    JAcquisitionModePanelComponentPanel.setVisible(true);
                } else {
                    JAcquisitionModePanelComponentPanel.setVisible(false);
                }

                JLiveVideoPanelComponentPanel.setVisible(true);
            }

            if (cbMode.getSelectedItem().toString().equals(mode.getStringValue(modeEnum.ICCS.getValue()))) {
                Common.selectedMode = modeEnum.ICCS;//iccs
                Common.analysisMode = $amode[3];//iccs
                tfTotalFrame.setEditable(false);
                JCalibrationPanelComponentPanel.setVisible(false);
                JICCSPanelComponentPanel.resetParam();
                JICCSPanelComponentPanel.setVisible(true);
                JAcquisitionModePanelComponentPanel.setVisible(false);
                JCumulativeCFPanelComponentPanel.setVisible(false);
                JLiveVideoPanelComponentPanel.setVisible(true);
            }
        };

        public JDirectCaptureComponent() {
            //JPanel
            JPanel AcquisitionPane = new JPanel(new GridLayout(3, 2));
            AcquisitionPane.setBorder(BorderFactory.createTitledBorder(""));

            JPanel PostProcessPane = new JPanel(new GridLayout(2, 2));
            PostProcessPane.setBorder(BorderFactory.createTitledBorder(""));

            JPanel CommandPane = new JPanel(new GridLayout(4, 2));
            CommandPane.setBorder(BorderFactory.createTitledBorder(""));

            //initialize
            tbPixelDimension = new JToggleButton("Live");
            tbPixelDimension.setToolTipText("Open/Close a dialog for ROI selection");
            tbStartStop = new JToggleButton("Start");
            tbStartStop.setToolTipText("Start/Stop recording.");
            tbStartStop.setForeground(Color.blue);
            tbSettings = new JToggleButton("Settings");
            tbSettings.setToolTipText("Open/Close a dialog with camera and FCS settings.");
            btnSave = new JButton("Save");
            btnSave.setToolTipText("Opens acquired imagestack for analysis and saving");
            btnExit = new JButton("Exit");
            btnExit.setForeground(Color.red);
            btnExit.setToolTipText("Turning off camera would switch off its fan and cooler.");
            tfPixelDimension = new JTextField("" + Common.oWidth + " x " + Common.oHeight + "", 8);
            tfPixelDimension.setEditable(false);
            tfPixelBinningSoftware = new JTextField("" + Common.BinXSoft + " x " + Common.BinYSoft + "", 8);
            tfPixelBinningSoftware.setEditable(true);
            tfPixelBinningSoftware.setToolTipText(
                    "Pixel binning used in live evaluation of ACF(s). NOTE: Go to ROI for in-camera binning option.");
            tfCCFdist = new JTextField("" + Common.CCFdistX + " x " + Common.CCFdistY + "", 8);
            tfCCFdist.setEditable(true);
            tfCCFdist.setToolTipText("Pixel shift for correlation curve. 0 x 0 implies autocorrelation");
            tfExposureTime = new JTextField(Double.toString(Common.exposureTime), 8);
            tfExposureTime.setToolTipText(
                    "Set exposure time per frame. NOTE: Upon pressing 'Start' time per frame will update accordingly " +
                            "depending on camera settings.");
            tfExposureTime.setEditable(true);
            tfTotalFrame = new JTextField(Integer.toString(Common.totalFrame), 8);
            tfTotalFrame.setToolTipText("Set total number of frame. Only applicable in acquisition mode.");
            tfTotalFrame.setEditable(false);
            tfTemperature = new JTextField(Integer.toString(Common.temperature) + " " + (char) 186 + " C", 8);
            tfTemperature.setFont(tfTemperature.getFont().deriveFont(Font.BOLD, panelFontSize + 2));
            tfTemperature.setEditable(false);
            cbMode = new JComboBox<>();
            //loop to fill cbMode combobox
            for (int i = 0; i < mode.size(); i++) {
                cbMode.addItem(mode.getStringValue(i));
            }
            //            cbMode.addItem(mode.getStringValue(mode.modeEnum.SINGLECAPTURE.getValue()));
            //            cbMode.addItem(mode.getStringValue(mode.modeEnum.LIVEVIDEO.getValue()));
            //            cbMode.addItem(mode.getStringValue(mode.modeEnum.CALIBRATION.getValue()));
            //            cbMode.addItem(mode.getStringValue(mode.modeEnum.ACQUISITION.getValue()));
            //            cbMode.addItem(mode.getStringValue(mode.modeEnum.ICCS.getValue()));

            Common.selectedMode = modeEnum.getEnum(cbMode.getSelectedItem().toString());

            //Acquisition panel (top panel)
            AcquisitionPane.add(tbPixelDimension);
            AcquisitionPane.add(tfPixelDimension);
            AcquisitionPane.add(new JLabel("Total Frame:"));
            AcquisitionPane.add(tfTotalFrame);
            AcquisitionPane.add(new JLabel("Exposure Time [s]:"));
            AcquisitionPane.add(tfExposureTime);

            //Post process panel (center panel)
            PostProcessPane.add(new JLabel("Pixel Binning:"));
            PostProcessPane.add(tfPixelBinningSoftware);
            PostProcessPane.add(new JLabel("CCF distance:"));
            PostProcessPane.add(tfCCFdist);

            //Command panel (bottom panel)
            CommandPane.add(new JLabel("Mode:"));
            CommandPane.add(cbMode);
            CommandPane.add(tbStartStop);
            CommandPane.add(btnSave);
            CommandPane.add(tbSettings);
            CommandPane.add(btnExit);
            CommandPane.add(new JLabel("Temperature:"));
            CommandPane.add(tfTemperature);

            Container cp = this.getContentPane();
            cp.setLayout(new BorderLayout(1, 1));
            cp.add(AcquisitionPane, BorderLayout.NORTH);
            cp.add(PostProcessPane, BorderLayout.CENTER);
            cp.add(CommandPane, BorderLayout.SOUTH);
            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setTitle(Common.$cameraHeadModel + "/" + Common.$serialNumber + " - " + DCR_VERSION);
            setSize(DCpanelDimX, DCpanelDimY);
            setLocation(new Point(DCpanelPosX, DCpanelPosY));
            setFocusable(true);
            setResizable(false);
            setVisible(false);
            setAlwaysOnTop(false);

            //add listeners
            btnSave.addActionListener(btnSavePressed);
            btnExit.addActionListener(btnExitPressed);
            cbMode.addActionListener(cbModeChanged);
            tfPixelBinningSoftware.getDocument().addDocumentListener(tfBinChanged);
            tfCCFdist.getDocument().addDocumentListener(tfCCFdistChanged);

            ItemListener tbPixelDimensionPressed = (ItemEvent ev) -> {
                if (ev.getStateChange() == ItemEvent.SELECTED) {

                    //Snap a single full frame image and update ROI selector window
                    if (!Common.isAcquisitionRunning) {
                        start_single_capture(true);
                    }

                    DisplayImageObj.toggleDisplay(true, false);
                    JDimensionpanelComponentPanel.setVisible(true);
                    tbPixelDimension.setBorderPainted(true);

                } else {
                    DisplayImageObj.toggleDisplay(false, false);
                    JDimensionpanelComponentPanel.setVisible(false);
                    tbPixelDimension.setBorderPainted(true);
                }
            };

            ItemListener tbSettingsPressed = (ItemEvent ev) -> {
                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    tbSettings.setBorderPainted(true);
                    JSettingspanelComponentPanel.setVisible(true);

                } else {
                    tbSettings.setBorderPainted(true);
                    JSettingspanelComponentPanel.setVisible(false);
                }
            };

            ItemListener tbStartStopPressed = (ItemEvent ev) -> {

                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    // Close all graph windows
                    if (Common.fromImFCSobj1 != null) {
                        Common.fromImFCSobj1.closeWindowsAll();
                    }

                    if (Common.fromImFCSobj2 != null) {
                        Common.fromImFCSobj2.closeWindowsAll();
                    }

                    SystemInfo.explicitGC();

                    //                    tbStartStop.setBorderPainted(true);
                    tbStartStop.setForeground(Color.red);
                    tbStartStop.setText("Stop");
                    cbMode.setEnabled(false);

                    switch (Common.selectedMode) {
                        case SINGLECAPTURE:
                            start_single_capture(false);
                            break;
                        case LIVEVIDEO: //deprecated; CALIBRATION mode essentially do the same with additional CF
                            // analysis
                            start_live_video();
                            break;
                        case CALIBRATION:
                            start_calibration();
                            break;
                        case ACQUISITION:
                            start_acquisition();
                            break;

                        case ICCS:
                            start_iccs_routine();
                            break;
                    }

                } else {

                    tbStartStop.setForeground(Color.blue);
                    tbStartStop.setText("Start");
                    cbMode.setEnabled(true);

                    if (Common.isAcquisitionRunning) {
                        //do something to stop calibration, acquisiton, live, ICCS
                        Common.isStopPressed = true;
                        APIcall.setStopMechanism(Common.isStopPressed);
                    } else {
                        //single scan, do nothing
                    }

                    if (Common.selectedMode == modeEnum.ICCS) {//"ICCS"
                        //reset
                        JICCSPanelComponentPanel.resetFitToggle();
                    }

                }
            };

            // add listener
            tbPixelDimension.addItemListener(tbPixelDimensionPressed);
            tbSettings.addItemListener(tbSettingsPressed);
            tbStartStop.addItemListener(tbStartStopPressed);

        }

        private void SoftwarePixelBinningDialogue() {
            GenericDialog gd = new GenericDialog("Pixel Binning");
            gd.addNumericField("Binning x: ", Common.BinXSoft, 0); // get bin
            gd.addNumericField("Binning y: ", Common.BinYSoft, 0); // get bin
            gd.showDialog();

            boolean changesmade = false;
            boolean needChanges = false;

            if (gd.wasOKed()) {
                int memx = Common.BinXSoft;
                int memy = Common.BinYSoft;
                int tempx = (int) gd.getNextNumber();
                int tempy = (int) gd.getNextNumber();

                //first test
                if (tempx < 1 || tempx > Common.oWidth) {
                    tempx = memx;
                }
                if (tempy < 1 || tempy > Common.oHeight) {
                    tempy = memy;
                }

                //second test
                if (Common.isAcquisitionRunning) {
                    if (Common.lWidth < tempx) {
                        needChanges = true;
                    }
                    if (Common.lHeight < tempy) {
                        needChanges = true;
                    }
                    if (needChanges) {
                        changesmade = checkerBinAndLiveROI(Common.lWidth, Common.lHeight, Common.lLeft, Common.lTop,
                                Common.oWidth, Common.oHeight, Common.CCFdistX, Common.CCFdistY, tempx, tempy,
                                Common.isCCFmode);
                        if (changesmade) {
                            Common.lWidth = tempx;
                            Common.lHeight = tempy;

                            if (Common.isCCFmode) {
                                Common.impRoiLive =
                                        new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);

                                Common.impRoiLive2 =
                                        new Roi(Common.lLeft + Common.CCFdistX - 1, Common.lTop + Common.CCFdistY - 1,
                                                Common.lWidth, Common.lHeight);
                                Common.impRoiLive2.setStrokeColor(Color.RED);
                                Overlay impov = new Overlay(Common.impRoiLive2);
                                Common.imp.setOverlay(impov);
                            } else {
                                Common.impRoiLive =
                                        new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                                Common.impRoiLive.setStrokeColor(Color.GREEN);
                                Common.imp.setRoi(Common.impRoiLive);

                            }

                        } else {
                            tempx = memx;
                            tempy = memy;
                        }
                    }
                }

                Common.BinXSoft = tempx;
                Common.BinYSoft = tempy;
                tfPixelBinningSoftware.setText(
                        Integer.toString(Common.BinXSoft) + " x " + Integer.toString(Common.BinYSoft));
            }
        }

        private void CCFdistDialogue() {
            GenericDialog gd = new GenericDialog("correlation shift distance in pixel");
            gd.addNumericField("Shift x: ", Common.CCFdistX, 0); // get bin
            gd.addNumericField("Shift y: ", Common.CCFdistY, 0); // get bin
            gd.showDialog();

            if (gd.wasOKed()) {
                boolean isChangesMade = false;
                boolean isCCFselectionOK;
                int memx = Common.CCFdistX;
                int memy = Common.CCFdistY;

                int tempx = (int) gd.getNextNumber();
                int tempy = (int) gd.getNextNumber();
                isCCFselectionOK = CCFselectorChecker(Common.oWidth, Common.oHeight, tempx, tempy, Common.BinXSoft,
                        Common.BinYSoft, Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                if (isCCFselectionOK) {
                    Common.CCFdistX = tempx;
                    Common.CCFdistY = tempy;
                    if (memx != Common.CCFdistX || memy != Common.CCFdistY) {
                        isChangesMade = true;
                    }
                } else {
                    Common.CCFdistX = memx;
                    Common.CCFdistY = memy;
                }
                tfCCFdist.setText(Integer.toString(Common.CCFdistX) + " x " + Integer.toString(Common.CCFdistY));

                if (Common.CCFdistX != 0 || Common.CCFdistY != 0) {
                    Common.isCCFmode = true;
                    if (isChangesMade && Common.isAcquisitionRunning && isCCFselectionOK) {
                        Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                        Common.impRoiLive.setStrokeColor(Color.GREEN);
                        Common.imp.setRoi(Common.impRoiLive);

                        Common.impRoiLive2 =
                                new Roi(Common.lLeft + Common.CCFdistX - 1, Common.lTop + Common.CCFdistY - 1,
                                        Common.lWidth, Common.lHeight);
                        Common.impRoiLive2.setStrokeColor(Color.RED);
                        Overlay impov = new Overlay(Common.impRoiLive2);
                        Common.imp.setOverlay(impov);
                    }
                } else {
                    Common.isCCFmode = false;
                    if (isChangesMade && Common.isAcquisitionRunning) {
                        Common.impRoiLive = new Roi(Common.lLeft - 1, Common.lTop - 1, Common.lWidth, Common.lHeight);
                        Common.impRoiLive.setStrokeColor(Color.GREEN);
                        Common.imp.setRoi(Common.impRoiLive);

                        if (Common.imp.getOverlay() != null) {
                            Common.imp.getOverlay().clear();
                            Common.imp.setOverlay(Common.imp.getOverlay());
                        }

                    }
                }
            }

        }

        private int ExitDialogue() {
            int val;
            int result = JOptionPane.showConfirmDialog(null, "Do you wish to save acquisition settings?",
                    "Save Configuration", JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            switch (result) {
                case JOptionPane.YES_OPTION:
                    val = 1;
                    break;
                case JOptionPane.NO_OPTION:
                    val = 2;
                    break;
                case JOptionPane.CANCEL_OPTION:
                    val = 3;
                    break;
                default:
                    val = 4;
                    break;
            }

            return val;
        }

        private int DisplaySavedImageDialogue() {
            int val;
            int result = JOptionPane.showConfirmDialog(null, "Display saved-images in Fiji?", null,
                    JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);
            switch (result) {
                case JOptionPane.YES_OPTION:
                    val = 1;
                    break;
                case JOptionPane.NO_OPTION:
                    val = 2;
                    break;
                case JOptionPane.CANCEL_OPTION:
                    val = 3;
                    break;
                default:
                    val = 4;
                    break;
            }
            return val;
        }

        private boolean SavingSizePerStackDialog(ImagePlus fullimp) {
            double fullimpinbyte = fullimp.getSizeInBytes();
            double fullimpinGB = fullimpinbyte / Math.pow(10, 9);

            int sizeInGB = (int) Math.floor(Common.maximumBytePerStack / Math.pow(10, 9));
            int ret;

            GenericDialog gd = new GenericDialog("Saving a single .tiff or multiple .tiff(s)");
            gd.addMessage("Size of images: " + IJ.d2s(fullimpinGB, 1) + " GB");
            gd.addMessage("Enter value larger than " + IJ.d2s(fullimpinGB, 1) + " GB to save as a single .tiff");
            gd.addNumericField("GB/stack", sizeInGB, 0);
            gd.showDialog();
            if (gd.wasOKed()) {
                ret = (int) gd.getNextNumber();
                if (ret < 2) {
                    ret = 2; //2 GB minimal
                }
                Common.maximumBytePerStack = ret * Math.pow(10, 9);
                return true;
            }
            return false;

        }

        private boolean getListOfStacks(ImagePlus fullimp, ArrayList<ImagePlus> arrayimp) {
            final double max = Common.maximumBytePerStack;
            double fullsizeinbytes = fullimp.getSizeInBytes();
            int width = fullimp.getWidth();
            int height = fullimp.getHeight();
            int size = fullimp.getStackSize();
            int numimages = (int) Math.floor(fullsizeinbytes / (max + 1)) + 1;

            if (numimages == 1) {
                arrayimp.add(new ImagePlus(fullimp.getTitle(), fullimp.getImageStack()));
                return true;
            }

            //splitting TODO: some user finds out operation can be slow in their CPU. Find faster way; currenctly
            // splitting can be disabled by setting Common.maximumBytePerStack to a large number say 16 GB
            int numframeperstack = (int) Math.floor(max / (2 * width * height));
            for (int i = 0; i < numimages; i++) {
                printlog("i: " + i);
                int start = numframeperstack * i + 1;
                int stop;
                if (i < (numimages - 1)) {
                    stop = numframeperstack * (i + 1);
                } else {
                    stop = size;
                }
                printlog("start: " + start + ", stop: " + stop);
                ImageStack ims = new ImageStack(width, height);
                for (int f = start; f <= stop; f++) {
                    ShortProcessor ip = new ShortProcessor(width, height);
                    for (int y = 0; y < height; y++) {
                        for (int x = 0; x < width; x++) {
                            ip.putPixel(x, y, fullimp.getImageStack().getProcessor(f).get(x, y));
                            //                        ip.putPixel(x, y, (int) fullimp.getImageStack().getVoxel(x, y,
                            //                        f - 1));
                        }
                    }
                    ims.addSlice(ip);
                }
                arrayimp.add(new ImagePlus("X_" + (i + 1) + "_", ims));
            }
            return true;
        }

        private int runSaveMechanism(boolean proceed, ImagePlus imp, int dispImage) {

            int retval = 0;//0-saveOK ; 1-saving in progress
            if (!proceed) {
                retval = 1;
                return retval;
            }

            Common.isSaveDone = false;
            SwingWorker<Boolean, Void> worker;
            worker = new SwingWorker<Boolean, Void>() {

                @Override
                protected Boolean doInBackground() throws Exception {

                    //split image if necessary
                    ArrayList<ImagePlus> res = new ArrayList<>();
                    getListOfStacks(imp, res);
                    if (dispImage == 1) { //whteher to display or not to display saved image in Fiji
                        for (int i = 0; i < res.size(); i++) {
                            res.get(i).show();
                        }
                    }

                    //directory and selection
                    String tiffFN;
                    if (Common.$impSavedOR == null) {
                        tiffFN = "Rename me";
                    } else {
                        tiffFN = Common.$impSavedOR;
                    }

                    int dotind = tiffFN.lastIndexOf('.');
                    if (dotind != -1) {
                        tiffFN = tiffFN.substring(0, dotind);
                    }

                    JFileChooser fc;
                    if (Common.$impSavingFolderPath == null) {
                        fc = new JFileChooser(System.getProperty("user.home"));
                    } else {
                        fc = new JFileChooser(Common.$impSavingFolderPath);
                    }

                    fc.setSelectedFile(new File(tiffFN));
                    fc.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    int returnVal = fc.showSaveDialog(null);

                    if (returnVal == JFileChooser.APPROVE_OPTION) {

                        Common.$impSavedOR = getFileName(fc);//Check filename to prevent file overwritting
                        Common.$impSavingFolderPath = fc.getSelectedFile().getParent();
                        Common.$impPathOR = Common.$impSavingFolderPath + "\\" +
                                Common.$impSavedOR;//fc.getSelectedFile().getAbsolutePath();

                        //Saving tif stacks
                        if (res.size() == 1) {
                            IJ.saveAsTiff(res.get(0), Common.$impPathOR);
                            IJ.log("Tiff file saved: " + Common.$impSavingFolderPath);
                        } else {
                            for (int i = 0; i < res.size(); i++) {
                                Common.$impPathOR = Common.$impPathOR + "_X" + (i + 1);
                                IJ.saveAsTiff(res.get(i), Common.$impPathOR);
                                IJ.log("Tiff file saved " + (i + 1));
                            }
                        }

                        //Saving excel file (timer and metadata)
                        if (IsSaveExcel) {
                            String xlsxFN = Common.$impSavedOR + "_metadata.xlsx";
                            String parentPath = Common.$impSavingFolderPath;
                            JFileChooser fcexcel = new JFileChooser(parentPath);
                            fcexcel.setSelectedFile(new File(parentPath + "\\" + xlsxFN));
                            APIcall.writeExcel(fcexcel.getSelectedFile(), "Failed to write excel", true);
                        }

                        //Saving time-tagged file
                        if (IsSaveJson && Common.ttsObj.isDataForSavingAvailable()) {
                            String jsonFN = Common.$impSavedOR + "_timetagged.txt";
                            String parentPath = Common.$impSavingFolderPath;
                            try {
                                Common.ttsObj.saveAsJson(parentPath + "\\" + jsonFN);
                            } catch (IOException e) {
                                IJ.log("Json file saving issue: " + e);
                            }
                        }

                    } else {
                        JOptionPane.showMessageDialog(null, "File not saved");
                    }

                    return true;
                }

                @Override
                protected void done() {
                    try {
                        Common.isSaveDone = get();
                    } catch (InterruptedException ex) {
                        Logger.getLogger(DirectCapturePanel.class.getName()).log(Level.SEVERE, null, ex);
                    } catch (ExecutionException ex) {
                        Logger.getLogger(DirectCapturePanel.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            };
            worker.execute();
            return retval;
        }

        private String getFileName(JFileChooser fc) {
            //Rename filename if needed to prevent overwriting (Increment numeric suffix to the exisitng filename)
            String fileName = fc.getSelectedFile().getName();
            File fileNameToCheck = new File(fc.getSelectedFile().getAbsoluteFile().toString() + ".tif");
            if (fileNameToCheck.exists()) {
                int lastInc = 0;
                if (Character.isDigit(fileName.charAt(fileName.length() - 1))) {
                    //increment suffix
                    int underscoreind = fileName.lastIndexOf("_");
                    if (underscoreind != -1) {
                        try {
                            lastInc = Integer.parseInt(fileName.substring(underscoreind + 1));
                        } catch (NumberFormatException nfe) {
                            lastInc = 665;
                        }
                        fileName = fileName.substring(0, underscoreind);
                    }
                }
                fileName = fileName + "_" + String.valueOf(lastInc + 1);
            }
            return fileName;
        }

    }

    public class JDimensionpanelComponent extends JFrame {

        final int DimpanelPosX = 700;
        // control panel, "ImFCS", position and dimensions
        final int DimpanelPosY = 125;
        final int DimpanelDimX = 405;
        final int DimpanelDimY = 225;

        //Control flow
        public boolean TriggerDimTfKeyListener = true;

        // Private variables of the GUI components
        JTextArea tArea;
        ActionListener btnFullFramePressed = (ActionEvent event) -> {
            Common.oWidth = Common.MAXpixelwidth / Common.inCameraBinning;
            tfoWidth.setText(Integer.toString(Common.oWidth));
            Common.oHeight = Common.MAXpixelheight / Common.inCameraBinning;
            tfoHeight.setText(Integer.toString(Common.oHeight));
            Common.oLeft = 1;
            tfoLeft.setText(Integer.toString(Common.oLeft));
            Common.oTop = 1;
            tfoTop.setText(Integer.toString(Common.oTop));
            Common.oRight = Common.MAXpixelwidth / Common.inCameraBinning;
            tfoRight.setText(Integer.toString(Common.oRight));
            Common.oBottom = Common.MAXpixelheight / Common.inCameraBinning;
            tfoBottom.setText(Integer.toString(Common.oBottom));
            DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth, Common.oHeight);
            tfPixelDimension.setText(Integer.toString(Common.oWidth) + " x " + Integer.toString(Common.oHeight));
            setSizeAandSizeB(Common.oWidth, Common.oHeight, Common.maxE, Common.minPI, Common.maxPI);
            if (Common.plotInterval > retMaxAllowablePlotInterval(Common.size_a, Common.size_b)) {
                if (retMaxAllowablePlotInterval(Common.size_a, Common.size_b) > 500) {
                    Common.plotInterval = 500;
                } else {
                    Common.plotInterval = retMaxAllowablePlotInterval(Common.size_a, Common.size_b);
                }
                tfPlotInterval.setText(Integer.toString(Common.plotInterval));
            }
        };
        ActionListener rbCustomROIChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (rbCustomROI.isSelected() == true) {
                    tfoLeft.setEditable(true);
                    tfoRight.setEditable(true);
                    tfoTop.setEditable(true);
                    tfoBottom.setEditable(true);
                    tfoWidth.setEditable(false);
                    tfoHeight.setEditable(false);

                } else {
                    tfoLeft.setEditable(false);
                    tfoRight.setEditable(false);
                    tfoTop.setEditable(false);
                    tfoBottom.setEditable(false);
                    tfoWidth.setEditable(true);
                    tfoHeight.setEditable(true);
                }
            }
        };
        // update when tfoWidth or tfoHeight changed
        DocumentListener dimTfoWHchanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!rbCustomROI.isSelected() && TriggerDimTfKeyListener) {
                    boolean redrawSelection = UpdateROIwh(isHamamatsu);
                    if (redrawSelection) {
                        DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                                Common.oHeight);
                    }
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!rbCustomROI.isSelected() && TriggerDimTfKeyListener) {
                    boolean redrawSelection = UpdateROIwh(isHamamatsu);
                    if (redrawSelection) {
                        DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                                Common.oHeight);
                    }
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!rbCustomROI.isSelected() && TriggerDimTfKeyListener) {
                    boolean redrawSelection = UpdateROIwh(isHamamatsu);
                    if (redrawSelection) {
                        DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                                Common.oHeight);
                    }
                }
            }
        };
        KeyListener dimTfoWHCursorMove = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyInt = e.getKeyCode();

                if (e.getSource() == tfoWidth) {
                    if (keyInt == KeyEvent.VK_RIGHT) {
                        tfoHeight.requestFocus();
                        tfoHeight.setCaretPosition(tfoHeight.getText().length());
                        UpdateDimTextField();
                    }
                }

                if (e.getSource() == tfoHeight) {
                    if (keyInt == KeyEvent.VK_LEFT) {
                        tfoWidth.requestFocus();
                        tfoWidth.setCaretPosition(tfoWidth.getText().length());
                        UpdateDimTextField();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyInt = e.getKeyCode();

                if (e.getSource() == tfoWidth) {
                    //                    tfoWidth.setText(Integer.toString(Common.oWidth));
                }

                if (e.getSource() == tfoHeight) {
                    //                    tfoHeight.setText(Integer.toString(Common.oHeight));
                }
            }
        };
        DocumentListener dimTfoLRTBchanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (rbCustomROI.isSelected() && TriggerDimTfKeyListener) {
                    boolean redrawSelection = false;
                    Document doc = e.getDocument();
                    if (doc == tfoLeft.getDocument()) {
                        redrawSelection = UpdateROIlrtb("l", isHamamatsu);
                    }
                    if (doc == tfoRight.getDocument()) {
                        redrawSelection = UpdateROIlrtb("r", isHamamatsu);
                    }
                    if (doc == tfoTop.getDocument()) {
                        redrawSelection = UpdateROIlrtb("t", isHamamatsu);
                    }
                    if (doc == tfoBottom.getDocument()) {
                        redrawSelection = UpdateROIlrtb("b", isHamamatsu);
                    }
                    if (redrawSelection) {
                        DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                                Common.oHeight);
                    }
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (rbCustomROI.isSelected() && TriggerDimTfKeyListener) {
                    boolean redrawSelection = false;
                    Document doc = e.getDocument();
                    if (doc == tfoLeft.getDocument()) {
                        redrawSelection = UpdateROIlrtb("l", isHamamatsu);
                    }
                    if (doc == tfoRight.getDocument()) {
                        redrawSelection = UpdateROIlrtb("r", isHamamatsu);
                    }
                    if (doc == tfoTop.getDocument()) {
                        redrawSelection = UpdateROIlrtb("t", isHamamatsu);
                    }
                    if (doc == tfoBottom.getDocument()) {
                        redrawSelection = UpdateROIlrtb("b", isHamamatsu);
                    }
                    if (redrawSelection) {
                        DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                                Common.oHeight);
                    }
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (rbCustomROI.isSelected() && TriggerDimTfKeyListener) {
                    boolean redrawSelection = false;
                    Document doc = e.getDocument();
                    if (doc == tfoLeft.getDocument()) {
                        redrawSelection = UpdateROIlrtb("l", isHamamatsu);
                    }
                    if (doc == tfoRight.getDocument()) {
                        redrawSelection = UpdateROIlrtb("r", isHamamatsu);
                    }
                    if (doc == tfoTop.getDocument()) {
                        redrawSelection = UpdateROIlrtb("t", isHamamatsu);
                    }
                    if (doc == tfoBottom.getDocument()) {
                        redrawSelection = UpdateROIlrtb("b", isHamamatsu);
                    }
                    if (redrawSelection) {
                        DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                                Common.oHeight);
                    }
                }
            }
        };
        KeyListener dimTfoLRTBCursorMove = new KeyListener() {
            @Override
            public void keyTyped(KeyEvent e) {
            }

            @Override
            public void keyPressed(KeyEvent e) {
                int keyInt = e.getKeyCode();

                if (e.getSource() == tfoLeft) {
                    if (keyInt == KeyEvent.VK_RIGHT) {
                        tfoRight.requestFocus();
                        tfoRight.setCaretPosition(tfoRight.getText().length());
                        UpdateDimTextField();
                    }
                    if (keyInt == KeyEvent.VK_DOWN) {
                        tfoTop.requestFocus();
                        tfoTop.setCaretPosition(tfoTop.getText().length());
                        UpdateDimTextField();
                    }
                }

                if (e.getSource() == tfoRight) {
                    if (keyInt == KeyEvent.VK_LEFT) {
                        tfoLeft.requestFocus();
                        tfoLeft.setCaretPosition(tfoLeft.getText().length());
                        UpdateDimTextField();
                    }
                    if (keyInt == KeyEvent.VK_DOWN) {
                        tfoBottom.requestFocus();
                        tfoBottom.setCaretPosition(tfoBottom.getText().length());
                        UpdateDimTextField();
                    }
                }

                if (e.getSource() == tfoTop) {
                    if (keyInt == KeyEvent.VK_UP) {
                        tfoLeft.requestFocus();
                        tfoLeft.setCaretPosition(tfoLeft.getText().length());
                        UpdateDimTextField();
                    }
                    if (keyInt == KeyEvent.VK_RIGHT) {
                        tfoBottom.requestFocus();
                        tfoBottom.setCaretPosition(tfoBottom.getText().length());
                        UpdateDimTextField();
                    }
                }

                if (e.getSource() == tfoBottom) {
                    if (keyInt == KeyEvent.VK_UP) {
                        tfoRight.requestFocus();
                        tfoRight.setCaretPosition(tfoRight.getText().length());
                        UpdateDimTextField();
                    }
                    if (keyInt == KeyEvent.VK_LEFT) {
                        tfoTop.requestFocus();
                        tfoTop.setCaretPosition(tfoTop.getText().length());
                        UpdateDimTextField();
                    }
                }
            }

            @Override
            public void keyReleased(KeyEvent e) {
                int keyInt = e.getKeyCode();

                if (e.getSource() == tfoLeft) {
                }

                if (e.getSource() == tfoRight) {
                }

                if (e.getSource() == tfoTop) {
                }

                if (e.getSource() == tfoBottom) {
                }

            }
        };
        MouseListener DimoTfMouseUsed = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
                UpdateDimTextField();
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
                UpdateDimTextField();
            }
        };
        ActionListener cbPixelEncodingChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String selectedPixEncd = cbPixelEncoding.getSelectedItem().toString();
                for (int i = 0; i < cameraConstant.Common_SONA.listPixelEncoding.length; i++) {
                    if (selectedPixEncd.equals(cameraConstant.Common_SONA.listPixelEncoding[i])) {
                        cameraConstant.Common_SONA.PixelEncoding = i;
                    }
                }
            }
        };
        ActionListener cbInCameraBinningChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                String selectedInBin = cbInCameraBinning.getSelectedItem().toString();
                Common.inCameraBinning = Integer.parseInt(String.valueOf(selectedInBin.charAt(0)));
                int index = DisplayImageObj.getIndexOfBin(DisplayImageObj.aListArrayBuffer, Common.inCameraBinning);

                if (Common.isCropMode == 0) {
                    int[] temp = getCenterCoordinate(Common.minHeight, Common.minHeight,
                            Common.MAXpixelwidth / Common.inCameraBinning,
                            Common.MAXpixelheight / Common.inCameraBinning);
                    Common.oWidth = temp[0];
                    Common.oHeight = temp[1];
                    Common.oLeft = temp[2];
                    Common.oTop = temp[3];
                    Common.oRight = Common.oLeft + Common.oWidth - 1;
                    Common.oBottom = Common.oTop + Common.oHeight - 1;

                } else {
                    setODimtoCDim(Common.inCameraBinning);
                }
                DisplayImageObj.updateImage(DisplayImageObj.aListArrayBuffer.get(1).get(index), Common.inCameraBinning,
                        Common.MAXpixelwidth, Common.MAXpixelheight, Common.isCropMode);
                DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth, Common.oHeight);
                JDimensionpanelComponentPanel.TriggerDimTfKeyListener = false;
                UpdateDimTextField();
                JDimensionpanelComponentPanel.TriggerDimTfKeyListener = true;

            }
        };
        ActionListener rbCropModeChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (rbCropMode.isSelected() == true) {
                    JCropModePanelComponentPanel.setVisible(true);
                    Common.isCropMode = 1;
                    btnFullFrame.setEnabled(false);

                    setODimtoCDim(Common.inCameraBinning);
                    DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                            Common.oHeight);

                    if (rbCustomROI.isSelected() == true) {
                        tfoLeft.setEditable(false);
                        tfoRight.setEditable(false);
                        tfoTop.setEditable(false);
                        tfoBottom.setEditable(false);
                    } else {
                        tfoWidth.setEditable(false);
                        tfoHeight.setEditable(false);
                    }
                } else {
                    JCropModePanelComponentPanel.setVisible(false);
                    Common.isCropMode = 0;
                    btnFullFrame.setEnabled(true);
                    if (rbCustomROI.isSelected() == true) {
                        tfoLeft.setEditable(true);
                        tfoRight.setEditable(true);
                        tfoTop.setEditable(true);
                        tfoBottom.setEditable(true);
                    } else {
                        tfoWidth.setEditable(true);
                        tfoHeight.setEditable(true);
                    }
                }

                if (rbCropMode.isSelected() == true) {
                    IJ.log("1345 unimplemented");
                } else {
                    //Update DisplayImage
                    int index = DisplayImageObj.getIndexOfBin(DisplayImageObj.aListArrayBuffer, Common.inCameraBinning);
                    int[] temp = getCenterCoordinate(Common.minHeight, Common.minHeight,
                            Common.MAXpixelwidth / Common.inCameraBinning,
                            Common.MAXpixelheight / Common.inCameraBinning);
                    Common.oWidth = temp[0];
                    Common.oHeight = temp[1];
                    Common.oLeft = temp[2];
                    Common.oTop = temp[3];
                    Common.oRight = Common.oLeft + Common.oWidth - 1;
                    Common.oBottom = Common.oTop + Common.oHeight - 1;
                    DisplayImageObj.updateImage(DisplayImageObj.aListArrayBuffer.get(1).get(index),
                            Common.inCameraBinning, Common.MAXpixelwidth, Common.MAXpixelheight, Common.isCropMode);
                    DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                            Common.oHeight);
                    JDimensionpanelComponentPanel.TriggerDimTfKeyListener = false;
                    UpdateDimTextField();
                    JDimensionpanelComponentPanel.TriggerDimTfKeyListener = true;
                }

            }
        };

        public JDimensionpanelComponent() {
            // JPanel for the text fields
            JPanel tfPanelCenterDim = new JPanel(new GridLayout(1, 5, 10, 2));
            tfPanelCenterDim.setBorder(BorderFactory.createTitledBorder("Center Dimension: "));

            JPanel tfPanelCustomDim = new JPanel(new GridLayout(3, 5, 10, 2));
            tfPanelCustomDim.setBorder(BorderFactory.createTitledBorder("Custom Dimension: "));

            JPanel tfPanelPhysicalBin = new JPanel(new GridLayout(1, 5, 10, 2));
            tfPanelPhysicalBin.setBorder(BorderFactory.createTitledBorder("In-camera Pixel Binning: "));

            //initialize
            tfoWidth = new JTextField(Integer.toString(Common.oWidth), 8);
            tfoWidth.setToolTipText(
                    "Width of centralized ROI is set and retrieved in units of super-pixels Therefore, when binning " +
                            "is in use, the Width value will always indicate the number of data pixels that each row " +
                            "of the " +
                            "image data contains and not the number of pixels read off the sensor NOTE: de-select " +
                            "'Custom' " + "to automatically centralized ROI.");
            tfoHeight = new JTextField(Integer.toString(Common.oHeight), 8);
            tfoHeight.setToolTipText(
                    "Width of centralized ROI is set and retrieved in units of super-pixels Therefore, when binning " +
                            "is in use, the Width value will always indicate the number of data pixels that each row " +
                            "of the " +
                            "image data contains and not the number of pixels read off the sensor NOTE: de-select " +
                            "'Custom' " + "to automatically centralized ROI.");
            tfoLeft = new JTextField(Integer.toString(Common.oLeft), 8);
            tfoLeft.setToolTipText(
                    "Coordinates are specified in units of super-pixels not sensor pixels being read off (index start" +
                            " at 1). NOTE: select 'Custom' to enable customize ROI position.");
            tfoTop = new JTextField(Integer.toString(Common.oTop), 8);
            tfoTop.setToolTipText(
                    "Coordinates are specified in units of super-pixels not sensor pixels being read off (index start" +
                            " at 1). NOTE: select 'Custom' to enable customize ROI position.");
            tfoRight = new JTextField(Integer.toString(Common.oRight), 8);
            tfoRight.setToolTipText(
                    "Coordinates are specified in units of super-pixels not sensor pixels being read off (index start" +
                            " at 1). NOTE: select 'Custom' to enable customize ROI position.");
            tfoBottom = new JTextField(Integer.toString(Common.oBottom), 8);
            tfoBottom.setToolTipText(
                    "Coordinates are specified in units of super-pixels not sensor pixels being read off (index start" +
                            " at 1). NOTE: select 'Custom' to enable customize ROI position.");
            rbCustomROI = new JRadioButton("Custom");
            rbCustomROI.setToolTipText(
                    "Select customiziable ROI or automatically cenralized ROI. NOTE: one could draw ROI directly from" +
                            " image window.");
            btnFullFrame = new JButton("Full");
            btnFullFrame.setToolTipText("Setting maximum allowable ROI.");

            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                rbCropMode = new JRadioButton("Crop Mode");
                rbCropMode.setSelected(Common.isCropMode == 1);
            }

            switch ($camera) {
                case "DU860_BV":
                    cbInCameraBinning = new JComboBox<>();
                    cbInCameraBinning.setToolTipText(
                            "Configure the amount of binning in each direction. Achieved by combining multiple sensor" +
                                    " pixels into a single data pixel by binning the values from each sensor pixel " +
                                    "together.");
                    cbInCameraBinning.addItem("1 x 1");
                    cbInCameraBinning.addItem("2 x 2");
                    cbInCameraBinning.addItem("3 x 3");
                    cbInCameraBinning.addItem("4 x 4");
                    cbInCameraBinning.addItem("8 x 8");
                    cbInCameraBinning.setSelectedIndex(Common.inCameraBinning - 1);
                    break;
                case "DU888_BV":
                    cbInCameraBinning = new JComboBox<>();
                    cbInCameraBinning.setToolTipText(
                            "Configure the amount of binning in each direction. Achieved by combining multiple sensor" +
                                    " pixels into a single data pixel by binning the values from each sensor pixel " +
                                    "together.");
                    cbInCameraBinning.addItem("1 x 1");
                    cbInCameraBinning.addItem("2 x 2");
                    cbInCameraBinning.addItem("3 x 3");
                    cbInCameraBinning.addItem("4 x 4");
                    cbInCameraBinning.addItem("8 x 8");
                    cbInCameraBinning.setSelectedIndex(Common.inCameraBinning - 1);
                    break;
                case "DU897_BV":
                    cbInCameraBinning = new JComboBox<>();
                    cbInCameraBinning.setToolTipText(
                            "Configure the amount of binning in each direction. Achieved by combining multiple sensor" +
                                    " pixels into a single data pixel by binning the values from each sensor pixel " +
                                    "together.");
                    cbInCameraBinning.addItem("1 x 1");
                    cbInCameraBinning.addItem("2 x 2");
                    cbInCameraBinning.addItem("3 x 3");
                    cbInCameraBinning.addItem("4 x 4");
                    cbInCameraBinning.addItem("8 x 8");
                    cbInCameraBinning.setSelectedIndex(Common.inCameraBinning - 1);
                    break;
                case "SONA-4BV11":
                    cbPixelEncoding = new JComboBox<>();
                    cbPixelEncoding.setToolTipText("‘12-bit (low noise)’ or ‘16-bit (low noise & high well capacity)’");
                    for (int i = 0; i < cameraConstant.Common_SONA.listPixelEncoding.length; i++) {
                        cbPixelEncoding.addItem(cameraConstant.Common_SONA.listPixelEncoding[i]);
                    }
                    cbPixelEncoding.setSelectedIndex(cameraConstant.Common_SONA.PixelEncoding);

                    cbInCameraBinning = new JComboBox<>();
                    cbInCameraBinning.setToolTipText(
                            "Configure the amount of binning in each direction. Achieved by combining multiple sensor" +
                                    " pixels into a single data pixel by binning the values from each sensor pixel " +
                                    "together.");
                    cbInCameraBinning.addItem("1 x 1");
                    cbInCameraBinning.addItem("2 x 2");
                    cbInCameraBinning.addItem("3 x 3");
                    cbInCameraBinning.addItem("4 x 4");
                    cbInCameraBinning.addItem("8 x 8");
                    cbInCameraBinning.setSelectedIndex(Common.inCameraBinning - 1);
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    cbInCameraBinning = new JComboBox<>();
                    cbInCameraBinning.setToolTipText(
                            "Configure the amount of binning in each direction. Achieved by combining multiple sensor" +
                                    " pixels into a single data pixel by binning the values from each sensor pixel " +
                                    "together.");
                    cbInCameraBinning.addItem("1 x 1");
                    cbInCameraBinning.addItem("2 x 2");
                    cbInCameraBinning.addItem("4 x 4");
                    cbInCameraBinning.setSelectedIndex(Common.inCameraBinning - 1);
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    cbInCameraBinning = new JComboBox<>();
                    cbInCameraBinning.setToolTipText(
                            "Configure the amount of binning in each direction. Achieved by combining multiple sensor" +
                                    " pixels into a single data pixel by binning the values from each sensor pixel " +
                                    "together.");
                    cbInCameraBinning.addItem("1 x 1");
                    cbInCameraBinning.addItem("2 x 2");
                    cbInCameraBinning.addItem("4 x 4");
                    cbInCameraBinning.setSelectedIndex(Common.inCameraBinning - 1);
                    break;
            }

            //tfPanelCenterDim
            //row1
            tfPanelCenterDim.add(new JLabel("Width: "));
            tfPanelCenterDim.add(tfoWidth);
            tfPanelCenterDim.add(new JLabel("Height: "));
            tfPanelCenterDim.add(tfoHeight);
            tfPanelCenterDim.add(btnFullFrame);

            //tfPanelCustomDim
            //row1
            tfPanelCustomDim.add(rbCustomROI);
            rbCustomROI.setSelected(false);
            tfPanelCustomDim.add(new JLabel(""));
            tfPanelCustomDim.add(new JLabel(""));
            tfPanelCustomDim.add(new JLabel(""));
            tfPanelCustomDim.add(new JLabel(""));

            //row2
            tfPanelCustomDim.add(new JLabel("Left: "));
            tfPanelCustomDim.add(tfoLeft);
            tfoLeft.setEditable(false);
            tfPanelCustomDim.add(new JLabel(""));
            tfPanelCustomDim.add(new JLabel("Right: "));
            tfPanelCustomDim.add(tfoRight);
            tfoRight.setEditable(false);

            //row3
            tfPanelCustomDim.add(new JLabel("Top: "));
            tfPanelCustomDim.add(tfoTop);
            tfoTop.setEditable(false);
            tfPanelCustomDim.add(new JLabel(""));
            tfPanelCustomDim.add(new JLabel("Bottom: "));
            tfPanelCustomDim.add(tfoBottom);
            tfoBottom.setEditable(false);

            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                tfPanelPhysicalBin.add(new JLabel("Binning: "));
                tfPanelPhysicalBin.add(cbInCameraBinning);
                tfPanelPhysicalBin.add(new JLabel(""));
                tfPanelPhysicalBin.add(new JLabel("Crop Mode:"));
                tfPanelPhysicalBin.add(rbCropMode);
            }

            switch ($camera) {
                case "SONA-4BV11":
                    tfPanelPhysicalBin.add(new JLabel("Pixel Encoding: "));
                    tfPanelPhysicalBin.add(cbPixelEncoding);
                    tfPanelPhysicalBin.add(new JLabel(""));
                    tfPanelPhysicalBin.add(new JLabel("Binning: "));
                    tfPanelPhysicalBin.add(cbInCameraBinning);
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    tfPanelPhysicalBin.add(new JLabel("Binning: "));
                    tfPanelPhysicalBin.add(cbInCameraBinning);
                    tfPanelPhysicalBin.add(new JLabel(""));
                    tfPanelPhysicalBin.add(new JLabel(""));
                    tfPanelPhysicalBin.add(new JLabel(""));
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    tfPanelPhysicalBin.add(new JLabel("Binning: "));
                    tfPanelPhysicalBin.add(cbInCameraBinning);
                    tfPanelPhysicalBin.add(new JLabel(""));
                    tfPanelPhysicalBin.add(new JLabel(""));
                    tfPanelPhysicalBin.add(new JLabel(""));
                    break;
            }

            btnFullFrame.addActionListener(btnFullFramePressed);
            rbCustomROI.addActionListener(rbCustomROIChanged);
            tfoWidth.getDocument().addDocumentListener(dimTfoWHchanged);
            tfoWidth.addKeyListener(dimTfoWHCursorMove);
            tfoHeight.getDocument().addDocumentListener(dimTfoWHchanged);
            tfoHeight.addKeyListener(dimTfoWHCursorMove);
            tfoLeft.getDocument().addDocumentListener(dimTfoLRTBchanged);
            tfoLeft.addKeyListener(dimTfoLRTBCursorMove);
            tfoRight.getDocument().addDocumentListener(dimTfoLRTBchanged);
            tfoRight.addKeyListener(dimTfoLRTBCursorMove);
            tfoTop.getDocument().addDocumentListener(dimTfoLRTBchanged);
            tfoTop.addKeyListener(dimTfoLRTBCursorMove);
            tfoBottom.getDocument().addDocumentListener(dimTfoLRTBchanged);
            tfoBottom.addKeyListener(dimTfoLRTBCursorMove);
            tfoWidth.addMouseListener(DimoTfMouseUsed);
            tfoHeight.addMouseListener(DimoTfMouseUsed);
            tfoLeft.addMouseListener(DimoTfMouseUsed);
            tfoRight.addMouseListener(DimoTfMouseUsed);
            tfoTop.addMouseListener(DimoTfMouseUsed);
            tfoBottom.addMouseListener(DimoTfMouseUsed);

            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                cbInCameraBinning.addActionListener(cbInCameraBinningChanged);
                rbCropMode.addActionListener(rbCropModeChanged);
            }
            switch ($camera) {
                case "SONA-4BV11":
                    cbPixelEncoding.addActionListener(cbPixelEncodingChanged);
                    cbInCameraBinning.addActionListener(cbInCameraBinningChanged);
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    cbInCameraBinning.addActionListener(cbInCameraBinningChanged);
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    cbInCameraBinning.addActionListener(cbInCameraBinningChanged);
                    break;

            }

            // Setup the content-pane of JFrame in BorderLayout
            Container cp = this.getContentPane();
            cp.setLayout(new BorderLayout(5, 5));
            cp.add(tfPanelCenterDim, BorderLayout.NORTH);
            cp.add(tfPanelCustomDim, BorderLayout.CENTER);
            cp.add(tfPanelPhysicalBin, BorderLayout.SOUTH);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setUndecorated(false);
            addWindowListener(getWindowAdapter());
            setTitle("ROI");
            switch ($camera) {
                case "DU860_BV":
                    setSize(DimpanelDimX, DimpanelDimY);
                    break;
                case "DU888_BV":
                    setSize(DimpanelDimX, DimpanelDimY);
                    break;
                case "DU897_BV":
                    setSize(DimpanelDimX, DimpanelDimY);
                    break;
                case "SONA-4BV11":
                    setSize(DimpanelDimX, DimpanelDimY);
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    setSize(DimpanelDimX, DimpanelDimY);
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    setSize(DimpanelDimX, DimpanelDimY);
                    break;
            }

            setLocation(new Point(DimpanelPosX, DimpanelPosY));
            setResizable(false);
            setVisible(false);
        }

        private WindowAdapter getWindowAdapter() {
            return new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {//overrode to show message
                    super.windowClosing(we);
                    JOptionPane.showMessageDialog(we.getComponent(), "Toggle off ROI on main panel to hide panel.");
                }

                @Override
                public void windowIconified(WindowEvent we) {
                    setState(JFrame.NORMAL);
                    JOptionPane.showMessageDialog(we.getComponent(), "Toggle off ROI on main panel to hide panel.");
                }
            };
        }

        private void setODimtoCDim(int bin) {
            // Tmapping based on top-left corner
            // let full full frame eg. 1024 x 1024
            // mapping into condensed window eg. 512 x 512 for bin2; 256 x 256 for bin 4

            Common.oWidth = (int) Math.floor(Common.cWidth / Common.inCameraBinning);
            Common.oHeight = (int) Math.floor(Common.cHeight / Common.inCameraBinning);

            Common.oLeft = Common.cLeft;
            Common.oTop = Common.cTop;
            Common.oLeft = (int) Math.floor((Common.oLeft - 1) / Common.inCameraBinning) + 1;
            Common.oTop = (int) Math.floor((Common.oTop - 1) / Common.inCameraBinning) + 1;

            Common.oRight = Common.oLeft + Common.oWidth - 1;
            Common.oBottom = Common.oTop + Common.oHeight - 1;

            TriggerDimTfKeyListener = false;
            setSizeAandSizeB(Common.oWidth, Common.oHeight, Common.maxE, Common.minPI, Common.maxPI);
            if (Common.plotInterval > retMaxAllowablePlotInterval(Common.size_a, Common.size_b)) {
                if (retMaxAllowablePlotInterval(Common.size_a, Common.size_b) > 500) {
                    Common.plotInterval = 500;
                } else {
                    Common.plotInterval = retMaxAllowablePlotInterval(Common.size_a, Common.size_b);
                }
                tfPlotInterval.setText(Integer.toString(Common.plotInterval));
            }
            UpdateDimTextField();
            TriggerDimTfKeyListener = true;
        }

    }

    public class JSettingspanelComponent extends JFrame {

        final int SettingpanelPosX = 425;
        // control panel, "ImFCS", position and dimensions
        final int SettingpanelPosY = 410;
        JPanel CameraPane;
        ActionListener btnTemperaturePressed = (ActionEvent event) -> {
            if (Common.isAcquisitionRunning) {
                JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to finish");
            } else {
                TemperatureDialogue();
            }

        };
        ActionListener btnFanPressed = (ActionEvent event) -> {
            if (Common.isAcquisitionRunning) {
                JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to finish");
            } else {
                FanDialogue();
            }

        };
        ActionListener btnEmGainPressed = (ActionEvent event) -> {
            if (Common.isAcquisitionRunning) {
                JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to finish");
            } else {
                EmGainDialogue();
            }

        };
        ActionListener cbVspeedChanged = (ActionEvent event) -> {
            Common.iVSpeed = cbVspeed.getSelectedIndex();
        };
        ActionListener cbVSAmpChanged = (ActionEvent event) -> {
            Common.iVSamp = cbVSAmp.getSelectedIndex();
        };
        ActionListener cbHspeedChanged = (ActionEvent event) -> {
            Common.iHSpeed = cbHspeed.getSelectedIndex();
        };
        ActionListener cbPreAmpGainChanged = (ActionEvent event) -> {
            Common.iPreamp = cbPreAmpGain.getSelectedIndex();
        };
        ItemListener tbMechanicalShutterPressed = (ItemEvent ev) -> {
            if (Common.isAcquisitionRunning) {
                JOptionPane.showMessageDialog(null, "Please press stop or wait for acquisition to finish");
                tbMechanicalShutter.setSelected(false);
            } else {
                if (ev.getStateChange() == ItemEvent.SELECTED) {
                    tbMechanicalShutter.setText("Shutter On");
                    APIcall.ShutterControl(false);

                } else {
                    tbMechanicalShutter.setText("Shutter Off");
                    APIcall.ShutterControl(true);
                }
            }

        };
        ActionListener cbBleacCorrectionChanged = (ActionEvent event) -> {
            if (cbBleachCorrection.getSelectedItem().toString().equals("Polynomial")) {
                PolynomialOrderDialogue();
            }

            Common.bleachCor = (String) cbBleachCorrection.getSelectedItem();
        };
        ActionListener cbCorrelator_pChanged = (ActionEvent event) -> {
            Common.correlator_p = Integer.parseInt(cbCorrelator_p.getSelectedItem().toString());
            cbCorrelator_p.setSelectedItem(cbCorrelator_p.getSelectedItem());
        };
        ActionListener btnCorrelator_qPressed = (ActionEvent event) -> {
            CorrelatorQDialogue();
        };
        ActionListener btnOptionPressed = (ActionEvent event) -> {
            OptionsDialogue();
        };
        ActionListener rbGPUonChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rbGPUon.isSelected() == true) {
                    if (Common.useGpu) {
                        Common.RunLiveReadOutOnGPU = true;
                        rbGPUon.setSelected(true);
                    } else {
                        rbGPUon.setSelected(false);
                        IJ.showMessage("NVIDIA not available. running on CPU mode");
                    }

                } else {
                    Common.RunLiveReadOutOnGPU = false;
                    rbGPUon.setSelected(false);
                }
            }
        };
        ActionListener cbOutputTrigger_hamChanged = (ActionEvent event) -> {
            cameraConstant.Common_Orca.OutputTriggerKind =
                    Arrays.asList(cameraConstant.Common_Orca.OutputTriggerKindArr)
                            .indexOf(cbOutputTrigger_ham.getSelectedItem().toString());

            if (cameraConstant.Common_Orca.OutputTriggerKind == 1) { //Programmable
                OutputTriggerDialogue();
            }
        };
        ActionListener cbReadoutSpeed_hamChanged = (ActionEvent event) -> {
            cameraConstant.Common_Orca.readoutSpeed = Arrays.asList(cameraConstant.Common_Orca.readoutSpeedArr)
                    .indexOf(cbReadoutSpeed_ham.getSelectedItem().toString());
        };
        ActionListener cbSensorMode_hamChanged = (ActionEvent event) -> {
            cameraConstant.Common_Orca.sensorMode = Arrays.asList(cameraConstant.Common_Orca.sensorModeArr)
                    .indexOf(cbSensorMode_ham.getSelectedItem().toString());

            if (cameraConstant.Common_Orca.sensorMode == 1) {
                cameraConstant.Common_Orca.readoutSpeed = 0;
                cbReadoutSpeed_ham.setSelectedIndex(cameraConstant.Common_Orca.readoutSpeed);
                cbReadoutSpeed_ham.setEnabled(false);
            } else {
                cbReadoutSpeed_ham.setEnabled(true);
            }

        };
        ActionListener cbOutputTrigger_sonaChanged = (ActionEvent event) -> {
            cameraConstant.Common_SONA.OutputTriggerKind =
                    Arrays.asList(cameraConstant.Common_SONA.OutputTriggerKindArr)
                            .indexOf(cbOutputTrigger_sona.getSelectedItem().toString());
        };
        ActionListener cbReadoutSpeed_photometricsChanged = (ActionEvent event) -> {
            int idx = cbReadoutSpeed_photometric.getSelectedIndex();

            cameraConstant.Common_Photometrics.readoutPortIndex =
                    Integer.valueOf(cameraConstant.Common_Photometrics.readoutSpeedDescription[idx][0]);
            cameraConstant.Common_Photometrics.readoutSpeedIndex =
                    Integer.valueOf(cameraConstant.Common_Photometrics.readoutSpeedDescription[idx][1]);
        };
        ActionListener btnClearWindowPressed = (ActionEvent event) -> {
            if (Common.fromImFCSobj1 != null) {
                Common.fromImFCSobj1.closeWindowsAll();
            }

            if (Common.fromImFCSobj2 != null) {
                Common.fromImFCSobj2.closeWindowsAll();
            }
        };
        ItemListener tbOverlap_sonaPressed = (ItemEvent ev) -> {
            if (ev.getStateChange() == ItemEvent.SELECTED) {
                tbOverlap_sona.setText("Overlap Off");
                cameraConstant.Common_SONA.isOverlap = 0;

            } else {
                tbOverlap_sona.setText("Overlap On");
                cameraConstant.Common_SONA.isOverlap = 1;
            }
        };

        public JSettingspanelComponent() {
            //JPanel
            //FCS paenl
            JPanel FCSPane = new JPanel(new GridLayout(4, 2));
            FCSPane.setBorder(BorderFactory.createTitledBorder("FCS"));

            //Camera setting panel
            switch ($camera) {
                case "DU860_BV":
                    CameraPane = new JPanel(new GridLayout(7, 2));
                    CameraPane.setBorder(BorderFactory.createTitledBorder("Camera"));
                    break;
                case "DU888_BV":
                    CameraPane = new JPanel(new GridLayout(7, 2));
                    CameraPane.setBorder(BorderFactory.createTitledBorder("Camera"));
                    break;
                case "DU897_BV":
                    CameraPane = new JPanel(new GridLayout(7, 2));
                    CameraPane.setBorder(BorderFactory.createTitledBorder("Camera"));
                    break;
                case "SONA-4BV11":
                    CameraPane = new JPanel(new GridLayout(2, 2));
                    CameraPane.setBorder(BorderFactory.createTitledBorder("Camera"));
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    CameraPane = new JPanel(new GridLayout(3, 2));
                    CameraPane.setBorder(BorderFactory.createTitledBorder("Camera"));
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    CameraPane = new JPanel(new GridLayout(1, 2));
                    CameraPane.setBorder(BorderFactory.createTitledBorder("Camera"));
                    break;

            }

            //Other panel
            JPanel OtherPane = new JPanel(new GridLayout(1, 2));
            OtherPane.setBorder(BorderFactory.createTitledBorder("Other"));

            //initialize
            cbBleachCorrection = new JComboBox<>();
            cbBleachCorrection.setToolTipText(
                    "Select or de-select bleach correction. NOTE: bleach correction algorithm segment total trace " +
                            "into 1000 equal interval for performance purposes.");
            cbBleachCorrection.addItem("none");
            cbBleachCorrection.addItem("Polynomial");
            cbBleachCorrection.setSelectedItem(Common.bleachCor);
            cbCorrelator_p = new JComboBox<>();
            cbCorrelator_p.setToolTipText("p correlator scheme. We recommend setting p = 16.");
            cbCorrelator_p.addItem("16");
            cbCorrelator_p.addItem("32");
            cbCorrelator_p.addItem("64");
            cbCorrelator_p.addItem("128");
            cbCorrelator_p.setSelectedItem(Common.correlator_p);
            btnCorrelator_q = new JButton("q:");
            btnCorrelator_q.setToolTipText(
                    "q correlator scheme. We recommend setting q = 8. NOTE: increase q to observe slower dynamics.");
            tfCorrelator_q = new JTextField(Integer.toString(Common.correlator_q), 8);
            tfCorrelator_q.setEditable(false);
            rbGPUon = new JRadioButton("GPU", Common.RunLiveReadOutOnGPU);
            rbGPUon.setToolTipText("activate or deactivate GPU for ACF(s) calculations.");
            btnTemperature = new JButton("Temperature");
            btnTemperature.setToolTipText("Opens dialog for sensor temperature.");
            btnFan = new JButton("Fan");
            btnFan.setToolTipText("Opens dialog for sensor fan.");
            btnOption = new JButton("Options");
            btnOption.setToolTipText("Opens dialog to activate various plotting option.");
            btnClearWindow = new JButton("Clear Windows");
            btnClearWindow.setFont(new java.awt.Font($panelFont, java.awt.Font.BOLD, 11));
            btnClearWindow.setToolTipText("Close all graph windows.");

            //Camera setting panel
            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                btnEmGain = new JButton("EM gain");
                btnEmGain.setToolTipText("Setting EMgain.");
                tfEmGain = new JTextField(Integer.toString(Common.EMgain), 8);
                tfEmGain.setEditable(false);
                tbMechanicalShutter = new JToggleButton("Shutter Off");
                tbMechanicalShutter.setToolTipText("Open/close shutter.");

                cbVspeed = new JComboBox<>();
                cbVspeed.setToolTipText(
                        "Speed at which each row on the CCD is shifted vertically into the Shift Register.");
                for (int i = 0; i < Common.VspeedArr.size(); i++) {
                    cbVspeed.addItem(Common.VspeedArr.get(i).toString());
                }
                cbVspeed.setSelectedIndex(Common.iVSpeed);

                cbVSAmp = new JComboBox<>();
                cbVSAmp.setToolTipText(
                        "Higher clocking voltages may result in increased clock-induced charge (noise) in your signal" +
                                ". In general only the very highest vertical clocking speeds (a low readout time) is " +
                                "likely " + "to benefit from increased vertical clock voltage amplitude.");
                for (int i = 0; i < Common.VSAmpArr.size(); i++) {
                    cbVSAmp.addItem(Common.VSAmpArr.get(i).toString());
                }
                cbVSAmp.setSelectedIndex(Common.iVSamp);

                cbHspeed = new JComboBox<>();
                cbHspeed.setToolTipText(
                        "Speed at which the pixels are shifted into the output node during the readout phase of an " +
                                "acquisition.");
                for (int i = 0; i < Common.HspeedArr.size(); i++) {
                    cbHspeed.addItem(Common.HspeedArr.get(i).toString());
                }
                cbHspeed.setSelectedIndex(Common.iHSpeed);

                cbPreAmpGain = new JComboBox<>();
                cbPreAmpGain.setToolTipText("Number of pre amp gains to apply to the data as it is read out.");
                for (int i = 0; i < Common.PreAmpGainArr.size(); i++) {
                    cbPreAmpGain.addItem(Common.PreAmpGainArr.get(i).toString());
                }
                cbPreAmpGain.setSelectedIndex(Common.iPreamp);

            }
            switch ($camera) {
                case "SONA-4BV11":
                    cbOutputTrigger_sona = new JComboBox<>();
                    cbOutputTrigger_sona.setToolTipText("Configure output timing");
                    for (String OutputTriggerKindArr : cameraConstant.Common_SONA.OutputTriggerKindArr) {
                        cbOutputTrigger_sona.addItem(OutputTriggerKindArr);
                    }
                    cbOutputTrigger_sona.setSelectedIndex(cameraConstant.Common_SONA.OutputTriggerKind);

                    if (cameraConstant.Common_SONA.isOverlap == 1) {
                        tbOverlap_sona = new JToggleButton("Overlap On");
                        tbOverlap_sona.setSelected(false);
                    } else {
                        tbOverlap_sona = new JToggleButton("Overlap Off");
                        tbOverlap_sona.setSelected(true);
                    }
                    tbOverlap_sona.setToolTipText("Overlap readout");
                    break;

                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    //Triggering
                    cbOutputTrigger_ham = new JComboBox<>();
                    cbOutputTrigger_ham.setToolTipText("Configure output timing");
                    for (String OutputTriggerKindArr : cameraConstant.Common_Orca.OutputTriggerKindArr) {
                        cbOutputTrigger_ham.addItem(OutputTriggerKindArr);
                    }
                    cbOutputTrigger_ham.setSelectedIndex(cameraConstant.Common_Orca.OutputTriggerKind);

                    //Readout speed
                    cbReadoutSpeed_ham = new JComboBox<>();
                    cbReadoutSpeed_ham.setToolTipText(
                            "Configure readout speed: faster scan comes at expense of higher readout noise");
                    for (String elem : cameraConstant.Common_Orca.readoutSpeedArr) {
                        cbReadoutSpeed_ham.addItem(elem);
                    }
                    cbReadoutSpeed_ham.setSelectedIndex(cameraConstant.Common_Orca.readoutSpeed);

                    //Sensor mode
                    cbSensorMode_ham = new JComboBox<>();
                    cbSensorMode_ham.setToolTipText("Configure sensor mode");
                    for (String elem : cameraConstant.Common_Orca.sensorModeArr) {
                        cbSensorMode_ham.addItem(elem);
                    }
                    cbSensorMode_ham.setSelectedIndex(cameraConstant.Common_Orca.sensorMode);
                    break;

                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    //Readout speed
                    cbReadoutSpeed_photometric = new JComboBox<>();
                    cbReadoutSpeed_photometric.setToolTipText("Configure readout speed");

                    // loop over total port * total speed per port
                    for (String[] readoutSpeedDescription :
                     cameraConstant.Common_Photometrics.readoutSpeedDescription) {
                        cbReadoutSpeed_photometric.addItem(readoutSpeedDescription[4]);
                    }

                    int defaultPortSpeedIdx = 0;
                    cbReadoutSpeed_photometric.setSelectedIndex(defaultPortSpeedIdx);
                    cameraConstant.Common_Photometrics.readoutPortIndex = Integer.valueOf(
                            cameraConstant.Common_Photometrics.readoutSpeedDescription[defaultPortSpeedIdx][0]);
                    cameraConstant.Common_Photometrics.readoutSpeedIndex = Integer.valueOf(
                            cameraConstant.Common_Photometrics.readoutSpeedDescription[defaultPortSpeedIdx][1]);


                    break;

            }

            //FCS settings (top panel)
            FCSPane.add(new JLabel("Bleach Correction"));
            FCSPane.add(cbBleachCorrection);
            FCSPane.add(new JLabel("p:"));
            FCSPane.add(cbCorrelator_p);
            FCSPane.add(btnCorrelator_q);
            FCSPane.add(tfCorrelator_q);
            FCSPane.add(rbGPUon);
            FCSPane.add(new JLabel(""));

            //Camera settings
            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                CameraPane.add(btnEmGain);
                CameraPane.add(tfEmGain);
                CameraPane.add(btnTemperature);
                CameraPane.add(btnFan);
                CameraPane.add(new JLabel("Shift Speed [usecs]"));
                CameraPane.add(cbVspeed);
                CameraPane.add(new JLabel("Vertical Amplitude"));
                CameraPane.add(cbVSAmp);
                CameraPane.add(new JLabel("Readout Rate [MHz]"));
                CameraPane.add(cbHspeed);
                CameraPane.add(new JLabel("Pre-Amp Gain"));
                CameraPane.add(cbPreAmpGain);
                CameraPane.add(tbMechanicalShutter);
                CameraPane.add(new JLabel(""));
            }
            switch ($camera) {
                case "SONA-4BV11":
                    CameraPane.add(btnTemperature);
                    CameraPane.add(btnFan);
                    CameraPane.add(tbOverlap_sona);
                    CameraPane.add(cbOutputTrigger_sona);
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    CameraPane.add(new JLabel("Out. Trigger"));
                    CameraPane.add(cbOutputTrigger_ham);
                    CameraPane.add(new JLabel("Readout Speed"));
                    CameraPane.add(cbReadoutSpeed_ham);
                    CameraPane.add(new JLabel("Sensor Mode"));
                    CameraPane.add(cbSensorMode_ham);
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    CameraPane.add(new JLabel("Readout Speed"));
                    CameraPane.add(cbReadoutSpeed_photometric);
                    break;
            }

            //Other settigs
            OtherPane.add(btnOption);
            OtherPane.add(btnClearWindow);

            Container cp = this.getContentPane();
            cp.setLayout(new BorderLayout(1, 1));
            cp.add(FCSPane, BorderLayout.NORTH);
            cp.add(CameraPane, BorderLayout.CENTER);
            cp.add(OtherPane, BorderLayout.SOUTH);

            //addListener
            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                btnEmGain.addActionListener(btnEmGainPressed);
                cbVspeed.addActionListener(cbVspeedChanged);
                cbVSAmp.addActionListener(cbVSAmpChanged);
                cbHspeed.addActionListener(cbHspeedChanged);
                cbPreAmpGain.addActionListener(cbPreAmpGainChanged);
                tbMechanicalShutter.addItemListener(tbMechanicalShutterPressed);
            }

            btnFan.addActionListener(btnFanPressed);
            cbBleachCorrection.addActionListener(cbBleacCorrectionChanged);
            cbCorrelator_p.addActionListener(cbCorrelator_pChanged);
            btnCorrelator_q.addActionListener(btnCorrelator_qPressed);
            btnOption.addActionListener(btnOptionPressed);
            rbGPUon.addActionListener(rbGPUonChanged);
            btnTemperature.addActionListener(btnTemperaturePressed);
            btnClearWindow.addActionListener(btnClearWindowPressed);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setUndecorated(false);
            addWindowListener(getWindowAdapter());
            setTitle("Settings");
            if ($camera.equals("DU860_BV") || $camera.equals("DU888_BV") || $camera.equals("DU897_BV")) {
                setSize(250, 400); //setSize(new Dimension(230, 220)); // 230,230
            }
            switch ($camera) {
                case "SONA-4BV11":
                    tbOverlap_sona.addItemListener(tbOverlap_sonaPressed);
                    cbOutputTrigger_sona.addActionListener(cbOutputTrigger_sonaChanged);
                    setSize(250, 300); //setSize(new Dimension(230, 220)); // 230,230
                    break;
                case "C11440-22CU":
                case "C11440-22C":
                case "C13440-20CU":
                case "C13440-20C":
                case "C15550-20UP":
                    cbOutputTrigger_ham.addActionListener(cbOutputTrigger_hamChanged);
                    cbReadoutSpeed_ham.addActionListener(cbReadoutSpeed_hamChanged);
                    cbSensorMode_ham.addActionListener(cbSensorMode_hamChanged);
                    setSize(250, 320);
                    break;
                case "EVOLVE- 512":
                case "GS144BSI":
                case "TMP-Kinetix":
                    cbReadoutSpeed_photometric.addActionListener(cbReadoutSpeed_photometricsChanged);
                    setSize(250, 270); //setSize(new Dimension(230, 220)); // 230,230
                    break;
            }
            setLocation(new Point(SettingpanelPosX, SettingpanelPosY));
            setFocusable(true);
            setResizable(false);
            setVisible(false);

        }

        private WindowAdapter getWindowAdapter() {
            return new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {//overrode to show message
                    super.windowClosing(we);
                    JOptionPane.showMessageDialog(we.getComponent(),
                            "Toggle off Settings on main panel to hide panel.");
                }

                @Override
                public void windowIconified(WindowEvent we) {
                    setState(JFrame.NORMAL);
                    JOptionPane.showMessageDialog(we.getComponent(),
                            "Toggle off Settings on main panel to hide panel.");
                }
            };
        }

        private boolean PolynomialOrderDialogue() {

            GenericDialog gd = new GenericDialog("Polynomial Order");
            gd.addMessage("Only Integers allowed.");
            gd.addNumericField("Order: ", Common.polynomDegree, 0);
            gd.showDialog();
            if (gd.wasCanceled()) {
                cbBleachCorrection.setSelectedItem(Common.bleachCor);
                return false;
            }
            int potmp = (int) Math.floor(gd.getNextNumber());
            if (!(potmp > 0)) {
                IJ.showMessage("Invalid polynomial order");
                return false;
            }
            if (potmp > 8) {
                IJ.showMessage("Order > " + 8 + "not allowed");
                return false;
            } else {
                Common.polynomDegree = potmp;
            }

            return true;
        }

        private boolean CorrelatorQDialogue() {

            GenericDialog gd = new GenericDialog("Correlator q");
            gd.addMessage("Only Integers allowed.");
            gd.addNumericField("value: ", Common.correlator_q, 0);
            gd.showDialog();
            if (gd.wasCanceled()) {
                tfCorrelator_q.setText(Integer.toString(Common.correlator_q));
                return false;
            }
            int tempval = (int) Math.floor(gd.getNextNumber());
            if (!(tempval > 0)) {
                IJ.showMessage("Invalid q value");
                Common.correlator_q = 8;
                tfCorrelator_q.setText(Integer.toString(Common.correlator_q));
                return false;
            }
            if (tempval > 64) {
                IJ.showMessage("q > " + 64 + "not allowed");
                Common.correlator_q = 8;
                tfCorrelator_q.setText(Integer.toString(Common.correlator_q));
                return false;
            }

            if (gd.wasOKed()) {
                Common.correlator_q = tempval;
                tfCorrelator_q.setText(Integer.toString(Common.correlator_q));
                tfCorrelator_q.setText(Integer.toString(Common.correlator_q));
            }

            return true;
        }

        private boolean OptionsDialogue() {
            GenericDialog gd = new GenericDialog("Options");
            gd.addCheckbox("ACF", Common.plotACFCurves);
            gd.addCheckbox("Intensity trace", Common.plotTrace);
            gd.addCheckbox("Live Video", Common.showLiveVideoCumul);
            gd.addCheckbox("Average Correlation", Common.plotAverage);
            gd.addCheckbox("Plot ACFs and CCF", !Common.plotJustCCF);
            gd.addCheckbox("Auto adjust ACF & Intensity scale", Common.isAutoAdjustACFintensityTraceScale);
            gd.addCheckbox("Auto adjust video dynamic range", Common.getAutoAdjustImageDynamicRange());
            gd.hideCancelButton();
            gd.showDialog();
            if (gd.wasOKed()) {
                Common.plotACFCurves = gd.getNextBoolean();
                Common.plotTrace = gd.getNextBoolean();
                Common.showLiveVideoCumul = gd.getNextBoolean();
                Common.plotAverage = gd.getNextBoolean();
                Common.plotJustCCF = !gd.getNextBoolean();
                Common.isAutoAdjustACFintensityTraceScale = gd.getNextBoolean();
                boolean tempIsAutoAdjustDynamicRange = gd.getNextBoolean();

                Common.setAutoAdjustImageDynamicRange(tempIsAutoAdjustDynamicRange);

                if (Common.plotACFCurves) {
                    if (Common.selectedMode == modeEnum.CALIBRATION) {
                        JAcquisitionModePanelComponentPanel.setVisible(false);
                        JCalibrationPanelComponentPanel.setVisible(true);
                    }

                    if (Common.selectedMode == modeEnum.ACQUISITION) {
                        JAcquisitionModePanelComponentPanel.setVisible(true);
                        if (tbIsNonCumulCF.isSelected()) {
                            JCalibrationPanelComponentPanel.setVisible(true);
                        } else {
                            JCumulativeCFPanelComponentPanel.setVisible(true);
                        }

                    }

                } else {
                    JAcquisitionModePanelComponentPanel.setVisible(false);
                    JCumulativeCFPanelComponentPanel.setVisible(false);
                    JCalibrationPanelComponentPanel.setVisible(false);

                }
            }

            return true;
        }

        private boolean EmGainDialogue() {
            GenericDialog gd = new GenericDialog("EM Gain");
            gd.addMessage("Only Integers allowed.");
            gd.addNumericField("gain: ", Common.EMgain, 0);
            gd.showDialog();
            if (gd.wasCanceled()) {
                tfEmGain.setText(Integer.toString(Common.EMgain));
                return false;
            }
            int tempval = (int) Math.floor(gd.getNextNumber());
            if (tempval > 300 || tempval < 1) {
                IJ.showMessage("Invalid gain. Valid gain = 1 - 300");
                Common.EMgain = 300;
                tfEmGain.setText(Integer.toString(Common.EMgain));
                return false;
            }
            if (gd.wasOKed()) {
                Common.EMgain = tempval;
                tfEmGain.setText(Integer.toString(Common.EMgain));
            }
            return true;
        }

        private boolean TemperatureDialogue() {
            GenericDialog gd = new GenericDialog("Temperature Control");
            gd.addCheckbox("Cooler", Common.isCooling);
            gd.addNumericField("Temperature " + (char) 186 + " C:", Common.temperature, 0);
            gd.showDialog();
            if (gd.wasCanceled()) {

                return false;
            }
            Common.isCooling = gd.getNextBoolean();
            int tempval = (int) Math.floor(gd.getNextNumber());
            if (tempval > Common.maxtemp || tempval < Common.mintemp) {
                IJ.showMessage("Invalid temperature. Valid temperature = " + Common.mintemp + " - " + Common.maxtemp);
                return false;
            }
            if (gd.wasOKed()) {
                Common.temperature = tempval;
                if (Common.isCooling == true) {
                    APIcall.setCooling(1);
                } else {
                    APIcall.setCooling(0);
                }
                APIcall.setTemperature(Common.temperature);
            }
            return true;
        }

        private boolean FanDialogue() {
            GenericDialog gd = new GenericDialog("Fan Control");
            gd.addChoice("Fan", Common.FanList, Common.FanStatus);
            gd.showDialog();
            if (!gd.wasCanceled()) {
                Common.FanStatus = gd.getNextChoice();
                APIcall.setFan(Common.FanStatus);
                return true;
            } else {
                return false;
            }
        }

        private boolean OutputTriggerDialogue() {
            GenericDialog gd = new GenericDialog("Programmable Output Trigger");
            gd.addNumericField("Delay (us): ", cameraConstant.Common_Orca.outTriggerDelay * Math.pow(10, 6), 0);
            gd.addNumericField("Period (us): ", cameraConstant.Common_Orca.outTriggerPeriod * Math.pow(10, 6), 0);
            gd.showDialog();
            if (gd.wasCanceled()) {
                return false;
            }

            double tempval = gd.getNextNumber() / Math.pow(10, 6);
            if (tempval > 10 || tempval < 0) {
                IJ.showMessage("Invalid Delay. Valid = " + 0 + " - " + 10 + " s");
                return false;
            }
            double tempval2 = gd.getNextNumber() / Math.pow(10, 6);
            if (tempval2 > 10 || tempval2 < 0.000001) {
                IJ.showMessage("Invalid Period. Valid = " + 1 + " us - " + 10 + " s");
                return false;
            }
            if (gd.wasOKed()) {
                cameraConstant.Common_Orca.outTriggerDelay = tempval;
                cameraConstant.Common_Orca.outTriggerPeriod = tempval2;
            }
            return true;
        }

    }

    public class JCropModePanelComponent extends JFrame {

        JPanel JROIselectionPane;
        JPanel JCropDimPane;

        boolean DeactivateTFDocListener = true;
        ActionListener cbCropModeChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                //update cropMode Pane
                int idx = cbCropMode.getSelectedIndex();
                boolean status = updateCropModeParam(idx);
                DeactivateTFDocListener = true;
                UpdateCropModeDimTextField();
                DeactivateTFDocListener = false;

                if (idx < Common.CornerTetherCropROI.length) {// if not custom top-left corner tethered
                    JDimensionpanelComponentPanel.setODimtoCDim(Common.inCameraBinning);
                    DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                            Common.oHeight);
                } else {
                    JDimensionpanelComponentPanel.setODimtoCDim(Common.inCameraBinning);
                    DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                            Common.oHeight);
                }

            }
        };
        DocumentListener dimTfcWHchanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                if (!DeactivateTFDocListener) {
                    boolean updateDisplyIm =
                            updateCustomCropMode(); //update crop textfield and global cropped coordinate
                    JDimensionpanelComponentPanel.setODimtoCDim(Common.inCameraBinning);
                    DisplayImageObj.performROIselection(Common.oLeft - 1, Common.oTop - 1, Common.oWidth,
                            Common.oHeight);
                    if (updateDisplyIm) {
                        IJ.log("3893 unimplemented");
                    }
                }
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                if (!DeactivateTFDocListener) {
                    boolean updateDisplyIm =
                            updateCustomCropMode(); //update crop textfield and global cropped coordinate
                    if (updateDisplyIm) {
                        IJ.log("3893 unimplemented");
                    }
                }
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                if (!DeactivateTFDocListener) {
                    boolean updateDisplyIm =
                            updateCustomCropMode(); //update crop textfield and global cropped coordinate
                    if (updateDisplyIm) {
                        IJ.log("3893 unimplemented");
                    }
                }
            }
        };
        MouseListener dimTfcWHMouseUsed = new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent arg0) {
            }

            @Override
            public void mousePressed(MouseEvent arg0) {
            }

            @Override
            public void mouseReleased(MouseEvent arg0) {
            }

            @Override
            public void mouseEntered(MouseEvent arg0) {
                DeactivateTFDocListener = true;
                tfcWidth.setText(Integer.toString(Common.cWidth));
                tfcHeight.setText(Integer.toString(Common.cHeight));
                DeactivateTFDocListener = false;
            }

            @Override
            public void mouseExited(MouseEvent arg0) {
                DeactivateTFDocListener = true;
                tfcWidth.setText(Integer.toString(Common.cWidth));
                tfcHeight.setText(Integer.toString(Common.cHeight));
                DeactivateTFDocListener = false;
            }
        };

        public JCropModePanelComponent() {

            JROIselectionPane = new JPanel(new GridLayout(1, 2));
            JROIselectionPane.setBorder(
                    BorderFactory.createTitledBorder("Centered or Top-Left Tethered ROI Selection"));

            JCropDimPane = new JPanel(new GridLayout(6, 2));
            JCropDimPane.setBorder(BorderFactory.createTitledBorder("Crop Dimension"));

            InitializeAndFillPane();

            Container cp = this.getContentPane();
            cp.setLayout(new BorderLayout(1, 1));
            cp.add(JROIselectionPane, BorderLayout.NORTH);
            cp.add(JCropDimPane, BorderLayout.CENTER);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setTitle("Crop Mode");
            setSize(300, 250); //setSize(new Dimension(230, 220)); // 230,230

            setLocation(new Point(390, 125));
            setFocusable(true);
            setResizable(false);
            setVisible(false);
        }

        private void InitializeAndFillPane() {
            cbCropMode = new JComboBox<>();

            for (int i = 0; i < Common.CornerTetherCropROI.length; i++) {
                cbCropMode.addItem(Common.CornerTetherCropROI[i][0] + " x " + Common.CornerTetherCropROI[i][1]);
            }
            cbCropMode.addItem("Corner Tethered");
            cbCropMode.setSelectedIndex(0);

            tfcWidth = new JTextField(Integer.toString(Common.cWidth));
            tfcHeight = new JTextField(Integer.toString(Common.cHeight));
            tfcLeft = new JTextField(Integer.toString(Common.cLeft));
            tfcRight = new JTextField(Integer.toString(Common.cRight));
            tfcTop = new JTextField(Integer.toString(Common.cTop));
            tfcBottom = new JTextField(Integer.toString(Common.cBottom));

            JROIselectionPane.add(new JLabel("Crop ROI"));
            JROIselectionPane.add(cbCropMode);
            JCropDimPane.add(new JLabel("Width"));
            JCropDimPane.add(tfcWidth);
            tfcWidth.setEditable(false);
            JCropDimPane.add(new JLabel("Height"));
            JCropDimPane.add(tfcHeight);
            tfcHeight.setEditable(false);
            JCropDimPane.add(new JLabel("Left"));
            JCropDimPane.add(tfcLeft);
            tfcLeft.setEditable(false);
            JCropDimPane.add(new JLabel("Right"));
            JCropDimPane.add(tfcRight);
            tfcRight.setEditable(false);
            JCropDimPane.add(new JLabel("Top"));
            JCropDimPane.add(tfcTop);
            tfcTop.setEditable(false);
            JCropDimPane.add(new JLabel("Bottom"));
            JCropDimPane.add(tfcBottom);
            tfcBottom.setEditable(false);

            cbCropMode.addActionListener(cbCropModeChanged);
            tfcWidth.getDocument().addDocumentListener(dimTfcWHchanged);
            tfcHeight.getDocument().addDocumentListener(dimTfcWHchanged);
            tfcWidth.addMouseListener(dimTfcWHMouseUsed);
            tfcHeight.addMouseListener(dimTfcWHMouseUsed);

        }

        private boolean updateCropModeParam(int index) {
            int limit = Common.CornerTetherCropROI.length;
            if (index == limit) {
                Common.cLeft = 1;
                Common.cTop = 1;
                Common.cRight = Common.cLeft + Common.cWidth - 1;
                Common.cBottom = Common.cTop + Common.cHeight - 1;
                tfcWidth.setEditable(true);
                tfcHeight.setEditable(true);
                return false;
            }

            //update from Recommended Cetnralized Crop Mode coordinate from Andor
            tfcWidth.setEditable(false);
            tfcHeight.setEditable(false);

            Common.cWidth = Common.CornerTetherCropROI[index][0];
            Common.cHeight = Common.CornerTetherCropROI[index][1];
            Common.cLeft = Common.CornerTetherCropROI[index][2];
            Common.cRight = Common.CornerTetherCropROI[index][3];
            Common.cTop = Common.CornerTetherCropROI[index][4];
            Common.cBottom = Common.CornerTetherCropROI[index][5];

            return true;
        }

        private void UpdateCropModeDimTextField() {
            tfcLeft.setText(Integer.toString(Common.cLeft));
            tfcRight.setText(Integer.toString(Common.cRight));
            tfcTop.setText(Integer.toString(Common.cTop));
            tfcBottom.setText(Integer.toString(Common.cBottom));
            tfcWidth.setText(Integer.toString(Common.cWidth));
            tfcHeight.setText(Integer.toString(Common.cHeight));
        }

        private boolean updateCustomCropMode() {
            boolean proceed = false;
            int tempw = 0, temph = 0;
            int[] oCoordinate;

            try {
                tempw = Integer.parseInt(tfcWidth.getText());
                temph = Integer.parseInt(tfcHeight.getText());
                proceed = true;
            } catch (NumberFormatException nfe) {
                return false;
            }

            if (proceed) {

                if (tempw > Common.chipSizeX) {
                    tempw = Common.chipSizeX;
                }
                if (tempw < 2) {
                    tempw = 2;
                }
                if (temph > Common.chipSizeY) {
                    temph = Common.chipSizeY;
                }
                if (temph < 2) {
                    temph = 2;
                }

                Common.cWidth = tempw;
                Common.cHeight = temph;
                Common.cLeft = 1; //top-left corner tethered
                Common.cTop = 1;//top-left corner tethered
                Common.cRight = Common.cLeft + Common.cWidth - 1;
                Common.cBottom = Common.cTop + Common.cHeight - 1;

                tfcLeft.setText(Integer.toString(Common.cLeft));
                tfcRight.setText(Integer.toString(Common.cRight));
                tfcTop.setText(Integer.toString(Common.cTop));
                tfcBottom.setText(Integer.toString(Common.cBottom));

                return true;
            } else {
                return false;
            }
        }
    }

    public class JCalibrationPanelComponent extends JFrame {

        final int CalibpanelPosX = 700;
        // control panel, "ImFCS", position and dimensions
        final int CalibpanelPosY = 355;
        final int CalibpanelDimX = 250;
        final int CalibpanelDimY = 150;
        JPanel FocusFinderPane;
        ActionListener btnPlotIntervalPressed = (ActionEvent event) -> {
            boolean changedpi = PlotIntervalDialogue();
        };
        ActionListener rbPlotCalibrationAmplitudeChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rbPlotCalibrationAmplitude.isSelected() == true) {
                    Common.plotCalibAmplitude = true;
                    rbPlotCalibrationAmplitude.setSelected(true);

                } else {
                    Common.plotCalibAmplitude = false;
                    rbPlotCalibrationAmplitude.setSelected(false);
                }
            }
        };
        ActionListener rbPlotCalibrationIntensityChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rbPlotCalibrationIntensity.isSelected() == true) {
                    Common.plotCalibIntensity = true;
                    rbPlotCalibrationIntensity.setSelected(true);

                } else {
                    Common.plotCalibIntensity = false;
                    rbPlotCalibrationIntensity.setSelected(false);
                }
            }
        };
        ActionListener rbPlotCalibrationDiffusionChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (rbPlotCalibrationDiffusion.isSelected() == true) {
                    Common.plotCalibDiffusion = true;
                    rbPlotCalibrationDiffusion.setSelected(true);

                } else {
                    Common.plotCalibDiffusion = false;
                    rbPlotCalibrationDiffusion.setSelected(false);
                }
            }
        };
        DocumentListener tfNoPtsCalibChanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                UpdateCalibParam();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                UpdateCalibParam();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                UpdateCalibParam();
            }
        };
        ItemListener tbCalibFixScaleChanged = (ItemEvent ev) -> {
            if (ev.getStateChange() == ItemEvent.SELECTED) {
                tbCalibFixScale.setText("Fix Scale");
                Common.isCalibFixScale = true; //deactivate autoscaling
            } else {
                tbCalibFixScale.setText("Free Scale");
                Common.isCalibFixScale = false;//activate autoscaling
            }
        };

        public JCalibrationPanelComponent() {
            FocusFinderPane = new JPanel(new GridLayout(4, 2));
            FocusFinderPane.setBorder(BorderFactory.createTitledBorder("Non-cumulative CF"));

            //            OptoSplitStaticAlignPane = new JPanel(new GridLayout(1, 2));
            //            OptoSplitStaticAlignPane.setBorder(BorderFactory.createTitledBorder("S.A."));
            //
            //            OptoSplitDynamicAlignPane = new JPanel(new GridLayout(1, 2));
            //            OptoSplitDynamicAlignPane.setBorder(BorderFactory.createTitledBorder("D.A."));
            //initialize
            rbPlotCalibrationAmplitude = new JRadioButton("Plot Amplitude", Common.plotCalibAmplitude);
            rbPlotCalibrationAmplitude.setToolTipText("activate Amplitude trace");
            rbPlotCalibrationDiffusion = new JRadioButton("Plot Diffusion", Common.plotCalibDiffusion);
            rbPlotCalibrationDiffusion.setToolTipText("activate Diffusion trace");
            rbPlotCalibrationIntensity = new JRadioButton("Plot Intensity", Common.plotCalibIntensity);
            rbPlotCalibrationIntensity.setToolTipText("activate Intensity trace");
            btnPlotInterval = new JButton("Plot Interval:");
            tfPlotInterval = new JTextField(Integer.toString(Common.plotInterval), 8);
            tfPlotInterval.setToolTipText(
                    "Set number of frame used in ACF(s) plotting. Only applicable in calibration mode. NOTE: reducing" +
                            " ROI would improve the maximum allowable plot interval.");
            tfPlotInterval.setEditable(false);
            tfNoPtsCalib = new JTextField(Integer.toString(Common.noptsavr), 8);
            tfNoPtsCalib.setToolTipText("Set number of correlation points to be averaged for focus-finder algorithm");
            tbCalibFixScale = new JToggleButton("Free Scale");
            tbCalibFixScale.setToolTipText("Set whether to auto scale calibration(s) plot");

            //FocusFinderPane (top panel)
            FocusFinderPane.add(new JLabel("No Average:"));
            FocusFinderPane.add(tfNoPtsCalib);
            FocusFinderPane.add(btnPlotInterval);
            FocusFinderPane.add(tfPlotInterval);
            FocusFinderPane.add(rbPlotCalibrationAmplitude);
            FocusFinderPane.add(rbPlotCalibrationIntensity);
            FocusFinderPane.add(rbPlotCalibrationDiffusion);
            FocusFinderPane.add(tbCalibFixScale);

            //            //OptoSplitStaticAlignPane (center panel)
            //            OptoSplitStaticAlignPane.add(new JLabel(""));
            //            OptoSplitStaticAlignPane.add(new JLabel(""));
            //
            //            //OptoSplitDynamicAlignPane (bottom panel)
            //            OptoSplitDynamicAlignPane.add(new JLabel(""));
            //            OptoSplitDynamicAlignPane.add(new JLabel(""));
            Container cp = this.getContentPane();
            cp.setLayout(new BorderLayout(1, 1));
            cp.add(FocusFinderPane, BorderLayout.CENTER);
            //            cp.add(OptoSplitStaticAlignPane, BorderLayout.CENTER);
            //            cp.add(OptoSplitDynamicAlignPane, BorderLayout.SOUTH);

            //add listener
            btnPlotInterval.addActionListener(btnPlotIntervalPressed);
            rbPlotCalibrationAmplitude.addActionListener(rbPlotCalibrationAmplitudeChanged);
            rbPlotCalibrationIntensity.addActionListener(rbPlotCalibrationIntensityChanged);
            rbPlotCalibrationDiffusion.addActionListener(rbPlotCalibrationDiffusionChanged);
            tfNoPtsCalib.getDocument().addDocumentListener(tfNoPtsCalibChanged);
            tbCalibFixScale.addItemListener(tbCalibFixScaleChanged);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setUndecorated(false);
            addWindowListener(getWindowAdapter());
            //            setTitle("Calibration Panel");
            setSize(CalibpanelDimX, CalibpanelDimY);
            setLocation(new Point(CalibpanelPosX, CalibpanelPosY));
            setFocusable(true);
            setResizable(false);
            setVisible(false);

        }

        private boolean PlotIntervalDialogue() {
            GenericDialog gd = new GenericDialog("Calibration Plot Interval");
            //            gd.addNumericField("Plot Interval: ", Common.plotInterval, 0);
            gd.addStringField("Plot Interval: ", Integer.toString(Common.plotInterval), 0);
            gd.showDialog();

            if (gd.wasOKed()) {
                try {
                    int tempPI = Integer.parseInt(gd.getNextString());
                    //                    int tempPI = (int) gd.getNextNumber();
                    int maxpi = retMaxAllowablePlotInterval(Common.size_a, Common.size_b);
                    if (tempPI < maxpi) {
                        Common.plotInterval = tempPI;
                    } else {
                        IJ.showMessage("Maximum allowable Plot Interval: " + maxpi +
                                " frames. Reduce ROI to expand Plot Interval range");
                        Common.plotInterval = maxpi;
                    }
                    if (tempPI < Common.minAllowableFrame) {
                        IJ.showMessage("Minimum allowable Plot Interval: " + Common.minAllowableFrame);
                        Common.plotInterval = Common.minAllowableFrame;
                    } else {
                        Common.plotInterval = tempPI;
                    }
                    tfPlotInterval.setText(Integer.toString(Common.plotInterval));
                    return true;
                } catch (NumberFormatException nfe) {
                    IJ.showMessage("Plot Interval has to be Integer.");
                    return false;
                }
            } else {
                return false;
            }
        }

        private WindowAdapter getWindowAdapter() {
            return new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {//overrode to show message
                    //                    super.windowClosing(we);
                }

                @Override
                public void windowIconified(WindowEvent we) {
                    setState(JFrame.NORMAL);
                }
            };
        }

    }

    public class JICCSPanelComponent extends JFrame {

        final int ICCSpanelPosX = 700;
        // control panel, "ImFCS", position and dimensions
        final int ICCSpanelPosY = 355;
        final int ICCSpanelDimX = 320;
        final int ICCSpanelDimY = 170;
        //reset index
        final int indexXYCCF = 0;
        JPanel ICCSPane;
        JToggleButton tbFitICCS;
        //Document Listener
        DocumentListener tfICCSCoordChanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                UpdateICCSSettigs("CoordinateGreen");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                UpdateICCSSettigs("CoordinateGreen");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                UpdateICCSSettigs("CoordinateGreen");
            }
        };
        DocumentListener tfICCSParamChanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                UpdateICCSSettigs("ParamRed");
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                UpdateICCSSettigs("ParamRed");
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                UpdateICCSSettigs("ParamRed");
            }
        };
        ItemListener tbFitICCSPressed = (ItemEvent ev) -> {
            if (ev.getStateChange() == ItemEvent.SELECTED) {
                if (iccsObj1 != null) {
                    tbFitICCS.setText("Fit on");
                    iccsObj1.isFittingICCS = true;
                    //                    iccsObj1.hideUnhidePlot(true);
                } else {
                    tbFitICCS.setBorderPainted(false);
                    tbFitICCS.setSelected(false);
                }

            } else {

                if (iccsObj1 != null) {
                    tbFitICCS.setText("Fit off");
                    iccsObj1.isFittingICCS = false;
                    //                    iccsObj1.hideUnhidePlot(false);
                } else {
                    tbFitICCS.setBorderPainted(false);
                    tbFitICCS.setSelected(false);
                }

            }

        };

        public JICCSPanelComponent() {
            ICCSPane = new JPanel(new GridLayout(5, 2));
            tbFitICCS = new JToggleButton("Fit off");
            ICCSPane.setBorder(BorderFactory.createTitledBorder("ICCS maps"));
            //initialize
            tfICCSRoi1Coord = new JTextField(
                    Integer.toString(Common.lWidth) + " / " + Integer.toString(Common.lHeight) + " / " +
                            Integer.toString(Common.lLeft) + " / " + Integer.toString(Common.lTop), 8);
            tfICCSRoi1Coord.setToolTipText("Setting coordinate of green channel to be correlated");
            tfICCSParam =
                    new JTextField(Integer.toString(Common.ICCSShiftX) + " / " + Integer.toString(Common.ICCSShiftY),
                            8);
            tfICCSParam.setToolTipText("Setting parameters of red channel to be correlated");
            //Panel
            ICCSPane.add(new JLabel("ROI (Intensity-map) :"));
            ICCSPane.add(new JLabel(""));
            ICCSPane.add(new JLabel("W / H / L / T")); //index L, T start from 1
            ICCSPane.add(tfICCSRoi1Coord);
            ICCSPane.add(new JLabel("Pixel shift :"));
            ICCSPane.add(new JLabel(""));
            ICCSPane.add(new JLabel("X / Y"));
            ICCSPane.add(tfICCSParam);
            ICCSPane.add(new JLabel("Fit :"));
            ICCSPane.add(tbFitICCS);

            Container cp = this.getContentPane();
            cp.setLayout(new BorderLayout(1, 1));
            cp.add(ICCSPane, BorderLayout.CENTER);

            //add listener
            tfICCSRoi1Coord.getDocument().addDocumentListener(tfICCSCoordChanged);
            tfICCSParam.getDocument().addDocumentListener(tfICCSParamChanged);
            tbFitICCS.addItemListener(tbFitICCSPressed);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setUndecorated(false);
            addWindowListener(getWindowAdapter());
            setTitle("Static alignment Panel");
            setSize(ICCSpanelDimX, ICCSpanelDimY);
            setLocation(new Point(ICCSpanelPosX, ICCSpanelPosY));
            setFocusable(true);
            setResizable(false);
            setVisible(false);

        }

        private WindowAdapter getWindowAdapter() {
            return new WindowAdapter() {

                @Override
                public void windowClosing(WindowEvent we) {//overrode to show message
                    //                    super.windowClosing(we);
                }

                @Override
                public void windowIconified(WindowEvent we) {
                    setState(JFrame.NORMAL);
                }
            };
        }

        public void resetFitToggle() {
            tbFitICCS.setText("Fit off");
            tbFitICCS.setBorderPainted(false);
            tbFitICCS.setSelected(false);

        }

        public void resetParam() {
            tfICCSRoi1Coord.setText(Integer.toString(Common.lWidth) + " / " + Integer.toString(Common.lHeight) + " / " +
                    Integer.toString(Common.lLeft) + " / " + Integer.toString(Common.lTop));
            tfICCSParam.setText(Integer.toString(0) + " / " + Integer.toString(0));
            tfCCFdist.setText(Integer.toString(Common.CCFdistX) + " x " + Integer.toString(Common.CCFdistY));
        }
    }

    public class JAcquisitionModePanelComponent extends JFrame {

        final int AcqModepanelPosX = 700;
        // control panel, "ImFCS", position and dimensions
        final int AcqModepanelPosY = 125;
        final int AcqModepanelDimX = 330;
        final int AcqModepanelDimY = 170;
        JPanel JAcqModePane;
        JTextField tfCalibrationType;
        ItemListener tbIsNonCumulCFChanged = (ItemEvent ev) -> {
            if (ev.getStateChange() == ItemEvent.SELECTED) {
                Common.analysisMode = $amode[1];//non-cumulative
                tbIsNonCumulCF.setText($amode[1]);
                JCalibrationPanelComponentPanel.setVisible(true);
                JCumulativeCFPanelComponentPanel.setVisible(false);

            } else {
                // Reset fitstart to the latest available frame index
                if (Common.isAcquisitionRunning && Common.framecounter != null && Common.framecounterIMSX != null) {
                    Common.fitStartCumulative = Common.framecounterIMSX.getCount();
                    tfCumulativeFitStart.setText(Integer.toString(Common.fitStartCumulative));
                }

                Common.analysisMode = $amode[2];//cumulative
                tbIsNonCumulCF.setText($amode[2]);
                JCalibrationPanelComponentPanel.setVisible(false);
                JCumulativeCFPanelComponentPanel.setVisible(true);
            }
        };
        ActionListener tbRecordTimePointChanged = (ActionEvent ev) -> {
            if (Common.isAcquisitionRunning) {
                if (tbRecordTimePoint.isSelected()) {
                    if (CalibrationTypeSelectionDialogue()) {

                        if (currentCalibrationType.equals(calibrationTypeList[calibrationTypeList.length - 1])) {
                            Common.ttsObj.fillOtherRemarksFrameIdx(otherRemarks,
                                    Common.framecounter.getCounter()); //update other user remarks and time stamp
                            toggleOnOff(false);
                        } else {
                            Common.ttsObj.fillCalibrationFrameIdx(retCalibIdx(currentCalibrationType),
                                    Common.framecounter.getCounter());//update calib start index
                            toggleOnOff(true);
                        }

                    } else {
                        toggleOnOff(false);
                    }
                } else {
                    Common.ttsObj.fillCalibrationFrameIdx(retCalibIdx(currentCalibrationType),
                            Common.framecounter.getCounter());//update calib end index
                    toggleOnOff(false);
                }
            } else {
                toggleOnOff(false);
            }
        };

        public JAcquisitionModePanelComponent() {
            JAcqModePane = new JPanel(new GridLayout(5, 2));
            JAcqModePane.setBorder(BorderFactory.createTitledBorder("Toggle on/off"));

            tbIsNonCumulCF = new JToggleButton($amode[2]);
            tbIsNonCumulCF.setToolTipText("Toggle between cumulative or non-cumulative correlation functions.");
            Common.analysisMode = $amode[2];//cumulative
            tbRecordTimePoint = new JToggleButton("Off");
            tbRecordTimePoint.setToolTipText(
                    "Toggle On to indicate time points in which changes to the microscope were made. Toggle Off once " +
                            "instrument stabilized. Information saved as a json file.");
            tbRecordTimePoint.setForeground(Color.blue);

            currentCalibrationType = calibrationTypeList[0];
            tfCalibrationType = new JTextField(currentCalibrationType);
            tfCalibrationType.setEditable(false);

            JAcqModePane.add(new JLabel("Correlation function type:"));
            JAcqModePane.add(tbIsNonCumulCF);
            //JAcqModePane (second row)
            JAcqModePane.add(new JLabel(""));
            JAcqModePane.add(new JLabel(""));
            //JAcqModePane (second row)
            JAcqModePane.add(new JLabel(""));
            JAcqModePane.add(new JLabel(""));
            //JAcqModePane (second row)
            JAcqModePane.add(new JLabel("Calibration type:"));
            JAcqModePane.add(tfCalibrationType);
            //JAcqModePane (third row)
            JAcqModePane.add(new JLabel("Recording time points:"));
            JAcqModePane.add(tbRecordTimePoint);

            Container cp = this.getContentPane();
            cp.setLayout(new BorderLayout(1, 1));
            cp.add(JAcqModePane, BorderLayout.CENTER);

            //add listener
            tbIsNonCumulCF.addItemListener(tbIsNonCumulCFChanged);
            tbRecordTimePoint.addActionListener(tbRecordTimePointChanged);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setUndecorated(false);
            addWindowListener(getWindowAdapter());
            setTitle("Acquisition mode");
            setSize(AcqModepanelDimX, AcqModepanelDimY);
            setLocation(new Point(AcqModepanelPosX, AcqModepanelPosY));
            setFocusable(true);
            setResizable(false);
            setVisible(false);
        }

        private boolean CalibrationTypeSelectionDialogue() {
            GenericDialog gd = new GenericDialog("Time-tagged infos stored in JSON format");
            gd.addChoice("Calibration type", calibrationTypeList, currentCalibrationType);
            gd.addTextAreas(otherRemarks, null, 5, 26);

            gd.showDialog();

            if (gd.wasOKed()) {
                currentCalibrationType = gd.getNextChoice();
                tfCalibrationType.setText(currentCalibrationType);

                if (currentCalibrationType.equals(calibrationTypeList[calibrationTypeList.length - 1])) {
                    otherRemarks = gd.getNextText();
                }
                return true;
            } else {
                return false;
            }

        }

        private void toggleOnOff(boolean isOn) {
            if (isOn) {
                tbRecordTimePoint.setText("On");
                tbRecordTimePoint.setForeground(Color.red);
                tbRecordTimePoint.setSelected(true);
            } else {
                tbRecordTimePoint.setText("Off");
                tbRecordTimePoint.setForeground(Color.blue);
                tbRecordTimePoint.setSelected(false);
            }
        }

        private int retCalibIdx(String userSTring) {
            return Arrays.asList(calibrationTypeList).indexOf(userSTring);
        }

        private WindowAdapter getWindowAdapter() {
            return new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {//overrode to show message
                    super.windowClosing(we);
                }

                @Override
                public void windowIconified(WindowEvent we) {
                    setState(JFrame.NORMAL);
                    //                    JOptionPane.showMessageDialog(we.getComponent(), "Can't Minimize");
                }
            };
        }

    }

    public class JCumulativeCFPanelComponent extends JFrame {

        final int CumulativepanelPosX = 700;
        // control panel, "ImFCS", position and dimensions
        final int CumulativepanelPosY = 355;
        final int CumulativepanelDimX = 250;
        final int CumulativepanelDimY = 150;
        JPanel CumulativeCFPane;
        JButton btnCumulativeFitStart;
        ActionListener btnFitStartPressed = (ActionEvent event) -> {
            boolean changedfs = FitStartDialogue();
        };

        public JCumulativeCFPanelComponent() {
            CumulativeCFPane = new JPanel(new GridLayout(4, 2));
            CumulativeCFPane.setBorder(BorderFactory.createTitledBorder("Cumulative CF"));

            //Initialize
            btnCumulativeFitStart = new JButton("Fit Start:");
            btnCumulativeFitStart.setEnabled(true);
            tfCumulativeFitStart = new JTextField(Integer.toString(Common.fitStartCumulative), 8);
            tfCumulativeFitStart.setToolTipText("Set starting point for calculation of CFs");
            tfCumulativeFitStart.setEditable(false);

            //CumulativeCFPane
            CumulativeCFPane.add(btnCumulativeFitStart);
            CumulativeCFPane.add(tfCumulativeFitStart);
            CumulativeCFPane.add(new JLabel(""));
            CumulativeCFPane.add(new JLabel(""));
            CumulativeCFPane.add(new JLabel(""));
            CumulativeCFPane.add(new JLabel(""));
            CumulativeCFPane.add(new JLabel(""));
            CumulativeCFPane.add(new JLabel(""));

            Container cp = this.getContentPane();
            cp.setLayout(new BorderLayout(1, 1));
            cp.add(CumulativeCFPane, BorderLayout.CENTER);

            //add listener
            btnCumulativeFitStart.addActionListener(btnFitStartPressed);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setUndecorated(false);
            addWindowListener(getWindowAdapter());
            //            setTitle("Cumulative CF");
            setSize(CumulativepanelDimX, CumulativepanelDimY);
            setLocation(new Point(CumulativepanelPosX, CumulativepanelPosY));
            setFocusable(true);
            setResizable(false);
            setVisible(false);
        }

        private WindowAdapter getWindowAdapter() {
            return new WindowAdapter() {
                @Override
                public void windowClosing(WindowEvent we) {//overrode to show message
                    //                    super.windowClosing(we);
                }

                @Override
                public void windowIconified(WindowEvent we) {
                    setState(JFrame.NORMAL);
                }
            };
        }

        private boolean FitStartDialogue() {
            GenericDialog gd = new GenericDialog("");
            gd.addStringField("Fit Start: ", Integer.toString(Common.fitStartCumulative), 0);
            gd.showDialog();

            if (gd.wasOKed()) {
                try {
                    if (!Common.isAcquisitionRunning || Common.framecounter == null) {
                        return false;
                    }
                    int currFrame = Common.framecounterIMSX.getCount();
                    int tempFS = Integer.parseInt(gd.getNextString());

                    // Check for valid fit start
                    if (tempFS >= 1 && tempFS <= currFrame) {
                        Common.fitStartCumulative = tempFS;
                    } else if (tempFS < 1) {
                        Common.fitStartCumulative = 1;
                    } else {
                        Common.fitStartCumulative = currFrame;
                    }

                } catch (NumberFormatException nfe) {
                    IJ.showMessage("Fit start has to be Integer.");
                    return false;

                }

                tfCumulativeFitStart.setText(Integer.toString(Common.fitStartCumulative));
                return true;

            } else {
                return false;
            }
        }
    }

    public class JLiveVideoPanelComponent extends JFrame {

        JPanel LiveVideoDisplayPane;
        JPanel LiveVideoBinningPane;
        JRadioButton rbSetAllFrames;    //whether to display all frames
        JRadioButton rbSetOddFrames;    //to display only odd frames (useful for PIE Imaging FCCS)
        JRadioButton rbSetEvenFrames;   //to display only even frames (useful for PIE Imaging FCCS)
        JComboBox<String> cbDisplayTimeBinningMode;    //binning mode: no binning, sum, or average
        JTextField tfTimeBinSize;       //number of frames for eitehr sum or average binning
        //Listener definitions
        ActionListener rbSetAllFramesChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (rbSetAllFrames.isSelected() == true) {
                    Common.livevideo_displayFramesMode = 0;
                    rbSetEvenFrames.setSelected(false);
                    rbSetOddFrames.setSelected(false);
                } else {
                    rbSetAllFrames.setSelected(true);
                }
            }
        };
        ActionListener rbSetOddFramesChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (rbSetOddFrames.isSelected() == true) {
                    Common.livevideo_displayFramesMode = 1;
                    rbSetEvenFrames.setSelected(false);
                    rbSetAllFrames.setSelected(false);
                } else {
                    rbSetOddFrames.setSelected(true);
                }
            }
        };
        ActionListener rbSetEvenFramesChanged = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                if (rbSetEvenFrames.isSelected() == true) {
                    Common.livevideo_displayFramesMode = 2;
                    rbSetOddFrames.setSelected(false);
                    rbSetAllFrames.setSelected(false);
                } else {
                    rbSetEvenFrames.setSelected(true);
                }
            }
        };
        DocumentListener tfTimeBinSizeChanged = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                UpdateLiveVideoBinNumber();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                UpdateLiveVideoBinNumber();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
            }
        };
        ActionListener cbDisplayTimeBinningModeChanged =
                new ActionListener() {//cbDisplayTimeBinningMode.getSelectedItem().toString().equals(Common
                    // .$livevideo_binningMode[0]
                    @Override
                    public void actionPerformed(
                            ActionEvent e) {//(cbMode.getSelectedItem().toString().equals(mode.getStringValue
                        // (modeEnum.SINGLECAPTURE.getValue()))

                        // No binning
                        if (cbDisplayTimeBinningMode.getSelectedItem()
                                .toString()
                                .equals(liveVideoBinMode.getStringValue(liveVideoBinModeEnum.NO_BINNING.getValue()))) {
                            tfTimeBinSize.setText(Integer.toString(1));
                            tfTimeBinSize.setEditable(false);
                            Common.selected_livevideo_binningMode = liveVideoBinModeEnum.NO_BINNING;
                        }

                        // Average binning
                        if (cbDisplayTimeBinningMode.getSelectedItem()
                                .toString()
                                .equals(liveVideoBinMode.getStringValue(
                                        liveVideoBinModeEnum.AVERAGE_BINNING.getValue()))) {
                            tfTimeBinSize.setEditable(true);
                            Common.selected_livevideo_binningMode = liveVideoBinModeEnum.AVERAGE_BINNING;
                        }

                        // Sum binning
                        if (cbDisplayTimeBinningMode.getSelectedItem()
                                .toString()
                                .equals(liveVideoBinMode.getStringValue(liveVideoBinModeEnum.SUM_BINNING.getValue()))) {
                            tfTimeBinSize.setEditable(true);
                            Common.selected_livevideo_binningMode = liveVideoBinModeEnum.SUM_BINNING;
                        }

                    }
                };

        public JLiveVideoPanelComponent() {

            //initialize (LiveVideoDisplayPane)
            LiveVideoDisplayPane = new JPanel(new GridLayout(1, 3));
            LiveVideoDisplayPane.setBorder(BorderFactory.createTitledBorder("Live video frame display option"));
            rbSetAllFrames = new JRadioButton("All frames", (Common.livevideo_displayFramesMode == 0));
            rbSetOddFrames = new JRadioButton("Odd frames", (Common.livevideo_displayFramesMode == 1));
            rbSetEvenFrames = new JRadioButton("Even frames", (Common.livevideo_displayFramesMode == 2));

            //initialize (LiveVideoBinningPane)
            LiveVideoBinningPane = new JPanel(new GridLayout(1, 3));
            LiveVideoBinningPane.setBorder(BorderFactory.createTitledBorder("Live video frame binning option"));

            cbDisplayTimeBinningMode = new JComboBox<>();
            for (int i = 0; i < liveVideoBinMode.size(); i++) {
                cbDisplayTimeBinningMode.addItem(liveVideoBinMode.getStringValue(i));
            }
            Common.selected_livevideo_binningMode =
                    liveVideoBinModeEnum.getEnum(cbDisplayTimeBinningMode.getSelectedItem().toString());

            tfTimeBinSize = new JTextField(Integer.toString(Common.livevideo_binningNo), 4);
            tfTimeBinSize.setEditable(false);

            //Add element(LiveVideoDisplayPane)
            LiveVideoDisplayPane.add(rbSetAllFrames);
            LiveVideoDisplayPane.add(rbSetOddFrames);
            LiveVideoDisplayPane.add(rbSetEvenFrames);

            //Add element(LiveVideoBinningPane)
            LiveVideoBinningPane.add(new JLabel("No frame bin:"));
            LiveVideoBinningPane.add(tfTimeBinSize);
            LiveVideoBinningPane.add(cbDisplayTimeBinningMode);

            //Listener
            rbSetAllFrames.addActionListener(rbSetAllFramesChanged);
            rbSetOddFrames.addActionListener(rbSetOddFramesChanged);
            rbSetEvenFrames.addActionListener(rbSetEvenFramesChanged);
            cbDisplayTimeBinningMode.addActionListener(cbDisplayTimeBinningModeChanged);
            tfTimeBinSize.getDocument().addDocumentListener(tfTimeBinSizeChanged);

            Container cp = this.getContentPane();
            cp.setLayout(new BorderLayout(1, 1));
            cp.add(LiveVideoDisplayPane, BorderLayout.NORTH);
            cp.add(LiveVideoBinningPane, BorderLayout.SOUTH);

            setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
            setTitle("Live Video Panel");
            setSize(400, 150);
            setLocation(new Point(500, 500));
            setFocusable(true);
            setResizable(false);
            setVisible(false);

        }

        private void UpdateLiveVideoBinNumber() {

            Runnable doUpdateLiveVideoBinNumber = new Runnable() {

                @Override
                public void run() {

                    boolean proceed = false;
                    int memint;
                    try {
                        memint = Integer.parseInt(tfTimeBinSize.getText());
                        proceed = true;
                    } catch (NumberFormatException nfe) {
                        IJ.log("video bin number nfe.");
                        tfTimeBinSize.setText(Integer.toString(Common.livevideo_binningNo));
                        IJ.log("catch Common.livevideo_binningNo: " + Common.livevideo_binningNo);
                        return;
                    }

                    if (memint < 1 || memint > 50) {
                        proceed = false;
                    }

                    if (proceed) {
                        Common.livevideo_binningNo = memint;
                    } else {
                        tfTimeBinSize.setText(Integer.toString(Common.livevideo_binningNo));
                    }
                }

            };
            SwingUtilities.invokeLater(doUpdateLiveVideoBinNumber);
        }

    }

}
