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

#include "b_BasicEm/Functions.h"
#include "b_ImageEm/UInt32Image.h"

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

void bim_UInt32Image_init( struct bbs_Context* cpA,
						   struct bim_UInt32Image* ptrA )
{
	bbs_UInt32Arr_init( cpA, &ptrA->arrE );
	ptrA->widthE = 0;
	ptrA->heightE = 0;
}

/* ------------------------------------------------------------------------- */

void bim_UInt32Image_exit( struct bbs_Context* cpA,
						   struct bim_UInt32Image* ptrA )
{
	bbs_UInt32Arr_exit( cpA, &ptrA->arrE );
	ptrA->widthE = 0;
	ptrA->heightE = 0;	
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bim_UInt32Image_copy( struct bbs_Context* cpA,
						   struct bim_UInt32Image* ptrA, 
						   const struct bim_UInt32Image* srcPtrA )
{
#ifdef DEBUG1
	if( ptrA->arrE.allocatedSizeE < srcPtrA->arrE.allocatedSizeE )
	{
		bbs_ERROR0( "void bim_UInt32Image_copy(...):\n"
				   "Unsufficient allocated memory in destination image." );
		return;
	}
#endif
	ptrA->widthE  = srcPtrA->widthE;
	ptrA->heightE = srcPtrA->heightE;
	bbs_UInt32Arr_copy( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

flag bim_UInt32Image_equal( struct bbs_Context* cpA,
						    const struct bim_UInt32Image* ptrA, 
							const struct bim_UInt32Image* srcPtrA )
{
	if( ptrA->widthE  != srcPtrA->widthE ) return FALSE;
	if( ptrA->heightE != srcPtrA->heightE ) return FALSE;
	return bbs_UInt32Arr_equal( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bim_UInt32Image_heapSize( struct bbs_Context* cpA,
								 const struct bim_UInt32Image* ptrA, 
								 uint32 widthA, 
								 uint32 heightA )
{
	return bbs_UInt32Arr_heapSize( cpA, &ptrA->arrE, widthA * heightA );
}

/* ------------------------------------------------------------------------- */

uint32 bim_UInt32Image_checkSum( struct bbs_Context* cpA,
								 const struct bim_UInt32Image* ptrA )
{
	uint32 sumL =0 ;
	uint32 iL;
	uint32 sizeL = ptrA->arrE.sizeE;
	const uint32* ptrL = ptrA->arrE.arrPtrE;
	for( iL =0; iL < sizeL; iL++ )
	{
		sumL += *ptrL++;
	}
	return sumL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bim_UInt32Image_create( struct bbs_Context* cpA,
							 struct bim_UInt32Image* ptrA, 
						    uint32 widthA, 
							uint32 heightA,
 					        struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	if( ptrA->arrE.arrPtrE != 0 )
	{
		bim_UInt32Image_size( cpA, ptrA, widthA, heightA );
	}
	else
	{
		bbs_UInt32Arr_create( cpA, &ptrA->arrE, widthA * heightA, mspA );
		ptrA->widthE  = widthA;
		ptrA->heightE = heightA;
	}
}

/* ------------------------------------------------------------------------- */

void bim_UInt32Image_assignExternalImage( struct bbs_Context* cpA,
										  struct bim_UInt32Image* ptrA, 
										  struct bim_UInt32Image* srcPtrA )
{
	struct bbs_MemSeg sharedSegL = bbs_MemSeg_createShared( cpA, srcPtrA->arrE.arrPtrE, srcPtrA->widthE * srcPtrA->heightE );

	if( ptrA->arrE.arrPtrE != 0 )
	{
		bbs_ERROR0( "void bim_UInt32Image_assignExternalImage( ... ): image was already created once" );
		return;
	}

	bim_UInt32Image_create( cpA, 
							ptrA, 
					        srcPtrA->widthE, 
						    srcPtrA->heightE,
						    &sharedSegL );
}

/* ------------------------------------------------------------------------- */
	
void bim_UInt32Image_size( struct bbs_Context* cpA,
						   struct bim_UInt32Image* ptrA, 
						   uint32 widthA, 
						   uint32 heightA )
{
	if( ptrA->arrE.allocatedSizeE < widthA * heightA )
	{
		bbs_ERROR0( "void bim_UInt32Image_size( struct bim_UInt32Image*, uint32 sizeA ):\n"
				   "Unsufficient allocated memory" );
		return;
	}
	ptrA->widthE  = widthA;
	ptrA->heightE = heightA;
	bbs_UInt32Arr_size( cpA, &ptrA->arrE, widthA * heightA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bim_UInt32Image_memSize( struct bbs_Context* cpA,
							    const struct bim_UInt32Image* ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->widthE ) 
		  + bbs_SIZEOF16( ptrA->heightE )
		  + bbs_UInt32Arr_memSize( cpA, &ptrA->arrE ); 
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt32Image_memWrite( struct bbs_Context* cpA,
								 const struct bim_UInt32Image* ptrA, 
								 uint16* memPtrA )
{
	uint32 memSizeL = bim_UInt32Image_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bim_UINT32_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->heightE, memPtrA );
	bbs_UInt32Arr_memWrite( cpA, &ptrA->arrE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt32Image_memRead( struct bbs_Context* cpA,
							    struct bim_UInt32Image* ptrA, 
							   const uint16* memPtrA,
 					           struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bim_UINT32_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->heightE, memPtrA );
	bbs_UInt32Arr_memRead( cpA, &ptrA->arrE, memPtrA, mspA );

	if( memSizeL != bim_UInt32Image_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bim_UInt32Image_memRead( const struct bim_UInt32Image* ptrA, const void* memPtrA ):\n"
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

void bim_UInt32Image_setAllPixels( struct bbs_Context* cpA,
								   struct bim_UInt32Image* ptrA, 
								   uint32 valueA, 
								   int32 bbpA )
{
	long iL;
	uint32* ptrL = ptrA->arrE.arrPtrE;
	for( iL = ptrA->widthE * ptrA->heightE; iL > 0; iL-- )
	{
		*ptrL++ = valueA;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


