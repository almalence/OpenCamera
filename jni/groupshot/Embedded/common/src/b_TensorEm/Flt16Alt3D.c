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

/* ---- includes ----------------------------------------------------------- */

#include "b_TensorEm/Flt16Alt3D.h"
#include "b_BasicEm/Math.h"
#include "b_BasicEm/Memory.h"
#include "b_BasicEm/Functions.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Flt16Alt3D_init( struct bts_Flt16Alt3D* ptrA )
{
	bts_Flt16Mat3D_init( &ptrA->matE );
	bts_Flt16Vec3D_init( &ptrA->vecE );
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Alt3D_exit( struct bts_Flt16Alt3D* ptrA )
{
	bts_Flt16Mat3D_exit( &ptrA->matE );
	bts_Flt16Vec3D_exit( &ptrA->vecE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Alt3D_memSize( struct bbs_Context* cpA,
							   const struct bts_Flt16Alt3D *ptrA )
{
	bbs_ERROR0( "unimplemented function" );
	return 0;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Alt3D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Flt16Alt3D* ptrA, 
								uint16* memPtrA )
{
	bbs_ERROR0( "unimplemented function" );
	return 0;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Alt3D_memRead( struct bbs_Context* cpA,
							   struct bts_Flt16Alt3D* ptrA, 
							   const uint16* memPtrA )
{
	if( bbs_Context_error( cpA ) ) return 0;
	bbs_ERROR0( "unimplemented function" );
	return 0;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt3D bts_Flt16Alt3D_createIdentity()
{
	struct bts_Flt16Alt3D altL = { { 1, 0, 0,
									 0, 1, 0,
									 0, 0, 1, 0 }, { 0, 0, 0, 0 } };
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt3D bts_Flt16Alt3D_createScale( int32 scaleA, 
												  int32 scaleBbpA, 
												  const struct bts_Flt16Vec3D* centerPtrA )
{
	struct bts_Flt16Alt3D altL;
	altL.matE = bts_Flt16Mat3D_createScale( scaleA, scaleBbpA );
	altL.vecE = bts_Flt16Vec3D_sub( *centerPtrA, bts_Flt16Mat3D_mapFlt( &altL.matE, centerPtrA ) );
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt3D bts_Flt16Alt3D_createLinear( const struct bts_Flt16Mat3D* matPtrA,
												   const struct bts_Flt16Vec3D* centerPtrA )
{
	struct bts_Flt16Alt3D altL;
	altL.matE = *matPtrA;
	altL.vecE = bts_Flt16Vec3D_sub( *centerPtrA, bts_Flt16Mat3D_mapFlt( &altL.matE, centerPtrA ) );
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt3D bts_Flt16Alt3D_create16( int16 xxA, int16 xyA, int16 xzA,
											   int16 yxA, int16 yyA, int16 yzA,
											   int16 zxA, int16 zyA, int16 zzA,
											   int16 matBbpA,
											   int16 xA, int16 yA, int16 zA,
											   int16 vecBbpA )
{
	struct bts_Flt16Alt3D altL;
	altL.matE = bts_Flt16Mat3D_create16( xxA, xyA, xzA,
										 yxA, yyA, yzA,
										 zxA, zyA, zzA,
										 matBbpA );

	altL.vecE = bts_Flt16Vec3D_create16( xA, yA, zA, vecBbpA );
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt3D bts_Flt16Alt3D_create32( int32 xxA, int32 xyA, int32 xzA,
											   int32 yxA, int32 yyA, int32 yzA,
											   int32 zxA, int32 zyA, int32 zzA,
											   int16 matBbpA,
											   int32 xA, int32 yA, int32 zA,
											   int16 vecBbpA )
{
	struct bts_Flt16Alt3D altL;
	altL.matE = bts_Flt16Mat3D_create32( xxA, xyA, xzA,
										 yxA, yyA, yzA,
										 zxA, zyA, zzA,
										 matBbpA );

	altL.vecE = bts_Flt16Vec3D_create32( xA, yA, zA, vecBbpA );
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec3D bts_Flt16Alt3D_mapFlt( const struct bts_Flt16Alt3D* altPtrA, 
								             const struct bts_Flt16Vec3D* vecPtrA )
{
	struct bts_Flt16Vec3D vecL = bts_Flt16Mat3D_mapFlt( &altPtrA->matE, vecPtrA );
	int32 shiftL = altPtrA->vecE.bbpE - vecL.bbpE;
	if( shiftL > 0 )
	{
		int32 sh1L = shiftL - 1;
		vecL.xE += ( ( altPtrA->vecE.xE >> sh1L ) + 1 ) >> 1;
		vecL.yE += ( ( altPtrA->vecE.yE >> sh1L ) + 1 ) >> 1;
		vecL.zE += ( ( altPtrA->vecE.zE >> sh1L ) + 1 ) >> 1;
	}
	else
	{
		vecL.xE += altPtrA->vecE.xE << -shiftL;
		vecL.yE += altPtrA->vecE.yE << -shiftL;
		vecL.zE += altPtrA->vecE.zE << -shiftL;
	}
	return vecL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt3D bts_Flt16Alt3D_mul( const struct bts_Flt16Alt3D* alt1PtrA, 
								          const struct bts_Flt16Alt3D* alt2PtrA )
{
	struct bts_Flt16Alt3D altL;
	altL.vecE = bts_Flt16Alt3D_mapFlt( alt1PtrA, &alt2PtrA->vecE );
	altL.matE = bts_Flt16Mat3D_mul( &alt1PtrA->matE, &alt2PtrA->matE );
	return altL;
}

/* ------------------------------------------------------------------------- */

/** multiplies matrix with matA; returns pointer to resulting matrix */
struct bts_Flt16Alt3D* bts_Flt16Alt3D_mulTo( struct bts_Flt16Alt3D* alt1PtrA, 
				                             const struct bts_Flt16Alt3D* alt2PtrA )
{
	*alt1PtrA = bts_Flt16Alt3D_mul( alt1PtrA, alt2PtrA );
	return alt1PtrA;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

