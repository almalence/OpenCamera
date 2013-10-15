/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

#ifndef bim_FUNCTIONS_EM_H
#define bim_FUNCTIONS_EM_H

/**
 * This files contains gerenral purpose functions.
 */

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/UInt8Arr.h"
#include "b_TensorEm/Functions.h"
#include "b_TensorEm/Flt16Alt2D.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/** pyramidal image type */
enum bim_PyramidalImageType
{
	bim_UINT8_PYRAMIDAL_IMG,	/* byte representation of pyramical image */
	bim_UINT16_PYRAMIDAL_IMG	/* 16-bit representation of pyramical image */
};

/* ---- constants ---------------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/** Warps an image with intermediate pyramidal downscaling if possible in order to minimize aliasing
 *  The actual warping happens using pixel interpolation
 *  *bufPtrA is an intermediate byte array that holds downscaled data (only needed when pyramidal downscaling happens; can be NULL otherwise)
 *  scaleThresholdA (16.16): 
 *		specifies the minimum scale ratio (inImage/outImage) required to initiate prior filtering
 *      A value range of 2.0...4.0 is recommended (<= 0.0: disabled)
 *
 *  offsPtrA specifies the pixel position (0,0) in the input image (format 16.0)
 */
void bim_filterWarpInterpolation( struct bbs_Context* cpA,
								  uint8* dstImagePtrA, 
								  const uint8* srcImagePtrA,
								  uint32 srcImageWidthA,
								  uint32 srcImageHeightA,
								  const struct bts_Int16Vec2D* offsPtrA,
								  const struct bts_Flt16Alt2D* altPtrA,
								  uint32 dstWidthA,
								  uint32 dstHeightA,
								  struct bbs_UInt8Arr* bufPtrA,
								  uint32 scaleThresholdA );

/** Warps an image with intermediate pyramidal downscaling if possible in order to minimize aliasing
 *  The actual warping happens using pixel replication (fast but prone to artefacts)
 *  *bufPtrA is an intermediate byte array that holds downscaled data (only needed when pyramidal downscaling happens; can be NULL otherwise)
 *  scaleThresholdA (16.16): 
 *		specifies the minimum scale ratio (inImage/outImage) required to initiate prior filtering
 *      A value range of 2.0...4.0 is recommended (0.0: disabled)
 *  offsPtrA specifies the pixel position (0,0) in the input image (format 16.0)
 */
void bim_filterWarpPixelReplication( struct bbs_Context* cpA,
									 uint8* dstImagePtrA, 
									 const uint8* srcImagePtrA,
									 uint32 srcImageWidthA,
									 uint32 srcImageHeightA,
								     const struct bts_Int16Vec2D* offsPtrA,
									 const struct bts_Flt16Alt2D* altPtrA,
									 uint32 dstWidthA,
									 uint32 dstHeightA,
									 struct bbs_UInt8Arr* bufPtrA,
									 uint32 scaleThresholdA );

/** Selects proper warp function above
 *  offsPtrA specifies the pixel position (0,0) in the input image (format 16.0)
 */
void bim_filterWarp( struct bbs_Context* cpA,
					 uint8* dstImagePtrA, 
					 const uint8* srcImagePtrA,
					 uint32 srcImageWidthA,
					 uint32 srcImageHeightA,
  				     const struct bts_Int16Vec2D* offsPtrA,
					 const struct bts_Flt16Alt2D* altPtrA,
					 uint32 dstWidthA,
					 uint32 dstHeightA,
					 struct bbs_UInt8Arr* bufPtrA,
					 uint32 scaleThresholdA,
					 flag interpolateA );

#endif /* bim_FUNCTIONS_EM_H */

