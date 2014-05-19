package com.almalence.plugins.vf.gyro;

/* <!-- +++
import com.almalence.panorama.smoothpanorama_free.PanoramaActivity;
+++ --> */
// <!-- -+-

//-+- -->

import com.almalence.plugins.capture.panoramaaugmented.AugmentedRotationListener.AugmentedRotationReceiver;
import com.almalence.plugins.capture.panoramaaugmented.Vector3d;


public class AugmentedSurfaceView implements AugmentedRotationReceiver {
	public final float[] initialTransform = new float[16];
	
	private final Vector3d currentVector = new Vector3d();
	private final Vector3d topVector = new Vector3d(0.0f, 0.0f, 1.0f);
	private final Vector3d sideVector = new Vector3d(0.0f, 1.0f, 0.0f);
	
	private GyroVFPlugin gyro;
	
	private final float[] transform = new float[16];
	
	private float radius;
	
	public AugmentedSurfaceView(GyroVFPlugin gyro) {
		this.gyro = gyro;
	}
	
	public void reset(int width, int height, float verticalViewAngleR)
	{
		
		float halfHeight = height / 2;
		
		if (verticalViewAngleR == 0)
			verticalViewAngleR = 45;
		
		// this is the radius to the center of a frame
		this.radius = (float)(halfHeight / Math.tan(Math.toRadians(verticalViewAngleR / 2.0f)));
	}
	
	@Override
	public void onRotationChanged(float[] transform)
	{		
		synchronized (this.transform)
		{
			if (transform.length == 16)
			{
				System.arraycopy(transform, 0, this.transform, 0, 16);
			}
			else if (transform.length == 9)
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
			}
			else
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
	
	public float getHorizonErrorAngle()
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
		
		final float t = (dir.x * top.y - dir.y * top.x) / (dir.x * dir.x + dir.y * dir.y);
		
		final Vector3d topProjection = new Vector3d(top.x + dir.y * t, top.y - dir.x * t, top.z);

		final float plane_top_x_signum = Math.signum(topProjection.x) == Math.signum(dir.x) ? 1.0f : -1.0f;
		final float plane_top_x = plane_top_x_signum * (float)Math.sqrt(topProjection.x * topProjection.x + topProjection.y * topProjection.y);
		final float plane_top_y = topProjection.z;
		
		float angle;
		if (Math.abs(plane_top_x) > 0.05f && Math.abs(plane_top_y) > 0.05f)
		{
			angle = (float)Math.asin(plane_top_x / Math.sqrt(plane_top_x * plane_top_x + plane_top_y * plane_top_y));
			
			if (plane_top_y < 0.0f)		
			{
				angle += Math.signum(angle) * (float)Math.PI / 2.0f;
			}
		}
		else
		{
			angle = 0.0f;
		}
		
		return angle;
	}	
	
	public float getHorizonSideErrorAngle()
	{
		final Vector3d top;
		final Vector3d side;
		
		synchronized (this.topVector) 
		{
			top = new Vector3d(this.topVector);
		}
		synchronized (this.sideVector)
		{
			side = new Vector3d(this.sideVector);
		}
		
		final float t = (side.x * top.y - side.y * top.x) / (side.x * side.x + side.y * side.y);
		
		final Vector3d topProjection = new Vector3d(top.x + side.y * t, top.y - side.x * t, top.z);

		final float plane_top_x_signum = Math.signum(topProjection.z) * (Math.signum(topProjection.y) == Math.signum(side.y) ? 1.0f : -1.0f);
		final float plane_top_x = plane_top_x_signum * (float)Math.sqrt(topProjection.x * topProjection.x + topProjection.y * topProjection.y);
		final float plane_top_y = topProjection.z;

		if (Math.abs(plane_top_x) > 0.05f && Math.abs(plane_top_y) > 0.05f)
		{
			float angle = (float)Math.asin(plane_top_x / Math.sqrt(plane_top_x * plane_top_x + plane_top_y * plane_top_y));
			
			if (plane_top_y < 0.0f)		
			{
				angle = Math.signum(angle) * (float)Math.PI - angle;
			}

			return angle;
		}
		else
		{
			return Float.POSITIVE_INFINITY;
		}
	}

	public void onDrawFrame() {
		gyro.updateHorizonIndicator(getHorizonErrorAngle(), getHorizonSideErrorAngle());
	}
}
