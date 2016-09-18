#include <jni.h>

#include <stdlib.h>

#include <android/log.h>
#include <android/bitmap.h>

#define get_Ysz(in, sx, sy, crop_x, crop_y, x, y)	((in)[(crop_x)+(crop_y)*(sx)+(x)+(y)*(sx)])
#define get_Usz(in, sx, sy, crop_x, crop_y, x, y)	((in)[(sx)*(sy)+(crop_x)+((crop_y)/2)*(sx)+((x)|1)+((y)/2)*(sx)])
#define get_Vsz(in, sx, sy, crop_x, crop_y, x, y)	((in)[(sx)*(sy)+(crop_x)+((crop_y)/2)*(sx)+((x)&~1)+((y)/2)*(sx)])

#define	CLIP8(x)	( (x)<0 ? 0 : (x)>255 ? 255 : (x) )
#define CSC_R(Y,V)			CLIP8((128*(Y)+176*((V)-128)) >> 7 )
#define CSC_B(Y,U)			CLIP8((128*(Y)+222*((U)-128)) >> 7 )
#define CSC_G(Y,U,V)		CLIP8((128*(Y)-89*((V)-128)-43*((U)-128)) >> 7 )

extern "C"
{

inline int min(int a, int b)
{
	return (a < b ? a : b);
}

void NV21_to_RGB
(
	unsigned char * in,
	int * out,
	int   sx,
	int   sy,
	int   crop_x,
	int   crop_y,
	int   crop_w,
	int   crop_h,
	int   crop_w_abs
)
{
	int x, y;
	short Y1, Y2, u, v, vp, up, va, ua;
	unsigned int R, G, B;

	for (y=0; y<crop_h; ++y)
	{
		up=(short)get_Usz(in, sx, sy, crop_x, crop_y, 0, y);
		vp=(short)get_Vsz(in, sx, sy, crop_x, crop_y, 0, y);

		for (x=0; x<crop_w; x+=2)
		{
			Y1 = (short)get_Ysz(in, sx, sy, crop_x, crop_y, x, y);
			Y2 = (short)get_Ysz(in, sx, sy, crop_x, crop_y, x+1, y);
			v  = (short)get_Vsz(in, sx, sy, crop_x, crop_y, x, y);
			if (x<crop_w-2)
				u  = (short)get_Usz(in, sx, sy, crop_x, crop_y, x+2, y);
			else
				u = up;

			ua = (u+up)/2;
			va = (v+vp)/2;

			R = CSC_R(Y1, va);
			G = CSC_G(Y1, up, va);
			B = CSC_B(Y1, up);

			out[y*crop_w_abs+x] = (255<<24) + (B<<16) + (G<<8) + R;

			R = CSC_R(Y2, v);
			G = CSC_G(Y2, ua, v);
			B = CSC_B(Y2, ua);

			out[y*crop_w_abs+x+1] = (255<<24) + (B<<16) + (G<<8) + R;

			vp = v;
			up = u;
		}
	}
}

JNIEXPORT jboolean JNICALL Java_com_almalence_plugins_processing_focusstacking_YuvBitmap_fromAddress(JNIEnv* env, jobject thiz,
		jobject bitmap, jint address, jint input_w, jint input_h, jint crop_x, jint crop_y, jint crop_w, jint crop_h)
{
	int ret;
	int * bitmap_data;

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, ((void**)&bitmap_data))) < 0)
	{
		return false;
	}

	const int actual_crop_w = min(crop_w, input_w - crop_x);
	const int actual_crop_h = min(crop_h, input_h - crop_y);

	NV21_to_RGB((uint8_t*)address, bitmap_data, input_w, input_h, crop_x, crop_y, actual_crop_w, actual_crop_h, crop_w);

	AndroidBitmap_unlockPixels(env, bitmap);

	return true;
}

JNIEXPORT jboolean JNICALL Java_com_almalence_plugins_processing_focusstacking_YuvBitmap_fromByteArray(JNIEnv* env, jobject thiz,
		jobject bitmap, jbyteArray inputArray, jint input_w, jint input_h, jint crop_x, jint crop_y, jint crop_w, jint crop_h)
{
	int ret;
	int * bitmap_data;

	if ((ret = AndroidBitmap_lockPixels(env, bitmap, ((void**)&bitmap_data))) < 0)
	{
		return false;
	}

	const int actual_crop_w = min(crop_w, input_w - crop_x);
	const int actual_crop_h = min(crop_h, input_h - crop_y);

	uint8_t* inputFrame = (uint8_t*)env->GetByteArrayElements(inputArray, NULL);

	NV21_to_RGB((uint8_t*)inputFrame, bitmap_data, input_w, input_h, crop_x, crop_y, actual_crop_w, actual_crop_h, crop_w);

	AndroidBitmap_unlockPixels(env, bitmap);

	env->ReleaseByteArrayElements(inputArray, (jbyte*)inputFrame, 0);

	return true;
}

}
