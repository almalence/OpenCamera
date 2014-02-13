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

 COPYRIGHT 2012, ALMALENCE, INC.

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
// Output:
//         hist     - Global histogram
//         hist_loc - Local histograms
// Return:
//         Pointer to hist.
void Dro_GetHistogramNV21(Uint8 *in, Uint32 hist[256], Uint32 hist_loc[3][3][256], int sx, int sy);


// ComputeToneTable - compute tone modification table lookup_table[256].
// Input:
//         hist - pointer to histogram with 256 elements
// Output:
//         lookup_table[256] - filled with tone-table multipliers (in q5.10 fixed-point format)
//
// Parameters:
//         crt       - Defines the method (0=old method, 1=new method), recommended value: 1
//         gamma     - Defines how 'bright' the output image will be. Lower values cause brighter output.
//                     Default: 0.5
//         min_limit - Minimum limit on contrast reduction. Range: [0..0.9]. Default: 0.5
//         max_limit - Maximum limit on contrast enhancement. Range: [1..10]. Default:
//                     4 - for hdr-like effects, 3 for more balanced results
//         global_limit - Maximum limit on total brightness amplification. Recommended: 4
// Return:
//         pointer to lookup_table.
Int32 *Dro_ComputeToneTable(Uint32 *hist, Int32 *lookup_table, int crt, float gamma, float min_limit, float max_limit, float global_limit);


// ApplyToneTableNV21 - apply lookup_table[256] to YUV.
//
// Input:
//         in - Input image in NV12 or NV21 format.
//         lookup_table[256] - Tone table returned by ComputeToneTable.
//         pull_uv - how much to enhance U and V 0..9 0=no saturation, 9(default)=enhance to the same level as Y
//         sx, sy - Image width and height.
// Output:
//         out - Processed image.
// Return:
//         0 = all Ok
//         1 = Not enough memory
int Dro_ApplyToneTableNV21(Uint8 *in, Uint8 *out, Int32 lookup_table[256], Int32 lookup_local[3][3][256], int pull_uv, int sx, int sy);

// ApplyToneTableFilteredNV21 - same as ApplyToneTableNV21 but with noise reduction
//
// Input:
//         filter - amount of filtering to apply
//         strong_filter - whether to apply soft-filter (==0, recommended), or strong-filter (==1, use only for extreme low-light)
int Dro_ApplyToneTableFilteredNV21(Uint8 *in, Uint8 *out, Int32 lookup_table[256], Int32 lookup_local[3][3][256], int filter, int strong_filter, int pull_uv, int sx, int sy);

// Description:
//    Detects if a new histogram is sufficiently different from the base one.
//    If so - new histogram is copied into base.
//    It is further detected if scene is changed.
//
// Return:
//
// 0 = no tone table update is needed
//     hist_base remain untouched
//
// 1 = tone update is needed, but no scene change (slowly advance to the new table)
//     hist_base updated with the new hist
//
// 2 = tone update is needed, scene change (switch to the new table immediately)
//     hist_base updated with the new hist
//
int Dro_CheckToneUpdateNeeded(Uint32 *hist, Uint32 *hist_base);


//
// Example:
//
// void example(Uint8* in, int sx, int sy)
// {
//     Uint32 hist[256];
//     Int32 lookup_table[256];
//
//     GetHistogramNV21(in, hist, sx, sy);
//     ComputeToneTable(hist, lookup_table, 1, 0.5, 4, 0.5);
//     ApplyToneTableNV21(in, out, lookup_table, sx, sy);
// }
//


#if defined __cplusplus
}
#endif

#endif /* __DRO_H__ */
