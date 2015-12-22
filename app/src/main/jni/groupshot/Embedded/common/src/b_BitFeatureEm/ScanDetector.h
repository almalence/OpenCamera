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

#ifndef bbf_SCAN_DETECTOR_EM_H
#define bbf_SCAN_DETECTOR_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/UInt32Arr.h"
#include "b_BasicEm/Int16Arr.h"
#include "b_BasicEm/MemTbl.h"
#include "b_TensorEm/IdCluster2D.h"
#include "b_BitFeatureEm/Sequence.h"
#include "b_BitFeatureEm/BitParam.h"
#include "b_BitFeatureEm/Scanner.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bbf_SCAN_DETECTOR_VERSION 100

/* maximum number of features in scan detector */
#define bbf_SCAN_DETECTOR_MAX_FEATURES 4

/* ---- object definition -------------------------------------------------- */

/** discrete feature set */
struct bbf_ScanDetector 
{
	/* ---- private data --------------------------------------------------- */

	/** minimum scale (12.20) */
	uint32 minScaleE;

	/** maximum scale (0: unlimited) (12.20) */
	uint32 maxScaleE;

	/** maximum image width (this variable must be specified before reading the parameter file) */
	uint32 maxImageWidthE;

	/** maximum image height (this variable must be specified before reading the parameter file) */
	uint32 maxImageHeightE;

	/** scanner */
	struct bbf_Scanner scannerE;

	/* ---- public data ---------------------------------------------------- */

	/** patch width */
	uint32 patchWidthE;
	
	/** patch height */
	uint32 patchHeightE;
	
	/** minimum default scale (12.20) */
	uint32 minDefScaleE;

	/** maximum default scale (0: unlimited) (12.20) */
	uint32 maxDefScaleE;

	/** scale step factor (1.32) (leading bit is always one and therefore ignored) */
	uint32 scaleStepE;

	/** overlap threshold (16.16) */
	uint32 overlapThrE;

	/** border width in pixels (refers to scaled image) */
	uint32 borderWidthE;

	/** border height in pixels (refers to scaled image) */
	uint32 borderHeightE;

	/** number of features */
	uint32 featuresE;

	/** bit param array */
	struct bbf_BitParam bitParamArrE[ bbf_SCAN_DETECTOR_MAX_FEATURES ];

	/** feature array */
	struct bbf_Sequence featureArrE[ bbf_SCAN_DETECTOR_MAX_FEATURES ];

	/** reference cluster */
	struct bts_IdCluster2D refClusterE;

	/** reference distance (e.g. eye distance) in ref cluster (16.16) */
	uint32 refDistanceE;
	
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbf_ScanDetector  */
void bbf_ScanDetector_init( struct bbs_Context* cpA,
						    struct bbf_ScanDetector* ptrA );

/** resets bbf_ScanDetector  */
void bbf_ScanDetector_exit( struct bbs_Context* cpA,
						    struct bbf_ScanDetector* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbf_ScanDetector_copy( struct bbs_Context* cpA,
						    struct bbf_ScanDetector* ptrA, 
						    const struct bbf_ScanDetector* srcPtrA );

/** equal operator */
flag bbf_ScanDetector_equal( struct bbs_Context* cpA,
						     const struct bbf_ScanDetector* ptrA, 
						     const struct bbf_ScanDetector* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bbf_ScanDetector_memSize( struct bbs_Context* cpA,
							     const struct bbf_ScanDetector* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bbf_ScanDetector_memWrite( struct bbs_Context* cpA,
							      const struct bbf_ScanDetector* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bbf_ScanDetector_memRead( struct bbs_Context* cpA,
							     struct bbf_ScanDetector* ptrA, 
							     const uint16* memPtrA, 
							     struct bbs_MemTbl* mtpA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** Scans over image returns number of detected positions
 *  After processing the output data are stored in composite format 
 *  in scannerPtrA->outArrE.
 *
 *  The output data array is located after execution at *outArrPtrPtrA.
 *  The output data are organized as follows: 
 *     x(16.16) y(16.16), scale(12.20), confidence(4.28),   x(16.16)....
 *
 *  All positions are sorted by descending confidence
 *
 *  If no faces were found, the function returns 0 but the output contains 
 *  one valid position with the highest confidence; the associated activity 
 *  value is negative (or 0) in that case. 
 *
 *  If roiPtrA is NULL, the whole image is considered for processsing 
 *  otherwise *roiPtrA specifies a section of the original image to which
 *  processing is limited. All coordinates refer to that section and must 
 *  eventually be adjusted externally.
 *  The roi rectangle must not include pixels outside of the original image
 *  (checked -> error). The rectangle may be of uneven width.
 */
uint32 bbf_ScanDetector_process( struct bbs_Context* cpA, 
							     struct bbf_ScanDetector* ptrA,
							     const void* imagePtrA,
								 uint32 imageWidthA,
								 uint32 imageHeightA,
								 const struct bts_Int16Rect* roiPtrA,
								 int32** outArrPtrPtrA );

#endif /* bbf_SCAN_DETECTOR_EM_H */

