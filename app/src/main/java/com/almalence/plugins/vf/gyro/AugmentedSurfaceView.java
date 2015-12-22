package com.almalence.plugins.vf.gyro;

import com.almalence.plugins.capture.panoramaaugmented.AugmentedRotationListener.AugmentedRotationReceiver;
import com.almalence.plugins.capture.panoramaaugmented.Vector3d;

public class AugmentedSurfaceView implements AugmentedRotationReceiver
{
	private final Vector3d	currentVector	= new Vector3d();
	private final Vector3d	topVector		= new Vector3d(0.0f, 0.0f, 1.0f);
	private final Vector3d	sideVector		= new Vector3d(0.0f, 1.0f, 0.0f);

	private GyroVFPlugin	gyro;

	private final float[]	transform		= new float[16];

	private float			radius;

	public AugmentedSurfaceView(GyroVFPlugin gyro)
	{
		this.gyro = gyro;
	}

	public void reset(int width, int height, float verticalViewAngleR)
	{

		float halfHeight = height / 2;

		if (verticalViewAngleR == 0)
			verticalViewAngleR = 45;

		// this is the radius to the center of a frame
		this.radius = (float) (halfHeight / Math.tan(Math.toRadians(verticalViewAngleR / 2.0f)));
	}

	@Override
	public void onRotationChanged(float[] transform)
	{
		synchronized (this.transform)
		{
			if (transform.length == 16)
			{
				System.arraycopy(transform, 0, this.transform, 0, 16);
			} else if (transform.length == 9)
			{
				System.arraycopy(transform, 0, this.transform, 0, 3);
				System.arraycopy(transform, 3, this.transform, 4, 3);
				System.arraycopy(transform, 6, this.transform, 8, 3);

				this.transform[3] = 0;
				this.transform[7] = 0;
				this.transform[11] = 0;

				this.transform[12] = 0;
				this.transform[13] = 0;
				this.transform[14] = 0;

				this.transform[15] = 1;
			} else
			{
				throw new RuntimeException();
			}

			synchronized (this.topVector)
			{
				this.topVector.x = this.transform[1];
				this.topVector.y = this.transform[5];
				this.topVector.z = this.transform[9];
			}

			synchronized (this.sideVector)
			{
				this.sideVector.x = this.transform[0];
				this.sideVector.y = this.transform[4];
				this.sideVector.z = this.transform[8];
			}

			synchronized (this.currentVector)
			{
				this.currentVector.x = -this.radius * this.transform[2];
				this.currentVector.y = -this.radius * this.transform[6];
				this.currentVector.z = -this.radius * this.transform[10];
			}
		}
	}

	public float getVerticalHorizonErrorAngle()
	{
		final Vector3d top;
		final Vector3d dir;

		synchronized (this.topVector)
		{
			top = new Vector3d(this.topVector);
		}
		synchronized (this.currentVector)
		{
			dir = new Vector3d(this.currentVector);
		}

		return getErrorAngle(top, dir);
	}

	public float getHorizontalHorizonErrorAngle()
	{
		final Vector3d side;
		final Vector3d dir;

		synchronized (this.sideVector)
		{
			side = new Vector3d(this.sideVector);
		}
		synchronized (this.currentVector)
		{
			dir = new Vector3d(this.currentVector);
		}

		return getErrorAngle(side, dir);
	}

	private float getErrorAngle(Vector3d top, Vector3d dir)
	{

		final float t = (dir.x * top.y - dir.y * top.x) / (dir.x * dir.x + dir.y * dir.y);

		final Vector3d topProjection = new Vector3d(top.x + dir.y * t, top.y - dir.x * t, top.z);

		final float plane_top_x_signum = Math.signum(topProjection.x) == Math.signum(dir.x) ? 1.0f : -1.0f;
		final float plane_top_x = plane_top_x_signum
				* (float) Math.sqrt(topProjection.x * topProjection.x + topProjection.y * topProjection.y);
		final float plane_top_y = topProjection.z;

		float angle;
		if (Math.abs(plane_top_x) > 0.01f && Math.abs(plane_top_y) > 0.01f)
		{
			angle = (float) Math.asin(plane_top_x / Math.sqrt(plane_top_x * plane_top_x + plane_top_y * plane_top_y));

			if (plane_top_y < 0.0f)
			{
				angle += Math.signum(angle) * (float) Math.PI / 2.0f;
			}
		} else
		{
			angle = 0.0f;
		}

		if (Math.abs(angle) > 1.6f && top.z < 0)
		{
			angle = (float) Math.acos(top.z);
			if (top.z < 0)
			{
				angle -= Math.signum(angle) * (float) Math.PI;
				if (plane_top_x > 0.0f)
				{
					angle = -angle;
				}
			}
		}

		if (Math.abs(angle) > 1.0f && top.z > 0)
		{
			angle = (float) Math.acos(top.z);
		}

		return angle;
	}

	public float getHorizonSideErrorAngleVertical()
	{
		final Vector3d side;

		synchronized (this.sideVector)
		{
			side = new Vector3d(this.sideVector);
		}

		return (float) (Math.acos(side.z));

	}

	public float getHorizonSideErrorAngleHorizontal()
	{
		final Vector3d top;

		synchronized (this.topVector)
		{
			top = new Vector3d(this.topVector);
		}

		return (float) (Math.acos(top.z));

	}

	public void onDrawFrame()
	{
		gyro.updateHorizonIndicator(getVerticalHorizonErrorAngle(), getHorizontalHorizonErrorAngle(),
				getHorizonSideErrorAngleVertical(), getHorizonSideErrorAngleHorizontal());
	}
}
