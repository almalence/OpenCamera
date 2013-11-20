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

#ifndef bbs_UINT32ARR_EM_H
#define bbs_UINT32ARR_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/MemSeg.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** byte array */
struct bbs_UInt32Arr 
{

	/* ---- private data --------------------------------------------------- */

	/** pointer to exclusive memory segment used for allocation */
	struct bbs_MemSeg* mspE;

	/* ---- public data ---------------------------------------------------- */

	/** pointer to array of uint32 */
	uint32* arrPtrE;

	/** current size */
	uint32 sizeE;

	/** allocated size */
	uint32 allocatedSizeE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbs_UInt32Arr  */
void bbs_UInt32Arr_init( struct bbs_Context* cpA,
						 struct bbs_UInt32Arr* ptrA );

/** frees bbs_UInt32Arr  */
void bbs_UInt32Arr_exit( struct bbs_Context* cpA,
						 struct bbs_UInt32Arr* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbs_UInt32Arr_copy( struct bbs_Context* cpA,
						 struct bbs_UInt32Arr* ptrA, 
						 const struct bbs_UInt32Arr* srcPtrA );

/** equal operator */
flag bbs_UInt32Arr_equal( struct bbs_Context* cpA,
						  const struct bbs_UInt32Arr* ptrA, 
						  const struct bbs_UInt32Arr* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** calculates the amount of heap memory needed (16bit words) if created with given parameters */ 
uint32 bbs_UInt32Arr_heapSize( struct bbs_Context* cpA,
							   const struct bbs_UInt32Arr* ptrA, 
							   uint32 sizeA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates memory for bbs_UInt32Arr */
void bbs_UInt32Arr_create( struct bbs_Context* cpA,
						   struct bbs_UInt32Arr* ptrA, 
						  uint32 sizeA, 
						  struct bbs_MemSeg* mspA );

/** sets array size */
void bbs_UInt32Arr_size( struct bbs_Context* cpA,
						 struct bbs_UInt32Arr* ptrA, uint32 sizeA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bbs_UInt32Arr_memSize( struct bbs_Context* cpA,
							  const struct bbs_UInt32Arr* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bbs_UInt32Arr_memWrite( struct bbs_Context* cpA,
							   const struct bbs_UInt32Arr* ptrA, 
							   uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bbs_UInt32Arr_memRead( struct bbs_Context* cpA,
							  struct bbs_UInt32Arr* ptrA, 
							 const uint16* memPtrA,
							 struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** fills array with a value */
void bbs_UInt32Arr_fill( struct bbs_Context* cpA,
						 struct bbs_UInt32Arr* ptrA, 
						 uint32 valA );

#endif /* bbs_UINT32ARR_EM_H */

