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

#ifndef bbs_MEM_SEG_EM_H
#define bbs_MEM_SEG_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"
#include "b_BasicEm/DynMemManager.h"

/* ---- related objects  --------------------------------------------------- */

struct bbs_Context;

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* overhead memory needed for each memory block allocated (exclusive memory only) */
#define bbs_MEM_BLOCK_OVERHD 2

/* Segment IDs */
#define bbs_SEG_DEFAULT			0

#if defined( HW_TMS320C5x ) || defined( HW_MeP ) || defined( bbs_MEP_MEM_CONFIG )
	#define bbs_SEG_DA			1
	#define bbs_SEG_DA_ALT		2
	#define bbs_SEG_SA			3
	#define bbs_SEG_SA_ALT		4
	#define bbs_SEG_EXT			5
	#define bbs_SEG_EXT_ALT		6
#elif defined ( bbs_KD_MEM_CONFIG ) || defined ( HW_KD_EASYSHARE )
/* on-chip optimization for Kodak Easyshare project */
	#define bbs_SEG_DA			1  /* = internal RAM segment */
	#define bbs_SEG_DA_ALT		0
	#define bbs_SEG_SA			0
	#define bbs_SEG_SA_ALT		0
	#define bbs_SEG_EXT			0
	#define bbs_SEG_EXT_ALT		0
#endif

/* ---- object definition -------------------------------------------------- */

/** Descriptor of a coherent memory segment available for memory management.
 *  How management works
 *  - Memory is arranged in blocks
 *  - Each block refers to a single call of function alloc()
 *  - Each block is aligned at 32bit
 *  - The size of each block is even (32bit aligned size)
 *  Uique (non-shared) segments:
 *  - Each block has a preceding 32 bit value indication its length
 *  - Function free() marks the corresponding block 'unused' and
 *    removes subsequently any unused block at the last position of allocated memory 
 *  Shared segments:
 *	- No write access to memory block by function alloc()
 *	- Function free has no effect
 *  Identifier:
 *  - Each segment contains an ID. The segment with the ID 0 is the default segment.
 */
struct bbs_MemSeg
{
	/* all member variables are considered read only. Only change them through functions */

	/** pointer to memory */
	uint16* memPtrE;

	/** size of memory segment in 16 bit units */
	uint32 sizeE;

	/** current allocation index in 16 bit units (index is always even -> 32 bit alignment enforced) */
	uint32 allocIndexE;

	/** Indicates that this isegment is to be shared among multiple objects */
	flag sharedE;

	/** ID of segment, id=0: unspecified */
	uint32 idE;

	/** pointer to external memory manager */
	struct bbs_DynMemManager* dynMemManagerPtrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbs_MemSeg  */
void bbs_MemSeg_init( struct bbs_Context* cpA, 
					  struct bbs_MemSeg* ptrA );

/** resets bbs_MemSeg  */
void bbs_MemSeg_exit( struct bbs_Context* cpA, 
					  struct bbs_MemSeg* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/** returns available 16bit units of memeory in given segment; (allocation is always 32 bit aligned) */
uint32 bbs_MemSeg_availableSize( struct bbs_Context* cpA, 
								 const struct bbs_MemSeg* ptrA );

/** returns currently allocated size in 16bit units of memeory in given segment */
uint32 bbs_MemSeg_allocatedSize( struct bbs_Context* cpA, 
								 const struct bbs_MemSeg* ptrA );

/** returns effectively used memory amount allocated size - unused blocks - overhead */
uint32 bbs_MemSeg_usedSize( struct bbs_Context* cpA, 
						    const struct bbs_MemSeg* ptrA );

/** counts amount of memory blocks allocated */
uint32 bbs_MemSeg_blocks( struct bbs_Context* cpA, 
						  const struct bbs_MemSeg* ptrA );

/** counts amount of memory blocks currently used */
uint32 bbs_MemSeg_usedBlocks( struct bbs_Context* cpA, 
							  const struct bbs_MemSeg* ptrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** creation of a exclusive memory segment; memPtrA must be 32-bit aligned */
struct bbs_MemSeg bbs_MemSeg_create( struct bbs_Context* cpA, 
									 void* memPtrA, 
									 uint32 sizeA );

/** creation of a shared memory segment; memPtrA must be 32-bit aligned */
struct bbs_MemSeg bbs_MemSeg_createShared( struct bbs_Context* cpA, 
										   void* memPtrA, 
										   uint32 sizeA );

/** allocation of memory (very fast); sizeA specifies number of 16bit units; (allocation is always 32 bit aligned) */
void* bbs_MemSeg_alloc( struct bbs_Context* cpA, 
					    struct bbs_MemSeg* ptrA, 
						uint32 sizeA );

/** Frees allocated memory
 *  If segment is shared, ptrA == NULL or memPtrA == NULL, nothing happens
 */
void bbs_MemSeg_free( struct bbs_Context* cpA, 
					  struct bbs_MemSeg* ptrA, 
					  void* memPtrA );

/** checks consistency of memory */
void bbs_MemSeg_checkConsistency( struct bbs_Context* cpA, 
								  const struct bbs_MemSeg* ptrA );

#endif /* bbs_MEM_SEG_EM_H */

