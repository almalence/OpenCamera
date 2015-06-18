/*
	HALv3 for OpenCamera project - interface to camera2 device
    Copyright (C) 2014  Almalence Inc.

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

/* <!-- +++
 package com.almalence.opencam_plus.cameracontroller;
 +++ --> */
// <!-- -+-
package com.almalence.opencam.cameracontroller;
//-+- -->

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera.Area;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraCharacteristics.Key;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Range;
import android.util.Size;
import android.util.SizeF;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;
import com.almalence.SwapHeap;
import com.almalence.YuvImage;
import com.almalence.util.Util;

//<!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.PluginManagerInterface;
//-+- -->
/* <!-- +++
import com.almalence.opencam_plus.CameraParameters;
import com.almalence.opencam_plus.ApplicationScreen;
import com.almalence.opencam_plus.ApplicationInterface;
import com.almalence.opencam_plus.PluginManagerInterface;
 +++ --> */


//HALv3 camera's objects
@SuppressLint("NewApi")
@TargetApi(21)
public class HALv3
{
	private static final String			TAG							= "HALv3Controller";

	private static HALv3				instance					= null;

	private static Rect					activeRect					= null;
	private static Rect					zoomCropPreview				= null;
	private static Rect					zoomCropCapture				= null;
	private static float				zoomLevel					= 1f;
	private static MeteringRectangle[]	af_regions;
	private static MeteringRectangle[]	ae_regions;

	private static int					totalFrames					= 0;
	private static int					currentFrameIndex			= 0;
	private static int[]				pauseBetweenShots			= null;
	private static int[]				evCompensation				= null;
	private static int[]				sensorGain					= null;
	private static long[]				exposureTime				= null;
	private static long 				currentExposure 			= 0;
	private static int 					currentSensitivity 			= 0;
	
	private static boolean				isManualExposureTime		= false;

	protected static boolean			resultInHeap				= false;

	private static int					MAX_SUPPORTED_PREVIEW_SIZE	= 1920 * 1088;

	protected static int				captureFormat				= CameraController.JPEG;
	
	protected static boolean			playShutterSound			= false;
	protected static boolean			captureAllowed 				= false;
	
	protected static RggbChannelVector 	rggbVector					= null;
//	protected static int[] 				colorTransformMatrix 		= new int[]{258, 128, -119, 128, -10, 128, -40, 128, 209, 128, -41, 128, -1, 128, -74, 128, 203, 128};
	protected static int[] 				colorTransformMatrix 		= new int[]{227, 128, -98, 128, -1, 128, -33, 128, 193, 128, -32, 128, 0, 128, -93, 128, 221, 128};
	protected static float				multiplierR					= 1.81f;
	protected static float				multiplierG					= 1.0f;
	protected static float				multiplierB					= 1.54f;

	private static boolean 				needPreviewFrame			= false;
	
	public static HALv3 getInstance()
	{
		if (instance == null)
		{
			instance = new HALv3();
		}
		return instance;
	}

	private CameraManager					manager					= null;
	private CameraCharacteristics			camCharacter			= null;

	private static CaptureRequest.Builder	previewRequestBuilder	= null;
	private static CaptureRequest.Builder	precaptureRequestBuilder= null;
	private static CaptureRequest.Builder	stillRequestBuilder		= null;
	private static CaptureRequest.Builder	rawRequestBuilder		= null;
	private CameraCaptureSession			mCaptureSession			= null;

	protected CameraDevice					camDevice				= null;

	private static boolean					autoFocusTriggered		= false;
	
	protected static Context				mainContext				= null;
	
	protected static Handler				messageHandler			= null;
	
	private static PluginManagerInterface	pluginManager			= null;
	private static ApplicationInterface		appInterface			= null;

	public static void onCreateHALv3(Context context, ApplicationInterface app, PluginManagerInterface pluginManagerBase, Handler msgHandler)
	{
		Log.e(TAG, "onCreateHALv3");
		mainContext = context;
		appInterface = app;
		pluginManager = pluginManagerBase;
		messageHandler = msgHandler;

		// HALv3 code ---------------------------------------------------------
		// get manager for camera devices
		HALv3.getInstance().manager = (CameraManager) mainContext.getSystemService("camera");

		// get list of camera id's (usually it will contain just {"0", "1"}
		try
		{
			CameraController.cameraIdList = HALv3.getInstance().manager.getCameraIdList();
		} catch (CameraAccessException e)
		{
			Log.d("HALv3", "getCameraIdList failed");
			e.printStackTrace();
		}
	}

	public static boolean checkHardwareLevel()
	{
		if(CameraController.cameraIdList == null || CameraController.cameraIdList.length == 0)
			return false;
		try
		{
			Log.e(TAG, "checkHardwareLevel. CameraIndex = " + CameraController.CameraIndex);
			int cameraIndex = CameraController.CameraIndex;
			if (cameraIndex >= CameraController.cameraIdList.length) {
				cameraIndex = 0;
			}
			HALv3.getInstance().camCharacter = HALv3.getInstance().manager
					.getCameraCharacteristics(CameraController.cameraIdList[cameraIndex]);

			return (HALv3.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED || HALv3
					.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean isLimitedHardwareLevel()
	{
		if(CameraController.cameraIdList == null || CameraController.cameraIdList.length == 0)
			return false;
		try
		{
			HALv3.getInstance().camCharacter = HALv3.getInstance().manager
					.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);
			
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED;
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean isFullHardwareLevel()
	{
		if(CameraController.cameraIdList == null || CameraController.cameraIdList.length == 0)
			return false;
		try
		{
			HALv3.getInstance().camCharacter = HALv3.getInstance().manager
					.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);
			
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL;
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static void onResumeHALv3()
	{
		try
		{
			Log.e(TAG, "onResumeHALv3. CameraIndex = " + CameraController.CameraIndex);
			HALv3.getInstance().camCharacter = HALv3.getInstance().manager
					.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);

			int[] keys = HALv3.getInstance().camCharacter.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
			CameraController.isRAWCaptureSupported = false;
			CameraController.isManualSensorSupported = false;
			for (int key : keys)
				if (key == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW)
					CameraController.isRAWCaptureSupported = true;
				else if(key == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
					CameraController.isManualSensorSupported = true;
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
		}
	}

	public static void onPauseHALv3()
	{
		if (null != HALv3.getInstance().camDevice && null != HALv3.getInstance().mCaptureSession)
			try
			{
				HALv3.getInstance().mCaptureSession.stopRepeating();
				HALv3.getInstance().mCaptureSession.close();
				HALv3.getInstance().mCaptureSession = null;
			} catch (final CameraAccessException e)
			{
				// Doesn't matter, closing device anyway
				e.printStackTrace();
			} catch (final IllegalStateException e2)
			{
				// Doesn't matter, closing device anyway
				e2.printStackTrace();
			} finally
			{
				HALv3.getInstance().camDevice.close();
				HALv3.getInstance().camDevice = null;
			}
	}

	public static void openCameraHALv3()
	{
		Log.e(TAG, "openCameraHALv3()");
		// HALv3 open camera
		// -----------------------------------------------------------------
		if (HALv3.getCamera2() == null)
		{
			try
			{
				Log.e(TAG, "try to manager.openCamera");
				String cameraId = CameraController.cameraIdList[CameraController.CameraIndex];
				HALv3.getInstance().camCharacter = HALv3.getInstance().manager
						.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);
				HALv3.getInstance().manager.openCamera(cameraId, openCallback, null);
			} catch (CameraAccessException e)
			{
				Log.e(TAG, "CameraAccessException manager.openCamera failed: " + e.getMessage());
				e.printStackTrace();
				appInterface.stopApplication();
			} catch (IllegalArgumentException e)
			{
				Log.e(TAG, "IllegalArgumentException manager.openCamera failed: " + e.getMessage());
				e.printStackTrace();
				appInterface.stopApplication();
			} catch (SecurityException e)
			{
				Log.e(TAG, "SecurityException manager.openCamera failed: " + e.getMessage());
				e.printStackTrace();
				appInterface.stopApplication();
			}
		}

		CameraController.CameraMirrored = (HALv3.getInstance().camCharacter.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT);

		CameraController.mVideoStabilizationSupported = HALv3.getInstance().camCharacter
				.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) == null ? false : true;

		Log.d(TAG,
				"HARWARE_SUPPORT_LEVEL = "
						+ HALv3.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL));
		// check that full hw level is supported
		if (HALv3.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) != CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL)
			messageHandler.sendEmptyMessage(ApplicationInterface.MSG_NOT_LEVEL_FULL);
		else
			Log.d(TAG, "HARWARE_SUPPORT_LEVEL_FULL");

		// Get sensor size for zoom and focus/metering areas.
		activeRect = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		// ^^ HALv3 open camera
		// -----------------------------------------------------------------
	}

	public static void setupImageReadersHALv3(CameraController.Size sz)
	{
		Log.e(TAG, "setupImageReadersHALv3(). Width = " + sz.getWidth() + " Height = " + sz.getHeight());
		appInterface.createImageReaders(imageAvailableListener);
	}

	public static void setCaptureFormat(int captureFormat)
	{
		Log.e(TAG, "set captureFormat.");
		HALv3.captureFormat = captureFormat;
	}

	public static boolean createCaptureSession(List<Surface> sfl)
	{
		try
		{
			CameraDevice camera = HALv3.getCamera2();
			if(camera == null)
				return false;
			Log.d(TAG, "Create capture session. Surface list size = " + sfl.size());
			// Here, we create a CameraCaptureSession for camera preview.
			camera.createCaptureSession(sfl, HALv3.captureSessionStateCallback, null);
		} catch (IllegalArgumentException e)
		{
			Log.e(TAG, "Create capture session failed. IllegalArgumentException: " + e.getMessage());
			e.printStackTrace();
		} catch (CameraAccessException e)
		{
			Log.e(TAG, "Create capture session failed. CameraAccessException: " + e.getMessage());
			e.printStackTrace();
		}
		return true;
	}

	
	public static void dumpCameraCharacteristics()
	{
		Log.i(TAG, "Total cameras found: " + CameraController.cameraIdList.length);

		for (int i = 0; i < CameraController.cameraIdList.length; ++i)
			Log.i(TAG, "Camera Id: " + CameraController.cameraIdList[i]);

		// Query a device for Capabilities
		CameraCharacteristics cc = null;
		try
		{
			cc = HALv3.getInstance().manager
					.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);
		} catch (CameraAccessException e)
		{
			Log.d(TAG, "getCameraCharacteristics failed");
			e.printStackTrace();
		}

		// dump all the keys from CameraCharacteristics
		List<Key<?>> ck = cc.getKeys();

		for (int i = 0; i < ck.size(); ++i)
		{
			Key<?> cm = ck.get(i);

			if (cm.getName() == android.util.Size[].class.getName())
			{
				android.util.Size[] s = (android.util.Size[]) cc.get(cm);
				Log.i(TAG, "Camera characteristics: " + cm.getName() + ": " + s[0].toString());
			} else
			{
				String cmTypeName = cm.getName();
				Log.i(TAG, "Camera characteristics: " + cm.getName() + "(" + cmTypeName + "): " + cc.get(cm));
			}
		}

		StreamConfigurationMap configMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		// dump supported image formats (all of preview, video and still image)
		int[] cintarr = configMap.getOutputFormats();
		for (int k = 0; k < cintarr.length; ++k)
			Log.i(TAG, "Scaler supports format: " + cintarr[k]);

		// dump supported image sizes (all of preview, video and still image)
		android.util.Size[] imSizes = configMap.getOutputSizes(ImageFormat.YUV_420_888);
		for (int i = 0; i < imSizes.length; ++i)
			Log.i(TAG, "Scaler supports output size: " + imSizes[i].getWidth() + "x" + imSizes[i].getHeight());
	}

	public static CameraController.Size getMaxCameraImageSizeHALv3(int captureFormat)
	{
		CameraCharacteristics params = getCameraParameters2();
		StreamConfigurationMap configMap = params.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		final Size[] cs = configMap.getOutputSizes(captureFormat);

		return new CameraController.Size(cs[0].getWidth(), cs[0].getHeight());
	}

	public static void populateCameraDimensionsHALv3()
	{
		Log.e(TAG, "populateCameraDimensionsHALv3. USE captureFormat.");
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<CameraController.Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();
		CameraController.FastIdxelist = new ArrayList<Integer>();

		int minMPIX = CameraController.MIN_MPIX_SUPPORTED;
		CameraCharacteristics params = getCameraParameters2();
		StreamConfigurationMap configMap = params.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		final Size[] cs = configMap.getOutputSizes(captureFormat);

		int iHighestIndex = 0;
		Size sHighest = cs[iHighestIndex];

		int ii = 0;
		for (Size s : cs)
		{
			int currSizeWidth = s.getWidth();
			int currSizeHeight = s.getHeight();
			int highestSizeWidth = sHighest.getWidth();
			int highestSizeHeight = sHighest.getHeight();

			if ((long) currSizeWidth * currSizeHeight > (long) highestSizeWidth * highestSizeHeight)
			{
				sHighest = s;
				iHighestIndex = ii;
			}

			if ((long) currSizeWidth * currSizeHeight < minMPIX)
				continue;

			CameraController.fillResolutionsList(ii, currSizeWidth, currSizeHeight);

			ii++;
		}

		if (CameraController.ResolutionsNamesList.isEmpty())
		{
			Size s = cs[iHighestIndex];

			int currSizeWidth = s.getWidth();
			int currSizeHeight = s.getHeight();

			CameraController.fillResolutionsList(0, currSizeWidth, currSizeHeight);
		}

		return;
	}

	public static void populateCameraDimensionsForMultishotsHALv3()
	{
		CameraController.MultishotResolutionsMPixList = new ArrayList<Long>(CameraController.ResolutionsMPixList);
		CameraController.MultishotResolutionsSizeList = new ArrayList<CameraController.Size>(
				CameraController.ResolutionsSizeList);
		CameraController.MultishotResolutionsIdxesList = new ArrayList<String>(CameraController.ResolutionsIdxesList);
		CameraController.MultishotResolutionsNamesList = new ArrayList<String>(CameraController.ResolutionsNamesList);

		List<CameraController.Size> previewSizes = fillPreviewSizeList();
		if (previewSizes != null && previewSizes.size() > 0)
		{
			fillResolutionsListMultishot(CameraController.MultishotResolutionsIdxesList.size(), previewSizes.get(0)
					.getWidth(), previewSizes.get(0).getHeight());
		}

		if (previewSizes != null && previewSizes.size() > 1)
		{
			fillResolutionsListMultishot(CameraController.MultishotResolutionsIdxesList.size(), previewSizes.get(1)
					.getWidth(), previewSizes.get(1).getHeight());
		}

		String prefIdx = appInterface.getSpecialImageSizeIndexPref();

		if (prefIdx.equals("-1"))
		{
			int maxFastIdx = -1;
			long maxMpx = 0;
			for (int i = 0; i < CameraController.FastIdxelist.size(); i++)
			{
				for (int j = 0; j < CameraController.MultishotResolutionsMPixList.size(); j++)
				{
					if (CameraController.FastIdxelist.get(i) == Integer
							.parseInt(CameraController.MultishotResolutionsIdxesList.get(j))
							&& CameraController.MultishotResolutionsMPixList.get(j) > maxMpx)
					{
						maxMpx = CameraController.MultishotResolutionsMPixList.get(j);
						maxFastIdx = j;
					}
				}
			}
			if (previewSizes != null && previewSizes.size() > 0 && maxMpx >= CameraController.MPIX_1080)
			{
				appInterface.setSpecialImageSizeIndexPref(maxFastIdx);
			}
		}

		return;
	}

	protected static void fillResolutionsListMultishot(int ii, int currSizeWidth, int currSizeHeight)
	{
		boolean needAdd = true;
		boolean isFast = true;

		Long lmpix = (long) currSizeWidth * currSizeHeight;
		float mpix = (float) lmpix / 1000000.f;
		float ratio = (float) currSizeWidth / currSizeHeight;

		// find good location in a list
		int loc;
		for (loc = 0; loc < CameraController.MultishotResolutionsMPixList.size(); ++loc)
			if (CameraController.MultishotResolutionsMPixList.get(loc) < lmpix)
				break;

		int ri = 0;
		if (Math.abs(ratio - 4 / 3.f) < 0.1f)
			ri = 1;
		if (Math.abs(ratio - 3 / 2.f) < 0.12f)
			ri = 2;
		if (Math.abs(ratio - 16 / 9.f) < 0.15f)
			ri = 3;
		if (Math.abs(ratio - 1) == 0)
			ri = 4;

		String newName;
		if (isFast)
		{
			newName = String.format("%3.1f Mpix  " + CameraController.RATIO_STRINGS[ri] + " (fast)", mpix);
		} else
		{
			newName = String.format("%3.1f Mpix  " + CameraController.RATIO_STRINGS[ri], mpix);
		}

		for (int i = 0; i < CameraController.MultishotResolutionsNamesList.size(); i++)
		{
			if (newName.equals(CameraController.MultishotResolutionsNamesList.get(i)))
			{
				Long lmpixInArray = (long) (CameraController.MultishotResolutionsSizeList.get(i).getWidth() * CameraController.MultishotResolutionsSizeList
						.get(i).getHeight());
				if (Math.abs(lmpixInArray - lmpix) / lmpix < 0.1)
				{
					needAdd = false;
					break;
				}
			}
		}

		if (needAdd)
		{
			if (isFast)
			{
				CameraController.FastIdxelist.add(ii);
			}
			CameraController.MultishotResolutionsNamesList.add(loc, newName);
			CameraController.MultishotResolutionsIdxesList.add(loc, String.format("%d", ii));
			CameraController.MultishotResolutionsMPixList.add(loc, lmpix);
			CameraController.MultishotResolutionsSizeList.add(loc, new CameraController.Size(currSizeWidth,
					currSizeHeight));
		}
	}

	public static List<CameraController.Size> fillPreviewSizeList()
	{
		List<CameraController.Size> previewSizes = new ArrayList<CameraController.Size>();
		StreamConfigurationMap configMap = HALv3.getInstance().camCharacter
				.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size[] cs = configMap.getOutputSizes(SurfaceHolder.class);
		if (cs != null) {
			for (Size sz : cs)
				if (sz.getWidth() * sz.getHeight() <= MAX_SUPPORTED_PREVIEW_SIZE)
				{
					previewSizes.add(new CameraController.Size(sz.getWidth(), sz.getHeight()));
				}
		}

		return previewSizes;
	}

	public static void fillPictureSizeList(List<CameraController.Size> pictureSizes)
	{
		Log.e(TAG, "fillPictureSizeList. USE captureFormat.");
		CameraCharacteristics camCharacter = HALv3.getInstance().camCharacter;
		StreamConfigurationMap configMap = camCharacter.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size[] cs = configMap.getOutputSizes(captureFormat);
		for (Size sz : cs)
		{
			pictureSizes.add(new CameraController.Size(sz.getWidth(), sz.getHeight()));
		}
	}

	public static void fillVideoSizeList(List<CameraController.Size> videoSizes)
	{
		CameraCharacteristics camCharacter = HALv3.getInstance().camCharacter;
		StreamConfigurationMap configMap = camCharacter.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size[] cs = configMap.getOutputSizes(MediaRecorder.class);
		for (Size sz : cs)
		{
			videoSizes.add(new CameraController.Size(sz.getWidth(), sz.getHeight()));
		}
	}
	
	public static CameraDevice getCamera2()
	{
		return HALv3.getInstance().camDevice;
	}

	public static CameraCharacteristics getCameraParameters2()
	{
		if (HALv3.getInstance().camCharacter != null)
			return HALv3.getInstance().camCharacter;

		return null;
	}

	// Camera parameters interface
	public static void setAutoExposureLock(boolean lock)
	{
		if (previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			previewRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, lock);
			setRepeatingRequest();
			
			PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
			.putBoolean(ApplicationScreen.sAELockPref, lock).commit();
		}
	}
	
	public static void setAutoWhiteBalanceLock(boolean lock)
	{
		if (previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, lock);
			setRepeatingRequest();
			
			PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
			.putBoolean(ApplicationScreen.sAWBLockPref, lock).commit();
		}
	}
	
	
	public static boolean isZoomSupportedHALv3()
	{
		if (HALv3.getInstance().camCharacter != null)
		{
			float maxzoom = HALv3.getInstance().camCharacter
					.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
			return maxzoom > 0 ? true : false;
		}

		return false;
	}

	public static float getMaxZoomHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) != null)
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) * 10.0f;

		return 0;
	}

	public static void setZoom(float newZoom)
	{
		if (newZoom < 1f)
		{
			zoomLevel = 1f;
			return;
		}
		zoomLevel = newZoom;
		zoomCropPreview = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
		previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropPreview);
		setRepeatingRequest();
	}

	public static float getZoom()
	{
		return zoomLevel;
	}

	public static Rect getZoomRect(float zoom, int imgWidth, int imgHeight)
	{
		int cropWidth = (int) (imgWidth / zoom) + 2 * 64;
		int cropHeight = (int) (imgHeight / zoom) + 2 * 64;
		// ensure crop w,h divisible by 4 (SZ requirement)
		cropWidth -= cropWidth & 3;
		cropHeight -= cropHeight & 3;
		// crop area for standard frame
		int cropWidthStd = cropWidth - 2 * 64;
		int cropHeightStd = cropHeight - 2 * 64;

		return new Rect((imgWidth - cropWidthStd) / 2, (imgHeight - cropHeightStd) / 2, (imgWidth + cropWidthStd) / 2,
				(imgHeight + cropHeightStd) / 2);
	}

	public static boolean isExposureCompensationSupportedHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) != null)
		{
			Range<Integer> expRange = HALv3.getInstance().camCharacter
					.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange.getLower() == expRange.getUpper() ? false : true;
		}

		return false;
	}

	public static int getMinExposureCompensationHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) != null)
		{
			Range<Integer> expRange = HALv3.getInstance().camCharacter
					.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange.getLower();
		}

		return 0;
	}

	public static int getMaxExposureCompensationHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) != null)
		{
			Range<Integer> expRange = HALv3.getInstance().camCharacter
					.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange.getUpper();
		}

		return 0;
	}

	public static float getExposureCompensationStepHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP) != null)
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
					.floatValue();
		return 0;
	}

	public static void resetExposureCompensationHALv3()
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
			HALv3.setRepeatingRequest();
		}
	}

	public static int[] getSupportedSceneModesHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES) != null)
		{
			int[] scenes = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
			if (scenes.length > 0 && scenes[0] != CameraCharacteristics.CONTROL_SCENE_MODE_DISABLED)
				return scenes;
		}

		return new int[0];
	}

	public static int[] getSupportedWhiteBalanceHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) != null)
		{
			int[] wb = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
			if (wb.length > 0)
				return wb;
		}

		return new int[0];
	}
	
	public static boolean isManualWhiteBalanceSupportedHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) != null
				&& !Build.MODEL.contains("Nexus 6")) //Disable manual WB for Nexus 6 - it manages WB wrong
		{
			int[] wb = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
			
			boolean wbOFF = false;
			for(int i = 0; i < wb.length; i++)
				if(wb[i] == CaptureRequest.CONTROL_AWB_MODE_OFF)
				{
					wbOFF = true;
					break;
				}
			
			return wbOFF;
		}
		
		return false;
	}

	public static int[] getSupportedFocusModesHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) != null)
		{
			int[] focus = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
			if (focus.length > 0)
				return focus;
		}

		return new int[0];
	}

	public static boolean isFlashModeSupportedHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) != null)
		{
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
		}

		return false;
	}

	public static int[] getSupportedCollorEffectsHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS) != null)
		{
			int[] collorEffect = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
			if (collorEffect.length > 0)
				return collorEffect;
		}

		return new int[]{0};
	}
	
	public static int[] getSupportedISOModesHALv3()
	{
		//Temprorary disable ISO in camera2 mode, because SENSOR_SENSITIVITY parameter is ignored by the camera.
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) != null)
		{
			Range<Integer> iso = HALv3.getInstance().camCharacter
					.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
			int max_iso = iso.getUpper();
			int min_iso = iso.getLower();

			int iso_count = 0;
			int index = 0;
			for (index = 0; index < CameraController.getIsoModeHALv3().size(); index++)
			{
				int iso_value = CameraController.getIsoModeHALv3().get(index);
				if (max_iso >= iso_value && min_iso <= iso_value)
					++iso_count;
			}
			int[] iso_values = new int[iso_count];

			int iso_index = 0;
			for (index = 0; index < CameraController.getIsoModeHALv3().size(); index++)
			{
				int iso_value = CameraController.getIsoModeHALv3().get(index);
				if (max_iso >= iso_value && min_iso <= iso_value)
					iso_values[iso_index++] = CameraController.getIsoValuesList().get(index).byteValue();
			}

			if (iso_values.length > 0)
				return iso_values;
		}
		return new int[0];
	}

	public static long getCameraCurrentExposureHALv3() {
		return currentExposure;
	}
	
	public static int getCameraCurrentSensitivityHALv3() {
		return currentSensitivity;
	}
	
	public static boolean isISOModeSupportedHALv3()
	{
		// CLOSED until manual exposure metering will be researched
		 if (HALv3.getInstance().camCharacter != null &&
		 HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE)
		 != null)
		 {
			 Range<Integer> iso =
			 HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
			 if (iso.getLower() == iso.getUpper())
				 return false;
			 return true;
		 }

		return false;
	}
	
	
	public static boolean isManualFocusDistanceSupportedHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) != null)
		{
			float minFocusDistance = HALv3.getInstance().camCharacter.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
			
			//If the lens is fixed-focus, minimum focus distance will be 0.
			if(minFocusDistance > 0.0f)
				return true;
		}
		
		return false;
	}
	
	public static float getCameraMinimumFocusDistance()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) != null)
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
		
		return 0;
	}
	
	public static long getCameraMinimumExposureTime()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) != null)
		{
			Range<Long> exposureTimeRange = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
			return exposureTimeRange.getLower();
		}
		
		return 0;
	}
	
	public static long getCameraMaximumExposureTime()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) != null)
		{
			Range<Long> exposureTimeRange = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
			return exposureTimeRange.getUpper();
		}
		
		return 0;
	}

	public static int getMaxNumMeteringAreasHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) != null)
		{
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
		}

		return 0;
	}

	public static int getMaxNumFocusAreasHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) != null)
		{
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
		}

		return 0;
	}

	public static void setCameraSceneModeHALv3(int mode)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			if (mode != CameraParameters.SCENE_MODE_AUTO)
			{
				HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
				HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, mode);
			} else
			{
				HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
			}

			HALv3.setRepeatingRequest();

		}

		appInterface.setSceneModePref(mode);
	}

	public static void setCameraWhiteBalanceHALv3(int mode)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			if (mode != CameraParameters.WB_MODE_AUTO)
				HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
			else
				HALv3.previewRequestBuilder
						.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);

			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, mode);
			HALv3.setRepeatingRequest();
		}

		appInterface.setWBModePref(mode);
	}
	
	public static void setCameraColorTemperatureHALv3(int iTemp)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			appInterface.setColorTemperature(iTemp);
			try
			{
				HALv3.setCameraWhiteBalanceHALv3(CameraParameters.WB_MODE_OFF);
				HALv3.getInstance().configurePreviewRequest(true);
			} catch (CameraAccessException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void setCameraFocusModeHALv3(int mode)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mode);
			HALv3.setRepeatingRequest();
		}

		appInterface.setFocusModePref(mode);
	}

	public static void setCameraFlashModeHALv3(int mode)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			int currentFlash = appInterface.getFlashModePref(CameraParameters.FLASH_MODE_AUTO);

			int previewFlash = mode;
			if (mode != CameraParameters.FLASH_MODE_TORCH && currentFlash == CameraParameters.FLASH_MODE_TORCH)
				previewFlash = CameraParameters.FLASH_MODE_OFF;

			if (mode == CameraParameters.FLASH_MODE_TORCH || currentFlash == CameraParameters.FLASH_MODE_TORCH)
			{
				HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
				HALv3.previewRequestBuilder.set(CaptureRequest.FLASH_MODE, previewFlash);
				HALv3.setRepeatingRequest();
			}
			else if(mode == CameraParameters.FLASH_MODE_SINGLE || mode == CameraParameters.FLASH_MODE_AUTO || mode == CameraParameters.FLASH_MODE_REDEYE)
			{
				int correctedMode = mode;
				if(mode == CameraParameters.FLASH_MODE_SINGLE)
					correctedMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
				else if(mode == CameraParameters.FLASH_MODE_AUTO )
					correctedMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
				else
					correctedMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
				
				HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
				HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, correctedMode);
				HALv3.setRepeatingRequest();
			}
			else if(mode == CameraParameters.FLASH_MODE_OFF)
			{
				HALv3.previewRequestBuilder.set(CaptureRequest.FLASH_MODE, mode);
				HALv3.setRepeatingRequest();
			}
		}

		appInterface.setFlashModePref(mode);
	}

	public static void setCameraISOModeHALv3(int mode)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			if (mode > 0)
			{
				int iso = CameraController.getIsoModeHALv3().get(mode);
				HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
				HALv3.previewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
			}
			HALv3.setRepeatingRequest();
		}

		appInterface.setISOModePref(mode);
	}

	public static void setCameraExposureCompensationHALv3(int iEV)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null
				&& HALv3.getInstance().mCaptureSession != null)
		{
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, iEV);
			HALv3.setRepeatingRequest();
		}

		appInterface.setEVPref(iEV);
	}
	
	/*
	 * Manual sensor parameters: focus distance and exposure time.
	 * Available only in Camera2 mode.
	*/
	public static void setCameraExposureTimeHALv3(long iTime)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null
				&& HALv3.getInstance().mCaptureSession != null)
		{
			boolean isRealExposureTimeOnPreview = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sRealExposureTimeOnPreviewPref, false);
			long exposureTime = iTime;
			long frameDuration = 0;
			int  sensorSensitivity = CameraController.getIsoModeHALv3().get(appInterface.getISOModePref(1));

			//Exposure time longer than 1/15 gets preview very slow
			//Set custom exposure time/frame duration/ISO allows preview looks like real but on high fps.
			if(!isRealExposureTimeOnPreview)
			{
				if(iTime == 100000000L)
				{
					exposureTime = 70000000L;
					frameDuration = 70000000L;
					sensorSensitivity = 500;
				}
				else if(iTime == 142857142L)
				{
					exposureTime = 35000000L;
					frameDuration = 39000000L;
					sensorSensitivity = 1100;
				}
				else if(iTime >= 200000000L)
				{
					exposureTime = 40000000L;
					frameDuration = 40000000L;
					sensorSensitivity = 1300;
				}
			}
			previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
			HALv3.previewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);
			
			if(frameDuration > 0)
				HALv3.previewRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration);
			
			if(sensorSensitivity > 0)
				HALv3.previewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, sensorSensitivity);
			
			HALv3.setRepeatingRequest();
			isManualExposureTime = true;
		}

		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
				.putLong(ApplicationScreen.sExposureTimePref, iTime).commit();
	}
	
	public static void setCameraCollorEffectHALv3(int mode)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mode);
			HALv3.setRepeatingRequest();
		}
	}
	
	public static void resetCameraAEModeHALv3()
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null
				&& HALv3.getInstance().mCaptureSession != null)
		{
			try
			{
				HALv3.getInstance().configurePreviewRequest(true);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	public static void setCameraFocusDistanceHALv3(float fDistance)
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null
				&& HALv3.getInstance().mCaptureSession != null)
		{
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
			HALv3.previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, fDistance);
			HALv3.setRepeatingRequest();
		}

		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
				.putFloat(ApplicationScreen.sFocusDistancePref, fDistance).commit();
		
		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
				.putInt(CameraController.isFrontCamera() ? ApplicationScreen.sRearFocusModePref
				: ApplicationScreen.sFrontFocusModePref, CameraParameters.MF_MODE).commit();
	}
	//////////////////////////////////////////////////////////////////////////////////////

	public static void setCameraFocusAreasHALv3(List<Area> focusAreas)
	{
		if(activeRect == null)
			return;
		
		Rect zoomRect = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
		if (focusAreas != null && zoomRect != null)
		{
			af_regions = new MeteringRectangle[focusAreas.size()];
			for (int i = 0; i < focusAreas.size(); i++)
			{
				Rect r = focusAreas.get(i).rect;
				Log.e(TAG, "focusArea: " + r.left + " " + r.top + " " + r.right + " " + r.bottom);

				Matrix matrix = new Matrix();
				matrix.setScale(1, 1);
				matrix.preTranslate(1000.0f, 1000.0f);
				matrix.postScale((zoomRect.width() - 1) / 2000.0f, (zoomRect.height() - 1) / 2000.0f);

				RectF rectF = new RectF(r.left, r.top, r.right, r.bottom);
				matrix.mapRect(rectF);
				Util.rectFToRect(rectF, r);
				Log.e(TAG, "focusArea after matrix: " + r.left + " " + r.top + " " + r.right + " " + r.bottom);

				int currRegion = i;
				af_regions[currRegion] = new MeteringRectangle(r.left, r.top, r.right, r.bottom, 1000);
			}
		} else
		{
			af_regions = new MeteringRectangle[1];
			af_regions[0] = new MeteringRectangle(0, 0, activeRect.width() - 1, activeRect.height() - 1, 1000);
		}

		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
			HALv3.setRepeatingRequest();
		}

	}

	public static void setCameraMeteringAreasHALv3(List<Area> meteringAreas)
	{
		if(activeRect == null)
			return;
		
		Rect zoomRect = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
		if (meteringAreas != null && zoomRect != null)
		{
			ae_regions = new MeteringRectangle[meteringAreas.size()];
			for (int i = 0; i < meteringAreas.size(); i++)
			{
				Rect r = meteringAreas.get(i).rect;

				Matrix matrix = new Matrix();
				matrix.setScale(1, 1);
				matrix.preTranslate(1000.0f, 1000.0f);
				matrix.postScale((zoomRect.width() - 1) / 2000.0f, (zoomRect.height() - 1) / 2000.0f);

				RectF rectF = new RectF(r.left, r.top, r.right, r.bottom);
				matrix.mapRect(rectF);
				Util.rectFToRect(rectF, r);

				int currRegion = i;
				ae_regions[currRegion] = new MeteringRectangle(r.left, r.top, r.right, r.bottom, 10);
			}
		} else
		{
			ae_regions = new MeteringRectangle[1];
			ae_regions[0] = new MeteringRectangle(0, 0, activeRect.width() - 1, activeRect.height() - 1, 10);
		}

		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, ae_regions);
			HALv3.setRepeatingRequest();
			isManualExposureTime = false;
		}
	}
	
	public static void setRepeatingRequest()
	{
		if(HALv3.getInstance().mCaptureSession == null)
			return;

		try
		{
			CameraController.iCaptureID = HALv3.getInstance().mCaptureSession.setRepeatingRequest(
					HALv3.previewRequestBuilder.build(), captureCallback, null);
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
		} catch (IllegalStateException e2)
		{
			e2.printStackTrace();
		}
	}

	public static int getPreviewFrameRateHALv3()
	{
		if (HALv3.getInstance().camCharacter != null
				&& HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) != null)
		{
			Range<Integer>[] range;
			range = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
			return range[range.length - 1].getUpper();
		}

		return 0;
	}

	public static float getVerticalViewAngle()
	{
		if (HALv3.getInstance().camCharacter != null)
		{
			float[] focalLenghts = HALv3.getInstance().camCharacter
					.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
			SizeF sensorSize = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
			
			// sensorSize contains pixel size, not physical sensor size.
			if (sensorSize.getHeight() == sensorSize.getWidth()) {
				sensorSize = new SizeF(sensorSize.getWidth() * activeRect.width() / 1000, sensorSize.getWidth() * activeRect.height() / 1000);
			}

			float sensorHeight = sensorSize.getHeight();
			float alphaRad = (float) (2 * Math.atan2(sensorHeight, 2 * focalLenghts[0]));
			float alpha = (float) (alphaRad * (180 / Math.PI));

			return alpha;

		} else if (Build.MODEL.contains("Nexus"))
			return 46.66f;

		return 42.7f;
	}

	public static float getHorizontalViewAngle()
	{
		if (HALv3.getInstance().camCharacter != null)
		{
			float[] focalLenghts = HALv3.getInstance().camCharacter
					.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
			SizeF sensorSize = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

			// sensorSize contains pixel size, not physical sensor size.
			if (sensorSize.getHeight() == sensorSize.getWidth()) {
				sensorSize = new SizeF(sensorSize.getWidth() * activeRect.width() / 1000, sensorSize.getWidth() * activeRect.height() / 1000);
			}
			
			float sensorWidth = sensorSize.getWidth();
			float alphaRad = (float) (2 * Math.atan2(sensorWidth, 2 * focalLenghts[0]));
			float alpha = (float) (alphaRad * (180 / Math.PI));

			return alpha;
		} else if (Build.MODEL.contains("Nexus"))
			return 59.63f;

		return 55.4f;
	}

	public static int getSensorOrientation()
	{
		if (HALv3.getInstance().camCharacter != null)
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_ORIENTATION);

		return -1;
	}

	public static void CreateRequests(final int format) throws CameraAccessException
	{
		final boolean isRAWCapture = (format == CameraController.RAW);

		stillRequestBuilder = HALv3.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
		precaptureRequestBuilder = HALv3.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
		rawRequestBuilder = HALv3.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
		
		//Set Noise reduction and Edge modes for different capture formats.
		if (format == CameraController.YUV_RAW)
		{
			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
			
			precaptureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
			precaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
		} else if (isRAWCapture)
		{
			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
					CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
			
			precaptureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
			precaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
					CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
			rawRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
			rawRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
		} else
		{
			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
					CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
			
			precaptureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
			precaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
					CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
		}

		//Optical stabilization
		stillRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
		precaptureRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
		if (isRAWCapture)
			rawRequestBuilder.set(CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE, CaptureRequest.LENS_OPTICAL_STABILIZATION_MODE_ON);
		
		//Tonemap quality
		stillRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
		precaptureRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
		if (isRAWCapture)
			rawRequestBuilder.set(CaptureRequest.TONEMAP_MODE, CaptureRequest.TONEMAP_MODE_HIGH_QUALITY);
		
		//Zoom
		if ((zoomLevel > 1.0f) && (format != CameraController.YUV_RAW))
		{
			zoomCropCapture = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
			stillRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropCapture);
			precaptureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropCapture);
			if (isRAWCapture)
				rawRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropCapture);
		}

		//Focus mode. Event in case of manual exposure switch off auto focusing.
		int focusMode = appInterface.getFocusModePref(-1);
		if(focusMode != CameraParameters.MF_MODE)
		{
			stillRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, focusMode);
			precaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, focusMode);
			if (isRAWCapture)
				rawRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, focusMode);
		}
		
		int wbMode = appInterface.getWBModePref();
		stillRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, wbMode);
		precaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, wbMode);
		if(wbMode == CaptureRequest.CONTROL_AWB_MODE_OFF)
		{
			stillRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
			precaptureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
			
			stillRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbVector);
			stillRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, new ColorSpaceTransform(colorTransformMatrix));
			
			precaptureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbVector);
			precaptureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, new ColorSpaceTransform(colorTransformMatrix));
		}

		//Color effect (filters)
		int colorEffect = appInterface.getColorEffectPref();
		if(colorEffect != CameraParameters.COLOR_EFFECT_MODE_OFF)
		{
			stillRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, colorEffect);
			precaptureRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, colorEffect);
			if (isRAWCapture)
				rawRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, colorEffect);
		}
		
		
		if (format == CameraController.JPEG)
		{
			stillRequestBuilder.addTarget(appInterface.getJPEGImageSurface());
		} else if (format == CameraController.YUV || format == CameraController.YUV_RAW)
		{
			stillRequestBuilder.addTarget(appInterface.getYUVImageSurface());
		} else if (format == CameraController.RAW)
		{
			rawRequestBuilder.addTarget(appInterface.getRAWImageSurface());
			stillRequestBuilder.addTarget(appInterface.getJPEGImageSurface());
		}
		precaptureRequestBuilder.addTarget(appInterface.getPreviewYUVImageSurface());
		
		
		boolean isAutoExTime = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sExposureTimeModePref, true);
		long exTime = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getLong(ApplicationScreen.sExposureTimePref, 0);
		
		boolean isAutoFDist = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sFocusDistanceModePref, true);
		float fDist = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getFloat(ApplicationScreen.sFocusDistancePref, 0);
		
		if(!isAutoFDist)
		{
			stillRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
			stillRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, fDist);
			precaptureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
			precaptureRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, fDist);
			if (isRAWCapture)
			{
				rawRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
				rawRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, fDist);
			}
		}
		
		int flashMode = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getInt(
				ApplicationScreen.sFlashModePref, -1);
		
		if(isAutoExTime)
		{
			if(flashMode == CameraParameters.FLASH_MODE_SINGLE || flashMode == CameraParameters.FLASH_MODE_AUTO || flashMode == CameraParameters.FLASH_MODE_REDEYE || flashMode == CameraParameters.FLASH_MODE_TORCH)
			{
				if(flashMode == CameraParameters.FLASH_MODE_SINGLE)
					flashMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
				else if(flashMode == CameraParameters.FLASH_MODE_AUTO )
					flashMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
				else if(flashMode == CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE )
					flashMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
				else if(flashMode == CameraParameters.FLASH_MODE_TORCH )
					flashMode = CaptureRequest.FLASH_MODE_TORCH;
				
				HALv3.stillRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
				HALv3.stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, flashMode);
				
				HALv3.precaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
				HALv3.precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, flashMode);
				
				HALv3.rawRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
				HALv3.rawRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, flashMode);
			}
		}
		else
		{
			HALv3.stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
			HALv3.stillRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exTime);
			HALv3.precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
			HALv3.precaptureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exTime);
			if (isRAWCapture)
			{
				HALv3.rawRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
				HALv3.rawRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exTime);
			}
		}
	}

	private static void SetupPerFrameParameters(int ev, int gain, long expo, boolean isRAWCapture)
	{
		// explicitly disable AWB for the duration of still/burst capture to get
		// full burst with the same WB
		// WB does not apply to RAW, so no need for this in rawRequestBuilder
		if (!Build.MODEL.contains("G925F"))
		{
			stillRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
			precaptureRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, CaptureRequest.CONTROL_AWB_MODE_OFF);
			if(appInterface.getWBModePref() == CaptureRequest.CONTROL_AWB_MODE_OFF)
			{
				stillRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbVector);
				stillRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, new ColorSpaceTransform(colorTransformMatrix));
				
				precaptureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbVector);
				precaptureRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, new ColorSpaceTransform(colorTransformMatrix));
			}
		}

		stillRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ev);
		precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ev);
		if (isRAWCapture)
			rawRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ev);
		
		if (gain > 0)
		{
			stillRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, gain);
			precaptureRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, gain);
			if (isRAWCapture)
				rawRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, gain);
		}

		if(isManualExposureTime)
		{
			expo = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getLong(ApplicationScreen.sExposureTimePref, 0);
		}
		
		if (expo > 0)
		{
			stillRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expo);
			stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);

			precaptureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expo);
			precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
			if (isRAWCapture)
			{
				rawRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, expo);
				rawRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
			}
		}
	}

	public static int captureImageWithParamsHALv3Simple(final int nFrames, final int format, final int[] pause,
			final int[] evRequested, final int[] gain, final long[] exposure, final boolean resInHeap, final boolean playShutter) {
		
		int requestID = -1;

		final boolean isRAWCapture = (format == CameraController.RAW);

		// ToDo: burst capture is implemented now in Camera2 API
		/*
		 * List<CaptureRequest> requests = new ArrayList<CaptureRequest>();
		 * for (int n=0; n<NUM_FRAMES; ++n)
		 * requests.add(stillRequestBuilder.build());
		 * 
		 * camDevice.captureBurst(requests, new captureCallback() , null);
		 */
		boolean hasPause = false;
		if(pause != null)
			for(int p : pause)
				if (p > 0) {
					hasPause = true;
					break;
				}
			
		// requests for SZ input frames
		resultInHeap = resInHeap;
		playShutterSound = playShutter;
		
		int selectedEvCompensation = 0;
		selectedEvCompensation = appInterface.getEVPref();
		
		if(hasPause)
		{
			totalFrames = nFrames;
			currentFrameIndex = 0;
			pauseBetweenShots = pause;
			evCompensation = evRequested;
			sensorGain = gain;
			exposureTime = exposure;
			captureNextImageWithParams(format, 0,
					pauseBetweenShots == null ? 0 : pauseBetweenShots[currentFrameIndex],
					evCompensation == null ? selectedEvCompensation : evCompensation[currentFrameIndex], sensorGain == null ? currentSensitivity
							: sensorGain[currentFrameIndex], exposureTime == null ? 0
							: exposureTime[currentFrameIndex]);
		} else
		{
			pauseBetweenShots = new int[totalFrames];
			appInterface.showCaptureIndication(playShutterSound);

			for (int n = 0; n < nFrames; ++n)
			{
				SetupPerFrameParameters(evRequested == null ? selectedEvCompensation : evRequested[n], gain == null ? currentSensitivity : gain[n],
						exposure == null ? 0 : exposure[n], isRAWCapture);

				try
				{
					requestID = HALv3.getInstance().mCaptureSession.capture(stillRequestBuilder.build(),
							stillCaptureCallback, null);

					pluginManager.addRequestID(n, requestID);
					Log.e("HALv3", "mCaptureSession.capture. REQUEST ID = " + requestID);
					// FixMe: Why aren't requestID assigned if there is request with ev's being adjusted??
//						if (evRequested == null) requestID = tmp;
					
					if(isRAWCapture)
						HALv3.getInstance().mCaptureSession.capture(rawRequestBuilder.build(),
								stillCaptureCallback, null);
				} catch (CameraAccessException e)
				{
					e.printStackTrace();
				}
			}
		}

		return requestID;

	}
	
	public static void captureImageWithParamsHALv3Allowed (final int nFrames, final int format, final int[] pause,
			final int[] evRequested, final int[] gain, final long[] exposure, final boolean resInHeap, final boolean playShutter) {
		try
		{
			int requestID = -1;
			CreateRequests(format);
			
			// Nexus 5 fix flash in dark conditions and exposure set to 0.
			int selectedEvCompensation = 0;
			selectedEvCompensation = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getInt(ApplicationScreen.sEvPref, 0);
			if ((stillRequestBuilder.get(CaptureRequest.CONTROL_AE_MODE) == CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
					|| stillRequestBuilder.get(CaptureRequest.CONTROL_AE_MODE) == CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
					|| stillRequestBuilder.get(CaptureRequest.CONTROL_AE_MODE) == CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE)
					&& evRequested == null && selectedEvCompensation == 0) {
				precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 1);
			}

			if (checkHardwareLevel())
			{
				precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
						CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
				requestID = HALv3.getInstance().mCaptureSession.capture(precaptureRequestBuilder.build(),
						new CameraCaptureSession.CaptureCallback()
						{
							@Override
							public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
									TotalCaptureResult result)
							{
								precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
										CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
								
								captureImageWithParamsHALv3Simple(nFrames, format, pause,
										evRequested, gain, exposure, resInHeap, playShutter);
							}
						}, null);
			} else
			{
				captureImageWithParamsHALv3Simple(nFrames, format, pause,
						evRequested, gain, exposure, resInHeap, playShutter);
			}
		} catch (CameraAccessException e)
		{
			Log.e(TAG, "setting up still image capture request failed");
			e.printStackTrace();
			throw new RuntimeException();
		}

	}
	
	public static int captureImageWithParamsHALv3(final int nFrames, final int format, final int[] pause,
			final int[] evRequested, final int[] gain, final long[] exposure, final boolean resInHeap, final boolean playShutter)
	{
		int requestID = -1;

		if (CameraController.getFocusMode() == CameraParameters.AF_MODE_CONTINUOUS_PICTURE) {
			captureAllowed = false;
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_START);
			try
			{
				CameraController.iCaptureID = HALv3.getInstance().mCaptureSession.capture(
						HALv3.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		} else {
			captureAllowed = true;
		}
		
		if (!captureAllowed) {
			new CountDownTimer(2000, 10) {
				public void onTick(long millisUntilFinished) {
					if (captureAllowed) {
						this.cancel();
						captureImageWithParamsHALv3Allowed(nFrames, format, pause, evRequested, gain, exposure, resInHeap, playShutter);
					}
				}

				public void onFinish() {
					captureImageWithParamsHALv3Allowed(nFrames, format, pause, evRequested, gain, exposure, resInHeap, playShutter);
				}
			}.start();
		} else
			captureImageWithParamsHALv3Allowed(nFrames, format, pause, evRequested, gain, exposure, resInHeap, playShutter);
		
		
		return requestID;
	}

	private static int captureNextImageWithParamsSimple(final int format, final int frameIndex, final int pause, final int evRequested,
			final int gain, final long exposure)
	{
		int requestID = -1;

		final boolean isRAWCapture = (format == CameraController.RAW);

		if (pause > 0)
		{
			new CountDownTimer(pause, pause)
			{
				public void onTick(long millisUntilFinished)
				{

				}

				public void onFinish()
				{
					// play tick sound
					appInterface.showCaptureIndication(playShutterSound);
					try
					{
						// FixMe: Why aren't requestID assigned if there is
						// request with ev's being adjusted??
						int requestID = HALv3.getInstance().mCaptureSession.capture(stillRequestBuilder.build(),
								captureCallback, null);

						pluginManager.addRequestID(frameIndex, requestID);
						Log.e("HALv3", "NEXT mCaptureSession.capture. REQUEST ID = " + requestID);
						if (isRAWCapture)
							HALv3.getInstance().mCaptureSession.capture(rawRequestBuilder.build(), captureCallback,
									null);
					} catch (CameraAccessException e)
					{
						e.printStackTrace();
					}
				}
			}.start();

		} else
		{
			// play tick sound
			appInterface.showCaptureIndication(true);

			try
			{
				HALv3.getInstance().mCaptureSession
						.capture(stillRequestBuilder.build(), stillCaptureCallback, null);
				if (isRAWCapture)
					HALv3.getInstance().mCaptureSession.capture(rawRequestBuilder.build(), stillCaptureCallback,
							null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}

		return requestID;
	}

	public static int captureNextImageWithParams(final int format, final int frameIndex, final int pause, final int evRequested,
			final int gain, final long exposure)
	{
		int requestID = -1;

		try
		{
			CreateRequests(format);

			final boolean isRAWCapture = (format == CameraController.RAW);
			SetupPerFrameParameters(evRequested, gain, exposure, isRAWCapture);
			
			if (checkHardwareLevel())
			{
				precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
						CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
				requestID = HALv3.getInstance().mCaptureSession.capture(precaptureRequestBuilder.build(),
						new CameraCaptureSession.CaptureCallback()
						{
							@Override
							public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
									TotalCaptureResult result)
							{
								Log.e(TAG, "TRIGER CAPTURE COMPLETED");
								
								precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
										CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
								
								captureNextImageWithParamsSimple(format, frameIndex, pause, evRequested, gain, exposure);
							}
						}, null);
			} else
			{
				captureNextImageWithParamsSimple(format, frameIndex, pause, evRequested, gain, exposure);
			}
		} catch (CameraAccessException e)
		{
			Log.e(TAG, "setting up still image capture request failed");
			e.printStackTrace();
			throw new RuntimeException();
		}

		return requestID;
	}
	
	public static void forceFocusHALv3()
	{
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_START);
			try
			{
				CameraController.iCaptureID = HALv3.getInstance().mCaptureSession.capture(
						HALv3.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static boolean autoFocusHALv3()
	{
		Log.e(TAG, "HALv3.autoFocusHALv3");
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
			
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_START);
			try
			{
				Log.e(TAG,
						"autoFocusHALv3. CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_START");
				CameraController.iCaptureID = HALv3.getInstance().mCaptureSession.capture(
						HALv3.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
				return false;
			}
			
			HALv3.autoFocusTriggered = true;
			return true;
		}
		return false;
	}

	public static void cancelAutoFocusHALv3()
	{
		Log.e(TAG, "HALv3.cancelAutoFocusHALv3");
		int focusMode = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getInt(
				CameraController.isFrontCamera() ? ApplicationScreen.sRearFocusModePref : ApplicationScreen.sFrontFocusModePref, -1);
		if (HALv3.previewRequestBuilder != null && HALv3.getInstance().camDevice != null && focusMode != CameraParameters.MF_MODE)
		{
			if(HALv3.getInstance().mCaptureSession == null)
				return;
				
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL);
			try
			{
				CameraController.iCaptureID = HALv3.getInstance().mCaptureSession.capture(
						HALv3.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
			
			// Force set IDLE to prevent canceling all the time.
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_IDLE);
			try
			{
				CameraController.iCaptureID = HALv3.getInstance().mCaptureSession.capture(
						HALv3.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
			
			try
			{
				HALv3.getInstance().configurePreviewRequest(true);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}

			HALv3.autoFocusTriggered = false;
		}
		HALv3.autoFocusTriggered = false;
	}

	public void configurePreviewRequest(boolean needZoom) throws CameraAccessException
	{
		if (camDevice == null)
			return;

		long exTime = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getLong(ApplicationScreen.sExposureTimePref, 0);
		boolean isAutoExpTime = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sExposureTimeModePref, true);
		boolean isRealExposureTimeOnPreview = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sRealExposureTimeOnPreviewPref, false);
		
		float fDist = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getFloat(ApplicationScreen.sFocusDistancePref, 0);
		boolean isAutoFDist = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sFocusDistanceModePref, true);
		
		int focusMode = appInterface.getFocusModePref(-1);
		int flashMode = appInterface.getFlashModePref(-1);
		int wbMode	  = appInterface.getWBModePref();
		int sceneMode = appInterface.getSceneModePref();
		int ev 		  = appInterface.getEVPref();
		int iso 	  = CameraController.getIsoModeHALv3().get(appInterface.getISOModePref(1));
		
		int antibanding = appInterface.getAntibandingModePref();
		
		boolean aeLock = appInterface.getAELockPref();
		boolean awbLock = appInterface.getAWBLockPref();
		
		int colorEffect = appInterface.getColorEffectPref();

		Log.e(TAG, "configurePreviewRequest()");
		previewRequestBuilder = camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
		
		previewRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, colorEffect);
		
		if(focusMode != CameraParameters.MF_MODE)
			previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, focusMode);

		previewRequestBuilder.set(CaptureRequest.CONTROL_AE_ANTIBANDING_MODE, antibanding);
		
		if(isAutoExpTime)
		{
			previewRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, aeLock);
			previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, awbLock);
		}
		else
		{
			previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, true);			
		}
		
		if(isAutoExpTime)
		{
			if (flashMode == CameraParameters.FLASH_MODE_TORCH)
			{
				previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
				previewRequestBuilder.set(CaptureRequest.FLASH_MODE, flashMode);
			}
			else if(flashMode == CameraParameters.FLASH_MODE_SINGLE || flashMode == CameraParameters.FLASH_MODE_AUTO || flashMode == CameraParameters.FLASH_MODE_REDEYE)
			{
				if(flashMode == CameraParameters.FLASH_MODE_SINGLE)
					flashMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
				else if(flashMode == CameraParameters.FLASH_MODE_AUTO )
					flashMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
				else
					flashMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
				
				previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
				previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, flashMode);
			}
			else if(flashMode == CameraParameters.FLASH_MODE_OFF)
			{
				previewRequestBuilder.set(CaptureRequest.FLASH_MODE, flashMode);
			}
			
			previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, ev);	
		}
		
		previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, wbMode);
		if(wbMode == CaptureRequest.CONTROL_AWB_MODE_OFF)
		{
			previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_MODE, CaptureRequest.COLOR_CORRECTION_MODE_TRANSFORM_MATRIX);
			
			float R = 0;
			float G_even = 0;
			float G_odd  = 0;
			float B      = 0;
			float tmpKelvin = appInterface.getColorTemperature()/100;
			
			/*RED*/
			if(tmpKelvin <= 66)
				R = 255;
			else
			{
				double tmpCalc = tmpKelvin - 60;
		        tmpCalc = 329.698727446 * Math.pow(tmpCalc, -0.1332047592);
		        R = (float)tmpCalc;
		        if(R < 0) R = 0.0f;
		        if(R > 255) R = 255;
			}		
//			double tmpCalcR = tmpKelvin;// - 60;
//			if (tmpKelvin > 66)
//				tmpCalcR = tmpKelvin - 60;
//	        tmpCalcR = 329.698727446 * Math.pow(tmpCalcR, -0.1332047592);
//	        R = (float)tmpCalcR;
//	        if(R < 0) R = 0;
	        
			/*GREENs*/
			if(tmpKelvin <= 66)
			{
				double tmpCalc = tmpKelvin;
		        tmpCalc = 99.4708025861 * Math.log(tmpCalc) - 161.1195681661;
		        G_even = (float)tmpCalc;
		        if(G_even < 0)
		        	G_even = 0.0f;
		        if(G_even > 255)
		        	G_even = 255;
		        G_odd = G_even;
			}
			else
			{
				double tmpCalc = tmpKelvin - 60;
		        tmpCalc = 288.1221695283 * Math.pow(tmpCalc, -0.0755148492);
		        G_even = (float)tmpCalc;
		        if(G_even < 0)
		        	G_even = 0.0f;
		        if(G_even > 255)
		        	G_even = 255;
		        G_odd = G_even;
			}
			
			/*BLUE*/
//			if(tmpKelvin >= 66)
//				B = 255.0f;
//			else if(tmpKelvin <= 19)
//			{
//		        B = 0.0f;
//			}
//			else
//			{
//				double tmpCalc = tmpKelvin - 10;
//		        tmpCalc = 138.5177312231 * Math.log(tmpCalc) - 305.0447927307;
//		        B = (float)tmpCalc;
//		        if(B < 0) B = 0.0f;
//		        if(B > 255) B = 255;
//			}
			
			if(tmpKelvin <= 19)
			{
		        B = 0.0f;
			}
			else
			{
				double tmpCalc = tmpKelvin - 10;
		        tmpCalc = 138.5177312231 * Math.log(tmpCalc) - 305.0447927307;
		        B = (float)tmpCalc;
		        if(B < 0) B = 0;
			}
			
			R = R/255 * multiplierR;
			G_even = G_even/255 * multiplierG;
			G_odd = G_even;
			B = B/255 * multiplierB;
			
			rggbVector = new RggbChannelVector(R, G_even, G_odd, B);
//			RggbChannelVector rggb = new RggbChannelVector(R, 1.0f, 1.0f, B);
			
//			Log.e(TAG, "RGGB: R:" + R + " G:" + G_even + " B:" + B);
			Log.e(TAG, "RGGB vector: " + rggbVector.toString());
			previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_GAINS, rggbVector);
			previewRequestBuilder.set(CaptureRequest.COLOR_CORRECTION_TRANSFORM, new ColorSpaceTransform(colorTransformMatrix));
		}
		
		if (sceneMode != CameraParameters.SCENE_MODE_AUTO)
		{
			previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
			previewRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, sceneMode);
		}
		else
		{
			previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
		}
		
		if(!isAutoFDist)
		{
			previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
			previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, fDist);
		}
		
		if(!isAutoExpTime)
		{
			long exposureTime = exTime;
			long frameDuration = 0;
			int  sensorSensitivity = iso;

			//Exposure time longer than 1/15 gets preview very slow
			//Set custom exposure time/frame duration/ISO allows preview looks like real but on high fps.
			if(!isRealExposureTimeOnPreview)
			{
				if(exTime == 100000000L)
				{
					exposureTime = 70000000L;
					frameDuration = 70000000L;
					sensorSensitivity = 500;
				}
				else if(exTime == 142857142L)
				{
					exposureTime = 35000000L;
					frameDuration = 39000000L;
					sensorSensitivity = 1100;
				}
				else if(exTime >= 200000000L)
				{
					exposureTime = 40000000L;
					frameDuration = 40000000L;
					sensorSensitivity = 1300;
				}
			}
			
			previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
			previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
			HALv3.previewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);
			
			if(frameDuration > 0)
				HALv3.previewRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration);
			
			if(sensorSensitivity > 0)
				HALv3.previewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, sensorSensitivity);
		}

		previewRequestBuilder.addTarget(appInterface.getCameraSurface());
		
		
		//Disable Image Reader for Nexus 6 according to slow focusing issue
		if (!Build.MODEL.equals("Nexus 6") && captureFormat != CameraController.RAW)
			previewRequestBuilder.addTarget(appInterface.getPreviewYUVImageSurface());
		
		if(needZoom)
			previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropPreview);

		setRepeatingRequest();
	}

	// HALv3 ------------------------------------------------ camera-related
	// Callbacks
	@SuppressLint("Override")
	public final static CameraDevice.StateCallback openCallback = new CameraDevice.StateCallback()
	{
		@Override
		public void onDisconnected(CameraDevice arg0)
		{
			Log.e(TAG, "CameraDevice.StateCallback.onDisconnected");
			if (HALv3.getInstance().camDevice != null)
			{
				try
				{
					HALv3.getInstance().camDevice.close();
					HALv3.getInstance().camDevice = null;
				}
				catch (Exception e)
				{
					HALv3.getInstance().camDevice = null;
					Log.e(TAG, "close camera device failed: " + e.getMessage());
					e.printStackTrace();
				}
			}
		}

		@Override
		public void onError(CameraDevice arg0, int arg1)
		{
			Log.e(TAG, "CameraDevice.StateCallback.onError: " + arg1);
		}

		@Override
		public void onOpened(CameraDevice arg0)
		{
			Log.e(TAG, "CameraDevice.StateCallback.onOpened");

			HALv3.getInstance().camDevice = arg0;

			messageHandler.sendEmptyMessage(ApplicationInterface.MSG_CAMERA_OPENED);
		}

		@Override
		public void onClosed(CameraDevice arg0)
		{
			Log.d(TAG,"CameraDevice.StateCallback.onClosed");
			CameraController.sendMessage(ApplicationInterface.MSG_CAMERA_STOPED, 0);
		}
	};

	public final static CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback()
	{
		@Override
		public void onConfigureFailed(final CameraCaptureSession session)
		{
			Log.e(TAG, "CaptureSessionConfigure failed");
			onPauseHALv3();
			appInterface.stopApplication();
		}

		@Override
		public void onConfigured(final CameraCaptureSession session)
		{
			HALv3.getInstance().mCaptureSession = session;

			try
			{
				try
				{
					HALv3.getInstance().configurePreviewRequest(false);
				} catch (CameraAccessException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				if (CameraController.isCameraRelaunch())
				{
					CameraController.needCameraRelaunch(false);
					appInterface.relaunchCamera();
				} else
				{
					Log.e(TAG, "session.setRepeatingRequest success. Session configured");
					CameraController.sendMessage(ApplicationInterface.MSG_CAMERA_CONFIGURED, 0);
				}
			} catch (final Exception e)
			{
				e.printStackTrace();
				Toast.makeText(mainContext, "Unable to start preview: " + e.getMessage(), Toast.LENGTH_SHORT).show();
				appInterface.stopApplication();
			}
		}

	};

	
	public final static CameraCaptureSession.CaptureCallback captureCallback	= new CameraCaptureSession.CaptureCallback()
	{
		boolean resetInProgress = false;
		int resetRequestId = 0;
		
		
		@Override
		public void onCaptureCompleted(
				CameraCaptureSession session,
				CaptureRequest request,
				TotalCaptureResult result)
		{
			try
			{
//				if(HALv3.autoFocusTriggered)
//					Log.e(TAG, "CAPTURE_AF_STATE = " + result.get(CaptureResult.CONTROL_AF_STATE));
				if (result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
						&& HALv3.autoFocusTriggered)
				{
//					 Log.e(TAG, "onCaptureCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED");
					resetCaptureCallback();
					CameraController.onAutoFocus(true);
					HALv3.autoFocusTriggered = false;
	
				}
				else if (result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
						&& HALv3.autoFocusTriggered)
				{
//					Log.e(TAG, "onCaptureCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED");
					resetCaptureCallback();
					CameraController.onAutoFocus(false);
					HALv3.autoFocusTriggered = false;
				}
				
				if (result.get(CaptureResult.CONTROL_AF_MODE) == CaptureResult.CONTROL_AF_MODE_CONTINUOUS_PICTURE) {
					if (result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_PASSIVE_FOCUSED || result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED) {
						CameraController.onAutoFocusMoving(false);
					} else {
						CameraController.onAutoFocusMoving(true);
					}
				} 
			} catch (Exception e)
			{
				Log.e(TAG, "Exception: " + e.getMessage());
			}
	
			// Note: result arriving here is just image metadata, not the image itself
			// good place to extract sensor gain and other parameters
	
			// Note: not sure which units are used for exposure time (ms?)
			 currentExposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
			 currentSensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY);
			
			 if (request.get(CaptureRequest.SENSOR_SENSITIVITY) >= 50 && currentSensitivity != request.get(CaptureRequest.SENSOR_SENSITIVITY) && request.get(CaptureRequest.CONTROL_AE_MODE) == CaptureRequest.CONTROL_AE_MODE_OFF && !resetInProgress) 
			 {
				try {
					resetCaptureCallback();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			 }
			 
			try {
				int focusState = result.get(CaptureResult.CONTROL_AF_STATE);
				if (focusState == CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN
						|| focusState == CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED)
					captureAllowed = false;
				else
					captureAllowed = true;
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	
		public void onCaptureSequenceCompleted(CameraCaptureSession session, int sequenceId, long frameNumber) {
			if (sequenceId == resetRequestId) {
				resetInProgress = false;
			}
		};
		
		private void resetCaptureCallback()
		{
			resetInProgress = true;
			
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL);
			try
			{
				Log.e(TAG,
						"resetCaptureCallback. CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL");
				CameraController.iCaptureID = HALv3.getInstance().mCaptureSession.capture(
						HALv3.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
			
			// Force set IDLE to prevent canceling all the time.
			HALv3.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_IDLE);
			try
			{
//				Log.e(TAG,
//						"resetCaptureCallback. CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_IDLE");
				resetRequestId = HALv3.getInstance().mCaptureSession.capture(
						HALv3.previewRequestBuilder.build(), captureCallback, null);
				CameraController.iCaptureID = resetRequestId;
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
	};

	public final static CameraCaptureSession.CaptureCallback stillCaptureCallback = new CameraCaptureSession.CaptureCallback()
	{
		@Override
		public void onCaptureCompleted(
				CameraCaptureSession session,
				CaptureRequest request,
				TotalCaptureResult result)
		{
			Log.e(TAG, "CAPTURE COMPLETED");
//			RggbChannelVector rggb = result.get(CaptureResult.COLOR_CORRECTION_GAINS);
//			ColorSpaceTransform transformMatrix = result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM);
//			Log.e(TAG, "RGGB = R: " + rggb.getRed() + " G_even: " + rggb.getGreenEven()+ " G_odd: " + rggb.getGreenOdd() + " B: " + rggb.getBlue());
//			Log.e(TAG, "Transform Matrix: " + transformMatrix.toString());
			Log.e(TAG, "Exposure time = " + result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
			Log.e(TAG, "Frame duration = " + result.get(CaptureResult.SENSOR_FRAME_DURATION));
			Log.e(TAG, "Sensor sensitivity = " + result.get(CaptureResult.SENSOR_SENSITIVITY));
			pluginManager.onCaptureCompleted(result);
		}
	};

	public static void setNeedPreviewFrame(boolean needPreviewFrames)
	{
		needPreviewFrame |= needPreviewFrames;
	}
	
	public static void resetNeedPreviewFrame()
	{
		needPreviewFrame = false;
	}
	
	public final static ImageReader.OnImageAvailableListener imageAvailableListener = new ImageReader.OnImageAvailableListener()
	{
		@Override
		public void onImageAvailable(ImageReader ir)
		{
			// Contrary to what is written in Aptina presentation acquireLatestImage is not working as described
			// Google: Also, not working as described in android docs (should work the same as acquireNextImage in
			// our case, but it is not)
			// Image im = ir.acquireLatestImage();

			Image im = ir.acquireNextImage();
			if (ir.getSurface() == CameraController.mPreviewSurface)
			{
				if (!needPreviewFrame)
				{
					im.close();
					return;
				}
				ByteBuffer Y = im.getPlanes()[0].getBuffer();
				ByteBuffer U = im.getPlanes()[1].getBuffer();
				ByteBuffer V = im.getPlanes()[2].getBuffer();

				if ((!Y.isDirect())
					|| (!U.isDirect())
					|| (!V.isDirect()))
				{
					Log.e(TAG,"Oops, YUV ByteBuffers isDirect failed");
					im.close();
					return;
				}

				int imageWidth = im.getWidth();
				int imageHeight = im.getHeight();
				// Note:
				// android documentation guarantee that:
				// - Y pixel stride is always 1
				// - U and V strides are the same
				// So, passing all these parameters is a bit overkill

				byte[] data = YuvImage.CreateYUVImageByteArray(
								Y,
								U,
								V,
								im.getPlanes()[0].getPixelStride(),
								im.getPlanes()[0].getRowStride(),
								im.getPlanes()[1].getPixelStride(),
								im.getPlanes()[1].getRowStride(),
								im.getPlanes()[2].getPixelStride(),
								im.getPlanes()[2].getRowStride(),
								imageWidth,
								imageHeight);

				pluginManager.onPreviewFrame(data);
			} else
			{
				Log.e("HALv3", "onImageAvailable");

				int frame = 0;
				byte[] frameData = new byte[0];
				int frame_len = 0;
				boolean isYUV = false;

				if (im.getFormat() == ImageFormat.YUV_420_888)
				{
					ByteBuffer Y = im.getPlanes()[0].getBuffer();
					ByteBuffer U = im.getPlanes()[1].getBuffer();
					ByteBuffer V = im.getPlanes()[2].getBuffer();

					if ((!Y.isDirect())
						|| (!U.isDirect())
						|| (!V.isDirect()))
					{
						Log.e(TAG,"Oops, YUV ByteBuffers isDirect failed");
						im.close();
						return;
					}

					CameraController.Size imageSize = CameraController.getCameraImageSize();
					// Note:
					// android documentation guarantee that:
					// - Y pixel stride is always 1
					// - U and V strides are the same
					// So, passing all these parameters is a bit overkill
					int status = YuvImage
							.CreateYUVImage(
									Y,
									U,
									V,
									im.getPlanes()[0].getPixelStride(),
									im.getPlanes()[0].getRowStride(),
									im.getPlanes()[1].getPixelStride(),
									im.getPlanes()[1].getRowStride(),
									im.getPlanes()[2].getPixelStride(),
									im.getPlanes()[2].getRowStride(),
									imageSize.getWidth(),
									imageSize.getHeight());

					if (status != 0)
						Log.e(TAG, "Error while cropping: "	+ status);

					pluginManager.collectExifData(null);
					if (!resultInHeap)
						frameData = YuvImage.GetByteFrame();
					else
						frame = YuvImage.GetFrame();

					frame_len = imageSize.getWidth() * imageSize.getHeight() + imageSize.getWidth()	* ((imageSize.getHeight() + 1) / 2);

					isYUV = true;

				} else if (im.getFormat() == ImageFormat.JPEG)
				{
					Log.e(TAG, "captured JPEG");
					ByteBuffer jpeg = im.getPlanes()[0].getBuffer();

					frame_len = jpeg.limit();
					frameData = new byte[frame_len];
					jpeg.get(frameData,	0, frame_len);

					pluginManager.collectExifData(frameData);
					if (resultInHeap)
					{
						frame = SwapHeap.SwapToHeap(frameData);
						frameData = null;
					}
				} else if (im.getFormat() == ImageFormat.RAW_SENSOR)
				{
					Log.e(TAG, "captured RAW");
					ByteBuffer raw = im.getPlanes()[0].getBuffer();

					frame_len = raw.limit();
					frameData = new byte[frame_len];
					raw.get(frameData, 0, frame_len);

					if (resultInHeap)
					{
						frame = SwapHeap.SwapToHeap(frameData);
						frameData = null;
					}
				}

				if (im.getFormat() == ImageFormat.RAW_SENSOR)
					pluginManager.onImageTaken(frame, frameData, frame_len, CameraController.RAW);
				else
				{
					ApplicationScreen.getPluginManager().onImageTaken(frame, frameData, frame_len, isYUV ? CameraController.YUV : CameraController.JPEG);
					if (CameraController.getFocusMode() != CameraParameters.AF_MODE_CONTINUOUS_PICTURE) {
						HALv3.cancelAutoFocusHALv3();
					}
				}

				currentFrameIndex++;
				if (currentFrameIndex < totalFrames)
					captureNextImageWithParams(
							CameraController.frameFormat, currentFrameIndex,
							pauseBetweenShots == null ? 0 : pauseBetweenShots[currentFrameIndex],
							evCompensation == null ? 0 : evCompensation[currentFrameIndex],
							sensorGain == null ? currentSensitivity : sensorGain[currentFrameIndex],
							exposureTime == null ? 0 : exposureTime[currentFrameIndex]);
			}

			// Image should be closed after we are done with it
			im.close();
		}
	};
	// ^^ HALv3 code
	// --------------------------------------------------------------
	// camera-related Callbacks
}
