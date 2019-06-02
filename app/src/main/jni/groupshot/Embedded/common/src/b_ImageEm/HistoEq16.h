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

#ifndef bim_HISTOEQ16_EM_H
#define bim_HISTOEQ16_EM_H

/* ---- includes ----------------------------------------------------------- */

/* ---- related objects  --------------------------------------------------- */

struct bim_UInt16ByteImage;
struct bts_Int16Rect;

/* ---- typedefs ----------------------------------------------------------- */

/* ---- constants ---------------------------------------------------------- */

/* ---- external functions ------------------------------------------------- */

/** Histogram equalization of image */
void bim_UInt16ByteImage_equalize( struct bim_UInt16ByteImage* imagePtrA );

/** Histogram equalization using histogram generated from subregion.
  * While the histogram is taken only in the specified sub-section of the
  * image, the equalization, i.e. remapping of the pixel values, is 
  * performed on the whole image.
  *
  * @param imagePtrA    pointer to image to be equalized
  * @param sectionPtrA  section specifying region in image where histogram is
  *                     generated from
  */
void bim_UInt16ByteImage_equalizeSection( struct bim_UInt16ByteImage* imagePtrA,
										  const struct bts_Int16Rect* sectionPtrA );

/* ------------------------------------------------------------------------- */

#endif /* bim_HISTOEQ16_EM_H */

