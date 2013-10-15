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

#include "b_BasicEm/String.h"
/*
#include <stdlib.h>
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

char* bbs_strcpy( char* dstA, const char* srcA )
{
	const char* srcL = srcA;
	char* dstL = dstA;
	while( ( *dstL++ = *srcL++ ) != 0 );
	return dstA;
}

/* ------------------------------------------------------------------------- */

char* bbs_strncpy( char* dstA, const char* srcA, uint32 sizeA )
{
	uint32 iL;
	for( iL = 0; iL < sizeA; iL++ )
	{
		if( ( dstA[ iL ] = srcA[ iL ] ) == 0 ) break;
	}
	if( iL == sizeA && sizeA > 0 ) dstA[ iL - 1 ] = 0;	
	return dstA;
}

/* ------------------------------------------------------------------------- */

char* bbs_strcat( char* dstA, const char* srcA )
{
	const char* srcL = srcA;
	char* dstL = dstA;
	while( *dstL != 0 ) dstL++;
	while( ( *dstL++ = *srcL++ ) != 0 );
	return dstA;
}

/* ------------------------------------------------------------------------- */

char* bbs_strncat( char* dstA, const char* srcA, uint32 sizeA )
{
	uint32 iL;
	for( iL = 0; iL < sizeA; iL++ ) 
	{
		if( dstA[ iL ] == 0 ) break;
	}

	for( ; iL < sizeA; iL++ )       
	{
		if( ( dstA[ iL ] = srcA[ iL ] ) == 0 ) break;
	}

	if( iL == sizeA && sizeA > 0 ) dstA[ iL - 1 ] = 0;	

	return dstA;
}

/* ------------------------------------------------------------------------- */

uint32 bbs_strlen( const char* strA )
{
	uint32 iL = 0;
	while( strA[ iL++ ] != 0 );
	return iL - 1;
}

/* ------------------------------------------------------------------------- */

flag bbs_strequal( const char* str1A, const char* str2A )
{
	const char* str1L = str1A;
	const char* str2L = str2A;

	if( str1L == NULL && str2L == NULL ) return TRUE;
	if( str1L == NULL || str2L == NULL ) return FALSE;

	while( ( *str1L != 0 ) && ( *str2L != 0 ) )
	{
		if( *str1L != *str2L ) break;
		str1L++;
		str2L++;
	}

	return *str1L == *str2L;
}

/* ------------------------------------------------------------------------- */

flag bbs_strmatch( const char* str1A, const char* str2A )
{
	const char* str1L = str1A;
	const char* str2L = str2A;

	if( str1L == NULL || str2L == NULL ) return TRUE;

	while( ( *str1L != 0 ) && ( *str2L != 0 ) )
	{
		if( *str1L != *str2L ) break;
		str1L++;
		str2L++;
	}

	if( *str1L == 0 || *str2L == 0 ) return TRUE;

	return *str1L == *str2L;
}

/* ------------------------------------------------------------------------- */

uint32 bbs_snprintf( char* bufA, uint32 bufSizeA, const char* formatA, ... )
{
	uint32 sizeL;
	va_list argsL;
	va_start( argsL, formatA );
	sizeL = bbs_vsnprintf( bufA, bufSizeA, formatA, argsL );
	va_end( argsL );
	return sizeL;
}

/* ------------------------------------------------------------------------- */

/* converts number to string without 0 termination - returns number of characters written */
uint32 bbs_cString( int32 valA, char* dstA, uint32 bufSizeA )
{
	uint32 valL = ( valA < 0 ) ? -valA : valA;
	uint32 iL = 0;
	uint32 digitsL = 0;
	if( valA < 0 )
	{
		if( iL < bufSizeA ) dstA[ iL++ ] = '-';
	}

	/* count #digits */
	if( valL == 0 )
	{
		iL++;
	}
	else
	{
		while( valL > 0 )
		{
			iL++;
			valL /= 10;
		}
	}

	digitsL = ( iL > bufSizeA ) ? bufSizeA : iL;

	valL = ( valA < 0 ) ? -valA : valA;

	if( valL == 0 )
	{
		if( iL < bufSizeA ) dstA[ --iL ] = '0';
	}
	else
	{
		while( valL > 0 )
		{
			if( iL < bufSizeA ) dstA[ --iL ] = '0' + ( valL % 10 );
			valL /= 10;
		}
	}

	return digitsL;
}

/* ------------------------------------------------------------------------- */

uint32 bbs_vsnprintf( char* bufA, uint32 bufSizeA, const char* formatA, va_list argsA )
{
	const char* fpL = formatA;
	uint32 iL = 0;

	while( *fpL != 0 )
	{
		if( *fpL == '%' )
		{
			if( *( fpL + 1 ) == 'i' || *( fpL + 1 ) == 'd' )
			{
				int valL = va_arg( argsA, int );
				if( iL < bufSizeA )
				{
					iL += bbs_cString( valL, bufA + iL, bufSizeA - iL );
				}
				fpL += 2;
			}
			else if( *( fpL + 1 ) == 's' )
			{
				const char* stringL = va_arg( argsA, char* );
				if( iL < bufSizeA )
				{
					bufA[ iL ] = 0;
					bbs_strncat( bufA + iL, stringL, bufSizeA - iL );
					iL += bbs_strlen( stringL );
				}
				fpL += 2;
			}
			else if( *( fpL + 1 ) == '%' )
			{
				if( iL < bufSizeA ) bufA[ iL++ ] = '%';
				fpL++;
			}
			else
			{
				if( iL < bufSizeA ) bufA[ iL++ ] = *fpL;
				fpL++;
			}
		}
		else
		{
			if( iL < bufSizeA ) bufA[ iL++ ] = *fpL;
			fpL++;
		}
	}

	if( iL < bufSizeA )
	{
		bufA[ iL ] = 0;
	}
	else if( bufSizeA > 0 )
	{
		bufA[ bufSizeA - 1 ] = 0;
	}
			   
	return iL;
}

/* ------------------------------------------------------------------------- */

int32 bbs_atoi( const char* strA )
{
	int32 valL = 0;
	int16 signL = 1;
	uint16 iL = 0, lenL = bbs_strlen( strA );
	
	while( iL < lenL && strA[ iL ] == ' ' ) iL++;
	if( strA[ iL ] == '-' )
	{
		signL = -1;
		iL++;
	}
	while( iL < lenL && strA[ iL ] == ' ' ) iL++;
	while( iL < lenL && strA[ iL ] >= '0' && strA[ iL ] <= '9' )
	{
		valL = valL * 10 + ( strA[ iL ] - '0' );
		iL++;
	}	
	return valL * signL;
}

/* ------------------------------------------------------------------------- */

