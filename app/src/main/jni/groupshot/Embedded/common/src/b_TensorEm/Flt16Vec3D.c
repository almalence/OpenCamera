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

#include "b_TensorEm/Flt16Vec3D.h"
#include "b_TensorEm/Functions.h"
#include "b_BasicEm/Math.h"
#include "b_BasicEm/Memory.h"

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

void bts_Flt16Vec3D_init( struct bts_Flt16Vec3D* ptrA )
{
	ptrA->xE = 0;
	ptrA->yE = 0;
	ptrA->zE = 0;
	ptrA->bbpE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec3D_exit( struct bts_Flt16Vec3D* ptrA )
{
	ptrA->xE = 0;
	ptrA->yE = 0;
	ptrA->zE = 0;
	ptrA->bbpE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

flag bts_Flt16Vec3D_equal( const struct bts_Flt16Vec3D* ptrA, const struct bts_Flt16Vec3D* srcPtrA )
{
	int32 bbpDiffL = ptrA->bbpE - srcPtrA->bbpE;
	if( bbpDiffL == 0 )
	{
		if( ptrA->xE != srcPtrA->xE ) return FALSE;
		if( ptrA->yE != srcPtrA->yE ) return FALSE;
		if( ptrA->zE != srcPtrA->zE ) return FALSE;
		return TRUE;
	}

	if( bbpDiffL > 0 )
	{
		int32 xL = ( int32 ) srcPtrA->xE << bbpDiffL;
		int32 yL = ( int32 ) srcPtrA->yE << bbpDiffL;
		int32 zL = ( int32 ) srcPtrA->zE << bbpDiffL;
		if( ptrA->xE != xL ) return FALSE;
		if( ptrA->yE != yL ) return FALSE;
		if( ptrA->zE != zL ) return FALSE;
		return TRUE;
	}

	if( bbpDiffL < 0 )
	{
		int32 xL = ( int32 ) ptrA->xE << -bbpDiffL;
		int32 yL = ( int32 ) ptrA->yE << -bbpDiffL;
		int32 zL = ( int32 ) ptrA->zE << -bbpDiffL;
		if( xL != srcPtrA->xE ) return FALSE;
		if( yL != srcPtrA->yE ) return FALSE;
		if( zL != srcPtrA->zE ) return FALSE;
		return TRUE;
	}

	return TRUE;
}

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
	
uint32 bts_Flt16Vec3D_memSize( struct bbs_Context* cpA,
							   const struct bts_Flt16Vec3D *ptrA )
{
	return bbs_SIZEOF16( *ptrA );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Vec3D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Flt16Vec3D* ptrA, 
								uint16* memPtrA )
{
	bbs_ERROR0( "not implemented" );
	return 0;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Vec3D_memRead( struct bbs_Context* cpA,
							   struct bts_Flt16Vec3D* ptrA, 
							   const uint16* memPtrA )
{
	if( bbs_Context_error( cpA ) ) return 0;
	bbs_ERROR0( "not implemented" );
	return 0;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec3D bts_Flt16Vec3D_create16( int16 xA, int16 yA, int16 zA, int16 bbpA )
{
	struct bts_Flt16Vec3D vecL;
	vecL.xE = xA;
	vecL.yE = yA;
	vecL.zE = zA;
	vecL.bbpE = bbpA;
	return vecL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec3D bts_Flt16Vec3D_create32( int32 xA, int32 yA, int32 zA, int32 bbpA )
{
	struct bts_Flt16Vec3D vecL;
	if( ( xA | yA | zA ) == 0 )
	{
		vecL.xE = 0;
		vecL.yE = 0;
		vecL.zE = 0;
		vecL.bbpE = 0;
	}
	else
	{
		int32 shiftL = bts_maxAbsIntLog2Of3( xA, yA, zA ) - 13;

		if( shiftL > 0 )
		{
			int32 sh1L = shiftL - 1;
			vecL.xE = ( ( xA >> sh1L ) + 1 ) >> 1;
			vecL.yE = ( ( yA >> sh1L ) + 1 ) >> 1;
			vecL.zE = ( ( zA >> sh1L ) + 1 ) >> 1;
		}
		else
		{
			vecL.xE = xA << -shiftL;
			vecL.yE = yA << -shiftL;
			vecL.zE = zA << -shiftL;
		}
		vecL.bbpE = bbpA - shiftL;
	}
	return vecL;
}
	
/* ------------------------------------------------------------------------- */

uint32 bts_Flt16Vec3D_norm2( const struct bts_Flt16Vec3D* ptrA )
{
	return ( int32 ) ptrA->xE * ptrA->xE +
		   ( int32 ) ptrA->yE * ptrA->yE +
		   ( int32 ) ptrA->zE * ptrA->zE;
}

/* ------------------------------------------------------------------------- */

uint16 bts_Flt16Vec3D_norm( const struct bts_Flt16Vec3D* ptrA )
{
	return bbs_sqrt32( ( int32 ) ptrA->xE * ptrA->xE +
					   ( int32 ) ptrA->yE * ptrA->yE +
					   ( int32 ) ptrA->zE * ptrA->zE );
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec3D_normalize( struct bts_Flt16Vec3D* ptrA )
{
	int32 normL = bbs_sqrt32( ( int32 ) ptrA->xE * ptrA->xE +
							  ( int32 ) ptrA->yE * ptrA->yE +
							  ( int32 ) ptrA->zE * ptrA->zE );

	int32 xL = ( ( int32 ) ptrA->xE << 16 ) / normL;
	int32 yL = ( ( int32 ) ptrA->yE << 16 ) / normL;
	int32 zL = ( ( int32 ) ptrA->zE << 16 ) / normL;
	*ptrA = bts_Flt16Vec3D_create32( xL, yL, zL, 16 );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec3D bts_Flt16Vec3D_normalized( const struct bts_Flt16Vec3D* ptrA )
{
	struct bts_Flt16Vec3D vecL = *ptrA;
	bts_Flt16Vec3D_normalize( &vecL );
	return vecL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec3D bts_Flt16Vec3D_add( struct bts_Flt16Vec3D vec1A, struct bts_Flt16Vec3D vec2A )
{
	int32 xL, yL, zL, bbpL;
	int32 shiftL = vec1A.bbpE - vec2A.bbpE;

	if( shiftL > 0 )
	{
		xL = vec1A.xE + ( ( int32 ) vec2A.xE << shiftL );
		yL = vec1A.yE + ( ( int32 ) vec2A.yE << shiftL );
		zL = vec1A.zE + ( ( int32 ) vec2A.zE << shiftL );
		bbpL = vec1A.bbpE;
	}
	else
	{
		xL = ( ( int32 ) vec1A.xE << -shiftL ) + vec2A.xE;
		yL = ( ( int32 ) vec1A.yE << -shiftL ) + vec2A.yE;
		zL = ( ( int32 ) vec1A.zE << -shiftL ) + vec2A.zE;
		bbpL = vec2A.bbpE;
	}

	return bts_Flt16Vec3D_create32( xL, yL, zL, bbpL );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec3D bts_Flt16Vec3D_sub( struct bts_Flt16Vec3D vec1A, struct bts_Flt16Vec3D vec2A )
{
	int32 xL, yL, zL, bbpL;
	int32 shiftL = vec1A.bbpE - vec2A.bbpE;

	if( shiftL > 0 )
	{
		xL = vec1A.xE - ( ( int32 ) vec2A.xE << shiftL );
		yL = vec1A.yE - ( ( int32 ) vec2A.yE << shiftL );
		zL = vec1A.zE - ( ( int32 ) vec2A.zE << shiftL );
		bbpL = vec1A.bbpE;
	}
	else
	{
		xL = ( ( int32 ) vec1A.xE << -shiftL ) - vec2A.xE;
		yL = ( ( int32 ) vec1A.yE << -shiftL ) - vec2A.yE;
		zL = ( ( int32 ) vec1A.zE << -shiftL ) - vec2A.zE;
		bbpL = vec2A.bbpE;
	}

	return bts_Flt16Vec3D_create32( xL, yL, zL, bbpL );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec3D bts_Flt16Vec3D_mul( struct bts_Flt16Vec3D vecA, int16 factorA, int32 bbpFactorA )
{
	int32 xL = ( int32 ) vecA.xE * factorA;
	int32 yL = ( int32 ) vecA.yE * factorA;
	int32 zL = ( int32 ) vecA.zE * factorA;
	return bts_Flt16Vec3D_create32( xL, yL, zL, bbpFactorA + vecA.bbpE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

