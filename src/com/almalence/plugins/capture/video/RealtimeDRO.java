package com.almalence.plugins.capture.video;

public class RealtimeDRO
{
	static
	{
		//System.loadLibrary("gbuffer");
		System.loadLibrary("almashot-jni");
	}
	
	public static native int initialize(int output_width, int output_height);
	
	public static native void render(int instance, int texture_in, float[] mtx, int sx, int sy,
			boolean filtering, boolean local_mapping, boolean night, boolean force_update, int pullYUV, int texture_out);
	
	public static native void release(int instance);
}
