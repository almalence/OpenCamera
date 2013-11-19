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

#include "b_TensorEm/Functions.h"
#include "b_BasicEm/Math.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ------------------------------------------------------------------------- */

/* ========================================================================= */
/*                                                                           */
/* ---- \ghd{ external functions } ----------------------------------------- */
/*                                                                           */
/* ========================================================================= */

/* ------------------------------------------------------------------------- */

uint32 bts_absIntLog2( int32 vA )
{
	return bbs_intLog2( bbs_abs( vA ) );
}

/* ------------------------------------------------------------------------- */

uint32 bts_maxAbsIntLog2Of2( int32 v1A, int32 v2A )
{
	uint32 maxL = bbs_max( ( uint32 )bbs_abs( v1A ), ( uint32 )bbs_abs( v2A ) );
	return bbs_intLog2( maxL );
}

/* ------------------------------------------------------------------------- */

uint32 bts_maxAbsIntLog2Of3( int32 v1A, int32 v2A, int32 v3A )
{
	uint32 maxL = bbs_abs( v1A );
	maxL = bbs_max( maxL, ( uint32 )bbs_abs( v2A ) );
	maxL = bbs_max( maxL, ( uint32 )bbs_abs( v3A ) );
	return bbs_intLog2( maxL );
}

/* ------------------------------------------------------------------------- */

uint32 bts_maxAbsIntLog2Of4( int32 v1A, int32 v2A, int32 v3A, int32 v4A )
{
	uint32 maxL = bbs_abs( v1A );
	maxL = bbs_max( maxL, ( uint32 )bbs_abs( v2A ) );
	maxL = bbs_max( maxL, ( uint32 )bbs_abs( v3A ) );
	maxL = bbs_max( maxL, ( uint32 )bbs_abs( v4A ) );
	return bbs_intLog2( maxL );
}

/* ------------------------------------------------------------------------- */
