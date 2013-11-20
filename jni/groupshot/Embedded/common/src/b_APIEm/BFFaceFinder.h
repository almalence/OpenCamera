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

#ifndef bpi_BF_FACE_FINDER_EM_H
#define bpi_BF_FACE_FINDER_EM_H

/* ---- includes ----------------------------------------------------------- */

#include "b_BasicEm/Context.h"
#include "b_APIEm/FaceFinder.h"
#include "b_BitFeatureEm/ScanDetector.h"

/* ---- related objects  --------------------------------------------------- */

struct bpi_DCR;

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* data format version number */
#define bpi_BF_FACE_FINDER_VERSION 100

/* ---- object definition -------------------------------------------------- */

/** Face Finder using ractangle features */
struct bpi_BFFaceFinder
{
	/* ---- private data --------------------------------------------------- */

	/** base object */
	struct bpi_FaceFinder baseE;

	/* number of detected faces in last call of multiProcess in face data buffer */
	uint32 detectedFacesE;

	/* number of available faces in last call of multiProcess in face data buffer */
	uint32 availableFacesE;

	/* pointer to face data buffer */
	int32* faceDataBufferE;

	/* ---- public data ---------------------------------------------------- */

	/* detector */
	struct bbf_ScanDetector detectorE;
};

/* ---- associated objects ------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/* ---- \ghd{ constructor/destructor } ------------------------------------- */

/** initializes module */
void bpi_BFFaceFinder_init( struct bbs_Context* cpA,
						    struct bpi_BFFaceFinder* ptrA );

/** destroys module */
void bpi_BFFaceFinder_exit( struct bbs_Context* cpA,
						    struct bpi_BFFaceFinder* ptrA );

/* ---- \ghd{ operators } -------------------------------------------------- */

/** copies module */
void bpi_BFFaceFinder_copy( struct bbs_Context* cpA,
						    struct bpi_BFFaceFinder* ptrA, 
							const struct bpi_BFFaceFinder* srcPtrA );

/** determines equality of parameters */
flag bpi_BFFaceFinder_equal( struct bbs_Context* cpA,
							 const struct bpi_BFFaceFinder* ptrA, 
							 const struct bpi_BFFaceFinder* srcPtrA );

/* ---- \ghd{ query functions } -------------------------------------------- */

/** minimum eye distance (pixel) */
uint32 bpi_BFFaceFinder_getMinEyeDistance( const struct bpi_BFFaceFinder* ptrA );

/** maximum eye distance (pixel) */
uint32 bpi_BFFaceFinder_getMaxEyeDistance( const struct bpi_BFFaceFinder* ptrA );

/* ---- \ghd{ modify functions } ------------------------------------------- */

/** minimum eye distance (pixel) */
void bpi_BFFaceFinder_setMinEyeDistance( struct bbs_Context* cpA,
										 struct bpi_BFFaceFinder* ptrA, 
										 uint32 distA );

/** maximum eye distance (pixel) */
void bpi_BFFaceFinder_setMaxEyeDistance( struct bbs_Context* cpA,
										 struct bpi_BFFaceFinder* ptrA, 
										 uint32 distA );

/* ---- \ghd{ memory I/O } ------------------------------------------------- */

/** size object needs when written to memory */
uint32 bpi_BFFaceFinder_memSize( struct bbs_Context* cpA,
								 const struct bpi_BFFaceFinder* ptrA );

/** writes object to memory; returns number of 16-bit words written */
uint32 bpi_BFFaceFinder_memWrite( struct bbs_Context* cpA,
								  const struct bpi_BFFaceFinder* ptrA, 
							      uint16* memPtrA );

/** reads object from memory; returns number of 16-bit words read 
 * Note: Before executing this function the maximum image dimensions must be specified 
 * through function bpi_BFFaceFinder_setMaxImageSize. This is to ensure proper allocation 
 * of internal memory. Otherwise this function will cause an exception.
 */
uint32 bpi_BFFaceFinder_memRead( struct bbs_Context* cpA,
								 struct bpi_BFFaceFinder* ptrA, 
								 const uint16* memPtrA,
							     struct bbs_MemTbl* mtpA );

/* ---- \ghd{ exec functions } --------------------------------------------- */

/** processes image for single face detection; 
 *  returns confidence ( 8.24 ) 
 *  fills external id cluster with node positions and ids
 *
 *  If roiPtrA is NULL, the whole image is considered for processsing 
 *  otherwise *roiPtrA specifies a section of the original image to which
 *  processing is limited. All coordinates refer to that section and must 
 *  eventually be adjusted externally.
 *  The roi rectangle must not include pixels outside of the original image
 *  (checked -> error). The rectangle may be of uneven width.
 *
 *  offsPtrA points to an offset vector (whole pixels) that is to be added to
 *  cluster coordinates in order to obtain image coordinates
 *
 */
int32 bpi_BFFaceFinder_process( struct bbs_Context* cpA,
							    const struct bpi_BFFaceFinder* ptrA, 
							    void* imagePtrA,
							    uint32 imageWidthA,
								uint32 imageHeightA,
								const struct bts_Int16Rect* roiPtrA,
								struct bts_Int16Vec2D* offsPtrA,
								struct bts_IdCluster2D* idClusterPtrA );

/** Processes integral image for multiple face detection; 
 *  returns number of faces detected 
 *  return pointer to faceBuffer (intermediate data format)
 *  call getFace() to retrieve face information from buffer.
 *  *faceDataBufferPtrA is set to the address of an internal buffer that is valid until the next image processing
 *
 *  Positions are sorted by confidence (highest confidence first)
 *
 *  When this function returns 0 (no face detected) faceDataBuffer
 *  still contains one valid entry retrievable by getFace() that 
 *  represents the most likely position. The confidence is then below
 *  or equal 0.5.
 *
 *  If roiPtrA is NULL, the whole image is considered for processsing 
 *  otherwise *roiPtrA specifies a section of the original image to which
 *  processing is limited. All coordinates refer to that section and must 
 *  eventually be adjusted externally.
 *  The roi rectangle must not include pixels outside of the original image
 *  (checked -> error). The rectangle may be of uneven width.
 */
uint32 bpi_BFFaceFinder_multiProcess( struct bbs_Context* cpA,
									  const struct bpi_BFFaceFinder* ptrA, 
									  void* imagePtrA,
									  uint32 imageWidthA,
									  uint32 imageHeightA,
									  const struct bts_Int16Rect* roiPtrA );

/** Extracts a single face from a face buffer that was previously filled with face data by function 
 *  multiProcess(). 
 *  returns confidence ( 8.24 ) 
 *  Fills external id cluster with node positions and ids
 *
 *  offsPtrA points to an offset vector (whole pixels) that is to be added to
 *  cluster coordinates in order to obtain image coordinates
 */
uint32 bpi_BFFaceFinder_getFace( struct bbs_Context* cpA,
								 const struct bpi_BFFaceFinder* ptrA, 
								 uint32 indexA,
								 struct bts_Int16Vec2D* offsPtrA,
								 struct bts_IdCluster2D* idClusterPtrA );

/** Extracts a single face from a face buffer that was previously filled with face data by function 
 *  multiProcess(). 
 *  returns confidence ( 8.24 ) 
 *  provides 
 *		- id cluster with node positions and ids
 *		- confidence
 */
void bpi_BFFaceFinder_getFaceDCR( struct bbs_Context* cpA,
								  const struct bpi_BFFaceFinder* ptrA, 
								  uint32 indexA,
								  struct bpi_DCR* dcrPtrA );

/** this function must be executed before calling _memRead */
void bpi_BFFaceFinder_setMaxImageSize( struct bbs_Context* cpA,
									   struct bpi_BFFaceFinder* ptrA, 
									   uint32 maxImageWidthA,
									   uint32 maxImageHeightA );

/** initializes some parameters prior to reading  
 *  Overload of vpSetParams
 *  wraps function setMaxImageSize
 */ 
void bpi_BFFaceFinder_setParams( struct bbs_Context* cpA,
								 struct bpi_FaceFinder* ptrA, 
								 uint32 maxImageWidthA,
								 uint32 maxImageHeightA );

/** sets detection range
 *  Overload of vpSetParams
 */
void bpi_BFFaceFinder_setRange( struct bbs_Context* cpA,
								struct bpi_FaceFinder* ptrA, 
								uint32 minEyeDistanceA,
								uint32 maxEyeDistanceA );

/** Single face processing function; returns confidence (8.24)  
 *  Overload of vpProcess
 *  wraps function process
 */ 
int32 bpi_BFFaceFinder_processDcr( struct bbs_Context* cpA,
								   const struct bpi_FaceFinder* ptrA, 
						           struct bpi_DCR* dcrPtrA );

/** Multiple face processing function; returns number of faces detected 
 *  Overload of vpPutDcr
 *  wraps function multiProcess
 */ 
int32 bpi_BFFaceFinder_putDcr( struct bbs_Context* cpA,
							   const struct bpi_FaceFinder* ptrA, 
							   struct bpi_DCR* dcrPtrA );

/** Retrieves indexed face from face finder after calling PutDCR 
 *  Overload of vpGetDcr
 *  wraps function getFaceDCR
 */ 
void bpi_BFFaceFinder_getDcr( struct bbs_Context* cpA,
							  const struct bpi_FaceFinder* ptrA, 
							  uint32 indexA,
							  struct bpi_DCR* dcrPtrA );

#endif /* bpi_BF_FACE_FINDER_EM_H */
