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

#ifndef bts_FUNCTIONS_EM_H
#define bts_FUNCTIONS_EM_H

/**
 * This file contains general purpose functions.
 */

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Functions.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

enum bts_AltType
{
	bts_ALT_IDENTITY = 1,   /* identity transformation */
	bts_ALT_TRANS,          /* restricted to translation only */
	bts_ALT_TRANS_SCALE,    /* restricted to translation and scale only */
	bts_ALT_RIGID,		    /* restricted to rigid transformation (translation + scale + rotation) */
	bts_ALT_LINEAR,			/* allows all degrees of freedom for affine linear transformation  */
	bts_ALT_TRANS_SCALE_XYZ	/* restricted to translation and scaling in x,y,z directions */
};

enum bts_RBFType
{
	bts_RBF_IDENTITY = 1,	/* no rbf deformation	*/
	bts_RBF_LINEAR			/* linear, i.e. ||r||	*/
};

/* ---- constants ---------------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/** compute the integer logarithm of the absolute value */
uint32 bts_absIntLog2( int32 vA );

/** compute the integer logarithm of the maximal absolute value of all arguments */
uint32 bts_maxAbsIntLog2Of2( int32 v1A, int32 v2A );

/** compute the integer logarithm of the maximal absolute value of all arguments */
uint32 bts_maxAbsIntLog2Of3( int32 v1A, int32 v2A, int32 v3A );

/** compute the integer logarithm of the maximal absolute value of all arguments */
uint32 bts_maxAbsIntLog2Of4( int32 v1A, int32 v2A, int32 v3A, int32 v4A );

#endif /* bts_FUNCTIONS_EM_H */

