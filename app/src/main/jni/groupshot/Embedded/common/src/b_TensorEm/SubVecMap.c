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
#include "b_BasicEm/Math.h"
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

void bts_SubVecMap_init( struct bbs_Context* cpA,
					      struct bts_SubVecMap* ptrA )
{
	bts_VectorMap_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bts_VM_SUB_VEC_MAP;
	ptrA->baseE.vpMapE = bts_SubVecMap_map;
	ptrA->offsetE = 0;
	ptrA->sizeE = -1;
}

/* ------------------------------------------------------------------------- */

void bts_SubVecMap_exit( struct bbs_Context* cpA,
					      struct bts_SubVecMap* ptrA )
{
	bts_VectorMap_exit( cpA, &ptrA->baseE );
	ptrA->offsetE = 0;
	ptrA->sizeE = -1;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_SubVecMap_copy( struct bbs_Context* cpA,
						 struct bts_SubVecMap* ptrA, 
					     const struct bts_SubVecMap* srcPtrA )
{
	ptrA->offsetE = srcPtrA->offsetE;
	ptrA->sizeE = srcPtrA->sizeE;
}

/* ------------------------------------------------------------------------- */

flag bts_SubVecMap_equal( struct bbs_Context* cpA,
						   const struct bts_SubVecMap* ptrA, 
						   const struct bts_SubVecMap* srcPtrA )
{
	if( ptrA->offsetE != srcPtrA->offsetE ) return FALSE;
	if( ptrA->sizeE   != srcPtrA->sizeE   ) return FALSE;
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
	
uint32 bts_SubVecMap_memSize( struct bbs_Context* cpA,
							   const struct bts_SubVecMap* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */
	memSizeL += bts_VectorMap_memSize( cpA, &ptrA->baseE );
	memSizeL += bbs_SIZEOF16( ptrA->offsetE );
	memSizeL += bbs_SIZEOF16( ptrA->sizeE );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_SubVecMap_memWrite( struct bbs_Context* cpA,
								const struct bts_SubVecMap* ptrA, 
								uint16* memPtrA )
{
	uint32 memSizeL = bts_SubVecMap_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_SUB_VEC_MAP_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->offsetE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->sizeE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bts_SubVecMap_memRead( struct bbs_Context* cpA,
							   struct bts_SubVecMap* ptrA, 
							   const uint16* memPtrA, 
							   struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_SUB_VEC_MAP_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memRead( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->offsetE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->sizeE, memPtrA );

	if( memSizeL != bts_SubVecMap_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_SubVecMap_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
			        "size mismatch" );
		return 0;
	}

	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

void bts_SubVecMap_map( struct bbs_Context* cpA, 
						const struct bts_VectorMap* ptrA, 
						const struct bts_Flt16Vec* inVecPtrA,
						struct bts_Flt16Vec* outVecPtrA )
{
	bbs_DEF_fNameL( "bts_SubVecMap_map" )
	struct bts_SubVecMap* ptrL = ( struct bts_SubVecMap* )ptrA;

	int32 sizeL = ( ptrL->sizeE != -1 ) ? ptrL->sizeE : ( int32 )inVecPtrA->arrE.sizeE - ptrL->offsetE;
	if( sizeL < 0 ) sizeL = 0;

	if( ( ptrL->offsetE + sizeL ) > ( int32 )inVecPtrA->arrE.sizeE )
	{
		bbs_ERROR1( "%s:\ninput vector too small", fNameL );
		return;
	}

	if( outVecPtrA->arrE.allocatedSizeE < ( uint32 )sizeL )
	{
		bbs_ERROR1( "%s:\noutput vector is insufficiently allocated", fNameL );
		return;
	}

	bts_Flt16Vec_size( cpA, outVecPtrA, sizeL );
	outVecPtrA->expE = inVecPtrA->expE;
	bbs_memcpy16( outVecPtrA->arrE.arrPtrE, inVecPtrA->arrE.arrPtrE + ptrL->offsetE, sizeL );

	bts_Flt16Vec_maximizeMantisse( cpA, outVecPtrA );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

