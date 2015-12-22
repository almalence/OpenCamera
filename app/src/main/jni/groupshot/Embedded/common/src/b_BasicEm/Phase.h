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

#ifndef bbs_PHASE_EM_H
#define bbs_PHASE_EM_H

/* ---- includes ----------------------------------------------------------- */

/** 
 * Phase data type.
 * This data type represents a phase or angle value and takes advantage 
 * of the circular value range when doing arithmetig with an integer
 * by ignoring overflow.
 * The phase value range lies within [ - PI, PI [;
 * The corresponding integer value range is [ MININT, MAXINT + 1 [.
 * The phase data type is to be used whereever an angle is needed.
 */

#include "b_BasicEm/Basic.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/** 8 bit phase value */
typedef int8 phase8;

/** 16 bit phase value */
typedef int16 phase16;

/** 32 bit phase value */
typedef int32 phase32;

/* ---- constants ---------------------------------------------------------- */

/** value PI in a phase16 expression */
#define bbs_M_PI_16 32768

/** value PI/2 in a phase16 expression */
#define bbs_M_PI_2_16 16384

/** value PI/4 in a phase16 expression */
#define bbs_M_PI_4_16 8192

/** value PI in a phase8 expression */
#define bbs_M_PI_8 128

/** value PI/2 in a phase8 expression */
#define bbs_M_PI_2_8 64

/** value ( 32768 / PI ) in the format 14.1 */
#define bbs_PHASE_MAX_BY_PI 20861

/** sine interpolation method */
#define bbs_SIN_INTERPOLATION_METHOD_2

/* ---- object definition -------------------------------------------------- */

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/** 
 * Computes sine of a phase
 * The return value has the format 8.24
 * The function approximates ( int32 )( sin( ( M_PI * phaseA ) / ( 1<<15 ) ) * ( 1<<24 ) )
 * Max error: 8.5E-5 (METHOD1); 7.0E-5 (METHOD2)
 * Std error: 4.4E-5 (METHOD1); 3.2E-5 (METHOD2) 
 */
int32 bbs_sin32( phase16 phaseA );

/** 
 * Computes cosine of a phase
 * The return value has the format 8.24
 * The function approximates ( int32 )( cos( ( M_PI * phaseA ) / ( 1<<15 ) ) * ( 1<<24 ) )
 * Max error: 8.5E-5 (METHOD1); 7.0E-5 (METHOD2)
 * Std error: 4.4E-5 (METHOD1); 3.2E-5 (METHOD2) 
 */
int32 bbs_cos32( phase16 phaseA );

/** 
 * Computes sine of a phase
 * The return value has the format 2.14
 * see sin32 for details
 */
int16 bbs_sin16( phase16 phaseA );

/** 
 * Computes cosine of a phase
 * The return value has the format 2.14
 * see cos32 for details
 */
int16 bbs_cos16( phase16 phaseA );

/** 
 * Computes arcus tangens between [0,1[, where valA has the format 16.16  
 * The function approximates ( int16 )( atan( double( valA ) / ( ( 1 << 16 ) ) / M_PI ) * ( 1 << 15 ) )
 * Max error: 5.1E-5 PI
 * Std error: 2.7E-5 PI
 */
phase16 bbs_atan16( uint32 valA );

/** 
 * Computes phase from a 2d vector as angle enclosed between vector and (0,0).
 * It is vec = ( cos( angle ), sin( angle ) );
 * Max error: 5.4E-5 PI
 * Std error: 2.9E-5 PI
 */
phase16 bbs_phase16( int32 xA, int32 yA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

#endif /* bbs_PHASE_EM_H */

