/*
	Camera2 for OpenCamera project - interface to camera2 device
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
import android.hardware.camera2.params.BlackLevelPattern;
import android.hardware.camera2.params.ColorSpaceTransform;
import android.hardware.camera2.params.MeteringRectangle;
import android.hardware.camera2.params.RggbChannelVector;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.hardware.camera2.params.TonemapCurve;
import android.media.Image;
import android.media.ImageReader;
import android.media.MediaRecorder;
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
import com.almalence.util.ImageConversion;
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


//Controller of camera2 interface
@SuppressLint("NewApi")
@TargetApi(21)
public class Camera2Controller
{
	private static final String			TAG							= "Camera2Controller";

	private static Camera2Controller	instance					= null;

	private static Rect					activeRect					= null; //The area of the image sensor which corresponds to active pixels 
																			//This is the region of the sensor that actually receives light from the scene 
	private static Rect					zoomCropPreview				= null; //The desired region of the sensor to read out for preview. Used for digital zoom 
	private static Rect					zoomCropCapture				= null; //The same as zoomCropPreview but implement digital zoom for still image capturing
	private static float				zoomLevel					= 1f;   //Digital zoom
	
	private static MeteringRectangle[]	af_regions;							//Focus regions
	private static MeteringRectangle[]	ae_regions;     					//Exposure metering regions

	private static int					totalFrames					= 0;    //Total number of requested images for capturing
	private static int					currentFrameIndex			= 0;    //Incremented variable to get proper exposure and pause for current frame
	private static int[]				pauseBetweenShots			= null; //List of pause between captures
	private static int[]				evCompensation				= null; //List of exposure compensation values for each requested shot
	private static int[]				sensorGain					= null; //List of sensor gain values for each requested shot
	private static long[]				exposureTime				= null; //List of exposure time values for each requested shot.
	private static long 				currentExposure 			= 0;    //Exposure time of last captured frame
	private static int 					currentSensitivity 			= 0;    //Sensor sensitivity (ISO) of last captured frame
	private static int 					blevel			 			= 0;    //Black level offset
	private static int 					wlevel 						= 1024; //Maximum raw value output by sensor.
	
	private static boolean				manualPowerGamma			= false; //Used only for Almalence's SuperSensor mode
	
	private static RggbChannelVector 	rggbChannelVector 			= null; //Gains applying to raw color channels for manual white-balance logic
	
	private static boolean				isManualExposureTime		= false; //Flag to know that image captured with manual exposure time value

	protected static boolean			resultInHeap				= false; //Capture plugin may request still image to be stored in heap or in byte array

	private static int					MAX_SUPPORTED_PREVIEW_SIZE	= 1920 * 1088;

	protected static int				captureFormat				= CameraController.JPEG; //Camera2 supports JPEG, YUV and RAW formats
	protected static int				lastCaptureFormat			= CameraController.JPEG; //Used in multishot captures to know format of entire frame sequence
	protected static int				originalCaptureFormat		= CameraController.JPEG; //In case when requested size isn't supported for requested format
																							 //Camera2Controller captures JPEG image of that size and then converts to original format
	
	protected static Size[] 			allJpegSizes				= null; //All supported JPEG sizes for current capture format
	protected static Size[] 			allYUVSizes					= null; //All supported YUV sizes for current capture format
	protected static Size				highestAvailableImageSize   = null; //Highest size of JPEG
	protected static Size				highestCurrentImageSize   	= null; //Highest size of YUV
	
	protected static boolean			indicateCapturing			= false; //Flag that passed to GUI to indicate capturing
	
	protected static boolean			captureAllowed 				= false; //Flag is depended from focus mode and state. Capture allowed only when we sure that image will be focused
	
	//Section of variables that used for manual white balance
	//Constant values collected from Nexus 5
	protected static RggbChannelVector 	rggbVector					= null;
	protected static int[] 				colorTransformMatrix 		= new int[]{258, 128, -119, 128, -10, 128, -40, 128, 209, 128, -41, 128, -1, 128, -74, 128, 203, 128};
	protected static float				multiplierR					= 1.6f;
	protected static float				multiplierG					= 1.0f;
	protected static float				multiplierB					= 2.4f;

	private static boolean 				needPreviewFrame			= false; //Indicate that camera2controller has to return to PluginManager byte array of received frame or not
	

	public static Camera2Controller getInstance()
	{
		if (instance == null)
		{
			instance = new Camera2Controller();
		}
		return instance;
	}

	private CameraManager					manager					= null;
	private CameraCharacteristics			camCharacter			= null;

	private static CaptureRequest.Builder	previewRequestBuilder	= null; //Build preview requests
	private static CaptureRequest.Builder	precaptureRequestBuilder= null; //Build pre-capture request that used for exposure metering or flash before capture still image
	private static CaptureRequest.Builder	stillRequestBuilder		= null; //Build still image requests
	private static CaptureRequest.Builder	rawRequestBuilder		= null; //Build raw still image requests
	private CameraCaptureSession			mCaptureSession			= null;

	protected CameraDevice					camDevice				= null;

	private static boolean					autoFocusTriggered		= false; //Flag to inform that auto focus is in action
	
	protected static Context				mainContext				= null; //Context of application
	
	protected static Handler				messageHandler			= null;
	
	private static PluginManagerInterface	pluginManager			= null;
	private static ApplicationInterface		appInterface			= null;

	public static void onCreateCamera2(Context context, ApplicationInterface app, PluginManagerInterface pluginManagerBase, Handler msgHandler)
	{
//		Log.e(TAG, "onCreateCamera2");
		mainContext = context;
		appInterface = app;
		pluginManager = pluginManagerBase;
		messageHandler = msgHandler;

		// Camera2 code ---------------------------------------------------------
		// get manager for camera devices
		Camera2Controller.getInstance().manager = (CameraManager) mainContext.getSystemService(Context.CAMERA_SERVICE);

		// get list of camera id's (usually it will contain just {"0", "1"}
		try
		{
			CameraController.cameraIdList = Camera2Controller.getInstance().manager.getCameraIdList();
		} catch (CameraAccessException e)
		{
			Log.d("Camera2", "getCameraIdList failed");
			e.printStackTrace();
		}
	}

	//We support only FULL and LIMITED devices
	//Back and front cameras may have (and usually it is) different hardware level
	//Method returns TRUE if we support hardware level of current device
	public static boolean checkHardwareLevel()
	{
		if(CameraController.cameraIdList == null || CameraController.cameraIdList.length == 0)
			return false;
		try
		{
			if(Camera2Controller.getInstance().camCharacter == null)
				Camera2Controller.getInstance().camCharacter = Camera2Controller.getInstance().manager
				.getCameraCharacteristics(CameraController.cameraIdList[0]);
			int level = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
			return (level == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED || level == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL);
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	//Return TRUE if device has hardware level LIMITED
	public static boolean isLimitedHardwareLevel()
	{
		if(CameraController.cameraIdList == null || CameraController.cameraIdList.length == 0)
			return false;
		try
		{
			Camera2Controller.getInstance().camCharacter = Camera2Controller.getInstance().manager
					.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);
			
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED;
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	//Return TRUE if device has hardware level FULL
	public static boolean isFullHardwareLevel()
	{
		if(CameraController.cameraIdList == null || CameraController.cameraIdList.length == 0)
			return false;
		try
		{
			Camera2Controller.getInstance().camCharacter = Camera2Controller.getInstance().manager
					.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);
			
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL;
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static int getHardwareLevel()
	{
		if(CameraController.cameraIdList == null || CameraController.cameraIdList.length == 0 || Camera2Controller.getInstance().camCharacter == null)
			return -1;
		try
		{
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL);
		} catch (Exception e)
		{
			e.printStackTrace();
			return -1;
		}
	}
	
	//At this point camera2controller collect camera capabilities such as support of RAW capture, manual sensor management (white balance and exposure time)
	public static void onResumeCamera2()
	{
		try
		{
//			Log.e(TAG, "onResumeCamera2. CameraIndex = " + CameraController.CameraIndex);
			Camera2Controller.getInstance().camCharacter = Camera2Controller.getInstance().manager
					.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);

			int[] keys = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES);
			CameraController.isRAWCaptureSupported = false;
			CameraController.isManualSensorSupported = false;
			for (int key : keys)
				if (key == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_RAW && !CameraController.isGalaxyS6)
					CameraController.isRAWCaptureSupported = true;
				else if(key == CameraCharacteristics.REQUEST_AVAILABLE_CAPABILITIES_MANUAL_SENSOR)
					CameraController.isManualSensorSupported = true;
			
			originalCaptureFormat = CameraController.JPEG; //Default capture format
			
			zoomCropPreview = null;
			activeRect = null;
			
//			inCapture = false; Debug variable. Used in logic to capture RAW in Super mode on Galaxy S6
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
		}
	}

	public static void onPauseCamera2()
	{
			if (null != Camera2Controller.getInstance().camDevice && null != Camera2Controller.getInstance().mCaptureSession)
			{
				try
				{
					Camera2Controller.getInstance().mCaptureSession.stopRepeating();
					Camera2Controller.getInstance().mCaptureSession.close();  //According to google docs isn't necessary to close session
					Camera2Controller.getInstance().mCaptureSession = null;
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
					//If onPause occurs for swithching from camera2 to camera1 interface, we have to close camera device (it will release camera module)
					if(ApplicationScreen.getPluginManager().isSwitchToOldCameraInterface())
					{
						Camera2Controller.getInstance().camDevice.close();
						Camera2Controller.getInstance().camDevice = null;
					}
					
					zoomCropPreview = null;
				}
			}
	}
	
	public static void onStopCamera2()
	{
		//It's need to close camera device to let it be re-used by other applications
		if (null != Camera2Controller.getInstance().camDevice)
		{
			Camera2Controller.getInstance().camDevice.close();
			Camera2Controller.getInstance().camDevice = null;
		}
	}

	public static void openCameraCamera2()
	{
//		Log.e(TAG, "openCameraCamera2()");
		// Camera2 open camera
		// -----------------------------------------------------------------
		if (Camera2Controller.getCamera2() != null)
		{
			Camera2Controller.getInstance().camDevice.close();
			Camera2Controller.getInstance().camDevice = null;
		}
		
		if(Camera2Controller.getCamera2() == null)
		{
			//Information about success or failure will be received in callback object 'StateCallback openCallback'
			try
			{
				Log.e(TAG, "try to manager.openCamera");
				String cameraId = CameraController.cameraIdList[CameraController.CameraIndex];
				Camera2Controller.getInstance().camCharacter = Camera2Controller.getInstance().manager
						.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);
				Camera2Controller.getInstance().manager.openCamera(cameraId, openCallback, null);
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

		//Back or front camera
		CameraController.CameraMirrored = (Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT);

		CameraController.mVideoStabilizationSupported = Camera2Controller.getInstance().camCharacter
				.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) == null ? false : true;
		
		BlackLevelPattern blackPatternLevel = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_BLACK_LEVEL_PATTERN);
		if (blackPatternLevel != null) {
			blevel = blackPatternLevel.getOffsetForIndex(0, 0);
		}
		wlevel = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_WHITE_LEVEL);

//		Log.d(TAG, "HARWARE_SUPPORT_LEVEL = " + Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL));
		
		// check that full hw level is supported
		if (Camera2Controller.isFullHardwareLevel())
			messageHandler.sendEmptyMessage(ApplicationInterface.MSG_NOT_LEVEL_FULL);
		else
			Log.d(TAG, "HARWARE_SUPPORT_LEVEL_FULL");

		// Get sensor size for zoom and focus/metering areas.
		activeRect = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
		// ^^ Camera2 open camera
		// -----------------------------------------------------------------
	}

	
	public static void setupImageReadersCamera2()
	{
		appInterface.createImageReaders(imageAvailableListener);
	}
	
	public static boolean isCaptureFormatSupported(int captureFormat)
	{
		boolean isSupported = false;
		try
		{
			CameraCharacteristics cc = Camera2Controller.getInstance().manager.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);
			StreamConfigurationMap configMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
			isSupported = configMap.isOutputSupportedFor(captureFormat);
//			Log.d(TAG, "Capture format " + captureFormat + " is supported? " + isSupported);
			return isSupported;
			
		} catch (CameraAccessException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
			return isSupported;
		}
	}

	public static void setCaptureFormat(int captureFormat)
	{
//		Log.e(TAG, "set captureFormat.");
		Camera2Controller.captureFormat = captureFormat;
	}

	public static boolean createCaptureSession(List<Surface> sfl)
	{
		try
		{
			CameraDevice camera = Camera2Controller.getCamera2();
			if(camera == null)
				return false;
//			Log.d(TAG, "Create capture session. Surface list size = " + sfl.size());
			// Here, we create a CameraCaptureSession for camera preview.
			camera.createCaptureSession(sfl, Camera2Controller.captureSessionStateCallback, null);
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

	
//	public static void dumpCameraCharacteristics()
//	{
////		Log.i(TAG, "Total cameras found: " + CameraController.cameraIdList.length);
//
////		for (int i = 0; i < CameraController.cameraIdList.length; ++i)
////			Log.i(TAG, "Camera Id: " + CameraController.cameraIdList[i]);
//
//		// Query a device for Capabilities
//		CameraCharacteristics cc = null;
//		try
//		{
//			cc = Camera2Controller.getInstance().manager
//					.getCameraCharacteristics(CameraController.cameraIdList[CameraController.CameraIndex]);
//		} catch (CameraAccessException e)
//		{
//			Log.d(TAG, "getCameraCharacteristics failed");
//			e.printStackTrace();
//		}
//
//		// dump all the keys from CameraCharacteristics
//		List<Key<?>> ck = cc.getKeys();
//
//		for (int i = 0; i < ck.size(); ++i)
//		{
//			Key<?> cm = ck.get(i);
//
//			if (cm.getName() == android.util.Size[].class.getName())
//			{
//				android.util.Size[] s = (android.util.Size[]) cc.get(cm);
////				Log.i(TAG, "Camera characteristics: " + cm.getName() + ": " + s[0].toString());
//			} else
//			{
//				String cmTypeName = cm.getName();
////				Log.i(TAG, "Camera characteristics: " + cm.getName() + "(" + cmTypeName + "): " + cc.get(cm));
//			}
//		}
//
//		StreamConfigurationMap configMap = cc.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
//		// dump supported image formats (all of preview, video and still image)
//		int[] cintarr = configMap.getOutputFormats();
//		for (int k = 0; k < cintarr.length; ++k)
//			Log.i(TAG, "Scaler supports format: " + cintarr[k]);
//
//		// dump supported image sizes (all of preview, video and still image)
//		android.util.Size[] imSizes = configMap.getOutputSizes(ImageFormat.YUV_420_888);
//		for (int i = 0; i < imSizes.length; ++i)
//			Log.i(TAG, "Scaler supports output size: " + imSizes[i].getWidth() + "x" + imSizes[i].getHeight());
//	}

	public static CameraController.Size getMaxCameraImageSizeCamera2(int captureFormat)
	{
		CameraCharacteristics params = getCameraCharacteristics();
		StreamConfigurationMap configMap = params.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		final Size[] cs = configMap.getOutputSizes(captureFormat);

		int maxSizeIndex = Util.getMaxImageSizeIndex(cs);
		
		return new CameraController.Size(cs[maxSizeIndex].getWidth(), cs[maxSizeIndex].getHeight());
	}

	
	//Fill different resolution lists with data
	public static void populateCameraDimensionsCamera2()
	{
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<CameraController.Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();
		CameraController.FastIdxelist = new ArrayList<Integer>();

		int minMPIX = CameraController.MIN_MPIX_SUPPORTED;
		CameraCharacteristics params = getCameraCharacteristics();
		StreamConfigurationMap configMap = params.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size[] cs = configMap.getOutputSizes(captureFormat);
		Size highestSize = findMaximumSize(cs);
		/*
		 * In case when device supports capturing YUV less maximum size than JPEG
		 * give users available JPEG sizes instead
		 */
		if(captureFormat == CameraController.YUV)
		{
			Size[] jpegSize = configMap.getOutputSizes(CameraController.JPEG);
			Size highestJPEGSize = findMaximumSize(jpegSize);
			if(highestJPEGSize.getWidth() > highestSize.getWidth())
				cs = jpegSize;
		}
		

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

			CameraController.addResolution(ii, currSizeWidth, currSizeHeight);

			ii++;
		}

		if (CameraController.ResolutionsNamesList.isEmpty())
		{
			Size s = cs[iHighestIndex];

			int currSizeWidth = s.getWidth();
			int currSizeHeight = s.getHeight();

			CameraController.addResolution(0, currSizeWidth, currSizeHeight);
		}

		return;
	}

	
	//In OpenCamera image size preference for single still image capturing is differ from multishot still capturing
	public static void populateCameraDimensionsForMultishotsCamera2()
	{
		//Generally multishot lists is the same as the common resolution lists
		//Copy common resolutions lists to multishot lists
		CameraController.MultishotResolutionsMPixList = new ArrayList<Long>(CameraController.ResolutionsMPixList);
		CameraController.MultishotResolutionsSizeList = new ArrayList<CameraController.Size>(CameraController.ResolutionsSizeList);
		CameraController.MultishotResolutionsIdxesList = new ArrayList<String>(CameraController.ResolutionsIdxesList);
		CameraController.MultishotResolutionsNamesList = new ArrayList<String>(CameraController.ResolutionsNamesList);

		//Add to multishot resolution lists 2 highest preview sizes
		List<CameraController.Size> previewSizes = getPreviewSizeList();
		if (previewSizes != null && previewSizes.size() > 0)
		{
			addMultishotResolution(CameraController.MultishotResolutionsIdxesList.size(), previewSizes.get(0)
					.getWidth(), previewSizes.get(0).getHeight(), true);
		}
		if (previewSizes != null && previewSizes.size() > 1)
		{
			addMultishotResolution(CameraController.MultishotResolutionsIdxesList.size(), previewSizes.get(1)
					.getWidth(), previewSizes.get(1).getHeight(), true);
		}
		

		String prefIdx = appInterface.getSpecialImageSizeIndexPref();
		
		//Search highest resolution in 'fast' image sizes list and use it as default
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

	protected static void addMultishotResolution(int ii, int currSizeWidth, int currSizeHeight, boolean isFast)
	{
		boolean needAdd = true;

		Long lmpix = (long) currSizeWidth * currSizeHeight;
		float mpix = (float) lmpix / 1000000.f;
		float ratio = (float) currSizeWidth / currSizeHeight;

		// find good location in a list
		int loc;
		for (loc = 0; loc < CameraController.MultishotResolutionsMPixList.size(); ++loc)
			if (CameraController.MultishotResolutionsMPixList.get(loc) < lmpix)
				break;

		//Check image ratio of current Size
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
		//'Fast' image size is the size that match some supported preview size
		// When plugin will request still image with that size, CC wont create
		// still capture request, instead of this it will use received preview frame as requested still image
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

	public static List<CameraController.Size> getPreviewSizeList()
	{
		List<CameraController.Size> previewSizes = new ArrayList<CameraController.Size>();
		StreamConfigurationMap configMap = Camera2Controller.getInstance().camCharacter
				.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size[] cs = configMap.getOutputSizes(SurfaceHolder.class);
		//Only with such preview size Galaxy S6 isn't crashed in camera2 mode
		if(CameraController.isGalaxyS6)
			cs = new Size[]{new Size(1920, 1080)};
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
//		Log.e(TAG, "fillPictureSizeList. USE captureFormat.");
		CameraCharacteristics camCharacter = Camera2Controller.getInstance().camCharacter;
		StreamConfigurationMap configMap = camCharacter.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size[] cs = configMap.getOutputSizes(captureFormat);
		
		/*
		 * In case when device supports capturing YUV less maximum size than JPEG
		 * give users available JPEG sizes instead
		 */
		if(captureFormat == CameraController.YUV)
		{
			allYUVSizes = cs;
			allJpegSizes = configMap.getOutputSizes(CameraController.JPEG);
			
			Camera2Controller.highestCurrentImageSize = findMaximumSize(cs);			
			Camera2Controller.highestAvailableImageSize = findMaximumSize(allJpegSizes);
			if(Camera2Controller.highestAvailableImageSize.getWidth() > Camera2Controller.highestCurrentImageSize.getWidth())
				cs = allJpegSizes;
		}
		
		for (Size sz : cs)
		{
			pictureSizes.add(new CameraController.Size(sz.getWidth(), sz.getHeight()));
		}
	}
	
	//Search maximum size in array by comparing size in megapixels
	protected static Size findMaximumSize(Size[] sizes)
	{
		if(sizes.length > 0)
		{
			Size maxSize = sizes[0];
			int maxMPix = maxSize.getWidth() * maxSize.getHeight();
			for(Size sz : sizes)
			{
				int currentMPix = sz.getWidth() * sz.getHeight();
				if(currentMPix > maxMPix)
				{
					maxSize = sz;
					maxMPix = currentMPix;
				}
			}
			return maxSize;
		}
		else
			return new Size(0, 0);
	}

	public static void fillVideoSizeList(List<CameraController.Size> videoSizes)
	{
		CameraCharacteristics camCharacter = Camera2Controller.getInstance().camCharacter;
		StreamConfigurationMap configMap = camCharacter.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
		Size[] cs = configMap.getOutputSizes(MediaRecorder.class);
		for (Size sz : cs)
		{
			videoSizes.add(new CameraController.Size(sz.getWidth(), sz.getHeight()));
		}
	}
	
	
	//Check size chosen by application. Related to case when plugin wants to capture YUV but chosen size available only for JPEG capturing
	public static void checkImageSize(CameraController.Size imageSize)
	{
		if(captureFormat == CameraController.YUV)
		{
			if(!isSizeAvailable(imageSize, captureFormat) && isSizeAvailable(imageSize, CameraController.JPEG))
			{
				originalCaptureFormat = captureFormat;
				ApplicationScreen.setCaptureFormat(CameraController.JPEG);
			}
		}
	}
	
	
	//Utility method to determine whether size is available for capture format
	//Used only for JPEG and YUV formats. Don't use that method for RAW format!
	public static boolean isSizeAvailable(CameraController.Size sz, int format)
	{
		boolean isSizeSupported = false;
		
		int width = sz.getWidth();
		int MPix  = sz.getWidth() * sz.getHeight();
		
		Size[] allSizes = null;
		if(format == CameraController.YUV)
			allSizes = allYUVSizes;
		else if(format == CameraController.JPEG)
			allSizes = allJpegSizes;
		else
			isSizeSupported = true;
		
		for(Size systemSize : allSizes)
		{
			int systemMPix = systemSize.getWidth() * systemSize.getHeight();
			int systemWidth = systemSize.getWidth();
			
			if(systemMPix == MPix && systemWidth == width)
				isSizeSupported = true;
		}
		
		return isSizeSupported;
	}
	
	
	
	public static CameraDevice getCamera2()
	{
		return Camera2Controller.getInstance().camDevice;
	}

	public static CameraCharacteristics getCameraCharacteristics()
	{
		if (Camera2Controller.getInstance().camCharacter != null)
			return Camera2Controller.getInstance().camCharacter;

		return null;
	}

	/* 
	 * Camera parameters interfaces (scene mode, white balance, exposure lock and etc)
	 */
	
	public static boolean setAutoExposureLock(boolean lock)
	{
		if (previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			previewRequestBuilder.set(CaptureRequest.CONTROL_AE_LOCK, lock);
			setRepeatingRequest();
			
			PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
			.putBoolean(ApplicationScreen.sAELockPref, lock).commit();
			
			return true;
		}
		
		return false;
	}
	
	public static boolean setAutoWhiteBalanceLock(boolean lock)
	{
		if (previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_LOCK, lock);
			setRepeatingRequest();
			
			PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
			.putBoolean(ApplicationScreen.sAWBLockPref, lock).commit();
			
			return true;
		}
		
		return false;
	}
	
	public static boolean isExposureLocked()
	{
		if (previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			return PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sAELockPref, false);
		}
		
		return false;
	}
	
	
	public static boolean isWhiteBalanceLocked()
	{
		if (previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			return PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sAWBLockPref, false);
		}
		
		return false;
	}
	
	
	public static boolean isZoomSupportedCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null)
		{
			float maxzoom = Camera2Controller.getInstance().camCharacter
					.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);
			return maxzoom > 0 ? true : false;
		}

		return false;
	}

	public static float getMaxZoomCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM) != null)
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_MAX_DIGITAL_ZOOM);

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
		//Zoom area is calculated relative to sensor area (activeRect)
		zoomCropPreview = getZoomRect(zoomLevel, activeRect.width(), activeRect.height());
		if(previewRequestBuilder != null)
		{
			previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropPreview);
			setRepeatingRequest();
		}
	}

	public static float getZoom()
	{
		return zoomLevel;
	}

	//Calculate zoom area according input image size
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

	public static boolean isExposureCompensationSupportedCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) != null)
		{
			Range<Integer> expRange = Camera2Controller.getInstance().camCharacter
					.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange.getLower() == expRange.getUpper() ? false : true;
		}

		return false;
	}

	public static int getMinExposureCompensationCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) != null)
		{
			Range<Integer> expRange = Camera2Controller.getInstance().camCharacter
					.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange.getLower();
		}

		return 0;
	}

	public static int getMaxExposureCompensationCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE) != null)
		{
			Range<Integer> expRange = Camera2Controller.getInstance().camCharacter
					.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange.getUpper();
		}

		return 0;
	}

	public static float getExposureCompensationStepCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP) != null)
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP)
					.floatValue();
		return 0;
	}

	public static void resetExposureCompensationCamera2()
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
			Camera2Controller.setRepeatingRequest();
		}
	}

	public static int[] getSupportedSceneModesCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES) != null)
		{
			int[] scenes = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
			if (scenes.length > 0 && scenes[0] != CameraCharacteristics.CONTROL_SCENE_MODE_DISABLED)
				return scenes;
		}

		return new int[0];
	}

	public static int[] getSupportedWhiteBalanceCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) != null)
		{
			int[] wb = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
			if (wb.length > 0)
				return wb;
		}

		return new int[0];
	}
	
	public static boolean isManualWhiteBalanceSupportedCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES) != null
				&& !CameraController.isNexus6) //Disable manual WB for Nexus 6 - it manages WB wrong
		{
			int[] wb = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
			
			//Only in AWB_MODE_OFF we can manually control color temperature of image data
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

	public static int[] getSupportedFocusModesCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES) != null)
		{
			int[] focus = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
			if (focus.length > 0)
				return focus;
		}

		return new int[0];
	}

	public static boolean isFlashModeSupportedCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) != null)
		{
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.FLASH_INFO_AVAILABLE);
		}

		return false;
	}

	public static int[] getSupportedCollorEffectsCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS) != null)
		{
			int[] collorEffect = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_EFFECTS);
			if (collorEffect.length > 0)
				return collorEffect;
		}

		return new int[]{0};
	}
	
	public static int[] getSupportedISOModesCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null &&
			Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) != null)
		{
			Range<Integer> iso = Camera2Controller.getInstance().camCharacter
					.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
			int max_iso = iso.getUpper();
			int min_iso = iso.getLower();

			int iso_count = 0;
			int index = 0;
			//Count number of supported iso (by OpenCamera) that lay in device's iso range
			for (index = 0; index < CameraController.getIsoModeCamera2().size(); index++)
			{
				int iso_value = CameraController.getIsoModeCamera2().get(index);
				if (max_iso >= iso_value && min_iso <= iso_value)
					++iso_count;
			}
			int[] iso_values = new int[iso_count];

			int iso_index = 0;
			//Put appropriate iso to the list
			for (index = 0; index < CameraController.getIsoModeCamera2().size(); index++)
			{
				int iso_value = CameraController.getIsoModeCamera2().get(index);
				if (max_iso >= iso_value && min_iso <= iso_value)
					iso_values[iso_index++] = CameraController.getIsoValuesList().get(index).byteValue();
			}

			if (iso_values.length > 0)
				return iso_values;
		}
		return new int[0];
	}

	public static long getCameraCurrentExposureCamera2() {
		return currentExposure;
	}
	
	public static int getCameraCurrentSensitivityCamera2() {
		return currentSensitivity;
	}
	
	public static boolean isISOModeSupportedCamera2()
	{
		 if (Camera2Controller.getInstance().camCharacter != null &&
			 Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE) != null)
		 {
			 //ISO is supported only if iso range is wider than 0
			 Range<Integer> iso = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_SENSITIVITY_RANGE);
			 if (iso.getLower() == iso.getUpper())
				 return false;
			 return true;
		 }

		return false;
	}
	
	
	public static boolean isManualFocusDistanceSupportedCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null &&
			Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) != null)
		{
			float minFocusDistance = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
			
			//If the lens is fixed-focus, minimum focus distance will be 0.
			if(minFocusDistance > 0.0f)
				return true;
		}
		
		return false;
	}
	
	public static float getCameraMinimumFocusDistance()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE) != null)
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.LENS_INFO_MINIMUM_FOCUS_DISTANCE);
		
		return 0;
	}
	
	public static long getCameraMinimumExposureTime()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) != null)
		{
			Range<Long> exposureTimeRange = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
			return exposureTimeRange.getLower();
		}
		
		return 0;
	}
	
	public static long getCameraMaximumExposureTime()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE) != null)
		{
			Range<Long> exposureTimeRange = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE);
			return exposureTimeRange.getUpper();
		}
		
		return 0;
	}

	public static int getMaxNumMeteringAreasCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE) != null)
		{
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AE);
		}

		return 0;
	}

	public static int getMaxNumFocusAreasCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
				&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF) != null)
		{
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS_AF);
		}

		return 0;
	}

	public static void setCameraSceneModeCamera2(int mode)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			if (mode != CameraParameters.SCENE_MODE_AUTO)
			{
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, mode);
			} else
			{
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
			}

			Camera2Controller.setRepeatingRequest();

		}

		appInterface.setSceneModePref(mode);
	}

	public static void setCameraWhiteBalanceCamera2(int mode)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			if (mode != CameraParameters.WB_MODE_AUTO)
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
			else
				Camera2Controller.previewRequestBuilder
						.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_USE_SCENE_MODE);

			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, mode);
			
			try
			{
				Camera2Controller.getInstance().configurePreviewRequest(true);
			} catch (CameraAccessException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		appInterface.setWBModePref(mode);
	}
	
	public static void setCameraColorTemperatureCamera2(int iTemp)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			appInterface.setColorTemperature(iTemp);
			try
			{
				Camera2Controller.setCameraWhiteBalanceCamera2(CameraParameters.WB_MODE_OFF);
				Camera2Controller.getInstance().configurePreviewRequest(true);
			} catch (CameraAccessException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void setCameraFocusModeCamera2(int mode)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mode);
			Camera2Controller.setRepeatingRequest();
		}

		appInterface.setFocusModePref(mode);
	}

	//Main method to set flash mode, but flash mode is adjusted also in 2 places:
	// - For preview in method configurePreviewRequest - it called at initialization or after cancelAutoFocus call
	// - For still capturing in method createRequests
	// So, in some cases setting flash mode will be called twice, maybe it's need to re-factore flash mode management
	public static void setCameraFlashModeCamera2(int mode)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			int currentFlash = appInterface.getFlashModePref(ApplicationScreen.sDefaultFlashValue);

			int previewFlash = mode;
			if (mode != CameraParameters.FLASH_MODE_TORCH && currentFlash == CameraParameters.FLASH_MODE_TORCH)
				previewFlash = CameraParameters.FLASH_MODE_OFF;

			if (mode == CameraParameters.FLASH_MODE_TORCH || currentFlash == CameraParameters.FLASH_MODE_TORCH)
			{
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.FLASH_MODE, previewFlash);
				Camera2Controller.setRepeatingRequest();
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
				
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, correctedMode);
				Camera2Controller.setRepeatingRequest();
			}
			else if(mode == CameraParameters.FLASH_MODE_OFF)
			{
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.FLASH_MODE, mode);
				Camera2Controller.setRepeatingRequest();
			}
			
			appInterface.setFlashModePref(mode);
		}
	}

	public static void setCameraISOModeCamera2(int mode)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			if (mode > 0)
			{
				int iso = CameraController.getIsoModeCamera2().get(mode);
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, iso);
			}
			Camera2Controller.setRepeatingRequest();
		}

		appInterface.setISOModePref(mode);
	}

	public static void setCameraExposureCompensationCamera2(int iEV)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null
				&& Camera2Controller.getInstance().mCaptureSession != null)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, iEV);
			Camera2Controller.setRepeatingRequest();
		}

		appInterface.setEVPref(iEV);
	}
	
	/*
	 * Manual sensor parameters: focus distance and exposure time.
	 * Available only in Camera2 mode.
	*/
	public static void setCameraExposureTimeCamera2(long iTime)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null
				&& Camera2Controller.getInstance().mCaptureSession != null)
		{
			boolean isRealExposureTimeOnPreview = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sRealExposureTimeOnPreviewPref, false);
			long exposureTime = iTime;
			long frameDuration = 0;
			int  sensorSensitivity = CameraController.getIsoModeCamera2().get(appInterface.getISOModePref(1));

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
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);
			
			if(frameDuration > 0)
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration);
			
			if(sensorSensitivity > 0)
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, sensorSensitivity);
			
			Camera2Controller.setRepeatingRequest();
			isManualExposureTime = true;
		}

		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
				.putLong(ApplicationScreen.sExposureTimePref, iTime).commit();
	}
	
	public static void setCameraCollorEffectCamera2(int mode)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_EFFECT_MODE, mode);
			Camera2Controller.setRepeatingRequest();
		}
	}
	
	public static void resetCameraAEModeCamera2()
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null
				&& Camera2Controller.getInstance().mCaptureSession != null)
		{
			try
			{
				Camera2Controller.getInstance().configurePreviewRequest(true);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	
	public static void setCameraFocusDistanceCamera2(float fDistance)
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null
				&& Camera2Controller.getInstance().mCaptureSession != null)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.LENS_FOCUS_DISTANCE, fDistance);
			Camera2Controller.setRepeatingRequest();
		}

		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
				.putFloat(ApplicationScreen.sFocusDistancePref, fDistance).commit();
		
		PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).edit()
				.putInt(CameraController.isFrontCamera() ? ApplicationScreen.sRearFocusModePref
				: ApplicationScreen.sFrontFocusModePref, CameraParameters.MF_MODE).commit();
	}
	//////////////////////////////////////////////////////////////////////////////////////

	public static void setCameraFocusAreasCamera2(List<Area> focusAreas)
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

				Matrix matrix = new Matrix();
				matrix.setScale(1, 1);
				matrix.preTranslate(1000.0f, 1000.0f);
				matrix.postScale((zoomRect.width() - 1) / 2000.0f, (zoomRect.height() - 1) / 2000.0f);

				RectF rectF = new RectF(r.left, r.top, r.right, r.bottom);
				matrix.mapRect(rectF);
				Util.rectFToRect(rectF, r);

				int currRegion = i;
				af_regions[currRegion] = new MeteringRectangle(r.left, r.top, r.right, r.bottom, 1000);
			}
		} else
		{
			af_regions = new MeteringRectangle[1];
			af_regions[0] = new MeteringRectangle(0, 0, activeRect.width() - 1, activeRect.height() - 1, 1000);
		}

		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
			Camera2Controller.setRepeatingRequest();
		}

	}

	public static void setCameraMeteringAreasCamera2(List<Area> meteringAreas)
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

		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, ae_regions);
			Camera2Controller.setRepeatingRequest();
			isManualExposureTime = false;
		}
	}
	
	//Repeating request used for preview frames
	public static void setRepeatingRequest()
	{
		//Second part of operator if is experimental. Used only for RAW capturing in Super mode on Galaxy S6
		if(Camera2Controller.getInstance().mCaptureSession == null/* || (inCapture == true && lastCaptureFormat == CameraController.YUV_RAW && CameraController.isGalaxyS6)*/)
			return;

		try
		{
			Camera2Controller.getInstance().mCaptureSession.setRepeatingRequest(Camera2Controller.previewRequestBuilder.build(), captureCallback, null);
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
		} catch (IllegalStateException e2)
		{
			e2.printStackTrace();
		}
	}

	public static int getPreviewFrameRateCamera2()
	{
		if (Camera2Controller.getInstance().camCharacter != null
			&& Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES) != null)
		{
			Range<Integer>[] range;
			range = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
			return range[range.length - 1].getUpper();
		}

		return 0;
	}

	public static float getVerticalViewAngle()
	{
		if (Camera2Controller.getInstance().camCharacter != null)
		{
			float[] focalLenghts = Camera2Controller.getInstance().camCharacter
					.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
			SizeF sensorSize = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);
			
			// sensorSize contains pixel size, not physical sensor size.
			if (sensorSize.getHeight() == sensorSize.getWidth()) {
				sensorSize = new SizeF(sensorSize.getWidth() * activeRect.width() / 1000, sensorSize.getWidth() * activeRect.height() / 1000);
			}

			float sensorHeight = sensorSize.getHeight();
			float alphaRad = (float) (2 * Math.atan2(sensorHeight, 2 * focalLenghts[0]));
			float alpha = (float) (alphaRad * (180 / Math.PI));

			return alpha;

		} else if (CameraController.isNexus)
			return 46.66f;

		return 42.7f;
	}

	public static float getHorizontalViewAngle()
	{
		if (Camera2Controller.getInstance().camCharacter != null)
		{
			float[] focalLenghts = Camera2Controller.getInstance().camCharacter
					.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS);
			SizeF sensorSize = Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_INFO_PHYSICAL_SIZE);

			// sensorSize contains pixel size, not physical sensor size.
			if (sensorSize.getHeight() == sensorSize.getWidth()) {
				sensorSize = new SizeF(sensorSize.getWidth() * activeRect.width() / 1000, sensorSize.getWidth() * activeRect.height() / 1000);
			}
			
			float sensorWidth = sensorSize.getWidth();
			float alphaRad = (float) (2 * Math.atan2(sensorWidth, 2 * focalLenghts[0]));
			float alpha = (float) (alphaRad * (180 / Math.PI));

			return alpha;
		} else if (CameraController.isNexus)
			return 59.63f;

		return 55.4f;
	}

	public static int getSensorOrientation(int cameraIndex)
	{
		if(CameraController.cameraIdList == null || CameraController.cameraIdList.length == 0)
			return -1;
		try
		{
			Camera2Controller.getInstance().camCharacter = Camera2Controller.getInstance().manager
					.getCameraCharacteristics(CameraController.cameraIdList[cameraIndex]);
			
			return Camera2Controller.getInstance().camCharacter.get(CameraCharacteristics.SENSOR_ORIENTATION);
		} catch (CameraAccessException e)
		{
			e.printStackTrace();
			return -1;
		}
	}

	

    //Create 3 request:
	//pre-capture request - used for exposure metering before main capture occurs
	//still request - main capture request for still image
	//raw request - in case of RAW format requested
	public static void CreateRequests(final int format) throws CameraAccessException
	{
		final boolean isRAWCapture = (format == CameraController.RAW);

		stillRequestBuilder = Camera2Controller.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
		precaptureRequestBuilder = Camera2Controller.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
		rawRequestBuilder = Camera2Controller.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
		
		//Set Noise reduction and Edge modes for different capture formats.
		if (format == CameraController.YUV_RAW)
		{
			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
			
			precaptureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
			precaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
		}
		else 
		{
			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
					CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
			
			precaptureRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
			precaptureRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE,
					CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
			
			if (isRAWCapture)
			{
				rawRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_OFF);
				rawRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_OFF);
			}
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
		
		
		if (format == CameraController.JPEG || captureFormat == CameraController.JPEG)
		{
			stillRequestBuilder.addTarget(appInterface.getJPEGImageSurface());
		}
		else if (format == CameraController.YUV || format == CameraController.YUV_RAW)
		{
			//Temporary disable super mode for Galaxy S6
//			if (CameraController.isGalaxyS6 && format == CameraController.YUV_RAW)
//				stillRequestBuilder.addTarget(appInterface.getRAWImageSurface()); //Used only for Super mode
//			else
				stillRequestBuilder.addTarget(appInterface.getYUVImageSurface());
		}
		else if (format == CameraController.RAW)
		{
			rawRequestBuilder.addTarget(appInterface.getRAWImageSurface());
			stillRequestBuilder.addTarget(appInterface.getJPEGImageSurface());
		}
		precaptureRequestBuilder.addTarget(appInterface.getPreviewYUVImageSurface());
		
		
		boolean isAutoExTime = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sExposureTimeModePref, true);
		long exTime = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getLong(ApplicationScreen.sExposureTimePref, 0);
		
		boolean isAutoFDist = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(ApplicationScreen.sFocusDistanceModePref, true);
		float fDist = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getFloat(ApplicationScreen.sFocusDistancePref, 0);
		
		//Manual focus distance
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
		
		int flashMode = appInterface.getFlashModePref(ApplicationScreen.sDefaultFlashValue);
		if (flashMode == CameraParameters.FLASH_MODE_CAPTURE_TORCH)
		{
			// If flashMode == FLASH_MODE_CAPTURE_TORCH, then turn on torch for captureRequests.
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.FLASH_MODE, CameraParameters.FLASH_MODE_TORCH);
			Camera2Controller.setRepeatingRequest();

			flashMode = CameraParameters.FLASH_MODE_TORCH;
		}
		
		if(isAutoExTime)
		{
			if(flashMode == CameraParameters.FLASH_MODE_SINGLE || flashMode == CameraParameters.FLASH_MODE_AUTO || flashMode == CameraParameters.FLASH_MODE_REDEYE)
			{
				if(flashMode == CameraParameters.FLASH_MODE_SINGLE)
					flashMode = CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH;
				else if(flashMode == CameraParameters.FLASH_MODE_AUTO )
					flashMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH;
				else if(flashMode == CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE )
					flashMode = CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE;
								
				Camera2Controller.stillRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
				Camera2Controller.stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, flashMode);
				
				Camera2Controller.precaptureRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
				Camera2Controller.precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, flashMode);
				
				Camera2Controller.rawRequestBuilder.set(CaptureRequest.CONTROL_MODE, CaptureRequest.CONTROL_MODE_AUTO);
				Camera2Controller.rawRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, flashMode);
			}
			else if(flashMode == CameraParameters.FLASH_MODE_TORCH || flashMode == CameraParameters.FLASH_MODE_OFF)
			{
				Camera2Controller.stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
				Camera2Controller.stillRequestBuilder.set(CaptureRequest.FLASH_MODE, flashMode);
				
				Camera2Controller.precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
				Camera2Controller.precaptureRequestBuilder.set(CaptureRequest.FLASH_MODE, flashMode);
				
				Camera2Controller.rawRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
				Camera2Controller.rawRequestBuilder.set(CaptureRequest.FLASH_MODE, flashMode);
			}
		}
		else //Manual exposure time
		{
			Camera2Controller.stillRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
			Camera2Controller.stillRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exTime);
			Camera2Controller.precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
			Camera2Controller.precaptureRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exTime);
			if (isRAWCapture)
			{
				Camera2Controller.rawRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_OFF);
				Camera2Controller.rawRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exTime);
			}
		}
	}

	//Used in case of multishot capturing to setting up request for each frame
	// ev - exposure compensation
	// gain - sensor sensitivity (ISO)
	// expo - exposure time value in case of manual exposure time preference
	private static void SetupPerFrameParameters(int ev, int gain, long expo, boolean isRAWCapture)
	{
		// explicitly disable AWB for the duration of still/burst capture to get
		// full burst with the same WB
		// WB does not apply to RAW, so no need for this in rawRequestBuilder
		if (!CameraController.isGalaxyS6)
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
	
	
	private static CaptureRequest.Builder setConstantPowerGamma(CaptureRequest.Builder request)
	{
		// using constant power 2.2 tone-mapping
		// so that manipulations with the luminance do not skew the colors
		
		request.set(CaptureRequest.TONEMAP_MODE,
				CaptureRequest.TONEMAP_MODE_CONTRAST_CURVE);
		
		float[] t22 = new float[]{
		            0.0000f, 0.0000f, 0.0667f, 0.2920f, 0.1333f, 0.4002f, 0.2000f, 0.4812f,
		            0.2667f, 0.5484f, 0.3333f, 0.6069f, 0.4000f, 0.6594f, 0.4667f, 0.7072f,
		            0.5333f, 0.7515f, 0.6000f, 0.7928f, 0.6667f, 0.8317f, 0.7333f, 0.8685f,
		            0.8000f, 0.9035f, 0.8667f, 0.9370f, 0.9333f, 0.9691f, 1.0000f, 1.0000f };
		// linear
		// float[] t22 = new float[]{ 0,0, 1,1};
		
		TonemapCurve t22curve = new TonemapCurve(t22, t22, t22);
		request.set(CaptureRequest.TONEMAP_CURVE, t22curve);
		
		return request;
	}

	
	/*
	 * captureImageWithParams*** methods
	 * sequence of calls is as follows:
	 * 
	 * 1) captureImageWithParamsCamera2
	 * 2) captureImageWithParamsCamera2Allowed  (if preview is already focused or after successful callback of auto focus)
	 * 3) captureImageWithParamsCamera2Simple
	 * 
	 *    if pause between shots is requested:
	 *   4a) captureNextImageWithParams (initiate pause)
	 *    
	 *    if no pause requested:
	 *   4b) captureSession.capture - final stage for logic without pause between shots
	 *   
	 * 5) captureNextImageWithParamsSimple (called after pause between shots in multishot sequence)
	 * 	 5a) captureSession.capture - final stage for multishot with pauses logic
	 */
	
	//Method that called from CameraController
	//it starts sequence of tuning still capture requests
	//Call next methods only when capture become allowed (preview is focused)
	public static int captureImageWithParamsCamera2(final int nFrames, final int format, final int[] pause,
													final int[] evRequested, final int[] gain, final long[] exposure,
													final boolean setPowerGamma, final boolean resInHeap, final boolean indication)
	{
//		inCapture = true; Debug variable. Used in logic to capture RAW in Super mode on Galaxy S6
		int requestID = -1;

		if (CameraController.getFocusMode() == CameraParameters.AF_MODE_CONTINUOUS_PICTURE && captureAllowed == false)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_START);
			
			if(Camera2Controller.getInstance().mCaptureSession != null)
			{
				try
				{
					Camera2Controller.getInstance().mCaptureSession.capture(Camera2Controller.previewRequestBuilder.build(), captureCallback, null);
				}
				catch (CameraAccessException e)
				{
					e.printStackTrace();
				}
			}
		}

		if (!captureAllowed)
		{
			new CountDownTimer(2000, 10)
			{
				public void onTick(long millisUntilFinished)
				{
					if (captureAllowed)
					{
						this.cancel();
						captureImageWithParamsCamera2Allowed(nFrames, format, pause, evRequested, gain, exposure, setPowerGamma, resInHeap, indication);
					}
				}

				public void onFinish()
				{
					captureImageWithParamsCamera2Allowed(nFrames, format, pause, evRequested, gain, exposure, setPowerGamma, resInHeap, indication);
				}
			}.start();
		}
		else
			captureImageWithParamsCamera2Allowed(nFrames, format, pause, evRequested, gain, exposure, setPowerGamma, resInHeap, indication);
		
		return requestID;
	}
	
	
	//Called when capture is allowed (actually when camera is focused)
	//First of all make pre-capture request for exposure metering
	//For all devices lower that HARDWARE_LEVEL_FULL or LIMITED just call capture method without pre-capture
	public static void captureImageWithParamsCamera2Allowed (final int nFrames, final int format, final int[] pause,
			final int[] evRequested, final int[] gain, final long[] exposure, final boolean setPowerGamma, final boolean resInHeap, final boolean indication) {
		try
		{
			lastCaptureFormat = format;
			CreateRequests(format);
			
			if(setPowerGamma)
			{
				stillRequestBuilder = setConstantPowerGamma(stillRequestBuilder);
				precaptureRequestBuilder = setConstantPowerGamma(precaptureRequestBuilder);
				if (format == CameraController.RAW)
					rawRequestBuilder = setConstantPowerGamma(rawRequestBuilder);
			}
			
			// Nexus 5 fix flash in dark conditions and exposure set to 0.
			if(CameraController.isNexus5)
			{
				int selectedEvCompensation = 0;
				selectedEvCompensation = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getInt(ApplicationScreen.sEvPref, 0);
				if ((stillRequestBuilder.get(CaptureRequest.CONTROL_AE_MODE) == CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH
						|| stillRequestBuilder.get(CaptureRequest.CONTROL_AE_MODE) == CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH
						|| stillRequestBuilder.get(CaptureRequest.CONTROL_AE_MODE) == CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH_REDEYE)
						&& evRequested == null && selectedEvCompensation == 0) {
					precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 1);
				}
			}

			if (checkHardwareLevel())
			{
				if(Camera2Controller.getInstance().mCaptureSession != null)
				{
					precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
							CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
					Camera2Controller.getInstance().mCaptureSession.capture(precaptureRequestBuilder.build(),
							new CameraCaptureSession.CaptureCallback()
							{
								@Override
								public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
										TotalCaptureResult result)
								{
									precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
											CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
									
									captureImageWithParamsCamera2Simple(nFrames, format, pause,
											evRequested, gain, exposure, setPowerGamma, resInHeap, indication);
								}
							}, null);
				}
			} else
			{
				captureImageWithParamsCamera2Simple(nFrames, format, pause,
						evRequested, gain, exposure, setPowerGamma, resInHeap, indication);
			}
		} catch (CameraAccessException e)
		{
			Log.e(TAG, "setting up still image capture request failed");
			e.printStackTrace();
			throw new RuntimeException();
		}

	}


	//Exact here we request capture session to capture frame
	public static int captureImageWithParamsCamera2Simple(final int nFrames, final int format, final int[] pause,
														  final int[] evRequested, final int[] gain, final long[] exposure,
														  final boolean setPowerGamma, final boolean resInHeap, final boolean indication)
	{
		
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
		
		//Check of pause duration between shot in case of multishot request
		boolean hasPause = false;
		if(pause != null)
			for(int p : pause)
				if (p > 0)
				{
					hasPause = true;
					break;
				}
			
		resultInHeap = resInHeap;
		indicateCapturing = indication;
		
		manualPowerGamma = setPowerGamma;
		
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
									   evCompensation == null ? selectedEvCompensation : evCompensation[currentFrameIndex],
									   sensorGain == null ? currentSensitivity : sensorGain[currentFrameIndex],
									   exposureTime == null ? 0 : exposureTime[currentFrameIndex], manualPowerGamma);
		} else
		{
			pauseBetweenShots = new int[totalFrames];
			appInterface.showCaptureIndication(indicateCapturing);
			
			/*
			 * Debug code for Galaxy S6. Trying to capture RAW with stopped preview (coz it hangs while capturing RAW)
			 */
//			if(Camera2.getInstance().mCaptureSession != null)
//			{
//				if(CameraController.isGalaxyS6/* && nFrames > 1*/)
//					try 
//					{
//						Camera2.getInstance().mCaptureSession.stopRepeating();
//						Log.wtf(TAG, "Capture SUPER. stop preview!");
//					}
//					catch (CameraAccessException e1)
//					{
//						Log.e(TAG, "Can't stop preview");
//						e1.printStackTrace();
//					}
//			}

			for (int n = 0; n < nFrames; ++n)
			{
				SetupPerFrameParameters(evRequested == null ? selectedEvCompensation : evRequested[n], gain == null ? currentSensitivity : gain[n],
										exposure == null ? 0 : exposure[n], isRAWCapture);

				if(Camera2Controller.getInstance().mCaptureSession != null)
				{
					try
					{
						requestID = Camera2Controller.getInstance().mCaptureSession.capture(stillRequestBuilder.build(),
																							stillCaptureCallback, null);
	
						pluginManager.addRequestID(n, requestID);
						// FixMe: Why aren't requestID assigned if there is request with ev's being adjusted??
	//						if (evRequested == null) requestID = tmp;
						
						if(isRAWCapture)
							Camera2Controller.getInstance().mCaptureSession.capture(rawRequestBuilder.build(),
									stillCaptureCallback, null);
					} catch (CameraAccessException e)
					{
						e.printStackTrace();
					}
				}
			}
		}

		return requestID;

	}
	
	
	//Method to capture next image in case of multishot requested
	//Called for all frames instead very first frame
	public static int captureNextImageWithParams(final int format, final int frameIndex, final int pause, final int evRequested,
			final int gain, final long exposure, final boolean setPowerGamma)
	{
		int requestID = -1;

		try
		{
			CreateRequests(format);
			
			if(setPowerGamma)
			{
				stillRequestBuilder = setConstantPowerGamma(stillRequestBuilder);
				precaptureRequestBuilder = setConstantPowerGamma(precaptureRequestBuilder);
				if (format == CameraController.RAW)
					rawRequestBuilder = setConstantPowerGamma(rawRequestBuilder);
			}

			final boolean isRAWCapture = (format == CameraController.RAW);
			SetupPerFrameParameters(evRequested, gain, exposure, isRAWCapture);
			
			if (checkHardwareLevel())
			{
				if(Camera2Controller.getInstance().mCaptureSession != null)
				{
					precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
							CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_START);
					requestID = Camera2Controller.getInstance().mCaptureSession.capture(precaptureRequestBuilder.build(),
							new CameraCaptureSession.CaptureCallback()
							{
								@Override
								public void onCaptureCompleted(CameraCaptureSession session, CaptureRequest request,
										TotalCaptureResult result)
								{
//									Log.e(TAG, "TRIGER CAPTURE COMPLETED");
									
									precaptureRequestBuilder.set(CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER,
											CaptureRequest.CONTROL_AE_PRECAPTURE_TRIGGER_IDLE);
									
									captureNextImageWithParamsSimple(format, frameIndex, pause, evRequested, gain, exposure);
								}
							}, null);
				}
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

	private static int captureNextImageWithParamsSimple(final int format, final int frameIndex,
														final int pause, final int evRequested,
														final int gain, final long exposure)
	{
		int requestID = -1;

		final boolean isRAWCapture = (format == CameraController.RAW);

		if (pause > 0)
		{
			new CountDownTimer(pause, pause)
			{
				public void onTick(long millisUntilFinished){}

				public void onFinish()
				{
					if(Camera2Controller.getInstance().mCaptureSession != null)
					{
						// play tick sound
						appInterface.showCaptureIndication(indicateCapturing);
						try
						{
							// FixMe: Why aren't requestID assigned if there is
							// request with ev's being adjusted??
							int requestID = Camera2Controller.getInstance().mCaptureSession.capture(stillRequestBuilder.build(),
																									captureCallback, null);
	
							pluginManager.addRequestID(frameIndex, requestID);
							if (isRAWCapture)
								Camera2Controller.getInstance().mCaptureSession.capture(rawRequestBuilder.build(), captureCallback,
										null);
						}
						catch (CameraAccessException e)
						{
							e.printStackTrace();
						}
					}
				}
			}.start();

		} else if(Camera2Controller.getInstance().mCaptureSession != null)
		{
			// play tick sound
			appInterface.showCaptureIndication(true);

			try
			{
				Camera2Controller.getInstance().mCaptureSession
						.capture(stillRequestBuilder.build(), stillCaptureCallback, null);
				if (isRAWCapture)
					Camera2Controller.getInstance().mCaptureSession.capture(rawRequestBuilder.build(), stillCaptureCallback,
							null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}

		return requestID;
	}
	
	


	//Initiate auto focus regardless to focus mode
	//actually used to 'lock' focus before manual exposure metering is set
	public static void forceFocusCamera2()
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null && Camera2Controller.getInstance().mCaptureSession != null)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_START);
			try
			{
				Camera2Controller.getInstance().mCaptureSession.capture(Camera2Controller.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
	}
	
	public static boolean autoFocusCamera2()
	{
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null && Camera2Controller.getInstance().mCaptureSession != null)
		{
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, af_regions);
			
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
					CameraCharacteristics.CONTROL_AF_TRIGGER_START);
			try
			{
				Camera2Controller.getInstance().mCaptureSession.capture(Camera2Controller.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
				return false;
			}
			
			Camera2Controller.autoFocusTriggered = true;
			return true;
		}
		return false;
	}

	public static void cancelAutoFocusCamera2()
	{
		int focusMode = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).
						getInt(CameraController.isFrontCamera() ? ApplicationScreen.sRearFocusModePref : ApplicationScreen.sFrontFocusModePref, -1);
		
		//Canceling is usefull only in auto focus modes, not in manual focus
		if (Camera2Controller.previewRequestBuilder != null && Camera2Controller.getInstance().camDevice != null &&
			focusMode != CameraParameters.MF_MODE && Camera2Controller.getInstance().mCaptureSession != null)
		{
			if(Camera2Controller.getInstance().mCaptureSession == null)
				return;
				
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
														CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL);
			try
			{
				Camera2Controller.getInstance().mCaptureSession.capture(Camera2Controller.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
			
			// Force set IDLE to prevent canceling all the time.
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
														CameraCharacteristics.CONTROL_AF_TRIGGER_IDLE);
			try
			{
				Camera2Controller.getInstance().mCaptureSession.capture(Camera2Controller.previewRequestBuilder.build(), captureCallback, null);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
			
			try
			{
				Camera2Controller.getInstance().configurePreviewRequest(true);
			} catch (CameraAccessException e)
			{
				e.printStackTrace();
			}

			Camera2Controller.autoFocusTriggered = false;
		}
		Camera2Controller.autoFocusTriggered = false;
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
		int iso 	  = CameraController.getIsoModeCamera2().get(appInterface.getISOModePref(1));
		
		int antibanding = appInterface.getAntibandingModePref();
		
		boolean aeLock = appInterface.getAELockPref();
		boolean awbLock = appInterface.getAWBLockPref();
		
		int colorEffect = appInterface.getColorEffectPref();

//		Log.e(TAG, "configurePreviewRequest()");
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
			if(flashMode == CameraParameters.FLASH_MODE_SINGLE || flashMode == CameraParameters.FLASH_MODE_AUTO || flashMode == CameraParameters.FLASH_MODE_REDEYE)
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
			else if(flashMode == CameraParameters.FLASH_MODE_TORCH || flashMode == CameraParameters.FLASH_MODE_OFF)
			{
				previewRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
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
//				tmpCalc = 300 * Math.pow(tmpCalc, -0.1332047592);
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
////				tmpCalc = 300 * Math.log(tmpCalc) - 305.0447927307;
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
//			float X = 255;
//			if(G_even > 0 && R > 0 && B > 0)
//				X = Math.min(G_even, Math.min(R, B));
//			else if(R > 0 && G_even > 0)
//				X = Math.min(R, G_even);
			
//			if(R > 0)
//				R = (float) (Math.pow(X/R, 2.2f) * multiplierR);
//				R = (X/R) * multiplierR;
				R = (R/255) * multiplierR;
//			if(G_even > 0)
//				G_even = (float) (Math.pow(X/G_even, 2.2f) * multiplierG);
//				G_even = (X/G_even) * multiplierG;
				G_even = (G_even/255) * multiplierG;
			G_odd = G_even;
//			if(B > 0)
//				B = (float) (Math.pow(X/B, 2.2f) * multiplierB);
//				B = (X/B) * multiplierB;
				B = (B/255) * multiplierB;
			
			rggbVector = new RggbChannelVector(R, G_even, G_odd, B);
//			RggbChannelVector rggb = new RggbChannelVector(R, 1.0f, 1.0f, B);
			
//			Log.e(TAG, "RGGB: R:" + R + " G:" + G_even + " B:" + B);
//			Log.e(TAG, "RGGB vector: " + rggbVector.toString());
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
			Camera2Controller.previewRequestBuilder.set(CaptureRequest.SENSOR_EXPOSURE_TIME, exposureTime);
			
			if(frameDuration > 0)
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.SENSOR_FRAME_DURATION, frameDuration);
			
			if(sensorSensitivity > 0)
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.SENSOR_SENSITIVITY, sensorSensitivity);
		}

		Surface cameraSurface = appInterface.getCameraSurface();
		if(cameraSurface != null)
			previewRequestBuilder.addTarget(appInterface.getCameraSurface());
		
		
		//Disable Image Reader for Nexus 6 according to slow focusing issue
		if (!CameraController.isNexus6  && captureFormat != CameraController.RAW)
		{
			Surface previewSurface = appInterface.getPreviewYUVImageSurface();
			if(previewSurface != null)
				previewRequestBuilder.addTarget(previewSurface);
		}
		
		if(needZoom && zoomCropPreview != null)
			previewRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCropPreview);

		setRepeatingRequest();
	}

	// Camera2 ------------------------------------------------ camera-related
	// Callbacks
	@SuppressLint("Override")
	public final static CameraDevice.StateCallback openCallback = new CameraDevice.StateCallback()
	{
		@Override
		public void onDisconnected(CameraDevice arg0)
		{
//			Log.e(TAG, "CameraDevice.StateCallback.onDisconnected");
			if (Camera2Controller.getInstance().camDevice != null)
			{
				try
				{
					Camera2Controller.getInstance().camDevice.close();
					Camera2Controller.getInstance().camDevice = null;
				}
				catch (Exception e)
				{
					Camera2Controller.getInstance().camDevice = null;
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
//			Log.e(TAG, "CameraDevice.StateCallback.onOpened");

			Camera2Controller.getInstance().camDevice = arg0;

			messageHandler.sendEmptyMessage(ApplicationInterface.MSG_CAMERA_OPENED);
		}

		@Override
		public void onClosed(CameraDevice arg0)
		{
//			Log.d(TAG,"CameraDevice.StateCallback.onClosed");
			CameraController.sendMessage(ApplicationInterface.MSG_CAMERA_STOPED, 0);
		}
	};

	public final static CameraCaptureSession.StateCallback captureSessionStateCallback = new CameraCaptureSession.StateCallback()
	{
		@Override
		public void onConfigureFailed(final CameraCaptureSession session)
		{
			Log.e(TAG, "CaptureSessionConfigure failed");
			onPauseCamera2();
			appInterface.stopApplication();
		}

		@Override
		public void onConfigured(final CameraCaptureSession session)
		{
			Camera2Controller.getInstance().mCaptureSession = session;

			try
			{
				try
				{
					Camera2Controller.getInstance().configurePreviewRequest(false);
				} catch (CameraAccessException e)
				{
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				finally
				{
					if (CameraController.isCamera2OnRelaunchUsed())
					{
						CameraController.useCamera2OnRelaunch(false);
						appInterface.relaunchCamera();
					} else
					{
//						Log.e(TAG, "Session.onConfigured");
						CameraController.sendMessage(ApplicationInterface.MSG_CAMERA_CONFIGURED, 0);
					}
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
//				if(Camera2.autoFocusTriggered)
//					Log.e(TAG, "CAPTURE_AF_STATE = " + result.get(CaptureResult.CONTROL_AF_STATE));
				if (result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED
						&& Camera2Controller.autoFocusTriggered)
				{
//					 Log.e(TAG, "onCaptureCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED");
					resetCaptureCallback();
					CameraController.onAutoFocus(true);
					Camera2Controller.autoFocusTriggered = false;
	
				}
				else if (result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED
						&& Camera2Controller.autoFocusTriggered)
				{
//					Log.e(TAG, "onCaptureCompleted. CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED");
					resetCaptureCallback();
					CameraController.onAutoFocus(false);
					Camera2Controller.autoFocusTriggered = false;
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
			 
			 ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_EV_CHANGED);
			 ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_ISO_CHANGED);
			
			 if (request.get(CaptureRequest.SENSOR_SENSITIVITY) >= 50 && currentSensitivity != request.get(CaptureRequest.SENSOR_SENSITIVITY) && request.get(CaptureRequest.CONTROL_AE_MODE) == CaptureRequest.CONTROL_AE_MODE_OFF && !resetInProgress) 
			 {
				try {
					resetCaptureCallback();
				} catch (Exception e)
				{
					e.printStackTrace();
				}
			 }
			 
			rggbChannelVector = result
						.get(CaptureResult.COLOR_CORRECTION_GAINS); 
			 
			try {
				int focusState = result.get(CaptureResult.CONTROL_AF_STATE);
				if (focusState == CaptureResult.CONTROL_AF_STATE_PASSIVE_SCAN
						|| focusState == CaptureResult.CONTROL_AF_STATE_PASSIVE_UNFOCUSED)
					captureAllowed = false;
				else if(!captureAllowed)
				{
					resetCaptureCallback();
					captureAllowed = true;
				}
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
			if(Camera2Controller.getInstance().mCaptureSession != null)
			{
				resetInProgress = true;
				
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
						CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL);
				try
				{
					Camera2Controller.getInstance().mCaptureSession.capture(Camera2Controller.previewRequestBuilder.build(), captureCallback, null);
				} catch (CameraAccessException e)
				{
					e.printStackTrace();
				}
				
				// Force set IDLE to prevent canceling all the time.
				Camera2Controller.previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER,
						CameraCharacteristics.CONTROL_AF_TRIGGER_IDLE);
				try
				{
					resetRequestId = Camera2Controller.getInstance().mCaptureSession.capture(Camera2Controller.previewRequestBuilder.build(), captureCallback, null);
				} catch (CameraAccessException e)
				{
					e.printStackTrace();
				}
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
//			Log.e(TAG, "CAPTURE COMPLETED");
			RggbChannelVector rggb = result.get(CaptureResult.COLOR_CORRECTION_GAINS);
			ColorSpaceTransform transformMatrix = result.get(CaptureResult.COLOR_CORRECTION_TRANSFORM);
//			Log.e(TAG, "RGGB = R: " + rggb.getRed() + " G_even: " + rggb.getGreenEven()+ " G_odd: " + rggb.getGreenOdd() + " B: " + rggb.getBlue());
//			Log.e(TAG, "Transform Matrix: " + transformMatrix.toString());
//			Log.e(TAG, "Exposure time = " + result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
//			Log.e(TAG, "Frame duration = " + result.get(CaptureResult.SENSOR_FRAME_DURATION));
//			Log.e(TAG, "Sensor sensitivity = " + result.get(CaptureResult.SENSOR_SENSITIVITY));
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
				Log.e("Camera2", "onImageAvailable");

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
//					Log.e(TAG, "captured JPEG");
					ByteBuffer jpeg = im.getPlanes()[0].getBuffer();

					frame_len = jpeg.limit();
					frameData = new byte[frame_len];
					jpeg.get(frameData,	0, frame_len);
					
					if(Camera2Controller.originalCaptureFormat == ImageFormat.YUV_420_888)
					{
						isYUV = true;
						CameraController.Size imageSize = CameraController.getCameraImageSize();
						
						frame = ImageConversion.JpegConvert(frameData, imageSize.getWidth(),
								imageSize.getHeight(), false, false, 0);
						frame_len = imageSize.getWidth() * imageSize.getHeight() + 2 * ((imageSize.getWidth() + 1) / 2)
								* ((imageSize.getHeight() + 1) / 2);

						pluginManager.collectExifData(null);
						if (!resultInHeap)
						{
							frameData = SwapHeap.SwapFromHeap(frame, frame_len);
							frame = 0;
						}
					}
					else
					{
						pluginManager.collectExifData(frameData);
						if (resultInHeap)
						{
							frame = SwapHeap.SwapToHeap(frameData);
							frameData = null;
						}
					}
				} else if (im.getFormat() == ImageFormat.RAW_SENSOR)
				{
					if (lastCaptureFormat == CameraController.YUV_RAW && CameraController.isGalaxyS6) {
						// This case is for SUPER mode on galaxy s6. And nothing more.
						// It means we have RAW image and need to crop and convert it to YUV.
						
						ByteBuffer raw = im.getPlanes()[0].getBuffer();

						if (!raw.isDirect())
						{
							Log.e(TAG,"Oops, YUV ByteBuffers isDirect failed");
							im.close();
							return;
						}
						
						CameraController.Size imageSize = CameraController.getCameraImageSize();
						CameraController.Size rawImageSize = CameraController.getMaxCameraImageSize(CameraController.RAW);
						int status = YuvImage.CreateYUVImageFromRAW(
								raw, 
								im.getPlanes()[0].getPixelStride(), 
								im.getPlanes()[0].getRowStride(), 
								rawImageSize.getWidth(), 
								rawImageSize.getHeight(), 
								imageSize.getWidth(), 
								imageSize.getHeight(), 
								(int) (rggbChannelVector.getRed() * 256), 
								(int) (rggbChannelVector.getBlue() * 256), 
								blevel, 
								wlevel, 
								4, 
								0);

						if (status != 0)
							Log.e(TAG, "Error while cropping: "	+ status);

						pluginManager.collectExifData(null);
						if (!resultInHeap)
							frameData = YuvImage.GetByteFrame();
						else
							frame = YuvImage.GetFrame();

						frame_len = imageSize.getWidth() * imageSize.getHeight() + imageSize.getWidth()	* ((imageSize.getHeight() + 1) / 2);

						isYUV = true;
						
					} else {
//						Log.e(TAG, "captured RAW");
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
					
				}

				if (im.getFormat() == ImageFormat.RAW_SENSOR && !(lastCaptureFormat == CameraController.YUV_RAW && CameraController.isGalaxyS6)) {
					pluginManager.onImageTaken(frame, frameData, frame_len, CameraController.RAW);
				}
				else
				{
					ApplicationScreen.getPluginManager().onImageTaken(frame, frameData, frame_len, isYUV ? CameraController.YUV : CameraController.JPEG);
					if (CameraController.getFocusMode() != CameraParameters.AF_MODE_CONTINUOUS_PICTURE) {
						Camera2Controller.cancelAutoFocusCamera2();
					}
				}

				currentFrameIndex++;
				if (currentFrameIndex < totalFrames)
					captureNextImageWithParams(
							CameraController.frameFormat, currentFrameIndex,
							pauseBetweenShots == null ? 0 : pauseBetweenShots[currentFrameIndex],
							evCompensation == null ? 0 : evCompensation[currentFrameIndex],
							sensorGain == null ? currentSensitivity : sensorGain[currentFrameIndex],
							exposureTime == null ? 0 : exposureTime[currentFrameIndex], manualPowerGamma);
			}

			// Image should be closed after we are done with it
			im.close();
		}
	};
	
	
	/*
	 * Debug code for Galaxy S6. Trying to capture RAW with stopped preview (coz it hangs while capturing RAW)
	 * At this point we try to resume preview. But now works only full camera re-launch
	 */
//	public static void onCaptureFinished()
//	{
//		inCapture = false;
//		if(lastCaptureFormat == CameraController.YUV_RAW && CameraController.isGalaxyS6)
////		{
////			ApplicationScreen.getGUIManager().setCameraModeGUI(0);
////			ApplicationScreen.instance.pauseMain();
////			ApplicationScreen.instance.switchingMode(false);
////			ApplicationScreen.instance.resumeMain();
////		}
//			try
//			{
//				Camera2.getInstance().configurePreviewRequest(false);
//			} catch (CameraAccessException e)
//			{
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
////			appInterface.relaunchCamera();
//	}
	// ^^ Camera2 code
	// --------------------------------------------------------------
	// camera-related Callbacks
}
