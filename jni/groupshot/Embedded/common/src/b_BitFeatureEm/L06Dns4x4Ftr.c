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
#include "b_BitFeatureEm/L06Dns4x4Ftr.h"

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

void bbf_L06Dns4x4Ftr_init( struct bbs_Context* cpA,
						    struct bbf_L06Dns4x4Ftr* ptrA )
{
	bbf_Feature_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bbf_FT_L06_DNS_4X4_FTR;
	ptrA->baseE.vpActivityE = bbf_L06Dns4x4Ftr_activity;
	bbs_UInt32Arr_init( cpA, &ptrA->dataArrE );
	ptrA->activityFactorE = 0;
}

/* ------------------------------------------------------------------------- */

void bbf_L06Dns4x4Ftr_exit( struct bbs_Context* cpA,
						    struct bbf_L06Dns4x4Ftr* ptrA )
{
	bbf_Feature_exit( cpA, &ptrA->baseE );
	bbs_UInt32Arr_exit( cpA, &ptrA->dataArrE );
	ptrA->activityFactorE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_L06Dns4x4Ftr_copy( struct bbs_Context* cpA,
						    struct bbf_L06Dns4x4Ftr* ptrA, 
						    const struct bbf_L06Dns4x4Ftr* srcPtrA )
{
	bbf_Feature_copy( cpA, &ptrA->baseE, &srcPtrA->baseE );
	bbs_UInt32Arr_copy( cpA, &ptrA->dataArrE, &srcPtrA->dataArrE );
	ptrA->activityFactorE = srcPtrA->activityFactorE;
}

/* ------------------------------------------------------------------------- */

flag bbf_L06Dns4x4Ftr_equal( struct bbs_Context* cpA,
						     const struct bbf_L06Dns4x4Ftr* ptrA, 
						     const struct bbf_L06Dns4x4Ftr* srcPtrA )
{
	if( !bbf_Feature_equal( cpA, &ptrA->baseE, &srcPtrA->baseE ) ) return FALSE;
	if( !bbs_UInt32Arr_equal( cpA, &ptrA->dataArrE, &srcPtrA->dataArrE ) ) return FALSE;
	if( ptrA->activityFactorE != srcPtrA->activityFactorE ) return FALSE;
	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bbf_L06Dns4x4Ftr_memSize( struct bbs_Context* cpA,
							     const struct bbf_L06Dns4x4Ftr* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bbf_Feature_memSize( cpA, &ptrA->baseE );
	memSizeL += bbs_UInt32Arr_memSize( cpA, &ptrA->dataArrE );
	memSizeL += bbs_SIZEOF16( ptrA->activityFactorE );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_L06Dns4x4Ftr_memWrite( struct bbs_Context* cpA,
							      const struct bbf_L06Dns4x4Ftr* ptrA, 
								  uint16* memPtrA )
{
	uint32 memSizeL = bbf_L06Dns4x4Ftr_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bbf_L06_DNS_4X4_FTR_VERSION, memPtrA );
	memPtrA += bbf_Feature_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_UInt32Arr_memWrite( cpA, &ptrA->dataArrE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->activityFactorE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bbf_L06Dns4x4Ftr_memRead( struct bbs_Context* cpA,
							     struct bbf_L06Dns4x4Ftr* ptrA, 
							     const uint16* memPtrA, 
							     struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_fastestSegPtr( cpA, &memTblL, 0 );
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bbf_L06_DNS_4X4_FTR_VERSION, memPtrA );
	memPtrA += bbf_Feature_memRead( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_UInt32Arr_memRead( cpA, &ptrA->dataArrE, memPtrA, espL );
	memPtrA += bbs_memRead32( &ptrA->activityFactorE, memPtrA );
	if( memSizeL != bbf_L06Dns4x4Ftr_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_L06Dns4x4Ftr_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
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

int32 bbf_L06Dns4x4Ftr_activity( const struct bbf_Feature* ptrA, const uint32* patchA )
{
	const struct bbf_L06Dns4x4Ftr* ptrL = ( struct bbf_L06Dns4x4Ftr* )ptrA;

	uint32 wL = ptrL->baseE.patchWidthE - 3;
    uint32 hL = ptrL->baseE.patchHeightE - 3;
	const uint32* dataPtrL = ptrL->dataArrE.arrPtrE;
	uint32 iL;

	uint32 borderMaskL = ( ( uint32 )1 << hL ) - 1;

	uint32 sL[ 16 ];
	uint32 bL[ 6 ] = { 0, 0, 0, 0, 0, 0 }; /* bit sum */

	for( iL = 0; iL < wL; iL++ )
	{
		uint32 vL, mL, tL; /* bit sum and thresholds */

		uint32 s1L = patchA[ iL     ];
		uint32 s2L = patchA[ iL + 1 ];
		uint32 s3L = patchA[ iL + 2 ];
		uint32 s4L = patchA[ iL + 3 ];

		/* comparison of pixels with patchHeightE - 3 features */
		sL[  0 ] = ( ( s1L      ) ^ dataPtrL[  0 ] ) & borderMaskL;
		sL[  1 ] = ( ( s1L >> 1 ) ^ dataPtrL[  1 ] ) & borderMaskL;
		sL[  2 ] = ( ( s1L >> 2 ) ^ dataPtrL[  2 ] ) & borderMaskL;
		sL[  3 ] = ( ( s1L >> 3 ) ^ dataPtrL[  3 ] ) & borderMaskL;

		sL[  4 ] = ( ( s2L      ) ^ dataPtrL[  4 ] ) & borderMaskL;
		sL[  5 ] = ( ( s2L >> 1 ) ^ dataPtrL[  5 ] ) & borderMaskL;
		sL[  6 ] = ( ( s2L >> 2 ) ^ dataPtrL[  6 ] ) & borderMaskL;
		sL[  7 ] = ( ( s2L >> 3 ) ^ dataPtrL[  7 ] ) & borderMaskL;

		sL[  8 ] = ( ( s3L      ) ^ dataPtrL[  8 ] ) & borderMaskL;
		sL[  9 ] = ( ( s3L >> 1 ) ^ dataPtrL[  9 ] ) & borderMaskL;
		sL[ 10 ] = ( ( s3L >> 2 ) ^ dataPtrL[ 10 ] ) & borderMaskL;
		sL[ 11 ] = ( ( s3L >> 3 ) ^ dataPtrL[ 11 ] ) & borderMaskL;

		sL[ 12 ] = ( ( s4L      ) ^ dataPtrL[ 12 ] ) & borderMaskL;
		sL[ 13 ] = ( ( s4L >> 1 ) ^ dataPtrL[ 13 ] ) & borderMaskL;
		sL[ 14 ] = ( ( s4L >> 2 ) ^ dataPtrL[ 14 ] ) & borderMaskL;
		sL[ 15 ] = ( ( s4L >> 3 ) ^ dataPtrL[ 15 ] ) & borderMaskL;

		/* parallel bit counting of patchHeightE - 2 features */

		vL = 0;

		mL = ( ( sL[  0 ] & 0x11111111 ) + ( sL[  1 ] & 0x11111111 ) + ( sL[  2 ] & 0x11111111 ) + 
		       ( sL[  3 ] & 0x11111111 ) + ( sL[  4 ] & 0x11111111 ) + ( sL[  5 ] & 0x11111111 ) + 
		  	   ( sL[  6 ] & 0x11111111 ) + ( sL[  7 ] & 0x11111111 ) + ( sL[  8 ] & 0x11111111 ) +
		  	   ( sL[  9 ] & 0x11111111 ) + ( sL[ 10 ] & 0x11111111 ) + ( sL[ 11 ] & 0x11111111 ) +
		  	   ( sL[ 12 ] & 0x11111111 ) + ( sL[ 13 ] & 0x11111111 ) + ( sL[ 14 ] & 0x11111111 ) );

		tL = dataPtrL[ 16 ];

		/* compare with thresholds and store results in vL */
		vL |= ( ( (   mL        & 0x0F0F0F0F ) + (   tL        & 0x0F0F0F0F ) + (   sL[ 15 ]        & 0x01010101 ) ) & 0x10101010 ) >> 4;
		vL |= ( ( ( ( mL >> 4 ) & 0x0F0F0F0F ) + ( ( tL >> 4 ) & 0x0F0F0F0F ) + ( ( sL[ 15 ] >> 4 ) & 0x01010101 ) ) & 0x10101010 );

		mL = ( ( sL[  0 ] & 0x02222222 ) + ( sL[  1 ] & 0x02222222 ) + ( sL[  2 ] & 0x02222222 ) + 
		       ( sL[  3 ] & 0x02222222 ) + ( sL[  4 ] & 0x02222222 ) + ( sL[  5 ] & 0x02222222 ) + 
		  	   ( sL[  6 ] & 0x02222222 ) + ( sL[  7 ] & 0x02222222 ) + ( sL[  8 ] & 0x02222222 ) +
		  	   ( sL[  9 ] & 0x02222222 ) + ( sL[ 10 ] & 0x02222222 ) + ( sL[ 11 ] & 0x02222222 ) +
		  	   ( sL[ 12 ] & 0x02222222 ) + ( sL[ 13 ] & 0x02222222 ) + ( sL[ 14 ] & 0x02222222 ) ) >> 1;

		sL[ 15 ] >>= 1;
		tL = dataPtrL[ 17 ];

		/* compare with thresholds and store results in vL */
		vL |= ( ( (   mL        & 0x0F0F0F0F ) + (   tL        & 0x0F0F0F0F ) + (   sL[ 15 ]        & 0x01010101 ) ) & 0x10101010 ) >> 3;
		vL |= ( ( ( ( mL >> 4 ) & 0x0F0F0F0F ) + ( ( tL >> 4 ) & 0x0F0F0F0F ) + ( ( sL[ 15 ] >> 4 ) & 0x01010101 ) ) & 0x10101010 ) << 1;

		mL = ( ( sL[  0 ] & 0x04444444 ) + ( sL[  1 ] & 0x04444444 ) + ( sL[  2 ] & 0x04444444 ) + 
		       ( sL[  3 ] & 0x04444444 ) + ( sL[  4 ] & 0x04444444 ) + ( sL[  5 ] & 0x04444444 ) + 
		  	   ( sL[  6 ] & 0x04444444 ) + ( sL[  7 ] & 0x04444444 ) + ( sL[  8 ] & 0x04444444 ) +
		  	   ( sL[  9 ] & 0x04444444 ) + ( sL[ 10 ] & 0x04444444 ) + ( sL[ 11 ] & 0x04444444 ) +
		  	   ( sL[ 12 ] & 0x04444444 ) + ( sL[ 13 ] & 0x04444444 ) + ( sL[ 14 ] & 0x04444444 ) ) >> 2;

		sL[ 15 ] >>= 1;
		tL = dataPtrL[ 18 ];

		/* compare with thresholds and store results in vL */
		vL |= ( ( (   mL        & 0x0F0F0F0F ) + (   tL        & 0x0F0F0F0F ) + (   sL[ 15 ]        & 0x01010101 ) ) & 0x10101010 ) >> 2;
		vL |= ( ( ( ( mL >> 4 ) & 0x0F0F0F0F ) + ( ( tL >> 4 ) & 0x0F0F0F0F ) + ( ( sL[ 15 ] >> 4 ) & 0x01010101 ) ) & 0x10101010 ) << 2;

		mL = ( ( sL[  0 ] & 0x08888888 ) + ( sL[  1 ] & 0x08888888 ) + ( sL[  2 ] & 0x08888888 ) + 
		       ( sL[  3 ] & 0x08888888 ) + ( sL[  4 ] & 0x08888888 ) + ( sL[  5 ] & 0x08888888 ) + 
		  	   ( sL[  6 ] & 0x08888888 ) + ( sL[  7 ] & 0x08888888 ) + ( sL[  8 ] & 0x08888888 ) +
		  	   ( sL[  9 ] & 0x08888888 ) + ( sL[ 10 ] & 0x08888888 ) + ( sL[ 11 ] & 0x08888888 ) +
		  	   ( sL[ 12 ] & 0x08888888 ) + ( sL[ 13 ] & 0x08888888 ) + ( sL[ 14 ] & 0x08888888 ) ) >> 3;

		sL[ 15 ] >>= 1;
		tL = dataPtrL[ 19 ];

		/* compare with thresholds and store results in vL */
		vL |= ( ( (   mL        & 0x0F0F0F0F ) + (   tL        & 0x0F0F0F0F ) + (   sL[ 15 ]        & 0x01010101 ) ) & 0x10101010 ) >> 1;
		vL |= ( ( ( ( mL >> 4 ) & 0x0F0F0F0F ) + ( ( tL >> 4 ) & 0x0F0F0F0F ) + ( ( sL[ 15 ] >> 4 ) & 0x01010101 ) ) & 0x10101010 ) << 3;

		vL = ~vL;

		/* mask out and count bits */
		{
			uint32 vmL;
			vmL = vL & dataPtrL[ 20 ];
			bL[ 0 ] += bbf_BIT_SUM_32( vmL );
			vmL = vL & dataPtrL[ 21 ];
			bL[ 1 ] += bbf_BIT_SUM_32( vmL);
			vmL = vL & dataPtrL[ 22 ];
			bL[ 2 ] += bbf_BIT_SUM_32( vmL );
			vmL = vL & dataPtrL[ 23 ];
			bL[ 3 ] += bbf_BIT_SUM_32( vmL );
			vmL = vL & dataPtrL[ 24 ];
			bL[ 4 ] += bbf_BIT_SUM_32( vmL );
			vmL = vL & dataPtrL[ 25 ];
			bL[ 5 ] += bbf_BIT_SUM_32( vmL );
		}

		dataPtrL += 26;
	}

	/* compute final activity */
	{
		uint32 actL = ( ( bL[ 0 ] << 5 ) + ( bL[ 1 ] << 4 ) + ( bL[ 2 ] << 3 ) + 
				        ( bL[ 3 ] << 2 ) + ( bL[ 4 ] << 1 ) + ( bL[ 5 ]      ) );

		return actL * ptrL->activityFactorE;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

