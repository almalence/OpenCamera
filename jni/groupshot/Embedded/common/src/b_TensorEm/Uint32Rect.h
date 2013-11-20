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

#ifndef bts_UINT32RECT_EM_H
#define bts_UINT32RECT_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** 2d vector */
struct bts_Uint32Rect 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** upper left component */
	uint32 x1E;

	/** upper left component */
	uint32 y1E;

	/** lower right component */
	uint32 x2E;

	/** lower right component */
	uint32 y2E;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_Uint32Rect_memSize( struct bbs_Context* cpA,
							   const struct bts_Uint32Rect* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_Uint32Rect_memWrite( struct bbs_Context* cpA,
							    const struct bts_Uint32Rect* ptrA, 
								uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_Uint32Rect_memRead( struct bbs_Context* cpA,
							   struct bts_Uint32Rect* ptrA, 
							   const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */


#endif /* bts_UINT32RECT_EM_H */

