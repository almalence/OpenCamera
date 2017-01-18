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

#ifndef bbf_SCANNER_EM_H
#define bbf_SCANNER_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/UInt32Arr.h"
#include "b_BasicEm/Int32Arr.h"
#include "b_BasicEm/Int16Arr.h"
#include "b_BasicEm/MemTbl.h"
#include "b_BasicEm/UInt16Arr.h"
#include "b_ImageEm/UInt32Image.h"

#include "b_BitFeatureEm/Feature.h"
#include "b_BitFeatureEm/BitParam.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bbf_SCANNER_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** scans an image and provides patches as needed */
struct bbf_Scanner 
{
	/* ---- private data --------------------------------------------------- */

	/** downscale exponent */
	uint32 scaleExpE;

	/** current scale (12.20) */
	uint32 scaleE;

	/** current scan x-coordinate */
	int32 xE;

	/** current scan y-coordinate */
	int32 yE;

	/** effective maximum scale (12.20) */
	uint32 effMaxScaleE;

	/** width of scaled image */
	uint32 currentWidthE;

	/** height of scaled image */
	uint32 currentHeightE;

	/** width of work image */
	uint32 workWidthE;

	/** height of work image */
	uint32 workHeightE;

	/** parameter for bit generation */
	struct bbf_BitParam bitParamE;

	/** work image (two pixels per uint16)*/
	struct bbs_UInt16Arr workImageE;

	/** summed-area table (ring buffer) */
	struct bim_UInt32Image satE;

	/** bit image */
	struct bim_UInt32Image bitImageE;

	/** patch buffer */
	struct bbs_UInt32Arr patchBufferE;

	/** image line buffer */
	struct bbs_UInt16Arr lineBufE;



	/** index position buffer */
	struct bbs_UInt32Arr idxArrE;

	/** activity buffer */
	struct bbs_Int32Arr actArrE;

	/** composite output buffer */
	struct bbs_Int32Arr outArrE;

	/* internal positions detected */
	uint32 intCountE;

	/* output positions detected */
	uint32 outCountE;

	/** Face positions buffer size (approx.: max faces * 20...60) 
	 *  This variable is not part of I/O and must be set before calling memRead
	 *  Default value: 1024 -> about 100...200 faces/image detectable
	 *
	 *  The Scanner allocates internally bufferSizeE * 10 bytes of exclusive memory
	 */
	uint32 bufferSizeE;

	/* ---- public data ---------------------------------------------------- */

	/** maximum image width */
	uint32 maxImageWidthE;

	/** maximum image height */
	uint32 maxImageHeightE;

	/** maximum filter radius */
	uint32 maxRadiusE;

	/** patch width */
	uint32 patchWidthE;
	
	/** patch height */
	uint32 patchHeightE;
	
	/** minimum scale (12.20) */
	uint32 minScaleE;

	/** maximum scale (12.20) (0: unlimited) */
	uint32 maxScaleE;

	/** scale step factor (1.32) (leading bit is always one and therfore ignored) */
	uint32 scaleStepE;

	/** x-border in pixels */
	uint32 borderWidthE;

	/** y-border in pixels */
	uint32 borderHeightE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbf_Scanner  */
void bbf_Scanner_init( struct bbs_Context* cpA,
					   struct bbf_Scanner* ptrA );

/** resets bbf_Scanner  */
void bbf_Scanner_exit( struct bbs_Context* cpA,
					   struct bbf_Scanner* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbf_Scanner_copy( struct bbs_Context* cpA,
					   struct bbf_Scanner* ptrA, 
					   const struct bbf_Scanner* srcPtrA );

/** equal operator */
flag bbf_Scanner_equal( struct bbs_Context* cpA,
					    const struct bbf_Scanner* ptrA, 
					    const struct bbf_Scanner* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** scan positions at current scale */
uint32 bbf_Scanner_positions( const struct bbf_Scanner* ptrA );

/** current scan index */
uint32 bbf_Scanner_scanIndex( const struct bbf_Scanner* ptrA );

/** returns current uls position relative to original image; x,y: 16.16; scale: 12.20 */
void bbf_Scanner_pos( const struct bbf_Scanner* ptrA, 
					  int32* xPtrA, int32* yPtrA, uint32* scalePtrA );

/** returns uls position relative to original image at index position; x,y: 16.16; scale: 12.20 */
void bbf_Scanner_idxPos( const struct bbf_Scanner* ptrA, uint32 scanIndexA,
					     int32* xPtrA, int32* yPtrA, uint32* scalePtrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** creates & initializes object */
void bbf_Scanner_create( struct bbs_Context* cpA,
						 struct bbf_Scanner* ptrA, 
						 flag maximizeSharedMemoryA,
						 uint32 maxImageWidthA,
					 	 uint32 maxImageHeightA,
						 uint32 maxRadiusA,
						 uint32 patchWidthA,
						 uint32 patchHeightA,
						 uint32 minScaleA,
						 uint32 maxScaleA,
						 uint32 scaleStepA,
						 uint32 borderWidthA,
						 uint32 borderHeightA,
						 uint32 bufferSizeA,
						 struct bbs_MemTbl* mtpA );

/** parameter for bit generation + recomputing bit image */
void bbf_Scanner_bitParam( struct bbs_Context* cpA,
						   struct bbf_Scanner* ptrA,
						   const struct bbf_BitParam* bitParamPtrA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bbf_Scanner_memSize( struct bbs_Context* cpA,
					        const struct bbf_Scanner* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bbf_Scanner_memWrite( struct bbs_Context* cpA,
							 const struct bbf_Scanner* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bbf_Scanner_memRead( struct bbs_Context* cpA,
							struct bbf_Scanner* ptrA, 
							const uint16* memPtrA, 
							struct bbs_MemTbl* mtpA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** resets scan position at current scale level */
void bbf_Scanner_resetScan( struct bbs_Context* cpA, struct bbf_Scanner* ptrA );

/** Assigns image; sets initial bit parameters; resets processor.
 *  If roiPtrA is NULL, the whole image is considered for processsing 
 *  otherwise *roiPtrA specifies a section of the original image to which
 *  procesing is limited. All coordinates refer to that section and must 
 *  eventually be corrected externally.
 *  The roi rectangle must not include pixels outside of the original image
 *  (checked -> error). The rectangle may be of uneven width.
 */
void bbf_Scanner_assign( struct bbs_Context* cpA, struct bbf_Scanner* ptrA,
					     const void* imagePtrA,
						 uint32 imageWidthA,
						 uint32 imageHeightA,
						 const struct bts_Int16Rect* roiPtrA,
						 const struct bbf_BitParam* paramPtrA );

/** goes to next scale position */
flag bbf_Scanner_nextScale( struct bbs_Context* cpA, struct bbf_Scanner* ptrA );

/** returns pointer to patch data */
const uint32* bbf_Scanner_getPatch( const struct bbf_Scanner* ptrA );

/** goes to next scan position */
flag bbf_Scanner_next( struct bbs_Context* cpA, struct bbf_Scanner* ptrA );

/** goes to scan position */
void bbf_Scanner_goToXY( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, int32 xA, int32 yA );

/** goes to scan index position */
void bbf_Scanner_goToIndex( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, uint32 scanIndexA );

/** goes to scan position from image uls position (error if scales do not match); x,y: 16.16; scale: 12.20 */
void bbf_Scanner_goToUls( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, 
						  int32 xA, int32 yA, uint32 scaleA );

/** The functions below offer positions management of temporary positions needed by the detector object */

/** resets internal positions */
void bbf_Scanner_resetIntPos( struct bbs_Context* cpA, struct bbf_Scanner* ptrA );

/** reset output positions */
void bbf_Scanner_resetOutPos( struct bbs_Context* cpA, struct bbf_Scanner* ptrA ) ;

/* add internal position */
void bbf_Scanner_addIntPos( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, uint32 idxA,	int32 actA );

/* add external position */
void bbf_Scanner_addOutPos( struct bbs_Context* cpA, 
							struct bbf_Scanner* ptrA, 
							int32 xA, 
							int32 yA, 
							uint32 scaleA, 
							int32 actA );

/* removes internal overlaps */
uint32 bbf_Scanner_removeIntOverlaps( struct bbs_Context* cpA, 
								      struct bbf_Scanner* ptrA,
									  uint32 overlapThrA );

/** removes output overlaps */
uint32 bbf_Scanner_removeOutOverlaps( struct bbs_Context* cpA, 
							          struct bbf_Scanner* ptrA,
									  uint32 overlapThrA );

#endif /* bbf_SCANNER_EM_H */

