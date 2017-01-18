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

#ifndef btk_DCR_EM_H
#define btk_DCR_EM_H

#ifdef __cplusplus
extern "C" {
#endif

/**
 *  Data Carrier object
 */

/* ---- includes ----------------------------------------------------------- */

#include "SDK.h"

/* ---- related objects  --------------------------------------------------- */

/** data carrier object */
struct btk_DCR;

/* ---- typedefs ----------------------------------------------------------- */

/** handle for data carrier object */
typedef struct btk_DCR* btk_HDCR;

/** node data structure */
typedef struct
{
	s16p16  x; /* x-coordinate */
	s16p16  y; /* y-coordinate */
	s32    id; /* node id */
	s16p16 reserved; /* reserved for future versions (0) */
} btk_Node;

/** rectangle data structure */
typedef struct
{
	s16p16  xMin; /* x min coordinate */
	s16p16  yMin; /* y min coordinate */
	s16p16  xMax; /* x max coordinate */
	s16p16  yMax; /* y max coordinate */
} btk_Rect;

/** DCR creation parameters */
typedef struct
{
	/* reserved parameter (0) */
	u32 reserved;

} btk_DCRCreateParam;

/* ---- constants ---------------------------------------------------------- */

/* ---- functions ---------------------------------------------------------- */

/** returns default data carrier parameters */
btk_DECLSPEC
btk_DCRCreateParam btk_DCR_defaultParam( void );

/** creates a data carrier object */
btk_DECLSPEC
btk_Status btk_DCR_create( btk_HSDK hsdkA,
						   const btk_DCRCreateParam* pCreateParamA,
						   btk_HDCR* hpdcrA );

/** closes a data carrier object */
btk_DECLSPEC
btk_Status btk_DCR_close( btk_HDCR hdcrA );

/** deprecated (use assignImage) */
btk_DECLSPEC
btk_Status btk_DCR_assignGrayByteImage( btk_HDCR hdcrA,
									    const void* pDataA,
										u32 widthA,
										u32 heightA );

/** assigns a byte gray image referenced by pDataA to the data carrier */
btk_DECLSPEC
btk_Status btk_DCR_assignImage( btk_HDCR hdcrA,
							    const void* pDataA,
								u32 widthA,
								u32 heightA );

/** deprecated (use assignImageROI) */
btk_DECLSPEC
btk_Status btk_DCR_assignGrayByteImageROI( btk_HDCR hdcrA,
										   const void* pDataA,
										   u32 widthA,
										   u32 heightA,
										   const btk_Rect* pRectA );

/** assigns a byte gray image referenced by pDataA to the data carrier and
  * a region of interest given by pRectA.
  */
btk_DECLSPEC
btk_Status btk_DCR_assignImageROI( btk_HDCR hdcrA,
								   const void* pDataA,
								   u32 widthA,
								   u32 heightA,
								   const btk_Rect* pRectA );

/** extracts facial rectangle */
btk_DECLSPEC
btk_Status btk_DCR_getRect( btk_HDCR hdcrA,
							btk_Rect* pRectA );

/** returns number of available landmark nodes */
btk_DECLSPEC
u32 btk_DCR_nodeCount( btk_HDCR hdcrA );

/** extracts information about indexed node */
btk_DECLSPEC
btk_Status btk_DCR_getNode( btk_HDCR hdcrA,
						    u32 indexA,
							btk_Node* pNodeA );

/** returns confidence 8.24 fixed format */
btk_DECLSPEC
s8p24 btk_DCR_confidence( btk_HDCR hdcrA );

/** returns approval flag (0=false; 1=true)*/
btk_DECLSPEC
u32 btk_DCR_approved( btk_HDCR hdcrA );


#ifdef __cplusplus
}
#endif

#endif /* btk_DCR_EM_H */
