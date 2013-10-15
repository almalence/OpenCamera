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

#include "b_TensorEm/Flt16Mat3D.h"
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

void bts_Flt16Mat3D_init( struct bts_Flt16Mat3D* ptrA )
{
	ptrA->bbpE = 0;
	ptrA->xxE = 0;
	ptrA->xyE = 0;
	ptrA->xzE = 0;
	ptrA->yxE = 0;
	ptrA->yyE = 0;
	ptrA->yzE = 0;
	ptrA->zxE = 0;
	ptrA->zyE = 0;
	ptrA->zzE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Mat3D_exit( struct bts_Flt16Mat3D* ptrA )
{
	ptrA->bbpE = 0;
	ptrA->xxE = 0;
	ptrA->xyE = 0;
	ptrA->xzE = 0;
	ptrA->yxE = 0;
	ptrA->yyE = 0;
	ptrA->yzE = 0;
	ptrA->zxE = 0;
	ptrA->zyE = 0;
	ptrA->zzE = 0;
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
	
uint32 bts_Flt16Mat3D_memSize( struct bbs_Context* cpA,
							   const struct bts_Flt16Mat3D *ptrA )
{
	return bbs_SIZEOF16( *ptrA );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Mat3D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Flt16Mat3D* ptrA, 
								uint16* memPtrA )
{
	bbs_ERROR0( "not implemented" );
	return 0;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Mat3D_memRead( struct bbs_Context* cpA,
							   struct bts_Flt16Mat3D* ptrA, 
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

struct bts_Flt16Mat3D bts_Flt16Mat3D_createIdentity()
{
	struct bts_Flt16Mat3D matL = { 1 << 14, 0, 0, 0, 1 << 14, 0, 0, 0, 1 << 14, 14 };
	return matL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat3D bts_Flt16Mat3D_createScale( int32 scaleA, int32 scaleBbpA )
{
	struct bts_Flt16Mat3D matL = bts_Flt16Mat3D_createIdentity();
	bts_Flt16Mat3D_scale( &matL, scaleA, scaleBbpA );
	return matL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat3D bts_Flt16Mat3D_create16( int16 xxA, int16 xyA, int16 xzA,
											   int16 yxA, int16 yyA, int16 yzA,
											   int16 zxA, int16 zyA, int16 zzA,
											   int16 bbpA )
{
	struct bts_Flt16Mat3D matL;
	matL.xxE = xxA;
	matL.xyE = xyA;
	matL.xzE = xzA;
	matL.yxE = yxA;
	matL.yyE = yyA;
	matL.yzE = yzA;
	matL.zxE = zxA;
	matL.zyE = zyA;
	matL.zzE = zzA;
	matL.bbpE = bbpA;
	return matL;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat3D bts_Flt16Mat3D_create32( int32 xxA, int32 xyA, int32 xzA,
											   int32 yxA, int32 yyA, int32 yzA,
											   int32 zxA, int32 zyA, int32 zzA,
											   int32 bbpA )
{
	struct bts_Flt16Mat3D matL;

	if( ( xxA | xyA | xzA | yxA | yyA | yzA | zxA | zyA | zzA ) == 0 )
	{
		matL.xxE = 0;
		matL.xyE = 0;
		matL.xzE = 0;
		matL.yxE = 0;
		matL.yyE = 0;
		matL.yzE = 0;
		matL.zxE = 0;
		matL.zyE = 0;
		matL.zzE = 0;
		matL.bbpE = 0;
	}
	else
	{
		int32 xShiftL = bts_maxAbsIntLog2Of3( xxA, xyA, xzA ) - 13;
		int32 yShiftL = bts_maxAbsIntLog2Of3( yxA, yyA, yzA ) - 13;
		int32 zShiftL = bts_maxAbsIntLog2Of3( zxA, zyA, zzA ) - 13;

		int32 shiftL = bbs_max( bbs_max( xShiftL, yShiftL ), zShiftL );

		if( shiftL > 0 )
		{
			int32 sh1L = shiftL - 1;
			matL.xxE = ( ( xxA >> sh1L ) + 1 ) >> 1;
			matL.xyE = ( ( xyA >> sh1L ) + 1 ) >> 1;
			matL.xzE = ( ( xzA >> sh1L ) + 1 ) >> 1;
			matL.yxE = ( ( yxA >> sh1L ) + 1 ) >> 1;
			matL.yyE = ( ( yyA >> sh1L ) + 1 ) >> 1;
			matL.yzE = ( ( yzA >> sh1L ) + 1 ) >> 1;
			matL.zxE = ( ( zxA >> sh1L ) + 1 ) >> 1;
			matL.zyE = ( ( zyA >> sh1L ) + 1 ) >> 1;
			matL.zzE = ( ( zzA >> sh1L ) + 1 ) >> 1;
		}
		else
		{
			matL.xxE = xxA << -shiftL;
			matL.xyE = xyA << -shiftL;
			matL.xzE = xzA << -shiftL;
			matL.yxE = yxA << -shiftL;
			matL.yyE = yyA << -shiftL;
			matL.yzE = yzA << -shiftL;
			matL.zxE = zxA << -shiftL;
			matL.zyE = zyA << -shiftL;
			matL.zzE = zzA << -shiftL;
		}

		matL.bbpE = bbpA - shiftL;
	}
	return matL;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Mat3D_scale( struct bts_Flt16Mat3D* ptrA, int32 scaleA, int32 scaleBbpA )
{
	/* fit scale in 15 bit */
	uint32 scaleExpL = bts_absIntLog2( scaleA );
	if( scaleExpL > 14 )
	{
		int32 shiftL = scaleExpL - 14;
		scaleA = ( ( scaleA >> ( shiftL - 1 ) ) + 1 ) >> 1;
		scaleBbpA -= shiftL;
	}

	*ptrA = bts_Flt16Mat3D_create32( ptrA->xxE * scaleA, ptrA->xyE * scaleA, ptrA->xzE * scaleA,
									 ptrA->yxE * scaleA, ptrA->yyE * scaleA, ptrA->yzE * scaleA,
									 ptrA->zxE * scaleA, ptrA->zyE * scaleA, ptrA->zzE * scaleA,
									 ptrA->bbpE + scaleBbpA );
}

/* ------------------------------------------------------------------------- */
#ifndef HW_EE /* causes internal compiler error in ee-gcc */
struct bts_Int16Vec3D bts_Flt16Mat3D_map( const struct bts_Flt16Mat3D* matPtrA, 
								          const struct bts_Int16Vec3D* vecPtrA )
{
	struct bts_Int16Vec3D vecL;

	int32 xL = ( int32 ) matPtrA->xxE * vecPtrA->xE + ( int32 ) matPtrA->xyE * vecPtrA->yE + ( int32 ) matPtrA->xzE * vecPtrA->zE;
	int32 yL = ( int32 ) matPtrA->yxE * vecPtrA->xE + ( int32 ) matPtrA->yyE * vecPtrA->yE + ( int32 ) matPtrA->yzE * vecPtrA->zE;
	int32 zL = ( int32 ) matPtrA->zxE * vecPtrA->xE + ( int32 ) matPtrA->zyE * vecPtrA->yE + ( int32 ) matPtrA->zzE * vecPtrA->zE;

	if( matPtrA->bbpE > 0 )
	{
		int32 sh1L = matPtrA->bbpE - 1;
		vecL.xE = ( ( xL >> sh1L ) + 1 ) >> 1;
		vecL.yE = ( ( yL >> sh1L ) + 1 ) >> 1;
		vecL.zE = ( ( zL >> sh1L ) + 1 ) >> 1;
	}
	else
	{
		/* not overflow safe */
		vecL.xE = xL << -matPtrA->bbpE;
		vecL.yE = yL << -matPtrA->bbpE;
		vecL.zE = zL << -matPtrA->bbpE;
	}

	return vecL;
}
#endif
/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec3D bts_Flt16Mat3D_mapFlt( const struct bts_Flt16Mat3D* matPtrA, 
								             const struct bts_Flt16Vec3D* vecPtrA )
{
	/* avoids overflow summing intermediate products */
	int32 xL = ( ( ( ( int32 ) matPtrA->xxE * vecPtrA->xE + 1 ) >> 1 ) +
				 ( ( ( int32 ) matPtrA->xyE * vecPtrA->yE + 1 ) >> 1 ) +
				 ( ( ( int32 ) matPtrA->xzE * vecPtrA->zE + 1 ) >> 1 ) );

	int32 yL = ( ( ( ( int32 ) matPtrA->yxE * vecPtrA->xE + 1 ) >> 1 ) +
				 ( ( ( int32 ) matPtrA->yyE * vecPtrA->yE + 1 ) >> 1 ) +
				 ( ( ( int32 ) matPtrA->yzE * vecPtrA->zE + 1 ) >> 1 ) );

	int32 zL = ( ( ( ( int32 ) matPtrA->zxE * vecPtrA->xE + 1 ) >> 1 ) +
				 ( ( ( int32 ) matPtrA->zyE * vecPtrA->yE + 1 ) >> 1 ) +
				 ( ( ( int32 ) matPtrA->zzE * vecPtrA->zE + 1 ) >> 1 ) );

	
	return bts_Flt16Vec3D_create32( xL, yL, zL, vecPtrA->bbpE + matPtrA->bbpE - 1 );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat3D bts_Flt16Mat3D_mul( const struct bts_Flt16Mat3D* mat1PtrA, 
								          const struct bts_Flt16Mat3D* mat2PtrA )
{
	/* avoids overflow summing intermediate products */
	return bts_Flt16Mat3D_create32(

		( ( ( int32 ) mat1PtrA->xxE * mat2PtrA->xxE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->xyE * mat2PtrA->yxE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->xzE * mat2PtrA->zxE + 1 ) >> 1 ),
		( ( ( int32 ) mat1PtrA->xxE * mat2PtrA->xyE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->xyE * mat2PtrA->yyE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->xzE * mat2PtrA->zyE + 1 ) >> 1 ),
		( ( ( int32 ) mat1PtrA->xxE * mat2PtrA->xzE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->xyE * mat2PtrA->yzE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->xzE * mat2PtrA->zzE + 1 ) >> 1 ),

		( ( ( int32 ) mat1PtrA->yxE * mat2PtrA->xxE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->yyE * mat2PtrA->yxE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->yzE * mat2PtrA->zxE + 1 ) >> 1 ),
		( ( ( int32 ) mat1PtrA->yxE * mat2PtrA->xyE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->yyE * mat2PtrA->yyE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->yzE * mat2PtrA->zyE + 1 ) >> 1 ),
		( ( ( int32 ) mat1PtrA->yxE * mat2PtrA->xzE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->yyE * mat2PtrA->yzE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->yzE * mat2PtrA->zzE + 1 ) >> 1 ),

		( ( ( int32 ) mat1PtrA->zxE * mat2PtrA->xxE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->zyE * mat2PtrA->yxE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->zzE * mat2PtrA->zxE + 1 ) >> 1 ),
		( ( ( int32 ) mat1PtrA->zxE * mat2PtrA->xyE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->zyE * mat2PtrA->yyE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->zzE * mat2PtrA->zyE + 1 ) >> 1 ),
		( ( ( int32 ) mat1PtrA->zxE * mat2PtrA->xzE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->zyE * mat2PtrA->yzE + 1 ) >> 1 ) + ( ( ( int32 ) mat1PtrA->zzE * mat2PtrA->zzE + 1 ) >> 1 ),

		mat1PtrA->bbpE + mat2PtrA->bbpE - 1 );
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Mat3D* bts_Flt16Mat3D_mulTo( struct bts_Flt16Mat3D* mat1PtrA, 
				                             const struct bts_Flt16Mat3D* mat2PtrA )
{
	*mat1PtrA = bts_Flt16Mat3D_mul( mat1PtrA, mat2PtrA );
	return mat1PtrA;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

