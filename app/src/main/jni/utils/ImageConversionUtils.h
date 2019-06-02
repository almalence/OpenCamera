/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013
by Almalence Inc. All Rights Reserved.
*/

#ifndef __IMAGECONVERSION_UTILS_H__
#define __IMAGECONVERSION_UTILS_H__

inline int min(int a, int b)
{
	return (a > b ? b : a);
}
inline int max(int a, int b)
{
	return (a > b ? a : b);
}

enum {
	PIXELS_BGRA,
	PIXELS_RGBA,
	PIXELS_RGB
};


int JPEG2NV21(
	unsigned char* yuv,
	unsigned char* jpegdata,
	int jpeglen,
	int sx,
	int sy,
	bool needRotation,
	bool cameraMirrored,
	int rotationDegree
);

int JPEG2RGBA
(
	unsigned char *dst,
	unsigned char *jpegdata,
	int jpeglen
);

int DecodeAndRotateMultipleJpegs
(
	unsigned char **yuvFrame,
	unsigned char **jpeg,
	int *jpeg_length,
	int sx,
	int sy,
	int nFrames,
	int needRotation,
	int cameraMirrored,
	int rotationDegree,
	bool needFreeMem//true by default
);

void TransformPlane32bit
(
	unsigned int * In,
	unsigned int * Out,
	int sx,
	int sy,
	int flipLeftRight,
	int flipUpDown,
	int rotate90
);


// mirror and/or rotate NV21 image
//
// Note:
//   - mirroring can be performed in-place, rotation can not
//   - rotation is 90 degree clockwise
//   - if need to rotate 180 degree - call with flipLeftRight=1, flipUpDown=1
//   - if need to rotate 90 degree counter-clockwise - call with flipLeftRight=1, flipUpDown=1, rotate90=1
//   - it is assumed that image width is even and image height is even
//   - crop coordinates are also transformed if given, but no cropping is performed
//
void TransformNV21
(
	unsigned char * InNV21,
	unsigned char * OutNV21,
	int sx,
	int sy,
	int *crop,
	int flipLeftRight,
	int flipUpDown,
	int rotate90
);


void NV21_to_RGB
(
	unsigned char * in,
	int * out,
	int   w,
	int   h,
	int   rotate
);


// convert NV21 into RGB or BGRA with 90 degree rotation and scaling
// used to make portrait-oriented on-screen previews
void NV21_to_RGB_scaled_rotated
(
	unsigned char *pY,
	int width,
	int height,
	int x0,
	int y0,
	int wCrop,
	int hCrop,
	int outWidth,
	int outHeight,
	int stride,
	unsigned char *buffer
);

void NV21_to_RGB_scaled
(
	unsigned char *pY,
	int width,
	int height,
	int x0,
	int y0,
	int wCrop,
	int hCrop,
	int outWidth,
	int outHeight,
	int stride,
	unsigned char *buffer
);

void NV21_to_Gray_scaled
(
	unsigned char *pY,
	int width,
	int height,
	int x0,
	int y0,
	int wCrop,
	int hCrop,
	int outWidth,
	int outHeight,
	unsigned char *buffer
);

void addRoundCornersRGBA8888
(
	unsigned char * const rgb_bytes,
	const int outWidth,
	const int outHeight
);

#endif // __IMAGECONVERSION_UTILS_H__
