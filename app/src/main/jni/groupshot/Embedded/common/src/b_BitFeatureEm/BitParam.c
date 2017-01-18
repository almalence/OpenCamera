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
#include "b_BitFeatureEm/BitParam.h"

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

void bbf_BitParam_init( struct bbs_Context* cpA,
					    struct bbf_BitParam* ptrA )
{
	ptrA->innerRadiusE = 0;
	ptrA->outerRadiusE = 0;
}

/* ------------------------------------------------------------------------- */

void bbf_BitParam_exit( struct bbs_Context* cpA,
						    struct bbf_BitParam* ptrA )
{
	ptrA->innerRadiusE = 0;
	ptrA->outerRadiusE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_BitParam_copy( struct bbs_Context* cpA,
					    struct bbf_BitParam* ptrA, 
					    const struct bbf_BitParam* srcPtrA )
{
	ptrA->innerRadiusE = srcPtrA->innerRadiusE;
	ptrA->outerRadiusE = srcPtrA->outerRadiusE;
}

/* ------------------------------------------------------------------------- */

flag bbf_BitParam_equal( struct bbs_Context* cpA,
					     const struct bbf_BitParam* ptrA, 
					     const struct bbf_BitParam* srcPtrA )
{
	if( ptrA->innerRadiusE != srcPtrA->innerRadiusE ) return FALSE;
	if( ptrA->outerRadiusE != srcPtrA->outerRadiusE ) return FALSE;
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
	
uint32 bbf_BitParam_memSize( struct bbs_Context* cpA,
						     const struct bbf_BitParam* ptrA )
{
	uint32 memSizeL = 0; 
	memSizeL += bbs_SIZEOF16( ptrA->innerRadiusE );
	memSizeL += bbs_SIZEOF16( ptrA->outerRadiusE );
	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_BitParam_memWrite( struct bbs_Context* cpA,
						      const struct bbf_BitParam* ptrA, 
							  uint16* memPtrA )
{
	memPtrA += bbs_memWrite32( &ptrA->innerRadiusE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->outerRadiusE, memPtrA );
	return bbf_BitParam_memSize( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */

uint32 bbf_BitParam_memRead( struct bbs_Context* cpA,
						     struct bbf_BitParam* ptrA, 
						     const uint16* memPtrA )
{
	memPtrA += bbs_memRead32( &ptrA->innerRadiusE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->outerRadiusE, memPtrA );
	return bbf_BitParam_memSize( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

/* ========================================================================= */

