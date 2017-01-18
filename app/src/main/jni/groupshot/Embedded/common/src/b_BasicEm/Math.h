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

#ifndef bbs_MATH_EM_H
#define bbs_MATH_EM_H

/**
 * This files contains mathematical functions.
 */

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"
#ifdef HW_TMS320C5x
#include "Dsplib.h"
#endif

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- macros ------------------------------------------------------------- */

/** computes the maximum of two variables */
#define bbs_max( val1A, val2A ) ( ( val1A ) > ( val2A ) ? ( val1A ) : ( val2A ) )

/** computes the minimum of two variables */
#define bbs_min( val1A, val2A ) ( ( val1A ) < ( val2A ) ? ( val1A ) : ( val2A ) )

/** computes the absolute value */
#define bbs_abs( valA ) ( ( valA ) > 0 ? ( valA ) : -( valA ) )

/* ---- external functions ------------------------------------------------- */

/** 
 * Computes square root from 32 bit value.
 * The return value 'r' is the largest possible integer that 
 * satisfies r * r <= valA.
 * This behavior is identical with (uint16)sqrt( valA ).
 * C6201: 162 cycles
 */
uint16 bbs_sqrt32( uint32 valA );

/** 
 * Computes square root from 16 bit value.
 * The return value 'r' is the largest possible integer that 
 * satisfies r * r <= valA.
 * This behavior is identical with (uint8)sqrt( valA ).
 */
uint8 bbs_sqrt16( uint16 valA );

/** Sqrt approximation */
uint16 bbs_fastSqrt32( uint32 valA );

/** sqrt(1/x) approximation 
 *  return format 1.31
 */
uint32 bbs_invSqrt32( uint32 valA );

/** 1/x approximation
 * return format 2.30 
 */
int32 bbs_inv32( int32 valA );

/** Returns integer log2 of valA
 * C6201: 24 cycles
 */
uint32 bbs_intLog2( uint32 valA );

/** 
 * Returns (2^x) - 1 for a value range of [0,1[
 * Format of valA: 0.32
 * Format of return value: 0.32 
 */
uint32 bbs_pow2M1( uint32 valA );

/** 
 * Returns (2^x) for a value range of [-16,16[
 * Format of valA: 5.27
 * Format of return value: 16.16
 */
uint32 bbs_pow2( int32 valA );


/** 
 * Returns (e^x) for a value range of [-11.0903,11.0903]
 * If valA is smaller than -11.0903, the function returns 0
 * If valA is larger than 11.0903, the function returns ( 2^32 - 1 ) / ( 2^16 )
 * Format of valA: 5.27
 * Format of return value: 16.16
 * C6201: 72 cycles
 */
uint32 bbs_exp( int32 valA );

/** saturates a signed 32 bit value to signed 16 bit */
int16 bbs_satS16( int32 valA );

/** 
 * Returns the value after rounding to the nearest integer.
 */
/*	int32 bbs_round( int32 valA, int32 bbpA );	*/

/** 
 * Computes the dot product of vec1A with vec2A, both of size sizeA. 
 * (no overflow handling, slow for sizeA < 32 )
 */
int32 bbs_dotProductInt16( const int16* vec1A, const int16* vec2A, uint32 sizeA );

/** Fermi function ( 1.0 / ( 1.0 + exp( -valA ) ) )
 *  Format valA: 16.16 
 *  Format return: 2.30 
 */
int32 bbs_fermi( int32 valA );

/** reduces uint32 to N bits; if it has already <= N bits, nothing happens */
void bbs_uint32ReduceToNBits( uint32* argPtrA, int32* bbpPtrA, uint32 nBitsA );

/** reduces int32 to N bits; if it has already <= N bits, nothing happens */
void bbs_int32ReduceToNBits( int32* argPtrA, int32* bbpPtrA, uint32 nBitsA );

/** converts a number with source bbp to a 32 bit number with dst bbp; 
 *  applies appropriate shifting, rounding and saturation to minimize overflow-damage
 */
uint32 bbs_convertU32( uint32 srcA, int32 srcBbpA, int32 dstBbpA );

/** converts a number with source bbp to a 32 bit number with dst bbp; 
 *  applies appropriate shifting, rounding and saturation to minimize overflow-damage
 */
int32 bbs_convertS32( int32 srcA, int32 srcBbpA, int32 dstBbpA );

/** vector power return val = sum(xA_i^2), input 1.15, output 1.30 */
int32 bbs_vecPowerFlt16( const int16 *xA, int16 nxA );

/** returns floating point squared norm of 32 bit vector (maximum accuracy - overflow-safe); 
 *  Function is slow
 *  returned square norm = man * 2^exp
 *  The returned exponent is always even
 */
void bbs_vecSqrNorm32( const int32* vecA, uint32 sizeA, uint32* manPtrA, uint32* expPtrA );

/** returns floating point squared norm of 16 bit vector (maximum accuracy - overflow-safe); 
 *  returned square norm = man * 2^exp
 *  The returned exponent is always even
 */
void bbs_vecSqrNorm16( const int16* vecA, uint32 sizeA, uint32* manPtrA, uint32* expPtrA );

/** returns the norm of a 16 bit vector; 
 *  overflow-safe when sizeA < 65535
 */
uint32 bbs_vecNorm16( const int16* vecA, uint32 sizeA );

/** multiplies two unsigned 32 bit values and returns product decomposed to mantisse and exponent  
 *  maximum accuracy - overflow-safe
 *  exponent is always >= 0
 */
void bbs_mulU32( uint32 v1A, uint32 v2A, uint32* manPtrA, int32* expPtrA );

/** multiplies two signed 32 bit values and returns product decomposed to mantisse and exponent  
 *  maximum accuracy - overflow-safe
 *  exponent is always >= 0
 */
void bbs_mulS32( int32 v1A, int32 v2A, int32* manPtrA, int32* expPtrA );

/** matrix multiply rA = x1A * x2A, input/output 1.15, no overflow protection, in-place not allowed */
void bbs_matMultiplyFlt16( const int16 *x1A, int16 row1A, int16 col1A, 
						   const int16 *x2A, int16 col2A, int16 *rA );

/** matrix multiply rA = x1A * transposed( x2A ), input/output 1.15, no overflow protection, in-place not allowed */
void bbs_matMultiplyTranspFlt16( const int16 *x1A, int16 row1A, int16 col1A, 
								 const int16 *x2A, int16 row2A, int16 *rA );

/*
#ifdef mtrans
#define bbs_matTrans mtrans
#else
uint16 bbs_matTrans( int16 *xA, int16 rowA, int16 colA, int16 *rA );
#endif

#ifdef atan2_16
#define bbs_vecPhase atan2_16
#else
uint16 bbs_vecPhase( int16* reA, int16* imA, int16* phaseA, uint16 sizeA );
#endif
*/

#endif /* bbs_MATH_EM_H */

