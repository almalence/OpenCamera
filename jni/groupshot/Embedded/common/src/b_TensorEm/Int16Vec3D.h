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

#ifndef bts_INT16VEC3D_EM_H
#define bts_INT16VEC3D_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_BasicEm/Basic.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** 3d vector */
struct bts_Int16Vec3D 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** x component */
	int16 xE;

	/** y component */
	int16 yE;

	/** z component */
	int16 zE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bts_Int16Vec3D_memSize( struct bbs_Context* cpA,
							   const struct bts_Int16Vec3D* ptrA );

/** writes object to memory; returns number of bytes written */
uint32 bts_Int16Vec3D_memWrite( struct bbs_Context* cpA,
							    const struct bts_Int16Vec3D* ptrA, 
								uint16* memPtrA );

/** reads object from memory; returns number of bytes read */
uint32 bts_Int16Vec3D_memRead( struct bbs_Context* cpA,
							   struct bts_Int16Vec3D* ptrA, 
							   const uint16* memPtrA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** returns square norm of vector */
uint32 bts_Int16Vec3D_norm2( const struct bts_Int16Vec3D* ptrA );

/** returns norm of vector */
uint16 bts_Int16Vec3D_norm( const struct bts_Int16Vec3D* ptrA );

/** normalizes vector; bbpA defines number of bits behind the point */
void bts_Int16Vec3D_normalize( struct bts_Int16Vec3D* ptrA, int32 bbpA );

/** returns normalized vector; bbpA defines number of bits behind the point */
struct bts_Int16Vec3D bts_Int16Vec3D_normalized( const struct bts_Int16Vec3D* ptrA, int32 bbpA );

#endif /* bts_INT16VEC3D_EM_H */

