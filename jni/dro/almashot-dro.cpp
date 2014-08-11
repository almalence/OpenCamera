#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "ImageConversionUtils.h"

#include "almashot.h"
#include "filters.h"
#include "dro.h"


// --------------------------------------------- still-image

static unsigned char *yuv[MAX_FRAMES] = {NULL};
static void *instance = NULL;
static int almashot_inited = 0;

// -------------------------------------------------------------------------------

extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_simple_AlmaShotDRO_Initialize
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

	sprintf (status, "init status: %d\n", err);
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_simple_AlmaShotDRO_Release
(
	JNIEnv* env,
	jobject thiz
)
{
	int i;

	if (almashot_inited == 1)
	{
		AlmaShot_Release();

		// remove un-freed frames
		for (i=0; i<MAX_FRAMES; ++i)
		{
			if (yuv[i])
			{
				free(yuv[i]);
				yuv[i] = NULL;
			}
		}
		almashot_inited = 0;
	}

	return 0;
}


// this is a very common operation - use ImageConversion jni interface instead (? - need to avoid global yuv array then?)
extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_simple_AlmaShotDRO_ConvertFromJpeg
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

	Uint8 *inp[4];
	int x, y;
	int x0_out, y0_out, w_out, h_out;

	jbyteArray infrms[MAX_FRAMES];

	jpeg = (unsigned char**)env->GetIntArrayElements(in, NULL);
	jpeg_length = (int*)env->GetIntArrayElements(in_len, NULL);

	DecodeAndRotateMultipleJpegs(yuv, jpeg, jpeg_length, sx, sy, nFrames, 0, 0, 0, true);

	/*
	// dump jpeg data
	{
		FILE *f;
		char str[256];

		for (i=0; i<nFrames; ++i)
		{
			sprintf(str, "/sdcard/DCIM/nightDump_%d.jpg", i);
			f = fopen(str,"wb");
			fwrite(jpeg[i], jpeg_length[i], 1, f);
			fclose(f);
		}
	}
	*/

	env->ReleaseIntArrayElements(in, (jint*)jpeg, JNI_ABORT);
	env->ReleaseIntArrayElements(in_len, (jint*)jpeg_length, JNI_ABORT);

	//sprintf (status, "frames total: %d\nsize0: %d\nsize1: %d\nsize2: %d\n", (int)nFrames, jpeg_length[0], jpeg_length[1], jpeg_length[2]);
	sprintf (status, "frames total: %d\n", (int)nFrames);
	return env->NewStringUTF(status);
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_simple_AlmaShotDRO_GetYUVFrame
(
	JNIEnv* env,
	jobject thiz,
	jint index
)
{
	if(yuv[index] != NULL)
	{
		return (jint)yuv[index];
	}
	else return -1;
}


extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_simple_AlmaShotDRO_DroProcess
(
	JNIEnv* env,
	jobject thiz,
	jint inputYUV,
	jint sx,
	jint sy,
	jfloat max_amplify,
	jboolean local_mapping,
	jint filterStrength,
	jint pullUV,
	jfloat dark_noise_pass,
	jfloat gamma
)
{
	int i, x, y;
	Uint8 *result_yuv;
	Uint32 *result;

	Uint32 hist[256];
	Uint32 hist_loc[3][3][256];
	Int32 lookup_table[3][3][256];

	unsigned char* yuv = (unsigned char*)inputYUV;

	result_yuv = (Uint8*)malloc(sx*sy+sx*((sy+1)/2));

	/*
	// dump yuv data
	{
		FILE *f;
		char str[256];

		sprintf(str, "/sdcard/DCIM/Dump.yuv");
		f = fopen(str,"wb");
		fwrite(yuv[0], sx*sy*3/2, 1, f);
		fclose(f);
	}
	//*/


	if (result_yuv)
	{
		Dro_GetHistogramNV21(yuv, hist, local_mapping ? hist_loc:NULL, sx, sy, 1.0f);

		for (y=0; y<(local_mapping ? 3:1); ++y)
			for (x=0; x<(local_mapping ? 3:1); ++x)
			{
				float min_limit[3] = {0.5f,0.5f,0.5f};
				float max_limit[3] = {3.0f,2.0f,2.0f};

				if (local_mapping)
					Dro_ComputeToneTable(hist_loc[x][y], lookup_table[x][y], gamma, 64, 0.5f, min_limit, max_limit, max_amplify);
				else
					Dro_ComputeToneTable(hist, lookup_table[x][y], gamma, 64, 0.5f, min_limit, max_limit, max_amplify);
			}


		int res = Dro_ApplyToneTableFilteredNV21(
				yuv,
				result_yuv,
				local_mapping ? NULL:lookup_table[0][0],
				local_mapping ? lookup_table:NULL,
				filterStrength,
				pullUV, 5,
				dark_noise_pass,
				sx, sy);
	}

	return (jint)result_yuv;
}

jint throwRuntimeException(JNIEnv* env, const char* message)
{
	const char* className = "java/lang/RuntimeException";

	jclass exClass = exClass = env->FindClass(className);

	return env->ThrowNew(exClass, message);
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_video_RealtimeDRO_initialize
(
	JNIEnv* env,
	jobject thiz,
	jint output_width,
	jint output_height
)
{
	void* fi;

	const int result = Dro_StreamingInitialize(&fi, output_width, output_height);

	if (result != ALMA_ALL_OK)
	{
		throwRuntimeException(env, "Native function Dro_StreamingInitialize() failed.");
	}

	return (jint)fi;
}


extern "C" JNIEXPORT void JNICALL Java_com_almalence_plugins_capture_video_RealtimeDRO_render
(
	JNIEnv* env,
	jobject thiz,
	jint instance,
	jint texture_in,
	jfloatArray jmtx,
	jint sx,
	jint sy,
	jboolean filter,
	jboolean local_mapping,
	jfloat  max_amplify,
	jboolean force_update,
    jint uv_desat,
    jint dark_uv_desat,
    jfloat dark_noise_pass,
    jfloat mix_factor,
    jfloat gamma,				// default = 0.5
    jfloat max_black_level,		// default = 16
    jfloat black_level_atten,	// default = 0.5
    jfloatArray jmin_limit,			// default = 0.5 0.5 0.5
    jfloatArray jmax_limit,			// default = 3 2 2
	jint texture_out
)
{
	float* mtx = env->GetFloatArrayElements(jmtx, 0);
	float* min_limit = env->GetFloatArrayElements(jmin_limit, 0);
	float* max_limit = env->GetFloatArrayElements(jmax_limit, 0);

    Dro_StreamingRender(
                        (void *)instance,
                        texture_in,
                        mtx,
                        sx,
                        sy,
                        filter,
                        max_amplify,
                        local_mapping,
                        force_update,
                        uv_desat,
                        dark_uv_desat,
                        dark_noise_pass,
                        mix_factor,
                        gamma,
                        max_black_level,
                        black_level_atten,
                        min_limit,
                        max_limit,
                        texture_out
                        );

	env->ReleaseFloatArrayElements(jmtx, mtx, JNI_ABORT);
	env->ReleaseFloatArrayElements(jmin_limit, min_limit, JNI_ABORT);
	env->ReleaseFloatArrayElements(jmax_limit, max_limit, JNI_ABORT);
}


extern "C" JNIEXPORT void JNICALL Java_com_almalence_plugins_capture_video_RealtimeDRO_release
(
	JNIEnv* env,
	jobject thiz,
	jint instance
)
{
	const int result = Dro_StreamingRelease((void*)instance);

	if (result != ALMA_ALL_OK)
	{
		throwRuntimeException(env, "Native function Dro_StreamingRelease() failed.");
	}
}
