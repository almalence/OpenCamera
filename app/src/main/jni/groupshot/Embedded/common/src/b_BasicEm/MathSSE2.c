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

#include "b_BasicEm/Basic.h" /* to disable some warnings in VC++ */

#if ( defined( WIN64 ) || defined( HW_SSE2 ) )

#include "emmintrin.h"

/* disable warning "local variable 'x' used without having been initialized" */
#pragma warning( disable : 4700 )


/** Using half register (64-bit) in SSE2 to calculate dot product.
 *  This is a SSE2 reimplementation of bbs_dotProduct_intelMMX16 in Math.c.
 *  Dependencies: input vectors need to be 16-bit aligned
 *  Return Value: int32 containing resultL of dot product
 */
int32 bbs_dotProduct_64SSE2( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
	__m128i m_XMM0, m_XMM1, m_XMM2, m_XMM3, m_XMM4, m_XMM5, m_XMM6, m_XMM7, m_XMM8;
	int16* vec1L = ( int16* )vec1A;
	int16* vec2L = ( int16* )vec2A;

	int32 resultL = 0;
	uint32 alignOffSetL = 0;

	/* initialize registers to 0 */
	m_XMM4 = _mm_xor_si128( m_XMM4, m_XMM4 );
	m_XMM6 = _mm_xor_si128( m_XMM6, m_XMM6 );
	m_XMM7 = _mm_xor_si128( m_XMM7, m_XMM7 );

	alignOffSetL = sizeA % 16;
	sizeA >>= 4;

	if( sizeA )
	{
		while( sizeA > 0 )
		{
			m_XMM0 = _mm_loadl_epi64( (__m128i *)&0[vec1L] );
			m_XMM7 = _mm_add_epi32( m_XMM7, m_XMM4 );

			m_XMM1 = _mm_loadl_epi64( (__m128i *)&0[vec2L] );
			m_XMM7 = _mm_add_epi32( m_XMM7, m_XMM6 );

			m_XMM2 = _mm_loadl_epi64( (__m128i *)&4[vec1L] );

			m_XMM0 = _mm_madd_epi16( m_XMM0, m_XMM1 );

			m_XMM3 = _mm_loadl_epi64( (__m128i *)&4[vec2L] );
			m_XMM4 = _mm_loadl_epi64( (__m128i *)&8[vec1L] );

			m_XMM2 = _mm_madd_epi16( m_XMM2, m_XMM3 );

			m_XMM5 = _mm_loadl_epi64( (__m128i *)&8[vec2L] );

			m_XMM7 = _mm_add_epi32( m_XMM7, m_XMM0 );

			m_XMM6 = _mm_loadl_epi64( (__m128i *)&12[vec1L] );

			m_XMM4 = _mm_madd_epi16( m_XMM4, m_XMM5 );

			m_XMM8 = _mm_loadl_epi64( (__m128i *)&12[vec2L] );
			m_XMM6 = _mm_madd_epi16( m_XMM6, m_XMM8 );

			m_XMM7 = _mm_add_epi32( m_XMM7, m_XMM2 );

			vec1L += 16;
			vec2L += 16;
			sizeA--;
		}

		/* sum up accumulators */
		m_XMM7 = _mm_add_epi32( m_XMM7, m_XMM4 );

		m_XMM7 = _mm_add_epi32( m_XMM7, m_XMM6 );

		m_XMM0 = _mm_loadl_epi64( (__m128i *)&m_XMM7 );

		m_XMM0 = _mm_srli_epi64( m_XMM0, 32 );

		m_XMM7 = _mm_add_epi32( m_XMM7, m_XMM0 );

		resultL = _mm_cvtsi128_si32( m_XMM7 );
	}

	/* switch statements produces faster code than loop */
	switch( alignOffSetL )
	{
		case 15:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 14:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 13:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 12:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 11:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 10:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 9:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 8:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 7:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 6:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 5:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 4:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 3:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 2:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 1:
			resultL += ( int32 )*vec1L++ * *vec2L++;
	}

	return resultL;
}

/* ------------------------------------------------------------------------- */

/** Using full register (128-bit) in SSE2 to calculate dot Product.
 *  Dependencies: 16-bit aligned
 *  Return Value: int32 containing dot Product
 */
int32 bbs_dotProduct_128SSE2( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
	__m128i m_XMM0, m_XMM2, m_XMM3, m_XMM5, m_XMM6;
	int16* vec1L = ( int16* )vec1A;
	int16* vec2L = ( int16* )vec2A;

	int32 resultL = 0;
	uint32 alignOffSetL = 0;

	m_XMM5 = _mm_xor_si128( m_XMM5, m_XMM5 );
	m_XMM6 = _mm_xor_si128( m_XMM6, m_XMM6 );

	alignOffSetL = sizeA % 16;
	sizeA >>= 4;

	if( sizeA )
	{
		while( sizeA > 0 )
		{
			m_XMM0 = _mm_load_si128( (__m128i *)&0[vec1L] );
			m_XMM5 = _mm_add_epi32( m_XMM5, m_XMM6 );

			m_XMM2 = _mm_load_si128( (__m128i *)&0[vec2L] );

			m_XMM6 = _mm_load_si128( (__m128i *)&8[vec1L] );

			m_XMM0 = _mm_madd_epi16( m_XMM0, m_XMM2 );

			m_XMM5 = _mm_add_epi32( m_XMM5, m_XMM0 );

			m_XMM3 = _mm_load_si128( (__m128i *)&8[vec2L] );

			m_XMM6 = _mm_madd_epi16( m_XMM6, m_XMM3 );

			vec1L += 16;
			vec2L += 16;
			sizeA--;
		}

		/* sum up accumulators */
		m_XMM5 = _mm_add_epi32( m_XMM5, m_XMM6 );

		m_XMM0 = _mm_load_si128( (__m128i *)&m_XMM5 );

		resultL = _mm_cvtsi128_si32( m_XMM0 );	/* 1st 32bits */

		m_XMM0 = _mm_srli_si128( m_XMM0, 4 );

		resultL += _mm_cvtsi128_si32( m_XMM0 );	/* 2nd 32bits */

		m_XMM0 = _mm_srli_si128( m_XMM0, 4 );

		resultL += _mm_cvtsi128_si32( m_XMM0 );	/* 3rd 32bits */

		m_XMM0 = _mm_srli_si128( m_XMM0, 4 );

		resultL += _mm_cvtsi128_si32( m_XMM0 );	/* 4th 32bits */
	}

	switch( alignOffSetL )
	{
		case 15:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 14:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 13:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 12:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 11:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 10:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 9:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 8:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 7:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 6:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 5:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 4:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 3:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 2:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 1:
			resultL += ( int32 )*vec1L++ * *vec2L++;
	}

	return resultL;
}

/* ------------------------------------------------------------------------- */


/** Using full register (128-bit) in SSE2 to calculate dot product (non aligned version).
 *  Dependencies: memory does not need to be 16-bit aligned
 *  Return Value: int32 containing dot product
 */
int32 bbs_dotProduct_u128SSE2( const int16* vec1A, const int16* vec2A, uint32 sizeA )
{
	__m128i m_XMM0, m_XMM2, m_XMM3, m_XMM5, m_XMM6;
	int16* vec1L = ( int16* )vec1A;
	int16* vec2L = ( int16* )vec2A;
	int32 resultL = 0;
	uint32 alignOffSetL = 0;

	/* initialize registers to 0 */
	m_XMM5 = _mm_xor_si128( m_XMM5, m_XMM5 );
	m_XMM6 = _mm_xor_si128( m_XMM6, m_XMM6 );


	alignOffSetL = sizeA % 16;
	sizeA >>= 4;

	if( sizeA )
	{
		while( sizeA > 0 )
		{
			m_XMM0 = _mm_loadu_si128( (__m128i *)&0[vec1L] );
			m_XMM5 = _mm_add_epi32( m_XMM5, m_XMM6 );

			m_XMM2 = _mm_loadu_si128( (__m128i *)&0[vec2L] );

			m_XMM6 = _mm_loadu_si128( (__m128i *)&8[vec1L] );

			m_XMM0 = _mm_madd_epi16( m_XMM0, m_XMM2 );

			m_XMM5 = _mm_add_epi32( m_XMM5, m_XMM0 );

			m_XMM3 = _mm_loadu_si128( (__m128i *)&8[vec2L] );

			m_XMM6 = _mm_madd_epi16( m_XMM6, m_XMM3 );

			vec1L += 16;
			vec2L += 16;
			sizeA--;
		}

		/* sum up accumulators */
		m_XMM5 = _mm_add_epi32( m_XMM5, m_XMM6 );
	        
		m_XMM0 = _mm_loadu_si128( (__m128i *)&m_XMM5 );

		resultL = _mm_cvtsi128_si32( m_XMM0 );	/* 1st 32bits */

		m_XMM0 = _mm_srli_si128( m_XMM0, 4 );

		resultL += _mm_cvtsi128_si32( m_XMM0 );	/* 2nd 32bits */

		m_XMM0 = _mm_srli_si128( m_XMM0, 4 );

		resultL += _mm_cvtsi128_si32( m_XMM0 );	/* 3rd 32bits */

		m_XMM0 = _mm_srli_si128( m_XMM0, 4 );

		resultL += _mm_cvtsi128_si32( m_XMM0 );	/* 4th 32bits */
	}


	switch( alignOffSetL )
	{
		case 15:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 14:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 13:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 12:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 11:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 10:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 9:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 8:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 7:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 6:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 5:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 4:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 3:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 2:
			resultL += ( int32 )*vec1L++ * *vec2L++;
		case 1:
			resultL += ( int32 )*vec1L++ * *vec2L++;
	}

	return resultL;
}

/* ------------------------------------------------------------------------- */

#endif /* HW_SSE2 */
