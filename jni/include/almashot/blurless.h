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

 Blurless Exposure public header

\* ------------------------------------------------------------------------- */

#ifndef __BLURLESS_H__
#define __BLURLESS_H__

#if defined __cplusplus
extern "C"
{
#endif


#include "almashot.h"

// BlurLess_Preview - call to construct preview
//
// instance - an address of a pointer to hold instance-related data, to be later passed to BlurLess_Process
// in - four or six captured frames in YUV format. in[0]-in[n] should be allocated with malloc
// Caller should allocate but should not free in[0]-in[n] buffers! (They are freed internally in BlurLess_Process)
// Preview - sx/4 x sy/4 preview image in interleaved RGB888 format
// SensorGain is the sensor gain during capture, in u8.8 fixed-point format
// DeGhostGain is used to adjust amount of de-ghosting, set to 256
// DeGhostFrame - number of frames to be used to get moving objects data, recommended: 1
// Mode:
//   0 - four input frames, with exposure adjustment, BE processing method 1
//   1 - six input frames with the same exposure, BE processing method 2
//   2 - six to ten input frames with the same exposure, BE processing method 3
// nFrames - number of input frames
// sx, sy: dimensions of input frames (width, height)
// preFilter - apply pre-filtering (useful for sensors with high noise)
// postFilter - apply luma and chroma post-filtering (soft-threshold median, sensor-gain and lens-shading dependent)
// zeroBlackLevel - do not subtract black level from the input frames
// enhanceLuma/enhanceChroma - amount of luma/chroma enhancement to apply (only in Mode 2), recommended value: 9
// 0 - no enhancement
// 1..10 - enhance (the higher the number the brighter or more colorful image output will be)
//
int BlurLess_Preview
(
	void **instance,
	Uint8 **in,
	Uint8 * Preview,
	Uint8 *restrict debug1,
	Uint8 *restrict debug2,
	int   SensorGain,
	int   DeGhostGain,
	int   DeGhostFrames,
	int   Mode,
	int   nFrames,
	int   sx,
	int   sy,
	int   preFilter,
	int   postFilter,
	int   postSharpen,
	int   zeroBlackLevel,
	int   enhanceLuma,
	int   enhanceChroma,
	int   externalBuffers
);

// BlurLess_Process - call to construct full-size image (should be called after BlurLess_Preview)
//
// instance - pointer to instance set by BlurLess_Preview
// out - SX x SY full-size composed image in interleaved RGB888 format
// caller should free 'out' once it is not needed.
//
int BlurLess_Process
(
	void  *instance,
	Uint8 ** Out,
	int		*x0_out,
	int		*y0_out,
	int		*w_out,
	int		*h_out
);


#if defined __cplusplus
}
#endif

#endif // __BLURLESS_H__
