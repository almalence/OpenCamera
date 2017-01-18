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

#include "b_TensorEm/Flt16Alt2D.h"
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

void bts_Flt16Alt2D_init( struct bts_Flt16Alt2D* ptrA )
{
	bts_Flt16Mat2D_init( &ptrA->matE );
	bts_Flt16Vec2D_init( &ptrA->vecE );
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Alt2D_exit( struct bts_Flt16Alt2D* ptrA )
{
	bts_Flt16Mat2D_exit( &ptrA->matE );
	bts_Flt16Vec2D_exit( &ptrA->vecE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ------------------------------------------------------------------------- */

void bts_Flt16Alt2D_copy( struct bts_Flt16Alt2D* ptrA, const struct bts_Flt16Alt2D* srcPtrA )
{
	bts_Flt16Mat2D_copy( &ptrA->matE, &srcPtrA->matE );
	bts_Flt16Vec2D_copy( &ptrA->vecE, &srcPtrA->vecE );
}

/* ------------------------------------------------------------------------- */

flag bts_Flt16Alt2D_equal( const struct bts_Flt16Alt2D* ptrA, const struct bts_Flt16Alt2D* srcPtrA )
{
	if( ! bts_Flt16Mat2D_equal( &ptrA->matE, &srcPtrA->matE ) ) return FALSE;
	if( ! bts_Flt16Vec2D_equal( &ptrA->vecE, &srcPtrA->vecE ) ) return FALSE;
	return TRUE;
}

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
	
uint32 bts_Flt16Alt2D_memSize( struct bbs_Context* cpA,
							   const struct bts_Flt16Alt2D *ptrA )
{
	bbs_ERROR0( "unimplemented function" );
	return 0;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Alt2D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Flt16Alt2D* ptrA, 
								uint16* memPtrA )
{
	bbs_ERROR0( "unimplemented function" );
	return 0;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Alt2D_memRead( struct bbs_Context* cpA,
							   struct bts_Flt16Alt2D* ptrA, 
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

struct bts_Flt16Alt2D bts_Flt16Alt2D_createIdentity()
{
	struct bts_Flt16Alt2D altL = { { 1, 0, 0, 1, 0 }, { 0, 0, 0 } };
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt2D bts_Flt16Alt2D_createRotation( phase16 angleA, 
													 const struct bts_Flt16Vec2D* centerPtrA )
{
	struct bts_Flt16Alt2D altL;
	altL.matE = bts_Flt16Mat2D_createRotation( angleA );
	altL.vecE = bts_Flt16Vec2D_sub( *centerPtrA, bts_Flt16Mat2D_mapFlt( &altL.matE, centerPtrA ) );
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt2D bts_Flt16Alt2D_createScale( int32 scaleA, 
												  int32 scaleBbpA, 
												  const struct bts_Flt16Vec2D* centerPtrA )
{
	struct bts_Flt16Alt2D altL;
	altL.matE = bts_Flt16Mat2D_createScale( scaleA, scaleBbpA );
	altL.vecE = bts_Flt16Vec2D_sub( *centerPtrA, bts_Flt16Mat2D_mapFlt( &altL.matE, centerPtrA ) );
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt2D bts_Flt16Alt2D_createRigid( phase16 angleA, 
												  int32 scaleA, 
												  int32 scaleBbpA,
												  const struct bts_Flt16Vec2D* centerPtrA )
{
	struct bts_Flt16Alt2D altL;
	altL.matE = bts_Flt16Mat2D_createRigid( angleA, scaleA, scaleBbpA );
	altL.vecE = bts_Flt16Vec2D_sub( *centerPtrA, bts_Flt16Mat2D_mapFlt( &altL.matE, centerPtrA ) );
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt2D bts_Flt16Alt2D_createRigidMap( struct bts_Flt16Vec2D vecIn1A,
												     struct bts_Flt16Vec2D vecIn2A,
												     struct bts_Flt16Vec2D vecOut1A,
												     struct bts_Flt16Vec2D vecOut2A )
{
	struct bts_Flt16Vec2D diffInL = bts_Flt16Vec2D_sub( vecIn1A, vecIn2A );
	struct bts_Flt16Vec2D diffOutL = bts_Flt16Vec2D_sub( vecOut1A, vecOut2A );
	struct bts_Flt16Vec2D centerInL = bts_Flt16Vec2D_mul( bts_Flt16Vec2D_add( vecIn1A, vecIn2A ), 1, 1 ); /* mul by 0.5 */ 
	struct bts_Flt16Vec2D centerOutL = bts_Flt16Vec2D_mul( bts_Flt16Vec2D_add( vecOut1A, vecOut2A ), 1, 1 ); /* mul by 0.5 */ 

	struct bts_Flt16Vec2D transL = bts_Flt16Vec2D_sub( centerOutL, centerInL );
	phase16 angleL = bts_Flt16Vec2D_enclosedAngle( &diffOutL, &diffInL );
	uint32 normInL = bts_Flt16Vec2D_norm( &diffInL );
	uint32 normOutL = bts_Flt16Vec2D_norm( &diffOutL );

	uint32 scaleL = ( normInL > 0 ) ? ( normOutL << 16 ) / normInL : 0xFFFFFFFF;
	uint32 scaleBbpL = 16 + diffOutL.bbpE - diffInL.bbpE;

	struct bts_Flt16Alt2D altL;

	/* fit scale factor in 15 bit */
	uint32 scaleExpL = bbs_intLog2( scaleL );
	if( scaleExpL > 14 )
	{
		scaleL >>= scaleExpL - 14;
		scaleBbpL -= scaleExpL - 14;
	}	

	altL = bts_Flt16Alt2D_createRigid( angleL, ( int16 )scaleL, scaleBbpL, &centerInL );

	altL.vecE = bts_Flt16Vec2D_add( altL.vecE, transL );

	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt2D bts_Flt16Alt2D_create16( int16 xxA, 
											   int16 xyA, 
											   int16 yxA, 
											   int16 yyA, 
											   int16 matBbpA,
											   int16 xA, 
											   int16 yA, 
											   int16 vecBbpA )
{
	struct bts_Flt16Alt2D altL;
	altL.matE = bts_Flt16Mat2D_create16( xxA, xyA, yxA, yyA, matBbpA );
	altL.vecE = bts_Flt16Vec2D_create16( xA, yA, vecBbpA );
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt2D bts_Flt16Alt2D_create32( int32 xxA, 
											   int32 xyA, 
											   int32 yxA, 
											   int32 yyA, 
											   int32 matBbpA,
											   int32 xA, 
											   int32 yA, 
											   int32 vecBbpA )
{
	struct bts_Flt16Alt2D altL;
	altL.matE = bts_Flt16Mat2D_create32( xxA, xyA, yxA, yyA, matBbpA );
	altL.vecE = bts_Flt16Vec2D_create32( xA, yA, vecBbpA );
	return altL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Flt16Alt2D_mapFlt( const struct bts_Flt16Alt2D* altPtrA, 
								             const struct bts_Flt16Vec2D* vecPtrA )
{
	return bts_Flt16Vec2D_add( altPtrA->vecE, bts_Flt16Mat2D_mapFlt( &altPtrA->matE, vecPtrA ) );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt2D bts_Flt16Alt2D_mul( const struct bts_Flt16Alt2D* alt1PtrA, 
								          const struct bts_Flt16Alt2D* alt2PtrA )
{
	struct bts_Flt16Alt2D altL;
	altL.vecE = bts_Flt16Alt2D_mapFlt( alt1PtrA, &alt2PtrA->vecE );
	altL.matE = bts_Flt16Mat2D_mul( &alt1PtrA->matE, &alt2PtrA->matE );
	return altL;
}

/* ------------------------------------------------------------------------- */

/** multiplies matrix with matA; returns pointer to resulting matrix */
struct bts_Flt16Alt2D* bts_Flt16Alt2D_mulTo( struct bts_Flt16Alt2D* alt1PtrA, 
				                             const struct bts_Flt16Alt2D* alt2PtrA )
{
	*alt1PtrA = bts_Flt16Alt2D_mul( alt1PtrA, alt2PtrA );
	return alt1PtrA;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Alt2D_invert( struct bts_Flt16Alt2D* ptrA )
{
	bts_Flt16Mat2D_invert( &ptrA->matE );
	
	ptrA->vecE = bts_Flt16Vec2D_create32( - ( ( ( int32 ) ptrA->matE.xxE * ptrA->vecE.xE + ( int32 ) ptrA->matE.xyE * ptrA->vecE.yE ) ),
										  - ( ( ( int32 ) ptrA->matE.yxE * ptrA->vecE.xE + ( int32 ) ptrA->matE.yyE * ptrA->vecE.yE ) ),
											ptrA->vecE.bbpE + ptrA->matE.bbpE );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt2D bts_Flt16Alt2D_inverted( const struct bts_Flt16Alt2D* ptrA )
{
	struct bts_Flt16Alt2D altL = *ptrA;
	bts_Flt16Alt2D_invert( &altL );
	return altL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


