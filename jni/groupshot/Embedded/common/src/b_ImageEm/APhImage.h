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

#ifndef bim_APH_IMAGE_EM_H
#define bim_APH_IMAGE_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/APhArr.h"
#include "b_TensorEm/Int16Rect.h"
#include "b_TensorEm/Flt16Alt2D.h"

/* ---- related objects  --------------------------------------------------- */

struct bim_ComplexImage;

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bim_APH_IMAGE_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** image of complex values (abs-phase format) */
struct bim_APhImage 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** width of image */
	uint32 widthE;

	/** height of image */
	uint32 heightE;

	/** array of bytes */
	struct bbs_APhArr arrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bim_APhImage  */
void bim_APhImage_init( struct bbs_Context* cpA,
					    struct bim_APhImage* ptrA );

/** creates bim_APhImage object */
void bim_APhImage_create( struct bbs_Context* cpA,
						  struct bim_APhImage* ptrA, 
						  uint32 widthA, 
						  uint32 heightA,
						  struct bbs_MemSeg* mspA );

/** frees bim_APhImage  */
void bim_APhImage_exit( struct bbs_Context* cpA,
					    struct bim_APhImage* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bim_APhImage_copy( struct bbs_Context* cpA,
					    struct bim_APhImage* ptrA, 
						const struct bim_APhImage* srcPtrA );

/** equal operator */
flag bim_APhImage_equal( struct bbs_Context* cpA,
						 const struct bim_APhImage* ptrA, 
						 const struct bim_APhImage* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** sets image size */
void bim_APhImage_size( struct bbs_Context* cpA,
					    struct bim_APhImage* ptrA, 
						uint32 widthA, 
						uint32 heightA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) needs when written to memory */
uint32 bim_APhImage_memSize( struct bbs_Context* cpA,
							 const struct bim_APhImage* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bim_APhImage_memWrite( struct bbs_Context* cpA,
							  const struct bim_APhImage* ptrA, 
							  uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bim_APhImage_memRead( struct bbs_Context* cpA,
							 struct bim_APhImage* ptrA, 
							 const uint16* memPtrA,
						     struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** sets all pixels to one value */
void bim_APhImage_setAllPixels( struct bbs_Context* cpA,
							    struct bim_APhImage* ptrA, 
								struct bbs_APh valueA );
							
/** copies a section of given image */
void bim_APhImage_copySection( struct bbs_Context* cpA,
							   struct bim_APhImage* ptrA, 
								 const struct bim_APhImage* srcPtrA, 
								 const struct bts_Int16Rect* sectionPtrA );

/** import complex image */
void bim_APhImage_importComplex( struct bbs_Context* cpA,
								 struct bim_APhImage* dstPtrA, 
							 const struct bim_ComplexImage* srcPtrA );

#endif /* bim_APH_IMAGE_EM_H */

