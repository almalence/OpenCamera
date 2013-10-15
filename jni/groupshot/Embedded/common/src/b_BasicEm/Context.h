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

#ifndef bbs_CONTEXT_EM_H
#define bbs_CONTEXT_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"
#include "b_BasicEm/MemTbl.h"
#include "b_BasicEm/DynMemManager.h"

/* ---- related objects  --------------------------------------------------- */

struct bbs_Context;

/* ---- typedefs ----------------------------------------------------------- */

/** error handler function pointer */
typedef void ( *bbs_errorFPtr )( struct bbs_Context* cpA );

/** callback handler function pointer */
typedef uint32 ( *bbs_callbackFPtr )( struct bbs_Context* cpA );

/* ---- constants ---------------------------------------------------------- */

#define bbs_CONTEXT_MAX_ERRORS			8
#define bbs_CONTEXT_MAX_MEM_MANAGERS	8

#ifdef bbs_COMPACT_MESSAGE_HANDLING
/* characters allocated for file name string (string is stored rightbound) (minimum 1)*/
#define bbs_ERROR_MAX_FILE_CHARS	24
/* characters allocated for text message (minimum 1) */
#define bbs_ERROR_MAX_TEXT_CHARS	1
#else
/* characters allocated for file name string (string is stored rightbound) (minimum 1)*/
#define bbs_ERROR_MAX_FILE_CHARS	52
/* characters allocated for text message (minimum 1) */
#define bbs_ERROR_MAX_TEXT_CHARS	256
#endif

/* defined error codes */
#define bbs_ERR_OK						0	/* no error condition */
#define bbs_ERR_ERROR					1	/* generic error */
#define bbs_ERR_OUT_OF_MEMORY			2	/* malloc handler returns with NULL*/
#define bbs_ERR_MEMORY_OVERFLOW			3	/* not enough memory in a segment or no segment */
#define bbs_ERR_WRONG_VERSION			4	/* incompatible version in ..._memRead() */
#define bbs_ERR_CORRUPT_DATA			5	/* corrupt data in ..._memRead()*/
#define bbs_ERR_CALLBACK_ERROR			6	/* a defined error originiating from a callback function */

/* ---- object definition -------------------------------------------------- */

/** error object */
struct bbs_Error
{
	/* error code */
	uint32 errorE;

	/* line number */
	uint32 lineE;

	/* file name */
	char fileE[ bbs_ERROR_MAX_FILE_CHARS ];

	/* error text */
	char textE[ bbs_ERROR_MAX_TEXT_CHARS ];
};

/* ------------------------------------------------------------------------- */

/** context object */
struct bbs_Context 
{

	/* ---- private data --------------------------------------------------- */

	/** error stack */
	struct bbs_Error errStackE[ bbs_CONTEXT_MAX_ERRORS ];

	/** error stack index */
	uint32 errIndexE;

	/** memory table */
	struct bbs_MemTbl memTblE;

	/** multiple purpose dynamic memory managers */
	struct bbs_DynMemManager dynMemManagerArrE[ bbs_CONTEXT_MAX_MEM_MANAGERS ];

	/** number of used memory managers */
	uint32 dynMemManagerArrSizeE;

	/** error function handler */
	bbs_errorFPtr errorHandlerE;

	/** callback function handler */
	bbs_callbackFPtr callbackHandlerE;

	/** user-defined pointer */
	void* userPtrE;

	/* ---- public data ---------------------------------------------------- */

};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes bbs_Context  */
void bbs_Context_init( struct bbs_Context* cpA );

/** frees bbs_Context  */
void bbs_Context_exit( struct bbs_Context* cpA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copy operator */
void bbs_Context_copy( struct bbs_Context* cpA, const struct bbs_Context* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** composes an error object */
struct bbs_Error bbs_Error_create( uint32 errorA, uint32 lineA, const char* fileA, const char* textA, ... );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

/****** ERROR HANDLING *********/

/** puts an error onto the error stack (returns false if stack was already full) */
flag bbs_Context_pushError( struct bbs_Context* cpA, struct bbs_Error errorA );

/** takes the last error from stack and returns it (when stack is empty: returns the error at stack position 0)*/
struct bbs_Error bbs_Context_popError( struct bbs_Context* cpA );

/** returns the last error of stack without removing it (when stack is empty: returns the error at stack position 0)*/
struct bbs_Error bbs_Context_peekError( struct bbs_Context* cpA );

/** returns true if the error stack is not empty */
flag bbs_Context_error( struct bbs_Context* cpA );

/** sets error handler; returns pointer to previous error handler 
 *  Pointer to Error handler can be NULL (->no handler call)
 *  The error handler is called by function pushError diectly after an error was posted
 */
bbs_errorFPtr bbs_Context_setErrorHandler( struct bbs_Context* cpA,
									       bbs_errorFPtr errorHandlerA );

/*******************************/

/****** CALLBACK HANDLING ******/

/** call the callback handler, push error if return value is != bbs_ERR_OK */
void bbs_Context_doCallback( struct bbs_Context* cpA );

/** sets callback handler; returns pointer to previous callback handler 
 *  Pointer to callback handler can be NULL (->no handler call)
 *  The callback handler is called by function doCallback
 */
bbs_callbackFPtr bbs_Context_setCallbackHandler( struct bbs_Context* cpA,
									             bbs_callbackFPtr callbackHandlerA );

/*******************************/

/******* MEMORY HANDLING *******/

/** adds a static memory segment to memory table of context */
void bbs_Context_addStaticSeg(	struct bbs_Context* cpA,
							    uint16* memPtrA, /* pointer to memory (32bit aligned)*/
								uint32 sizeA,    /* size of memory segment in 16 bit units */
								flag sharedA,    /* Indicates that this segment is to be shared among multiple objects */
								uint32 idA );    /* ID of segment, id=0: unspecified */

/* adds a dynamic memory segment to memory table of context
 * Upon destruction of the context object any residual will be freed automatically
 */
void bbs_Context_addDynamicSeg(	struct bbs_Context* cpA,
								bbs_mallocFPtr mallocFPtrA,	/* function pointer to external mem alloc function (s. comment of type declaration)*/
								bbs_freeFPtr freeFPtrA,     /* function pointer to external mem free function */
								flag sharedA,    /* Indicates that this segment is to be shared among multiple objects */
								uint32 idA );    /* ID of segment, id=0: unspecified */


/** Returns allocated memory in selected exclusive segment in units of 16bits */
uint32 bbs_Context_exclAllocSize( struct bbs_Context* cpA, uint32 segIndexA );
								  
/** Returns allocated memory in selected exclusive segment in units of 16bits 
 *  Note that in case of static memory the return value might not reflect 
 *  the actually allocated memory amount.
 */
uint32 bbs_Context_shrdAllocSize( struct bbs_Context* cpA, uint32 segIndexA );
								  
/*******************************/


/** quick compact setup for dynamic memory management environment 
 *  creates an initialized segment with
 *  - one dynamic exclusive segment
 *  - one dynamic shared segment
 *  - error handler (can be NULL)
 *
 * Don't forget to call bbs_Context_exit on returned context if it goes out of scope
 */
void bbs_Context_quickInit( struct bbs_Context* cpA, 
	 					    bbs_mallocFPtr mallocFPtrA,	/* function pointer to external mem alloc function (s. comment of type declaration)*/
						    bbs_freeFPtr freeFPtrA,
						    bbs_errorFPtr errorHandlerA );

			  
#endif /* bbs_CONTEXT_EM_H */

