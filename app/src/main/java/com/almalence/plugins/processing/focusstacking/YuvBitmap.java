package com.almalence.plugins.processing.focusstacking;

import android.graphics.Bitmap;

public class YuvBitmap
{
	static
	{
		System.loadLibrary("YuvBitmap");
	}
	
	private static native boolean fromAddress(Object bitmap, int address,
			int width, int height, int crop_x, int crop_y, int crop_w, int crop_h);
	
	private static native boolean fromByteArray(Object bitmap, byte[] inputArray,
			int width, int height, int crop_x, int crop_y, int crop_w, int crop_h);
	

	/**
	 * @param address Native address of NV21 image data block
	 * @param width NV21 image width
	 * @param height NV21 image height
	 * @param crop_x Left coordinate of crop region to be contained in created Bitmap instance
	 * @param crop_y Top coordinate of crop region to be contained in created Bitmap instance
	 * @param crop_w Width of crop region to be contained in created Bitmap instance
	 * @param crop_h Heidth of crop region to be contained in created Bitmap instance
	 * @return Created Bitmap instance containing cropped image from native NV21 image
	 */
	public static Bitmap createFromAddress(final int address, 
			final int width, final int height, final int crop_x, final int crop_y, 
			final int crop_w, final int crop_h)
	{
		final Bitmap bitmap = Bitmap.createBitmap(crop_w, crop_h, Bitmap.Config.ARGB_8888);
		if (bitmap == null)
		{
			throw new OutOfMemoryError();
		}
		
		YuvBitmap.setFromAddress(bitmap, address, width, height, crop_x, crop_y, crop_w, crop_h);
	
		return bitmap;
	}
	
	/**
	 * @param bitmap Bitmap instance to be filled
	 * @param address Native address of NV21 image data block
	 * @param width NV21 image width
	 * @param height NV21 image height
	 * @param crop_x Left coordinate of crop region to be contained in created Bitmap instance
	 * @param crop_y Top coordinate of crop region to be contained in created Bitmap instance
	 * @param crop_w Width of crop region to be contained in created Bitmap instance
	 * @param crop_h Heidth of crop region to be contained in created Bitmap instance
	 */
	public static void setFromAddress(final Bitmap bitmap, final int address, 
			final int width, final int height, final int crop_x, final int crop_y, 
			final int crop_w, final int crop_h)
	{		
		if (!YuvBitmap.fromAddress(bitmap, address, width, height, crop_x, crop_y, crop_w, crop_h))
		{
			throw new RuntimeException("Could not access bitmap pixel data.");
		}
	}
	
	public static void setFromByteArray(final Bitmap bitmap, final byte[] inputArray, 
			final int width, final int height, final int crop_x, final int crop_y, 
			final int crop_w, final int crop_h)
	{		
		if (!YuvBitmap.fromByteArray(bitmap, inputArray, width, height, crop_x, crop_y, crop_w, crop_h))
		{
			throw new RuntimeException("Could not access bitmap pixel data.");
		}
	}
}
