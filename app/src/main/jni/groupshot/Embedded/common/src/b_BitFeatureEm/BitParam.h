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

#ifndef bbf_BIT_PARAM_EM_H
#define bbf_BIT_PARAM_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** parameters for bit generation. */
struct bbf_BitParam 
{
	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** outer radius of filter block */
	uint32 outerRadiusE;
	
	/** inner radius of filter block */
	uint32 innerRadiusE;

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbf_BitParam  */
void bbf_BitParam_init( struct bbs_Context* cpA,
					    struct bbf_BitParam* ptrA );

/** resets bbf_BitParam  */
void bbf_BitParam_exit( struct bbs_Context* cpA,
					    struct bbf_BitParam* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbf_BitParam_copy( struct bbs_Context* cpA,
					    struct bbf_BitParam* ptrA, 
					    const struct bbf_BitParam* srcPtrA );

/** equal operator */
flag bbf_BitParam_equal( struct bbs_Context* cpA,
					     const struct bbf_BitParam* ptrA, 
					     const struct bbf_BitParam* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bbf_BitParam_memSize( struct bbs_Context* cpA,
						     const struct bbf_BitParam* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bbf_BitParam_memWrite( struct bbs_Context* cpA,
						      const struct bbf_BitParam* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bbf_BitParam_memRead( struct bbs_Context* cpA,
						     struct bbf_BitParam* ptrA, 
						     const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

#endif /* bbf_BIT_PARAM_EM_H */

