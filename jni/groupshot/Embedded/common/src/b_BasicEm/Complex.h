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

#ifndef bbs_COMPLEX_H
#define bbs_COMPLEX_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/Phase.h"

/* ---- related objects  --------------------------------------------------- */

struct bbs_APh;

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** Complex object */
struct bbs_Complex 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** real part */
	int16 realE;
	
	/** imaginary part */
	int16 imagE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/* ---- \ghd{ operators } -------------------------------------------------- */

/** equal operator */
flag bbs_Complex_equal( struct bbs_Complex compl1A, struct bbs_Complex compl2A );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size in 16-bit words object needs when written to memory */
uint32 bbs_Complex_memSize( struct bbs_Context* cpA,
						    struct bbs_Complex complA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bbs_Complex_memWrite( struct bbs_Context* cpA,
							 const struct bbs_Complex* ptrA, 
							 uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bbs_Complex_memRead( struct bbs_Context* cpA,
						    struct bbs_Complex* ptrA, 
							const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** conjugated value */
struct bbs_Complex bbs_Complex_conj( struct bbs_Complex complA );

/** returns squared abs value */
uint32 bbs_Complex_abs2( struct bbs_Complex complA );

/** returns absolute value */
uint16 bbs_Complex_abs( struct bbs_Complex complA );

/** returns phase value */
phase16 bbs_Complex_phase( struct bbs_Complex complA );

/** imports abs-phase value */
void bbs_Complex_importAPh( struct bbs_Complex* dstPtrA, const struct bbs_APh* srcPtrA );

/* ------------------------------------------------------------------------- */

#endif /* bbs_COMPLEX_H */
