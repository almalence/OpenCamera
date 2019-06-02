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
#include "b_BasicEm/Context.h"
#include "b_BasicEm/String.h"

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

void bbs_Context_init( struct bbs_Context* cpA )
{
	uint32 iL;
	for( iL = 0; iL < bbs_CONTEXT_MAX_ERRORS; iL++ )
	{
		cpA->errStackE[ iL ].errorE = bbs_ERR_OK;
		cpA->errStackE[ iL ].fileE[ 0 ] = 0;
		cpA->errStackE[ iL ].lineE = 0;
		cpA->errStackE[ iL ].textE[ 0 ] = 0;
	}

	cpA->errIndexE = 0;

	bbs_MemTbl_init( cpA, &cpA->memTblE );

	for( iL = 0; iL < bbs_CONTEXT_MAX_MEM_MANAGERS; iL++ )
	{
		bbs_DynMemManager_init( cpA, &cpA->dynMemManagerArrE[ iL ] );
	}

	cpA->dynMemManagerArrSizeE = 0;
	cpA->errorHandlerE = NULL;
	cpA->callbackHandlerE = NULL;
	cpA->userPtrE = NULL;
}

/* ------------------------------------------------------------------------- */

void bbs_Context_exit( struct bbs_Context* cpA )
{
	uint32 iL;
	for( iL = 0; iL < bbs_CONTEXT_MAX_ERRORS; iL++ )
	{
		cpA->errStackE[ iL ].errorE = bbs_ERR_OK;
		cpA->errStackE[ iL ].fileE[ 0 ] = 0;
		cpA->errStackE[ iL ].lineE = 0;
		cpA->errStackE[ iL ].textE[ 0 ] = 0;
	}

	cpA->errIndexE = 0;

	bbs_MemTbl_exit( cpA, &cpA->memTblE );

	for( iL = 0; iL < cpA->dynMemManagerArrSizeE; iL++ )
	{
		bbs_DynMemManager_freeAll( cpA, &cpA->dynMemManagerArrE[ iL ] );
	}

	for( iL = 0; iL < bbs_CONTEXT_MAX_MEM_MANAGERS; iL++ )
	{
		bbs_DynMemManager_exit( cpA, &cpA->dynMemManagerArrE[ iL ] );
	}

	cpA->dynMemManagerArrSizeE = 0;
	cpA->errorHandlerE = NULL;
	cpA->callbackHandlerE = NULL;
	cpA->userPtrE = NULL;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void bbs_Context_copy( struct bbs_Context* cpA, const struct bbs_Context* srcPtrA )
{
	bbs_ERROR0( "void bbs_Context_copy( struct bbs_Context* cpA, const struct bbs_Context* srcPtrA ):\n"
		        "A comtext object cannot be copied" );
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
	
struct bbs_Error bbs_Error_create( uint32 errorA, 
								   uint32 lineA, 
								   const char* fileA, 
								   const char* textA, 
								   ... )
{
	struct bbs_Error errorL;
	errorL.errorE = errorA;
	errorL.lineE = lineA;

	if( fileA != NULL )
	{
		uint32 lenL = bbs_strlen( fileA );
		uint32 ofsL = ( lenL + 1 > bbs_ERROR_MAX_FILE_CHARS ) ? lenL + 1 - bbs_ERROR_MAX_FILE_CHARS : 0;
		bbs_strcpy( errorL.fileE, fileA + ofsL );
	}
	else
	{
		errorL.fileE[ 0 ] = 0;
	}

	if( textA != NULL )
	{
		va_list argsL;
		va_start( argsL, textA );
		bbs_vsnprintf( errorL.textE, bbs_ERROR_MAX_TEXT_CHARS, textA, argsL );
		va_end( argsL );
	}
	else
	{
		errorL.textE[ 0 ] = 0;
	}

	return errorL;
}

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

flag bbs_Context_pushError( struct bbs_Context* cpA, struct bbs_Error errorA )
{
	flag returnL = FALSE;
	if( cpA->errIndexE < bbs_CONTEXT_MAX_ERRORS )
	{
		cpA->errStackE[ cpA->errIndexE++ ] = errorA;
		returnL = TRUE;
	}

	if( cpA->errorHandlerE != NULL )
	{
		cpA->errorHandlerE( cpA );
	}

	return returnL;
}

/* ------------------------------------------------------------------------- */

struct bbs_Error bbs_Context_popError( struct bbs_Context* cpA )
{
	if( cpA->errIndexE > 0 )
	{
		return cpA->errStackE[ --( cpA->errIndexE ) ];
	}
	else
	{
		return cpA->errStackE[ 0 ];
	}
}

/* ------------------------------------------------------------------------- */

struct bbs_Error bbs_Context_peekError( struct bbs_Context* cpA )
{
	if( cpA->errIndexE > 0 )
	{
		return cpA->errStackE[ cpA->errIndexE - 1 ];
	}
	else
	{
		return cpA->errStackE[ 0 ];
	}
}

/* ------------------------------------------------------------------------- */

flag bbs_Context_error( struct bbs_Context* cpA )
{
	return cpA->errIndexE > 0;
}

/* ------------------------------------------------------------------------- */

bbs_errorFPtr bbs_Context_setErrorHandler( struct bbs_Context* cpA, 
									       bbs_errorFPtr errorHandlerA )
{
	bbs_errorFPtr oldErrorHandlerL = cpA->errorHandlerE;
	cpA->errorHandlerE = errorHandlerA;
	return oldErrorHandlerL;
}

/* ------------------------------------------------------------------------- */

void bbs_Context_doCallback( struct bbs_Context* cpA )
{
	if( cpA->callbackHandlerE != NULL )
	{
		uint32 errorL = ( *cpA->callbackHandlerE )( cpA );
		if( errorL != bbs_ERR_OK ) 
		{
			bbs_Context_pushError( cpA, bbs_Error_create( errorL, 0, NULL, NULL ) );
		}
	}
}

/* ------------------------------------------------------------------------- */

bbs_callbackFPtr bbs_Context_setCallbackHandler( struct bbs_Context* cpA,
									       bbs_callbackFPtr callbackHandlerA )
{
	bbs_callbackFPtr oldCallbackHandlerL = cpA->callbackHandlerE;
	cpA->callbackHandlerE = callbackHandlerA;
	return oldCallbackHandlerL;
}

/* ------------------------------------------------------------------------- */

/** adds a static memory segment to memory table of context */
void bbs_Context_addStaticSeg(	struct bbs_Context* cpA,
							    uint16* memPtrA, /* pointer to memory */
								uint32 sizeA,    /* size of memory segment in 16 bit units */
								flag sharedA,    /* Indicates that this segment is to be shared among multiple objects */
								uint32 idA )     /* ID of segment, id=0: unspecified */
{
	struct bbs_MemSeg memSegL;
	bbs_DEF_fNameL( "void bbs_Context_addStaticSeg(....)" )


	/* checks */
	if( sharedA && cpA->memTblE.ssSizeE == bbs_MAX_MEM_SEGS )
	{
		bbs_ERROR1( "%s:\nShared Memory Table is full! Increase bbs_MAX_MEM_SEGS", fNameL );
		return;
	}
	if( sharedA && cpA->memTblE.esSizeE == bbs_MAX_MEM_SEGS )
	{
		bbs_ERROR1( "%s:\nExclusive Memory Table is full! Increase bbs_MAX_MEM_SEGS", fNameL );
		return;
	}


	bbs_MemSeg_init( cpA, &memSegL );
	memSegL.memPtrE = memPtrA;
	memSegL.sizeE = sizeA;
	memSegL.allocIndexE = 0;
	memSegL.sharedE = sharedA;
	memSegL.idE = idA;
	memSegL.dynMemManagerPtrE = NULL;

	if( sharedA )
	{
		cpA->memTblE.ssArrE[ cpA->memTblE.ssSizeE++ ] = memSegL;
	}
	else
	{
		cpA->memTblE.esArrE[ cpA->memTblE.esSizeE ] = memSegL;
		cpA->memTblE.espArrE[ cpA->memTblE.esSizeE ] = &cpA->memTblE.esArrE[ cpA->memTblE.esSizeE ];
		cpA->memTblE.esSizeE++;
	}
}

/* ------------------------------------------------------------------------- */

/* adds a dynamic memory segment to memory table of context
 * Upon destruction of the context object any residual will be freed automatically
 */
void bbs_Context_addDynamicSeg(	struct bbs_Context* cpA,
								bbs_mallocFPtr mallocFPtrA,	/* function pointer to external mem alloc function (s. comment of type declaration)*/
								bbs_freeFPtr freeFPtrA,     /* function pointer to external mem free function */
								flag sharedA,    /* Indicates that this segment is to be shared among multiple objects */
								uint32 idA )     /* ID of segment, id=0: unspecified */
{
	struct bbs_DynMemManager memManagerL;
	struct bbs_MemSeg memSegL;
	bbs_DEF_fNameL( "void bbs_Context_addDynamicSeg(....)" )


	/* checks */
	if( cpA->dynMemManagerArrSizeE == bbs_CONTEXT_MAX_MEM_MANAGERS )
	{
		bbs_ERROR1( "%s:\nMemory Manager Table is full! Increase bbs_CONTEXT_MAX_MEM_MANAGERS", fNameL );
		return;
	}
	if( sharedA && cpA->memTblE.ssSizeE == bbs_MAX_MEM_SEGS )
	{
		bbs_ERROR1( "%s:\nShared Memory Table is full! Increase bbs_MAX_MEM_SEGS", fNameL );
		return;
	}
	if( sharedA && cpA->memTblE.esSizeE == bbs_MAX_MEM_SEGS )
	{
		bbs_ERROR1( "%s:\nExclusive Memory Table is full! Increase bbs_MAX_MEM_SEGS", fNameL );
		return;
	}
	
	bbs_DynMemManager_init( cpA, &memManagerL );
	memManagerL.mallocFPtrE = mallocFPtrA;
	memManagerL.freeFPtrE = freeFPtrA;
	memManagerL.memPtrE = NULL;
	cpA->dynMemManagerArrE[ cpA->dynMemManagerArrSizeE++ ] = memManagerL;

	bbs_MemSeg_init( cpA, &memSegL );
	memSegL.memPtrE = NULL;
	memSegL.sizeE = 0;
	memSegL.allocIndexE = 0;
	memSegL.sharedE = sharedA;
	memSegL.idE = idA;
	memSegL.dynMemManagerPtrE = &cpA->dynMemManagerArrE[ cpA->dynMemManagerArrSizeE - 1 ];

	if( sharedA )
	{
		cpA->memTblE.ssArrE[ cpA->memTblE.ssSizeE++ ] = memSegL;
	}
	else
	{
		cpA->memTblE.esArrE[ cpA->memTblE.esSizeE ] = memSegL;
		cpA->memTblE.espArrE[ cpA->memTblE.esSizeE ] = &cpA->memTblE.esArrE[ cpA->memTblE.esSizeE ];
		cpA->memTblE.esSizeE++;
	}
}
			  
/* ------------------------------------------------------------------------- */

uint32 bbs_Context_exclAllocSize( struct bbs_Context* cpA, uint32 segIndexA )
{
	return bbs_MemSeg_allocatedSize( cpA, &cpA->memTblE.esArrE[ segIndexA ] );
}
								  
/* ------------------------------------------------------------------------- */

uint32 bbs_Context_shrdAllocSize( struct bbs_Context* cpA, uint32 segIndexA )
{
	return bbs_MemSeg_allocatedSize( cpA, &cpA->memTblE.ssArrE[ segIndexA ] );
}
								  
/* ------------------------------------------------------------------------- */

void bbs_Context_quickInit( struct bbs_Context* cpA, 
	 					    bbs_mallocFPtr mallocFPtrA,	/* function pointer to external mem alloc function (s. comment of type declaration)*/
						    bbs_freeFPtr freeFPtrA,
						    bbs_errorFPtr errorHandlerA )
{
	bbs_Context_init( cpA );
	bbs_Context_addDynamicSeg( cpA, mallocFPtrA, freeFPtrA, FALSE, 0 );
	bbs_Context_addDynamicSeg( cpA, mallocFPtrA, freeFPtrA, TRUE, 0 );
	bbs_Context_setErrorHandler( cpA, errorHandlerA );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */


