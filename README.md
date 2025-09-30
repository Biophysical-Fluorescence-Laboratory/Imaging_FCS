[![Build](https://github.com/Biophysical-Fluorescence-Laboratory/Imaging_FCS/actions/workflows/build_and_release.yml/badge.svg)](https://github.com/Biophysical-Fluorescence-Laboratory/Imaging_FCS/actions/workflows/build_and_release.yml)
[![Version](https://img.shields.io/github/v/release/Biophysical-Fluorescence-Laboratory/Imaging_FCS?sort=semver)](https://github.com/Biophysical-Fluorescence-Laboratory/Imaging_FCS/releases)

# Imaging FCS :microscope:

<p align="center">
    <img src="src/main/resources/images/Imaging_FCS_screenshot.png" style"zoom: 50%;" />
</p>

*Imaging FCS* is an *ImageJ* plugin featuring post-processing tools to calculate
and view spatio-temporal correlation functions from 16 bit grey tiff stack
files as well as data acquisition software for real-time image analysis. It was
written as a *FIJI plugin* (`ImageJ 1.53f`; `Java 1.8.0_281`).

*Imaging FCS* provides a comprehensive software tool to calculate and evaluate
spatiotemporal correlation functions. It includes the calculation of all auto-
or cross-correlation functions for arbitrary pixel binning and regions of
interest within an image, provides fit functions for total internal reflection
fluorescence (TIRF) and single plane illumination microscopy (SPIM) based FCS
measurements, can calculate the FCS diffusion laws and contains an essential
simulator to create simulated data for different diffusive modes.

ImagingFCS runs under ImageJ, FIJI and Micromanager, and it runs on PC, Linux,
and Mac OS. We will always use FIJI in the following text, but it should be
understood that the same is true for ImageJ and Micromanager.

## Installation

### Installation from the *ImageJ update site*

The easiest way to install *Imaging FCS* is by using the *ImageJ* update site. In
*ImageJ* chose `Help->Update`. This opens the `ImageJ Updater` window. Click on
`Manage update sites`.

Please tick both `Image Science` and `ImagingFCS`. Then close the
`Manage update site` window and click `Apply Changes` in the `ImageJ Updater`
window.

Any bugs found through this method should be reported under Github Issues.

### Manual installation

#### Download on [Github Releases](https://github.com/Biophysical-Fluorescence-Laboratory/Imaging_FCS/releases/latest)

You can directly download the jar file from the GitHub releases page and move
the file in `fiji_root/plugins/` (here `fiji_root` is the location of your fiji
root). It will work on Windows, Linux and MacOS without additional features
(GPU and camera readout).

To enable these additional features, you will need to compile the plugin manually
to ensure proper linking.

#### Manual compilation
##### Plugin-only (No GPU fitting or Camera SDKs)
To compile the `.jar` file, you need to have *Maven* and *JDK 8* installed and
then you can simply run from the root of the project:

```sh
mvn clean package
```

You can then find the `.jar` file in the `target` folder and move it to
`fiji_root/plugins`.

By default, this compilation method does not bundle in GPU fitting or 
live readout functionality. To include this functionality, see below.

##### Including Pre-built Libraries
This method allows you to skip the C++ and CUDA compilation steps, but
it does not guarantee a plugin build that supports all features right
out-of-the-box.

Depending on your operating system, you can download the corresponding
compiled libraries from Github releases, i.e. either `linux-libs.zip`
or `windows-libs.zip`.

After extracting these files, include them in the corresponding
`resources` folder. During the Java compilation step, any files
and folders in the `resources` folder will be bundled into the JAR 
file, and be accessible at runtime.

The expected filepaths are as follows:

```
# GPU fitting libraries
src/main/resources/libs/gpufit/agpufit.dll         # Windows DLL binary
src/main/resources/libs/gpufit/libagpufit.so       # Linux .so binary

# Camera SDK libraries for Live Readout
src/main/resources/libs/camera_readout/dcam/JNIHamamatsuDCAMsdk4.dll
src/main/resources/libs/camera_readout/pvcam/JNIPhotometricsPVCAMsdk.dll
src/main/resources/libs/camera_readout/sdk2/JNIAndorSDK2v3.dll
src/main/resources/libs/camera_readout/sdk3/JNIAndorSDK3v2.dll
```

Note that these exact filepaths will still be expected even when doing
manual compilation as described below.

Also, live camera readout functionality has dependencies on additional 
DLL files that must be included in the resources folder as well. In
other words, simply including these DLL files **is not sufficient to**
**enable live readout functionality**.

See below for more details.

##### Compiling on Linux
On Linux, the plugin can be compiled with GPU support.

This requires [Cmake](https://cmake.org/cmake/help/latest/index.html) and
[CUDA Toolkit](https://developer.nvidia.com/cuda-12-6-0-download-archive?target_os=Linux&target_arch=x86_64) to be installed.

We can then execute the C compilation steps using the following:
```
cmake -B ./src/main/cpp/build -DCMAKE_CXX_COMPILER=g++ -DCMAKE_C_COMPILER=gcc -DCMAKE_BUILD_TYPE=Release -S ./src/main/cpp
cmake --build ./src/main/cpp/build --config Release
```

If the compilation succeeds, you can find the compiled `libagpufit.so` binary
in the `./src/main/cpp/build/Release` folder. You can move the files to the 
expected location in the `resources` folder to bundle it into the JAR compilation
step.

Note the specific version 12.6 for CUDA toolkit specified here. Any version
post-12.6 removes support for pre-Pascal GPUs, which is something we 
prefer to avoid at this stage.

##### Compiling on Windows
To compile the libraries on Windows, you **must install Visual Studio** and the
**C++ build tools**. You can find them using the links below:
- [Visual Studio](https://visualstudio.microsoft.com/)
- [Visual Studio C++ Build Tools](https://visualstudio.microsoft.com/visual-cpp-build-tools/)

Once both are installed, search your Start Menu for **Visual Studio Build Tools**.
Then, install the **Desktop development with C++** option.

You can then run the build chain using the following commands. It is recommended
to run these from within the "Powershell for Visual Studio" prompt window, which
you can find in your Start Menu under the Visual Studio folder.
```
cmake -S src/main/cpp -B src/main/cpp/build -G "Visual Studio 17 2022" -A x64 -DCMAKE_BUILD_TYPE=Release
cmake --build $buildDir --config Release
```

If the compilation succeeds, you can find the compiled `libagpufit.so` binary
in the `./src/main/cpp/build/Release` folder. You can move the files to the 
expected location in the `resources` folder to bundle it into the JAR compilation
step.

##### Packaging the Plugin for Fiji Repositories
**Windows and Linux Libraries must both be included for public publishing**.

**CUDA Support for BOTH WINDOWS AND LINUX must be included.**

When packaging the plugin for Fiji repositories, i.e. publishing the plugin for
public access, **manual compilation is strongly recommended**. In other words,
**DO NOT RELY ON THE GITHUB RELEASE BUILDS**!!

This is due to hardware support. We aim to retain backwards compatibility where
possible, and using more recent C++ and CUDA build tools might result in older
systems being locked out.

For example:
- CUDA Toolkit versions above 12.6 remove support for Nvidia GPUs pre-Pascal.
- More recent `gcc` or `g++` versions may remove support for old `glibc` versions.

As such, manual compilation should be used.

Furthermore, **REMEMBER THAT ADDITIONAL DLL FILES ARE REQUIRED FOR THE CAMERA**
**SDKS TO WORK FULLY**. The compiled binaries have separate dependencies. These
files should be included in the `resources` folder using the following paths:

To confirm the paths required, you can check the code of each camera
manufacturer's Java code. For example:
```
src/main/java/fiji/plugin/imaging_fcs/directCameraReadout/andorsdk2v3
src/main/java/fiji/plugin/imaging_fcs/directCameraReadout/andorsdk3v2/AndorSDK3v2.java
src/main/java/fiji/plugin/imaging_fcs/directCameraReadout/hamadcamsdk4/Hamamatsu_DCAM_SDK4.java
src/main/java/fiji/plugin/imaging_fcs/directCameraReadout/pvcamsdk/Photometrics_PVCAM_SDK.java
```

In the current codebase, these binaries **must be included in the JAR file**,
and cannot be discovered at runtime or via the System PATH variable.

## Updating the codebase

### C++ / CUDA

If you want to add files, you will need to update the `CMakeLists.txt` in the
current folder. Moreover, if you want to add a JNI function, you will need to
update the Java code as well and generate a new header file for your code.

To generate the header files, run:

```sh
mvn clean package -DgenerateJniHeaders=true
```

The output files will directly be at the right location in the project.

### Java

You can create or update files and the compilation should not change and work
the same way as before.

## What's new?

This version includes ImFCSNet and FCSNet inference. ImFCSNet predicts
diffusion coefficient directly from intensity traces. FCSNet predicts diffusion
coefficient from autocorrelation function.

v2.0.4 fixes some compatibility issues with the latest Java 21 versions of Fiji.
GPU fitting in end-of-life Linux distributions like CentOS 7 and Ubuntu 18.04 is
no longer supported.

## ImFCS documentation 1_62.pdf

This manual contains the basic instructions on using the program, the
definition of all items in the control and fit panels, the file formats of the
saved data, and the theoretical functions used for fitting.

# References

**(Deep learning)** Tang WH, et al. "Deep learning reduces data requirements
and allows real-time measurements in Imaging Fluorescence Correlation
Spectroscopy."
bioRxiv. 2023. https://doi.org/10.1101/2023.08.07.552352

**(Direct camera readout)** Aik DYK, et al. "Microscope alignment using
real-time Imaging FCS."
Biophys J. 2022. https://doi.org/10.1016/j.bpj.2022.06.009

**(GPU capabilities)** Sankaran J, et al. "Simultaneous spatiotemporal
super-resolution and multi-parametric fluorescence microscopy."
Nat Commun. 2021. https://doi.org/10.1038/s41467-021-22002-9

**(Correlator scheme)** Sankaran K, et al. "ImFCS: a software for imaging FCS
data analysis and visualization."
Opt Express. 2010. https://doi.org/10.1364/OE.18.025468

## Disclaimer

The software and data on this site are provided for personal or academic use
only and may not be used in any commercial venture or distributions. All files
have been virus scanned, however, for your own protection; you should scan
these files again. You assume the entire risk related to your use of this
software and data. By using the software and data on this site your expressly
assume all risks of data loss or damage alleged to have been caused by the
software and data. The Biophysical Fluorescence Laboratory at NUS is providing
this data "as is," and disclaims any and all warranties, whether express or
implied, including (without limitation) any implied warranties of
merchantability or fitness for a particular purpose. In no event will the
Biophysical Fluorescence Laboratory at NUS and/or NUS be liable to you or to
any third party for any direct, indirect, incidental, consequential, special or
exemplary damages or lost profit resulting from any use or misuse of this
software and data.
