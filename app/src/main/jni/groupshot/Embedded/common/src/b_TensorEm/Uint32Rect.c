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

#include "b_TensorEm/Uint32Rect.h"
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
	
uint32 bts_Uint32Rect_memSize( struct bbs_Context* cpA,
							   const struct bts_Uint32Rect *ptrA )
{
	return bbs_SIZEOF16( struct bts_Uint32Rect );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Uint32Rect_memWrite( struct bbs_Context* cpA,
							    const struct bts_Uint32Rect* ptrA, 
								uint16* memPtrA )
{
	memPtrA += bbs_memWrite32( &ptrA->x1E, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->y1E, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->x2E, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->y2E, memPtrA );
	return bbs_SIZEOF16( *ptrA );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Uint32Rect_memRead( struct bbs_Context* cpA,
							   struct bts_Uint32Rect* ptrA, 
							   const uint16* memPtrA )
{
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &ptrA->x1E, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->y1E, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->x2E, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->y2E, memPtrA );
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


