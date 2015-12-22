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

#include "b_BasicEm/Functions.h"
#include "b_BasicEm/APh.h"
#include "b_BasicEm/Complex.h"
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

flag bbs_APh_equal( struct bbs_APh aph1A, 
					struct bbs_APh aph2A )
{
	return ( aph1A.absE == aph2A.absE ) && ( aph1A.phaseE == aph2A.phaseE );
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
	
uint32 bbs_APh_memSize( struct bbs_Context* cpA,
					    struct bbs_APh aPhA )
{
	return bbs_SIZEOF16( aPhA.absE ) + bbs_SIZEOF16( aPhA.phaseE );
}

/* ------------------------------------------------------------------------- */
	
uint32 bbs_APh_memWrite( struct bbs_Context* cpA,
						 const struct bbs_APh* ptrA, 
						 uint16* memPtrA )
{
	memPtrA += bbs_memWrite16( &ptrA->absE, memPtrA );
	memPtrA += bbs_memWrite16( &ptrA->phaseE, memPtrA );
	return bbs_APh_memSize( cpA, *ptrA );
}

/* ------------------------------------------------------------------------- */
	
uint32 bbs_APh_memRead( struct bbs_Context* cpA,
					    struct bbs_APh* ptrA, 
						const uint16* memPtrA )
{
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead16( &ptrA->absE, memPtrA );
	memPtrA += bbs_memRead16( &ptrA->phaseE, memPtrA );
	return bbs_APh_memSize( cpA, *ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

struct bbs_APh bbs_APh_conj( const struct bbs_APh aPhA )
{
	struct bbs_APh aphL;
	aphL.absE = aPhA.absE;
	aphL.phaseE = - aPhA.phaseE;
	return aphL;
}

/* ------------------------------------------------------------------------- */

void bbs_APh_importComplex( struct bbs_APh* dstPtrA, 
							const struct bbs_Complex* srcPtrA )
{
	dstPtrA->absE = bbs_sqrt32( ( int32 ) srcPtrA->realE * srcPtrA->realE + ( int32 ) srcPtrA->imagE * srcPtrA->imagE );
	dstPtrA->phaseE = bbs_phase16( srcPtrA->realE, srcPtrA->imagE );
}

/* ========================================================================= */


