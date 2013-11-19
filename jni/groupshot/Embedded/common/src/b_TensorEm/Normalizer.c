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

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Functions.h"
#include "b_BasicEm/Math.h"
#include "b_TensorEm/Normalizer.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Normalizer_init( struct bbs_Context* cpA,
					      struct bts_Normalizer* ptrA )
{
	bts_VectorMap_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bts_VM_NORMALIZER;
	ptrA->baseE.vpMapE = bts_Normalizer_map;
}

/* ------------------------------------------------------------------------- */

void bts_Normalizer_exit( struct bbs_Context* cpA,
					      struct bts_Normalizer* ptrA )
{
	bts_VectorMap_exit( cpA, &ptrA->baseE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Normalizer_copy( struct bbs_Context* cpA,
						  struct bts_Normalizer* ptrA, 
					      const struct bts_Normalizer* srcPtrA )
{
}

/* ------------------------------------------------------------------------- */

flag bts_Normalizer_equal( struct bbs_Context* cpA,
						   const struct bts_Normalizer* ptrA, 
						   const struct bts_Normalizer* srcPtrA )
{
	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_Normalizer_memSize( struct bbs_Context* cpA,
							   const struct bts_Normalizer* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */
	memSizeL += bts_VectorMap_memSize( cpA, &ptrA->baseE );
	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Normalizer_memWrite( struct bbs_Context* cpA,
								const struct bts_Normalizer* ptrA, 
								uint16* memPtrA )
{
	uint32 memSizeL = bts_Normalizer_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_NORMALIZER_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memWrite( cpA, &ptrA->baseE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bts_Normalizer_memRead( struct bbs_Context* cpA,
							   struct bts_Normalizer* ptrA, 
							   const uint16* memPtrA, 
							   struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_NORMALIZER_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memRead( cpA, &ptrA->baseE, memPtrA );

	if( memSizeL != bts_Normalizer_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_Normalizer_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
			        "size mismatch" );
		return 0;
	}

	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

void bts_Normalizer_map( struct bbs_Context* cpA, 
						 const struct bts_VectorMap* ptrA, 
						 const struct bts_Flt16Vec* inVecPtrA,
						 struct bts_Flt16Vec* outVecPtrA )
{
	bts_Flt16Vec_copy( cpA, outVecPtrA, inVecPtrA );
	bts_Flt16Vec_normalize( cpA, outVecPtrA );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

