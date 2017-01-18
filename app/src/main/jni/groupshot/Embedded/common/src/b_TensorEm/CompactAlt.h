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

#ifndef bts_COMPACT_ALT_EM_H
#define bts_COMPACT_ALT_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_TensorEm/CompactMat.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bts_COMPACT_ALT_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** Compact Affine linear trasformation composed of compact matrix and 
 *  translation vector (not compressed)
 *
 *  Use this object for memory efficient storage of large matrices.
 */
struct bts_CompactAlt 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** compact matrix */
	struct bts_CompactMat matE;

	/** translation vector (size = 0 when no translation) */
	struct bbs_Int16Arr vecE;

	/** exponent of translation vector */
	int32 vecExpE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes object */
void bts_CompactAlt_init( struct bbs_Context* cpA,
					      struct bts_CompactAlt* ptrA );

/** destroys object */
void bts_CompactAlt_exit( struct bbs_Context* cpA,
					      struct bts_CompactAlt* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/* copies alt */
void bts_CompactAlt_copy( struct bbs_Context* cpA,
					      struct bts_CompactAlt* ptrA, 
						  const struct bts_CompactAlt* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates alt */
void bts_CompactAlt_create( struct bbs_Context* cpA,
						    struct bts_CompactAlt* ptrA, 
						    uint32 widthA,
						    uint32 heightA,
						    uint32 bitsA,
							uint32 maxRowSizeA,
				            struct bbs_MemSeg* mspA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_CompactAlt_memSize( struct bbs_Context* cpA,
							   const struct bts_CompactAlt* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_CompactAlt_memWrite( struct bbs_Context* cpA,
							    const struct bts_CompactAlt* ptrA, 
							    uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_CompactAlt_memRead( struct bbs_Context* cpA,
							   struct bts_CompactAlt* ptrA, 
							   const uint16* memPtrA,
				               struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** Maps vector inVec to outVec (overflow-safe) 
 *  Memory areas of vectors may not overlap
 *  Function executes reasonably fast with maximum possible accuracy
 *  inExpA - input exponent
 *  outExpPtrA - exponent to output vector values
 */
void bts_CompactAlt_map( struct bbs_Context* cpA, 
						 const struct bts_CompactAlt* ptrA, 
						 const int16* inVecA,
						 int16  inExpA,
						 int16* outVecA,
						 int16* outExpPtrA );

#endif /* bts_COMPACT_ALT_EM_H */

