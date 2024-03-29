cmake_minimum_required(VERSION 3.8 FATAL_ERROR)
project(agpufit LANGUAGES CXX CUDA)

set(CMAKE_CUDA_STANDARD 14)
set(CMAKE_CUDA_STANDARD_REQUIRED ON)
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

find_package(CUDA REQUIRED)
include_directories(${CUDA_INCLUDE_DIRS})

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
    constants.h
    cuda_gaussjordan.cu
    cuda_gaussjordan.cuh
    cuda_kernels.cu
    cuda_kernels.cuh
    definitions.h
    gpu_data.cu
    gpu_data.cuh
    gpufit.cpp
    gpufit.h
    fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS.cu
    fiji_plugin_imaging_fcs_gpufitImFCS_GpufitImFCS.h
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

target_link_libraries(agpufit ${CUDA_LIBRARIES})