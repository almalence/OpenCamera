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

 COPYRIGHT 2010-2014, ALMALENCE, INC.

 ---------------------------------------------------------------------------

 Single Frame Filtering public header

\* ------------------------------------------------------------------------- */

#ifndef __FILTERS_H__
#define __FILTERS_H__

#if defined __cplusplus
extern "C"
{
#endif


#include "almashot.h"


int Filters_Initialize
(
	void **pInstance,
	int max_width,
	int max_height
);


int Filters_Release
(
	void *instance
);


// Filters_FindNoiseLevel - estimate noise level (per color channel) from the image contents
//
// Input:
// in - image to be analyzed
// reserved - used for debugging, set to NULL
// sx, sy - image dimensions
// Output:
// nl - estimated noise levels
//
void Filters_FindNoiseLevel
(
	void *instance,
	Uint8 * in,
	Uint8 * reserved,
	int sx,
	int sy,
	int nl[3]
);


// Filters_FilterFrame
// calls Filters_PostFilterQuick and Filters_PostFilterUV with right parameters
// to filter Y and UV planes of YUV frame
void Filters_FilterFrame
(
	void *instance,
	Uint8 *in,
	Uint8 *inUV,
	Uint8 *out,
	Uint8 *outUV,
	int sx,
	int sy,
	int Filter,
	int HighQuality,
	int Sharpen
);


// Limitations:
// x0, y0, w, h should be even
void Filters_ScaleFrame
(
	void *instance,
	Uint8 *in,
	Uint8 *inUV,
	Uint8 *out,
	Uint8 *outUV,
	int x0,
	int y0,
	int w,
	int h,
	int sxi,
	int syi,
	int sxo,
	int syo
);


// Filters_EnhanceEdges - edge-enhancer, an alternative to sharpening
//
// Input:
// Y - image plane to be filtered/sharpened
// x_st, y_st, x_en, y_en - coordinates of crop area where to apply edge enhancer
// sx, sy - image dimensions
// mode - amount of edge enhancing: =0 - less, =1 - more
// stride - distance (in bytes) between pixel values, set to
//          1 - for Y channel in NV21
//          2 - for U/V channels in NV21
//          2 - for Y channel in YUYV
//          4 - for U/V channels in YUYV
//          3 - for interleaved RGB
//          4 - for RGBA
//
// Output:
// Y - image plane with enhanced edges
//
void Filters_EnhanceEdges
(
	void *instance,
	Uint8 *Y,
	int x_st,
	int x_en,
	int y_st,
	int y_en,
	int sx,
    int sy,
	int mode,
    int stride
);


// ----------------------------------------------------------------------
// Note: Unstable API below

#define QUARTER_PADDED(x)  ((x)/4+4+(((x)/4)&1))


// Filters_PostFilter - Noise filter and Sharpen
//
// Input:
// Y - single image plane to be filtered/sharpened
// Scale - amount of noise filtering, in u15.16 arithmetic. Can either be:
//          - proportional to ISO sensor gain (if known), or
//          - proportional to estimated noise
//   Proportionality factor depends on sensor, typical formula is 100*256*max(nl)
// sx, sy - image dimensions
// stride - distance (in bytes) between pixel values, set to
//          1 - for Y channel in NV21
//          2 - for U/V channels in NV21
//          2 - for Y channel in YUYV
//          4 - for U/V channels in YUYV
//          3 - for interleaved RGB
//          4 - for RGBA
// sharpen - amount of sharpening to apply (0 = no sharpening)
//
// Output:
// Y - filtered image plane
//

void Filters_PostFilter
(
	void *instance,
	Uint8 *Y,
	Int32 Scale,
	int sx,
	int sy,
	int stride,
	int sharpen
);

void Filters_OuterMirrorFill
(
	Uint8 *in,
	int sx,
	int sy,
	int x0,
	int y0,
	int w,
	int h
);

void Filters_FillFilterPressure
(
	Int32 Scale,
	int sx,
	int sy,
	Uint8 *nMov,
	Uint8 *mcurTbl,
	int subsampMov,
	int sxMov
);

void Filters_PostFilterQuick
(
	void *instance,
	Uint8 *Y_in,
	Uint8 *Y_out,
	Int32 Scale,
	int sx,
	int sy,
	int sharpen,
    Uint8 *nMov,
    Uint8 *mcurTbl,
    int subsampMov,
    int sxMov
);

void Filters_PostFilterQuick_CPU
(
	void *instance,
	Uint8 *Y,
	int sx,
	int sy,
	int stride,
	int sharpen,
    Uint8 *mcurTbl,
    int subsampMov,
    int sxMov
);

void Filters_PostFilterUV
(
	void *instance,
	Uint8 *UV_in,
	Uint8 *UV_out,
	Int32 Scale,
	int sx,
	int sy
);


int Filters_DownscaleLowSpatial
(
	void *instance,
	Uint8 *in,
	Uint8 *inUV,
	int sx,
	int sy,
	Uint8 **quarterIn,
	Uint8 **quarterOut
);

int Filters_DownscaleLowSpatial16bit
(
	void *instance,
	Uint8 *in,
	Uint8 *inUV,
	int sx,
	int sy,
	Int16 **quarterIn,
	Int16 **quarterOut
);

int Filters_GetFilteredLowSpatial
(
	void *instance,
	Uint8 *qIn,
	Uint8 *qOut,
	int sxs,
	int sys,
	int Filter
);

int Filters_GetFilteredLowSpatial16bit
(
	void *instance,
	Int16 *qIn,
	Int16 *qOut,
	int sxs,
	int sys,
	int Filter
);

void Filters_ResidualQuarterCompute
(
	Uint8 *in,
	Uint8 *quarterIn,
	Uint8 *quarterOut,
	int sx,
	int sy,
	int sxs,
	int sys,
	int pressure
);

void Filters_ResidualQuarterCompute16bit
(
	Uint8 *in,
	Int16 *quarterIn,
	Int16 *quarterOut,
	int sx,
	int sy,
	int sxs,
	int sys,
	int pressure
);

void Filters_ResidualQuarterComputeUV
(
	Uint8 *in,
	Uint8 *quarterIn,
	Uint8 *quarterOut,
	int sx,
	int sy,
	int sxs,
	int sys,
	int stride
);

void Filters_ResidualQuarterComputeUV16bit
(
	Uint8 *in,
	Int16 *quarterIn,
	Int16 *quarterOut,
	int sx,
	int sy,
	int sxs,
	int sys,
	int stride
);

// Filter lower spatial components of the frame
// (useful for small-pixel sensors)
void Filters_FilterLowSpatial
(
	void *instance,
	Uint8 *in,
	Uint8 *inUV,
	int sx,
	int sy,
	int Filter
);

void Filters_FilterLowSpatialUV
(
	void *instance,
	Uint8 *inUV,
	int sx,
	int sy,
	int Filter,
	int stride
);

void Filters_FilterMoving
(
	void *instance,
	Uint8 *Y,
	Uint8 *nMov,
	Int32 Scale,
//	int mtx,
//	int mty,
	int x_st,
	int y_st,
	int x_en,
	int y_en,
	int sx,
    int sy,
    int stride
);


#if defined __cplusplus
}
#endif

#endif // __FILTERS_H__
