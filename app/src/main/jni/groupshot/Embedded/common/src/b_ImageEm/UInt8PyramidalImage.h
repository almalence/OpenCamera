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

#ifndef bim_UINT8_PYRAMIDAL_IMAGE_EM_H
#define bim_UINT8_PYRAMIDAL_IMAGE_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_ImageEm/UInt8Image.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

#define bim_PYRAMIDAL_IMAGE_STANDARD_DEPTH 4

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bim_UINT8_PYRAMIDAL_IMAGE_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** Pyramidal image of uint8.
 *  The Pyramidal format is as follows
 *  widthE  specifies width of first image (image 0)
 *  heightE specifies height of first image (image 0)
 *  depthE  specifies the number of levels present
 *  image n has half of the width,heigth dimension of image n-1
 *  A pixel of in image n is the average of the corresponding 4 
 *  covering pixels in image n-1
 *  Adresses of data relative to arrE.arrPtrE
 *  The address of image 0 is 0
 *  The address of image 1 is widthE * heightE
 *  The address of image n is widthE * heightE + widthE * heightE / 4 + ... + widthE * heightE * ( 2^-(2*n) )
 *  Use function uint8* bim_UInt8PyramidalImage_arrPtr( uint32 levelA ) to obtain adress of image at given depth level
 *  Use function bim_UInt8PyramidalImage_importUInt8 to create a pyramidal image from an uint8 image
*/
struct bim_UInt8PyramidalImage 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** width of image */
	uint32 widthE;

	/** height of image */
	uint32 heightE;

	/** depth of image (number of layers) */
	uint32 depthE;

	/** pyramidal image type (temporary: until switch to 16-bit complete) */
	uint32 typeE;

	/** array of bytes */
	struct bbs_UInt8Arr arrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bim_UInt8PyramidalImage  */
void bim_UInt8PyramidalImage_init( struct bbs_Context* cpA,
								   struct bim_UInt8PyramidalImage* ptrA );

/** allocates memory for bim_UInt8PyramidalImage  */
void bim_UInt8PyramidalImage_create( struct bbs_Context* cpA,
									 struct bim_UInt8PyramidalImage* ptrA, 
									 uint32 widthA, uint32 heightA, 
									 uint32 depthA,
 									 struct bbs_MemSeg* mspA );

/** frees bim_UInt8PyramidalImage  */
void bim_UInt8PyramidalImage_exit( struct bbs_Context* cpA,
								   struct bim_UInt8PyramidalImage* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bim_UInt8PyramidalImage_copy( struct bbs_Context* cpA,
								   struct bim_UInt8PyramidalImage* ptrA, 
								   const struct bim_UInt8PyramidalImage* srcPtrA );

/** equal operator */
flag bim_UInt8PyramidalImage_equal( struct bbs_Context* cpA,
								    const struct bim_UInt8PyramidalImage* ptrA, const struct bim_UInt8PyramidalImage* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** returns adress of image at given depth level */
uint8* bim_UInt8PyramidalImage_arrPtr( struct bbs_Context* cpA,
									   const struct bim_UInt8PyramidalImage* ptrA, 
									   uint32 levelA );

/** calculates the amount of heap memory needed (16bit words) if created with given parameters */ 
uint32 bim_UInt8PyramidalImage_heapSize( struct bbs_Context* cpA,
										 const struct bim_UInt8PyramidalImage* ptrA, 
										 uint32 widthA, 
										 uint32 heightA, 
										 uint32 depthA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** sets image size */
void bim_UInt8PyramidalImage_size( struct bbs_Context* cpA,
								   struct bim_UInt8PyramidalImage* ptrA, 
								   uint32 widthA, 
								   uint32 heightA, 
								   uint32 depthA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bim_UInt8PyramidalImage_memSize( struct bbs_Context* cpA,
									    const struct bim_UInt8PyramidalImage* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bim_UInt8PyramidalImage_memWrite( struct bbs_Context* cpA,
										 const struct bim_UInt8PyramidalImage* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bim_UInt8PyramidalImage_memRead( struct bbs_Context* cpA,
									    struct bim_UInt8PyramidalImage* ptrA, 
									    const uint16* memPtrA,
 									    struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** create overlay bim_UInt8Image (does not own memory) */
void bim_UInt8PyramidalImage_overlayUInt8( struct bbs_Context* cpA,
										   const struct bim_UInt8PyramidalImage* ptrA,  
										   struct bim_UInt8Image* uint8ImageA );

/** recompute pyramidal format with given depth from data in layer 0 */
void bim_UInt8PyramidalImage_recompute( struct bbs_Context* cpA,
									    struct bim_UInt8PyramidalImage* dstPtrA );

/** import uint8image and creates pyramidal format with given depth */
void bim_UInt8PyramidalImage_importUInt8( struct bbs_Context* cpA,
										  struct bim_UInt8PyramidalImage* dstPtrA, 
									      const struct bim_UInt8Image* srcPtrA,
										  uint32 depthA );

#endif /* bim_UINT8_PYRAMIDAL_IMAGE_EM_H */

