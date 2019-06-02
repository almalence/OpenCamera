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

  Super Zoom public header

\* ------------------------------------------------------------------------- */

#ifndef __SUPERZOOM_H__
#define __SUPERZOOM_H__

#if defined __cplusplus
extern "C"
{
#endif


#include "almashot.h"

// Maximum accepted input frames
#define SUPERZOOM_MAX_FRAMES	15

// Input frames should be that much larger at each edge
// to provide expected zoom level when SizeGuaranteeMode is set
#ifndef SIZE_GUARANTEE_BORDER   // Eugene: added this to make it possible to define the border size in Makefile
#define SIZE_GUARANTEE_BORDER	64
#endif

#define SPARE_LINES_RESOLVE_FRAME	658 // 656

// -------------------------------------------------------------------
// Still image superzoom functions

enum {
	ALMA_SUPER_NOSRES = 1, 
	ALMA_SUPER_NOLARGEDISP = 2, // !largeDisplacements
	ALMA_SUPER_DROLOCAL = 4,
	ALMA_SUPER_EXTBUF = 8, // externalBuffers
	ALMA_SUPER_SIZEGUARANTEE = 0x10,
	ALMA_SUPER_VIDEOBOUND = 0x20, // special frame bounding for RTSS
	ALMA_SUPER_REFBLUR = 0x40,
	ALMA_SUPER_FASTFILTER = 0x80,
	ALMA_SUPER_DROSLOVENIAN = 0x100,
	ALMA_SUPER_NLNET = 0x200,
	ALMA_SUPER_FUSEBICUBIC = 0x400,
	ALMA_SUPER_NORMALIZE = 0x800,
	ALMA_SUPER_NOFILTREFUV1 = 0x1000,
	ALMA_SUPER_NOFILTREFUV2 = 0x2000,
	ALMA_SUPER_ADRCFIX = 0x4000
};

int SuperZoom_Preview(
	void **instance,
	Uint8 **in,
	Uint8 **inUV,
	Uint8 *Preview,
	int sx,
	int sy,
	int stride,
	int sxo,
	int syo,
	int ostride,
	int sxp,
	int syp,
	int nImages,
	int flags,
	int SensorGain,
	int DeGhostGain,
	int DeGhostFrames,
	int kelvin1,
	int kelvin2,
	int postFilter,
	int postSharpen,
	int *nBaseFrame,
	int cameraIndex,
	int maxStab
);

// instance - pointer to instance set by SuperZoom_Preview
int SuperZoom_Process
(
	void  * instance,
	Uint8 ** out,
	Uint8 ** outUV,
	int		*x0_out,
	int		*y0_out,
	int		*w_out,
	int		*h_out
);

// Cancel superzoom processing (stop SuperZoom_Process)
void SuperZoom_CancelProcessing
(
	void  * instance
);

// SuperZoom_FreeInstance - can be called after SuperZoom_Preview instead of SuperZoom_Process
// if full-size processing is not needed.
// Set keepBuffers to non-zero to keep input frame buffers and only free processing instance.
void SuperZoom_FreeInstance
(
	void *instance,
	int keepBuffers
);


// -------------------------------------------------------------------
// Video superzoom functions

int SuperZoom_StartStreaming
(
	void  **instance,
	int		sx,
	int		sy,
	int		stride,
	int		sxo,
	int		syo,
	int     DeGhostFrames,
	int		noSres,
	int     cameraIndex
);


int SuperZoom_StopStreaming
(
	void  *instance
);


#if defined __cplusplus
}
#endif

#endif // __SUPERZOOM_H__
