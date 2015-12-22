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

#include "SDK_Internal.h"
#include "b_BasicEm/Functions.h"

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- functions ---------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void btk_SDK_init( struct btk_SDK* ptrA )
{
	bbs_Context_init( &ptrA->contextE );
	ptrA->hidE = btk_HID_SDK;
	ptrA->refCtrE = 0;
	ptrA->mallocFPtrE = NULL;
	ptrA->freeFPtrE = NULL;
	ptrA->errorFPtrE = NULL;
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
}

/* ------------------------------------------------------------------------- */

void btk_SDK_exit( struct btk_SDK* ptrA )
{
	bbs_Context_exit( &ptrA->contextE );
	ptrA->hidE = btk_HID_SDK;
	ptrA->refCtrE = 0;
	ptrA->mallocFPtrE = NULL;
	ptrA->freeFPtrE = NULL;
	ptrA->errorFPtrE = NULL;
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
}

/* ------------------------------------------------------------------------- */

/* malloc wrapper */
void* btk_malloc( struct bbs_Context* cpA,
				  const struct bbs_MemSeg* memSegPtrA,
				  uint32 sizeA )
{
	btk_HSDK hsdkL = ( btk_HSDK )cpA;
	if( hsdkL->mallocFPtrE != NULL )
	{
		return hsdkL->mallocFPtrE( sizeA );
	}
	else
	{
		return NULL;
	}
}

/* ------------------------------------------------------------------------- */

/** error handler wrapper */
void btk_error( struct bbs_Context* cpA )
{
	btk_HSDK hsdkL = ( btk_HSDK )cpA;
	if( hsdkL->errorFPtrE != NULL )
	{
		hsdkL->errorFPtrE( hsdkL );
	}
}

/* ------------------------------------------------------------------------- */

btk_SDKCreateParam btk_SDK_defaultParam()
{
	btk_SDKCreateParam paramL;
	paramL.fpError = NULL;
	paramL.fpMalloc = NULL;
	paramL.fpFree = NULL;
	paramL.pExMem = NULL;
	paramL.sizeExMem = 0;
	paramL.pShMem = NULL;
	paramL.sizeShMem = 0;
	paramL.licenseKey = NULL;
	paramL.maxImageWidth = 0;
	paramL.maxImageHeight = 0;
	return paramL;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_SDK_create( const btk_SDKCreateParam* pCreateParamA,
						   btk_HSDK* hpsdkA )
{
	btk_HSDK hsdkL = NULL;
	if( hpsdkA == NULL )	return btk_STATUS_INVALID_HANDLE;
	if( *hpsdkA != NULL )	return btk_STATUS_INVALID_HANDLE;

	if( pCreateParamA->fpMalloc != NULL )
	{
		if( pCreateParamA->fpFree == NULL ) return btk_STATUS_INVALID_HANDLE;

		/* allocate context */
		hsdkL = ( btk_HSDK )pCreateParamA->fpMalloc( bbs_SIZEOF8( struct btk_SDK ) );
		if( hsdkL == NULL ) return btk_STATUS_INVALID_HANDLE;

		btk_SDK_init( hsdkL );

		/* initialize SDK context */
		hsdkL->mallocFPtrE	= pCreateParamA->fpMalloc;
		hsdkL->freeFPtrE	= pCreateParamA->fpFree;
		hsdkL->errorFPtrE	= pCreateParamA->fpError;

		/* initialize core context */
		bbs_Context_quickInit( &hsdkL->contextE, btk_malloc, pCreateParamA->fpFree, btk_error );
		if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;
	}
	else
	{
		uint16* exMemPtrL = ( uint16* )pCreateParamA->pExMem;
		uint32 exMemSizeL = pCreateParamA->sizeExMem >> 1;

		if( pCreateParamA->pExMem == NULL )					 return btk_STATUS_INVALID_HANDLE;
		if( pCreateParamA->pShMem == NULL )					 return btk_STATUS_INVALID_HANDLE;
		if( pCreateParamA->pExMem == pCreateParamA->pShMem ) return btk_STATUS_INVALID_HANDLE;

		if( pCreateParamA->sizeExMem < bbs_SIZEOF16( struct btk_SDK ) ) return btk_STATUS_INVALID_HANDLE;

		/* allocate context */
		hsdkL = ( btk_HSDK )exMemPtrL;
		exMemPtrL  += bbs_SIZEOF16( struct btk_SDK );
		exMemSizeL -= bbs_SIZEOF16( struct btk_SDK );

		btk_SDK_init( hsdkL );

		hsdkL->errorFPtrE	= pCreateParamA->fpError;
		hsdkL->contextE.errorHandlerE = btk_error;

		/* initialize core context */
		bbs_Context_addStaticSeg( &hsdkL->contextE, exMemPtrL, exMemSizeL, FALSE, 0 );
		bbs_Context_addStaticSeg( &hsdkL->contextE, pCreateParamA->pShMem, pCreateParamA->sizeShMem >> 1, TRUE, 0 );
	}

	hsdkL->maxImageWidthE = pCreateParamA->maxImageWidth;
	hsdkL->maxImageHeightE = pCreateParamA->maxImageHeight;

	*hpsdkA = hsdkL;
	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_SDK_close( btk_HSDK hsdkA )
{
	const char* fNameL = "btk_SDK_close";

	if( hsdkA == NULL )							return btk_STATUS_INVALID_HANDLE;
	if( hsdkA->hidE != btk_HID_SDK )			return btk_STATUS_INVALID_HANDLE;
	if( bbs_Context_error( &hsdkA->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	if( hsdkA->refCtrE > 0 )
	{
		bbs_Context_pushError( &hsdkA->contextE,
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nThis SDK context is still in use by %i objects!\n"
							                                             "Close all instances of the context scope first.\n",
																		  fNameL,
																		  hsdkA->refCtrE ) );

		return btk_STATUS_ERROR;
	}

	if( hsdkA->freeFPtrE )
	{
		btk_fpFree freeFPtrL = hsdkA->freeFPtrE;
		btk_SDK_exit( hsdkA );
		freeFPtrL( hsdkA );
	}
	else
	{
		btk_SDK_exit( hsdkA );
	}

	/* btk_SDK_exit clears error stack and does not produce an error condition */

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Error btk_SDK_getError( btk_HSDK hsdkA, char* msgBufA, u32 msgBufSizeA )
{
	if( hsdkA == NULL )					return btk_ERR_CORRUPT_DATA;
	if( hsdkA->hidE != btk_HID_SDK )	return btk_STATUS_INVALID_HANDLE;

	if( bbs_Context_error( &hsdkA->contextE ) )
	{
		struct bbs_Error errL = bbs_Context_popError( &hsdkA->contextE );
		if( msgBufA != NULL ) bbs_strncpy( msgBufA, errL.textE, msgBufSizeA );
		switch( errL.errorE )
		{
			case bbs_ERR_OUT_OF_MEMORY:		return btk_ERR_MEMORY;
			case bbs_ERR_MEMORY_OVERFLOW:	return btk_ERR_MEMORY;
			case bbs_ERR_WRONG_VERSION:		return btk_ERR_VERSION;
			case bbs_ERR_CORRUPT_DATA:		return btk_ERR_CORRUPT_DATA;
			default:						return btk_ERR_INTERNAL;
		}
	}

	return btk_ERR_NO_ERROR;
}

/* ------------------------------------------------------------------------- */

u32 btk_SDK_exAllocSize( btk_HSDK hsdkA )
{
	if( hsdkA == NULL )					return 0;
	if( hsdkA->hidE != btk_HID_SDK )	return 0;
	return ( bbs_Context_exclAllocSize( &hsdkA->contextE, 0 ) * 2 ) + bbs_SIZEOF8( struct btk_SDK );
}

/* ------------------------------------------------------------------------- */

u32 btk_SDK_shAllocSize( btk_HSDK hsdkA )
{
	if( hsdkA == NULL )					return 0;
	if( hsdkA->hidE != btk_HID_SDK )	return 0;
	return bbs_Context_shrdAllocSize( &hsdkA->contextE, 0 ) * 2;
}

/* ------------------------------------------------------------------------- */

u32 btk_SDK_allocSize( btk_HSDK hsdkA )
{
	return  btk_SDK_exAllocSize( hsdkA ) + btk_SDK_shAllocSize( hsdkA );
}

/* ------------------------------------------------------------------------- */

btk_Status btk_SDK_paramConsistencyTest( struct btk_SDK* hsdkA,
										 const void* memPtrA,
										 u32 memSizeA,
										 const char* fNameA )
{
	const uint16* memPtrL = ( uint16* )memPtrA;
	uint32 memSizeL;
	uint32 iL;
	uint16 sumL = 0;

	if( memSizeA < sizeof( memSizeL ) )
	{
		bbs_Context_pushError( &hsdkA->contextE,
							   bbs_Error_create( bbs_ERR_ERROR, 0, NULL,
					               "%s:\nCorrupt parameter data.", fNameA ) );
		return btk_STATUS_ERROR;
	}

	memPtrL += bbs_memRead32( &memSizeL, memPtrL );

	if( memSizeA < ( memSizeL << 1 ) )
	{
		bbs_Context_pushError( &hsdkA->contextE,
							   bbs_Error_create( bbs_ERR_ERROR, 0, NULL,
					               "%s:\nCorrupt parameter data.", fNameA ) );
		return btk_STATUS_ERROR;
	}

	memPtrL = ( uint16* )memPtrA;

	for( iL = 0; iL < memSizeL; iL++ )
	{
		uint16 valL = 0;
		memPtrL += bbs_memRead16( &valL, memPtrL );
		sumL += valL;
	}

    if( sumL != 0xFFFF )
	{
		bbs_Context_pushError( &hsdkA->contextE,
							   bbs_Error_create( bbs_ERR_ERROR, 0, NULL,
					               "%s:\nChecksum error; corrupt parameter data.", fNameA ) );
		return btk_STATUS_ERROR;
	}

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
