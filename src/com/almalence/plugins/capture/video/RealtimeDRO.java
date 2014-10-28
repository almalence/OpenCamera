package com.almalence.plugins.capture.video;

public class RealtimeDRO
{
	static
	{
		System.loadLibrary("utils-image");
		System.loadLibrary("almalib");
		System.loadLibrary("almashot-dro");
	}

	public static native int initialize(int output_width, int output_height);

	public static native void render(
			int instance,
			int texture_in,
			float[] jmtx,
			int sx,
			int sy,
			boolean filter,
			boolean local_mapping,
			float max_amplify,
			boolean force_update,
			int uv_desat,
			int dark_uv_desat,
			float dark_noise_pass,
			float mix_factor,
			float gamma,
			float max_black_level,
			float black_level_atten,
			float[] min_limit,
			float[] max_limit,
			int texture_out
			);

	public static native void release(int instance);
}
