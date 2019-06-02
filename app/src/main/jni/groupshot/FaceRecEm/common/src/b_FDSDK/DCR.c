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

#include "DCR_Internal.h"

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

void btk_DCR_init( struct bbs_Context* cpA, struct btk_DCR* ptrA )
{
	ptrA->hsdkE = NULL;
	ptrA->hidE = btk_HID_DCR;
	bpi_DCR_init( cpA, &ptrA->dcrE );
}

/* ------------------------------------------------------------------------- */

void btk_DCR_exit( struct bbs_Context* cpA, struct btk_DCR* ptrA )
{
	ptrA->hsdkE = NULL;
	ptrA->hidE = btk_HID_DCR;
	bpi_DCR_exit( cpA, &ptrA->dcrE );
}

/* ------------------------------------------------------------------------- */

btk_DCRCreateParam btk_DCR_defaultParam()
{
	btk_DCRCreateParam paramL;
	paramL.reserved = 0;
	return paramL;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_DCR_create( btk_HSDK hsdkA, 
						   const btk_DCRCreateParam* pCreateParamA,
						   btk_HDCR* hpdcrA )
{
	btk_HDCR hdcrL = NULL;

	if( hpdcrA == NULL )						return btk_STATUS_INVALID_HANDLE;
	if( *hpdcrA != NULL )						return btk_STATUS_INVALID_HANDLE;
	if( hsdkA == NULL )							return btk_STATUS_INVALID_HANDLE;
	if( hsdkA->hidE != btk_HID_SDK )			return btk_STATUS_INVALID_HANDLE;
	if( pCreateParamA == NULL )					return btk_STATUS_INVALID_HANDLE;
	if( bbs_Context_error( &hsdkA->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	hdcrL = ( btk_HDCR )bbs_MemSeg_alloc( &hsdkA->contextE, hsdkA->contextE.memTblE.espArrE[ 0 ], bbs_SIZEOF16( struct btk_DCR ) );
	if( bbs_Context_error( &hsdkA->contextE ) ) return btk_STATUS_ERROR;

	btk_DCR_init( &hsdkA->contextE, hdcrL );
	hdcrL->hsdkE = hsdkA;

	if( bbs_Context_error( &hsdkA->contextE ) ) return btk_STATUS_ERROR;

	bpi_DCR_create( &hsdkA->contextE,
		            &hdcrL->dcrE,
					hsdkA->maxImageWidthE,
					hsdkA->maxImageHeightE,
#ifdef btk_FRSDK
					6000 >> 1,
#else
                        0,
#endif
					&hsdkA->contextE.memTblE );

	if( bbs_Context_error( &hsdkA->contextE ) ) return btk_STATUS_ERROR;

	*hpdcrA = hdcrL;
	hsdkA->refCtrE++;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_DCR_close( btk_HDCR hdcrA )
{
	btk_HSDK hsdkL = NULL;
	if( hdcrA == NULL )					return btk_STATUS_INVALID_HANDLE;
	if( hdcrA->hidE != btk_HID_DCR )	return btk_STATUS_INVALID_HANDLE;
	if( hdcrA->hsdkE == NULL )			return btk_STATUS_INVALID_HANDLE;
	hsdkL = hdcrA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	hsdkL->refCtrE--;

	btk_DCR_exit( &hsdkL->contextE, hdcrA );
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	bbs_MemSeg_free( &hsdkL->contextE, hsdkL->contextE.memTblE.espArrE[ 0 ], hdcrA );
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_DCR_assignGrayByteImage( btk_HDCR hdcrA, 
									    const void* pDataA, 
										u32 widthA, 
										u32 heightA )
{
	return btk_DCR_assignImage( hdcrA, pDataA, widthA, heightA );
}

/* ------------------------------------------------------------------------- */

btk_Status btk_DCR_assignImage( btk_HDCR hdcrA, 
							    const void* pDataA, 
								u32 widthA, 
								u32 heightA )
{
	const char* fNameL = "btk_DCR_assignImage";

	btk_HSDK hsdkL = NULL;
	if( hdcrA == NULL )					return btk_STATUS_INVALID_HANDLE;
	if( hdcrA->hidE != btk_HID_DCR )	return btk_STATUS_INVALID_HANDLE;
	hsdkL = hdcrA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	if( pDataA == NULL )
	{
		bbs_Context_pushError( &hsdkL->contextE, 
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nAssigned image references inavlid memory", fNameL ) );

		return btk_STATUS_ERROR;
	}

	if( widthA == 0 || heightA == 0 )
	{
		bbs_Context_pushError( &hsdkL->contextE, 
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nAssigned image has size 0", fNameL ) );

		return btk_STATUS_ERROR;
	}

	bpi_DCR_assignGrayByteImage( &hsdkL->contextE, &hdcrA->dcrE, pDataA, widthA, heightA );
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_DCR_assignGrayByteImageROI( btk_HDCR hdcrA, 
										   const void* pDataA, 
										   u32 widthA, 
										   u32 heightA,
										   const btk_Rect* pRectA )
{
	return btk_DCR_assignImageROI( hdcrA, pDataA, widthA, heightA, pRectA );
}

/* ------------------------------------------------------------------------- */

btk_Status btk_DCR_assignImageROI( btk_HDCR hdcrA, 
								   const void* pDataA, 
								   u32 widthA, 
								   u32 heightA,
								   const btk_Rect* pRectA )
{
	const char* fNameL = "btk_DCR_assignGrayByteImageROI";

	btk_HSDK hsdkL = NULL;
	if( hdcrA == NULL )					return btk_STATUS_INVALID_HANDLE;
	if( hdcrA->hidE != btk_HID_DCR )	return btk_STATUS_INVALID_HANDLE;
	hsdkL = hdcrA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	if( pDataA == NULL )
	{
		bbs_Context_pushError( &hsdkL->contextE, 
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nAssigned image references invalid memory", fNameL ) );
		return btk_STATUS_ERROR;
	}

	if( widthA == 0 || heightA == 0 )
	{
		bbs_Context_pushError( &hsdkL->contextE, 
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nAssigned image has size 0", fNameL ) );
		return btk_STATUS_ERROR;
	}
	
	if( pRectA == NULL )
	{
		bbs_Context_pushError( &hsdkL->contextE, 
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nAssigned ROI rectangle references invalid memory", fNameL ) );
		return btk_STATUS_ERROR;
	}

	if( pRectA->xMax <= pRectA->xMin || pRectA->yMax <= pRectA->yMin )
	{
		bbs_Context_pushError( &hsdkL->contextE, 
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nAssigned ROI rectangle is inverted (max<min) or zero", fNameL ) );
		return btk_STATUS_ERROR;
	}

	{
		struct bts_Int16Rect rectL;
		rectL = bts_Int16Rect_create( pRectA->xMin >> 16, 
									  pRectA->yMin >> 16, 
									  pRectA->xMax >> 16, 
									  pRectA->yMax >> 16 );

		/* rect must stay within image boundaries - adjust coordinates if necessary */
		rectL.x1E = rectL.x1E < 0         ? 0 : rectL.x1E;
		rectL.y1E = rectL.y1E < 0         ? 0 : rectL.y1E;
		rectL.x2E = rectL.x2E > ( int32 )widthA    ? widthA : rectL.x2E;
		rectL.y2E = rectL.y2E > ( int32 )heightA   ? heightA : rectL.y2E;

		bpi_DCR_assignGrayByteImageROI( &hsdkL->contextE, &hdcrA->dcrE, pDataA, widthA, heightA, &rectL );
	}
	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_ERROR;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

u32 btk_DCR_nodeCount( btk_HDCR hdcrA )
{
	if( hdcrA == NULL )					return 0;
	if( hdcrA->hidE != btk_HID_DCR )	return 0;
	return hdcrA->dcrE.sdkClusterE.clusterE.sizeE;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_DCR_getNode( btk_HDCR hdcrA, 
						    u32 indexA, 
							btk_Node* nodePtrA )
{
	const char* fNameL = "btk_DCR_getNode";

	btk_HSDK hsdkL = NULL;
	if( hdcrA == NULL )					return btk_STATUS_INVALID_HANDLE;
	if( hdcrA->hidE != btk_HID_DCR )	return btk_STATUS_INVALID_HANDLE;
	hsdkL = hdcrA->hsdkE;
	if( nodePtrA == NULL ) return btk_STATUS_INVALID_HANDLE;

	if( bbs_Context_error( &hsdkL->contextE ) ) return btk_STATUS_PREEXISTING_ERROR;

	if( indexA >= hdcrA->dcrE.sdkClusterE.clusterE.sizeE )
	{
		bbs_Context_pushError( &hsdkL->contextE, 
			                   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nIndex is out of range", fNameL ) );
		return btk_STATUS_ERROR;
	}

	nodePtrA->id = hdcrA->dcrE.sdkClusterE.idArrE.arrPtrE[ indexA ];
	nodePtrA->x  = ( ( s16p16 )hdcrA->dcrE.sdkClusterE.clusterE.vecArrE[ indexA ].xE ) << ( 16 - hdcrA->dcrE.sdkClusterE.clusterE.bbpE );
	nodePtrA->y  = ( ( s16p16 )hdcrA->dcrE.sdkClusterE.clusterE.vecArrE[ indexA ].yE ) << ( 16 - hdcrA->dcrE.sdkClusterE.clusterE.bbpE );
	if( hdcrA->dcrE.roiRectE.x1E > 0 ) nodePtrA->x += ( int32 )hdcrA->dcrE.roiRectE.x1E << 16;
	if( hdcrA->dcrE.roiRectE.y1E > 0 ) nodePtrA->y += ( int32 )hdcrA->dcrE.roiRectE.y1E << 16;
	nodePtrA->x += ( int32 )hdcrA->dcrE.offsE.xE << 16;
	nodePtrA->y += ( int32 )hdcrA->dcrE.offsE.yE << 16;

	nodePtrA->reserved = 0;

	return btk_STATUS_OK;
}

/* ------------------------------------------------------------------------- */

btk_Status btk_DCR_getRect( btk_HDCR hdcrA, 
							btk_Rect* pRectA )
{
	const char* fNameL = "btk_DCR_getRect";

	btk_HSDK hsdkL = NULL;
	if( hdcrA == NULL )					return btk_STATUS_INVALID_HANDLE;
	if( hdcrA->hidE != btk_HID_DCR )	return btk_STATUS_INVALID_HANDLE;
	hsdkL = hdcrA->hsdkE;
	if( pRectA == NULL ) return btk_STATUS_INVALID_HANDLE;

	/* find eye nodes */
	{
		const struct bbs_Int16Arr* pIdArrL = &hdcrA->dcrE.sdkClusterE.idArrE;
		int32 lIndexL = -1;
		int32 rIndexL = -1;
		uint32 iL;
		for( iL = 0; iL < pIdArrL->sizeE; iL++ )
		{
			if( pIdArrL->arrPtrE[ iL ] == 0 )
			{
				lIndexL = iL;
			}
			else if( pIdArrL->arrPtrE[ iL ] == 1 )
			{
				rIndexL = iL;
			}
		}

		if( lIndexL == -1 || rIndexL == -1 )
		{
			bbs_Context_pushError( &hsdkL->contextE, 
								   bbs_Error_create( bbs_ERR_ERROR, 0, NULL, "%s:\nFace rectangle is not available", fNameL ) );
			return btk_STATUS_ERROR;
		}

		{
			int32 bbpL = hdcrA->dcrE.sdkClusterE.clusterE.bbpE; 
			int32 lxL = ( hdcrA->dcrE.sdkClusterE.clusterE.vecArrE[ lIndexL ].xE + ( 1 << ( bbpL - 1 ) ) ) >> bbpL;
			int32 lyL = ( hdcrA->dcrE.sdkClusterE.clusterE.vecArrE[ lIndexL ].yE + ( 1 << ( bbpL - 1 ) ) ) >> bbpL;
			int32 rxL = ( hdcrA->dcrE.sdkClusterE.clusterE.vecArrE[ rIndexL ].xE + ( 1 << ( bbpL - 1 ) ) ) >> bbpL;
			int32 ryL = ( hdcrA->dcrE.sdkClusterE.clusterE.vecArrE[ rIndexL ].yE + ( 1 << ( bbpL - 1 ) ) ) >> bbpL;
			int32 doffL = ( rxL - lxL ) >> 1;

			pRectA->xMin = ( lxL - doffL ) << 16;
			pRectA->xMax = ( rxL + doffL ) << 16;
			pRectA->yMin = ( ( ( lyL + ryL + 1 ) >> 1 ) - doffL ) << 16;
			pRectA->yMax = ( pRectA->yMin + ( pRectA->xMax - pRectA->xMin ) );
			if( hdcrA->dcrE.roiRectE.x1E > 0 ) 
			{	
				pRectA->xMin += ( int32 )hdcrA->dcrE.roiRectE.x1E << 16;
				pRectA->xMax += ( int32 )hdcrA->dcrE.roiRectE.x1E << 16;
			}
			if( hdcrA->dcrE.roiRectE.y1E > 0 ) 
			{
				pRectA->yMin += ( int32 )hdcrA->dcrE.roiRectE.y1E << 16;
				pRectA->yMax += ( int32 )hdcrA->dcrE.roiRectE.y1E << 16;
			}

			pRectA->xMin += ( int32 )hdcrA->dcrE.offsE.xE << 16;
			pRectA->yMin += ( int32 )hdcrA->dcrE.offsE.yE << 16;
			pRectA->xMax += ( int32 )hdcrA->dcrE.offsE.xE << 16;
			pRectA->yMax += ( int32 )hdcrA->dcrE.offsE.yE << 16;

		}
	}

	return btk_STATUS_OK;
}


/* ------------------------------------------------------------------------- */

s8p24 btk_DCR_confidence( btk_HDCR hdcrA )
{
	btk_HSDK hsdkL = NULL;
	if( hdcrA == NULL )					return 0;
	if( hdcrA->hidE != btk_HID_DCR )	return 0;
	hsdkL = hdcrA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return 0;

	return hdcrA->dcrE.confidenceE;
}

/* ------------------------------------------------------------------------- */

u32 btk_DCR_approved( btk_HDCR hdcrA )
{
	btk_HSDK hsdkL = NULL;
	if( hdcrA == NULL )					return 0;
	if( hdcrA->hidE != btk_HID_DCR )	return 0;
	hsdkL = hdcrA->hsdkE;
	if( bbs_Context_error( &hsdkL->contextE ) ) return 0;

	return ( u32 )hdcrA->dcrE.approvedE;
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
