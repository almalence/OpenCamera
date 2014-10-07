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

#include <jni.h>
#include <stdlib.h>
#include <stdio.h>
#include <android/log.h>

#include "almashot.h"
#include "panorama.h"

#define LOG_ON
#ifdef LOG_ON
#define LOG_TAG "PANO_JNI"
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

static int almashot_inited = 0;
static void* instance = NULL;

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_panorama_AlmashotPanorama_initialize
(
	JNIEnv* env,
	jclass
)
{
	char status[1024];
	int err = 0;

	if (almashot_inited == 0)
	{
		err = AlmaShot_Initialize(0);
		if (err == 0)
			almashot_inited = 1;
	}

	instance = NULL;

	return err;
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_panorama_AlmashotPanorama_release
(
	JNIEnv*,
	jclass
)
{
	int i;
	int result = 0;

	if (instance != NULL)
	{
		Pano_Cancel(instance);
		instance = NULL;
	}


	if (almashot_inited == 1)
	{
		result = AlmaShot_Release();

		almashot_inited = 0;
	}

	return result;
}

extern "C" JNIEXPORT jintArray JNICALL Java_com_almalence_plugins_processing_panorama_AlmashotPanorama_process
(
	JNIEnv* env,
	jclass,
	jint width,
	jint height,
	jintArray jframes,
	jobjectArray jtrs,
	jint cameraFOV,
	jboolean useAll,
	jboolean freeInput
)
{
	const int nframesCount = env->GetArrayLength(jframes);
	int fx0[nframesCount];
	int fy0[nframesCount];
	int fsx[nframesCount];
	int fsy[nframesCount];
	int nFramesSelected;
	Uint8* framesSelected[nframesCount];
	Uint8* framesRelevant[nframesCount];
	int crop[4];
	Uint8* out;
	int out_width;
	int out_height;
	float trs[nframesCount][3][3];
	int status;

	{
		const int trs_count = env->GetArrayLength(jtrs);

		for (int i = 0; i < trs_count; i++)
		{
			jobjectArray mtx = (jobjectArray)env->GetObjectArrayElement(jtrs, i);

			for (int cy = 0; cy < 3; cy++)
			{
				jfloatArray mtx_row = (jfloatArray)env->GetObjectArrayElement(mtx, cy);
				jfloat* mtx_row_elements = env->GetFloatArrayElements(mtx_row, 0);

				for (int cx = 0; cx < 3; cx++)
				{
					trs[i][cy][cx] = mtx_row_elements[cx];
				}

				env->ReleaseFloatArrayElements(mtx_row, mtx_row_elements, JNI_ABORT);
				env->DeleteLocalRef(mtx_row);
			}

			env->DeleteLocalRef(mtx);

			//__android_log_print(ANDROID_LOG_ERROR, "Almalence", "MTX:  %2.2f  %2.2f  %2.2f",
			//		trs[i][0][0], trs[i][0][1], trs[i][0][2]);
			//__android_log_print(ANDROID_LOG_ERROR, "Almalence", "MTX:  %2.2f  %2.2f  %2.2f",
			//		trs[i][1][0], trs[i][1][1], trs[i][1][2]);
			//__android_log_print(ANDROID_LOG_ERROR, "Almalence", "MTX:  %2.2f  %2.2f  %2.2f",
			//		trs[i][2][0], trs[i][2][1], trs[i][2][2]);
		}
	}

	jint* nframes = env->GetIntArrayElements(jframes, 0);

	//__android_log_print(ANDROID_LOG_ERROR, "Almalence", "input w x h:  %d x %d",
	//		width, height);

#if 0 // debug dump
		FILE *f = fopen("/sdcard/pano/pano_dims_LR.txt", "wb");
		fprintf(f, "float pano_offs[%d][3][3] =\n{\t// Fov = %d\n", nframesCount, cameraFOV);
		for (int i=0; i<nframesCount; ++i)
		{
			fprintf(f, "\t{ ");
			for (int j=0; j<3; ++j)
			{
				fprintf(f, "{");
				for (int k=0; k<3; ++k)
				{
					fprintf(f, " %3.4f", trs[i][j][k]);
					if (k<2)
						fprintf(f, ",");
				}
				fprintf(f, " }");
				if (j<2)
					fprintf(f, ",");
			}
			if (i<nframesCount-1)
				fprintf(f, " },\n");
			else
				fprintf(f, " }\n");
		}
		fprintf(f, "};\n");
		fclose(f);
#endif
#if 0 // debug dump
		for (int i=0; i<nframesCount; ++i)
		{
			char str[1024];

			sprintf (str, "/sdcard/pano/pi%d.yuv", i);
			FILE *fi = fopen(str, "wb");
			fwrite((Uint8*)nframes[i], width*height*3/2, 1, fi);
			fclose(fi);
		}
#endif

	Pano_PrepareFrames((Uint8**)nframes, width, height, nframesCount, framesSelected,
			trs, framesRelevant, fx0, fy0, fsx, fsy, &nFramesSelected, &out_width, &out_height,
			&crop[0], &crop[1], &crop[2], &crop[3], cameraFOV, 1, 1,
			useAll, freeInput);

	//__android_log_print(ANDROID_LOG_ERROR, "Almalence", "panorama w x h:  %d x %d",
	//		out_width, out_height);

	env->ReleaseIntArrayElements(jframes, nframes, JNI_ABORT);

	//for (int i=0; i<nframesCount; ++i)
	//	free(framesRelevant[i]);

	Pano_Preview(&instance, framesRelevant, NULL, NULL, NULL, 5 * 256, 0, 0, fx0, fy0, fsx, fsy,
			out_width, out_height, nFramesSelected, 0);

	Pano_Preview2(instance, NULL);

	out = NULL;
	status = Pano_Process(instance, &out);

	//FILE* f = fopen("/sdcard/output.yuv", "wb");
	//fwrite(out, out_width * out_height * 3 / 2, 1, f);
	//fclose(f);

	instance = NULL;

	jintArray jresult = env->NewIntArray(1 + 2 + 4 + 1 + nFramesSelected);
	jint* nresult = env->GetIntArrayElements(jresult, 0);

	/*
	out = (Uint8*) malloc(16*16*3/2);
	crop[2] = out_width = 16;
	crop[3] = out_height = 16;
	crop[0] = crop[1] = 0;
	//*/

	int result_cursor = 0;
	nresult[result_cursor++] = (int)out;
	nresult[result_cursor++] = out_width;
	nresult[result_cursor++] = out_height;
	nresult[result_cursor++] = crop[0];
	nresult[result_cursor++] = crop[1];
	nresult[result_cursor++] = crop[2];
	nresult[result_cursor++] = crop[3];
	nresult[result_cursor++] = nFramesSelected;
	for (int i = 0; i < nFramesSelected; i++)
	{
		nresult[result_cursor++] = (int)framesSelected[i];
	}


	env->ReleaseIntArrayElements(jresult, nresult, 0);

	return jresult;
}
