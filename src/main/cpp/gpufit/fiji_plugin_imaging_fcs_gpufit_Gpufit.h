/* DO NOT EDIT THIS FILE - it is machine generated */
#include <jni.h>
/* Header for class fiji_plugin_imaging_fcs_gpufit_Gpufit */

#ifndef _Included_fiji_plugin_imaging_fcs_gpufit_Gpufit
#define _Included_fiji_plugin_imaging_fcs_gpufit_Gpufit
#ifdef __cplusplus
extern "C" {
#endif
/*
 * Class:     fiji_plugin_imaging_fcs_gpufit_Gpufit
 * Method:    fit
 * Signature: (IILjava/nio/FloatBuffer;Ljava/nio/FloatBuffer;ILjava/nio/FloatBuffer;FIILjava/nio/IntBuffer;IILjava/nio/FloatBuffer;Ljava/nio/FloatBuffer;Ljava/nio/IntBuffer;Ljava/nio/FloatBuffer;Ljava/nio/IntBuffer;)I
 */
JNIEXPORT jint JNICALL Java_fiji_plugin_imaging_1fcs_gpufit_Gpufit_fit
  (JNIEnv *, jclass, jint, jint, jobject, jobject, jint, jobject, jfloat, jint, jint, jobject, jint, jint, jobject, jobject, jobject, jobject, jobject);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufit_Gpufit
 * Method:    getLastError
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_fiji_plugin_imaging_1fcs_gpufit_Gpufit_getLastError
  (JNIEnv *, jclass);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufit_Gpufit
 * Method:    isCudaAvailable
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_fiji_plugin_imaging_1fcs_gpufit_Gpufit_isCudaAvailable
  (JNIEnv *, jclass);

/*
 * Class:     fiji_plugin_imaging_fcs_gpufit_Gpufit
 * Method:    getCudaVersionAsArray
 * Signature: ()[I
 */
JNIEXPORT jintArray JNICALL Java_fiji_plugin_imaging_1fcs_gpufit_Gpufit_getCudaVersionAsArray
  (JNIEnv *, jclass);

#ifdef __cplusplus
}
#endif
#endif
