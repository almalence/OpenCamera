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
#include "b_BasicEm/Functions.h"
#include "b_ImageEm/Functions.h"
#include "b_ImageEm/UInt16BytePyrImage.h"

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

void bim_UInt16BytePyrImage_init( struct bbs_Context* cpA,
								  struct bim_UInt16BytePyrImage* ptrA )
{
	bbs_UInt16Arr_init( cpA, &ptrA->arrE );
	ptrA->widthE = 0;
	ptrA->heightE = 0;
	ptrA->depthE = 0;
	ptrA->typeE = bim_UINT16_PYRAMIDAL_IMG;
}

/* ------------------------------------------------------------------------- */

void bim_UInt16BytePyrImage_exit( struct bbs_Context* cpA,
								  struct bim_UInt16BytePyrImage* ptrA )
{
	bbs_UInt16Arr_exit( cpA, &ptrA->arrE );
	ptrA->widthE = 0;
	ptrA->heightE = 0;	
	ptrA->depthE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bim_UInt16BytePyrImage_copy( struct bbs_Context* cpA,
								  struct bim_UInt16BytePyrImage* ptrA, 
								  const struct bim_UInt16BytePyrImage* srcPtrA )
{
#ifdef DEBUG1
	if( ptrA->arrE.allocatedSizeE < srcPtrA->arrE.allocatedSizeE )
	{
		bbs_ERROR0( "void bim_UInt16BytePyrImage_copy( ... ):\n"
				   "Unsufficient allocated memory in destination image" );		
		return;
	}
#endif
	ptrA->widthE = srcPtrA->widthE;
	ptrA->heightE = srcPtrA->heightE;
	ptrA->depthE = srcPtrA->depthE;
	bbs_UInt16Arr_copy( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

flag bim_UInt16BytePyrImage_equal( struct bbs_Context* cpA,
								   const struct bim_UInt16BytePyrImage* ptrA, 
								   const struct bim_UInt16BytePyrImage* srcPtrA )
{
	if( ptrA->widthE != srcPtrA->widthE ) return FALSE;
	if( ptrA->heightE != srcPtrA->heightE ) return FALSE;
	if( ptrA->depthE != srcPtrA->depthE ) return FALSE;
	return bbs_UInt16Arr_equal( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint16* bim_UInt16BytePyrImage_arrPtr( struct bbs_Context* cpA,
									   const struct bim_UInt16BytePyrImage* ptrA, 
										 uint32 levelA )
{
	uint32 iL;
	uint32 offsL = 0;
	uint32 baseSizeL = ( ptrA->widthE * ptrA->heightE ) >> 1;

#ifdef DEBUG2
	if( levelA >= ptrA->depthE )
	{
		bbs_ERROR2( "uint16* bim_UInt16BytePyrImage_arrPtr( struct bim_UInt16BytePyrImage*, uint32 levelA ):\n"
			       "levelA = %i out of range [0,%i]", levelA, ptrA->depthE - 1 );
		return NULL;
	}
#endif

	for( iL = 0; iL < levelA; iL++ )
	{
		offsL += ( baseSizeL >> ( iL * 2 ) );
	}
	return ptrA->arrE.arrPtrE + offsL;
}

/* ------------------------------------------------------------------------- */

uint32 bim_UInt16BytePyrImage_heapSize( struct bbs_Context* cpA,
									    const struct bim_UInt16BytePyrImage* ptrA, 
										  uint32 widthA, uint32 heightA, 
										  uint32 depthA )
{
	uint32 baseSizeL = ( widthA * heightA ) >> 1;
	uint32 sizeL = 0;
	uint32 iL;
	for( iL = 0; iL < depthA; iL++ )
	{
		sizeL += ( baseSizeL >> ( iL * 2 ) );
	}
	return 	bbs_UInt16Arr_heapSize( cpA, &ptrA->arrE, sizeL );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bim_UInt16BytePyrImage_create( struct bbs_Context* cpA,
								    struct bim_UInt16BytePyrImage* ptrA, 
									 uint32 widthA, uint32 heightA, 
									 uint32 depthA,
								     struct bbs_MemSeg* mspA )
{
	uint32 baseSizeL = ( widthA * heightA ) >> 1;
	uint32 sizeL = 0;
	uint32 iL;
	
	if( bbs_Context_error( cpA ) ) return;
	if( ptrA->arrE.arrPtrE != 0 )
	{
		bim_UInt16BytePyrImage_size( cpA, ptrA, widthA, heightA, depthA );
		return;
	}

#ifdef DEBUG1
	{
		uint32 depthMaskL = ( ( int32 )1 << ( depthA - 1 ) ) - 1;
		if( depthA == 0 )
		{
			bbs_ERROR0( "void bim_UInt16BytePyrImage_create( struct bim_UInt16BytePyrImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
					   "depthA must be > 0" );
			return;
		}
		if( ( ( widthA & depthMaskL ) > 0 ) || ( ( heightA & depthMaskL ) > 0 ) )
		{
			bbs_ERROR1( "void bim_UInt16BytePyrImage_create( struct bim_UInt16BytePyrImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
					   "widthA and heightA must be divisible by %i", depthMaskL + 1 );
			return;
		}
	}
#endif

	ptrA->widthE  = widthA;
	ptrA->heightE = heightA;
	ptrA->depthE  = depthA;

	for( iL = 0; iL < depthA; iL++ )
	{
		sizeL += ( baseSizeL >> ( iL * 2 ) );
	}
	bbs_UInt16Arr_create( cpA, &ptrA->arrE, sizeL, mspA );
}

/* ------------------------------------------------------------------------- */

void bim_UInt16BytePyrImage_size( struct bbs_Context* cpA,
								  struct bim_UInt16BytePyrImage* ptrA, 
								  uint32 widthA, 
								  uint32 heightA, 
								  uint32 depthA )
{
	uint32 baseSizeL = ( widthA * heightA ) >> 1;
	uint32 sizeL = 0;
	uint32 iL;

#ifdef DEBUG1
	uint32 depthMaskL = ( 1 << ( depthA - 1 ) ) - 1;
	if( depthA == 0 )
	{
		bbs_ERROR0( "void bim_UInt16BytePyrImage_size( struct bim_UInt16BytePyrImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
			       "depthA must be > 0" );
		return;
	}

	if( ( ( widthA & depthMaskL ) > 0 ) || ( ( heightA & depthMaskL ) > 0 ) )
	{
		bbs_ERROR1( "void bim_UInt16BytePyrImage_size( struct bim_UInt16BytePyrImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
			       "widthA and heightA must be divisible by %i", depthMaskL + 1 );
		return;
	}
#endif

	ptrA->widthE  = widthA;
	ptrA->heightE = heightA;
	ptrA->depthE  = depthA;

	for( iL = 0; iL < depthA; iL++ )
	{
		sizeL += ( baseSizeL >> ( iL * 2 ) );
	}
#ifdef DEBUG1
	if( sizeL > ptrA->arrE.allocatedSizeE )
	{
		bbs_ERROR0( "void bim_UInt16BytePyrImage_size( struct bim_UInt16BytePyrImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
			       "Insufficient allocated memory." );
		return;
	}
#endif
	bbs_UInt16Arr_size( cpA, &ptrA->arrE, sizeL );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt16BytePyrImage_memSize( struct bbs_Context* cpA,
									   const struct bim_UInt16BytePyrImage* ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->widthE ) 
		  + bbs_SIZEOF16( ptrA->heightE )
		  + bbs_SIZEOF16( ptrA->depthE )
		  + bbs_UInt16Arr_memSize( cpA, &ptrA->arrE ); 
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt16BytePyrImage_memWrite( struct bbs_Context* cpA,
									    const struct bim_UInt16BytePyrImage* ptrA, 
										uint16* memPtrA )
{
	uint32 memSizeL = bim_UInt16BytePyrImage_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bim_UINT16_PYRAMIDAL_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->heightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->depthE, memPtrA );
	bbs_UInt16Arr_memWrite( cpA, &ptrA->arrE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt16BytePyrImage_memRead( struct bbs_Context* cpA,
									   struct bim_UInt16BytePyrImage* ptrA, 
									    const uint16* memPtrA,
 									    struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL, widthL, heightL, depthL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bim_UINT16_PYRAMIDAL_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &widthL, memPtrA );
	memPtrA += bbs_memRead32( &heightL, memPtrA );
	memPtrA += bbs_memRead32( &depthL, memPtrA );

	ptrA->widthE  = widthL;
	ptrA->heightE = heightL;
	ptrA->depthE  = depthL;
	bbs_UInt16Arr_memRead( cpA, &ptrA->arrE, memPtrA, mspA );

	if( memSizeL != bim_UInt16BytePyrImage_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bim_UInt16BytePyrImage_memRead( const struct bim_UInt16BytePyrImage* ptrA, const void* memPtrA ):\n"
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

void bim_UInt16BytePyrImage_overlayUInt16( struct bbs_Context* cpA,
										   const struct bim_UInt16BytePyrImage* ptrA,  
											 struct bim_UInt16ByteImage* uint16ImageA )
{
	uint16ImageA->widthE = ptrA->widthE;
	uint16ImageA->heightE = ptrA->heightE;
	uint16ImageA->arrE.sizeE = ptrA->widthE * ptrA->heightE;
	uint16ImageA->arrE.allocatedSizeE = ptrA->widthE * ptrA->heightE;
	uint16ImageA->arrE.arrPtrE = ptrA->arrE.arrPtrE;
	uint16ImageA->arrE.mspE = 0;
}

/* ------------------------------------------------------------------------- */

/** process remaining layers */
void bim_UInt16BytePyrImage_recompute( struct bbs_Context* cpA,
									   struct bim_UInt16BytePyrImage* dstPtrA )
{
	count_t iL, jL, layerL;
	uint16 tmpL;

	uint32 widthL = dstPtrA->widthE;
	uint32 halfWidthL = widthL >> 1;
	uint32 heightL = dstPtrA->heightE;

	uint16* srcL = dstPtrA->arrE.arrPtrE;
	uint16* dstL = srcL + ( heightL * halfWidthL );
	for( layerL = 1; layerL < dstPtrA->depthE; layerL++ )
	{
		for( jL = ( heightL >> 1 ); jL > 0; jL-- )
		{
			for( iL = ( halfWidthL >> 1 ); iL > 0; iL-- )
			{
				/* averaging with rounding */
					tmpL = ( ( *srcL & 0x0FF ) + ( *srcL >> 8 ) + ( *( srcL + halfWidthL ) & 0x0FF ) +
							 ( *( srcL + halfWidthL ) >> 8 ) + 2 ) >> 2;
				#ifdef HW_BIG_ENDIAN
					*dstL = tmpL << 8;
				#else
					*dstL = tmpL;
				#endif
				srcL++;

					tmpL = ( ( *srcL & 0x0FF ) + ( *srcL >> 8 ) + ( *( srcL + halfWidthL ) & 0x0FF ) +
							 ( *( srcL + halfWidthL ) >> 8 ) + 2 ) >> 2;
				#ifdef HW_BIG_ENDIAN
					*dstL |= tmpL;
				#else
					*dstL |= tmpL << 8;
				#endif
				srcL++;
				dstL++;
			}
			srcL += halfWidthL;
		}
		halfWidthL >>= 1;
		heightL >>= 1;
	}
} 


/* ------------------------------------------------------------------------- */


void bim_UInt16BytePyrImage_importUInt16( struct bbs_Context* cpA,
										  struct bim_UInt16BytePyrImage* dstPtrA, 
											const struct bim_UInt16ByteImage* srcPtrA,
											uint32 depthA )
{

	bim_UInt16BytePyrImage_size( cpA, dstPtrA, srcPtrA->widthE, srcPtrA->heightE, depthA );

	/* copy first layer */
	bbs_memcpy16( dstPtrA->arrE.arrPtrE, srcPtrA->arrE.arrPtrE, srcPtrA->arrE.sizeE );

	bim_UInt16BytePyrImage_recompute( cpA, dstPtrA );
}


/* ------------------------------------------------------------------------- */

/* ========================================================================= */


