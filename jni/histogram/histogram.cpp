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

#define	CLIP8(x)			( (x)<0 ? 0 : (x)>255 ? 255 : (x) )
#define CSC_R(Y,V)			CLIP8((128*(Y)+176*((V)-128)) >> 7 )
#define CSC_B(Y,U)			CLIP8((128*(Y)+222*((U)-128)) >> 7 )
#define CSC_G(Y,U,V)		CLIP8((128*(Y)-89*((V)-128)-43*((U)-128)) >> 7 )

#define get_Ysz(in, x,y)	((in)[(x)+(y)*w])
#define get_Usz(in, x, y)	((in)[w*h+((x)|1)+((y)/2)*w])
#define get_Vsz(in, x, y)	((in)[w*h+((x)&~1)+((y)/2)*w])


inline void makeHistogram(unsigned char *yuv420sp, int width, int height, int histHeight, int *histFacts)
{
	int i, j;
	int maxY;

	memset(histFacts, 0, 256*sizeof(int));

	for (i=0; i < height*width; i+=8)		// analyze one pixel out of 8 (for speed-up)
		histFacts[yuv420sp[i]]++;

	maxY = 0;
	for(i = 0; i < 256; i++)
		if(histFacts[i] > maxY)
			maxY = histFacts[i];

	int maxNorm = 256 * histHeight / maxY;
	for(i = 0; i < 256; i++)
		histFacts[i] = (histFacts[i]*maxNorm) >> 8;
}


inline void makeRGBHistogram(unsigned char *yuv420sp, int w, int h, int histHeight, int *histFactsR, int *histFactsG, int *histFactsB)
{
	int i, j, x, y;
	int r,g,b;
	int Y, U, V;
	int maxY;

	memset(histFactsR, 0, 256*sizeof(int));
	memset(histFactsG, 0, 256*sizeof(int));
	memset(histFactsB, 0, 256*sizeof(int));

	for (y=0; y<h; y+=2)		// analyze one pixel out of 8 (for speed-up)
	{
		for (x=0; x<w; x+=4)
		{
			Y = get_Ysz(yuv420sp, x, y);
			U = get_Usz(yuv420sp, x, y);
			V = get_Vsz(yuv420sp, x, y);

			r = CSC_R(Y, V);
			g = CSC_G(Y, U, V);
			b = CSC_B(Y, U);

			histFactsR[r]++;
			histFactsG[g]++;
			histFactsB[b]++;
		}
	}

	maxY = 0;
	for(i = 0; i < 256; i++)
	{
		if(histFactsR[i] > maxY) maxY = histFactsR[i];
		if(histFactsG[i] > maxY) maxY = histFactsG[i];
		if(histFactsB[i] > maxY) maxY = histFactsB[i];
	}

	int maxNorm = 256 * histHeight / maxY;
	for(i = 0; i < 256; i++)
	{
		histFactsR[i] = (histFactsR[i]*maxNorm) >> 8;
		histFactsG[i] = (histFactsG[i]*maxNorm) >> 8;
		histFactsB[i] = (histFactsB[i]*maxNorm) >> 8;
	}
}


extern "C" JNIEXPORT void JNICALL Java_com_almalence_plugins_vf_histogram_Histogram_createHistogram(
		JNIEnv *env, jclass clazz, jbyteArray ain, jintArray afacts, jint width,	jint height, jint histWidth, jint histHeight)
{
	jbyte *cImageIn = env->GetByteArrayElements(ain, 0);
	jint *cFacts = env->GetIntArrayElements(afacts, 0);

	makeHistogram((unsigned char*)cImageIn, width, height, histHeight, cFacts);

	env->ReleaseByteArrayElements(ain, cImageIn, 0);
	env->ReleaseIntArrayElements(afacts, cFacts, 0);
}


extern "C" JNIEXPORT void JNICALL Java_com_almalence_plugins_vf_histogram_Histogram_createRGBHistogram(
		JNIEnv *env, jclass clazz, jbyteArray ain, jintArray afactsR, jintArray afactsG, jintArray afactsB, jint width, jint height, jint histWidth, jint histHeight)
{
	jbyte *cImageIn = env->GetByteArrayElements(ain, 0);
	jint *cFactsR = env->GetIntArrayElements(afactsR, 0);
	jint *cFactsG = env->GetIntArrayElements(afactsG, 0);
	jint *cFactsB = env->GetIntArrayElements(afactsB, 0);

	makeRGBHistogram((unsigned char*)cImageIn, width, height, histHeight, cFactsR, cFactsG, cFactsB);

	env->ReleaseByteArrayElements(ain, cImageIn, 0);
	env->ReleaseIntArrayElements(afactsR, cFactsR, 0);
	env->ReleaseIntArrayElements(afactsG, cFactsG, 0);
	env->ReleaseIntArrayElements(afactsB, cFactsB, 0);
}
