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

#include "b_TensorEm/Int32Mat.h"
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

void bts_Int32Mat_reduceToNBits( int32* ptrA, uint32 sizeA, int32* bbpPtrA, uint32 nBitsA )
{
	int32 shiftL;

	/* find max element */
	int32 maxL = 0;
	int32* ptrL = ptrA;
	int32 iL = sizeA;
	while( iL-- )
	{
		int32 xL = *ptrL++;
		if( xL < 0 ) xL = -xL;
		if( xL > maxL ) maxL = xL;
	}

	/* determine shift */
	shiftL = bts_absIntLog2( maxL ) + 1 - nBitsA;

	if( shiftL > 0 )
	{
		ptrL = ptrA;
		iL = sizeA;
		while( iL-- )
		{
			*ptrL = ( ( *ptrL >> ( shiftL - 1 ) ) + 1 ) >> 1;
			ptrL++;
		}

		*bbpPtrA -= shiftL;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Int32Mat_init( struct bbs_Context* cpA,
					    struct bts_Int32Mat* ptrA )
{
	ptrA->widthE = 0;
	bbs_Int32Arr_init( cpA, &ptrA->arrE );
}

/* ------------------------------------------------------------------------- */

void bts_Int32Mat_exit( struct bbs_Context* cpA,
					    struct bts_Int32Mat* ptrA )
{
	ptrA->widthE = 0;
	bbs_Int32Arr_exit( cpA, &ptrA->arrE );
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
	
void bts_Int32Mat_create( struct bbs_Context* cpA,
						  struct bts_Int32Mat* ptrA, 
						  int32 widthA,
				          struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	bbs_Int32Arr_create( cpA, &ptrA->arrE, widthA * widthA, mspA );
	ptrA->widthE = widthA;
}

/* ------------------------------------------------------------------------- */
	
void bts_Int32Mat_copy( struct bbs_Context* cpA,
					    struct bts_Int32Mat* ptrA, 
						const struct bts_Int32Mat* srcPtrA )
{
	if( ptrA->widthE != srcPtrA->widthE )
	{
		bbs_ERROR0( "void bts_Int32Mat_copy( struct bts_Int32Mat* ptrA, struct bts_Int32Mat* srcPtrA ):\n"
			       "size mismatch" );
		return;
	}

	bbs_Int32Arr_copy( cpA, &ptrA->arrE, &srcPtrA->arrE );
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int32Mat_memSize( struct bbs_Context* cpA,
							 const struct bts_Int32Mat *ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bbs_SIZEOF16( ptrA->widthE ) 
		  + bbs_Int32Arr_memSize( cpA, &ptrA->arrE );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int32Mat_memWrite( struct bbs_Context* cpA,
							  const struct bts_Int32Mat* ptrA, 
							  uint16* memPtrA )
{
	uint32 memSizeL = bts_Int32Mat_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_INT32MAT_VERSION, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_Int32Arr_memWrite( cpA, &ptrA->arrE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Int32Mat_memRead( struct bbs_Context* cpA,
							 struct bts_Int32Mat* ptrA, 
							 const uint16* memPtrA,
				             struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_INT32MAT_VERSION, memPtrA );
	memPtrA += bbs_memRead32( &ptrA->widthE, memPtrA );
	memPtrA += bbs_Int32Arr_memRead( cpA, &ptrA->arrE, memPtrA, mspA );

	if( memSizeL != bts_Int32Mat_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_Int32Mat_memRead( const struct bts_Int32Mat* ptrA, const void* memPtrA ):\n"
                  "size mismatch" ); 
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

flag bts_Int32Mat_solve( struct bbs_Context* cpA,
						 const int32* matA,
						 int32 matWidthA,
						 const int32* inVecA,
						 int32* outVecA,
						 int32 bbpA,
						 int32* tmpMatA,
						 int32* tmpVecA )
{
	bbs_memcpy32( tmpMatA, matA, ( matWidthA * matWidthA ) * bbs_SIZEOF32( int32 ) );

	return bts_Int32Mat_solve2( cpA, 
		                        tmpMatA,
								matWidthA,
								inVecA,
								outVecA,
								bbpA,
								tmpVecA );
}

/* ------------------------------------------------------------------------- */

flag bts_Int32Mat_solve2( struct bbs_Context* cpA,
						  int32* matA,
						  int32 matWidthA,
						  const int32* inVecA,
						  int32* outVecA,
						  int32 bbpA,
						  int32* tmpVecA )
{
	int32 sizeL = matWidthA;
	int32 bbpL = bbpA;
	int32 iL, jL, kL;
	int32 iPivL;
	int32 jPivL;

	int32* vecL      = outVecA;
	int32* matL      = matA;
	int32* checkArrL = tmpVecA;

	for( iL = 0; iL < sizeL; iL++ )
	{
		checkArrL[ iL ] = 0;
	}
	
	bbs_memcpy32( outVecA, inVecA, sizeL * bbs_SIZEOF32( int32 ) );

	iPivL = 0;

	for( kL = 0; kL < sizeL; kL++ )
	{
		/* find pivot */
		int32 maxAbsL = 0;
		int32* pivRowL;

		int32 bbp_pivRowL, bbp_vecL, shiftL;

		jPivL = -1;
		for( iL = 0; iL < sizeL; iL++ )
		{
			if( checkArrL[ iL ] != 1 )
			{
				int32* rowL = matL + ( iL * sizeL );
				for( jL = 0; jL < sizeL; jL++ )
				{
					if( checkArrL[ jL ] == 0 )
					{
						int32 absElemL = rowL[ jL ];
						if( absElemL < 0 ) absElemL = -absElemL;
						if( maxAbsL < absElemL )
						{
							maxAbsL = absElemL;
							iPivL = iL;
							jPivL = jL;
						}
					} 
					else if( checkArrL[ jL ] > 1 )
					{
						return FALSE;
					}
				}
			}
		}

		/* successfull ? */
		if( jPivL < 0 )
		{
			return FALSE;
		}

		checkArrL[ jPivL ]++; 

		/* exchange rows to put pivot on diagonal, if neccessary */
		if( iPivL != jPivL )
		{
			int32* row1PtrL = matL + ( iPivL * sizeL );
			int32* row2PtrL = matL + ( jPivL * sizeL );
			for( jL = 0; jL < sizeL; jL++ )
			{
				int32 tmpL = *row1PtrL;
				*row1PtrL++ = *row2PtrL;
				*row2PtrL++ = tmpL;
			}

			{
				int32 tmpL = vecL[ jPivL ];
				vecL[ jPivL ] = vecL[ iPivL ];
				vecL[ iPivL ] = tmpL;
			}
		}
		/* now index jPivL specifies pivot row and maximum element */


		/**	Overflow protection: only if the highest bit of the largest matrix element is set,
		 *	we need to shift the whole matrix and the right side vector 1 bit to the right,
		 *	to make sure there can be no overflow when the pivot row gets subtracted from the
		 *	other rows.
		 *	Getting that close to overflow is a rare event, so this shift will happen only 
		 *	occasionally, or not at all.
		 */
		if( maxAbsL & 1073741824 )  /*( 1 << 30 )*/
		{
			/* right shift matrix by 1 */
			int32 iL = sizeL * sizeL;
			int32* ptrL = matL;
			while( iL-- )
			{
				*ptrL = ( *ptrL + 1 ) >> 1;
				ptrL++;
			}

			/* right shift right side vector by 1 */
			iL = sizeL;
			ptrL = vecL;
			while( iL-- )
			{
				*ptrL = ( *ptrL + 1 ) >> 1;
				ptrL++;
			}

			/* decrement bbpL */
			bbpL--;
		}


		/* reduce elements of pivot row to 15 bit */
		pivRowL = matL + jPivL * sizeL;
		bbp_pivRowL = bbpL;
		bts_Int32Mat_reduceToNBits( pivRowL, sizeL, &bbp_pivRowL, 15 );

		/* scale pivot row such that maximum equals 1 */
		{
			int32 maxL = pivRowL[ jPivL ];
			int32 bbp_maxL = bbp_pivRowL;
			int32 factorL = 1073741824 / maxL; /*( 1 << 30 )*/

			for( jL = 0; jL < sizeL; jL++ )
			{
				pivRowL[ jL ] = ( pivRowL[ jL ] * factorL + ( 1 << 14 ) ) >> 15;
			}
			bbp_pivRowL = 15;

			/* set to 1 to avoid computational errors */
			pivRowL[ jPivL ] = ( int32 )1 << bbp_pivRowL; 

			shiftL = 30 - bts_absIntLog2( vecL[ jPivL ] );

			vecL[ jPivL ] = ( vecL[ jPivL ] << shiftL ) / maxL;
			bbp_vecL = bbpL + shiftL - bbp_maxL;

			bbs_int32ReduceToNBits( &( vecL[ jPivL ] ), &bbp_vecL, 15 );
		}

		/* subtract pivot row from all other rows */
		for( iL = 0; iL < sizeL; iL++ )
		{
			if( iL != jPivL )
			{
				int32* rowPtrL = matL + iL * sizeL;

				int32 tmpL = *( rowPtrL + jPivL );
				int32 bbp_tmpL = bbpL;
				bbs_int32ReduceToNBits( &tmpL, &bbp_tmpL, 15 );

				shiftL = bbp_tmpL + bbp_pivRowL - bbpL;
				if( shiftL > 0 )
				{
					for( jL = 0; jL < sizeL; jL++ )
					{
						*rowPtrL++ -= ( ( ( tmpL * pivRowL[ jL ] ) >> ( shiftL - 1 ) ) + 1 ) >> 1;
					}
				}
				else
				{
					for( jL = 0; jL < sizeL; jL++ )
					{
						*rowPtrL++ -= ( tmpL * pivRowL[ jL ] ) << -shiftL;
					}
				}

				shiftL = bbp_tmpL + bbp_vecL - bbpL;
				if( shiftL > 0 )
				{
					vecL[ iL ] -= ( ( ( tmpL * vecL[ jPivL ] ) >> ( shiftL - 1 ) ) + 1 ) >> 1;
				}
				else
				{
					vecL[ iL ] -= ( tmpL * vecL[ jPivL ] ) << -shiftL;
				}
			}
		}

		/* change bbp of pivot row back to bbpL */
		shiftL = bbpL - bbp_pivRowL;
		if( shiftL >= 0 )
		{
			for( jL = 0; jL < sizeL; jL++ )
			{
				pivRowL[ jL ] <<= shiftL;
			}
		}
		else
		{
			shiftL = -shiftL;
			for( jL = 0; jL < sizeL; jL++ )
			{
				pivRowL[ jL ] = ( ( pivRowL[ jL ] >> ( shiftL - 1 ) ) + 1 ) >> 1;
			}
		}

		shiftL = bbpL - bbp_vecL;
		if( shiftL >= 0 )
		{
			vecL[ jPivL ] <<= shiftL;
		}
		else
		{
			shiftL = -shiftL;
			vecL[ jPivL ] = ( ( vecL[ jPivL ] >> ( shiftL - 1 ) ) + 1 ) >> 1;
		}
/*
if( sizeL <= 5 ) bts_Int32Mat_print( matL, vecL, sizeL, bbpL );
*/
	}	/* of kL */

	/* in case bbpL has been decreased by the overflow protection, change it back now */
	if( bbpA > bbpL )
	{
		/* find largest element of solution vector */
		int32 maxL = 0;
		int32 iL, shiftL;
		for( iL = 0; iL < sizeL; iL++ )
		{
			int32 xL = vecL[ iL ];
			if( xL < 0 ) xL = -xL;
			if( xL > maxL ) maxL = xL;
		}
		
		/* check whether we can left shift without overflow */
		shiftL = 30 - bts_absIntLog2( maxL );
		if( shiftL < ( bbpA - bbpL ) )
		{
			/* 
			    bbs_WARNING1( "flag bts_Int32Mat_solve2( ... ): getting overflow when trying to "
				"compute solution vector with bbp = %d. Choose smaller bbp.\n", bbpA );
			*/

			return FALSE;
		}	

		/* shift left */
		shiftL = bbpA - bbpL;
		for( iL = 0; iL < sizeL; iL++ ) vecL[ iL ] <<= shiftL;
	}

	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

