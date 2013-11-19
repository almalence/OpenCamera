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

 Moving Objects Removal public header

\* ------------------------------------------------------------------------- */

#ifndef __MOVOBJ_H__
#define __MOVOBJ_H__

#if defined __cplusplus
extern "C"
{
#endif


#include "almashot.h"
#include "bestshot.h"


int MovObj_Process
(
	void  **instance,
	Uint8 **in,
	Uint8 *Out,
	Uint8 *restrict Layout,
	Uint8 *restrict reserved,
	int   SensorGain,
	int   sx,
	int   sy,
	int   nFrames,
	int   sensitivity,
	int   minSize,
	int   weakObj,
	int   ignoreGhosting,
	int   extraBorder,
	int   seqPhotoMode,
	int  *seqFrameOrder,
	int   maxDisplace,
	int   postFilter,
	int   scaleOutput,
	int   *x0_base,
	int   *y0_base,
	int   *w_base,
	int   *h_base,
	int   *x0_out,
	int   *y0_out,
	int   *w_out,
	int   *h_out,
	int   *baseFrame,
	int   fastMode,
	int   discardUnfocused
);


void MovObj_FixHoles
(
	Uint8 *layout,
	int sx,
	int sy,
	int baseFrame
);


int MovObj_Enumerate
(
	Uint8 *layout,
	Uint8 *enumer,
	int sx,
	int sy,
	int baseFrame,
	int nFrames
);


void MovObj_FreeInstance
(
	void  *instance
);


#if defined __cplusplus
}
#endif

#endif // __MOVOBJ_H__
