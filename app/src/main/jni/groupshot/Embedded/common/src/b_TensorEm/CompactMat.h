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

#ifndef bts_COMPACT_MAT_EM_H
#define bts_COMPACT_MAT_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Int16Arr.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bts_COMPACT_MAT_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** Compact Matrix 
 *  This object represents a general nxm Matrix that stores its values in  
 *  bit-packs of fixed size. Rows are encoded individually to yield 
 *  maximum accuracy for the given number of bits. The matrix takes sparseness
 *  into account.
 *
 *  Use this object for memory efficient storage of large matrices.
 */
struct bts_CompactMat 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/* width (columns) of matrix */
	uint32 widthE;

	/* height (rows) of matrix */
	uint32 heightE;

	/* bits per value */
	uint32 bitsPerValueE;

	/* 16 bit words per row including row-header (always even) */
	uint32 wordsPerRowE;

	/* maximum of ( 16 + factorExp + normBits ) for all rows (this value can be negative!) */
	int32 maxRowBitsE;

	/** Composite data array 
	 *  Encoding per row: 
	 *  (int16) 'offs' offset of row-vector (0 when row is not sparse)
	 *  (int16) 'size' effective size of row vector (= widthE when row is not sparse)
	 *  (int16) 'factorMan' mantisse of factor
	 *  (int16) 'factorExp' exponent of factor
	 *  (int16) 'normBits' norm bits of row vector
	 *  (int16), (int16), ... packed data
	 *  Each row has the effective length of 'wordsPerRowE'
	 *  wordsPerRowE is always even -> rows are 32bit-aligned
	 */
	struct bbs_Int16Arr cpsArrE;

	/** temorary array used for exponents */
	struct bbs_Int16Arr expArrE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes object */
void bts_CompactMat_init( struct bbs_Context* cpA,
					      struct bts_CompactMat* ptrA );

/** destroys object */
void bts_CompactMat_exit( struct bbs_Context* cpA,
					      struct bts_CompactMat* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/* copies matrix */
void bts_CompactMat_copy( struct bbs_Context* cpA,
					      struct bts_CompactMat* ptrA, 
						  const struct bts_CompactMat* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates matrix */
void bts_CompactMat_create( struct bbs_Context* cpA,
						    struct bts_CompactMat* ptrA, 
						    uint32 widthA,
						    uint32 heightA,
						    uint32 bitsA,
							uint32 maxRowSizeA,
				            struct bbs_MemSeg* mspA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_CompactMat_memSize( struct bbs_Context* cpA,
							   const struct bts_CompactMat* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_CompactMat_memWrite( struct bbs_Context* cpA,
							    const struct bts_CompactMat* ptrA, 
							    uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_CompactMat_memRead( struct bbs_Context* cpA,
							   struct bts_CompactMat* ptrA, 
							   const uint16* memPtrA,
				               struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** Maps vector inVec to outVec (overflow-safe) 
 *  Memory areas of vectors may not overlap
 *  Function executes reasonably fast with maximum possible accuracy
 *  outExpPtrA - exponent to output vector values
 */
void bts_CompactMat_map( struct bbs_Context* cpA, 
						 const struct bts_CompactMat* ptrA, 
						 const int16* inVecA,
						 int16* outVecA,
						 int16* outExpPtrA );

#endif /* bts_COMPACT_MAT_EM_H */

