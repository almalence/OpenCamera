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

#ifndef btk_SDK_EM_H
#define btk_SDK_EM_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 *  Visual Sensing SDK
 *  SDK Context object
 */

/* ---- includes ----------------------------------------------------------- */

#include "Types.h"

/* ---- related objects  --------------------------------------------------- */

/** SDK context object */
struct btk_SDK;

/* ---- typedefs ----------------------------------------------------------- */

/** handle for SDK context */
typedef struct btk_SDK* btk_HSDK;

/** malloc function pointer */
typedef void* ( *btk_fpMalloc )( u32 sizeA );

/** free function pointer */
typedef void ( *btk_fpFree )( void* memPtrA );

/** error handler function pointer */
typedef void ( *btk_fpError )( btk_HSDK hsdkA );

/** SDK creation parameters */
typedef struct
{
	/** (optional) handler to error-handler function */
	btk_fpError  fpError;

	/** handler to malloc function */
	btk_fpMalloc fpMalloc;

	/** handler to free function */
	btk_fpFree   fpFree;

	/** pointer to preallocated exclusive (=persistent) memory (alternative to fpMalloc) */
	void* pExMem;

	/** size of external memory */
	u32 sizeExMem;

	/** pointer to preallocated shared memory (alternative to fpMalloc) */
	void* pShMem;

	/** size of external memory */
	u32 sizeShMem;

	/** pointer to 0-terminated license key string */
	const char* licenseKey;

	/** maximum image witdh used */
	u32 maxImageWidth;

	/** maximum image height used */
	u32 maxImageHeight;

} btk_SDKCreateParam;

/* ---- constants ---------------------------------------------------------- */

/* ---- functions ---------------------------------------------------------- */

/** returns default SDK parameters */
btk_DECLSPEC
btk_SDKCreateParam btk_SDK_defaultParam( void );

/** creates an SDK context using dynamic memory management */
btk_DECLSPEC
btk_Status btk_SDK_create( const btk_SDKCreateParam* pCreateParamA,
						   btk_HSDK* hpsdkA );

/** closes an SDK context */
btk_DECLSPEC
btk_Status btk_SDK_close( btk_HSDK hsdkA );

/** returns last occurred error and removes it from the error stack */
btk_DECLSPEC
btk_Error btk_SDK_getError( btk_HSDK hsdkA,
						    char* msgBufA,
							u32 msgBufSizeA );

/** returns amount of allocated exclusive memory in bytes */
btk_DECLSPEC
u32 btk_SDK_exAllocSize( btk_HSDK hsdkA );

/** returns amount of allocated shared memory in bytes */
btk_DECLSPEC
u32 btk_SDK_shAllocSize( btk_HSDK hsdkA );

/** returns total amount of allocated memory in bytes */
btk_DECLSPEC
u32 btk_SDK_allocSize( btk_HSDK hsdkA );

#ifdef __cplusplus
}
#endif

#endif /* btk_SDK_EM_H */
