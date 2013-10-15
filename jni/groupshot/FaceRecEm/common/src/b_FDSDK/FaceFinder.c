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

#include "FaceFinder_Internal.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- functions ---------------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

void btk_FaceFinder_init( struct bbs_Context* cpA, struct btk_FaceFinder* ptrA )
{
	ptrA->hsdkE = NULL;
	ptrA->hidE = btk_HID_FF;

	bpi_FaceFinderRef_init( cpA, &ptrA->ffE );

	ptrA->facesE = 0;
	ptrA->faceIndexE = 0;
}

/* ------------------------------------------------------------------------- */

void btk_FaceFinder_exit( struct bbs_Context* cpA, struct btk_FaceFinder* ptrA )
{
	ptrA->hsdkE = NULL;
	ptrA->hidE = btk_HID_FF;

	bpi_FaceFinderRef_exit( cpA, &ptrA->ffE );

	ptrA->facesE = 0;
	ptrA->faceIndexE = 0;
}

/* ------------------------------------------------------------------------- */

btk_FaceFinderCreateParam btk_FaceFinder_defaultParam()
{
	btk_FaceFinderCreateParam paramL;
	paramL.reserved = 0;
	paramL.pModuleParam = NULL;
	paramL.moduleParamSize = 0;
	paramL.maxDetectableFaces = 0;
	return paramL;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_FaceFinder_create( btk_HSDK hsdkA,     /* sdk handle */
								  const btk_FaceFinderCreateParam* pCreateParamA,
								  btk_HFaceFinder* hpFaceFinderA )
{
	const char* fNameL = "btk_FaceFinder_create";

	btk_HFaceFinder hFaceFinderL = NULL;

	if( hpFaceFinderA == NULL )					return btk_STATUS_INVALID_HANDLE;
	if( *hpFaceFinderA != NULL )				return btk_STATUS_INVALID_HANDLE;
	if( hsdkA == NULL )							return btk_STATUS_INVALID_HANDLE;
	if( hsdkA->hidE != btk_HID_SDK )			return btk_STATUS_INVALID_HANDLE;
	if( pCreateParamA == NULL )					return btk_STATUS_INVALID_HANDLE;
	if( bbs_Context_error( &hsdkA->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	hFaceFinderL = ( btk_HFaceFinder )bbs_MemSeg_alloc( &hsdkA->contextE, hsdkA->contextE.memTblE.espArrE[ 0 ], bbs_SIZEOF16( struct btk_FaceFinder ) );
	if( bbs_Context_error( &hsdkA->contextE ) ) return btk_STATUS_ERROR;

	btk_FaceFinder_init( &hsdkA->contextE, hFaceFinderL );
	if( bbs_Context_error( &hsdkA->contextE ) ) return btk_STATUS_ERROR;

	hFaceFinderL->hsdkE = hsdkA;

	if( btk_SDK_paramConsistencyTest( hsdkA, pCreateParamA->pModuleParam, pCreateParamA->moduleParamSize, fNameL ) == btk_STATUS_ERROR ) return btk_STATUS_ERROR;

	if( hsdkA->maxImageWidthE * hsdkA->maxImageHeightE == 0 )
	{
		bbs_Context_pushError( &hsdkA->contextE, 
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nSDK parameter maxImageWidth or maxImageWidth is 0!\n"
							                                             "Since SDK version 1.3.0 the maximum image size must be specified when creating the SDK handle.\n"
																		 "Set the values in *pCreateParamA when you call function btk_SDK_create.", fNameL ) );
		return btk_STATUS_ERROR;
	}

	bpi_FaceFinderRef_memRead( &hsdkA->contextE,
							   &hFaceFinderL->ffE,
							   hsdkA->maxImageWidthE,
							   hsdkA->maxImageHeightE,
							   pCreateParamA->pModuleParam,
							   &hsdkA->contextE.memTblE );

	if( bbs_Context_error( &hsdkA->contextE ) ) return btk_STATUS_ERROR;

	*hpFaceFinderA = hFaceFinderL;
	hsdkA->refCtrE++;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_FaceFinder_close( btk_HFaceFinder hFaceFinderA )
{
	btk_HSDK hsdkL = NULL;
	if( hFaceFinderA == NULL )				return btk_STATUS_INVALID_HANDLE;
	if( hFaceFinderA->hidE != btk_HID_FF )	return btk_STATUS_INVALID_HANDLE;
	if( hFaceFinderA->hsdkE == NULL )		return btk_STATUS_INVALID_HANDLE;
	hsdkL = hFaceFinderA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	hsdkL->refCtrE--;

	btk_FaceFinder_exit( &hsdkL->contextE, hFaceFinderA );
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	bbs_MemSeg_free( &hsdkL->contextE, hsdkL->contextE.memTblE.espArrE[ 0 ], hFaceFinderA );
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_FaceFinder_setRange( btk_HFaceFinder hFaceFinderA,
								    u32 minDistA,
									u32 maxDistA )
{
	btk_HSDK hsdkL = NULL;
	if( hFaceFinderA == NULL )				return btk_STATUS_INVALID_HANDLE;
	if( hFaceFinderA->hidE != btk_HID_FF )	return btk_STATUS_INVALID_HANDLE;
	hsdkL = hFaceFinderA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	bpi_FaceFinderRef_setRange( &hsdkL->contextE, &hFaceFinderA->ffE, minDistA, maxDistA );
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_FaceFinder_putDCR( btk_HFaceFinder hFaceFinderA,
								  btk_HDCR hdcrA )
{
	const char* fNameL = "btk_FaceFinder_putDCR";

	btk_HSDK hsdkL = NULL;
	if( hFaceFinderA == NULL )				return btk_STATUS_INVALID_HANDLE;
	if( hFaceFinderA->hidE != btk_HID_FF )	return btk_STATUS_INVALID_HANDLE;
	if( hdcrA == NULL )			return btk_STATUS_INVALID_HANDLE;
	hsdkL = hFaceFinderA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	if( hdcrA->dcrE.imageDataPtrE == NULL )
	{
		bbs_Context_pushError( &hsdkL->contextE,
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL,
							       "%s:\nNo image was assigned to data carrier", fNameL ) );
	}

	hFaceFinderA->facesE = bpi_FaceFinderRef_putDcr( &hsdkL->contextE,
												     &hFaceFinderA->ffE,
													 &hdcrA->dcrE );

	hFaceFinderA->faceIndexE = 0;
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

u32 btk_FaceFinder_faces( btk_HFaceFinder hFaceFinderA )
{
	if( hFaceFinderA == NULL )				return 0;
	if( hFaceFinderA->hidE != btk_HID_FF )	return 0;
	return hFaceFinderA->facesE - hFaceFinderA->faceIndexE;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_FaceFinder_getDCR( btk_HFaceFinder hFaceFinderA,
								  btk_HDCR hdcrA )
{
	btk_HSDK hsdkL = NULL;
	if( hFaceFinderA == NULL )				return btk_STATUS_INVALID_HANDLE;
	if( hFaceFinderA->hidE != btk_HID_FF )	return btk_STATUS_INVALID_HANDLE;
	if( hdcrA == NULL )			return btk_STATUS_INVALID_HANDLE;
	hsdkL = hFaceFinderA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	if( hFaceFinderA->faceIndexE < hFaceFinderA->facesE )
	{
		bpi_FaceFinderRef_getDcr( &hsdkL->contextE,
								  &hFaceFinderA->ffE,
								   hFaceFinderA->faceIndexE,
								  &hdcrA->dcrE );

		if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

		hdcrA->dcrE.approvedE = TRUE;
		hFaceFinderA->faceIndexE++;
	}
	else
	{
		bpi_FaceFinderRef_getDcr( &hsdkL->contextE,
								  &hFaceFinderA->ffE,
								  0,
								  &hdcrA->dcrE );
		hdcrA->dcrE.approvedE = FALSE;
	}

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_FaceFinder_process( btk_HFaceFinder hFaceFinderA,
								   btk_HDCR hdcrA )
{
	const char* fNameL = "btk_FaceFinder_process";
	int32 confL;

	btk_HSDK hsdkL = NULL;
	if( hFaceFinderA == NULL )				return btk_STATUS_INVALID_HANDLE;
	if( hFaceFinderA->hidE != btk_HID_FF )	return btk_STATUS_INVALID_HANDLE;
	if( hdcrA == NULL )						return btk_STATUS_INVALID_HANDLE;
	hsdkL = hFaceFinderA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	if( hdcrA->dcrE.imageDataPtrE == NULL )
	{
		bbs_Context_pushError( &hsdkL->contextE,
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL,
							       "%s:\nNo image was assigned to data carrier", fNameL ) );
	}

	confL = bpi_FaceFinderRef_process( &hsdkL->contextE,
									   &hFaceFinderA->ffE,
									   &hdcrA->dcrE );

	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	hdcrA->dcrE.confidenceE = confL;
	hdcrA->dcrE.approvedE = confL > ( ( int32 )1 << 23 );

	hFaceFinderA->faceIndexE = 0;
	hFaceFinderA->facesE = 0;

	bts_IdCluster2D_copy( &hsdkL->contextE,
		                  &hdcrA->dcrE.sdkClusterE,
						  &hdcrA->dcrE.mainClusterE );

	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
