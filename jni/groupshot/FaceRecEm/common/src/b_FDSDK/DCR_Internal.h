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

#ifndef btk_DCR_Internal_EM_H
#define btk_DCR_Internal_EM_H

/**
 *  DCR object
 */

/* ---- includes ----------------------------------------------------------- */

#include "DCR.h"
#include "SDK_Internal.h"
#include "b_APIEm/DCR.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/** data carrier object */
struct btk_DCR
{
	/** SDK context handle */
	btk_HSDK hsdkE;

	/** handle id */
	btk_HandleId hidE;

	/** API DCR */
	struct bpi_DCR dcrE;
};

/* ---- constants ---------------------------------------------------------- */

/* ---- functions ---------------------------------------------------------- */

#endif /* btk_DCR_Internal_EM_H */
