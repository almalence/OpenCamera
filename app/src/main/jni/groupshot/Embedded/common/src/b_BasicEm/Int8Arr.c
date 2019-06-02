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
#include "b_BasicEm/Int8Arr.h"

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

void bbs_Int8Arr_init( struct bbs_Context* cpA,
					   struct bbs_Int8Arr* ptrA )
{
	ptrA->arrPtrE = NULL;
	ptrA->sizeE = 0;
	ptrA->allocatedSizeE = 0;
	ptrA->mspE = NULL;
}

/* ------------------------------------------------------------------------- */

void bbs_Int8Arr_exit( struct bbs_Context* cpA,
					   struct bbs_Int8Arr* ptrA )
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

void bbs_Int8Arr_copy( struct bbs_Context* cpA,
					   struct bbs_Int8Arr* ptrA, 
					   const struct bbs_Int8Arr* srcPtrA )
{
#ifdef DEBUG1
	if( ptrA->allocatedSizeE < srcPtrA->allocatedSizeE )
	{
		bbs_ERROR0( "void bbs_Int8Arr_copy(...):\n"
				   "Unsufficient allocated memory in destination array." );		
		return;
	}
#endif
	bbs_Int8Arr_size( cpA, ptrA, srcPtrA->sizeE );
	bbs_memcpy16( ptrA->arrPtrE, srcPtrA->arrPtrE, srcPtrA->sizeE >> 1 ); 
}

/* ------------------------------------------------------------------------- */

flag bbs_Int8Arr_equal( struct bbs_Context* cpA,
					    const struct bbs_Int8Arr* ptrA, 
						const struct bbs_Int8Arr* srcPtrA )
{
	long iL;
	const int8* ptr1L = ptrA->arrPtrE;
	const int8* ptr2L = srcPtrA->arrPtrE;
	if( ptrA->sizeE != srcPtrA->sizeE ) return FALSE;
	for( iL = ptrA->sizeE; iL > 0; iL-- )
	{
		if( *ptr1L++ != *ptr2L++ ) return FALSE;
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

uint32 bbs_Int8Arr_heapSize( struct bbs_Context* cpA,
							 const struct bbs_Int8Arr* ptrA, 
							 uint32 sizeA )
{
	return ( sizeA >> 1 ) + bbs_MEM_BLOCK_OVERHD;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ modify functions } ------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */
	
void bbs_Int8Arr_create( struct bbs_Context* cpA,
						 struct bbs_Int8Arr* ptrA, 
						 uint32 sizeA,
						 struct bbs_MemSeg* mspA )
{
	if( bbs_Context_error( cpA ) ) return;
	if( ptrA->sizeE == sizeA ) return;
	if( ptrA->arrPtrE != 0 )
	{
		bbs_Int8Arr_size( cpA, ptrA, sizeA );
	}
	else
	{
		/* if size is odd increase by 1 byte */
		uint32 sizeL = sizeA;
		if( ( sizeL & 1 ) != 0 ) sizeL++;

		ptrA->arrPtrE = bbs_MemSeg_alloc( cpA, mspA, sizeL >> 1 );
		if( bbs_Context_error( cpA ) ) return;
		ptrA->allocatedSizeE = sizeL;

		ptrA->sizeE = sizeA;
		if( !mspA->sharedE ) ptrA->mspE = mspA;
	}
}

/* ------------------------------------------------------------------------- */

void bbs_Int8Arr_size( struct bbs_Context* cpA,
					   struct bbs_Int8Arr* ptrA, 
					   uint32 sizeA )
{
	if( ptrA->allocatedSizeE < sizeA )
	{
		bbs_ERROR1( "void bbs_Int8Arr_size( struct bbs_Int8Arr*, uint32 ):\n"
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
	
uint32 bbs_Int8Arr_memSize( struct bbs_Context* cpA,
						    const struct bbs_Int8Arr* ptrA )
{
	return bbs_SIZEOF16( uint32 ) + bbs_SIZEOF16( ptrA->sizeE ) + 
										ptrA->sizeE / 2; /* int8 = 0.5 word size*/
}

/* ------------------------------------------------------------------------- */
	
uint32 bbs_Int8Arr_memWrite( struct bbs_Context* cpA,
							 const struct bbs_Int8Arr* ptrA, 
							 uint16* memPtrA )
{
	uint32 memSizeL = bbs_Int8Arr_memSize( cpA, ptrA );
	memPtrA += bbs_memWrite32( &memSizeL, memPtrA );
	memPtrA += bbs_memWrite32( &ptrA->sizeE, memPtrA );
	memPtrA += bbs_memWrite16Arr( cpA, ptrA->arrPtrE, ptrA->sizeE / 2, memPtrA );
	/*bbs_memcpy( memPtrA, ptrA->arrPtrE, ptrA->sizeE );*/
	return memSizeL;
}

/* ------------------------------------------------------------------------- */
	
uint32 bbs_Int8Arr_memRead( struct bbs_Context* cpA,
						    struct bbs_Int8Arr* ptrA, 
						    const uint16* memPtrA,
							struct bbs_MemSeg* mspA )
{
	uint32 memSizeL, sizeL;
	if( bbs_Context_error( cpA ) ) return 0;
	memPtrA += bbs_memRead32( &memSizeL, memPtrA );
	memPtrA += bbs_memRead32( &sizeL, memPtrA );
	bbs_Int8Arr_create( cpA, ptrA, sizeL, mspA );
	memPtrA += bbs_memRead16Arr( cpA, ptrA->arrPtrE, ptrA->sizeE / 2, memPtrA );

	if( memSizeL != bbs_Int8Arr_memSize( cpA, ptrA ) )
	{
		bbs_ERR0( bbs_ERR_CORRUPT_DATA, "uint32 bbs_Int8Arr_memRead( const struct bbs_Int8Arr*, const uint16* ):\n"
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

void bbs_Int8Arr_fill( struct bbs_Context* cpA,
					   struct bbs_Int8Arr* ptrA, 
					   int8 valA )
{
	uint32 iL;
	for( iL = 0; iL < ptrA->sizeE; iL++ )
	{
		ptrA->arrPtrE[ iL ] = valA;
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


