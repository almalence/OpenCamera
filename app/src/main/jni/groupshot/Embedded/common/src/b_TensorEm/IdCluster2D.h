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

#ifndef bts_ID_CLUSTER2D_EM_H
#define bts_ID_CLUSTER2D_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Int16Arr.h"
#include "b_TensorEm/Cluster2D.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bts_ID_CLUSTER2D_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** 2d vector array with node id information */
struct bts_IdCluster2D 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/* vector array */
	struct bts_Cluster2D clusterE;

	/** array of id numbers */
	struct bbs_Int16Arr idArrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes cluster */
void bts_IdCluster2D_init( struct bbs_Context* cpA,
						   struct bts_IdCluster2D* ptrA );

/** destroys cluster */
void bts_IdCluster2D_exit( struct bbs_Context* cpA,
						   struct bts_IdCluster2D* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copies cluster */
void bts_IdCluster2D_copy( struct bbs_Context* cpA,
						   struct bts_IdCluster2D* ptrA, 
						   const struct bts_IdCluster2D* srcPtrA );

/** compares cluster */
flag bts_IdCluster2D_equal( struct bbs_Context* cpA,
						    const struct bts_IdCluster2D* ptrA, 
							const struct bts_IdCluster2D* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** returns center of gravity */
struct bts_Flt16Vec2D bts_IdCluster2D_center( struct bbs_Context* cpA,
											  const struct bts_IdCluster2D* ptrA );

/** returns bounding box */
struct bts_Int16Rect bts_IdCluster2D_boundingBox( struct bbs_Context* cpA,
												  const struct bts_IdCluster2D* ptrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** allocates cluster */
void bts_IdCluster2D_create( struct bbs_Context* cpA,
							 struct bts_IdCluster2D* ptrA, 
							 uint32 sizeA,
						     struct bbs_MemSeg* mspA );

/** resize cluster (sizeA must be smaller or equal to allocated size)*/
void bts_IdCluster2D_size( struct bbs_Context* cpA,
						   struct bts_IdCluster2D* ptrA, 
						   uint32 sizeA );

/** transforms cluster according to alt (function does not change bbp of cluster) */
void bts_IdCluster2D_transform( struct bbs_Context* cpA,
							    struct bts_IdCluster2D* ptrA, 
								struct bts_Flt16Alt2D altA );

/** copies src cluster and simultaneously transforms vectors according to alt using dstBbpA as resulting cluster format */
void bts_IdCluster2D_copyTransform( struct bbs_Context* cpA,
								    struct bts_IdCluster2D* ptrA, 
									const struct bts_IdCluster2D* srcPtrA, 
									struct bts_Flt16Alt2D altA, 
									uint32 dstBbpA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_IdCluster2D_memSize( struct bbs_Context* cpA,
							    const struct bts_IdCluster2D* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_IdCluster2D_memWrite( struct bbs_Context* cpA,
								 const struct bts_IdCluster2D* ptrA, 
								 uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_IdCluster2D_memRead( struct bbs_Context* cpA,
							    struct bts_IdCluster2D* ptrA, 
							    const uint16* memPtrA,
						        struct bbs_MemSeg* mspA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/* This function extracts equivalent 2d sub clusters which positions 
 * correponts to those nodes that have a non-negative id occurring 
 * in both input clusters. 
 * Note: Nodes with negative ids are ignored
 *       Non-Negative ids must not occur twice in one cluster.
 */
void bts_IdCluster2D_convertToEqivalentClusters( struct bbs_Context* cpA,
												 const struct bts_IdCluster2D* inCluster1PtrA,
												 const struct bts_IdCluster2D* inCluster2PtrA,
												 struct bts_Cluster2D* outCluster1PtrA,
												 struct bts_Cluster2D* outCluster2PtrA );

/** Computes the best affine linear transformation from *srcPtrA to *dstPtrA using matching id values.
 *  Constrains of trafo are given by altTypeA
 *
 *  This function selects and matches nodes with corresponsing non-negative id values of source 
 *  an destination clusters. Nodes with negative id values are ignored. Id values >= 0 must be unique 
 *  per node.
 */
struct bts_Flt16Alt2D bts_IdCluster2D_alt( struct bbs_Context* cpA,
										   const struct bts_IdCluster2D* srcPtrA,
										   struct bts_IdCluster2D* dstPtrA,
										   enum bts_AltType altTypeA,
										   struct bts_Cluster2D* tmpPtr1A,   /* temporary cluster 1 */
										   struct bts_Cluster2D* tmpPtr2A ); /* temporary cluster 2 */
										   

#endif /* bts_ID_CLUSTER2D_EM_H */

