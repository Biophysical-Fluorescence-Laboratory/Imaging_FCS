/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class fiji_plugin_imaging_fcs_imfcs_gpu_GpuParameters */

#ifndef _Included_fiji_plugin_imaging_fcs_imfcs_gpu_GpuParameters
#define _Included_fiji_plugin_imaging_fcs_imfcs_gpu_GpuParameters
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     fiji_plugin_imaging_fcs_imfcs_gpu_GpuParameters
 * Method:    isBinningMemorySufficient
 * Signature: (Lfiji/plugin/imaging_fcs/imfcs/gpu/GpuParameters;)Z
 */
JNIEXPORT jboolean JNICALL Java_fiji_plugin_imaging_1fcs_imfcs_gpu_GpuParameters_isBinningMemorySufficient
  (JNIEnv *, jclass, jobject);

/*
 * Class:     fiji_plugin_imaging_fcs_imfcs_gpu_GpuParameters
 * Method:    calcBinning
 * Signature: ([F[FLfiji/plugin/imaging_fcs/imfcs/gpu/GpuParameters;)V
 */
JNIEXPORT void JNICALL Java_fiji_plugin_imaging_1fcs_imfcs_gpu_GpuParameters_calcBinning
  (JNIEnv *, jclass, jfloatArray, jfloatArray, jobject);

/*
 * Class:     fiji_plugin_imaging_fcs_imfcs_gpu_GpuParameters
 * Method:    calcDataBleachCorrection
 * Signature: ([F[FLfiji/plugin/imaging_fcs/imfcs/gpu/GpuParameters;)V
 */
JNIEXPORT void JNICALL Java_fiji_plugin_imaging_1fcs_imfcs_gpu_GpuParameters_calcDataBleachCorrection
  (JNIEnv *, jclass, jfloatArray, jfloatArray, jobject);

#ifdef __cplusplus
}
#endif
#endif
