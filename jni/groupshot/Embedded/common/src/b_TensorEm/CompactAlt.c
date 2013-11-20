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

#include "b_TensorEm/CompactAlt.h"
#include "b_TensorEm/Functions.h"
#include "b_BasicEm/Math.h"
#include "b_BasicEm/Functions.h"
#include "b_BasicEm/Memory.h"

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

void bts_CompactAlt_init( struct bbs_Context* cpA,
					      struct bts_CompactAlt* ptrA )
{
	bts_CompactMat_init( cpA, &ptrA->matE );
	bbs_Int16Arr_init( cpA, &ptrA->vecE );
	ptrA->vecExpE = 0;
}

/* ------------------------------------------------------------------------- */

void bts_CompactAlt_exit( struct bbs_Context* cpA,
					      struct bts_CompactAlt* ptrA )
{
	bts_CompactMat_exit( cpA, &ptrA->matE );
	bbs_Int16Arr_exit( cpA, &ptrA->vecE );
	ptrA->vecExpE = 0;
}
/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

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
	
void bts_CompactAlt_create( struct bbs_Context* cpA,
						    struct bts_CompactAlt* ptrA, 
						    uint32 widthA,
						    uint32 heightA,
						    uint32 bitsA,
							uint32 maxRowSizeA,
				            struct bbs_MemSeg* mspA )
{
	bts_CompactMat_create( cpA, &ptrA->matE, widthA, heightA, bitsA, maxRowSizeA, mspA );
	bbs_Int16Arr_create( cpA, &ptrA->vecE, heightA, mspA );
	bbs_Int16Arr_fill( cpA, &ptrA->vecE, 0 );
	ptrA->vecExpE = 0;
}

/* ------------------------------------------------------------------------- */
	
void bts_CompactAlt_copy( struct bbs_Context* cpA,
					      struct bts_CompactAlt* ptrA, 
						  const struct bts_CompactAlt* srcPtrA )
{
	bts_CompactMat_copy( cpA, &ptrA->matE, &srcPtrA->matE );
	bbs_Int16Arr_copy( cpA, &ptrA->vecE, &srcPtrA->vecE );
	ptrA->vecExpE = srcPtrA->vecExpE;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bts_CompactAlt_memSize( struct bbs_Context* cpA,
							   const struct bts_CompactAlt *ptrA )
{
	return  bbs_SIZEOF16( uint32 )
		  + bbs_SIZEOF16( uint32 ) /* version */
		  + bts_CompactMat_memSize( cpA, &ptrA->matE )
		  + bbs_Int16Arr_memSize( cpA, &ptrA->vecE )
		  + bbs_SIZEOF16( ptrA->vecExpE );
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_CompactAlt_memWrite( struct bbs_Context* cpA,
							    const struct bts_CompactAlt* ptrA, 
							    uint16* memPtrA )
{
	uint32 memSizeL = bts_CompactAlt_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWriteUInt32( bts_COMPACT_ALT_VERSION, memPtrA );
	memPtrA += bts_CompactMat_memWrite( cpA, &ptrA->matE, memPtrA );
	memPtrA += bbs_Int16Arr_memWrite( cpA, &ptrA->vecE, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->vecExpE, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bts_CompactAlt_memRead( struct bbs_Context* cpA,
							 struct bts_CompactAlt* ptrA, 
							 const uint16* memPtrA,
				             struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, versionL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memReadVersion32( cpA, &versionL, bts_COMPACT_ALT_VERSION, memPtrA );
	memPtrA += bts_CompactMat_memRead( cpA, &ptrA->matE, memPtrA, mspA );
	memPtrA += bbs_Int16Arr_memRead( cpA, &ptrA->vecE, memPtrA, mspA );
	memPtrA += bbs_memRead32( &ptrA->vecExpE, memPtrA );

	if( memSizeL != bts_CompactAlt_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bts_CompactAlt_memRead( const struct bts_CompactAlt* ptrA, const void* memPtrA ):\n"
                  "size mismatch" ); 
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

void bts_CompactAlt_map( struct bbs_Context* cpA, 
						 const struct bts_CompactAlt* ptrA, 
						 const int16* inVecA,
						 int16  inExpA,
						 int16* outVecA,
						 int16* outExpPtrA )
{
	uint32 iL;
	uint32 sizeL = ptrA->matE.heightE;

	int32 expL = inExpA;
	int16 mapExpL;
	bts_CompactMat_map( cpA, &ptrA->matE, inVecA, outVecA, &mapExpL );
	expL += mapExpL;

	/* translation */
	if( ptrA->vecE.sizeE > 0 )
	{
		const int16* vecL = ptrA->vecE.arrPtrE;
		if( expL == ptrA->vecExpE )
		{
			for( iL = 0; iL < sizeL; iL++ ) outVecA[ iL ] = ( ( int32 )outVecA[ iL ] + vecL[ iL ] + 1 ) >> 1;
			expL += 1;
		}
		else if( expL > ptrA->vecExpE )
		{
			int32 shrL = expL - ptrA->vecExpE;
			int32 addL = ( int32 )1 << ( shrL - 1 );
			for( iL = 0; iL < sizeL; iL++ ) outVecA[ iL ] = ( ( int32 )outVecA[ iL ] + ( ( ( int32 )vecL[ iL ] + addL ) >> shrL ) + 1 ) >> 1;
			expL += 1;
		}
		else
		{
			int32 shrL = ptrA->vecExpE - expL;
			int32 addL = ( int32 )1 << ( shrL - 1 );
			for( iL = 0; iL < sizeL; iL++ ) outVecA[ iL ] = ( ( ( ( int32 )outVecA[ iL ] + addL ) >> shrL ) + vecL[ iL ] + 1 ) >> 1;
			expL += 1 + shrL;
		}
	}

	/* precision underflow */
	if( expL < -32767 )
	{
		bbs_memset16( outVecA, 0, ptrA->matE.heightE );
		expL = 0;
	}

	if( outExpPtrA != NULL ) *outExpPtrA = expL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */

