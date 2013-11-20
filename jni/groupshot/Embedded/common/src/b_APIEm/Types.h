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

#ifndef bpi_TYPES_EM_H
#define bpi_TYPES_EM_H

/**
 * This file contains gerenral purpose types.
 */

/* ---- includes ----------------------------------------------------------- */

/* ---- related objects  --------------------------------------------------- */

/* ---- typedefs ----------------------------------------------------------- */

/** Type of module */
enum bpi_ModuleType
{
	bpi_UNDEFINED,
	bpi_OLD_FACE_FINDER_REMOVED,
	bpi_FACE_FINDER,
	bpi_LANDMARKER,
	bpi_CONVERTER,
	bpi_IDENTIFIER
};

/** List of object identifiers 
 *  This list is is synchronized with enum list epi_ObjectId
 *  Not all types are neccessarily in use in the embedded realm
 *  values in round braces ( e.g. (32) ) denote the size in bits of the associated data type
 *
 *  Object formats:
 *		ASCII String: 0-terminates string of characters
 *
 *		Image:	<(32) type><(32) width><(32) height><(8) byte1><(8) byte2>....
 *				type: 0: gray image - pixels are bytes starting at upper left corner
 *				      1: rgb color images - prixels are 3-byte rgb groups starting at upper left corner
 *					  2: jpeg compressed image (<(32) type><(32) width><(32) height> precede jpeg data)
 *
 *      Cue:    SDK compatible template (bpi_IdCueHdr + subsequent data)
 *
 *  The type values never change. Type numbers can be taken for granted.
 */
enum bpi_ObjectId
{
	bpi_ID_FILE,                 /** (ASCII String) file name (of image) */
	bpi_ID_BOUNDING_BOX,         /** bounding box (coordinates of original image) */
	bpi_ID_GRAPH,                /** ground truth graph */
	bpi_ID_FILE_LIST,		     /** list of filenames  */
	bpi_ID_GRAPH_LIST,			 /** list of egp_SpatialGraph (multiple ground truth graphs per image) */
	bpi_ID_GROUP,				 /** generic group element (used in the embedded domain to identify an object set) */
	bpi_ID_IMAGE = 256,          /** (Image) downscaled byte image */
	bpi_ID_IMAGE_FRAME,          /** bounding box surrounding original image */
	bpi_ID_IMAGE_ID,			 /** (32)-integer id number of person por object in image */
	bpi_ID_SIGNATURE_NAME = 512, /** (ASCII String) name of gallery element (=signature) */
	bpi_ID_CONFIDENCE,           /** general purpose confidence value */
	bpi_ID_CUE,                  /** (Cue) general purpose cue   */
	bpi_ID_PCA_MAT,              /** eigenvector matrix obtained from PCA analysis */
	bpi_ID_PCA_AVG,              /** PCA average vector */
	bpi_ID_PCA_EVL,              /** PCA eigen values */
	bpi_ID_COMMENT               /** (ASCII String) comment or description of data */
	// never modify this list alone (!) - modification must be initiated in Kernel/API
};

/* ---- constants ---------------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

#endif /* bpi_TYPES_EM_H */

