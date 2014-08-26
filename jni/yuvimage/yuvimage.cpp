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
#include <jni.h>
#include <android/log.h>

#include "CreateJavaOutputStreamAdaptor.h"
#include "YuvToJpegEncoder.h"

extern "C" {
JNIEXPORT jboolean JNICALL Java_com_mobiroo_n_almalence_YuvImage_SaveJpegFreeOut
(
	JNIEnv* env, jobject, jint jout,
	int format, int width, int height, jintArray offsets,
	jintArray strides, int jpegQuality, jobject jstream,
	jbyteArray jstorage
);

};

JNIEXPORT jboolean JNICALL Java_com_mobiroo_n_almalence_YuvImage_SaveJpegFreeOut
(
	JNIEnv* env, jobject, jint jout,
	int format, int width, int height, jintArray offsets,
	jintArray strides, int jpegQuality, jobject jstream,
	jbyteArray jstorage
)
{
	jbyte* OutPic;

	OutPic = (jbyte*)jout;

	SkWStream* strm = CreateJavaOutputStreamAdaptor(env, jstream, jstorage);

	jint* imgOffsets = env->GetIntArrayElements(offsets, NULL);
	jint* imgStrides = env->GetIntArrayElements(strides, NULL);
	YuvToJpegEncoder* encoder = YuvToJpegEncoder::create(format, imgStrides);
	if (encoder == NULL)
	{
		free(OutPic);
		return false;
	}

	bool result = encoder->encode(strm, OutPic, width, height, imgOffsets, jpegQuality);

	delete encoder;
	env->ReleaseIntArrayElements(offsets, imgOffsets, 0);
	env->ReleaseIntArrayElements(strides, imgStrides, 0);

	return result;
}
