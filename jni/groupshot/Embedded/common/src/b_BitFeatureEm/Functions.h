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

#ifndef bbf_FUNCTIONS_EM_H
#define bbf_FUNCTIONS_EM_H

/**
 * This files contains gerneral purpose functions.
 */

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_TensorEm/Int16Rect.h"
#include "b_ImageEm/UInt32Image.h"
#include "b_ImageEm/UInt16ByteImage.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/** table to quickly determine the number of set bits in an 8 bit variable */
extern const uint16 bbf_bit8TblG[ 256 ];

/* ---- external functions ------------------------------------------------- */

/** sums up bits in 8 bit variable */
#define bbf_BIT_SUM_8( vA ) ( bbf_bit8TblG[ vA ] )

/** sums up bits in 16 bit variable */
#define bbf_BIT_SUM_16( vA ) ( bbf_bit8TblG[ vA & 0x00FF ] + bbf_bit8TblG[ ( vA >> 8 ) & 0x00FF ] )

/** sums up bits in 16 bit variable */
#define bbf_BIT_SUM_32( vA ) ( bbf_bit8TblG[ vA & 0x00FF ] + bbf_bit8TblG[ ( vA >> 8 ) & 0x00FF ]  + bbf_bit8TblG[ ( vA >> 16 ) & 0x00FF ] + bbf_bit8TblG[ ( vA >> 24 ) & 0x00FF ] )


#endif /* bbf_FUNCTIONS_EM_H */

