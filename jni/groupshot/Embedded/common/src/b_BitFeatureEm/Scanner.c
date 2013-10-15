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
#include "b_BasicEm/Math.h"
#include "b_BitFeatureEm/Scanner.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/** multiplies a scale with a 0.32 scale factor */
uint32 bbf_Scanner_scalePrd( uint32 scaleA, uint32 factorA /*0.32 */ )\
{
	return      ( scaleA >> 16     ) * ( factorA >> 16     ) + 
			( ( ( scaleA & 0x0FFFF ) * ( factorA >> 16     ) ) >> 16 ) +
			( ( ( scaleA >> 16     ) * ( factorA & 0x0FFFF ) ) >> 16 );
}

/* ------------------------------------------------------------------------- */

/** allocates arays */
void bbf_Scanner_alloc( struct bbs_Context* cpA,
						struct bbf_Scanner* ptrA, 
						struct bbs_MemTbl* mtpA,
						flag maximizeSharedMemoryA )
{
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_segPtr( cpA, &memTblL, 0 );
	struct bbs_MemSeg* sspL = bbs_MemTbl_sharedSegPtr( cpA, &memTblL, 0 );
	struct bbs_MemSeg* mspL = maximizeSharedMemoryA ? sspL : espL;

	/* filter patch dimension */
	uint32 proL = ptrA->maxRadiusE;
	uint32 pwoL = ( proL << 1 ) + 1;

	/* output image size (bit image) */
	uint32 woL = ptrA->maxImageWidthE;
	uint32 hoL = ptrA->maxImageHeightE;

	/* extended output image size (bit image) considering borders */
	uint32 xwoL = woL + ( ptrA->borderWidthE  << 1 );
	uint32 xhoL = hoL + ( ptrA->borderHeightE << 1 );

	/* allocate working image */
	bbs_UInt16Arr_create( cpA, &ptrA->workImageE, ( ( woL >> 1 ) + ( woL & 1 ) ) * hoL, mspL );
	if( bbs_Context_error( cpA ) ) return;
	bbs_UInt16Arr_fill( cpA, &ptrA->workImageE, 0 );

	/* allocate bit image */
	bim_UInt32Image_create( cpA, &ptrA->bitImageE, xwoL, ( xhoL >> 5 ) + ( ( ( xhoL & 0x1F ) != 0 ) ? 1 : 0 ), mspL );
	if( bbs_Context_error( cpA ) ) return;
	bim_UInt32Image_setAllPixels( cpA, &ptrA->bitImageE, 0, 0 );

	/* allocate patch buffer */
	bbs_UInt32Arr_create( cpA, &ptrA->patchBufferE, ptrA->bitImageE.widthE, mspL );
	if( bbs_Context_error( cpA ) ) return;
	bbs_UInt32Arr_fill( cpA, &ptrA->patchBufferE, 0 );

	/* allocate line buffer */
	bbs_UInt16Arr_create( cpA, &ptrA->lineBufE, woL + ( woL & 1 ), sspL );

	/* allocate table */
	bim_UInt32Image_create( cpA, &ptrA->satE, woL + pwoL, pwoL + 1, sspL );

	/* allocate buffers */
	bbs_UInt32Arr_create( cpA, &ptrA->idxArrE, ptrA->bufferSizeE, mspL );
	bbs_Int32Arr_create(  cpA, &ptrA->actArrE, ptrA->bufferSizeE, mspL );

	bbs_Int32Arr_create(  cpA, &ptrA->outArrE, ptrA->bufferSizeE >> 1, espL );
}

/* ------------------------------------------------------------------------- */

/** downscales work image */
void bbf_Scanner_downscale( struct bbs_Context* cpA, struct bbf_Scanner* ptrA )
{
	uint32 w0L = ptrA->workWidthE;
	uint32 h0L = ptrA->workHeightE;
	uint32 w1L = w0L >> 1;
	uint32 h1L = h0L >> 1;
	uint32 w20L = ( w0L >> 1 ) + ( w0L & 1 );
	uint16* arrL = ptrA->workImageE.arrPtrE;

	uint32 iL, jL;
	uint32 kL = 0;
	for( jL = 0; jL < h1L; jL++ )
	{
		for( iL = 0; iL < ( w1L >> 1 ); iL++ )
		{
			uint16 loL, hiL;
			uint32 idxL = jL * 2 * w20L + iL * 2;

			loL = ( ( arrL[ idxL ] & 0x00FF ) + ( arrL[ idxL ] >> 8 ) + ( arrL[ idxL + w20L ] & 0x00FF ) + ( arrL[ idxL + w20L ] >> 8 ) + 2 ) >> 2;
			idxL++;
			hiL = ( ( arrL[ idxL ] & 0x00FF ) + ( arrL[ idxL ] >> 8 ) + ( arrL[ idxL + w20L ] & 0x00FF ) + ( arrL[ idxL + w20L ] >> 8 ) + 2 ) >> 2;

			arrL[ kL ] = loL | ( hiL << 8 );
			kL++;
		}
		if( ( w1L & 1 ) != 0 )
		{
			uint32 idxL = jL * 2 * w20L + iL;
			arrL[ kL++ ] = ( ( arrL[ idxL ] & 0x00FF ) + ( arrL[ idxL ] >> 8 ) + ( arrL[ idxL + w20L ] & 0x00FF ) + ( arrL[ idxL + w20L ] >> 8 ) + 2 ) >> 2;
		}
	}

	ptrA->workWidthE = w1L;
	ptrA->workHeightE = h1L;
	ptrA->scaleExpE++;
}

/* ------------------------------------------------------------------------- */

/** copies image
 * handling for 8 bit images is implemented 
 * 16 bit image handling for the whole class needs to be added in this function only
 */
void bbf_Scanner_copyImage( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, const void* imagePtrA, uint32 imageWidthA, uint32 imageHeightA, const struct bts_Int16Rect* roiPtrA )
{
	bbs_DEF_fNameL( "void bbf_Scanner_copyImage( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, const struct bim_UInt16ByteImage* imagePtrA, const struct bts_Int16Rect* roiPtrA )" )

	if( imageWidthA > ptrA->maxImageWidthE || imageHeightA > ptrA->maxImageHeightE ) 
	{
		bbs_ERROR5( "%s:\n Input image (%ix%i)is too large; Scanner is configured for maximal (%ix%i)", 
			fNameL, imageWidthA, imageHeightA, ptrA->maxImageWidthE, ptrA->maxImageHeightE );
		return;
	}

	if( roiPtrA == 0 )
	{
		uint32 iL, jL;
		const uint8*  srcL = ( uint8* )imagePtrA;
		uint16* dstL = ptrA->workImageE.arrPtrE;
		ptrA->workWidthE  = imageWidthA;
		ptrA->workHeightE = imageHeightA;
		for( iL = 0; iL < ptrA->workHeightE; iL++ )
		{
			for( jL = ptrA->workWidthE >> 1; jL > 0; jL-- )
			{
				*dstL++ = ( uint16 )srcL[ 0 ] | ( uint16 )srcL[ 1 ] << 8;
				srcL += 2;
			}

			/* uneven width */
			if( ptrA->workWidthE & 1 ) *dstL++ = *srcL++;
		}
	}
	else
	{
		uint32 iL, jL;
		const uint8* srcL = ( uint8* )imagePtrA + roiPtrA->y1E * imageWidthA + roiPtrA->x1E;
		uint16* dstL = ptrA->workImageE.arrPtrE;

		if( roiPtrA->x2E <= roiPtrA->x1E || roiPtrA->y2E <= roiPtrA->y1E )
		{
			bbs_ERROR1( "%s:\n ROI is invalid or zero", fNameL );
			return;
		}
		if( roiPtrA->x1E < 0 || roiPtrA->y1E < 0 || roiPtrA->x2E > ( int32 )imageWidthA || roiPtrA->y2E > ( int32 )imageHeightA )
		{
			bbs_ERROR1( "%s:\n ROI exceeds image boundary", fNameL );
			return;
		}

		ptrA->workWidthE  = roiPtrA->x2E - roiPtrA->x1E;
		ptrA->workHeightE = roiPtrA->y2E - roiPtrA->y1E;
		for( iL = 0; iL < ptrA->workHeightE; iL++ )
		{
			for( jL = ptrA->workWidthE >> 1; jL > 0; jL-- )
			{
				*dstL++ = ( uint16 )srcL[ 0 ] | ( uint16 )srcL[ 1 ] << 8;
				srcL += 2;
			}

			/* uneven width */
			if( ptrA->workWidthE & 1 ) *dstL++ = *srcL++;

			srcL += imageWidthA - ptrA->workWidthE;
		}
	}
}

/* ------------------------------------------------------------------------- */

/** creates bit image */
void bbf_Scanner_createBitImage( struct bbs_Context* cpA, struct bbf_Scanner* ptrA )
{
	bbs_DEF_fNameL( "void bbf_Scanner_createBitImage( struct bbs_Context* cpA, struct bbf_Scanner* ptrA )" )


	/* declarations */
	uint32 proL, priL, pwoL, pwiL;
	uint32 wiL, wi2L, hiL, woL, hoL, xwoL, xhoL; /* image size specifies */
	uint32 stepL;    /* scan step (16.16) */
	uint32 bitMaskL; /* current bit mask */
	uint32* bitRowL; /* pointer to bit-row */
	uint32 wsL, hsL; /* size of summed area table (ringbuffer) */
	uint32 satSizeL; 
	uint32* satL;     /* pointer to summed area table */
	uint16* lBufL;	  /* pointer to line buffer */
	uint32 yfL;       /* fixed point y-coordinate (16.16) */
	uint32 iL, jL;

	uint32 swi1L; /* table writing index */
	uint32 swi2L; /* table writing index */
	uint32 sriL;  /* table reading index */

	uint32 poAreaL, piAreaL; /* areas of inner and outer rectangles */
	uint32 siL[ 8 ]; /* table indices */


	proL = ptrA->bitParamE.outerRadiusE;
	priL = ptrA->bitParamE.innerRadiusE;
	pwoL = ( proL << 1 ) + 1;
	pwiL = ( priL << 1 ) + 1;

	if( ptrA->borderHeightE >= 32 )
	{
		bbs_ERROR1( "%s:\n borderHeightE >= 32", fNameL );
		return;
	}

	if( proL <= priL )
	{
		bbs_ERROR1( "%s:\n outer radius <= inner radius", fNameL );
		return;
	}

	/* input image size (bit image) */
	wiL = ptrA->workWidthE;
	hiL = ptrA->workHeightE;
	wi2L = ( wiL >> 1 ) + ( wiL & 1 );

	/* 16.16 */
	stepL = ptrA->scaleE >> ( ptrA->scaleExpE + 4 );

	/* output image size (bit image) */
	woL = ( wiL << 16 ) / stepL;
	hoL = ( hiL << 16 ) / stepL;

	if( woL <= pwoL || hoL <= pwoL )
	{
		bbs_ERROR1( "%s:\n scaled image is too small", fNameL );
		return;
	}

	if( woL * stepL >= ( wiL << 16 ) ) woL--;
	if( hoL * stepL >= ( hiL << 16 ) ) hoL--;

	/* extended output image size (bit image) considering borders */
	xwoL = woL + ( ptrA->borderWidthE  << 1 );
	xhoL = hoL + ( ptrA->borderHeightE << 1 );

	ptrA->currentWidthE  = xwoL;
	ptrA->currentHeightE = xhoL;

	/* initialize bit image */
	bim_UInt32Image_size( cpA, &ptrA->bitImageE, xwoL, ( xhoL >> 5 ) + ( ( ( xhoL & 0x1F ) != 0 ) ? 1 : 0 ) );
	bim_UInt32Image_setAllPixels( cpA, &ptrA->bitImageE, 0, 0 );

	bitMaskL = ( uint32 )1 << ptrA->borderHeightE;
	bitRowL = ( uint32* )ptrA->bitImageE.arrE.arrPtrE + ptrA->borderWidthE;

	/* width of table */
	wsL = woL + pwoL;

	/* height of table */
	hsL = pwoL + 1;

	bim_UInt32Image_size( cpA, &ptrA->satE, wsL, hsL );

	satL = ptrA->satE.arrE.arrPtrE;
	satSizeL = wsL * hsL;

	lBufL = ptrA->lineBufE.arrPtrE;

	yfL = 0; /* fixed point y-coordinate ( 16.16 )*/

	swi1L = 0; /* table writing index */
	swi2L = 0; /* table writing index */
	sriL = 0;  /* table reading index */

	/* areas of inner and outer rectangles */
	poAreaL = pwoL * pwoL;
	piAreaL = pwiL * pwiL;

	/* interpolate pixels; compute table and bit image */

	for( iL = wsL * ( proL + 1 ); iL > 0; iL-- ) satL[ swi1L++ ] = 0;
	swi2L = swi1L - wsL;

	for( jL = 0; jL < hoL + proL; jL++ )
	{
		if( jL < hoL ) /* rescale area */
		{
			uint32 ypL = ( yfL >> 16 );
			uint32 yoff1L = yfL & 0x0FFFF;
			uint32 yoff0L = 0x010000 - yoff1L;
			const uint16* arr0L = ptrA->workImageE.arrPtrE + ypL * wi2L;
			const uint16* arr1L = arr0L + wi2L;

			
			uint32 xfL   = 0; /* fixed point x-coordinate (16.16) */
			uint32 hSumL = 0;

			yfL += stepL;

			for( iL = 0; iL <= proL; iL++ ) satL[ swi1L++ ] = 0;
			swi2L += iL;

			/* fill line buffer */
			for( iL = 0; iL < wi2L; iL++ )
			{
				lBufL[ iL * 2     ] = ( ( ( arr0L[ iL ] & 0x0FF ) * yoff0L ) + ( ( arr1L[ iL ] & 0x0FF ) * yoff1L ) ) >> 10;
				lBufL[ iL * 2 + 1 ] = ( ( ( arr0L[ iL ] >> 8    ) * yoff0L ) + ( ( arr1L[ iL ] >> 8    ) * yoff1L ) ) >> 10;
			}

			for( iL = 0; iL < woL; iL++ )
			{
				uint32 xpL = ( xfL >> 16 );
				uint32 xoff1L = xfL & 0x0FFFF;
				uint16 pixL = ( lBufL[ xpL ] * ( 0x010000 - xoff1L ) + lBufL[ xpL + 1 ] * xoff1L ) >> 22;
				satL[ swi1L ] = ( hSumL += pixL ) + satL[ swi2L ];
				xfL += stepL;
				swi1L++;
				swi2L++;
			}

			for( iL = 0; iL < proL; iL++ )
			{
				satL[ swi1L ] = hSumL + satL[ swi2L ];
				swi1L++;
				swi2L++;
			}
		}
		else /* image is processed - fill in 0s */
		{
			for( iL = 0; iL < wsL; iL++ ) satL[ swi1L++ ] = satL[ swi2L++ ];
		}

		swi1L = ( swi1L < satSizeL ) ? swi1L : 0;
		swi2L = ( swi2L < satSizeL ) ? swi2L : 0;

		/* fill line in bit image */
		if( jL >= proL ) 
		{
			const uint32* rSatL = satL;

			/* table coordinate indices for outer rectangle */
			siL[ 0 ] = sriL;
			siL[ 1 ] = siL[ 0 ] + pwoL;
			siL[ 2 ] = siL[ 0 ] + pwoL * wsL;
			siL[ 2 ] -= ( siL[ 2 ] >= satSizeL ) ? satSizeL : 0;
			siL[ 3 ] = siL[ 2 ] + pwoL;

			/* table coordinate indices for inner rectangle */
			siL[ 4 ] = siL[ 0 ] + ( proL - priL ) * wsL + ( proL - priL );
			siL[ 4 ] -= ( siL[ 4 ] >= satSizeL ) ? satSizeL : 0;
			siL[ 5 ] = siL[ 4 ] + pwiL;
			siL[ 6 ] = siL[ 4 ] + pwiL * wsL;
			siL[ 6 ] -= ( siL[ 6 ] >= satSizeL ) ? satSizeL : 0;
			siL[ 7 ] = siL[ 6 ] + pwiL;
			sriL += wsL;
			if( sriL == satSizeL ) sriL = 0;

			for( iL = 0; iL < woL; iL++ )
			{
				uint32 oAvgL = ( rSatL[ siL[ 0 ] ] - rSatL[ siL[ 1 ] ] - rSatL[ siL[ 2 ] ] + rSatL[ siL[ 3 ] ] ) * piAreaL;
				uint32 iAvgL = ( rSatL[ siL[ 4 ] ] - rSatL[ siL[ 5 ] ] - rSatL[ siL[ 6 ] ] + rSatL[ siL[ 7 ] ] ) * poAreaL;
				bitRowL[ iL ] |= ( iAvgL > oAvgL ) ? bitMaskL : 0;
				rSatL++;
			}
			if( ( bitMaskL <<= 1 ) == 0 )
			{
				bitRowL += xwoL;
				bitMaskL = 1;
			}
		}
	}
}

/* ------------------------------------------------------------------------- */

/** initialize patch buffer */
void bbf_Scanner_initPatchBuffer( struct bbs_Context* cpA, struct bbf_Scanner* ptrA )
{
	bbs_UInt32Arr_size( cpA, &ptrA->patchBufferE, ptrA->bitImageE.widthE );	
	bbs_memcpy32( ptrA->patchBufferE.arrPtrE, ptrA->bitImageE.arrE.arrPtrE, ptrA->bitImageE.widthE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_Scanner_init( struct bbs_Context* cpA,
					   struct bbf_Scanner* ptrA )
{
	ptrA->scaleExpE = 0;
	ptrA->scaleE = 0;
	ptrA->xE = 0;
	ptrA->yE = 0;
	ptrA->effMaxScaleE = 0;
	ptrA->currentWidthE = 0;
	ptrA->currentHeightE = 0;
	ptrA->workWidthE = 0;
	ptrA->workHeightE = 0;
	bbf_BitParam_init( cpA, &ptrA->bitParamE );
	bbs_UInt16Arr_init( cpA, &ptrA->workImageE );
	bim_UInt32Image_init( cpA, &ptrA->satE );
	bim_UInt32Image_init( cpA, &ptrA->bitImageE );
	bbs_UInt32Arr_init( cpA, &ptrA->patchBufferE );
	bbs_UInt16Arr_init( cpA, &ptrA->lineBufE );

	bbs_UInt32Arr_init( cpA, &ptrA->idxArrE );
	bbs_Int32Arr_init( cpA, &ptrA->actArrE );
	bbs_Int32Arr_init( cpA, &ptrA->outArrE );
	ptrA->outCountE = 0;
	ptrA->intCountE = 0;
	ptrA->bufferSizeE = 1024;

	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
	ptrA->maxRadiusE = 0;
	ptrA->patchWidthE = 0;
	ptrA->patchHeightE = 0;
	ptrA->minScaleE = 0;
	ptrA->maxScaleE = 0;
	ptrA->scaleStepE = 0;
	ptrA->borderWidthE = 0;
	ptrA->borderHeightE = 0;
}

/* ------------------------------------------------------------------------- */

void bbf_Scanner_exit( struct bbs_Context* cpA,
				       struct bbf_Scanner* ptrA )
{
	ptrA->scaleExpE = 0;
	ptrA->scaleE = 0;
	ptrA->xE = 0;
	ptrA->yE = 0;
	ptrA->effMaxScaleE = 0;
	ptrA->currentWidthE = 0;
	ptrA->currentHeightE = 0;
	ptrA->workWidthE = 0;
	ptrA->workHeightE = 0;
	bbf_BitParam_exit( cpA, &ptrA->bitParamE );
	bbs_UInt16Arr_exit( cpA, &ptrA->workImageE );
	bim_UInt32Image_exit( cpA, &ptrA->satE );
	bim_UInt32Image_exit( cpA, &ptrA->bitImageE );
	bbs_UInt32Arr_exit( cpA, &ptrA->patchBufferE );
	bbs_UInt16Arr_exit( cpA, &ptrA->lineBufE );

	bbs_UInt32Arr_exit( cpA, &ptrA->idxArrE );
	bbs_Int32Arr_exit( cpA, &ptrA->actArrE );
	bbs_Int32Arr_exit( cpA, &ptrA->outArrE );
	ptrA->outCountE = 0;
	ptrA->intCountE = 0;
	ptrA->bufferSizeE = 1024;

	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
	ptrA->maxRadiusE = 0;
	ptrA->patchWidthE = 0;
	ptrA->patchHeightE = 0;
	ptrA->minScaleE = 0;
	ptrA->maxScaleE = 0;
	ptrA->scaleStepE = 0;
	ptrA->borderWidthE = 0;
	ptrA->borderHeightE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_Scanner_copy( struct bbs_Context* cpA,
				       struct bbf_Scanner* ptrA, 
					   const struct bbf_Scanner* srcPtrA )
{
	ptrA->scaleExpE = srcPtrA->scaleExpE;
	ptrA->scaleE = srcPtrA->scaleE;
	ptrA->xE = srcPtrA->xE;
	ptrA->yE = srcPtrA->yE;
	ptrA->effMaxScaleE = srcPtrA->effMaxScaleE;
	ptrA->currentWidthE = srcPtrA->currentWidthE;
	ptrA->currentHeightE = srcPtrA->currentHeightE;
	ptrA->workWidthE = srcPtrA->workWidthE;
	ptrA->workHeightE = srcPtrA->workHeightE;

	bbf_BitParam_copy( cpA, &ptrA->bitParamE, &srcPtrA->bitParamE );
	bbs_UInt16Arr_copy( cpA, &ptrA->workImageE, &srcPtrA->workImageE );
	bim_UInt32Image_copy( cpA, &ptrA->satE, &srcPtrA->satE );
	bim_UInt32Image_copy( cpA, &ptrA->bitImageE, &srcPtrA->bitImageE );
	bbs_UInt32Arr_copy( cpA, &ptrA->patchBufferE, &srcPtrA->patchBufferE );
	bbs_UInt16Arr_copy( cpA, &ptrA->lineBufE, &srcPtrA->lineBufE );

	ptrA->maxImageWidthE = srcPtrA->maxImageWidthE;
	ptrA->maxImageHeightE = srcPtrA->maxImageHeightE;
	ptrA->maxRadiusE = srcPtrA->maxRadiusE;
	ptrA->patchWidthE = srcPtrA->patchWidthE;
	ptrA->patchHeightE = srcPtrA->patchHeightE;
	ptrA->minScaleE = srcPtrA->minScaleE;
	ptrA->maxScaleE = srcPtrA->maxScaleE;
	ptrA->scaleStepE = srcPtrA->scaleStepE;
	ptrA->borderWidthE = srcPtrA->borderWidthE;
	ptrA->borderHeightE = srcPtrA->borderHeightE;
}

/* ------------------------------------------------------------------------- */

flag bbf_Scanner_equal( struct bbs_Context* cpA,
				        const struct bbf_Scanner* ptrA, 
						const struct bbf_Scanner* srcPtrA )
{
	if( ptrA->maxImageWidthE != srcPtrA->maxImageWidthE ) return FALSE;
	if( ptrA->maxImageHeightE != srcPtrA->maxImageHeightE ) return FALSE;
	if( ptrA->maxRadiusE != srcPtrA->maxRadiusE ) return FALSE;
	if( ptrA->patchWidthE != srcPtrA->patchWidthE ) return FALSE;
	if( ptrA->patchHeightE != srcPtrA->patchHeightE ) return FALSE;
	if( ptrA->minScaleE != srcPtrA->minScaleE ) return FALSE;
	if( ptrA->maxScaleE != srcPtrA->maxScaleE ) return FALSE;
	if( ptrA->scaleStepE != srcPtrA->scaleStepE ) return FALSE;
	if( ptrA->borderWidthE != srcPtrA->borderWidthE ) return FALSE;
	if( ptrA->borderHeightE != srcPtrA->borderHeightE ) return FALSE;
	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bbf_Scanner_positions( const struct bbf_Scanner* ptrA )
{
	int32 wL = ( int32 )ptrA->currentWidthE - ptrA->patchWidthE;
	int32 hL = ( int32 )ptrA->currentHeightE - ptrA->patchHeightE;
	return ( wL >= 0 ? wL : 0 ) * ( hL >= 0 ? hL : 0 );
}

/* ------------------------------------------------------------------------- */

uint32 bbf_Scanner_scanIndex( const struct bbf_Scanner* ptrA )
{
	return ptrA->yE * ptrA->currentWidthE + ptrA->xE;
}

/* ------------------------------------------------------------------------- */

void bbf_Scanner_pos( const struct bbf_Scanner* ptrA, 
					  int32* xPtrA, int32* yPtrA, uint32* scalePtrA )
{
	/* 16.16 */
	*xPtrA = ( int32 )( ptrA->xE - ptrA->borderWidthE ) * ( int32 )( ptrA->scaleE >> 4 );

	/* 16.16 */
	*yPtrA = ( int32 )( ptrA->yE - ptrA->borderHeightE ) * ( int32 )( ptrA->scaleE >> 4 );

	/* 12.20 */
	*scalePtrA = ptrA->scaleE;
}

/* ------------------------------------------------------------------------- */

void bbf_Scanner_idxPos( const struct bbf_Scanner* ptrA, uint32 scanIndexA,
					     int32* xPtrA, int32* yPtrA, uint32* scalePtrA )
{
	int32 yL = scanIndexA / ptrA->currentWidthE;
	int32 xL = scanIndexA - ( yL * ptrA->currentWidthE );

	/* 16.16 */
	*xPtrA = ( int32 )( xL - ptrA->borderWidthE  ) * ( int32 )( ptrA->scaleE >> 4 );

	/* 16.16 */
	*yPtrA = ( int32 )( yL - ptrA->borderHeightE ) * ( int32 )( ptrA->scaleE >> 4 );

	*scalePtrA = ptrA->scaleE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bbf_Scanner_create( struct bbs_Context* cpA,
						 struct bbf_Scanner* ptrA, 
						 flag maximizeSharedMemoryA,
						 uint32 maxImageWidthA,
					 	 uint32 maxImageHeightA,
						 uint32 maxRadiusA,
						 uint32 patchWidthA,
						 uint32 patchHeightA,
						 uint32 minScaleA,
						 uint32 maxScaleA,
						 uint32 scaleStepA,
						 uint32 borderWidthA,
						 uint32 borderHeightA,
						 uint32 bufferSizeA,
						 struct bbs_MemTbl* mtpA )
{
	ptrA->maxImageWidthE = maxImageWidthA;
	ptrA->maxImageHeightE = maxImageHeightA;
	ptrA->maxRadiusE = maxRadiusA;
	ptrA->patchWidthE = patchWidthA;
	ptrA->patchHeightE = patchHeightA;
	ptrA->minScaleE = minScaleA;
	ptrA->maxScaleE = maxScaleA;
	ptrA->scaleStepE = scaleStepA;
	ptrA->borderWidthE = borderWidthA;
	ptrA->borderHeightE = borderHeightA;
	ptrA->bufferSizeE = bufferSizeA;
	bbf_Scanner_alloc( cpA, ptrA, mtpA, maximizeSharedMemoryA );
}

/* ------------------------------------------------------------------------- */
	
void bbf_Scanner_bitParam( struct bbs_Context* cpA,
						   struct bbf_Scanner* ptrA,
						   const struct bbf_BitParam* bitParamPtrA )
{
	if( !bbf_BitParam_equal( cpA, &ptrA->bitParamE, bitParamPtrA ) )
	{
		bbf_BitParam_copy( cpA, &ptrA->bitParamE, bitParamPtrA );
		bbf_Scanner_createBitImage( cpA, ptrA );
	}

	bbf_Scanner_resetScan( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bbf_Scanner_memSize( struct bbs_Context* cpA,
							const struct bbf_Scanner* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */
	memSizeL += bbs_SIZEOF16( ptrA->maxImageWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->maxImageHeightE );
	memSizeL += bbs_SIZEOF16( ptrA->maxRadiusE );
	memSizeL += bbs_SIZEOF16( ptrA->patchWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->patchHeightE );
	memSizeL += bbs_SIZEOF16( ptrA->minScaleE );
	memSizeL += bbs_SIZEOF16( ptrA->maxScaleE );
	memSizeL += bbs_SIZEOF16( ptrA->scaleStepE );
	memSizeL += bbs_SIZEOF16( ptrA->borderWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->borderHeightE );
	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_Scanner_memWrite( struct bbs_Context* cpA,
						     const struct bbf_Scanner* ptrA, 
						     uint16* memPtrA )
{
	uint32 memSizeL = bbf_Scanner_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bbf_SCANNER_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxImageWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxImageHeightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxRadiusE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->patchWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->patchHeightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->minScaleE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxScaleE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->scaleStepE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->borderWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->borderHeightE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bbf_Scanner_memRead( struct bbs_Context* cpA,
						    struct bbf_Scanner* ptrA, 
						    const uint16* memPtrA, 
						    struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;

	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bbf_SCANNER_VERSION, memPtrA );

	memPtrA += bbs_memRead32( &ptrA->maxImageWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxImageHeightE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxRadiusE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->patchWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->patchHeightE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->minScaleE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxScaleE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->scaleStepE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->borderWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->borderHeightE, memPtrA );

	if( memSizeL != bbf_Scanner_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_Scanner_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
			        "size mismatch" );
		return 0;
	}

	if( bbs_Context_error( cpA ) ) return 0;

	/* allocate arrays */
	bbf_Scanner_alloc( cpA, ptrA, mtpA, FALSE );

	if( bbs_Context_error( cpA ) ) return 0;

	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

void bbf_Scanner_resetScan( struct bbs_Context* cpA, struct bbf_Scanner* ptrA )
{
	ptrA->xE = 0;
	ptrA->yE = 0;
	bbf_Scanner_initPatchBuffer( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */

void bbf_Scanner_assign( struct bbs_Context* cpA, struct bbf_Scanner* ptrA,
					     const void* imagePtrA,
						 uint32 imageWidthA,
						 uint32 imageHeightA,
						 const struct bts_Int16Rect* roiPtrA,
						 const struct bbf_BitParam* paramPtrA )
{
	/* copy image */
	bbf_Scanner_copyImage( cpA, ptrA, imagePtrA, imageWidthA, imageHeightA, roiPtrA );

	ptrA->scaleE = ptrA->minScaleE;
	bbf_BitParam_copy( cpA, &ptrA->bitParamE, paramPtrA );

	/* compute effective max scale */
	{
		/* 16.16 */
		uint32 maxHScaleL = ( ptrA->workWidthE << 16 ) / ( ptrA->patchWidthE + 1 );
		uint32 maxVScaleL = ( ptrA->workHeightE << 16 ) / ( ptrA->patchHeightE + 1 );

		/* 12.20 */
		ptrA->effMaxScaleE = maxHScaleL < maxVScaleL ? ( maxHScaleL << 4 ) : ( maxVScaleL << 4 );
		
		if( ptrA->maxScaleE > 0 ) ptrA->effMaxScaleE = ptrA->effMaxScaleE < ptrA->maxScaleE ? ptrA->effMaxScaleE : ptrA->maxScaleE;
	}

	ptrA->scaleExpE = 0;

	/* downscale work image if necessary */
	while( ptrA->scaleE > ( ( uint32 )( 2 << ptrA->scaleExpE ) << 20 ) ) bbf_Scanner_downscale( cpA, ptrA );

	bbf_Scanner_createBitImage( cpA, ptrA );
	bbf_Scanner_resetScan( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */

flag bbf_Scanner_nextScale( struct bbs_Context* cpA, struct bbf_Scanner* ptrA )
{
	if( ptrA->scaleE + bbf_Scanner_scalePrd( ptrA->scaleE, ptrA->scaleStepE ) >= ptrA->effMaxScaleE ) return FALSE;
	ptrA->scaleE += bbf_Scanner_scalePrd( ptrA->scaleE, ptrA->scaleStepE );

	/* downscale work image if necessary */
	while( ptrA->scaleE > ( ( uint32 )( 2 << ptrA->scaleExpE ) << 20 ) ) bbf_Scanner_downscale( cpA, ptrA );

	bbf_Scanner_createBitImage( cpA, ptrA );
	bbf_Scanner_resetScan( cpA, ptrA );
	return TRUE;
}

/* ------------------------------------------------------------------------- */

const uint32* bbf_Scanner_getPatch( const struct bbf_Scanner* ptrA )
{
	return ptrA->patchBufferE.arrPtrE + ptrA->xE;
}

/* ------------------------------------------------------------------------- */

flag bbf_Scanner_next( struct bbs_Context* cpA, struct bbf_Scanner* ptrA )
{
	if( ( ptrA->xE + 1 ) < ( int32 )( ptrA->currentWidthE - ptrA->patchWidthE ) )
	{
		ptrA->xE++;
		return TRUE;
	}

	if( ( ptrA->yE + 1 ) >= ( int32 )( ptrA->currentHeightE - ptrA->patchHeightE ) ) return FALSE;

	ptrA->xE = 0;
	ptrA->yE++;

	{
		uint32 offL = ( ptrA->yE & 0x1F );
		uint32 rowL = ( ptrA->yE >> 5 ) + ( offL > 0 ? 1 : 0 );

		uint32 sizeL = ptrA->bitImageE.widthE;
		uint32* dstL = ptrA->patchBufferE.arrPtrE;
		uint32 iL;

		if( rowL < ptrA->bitImageE.heightE )
		{
			uint32* srcL = ( uint32* )ptrA->bitImageE.arrE.arrPtrE + rowL * sizeL;
			if( offL > 0 )
			{
				uint32 shlL = 32 - offL;
				for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = ( dstL[ iL ] >> 1 ) | ( srcL[ iL ] << shlL );
			}
			else
			{
				bbs_memcpy32( dstL, srcL, sizeL );
			}
		}
		else
		{
			for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] >>= 1;
		}
	}

	return TRUE;
}

/* ------------------------------------------------------------------------- */

void bbf_Scanner_goToXY( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, int32 xA, int32 yA )
{
	bbs_DEF_fNameL( "void bbf_Scanner_goToXY( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, int32 xA, int32 yA )" )

	if( xA > ( int32 )( ptrA->currentWidthE - ptrA->patchWidthE ) )
	{
		bbs_ERROR1( "%s:\nyA out of range", fNameL );
		return;
	}

	ptrA->xE = xA;

	if( ptrA->yE == yA ) return;

	if( yA >= ( int32 )( ptrA->currentHeightE - ptrA->patchHeightE ) )
	{
		bbs_ERROR1( "%s:\nyA out of range", fNameL );
		return;
	}

	if( yA == ptrA->yE + 1 )
	{
		uint32 offL, rowL;
		uint32 sizeL;
		uint32* dstL;
		uint32 iL;

		ptrA->yE = yA;
		offL = ( ptrA->yE & 0x1F );
		rowL = ( ptrA->yE >> 5 ) + ( offL > 0 ? 1 : 0 );

		sizeL = ptrA->bitImageE.widthE;
		dstL = ptrA->patchBufferE.arrPtrE;

		if( rowL < ptrA->bitImageE.heightE )
		{
			uint32* srcL = ptrA->bitImageE.arrE.arrPtrE + rowL * sizeL;
			if( offL > 0 )
			{
				uint32 shlL = 32 - offL;
				for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = ( dstL[ iL ] >> 1 ) | ( srcL[ iL ] << shlL );
			}
			else
			{
				bbs_memcpy32( dstL, srcL, sizeL );
			}
		}
		else
		{
			for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] >>= 1;
		}
	}
	else
	{
		uint32 offL, rowL;
		uint32 sizeL;
		uint32* dstL;
		uint32 iL;

		ptrA->yE = yA;
		offL = ( ptrA->yE & 0x1F );
		rowL = ( ptrA->yE >> 5 ) + ( offL > 0 ? 1 : 0 );

		sizeL = ptrA->bitImageE.widthE;
		dstL = ptrA->patchBufferE.arrPtrE;

		if( rowL < ptrA->bitImageE.heightE )
		{
			if( offL > 0 )
			{
				uint32* src1L = ptrA->bitImageE.arrE.arrPtrE + rowL * sizeL;
				uint32* src0L = src1L - sizeL;
				uint32 shlL = 32 - offL;
				for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = ( src0L[ iL ] >> offL ) | ( src1L[ iL ] << shlL );
			}
			else
			{
				bbs_memcpy32( dstL, ptrA->bitImageE.arrE.arrPtrE + rowL * sizeL, sizeL );
			}
		}
		else
		{
			uint32* srcL = ptrA->bitImageE.arrE.arrPtrE + ( rowL - 1 ) * sizeL;
			for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = srcL[ iL ] >> offL;
		}
	}
}

/* ------------------------------------------------------------------------- */

void bbf_Scanner_goToIndex( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, uint32 scanIndexA )
{
	int32 yL = scanIndexA / ptrA->currentWidthE; 
	int32 xL = scanIndexA - yL * ptrA->currentWidthE;
	bbf_Scanner_goToXY( cpA, ptrA, xL, yL ); 
}

/* ------------------------------------------------------------------------- */

void bbf_Scanner_goToUls( struct bbs_Context* cpA, struct bbf_Scanner* ptrA, 
						  int32 xA, int32 yA, uint32 scaleA )
{
	int32 xL = ( xA / ( int32 )( ptrA->scaleE >> 4 ) ) + ptrA->borderWidthE;
	int32 yL = ( yA / ( int32 )( ptrA->scaleE >> 4 ) ) + ptrA->borderHeightE;

	if( ptrA->scaleE != scaleA )
	{
		bbs_ERROR0( "bbf_Scanner_goToUls:\nScales no not match" );
		return;
	}

	bbf_Scanner_goToXY( cpA, ptrA, xL, yL );
}

/* ------------------------------------------------------------------------- */

/* resets output positions */
void bbf_Scanner_resetOutPos( struct bbs_Context* cpA, struct bbf_Scanner* ptrA ) 
{
	ptrA->outCountE = 0;
}

/* ------------------------------------------------------------------------- */

/* resets internal positions */
void bbf_Scanner_resetIntPos( struct bbs_Context* cpA, struct bbf_Scanner* ptrA )
{
	ptrA->intCountE = 0;
}

/* ------------------------------------------------------------------------- */

/* add internal position */
void bbf_Scanner_addIntPos( struct bbs_Context* cpA, 
							struct bbf_Scanner* ptrA,
							uint32 idxA,
							int32 actA )
{
	if( ptrA->intCountE < ptrA->idxArrE.sizeE )
	{
        ptrA->idxArrE.arrPtrE[ ptrA->intCountE ] = idxA;
        ptrA->actArrE.arrPtrE[ ptrA->intCountE ] = actA;
		ptrA->intCountE++;
	}
	else
	{
		/* When buffer is full then replace lowest confidence-entry with new input 
		 * This fallback solution causes soft degradation of performance when the buffer limit is reached.
		 */
		int32 minActL = 0x7FFFFFFF;
		uint32 minIdxL = 0;
		uint32 iL;
		int32* actArrL = ptrA->actArrE.arrPtrE;
		for( iL = 0; iL < ptrA->intCountE; iL++ )
		{
			if( actArrL[ iL ] < minActL ) 
			{
				minActL = actArrL[ iL ];
				minIdxL = iL;
			}
		}

		if( actA > minActL )
		{
			ptrA->idxArrE.arrPtrE[ minIdxL ] = idxA;
			ptrA->actArrE.arrPtrE[ minIdxL ] = actA;
		}
	}
}

/* ------------------------------------------------------------------------- */

/* add external position */
void bbf_Scanner_addOutPos( struct bbs_Context* cpA, 
							struct bbf_Scanner* ptrA, 
							int32 xA, 
							int32 yA, 
							uint32 scaleA, 
							int32 actA )
{
	if( ( ptrA->outCountE * 4 ) < ptrA->outArrE.sizeE )
	{
        ptrA->outArrE.arrPtrE[ ptrA->outCountE * 4 + 0 ] = xA;
        ptrA->outArrE.arrPtrE[ ptrA->outCountE * 4 + 1 ] = yA;
        ptrA->outArrE.arrPtrE[ ptrA->outCountE * 4 + 2 ] = scaleA;
        ptrA->outArrE.arrPtrE[ ptrA->outCountE * 4 + 3 ] = actA;
		ptrA->outCountE++;
	}
	else
	{
		/* When buffer is full then replace lowest confidence-entry with new input 
		 * This fallback solution causes soft degradation of performance when the buffer limit is reached.
		 */
		int32 minActL = 0x7FFFFFFF;
		uint32 minIdxL = 0;
		uint32 iL;
		int32* outArrL = ptrA->outArrE.arrPtrE;
		for( iL = 0; iL < ptrA->outCountE; iL++ )
		{
			if( outArrL[ iL * 4 + 3 ] < minActL ) 
			{
				minActL = outArrL[ iL * 4 + 3 ];
				minIdxL = iL;
			}
		}

		if( actA > minActL )
		{
			ptrA->idxArrE.arrPtrE[ minIdxL * 4 + 0 ] = xA;
			ptrA->idxArrE.arrPtrE[ minIdxL * 4 + 1 ] = yA;
			ptrA->idxArrE.arrPtrE[ minIdxL * 4 + 2 ] = scaleA;
			ptrA->idxArrE.arrPtrE[ minIdxL * 4 + 3 ] = actA;
		}
	}
}

/* ------------------------------------------------------------------------- */

/* remove overlaps */
uint32 bbf_Scanner_removeOutOverlaps( struct bbs_Context* cpA, 
							          struct bbf_Scanner* ptrA,
									  uint32 overlapThrA )
{
	uint32 begIdxL = 0;				   /* begin index */
	uint32 endIdxL = ptrA->outCountE;  /* end index */
	uint32 iL;
	uint32 rw0L = ptrA->patchWidthE;
	uint32 rh0L = ptrA->patchHeightE;
	int32* outArrL = ptrA->outArrE.arrPtrE;

	if( overlapThrA >= 0x010000 ) return ptrA->outCountE;

	while( endIdxL - begIdxL > 1 )
	{
		int32 x1L, y1L, s1L, a1L;
		int32 r1wL, r1hL;
		uint32 r1aL;

		/* find maximum activity */
		uint32 maxIdxL  = 0;

		{
			int32 maxActL = ( int32 )0x80000000;
			for( iL = begIdxL; iL < endIdxL; iL++ )
			{
				if( outArrL[ iL * 4 + 3 ] > maxActL )
				{
					maxActL = outArrL[ iL * 4 + 3 ];
					maxIdxL = iL;
				}
			}
		}

		/* swap with position 0 */
		x1L = outArrL[ maxIdxL * 4 + 0 ];
		y1L = outArrL[ maxIdxL * 4 + 1 ];
		s1L = outArrL[ maxIdxL * 4 + 2 ];
		a1L = outArrL[ maxIdxL * 4 + 3 ];

		outArrL[ maxIdxL * 4 + 0 ] = outArrL[ begIdxL * 4 + 0 ];
		outArrL[ maxIdxL * 4 + 1 ] = outArrL[ begIdxL * 4 + 1 ];
		outArrL[ maxIdxL * 4 + 2 ] = outArrL[ begIdxL * 4 + 2 ];
		outArrL[ maxIdxL * 4 + 3 ] = outArrL[ begIdxL * 4 + 3 ];

		outArrL[ begIdxL * 4 + 0 ] = x1L;
		outArrL[ begIdxL * 4 + 1 ] = y1L;
		outArrL[ begIdxL * 4 + 2 ] = s1L;
		outArrL[ begIdxL * 4 + 3 ] = a1L;

		/* rectangle */
		r1wL = ( rw0L * ( s1L >> 12 ) + 128 ) >> 8;
		r1hL = ( rh0L * ( s1L >> 12 ) + 128 ) >> 8;
		r1aL = ( uint32 )r1wL * ( uint32 )r1hL;

		/* remove coordinate fractions */
		x1L = ( x1L + ( 1 << 15 ) ) >> 16;
		y1L = ( y1L + ( 1 << 15 ) ) >> 16;

		/* compare to other rectangles and remove overlaps */
		for( iL = endIdxL - 1; iL > begIdxL; iL-- )
		{
			int32* x2pL = &outArrL[ iL * 4 + 0 ];
			int32* y2pL = &outArrL[ iL * 4 + 1 ];
			int32* s2pL = &outArrL[ iL * 4 + 2 ];
			int32* a2pL = &outArrL[ iL * 4 + 3 ];

			int32 x2L = ( *x2pL + ( 1 << 15 ) ) >> 16;
			int32 y2L = ( *y2pL + ( 1 << 15 ) ) >> 16;

			/* rectangle */
			int32 r2wL = ( rw0L * ( *s2pL >> 12 ) + 128 ) >> 8;
			int32 r2hL = ( rh0L * ( *s2pL >> 12 ) + 128 ) >> 8;
			uint32 r2aL = r2wL * r2hL;

			/* intersection */
			int32 rx1L = x1L > x2L ? x1L : x2L;
			int32 rx2L = ( x1L + r1wL ) < ( x2L + r2wL ) ? ( x1L + r1wL ) : ( x2L + r2wL );
			int32 ry1L = y1L > y2L ? y1L : y2L;
			int32 ry2L = ( y1L + r1hL ) < ( y2L + r2hL ) ? ( y1L + r1hL ) : ( y2L + r2hL );
			uint32 riwL;

			rx2L = ( rx2L > rx1L ) ? rx2L : rx1L;
			ry2L = ( ry2L > ry1L ) ? ry2L : ry1L;
			riwL = ( uint32 )( rx2L - rx1L ) * ( uint32 )( ry2L - ry1L );

			if( riwL > ( ( ( overlapThrA >> 8 ) * ( r1aL < r2aL ? r1aL : r2aL ) ) >> 8 ) )
			{
				endIdxL--;
				*x2pL = outArrL[ endIdxL * 4 + 0 ];
				*y2pL = outArrL[ endIdxL * 4 + 1 ];
				*s2pL = outArrL[ endIdxL * 4 + 2 ];
				*a2pL = outArrL[ endIdxL * 4 + 3 ];
			}
		}

		begIdxL++;
	}

	ptrA->outCountE = endIdxL;

	return endIdxL;
}

/* ------------------------------------------------------------------------- */

/* remove internal overlaps */
uint32 bbf_Scanner_removeIntOverlaps( struct bbs_Context* cpA, 
								      struct bbf_Scanner* ptrA,
									  uint32 overlapThrA )
{
    uint32 begIdxL = 0;		 /* begin index */
    uint32 endIdxL = ptrA->intCountE;  /* end index */
	uint32 iL;
	uint32 rw0L   = ptrA->patchWidthE;
	uint32 rh0L   = ptrA->patchHeightE;
	int32 minAreaL = ( overlapThrA * rw0L * rh0L ) >> 16;

	int32*  actArrL = ptrA->actArrE.arrPtrE;
	uint32* idxArrL = ptrA->idxArrE.arrPtrE;

	if( overlapThrA >= 0x010000 ) return ptrA->intCountE;

	while( endIdxL - begIdxL > 1 )
	{
		/* find maximum activity */
		int32 a1L = ( int32 )0x80000000;
		uint32 i1L = 0;
		uint32 maxIdxL  = 0;
		int32 x1L, y1L;

		for( iL = begIdxL; iL < endIdxL; iL++ )
		{
            if( actArrL[ iL ] > a1L )
			{
				a1L = actArrL[ iL ];
				maxIdxL = iL;
			}
		}

		/* swap with position 0 */
		i1L = idxArrL[ maxIdxL ];
		idxArrL[ maxIdxL ] = idxArrL[ begIdxL ];
		actArrL[ maxIdxL ] = actArrL[ begIdxL ];
		idxArrL[ begIdxL ] = i1L;
		actArrL[ begIdxL ] = a1L;

		/* upper left coordinates */
		y1L = i1L / ptrA->currentWidthE;
		x1L = i1L - ( y1L * ptrA->currentWidthE );

		/* compare to other rectangles and remove overlaps */
		for( iL = endIdxL - 1; iL > begIdxL; iL-- )
		{
			int32*  a2pL = &actArrL[ iL ];
			uint32* i2pL = &idxArrL[ iL ];

			int32 y2L = *i2pL / ptrA->currentWidthE;
			int32 x2L = *i2pL - ( y2L * ptrA->currentWidthE );

			int32 dxL = rw0L - ( x1L > x2L ? x1L - x2L : x2L - x1L );
			int32 dyL = rh0L - ( y1L > y2L ? y1L - y2L : y2L - y1L );

			dxL = dxL > 0 ? dxL : 0;
			dyL = dyL > 0 ? dyL : 0;

			if( dxL * dyL > minAreaL )
			{
				endIdxL--;
				*a2pL = actArrL[ endIdxL ];
				*i2pL = idxArrL[ endIdxL ];
			}
		}

		begIdxL++;
	}

	ptrA->intCountE = endIdxL;

	return ptrA->intCountE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
