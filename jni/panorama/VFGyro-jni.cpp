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
#include <stdlib.h>
#include <stdint.h>
#include <string.h>
#include <jni.h>
#include <android/log.h>

#define restrict
#include "almashot.h"
#include "aligner.h"

#define MAX_FRAME_PIXELS	520000	// max resolution 960x540 (eg 960x720 will be scaled down to 480x360)
#define N_FRAMES			16

#define PI					3.1415926535897932384f


int params_updated = 1;
int almashot_inited = 0;

int64_t timestamp = 0;
float radians[3];

int frame_width, frame_height;
int frame_width_ds, frame_height_ds;
int ds = 0;
float normX, normY, normZ;

Uint8 frame_buf[N_FRAMES][MAX_FRAME_PIXELS];
Int32 frame_sharp[N_FRAMES];
Int32 frame_dx[N_FRAMES];
Int32 frame_dy[N_FRAMES];
Int32 frame_rot[N_FRAMES];

Uint8 scratch[SCRATCH_SIZE];

int frame_idx, base_idx;
//Uint8 cur_frame[MAX_FRAME_PIXELS];
//Uint8 prev_frame[MAX_FRAME_PIXELS];

extern "C"
{

JNIEXPORT void JNICALL Java_com_almalence_plugins_capture_panoramaaugmented_VfGyroSensor_Initialize
(
	JNIEnv* env,
	jobject thiz
)
{
	//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "in Initialize");

	if (!almashot_inited)
	{
		base_idx = frame_idx = 0;
		memset(frame_sharp, 0, N_FRAMES*sizeof(Int32));
		memset(frame_dx, 0, N_FRAMES*sizeof(Int32));
		memset(frame_dy, 0, N_FRAMES*sizeof(Int32));
		memset(frame_rot, 0, N_FRAMES*sizeof(Int32));

		AlmaShot_Initialize(0);
		almashot_inited = 1;

		//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "Initialized");
	}
}

// debug
//Uint8 *dbg_ptr[1000];
//int n_dbg = 0;

JNIEXPORT void JNICALL Java_com_almalence_plugins_capture_panoramaaugmented_VfGyroSensor_Release
(
	JNIEnv* env,
	jobject thiz
)
{
	//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "in Release");

	if (almashot_inited)
	{
		AlmaShot_Release();
		almashot_inited = 0;

		//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "Released");

		/*
		// debug
		for (int i=0; i<n_dbg; ++i)
		{
			char str[256];
			FILE *f;

			sprintf(str, "/sdcard/DCIM/ppp/%03d.bin", i);
			f = fopen(str, "wb");
			fwrite (dbg_ptr[i], frame_width_ds*frame_height_ds, 1, f);
			fclose(f);
		}
		*/
	}
}

JNIEXPORT void JNICALL Java_com_almalence_plugins_capture_panoramaaugmented_VfGyroSensor_SetFrameParameters
(
	JNIEnv* env,
	jobject thiz,
	jint w,
	jint h,
	jfloat horz_FOV,
	jfloat vert_FOV
)
{
	if (!almashot_inited)
	{
		AlmaShot_Initialize(0);
		almashot_inited = 1;
	}

	ds = 0;
	while ((w>>ds)*(h>>ds) > MAX_FRAME_PIXELS) ++ds;

	frame_width = w;
	frame_height = h;

	frame_width_ds = w>>ds;
	frame_height_ds = h>>ds;

	normX = 1e9 * horz_FOV * PI/180 / 256 / frame_width_ds;
	normY = 1e9 * vert_FOV * PI/180 / 256 / frame_height_ds;
	normZ = 1e9 / 256 / 256;

	//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "SetFrameParameters: w:%d h:%d ds:%d hFOV:%3.2f vFOV:%3.2f", w, h, ds, horz_FOV, vert_FOV);
	//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "SetFrameParameters: normX:%3.2f normY:%3.2f normZ:%3.2f", normX, normY, normZ);

	params_updated = 1;
}

//#define ROLLING_BASE_FRAME

JNIEXPORT void JNICALL Java_com_almalence_plugins_capture_panoramaaugmented_VfGyroSensor_Update
(
	JNIEnv* env,
	jobject thiz,
	jbyteArray data,
	jlong stamp
)
{
	int i, j, y;
	Uint8 *cur_frame_in;
	Uint8 *in[2];
	Int32 dx[2]={0,0};
	Int32 dy[2]={0,0};
	Int32 rot[2]={0,0};
	Int32 sharp[2]={0,0};
	__int64_t dt;
	int prev_idx, old_base_idx;
	Int32 best_sharp;

	cur_frame_in = (unsigned char*)env->GetByteArrayElements(data, NULL);

	//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "11: %d %d %d", ds, cur_frame_in, data);

	//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "OMP --- N cores: %d Max Threads: %d\n", omp_get_num_procs(), omp_get_max_threads());

	if (ds)
	{
		//#pragma omp parallel for
		for (y=0; y<frame_height_ds; ++y)
		{
			int x, xx, yy;
			int sum;

			for (x=0; x<frame_width_ds; ++x)
			{
				sum = 0;
				for (yy=0; yy<(1<<ds); ++yy)
					for (xx=0; xx<(1<<ds); ++xx)
						sum += cur_frame_in[x*(1<<ds)+xx + (y*(1<<ds)+yy)*frame_width];

				frame_buf[frame_idx][x+y*frame_width_ds] = sum >> (2*ds);
			}
		}
	}
	else
		memcpy (frame_buf[frame_idx], cur_frame_in, frame_width_ds*frame_height_ds);

	/*
	// debug
	dbg_ptr[n_dbg] = (Uint8*)malloc(frame_width_ds*frame_height_ds);
	memcpy (dbg_ptr[n_dbg], cur_frame_in, frame_width_ds*frame_height_ds);
	++n_dbg;
	*/

	// dt is in nano-seconds
	dt = stamp-timestamp;

	if ((!params_updated) && (dt>0))
	{
		prev_idx = (frame_idx+N_FRAMES-1)&(N_FRAMES-1);

		in[0] = frame_buf[base_idx];
		in[1] = frame_buf[frame_idx];

		AlmaShot_EstimateTranslationAndRotation(in, dx, dy, rot, sharp, frame_width_ds, frame_height_ds, 0, 2, 3, 1, scratch); // 4, 1);

		frame_sharp[frame_idx] = sharp[1];
		frame_dx[frame_idx] = dx[1];
		frame_dy[frame_idx] = dy[1];
		frame_rot[frame_idx] = rot[1];

#ifdef ROLLING_BASE_FRAME
		radians[0] = -(float)(dx[1]) * normX/dt;
		radians[1] = (float)(dy[1]) * normY/dt;
		radians[2] = (float)(rot[1]) * normZ/dt;
#else
		// compute radians from dx,dy,rot and timestamp
		radians[0] = -(float)(dx[1]-frame_dx[prev_idx]) * normX/dt;
		radians[1] = (float)(dy[1]-frame_dy[prev_idx]) * normY/dt;
		radians[2] = (float)(rot[1]-frame_rot[prev_idx]) * normZ/dt;
#endif

		//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "dx:%d dy:%d", dx[1]/256, dy[1]/256);
		//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "rot:%d", rot[1]);
		//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "dt:%d", (int)dt);

#ifndef ROLLING_BASE_FRAME
		// select new base frame if displacement is high or base frame is too many frames behind
		if ((abs(dx[1]) > 256*frame_width_ds/8 ) ||
			(abs(dy[1]) > 256*frame_height_ds/8) ||
			(abs(rot[1]) > 2*PI/180*256*256 ) ||
			(((frame_idx+N_FRAMES-base_idx)&(N_FRAMES-1)) > N_FRAMES-2))
		{
			//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "dx:%d dy:%d rot:%d", (abs(dx[1]) > 256*frame_width_ds/8 ), (abs(dy[1]) > 256*frame_height_ds/8), (abs(rot[1]) > 2*PI/180*256*256 ));
			//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "delta:%d", (frame_idx+N_FRAMES-base_idx)&(N_FRAMES-1));

			// select new base frame from the previous 4 frames,
			// will use the sharpest frame provided the displacement from current is not too high
			best_sharp = frame_sharp[frame_idx];
			old_base_idx = base_idx;
			base_idx = frame_idx;

			for (i=(frame_idx+N_FRAMES-1)&(N_FRAMES-1), j=0; (i!=old_base_idx) && (j<4); ++j, i=(i+N_FRAMES-1)&(N_FRAMES-1))
			{
				if ((abs(frame_dx[i]-dx[1]) > 256*frame_width_ds/8 ) ||
					(abs(frame_dy[i]-dy[1]) > 256*frame_height_ds/8) ||
					(abs(frame_rot[i]-rot[1]) > 2*PI/180*256*256 ))
						break;

				if (frame_sharp[i] > best_sharp)
				{
					best_sharp = frame_sharp[i];
					base_idx = i;
				}
			}

			//__android_log_print(ANDROID_LOG_INFO, "AlmaShot", "idxes: %d %d %d", old_base_idx, base_idx, frame_idx);

			for (i=(base_idx+1)&(N_FRAMES-1); base_idx!=frame_idx; i=(i+1)&(N_FRAMES-1))
			{
				frame_dx[i] -= frame_dx[base_idx];
				frame_dy[i] -= frame_dy[base_idx];
				frame_rot[i] -= frame_rot[base_idx];

				if (i==frame_idx) break;
			}

			frame_dx[base_idx] = frame_dy[base_idx] = frame_rot[base_idx] = 0;
		}
#endif
	}
	else
	{
		radians[0] = 0;
		radians[1] = 0;
		radians[2] = 0;
		base_idx = frame_idx;
		frame_sharp[base_idx] = 0;
		frame_dx[base_idx] = 0;
		frame_dy[base_idx] = 0;
		frame_rot[base_idx] = 0;
		params_updated = 0;
	}

#ifdef ROLLING_BASE_FRAME
	base_idx = frame_idx;
#endif
	frame_idx = (frame_idx+1)&(N_FRAMES-1);
	timestamp = stamp;

	env->ReleaseByteArrayElements(data, (jbyte*)cur_frame_in, JNI_ABORT);
}


JNIEXPORT jlong JNICALL Java_com_almalence_plugins_capture_panoramaaugmented_VfGyroSensor_Get
(
	JNIEnv* env,
	jobject thiz,
	jfloatArray vals
)
{
	float * rad_out;

	rad_out = (float*)env->GetFloatArrayElements(vals, NULL);

	memcpy(rad_out, radians, 3*sizeof(float));

	env->ReleaseFloatArrayElements(vals, (jfloat*)rad_out, JNI_ABORT);

	return timestamp;
}


};
