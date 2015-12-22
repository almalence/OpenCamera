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

#ifndef bim_FLT16_IMAGE_EM_H
#define bim_FLT16_IMAGE_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Int16Arr.h"
#include "b_TensorEm/Int16Rect.h"
#include "b_TensorEm/Flt16Alt2D.h"

/* ---- related objects  --------------------------------------------------- */

struct bim_ComplexImage;

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bim_FLT16_IMAGE_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** image of int16 with floating point */
struct bim_Flt16Image 
{

	/* ---- private data --------------------------------------------------- */

	/** allocated array of bytes */
	struct bbs_Int16Arr allocArrE;

	/* ---- public data ---------------------------------------------------- */

	/** width of image */
	uint32 widthE;

	/** height of image */
	uint32 heightE;

	/** point position */
	int32 bbpE;

	/** array of bytes */
	struct bbs_Int16Arr arrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bim_Flt16Image  */
void bim_Flt16Image_init( struct bbs_Context* cpA,
						  struct bim_Flt16Image* ptrA );

/** destroys bim_Flt16Image  */
void bim_Flt16Image_exit( struct bbs_Context* cpA,
						  struct bim_Flt16Image* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bim_Flt16Image_copy( struct bbs_Context* cpA,
						  struct bim_Flt16Image* ptrA, 
						  const struct bim_Flt16Image* srcPtrA );

/** equal operator */
flag bim_Flt16Image_equal( struct bbs_Context* cpA,
						   const struct bim_Flt16Image* ptrA, 
						   const struct bim_Flt16Image* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates memory for bim_Flt16Image */
void bim_Flt16Image_create( struct bbs_Context* cpA,
						    struct bim_Flt16Image* ptrA, 
						    uint32 widthA, 
							uint32 heightA,
 					        struct bbs_MemSeg* mspA );

/** assigns external image to array (no allocation, deallocation or copying of data) */
/*void bim_Flt16Image_assignExternalImage( struct bbs_Context* cpA,
										 struct bim_Flt16Image* ptrA, 
										 struct bim_Flt16Image* srcPtrA );
*/
/** sets image size */
void bim_Flt16Image_size( struct bbs_Context* cpA,
						  struct bim_Flt16Image* ptrA, 
						  uint32 widthA, 
						  uint32 heightA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) needs when written to memory */
uint32 bim_Flt16Image_memSize( struct bbs_Context* cpA,
							   const struct bim_Flt16Image* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bim_Flt16Image_memWrite( struct bbs_Context* cpA,
							    const struct bim_Flt16Image* ptrA, 
							    uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bim_Flt16Image_memRead( struct bbs_Context* cpA,
							   struct bim_Flt16Image* ptrA, 
							   const uint16* memPtrA,
 					           struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** sets all pixels to one value */
void bim_Flt16Image_setAllPixels( struct bbs_Context* cpA,
								  struct bim_Flt16Image* ptrA, 
								  int16 valueA, int32 bbpA );
							
/** copies a section of given image */
void bim_Flt16Image_copySection( struct bbs_Context* cpA,
								 struct bim_Flt16Image* ptrA, 
								 const struct bim_Flt16Image* srcPtrA, 
								 const struct bts_Int16Rect* sectionPtrA );

/** imports real values from complex image */
void bim_Flt16Image_importReal( struct bbs_Context* cpA,
							    struct bim_Flt16Image* dstPtrA, 
						        const struct bim_ComplexImage* srcPtrA );

/** imports imaginary values from complex image */
void bim_Flt16Image_importImag( struct bbs_Context* cpA,
							    struct bim_Flt16Image* dstPtrA, 
						        const struct bim_ComplexImage* srcPtrA );

/** imports magnitudes from complex image */
void bim_Flt16Image_importAbs( struct bbs_Context* cpA,
							   struct bim_Flt16Image* dstPtrA, 
						       const struct bim_ComplexImage* srcPtrA );

/** imports phases from complex image */
void bim_Flt16Image_importPhase( struct bbs_Context* cpA,
								 struct bim_Flt16Image* dstPtrA, 
						         const struct bim_ComplexImage* srcPtrA );


#endif /* bim_FLT16_IMAGE_EM_H */

