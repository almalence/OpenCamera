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

 Panorama public header

\* ------------------------------------------------------------------------- */

#ifndef __PANORAMA_H__
#define __PANORAMA_H__

#if defined __cplusplus
extern "C"
{
#endif


#include "almashot.h"

#define DEFAULT_FOV		49 // default FOV in panorama direction, in degrees


// Pano_PrepareFrames - select relevant frames, compute final mosaic dimensions, pre-warp relevant frames
//
// Input:
//   in - captured frames in YUV format. in's are not free'd inside Panorame processing functions
//   sx, sy - dimensions of input captured frames (width, height)
//   nFrames - total number of captured frames
//   trs - alignment matrixes for captured frames
// Output:
//   out - selected (relevant) frames for further processing with Pano_Preview, pre-warped.
//   fx0, fy0 - arrays of top-left corner coordinates of warped frames
//   fsx, fsy - arrays of dimensions of warped frames (individual dimensions per each frame)
//   nFramesSelected - total number of selected frames
//   psx, psy - dimensions of final panorama (width, height)
//   trs - alignment matrixes for captured frames corrected for angle
int Pano_PrepareFrames
(
	Uint8 **in,
	int   sx,
	int   sy,
	int   nFrames,
	Uint8 **framesSelected,
	float (*trs)[3][3],
	Uint8 **out,
   	int   *fx0,
   	int   *fy0,
   	int   *fsx,
   	int   *fsy,
   	int   *nFramesSelected,
   	int   *psx,
   	int   *psy,
	int   *x0_out,
	int   *y0_out,
	int   *w_out,
	int   *h_out,
	int   cameraFOV,
	int   translationCorrection,
	int   trapezoidCorrection,
	int   useAll,
	int   freeInput
);


// Pano_Preview - call to construct fast preview and initialize instance
//
// instance - an address of a pointer to hold instance-related data, to be later passed to Hdr_Process
// in - captured frames in YUV format. in's should be allocated with malloc, these are free'd inside Pano_Process
// caller should not free in buffers!
// Preview - (sx/4) x (sy/4) preview image in interleaved RGB888 format
// SensorGain is the sensor gain during capture, in u8.8 fixed-point format
// ctrstPref: 0=low 1=normal 2=high contrast
// ctrstPref, microPref also accept fine grained values as -0..-100
// microPref: 0=low 1=normal 2=high micro contrast
// fx0, fy0 - arrays of top-left corner coordinates of warped frames
// fsx, fsy - arrays of dimensions of warped frames (individual dimensions per each frame)
// sx, sy: dimensions of input frames (width, height)
// postFilter: set to 1 to apply post-filtering and possibly sharpening to the result
int Pano_Preview
(
	void  **instance,
	Uint8 **in,
	Uint8 * Preview,
	Uint8 *restrict debug1,
	Uint8 *restrict debug2,
	int   SensorGain,
	int   ctrstPref,
	int   microPref,
   	int   *fx0,
   	int   *fy0,
   	int   *fsx,
   	int   *fsy,
	int   sx,
	int   sy,
	int   nFrames,
	int   postFilter
);


// Pano_Preview2 - call to construct fused preview
//
// instance - pointer to instance set by Hdr_Preview
// Preview - (sx/4) x (sy/4) preview image in interleaved RGB888 format
//
int Pano_Preview2
(
	void *instance,
	Uint8 * Preview
);


// Pano_Process - call to construct full-size panorama (should be called after Pano_Preview)
//
// instance - pointer to instance set by Hdr_Preview
// Out - SX x SY full-size composed image in YUV format
// Set Out to NULL before calling Pano_Process for it to be allocated within Pan_Process
// or set it to SX x SY buffer.
// Caller should free 'Out' once it is not needed.
//
int Pano_Process
(
	void *instance,
	Uint8 ** Out
);


// Pano_Cancel - may be called by invoking application during Pano_Process to cancel
// full-size computation
void Pano_Cancel
(
	void *instance
);


#if defined __cplusplus
}
#endif

#endif // __PANORAMA_H__
