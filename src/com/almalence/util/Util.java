/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almalence.util;

import java.io.Closeable;
import java.io.IOException;
import java.text.NumberFormat;
import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.location.Location;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.os.StatFs;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

/**
 * Collection of utility functions used in this package.
 */
public final class Util
{

	private static final String	TAG							= "Util";

	// The brightness setting used when it is set to automatic in the system.
	// The reason why it is set to 0.7 is just because 1.0 is too bright.
	// Use the same setting among the Camera, VideoCamera and Panorama modes.
	private static final float	DEFAULT_CAMERA_BRIGHTNESS	= 0.7f;

	// Orientation hysteresis amount used in rounding, in degrees
	private static final int	ORIENTATION_HYSTERESIS		= 5;

	private static final String	REVIEW_ACTION				= "com.android.camera.action.REVIEW";

	private static boolean		sIsTabletUI;
	private static float		sPixelDensity				= 1;

	// Workaround for QC cameras with broken face detection on front camera
	private static boolean		sNoFaceDetectOnFrontCamera;
	private static boolean		sNoFaceDetectOnRearCamera;

	private static Matrix		mMeteringMatrix				= new Matrix();

	private Util()
	{
	}

	public static void initialize(Context context)
	{
		sIsTabletUI = false;

		DisplayMetrics metrics = new DisplayMetrics();
		WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
		wm.getDefaultDisplay().getMetrics(metrics);
		sPixelDensity = metrics.density;
	}

	public static boolean isTabletUI()
	{
		return sIsTabletUI;
	}

	public static int dpToPixel(int dp)
	{
		return Math.round(sPixelDensity * dp);
	}

	public static boolean noFaceDetectOnFrontCamera()
	{
		return sNoFaceDetectOnFrontCamera;
	}

	public static boolean noFaceDetectOnRearCamera()
	{
		return sNoFaceDetectOnRearCamera;
	}

	// Rotates the bitmap by the specified degree.
	// If a new bitmap is created, the original bitmap is recycled.
	public static Bitmap rotate(Bitmap b, int degrees)
	{
		return rotateAndMirror(b, degrees, false);
	}

	// Rotates and/or mirrors the bitmap. If a new bitmap is created, the
	// original bitmap is recycled.
	public static Bitmap rotateAndMirror(Bitmap b, int degrees, boolean mirror)
	{
		if ((degrees != 0 || mirror) && b != null)
		{
			Matrix m = new Matrix();
			// Mirror first.
			// horizontal flip + rotation = -rotation + horizontal flip
			if (mirror)
			{
				m.postScale(-1, 1);
				degrees = (degrees + 360) % 360;
				if (degrees == 0 || degrees == 180)
				{
					m.postTranslate((float) b.getWidth(), 0);
				} else if (degrees == 90 || degrees == 270)
				{
					m.postTranslate((float) b.getHeight(), 0);
				} else
				{
					throw new IllegalArgumentException("Invalid degrees=" + degrees);
				}
			}
			if (degrees != 0)
			{
				// clockwise
				m.postRotate(degrees, (float) b.getWidth() / 2, (float) b.getHeight() / 2);
			}

			try
			{
				Bitmap b2 = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
				if (b != b2)
				{
					b.recycle();
					b = b2;
				}
			} catch (OutOfMemoryError ex)
			{
				// We have no memory to rotate. Return the original bitmap.
			}
		}
		return b;
	}

	/*
	 * Compute the sample size as a function of minSideLength and
	 * maxNumOfPixels. minSideLength is used to specify that minimal width or
	 * height of a bitmap. maxNumOfPixels is used to specify the maximal size in
	 * pixels that is tolerable in terms of memory usage.
	 * 
	 * The function returns a sample size based on the constraints. Both size
	 * and minSideLength can be passed in as -1 which indicates no care of the
	 * corresponding constraint. The functions prefers returning a sample size
	 * that generates a smaller bitmap, unless minSideLength = -1.
	 * 
	 * Also, the function rounds up the sample size to a power of 2 or multiple
	 * of 8 because BitmapFactory only honors sample size this way. For example,
	 * BitmapFactory downsamples an image by 2 even though the request is 3. So
	 * we round up the sample size to avoid OOM.
	 */
	public static int computeSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels)
	{
		int initialSize = computeInitialSampleSize(options, minSideLength, maxNumOfPixels);

		int roundedSize;
		if (initialSize <= 8)
		{
			roundedSize = 1;
			while (roundedSize < initialSize)
			{
				roundedSize <<= 1;
			}
		} else
		{
			roundedSize = (initialSize + 7) / 8 * 8;
		}

		return roundedSize;
	}

	private static int computeInitialSampleSize(BitmapFactory.Options options, int minSideLength, int maxNumOfPixels)
	{
		double w = options.outWidth;
		double h = options.outHeight;

		int lowerBound = (maxNumOfPixels < 0) ? 1 : (int) Math.ceil(Math.sqrt(w * h / maxNumOfPixels));
		int upperBound = (minSideLength < 0) ? 128 : (int) Math.min(Math.floor(w / minSideLength),
				Math.floor(h / minSideLength));

		if (upperBound < lowerBound)
		{
			// return the larger one when there is no overlapping zone.
			return lowerBound;
		}

		if (maxNumOfPixels < 0 && minSideLength < 0)
		{
			return 1;
		} else if (minSideLength < 0)
		{
			return lowerBound;
		} else
		{
			return upperBound;
		}
	}

	public static Bitmap makeBitmap(byte[] jpegData, int maxNumOfPixels)
	{
		try
		{
			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inJustDecodeBounds = true;
			BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
			if (options.mCancel || options.outWidth == -1 || options.outHeight == -1)
			{
				return null;
			}
			options.inSampleSize = computeSampleSize(options, -1, maxNumOfPixels);
			options.inJustDecodeBounds = false;

			options.inDither = false;
			options.inPreferredConfig = Bitmap.Config.ARGB_8888;
			return BitmapFactory.decodeByteArray(jpegData, 0, jpegData.length, options);
		} catch (OutOfMemoryError ex)
		{
			Log.e(TAG, "Got oom exception ", ex);
			return null;
		}
	}

	public static void closeSilently(Closeable c)
	{
		if (c == null)
			return;
		try
		{
			c.close();
		} catch (Exception t)
		{
		}
	}

	public static void Assert(boolean cond)
	{
		if (!cond)
		{
			throw new AssertionError();
		}
	}

	public static <T> T checkNotNull(T object)
	{
		if (object == null)
			throw new NullPointerException();
		return object;
	}

	public static int nextPowerOf2(int n)
	{
		n -= 1;
		n |= n >>> 16;
		n |= n >>> 8;
		n |= n >>> 4;
		n |= n >>> 2;
		n |= n >>> 1;
		return n + 1;
	}

	public static float distance(float x, float y, float sx, float sy)
	{
		float dx = x - sx;
		float dy = y - sy;
		return (float) Math.sqrt(dx * dx + dy * dy);
	}

	public static int clamp(int x, int min, int max)
	{
		if (x > max)
			return max;
		if (x < min)
			return min;
		return x;
	}

	public static float clamp(float x, float min, float max)
	{
		if (x > max)
			return max;
		if (x < min)
			return min;
		return x;
	}

	public static int getDisplayRotation(Activity activity)
	{
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		switch (rotation)
		{
		case Surface.ROTATION_0:
			return 0;
		case Surface.ROTATION_90:
			return 90;
		case Surface.ROTATION_180:
			return 180;
		case Surface.ROTATION_270:
			return 270;
		default:
			break;
		}
		return 0;
	}

	@TargetApi(9)
	public static int getDisplayOrientation(int degrees, int cameraId)
	{
		// See android.hardware.Camera.setDisplayOrientation for
		// documentation.
		Camera.CameraInfo info = new Camera.CameraInfo();
		Camera.getCameraInfo(cameraId, info);
		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
		{
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else
		{ // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}
		return result;
	}

	public static int roundOrientation(int orientation, int orientationHistory)
	{
		boolean changeOrientation = false;
		if (orientationHistory == OrientationEventListener.ORIENTATION_UNKNOWN)
		{
			changeOrientation = true;
		} else
		{
			int dist = Math.abs(orientation - orientationHistory);
			dist = Math.min(dist, 360 - dist);
			changeOrientation = (dist >= 45 + ORIENTATION_HYSTERESIS);
		}
		if (changeOrientation)
		{
			return ((orientation + 45) / 90 * 90) % 360;
		}
		return orientationHistory;
	}

	// Returns the largest picture size which matches the given aspect ratio.
	public static Size getOptimalVideoSnapshotPictureSize(List<Size> sizes, double targetRatio)
	{
		// Use a very small tolerance because we want an exact match.
		final double ASPECT_TOLERANCE = 0.001;
		if (sizes == null)
			return null;

		Size optimalSize = null;

		// Try to find a size matches aspect ratio and has the largest width
		for (Size size : sizes)
		{
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (optimalSize == null || size.width > optimalSize.width)
			{
				optimalSize = size;
			}
		}

		// Cannot find one that matches the aspect ratio. This should not
		// happen.
		// Ignore the requirement.
		if (optimalSize == null)
		{
			Log.w(TAG, "No picture size match the aspect ratio");
			for (Size size : sizes)
			{
				if (optimalSize == null || size.width > optimalSize.width)
				{
					optimalSize = size;
				}
			}
		}
		return optimalSize;
	}

	public static void dumpParameters(Parameters parameters)
	{
		String flattened = parameters.flatten();
		StringTokenizer tokenizer = new StringTokenizer(flattened, ";");
		Log.d(TAG, "Dump all camera parameters:");
		while (tokenizer.hasMoreElements())
		{
			Log.d(TAG, tokenizer.nextToken());
		}
	}

	private static int[]	mLocation	= new int[2];

	// This method is not thread-safe.
	public static boolean pointInView(float x, float y, View v)
	{
		v.getLocationInWindow(mLocation);
		return x >= mLocation[0] && x < (mLocation[0] + v.getWidth()) && y >= mLocation[1]
				&& y < (mLocation[1] + v.getHeight());
	}

	public static void dumpRect(RectF rect, String msg)
	{
		Log.v(TAG, msg + "=(" + rect.left + "," + rect.top + "," + rect.right + "," + rect.bottom + ")");
	}

	public static void rectFToRect(RectF rectF, Rect rect)
	{
		rect.left = Math.round(rectF.left);
		rect.top = Math.round(rectF.top);
		rect.right = Math.round(rectF.right);
		rect.bottom = Math.round(rectF.bottom);
	}

	public static void prepareMatrix(Matrix matrix, boolean mirror, int displayOrientation, int viewWidth,
			int viewHeight)
	{
		// Need mirror for front camera.
		matrix.setScale(mirror ? -1 : 1, 1);
		// This is the value for android.hardware.Camera.setDisplayOrientation.
		matrix.postRotate(displayOrientation);
		// Camera driver coordinates range from (-1000, -1000) to (1000, 1000).
		// UI coordinates range from (0, 0) to (width, height).
		matrix.postScale(viewWidth / 2000f, viewHeight / 2000f);
		matrix.postTranslate(viewWidth / 2f, viewHeight / 2f);
	}

	public static void fadeIn(View view)
	{
		if (view.getVisibility() == View.VISIBLE)
			return;

		view.setVisibility(View.VISIBLE);
		Animation animation = new AlphaAnimation(0F, 1F);
		animation.setDuration(400);
		view.startAnimation(animation);
	}

	public static void fadeOut(View view)
	{
		if (view.getVisibility() != View.VISIBLE)
			return;

		Animation animation = new AlphaAnimation(1F, 0F);
		animation.setDuration(400);
		view.startAnimation(animation);
		view.setVisibility(View.GONE);
	}

	public static void setGpsParameters(Parameters parameters, Location loc)
	{
		// Clear previous GPS location from the parameters.
		parameters.removeGpsData();

		// We always encode GpsTimeStamp
		parameters.setGpsTimestamp(System.currentTimeMillis() / 1000);

		// Set GPS location.
		if (loc != null)
		{
			double lat = loc.getLatitude();
			double lon = loc.getLongitude();
			boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);

			if (hasLatLon)
			{
				//Log.d(TAG, "Set gps location");
				parameters.setGpsLatitude(lat);
				parameters.setGpsLongitude(lon);
				parameters.setGpsProcessingMethod(loc.getProvider().toUpperCase());
				if (loc.hasAltitude())
				{
					parameters.setGpsAltitude(loc.getAltitude());
				} else
				{
					// for NETWORK_PROVIDER location provider, we may have
					// no altitude information, but the driver needs it, so
					// we fake one.
					parameters.setGpsAltitude(0);
				}
				if (loc.getTime() != 0)
				{
					// Location.getTime() is UTC in milliseconds.
					// gps-timestamp is UTC in seconds.
					long utcTimeSeconds = loc.getTime() / 1000;
					parameters.setGpsTimestamp(utcTimeSeconds);
				}
			} else
			{
				loc = null;
			}
		}
	}

	public static boolean isNumeric(String str)
	{
		NumberFormat formatter = NumberFormat.getInstance();
		ParsePosition pos = new ParsePosition(0);
		formatter.parse(str, pos);
		return str.length() == pos.getIndex();
	}

	public static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight)
	{
		// Raw height and width of image
		final int height = options.outHeight;
		final int width = options.outWidth;
		int inSampleSize = 1;

		if (height > reqHeight || width > reqWidth)
		{

			// Calculate ratios of height and width to requested height and
			// width
			final int heightRatio = Math.round((float) height / (float) reqHeight);
			final int widthRatio = Math.round((float) width / (float) reqWidth);

			// Choose the smallest ratio as inSampleSize value, this will
			// guarantee
			// a final image with both dimensions larger than or equal to the
			// requested height and width.
			inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
		}

		return inSampleSize;
	}

	public static Bitmap decodeSampledBitmapFromResource(Resources res, int resId, int reqWidth, int reqHeight)
	{

		// First decode with inJustDecodeBounds=true to check dimensions
		final BitmapFactory.Options options = new BitmapFactory.Options();
		options.inJustDecodeBounds = true;
		BitmapFactory.decodeResource(res, resId, options);

		// Calculate inSampleSize
		options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

		// Decode bitmap with inSampleSize set
		options.inJustDecodeBounds = false;
		return BitmapFactory.decodeResource(res, resId, options);
	}

	public static String createName(String format, long dateTaken)
	{
		Date date = new Date(dateTaken);
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(date);
	}

	public static String createNameForOriginalFrames(String format, long dateTaken, int frameIndex)
	{
		Date date = new Date(dateTaken);
		SimpleDateFormat dateFormat = new SimpleDateFormat(format);
		return dateFormat.format(date);
	}

	public static boolean isUriValid(Uri uri, ContentResolver resolver)
	{
		if (uri == null)
			return false;

		try
		{
			ParcelFileDescriptor pfd = resolver.openFileDescriptor(uri, "r");
			if (pfd == null)
			{
				Log.e(TAG, "Fail to open URI. URI=" + uri);
				return false;
			}
			pfd.close();
		} catch (IOException ex)
		{
			return false;
		}
		return true;
	}

	public static void viewUri(Uri uri, Context context)
	{
		if (!isUriValid(uri, context.getContentResolver()))
		{
			Log.e(TAG, "Uri invalid. uri=" + uri);
			return;
		}

		try
		{
			context.startActivity(new Intent(Util.REVIEW_ACTION, uri));
		} catch (ActivityNotFoundException ex)
		{
			try
			{
				context.startActivity(new Intent(Intent.ACTION_VIEW, uri));
			} catch (ActivityNotFoundException e)
			{
				Log.e(TAG, "review image fail. uri=" + uri, e);
			}
		}
	}

	public static void enterLightsOutMode(Window window)
	{
		WindowManager.LayoutParams params = window.getAttributes();
		window.setAttributes(params);
	}

	public static void initializeScreenBrightness(Window win, ContentResolver resolver)
	{
		// Overright the brightness settings if it is automatic
		int mode = Settings.System.getInt(resolver, Settings.System.SCREEN_BRIGHTNESS_MODE,
				Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
		if (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC)
		{
			WindowManager.LayoutParams winParams = win.getAttributes();
			winParams.screenBrightness = DEFAULT_CAMERA_BRIGHTNESS;
			win.setAttributes(winParams);
		}
	}

	/**
	 * SHAME@JAVA
	 */
	public static float mathSquare(double value)
	{
		return (float) (value * value);
	}

	/*************************************************************************************************
	 * Returns size in MegaBytes.
	 * 
	 * If you need calculate external memory, change this: StatFs statFs = new
	 * StatFs(Environment.getRootDirectory().getAbsolutePath()); to this: StatFs
	 * statFs = new
	 * StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
	 **************************************************************************************************/
	public static long TotalDeviceMemory()
	{
		StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
		long blockSize = statFs.getBlockSize();
		long blockCount = statFs.getBlockCount();
		return (blockCount * blockSize) / 1048576;
	}

	public static long TotalExternalMemory()
	{
		StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long blockSize = statFs.getBlockSize();
		long blockCount = statFs.getBlockCount();
		return (blockCount * blockSize) / 1048576;
	}

	public static long FreeDeviceMemory()
	{
		StatFs statFs = new StatFs(Environment.getDataDirectory().getPath());
		long blockSize = statFs.getBlockSize();
		long availableBloks = statFs.getAvailableBlocks();
		return (availableBloks * blockSize) / 1048576;
	}

	public static long FreeExternalMemory()
	{
		StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getPath());
		long blockSize = statFs.getBlockSize();
		long availableBloks = statFs.getAvailableBlocks();
		return (availableBloks * blockSize) / 1048576;
	}

	public static long BusyDeviceMemory()
	{
		StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
		long Total = (statFs.getBlockCount() * statFs.getBlockSize()) / 1048576;
		long Free = (statFs.getAvailableBlocks() * statFs.getBlockSize()) / 1048576;
		return (Total - Free);
	}

	public static long BusyExternalMemory()
	{
		StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());
		long Total = (statFs.getBlockCount() * statFs.getBlockSize()) / 1048576;
		long Free = (statFs.getAvailableBlocks() * statFs.getBlockSize()) / 1048576;
		return (Total - Free);
	}

	public static long AvailablePictureCount()
	{
		long freeMemory = Util.FreeDeviceMemory() - 5;
		if (freeMemory < 5)
			return 0;

		// RAW size of picture is width*height*3 (rgb). JPEG compress picture on
		// average 86% (compress quality 95)
		CameraController.Size saveImageSize = CameraController.getCameraImageSize();
		double imageSize = ((double) (saveImageSize.getWidth() * saveImageSize.getHeight() * 3 * 0.14) / 1048576d);
		if (imageSize == 0)
			return 0;

		return Math.round(freeMemory / imageSize);
	}

	public static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c)
	{
		List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	public static void initializeMeteringMatrix()
	{
		Matrix matrix = new Matrix();
		Util.prepareMatrix(matrix, CameraController.isFrontCamera(), 0, MainScreen.getPreviewWidth(),
				MainScreen.getPreviewHeight());
		matrix.invert(mMeteringMatrix);
	}

	public static Rect convertToDriverCoordinates(Rect rect)
	{
		RectF rectF = new RectF(rect.left, rect.top, rect.right, rect.bottom);
		mMeteringMatrix.mapRect(rectF);
		Util.rectFToRect(rectF, rect);

		if (rect.left < -1000)
			rect.left = -1000;
		if (rect.left > 1000)
			rect.left = 1000;

		if (rect.right < -1000)
			rect.right = -1000;
		if (rect.right > 1000)
			rect.right = 1000;

		if (rect.top < -1000)
			rect.top = -1000;
		if (rect.top > 1000)
			rect.top = 1000;

		if (rect.bottom < -1000)
			rect.bottom = -1000;
		if (rect.bottom > 1000)
			rect.bottom = 1000;

		return rect;
	}

	public static String toString(final Object[] objects, final char separator)
	{
		final StringBuilder stringBuilder = new StringBuilder();

		for (final Object object : objects)
		{
			stringBuilder.append(object.toString());
			stringBuilder.append(separator);
		}

		return stringBuilder.toString();
	}

	public static String logMatrix(final float[] transform, final int width, final int height)
	{
		if (width * height < transform.length)
		{
			throw new ArrayIndexOutOfBoundsException(String.format("width(%d) * height(%d) > transform(%d)", width,
					height, transform));
		}

		String format = "";
		final Object[] args = new Object[width * height];
		int cursor = 0;

		for (int i = 0; i < height; i++)
		{
			for (int j = 0; j < width; j++)
			{
				format += "% #6.2f  ";
				args[cursor] = transform[cursor];
				cursor++;
			}

			format += "\n";
		}

		return String.format(format, args);
	}

	public static boolean shouldRemapOrientation(final int orientationProc, final int rotation)
	{
		return (orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0)
				|| (orientationProc == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180)
				|| (orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90)
				|| (orientationProc == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);
	}
}
