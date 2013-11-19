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

#ifndef bbf_LOCAL_SCAN_DETECTOR_EM_H
#define bbf_LOCAL_SCAN_DETECTOR_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/UInt32Arr.h"
#include "b_BasicEm/Int16Arr.h"
#include "b_BasicEm/MemTbl.h"
#include "b_TensorEm/IdCluster2D.h"
#include "b_BitFeatureEm/Sequence.h"
#include "b_BitFeatureEm/BitParam.h"
#include "b_BitFeatureEm/LocalScanner.h"
#include "b_TensorEm/RBFMap2D.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bbf_LOCAL_SCAN_DETECTOR_VERSION 100

/* maximum number of features in scan detector */
#define bbf_LOCAL_SCAN_DETECTOR_MAX_FEATURES 16

/* maximum size of any cluster in all processing stages of landmarker */
#define bpi_LOCAL_SCAN_DETECTOR_MAX_CLUSTER_SIZE 24

/* maximum dimension of PCA subspace  */
#define bpi_LOCAL_SCAN_DETECTOR_MAX_PCA_DIM 12

/* ---- object definition -------------------------------------------------- */

/** discrete feature set */
struct bbf_LocalScanDetector 
{
	/* ---- private data --------------------------------------------------- */

	/** feature pointer arrray */
	struct bbf_Feature* ftrPtrArrE[ bbf_LOCAL_SCAN_DETECTOR_MAX_FEATURES ];

	/** multiple purpose rbf map */
	struct bts_RBFMap2D rbfMapE;

	/** temporary cluster */
	struct bts_Cluster2D tmpCluster1E; 

	/** temporary cluster */
	struct bts_Cluster2D tmpCluster2E; 

	/** temporary cluster */
	struct bts_Cluster2D tmpCluster3E; 

	/** temporary cluster */
	struct bts_Cluster2D tmpCluster4E; 

	/** local scanner */
	struct bbf_LocalScanner scannerE;

	/** activity array */
	struct bbs_Int32Arr actArrE;

	/** index array */
	struct bbs_Int16Arr idxArrE;

	/** working image buffer */
	struct bbs_UInt8Arr workImageBufE;

	/* ---- public data ---------------------------------------------------- */

	/** patch width */
	uint32 patchWidthE;

	/** patch height*/
	uint32 patchHeightE;

	/** width of scan area */
	uint32 scanWidthE;

	/** height of scan area */
	uint32 scanHeightE;

	/** scanner scale exponent */
	uint32 scaleExpE;

	/** interpolated image warping */
	flag interpolatedWarpingE;

	/** image downscale threshold (part of image warping) (16.16) */
	uint32 warpScaleThresholdE;

	/** reference cluster */
	struct bts_IdCluster2D refClusterE; 

	/** cluster with scan positions */
	struct bts_Cluster2D scanClusterE; 

	/** feature data array (contains feature elements) */
	struct bbs_UInt16Arr ftrDataArrE;

	/** parameter for bit generation */
	struct bbf_BitParam bitParamE;

	/** outlier distance in pixels (16.16); ( >0: activates outlier analysis ) */
	uint32 outlierDistanceE;

	/** pca reference cluster */
	struct bts_IdCluster2D pcaClusterE; 

	/** pca average vector (10.6) */
	struct bbs_Int16Arr pcaAvgE;

	/** pca projection matrix (8.8) */
	struct bbs_Int16Arr pcaMatE; 

	/** pcs subspace dimensions */
	uint32 pcaDimSubSpaceE;

	/** max width of working image */
	uint32 maxImageWidthE;

	/** max height of working image */
	uint32 maxImageHeightE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbf_LocalScanDetector  */
void bbf_LocalScanDetector_init( struct bbs_Context* cpA,
								 struct bbf_LocalScanDetector* ptrA );

/** resets bbf_LocalScanDetector  */
void bbf_LocalScanDetector_exit( struct bbs_Context* cpA,
								 struct bbf_LocalScanDetector* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbf_LocalScanDetector_copy( struct bbs_Context* cpA,
								 struct bbf_LocalScanDetector* ptrA, 
								 const struct bbf_LocalScanDetector* srcPtrA );

/** equal operator */
flag bbf_LocalScanDetector_equal( struct bbs_Context* cpA,
								  const struct bbf_LocalScanDetector* ptrA, 
								  const struct bbf_LocalScanDetector* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bbf_LocalScanDetector_memSize( struct bbs_Context* cpA,
									  const struct bbf_LocalScanDetector* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bbf_LocalScanDetector_memWrite( struct bbs_Context* cpA,
									   const struct bbf_LocalScanDetector* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bbf_LocalScanDetector_memRead( struct bbs_Context* cpA,
									  struct bbf_LocalScanDetector* ptrA, 
									  const uint16* memPtrA, 
									  struct bbs_MemTbl* mtpA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** processes image with cluster; produces output cluster and returns confidence (8.24) 
 *  offsPtrA specifies pixel position (0,0) in input image
 */
int32 bbf_LocalScanDetector_process( struct bbs_Context* cpA,
									 const struct bbf_LocalScanDetector* ptrA, 
                                     uint8* imagePtrA, 
									 uint32 imageWidthA,
									 uint32 imageHeightA,
									 const struct bts_Int16Vec2D*  offsPtrA,
									 const struct bts_IdCluster2D* inClusterPtrA,
									 struct bts_IdCluster2D* outClusterPtrA );

#endif /* bbf_LOCAL_SCAN_DETECTOR_EM_H */

