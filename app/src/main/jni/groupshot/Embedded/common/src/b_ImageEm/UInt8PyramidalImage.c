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
#include "b_ImageEm/UInt8PyramidalImage.h"

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

void bim_UInt8PyramidalImage_init( struct bbs_Context* cpA,
								   struct bim_UInt8PyramidalImage* ptrA )
{
	bbs_UInt8Arr_init( cpA, &ptrA->arrE );
	ptrA->widthE = 0;
	ptrA->heightE = 0;
	ptrA->depthE = 0;
	ptrA->typeE = bim_UINT8_PYRAMIDAL_IMG;
}

/* ------------------------------------------------------------------------- */

void bim_UInt8PyramidalImage_exit( struct bbs_Context* cpA,
								   struct bim_UInt8PyramidalImage* ptrA )
{
	bbs_UInt8Arr_exit( cpA, &ptrA->arrE );
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

void bim_UInt8PyramidalImage_copy( struct bbs_Context* cpA,
								   struct bim_UInt8PyramidalImage* ptrA, 
								   const struct bim_UInt8PyramidalImage* srcPtrA )
{
#ifdef DEBUG1
	if( ptrA->arrE.allocatedSizeE < srcPtrA->arrE.allocatedSizeE )
	{
		bbs_ERROR0( "void bim_UInt8PyramidalImage_copy( ... ):\n"
				   "Unsufficient allocated memory in destination image" );		
		return;
	}
#endif
	ptrA->widthE = srcPtrA->widthE;
	ptrA->heightE = srcPtrA->heightE;
	ptrA->depthE = srcPtrA->depthE;
	bbs_UInt8Arr_copy( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

flag bim_UInt8PyramidalImage_equal( struct bbs_Context* cpA,
								    const struct bim_UInt8PyramidalImage* ptrA, 
									const struct bim_UInt8PyramidalImage* srcPtrA )
{
	if( ptrA->widthE != srcPtrA->widthE ) return FALSE;
	if( ptrA->heightE != srcPtrA->heightE ) return FALSE;
	if( ptrA->depthE != srcPtrA->depthE ) return FALSE;
	return bbs_UInt8Arr_equal( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint8* bim_UInt8PyramidalImage_arrPtr( struct bbs_Context* cpA,
									   const struct bim_UInt8PyramidalImage* ptrA, 
									   uint32 levelA )
{
	uint32 iL;
	uint32 offsL = 0;
	uint32 baseSizeL = ptrA->widthE * ptrA->heightE;

#ifdef DEBUG2
	if( levelA >= ptrA->depthE )
	{
		bbs_ERROR2( "uint8* bim_UInt8PyramidalImage_arrPtr( struct bim_UInt8PyramidalImage* ptrA, uint32 levelA ):\n"
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

uint32 bim_UInt8PyramidalImage_heapSize( struct bbs_Context* cpA,
										 const struct bim_UInt8PyramidalImage* ptrA, 
										 uint32 widthA, 
										 uint32 heightA, 
										 uint32 depthA )
{
	uint32 baseSizeL = widthA * heightA;
	uint32 sizeL = 0;
	uint32 iL;
	for( iL = 0; iL < depthA; iL++ )
	{
		sizeL += ( baseSizeL >> ( iL * 2 ) );
	}
	return 	bbs_UInt8Arr_heapSize( cpA, &ptrA->arrE, sizeL );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bim_UInt8PyramidalImage_create( struct bbs_Context* cpA,
									 struct bim_UInt8PyramidalImage* ptrA, 
									 uint32 widthA, uint32 heightA, 
									 uint32 depthA,
								     struct bbs_MemSeg* mspA )
{
	uint32 baseSizeL = widthA * heightA;
	uint32 sizeL = 0;
	uint32 iL;
	if( bbs_Context_error( cpA ) ) return;
	for( iL = 0; iL < depthA; iL++ )
	{
		sizeL += ( baseSizeL >> ( iL * 2 ) );
	}

	if( ptrA->arrE.arrPtrE != 0 )
	{
		bim_UInt8PyramidalImage_size( cpA, ptrA, widthA, heightA, depthA );
		return;
	}

#ifdef DEBUG1
	{
		uint32 depthMaskL = ( 1 << ( depthA - 1 ) ) - 1;
		if( depthA == 0 )
		{
			bbs_ERROR0( "void bim_UInt8PyramidalImage_create( struct bim_UInt8PyramidalImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
					   "depthA must be > 0" );
			return;
		}
		if( ( ( widthA & depthMaskL ) > 0 ) || ( ( heightA & depthMaskL ) > 0 ) )
		{
			bbs_ERROR1( "void bim_UInt8PyramidalImage_create( struct bim_UInt8PyramidalImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
					   "widthA and heightA must be divisible by %i", depthMaskL + 1 );
			return;
		}
	}
#endif

	ptrA->widthE  = widthA;
	ptrA->heightE = heightA;
	ptrA->depthE  = depthA;

	bbs_UInt8Arr_create( cpA, &ptrA->arrE, sizeL, mspA );
}

/* ------------------------------------------------------------------------- */

void bim_UInt8PyramidalImage_size( struct bbs_Context* cpA,
								   struct bim_UInt8PyramidalImage* ptrA, 
								   uint32 widthA, 
								   uint32 heightA, 
								   uint32 depthA )
{
	uint32 baseSizeL = widthA * heightA;
	uint32 sizeL = 0;
	uint32 iL;

#ifdef DEBUG1
	uint32 depthMaskL = ( 1 << ( depthA - 1 ) ) - 1;
	if( depthA == 0 )
	{
		bbs_ERROR0( "void bim_UInt8PyramidalImage_size( struct bim_UInt8PyramidalImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
			       "depthA must be > 0" );
		return;
	}

	if( ( ( widthA & depthMaskL ) > 0 ) || ( ( heightA & depthMaskL ) > 0 ) )
	{
		bbs_ERROR1( "void bim_UInt8PyramidalImage_size( struct bim_UInt8PyramidalImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
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
		bbs_ERROR0( "void bim_UInt8PyramidalImage_size( struct bim_UInt8PyramidalImage* ptrA, uint32 widthA, uint32 heightA, uint32 depthA ):\n"
			       "Insufficient allocated memory." );
		return;
	}
#endif
	bbs_UInt8Arr_size( cpA, &ptrA->arrE, sizeL );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt8PyramidalImage_memSize( struct bbs_Context* cpA,
									    const struct bim_UInt8PyramidalImage* ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->widthE ) 
		  + bbs_SIZEOF16( ptrA->heightE )
		  + bbs_SIZEOF16( ptrA->depthE )
		  + bbs_UInt8Arr_memSize( cpA, &ptrA->arrE ); 
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt8PyramidalImage_memWrite( struct bbs_Context* cpA,
										 const struct bim_UInt8PyramidalImage* ptrA, 
										 uint16* memPtrA )
{
	uint32 memSizeL = bim_UInt8PyramidalImage_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bim_UINT8_PYRAMIDAL_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->heightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->depthE, memPtrA );
	bbs_UInt8Arr_memWrite( cpA, &ptrA->arrE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt8PyramidalImage_memRead( struct bbs_Context* cpA,
									    struct bim_UInt8PyramidalImage* ptrA, 
									    const uint16* memPtrA,
 									    struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL, widthL, heightL, depthL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bim_UINT8_PYRAMIDAL_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &widthL, memPtrA );
	memPtrA += bbs_memRead32( &heightL, memPtrA );
	memPtrA += bbs_memRead32( &depthL, memPtrA );

	ptrA->widthE  = widthL;
	ptrA->heightE = heightL;
	ptrA->depthE  = depthL;
	bbs_UInt8Arr_memRead( cpA, &ptrA->arrE, memPtrA, mspA );

	if( memSizeL != bim_UInt8PyramidalImage_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bim_UInt8PyramidalImage_memRead( const struct bim_UInt8PyramidalImage* ptrA, const void* memPtrA ):\n"
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

void bim_UInt8PyramidalImage_overlayUInt8( struct bbs_Context* cpA,
										   const struct bim_UInt8PyramidalImage* ptrA,  
										   struct bim_UInt8Image* uint8ImageA )
{
	uint8ImageA->widthE = ptrA->widthE;
	uint8ImageA->heightE = ptrA->heightE;
	uint8ImageA->arrE.sizeE = ptrA->widthE * ptrA->heightE;
	uint8ImageA->arrE.allocatedSizeE = ptrA->widthE * ptrA->heightE;
	uint8ImageA->arrE.arrPtrE = ptrA->arrE.arrPtrE;
	uint8ImageA->arrE.mspE = 0;
}

/* ------------------------------------------------------------------------- */

void bim_UInt8PyramidalImage_recompute( struct bbs_Context* cpA,
									    struct bim_UInt8PyramidalImage* dstPtrA )
{
	uint32 iL, jL, layerL, widthL, heightL;
	uint8 *srcL, *dstL;

	/* process remaining layers */
	widthL = dstPtrA->widthE;
	heightL = dstPtrA->heightE;
	srcL = dstPtrA->arrE.arrPtrE;
	dstL = srcL + widthL * heightL;
	for( layerL = 1; layerL < dstPtrA->depthE; layerL++ )
	{
		for( jL = ( heightL >> 1 ); jL > 0; jL-- )
		{
			for( iL = ( widthL >> 1 ); iL > 0; iL-- )
			{
				/* averaging with roundig */
				*dstL++ = ( ( *srcL + *( srcL + 1 ) + *( srcL + widthL ) + *( srcL + widthL + 1 ) ) + 2 ) >> 2;
				srcL += 2;
			}
			srcL += widthL;
		}
		widthL >>= 1;
		heightL >>= 1;
	}
} 

/* ------------------------------------------------------------------------- */

void bim_UInt8PyramidalImage_importUInt8( struct bbs_Context* cpA,
										  struct bim_UInt8PyramidalImage* dstPtrA, 
									      const struct bim_UInt8Image* srcPtrA,
										  uint32 depthA )
{

	bim_UInt8PyramidalImage_size( cpA, dstPtrA, srcPtrA->widthE, srcPtrA->heightE, depthA );

	if( srcPtrA->arrE.sizeE & 1 )
	{
		bbs_ERROR0( "void bim_UInt8PyramidalImage_importUInt8(....):\n"
			       "Size of source image must be even.\n" );
		return;

	}

	/* copy first layer */
	bbs_memcpy16( dstPtrA->arrE.arrPtrE, srcPtrA->arrE.arrPtrE, srcPtrA->arrE.sizeE >> 1 );

	bim_UInt8PyramidalImage_recompute( cpA, dstPtrA );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


