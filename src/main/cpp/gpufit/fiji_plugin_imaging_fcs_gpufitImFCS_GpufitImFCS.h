/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS */

#ifndef _Included_fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
#define _Included_fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    fit
 * Signature: (IILjava/nio/FloatBuffer;Ljava/nio/FloatBuffer;ILjava/nio/FloatBuffer;FIILjava/nio/IntBuffer;IILjava/nio/FloatBuffer;Ljava/nio/FloatBuffer;Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;Ljava/nio/IntBuffer;)I
 */
JNIEXPORT jint JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_fit
  (JNIEnv *, jclass, jint, jint, jobject, jobject, jint, jobject, jfloat, jint, jint, jobject, jint, jint, jobject, jobject, jobject, jobject, jobject);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    getLastError
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_getLastError
  (JNIEnv *, jclass);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    isCudaAvailableInt
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_isCudaAvailableInt
  (JNIEnv *, jclass);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    getCudaVersionAsArray
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_getCudaVersionAsArray
  (JNIEnv *, jclass);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    resetGPU
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_resetGPU
  (JNIEnv *, jclass);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    calcDataBleachCorrection
 * Signature: ([F[FLfiji/plugin/imaging_fcs/gpufitImFCS/GpufitImFCS/ACFParameters;)V
 */
JNIEXPORT void JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_calcDataBleachCorrection
  (JNIEnv *, jclass, jfloatArray, jfloatArray, jobject);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    isBinningMemorySufficient
 * Signature: (Lfiji/plugin/imaging_fcs/gpufitImFCS/GpufitImFCS/ACFParameters;)Z
 */
JNIEXPORT jboolean JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_isBinningMemorySufficient
  (JNIEnv *, jclass, jobject);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    calcBinning
 * Signature: ([F[FLfiji/plugin/imaging_fcs/gpufitImFCS/GpufitImFCS/ACFParameters;)V
 */
JNIEXPORT void JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_calcBinning
  (JNIEnv *, jclass, jfloatArray, jfloatArray, jobject);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    isACFmemorySufficient
 * Signature: (Lfiji/plugin/imaging_fcs/gpufitImFCS/GpufitImFCS/ACFParameters;)Z
 */
JNIEXPORT jboolean JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_isACFmemorySufficient
  (JNIEnv *, jclass, jobject);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    calcACF
 * Signature: ([F[D[D[D[D[D[D[D[ILfiji/plugin/imaging_fcs/gpufitImFCS/GpufitImFCS/ACFParameters;)V
 */
JNIEXPORT void JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_calcACF
  (JNIEnv *, jclass, jfloatArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jdoubleArray, jintArray, jobject);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS
 * Method:    resetDevice
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_fiji_plugin_imaging_1fcs_gpufitImFCS_GpufitImFCS_resetDevice
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
