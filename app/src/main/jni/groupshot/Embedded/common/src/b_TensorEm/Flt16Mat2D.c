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

#include "b_TensorEm/Flt16Mat2D.h"
#include "b_TensorEm/Functions.h"
#include "b_BasicEm/Math.h"

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

void bts_Flt16Mat2D_init( struct bts_Flt16Mat2D* ptrA )
{
	ptrA->bbpE = 0;
	ptrA->xxE = 0;
	ptrA->xyE = 0;
	ptrA->yxE = 0;
	ptrA->yyE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Mat2D_exit( struct bts_Flt16Mat2D* ptrA )
{
	ptrA->bbpE = 0;
	ptrA->xxE = 0;
	ptrA->xyE = 0;
	ptrA->yxE = 0;
	ptrA->yyE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Flt16Mat2D_copy( struct bts_Flt16Mat2D* ptrA, const struct bts_Flt16Mat2D* srcPtrA )
{
	ptrA->bbpE = srcPtrA->bbpE;
	ptrA->xxE = srcPtrA->xxE;
	ptrA->xyE = srcPtrA->xyE;
	ptrA->yxE = srcPtrA->yxE;
	ptrA->yyE = srcPtrA->yyE;
}

/* ------------------------------------------------------------------------- */

flag bts_Flt16Mat2D_equal( const struct bts_Flt16Mat2D* ptrA, const struct bts_Flt16Mat2D* srcPtrA )
{
	int32 bbpDiffL = ptrA->bbpE - srcPtrA->bbpE;
	if( bbpDiffL == 0 )
	{
		if( ptrA->xxE != srcPtrA->xxE ) return FALSE;
		if( ptrA->xyE != srcPtrA->xyE ) return FALSE;
		if( ptrA->yxE != srcPtrA->yxE ) return FALSE;
		if( ptrA->yyE != srcPtrA->yyE ) return FALSE;
		return TRUE;
	}

	if( bbpDiffL > 0 )
	{
		int32 xxL = ( int32 ) srcPtrA->xxE << bbpDiffL;
		int32 xyL = ( int32 ) srcPtrA->xyE << bbpDiffL;
		int32 yxL = ( int32 ) srcPtrA->yxE << bbpDiffL;
		int32 yyL = ( int32 ) srcPtrA->yyE << bbpDiffL;

		if( ptrA->xxE != xxL ) return FALSE;
		if( ptrA->xyE != xyL ) return FALSE;
		if( ptrA->yxE != yxL ) return FALSE;
		if( ptrA->yyE != yyL ) return FALSE;

		/* check if bits were lost by the shifting */
		if( srcPtrA->xxE != ( xxL >> bbpDiffL ) ) return FALSE;
		if( srcPtrA->xyE != ( xyL >> bbpDiffL ) ) return FALSE;
		if( srcPtrA->yxE != ( yxL >> bbpDiffL ) ) return FALSE;
		if( srcPtrA->yyE != ( yyL >> bbpDiffL ) ) return FALSE;

		return TRUE;
	}

	if( bbpDiffL < 0 )
	{
		int32 xxL = ( int32 ) ptrA->xxE << -bbpDiffL;
		int32 xyL = ( int32 ) ptrA->xyE << -bbpDiffL;
		int32 yxL = ( int32 ) ptrA->yxE << -bbpDiffL;
		int32 yyL = ( int32 ) ptrA->yyE << -bbpDiffL;

		if( xxL != srcPtrA->xxE ) return FALSE;
		if( xyL != srcPtrA->xyE ) return FALSE;
		if( yxL != srcPtrA->yxE ) return FALSE;
		if( yyL != srcPtrA->yyE ) return FALSE;

		/* check if bits were lost by the shifting */
		if( ptrA->xxE != ( xxL >> -bbpDiffL ) ) return FALSE;
		if( ptrA->xyE != ( xyL >> -bbpDiffL ) ) return FALSE;
		if( ptrA->yxE != ( yxL >> -bbpDiffL ) ) return FALSE;
		if( ptrA->yyE != ( yyL >> -bbpDiffL ) ) return FALSE;

		return TRUE;
	}

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
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bts_Flt16Mat2D_det( const struct bts_Flt16Mat2D* ptrA )
{
	/* This could be negativ, in theory. But almost always det > 0 for us,
	   matrix is a rotation or scaling matrix.
	   Then uint32 makes sure there is no overflow. */
	uint32 detL = ( int32 ) ptrA->xxE * ptrA->yyE - ( int32 ) ptrA->xyE * ptrA->yxE;
	return detL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat2D bts_Flt16Mat2D_createIdentity()
{
	struct bts_Flt16Mat2D matL = { 1 << 14, 0, 0, 1 << 14, 14 };
	return matL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat2D bts_Flt16Mat2D_createRotation( phase16 angleA )
{
	int16 cL = bbs_cos16( angleA );
	int16 sL = bbs_sin16( angleA );
	struct bts_Flt16Mat2D matL;
	matL.xxE =  cL;
	matL.xyE = -sL;
	matL.yxE =  sL;
	matL.yyE =  cL;
	matL.bbpE = 14;
	return matL; 
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat2D bts_Flt16Mat2D_createScale( int32 scaleA, int32 scaleBbpA )
{
	struct bts_Flt16Mat2D matL = bts_Flt16Mat2D_createIdentity();
	bts_Flt16Mat2D_scale( &matL, scaleA, scaleBbpA );
	return matL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat2D bts_Flt16Mat2D_createRigid( phase16 angleA, int32 scaleA, int32 scaleBbpA )
{
	struct bts_Flt16Mat2D matL = bts_Flt16Mat2D_createRotation( angleA );
	bts_Flt16Mat2D_scale( &matL, scaleA, scaleBbpA );
	return matL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat2D bts_Flt16Mat2D_create16( int16 xxA, int16 xyA, int16 yxA, int16 yyA, int16 bbpA )
{
	struct bts_Flt16Mat2D matL;
	matL.xxE = xxA;
	matL.xyE = xyA;
	matL.yxE = yxA;
	matL.yyE = yyA;
	matL.bbpE = bbpA;
	return matL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat2D bts_Flt16Mat2D_create32( int32 xxA, int32 xyA, int32 yxA, int32 yyA, int32 bbpA )
{
	struct bts_Flt16Mat2D matL;

	if( ( xxA | xyA | yxA | yyA ) == 0 )
	{
		matL.xxE = 0;
		matL.xyE = 0;
		matL.yxE = 0;
		matL.yyE = 0;
		matL.bbpE = 0;
	}
	else
	{
		int32 shiftL = bts_maxAbsIntLog2Of4( xxA, xyA, yxA, yyA ) - 13;

		if( shiftL > 0 )
		{
			int32 sh1L = shiftL - 1;
			matL.xxE = ( ( xxA >> sh1L ) + 1 ) >> 1;
			matL.xyE = ( ( xyA >> sh1L ) + 1 ) >> 1;
			matL.yxE = ( ( yxA >> sh1L ) + 1 ) >> 1;
			matL.yyE = ( ( yyA >> sh1L ) + 1 ) >> 1;
		}
		else
		{
			matL.xxE = xxA << -shiftL;
			matL.xyE = xyA << -shiftL;
			matL.yxE = yxA << -shiftL;
			matL.yyE = yyA << -shiftL;
		}

		matL.bbpE = bbpA - shiftL;
	}

	return matL;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Mat2D_scale( struct bts_Flt16Mat2D* ptrA, int32 scaleA, int32 scaleBbpA )
{
	/* fit scale in 15 bit */
	uint32 scaleExpL = bts_absIntLog2( scaleA );
	if( scaleExpL > 14 )
	{
		int32 shiftL = scaleExpL - 14;
		scaleA = ( ( scaleA >> ( shiftL - 1 ) ) + 1 ) >> 1;
		scaleBbpA -= shiftL;
	}
	
	*ptrA = bts_Flt16Mat2D_create32( (int32)ptrA->xxE * scaleA,
									 (int32)ptrA->xyE * scaleA,
									 (int32)ptrA->yxE * scaleA,
									 (int32)ptrA->yyE * scaleA,
									 ptrA->bbpE + scaleBbpA );
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Vec2D bts_Flt16Mat2D_map( const struct bts_Flt16Mat2D* matPtrA, 
								          const struct bts_Int16Vec2D* vecPtrA )
{
	struct bts_Int16Vec2D vecL;

	int32 xL = ( int32 ) matPtrA->xxE * vecPtrA->xE + ( int32 ) matPtrA->xyE * vecPtrA->yE;
	int32 yL = ( int32 ) matPtrA->yxE * vecPtrA->xE + ( int32 ) matPtrA->yyE * vecPtrA->yE;

	if( matPtrA->bbpE > 0 )
	{
		int32 sh1L = matPtrA->bbpE - 1;
		vecL.xE = ( ( xL >> sh1L ) + 1 ) >> 1;
		vecL.yE = ( ( yL >> sh1L ) + 1 ) >> 1;
	}
	else
	{
		/* not overflow safe */
		vecL.xE = xL << -matPtrA->bbpE;
		vecL.yE = yL << -matPtrA->bbpE;
	}

	return vecL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Flt16Mat2D_mapFlt( const struct bts_Flt16Mat2D* matPtrA, 
								             const struct bts_Flt16Vec2D* vecPtrA )
{
	int32 xL = ( int32 ) matPtrA->xxE * vecPtrA->xE + ( int32 ) matPtrA->xyE * vecPtrA->yE;
	int32 yL = ( int32 ) matPtrA->yxE * vecPtrA->xE + ( int32 ) matPtrA->yyE * vecPtrA->yE;
	int32 bbpL = matPtrA->bbpE + vecPtrA->bbpE;
	return bts_Flt16Vec2D_create32( xL, yL, bbpL );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat2D bts_Flt16Mat2D_mul( const struct bts_Flt16Mat2D* mat1PtrA, 
								          const struct bts_Flt16Mat2D* mat2PtrA )
{
	return bts_Flt16Mat2D_create32( ( int32 ) mat1PtrA->xxE * mat2PtrA->xxE + ( int32 ) mat1PtrA->xyE * mat2PtrA->yxE,
									( int32 ) mat1PtrA->xxE * mat2PtrA->xyE + ( int32 ) mat1PtrA->xyE * mat2PtrA->yyE,
									( int32 ) mat1PtrA->yxE * mat2PtrA->xxE + ( int32 ) mat1PtrA->yyE * mat2PtrA->yxE,
									( int32 ) mat1PtrA->yxE * mat2PtrA->xyE + ( int32 ) mat1PtrA->yyE * mat2PtrA->yyE,
									mat1PtrA->bbpE + mat2PtrA->bbpE );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat2D* bts_Flt16Mat2D_mulTo( struct bts_Flt16Mat2D* mat1PtrA, 
				                             const struct bts_Flt16Mat2D* mat2PtrA )
{
	*mat1PtrA = bts_Flt16Mat2D_mul( mat1PtrA, mat2PtrA );
	return mat1PtrA;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Mat2D_invert( struct bts_Flt16Mat2D* ptrA )
{
	int32 detL = ( int32 ) ptrA->xxE * ptrA->yyE - ( int32 ) ptrA->xyE * ptrA->yxE;
	int32 detExpL = bbs_intLog2( detL );
	int32 dShrL = 0;
	if( detExpL > 15 )
	{
		dShrL = detExpL - 15; 
		detL = ( ( detL >> ( dShrL - 1 ) ) + 1 ) >> 1;
	}

	if( detL == 0 )
	{
		ptrA->xxE = ptrA->yyE = ptrA->xyE = ptrA->yxE = 0;
	}
	else
	{
		/* bbp: bbpE + 16 - ( bbpE * 2 - dShrL ) = 16 + dShrL - bbpE */
		int32 xxL = ( ( int32 )ptrA->xxE << 16 ) / detL;
		int32 xyL = ( ( int32 )ptrA->xyE << 16 ) / detL;
		int32 yxL = ( ( int32 )ptrA->yxE << 16 ) / detL;
		int32 yyL = ( ( int32 )ptrA->yyE << 16 ) / detL;
		*ptrA = bts_Flt16Mat2D_create32( xxL, -xyL, -yxL, yyL, 16 + dShrL - ptrA->bbpE );
	}
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat2D bts_Flt16Mat2D_inverted( const struct bts_Flt16Mat2D* ptrA )
{
	struct bts_Flt16Mat2D matL = *ptrA;
	bts_Flt16Mat2D_invert( &matL );
	return matL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


