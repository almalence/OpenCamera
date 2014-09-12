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
#include <stdlib.h>
#include <android/log.h>

#include <setjmp.h>
#include "jpeglib.h"
#include <math.h>

#include "ImageConversionUtils.h"

#define LOG_TAG "ImageConversion"
#ifdef LOG_ON
#define LOGD(...) __android_log_print(ANDROID_LOG_DEBUG, LOG_TAG,__VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__ )
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__ )
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__ )
#else
#define LOGD(...)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__ )
#define LOGI(...)
#define LOGW(...)
#endif


typedef int Int32;
typedef short Int16;
typedef unsigned int Uint32;
typedef unsigned short Uint16;
typedef unsigned char Uint8;
typedef signed char Int8;



#define get_Ysz(in, x,y)	((in)[(x)+(y)*sx])
#define get_Usz(in, x, y)	((in)[sx*sy+((x)|1)+((y)/2)*sx])
#define get_Vsz(in, x, y)	((in)[sx*sy+((x)&~1)+((y)/2)*sx])

#define	CLIP8(x)			( (x)<0 ? 0 : (x)>255 ? 255 : (x) )
#define CSC_R(Y,V)			CLIP8((128*(Y)+176*((V)-128)) >> 7 )
#define CSC_B(Y,U)			CLIP8((128*(Y)+222*((U)-128)) >> 7 )
#define CSC_G(Y,U,V)		CLIP8((128*(Y)-89*((V)-128)-43*((U)-128)) >> 7 )


#define BMP_R(p)	((p) & 0xFF)
#define BMP_G(p)	(((p)>>8) & 0xFF)
#define BMP_B(p)	(((p)>>16)& 0xFF)

// No clipping control needed if converting from 8 bit data
#define CSC_Y(R,G,B)		((77*(R)+150*(G)+29*(B)) >> 8 )
#define CSC_U(R,G,B)		(((-43*(R)-85*(G)+128*(B)) >> 8) +128)
#define CSC_V(R,G,B)		(((128*(R)-107*(G)-21*(B)) >> 8) +128)


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

int JPEG2NV21(Uint8 *yuv, Uint8 *jpegdata, int jpeglen, int sx, int sy, bool needRotation, bool cameraMirrored, int rotationDegree)
{
	int i, y;
	int inlen;

	struct jpeg_decompress_struct cinfo;
	struct my_error_mgr jerr;
	JSAMPARRAY scanline;
	unsigned char  *wline;
	unsigned char *y_buffer;
	unsigned char *cbcr_buffer;

	Uint8* dst;

	if (needRotation || cameraMirrored)
	//if(rotationDegree != 0 || cameraMirrored)
		dst = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));
	else
		dst = yuv;

	cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = my_error_exit;
    /* Establish the setjmp return context for my_error_exit to use. */
    if (setjmp(jerr.setjmp_buffer))
    {
        jpeg_destroy_decompress(&cinfo);
        return 0;
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
		TransformNV21(dst, yuv, sx, sy, NULL, cameraMirrored, flipUD, nRotate);
		free(dst);
	}

	return 1;
}

int JPEG2RGBA(Uint8 *dst, Uint8 *jpegdata, int jpeglen)
{
	int i, y;
	int inlen;

	struct jpeg_decompress_struct cinfo;
	struct my_error_mgr jerr;
	JSAMPARRAY scanline;
	unsigned char *wline;
	unsigned char *buff_cursor;
	unsigned char *y_buffer;
	unsigned char *cbcr_buffer;

	cinfo.err = jpeg_std_error(&jerr.pub);
    jerr.pub.error_exit = my_error_exit;
    /* Establish the setjmp return context for my_error_exit to use. */
    if (setjmp(jerr.setjmp_buffer))
    {
        jpeg_destroy_decompress(&cinfo);
        return 0;
    }
	jpeg_create_decompress(&cinfo);

	jpeg_mem_src(&cinfo, jpegdata, jpeglen);
	(void) jpeg_read_header(&cinfo, TRUE);
	//cinfo.raw_data_out = TRUE;
    cinfo.out_color_space = JCS_EXT_RGBA;

	(void) jpeg_start_decompress(&cinfo);

	buff_cursor = dst;
	y_buffer = dst;
	cbcr_buffer = y_buffer + cinfo.output_width * cinfo.output_height;
	scanline = (*cinfo.mem->alloc_sarray)((j_common_ptr) &cinfo, JPOOL_IMAGE, cinfo.output_width * cinfo.output_components, 1);
	wline = scanline[0];

	while (cinfo.output_scanline < cinfo.output_height)
	{
		jpeg_read_scanlines(&cinfo, scanline, (JDIMENSION)1);

		for (i = 0; i < cinfo.output_width; i++)
		{
			buff_cursor[0] = wline[i * 4];
			buff_cursor[1] = wline[i * 4 + 1];
			buff_cursor[2] = wline[i * 4 + 2];
			buff_cursor[3] = wline[i * 4 + 3];

			buff_cursor += 4;
		}
	}

	(void) jpeg_finish_decompress(&cinfo);
	jpeg_destroy_decompress(&cinfo);

	return 1;
}


// returns:
// -1 - not enough memory
// 0<X<nFrames - X = frame index where error is happened during decoding
// 255 - all ok
int DecodeAndRotateMultipleJpegs
(
	unsigned char **yuvFrame,
	unsigned char **jpeg,
	int *jpeg_length,
	int sx,
	int sy,
	int nFrames,
	int needRotation,
	int cameraMirrored,
	int rotationDegree,
	bool needFreeMem//true by default
)
{
	int i;
	int isFoundinInput = 255; // if error is found during decoding - the frame index will be here

	LOGD("ConvertFromJpeg - start");

	// pre-allocate uncompressed yuv buffers
	for (i=0; i<nFrames; ++i)
	{
		yuvFrame[i] = (unsigned char*)malloc(sx*sy+2*((sx+1)/2)*((sy+1)/2));

		if (yuvFrame[i]==NULL)
		{
			LOGE("ConvertFromJpeg - not enough memory");

			i--;
			for (;i>=0;--i)
			{
				free(yuvFrame[i]);
				yuvFrame[i] = NULL;
			}
			return -1;
		}
	}

	#pragma omp parallel for num_threads(10)
	for (i=0; i<nFrames; ++i)
	{
		// decode from jpeg
		if(JPEG2NV21(yuvFrame[i], jpeg[i], jpeg_length[i], sx, sy, needRotation, cameraMirrored, rotationDegree) == 0)
		{
			isFoundinInput = i;
			LOGE("Error Found in %d - jpeg frame\n", (int)i);
		}

		// release compressed memory
		if (needFreeMem)
			free (jpeg[i]);
	}

	LOGD("ConvertFromJpeg - end");

	return isFoundinInput;
}

void TransformPlane8bit
(
	unsigned char * In,
	unsigned char * Out,
	int sx,
	int sy,
	int flipLeftRight,
	int flipUpDown,
	int rotate90
)
{
	int y;
	int osx, osy;

	// no transform case
	if ((!flipLeftRight) && (!flipUpDown) && (!rotate90))
	{
		if (In!=Out)
			memcpy (Out, In, sx*sy*sizeof(unsigned char));
		return;
	}

	// can't rotate in-place
	if (rotate90 && (In == Out))
		return;

	if (rotate90) {osx = sy; osy = sx;}
		else {osx = sx; osy = sy;}

	// processing 4 mirrored locations at once
	// +1 here to cover case when image dimensions are odd
	#pragma omp parallel for schedule(guided)
	for (y=0; y<(sy+1)/2; ++y)
	{
		int x;
		int ox, oy;
		unsigned char t1, t2, t3, t4;

		for (x=0; x<(sx+1)/2; ++x)
		{
			if (rotate90)
			{
				if (flipLeftRight) ox = y;
					else ox = osx-1-y;
				if (flipUpDown) oy = osy-1-x;
					else oy = x;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t3;
				Out[ox + (osy-1-oy)*osx] = t2;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
			else
			{
				if (flipLeftRight) ox = sx-1-x;
					else ox = x;
				if (flipUpDown) oy = sy-1-y;
					else oy = y;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t2;
				Out[ox + (osy-1-oy)*osx] = t3;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
		}
	}
}


void TransformPlane16bit
(
	unsigned short * In,
	unsigned short * Out,
	int sx,
	int sy,
	int flipLeftRight,
	int flipUpDown,
	int rotate90
)
{
	int y;
	int osx, osy;

	// no transform case
	if ((!flipLeftRight) && (!flipUpDown) && (!rotate90))
	{
		if (In!=Out)
			memcpy (Out, In, sx*sy*sizeof(unsigned short));
		return;
	}

	// can't rotate in-place
	if (rotate90 && (In == Out))
		return;

	if (rotate90) {osx = sy; osy = sx;}
		else {osx = sx; osy = sy;}

	// processing 4 mirrored locations at once
	#pragma omp parallel for schedule(guided)
	for (y=0; y<(sy+1)/2; ++y)
	{
		int x;
		int ox, oy;
		unsigned short t1, t2, t3, t4;

		for (x=0; x<(sx+1)/2; ++x)
		{
			if (rotate90)
			{
				if (flipLeftRight) ox = y;
					else ox = osx-1-y;
				if (flipUpDown) oy = osy-1-x;
					else oy = x;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t3;
				Out[ox + (osy-1-oy)*osx] = t2;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
			else
			{
				if (flipLeftRight) ox = sx-1-x;
					else ox = x;
				if (flipUpDown) oy = sy-1-y;
					else oy = y;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t2;
				Out[ox + (osy-1-oy)*osx] = t3;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
		}
	}
}


void TransformPlane32bit
(
	unsigned int * In,
	unsigned int * Out,
	int sx,
	int sy,
	int flipLeftRight,
	int flipUpDown,
	int rotate90
)
{
	int y;
	int osx, osy;

	// no transform case
	if ((!flipLeftRight) && (!flipUpDown) && (!rotate90))
	{
		if (In!=Out)
			memcpy (Out, In, sx*sy*sizeof(unsigned int));
		return;
	}

	// can't rotate in-place
	if (rotate90 && (In == Out))
		return;

	if (rotate90) {osx = sy; osy = sx;}
		else {osx = sx; osy = sy;}

	// processing 4 mirrored locations at once
	#pragma omp parallel for schedule(guided)
	for (y=0; y<(sy+1)/2; ++y)
	{
		int x;
		int ox, oy;
		unsigned int t1, t2, t3, t4;

		for (x=0; x<(sx+1)/2; ++x)
		{
			if (rotate90)
			{
				if (flipLeftRight) ox = y;
					else ox = osx-1-y;
				if (flipUpDown) oy = osy-1-x;
					else oy = x;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t3;
				Out[ox + (osy-1-oy)*osx] = t2;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
			else
			{
				if (flipLeftRight) ox = sx-1-x;
					else ox = x;
				if (flipUpDown) oy = sy-1-y;
					else oy = y;

				t1 = In[x + y*sx];
				t2 = In[sx-1-x + y*sx];
				t3 = In[x + (sy-1-y)*sx];
				t4 = In[sx-1-x + (sy-1-y)*sx];

				Out[ox + oy*osx] = t1;
				Out[osx-1-ox + oy*osx] = t2;
				Out[ox + (osy-1-oy)*osx] = t3;
				Out[osx-1-ox + (osy-1-oy)*osx] = t4;
			}
		}
	}
}


// mirror and/or rotate NV21 image
//
// Note:
//   - mirroring can be performed in-place, rotation can not
//   - rotation is 90 degree clockwise
//   - if need to rotate 180 degree - call with flipLeftRight=1, flipUpDown=1
//   - if need to rotate 90 degree counter-clockwise - call with flipLeftRight=1, flipUpDown=1, rotate90=1
//   - it is assumed that image width is even and image height is even
//   - crop coordinates are also transformed if given, but no cropping is performed
//
void TransformNV21
(
	unsigned char * InNV21,
	unsigned char * OutNV21,
	int sx,
	int sy,
	int *crop,
	int flipLeftRight,
	int flipUpDown,
	int rotate90
)
{
	int tmp;

	TransformPlane8bit(InNV21, OutNV21, sx, sy, flipLeftRight, flipUpDown, rotate90);

	// treat UV as a single 16bit entity - makes transform faster
	TransformPlane16bit((unsigned short*)(InNV21+sx*sy), (unsigned short*)(OutNV21+sx*sy), sx/2, sy/2, flipLeftRight, flipUpDown, rotate90);

	if (crop)
	{
		if (rotate90)
		{
			tmp = crop[0]; crop[0] = crop[1]; crop[1] = tmp;
			tmp = crop[2]; crop[2] = crop[3]; crop[3] = tmp;
			if (!flipLeftRight) crop[0] = sy-(crop[0]+crop[2]);
			if (flipUpDown) crop[1] = sx-(crop[1]+crop[3]);
		}
		else
		{
			if (flipLeftRight) crop[0] = sx-(crop[0]+crop[2]);
			if (flipUpDown) crop[1] = sy-(crop[1]+crop[3]);
		}
	}
}


void NV21_to_RGB
(
	unsigned char * in,
	int * out,
	int   sx,
	int   sy,
	int   rotate
)
{
	int x, y;
	short Y1, Y2, u, v, vp, up, va, ua;
	unsigned int R, G, B;

	for (y=0; y<sy; ++y)
	{
		up=(short)get_Usz(in, 0, y);
		vp=(short)get_Vsz(in, 0, y);

		for (x=0; x<sx; x+=2)
		{
			Y1 = (short)get_Ysz(in, x, y);
			Y2 = (short)get_Ysz(in, x+1, y);
			v  = (short)get_Vsz(in, x, y);
			if (x<sx-2)
				u  = (short)get_Usz(in, x+2, y);
			else
				u = up;

			ua = (u+up)/2;
			va = (v+vp)/2;

			R = CSC_R(Y1, va);
			G = CSC_G(Y1, up, va);
			B = CSC_B(Y1, up);

			if (rotate)
				out[x*sy+sy-1-y] = (R<<16) + (G<<8) + B + (255<<24);
			else
				out[y*sx+x] = (R<<16) + (G<<8) + B + (255<<24);

			R = CSC_R(Y2, v);
			G = CSC_G(Y2, ua, v);
			B = CSC_B(Y2, ua);

			if (rotate)
				out[(x+1)*sy+sy-1-y] = (R<<16) + (G<<8) + B + (255<<24);
			else
				out[y*sx+x+1] = (R<<16) + (G<<8) + B + (255<<24);

			vp = v;
			up = u;
		}
	}
}


void NV21_to_RGB_scaled_rotated
(
	unsigned char *pY,
	int width,
	int height,
	int x0,
	int y0,
	int wCrop,
	int hCrop,
	int outWidth,
	int outHeight,
	int stride,
	unsigned char *buffer
)
{
	unsigned char *pUV = pY + width * height;

	int i, j, is, js;
	int nY, nU, nV;
	unsigned char *out = buffer;
	int offset;
	int bgr = 1;

	if (stride >= 5)	// a special case for GL - RGBA
	{
		bgr = 0;
		stride -= 2;
	}
	else if (stride == 4)
	{
		bgr = 0;
	}

	const int tripleHeight = (outHeight - 1) * stride;
	int yoffset = tripleHeight;

	pY += x0 + y0 * width;
	pUV += x0-(x0&1) + (y0/2) * width;

	for (i = 0; i < outHeight; i++)
	{
		offset = yoffset;

		is = i * hCrop / outHeight;

		for (j = 0; j < outWidth; j++)
		{
			js = j * wCrop / outWidth;

			nY = *(pY + is * width + js);
			nV = *(pUV + (is / 2) * width + 2 * (js / 2));
			nU = *(pUV + (is / 2) * width + 2 * (js / 2) + 1);

			if (bgr)	// usual bitmap has BGRA format
			{
				out[offset++] = CSC_B(nY, nU);
				out[offset++] = CSC_G(nY, nU, nV);
				out[offset++] = CSC_R(nY, nV);
			}
			else		// a special case for GL - RGBA
			{
				out[offset++] = CSC_R(nY, nV);
				out[offset++] = CSC_G(nY, nU, nV);
				out[offset++] = CSC_B(nY, nU);
			}
			if (stride == 4) out[offset++] = 255;

			offset += tripleHeight;
		}

		yoffset -= stride;
	}
}


void NV21_to_RGB_scaled
(
	unsigned char *pY,
	int width,
	int height,
	int x0,
	int y0,
	int wCrop,
	int hCrop,
	int outWidth,
	int outHeight,
	int stride,
	unsigned char *buffer
)
{
    unsigned char *pUV = pY + width * height + (x0&~1) + (y0/2)*width;
    pY += x0+y0*width;

    int i, j, is, js;
    unsigned char *out = buffer;
    int offset = 0;
    const float scaleWidth = wCrop / outWidth;
    const float scaleHeight = hCrop / outHeight;

    int Y, U, V;

    for (i = 0; i < outHeight; i++)
    {
    	offset = i * outWidth * 4;

        is = i * hCrop / outHeight;

        for (j = 0; j < outWidth; j++)
        {
            js = j * wCrop / outWidth;

            Y = *(pY + is * width + js);
            V = *(pUV + (is / 2) * width + 2*(js / 2));
            U = *(pUV + (is / 2) * width + 2*(js / 2) + 1);

			out[offset++] = CSC_B(Y,U);
			out[offset++] = CSC_G(Y, U, V);
			out[offset++] = CSC_R(Y,V);
			if (stride == 4) out[offset++] = 255;
        }
    }
}


void NV21_to_Gray_scaled
(
	unsigned char *pY,
	int width,
	int height,
	int x0,
	int y0,
	int wCrop,
	int hCrop,
	int outWidth,
	int outHeight,
	unsigned char *buffer
)
{
    int i, j, is, js;
    int Yoffset = 0;

    pY += x0+y0*width;

    for (i = 0; i < outHeight; i++)
    {
    	Yoffset = i * outWidth;
        is = i * hCrop / outHeight;

        for (j = 0; j < outWidth; j++)
        {
           js = j * wCrop / outWidth;

           buffer[Yoffset] = *(pY + is * width + js);
           Yoffset++;
        }
    }
}

void addRoundCornersRGBA8888
(
	unsigned char * const rgb_bytes,
	const int outWidth,
	const int outHeight
)
{
	// make nice corners and edges,
	// apply softbox-like effect
	int edge = (outWidth < outHeight ? outWidth:outHeight)/60;
	int corner = (outWidth < outHeight ? outWidth:outHeight)/60;
	for (int j = 0; j < outWidth; j++)
	{
		int thr = 2*(outHeight/3) - (outHeight/3) * (j+outWidth-(outWidth-j)*(outWidth-j)/outWidth) / (outWidth*2);

		for (int i = 0; i < outHeight; i++)
		{
			if ((j>corner) && (j<outWidth-1-corner) && (i>edge+1) && (i<outHeight-1-edge))
				continue;
			if ((j>edge+1) && (j<outWidth-2-edge) && (i>corner) && (i<outHeight-1-corner))
				continue;

			int offset = (j+i*outWidth)*4;

			// soft-box-like effect
			int eclr  = 228 + ( i>thr ? (outHeight-i)*(200-228)/(outHeight-thr) : i*(255-228)/thr );
			int sbclr = 128 + ( i>thr ? (outHeight-i)*(0-128)/(outHeight-thr) : i*(255-128)/thr );

			float r = 0;
			float e = 0;
			int ecorner = 0;

			if ((i<corner) && (j<corner))
				r = (float)(corner-j)*(corner-j)+(float)(corner-i)*(corner-i);
			if ((i<corner) && (j>outWidth-1-corner))
				r = (float)(outWidth-1-corner-j)*(outWidth-1-corner-j)+(float)(corner-i)*(corner-i);
			if ((i>outHeight-1-corner) && (j>outWidth-1-corner))
				r = (float)(outWidth-1-corner-j)*(outWidth-1-corner-j)+(float)(outHeight-1-corner-i)*(outHeight-1-corner-i);
			if ((i>outHeight-1-corner) && (j<corner))
				r = (float)(corner-j)*(corner-j)+(float)(outHeight-1-corner-i)*(outHeight-1-corner-i);

			if (r>(corner-edge)*(corner-edge))
			{
				if (r<corner*corner)
				{
					e = edge-(corner-sqrtf(r));
					ecorner = 1;
				}
				else
				{
					rgb_bytes[offset+0] = 0;
					rgb_bytes[offset+1] = 0;
					rgb_bytes[offset+2] = 0;
					rgb_bytes[offset+3] = 0;
					continue;
				}
			}
			else if (i<edge)
				e = edge-i;
			else if (j<edge)
				e = edge-j;
			else if (j>outWidth-1-edge)
				e = j-outWidth+1+edge;
			else if (i>outHeight-1-edge)
				e = i-outHeight+1+edge;

			if (e>0)	// edges
			{
				if ((e<=1) && (r>0))	// anti-aliasing inner corners
				{
					rgb_bytes[offset+0] = ((int)rgb_bytes[offset+0]+eclr)/2;
					rgb_bytes[offset+1] = ((int)rgb_bytes[offset+1]+eclr)/2;
					rgb_bytes[offset+2] = ((int)rgb_bytes[offset+2]+eclr)/2;
				}
				else if (e>edge-2)	// anti-aliasing outer edge of a frame
				{
					int clr;

					if (ecorner)
						clr = max(0, 255-(int)((e-(edge-2))*255));
					else
						clr = 0;
					rgb_bytes[offset+0] = (eclr*clr)>>8;
					rgb_bytes[offset+1] = (eclr*clr)>>8;
					rgb_bytes[offset+2] = (eclr*clr)>>8;
					rgb_bytes[offset+3] = clr;
				}
				else
				{
					rgb_bytes[offset+0] = eclr;
					rgb_bytes[offset+1] = eclr;
					rgb_bytes[offset+2] = eclr;
					rgb_bytes[offset+3] = 255;
				}

				continue;
			}

			// inner part of the image - soft-box effect
			rgb_bytes[offset+0] = ((int)rgb_bytes[offset+0]*7+sbclr)/8;
			rgb_bytes[offset+1] = ((int)rgb_bytes[offset+1]*7+sbclr)/8;
			rgb_bytes[offset+2] = ((int)rgb_bytes[offset+2]*7+sbclr)/8;
		}
	}
}
