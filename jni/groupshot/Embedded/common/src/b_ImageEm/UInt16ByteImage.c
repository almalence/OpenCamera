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
#include "b_ImageEm/UInt16ByteImage.h"

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

void bim_UInt16ByteImage_init( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA )
{
	bbs_UInt16Arr_init( cpA, &ptrA->arrE );
	ptrA->widthE = 0;
	ptrA->heightE = 0;
}

/* ------------------------------------------------------------------------- */

void bim_UInt16ByteImage_exit( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA )
{
	bbs_UInt16Arr_exit( cpA, &ptrA->arrE );
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

void bim_UInt16ByteImage_copy( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA, 
							   const struct bim_UInt16ByteImage* srcPtrA )
{
#ifdef DEBUG1
	if( ptrA->arrE.sizeE < srcPtrA->arrE.sizeE )
	{
		bbs_ERROR0( "void bim_UInt16ByteImage_copy( struct bim_UInt16ByteImage*, const struct bim_UInt16ByteImage* ):\n"
				   "Unsufficient allocated memory in destination image" );		
		return;
	}
#endif
	ptrA->widthE = srcPtrA->widthE;
	ptrA->heightE = srcPtrA->heightE;
	bbs_UInt16Arr_copy( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

flag bim_UInt16ByteImage_equal( struct bbs_Context* cpA,
							    const struct bim_UInt16ByteImage* ptrA, 
								const struct bim_UInt16ByteImage* srcPtrA )
{
	if( ptrA->widthE != srcPtrA->widthE ) return FALSE;
	if( ptrA->heightE != srcPtrA->heightE ) return FALSE;
	return bbs_UInt16Arr_equal( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bim_UInt16ByteImage_checkSum( struct bbs_Context* cpA,
									 const struct bim_UInt16ByteImage* ptrA )
{
	uint32 sumL =0 ;
	uint32 iL;
	uint32 sizeL = ptrA->arrE.sizeE;
	const uint16* ptrL = ptrA->arrE.arrPtrE;
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
	
void bim_UInt16ByteImage_create( struct bbs_Context* cpA,
								 struct bim_UInt16ByteImage* ptrA, 
						         uint32 widthA, 
							     uint32 heightA,
 					             struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	if( widthA & 1 )
	{
		bbs_ERROR0( "bim_UInt16ByteImage_create( .... ): width of image must be even" );
		return;
	}

	if( ptrA->arrE.arrPtrE != 0 )
	{
		bim_UInt16ByteImage_size( cpA, ptrA, widthA, heightA );
	}
	else
	{
		bbs_UInt16Arr_create( cpA, &ptrA->arrE, ( widthA * heightA ) >> 1, mspA );
		ptrA->widthE  = widthA;
		ptrA->heightE = heightA;
	}
}

/* ------------------------------------------------------------------------- */

void bim_UInt16ByteImage_assignExternalImage( struct bbs_Context* cpA,
											  struct bim_UInt16ByteImage* ptrA, 
											  struct bim_UInt16ByteImage* srcPtrA )
{
	struct bbs_MemSeg sharedSegL = bbs_MemSeg_createShared( cpA, srcPtrA->arrE.arrPtrE, ( srcPtrA->widthE * srcPtrA->heightE ) / 2 );

	if( ptrA->arrE.arrPtrE != 0 )
	{
		bbs_ERROR0( "void bim_UInt16ByteImage_assignExternalImage( ... ): image was already created once" );
		return;
	}

	bim_UInt16ByteImage_create( cpA, ptrA, 
					            srcPtrA->widthE, 
						        srcPtrA->heightE,
						        &sharedSegL );
}

/* ------------------------------------------------------------------------- */
	
void bim_UInt16ByteImage_size( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA, 
							   uint32 widthA, uint32 heightA )
{
	if( widthA & 1 )
	{
		bbs_ERROR0( "bim_UInt16ByteImage_size( .... ): width of image must be even" );
		return;
	}

	if( ptrA->arrE.allocatedSizeE < ( ( widthA * heightA ) >> 1 ) )
	{
		bbs_ERROR0( "void bim_UInt16ByteImage_size( struct bim_UInt16ByteImage*, uint32 sizeA ):\n"
				   "Unsufficient allocated memory" );
		return;
	}
	bbs_UInt16Arr_size( cpA, &ptrA->arrE, ( widthA * heightA ) >> 1 );
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
	
uint32 bim_UInt16ByteImage_memSize( struct bbs_Context* cpA,
								    const struct bim_UInt16ByteImage* ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->widthE ) 
		  + bbs_SIZEOF16( ptrA->heightE )
		  + bbs_UInt16Arr_memSize( cpA, &ptrA->arrE ); 
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt16ByteImage_memWrite( struct bbs_Context* cpA,
									 const struct bim_UInt16ByteImage* ptrA, 
									 uint16* memPtrA )
{
	uint32 memSizeL = bim_UInt16ByteImage_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bim_UINT16_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->heightE, memPtrA );
	bbs_UInt16Arr_memWrite( cpA, &ptrA->arrE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bim_UInt16ByteImage_memRead( struct bbs_Context* cpA,
								    struct bim_UInt16ByteImage* ptrA, 
							   const uint16* memPtrA,
 					           struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL, widthL, heightL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bim_UINT16_IMAGE_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &widthL, memPtrA );
	memPtrA += bbs_memRead32( &heightL, memPtrA );

	ptrA->widthE  = widthL;
	ptrA->heightE = heightL;
	bbs_UInt16Arr_memRead( cpA, &ptrA->arrE, memPtrA, mspA );

	if( memSizeL != bim_UInt16ByteImage_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bim_UInt16ByteImage_memRead( const struct bim_UInt16ByteImage* ptrA, const void* memPtrA ):\n"
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

void bim_UInt16ByteImage_setAllPixels( struct bbs_Context* cpA,
									   struct bim_UInt16ByteImage* ptrA, 
									   uint16 valueA )
{
	long iL;
	uint16* ptrL = ptrA->arrE.arrPtrE;
	uint16 fillL = ( valueA & 0x0FF ) | ( ( valueA & 0x0FF ) << 8 );
	for( iL = ptrA->arrE.sizeE; iL > 0; iL-- )
	{
		*ptrL++ = fillL;
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
void bim_UInt16ByteImage_warp( struct bbs_Context* cpA,
							   struct bim_UInt16ByteImage* ptrA, 
						       const struct bim_UInt16ByteImage* srcPtrA, 
						       const struct bts_Flt16Alt2D* altPtrA,
			                   int32 resultWidthA,
			                   int32 resultHeightA )
{
	long srcWidthL = srcPtrA->widthE;
	long srcHeightL = srcPtrA->heightE;
	long halfSrcWidthL = srcWidthL >> 1;
	
	struct bts_Flt16Alt2D invAlt2DL;
	
	uint16* dstPtrL;
	const uint16* ulPtrL = srcPtrA->arrE.arrPtrE;
	const uint16* urPtrL = ulPtrL + halfSrcWidthL - 1;
	const uint16* llPtrL = ulPtrL + ( srcHeightL - 1 ) * halfSrcWidthL;
	const uint16* lrPtrL = llPtrL + halfSrcWidthL - 1;
	
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

	bim_UInt16ByteImage_size( cpA, ptrA, resultWidthA, resultHeightA );
	dstPtrL = ptrA->arrE.arrPtrE;
	
	/* compute inverse */
	invAlt2DL = bts_Flt16Alt2D_inverted( altPtrA );
	
	if( srcWidthL == 0 || srcHeightL == 0 )
	{
		bim_UInt16ByteImage_size( cpA, ptrA, srcWidthL, srcHeightL );
		bbs_ERROR2( "Size of output image is %d/%d", srcWidthL, srcHeightL );
		return;
	}

	/* align Matrix and Vector to 8 bits bbp */
	shiftL = invAlt2DL.matE.bbpE - bbpL;
	if( shiftL >= 0 )
	{
		mxxL = ( int32 )invAlt2DL.matE.xxE >> shiftL;
		mxyL = ( int32 )invAlt2DL.matE.xyE >> shiftL;
		myxL = ( int32 )invAlt2DL.matE.yxE >> shiftL;
		myyL = ( int32 )invAlt2DL.matE.yyE >> shiftL;
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

		mxxL = ( int32 )invAlt2DL.matE.xxE << -shiftL;
		mxyL = ( int32 )invAlt2DL.matE.xyE << -shiftL;
		myxL = ( int32 )invAlt2DL.matE.yxE << -shiftL;
		myyL = ( int32 )invAlt2DL.matE.yyE << -shiftL;
		maxInt32Value8bbpL <<= -shiftL;
	}
	invAlt2DL.matE.bbpE = bbpL;

	shiftL = invAlt2DL.vecE.bbpE - bbpL;
	if( shiftL >= 0 )
	{
		txL  = ( int32 )invAlt2DL.vecE.xE >> shiftL;
		tyL  = ( int32 )invAlt2DL.vecE.yE >> shiftL;
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
		txL  = ( int32 )invAlt2DL.vecE.xE << -shiftL;
		tyL  = ( int32 )invAlt2DL.vecE.yE << -shiftL;
		maxInt32Value8bbpL <<= -shiftL;
	}
	invAlt2DL.vecE.bbpE = bbpL;

	/* For each destination pixel find the correspoding source pixel by applying the inverse transformation */
	for( jL = 0; jL < ptrA->heightE; jL++ )
	{
		xL = txL + mxyL * jL;
		yL = tyL + myyL * jL;
		for( iL = 0; iL < ptrA->widthE; iL++ )
		{
			const uint16 bbpLby2L = bbpL / 2;
			const int32 oneL = ( int32 )0x00000001 << bbpLby2L;
			const int32 fractionOnlyL = 0xFFFFFFFF >> ( 32 - bbpL );
			uint16 dstPixelL;

			/* The bbp for all these variables is the same as bbpLby2L */
			int32 f2xL;
			int32 f2yL;
			int32 f1xL;
			int32 f1yL;

			/* always whole numbers with a bbp of 0 */
			int32 kL, khL;
			int32 lL;

			flag kEvenL;

			/* The bbpE for these variables is bbpLby2L */
			int32 valL;

			/* Get the whole numbers only and make the bbp 0. */
			kL = xL >> bbpL;
			lL = yL >> bbpL;

			khL = kL >> 1;
			kEvenL = !( kL & 1 );

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
					dstPixelL = *ulPtrL & 0x0FF;
				}
				else if( kL >= srcWidthL - 1 )
				{
					/* handle all pixels in region x2y0 */
					dstPixelL = *urPtrL >> 8;
				}
				else
				{
					/* handle all pixels in region x1y0 */
					/* The bbp has shifted left by bbpLby2L */
					if( kEvenL )
					{
						uint16 srcL = *( ulPtrL + khL );
						valL = f1xL * ( srcL & 0x00FF )  +  f2xL * ( srcL >> 8 );
					}
					else
					{
						valL =  f1xL * ( *( ulPtrL + khL ) >> 8 ) + f2xL * ( *( ulPtrL + khL + 1 ) & 0x0FF );
					}
					dstPixelL = valL >> bbpLby2L;
				}
			} /* if( lL < 0 ) */
			else if( lL >= srcHeightL - 1 )
			{
				if( kL < 0 )
				{
					/* handle all pixels in region x0y2 */
					dstPixelL = *llPtrL & 0x0FF;
				}
				else if( kL >= srcWidthL - 1 )
				{
					/* handle all pixels in region x2y2 */
					dstPixelL = *lrPtrL >> 8;
				}
				else
				{
					/* handle all pixels in region x1y2 */
					/* The bbp has shifted left by bbpLby2L */
					if( kEvenL )
					{
						uint16 srcL = *( llPtrL + khL );
						valL = f1xL * ( srcL & 0x00FF ) + f2xL * ( srcL >> 8 );
					}
					else
					{
						valL =  f1xL * ( *( llPtrL + khL ) >> 8 ) + f2xL * ( *( llPtrL + khL + 1 ) & 0x0FF );
					}

					dstPixelL = valL >> bbpLby2L;
				}
			} /* if( lL >= srcHeightL - 1 ) */
			else
			{
				const uint16* ptr1L;
				const uint16* ptr2L;

				ptr1L = ulPtrL + lL * halfSrcWidthL;
				/* point to the pixel in the same column */
				ptr2L = ptr1L + halfSrcWidthL;
				if( kL < 0 )
				{
					/* handle all pixels in region x0y1 */
					valL =  f1yL * ( *ptr1L & 0x0FF ) + f2yL * ( *ptr2L & 0x0FF );
					dstPixelL = valL >> bbpLby2L;
				}
				else if( kL >= srcWidthL - 1 )
				{
					/* handle all pixels in region x2y1 */
					valL = f1yL * ( *( ptr1L + halfSrcWidthL - 1 ) >> 8 ) + 
						   f2yL * ( *( ptr2L + halfSrcWidthL - 1 ) >> 8 );
					dstPixelL = valL >> bbpLby2L;
				}
				else
				{
					/* assuming that bbpL = bbpLby2 * 2 */
					/* The bbp for these variables is bbpL */
					int32 v1L;
					int32 v2L;
					const int32 halfL = ( int32 )0x00000001 << ( bbpL - 1 );
	
					/* handle all pixels in region x1y1 */
					if( kEvenL )
					{
						#ifdef HW_BIG_ENDIAN
							/* Our images are in byte order for big & little endian  so when using a
                                                           16bit ptr our bytes will be swapped on big endian hardware shift and mask*/
							v1L = f1xL * ( *( ptr1L + khL ) >> 8 ) + f2xL * ( *( ptr1L + khL ) & 0x0FF );
							v2L = f1xL * ( *( ptr2L + khL ) >> 8 ) + f2xL * ( *( ptr2L + khL ) & 0x0FF );
						#else
							v1L = f1xL * ( *( ptr1L + khL ) & 0x0FF ) + f2xL * ( *( ptr1L + khL ) >> 8 );
							v2L = f1xL * ( *( ptr2L + khL ) & 0x0FF ) + f2xL * ( *( ptr2L + khL ) >> 8 );
						#endif
					}
					else
					{
						#ifdef HW_BIG_ENDIAN
							v1L = f1xL * ( *( ptr1L + khL ) & 0x0FF ) + f2xL * ( *( ptr1L + khL + 1 ) >> 8 );
							v2L = f1xL * ( *( ptr2L + khL ) & 0x0FF ) + f2xL * ( *( ptr2L + khL + 1 ) >> 8 );
						#else					
							v1L = f1xL * ( *( ptr1L + khL ) >> 8 ) + f2xL * ( *( ptr1L + khL + 1 ) & 0x0FF );
							v2L = f1xL * ( *( ptr2L + khL ) >> 8 ) + f2xL * ( *( ptr2L + khL + 1 ) & 0x0FF );
						#endif
					}
					/* adding the half to round off the resulting value */
					valL = v1L * f1yL + v2L * f2yL + halfL;
					dstPixelL = valL >> bbpL;
				}
			}

			if( iL & 1 )
			{
				#ifdef HW_BIG_ENDIAN
					*dstPtrL |= dstPixelL & 0x0FF;
				#else			
					*dstPtrL |= dstPixelL << 8;
				#endif
				dstPtrL++;
			}
			else
			{
				#ifdef HW_BIG_ENDIAN
					*dstPtrL = dstPixelL << 8;
				#else			
					*dstPtrL = dstPixelL & 0x0FF;
				#endif
			}

		} /* iL loop */
	} /* jL loop */

}

/* ------------------------------------------------------------------------- */

#ifndef HW_TMS320C5x /* 16bit architecture excluded */

void bim_UInt16ByteImage_warp8( struct bbs_Context* cpA,
							    struct bim_UInt16ByteImage* ptrA, 
							    const struct bim_UInt16ByteImage* srcPtrA, 
							    const struct bts_Flt16Alt2D* altPtrA,
							    int32 resultWidthA,
							    int32 resultHeightA )
{
	long srcWidthL = srcPtrA->widthE;
	long srcHeightL = srcPtrA->heightE;
	
	struct bts_Flt16Alt2D invAlt2DL;
	
	uint8* dstPtrL;
	const uint8* ulPtrL = ( const uint8* )srcPtrA->arrE.arrPtrE;
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

	bim_UInt16ByteImage_size( cpA, ptrA, resultWidthA, resultHeightA );
	dstPtrL = ( uint8* )ptrA->arrE.arrPtrE;
	
	/* compute inverse */
	invAlt2DL = bts_Flt16Alt2D_inverted( altPtrA );
	
	if( srcWidthL == 0 || srcHeightL == 0 )
	{
		bbs_ERROR2( "Size of output image is %d/%d", srcWidthL, srcHeightL );
		return;
	}

	/* align Matrix and Vector to 8 bits bbp */
	shiftL = invAlt2DL.matE.bbpE - bbpL;
	if( shiftL >= 0 )
	{
		mxxL = ( int32 )invAlt2DL.matE.xxE >> shiftL;
		mxyL = ( int32 )invAlt2DL.matE.xyE >> shiftL;
		myxL = ( int32 )invAlt2DL.matE.yxE >> shiftL;
		myyL = ( int32 )invAlt2DL.matE.yyE >> shiftL;
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
						( int32 )invAlt2DL.matE.xxE >> invAlt2DL.matE.bbpE,
						( int32 )invAlt2DL.matE.xyE >> invAlt2DL.matE.bbpE,
						( int32 )invAlt2DL.matE.yxE >> invAlt2DL.matE.bbpE,
						( int32 )invAlt2DL.matE.yyE >> invAlt2DL.matE.bbpE,
						maxInt32Value8bbpL >> ( bbpL - ( -shiftL ) ) );
			return;
		}

		mxxL = ( int32 )invAlt2DL.matE.xxE << -shiftL;
		mxyL = ( int32 )invAlt2DL.matE.xyE << -shiftL;
		myxL = ( int32 )invAlt2DL.matE.yxE << -shiftL;
		myyL = ( int32 )invAlt2DL.matE.yyE << -shiftL;
		maxInt32Value8bbpL <<= -shiftL;
	}
	invAlt2DL.matE.bbpE = bbpL;

	shiftL = invAlt2DL.vecE.bbpE - bbpL;
	if( shiftL >= 0 )
	{
		txL  = ( int32 )invAlt2DL.vecE.xE >> shiftL;
		tyL  = ( int32 )invAlt2DL.vecE.yE >> shiftL;
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
		txL  = ( int32 )invAlt2DL.vecE.xE << -shiftL;
		tyL  = ( int32 )invAlt2DL.vecE.yE << -shiftL;
		maxInt32Value8bbpL <<= -shiftL;
	}
	invAlt2DL.vecE.bbpE = bbpL;

	/* For each destination pixel find the correspoding source pixel by applying the inverse transformation */
	for( jL = 0; jL < ptrA->heightE; jL++ )
	{
		xL = txL + mxyL * jL;
		yL = tyL + myyL * jL;
		for( iL = 0; iL < ptrA->widthE; iL++ )
		{
			const uint16 bbpLby2L = bbpL / 2;
			const int32 oneL = ( int32 )0x00000001 << bbpLby2L;
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
					const int32 halfL = ( int32 )0x00000001 << ( bbpL - 1 );
	
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

#endif

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


