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

#include "b_TensorEm/Int16Mat2D.h"
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
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

struct bts_Int16Mat2D bts_Int16Mat2D_createIdentity()
{
	struct bts_Int16Mat2D matL = { 1 << 14, 0, 0, 1 << 14, 14 };
	return matL;
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Mat2D bts_Int16Mat2D_createRotation( phase16 angleA )
{
	int16 cL = bbs_cos16( angleA );
	int16 sL = bbs_sin16( angleA );
	struct bts_Int16Mat2D matL;
	matL.xxE =  cL;
	matL.xyE = -sL;
	matL.yxE =  sL;
	matL.yyE =  cL;
	matL.bbpE = 14;
	return matL; 
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Mat2D bts_Int16Mat2D_createRigid( phase16 angleA, struct flt16 scaleA )
{
	struct bts_Int16Mat2D matL = bts_Int16Mat2D_createRotation( angleA );
	bts_Int16Mat2D_scale( &matL, scaleA );
	return matL;
}

/* ------------------------------------------------------------------------- */

void bts_Int16Mat2D_scale( struct bts_Int16Mat2D* ptrA, struct flt16 scaleA )
{
	int32 xxL = ( int32 ) ptrA->xxE * scaleA.valE;
	int32 xyL = ( int32 ) ptrA->xyE * scaleA.valE;
	int32 yxL = ( int32 ) ptrA->yxE * scaleA.valE;
	int32 yyL = ( int32 ) ptrA->yyE * scaleA.valE;

	uint32 shiftL = bts_maxAbsIntLog2Of4( xxL, xyL, yxL, yyL ) - 15;

	ptrA->xxE = xxL >> shiftL;
	ptrA->xyE = xyL >> shiftL;
	ptrA->yxE = yxL >> shiftL;
	ptrA->yyE = yyL >> shiftL;

	ptrA->bbpE += scaleA.bbpE - shiftL;
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Vec2D bts_Int16Mat2D_map( const struct bts_Int16Mat2D* matPtrA, 
								          const struct bts_Int16Vec2D* vecPtrA )
{
	struct bts_Int16Vec2D vecL;
	vecL.xE = ( ( int32 ) matPtrA->xxE * vecPtrA->xE + ( int32 ) matPtrA->xyE * vecPtrA->yE ) >> matPtrA->bbpE;
	vecL.yE = ( ( int32 ) matPtrA->yxE * vecPtrA->xE + ( int32 ) matPtrA->yyE * vecPtrA->yE ) >> matPtrA->bbpE;
	return vecL;
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Mat2D bts_Int16Mat2D_mul( const struct bts_Int16Mat2D* mat1PtrA, 
								          const struct bts_Int16Mat2D* mat2PtrA )
{
	struct bts_Int16Mat2D matL;
	int32 xxL = ( int32 ) mat1PtrA->xxE * mat2PtrA->xxE + ( int32 ) mat1PtrA->xyE * mat2PtrA->yxE;
	int32 xyL = ( int32 ) mat1PtrA->xxE * mat2PtrA->xyE + ( int32 ) mat1PtrA->xyE * mat2PtrA->yyE;
	int32 yxL = ( int32 ) mat1PtrA->yxE * mat2PtrA->xxE + ( int32 ) mat1PtrA->yyE * mat2PtrA->yxE;
	int32 yyL = ( int32 ) mat1PtrA->yxE * mat2PtrA->xyE + ( int32 ) mat1PtrA->yyE * mat2PtrA->yyE;

	uint32 shiftL = bts_maxAbsIntLog2Of4( xxL, xyL, yxL, yyL ) - 15;

	matL.xxE = xxL >> shiftL;
	matL.xyE = xyL >> shiftL;
	matL.yxE = yxL >> shiftL;
	matL.yyE = yyL >> shiftL;

	matL.bbpE = mat1PtrA->bbpE + mat2PtrA->bbpE - shiftL;

	return matL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


