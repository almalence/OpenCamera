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

#include "b_TensorEm/Cluster2D.h"
#include "b_TensorEm/RBFMap2D.h"
#include "b_BasicEm/Math.h"
#include "b_BasicEm/Memory.h"
#include "b_BasicEm/Functions.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/** Computes relative scale factor from the 2 mean square node distances to the
 *	cluster centers for 2 clusters.
 */
void bts_Cluster2D_computeScale( uint32 enumA,		/* mean square radius, dst cluster */
								 int32 bbp_enumA,	/* bbp of enumA */
								 uint32 denomA,		/* mean square radius, src cluster */
								 int32 bbp_denomA,	/* bbp of denomA */
								 uint32* scaleA,	/* resulting scale factor */
								 int32* bbp_scaleA )/* bbp of scale factor */
{
	uint32 shiftL, quotientL;
	int32 posL, bbp_denomL;

	/* how far can we shift enumA to the left */
	shiftL = 31 - bbs_intLog2( enumA );

	/* how far do we have to shift denomA to the right */
	posL = bbs_intLog2( denomA ) + 1;
	bbp_denomL = bbp_denomA;

	if( posL - bbp_denomL > 12 )
	{
		/* if denomA has more than 12 bit before the point, discard bits behind the point */
		denomA >>= bbp_denomL;
		bbp_denomL = 0;
	}
	else
	{
		/* otherwise reduce denomA to 12 bit */
		bbs_uint32ReduceToNBits( &denomA, &bbp_denomL, 12 );
	}

	/* make result bbp even for call of sqrt */
	if( ( bbp_enumA + shiftL - bbp_denomL ) & 1 ) shiftL--;

	quotientL = ( enumA << shiftL ) / denomA;

	*scaleA = bbs_fastSqrt32( quotientL );
	*bbp_scaleA = ( bbp_enumA + shiftL - bbp_denomL ) >> 1;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Cluster2D_init( struct bbs_Context* cpA,
						 struct bts_Cluster2D* ptrA )
{
	ptrA->mspE = NULL;
	ptrA->vecArrE = NULL;
	ptrA->allocatedSizeE = 0;
	ptrA->sizeE = 0;
	ptrA->bbpE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_Cluster2D_exit( struct bbs_Context* cpA,
						 struct bts_Cluster2D* ptrA )
{
	bbs_MemSeg_free( cpA, ptrA->mspE, ptrA->vecArrE );
	ptrA->vecArrE = NULL;
	ptrA->mspE = NULL;
	ptrA->allocatedSizeE = 0;
	ptrA->sizeE = 0;
	ptrA->bbpE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Cluster2D_copy( struct bbs_Context* cpA,
						 struct bts_Cluster2D* ptrA, 
						 const struct bts_Cluster2D* srcPtrA )
{
#ifdef DEBUG2
	if( ptrA->allocatedSizeE < srcPtrA->sizeE )
	{
		bbs_ERROR0( "void bts_Cluster2D_copy( struct bts_Cluster2D* ptrA, const struct bts_Cluster2D* srcPtrA ): allocated size too low in destination cluster" );
		return;
	}
#endif

	bbs_memcpy32( ptrA->vecArrE, srcPtrA->vecArrE, bbs_SIZEOF32( struct bts_Int16Vec2D ) * srcPtrA->sizeE );

	ptrA->bbpE = srcPtrA->bbpE;
	ptrA->sizeE = srcPtrA->sizeE;
}

/* ------------------------------------------------------------------------- */

flag bts_Cluster2D_equal( struct bbs_Context* cpA,
						  const struct bts_Cluster2D* ptrA, 
						  const struct bts_Cluster2D* srcPtrA )
{
	uint32 iL;
	const struct bts_Int16Vec2D* src1L = ptrA->vecArrE;
	const struct bts_Int16Vec2D* src2L = srcPtrA->vecArrE;

	if( ptrA->sizeE != srcPtrA->sizeE ) return FALSE;
	if( ptrA->bbpE != srcPtrA->bbpE ) return FALSE;

	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		if( ( src1L->xE != src2L->xE ) || ( src1L->yE != src2L->yE ) ) return FALSE;
		src1L++;
		src2L++;
	}

	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

struct bts_Flt16Vec2D bts_Cluster2D_center( struct bbs_Context* cpA,
										    const struct bts_Cluster2D* ptrA )
{
	struct bts_Int16Vec2D* vecPtrL = ptrA->vecArrE;
	uint32 iL;
	int32 xL = 0;
	int32 yL = 0;

	if( ptrA->sizeE == 0 ) return bts_Flt16Vec2D_create16( 0, 0, 0 );

	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		xL += vecPtrL->xE;
		yL += vecPtrL->yE;
		vecPtrL++;
	}

	xL = ( ( ( xL << 1 ) / ( int32 )ptrA->sizeE ) + 1 ) >> 1;
	yL = ( ( ( yL << 1 ) / ( int32 )ptrA->sizeE ) + 1 ) >> 1;

	return bts_Flt16Vec2D_create16( ( int16 )xL, ( int16 )yL, ( int16 )ptrA->bbpE );
}

/* ------------------------------------------------------------------------- */

uint32 bts_Cluster2D_checkSum( struct bbs_Context* cpA,
							   const struct bts_Cluster2D* ptrA )
{
	struct bts_Int16Vec2D* vecPtrL = ptrA->vecArrE;
	uint32 iL;
	int32 sumL = ptrA->bbpE;

	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		sumL += vecPtrL->xE;
		sumL += vecPtrL->yE;
		vecPtrL++;
	}

	return (uint32)sumL;
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Rect bts_Cluster2D_boundingBox( struct bbs_Context* cpA,
											    const struct bts_Cluster2D* ptrA )
{
	struct bts_Int16Vec2D* vecPtrL = ptrA->vecArrE;
	uint32 iL;
	int32 xMinL = 65536; 
	int32 yMinL = 65536; 
	int32 xMaxL = -65536;
	int32 yMaxL = -65536;

	if( ptrA->sizeE == 0 ) return bts_Int16Rect_create( 0, 0, 0, 0 );

	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		xMinL = bbs_min( xMinL, vecPtrL->xE );
		yMinL = bbs_min( yMinL, vecPtrL->yE );
		xMaxL = bbs_max( xMaxL, vecPtrL->xE );
		yMaxL = bbs_max( yMaxL, vecPtrL->yE );
		vecPtrL++;
	}

	return bts_Int16Rect_create( ( int16 )xMinL, ( int16 )yMinL, ( int16 )xMaxL, ( int16 )yMaxL );
}


/* ------------------------------------------------------------------------- */

int32 bts_Cluster2D_int32X( struct bbs_Context* cpA,
						    const struct bts_Cluster2D* ptrA, 
							uint32 indexA, int32 bbpA )
{
#ifdef DEBUG2
	if( indexA >= ptrA->sizeE )
	{
		bbs_ERROR2( "int32 bts_Cluster2D_int32X( .... )\n"
			       "indexA = %i is out of range [0,%i]",
				   indexA,
				   ptrA->sizeE - 1 );
		return 0;
	}
#endif

	{
		int32 shiftL = bbpA - ptrA->bbpE;
		int32 xL = ptrA->vecArrE[ indexA ].xE;
		if( shiftL >= 0 )
		{
			xL <<= shiftL;
		}
		else
		{
			xL = ( ( xL >> ( -shiftL - 1 ) ) + 1 ) >> 1;
		}

		return xL;
	}
}

/* ------------------------------------------------------------------------- */

int32 bts_Cluster2D_int32Y( struct bbs_Context* cpA,
						    const struct bts_Cluster2D* ptrA, 
							uint32 indexA, 
							int32 bbpA )
{
#ifdef DEBUG2
	if( indexA >= ptrA->sizeE )
	{
		bbs_ERROR2( "int32 bts_Cluster2D_int32Y( .... )\n"
			       "indexA = %i is out of range [0,%i]",
				   indexA,
				   ptrA->sizeE - 1 );
		return 0;
	}
#endif
	{
		int32 shiftL = bbpA - ptrA->bbpE;
		int32 yL = ptrA->vecArrE[ indexA ].yE;
		if( shiftL >= 0 )
		{
			yL <<= shiftL;
		}
		else
		{
			yL = ( ( yL >> ( -shiftL - 1 ) ) + 1 ) >> 1;
		}

		return yL;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bts_Cluster2D_create( struct bbs_Context* cpA,
						   struct bts_Cluster2D* ptrA, 
						   uint32 sizeA,
						   struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	if( ptrA->mspE == NULL )
	{
		ptrA->sizeE = 0;
		ptrA->allocatedSizeE = 0;
		ptrA->vecArrE = NULL;
	}

	if( ptrA->sizeE == sizeA ) return;

	if( ptrA->vecArrE != 0 )
	{
		bbs_ERROR0( "void bts_Cluster2D_create( const struct bts_Cluster2D*, uint32 ):\n"
				   "object has already been created and cannot be resized." ); 
		return;
	}

	ptrA->vecArrE = bbs_MemSeg_alloc( cpA, mspA, sizeA * bbs_SIZEOF16( struct bts_Int16Vec2D ) );
	if( bbs_Context_error( cpA ) ) return;
	ptrA->sizeE = sizeA;
	ptrA->allocatedSizeE = sizeA;
	if( !mspA->sharedE ) ptrA->mspE = mspA;
}

/* ------------------------------------------------------------------------- */
	
void bts_Cluster2D_size( struct bbs_Context* cpA,
						 struct bts_Cluster2D* ptrA, 
						 uint32 sizeA )
{
	if( ptrA->allocatedSizeE < sizeA )
	{
		bbs_ERROR2( "void bts_Cluster2D_size( struct bts_Cluster2D* ptrA, uint32 sizeA ):\n"
				   "Allocated size (%i) of cluster is smaller than requested size (%i).",
				   ptrA->allocatedSizeE,
				   sizeA ); 
		return;
	}
	ptrA->sizeE = sizeA;
}

/* ------------------------------------------------------------------------- */
	
void bts_Cluster2D_transform( struct bbs_Context* cpA,
							  struct bts_Cluster2D* ptrA, 
							  struct bts_Flt16Alt2D altA )
{
	uint32 iL;
	for( iL = 0; iL < ptrA->sizeE; iL++ )
	{
		struct bts_Flt16Vec2D vL = bts_Flt16Vec2D_createVec16( ptrA->vecArrE[ iL ], ptrA->bbpE );
		ptrA->vecArrE[ iL ] = bts_Flt16Vec2D_int16Vec2D( bts_Flt16Alt2D_mapFlt( &altA, &vL ), ptrA->bbpE );
	}
}

/* ------------------------------------------------------------------------- */
	
void bts_Cluster2D_transformBbp( struct bbs_Context* cpA,
							     struct bts_Cluster2D* ptrA, 
							     struct bts_Flt16Alt2D altA,
								 uint32 dstBbpA )
{
	uint32 iL;
	for( iL = 0; iL < ptrA->sizeE; iL++ )
	{
		struct bts_Flt16Vec2D vL = bts_Flt16Vec2D_createVec16( ptrA->vecArrE[ iL ], ptrA->bbpE );
		ptrA->vecArrE[ iL ] = bts_Flt16Vec2D_int16Vec2D( bts_Flt16Alt2D_mapFlt( &altA, &vL ), dstBbpA );
	}
	ptrA->bbpE = dstBbpA;
}

/* ------------------------------------------------------------------------- */

void bts_Cluster2D_rbfTransform( struct bbs_Context* cpA,
								 struct bts_Cluster2D* ptrA, 
								 const struct bts_RBFMap2D* rbfMapPtrA )
{
	bts_RBFMap2D_mapCluster( cpA, rbfMapPtrA, ptrA, ptrA, ptrA->bbpE );
}

/* ------------------------------------------------------------------------- */
	
void bts_Cluster2D_copyTransform( struct bbs_Context* cpA,
								  struct bts_Cluster2D* ptrA, 
								  const struct bts_Cluster2D* srcPtrA, 
								  struct bts_Flt16Alt2D altA, 
								  uint32 dstBbpA )
{
	uint32 iL;

	/* prepare destination cluster */
	if( ptrA->allocatedSizeE < srcPtrA->sizeE )
	{
		bbs_ERROR0( "void bts_Cluster2D_copyTransform( struct bts_Cluster2D* ptrA, const struct bts_Cluster2D* srcPtrA, struct bts_Flt16Alt2D altA, uint32 dstBbpA ): allocated size too low in destination cluster" );
		return;
	}

	ptrA->sizeE = srcPtrA->sizeE;
	ptrA->bbpE = dstBbpA;

	/* transform */
	for( iL = 0; iL < ptrA->sizeE; iL++ )
	{
		struct bts_Flt16Vec2D vL = bts_Flt16Vec2D_createVec16( srcPtrA->vecArrE[ iL ], srcPtrA->bbpE );
		ptrA->vecArrE[ iL ] = bts_Flt16Vec2D_int16Vec2D( bts_Flt16Alt2D_mapFlt( &altA, &vL ), ptrA->bbpE );
	}
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_Cluster2D_memSize( struct bbs_Context* cpA,
							  const struct bts_Cluster2D *ptrA )
{
	return  bbs_SIZEOF16( uint32 ) /* mem size */
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->sizeE ) 
		  + bbs_SIZEOF16( ptrA->bbpE ) 
		  + bbs_SIZEOF16( struct bts_Int16Vec2D ) * ptrA->sizeE;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Cluster2D_memWrite( struct bbs_Context* cpA,
							   const struct bts_Cluster2D* ptrA, 
							   uint16* memPtrA )
{
	uint32 memSizeL = bts_Cluster2D_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_CLUSTER2D_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->sizeE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->bbpE, memPtrA );
	memPtrA += bbs_memWrite16Arr( cpA, ptrA->vecArrE, ptrA->sizeE * 2, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Cluster2D_memRead( struct bbs_Context* cpA,
							  struct bts_Cluster2D* ptrA, 
							  const uint16* memPtrA,
							  struct bbs_MemSeg* mspA )
{
	uint32 memSizeL;
	uint32 sizeL;
	uint32 versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_CLUSTER2D_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &sizeL, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->bbpE, memPtrA );

	if( ptrA->allocatedSizeE < sizeL )
	{
		bts_Cluster2D_create( cpA, ptrA, sizeL, mspA );
	}
	else
	{
		bts_Cluster2D_size( cpA, ptrA, sizeL );
	}

	memPtrA += bbs_memRead16Arr( cpA, ptrA->vecArrE, ptrA->sizeE * 2, memPtrA );

	if( memSizeL != bts_Cluster2D_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_Cluster2D_memRead( const struct bts_Cluster2D* ptrA, const void* memPtrA ):\n"
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

struct bts_Flt16Alt2D bts_Cluster2D_alt( struct bbs_Context* cpA,
										 const struct bts_Cluster2D* srcPtrA,
										 const struct bts_Cluster2D* dstPtrA,
										 enum bts_AltType altTypeA )
{
	struct bts_Flt16Alt2D altL = bts_Flt16Alt2D_createIdentity();
	enum bts_AltType altTypeL = altTypeA;

	uint32 sizeL = srcPtrA->sizeE;
	int32 srcBbpL = srcPtrA->bbpE;
	int32 dstBbpL = dstPtrA->bbpE;

	struct bts_Flt16Vec2D cpL, cqL, cpMappedL, cpAdjustedL;

	if( dstPtrA->sizeE != srcPtrA->sizeE )
	{
		bbs_ERROR2( "struct bts_Flt16Alt2D bts_Cluster2D_alt( ... ):\n"
                   "the 2 input clusters differ in size: %d vs %d", srcPtrA->sizeE, dstPtrA->sizeE );
	}

	if( sizeL <= 2 )
	{
		if( altTypeL == bts_ALT_LINEAR )
		{
			altTypeL = bts_ALT_RIGID;
		}
	}

	if( sizeL <= 1 )
	{
		if( altTypeL == bts_ALT_RIGID ) 
		{
			altTypeL = bts_ALT_TRANS;
		}
		else if( altTypeL == bts_ALT_TRANS_SCALE ) 
		{
			altTypeL = bts_ALT_TRANS;
		}
	}

	if( sizeL == 0 || altTypeL == bts_ALT_IDENTITY )
	{
		/* return identity */
		return altL;
	}

	cpL = bts_Cluster2D_center( cpA, srcPtrA );
	cqL = bts_Cluster2D_center( cpA, dstPtrA );

	if( altTypeL == bts_ALT_TRANS )
	{
		/* return translation only */
		altL.vecE = bts_Flt16Vec2D_sub( cqL, cpL );
		return altL;
	}

	switch( altTypeL )
	{
		case bts_ALT_TRANS_SCALE:
		{
			uint32 spL = 0;
			uint32 sqL = 0;

			struct bts_Int16Vec2D* srcPtrL = srcPtrA->vecArrE;
			struct bts_Int16Vec2D* dstPtrL = dstPtrA->vecArrE;

			int32 iL = sizeL;
			while( iL-- )
			{
				int32 pxL = srcPtrL->xE - cpL.xE;
				int32 pyL = srcPtrL->yE - cpL.yE;
				int32 qxL = dstPtrL->xE - cqL.xE;
				int32 qyL = dstPtrL->yE - cqL.yE;
				srcPtrL++;
				dstPtrL++;

				/* overflow estimate: no problem with  100 nodes,  bbp = 6,  x = y = 500 */
				spL += ( pxL * pxL ) >> srcBbpL;
				spL += ( pyL * pyL ) >> srcBbpL;
				sqL += ( qxL * qxL ) >> dstBbpL;
				sqL += ( qyL * qyL ) >> dstBbpL;
			}

			spL /= sizeL;
			sqL /= sizeL;

			if( spL == 0 )
			{
				bbs_ERROR0( "struct bts_Flt16Alt2D bts_Cluster2D_alt( ... ):\n"
						   "All nodes of the src cluster are sitting in the center -> "
						   "unable to compute scale matrix between clusters" );
			}
			else
			{
				uint32 scaleL;
				int32 factor32L, bbp_scaleL;
				int16 factor16L;

				bts_Cluster2D_computeScale( sqL, dstBbpL, spL, srcBbpL, &scaleL, &bbp_scaleL );

				/* create scale matrix */
				factor32L = ( int32 )scaleL;
				altL.matE = bts_Flt16Mat2D_createScale( factor32L, bbp_scaleL );

				/* create translation vector */
				factor16L = scaleL;
				cpMappedL = bts_Flt16Vec2D_mul( cpL, factor16L, bbp_scaleL );
				altL.vecE = bts_Flt16Vec2D_sub( cqL, cpMappedL );

				return altL;
			}
		}
		break;

		case bts_ALT_RIGID:
		{
			/* smaller of the 2 bbp's */
			int32 minBbpL = bbs_min( srcBbpL, dstBbpL );

			uint32 spL = 0;
			uint32 sqL = 0;
			int32 pxqxL = 0;
			int32 pxqyL = 0;
			int32 pyqxL = 0;
			int32 pyqyL = 0;

			struct bts_Int16Vec2D* srcPtrL = srcPtrA->vecArrE;
			struct bts_Int16Vec2D* dstPtrL = dstPtrA->vecArrE;

			int32 iL = sizeL;
			while( iL-- )
			{
				int32 pxL = srcPtrL->xE - cpL.xE;
				int32 pyL = srcPtrL->yE - cpL.yE;
				int32 qxL = dstPtrL->xE - cqL.xE;
				int32 qyL = dstPtrL->yE - cqL.yE;
				srcPtrL++;
				dstPtrL++;

				/* overflow estimate: no problem with  100 nodes,  bbp = 6,  x = y = 500 */
				spL += ( pxL * pxL ) >> srcBbpL;
				spL += ( pyL * pyL ) >> srcBbpL;
				sqL += ( qxL * qxL ) >> dstBbpL;
				sqL += ( qyL * qyL ) >> dstBbpL;

				pxqxL += ( pxL * qxL ) >> minBbpL;
				pxqyL += ( pxL * qyL ) >> minBbpL;
				pyqxL += ( pyL * qxL ) >> minBbpL;
				pyqyL += ( pyL * qyL ) >> minBbpL;
			}

			spL /= sizeL;
			sqL /= sizeL;
			pxqxL /= ( int32 )sizeL;
			pxqyL /= ( int32 )sizeL;
			pyqxL /= ( int32 )sizeL;
			pyqyL /= ( int32 )sizeL;

			if( spL == 0 )
			{
				bbs_ERROR0( "struct bts_Flt16Alt2D bts_Cluster2D_alt( ... ):\n"
						   "All nodes of the src cluster are sitting in the center -> "
						   "unable to compute scale matrix between clusters" );
			}
			else
			{
				uint32 scaleL, shiftL, quotientL, enumL, denomL, bitsTaken0L, bitsTaken1L;
				int32 bbp_scaleL, cL, rL, c1L, r1L;
				int32 ppL, pmL, mpL, mmL, maxL;
				int32 quotientBbpL, bbp_crL, posL;


				/* find scale factor: */

				bts_Cluster2D_computeScale( sqL, dstBbpL, spL, srcBbpL, &scaleL, &bbp_scaleL );


				/* find rotation matrix: */

				/* sign not needed any more */
				enumL = bbs_abs( pxqyL - pyqxL );
				denomL = bbs_abs( pxqxL + pyqyL );

				if( denomL == 0 )
				{
					cL = 0;
					rL = 1;
					quotientBbpL = 0;
				}
				else
				{
					/* original formula:

					float aL = enumL / denomL;
					cL = sqrt( 1.0 / ( 1.0 + ebs_sqr( aL ) ) );
					rL = sqrt( 1 - ebs_sqr( cL ) );

					*/

					/* how far can we shift enumL to the left */
					shiftL = 31 - bbs_intLog2( enumL );

					/* result has bbp = shiftL */
					quotientL = ( enumL << shiftL ) / denomL;
					quotientBbpL = shiftL;

					posL = bbs_intLog2( quotientL );

					/* if enumL much larger than denomL, then we cannot square the quotient */
					if( posL > ( quotientBbpL + 14 ) )
					{
						cL = 0;
						rL = 1;
						quotientBbpL = 0;
					}
					else if( quotientBbpL > ( posL + 14 ) )
					{
						cL = 1;
						rL = 0;
						quotientBbpL = 0;
					}
					else
					{
						bbs_uint32ReduceToNBits( &quotientL, &quotientBbpL, 15 );

						/* to avoid an overflow in the next operation */
						if( quotientBbpL > 15 )
						{
							quotientL >>= ( quotientBbpL - 15 );
							quotientBbpL -= ( quotientBbpL - 15 );
						}

						/* result has again bbp = quotientBbpL */
						denomL = bbs_fastSqrt32( quotientL * quotientL + ( ( int32 )1 << ( quotientBbpL << 1 ) ) );

						quotientL = ( ( uint32 )1 << 31 ) / denomL;
						quotientBbpL = 31 - quotientBbpL;

						bbs_uint32ReduceToNBits( &quotientL, &quotientBbpL, 15 );

						/* to avoid an overflow in the next operation */
						if( quotientBbpL > 15 )
						{
							quotientL >>= ( quotientBbpL - 15 );
							quotientBbpL -= ( quotientBbpL - 15 );
						}

						cL = quotientL;
						rL = bbs_fastSqrt32( ( ( int32 )1 << ( quotientBbpL << 1 ) ) - quotientL * quotientL );
					}
				}

				/* save cL and rL with this accuracy for later */
				c1L = cL;
				r1L = rL;
				bbp_crL = quotientBbpL;

				/* prepare the next computations */
				bitsTaken0L = bts_maxAbsIntLog2Of4( pxqxL, pxqyL, pyqxL, pyqyL ) + 1;
				bitsTaken1L = bts_maxAbsIntLog2Of2( cL, rL ) + 1;

				if( ( bitsTaken0L + bitsTaken1L ) > 29 )
				{
					int32 shiftL = bitsTaken0L + bitsTaken1L - 29;
					cL >>= shiftL;
					rL >>= shiftL;
					quotientBbpL -= shiftL;
				}

				/* best combination: */
				ppL =   cL * pxqxL - rL * pyqxL + cL * pyqyL + rL * pxqyL;
				pmL =   cL * pxqxL + rL * pyqxL + cL * pyqyL - rL * pxqyL;
				mpL = - cL * pxqxL - rL * pyqxL - cL * pyqyL + rL * pxqyL;
				mmL = - cL * pxqxL + rL * pyqxL - cL * pyqyL - rL * pxqyL;

				maxL = bbs_max( bbs_max( ppL, pmL ), bbs_max( mpL, mmL ) );

				/* restore cL and rL, bbp = bbp_crL */
				cL = c1L;
				rL = r1L;

				/* rotation matrix */
				if( ppL == maxL )
				{
					altL.matE = bts_Flt16Mat2D_create32( cL, -rL, rL, cL, bbp_crL );
				}
				else if( pmL == maxL )
				{
					altL.matE = bts_Flt16Mat2D_create32( cL, rL, -rL, cL, bbp_crL );
				}
				else if( mpL == maxL )
				{
					altL.matE = bts_Flt16Mat2D_create32( -cL, -rL, rL, -cL, bbp_crL );
				}
				else
				{
					altL.matE = bts_Flt16Mat2D_create32( -cL, rL, -rL, -cL, bbp_crL );
				}


				/* find translation: */

				/* original formula:

				ets_Float2DVec transL = cqL - ( scaleL * ( rotL * cpL ) );
				altL.mat( rotL * scaleL );
				altL.vec( transL );

				*/

				bts_Flt16Mat2D_scale( &altL.matE, scaleL, bbp_scaleL );
				cpMappedL = bts_Flt16Mat2D_mapFlt( &altL.matE, &cpL );
				altL.vecE = bts_Flt16Vec2D_sub( cqL, cpMappedL );
			}

			return altL;
		}

		case bts_ALT_LINEAR:
		{
			/* smaller of the 2 bbp's */
			int32 minBbpL = bbs_min( srcBbpL, dstBbpL );

			int32 iL = 0;
			int32 pxpxL = 0;
			int32 pxpyL = 0;
			int32 pypyL = 0;
			int32 pxqxL = 0;
			int32 pxqyL = 0;
			int32 pyqxL = 0;
			int32 pyqyL = 0;

			struct bts_Int16Vec2D* srcPtrL = srcPtrA->vecArrE;
			struct bts_Int16Vec2D* dstPtrL = dstPtrA->vecArrE;

			/* get cp adjusted to dstBbpL */
			int32 shiftL = dstBbpL - srcBbpL;
			if( shiftL > 0 )
			{
				cpAdjustedL.xE = cpL.xE << shiftL;
				cpAdjustedL.yE = cpL.yE << shiftL;
				cpAdjustedL.bbpE = dstBbpL;
			}
			else
			{
				cpAdjustedL.xE = ( ( cpL.xE >> ( -shiftL - 1 ) ) + 1 ) >> 1;
				cpAdjustedL.yE = ( ( cpL.yE >> ( -shiftL - 1 ) ) + 1 ) >> 1;
				cpAdjustedL.bbpE = dstBbpL;
			}

			iL = sizeL;
			while( iL-- )
			{
				int32 pxL = srcPtrL->xE - cpL.xE;
				int32 pyL = srcPtrL->yE - cpL.yE;
				int32 qxL = dstPtrL->xE - cpAdjustedL.xE;	/* cp, not cq! */
				int32 qyL = dstPtrL->yE - cpAdjustedL.yE;
				srcPtrL++;
				dstPtrL++;

				/* overflow estimate: no problem with  100 nodes,  bbp = 6,  x = y = 500 */
				pxpxL += ( pxL * pxL ) >> srcBbpL;
				pxpyL += ( pxL * pyL ) >> srcBbpL;
				pypyL += ( pyL * pyL ) >> srcBbpL;

				pxqxL += ( pxL * qxL ) >> minBbpL;
				pxqyL += ( pxL * qyL ) >> minBbpL;
				pyqxL += ( pyL * qxL ) >> minBbpL;
				pyqyL += ( pyL * qyL ) >> minBbpL;
			}

			pxpxL /= ( int32 )sizeL;
			pxpyL /= ( int32 )sizeL;
			pypyL /= ( int32 )sizeL;
			pxqxL /= ( int32 )sizeL;
			pxqyL /= ( int32 )sizeL;
			pyqxL /= ( int32 )sizeL;
			pyqyL /= ( int32 )sizeL;

			{
				/* original code:

				float detPL = ( pxpxL * pypyL ) - ( pxpyL * pxpyL );

				if( ebs_neglectable( detPL ) )
				{
					matL.setIdentity();
				}
				else
				{
					matL.xx( ( pxqxL * pypyL - pyqxL * pxpyL ) / detPL );
					matL.xy( ( pyqxL * pxpxL - pxqxL * pxpyL ) / detPL ); 
					matL.yx( ( pxqyL * pypyL - pyqyL * pxpyL ) / detPL );
					matL.yy( ( pyqyL * pxpxL - pxqyL * pxpyL ) / detPL ); 
				}

				*/

				/* compute det first */
				uint32 bitsTaken0L = bts_maxAbsIntLog2Of4( pxpxL, pypyL, pxpyL, pxpyL ) + 1;
				int32 shL = 0;
				int32 detL = 0;
				int32 detBbpL = 0;

				if( bitsTaken0L > 15 )
				{
					shL = bitsTaken0L - 15;
				}

				detL = ( pxpxL >> shL ) * ( pypyL >> shL ) - ( pxpyL >> shL ) * ( pxpyL >> shL );

				/* this can be negative */
				detBbpL = ( srcBbpL - shL ) << 1;

				/* reduce to 15 bit */
				shL = ( int32 )bts_absIntLog2( detL );
				if( shL > 15 )
				{
					detL >>= ( shL - 15 );
					detBbpL -= ( shL - 15 );
				}

				if( detL != 0 )
				{
					int32 sh0L, sh1L, xxL, xyL, yxL, yyL, bbp_enumL;
					uint32 bitsTaken1L, highestBitL;

					sh0L = 0;
					if( bitsTaken0L > 15 )
					{
						sh0L = bitsTaken0L - 15;
					}

					bitsTaken1L = bts_maxAbsIntLog2Of4( pxqxL, pxqyL, pyqxL, pyqyL ) + 1;
					sh1L = 0;
					if( bitsTaken1L > 15 )
					{
						sh1L = bitsTaken1L - 15;
					}

					xxL = ( pxqxL >> sh1L ) * ( pypyL >> sh0L ) - ( pyqxL >> sh1L ) * ( pxpyL >> sh0L );
					xyL = ( pyqxL >> sh1L ) * ( pxpxL >> sh0L ) - ( pxqxL >> sh1L ) * ( pxpyL >> sh0L );
					yxL = ( pxqyL >> sh1L ) * ( pypyL >> sh0L ) - ( pyqyL >> sh1L ) * ( pxpyL >> sh0L );
					yyL = ( pyqyL >> sh1L ) * ( pxpxL >> sh0L ) - ( pxqyL >> sh1L ) * ( pxpyL >> sh0L );

					/* again, can be negative */
					bbp_enumL = ( srcBbpL - sh0L ) + ( bbs_max( srcBbpL, dstBbpL ) - sh1L );

					highestBitL = bts_maxAbsIntLog2Of4( xxL, xyL, yxL, yyL ) + 1;

					/* shift left */
					xxL <<= ( 31 - highestBitL );
					xyL <<= ( 31 - highestBitL );
					yxL <<= ( 31 - highestBitL );
					yyL <<= ( 31 - highestBitL );

					bbp_enumL += ( 31 - highestBitL );

					xxL /= detL;
					xyL /= detL;
					yxL /= detL;
					yyL /= detL;

					bbp_enumL -= detBbpL;

					altL.matE = bts_Flt16Mat2D_create32( xxL, xyL, yxL, yyL, bbp_enumL );
				}

				cpMappedL = bts_Flt16Mat2D_mapFlt( &altL.matE, &cpL );
				altL.vecE = bts_Flt16Vec2D_sub( cqL, cpMappedL );
			}

			return altL;
		}

		default:
		{
			bbs_ERROR1( "struct bts_Flt16Alt2D bts_Cluster2D_alt( ... ):\n"
				       "altType %d is not handled", altTypeL );
		}
	}

	return altL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

