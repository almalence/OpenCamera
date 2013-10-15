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
#include "b_BasicEm/DynMemManager.h"
#include "b_BasicEm/Context.h"

/* ------------------------------------------------------------------------- */

/* minimum block size dynamically allocated in function nextBlock (affects only shared memory) */
#define bbs_DYN_MEM_MIN_NEW_BLOCK_SIZE 0

/** Offset to actual memory area on allocated memory blocks (in 16-bit words).
  * Value needs to be large enough to hold the pointer to the next memory block
  * and the size value (32-bit) of the memory area.
  */
#define bbs_MEM_OFFSET 6

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

void bbs_DynMemManager_init( struct bbs_Context* cpA, 
							 struct bbs_DynMemManager* ptrA )
{
	ptrA->memPtrE = NULL;
	ptrA->mallocFPtrE = NULL;
	ptrA->freeFPtrE = NULL;
}

/* ------------------------------------------------------------------------- */

void bbs_DynMemManager_exit( struct bbs_Context* cpA, 
							 struct bbs_DynMemManager* ptrA )
{
	ptrA->memPtrE = NULL;
	ptrA->mallocFPtrE = NULL;
	ptrA->freeFPtrE = NULL;
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

uint32 bbs_DynMemManager_allocatedSize( struct bbs_Context* cpA, 
									    const struct bbs_DynMemManager* ptrA )
{
	uint32 sizeL = 0;
	uint16* pL = ( uint16* )ptrA->memPtrE;
	while( pL != NULL )
	{
		sizeL += ( ( uint32* )pL )[ 2 ];
		pL = *( uint16** )pL;
	}
	return sizeL; 
}

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
	
/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ exec functions } --------------------------------------------- */
/*                                                                           */
/* ========================================================================= */
	
/* ------------------------------------------------------------------------- */

uint16* bbs_DynMemManager_alloc( struct bbs_Context* cpA, 
								 struct bbs_DynMemManager* ptrA, 
								 const struct bbs_MemSeg* memSegPtrA,
								 uint32 sizeA )
{
	uint16* pL = NULL;
	bbs_DEF_fNameL( "uint16* bbs_DynMemManager_alloc( struct bbs_DynMemManager* ptrA, uint32 sizeA )" )


	if( ptrA->mallocFPtrE == NULL )
	{
		bbs_ERROR1( "%s:\n Malloc handler not defined.\n", fNameL );
		return NULL;
	}

	if( ptrA->memPtrE == NULL )
	{
		ptrA->memPtrE = ptrA->mallocFPtrE( cpA, memSegPtrA, ( sizeA + bbs_MEM_OFFSET ) << 1 );
		pL = ptrA->memPtrE;
	}
	else
	{
		uint16** ppL = ( uint16** )ptrA->memPtrE;
		while( *ppL != NULL ) ppL = ( uint16** )*ppL;
		*ppL = ptrA->mallocFPtrE( cpA, memSegPtrA, ( sizeA + bbs_MEM_OFFSET ) << 1 );
		pL = *ppL;
	}

	if( pL == NULL )
	{
		bbs_ERR1( bbs_ERR_OUT_OF_MEMORY, "%s:\n Allocation failed.\n", fNameL );
		return NULL;
	}

	( ( uint32* )pL )[ 0 ] = 0;
	( ( uint32* )pL )[ 1 ] = 0;
	( ( uint32* )pL )[ 2 ] = sizeA + bbs_MEM_OFFSET;

	return pL + bbs_MEM_OFFSET;
}

/* ------------------------------------------------------------------------- */

void bbs_DynMemManager_free( struct bbs_Context* cpA, 
							 struct bbs_DynMemManager* ptrA, 
							 uint16* memPtrA )
{
	bbs_DEF_fNameL( "void bbs_DynMemManager_free( .... )" )

	if( ptrA->memPtrE == NULL )
	{
		bbs_ERROR1( "%s:\n Memory was not allocated.\n", fNameL );
		return;
	}
	else if( ptrA->memPtrE + bbs_MEM_OFFSET == memPtrA )
	{
		uint16* memPtrL = ptrA->memPtrE;
		ptrA->memPtrE = *( uint16** )ptrA->memPtrE;
		ptrA->freeFPtrE( memPtrL );
	}
	else
	{
		uint16* p0L = NULL; 
		uint16* pL = ( uint16* )ptrA->memPtrE;

		while( pL != NULL )
		{
			if( pL + bbs_MEM_OFFSET == memPtrA ) break;
			p0L = pL;
			pL = *( uint16** )pL;
		}

		if( pL != NULL )
		{
			if( ptrA->freeFPtrE == NULL )
			{
				bbs_ERROR1( "%s:\n Free handler not defined.\n", fNameL );
				return;
			}

			if( p0L != NULL )
			{
				*( uint16** )p0L = *( uint16** )pL;
			}
			else
			{
				ptrA->memPtrE = *( uint16** )pL;
			}

			ptrA->freeFPtrE( pL );
		}
		else
		{
			bbs_ERROR1( "%s:\n Attempt to free memory that was not allocated.\n", fNameL );
			return;
		}
	}
}

/* ------------------------------------------------------------------------- */

uint16* bbs_DynMemManager_nextBlock( struct bbs_Context* cpA, 
									 struct bbs_DynMemManager* ptrA, 
									 const struct bbs_MemSeg* memSegPtrA,
									 uint16* curBlockPtrA, 
									 uint32 minSizeA, 
									 uint32* actualSizePtrA )
{
	uint16* pL = ( uint16* )ptrA->memPtrE;
	bbs_DEF_fNameL( "uint16* bbs_DynMemManager_nextBlock( .... )" )

	if( curBlockPtrA != NULL )
	{
		/* find current block */
		while( pL != NULL )
		{
			if( pL + bbs_MEM_OFFSET == curBlockPtrA ) break;
			pL = *( uint16** )pL;
		}

		if( pL == NULL )
		{
			bbs_ERROR1( "%s:\nCould not find current memory block.\n", fNameL );
			*actualSizePtrA = 0;
			return NULL;
		}

		/* go to next block */
		pL = *( uint16** )pL;
	}

	/* find next fitting block */
	while( pL != NULL )
	{
		if( ( ( uint32* )pL )[ 2 ] >= minSizeA + bbs_MEM_OFFSET ) break;
		pL = *( uint16** )pL;
	}

	if( pL == NULL )
	{
		/* no proper block -> allocate new one */
		uint32 blockSizeL = minSizeA > bbs_DYN_MEM_MIN_NEW_BLOCK_SIZE ? minSizeA : bbs_DYN_MEM_MIN_NEW_BLOCK_SIZE;
		uint16* memPtrL = bbs_DynMemManager_alloc( cpA, ptrA, memSegPtrA, blockSizeL );
		if( memPtrL != NULL )
		{
			*actualSizePtrA = blockSizeL;
		}
		else
		{
			*actualSizePtrA = 0;
		}
		return memPtrL; 
	}
	else
	{
		*actualSizePtrA = ( ( uint32* )pL )[ 2 ] - bbs_MEM_OFFSET;
		return pL + bbs_MEM_OFFSET;
	}
}

/* ------------------------------------------------------------------------- */

void bbs_DynMemManager_freeAll( struct bbs_Context* cpA, struct bbs_DynMemManager* ptrA )
{
	uint16** ppL = ( uint16** )ptrA->memPtrE;
	while( ppL != NULL )
	{
		uint16* memPtrL = ( uint16* )ppL;
		ppL = ( uint16** )*ppL;
		ptrA->freeFPtrE( memPtrL );
	}
	ptrA->memPtrE = NULL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
