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

#ifndef bim_UINT8_IMAGE_EM_H
#define bim_UINT8_IMAGE_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/UInt8Arr.h"
#include "b_TensorEm/Int16Rect.h"
#include "b_TensorEm/Flt16Alt2D.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bim_UINT8_IMAGE_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** image of uint8 */
struct bim_UInt8Image 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** width of image */
	uint32 widthE;

	/** height of image */
	uint32 heightE;

	/** array of bytes */
	struct bbs_UInt8Arr arrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bim_UInt8Image  */
void bim_UInt8Image_init( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA );

/** allocates memory for bim_UInt8Image */
void bim_UInt8Image_create( struct bbs_Context* cpA,
						    struct bim_UInt8Image* ptrA, 
						    uint32 widthA, 
							uint32 heightA,
 					        struct bbs_MemSeg* mspA );

/** destructor of bim_UInt8Image  */
void bim_UInt8Image_exit( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bim_UInt8Image_copy( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA, 
						  const struct bim_UInt8Image* srcPtrA );

/** equal operator */
flag bim_UInt8Image_equal( struct bbs_Context* cpA,
						   const struct bim_UInt8Image* ptrA, 
						   const struct bim_UInt8Image* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** checksum of image (for debugging purposes) */
uint32 bim_UInt8Image_checkSum( struct bbs_Context* cpA,
							    const struct bim_UInt8Image* ptrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** assigns external image to array (no allocation, deallocation or copying of data) */
void bim_UInt8Image_assignExternalImage( struct bbs_Context* cpA,
										 struct bim_UInt8Image* ptrA, 
										 struct bim_UInt8Image* srcPtrA );

/** sets image size */
void bim_UInt8Image_size( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA, 
						  uint32 widthA, 
						  uint32 heightA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bim_UInt8Image_memSize( struct bbs_Context* cpA,
							   const struct bim_UInt8Image* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bim_UInt8Image_memWrite( struct bbs_Context* cpA,
							    const struct bim_UInt8Image* ptrA, 
								uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bim_UInt8Image_memRead( struct bbs_Context* cpA,
							   struct bim_UInt8Image* ptrA, 
							   const uint16* memPtrA,
 					           struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** sets all pixels to one value */
void bim_UInt8Image_setAllPixels( struct bbs_Context* cpA,
								  struct bim_UInt8Image* ptrA, 
								  uint8 valueA );
							
/** copies a section of given image */
void bim_UInt8Image_copySection( struct bbs_Context* cpA,
								 struct bim_UInt8Image* ptrA, 
								 const struct bim_UInt8Image* srcPtrA, 
								 const struct bts_Int16Rect* sectionPtrA );

/** applies affine linear warping to pixels positions of imageA before copying the into *ptrA 
 *  xOffsA, yOffsA specify an additional offset vector (16.0) that is added to image coordinates
 */
void bim_UInt8Image_warpOffs( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA, 
						  const struct bim_UInt8Image* srcPtrA, 
						  int32 xOffsA,
						  int32 yOffsA,
						  const struct bts_Flt16Alt2D* altPtrA,
			              int32 resultWidthA,
			              int32 resultHeightA );

/** applies affine linear warping to pixels positions of imageA before copying the into *ptrA */
void bim_UInt8Image_warp( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA, 
						  const struct bim_UInt8Image* srcPtrA, 
						  const struct bts_Flt16Alt2D* altPtrA,
			              int32 resultWidthA,
			              int32 resultHeightA );

#endif /* bim_UINT8_IMAGE_EM_H */

