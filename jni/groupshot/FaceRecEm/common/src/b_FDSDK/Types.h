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

#ifndef btk_TYPES_EM_H
#define btk_TYPES_EM_H

/**
 * This file contains general purpose types.
 */

/* ---- includes ----------------------------------------------------------- */

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/** elementary data types */

/** integer data formats */
typedef signed short s16;
typedef unsigned short u16;

#if defined HW_TMS320C6x

	typedef signed int    s32;
	typedef unsigned int  u32;

#elif defined HW_TMS320C5x

	typedef signed long   s32;
	typedef unsigned long u32;

#else

	typedef signed int    s32;
	typedef unsigned int  u32;

#endif

/** signed 16.16 fixed point format */
typedef s32 s16p16;

/** signed 8.24 fixed point format */
typedef s32 s8p24;

/** function return status */
typedef enum
{
	/** execution finished without error */
	btk_STATUS_OK,

	/** execution could not continue because the object handle was invalid */
	btk_STATUS_INVALID_HANDLE,

	/** execution could not continue because of a preexisting unhandled error condition */
	btk_STATUS_PREEXISTING_ERROR,

	/** execution caused a new error condition */
	btk_STATUS_ERROR

} btk_Status;


/** gallery type */
typedef enum
{
	/** album gallery */
	btk_GALLERY_ALBUM,

	/** reference gallery */
	btk_GALLERY_REFERENCE

} btk_GalleryType;

/** database arrangement type */
typedef enum
{
	/** database entries are arranged in one coherent memory block without spaces */
	btk_COHERENT,

	/** database entries are arbitrarily distributed in memory and are referenced through pointers */
	btk_DISTRIBUTED

} btk_DataArrangement;


/** error types */
typedef enum
{
	/** execution finished without error */
	btk_ERR_NO_ERROR,	  /* no error */
	btk_ERR_INTERNAL,	  /* internal error */
	btk_ERR_MEMORY,		  /* failure to allocate memory */
	btk_ERR_VERSION,	  /* version conflict (software version is older than parameter version) */
	btk_ERR_CORRUPT_DATA  /* corrup parameter data or corrupt internal structure */

} btk_Error;

/** the following definitions are used to specify dll handling */
#if ( defined WIN32 || defined _WIN32_WCE || defined __SYMBIAN32__ ) && !defined btk_NO_DLL
	#ifdef btk_EXPORTING
		#define btk_DECLSPEC    __declspec(dllexport)
	#else
		#define btk_DECLSPEC    __declspec(dllimport)
	#endif
#else
	#define btk_DECLSPEC
#endif

/* ---- constants ---------------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

#endif /* btk_TYPES_EM_H */
