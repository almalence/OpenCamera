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
#include "b_BitFeatureEm/I04Dns2x2Ftr.h"

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

void bbf_I04Dns2x2Ftr_init( struct bbs_Context* cpA,
						    struct bbf_I04Dns2x2Ftr* ptrA )
{
	bbf_Feature_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bbf_FT_I04_DNS_2X2_FTR;
	ptrA->baseE.vpActivityE = bbf_I04Dns2x2Ftr_activity;
	bbs_UInt32Arr_init( cpA, &ptrA->ptnArrE );
	bbs_Int16Arr_init( cpA, &ptrA->tableE );
	ptrA->activityFactorE = 0;
}

/* ------------------------------------------------------------------------- */

void bbf_I04Dns2x2Ftr_exit( struct bbs_Context* cpA,
						    struct bbf_I04Dns2x2Ftr* ptrA )
{
	bbf_Feature_exit( cpA, &ptrA->baseE );
	bbs_UInt32Arr_exit( cpA, &ptrA->ptnArrE );
	bbs_Int16Arr_exit( cpA, &ptrA->tableE );
	ptrA->activityFactorE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbf_I04Dns2x2Ftr_copy( struct bbs_Context* cpA,
						    struct bbf_I04Dns2x2Ftr* ptrA, 
						    const struct bbf_I04Dns2x2Ftr* srcPtrA )
{
	bbf_Feature_copy( cpA, &ptrA->baseE, &srcPtrA->baseE );
	bbs_UInt32Arr_copy( cpA, &ptrA->ptnArrE, &srcPtrA->ptnArrE );
	bbs_Int16Arr_copy( cpA, &ptrA->tableE, &srcPtrA->tableE );
	ptrA->activityFactorE = srcPtrA->activityFactorE;
}

/* ------------------------------------------------------------------------- */

flag bbf_I04Dns2x2Ftr_equal( struct bbs_Context* cpA,
						     const struct bbf_I04Dns2x2Ftr* ptrA, 
						     const struct bbf_I04Dns2x2Ftr* srcPtrA )
{
	if( !bbf_Feature_equal( cpA, &ptrA->baseE, &srcPtrA->baseE ) ) return FALSE;
	if( !bbs_UInt32Arr_equal( cpA, &ptrA->ptnArrE, &srcPtrA->ptnArrE ) ) return FALSE;
	if( !bbs_Int16Arr_equal( cpA, &ptrA->tableE, &srcPtrA->tableE ) ) return FALSE;
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
	
uint32 bbf_I04Dns2x2Ftr_memSize( struct bbs_Context* cpA,
							     const struct bbf_I04Dns2x2Ftr* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bbf_Feature_memSize( cpA, &ptrA->baseE );
	memSizeL += bbs_UInt32Arr_memSize( cpA, &ptrA->ptnArrE );
	memSizeL += bbs_Int16Arr_memSize( cpA, &ptrA->tableE );
	memSizeL += bbs_SIZEOF16( ptrA->activityFactorE );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bbf_I04Dns2x2Ftr_memWrite( struct bbs_Context* cpA,
							      const struct bbf_I04Dns2x2Ftr* ptrA, 
								  uint16* memPtrA )
{
	uint32 memSizeL = bbf_I04Dns2x2Ftr_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bbf_I04_DNS_2X2_FTR_VERSION, memPtrA );
	memPtrA += bbf_Feature_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_UInt32Arr_memWrite( cpA, &ptrA->ptnArrE, memPtrA );
	memPtrA += bbs_Int16Arr_memWrite( cpA, &ptrA->tableE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->activityFactorE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bbf_I04Dns2x2Ftr_memRead( struct bbs_Context* cpA,
							     struct bbf_I04Dns2x2Ftr* ptrA, 
							     const uint16* memPtrA, 
							     struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_fastestSegPtr( cpA, &memTblL, 0 );
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bbf_I04_DNS_2X2_FTR_VERSION, memPtrA );
	memPtrA += bbf_Feature_memRead( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bbs_UInt32Arr_memRead( cpA, &ptrA->ptnArrE, memPtrA, espL );
	memPtrA += bbs_Int16Arr_memRead( cpA, &ptrA->tableE, memPtrA, espL );
	memPtrA += bbs_memRead32( &ptrA->activityFactorE, memPtrA );
	if( memSizeL != bbf_I04Dns2x2Ftr_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbf_I04Dns2x2Ftr_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
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

int32 bbf_I04Dns2x2Ftr_activity( const struct bbf_Feature* ptrA, const uint32* patchA )
{
	const struct bbf_I04Dns2x2Ftr* ptrL = ( struct bbf_I04Dns2x2Ftr* )ptrA;

	uint32 iL;

	const uint32* ptnPtrL = ptrL->ptnArrE.arrPtrE;
	const int16*  tableL  = ptrL->tableE.arrPtrE;
	const uint32* patchL = patchA;

	int32 sumL = 0;

	uint32 wL = ptrL->baseE.patchWidthE - 1;
	uint32 hL = ptrL->baseE.patchHeightE - 1;

	uint32 borderMaskL = ( ( uint32 )1 << hL ) - 1;

	uint32 blocksL = ptrL->baseE.patchHeightE >> 2; /* number of 4 bit blocks */

	if( hL == 31 )
	{
		for( iL = 0; iL < wL; iL++ )
		{
			uint32 vL = (   patchL[ 0 ]        ^ ptnPtrL[ 0 ] ) & 
					    ( ( patchL[ 0 ] >> 1 ) ^ ptnPtrL[ 1 ] ) &
					    ( ( patchL[ 1 ]      ) ^ ptnPtrL[ 2 ] ) &
					    ( ( patchL[ 1 ] >> 1 ) ^ ptnPtrL[ 3 ] ) & borderMaskL;

			patchL++;

			ptnPtrL += 4;

			sumL += tableL[         ( vL       ) & 0x0F   ];
			sumL += tableL[  16 + ( ( vL >>  4 ) & 0x0F ) ];
			sumL += tableL[  32 + ( ( vL >>  8 ) & 0x0F ) ];
			sumL += tableL[  48 + ( ( vL >> 12 ) & 0x0F ) ];
			sumL += tableL[  64 + ( ( vL >> 16 ) & 0x0F ) ];
			sumL += tableL[  80 + ( ( vL >> 20 ) & 0x0F ) ];
			sumL += tableL[  96 + ( ( vL >> 24 ) & 0x0F ) ];
			sumL += tableL[ 112 + ( ( vL >> 28 ) & 0x0F ) ];
			tableL += 128;
		}
	}
	else
	{
		for( iL = 0; iL < wL; iL++ )
		{
			uint32 jL;
			uint32 vL = (   patchL[ 0 ]        ^ ptnPtrL[ 0 ] ) & 
					    ( ( patchL[ 0 ] >> 1 ) ^ ptnPtrL[ 1 ] ) &
					    ( ( patchL[ 1 ]      ) ^ ptnPtrL[ 2 ] ) &
					    ( ( patchL[ 1 ] >> 1 ) ^ ptnPtrL[ 3 ] ) & borderMaskL;

			patchL++;
			ptnPtrL += 4;

			for( jL = 0; jL < blocksL; jL++ )
			{
				sumL += tableL[ vL & 0x0F ];
				vL >>= 4;
				tableL += 16;
			}
		}
	}

	return sumL * ( ptrL->activityFactorE >> 8 ) + ( ( sumL * ( int32 )( ptrL->activityFactorE & 0x0FF ) ) >> 8 );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

