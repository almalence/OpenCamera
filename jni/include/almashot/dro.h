/* ------------------------------------------------------------------------- *\

 Almalence, Inc.
 3803 Mt. Bonnell Rd
 Austin, 78731
 Texas, USA

 CONFIDENTIAL: CONTAINS CONFIDENTIAL PROPRIETARY INFORMATION OWNED BY
 ALMALENCE, INC., INCLUDING BUT NOT LIMITED TO TRADE SECRETS,
 KNOW-HOW, TECHNICAL AND BUSINESS INFORMATION. USE, DISCLOSURE OR
 DISTRIBUTION OF THE SOFTWARE IN ANY FORM IS LIMITED TO SPECIFICALLY
 AUTHORIZED LICENSEES OF ALMALENCE, INC. ANY UNAUTHORIZED DISCLOSURE
 IS A VIOLATION OF STATE, FEDERAL, AND INTERNATIONAL LAWS.
 BOTH CIVIL AND CRIMINAL PENALTIES APPLY.

 DO NOT DUPLICATE. UNAUTHORIZED DUPLICATION IS A VIOLATION OF STATE,
 FEDERAL AND INTERNATIONAL LAWS.

 USE OF THE SOFTWARE IS AT YOUR SOLE RISK. THE SOFTWARE IS PROVIDED ON AN
 "AS IS" BASIS AND WITHOUT WARRANTY OF ANY KIND. TO THE MAXIMUM EXTENT
 PERMITTED BY LAW, ALMALENCE EXPRESSLY DISCLAIM ALL WARRANTIES AND
 CONDITIONS OF ANY KIND, WHETHER EXPRESS OR IMPLIED, INCLUDING, BUT NOT
 LIMITED TO THE IMPLIED WARRANTIES AND CONDITIONS OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT.

 ALMALENCE DOES NOT WARRANT THAT THE SOFTWARE WILL MEET YOUR REQUIREMENTS,
 OR THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE,
 OR THAT DEFECTS IN THE SOFTWARE WILL BE CORRECTED. UNDER NO CIRCUMSTANCES,
 INCLUDING NEGLIGENCE, SHALL ALMALENCE, OR ITS DIRECTORS, OFFICERS,
 EMPLOYEES OR AGENTS, BE LIABLE TO YOU FOR ANY INCIDENTAL, INDIRECT,
 SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE, MISUSE OR
 INABILITY TO USE THE SOFTWARE OR RELATED DOCUMENTATION.

 COPYRIGHT 2012-2014, ALMALENCE, INC.

 ---------------------------------------------------------------------------

 Dynamic range optimizer interface

\* ------------------------------------------------------------------------- */


#ifndef __DRO_H__
#define __DRO_H__

#if defined __cplusplus
extern "C"
{
#endif


#include "almashot.h"


// GetHistogramNV21 - computes histogram
// Input:
//         in       - frame in NV21 format
//         sx,sy    - frame dimensions
//         hist     - array to hold global histogram
//         hist_loc - arrays to hold local histograms, pass NULL to not compute local histograms
//         mix_factor - used in local histograms computation.
//                    Defines the level of inter-dependence of image areas. Range: [0..1]. Default: 1.0
// Output:
//         hist     - Global histogram
//         hist_loc - Local histograms
void Dro_GetHistogramNV21
(
	Uint8 *in,
	Uint32 hist[256],
	Uint32 hist_loc[3][3][256],
	int sx,
	int sy,
	float mix_factor
);


// MixLocalTables - mix local tables (either lookup or histograms) proportionally to mix_factor
void MixLocalTables
(
	Uint32 in[3][3][256],
	Uint32 out[3][3][256],
	float mix_factor,
	int normalize
);

// ComputeToneTable - compute tone modification table lookup_table[256].
// Input:
//         hist - pointer to histogram with 256 elements
// Output:
//         lookup_table - filled with tone-table multipliers (in q5.10 fixed-point format)
//
// Parameters:
//         gamma        - Defines how 'bright' the output image will be. Lower values cause brighter output.
//                        Default: 0.5
//         max_black_level - threshold for black level correction. Default: 16
//         black_level_atten - how much to attenuate black level. Default: 0.5
//         min_limit[3] - Minimum limit on contrast reduction. Range: [0..0.9]. Default: {0.5, 0.5, 0.5}
//         max_limit[3] - Maximum limit on contrast enhancement. Range: [1..10]. Default:
//                        [0] - for shadows, [1] - for midtones, [2] - for highlights.
//                        {4.0, 2.0, 2.0} - for hdr-like effects;
//                        {3.0, 2.0, 2.0} - for more balanced results.
//         global_limit - Maximum limit on total brightness amplification. Recommended: 4
void Dro_ComputeToneTable
(
	Uint32 hist[256],
	Int32 lookup_table[256],
	float gamma,
	float max_black_level,
	float black_level_atten,
	float min_limit[3],
	float max_limit[3],
	float global_limit
);


// ComputeToneTableLocal - same as ComputeToneTable, but compute 3x3 tone tables.
// Input:
//         hist_loc - 3x3 histograms each with 256 elements
// Output:
//         lookup_table_loc - filled with multipliers for each of 3x3 image areas
//
// Parameters:
//         gamma        - Defines how 'bright' the output image will be. Lower values cause brighter output.
//                        Default: 0.5
//         max_black_level - threshold for black level correction. Default: 16
//         black_level_atten - how much to attenuate black level. Default: 0.5
//         min_limit[3] - Minimum limit on contrast reduction. Range: [0..0.9]. Default: {0.5, 0.5, 0.5}
//         max_limit[3] - Maximum limit on contrast enhancement. Range: [1..10]. Default:
//                        [0] - for shadows, [1] - for midtones, [2] - for highlights.
//                        {4.0, 2.0, 2.0} - for hdr-like effects;
//                        {3.0, 2.0, 2.0} - for more balanced results.
//         global_limit - Maximum limit on total brightness amplification. Recommended: 4
//         mix_factor   - Defines the level of inter-dependence of image areas. Range: [0..1].
//                        Recommended: 0.2
void Dro_ComputeToneTableLocal
(
	Uint32 hist_loc[3][3][256],
	Int32 lookup_table_loc[3][3][256],
	float gamma,
	float max_black_level,
	float black_level_atten,
	float min_limit[3],
	float max_limit[3],
	float global_limit,
	float mix_factor
);


// ApplyToneTableNV21 - apply lookup_table[256] to YUV.
//
// Input:
//         in - Input image in NV12 or NV21 format.
//         lookup_table[256] - Tone table returned by ComputeToneTable.
//         uv_desat - how much to reduce U and V 0..9 0=no saturation, 9(default)=enhance to the same level as Y
//         dark_uv_desat - de-saturate U and V if log2(Y) is below this level.
//                  Valid range is: [0..7], default=5
//         sx, sy - Image width and height.
// Output:
//         out - Processed image.
// Return:
//         0 = all Ok
//         1 = Not enough memory
int Dro_ApplyToneTableNV21
(
	Uint8 *in,
	Uint8 *out,
	Int32 lookup_table[256],
	Int32 lookup_local[3][3][256],
	int uv_desat,
	int dark_uv_desat,
	int sx,
	int sy
);

// ApplyToneTableFilteredNV21 - same as ApplyToneTableNV21 but with noise reduction
//
// Input:
//         in - Input image in NV12 or NV21 format.
//         lookup_table[256] - Tone table returned by ComputeToneTable.
//         filter - amount of filtering to apply
//         strong_filter - whether to apply soft-filter (==0, recommended), or strong-filter (==1, use only for extreme low-light)
//         uv_desat - how much to reduce U and V 0..9 0=no saturation, 9(default)=enhance to the same level as Y
//         dark_uv_desat - de-saturate U and V if log2(Y) is below this level.
//                  Valid range is: [0..7], default=5
//         sx, sy - Image width and height.
// Output:
//         out - Processed image.
// Return:
//         0 = all Ok
//         1 = Not enough memory
int Dro_ApplyToneTableFilteredNV21
(
	Uint8 *in,
	Uint8 *out,
	Int32 lookup_table[256],
	Int32 lookup_local[3][3][256],
	int filter,
	int uv_desat,
	int dark_uv_desat,
	float dark_noise_pass,
	int sx,
	int sy
);


//
// Example:
//
// void example(Uint8* in, int sx, int sy)
// {
//     Uint32 hist[256];
//     Int32 lookup_table[256];
//
//     GetHistogramNV21(in, hist, NULL, sx, sy);
//     ComputeToneTable(hist, lookup_table, 1, 0.5, 0.5, 3, 4);
//     ApplyToneTableFilteredNV21(in, out, lookup_table, NULL, 0, 0, 9, sx, sy);
// }
//


// --------------------------------------------------------------------------
// Video-stream processing functions


// Return: Error code:
//         ALMA_GL_CONTEXT_ERROR - Open GL Error
//         ALMA_NOT_ENOUGH_MEMORY - note enough memory
//         ALMA_ALL_OK - initialization completed successfully
int Dro_StreamingInitialize
(
	void **instance,
	int output_width,
	int output_height
);


// Return: Error code (currently just ALMA_ALL_OK)
int Dro_StreamingRelease
(
	void *instance
);


void Dro_StreamingRender
(
	void *instance,
	unsigned int texture_in,
	float *mtx,
	int sx,
	int sy,
	int filter,
	float max_amplify,
	int local_mapping,
	int force_update,
	int uv_desat,
	int dark_uv_desat,
	float dark_noise_pass,
	float mix_factor,
	float gamma,				// default = 0.5
	float max_black_level,		// default = 16
	float black_level_atten,	// default = 0.5
	float min_limit[3],			// default = {0.5, 0.5, 0.5}
	float max_limit[3],			// default = {3.0, 2.0, 2.0}
	unsigned int texture_out
);


#if defined __cplusplus
}
#endif

#endif /* __DRO_H__ */
