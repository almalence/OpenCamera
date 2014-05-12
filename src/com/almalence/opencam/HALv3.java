package com.almalence.opencam;

import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;

import android.graphics.ImageFormat;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CameraAccessException;

import android.media.Image;
import android.media.ImageReader;
import android.preference.PreferenceManager;
import android.util.Log;

//HALv3 camera's objects
@TargetApi(19)
public class HALv3
{
	private final String TAG = "HALv3Controller";
	
	private static HALv3 instance = null;
	
	public static HALv3 getInstance()
	{
		if (instance == null)
		{
			instance = new HALv3();
		}
		return instance;
	}
	
	public CameraManager manager = null;
	public CameraCharacteristics camCharacter=null;
	public cameraAvailableListener availListener = null;
	public CameraDevice camDevice = null;
	public CaptureRequest.Builder previewRequestBuilder = null;
	
	public static boolean autoFocusTriggered = false;
	
	
	public static void onCreateHALv3()
	{
		// HALv3 code ---------------------------------------------------------
		// get manager for camera devices
		HALv3.getInstance().manager = (CameraManager)MainScreen.mainContext.getSystemService("camera"); // = Context.CAMERA_SERVICE;
		
		// get list of camera id's (usually it will contain just {"0", "1"}
		try {
			CameraController.getInstance().cameraIdList = HALv3.getInstance().manager.getCameraIdList();
		} catch (CameraAccessException e) {
			Log.d("MainScreen", "getCameraIdList failed");
			e.printStackTrace();
		}
	}
	
	public static void onPauseHALv3()
	{
		// HALv3 code -----------------------------------------
		if ((HALv3.getInstance().availListener != null) && (HALv3.getInstance().manager != null))
			HALv3.getInstance().manager.removeAvailabilityListener(HALv3.getInstance().availListener);
		
		if (HALv3.getInstance().camDevice != null)
		{
			HALv3.getInstance().camDevice.close();
			HALv3.getInstance().camDevice = null;
		}
	}
	
	
	public static void openCameraHALv3()
	{
		// HALv3 open camera -----------------------------------------------------------------
		try
		{
			HALv3.getInstance().manager.openCamera (CameraController.getInstance().cameraIdList[CameraController.CameraIndex], HALv3.getInstance().new openListener(), null);
		}
		catch (CameraAccessException e)
		{
			Log.d("MainScreen", "manager.openCamera failed");
			e.printStackTrace();
		}
		
		// find suitable image sizes for preview and capture
		try	{
			HALv3.getInstance().camCharacter = HALv3.getInstance().manager.getCameraCharacteristics(CameraController.getInstance().cameraIdList[CameraController.CameraIndex]);
		} catch (CameraAccessException e) {
			Log.d("MainScreen", "getCameraCharacteristics failed");
			e.printStackTrace();
		}
		
		if (HALv3.getInstance().camCharacter.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
			CameraController.CameraMirrored = true;
		else
			CameraController.CameraMirrored = false;
		
		// Add an Availability Listener as Cameras become available or unavailable
		HALv3.getInstance().availListener = HALv3.getInstance().new cameraAvailableListener();
		HALv3.getInstance().manager.addAvailabilityListener(HALv3.getInstance().availListener, null);
		
		CameraController.getInstance().mVideoStabilizationSupported = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) == null? false : true;
		
		// check that full hw level is supported
		if (HALv3.getInstance().camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) != CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) 
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_NOT_LEVEL_FULL);		
		// ^^ HALv3 open camera -----------------------------------------------------------------
	}
	
	
	public static void setupImageReadersHALv3()
	{
		//surfaceHolder.setFixedSize(MainScreen.imageWidth, MainScreen.imageHeight);
		
		MainScreen.thiz.surfaceHolder.setFixedSize(1280, 720);
		MainScreen.previewWidth = 1280;
		MainScreen.previewHeight = 720;
		
		//MainScreen.thiz.surfaceHolder.setFixedSize(MainScreen.imageWidth, MainScreen.imageHeight);
//		MainScreen.previewWidth = MainScreen.imageWidth;
//		MainScreen.previewHeight = MainScreen.imageHeight;
		
		// HALv3 code -------------------------------------------------------------------
		MainScreen.mImageReaderPreviewYUV = ImageReader.newInstance(MainScreen.previewWidth, MainScreen.previewHeight, ImageFormat.YUV_420_888, 1);
		MainScreen.mImageReaderPreviewYUV.setOnImageAvailableListener(HALv3.getInstance().new imageAvailableListener(), null);
		
		MainScreen.mImageReaderYUV = ImageReader.newInstance(MainScreen.imageWidth, MainScreen.imageHeight, ImageFormat.YUV_420_888, 1);
		MainScreen.mImageReaderYUV.setOnImageAvailableListener(HALv3.getInstance().new imageAvailableListener(), null);
		
		MainScreen.mImageReaderJPEG = ImageReader.newInstance(MainScreen.imageWidth, MainScreen.imageHeight, ImageFormat.JPEG, 1);
		MainScreen.mImageReaderJPEG.setOnImageAvailableListener(HALv3.getInstance().new imageAvailableListener(), null);
	}
	
	
	public static void PopulateCameraDimensionsHALv3()
	{
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<CameraController.Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();
		
		int MinMPIX = CameraController.MIN_MPIX_SUPPORTED;
		CameraCharacteristics params = getCameraParameters2();
		android.hardware.camera2.Size[] cs = params.get(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES);

		CharSequence[] RatioStrings = { " ", "4:3", "3:2", "16:9", "1:1" };

		int iHighestIndex = 0;		
		android.hardware.camera2.Size sHighest = cs[iHighestIndex];
		
		int ii = 0;
		for(android.hardware.camera2.Size s : cs)
		{
			if ((long) s.getWidth() * s.getHeight() > (long) sHighest.getWidth()
					* sHighest.getHeight()) {
				sHighest = s;
				iHighestIndex = ii;
			}

			if ((long) s.getWidth() * s.getHeight() < MinMPIX)
				continue;

			Long lmpix = (long) s.getWidth() * s.getHeight();
			float mpix = (float) lmpix / 1000000.f;
			float ratio = (float) s.getWidth() / s.getHeight();

			// find good location in a list
			int loc;
			for (loc = 0; loc < CameraController.ResolutionsMPixList.size(); ++loc)
				if (CameraController.ResolutionsMPixList.get(loc) < lmpix)
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

			CameraController.ResolutionsNamesList.add(loc,
					String.format("%3.1f Mpix  " + RatioStrings[ri], mpix));
			CameraController.ResolutionsIdxesList.add(loc, String.format("%d", ii));
			CameraController.ResolutionsMPixList.add(loc, lmpix);
			CameraController.ResolutionsSizeList.add(loc, CameraController.getInstance().new Size(s.getWidth(), s.getHeight()));
			
			ii++;
		}

		if (CameraController.ResolutionsNamesList.size() == 0) {
			android.hardware.camera2.Size s = cs[iHighestIndex];

			Long lmpix = (long) s.getWidth() * s.getHeight();
			float mpix = (float) lmpix / 1000000.f;
			float ratio = (float) s.getWidth() / s.getHeight();

			int ri = 0;
			if (Math.abs(ratio - 4 / 3.f) < 0.1f)
				ri = 1;
			if (Math.abs(ratio - 3 / 2.f) < 0.12f)
				ri = 2;
			if (Math.abs(ratio - 16 / 9.f) < 0.15f)
				ri = 3;
			if (Math.abs(ratio - 1) == 0)
				ri = 4;

			CameraController.ResolutionsNamesList.add(0,
					String.format("%3.1f Mpix  " + RatioStrings[ri], mpix));
			CameraController.ResolutionsIdxesList.add(0, String.format("%d", 0));
			CameraController.ResolutionsMPixList.add(0, lmpix);
			CameraController.ResolutionsSizeList.add(0, CameraController.getInstance().new Size(s.getWidth(), s.getHeight()));
		}

		return;
	}
	
	public static void fillPreviewSizeList(List<CameraController.Size> previewSizes)
	{
		android.hardware.camera2.Size[] cs = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES);
		for(android.hardware.camera2.Size sz : cs)
			previewSizes.add(CameraController.getInstance().new Size(sz.getWidth(), sz.getHeight()));
	}
	
	public static void fillPictureSizeList(List<CameraController.Size> pictureSizes)
	{
		android.hardware.camera2.Size[] cs = HALv3.getInstance().camCharacter.get(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES);
		for(android.hardware.camera2.Size sz : cs)
			pictureSizes.add(CameraController.getInstance().new Size(sz.getWidth(), sz.getHeight()));
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
	public static boolean isExposureCompensationSupportedHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			int expRange[] = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange[1] == expRange[0] ? false : true;
		}
		
		return false;	
	}
	
	public static int getMinExposureCompensationHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			int expRange[] = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange[0];
		}
		
		return 0;
	}
	
	public static int getMaxExposureCompensationHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			int expRange[] = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_RANGE);
			return expRange[1];
		}
		
		return 0;
	}

	
	public static float getExposureCompensationStepHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			float step = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_COMPENSATION_STEP).toFloat();
			return step;
		}
		
		return 0;	
	}

	
	public static void resetExposureCompensationHALv3()
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, 0);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
	}
	

	public static byte[] getSupportedSceneModesHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			byte scenes[]  = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
			if(scenes.length > 0 && scenes[0] != CameraCharacteristics.CONTROL_SCENE_MODE_UNSUPPORTED)
				return scenes;				
		}
		
		return null;
	}
	

	public static byte[] getSupportedWhiteBalanceHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			byte wb[]  = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AWB_AVAILABLE_MODES);
			if(wb.length > 0 )
				return wb;				
		}
		
		return null;
	}
	

	public static byte[] getSupportedFocusModesHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			byte focus[]  = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AF_AVAILABLE_MODES);
			if(focus.length > 0 )
				return focus;				
		}
		
		return null;
	}
	
	
	public static boolean isFlashModeSupportedHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == 1? true : false;						
		}
		
		return false;
	}
	
	
	public static int getMaxNumMeteringAreasHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS);
		}
		
		return 0;
	}
	
	public static int getMaxNumFocusAreasHALv3()
	{
		if(HALv3.getInstance().camCharacter != null)
		{
			return HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_MAX_REGIONS);						
		}
		
		return 0;
	}
	
	
	
	
	
	
	public static void setCameraSceneModeHALv3(int mode)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_SCENE_MODE, mode);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.sSceneModePref, mode).commit();
	}
	
	public static void setCameraWhiteBalanceHALv3(int mode)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AWB_MODE, mode);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.sWBModePref, mode).commit();
	}
	

	public static void setCameraFocusModeHALv3(int mode)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, mode);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.getCameraMirrored() ? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, mode).commit();
	}
	
	
	public static void setCameraFlashModeHALv3(int mode)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.FLASH_MODE, mode);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.sFlashModePref, mode).commit();
	}
	
	
	public static void setCameraExposureCompensationHALv3(int iEV)
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AE_EXPOSURE_COMPENSATION, iEV);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.sEvPref, iEV).commit();	
	}
	
	public static int getPreviewFrameRateHALv3()
	{
		int range[] = {0 , 0};
		range = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AE_AVAILABLE_TARGET_FPS_RANGES);
		return range[range.length-1]; 
	}
	
	
	
	
	
	
	public static int captureImageHALv3(int nFrames, int format)
	{
	// stop preview
	//		try {
	//			HALv3.getInstance().camDevice.stopRepeating();
	//		} catch (CameraAccessException e1) {
	//			Log.e("MainScreen", "Can't stop preview");
	//			e1.printStackTrace();
	//		}
			
		// create capture requests for the burst of still images
		//Log.e("CameraController", "captureImage 1");
		int requestID = -1;
		CaptureRequest.Builder stillRequestBuilder = null;
		try
		{
			stillRequestBuilder = HALv3.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
			
			// no re-focus needed, already focused in preview, so keeping the same focusing mode for snapshot
			//stillRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
			// Google: note: CONTROL_AF_MODE_OFF causes focus to move away from current position 
			//stillRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_OFF);
	//					Log.e("CameraController", "captureImage 2");
			if(format == CameraController.JPEG)
			{
				Log.e("HALv3", "Capture JPEG");
				stillRequestBuilder.addTarget(MainScreen.mImageReaderJPEG.getSurface());
	//						Log.e("CameraController", "captureImage 3.1");
			}
			else
			{
				Log.e("HALv3", "Capture YUV");
				stillRequestBuilder.addTarget(MainScreen.mImageReaderYUV.getSurface());
	//						Log.e("CameraController", "captureImage 3.2");
			}
	
			// Google: throw: "Burst capture implemented yet", when to expect implementation?
			/*
			List<CaptureRequest> requests = new ArrayList<CaptureRequest>();
			for (int n=0; n<NUM_FRAMES; ++n)
				requests.add(stillRequestBuilder.build());
			
			camDevice.captureBurst(requests, new captureListener() , null);
			*/
			
			// requests for SZ input frames
			for (int n=0; n<nFrames; ++n)
				requestID = HALv3.getInstance().camDevice.capture(stillRequestBuilder.build(), HALv3.getInstance().new captureListener() , null);
	//					Log.e("CameraController", "captureImage 4");				
			// One more capture for comparison with a standard frame
	//			stillRequestBuilder.set(CaptureRequest.EDGE_MODE, CaptureRequest.EDGE_MODE_HIGH_QUALITY);
	//			stillRequestBuilder.set(CaptureRequest.NOISE_REDUCTION_MODE, CaptureRequest.NOISE_REDUCTION_MODE_HIGH_QUALITY);
	//			// set crop area for the scaler to have interpolation applied by camera HW
	//			stillRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, zoomCrop);
	//			camDevice.capture(stillRequestBuilder.build(), new captureListener() , null);
		}
		catch (CameraAccessException e)
		{
			Log.e("MainScreen", "setting up still image capture request failed");
			e.printStackTrace();
			throw new RuntimeException();
		}
		
		return requestID;
	}
	
	
	public static boolean autoFocusHALv3()
	{
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_START);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.capture(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
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
		if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
		{		
			HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL);
			try 
			{
				CameraController.iCaptureID = HALv3.getInstance().camDevice.capture(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);
			}
			catch (CameraAccessException e)
			{
				e.printStackTrace();
			}
		}	
	}
	
	
	
	public void configurePreviewRequest() throws CameraAccessException
	{
		HALv3.getInstance().previewRequestBuilder = HALv3.getInstance().camDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
		HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
		HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.REQUEST_ID, 555);
		HALv3.getInstance().previewRequestBuilder.addTarget(MainScreen.thiz.getCameraSurface());
		HALv3.getInstance().previewRequestBuilder.addTarget(MainScreen.thiz.getPreviewYUVSurface());
		HALv3.getInstance().camDevice.setRepeatingRequest(HALv3.getInstance().previewRequestBuilder.build(), HALv3.getInstance().new captureListener(), null);		
	}
	
	
	// HALv3 ------------------------------------------------ camera-related listeners

		// Note: never received onCameraAvailable notifications, only onCameraUnavailable
		@TargetApi(19)
		public class cameraAvailableListener extends CameraManager.AvailabilityListener
		{
			@Override
			public void onCameraAvailable(java.lang.String cameraId)
			{
				// should we call this?
				super.onCameraAvailable(cameraId);
				
				Log.d(TAG, "CameraManager.AvailabilityListener.onCameraAvailable");
			}
			
			@Override
			public void onCameraUnavailable(java.lang.String cameraId)
			{
				// should we call this?
				super.onCameraUnavailable(cameraId);
				
				Log.d(TAG, "CameraManager.AvailabilityListener.onCameraUnavailable");
			}
		}

		@TargetApi(19)
		public class openListener extends CameraDevice.StateListener
		{
			@Override
			public void onDisconnected(CameraDevice arg0) {
				Log.d(TAG, "CameraDevice.StateListener.onDisconnected");
			}

			@Override
			public void onError(CameraDevice arg0, int arg1) {
				Log.d(TAG, "CameraDevice.StateListener.onError: "+arg1);
			}

			@Override
			public void onOpened(CameraDevice arg0)
			{
				Log.d(TAG, "CameraDevice.StateListener.onOpened");

				HALv3.getInstance().camDevice = arg0;
				
				MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAMERA_OPENED);

				//dumpCameraCharacteristics();
			}
		}

		// Note: there other onCaptureXxxx methods in this listener which we do not implement
		@TargetApi(19)
		public class captureListener extends CameraDevice.CaptureListener
		{
			@Override
			public void onCaptureCompleted(CameraDevice camera, CaptureRequest request, CaptureResult result)
			{
				PluginManager.getInstance().onCaptureCompleted(result);
				if(result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_FOCUSED_LOCKED && HALv3.autoFocusTriggered)
				{
					resetCaptureListener();
					CameraController.getInstance().onAutoFocus(true);
					HALv3.autoFocusTriggered = false;
					
				}
				else if(result.get(CaptureResult.CONTROL_AF_STATE) == CaptureResult.CONTROL_AF_STATE_NOT_FOCUSED_LOCKED && HALv3.autoFocusTriggered)
				{
					resetCaptureListener();
					CameraController.getInstance().onAutoFocus(false);
					HALv3.autoFocusTriggered = false;
				}			
				
//				if(result.get(CaptureResult.REQUEST_ID) == iCaptureID)
//				{
//					//Log.e(TAG, "Image metadata received. Capture timestamp = " + result.get(CaptureResult.SENSOR_TIMESTAMP));
//					iPreviewFrameID = result.get(CaptureResult.SENSOR_TIMESTAMP);
//				}
				
				// Note: result arriving here is just image metadata, not the image itself
				// good place to extract sensor gain and other parameters

				// Note: not sure which units are used for exposure time (ms?)
//						currentExposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
//						currentSensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY);
				
				//dumpCaptureResult(result);
			}
			
			private void resetCaptureListener()
			{
				if(HALv3.getInstance().previewRequestBuilder != null && HALv3.getInstance().camDevice != null)
				{
					int focusMode = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(MainScreen.getCameraMirrored() ? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_AUTO);
					HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, focusMode);
					HALv3.getInstance().previewRequestBuilder.set(CaptureRequest.CONTROL_AF_TRIGGER, CameraCharacteristics.CONTROL_AF_TRIGGER_CANCEL);
					try 
					{
						//HALv3.getInstance().camDevice.stopRepeating();
						CameraController.iCaptureID = HALv3.getInstance().camDevice.capture(HALv3.getInstance().previewRequestBuilder.build(), null, null);
					}
					catch (CameraAccessException e)
					{
						e.printStackTrace();
					}
				}
			}
		}
		
		@TargetApi(19)
		public class imageAvailableListener implements ImageReader.OnImageAvailableListener
		{
			@Override
			public void onImageAvailable(ImageReader ir)
			{
				// Contrary to what is written in Aptina presentation acquireLatestImage is not working as described
				// Google: Also, not working as described in android docs (should work the same as acquireNextImage in our case, but it is not)
				//Image im = ir.acquireLatestImage();
				
				Image im = ir.acquireNextImage();
				//if(iPreviewFrameID == im.getTimestamp())
				if(ir.getSurface() == CameraController.mPreviewSurface)
					PluginManager.getInstance().onPreviewAvailable(im);
				else			
					PluginManager.getInstance().onImageAvailable(im);

				// Image should be closed after we are done with it
				im.close();
			}
		}
			// ^^ HALv3 code -------------------------------------------------------------- camera-related listeners
}
