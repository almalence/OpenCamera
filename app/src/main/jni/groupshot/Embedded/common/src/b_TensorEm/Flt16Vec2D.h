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

#ifndef bts_FLT16VEC2D_EM_H
#define bts_FLT16VEC2D_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"
#include "b_BasicEm/Phase.h"
#include "b_TensorEm/Int16Vec2D.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** 2d vector with floating point */
struct bts_Flt16Vec2D 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** x component */
	int16 xE;

	/** y component */
	int16 yE;

	/** point position */
	int16 bbpE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes vector */
void bts_Flt16Vec2D_init( struct bts_Flt16Vec2D* ptrA );

/** destroys vector */
void bts_Flt16Vec2D_exit( struct bts_Flt16Vec2D* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bts_Flt16Vec2D_copy( struct bts_Flt16Vec2D* ptrA, const struct bts_Flt16Vec2D* srcPtrA );

/** equal operator */
flag bts_Flt16Vec2D_equal( const struct bts_Flt16Vec2D* ptrA, const struct bts_Flt16Vec2D* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** creates vector from 16 bit values */
struct bts_Flt16Vec2D bts_Flt16Vec2D_create16( int16 xA, int16 yA, int16 bbpA );

/** creates vector from 16 bit vactor */
struct bts_Flt16Vec2D bts_Flt16Vec2D_createVec16( struct bts_Int16Vec2D vecA, int16 bbpA );

/** creates vector from 32 bit values (automatic adjustment of bbp value) */
struct bts_Flt16Vec2D bts_Flt16Vec2D_create32( int32 xA, int32 yA, int32 bbpA );


/** dot product of vectors; return bbp is  vec1PtrA->bbpE + vec2PtrA->bbpE */
int32 bts_Flt16Vec2D_dotPrd( const struct bts_Flt16Vec2D* vec1PtrA, 
							 const struct bts_Flt16Vec2D* vec2PtrA );

/** returns square norm of vector; return bbp is  ptrA->bbpE * 2 */
uint32 bts_Flt16Vec2D_norm2( const struct bts_Flt16Vec2D* ptrA );

/** returns norm of vector; return bbp is ptrA->bbpE*/
uint16 bts_Flt16Vec2D_norm( const struct bts_Flt16Vec2D* ptrA );

/** normalizes vector; bbpA defines number of bits behind the point */
void bts_Flt16Vec2D_normalize( struct bts_Flt16Vec2D* ptrA );

/** returns normalized vector; bbpA defines number of bits behind the point */
struct bts_Flt16Vec2D bts_Flt16Vec2D_normalized( const struct bts_Flt16Vec2D* ptrA );

/** computes angle between vector and x axis*/
phase16 bts_Flt16Vec2D_angle( const struct bts_Flt16Vec2D* vecPtrA );

/** computes angle between two vectors */
phase16 bts_Flt16Vec2D_enclosedAngle( const struct bts_Flt16Vec2D* vec1PtrA, 
									  const struct bts_Flt16Vec2D* vec2PtrA );

/** adds two vectors; returns resulting vector */
struct bts_Flt16Vec2D bts_Flt16Vec2D_add( struct bts_Flt16Vec2D vec1A, struct bts_Flt16Vec2D vec2A );

/** subtracts vec1A - vec2A; returns resulting vector */
struct bts_Flt16Vec2D bts_Flt16Vec2D_sub( struct bts_Flt16Vec2D vec1A, struct bts_Flt16Vec2D vec2A );

/** multiplies vecor with scalar; returns resulting vector */
struct bts_Flt16Vec2D bts_Flt16Vec2D_mul( struct bts_Flt16Vec2D vecA, int16 factorA, int32 bbpFactorA );

/** converts vecA into bts_Int16Vec2D */
struct bts_Int16Vec2D bts_Flt16Vec2D_int16Vec2D( struct bts_Flt16Vec2D vecA, int32 dstBbpA );

#endif /* bts_FLT16VEC2D_EM_H */

