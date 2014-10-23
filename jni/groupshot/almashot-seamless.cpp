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

#include "ImageConversionUtils.h"
#include "FaceDetector.h"

#include "almashot.h"
#include "seamless.h"
#include "bestshot.h"

#ifdef LOG_ON
#define LOG_TAG "Seamless"
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


#define MAX_GS_FRAMES	20
#define MAX_FACE_DETECTED	20

static unsigned char *inputFrame[MAX_GS_FRAMES] = {NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL};

static void *instance = NULL;
static int almashot_inited = 0;
static Uint8 *outBuffer = NULL;

// global storage for detected faces data
int   fd_nFaces[MAX_GS_FRAMES];
float fd_confid[MAX_GS_FRAMES][MAX_FACE_DETECTED];
float fd_midx[MAX_GS_FRAMES][MAX_FACE_DETECTED];
float fd_midy[MAX_GS_FRAMES][MAX_FACE_DETECTED];
float fd_eyedist[MAX_GS_FRAMES][MAX_FACE_DETECTED];


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_Initialize
(
	JNIEnv* env,
	jobject thiz
)
{
	LOGD("Initialize - start");
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

	LOGD("Initialize - end");
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_Release
(
	JNIEnv* env,
	jobject,
	jint nFrames
)
{
	LOGD("Release - start");
	int i;

	for (int i=0; i<nFrames; ++i)
	{
		if (inputFrame[i]) free(inputFrame[i]);
		inputFrame[i] = NULL;
	}

	if (instance != NULL)
	{
		Seamless_FreeInstance(instance, 1);
		instance = NULL;
	}

	if (almashot_inited == 1)
	{
		AlmaShot_Release();
		almashot_inited = 0;
	}

	LOGD("Release - end");
	return 0;
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_ConvertAndDetectFacesFromJpegs
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jintArray in_len,
	jint nFrames,
	jint sx,
	jint sy,
	jint fd_sx,
	jint fd_sy,
	jboolean needRotation,
	jboolean cameraMirrored,
	jint rotationDegree
)
{
	int i;
	int *jpeg_length;
	unsigned char * *jpeg;
	char status[1024];
	int isFoundinInput;

	int x, y;
	int x0_out, y0_out, w_out, h_out;

	jpeg = (unsigned char**)env->GetIntArrayElements(in, NULL);
	jpeg_length = (int*)env->GetIntArrayElements(in_len, NULL);

	isFoundinInput = DecodeAndRotateMultipleJpegs(inputFrame, jpeg, jpeg_length, sx, sy, nFrames, needRotation, cameraMirrored, rotationDegree, true);

	// prepare down-scaled gray frames for face detection analisys and detect faces
	#pragma omp parallel for
	for (i=0; i<nFrames; ++i)
	{
		unsigned char * grayFrame = (unsigned char *)malloc(fd_sx*fd_sy);
		if (grayFrame == NULL)
			isFoundinInput = i;
		else
		{
			void *inst;

			if(rotationDegree == 0 || rotationDegree == 180)
				NV21_to_Gray_scaled(inputFrame[i], sx, sy, 0, 0, sx, sy, fd_sx, fd_sy, grayFrame);
			else
				NV21_to_Gray_scaled(inputFrame[i], sy, sx, 0, 0, sy, sx, fd_sx, fd_sy, grayFrame);

			FaceDetector_initialize(&inst, fd_sx, fd_sy, MAX_FACE_DETECTED);
			fd_nFaces[i] = FaceDetector_detect(inst, grayFrame);
			if (fd_nFaces[i] > MAX_FACE_DETECTED)
				fd_nFaces[i] = MAX_FACE_DETECTED;
			for (int f=0; f<fd_nFaces[i]; ++f)
				FaceDetector_get_face(inst, &fd_confid[i][f], &fd_midx[i][f], &fd_midy[i][f], &fd_eyedist[i][f]);
			FaceDetector_destroy(inst);

			free(grayFrame);
		}
	}

	env->ReleaseIntArrayElements(in, (jint*)jpeg, JNI_ABORT);
	env->ReleaseIntArrayElements(in_len, (jint*)jpeg_length, JNI_ABORT);

	LOGD("frames total: %d\n", (int)nFrames);
	return isFoundinInput;
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_DetectFacesFromYUVs
(
	JNIEnv* env,
	jobject thiz,
	jintArray in,
	jintArray in_len,
	jint nFrames,
	jint sx,
	jint sy,
	jint fd_sx,
	jint fd_sy,
	jboolean needRotation,
	jboolean cameraMirrored,
	jint rotationDegree
)
{
	int i;
	int *yuv_length;
	unsigned char * *yuv;
	char status[1024];
	int isFoundinInput = 255;

	int x, y;
	int x0_out, y0_out, w_out, h_out;

	yuv = (unsigned char**)env->GetIntArrayElements(in, NULL);
	yuv_length = (int*)env->GetIntArrayElements(in_len, NULL);

	// pre-allocate uncompressed yuv buffers
	for (i=0; i<nFrames; ++i)
	{
		inputFrame[i] = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

		if (inputFrame[i]==NULL)
		{
			isFoundinInput = i;
			i--;
			for (;i>=0;--i)
			{
				free(inputFrame[i]);
				inputFrame[i] = NULL;
			}
			break;
		}

		memcpy(inputFrame[i], yuv[i], yuv_length[i]);
	}
	//isFoundinInput = DecodeAndRotateMultipleJpegs(inputFrame, jpeg, jpeg_length, sx, sy, nFrames, 0, 0, 0);




	// prepare down-scaled gray frames for face detection analisys and detect faces
	#pragma omp parallel for
	for (i=0; i<nFrames; ++i)
	{
//		//Rotate yuv if needed
//		Uint8* dst;
//
//		if (needRotation || cameraMirrored)
//		//if(rotationDegree != 0 || cameraMirrored)
//			dst = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));
//		else
//			dst = inputFrame[i];

		if (needRotation || cameraMirrored)
			{
				int nRotate = 0;
				int flipUD = 0;
				if(rotationDegree == 180 || rotationDegree == 270)
				{
					cameraMirrored = !cameraMirrored; //used to support 4-side rotation
					flipUD = 1; //used to support 4-side rotation
				}
				if(rotationDegree == 90 || rotationDegree == 270)
					nRotate = 1; //used to support 4-side rotation

				// not sure if it should be 'cameraMirrored, 0,' or '0, cameraMirrored,'
				TransformNV21(yuv[i], inputFrame[i], sx, sy, NULL, cameraMirrored, flipUD, nRotate);
//				free(dst);
			}



		unsigned char * grayFrame = (unsigned char *)malloc(fd_sx*fd_sy);
		if (grayFrame == NULL)
			isFoundinInput = i;
		else
		{
			void *inst;

			if(rotationDegree == 0 || rotationDegree == 180)
				NV21_to_Gray_scaled(inputFrame[i], sx, sy, 0, 0, sx, sy, fd_sx, fd_sy, grayFrame);
			else
				NV21_to_Gray_scaled(inputFrame[i], sy, sx, 0, 0, sy, sx, fd_sx, fd_sy, grayFrame);

			FaceDetector_initialize(&inst, fd_sx, fd_sy, MAX_FACE_DETECTED);
			fd_nFaces[i] = FaceDetector_detect(inst, grayFrame);
			if (fd_nFaces[i] > MAX_FACE_DETECTED)
				fd_nFaces[i] = MAX_FACE_DETECTED;
			for (int f=0; f<fd_nFaces[i]; ++f)
				FaceDetector_get_face(inst, &fd_confid[i][f], &fd_midx[i][f], &fd_midy[i][f], &fd_eyedist[i][f]);
			FaceDetector_destroy(inst);

			free(grayFrame);
		}
	}

	env->ReleaseIntArrayElements(in, (jint*)yuv, JNI_ABORT);
	env->ReleaseIntArrayElements(in_len, (jint*)yuv_length, JNI_ABORT);

	LOGD("frames total: %d\n", (int)nFrames);
	return isFoundinInput;
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_GetFaces
(
	JNIEnv* env,
	jobject thiz,
	jint index,
	jobjectArray faces
)
{
	int f;

	for (f=0; f<fd_nFaces[index]; ++f)
	{
		jobject face = env->GetObjectArrayElement(faces, f);
		jclass class_face = env->GetObjectClass(face);

		jfieldID id_confid = env->GetFieldID(class_face, "mConfidence", "F");
		env->SetFloatField(face, id_confid, fd_confid[index][f]);

		jfieldID id_midx = env->GetFieldID(class_face, "mMidPointX", "F");
		env->SetFloatField(face, id_midx, fd_midx[index][f]);

		jfieldID id_midy = env->GetFieldID(class_face, "mMidPointY", "F");
		env->SetFloatField(face, id_midy, fd_midy[index][f]);

		jfieldID id_eyedist = env->GetFieldID(class_face, "mEyesDist", "F");
		env->SetFloatField(face, id_eyedist, fd_eyedist[index][f]);

		env->DeleteLocalRef(face);
	}

	return fd_nFaces[index];
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_getInputFrame
(
	JNIEnv* env,
	jobject thiz,
	jint index
)
{
	return (jint)inputFrame[index];
}

extern "C" JNIEXPORT jintArray JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_NV21toARGB
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


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_Align
(
	JNIEnv* env,
	jobject thiz,
	jint sx,
	jint sy,
	jint baseFrame,
	jint numOfImg
)
{
	LOGD("Align - start");
	Uint8 *layout;
	int ret;

	LOGD("Align numOfImg: %d", numOfImg);
	LOGD("Align sx: %d  sy: %d", sx, sy);

	ret = Seamless_Align(&instance,
			inputFrame,
			600,						// gain (sensor specific, might need adjustment)
			sx, sy, numOfImg,
			baseFrame,					// base frame (from which to take background)
			0,							// do not use pre-filter
			0,							// do not use post-filter
			2,							// re-scale output
			0
			);

	LOGD("Align - end");
	return ret;
}

extern "C" JNIEXPORT jintArray JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_Preview
(
	JNIEnv* env,
	jobject thiz,
	jint baseFrame,
	jint inWidth,
	jint inHeight,
	jint outWidth,
	jint outHeight,
	jbyteArray jlayout
)
{
	LOGD("Preview - start");

	Uint8 *layout;
	int crop[4];
	int x,y;

	layout = (Uint8 *)env->GetByteArrayElements(jlayout, NULL);

	outBuffer = (Uint8 *)malloc(inWidth * inHeight * 3 / 2);
	LOGD("alloc %d byte yvu memory", inWidth * inHeight * 3 / 2);
	LOGD("base frame = %d", baseFrame);

	Seamless_Process(instance,
			outBuffer,
			layout, NULL,				// prescribed layout
			baseFrame,							// base frame (from which to take background)
			// Set it to non-zero to get very quick stitched result
			2,					// quick method
			&crop[0], &crop[1], &crop[2], &crop[3]);

	env->ReleaseByteArrayElements(jlayout, (jbyte*)layout, JNI_ABORT);

	jintArray jpixels = NULL;
	Uint32 * pixels;

	jpixels = env->NewIntArray(outWidth * outHeight * 4);
	LOGD("alloc %d byte argb memory", outWidth * outHeight * 4);
	pixels = (Uint32 *)env->GetIntArrayElements(jpixels, NULL);

	NV21_to_RGB_scaled(outBuffer, inWidth, inHeight, 0, 0, inWidth, inHeight, outWidth, outHeight, 4, (Uint8 *)pixels);

	free(outBuffer);

	env->ReleaseIntArrayElements(jpixels, (jint*)pixels, 0);

	LOGD("Preview - end");
	return jpixels;
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_groupshot_AlmaShotSeamless_RealView
(
	JNIEnv* env,
	jobject thiz,
	jint width,
	jint height,
	jintArray jcrop,
	jbyteArray jlayout
)
{
	LOGE("RealView - start");

	Uint8 *layout;
	int *crop;

	LOGE("alloc %d byte yvu memory", width * height * 3 / 2);

	Uint8 *outBuffer = (Uint8 *)malloc(width * height * 3 / 2);

	crop = (int*)env->GetIntArrayElements(jcrop, NULL);
	layout = (Uint8 *)env->GetByteArrayElements(jlayout, NULL);

	if (Seamless_Process(instance,
			outBuffer,
			layout, NULL,				// prescribed layout
			0,							// base frame (from which to take background)
			// Set it to non-zero to get very quick stitched result
			0,					// full processing, not a quick method
			&crop[0], &crop[1], &crop[2], &crop[3]) == ALMA_ALL_OK)
	{
		LOGE("RealView - ok");
	}
	else
	{
		LOGE("RealView - error");
	}

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, 0);
	env->ReleaseByteArrayElements(jlayout, (jbyte*)layout, JNI_ABORT);

	LOGD("RealView - end");
	return (jint)outBuffer;
}
