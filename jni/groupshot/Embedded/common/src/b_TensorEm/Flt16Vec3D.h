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

#ifndef bts_FLT16VEC3D_EM_H
#define bts_FLT16VEC3D_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/Phase.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** 3d vector with floating point */
struct bts_Flt16Vec3D 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** x component */
	int16 xE;

	/** y component */
	int16 yE;

	/** z component */
	int16 zE;

	/** point position */
	int16 bbpE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes vector */
void bts_Flt16Vec3D_init( struct bts_Flt16Vec3D* ptrA );

/** destroys vector */
void bts_Flt16Vec3D_exit( struct bts_Flt16Vec3D* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** equal operator */
flag bts_Flt16Vec3D_equal( const struct bts_Flt16Vec3D* ptrA, const struct bts_Flt16Vec3D* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_Flt16Vec3D_memSize( struct bbs_Context* cpA,
							   const struct bts_Flt16Vec3D* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_Flt16Vec3D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Flt16Vec3D* ptrA, 
								uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_Flt16Vec3D_memRead( struct bbs_Context* cpA,
							   struct bts_Flt16Vec3D* ptrA, 
							   const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** creates vector from 16 bit values */
struct bts_Flt16Vec3D bts_Flt16Vec3D_create16( int16 xA, int16 yA, int16 zA, int16 bbpA );

/** creates vector from 32 bit values (automatic adjustment of bbp value) */
struct bts_Flt16Vec3D bts_Flt16Vec3D_create32( int32 xA, int32 yA, int32 zA, int32 bbpA );

/** returns square norm of vector; return bbp is  ptrA->bbpE * 2 */
uint32 bts_Flt16Vec3D_norm2( const struct bts_Flt16Vec3D* ptrA );

/** returns norm of vector; return bbp is ptrA->bbpE*/
uint16 bts_Flt16Vec3D_norm( const struct bts_Flt16Vec3D* ptrA );

/** normalizes vector; bbpA defines number of bits behind the point */
void bts_Flt16Vec3D_normalize( struct bts_Flt16Vec3D* ptrA );

/** returns normalized vector; bbpA defines number of bits behind the point */
struct bts_Flt16Vec3D bts_Flt16Vec3D_normalized( const struct bts_Flt16Vec3D* ptrA );

/** adds two vectors; returns resulting vector */
struct bts_Flt16Vec3D bts_Flt16Vec3D_add( struct bts_Flt16Vec3D vec1A, struct bts_Flt16Vec3D vec2A );

/** subtracts vec1A - vec2A; returns resulting vector */
struct bts_Flt16Vec3D bts_Flt16Vec3D_sub( struct bts_Flt16Vec3D vec1A, struct bts_Flt16Vec3D vec2A );

/** multiplies vecor with scalar; returns resulting vector */
struct bts_Flt16Vec3D bts_Flt16Vec3D_mul( struct bts_Flt16Vec3D vecA, int16 factorA, int32 bbpFactorA );

#endif /* bts_FLT16VEC3D_EM_H */

