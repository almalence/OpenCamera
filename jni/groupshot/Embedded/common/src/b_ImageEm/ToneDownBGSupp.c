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

#include "b_BasicEM/Math.h"
#include "b_BasicEM/Memory.h"
#include "b_BasicEM/Int16Arr.h"
#include "b_BasicEM/Int32Arr.h"

#include "b_ImageEM/ToneDownBGSupp.h"

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

void bim_ToneDownBGSupp_BGGreyLevelOutside( struct bim_UInt8Image* imgA,
											struct bts_Int16Rect* rectA,
											int16 rectExpansionA,
											uint32* meanBGGrayLevelA )
{
	/* image access */
	int16 iL, jL;
	uint8 *imgPtrL = 0;
	uint8 *imgPtrMaxL = 0;

	/* the sum is possibly a large number. e.g. for a 512x512 byte image, maximum brightness, sumL is 7x10E7 */
	uint32 sumL, ctrL;

	/* the rectangle vertices */
	int16 rectXMinL, rectXMaxL, rectYMinL, rectYMaxL;
	int16 rectIxXMinL, rectIxXMaxL, rectIxYMinL, rectIxYMaxL;

	/* expand the rectangle */

	/* expand rectangle. the result is called the ROI */
	rectXMinL = rectA->x1E + rectExpansionA;
	rectXMaxL = rectA->x2E - rectExpansionA;
	rectYMinL = rectA->y1E + rectExpansionA;
	rectYMaxL = rectA->y2E - rectExpansionA;

	rectIxXMinL = bbs_max( rectXMinL, ( int16 ) 0 );
	rectIxXMaxL = bbs_max( rectXMaxL, ( int16 ) 0 );
	rectIxXMaxL = bbs_min( rectXMaxL, ( int16 ) imgA->widthE );
	rectIxYMinL = bbs_max( rectYMinL, ( int16 ) 0 );
	rectIxYMaxL = bbs_min( rectYMaxL, ( int16 ) 0 );
	rectIxYMaxL = bbs_min( rectYMaxL, ( int16 ) imgA->heightE );

	/* avoid negative overlap */
	rectIxXMinL = bbs_min( rectIxXMinL, rectIxXMaxL );
	rectIxYMinL = bbs_min( rectIxYMinL, rectIxYMaxL );

/*	printf( "new xmin=%d, xmax=%d, ymin=%d,ymax=%d \n", rectIxXMinL, rectIxXMaxL, rectIxYMinL, rectIxYMaxL ); */

	/* part 1: sum up all the lines above the ROI */

	sumL = 0;
	ctrL = 0;

	imgPtrL = &(imgA->arrE.arrPtrE[ 0 ]);
	ctrL += rectIxYMinL * imgA->widthE;
	imgPtrMaxL = imgPtrL + rectIxYMinL * imgA->widthE;
	while ( imgPtrL < imgPtrMaxL )
	{
		sumL += *imgPtrL;
		imgPtrL++;
	}

	/* part 2: sum up all the lines below the ROI */

	ctrL += ( imgA->heightE - rectIxYMaxL ) * imgA->widthE;

	imgPtrL = &(imgA->arrE.arrPtrE[ rectIxYMaxL * imgA->widthE ]);
	imgPtrMaxL = &(imgA->arrE.arrPtrE[ imgA->heightE * imgA->widthE ]);
	while ( imgPtrL < imgPtrMaxL )
	{
		sumL += *imgPtrL;
		imgPtrL++;
	}

	/* part 3: sum over the two vertically adjacent blocks */

	for ( jL = rectIxYMinL; jL < rectIxYMaxL; jL++ )
	{
		imgPtrL = &(imgA->arrE.arrPtrE[ rectIxYMinL * imgA->widthE ]);
		ctrL += bbs_max( 0, rectIxXMinL );

		for ( iL = 0; iL < rectIxXMinL; iL++ )
		{
			sumL += imgPtrL[ iL ];
		}
		
		if( ( int32 )imgA->widthE > ( int32 )rectIxXMaxL )
		{
			ctrL += ( int32 )imgA->widthE - ( int32 )rectIxXMaxL;
		}	

		for ( iL = rectIxXMaxL; iL < ( int16 ) imgA->widthE; iL++ )
		{
			sumL += imgPtrL[ iL ];
		}
	}

	/* printf( "new sum = %d, new ctr = %d \n", sumL, ctrL ); */

	/* result is bpb=[16.16] */
	*meanBGGrayLevelA = ( sumL << 16 ) / ( uint32 ) ctrL;

	/* result is bpb=[16.16] */
	*meanBGGrayLevelA = sumL / ctrL;								/* integer division */
	sumL = sumL - *meanBGGrayLevelA * ctrL;							/* result always greater than or equal to zero */
	*meanBGGrayLevelA = *meanBGGrayLevelA << 16;					/* shift to left */
	*meanBGGrayLevelA = *meanBGGrayLevelA + ( sumL << 16 ) / ctrL;	/* add residue */

}

/* ------------------------------------------------------------------------- */

void bim_ToneDownBGSupp_BGGreyLevelContour( struct bim_UInt8Image* imgA,
											struct bts_Int16Rect* rectA,
											uint32* meanBGGrayLevelA )
{
	/* image access */
	int16 iL;
	uint8 *imgPtr0L = 0;
	uint8 *imgPtr1L = 0;

	/* the sum is possibly a large number. e.g. for a 512x512 byte image, maximum brightness, sumL is 7x10E7 */
	uint32 sumL, ctrL;

	/* the rectangle vertices */
	int16 rectXMinL, rectXMaxL, rectYMinL, rectYMaxL;
	int16 rectIxXMinL, rectIxXMaxL, rectIxYMinL, rectIxYMaxL;
	int16 rectMinWidthL = 10, rectMinHeightL = 10;
	int16 rectXMidPointL, rectYMidPointL;
	int16 shiftXRectL, shiftYRectL;

	/* cut off the rectangle at the image bounaries 
	 * when its size becomes too small
	 * the rectangle is shifted back inside the image */

	/* cut off at image boundaries */
	rectXMinL = rectA->x1E;
	rectXMaxL = rectA->x2E;
	rectYMinL = rectA->y1E;
	rectYMaxL = rectA->y2E;

	rectIxXMinL = bbs_max( rectXMinL, ( int16 ) 0 );
	rectIxXMaxL = bbs_max( rectXMaxL, ( int16 ) 0 );
	rectIxXMaxL = bbs_min( rectXMaxL, ( int16 ) imgA->widthE );
	rectIxYMinL = bbs_max( rectYMinL, ( int16 ) 0 );
	rectIxYMaxL = bbs_min( rectYMaxL, ( int16 ) 0 );
	rectIxYMaxL = bbs_min( rectYMaxL, ( int16 ) imgA->heightE );

	/* shift back into image */
	shiftXRectL = 0;
	shiftYRectL = 0;
	if ( rectIxXMaxL - rectIxXMinL < rectMinWidthL )
	{
		rectXMidPointL = ( rectIxXMaxL + rectIxXMinL ) >> 1;
		rectIxXMinL = rectXMidPointL - ( rectMinWidthL >> 1 );
		rectIxXMaxL = rectXMidPointL + ( rectMinWidthL >> 1 );

		if ( rectIxXMinL < 0 )
		{
			shiftXRectL = -rectIxXMinL;
		}
		if ( rectIxXMaxL > ( int16 ) imgA->widthE )
		{
			shiftXRectL = rectIxXMaxL - ( int16 ) imgA->widthE;
		}
	}
	if ( rectIxYMaxL - rectIxYMinL < rectMinHeightL )
	{
		rectYMidPointL = ( rectIxYMaxL + rectIxYMinL ) >> 1;
		rectIxYMinL = rectYMidPointL - ( rectMinWidthL >> 1 );
		rectIxYMaxL = rectYMidPointL + ( rectMinWidthL >> 1 );

		if ( rectIxYMinL < 0 )
		{
			shiftXRectL = -rectIxYMinL;
		}
		if ( rectIxYMaxL > ( int16 ) imgA->widthE )
		{
			shiftXRectL = rectIxYMaxL - ( int16 ) imgA->widthE;
		}
	}
	rectIxXMinL += shiftXRectL;
	rectIxXMaxL += shiftXRectL;
	rectIxYMinL += shiftYRectL;
	rectIxYMaxL += shiftYRectL;

	/* when the image is small, there is a possibility that the shifted rectangle lies outside of the image. 
	 * => lop off the rectangle at image boundaries once again */
	rectIxXMinL = bbs_max( rectXMinL, ( int16 ) 0 );
	rectIxXMaxL = bbs_min( rectXMaxL, ( int16 ) imgA->widthE );
	rectIxYMinL = bbs_max( rectYMinL, ( int16 ) 0 );
	rectIxYMaxL = bbs_min( rectYMaxL, ( int16 ) imgA->heightE );


	sumL = 0;
	ctrL = 0;
	ctrL += ( rectIxXMaxL - rectIxXMinL ) << 1;
	ctrL += ( rectIxYMaxL - rectIxYMinL - 2 ) << 1;

	/* loop over the contour */
	imgPtr0L = &(imgA->arrE.arrPtrE[ rectIxYMinL * imgA->widthE ]);
	imgPtr1L = &(imgA->arrE.arrPtrE[ ( rectIxYMaxL - 1 ) * imgA->widthE ]);
	for ( iL = rectIxXMinL; iL < rectIxXMaxL; iL++ )
	{
		sumL += imgPtr0L[ iL ];
		sumL += imgPtr1L[ iL ];
	}
	imgPtr0L = &(imgA->arrE.arrPtrE[ ( rectIxYMinL + 1 ) * imgA->widthE + rectIxXMinL ]);
	imgPtr1L = &(imgA->arrE.arrPtrE[ ( rectIxYMinL + 1 ) * imgA->widthE + rectIxXMaxL - 1 ]);
	for ( iL = rectIxYMinL + 1; iL < rectIxYMaxL - 1; iL++ )
	{
		sumL += *imgPtr0L;
		sumL += *imgPtr1L;
		imgPtr0L += imgA->widthE;
		imgPtr1L += imgA->widthE;
	}


	/* printf( "new sum = %d, new ctr = %d \n", sumL, ctrL ); */

	/* result is bpb=[16.16] */
	*meanBGGrayLevelA = ( sumL << 16 ) / ( uint32 ) ctrL;

	/* result is bpb=[16.16] */
	*meanBGGrayLevelA = sumL / ctrL;								/* integer division */
	sumL = sumL - *meanBGGrayLevelA * ctrL;							/* result always greater than or equal to zero */
	*meanBGGrayLevelA = *meanBGGrayLevelA << 16;					/* shift to left */
	*meanBGGrayLevelA = *meanBGGrayLevelA + ( sumL << 16 ) / ctrL;	/* add residue */
}

/* ------------------------------------------------------------------------- */

void bim_ToneDownBGSupp_suppress( struct bim_UInt8Image* imgA,
								  struct bts_Int16Rect* rectA,
								  int16 rectShrinkageA,
								  int32 toneDownFactorA,	/* ToDo: change to int16, bpb=[0.16] */
								  int32 cutOffAccuracyA )
{
	/* ((( variable declarations begin ))) */

	/* the rectangle vertices */
	int16 rectXMinL, rectXMaxL, rectYMinL, rectYMaxL;
	int16 rectIxXMinL, rectIxXMaxL, rectIxYMinL, rectIxYMaxL;
	int16 rectShrinkageL;

	/* the BG mean grey value */
	uint8  meanBGGreyBBPL;
	uint32 meanBGGreyLevelL;
	uint32 meanBGGreyLevelByteL;
	int32  meanBGGreyLevelLongL;

	/* maximum reach of the ROI */
	uint32 maxROIReachL;
	int16  rOIReachXMinL, rOIReachXMaxL, rOIReachYMinL, rOIReachYMaxL;
	int16  rOIReachIxXMinL, rOIReachIxXMaxL, rOIReachIxYMinL, rOIReachIxYMaxL;
	int16  ridgeIxLeftL, ridgeIxRightL;

	/* tone down table */
	struct bbs_Int32Arr toneDownFactorsL;	/* ToDo: change int32 bpb=[16.16] to uint bpb=[0.16] */
	int32 toneDownFactorPowA;
	int32* toneDownFactorsPtrL;
	int32 ctrL;

	/* image access */
	int16 iL, jL;
	uint8 *imgPtrL = 0;	/* welcome back to the stoneage */

	/* weighting formula */
	int32 weightL, invWeightL;		/* R=[0.0...1.0], bpb=[16.16] */
	int32 opSrcL, opBGL, sumL;		/* R=[0.0...255.0], bpb=[24,8] */

	/* ((( variable declarations end ))) */

	/* make sure that the width is smaller than the rectangle */
	rectShrinkageL = rectShrinkageA;
	rectShrinkageL = bbs_min( rectShrinkageL, ( rectA->x2E - rectA->x1E ) >> 1 );
	rectShrinkageL = bbs_min( rectShrinkageL, ( rectA->y2E - rectA->y1E ) >> 1 );

	/* shrink rectangle. the result is called the ROI */
	rectXMinL = rectA->x1E + rectShrinkageL;
	rectXMaxL = rectA->x2E - rectShrinkageL;
	rectYMinL = rectA->y1E + rectShrinkageL;
	rectYMaxL = rectA->y2E - rectShrinkageL;

	rectIxXMinL = bbs_max( rectXMinL, 0 );
	rectIxXMinL = bbs_min( rectIxXMinL, ( int16 ) imgA->widthE );
	rectIxXMaxL = bbs_min( rectXMaxL, ( int16 ) imgA->widthE );
	rectIxXMaxL = bbs_max( rectIxXMaxL, 0 );

	rectIxYMinL = bbs_max( rectYMinL, 0 );
	rectIxYMinL = bbs_min( rectIxYMinL, ( int16 ) imgA->heightE );
	rectIxYMaxL = bbs_min( rectYMaxL, ( int16 ) imgA->heightE );
	rectIxYMaxL = bbs_max( rectIxYMaxL, 0 );

	/* exit function at exceptional cases */
	if ( ( imgA->heightE == 0 ) || ( imgA->widthE == 0 ) ) return;
	if ( rectShrinkageL == 0 ) return;

	/* compute the mean gray level aloong the rectangle contour */
	bim_ToneDownBGSupp_BGGreyLevelContour( imgA, rectA, &meanBGGreyLevelL );

	/* printf( "new mean BG gray value = %f \n", ( float ) meanBGGreyLevelL / 65536.0f ); */

	/* R=[0.0...255.0], bpb=[24.8] */
	meanBGGreyBBPL = 16;
	meanBGGreyLevelL = ( 128 << meanBGGreyBBPL );
	meanBGGreyLevelByteL = meanBGGreyLevelL >> meanBGGreyBBPL;
	meanBGGreyLevelLongL = ( 128 << meanBGGreyBBPL );
	/* ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo ToDo */
	
	/* this function computes an image that moving away from the ROI gradually fades to
	 * the background grey level BG according to the formula
	 * tonedImg = w srcImg + (1-w) BG
	 * w depends on the distance to the ROI. 
	 * there is a distance maxROIReachL beyond which 
	 * the importance of the source image 
	 * relative to the BG in the equation 
	 * falls below a small threshold.
	 * in those regions the toned image is equal to 
	 * the mean BG grey value. i.e. w=0, tonedImg = BG */
	maxROIReachL = bbs_max( imgA->widthE, imgA->heightE );
	
	/* pre-compute an array of tone down factors. R=[0.0...1.0] => bpb=[0.16] (idealy, bpb=[16.16] due to missing uInt16Arr ) */
	bbs_Int32Arr_init( &toneDownFactorsL );
	bbs_Int32Arr_size( &toneDownFactorsL, maxROIReachL );
	toneDownFactorPowA = toneDownFactorA;
	toneDownFactorsPtrL = toneDownFactorsL.arrPtrE;
	for( ctrL = 0; ctrL < ( int32 ) maxROIReachL && toneDownFactorPowA > cutOffAccuracyA; ctrL++ )
	{
		toneDownFactorsPtrL[ ctrL ] = toneDownFactorPowA;
		toneDownFactorPowA = toneDownFactorPowA * ( toneDownFactorA >> 1 );
		toneDownFactorPowA = toneDownFactorPowA >> 15;

		/* make active to check the error that accumulates by recursively multiplying factors */
		/* printf( "pow = %d, tonedown dec = %d, tonedown float = %f \n", ctrL + 2, toneDownFactorPowA, toneDownFactorPowA / 65536.0f ); */
	}
	maxROIReachL = ctrL;
	/* printf( "maxROIReachL = %d, tonedown = %d \n", maxROIReachL, toneDownFactorPowA ); */

	/* move across the image one row at a time. 
	 * (1) fill the outside frame with BG grey level
	 * (2) blend in the original image moving towards the ROI
	 */

	rOIReachXMinL = rectXMinL - ( int32 ) maxROIReachL;
	rOIReachXMaxL = rectXMaxL + ( int32 ) maxROIReachL;
	rOIReachYMinL = rectYMinL - ( int32 ) maxROIReachL;
	rOIReachYMaxL = rectYMaxL + ( int32 ) maxROIReachL;

	rOIReachIxXMinL = bbs_max( rOIReachXMinL, ( int16 ) 0 );
	rOIReachIxXMinL = bbs_min( rOIReachIxXMinL, ( int16 ) imgA->widthE );
	rOIReachIxXMaxL = bbs_min( rOIReachXMaxL, ( int16 ) imgA->widthE );
	rOIReachIxXMaxL = bbs_max( rOIReachIxXMaxL, ( int16 ) 0 );

	rOIReachIxYMinL = bbs_max( rOIReachYMinL, ( int16 ) 0 );
	rOIReachIxYMinL = bbs_min( rOIReachIxYMinL, ( int16 ) imgA->heightE );
	rOIReachIxYMaxL = bbs_min( rOIReachYMaxL, ( int16 ) imgA->heightE );
	rOIReachIxYMaxL = bbs_max( rOIReachIxYMaxL, ( int16 ) 0 );

	/* (1) far from the ROI the image is filled with the BG grey value */

	imgPtrL = 0;
	for ( jL = 0; jL < rOIReachYMinL; jL++ )
	{
		imgPtrL = &( imgA->arrE.arrPtrE[ jL * imgA->widthE ] );
		for ( iL = 0; iL <= ( int16 ) imgA->widthE; iL++ )
		{
			imgPtrL[ iL ] = meanBGGreyLevelByteL;
		}
	}
	for ( jL = rOIReachYMaxL; jL < ( int16 ) imgA->heightE; jL++ )
	{
		imgPtrL = &( imgA->arrE.arrPtrE[ jL * imgA->widthE ] );
		for ( iL = 0; iL <= ( int16 ) imgA->widthE; iL++ )
		{
			imgPtrL[ iL ] = meanBGGreyLevelByteL;
		}
	}
	for ( jL = rOIReachIxYMinL; jL < rOIReachIxYMaxL; jL++ )
	{
		imgPtrL = &( imgA->arrE.arrPtrE[ jL * imgA->widthE ] );
		for ( iL = 0; iL < rOIReachXMinL; iL++ )
		{
			imgPtrL[ iL ] = meanBGGreyLevelByteL;
		}
		for ( iL = rOIReachXMaxL; iL < ( int16 ) imgA->widthE; iL++ )
		{
			imgPtrL[ iL ] = meanBGGreyLevelByteL;
		}
	}

	/* (2) blend from ROI to outside regions */

	for ( jL = rOIReachIxYMinL; jL < rectIxYMinL; jL++ )
	{
		/* the factor for one row is a constant */
		weightL = ( int32 ) toneDownFactorsPtrL[ maxROIReachL - 1 - ( jL - rOIReachYMinL ) ];
		invWeightL = 0x00010000 - weightL;
		opBGL = ( meanBGGreyLevelLongL >> 9 ) * invWeightL;				/* result is bpb=[8,24] */
		opBGL = opBGL >> 7;
		imgPtrL = &( imgA->arrE.arrPtrE[ jL * imgA->widthE ] );

		/* compute the ridge position */
		ridgeIxLeftL = bbs_max( 0, rOIReachXMinL + jL - rOIReachYMinL );
		ridgeIxRightL = bbs_min( ( int16 ) imgA->widthE - 1, rOIReachXMaxL - 1 - ( jL - rOIReachYMinL ) );

		/* loop over all elements from left ridge through right ridge */
		for ( iL = ridgeIxLeftL; iL <= ridgeIxRightL; iL++ )
		{
			opSrcL = imgPtrL[ iL ];							/* leave at byte */
			opSrcL = opSrcL * weightL;						/* result is bpb=[16,16] */
			sumL = opSrcL + opBGL;							/* OF impossible */
			imgPtrL[ iL ] = sumL >> 16;						/* round to byte */
		}
	}
	for ( jL = rOIReachIxYMaxL - 1; jL >= rectIxYMaxL; jL-- )
	{
		/* the factor for one row is a constant */
		weightL = ( int32 ) toneDownFactorsPtrL[ maxROIReachL - 1 - ( rOIReachYMaxL - 1 - jL ) ];
		invWeightL = 0x00010000 - weightL;
		opBGL = ( meanBGGreyLevelLongL >> 9 ) * invWeightL;				/* result is bpb=[8,24] */
		opBGL = opBGL >> 7;
		imgPtrL = &( imgA->arrE.arrPtrE[ jL * imgA->widthE ] );

		/* compute the ridge position */
		ridgeIxLeftL = bbs_max( 0, rOIReachXMinL + ( rOIReachYMaxL - 1 - jL ) );
		ridgeIxRightL = bbs_min( ( int16 ) imgA->widthE - 1, rOIReachXMaxL - 1 - ( rOIReachYMaxL - 1 - jL ) );

		/* loop over all elements from left ridge through right ridge */
		for ( iL = ridgeIxLeftL; iL <= ridgeIxRightL; iL++ )
		{
			opSrcL = imgPtrL[ iL ];							/* leave at byte */
			opSrcL = opSrcL * weightL;						/* result is bpb=[16,16] */
			sumL = opSrcL + opBGL;							/* OF impossible */
			imgPtrL[ iL ] = sumL >> 16;						/* round to byte */
		}
	}
	for ( jL = rOIReachIxYMinL; jL < rOIReachIxYMaxL; jL++ )
	{
		imgPtrL = &( imgA->arrE.arrPtrE[ jL * imgA->widthE ] );

		ridgeIxLeftL = bbs_min( rOIReachXMinL + ( jL - rOIReachYMinL ) - 1, rectXMinL - 1 );
		ridgeIxLeftL = bbs_min( ridgeIxLeftL, rOIReachXMinL + ( rOIReachYMaxL - 1 - jL ) - 1 );
		for ( iL = rOIReachIxXMinL; iL <= ridgeIxLeftL; iL++ )
		{
			weightL = ( int32 ) toneDownFactorsPtrL[ maxROIReachL - 1 - ( iL - rOIReachXMinL ) ];
			invWeightL = 0x00010000 - weightL;
			opBGL = ( meanBGGreyLevelLongL >> 9 ) * invWeightL;				/* result is bpb=[16,16] */
			opBGL = opBGL >> 7;

			opSrcL = imgPtrL[ iL ];											/* leave at byte */
			opSrcL = opSrcL * weightL;										/* result is bpb=[16,16] */
			sumL = opSrcL + opBGL;											/* OF impossible */
			imgPtrL[ iL ] = sumL >> 16;										/* round to byte */
		}

		ridgeIxRightL = bbs_max( rOIReachXMaxL - 1 - ( jL - rOIReachYMinL ) + 1 , rectXMaxL );
		ridgeIxRightL = bbs_max( ridgeIxRightL, rOIReachXMaxL - 1 - ( rOIReachYMaxL - 1 - jL ) + 1 );
		for ( iL = ridgeIxRightL; iL < rOIReachIxXMaxL; iL++ )
		{
			weightL = ( int32 ) toneDownFactorsPtrL[ iL - rectXMaxL ];
			invWeightL = 0x00010000 - weightL;
			opBGL = ( meanBGGreyLevelLongL >> 9 ) * invWeightL;				/* result is bpb=[16,16] */
			opBGL = opBGL >> 7;

			opSrcL = imgPtrL[ iL ];											/* leave at byte */
			opSrcL = opSrcL * weightL;										/* result is bpb=[16,16] */
			sumL = opSrcL + opBGL;											/* OF impossible */
			imgPtrL[ iL ] = sumL >> 16;										/* round to byte */	
		}
	}
}

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
