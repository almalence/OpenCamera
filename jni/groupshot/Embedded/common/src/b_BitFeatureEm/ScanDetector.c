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
#include "b_BitFeatureEm/ScanDetector.h"

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

void bbf_ScanDetector_init( struct bbs_Context* cpA,
						    struct bbf_ScanDetector* ptrA )
{
	uint32 iL;

	ptrA->minScaleE = 0;
	ptrA->maxScaleE = 0;
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
	bbf_Scanner_init( cpA, &ptrA->scannerE );

	ptrA->patchWidthE = 0;
	ptrA->patchHeightE = 0;
	ptrA->minDefScaleE = 0;
	ptrA->maxDefScaleE = 0;
	ptrA->scaleStepE = 0;
	ptrA->overlapThrE = 0;
	ptrA->borderWidthE = 0;
	ptrA->borderHeightE = 0;
	ptrA->featuresE = 0;
	for( iL = 0; iL < bbf_SCAN_DETECTOR_MAX_FEATURES; iL++ ) bbf_BitParam_init( cpA, &ptrA->bitParamArrE[ iL ] );
	for( iL = 0; iL < bbf_SCAN_DETECTOR_MAX_FEATURES; iL++ ) bbf_Sequence_init( cpA, &ptrA->featureArrE[ iL ] );
	bts_IdCluster2D_init( cpA, &ptrA->refClusterE );
	ptrA->refDistanceE = 10;
}

/* ------------------------------------------------------------------------- */

void bbf_ScanDetector_exit( struct bbs_Context* cpA,
						    struct bbf_ScanDetector* ptrA )
{
	uint32 iL;

	ptrA->minScaleE = 0;
	ptrA->maxScaleE = 0;
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
	bbf_Scanner_exit( cpA, &ptrA->scannerE );

	ptrA->patchWidthE = 0;
	ptrA->patchHeightE = 0;
	ptrA->minDefScaleE = 0;
	ptrA->maxDefScaleE = 0;
	ptrA->scaleStepE = 0;
	ptrA->overlapThrE = 0;
	ptrA->borderWidthE = 0;
	ptrA->borderHeightE = 0;
	ptrA->featuresE = 0;
	for( iL = 0; iL < bbf_SCAN_DETECTOR_MAX_FEATURES; iL++ ) bbf_BitParam_exit( cpA, &ptrA->bitParamArrE[ iL ] );
	for( iL = 0; iL < bbf_SCAN_DETECTOR_MAX_FEATURES; iL++ ) bbf_Sequence_exit( cpA, &ptrA->featureArrE[ iL ] );
	bts_IdCluster2D_exit( cpA, &ptrA->refClusterE );
	ptrA->refDistanceE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_ScanDetector_copy( struct bbs_Context* cpA,
						    struct bbf_ScanDetector* ptrA, 
						    const struct bbf_ScanDetector* srcPtrA )
{
	bbs_ERROR0( "bbf_ScanDetector_copy:\n Function is not available" );
}

/* ------------------------------------------------------------------------- */

flag bbf_ScanDetector_equal( struct bbs_Context* cpA,
						     const struct bbf_ScanDetector* ptrA, 
						     const struct bbf_ScanDetector* srcPtrA )
{
	bbs_ERROR0( "bbf_ScanDetector_equal:\n Function is not available" );
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
	
uint32 bbf_ScanDetector_memSize( struct bbs_Context* cpA,
							     const struct bbf_ScanDetector* ptrA )
{
	uint32 iL;
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bbs_SIZEOF16( ptrA->patchWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->patchHeightE );
	memSizeL += bbs_SIZEOF16( ptrA->minDefScaleE );
	memSizeL += bbs_SIZEOF16( ptrA->maxDefScaleE );
	memSizeL += bbs_SIZEOF16( ptrA->scaleStepE );
	memSizeL += bbs_SIZEOF16( ptrA->overlapThrE );
	memSizeL += bbs_SIZEOF16( ptrA->borderWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->borderHeightE );
	memSizeL += bbs_SIZEOF16( ptrA->featuresE );
	for( iL = 0; iL < ptrA->featuresE; iL++ ) memSizeL += bbf_BitParam_memSize( cpA, &ptrA->bitParamArrE[ iL ] );
	for( iL = 0; iL < ptrA->featuresE; iL++ ) memSizeL += bbf_Sequence_memSize( cpA, &ptrA->featureArrE[ iL ] );
	memSizeL += bts_IdCluster2D_memSize( cpA, &ptrA->refClusterE );
	memSizeL += bbs_SIZEOF16( ptrA->refDistanceE );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_ScanDetector_memWrite( struct bbs_Context* cpA,
							      const struct bbf_ScanDetector* ptrA, 
								  uint16* memPtrA )
{
	uint32 iL;
	uint32 memSizeL = bbf_ScanDetector_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bbf_SCAN_DETECTOR_VERSION, memPtrA );

	memPtrA += bbs_memWrite32( &ptrA->patchWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->patchHeightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->minDefScaleE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxDefScaleE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->scaleStepE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->overlapThrE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->borderWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->borderHeightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->featuresE, memPtrA );
	for( iL = 0; iL < ptrA->featuresE; iL++ ) memPtrA += bbf_BitParam_memWrite( cpA, &ptrA->bitParamArrE[ iL ], memPtrA );
	for( iL = 0; iL < ptrA->featuresE; iL++ ) memPtrA += bbf_Sequence_memWrite( cpA, &ptrA->featureArrE[ iL ], memPtrA );
	memPtrA += bts_IdCluster2D_memWrite( cpA, &ptrA->refClusterE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->refDistanceE, memPtrA );

	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bbf_ScanDetector_memRead( struct bbs_Context* cpA,
							     struct bbf_ScanDetector* ptrA, 
							     const uint16* memPtrA, 
							     struct bbs_MemTbl* mtpA )
{
	bbs_DEF_fNameL( "bbf_ScanDetector_memRead" )

	/* debugging hint: set this flag to FALSE when you suspect a shared memory conflict */
	const flag maximizeSharedMemoryL = TRUE;

	uint32 iL;
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_segPtr( cpA, &memTblL, 0 );
	if( bbs_Context_error( cpA ) ) return 0;

	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bbf_SCAN_DETECTOR_VERSION, memPtrA );

	memPtrA += bbs_memRead32( &ptrA->patchWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->patchHeightE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->minDefScaleE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxDefScaleE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->scaleStepE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->overlapThrE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->borderWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->borderHeightE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->featuresE, memPtrA );
	for( iL = 0; iL < ptrA->featuresE; iL++ ) memPtrA += bbf_BitParam_memRead( cpA, &ptrA->bitParamArrE[ iL ], memPtrA );
	for( iL = 0; iL < ptrA->featuresE; iL++ ) memPtrA += bbf_Sequence_memRead( cpA, &ptrA->featureArrE[ iL ], memPtrA, &memTblL );
	memPtrA += bts_IdCluster2D_memRead( cpA, &ptrA->refClusterE, memPtrA, espL );
	memPtrA += bbs_memRead32( &ptrA->refDistanceE, memPtrA );
/*
	if( memSizeL != bbf_ScanDetector_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_ScanDetector_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
			        "size mismatch" );
		return 0;
	}
*/

	ptrA->minScaleE = ptrA->minDefScaleE;
	ptrA->maxScaleE = ptrA->maxDefScaleE;

	/* initialize scanner; be aware of shared memory settings(!) */
	{
		uint32 maxImageSizeL = ptrA->maxImageWidthE * ptrA->maxImageHeightE;

		/* estimate of maximal possible faces in image */
		uint32 maxFacesL = maxImageSizeL / ( 768 << 1 );

		uint32 maxRadiusL = 0;

		if( maxImageSizeL == 0 ) 
		{
			bbs_ERROR1( "%s:\nMaximum image size was not defined (size variables must be set before calling _memRead)", fNameL );
			return memSizeL; 
		}

		for( iL = 0; iL < ptrA->featuresE; iL++ ) 
		{
			maxRadiusL = maxRadiusL > ptrA->bitParamArrE[ iL ].outerRadiusE ? maxRadiusL : ptrA->bitParamArrE[ iL ].outerRadiusE;
		}

		if( maxFacesL < 1 ) maxFacesL = 1;

		bbf_Scanner_create( cpA, &ptrA->scannerE, 
							maximizeSharedMemoryL,
							ptrA->maxImageWidthE,
							ptrA->maxImageHeightE,
							maxRadiusL, 
							ptrA->patchWidthE,
							ptrA->patchHeightE,
							ptrA->minScaleE,
							ptrA->maxScaleE,
							ptrA->scaleStepE,
							ptrA->borderWidthE,
							ptrA->borderHeightE,
							maxFacesL * 20,  /* bufferSizeA */
							&memTblL );
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

uint32 bbf_ScanDetector_process( struct bbs_Context* cpA, 
							     struct bbf_ScanDetector* ptrA,
							     const void* imagePtrA,
								 uint32 imageWidthA,
								 uint32 imageHeightA,
								 const struct bts_Int16Rect* roiPtrA,
								 int32** outArrPtrPtrA )
{
	/* best global values (used when no positives could be found) */
	int32 bestGlobalActL = ( int32 )0x80000000;
	int32 bestGlobalXL = 0;
	int32 bestGlobalYL = 0;
	uint32 bestGlobalScaleL = 0;

	struct bbf_Scanner* scannerPtrL = &ptrA->scannerE;

	scannerPtrL->minScaleE = ptrA->minScaleE;
	scannerPtrL->maxScaleE = ptrA->maxScaleE;

	*outArrPtrPtrA = NULL;

	if( bbs_Context_error( cpA ) ) return 0;
	if( ptrA->featuresE == 0 )
	{
		bbs_ERROR0( "bbf_ScanDetector_process: detector has no features" );
		return 0;
	}

	if( imageWidthA > ptrA->maxImageWidthE || imageHeightA > ptrA->maxImageHeightE )
	{
		bbs_ERROR0( "bbf_ScanDetector_process: images size exceeds preallocated size" );
		return 0;
	}

	/* resets output positions */
	bbf_Scanner_resetOutPos( cpA, scannerPtrL ); 

	/* assign image to scanner - reset scanner */
	bbf_Scanner_assign( cpA, scannerPtrL, imagePtrA, imageWidthA, imageHeightA, roiPtrA, &ptrA->bitParamArrE[ 0 ] );

	while( bbf_Scanner_positions( scannerPtrL ) > 0 )
	{
		int32 bestActL = ( int32 )0x80000000;
		uint32 bestIdxL = 0;
		uint32 bestLvlL = 0;
		uint32 iL;

		const struct bbf_Feature* featurePtrL = ( const struct bbf_Feature* )&ptrA->featureArrE[ 0 ];
		const struct bbf_BitParam* paramPtrL = &ptrA->bitParamArrE[ 0 ];
		bbf_Scanner_bitParam( cpA, scannerPtrL, paramPtrL );

		/* resets internal positions */
		bbf_Scanner_resetIntPos( cpA, scannerPtrL );

		do
		{
			int32 actL = featurePtrL->vpActivityE( featurePtrL, bbf_Scanner_getPatch( scannerPtrL ) );
			if( actL > 0 ) 
			{
				bbf_Scanner_addIntPos( cpA, scannerPtrL, bbf_Scanner_scanIndex( scannerPtrL ), actL );
			}
			
			if( actL > bestActL )
			{
				bestActL = actL;
				bestIdxL = bbf_Scanner_scanIndex( scannerPtrL );
			}
		}
		while( bbf_Scanner_next( cpA, scannerPtrL ) );

		for( iL = 1; iL < ptrA->featuresE; iL++ )
		{
			const struct bbf_Feature* featurePtrL = ( const struct bbf_Feature* )&ptrA->featureArrE[ iL ];
			const struct bbf_BitParam* paramPtrL = &ptrA->bitParamArrE[ iL ];
			uint32* idxArrL = scannerPtrL->idxArrE.arrPtrE;
			int32* actArrL = scannerPtrL->actArrE.arrPtrE;

			uint32 kL = 0;
			uint32 jL;

			if( scannerPtrL->intCountE == 0 ) break;
			bestActL = ( int32 )0x80000000;
			bbf_Scanner_bitParam( cpA, scannerPtrL, paramPtrL );

			for( jL = 0; jL < scannerPtrL->intCountE; jL++ )
			{
				int32 actL;
				bbf_Scanner_goToIndex( cpA, scannerPtrL, idxArrL[ jL ] );
				actL = featurePtrL->vpActivityE( featurePtrL, bbf_Scanner_getPatch( scannerPtrL ) );
				if( actL > 0 )
				{
					idxArrL[ kL ] = idxArrL[ jL ];
					actArrL[ kL ] = ( actArrL[ jL ] + actL ) >> 1;
					kL++;
				}

				if( actL > bestActL )
				{
					bestActL = actL;
					bestIdxL = idxArrL[ jL ];
					bestLvlL = iL;
				}
			}

			scannerPtrL->intCountE = kL;
		}

		if( scannerPtrL->intCountE == 0 )
		{
			int32 xL, yL;
			uint32 scaleL;

			/* 8.24 */
			int32 actL = ( bestActL >> 4 ) + ( ( ( int32 )( bestLvlL + 1 - ptrA->featuresE ) << 24 ) / ( int32 )ptrA->featuresE );

			/* 4.28 */
			actL <<= 4;

			bbf_Scanner_idxPos( scannerPtrL, bestIdxL, &xL, &yL, &scaleL );

			if( actL > bestGlobalActL )
			{
            	bestGlobalActL = actL;
				bestGlobalXL = xL;
				bestGlobalYL = yL;
				bestGlobalScaleL = scaleL;
			}
		}
		else
		{
			/* remove overlaps for current scale */
			bbf_Scanner_removeIntOverlaps( cpA, scannerPtrL, ptrA->overlapThrE );

			for( iL = 0; iL < scannerPtrL->intCountE; iL++ )
			{
				int32 xL, yL;
				uint32 scaleL;
				uint32* idxArrL = scannerPtrL->idxArrE.arrPtrE;
				int32* actArrL = scannerPtrL->actArrE.arrPtrE;

				int32 actL = actArrL[ iL ];
				bbf_Scanner_idxPos( scannerPtrL, idxArrL[ iL ], &xL, &yL, &scaleL );

				/* add external position */
				bbf_Scanner_addOutPos( cpA, scannerPtrL, xL, yL, scaleL, actL ); 
			}

			/* remove overlapping positions */
			bbf_Scanner_removeOutOverlaps( cpA, scannerPtrL, ptrA->overlapThrE ); 

		}

		if( !bbf_Scanner_nextScale( cpA, scannerPtrL ) ) break;
	}
/*
	{
		uint32 iL;
		printf( "\n-----------------------------------------------" );
		for( iL = 0; iL < scannerPtrL->outCountE; iL++ )
		{
			printf( "\n%02i: %6.1f %6.1f %6.2f %6.3f", 
					iL,
					( float )scannerPtrL->outArrE.arrPtrE[ iL * 4 + 0 ] / ( 1L << 16 ),
					( float )scannerPtrL->outArrE.arrPtrE[ iL * 4 + 1 ] / ( 1L << 16 ),
					( float )scannerPtrL->outArrE.arrPtrE[ iL * 4 + 2 ] / ( 1L << 20 ),
					( float )scannerPtrL->outArrE.arrPtrE[ iL * 4 + 3 ] / ( 1L << 28 ) );

		}
	}
*/

	*outArrPtrPtrA = scannerPtrL->outArrE.arrPtrE;
	if( scannerPtrL->outCountE == 0 )
	{
		/* no positive activities found: store best negative activity */
		bbf_Scanner_addOutPos( cpA, scannerPtrL, bestGlobalXL, bestGlobalYL, bestGlobalScaleL, bestGlobalActL );
		return 0;
	}
	else
	{
		return scannerPtrL->outCountE;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

