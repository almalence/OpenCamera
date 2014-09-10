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
#include "blurless.h"
#include "superzoom.h"

#include "ImageConversionUtils.h"

// currently - no concurrent processing, using same instance for all processing types
static unsigned char *yuv[MAX_FRAMES] = {NULL};
static void *instance = NULL;
static int almashot_inited = 0;
static Uint8 *OutPic = NULL;



extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_Initialize
(
	JNIEnv* env,
	jobject thiz
)
{
	char status[1024];
	int err=0;
	long mem_used, mem_free;

	if (almashot_inited == 0)
	{
		err = AlmaShot_Initialize(0);

		if (err == 0)
			almashot_inited = 1;
	}

	sprintf (status, "init status: %d\n", err);
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_Release
(
	JNIEnv* env,
	jobject thiz
)
{
	int i;

	if (almashot_inited == 1)
	{
		AlmaShot_Release();

		almashot_inited = 0;
	}

	return 0;
}


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_NightAddYUVFrames
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jint nFrames,
	jint sx,
	jint sy
)
{
	int i;
	unsigned char * *yuvIn;
	char status[1024];

	Uint8 *inp[4];
	int x, y;
	int x0_out, y0_out, w_out, h_out;

	yuvIn = (unsigned char**)env->GetIntArrayElements(in, NULL);

//	for (int i=0; i<nFrames; ++i)
//	{
//		char str[256];
//		sprintf(str, "/sdcard/DCIM/nightin%02d.yuv", i);
//		FILE *f = fopen (str, "wb");
//		fwrite(yuvIn[i], sx*sy+2*((sx+1)/2)*((sy+1)/2), 1, f);
//		fclose(f);
//	}

	// pre-allocate uncompressed yuv buffers
	for (i=0; i<nFrames; ++i)
	{
		yuv[i] = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

		if (yuv[i]==NULL)
		{
			i--;
			for (;i>=0;--i)
			{
				free(yuv[i]);
				yuv[i] = NULL;
			}
			break;
		}

		//		yuv[i] = yuvIn[i];
		for (y=0; y<sy; y+=2)
		{
			// Y
			memcpy (&yuv[i][y*sx],     &yuvIn[i][y*sx],   sx);
			memcpy (&yuv[i][(y+1)*sx], &yuvIn[i][(y+1)*sx], sx);

			// UV - no direct memcpy as swap may be needed
			for (x=0; x<sx/2; ++x)
			{
				// U
				yuv[i][sx*sy+(y/2)*sx+x*2+1] = yuvIn[i][sx*sy+(y/2)*sx+x*2+1];

				// V
				yuv[i][sx*sy+(y/2)*sx+x*2]   = yuvIn[i][sx*sy+(y/2)*sx+x*2];
			}
		}
	}

	env->ReleaseIntArrayElements(in, (jint*)yuvIn, JNI_ABORT);

	//sprintf (status, "frames total: %d\nsize0: %d\nsize1: %d\nsize2: %d\n", (int)nFrames, jpeg_length[0], jpeg_length[1], jpeg_length[2]);
	sprintf (status, "frames total: %d\n", (int)nFrames);
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_Process
(
	JNIEnv* env,
	jobject thiz,
	jint sx,
	jint sy,
	jint sxo,
	jint syo,
	jint sensorGainPref,
	jint DeGhostPref,
	jint lumaEnh,
	jint chromaEnh,
	jint nImages,
	jintArray jcrop,
	jboolean jrot,
	jboolean jmirror,
	jboolean isHALv3
)
{
	Uint8 *OutPic, *OutNV21;
	int *crop;
	int nTable[3] = {2,4,6};
	int deghTable[3] = {256/2, 256, 3*256/2};

	crop = (int*)env->GetIntArrayElements(jcrop, NULL);

	BlurLess_Preview(&instance, yuv, NULL, NULL, NULL,
		0, // 256*3,
		deghTable[DeGhostPref], 1,
		2, nImages, sx, sy, 0, 64*nTable[sensorGainPref], 1, 0, lumaEnh, chromaEnh, 0);

	crop[0]=crop[1]=crop[2]=crop[3]=-1;
	BlurLess_Process(instance, &OutPic, &crop[0], &crop[1], &crop[2], &crop[3]);

	OutNV21 = OutPic;
	if (jrot)
		OutNV21 = (Uint8 *)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	TransformNV21(OutPic, OutNV21, sx, sy, crop, jmirror&&jrot, jmirror&&jrot, jrot);

	if (jrot)
	{
		free(OutPic);
		OutPic = OutNV21;
	}

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, JNI_ABORT);

	return (jint)OutPic;
}
