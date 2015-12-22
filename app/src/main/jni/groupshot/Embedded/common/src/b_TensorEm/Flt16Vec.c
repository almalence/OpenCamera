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

#include "b_TensorEm/Flt16Vec.h"
#include "b_BasicEm/Memory.h"
#include "b_BasicEm/Math.h"
#include "b_BasicEm/Functions.h"

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

void bts_Flt16Vec_init( struct bbs_Context* cpA,
						struct bts_Flt16Vec* ptrA )
{
	bbs_Int16Arr_init( cpA, &ptrA->arrE );
	ptrA->expE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec_exit( struct bbs_Context* cpA,
						struct bts_Flt16Vec* ptrA )
{
	bbs_Int16Arr_exit( cpA, &ptrA->arrE );
	ptrA->expE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec_copy( struct bbs_Context* cpA,
						struct bts_Flt16Vec* ptrA, 
						const struct bts_Flt16Vec* srcPtrA )
{
	bbs_Int16Arr_copy( cpA, &ptrA->arrE, &srcPtrA->arrE );
	ptrA->expE = srcPtrA->expE;
}

/* ------------------------------------------------------------------------- */

flag bts_Flt16Vec_equal( struct bbs_Context* cpA,
						 const struct bts_Flt16Vec* ptrA, 
						 const struct bts_Flt16Vec* srcPtrA )
{
	if( !bbs_Int16Arr_equal( cpA, &ptrA->arrE, &srcPtrA->arrE ) ) return FALSE;
	if( ptrA->expE != srcPtrA->expE ) return FALSE;
	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

int16 bts_Flt16Vec_avg( struct bbs_Context* cpA, const struct bts_Flt16Vec* ptrA )
{
	uint16 iL;
	uint16 sizeL = ptrA->arrE.sizeE;
	int32 sumL = 0;
	const int16* srcL = ptrA->arrE.arrPtrE;
	for( iL = 0; iL < sizeL; iL++ )
	{
		sumL += srcL[ iL ];
	}
	return sumL / ( int32 )sizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bts_Flt16Vec_norm( struct bbs_Context* cpA, const struct bts_Flt16Vec* ptrA )
{
	return bbs_vecNorm16( ptrA->arrE.arrPtrE, ptrA->arrE.sizeE );
}

/* ------------------------------------------------------------------------- */

uint16 bts_Flt16Vec_maxAbs( struct bbs_Context* cpA, const struct bts_Flt16Vec* ptrA )
{
	uint16 iL;
	uint16 sizeL = ptrA->arrE.sizeE;
	uint16 maxL = 0;
	const int16* srcL = ptrA->arrE.arrPtrE;
	for( iL = 0; iL < sizeL; iL++ )
	{
		uint16 vL = srcL[ iL ] > 0 ? srcL[ iL ] : -srcL[ iL ];
		maxL = vL > maxL ? vL : maxL;
	}
	return maxL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bts_Flt16Vec_create( struct bbs_Context* cpA,
						  struct bts_Flt16Vec* ptrA, 
						  uint32 sizeA,
						  struct bbs_MemSeg* mspA )
{
	bbs_Int16Arr_create( cpA, &ptrA->arrE, sizeA, mspA );
}

/* ------------------------------------------------------------------------- */
	
void bts_Flt16Vec_size( struct bbs_Context* cpA,
						struct bts_Flt16Vec* ptrA, 
						uint32 sizeA )
{
	bbs_Int16Arr_size( cpA, &ptrA->arrE, sizeA );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Vec_memSize( struct bbs_Context* cpA,
							  const struct bts_Flt16Vec *ptrA )
{
	return  bbs_SIZEOF16( uint32 ) /* mem size */
		+ bbs_Int16Arr_memSize( cpA, &ptrA->arrE )
		+ bbs_SIZEOF16( ptrA->expE );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Vec_memWrite( struct bbs_Context* cpA,
							   const struct bts_Flt16Vec* ptrA, 
							   uint16* memPtrA )
{
	uint32 memSizeL = bts_Flt16Vec_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_Int16Arr_memWrite( cpA, &ptrA->arrE, memPtrA );
	memPtrA += bbs_memWrite16( &ptrA->expE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Flt16Vec_memRead( struct bbs_Context* cpA,
							  struct bts_Flt16Vec* ptrA, 
							  const uint16* memPtrA,
							  struct bbs_MemSeg* mspA )
{
	uint32 memSizeL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_Int16Arr_memRead( cpA, &ptrA->arrE, memPtrA, mspA );
	memPtrA += bbs_memRead16( &ptrA->expE, memPtrA );

	if( memSizeL != bts_Flt16Vec_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_Flt16Vec_memRead( const struct bts_Flt16Vec* ptrA, const void* memPtrA ):\n"
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

void bts_Flt16Vec_maximizeMantisse( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA )
{
    uint32 maxAbsL = bts_Flt16Vec_maxAbs( cpA, ptrA );
	int16 shlL = 0;

	if( maxAbsL == 0 ) return; /* cannot maximize 0 */

	while( maxAbsL < 0x4000 )
	{
		shlL++;
		maxAbsL <<= 1;
	}

	if( shlL > 0 )
	{
		uint32 iL;
		uint32 sizeL = ptrA->arrE.sizeE;
		int16* dstL = ptrA->arrE.arrPtrE;
		for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] <<= shlL;
		ptrA->expE -= shlL;
	}
}

/* ------------------------------------------------------------------------- */

uint32 bts_Flt16Vec_maximizeAbsValue( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA )
{
    int32 maxAbsL = bts_Flt16Vec_maxAbs( cpA, ptrA );
	int32 fL;
	if( maxAbsL == 0 ) return 0; /* vector is zero */

	fL = ( int32 )0x7FFF0000 / maxAbsL;

	{
		uint32 iL;
		uint32 sizeL = ptrA->arrE.sizeE;
		int16* dstL = ptrA->arrE.arrPtrE;
		for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = ( ( int32 )dstL[ iL ] * fL + 32768 ) >> 16;
	}

	return fL;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec_zeroAverage( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA )
{
	uint16 iL;
	uint16 sizeL = ptrA->arrE.sizeE;
	int16* dstL = ptrA->arrE.arrPtrE;
	int16 avgL = bts_Flt16Vec_avg( cpA, ptrA );
	for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] -= avgL;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec_normalize( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA )
{
	uint32 normL = bts_Flt16Vec_norm( cpA, ptrA );

	if( normL == 0 ) 
	{
		/* vector is zero - do nothing */
		return; 
	}
	else
	{
		int16* dstL = ptrA->arrE.arrPtrE;
		uint16 iL;
		uint16 sizeL = ptrA->arrE.sizeE;
	    int16 expL = 0;
		int32 fL;

		/* let norm occupy 17 bits */
		if( ( normL & 0xFFFE0000 ) != 0 )
		{
			while( ( ( normL >> -expL ) & 0xFFFE0000 ) != 0 ) expL--;
			normL >>= -expL;
		}
		else
		{
			while( ( ( normL <<  expL ) & 0xFFFF0000 ) == 0 ) expL++;
			normL <<=  expL;
		}

		/* fL is positive and occupies only 16 bits - a product with int16 fits in int32 */
		fL = ( uint32 )0xFFFFFFFF / normL;

		/* multiply with factor */
		for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = ( ( ( ( int32 )dstL[ iL ] * fL ) >> 15 ) + 1 ) >> 1;

		/* set exponent */
		ptrA->expE = expL - 16;
	}
/*
	{
		uint32 testNormL = bts_Flt16Vec_norm( cpA, ptrA );
		printf( "test norm %f\n", ( float )testNormL / ( 1 << -ptrA->expE ) );
	}
*/
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec_setZero( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA )
{
	bbs_Int16Arr_fill( cpA, &ptrA->arrE, 0 );
	ptrA->expE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec_mul( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA, int32 valA, int16 expA )
{
	int32 valL = valA;
	int16 expL = expA;

	if( valL == 0 )
	{
		bts_Flt16Vec_setZero( cpA, ptrA );
		return;
	}
	else
	{
		uint32 iL;
		uint32 sizeL = ptrA->arrE.sizeE;
		int16* dstL = ptrA->arrE.arrPtrE;

		/* adjust valL to maximum 16 bit accuracy  */
		uint32 absValL = valL > 0 ? valL : -valL;
		if( ( absValL & 0xFFFF8000 ) != 0 )
		{
			int32 shrL = 0;
			while( ( absValL & 0xFFFF8000 ) != 0 )
			{
				absValL >>= 1;
				shrL++;
			}

			if( shrL > 0 ) 
			{
				valL = ( ( valL >> ( shrL - 1 ) ) + 1 ) >> 1;
				expL += shrL;
				if( valL >= 0x08000 ) valL = 0x07FFF; /* saturate */
			}
		}
		else
		{
			int32 shlL = 0;
			while( ( absValL & 0xFFFFC000 ) == 0 )
			{
				absValL <<= 1;
				shlL++;
			}

			valL <<= shlL;
			expL -= shlL;
		}

		for( iL = 0; iL < sizeL; iL++ )
		{
			dstL[ iL ] = ( ( ( ( int32 )dstL[ iL ] * valL ) >> 15 ) + 1 ) >> 1;
		}
		ptrA->expE += expL + 16;
	}
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec_dotPtrd( struct bbs_Context* cpA, struct bts_Flt16Vec* vp1A, struct bts_Flt16Vec* vp2A, int32* manPtrA, int32* expPtrA )
{
	bbs_DEF_fNameL( "void bts_Flt16Vec_dotPtrd( struct bbs_Context* cpA, struct bts_Flt16Vec* vp1A, struct bts_Flt16Vec* vp2A, int32* matPtrA, int32* expPtrA )" )
	uint16 iL;
	uint16 sizeL = vp1A->arrE.sizeE;
	const int16* arr1L = vp1A->arrE.arrPtrE;
	const int16* arr2L = vp2A->arrE.arrPtrE;
	int16 shrm1L = -1; /* shift minus 1 */
	int32 sumL;

	if( vp1A->arrE.sizeE != vp2A->arrE.sizeE )
	{
		bbs_ERROR1( "%s:\nVectors have different size", fNameL );
		return;
	}

	sumL = 0;
	/* shrm1L == -1 */
	for( iL = 0; iL < sizeL; iL++ )
	{
		sumL += ( int32 )arr1L[ iL ] * ( int32 )arr2L[ iL ];
		if( ( ( ( sumL > 0 ) ? sumL : -sumL ) & 0xC0000000 ) != 0 ) break;
	}

	if( iL < sizeL )
	{
		/* danger of overflow: increase shift; adjust sum */
		shrm1L++;
		sumL = ( ( sumL >> 1 ) + 1 ) >> 1;

		/* shrm1L == 0 */
		for( iL = 0; iL < sizeL; iL++ )
		{
			sumL += ( int32 )( ( arr1L[ iL ] + 1 ) >> 1 ) * ( int32 )( ( arr2L[ iL ] + 1 ) >> 1 );
			if( ( ( ( sumL > 0 ) ? sumL : -sumL ) & 0xC0000000 ) != 0 ) break;
		}

		for( iL = 0; iL < sizeL; iL++ )
		{
			if( ( ( ( sumL > 0 ) ? sumL : -sumL ) & 0xC0000000 ) != 0 )
			{
				/* danger of overflow: increase shift; adjust sum */
				shrm1L++;
				sumL = ( ( sumL >> 1 ) + 1 ) >> 1;
			}

			sumL += ( int32 )( ( ( arr1L[ iL ] >> shrm1L ) + 1 ) >> 1 ) * ( int32 )( ( ( arr2L[ iL ] >> shrm1L ) + 1 ) >> 1 );
		}
	}

	if( manPtrA != NULL ) *manPtrA = sumL;
	if( expPtrA != NULL ) *expPtrA = vp1A->expE + vp2A->expE + ( ( shrm1L + 1 ) << 1 );
}

/* ------------------------------------------------------------------------- */

void bts_Flt16Vec_append( struct bbs_Context* cpA, struct bts_Flt16Vec* ptrA, struct bts_Flt16Vec* srcPtrA )
{
	if( ptrA->arrE.sizeE == 0 ) 
	{
		bts_Flt16Vec_copy( cpA, ptrA, srcPtrA );
	}
	else
	{
		uint32 idxL = ptrA->arrE.sizeE;
		bts_Flt16Vec_size( cpA, ptrA, idxL + srcPtrA->arrE.sizeE );

		/* copy data */
		bbs_memcpy16( ptrA->arrE.arrPtrE + idxL, srcPtrA->arrE.arrPtrE, srcPtrA->arrE.sizeE );

		/* equalize exponent */
		if( ptrA->expE > srcPtrA->expE )
		{
			uint32 iL;
			uint32 sizeL = srcPtrA->arrE.sizeE;
			uint32 shrL = ptrA->expE - srcPtrA->expE;
			int16* dstL = ptrA->arrE.arrPtrE + idxL;
			for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = ( ( dstL[ iL ] >> ( shrL - 1 ) ) + 1 ) >> 1;
		}
		else if( ptrA->expE < srcPtrA->expE )
		{
			uint32 iL;
			uint32 sizeL = idxL;
			uint32 shrL = srcPtrA->expE - ptrA->expE;
			int16* dstL = ptrA->arrE.arrPtrE;
			for( iL = 0; iL < sizeL; iL++ ) dstL[ iL ] = ( ( dstL[ iL ] >> ( shrL - 1 ) ) + 1 ) >> 1;
			ptrA->expE = srcPtrA->expE;
		}
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

