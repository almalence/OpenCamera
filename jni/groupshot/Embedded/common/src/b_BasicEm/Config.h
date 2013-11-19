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

#ifndef bbs_CONFIG_EM_H
#define bbs_CONFIG_EM_H

/**
 * This file contains hardware and OS specific definitions
 */

/* ---- release specific defines ------------------------------------------- */

/* ---- hardware specific defines ------------------------------------------ */

#if defined( HW_i586 ) || defined( HW_i686 )
	#ifdef HW_SSE2
		#define bbs_MEMORY_ALIGNMENT	16 /* SSE2: align data to 128 bits */
	#else
		#define bbs_MEMORY_ALIGNMENT	8  /* MMX: align data to 64 bits */
	#endif
#elif defined( HW_EE )
	#define bbs_MEMORY_ALIGNMENT	16 /* align EE-MMI data to 128 bits */
#else
	#define bbs_MEMORY_ALIGNMENT	1
#endif

#ifdef HW_TMS470R2X
	#pragma message("Warning: deprecated define HW_TMS470R2X, use HW_ARMv4 instead")
	#define HW_ARMv4
#endif

#ifdef HW_ARM9E
	#pragma message("Warning: deprecated define HW_ARM9E, use HW_ARMv5TE instead")
	#define HW_ARMv5TE
#endif

/* ---- operating system specific defines ---------------------------------- */

#if defined( WIN32 ) || defined( _WIN32_WCE )
	/* disable warning "unreferenced formal parameter": */
	#pragma warning( disable : 4100 )

	/* disable warning for constant expression in condition: */
	#pragma warning( disable : 4127 )

	/* disable warning for short += short: */
	#pragma warning( disable : 4244 )

	/* disable warning 'unreachable code' in release build: */
	/* this warning occurs due to a wrong code evaluation of the compiler */
	#pragma warning( disable : 4702 )

	/* disable warning for not expanded inline functions in release build: */
	#pragma warning( disable : 4710 )

	/* disable warning for automatic expanded inline functions in release build: */
	#pragma warning( disable : 4711 )

	/* disable warning "unreferenced inline function has been removed": */
	#pragma warning( disable : 4514 )

#endif

/* -------------------------------------------------------------------------- */

#endif /* bbs_CONFIG_EM_H */

