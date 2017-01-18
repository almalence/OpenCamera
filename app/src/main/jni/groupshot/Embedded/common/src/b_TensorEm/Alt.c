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
#include "b_TensorEm/Alt.h"

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

void bts_Alt_init( struct bbs_Context* cpA,
			       struct bts_Alt* ptrA )
{
	bts_VectorMap_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bts_VM_ALT;
	ptrA->baseE.vpMapE = bts_Alt_map;

	bts_CompactAlt_init( cpA, &ptrA->altE );
}

/* ------------------------------------------------------------------------- */

void bts_Alt_exit( struct bbs_Context* cpA,
			       struct bts_Alt* ptrA )
{
	bts_CompactAlt_exit( cpA, &ptrA->altE );

	bts_VectorMap_exit( cpA, &ptrA->baseE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Alt_copy( struct bbs_Context* cpA,
				   struct bts_Alt* ptrA, 
				   const struct bts_Alt* srcPtrA )
{
	bts_CompactAlt_copy( cpA, &ptrA->altE, &srcPtrA->altE );
}

/* ------------------------------------------------------------------------- */

flag bts_Alt_equal( struct bbs_Context* cpA,
					const struct bts_Alt* ptrA, 
					const struct bts_Alt* srcPtrA )
{
	bbs_ERROR0( "bts_Alt_equal:\n Function is not available" );
	return FALSE;
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
	
uint32 bts_Alt_memSize( struct bbs_Context* cpA,
					    const struct bts_Alt* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */
	memSizeL += bts_VectorMap_memSize( cpA, &ptrA->baseE );
	memSizeL += bts_CompactAlt_memSize( cpA, &ptrA->altE );
	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Alt_memWrite( struct bbs_Context* cpA,
						 const struct bts_Alt* ptrA, 
						 uint16* memPtrA )
{
	uint32 memSizeL = bts_Alt_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_ALT_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bts_CompactAlt_memWrite( cpA, &ptrA->altE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bts_Alt_memRead( struct bbs_Context* cpA,
						struct bts_Alt* ptrA, 
						const uint16* memPtrA, 
						struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_segPtr( cpA, &memTblL, 0 );

	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_ALT_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memRead( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bts_CompactAlt_memRead( cpA, &ptrA->altE, memPtrA, espL );

	if( memSizeL != bts_Alt_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_Alt_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
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

void bts_Alt_map( struct bbs_Context* cpA, 
				  const struct bts_VectorMap* ptrA, 
				  const struct bts_Flt16Vec* inVecPtrA,
				  struct bts_Flt16Vec* outVecPtrA )
{
	bbs_DEF_fNameL( "bts_Alt_map" )
	const struct bts_Alt* ptrL = ( const struct bts_Alt* )ptrA;

	if( inVecPtrA->arrE.sizeE != ptrL->altE.matE.widthE )
	{
		bbs_ERROR1( "%s:\ninput vector has incorrect size", fNameL );
		return;
	}

	if( outVecPtrA->arrE.allocatedSizeE < ptrL->altE.matE.heightE )
	{
		bbs_ERROR1( "%s:\noutput vector is insufficiently allocated", fNameL );
		return;
	}

	bts_Flt16Vec_size( cpA, outVecPtrA, ptrL->altE.matE.heightE );

	bts_CompactAlt_map( cpA, &ptrL->altE, inVecPtrA->arrE.arrPtrE, inVecPtrA->expE, outVecPtrA->arrE.arrPtrE, &outVecPtrA->expE );

	bts_Flt16Vec_maximizeMantisse( cpA, outVecPtrA );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

