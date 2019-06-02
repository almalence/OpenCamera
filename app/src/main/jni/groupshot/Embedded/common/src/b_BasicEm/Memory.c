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

#include "b_BasicEm/Memory.h"
#include "b_BasicEm/Functions.h"
/*
#include <string.h>
*/
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
/*
void* bbs_memcpy( void* dstA, const void* srcA, uint32 sizeA )
{
	if( sizeA & 1 )
	{
		bbs_ERROR0( "bbs_memcpy( .... ): sizeA must be even" );
		return NULL;
	}
	return bbs_memcpy16( dstA, srcA, sizeA >> 1 );
}
*/
/* ------------------------------------------------------------------------- */

void* bbs_memcpy16( void* dstA, const void* srcA, uint32 sizeA )
{
#ifdef HW_TMS320C5x
	if( ( ( int32 ) dstA >> 16 ) == ( ( ( int32 ) dstA + sizeA ) >> 16 ) &&
		( ( int32 ) srcA >> 16 ) == ( ( ( int32 ) srcA + sizeA ) >> 16 ) )
	{
		/* fast version, works only if pointers do not cross page boundary. */
		uint16* dstL = ( uint16* )dstA;
		const uint16* srcL = ( uint16* )srcA;
		uint16 iL;
		for( iL = sizeA; iL--; )
		{
			*dstL++ = *srcL++;
		}
	}
	else
	{
		/* safe version */
		uint32 iL;
		for( iL = 0; iL < sizeA; iL++ )
		{
			*( uint16* ) ( ( int32 ) dstA + iL ) = *( uint16* ) ( ( int32 ) srcA + iL );
		}
	}
	return dstA;
#else
	uint16* dstL = ( uint16* )dstA;
	const uint16* srcL = ( uint16* )srcA;

	for( ; sizeA >= 4; sizeA -= 4 )
	{
		dstL[ 0 ] = srcL[ 0 ];
		dstL[ 1 ] = srcL[ 1 ];
		dstL[ 2 ] = srcL[ 2 ];
		dstL[ 3 ] = srcL[ 3 ];
		dstL += 4;
		srcL += 4;
	}

	for( ; sizeA > 0; sizeA-- )
	{
		*dstL++ = *srcL++;
	}

	return dstA;
#endif
}

/* ------------------------------------------------------------------------- */

void* bbs_memcpy32( void* dstA, const void* srcA, uint32 sizeA )
{
#ifdef HW_TMS320C5x
	if( ( ( int32 ) dstA >> 16 ) == ( ( ( int32 ) dstA + ( sizeA << 1 ) ) >> 16 ) &&
		( ( int32 ) srcA >> 16 ) == ( ( ( int32 ) srcA + ( sizeA << 1 ) ) >> 16 ) )
	{
		/* fast version, works only if pointers do not cross page boundary. */
		uint32* dstL = ( uint32* )dstA;
		const uint32* srcL = ( uint32* )srcA;
		uint16 iL;
		for( iL = sizeA; iL--; )
		{
			*dstL++ = *srcL++;
		}
	}
	else
	{
		/* safe version */
		uint32 iL;
		sizeA <<= 1;
		for( iL = 0; iL < sizeA; iL += 2 )
		{
			*( uint32* ) ( ( int32 ) dstA + iL ) = *( uint32* ) ( ( int32 ) srcA + iL );
		}
	}
	return dstA;
/*
	uint16* dstL = ( uint16* )dstA;
	const uint16* srcL = ( uint16* )srcA;

	// copying with base object-size of 16bit 
	// is more efficient on 16 bit architecture
	sizeA <<= 1;

	for( ; sizeA >= 4; sizeA -= 4 )
	{
		dstL[ 0 ] = srcL[ 0 ];
		dstL[ 1 ] = srcL[ 1 ];
		dstL[ 2 ] = srcL[ 2 ];
		dstL[ 3 ] = srcL[ 3 ];
		dstL += 4;
		srcL += 4;
	}

	for( ; sizeA > 0; sizeA-- )
	{
		*dstL++ = *srcL++;
	}

	return dstA;
*/
#else	/* 32bit architectures */

	uint32* dstL = ( uint32* )dstA;
	const uint32* srcL = ( uint32* )srcA;

	for( ; sizeA >= 4; sizeA -= 4 )
	{
		dstL[ 0 ] = srcL[ 0 ];
		dstL[ 1 ] = srcL[ 1 ];
		dstL[ 2 ] = srcL[ 2 ];
		dstL[ 3 ] = srcL[ 3 ];
		dstL += 4;
		srcL += 4;
	}

	for( ; sizeA > 0; sizeA-- )
	{
		*dstL++ = *srcL++;
	}

	return dstA;

#endif
}

/* ------------------------------------------------------------------------- */

void* bbs_memset16( void* dstA, uint16 valA, uint32 sizeA )
{
	uint32 iL;
	uint16* dstL = ( uint16* )dstA;
	/* to be optimized */
	for( iL = 0; iL < sizeA; iL++ )
	{
		*dstL++ = valA;
	}
	return dstA;
}

/* ------------------------------------------------------------------------- */

void* bbs_memset32( void* dstA, uint32 valA, uint32 sizeA )
{
	uint32 iL;
	uint32* dstL = ( uint32* )dstA;
	/* to be optimized */
	for( iL = 0; iL < sizeA; iL++ )
	{
		*dstL++ = valA;
	}
	return dstA;
}

/* ------------------------------------------------------------------------- */

