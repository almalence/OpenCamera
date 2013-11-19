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

#include "b_TensorEm/Int16Vec3D.h"
#include "b_BasicEm/Functions.h"
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
	
uint32 bts_Int16Vec3D_memSize( struct bbs_Context* cpA,
							   const struct bts_Int16Vec3D *ptrA )
{
	return bbs_SIZEOF16( struct bts_Int16Vec3D );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int16Vec3D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Int16Vec3D* ptrA, 
								uint16* memPtrA )
{
	memPtrA += bbs_memWrite16( &ptrA->xE, memPtrA );
	memPtrA += bbs_memWrite16( &ptrA->yE, memPtrA );
	memPtrA += bbs_memWrite16( &ptrA->zE, memPtrA );
	return bbs_SIZEOF16( *ptrA );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int16Vec3D_memRead( struct bbs_Context* cpA,
							   struct bts_Int16Vec3D* ptrA, 
							   const uint16* memPtrA )
{
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead16( &ptrA->xE, memPtrA );
	memPtrA += bbs_memRead16( &ptrA->yE, memPtrA );
	memPtrA += bbs_memRead16( &ptrA->zE, memPtrA );
	return bbs_SIZEOF16( *ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bts_Int16Vec3D_norm2( const struct bts_Int16Vec3D* ptrA )
{
	return ( int32 ) ptrA->xE * ptrA->xE +
		   ( int32 ) ptrA->yE * ptrA->yE +
		   ( int32 ) ptrA->zE * ptrA->zE;
}

/* ------------------------------------------------------------------------- */

uint16 bts_Int16Vec3D_norm( const struct bts_Int16Vec3D* ptrA )
{
	return bbs_sqrt32( ( int32 ) ptrA->xE * ptrA->xE +
					   ( int32 ) ptrA->yE * ptrA->yE +
					   ( int32 ) ptrA->zE * ptrA->zE );
}

/* ------------------------------------------------------------------------- */

void bts_Int16Vec3D_normalize( struct bts_Int16Vec3D* ptrA, int32 bbpA )
{
	int32 normL = bbs_sqrt32( ( int32 ) ptrA->xE * ptrA->xE +
							  ( int32 ) ptrA->yE * ptrA->yE +
							  ( int32 ) ptrA->zE * ptrA->zE );

	int32 xL = ( ( int32 )ptrA->xE << 16 ) / normL;
	int32 yL = ( ( int32 )ptrA->yE << 16 ) / normL;
	int32 zL = ( ( int32 )ptrA->zE << 16 ) / normL;
	ptrA->xE = xL >> ( 16 - bbpA );
	ptrA->yE = yL >> ( 16 - bbpA );
	ptrA->zE = zL >> ( 16 - bbpA );
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Vec3D bts_Int16Vec3D_normalized( const struct bts_Int16Vec3D* ptrA, int32 bbpA )
{
	struct bts_Int16Vec3D vecL = *ptrA;
	bts_Int16Vec3D_normalize( &vecL, bbpA );
	return vecL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

