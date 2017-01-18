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

#include "b_TensorEm/Flt16Vec2D.h"
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

void bts_Flt16Vec2D_init( struct bts_Flt16Vec2D* ptrA )
{
	ptrA->xE = 0;
	ptrA->yE = 0;
	ptrA->bbpE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec2D_exit( struct bts_Flt16Vec2D* ptrA )
{
	ptrA->xE = 0;
	ptrA->yE = 0;
	ptrA->bbpE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec2D_copy( struct bts_Flt16Vec2D* ptrA, const struct bts_Flt16Vec2D* srcPtrA )
{
	ptrA->bbpE = srcPtrA->bbpE;
	ptrA->xE = srcPtrA->xE;
	ptrA->yE = srcPtrA->yE;
}

/* ------------------------------------------------------------------------- */

flag bts_Flt16Vec2D_equal( const struct bts_Flt16Vec2D* ptrA, const struct bts_Flt16Vec2D* srcPtrA )
{
	int32 bbpDiffL = ptrA->bbpE - srcPtrA->bbpE;
	if( bbpDiffL == 0 )
	{
		if( ptrA->xE != srcPtrA->xE ) return FALSE;
		if( ptrA->yE != srcPtrA->yE ) return FALSE;
		return TRUE;
	}

	if( bbpDiffL > 0 )
	{
		int32 xL = ( int32 ) srcPtrA->xE << bbpDiffL;
		int32 yL = ( int32 ) srcPtrA->yE << bbpDiffL;
		if( ptrA->xE != xL ) return FALSE;
		if( ptrA->yE != yL ) return FALSE;
		/* check if bits were lost by the shifting */
		if( srcPtrA->xE != ( xL >> bbpDiffL ) ) return FALSE;
		if( srcPtrA->yE != ( yL >> bbpDiffL ) ) return FALSE;
		return TRUE;
	}

	if( bbpDiffL < 0 )
	{
		int32 xL = ( int32 ) ptrA->xE << -bbpDiffL;
		int32 yL = ( int32 ) ptrA->yE << -bbpDiffL;
		if( xL != srcPtrA->xE ) return FALSE;
		if( yL != srcPtrA->yE ) return FALSE;
		/* check if bits were lost by the shifting */
		if( ptrA->xE != ( xL >> -bbpDiffL ) ) return FALSE;
		if( ptrA->yE != ( yL >> -bbpDiffL ) ) return FALSE;
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
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Flt16Vec2D_create16( int16 xA, int16 yA, int16 bbpA )
{
	struct bts_Flt16Vec2D vecL;
	vecL.xE = xA;
	vecL.yE = yA;
	vecL.bbpE = bbpA;
	return vecL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Flt16Vec2D_createVec16( struct bts_Int16Vec2D vecA, int16 bbpA )
{
	struct bts_Flt16Vec2D vecL;
	vecL.xE = vecA.xE;
	vecL.yE = vecA.yE;
	vecL.bbpE = bbpA;
	return vecL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Flt16Vec2D_create32( int32 xA, int32 yA, int32 bbpA )
{
	struct bts_Flt16Vec2D vecL;
	if( ( xA | yA ) == 0 )
	{
		vecL.xE = 0;
		vecL.yE = 0;
		vecL.bbpE = 0;
	}
	else
	{
		int32 shiftL = bts_maxAbsIntLog2Of2( xA, yA ) - 13;

		if( shiftL > 0 )
		{
			int32 sh1L = shiftL - 1;
			vecL.xE = ( ( xA >> sh1L ) + 1 ) >> 1;
			vecL.yE = ( ( yA >> sh1L ) + 1 ) >> 1;
		}
		else
		{
			vecL.xE = xA << -shiftL;
			vecL.yE = yA << -shiftL;
		}
		vecL.bbpE = bbpA - shiftL;
	}
	return vecL;
}

/* ------------------------------------------------------------------------- */

int32 bts_Flt16Vec2D_dotPrd( const struct bts_Flt16Vec2D* vec1PtrA, 
							 const struct bts_Flt16Vec2D* vec2PtrA )
{
	return ( int32 ) vec1PtrA->xE * vec2PtrA->xE + ( int32 ) vec1PtrA->yE * vec2PtrA->yE;
}
	
/* ------------------------------------------------------------------------- */

uint32 bts_Flt16Vec2D_norm2( const struct bts_Flt16Vec2D* ptrA )
{
	return ( int32 ) ptrA->xE * ptrA->xE + ( int32 ) ptrA->yE * ptrA->yE;
}

/* ------------------------------------------------------------------------- */

uint16 bts_Flt16Vec2D_norm( const struct bts_Flt16Vec2D* ptrA )
{
	return bbs_sqrt32( ( int32 ) ptrA->xE * ptrA->xE + ( int32 ) ptrA->yE * ptrA->yE );
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec2D_normalize( struct bts_Flt16Vec2D* ptrA )
{
	int32 normL = bbs_sqrt32( ( int32 ) ptrA->xE * ptrA->xE + ( int32 ) ptrA->yE * ptrA->yE );
	int32 xL = ( ( int32 ) ptrA->xE << 16 ) / normL;
	int32 yL = ( ( int32 ) ptrA->yE << 16 ) / normL;
	*ptrA = bts_Flt16Vec2D_create32( xL, yL, 16 );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Flt16Vec2D_normalized( const struct bts_Flt16Vec2D* ptrA )
{
	struct bts_Flt16Vec2D vecL = *ptrA;
	bts_Flt16Vec2D_normalize( &vecL );
	return vecL;
}

/* ------------------------------------------------------------------------- */

phase16 bts_Flt16Vec2D_angle( const struct bts_Flt16Vec2D* vecPtrA )
{
	return bbs_phase16( vecPtrA->xE, vecPtrA->yE );
}

/* ------------------------------------------------------------------------- */

phase16 bts_Flt16Vec2D_enclosedAngle( const struct bts_Flt16Vec2D* vec1PtrA, 
									  const struct bts_Flt16Vec2D* vec2PtrA )
{
	int32 xL = ( int32 ) vec1PtrA->xE * vec2PtrA->xE + ( int32 ) vec1PtrA->yE * vec2PtrA->yE;
	int32 yL = ( int32 ) vec1PtrA->yE * vec2PtrA->xE - ( int32 ) vec1PtrA->xE * vec2PtrA->yE;
	return bbs_phase16( xL, yL );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Flt16Vec2D_add( struct bts_Flt16Vec2D vec1A, struct bts_Flt16Vec2D vec2A )
{
	int32 xL, yL, bbpL;
	int32 shiftL = vec1A.bbpE - vec2A.bbpE;

	if( shiftL > 0 )
	{
		xL = ( ( int32 ) vec2A.xE << shiftL ) + vec1A.xE;
		yL = ( ( int32 ) vec2A.yE << shiftL ) + vec1A.yE;
		bbpL = vec1A.bbpE;
	}
	else
	{
		xL = ( ( int32 ) vec1A.xE << -shiftL ) + vec2A.xE;
		yL = ( ( int32 ) vec1A.yE << -shiftL ) + vec2A.yE;
		bbpL = vec2A.bbpE;
	}

	return bts_Flt16Vec2D_create32( xL, yL, bbpL );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Flt16Vec2D_sub( struct bts_Flt16Vec2D vec1A, struct bts_Flt16Vec2D vec2A )
{
	int32 xL, yL, bbpL;
	int32 shiftL = vec1A.bbpE - vec2A.bbpE;

	if( shiftL > 0 )
	{
		xL = ( int32 ) vec1A.xE - ( ( int32 ) vec2A.xE << shiftL );
		yL = ( int32 ) vec1A.yE - ( ( int32 ) vec2A.yE << shiftL );
		bbpL = vec1A.bbpE;
	}
	else
	{
		xL = ( ( int32 ) vec1A.xE << -shiftL ) - vec2A.xE;
		yL = ( ( int32 ) vec1A.yE << -shiftL ) - vec2A.yE;
		bbpL = vec2A.bbpE;
	}

	return bts_Flt16Vec2D_create32( xL, yL, bbpL );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Flt16Vec2D_mul( struct bts_Flt16Vec2D vecA, int16 factorA, int32 bbpFactorA )
{
	int32 xL = ( int32 ) vecA.xE * factorA;
	int32 yL = ( int32 ) vecA.yE * factorA;
	return bts_Flt16Vec2D_create32( xL, yL, bbpFactorA + vecA.bbpE );
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Vec2D bts_Flt16Vec2D_int16Vec2D( struct bts_Flt16Vec2D vecA, int32 dstBbpA )
{
	struct bts_Int16Vec2D vecL;
	int32 shiftL = vecA.bbpE - dstBbpA;

	if( shiftL > 0 )
	{
		vecL.xE = ( ( vecA.xE >> ( shiftL - 1 ) ) + 1 ) >> 1;
		vecL.yE = ( ( vecA.yE >> ( shiftL - 1 ) ) + 1 ) >> 1;
	}
	else
	{
		vecL.xE = vecA.xE << ( -shiftL );
		vecL.yE = vecA.yE << ( -shiftL );
	}

	return vecL;
}

/* ========================================================================= */


