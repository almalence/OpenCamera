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

#ifndef bbs_STRING_EM_H
#define bbs_STRING_EM_H

/**
 * This file contains string related functions.
 */

/* ---- includes ----------------------------------------------------------- */

#include <stdarg.h>

#include "b_BasicEm/Basic.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/** copies a string from srcA to dstA; returns dstA */
char* bbs_strcpy( char* dstA, const char* srcA );

/** copies sizeA caracters from from srcA to dstA; returns dstA */
char* bbs_strncpy( char* dstA, const char* srcA, uint32 sizeA );

/** adds a string srcA to string dstA; returns dstA */
char* bbs_strcat( char* dstA, const char* srcA );

/** adds sizeA characters from srcA to string dstA; returns dstA */
char* bbs_strncat( char* dstA, const char* srcA, uint32 sizeA );

/** returns number of characters in string excluding terminating 0 */
uint32 bbs_strlen( const char* strA );

/** returns true if both strings are equal */ 
flag bbs_strequal( const char* str1A, const char* str2A );

/** returns true if all characters of the smaller of both string are equal with the other string */ 
flag bbs_strmatch( const char* str1A, const char* str2A );

/** writes a formated string to buffer with size limitation; returns number of characters written 
 *  Not all possible format types of stdlib function snprintf are handled in this function
 */
uint32 bbs_snprintf( char* dstA, uint32 bufSizeA, const char* formatA, ... );

/** writes a formated string to buffer with size limitation; returns number of characters written
 *  Not all possible format types of stdlib function vsnprintf are handled in this function
 */
uint32 bbs_vsnprintf( char* dstA, uint32 bufSizeA, const char* formatA, va_list argsA );

/** converts a string to an integer */
int32 bbs_atoi( const char* strA );

#endif /* bbs_STRING_EM_H */

