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

#ifndef bts_INT32MAT_EM_H
#define bts_INT32MAT_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Int32Arr.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bts_INT32MAT_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** square matrix */
struct bts_Int32Mat 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/* width = height of square matrix */
	uint32 widthE;

	/* array of matrix elements (data is arranged by rows) */
	struct bbs_Int32Arr arrE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes matrix */
void bts_Int32Mat_init( struct bbs_Context* cpA,
					    struct bts_Int32Mat* ptrA );

/** destroys matric */
void bts_Int32Mat_exit( struct bbs_Context* cpA,
					    struct bts_Int32Mat* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/* copies matrices */
void bts_Int32Mat_copy( struct bbs_Context* cpA,
					    struct bts_Int32Mat* ptrA, 
						const struct bts_Int32Mat* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates square matrix */
void bts_Int32Mat_create( struct bbs_Context* cpA,
						  struct bts_Int32Mat* ptrA, 
						  int32 widthA,
				          struct bbs_MemSeg* mspA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_Int32Mat_memSize( struct bbs_Context* cpA,
							 const struct bts_Int32Mat* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_Int32Mat_memWrite( struct bbs_Context* cpA,
							  const struct bts_Int32Mat* ptrA, 
							  uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_Int32Mat_memRead( struct bbs_Context* cpA,
							 struct bts_Int32Mat* ptrA, 
							 const uint16* memPtrA,
				             struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** computes the solution to  matrix * outVecA = inVecA,  returns false if the
 *	function was not successful;
 *	internally calls solve2() which is overflow safe;
 *	use large bbpA if you need high accuracy;
 *
 *	matA: the square matrix, array of size ( matWidthA * matWidthA )
 *	matWidthA: width of the matrix
 *  inVecA:  array of size matWidthA
 *  outVecA: array of size matWidthA
 *	bbpA: bbp for all matrices and vectors
 *	tmpMatA: matrix of same size as matA
 *  tmpVecA: array of size matWidthA
 */
flag bts_Int32Mat_solve( struct bbs_Context* cpA,
						 const int32* matA,
						 int32 matWidthA,
						 const int32* inVecA,
						 int32* outVecA,
						 int32 bbpA,
						 int32* tmpMatA,
						 int32* tmpVecA );

/**	same as _solve(), but matA gets overwritten, and tmpMatA is not needed:
 *	saves memory when matA is large;
 *	overflow safe;
 *	use large bbpA if you need high accuracy;
 */
flag bts_Int32Mat_solve2( struct bbs_Context* cpA,
						  int32* matA,
						  int32 matWidthA,
						  const int32* inVecA,
						  int32* outVecA,
						  int32 bbpA,
						  int32* tmpVecA );

#endif /* bts_INT32MAT_EM_H */

