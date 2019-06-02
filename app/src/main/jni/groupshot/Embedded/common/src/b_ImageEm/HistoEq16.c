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
#include "b_BasicEm/Int16Arr.h"
#include "b_BasicEm/Math.h"
#include "b_ImageEm/HistoEq16.h"
#include "b_ImageEm/UInt16ByteImage.h"

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/** Computes grey level histogram of given image. */
void bim_createHisto16( uint16* histoPtrA, 
						const struct bim_UInt16ByteImage* imagePtrA )
{
	uint32 iL;
	uint16* dstPtrL;
	const uint16* srcPtrL;
	
	/* init histogram array with 0 */
	dstPtrL = histoPtrA;
	for( iL = 256; iL > 0; iL-- )
	{
		*dstPtrL++ = 0;
	}
	
	srcPtrL = imagePtrA->arrE.arrPtrE;
	dstPtrL = histoPtrA;
	/* calculate histogram (assuming even image width) */
	for( iL = imagePtrA->arrE.sizeE; iL > 0; iL-- ) 
	{
		dstPtrL[ ( *srcPtrL & 0x0FF ) ]++;	
		dstPtrL[ ( *srcPtrL >> 8 ) ]++;
		srcPtrL++;
	}
}

/* ------------------------------------------------------------------------- */

/** Computes grey level histogram of given image. */
void bim_createHistoOfSection16( uint16* histoPtrA,
								 const struct bts_Int16Rect* sectionPtrA, 
								 const struct bim_UInt16ByteImage* imagePtrA )
{
	uint32 xL, yL;
	const uint16* srcPtrL;
	uint16* dstPtrL;
	struct bts_Int16Rect sectionL = *sectionPtrA;
	uint32 sectWidthL;
	uint32 sectHeightL;
	int32 imgWidthL = imagePtrA->widthE;
	int32 imgHeightL = imagePtrA->heightE;

	bbs_ERROR0( "bim_createHistoOfSection16(...): not implemented" );

	/* adjustments */
	sectionL.x1E = bbs_max( 0, sectionL.x1E );
	sectionL.x1E = bbs_min( imgWidthL, sectionL.x1E );
	sectionL.x2E = bbs_max( 0, sectionL.x2E );
	sectionL.x2E = bbs_min( imgWidthL, sectionL.x2E );
	sectionL.y1E = bbs_max( 0, sectionL.y1E );
	sectionL.y1E = bbs_min( imgHeightL, sectionL.y1E );
	sectionL.y2E = bbs_max( 0, sectionL.y2E );
	sectionL.y2E = bbs_min( imgHeightL, sectionL.y2E );

	sectWidthL = sectionL.x2E - sectionL.x1E;
	sectHeightL = sectionL.y2E - sectionL.y1E;

	/* init histogram with 0 */
	dstPtrL = histoPtrA;
	for( xL = 256; xL > 0; xL-- )
	{
		*dstPtrL++ = 0;
	}
	
	/* calculate histogram */
	srcPtrL = imagePtrA->arrE.arrPtrE + sectionL.y1E * imgWidthL + sectionL.x1E;
	dstPtrL = histoPtrA;
	for( yL = 0; yL < sectHeightL; yL++ )
	{
		for( xL = 0; xL < sectWidthL; xL++ )
		{
			dstPtrL[ ( *srcPtrL & 0x0FF ) ]++;	
			dstPtrL[ ( *srcPtrL >> 8 ) ]++;
			srcPtrL++;
			/* dstPtrL[ *srcPtrL++ ]++;	 */
		}
		srcPtrL += imgWidthL - sectWidthL;
	}
}

/* ------------------------------------------------------------------------- */

/** equalize image using given histogram */
void bim_equalize16( struct bim_UInt16ByteImage* imagePtrA, 
					 const uint16* histoPtrA )
{
	uint32 kL;
	uint32 sumL = 0;
	uint32 totalSumL = 0;
	const uint16* histoArrPtrL;
	uint16* dstPtrL;
	uint16 mappingL[ 256 ];

	/* determine number of counts in histogram */
	histoArrPtrL = histoPtrA;
	for( kL = 256; kL > 0; kL-- )
	{
		totalSumL += *histoArrPtrL++;
	}

	if( totalSumL == 0 ) totalSumL = 1;
	
	/* compute transfer function (cumulative histogram) */
	histoArrPtrL = histoPtrA;
	for( kL = 0; kL < 256; kL++ )
	{
		sumL += *histoArrPtrL++;
		mappingL[ kL ] = ( sumL * 255 ) / totalSumL;
	}

	/* remap pixel values */
	dstPtrL = imagePtrA->arrE.arrPtrE;
	for( kL = imagePtrA->arrE.sizeE; kL > 0; kL-- )
	{
		*dstPtrL = mappingL[ *dstPtrL & 0x00FF ] | ( mappingL[ *dstPtrL >> 8 ] << 8 );
		dstPtrL++;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ external functions } ----------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bim_UInt16ByteImage_equalize( struct bim_UInt16ByteImage* imagePtrA )
{
	uint16 histogramL[ 256 ];
	bim_createHisto16( histogramL, imagePtrA );
	bim_equalize16( imagePtrA, histogramL );
}

/* ------------------------------------------------------------------------- */

void bim_UInt16ByteImage_equalizeSection( struct bim_UInt16ByteImage* imagePtrA,
										  const struct bts_Int16Rect* sectionPtrA )
{
	uint16 histogramL[ 256 ];
	bim_createHistoOfSection16( histogramL, sectionPtrA, imagePtrA );
	bim_equalize16( imagePtrA, histogramL );
}

/* ========================================================================= */
