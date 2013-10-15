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

#ifndef bbs_BASIC_EM_H
#define bbs_BASIC_EM_H

/* ---- includes ----------------------------------------------------------- */

/** This header file is not part of the repository.
 *  If you get an error message at this point, copy 
 *  b_BasicEm/LibConfigDefault.h into
 *  "../conf/b_BasicEm/LibConfig.h"
 */
#include "../conf/b_BasicEm/LibConfig.h"

#include "b_BasicEm/Config.h"

/* ---- defines ------------------------------------------------------------ */

#if defined( WIN32 )
	/* disable warning for short += short: */
	#pragma warning( disable : 4244 )
#endif

#if defined( bbs_NO_MESSAGE_HANDLING ) 
#error bbs_NO_MESSAGE_HANDLING is obsolete, please use bbs_COMPACT_MESSAGE_HANDLING instead.
#endif
#if defined( bbs_ENABLE_MESSAGE_FPTRG )
#error bbs_ENABLE_MESSAGE_FPTRG is obsolete, please use error handler in context object instead.
#endif

#if defined( bbs_NO_MESSAGE_HANDLING ) && defined( bbs_ENABLE_MESSAGE_FPTRG )
#error LibConfig.h: bbs_NO_MESSAGE_HANDLING and bbs_ENABLE_MESSAGE_FPTRG are mutually exclusive
#endif



/* ---- typedefs ----------------------------------------------------------- */

typedef signed char		int8;
typedef signed short	int16;
typedef unsigned char	uint8;
typedef unsigned short	uint16;

#if defined HW_TMS320C6x
	typedef signed int		int32;
	typedef unsigned int	uint32;
	typedef uint32			count_t;
#elif defined HW_TMS320C5x
	typedef signed long		int32; 
	typedef unsigned long	uint32;
	typedef uint16			count_t;
#else
	typedef signed int		int32;
	typedef unsigned int	uint32;
	typedef uint32			count_t;
#endif


typedef uint32 flag; /* boolean type */

/* 
	Please modify the 64 bit types declarations below for specific platforms/compilers
	where necessary; 
	bbs_TYPES_64_AVAILABLE should be checked in code sections that make use of 64 bit data types.
*/
#ifdef bbs_TYPES_64_AVAILABLE

#ifdef WIN64
	typedef __int64				int64;
	typedef unsigned __int64	uint64;
#else
	typedef long long			int64;
	typedef unsigned long long	uint64;
#endif

#endif /* bbs_TYPES_64_AVAILABLE */

/** floating point type */
struct flt16
{
	int16 valE;
	int16 bbpE;
};

#ifndef TRUE
	#define TRUE 1
	#define FALSE 0
#endif

#ifndef NULL
	#define NULL 0L
#endif

#define bbs_MAX_STRING_LENGTH 1024

/* ---- macros ------------------------------------------------------------- */

/** device independent macro definitions for sizeof:
  * bbs_SIZEOF8:  size in bytes
  *	bbs_SIZEOF16: size in 16-bit words
  *	bbs_SIZEOF32: size in 32-bit words
  */				  
#if defined( HW_TMS320C5x )
	#define bbs_SIZEOF8( typeA )  ( sizeof( typeA ) << 1 )
	#define bbs_SIZEOF16( typeA ) ( sizeof( typeA ) )
	#define bbs_SIZEOF32( typeA ) ( sizeof( typeA ) >> 1 )
#else
	#define bbs_SIZEOF8( typeA )  ( sizeof( typeA ) )
	#define bbs_SIZEOF16( typeA ) ( sizeof( typeA ) >> 1 )
	#define bbs_SIZEOF32( typeA ) ( sizeof( typeA ) >> 2 )
#endif

/** messages */
#if defined( HW_TMS320C5x ) || defined( bbs_COMPACT_MESSAGE_HANDLING ) 

	#define bbs_DEF_fNameL( fNameA )

	#define bbs_ERROR0( formatA )										bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, NULL ) )
	#define bbs_ERROR1( formatA, arg1A )								bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, NULL ) )
	#define bbs_ERROR2( formatA, arg1A, arg2A )							bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, NULL ) )
	#define bbs_ERROR3( formatA, arg1A, arg2A, arg3A )					bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, NULL ) )
	#define bbs_ERROR4( formatA, arg1A, arg2A, arg3A, arg4A )			bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, NULL ) )
	#define bbs_ERROR5( formatA, arg1A, arg2A, arg3A, arg4A, arg5A )	bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, NULL ) )

	#define bbs_ERR0( errorA, formatA )									bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, NULL ) )
	#define bbs_ERR1( errorA, formatA, arg1A )							bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, NULL ) )
	#define bbs_ERR2( errorA, formatA, arg1A, arg2A )					bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, NULL ) )
	#define bbs_ERR3( errorA, formatA, arg1A, arg2A, arg3A )			bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, NULL ) )
	#define bbs_ERR4( errorA, formatA, arg1A, arg2A, arg3A, arg4A )		bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, NULL ) )
	#define bbs_ERR5( errorA, formatA, arg1A, arg2A, arg3A, arg4A, arg5A )	bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, NULL ) )

#else

	#define bbs_DEF_fNameL( fNameA )									const char* fNameL = fNameA;

	#define bbs_ERROR0( formatA )										bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, formatA ) )
	#define bbs_ERROR1( formatA, arg1A )								bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, formatA, arg1A ) )
	#define bbs_ERROR2( formatA, arg1A, arg2A )							bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, formatA, arg1A, arg2A ) )
	#define bbs_ERROR3( formatA, arg1A, arg2A, arg3A )					bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, formatA, arg1A, arg2A, arg3A ) )
	#define bbs_ERROR4( formatA, arg1A, arg2A, arg3A, arg4A )			bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, formatA, arg1A, arg2A, arg3A, arg4A ) )
    #define bbs_ERROR5( formatA, arg1A, arg2A, arg3A, arg4A, arg5A )	bbs_Context_pushError( cpA, bbs_Error_create( bbs_ERR_ERROR, __LINE__, __FILE__, formatA, arg1A, arg2A, arg3A, arg4A, arg5A ) )	

	#define bbs_ERR0( errorA, formatA )										bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, formatA ) )
	#define bbs_ERR1( errorA, formatA, arg1A )								bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, formatA, arg1A ) )
	#define bbs_ERR2( errorA, formatA, arg1A, arg2A )						bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, formatA, arg1A, arg2A ) )
	#define bbs_ERR3( errorA, formatA, arg1A, arg2A, arg3A )				bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, formatA, arg1A, arg2A, arg3A ) )
	#define bbs_ERR4( errorA, formatA, arg1A, arg2A, arg3A, arg4A )			bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, formatA, arg1A, arg2A, arg3A, arg4A ) )
    #define bbs_ERR5( errorA, formatA, arg1A, arg2A, arg3A, arg4A, arg5A )	bbs_Context_pushError( cpA, bbs_Error_create( errorA, __LINE__, __FILE__, formatA, arg1A, arg2A, arg3A, arg4A, arg5A ) )	

#endif

/* ---- constants ---------------------------------------------------------- */

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

#endif /* bbs_BASIC_EM_H */

