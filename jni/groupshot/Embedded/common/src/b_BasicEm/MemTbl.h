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

#ifndef bbs_MEM_TBL_EM_H
#define bbs_MEM_TBL_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/MemSeg.h"

/* ---- related objects  --------------------------------------------------- */

struct bbs_Context;

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* maximum number of exclusive and shared memory segments used, increase this number if needed */
#define bbs_MAX_MEM_SEGS 4 

/* ---- object definition -------------------------------------------------- */

/** Descriptor of a set of memory segments
 *  The first segment in each array (exclusive and shared) with a size > 0 is
 *  the default segment.
 */
struct bbs_MemTbl
{
	/* number of exclusive memory segments */
	uint32 esSizeE;

	/** array of exclusive memory segments (for initialisation purposes only ) */
	struct bbs_MemSeg esArrE[ bbs_MAX_MEM_SEGS ];

	/** array of pointer to exclusive memory segments */
	struct bbs_MemSeg* espArrE[ bbs_MAX_MEM_SEGS ];

	/* number of shared memory segments */
	uint32 ssSizeE;

	/** array of shared memory segments */
	struct bbs_MemSeg ssArrE[ bbs_MAX_MEM_SEGS ];
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbs_MemTbl  */
void bbs_MemTbl_init( struct bbs_Context* cpA,
					  struct bbs_MemTbl* ptrA );

/** resets bbs_MemTbl  */
void bbs_MemTbl_exit( struct bbs_Context* cpA,
					  struct bbs_MemTbl* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/* indicates whether memory segment overalps with any segment in memory table */
flag bbs_MemTbl_overlap( struct bbs_Context* cpA,
						 struct bbs_MemTbl* ptrA, 
						 const void* memPtrA, uint32 sizeA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** creates a memory table with one exclusive and one shared segment from a coherent memory block */
void bbs_MemTbl_create( struct bbs_Context* cpA,
					    struct bbs_MemTbl* ptrA, 
						void* memPtrA, 
						uint32 sizeA, 
						uint32 sharedSubSizeA );

/** adds new exclusive segment to table ( default segment must be added first ) */
void bbs_MemTbl_add( struct bbs_Context* cpA,
					 struct bbs_MemTbl* ptrA, 
					 void* memPtrA, 
					 uint32 sizeA, 
					 uint32 idA );

/** adds new shared segment to table ( default segment must be added first )  */
void bbs_MemTbl_addShared( struct bbs_Context* cpA,
						   struct bbs_MemTbl* ptrA, 
						   void* memPtrA, 
						   uint32 sizeA, 
						   uint32 idA );

/** returns specified segment. If specified segment is not found the default segment is returned */
struct bbs_MemSeg* bbs_MemTbl_segPtr( struct bbs_Context* cpA,
									  struct bbs_MemTbl* ptrA, 
									  uint32 idA );

struct bbs_MemSeg* bbs_MemTbl_sharedSegPtr( struct bbs_Context* cpA,
										    struct bbs_MemTbl* ptrA, 
											uint32 idA );

/* Search functions below are obsolete. Please use bbs_MemTbl_segPtr or bbs_MemTbl_sharedSegPtr instead. */

/** returns pointer to fastest exclusive segment that has at least minSizeA words available */
struct bbs_MemSeg* bbs_MemTbl_fastestSegPtr( struct bbs_Context* cpA,
											 struct bbs_MemTbl* ptrA, 
											 uint32 minSizeA );

/** returns pointer to exclusive segment that has most words available */
struct bbs_MemSeg* bbs_MemTbl_largestSegPtr( struct bbs_Context* cpA,
											 struct bbs_MemTbl* ptrA );

/** returns fastest shared segment that has at least minSizeA words available */
struct bbs_MemSeg* bbs_MemTbl_fastestSharedSegPtr( struct bbs_Context* cpA,
												   struct bbs_MemTbl* ptrA, 
												   uint32 minSizeA );

/** returns shared segment that has most words available */
struct bbs_MemSeg* bbs_MemTbl_largestSharedSegPtr( struct bbs_Context* cpA,
												   struct bbs_MemTbl* ptrA );

#endif /* bbs_MEM_TBL_EM_H */

