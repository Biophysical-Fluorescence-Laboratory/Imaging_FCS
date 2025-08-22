cmake_minimum_required(VERSION 3.18 FATAL_ERROR)
project(agpufit LANGUAGES CXX CUDA)

set(CMAKE_CUDA_STANDARD 11)
set(CMAKE_CUDA_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(CUDAToolkit REQUIRED)
include_directories(${CUDAToolkit_INCLUDE_DIRS})

include_directories(cuda_utils)
include_directories(estimators)
include_directories(models)

include_directories($ENV{JAVA_HOME}/include)
# Additionally, for platform-specific headers (e.g., jni_md.h)
if(APPLE)
    include_directories($ENV{JAVA_HOME}/include/darwin)
elseif(WIN32)
    include_directories($ENV{JAVA_HOME}/include/win32)
else()
    include_directories($ENV{JAVA_HOME}/include/linux)
endif()

# Define the library this folder will build
add_library(agpufit SHARED
    estimators/estimators.cuh
    estimators/lse.cuh
    estimators/mle.cuh
    models/acf_1d.cuh
    models/acf_numerical3d.cuh
    models/gauss_2d.cuh
    models/linear_1d.cuh
    models/models.cuh
    acf_input_params_wrapper.cpp
    acf_input_params_wrapper.h
    constants.h
    cuda_gaussjordan.cu
    cuda_gaussjordan.cuh
    cuda_kernels.cu
    cuda_kernels.cuh
    cuda_fcs_kernels.cu
    cuda_fcs_kernels.cuh
    definitions.h
    gpu_data.cu
    gpu_data.cuh
    gpufit.cpp
    gpufit.h
    fiji_plugin_imaging_fcs_imfcs_gpu_GpuCorrelator.cu
    fiji_plugin_imaging_fcs_imfcs_gpu_GpuCorrelator.h
    fiji_plugin_imaging_fcs_imfcs_gpu_GpuParameters.cu
    fiji_plugin_imaging_fcs_imfcs_gpu_GpuParameters.h
    fiji_plugin_imaging_fcs_gpufit_Gpufit.cu
    fiji_plugin_imaging_fcs_gpufit_Gpufit.h
    info.cpp
    info.cu
    info.h
    interface.cpp
    interface.h
    lm_fit_cuda.cpp
    lm_fit_cuda.cu
    lm_fit.cpp
    lm_fit.h
    )

target_link_libraries(agpufit PRIVATE CUDA::cudart_static)

# Issue here with 52 being unsupported on more recent versions of CUDA toolkit.
set_target_properties(agpufit PROPERTIES CUDA_ARCHITECTURES "61;75;86")