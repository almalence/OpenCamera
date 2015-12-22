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

#include "b_TensorEm/Cluster3D.h"
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

void bts_Cluster3D_init( struct bbs_Context* cpA,
						 struct bts_Cluster3D* ptrA )
{
	ptrA->mspE = NULL;
	ptrA->vecArrE = NULL;
	ptrA->allocatedSizeE = 0;
	ptrA->sizeE = 0;
	ptrA->bbpE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_Cluster3D_exit( struct bbs_Context* cpA,
						 struct bts_Cluster3D* ptrA )
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

void bts_Cluster3D_copy( struct bbs_Context* cpA,
						 struct bts_Cluster3D* ptrA, 
						 const struct bts_Cluster3D* srcPtrA )
{
#ifdef DEBUG1
	if( ptrA->allocatedSizeE < srcPtrA->sizeE )
	{
		bbs_ERROR0( "void bts_Cluster3D_copy( struct bts_Cluster2D* ptrA, const struct bts_Cluster2D* srcPtrA ): allocated size too low in destination cluster" );
		return;
	}
#endif

	bbs_memcpy16( ptrA->vecArrE, srcPtrA->vecArrE, bbs_SIZEOF16( struct bts_Int16Vec3D ) * srcPtrA->sizeE );

	ptrA->bbpE = srcPtrA->bbpE;
	ptrA->sizeE = srcPtrA->sizeE;
}

/* ------------------------------------------------------------------------- */

flag bts_Cluster3D_equal( struct bbs_Context* cpA,
						  const struct bts_Cluster3D* ptrA, 
						  const struct bts_Cluster3D* srcPtrA )
{
	uint32 iL;
	const struct bts_Int16Vec3D* src1L = ptrA->vecArrE;
	const struct bts_Int16Vec3D* src2L = srcPtrA->vecArrE;

	if( ptrA->sizeE != srcPtrA->sizeE ) return FALSE;
	if( ptrA->bbpE != srcPtrA->bbpE ) return FALSE;

	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		if( ( src1L->xE != src2L->xE ) ||
			( src1L->yE != src2L->yE ) ||
			( src1L->zE != src2L->zE ) ) return FALSE;
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

struct bts_Flt16Vec3D bts_Cluster3D_center( struct bbs_Context* cpA,
										    const struct bts_Cluster3D* ptrA )
{
	struct bts_Int16Vec3D* vecPtrL = ptrA->vecArrE;
	uint32 iL;
	int32 xL = 0;
	int32 yL = 0;
	int32 zL = 0;

	if( ptrA->sizeE == 0 ) return bts_Flt16Vec3D_create16( 0, 0, 0, 0 );

	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		xL += vecPtrL->xE;
		yL += vecPtrL->yE;
		zL += vecPtrL->zE;
		vecPtrL++;
	}

	xL = ( ( ( xL << 1 ) / ( int32 )ptrA->sizeE ) + 1 ) >> 1;
	yL = ( ( ( yL << 1 ) / ( int32 )ptrA->sizeE ) + 1 ) >> 1;
	zL = ( ( ( zL << 1 ) / ( int32 )ptrA->sizeE ) + 1 ) >> 1;

	return bts_Flt16Vec3D_create16( ( int16 )xL, ( int16 )yL, ( int16 )zL, ( int16 )ptrA->bbpE );
}

/* ------------------------------------------------------------------------- */

struct bts_Int16Rect bts_Cluster3D_boundingBox( struct bbs_Context* cpA,
											    const struct bts_Cluster3D* ptrA )
{
	struct bts_Int16Vec3D* vecPtrL = ptrA->vecArrE;
	uint32 iL;
	int32 xMinL = 65536; /*( 1 << 16 )*/
	int32 yMinL = 65536; /*( 1 << 16 )*/
	int32 xMaxL = 0;
	int32 yMaxL = 0;

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

int32 bts_Cluster3D_int32X( struct bbs_Context* cpA,
						    const struct bts_Cluster3D* ptrA, 
							uint32 indexA, int32 bbpA )
{
	int32 shiftL = bbpA - ptrA->bbpE;
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
	if( shiftL >= 0 )
	{
		return ( int32 ) ptrA->vecArrE[ indexA ].xE << shiftL;
	}
	else
	{
		return ( ( ( int32 ) ptrA->vecArrE[ indexA ].xE >> ( -shiftL - 1 ) ) + 1 ) >> 1;
	}
}

/* ------------------------------------------------------------------------- */

int32 bts_Cluster3D_int32Y( struct bbs_Context* cpA,
						    const struct bts_Cluster3D* ptrA, 
							uint32 indexA, 
							int32 bbpA )
{
	int32 shiftL = bbpA - ptrA->bbpE;
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
	if( shiftL >= 0 )
	{
		return ( int32 ) ptrA->vecArrE[ indexA ].yE << shiftL;
	}
	else
	{
		return ( ( ( int32 ) ptrA->vecArrE[ indexA ].yE >> ( -shiftL - 1 ) ) + 1 ) >> 1;
	}
}

/* ------------------------------------------------------------------------- */

int32 bts_Cluster3D_int32Z( struct bbs_Context* cpA,
						    const struct bts_Cluster3D* ptrA, 
							uint32 indexA, 
							int32 bbpA )
{
	int32 shiftL = bbpA - ptrA->bbpE;
#ifdef DEBUG2
	if( indexA >= ptrA->sizeE )
	{
		bbs_ERROR2( "int32 bts_Cluster2D_int32Z( .... )\n"
			       "indexA = %i is out of range [0,%i]",
				   indexA,
				   ptrA->sizeE - 1 );
		return 0;
	}
#endif
	if( shiftL >= 0 )
	{
		return ( int32 ) ptrA->vecArrE[ indexA ].zE << shiftL;
	}
	else
	{
		return ( ( ( int32 ) ptrA->vecArrE[ indexA ].zE >> ( -shiftL - 1 ) ) + 1 ) >> 1;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bts_Cluster3D_create( struct bbs_Context* cpA,
						   struct bts_Cluster3D* ptrA, 
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
		bbs_ERROR0( "void bts_Cluster3D_create( const struct bts_Cluster3D*, uint32 ):\n"
				   "object has already been created and cannot be resized." ); 
		return;
	}

	ptrA->vecArrE = bbs_MemSeg_alloc( cpA, mspA, sizeA * bbs_SIZEOF16( struct bts_Int16Vec3D ) );
	if( bbs_Context_error( cpA ) ) return;
	ptrA->sizeE = sizeA;
	ptrA->allocatedSizeE = sizeA;
	if( !mspA->sharedE ) ptrA->mspE = mspA;
}

/* ------------------------------------------------------------------------- */

void bts_Cluster3D_size( struct bbs_Context* cpA,
						 struct bts_Cluster3D* ptrA, 
						 uint32 sizeA )
{
	if( ptrA->allocatedSizeE < sizeA )
	{
		bbs_ERROR2( "void bts_Cluster3D_size( struct bts_Cluster3D* ptrA, uint32 sizeA ):\n"
				   "Allocated size (%i) of cluster is smaller than requested size (%i).",
				   ptrA->allocatedSizeE,
				   sizeA ); 
		return;
	}
	ptrA->sizeE = sizeA;
}

/* ------------------------------------------------------------------------- */
	
void bts_Cluster3D_transform( struct bbs_Context* cpA,
							  struct bts_Cluster3D* ptrA, 
							  struct bts_Flt16Alt3D altA )
{
	struct bts_Int16Vec3D* vecPtrL = ptrA->vecArrE;
	uint32 iL;

	int32 x0L = altA.vecE.xE;
	int32 y0L = altA.vecE.yE;
	int32 z0L = altA.vecE.zE;

	int32 shiftL = altA.matE.bbpE + ptrA->bbpE - altA.vecE.bbpE;

	if( shiftL < 0 )
	{
		x0L = ( ( x0L >> ( -shiftL - 1 ) ) + 1 ) >> 1;
		y0L = ( ( y0L >> ( -shiftL - 1 ) ) + 1 ) >> 1;
		z0L = ( ( z0L >> ( -shiftL - 1 ) ) + 1 ) >> 1;
	}
	else
	{
		x0L <<= shiftL;
		y0L <<= shiftL;
		z0L <<= shiftL;
	}

	if( altA.matE.bbpE > 0 )
	{
		x0L += (int32)1 << ( altA.matE.bbpE - 1 );
		y0L += (int32)1 << ( altA.matE.bbpE - 1 );
		z0L += (int32)1 << ( altA.matE.bbpE - 1 );
	}

	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		int32 xL = vecPtrL->xE;
		int32 yL = vecPtrL->yE;
		int32 zL = vecPtrL->zE;
		vecPtrL->xE = ( x0L + xL * altA.matE.xxE + yL * altA.matE.xyE + zL * altA.matE.xzE ) >> altA.matE.bbpE;
		vecPtrL->yE = ( y0L + xL * altA.matE.yxE + yL * altA.matE.yyE + zL * altA.matE.yzE ) >> altA.matE.bbpE;
		vecPtrL->zE = ( z0L + xL * altA.matE.zxE + yL * altA.matE.zyE + zL * altA.matE.zzE ) >> altA.matE.bbpE;
		vecPtrL++;
	}
}

/* ------------------------------------------------------------------------- */
	
struct bts_Flt16Vec3D bts_Cluster3D_centerFree( struct bbs_Context* cpA,
											    struct bts_Cluster3D* ptrA )
{
	struct bts_Flt16Vec3D centerL = bts_Cluster3D_center( cpA, ptrA );
	struct bts_Int16Vec3D* vecPtrL = ptrA->vecArrE;
	uint32 iL;

	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		vecPtrL->xE -= centerL.xE;
		vecPtrL->yE -= centerL.yE;
		vecPtrL->zE -= centerL.zE;
		vecPtrL++;
	}

	return centerL;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_Cluster3D_memSize( struct bbs_Context* cpA,
							  const struct bts_Cluster3D *ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->sizeE ) 
		  + bbs_SIZEOF16( ptrA->bbpE ) 
		  + bbs_SIZEOF16( struct bts_Int16Vec3D ) * ptrA->sizeE;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Cluster3D_memWrite( struct bbs_Context* cpA,
							   const struct bts_Cluster3D* ptrA, 
							   uint16* memPtrA )
{
	uint32 memSizeL = bts_Cluster3D_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_CLUSTER3D_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->sizeE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->bbpE, memPtrA );
	memPtrA += bbs_memWrite16Arr( cpA, ptrA->vecArrE, 
								  ptrA->sizeE * bbs_SIZEOF16( struct bts_Int16Vec3D ), 
								  memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Cluster3D_memRead( struct bbs_Context* cpA,
							  struct bts_Cluster3D* ptrA, 
							  const uint16* memPtrA,
						      struct bbs_MemSeg* mspA )
{
	uint32 memSizeL;
	uint32 sizeL;
	uint32 versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_CLUSTER3D_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &sizeL, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->bbpE, memPtrA );

	if( ptrA->allocatedSizeE < sizeL )
	{
		bts_Cluster3D_create( cpA, ptrA, sizeL, mspA );
	}
	else
	{
		bts_Cluster3D_size( cpA, ptrA, sizeL );
	}


	bbs_memcpy16( ptrA->vecArrE, memPtrA, bbs_SIZEOF16( struct bts_Int16Vec3D ) * ptrA->sizeE );
	memPtrA += bbs_memRead16Arr( cpA, ptrA->vecArrE, 
								 ptrA->sizeE * bbs_SIZEOF16( struct bts_Int16Vec3D ), 
								 memPtrA );

	if( memSizeL != bts_Cluster3D_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_Cluster3D_memRead( const struct bts_Cluster3D* ptrA, const void* memPtrA ):\n"
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

/* ========================================================================= */

