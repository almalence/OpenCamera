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

#include <sys/stat.h>
#include <dirent.h>

static int nextdirnum(const char *path) {
	DIR *dir = opendir(path);
	int max = -1;
	if (dir) {
		struct dirent *ent;
		while ((ent = readdir(dir))) {
			char c;	int a;
			if (sscanf(ent->d_name, "%i%c", &a, &c) == 1)
				if (max < a) max = a;	
		}
		closedir(dir);
	}
	return max + 1;
}

#include "../libjpeg/jpeglib.h"

static int write_jpeg(void *image, void *imgUV, int stride,
		int width, int height, const char *fn, int quality) {
  struct jpeg_compress_struct cinfo;
  struct jpeg_error_mgr jerr;
  FILE *fo; int i, wa = (width+15)&-16;
	JSAMPROW ptrY[16], ptrU[8], ptrV[8];
	JSAMPARRAY data[3] = { ptrY, ptrU, ptrV };
	JSAMPROW buf;

	if (!imgUV) imgUV = (char*)image + width * height, stride = width;

	if ((width | height) & 1) return 0;
	if (!(buf = (JSAMPROW)malloc(24 * wa))) return 0;
  if (!(fo = fopen(fn, "wb"))) { free(buf); return 0; }

  cinfo.err = jpeg_std_error(&jerr);
  jpeg_create_compress(&cinfo);
  jpeg_stdio_dest(&cinfo, fo);

  cinfo.image_width = width;
  cinfo.image_height = height;
  cinfo.input_components = 3;
  jpeg_set_defaults(&cinfo);
	// jpeg_set_colorspace(&cinfo, JCS_YCbCr); 
  cinfo.in_color_space = JCS_YCbCr;
	cinfo.raw_data_in = TRUE;
	cinfo.comp_info[0].h_samp_factor = 2;
	cinfo.comp_info[0].v_samp_factor = 2;
	cinfo.comp_info[1].h_samp_factor = 1;
	cinfo.comp_info[1].v_samp_factor = 1;
	cinfo.comp_info[2].h_samp_factor = 1;
	cinfo.comp_info[2].v_samp_factor = 1;

  jpeg_set_quality(&cinfo, quality, TRUE /* limit to baseline-JPEG values */);
  jpeg_start_compress(&cinfo, TRUE);

	for (i = 0; i < 16; i++) ptrY[i] = buf + i * wa;
	for (i = 0; i < 8; i++)
		ptrV[i] = (ptrU[i] = buf + (i + 16) * wa) + (wa >> 1);

	for (i = 0; i < height; i += 16) {
		int j, n = (height - i);
		if (n > 16) n = 16;
		for (j = 0; j < n; j++) {
			memcpy(ptrY[j], (JSAMPROW)image + (i + j) * stride, width);
			if (width < wa) memset(ptrY[j]+width, ptrY[j][width-1], wa - width);
			if (j&1) {
				int k; JSAMPROW d = ptrU[j>>1], s = (JSAMPROW)imgUV + ((i+j)>>1) * stride;
				for (k = 0; k < (width>>1); k++) d[k] = s[k*2+1], d[k + (wa>>1)] = s[k*2];
				for ( ; k < (wa>>1); k++) d[k] = d[(width>>1)-1], d[k + (wa>>1)] = d[(wa>>1) + (width>>1)-1];
			}
		}
		for ( ; j < 15; j += 2) {
			ptrY[j] = ptrY[n-1]; ptrY[j+1] = ptrY[n-1];
			ptrU[j>>1] = ptrU[(n>>1)-1]; ptrV[j>>1] = ptrV[(n>>1)-1];
		}
		jpeg_write_raw_data(&cinfo, data, 16);
  }
  jpeg_finish_compress(&cinfo);
  jpeg_destroy_compress(&cinfo);
  fclose(fo);
	free(buf);
	return 1;
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
	jboolean freeInput,
	jfloat intersection
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
	char path[128], *fn;
	{
		int i;

		mkdir("/sdcard/pano", 0770);
		i = nextdirnum("/sdcard/pano");
		fn = path + sprintf(path, "/sdcard/pano/%i", i);
		mkdir(path, 0770);
		*fn++ = '/';
		strcpy(fn, "pano.txt");

		FILE *fo = fopen(path, "wb");
		if (fo) {
			fprintf(fo, "#pano (w = %i, h = %i, n = %i, fov = %i, overlap = %.2f)\n\n",
					width, height, nframesCount, cameraFOV, intersection);
			for (i = 0; i < nframesCount; i++)
#define FMT "% 10.4f"
				fprintf(fo, FMT","FMT","FMT";"FMT","FMT","FMT";"FMT","FMT","FMT";\n",
#undef FMT
						trs[i][0][0], trs[i][0][1], trs[i][0][2],
						trs[i][1][0], trs[i][1][1], trs[i][1][2],
						trs[i][2][0], trs[i][2][1], trs[i][2][2]);
			fclose(fo);
		}

		for (i = 0; i < nframesCount; i++) {
			sprintf(fn, "pano%i.yuv", i);
			fo = fopen(path, "wb");
			if (fo) {
				fwrite((void*)nframes[i], 1, width*height*3/2, fo);
				fclose(fo);
			}
		}
		strcpy(fn, "pano.jpg");
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

	Pano_Preview(&instance, framesRelevant, NULL, NULL, NULL, 5 * 256, 0, 0, intersection,
			fx0, fy0, fsx, fsy, out_width, out_height, nFramesSelected, 0);

	Pano_Preview2(instance, NULL);

	out = NULL;
	status = Pano_Process(instance, &out);

#if 0 // result for debug dump
	write_jpeg(out, 0, 0, out_width, out_height, path, 100);
	if (0) {
		strcpy(fn, "crop.jpg");
		unsigned char *outUV = out + out_width * out_height;
		int st_x = crop[0], st_y = crop[1], width = crop[2], height = crop[3];
		width = (width + (st_x & 1) + 1) & -2; st_x &= -2;
		height = (height + (st_y & 1) + 1) & -2; st_y &= -2;
		write_jpeg(
			out + st_y * out_width + st_x,
			outUV + (st_y>>1) * out_width + st_x,
			out_width, width, height, path, 100);
	}
#endif

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
