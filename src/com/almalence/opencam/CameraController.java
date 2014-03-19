package com.almalence.opencam;

import java.util.List;

import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.media.ImageReader;
import android.util.Log;

public class CameraController implements Camera.PictureCallback, Camera.AutoFocusCallback, Camera.ErrorCallback, Camera.PreviewCallback, Camera.ShutterCallback
{
	private final String TAG = "CameraController";
	
	private static CameraController cameraController;
	
	//Old camera interface
	public static Camera camera = null;
	public static Camera.Parameters cameraParameters = null;
	
	//HALv3 camera's objects
	public CameraManager manager = null;
	public static CameraCharacteristics camCharacter=null;
	public cameraAvailableListener availListener = null;
	public static CameraDevice camDevice = null;
	public CaptureRequest.Builder previewRequestBuilder = null;
	public String[] cameraIdList={""};
	
	public static boolean cameraConfigured = false;
	
	
	// Flags to know which camera feature supported at current device
	public boolean mEVSupported = false;
	public boolean mSceneModeSupported = false;
	public boolean mWBSupported = false;
	public boolean mFocusModeSupported = false;
	public boolean mFlashModeSupported = false;
	public boolean mISOSupported = false;
	public boolean mCameraChangeSupported = false;
	
	public boolean mVideoStabilizationSupported = false;

	public static byte[] supportedSceneModes;
	public static byte[] supportedWBModes;
	public static byte[] supportedFocusModes;
	public static byte[] supportedFlashModes;
	public static byte[] supportedISOModes;
	
	
	public static int CameraIndex = 0;
	public static boolean CameraMirrored = false;
	
	//Image size index for capturing
	public static int CapIdx;
	
	public static final int MIN_MPIX_SUPPORTED = 1280 * 960;

	//Lists of resolutions, their indexes and names (for capturing and preview)
	public static List<Long> ResolutionsMPixList;
	public static List<String> ResolutionsIdxesList;
	public static List<String> ResolutionsNamesList;

	public static List<Long> ResolutionsMPixListIC;
	public static List<String> ResolutionsIdxesListIC;
	public static List<String> ResolutionsNamesListIC;

	public static List<Long> ResolutionsMPixListVF;
	public static List<String> ResolutionsIdxesListVF;
	public static List<String> ResolutionsNamesListVF;

	//States of focus and capture
	public static final int FOCUS_STATE_IDLE = 0;
	public static final int FOCUS_STATE_FOCUSED = 1;
	public static final int FOCUS_STATE_FAIL = 3;
	public static final int FOCUS_STATE_FOCUSING = 4;

	public static final int CAPTURE_STATE_IDLE = 0;
	public static final int CAPTURE_STATE_CAPTURING = 1;

	public static int mFocusState = FOCUS_STATE_IDLE;
	public static int mCaptureState = CAPTURE_STATE_IDLE;
	
	
	//Possible names of iso in CameraParameters variable
	public final static String isoParam = "iso";
	public final static String isoParam2 = "iso-speed";
	
	
	//Singleton access function
	public static CameraController getInstance()
	{
		if (cameraController == null)
		{
			cameraController = new CameraController();
		}
		return cameraController;
	}

	
	private CameraController()
	{
		
	}
	
	
	
	public void onCreate()
	{
		if(MainScreen.isHALv3)
		{
			// HALv3 code ---------------------------------------------------------
			// get manager for camera devices
			manager = (CameraManager)MainScreen.mainContext.getSystemService("camera"); // = Context.CAMERA_SERVICE;
			
			// get list of camera id's (usually it will contain just {"0", "1"}
			try {
				cameraIdList = manager.getCameraIdList();
			} catch (CameraAccessException e) {
				Log.d("MainScreen", "getCameraIdList failed");
				e.printStackTrace();
			}
		}
	}
	
	public void onStart()
	{
		
	}
	
	public void onResume()
	{
		
	}
	
	public void onPause()
	{
		
	}
	
	public void onStop()
	{
		
	}
	
	public void onDestroy()
	{
		
	}	
	
	

	@Override
	public void onShutter()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPreviewFrame(byte[] arg0, Camera arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onError(int arg0, Camera arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onAutoFocus(boolean arg0, Camera arg1)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPictureTaken(byte[] arg0, Camera arg1)
	{
		// TODO Auto-generated method stub
		
	}
	
	
	
	
	// HALv3 ------------------------------------------------ camera-related listeners

	// Note: never received onCameraAvailable notifications, only onCameraUnavailable
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

			camDevice = arg0;
			
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAMERA_OPENED);

			//dumpCameraCharacteristics();
		}
	}

	// Note: there other onCaptureXxxx methods in this listener which we do not implement
	public class captureListener extends CameraDevice.CaptureListener
	{
		@Override
		public void onCaptureCompleted(CameraDevice camera, CaptureRequest request, CaptureResult result)
		{
			Log.d(TAG, "CameraDevice.CaptureListener.onCaptureCompleted");
			
			// Note: result arriving here is just image metadata, not the image itself
			// good place to extract sensor gain and other parameters

			// Note: not sure which units are used for exposure time (ms?)
//					currentExposure = result.get(CaptureResult.SENSOR_EXPOSURE_TIME);
//					currentSensitivity = result.get(CaptureResult.SENSOR_SENSITIVITY);
			
			//dumpCaptureResult(result);
		}
	}
	
	public class imageAvailableListener implements ImageReader.OnImageAvailableListener
	{
		@Override
		public void onImageAvailable(ImageReader ir)
		{
			Log.e(TAG, "ImageReader.OnImageAvailableListener.onImageAvailable");
			
			// Contrary to what is written in Aptina presentation acquireLatestImage is not working as described
			// Google: Also, not working as described in android docs (should work the same as acquireNextImage in our case, but it is not)
			//Image im = ir.acquireLatestImage();
			Image im = ir.acquireNextImage();
			
			PluginManager.getInstance().onImageAvailable(im);

			// Image should be closed after we are done with it
			im.close();
		}
	}
		// ^^ HALv3 code -------------------------------------------------------------- camera-related listeners
}
