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

#include "b_ImageEm/Functions.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ external functions } ----------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/** downscale by factor 2 (dstPtrA and srcPtrA may be identical) */
void bim_downscaleBy2( uint8*       dstPtrA, 
					   const uint8* srcPtrA,
					   uint32 srcWidthA,
					   uint32 effWidthA,
					   uint32 effHeightA )
{
	uint32 wsL = srcWidthA;
	uint32 w0L = effWidthA;
	uint32 h0L = effHeightA;
	uint32 w1L = w0L >> 1;
	uint32 h1L = h0L >> 1;

	const uint8* srcL = srcPtrA;
	uint8* dstL = dstPtrA;

	uint32 iL, jL;
	for( jL = 0; jL < h1L; jL++ )
	{
		for( iL = 0; iL < w1L; iL++ )
		{
			*dstL = ( ( uint32 )srcL[ 0 ] + srcL[ 1 ] + srcL[ wsL ] + srcL[ wsL + 1 ] + 2 ) >> 2;
			dstL++;
			srcL += 2;
		}
		srcL += ( wsL - w1L ) * 2;
	}
}

/* ------------------------------------------------------------------------- */

void bim_filterWarpInterpolation( struct bbs_Context* cpA,
								  uint8* dstImagePtrA, 
								  const uint8* srcImagePtrA,
								  uint32 srcImageWidthA,
								  uint32 srcImageHeightA,
							      const struct bts_Int16Vec2D* offsPtrA,
								  const struct bts_Flt16Alt2D* altPtrA,
								  uint32 dstWidthA,
								  uint32 dstHeightA,
								  struct bbs_UInt8Arr* bufPtrA,
								  uint32 scaleThresholdA )
{
	bbs_DEF_fNameL( "bim_filterWarpInterpolation" )

	uint32 w0L = srcImageWidthA;
	uint32 h0L = srcImageHeightA;

	const uint8* srcL = srcImagePtrA;
	uint8* dstL = dstImagePtrA;

	uint32 w1L = w0L;
	uint32 h1L = h0L;

	/* 16.16 */
	uint32 scaleThrL = scaleThresholdA;
	struct bts_Flt16Alt2D invAltL;

	/* matrix variables */
	int32 mxxL, mxyL, myxL, myyL, txL, tyL;

	flag downScaledL = FALSE;
	flag boundsOkL = TRUE;

	if( w0L == 0 || h0L == 0 || bts_Flt16Mat2D_det( &altPtrA->matE ) == 0 )
	{
		uint32 iL;
		for( iL = 0; iL < dstWidthA * dstHeightA; iL++ ) dstImagePtrA[ iL ] = 0;
		return;
	}

	/* compute inverse ALT */
	invAltL = bts_Flt16Alt2D_inverted( altPtrA );

	/* fixed point ALT 16.16 */
	if( invAltL.matE.bbpE <= 16 )
	{
		uint32 shlL = 16 - invAltL.matE.bbpE;
		mxxL = invAltL.matE.xxE << shlL;
		mxyL = invAltL.matE.xyE << shlL;
		myxL = invAltL.matE.yxE << shlL;
		myyL = invAltL.matE.yyE << shlL;
	}
	else
	{
		uint32 shrL = invAltL.matE.bbpE - 16;
		mxxL = ( ( invAltL.matE.xxE >> ( shrL - 1 ) ) + 1 ) >> 1;
		mxyL = ( ( invAltL.matE.xyE >> ( shrL - 1 ) ) + 1 ) >> 1;
		myxL = ( ( invAltL.matE.yxE >> ( shrL - 1 ) ) + 1 ) >> 1;
		myyL = ( ( invAltL.matE.yyE >> ( shrL - 1 ) ) + 1 ) >> 1;
	}

	if( invAltL.vecE.bbpE <= 16 )
	{
		uint32 shlL = 16 - invAltL.vecE.bbpE;
		txL = invAltL.vecE.xE << shlL;
		tyL = invAltL.vecE.yE << shlL;
	}
	else
	{
		uint32 shrL = invAltL.vecE.bbpE - 16;
		txL = ( ( invAltL.vecE.xE >> ( shrL - 1 ) ) + 1 ) >> 1;
		tyL = ( ( invAltL.vecE.yE >> ( shrL - 1 ) ) + 1 ) >> 1;
	}

	/* add offset */
	txL += ( int32 )offsPtrA->xE << 16;
	tyL += ( int32 )offsPtrA->yE << 16;

	if( scaleThresholdA > 0 )
	{
		/* compute downscale exponent */
		uint32 axxL = ( mxxL >= 0 ) ? mxxL : -mxxL;
		uint32 axyL = ( mxyL >= 0 ) ? mxyL : -mxyL;
		uint32 ayxL = ( myxL >= 0 ) ? myxL : -myxL;
		uint32 ayyL = ( myyL >= 0 ) ? myyL : -myyL;

		uint32 a1L = ( axxL > ayxL ) ? axxL : ayxL;
		uint32 a2L = ( axyL > ayyL ) ? axyL : ayyL;

		uint32 invScaleL = ( a1L < a2L ) ? a1L : a2L;
		uint32 scaleExpL = 0;
		while( ( invScaleL >> scaleExpL ) > scaleThrL ) scaleExpL++;
		while( ( scaleExpL > 0 ) && ( w0L >> scaleExpL ) < 2 ) scaleExpL--;
		while( ( scaleExpL > 0 ) && ( h0L >> scaleExpL ) < 2 ) scaleExpL--;

		/* downscale image */
		if( scaleExpL > 0 )
		{
			/* down sampling is limited to the effective area of the original image */

			/* compute effective area by mapping all corners of the dst rectangle */
			int32 xMinL = 0x7FFFFFFF;
			int32 yMinL = 0x7FFFFFFF;
			int32 xMaxL = 0x80000000;
			int32 yMaxL = 0x80000000;
			uint32 wEffL, hEffL;

			{
				int32 xL, yL;
				xL = txL;
				yL = tyL;
				xMinL = xL < xMinL ? xL : xMinL;
				yMinL = yL < yMinL ? yL : yMinL;
				xMaxL = xL > xMaxL ? xL : xMaxL;
				yMaxL = yL > yMaxL ? yL : yMaxL;
				xL = txL + mxxL * ( int32 )dstWidthA + mxyL * ( int32 )dstHeightA;
				yL = tyL + myxL * ( int32 )dstWidthA + myyL * ( int32 )dstHeightA;
				xMinL = xL < xMinL ? xL : xMinL;
				yMinL = yL < yMinL ? yL : yMinL;
				xMaxL = xL > xMaxL ? xL : xMaxL;
				yMaxL = yL > yMaxL ? yL : yMaxL;
				xL = txL + mxyL * ( int32 )dstHeightA;
				yL = tyL + myyL * ( int32 )dstHeightA;
				xMinL = xL < xMinL ? xL : xMinL;
				yMinL = yL < yMinL ? yL : yMinL;
				xMaxL = xL > xMaxL ? xL : xMaxL;
				yMaxL = yL > yMaxL ? yL : yMaxL;
				xL = txL + mxxL * ( int32 )dstWidthA;
				yL = tyL + myxL * ( int32 )dstWidthA;
				xMinL = xL < xMinL ? xL : xMinL;
				yMinL = yL < yMinL ? yL : yMinL;
				xMaxL = xL > xMaxL ? xL : xMaxL;
				yMaxL = yL > yMaxL ? yL : yMaxL;
			}

			xMaxL = ( xMaxL >> 16 ) + 2;
			yMaxL = ( yMaxL >> 16 ) + 2;
			xMinL >>= 16; 
			yMinL >>= 16; 

			/* ensre effective area stays within original frame */
			xMinL = 0 > xMinL ? 0 : xMinL;
			yMinL = 0 > yMinL ? 0 : yMinL;
			xMinL = ( int32 )w0L < xMinL ? w0L : xMinL;
			yMinL = ( int32 )h0L < yMinL ? h0L : yMinL;
			xMaxL = 0 > xMaxL ? 0 : xMaxL;
			yMaxL = 0 > yMaxL ? 0 : yMaxL;
			xMaxL = ( int32 )w0L < xMaxL ? w0L : xMaxL;
			yMaxL = ( int32 )h0L < yMaxL ? h0L : yMaxL;

			wEffL = xMaxL - xMinL;
			hEffL = yMaxL - yMinL;

			/* ensure downscaling does not reduce image to 0 */
			while( ( scaleExpL > 0 ) && ( wEffL >> scaleExpL ) < 2 ) scaleExpL--;
			while( ( scaleExpL > 0 ) && ( hEffL >> scaleExpL ) < 2 ) scaleExpL--;

			/* downscale */
			if( scaleExpL > 0 )
			{
				uint32 iL;
				w1L = wEffL >> 1;
				h1L = hEffL >> 1;
				if( bufPtrA == NULL ) bbs_ERROR1( "%s:\nPreallocated buffer is needed", fNameL );
				bbs_UInt8Arr_size( cpA, bufPtrA, w1L * h1L );
				bim_downscaleBy2( bufPtrA->arrPtrE, srcL + yMinL * w0L + xMinL, w0L, wEffL, hEffL );
				for( iL = 1; iL < scaleExpL; iL++ )
				{
					bim_downscaleBy2( bufPtrA->arrPtrE, bufPtrA->arrPtrE, w1L, w1L, h1L );
					w1L >>= 1;
					h1L >>= 1;
				}

				/* adjust inverted cordinates */
				txL -= ( xMinL << 16 );
				tyL -= ( yMinL << 16 );
				mxxL >>= scaleExpL;
				mxyL >>= scaleExpL;
				myxL >>= scaleExpL;
				myyL >>= scaleExpL;
				txL >>= scaleExpL;
				tyL >>= scaleExpL;
				srcL = bufPtrA->arrPtrE;
			}

			downScaledL = TRUE;
		}
	}
	
	/* if not downscaled and src and dst images are identcal then copy srcImage into buffer */
	if( !downScaledL && dstImagePtrA == srcImagePtrA ) 
	{
		uint32 iL;
		uint32 srcSizeL = srcImageWidthA * srcImageHeightA;
		if( bufPtrA == NULL ) bbs_ERROR1( "%s:\nPreallocated buffer is needed", fNameL );
		bbs_UInt8Arr_size( cpA, bufPtrA, srcSizeL );
		for( iL = 0; iL < srcSizeL; iL++ ) bufPtrA->arrPtrE[ iL ] = srcImagePtrA[ iL ];
		srcL = bufPtrA->arrPtrE;
	}

	/* compute destination image */

	/* bounds check (dst image fully inside src image? -> fast algorithm) */
	{
		int32 xL, yL;
		int32 wbL = w1L - 1;
		int32 hbL = h1L - 1;

		xL = txL >> 16;
		yL = tyL >> 16;
		boundsOkL = boundsOkL && ( xL >= 0 && xL < wbL && yL >= 0 && yL < hbL );

		xL = ( txL + mxxL * ( int32 )( dstWidthA - 1 ) ) >> 16;
		yL = ( tyL + myxL * ( int32 )( dstWidthA - 1 ) ) >> 16;
		boundsOkL = boundsOkL && ( xL >= 0 && xL < wbL && yL >= 0 && yL < hbL );
      
		xL = ( txL + mxyL * ( int32 )( dstHeightA - 1 ) ) >> 16;
		yL = ( tyL + myyL * ( int32 )( dstHeightA - 1 ) ) >> 16;
		boundsOkL = boundsOkL && ( xL >= 0 && xL < wbL && yL >= 0 && yL < hbL );

		xL = ( txL + mxyL * ( int32 )( dstHeightA - 1 ) + mxxL * ( int32 )( dstWidthA - 1 ) ) >> 16;
		yL = ( tyL + myyL * ( int32 )( dstHeightA - 1 ) + myxL * ( int32 )( dstWidthA - 1 ) ) >> 16;
		boundsOkL = boundsOkL && ( xL >= 0 && xL < wbL && yL >= 0 && yL < hbL );
	}

	if( boundsOkL )
	{
		int32 iL, jL;
		for( jL = 0; jL < ( int32 )dstHeightA; jL++ )
		{
			/* 16.16 */
			int32 xL = txL + mxyL * jL;
			int32 yL = tyL + myyL * jL;
			for( iL = 0; iL < ( int32 )dstWidthA; iL++ )
			{
				int32 x0L = xL >> 16;
				int32 y0L = yL >> 16;
				uint32 xf2L = xL & 0x0FFFF;
				uint32 yf2L = yL & 0x0FFFF;
				uint32 xf1L = 0x10000 - xf2L;
				uint32 yf1L = 0x10000 - yf2L;

				xL += mxxL;
				yL += myxL;

				{
					uint32 idxL = y0L * w1L + x0L;
					uint32 v1L = ( ( uint32 )srcL[ idxL       ] * xf1L + ( uint32 )srcL[ idxL + 1       ] * xf2L + 0x0800 ) >> 12;
					uint32 v2L = ( ( uint32 )srcL[ idxL + w1L ] * xf1L + ( uint32 )srcL[ idxL + w1L + 1 ] * xf2L + 0x0800 ) >> 12;
					*dstL++ = ( v1L * yf1L + v2L * yf2L + 0x080000 ) >> 20;
				}
			}
		}
	}
	else
	{
		int32 iL, jL;
		for( jL = 0; jL < ( int32 )dstHeightA; jL++ )
		{
			/* 16.16 */
			int32 xL = txL + mxyL * jL;
			int32 yL = tyL + myyL * jL;
			for( iL = 0; iL < ( int32 )dstWidthA; iL++ )
			{
				int32 x0L = xL >> 16;
				int32 y0L = yL >> 16;
				uint32 xf2L = xL & 0x0FFFF;
				uint32 yf2L = yL & 0x0FFFF;
				uint32 xf1L = 0x10000 - xf2L;
				uint32 yf1L = 0x10000 - yf2L;

				xL += mxxL;
				yL += myxL;

				if( y0L < 0 )
				{
					if( x0L < 0 )
					{
						*dstL++ = srcL[ 0 ];
					}
					else if( x0L >= ( int32 )w1L - 1 )
					{
						*dstL++ = srcL[ w1L - 1 ];
					}
					else
					{
						*dstL++ = ( ( uint32 )srcL[ x0L ] * xf1L + ( uint32 )srcL[ x0L + 1 ] * xf2L + 0x08000 ) >> 16;
					}
				}
				else if( y0L >= ( int32 )h1L - 1 )
				{
					if( x0L < 0 )
					{
						*dstL++ = srcL[ ( h1L - 1 ) * w1L ];
					}
					else if( x0L >= ( int32 )w1L - 1 )
					{
						*dstL++ = srcL[ ( h1L * w1L ) - 1 ];
					}
					else
					{
						uint32 idxL = ( h1L - 1 ) * w1L + x0L;
						*dstL++ = ( ( uint32 )srcL[ idxL ] * xf1L + ( uint32 )srcL[ idxL + 1 ] * xf2L + 0x08000 ) >> 16;
					}
				}
				else
				{
					if( x0L < 0 )
					{
						uint32 idxL = y0L * w1L;
						*dstL++ = ( ( uint32 )srcL[ idxL ] * yf1L + ( uint32 )srcL[ idxL + w1L ] * yf2L + 0x08000 ) >> 16;
					}
					else if( x0L >= ( int32 )w1L - 1 )
					{
						uint32 idxL = ( y0L + 1 ) * w1L - 1;
						*dstL++ = ( ( uint32 )srcL[ idxL ] * yf1L + ( uint32 )srcL[ idxL + w1L ] * yf2L + 0x08000 ) >> 16;
					}
					else
					{
						uint32 idxL = y0L * w1L + x0L;
						uint32 v1L = ( ( uint32 )srcL[ idxL       ] * xf1L + ( uint32 )srcL[ idxL + 1       ] * xf2L + 0x0800 ) >> 12;
						uint32 v2L = ( ( uint32 )srcL[ idxL + w1L ] * xf1L + ( uint32 )srcL[ idxL + w1L + 1 ] * xf2L + 0x0800 ) >> 12;
						*dstL++ = ( v1L * yf1L + v2L * yf2L + 0x080000 ) >> 20;
					}
				}
			}
		}
	}
}

/* ------------------------------------------------------------------------- */

void bim_filterWarpPixelReplication( struct bbs_Context* cpA,
								     uint8* dstImagePtrA, 
								     const uint8* srcImagePtrA,
								     uint32 srcImageWidthA,
								     uint32 srcImageHeightA,
								     const struct bts_Int16Vec2D* offsPtrA,
								     const struct bts_Flt16Alt2D* altPtrA,
								     uint32 dstWidthA,
								     uint32 dstHeightA,
								     struct bbs_UInt8Arr* bufPtrA,
								     uint32 scaleThresholdA )
{
	bbs_DEF_fNameL( "bim_filterWarpPixelReplication" )

	uint32 w0L = srcImageWidthA;
	uint32 h0L = srcImageHeightA;

	const uint8* srcL = srcImagePtrA;
	uint8* dstL = dstImagePtrA;

	uint32 w1L = w0L;
	uint32 h1L = h0L;

	/* 16.16 */
	uint32 scaleThrL = scaleThresholdA;
	struct bts_Flt16Alt2D invAltL;

	/* matrix variables */
	int32 mxxL, mxyL, myxL, myyL, txL, tyL;

	flag downScaledL = FALSE;
	flag boundsOkL = TRUE;

	if( w0L == 0 || h0L == 0 || bts_Flt16Mat2D_det( &altPtrA->matE ) == 0 )
	{
		uint32 iL;
		for( iL = 0; iL < dstWidthA * dstHeightA; iL++ ) dstImagePtrA[ iL ] = 0;
		return;
	}

	/* compute inverse ALT */
	invAltL = bts_Flt16Alt2D_inverted( altPtrA );

	/* fixed point ALT 16.16 */
	if( invAltL.matE.bbpE <= 16 )
	{
		uint32 shlL = 16 - invAltL.matE.bbpE;
		mxxL = invAltL.matE.xxE << shlL;
		mxyL = invAltL.matE.xyE << shlL;
		myxL = invAltL.matE.yxE << shlL;
		myyL = invAltL.matE.yyE << shlL;
	}
	else
	{
		uint32 shrL = invAltL.matE.bbpE - 16;
		mxxL = ( ( invAltL.matE.xxE >> ( shrL - 1 ) ) + 1 ) >> 1;
		mxyL = ( ( invAltL.matE.xyE >> ( shrL - 1 ) ) + 1 ) >> 1;
		myxL = ( ( invAltL.matE.yxE >> ( shrL - 1 ) ) + 1 ) >> 1;
		myyL = ( ( invAltL.matE.yyE >> ( shrL - 1 ) ) + 1 ) >> 1;
	}

	if( invAltL.vecE.bbpE <= 16 )
	{
		uint32 shlL = 16 - invAltL.vecE.bbpE;
		txL = invAltL.vecE.xE << shlL;
		tyL = invAltL.vecE.yE << shlL;
	}
	else
	{
		uint32 shrL = invAltL.vecE.bbpE - 16;
		txL = ( ( invAltL.vecE.xE >> ( shrL - 1 ) ) + 1 ) >> 1;
		tyL = ( ( invAltL.vecE.yE >> ( shrL - 1 ) ) + 1 ) >> 1;
	}

	/* add offset */
	txL += ( int32 )offsPtrA->xE << 16;
	tyL += ( int32 )offsPtrA->yE << 16;

	if( scaleThresholdA > 0 )
	{
		/* compute downscale exponent */
		uint32 axxL = ( mxxL >= 0 ) ? mxxL : -mxxL;
		uint32 axyL = ( mxyL >= 0 ) ? mxyL : -mxyL;
		uint32 ayxL = ( myxL >= 0 ) ? myxL : -myxL;
		uint32 ayyL = ( myyL >= 0 ) ? myyL : -myyL;

		uint32 a1L = ( axxL > ayxL ) ? axxL : ayxL;
		uint32 a2L = ( axyL > ayyL ) ? axyL : ayyL;

		uint32 invScaleL = ( a1L < a2L ) ? a1L : a2L;
		uint32 scaleExpL = 0;
		while( ( invScaleL >> scaleExpL ) > scaleThrL ) scaleExpL++;
		while( ( scaleExpL > 0 ) && ( w0L >> scaleExpL ) < 2 ) scaleExpL--;
		while( ( scaleExpL > 0 ) && ( h0L >> scaleExpL ) < 2 ) scaleExpL--;

		/* downscale image */
		if( scaleExpL > 0 )
		{
			/* down sampling is limited to the effective area of the original image */

			/* compute effective area by mapping all corners of the dst rectangle */
			int32 xMinL = 0x7FFFFFFF;
			int32 yMinL = 0x7FFFFFFF;
			int32 xMaxL = 0x80000000;
			int32 yMaxL = 0x80000000;
			uint32 wEffL, hEffL;

			{
				int32 xL, yL;
				xL = txL;
				yL = tyL;
				xMinL = xL < xMinL ? xL : xMinL;
				yMinL = yL < yMinL ? yL : yMinL;
				xMaxL = xL > xMaxL ? xL : xMaxL;
				yMaxL = yL > yMaxL ? yL : yMaxL;
				xL = txL + mxxL * ( int32 )dstWidthA + mxyL * ( int32 )dstHeightA;
				yL = tyL + myxL * ( int32 )dstWidthA + myyL * ( int32 )dstHeightA;
				xMinL = xL < xMinL ? xL : xMinL;
				yMinL = yL < yMinL ? yL : yMinL;
				xMaxL = xL > xMaxL ? xL : xMaxL;
				yMaxL = yL > yMaxL ? yL : yMaxL;
				xL = txL + mxyL * ( int32 )dstHeightA;
				yL = tyL + myyL * ( int32 )dstHeightA;
				xMinL = xL < xMinL ? xL : xMinL;
				yMinL = yL < yMinL ? yL : yMinL;
				xMaxL = xL > xMaxL ? xL : xMaxL;
				yMaxL = yL > yMaxL ? yL : yMaxL;
				xL = txL + mxxL * ( int32 )dstWidthA;
				yL = tyL + myxL * ( int32 )dstWidthA;
				xMinL = xL < xMinL ? xL : xMinL;
				yMinL = yL < yMinL ? yL : yMinL;
				xMaxL = xL > xMaxL ? xL : xMaxL;
				yMaxL = yL > yMaxL ? yL : yMaxL;
			}

			xMaxL = ( xMaxL >> 16 ) + 2;
			yMaxL = ( yMaxL >> 16 ) + 2;
			xMinL >>= 16; 
			yMinL >>= 16; 

			/* ensre effective area stays within original frame */
			xMinL = 0 > xMinL ? 0 : xMinL;
			yMinL = 0 > yMinL ? 0 : yMinL;
			xMinL = ( int32 )w0L < xMinL ? w0L : xMinL;
			yMinL = ( int32 )h0L < yMinL ? h0L : yMinL;
			xMaxL = 0 > xMaxL ? 0 : xMaxL;
			yMaxL = 0 > yMaxL ? 0 : yMaxL;
			xMaxL = ( int32 )w0L < xMaxL ? w0L : xMaxL;
			yMaxL = ( int32 )h0L < yMaxL ? h0L : yMaxL;

			wEffL = xMaxL - xMinL;
			hEffL = yMaxL - yMinL;

			/* ensure downscaling does not reduce image to 0 */
			while( ( scaleExpL > 0 ) && ( wEffL >> scaleExpL ) < 2 ) scaleExpL--;
			while( ( scaleExpL > 0 ) && ( hEffL >> scaleExpL ) < 2 ) scaleExpL--;

			/* downscale */
			if( scaleExpL > 0 )
			{
				uint32 iL;
				w1L = wEffL >> 1;
				h1L = hEffL >> 1;
				if( bufPtrA == NULL ) bbs_ERROR1( "%s:\nPreallocated buffer is needed", fNameL );
				bbs_UInt8Arr_size( cpA, bufPtrA, w1L * h1L );
				bim_downscaleBy2( bufPtrA->arrPtrE, srcL + yMinL * w0L + xMinL, w0L, wEffL, hEffL );
				for( iL = 1; iL < scaleExpL; iL++ )
				{
					bim_downscaleBy2( bufPtrA->arrPtrE, bufPtrA->arrPtrE, w1L, w1L, h1L );
					w1L >>= 1;
					h1L >>= 1;
				}

				/* adjust inverted cordinates */
				txL -= ( xMinL << 16 );
				tyL -= ( yMinL << 16 );
				mxxL >>= scaleExpL;
				mxyL >>= scaleExpL;
				myxL >>= scaleExpL;
				myyL >>= scaleExpL;
				txL >>= scaleExpL;
				tyL >>= scaleExpL;
				srcL = bufPtrA->arrPtrE;
			}

			downScaledL = TRUE;
		}
	}
	
	/* if not downscaled and src and dst images are identcal then copy srcImage into buffer */
	if( !downScaledL && dstImagePtrA == srcImagePtrA ) 
	{
		uint32 iL;
		uint32 srcSizeL = srcImageWidthA * srcImageHeightA;
		if( bufPtrA == NULL ) bbs_ERROR1( "%s:\nPreallocated buffer is needed", fNameL );
		bbs_UInt8Arr_size( cpA, bufPtrA, srcSizeL );
		for( iL = 0; iL < srcSizeL; iL++ ) bufPtrA->arrPtrE[ iL ] = srcImagePtrA[ iL ];
		srcL = bufPtrA->arrPtrE;
	}

	/* compute destination image */

	/* bounds check (dst image fully inside src image? -> fast algorithm) */
	{
		int32 xL, yL;
		int32 wbL = w1L - 1;
		int32 hbL = h1L - 1;

		xL = txL >> 16;
		yL = tyL >> 16;
		boundsOkL = boundsOkL && ( xL >= 0 && xL < wbL && yL >= 0 && yL < hbL );

		xL = ( txL + mxxL * ( int32 )( dstWidthA - 1 ) ) >> 16;
		yL = ( tyL + myxL * ( int32 )( dstWidthA - 1 ) ) >> 16;
		boundsOkL = boundsOkL && ( xL >= 0 && xL < wbL && yL >= 0 && yL < hbL );
      
		xL = ( txL + mxyL * ( int32 )( dstHeightA - 1 ) ) >> 16;
		yL = ( tyL + myyL * ( int32 )( dstHeightA - 1 ) ) >> 16;
		boundsOkL = boundsOkL && ( xL >= 0 && xL < wbL && yL >= 0 && yL < hbL );

		xL = ( txL + mxyL * ( int32 )( dstHeightA - 1 ) + mxxL * ( int32 )( dstWidthA - 1 ) ) >> 16;
		yL = ( tyL + myyL * ( int32 )( dstHeightA - 1 ) + myxL * ( int32 )( dstWidthA - 1 ) ) >> 16;
		boundsOkL = boundsOkL && ( xL >= 0 && xL < wbL && yL >= 0 && yL < hbL );
	}

	if( boundsOkL )
	{
		int32 iL, jL;
		for( jL = 0; jL < ( int32 )dstHeightA; jL++ )
		{
			/* 16.16 */
			int32 xL = txL + mxyL * jL;
			int32 yL = tyL + myyL * jL;
			for( iL = 0; iL < ( int32 )dstWidthA; iL++ )
			{
				/* nearest whole position */
				*dstL++ = srcL[ ( ( ( yL >> 15 ) + 1 ) >> 1 ) * w1L + ( ( ( xL >> 15 ) + 1 ) >> 1 ) ];
				xL += mxxL;
				yL += myxL;
			}
		}
	}
	else
	{
		int32 iL, jL;
		for( jL = 0; jL < ( int32 )dstHeightA; jL++ )
		{
			/* 16.16 */
			int32 xL = txL + mxyL * jL;
			int32 yL = tyL + myyL * jL;
			for( iL = 0; iL < ( int32 )dstWidthA; iL++ )
			{
				/* nearest whole position */
				int32 x0L = ( ( xL >> 15 ) + 1 ) >> 1;
				int32 y0L = ( ( yL >> 15 ) + 1 ) >> 1;
				xL += mxxL;
				yL += myxL;

				if( y0L < 0 )
				{
					if( x0L < 0 )
					{
						*dstL++ = srcL[ 0 ];
					}
					else if( x0L >= ( int32 )w1L - 1 )
					{
						*dstL++ = srcL[ w1L - 1 ];
					}
					else
					{
						*dstL++ = srcL[ x0L ];
					}
				}
				else if( y0L >= ( int32 )h1L - 1 )
				{
					if( x0L < 0 )
					{
						*dstL++ = srcL[ ( h1L - 1 ) * w1L ];
					}
					else if( x0L >= ( int32 )w1L - 1 )
					{
						*dstL++ = srcL[ ( h1L * w1L ) - 1 ];
					}
					else
					{
						*dstL++ = srcL[ ( h1L - 1 ) * w1L + x0L ];
					}
				}
				else
				{
					if( x0L < 0 )
					{
						*dstL++ = srcL[ y0L * w1L ];
					}
					else if( x0L >= ( int32 )w1L - 1 )
					{
						*dstL++ = srcL[ ( y0L + 1 ) * w1L - 1 ];
					}
					else
					{
						*dstL++ = srcL[ y0L * w1L + x0L ];
					}
				}
			}
		}
	}
}

/* ------------------------------------------------------------------------- */

void bim_filterWarp( struct bbs_Context* cpA,
					 uint8* dstImagePtrA, 
					 const uint8* srcImagePtrA,
					 uint32 srcImageWidthA,
					 uint32 srcImageHeightA,
				     const struct bts_Int16Vec2D* offsPtrA,
					 const struct bts_Flt16Alt2D* altPtrA,
					 uint32 dstWidthA,
					 uint32 dstHeightA,
					 struct bbs_UInt8Arr* bufPtrA,
					 uint32 scaleThresholdA,
					 flag interpolateA )
{
	if( interpolateA )
	{
		bim_filterWarpInterpolation( cpA, dstImagePtrA, srcImagePtrA, srcImageWidthA, srcImageHeightA, offsPtrA, altPtrA, dstWidthA, dstHeightA, bufPtrA, scaleThresholdA );
	}
	else
	{
		bim_filterWarpPixelReplication( cpA, dstImagePtrA, srcImagePtrA, srcImageWidthA, srcImageHeightA, offsPtrA, altPtrA, dstWidthA, dstHeightA, bufPtrA, scaleThresholdA );
	}
}

/* ------------------------------------------------------------------------- */

