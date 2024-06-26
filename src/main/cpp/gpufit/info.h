#ifndef GPUFIT_PARAMETERS_H_INCLUDED
#define GPUFIT_PARAMETERS_H_INCLUDED

#include <vector>

#include "constants.h"
#include "definitions.h"
#include "gpufit.h"

class Info
{
public:
    Info();
    virtual ~Info();

    void set_fits_per_block(std::size_t const n_fits);
    void set_number_of_parameters_to_fit(int const *parameters_to_fit);
    void configure();

private:
    void get_gpu_properties();
    void set_max_chunk_size();
    void set_blocks_per_fit();

public:
    int n_parameters_;
    int n_parameters_to_fit_;

    int n_points_;
    int power_of_two_n_points_;

    std::size_t n_fits_;

    std::size_t user_info_size_;

    int max_n_iterations_;
    int num_v_coefs_; // NEW
    std::size_t max_chunk_size_;

    int n_fits_per_block_;
    int n_blocks_per_fit_;
    ModelID model_id_;
    EstimatorID estimator_id_;

    bool use_weights_;

    int max_threads_;
    int warp_size_;

    DataLocation data_location_;

private:
    std::size_t max_blocks_;
    std::size_t available_gpu_memory_;
};

int getDeviceCount();

#endif