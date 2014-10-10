#include <stdio.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>
#include "jpeglib.h"
#include "jerror.h"
#include "jinclude.h"
#include "YuvToJpegEncoderMT.h"
#undef ANDROID
#include "jpegint.h"

extern "C" {
#include <setjmp.h>
};

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

#define ImageFormat_NV21 0x11
#define ImageFormat_YUY2 0x14

// This triggers openmp constructors and destructors to be called upon library load/unload
void __attribute__((constructor)) initialize_openmp() {}
void __attribute__((destructor)) release_openmp() {}

/* Expanded data destination object for memory output */
typedef struct {
  struct jpeg_destination_mgr pub; /* public fields */

  unsigned char ** outbuffer;	/* target buffer */
  unsigned long * outsize;
  unsigned char * newbuffer;	/* newly allocated buffer */
  JOCTET * buffer;		/* start of buffer */
  size_t bufsize;
} mem_destination_mgr;

typedef mem_destination_mgr * mem_dest_ptr;

//Error handler
struct mt_jpeg_error_mgr : jpeg_error_mgr {
    jmp_buf fJmpBuf;
    int thread_num;
};

void mt_jpeg_error_exit(j_common_ptr cinfo) {
    mt_jpeg_error_mgr* error = (mt_jpeg_error_mgr*)cinfo->err;

    (*error->output_message) (cinfo);

    // Let the memory manager delete any temp files before we die
    //jpeg_destroy(cinfo);

    longjmp(error->fJmpBuf, error->thread_num);
}

static jmethodID    gOutputStream_writeMethodID;
static jmethodID    gOutputStream_flushMethodID;

static jclass findClass(JNIEnv* env, const char classname[]) {
    jclass clazz = env->FindClass(classname);
    return clazz;
}

static jmethodID getMethodID(JNIEnv* env, jclass clazz,
                                  const char methodname[], const char type[]) {
    jmethodID id = env->GetMethodID(clazz, methodname, type);
    return id;
}

int initStreamMethods(JNIEnv* env) {
    jclass outputStream_Clazz = findClass(env, "java/io/OutputStream");
    gOutputStream_writeMethodID = getMethodID(env, outputStream_Clazz, "write", "([BII)V");
    gOutputStream_flushMethodID = getMethodID(env, outputStream_Clazz, "flush", "()V");

    return 0;
}

void YuvToJpegEncoderMT_setJpegCompressStruct(jpeg_compress_struct* cinfo,
        int width, int height, int quality);
void Yuv420SpToJpegEncoderMT_init(int* strides);
boolean Yuv420SpToJpegEncoderMT_compress(jpeg_compress_struct* cinfo,
        uint8_t* yuv, int* offsets, int start_row, int end_row, boolean call_pass_startup);
void Yuv420SpToJpegEncoderMT_configSamplingFactors(jpeg_compress_struct* cinfo);
void Yuv422IToJpegEncoderMT_init(int* strides);
boolean Yuv422IToJpegEncoderMT_compress(jpeg_compress_struct* cinfo,
        uint8_t* yuv, int* offsets, int start_row, int end_row, boolean call_pass_startup);
void Yuv422IToJpegEncoderMT_configSamplingFactors(jpeg_compress_struct* cinfo);

static int fNumPlanes;
static int* fStrides;
static int fFormat;

int getThreadsNum()
{
	return 4;
}

int getBuffSize(int h, int w, int tn)
{
	return 1024*1024;
}

int getOptHeight(int height, int width, int bufsize, int thread_num)
{
	int thread_height;
	int curr_height;
	int estimated_height;
	int diff;
	int tmp_diff;
	int i;

	estimated_height = (bufsize*5)/width;

	estimated_height += 0xf;

	estimated_height &= ~0xf;

	if (estimated_height*thread_num >= height)
	{
		thread_height = height + thread_num - 1;
		thread_height /= thread_num;
		thread_height += 0xf;
		thread_height &= ~0xf;
		LOGD("thread_height 1 %d", (int)thread_height);
		return thread_height;
	}

	diff = curr_height*thread_num;
	curr_height = estimated_height;
	thread_height = estimated_height;
	for (i = 0; i < 8, curr_height > 16; i++, curr_height -= 16)
	{
		tmp_diff = curr_height - (height % (curr_height*thread_num));
		if (diff > tmp_diff)
		{
			diff = tmp_diff;
			thread_height = curr_height;
			LOGD("curr_height 2 %d", (int)curr_height);
		}

	}

	LOGD( "estimated_height %d thread_height 3 %d", estimated_height, (int)thread_height);

	return thread_height;
}

int YuvToJpegEncoderMT_init(int format, int* strides) {
    // Only ImageFormat.NV21 and ImageFormat.YUY2 are supported
    // for now.
	fFormat = format;
	if (fFormat == ImageFormat_NV21)
		Yuv420SpToJpegEncoderMT_init(strides);
	else if (fFormat == ImageFormat_YUY2)
		Yuv422IToJpegEncoderMT_init(strides);
	else
		return 1;
	return 0;
}

int write_to_stream(JNIEnv* env, jobject jstream, jbyteArray jstorage, int storage_size, uint8_t* out_data, int outsize)
{
	int size = 0;

	while(outsize > 0)
	{
		if ( storage_size < outsize)
			size = storage_size;
		else
			size = outsize;
		env->SetByteArrayRegion(jstorage, 0, size, (const jbyte*) out_data);
		if (env->ExceptionCheck()) {
			env->ExceptionDescribe();
			env->ExceptionClear();
			return 1;
		}

		env->CallVoidMethod(jstream, gOutputStream_writeMethodID,
			jstorage, 0, size);
		if (env->ExceptionCheck()) {
			env->ExceptionDescribe();
			env->ExceptionClear();
			return 1;
		}
		outsize -= size;
		out_data += size;
		env->CallVoidMethod(jstream, gOutputStream_flushMethodID);
	}
	return 0;
}

boolean YuvToJpegEncoderMT_encode(JNIEnv* env, jobject jstream, jbyteArray jstorage, uint8_t* inYuv, int width,
        int height, int* offsets, int* strides, int jpegQuality, int format) {
    unsigned char *out_data = NULL;
    unsigned long outsize = 0;
    mem_dest_ptr dest;
	int i, j;
	struct jpeg_compress_struct* cinfo_arr;
    struct mt_jpeg_error_mgr* jerr_arr;
	int thread_num = getThreadsNum();
	unsigned char **outbuffer_arr;
	unsigned long *outsize_arr;
	int thread_height;
	int bufsize;
	int storage_size;
	int processed_lines;
	int restart_cnt = 0;
	int lines_per_iMCU_row = 16;
	int last_thread_num;
	boolean err = false;
	boolean err_thread_num = 0;
	int file_size = 0;

	storage_size = env->GetArrayLength(jstorage);
	bufsize = getBuffSize(height, width, thread_num);

    if (height < 64)
    {
    	thread_num = 1;
    	thread_height = height;
    }
    else
    	thread_height = getOptHeight(height, width, bufsize, thread_num);

	cinfo_arr = (struct jpeg_compress_struct *)malloc(sizeof(struct jpeg_compress_struct) * thread_num);
	jerr_arr = (struct mt_jpeg_error_mgr *)malloc(sizeof(struct mt_jpeg_error_mgr) * thread_num);
	outbuffer_arr = (unsigned char**)malloc(sizeof(unsigned char *)*thread_num);
	outsize_arr = (unsigned long*)malloc(sizeof(unsigned long)*thread_num);

	//init cinfo structures
	for (i = 0; i < thread_num; i++)
	{
		outbuffer_arr[i] = NULL;

		outsize_arr[i] = 0;

	    cinfo_arr[i].err = jpeg_std_error(&jerr_arr[i]);
	    jerr_arr[i].error_exit = mt_jpeg_error_exit;
	    jerr_arr[i].thread_num = i+1;
	    if (err_thread_num = setjmp(jerr_arr[i].fJmpBuf)) {
	        err = true;
	        break;
	    }

	    jpeg_create_compress(&cinfo_arr[i]);

	    YuvToJpegEncoderMT_setJpegCompressStruct(&cinfo_arr[i], width, height, jpegQuality);

	    outsize_arr[i] = bufsize;

	    outbuffer_arr[i] = (uint8_t*)malloc(outsize_arr[i]);

	    jpeg_mem_dest(&cinfo_arr[i], &outbuffer_arr[i], &outsize_arr[i]);

	    cinfo_arr[i].restart_in_rows = thread_height/lines_per_iMCU_row;

		jpeg_start_compress(&cinfo_arr[i], TRUE);
	}

	if (err)
	{
		for (i = 0; i < err_thread_num; i++)
		{
			jpeg_destroy((j_common_ptr)&cinfo_arr[i]);
			free(outbuffer_arr[i]);
		}
	    free(cinfo_arr);
	    free(jerr_arr);
	    free(outbuffer_arr);
	    free(outsize_arr);
		return false;
	}

	for (processed_lines = 0; processed_lines < height; processed_lines += thread_height * thread_num)
	{
		boolean last_iter = (processed_lines + thread_height * thread_num) >= height;
		LOGD("processed_lines %d %d last_iter", processed_lines, last_iter);
#pragma omp parallel for num_threads(thread_num)
		for (i = 0; i < thread_num; i++)
		{
			boolean call_pass_startup = (i == 0) && (processed_lines == 0);
			int start_row = i * thread_height + processed_lines;
			int end_row = start_row +  thread_height;

			if (start_row >= height)
				continue;

			if (end_row >= height)
			{
				end_row = height;
				cinfo_arr[i].restart_in_rows = 0;
			}

		   /* if (err_thread_num = setjmp(jerr_arr[i].fJmpBuf)) {
		        err = true;
		        continue;
		    }*/

			if (fFormat == ImageFormat_NV21)
				err |= Yuv420SpToJpegEncoderMT_compress(&cinfo_arr[i],
						(uint8_t*) inYuv, offsets, start_row, end_row, call_pass_startup);
			else
				err |= Yuv422IToJpegEncoderMT_compress(&cinfo_arr[i], (uint8_t*) inYuv,
						offsets, start_row, end_row, call_pass_startup);

			if (err)
			{
				continue;
			}

			if (end_row == height)
			{
				last_thread_num = i;
				jpeg_finish_compress(&cinfo_arr[i]);
			}

			LOGD("start_row %d end_row %d i %d ", start_row, end_row, i);
		}

		if (err) break;

		if (!last_iter)
		{
			for (i = 0; i < thread_num; i++)
			{
				dest = (mem_dest_ptr) cinfo_arr[i].dest;
				outsize = (dest->bufsize)-(dest->pub.free_in_buffer);
				out_data = dest->buffer;
				dest->pub.free_in_buffer = dest->bufsize;
				dest->pub.next_output_byte = dest->buffer;

				//correct restart marker;
				out_data[outsize-1] =  JPEG_RST0 + (restart_cnt & 0x7);
				restart_cnt++;

				file_size += outsize;

				if(write_to_stream(env, jstream, jstorage, storage_size, out_data, outsize))
				{
					err= true;
					break;
				}
			}
		}

		if (err) break;
	}

	if (err)
	{
		for (i = 0; i < thread_num; i++)
		{
			jpeg_destroy((j_common_ptr)&cinfo_arr[i]);
			free(outbuffer_arr[i]);
		}
	    free(cinfo_arr);
	    free(jerr_arr);
	    free(outbuffer_arr);
	    free(outsize_arr);
		return false;
	}

	for (i = 0; i < thread_num; i++)
	{
		dest = (mem_dest_ptr) cinfo_arr[i].dest;
		outsize = (dest->bufsize)-(dest->pub.free_in_buffer);
		out_data = dest->buffer;

		//correct restart marker;

    	if (i < last_thread_num)
    	{
    		out_data[outsize-1] =  JPEG_RST0 + (restart_cnt & 0x7);
    		restart_cnt++;
    	}

		if (i <= last_thread_num)
		{
			file_size += outsize;

			if(write_to_stream(env, jstream, jstorage, storage_size, out_data, outsize))
			{
				err= true;
				break;
			}
		}

    	if (i != last_thread_num)
    	{
    		cinfo_arr[i].next_scanline = cinfo_arr[i].image_height;
    		jpeg_finish_compress(&cinfo_arr[i]);
    	}
		if (err) break;
	}

	if (err)
	{
		for (i = 0; i < thread_num; i++)
		{
			jpeg_destroy((j_common_ptr)&cinfo_arr[i]);
			free(outbuffer_arr[i]);
		}
	    free(cinfo_arr);
	    free(jerr_arr);
	    free(outbuffer_arr);
	    free(outsize_arr);
		return false;
	}

    env->CallVoidMethod(jstream, gOutputStream_flushMethodID);

    for ( i = 0; i < thread_num; i++)
	{
		jpeg_destroy_compress(&cinfo_arr[i]);
		free(outbuffer_arr[i]);
	}

    LOGD("file_size %d ", file_size);

    free(cinfo_arr);
    free(jerr_arr);
    free(outbuffer_arr);
    free(outsize_arr);
    return true;
}

void YuvToJpegEncoderMT_setJpegCompressStruct(jpeg_compress_struct* cinfo,
        int width, int height, int quality) {
    cinfo->image_width = width;
    cinfo->image_height = height;
    cinfo->input_components = 3;
    cinfo->in_color_space = JCS_YCbCr;
    jpeg_set_defaults(cinfo);

    jpeg_set_quality(cinfo, quality, TRUE);
    jpeg_set_colorspace(cinfo, JCS_YCbCr);
    cinfo->raw_data_in = TRUE;
    cinfo->dct_method = JDCT_IFAST;

	if (fFormat == ImageFormat_NV21)
		Yuv420SpToJpegEncoderMT_configSamplingFactors(cinfo);
	else
		Yuv422IToJpegEncoderMT_configSamplingFactors(cinfo);

}

///////////////////////////////////////////////////////////////////
void Yuv420SpToJpegEncoderMT_init(int* strides)
{
	fStrides = strides;
    fNumPlanes = 2;
}

void Yuv420SpToJpegEncoderMT_deinterleave(uint8_t* vuPlanar, uint8_t* uRows,
        uint8_t* vRows, int rowIndex, int width, int height) {
    int numRows = (height - rowIndex) / 2;
    if (numRows > 8) numRows = 8;
    for (int row = 0; row < numRows; ++row) {
        int offset = ((rowIndex >> 1) + row) * fStrides[1];
        uint8_t* vu = vuPlanar + offset;
        for (int i = 0; i < (width >> 1); ++i) {
            int index = row * (width >> 1) + i;
            uRows[index] = vu[1];
            vRows[index] = vu[0];
            vu += 2;
        }
    }
}

boolean Yuv420SpToJpegEncoderMT_compress(jpeg_compress_struct* cinfo,
        uint8_t* yuv, int* offsets, int start_row, int end_row, boolean call_pass_startup) {
    //SkDebugf("onFlyCompress");
    JSAMPROW y[16];
    JSAMPROW cb[8];
    JSAMPROW cr[8];
    JSAMPARRAY planes[3];
    planes[0] = y;
    planes[1] = cb;
    planes[2] = cr;

    int width = cinfo->image_width;
    int height = cinfo->image_height;
    uint8_t* yPlanar = yuv + offsets[0];
    uint8_t* vuPlanar = yuv + offsets[1]; //width * height;
    uint8_t* uRows = (uint8_t*)malloc(8 * (width >> 1));
    uint8_t* vRows = (uint8_t*)malloc(8 * (width >> 1));
    mem_dest_ptr dest;
    mt_jpeg_error_mgr *err = (mt_jpeg_error_mgr *)cinfo->err;
    int err_thread_num = 0;

    if (err_thread_num = setjmp(err->fJmpBuf)) {
        free(uRows);
        free(vRows);
    	return true;
    }

	if (call_pass_startup)
		cinfo->master->call_pass_startup = TRUE;
	else
	{
		cinfo->master->call_pass_startup = FALSE;
		dest = (mem_dest_ptr) cinfo->dest;
		dest->pub.free_in_buffer = dest->bufsize;
		dest->pub.next_output_byte = dest->buffer;
	}

	cinfo->next_scanline = start_row;

    // process 16 lines of Y and 8 lines of U/V each time.
	while (cinfo->next_scanline < end_row) {
        //deitnerleave u and v
    	Yuv420SpToJpegEncoderMT_deinterleave(vuPlanar, uRows, vRows, cinfo->next_scanline, width, height);

        // Jpeg library ignores the rows whose indices are greater than height.
        for (int i = 0; i < 16; i++) {
            // y row
            y[i] = yPlanar + (cinfo->next_scanline + i) * fStrides[0];

            // construct u row and v row
            if ((i & 1) == 0) {
                // height and width are both halved because of downsampling
                int offset = (i >> 1) * (width >> 1);
                cb[i/2] = uRows + offset;
                cr[i/2] = vRows + offset;
            }
          }
        jpeg_write_raw_data(cinfo, planes, 16);
    }
    free(uRows);
    free(vRows);
    return false;
}

void Yuv420SpToJpegEncoderMT_configSamplingFactors(jpeg_compress_struct* cinfo) {
    // cb and cr are horizontally downsampled and vertically downsampled as well.
    cinfo->comp_info[0].h_samp_factor = 2;
    cinfo->comp_info[0].v_samp_factor = 2;
    cinfo->comp_info[1].h_samp_factor = 1;
    cinfo->comp_info[1].v_samp_factor = 1;
    cinfo->comp_info[2].h_samp_factor = 1;
    cinfo->comp_info[2].v_samp_factor = 1;
}

///////////////////////////////////////////////////////////////////////////////
void Yuv422IToJpegEncoderMT_init(int* strides)
{
	fStrides = strides;
    fNumPlanes = 1;
}

void Yuv422IToJpegEncoderMT_deinterleave(uint8_t* yuv, uint8_t* yRows, uint8_t* uRows,
        uint8_t* vRows, int rowIndex, int width, int height) {
    int numRows = height - rowIndex;
    if (numRows > 16) numRows = 16;
    for (int row = 0; row < numRows; ++row) {
        uint8_t* yuvSeg = yuv + (rowIndex + row) * fStrides[0];
        for (int i = 0; i < (width >> 1); ++i) {
            int indexY = row * width + (i << 1);
            int indexU = row * (width >> 1) + i;
            yRows[indexY] = yuvSeg[0];
            yRows[indexY + 1] = yuvSeg[2];
            uRows[indexU] = yuvSeg[1];
            vRows[indexU] = yuvSeg[3];
            yuvSeg += 4;
        }
    }
}

boolean Yuv422IToJpegEncoderMT_compress(jpeg_compress_struct* cinfo,
        uint8_t* yuv, int* offsets, int start_row, int end_row, boolean call_pass_startup) {
    //SkDebugf("onFlyCompress_422");
    JSAMPROW y[16];
    JSAMPROW cb[16];
    JSAMPROW cr[16];
    JSAMPARRAY planes[3];
    planes[0] = y;
    planes[1] = cb;
    planes[2] = cr;

    int width = cinfo->image_width;
    int height = cinfo->image_height;
    uint8_t* yRows = (uint8_t*)malloc(16 * width);
    uint8_t* uRows = (uint8_t*)malloc(16 * (width >> 1));
    uint8_t* vRows = (uint8_t*)malloc(16 * (width >> 1));

    uint8_t* yuvOffset = yuv + offsets[0];
    mem_dest_ptr dest;
    mt_jpeg_error_mgr *err = (mt_jpeg_error_mgr *)cinfo->err;
    int err_thread_num = 0;

    if (err_thread_num = setjmp(err->fJmpBuf)) {
        free(yRows);
        free(uRows);
        free(vRows);
    	return true;
    }

	if (call_pass_startup)
		cinfo->master->call_pass_startup = TRUE;
	else
	{
		cinfo->master->call_pass_startup = FALSE;
		dest = (mem_dest_ptr) cinfo->dest;
		dest->pub.free_in_buffer = dest->bufsize;
		dest->pub.next_output_byte = dest->buffer;
	}

	cinfo->next_scanline = start_row;
    // process 16 lines of Y and 16 lines of U/V each time.
	while (cinfo->next_scanline < end_row) {
    	Yuv422IToJpegEncoderMT_deinterleave(yuvOffset, yRows, uRows, vRows, cinfo->next_scanline, width, height);

        // Jpeg library ignores the rows whose indices are greater than height.
        for (int i = 0; i < 16; i++) {
            // y row
            y[i] = yRows + i * width;

            // construct u row and v row
            // width is halved because of downsampling
            int offset = i * (width >> 1);
            cb[i] = uRows + offset;
            cr[i] = vRows + offset;
        }

        jpeg_write_raw_data(cinfo, planes, 16);
    }
    free(yRows);
    free(uRows);
    free(vRows);
    return false;
}

void Yuv422IToJpegEncoderMT_configSamplingFactors(jpeg_compress_struct* cinfo) {
    // cb and cr are horizontally downsampled and vertically downsampled as well.
    cinfo->comp_info[0].h_samp_factor = 2;
    cinfo->comp_info[0].v_samp_factor = 2;
    cinfo->comp_info[1].h_samp_factor = 1;
    cinfo->comp_info[1].v_samp_factor = 2;
    cinfo->comp_info[2].h_samp_factor = 1;
    cinfo->comp_info[2].v_samp_factor = 2;
}
