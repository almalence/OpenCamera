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

#ifndef bim_TONE_DOWN_BG_SUPP_EM_H
#define bim_TONE_DOWN_BG_SUPP_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"
#include "b_TensorEm/Int16Rect.h"
#include "b_ImageEm/UInt8Image.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** computes the mean BG gray level outside of the rectangle */
void bim_ToneDownBGSupp_BGGreyLevelOutside( struct bim_UInt8Image* imgA,
											struct bts_Int16Rect* rectA,
											int16 rectExpansionA,			/* this is a remnant of the original c++ class */
											uint32* meanBGGrayLevelA );		/* was mistakenly converted to c */

/** computes the mean BG gray level right on the rectangle contours */
void bim_ToneDownBGSupp_BGGreyLevelContour( struct bim_UInt8Image* imgA,
											struct bts_Int16Rect* rectA,
											uint32* meanBGGrayLevelA );

/** attenuates the image away from the rectangle boundary */ 
void bim_ToneDownBGSupp_suppress( struct bim_UInt8Image* imgA,
								  struct bts_Int16Rect* rectA,
								  int16 rectShrinkageA,
								  int32 toneDownFactorA,		/* bpb = [16.16] */
								  int32 cutOffAccuracyA );		/* bpb = [16.16], put in 0 for highest accuracy */

#endif /* bim_TONE_DOWN_BG_SUPP_EM_H */

