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

#ifndef bts_FLT16MAT2D_EM_H
#define bts_FLT16MAT2D_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"
#include "b_BasicEm/Phase.h"
#include "b_TensorEm/Int16Vec2D.h"
#include "b_TensorEm/Flt16Vec2D.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** 2d matrix with floating point */
struct bts_Flt16Mat2D 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** xx component */
	int16 xxE;

	/** xy component */
	int16 xyE;

	/** yx component */
	int16 yxE;

	/** yy component */
	int16 yyE;

	/** point position */
	int16 bbpE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes matrix */
void bts_Flt16Mat2D_init( struct bts_Flt16Mat2D* ptrA );

/** destroys matrix */
void bts_Flt16Mat2D_exit( struct bts_Flt16Mat2D* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bts_Flt16Mat2D_copy( struct bts_Flt16Mat2D* ptrA, const struct bts_Flt16Mat2D* srcPtrA );

/** equal operator */
flag bts_Flt16Mat2D_equal( const struct bts_Flt16Mat2D* ptrA, const struct bts_Flt16Mat2D* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** returns determinate of matrix; return bbp is  ptrA->bbpE * 2 */
uint32 bts_Flt16Mat2D_det( const struct bts_Flt16Mat2D* ptrA );

/** inverts matrix */
void bts_Flt16Mat2D_invert( struct bts_Flt16Mat2D* ptrA );

/** returns inverted matrix */
struct bts_Flt16Mat2D bts_Flt16Mat2D_inverted( const struct bts_Flt16Mat2D* ptrA );

/** creates identity matrix */
struct bts_Flt16Mat2D bts_Flt16Mat2D_createIdentity( void );

/** creates rotation matrix */
struct bts_Flt16Mat2D bts_Flt16Mat2D_createRotation( phase16 angleA );

/** creates scale matrix */
struct bts_Flt16Mat2D bts_Flt16Mat2D_createScale( int32 scaleA, int32 scaleBbpA );

/** creates rigid matrix (scale & rotation) */
struct bts_Flt16Mat2D bts_Flt16Mat2D_createRigid( phase16 angleA, int32 scaleA, int32 scaleBbpA );

/** creates matrix from 16 bit values */
struct bts_Flt16Mat2D bts_Flt16Mat2D_create16( int16 xxA, int16 xyA, int16 yxA, int16 yyA, int16 bbpA );

/** creates matrix from 32 bit values (automatic adjustment of bbp value) */
struct bts_Flt16Mat2D bts_Flt16Mat2D_create32( int32 xxA, int32 xyA, int32 yxA, int32 yyA, int32 bbpA );

/** scales matrix by a factor */
void bts_Flt16Mat2D_scale( struct bts_Flt16Mat2D* ptrA, int32 scaleA, int32 scaleBbpA );

/** multiplies matrix with vecA; returns resulting vector */
struct bts_Int16Vec2D bts_Flt16Mat2D_map( const struct bts_Flt16Mat2D* matPtrA, 
								          const struct bts_Int16Vec2D* vecPtrA );

/** Multiplies matrix with float vecA; returns resulting vector. */
struct bts_Flt16Vec2D bts_Flt16Mat2D_mapFlt( const struct bts_Flt16Mat2D* matPtrA, 
								             const struct bts_Flt16Vec2D* vecPtrA );

/** multiplies matrix with matA; returns resulting matrix */
struct bts_Flt16Mat2D bts_Flt16Mat2D_mul( const struct bts_Flt16Mat2D* mat1PtrA, 
								          const struct bts_Flt16Mat2D* mat2PtrA );

/** multiplies matrix with matA; returns pointer to resulting matrix */
struct bts_Flt16Mat2D* bts_Flt16Mat2D_mulTo( struct bts_Flt16Mat2D* mat1PtrA, 
				                             const struct bts_Flt16Mat2D* mat2PtrA );

#endif /* bts_FLT16MAT2D_EM_H */

