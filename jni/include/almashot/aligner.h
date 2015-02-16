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

 Various image alignment functions

\* ------------------------------------------------------------------------- */


#ifndef ALIGNER_H_
#define ALIGNER_H_

#if defined __cplusplus
extern "C"
{
#endif


#include "almashot.h"


#define SCRATCH_SIZE	360448


void AlmaShot_EstimateGlobalTranslation
(
	Uint8 * restrict * in,
	int sx,
	int sy,
	Int32 *dx,
	Int32 *dy,
	int nBaseFrame,
	int nFrames,
	Int32 *confidence,
	Uint8 * scratch
);


// estimationMethod:
// 0 = small displacements only
// 1 = large displacements
void AlmaShot_EstimateTranslationAndRotation
(
	Uint8 ** in,
	Int32 *dx,
	Int32 *dy,
	Int32 *rot,
	Int32 *sharp,
	int sx,
	int sy,
	int nBaseFrame,
	int nFrames,
	int nRef,
	int estimationMethod,
	Uint8 * scratch
);


int AlmaShot_DigestInitialize
(
	void **instance,
	int sx,
	int sy
);

void AlmaShot_DigestRelease(void *instance);


// Return: strength of the weakest corner included in the digest
// (indication of both how sharp the frame is and if it has strong features for matching)
int AlmaShot_ComputeDigest
(
	void *instance,
	Uint8 * Y,
	Uint8 *digest_img
);

// Return: strength of the weakest corner for the digest_img
int AlmaShot_EstimateTranslationAndRotationQuick
(
	void *instance,
	Uint8 * in,
	Int32 *dx,
	Int32 *dy,
	Int32 *rot,
	Uint8 * digest_ref,
	Uint8 * digest_img
);


#if defined __cplusplus
}
#endif

#endif /* ALIGNER_H_ */
