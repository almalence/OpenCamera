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

#ifndef btk_FaceFinder_EM_H
#define btk_FaceFinder_EM_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 *  Face Finder object
 */

/* ---- includes ----------------------------------------------------------- */

#include "DCR.h"

/* ---- related objects  --------------------------------------------------- */

/** face finder object */
struct btk_FaceFinder;

/* ---- typedefs ----------------------------------------------------------- */

/** handle for face finder object */
typedef struct btk_FaceFinder* btk_HFaceFinder;

/** FaceFinder creation parameters */
typedef struct
{
	/* reserved parameter */
	u32 reserved;

	/* obaque module parameters */
	void* pModuleParam;

	/* size of module parameters */
	u32 moduleParamSize;

	/* maximum number of detectable faces */
	u32 maxDetectableFaces;

} btk_FaceFinderCreateParam;

/* ---- constants ---------------------------------------------------------- */

/* ---- functions ---------------------------------------------------------- */

/** returns default FaceFinder parameters */
btk_DECLSPEC
btk_FaceFinderCreateParam btk_FaceFinder_defaultParam( void );

/** creates a face finder object */
btk_DECLSPEC
btk_Status btk_FaceFinder_create( btk_HSDK hsdkA,     /* sdk handle */
								  const btk_FaceFinderCreateParam* pCreateParamA,
								  btk_HFaceFinder* hpFaceFinderA );

/** closes a face finder object */
btk_DECLSPEC
btk_Status btk_FaceFinder_close( btk_HFaceFinder hFaceFinderA );

/** sets eye distance range */
btk_DECLSPEC
btk_Status btk_FaceFinder_setRange( btk_HFaceFinder hFaceFinderA,
								    u32 minDistA,
									u32 maxDistA );

/** passes a DCR object and triggers image processing */
btk_DECLSPEC
btk_Status btk_FaceFinder_putDCR( btk_HFaceFinder hFaceFinderA,
								  btk_HDCR hdcrA );

/** returns number of faces that can be retrieved from face finder with function btk_FaceFinder_getDCR */
btk_DECLSPEC
u32 btk_FaceFinder_faces( btk_HFaceFinder hFaceFinderA );

/** retrieves a DCR object for each detected face */
btk_DECLSPEC
btk_Status btk_FaceFinder_getDCR( btk_HFaceFinder hFaceFinderA,
								  btk_HDCR hdcrA );

/** processes DCR for single face detection */
btk_DECLSPEC
btk_Status btk_FaceFinder_process( btk_HFaceFinder hFaceFinderA,
								   btk_HDCR hdcrA );

#ifdef __cplusplus
}
#endif

#endif /* btk_FaceFinder_EM_H */
