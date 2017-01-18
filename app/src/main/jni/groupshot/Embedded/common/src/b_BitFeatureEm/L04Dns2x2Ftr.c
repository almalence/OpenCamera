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
#include "b_BitFeatureEm/L04Dns2x2Ftr.h"

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

void bbf_L04Dns2x2Ftr_init( struct bbs_Context* cpA,
						    struct bbf_L04Dns2x2Ftr* ptrA )
{
	bbf_Feature_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bbf_FT_L04_DNS_2X2_FTR;
	ptrA->baseE.vpActivityE = bbf_L04Dns2x2Ftr_activity;
	bbs_UInt32Arr_init( cpA, &ptrA->dataArrE );
	ptrA->wShiftE = 0;
	ptrA->activityFactorE = 0;
}

/* ------------------------------------------------------------------------- */

void bbf_L04Dns2x2Ftr_exit( struct bbs_Context* cpA,
						    struct bbf_L04Dns2x2Ftr* ptrA )
{
	bbf_Feature_exit( cpA, &ptrA->baseE );
	bbs_UInt32Arr_exit( cpA, &ptrA->dataArrE );
	ptrA->wShiftE = 0;
	ptrA->activityFactorE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_L04Dns2x2Ftr_copy( struct bbs_Context* cpA,
						    struct bbf_L04Dns2x2Ftr* ptrA, 
						    const struct bbf_L04Dns2x2Ftr* srcPtrA )
{
	bbf_Feature_copy( cpA, &ptrA->baseE, &srcPtrA->baseE );
	bbs_UInt32Arr_copy( cpA, &ptrA->dataArrE, &srcPtrA->dataArrE );
	ptrA->wShiftE = srcPtrA->wShiftE;
	ptrA->activityFactorE = srcPtrA->activityFactorE;
}

/* ------------------------------------------------------------------------- */

flag bbf_L04Dns2x2Ftr_equal( struct bbs_Context* cpA,
						     const struct bbf_L04Dns2x2Ftr* ptrA, 
						     const struct bbf_L04Dns2x2Ftr* srcPtrA )
{
	if( !bbf_Feature_equal( cpA, &ptrA->baseE, &srcPtrA->baseE ) ) return FALSE;
	if( !bbs_UInt32Arr_equal( cpA, &ptrA->dataArrE, &srcPtrA->dataArrE ) ) return FALSE;
	if( ptrA->wShiftE != srcPtrA->wShiftE ) return FALSE;
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
	
uint32 bbf_L04Dns2x2Ftr_memSize( struct bbs_Context* cpA,
							     const struct bbf_L04Dns2x2Ftr* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bbf_Feature_memSize( cpA, &ptrA->baseE );
	memSizeL += bbs_UInt32Arr_memSize( cpA, &ptrA->dataArrE );
	memSizeL += bbs_SIZEOF16( ptrA->wShiftE );
	memSizeL += bbs_SIZEOF16( ptrA->activityFactorE );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_L04Dns2x2Ftr_memWrite( struct bbs_Context* cpA,
							      const struct bbf_L04Dns2x2Ftr* ptrA, 
								  uint16* memPtrA )
{
	uint32 memSizeL = bbf_L04Dns2x2Ftr_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bbf_L04_DNS_2X2_FTR_VERSION, memPtrA );
	memPtrA += bbf_Feature_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_UInt32Arr_memWrite( cpA, &ptrA->dataArrE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->wShiftE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->activityFactorE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bbf_L04Dns2x2Ftr_memRead( struct bbs_Context* cpA,
							     struct bbf_L04Dns2x2Ftr* ptrA, 
							     const uint16* memPtrA, 
							     struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_fastestSegPtr( cpA, &memTblL, 0 );
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bbf_L04_DNS_2X2_FTR_VERSION, memPtrA );
	memPtrA += bbf_Feature_memRead( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_UInt32Arr_memRead( cpA, &ptrA->dataArrE, memPtrA, espL );
	memPtrA += bbs_memRead32( &ptrA->wShiftE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->activityFactorE, memPtrA );
	if( memSizeL != bbf_L04Dns2x2Ftr_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_L04Dns2x2Ftr_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
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

int32 bbf_L04Dns2x2Ftr_activity( const struct bbf_Feature* ptrA, const uint32* patchA )
{
	const struct bbf_L04Dns2x2Ftr* ptrL = ( struct bbf_L04Dns2x2Ftr* )ptrA;

	uint32 wL = ptrL->baseE.patchWidthE - 1;
    uint32 hL = ptrL->baseE.patchHeightE - 1;

	const uint32* dataPtrL = ptrL->dataArrE.arrPtrE;
	uint32 iL;

	uint32 borderMaskL = ( ( uint32 )1 << hL ) - 1;
	const uint32* patchL = patchA;
	int32 sumL = 0;

	uint32 bL[ 4 ] = { 0, 0, 0, 0 }; /* bit sum */

	for( iL = 0; iL < wL; iL++ )
	{
		uint32 vL = (   patchL[ 0 ]        ^ dataPtrL[ 0 ] ) & 
				    ( ( patchL[ 0 ] >> 1 ) ^ dataPtrL[ 1 ] ) &
				    ( ( patchL[ 1 ]      ) ^ dataPtrL[ 2 ] ) &
				    ( ( patchL[ 1 ] >> 1 ) ^ dataPtrL[ 3 ] ) & borderMaskL;


		{
			uint32 vmL;
			vmL = vL & dataPtrL[ 4 ];
			bL[ 0 ] += bbf_BIT_SUM_32( vmL );
			vmL = vL & dataPtrL[ 5 ];
			bL[ 1 ] += bbf_BIT_SUM_32( vmL);
			vmL = vL & dataPtrL[ 6 ];
			bL[ 2 ] += bbf_BIT_SUM_32( vmL );
			vmL = vL & dataPtrL[ 7 ];
			bL[ 3 ] += bbf_BIT_SUM_32( vmL );
		}

		sumL += bbf_BIT_SUM_32( vL );

		patchL++;
		dataPtrL += 8;
	}

	/* compute final activity */
	{
		uint32 actL = ( ( bL[ 0 ] << 3 ) + ( bL[ 1 ] << 2 ) + ( bL[ 2 ] << 1 ) + bL[ 3 ] );
		return actL * ptrL->activityFactorE + sumL * ptrL->wShiftE;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

