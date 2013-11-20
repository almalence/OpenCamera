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

#ifndef bim_COMPLEX_IMAGE_EM_H
#define bim_COMPLEX_IMAGE_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/ComplexArr.h"
#include "b_TensorEm/Int16Rect.h"
#include "b_TensorEm/Flt16Alt2D.h"

/* ---- related objects  --------------------------------------------------- */

struct bim_APhImage;

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bim_COMPLEX_IMAGE_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** image of complex values */
struct bim_ComplexImage 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** width of image */
	uint32 widthE;

	/** height of image */
	uint32 heightE;

	/** array of bytes */
	struct bbs_ComplexArr arrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bim_ComplexImage  */
void bim_ComplexImage_init( struct bbs_Context* cpA,
						    struct bim_ComplexImage* ptrA );

/** frees bim_ComplexImage  */
void bim_ComplexImage_exit( struct bbs_Context* cpA,
						    struct bim_ComplexImage* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bim_ComplexImage_copy( struct bbs_Context* cpA,
						    struct bim_ComplexImage* ptrA, 
							const struct bim_ComplexImage* srcPtrA );

/** equal operator */
flag bim_ComplexImage_equal( struct bbs_Context* cpA,
							 const struct bim_ComplexImage* ptrA, 
							 const struct bim_ComplexImage* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** checksum of image (for debugging purposes) */
uint32 bim_ComplexImage_checkSum( struct bbs_Context* cpA,
								  const struct bim_ComplexImage* ptrA );

/** calculates the amount of heap memory needed (16bit words) if created with given parameters */ 
uint32 bim_ComplexImage_heapSize( struct bbs_Context* cpA,
								  const struct bim_ComplexImage* ptrA, 
								  uint32 widthA, uint32 heightA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates memory for bim_ComplexImage */
void bim_ComplexImage_create( struct bbs_Context* cpA,
							  struct bim_ComplexImage* ptrA, 
						    uint32 widthA, 
							uint32 heightA,
 					        struct bbs_MemSeg* mspA );
							
/** sets image size */
void bim_ComplexImage_size( struct bbs_Context* cpA,
						    struct bim_ComplexImage* ptrA, 
							uint32 widthA, 
							uint32 heightA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bim_ComplexImage_memSize( struct bbs_Context* cpA,
								 const struct bim_ComplexImage* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bim_ComplexImage_memWrite( struct bbs_Context* cpA,
								  const struct bim_ComplexImage* ptrA, 
								  uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bim_ComplexImage_memRead( struct bbs_Context* cpA,
								 struct bim_ComplexImage* ptrA, 
								 const uint16* memPtrA,
 					             struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** sets all pixels to one value */
void bim_ComplexImage_setAllPixels( struct bbs_Context* cpA,
								    struct bim_ComplexImage* ptrA, 
									struct bbs_Complex valueA );
							
/** copies a section of given image */
void bim_ComplexImage_copySection( struct bbs_Context* cpA,
								   struct bim_ComplexImage* ptrA, 
								 const struct bim_ComplexImage* srcPtrA, 
								 const struct bts_Int16Rect* sectionPtrA );

/** import abs-phase image */
void bim_ComplexImage_importAPh( struct bbs_Context* cpA,
								 struct bim_ComplexImage* dstPtrA, 
								 const struct bim_APhImage* srcPtrA );

#endif /* bim_COMPLEX_IMAGE_EM_H */

