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

#ifndef bpi_FACE_FINDER_REF_EM_H
#define bpi_FACE_FINDER_REF_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/UInt16Arr.h"
#include "b_APIEm/FaceFinder.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** type independent reference to a faceFinder module */
struct bpi_FaceFinderRef 
{
	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** object buffer */
	struct bbs_UInt16Arr objBufE;

	/** faceFinder pointer */
	struct bpi_FaceFinder* faceFinderPtrE;

	/* ---- functions ------------------------------------------------------ */

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bpi_FaceFinderRef  */
void bpi_FaceFinderRef_init( struct bbs_Context* cpA,
							 struct bpi_FaceFinderRef* ptrA );

/** resets bpi_FaceFinderRef  */
void bpi_FaceFinderRef_exit( struct bbs_Context* cpA,
 							 struct bpi_FaceFinderRef* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bpi_FaceFinderRef_copy( struct bbs_Context* cpA,
							 struct bpi_FaceFinderRef* ptrA, 
							 const struct bpi_FaceFinderRef* srcPtrA );

/** equal operator */
flag bpi_FaceFinderRef_equal( struct bbs_Context* cpA,
							  const struct bpi_FaceFinderRef* ptrA, 
							  const struct bpi_FaceFinderRef* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bpi_FaceFinderRef_memSize( struct bbs_Context* cpA,
						          const struct bpi_FaceFinderRef* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bpi_FaceFinderRef_memWrite( struct bbs_Context* cpA,
							       const struct bpi_FaceFinderRef* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bpi_FaceFinderRef_memRead( struct bbs_Context* cpA,
							      struct bpi_FaceFinderRef* ptrA, 
								  uint32 maxImageWidthA,
								  uint32 maxImageHeightA,
								  const uint16* memPtrA,
								  struct bbs_MemTbl* mtpA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** sets detection range */ 
void bpi_FaceFinderRef_setRange( struct bbs_Context* cpA,
								 struct bpi_FaceFinderRef* ptrA, 
								 uint32 minEyeDistanceA,
								 uint32 maxEyeDistanceA );

/** single face processing function; returns confidence (8.24) */ 
int32 bpi_FaceFinderRef_process( struct bbs_Context* cpA,
							     const struct bpi_FaceFinderRef* ptrA, 
								 struct bpi_DCR* dcrPtrA );

/** multiple face processing function; returns number of faces detected */ 
int32 bpi_FaceFinderRef_putDcr( struct bbs_Context* cpA,
							 	const struct bpi_FaceFinderRef* ptrA, 
								struct bpi_DCR* dcrPtrA );

/** retrieves indexed face from face finder after calling PutDCR */ 
void bpi_FaceFinderRef_getDcr( struct bbs_Context* cpA,
							   const struct bpi_FaceFinderRef* ptrA, 
							   uint32 indexA,
							   struct bpi_DCR* dcrPtrA );

#endif /* bpi_FACE_FINDER_REF_EM_H */

