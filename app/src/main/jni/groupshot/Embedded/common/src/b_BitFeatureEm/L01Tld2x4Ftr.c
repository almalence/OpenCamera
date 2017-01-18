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
#include "b_BitFeatureEm/L01Tld2x4Ftr.h"

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

void bbf_L01Tld2x4Ftr_init( struct bbs_Context* cpA,
						    struct bbf_L01Tld2x4Ftr* ptrA )
{
	bbf_Feature_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bbf_FT_L01_TLD_2X4_FTR;
	ptrA->baseE.vpActivityE = bbf_L01Tld2x4Ftr_activity;
	bbs_UInt32Arr_init( cpA, &ptrA->dataArrE );
	ptrA->activityFactorE = 0;
}

/* ------------------------------------------------------------------------- */

void bbf_L01Tld2x4Ftr_exit( struct bbs_Context* cpA,
						    struct bbf_L01Tld2x4Ftr* ptrA )
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

void bbf_L01Tld2x4Ftr_copy( struct bbs_Context* cpA,
						    struct bbf_L01Tld2x4Ftr* ptrA, 
						    const struct bbf_L01Tld2x4Ftr* srcPtrA )
{
	bbf_Feature_copy( cpA, &ptrA->baseE, &srcPtrA->baseE );
	bbs_UInt32Arr_copy( cpA, &ptrA->dataArrE, &srcPtrA->dataArrE );
	ptrA->activityFactorE = srcPtrA->activityFactorE;
}

/* ------------------------------------------------------------------------- */

flag bbf_L01Tld2x4Ftr_equal( struct bbs_Context* cpA,
						     const struct bbf_L01Tld2x4Ftr* ptrA, 
						     const struct bbf_L01Tld2x4Ftr* srcPtrA )
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
	
uint32 bbf_L01Tld2x4Ftr_memSize( struct bbs_Context* cpA,
							     const struct bbf_L01Tld2x4Ftr* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bbf_Feature_memSize( cpA, &ptrA->baseE );
	memSizeL += bbs_UInt32Arr_memSize( cpA, &ptrA->dataArrE );
	memSizeL += bbs_SIZEOF16( ptrA->activityFactorE );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_L01Tld2x4Ftr_memWrite( struct bbs_Context* cpA,
							      const struct bbf_L01Tld2x4Ftr* ptrA, 
								  uint16* memPtrA )
{
	uint32 memSizeL = bbf_L01Tld2x4Ftr_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bbf_L01_TLD_2X4_FTR_VERSION, memPtrA );
	memPtrA += bbf_Feature_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_UInt32Arr_memWrite( cpA, &ptrA->dataArrE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->activityFactorE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bbf_L01Tld2x4Ftr_memRead( struct bbs_Context* cpA,
							     struct bbf_L01Tld2x4Ftr* ptrA, 
							     const uint16* memPtrA, 
							     struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_fastestSegPtr( cpA, &memTblL, 0 );
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bbf_L01_TLD_2X4_FTR_VERSION, memPtrA );
	memPtrA += bbf_Feature_memRead( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_UInt32Arr_memRead( cpA, &ptrA->dataArrE, memPtrA, espL );
	memPtrA += bbs_memRead32( &ptrA->activityFactorE, memPtrA );
	if( memSizeL != bbf_L01Tld2x4Ftr_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_L01Tld2x4Ftr_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
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

int32 bbf_L01Tld2x4Ftr_activity( const struct bbf_Feature* ptrA, const uint32* patchA )
{
	const struct bbf_L01Tld2x4Ftr* ptrL = ( struct bbf_L01Tld2x4Ftr* )ptrA;

	const uint32* dataPtrL = ptrL->dataArrE.arrPtrE;
	const uint32* patchL = patchA;

	uint32 iL;

	uint32 bs1L = 0;
	uint32 bs2L = 0;

	for( iL = ptrL->baseE.patchWidthE >> 3; iL > 0; iL-- )
	{
		uint32 vL = ~dataPtrL[ 0 ];

		/* compare with pattern */
		uint32 s1L = patchL[ 0 ] ^ dataPtrL[ 1 ];
		uint32 s2L = patchL[ 1 ] ^ dataPtrL[ 2 ];

		/* bit count */
		s1L = ( s1L & 0x55555555 ) + ( ( s1L >> 1 ) & 0x55555555 );
		s1L = ( s1L & 0x33333333 ) + ( ( s1L >> 2 ) & 0x33333333 );
		s2L = ( s2L & 0x55555555 ) + ( ( s2L >> 1 ) & 0x55555555 );
		s2L = ( s2L & 0x33333333 ) + ( ( s2L >> 2 ) & 0x33333333 );

		/* compare with threshold and store results in vL */
		vL |= ( ( s1L + s2L + dataPtrL[ 3 ] ) & 0x88888888 ) >> 3;

		/* compare with pattern */
		s1L = patchL[ 2 ] ^ dataPtrL[ 4 ];
		s2L = patchL[ 3 ] ^ dataPtrL[ 5 ];

		/* bit count */
		s1L = ( s1L & 0x55555555 ) + ( ( s1L >> 1 ) & 0x55555555 );
		s1L = ( s1L & 0x33333333 ) + ( ( s1L >> 2 ) & 0x33333333 );
		s2L = ( s2L & 0x55555555 ) + ( ( s2L >> 1 ) & 0x55555555 );
		s2L = ( s2L & 0x33333333 ) + ( ( s2L >> 2 ) & 0x33333333 );

		/* compare with threshold and store results in vL */
		vL |= ( ( s1L + s2L + dataPtrL[ 6 ] ) & 0x88888888 ) >> 2;

		/* compare with pattern */
		s1L = patchL[ 4 ] ^ dataPtrL[ 7 ];
		s2L = patchL[ 5 ] ^ dataPtrL[ 8 ];

		/* bit count */
		s1L = ( s1L & 0x55555555 ) + ( ( s1L >> 1 ) & 0x55555555 );
		s1L = ( s1L & 0x33333333 ) + ( ( s1L >> 2 ) & 0x33333333 );
		s2L = ( s2L & 0x55555555 ) + ( ( s2L >> 1 ) & 0x55555555 );
		s2L = ( s2L & 0x33333333 ) + ( ( s2L >> 2 ) & 0x33333333 );

		/* compare with threshold and store results in vL */
		vL |= ( ( s1L + s2L + dataPtrL[ 9 ] ) & 0x88888888 ) >> 1;

		/* compare with pattern */
		s1L = patchL[ 6 ] ^ dataPtrL[ 10 ];
		s2L = patchL[ 7 ] ^ dataPtrL[ 11 ];

		/* bit count */
		s1L = ( s1L & 0x55555555 ) + ( ( s1L >> 1 ) & 0x55555555 );
		s1L = ( s1L & 0x33333333 ) + ( ( s1L >> 2 ) & 0x33333333 );
		s2L = ( s2L & 0x55555555 ) + ( ( s2L >> 1 ) & 0x55555555 );
		s2L = ( s2L & 0x33333333 ) + ( ( s2L >> 2 ) & 0x33333333 );

		/* compare with threshold and store results in vL */
		vL |= ( ( s1L + s2L + dataPtrL[ 12 ] ) & 0x88888888 );

		vL = ~vL;

		/* add bits */
		bs1L +=    vL        & 0x55555555;
		bs2L +=  ( vL >> 1 ) & 0x55555555;

		dataPtrL += 13;
		patchL  += 8;
	}

	/* complete partial sums and compute final confidence */
	bs1L = ( bs1L & 0x33333333 ) + ( ( bs1L >> 2 ) & 0x33333333 ) + ( bs2L & 0x33333333 ) + ( ( bs2L >> 2 ) & 0x33333333 );
	bs1L = ( bs1L & 0x0F0F0F0F ) + ( ( bs1L >> 4 ) & 0x0F0F0F0F );
	bs1L += bs1L >> 8;

	/* compute final activity */
	{
		uint32 actL =  ( bs1L + ( bs1L >> 16 ) ) & 0x0FF;
		return actL * ptrL->activityFactorE;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

