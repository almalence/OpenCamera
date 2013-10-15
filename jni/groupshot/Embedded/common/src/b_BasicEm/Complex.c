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

#include "b_BasicEm/Complex.h"
#include "b_BasicEm/APh.h"
#include "b_BasicEm/Functions.h"
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

flag bbs_Complex_equal( struct bbs_Complex compl1A, struct bbs_Complex compl2A )
{
	return ( compl1A.realE == compl2A.realE ) && ( compl1A.imagE == compl2A.imagE );
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
	
uint32 bbs_Complex_memSize( struct bbs_Context* cpA,
						    struct bbs_Complex complA )
{
	return bbs_SIZEOF16( complA.realE ) + bbs_SIZEOF16( complA.imagE );
}

/* ------------------------------------------------------------------------- */
	
uint32 bbs_Complex_memWrite( struct bbs_Context* cpA,
							 const struct bbs_Complex* ptrA, 
							 uint16* memPtrA )
{
	memPtrA += bbs_memWrite16( &ptrA->realE, memPtrA );
	memPtrA += bbs_memWrite16( &ptrA->imagE, memPtrA );
	return bbs_Complex_memSize( cpA, *ptrA );
}

/* ------------------------------------------------------------------------- */
	
uint32 bbs_Complex_memRead( struct bbs_Context* cpA,
						    struct bbs_Complex* ptrA, 
							const uint16* memPtrA )
{
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead16( &ptrA->realE, memPtrA );
	memPtrA += bbs_memRead16( &ptrA->imagE, memPtrA );
	return bbs_Complex_memSize( cpA, *ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

struct bbs_Complex bbs_Complex_conj( struct bbs_Complex complA )
{
	struct bbs_Complex resultL;
	resultL.imagE = - complA.imagE;
	resultL.realE = complA.realE;
	return resultL;
}

/* ------------------------------------------------------------------------- */

uint32 bbs_Complex_abs2( struct bbs_Complex complA )
{
	return ( int32 ) complA.realE * complA.realE + 
		   ( int32 ) complA.imagE * complA.imagE;
}

/* ------------------------------------------------------------------------- */

uint16 bbs_Complex_abs( struct bbs_Complex complA )
{
	return bbs_sqrt32( bbs_Complex_abs2( complA ) );
}

/* ------------------------------------------------------------------------- */

phase16 bbs_Complex_phase( struct bbs_Complex complA )
{
	int32 realL, imagL;
	realL = complA.realE;
	imagL = complA.imagE;

	return bbs_phase16( realL, imagL );
}

/* ------------------------------------------------------------------------- */

void bbs_Complex_importAPh( struct bbs_Complex* dstPtrA, const struct bbs_APh* srcPtrA )
{
	dstPtrA->realE = ( ( bbs_cos32( srcPtrA->phaseE ) >> 8 ) * srcPtrA->absE ) >> 16;
	dstPtrA->imagE = ( ( bbs_sin32( srcPtrA->phaseE ) >> 8 ) * srcPtrA->absE ) >> 16;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


