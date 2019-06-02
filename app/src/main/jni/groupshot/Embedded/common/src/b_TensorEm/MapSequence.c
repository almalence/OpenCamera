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
#include "b_TensorEm/MapSequence.h"

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

void bts_MapSequence_init( struct bbs_Context* cpA,
					       struct bts_MapSequence* ptrA )
{
	ptrA->ptrArrE = NULL;
	bts_Flt16Vec_init( cpA, &ptrA->vecE );
	bts_VectorMap_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bts_VM_MAP_SEQUENCE;
	ptrA->baseE.vpMapE = bts_MapSequence_map;
	ptrA->sizeE = 0;
	ptrA->vecSizeE = 0;
	bbs_UInt16Arr_init( cpA, &ptrA->objBufE );
}

/* ------------------------------------------------------------------------- */

void bts_MapSequence_exit( struct bbs_Context* cpA,
					       struct bts_MapSequence* ptrA )
{
	uint16 iL;
	for( iL = 0; iL < ptrA->sizeE; iL++ ) bts_vectorMapExit( cpA, ptrA->ptrArrE[ iL ] );
	ptrA->ptrArrE = NULL;
	bts_Flt16Vec_exit( cpA, &ptrA->vecE );
	ptrA->sizeE = 0;
	ptrA->vecSizeE = 0;
	bbs_UInt16Arr_exit( cpA, &ptrA->objBufE );

	bts_VectorMap_exit( cpA, &ptrA->baseE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_MapSequence_copy( struct bbs_Context* cpA,
						   struct bts_MapSequence* ptrA, 
					       const struct bts_MapSequence* srcPtrA )
{
	bbs_ERROR0( "bts_MapSequence_copy:\n Function is not available" );
}

/* ------------------------------------------------------------------------- */

flag bts_MapSequence_equal( struct bbs_Context* cpA,
						    const struct bts_MapSequence* ptrA, 
						    const struct bts_MapSequence* srcPtrA )
{
	bbs_ERROR0( "bts_MapSequence_equal:\n Function is not available" );
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
	
uint32 bts_MapSequence_memSize( struct bbs_Context* cpA,
								const struct bts_MapSequence* ptrA )
{
	uint16 iL;
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bts_VectorMap_memSize( cpA, &ptrA->baseE );
	memSizeL += bbs_SIZEOF16( ptrA->sizeE );
	memSizeL += bbs_SIZEOF16( ptrA->vecSizeE );
	for( iL = 0; iL < ptrA->sizeE; iL++ ) memSizeL += bts_vectorMapMemSize( cpA, ptrA->ptrArrE[ iL ] );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_MapSequence_memWrite( struct bbs_Context* cpA,
								 const struct bts_MapSequence* ptrA, 
								 uint16* memPtrA )
{
	uint16 iL;
	uint32 memSizeL = bts_MapSequence_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_MAP_SEQUENCE_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->sizeE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->vecSizeE, memPtrA );
	for( iL = 0; iL < ptrA->sizeE; iL++ ) memPtrA += bts_vectorMapMemWrite( cpA, ptrA->ptrArrE[ iL ], memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bts_MapSequence_memRead( struct bbs_Context* cpA,
								struct bts_MapSequence* ptrA, 
								const uint16* memPtrA, 
								struct bbs_MemTbl* mtpA )
{
	uint16 iL;
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_segPtr( cpA, &memTblL, 0 );
	struct bbs_MemSeg* sspL = bbs_MemTbl_sharedSegPtr( cpA, &memTblL, 0 );
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_MAP_SEQUENCE_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memRead( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->sizeE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->vecSizeE, memPtrA );

	/* put buffer vector on shared memory */
	bts_Flt16Vec_create( cpA, &ptrA->vecE, ptrA->vecSizeE, sspL );

	/* check maps & allocate data buffer */
	{
		const uint16* memPtrL = memPtrA;
		uint32 dataSizeL = ptrA->sizeE * bbs_SIZEOF16( struct bts_VectorMap* );

		for( iL = 0; iL < ptrA->sizeE; iL++ )
		{
			enum bts_VectorMapType typeL = ( enum bts_VectorMapType )bbs_memPeek32( memPtrL + 4 );
			dataSizeL += bts_vectorMapSizeOf16( cpA, typeL );
			memPtrL += bbs_memPeek32( memPtrL );
		}

		bbs_UInt16Arr_create( cpA, &ptrA->objBufE, dataSizeL, espL );
		if( bbs_Context_error( cpA ) ) return 0;
	}

	/* load maps & initialize pointers */
	{
		uint16* dataPtrL = ptrA->objBufE.arrPtrE;
		ptrA->ptrArrE = ( struct bts_VectorMap** )dataPtrL;
		dataPtrL += ptrA->sizeE * bbs_SIZEOF16( struct bts_VectorMap* );
		for( iL = 0; iL < ptrA->sizeE; iL++ )
		{
			enum bts_VectorMapType typeL = ( enum bts_VectorMapType )bbs_memPeek32( memPtrA + 4 );
			ptrA->ptrArrE[ iL ] = ( struct bts_VectorMap* )dataPtrL;
			bts_vectorMapInit( cpA, ptrA->ptrArrE[ iL ], typeL );
			memPtrA += bts_vectorMapMemRead( cpA, ptrA->ptrArrE[ iL ], memPtrA, &memTblL );
			dataPtrL += bts_vectorMapSizeOf16( cpA, typeL );
		}
	}

	if( memSizeL != bts_MapSequence_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_MapSequence_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
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

void bts_MapSequence_map( struct bbs_Context* cpA, 
						  const struct bts_VectorMap* ptrA, 
						  const struct bts_Flt16Vec* inVecPtrA,
						  struct bts_Flt16Vec* outVecPtrA )
{
	struct bts_MapSequence* ptrL = ( struct bts_MapSequence* )ptrA;
	if( ptrL->sizeE == 0 )
	{
		bts_Flt16Vec_copy( cpA, outVecPtrA, inVecPtrA );
	}
	else if( ptrL->sizeE == 1 )
	{
		struct bts_VectorMap* mapPtrL = ptrL->ptrArrE[ 0 ];
		mapPtrL->vpMapE( cpA, mapPtrL, inVecPtrA, outVecPtrA );
	}
	else
	{
		uint32 iL;
		struct bts_Flt16Vec* vp1L = &ptrL->vecE;
		struct bts_Flt16Vec* vp2L = outVecPtrA;
		struct bts_VectorMap* mapPtrL = ptrL->ptrArrE[ 0 ];
		mapPtrL->vpMapE( cpA, mapPtrL, inVecPtrA, vp1L );

		for( iL = 1; iL < ptrL->sizeE; iL++ )
		{
			mapPtrL = ptrL->ptrArrE[ iL ];
			mapPtrL->vpMapE( cpA, mapPtrL, vp1L, vp2L );

			/* swap vectors */
			{
				struct bts_Flt16Vec* vpL = vp1L;
				vp1L = vp2L;
				vp2L = vpL;
			}
		}

		/* vp1 holds output */
		if( vp1L != outVecPtrA ) bts_Flt16Vec_copy( cpA, outVecPtrA, vp1L );
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

