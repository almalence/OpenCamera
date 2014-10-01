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
#include "movobj.h"

#include "ImageConversionUtils.h"

#ifdef LOG_ON
#define LOG_TAG "ObjectRemoval"
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__ )
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__ )
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__ )
#else
#define LOG_TAG
#define LOGD(...)
#define LOGE(...)
#define LOGI(...)
#define LOGW(...)
#endif

// This triggers openmp constructors and destructors to be called upon library load/unload
void __attribute__((constructor)) initialize_openmp() {}
void __attribute__((destructor)) release_openmp() {}


#define MAX_MOV_FRAMES	10


static unsigned char *inputFrame[MAX_MOV_FRAMES] = {NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL};
static void *instance = NULL;
static int almashot_inited = 0;
static Uint8 *OutPic = NULL;


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_objectremoval_AlmaCLRShot_Initialize
(
	JNIEnv* env,
	jobject thiz
)
{
	char status[1024];
	int err=0;

	if (almashot_inited == 0)
	{
		err = AlmaShot_Initialize(0);
		if (err == 0)
			almashot_inited = 1;
	}

	instance = NULL;

	sprintf (status, " init status: %d\n", err);
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_objectremoval_AlmaCLRShot_Release
(
	JNIEnv*,
	jobject,
	jint nFrames
)
{
	LOGD("Release - start");
	int i;

	for (int i=0; i<nFrames; ++i)
	{
		free(inputFrame[i]);
		inputFrame[i] = NULL;
	}

	MovObj_FreeInstance(instance);

	if (almashot_inited == 1)
	{
		AlmaShot_Release();

		almashot_inited = 0;
	}
	LOGD("Release - end");
	return 0;
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_objectremoval_AlmaCLRShot_AddYUVInputFrame
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jintArray in_len,
	jint nFrames,
	jint sx,
	jint sy
)
{
	int i;
	int *yuv_length;
	unsigned char * *yuv;
	int isFoundinInput;

	yuv = (unsigned char**)env->GetIntArrayElements(in, NULL);
	yuv_length = (int*)env->GetIntArrayElements(in_len, NULL);

	// pre-allocate uncompressed yuv buffers
	for (i=0; i<nFrames; ++i)
	{
		inputFrame[i] = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

		if (inputFrame[i]==NULL)
		{
			i--;
			for (;i>=0;--i)
			{
				free(inputFrame[i]);
				inputFrame[i] = NULL;
			}
			break;
		}

		inputFrame[i] = yuv[i];
		++isFoundinInput;
	}
	//isFoundinInput = DecodeAndRotateMultipleJpegs(inputFrame, jpeg, jpeg_length, sx, sy, nFrames, 0, 0, 0);

	env->ReleaseIntArrayElements(in, (jint*)yuv, JNI_ABORT);
	env->ReleaseIntArrayElements(in_len, (jint*)yuv_length, JNI_ABORT);

	return isFoundinInput;
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_objectremoval_AlmaCLRShot_ConvertFromJpeg
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jintArray in_len,
	jint nFrames,
	jint sx,
	jint sy
)
{
	int *jpeg_length;
	unsigned char * *jpeg;
	int isFoundinInput;

	jpeg = (unsigned char**)env->GetIntArrayElements(in, NULL);
	jpeg_length = (int*)env->GetIntArrayElements(in_len, NULL);

	isFoundinInput = DecodeAndRotateMultipleJpegs(inputFrame, jpeg, jpeg_length, sx, sy, nFrames, 0, 0, 0, true);

	env->ReleaseIntArrayElements(in, (jint*)jpeg, JNI_ABORT);
	env->ReleaseIntArrayElements(in_len, (jint*)jpeg_length, JNI_ABORT);

	return isFoundinInput;
}


extern "C" JNIEXPORT jintArray JNICALL Java_com_almalence_plugins_processing_objectremoval_AlmaCLRShot_NV21toARGB
(
	JNIEnv* env,
	jobject thiz,
	jint inptr,
	jobject srcSize,
	jobject rect,
	jobject dstSize
)
{
	LOGD("NV21toARGB - start");

	Uint32 * pixels;
	jintArray jpixels = NULL;

	jclass src_size = env->GetObjectClass(srcSize);
	jfieldID id_srcW = env->GetFieldID(src_size, "width", "I");
	jint srcW = env->GetIntField(srcSize,id_srcW);
	jfieldID id_srcH = env->GetFieldID(src_size, "height", "I");
	jint srcH = env->GetIntField(srcSize,id_srcH);

	jclass class_rect = env->GetObjectClass(rect);
	jfieldID id_left = env->GetFieldID(class_rect, "left", "I");
	jint left = env->GetIntField(rect,id_left);
	jfieldID id_top = env->GetFieldID(class_rect, "top", "I");
	jint top = env->GetIntField(rect,id_top);
	jfieldID id_right = env->GetFieldID(class_rect, "right", "I");
	jint right = env->GetIntField(rect,id_right);
	jfieldID id_bottom = env->GetFieldID(class_rect, "bottom", "I");
	jint bottom = env->GetIntField(rect,id_bottom);

	jclass dst_size = env->GetObjectClass(dstSize);
	jfieldID id_dstW = env->GetFieldID(dst_size, "width", "I");
	jint dstW = env->GetIntField(dstSize,id_dstW);
	jfieldID id_dstH = env->GetFieldID(dst_size, "height", "I");
	jint dstH = env->GetIntField(dstSize,id_dstH);

	LOGD("inptr = %d srcW = %d srcH = %d ", inptr, srcW, srcH);
	LOGD("left = %d top = %d right = %d bottom = %d ", left, top, right, bottom);
	LOGD("dstW = %d dstH = %d", dstW, dstH);

	jpixels = env->NewIntArray(dstW*dstH);
	LOGD("Memory alloc size = %d * %d", dstW, dstH);
	pixels = (Uint32 *)env->GetIntArrayElements(jpixels, NULL);

	NV21_to_RGB_scaled((Uint8 *)inptr, srcW, srcH, left, top, right - left, bottom - top, dstW, dstH, 4, (Uint8 *)pixels);

	env->ReleaseIntArrayElements(jpixels, (jint*)pixels, 0);

	LOGD("NV21toARGB - end");

	return jpixels;
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_objectremoval_AlmaCLRShot_getInputFrame
(
	JNIEnv* env,
	jobject thiz,
	jint index
)
{
	return (jint)inputFrame[index];
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_objectremoval_AlmaCLRShot_MovObjProcess
(
	JNIEnv* env,
	jobject thiz,
	jint nFrames,
	jobject size,
	jint sensitivity,
	jint minSize,
	jintArray jbase_area,
	jintArray jcrop,
	jbyteArray jlayout,
	jint ghosting,
	jint ratio
)
{
	Uint8 *layout;
	int *base_area;
	int *crop;
	int x,y;
	int tmp;

	LOGD("MovObjProcess - start");

	jclass src_size = env->GetObjectClass(size);
	jfieldID id_srcW = env->GetFieldID(src_size, "width", "I");
	jint sx = env->GetIntField(size,id_srcW);
	jfieldID id_srcH = env->GetFieldID(src_size, "height", "I");
	jint sy = env->GetIntField(size,id_srcH);

	LOGD("nFrames = %d sx = %d sy = %d", nFrames, sx, sy);

	OutPic = (Uint8 *)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));
	LOGD("Memory alloc size = %d", sx*sy+2*((sx+1)/2)*((sy+1)/2));

	base_area = (int*)env->GetIntArrayElements(jbase_area, NULL);
	crop = (int*)env->GetIntArrayElements(jcrop, NULL);
	layout = (Uint8 *)env->GetByteArrayElements(jlayout, NULL);

//	saveBufferToFile(inputFrame[0], sx * sy * 3 / 2, "/mnt/sdcard/input.yuv");

	/* {
		for (int i=0; i<nFrames; ++i)
		{
			char str[256];
			sprintf(str, "/sdcard/DCIM/ClrCam/in%02d.yuv", i);
			FILE *f = fopen (str, "wb");
			fwrite(inputFrame[i], sx*sy+sx*sy/2, 1, f);
			fclose(f);
		}
	} */

	int sports_mode_order[MAX_FRAMES] = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, -1};
	int use_sports_mode = 0; // 1;
	int fastmode = 0;

	switch (ratio)
	{
	case 16:
		LOGD("processing mode : fast mode");
		fastmode = 1;
		break;
	case 8:
		LOGD("processing mode : normal mode");
		fastmode = 0;
		break;
	default:
		LOGD("mode is invalid");
	}

	/*
	 * 		0 = detect and remove only objects on stable background.
	 *		1 = same as '0' but also detect objects on unstable background, which are unlikely to cause ghosting.
	 *		2 = same as '1' but also remove objects on unstable background.
	 *		3 = same as '0' but also detect objects on unstable background which will cause ghosting.
	 *		4 = same as '3' but also remove objects on unstable background.
	 */
	MovObj_Process(&instance, inputFrame, OutPic,
			layout, NULL, // used in manual removal mode
			256, // gain (sensor specific, might need adjustment)
			sx, sy, nFrames,
			sensitivity, // sensitivity
			minSize,	// smallest detectable object size
			5, // do not detect weak objects (with little movement or low contrast)
			ghosting,// prevent ghosting
			0, // no extra border around detected object
			0, NULL, // sports-mode parameters
			0, // default maximum displacement between input frames
			0, // do not use post-filter
			2,	// re-scale output (and keep aspect ratio)
			&base_area[0], &base_area[1], &base_area[2], &base_area[3], // area covered in base frame by Layout
			&crop[0], &crop[1], &crop[2], &crop[3], &crop[4],
			fastmode, // use fast mode (recommended for sensors >= 8Mpix)
			0);

	env->ReleaseIntArrayElements(jbase_area, (jint*)base_area, JNI_COMMIT);

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, JNI_COMMIT);
	env->ReleaseByteArrayElements(jlayout, (jbyte*)layout, JNI_COMMIT);

	LOGD("MovObjProcess - end");

	return (jint)OutPic;
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_objectremoval_AlmaCLRShot_MovObjFixHoles
(
	JNIEnv* env,
	jobject thiz,
	jobject size,
	jbyteArray jenumer,
	jint baseFrame
)
{
	Uint8 *layout, *enumer;
	int totalObj = 0;

	LOGD("MovObjFixHoles - start");

	jclass src_size = env->GetObjectClass(size);
	jfieldID id_w = env->GetFieldID(src_size, "width", "I");
	jint sx = env->GetIntField(size,id_w);
	jfieldID id_h = env->GetFieldID(src_size, "height", "I");
	jint sy = env->GetIntField(size,id_h);

	LOGD("sx = %d sy = %d", sx, sy);

	enumer = (Uint8 *)env->GetByteArrayElements(jenumer, NULL);

	MovObj_FixHoles(enumer, sx, sy, baseFrame);

	env->ReleaseByteArrayElements(jenumer, (jbyte*)enumer, JNI_COMMIT);

	LOGD("MovObjFixHoles - end");

	return 0;
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_objectremoval_AlmaCLRShot_MovObjEnumerate
(
	JNIEnv* env,
	jobject thiz,
	jint nFrames,
	jobject size,
	jbyteArray jlayout,
	jbyteArray jenumer,
	jint baseFrame
)
{
	Uint8 *layout, *enumer;
	int totalObj = 0;

	LOGD("MovObjEnumerate - start");

	jclass src_size = env->GetObjectClass(size);
	jfieldID id_w = env->GetFieldID(src_size, "width", "I");
	jint sx = env->GetIntField(size,id_w);
	jfieldID id_h = env->GetFieldID(src_size, "height", "I");
	jint sy = env->GetIntField(size,id_h);

	LOGD("nFremes = %d sx = %d sy = %d", nFrames, sx, sy);

	layout = (Uint8 *)env->GetByteArrayElements(jlayout, NULL);
	enumer = (Uint8 *)env->GetByteArrayElements(jenumer, NULL);

	totalObj = MovObj_Enumerate(layout, enumer, sx, sy, baseFrame, nFrames);

	env->ReleaseByteArrayElements(jenumer, (jbyte*)enumer, JNI_COMMIT);
	env->ReleaseByteArrayElements(jlayout, (jbyte*)layout, JNI_ABORT);

	LOGD("MovObjEnumerate - end, objects detected: %d", totalObj);

	return (jint) totalObj;
}
