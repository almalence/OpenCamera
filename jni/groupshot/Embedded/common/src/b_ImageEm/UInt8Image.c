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
#include "b_ImageEm/UInt8Image.h"

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

void bim_UInt8Image_init( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA )
{
	bbs_UInt8Arr_init( cpA, &ptrA->arrE );
	ptrA->widthE = 0;
	ptrA->heightE = 0;
}

/* ------------------------------------------------------------------------- */

void bim_UInt8Image_create( struct bbs_Context* cpA,
						    struct bim_UInt8Image* ptrA, 
						    uint32 widthA, 
							uint32 heightA,
 					        struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	if( ptrA->arrE.arrPtrE != 0 )
	{
		bim_UInt8Image_size( cpA, ptrA, widthA, heightA );
	}
	else
	{
		bbs_UInt8Arr_create( cpA, &ptrA->arrE, widthA * heightA, mspA );
		ptrA->widthE  = widthA;
		ptrA->heightE = heightA;
	}
}
/* ------------------------------------------------------------------------- */

void bim_UInt8Image_exit( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA )
{
	bbs_UInt8Arr_exit( cpA, &ptrA->arrE );
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

void bim_UInt8Image_copy( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA, 
						  const struct bim_UInt8Image* srcPtrA )
{
#ifdef DEBUG1
	if( ptrA->arrE.sizeE < srcPtrA->arrE.sizeE )
	{
		bbs_ERROR0( "void bim_UInt8Image_copy( struct bim_UInt8Image*, const struct bim_UInt8Image* ):\n"
				   "Unsufficient allocated memory in destination image" );		
		return;
	}
#endif
	ptrA->widthE = srcPtrA->widthE;
	ptrA->heightE = srcPtrA->heightE;
	bbs_UInt8Arr_copy( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

flag bim_UInt8Image_equal( struct bbs_Context* cpA,
						   const struct bim_UInt8Image* ptrA, 
						   const struct bim_UInt8Image* srcPtrA )
{
	if( ptrA->widthE != srcPtrA->widthE ) return FALSE;
	if( ptrA->heightE != srcPtrA->heightE ) return FALSE;
	return bbs_UInt8Arr_equal( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bim_UInt8Image_checkSum( struct bbs_Context* cpA,
							    const struct bim_UInt8Image* ptrA )
{
	uint32 sumL =0 ;
	uint32 iL;
	uint32 sizeL = ptrA->arrE.sizeE;
	const uint8* ptrL = ptrA->arrE.arrPtrE;
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
	
void bim_UInt8Image_assignExternalImage( struct bbs_Context* cpA,
										 struct bim_UInt8Image* ptrA, 
										 struct bim_UInt8Image* srcPtrA )
{
	struct bbs_MemSeg sharedSegL = bbs_MemSeg_createShared( cpA, srcPtrA->arrE.arrPtrE, ( srcPtrA->widthE * srcPtrA->heightE ) / 2 );

	if( ptrA->arrE.arrPtrE != 0 )
	{
		bbs_ERROR0( "void bim_UInt8Image_assignExternalImage( ... ): image was already created once" );
		return;
	}

	bim_UInt8Image_create( cpA, ptrA, 
					       srcPtrA->widthE, 
						   srcPtrA->heightE,
						   &sharedSegL );
}

/* ------------------------------------------------------------------------- */
	
void bim_UInt8Image_size( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA, 
						  uint32 widthA, 
						  uint32 heightA )
{
	if( ptrA->arrE.allocatedSizeE < widthA * heightA )
	{
		bbs_ERROR0( "void bim_UInt8Image_size( struct bim_UInt8Image*, uint32 sizeA ):\n"
				   "Unsufficient allocated memory" );
		return;
	}
	bbs_UInt8Arr_size( cpA, &ptrA->arrE, widthA * heightA );
	ptrA->widthE  = widthA;
	ptrA->heightE = heightA;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt8Image_memSize( struct bbs_Context* cpA,
							   const struct bim_UInt8Image* ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->widthE ) 
		  + bbs_SIZEOF16( ptrA->heightE )
		  + bbs_UInt8Arr_memSize( cpA, &ptrA->arrE ); 
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt8Image_memWrite( struct bbs_Context* cpA,
							    const struct bim_UInt8Image* ptrA, 
								uint16* memPtrA )
{
	uint32 memSizeL = bim_UInt8Image_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bim_UINT8_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->heightE, memPtrA );
	bbs_UInt8Arr_memWrite( cpA, &ptrA->arrE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt8Image_memRead( struct bbs_Context* cpA,
							   struct bim_UInt8Image* ptrA, 
							   const uint16* memPtrA,
 					           struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL, widthL, heightL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bim_UINT8_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &widthL, memPtrA );
	memPtrA += bbs_memRead32( &heightL, memPtrA );

	ptrA->widthE  = widthL;
	ptrA->heightE = heightL;
	bbs_UInt8Arr_memRead( cpA, &ptrA->arrE, memPtrA, mspA );

	if( memSizeL != bim_UInt8Image_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bim_UInt8Image_memRead( const struct bim_UInt8Image* ptrA, const void* memPtrA ):\n"
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

void bim_UInt8Image_setAllPixels( struct bbs_Context* cpA,
								  struct bim_UInt8Image* ptrA, 
								  uint8 valueA )
{
	long iL;
	uint8* ptrL = ptrA->arrE.arrPtrE;
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
void bim_UInt8Image_copySection( struct bbs_Context* cpA,
								 struct bim_UInt8Image* ptrA, 
								 const struct bim_UInt8Image* srcPtrA, 
								 const struct bts_Int16Rect* sectionPtrA )
{

	uint8* srcPixelPtrL;
	uint8* dstPixelPtrL;
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
	bim_UInt8Image_size( cpA, ptrA, sectionL.x2E - sectionL.x1E, sectionL.y2E - sectionL.y1E );

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

/**

 
		M-------------------------------------------------------M
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|	region x0y0	|		region x1y0		|	region x2y0	|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|---------------I-----------------------I---------------|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|	region x0y1	|		region x1y1		|	region x2y1	|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|---------------I-----------------------I---------------|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		|	region x0y2	|		region x1y2		|	region x2y2	|
		|				|						|				|
		|				|						|				|
		|				|						|				|
		M-------------------------------------------------------M


  To see how the code is organized. Refer to the diagram above.
  Assume the original image after applying the tranzformations(translation, rotation and scaling) is "O" 
	(boundaries of the image are shown above bounded by the letter 'O').
  This image is being Warped to the area "M" (boundaries of this area are bounded by the letter 'M').
  
	Refer to the source code below to point to the loop that maps pixels in the particular region.

 */

/** applies affine linear warping to pixels positions of imageA before copying the into *ptrA */
void bim_UInt8Image_warpOffs( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA, 
						  const struct bim_UInt8Image* srcPtrA, 
						  int32 xOffsA,
						  int32 yOffsA,
						  const struct bts_Flt16Alt2D* altPtrA,
			              int32 resultWidthA,
			              int32 resultHeightA )
{
	long srcWidthL = srcPtrA->widthE;
	long srcHeightL = srcPtrA->heightE;
	
	struct bts_Flt16Alt2D invAlt2DL;
	
	uint8* dstPtrL;
	const uint8* ulPtrL = srcPtrA->arrE.arrPtrE;
	const uint8* urPtrL = ulPtrL + srcWidthL - 1;
	const uint8* llPtrL = ulPtrL + ( srcHeightL - 1 ) * srcWidthL;
	const uint8* lrPtrL = llPtrL + srcWidthL - 1;
	
	uint32 iL, jL;
	int32 shiftL;

	const uint16 bbpL = 16;
	int32 maxInt32Value8bbpL  = 0x7FFFFFFF;

	/* The bbp for all these variables is the same as bbpL */
	int32 mxxL;
	int32 mxyL;
	int32 myxL;
	int32 myyL;

	int32 txL;
	int32 tyL;

	int32 xL;
	int32 yL;

	bim_UInt8Image_size( cpA, ptrA, resultWidthA, resultHeightA );
	dstPtrL = ptrA->arrE.arrPtrE;
	
	/* compute inverse */
	invAlt2DL = bts_Flt16Alt2D_inverted( altPtrA );
	
	if( srcWidthL == 0 || srcHeightL == 0 )
	{
		bim_UInt8Image_size( cpA, ptrA, srcWidthL, srcHeightL );
		bbs_ERROR2( "Size of output image is %d/%d", srcWidthL, srcHeightL );
		return;
	}

	/* align Matrix and Vector to 8 bits bbp */
	shiftL = invAlt2DL.matE.bbpE - bbpL;
	if( shiftL >= 0 )
	{
		mxxL = invAlt2DL.matE.xxE >> shiftL;
		mxyL = invAlt2DL.matE.xyE >> shiftL;
		myxL = invAlt2DL.matE.yxE >> shiftL;
		myyL = invAlt2DL.matE.yyE >> shiftL;
	}
	else
	{
		/* Check for overflow since we are left shifting. */
		maxInt32Value8bbpL >>= -shiftL;
		if( invAlt2DL.matE.xxE > maxInt32Value8bbpL ||
			invAlt2DL.matE.xyE > maxInt32Value8bbpL ||
			invAlt2DL.matE.yxE > maxInt32Value8bbpL ||
			invAlt2DL.matE.yyE > maxInt32Value8bbpL )
		{
			/* Overflow error */
			bbs_ERROR5( "The values in the transformation matrix cause overflow during bitshift\n%d, %d,\n%d, %d\n"
						"The maximum allowed value is %d", 
						invAlt2DL.matE.xxE >> invAlt2DL.matE.bbpE,
						invAlt2DL.matE.xyE >> invAlt2DL.matE.bbpE,
						invAlt2DL.matE.yxE >> invAlt2DL.matE.bbpE,
						invAlt2DL.matE.yyE >> invAlt2DL.matE.bbpE,
						maxInt32Value8bbpL >> ( bbpL - ( -shiftL ) ) );
			return;
		}

		mxxL = invAlt2DL.matE.xxE << -shiftL;
		mxyL = invAlt2DL.matE.xyE << -shiftL;
		myxL = invAlt2DL.matE.yxE << -shiftL;
		myyL = invAlt2DL.matE.yyE << -shiftL;
		maxInt32Value8bbpL <<= -shiftL;
	}

	/* invAlt2DL.matE.bbpE = bbpL; nonsense! */

	shiftL = invAlt2DL.vecE.bbpE - bbpL;
	if( shiftL >= 0 )
	{
		txL  = invAlt2DL.vecE.xE >> shiftL;
		tyL  = invAlt2DL.vecE.yE >> shiftL;
	}
	else
	{
		/* Check for overflow since we are left shifting. */
		maxInt32Value8bbpL >>= -shiftL;
		if(	invAlt2DL.vecE.xE  > maxInt32Value8bbpL ||
			invAlt2DL.vecE.yE  > maxInt32Value8bbpL )
		{
			/* Overflow error */
			bbs_ERROR3( "The values in the vector cause overflow during bitshift\n%d, %d,\n"
						"The maximum allowed value is %d", 
						invAlt2DL.vecE.xE >> invAlt2DL.vecE.bbpE,
						invAlt2DL.vecE.yE >> invAlt2DL.vecE.bbpE,
						maxInt32Value8bbpL >> ( bbpL - ( -shiftL ) ) );
			return;
		}
		txL  = invAlt2DL.vecE.xE << -shiftL;
		tyL  = invAlt2DL.vecE.yE << -shiftL;
		maxInt32Value8bbpL <<= -shiftL;
	}

	/* invAlt2DL.vecE.bbpE = bbpL; nonsense! */

	/* adjust offset */
	txL += xOffsA << bbpL;
	tyL += yOffsA << bbpL;

	/* For each destination pixel find the correspoding source pixel by applying the inverse transformation */
	for( jL = 0; jL < ptrA->heightE; jL++ )
	{
		xL = txL + mxyL * jL;
		yL = tyL + myyL * jL;
		for( iL = 0; iL < ptrA->widthE; iL++ )
		{
			const uint16 bbpLby2L = bbpL / 2;
			const int32 oneL = 0x00000001 << bbpLby2L;
			const int32 fractionOnlyL = 0xFFFFFFFF >> ( 32 - bbpL );

			/* The bbp for all these variables is the same as bbpLby2L */
			int32 f2xL;
			int32 f2yL;
			int32 f1xL;
			int32 f1yL;

			/* always whole numbers with a bbp of 0 */
			int32 kL;
			int32 lL;

			/* The bbpE for these variables is bbpLby2L */
			int32 valL;

			/* Get the whole numbers only and make the bbp 0. */
			kL = xL >> bbpL;
			lL = yL >> bbpL;

			/* fraction of destination pixel in the next source pixel */
			f2xL = ( xL & fractionOnlyL ) >> bbpLby2L;
			f2yL = ( yL & fractionOnlyL ) >> bbpLby2L;
			/* fraction of destination pixel in the current source pixel */
			f1xL = oneL - f2xL;
			f1yL = oneL - f2yL;

			/* increment values for next loop */
			xL += mxxL;
			yL += myxL;

			if( lL < 0 )
			{
				if( kL < 0 )
				{
					/* handle all pixels in region x0y0 */
					*dstPtrL++ = *ulPtrL;
				}
				else if( kL >= srcWidthL - 1 )
				{
					/* handle all pixels in region x2y0 */
					*dstPtrL++ = *urPtrL;
				}
				else
				{
					/* handle all pixels in region x1y0 */
					/* The bbp has shifted left by bbpLby2L */
					valL =  *( ulPtrL + kL ) * f1xL + *( ulPtrL + kL + 1 ) * f2xL;
					*dstPtrL++ = valL >> bbpLby2L;
				}
			} /* if( lL < 0 ) */
			else if( lL >= srcHeightL - 1 )
			{
				if( kL < 0 )
				{
					/* handle all pixels in region x0y2 */
					*dstPtrL++ = *llPtrL;
				}
				else if( kL >= srcWidthL - 1 )
				{
					/* handle all pixels in region x2y2 */
					*dstPtrL++ = *lrPtrL;
				}
				else
				{
					/* handle all pixels in region x1y2 */
					/* The bbp has shifted left by bbpLby2L */
					valL =   *( llPtrL + kL ) * f1xL + *( llPtrL +  kL + 1 ) * f2xL;
					*dstPtrL++ = valL >> bbpLby2L;
				}
			} /* if( lL >= srcHeightL - 1 ) */
			else
			{
				const uint8* ptr1L;
				const uint8* ptr2L;

				ptr1L = ulPtrL + lL * srcWidthL;
				/* point to the pixel in the same column */
				ptr2L = ptr1L + srcWidthL;
				if( kL < 0 )
				{
					/* handle all pixels in region x0y1 */
					/* The bbp has shifted left by bbpLby2L */
					valL = *ptr1L * f1yL + *ptr2L * f2yL ;
					*dstPtrL++ = valL >> bbpLby2L;
				}
				else if( kL >= srcWidthL - 1 )
				{
					/* handle all pixels in region x2y1 */
					/* The bbp has shifted left by bbpLby2L */
					valL =  *( ptr1L + srcWidthL - 1 ) * f1yL + *( ptr2L  + srcWidthL - 1 ) * f2yL;
					*dstPtrL++ = valL >> bbpLby2L;
				}
				else
				{
					/* assuming that bbpL = bbpLby2 * 2 */
					/* The bbp for these variables is bbpLby2L */
					int32 v1L;
					int32 v2L;
					/* The bbp for these variables is bbpL */
					const int32 halfL = 0x00000001 << ( bbpL - 1 );
	
					/* handle all pixels in region x1y1 */
					/* The bbp has shifted left by bbpLby2L */
					v1L = *( ptr1L + kL ) * f1xL + *( ptr1L + kL + 1 ) * f2xL;
					v2L = *( ptr2L + kL ) * f1xL + *( ptr2L + kL + 1 ) * f2xL;
					/* The bbp has shifted left again by bbpLby2L */
					/* adding the half to round off the resulting value */
					valL = v1L * f1yL + v2L * f2yL + halfL;
					*dstPtrL++ = valL >> bbpL;
				}
			}
		} /* iL loop */
	} /* jL loop */

}

/* ------------------------------------------------------------------------- */

void bim_UInt8Image_warp( struct bbs_Context* cpA,
						  struct bim_UInt8Image* ptrA, 
						  const struct bim_UInt8Image* srcPtrA, 
						  const struct bts_Flt16Alt2D* altPtrA,
			              int32 resultWidthA,
			              int32 resultHeightA )
{
	bim_UInt8Image_warpOffs( cpA, ptrA, srcPtrA, 0, 0, altPtrA, resultWidthA, resultHeightA );
}

/* ========================================================================= */


