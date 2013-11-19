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

#include "b_APIEm/Functions.h"
#include "b_BasicEm/Memory.h"


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

void bpi_normalizeSimilarities( struct bbs_Context* cpA,
							    const int32* rawSimArrA,
							    const int32* rawIdArrA,
								uint32 rawSizeA,
								const int32* refSimArrA,
								const int32* refIdArrA,
								uint32 refSizeA,
								enum bpi_SimType simTypeA,
								int32* outSimArrA )
{
	/* 8.24 */
	int32 refSimL = 0;
	uint32 iL, jL, kL; 
	int32* outPtrL = outSimArrA;
	const int32* rawPtrL = rawSimArrA;

	switch( simTypeA )
	{
		case bpi_RAW_SIM: 
		{
			/* nothing to do */
		}
		break;

		case bpi_SUB_MEAN:
		{
			int32 shiftL = 0;
			int32 roundL = 0;
			refSimL = 0;
			for( iL = 0; iL < refSizeA; iL++ )
			{
				refSimL += ( refSimArrA[ iL ] + roundL ) >> shiftL;
				if( refSimL > 0x40000000 )
				{
					refSimL = ( refSimL + 1 ) >> 1;
					shiftL++;
					roundL = ( int32 )1 << ( shiftL - 1 );
				}
			}
			refSimL = ( refSimL / refSizeA ) << shiftL;
		}
		break;

		case bpi_SUB_MAX_2:
		{
			int32 maxL = 0;
			uint32 maxIndexL = 0;
			int32 idL = 0;

			/* find raw maximum */
			for( iL = 0; iL < rawSizeA; iL++ )
			{
				if( maxL < rawSimArrA[ iL ] )
				{
					maxL = refSimArrA[ iL ];
					maxIndexL = iL;
				}
			}

			/* consider id of maximum equal to probe id */
			idL = rawIdArrA[ maxIndexL ];

			/* find maximum similarity in ref array of different id */
			for( iL = 0; iL < refSizeA; iL++ )
			{
				if( refIdArrA[ iL ] != idL )
				{
					refSimL = ( refSimL > refSimArrA[ iL ] ) ? refSimL : refSimArrA[ iL ];
				}
			}
		}
		break;

		case bpi_SUB_16_MAX_2:
		{
			int32 maxL = 0;
			uint32 maxIndexL = 0;
			int32 idL = 0;

			int32 maxSimArrL[ 16 ];
			bbs_memset32( maxSimArrL, ( uint32 )-1, bbs_SIZEOF32( maxSimArrL ) );

			/* find raw maximum */
			for( iL = 0; iL < rawSizeA; iL++ )
			{
				if( maxL < rawSimArrA[ iL ] )
				{
					maxL = rawSimArrA[ iL ];
					maxIndexL = iL;
				}
			}

			/* consider id of maximum equal to probe id */
			idL = rawIdArrA[ maxIndexL ];

			/* find 16 maximum similarities of different id in ref array */
			for( iL = 0; iL < refSizeA; iL++ )
			{
				if( refIdArrA[ iL ] != idL )
				{
					int32 simL = refSimArrA[ iL ];
					for( jL = 0; jL < 16; jL++ )
					{
						if( simL > maxSimArrL[ jL ] ) break;
					}
					for( kL = 15; kL > jL; kL-- )
					{
						maxSimArrL[ kL ] = maxSimArrL[ kL - 1 ];
					}
					if( jL < 16 ) maxSimArrL[ jL ] = simL;
				}
			}

			refSimL = 0;
			for( jL = 0; jL < 16; jL++ )
			{
				if( maxSimArrL[ jL ] == -1 ) break;
				refSimL += maxSimArrL[ jL ];
			}

			if( jL > 0 )
			{
				refSimL /= jL;
			}
		}
		break;

		default:
		{
			bbs_ERROR1( "void bpi_Identifier_normalizeSimilarities(): simTypeA '%i' is handled", simTypeA );
			return;
		}
	}

	/* refSimL -= 1.0 */
	refSimL -= ( (uint32)1 << 24 );

	for( iL = rawSizeA; iL > 0; iL-- )
	{
		*outPtrL++ = ( *rawPtrL++ - refSimL + 1 ) >> 1;
	}

}

/* ------------------------------------------------------------------------- */

int32 bpi_normalizedSimilarity( struct bbs_Context* cpA,
							    int32 rawSimA,
							    int32 rawIdA,
								const int32* refSimArrA,
								const int32* refIdArrA,
								uint32 refSizeA,
								enum bpi_SimType simTypeA )
{
	/* 8.24 */
	int32 refSimL = 0;
	uint32 iL, jL, kL; 

	switch( simTypeA )
	{
		case bpi_RAW_SIM: 
		{
			/* nothing to do */
			return rawSimA; /* return without adjustment of value range */
		}

		case bpi_SUB_MEAN:
		{
			int32 shiftL = 0;
			int32 roundL = 0;
			refSimL = 0;
			for( iL = 0; iL < refSizeA; iL++ )
			{
				refSimL += ( refSimArrA[ iL ] + roundL ) >> shiftL;
				if( refSimL > 0x40000000 )
				{
					refSimL = ( refSimL + 1 ) >> 1;
					shiftL++;
					roundL = ( int32 )1 << ( shiftL - 1 );
				}
			}
			refSimL = ( refSimL / refSizeA ) << shiftL;
		}
		break;

		case bpi_SUB_MAX_2:
		{
			/* find maximum similarity in ref array of different rawIdA */
			for( iL = 0; iL < refSizeA; iL++ )
			{
				if( refIdArrA[ iL ] != rawIdA )
				{
					refSimL = ( refSimL > refSimArrA[ iL ] ) ? refSimL : refSimArrA[ iL ];
				}
			}
		}
		break;

		case bpi_SUB_16_MAX_2:
		{
			int32 maxSimArrL[ 16 ];
			int32 idL = rawIdA;
			bbs_memset32( maxSimArrL, ( uint32 )-1, bbs_SIZEOF32( maxSimArrL ) );

			/* find 16 maximum similarities of different id in ref array */
			for( iL = 0; iL < refSizeA; iL++ )
			{
				if( refIdArrA[ iL ] != idL )
				{
					int32 simL = refSimArrA[ iL ];
					for( jL = 0; jL < 16; jL++ )
					{
						if( simL > maxSimArrL[ jL ] ) break;
					}
					for( kL = 15; kL > jL; kL-- )
					{
						maxSimArrL[ kL ] = maxSimArrL[ kL - 1 ];
					}
					if( jL < 16 ) maxSimArrL[ jL ] = simL;
				}
			}

			refSimL = 0;
			for( jL = 0; jL < 16; jL++ )
			{
				if( maxSimArrL[ jL ] == -1 ) break;
				refSimL += maxSimArrL[ jL ];
			}

			if( jL > 0 )
			{
				refSimL /= jL;
			}
		}
		break;

		default:
		{
			bbs_ERROR1( "void bpi_Identifier_normalizeSimilarities(): simTypeA '%i' is handled", simTypeA );
		}
		break;
	}

	/* refSimL -= 1.0 */
	refSimL -= ( (uint32)1 << 24 );
	return ( rawSimA - refSimL + 1 ) >> 1;
}

/* ------------------------------------------------------------------------- */

uint32 bpi_memWriteCsa16( uint16* memPtrA, uint32 memSizeA, uint16 chkSumA )
{
	uint16* memPtrL = memPtrA - memSizeA + 1;
	uint32 iL;
	uint16 sumL = 0;
	uint16 csaL = 0;

	bbs_memWrite16( &csaL, memPtrA );
	for( iL = 0; iL < memSizeA; iL++ )
	{
		uint16 valL = 0;
		memPtrL += bbs_memRead16( &valL, memPtrL );
		sumL += valL;
	}
	csaL = chkSumA - sumL;

	return bbs_memWrite16( &csaL, memPtrA );
}

/* ------------------------------------------------------------------------- */

uint32 bpi_memReadCsa16( const uint16* memPtrA )
{
	return bbs_SIZEOF16( uint16 );
}

/* ------------------------------------------------------------------------- */

