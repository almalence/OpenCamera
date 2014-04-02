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

#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "almashot.h"
#include "CreateJavaOutputStreamAdaptor.h"
#include "YuvToJpegEncoder.h"

static unsigned char *yuv[50] = {NULL};
static int SX = 0;
static int SY = 0;

extern "C" {
JNIEXPORT jboolean JNICALL Java_com_almalence_YuvImage_SaveJpegFreeOut
(
	JNIEnv* env, jobject, jint jout,
	int format, int width, int height, jintArray offsets,
	jintArray strides, int jpegQuality, jobject jstream,
	jbyteArray jstorage
);

};

JNIEXPORT jboolean JNICALL Java_com_almalence_YuvImage_SaveJpegFreeOut
(
	JNIEnv* env, jobject, jint jout,
	int format, int width, int height, jintArray offsets,
	jintArray strides, int jpegQuality, jobject jstream,
	jbyteArray jstorage
)
{
	jbyte* OutPic;

	OutPic = (jbyte*)jout;

	SkWStream* strm = CreateJavaOutputStreamAdaptor(env, jstream, jstorage);

	jint* imgOffsets = env->GetIntArrayElements(offsets, NULL);
	jint* imgStrides = env->GetIntArrayElements(strides, NULL);
	YuvToJpegEncoder* encoder = YuvToJpegEncoder::create(format, imgStrides);
	if (encoder == NULL)
	{
		free(OutPic);
		return false;
	}

	bool result = encoder->encode(strm, OutPic, width, height, imgOffsets, jpegQuality);

	delete encoder;
	env->ReleaseIntArrayElements(offsets, imgOffsets, 0);
	env->ReleaseIntArrayElements(strides, imgStrides, 0);

	return result;
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_YuvImage_GetFrame
(
	JNIEnv* env,
	jobject thiz,
	jint index
)
{
	return (jint)yuv[index];
}

extern "C" JNIEXPORT jbyte* JNICALL Java_com_almalence_YuvImage_GetByteFrame
(
	JNIEnv* env,
	jobject thiz,
	jint index
)
{
	int x, y;
	jbyteArray jpixels = NULL;
	Uint8 * pixels;

	jpixels = env->NewByteArray(SX*SY+SX*((SY+1)/2));

	pixels = (Uint8 *)env->GetByteArrayElements(jpixels, NULL);

//	for (y=0; y<SY; ++y)
//	{
//		for (x=0; x<SX; ++x)
//		{
//			pixels[y+x] = yuv[index][y+x];
//		}
//	}

	for (y=0; y<SY; y+=2)
		{
			// Y
			memcpy (&pixels[y*SX],     &yuv[index][y*SX],   SX);
			memcpy (&pixels[(y+1)*SX], &yuv[index][(y+1)*SX], SX);

			// UV - no direct memcpy as swap may be needed
			for (x=0; x<SX/2; ++x)
			{
				// U
				pixels[SX*SY+(y/2)*SX+x*2+1] = yuv[index][SX*SY+(y/2)*SX+x*2+1];

				// V
				pixels[SX*SY+(y/2)*SX+x*2]   = yuv[index][SX*SY+(y/2)*SX+x*2];
			}
		}

	env->ReleaseByteArrayElements(jpixels, (jbyte*)pixels, JNI_ABORT);

	return (jbyte *)jpixels;
}

extern "C" JNIEXPORT int JNICALL Java_com_almalence_YuvImage_CreateYUVImage
(
		JNIEnv* env,
		jobject thiz,
		jobject bufY,
		jobject bufU,
		jobject bufV,
		jint pixelStrideY,
		jint rowStrideY,
		jint pixelStrideU,
		jint rowStrideU,
		jint pixelStrideV,
		jint rowStrideV,
		jint sx,
		jint sy,
		jint n
)
{
	int i, x, y;
	Uint8 *Y, *U, *V;
	Uint8 *UV;

	// All Buffer objects have an effectiveDirectAddress field
	Y = (Uint8*)env->GetDirectBufferAddress(bufY);
	U = (Uint8*)env->GetDirectBufferAddress(bufU);
	V = (Uint8*)env->GetDirectBufferAddress(bufV);

	if ((Y == NULL) || (U == NULL) || (V == NULL))
		return -1;

	SX = sx;
	SY = sy;

	// extract crop as NV21 image
	yuv[n] = (unsigned char *)malloc (sx*sy+sx*((sy+1)/2));
	if (yuv[n] == NULL)
		return -2;

	__android_log_print(ANDROID_LOG_INFO, "OpenCamera. CreateYUV", "Allocated memory for frame %d", n);

	// Note: assumption of:
	// - even w, h, x0 here (guaranteed by SZ requirements)
	// - pixelStrideY=1 (guaranteed by android doc)
	// - U,V being sub-sampled 2x horizontally and vertically
	for (y=0; y<sy; y+=2)
	{
		// Y
		memcpy (&yuv[n][y*sx],     &Y[y*rowStrideY],   sx);
		memcpy (&yuv[n][(y+1)*sx], &Y[(y+1)*rowStrideY], sx);

		// UV - no direct memcpy as swap may be needed
		for (x=0; x<sx/2; ++x)
		{
			// U
			yuv[n][sx*sy+(y/2)*sx+x*2+1] = U[x*pixelStrideU + (y/2)*rowStrideU];

			// V
			yuv[n][sx*sy+(y/2)*sx+x*2]   = V[x*pixelStrideV + (y/2)*rowStrideV];
		}
	}

	return 0;
}
