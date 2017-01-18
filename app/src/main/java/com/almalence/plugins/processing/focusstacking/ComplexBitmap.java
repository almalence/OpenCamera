package com.almalence.plugins.processing.focusstacking;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Matrix;
import android.graphics.Matrix.ScaleToFit;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import android.util.Log;
//import android.util.Log;

import com.almalence.util.ImageConversion;

public class ComplexBitmap extends Drawable
{
	public static final String TAG = "ComplexBitmap";
	
    private static final int TILE_SIZE = 512;
	
    private boolean abandon = true;    
	
	private final Paint paint = new Paint();
	private final RectF rectIn = new RectF();
	private final RectF rectOut = new RectF();
	private final Matrix matrixRendering = new Matrix();
	
	private final Bitmap[][][] bitmaps;
	
	private final int width;
	private final int height;

	private final int tiles_count_x;
	private final int tiles_count_y;
	
	private final int dpiCount;
	
	private float rotation = 0.0f;
	
	/**
	 * @param address Native address of NV21 image data block
	 * @param width NV21 image width
	 * @param height NV21 image height
	 */
	public ComplexBitmap(final int width, final int height)
	{
		this(0, /*-1,*/ null, width, height, 0, 0, width, height, 4); // 1);
	}
	
	/**
	 * @param address Native address of NV21 image data block
	 * @param width NV21 image width
	 * @param height NV21 image height
	 */
//	public ComplexBitmap(final byte[] frame, final int width, final int height)
//	{
//		this(frame, width, height, 0, 0, width, height, 4); // 1);
//	}
	
	/**
	 * @param frame Native address of NV21 image data block
	 * @param width NV21 image width
	 * @param height NV21 image height
	 * @param crop_x Left coordinate of crop region to be contained in ComplexBitmap instance
	 * @param crop_y Top coordinate of crop region to be contained in ComplexBitmap instance
	 * @param crop_w Width of crop region to be contained ComplexBitmap instance
	 * @param crop_h Height of crop region to be contained ComplexBitmap instance
	 * @param dpiCount Quantity of downscaled copies. Can improve performance but consumes more RAM
	 */
	public ComplexBitmap(final byte[] frame, final int width, final int height)
	{		
		
		
		this.width = width;
		this.height = height;
		
		this.dpiCount = 1;
		
		this.bitmaps = new Bitmap[1][1][1];

		long t, bmc = 0;
		
		t = System.currentTimeMillis();
		
		Rect rect = new Rect(0, 0, width, height);
		int[] ARGBBuffer = ImageConversion.NV21ByteArraytoARGB(frame, width, height, rect, width, height);
		
		this.bitmaps[0][0][0] = Bitmap.createBitmap(ARGBBuffer, width, height, Config.ARGB_8888);
		
		bmc += System.currentTimeMillis()-t;
		
		this.tiles_count_x = 1;//(crop_w / TILE_SIZE) + ((crop_w % TILE_SIZE) > 0 ? 1 : 0);
		this.tiles_count_y = 1;//(crop_h / TILE_SIZE) + ((crop_h % TILE_SIZE) > 0 ? 1 : 0);
//		
//		this.bitmaps = new Bitmap[dpiCount][this.tiles_count_y][this.tiles_count_x];
//
//		long t, bmc = 0, bms = 0;
//		for (int y = 0; y < this.tiles_count_y; y++)
//		{
//			for (int x = 0; x < this.tiles_count_x; x++)
//			{
//				t = System.currentTimeMillis();
//				this.bitmaps[0][y][x] = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
//				bmc += System.currentTimeMillis()-t;
//				
////				if (frame == null)
////				{
////					for (int i = 1; i < dpiCount; i++)
////					{
////						final int scaled_size = getTileSize(i, dpiCount);
////						
////						t = System.currentTimeMillis();
////						this.bitmaps[i][y][x] = Bitmap.createScaledBitmap(
////								this.bitmaps[i - 1][y][x], scaled_size, scaled_size, false);
////						bms += System.currentTimeMillis()-t;
////					}
////				}
//			}
//		}
		
		Log.e(TAG, String.format("Bitmap creation time: %d", bmc));
//		Log.e(TAG, String.format("Scaling time: %d", bms));
//		Log.e(TAG, String.format("dpiCount: %d", this.dpiCount));
//		Log.e(TAG, String.format("tiles_count_x: %d", this.tiles_count_x));
//		Log.e(TAG, String.format("tiles_count_y: %d", this.tiles_count_y));

//		if (frame != null)
//		{
//			this.setFromNV21(frame, width, height, width, height);
//		}

		this.paint.setAntiAlias(false);
		this.paint.setDither(false);
		
		this.invalidateSelf();
	}
	
	
	/**
	 * @param frame Native address of NV21 image data block
	 * @param width NV21 image width
	 * @param height NV21 image height
	 * @param crop_x Left coordinate of crop region to be contained in ComplexBitmap instance
	 * @param crop_y Top coordinate of crop region to be contained in ComplexBitmap instance
	 * @param crop_w Width of crop region to be contained ComplexBitmap instance
	 * @param crop_h Height of crop region to be contained ComplexBitmap instance
	 * @param dpiCount Quantity of downscaled copies. Can improve performance but consumes more RAM
	 */
	public ComplexBitmap(final int frameAddress, /*final int frame*/final byte[] frame, final int width, final int height,
			final int crop_x, final int crop_y,
			final int crop_w, final int crop_h, final int dpiCount)
	{		
		if ((crop_x + crop_w > width) || (crop_y + crop_h > height))
		{
			throw new IndexOutOfBoundsException();
		}
		
		this.width = crop_w;
		this.height = crop_h;
		
		this.dpiCount = dpiCount;
		
		this.tiles_count_x = (crop_w / TILE_SIZE) + ((crop_w % TILE_SIZE) > 0 ? 1 : 0);
		this.tiles_count_y = (crop_h / TILE_SIZE) + ((crop_h % TILE_SIZE) > 0 ? 1 : 0);
		
		this.bitmaps = new Bitmap[dpiCount][this.tiles_count_y][this.tiles_count_x];
		
		Log.e(TAG, "ComplexBitmap. dpi = " + dpiCount + " Tiles count x = " + this.tiles_count_x + " tiles count y " + this.tiles_count_y);

		long t, bmc = 0, bms = 0;
		t = System.currentTimeMillis();
		for (int y = 0; y < this.tiles_count_y; y++)
		{
			for (int x = 0; x < this.tiles_count_x; x++)
			{
//				t = System.currentTimeMillis();
				this.bitmaps[0][y][x] = Bitmap.createBitmap(TILE_SIZE, TILE_SIZE, Bitmap.Config.ARGB_8888);
				//bmc += System.currentTimeMillis()-t;
				
				if (frame == null)
//				if (frame == -1)
				{
					for (int i = 1; i < dpiCount; i++)
					{
						final int scaled_size = getTileSize(i, dpiCount);
						
//						t = System.currentTimeMillis();
						this.bitmaps[i][y][x] = Bitmap.createScaledBitmap(
								this.bitmaps[i - 1][y][x], scaled_size, scaled_size, false);
						
//						bms += System.currentTimeMillis()-t;
					}
				}
			}
		}
		bmc += System.currentTimeMillis()-t;
		
		Log.e(TAG, String.format("Bitmap creation and scaling time: %d", bmc));
//		Log.e(TAG, String.format("Scaling time: %d", bms));
//		Log.e(TAG, String.format("dpiCount: %d", this.dpiCount));
//		Log.e(TAG, String.format("tiles_count_x: %d", this.tiles_count_x));
//		Log.e(TAG, String.format("tiles_count_y: %d", this.tiles_count_y));

		if (frame != null || frameAddress != 0)
//		if (frame != -1)
		{
			this.setFromNV21(frameAddress, frame, width, height, crop_x, crop_y);
		}

		this.paint.setAntiAlias(false);
		this.paint.setDither(false);
		
		this.invalidateSelf();
	}
	
	/**
	 * Sets content from native NV21 image data block
	 * @param frame Native address of NV21 image data block
	 * @param width NV21 image width
	 * @param height NV21 image height
	 * @param crop_x Left coordinate of crop region to be contained in ComplexBitmap instance
	 * @param crop_y Top coordinate of crop region to be contained in ComplexBitmap instance
	 */
	public void setFromNV21(final int frameAddress, /*final int frame*/final byte[] frame, final int width,
			final int height, final int crop_x, final int crop_y)
	{
		if ((crop_x + this.width > width) || (crop_y + this.height > height))
		{
			throw new IndexOutOfBoundsException();
		}
		
//		try {
//		    Thread.sleep(1500);
//		} catch (InterruptedException e)
//		{
//		    //handle
//		}
		Log.e(TAG, String.format("setFromNV21 -- start"));
		
		this.release();
		
		final long tt = System.currentTimeMillis();
		final AtomicLong bma = new AtomicLong();
		final AtomicLong bms = new AtomicLong();

		final int cpu_count = Runtime.getRuntime().availableProcessors();
		final int tiles_per_cpu = this.tiles_count_x / cpu_count;
		
//		try {
//			sleep(2000);
//		} catch (InterruptedException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		for (int y = 0; y < this.tiles_count_y; y++)
		{
			int tiles_mod_offset = 0;
			int tiles_mod = this.tiles_count_x % cpu_count;
			final int yy = y;
			final AtomicInteger sync = new AtomicInteger(cpu_count);
			synchronized (sync)
			{
				for (int c = 0; c < cpu_count; c++)
				{
					final int xx0 = tiles_mod_offset + c * (this.tiles_count_x / cpu_count);
					final int xx1;
					if (tiles_mod > 0)
					{
						xx1 = tiles_mod_offset + (c + 1) * tiles_per_cpu + 1;
						tiles_mod_offset++;
						tiles_mod--;
					}
					else
						xx1 = tiles_mod_offset + (c + 1) * tiles_per_cpu;
//					Log.i(TAG, String.format("Tiles %d-%d of %d", xx0, xx1, this.tiles_count_x));
					new Thread()
					{
						@Override
						public void run()
						{
//							this.setPriority(Thread.NORM_PRIORITY);
							this.setPriority(Thread.MAX_PRIORITY);
							
							for (int x = xx0; x < xx1; x++)
							{
								long t = System.currentTimeMillis();
								
								if(frameAddress != 0)
								{
									YuvBitmap.setFromAddress(ComplexBitmap.this.bitmaps[0][yy][x],
											frameAddress, width, height, x * TILE_SIZE, yy * TILE_SIZE, TILE_SIZE, TILE_SIZE);
								}
								else
								{
//									Log.e(TAG, "YuvBitmap.setFromByteArray -- start");
									//This code 2 times faster than below code with setPixels to Bitmap.
									YuvBitmap.setFromByteArray(ComplexBitmap.this.bitmaps[0][yy][x],
											frame, width, height, x * TILE_SIZE, yy * TILE_SIZE, TILE_SIZE, TILE_SIZE);
//									Log.e(TAG, "YuvBitmap.setFromByteArray -- end");
									
//									YuvBitmap.setFromAddress(ComplexBitmap.this.bitmaps[0][yy][x],
//											frame, width, height, x * TILE_SIZE, yy * TILE_SIZE, TILE_SIZE, TILE_SIZE);
									
//									Log.e(TAG, "Set Pixels to Bitmap from NV21ByteArraytoARGB convert -- start");
//									int x0 = x * TILE_SIZE;
//									int y0 = yy * TILE_SIZE;
//									int x_w = x * TILE_SIZE + TILE_SIZE;
//									int y_h = yy * TILE_SIZE + TILE_SIZE;
//									
//									int x_w_actual = Math.min(x_w, width);
//									int tile_width_actual = x_w_actual - x0;
//									int y_h_actual = Math.min(y_h, height);
//									int tile_height_actual = y_h_actual - y0;
//									
//									Rect rect = new Rect(x0, y0, x_w_actual, y_h_actual);
//									int[] ARGBBuffer = ImageConversion.NV21ByteArraytoARGB(frame, width, height, rect, tile_width_actual, tile_height_actual);
//									ComplexBitmap.this.bitmaps[0][yy][x].setPixels(ARGBBuffer, 0, tile_width_actual, 0, 0, tile_width_actual, tile_height_actual);
//									ARGBBuffer = null;
//									Log.e(TAG, "Set Pixels to Bitmap from NV21ByteArraytoARGB convert -- end");
									
//									ImageConversion.BitmapFromNV21ByteArray(ComplexBitmap.this.bitmaps[0][yy][x], frame, width, height, rect, tile_width_actual, tile_height_actual);
								}
								
								bma.addAndGet(System.currentTimeMillis()-t);
								
								for (int i = 1; i < ComplexBitmap.this.dpiCount; i++)
								{
									final int scaled_size = getTileSize(i, ComplexBitmap.this.dpiCount);
									
									t = System.currentTimeMillis();
									ComplexBitmap.this.bitmaps[i][yy][x] = Bitmap.createScaledBitmap(
											ComplexBitmap.this.bitmaps[i - 1][yy][x], scaled_size, scaled_size, false);
									bms.addAndGet(System.currentTimeMillis()-t);
								}
							}
							
							synchronized (sync)
							{
								sync.decrementAndGet();
								sync.notify();
							}
							
//							Log.e(TAG, String.format("Scaling time (setFromNV21): %dms", bms.get()));
//							Log.e(TAG, String.format("setFromAddress (setFromNV21): %dms", bma.get()));
//							Log.e(TAG, String.format("setFromNV21 total time: %dms", System.currentTimeMillis() - tt));
						}
					}.start();
				}
				
				try
				{
					while (sync.get() > 0) sync.wait();
				}
				catch (final InterruptedException e)
				{
					throw new RuntimeException(e);
				}
			}
		}
		
		this.abandon = false;
		
		Log.e(TAG, String.format("Scaling time (setFromNV21): %dms", bms.get()));
		Log.e(TAG, String.format("setFromAddress (setFromNV21): %dms", bma.get()));
		Log.e(TAG, String.format("setFromNV21 total time: %dms", System.currentTimeMillis() - tt));

		this.invalidateSelf();
	}
	
	private static float getDownscalingFactor(final int mode, final int dpiCount)
	{
		final float scaled = ((dpiCount - mode) / (float)dpiCount);
		
		return (float)Math.pow(scaled, 0.6f);
	}
	
	private static int getTileSize(final int mode, final int dpiCount)
	{
		return (int)(TILE_SIZE * getDownscalingFactor(mode, dpiCount));
	}
	
	private int getMode(final Rect bounds)
	{
		final float scale = Math.max(bounds.width() / (float)this.width, bounds.height() / (float)this.height);

		for (int i = 0; i < this.dpiCount; i++)
		{
			if (getDownscalingFactor(i, this.dpiCount) < scale)
				return Math.max(0, i - 1);
		}
		
		return (this.dpiCount - 1);
	}
	 
	@Override
	public void draw(final Canvas canvas)
	{
		if (this.abandon) return;
		
		final Rect bounds = this.getBounds();

		final float scale_x = bounds.width() / (float)this.width;
		final float scale_y = bounds.height() / (float)this.height;
		
//		Log.e(TAG, "draw. bounds.width = " + bounds.width() + " bounds.height = " + bounds.height() + " scale_x = " + scale_x + " scale_y = " + scale_y);
		
		final float scaled_size_x = (float)Math.ceil(TILE_SIZE * scale_x);
		final float scaled_size_y = (float)Math.ceil(TILE_SIZE * scale_y);
		
		float offset_x = bounds.left;
		float offset_y = bounds.top;
		
		final int mode = getMode(bounds);
		final int tile_side = getTileSize(mode, this.dpiCount);
		
		final Bitmap[][] bitmaps = this.bitmaps[mode];
		
		for (int y = 0; y < this.tiles_count_y; y++)
		{
			for (int x = 0; x < this.tiles_count_x; x++)
			{	
				this.rectOut.left = offset_x;
				this.rectOut.top = offset_y;
				this.rectOut.right = Math.min(offset_x + scaled_size_x, bounds.right);
				this.rectOut.bottom = Math.min(offset_y + scaled_size_y, bounds.bottom);
				
				this.rectIn.left = 0.0f;
				this.rectIn.top = 0.0f;
				this.rectIn.right = Math.min((float)Math.floor(this.rectOut.width() / scale_x), tile_side);
				this.rectIn.bottom = Math.min((float)Math.floor(this.rectOut.height() / scale_y), tile_side);
				
				this.matrixRendering.setRectToRect(this.rectIn, this.rectOut, ScaleToFit.FILL);
				this.matrixRendering.postRotate(this.rotation, bounds.width() / 2.0f, bounds.height() / 2.0f);
				this.matrixRendering.postScale(
						this.getIntrinsicWidth() / (float)this.width, 
						this.getIntrinsicHeight() / (float)this.height, 
						bounds.width() / 2.0f, bounds.height() / 2.0f);
				canvas.drawBitmap(bitmaps[y][x], this.matrixRendering, this.paint);
				
				offset_x += scaled_size_x;
			}
			
			offset_x = bounds.left;
			offset_y += scaled_size_y;
		}
	}
	
	@Override
    public int getIntrinsicWidth()
    {
		final double rad = Math.toRadians(this.rotation);
		
		return (int)(this.height * Math.abs(Math.sin(rad)) + this.width * Math.abs(Math.cos(rad)));
    }

    @Override
    public int getIntrinsicHeight()
    {
		final double rad = Math.toRadians(this.rotation);
		
		return (int)(this.width * Math.abs(Math.sin(rad)) + this.height * Math.abs(Math.cos(rad)));
    }

	@Override
	public int getOpacity()
	{
		return this.paint.getAlpha();
	}

	@Override
	public void setAlpha(final int alpha)
	{
		this.paint.setAlpha(alpha);
        this.invalidateSelf();
	}

	@Override
	public void setColorFilter(final ColorFilter cf)
	{
		this.paint.setColorFilter(cf);
        this.invalidateSelf();
	}
	
	public float getRotation()
	{
		return this.rotation;
	}
	
	public void setRotation(final float rotation)
	{
		this.rotation = rotation;
		this.invalidateSelf();
	}

	
	/**
     * Enables or disables anti-aliasing for this drawable. Anti-aliasing affects
     * the edges of the bitmap only so it applies only when the drawable is rotated.
     * 
     * @param aa True if the bitmap should be anti-aliased, false otherwise.
     */
	public void setAntiAlias(final boolean aa)
	{
		this.paint.setAntiAlias(aa);
		this.invalidateSelf();
	}

	@Override
	public void setFilterBitmap(final boolean filter)
	{
		this.paint.setFilterBitmap(filter);
		this.invalidateSelf();
	}

	@Override
	public void setDither(final boolean dither)
	{
		this.paint.setDither(dither);
		this.invalidateSelf();
	}
	
	public void release()
	{
		if (!this.abandon)
		{
			this.abandon = true;
			
			for (final Bitmap[][] bitmaps_of_q : this.bitmaps)
			{
				for (final Bitmap[] bitmaps_row : bitmaps_of_q)
				{
					for (final Bitmap bitmap : bitmaps_row)
					{
						bitmap.recycle();
					}
				}
			}

			System.gc();
		}
	}
}
