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

#include <string.h>
#include <stdio.h>
#include <jni.h>
#include <pthread.h>
#include <android/log.h>


#include "ImageConversionUtils.h"

typedef int Int32;
typedef short Int16;
typedef unsigned int Uint32;
typedef unsigned short Uint16;
typedef unsigned char Uint8;
typedef signed char Int8;

static int FPS = 1;

//cyclic buffer for storing data
static unsigned char *frame_buffer = NULL;
static int buf_size = 0;

//cyclic buffer for storing image orientation
static unsigned char *orient_buffer = NULL;
//static unsigned char *reserved_orient_buffer = NULL;

//cyclic buffer for storing data length
static unsigned int *len_buffer = NULL;
//static unsigned int *reserved_len_buffer = NULL;

//reserved cyclic buffer for storing data. reserved used while saving
//static unsigned char *frame_buffer_reserved = NULL;

//static int buf_size_reserved = 0;
//static int elemSizeReserved = 0;
//static int isReservedAllocated=0;

static int image_w = 0;
static int image_h = 0;

//static int image_wReserved = 0;
//static int image_hReserved = 0;

static int idxIN = 0;
static int idxOUT = 0;
static int elemSize = 0;
static long mem_free = 0;


extern "C" {


//checks available heap memory
void mem_usage(long *mem_free)
{
	FILE *f;
	char dummy[1024];

	// the fields we want
	unsigned long curr_mem_used;
	unsigned long curr_mem_free;
	unsigned long curr_mem_buffers;
	unsigned long curr_mem_cached;

	*mem_free = 0;

	// 'file' stat seems to give the most reliable results
	f = fopen ("/proc/meminfo", "r");
	if (f==NULL) return;
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_used, dummy);
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_free, dummy);
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_buffers, dummy);
	fscanf(f, "%s %ld %s ", dummy, &curr_mem_cached, dummy);
	fclose(f);

	*mem_free = (curr_mem_free + curr_mem_cached) * 1024;
}

//max amount of elements which can be allocated
JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_AvailableMemory
(
	JNIEnv* env,
	jobject pObj
)
{
	//count amount of elements. we can't use all available memory. take just 80%
	return mem_free*0.8/elemSize;
}


//allocate buffer for storing
//allocation depends on image w and h, selected fps, selected amount of seconds to store, image format
JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_AllocateBuffer
(
	JNIEnv* env,
	jobject pObj,
	jint jimgw,
	jint jimgh,
	jint jfps,
	jint secondsToAllocate,
	jint isJPG
)
{
	int i=0;

	int desiredBufSize = 0;
	int maxBufSize = 0;

	image_w = jimgw;
	image_h = jimgh;
	FPS = jfps;

	//zero buf head&tail
	idxIN=0;
	idxOUT=0;

	if (isJPG==1)
		elemSize = jimgw*jimgh/2;
	else
		elemSize = jimgw*jimgh*3/2;
	//long mem_free;
	mem_usage(&mem_free);

	desiredBufSize = secondsToAllocate*FPS+1;
//	if (!isReservedAllocated)
//		maxBufSize = mem_free/2/elemSize;
//	else
		maxBufSize = mem_free*0.8/elemSize;

	//count buf_size
	buf_size = (desiredBufSize<maxBufSize)?desiredBufSize:maxBufSize;

	frame_buffer = (unsigned char *)malloc(sizeof(unsigned char)*buf_size*elemSize);
	if (!frame_buffer)
		return 0;

	orient_buffer = (unsigned char *)malloc(sizeof(unsigned char)*buf_size);
	if (!orient_buffer)
		return 0;

	len_buffer = (unsigned int *)malloc(sizeof(unsigned int)*buf_size);
	if (!len_buffer)
		return 0;

	__android_log_print(ANDROID_LOG_ERROR, "Allocation", "Allocated %d bufers of %d size", buf_size, elemSize);

	return buf_size/FPS;
}

//free allocated bufer
JNIEXPORT jboolean JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_FreeBuffer
(
	JNIEnv* env,
	jobject pObj
)
{
	int i;

	idxIN = 0;
	idxOUT = 0;

	if (!frame_buffer)
	{
		return 0;
	}
	free(frame_buffer);
	frame_buffer = 0;

	if (!orient_buffer)
	{
		return 0;
	}
	free(orient_buffer);
	orient_buffer = 0;

	if (!len_buffer)
	{
		return 0;
	}
	free(len_buffer);
	len_buffer = 0;


	__android_log_print(ANDROID_LOG_ERROR, "Allocation", "Buffers freed");

	return 1;
}

//insert data into buffer specifying if image is in portrait/landscape orientation
JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_InsertToBuffer
(
	JNIEnv* env,
	jobject pObj,
	jbyteArray jdata,
	//jint isPortrait
	jint orientation
)
{
	unsigned char *data;
	int data_length;

//	if (!isBuffering)
//		return 0;

	data_length = env->GetArrayLength(jdata);
	//__android_log_print(ANDROID_LOG_ERROR, "Insert", "Buffer size %d data size %d", elemSize, data_length);
	if (data_length > elemSize)
		return -1;
	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);

	memcpy ((void*)(frame_buffer+ idxIN*elemSize), (void*)data, data_length);

//	if (1==isPortrait)
//		*(orient_buffer+ idxIN) = 1;
//	else
//		*(orient_buffer+ idxIN) = 0;
	if(0 == orientation)
		*(orient_buffer+ idxIN) = 0;
	else if(90 == orientation)
		*(orient_buffer+ idxIN) = 1;
	else if(180 == orientation)
		*(orient_buffer+ idxIN) = 2;
	else if(270 == orientation)
		*(orient_buffer+ idxIN) = 3;

	*(len_buffer+ idxIN) = data_length;

	env->ReleaseByteArrayElements(jdata, (jbyte*)data, JNI_ABORT);

	//move head
	idxIN++;
	if(idxIN == (buf_size))
		idxIN=0;

	//move tail
	if (idxIN == idxOUT)
	{
		idxOUT++;
		if(idxOUT == (buf_size))
			idxOUT=0;
	}

	return 0;
}


//get image converted from yuv to JPEG (to show preview)
//specify orientation or use orientation from orient_buffer
JNIEXPORT jintArray JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_GetFromBufferRGBA
(
	JNIEnv* env,
	jobject pObj,
	jint idx,
	jboolean manualOrientation,
	jboolean orientation
)
{
	unsigned int *data;
	int TempidxOUT = idxOUT;
	jintArray jdata = env->NewIntArray(0);

	if (idxIN == idxOUT)
	{
		env->ReleaseIntArrayElements(jdata, (jint*)data, 0);
		return jdata;
	}

	TempidxOUT +=idx;

	if(TempidxOUT == (buf_size))
		TempidxOUT=0;
	if (idxIN == TempidxOUT)
	{
		env->ReleaseIntArrayElements(jdata, (jint*)data, 0);
		return jdata;
	}
	if(TempidxOUT > (buf_size-1))
		TempidxOUT = idx - (buf_size - idxOUT);

	jdata = env->NewIntArray(elemSize);

	data = (unsigned int*)env->GetIntArrayElements(jdata, NULL);

	//bool rotate = (manualOrientation ? orientation : (1 == *(orient_buffer+TempidxOUT)));

	bool rotate = (manualOrientation ? orientation : (1 == *(orient_buffer+TempidxOUT) || 3 == *(orient_buffer+TempidxOUT)));
	NV21_to_RGB(frame_buffer+TempidxOUT*elemSize, (int*)data, image_w, image_h, rotate);

	env->ReleaseIntArrayElements(jdata, (jint*)data, 0);

	return jdata;
}


// - move GetFromBufferToShowInSlow jpeg decodings and downscaling into java
// (it is always a single-image decode, there will always be enough java heap memory),
// all what is needed - to move data from native heap into java heap
//
//show in slow mode
JNIEXPORT jbyteArray JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_GetFromBufferToShowInSlow
(
	JNIEnv* env,
	jobject pObj,
	jint idx,
	jint previewW,
	jint previewH,
	jboolean cameraMirrored
)
{
	unsigned char *data;
	int TempidxOUT = idxOUT;
	jbyteArray jdata = env->NewByteArray(0);

	if (idxIN == idxOUT)
	{
		//env->ReleaseIntArrayElements(jdata, (jint*)data, JNI_ABORT);
		env->ReleaseByteArrayElements(jdata, (jbyte*)data, 0);
		return jdata;
	}

	TempidxOUT +=idx;

	if(TempidxOUT == (buf_size))
		TempidxOUT=0;
	if (idxIN == TempidxOUT)
	{
		//env->ReleaseIntArrayElements(jdata, (jint*)data, JNI_ABORT);
		env->ReleaseByteArrayElements(jdata, (jbyte*)data, 0);
		return jdata;
	}
	if(TempidxOUT > (buf_size-1))
		TempidxOUT = idx - (buf_size - idxOUT);




	size_t size_src = *(len_buffer+ TempidxOUT);
	//size_t size = image_h*image_w;
	//jdata = env->NewIntArray(size);
	jdata = env->NewByteArray(size_src);
	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);

//	jdata = env->NewIntArray(size_src/sizeof(int));
//	data = (unsigned int*)env->GetIntArrayElements(jdata, NULL);
	memcpy (data, frame_buffer+TempidxOUT*elemSize, size_src);
	//memcpy (data, frame_buffer+TempidxOUT*elemSize, size*sizeof(int));
	//env->ReleaseIntArrayElements(jdata, (jint*)data, JNI_ABORT);
	env->ReleaseByteArrayElements(jdata, (jbyte*)data, 0);

	return jdata;
}

//check image orientation by index
//JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_isPortrait
JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_getOrientation
(
	JNIEnv* env,
	jobject pObj,
	jint idx
)
{
	int TempidxOUT = idxOUT;

	TempidxOUT +=idx;

	if(TempidxOUT == (buf_size))
		TempidxOUT=0;
	if (idxIN == TempidxOUT)
	{
		return 0;
	}
	if(TempidxOUT > (buf_size-1))
		TempidxOUT = idx - (buf_size - idxOUT);

//	if (1 == *(orient_buffer+TempidxOUT))
//		return 1;
//	else
//		return 0;
	if(0 == *(orient_buffer+TempidxOUT))
		return 0;
	else if(1 == *(orient_buffer+TempidxOUT))
		return 90;
	else if(2 == *(orient_buffer+TempidxOUT))
		return 180;
	else if(3 == *(orient_buffer+TempidxOUT))
		return 270;
	//return *(orient_buffer+TempidxOUT);
}

////check image orientation by index in reserved buffer
////JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_isPortraitReserved
//JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_getOrientationReserved
//(
//	JNIEnv* env,
//	jobject pObj,
//	jint idx
//)
//{
////	if (1 == *(reserved_orient_buffer+idx))
////		return 1;
////	else
////		return 0;
//	if(0 == *(reserved_orient_buffer+idx))
//		return 0;
//	else if(1 == *(reserved_orient_buffer+idx))
//		return 90;
//	else if(2 == *(reserved_orient_buffer+idx))
//			return 180;
//	else if(3 == *(reserved_orient_buffer+idx))
//			return 270;
//	//return *(reserved_orient_buffer+idx);
//}

//gets amount of images in buffer
JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_GetImageCount
(
	JNIEnv* env,
	jobject pObj
)
{
	int imgCnt=0;

	if (idxIN == 0 && idxOUT == 0)
		return 0;

	if (idxIN > idxOUT)
	{
		imgCnt = idxIN - idxOUT;
	}
	else
	{
		imgCnt = buf_size - idxOUT + idxIN;
	}
	return imgCnt;
}


////allocate reserved buffer - can be used to store data while saving - not to waste time waiting for operation complete
//int AllocateBufferReserved()
//{
//	int i=0;
//
//	frame_buffer_reserved = (unsigned char *)malloc(sizeof(unsigned char )*buf_size_reserved*elemSize);
//	if (!frame_buffer_reserved)
//		return 0;
//
//	reserved_orient_buffer = (unsigned char *)malloc(sizeof(unsigned char )*buf_size_reserved);
//	if (!reserved_orient_buffer)
//		return 0;
//
//	reserved_len_buffer = (unsigned int*)malloc(sizeof(unsigned int)*buf_size_reserved);
//	if (!reserved_len_buffer)
//		return 0;
//
//	isReservedAllocated = 1;
//	__android_log_print(ANDROID_LOG_ERROR, "Allocation", "Allocated reserved %d bufers of %d size", buf_size_reserved, elemSizeReserved);
//
//	return 1;
//}

////copy data from original buffer to reserved
//JNIEXPORT jint JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_MakeCopy
//(
//	JNIEnv* env,
//	jobject pObj
//)
//{
//
//	if (isReservedAllocated)
//		return 0;
//
//	int imgCnt=0;
//	if (idxIN > idxOUT)
//	{
//		imgCnt = idxIN - idxOUT;
//	}
//	else
//	{
//		imgCnt = buf_size - idxOUT + idxIN;
//	}
//
//	buf_size_reserved = imgCnt;
//	elemSizeReserved = elemSize;
//
//	image_wReserved = image_w;
//	image_hReserved = image_h;
//
//	if (!AllocateBufferReserved())
//		return 0;
//
//	int TempidxOUT = idxOUT;
//
//	int i=0;
//	for (i=0; i<imgCnt; i++)
//	{
//		if (idxIN == TempidxOUT)
//		{
//			break;
//		}
//		memcpy (frame_buffer_reserved+i*elemSize, frame_buffer+TempidxOUT*elemSize, elemSize);
//
//		*(reserved_orient_buffer+i) = *(orient_buffer+TempidxOUT);
//
//		*(reserved_len_buffer+i) = *(len_buffer+TempidxOUT);
//
//		TempidxOUT++;
//		if(TempidxOUT == (buf_size))
//			TempidxOUT=0;
//	}
//
//	return 1;
//}

//get data from buffer
JNIEXPORT jbyteArray JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_GetFromBufferNV21
(
	JNIEnv* env,
	jobject pObj,
	jint idx,
	jint W,
	jint H,
	jint mirrored
)
{
	unsigned char *data;

	jbyteArray jdata;

	jdata = env->NewByteArray(elemSize);
	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);

	if (1 != mirrored)
	{
		if (1 == *(orient_buffer+idx))
			TransformNV21(frame_buffer+idx*elemSize, data, image_w, image_h, NULL, 0, 0, 1);
		else if(3 == *(orient_buffer+idx))
			TransformNV21(frame_buffer+idx*elemSize, data, image_w, image_h, NULL, 1, 1, 1);
		else if(2 == *(orient_buffer+idx))
			TransformNV21(frame_buffer+idx*elemSize, data, image_w, image_h, NULL, 1, 1, 0);
		else
			memcpy (data, frame_buffer+idx*elemSize, elemSize);
	}
	else
	{
		if (1 == *(orient_buffer+idx))
			TransformNV21(frame_buffer+idx*elemSize, data, image_w, image_h, NULL, 1, 1, 1);
		else if(3 == *(orient_buffer+idx))
			TransformNV21(frame_buffer+idx*elemSize, data, image_w, image_h, NULL, 0, 0, 1);
		else if(2 == *(orient_buffer+idx))
			TransformNV21(frame_buffer+idx*elemSize, data, image_w, image_h, NULL, 1, 1, 0);
		else
			memcpy (data, frame_buffer+idx*elemSize, elemSize);
	}

	env->ReleaseByteArrayElements(jdata, (jbyte*)data, 0);

	return jdata;
}

////get data from reserved buffer in JPEG format
//JNIEXPORT jbyteArray JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_GetFromBufferReservedNV21
//(
//	JNIEnv* env,
//	jobject pObj,
//	jint idx,
//	jint W,
//	jint H,
//	jint mirrored
//)
//{
//	unsigned char *data;
//
//	jbyteArray jdata;
//
//	jdata = env->NewByteArray(elemSizeReserved);
//	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);
//
//	if (1 != mirrored)
//	{
//		//if (1 == *(reserved_orient_buffer+idx))
//		if (1 == *(reserved_orient_buffer+idx))
//			TransformNV21(frame_buffer_reserved+idx*elemSizeReserved, data, image_wReserved, image_hReserved, NULL, 0, 0, 1);
//		else if(3 == *(reserved_orient_buffer+idx))
//			TransformNV21(frame_buffer_reserved+idx*elemSizeReserved, data, image_wReserved, image_hReserved, NULL, 1, 1, 1);
//		else if(2 == *(reserved_orient_buffer+idx))
//			TransformNV21(frame_buffer_reserved+idx*elemSizeReserved, data, image_wReserved, image_hReserved, NULL, 1, 1, 0);
//		else
//			memcpy (data, frame_buffer_reserved+idx*elemSizeReserved, elemSizeReserved);
//		//memcpy (data, frame_buffer_reserved+idx*elemSizeReserved, elemSizeReserved);
//	}
//	else
//	{
//		//if (1 == *(reserved_orient_buffer+idx))
//		if (1 == *(reserved_orient_buffer+idx))
//			TransformNV21(frame_buffer_reserved+idx*elemSizeReserved, data, image_wReserved, image_hReserved, NULL, 1, 1, 1);
//		else if(3 == *(reserved_orient_buffer+idx))
//			TransformNV21(frame_buffer_reserved+idx*elemSizeReserved, data, image_wReserved, image_hReserved, NULL, 0, 0, 1);
//		else if(2 == *(reserved_orient_buffer+idx))
//			TransformNV21(frame_buffer_reserved+idx*elemSizeReserved, data, image_wReserved, image_hReserved, NULL, 1, 1, 0);
//		else
//			memcpy (data, frame_buffer_reserved+idx*elemSizeReserved, elemSizeReserved);
//	}
//
//	env->ReleaseByteArrayElements(jdata, (jbyte*)data, JNI_ABORT);
//
//	return jdata;
//}

////get data from reserved buffer in JPEG format without any rotation. ONLY FOR SLOW!!!
//JNIEXPORT jbyteArray JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_GetFromBufferSimpleReservedNV21
//(
//	JNIEnv* env,
//	jobject pObj,
//	jint idx,
//	jint W,
//	jint H
//)
//{
//	unsigned char *data;
//	jbyteArray jdata = env->NewByteArray(0);
//
//	jdata = env->NewByteArray(*(reserved_len_buffer + idx));
//	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);
//	memcpy (data, frame_buffer_reserved+idx*elemSizeReserved, *(reserved_len_buffer + idx));
//
//	env->ReleaseByteArrayElements(jdata, (jbyte*)data, JNI_ABORT);
//
//	return jdata;
//}

//get data from buffer in JPEG format without any rotation. ONLY FOR SLOW!!!
JNIEXPORT jbyteArray JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_GetFromBufferSimpleNV21
(
	JNIEnv* env,
	jobject pObj,
	jint idx,
	jint W,
	jint H
)
{
	unsigned char *data;
	jbyteArray jdata = env->NewByteArray(0);

	jdata = env->NewByteArray(*(len_buffer + idx));
	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);
	memcpy (data, frame_buffer+idx*elemSize, *(len_buffer + idx));

	env->ReleaseByteArrayElements(jdata, (jbyte*)data, 0);

	return jdata;
}

////free reserved buffer
//JNIEXPORT jboolean JNICALL Java_com_almalence_plugins_capture_preshot_PreShot_FreeBufferReserved
//(
//	JNIEnv* env,
//	jobject pObj
//)
//{
//	if (!frame_buffer_reserved)
//	{
//		return 0;
//	}
//
//	free(frame_buffer_reserved);
//	frame_buffer_reserved = 0;
//
//	if (!reserved_orient_buffer)
//	{
//		return 0;
//	}
//
//	free(reserved_orient_buffer);
//	reserved_orient_buffer = 0;
//
//	free(reserved_len_buffer);
//	reserved_len_buffer = 0;
//
//	isReservedAllocated = 0;
//	__android_log_print(ANDROID_LOG_ERROR, "Allocation", "Buffers reserved freed");
//
//	return 1;
//}

}

