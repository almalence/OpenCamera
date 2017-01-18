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

#ifndef bts_FLT16ALT2D_EM_H
#define bts_FLT16ALT2D_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_TensorEm/Flt16Mat2D.h"
#include "b_TensorEm/Flt16Vec2D.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** 2d affine linear trafo */
struct bts_Flt16Alt2D 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** matrix */
	struct bts_Flt16Mat2D matE;

	/** vector */
	struct bts_Flt16Vec2D vecE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes alt */
void bts_Flt16Alt2D_init( struct bts_Flt16Alt2D* ptrA );

/** destroys alt */
void bts_Flt16Alt2D_exit( struct bts_Flt16Alt2D* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bts_Flt16Alt2D_copy( struct bts_Flt16Alt2D* ptrA, 
						  const struct bts_Flt16Alt2D* srcPtrA );

/** equal operator */
flag bts_Flt16Alt2D_equal( const struct bts_Flt16Alt2D* ptrA, 
						   const struct bts_Flt16Alt2D* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_Flt16Alt2D_memSize( struct bbs_Context* cpA,
							   const struct bts_Flt16Alt2D* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_Flt16Alt2D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Flt16Alt2D* ptrA, 
								uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_Flt16Alt2D_memRead( struct bbs_Context* cpA,
							   struct bts_Flt16Alt2D* ptrA, 
							   const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** inverts alt */
void bts_Flt16Alt2D_invert( struct bts_Flt16Alt2D* ptrA );

/** returns inverted alt */
struct bts_Flt16Alt2D bts_Flt16Alt2D_inverted( const struct bts_Flt16Alt2D* ptrA );

/** creates identity alt */
struct bts_Flt16Alt2D bts_Flt16Alt2D_createIdentity( void );

/** creates rotation alt */
struct bts_Flt16Alt2D bts_Flt16Alt2D_createRotation( phase16 angleA, 
													 const struct bts_Flt16Vec2D* centerPtrA );

/** creates scale alt */
struct bts_Flt16Alt2D bts_Flt16Alt2D_createScale( int32 scaleA, 
												  int32 scaleBbpA, 
												  const struct bts_Flt16Vec2D* centerPtrA );

/** creates rigid alt (scale & rotation) */
struct bts_Flt16Alt2D bts_Flt16Alt2D_createRigid( phase16 angleA, 
												  int32 scaleA, 
												  int32 scaleBbpA, 
												  const struct bts_Flt16Vec2D* centerPtrA );

/** creates rigid alt (scale & rotation) that mapps vecIn1 and vecIn2 to vecOut1 and vecOut2*/
struct bts_Flt16Alt2D bts_Flt16Alt2D_createRigidMap( struct bts_Flt16Vec2D vecIn1A,
												     struct bts_Flt16Vec2D vecIn2A,
												     struct bts_Flt16Vec2D vecOut1A,
												     struct bts_Flt16Vec2D vecOut2A );

/** creates alt from 16 bit values */
struct bts_Flt16Alt2D bts_Flt16Alt2D_create16( int16 xxA, 
											   int16 xyA, 
											   int16 yxA, 
											   int16 yyA, 
											   int16 matBbpA,
											   int16 xA, 
											   int16 yA, 
											   int16 vecBbpA );

/** creates alt from 32 bit values (automatic adjustment of bbp value) */
struct bts_Flt16Alt2D bts_Flt16Alt2D_create32( int32 xxA, 
											   int32 xyA, 
											   int32 yxA, 
											   int32 yyA,
											   int32 matBbpA,
											   int32 xA, 
											   int32 yA, 
											   int32 vecBbpA );

/** Multiplies matrix with float vecA; returns resulting vector. 
 *  bbp can get changed.
 */
struct bts_Flt16Vec2D bts_Flt16Alt2D_mapFlt( const struct bts_Flt16Alt2D* matPtrA, 
								             const struct bts_Flt16Vec2D* vecPtrA );

/** multiplies alt with altA returns resulting alt */
struct bts_Flt16Alt2D bts_Flt16Alt2D_mul( const struct bts_Flt16Alt2D* alt1PtrA, 
								          const struct bts_Flt16Alt2D* alt2PtrA );

/** multiplies alt with matA; returns pointer to resulting alt */
struct bts_Flt16Alt2D* bts_Flt16Alt2D_mulTo( struct bts_Flt16Alt2D* alt1PtrA, 
				                             const struct bts_Flt16Alt2D* alt2PtrA );

#endif /* bts_FLT16ALT2D_EM_H */

