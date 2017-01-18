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

#ifndef bim_UINT32_IMAGE_EM_H
#define bim_UINT32_IMAGE_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/UInt32Arr.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bim_UINT32_IMAGE_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** image of uint32 */
struct bim_UInt32Image 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** width of image */
	uint32 widthE;

	/** height of image */
	uint32 heightE;

	/** array of bytes */
	struct bbs_UInt32Arr arrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bim_UInt32Image  */
void bim_UInt32Image_init( struct bbs_Context* cpA,
						   struct bim_UInt32Image* ptrA );

/** destroys bim_UInt32Image  */
void bim_UInt32Image_exit( struct bbs_Context* cpA,
						   struct bim_UInt32Image* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bim_UInt32Image_copy( struct bbs_Context* cpA,
						   struct bim_UInt32Image* ptrA, 
						   const struct bim_UInt32Image* srcPtrA );

/** equal operator */
flag bim_UInt32Image_equal( struct bbs_Context* cpA,
						    const struct bim_UInt32Image* ptrA, 
							const struct bim_UInt32Image* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** calculates the amount of heap memory needed (16bit words) if created with given parameters */ 
uint32 bim_UInt32Image_heapSize( struct bbs_Context* cpA,
								 const struct bim_UInt32Image* ptrA, 
								 uint32 widthA, 
								 uint32 heightA );

/** checksum of image (for debugging purposes) */
uint32 bim_UInt32Image_checkSum( struct bbs_Context* cpA,
								 const struct bim_UInt32Image* ptrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates memory for bim_UInt32Image */
void bim_UInt32Image_create( struct bbs_Context* cpA,
							 struct bim_UInt32Image* ptrA, 
						    uint32 widthA, 
							uint32 heightA,
 					        struct bbs_MemSeg* mspA );

/** assigns external image to array (no allocation, deallocation or copying of data) */
void bim_UInt32Image_assignExternalImage( struct bbs_Context* cpA,
										  struct bim_UInt32Image* ptrA, 
										  struct bim_UInt32Image* srcPtrA );

/** sets image size */
void bim_UInt32Image_size( struct bbs_Context* cpA,
						   struct bim_UInt32Image* ptrA, 
						   uint32 widthA, 
						   uint32 heightA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) needs when written to memory */
uint32 bim_UInt32Image_memSize( struct bbs_Context* cpA,
							    const struct bim_UInt32Image* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bim_UInt32Image_memWrite( struct bbs_Context* cpA,
								 const struct bim_UInt32Image* ptrA, 
								 uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bim_UInt32Image_memRead( struct bbs_Context* cpA,
							    struct bim_UInt32Image* ptrA, 
							    const uint16* memPtrA,
 					            struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** sets all pixels to one value */
void bim_UInt32Image_setAllPixels( struct bbs_Context* cpA,
								   struct bim_UInt32Image* ptrA, 
								   uint32 valueA, 
								   int32 bbpA );
							
#endif /* bim_UINT32_IMAGE_EM_H */

