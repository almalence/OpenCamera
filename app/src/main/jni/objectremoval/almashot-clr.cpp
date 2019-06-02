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
		if (err == ALMA_ALL_OK)
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
	return 0;
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
	unsigned char * *yuv;
	char status[1024];
	int isFoundinInput = 255;

	yuv = (unsigned char**)env->GetIntArrayElements(in, NULL);

	for (i=0; i<nFrames; ++i)
		inputFrame[i] = yuv[i];

	env->ReleaseIntArrayElements(in, (jint*)yuv, JNI_ABORT);

	LOGD("frames total: %d\n", (int)nFrames);
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
	jint ratio,
	jintArray jsports_order
)
{
	Uint8 *layout;
	int *base_area;
	int *crop;
	int x,y;
	int tmp;
	int *sports_mode_order;

	LOGE("MovObjProcess - start");

	jclass src_size = env->GetObjectClass(size);
	jfieldID id_srcW = env->GetFieldID(src_size, "width", "I");
	jint sx = env->GetIntField(size,id_srcW);
	jfieldID id_srcH = env->GetFieldID(src_size, "height", "I");
	jint sy = env->GetIntField(size,id_srcH);

	OutPic = (Uint8 *)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	crop = (int*)env->GetIntArrayElements(jcrop, NULL);

	int use_sports_mode = 1;
	int fastmode = 0;
	if(jsports_order != NULL)
		sports_mode_order = (int*)env->GetIntArrayElements(jsports_order, NULL);
	else
	{
		sports_mode_order = NULL;
		use_sports_mode = 0;
	}


	if(jlayout != NULL)
		layout = (Uint8 *)env->GetByteArrayElements(jlayout, NULL);
	else
	{
		layout = (Uint8 *) malloc ((sx/ratio)*(sy/ratio)*sizeof(Uint8));
		memset (layout, -1, (sx/ratio)*(sy/ratio)*sizeof(Uint8));
	}

	if(jbase_area != NULL)
		base_area = (int*)env->GetIntArrayElements(jbase_area, NULL);
	else
	{
		base_area = (int *) malloc(4*sizeof(int));
		memset(base_area, 0, 4*sizeof(int));
	}

	MovObj_Process(&instance, inputFrame, OutPic,
					layout, NULL, // used in manual removal mode
					256, // gain (sensor specific, might need adjustment)
					sx, sy, nFrames,
					sensitivity, // sensitivity
					minSize,	// smallest detectable object size
					5, // do not detect weak objects (with little movement or low contrast)
					ghosting,// prevent ghosting
					0, // no extra border around detected object
					use_sports_mode, sports_mode_order, // sports-mode parameters
					0, // default maximum displacement between input frames
					0, // do not use post-filter
					2,	// re-scale output (2 = keep aspect ratio)
					&base_area[0], &base_area[1], &base_area[2], &base_area[3], // area covered in base frame by Layout
					&crop[0], &crop[1], &crop[2], &crop[3], &crop[4],
					ratio == 16 ? 1:0, // use fast mode (recommended for sensors >= 8Mpix)
					0);

	if(jlayout != NULL)
	{
		env->ReleaseByteArrayElements(jlayout, (jbyte*)layout, JNI_COMMIT);
	}
	else
		free(layout);

	if(jbase_area != NULL)
		env->ReleaseIntArrayElements(jbase_area, (jint*)base_area, JNI_COMMIT);
	else
		free(base_area);

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, JNI_COMMIT);

	if(jsports_order != NULL)
		env->ReleaseIntArrayElements(jsports_order, (jint*)sports_mode_order, JNI_ABORT);

	LOGE("MovObjProcess - end");

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
