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
#include "b_BasicEm/ComplexArr.h"

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

void bbs_ComplexArr_init( struct bbs_Context* cpA,
						  struct bbs_ComplexArr* ptrA )
{
	ptrA->arrPtrE = NULL;
	ptrA->sizeE = 0;
	ptrA->allocatedSizeE = 0;
	ptrA->mspE = NULL;
}

/* ------------------------------------------------------------------------- */

void bbs_ComplexArr_exit( struct bbs_Context* cpA,
						  struct bbs_ComplexArr* ptrA )
{
	bbs_MemSeg_free( cpA, ptrA->mspE, ptrA->arrPtrE );
	ptrA->arrPtrE = NULL;
	ptrA->mspE = NULL;
	ptrA->sizeE = 0;
	ptrA->allocatedSizeE = 0;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbs_ComplexArr_copy( struct bbs_Context* cpA,
						  struct bbs_ComplexArr* ptrA, 
						  const struct bbs_ComplexArr* srcPtrA )
{
#ifdef DEBUG1
	if( ptrA->allocatedSizeE < srcPtrA->allocatedSizeE )
	{
		bbs_ERROR0( "void bbs_ComplexArr_copy( ... ):\n"
				   "Unsufficient allocated memory in destination array." );		
		return;
	}
#endif
	bbs_ComplexArr_size( cpA, ptrA, srcPtrA->sizeE );
	bbs_memcpy32( ptrA->arrPtrE, srcPtrA->arrPtrE, srcPtrA->sizeE * bbs_SIZEOF32( struct bbs_Complex ) ); 
}

/* ------------------------------------------------------------------------- */

flag bbs_ComplexArr_equal( struct bbs_Context* cpA,
						   const struct bbs_ComplexArr* ptrA, 
						   const struct bbs_ComplexArr* srcPtrA )
{
	uint32 iL;
	const struct bbs_Complex* ptr1L = ptrA->arrPtrE;
	const struct bbs_Complex* ptr2L = srcPtrA->arrPtrE;
	if( ptrA->sizeE != srcPtrA->sizeE ) return FALSE;
	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		if( !bbs_Complex_equal( *ptr1L, *ptr2L ) ) return FALSE;
	}
	return TRUE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ query functions } -------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bbs_ComplexArr_heapSize( struct bbs_Context* cpA,
							    const struct bbs_ComplexArr* ptrA, 
								uint32 sizeA )
{
	return sizeA * bbs_SIZEOF16( struct bbs_Complex ) + bbs_MEM_BLOCK_OVERHD;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bbs_ComplexArr_create( struct bbs_Context* cpA,
						    struct bbs_ComplexArr* ptrA, 
						    uint32 sizeA, 
							struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	if( ptrA->arrPtrE != 0 )
	{
		bbs_ComplexArr_size( cpA, ptrA, sizeA );
	}
	else
	{
		ptrA->arrPtrE = bbs_MemSeg_alloc( cpA, mspA, sizeA * bbs_SIZEOF16( struct bbs_Complex ) );
		if( bbs_Context_error( cpA ) ) return;
		ptrA->allocatedSizeE = sizeA;
		ptrA->sizeE = sizeA;
		if( !mspA->sharedE ) ptrA->mspE = mspA;
	}
}

/* ------------------------------------------------------------------------- */

void bbs_ComplexArr_size( struct bbs_Context* cpA,
						  struct bbs_ComplexArr* ptrA, 
						  uint32 sizeA )
{
	if( ptrA->allocatedSizeE < sizeA )
	{
		bbs_ERROR1( "void bbs_ComplexArr_size( struct bbs_ComplexArr*, uint32 sizeA ):\n"
				   "Unsufficient allocated memory (allocatedSizeE = '%i')",
				   ptrA->allocatedSizeE );
		return;
	}
	ptrA->sizeE = sizeA;
}

/* ------------------------------------------------------------------------- */
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ I/O } -------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
uint32 bbs_ComplexArr_memSize( struct bbs_Context* cpA,
							   const struct bbs_ComplexArr* ptrA )

{
	return	bbs_SIZEOF16( uint32 ) + bbs_SIZEOF16( ptrA->sizeE ) + 
	 						ptrA->sizeE * bbs_SIZEOF16( struct bbs_Complex );
}

/* ------------------------------------------------------------------------- */
	
uint32 bbs_ComplexArr_memWrite( struct bbs_Context* cpA,
							    const struct bbs_ComplexArr* ptrA, 
								uint16* memPtrA )
{
	uint32 memSizeL = bbs_ComplexArr_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->sizeE, memPtrA );
	memPtrA += bbs_memWrite16Arr( cpA, ptrA->arrPtrE, ptrA->sizeE * 2, memPtrA );
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bbs_ComplexArr_memRead( struct bbs_Context* cpA,
							   struct bbs_ComplexArr* ptrA, 
							   const uint16* memPtrA, 
							   struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, sizeL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memRead32( &sizeL, memPtrA );
	bbs_ComplexArr_create( cpA, ptrA, sizeL, mspA );
	memPtrA += bbs_memRead16Arr( cpA, ptrA->arrPtrE, ptrA->sizeE * 2, memPtrA );

	if( memSizeL != bbs_ComplexArr_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbs_ComplexArr_memRead( const struct bbs_ComplexArr* ptrA, const void* memPtrA ):\n"
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

/* ========================================================================= */


