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

#ifndef bim_UINT16_IMAGE_EM_H
#define bim_UINT16_IMAGE_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/UInt16Arr.h"
#include "b_TensorEm/Int16Rect.h"
#include "b_TensorEm/Flt16Alt2D.h"
#include "b_ImageEm/UInt8Image.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bim_UINT16_IMAGE_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** Packed byte image 
 *  2 pixels are stored on a 16 bit space. 
 *  Using conventional pixel order, the first pixel is represented by the low-byte.
 *  A Pixel at position (x,y) can be accessed as follows:
 *  ( ( arrE.arrE + y * withE + ( x >> 1 ) ) >> ( 8 * ( x & 1 ) ) ) & 0x0FF;
 *
 *  On little endian platforms bim_UInt16ByteImage and bim_UInt8Image 
 *  have the same memory representation of the image data.
 */
struct bim_UInt16ByteImage 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** width of image */
	uint32 widthE;

	/** height of image */
	uint32 heightE;

	/** array of 16bit words */
	struct bbs_UInt16Arr arrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bim_UInt16ByteImage  */
void bim_UInt16ByteImage_init( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA );

/** allocates memory for bim_UInt16ByteImage */
void bim_UInt16ByteImage_create( struct bbs_Context* cpA,
								 struct bim_UInt16ByteImage* ptrA, 
						    uint32 widthA, 
							uint32 heightA,
 					        struct bbs_MemSeg* mspA );

/** destructor of bim_UInt16ByteImage  */
void bim_UInt16ByteImage_exit( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bim_UInt16ByteImage_copy( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA, 
							   const struct bim_UInt16ByteImage* srcPtrA );

/** equal operator */
flag bim_UInt16ByteImage_equal( struct bbs_Context* cpA,
							    const struct bim_UInt16ByteImage* ptrA, 
								const struct bim_UInt16ByteImage* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** checksum of image (for debugging purposes) */
uint32 bim_UInt16ByteImage_checkSum( struct bbs_Context* cpA,
									 const struct bim_UInt16ByteImage* ptrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** assigns external image to array (no allocation, deallocation or copying of data) */
void bim_UInt16ByteImage_assignExternalImage( struct bbs_Context* cpA,
											  struct bim_UInt16ByteImage* ptrA, 
											  struct bim_UInt16ByteImage* srcPtrA );

/** sets image size */
void bim_UInt16ByteImage_size( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA, 
							   uint32 widthA, uint32 heightA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bim_UInt16ByteImage_memSize( struct bbs_Context* cpA,
								    const struct bim_UInt16ByteImage* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bim_UInt16ByteImage_memWrite( struct bbs_Context* cpA,
									 const struct bim_UInt16ByteImage* ptrA, 
									 uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bim_UInt16ByteImage_memRead( struct bbs_Context* cpA,
								    struct bim_UInt16ByteImage* ptrA, 
							   const uint16* memPtrA,
 					           struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** sets all pixels to one value; higher 8 bits of valueA are ignored */
void bim_UInt16ByteImage_setAllPixels( struct bbs_Context* cpA,
									   struct bim_UInt16ByteImage* ptrA, 
									   uint16 valueA );
							
/** applies affine linear warping to pixels positions of imageA before copying the into *ptrA */
void bim_UInt16ByteImage_warp( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA, 
							   const struct bim_UInt16ByteImage* srcPtrA, 
						       const struct bts_Flt16Alt2D* altPtrA,
			                   int32 resultWidthA,
			                   int32 resultHeightA );


#ifndef HW_TMS320C5x /* 16bit architecture excluded */

/** applies affine linear warping to pixels positions of ptrA before copying the into *ptrA.
 *  This function accepts an bim_UInt16ByteImage as input, but uses a faster algorithm  
 *  utilizing 8-bit data access for warping.
 *  Only available for platforms that allow 8 bit data access.
 */
void bim_UInt16ByteImage_warp8( struct bbs_Context* cpA,
							    struct bim_UInt16ByteImage* ptrA, 
							    const struct bim_UInt16ByteImage* srcPtrA, 
							    const struct bts_Flt16Alt2D* altPtrA,
							    int32 resultWidthA,
							    int32 resultHeightA );
#endif /* HW_TMS320C5x */

#endif /* bim_UINT16_IMAGE_EM_H */

