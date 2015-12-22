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
#include "b_TensorEm/Mat.h"

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

void bts_Mat_init( struct bbs_Context* cpA,
				   struct bts_Mat* ptrA )
{
	bts_VectorMap_init( cpA, &ptrA->baseE );
	ptrA->baseE.typeE = ( uint32 )bts_VM_MAT;
	ptrA->baseE.vpMapE = bts_Mat_map;

	bts_CompactMat_init( cpA, &ptrA->matE );
}

/* ------------------------------------------------------------------------- */

void bts_Mat_exit( struct bbs_Context* cpA,
				   struct bts_Mat* ptrA )
{
	bts_CompactMat_exit( cpA, &ptrA->matE );

	bts_VectorMap_exit( cpA, &ptrA->baseE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bts_Mat_copy( struct bbs_Context* cpA,
				   struct bts_Mat* ptrA, 
				   const struct bts_Mat* srcPtrA )
{
	bts_CompactMat_copy( cpA, &ptrA->matE, &srcPtrA->matE );
}

/* ------------------------------------------------------------------------- */

flag bts_Mat_equal( struct bbs_Context* cpA,
					const struct bts_Mat* ptrA, 
					const struct bts_Mat* srcPtrA )
{
	bbs_ERROR0( "bts_Mat_equal:\n Function is not available" );
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
	
uint32 bts_Mat_memSize( struct bbs_Context* cpA,
					    const struct bts_Mat* ptrA )
{
	uint32 memSizeL = bbs_SIZEOF16( uint32 ) +
					  bbs_SIZEOF16( uint32 ); /* version */

	memSizeL += bts_VectorMap_memSize( cpA, &ptrA->baseE );
	memSizeL += bts_CompactMat_memSize( cpA, &ptrA->matE );

	return memSizeL; 
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_Mat_memWrite( struct bbs_Context* cpA,
						 const struct bts_Mat* ptrA, 
						 uint16* memPtrA )
{
	uint32 memSizeL = bts_Mat_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_MAT_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memWrite( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bts_CompactMat_memWrite( cpA, &ptrA->matE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */

uint32 bts_Mat_memRead( struct bbs_Context* cpA,
						struct bts_Mat* ptrA, 
						const uint16* memPtrA, 
						struct bbs_MemTbl* mtpA )
{
	uint32 memSizeL, versionL;
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL = bbs_MemTbl_segPtr( cpA, &memTblL, 0 );

	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_MAT_VERSION, memPtrA );
	memPtrA += bts_VectorMap_memRead( cpA, &ptrA->baseE, memPtrA );
	memPtrA += bts_CompactMat_memRead( cpA, &ptrA->matE, memPtrA, espL );

	if( memSizeL != bts_Mat_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_Mat_memRead( struct bem_ScanGradientMove* ptrA, const uint16* memPtrA ):\n"
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

void bts_Mat_map( struct bbs_Context* cpA, 
				  const struct bts_VectorMap* ptrA, 
				  const struct bts_Flt16Vec* inVecPtrA,
				  struct bts_Flt16Vec* outVecPtrA )
{
	bbs_DEF_fNameL( "bts_Mat_map" )
	const struct bts_Mat* ptrL = ( const struct bts_Mat* )ptrA;

	if( inVecPtrA->arrE.sizeE != ptrL->matE.widthE )
	{
		bbs_ERROR1( "%s:\ninput vector has incorrect size", fNameL );
		return;
	}

	if( outVecPtrA->arrE.allocatedSizeE < ptrL->matE.heightE )
	{
		bbs_ERROR1( "%s:\noutput vector is insufficiently allocated", fNameL );
		return;
	}

	bts_Flt16Vec_size( cpA, outVecPtrA, ptrL->matE.heightE );

	{
		int16 expL = 0;
		int32 outExpL = inVecPtrA->expE;
		bts_CompactMat_map( cpA, &ptrL->matE, inVecPtrA->arrE.arrPtrE, outVecPtrA->arrE.arrPtrE, &expL );
		outExpL += expL;

		/* precision underflow */
		if( outExpL < -32767 ) bts_Flt16Vec_setZero( cpA, outVecPtrA );
	}

	bts_Flt16Vec_maximizeMantisse( cpA, outVecPtrA );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

