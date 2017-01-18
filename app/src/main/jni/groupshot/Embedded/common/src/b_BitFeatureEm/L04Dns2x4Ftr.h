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

#ifndef bbf_L04_DNS_2X4_FTR_EM_H
#define bbf_L04_DNS_2X4_FTR_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/UInt32Arr.h"
#include "b_BasicEm/MemTbl.h"
#include "b_BitFeatureEm/Feature.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bbf_L04_DNS_2X4_FTR_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** discrete feature set */
struct bbf_L04Dns2x4Ftr 
{
	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** base element */
	struct bbf_Feature baseE;

	/** data array */
	struct bbs_UInt32Arr dataArrE;

	/** factor to convert activity to proper range */
	uint32 activityFactorE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbf_L04Dns2x4Ftr  */
void bbf_L04Dns2x4Ftr_init( struct bbs_Context* cpA,
						    struct bbf_L04Dns2x4Ftr* ptrA );

/** resets bbf_L04Dns2x4Ftr  */
void bbf_L04Dns2x4Ftr_exit( struct bbs_Context* cpA,
						    struct bbf_L04Dns2x4Ftr* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbf_L04Dns2x4Ftr_copy( struct bbs_Context* cpA,
						    struct bbf_L04Dns2x4Ftr* ptrA, 
						    const struct bbf_L04Dns2x4Ftr* srcPtrA );

/** equal operator */
flag bbf_L04Dns2x4Ftr_equal( struct bbs_Context* cpA,
						     const struct bbf_L04Dns2x4Ftr* ptrA, 
						     const struct bbf_L04Dns2x4Ftr* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bbf_L04Dns2x4Ftr_memSize( struct bbs_Context* cpA,
							     const struct bbf_L04Dns2x4Ftr* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bbf_L04Dns2x4Ftr_memWrite( struct bbs_Context* cpA,
							      const struct bbf_L04Dns2x4Ftr* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bbf_L04Dns2x4Ftr_memRead( struct bbs_Context* cpA,
							     struct bbf_L04Dns2x4Ftr* ptrA, 
							     const uint16* memPtrA, 
							     struct bbs_MemTbl* mtpA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** computes feature's activity (4.28) on the given patch */
int32 bbf_L04Dns2x4Ftr_activity( const struct bbf_Feature* ptrA, const uint32* patchA );

#endif /* bbf_L04_DNS_2X4_FTR_EM_H */

