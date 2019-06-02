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

// Usage:
// - create in activity onCreate
// - call SetFrameParameters just before starting camera preview
// - call NewData(data) from onPreviewFrame
// - call close() from activity onDestroy

import java.io.Closeable;
import java.lang.reflect.Constructor;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Message;

public class VfGyroSensor implements Closeable, Handler.Callback
{
	private static final boolean	SMOOTH_MOTION		= true;
	private static final boolean	EARLY_TIMESTAMP		= true; // false;

	// custom VF GYRO sensor type
	public static final int			TYPE_VF_GYROSCOPE	= Sensor.TYPE_GYROSCOPE | 0x01000000;

	public String getName()
	{
		return "ViewFinder Gyro";
	}

	public String getVendor()
	{
		return "Almalence";
	}

	public int getType()
	{
		return TYPE_VF_GYROSCOPE;
	}

	public int getVersion()
	{
		return 1;
	}

	public float getMaximumRange()
	{
		return 2 * (float) Math.PI;
	}// full circle in a second

	public float getResolution()
	{
		return 1e-5f;
	}

	public float getPower()
	{
		return 0;
	}

	public int getMinDelay()
	{
		return 16000;
	} // ~ 60 fps

	int getHandle()
	{
		return 0;
	}

	Constructor<SensorEvent>	EventConstructor	= null;
	SensorEvent					sensorEvent, sensorEventPrev;
	private float[]				sensorValuesPrev	= new float[3];

	SensorEventListener			m_listener;

	private boolean				m_justStability;

	private byte[]				datacopy;
	private long				timestamp;
	private long				timestamp_initial;
	private VfGyroSensor		mThiz;
	private boolean				doneWithNewData		= true;
	private int					nBlankRuns;

	private final Handler		H					= new Handler(this);
	private static final int	MSG_SMOOTHER_GYRO	= 1;

	public VfGyroSensor(SensorEventListener listener)
	{
		mThiz = this;

		doneWithNewData = true;

		Initialize();

		m_listener = listener;
		m_justStability = false;

		sensorValuesPrev[0] = sensorValuesPrev[1] = sensorValuesPrev[2] = 0;
		nBlankRuns = 0;

		// A hack to construct SensorEvent
		try
		{
			EventConstructor = SensorEvent.class.getDeclaredConstructor(int.class); // Parameter
																					// is
																					// the
																					// type
																					// of
																					// params
																					// in
																					// the
																					// constructor
																					// (e.g.
																					// String)
			EventConstructor.setAccessible(true);
			sensorEvent = EventConstructor.newInstance(3);
			sensorEventPrev = EventConstructor.newInstance(3);

			sensorEvent.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
			sensorEvent.sensor = (Sensor) null;
			sensorEventPrev.accuracy = SensorManager.SENSOR_STATUS_ACCURACY_HIGH;
			sensorEventPrev.sensor = (Sensor) null;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public void open()
	{
		Initialize();
	}

	@Override
	public void close() // throws IOException
	{
		Release();
	}

	@Override
	public void finalize() throws Throwable
	{
		this.close();

		super.finalize();
	}

	public void SetListener(SensorEventListener listener)
	{
		m_listener = listener;
	}

	public void SetStabilityOnly(boolean justStability)
	{
		m_justStability = justStability;
	}

	public boolean handleMessage(Message msg)
	{
		if (msg.what == MSG_SMOOTHER_GYRO)
			NewData(null);

		return true;
	}

	public void NewData(byte[] data)
	{
		if (EARLY_TIMESTAMP)
			timestamp = System.nanoTime();

		synchronized (sensorEventPrev)
		{
			// For smoother GUI: if frame is still in processing - pass the
			// previous values,
			// but take this (timestamp, values) into account for the next
			// calculation
			// Simpler way: if not finished with previous frame - do not accept
			// the new one
			if (SMOOTH_MOTION && ((!doneWithNewData) || (data == null)))
			{
				if (nBlankRuns < 4)
				{
					sensorEventPrev.timestamp = System.nanoTime(); // timestamp;

					++nBlankRuns;

					// emit sensor event
					if (m_listener != null)
					{
						m_listener.onSensorChanged(sensorEventPrev);
						H.sendEmptyMessageDelayed(MSG_SMOOTHER_GYRO, getMinDelay() / 1000);
					}
				}

				return;
			}
		}

		if ((!doneWithNewData) || (data == null))
			return;

		if (datacopy == null)
			datacopy = new byte[data.length];
		else if (datacopy.length != data.length)
			datacopy = new byte[data.length];

		System.arraycopy(data, 0, datacopy, 0, data.length);

		doneWithNewData = false;

		if (EARLY_TIMESTAMP)
		{
			sensorEvent.timestamp = timestamp;
		}

		// To prevent pressure on UI part - run gyro processing in separate
		// thread
		new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				synchronized (mThiz)
				{
					Update(datacopy, sensorEvent.timestamp, m_justStability);

					Get(sensorEvent.values);

					// clean any pending blank-run messages
					if (SMOOTH_MOTION)
						H.removeMessages(MSG_SMOOTHER_GYRO);

					float[] savedValues = new float[3];

					savedValues[0] = sensorEvent.values[0];
					savedValues[1] = sensorEvent.values[1];
					savedValues[2] = sensorEvent.values[2];

					synchronized (sensorEventPrev)
					{
						if (!EARLY_TIMESTAMP)
							sensorEvent.timestamp = System.nanoTime();

						// if there were blank runs - correct for accumulated
						// error
						if (sensorEventPrev.timestamp != timestamp_initial)
						{
							long dt1 = sensorEvent.timestamp - timestamp_initial;
							long dt2 = sensorEventPrev.timestamp - timestamp_initial;

							if (dt1 != dt2) // replace with dt1 > dt2
							{
								float norm = 1.f / (dt1 - dt2);

								for (int i = 0; i < 3; ++i)
								{
									float dx1 = dt1 * sensorEvent.values[i];
									float dx2 = dt2 * sensorEventPrev.values[i];

									sensorEvent.values[i] = (dx1 - dx2) * norm;
								}
							}
						}

						for (int i = 0; i < 3; ++i)
						{
							if (savedValues[i] * sensorValuesPrev[i] <= 0)
								sensorEventPrev.values[i] = 0;
							else if (Math.abs(savedValues[i]) < Math.abs(sensorValuesPrev[i]))
								sensorEventPrev.values[i] = savedValues[i];
							else
								sensorEventPrev.values[i] = sensorValuesPrev[i];

							sensorValuesPrev[i] = savedValues[i];
						}

						sensorEventPrev.timestamp = sensorEvent.timestamp;
						timestamp_initial = sensorEvent.timestamp;
						nBlankRuns = 0;

						// emit sensor event
						if (m_listener != null)
						{
							m_listener.onSensorChanged(sensorEvent);
							if (SMOOTH_MOTION && (!m_justStability))
								H.sendEmptyMessageDelayed(MSG_SMOOTHER_GYRO, getMinDelay() / 1000);
						}

						doneWithNewData = true;
					}
				}
			}
		}).start();
	}

	public native void Initialize();

	public native void Release();

	public native void SetFrameParameters(int w, int h, float HorizontalFOV, float VerticalFOV);

	public native void Update(byte[] data, long timestamp, boolean justStability);

	public native long Get(float[] values); // return value is timestamp

	public static native void FixDrift(float[] values, boolean updateDrift);

	static
	{
		System.loadLibrary("utils-image");
		System.loadLibrary("almalib");
		System.loadLibrary("almashot-pano");
	}
}
