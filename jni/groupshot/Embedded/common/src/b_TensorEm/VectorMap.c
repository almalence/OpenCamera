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
#include "b_TensorEm/VectorMap.h"

#include "b_TensorEm/MapSequence.h"
#include "b_TensorEm/Normalizer.h"
#include "b_TensorEm/Alt.h"
#include "b_TensorEm/Mat.h"
#include "b_TensorEm/SubVecMap.h"

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

void bts_VectorMap_init( struct bbs_Context* cpA,
					     struct bts_VectorMap* ptrA )
{
	ptrA->typeE = 0;
	ptrA->vpMapE = NULL;
}

/* ------------------------------------------------------------------------- */

void bts_VectorMap_exit( struct bbs_Context* cpA,
					   struct bts_VectorMap* ptrA )
{
	ptrA->typeE = 0;
	ptrA->vpMapE = NULL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_VectorMap_copy( struct bbs_Context* cpA,
					     struct bts_VectorMap* ptrA, 
					     const struct bts_VectorMap* srcPtrA )
{
	ptrA->typeE  = srcPtrA->typeE;
	ptrA->vpMapE = srcPtrA->vpMapE;
}

/* ------------------------------------------------------------------------- */

flag bts_VectorMap_equal( struct bbs_Context* cpA,
					    const struct bts_VectorMap* ptrA, 
						const struct bts_VectorMap* srcPtrA )
{

	if( ptrA->typeE	 != srcPtrA->typeE ) return FALSE;
	if( ptrA->vpMapE != srcPtrA->vpMapE ) return FALSE;
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
	
uint32 bts_VectorMap_memSize( struct bbs_Context* cpA,
						    const struct bts_VectorMap* ptrA )
{
	uint32 memSizeL = 0;
	memSizeL += bbs_SIZEOF16( ptrA->typeE );
	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_VectorMap_memWrite( struct bbs_Context* cpA,
							 const struct bts_VectorMap* ptrA, 
							 uint16* memPtrA )
{
	uint32 memSizeL = bts_VectorMap_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &ptrA->typeE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bts_VectorMap_memRead( struct bbs_Context* cpA,
						    struct bts_VectorMap* ptrA, 
							const uint16* memPtrA )
{
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &ptrA->typeE, memPtrA );
	return bts_VectorMap_memSize( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

void bts_vectorMapInit( struct bbs_Context* cpA,
					    struct bts_VectorMap* ptrA,
					    enum bts_VectorMapType typeA )
{
	switch( typeA )
	{
		case bts_VM_MAP_SEQUENCE:	bts_MapSequence_init( cpA,	( struct bts_MapSequence* )ptrA ); return; 
		case bts_VM_NORMALIZER:		bts_Normalizer_init( cpA,	( struct bts_Normalizer* )ptrA ); return; 
		case bts_VM_MAT:			bts_Mat_init( cpA,			( struct bts_Mat* )ptrA ); return; 
		case bts_VM_ALT:			bts_Alt_init( cpA,			( struct bts_Alt* )ptrA ); return; 
		case bts_VM_SUB_VEC_MAP:	bts_SubVecMap_init( cpA,	( struct bts_SubVecMap* )ptrA ); return; 
			
		default: bbs_ERROR0( "bts_vectorMapInit: invalid type" );
	}
}

/* ------------------------------------------------------------------------- */

void bts_vectorMapExit( struct bbs_Context* cpA, 
					    struct bts_VectorMap* ptrA )
{
	switch( ptrA->typeE )
	{
		case bts_VM_MAP_SEQUENCE:	bts_MapSequence_exit( cpA,	( struct bts_MapSequence* )ptrA ); return;
		case bts_VM_NORMALIZER:		bts_Normalizer_exit( cpA,	( struct bts_Normalizer* )ptrA ); return;
		case bts_VM_MAT:			bts_Mat_exit( cpA,			( struct bts_Mat* )ptrA ); return; 
		case bts_VM_ALT:			bts_Alt_exit( cpA,			( struct bts_Alt* )ptrA ); return; 
		case bts_VM_SUB_VEC_MAP:	bts_SubVecMap_exit( cpA,	( struct bts_SubVecMap* )ptrA ); return; 

		default: bbs_ERROR0( "bts_vectorMapExit: invalid type" );
	}
}

/* ------------------------------------------------------------------------- */

uint32 bts_vectorMapMemSize( struct bbs_Context* cpA, 
						     const struct bts_VectorMap* ptrA )
{
	switch( ptrA->typeE )
	{
		case bts_VM_MAP_SEQUENCE:	return bts_MapSequence_memSize( cpA,	( struct bts_MapSequence* )ptrA );
		case bts_VM_NORMALIZER:		return bts_Normalizer_memSize( cpA,		( struct bts_Normalizer* )ptrA );
		case bts_VM_MAT:			return bts_Mat_memSize( cpA,			( struct bts_Mat* )ptrA );
		case bts_VM_ALT:			return bts_Alt_memSize( cpA,			( struct bts_Alt* )ptrA );
		case bts_VM_SUB_VEC_MAP:	return bts_SubVecMap_memSize( cpA,		( struct bts_SubVecMap* )ptrA );

		default: bbs_ERROR0( "bts_vectorMapExit: invalid type" );
	}
	return 0;
}

/* ------------------------------------------------------------------------- */

uint32 bts_vectorMapMemWrite( struct bbs_Context* cpA, 
						      const struct bts_VectorMap* ptrA, uint16* memPtrA )
{
	switch( ptrA->typeE )
	{
		case bts_VM_MAP_SEQUENCE:	return bts_MapSequence_memWrite( cpA,	( struct bts_MapSequence* )ptrA, memPtrA  );
		case bts_VM_NORMALIZER:		return bts_Normalizer_memWrite( cpA,	( struct bts_Normalizer* )ptrA, memPtrA  );
		case bts_VM_MAT:			return bts_Mat_memWrite( cpA,			( struct bts_Mat* )ptrA, memPtrA  );
		case bts_VM_ALT:			return bts_Alt_memWrite( cpA,			( struct bts_Alt* )ptrA, memPtrA  );
		case bts_VM_SUB_VEC_MAP:	return bts_SubVecMap_memWrite( cpA,		( struct bts_SubVecMap* )ptrA, memPtrA  );

		default: bbs_ERROR0( "bts_vectorMapMemWrite: invalid type" );
	}
	return 0;
}

/* ------------------------------------------------------------------------- */

uint32 bts_vectorMapMemRead( struct bbs_Context* cpA,
						     struct bts_VectorMap* ptrA, 
						     const uint16* memPtrA,
						     struct bbs_MemTbl* mtpA )
{
	switch( ptrA->typeE )
	{
		case bts_VM_MAP_SEQUENCE:	return bts_MapSequence_memRead( cpA,	( struct bts_MapSequence* )ptrA, memPtrA, mtpA );
		case bts_VM_NORMALIZER:		return bts_Normalizer_memRead( cpA,		( struct bts_Normalizer* )ptrA, memPtrA, mtpA );
		case bts_VM_MAT:			return bts_Mat_memRead( cpA,			( struct bts_Mat* )ptrA, memPtrA, mtpA );
		case bts_VM_ALT:			return bts_Alt_memRead( cpA,			( struct bts_Alt* )ptrA, memPtrA, mtpA );
		case bts_VM_SUB_VEC_MAP:	return bts_SubVecMap_memRead( cpA,		( struct bts_SubVecMap* )ptrA, memPtrA, mtpA );

		default: bbs_ERROR0( "bts_vectorMapMemRead: invalid type" );
	}
	return 0;
}

/* ------------------------------------------------------------------------- */

uint32 bts_vectorMapSizeOf16( struct bbs_Context* cpA, enum bts_VectorMapType typeA )
{
	switch( typeA )
	{
		case bts_VM_MAP_SEQUENCE:	return bbs_SIZEOF16( struct bts_MapSequence );
		case bts_VM_NORMALIZER:		return bbs_SIZEOF16( struct bts_Normalizer );
		case bts_VM_MAT:			return bbs_SIZEOF16( struct bts_Mat );
		case bts_VM_ALT:			return bbs_SIZEOF16( struct bts_Alt );
		case bts_VM_SUB_VEC_MAP:	return bbs_SIZEOF16( struct bts_SubVecMap );

		default: bbs_ERROR0( "bts_vectorMapSizeOf16: invalid type" );
	}
	return 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

