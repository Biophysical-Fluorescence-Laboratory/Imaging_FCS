package fiji.plugin.imaging_fcs.directCameraReadout.cameras;

import fiji.plugin.imaging_fcs.directCameraReadout.control.FrameCounter;
import fiji.plugin.imaging_fcs.directCameraReadout.control.FrameCounterX;
import fiji.plugin.imaging_fcs.directCameraReadout.gui.DirectCapturePanel;
import fiji.plugin.imaging_fcs.directCameraReadout.workers.Workers;
import fiji.plugin.imaging_fcs.imfcs.utils.LibraryLoader;
import ij.IJ;
import ij.ImagePlus;
import ij.ImageStack;
import ij.process.ShortProcessor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import static fiji.plugin.imaging_fcs.directCameraReadout.workers.Workers.*;
import static fiji.plugin.imaging_fcs.version.VERSION.DCR_VERSION;
import static fiji.plugin.imaging_fcs.version.VERSION.SDK2_VERSION;

public class AndorSDK2v3 extends Camera {
    private final static int impwinposx = 1110;
    private final static int impwinposy = 125;
    //SDK2
    private final static String NATIVE_LIBRARY_PATH = "/libs/camera_readout/sdk2/";
    private final static String ATMCD64D = "atmcd64d.dll";
    private final static String JNI_ANDOR_LIB = "JNIAndorSDK2v3.dll";
    private final static String[] DLL_DEPENDENCIES = {
            "ucrtbased.dll", "kernel32.dll", "vcruntime140d.dll", "vcruntime140_1d.dll", "msvcp140d.dll"
    };

    public AndorSDK2v3() {
        super();
        try {
            loadNativeLibraries();
        } catch (IOException e) {
            IJ.log("Error during native library loading: " + e.getMessage());
        }
    }

    //-------------------------------------------------------------------------
    // Native method declarations
    //-------------------------------------------------------------------------
    private static native boolean InitializeEMCCDSDK2(); //Initialize SDK2 and turn on cooler

    private static native void SystemShutDownSDK2();//Turn off cooler and shutdown sdk2

    private static native void ShutDownSDK2(); // trigger ShutDown() API function

    private static native int[] getMinMaxTemperatureSDK2();

    private static native void SetTemperatureSDK2(int temp);

    private static native void SetCoolingSDK2(int iscooling);

    private static native int[] GetTemperatureAndStatusSDK2(); ////20037 temp not reached //20035 temp not stabilized

    private static native int SetFanModeSDK2(int iFan);

    private static native void DoCameraAcquisitionSDK2(); //Trigger acquisition

    private static native void setStopMechanismSDK2(boolean isStopCalled);

    private static native int isEMCCDconnectedSDK2();//20002 SUCCESS; 20990 NO CAMERA CONNECTED; 20992 Other
    // 20034 temp off //20036 temp stabilized

    private static native void setParameterSingleSDK2(float exposuretime, int width, int height, int left, int top,
                                                      int acqmode, int gain, int incamerabinning, int ixonmodel,
                                                      int iVspeed, int iVamp, int iHspeed, int iPreAmpGain,
                                                      int isCropMode, int croppedWidth, int croppedHeight,
                                                      int croppedLeft,
                                                      int croppedTop);// acqMode = 1 for single scan; AcqMode = 5 for

    private static native short[] runSingleScanSDK2();

    private static native boolean setParameterInfiniteLoopSDK2(int size_b, int transferFrameInterval,
                                                               float exposureTime, int width, int height, int left,
                                                               int top, int acqmode, int gain, int incamerabinning,
                                                               int ixonmodel, int iVspeed, int iVamp, int iHspeed,
                                                               int iPreAmpGain, int isCropMode, int croppedWidth,
                                                               int croppedHeight, int croppedLeft, int croppedTop,
                                                               int arraysize);

    private static synchronized native void runInfiniteLoopSDK2(short[] outArray, FrameCounter fcObj);
    // application such as Andor Solis/uManager is accessing

    private static native boolean setParameterContinuousAcquisitionSDK2(int size_b, int totalFrame,
                                                                        int transferFrameInterval,
                                                                        float exposureTimeCont, int width, int height,
                                                                        int left, int top, int acqmode, int gain,
                                                                        int incamerabinning, int ixonmodel, int iVspeed,
                                                                        int iVamp, int iHspeed, int iPreAmpGain,
                                                                        int isCropMode, int croppedWidth,
                                                                        int croppedHeight, int croppedLeft,
                                                                        int croppedTop, int arraysize);
    // runtillAbort

    private static synchronized native void runContinuousScanAcquisitionSDK2(short[] outArray, FrameCounter fcObj);

    private static native boolean setParameterInfiniteLoopV2SDK2(int size_b, int totalFrame, float exposureTime,
                                                                 int width, int height, int left, int top, int acqmode,
                                                                 int gain, int incamerabinning, int ixonmodel,
                                                                 int iVspeed, int iVamp, int iHspeed, int iPreAmpGain,
                                                                 int isCropMode, int croppedWidth, int croppedHeight,
                                                                 int croppedLeft, int croppedTop); //UNUSED

    private static native int[] getDetectorDimensionSDK2();

    private static native int getEMGainSDK2();

    private static native int getFrameTransferSDK2();

    private static native float getPreAmpGainSDK2();

    private static native float getVSSpeedSDK2();

    private static native int getVSClockVoltageSDK2();

    private static native int getnADchannelsSDK2();

    private static native float getHSSpeedSDK2();

    private static native int getNoAvailableHSSpeedSDK2();

    private static native float getFastestVerticalSpeedSDK2();

    private static native float getKineticCycleSDK2(); // getter; kinetic cycle != exposure time set

    private static native float getExposureTimeSDK2();

    private static native int getBaseLineClampStateSDK2();

    private static native int getBaseLineOffsetSDK2();

    private static native int getBitDepthSDK2(); //get bit depth per pixel for a particular AD converter

    private static native int getisCoolerOnSDK2();

    private static native int getWidthSDK2();

    private static native int getHeightSDK2();

    private static native int getLeftSDK2(); // index at 1

    private static native int getTopSDK2(); // index at 1

    private static native int getCameraSerialNumSDK2();

    private static native String getHeadModelSDK2();

    private static native String[] GetAvailableVSAmplitudeSDK2();//V

    private static native String[] GetAvailableVSSpeedsSDK2();//V

    private static native String[] GetAvailableHSSpeedsSDK2(); //V

    private static native String[] GetAvailablePreAmpGainSDK2();// 99 means NA

    private static native void ShutterControlSDK2(boolean isOpen);
    //888: Normal, +1, +2, +3, +4
    //886: Normal, +1, +2, +3, +4

    private void loadNativeLibraries() throws IOException {
        LibraryLoader.loadSystemLibraries(NATIVE_LIBRARY_PATH, DLL_DEPENDENCIES);
        LibraryLoader.loadNativeLibraries(NATIVE_LIBRARY_PATH, ATMCD64D, JNI_ANDOR_LIB);
    }
    //888: 0.6, 1.13, 2.20, 4.33
    //860: 0.09, 0.10, 0.15, 0.25, 0.45

    /*
    JNI EMCCD SDK2 ends here
     */
    private void runThread_updatetemp() {
        SwingWorker<Void, List<Integer>> worker = new SwingWorker<Void, List<Integer>>() {
            @Override
            protected Void doInBackground() throws Exception {

                while (!DirectCapturePanel.Common.isShutSystemPressed) {
                    DirectCapturePanel.Common.tempStatus = GetTemperatureAndStatusSDK2();
                    List<Integer> list = Arrays.asList(0, 0);
                    list.set(0, DirectCapturePanel.Common.tempStatus[0]);
                    list.set(1, DirectCapturePanel.Common.tempStatus[1]);
                    publish(list);
                    Thread.sleep(5000);
                }
                return null;
            }

            @Override
            protected void process(List<List<Integer>> chunks) {
                List<Integer> tempandstatusList = chunks.get(chunks.size() - 1);
                Integer currtemp = tempandstatusList.get(0);
                Integer currtempstatus = tempandstatusList.get(1);
                DirectCapturePanel.tfTemperature.setText(Integer.toString(currtemp) + " " + (char) 186 + " C");
                if (currtempstatus ==
                        20036) {//20037 temp not reached //20035 temp not stabilized //20034 temp off //20036 temp
                    // stabilized
                    DirectCapturePanel.tfTemperature.setBackground(Color.BLUE);
                } else if (currtempstatus == 20034) {
                    DirectCapturePanel.tfTemperature.setBackground(Color.BLACK);
                } else {
                    DirectCapturePanel.tfTemperature.setBackground(Color.RED);
                }
            }
        };
        worker.execute();
    }
    //888: 30, 20, 10, 1
    //860: 10, 5, 3

    // Working Version (live video mode)
    private void runThreadLiveVideo() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                Thread.currentThread().setName("runThread_livevideoV2");

                final int noThread = 3; // number of working threads for LiveVideo Mode
                final int fbuffersize =
                        DirectCapturePanel.Common.size_a * DirectCapturePanel.Common.size_b; // number of frame

                // Control flow reset, buffer reset
                DirectCapturePanel.Common.arraysize =
                        fbuffersize * DirectCapturePanel.Common.tempWidth * DirectCapturePanel.Common.tempHeight;
                long timer1 = System.currentTimeMillis();
                DirectCapturePanel.Common.bufferArray1D = new short[DirectCapturePanel.Common.arraysize];
                DirectCapturePanel.Common.framecounter = new FrameCounter();
                CountDownLatch latch = new CountDownLatch(noThread);

                // JNI call SetParameter
                timer1 = System.currentTimeMillis();
                setParameterInfiniteLoopSDK2(fbuffersize, DirectCapturePanel.Common.transferFrameInterval,
                        (float) DirectCapturePanel.Common.exposureTime, DirectCapturePanel.Common.oWidth,
                        DirectCapturePanel.Common.oHeight, DirectCapturePanel.Common.oLeft,
                        DirectCapturePanel.Common.oTop, 5, DirectCapturePanel.Common.EMgain,
                        DirectCapturePanel.Common.inCameraBinning, DirectCapturePanel.cameraint,
                        DirectCapturePanel.Common.iVSpeed, DirectCapturePanel.Common.iVSamp,
                        DirectCapturePanel.Common.iHSpeed, DirectCapturePanel.Common.iPreamp,
                        DirectCapturePanel.Common.isCropMode, DirectCapturePanel.Common.cWidth,
                        DirectCapturePanel.Common.cHeight, DirectCapturePanel.Common.cLeft,
                        DirectCapturePanel.Common.cTop,
                        DirectCapturePanel.Common.arraysize); //Setting parameter for infinite loop recordimg
                DirectCapturePanel.Common.kineticCycleTime = getKineticCycleSDK2();
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f",
                        DirectCapturePanel.Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                DoCameraAcquisitionSDK2(); // Trigger

                CppToJavaTransferInfWorkerEXTENDEDV2 CppToJavaTransferInfWorkerEXTENDEDV2Instant =
                        new CppToJavaTransferInfWorkerEXTENDEDV2(DirectCapturePanel.Common.bufferArray1D, latch);
                LiveVideoWorkerV2Instant = new Workers.LiveVideoWorkerV2(DirectCapturePanel.Common.tempWidth,
                        DirectCapturePanel.Common.tempHeight, latch);
                SynchronizerWorkerInstant = new Workers.SynchronizerWorker(latch);

                long timeelapse = System.nanoTime();
                CppToJavaTransferInfWorkerEXTENDEDV2Instant.execute();
                LiveVideoWorkerV2Instant.execute();
                SynchronizerWorkerInstant.execute();

                latch.await();
                System.out.println("***Live V2***");
                System.out.println(
                        "Java Time elapse: " + (System.nanoTime() - timeelapse) / 1000000 + " ms; kinetic cycle: " +
                                DirectCapturePanel.Common.kineticCycleTime);

                return null;
            }

            @Override
            protected void done() {
                DirectCapturePanel.Common.isAcquisitionRunning = false;
                DirectCapturePanel.tfExposureTime.setEditable(true);
                System.out.println("Native counter cpp: " + DirectCapturePanel.Common.framecounter.getCounter() +
                        ", time overall: " + DirectCapturePanel.Common.framecounter.time1 +
                        ", time average readbuffer combin: " + DirectCapturePanel.Common.framecounter.time2 +
                        ", time JNI copy: " + DirectCapturePanel.Common.framecounter.time3 + ", JNI transfer: " +
                        (DirectCapturePanel.Common.tempWidth * DirectCapturePanel.Common.tempHeight * 2 /
                                DirectCapturePanel.Common.framecounter.time3) + " MBps, fps(cap): " +
                        (1 / DirectCapturePanel.Common.framecounter.time3));
                System.out.println("***");
                System.out.println("");
                IJ.showMessage("Live done");
            }

        };
        worker.execute();
    }
    //888: 1, 2, 99 (for 888 lets set to Gain1 and Gain2
    //860: 1, 2.2, 4.5

    // Working Version (calibration mode)
    private void runThreadNonCumulative() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                Thread.currentThread().setName("runThread_noncumulativeV3");

                final int noThread = 3; // number of working threads
                final int fbuffersize =
                        DirectCapturePanel.Common.size_a * DirectCapturePanel.Common.size_b; // number of frame

                // Control flow reset, buffer reset
                DirectCapturePanel.Common.arraysize =
                        fbuffersize * DirectCapturePanel.Common.tempWidth * DirectCapturePanel.Common.tempHeight;
                long timer1 = System.currentTimeMillis();
                DirectCapturePanel.Common.bufferArray1D = new short[DirectCapturePanel.Common.arraysize];
                DirectCapturePanel.Common.framecounter = new FrameCounter();
                CountDownLatch latch = new CountDownLatch(noThread);

                // JNI call SetParameter
                timer1 = System.currentTimeMillis();
                setParameterInfiniteLoopSDK2(fbuffersize, DirectCapturePanel.Common.transferFrameInterval,
                        (float) DirectCapturePanel.Common.exposureTime, DirectCapturePanel.Common.oWidth,
                        DirectCapturePanel.Common.oHeight, DirectCapturePanel.Common.oLeft,
                        DirectCapturePanel.Common.oTop, 5, DirectCapturePanel.Common.EMgain,
                        DirectCapturePanel.Common.inCameraBinning, DirectCapturePanel.cameraint,
                        DirectCapturePanel.Common.iVSpeed, DirectCapturePanel.Common.iVSamp,
                        DirectCapturePanel.Common.iHSpeed, DirectCapturePanel.Common.iPreamp,
                        DirectCapturePanel.Common.isCropMode, DirectCapturePanel.Common.cWidth,
                        DirectCapturePanel.Common.cHeight, DirectCapturePanel.Common.cLeft,
                        DirectCapturePanel.Common.cTop,
                        DirectCapturePanel.Common.arraysize); //Setting parameter for infinite loop recordimg
                // Receive real kinetic cycle and display in GUI
                DirectCapturePanel.Common.kineticCycleTime = getKineticCycleSDK2();
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f",
                        DirectCapturePanel.Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                DoCameraAcquisitionSDK2(); // Trigger

                CppToJavaTransferInfWorkerEXTENDEDV2 CppToJavaTransferInfWorkerEXTENDEDV2Instant =
                        new CppToJavaTransferInfWorkerEXTENDEDV2(DirectCapturePanel.Common.bufferArray1D, latch);
                LiveVideoWorkerV3Instant =
                        new LiveVideoWorkerV3(DirectCapturePanel.Common.tempWidth, DirectCapturePanel.Common.tempHeight,
                                latch);
                NonCumulativeACFWorkerV3Instant = new NonCumulativeACFWorkerV3(DirectCapturePanel.Common.tempWidth,
                        DirectCapturePanel.Common.tempHeight, latch, DirectCapturePanel.Common.arraysize);

                long timeelapse = System.nanoTime();
                CppToJavaTransferInfWorkerEXTENDEDV2Instant.execute();
                LiveVideoWorkerV3Instant.execute();
                NonCumulativeACFWorkerV3Instant.execute();

                latch.await();
                System.out.println("***Calibration V3***");
                System.out.println(
                        "Java Time elapse: " + (System.nanoTime() - timeelapse) / 1000000 + " ms; kinetic cycle: " +
                                DirectCapturePanel.Common.kineticCycleTime);

                return null;
            }

            @Override
            protected void done() {
                DirectCapturePanel.Common.isAcquisitionRunning = false;
                DirectCapturePanel.tfExposureTime.setEditable(true);
                System.out.println("Native counter cpp: " + DirectCapturePanel.Common.framecounter.getCounter() +
                        ", time overall: " + DirectCapturePanel.Common.framecounter.time1 +
                        ", time average readbuffer combin: " + DirectCapturePanel.Common.framecounter.time2 +
                        ", time JNI copy: " + DirectCapturePanel.Common.framecounter.time3 + ", JNI transfer: " +
                        (DirectCapturePanel.Common.tempWidth * DirectCapturePanel.Common.tempHeight * 2 /
                                DirectCapturePanel.Common.framecounter.time3) + " MBps, fps(cap): " +
                        (1 / DirectCapturePanel.Common.framecounter.time3));
                System.out.println("***");
                System.out.println("");
                IJ.showMessage("Calibration done");
            }

        };
        worker.execute();
    }

    // Working Version (acquisition mode)
    private void runThreadCumulative() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {

                Thread.currentThread().setName("runThread_noncumulativeV3");

                final int noThread = 5; // number of working threads
                final int fbuffersize =
                        DirectCapturePanel.Common.size_a * DirectCapturePanel.Common.size_b; // number of frame

                // Control flow reset, buffer reset
                DirectCapturePanel.Common.arraysize =
                        fbuffersize * DirectCapturePanel.Common.tempWidth * DirectCapturePanel.Common.tempHeight;
                long timer1 = System.currentTimeMillis();
                DirectCapturePanel.Common.bufferArray1D = new short[DirectCapturePanel.Common.arraysize];
                DirectCapturePanel.Common.ims_cum =
                        new ImageStack(DirectCapturePanel.Common.tempWidth, DirectCapturePanel.Common.tempHeight);
                DirectCapturePanel.Common.framecounterIMSX = new FrameCounterX();
                DirectCapturePanel.Common.framecounter = new FrameCounter();
                CountDownLatch latch = new CountDownLatch(noThread);

                // JNI call SetParameter
                timer1 = System.currentTimeMillis();
                setParameterContinuousAcquisitionSDK2(fbuffersize, DirectCapturePanel.Common.totalFrame,
                        DirectCapturePanel.Common.transferFrameInterval, (float) DirectCapturePanel.Common.exposureTime,
                        DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight,
                        DirectCapturePanel.Common.oLeft, DirectCapturePanel.Common.oTop, 5,
                        DirectCapturePanel.Common.EMgain, DirectCapturePanel.Common.inCameraBinning,
                        DirectCapturePanel.cameraint, DirectCapturePanel.Common.iVSpeed,
                        DirectCapturePanel.Common.iVSamp, DirectCapturePanel.Common.iHSpeed,
                        DirectCapturePanel.Common.iPreamp, DirectCapturePanel.Common.isCropMode,
                        DirectCapturePanel.Common.cWidth, DirectCapturePanel.Common.cHeight,
                        DirectCapturePanel.Common.cLeft, DirectCapturePanel.Common.cTop,
                        DirectCapturePanel.Common.arraysize);

                // Receive real kinetic cycle and display in GUI
                DirectCapturePanel.Common.kineticCycleTime = getKineticCycleSDK2();
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f",
                        DirectCapturePanel.Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                DoCameraAcquisitionSDK2(); // Trigger

                CppToJavaTransferAcqWorkerEXTENDEDV2 CppToJavaTransferAcqWorkerEXTENDEDV2Instant =
                        new CppToJavaTransferAcqWorkerEXTENDEDV2(DirectCapturePanel.Common.bufferArray1D, latch);
                LiveVideoWorkerV3Instant =
                        new LiveVideoWorkerV3(DirectCapturePanel.Common.tempWidth, DirectCapturePanel.Common.tempHeight,
                                latch);
                BufferToStackWorkerInstant = new BufferToStackWorker(DirectCapturePanel.Common.tempWidth,
                        DirectCapturePanel.Common.tempHeight, DirectCapturePanel.Common.totalFrame, latch,
                        DirectCapturePanel.Common.arraysize);
                CumulativeACFWorkerV3Instant = new CumulativeACFWorkerV3(latch);
                NonCumulativeACFWorkerV3Instant = new NonCumulativeACFWorkerV3(DirectCapturePanel.Common.tempWidth,
                        DirectCapturePanel.Common.tempHeight, latch, DirectCapturePanel.Common.arraysize);

                long timeelapse = System.nanoTime();
                CppToJavaTransferAcqWorkerEXTENDEDV2Instant.execute();
                LiveVideoWorkerV3Instant.execute();
                BufferToStackWorkerInstant.execute();
                CumulativeACFWorkerV3Instant.execute();
                NonCumulativeACFWorkerV3Instant.execute();

                latch.await();
                System.out.println("***Acquisition V3***");
                System.out.println(
                        "Java Time elapse: " + (System.nanoTime() - timeelapse) / 1000000 + " ms; kinetic cycle: " +
                                DirectCapturePanel.Common.kineticCycleTime + ", totalframe: " +
                                DirectCapturePanel.Common.ims_cum.getSize());

                return null;

            }

            @Override
            protected void done() {
                DirectCapturePanel.Common.isAcquisitionRunning = false;
                DirectCapturePanel.tbStartStop.setSelected(false);
                DirectCapturePanel.tfTotalFrame.setEditable(true);
                DirectCapturePanel.tfExposureTime.setEditable(true);
                DirectCapturePanel.Common.fitStartCumulative = 1;
                DirectCapturePanel.tfCumulativeFitStart.setText(
                        Integer.toString(DirectCapturePanel.Common.fitStartCumulative));
                System.out.println("Native counter cpp: " + DirectCapturePanel.Common.framecounter.getCounter() +
                        ", time overall: " + DirectCapturePanel.Common.framecounter.time1 +
                        ", time average readbuffer combin: " + DirectCapturePanel.Common.framecounter.time2 +
                        ", time JNI copy: " + DirectCapturePanel.Common.framecounter.time3 + ", JNI transfer: " +
                        (DirectCapturePanel.Common.tempWidth * DirectCapturePanel.Common.tempHeight * 2 /
                                DirectCapturePanel.Common.framecounter.time3) + " MBps, fps(cap): " +
                        (1 / DirectCapturePanel.Common.framecounter.time3) + "Common.framecounterIMSX: " +
                        DirectCapturePanel.Common.framecounterIMSX.getCount());
                System.out.println("***");
                System.out.println("");
                IJ.showMessage("Acquisition done");

            }

        };
        worker.execute();
    }

    // Working Version (ICCS mode)
    private void runThreadICCS() {
        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                Thread.currentThread().setName("runThreadICCS");

                final int noThread = 3; //number of working threads
                final int fbuffersize = DirectCapturePanel.Common.size_a * DirectCapturePanel.Common.size_b;

                // Control flow reset, buffer reset
                DirectCapturePanel.Common.arraysize =
                        fbuffersize * DirectCapturePanel.Common.tempWidth * DirectCapturePanel.Common.tempHeight;
                long timer1 = System.currentTimeMillis();
                DirectCapturePanel.Common.bufferArray1D = new short[DirectCapturePanel.Common.arraysize];
                DirectCapturePanel.Common.framecounter = new FrameCounter();
                CountDownLatch latch = new CountDownLatch(noThread);

                // JNI call setParameter
                timer1 = System.currentTimeMillis();
                setParameterInfiniteLoopSDK2(fbuffersize, DirectCapturePanel.Common.transferFrameInterval,
                        (float) DirectCapturePanel.Common.exposureTime, DirectCapturePanel.Common.oWidth,
                        DirectCapturePanel.Common.oHeight, DirectCapturePanel.Common.oLeft,
                        DirectCapturePanel.Common.oTop, 5, DirectCapturePanel.Common.EMgain,
                        DirectCapturePanel.Common.inCameraBinning, DirectCapturePanel.cameraint,
                        DirectCapturePanel.Common.iVSpeed, DirectCapturePanel.Common.iVSamp,
                        DirectCapturePanel.Common.iHSpeed, DirectCapturePanel.Common.iPreamp,
                        DirectCapturePanel.Common.isCropMode, DirectCapturePanel.Common.cWidth,
                        DirectCapturePanel.Common.cHeight, DirectCapturePanel.Common.cLeft,
                        DirectCapturePanel.Common.cTop,
                        DirectCapturePanel.Common.arraysize); //Setting parameter for infinite loop recordimg
                // Receive real kinetic cycle and display in GUI
                DirectCapturePanel.Common.kineticCycleTime = getKineticCycleSDK2();
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f",
                        DirectCapturePanel.Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                DoCameraAcquisitionSDK2(); // Trigger

                CppToJavaTransferInfWorkerEXTENDEDV2 CppToJavaTransferInfWorkerEXTENDEDV2Instant =
                        new CppToJavaTransferInfWorkerEXTENDEDV2(DirectCapturePanel.Common.bufferArray1D, latch);
                LiveVideoWorkerV3Instant =
                        new LiveVideoWorkerV3(DirectCapturePanel.Common.tempWidth, DirectCapturePanel.Common.tempHeight,
                                latch);
                ICCSWorkerInstant =
                        new ICCSWorker(DirectCapturePanel.Common.tempWidth, DirectCapturePanel.Common.tempHeight, latch,
                                DirectCapturePanel.Common.arraysize);

                long timelapse = System.nanoTime();
                CppToJavaTransferInfWorkerEXTENDEDV2Instant.execute();
                LiveVideoWorkerV3Instant.execute();
                ICCSWorkerInstant.execute();

                latch.await();
                System.out.println("***ICCS***");
                System.out.println(
                        "Java Time elapse: " + (System.nanoTime() - timelapse) / 1000000 + " ms; kinetic cycle: " +
                                DirectCapturePanel.Common.kineticCycleTime);

                return null;

            }

            @Override
            protected void done() {
                DirectCapturePanel.Common.isAcquisitionRunning = false;
                DirectCapturePanel.tfExposureTime.setEditable(true);
                ICCSWorkerInstant.setNullICCS();

                System.out.println("Native counter cpp: " + DirectCapturePanel.Common.framecounter.getCounter() +
                        ", time overall: " + DirectCapturePanel.Common.framecounter.time1 +
                        ", time average readbuffer combin: " + DirectCapturePanel.Common.framecounter.time2 +
                        ", time JNI copy: " + DirectCapturePanel.Common.framecounter.time3 + ", JNI transfer: " +
                        (DirectCapturePanel.Common.tempWidth * DirectCapturePanel.Common.tempHeight * 2 /
                                DirectCapturePanel.Common.framecounter.time3) + " MBps, fps(cap): " +
                        (1 / DirectCapturePanel.Common.framecounter.time3));
                System.out.println("***");
                System.out.println("");
                IJ.showMessage("ICCS done");

            }

        };
        worker.execute();
    }

    // Working Version (single capture mode)
    private void runThread_singlecapture(boolean isFF) {

        if (isFF) {
            //Recall previous ROI setting
            DirectCapturePanel.Common.mem_oWidth = DirectCapturePanel.Common.oWidth;
            DirectCapturePanel.Common.mem_oHeight = DirectCapturePanel.Common.oHeight;
            DirectCapturePanel.Common.mem_oLeft = DirectCapturePanel.Common.oLeft;
            DirectCapturePanel.Common.mem_oTop = DirectCapturePanel.Common.oTop;

            //Set setting for maximum coverage before capturing a single image for display
            DirectCapturePanel.Common.oWidth =
                    DirectCapturePanel.Common.MAXpixelwidth / DirectCapturePanel.Common.inCameraBinning;
            DirectCapturePanel.Common.oHeight =
                    DirectCapturePanel.Common.MAXpixelheight / DirectCapturePanel.Common.inCameraBinning;
            DirectCapturePanel.Common.oLeft = 1;
            DirectCapturePanel.Common.oTop = 1;
        }

        SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                setParameterSingleSDK2((float) DirectCapturePanel.Common.exposureTime, DirectCapturePanel.Common.oWidth,
                        DirectCapturePanel.Common.oHeight, DirectCapturePanel.Common.oLeft,
                        DirectCapturePanel.Common.oTop, 1, DirectCapturePanel.Common.EMgain,
                        DirectCapturePanel.Common.inCameraBinning, DirectCapturePanel.cameraint,
                        DirectCapturePanel.Common.iVSpeed, DirectCapturePanel.Common.iVSamp,
                        DirectCapturePanel.Common.iHSpeed, DirectCapturePanel.Common.iPreamp,
                        DirectCapturePanel.Common.isCropMode, DirectCapturePanel.Common.cWidth,
                        DirectCapturePanel.Common.cHeight, DirectCapturePanel.Common.cLeft,
                        DirectCapturePanel.Common.cTop);
                DirectCapturePanel.Common.kineticCycleTime = getKineticCycleSDK2(); // get real kinetic cycle
                DirectCapturePanel.tfExposureTime.setText(String.format("%.6f",
                        DirectCapturePanel.Common.kineticCycleTime));// update real kinetic cycle time [s] to GUI
                DirectCapturePanel.Common.arraysingleS = runSingleScanSDK2();

                //Update Display Image for ROI ifcapturing full frame or full crop mode
                int newX = (int) Math.floor(
                        DirectCapturePanel.Common.MAXpixelwidth / DirectCapturePanel.Common.inCameraBinning);
                int newY = (int) Math.floor(
                        DirectCapturePanel.Common.MAXpixelheight / DirectCapturePanel.Common.inCameraBinning);

                if (newX * newY == DirectCapturePanel.Common.arraysingleS.length) {//Fullframe captured
                    int[] tempIntArray = new int[DirectCapturePanel.Common.arraysingleS.length];
                    for (int i = 0; i < DirectCapturePanel.Common.arraysingleS.length; i++) {
                        tempIntArray[i] = (int) DirectCapturePanel.Common.arraysingleS[i];
                    }
                    DirectCapturePanel.DisplayImageObj.updateImage(tempIntArray,
                            DirectCapturePanel.Common.inCameraBinning, DirectCapturePanel.Common.MAXpixelwidth,
                            DirectCapturePanel.Common.MAXpixelheight, DirectCapturePanel.Common.isCropMode);
                }

                if (!isFF) {// display extra images onto Fiji
                    boolean alteredDim = false;
                    if (DirectCapturePanel.Common.ip != null) {
                        alteredDim =
                                (DirectCapturePanel.Common.ip.getHeight() * DirectCapturePanel.Common.ip.getWidth()) !=
                                        DirectCapturePanel.Common.arraysingleS.length;
                    }

                    if (DirectCapturePanel.Common.impwin == null || DirectCapturePanel.Common.imp == null ||
                            (DirectCapturePanel.Common.impwin != null && DirectCapturePanel.Common.imp != null) &&
                                    DirectCapturePanel.Common.impwin.isClosed() || alteredDim) {
                        DirectCapturePanel.Common.ip =
                                new ShortProcessor(DirectCapturePanel.Common.oWidth, DirectCapturePanel.Common.oHeight);
                    }

                    for (int y = 0; y < DirectCapturePanel.Common.oHeight; y++) {
                        for (int x = 0; x < DirectCapturePanel.Common.oWidth; x++) {
                            int index = (y * DirectCapturePanel.Common.oWidth) + x;
                            DirectCapturePanel.Common.ip.putPixel(x, y,
                                    (int) DirectCapturePanel.Common.arraysingleS[index]);
                        }
                    }

                    if (DirectCapturePanel.Common.impwin == null || DirectCapturePanel.Common.imp == null ||
                            (DirectCapturePanel.Common.impwin != null && DirectCapturePanel.Common.imp != null) &&
                                    DirectCapturePanel.Common.impwin.isClosed() || alteredDim) {
                        if (DirectCapturePanel.Common.impwin != null) {
                            DirectCapturePanel.Common.impwin.close();
                        }
                        DirectCapturePanel.Common.imp = new ImagePlus("Single Scan", DirectCapturePanel.Common.ip);
                        DirectCapturePanel.Common.imp.show();

                        DirectCapturePanel.Common.impwin = DirectCapturePanel.Common.imp.getWindow();
                        DirectCapturePanel.Common.impcan = DirectCapturePanel.Common.imp.getCanvas();
                        DirectCapturePanel.Common.impwin.setLocation(impwinposx, impwinposy);

                        //enlarge image to see better pixels
                        if (DirectCapturePanel.Common.oWidth >= DirectCapturePanel.Common.oHeight) {
                            DirectCapturePanel.Common.scimp = DirectCapturePanel.Common.zoomFactor /
                                    DirectCapturePanel.Common.oWidth; //adjustable: zoomFactor is by default 250 (see
                            // parameter definitions), a value chosen as it produces a good size on the screen
                        } else {
                            DirectCapturePanel.Common.scimp =
                                    DirectCapturePanel.Common.zoomFactor / DirectCapturePanel.Common.oHeight;
                        }
                        if (DirectCapturePanel.Common.scimp < 1.0) {
                            DirectCapturePanel.Common.scimp = 1.0;
                        }
                        DirectCapturePanel.Common.scimp *= 100;// transfrom this into %tage to run ImageJ command
                        IJ.run(DirectCapturePanel.Common.imp, "Original Scale", "");
                        IJ.run(DirectCapturePanel.Common.imp, "Set... ",
                                "zoom=" + DirectCapturePanel.Common.scimp + " x=" +
                                        (int) Math.floor(DirectCapturePanel.Common.oWidth / 2) + " y=" +
                                        (int) Math.floor(DirectCapturePanel.Common.oHeight / 2));
                        IJ.run("In [+]",
                                "");    // This needs to be used since ImageJ 1.48v to set the window to the right
                        // this might be a bug and is an ad hoc solution for the moment; before only the "Set"
                        // command was necessary

                        DirectCapturePanel.Common.impcan.setFocusable(true);
                    } else {
                        //(Common.impwin != null && Common.imp != null) && !Common.impwin.isClosed()
                        DirectCapturePanel.Common.ip.resetMinAndMax();
                        DirectCapturePanel.Common.imp.updateAndDraw();
                    }
                }

                return null;
            }

            @Override
            protected void done() {

                //Reset previos setting
                if (isFF) {
                    DirectCapturePanel.Common.oWidth = DirectCapturePanel.Common.mem_oWidth;
                    DirectCapturePanel.Common.oHeight = DirectCapturePanel.Common.mem_oHeight;
                    DirectCapturePanel.Common.oLeft = DirectCapturePanel.Common.mem_oLeft;
                    DirectCapturePanel.Common.oTop = DirectCapturePanel.Common.mem_oTop;
                }
            }
        };
        worker.execute();
    }

    @Override
    protected void initialize() {
        InitializeEMCCDSDK2();
    }

    @Override
    public void shutdown() {
        SystemShutDownSDK2();
    }

    @Override
    public void setTemperature(int temp) {
        SetTemperatureSDK2(temp);
    }

    @Override
    public void setCooling(boolean isCool) {
        SetCoolingSDK2(isCool ? 1 : 0);
    }

    @Override
    public void setFan(String fan) {
        int iFan = Arrays.asList(DirectCapturePanel.Common.FanList).indexOf(fan);
        int iRet = SetFanModeSDK2(iFan);
        if (iRet != 0) {
            IJ.log("Error setting fan mode error: " + iRet);
        }
    }

    @Override
    public int[] getMinMaxTemperature() {
        return getMinMaxTemperatureSDK2();
    }

    @Override
    public void updateTemperature() {
        runThread_updatetemp();
    }

    @Override
    public int[] getDetectorDimension() {
        return getDetectorDimensionSDK2();
    }

    @Override
    public void shutterControl(boolean isShutterOn) {
        ShutterControlSDK2(isShutterOn);
    }

    @Override
    public void singleCapture(boolean isFF) {
        runThread_singlecapture(isFF);
    }

    @Override
    public void liveVideoCapture() {
        runThreadLiveVideo();
    }

    @Override
    public void nonCumulativeCapture() {
        runThreadNonCumulative();
    }

    @Override
    public void cumulativeCapture() {
        runThreadCumulative();
    }

    @Override
    public void iccsCalibrationCapture() {
        runThreadICCS();
    }

    @Override
    public void setStopMechanism(boolean isStopPressed) {
        setStopMechanismSDK2(isStopPressed);
    }

    @Override
    public void writeExcel(File file, String exception, boolean showLog) {
        //There is a limit of 1,048,576 rows and 16,384 (corresponds to 128 x 128(
        //columns in xlsx file as of 2019

        File newFile;
        String $sfile = file.toString();
        int dotind = $sfile.lastIndexOf('.');
        if (dotind != -1) {
            $sfile = $sfile.substring(0, dotind);
        }
        newFile = new File($sfile + ".xlsx");

        XSSFWorkbook wb = new XSSFWorkbook();
        Sheet metaSheet = wb.createSheet("Metadata");

        Row row;

        //write Metadata
        int t;
        int nopm = 26;
        String[] metadatatag = new String[nopm];
        String[] metadatavalue = new String[nopm];
        t = 0;
        metadatatag[t++] = "SizeT";
        metadatatag[t++] = "sizeX";
        metadatatag[t++] = "sizeY";
        metadatatag[t++] = "Image Coordinate Left (index start at 1)"; // Index start at 1
        metadatatag[t++] = "Image Coordinate Top (index start at 1)"; // Index start at 1
        metadatatag[t++] = "Acquisition cycle time (in seconds)";
        metadatatag[t++] = "Exposure time (in seconds)";
        metadatatag[t++] = "Baseline clamp";
        metadatatag[t++] = "Baseline offset";
        metadatatag[t++] = "Frame transfer";
        metadatatag[t++] = "Camera Model";
        metadatatag[t++] = "Camera Serial";
        metadatatag[t++] = "Physical binning X";
        metadatatag[t++] = "Physical binning Y";
        metadatatag[t++] = "Chip size X";
        metadatatag[t++] = "Chip size Y";
        metadatatag[t++] = "EM DAC";
        metadatatag[t++] = "Pre-amp";
        metadatatag[t++] = "Readout rate (Mhz)";
        metadatatag[t++] = "Bit depth AD converter";
        metadatatag[t++] = "Vertical clock voltage";
        metadatatag[t++] = "Vertical shift speed (usecs)";
        metadatatag[t++] = "Actual Temperature (C)";
        metadatatag[t++] = "Software";
        metadatatag[t++] = "SDK";
        metadatatag[t++] = "Time Stamp";

        t = 0;
        metadatavalue[t++] = Integer.toString(DirectCapturePanel.Common.framecounterIMSX.getCount());
        metadatavalue[t++] = Integer.toString(getWidthSDK2());
        metadatavalue[t++] = Integer.toString(getHeightSDK2());
        metadatavalue[t++] = Integer.toString(getLeftSDK2());
        metadatavalue[t++] = Integer.toString(getTopSDK2());
        metadatavalue[t++] = Float.toString(getKineticCycleSDK2());
        metadatavalue[t++] = Float.toString(getExposureTimeSDK2());
        metadatavalue[t++] = Integer.toString(getBaseLineClampStateSDK2());
        metadatavalue[t++] = Integer.toString(getBaseLineOffsetSDK2());
        metadatavalue[t++] = Integer.toString(getFrameTransferSDK2());
        metadatavalue[t++] = getHeadModelSDK2();
        metadatavalue[t++] = Integer.toString(getCameraSerialNumSDK2());
        metadatavalue[t++] = Integer.toString(DirectCapturePanel.Common.inCameraBinning);
        metadatavalue[t++] = Integer.toString(DirectCapturePanel.Common.inCameraBinning);
        int[] chipsize = getDetectorDimensionSDK2();
        metadatavalue[t++] = Integer.toString(chipsize[0]);
        metadatavalue[t++] = Integer.toString(chipsize[1]);
        metadatavalue[t++] = Integer.toString(getEMGainSDK2());
        metadatavalue[t++] = Float.toString(getPreAmpGainSDK2());
        metadatavalue[t++] = Float.toString(getHSSpeedSDK2());
        metadatavalue[t++] = Integer.toString(getBitDepthSDK2());
        metadatavalue[t++] = Integer.toString(getVSClockVoltageSDK2());
        metadatavalue[t++] = Float.toString(getVSSpeedSDK2());
        metadatavalue[t++] = Integer.toString(GetTemperatureAndStatusSDK2()[0]);
        metadatavalue[t++] = "DirectCameraReadout_" + DCR_VERSION;
        metadatavalue[t++] = "SDK_" + SDK2_VERSION;
        Date date = new Date();
        long time = date.getTime();
        Timestamp ts = new Timestamp(time);
        metadatavalue[t++] = ts.toString();

        for (int i = 0; i < nopm; i++) {
            row = metaSheet.createRow(i);
            row.createCell(0).setCellValue(metadatatag[i]);
            row.createCell(1).setCellValue(metadatavalue[i]);
        }

        try {
            FileOutputStream fileOut = new FileOutputStream(newFile);
            wb.write(fileOut);
            fileOut.close();
        } catch (IOException e) {
            throw new RuntimeException(exception, e);
        }
    }

    private static class CppToJavaTransferInfWorkerEXTENDEDV2 extends CppTOJavaTransferWorkerV2 {

        public CppToJavaTransferInfWorkerEXTENDEDV2(short[] array, CountDownLatch latch) {
            super(array, latch);
        }

        @Override
        protected void runInfinteLoop() {
            runInfiniteLoopSDK2(array, DirectCapturePanel.Common.framecounter);
        }

    }

    private static class CppToJavaTransferAcqWorkerEXTENDEDV2 extends CppTOJavaTransferWorkerV2 {

        public CppToJavaTransferAcqWorkerEXTENDEDV2(short[] array, CountDownLatch latch) {
            super(array, latch);
        }

        @Override
        protected void runInfinteLoop() {
            runContinuousScanAcquisitionSDK2(array, DirectCapturePanel.Common.framecounter);
        }
    }
}
