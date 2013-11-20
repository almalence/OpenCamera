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

#ifndef bbf_SEQUENCE_EM_H
#define bbf_SEQUENCE_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/Int32Arr.h"
#include "b_BasicEm/UInt16Arr.h"
#include "b_BasicEm/MemTbl.h"
#include "b_BitFeatureEm/Feature.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/** data format version number */
#define bbf_SEQUENCE_VERSION 101

/** maximum sequence size */
#define bbf_SEQUENCE_MAX_SIZE 16

/* ---- object definition -------------------------------------------------- */

/** inhomogenious sequence of features */
struct bbf_Sequence 
{
	/* ---- public data ---------------------------------------------------- */

	/** base element (must be first element) */
	struct bbf_Feature baseE;

	/* ---- private data --------------------------------------------------- */

	/** feature pointer arrray */
	struct bbf_Feature* ftrPtrArrE[ bbf_SEQUENCE_MAX_SIZE ];

	/* ---- public data ---------------------------------------------------- */

	/** sequence size */
	uint32 sizeE;

	/** array of thresholds (4.28) */
	struct bbs_Int32Arr thrArrE;

	/** array of weights (0.16); value 1.0 is saturated to 0xFFFF */
	struct bbs_UInt16Arr wgtArrE;

	/** data array (contains feature elements) */
	struct bbs_UInt16Arr dataArrE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbf_Sequence  */
void bbf_Sequence_init( struct bbs_Context* cpA,
					   struct bbf_Sequence* ptrA );

/** resets bbf_Sequence  */
void bbf_Sequence_exit( struct bbs_Context* cpA,
					   struct bbf_Sequence* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbf_Sequence_copy( struct bbs_Context* cpA,
 					   struct bbf_Sequence* ptrA, 
					   const struct bbf_Sequence* srcPtrA );

/** equal operator */
flag bbf_Sequence_equal( struct bbs_Context* cpA,
						const struct bbf_Sequence* ptrA, 
						const struct bbf_Sequence* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bbf_Sequence_memSize( struct bbs_Context* cpA,
						    const struct bbf_Sequence* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bbf_Sequence_memWrite( struct bbs_Context* cpA,
							 const struct bbf_Sequence* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bbf_Sequence_memRead( struct bbs_Context* cpA,
							struct bbf_Sequence* ptrA, 
							const uint16* memPtrA, 
							struct bbs_MemTbl* mtpA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** computes feature's activity (4.28) on the given patch */
int32 bbf_Sequence_activity( const struct bbf_Feature* ptrA, const uint32* patchA );

#endif /* bbf_SEQUENCE_EM_H */

