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

#include "b_BasicEm/MemSeg.h"
#include "b_BasicEm/Functions.h"
#include "b_BasicEm/Context.h"

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

void bbs_MemSeg_init( struct bbs_Context* cpA, 
					  struct bbs_MemSeg* ptrA )
{
	ptrA->memPtrE = NULL;
	ptrA->sizeE = 0;
	ptrA->allocIndexE = 0;
	ptrA->sharedE = FALSE;
	ptrA->idE = 0;
	ptrA->dynMemManagerPtrE = NULL;
}

/* ------------------------------------------------------------------------- */

void bbs_MemSeg_exit( struct bbs_Context* cpA, 
					  struct bbs_MemSeg* ptrA )
{
	ptrA->memPtrE = NULL;
	ptrA->sizeE = 0;
	ptrA->allocIndexE = 0;
	ptrA->sharedE = FALSE;
	ptrA->idE = 0;
	ptrA->dynMemManagerPtrE = NULL;
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

uint32 bbs_MemSeg_availableSize( struct bbs_Context* cpA, 
								 const struct bbs_MemSeg* ptrA )
{
	if( ptrA->dynMemManagerPtrE == NULL )
	{
		return ( ptrA->sizeE == ptrA->allocIndexE ) ? 0 : ptrA->sizeE - ptrA->allocIndexE - 2 * ptrA->sharedE;
	}
	else
	{
		return 0xFFFFFFFF;
	}
}

/* ------------------------------------------------------------------------- */

uint32 bbs_MemSeg_allocatedSize( struct bbs_Context* cpA, 
								 const struct bbs_MemSeg* ptrA )
{
	if( ptrA->dynMemManagerPtrE == NULL )
	{
		return ptrA->allocIndexE;
	}
	else
	{
		return bbs_DynMemManager_allocatedSize( cpA, ptrA->dynMemManagerPtrE );
	}
}

/* ------------------------------------------------------------------------- */

uint32 bbs_MemSeg_usedSize( struct bbs_Context* cpA, 
						    const struct bbs_MemSeg* ptrA )
{
	if( ptrA->dynMemManagerPtrE == NULL )
	{
		if( ptrA->sharedE )
		{
			return ptrA->allocIndexE;
		}
		else
		{
			uint32 indexL = 0;
			uint32 countL = 0;
			while( indexL < ptrA->allocIndexE )
			{
				uint32 sizeL = *( uint32* )( ptrA->memPtrE + indexL );
				indexL += ( sizeL & 0xFFFFFFFE );
				if( ( sizeL & 1 ) == 0 )
				{
					countL += sizeL - 2;
				}
			}
			return countL;
		}
	}
	else
	{
		return bbs_MemSeg_allocatedSize( cpA, ptrA );
	}
}

/* ------------------------------------------------------------------------- */

uint32 bbs_MemSeg_blocks( struct bbs_Context* cpA, 
						  const struct bbs_MemSeg* ptrA )
{
	uint32 indexL = 0;
	uint32 countL = 0;

	if( ptrA->sharedE ) return 0;

	while( indexL < ptrA->allocIndexE )
	{
		uint32 sizeL = *( uint32* )( ptrA->memPtrE + indexL );
		indexL += ( sizeL & 0xFFFFFFFE );
		countL++;
	}
	return countL;
}

/* ------------------------------------------------------------------------- */

uint32 bbs_MemSeg_usedBlocks( struct bbs_Context* cpA, 
							  const struct bbs_MemSeg* ptrA )
{
	uint32 indexL = 0;
	uint32 countL = 0;

	if( ptrA->sharedE ) return 0;

	while( indexL < ptrA->allocIndexE )
	{
		uint32 sizeL = *( uint32* )( ptrA->memPtrE + indexL );
		indexL += ( sizeL & 0xFFFFFFFE );
		countL += ( ( sizeL & 1 ) == 0 );
	}
	return countL;
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

struct bbs_MemSeg bbs_MemSeg_create( struct bbs_Context* cpA,
									 void* memPtrA, uint32 sizeA )
{
	struct bbs_MemSeg memSegL;
	memSegL.memPtrE     = ( uint16* )memPtrA;
	memSegL.sizeE       = sizeA & 0xFFFFFFFE; /* enforce even size to avoid overflow problems */
	memSegL.allocIndexE = 0;
	memSegL.sharedE     = FALSE;
	memSegL.idE         = 0;
	memSegL.dynMemManagerPtrE = NULL;
	return memSegL;
}

/* ------------------------------------------------------------------------- */

struct bbs_MemSeg bbs_MemSeg_createShared( struct bbs_Context* cpA,
										   void* memPtrA, uint32 sizeA )
{
	struct bbs_MemSeg memSegL;
	memSegL.memPtrE     = ( uint16* )memPtrA;
	memSegL.sizeE       = sizeA;
	memSegL.allocIndexE = 0;
	memSegL.sharedE     = TRUE;
	memSegL.idE         = 0;
	memSegL.dynMemManagerPtrE = NULL;
	return memSegL;
}

/* ------------------------------------------------------------------------- */

void* bbs_MemSeg_alloc( struct bbs_Context* cpA, 
					    struct bbs_MemSeg* ptrA, 
						uint32 sizeA )
{
	uint16* memPtrL = NULL;

	if( bbs_Context_error( cpA ) ) return NULL;

	if( !ptrA->sharedE )
	{
		if( ptrA->dynMemManagerPtrE == NULL )
		{
			uint32 effSizeL = sizeA + ( sizeA & 1 ) + 2; /* effective block size */
			memPtrL = ptrA->memPtrE + ptrA->allocIndexE;
			*( ( uint32* )memPtrL ) = effSizeL;
			memPtrL += 2;
			if( ptrA->allocIndexE + effSizeL > ptrA->sizeE )
			{
				bbs_ERR2( bbs_ERR_MEMORY_OVERFLOW,
						  "uint16* bbs_MemSeg_alloc( struct bbs_MemSeg* ptrA, uint32 sizeA ):\n"
						  "Exclusive Memory overflow. Segment size: %i. Requested size: %i", ptrA->sizeE, sizeA );
				return NULL;
			}
			ptrA->allocIndexE += effSizeL;
		}
		else
		{
			memPtrL = bbs_DynMemManager_alloc( cpA, ptrA->dynMemManagerPtrE, ptrA, sizeA );
		}
	}
	else
	{
		uint32 effSizeL = sizeA + ( sizeA & 1 );  /* effective block size */

		if( ptrA->allocIndexE + effSizeL > ptrA->sizeE  + ( ptrA->sizeE & 1 ) )
		{
			if( ptrA->dynMemManagerPtrE == NULL )
			{
				bbs_ERR2( bbs_ERR_MEMORY_OVERFLOW,
						  "uint16* bbs_MemSeg_alloc( struct bbs_MemSeg* ptrA, uint32 sizeA ):\n"
						  "Shared Memory overflow. Segment size: %i. Requested size: %i", ptrA->sizeE, sizeA );
				return NULL;
			}
			else
			{
				uint32 actualBlockSizeL = 0;
				ptrA->memPtrE = bbs_DynMemManager_nextBlock( cpA, ptrA->dynMemManagerPtrE, ptrA, ptrA->memPtrE, effSizeL, &actualBlockSizeL );
				ptrA->sizeE = actualBlockSizeL;
				ptrA->allocIndexE = 0;
			}
		}

		memPtrL = ptrA->memPtrE + ptrA->allocIndexE;
		ptrA->allocIndexE += effSizeL;
	}

	#if defined( HW_TMS320C5x )
	#ifdef DEBUG2
	{
		/* check if segment crosses page boundary */
		if( ( ( ( uint32 ) ptrA->memPtrE ) >> 16 ) !=
			( ( ( uint32 ) ptrA->memPtrE + ( ptrA->sizeE - 1 ) ) >> 16 ) )
		{
			bbs_ERROR0( "uint16* bbs_MemSeg_alloc( struct bbs_MemSeg* ptrA, uint32 sizeA ):\nSegment crosses page boundary\n" );
			return NULL;
		}
	}
	#endif
	#endif

	return memPtrL;
}

/* ------------------------------------------------------------------------- */

void bbs_MemSeg_free( struct bbs_Context* cpA,
					  struct bbs_MemSeg* ptrA,
					  void* memPtrA )
{
	bbs_DEF_fNameL( "void bbs_MemSeg_free( struct bbs_MemSeg* ptrA, void* memPtrA )" )

	if( bbs_Context_error( cpA ) ) return;

	/** only valid exclusive segments can be freed */
	if( ptrA == NULL || memPtrA == NULL || ptrA->sharedE ) return;

	if( ptrA->dynMemManagerPtrE != NULL )
	{
		bbs_DynMemManager_free( cpA, ptrA->dynMemManagerPtrE, memPtrA );
	}
	else
	{
		uint32 indexL, sizeL;
		uint16* memPtrL;

		if( ptrA == NULL || memPtrA == NULL ) return;
		if( ptrA->sharedE ) return;

		#ifdef HW_TMS320C5x
			indexL = ( uint32 ) memPtrA - ( uint32 ) ptrA->memPtrE - 2;
		#else
			indexL = ( uint16* )memPtrA - ptrA->memPtrE - 2;
		#endif

		memPtrL = ptrA->memPtrE + indexL;
		sizeL = *( ( int32* )memPtrL );

		/* checks */
		if( indexL > ptrA->allocIndexE || ( indexL & 1 ) != 0 )
		{
			bbs_ERROR4( "%s\n: Invalid memory.\n"
						"sizeE       = %i\n"
						"allocIndexE = %i\n"
						"indexL      = %i\n",
						fNameL,
						ptrA->sizeE,
						ptrA->allocIndexE,
						indexL );
			return;
		}

		if( ( sizeL & 1 ) != 0 )
		{
			bbs_ERROR1( "%s\n: Memory block was already freed once", fNameL );
			return;
		}

		*( ( uint32* )memPtrL ) += 1; /* odd size value indicates unused memory block */

		/* free last unused blocks if any */
		if( indexL + sizeL == ptrA->allocIndexE )
		{
			uint32 newAllocIndexL = 0;
			indexL = 0;
			while( indexL < ptrA->allocIndexE )
			{
				uint32 sizeL = *( uint32* )( ptrA->memPtrE + indexL );
				indexL += ( sizeL & 0xFFFFFFFE );
				if( ( sizeL & 1 ) == 0 )
				{
					newAllocIndexL = indexL;
				}
			}

			ptrA->allocIndexE = newAllocIndexL;
		}

	#ifdef DEBUG2
		bbs_MemSeg_checkConsistency( cpA, ptrA );
	#endif

	}
}

/* ------------------------------------------------------------------------- */

void bbs_MemSeg_checkConsistency( struct bbs_Context* cpA,
								  const struct bbs_MemSeg* ptrA )
{
	uint32 indexL = 0;

	if( ptrA->sharedE ) return;

	while( indexL < ptrA->allocIndexE )
	{
		uint32 sizeL = *( uint32* )( ptrA->memPtrE + indexL );
		indexL += ( sizeL & 0xFFFFFFFE );
	}

	if( indexL != ptrA->allocIndexE )
	{
		bbs_ERROR0( "Memory consistency check failed" );
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
