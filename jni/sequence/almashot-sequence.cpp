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
#define LOG_TAG "CLRShot"
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


#define CSC_Y(R,G,B)		((77*(R)+150*(G)+29*(B)) >> 8 )
#define CSC_U(R,G,B)		(((-43*(R)-85*(G)+128*(B)) >> 8) +128)
#define CSC_V(R,G,B)		(((128*(R)-107*(G)-21*(B)) >> 8) +128)

#define BMP_R(p)	((p) & 0xFF)
#define BMP_G(p)	(((p)>>8) & 0xFF)
#define BMP_B(p)	(((p)>>16)& 0xFF)

#define CLIP(x) (((x) < 0) ? 0 : (((x) > 255) ? 255 : (x)))

void NV21toARGB(
		unsigned char *pY,
		int width,
		int height,
		int x0,
		int y0,
		int wCrop,
		int hCrop,
		int outWidth,
		int outHeight,
		unsigned char *buffer)
{
    unsigned char *pUV = pY + width * height + (x0&~1) + (y0/2)*width;
    pY += x0+y0*width;

    int i, j, is, js;
    unsigned char *out = buffer;
    int offset = 0;
    const float scaleWidth = wCrop / outWidth;
    const float scaleHeight = hCrop / outHeight;

    int R, G, B;
    int Y, U, V;

    for (i = 0; i < outHeight; i++)
    {
    	offset = i * outWidth * 4;

        is = i * hCrop / outHeight;

        for (j = 0; j < outWidth; j++)
        {
            js = j * wCrop / outWidth;

            Y = *(pY + is * width + js);
            U = *(pUV + (is / 2) * width + 2*(js / 2));
            V = *(pUV + (is / 2) * width + 2*(js / 2) + 1);

            R = CLIP((128*(Y)+176*((V)-128)) >> 7 );
			B = CLIP((128*(Y)+222*((U)-128)) >> 7 );
			G = CLIP((128*(Y)-89*((V)-128)-43*((U)-128)) >> 7 );

            out[offset++] = (unsigned char) R;
            out[offset++] = (unsigned char) G;
            out[offset++] = (unsigned char) B;
            out[offset++] = 255;
        }
    }
}


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_sequence_AlmaCLRShot_Initialize
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


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_sequence_AlmaCLRShot_Release
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


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_sequence_AlmaCLRShot_ConvertFromJpeg
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
	int *jpeg_length;
	unsigned char * *jpeg;
	char status[1024];
	int isFoundinInput = MAX_MOV_FRAMES;

	int x, y;
	int x0_out, y0_out, w_out, h_out;
/*
	// pre-allocate uncompressed yuv buffers
	for (i=0; i<nFrames; ++i)
	{
		inputFrame[i] = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));
		if (inputFrame[i]==NULL)
		{
			for (;i>=0;--i)
			{
				free(inputFrame[i]);
				inputFrame[i] = NULL;
			}
			LOGD("not enough memory");
			return -1;
		}
	}

	jpeg = (unsigned char**)env->GetIntArrayElements(in, NULL);
	jpeg_length = (int*)env->GetIntArrayElements(in_len, NULL);

	#pragma omp parallel for num_threads(10)
	for (i=0; i<nFrames; ++i)
	{
		// decode from jpeg
		if(decodeFromJpeg(jpeg[i], jpeg_length[i], i, sx, sy) == 0)
		{
			isFoundinInput = i;
			LOGD("JPEG buffer error found in %d frame\n", (int)i);
		}
		// release compressed memory
		free (jpeg[i]);
	}
*/
	jpeg = (unsigned char**)env->GetIntArrayElements(in, NULL);
	jpeg_length = (int*)env->GetIntArrayElements(in_len, NULL);

	isFoundinInput = DecodeAndRotateMultipleJpegs(inputFrame, jpeg, jpeg_length, sx, sy, nFrames, 0, 0, 0, true);

	env->ReleaseIntArrayElements(in, (jint*)jpeg, JNI_ABORT);
	env->ReleaseIntArrayElements(in_len, (jint*)jpeg_length, JNI_ABORT);

	return isFoundinInput;
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_sequence_AlmaCLRShot_addYUVFrames
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
//	int *yuv_length;
	unsigned char * *yuv;
	char status[1024];
	int isFoundinInput = 255;

//	int x, y;
//	int x0_out, y0_out, w_out, h_out;

	yuv = (unsigned char**)env->GetIntArrayElements(in, NULL);
//	yuv_length = (int*)env->GetIntArrayElements(in_len, NULL);

	// pre-allocate uncompressed yuv buffers
//	for (i=0; i<nFrames; ++i)
//	{
//		inputFrame[i] = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));
//
//		if (inputFrame[i]==NULL)
//		{
//			isFoundinInput = i;
//			i--;
//			for (;i>=0;--i)
//			{
//				free(inputFrame[i]);
//				inputFrame[i] = NULL;
//			}
//			break;
//		}
//
//		memcpy(inputFrame[i], yuv[i], yuv_length[i]);
//	}

	for (i=0; i<nFrames; ++i)
		inputFrame[i] = yuv[i];


	env->ReleaseIntArrayElements(in, (jint*)yuv, JNI_ABORT);
//	env->ReleaseIntArrayElements(in_len, (jint*)yuv_length, JNI_ABORT);

	LOGD("frames total: %d\n", (int)nFrames);
	return isFoundinInput;
}


extern "C" JNIEXPORT jintArray JNICALL Java_com_almalence_plugins_processing_sequence_AlmaCLRShot_NV21toARGB
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

	NV21toARGB((Uint8 *)inptr, srcW, srcH, left, top, right - left, bottom - top, dstW, dstH, (Uint8 *)pixels);

	env->ReleaseIntArrayElements(jpixels, (jint*)pixels, 0);

	LOGD("NV21toARGB - end");

	return jpixels;
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_sequence_AlmaCLRShot_MovObjProcess
(
	JNIEnv* env,
	jobject thiz,
	jint nFrames,
	jobject size,
	jint sensitivity,
	jint minSize,
	jintArray jcrop,
	jint ghosting,
	jint ratio,
	jintArray jsports_order
)
{
	Uint8 *layout;
	int *crop;
	int x,y;
	int tmp;
	int *sports_mode_order;
	int base_area[4];

	LOGD("MovObjProcess - start");

	jclass src_size = env->GetObjectClass(size);
	jfieldID id_srcW = env->GetFieldID(src_size, "width", "I");
	jint sx = env->GetIntField(size,id_srcW);
	jfieldID id_srcH = env->GetFieldID(src_size, "height", "I");
	jint sy = env->GetIntField(size,id_srcH);

	OutPic = (Uint8 *)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	crop = (int*)env->GetIntArrayElements(jcrop, NULL);
	sports_mode_order = (int*)env->GetIntArrayElements(jsports_order, NULL);

	int use_sports_mode = 1;
	int fastmode = 0;

	layout = (Uint8 *) malloc ((sx/ratio)*(sy/ratio)*sizeof(Uint8));
	memset (layout, 0, (sx/ratio)*(sy/ratio)*sizeof(Uint8));

	MovObj_Process(&instance, inputFrame, OutPic, layout, NULL, 256, sx, sy, nFrames,
			sensitivity, minSize,	// sensitivity and min size of moving object
			5, ghosting,//ghosting,
			0, // 1, // extraBorder
			use_sports_mode, sports_mode_order,
			0, 0, 2,	// 2 = keep aspect ratio in output
			&base_area[0], &base_area[1], &base_area[2], &base_area[3],
			&crop[0], &crop[1], &crop[2], &crop[3],
			0, ratio == 16 ? 1:0, 0);

	free(layout);

	env->ReleaseIntArrayElements(jcrop, (jint*)crop, 0);
	env->ReleaseIntArrayElements(jsports_order, (jint*)sports_mode_order, JNI_ABORT);

	LOGD("MovObjProcess - end");

	return (jint)OutPic;
}
