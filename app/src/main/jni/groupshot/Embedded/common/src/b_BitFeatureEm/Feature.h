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

#ifndef bbf_FEATURE_EM_H
#define bbf_FEATURE_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/MemTbl.h"
#include "b_BitFeatureEm/Functions.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

enum bbf_FeatureType
{
	bbf_FT_UNDEFINED = 0,
	bbf_FT_SEQUENCE,
	bbf_FT_I04_DNS_2X2_FTR,
	bbf_FT_I04_TLD_2X4_FTR,
	bbf_FT_I04_DNS_2X4_FTR,
	bbf_FT_L01_TLD_2X4_FTR,
	bbf_FT_L01_DNS_2X4_FTR,
	bbf_FT_L04_DNS_2X4_FTR,
	bbf_FT_L04_DNS_3X3_FTR,
	bbf_FT_L06_DNS_3X3_FTR,
	bbf_FT_L06_DNS_4X4_FTR,
	bbf_FT_L06_DNS_NX4X4_FTR,

	bbf_FT_L01_TLD_1X1_FTR,
	bbf_FT_L04_TLD_2X4_FTR,
	bbf_FT_L04_DNS_2X2_FTR
};

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** base object for features (occurs as first element in all feature objects) */
struct bbf_Feature 
{
	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** feature type */
	uint32 typeE;

	/** width of patch */
	uint32 patchWidthE;
	
	/** height of patch */
	uint32 patchHeightE;

	/* ---- virtual functions ---------------------------------------------- */

	/** computes feature's activity (4.28) on the given patch */
	int32 ( *vpActivityE )( const struct bbf_Feature* ptrA, const uint32* patchA ); 
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbf_Feature  */
void bbf_Feature_init( struct bbs_Context* cpA,
					   struct bbf_Feature* ptrA );

/** resets bbf_Feature  */
void bbf_Feature_exit( struct bbs_Context* cpA,
					   struct bbf_Feature* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbf_Feature_copy( struct bbs_Context* cpA,
					   struct bbf_Feature* ptrA, 
					   const struct bbf_Feature* srcPtrA );

/** equal operator */
flag bbf_Feature_equal( struct bbs_Context* cpA,
						const struct bbf_Feature* ptrA, 
						const struct bbf_Feature* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bbf_Feature_memSize( struct bbs_Context* cpA,
						    const struct bbf_Feature* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bbf_Feature_memWrite( struct bbs_Context* cpA,
							 const struct bbf_Feature* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bbf_Feature_memRead( struct bbs_Context* cpA,
							struct bbf_Feature* ptrA, const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** virtual init function  */
void bbf_featureInit( struct bbs_Context* cpA,
					  struct bbf_Feature* ptrA,
					  enum bbf_FeatureType typeA );

/** virtual exit function */
void bbf_featureExit( struct bbs_Context* cpA, 
					  struct bbf_Feature* ptrA );

/** virtual mem size function */
uint32 bbf_featureMemSize( struct bbs_Context* cpA, 
						   const struct bbf_Feature* ptrA );

/** virtual mem write function */
uint32 bbf_featureMemWrite( struct bbs_Context* cpA, 
						    const struct bbf_Feature* ptrA, uint16* memPtrA );

/** virtual mem read function */
uint32 bbf_featureMemRead( struct bbs_Context* cpA,
						   struct bbf_Feature* ptrA, 
						   const uint16* memPtrA,
						   struct bbs_MemTbl* mtpA );

/** virtual sizeof operator for 16bit units */
uint32 bbf_featureSizeOf16( struct bbs_Context* cpA, enum bbf_FeatureType typeA );

#endif /* bbf_FEATURE_EM_H */

