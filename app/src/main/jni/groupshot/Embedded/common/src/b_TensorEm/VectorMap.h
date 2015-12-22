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

#ifndef bts_VECTOR_MAP_EM_H
#define bts_VECTOR_MAP_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/MemTbl.h"
#include "b_TensorEm/Flt16Vec.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/** Object Type */
enum bts_VectorMapType
{
	bts_VM_UNDEFINED = 0,
	bts_VM_MAP_SEQUENCE,   /* sequence of vector maps */
	bts_VM_NORMALIZER,     /* normalizes a vector using euclidean norm */
	bts_VM_MAT,            /* linear transformation (matrix) */
	bts_VM_ALT,		       /* affine linear transformation */
	bts_VM_SUB_VEC_MAP     /* sub vector extraction */
};

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** base object for vector maps (occurs as first element in all vector map objects) */
struct bts_VectorMap 
{
	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** vector map type */
	uint32 typeE;

	/* ---- virtual functions ---------------------------------------------- */

	/** vector map operation.
	 *  Maps vector inVec to outVec (overflow-safe) 
	 *  Memory areas of vectors may not overlap
	 */
	void ( *vpMapE )( struct bbs_Context* cpA, 
					  const struct bts_VectorMap* ptrA, 
					  const struct bts_Flt16Vec* inVecPtrA,
					  struct bts_Flt16Vec* outVecPtrA ); 

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bts_VectorMap  */
void bts_VectorMap_init( struct bbs_Context* cpA,
					     struct bts_VectorMap* ptrA );

/** resets bts_VectorMap  */
void bts_VectorMap_exit( struct bbs_Context* cpA,
					     struct bts_VectorMap* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bts_VectorMap_copy( struct bbs_Context* cpA,
					     struct bts_VectorMap* ptrA, 
					     const struct bts_VectorMap* srcPtrA );

/** equal operator */
flag bts_VectorMap_equal( struct bbs_Context* cpA,
						  const struct bts_VectorMap* ptrA, 
						  const struct bts_VectorMap* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bts_VectorMap_memSize( struct bbs_Context* cpA,
						      const struct bts_VectorMap* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bts_VectorMap_memWrite( struct bbs_Context* cpA,
							   const struct bts_VectorMap* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bts_VectorMap_memRead( struct bbs_Context* cpA,
							  struct bts_VectorMap* ptrA, const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** virtual init function  */
void bts_vectorMapInit( struct bbs_Context* cpA,
					    struct bts_VectorMap* ptrA,
					    enum bts_VectorMapType typeA );

/** virtual exit function */
void bts_vectorMapExit( struct bbs_Context* cpA, 
					    struct bts_VectorMap* ptrA );

/** virtual mem size function */
uint32 bts_vectorMapMemSize( struct bbs_Context* cpA, 
						     const struct bts_VectorMap* ptrA );

/** virtual mem write function */
uint32 bts_vectorMapMemWrite( struct bbs_Context* cpA, 
						      const struct bts_VectorMap* ptrA, uint16* memPtrA );

/** virtual mem read function */
uint32 bts_vectorMapMemRead( struct bbs_Context* cpA,
						     struct bts_VectorMap* ptrA, 
						     const uint16* memPtrA,
						     struct bbs_MemTbl* mtpA );

/** virtual sizeof operator for 16bit units */
uint32 bts_vectorMapSizeOf16( struct bbs_Context* cpA, enum bts_VectorMapType typeA );

#endif /* bts_VECTOR_MAP_EM_H */

