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

#ifndef bts_NORMALIZER_EM_H
#define bts_NORMALIZER_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/MemTbl.h"
#include "b_TensorEm/VectorMap.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/** data format version number */
#define bts_NORMALIZER_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** applies euclidean normalization to vector */
struct bts_Normalizer 
{
	/* ---- public data ---------------------------------------------------- */

	/** base element (must be first element) */
	struct bts_VectorMap baseE;

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bts_Normalizer  */
void bts_Normalizer_init( struct bbs_Context* cpA,
					       struct bts_Normalizer* ptrA );

/** resets bts_Normalizer  */
void bts_Normalizer_exit( struct bbs_Context* cpA,
					       struct bts_Normalizer* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bts_Normalizer_copy( struct bbs_Context* cpA,
 					       struct bts_Normalizer* ptrA, 
					       const struct bts_Normalizer* srcPtrA );

/** equal operator */
flag bts_Normalizer_equal( struct bbs_Context* cpA,
						    const struct bts_Normalizer* ptrA, 
						    const struct bts_Normalizer* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bts_Normalizer_memSize( struct bbs_Context* cpA,
						        const struct bts_Normalizer* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bts_Normalizer_memWrite( struct bbs_Context* cpA,
							     const struct bts_Normalizer* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bts_Normalizer_memRead( struct bbs_Context* cpA,
							    struct bts_Normalizer* ptrA, 
							    const uint16* memPtrA, 
							    struct bbs_MemTbl* mtpA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** Vector map operation.
 *  Maps vector inVec to outVec (overflow-safe) 
 *  Memory areas of vectors may not overlap
 */
void bts_Normalizer_map( struct bbs_Context* cpA, 
						  const struct bts_VectorMap* ptrA, 
						  const struct bts_Flt16Vec* inVecPtrA,
						  struct bts_Flt16Vec* outVecPtrA ); 

#endif /* bts_NORMALIZER_EM_H */

