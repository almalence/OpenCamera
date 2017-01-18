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
#include "b_BitFeatureEm/Sequence.h"

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

void bbf_Sequence_init( struct bbs_Context* cpA,
					    struct bbf_Sequence* ptrA )
{
	bbs_memset16( ptrA->ftrPtrArrE, 0, bbs_SIZEOF16( ptrA->ftrPtrArrE ) );

	bbf_Feature_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bbf_FT_SEQUENCE;
	ptrA->baseE.vpActivityE = bbf_Sequence_activity;
	ptrA->sizeE = 0;
	bbs_Int32Arr_init( cpA, &ptrA->thrArrE );
	bbs_UInt16Arr_init( cpA, &ptrA->wgtArrE );
	bbs_UInt16Arr_init( cpA, &ptrA->dataArrE );
}

/* ------------------------------------------------------------------------- */

void bbf_Sequence_exit( struct bbs_Context* cpA,
					    struct bbf_Sequence* ptrA )
{
	uint16 iL;
	for( iL = 0; iL < ptrA->sizeE; iL++ ) bbf_featureExit( cpA, ptrA->ftrPtrArrE[ iL ] );

	bbs_memset16( ptrA->ftrPtrArrE, 0, bbs_SIZEOF16( ptrA->ftrPtrArrE ) );
	bbf_Feature_exit( cpA, &ptrA->baseE );
	ptrA->sizeE = 0;
	bbs_Int32Arr_exit( cpA, &ptrA->thrArrE );
	bbs_UInt16Arr_exit( cpA, &ptrA->wgtArrE );
	bbs_UInt16Arr_exit( cpA, &ptrA->dataArrE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_Sequence_copy( struct bbs_Context* cpA,
					    struct bbf_Sequence* ptrA, 
					    const struct bbf_Sequence* srcPtrA )
{
	bbs_ERROR0( "bbf_Sequence_copy:\n Function is not available" );
}

/* ------------------------------------------------------------------------- */

flag bbf_Sequence_equal( struct bbs_Context* cpA,
						 const struct bbf_Sequence* ptrA, 
						 const struct bbf_Sequence* srcPtrA )
{
	bbs_ERROR0( "bbf_Sequence_equal:\n Function is not available" );
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
	
uint32 bbf_Sequence_memSize( struct bbs_Context* cpA,
						     const struct bbf_Sequence* ptrA )
{
	uint16 iL;
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bbf_Feature_memSize( cpA, &ptrA->baseE );
	memSizeL += bbs_SIZEOF16( ptrA->sizeE );
	memSizeL += bbs_Int32Arr_memSize( cpA, &ptrA->thrArrE ); 
	memSizeL += bbs_UInt16Arr_memSize( cpA, &ptrA->wgtArrE ); 
	for( iL = 0; iL < ptrA->sizeE; iL++ ) memSizeL += bbf_featureMemSize( cpA, ptrA->ftrPtrArrE[ iL ] );
	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_Sequence_memWrite( struct bbs_Context* cpA,
						      const struct bbf_Sequence* ptrA, 
							  uint16* memPtrA )
{
	uint16 iL;
	uint32 memSizeL = bbf_Sequence_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bbf_SEQUENCE_VERSION, memPtrA );
	memPtrA += bbf_Feature_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->sizeE, memPtrA );
	memPtrA += bbs_Int32Arr_memWrite( cpA, &ptrA->thrArrE, memPtrA ); 
	memPtrA += bbs_UInt16Arr_memWrite( cpA, &ptrA->wgtArrE, memPtrA ); 
	for( iL = 0; iL < ptrA->sizeE; iL++ ) memPtrA += bbf_featureMemWrite( cpA, ptrA->ftrPtrArrE[ iL ], memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bbf_Sequence_memRead( struct bbs_Context* cpA,
						     struct bbf_Sequence* ptrA, 
							 const uint16* memPtrA, 
							 struct bbs_MemTbl* mtpA )
{
	uint16 iL;
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_fastestSegPtr( cpA, &memTblL, 0 );
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bbf_SEQUENCE_VERSION, memPtrA );
	memPtrA += bbf_Feature_memRead( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->sizeE, memPtrA );

	if( ptrA->sizeE > bbf_SEQUENCE_MAX_SIZE )
	{
		bbs_ERROR0( "bbf_Sequence_memRead:\n Sequence size exceeds bbf_SEQUENCE_MAX_SIZE" );
		return 0;
	}

	memPtrA += bbs_Int32Arr_memRead( cpA, &ptrA->thrArrE, memPtrA, espL ); 

	if( versionL >= 101 ) memPtrA += bbs_UInt16Arr_memRead( cpA, &ptrA->wgtArrE, memPtrA, espL ); 

	/* check features & allocate data buffer */
	{
		const uint16* memPtrL = memPtrA;
		uint32 dataSizeL = 0;
		for( iL = 0; iL < ptrA->sizeE; iL++ )
		{
			enum bbf_FeatureType typeL = ( enum bbf_FeatureType )bbs_memPeek32( memPtrL + 4 );
			dataSizeL += bbf_featureSizeOf16( cpA, typeL );
			memPtrL += bbs_memPeek32( memPtrL );
		}
		bbs_UInt16Arr_create( cpA, &ptrA->dataArrE, dataSizeL, espL );
	}

	/* load features & initialize pointers */
	{
		uint16* dataPtrL = ptrA->dataArrE.arrPtrE;
		for( iL = 0; iL < ptrA->sizeE; iL++ )
		{
			enum bbf_FeatureType typeL = ( enum bbf_FeatureType )bbs_memPeek32( memPtrA + 4 );
			ptrA->ftrPtrArrE[ iL ] = ( struct bbf_Feature* )dataPtrL;
			bbf_featureInit( cpA, ptrA->ftrPtrArrE[ iL ], typeL );
			memPtrA += bbf_featureMemRead( cpA, ptrA->ftrPtrArrE[ iL ], memPtrA, &memTblL );
			dataPtrL += bbf_featureSizeOf16( cpA, typeL );
		}
	}

/*	if( memSizeL != bbf_Sequence_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_Sequence_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
			        "size mismatch" );
		return 0;
	}
*/

	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

int32 bbf_Sequence_activity( const struct bbf_Feature* ptrA, const uint32* patchA )
{
	const struct bbf_Sequence* ptrL = ( struct bbf_Sequence* )ptrA;

	count_t iL;
	
	int32 sizeL = ptrL->sizeE;

	/* 12.20 */
	int32 actSumL = ( -sizeL ) << 20;

	if( sizeL == 0 ) return 0x10000000; /* 1.0 in 4.28 format */

	if( ptrL->wgtArrE.sizeE == 0 )
	{
		for( iL = 0; iL < ptrL->sizeE; iL++ )
		{
			/* 4.28 */
			int32 actL = ptrL->ftrPtrArrE[ iL ]->vpActivityE( ptrL->ftrPtrArrE[ iL ], patchA ) - ptrL->thrArrE.arrPtrE[ iL ];
			actSumL += ( actL >> 8 );
			if( actL < 0 ) return ( actSumL / sizeL ) << 7; /* return 4.28 */
		}
	}
	else
	{
		for( iL = 0; iL < ptrL->sizeE; iL++ )
		{
			/* 4.28 */
			int32 actL = ptrL->ftrPtrArrE[ iL ]->vpActivityE( ptrL->ftrPtrArrE[ iL ], patchA ) - ptrL->thrArrE.arrPtrE[ iL ];
			int32 wgtL = ptrL->wgtArrE.arrPtrE[ iL ];
			actL = ( actL >> 16 ) * wgtL + ( ( ( int32 )( actL & 0x0000FFFF ) * wgtL ) >> 16 );
			actSumL += ( actL >> 8 );
			if( actL < 0 ) return ( actSumL / sizeL ) << 7; /* return 4.28 */
		}
	}

	actSumL += sizeL << 20;

    /* positive activity: ] 0, 1 ] */
	return ( actSumL / sizeL ) << 7; /* return 4.28 */
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

