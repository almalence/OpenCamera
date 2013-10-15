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

#include "b_TensorEm/CompactMat.h"
#include "b_TensorEm/Functions.h"
#include "b_BasicEm/Math.h"
#include "b_BasicEm/Functions.h"
#include "b_BasicEm/Memory.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/** Returns dot product of inVec with indexed row 
    The result is a floating point expresstion:
		upper 16 bit: signed value
		lower 16 bit: signed exponent
 */
int32 bts_CompactMat_fltDotPrdRow( struct bbs_Context* cpA, 
								   struct bts_CompactMat* ptrA, 
							       const int16* inVecA,
							       uint32 inNormBitsA,
							       uint32 rowA )
{
	const int16* rowPtrL = ptrA->cpsArrE.arrPtrE + ptrA->wordsPerRowE * rowA;

	/* extract row-header info */
	uint32 offsL = *rowPtrL++;
	uint32 sizeL = *rowPtrL++;
	int32 factorManL = *rowPtrL++;
	int32 factorExpL = *rowPtrL++;
	uint32 rowNormBitsL = *rowPtrL++;

	/* consider possible overflow */
	uint16 overflowBitsL = ( inNormBitsA + rowNormBitsL >= 31 ) ? inNormBitsA + rowNormBitsL - 31 : 0;

	const int16* inPtrL = inVecA + offsL;

	count_t iL;
	int32 sumL = 0;

	if( overflowBitsL == 0 ) /* raw dot product fits in int32 */
	{
		switch( ptrA->bitsPerValueE )
		{
			case 16:
			{
				for( iL = sizeL; iL > 0; iL-- ) sumL += ( ( int32 )*rowPtrL++ * ( int32 )*inPtrL++ );
			}
			break;

			#ifndef HW_TMS320C5x /* platforms that don't have int8 must use the 'default' implementation */

			case 8:
			{
				const uint16* dpL = ( uint16* )rowPtrL;
				for( iL = sizeL; iL >= 8; iL -= 8 )
				{
					sumL += ( ( int8 )  dpL[ 0 ]         * ( int32 )inPtrL[ 0 ] );
					sumL += ( ( int8 )( dpL[ 0 ] >>  8 ) * ( int32 )inPtrL[ 1 ] );
					sumL += ( ( int8 )  dpL[ 1 ]         * ( int32 )inPtrL[ 2 ] );
					sumL += ( ( int8 )( dpL[ 1 ] >>  8 ) * ( int32 )inPtrL[ 3 ] );
					sumL += ( ( int8 )  dpL[ 2 ]         * ( int32 )inPtrL[ 4 ] );
					sumL += ( ( int8 )( dpL[ 2 ] >>  8 ) * ( int32 )inPtrL[ 5 ] );
					sumL += ( ( int8 )  dpL[ 3 ]         * ( int32 )inPtrL[ 6 ] );
					sumL += ( ( int8 )( dpL[ 3 ] >>  8 ) * ( int32 )inPtrL[ 7 ] );
					dpL += 4;
					inPtrL += 8;
				}
				for( ; iL >= 2; iL -= 2 )
				{
					sumL += ( ( int8 )  *dpL         * ( int32 )inPtrL[ 0 ] );
					sumL += ( ( int8 )( *dpL >>  8 ) * ( int32 )inPtrL[ 1 ] );
					dpL++;
					inPtrL += 2;
				}
				if( iL > 0 )
				{
					sumL += ( ( int8 )*dpL++ * ( int32 )inPtrL[ 0 ] );
				}
			}
			break;

			case 6:
			{
				const uint16* dpL = ( uint16* )rowPtrL;
				for( iL = sizeL; iL >= 8; iL -= 8 )
				{
					int32 lSumL = 0;
					lSumL += ( ( int8 )     ( dpL[ 0 ] <<  2 )                                  * ( int32 )inPtrL[ 0 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 0 ] >>  4 )                       & 0x00FC ) * ( int32 )inPtrL[ 1 ] );
					lSumL += ( ( int8 ) ( ( ( dpL[ 0 ] >> 10 ) | ( dpL[ 1 ] << 6 ) ) & 0x00FC ) * ( int32 )inPtrL[ 2 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 1 ]       )                       & 0x00FC ) * ( int32 )inPtrL[ 3 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 1 ] >>  6 )                       & 0x00FC ) * ( int32 )inPtrL[ 4 ] );
					lSumL += ( ( int8 ) ( ( ( dpL[ 1 ] >> 12 ) | ( dpL[ 2 ] << 4 ) ) & 0x00FC ) * ( int32 )inPtrL[ 5 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 2 ] >>  2 )                       & 0x00FC ) * ( int32 )inPtrL[ 6 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 2 ] >>  8 )                       & 0x00FC ) * ( int32 )inPtrL[ 7 ] );
					sumL += ( lSumL >> 2 );
					dpL += 3;
					inPtrL += 8;
				}

				{
					int32 lSumL = 0;
					if( iL > 0 ) lSumL += ( ( int8 )     ( dpL[ 0 ] <<  2 )                                  * ( int32 )inPtrL[ 0 ] );
					if( iL > 1 ) lSumL += ( ( int8 ) (   ( dpL[ 0 ] >>  4 )                       & 0x00FC ) * ( int32 )inPtrL[ 1 ] );
					if( iL > 2 ) lSumL += ( ( int8 ) ( ( ( dpL[ 0 ] >> 10 ) | ( dpL[ 1 ] << 6 ) ) & 0x00FC ) * ( int32 )inPtrL[ 2 ] );
					if( iL > 3 ) lSumL += ( ( int8 ) (   ( dpL[ 1 ]       )                       & 0x00FC ) * ( int32 )inPtrL[ 3 ] );
					if( iL > 4 ) lSumL += ( ( int8 ) (   ( dpL[ 1 ] >>  6 )                       & 0x00FC ) * ( int32 )inPtrL[ 4 ] );
					if( iL > 5 ) lSumL += ( ( int8 ) ( ( ( dpL[ 1 ] >> 12 ) | ( dpL[ 2 ] << 4 ) ) & 0x00FC ) * ( int32 )inPtrL[ 5 ] );
					if( iL > 6 ) lSumL += ( ( int8 ) (   ( dpL[ 2 ] >>  2 )                       & 0x00FC ) * ( int32 )inPtrL[ 6 ] );
					sumL += ( lSumL >> 2 );
				}
			}
			break;

			case 5: 
			{
				const uint16* dpL = ( uint16* )rowPtrL;
				for( iL = sizeL; iL >= 16; iL -= 16 )
				{
					int32 lSumL = 0;
					lSumL += ( ( int8 )     ( dpL[ 0 ] <<  3 )                                  * ( int32 )inPtrL[  0 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 0 ] >>  2 )                       & 0x00F8 ) * ( int32 )inPtrL[  1 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 0 ] >>  7 )                       & 0x00F8 ) * ( int32 )inPtrL[  2 ] );
					lSumL += ( ( int8 ) ( ( ( dpL[ 0 ] >> 12 ) | ( dpL[ 1 ] << 4 ) ) & 0x00F8 ) * ( int32 )inPtrL[  3 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 1 ] >>  1 )                       & 0x00F8 ) * ( int32 )inPtrL[  4 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 1 ] >>  6 )                       & 0x00F8 ) * ( int32 )inPtrL[  5 ] );
					lSumL += ( ( int8 ) ( ( ( dpL[ 1 ] >> 11 ) | ( dpL[ 2 ] << 5 ) ) & 0x00F8 ) * ( int32 )inPtrL[  6 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 2 ]       )                       & 0x00F8 ) * ( int32 )inPtrL[  7 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 2 ] >>  5 )                       & 0x00F8 ) * ( int32 )inPtrL[  8 ] );
					lSumL += ( ( int8 ) ( ( ( dpL[ 2 ] >> 10 ) | ( dpL[ 3 ] << 6 ) ) & 0x00F8 ) * ( int32 )inPtrL[  9 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 3 ] <<  1 )                       & 0x00F8 ) * ( int32 )inPtrL[ 10 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 3 ] >>  4 )                       & 0x00F8 ) * ( int32 )inPtrL[ 11 ] );
					lSumL += ( ( int8 ) ( ( ( dpL[ 3 ] >>  9 ) | ( dpL[ 4 ] << 7 ) ) & 0x00F8 ) * ( int32 )inPtrL[ 12 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 4 ] <<  2 )                       & 0x00F8 ) * ( int32 )inPtrL[ 13 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 4 ] >>  3 )                       & 0x00F8 ) * ( int32 )inPtrL[ 14 ] );
					lSumL += ( ( int8 ) (   ( dpL[ 4 ] >>  8 )                       & 0x00F8 ) * ( int32 )inPtrL[ 15 ] );
					sumL += ( lSumL >> 3 );
					dpL += 5;
					inPtrL += 16;
				}

				{
					int32 lSumL = 0;
					if( iL >  0 ) lSumL += ( ( int8 )     ( dpL[ 0 ] <<  3 )                                  * ( int32 )inPtrL[  0 ] );
					if( iL >  1 ) lSumL += ( ( int8 ) (   ( dpL[ 0 ] >>  2 )                       & 0x00F8 ) * ( int32 )inPtrL[  1 ] );
					if( iL >  2 ) lSumL += ( ( int8 ) (   ( dpL[ 0 ] >>  7 )                       & 0x00F8 ) * ( int32 )inPtrL[  2 ] );
					if( iL >  3 ) lSumL += ( ( int8 ) ( ( ( dpL[ 0 ] >> 12 ) | ( dpL[ 1 ] << 4 ) ) & 0x00F8 ) * ( int32 )inPtrL[  3 ] );
					if( iL >  4 ) lSumL += ( ( int8 ) (   ( dpL[ 1 ] >>  1 )                       & 0x00F8 ) * ( int32 )inPtrL[  4 ] );
					if( iL >  5 ) lSumL += ( ( int8 ) (   ( dpL[ 1 ] >>  6 )                       & 0x00F8 ) * ( int32 )inPtrL[  5 ] );
					if( iL >  6 ) lSumL += ( ( int8 ) ( ( ( dpL[ 1 ] >> 11 ) | ( dpL[ 2 ] << 5 ) ) & 0x00F8 ) * ( int32 )inPtrL[  6 ] );
					if( iL >  7 ) lSumL += ( ( int8 ) (   ( dpL[ 2 ]       )                       & 0x00F8 ) * ( int32 )inPtrL[  7 ] );
					if( iL >  8 ) lSumL += ( ( int8 ) (   ( dpL[ 2 ] >>  5 )                       & 0x00F8 ) * ( int32 )inPtrL[  8 ] );
					if( iL >  9 ) lSumL += ( ( int8 ) ( ( ( dpL[ 2 ] >> 10 ) | ( dpL[ 3 ] << 6 ) ) & 0x00F8 ) * ( int32 )inPtrL[  9 ] );
					if( iL > 10 ) lSumL += ( ( int8 ) (   ( dpL[ 3 ] <<  1 )                       & 0x00F8 ) * ( int32 )inPtrL[ 10 ] );
					if( iL > 11 ) lSumL += ( ( int8 ) (   ( dpL[ 3 ] >>  4 )                       & 0x00F8 ) * ( int32 )inPtrL[ 11 ] );
					if( iL > 12 ) lSumL += ( ( int8 ) ( ( ( dpL[ 3 ] >>  9 ) | ( dpL[ 4 ] << 7 ) ) & 0x00F8 ) * ( int32 )inPtrL[ 12 ] );
					if( iL > 13 ) lSumL += ( ( int8 ) (   ( dpL[ 4 ] <<  2 )                       & 0x00F8 ) * ( int32 )inPtrL[ 13 ] );
					if( iL > 14 ) lSumL += ( ( int8 ) (   ( dpL[ 4 ] >>  3 )                       & 0x00F8 ) * ( int32 )inPtrL[ 14 ] );
					sumL += ( lSumL >> 3 );
				}
			}
			break;

			case 4: 
			{
				for( iL = sizeL; iL >= 4; iL -= 4 )
				{
					uint16 v1L = *rowPtrL++;
					int32 lSumL = 0;
					lSumL += ( ( int8 )( ( v1L << 4 )        ) * ( int32 )inPtrL[ 0 ] );
					lSumL += ( ( int8 )( ( v1L      ) & 0xF0 ) * ( int32 )inPtrL[ 1 ] );
					lSumL += ( ( int8 )( ( v1L >> 4 ) & 0xF0 ) * ( int32 )inPtrL[ 2 ] );
					lSumL += ( ( int8 )( ( v1L >> 8 ) & 0xF0 ) * ( int32 )inPtrL[ 3 ] );
					inPtrL += 4;
					sumL += ( lSumL >> 4 );
				}
				{
					uint16 v1L = *rowPtrL++;
					int32 lSumL = 0;
					if( iL-- > 0 ) lSumL += ( ( int8 )( ( v1L << 4 )        ) * ( int32 )inPtrL[ 0 ] );
					if( iL-- > 0 ) lSumL += ( ( int8 )( ( v1L      ) & 0xF0 ) * ( int32 )inPtrL[ 1 ] );
					if( iL-- > 0 ) lSumL += ( ( int8 )( ( v1L >> 4 ) & 0xF0 ) * ( int32 )inPtrL[ 2 ] );
					sumL += ( lSumL >> 4 );
				}
			}
			break;

			#endif /*ifndef HW_TMS320C5x*/

			/* The default case can process all bit sizes including those that are explicitly encoded above
			 * Use the default for all bit sizes when the platform cannot handle the int8 data type (e.g. HW_TMS320C5x)
			 */
			default:
			{
				uint32 bfL = ( ( uint32 )*rowPtrL++ ) << 16;
				uint32 bitsL = ptrA->bitsPerValueE;
				uint16 adjL = 16 - bitsL;
				uint32 mkL = ( ( 1 << bitsL ) - 1 ) << adjL;
				uint32 srL = bitsL;
				for( iL = 0; iL < sizeL; iL++ )
				{
					if( srL > 16 )
					{
						bfL = ( ( ( uint32 )*rowPtrL++ ) << 16 ) | ( bfL >> 16 );
						srL -= 16;
					}
					sumL += ( ( int16 )( ( bfL >> srL ) & mkL ) * ( int32 )inPtrL[ iL ] ) >> adjL;
					srL += bitsL;
				}
			}
		}
	}
	else /* raw dot product does not fit in int32 */
	{
		int32 roundL = 1 << ( overflowBitsL - 1 );
		switch( ptrA->bitsPerValueE )
		{
			case 16:
			{
				for( iL = sizeL; iL > 0; iL-- ) sumL += ( ( ( int32 )*rowPtrL++ * ( int32 )*inPtrL++ ) + roundL ) >> overflowBitsL;
			}
			break;

			case 8: 
			{
				for( iL = sizeL; iL >= 2; iL -= 2 )
				{
					uint16 v1L = *rowPtrL++;
					int32 lSumL =   ( ( int8 )  v1L         * ( int32 )inPtrL[ 0 ] )
						          + ( ( int8 )( v1L >>  8 ) * ( int32 )inPtrL[ 1 ] );
					sumL += ( lSumL + roundL ) >> overflowBitsL;
					inPtrL += 2;
				}
				if( iL > 0 )
				{
					sumL += ( ( ( int8 )*rowPtrL++ * ( int32 )inPtrL[ 0 ] ) + roundL ) >> overflowBitsL;
				}
			}
			break;

			case 4: 
			{
				for( iL = sizeL; iL >= 4; iL -= 4 )
				{
					uint16 v1L = *rowPtrL++;
					int32 lSumL = 0;
					lSumL += ( ( int8 )( ( v1L << 4 )        ) * ( int32 )inPtrL[ 0 ] );
					lSumL += ( ( int8 )( ( v1L      ) & 0xF0 ) * ( int32 )inPtrL[ 1 ] );
					lSumL += ( ( int8 )( ( v1L >> 4 ) & 0xF0 ) * ( int32 )inPtrL[ 2 ] );
					lSumL += ( ( int8 )( ( v1L >> 8 ) & 0xF0 ) * ( int32 )inPtrL[ 3 ] );
					inPtrL += 4;
					sumL += ( ( lSumL >> 4 ) + roundL ) >> overflowBitsL;
				}
				{
					uint16 v1L = *rowPtrL++;
					int32 lSumL = 0;
					if( iL-- > 0 ) lSumL += ( ( int8 )( ( v1L << 4 )        ) * ( int32 )inPtrL[ 0 ] );
					if( iL-- > 0 ) lSumL += ( ( int8 )( ( v1L      ) & 0xF0 ) * ( int32 )inPtrL[ 1 ] );
					if( iL-- > 0 ) lSumL += ( ( int8 )( ( v1L >> 4 ) & 0xF0 ) * ( int32 )inPtrL[ 2 ] );
					sumL += ( ( lSumL >> 4 ) + roundL ) >> overflowBitsL;
				}
			}
			break;

			default:
			{
				uint32 bfL = ( ( uint32 )*rowPtrL++ ) << 16;
				uint32 bitsL = ptrA->bitsPerValueE;
				uint16 adjL = 16 - bitsL;
				uint32 mkL = ( ( 1 << bitsL ) - 1 ) << adjL;
				uint32 srL = bitsL;
				int32 lRoundL = roundL << adjL;
				int32 lAdjL = overflowBitsL + adjL;
				for( iL = 0; iL < sizeL; iL++ )
				{
					if( srL > 16 )
					{
						bfL = ( ( ( uint32 )*rowPtrL++ ) << 16 ) | ( bfL >> 16 );
						srL -= 16;
					}
					sumL += ( ( int16 )( ( bfL >> srL ) & mkL ) * ( int32 )inPtrL[ iL ] + lRoundL ) >> lAdjL;
					srL += bitsL;
				}
			}
		}
	}

	/* compute result */
	{
		int32 resultManL;
		int32 resultExpL;
		int32 resultLogL;
		bbs_mulS32( sumL, factorManL, &resultManL, &resultExpL );
		resultExpL += factorExpL + overflowBitsL;
		resultLogL = bbs_intLog2( resultManL > 0 ? resultManL : -resultManL );
		if( resultLogL < 30 )
		{
			resultManL <<= 30 - resultLogL;
			resultExpL  -= 30 - resultLogL;
		}

		resultManL = ( ( resultManL >> 15 ) + 1 ) >> 1;
		resultExpL = resultExpL + 16;

		return ( ( resultManL & 0x0000FFFF ) << 16 ) | ( resultExpL & 0x0000FFFF );
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_CompactMat_init( struct bbs_Context* cpA,
					      struct bts_CompactMat* ptrA )
{
	ptrA->widthE = 0;
	ptrA->heightE = 0;
	ptrA->bitsPerValueE = 0;
	ptrA->wordsPerRowE = 0;
	ptrA->maxRowBitsE = 0;
	bbs_Int16Arr_init( cpA, &ptrA->cpsArrE );
	bbs_Int16Arr_init( cpA, &ptrA->expArrE );
	
}

/* ------------------------------------------------------------------------- */

void bts_CompactMat_exit( struct bbs_Context* cpA,
					    struct bts_CompactMat* ptrA )
{
	ptrA->widthE = 0;
	ptrA->heightE = 0;
	ptrA->bitsPerValueE = 0;
	ptrA->wordsPerRowE = 0;
	ptrA->maxRowBitsE = 0;
	bbs_Int16Arr_exit( cpA, &ptrA->cpsArrE );
	bbs_Int16Arr_exit( cpA, &ptrA->expArrE );
}
/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

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
	
void bts_CompactMat_create( struct bbs_Context* cpA,
						    struct bts_CompactMat* ptrA, 
						    uint32 widthA,
						    uint32 heightA,
						    uint32 bitsA,
							uint32 maxRowSizeA,
				            struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	if( bitsA < 2 || bitsA > 16 )
	{
		bbs_ERROR0( "bts_CompactMat_create:\nbitsA must be between 2 and 16" );
		return;
	}

	ptrA->widthE = widthA;
	ptrA->heightE = heightA;
	ptrA->bitsPerValueE = bitsA;
	ptrA->wordsPerRowE = 6 /*header + 1*/ + ( ( maxRowSizeA * bitsA ) / ( 8 * sizeof( short ) ) );
	ptrA->maxRowBitsE = 0;
	if( ( ptrA->wordsPerRowE & 1 ) != 0 ) ptrA->wordsPerRowE++;
	bbs_Int16Arr_create( cpA, &ptrA->cpsArrE, heightA * ptrA->wordsPerRowE, mspA );
	bbs_Int16Arr_fill( cpA, &ptrA->cpsArrE, 0 );
	bbs_Int16Arr_create( cpA, &ptrA->expArrE, ptrA->heightE, mspA );
	bbs_Int16Arr_fill( cpA, &ptrA->expArrE, 0 );
}

/* ------------------------------------------------------------------------- */
	
void bts_CompactMat_copy( struct bbs_Context* cpA,
					      struct bts_CompactMat* ptrA, 
						  const struct bts_CompactMat* srcPtrA )
{
	ptrA->widthE = srcPtrA->widthE;
	ptrA->heightE = srcPtrA->heightE;
	ptrA->bitsPerValueE = srcPtrA->bitsPerValueE;
	ptrA->wordsPerRowE = srcPtrA->wordsPerRowE;
	ptrA->maxRowBitsE = srcPtrA->maxRowBitsE;
	bbs_Int16Arr_copy( cpA, &ptrA->cpsArrE, &srcPtrA->cpsArrE );
	bbs_Int16Arr_size( cpA, &ptrA->expArrE, ptrA->heightE );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_CompactMat_memSize( struct bbs_Context* cpA,
							 const struct bts_CompactMat *ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->widthE ) 
		  + bbs_SIZEOF16( ptrA->heightE ) 
		  + bbs_SIZEOF16( ptrA->bitsPerValueE ) 
		  + bbs_SIZEOF16( ptrA->wordsPerRowE )
		  + bbs_SIZEOF16( ptrA->maxRowBitsE )
		  + bbs_Int16Arr_memSize( cpA, &ptrA->cpsArrE );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_CompactMat_memWrite( struct bbs_Context* cpA,
							  const struct bts_CompactMat* ptrA, 
							  uint16* memPtrA )
{
	uint32 memSizeL = bts_CompactMat_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_COMPACT_MAT_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->heightE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->bitsPerValueE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->wordsPerRowE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->maxRowBitsE, memPtrA );
	memPtrA += bbs_Int16Arr_memWrite( cpA, &ptrA->cpsArrE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_CompactMat_memRead( struct bbs_Context* cpA,
							 struct bts_CompactMat* ptrA, 
							 const uint16* memPtrA,
				             struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_COMPACT_MAT_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->heightE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->bitsPerValueE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->wordsPerRowE, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->maxRowBitsE, memPtrA );
	memPtrA += bbs_Int16Arr_memRead( cpA, &ptrA->cpsArrE, memPtrA, mspA );

	if( memSizeL != bts_CompactMat_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_CompactMat_memRead( const struct bts_CompactMat* ptrA, const void* memPtrA ):\n"
                  "size mismatch" ); 
	}

	bbs_Int16Arr_create( cpA, &ptrA->expArrE, ptrA->heightE, mspA );
	bbs_Int16Arr_fill( cpA, &ptrA->expArrE, 0 );

	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_CompactMat_map( struct bbs_Context* cpA, 
						 const struct bts_CompactMat* ptrA, 
						 const int16* inVecA,
						 int16* outVecA,
						 int16* outExpPtrA )
{
	uint32 inNormBitsL = bbs_intLog2( bbs_vecNorm16( inVecA, ptrA->widthE ) ) + 1;
	uint32 iL;

	int16* expArrL = ( ( struct bts_CompactMat* )ptrA )->expArrE.arrPtrE;
	int16 maxExpL = -32767;

	for( iL = 0; iL < ptrA->heightE; iL++ )
	{
		int32 fltL = bts_CompactMat_fltDotPrdRow( cpA, ( struct bts_CompactMat* )ptrA, inVecA, inNormBitsL, iL );
		outVecA[ iL ] = fltL >> 16; 
		expArrL[ iL ] = fltL & 0x0000FFFF;

		maxExpL = ( expArrL[ iL ] > maxExpL ) ? expArrL[ iL ] : maxExpL;
	}

	if( outExpPtrA != NULL ) *outExpPtrA = maxExpL;

	for( iL = 0; iL < ptrA->heightE; iL++ )
	{
		int32 shrL = maxExpL - expArrL[ iL ];
		if( shrL > 0 )
		{
			outVecA[ iL ] = ( ( outVecA[ iL ] >> ( shrL - 1 ) ) + 1 ) >> 1;
		}
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

