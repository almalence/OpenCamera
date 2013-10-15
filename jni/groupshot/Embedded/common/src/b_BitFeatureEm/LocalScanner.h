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

#ifndef bbf_LOCAL_SCANNER_EM_H
#define bbf_LOCAL_SCANNER_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/UInt32Arr.h"
#include "b_BasicEm/Int32Arr.h"
#include "b_BasicEm/Int16Arr.h"
#include "b_BasicEm/MemTbl.h"
#include "b_BasicEm/UInt16Arr.h"
#include "b_TensorEm/Int16Rect.h"
#include "b_ImageEm/UInt32Image.h"

#include "b_BitFeatureEm/Feature.h"
#include "b_BitFeatureEm/BitParam.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bbf_LOCAL_SCANNER_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** supports scanning an image on a fixed scale and provides patches on local areas as needed */
struct bbf_LocalScanner 
{
	/* ---- private data --------------------------------------------------- */

	/** current scan x-coordinate */
	int32 xE;

	/** current scan y-coordinate */
	int32 yE;

	/** current xOffset */
	int32 xOffE;

	/** current yOffset */
	int32 yOffE;

	/** width of scaled image */
	uint32 currentWidthE;

	/** height of scaled image */
	uint32 currentHeightE;

	/** width of work image */
	uint32 workWidthE;

	/** height of work image */
	uint32 workHeightE;

	/** pointer to working image data */
	const uint8* workImagePtrE;

	/** width of original image */
	uint32 origWidthE;

	/** height of original image */
	uint32 origHeightE;

	/** pointer to original image data */
	const uint8* origImagePtrE;

	/** parameter for bit generation */
	struct bbf_BitParam bitParamE;

	/** work image (two pixels per uint16)*/
	struct bbs_UInt8Arr workImageBufferE;

	/** summed-area table (ring buffer) */
	struct bim_UInt32Image satE;

	/** bit image */
	struct bim_UInt32Image bitImageE;

	/** patch buffer */
	struct bbs_UInt32Arr patchBufferE;

	/** original scan region */
	struct bts_Int16Rect origScanRegionE;

	/** current scan region */
	struct bts_Int16Rect workScanRegionE;

	/* ---- public data ---------------------------------------------------- */

	/** patch width */
	uint32 patchWidthE;
	
	/** patch height */
	uint32 patchHeightE;
	
	/** scale exponent (determines at which scale patch data is actually generated) */
	uint32 scaleExpE;

	/** max image width */
	uint32 maxImageWidthE;
	
	/** max image height */
	uint32 maxImageHeightE;
	
	/** min scale exponent */
	uint32 minScaleExpE;
	
	/** maximum filter radius */
	uint32 maxRadiusE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbf_LocalScanner  */
void bbf_LocalScanner_init( struct bbs_Context* cpA,
							struct bbf_LocalScanner* ptrA );

/** resets bbf_LocalScanner  */
void bbf_LocalScanner_exit( struct bbs_Context* cpA,
							struct bbf_LocalScanner* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbf_LocalScanner_copy( struct bbs_Context* cpA,
							struct bbf_LocalScanner* ptrA, 
							const struct bbf_LocalScanner* srcPtrA );

/** equal operator */
flag bbf_LocalScanner_equal( struct bbs_Context* cpA,
							 const struct bbf_LocalScanner* ptrA, 
							 const struct bbf_LocalScanner* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** total scan positions at current scale */
uint32 bbf_LocalScanner_positions( const struct bbf_LocalScanner* ptrA );

/** current scan index */
uint32 bbf_LocalScanner_scanIndex( const struct bbf_LocalScanner* ptrA );

/** returns ul position relative to original image; x,y coordinates: 16.16 */
void bbf_LocalScanner_pos( const struct bbf_LocalScanner* ptrA, int32* xPtrA, int32* yPtrA );

/** returns uls position relative to original image at index position; x,y coordinates: 16.16 */
void bbf_LocalScanner_idxPos( const struct bbf_LocalScanner* ptrA, uint32 scanIndexA, int32* xPtrA, int32* yPtrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** creates & initializes object */
void bbf_LocalScanner_create( struct bbs_Context* cpA,
							  struct bbf_LocalScanner* ptrA, 
							  uint32 patchWidthA,
							  uint32 patchHeightA,
							  uint32 scaleExpA,
							  uint32 maxImageWidthA,
							  uint32 maxImageHeightA,
							  uint32 minScaleExpA,
							  uint32 maxRadiusA,
							  struct bbs_MemTbl* mtpA );

/** parameter for bit generation + recomputing bit image */
void bbf_LocalScanner_bitParam( struct bbs_Context* cpA,
							    struct bbf_LocalScanner* ptrA,
								const struct bbf_BitParam* bitParamPtrA );

/** Specifies a (sub-) scan region
 *  Scanning within a given scale is limited to that region.
 *  Region is truncateed to physical region
 *  Image assignment, bitParam assignment and function nextScale reset the sacn region to 
 *  the whole image.
 */
void bbf_LocalScanner_origScanRegion( struct bbs_Context* cpA,
									  struct bbf_LocalScanner* ptrA,
									  const struct bts_Int16Rect* scanRegionPtrA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bbf_LocalScanner_memSize( struct bbs_Context* cpA,
						         const struct bbf_LocalScanner* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bbf_LocalScanner_memWrite( struct bbs_Context* cpA,
								  const struct bbf_LocalScanner* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bbf_LocalScanner_memRead( struct bbs_Context* cpA,
								 struct bbf_LocalScanner* ptrA, 
								 const uint16* memPtrA, 
								 struct bbs_MemTbl* mtpA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** resets scan position at current scale level */
void bbf_LocalScanner_resetScan( struct bbs_Context* cpA,
								 struct bbf_LocalScanner* ptrA );

/** assigns image; sets initial bit parameters; resets processor */
void bbf_LocalScanner_assign( struct bbs_Context* cpA,
							  struct bbf_LocalScanner* ptrA,
							  const uint8* imagePtrA, 
							  uint32 imageWidthA,
							  uint32 imageHeightA,
							  const struct bbf_BitParam* paramPtrA );

/** returns pointer to patch data */
const uint32* bbf_LocalScanner_getPatch( const struct bbf_LocalScanner* ptrA );

/** goes to next scan position */
flag bbf_LocalScanner_next( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA );

/** goes to scan position (integer coordinate numbers) */
void bbf_LocalScanner_goToXY( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA, int32 xA, int32 yA );

/** goes to scan position */
void bbf_LocalScanner_goToIndex( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA, uint32 scanIndexA );

/** goes to next offset position */
flag bbf_LocalScanner_nextOffset( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA );

#endif /* bbf_LOCAL_SCANNER_EM_H */

