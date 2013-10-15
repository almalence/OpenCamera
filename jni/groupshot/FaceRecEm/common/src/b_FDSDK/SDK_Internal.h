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

#ifndef btk_SDK_Internal_EM_H
#define btk_SDK_Internal_EM_H

/**
 *  SDK object
 */

/* ---- includes ----------------------------------------------------------- */

#include "SDK.h"
#include "b_BasicEm/Context.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/** function return status */
typedef enum
{
	/** sdk handle id */
	btk_HID_SDK,

	/** dcr handle id */
	btk_HID_DCR,

	/** face finder handle id */
	btk_HID_FF

} btk_HandleId;


/** SDK context object */
struct btk_SDK
{
	/** context (must occur as first element) */
	struct bbs_Context contextE;

	/** handle id */
	btk_HandleId hidE;

	/** reference counter */
	u32 refCtrE;

	/** ptr to malloc function */
	btk_fpMalloc mallocFPtrE;

	/** ptr to free function */
	btk_fpFree freeFPtrE;

	/** error handler function pointer */
	btk_fpError errorFPtrE;

	/* maximum image witdh used */
	u32 maxImageWidthE;

	/* maximum image height used */
	u32 maxImageHeightE;
};

/* ---- constants ---------------------------------------------------------- */

/* ---- functions ---------------------------------------------------------- */

/** tests parameter consistency */
btk_Status btk_SDK_paramConsistencyTest( struct btk_SDK* hsdkA,
										 const void* memPtrA,
										 u32 memSizeA,
										 const char* fNameA );

#endif /* btk_SDK_Internal_EM_H */
