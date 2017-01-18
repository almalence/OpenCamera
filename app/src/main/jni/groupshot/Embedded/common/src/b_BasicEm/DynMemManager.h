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

#ifndef bbs_DYN_MEM_MANAGER_EM_H
#define bbs_DYN_MEM_MANAGER_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"

/* ---- related objects  --------------------------------------------------- */

struct bbs_Context;
struct bbs_MemSeg;

/* ---- typedefs ----------------------------------------------------------- */

/** 'malloc' function pointer. 
  * Allocated memory block must be 32-bit-aligned.
  * sizeA refers to the size of a memory block in bytes   
  */
typedef void* ( *bbs_mallocFPtr )( struct bbs_Context* cpA, 
								   const struct bbs_MemSeg* memSegPtrA, 
								   uint32 sizeA );

/** free function pointer */
typedef void ( *bbs_freeFPtr )( void* memPtrA );

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** Dynamic memory manager.
  * Handles allocation and deallocation of memory blocks via function pointers
  * to malloc and free.
  *
  * Each memory block is organized as follows:
  * - The first 8 bytes are reserved for the pointer to the next 
  *    memory block (8 to allow support of 64-bit platforms).
  * - Next a 32-bit value stores the allocated memory size in 16-bit units.
  * - Finally the actual allocated memory area. 
  * This means for each new memory block an additional 12 bytes are allocated.
  */
struct bbs_DynMemManager 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** pointer to first memory block */ 
	uint16* memPtrE;

	/** function pointer to external mem alloc function (s. comment of type declaration)*/
	bbs_mallocFPtr mallocFPtrE;

	/** function pointer to external mem free function */
	bbs_freeFPtr freeFPtrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbs_DynMemManager  */
void bbs_DynMemManager_init( struct bbs_Context* cpA, struct bbs_DynMemManager* ptrA );

/** frees bbs_DynMemManager  */
void bbs_DynMemManager_exit( struct bbs_Context* cpA, struct bbs_DynMemManager* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/** returns size of currently allocated memory in 16bit units */
uint32 bbs_DynMemManager_allocatedSize( struct bbs_Context* cpA, const struct bbs_DynMemManager* ptrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** allocates sizeA words of memory */
uint16* bbs_DynMemManager_alloc( struct bbs_Context* cpA, 
								 struct bbs_DynMemManager* ptrA, 
								 const struct bbs_MemSeg* memSegPtrA,
								 uint32 sizeA );

/** frees previously allocated memory */
void bbs_DynMemManager_free( struct bbs_Context* cpA, 
							 struct bbs_DynMemManager* ptrA, 
							 uint16* memPtrA );

/** returns the next memory block of at least minSizeA length; allocates new block if neccessary */
uint16* bbs_DynMemManager_nextBlock( struct bbs_Context* cpA, 
									 struct bbs_DynMemManager* ptrA, 
									 const struct bbs_MemSeg* memSegPtrA,
									 uint16* curBlockPtrA, 
									 uint32 minSizeA, 
									 uint32* actualSizePtrA );

/** frees all allocated memory */
void bbs_DynMemManager_freeAll( struct bbs_Context* cpA, 
							    struct bbs_DynMemManager* ptrA );


#endif /* bbs_DYN_MEM_MANAGER_EM_H */

