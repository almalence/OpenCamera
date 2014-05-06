#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include <setjmp.h>
#include "jpeglib.h"


#include "almashot.h"
#include "filters.h"
#include "dro.h"


// --------------------------------------------- color conversion

#define	CLIP8(x)	( (x) & (-256) ? ( (x)<0 ? 0 : 255 ) : (x) )

#define CSC_R(Y,V)			CLIP8((128*(Y)+176*((V)-128)) >> 7 )
#define CSC_G(Y,U,V)		CLIP8((128*(Y)-89*((V)-128)-43*((U)-128)) >> 7 )
#define CSC_B(Y,U)			CLIP8((128*(Y)+222*((U)-128)) >> 7 )


// --------------------------------------------- still-image

static unsigned char *yuv[MAX_FRAMES] = {NULL};
static void *instance = NULL;
static int almashot_inited = 0;


// --------------------------------------------- jpeg stuff

struct my_error_mgr {
  struct jpeg_error_mgr pub;	/* "public" fields */
  jmp_buf setjmp_buffer;	/* for return to caller */
};
typedef struct my_error_mgr * my_error_ptr;

METHODDEF(void) my_error_exit (j_common_ptr cinfo)
{
  /* cinfo->err really points to a my_error_mgr struct, so coerce pointer */
  my_error_ptr myerr = (my_error_ptr) cinfo->err;

  /* Always display the message. */
  /* We could postpone this until after returning, if we chose. */
  (*cinfo->err->output_message) (cinfo);

  /* Return control to the setjmp point */
  longjmp(myerr->setjmp_buffer, 1);
}

void decodeFromJpeg(unsigned char *jpegdata, int jpeglen, int idx, int sx, int sy)
{
	int i, y;
	int inlen;

	struct jpeg_decompress_struct cinfo;
	struct my_error_mgr jerr;
	JSAMPARRAY scanline;
	unsigned char  *wline;
	unsigned char *y_buffer;
	unsigned char *cbcr_buffer;

	Uint8* dst=yuv[idx];

	cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = my_error_exit;
    /* Establish the setjmp return context for my_error_exit to use. */
    if (setjmp(jerr.setjmp_buffer))
    {
        jpeg_destroy_decompress(&cinfo);
        return;
    }
	jpeg_create_decompress(&cinfo);

	jpeg_mem_src(&cinfo, jpegdata, jpeglen);
	(void) jpeg_read_header(&cinfo, TRUE);
	//cinfo.raw_data_out = TRUE;
    cinfo.out_color_space = JCS_YCbCr;

	(void) jpeg_start_decompress(&cinfo);

	y_buffer = dst;
	cbcr_buffer = y_buffer + cinfo.output_width * cinfo.output_height;
	scanline = (*cinfo.mem->alloc_sarray)((j_common_ptr) &cinfo, JPOOL_IMAGE, cinfo.output_width * cinfo.output_components, 1);
	wline = scanline[0];

	while (cinfo.output_scanline < cinfo.output_height)
	{
		jpeg_read_scanlines(&cinfo, scanline, (JDIMENSION)1);

		for (i = 0; i < cinfo.output_width; i++)
			y_buffer[i] = wline[i*3];

		y_buffer += cinfo.output_width;

		if (y++ & 1)
		{
			for (int i = 0; i < cinfo.output_width; i+=2)
			{
				cbcr_buffer[i] = wline[(i*3) + 2];		// V
				cbcr_buffer[i + 1] = wline[(i*3) + 1];	// U
			}
			cbcr_buffer += cinfo.output_width;
		}
	}

	(void) jpeg_finish_decompress(&cinfo);
	jpeg_destroy_decompress(&cinfo);
}


void mem_usage(long *mem_used, long *mem_free)
{
	FILE *f;
	char dummy[1024];

	// the fields we want
	unsigned long curr_mem_used;
	unsigned long curr_mem_free;
	unsigned long curr_mem_buffers;
	unsigned long curr_mem_cached;

	*mem_used = 0;
	*mem_free = 0;

	// 'file' stat seems to give the most reliable results
	f = fopen ("/proc/meminfo", "r");
	if (f==NULL) return;
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_used, dummy);
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_free, dummy);
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_buffers, dummy);
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_cached, dummy);
	fclose(f);

	*mem_used = curr_mem_used / 1024;
	*mem_free = (curr_mem_free + curr_mem_cached) / 1024;
}


#ifndef max
#define max(a,b) ({typeof(a) _a = (a); typeof(b) _b = (b); _a > _b ? _a : _b; })
#define min(a,b) ({typeof(a) _a = (a); typeof(b) _b = (b); _a < _b ? _a : _b; })
#endif

inline void NV21_to_RGB_rotated(unsigned char *pY, int width, int height, int *crop, int outWidth, int outHeight, int stride, unsigned char *buffer)
{
	unsigned char *pUV = pY + width * height;

	int i, j, is, js;
	int nR, nG, nB;
	int nY, nU, nV;
	unsigned char *out = buffer;
	int offset;
	const int tripleHeight = (outHeight - 1) * stride;
	const float scaleWidth = (float)crop[2] / outWidth;
	const float scaleHeight = (float)crop[3] / outHeight;
	int yoffset = tripleHeight;

	pY += crop[0] + crop[1] * width;
	pUV += crop[0]-(crop[0]&1) + (crop[1]/2) * width;

	for (i = 0; i < outHeight; i++)
	{
		offset = yoffset;

		is = (int)(i * scaleHeight);

		for (j = 0; j < outWidth; j++)
		{
			js = (int)(j * scaleWidth);

			nY = *(pY + is * width + js);
			nU = *(pUV + (is / 2) * width + 2 * (js / 2) + 1);
			nV = *(pUV + (is / 2) * width + 2 * (js / 2));

			if (stride == 4)
			{
				out[offset++] = CSC_R(nY, nV);
				out[offset++] = CSC_G(nY, nU, nV);
				out[offset++] = CSC_B(nY, nU);
				out[offset++] = 0xFF;
			}
			else
			{
				out[offset++] = CSC_R(nY, nV);
				out[offset++] = CSC_G(nY, nU, nV);
				out[offset++] = CSC_B(nY, nU);
			}

			offset += tripleHeight;
		}

		yoffset -= stride;
	}
}

inline void NV21_to_RGB(unsigned char *pY, int width, int height, int *crop, int outWidth, int outHeight, int stride, unsigned char *buffer)
{
	unsigned char *pUV = pY + width * height;

	int i, j, is, js;
	int nR, nG, nB;
	int nY, nU, nV;
	unsigned char *out = buffer;
	int offset;
	const float scaleWidth = (float)crop[2] / outWidth;
	const float scaleHeight = (float)crop[3] / outHeight;

	pY += crop[0] + crop[1] * width;
	pUV += crop[0]-(crop[0]&1) + (crop[1]/2) * width;

	for (i = 0; i < outHeight; i++)
	{
		is = (int)i * scaleHeight;

		for (j = 0; j < outWidth; j++)
		{
			offset = (j+i*outWidth)*stride;

			js = (int)j * scaleWidth;

			nY = *(pY + is * width + js);
			nU = *(pUV + (is / 2) * width + 2 * (js / 2) + 1);
			nV = *(pUV + (is / 2) * width + 2 * (js / 2));

			if (stride == 4)
			{
				out[offset++] = CSC_R(nY, nV);
				out[offset++] = CSC_G(nY, nU, nV);
				out[offset++] = CSC_B(nY, nU);
				out[offset++] = 0xFF;
			}
			else
			{
				out[offset++] = CSC_R(nY, nV);
				out[offset++] = CSC_G(nY, nU, nV);
				out[offset++] = CSC_B(nY, nU);
			}
		}
	}
}


void RotateToNV21(Uint8 ** OutPic, int sx, int sy)
{
	Uint8 *OutNV21, *InPic;
	int x,y;

	OutNV21 = (Uint8 *)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

	InPic = *OutPic;

	// failed to allocate - just return non-rotated image
	if (OutNV21)
	{
		for (y=0; y<sy; y+=2)
			for (x=0; x<sx; x+=2)
			{
				// Y
				OutNV21[x*sy + sy-1-y]       = InPic[x+y*sx];
				OutNV21[(x+1)*sy + sy-1-y]   = InPic[x+1+y*sx];
				OutNV21[x*sy + sy-1-y-1]     = InPic[x+(y+1)*sx];
				OutNV21[(x+1)*sy + sy-1-y-1] = InPic[x+1+(y+1)*sx];

				// U
				OutNV21[sx*sy+(x/2)*sy + sy-1-y] = InPic[sx*sy+x+1+(y/2)*sx];

				// V
				OutNV21[sx*sy+(x/2)*sy + sy-1-y-1] = InPic[sx*sy+x+(y/2)*sx];
			}

		free(*OutPic);
		*OutPic = OutNV21;
	}
}

void Preview_YUV2ARGB(Uint8 *pview_yuv, Uint32 *pview, int sx, int sy)
{
	int x, y;

	for (y=0; y<sy; ++y)
		for (x=0; x<sx; ++x)
		{
			int Y = pview_yuv[(y*sx+x)*3];
			int U = pview_yuv[(y*sx+x)*3+1];
			int V = pview_yuv[(y*sx+x)*3+2];
			Uint32 R = CSC_R(Y,V);
			Uint32 G = CSC_G(Y,U,V);
			Uint32 B = CSC_B(Y,U);

			//pview[y*sx+x] =
			pview[x*sy+sy-1-y] =						// rotate 90 degree for portrait layout
					(R<<16) + (G<<8) + B + (255<<24);
		}
}


// -------------------------------------------------------------------------------


extern "C" JNIEXPORT jstring JNICALL Java_com_almalence_plugins_processing_simple_AlmaShotDRO_Initialize
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

	mem_usage(&mem_used, &mem_free);

	sprintf (status, " memory: %ld(%ld) init status: %d\n", mem_free, mem_used, err);
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

#ifdef HAVE_NEON
	//__android_log_print(ANDROID_LOG_INFO, "CameraTest", "Using Neon instructions");
#endif

	// pre-allocate uncompressed yuv buffers
	for (i=0; i<nFrames; ++i)
	{
		//yuv[i] = (unsigned char*)malloc(sx*sy*2);
		yuv[i] = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));
		if (yuv[i]==NULL)
		{
			for (;i>=0;--i)
			{
				free(yuv[i]);
				yuv[i] = NULL;
			}
			return env->NewStringUTF("not enough memory");
		}
	}

	jpeg = (unsigned char**)env->GetIntArrayElements(in, NULL);
	jpeg_length = (int*)env->GetIntArrayElements(in_len, NULL);

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

	#pragma omp parallel for
	for (i=0; i<nFrames; ++i)
	{
		//__android_log_print(ANDROID_LOG_INFO, "CameraTest", "decoding jpeg: %d len: %d", (int)jpeg[i], jpeg_length[i]);

		// decode from jpeg
		decodeFromJpeg(jpeg[i], jpeg_length[i], i, sx, sy);
		// release compressed memory
		free (jpeg[i]);
	}


	env->ReleaseIntArrayElements(in, (jint*)jpeg, JNI_ABORT);
	env->ReleaseIntArrayElements(in_len, (jint*)jpeg_length, JNI_ABORT);

	//sprintf (status, "frames total: %d\nsize0: %d\nsize1: %d\nsize2: %d\n", (int)nFrames, jpeg_length[0], jpeg_length[1], jpeg_length[2]);
	sprintf (status, "frames total: %d\n", (int)nFrames);
	return env->NewStringUTF(status);
}

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_simple_AlmaShotDRO_GetInputFrameNV21
(
	JNIEnv* env,
	jobject thiz,
	jint index,
	jint sx,
	jint sy
)
{
	if(yuv[index] != NULL)
	{
		RotateToNV21(&yuv[index], sx, sy);
		return (jint)yuv[index];
	}
	else return -1;
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

extern "C" JNIEXPORT jint JNICALL Java_com_almalence_plugins_processing_simple_AlmaShotDRO_RotateResult
(
	JNIEnv* env,
	jobject thiz,
	jint inputresult_yuv,
	jint sx,
	jint sy
)
{
	Uint8 *result_yuv = (Uint8*)inputresult_yuv;

	RotateToNV21(&result_yuv, sx, sy);
	return (jint)result_yuv;
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
	jint strongFilter,
	jint pullUV
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
					Dro_ComputeToneTable(hist_loc[x][y], lookup_table[x][y], 1, 0.5, 64, 0.5f, min_limit, max_limit, max_amplify);
				else
					Dro_ComputeToneTable(hist, lookup_table[x][y], 1, 0.5, 64, 0.5f, min_limit, max_limit, max_amplify);
			}


		int res = Dro_ApplyToneTableFilteredNV21(
				yuv,
				result_yuv,
				local_mapping ? NULL:lookup_table[0][0],
				local_mapping ? lookup_table:NULL,
				filterStrength,
				strongFilter,
				pullUV, 5,
				sx, sy);
	}

	return (jint)result_yuv;
}
