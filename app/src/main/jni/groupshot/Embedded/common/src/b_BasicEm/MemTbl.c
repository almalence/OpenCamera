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

#include "b_BasicEm/Context.h"
#include "b_BasicEm/MemTbl.h"
#include "b_BasicEm/Functions.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ auxiliary functions } ---------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

flag bbs_MemTbl_memOverlap( const uint16* memPtr1A, uint32 size1A, 
						    const uint16* memPtr2A, uint32 size2A )
{
	int32 diffL = memPtr2A - memPtr1A;
	if( diffL >= 0 && diffL < ( int32 )size1A ) return TRUE;
	diffL += ( int32 )size2A;
	if( diffL >= 0 && diffL < ( int32 )size1A ) return TRUE;
	return FALSE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ constructor / destructor } ----------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbs_MemTbl_init( struct bbs_Context* cpA,
					  struct bbs_MemTbl* ptrA )
{
	uint32 iL;
	for( iL = 0; iL < bbs_MAX_MEM_SEGS; iL++ )
	{
		bbs_MemSeg_init( cpA, &ptrA->esArrE[ iL ] );
		bbs_MemSeg_init( cpA, &ptrA->ssArrE[ iL ] );
		ptrA->espArrE[ iL ] = NULL;
	}
	ptrA->esSizeE = 0;
	ptrA->ssSizeE = 0;
}

/* ------------------------------------------------------------------------- */

void bbs_MemTbl_exit( struct bbs_Context* cpA,
					  struct bbs_MemTbl* ptrA )
{
	uint32 iL;
	for( iL = 0; iL < bbs_MAX_MEM_SEGS; iL++ )
	{
		bbs_MemSeg_exit( cpA, &ptrA->esArrE[ iL ] );
		bbs_MemSeg_exit( cpA, &ptrA->ssArrE[ iL ] );
		ptrA->espArrE[ iL ] = NULL;
	}
	ptrA->esSizeE = 0;
	ptrA->ssSizeE = 0;
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

flag bbs_MemTbl_overlap( struct bbs_Context* cpA,
						 struct bbs_MemTbl* ptrA, 
						 const void* memPtrA, uint32 sizeA )
{
	uint32 iL;
	for( iL = 0; iL < ptrA->esSizeE; iL++ )
	{
		if( bbs_MemTbl_memOverlap( ptrA->espArrE[ iL ]->memPtrE, 
								   ptrA->espArrE[ iL ]->sizeE,
								   memPtrA, sizeA ) )
		{
			return TRUE;
		}
	}

	for( iL = 0; iL < ptrA->ssSizeE; iL++ )
	{
		if( bbs_MemTbl_memOverlap( ptrA->ssArrE[ iL ].memPtrE, 
								   ptrA->ssArrE[ iL ].sizeE,
								   memPtrA, sizeA ) )
		{
			return TRUE;
		}
	}

	return FALSE;
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

void bbs_MemTbl_create( struct bbs_Context* cpA,
					    struct bbs_MemTbl* ptrA, 
						void* memPtrA, 
						uint32 sizeA, 
						uint32 sharedSubSizeA )
{
	if( sharedSubSizeA > sizeA )
	{
		bbs_ERROR0( "struct bbs_MemTbl bbs_MemTbl_create( void* memPtrA, uint32 sizeA, uint32 sharedSubSizeA ):\n"
			       "sharedSubSizeA > sizeA" );
		return;
	}
	bbs_MemTbl_init( cpA, ptrA );

	
	ptrA->esArrE[ 0 ] = bbs_MemSeg_create( cpA, memPtrA, sizeA - sharedSubSizeA );
	#ifdef HW_TMS320C5x		
		ptrA->ssArrE[ 0 ] = bbs_MemSeg_createShared( cpA, ( uint16* ) ( ( int32 ) ( ( uint16* )memPtrA ) + sizeA - sharedSubSizeA ), sharedSubSizeA );
	#else
		ptrA->ssArrE[ 0 ] = bbs_MemSeg_createShared( cpA, ( uint16* )memPtrA + sizeA - sharedSubSizeA, sharedSubSizeA );
	#endif
	ptrA->espArrE[ 0 ] = &ptrA->esArrE[ 0 ];

	ptrA->esSizeE = 1;
	ptrA->ssSizeE = 1;
}

/* ------------------------------------------------------------------------- */

void bbs_MemTbl_add( struct bbs_Context* cpA,
					 struct bbs_MemTbl* ptrA, 
					 void* memPtrA, 
					 uint32 sizeA, 
					 uint32 idA )
{
	if( ptrA->esSizeE == bbs_MAX_MEM_SEGS )
	{
		bbs_ERROR0( "void bbs_MemTbl_add( struct bbs_MemTbl* ptrA, void* memPtrA, uint32 sizeA ):\n"
			       "Table is full! Increase constant bbs_MAX_MEM_SEGS" );
		return;
	}
	ptrA->esArrE[ ptrA->esSizeE ] = bbs_MemSeg_create( cpA, memPtrA, sizeA );
	ptrA->esArrE[ ptrA->esSizeE ].idE = idA;
	ptrA->espArrE[ ptrA->esSizeE ] = &ptrA->esArrE[ ptrA->esSizeE ];
	ptrA->esSizeE++;
}

/* ------------------------------------------------------------------------- */

void bbs_MemTbl_addShared( struct bbs_Context* cpA,
						   struct bbs_MemTbl* ptrA, 
						   void* memPtrA, 
						   uint32 sizeA, 
						   uint32 idA )
{
	if( ptrA->ssSizeE == bbs_MAX_MEM_SEGS )
	{
		bbs_ERROR0( "void bbs_MemTbl_addShared( struct bbs_MemTbl* ptrA, void* memPtrA, uint32 sizeA ):\n"
			       "Table is full! Increase constant bbs_MAX_MEM_SEGS" );
		return;
	}
	ptrA->ssArrE[ ptrA->ssSizeE ] = bbs_MemSeg_createShared( cpA, memPtrA, sizeA );
	ptrA->ssArrE[ ptrA->ssSizeE ].idE = idA;
	ptrA->ssSizeE++;
}

/* ------------------------------------------------------------------------- */

struct bbs_MemSeg* bbs_MemTbl_segPtr( struct bbs_Context* cpA,
									  struct bbs_MemTbl* ptrA, 
									  uint32 idA )
{
	uint32 iL;
	if( ptrA->esSizeE == 0 )
	{
		bbs_ERROR0( "bbs_MemTbl_segPtr(): Table contains no exclusive segments." );
		return NULL;
	}
	if( idA > 0 ) 
	{
		for( iL = 0; iL < ptrA->esSizeE; iL++ )
		{
			if( idA == ptrA->espArrE[ iL ]->idE ) return ptrA->espArrE[ iL ];
		}
	}
	for( iL = 0; iL < ptrA->esSizeE; iL++ )
	{
		if( ptrA->espArrE[ iL ]->sizeE > 0 ||
			ptrA->espArrE[ iL ]->dynMemManagerPtrE != 0 )
		{
			return ptrA->espArrE[ iL ];
		}
	}
	bbs_ERR0( bbs_ERR_MEMORY_OVERFLOW,
			  "bbs_MemTbl_segPtr(): Table contains no valid exclusive segments." );
	return 0;
}

/* ------------------------------------------------------------------------- */

struct bbs_MemSeg* bbs_MemTbl_sharedSegPtr( struct bbs_Context* cpA,
										    struct bbs_MemTbl* ptrA, 
											uint32 idA )
{
	uint32 iL;
	if( ptrA->ssSizeE == 0 )
	{
		bbs_ERROR0( "bbs_MemTbl_sharedSegPtr(): Table contains no shared segments." );
		return NULL;
	}
	if( idA > 0 ) 
	{
		for( iL = 0; iL < ptrA->ssSizeE; iL++ )
		{
			if( idA == ptrA->ssArrE[ iL ].idE ) return &ptrA->ssArrE[ iL ];
		}
	}
	for( iL = 0; iL < ptrA->ssSizeE; iL++ )
	{
		if( ptrA->ssArrE[ iL ].sizeE > 0 ||
			ptrA->ssArrE[ iL ].dynMemManagerPtrE != 0 )
		{
			return &ptrA->ssArrE[ iL ];
		}
	}
	bbs_ERR0( bbs_ERR_MEMORY_OVERFLOW,
			  "bbs_MemTbl_sharedSegPtr(): Table contains no valid shared segments." );
	return 0;
}

/* ------------------------------------------------------------------------- */

struct bbs_MemSeg* bbs_MemTbl_fastestSegPtr( struct bbs_Context* cpA,
											 struct bbs_MemTbl* ptrA, 
											 uint32 minSizeA )
{
	uint32 iL;
	for( iL = 0; iL < ptrA->esSizeE; iL++ )
	{
		if( bbs_MemSeg_availableSize( cpA, ptrA->espArrE[ iL ] ) >= minSizeA ) break;
	}
	if( iL == ptrA->esSizeE )
	{
		if( ptrA->esSizeE == 0 )
		{
			bbs_ERROR0( "struct bbs_MemSeg* bbs_MemTbl_fastestSegPtr( struct bbs_MemTbl* ptrA, uint32 minSizeA ):\n"
					   "Table contains no exclusive segments" );
			return NULL;
		}
		else
		{
			bbs_ERR0( bbs_ERR_MEMORY_OVERFLOW,
					  "struct bbs_MemSeg* bbs_MemTbl_fastestSegPtr( struct bbs_MemTbl* ptrA, uint32 minSizeA ):\n"
					  "Could not find segment with sufficient free space" );
			return NULL;
		}
	}
	if( ptrA->espArrE[ iL ]->sharedE )
	{
		bbs_ERROR0( "struct bbs_MemSeg* bbs_MemTbl_fastestSegPtr( struct bbs_MemTbl* ptrA, uint32 minSizeA ):\n"
			       "Table corrupt: Found shared segment in exclusive table" );
		return NULL;
	}

	return ptrA->espArrE[ iL ];
}

/* ------------------------------------------------------------------------- */

struct bbs_MemSeg* bbs_MemTbl_largestSegPtr( struct bbs_Context* cpA,
											 struct bbs_MemTbl* ptrA )
{
	uint32 iL;
	uint32 maxIndexL = 0;
	uint32 maxSizeL = 0;

	if( ptrA->esSizeE == 0 )
	{
		bbs_ERROR0( "struct bbs_MemSeg* bbs_MemTbl_largestSegPtr( struct bbs_MemTbl* ptrA ):\n"
			       "No exclusive segments available" );
		return NULL;
	}

	for( iL = 0; iL < ptrA->esSizeE; iL++ )
	{
		uint32 sizeL = bbs_MemSeg_availableSize( cpA, ptrA->espArrE[ iL ] );
		if( sizeL > maxSizeL )
		{
			maxSizeL = sizeL;
			maxIndexL = iL;
		}
	}

	if( ptrA->espArrE[ maxIndexL ]->sharedE )
	{
		bbs_ERROR0( "struct bbs_MemSeg* bbs_MemTbl_largestSegPtr( struct bbs_MemTbl* ptrA ):\n"
			       "Table corrupt: Found shared segment in exclusive table" );
		return NULL;
	}

	return ptrA->espArrE[ maxIndexL ];
}

/* ------------------------------------------------------------------------- */

struct bbs_MemSeg* bbs_MemTbl_fastestSharedSegPtr( struct bbs_Context* cpA,
												   struct bbs_MemTbl* ptrA, 
												   uint32 minSizeA )
{
	uint32 iL;
	for( iL = 0; iL < ptrA->ssSizeE; iL++ )
	{
		if( bbs_MemSeg_availableSize( cpA, &ptrA->ssArrE[ iL ] ) >= minSizeA ) break;
	}
	if( iL == ptrA->ssSizeE )
	{
		if( ptrA->esSizeE == 0 )
		{
			bbs_ERROR0( "struct bbs_MemSeg* bbs_MemTbl_fastestSegPtr( struct bbs_MemTbl* ptrA, uint32 minSizeA ):\n"
					   "Table contains no shared segments" );
			return NULL;
		}
		else
		{
			bbs_ERR0( bbs_ERR_MEMORY_OVERFLOW, 
					  "struct bbs_MemSeg* bbs_MemTbl_fastestSharedSegPtr( struct bbs_MemTbl* ptrA, uint32 minSizeA ):\n"
					  "Could not find segment with sufficient free space" );
			return NULL;
		}
	}
	if( !ptrA->ssArrE[ iL ].sharedE )
	{
		bbs_ERROR0( "struct bbs_MemSeg* bbs_MemTbl_fastestSharedSegPtr( struct bbs_MemTbl* ptrA, uint32 minSizeA ):\n"
			       "Table corrupt: Found exclusive segment in shared table" );
		return NULL;
	}

	return &ptrA->ssArrE[ iL ];
}

/* ------------------------------------------------------------------------- */

struct bbs_MemSeg* bbs_MemTbl_largestSharedSegPtr( struct bbs_Context* cpA,
												   struct bbs_MemTbl* ptrA )
{
	uint32 iL;
	uint32 maxIndexL = 0;
	uint32 maxSizeL = 0;

	if( ptrA->ssSizeE == 0 )
	{
		bbs_ERROR0( "struct bbs_MemSeg* bbs_MemTbl_largestSharedSegPtr( struct bbs_MemTbl* ptrA ):\n"
			       "No shared segments available" );
		return NULL;
	}

	for( iL = 0; iL < ptrA->ssSizeE; iL++ )
	{
		uint32 sizeL = bbs_MemSeg_availableSize( cpA, &ptrA->ssArrE[ iL ] );
		if( sizeL > maxSizeL )
		{
			maxSizeL = sizeL;
			maxIndexL = iL;
		}
	}

	if( !ptrA->ssArrE[ maxIndexL ].sharedE )
	{
		bbs_ERROR0( "struct bbs_MemSeg* bbs_MemTbl_largestSharedSegPtr( struct bbs_MemTbl* ptrA ):\n"
			       "Table corrupt: Found exclusive segment in shared table" );
		return NULL;
	}

	return &ptrA->ssArrE[ maxIndexL ];
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


