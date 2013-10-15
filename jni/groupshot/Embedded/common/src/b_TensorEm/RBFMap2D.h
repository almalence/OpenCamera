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

#ifndef bts_RBFMAP2D_EM_H
#define bts_RBFMAP2D_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/Int16Arr.h"
#include "b_TensorEm/Int16Vec2D.h"
#include "b_TensorEm/Flt16Vec2D.h"
#include "b_TensorEm/Flt16Alt2D.h"
#include "b_TensorEm/Functions.h"
#include "b_TensorEm/Cluster2D.h"
#include "b_TensorEm/Int32Mat.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bts_IRBFMAP2D_VERSION 100

/* ---- object definition -------------------------------------------------- */

/**
 * radial basis function transformation (RBF).
 * T( x ) = A( x ) + R( x ) where,
 * T is the resulting overall tranformation
 * A is a possibly linear tranformation of type altTypeE
 * R is the rbf ( non-linear ) transformation of type typeE
 * See member declaration for more information on typeE and altTypeE.
 * See also
 * 'Image Warping Using few Anchor Points and Radial Functions', 
 * Nur Arad and Daniel Reisfeld, 1994
 */
struct bts_RBFMap2D 
{
	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

    /** type of radial basis function (enum bts_RBFType).
	 * one of:
	 * bts_RBF_IDENTITY   : no rbf deformation
	 * bts_RBF_LINEAr     : linear, i.e. ||r||
	 */
	int32 RBFTypeE;

	/** src cluster, part of the RBF trafo */
	struct bts_Cluster2D srcClusterE;

	/** cluster of rbf coefficients, x and y */
	struct bts_Cluster2D rbfCoeffClusterE;

    /** type of linear transformation (enum bts_AltType) */
	int32 altTypeE;

    /** affine linear transformation */
	struct bts_Flt16Alt2D altE;

	/** apply only affine lnear transformation */
	flag altOnlyE;

	/* ---- temporary data ------------------------------------------------- */

	/** matrix needed for computation of rbf coefficients */
	struct bts_Int32Mat matE;
	struct bts_Int32Mat tempMatE;

	/** arrays needed for computation of rbf coefficients */
	struct bbs_Int32Arr inVecE;
	struct bbs_Int32Arr outVecE;
	struct bbs_Int32Arr tempVecE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes RBFMap */
void bts_RBFMap2D_init( struct bbs_Context* cpA,
					    struct bts_RBFMap2D* ptrA );

/** destroys RBFMap */
void bts_RBFMap2D_exit( struct bbs_Context* cpA,
					    struct bts_RBFMap2D* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copies RBFMap */
void bts_RBFMap2D_copy( struct bbs_Context* cpA,
					    struct bts_RBFMap2D* ptrA, 
						const struct bts_RBFMap2D* srcPtrA );

/** compares RBFMap */
flag bts_RBFMap2D_equal( struct bbs_Context* cpA,
						 const struct bts_RBFMap2D* ptrA, 
						 const struct bts_RBFMap2D* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates RBFMap */
void bts_RBFMap2D_create( struct bbs_Context* cpA,
						  struct bts_RBFMap2D* ptrA, 
						  uint32 sizeA,
				          struct bbs_MemSeg* mspA );

/** computes rbf transform from 2 given clusters of same size and bbp */
void bts_RBFMap2D_compute( struct bbs_Context* cpA,
						   struct bts_RBFMap2D* ptrA,
						   const struct bts_Cluster2D* srcPtrA,
						   const struct bts_Cluster2D* dstPtrA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_RBFMap2D_memSize( struct bbs_Context* cpA,
							 const struct bts_RBFMap2D* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_RBFMap2D_memWrite( struct bbs_Context* cpA,
							  const struct bts_RBFMap2D* ptrA, 
							  uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_RBFMap2D_memRead( struct bbs_Context* cpA,
							 struct bts_RBFMap2D* ptrA, 
							 const uint16* memPtrA,
				             struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** vector map operation: apply rbf to a vector */
struct bts_Flt16Vec2D bts_RBFMap2D_mapVector( struct bbs_Context* cpA,
											  const struct bts_RBFMap2D* ptrA,
											  struct bts_Flt16Vec2D vecA );

/** cluster map operation: apply rbf to all vectors in cluster */
void bts_RBFMap2D_mapCluster( struct bbs_Context* cpA,
							  const struct bts_RBFMap2D* ptrA,
							  const struct bts_Cluster2D* srcPtrA,
							  struct bts_Cluster2D* dstPtrA,
							  int32 dstBbpA );

#endif /* bts_RBFMAP2D_EM_H */

