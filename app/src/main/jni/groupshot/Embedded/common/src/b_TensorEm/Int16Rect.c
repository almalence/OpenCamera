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

#include "b_TensorEm/Int16Rect.h"
#include "b_BasicEm/Math.h"
#include "b_BasicEm/Functions.h"
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

void bts_Int16Rect_init( struct bbs_Context* cpA, struct bts_Int16Rect* ptrA )
{
	ptrA->x1E = 0;
	ptrA->y1E = 0;
	ptrA->x2E = 0;
	ptrA->y2E = 0;
}

void bts_Int16Rect_exit( struct bbs_Context* cpA, struct bts_Int16Rect* ptrA )
{
	ptrA->x1E = 0;
	ptrA->y1E = 0;
	ptrA->x2E = 0;
	ptrA->y2E = 0;
}

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
	
struct bts_Int16Rect bts_Int16Rect_create( int16 x1A, int16 y1A, int16 x2A, int16 y2A )
{
	struct bts_Int16Rect rectL;
	rectL.x1E = x1A;
	rectL.y1E = y1A;
	rectL.x2E = x2A;
	rectL.y2E = y2A;
	return rectL;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int16Rect_memSize( struct bbs_Context* cpA,
							  const struct bts_Int16Rect *ptrA )
{
	return bbs_SIZEOF16( struct bts_Int16Rect );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int16Rect_memWrite( struct bbs_Context* cpA,
							   const struct bts_Int16Rect* ptrA, 
							   uint16* memPtrA )
{
	memPtrA += bbs_memWrite16( &ptrA->x1E, memPtrA );
	memPtrA += bbs_memWrite16( &ptrA->y1E, memPtrA );
	memPtrA += bbs_memWrite16( &ptrA->x2E, memPtrA );
	memPtrA += bbs_memWrite16( &ptrA->y2E, memPtrA );
	return bbs_SIZEOF16( *ptrA );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int16Rect_memRead( struct bbs_Context* cpA,
							  struct bts_Int16Rect* ptrA, 
							  const uint16* memPtrA )
{
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead16( &ptrA->x1E, memPtrA );
	memPtrA += bbs_memRead16( &ptrA->y1E, memPtrA );
	memPtrA += bbs_memRead16( &ptrA->x2E, memPtrA );
	memPtrA += bbs_memRead16( &ptrA->y2E, memPtrA );
	return bbs_SIZEOF16( *ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

/* ========================================================================= */


