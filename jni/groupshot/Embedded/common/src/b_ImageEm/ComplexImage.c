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
#include "b_ImageEm/ComplexImage.h"
#include "b_ImageEm/APhImage.h"

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

void bim_ComplexImage_init( struct bbs_Context* cpA,
						    struct bim_ComplexImage* ptrA )
{
	bbs_ComplexArr_init( cpA, &ptrA->arrE );
	ptrA->widthE = 0;
	ptrA->heightE = 0;
}

/* ------------------------------------------------------------------------- */

void bim_ComplexImage_exit( struct bbs_Context* cpA,
						    struct bim_ComplexImage* ptrA )
{
	bbs_ComplexArr_exit( cpA, &ptrA->arrE );
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

void bim_ComplexImage_copy( struct bbs_Context* cpA,
						    struct bim_ComplexImage* ptrA, 
							const struct bim_ComplexImage* srcPtrA )
{
#ifdef DEBUG1
	if( ptrA->arrE.allocatedSizeE < srcPtrA->arrE.allocatedSizeE )
	{
		bbs_ERROR0( "void bim_ComplexImage_copy(...):\n"
				   "Unsufficient allocated memory in destination image." );
		return;
	}
#endif
	ptrA->widthE = srcPtrA->widthE;
	ptrA->heightE = srcPtrA->heightE;
	bbs_ComplexArr_copy( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

flag bim_ComplexImage_equal( struct bbs_Context* cpA,
							 const struct bim_ComplexImage* ptrA, 
							 const struct bim_ComplexImage* srcPtrA )
{
	if( ptrA->widthE != srcPtrA->widthE ) return FALSE;
	if( ptrA->heightE != srcPtrA->heightE ) return FALSE;
	return bbs_ComplexArr_equal( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bim_ComplexImage_checkSum( struct bbs_Context* cpA,
								  const struct bim_ComplexImage* ptrA )
{
	uint32 sumL =0 ;
	uint32 iL;
	uint32 sizeL = ptrA->arrE.sizeE;
	const struct bbs_Complex* ptrL = ptrA->arrE.arrPtrE;
	for( iL =0; iL < sizeL; iL++ )
	{
		sumL += ptrL->imagE + ptrL->realE;
		ptrL++;
	}
	return sumL;
}

/* ------------------------------------------------------------------------- */

uint32 bim_ComplexImage_heapSize( struct bbs_Context* cpA,
								  const struct bim_ComplexImage* ptrA, 
								  uint32 widthA, uint32 heightA )
{
	return bbs_ComplexArr_heapSize( cpA, &ptrA->arrE, widthA * heightA );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bim_ComplexImage_create( struct bbs_Context* cpA,
							  struct bim_ComplexImage* ptrA, 
						      uint32 widthA, 
							  uint32 heightA,
 					          struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	if( ptrA->arrE.arrPtrE != 0 )
	{
		bim_ComplexImage_size( cpA, ptrA, widthA, heightA );
	}
	else
	{
		bbs_ComplexArr_create( cpA, &ptrA->arrE, widthA * heightA, mspA );
		ptrA->widthE  = widthA;
		ptrA->heightE = heightA;
	}
}

/* ------------------------------------------------------------------------- */

void bim_ComplexImage_size( struct bbs_Context* cpA,
						    struct bim_ComplexImage* ptrA, 
							uint32 widthA, 
							uint32 heightA )
{
	if( ptrA->arrE.allocatedSizeE < widthA * heightA )
	{
		bbs_ERROR0( "void bim_ComplexImage_size( struct bim_ComplexImage*, uint32 sizeA ):\n"
				   "Unsufficient allocated memory" );
		return;
	}
	ptrA->widthE  = widthA;
	ptrA->heightE = heightA;
	bbs_ComplexArr_size( cpA, &ptrA->arrE, widthA * heightA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bim_ComplexImage_memSize( struct bbs_Context* cpA,
								 const struct bim_ComplexImage* ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->widthE ) 
		  + bbs_SIZEOF16( ptrA->heightE )
		  + bbs_ComplexArr_memSize( cpA, &ptrA->arrE ); 
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_ComplexImage_memWrite( struct bbs_Context* cpA,
								  const struct bim_ComplexImage* ptrA, 
								  uint16* memPtrA )
{
	uint32 memSizeL = bim_ComplexImage_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bim_COMPLEX_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->heightE, memPtrA );
	bbs_ComplexArr_memWrite( cpA, &ptrA->arrE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_ComplexImage_memRead( struct bbs_Context* cpA,
								 struct bim_ComplexImage* ptrA, 
								 const uint16* memPtrA,
 					             struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL, widthL, heightL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bim_COMPLEX_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &widthL, memPtrA );
	memPtrA += bbs_memRead32( &heightL, memPtrA );

	ptrA->widthE  = widthL;
	ptrA->heightE = heightL;
	bbs_ComplexArr_memRead( cpA, &ptrA->arrE, memPtrA, mspA );

	if( memSizeL != bim_ComplexImage_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bim_ComplexImage_memRead( const struct bim_ComplexImage* ptrA, const void* memPtrA ):\n"
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

void bim_ComplexImage_setAllPixels( struct bbs_Context* cpA,
								    struct bim_ComplexImage* ptrA, 
									struct bbs_Complex valueA )
{
	long iL;
	struct bbs_Complex* ptrL = ptrA->arrE.arrPtrE;
	for( iL = ptrA->widthE * ptrA->heightE; iL > 0; iL-- )
	{
		*ptrL++ = valueA;
	}
}

/* ------------------------------------------------------------------------- */

/**
			|				|				|				|
			|	(loop x1)	|	(loop x2)	|	(loop x3)	|
			o------------->-o------------>--o------------->-o
			|				|				|				|
			|				|				|				|
			|				|				|				|
			|				|				|				|
	( sectionL->x1E, sectionL->y1E )		|				|
---------o-	R-------------------------------|----------------
		 |	|				|				|				|
		 |	|				|				|				|
		 |	|				|				|				|
		 |	|				|				|				|
   (loop y1)|				|				|				|
		 |	|				|				|				|
		 V	|				|				|				|
		 |	|				|( 0, 0 )		|				|		X
---------o------------------I------------------------------------------------->
		 |	|				|				|				|
		 |	|				|				|				|
		 |	|				|				|				|
		 |	|				|				|				|
		 |	|				|				|				|
   (loop y2)|				|				|				|
		 |	|				|				|				|
		 |	|				|				|				|
		 |	|				|				|				|
		 V	|				|				|				|
		 |	|				|				|				|
---------o------------------|---------------I				|
		 |	|				|		( srcPtrA->widthE, srcPtrA->heightE )
		 |	|				|								|
		 |	|				|								|
		 |	|				|								|
		 |	|				|								|
		 |	|				|								|
   (loop y3)|				|								|
		 |	|				|								|
		 |	|				|								|
		 V	|				|								|
		 |	|				|								|
---------o--------------------------------------------------R
							|				( sectionL->x2E, sectionL->y2E )
							|
						  Y	|
							|
							|
							V

  To understand how the algorithm work refer to the diagram above.
  The image boundaries are indicated by letter "I" ( 0, 0 ) to ( srcPtrA->widthE, srcPtrA->heightE )
  The rectangle boundaries are indicated by letter "R" ( sectionPtrA->x1E, sectionPtrA->y1E ) to ( sectionPtrA->x2E, sectionPtrA->y2E )

  In the above example the intersection of the image and the rectange is
  ( 0, 0 ), ( srcPtrA->widthE, srcPtrA->heightE )

  The size of the destination image is always ( ( sectionL->x2E, sectionL->y2E ) - ( sectionL->x1E, sectionL->y1E ) )

  All coordinates are assumed to be relative to the original image.

  1. parse all pixels in "loop y1"
	1.a. parse all pixels in "loop x1"
	1.b. parse all pixels in "loop x2"
	1.c. parse all pixels in "loop x3"
  2. parse all pixels in "loop y2"
	2.a. parse all pixels in "loop x1"
	2.b. parse all pixels in "loop x2"
	2.c. parse all pixels in "loop x3"
  3. parse all pixels in "loop y3"
	3.a. parse all pixels in "loop x1"
	3.b. parse all pixels in "loop x2"
	3.c. parse all pixels in "loop x3"

*/

/** copies a section of given image */
void bim_ComplexImage_copySection( struct bbs_Context* cpA,
								   struct bim_ComplexImage* ptrA, 
								   const struct bim_ComplexImage* srcPtrA, 
								   const struct bts_Int16Rect* sectionPtrA )
{

	struct bbs_Complex* srcPixelPtrL;
	struct bbs_Complex* dstPixelPtrL;
	int32 yIndexL;
	int32 xIndexL;

	struct bts_Int16Rect srcImageSubSectionL;
	struct bts_Int16Rect sectionL;

	/* make sure that the rectangle passed is correct, in case the x2 < x1 or y2 < y1, swap them */
	sectionL.x1E = bbs_min( sectionPtrA->x1E, sectionPtrA->x2E );
	sectionL.x2E = bbs_max( sectionPtrA->x1E, sectionPtrA->x2E );
	sectionL.y1E = bbs_min( sectionPtrA->y1E, sectionPtrA->y2E );
	sectionL.y2E = bbs_max( sectionPtrA->y1E, sectionPtrA->y2E );

	/* find the intersection betweem the rectangle and the image, the image always starts at 0,0 */
	srcImageSubSectionL.x1E = bbs_max( 0, sectionL.x1E );
	srcImageSubSectionL.y1E = bbs_max( 0, sectionL.y1E );
	srcImageSubSectionL.x2E = bbs_min( ( int32 ) srcPtrA->widthE, sectionL.x2E );
	srcImageSubSectionL.y2E = bbs_min( ( int32 ) srcPtrA->heightE, sectionL.y2E );

	/* If the image and the rectangle do not intersect in X direction, set the intersecting rectangle to the image coordinates */
	if( srcImageSubSectionL.x2E < srcImageSubSectionL.x1E )
	{
		srcImageSubSectionL.x1E = 0;
		srcImageSubSectionL.x2E = srcPtrA->widthE;
	}
	/* do the same as above in the Y direction */
	if( srcImageSubSectionL.y2E < srcImageSubSectionL.y1E )
	{
		srcImageSubSectionL.y1E = 0;
		srcImageSubSectionL.y2E = srcPtrA->heightE;
	}

	/* set size, and allocate required memory for the destination image if required */
	bim_ComplexImage_size( cpA, ptrA, sectionL.x2E - sectionL.x1E, sectionL.y2E - sectionL.y1E );

	/* get the pointer to the destination image */
	dstPixelPtrL = ptrA->arrE.arrPtrE;

	/* 1. parse all pixels in "loop y1" */
	for( yIndexL = sectionL.y1E; yIndexL < srcImageSubSectionL.y1E && yIndexL < sectionL.y2E; yIndexL++ )
	{
		/* move to the first pixel that needs to be copied. */
		srcPixelPtrL = srcPtrA->arrE.arrPtrE;

		/* 1.a. parse all pixels in "loop x1" */
		for( xIndexL = sectionL.x1E; xIndexL < srcImageSubSectionL.x1E && xIndexL < sectionL.x2E; xIndexL++ )
		{
			*dstPixelPtrL++ = *srcPixelPtrL;
		}
		/* 1.b. parse all pixels in "loop x2" */
		for( ; xIndexL < srcImageSubSectionL.x2E && xIndexL < sectionL.x2E; xIndexL++ )
		{
			*dstPixelPtrL++ = *srcPixelPtrL++;
		}
		srcPixelPtrL--;
		/* 1.c. parse all pixels in "loop x3" */
		for( ; xIndexL < sectionL.x2E; xIndexL++ )
		{
			*dstPixelPtrL++ = *srcPixelPtrL;
		}
	}
	/* 2. parse all pixels in "loop y2" */
	for( ; yIndexL < srcImageSubSectionL.y2E && yIndexL < sectionL.y2E; yIndexL++ )
	{
		/* move to the first pixel that needs to be copied. */
		srcPixelPtrL = srcPtrA->arrE.arrPtrE + yIndexL * srcPtrA->widthE + srcImageSubSectionL.x1E;

		/* 2.a. parse all pixels in "loop x1" */
		for( xIndexL = sectionL.x1E; xIndexL < srcImageSubSectionL.x1E && xIndexL < sectionL.x2E; xIndexL++ )
		{
			*dstPixelPtrL++ = *srcPixelPtrL;
		}
		/* 2.b. parse all pixels in "loop x2" */
		for( ; xIndexL < srcImageSubSectionL.x2E && xIndexL < sectionL.x2E; xIndexL++ )
		{
			*dstPixelPtrL++ = *srcPixelPtrL++;
		}
		srcPixelPtrL--;
		/* 2.c. parse all pixels in "loop x3" */
		for( ; xIndexL < sectionL.x2E; xIndexL++ )
		{
			*dstPixelPtrL++ = *srcPixelPtrL;
		}
	}
	/* 3. parse all pixels in "loop y3" */
	for( ; yIndexL < sectionL.y2E; yIndexL++ )
	{
		srcPixelPtrL = srcPtrA->arrE.arrPtrE + ( srcImageSubSectionL.y2E - 1 ) * srcPtrA->widthE + srcImageSubSectionL.x1E;

		/* 3.a. parse all pixels in "loop x1" */
		for( xIndexL = sectionL.x1E; xIndexL < srcImageSubSectionL.x1E && xIndexL < sectionL.x2E; xIndexL++ )
		{
			*dstPixelPtrL++ = *srcPixelPtrL;
		}
		/* 3.b. parse all pixels in "loop x3" */
		for( ; xIndexL < srcImageSubSectionL.x2E && xIndexL < sectionL.x2E; xIndexL++ )
		{
			*dstPixelPtrL++ = *srcPixelPtrL++;
		}
		srcPixelPtrL--;
		/* 3.c. parse all pixels in "loop x3" */
		for( ; xIndexL < sectionL.x2E; xIndexL++ )
		{
			*dstPixelPtrL++ = *srcPixelPtrL;
		}
	}

}

/* ------------------------------------------------------------------------- */

void bim_ComplexImage_importAPh( struct bbs_Context* cpA,
								 struct bim_ComplexImage* dstPtrA, 
								 const struct bim_APhImage* srcPtrA )
{
	long iL;
	struct bbs_Complex* dstL;
	const struct bbs_APh* srcL;
	bim_ComplexImage_size( cpA, dstPtrA, srcPtrA->widthE, srcPtrA->heightE );
	dstL = dstPtrA->arrE.arrPtrE;
	srcL = srcPtrA->arrE.arrPtrE;
	for( iL = srcPtrA->widthE * srcPtrA->heightE; iL > 0; iL-- )
	{
		bbs_Complex_importAPh( dstL++, srcL++ );
	}	
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


