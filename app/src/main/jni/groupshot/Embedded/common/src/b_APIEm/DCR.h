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

#ifndef bpi_DCR_EM_H
#define bpi_DCR_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/MemTbl.h"
#include "b_ImageEm/UInt16ByteImage.h"
#include "b_ImageEm/UInt32Image.h"
#include "b_TensorEm/IdCluster2D.h"
#include "b_TensorEm/RBFMap2D.h"
#include "b_BitFeatureEm/Scanner.h"


/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/** maximum size of dcr cluster */
#define bpi_DCR_MAX_CLUSTER_SIZE 60

/** maximum size of dcr sdk cluster */
#define bpi_DCR_MAX_SDK_CLUSTER_SIZE 24

/* ---- object definition -------------------------------------------------- */

/** data carrier */
struct bpi_DCR
{
	/* ---- temporary data ------------------------------------------------- */

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** maximum allowed image width */
	uint32 maxImageWidthE;

	/** maximum allowed image height */
	uint32 maxImageHeightE;

	/** pointer to original image data */
	void* imageDataPtrE;

	/** width of original image */
	uint32 imageWidthE;

	/** height of original image */
	uint32 imageHeightE;

	/** offset refering to main and sdk clusters */
	struct bts_Int16Vec2D offsE;

	/** main cluster */
	struct bts_IdCluster2D mainClusterE;

	/** output cluster accessible by sdk users */
	struct bts_IdCluster2D sdkClusterE;

	/** confidence value ( 8.24 ) */
	int32 confidenceE;

	/** approval flag */
	flag approvedE;

	/** (image) id value */
	int32 idE;

	/** region of interest */
	struct bts_Int16Rect roiRectE;

	/** cue data */
	struct bbs_UInt16Arr cueDataE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes data carrier */
void bpi_DCR_init( struct bbs_Context* cpA,
				   struct bpi_DCR* ptrA );

/** destroys data carrier */
void bpi_DCR_exit( struct bbs_Context* cpA,
				   struct bpi_DCR* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** create a data carrier */
void bpi_DCR_create( struct bbs_Context* cpA,
					 struct bpi_DCR* ptrA, 
					 uint32 imageWidthA,
					 uint32 imageHeightA,
					 uint32 cueSizeA,
					 struct bbs_MemTbl* mtpA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** references external byte gray image through memory block referenced by bufferPtrA to be used as input image */
void bpi_DCR_assignGrayByteImage( struct bbs_Context* cpA,
								  struct bpi_DCR* ptrA,
								  const void* bufferPtrA,
								  uint32 widthA,
								  uint32 heightA );

/** assigns external byte gray image as input image and region of interest.
  *
  * bufferPtrA:  pointer to memory block of imput image
  * pRectA:		 rectangle describing region of interest
  */
void bpi_DCR_assignGrayByteImageROI( struct bbs_Context* cpA,
									 struct bpi_DCR* ptrA,
									 const void* bufferPtrA, 
									 uint32 widthA, 
									 uint32 heightA,
									 const struct bts_Int16Rect* pRectA );

/** returns confidence 8.24 fixed format */
int32 bpi_DCR_confidence( struct bbs_Context* cpA,
						  const struct bpi_DCR* ptrA );

#endif /* bpi_DCR_EM_H */
