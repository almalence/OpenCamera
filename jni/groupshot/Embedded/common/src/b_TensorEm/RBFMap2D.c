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

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_RBFMap2D_init( struct bbs_Context* cpA,
					    struct bts_RBFMap2D* ptrA )
{
	ptrA->RBFTypeE = bts_RBF_LINEAR;
	bts_Cluster2D_init( cpA, &ptrA->srcClusterE );
	bts_Cluster2D_init( cpA, &ptrA->rbfCoeffClusterE );
	ptrA->altTypeE = bts_ALT_LINEAR;
	bts_Flt16Alt2D_init( &ptrA->altE );

	ptrA->altOnlyE = FALSE;

	bts_Int32Mat_init( cpA, &ptrA->matE );
	bts_Int32Mat_init( cpA, &ptrA->tempMatE );
	bbs_Int32Arr_init( cpA, &ptrA->inVecE );
	bbs_Int32Arr_init( cpA, &ptrA->outVecE );
	bbs_Int32Arr_init( cpA, &ptrA->tempVecE );
}

/* ------------------------------------------------------------------------- */

void bts_RBFMap2D_exit( struct bbs_Context* cpA,
					    struct bts_RBFMap2D* ptrA )
{
	ptrA->RBFTypeE = bts_RBF_LINEAR;
	bts_Cluster2D_exit( cpA, &ptrA->srcClusterE );
	bts_Cluster2D_exit( cpA, &ptrA->rbfCoeffClusterE );
	ptrA->altTypeE = bts_ALT_LINEAR;
	bts_Flt16Alt2D_exit( &ptrA->altE );

	ptrA->altOnlyE = FALSE;

	bts_Int32Mat_exit( cpA, &ptrA->matE );
	bts_Int32Mat_exit( cpA, &ptrA->tempMatE );
	bbs_Int32Arr_exit( cpA, &ptrA->inVecE );
	bbs_Int32Arr_exit( cpA, &ptrA->outVecE );
	bbs_Int32Arr_exit( cpA, &ptrA->tempVecE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_RBFMap2D_copy( struct bbs_Context* cpA,
					    struct bts_RBFMap2D* ptrA, 
						const struct bts_RBFMap2D* srcPtrA )
{
	ptrA->RBFTypeE = srcPtrA->RBFTypeE;
	bts_Cluster2D_copy( cpA, &ptrA->srcClusterE, &srcPtrA->srcClusterE );
	bts_Cluster2D_copy( cpA, &ptrA->rbfCoeffClusterE, &srcPtrA->rbfCoeffClusterE );
	ptrA->altTypeE = srcPtrA->altTypeE;
	bts_Flt16Alt2D_copy( &ptrA->altE, &srcPtrA->altE );
}

/* ------------------------------------------------------------------------- */

flag bts_RBFMap2D_equal( struct bbs_Context* cpA,
						 const struct bts_RBFMap2D* ptrA, 
						 const struct bts_RBFMap2D* srcPtrA )
{
	if( ptrA->RBFTypeE != srcPtrA->RBFTypeE ) return FALSE;
	if( ! bts_Cluster2D_equal( cpA, &ptrA->srcClusterE, &srcPtrA->srcClusterE ) ) return FALSE;
	if( ! bts_Cluster2D_equal( cpA, &ptrA->rbfCoeffClusterE, &srcPtrA->rbfCoeffClusterE ) ) return FALSE;
	if( ptrA->altTypeE != srcPtrA->altTypeE ) return FALSE;
	if( ! bts_Flt16Alt2D_equal( &ptrA->altE, &srcPtrA->altE ) ) return FALSE;
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
	
void bts_RBFMap2D_create( struct bbs_Context* cpA,
						  struct bts_RBFMap2D* ptrA,
						  uint32 sizeA,
				          struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	bts_Cluster2D_create( cpA, &ptrA->srcClusterE, sizeA, mspA );
	bts_Cluster2D_create( cpA, &ptrA->rbfCoeffClusterE, sizeA, mspA );

	bts_Int32Mat_create( cpA, &ptrA->matE, sizeA, mspA );
	bts_Int32Mat_create( cpA, &ptrA->tempMatE, sizeA, mspA );
	bbs_Int32Arr_create( cpA, &ptrA->inVecE, sizeA, mspA );
	bbs_Int32Arr_create( cpA, &ptrA->outVecE, sizeA, mspA );
	bbs_Int32Arr_create( cpA, &ptrA->tempVecE, sizeA, mspA );
}

/* ------------------------------------------------------------------------- */

void bts_RBFMap2D_compute( struct bbs_Context* cpA,
						   struct bts_RBFMap2D* ptrA,
						   const struct bts_Cluster2D* srcPtrA,
						   const struct bts_Cluster2D* dstPtrA )
{
	const uint32 sizeL = srcPtrA->sizeE;
	int32 bbp_internalL = 15;
	int32 bbp_rbfCoeffL = 12;

	int32 internalShiftL = bbp_internalL - srcPtrA->bbpE;
	int32 rbfCoeffShiftL;

	uint32 iL, jL;

	if( dstPtrA->sizeE != srcPtrA->sizeE )
	{
		bbs_ERROR2( "void bts_RBFMap2D_compute( ... ): size mismatch, src cluster has size %d,"
			"but dst cluster has size %d\n", srcPtrA->sizeE, dstPtrA->sizeE );
		return;
	}

	ptrA->altOnlyE = FALSE;

	/* if bbp of src cluster should be larger than bbp_internal, use it instead */
	if( internalShiftL < 0 )
	{
		internalShiftL = 0;
		bbp_internalL = srcPtrA->bbpE;
	}

	/* also checks for sizeL > allocated size */
	bts_Cluster2D_size( cpA, &ptrA->rbfCoeffClusterE, sizeL );

	/* set rbf coefficients to 0 in case they don't get computed */
	for( iL =0; iL < sizeL; iL++ )
	{
		ptrA->rbfCoeffClusterE.vecArrE[ iL ].xE = 0;
		ptrA->rbfCoeffClusterE.vecArrE[ iL ].yE = 0;
	}

	/* 1. Compute rigid transformation: if cluster size == 0 returns identity */
	ptrA->altE = bts_Cluster2D_alt( cpA, srcPtrA, dstPtrA, ptrA->altTypeE );

	/* if cluster size is less than 3 affine trafo covers whole transformation */
	if( sizeL < 3 )
	{
		bts_Cluster2D_copy( cpA, &ptrA->srcClusterE, srcPtrA );
		ptrA->altOnlyE = TRUE;
		return;
	}

	/* 2. Compute RBF trafo */
	ptrA->matE.widthE = sizeL;
	ptrA->tempMatE.widthE = sizeL;
	
	/* Set up linear matrix to invert */
	switch( ptrA->RBFTypeE )
	{
		case bts_RBF_IDENTITY:
		{
			return;
		}

		case bts_RBF_LINEAR:
		{
			/* ||r|| */
			for( iL = 0; iL < sizeL; iL++ )
			{
				struct bts_Int16Vec2D vec0L = srcPtrA->vecArrE[ iL ];
				int32* ptrL = ptrA->matE.arrE.arrPtrE + iL * sizeL;

				/* set diagonal elements having null distance */
				*( ptrL + iL ) = 0;

				for( jL = 0; jL < iL; jL++ )	/* use symmetry */
				{
					int32 normL = 0;
					struct bts_Int16Vec2D vecL = srcPtrA->vecArrE[ jL ];
					vecL.xE -= vec0L.xE;
					vecL.yE -= vec0L.yE;
					normL = bts_Int16Vec2D_norm( &vecL );
					*ptrL++ = normL << internalShiftL;
				}
			}
		}
		break;

		/* Add a new RBF type here */

		default:
		{
			bbs_ERROR1( "void bts_RBFMap2D_compute( ... ): RBFType %d is not handled\n", ptrA->RBFTypeE );
			return;
		}
	}

	/* use symmetry: set symmetric elements in matrix */
	for( iL = 0; iL < sizeL; iL++ )
	{
		int32* basePtrL = ptrA->matE.arrE.arrPtrE;
		uint32 jL;
		for( jL = iL + 1; jL < sizeL; jL++ )
		{
			*( basePtrL + iL * sizeL + jL ) = *( basePtrL + jL * sizeL + iL );
		}
	}

	/* Precompute alt transformed cluster, srcClusterE will be restored at the end */
	bts_Cluster2D_copy( cpA, &ptrA->srcClusterE, srcPtrA );
	bts_Cluster2D_transformBbp( cpA, &ptrA->srcClusterE, ptrA->altE, dstPtrA->bbpE );

	bbs_Int32Arr_size( cpA, &ptrA->inVecE, sizeL );
	bbs_Int32Arr_size( cpA, &ptrA->outVecE, sizeL );
	bbs_Int32Arr_size( cpA, &ptrA->tempVecE, sizeL );

	{
		flag successL;

		/* compute right side vector of linear system to be solved, for x */
		int32* inPtrL = ptrA->inVecE.arrPtrE;
		struct bts_Int16Vec2D* dstVecL = dstPtrA->vecArrE;
		struct bts_Int16Vec2D* altVecL = ptrA->srcClusterE.vecArrE;

		int32 shiftL = srcPtrA->bbpE - ptrA->srcClusterE.bbpE + internalShiftL;
		if( shiftL >= 0 )
		{
			for( iL = 0; iL < sizeL; iL++ ) inPtrL[ iL ] = ( int32 )( dstVecL[ iL ].xE - altVecL[ iL ].xE ) << shiftL;
		}
		else
		{
			for( iL = 0; iL < sizeL; iL++ ) inPtrL[ iL ] = ( ( ( int32 )( dstVecL[ iL ].xE - altVecL[ iL ].xE ) >> ( ( -shiftL ) - 1 ) ) + 1 ) >> 1;
		}

		/* solve linear system in x */
		successL = bts_Int32Mat_solve(  cpA, 
			                            ptrA->matE.arrE.arrPtrE,
										sizeL,
										ptrA->inVecE.arrPtrE,
										ptrA->outVecE.arrPtrE,
										bbp_internalL,
										ptrA->tempMatE.arrE.arrPtrE,
										ptrA->tempVecE.arrPtrE );

		/* no error condition here! system must be failsafe */
		if( !successL ) ptrA->altOnlyE = TRUE;

		/* store rbf coefficients, x component */
		rbfCoeffShiftL = bbp_internalL - bbp_rbfCoeffL;
		for( iL = 0; iL < sizeL; iL++ )
		{
			int32 rbfCoeffL = ptrA->outVecE.arrPtrE[ iL ] >> rbfCoeffShiftL;
			if( rbfCoeffL < -32768 || rbfCoeffL > 32767 ) ptrA->altOnlyE = TRUE; /* check for overflow */
			ptrA->rbfCoeffClusterE.vecArrE[ iL ].xE = rbfCoeffL;
		}


		/* compute right side vector of linear system to be solved, for y */
		if( shiftL >= 0 )
		{
			for( iL = 0; iL < sizeL; iL++ ) inPtrL[ iL ] = ( int32 )( dstVecL[ iL ].yE - altVecL[ iL ].yE ) << shiftL;
		}
		else
		{
			for( iL = 0; iL < sizeL; iL++ ) inPtrL[ iL ] = ( ( ( int32 )( dstVecL[ iL ].yE - altVecL[ iL ].yE ) >> ( ( -shiftL ) - 1 ) ) + 1 ) >> 1;
		}

		/* solve linear system in y */
		successL = bts_Int32Mat_solve(  cpA, 
			                            ptrA->matE.arrE.arrPtrE,
										sizeL,
										ptrA->inVecE.arrPtrE,
										ptrA->outVecE.arrPtrE,
										bbp_internalL,
										ptrA->tempMatE.arrE.arrPtrE,
										ptrA->tempVecE.arrPtrE );
		if( !successL )
		{
			/* no error condition here! system must be failsafe */
			ptrA->altOnlyE = TRUE;
		}

		/* store rbf coefficients, y component */
		for( iL = 0; iL < sizeL; iL++ )
		{
			int32 rbfCoeffL = ptrA->outVecE.arrPtrE[ iL ] >> rbfCoeffShiftL;
			if( rbfCoeffL < -32768 || rbfCoeffL > 32767 ) ptrA->altOnlyE = TRUE; /* check for overflow */
			ptrA->rbfCoeffClusterE.vecArrE[ iL ].yE = rbfCoeffL;
		}

		/* set bbp of coeff cluster */
		ptrA->rbfCoeffClusterE.bbpE = bbp_rbfCoeffL;
	}

	/** after having used srcClusterE for temporary storage of the alt src cluster,
		restore the orig src cluster as needed for the RBF trafo */
	bts_Cluster2D_copy( cpA, &ptrA->srcClusterE, srcPtrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_RBFMap2D_memSize( struct bbs_Context* cpA,
							 const struct bts_RBFMap2D *ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->RBFTypeE )
		  + bts_Cluster2D_memSize( cpA, &ptrA->srcClusterE )
		  + bts_Cluster2D_memSize( cpA, &ptrA->rbfCoeffClusterE )
		  + bbs_SIZEOF16( ptrA->altTypeE ) 
		  + bts_Flt16Alt2D_memSize( cpA, &ptrA->altE );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_RBFMap2D_memWrite( struct bbs_Context* cpA,
							  const struct bts_RBFMap2D* ptrA, 
							  uint16* memPtrA )
{
	uint32 memSizeL = bts_RBFMap2D_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_IRBFMAP2D_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->RBFTypeE, memPtrA );
	memPtrA += bts_Cluster2D_memWrite( cpA, &ptrA->srcClusterE, memPtrA );
	memPtrA += bts_Cluster2D_memWrite( cpA, &ptrA->rbfCoeffClusterE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->altTypeE, memPtrA );
	memPtrA += bts_Flt16Alt2D_memWrite( cpA, &ptrA->altE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_RBFMap2D_memRead( struct bbs_Context* cpA,
							 struct bts_RBFMap2D* ptrA, 
							 const uint16* memPtrA,
				             struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_IRBFMAP2D_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->RBFTypeE, memPtrA );
	memPtrA += bts_Cluster2D_memRead( cpA, &ptrA->srcClusterE, memPtrA, mspA );
	memPtrA += bts_Cluster2D_memRead( cpA, &ptrA->rbfCoeffClusterE, memPtrA, mspA );
	memPtrA += bbs_memRead32( &ptrA->altTypeE, memPtrA );
	memPtrA += bts_Flt16Alt2D_memRead( cpA, &ptrA->altE, memPtrA );

	bts_Int32Mat_create( cpA, &ptrA->matE, ptrA->srcClusterE.sizeE, mspA );
	bts_Int32Mat_create( cpA, &ptrA->tempMatE, ptrA->srcClusterE.sizeE, mspA );

	if( memSizeL != bts_RBFMap2D_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_RBFMap2D_memRead( ... ): size mismatch\n" ); 
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
/**	R, A are rbf and A affine linear transformations 
 *	T( x ) = R( x ) + A( x )
 */
struct bts_Flt16Vec2D bts_RBFMap2D_mapVector( struct bbs_Context* cpA,
											  const struct bts_RBFMap2D* ptrA,
											  struct bts_Flt16Vec2D vecA )
{
	const uint32 sizeL = ptrA->srcClusterE.sizeE;
	const int32 bbp_internalL = ptrA->rbfCoeffClusterE.bbpE;
	uint32 iL;
	int32 shL;

	int32 outXL;
	int32 outYL;
	int32 outBbpL;

	/* 1. Compute rigid transformation, i.e. A( x ) */
	struct bts_Flt16Vec2D altVecL = bts_Flt16Alt2D_mapFlt( &ptrA->altE, &vecA );

	/* compute output on 32 bit here to prevent temporary overflows (j.s.) */
	outXL   = altVecL.xE;
	outYL   = altVecL.yE;
	outBbpL = altVecL.bbpE;

	/* if bbp was altered, change it back to bbp of vecA ( det A is always close to 1 here ) */
	shL = vecA.bbpE - outBbpL;
	if( shL > 0 )
	{
		outXL <<= shL;
		outYL <<= shL;
	}
	else if( shL < 0 )
	{
		outXL = ( ( outXL >> ( -shL - 1 ) ) + 1 ) >> 1;
		outYL = ( ( outYL >> ( -shL - 1 ) ) + 1 ) >> 1;
	}
	outBbpL = vecA.bbpE;

	/* stop here if rbf coefficients could not be computed  */
	if( ptrA->altOnlyE )
	{
		return bts_Flt16Vec2D_create32( outXL, outYL, outBbpL );
	}

	/* 2. Compute RBF transformation, i.e. R( x ) depending on type */
	switch( ptrA->RBFTypeE )
    {
		case bts_RBF_IDENTITY:
		break;

        case bts_RBF_LINEAR:
        {
			int32 xSumL = 0;
			int32 ySumL = 0;
			int32 internalShiftL = bbp_internalL - ptrA->srcClusterE.bbpE;

			/* first adapt vecA to bbp of srcCluster */
			int32 xL = vecA.xE;
			int32 yL = vecA.yE;
			int32 shiftL = ptrA->srcClusterE.bbpE - vecA.bbpE;
			if( shiftL > 0 )
			{
				xL <<= shiftL;
				yL <<= shiftL;
			}
			else if( shiftL < 0 )
			{
				xL = ( ( xL >> ( -shiftL - 1 ) ) + 1 ) >> 1;
				yL = ( ( yL >> ( -shiftL - 1 ) ) + 1 ) >> 1;
			}

			shiftL = ptrA->srcClusterE.bbpE;

            for( iL = 0; iL < sizeL; iL++ )
            {
				struct bts_Int16Vec2D vecL = ptrA->srcClusterE.vecArrE[ iL ];
				int32 normL = 0;
				vecL.xE -= xL;
				vecL.yE -= yL;
				normL = bts_Int16Vec2D_norm( &vecL );

/* printf( "iL = %d, norm = %d\n", iL, normL ); */

				xSumL += ( normL * ptrA->rbfCoeffClusterE.vecArrE[ iL ].xE ) >> shiftL;
				ySumL += ( normL * ptrA->rbfCoeffClusterE.vecArrE[ iL ].yE ) >> shiftL;

/* printf( "iL = %d, xSumL = %d, ySumL = %d\n", iL, xSumL, ySumL ); */

            }

			xSumL >>= internalShiftL;
			ySumL >>= internalShiftL;

			/* change bbp of result back to bbp of vecA */
		/*	shiftL = vecA.bbpE - ptrA->srcClusterE.bbpE - internalShiftL; */
			shiftL = vecA.bbpE - ptrA->srcClusterE.bbpE;
			if( shiftL > 0 )
			{
				xSumL <<= shiftL;
				ySumL <<= shiftL;
			}
			else if( shiftL < 0 )
			{
				xSumL = ( ( xSumL >> ( -shiftL - 1 ) ) + 1 ) >> 1;
				ySumL = ( ( ySumL >> ( -shiftL - 1 ) ) + 1 ) >> 1;
			}

			/* add rbf part to already computed alt part */
			outXL += xSumL;
			outYL += ySumL;
        }
        break;

		/* Add a new RBF type here */

		default:
		{
			bbs_ERROR1( "struct bts_Flt16Vec2D bts_RBFMap2D_mapVector( ... ): "
				"RBFType %d is not handled\n", ptrA->RBFTypeE );
			return bts_Flt16Vec2D_create32( outXL, outYL, outBbpL );
		}
	}

	return bts_Flt16Vec2D_create32( outXL, outYL, outBbpL );
}

/* ------------------------------------------------------------------------- */

void bts_RBFMap2D_mapCluster( struct bbs_Context* cpA,
							  const struct bts_RBFMap2D* ptrA,
							  const struct bts_Cluster2D* srcPtrA,
							  struct bts_Cluster2D* dstPtrA,
							  int32 dstBbpA )
{
	if( dstPtrA->sizeE != srcPtrA->sizeE )
	{
		/* resizing of clusters is allowed as long as allocated size is not exceeded */
		bts_Cluster2D_size( cpA, dstPtrA, srcPtrA->sizeE );
	}

	{
		uint32 iL;
		int16 bbpL = srcPtrA->bbpE;

		dstPtrA->bbpE = dstBbpA;

		for( iL = 0; iL < srcPtrA->sizeE; iL++ )
		{
			struct bts_Int16Vec2D vecL = srcPtrA->vecArrE[ iL ];
			struct bts_Flt16Vec2D srcVecL = bts_Flt16Vec2D_create16( vecL.xE, vecL.yE, bbpL );
			struct bts_Flt16Vec2D dstVecL = bts_RBFMap2D_mapVector( cpA, ptrA, srcVecL );
			dstPtrA->vecArrE[ iL ].xE = bbs_convertS32( dstVecL.xE, dstVecL.bbpE, dstBbpA );
			dstPtrA->vecArrE[ iL ].yE = bbs_convertS32( dstVecL.yE, dstVecL.bbpE, dstBbpA );
		}
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

