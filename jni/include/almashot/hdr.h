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

 COPYRIGHT 2010-2012, ALMALENCE, INC.

 ---------------------------------------------------------------------------

 Hdr Exposure public header

\* ------------------------------------------------------------------------- */

#ifndef __HDR_H__
#define __HDR_H__

#if defined __cplusplus
extern "C"
{
#endif


#include "almashot.h"

// Hdr_Preview - call to construct fast preview and initialize instance
//
// instance - an address of a pointer to hold instance-related data, to be later passed to Hdr_Process
// in - captured frames in YUV format. in's should be allocated with malloc, these are free'd inside Hdr_Process
// caller should not free in buffers!
// Preview - (sx/4) x (sy/4) preview image in interleaved RGB888 format
// SensorGain is the sensor gain during capture, in u8.8 fixed-point format
// expoPref: 0=Normal, 1=Bright
// colorPref: 0=B&W, 1=Normal, 2=Vivid, 3=High saturation
// ctrstPref: 0=low 1=normal 2=high contrast
// microPref: 0=low 1=normal 2=high micro contrast
// colorPref, ctrstPref, microPref also accept fine grained values as -0..-100
// sx, sy: dimensions of input frames (width, height)
// alignmentLevel: 0=small displacements only, 1=correct large translations, 2=no alignment
// disableDeghosting: if set to 1 - use noise-free fusion instead of de-ghosting
// preFilter: set to 1 to apply noise filtering to input frames, set to 2 to have it more aggressive
// postFilter: set to 1 to apply post-filtering and possibly sharpening to the result
// scaleOutput: if set to 1 - output image will be up-scaled to input dimensions to
//    cover non-aligned edges, no in-place processing is possible in this case
//    (Out parameter to Hdr_Process() should be pre-allocated by caller)
// fastMode: 0=regular processing, 1=fast processing (recommended for >=8Mpix)
int Hdr_Preview
(
	void  **instance,
	Uint8 **in,
	Uint8 * Preview,
	Uint8 *restrict debug1,
	Uint8 *restrict debug2,
	int   SensorGain,
	int   expoPref,
	int   colorPref,
	int   ctrstPref,
	int   microPref,
	int   sx,
	int   sy,
	int   nFrames,
	int   alignmentLevel,
	int   disableDeghosting,
	int   preFilter,
	int   postFilter,
	int   scaleOutput,
	int   fastMode
);


// Hdr_Preview2 - call to construct fused preview
//
// instance - pointer to instance set by Hdr_Preview
// Preview - SXP x SYP preview image in interleaved RGB888 format
//
// if ReFuse is not zero - re-compute preview with new
//  newExpoPref, newColorPref, newCtrstPref, newMicroPref
// ReFuse should not be set on the first invocation of Hdr_Preview2

int Hdr_Preview2
(
	void *instance,
	Uint8 * Preview,
	int   ReFuse,
	int   newExpoPref,
	int   newColorPref,
	int   newCtrstPref,
	int   newMicroPref
);


// Hdr_Process - call to construct full-size image (should be called after Hdr_Preview)
//
// instance - pointer to instance set by Hdr_Preview
// Out - SX x SY full-size composed image in YUV format
// keepInstance - set to non-zero to avoid memory freeing after processing finished
//                keepInstance = 1 - keep both frame buffers and processing instance
//                keepInstance = 2 - keep only frame buffers
//                Hdr_FreeInstance() should be called separately in this case
// Set Out to NULL before calling Hdr_Process for in-place operation
// ('Out' will point to one of the 'in' buffers passed to Hdr_Preview)
// or set it to SX x SY buffer.
// Caller should free 'Out' once it is not needed.
// If multiple invocations of Hdr_Process() are used - Out should not be NULL
// (no in-place operation is possible), keepInstance should be non-zero.
//
int Hdr_Process
(
	void *instance,
	Uint8 ** Out,
	int		*x0_out,
	int		*y0_out,
	int		*w_out,
	int		*h_out,
	int		keepInstance
);


// Hdr_Cancel - may be called by invoking application during Hdr_Process to cancel
// full-size computation
void Hdr_Cancel
(
	void *instance
);


// Hdr_FreeInstance - should be called after Hdr_Process
// if keepInstance was set to non-zero during call to Hdr_Process
// full-size computation
// set keepBuffers to non-zero to keep frame buffers and only free processing instance
void Hdr_FreeInstance
(
	void *instance,
	int keepBuffers
);


// Hdr_FreePreviewArrays - can be called after Hdr_Preview and Hdr_Preview2 has finished
// to free up some of the instance memory, which will not be used during full-size processing
void Hdr_FreePreviewArrays
(
	void *instance
);


// Hdr_SortExposures - sort frames in exposure descending order
//
void Hdr_SortExposures
(
	Uint8 **in,
	int sx,
	int sy,
	int nFrames
);


// Output: Ev[0], Ev[1], Ev[2] - recommended expo-correction for each frame in 1/10th of Ev step, in descending order of exposures
// Return: recommended number of frames to capture (2 or 3)
// Note:
//  - a maximum range of [-2..+2] Ev is covered for now
//  - maximum input dimensions are ~40Mpix, function is intended for use with preview-sized images
int Hdr_RecommendEvRange
(
	Uint8 *in,
	int sx,
	int sy,
	int *Ev
);


#if defined __cplusplus
}
#endif

#endif // __HDR_H__
