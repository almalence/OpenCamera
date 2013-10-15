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

#include "b_APIEm/BFFaceFinder.h"
#include "b_APIEm/Functions.h"
#include "b_APIEm/DCR.h"
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

void bpi_BFFaceFinder_init( struct bbs_Context* cpA,
						    struct bpi_BFFaceFinder* ptrA )
{
	bpi_FaceFinder_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bpi_FF_BF_FACE_FINDER;
	ptrA->baseE.vpSetParamsE = bpi_BFFaceFinder_setParams;
	ptrA->baseE.vpSetRangeE = bpi_BFFaceFinder_setRange;
	ptrA->baseE.vpProcessE = bpi_BFFaceFinder_processDcr;
	ptrA->baseE.vpPutDcrE = bpi_BFFaceFinder_putDcr;
	ptrA->baseE.vpGetDcrE = bpi_BFFaceFinder_getDcr;

	ptrA->detectedFacesE = 0;
	ptrA->availableFacesE = 0;
	ptrA->faceDataBufferE = NULL;
	bbf_ScanDetector_init( cpA, &ptrA->detectorE );
}

/* ------------------------------------------------------------------------- */

void bpi_BFFaceFinder_exit( struct bbs_Context* cpA,
						    struct bpi_BFFaceFinder* ptrA )
{
	ptrA->detectedFacesE = 0;
	ptrA->availableFacesE = 0;
	ptrA->faceDataBufferE = NULL;
	bbf_ScanDetector_exit( cpA, &ptrA->detectorE );

	bpi_FaceFinder_exit( cpA, &ptrA->baseE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bpi_BFFaceFinder_copy( struct bbs_Context* cpA,
						    struct bpi_BFFaceFinder* ptrA, 
							const struct bpi_BFFaceFinder* srcPtrA )
{
	bpi_FaceFinder_copy( cpA, &ptrA->baseE, &srcPtrA->baseE );
	bbf_ScanDetector_copy( cpA, &ptrA->detectorE, &srcPtrA->detectorE );
}

/* ------------------------------------------------------------------------- */

flag bpi_BFFaceFinder_equal( struct bbs_Context* cpA,
							 const struct bpi_BFFaceFinder* ptrA, 
							 const struct bpi_BFFaceFinder* srcPtrA )
{
	if( !bpi_FaceFinder_equal( cpA, &ptrA->baseE, &srcPtrA->baseE ) ) return FALSE;
	if( !bbf_ScanDetector_equal( cpA, &ptrA->detectorE, &srcPtrA->detectorE ) ) return FALSE;
	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bpi_BFFaceFinder_getMinEyeDistance( const struct bpi_BFFaceFinder* ptrA )
{
	return ( ( ptrA->detectorE.refDistanceE >> 8 ) * ( ptrA->detectorE.minScaleE >> 12 ) ) >> 16;
}

/* ------------------------------------------------------------------------- */

uint32 bpi_BFFaceFinder_getMaxEyeDistance( const struct bpi_BFFaceFinder* ptrA )
{
	return ( ( ptrA->detectorE.refDistanceE >> 8 ) * ( ptrA->detectorE.maxScaleE >> 12 ) ) >> 16;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bpi_BFFaceFinder_setMinEyeDistance( struct bbs_Context* cpA,
										 struct bpi_BFFaceFinder* ptrA, 
										 uint32 distA )
{
	ptrA->detectorE.minScaleE = ( ( distA << 16 ) / ( ptrA->detectorE.refDistanceE >> 8 ) ) << 12;
	if( ptrA->detectorE.minScaleE < 0x100000 /* 1.0 */ ) ptrA->detectorE.minScaleE = 0x100000;
}

/* ------------------------------------------------------------------------- */
	
void bpi_BFFaceFinder_setMaxEyeDistance( struct bbs_Context* cpA,
										 struct bpi_BFFaceFinder* ptrA, 
										 uint32 distA )
{
	if( distA > 0x0FFFF )
	{
		ptrA->detectorE.maxScaleE = 0; /* unlimited */
	}
	else
	{
		ptrA->detectorE.maxScaleE = ( ( distA << 16 ) / ( ptrA->detectorE.refDistanceE >> 8 ) ) << 12;
	}
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bpi_BFFaceFinder_memSize( struct bbs_Context* cpA,
								 const struct bpi_BFFaceFinder *ptrA )
{
	uint32 memSizeL = 0;
	memSizeL += bbs_SIZEOF16( uint32 );
	memSizeL += bbs_SIZEOF16( uint32 ); /* version */
	memSizeL += bpi_FaceFinder_memSize( cpA, &ptrA->baseE );
	memSizeL += bbf_ScanDetector_memSize( cpA, &ptrA->detectorE );
	memSizeL += bbs_SIZEOF16( uint16 ); /* csa */
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bpi_BFFaceFinder_memWrite( struct bbs_Context* cpA,
								  const struct bpi_BFFaceFinder* ptrA, 
								  uint16* memPtrA )
{
	uint32 memSizeL = bpi_BFFaceFinder_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bpi_BF_FACE_FINDER_VERSION, memPtrA );
	memPtrA += bpi_FaceFinder_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbf_ScanDetector_memWrite( cpA, &ptrA->detectorE, memPtrA );
	memPtrA += bpi_memWriteCsa16( memPtrA, memSizeL, 0xFFFF );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bpi_BFFaceFinder_memRead( struct bbs_Context* cpA,
								 struct bpi_BFFaceFinder* ptrA, 
								 const uint16* memPtrA,
   								 struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bpi_BF_FACE_FINDER_VERSION, memPtrA );
	memPtrA += bpi_FaceFinder_memRead( cpA, &ptrA->baseE, memPtrA );
	if( bbs_Context_error( cpA ) ) return 0;

	memPtrA += bbf_ScanDetector_memRead( cpA, &ptrA->detectorE, memPtrA, mtpA );
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bpi_memReadCsa16( memPtrA );

/*	if( memSizeL != bpi_BFFaceFinder_memSize( cpA, ptrA ) )
	{
		bbs_ERROR0( "uint32 bpi_BFFaceFinder_memRead( .... ):\n"
                    "Module file is corrupt or incorrect. Please check if the face finder module is still supported." ); 
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

int32 bpi_BFFaceFinder_process( struct bbs_Context* cpA,
							    const struct bpi_BFFaceFinder* ptrA, 
								void* imagePtrA,
								uint32 imageWidthA,
								uint32 imageHeightA,
								const struct bts_Int16Rect* roiPtrA,
								struct bts_Int16Vec2D* offsPtrA,
								struct bts_IdCluster2D* idClusterPtrA )
{
	int32 xL = 0; /* 16.16 */
	int32 yL = 0; /* 16.16 */
	uint32 scaleL = 0;
	int32 actL = 0;
	int32* outArrL;

	struct bts_Flt16Alt2D altL;
	struct bts_Flt16Vec2D centerL;

	struct bpi_BFFaceFinder* ptrL = ( struct bpi_BFFaceFinder* )ptrA;

	/* reset multi face imformation so they are not accidentally used */
	ptrL->detectedFacesE = 0;
	ptrL->availableFacesE = 0;
	ptrL->faceDataBufferE = NULL;

	bbf_ScanDetector_process( cpA, ( struct bbf_ScanDetector* )&ptrA->detectorE, imagePtrA, imageWidthA, imageHeightA, roiPtrA, &outArrL );

	xL      = outArrL[ 0 ]; /* 16.16 */
	yL      = outArrL[ 1 ]; /* 16.16 */
	scaleL  = outArrL[ 2 ]; /* 12.20 */
	actL    = outArrL[ 3 ]; /*  4.28 */

	if( bbs_Context_error( cpA ) ) return 0;

	offsPtrA->xE = xL >> 16;
	offsPtrA->yE = yL >> 16;
	xL -= ( ( int32 )offsPtrA->xE << 16 );
	yL -= ( ( int32 )offsPtrA->yE << 16 );

	centerL = bts_Flt16Vec2D_create32( 0, 0, 0 );
	altL = bts_Flt16Alt2D_createScale( scaleL, 20, &centerL );
	altL.vecE = bts_Flt16Vec2D_create32( xL, yL, 16 );

	/* compute cluster */
	{
		uint32 eyeDistL = ( ( ptrA->detectorE.refDistanceE >> 16 ) * scaleL ) >> 20;
		uint32 logEyeDistL = bbs_intLog2( eyeDistL );
		int32 bbpL = 11 - logEyeDistL;
		bbpL = bbpL < 0 ? 0 : bbpL;
		bbpL = bbpL > 6 ? 6 : bbpL;
		bts_IdCluster2D_copyTransform( cpA, idClusterPtrA, &ptrA->detectorE.refClusterE, altL, bbpL );
	}


	return ( actL + 0x10000000 ) >> 5; /*output range 0...1 in 8.24*/
}

/* ------------------------------------------------------------------------- */

uint32 bpi_BFFaceFinder_multiProcess( struct bbs_Context* cpA,
									  const struct bpi_BFFaceFinder* ptrA, 
									  void* imagePtrA,
									  uint32 imageWidthA,
									  uint32 imageHeightA,
									  const struct bts_Int16Rect* roiPtrA )
{
	struct bpi_BFFaceFinder* ptrL = ( struct bpi_BFFaceFinder* )ptrA;
	ptrL->detectedFacesE = bbf_ScanDetector_process( cpA, ( struct bbf_ScanDetector* )&ptrA->detectorE, imagePtrA, imageWidthA, imageHeightA, roiPtrA, &ptrL->faceDataBufferE );
	ptrL->availableFacesE = ptrA->detectedFacesE > 0 ? ptrA->detectedFacesE : 1;
	if( bbs_Context_error( cpA ) ) return 0;
	return ptrL->detectedFacesE;
}

/* ------------------------------------------------------------------------- */

uint32 bpi_BFFaceFinder_getFace( struct bbs_Context* cpA,
								 const struct bpi_BFFaceFinder* ptrA, 
								 uint32 indexA,
								 struct bts_Int16Vec2D* offsPtrA,
								 struct bts_IdCluster2D* idClusterPtrA )
{
	bbs_DEF_fNameL( "bpi_BFFaceFinder_getFace" )
	int32 xL = 0; /* 16.16 */
	int32 yL = 0; /* 16.16 */
	uint32 scaleL = 0;
	int32 actL = 0;
	struct bts_Flt16Alt2D altL;
	struct bts_Flt16Vec2D centerL;

	if( bbs_Context_error( cpA ) ) return 0;

	if( ptrA->availableFacesE == 0 || ptrA->faceDataBufferE == NULL ) 
	{
		bbs_ERROR1( "%s:\nNo faces are availabe. This function was called before the face finder could detect multiple faces in an image", fNameL );
		return 0;
	}

	if( indexA >= ptrA->availableFacesE ) 
	{
		bbs_ERROR1( "%s:\nface index exceeds number of available faces", fNameL );
		return 0;
	}

	xL      = ptrA->faceDataBufferE[ indexA * 4 + 0 ]; /* 16.16 */
	yL      = ptrA->faceDataBufferE[ indexA * 4 + 1 ]; /* 16.16 */
	scaleL  = ptrA->faceDataBufferE[ indexA * 4 + 2 ]; /* 12.20 */
	actL    = ptrA->faceDataBufferE[ indexA * 4 + 3 ]; /*  4.28 */

	offsPtrA->xE = xL >> 16;
	offsPtrA->yE = yL >> 16;

	xL -= ( ( int32 )offsPtrA->xE << 16 );
	yL -= ( ( int32 )offsPtrA->yE << 16 );

	centerL = bts_Flt16Vec2D_create32( 0, 0, 0 );
	altL = bts_Flt16Alt2D_createScale( scaleL, 20, &centerL );
	altL.vecE = bts_Flt16Vec2D_create32( xL, yL, 16 );

	/* compute cluster */
	{
		uint32 eyeDistL = ( ( ptrA->detectorE.refDistanceE >> 16 ) * scaleL ) >> 20;
		uint32 logEyeDistL = bbs_intLog2( eyeDistL );
		int32 bbpL = 11 - logEyeDistL;
		bbpL = bbpL < 0 ? 0 : bbpL;
		bbpL = bbpL > 6 ? 6 : bbpL;
		bts_IdCluster2D_copyTransform( cpA, idClusterPtrA, &ptrA->detectorE.refClusterE, altL, bbpL );
	}

	return ( actL + 0x10000000 ) >> 5; /*output range 0...1 in 8.24*/
}

/* ------------------------------------------------------------------------- */

void bpi_BFFaceFinder_getFaceDCR( struct bbs_Context* cpA,
								  const struct bpi_BFFaceFinder* ptrA, 
								  uint32 indexA,
								  struct bpi_DCR* dcrPtrA )
{
	int32 confL = bpi_BFFaceFinder_getFace( cpA, ptrA, indexA, &dcrPtrA->offsE, &dcrPtrA->mainClusterE );
	bts_IdCluster2D_copy( cpA, &dcrPtrA->sdkClusterE, &dcrPtrA->mainClusterE );
	dcrPtrA->confidenceE = confL;
	dcrPtrA->approvedE = confL > ( ( int32 )1 << 23 );
}

/* ------------------------------------------------------------------------- */

void bpi_BFFaceFinder_setMaxImageSize( struct bbs_Context* cpA,
									   struct bpi_BFFaceFinder* ptrA, 
									   uint32 maxImageWidthA,
									   uint32 maxImageHeightA )
{
	ptrA->detectorE.maxImageWidthE = maxImageWidthA;
	ptrA->detectorE.maxImageHeightE = maxImageHeightA;
}

/* ------------------------------------------------------------------------- */

void bpi_BFFaceFinder_setParams( struct bbs_Context* cpA,
								 struct bpi_FaceFinder* ptrA, 
								 uint32 maxImageWidthA,
								 uint32 maxImageHeightA )
{
	bbs_DEF_fNameL( "bpi_BFFaceFinder_setParams" );

	if( bbs_Context_error( cpA ) ) return;

	if( ptrA->typeE != bpi_FF_BF_FACE_FINDER ) 
	{
		bbs_ERROR1( "%s:\nObject type mismatch", fNameL );
		return;
	}
	bpi_BFFaceFinder_setMaxImageSize( cpA, ( struct bpi_BFFaceFinder* )ptrA, maxImageWidthA, maxImageHeightA );
}

/* ------------------------------------------------------------------------- */

void bpi_BFFaceFinder_setRange( struct bbs_Context* cpA,
								struct bpi_FaceFinder* ptrA, 
								uint32 minEyeDistanceA,
								uint32 maxEyeDistanceA )
{
	bbs_DEF_fNameL( "bpi_BFFaceFinder_setParams" );

	if( bbs_Context_error( cpA ) ) return;

	if( ptrA->typeE != bpi_FF_BF_FACE_FINDER ) 
	{
		bbs_ERROR1( "%s:\nObject type mismatch", fNameL );
		return;
	}
	bpi_BFFaceFinder_setMinEyeDistance( cpA, ( struct bpi_BFFaceFinder* )ptrA, minEyeDistanceA );
	bpi_BFFaceFinder_setMaxEyeDistance( cpA, ( struct bpi_BFFaceFinder* )ptrA, maxEyeDistanceA );
}

/* ------------------------------------------------------------------------- */

int32 bpi_BFFaceFinder_processDcr( struct bbs_Context* cpA,
								   const struct bpi_FaceFinder* ptrA, 
						           struct bpi_DCR* dcrPtrA )
{
	bbs_DEF_fNameL( "bpi_BFFaceFinder_processDcr" );

	if( bbs_Context_error( cpA ) ) return 0;

	if( ptrA->typeE != bpi_FF_BF_FACE_FINDER ) 
	{
		bbs_ERROR1( "%s:\nObject type mismatch", fNameL );
		return 0;
	}

	return bpi_BFFaceFinder_process( cpA, 
									( const struct bpi_BFFaceFinder* )ptrA, 
									dcrPtrA->imageDataPtrE,
									dcrPtrA->imageWidthE,
									dcrPtrA->imageHeightE,
									&dcrPtrA->roiRectE,
									&dcrPtrA->offsE,
									&dcrPtrA->mainClusterE );
}

/* ------------------------------------------------------------------------- */

int32 bpi_BFFaceFinder_putDcr( struct bbs_Context* cpA,
							   const struct bpi_FaceFinder* ptrA, 
							   struct bpi_DCR* dcrPtrA )
{
	bbs_DEF_fNameL( "bpi_BFFaceFinder_putDcr" );

	if( bbs_Context_error( cpA ) ) return 0;

	if( ptrA->typeE != bpi_FF_BF_FACE_FINDER ) 
	{
		bbs_ERROR1( "%s:\nObject type mismatch", fNameL );
		return 0;
	}

	return bpi_BFFaceFinder_multiProcess( cpA, 
										 ( const struct bpi_BFFaceFinder* )ptrA, 
										 dcrPtrA->imageDataPtrE,
										 dcrPtrA->imageWidthE,
										 dcrPtrA->imageHeightE,
										 &dcrPtrA->roiRectE );
}

/* ------------------------------------------------------------------------- */

void bpi_BFFaceFinder_getDcr( struct bbs_Context* cpA,
							  const struct bpi_FaceFinder* ptrA, 
							  uint32 indexA,
							  struct bpi_DCR* dcrPtrA )
{
	bbs_DEF_fNameL( "bpi_BFFaceFinder_getDcr" );

	if( bbs_Context_error( cpA ) ) return;

	if( ptrA->typeE != bpi_FF_BF_FACE_FINDER ) 
	{
		bbs_ERROR1( "%s:\nObject type mismatch", fNameL );
		return;
	}

	bpi_BFFaceFinder_getFaceDCR( cpA, ( const struct bpi_BFFaceFinder* )ptrA, indexA, dcrPtrA );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
