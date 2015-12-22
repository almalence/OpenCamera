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


extern "C" {


JNIEXPORT jint JNICALL Java_com_almalence_SwapHeap_SwapToHeap
(
	JNIEnv* env,
	jobject,
	jbyteArray jdata
)
{
	int data_length;
	unsigned char *heap, *data;

	data_length = env->GetArrayLength(jdata);
	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);


	heap = (unsigned char *)malloc(data_length);
	if (heap)
		memcpy (heap, data, data_length);

	env->ReleaseByteArrayElements(jdata, (jbyte*)data, JNI_ABORT);

	return (jint)heap;
}

JNIEXPORT jint JNICALL Java_com_almalence_SwapHeap_SwapYuvToHeap
(
	JNIEnv* env,
	jobject,
	jint jdata,
	jint jdata_length
)
{
	unsigned char *heap;

	heap = (unsigned char *)malloc(jdata_length);
	if (heap)
		memcpy (heap, (unsigned char*)jdata, jdata_length);

	return (jint)heap;
}


JNIEXPORT jbyteArray JNICALL Java_com_almalence_SwapHeap_CopyFromHeap
(
	JNIEnv* env,
	jobject,
	jint jheap,
	jint jdata_length
)
{
	unsigned char *heap, *data;
	jbyteArray jdata;

	jdata = env->NewByteArray(jdata_length);

	heap = (unsigned char *)jheap;

	data = (unsigned char*)env->GetByteArrayElements(jdata, NULL);

	memcpy (data, heap, jdata_length);

	env->ReleaseByteArrayElements(jdata, (jbyte*)data, 0);

	return jdata;
}

JNIEXPORT jbyteArray JNICALL Java_com_almalence_SwapHeap_SwapFromHeap
(
	JNIEnv* env,
	jobject thiz,
	jint jheap,
	jint jdata_length
)
{
	jbyteArray jdata = Java_com_almalence_SwapHeap_CopyFromHeap(env, thiz, jheap, jdata_length);

	free ((void*)jheap);

	return jdata;
}

JNIEXPORT jboolean JNICALL Java_com_almalence_SwapHeap_FreeFromHeap
(
	JNIEnv* env,
	jobject,
	jint jheap
)
{
	free ((void*)jheap);

	return 1;
}


}
