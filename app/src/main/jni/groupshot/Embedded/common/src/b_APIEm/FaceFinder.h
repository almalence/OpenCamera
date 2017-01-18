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

#ifndef bpi_FACE_FINDER_EM_H
#define bpi_FACE_FINDER_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"
#include "b_BasicEm/MemTbl.h"
#include "b_TensorEm/Flt16Vec.h"
#include "b_TensorEm/IdCluster2D.h"
#include "b_APIEm/DCR.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/** Object Type */
enum bpi_FaceFinderType
{
	bpi_FF_UNDEFINED = 0,
	bpi_FF_BF_FACE_FINDER    /* bitfeature based faceFinder */
};

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** base object for face finder modules (occurs as first element in all face finder objects) */
struct bpi_FaceFinder 
{
	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** object type */
	uint32 typeE;

	/* ---- virtual functions ---------------------------------------------- */

	/** initializes some parameters prior to reading */ 
	void ( *vpSetParamsE )( struct bbs_Context* cpA,
							struct bpi_FaceFinder* ptrA, 
							uint32 maxImageWidthA,
							uint32 maxImageHeightA );

	/** sets detection range */ 
	void ( *vpSetRangeE )( struct bbs_Context* cpA,
						   struct bpi_FaceFinder* ptrA, 
						   uint32 minEyeDistanceA,
						   uint32 maxEyeDistanceA );

	/** single face processing function; returns confidence (8.24) */ 
	int32 ( *vpProcessE )( struct bbs_Context* cpA,
						   const struct bpi_FaceFinder* ptrA, 
						   struct bpi_DCR* dcrPtrA );

	/** multiple face processing function; returns number of faces detected */ 
	int32 ( *vpPutDcrE )( struct bbs_Context* cpA,
						  const struct bpi_FaceFinder* ptrA, 
						  struct bpi_DCR* dcrPtrA );

	/** retrieves indexed face from face finder after calling PutDCR */ 
	void ( *vpGetDcrE )( struct bbs_Context* cpA,
						 const struct bpi_FaceFinder* ptrA, 
						 uint32 indexA,
						 struct bpi_DCR* dcrPtrA );

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bpi_FaceFinder  */
void bpi_FaceFinder_init( struct bbs_Context* cpA,
				          struct bpi_FaceFinder* ptrA );

/** resets bpi_FaceFinder  */
void bpi_FaceFinder_exit( struct bbs_Context* cpA,
 				          struct bpi_FaceFinder* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bpi_FaceFinder_copy( struct bbs_Context* cpA,
					      struct bpi_FaceFinder* ptrA, 
					      const struct bpi_FaceFinder* srcPtrA );

/** equal operator */
flag bpi_FaceFinder_equal( struct bbs_Context* cpA,
						   const struct bpi_FaceFinder* ptrA, 
						   const struct bpi_FaceFinder* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** word size (16-bit) object needs when written to memory */
uint32 bpi_FaceFinder_memSize( struct bbs_Context* cpA,
						       const struct bpi_FaceFinder* ptrA );

/** writes object to memory; returns number of words (16-bit) written */
uint32 bpi_FaceFinder_memWrite( struct bbs_Context* cpA,
							    const struct bpi_FaceFinder* ptrA, uint16* memPtrA );

/** reads object from memory; returns number of words (16-bit) read */
uint32 bpi_FaceFinder_memRead( struct bbs_Context* cpA,
							   struct bpi_FaceFinder* ptrA, const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** virtual init function  */
void bpi_faceFinderInit( struct bbs_Context* cpA,
						 struct bpi_FaceFinder* ptrA,
						 enum bpi_FaceFinderType typeA );

/** virtual exit function */
void bpi_faceFinderExit( struct bbs_Context* cpA, 
						 struct bpi_FaceFinder* ptrA );

/** virtual mem size function */
uint32 bpi_faceFinderMemSize( struct bbs_Context* cpA, 
						      const struct bpi_FaceFinder* ptrA );

/** virtual mem write function */
uint32 bpi_faceFinderMemWrite( struct bbs_Context* cpA, 
 						       const struct bpi_FaceFinder* ptrA, 
							   uint16* memPtrA );

/** virtual mem read function */
uint32 bpi_faceFinderMemRead( struct bbs_Context* cpA,
 							  struct bpi_FaceFinder* ptrA, 
							  const uint16* memPtrA,
							  struct bbs_MemTbl* mtpA );

/** virtual sizeof operator for 16bit units */
uint32 bpi_faceFinderSizeOf16( struct bbs_Context* cpA, 
							   enum bpi_FaceFinderType typeA );

#endif /* bpi_FACE_FINDER_EM_H */

