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
#include "b_BitFeatureEm/LocalScanner.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/** allocates arays */
void bbf_LocalScanner_alloc( struct bbs_Context* cpA,
							 struct bbf_LocalScanner* ptrA, 
							 struct bbs_MemTbl* mtpA )
{
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_segPtr( cpA, &memTblL, 0 );
	struct bbs_MemSeg* sspL = bbs_MemTbl_sharedSegPtr( cpA, &memTblL, 0 );

	/* filter patch dimension */
	uint32 proL = ptrA->maxRadiusE;
	uint32 pwoL = ( proL << 1 ) + 1;

	/* output image size (bit image) */
	uint32 woL = ptrA->maxImageWidthE;
	uint32 hoL = ptrA->maxImageHeightE;

	if( ptrA->minScaleExpE > 0 )
	{
		/* allocate working image */
		bbs_UInt8Arr_create( cpA, &ptrA->workImageBufferE, ( woL >> 1 ) * ( hoL >> 1 ), espL );
		bbs_UInt8Arr_fill( cpA, &ptrA->workImageBufferE, 0 );
	}

	/* allocate bit image */
	bim_UInt32Image_create( cpA, &ptrA->bitImageE, woL, ( hoL >> 5 ) + ( ( ( hoL & 0x1F ) != 0 ) ? 1 : 0 ), espL );
	bim_UInt32Image_setAllPixels( cpA, &ptrA->bitImageE, 0, 0 );

	/* allocate patch buffer */
	bbs_UInt32Arr_create( cpA, &ptrA->patchBufferE, ptrA->bitImageE.widthE, espL );
	bbs_UInt32Arr_fill( cpA, &ptrA->patchBufferE, 0 );

	/* allocate table */
	bim_UInt32Image_create( cpA, &ptrA->satE, woL + pwoL, pwoL + 1, sspL );
}

/* ------------------------------------------------------------------------- */

/** downscales original image by factor 2 */
void bbf_LocalScanner_downscale0( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA )
{
	int32 w0L = ptrA->origWidthE;
	int32 h0L = ptrA->origHeightE;

	int32 w1L = ( w0L - ptrA->xOffE ) >> 1;
	int32 h1L = ( h0L - ptrA->yOffE ) >> 1;

	const uint8* iArrL = ptrA->origImagePtrE + ptrA->xOffE + ptrA->yOffE * w0L;
		  uint8* oArrL = ptrA->workImageBufferE.arrPtrE;

	int32 iL, jL;
	int32 kL = 0;

	bbs_UInt8Arr_size( cpA, &ptrA->workImageBufferE, w1L * h1L );
	ptrA->workImagePtrE = ptrA->workImageBufferE.arrPtrE;
	ptrA->workWidthE = w1L;
	ptrA->workHeightE = h1L;

	for( jL = 0; jL < h1L; jL++ )
	{
		for( iL = 0; iL < w1L; iL++ )
		{
			int32 idxL = jL * 2 * w0L + iL * 2;
			oArrL[ kL++ ] = ( ( uint32 )iArrL[ idxL           ] + 
										iArrL[ idxL + 1       ] + 
										iArrL[ idxL + w0L     ] + 
										iArrL[ idxL + w0L + 1 ] + 2 ) >> 2;
		}
	}
}

/* ------------------------------------------------------------------------- */

/** downscales work image by factor 2 */
void bbf_LocalScanner_downscale1( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA )
{
	int32 w0L = ptrA->workWidthE;
	int32 h0L = ptrA->workHeightE;
	int32 w1L = w0L >> 1;
	int32 h1L = h0L >> 1;

    uint8* arrL = ptrA->workImageBufferE.arrPtrE;

	int32 iL, jL;
	int32 kL = 0;

	for( jL = 0; jL < h1L; jL++ )
	{
		for( iL = 0; iL < w1L; iL++ )
		{
			int32 idxL = jL * 2 * w0L + iL * 2;
			arrL[ kL++ ] = ( ( uint32 )arrL[ idxL ] + 
									   arrL[ idxL + 1 ] + 
									   arrL[ idxL + w0L ] + 
									   arrL[ idxL + w0L + 1 ] + 2 ) >> 2;
		}
	}

	ptrA->workWidthE = w1L;
	ptrA->workHeightE = h1L;
}

/* ------------------------------------------------------------------------- */

/** downscales by factor 2 */
void bbf_LocalScanner_downscale( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA )
{
	uint32 iL;
	if( ptrA->scaleExpE > 0 ) bbf_LocalScanner_downscale0( cpA, ptrA );
	for( iL = 1; iL < ptrA->scaleExpE; iL++ ) bbf_LocalScanner_downscale1( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */

/** computes bit image */
void bbf_LocalScanner_createBitImage( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA )
{
	bbs_DEF_fNameL( "void bbf_LocalScanner_createBitImage( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA )" )

	uint32 iL, jL;

	uint32 proL = ptrA->bitParamE.outerRadiusE;
	uint32 priL = ptrA->bitParamE.innerRadiusE;
	uint32 pwoL = ( proL << 1 ) + 1;
	uint32 pwiL = ( priL << 1 ) + 1;

	/* areas of inner and outer rectangles */
	uint32 poAreaL = pwoL * pwoL;
	uint32 piAreaL = pwiL * pwiL;

	uint32 wL, hL; /* input image size */

	uint32 wsL, hsL; 
	uint32* satL;
	uint32 satSizeL;
	uint32 swi1L = 0; /* writing index */
	uint32 swi2L = 0; /* writing index */
	uint32 sriL = 0;  /* reading index */
	uint32 siL[ 8 ];

	uint32  bitMaskL;
	uint32* bitRowL;


	if( proL <= priL ) 
	{
		bbs_ERROR1( "%s:\n outer radius <= inner radius", fNameL );
		return;
	}

	/* input image size */
	wL = ptrA->workWidthE;
	hL = ptrA->workHeightE;

	if( wL <= pwoL || hL <= pwoL ) 
	{
		bbs_ERROR1( "%s:\n image is too small", fNameL );
		return;
	}

	ptrA->currentWidthE  = wL;
	ptrA->currentHeightE = hL;

	/* reset scan region */
	ptrA->workScanRegionE = bts_Int16Rect_create( 0, 0, ptrA->currentWidthE, ptrA->currentHeightE );

	/* initialize bit image */
	bim_UInt32Image_size( cpA, &ptrA->bitImageE, wL, ( hL >> 5 ) + ( ( ( hL & 0x1F ) != 0 ) ? 1 : 0 ) );
	bim_UInt32Image_setAllPixels( cpA, &ptrA->bitImageE, 0, 0 );

	bitMaskL = 1;
	bitRowL = ( uint32* )ptrA->bitImageE.arrE.arrPtrE;

	/* width of table */
	wsL = wL + pwoL;

	/* height of table */
	hsL = pwoL + 1;

	bim_UInt32Image_size( cpA, &ptrA->satE, wsL, hsL );

	satL = ( uint32* )ptrA->satE.arrE.arrPtrE;
	satSizeL = ptrA->satE.arrE.sizeE;

	/* compute table and bit image */
	for( iL = wsL * ( proL + 1 ); iL > 0; iL-- ) satL[ swi1L++ ] = 0;
	swi2L = swi1L - wsL;

	for( jL = 0; jL < hL + proL; jL++ )
	{
		if( jL < hL ) /* rescale area */
		{
			const uint8* arr0L = &ptrA->workImagePtrE[ jL * wL ];
			uint32 hSumL = 0;
			for( iL = 0; iL <= proL; iL++ ) satL[ swi1L++ ] = 0;
			swi2L += iL;
			for( iL = 0; iL < wL; iL++ )   satL[ swi1L++ ] = ( hSumL += arr0L[ iL ] ) + satL[ swi2L++ ];
			for( iL = 0; iL < proL; iL++ ) satL[ swi1L++ ] =   hSumL                  + satL[ swi2L++ ];
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

			for( iL = 0; iL < wL; iL++ )
			{
				uint32 oAvgL = ( rSatL[ siL[ 0 ] ] - rSatL[ siL[ 1 ] ] - rSatL[ siL[ 2 ] ] + rSatL[ siL[ 3 ] ] ) * piAreaL;
				uint32 iAvgL = ( rSatL[ siL[ 4 ] ] - rSatL[ siL[ 5 ] ] - rSatL[ siL[ 6 ] ] + rSatL[ siL[ 7 ] ] ) * poAreaL;
				bitRowL[ iL ] |= ( iAvgL > oAvgL ) ? bitMaskL : 0;
				rSatL++;
			}
			if( ( bitMaskL <<= 1 ) == 0 )
			{
				bitRowL += wL;
				bitMaskL = 1;
			}
		}
	}
}

/* -------------------------------------------------------------------------- */

/** inilialize patch buffer */
void bbf_LocalScanner_initPatchBuffer( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA )
{
	int32 ybL = ptrA->workScanRegionE.y1E >> 5;
	int32 yoL = ptrA->workScanRegionE.y1E & 0x1F;
	int32 xbL = ptrA->workScanRegionE.x1E;
	uint32 wsrWidthL = ptrA->workScanRegionE.x2E - ptrA->workScanRegionE.x1E;

	bbs_UInt32Arr_size( cpA, &ptrA->patchBufferE, ptrA->bitImageE.widthE );

	if( yoL == 0 )
	{
		bbs_memcpy32( ptrA->patchBufferE.arrPtrE + xbL, 
			          ptrA->bitImageE.arrE.arrPtrE + ybL * ptrA->bitImageE.widthE + xbL, 
					  wsrWidthL );
	}
	else if( ybL == ( int32 )ptrA->bitImageE.heightE - 1 )
	{
		uint32* dstL = ptrA->patchBufferE.arrPtrE + xbL;
		const uint32* srcL = ptrA->bitImageE.arrE.arrPtrE + ybL * ptrA->bitImageE.widthE + xbL;
		uint32 iL;
		for( iL = 0; iL < wsrWidthL; iL++ ) dstL[ iL ] = srcL[ iL ] >> yoL;
	}
	else
	{
		uint32* dstL = ptrA->patchBufferE.arrPtrE + xbL;
		const uint32* src0L = ptrA->bitImageE.arrE.arrPtrE + ybL * ptrA->bitImageE.widthE + xbL;
		const uint32* src1L = src0L + ptrA->bitImageE.widthE;
		uint32 iL;
		uint32 slL = 32 - yoL;
		for( iL = 0; iL < wsrWidthL; iL++ ) dstL[ iL ] = ( src0L[ iL ] >> yoL ) | ( src1L[ iL ] << slL );
	}
}

/* ------------------------------------------------------------------------- */

/* sets work scan region from original scan region according to scale exponent */
void bbf_LocalScanner_setWorkScanRegion( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA )
{
	int32 xMinL = ptrA->origScanRegionE.x1E >> ptrA->scaleExpE;
	int32 yMinL = ptrA->origScanRegionE.y1E >> ptrA->scaleExpE;
	int32 xMaxL = ptrA->origScanRegionE.x2E >> ptrA->scaleExpE;
	int32 yMaxL = ptrA->origScanRegionE.y2E >> ptrA->scaleExpE;
	ptrA->workScanRegionE.x1E = ( xMinL < 0 ) ? 0 : xMinL;
	ptrA->workScanRegionE.y1E = ( yMinL < 0 ) ? 0 : yMinL;
	ptrA->workScanRegionE.x2E = ( xMaxL > ( int32 )ptrA->currentWidthE ) ? ptrA->currentWidthE : xMaxL;
	ptrA->workScanRegionE.y2E = ( yMaxL > ( int32 )ptrA->currentHeightE ) ? ptrA->currentHeightE : yMaxL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_LocalScanner_init( struct bbs_Context* cpA,
					        struct bbf_LocalScanner* ptrA )
{
	ptrA->xE = 0;
	ptrA->yE = 0;
	ptrA->xOffE = 0;
	ptrA->yOffE = 0;
	ptrA->currentWidthE = 0;
	ptrA->currentHeightE = 0;
	ptrA->workWidthE = 0;
	ptrA->workHeightE = 0;
	ptrA->workImagePtrE = NULL;
	ptrA->origWidthE = 0;
	ptrA->origHeightE = 0;
	ptrA->origImagePtrE = NULL;
	bbf_BitParam_init( cpA, &ptrA->bitParamE );
	bbs_UInt8Arr_init( cpA, &ptrA->workImageBufferE );
	bim_UInt32Image_init( cpA, &ptrA->satE );
	bim_UInt32Image_init( cpA, &ptrA->bitImageE );
	bbs_UInt32Arr_init( cpA, &ptrA->patchBufferE );
	bts_Int16Rect_init( cpA, &ptrA->origScanRegionE );
	bts_Int16Rect_init( cpA, &ptrA->workScanRegionE );

	ptrA->patchWidthE = 0;
	ptrA->patchHeightE = 0;
	ptrA->scaleExpE = 0;
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
	ptrA->minScaleExpE = 0;
	ptrA->maxRadiusE = 0;
}

/* ------------------------------------------------------------------------- */

void bbf_LocalScanner_exit( struct bbs_Context* cpA,
				            struct bbf_LocalScanner* ptrA )
{
	ptrA->xE = 0;
	ptrA->yE = 0;
	ptrA->xOffE = 0;
	ptrA->yOffE = 0;
	ptrA->currentWidthE = 0;
	ptrA->currentHeightE = 0;
	ptrA->workWidthE = 0;
	ptrA->workHeightE = 0;
	ptrA->workImagePtrE = NULL;
	ptrA->origWidthE = 0;
	ptrA->origHeightE = 0;
	ptrA->origImagePtrE = NULL;
	bbf_BitParam_exit( cpA, &ptrA->bitParamE );
	bbs_UInt8Arr_exit( cpA, &ptrA->workImageBufferE );
	bim_UInt32Image_exit( cpA, &ptrA->satE );
	bim_UInt32Image_exit( cpA, &ptrA->bitImageE );
	bbs_UInt32Arr_exit( cpA, &ptrA->patchBufferE );
	bts_Int16Rect_exit( cpA, &ptrA->origScanRegionE );
	bts_Int16Rect_exit( cpA, &ptrA->workScanRegionE );

	ptrA->patchWidthE = 0;
	ptrA->patchHeightE = 0;
	ptrA->scaleExpE = 0;
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
	ptrA->minScaleExpE = 0;
	ptrA->maxRadiusE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_LocalScanner_copy( struct bbs_Context* cpA,
				            struct bbf_LocalScanner* ptrA, 
					        const struct bbf_LocalScanner* srcPtrA )
{
	bbs_ERROR0( "bbf_LocalScanner_copy:\n Function is not available" );
}

/* ------------------------------------------------------------------------- */

flag bbf_LocalScanner_equal( struct bbs_Context* cpA,
							 const struct bbf_LocalScanner* ptrA, 
							 const struct bbf_LocalScanner* srcPtrA )
{
	bbs_ERROR0( "bbf_LocalScanner_equal:\n Function is not available" );
	return FALSE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bbf_LocalScanner_positions( const struct bbf_LocalScanner* ptrA )
{ 
	int32 wL = ptrA->workScanRegionE.x2E - ptrA->workScanRegionE.x1E - ptrA->patchWidthE;
	int32 hL = ptrA->workScanRegionE.y2E - ptrA->workScanRegionE.y1E - ptrA->patchHeightE;
	return ( ( wL < 0 ) ? 0 : wL ) * ( ( hL < 0 ) ? 0 : hL );
}

/* ------------------------------------------------------------------------- */

uint32 bbf_LocalScanner_scanIndex( const struct bbf_LocalScanner* ptrA )
{
	int32 wL = ptrA->workScanRegionE.x2E - ptrA->workScanRegionE.x1E - ptrA->patchWidthE;
	return ( ptrA->yE - ptrA->workScanRegionE.y1E ) * wL + ( ptrA->xE - ptrA->workScanRegionE.x1E );
}

/* ------------------------------------------------------------------------- */

void bbf_LocalScanner_pos( const struct bbf_LocalScanner* ptrA, int32* xPtrA, int32* yPtrA )
{
	*xPtrA = ( ( ptrA->xE << ptrA->scaleExpE ) + ptrA->xOffE ) << 16;
	*yPtrA = ( ( ptrA->yE << ptrA->scaleExpE ) + ptrA->yOffE ) << 16;
}

/* ------------------------------------------------------------------------- */

void bbf_LocalScanner_idxPos( const struct bbf_LocalScanner* ptrA, uint32 scanIndexA, int32* xPtrA, int32* yPtrA )
{
	uint32 wL = ptrA->workScanRegionE.x2E - ptrA->workScanRegionE.x1E - ptrA->patchWidthE;
	int32 xL = ( scanIndexA % wL ) + ptrA->workScanRegionE.x1E;
	int32 yL = ( scanIndexA / wL ) + ptrA->workScanRegionE.y1E;
	*xPtrA = ( ( xL << ptrA->scaleExpE ) + ptrA->xOffE ) << 16;
	*yPtrA = ( ( yL << ptrA->scaleExpE ) + ptrA->yOffE ) << 16;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bbf_LocalScanner_create( struct bbs_Context* cpA,
							  struct bbf_LocalScanner* ptrA, 
							  uint32 patchWidthA,
							  uint32 patchHeightA,
							  uint32 scaleExpA,
							  uint32 maxImageWidthA,
							  uint32 maxImageHeightA,
							  uint32 minScaleExpA,
							  uint32 maxRadiusA,
							  struct bbs_MemTbl* mtpA )
{
	ptrA->patchWidthE = patchWidthA;
	ptrA->patchHeightE = patchHeightA;
	ptrA->scaleExpE = scaleExpA;
	ptrA->maxImageWidthE = maxImageWidthA;
	ptrA->maxImageHeightE = maxImageHeightA;
	ptrA->minScaleExpE = minScaleExpA;
	ptrA->maxRadiusE = maxRadiusA;
	bbf_LocalScanner_alloc( cpA, ptrA, mtpA );
}

/* ------------------------------------------------------------------------- */
	
void bbf_LocalScanner_bitParam( struct bbs_Context* cpA,
							    struct bbf_LocalScanner* ptrA,
								const struct bbf_BitParam* bitParamPtrA )
{
	if( !bbf_BitParam_equal( cpA, &ptrA->bitParamE, bitParamPtrA ) )
	{
		bbf_BitParam_copy( cpA, &ptrA->bitParamE, bitParamPtrA );
		bbf_LocalScanner_createBitImage( cpA, ptrA );
	}

	bbf_LocalScanner_resetScan( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */
	
void bbf_LocalScanner_origScanRegion( struct bbs_Context* cpA,
									  struct bbf_LocalScanner* ptrA,
									  const struct bts_Int16Rect* scanRegionPtrA )
{
	ptrA->origScanRegionE = *scanRegionPtrA;
	bbf_LocalScanner_setWorkScanRegion( cpA, ptrA );
	bbf_LocalScanner_resetScan( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bbf_LocalScanner_memSize( struct bbs_Context* cpA,
								 const struct bbf_LocalScanner* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bbs_SIZEOF16( ptrA->patchWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->patchHeightE );
	memSizeL += bbs_SIZEOF16( ptrA->scaleExpE );
	memSizeL += bbs_SIZEOF16( ptrA->maxImageWidthE );
	memSizeL += bbs_SIZEOF16( ptrA->maxImageHeightE );
	memSizeL += bbs_SIZEOF16( ptrA->minScaleExpE );
	memSizeL += bbs_SIZEOF16( ptrA->maxRadiusE );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_LocalScanner_memWrite( struct bbs_Context* cpA,
						     const struct bbf_LocalScanner* ptrA, 
						     uint16* memPtrA )
{
	uint32 memSizeL = bbf_LocalScanner_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bbf_LOCAL_SCANNER_VERSION, memPtrA );

	memPtrA += bbs_memWrite32( &ptrA->patchWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->patchHeightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->scaleExpE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxImageWidthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxImageHeightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->minScaleExpE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxRadiusE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bbf_LocalScanner_memRead( struct bbs_Context* cpA,
						    struct bbf_LocalScanner* ptrA, 
						    const uint16* memPtrA, 
						    struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;

	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bbf_LOCAL_SCANNER_VERSION, memPtrA );

	memPtrA += bbs_memRead32( &ptrA->patchWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->patchHeightE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->scaleExpE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxImageWidthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxImageHeightE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->minScaleExpE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxRadiusE, memPtrA );

	if( memSizeL != bbf_LocalScanner_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_LocalScanner_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
			        "size mismatch" );
		return 0;
	}

	if( bbs_Context_error( cpA ) ) return 0;

	/* allocate arrays */
	bbf_LocalScanner_alloc( cpA, ptrA, mtpA );

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

void bbf_LocalScanner_resetScan( struct bbs_Context* cpA,
								 struct bbf_LocalScanner* ptrA )
{
	ptrA->xE = ptrA->workScanRegionE.x1E;
	ptrA->yE = ptrA->workScanRegionE.y1E;
	bbf_LocalScanner_initPatchBuffer( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */

void bbf_LocalScanner_assign( struct bbs_Context* cpA,
							  struct bbf_LocalScanner* ptrA,
							  const uint8* imagePtrA, 
							  uint32 imageWidthA,
							  uint32 imageHeightA,
							  const struct bbf_BitParam* paramPtrA )
{
	if( ptrA->scaleExpE == 0 ) 
	{
		ptrA->workImagePtrE = imagePtrA;
		ptrA->workWidthE = imageWidthA;
		ptrA->workHeightE = imageHeightA;
	}
	else
	{
		ptrA->origImagePtrE = imagePtrA;
		ptrA->origWidthE = imageWidthA;
		ptrA->origHeightE = imageHeightA;
	}

	ptrA->bitParamE = *paramPtrA;
	ptrA->xOffE = 0;
	ptrA->yOffE = 0;
	ptrA->origScanRegionE = bts_Int16Rect_create( 0, 0, imageWidthA, imageHeightA );
	bbf_LocalScanner_downscale( cpA, ptrA );
	bbf_LocalScanner_createBitImage( cpA, ptrA );
	bbf_LocalScanner_resetScan( cpA, ptrA );
}

/* ------------------------------------------------------------------------- */

const uint32* bbf_LocalScanner_getPatch( const struct bbf_LocalScanner* ptrA )
{
	return ptrA->patchBufferE.arrPtrE + ptrA->xE;
}

/* ------------------------------------------------------------------------- */

flag bbf_LocalScanner_next( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA )
{
	if( ( ptrA->xE + 1 ) < ptrA->workScanRegionE.x2E - ( int32 )ptrA->patchWidthE )
	{
		ptrA->xE++;
		return TRUE;
	}

	if( ( ptrA->yE + 1 ) >= ptrA->workScanRegionE.y2E - ( int32 )ptrA->patchHeightE ) return FALSE;

	ptrA->xE = ptrA->workScanRegionE.x1E;
	ptrA->yE++;

	{
		uint32 offL = ( ptrA->yE & 0x1F );
		uint32 rowL = ( ptrA->yE >> 5 ) + ( offL > 0 ? 1 : 0 );

		uint32 widthL = ptrA->bitImageE.widthE;
		uint32 sizeL = ptrA->workScanRegionE.x2E - ptrA->workScanRegionE.x1E;
		uint32* dstL = ( uint32* )ptrA->patchBufferE.arrPtrE + ptrA->xE;
		uint32 iL;

		if( rowL < ptrA->bitImageE.heightE )
		{
			uint32* srcL = ptrA->bitImageE.arrE.arrPtrE + rowL * widthL + ptrA->xE;
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

void bbf_LocalScanner_goToXY( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA, int32 xA, int32 yA )
{
	bbs_DEF_fNameL( "void bbf_LocalScanner_goToXY( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA, int32 xA, int32 yA )" )
	if( xA < ptrA->workScanRegionE.x1E || xA >= ptrA->workScanRegionE.x2E - ( int32 )ptrA->patchWidthE )
	{
		bbs_ERROR1( "%s:\nxA out of range", fNameL );
		return;
	}
	ptrA->xE = xA;
	if( ptrA->yE == yA ) return;
	if( yA < ptrA->workScanRegionE.y1E || yA >= ptrA->workScanRegionE.y2E - ( int32 )ptrA->patchHeightE )
	{
		bbs_ERROR1( "%s:\nyA out of range", fNameL );
		return;
	}
	ptrA->yE = yA;

	{
		uint32 offL = ( ptrA->yE & 0x1F );
		uint32 rowL = ( ptrA->yE >> 5 ) + ( offL > 0 ? 1 : 0 );

		uint32 sizeL = ptrA->workScanRegionE.x2E - ptrA->workScanRegionE.x1E;
		uint32 imgWidthL = ptrA->bitImageE.widthE;
		uint32 imgOffsL = ptrA->workScanRegionE.x1E;
		uint32* dstL = ptrA->patchBufferE.arrPtrE + imgOffsL;
		uint32 iL;

		if( rowL < ptrA->bitImageE.heightE )
		{
			if( offL > 0 )
			{
				uint32* src1L = ptrA->bitImageE.arrE.arrPtrE + rowL * imgWidthL + imgOffsL;
				uint32* src0L = src1L - imgWidthL;
				uint32 shlL = 32 - offL;
				for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = ( src0L[ iL ] >> offL ) | ( src1L[ iL ] << shlL );
			}
			else
			{
				bbs_memcpy32( dstL, ptrA->bitImageE.arrE.arrPtrE + rowL * imgWidthL + imgOffsL, sizeL );
			}
		}
		else
		{
			uint32* srcL = ptrA->bitImageE.arrE.arrPtrE + ( rowL - 1 ) * imgWidthL + imgOffsL;
			for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = srcL[ iL ] >> offL;
		}
	}
}

/* ------------------------------------------------------------------------- */

void bbf_LocalScanner_goToIndex( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA, uint32 scanIndexA )
{
	uint32 wL = ptrA->workScanRegionE.x2E - ptrA->workScanRegionE.x1E - ptrA->patchWidthE;
	bbf_LocalScanner_goToXY( cpA, ptrA,
							 ( scanIndexA % wL ) + ptrA->workScanRegionE.x1E, 
							 ( scanIndexA / wL ) + ptrA->workScanRegionE.y1E ); 
}

/* ------------------------------------------------------------------------- */

flag bbf_LocalScanner_nextOffset( struct bbs_Context* cpA, struct bbf_LocalScanner* ptrA )
{
	int32 maxL = ( 1 << ptrA->scaleExpE );
	if( ptrA->yOffE == maxL ) return FALSE;

	ptrA->xOffE++;

	if( ptrA->xOffE == maxL )
	{
		ptrA->xOffE = 0;
		ptrA->yOffE++;
		if( ptrA->yOffE == maxL ) return FALSE;
	}

	bbf_LocalScanner_downscale( cpA, ptrA );
	bbf_LocalScanner_createBitImage( cpA, ptrA );
	bbf_LocalScanner_setWorkScanRegion( cpA, ptrA );
	bbf_LocalScanner_resetScan( cpA, ptrA );

	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
