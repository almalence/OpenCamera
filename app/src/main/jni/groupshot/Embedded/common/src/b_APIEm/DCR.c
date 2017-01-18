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

#include "b_APIEm/DCR.h"
#include "b_BasicEm/Functions.h"

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

void bpi_DCR_init( struct bbs_Context* cpA,
				   struct bpi_DCR* ptrA )
{
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
	ptrA->imageDataPtrE = NULL;
	ptrA->imageWidthE = 0;
	ptrA->imageHeightE = 0;
	bts_Int16Vec2D_init( &ptrA->offsE );
	bts_IdCluster2D_init( cpA, &ptrA->mainClusterE );
	bts_IdCluster2D_init( cpA, &ptrA->sdkClusterE );
	ptrA->confidenceE = 0;
	ptrA->approvedE = FALSE;
	ptrA->idE = 0;
	ptrA->roiRectE = bts_Int16Rect_create( 0, 0, 0, 0 );
	bbs_UInt16Arr_init( cpA, &ptrA->cueDataE );
}

/* ------------------------------------------------------------------------- */

void bpi_DCR_exit( struct bbs_Context* cpA,
				   struct bpi_DCR* ptrA )
{
	ptrA->maxImageWidthE = 0;
	ptrA->maxImageHeightE = 0;
	ptrA->imageDataPtrE = NULL;
	ptrA->imageWidthE = 0;
	ptrA->imageHeightE = 0;
	bts_Int16Vec2D_exit( &ptrA->offsE );
	bts_IdCluster2D_exit( cpA, &ptrA->mainClusterE );
	bts_IdCluster2D_exit( cpA, &ptrA->sdkClusterE );
	ptrA->confidenceE = 0;
	ptrA->approvedE = FALSE;
	ptrA->idE = 0;
	bbs_UInt16Arr_exit( cpA, &ptrA->cueDataE );
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ operators } -------------------------------------------------- */
/*                                                                           */
/* ========================================================================= */

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

void bpi_DCR_create( struct bbs_Context* cpA,
					 struct bpi_DCR* ptrA,
					 uint32 imageWidthA,
					 uint32 imageHeightA,
					 uint32 cueSizeA,
					 struct bbs_MemTbl* mtpA )
{
	struct bbs_MemTbl memTblL = *mtpA;
	struct bbs_MemSeg* espL =
            bbs_MemTbl_fastestSegPtr( cpA, &memTblL,
                                      bpi_DCR_MAX_CLUSTER_SIZE * bbs_SIZEOF16( struct bts_Int16Vec2D ) );
	if( bbs_Context_error( cpA ) ) return;

	bts_IdCluster2D_create( cpA, &ptrA->mainClusterE, bpi_DCR_MAX_CLUSTER_SIZE, espL );
	bts_IdCluster2D_size( cpA, &ptrA->mainClusterE, 0 );
	if( bbs_Context_error( cpA ) ) return;
	bts_IdCluster2D_create( cpA, &ptrA->sdkClusterE, bpi_DCR_MAX_SDK_CLUSTER_SIZE, espL );
	bts_IdCluster2D_size( cpA, &ptrA->sdkClusterE, 0 );
	if( bbs_Context_error( cpA ) ) return;
	if( bbs_Context_error( cpA ) ) return;
	bbs_UInt16Arr_create( cpA, &ptrA->cueDataE, cueSizeA, espL );
	bbs_UInt16Arr_size( cpA, &ptrA->cueDataE, 0 );

	ptrA->maxImageWidthE = imageWidthA;
	ptrA->maxImageHeightE = imageHeightA;
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

void bpi_DCR_assignGrayByteImage( struct bbs_Context* cpA,
								  struct bpi_DCR* ptrA, 
								  const void* bufferPtrA, 
								  uint32 widthA, 
								  uint32 heightA )
{
	bbs_DEF_fNameL( "void bpi_DCR_assignGrayByteImage( struct bbs_Context* cpA, struct bpi_DCR* ptrA, const void* bufferPtrA, uint32 widthA, uint32 heightA )" )

	if( widthA > ptrA->maxImageWidthE || heightA > ptrA->maxImageHeightE )
	{
		bbs_ERROR5( "%s:\nSize of assigned image (%ix%i) exceeds maximum size defined at DCR initialization (%ix%i).",
				    fNameL,
					widthA, heightA,
					ptrA->maxImageWidthE, ptrA->maxImageHeightE	);
		return;
	}

	if( ( widthA & 1 ) != 0 )
	{
		bbs_ERROR1( "%s:\nWidth of image must be even.\n", fNameL );
		return;
	}

	ptrA->imageDataPtrE = ( void* )bufferPtrA;
	ptrA->imageWidthE = widthA;
	ptrA->imageHeightE = heightA;

	/* reset some data */
	ptrA->roiRectE = bts_Int16Rect_create( 0, 0, widthA, heightA );
	bts_IdCluster2D_size( cpA, &ptrA->mainClusterE, 0 );
	bts_IdCluster2D_size( cpA, &ptrA->sdkClusterE, 0 );
	bbs_UInt16Arr_size( cpA, &ptrA->cueDataE, 0 );
}

/* ------------------------------------------------------------------------- */

void bpi_DCR_assignGrayByteImageROI( struct bbs_Context* cpA,
									 struct bpi_DCR* ptrA, 
									 const void* bufferPtrA, 
									 uint32 widthA, 
									 uint32 heightA,
									 const struct bts_Int16Rect* pRectA )
{
	bbs_DEF_fNameL( "void bpi_DCR_assignGrayByteImageROI( struct bpi_DCR* ptrA, const void* bufferPtrA, uint32 widthA, uint32 heightA )" )

	if( widthA > ptrA->maxImageWidthE || heightA > ptrA->maxImageHeightE )
	{
		bbs_ERROR5( "%s:\nSize of assigned image (%ix%i) exceeds maximum size defined at DCR initialization (%ix%i).",
				    fNameL,
					widthA, heightA,
					ptrA->maxImageWidthE, ptrA->maxImageHeightE	);
		return;
	}

	if( ( widthA & 1 ) != 0 )
	{
		bbs_ERROR1( "%s:\nWidth of image must be even.\n",
				    fNameL );
		return;
	}

	if( pRectA->x2E < pRectA->x1E || pRectA->y2E < pRectA->y1E || 
		pRectA->x1E < 0           || pRectA->y1E < 0 ||
		pRectA->x2E > ( int32 )widthA || pRectA->y2E > ( int32 )heightA )
	{
		bbs_ERROR1( "%s:\nInvalid ROI rectangle.\n", fNameL );
		return;
	}

	ptrA->imageDataPtrE = ( void* )bufferPtrA;
	ptrA->imageWidthE = widthA;
	ptrA->imageHeightE = heightA;

	/* reset some data */
	ptrA->roiRectE = *pRectA;
	bts_IdCluster2D_size( cpA, &ptrA->mainClusterE, 0 );
	bts_IdCluster2D_size( cpA, &ptrA->sdkClusterE, 0 );
	bbs_UInt16Arr_size( cpA, &ptrA->cueDataE, 0 );
}

/* ------------------------------------------------------------------------- */

int32 bpi_DCR_confidence( struct bbs_Context* cpA,
						  const struct bpi_DCR* ptrA )
{
	return ptrA->confidenceE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
