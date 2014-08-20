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

package com.almalence.plugins.capture.panoramaaugmented;

import android.annotation.SuppressLint;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.FloatMath;

@SuppressLint("FloatMath")
public class AugmentedRotationListener implements SensorEventListener
{
	public interface AugmentedRotationReceiver
	{
		public void onRotationChanged(float[] transformation_matrix);
	}

	private static final float			NS2S						= 1.0f / 1000000000.0f;

	private static final float			VF_GYRO_SPEED_LIMIT			= 5.f;							// radians/sec

	private static final float			GYRO_FUSION_CF				= 0.02f;
	private static final float			GRAVITY_FILTER_CF			= 0.1f;

	private final Object				receiverSynchObject			= new Object();
	private AugmentedRotationReceiver	receiver;

	// filter state should be pre-filled with zeros (where to put
	// initialization??)
	private static final int			ACC_FILT_LEN				= 8;
	private final float[][]				acc_filter					= new float[ACC_FILT_LEN][3];	// filter
																									// state
																									// for
																									// acceleration
																									// filter
	private int							acc_filt_idx				= 0;

	private final float[]				angle_magnetic				= new float[3];
	private final float[]				angleLF_magnetic			= new float[3];
	private final float[]				angle_initial				= new float[3];
	private final float[]				rate_magnetic				= new float[3];
	private final float[]				filt_magnetic				= new float[3];
	private final float[]				data_magnetic				= new float[3];
	private final float[]				data_acceleration			= new float[3];
	private final float[]				data_gyroscope				= new float[3];

	private final boolean[]				dataSynchObject				= new boolean[2];

	private boolean						accelerometerValueIsFresh	= true;

	private int							magneticDataValid			= 0;
	private long						timestamp_magnetic;
	private long						timestamp_gyro;

	private final float[]				gyroOrientation				= new float[3];
	private float[]						accMagOrientation			= null;
	private float[]						fusedOrientation			= new float[3];
	private float[]						gyroMatrix					= null;

	private final boolean				remap;
	private final boolean				softGyro;
	
	private boolean						updateDrift;
	

	public AugmentedRotationListener(final boolean remap, final boolean softGyro)
	{
		this.remap = remap;
		this.softGyro = softGyro;
		this.updateDrift = true;
		this.filt_magnetic[0] = this.filt_magnetic[1] = this.filt_magnetic[2] = 0;
	}

	@Override
	public void onAccuracyChanged(final Sensor sensor, final int accuracy)
	{
	}

	@Override
	public void onSensorChanged(final SensorEvent event)
	{
		final AugmentedRotationReceiver receiver;

		synchronized (this.receiverSynchObject)
		{
			receiver = this.receiver;
		}

		if (receiver != null)
		{
			if ((this.remap) && (event.sensor != null)) // null means VfGyro
			{
				final float remap_t = -event.values[0];
				event.values[0] = event.values[1];
				event.values[1] = remap_t;
			}

			synchronized (this.dataSynchObject)
			{
				if (event.sensor == null) // null means VfGyro
				{
					if (softGyro)
					{
						this.magnetoFixLarge(event);
						this.gyroFunction(event);
					}
				} else
				{
					switch (event.sensor.getType())
					{
					case Sensor.TYPE_GRAVITY:
					case Sensor.TYPE_ACCELEROMETER:
						System.arraycopy(event.values, 0, this.data_acceleration, 0, 3);
						this.calculateAccMagOrientation();
						accelerometerValueIsFresh = true;
						break;

					case Sensor.TYPE_GYROSCOPE:
						VfGyroSensor.FixDrift(event.values, updateDrift);
						this.gyroFunction(event);
						accelerometerValueIsFresh = false;
						break;

					case Sensor.TYPE_MAGNETIC_FIELD:
						this.magnetoFunction(event);
						break;
					default:
						break;
					}
				}
			}
		}
	}

	public void setUpdateDrift(boolean updateDrift)
	{
		this.updateDrift = updateDrift;
	}
	
	public void setReceiver(final AugmentedRotationReceiver receiver)
	{
		synchronized (this.receiverSynchObject)
		{
			this.receiver = receiver;
		}
	}

	// discard acceleration measurements that are far from g,
	// i.e. when user started/stopped moving, or hand is shaking
	private void filterGravity(final float[] accel, final float[] gravity)
	{
		acc_filter[acc_filt_idx][0] = accel[0];
		acc_filter[acc_filt_idx][1] = accel[1];
		acc_filter[acc_filt_idx][2] = accel[2];

		float wsum;

		gravity[0] = 0;
		gravity[1] = 0;
		gravity[2] = 0;
		wsum = 0;
		int idx = acc_filt_idx;
		for (int i = 0; i < ACC_FILT_LEN; ++i)
		{
			float g = FloatMath.sqrt(acc_filter[idx][0] * acc_filter[idx][0] + acc_filter[idx][1] * acc_filter[idx][1]
					+ acc_filter[idx][2] * acc_filter[idx][2]);

			// the farther the measured g from 9.81 the less the weight in a
			// filter
			float w = 1.f / (Math.abs(g - SensorManager.STANDARD_GRAVITY) + GRAVITY_FILTER_CF);

			// the older the value from accelerometer - the lower the weight
			w *= ACC_FILT_LEN - i;

			wsum += w;

			gravity[0] += w * acc_filter[i][0];
			gravity[1] += w * acc_filter[i][1];
			gravity[2] += w * acc_filter[i][2];

			idx = (idx + ACC_FILT_LEN - 1) & (ACC_FILT_LEN - 1);
		}

		// normalize filtered result
		gravity[0] /= wsum;
		gravity[1] /= wsum;
		gravity[2] /= wsum;

		acc_filt_idx = (acc_filt_idx + 1) & (ACC_FILT_LEN - 1);
	}

	public void calculateAccMagOrientation()
	{
		if (this.accMagOrientation == null)
		{
			this.accMagOrientation = new float[3];
			this.accMagOrientation[0] = 0;
			this.accMagOrientation[1] = 0;
			this.accMagOrientation[2] = 0;
			this.gyroOrientation[0] = 1; // give some initial direction to get
											// meaningful results from fake
											// magneto
			this.gyroOrientation[1] = 0;
			this.gyroOrientation[2] = 0;
		}

		// pre-condition data from the sensors
		float[] gravity = new float[3]; // filtered acceleration

		// LP-filter data from accelerometer (nearly-discard data from
		// non-stationary samplings)
		filterGravity(this.data_acceleration, gravity);

		// generate fake magneto readings from gyroscope (invert gyro matrix to
		// get conversion from world coordinates to device coordinates)
		// inversion of rotation matrix is just it's transpose
		float[] fakeMagnetoMatrix = getRotationMatrixFromOrientation(this.gyroOrientation);
		float[] fakeMagneto = new float[3];

		fakeMagneto[0] = fakeMagnetoMatrix[3]; // compose vector of magnetic
												// field
		fakeMagneto[1] = fakeMagnetoMatrix[4]; // using axis pointing to north
												// (0,1,0)
		fakeMagneto[2] = fakeMagnetoMatrix[5]; // 1,4,7 transposed to 3,4,5

		float[] accMatrix = new float[9];
		boolean accMatrixGood = SensorManager.getRotationMatrix(accMatrix, null, gravity, fakeMagneto);

		if (accMatrixGood)
		{
			float[] accOrientation = new float[3];

			SensorManager.getOrientation(accMatrix, accOrientation);

			this.accMagOrientation[0] = accOrientation[0];
			this.accMagOrientation[1] = accOrientation[1];
			this.accMagOrientation[2] = accOrientation[2];
		}
	}

	public void magnetoFixLarge(final SensorEvent event)
	{
		if (magneticDataValid < 2)
			return;

		for (int i = 0; i < 3; ++i)
		{
			// correct both:
			// - high outbursts from vf-based gyro (above 5 radians/sec)
			// - high-velocity movements not captured by vf-gyro
			// (magnetometer seem reasonably stable at rates around 1
			// radian/sec)
			if ((float) Math.abs(event.values[i]) > VF_GYRO_SPEED_LIMIT)
				event.values[i] = this.rate_magnetic[i];
		}
	}

	public void magnetoFunction(final SensorEvent event)
	{
		// simple averaging iir-filter on magnetic field
		for (int i = 0; i < 3; ++i)
		{
			this.filt_magnetic[i] = event.values[i] = (this.filt_magnetic[i] + event.values[i]) / 2;
		}

		float norm = FloatMath.sqrt(event.values[0] * event.values[0] + event.values[1] * event.values[1]
				+ event.values[2] * event.values[2]);

		if (norm > 1) // normally earth magnetic field is 25-65 uTesla, but some
						// sensors are way-off in magnitude, setting limit as 1
						// uTesla here
		{
			for (int i = 0; i < 3; ++i)
				event.values[i] /= norm;

			int[] mag_remap = { 2, 0, 1 };

			if (magneticDataValid > 0)
			{
				float dT = (event.timestamp - timestamp_magnetic) * NS2S;

				for (int i = 0; i < 3; ++i)
				{
					int j = (i + 1) % 3;
					float mag_new = event.values[i] * event.values[i] + event.values[j] * event.values[j];
					float mag_old = this.data_magnetic[i] * this.data_magnetic[i] + this.data_magnetic[j]
							* this.data_magnetic[j];

					if ((mag_old > 0.01) && (mag_new > 0.01)) // note the
																// absence of
																// sqrt for
																// these
					{
						// compute rotation rate
						this.angle_magnetic[mag_remap[i]] = (float) Math.atan2(event.values[i], event.values[j]);

						float adiff = (this.angle_magnetic[mag_remap[i]] - (float) Math.atan2(this.data_magnetic[i],
								this.data_magnetic[j]));

						while (adiff > Math.PI)
							adiff -= 2 * Math.PI;
						while (adiff < -Math.PI)
							adiff += 2 * Math.PI;

						this.rate_magnetic[mag_remap[i]] = adiff / dT;

						// compute LP-filtered angle
						adiff = this.angle_magnetic[mag_remap[i]] - this.angleLF_magnetic[mag_remap[i]];
						while (adiff > Math.PI)
							adiff -= 2 * Math.PI;
						while (adiff < -Math.PI)
							adiff += 2 * Math.PI;

						if (Math.abs(adiff) > Math.PI * 3 / 180) // above 3
																	// degree
																	// difference
																	// - assume
																	// fast
																	// change in
																	// position
							this.angleLF_magnetic[mag_remap[i]] = this.angle_magnetic[mag_remap[i]];
						else
							this.angleLF_magnetic[mag_remap[i]] = (this.angleLF_magnetic[mag_remap[i]] * 15 + this.angle_magnetic[mag_remap[i]]) / 16;
					} else
						this.rate_magnetic[mag_remap[i]] = 0; // means unknown
				}
				magneticDataValid = 2;
			} else
			{
				for (int i = 0; i < 3; ++i)
				{
					int j = (i + 1) % 3;
					this.angle_initial[mag_remap[i]] = (float) Math.atan2(event.values[i], event.values[j]);
				}

				magneticDataValid = 1;
			}

			for (int i = 0; i < 3; ++i)
				this.data_magnetic[i] = event.values[i];
		} else
		{
			magneticDataValid = 0;
			this.filt_magnetic[0] = this.filt_magnetic[1] = this.filt_magnetic[2] = 0;
		}

		timestamp_magnetic = event.timestamp;

	}

	public void gyroFunction(final SensorEvent event)
	{
		if (this.accMagOrientation == null)
		{
			return;
		}

		if (this.gyroMatrix == null)
		{
			this.gyroMatrix = getRotationMatrixFromOrientation(this.accMagOrientation);
			System.arraycopy(this.accMagOrientation, 0, this.gyroOrientation, 0, 3);
		}

		float[] deltaVector = new float[4];
		if (this.timestamp_gyro != 0)
		{
			final float dT = (event.timestamp - this.timestamp_gyro) * NS2S;
			System.arraycopy(event.values, 0, this.data_gyroscope, 0, 3);
			getRotationVectorFromGyro(this.data_gyroscope, deltaVector, dT / 2.0f);
		}

		this.timestamp_gyro = event.timestamp;

		float[] deltaMatrix = new float[9];
		GetRotationMatrixFromVector(deltaMatrix, deltaVector);

		this.gyroMatrix = matrixMultiplication(this.gyroMatrix, deltaMatrix);

		SensorManager.getOrientation(this.gyroMatrix, this.gyroOrientation);

		this.calculateFusion(accelerometerValueIsFresh);

		final AugmentedRotationReceiver receiver;
		synchronized (this.receiverSynchObject)
		{
			receiver = this.receiver;
		}
		receiver.onRotationChanged(this.gyroMatrix);
	}

	private float fuseAngles(float nice, float dirty, final float compensation, float filtCf)
	{
		while (nice - (dirty + compensation) > Math.PI)
			dirty += 2 * Math.PI;
		while (nice - (dirty + compensation) < -Math.PI)
			dirty -= 2 * Math.PI;

		// if gyro (nice) is very far from accelerometer (dirty) - converge
		// faster
		if (nice - (dirty + compensation) > Math.PI * 5 / 180) // 5 degrees
			filtCf *= 5;

		float oneMinusCoeff = 1.0f - filtCf;

		nice = oneMinusCoeff * nice + filtCf * (dirty + compensation);

		if (nice < -Math.PI)
			nice += 2 * Math.PI;
		if (nice > Math.PI)
			nice -= 2 * Math.PI;

		return nice;
	}

	private void calculateFusion(boolean accelerometerValueIsFresh)
	{

		for (int i = 0; i < 3; ++i)
		{
			if (accelerometerValueIsFresh)
				this.fusedOrientation[i] = fuseAngles(this.gyroOrientation[i], this.accMagOrientation[i], 0,
						GYRO_FUSION_CF);
			else
				this.fusedOrientation[i] = fuseAngles(this.gyroOrientation[i], this.accMagOrientation[i], 0, 0);

		}

		// overwrite gyro matrix and orientation with fused orientation to
		// compensate gyro drift
		this.gyroMatrix = getRotationMatrixFromOrientation(this.fusedOrientation);
		System.arraycopy(this.fusedOrientation, 0, this.gyroOrientation, 0, 3);
	}

	private static void getRotationVectorFromGyro(final float[] gyroValues, final float[] deltaRotationVector,
			final float timeFactor)
	{
		float[] normValues = new float[3];

		// Calculate the angular speed of the sample
		float omegaMagnitude = FloatMath.sqrt(gyroValues[0] * gyroValues[0] + gyroValues[1] * gyroValues[1]
				+ gyroValues[2] * gyroValues[2]);

		// Normalize the rotation vector if it's big enough to get the axis
		if (omegaMagnitude != 0.0f)
		{
			normValues[0] = gyroValues[0] / omegaMagnitude;
			normValues[1] = gyroValues[1] / omegaMagnitude;
			normValues[2] = gyroValues[2] / omegaMagnitude;
		}

		// Integrate around this axis with the angular speed by the timestep
		// in order to get a delta rotation from this sample over the timestep
		// We will convert this axis-angle representation of the delta rotation
		// into a quaternion before turning it into the rotation matrix.
		float thetaOverTwo = omegaMagnitude * timeFactor;
		float sinThetaOverTwo = FloatMath.sin(thetaOverTwo);
		float cosThetaOverTwo = FloatMath.cos(thetaOverTwo);
		deltaRotationVector[0] = sinThetaOverTwo * normValues[0];
		deltaRotationVector[1] = sinThetaOverTwo * normValues[1];
		deltaRotationVector[2] = sinThetaOverTwo * normValues[2];
		deltaRotationVector[3] = cosThetaOverTwo;
	}

	private static float[] matrixMultiplication(float[] A, float[] B)
	{
		float[] result = new float[9];

		result[0] = A[0] * B[0] + A[1] * B[3] + A[2] * B[6];
		result[1] = A[0] * B[1] + A[1] * B[4] + A[2] * B[7];
		result[2] = A[0] * B[2] + A[1] * B[5] + A[2] * B[8];

		result[3] = A[3] * B[0] + A[4] * B[3] + A[5] * B[6];
		result[4] = A[3] * B[1] + A[4] * B[4] + A[5] * B[7];
		result[5] = A[3] * B[2] + A[4] * B[5] + A[5] * B[8];

		result[6] = A[6] * B[0] + A[7] * B[3] + A[8] * B[6];
		result[7] = A[6] * B[1] + A[7] * B[4] + A[8] * B[7];
		result[8] = A[6] * B[2] + A[7] * B[5] + A[8] * B[8];

		return result;
	}

	private static float[] getRotationMatrixFromOrientation(final float[] o)
	{
		float[] xM = new float[9];
		float[] yM = new float[9];
		float[] zM = new float[9];

		float sinX = FloatMath.sin(o[1]);
		float cosX = FloatMath.cos(o[1]);
		float sinY = FloatMath.sin(o[2]);
		float cosY = FloatMath.cos(o[2]);
		float sinZ = FloatMath.sin(o[0]);
		float cosZ = FloatMath.cos(o[0]);

		// rotation about x-axis (pitch)
		xM[0] = 1.0f;
		xM[1] = 0.0f;
		xM[2] = 0.0f;
		xM[3] = 0.0f;
		xM[4] = cosX;
		xM[5] = sinX;
		xM[6] = 0.0f;
		xM[7] = -sinX;
		xM[8] = cosX;

		// rotation about y-axis (roll)
		yM[0] = cosY;
		yM[1] = 0.0f;
		yM[2] = sinY;
		yM[3] = 0.0f;
		yM[4] = 1.0f;
		yM[5] = 0.0f;
		yM[6] = -sinY;
		yM[7] = 0.0f;
		yM[8] = cosY;

		// rotation about z-axis (azimuth)
		zM[0] = cosZ;
		zM[1] = sinZ;
		zM[2] = 0.0f;
		zM[3] = -sinZ;
		zM[4] = cosZ;
		zM[5] = 0.0f;
		zM[6] = 0.0f;
		zM[7] = 0.0f;
		zM[8] = 1.0f;

		// rotation order is y, x, z (roll, pitch, azimuth)
		float[] resultMatrix = matrixMultiplication(xM, yM);
		resultMatrix = matrixMultiplication(zM, resultMatrix);
		return resultMatrix;
	}

	private static void GetRotationMatrixFromVector(final float[] R, final float[] rotationVector)
	{
		final float q0;
		final float q1 = rotationVector[0];
		final float q2 = rotationVector[1];
		final float q3 = rotationVector[2];

		if (rotationVector.length == 4)
		{
			q0 = rotationVector[3];
		} else
		{
			final float q0_t = 1 - q1 * q1 - q2 * q2 - q3 * q3;
			q0 = (q0_t > 0) ? (float) Math.sqrt(q0_t) : 0;
		}

		float sq_q1 = 2 * q1 * q1;
		float sq_q2 = 2 * q2 * q2;
		float sq_q3 = 2 * q3 * q3;
		float q1_q2 = 2 * q1 * q2;
		float q3_q0 = 2 * q3 * q0;
		float q1_q3 = 2 * q1 * q3;
		float q2_q0 = 2 * q2 * q0;
		float q2_q3 = 2 * q2 * q3;
		float q1_q0 = 2 * q1 * q0;

		if (R.length == 9)
		{
			R[0] = 1 - sq_q2 - sq_q3;
			R[1] = q1_q2 - q3_q0;
			R[2] = q1_q3 + q2_q0;

			R[3] = q1_q2 + q3_q0;
			R[4] = 1 - sq_q1 - sq_q3;
			R[5] = q2_q3 - q1_q0;

			R[6] = q1_q3 - q2_q0;
			R[7] = q2_q3 + q1_q0;
			R[8] = 1 - sq_q1 - sq_q2;
		} else if (R.length == 16)
		{
			R[0] = 1 - sq_q2 - sq_q3;
			R[1] = q1_q2 - q3_q0;
			R[2] = q1_q3 + q2_q0;
			R[3] = 0.0f;

			R[4] = q1_q2 + q3_q0;
			R[5] = 1 - sq_q1 - sq_q3;
			R[6] = q2_q3 - q1_q0;
			R[7] = 0.0f;

			R[8] = q1_q3 - q2_q0;
			R[9] = q2_q3 + q1_q0;
			R[10] = 1 - sq_q1 - sq_q2;
			R[11] = 0.0f;

			R[12] = R[13] = R[14] = 0.0f;
			R[15] = 1.0f;
		}
	}
}
