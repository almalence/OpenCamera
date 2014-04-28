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

 AlmaShot public header

\* ------------------------------------------------------------------------- */

#ifndef __ALMASHOT_H__
#define __ALMASHOT_H__

#if defined __cplusplus
extern "C"
{
#endif

#if (!defined(__STDC_VERSION__)) || (__STDC_VERSION__ < 199901L)
// restrict keyword is not known in pre-C99 dialects
#define restrict
#endif

typedef int Int32;
typedef short Int16;
typedef unsigned int Uint32;
typedef unsigned short Uint16;
typedef unsigned char Uint8;
typedef signed char Int8;


// ---------------------------------------------------------------------------
// Defines
// ---------------------------------------------------------------------------

#define MAX_FRAMES	50	// at most 50 frames are accepted where applicable

// Error codes
#define ALMA_ALL_OK					0
#define ALMA_NOT_ENOUGH_MEMORY		1
#define ALMA_INVALID_INSTANCE		2
#define ALMA_FRAMES_TOO_LARGE		3
#define ALMA_INVALID_NUM_FRAMES		4
#define ALMA_INVALID_BUFFERS		5
#define ALMA_GL_CONTEXT_ERROR		6
#define ALMA_GL_SURFACE_ERROR		7
#define ALMA_INVALID_PARAMETER		8


// ---------------------------------------------------------------------------
// Function prototypes
// ---------------------------------------------------------------------------

int AlmaShot_Initialize(int initGlFramework);
int AlmaShot_Release(void);
int AlmaShot_ReAcquireOpenGL(void);
int AlmaShot_ReleaseOpenGL(void);

char * AlmaShot_GetVersionString(void);

void AlmaShot_SubsampleForPreview
(
	Uint8 *restrict in,
	Uint8 *restrict out,
	int sx,
	int sy,
	int sxp,
	int syp,
	int raw
);

void AlmaShot_ComposeRGBi
(
	Uint8 *in,
	Uint8 *out,
	int   sx,
	int   sy,
	int   x0,
	int   y0,
	int   w,
	int   h,
	int   pitch
);

void AlmaShot_ComposeRotatedRGBi
(
	Uint8 *in,
	Uint8 *out,
	int   sx,
	int   sy,
	int   x0,
	int   y0,
	int   w,
	int   h,
	int   pitch
);

void AlmaShot_Preview2RGBi
(
	Uint8 *in,
	Uint8 *out,
	int   sx,
	int   sy,
	int   x0,
	int   y0,
	int   w,
	int   h,
	int   pitch
);

void AlmaShot_ComposeRGBAi
(
	Uint8 *in,
	Uint8 *out,
	int   sx,
	int   sy,
	int   x0,
	int   y0,
	int   w,
	int   h,
	int   pitch
);

void AlmaShot_ComposeBGRAi
(
	Uint8 *in,
	Uint8 *out,
	int   sx,
	int   sy,
	int   x0,
	int   y0,
	int   w,
	int   h,
	int   pitch
);

void AlmaShot_ComposeRotatedRGBAi
(
	Uint8 *in,
	Uint8 *out,
	int   sx,
	int   sy,
	int   x0,
	int   y0,
	int   w,
	int   h,
	int   pitch
);

void AlmaShot_ComposeRotatedBGRAi
(
	Uint8 *in,
	Uint8 *out,
	int   sx,
	int   sy,
	int   x0,
	int   y0,
	int   w,
	int   h,
	int   pitch
);

void AlmaShot_Preview2RGBAi
(
	Uint8 *in,
	Uint8 *out,
	int   sx,
	int   sy,
	int   x0,
	int   y0,
	int   w,
	int   h,
	int   pitch
);

void AlmaShot_Preview2BGRAi
(
	Uint8 *in,
	Uint8 *out,
	int   sx,
	int   sy,
	int   x0,
	int   y0,
	int   w,
	int   h,
	int   pitch
);

void AlmaShot_SwapUV
(
	Uint8 *restrict in,
	int    sx,
	int    sy
);

void AlmaShot_ConvertNV12toNV16
(
	Uint8 *restrict in,
	Uint8 *restrict out,
	int    sx,
	int    sy
);
    
void AlmaShot_ConvertNV21toUYVY
(
    Uint8 *restrict in,
    Uint8 *restrict out,
    int sx,
    int sy
);

void AlmaShot_PauseProcessing(void);
void AlmaShot_ResumeProcessing(void);

// returns processing time in msec
int AlmaShot_MeasureProcessingTimeSuperZoom(int sx, int sy);
int AlmaShot_MeasureProcessingTimeHdr(int sx, int sy);
int AlmaShot_MeasureProcessingTimeBlurLess(int sx, int sy);




#if defined __cplusplus
}
#endif

#endif // __ALMASHOT_H__
