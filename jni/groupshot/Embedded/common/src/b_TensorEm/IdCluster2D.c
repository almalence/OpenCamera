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

#include "b_BasicEm/Math.h"
#include "b_TensorEm/IdCluster2D.h"

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

void bts_IdCluster2D_init( struct bbs_Context* cpA,
						   struct bts_IdCluster2D* ptrA )
{
	bts_Cluster2D_init( cpA, &ptrA->clusterE );
	bbs_Int16Arr_init( cpA, &ptrA->idArrE );
}

/* ------------------------------------------------------------------------- */

void bts_IdCluster2D_exit( struct bbs_Context* cpA,
						   struct bts_IdCluster2D* ptrA )
{
	bts_Cluster2D_exit( cpA, &ptrA->clusterE );
	bbs_Int16Arr_exit( cpA, &ptrA->idArrE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_IdCluster2D_copy( struct bbs_Context* cpA,
						   struct bts_IdCluster2D* ptrA, 
						   const struct bts_IdCluster2D* srcPtrA )
{
	bts_Cluster2D_copy( cpA, &ptrA->clusterE, &srcPtrA->clusterE );
	bbs_Int16Arr_copy( cpA, &ptrA->idArrE, &srcPtrA->idArrE );
}

/* ------------------------------------------------------------------------- */

flag bts_IdCluster2D_equal( struct bbs_Context* cpA,
						    const struct bts_IdCluster2D* ptrA, 
							const struct bts_IdCluster2D* srcPtrA )
{
	if( !bts_Cluster2D_equal( cpA, &ptrA->clusterE, &srcPtrA->clusterE ) ) return FALSE;
	if( !bbs_Int16Arr_equal( cpA, &ptrA->idArrE, &srcPtrA->idArrE ) ) return FALSE;
	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_IdCluster2D_center( struct bbs_Context* cpA,
											  const struct bts_IdCluster2D* ptrA )
{
	return bts_Cluster2D_center( cpA, &ptrA->clusterE );
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Rect bts_IdCluster2D_boundingBox( struct bbs_Context* cpA,
												  const struct bts_IdCluster2D* ptrA )
{
	return bts_Cluster2D_boundingBox( cpA, &ptrA->clusterE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bts_IdCluster2D_create( struct bbs_Context* cpA,
							 struct bts_IdCluster2D* ptrA, 
							 uint32 sizeA,
						     struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	bts_Cluster2D_create( cpA, &ptrA->clusterE, sizeA, mspA );
	bbs_Int16Arr_create( cpA, &ptrA->idArrE, sizeA, mspA );
}

/* ------------------------------------------------------------------------- */
	
void bts_IdCluster2D_size( struct bbs_Context* cpA,
						   struct bts_IdCluster2D* ptrA, 
						   uint32 sizeA )
{
	bts_Cluster2D_size( cpA, &ptrA->clusterE, sizeA );
	bbs_Int16Arr_size( cpA, &ptrA->idArrE, sizeA );
}

/* ------------------------------------------------------------------------- */
	
void bts_IdCluster2D_transform( struct bbs_Context* cpA,
							    struct bts_IdCluster2D* ptrA, 
								struct bts_Flt16Alt2D altA )
{
	bts_Cluster2D_transform( cpA, &ptrA->clusterE, altA );
}

/* ------------------------------------------------------------------------- */
	
void bts_IdCluster2D_copyTransform( struct bbs_Context* cpA,
								    struct bts_IdCluster2D* ptrA, 
									const struct bts_IdCluster2D* srcPtrA, 
									struct bts_Flt16Alt2D altA, 
									uint32 dstBbpA )
{
	bts_Cluster2D_copyTransform( cpA, &ptrA->clusterE, &srcPtrA->clusterE, altA, dstBbpA );
	bbs_Int16Arr_copy( cpA, &ptrA->idArrE, &srcPtrA->idArrE );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_IdCluster2D_memSize( struct bbs_Context* cpA,
							    const struct bts_IdCluster2D *ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  +	bts_Cluster2D_memSize( cpA, &ptrA->clusterE )
		  + bbs_Int16Arr_memSize( cpA, &ptrA->idArrE );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_IdCluster2D_memWrite( struct bbs_Context* cpA,
								 const struct bts_IdCluster2D* ptrA, 
								 uint16* memPtrA )
{
	uint32 memSizeL = bts_IdCluster2D_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_ID_CLUSTER2D_VERSION, memPtrA );
	memPtrA += bts_Cluster2D_memWrite( cpA, &ptrA->clusterE, memPtrA );
	memPtrA += bbs_Int16Arr_memWrite( cpA, &ptrA->idArrE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_IdCluster2D_memRead( struct bbs_Context* cpA,
							    struct bts_IdCluster2D* ptrA, 
							    const uint16* memPtrA,
						        struct bbs_MemSeg* mspA )
{
	uint32 memSizeL;
	uint32 versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_ID_CLUSTER2D_VERSION, memPtrA );
	memPtrA += bts_Cluster2D_memRead( cpA, &ptrA->clusterE, memPtrA, mspA ); 
	memPtrA += bbs_Int16Arr_memRead( cpA, &ptrA->idArrE, memPtrA, mspA );
	if( memSizeL != bts_IdCluster2D_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_IdCluster2D_memRead( const struct bts_IdCluster2D* ptrA, const void* memPtrA ):\n"
                   "size mismatch" ); 
		return 0;
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

void bts_IdCluster2D_convertToEqivalentClusters( struct bbs_Context* cpA,
												 const struct bts_IdCluster2D* inCluster1PtrA,
												 const struct bts_IdCluster2D* inCluster2PtrA,
												 struct bts_Cluster2D* outCluster1PtrA,
												 struct bts_Cluster2D* outCluster2PtrA )
{
	uint32 iL, jL;
	uint32 countL = 0;

	uint32 size1L = inCluster1PtrA->clusterE.sizeE;
	uint32 size2L = inCluster2PtrA->clusterE.sizeE;

	const int16* idArr1L = inCluster1PtrA->idArrE.arrPtrE;
	const int16* idArr2L = inCluster2PtrA->idArrE.arrPtrE;

	const struct bts_Int16Vec2D* srcVecArr1E = inCluster1PtrA->clusterE.vecArrE;
	const struct bts_Int16Vec2D* srcVecArr2E = inCluster2PtrA->clusterE.vecArrE;

	struct bts_Int16Vec2D* dstVecArr1E = outCluster1PtrA->vecArrE;
	struct bts_Int16Vec2D* dstVecArr2E = outCluster2PtrA->vecArrE;

	uint32 maxOutSizeL = bbs_min( outCluster1PtrA->allocatedSizeE, outCluster2PtrA->allocatedSizeE );
	bts_Cluster2D_size( cpA, outCluster1PtrA, maxOutSizeL );
	bts_Cluster2D_size( cpA, outCluster2PtrA, maxOutSizeL );

	for( iL = 0; iL < size1L; iL++ )
	{
		int32 idL = idArr1L[ iL ];
		if( idL >= 0 )
		{
			for( jL = 0; jL < size2L; jL++ )
			{
				if( idL == idArr2L[ jL ] ) break;
			}

			if( jL < size2L )
			{
				if( countL == maxOutSizeL )
				{
					bbs_ERROR0( "void bts_IdCluster2D_convertToEqivalentClusters( .... ):\n"
						       "Destination clusters are insufficiently allocated" );
					return;
				}

				dstVecArr1E[ countL ] = srcVecArr1E[ iL ];
				dstVecArr2E[ countL ] = srcVecArr2E[ jL ];
				countL++;
			}
		}
	}

	bts_Cluster2D_size( cpA, outCluster1PtrA, countL );
	bts_Cluster2D_size( cpA, outCluster2PtrA, countL );

	outCluster1PtrA->bbpE = inCluster1PtrA->clusterE.bbpE;
	outCluster2PtrA->bbpE = inCluster2PtrA->clusterE.bbpE;
}

/* ------------------------------------------------------------------------- */

struct bts_Flt16Alt2D bts_IdCluster2D_alt( struct bbs_Context* cpA,
										   const struct bts_IdCluster2D* srcPtrA,
										   struct bts_IdCluster2D* dstPtrA,
										   enum bts_AltType altTypeA,
										   struct bts_Cluster2D* tmpPtr1A,  /* temporary cluster 1 */
										   struct bts_Cluster2D* tmpPtr2A ) /* temporary cluster 2 */
{
	bts_IdCluster2D_convertToEqivalentClusters( cpA, srcPtrA, dstPtrA, tmpPtr1A, tmpPtr2A );
	return bts_Cluster2D_alt( cpA, tmpPtr1A, tmpPtr2A, altTypeA );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


