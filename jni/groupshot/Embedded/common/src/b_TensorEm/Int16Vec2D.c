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

#include "b_TensorEm/Int16Vec2D.h"
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

void bts_Int16Vec2D_init( struct bts_Int16Vec2D* ptrA )
{
	ptrA->xE = 0;
	ptrA->yE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_Int16Vec2D_exit( struct bts_Int16Vec2D* ptrA )
{
	ptrA->xE = 0;
	ptrA->yE = 0;
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
	
uint32 bts_Int16Vec2D_memSize( struct bbs_Context* cpA,
							   const struct bts_Int16Vec2D *ptrA )
{
	return bbs_SIZEOF16( struct bts_Int16Vec2D );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int16Vec2D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Int16Vec2D* ptrA, 
								uint16* memPtrA )
{
	memPtrA += bbs_memWrite16( &ptrA->xE, memPtrA );
	memPtrA += bbs_memWrite16( &ptrA->yE, memPtrA );
	return bbs_SIZEOF16( *ptrA );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int16Vec2D_memRead( struct bbs_Context* cpA,
							   struct bts_Int16Vec2D* ptrA, 
							   const uint16* memPtrA )
{
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead16( &ptrA->xE, memPtrA );
	memPtrA += bbs_memRead16( &ptrA->yE, memPtrA );
	return bbs_SIZEOF16( *ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

int32 bts_Int16Vec2D_dotPrd( const struct bts_Int16Vec2D* vec1PtrA, 
							 const struct bts_Int16Vec2D* vec2PtrA )
{
	return ( int32 ) vec1PtrA->xE * vec2PtrA->xE + ( int32 ) vec1PtrA->yE * vec2PtrA->yE;
}
	
/* ------------------------------------------------------------------------- */

uint32 bts_Int16Vec2D_norm2( const struct bts_Int16Vec2D* ptrA )
{
	return ( int32 ) ptrA->xE * ptrA->xE + ( int32 ) ptrA->yE * ptrA->yE;
}

/* ------------------------------------------------------------------------- */

uint16 bts_Int16Vec2D_norm( const struct bts_Int16Vec2D* ptrA )
{
	return bbs_sqrt32( ( int32 ) ptrA->xE * ptrA->xE + ( int32 ) ptrA->yE * ptrA->yE );
}

/* ------------------------------------------------------------------------- */

void bts_Int16Vec2D_normalize( struct bts_Int16Vec2D* ptrA, int32 bbpA )
{
	int32 normL = bbs_sqrt32( ( int32 ) ptrA->xE * ptrA->xE + ( int32 ) ptrA->yE * ptrA->yE );
	int32 xL = ( ( int32 ) ptrA->xE << 16 ) / normL;
	int32 yL = ( ( int32 ) ptrA->yE << 16 ) / normL;
	ptrA->xE = xL >> ( 16 - bbpA );
	ptrA->yE = yL >> ( 16 - bbpA );
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Vec2D bts_Int16Vec2D_normalized( const struct bts_Int16Vec2D* ptrA, int32 bbpA )
{
	struct bts_Int16Vec2D vecL = *ptrA;
	bts_Int16Vec2D_normalize( &vecL, bbpA );
	return vecL;
}

/* ------------------------------------------------------------------------- */

phase16 bts_Int16Vec2D_angle( const struct bts_Int16Vec2D* vecPtrA )
{
	return bbs_phase16( vecPtrA->xE, vecPtrA->yE );
}

/* ------------------------------------------------------------------------- */

phase16 bts_Int16Vec2D_enclosedAngle( const struct bts_Int16Vec2D* vec1PtrA, 
									  const struct bts_Int16Vec2D* vec2PtrA )
{
	int32 xL = ( int32 ) vec1PtrA->xE * vec2PtrA->xE + ( int32 ) vec1PtrA->yE * vec2PtrA->yE;
	int32 yL = ( int32 ) vec1PtrA->yE * vec2PtrA->xE - ( int32 ) vec1PtrA->xE * vec2PtrA->yE;
	return bbs_phase16( xL, yL );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


