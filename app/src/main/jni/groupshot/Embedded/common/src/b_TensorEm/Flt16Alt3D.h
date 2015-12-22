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

#ifndef bts_FLT16ALT3D_EM_H
#define bts_FLT16ALT3D_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_TensorEm/Flt16Mat3D.h"
#include "b_TensorEm/Flt16Vec3D.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** 3d affine linear trafo */
struct bts_Flt16Alt3D 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** matrix */
	struct bts_Flt16Mat3D matE;

	/** vector */
	struct bts_Flt16Vec3D vecE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes alt */
void bts_Flt16Alt3D_init( struct bts_Flt16Alt3D* ptrA );

/** destroys alt */
void bts_Flt16Alt3D_exit( struct bts_Flt16Alt3D* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_Flt16Alt3D_memSize( struct bbs_Context* cpA,
							   const struct bts_Flt16Alt3D* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_Flt16Alt3D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Flt16Alt3D* ptrA, 
								uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_Flt16Alt3D_memRead( struct bbs_Context* cpA,
							   struct bts_Flt16Alt3D* ptrA, 
							   const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** creates identity alt */
struct bts_Flt16Alt3D bts_Flt16Alt3D_createIdentity( void );

/** creates scale alt */
struct bts_Flt16Alt3D bts_Flt16Alt3D_createScale( int32 scaleA, 
												  int32 scaleBbpA, 
												  const struct bts_Flt16Vec3D* centerPtrA );

/** creates linear alt from matrix and center */
struct bts_Flt16Alt3D bts_Flt16Alt3D_createLinear( const struct bts_Flt16Mat3D* matPtrA,
												   const struct bts_Flt16Vec3D* centerPtrA );

/** creates alt from 16 bit values */
struct bts_Flt16Alt3D bts_Flt16Alt3D_create16( int16 xxA, int16 xyA, int16 xzA,
											   int16 yxA, int16 yyA, int16 yzA,
											   int16 zxA, int16 zyA, int16 zzA,
											   int16 matBbpA,
											   int16 xA, int16 yA, int16 zA,
											   int16 vecBbpA );

/** creates alt from 32 bit values (automatic adjustment of bbp value) */
struct bts_Flt16Alt3D bts_Flt16Alt3D_create32( int32 xxA, int32 xyA, int32 xzA,
											   int32 yxA, int32 yyA, int32 yzA,
											   int32 zxA, int32 zyA, int32 zzA,
											   int16 matBbpA,
											   int32 xA, int32 yA, int32 zA,
											   int16 vecBbpA );

/** Multiplies matrix with float vecA; returns resulting vector */
struct bts_Flt16Vec3D bts_Flt16Alt3D_mapFlt( const struct bts_Flt16Alt3D* matPtrA, 
								             const struct bts_Flt16Vec3D* vecPtrA );

/** multiplies alt with altA returns resulting alt */
struct bts_Flt16Alt3D bts_Flt16Alt3D_mul( const struct bts_Flt16Alt3D* mat1PtrA, 
								          const struct bts_Flt16Alt3D* mat2PtrA );

/** multiplies alt with matA; returns pointer to resulting alt */
struct bts_Flt16Alt3D* bts_Flt16Alt3D_mulTo( struct bts_Flt16Alt3D* mat1PtrA, 
				                             const struct bts_Flt16Alt3D* mat2PtrA );

#endif /* bts_FLT16ALT3D_EM_H */

