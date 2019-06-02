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

#ifndef btk_FaceFinder_Internal_EM_H
#define btk_FaceFinder_Internal_EM_H

/**
 *  FaceFinder object
 */

/* ---- includes ----------------------------------------------------------- */

#include "FaceFinder.h"
#include "DCR_Internal.h"
#include "b_APIEm/FaceFinderRef.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/** face finder object */
struct btk_FaceFinder
{
	/** SDK context handle */
	btk_HSDK hsdkE;

	/** handle id */
	btk_HandleId hidE;

	/** internal module */
	struct bpi_FaceFinderRef ffE;

	/** number of available faces */
	uint32 facesE;

	/** index into face - array */
	uint32 faceIndexE;
};

/* ---- constants ---------------------------------------------------------- */

/* ---- functions ---------------------------------------------------------- */

#endif /* btk_FaceFinder_Internal_EM_H */
