#ifndef CUDA_FCS_KERNELS_H
#define CUDA_FCS_KERNELS_H

#include <cuda_runtime.h>

extern __global__ void calc_binning(float *data, float *data1, int win_star, int hin_star, int w_temp, int h_temp,
                                    int framediff, int pixbinX, int pixbinY, int binningX, int binningY);

extern __global__ void calc_data_bleach_correction(float *data, float *data1, int width, int height, int nopit,
                                                   int ave);

extern __global__ void bleachcorrection(float *data, int w_temp, int h_temp, int d, int bleachcorr_order,
                                        double frametimegpu, double *bleachcorr_params);

extern __global__ void calcacf2a(float *data, int w_temp, int h_temp, int numbin);

extern __global__ void calcacf2b_NB(const float *data, int cfXDistancegpu, int cfYDistancegpu, int w, int h, int w_temp,
                                    int h_temp, int pixbinX, int pixbinY, float *prod, const int *laggpu,
                                    int *prodnumarray, double *NBmeanGPU, double *NBcovarianceGPU, int x, int numbin,
                                    int currentIncrement);

extern __global__ void calcacf2b_ACF(const float *data, int cfXDistancegpu, int cfYDistancegpu, int w, int h,
                                     int w_temp, int h_temp, int pixbinX, int pixbinY, double *data1, float *prod,
                                     const int *laggpu, int *prodnumarray, const int *indexarray, double *blockvararray,
                                     double *sdarray, const int *pnumgpu, int x, int numbin, int currentIncrement,
                                     int ctbin);

extern __global__ void calcacf3(float *data, int cfXDistancegpu, int cfYDistancegpu, int blocklag, int w, int h,
                                int w_temp, int h_temp, int pixbinX, int pixbinY, int d, int correlatorp,
                                int correlatorq, int chanum, double frametimegpu, double *data1, float *prod,
                                double *prodnum, double *blocksd, double *upper, double *lower, int *crt, int *cr12,
                                int *cr3, int *diffpos, double *varblock0, double *varblock1, double *varblock2,
                                double *sampgpu, int *laggpu);

#endif // CUDA_FCS_KERNELS_H