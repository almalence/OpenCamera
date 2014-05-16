// <!-- -+-
package com.almalence.plugins.vf.gyro;
//-+- -->

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;

public class VerticalListener implements SensorEventListener
{
	private static final float Z_AXIS_THRESHOLD = 1.5f;
	//private static final float Z_AXIS_FOR_VERTICAL_PANO = 8.f; // 45f;
	
	private float value = 0.0f;

	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy)
	{
		
	}

	@Override
	public void onSensorChanged(SensorEvent event)
	{
		this.value = Math.abs(event.values[2]);
	}
	
	public boolean showWarning()
	{
		return this.value > Z_AXIS_THRESHOLD;
	}

	public boolean showVerticalPano()
	{
		// not reliable for g-sensor
		return false; // (this.value > Z_AXIS_FOR_VERTICAL_PANO);
	}

}
