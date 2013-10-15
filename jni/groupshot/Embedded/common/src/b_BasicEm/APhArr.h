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

#ifndef bbs_APH_ARR_EM_H
#define bbs_APH_ARR_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/MemSeg.h"
#include "b_BasicEm/APh.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** abs phase array */
struct bbs_APhArr 
{

	/* ---- private data --------------------------------------------------- */

	/** pointer to exclusive memory segment used for allocation */
	struct bbs_MemSeg* mspE;

	/* ---- public data ---------------------------------------------------- */

	/** pointer to array of bytes */
	struct bbs_APh* arrPtrE;

	/** current size */
	uint32 sizeE;

	/** allocated size */
	uint32 allocatedSizeE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbs_APhArr  */
void bbs_APhArr_init( struct bbs_Context* cpA,
					  struct bbs_APhArr* ptrA );

/** frees bbs_APhArr  */
void bbs_APhArr_exit( struct bbs_Context* cpA,
					  struct bbs_APhArr* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbs_APhArr_copy( struct bbs_Context* cpA,
					  struct bbs_APhArr* ptrA, 
					  const struct bbs_APhArr* srcPtrA );

/** equal operator */
flag bbs_APhArr_equal( struct bbs_Context* cpA,
					   const struct bbs_APhArr* ptrA, 
					   const struct bbs_APhArr* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** calculates the amount of heap memory needed (16bit words) if created with given parameters */ 
uint32 bbs_APhArr_heapSize( struct bbs_Context* cpA,
						    const struct bbs_APhArr* ptrA, 
							uint32 sizeA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** creates bbs_APhArr object. 
  * This function must be called after initialization before usage of 
  * object. 
  */
void bbs_APhArr_create( struct bbs_Context* cpA,
					    struct bbs_APhArr* ptrA, 
					    uint32 sizeA, 
						struct bbs_MemSeg* mspA );

/** sets array size */
void bbs_APhArr_size( struct bbs_Context* cpA,
					  struct bbs_APhArr* ptrA, 
					  uint32 sizeA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bbs_APhArr_memSize( struct bbs_Context* cpA,
						   const struct bbs_APhArr* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bbs_APhArr_memWrite( struct bbs_Context* cpA,
						    const struct bbs_APhArr* ptrA, 
							uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bbs_APhArr_memRead( struct bbs_Context* cpA,
						   struct bbs_APhArr* ptrA, 
						   const uint16* memPtrA, 
						   struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

#endif /* bbs_APH_ARR_EM_H */

