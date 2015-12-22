/* ------------------------------------------------------------------------- *\

 Almalence, Inc.
 3803 Mt. Bonnell Rd
 Austin, 78731
 Texas, USA

 CONFIDENTIAL: CONTAINS CONFIDENTIAL PROPRIETARY INFORMATION OWNED BY
 ALMALENCE, INC., INCLUDING BUT NOT LIMITED TO TRADE SECRETS,
 KNOW-HOW, TECHNICAL AND BUSINESS INFORMATION. USE, DISCLOSURE OR
 DISTRIBUTION OF THE SOFTWARE IN ANY FORM IS LIMITED TO SPECIFICALLY
 AUTHORIZED LICENSEES OF ALMALENCE, INC. ANY UNAUTHORIZED DISCLOSURE
 IS A VIOLATION OF STATE, FEDERAL, AND INTERNATIONAL LAWS.
 BOTH CIVIL AND CRIMINAL PENALTIES APPLY.

 DO NOT DUPLICATE. UNAUTHORIZED DUPLICATION IS A VIOLATION OF STATE,
 FEDERAL AND INTERNATIONAL LAWS.

 USE OF THE SOFTWARE IS AT YOUR SOLE RISK. THE SOFTWARE IS PROVIDED ON AN
 "AS IS" BASIS AND WITHOUT WARRANTY OF ANY KIND. TO THE MAXIMUM EXTENT
 PERMITTED BY LAW, ALMALENCE EXPRESSLY DISCLAIM ALL WARRANTIES AND
 CONDITIONS OF ANY KIND, WHETHER EXPRESS OR IMPLIED, INCLUDING, BUT NOT
 LIMITED TO THE IMPLIED WARRANTIES AND CONDITIONS OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT.

 ALMALENCE DOES NOT WARRANT THAT THE SOFTWARE WILL MEET YOUR REQUIREMENTS,
 OR THAT THE OPERATION OF THE SOFTWARE WILL BE UNINTERRUPTED OR ERROR-FREE,
 OR THAT DEFECTS IN THE SOFTWARE WILL BE CORRECTED. UNDER NO CIRCUMSTANCES,
 INCLUDING NEGLIGENCE, SHALL ALMALENCE, OR ITS DIRECTORS, OFFICERS,
 EMPLOYEES OR AGENTS, BE LIABLE TO YOU FOR ANY INCIDENTAL, INDIRECT,
 SPECIAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE, MISUSE OR
 INABILITY TO USE THE SOFTWARE OR RELATED DOCUMENTATION.

 COPYRIGHT 2010-2012, ALMALENCE, INC.

 ---------------------------------------------------------------------------

 Viewfinder-based gyroscope

\* ------------------------------------------------------------------------- */

#include <stdio.h>
#include <math.h>
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#include "almashot.h"
#include "aligner.h"

#define MAX_FRAME_PIXELS	520000	// max resolution 960x540 (eg 960x720 will be scaled down to 480x360)

#define PI					3.1415926535897932384f


int almashot_inited = 0;
int fov_matched = 0;

float front_frame_fov;
int front_frame_width, front_frame_height;
int match_width, match_height;
int rear_border_lr, rear_border_tb;
Uint8 * front_frame_buf = NULL;
Uint8 * rear_frame_buf = NULL;

int frame_width_ds, frame_height_ds;
int ds = 0;


Uint8 scratch[SCRATCH_SIZE];

extern "C"
{

JNIEXPORT void JNICALL Java_com_almalence_plugins_capture_hiresportrait_HiresPortraitCapturePlugin_Initialize
(
	JNIEnv* env,
	jobject thiz
)
{
	//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "in Initialize");

	if (!almashot_inited)
	{
		AlmaShot_Initialize(0);
		almashot_inited = 1;

		//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "Initialized");
	}
}


JNIEXPORT void JNICALL Java_com_almalence_plugins_capture_hiresportrait_HiresPortraitCapturePlugin_Release
(
	JNIEnv* env,
	jobject thiz
)
{
	if (almashot_inited)
	{
		AlmaShot_Release();
		almashot_inited = 0;
	}

	if (front_frame_buf) {free(front_frame_buf); front_frame_buf = NULL;}
	if (rear_frame_buf) {free(rear_frame_buf); rear_frame_buf = NULL;}
}


// Note:
// input: NV21 data from VF
// return: 0 = ok, 1 = no memory
JNIEXPORT int JNICALL Java_com_almalence_plugins_capture_hiresportrait_HiresPortraitCapturePlugin_SetFrontFrame
(
	JNIEnv* env,
	jobject thiz,
	jbyteArray data,
	jint w,
	jint h,
	jfloat horz_FOV
)
{
	Uint8 *frame_in;

	if (!almashot_inited)
	{
		AlmaShot_Initialize(0);
		almashot_inited = 1;
	}

	fov_matched = 0;

	front_frame_width = w;
	front_frame_height = h;
	front_frame_fov = horz_FOV;

	// only keeping intensity part of the frame, colors discarded
	front_frame_buf = (Uint8*)malloc(w*h);
	if (front_frame_buf == NULL) return 1;

	frame_in = (unsigned char*)env->GetByteArrayElements(data, NULL);

	memcpy (front_frame_buf, frame_in, w*h);

	env->ReleaseByteArrayElements(data, (jbyte*)frame_in, JNI_ABORT);

	return 0;
}


// Note:
// input: NV21 data from VF
// return: distance in percentages of frame half-width (0=perfect alignment, 100=displaced half-way
// return confidence measure
JNIEXPORT int JNICALL Java_com_almalence_plugins_capture_hiresportrait_HiresPortraitCapturePlugin_CheckRearAlignment
(
	JNIEnv* env,
	jobject thiz,
	jbyteArray data,
	jint w,
	jint h,
	jfloat horz_FOV
)
{
	int x, y;
	int front_border_lr, front_border_tb;
	float min_fov, front_vert_FOV, rear_vert_FOV;
	Uint8 *cur_frame_in;
	Uint8 * fscaled;
	Uint8 *in[2];
	Int32 dx[2]={0,0};
	Int32 dy[2]={0,0};
	Int32 confidence[2];

	cur_frame_in = (unsigned char*)env->GetByteArrayElements(data, NULL);

	if (!fov_matched)
	{
		// figure whether front or rear camera have the smallest fov and set
		// re-scaled image sizes and crop regions accordingly

		if (front_frame_fov > horz_FOV) min_fov = horz_FOV;
			else min_fov = front_frame_fov;

		front_border_lr = (int)(front_frame_width*(1-min_fov/front_frame_fov)/2);
		rear_border_lr = (int)(w*(1-min_fov/horz_FOV)/2);

		front_vert_FOV = front_frame_fov*front_frame_height/front_frame_width;
		rear_vert_FOV = horz_FOV*h/w;

		if (front_vert_FOV > rear_vert_FOV) min_fov = rear_vert_FOV;
			else min_fov = front_vert_FOV;

		front_border_tb = (int)(front_frame_height*(1-min_fov/front_vert_FOV)/2);
		rear_border_tb = (int)(h*(1-min_fov/rear_vert_FOV)/2);

		//match_width = w - rear_border_lr*2;
		//match_height = h - rear_border_tb*2;
		match_width = 128;
		match_height = 128;

		fscaled = (Uint8 * )malloc(match_width*match_height);
		if (fscaled == NULL) return 0;
		rear_frame_buf = (Uint8 * )malloc(match_width*match_height);
		if (rear_frame_buf == NULL) {free(fscaled); return 0;}

		// simple nearest-neighbor re-scaling
		for (y=0; y<match_height; ++y)
			for (x=0; x<match_width; ++x)
			{
				fscaled[x+y*match_width] =
					front_frame_buf[x*(front_frame_width-2*front_border_lr)/match_width+front_border_lr+(y*(front_frame_height-2*front_border_tb)/match_height+front_border_tb)*front_frame_width];
			}

		free(front_frame_buf);
		front_frame_buf = fscaled;

		fov_matched = 1;
	}

	// crop rear frame input
	// simple nearest-neighbor re-scaling
	for (y=0; y<match_height; ++y)
		for (x=0; x<match_width; ++x)
		{
			rear_frame_buf[x+y*match_width] =
				cur_frame_in[x*(w-2*rear_border_lr)/match_width+rear_border_lr+(y*(h-2*rear_border_tb)/match_height+rear_border_tb)*w];
		}

	// crop rear frame input
	//for (y=0; y<match_height; ++y)
	//	memcpy (&rear_frame_buf[y*match_width], &cur_frame_in[rear_border_lr+(y+rear_border_tb)*w], match_width);

	in[0] = front_frame_buf;
	in[1] = rear_frame_buf;

	AlmaShot_EstimateGlobalTranslation(in, match_width, match_height, dx, dy, 0, 2, confidence, scratch);

	env->ReleaseByteArrayElements(data, (jbyte*)cur_frame_in, JNI_ABORT);

	//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "distance: %d", (int)(sqrtf(dx[1]*dx[1]+dy[1]*dy[1])*100/(match_width/2)));
	//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "confidence: %d", confidence[1]);

	if (confidence[1] < (1<<18))
		return 100;
	else
		return (int)(sqrtf(dx[1]*dx[1]+dy[1]*dy[1])*100/(match_width/2));
}


};
