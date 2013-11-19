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

#ifndef bpi_FUNCTIONS_EM_H
#define bpi_FUNCTIONS_EM_H

/**
 * This files contains general purpose functions.
 */

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Basic.h"
#include "b_BasicEm/Context.h"
#include "b_BasicEm/Functions.h"

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */


/** methods of similarty normalization for identification and verification */
enum bpi_SimType
{
	bpi_RAW_SIM,       /* take raw similarity only */
	bpi_SUB_MEAN,      /* subtract average  */
	bpi_SUB_MAX_1,     /* subtract maximum (different id of each entry) */
	bpi_SUB_MAX_2,     /* subtract maximum (different id of best entry) */
	bpi_SUB_4_MAX_2,   /* subtract average maximum of best 4 entries (method 2) */
	bpi_SUB_8_MAX_2,   /* subtract average maximum of best 8 entries (method 2) */
	bpi_SUB_16_MAX_2,  /* subtract average maximum of best 16 entries (method 2) */
	bpi_SUB_32_MAX_2   /* subtract average maximum of best 32 entries (method 2) */
};

/* ---- constants ---------------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/** Normalizes similarities. 
 *  This function is used by identifier module
 */
void bpi_normalizeSimilarities( struct bbs_Context* cpA,
							    const int32* rawSimArrA,
							    const int32* rawIdArrA,
								uint32 rawSizeA,
								const int32* refSimArrA,
								const int32* refIdArrA,
								uint32 refSizeA,
								enum bpi_SimType simTypeA,
								int32* outSimArrA );

/** Returnes normalized single similarity. 
 *  This function is used by verifier module
 */
int32 bpi_normalizedSimilarity( struct bbs_Context* cpA,
							    int32 rawSimA,
							    int32 rawIdA,
								const int32* refSimArrA,
								const int32* refIdArrA,
								uint32 refSizeA,
								enum bpi_SimType simTypeA );



/** writes checksum adjustment value to meet chkSumA to memory
 *  the function assumes that memPtrA is memSizeA - 1 units 
 *  away from beginning of object-memory block 
 */
uint32 bpi_memWriteCsa16( uint16* memPtrA, uint32 memSizeA, uint16 chkSumA );

/** takes checksum adjustment value from memory stream */
uint32 bpi_memReadCsa16( const uint16* memPtrA );

/** tests check sum and produxes error condition if no match */
void bpi_testCheckSum( struct bbs_Context* cpA, uint16* memPtrA, uint16 chkSumA, const char* fNameA );


#endif /* bpi_FUNCTIONS_EM_H */

