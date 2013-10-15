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

#ifndef bbs_MEMORY_EM_H
#define bbs_MEMORY_EM_H

/**
 * This files contains memory related functions.
 */

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/** copies memory for src to dst (no overlap allowed); returns dstA
  * src & dst data must be 16 bit aligned
  */
/* void* bbs_memcpy( void* dstA, const void* srcA, uint32 sizeA ); */

/** copies memory for src to dst (no overlap allowed), size is given in 16-bit words
  * src & dst data must be 16 bit aligned
  * returns dstA
  */
void* bbs_memcpy16( void* dstA, const void* srcA, uint32 sizeA );

/** copies memory for src to dst (no overlap allowed), size is given in 32-bit words
  * src & dst data must be 32 bit aligned
  * returns dstA
  */
void* bbs_memcpy32( void* dstA, const void* srcA, uint32 sizeA );

/** fills memory with a value, size is given in 16-bit words
  * dst data must be 16 bit aligned
  * returns dstA
  */
void* bbs_memset16( void* dstA, uint16 valA, uint32 sizeA );

/** fills memory with a value, size is given in 32-bit words
  * dst data must be 32 bit aligned
  * returns dstA
  */
void* bbs_memset32( void* dstA, uint32 valA, uint32 sizeA );

#endif /* bbs_MEMORY_EM_H */

