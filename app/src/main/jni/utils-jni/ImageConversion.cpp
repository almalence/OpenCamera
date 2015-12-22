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
#include <string.h>
#include <stdio.h>
#include <jni.h>
#include <android/log.h>

#include "ImageConversionUtils.h"

#define BMP_R(p)	((p) & 0xFF)
#define BMP_G(p)	(((p)>>8) & 0xFF)
#define BMP_B(p)	(((p)>>16)& 0xFF)

jbyte NightGamma[511] =
{
	0,1,2,3,4,5,6,6,7,8,9,10,11,12,13,13,14,15,16,17,18,19,19,20,21,22,23,24,24,25,26,27,28,29,29,30,31,
	32,33,33,34,35,36,37,38,38,39,40,41,42,42,43,44,45,46,46,47,48,49,50,50,51,52,53,54,54,55,56,57,58,58,59,60,61,61,62,63,
	64,65,65,66,67,68,68,69,70,71,71,72,73,74,75,75,76,77,78,78,79,80,81,81,82,83,84,84,85,86,87,87,88,89,90,90,91,92,93,93,94,95,
	96,96,97,98,98,99,100,101,101,102,103,104,104,105,106,106,107,108,109,109,110,111,111,112,113,113,114,115,116,116,117,118,118,
	119,120,120,121,122,123,123,124,125,125,126,127,127,128,129,129,130,131,131,132,133,133,134,135,135,136,137,137,138,139,139,140,
	141,141,142,143,143,144,144,145,146,146,147,148,148,149,150,150,151,151,152,153,153,154,155,155,156,156,157,158,158,159,159,160,
	161,161,162,163,163,164,164,165,165,166,167,167,168,168,169,170,170,171,171,172,172,173,174,174,175,175,176,176,177,178,178,179,
	179,180,180,181,181,182,182,183,184,184,185,185,186,186,187,187,188,188,189,189,190,190,191,191,192,192,193,193,194,194,195,195,
	196,196,197,197,198,198,199,199,200,200,201,201,202,202,203,203,204,204,205,205,205,206,206,207,207,208,208,209,209,210,210,210,
	211,211,212,212,213,213,213,214,214,215,215,215,216,216,217,217,218,218,218,219,219,220,220,220,221,221,221,222,222,223,223,223,
	224,224,224,225,225,226,226,226,227,227,227,228,228,228,229,229,229,230,230,230,231,231,231,232,232,232,233,233,233,234,234,234,
	234,235,235,235,236,236,236,237,237,237,237,238,238,238,238,239,239,239,240,240,240,240,241,241,241,241,242,242,242,242,243,243,
	243,243,244,244,244,244,244,245,245,245,245,245,246,246,246,246,246,247,247,247,247,247,248,248,248,248,248,249,249,249,249,249,
	249,250,250,250,250,250,250,250,251,251,251,251,251,251,251,252,252,252,252,252,252,252,252,252,253,253,253,253,253,253,253,253,
	253,253,254,254,254,254,254,254,254,254,254,254,254,254,254,254,254,254,255,255,255,255,255,255,255,255,255,255,255,255,255,255,
	255,255,255,255,255,255,255
};

// summation with tone-curve applied after
// used in night-mode viewfinder
extern "C" JNIEXPORT void JNICALL Java_com_almalence_util_ImageConversion_sumByteArraysNV21
(
	JNIEnv* env,
	jobject thiz,
	jbyteArray data1,
	jbyteArray data2,
	jbyteArray out,
	jint width,
	jint height
)
{
	jbyte* frame1 = env->GetByteArrayElements(data1, 0);
	jbyte* frame2 = env->GetByteArrayElements(data2, 0);
	jbyte* frame_res = env->GetByteArrayElements(out, 0);

	jint frameSize = width * height;

	for (jint j = 0, yp = 0; j < height; j++) {

		jint uvp1 = frameSize + (j >> 1) * width, u1 = 0, v1 = 0;
		jint uvp2 = frameSize + (j >> 1) * width, u2 = 0, v2 = 0;

	  for (jint i = 0; i < width; i++, yp++) {
		jint y1 = (0xff & ((jint) frame1[yp]));
		jint y2 = (0xff & ((jint) frame2[yp]));


		if ((i & 1) == 0) {
		  v1 = (0xff & frame1[uvp1++]) - 128;
		  u1 = (0xff & frame1[uvp1++]) - 128;
		  v2 = (0xff & frame2[uvp2++]) - 128;
		  u2 = (0xff & frame2[uvp2++]) - 128;


		  jint v0 = (v1+v2)/2;
		  jint u0 = (u1+u2)/2;
		  frame_res[uvp1-1] = (jbyte)((u0+128));
		  frame_res[uvp1-2] = (jbyte)((v0+128));
		}
		//jint y0 = (y1+y2) < 255 ? (y1+y2) : 255;
		frame_res[yp] = NightGamma[y1+y2]; // (jbyte)(y0);
	  }
	}

	env->ReleaseByteArrayElements(out, frame_res, 0);
	env->ReleaseByteArrayElements(data2, frame2, 0);
	env->ReleaseByteArrayElements(data1, frame1, 0);
}


extern "C" JNIEXPORT void JNICALL Java_com_almalence_util_ImageConversion_TransformNV21
(
	JNIEnv* env,
	jobject thiz,
	jbyteArray InPic,
	jbyteArray OutPic,
	int sx,
	int sy,
	int flipLR,
	int flipUD,
	int rotate90
)
{
	jbyte * InNV21 = env->GetByteArrayElements(InPic, 0);
	jbyte * OutNV21;

	if (OutPic != InPic) OutNV21 = env->GetByteArrayElements(OutPic, 0);
		else OutNV21 = InNV21;

	TransformNV21((unsigned char*)InNV21, (unsigned char*)OutNV21, sx, sy, NULL, flipLR, flipUD, rotate90);

	if (OutPic != InPic)
		env->ReleaseByteArrayElements(OutPic, OutNV21, JNI_ABORT);
	env->ReleaseByteArrayElements(InPic, InNV21, JNI_ABORT);

}

extern "C" JNIEXPORT void JNICALL Java_com_almalence_util_ImageConversion_TransformNV21N
(
	JNIEnv* env,
	jobject thiz,
	int InPic,
	int OutPic,
	int sx,
	int sy,
	int flipLR,
	int flipUD,
	int rotate90
)
{
	TransformNV21((unsigned char*)InPic, (unsigned char*)OutPic, sx, sy, NULL, flipLR, flipUD, rotate90);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_util_ImageConversion_JpegConvert
(
	JNIEnv* env,
	jobject thiz,
	jbyteArray jdata,
	jint sx,
	jint sy,
	jboolean jrot,
	jboolean mirror,
	jint rotationDegree
)
{
	int data_length;
	unsigned char *data;

	data_length = env->GetArrayLength(jdata);
	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);

	unsigned char* out = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	if (out != NULL)
	{
		if (JPEG2NV21(out, data, data_length, sx, sy, jrot, mirror, rotationDegree) == 0)
		{
			free(out);
			out = NULL;
		}
	}

	env->ReleaseByteArrayElements(jdata, (jbyte*)data, JNI_ABORT);

	return (jint)out;
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_util_ImageConversion_JpegConvertN
(
	JNIEnv* env,
	jobject thiz,
	jint jpeg,
	jint jpeg_length,
	jint sx,
	jint sy,
	jboolean jrot,
	jboolean mirror,
	jint rotationDegree
)
{
	int data_length;
	unsigned char *data;

	unsigned char* out = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	if (out != NULL)
	{
		if (JPEG2NV21(out, (unsigned char*)jpeg, jpeg_length, sx, sy, jrot, mirror, rotationDegree) == 0)
		{
			free(out);
			out = NULL;
		}
	}

	return (jint)out;
}

extern "C" JNIEXPORT void JNICALL Java_com_almalence_util_ImageConversion_convertNV21toGLN(
		JNIEnv *env, jclass clazz, jint ain, jbyteArray aout, jint width,	jint height, jint outWidth, jint outHeight)
{
	jbyte *cImageOut = env->GetByteArrayElements(aout, 0);

	NV21_to_RGB_scaled_rotated((unsigned char*)ain, width, height, 0, 0, width, height, outWidth, outHeight, 4, (unsigned char*)cImageOut);

	env->ReleaseByteArrayElements(aout, cImageOut, 0);
}

extern "C" JNIEXPORT void JNICALL Java_com_almalence_util_ImageConversion_convertNV21toGL(
		JNIEnv *env, jclass clazz, jbyteArray ain, jbyteArray aout, jint width,	jint height, jint outWidth, jint outHeight)
{
	jbyte *cImageIn = env->GetByteArrayElements(ain, 0);
	jbyte *cImageOut = env->GetByteArrayElements(aout, 0);

	NV21_to_RGB_scaled_rotated((unsigned char*)cImageIn, width, height, 0, 0, width, height, outWidth, outHeight, 5, (unsigned char*)cImageOut);

	env->ReleaseByteArrayElements(ain, cImageIn, 0);
	env->ReleaseByteArrayElements(aout, cImageOut, 0);
}

extern "C" JNIEXPORT void JNICALL Java_com_almalence_util_ImageConversion_resizeJpeg2RGBA(
		JNIEnv *env, jclass clazz,
		jint jpeg,
		jint jpeg_length,
		jbyteArray rgb_out,
		jint inHeight, jint inWidth,
		jint outWidth, jint outHeight,
		jboolean mirror)
{
	unsigned int * data_rgba = (unsigned int*)malloc(inHeight*inWidth*sizeof(unsigned int));
	if (data_rgba == NULL)
	{
		__android_log_print(ANDROID_LOG_ERROR, "Almalence", "nativeresizeJpeg2RGBA(): malloc() returned NULL");
		return;
	}

	JPEG2RGBA((unsigned char*)data_rgba, (unsigned char*)jpeg, jpeg_length);

	unsigned char * rgb_bytes = (unsigned char*)env->GetByteArrayElements(rgb_out, 0);

	const int tripleHeight = (outHeight - 1) * 4;
	int yoffset = tripleHeight;
	int cr, cb, cg;
	int offset;

	// down-scaling with area averaging gives a higher-quality result comparing to skia scaling
	int navg = max(1, 3*max(inWidth/outWidth, inHeight/outHeight)/2);
	int norm = 65536/(navg*navg);

	for (int i = 0; i < outHeight; i++)
	{
		offset = yoffset;

		int ys = i*inHeight/outHeight;
		int ye = min(ys+navg, inHeight);

		for (int j = 0; j < outWidth; j++)
		{
			int xs = j*inWidth/outWidth;
			int xe = min(xs+navg, inWidth);

			cr = cb = cg = 0;
			for (int ii=ys; ii<ye; ++ii)
				for (int jj=xs; jj<xe; ++jj)
				{
					cr += BMP_R(data_rgba[ii * inWidth + jj]);
					cg += BMP_G(data_rgba[ii * inWidth + jj]);
					cb += BMP_B(data_rgba[ii * inWidth + jj]);
				}

			cr = norm*cr/65536;
			cg = norm*cg/65536;
			cb = norm*cb/65536;

			rgb_bytes[offset++] = cr;
			rgb_bytes[offset++] = cg;
			rgb_bytes[offset++] = cb;
			rgb_bytes[offset++] = 255;

			offset += tripleHeight;
		}

		yoffset -= 4;
	}

	free (data_rgba);

	addRoundCornersRGBA8888(rgb_bytes, outWidth, outHeight);

	if (mirror)
	{
		TransformPlane32bit((unsigned int*)rgb_bytes, (unsigned int*)rgb_bytes, outWidth, outHeight, 0, 1, 0);
	}

	env->ReleaseByteArrayElements(rgb_out, (jbyte*)rgb_bytes, JNI_COMMIT);
}

extern "C" JNIEXPORT void JNICALL Java_com_almalence_util_ImageConversion_addCornersRGBA8888(JNIEnv* env, jclass,
		jbyteArray rgb_out, jint outWidth, jint outHeight)
{
	unsigned char *rgb_bytes = (unsigned char *)env->GetByteArrayElements(rgb_out, 0);
	addRoundCornersRGBA8888(rgb_bytes, outWidth, outHeight);
	env->ReleaseByteArrayElements(rgb_out, (jbyte*)rgb_bytes, JNI_COMMIT);
}

extern "C" JNIEXPORT jintArray JNICALL Java_com_almalence_util_HeapUtil_getMemoryInfo(JNIEnv* env, jclass)
{
	FILE *f;
	char dummy[1024];
	int MbInfo[2];

	// the fields we want
	unsigned long curr_mem_used;
	unsigned long curr_mem_free;
	unsigned long curr_mem_buffers;
	unsigned long curr_mem_cached;

	// 'file' stat seems to give the most reliable results
	f = fopen ("/proc/meminfo", "r");
	if (f==NULL) return 0;
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_used, dummy);
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_free, dummy);
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_buffers, dummy);
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_cached, dummy);
	fclose(f);

	MbInfo[0] = curr_mem_used / 1024;
	MbInfo[1] = (curr_mem_free + curr_mem_cached) / 1024;

	//LOGI ("memory used: %ld  free: %ld", MbInfo[0], MbInfo[1]);

	jintArray memInfo = env->NewIntArray(2);
    if(memInfo)
        env->SetIntArrayRegion(memInfo, 0, 2, (jint*) MbInfo);

    return memInfo;
}
