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

#ifndef bts_INT16MAT2D_EM_H
#define bts_INT16MAT2D_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"
#include "b_BasicEm/Phase.h"
#include "b_TensorEm/Int16Vec2D.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- object definition -------------------------------------------------- */

/** 2d matrix */
struct bts_Int16Mat2D 
{

	/* ---- private data --------------------------------------------------- */

	/* ---- public data ---------------------------------------------------- */

	/** xx component */
	int16 xxE;

	/** xy component */
	int16 xyE;

	/** yx component */
	int16 yxE;

	/** yy component */
	int16 yyE;

	/** fixed point position */
	int16 bbpE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/* ---- \ghd{ operators } -------------------------------------------------- */

/* ---- \ghd{ query functions } -------------------------------------------- */

/* ---- \ghd{ modify functions } ------------------------------------------- */

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** creates identity matrix */
struct bts_Int16Mat2D bts_Int16Mat2D_createIdentity( void );

/** creates rotation matrix */
struct bts_Int16Mat2D bts_Int16Mat2D_createRotation( phase16 angleA );

/** creates rigid matrix (scale & rotation) */
struct bts_Int16Mat2D bts_Int16Mat2D_createRigid( phase16 angleA, struct flt16 scaleA );

/** scales matrix by a factor */
void bts_Int16Mat2D_scale( struct bts_Int16Mat2D* ptrA, struct flt16 scaleA );

/** multiplies matrix with vecA; returns resulting vector */
struct bts_Int16Vec2D bts_Int16Mat2D_map( const struct bts_Int16Mat2D* matPtrA, 
								          const struct bts_Int16Vec2D* vecPtrA );

/** multiplies matrix with matA; returns resulting matrix */
struct bts_Int16Mat2D bts_Int16Mat2D_mul( const struct bts_Int16Mat2D* mat1PtrA, 
								          const struct bts_Int16Mat2D* mat2PtrA );

#endif /* bts_INT16MAT2D_EM_H */

