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

#ifndef bts_FLT_16_VEC_EM_H
#define bts_FLT_16_VEC_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/MemSeg.h"
#include "b_BasicEm/Int16Arr.h"
#include "b_TensorEm/Functions.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** 
 * Vector with 16 bit components 
 * The vector operations are implemented with respect to maintain high accuracy and 
 * overflow safety for all possible vector configurations.
 */
struct bts_Flt16Vec 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** array of vector elements */
	struct bbs_Int16Arr arrE;

	/** exponent to elements */
	int16 expE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes vector */
void bts_Flt16Vec_init( struct bbs_Context* cpA,
						struct bts_Flt16Vec* ptrA );

/** destroys vector */
void bts_Flt16Vec_exit( struct bbs_Context* cpA,
						struct bts_Flt16Vec* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copies vector */
void bts_Flt16Vec_copy( struct bbs_Context* cpA,
						struct bts_Flt16Vec* ptrA, 
						const struct bts_Flt16Vec* srcPtrA );

/** compares vector */
flag bts_Flt16Vec_equal( struct bbs_Context* cpA,
						 const struct bts_Flt16Vec* ptrA, 
						 const struct bts_Flt16Vec* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** returns average of vector without exponent */
int16 bts_Flt16Vec_avg( struct bbs_Context* cpA, const struct bts_Flt16Vec* ptrA );

/** returns norm of vector without exponent */
uint32 bts_Flt16Vec_norm( struct bbs_Context* cpA, const struct bts_Flt16Vec* ptrA );

/** returns maximum absulute value without exponent */
uint16 bts_Flt16Vec_maxAbs( struct bbs_Context* cpA, const struct bts_Flt16Vec* ptrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates vector */
void bts_Flt16Vec_create( struct bbs_Context* cpA,
						  struct bts_Flt16Vec* ptrA, 
						  uint32 sizeA,
						  struct bbs_MemSeg* mspA );

/** resize vector (sizeA must be smaller or equal to allocated size)*/
void bts_Flt16Vec_size( struct bbs_Context* cpA,
						struct bts_Flt16Vec* ptrA, 
						uint32 sizeA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size in words (16-bit) object needs when written to memory */
uint32 bts_Flt16Vec_memSize( struct bbs_Context* cpA,
							 const struct bts_Flt16Vec* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bts_Flt16Vec_memWrite( struct bbs_Context* cpA,
							  const struct bts_Flt16Vec* ptrA, 
							  uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bts_Flt16Vec_memRead( struct bbs_Context* cpA,
							 struct bts_Flt16Vec* ptrA, 
							 const uint16* memPtrA,
						     struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** maximize mantisse values to reduce error propagation through multiple vector operations */
void bts_Flt16Vec_maximizeMantisse( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA );

/** scales vector such that max abs is as near as possible to 0x7FFF; returns scale factor used in format 16.16; returns 0 when vector is 0 */
uint32 bts_Flt16Vec_maximizeAbsValue( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA );

/** tranlates vector to zero average */
void bts_Flt16Vec_zeroAverage( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA );

/** normalizes vector (euclidean norm) */
void bts_Flt16Vec_normalize( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA );

/** sets vector to zero */
void bts_Flt16Vec_setZero( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA );

/** multiplies a scalar to vector */
void bts_Flt16Vec_mul( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA, int32 valA, int16 expA );

/** computes dot product; returns product as mantisse + exponent */
void bts_Flt16Vec_dotPtrd( struct bbs_Context* cpA, struct bts_Flt16Vec* vp1A, struct bts_Flt16Vec* vp2A, int32* manPtrA, int32* expPtrA );

/** appends a vector */
void bts_Flt16Vec_append( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA, struct bts_Flt16Vec* srcPtrA );

#endif /* bts_FLT_16_VEC_EM_H */

