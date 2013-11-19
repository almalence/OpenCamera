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
#include "b_ImageEm/Functions.h"
#include "b_BitFeatureEm/LocalScanDetector.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/** applies PCA mapping 
 *  Input and output clusters may be identical
 */
void bbf_LocalScanDetector_pcaMap( struct bbs_Context* cpA,
								   const struct bbf_LocalScanDetector* ptrA, 
								   const struct bts_IdCluster2D* inClusterPtrA,
								   struct bts_IdCluster2D* outClusterPtrA )
{
	bbs_DEF_fNameL( "bbf_LocalScanDetector_pcaMap" )

	struct bts_Cluster2D* tmpCl1PtrL  = ( struct bts_Cluster2D* )&ptrA->tmpCluster1E;
	struct bts_Cluster2D* tmpCl2PtrL  = ( struct bts_Cluster2D* )&ptrA->tmpCluster2E;
	struct bts_RBFMap2D*  rbfPtrL     = ( struct bts_RBFMap2D* )&ptrA->rbfMapE;
	struct bts_Flt16Alt2D altL;
	uint32 outBbpL = inClusterPtrA->clusterE.bbpE;
	uint32 iL, jL;

	/* setup two equivalent clusters holding the essential (alt-free) moves to be handled by PCA */
	bts_IdCluster2D_convertToEqivalentClusters( cpA, 
												inClusterPtrA,
												&ptrA->pcaClusterE,
												tmpCl1PtrL,
												tmpCl2PtrL );

	altL = bts_Cluster2D_alt( cpA, tmpCl1PtrL, tmpCl2PtrL, bts_ALT_RIGID );
	bts_Cluster2D_transform( cpA, tmpCl1PtrL, altL );
	bts_RBFMap2D_compute( cpA, rbfPtrL, tmpCl2PtrL, tmpCl1PtrL );
	bts_RBFMap2D_mapCluster( cpA, rbfPtrL, &ptrA->pcaClusterE.clusterE, tmpCl1PtrL, 6/* ! */ );

	/* PCA projection: cluster1 -> cluster1 */
	{
		/* mat elements: 8.8 */
		const int16* matPtrL = ptrA->pcaMatE.arrPtrE;
		
		/* same bbp as pca cluster */
		const int16* avgPtrL = ptrA->pcaAvgE.arrPtrE;

		struct bts_Int16Vec2D* vecArrL = tmpCl1PtrL->vecArrE;

		/* projected vector */
		int32 prjVecL[ bpi_LOCAL_SCAN_DETECTOR_MAX_PCA_DIM ];

		/* width of matrix */
		uint16 matWidthL = tmpCl1PtrL->sizeE * 2;

		if( ptrA->pcaDimSubSpaceE > bpi_LOCAL_SCAN_DETECTOR_MAX_PCA_DIM )
		{
			bbs_ERROR1( "%s:\nbpi_RF_LANDMARKER_MAX_PCA_DIM exceeded", fNameL );
			return;
		}

		/* forward trafo */
		for( iL = 0; iL < ptrA->pcaDimSubSpaceE; iL++ )
		{
			int32 sumL = 0;
			avgPtrL = ptrA->pcaAvgE.arrPtrE;
			for( jL = 0; jL < tmpCl1PtrL->sizeE; jL++ )
			{
				sumL += matPtrL[ 0 ] * ( vecArrL[ jL ].xE - avgPtrL[ 0 ] );
				sumL += matPtrL[ 1 ] * ( vecArrL[ jL ].yE - avgPtrL[ 1 ] );
				avgPtrL += 2;
				matPtrL += 2;
			}
			prjVecL[ iL ] = ( sumL + 128 ) >> 8;
		}

		matPtrL = ptrA->pcaMatE.arrPtrE;
		avgPtrL = ptrA->pcaAvgE.arrPtrE;
		vecArrL = tmpCl1PtrL->vecArrE;

		/* backward trafo */
		for( jL = 0; jL < tmpCl1PtrL->sizeE; jL++ )
		{
			int32 sumL = 0;
			for( iL = 0; iL < ptrA->pcaDimSubSpaceE; iL++ )
			{
				sumL += matPtrL[ iL * matWidthL + 0 ] * prjVecL[ iL ];
			}

			vecArrL[ jL ].xE = ( ( sumL + 128 ) >> 8 ) + avgPtrL[ 0 ];

			sumL = 0;
			for( iL = 0; iL < ptrA->pcaDimSubSpaceE; iL++ )
			{
				sumL += matPtrL[ iL * matWidthL + 1 ] * prjVecL[ iL ];
			}

			vecArrL[ jL ].yE = ( ( sumL + 128 ) >> 8 ) + avgPtrL[ 1 ];

			matPtrL += 2;
			avgPtrL += 2;
		}
	}

	/* ALT backtransformation */
	bts_IdCluster2D_copy( cpA, outClusterPtrA, &ptrA->pcaClusterE ); 
	bts_Cluster2D_copyTransform( cpA, &outClusterPtrA->clusterE, tmpCl1PtrL, bts_Flt16Alt2D_inverted( &altL ), outBbpL );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_LocalScanDetector_init( struct bbs_Context* cpA,
							     struct bbf_LocalScanDetector* ptrA )
{
	bbs_memset16( ptrA->ftrPtrArrE, 0, bbs_SIZEOF16( ptrA->ftrPtrArrE ) );
	bts_RBFMap2D_init( cpA, &ptrA->rbfMapE );
	bts_Cluster2D_init( cpA, &ptrA->tmpCluster1E ); 
	bts_Cluster2D_init( cpA, &ptrA->tmpCluster2E ); 
	bts_Cluster2D_init( cpA, &ptrA->tmpCluster3E ); 
	bts_Cluster2D_init( cpA, &ptrA->tmpCluster4E ); 
	bbf_LocalScanner_init( cpA, &ptrA->scannerE );
	bbs_Int32Arr_init( cpA, &ptrA->actArrE );
	bbs_Int16Arr_init( cpA, &ptrA->idxArrE );
	bbs_UInt8Arr_init( cpA, &ptrA->workImageBufE );
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;

	ptrA->patchWidthE = 0;
	ptrA->patchHeightE = 0;
	ptrA->scanWidthE = 0;
	ptrA->scanHeightE = 0;
	ptrA->scaleExpE = 0;
	ptrA->interpolatedWarpingE = TRUE;
	ptrA->warpScaleThresholdE = 0;
	bts_IdCluster2D_init( cpA, &ptrA->refClusterE );
	bts_Cluster2D_init( cpA, &ptrA->scanClusterE );
	bbs_UInt16Arr_init( cpA, &ptrA->ftrDataArrE );
	bbf_BitParam_init( cpA, &ptrA->bitParamE );
	ptrA->outlierDistanceE = 0;
	bts_IdCluster2D_init( cpA, &ptrA->pcaClusterE );
	bbs_Int16Arr_init( cpA, &ptrA->pcaAvgE );
	bbs_Int16Arr_init( cpA, &ptrA->pcaMatE );
	ptrA->pcaDimSubSpaceE = 0;
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
}

/* ------------------------------------------------------------------------- */

void bbf_LocalScanDetector_exit( struct bbs_Context* cpA,
							     struct bbf_LocalScanDetector* ptrA )
{
	uint16 iL;
	for( iL = 0; iL < ptrA->scanClusterE.sizeE; iL++ ) bbf_featureExit( cpA, ptrA->ftrPtrArrE[ iL ] );
	bbs_memset16( ptrA->ftrPtrArrE, 0, bbs_SIZEOF16( ptrA->ftrPtrArrE ) );

	bts_RBFMap2D_exit( cpA, &ptrA->rbfMapE );
	bts_Cluster2D_exit( cpA, &ptrA->tmpCluster1E ); 
	bts_Cluster2D_exit( cpA, &ptrA->tmpCluster2E ); 
	bts_Cluster2D_exit( cpA, &ptrA->tmpCluster3E ); 
	bts_Cluster2D_exit( cpA, &ptrA->tmpCluster4E ); 
	bbf_LocalScanner_exit( cpA, &ptrA->scannerE );
	bbs_Int32Arr_exit( cpA, &ptrA->actArrE );
	bbs_Int16Arr_exit( cpA, &ptrA->idxArrE );
	bbs_UInt8Arr_exit( cpA, &ptrA->workImageBufE );
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;

	ptrA->patchWidthE = 0;
	ptrA->patchHeightE = 0;
	ptrA->scanWidthE = 0;
	ptrA->scanHeightE = 0;
	ptrA->scaleExpE = 0;
	ptrA->interpolatedWarpingE = TRUE;
	ptrA->warpScaleThresholdE = 0;
	bts_IdCluster2D_exit( cpA, &ptrA->refClusterE );
	bts_Cluster2D_exit( cpA, &ptrA->scanClusterE );
	bbs_UInt16Arr_exit( cpA, &ptrA->ftrDataArrE );
	bbf_BitParam_exit( cpA, &ptrA->bitParamE );
	ptrA->outlierDistanceE = 0;
	bts_IdCluster2D_exit( cpA, &ptrA->pcaClusterE );
	bbs_Int16Arr_exit( cpA, &ptrA->pcaAvgE );
	bbs_Int16Arr_exit( cpA, &ptrA->pcaMatE );
	ptrA->pcaDimSubSpaceE = 0;
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_LocalScanDetector_copy( struct bbs_Context* cpA,
						    struct bbf_LocalScanDetector* ptrA, 
						    const struct bbf_LocalScanDetector* srcPtrA )
{
	bbs_ERROR0( "bbf_LocalScanDetector_copy:\n Function is not available" );
}

/* ------------------------------------------------------------------------- */

flag bbf_LocalScanDetector_equal( struct bbs_Context* cpA,
						     const struct bbf_LocalScanDetector* ptrA, 
						     const struct bbf_LocalScanDetector* srcPtrA )
{
	bbs_ERROR0( "bbf_LocalScanDetector_equal:\n Function is not available" );
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
	
uint32 bbf_LocalScanDetector_memSize( struct bbs_Context* cpA,
								      const struct bbf_LocalScanDetector* ptrA )
{
	uint32 iL;
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bbs_SIZEOF16( ptrA->patchWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->patchHeightE );
	memSizeL += bbs_SIZEOF16( ptrA->scanWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->scanHeightE );
	memSizeL += bbs_SIZEOF16( ptrA->scaleExpE );
	memSizeL += bbs_SIZEOF16( ptrA->interpolatedWarpingE );
	memSizeL += bbs_SIZEOF16( ptrA->warpScaleThresholdE );
	memSizeL += bts_IdCluster2D_memSize( cpA, &ptrA->refClusterE );
	memSizeL += bts_Cluster2D_memSize( cpA, &ptrA->scanClusterE );
	memSizeL += bbf_BitParam_memSize( cpA, &ptrA->bitParamE );
	memSizeL += bbs_SIZEOF16( ptrA->outlierDistanceE );
	memSizeL += bts_IdCluster2D_memSize( cpA, &ptrA->pcaClusterE );
	memSizeL += bbs_Int16Arr_memSize( cpA, &ptrA->pcaAvgE );
	memSizeL += bbs_Int16Arr_memSize( cpA, &ptrA->pcaMatE );
	memSizeL += bbs_SIZEOF16( ptrA->pcaDimSubSpaceE );
	memSizeL += bbs_SIZEOF16( ptrA->maxImageWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->maxImageHeightE );
	for( iL = 0; iL < ptrA->scanClusterE.sizeE; iL++ ) memSizeL += bbf_featureMemSize( cpA, ptrA->ftrPtrArrE[ iL ] );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_LocalScanDetector_memWrite( struct bbs_Context* cpA,
									   const struct bbf_LocalScanDetector* ptrA, 
									   uint16* memPtrA )
{
	uint32 iL;
	uint32 memSizeL = bbf_LocalScanDetector_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bbf_LOCAL_SCAN_DETECTOR_VERSION, memPtrA );

	memPtrA += bbs_memWrite32( &ptrA->patchWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->patchHeightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->scanWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->scanHeightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->scaleExpE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->interpolatedWarpingE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->warpScaleThresholdE, memPtrA );
	memPtrA += bts_IdCluster2D_memWrite( cpA, &ptrA->refClusterE, memPtrA );
	memPtrA += bts_Cluster2D_memWrite( cpA, &ptrA->scanClusterE, memPtrA );
	memPtrA += bbf_BitParam_memWrite( cpA, &ptrA->bitParamE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->outlierDistanceE, memPtrA );
	memPtrA += bts_IdCluster2D_memWrite( cpA, &ptrA->pcaClusterE, memPtrA );
	memPtrA += bbs_Int16Arr_memWrite( cpA, &ptrA->pcaAvgE, memPtrA );
	memPtrA += bbs_Int16Arr_memWrite( cpA, &ptrA->pcaMatE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->pcaDimSubSpaceE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxImageWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxImageHeightE, memPtrA );

	for( iL = 0; iL < ptrA->scanClusterE.sizeE; iL++ ) memPtrA += bbf_featureMemWrite( cpA, ptrA->ftrPtrArrE[ iL ], memPtrA );

	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bbf_LocalScanDetector_memRead( struct bbs_Context* cpA,
									  struct bbf_LocalScanDetector* ptrA, 
									  const uint16* memPtrA, 
									  struct bbs_MemTbl* mtpA )
{
	uint32 iL;
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_segPtr( cpA, &memTblL, 0 );
	struct bbs_MemSeg* sspL = bbs_MemTbl_sharedSegPtr( cpA, &memTblL, 0 );
	if( bbs_Context_error( cpA ) ) return 0;

	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bbf_LOCAL_SCAN_DETECTOR_VERSION, memPtrA );


	memPtrA += bbs_memRead32( &ptrA->patchWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->patchHeightE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->scanWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->scanHeightE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->scaleExpE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->interpolatedWarpingE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->warpScaleThresholdE, memPtrA );
	memPtrA += bts_IdCluster2D_memRead( cpA, &ptrA->refClusterE, memPtrA, espL );
	memPtrA += bts_Cluster2D_memRead( cpA, &ptrA->scanClusterE, memPtrA, espL );
	memPtrA += bbf_BitParam_memRead( cpA, &ptrA->bitParamE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->outlierDistanceE, memPtrA );
	memPtrA += bts_IdCluster2D_memRead( cpA, &ptrA->pcaClusterE, memPtrA, espL );
	memPtrA += bbs_Int16Arr_memRead( cpA, &ptrA->pcaAvgE, memPtrA, espL );
	memPtrA += bbs_Int16Arr_memRead( cpA, &ptrA->pcaMatE, memPtrA, espL );
	memPtrA += bbs_memRead32( &ptrA->pcaDimSubSpaceE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxImageWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxImageHeightE, memPtrA );

	/* check features & allocate data buffer */
	{
		const uint16* memPtrL = memPtrA;
		uint32 dataSizeL = 0;
		for( iL = 0; iL < ptrA->scanClusterE.sizeE; iL++ )
		{
			enum bbf_FeatureType typeL = ( enum bbf_FeatureType )bbs_memPeek32( memPtrL + 4 );
			dataSizeL += bbf_featureSizeOf16( cpA, typeL );
			memPtrL += bbs_memPeek32( memPtrL );
		}
		bbs_UInt16Arr_create( cpA, &ptrA->ftrDataArrE, dataSizeL, espL );
	}

	/* load features & initialize pointers */
	{
		uint16* dataPtrL = ptrA->ftrDataArrE.arrPtrE;
		for( iL = 0; iL < ptrA->scanClusterE.sizeE; iL++ )
		{
			enum bbf_FeatureType typeL = ( enum bbf_FeatureType )bbs_memPeek32( memPtrA + 4 );
			ptrA->ftrPtrArrE[ iL ] = ( struct bbf_Feature* )dataPtrL;
			bbf_featureInit( cpA, ptrA->ftrPtrArrE[ iL ], typeL );
			memPtrA += bbf_featureMemRead( cpA, ptrA->ftrPtrArrE[ iL ], memPtrA, &memTblL );
			dataPtrL += bbf_featureSizeOf16( cpA, typeL );
		}
	}

	if( memSizeL != bbf_LocalScanDetector_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_LocalScanDetector_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
			        "size mismatch" );
		return 0;
	}

	if( ptrA->maxImageWidthE * ptrA->maxImageHeightE == 0 )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_LocalScanDetector_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
								        "maximum image width/height not set" );
		return 0;
	}

	/* initialize internal data */

	/* ought to be placed on shared memory later */
	bts_RBFMap2D_create( cpA, &ptrA->rbfMapE, bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE, sspL );
	ptrA->rbfMapE.RBFTypeE = bts_RBF_LINEAR;
	ptrA->rbfMapE.altTypeE = bts_ALT_RIGID;

	bts_Cluster2D_create( cpA, &ptrA->tmpCluster1E, bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE, sspL ); 
	bts_Cluster2D_create( cpA, &ptrA->tmpCluster2E, bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE, sspL );
	bts_Cluster2D_create( cpA, &ptrA->tmpCluster3E, bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE, sspL );
	bts_Cluster2D_create( cpA, &ptrA->tmpCluster4E, bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE, sspL );

	bbs_Int32Arr_create( cpA, &ptrA->actArrE, bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE, sspL );
	bbs_Int16Arr_create( cpA, &ptrA->idxArrE, bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE, sspL );

	/* working image memory */
	/* ought to be placed on shared memory later */
	bbs_UInt8Arr_create( cpA, &ptrA->workImageBufE, ptrA->maxImageWidthE * ptrA->maxImageHeightE, sspL );

	/* initialize local scanner (be aware of shared memory usage when moving this create function) */
	bbf_LocalScanner_create( cpA, &ptrA->scannerE,
							 ptrA->patchWidthE,
							 ptrA->patchHeightE,
							 ptrA->scaleExpE,
							 ptrA->maxImageWidthE,
							 ptrA->maxImageHeightE,
							 ptrA->scaleExpE,
							 ptrA->bitParamE.outerRadiusE,
							 &memTblL );

	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

int32 bbf_LocalScanDetector_process( struct bbs_Context* cpA,
									 const struct bbf_LocalScanDetector* ptrA, 
                                     uint8* imagePtrA, 
									 uint32 imageWidthA,
									 uint32 imageHeightA,
									 const struct bts_Int16Vec2D*  offsPtrA,
									 const struct bts_IdCluster2D* inClusterPtrA,
									 struct bts_IdCluster2D* outClusterPtrA )
{
	bbs_DEF_fNameL( "bbf_LocalScanDetector_process" )

	int32 pw0L = ptrA->patchWidthE;
	int32 ph0L = ptrA->patchHeightE;
	int32 pw1L = pw0L << ptrA->scaleExpE;
	int32 ph1L = ph0L << ptrA->scaleExpE;

	struct bts_Cluster2D* wrkClPtrL  = ( struct bts_Cluster2D* )&ptrA->tmpCluster1E;
	struct bts_Cluster2D* refClPtrL  = ( struct bts_Cluster2D* )&ptrA->tmpCluster2E;
	struct bts_Cluster2D* dstClPtrL  = ( struct bts_Cluster2D* )&ptrA->tmpCluster3E;
	struct bts_Cluster2D* tmpClPtrL  = ( struct bts_Cluster2D* )&ptrA->tmpCluster4E;
	struct bts_RBFMap2D*  rbfPtrL    = ( struct bts_RBFMap2D* )&ptrA->rbfMapE;
	struct bbf_LocalScanner* scnPtrL = ( struct bbf_LocalScanner* )&ptrA->scannerE;

	int32* actArrL = ( int32* )ptrA->actArrE.arrPtrE;
	int16* idxArrL = ( int16* )ptrA->idxArrE.arrPtrE;

	uint32 workImageWidthL, workImageHeightL;

	struct bts_Flt16Alt2D altL;

	int32 confidenceL;
	uint32 iL;
	uint32 sizeL = ptrA->scanClusterE.sizeE;

	if( sizeL > bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE )
	{
		bbs_ERROR1( "%s:\nScan cluster size exceeds bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE", fNameL );
		return 0;
	}

	/* compute equivalent clusters (matching ids) from input and reference cluster */
	bts_IdCluster2D_convertToEqivalentClusters( cpA, inClusterPtrA, &ptrA->refClusterE, wrkClPtrL, refClPtrL );

	/* altL: orig image -> normalized image */
	altL = bts_Cluster2D_alt( cpA, wrkClPtrL, refClPtrL, bts_ALT_RIGID );

	/* transorm work cluster to normalized image */
	bts_Cluster2D_transformBbp( cpA, wrkClPtrL, altL, 6 );

	/* map: ref cluster -> work cluster */
	bts_RBFMap2D_compute( cpA, rbfPtrL, refClPtrL, wrkClPtrL );

	/* copy: scanClusterE -> work cluster */
	bts_Cluster2D_copy( cpA, wrkClPtrL, &ptrA->scanClusterE );

	/* copy: refClusterE -> ref cluster */
	bts_Cluster2D_copy( cpA, refClPtrL, &ptrA->refClusterE.clusterE );

	/* apply map to work cluster */
	bts_Cluster2D_rbfTransform( cpA, wrkClPtrL, rbfPtrL );

	/* apply map to ref cluster */
	bts_Cluster2D_rbfTransform( cpA, refClPtrL, rbfPtrL );

	{
		/* analyze boundaries; get exact dimensions of working image */
		int32 workBorderWL = ( ( ptrA->scanWidthE  + pw1L + 1 ) >> 1 ) + 1; /* add a pixel to ensure full search area */
		int32 workBorderHL = ( ( ptrA->scanHeightE + ph1L + 1 ) >> 1 ) + 1; /* add a pixel to ensure full search area */
		struct bts_Int16Rect workAreaL = bts_Cluster2D_boundingBox( cpA, wrkClPtrL );
		workAreaL.x1E = workAreaL.x1E >> wrkClPtrL->bbpE;
		workAreaL.y1E = workAreaL.y1E >> wrkClPtrL->bbpE;
		workAreaL.x2E = workAreaL.x2E >> wrkClPtrL->bbpE;
		workAreaL.y2E = workAreaL.y2E >> wrkClPtrL->bbpE;
		workAreaL.x1E -= workBorderWL;
		workAreaL.y1E -= workBorderHL;
		workAreaL.x2E += workBorderWL;
		workAreaL.y2E += workBorderHL;

		workImageWidthL  = workAreaL.x2E - workAreaL.x1E;
		workImageHeightL = workAreaL.y2E - workAreaL.y1E;

		/* truncate if necessary (should not occur in normal operation) */
		workImageWidthL = workImageWidthL > ptrA->maxImageWidthE ? ptrA->maxImageWidthE : workImageWidthL;
		workImageHeightL = workImageHeightL > ptrA->maxImageHeightE ? ptrA->maxImageHeightE : workImageHeightL;

		/* adjust ALT */
		altL.vecE.xE -= workAreaL.x1E << altL.vecE.bbpE;
		altL.vecE.yE -= workAreaL.y1E << altL.vecE.bbpE;

		/* adjust work cluster */
		for( iL = 0; iL < wrkClPtrL->sizeE; iL++ )
		{
			wrkClPtrL->vecArrE[ iL ].xE -= workAreaL.x1E << wrkClPtrL->bbpE;
			wrkClPtrL->vecArrE[ iL ].yE -= workAreaL.y1E << wrkClPtrL->bbpE;
		}

		/* adjust ref cluster */
		for( iL = 0; iL < wrkClPtrL->sizeE; iL++ )
		{
			refClPtrL->vecArrE[ iL ].xE -= workAreaL.x1E << refClPtrL->bbpE;
			refClPtrL->vecArrE[ iL ].yE -= workAreaL.y1E << refClPtrL->bbpE;
		}

		/* transform image */
		bim_filterWarp( cpA, 
					    ptrA->workImageBufE.arrPtrE, 
						imagePtrA, imageWidthA, imageHeightA, 
						offsPtrA,
						&altL, 
						workImageWidthL, workImageHeightL, 
						NULL, 
						ptrA->warpScaleThresholdE, 
						ptrA->interpolatedWarpingE );

	}

	/* scan over all positions of work cluster; target positions are stored in *dstClPtrL*/
	{
		int32 regionWHL = ( ptrA->scanWidthE  + pw1L + 1 ) >> 1;
		int32 regionHHL = ( ptrA->scanHeightE + ph1L + 1 ) >> 1;
		struct bts_Int16Vec2D* srcVecArrL = wrkClPtrL->vecArrE;
		struct bts_Int16Vec2D* dstVecArrL = dstClPtrL->vecArrE;
		int32 vecBbpL = wrkClPtrL->bbpE;
		bts_Cluster2D_size( cpA, dstClPtrL, sizeL );
		dstClPtrL->bbpE = vecBbpL;

		/* initialize scanner */
		scnPtrL->patchWidthE = ptrA->patchWidthE;
		scnPtrL->patchHeightE = ptrA->patchWidthE;
		scnPtrL->scaleExpE = ptrA->scaleExpE;

		bbf_LocalScanner_assign( cpA, scnPtrL, ptrA->workImageBufE.arrPtrE, workImageWidthL, workImageHeightL, &ptrA->bitParamE );

		bbs_memset32( actArrL, 0x80000000, sizeL );

		do
		{
			for( iL = 0; iL < sizeL; iL++ )
			{
				int32 bestActL = 0x80000000;
				uint32 bestIdxL = 0;
				struct bbf_Feature* ftrPtrL = ptrA->ftrPtrArrE[ iL ];

				/* set scan region */
				{
					int32 x0L = ( ( wrkClPtrL->vecArrE[ iL ].xE >> ( wrkClPtrL->bbpE - 1 ) ) + 1 ) >> 1;
					int32 y0L = ( ( wrkClPtrL->vecArrE[ iL ].yE >> ( wrkClPtrL->bbpE - 1 ) ) + 1 ) >> 1;
					struct bts_Int16Rect scanRegionL = bts_Int16Rect_create( x0L - regionWHL, y0L - regionHHL, x0L + regionWHL, y0L + regionHHL );
					bbf_LocalScanner_origScanRegion( cpA, scnPtrL, &scanRegionL );
				}

				do
				{
					int32 actL = ftrPtrL->vpActivityE( ftrPtrL, bbf_LocalScanner_getPatch( scnPtrL ) );

					if( actL > bestActL )
					{
						bestActL = actL;
						bestIdxL = bbf_LocalScanner_scanIndex( scnPtrL );
					}
				}
				while( bbf_LocalScanner_next( cpA, scnPtrL ) );

				{
					int32 xL, yL; /* 16.16 */
					bbf_LocalScanner_idxPos( scnPtrL, bestIdxL, &xL, &yL );
					xL += pw1L << 15;
					yL += ph1L << 15;
					if( bestActL > actArrL[ iL ] )
					{
						dstVecArrL[ iL ].xE = ( ( xL >> ( 15 - vecBbpL ) ) + 1 ) >> 1;
						dstVecArrL[ iL ].yE = ( ( yL >> ( 15 - vecBbpL ) ) + 1 ) >> 1;
						actArrL[ iL ] = bestActL;
					}
				}
			}
		}
		while( bbf_LocalScanner_nextOffset( cpA, scnPtrL ) );

		/* outlier analysis: outliers are disabled by setting their similarity to -1 */
		if( ptrA->outlierDistanceE > 0 )
		{
			/* altL: work cluster -> ref cluster */
			struct bts_Flt16Alt2D localAltL = bts_Cluster2D_alt( cpA, wrkClPtrL, dstClPtrL, bts_ALT_RIGID );

			/* squared distance 16.16 */
			uint32 dist2L = ( ptrA->outlierDistanceE >> 8 ) * ( ptrA->outlierDistanceE >> 8 );

			/* analyze deviations */
			for( iL = 0; iL < sizeL; iL++ )
			{
				struct bts_Flt16Vec2D vecL = bts_Flt16Vec2D_create32( srcVecArrL[ iL ].xE, srcVecArrL[ iL ].yE, vecBbpL );
				uint32 dev2L; /* squared deviation 16.16 */
				vecL = bts_Flt16Alt2D_mapFlt( &localAltL, &vecL );
				vecL = bts_Flt16Vec2D_sub( vecL, bts_Flt16Vec2D_create32( dstVecArrL[ iL ].xE, dstVecArrL[ iL ].yE, vecBbpL ) );
				dev2L = bbs_convertU32( bts_Flt16Vec2D_norm2( &vecL ), vecL.bbpE << 1, 16 );
				if( dev2L > dist2L ) actArrL[ iL ] = 0xF0000000;
			}
		}

		/* remove undetected positions but keep at least 1/2 best positions */
		{
			flag sortedL;

			/* bubble sort (no speed issue in this case) */
			for( iL = 0; iL < sizeL; iL++ ) idxArrL[ iL ] = iL;

			do
			{
				sortedL = TRUE;
				for( iL = 1; iL < sizeL; iL++ )
				{
					if( actArrL[ idxArrL[ iL - 1 ] ] < actArrL[ idxArrL[ iL ] ] )
					{
						int16 tmpL = idxArrL[ iL - 1 ];
						idxArrL[ iL - 1 ] = idxArrL[ iL ];
						idxArrL[ iL ] = tmpL;
						sortedL = FALSE;
					}
				}
			}
			while( !sortedL );

			for( iL = ( sizeL >> 1 ); iL < sizeL && actArrL[ idxArrL[ iL ] ] >= 0; iL++ );

			{
				uint32 subSizeL = iL;

				/* reorder clusters */
				bts_Cluster2D_size( cpA, tmpClPtrL, subSizeL );
				{
					struct bts_Int16Vec2D* tmpVecArrL = tmpClPtrL->vecArrE;
					for( iL = 0; iL < subSizeL; iL++ ) tmpVecArrL[ iL ] = srcVecArrL[ idxArrL[ iL ] ];
					for( iL = 0; iL < subSizeL; iL++ ) srcVecArrL[ iL ] = tmpVecArrL[ iL ];
					for( iL = 0; iL < subSizeL; iL++ ) tmpVecArrL[ iL ] = dstVecArrL[ idxArrL[ iL ] ];
					for( iL = 0; iL < subSizeL; iL++ ) dstVecArrL[ iL ] = tmpVecArrL[ iL ];
				}
				bts_Cluster2D_size( cpA, wrkClPtrL, subSizeL );
				bts_Cluster2D_size( cpA, dstClPtrL, subSizeL );
			}
		}

		/* compute confidence */
		{
			int16* idxArrL = ptrA->idxArrE.arrPtrE;
			int32* actArrL = ptrA->actArrE.arrPtrE;
			int32 actSumL = 0; /* .20 */
			for( iL = 0; iL < sizeL; iL++ )
			{
				float actL = ( actArrL[ idxArrL[ iL ] ] + 128 ) >> 8;
				if( actL < 0 ) break;
				actSumL += actL;
			}

			/* actSumL = average positive activity */
			actSumL = ( iL > 0 ) ? actSumL / iL : 0;

			confidenceL = ( ( ( int32 )iL << 20 ) - ( ( ( int32 )1 << 20 ) - actSumL ) ) / sizeL;

			/* adjust to 4.28 */
			confidenceL <<= 8;
		}

	}

	/* map: wrkCluster -> dstCluster */
	bts_RBFMap2D_compute( cpA, rbfPtrL, wrkClPtrL, dstClPtrL );

	/* apply map to ref cluster */
	bts_Cluster2D_rbfTransform( cpA, refClPtrL, rbfPtrL );

	/* copy ref cluster to outCluster */
	bts_Cluster2D_copy( cpA, &outClusterPtrA->clusterE, refClPtrL );
	bbs_Int16Arr_copy( cpA, &outClusterPtrA->idArrE, &ptrA->refClusterE.idArrE );

	/* PCA Mapping */
	if( ptrA->pcaDimSubSpaceE > 0 )
	{
		bbf_LocalScanDetector_pcaMap( cpA, ptrA, outClusterPtrA, outClusterPtrA );
	}

	/* backtransform out cluster to original image */
	bts_Cluster2D_transformBbp( cpA, &outClusterPtrA->clusterE, bts_Flt16Alt2D_inverted( &altL ), inClusterPtrA->clusterE.bbpE );

	return confidenceL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

