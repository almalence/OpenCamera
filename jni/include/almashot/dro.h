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
//         hist - Pointer to pre-allocated array of 256 elements.
// Output:
//         hist - Computed histogram.
// Return:
//         Pointer to hist.
Uint32 *Dro_GetHistogramNV21(Uint8 *in, Uint32 *hist, int sx, int sy);


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
// Return:
//         pointer to lookup_table.
Int32 *Dro_ComputeToneTable(Uint32 *hist, Int32 *lookup_table, int crt, float gamma, float max_limit, float min_limit);


// ApplyToneTableNV21 - apply lookup_table[256] to YUV.
//
// Input:
//         in - Input image in NV12 or NV21 format.
//         lookup_table[256] - Tone table returned by ComputeToneTable.
//         sx, sy - Image width and height.
// Output:
//         out - Processed image.
// Return:
//         0 = all Ok
//         1 = Not enough memory
int Dro_ApplyToneTableNV21(Uint8 *in, Uint8 *out, Int32 *lookup_table, int sx, int sy);


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
