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

#include <math.h>
#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "almashot.h"
#include "blurless.h"
#include "supersensor.h"
#include "superzoom.h"

#include "ImageConversionUtils.h"

// currently - no concurrent processing, using same instance for all processing types
static unsigned char *yuv[MAX_FRAMES] = {NULL};
static void *instance = NULL;
static int almashot_inited = 0;



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


extern "C" JNIEXPORT void JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_NightAddYUVFrames
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
	unsigned char **yuvIn;

	yuvIn = (unsigned char**)env->GetIntArrayElements(in, NULL);

	/*
	for (int i=0; i<nFrames; ++i)
	{
		char str[256];
		sprintf(str, "/sdcard/DCIM/nightin%02d.yuv", i);
		FILE *f = fopen (str, "wb");
		fwrite(yuvIn[i], sx*sy+2*((sx+1)/2)*((sy+1)/2), 1, f);
		fclose(f);
	} //*/

	// pre-allocate uncompressed yuv buffers
	for (i=0; i<nFrames; ++i)
		yuv[i] = yuvIn[i];

	env->ReleaseIntArrayElements(in, (jint*)yuvIn, JNI_ABORT);
}


extern "C" JNIEXPORT jboolean JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_CheckClipping
(
	JNIEnv* env,
	jobject thiz,
	jint in,
	jint sx,
	jint sy,
	jint x0,
	jint y0,
	jint w,
	jint h
)
{
	Uint8 *yuv = (Uint8 *)in;

	return Super_ExposureVerification(yuv, sx, sy, x0, y0, w, h);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_night_AlmaShotNight_Process
(
	JNIEnv* env,
	jobject thiz,
	jint sx,
	jint sy,
	jint sxo,
	jint syo,
	jint iso,
	jint noisePref,
	jint DeGhostPref,
	jint lumaEnh,
	jint chromaEnh,
	jfloat fgamma,
	jint nImages,
	jintArray jcrop,
	jint orientation,
	jboolean mirror,
	jfloat zoom,
	jint cameraIndex,
	jboolean isCamera2
)
{
	Uint8 *OutPic, *OutNV21;
	int *crop;
	int nTable[3] = {256/2, 256, 3*256/2};
	int deghostTable[3] = {3*256/4, 256, 3*256/2};

	crop = (int*)env->GetIntArrayElements(jcrop, NULL);

	if (isCamera2)
	{
		//__android_log_print(ANDROID_LOG_ERROR, "Almalence", "sx:%d sy:%d sxo:%d syo:%d", sx, sy, sxo, syo);

		// find zoomed region in the frame
		int sx_zoom = (int)(sx/zoom) + 2*SIZE_GUARANTEE_BORDER;
		int sy_zoom = (int)(sy/zoom) + 2*SIZE_GUARANTEE_BORDER;
		sx_zoom -= sx_zoom&3;
		sy_zoom -= sy_zoom&3;
		if (sx_zoom > sx) sx_zoom = sx;
		if (sy_zoom > sy) sy_zoom = sy;

		// in-place crop of input frames according to the zoom value
		if ((sx_zoom < sx) || (sy_zoom < sy))
		{
			int x0 = (sx-sx_zoom)/2;
			int y0 = (sy-sy_zoom)/2;
			x0 -= x0&1;
			y0 -= y0&1;

			for (int i=0; i<nImages; ++i)
			{
				// Y part
				for (int y=0; y<sy_zoom; ++y)
					memmove(&yuv[i][y*sx_zoom], &yuv[i][x0+(y+y0)*sx], sx_zoom);

				// UV part
				for (int y=0; y<sy_zoom/2; ++y)
					memmove(&yuv[i][sx_zoom*sy_zoom+y*sx_zoom], &yuv[i][sx*sy+x0+(y+y0/2)*sx], sx_zoom);
			}
		}

		if (fgamma != 0.0f && (iso > 0) && cameraIndex != 903)
		{
			// iso 100 = +0.1
			// iso 800 = -0.05
			fgamma += 0.09f - (logf(iso) * 3.321928095f - 6.644f) * 0.165f/3.0f;//log2f replaced with logf(iso) * 3.321928095f for android <4.3
		}

		if (fgamma != 0.0f)
		{
			if (fgamma < 0.43f) fgamma = 0.43f;
			if (fgamma > 0.6f) fgamma = 0.6f;
		}

		int zoomAbove15x = sxo >= 3*(sx_zoom-2*SIZE_GUARANTEE_BORDER)/2;
		int zoomAbove30x = sxo >= 3*sx_zoom;
		float zoom = sxo / float(sx_zoom);

		int filter = 256;
		int sharpen = 1;
		if (zoomAbove30x) sharpen = 0x80; // fine edge enhancement instead of primitive sharpen at high zoom levels

//
//
//
//		if (fgamma && (iso>0))
//		{
//			// iso 100 = +0.1
//			// iso 800 = -0.05
//			fgamma += 0.1f - ( logf(iso) * 3.321928095f-6.644f)*0.15f/3.f;
//			if (fgamma < 0.45f) fgamma = 0.45f;
//			if (fgamma > 0.6f) fgamma = 0.6f;
//		}
//
//		// for SR-only fgamma = 0, gamma will evaluate to 0 also
//		int gamma = (int)(fgamma * 256 + 0.5f);
//
//		// threshold at which profiles are switched (about 1.5x zoom)
//		int zoomAbove15x = sxo >= 3*(sx_zoom-2*SIZE_GUARANTEE_BORDER)/2;
//		int zoomAbove30x = sxo >= 3*sx_zoom;


		switch (cameraIndex)
		{
		case 103:// Nexus 6
		case 507:// LG G Flex2
		case 1900:// Cyanogen 0
		case 2201:
			break;

		case 100:		// Nexus 5
			// slightly more sharpening and less filtering at low zooms
			sharpen = 2;
			break;

		case 105:// Nexus 5X - same camera module as in Nexus 6p
		case 106:// Nexus 6P
			sharpen = 2;
			break;

		case 903:// HTC10
			// use both edge enhancement and sharpening at high zoom levels
			sharpen = 2;
			break;

		case 1005:// Galaxy S6
			// do not use fine edge enhancement (looks too plastic on S6)
			sharpen = 1;
			break;

		case 1006:// Galaxy S7
			// do not use fine edge enhancement (looks too plastic on S6)
			sharpen = 1;
			break;

		case 2000:// OnePlus 2
			break;
		}
		if (zoomAbove30x) sharpen = 0x80;	// fine edge enhancement instead of primitive sharpen at high zoom levels

		//__android_log_print(ANDROID_LOG_ERROR, "Almalence", "Before Super_Process, sensorGain: %d, deghostGain: %d, filter: %d, sharpen: %d, nImages: %d cameraIndex: %d",
		//		sensorGain, deGhostGain, filter, sharpen, nImages, cameraIndex);

		int err = Super_Process(
				yuv, NULL, &OutPic,
				sx_zoom, sy_zoom, sx_zoom, sxo, syo, nImages,
				iso,
				filter,
				sharpen,
				fgamma,
				4.5f,
				cameraIndex,
				0);							// externalBuffers

		//__android_log_print(ANDROID_LOG_ERROR, "Almalence", "Super_Process finished, iso: %d, noise: %d %d", iso, noisePref, nTable[noisePref]);
		__android_log_print(ANDROID_LOG_INFO, "Almalence", "super processing finished, result code: %d", err);

		crop[0]=crop[1]=0;
		crop[2]=sxo;
		crop[3]=syo;
	}
	else
	{
		BlurLess_Preview(&instance, yuv, NULL, NULL, NULL,
			0, // 256*3,
			deghostTable[DeGhostPref], 1,
			2, nImages, sx, sy, 0, nTable[noisePref], 1, 0, lumaEnh, chromaEnh, 0);

		crop[0]=crop[1]=crop[2]=crop[3]=-1;
		BlurLess_Process(instance, &OutPic, &crop[0], &crop[1], &crop[2], &crop[3]);
	}

	int flipLeftRight, flipUpDown;
	int rotate90 = orientation == 90 || orientation == 270;
	if (mirror)
		flipUpDown = flipLeftRight = orientation == 180 || orientation == 90;
	else
		flipUpDown = flipLeftRight = orientation == 180 || orientation == 270;

	// 90/270-degree rotations are out-ot-place
	OutNV21 = OutPic;
	if (rotate90)
		OutNV21 = (Uint8 *)malloc(sxo*syo+2*((sxo+1)/2)*((syo+1)/2));

	TransformNV21(OutPic, OutNV21, sxo, syo, crop, flipLeftRight, flipUpDown, rotate90);

	if (rotate90)
	{
		free(OutPic);
		OutPic = OutNV21;
	}

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, JNI_ABORT);

	return (jint)OutPic;
}
