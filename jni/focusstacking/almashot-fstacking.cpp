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
#include "fstacking.h"

#ifdef LOG_ON
#define LOG_TAG "AlmalenceFocusStacking"
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
void __attribute__((constructor)) initialize_openmp() {
}
void __attribute__((destructor)) release_openmp() {
}

#include "fstacking-utils.h"

#define MAX_FFRAMES	8

static int iInputWidth = 0;
static int iInputHeight = 0;
static int iImageAmount = 0;

static unsigned char *inputFrame[MAX_FFRAMES] = { NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL };
static unsigned char *alignedFrame[MAX_FFRAMES] = { NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL };

static float focusDistances[MAX_FFRAMES] = {0, 0, 0, 0, 0, 0, 0, 0};
static unsigned char* focusMap = NULL;

static void *instance = NULL;
static int almashot_inited = 0;
static unsigned char *OutPic = NULL;

/*
 * Initialize Almashot engine. Must be called first
 */
extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_focusstacking_AlmaShotFocusStacking_AlmaShotInitialize
(
	JNIEnv* env,
	jobject thiz
)
{
	LOGE("Initialize - start");
	char status[1024];
	int err=0;

	if (almashot_inited == 0)
	{
		err = AlmaShot_Initialize(1);

		if (err == ALMA_ALL_OK)
			almashot_inited = 1;
	}

	sprintf (status, " init status: %d\n", err);

	LOGE("Initialize - end");
	return env->NewStringUTF(status);
}


/*
 * Initialize focus stacking instance
 * @in - array of pointers to input frames
 * @focusDist - array of focus distance for each input frame (in diopter)
 * @nFrames - number of input frames
 * @sx - width of input frames
 * @sy - height of input frames
 * @fmap - byte array that represents focus areas map (read doc for details)
 */
extern "C" JNIEXPORT jint JNICALL Java_com_almalence_focusstacking_AlmaShotFocusStacking_FStackingInitialize
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jfloatArray focusDist,
	jint nFrames,
	jint sx,
	jint sy,
	jbyteArray fmap
)
{
	LOGE("FStackingInitialize - start");
	int i;
	unsigned char **yuv;
	float* focus;
	char status[1024];

	iInputWidth = sx;
	iInputHeight = sy;
	iImageAmount = nFrames;

	yuv = (unsigned char**)env->GetIntArrayElements(in, NULL);
	int yuv_length = sx*sy+2*((sx+1)/2)*((sy+1)/2);

	focus = (float*)env->GetFloatArrayElements(focusDist, NULL);

	if(fmap != NULL)
		focusMap = (unsigned char *)env->GetByteArrayElements(fmap, NULL);
	else
		focusMap = NULL;

	for (i = 0; i < iImageAmount; ++i)
	{
		inputFrame[i] = yuv[i];
		focusDistances[i] = focus[i];
	}

//	__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "START INPUT SAVE");
//	for (int i=0; i<nFrames; ++i)
//	{
//		char str[256];
//		sprintf(str, "/sdcard/DCIM/fstackingin%02d.yuv", i);
//		FILE *f = fopen (str, "wb");
//		fwrite(inputFrame[i], sx*sy+2*((sx+1)/2)*((sy+1)/2), 1, f);
//		fclose(f);
//	}
//	__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "INPUT SAVED");

	env->ReleaseIntArrayElements(in, (jint*)yuv, JNI_ABORT);
	env->ReleaseFloatArrayElements(focusDist, (jfloat*)focus, JNI_ABORT);

	if(fmap != NULL)
		env->ReleaseByteArrayElements(fmap, (jbyte*)focusMap, JNI_ABORT);

	LOGE("FStacking_Initialize. Frames total: %d\n", (int)nFrames);
	return nFrames;
}


/*
 * Finalize Almashot engine
 */
extern "C" JNIEXPORT jint JNICALL Java_com_almalence_focusstacking_AlmaShotFocusStacking_AlmaShotRelease
(
	JNIEnv* env,
	jobject
)
{
	LOGE("Release - start");

	if (almashot_inited == 1)
	{
		AlmaShot_Release();
		almashot_inited = 0;
	}

	LOGE("Release - end");
	return 0;
}


/*
 * Free FocusStacking instance and memory for the output frame
 */
extern "C" JNIEXPORT jint JNICALL Java_com_almalence_focusstacking_AlmaShotFocusStacking_FStackingFreeInstance
(
	JNIEnv* env,
	jobject
)
{
	LOGE("FreeInstance - start");
	if (OutPic)
	{
//		LOGE("free(OutPic) - start");
		free(OutPic);
		OutPic = NULL;
//		LOGE("free(OutPic) - end");
	}

	if (instance)
	{
		FStacking_FreeInstance(instance, 1);
		instance = NULL;
	}

	LOGE("FreeInstance - end");
	return 0;
}


/*
 * Get one input frame by index
 */
extern "C" JNIEXPORT jint JNICALL Java_com_almalence_focusstacking_AlmaShotFocusStacking_GetInputFrame
(
	JNIEnv* env,
	jobject thiz,
	jint index
)
{
	return (jint)inputFrame[index];
}


/*
 * Get one input frame by index in byteArray format
 * @in - input frames
 * @index - index of desired frame
 * @orientation - desired rotation degrees of frame
 * @mirror - flip frame horizontally
 */
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_almalence_focusstacking_AlmaShotFocusStacking_GetInputByteFrameNative
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jint index,
	jint orientation,
	jboolean mirror
)
{
//	LOGE("GetInputByteFrame - start");

	unsigned char** yuv = (unsigned char**)env->GetIntArrayElements(in, NULL);

	unsigned char *data;
	jbyteArray jdata = env->NewByteArray(iInputWidth * iInputHeight * 2);
	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);

	int flipLeftRight, flipUpDown;
	int rotate90 = orientation == 90 || orientation == 270;
	if (mirror)
		flipUpDown = flipLeftRight = orientation == 180 || orientation == 90;
	else
		flipUpDown = flipLeftRight = orientation == 180 || orientation == 270;

//	LOGE("TransformNV21 - start");
	TransformNV21(yuv[index], data, iInputWidth, iInputHeight, NULL, flipLeftRight, flipUpDown, rotate90);
//	LOGE("TransformNV21 - end");

	env->ReleaseByteArrayElements(jdata, (jbyte*)data, 0);
	env->ReleaseIntArrayElements(in, (jint*)yuv, 0);

//	LOGE("GetInputByteFrame - end");

	return jdata;
}


/*
 * Get pointer to aligned frame by index
 * NOTE: Aligned frames is available only after FStackingProcess method successfully done
 * @index - requested frame index
 */
extern "C" JNIEXPORT jint JNICALL Java_com_almalence_focusstacking_AlmaShotFocusStacking_GetAlignedFrame
(
	JNIEnv* env,
	jobject thiz,
	jint index
)
{
	return (jint)alignedFrame[index];
}


/*
 * Get all aligned frames from Focus stacking instance
 */
extern "C" JNIEXPORT jintArray JNICALL Java_com_almalence_focusstacking_AlmaShotFocusStacking_GetAlignedFrames
(
	JNIEnv* env,
	jobject thiz
)
{
//	LOGE("Get aligned frames - start. iInputWidth = %d, iInputHeight = %d, iImageAmount = %d", iInputWidth, iInputHeight, iImageAmount);
	//Store aligned input frames
	FStacking_GetAlignedFrames(instance,
			alignedFrame,
			iInputWidth,
			iInputHeight,
			iImageAmount);
//	LOGE("Get aligned frames - end");

//	__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "START ALIGNED SAVE");
//	for (int i = 0; i < iImageAmount; ++i)
//	{
//		char str[256];
//		sprintf(str, "/sdcard/DCIM/fstacking_aligned%02d.yuv", i);
//		FILE *f = fopen (str, "wb");
//		fwrite(alignedFrame[i], iInputWidth * iInputHeight + 2 * ((iInputWidth + 1)/2) * ((iInputHeight + 1)/2), 1, f);
//		fclose(f);
//	}
//	__android_log_print(ANDROID_LOG_ERROR, "CameraTest", "ALIGNED SAVCED");

	jintArray jalign = env->NewIntArray(iImageAmount);
	env->SetIntArrayRegion(jalign, 0, iImageAmount, (jint*)alignedFrame);

//	LOGE("Create aligned frames array - end");

	return jalign;
}


/*
 * Main process function. Calls an Almalence's focus stacking feature
 * This method can be called only after FStackingInitialize function
 * @orientation - desired rotation degrees of result frame
 * @mirror - flip result frame horizontally
 * @transform - true if it's need to transform result according above two arguments
 *
 * @return - 'All-In-Focus' frame as byte array in NV21 format
 */
extern "C" JNIEXPORT jbyteArray JNICALL Java_com_almalence_focusstacking_AlmaShotFocusStacking_FStackingProcess
(
	JNIEnv* env,
	jobject thiz,
	jint orientation,
	jboolean mirror,
	jboolean transform
)
{
	FStacking_Align(&instance,
					inputFrame,
					focusMap,
					NULL,
					655,						// gain (sensor specific, might need adjustment)
					iInputWidth, iInputHeight,
					iImageAmount,
					iImageAmount/2,				// base frame (from which to take background)
					256,						// do not use pre-filter
					256,						// do not use post-filter
					1,							// re-scale output
					1
					);

//	for(int i = 0; i < iInputWidth/16*iInputHeight/16; i++)
//		LOGE("focusMap[i] = %d", (int)focusMap[i]);

	LOGE("Focus stacking processing - start");

	unsigned char *OutNV21;
	unsigned char *data;
//	LOGE("JBYTEARRAY - start new size 0");
	jbyteArray jdata = env->NewByteArray(0);
//	LOGE("JBYTEARRAY - end new size 0");

	int x0_out, y0_out, w_out, h_out;

	if(OutPic)
	{
		free(OutPic);
		LOGE("old OutPic free");
	}

//	jbyteArray jdata = env->NewByteArray(iInputWidth * iInputHeight * 2);
//	OutPic = (unsigned char*)env->GetByteArrayElements(jdata, NULL);
	OutPic = (unsigned char *)malloc(iInputWidth * iInputHeight * 2);
//	LOGE("OutPic malloced");

	if (FStacking_Process(instance,
					&OutPic,
					&x0_out, &y0_out,
					&w_out, &h_out) == ALMA_ALL_OK)
	{
		LOGE("Focus stacking processing - ok. x0_out = %d y0_out = %d width = %d height = %d", x0_out, y0_out, w_out, h_out);
	}
	else
	{
		LOGE("Focus stacking processing - error");
		return NULL;
	}


//	char str[256];
//	sprintf(str, "/sdcard/DCIM/fstacking_android_res.yuv");
//	FILE *f = fopen (str, "wb");
//	fwrite(OutPic, sx*sy+2*((sx+1)/2)*((sy+1)/2), 1, f);
//	fclose(f);

	if(transform)
	{
//		LOGE("Focus stacking processing - transform start");
		int flipLeftRight, flipUpDown;
		int rotate90 = orientation == 90 || orientation == 270;
		if (mirror)
			flipUpDown = flipLeftRight = orientation == 180 || orientation == 90;
		else
			flipUpDown = flipLeftRight = orientation == 180 || orientation == 270;

		OutNV21 = OutPic;
		if (rotate90)
			OutNV21 = (unsigned char *)malloc(iInputWidth * iInputHeight * 2);

		TransformNV21(OutPic, OutNV21, iInputWidth, iInputHeight, NULL, flipLeftRight, flipUpDown, rotate90);

		if (rotate90)
		{
			free(OutPic);
			OutPic = OutNV21;
		}
//		LOGE("Focus stacking processing - transform end");
	}

//	LOGE("Focus stacking processing - new ouput byte array start");
	jdata = env->NewByteArray(iInputWidth * iInputHeight * 2);
	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);

//	data = (unsigned char *)malloc(iInputWidth * iInputHeight * 2);
	memcpy (data, OutPic, iInputWidth * iInputHeight * 2);

	env->ReleaseByteArrayElements(jdata, (jbyte*)data, 0);
//	LOGE("Focus stacking processing - new ouput byte array end");
//	free(OutPic);
//	OutPic = NULL;

	return jdata;
//	return (jint)data;
//	return (jint) OutPic;
}
