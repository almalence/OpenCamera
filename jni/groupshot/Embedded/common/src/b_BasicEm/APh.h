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

#ifndef bbs_APH_H
#define bbs_APH_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/Phase.h"

/* ---- related objects  --------------------------------------------------- */

struct bbs_Complex;

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** Object representing absolute and phase value of a complex number */
struct bbs_APh 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** absolute value */
	uint16 absE;
	
	/** phase value */
	phase16 phaseE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/* ---- \ghd{ operators } -------------------------------------------------- */

/** equal operator */
flag bbs_APh_equal( struct bbs_APh aph1A, 
					struct bbs_APh aph2A );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size in 16-bit words object needs when written to memory */
uint32 bbs_APh_memSize( struct bbs_Context* cpA,
					    struct bbs_APh aPhA );

/** writes object to memory; returns number of 16-bit words written */
uint32 bbs_APh_memWrite( struct bbs_Context* cpA,
						 const struct bbs_APh* ptrA, 
						 uint16* memPtrA );

/** reads object from memory; returns number of 16-bit words read */
uint32 bbs_APh_memRead( struct bbs_Context* cpA,
					    struct bbs_APh* ptrA, 
						const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** conjugated value */
struct bbs_APh bbs_APh_conj( const struct bbs_APh aPhA );

/** imports complex value */
void bbs_APh_importComplex( struct bbs_APh* dstPtrA, 
							const struct bbs_Complex* srcPtrA );

/* ------------------------------------------------------------------------- */

#endif /* bbs_APH_H */
