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
#include "b_APIEm/FaceFinderRef.h"
#include "b_APIEm/Functions.h"

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

void bpi_FaceFinderRef_init( struct bbs_Context* cpA,
							 struct bpi_FaceFinderRef* ptrA )
{
	bbs_UInt16Arr_init( cpA, &ptrA->objBufE );
	ptrA->faceFinderPtrE = NULL;
}

/* ------------------------------------------------------------------------- */

void bpi_FaceFinderRef_exit( struct bbs_Context* cpA,
							 struct bpi_FaceFinderRef* ptrA )
{
	if( ptrA->faceFinderPtrE != NULL ) bpi_faceFinderExit( cpA, ptrA->faceFinderPtrE );
	bbs_UInt16Arr_exit( cpA, &ptrA->objBufE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bpi_FaceFinderRef_copy( struct bbs_Context* cpA,
							 struct bpi_FaceFinderRef* ptrA, 
							 const struct bpi_FaceFinderRef* srcPtrA )
{
	bbs_ERROR0( "bpi_FaceFinderRef_copy: function is not implemented" );
}

/* ------------------------------------------------------------------------- */

flag bpi_FaceFinderRef_equal( struct bbs_Context* cpA,
							  const struct bpi_FaceFinderRef* ptrA, 
							  const struct bpi_FaceFinderRef* srcPtrA )
{
	bbs_ERROR0( "bpi_FaceFinderRef_equal: function is not implemented" );
	return FALSE;
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
	
uint32 bpi_FaceFinderRef_memSize( struct bbs_Context* cpA,
								  const struct bpi_FaceFinderRef* ptrA )
{
	uint32 memSizeL = 0;
	memSizeL += bbs_SIZEOF16( uint32 ); /* mem size */
	memSizeL += bbs_SIZEOF16( flag ); /* object presence flag */
	if( ptrA->faceFinderPtrE != NULL ) memSizeL += bpi_faceFinderMemSize( cpA, ptrA->faceFinderPtrE );
	memSizeL += bbs_SIZEOF16( uint16 ); /* csa */
	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bpi_FaceFinderRef_memWrite( struct bbs_Context* cpA,
								   const struct bpi_FaceFinderRef* ptrA, 
								   uint16* memPtrA )
{
	uint32 memSizeL = bpi_FaceFinderRef_memSize( cpA, ptrA );
	flag objPresentL = ptrA->faceFinderPtrE != NULL;
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWrite32( &objPresentL, memPtrA );
	if( objPresentL ) memPtrA += bpi_faceFinderMemWrite( cpA, ptrA->faceFinderPtrE, memPtrA );
	memPtrA += bpi_memWriteCsa16( memPtrA, memSizeL, 0xFFFF );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bpi_FaceFinderRef_memRead( struct bbs_Context* cpA,
								  struct bpi_FaceFinderRef* ptrA, 
								  uint32 maxImageWidthA,
								  uint32 maxImageHeightA,
								  const uint16* memPtrA,
								  struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL;
	flag objPresentL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_segPtr( cpA, &memTblL, 0 );
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memRead32( &objPresentL, memPtrA );

	/* check object & allocate data buffer */
	{
		const uint16* memPtrL = memPtrA;
		uint32 dataSizeL = 0;

		if( objPresentL )
		{
			enum bpi_FaceFinderType typeL = ( enum bpi_FaceFinderType )bbs_memPeek32( memPtrL + 4 );
			dataSizeL += bpi_faceFinderSizeOf16( cpA, typeL );
			memPtrL += bbs_memPeek32( memPtrL );
		}

		bbs_UInt16Arr_create( cpA, &ptrA->objBufE, dataSizeL, espL );
	}

	/* load object */
	{
		uint16* dataPtrL = ptrA->objBufE.arrPtrE;

		if( objPresentL )
		{
			enum bpi_FaceFinderType typeL = ( enum bpi_FaceFinderType )bbs_memPeek32( memPtrA + 4 );
			ptrA->faceFinderPtrE = ( struct bpi_FaceFinder* )dataPtrL;
			bpi_faceFinderInit( cpA, ptrA->faceFinderPtrE, typeL );
			ptrA->faceFinderPtrE->vpSetParamsE( cpA, ptrA->faceFinderPtrE, maxImageWidthA, maxImageHeightA );
			memPtrA += bpi_faceFinderMemRead( cpA, ptrA->faceFinderPtrE, memPtrA, &memTblL );
			dataPtrL += bpi_faceFinderSizeOf16( cpA, typeL );
		}
		else
		{
			ptrA->faceFinderPtrE = NULL;
		}
	}

	memPtrA += bpi_memReadCsa16( memPtrA );

	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

void bpi_FaceFinderRef_setParams( struct bbs_Context* cpA,
								  struct bpi_FaceFinderRef* ptrA, 
								  uint32 maxImageWidthA,
								  uint32 maxImageHeightA )
{
	bbs_DEF_fNameL( "bpi_FaceFinderRef_setParams" );
	if( ptrA->faceFinderPtrE == NULL )
	{
		bbs_ERROR1( "%s:\nNo face finder object was loaded", fNameL );
		return;
 	}
	ptrA->faceFinderPtrE->vpSetParamsE( cpA, ptrA->faceFinderPtrE, maxImageWidthA, maxImageHeightA );
}

/* ------------------------------------------------------------------------- */

void bpi_FaceFinderRef_setRange( struct bbs_Context* cpA,
								 struct bpi_FaceFinderRef* ptrA, 
								 uint32 minEyeDistanceA,
								 uint32 maxEyeDistanceA )
{
	bbs_DEF_fNameL( "bpi_FaceFinderRef_setRange" );
	if( ptrA->faceFinderPtrE == NULL )
	{
		bbs_ERROR1( "%s:\nNo face finder object was loaded", fNameL );
		return;
 	}
	ptrA->faceFinderPtrE->vpSetRangeE( cpA, ptrA->faceFinderPtrE, minEyeDistanceA, maxEyeDistanceA );
}

/* ------------------------------------------------------------------------- */

int32 bpi_FaceFinderRef_process( struct bbs_Context* cpA,
							     const struct bpi_FaceFinderRef* ptrA, 
								 struct bpi_DCR* dcrPtrA )
{
	bbs_DEF_fNameL( "bpi_FaceFinderRef_process" );
	if( ptrA->faceFinderPtrE == NULL )
	{
		bbs_ERROR1( "%s:\nNo face finder object was loaded", fNameL );
		return 0;
 	}
	return ptrA->faceFinderPtrE->vpProcessE( cpA, ptrA->faceFinderPtrE, dcrPtrA );
}

/* ------------------------------------------------------------------------- */

int32 bpi_FaceFinderRef_putDcr( struct bbs_Context* cpA,
							 	const struct bpi_FaceFinderRef* ptrA, 
								struct bpi_DCR* dcrPtrA )
{
	bbs_DEF_fNameL( "bpi_FaceFinderRef_putDcr" );
	if( ptrA->faceFinderPtrE == NULL )
	{
		bbs_ERROR1( "%s:\nNo face finder object was loaded", fNameL );
		return 0;
 	}
	return ptrA->faceFinderPtrE->vpPutDcrE( cpA, ptrA->faceFinderPtrE, dcrPtrA );
}

/* ------------------------------------------------------------------------- */

void bpi_FaceFinderRef_getDcr( struct bbs_Context* cpA,
							   const struct bpi_FaceFinderRef* ptrA, 
							   uint32 indexA,
							   struct bpi_DCR* dcrPtrA )
{
	bbs_DEF_fNameL( "bpi_FaceFinderRef_getDcr" );
	if( ptrA->faceFinderPtrE == NULL )
	{
		bbs_ERROR1( "%s:\nNo face finder object was loaded", fNameL );
		return;
 	}
	ptrA->faceFinderPtrE->vpGetDcrE( cpA, ptrA->faceFinderPtrE, indexA, dcrPtrA );
}


/* ------------------------------------------------------------------------- */

/* ========================================================================= */

