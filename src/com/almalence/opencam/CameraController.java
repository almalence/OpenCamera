package com.almalence.opencam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.Size;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.util.Log;
import android.view.SurfaceHolder;
import android.widget.Toast;

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
	
	
	public void setupCamera(SurfaceHolder holder)
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera == null) {
				try {
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
						openCameraFrontOrRear();
					} else {
						CameraController.camera = Camera.open();
					}
				} catch (RuntimeException e) {
					CameraController.camera = null;
				}
	
				if (CameraController.camera == null) {
					Toast.makeText(MainScreen.thiz, "Unable to start camera", Toast.LENGTH_LONG).show();
					return;
				}
			}
			
			CameraController.cameraParameters = CameraController.camera.getParameters(); //Initialize of camera parameters variable
			
			if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				cameraController.mVideoStabilizationSupported = MainScreen.thiz.isVideoStabilizationSupported();
		}
		else
		{
			// HALv3 open camera -----------------------------------------------------------------
			try
			{
				cameraController.manager.openCamera (cameraController.cameraIdList[CameraController.CameraIndex], cameraController.new openListener(), null);
			}
			catch (CameraAccessException e)
			{
				Log.d("MainScreen", "manager.openCamera failed");
				e.printStackTrace();
			}
			
			// find suitable image sizes for preview and capture
			try	{
				CameraController.camCharacter = cameraController.manager.getCameraCharacteristics(cameraController.cameraIdList[CameraController.CameraIndex]);
			} catch (CameraAccessException e) {
				Log.d("MainScreen", "getCameraCharacteristics failed");
				e.printStackTrace();
			}
			
			if (CameraController.camCharacter.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_FRONT)
				CameraController.CameraMirrored = true;
			else
				CameraController.CameraMirrored = false;
			
			// Add an Availability Listener as Cameras become available or unavailable
			cameraController.availListener = cameraController.new cameraAvailableListener();
			cameraController.manager.addAvailabilityListener(cameraController.availListener, null);
			
			cameraController.mVideoStabilizationSupported = CameraController.camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_VIDEO_STABILIZATION_MODES) == null? false : true;
			
			// check that full hw level is supported
			if (CameraController.camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) != CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) 
				MainScreen.H.sendEmptyMessage(PluginManager.MSG_NOT_LEVEL_FULL);		
			// ^^ HALv3 open camera -----------------------------------------------------------------
		}
		


		PluginManager.getInstance().SelectDefaults();

		if(!MainScreen.isHALv3)
		{
			// screen rotation
			try {
				CameraController.camera.setDisplayOrientation(90);
			} catch (RuntimeException e) {
				e.printStackTrace();
			}
	
			try {
				CameraController.camera.setPreviewDisplay(holder);
			} catch (IOException e) {
				e.printStackTrace();
			}
	
			if (CameraController.camera == null)
				return;
			Camera.Parameters cp = CameraController.cameraParameters;
		}
		
		PopulateCameraDimensions();
		CameraController.ResolutionsMPixListIC = CameraController.ResolutionsMPixList;
		CameraController.ResolutionsIdxesListIC = CameraController.ResolutionsIdxesList;
		CameraController.ResolutionsNamesListIC = CameraController.ResolutionsNamesList;

		PluginManager.getInstance().SelectImageDimension(); // updates SX, SY
															// values

		
		if(MainScreen.isHALv3)
		{
			//surfaceHolder.setFixedSize(MainScreen.imageWidth, MainScreen.imageHeight);
			MainScreen.thiz.surfaceHolder.setFixedSize(1280, 720);
			MainScreen.previewWidth = 1280;
			MainScreen.previewHeight = 720;
			
			// HALv3 code -------------------------------------------------------------------
			MainScreen.mImageReaderYUV = ImageReader.newInstance(MainScreen.imageWidth, MainScreen.imageHeight, ImageFormat.YUV_420_888, 2);
			MainScreen.mImageReaderYUV.setOnImageAvailableListener(cameraController.new imageAvailableListener(), null);
			
			MainScreen.mImageReaderJPEG = ImageReader.newInstance(MainScreen.imageWidth, MainScreen.imageHeight, ImageFormat.JPEG, 2);
			MainScreen.mImageReaderJPEG.setOnImageAvailableListener(cameraController.new imageAvailableListener(), null);
		}
		
		MainScreen.thiz.surfaceHolder.addCallback(MainScreen.thiz);
		
		if(!MainScreen.isHALv3)
			MainScreen.thiz.configureCamera();
		
//		// ----- Select preview dimensions with ratio correspondent to full-size
//		// image
////		PluginManager.getInstance().SetCameraPreviewSize(cameraParameters);
////
////		guiManager.setupViewfinderPreviewSize(cameraParameters);
//
////		Size previewSize = cameraParameters.getPreviewSize();
//
//		if (PluginManager.getInstance().isGLSurfaceNeeded()) {
//			if (glView == null) {
//				glView = new GLLayer(MainScreen.mainContext);// (GLLayer)findViewById(R.id.SurfaceView02);
//				glView.setLayoutParams(new LayoutParams(
//						LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
//				((RelativeLayout) findViewById(R.id.mainLayout2)).addView(
//						glView, 1);
//				glView.setZOrderMediaOverlay(true);
//				glView.setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
//			}
//		} else {
//			((RelativeLayout) findViewById(R.id.mainLayout2))
//					.removeView(glView);
//			glView = null;
//		}
//
//		RelativeLayout.LayoutParams lp = (RelativeLayout.LayoutParams) preview
//				.getLayoutParams();
//		if (glView != null) {
//			glView.setVisibility(View.VISIBLE);
//			glView.setLayoutParams(lp);
//		} else {
//			if (glView != null)
//				glView.setVisibility(View.GONE);
//		}
//
////		pviewBuffer = new byte[previewSize.width
////				* previewSize.height
////				* ImageFormat.getBitsPerPixel(cameraParameters
////						.getPreviewFormat()) / 8];
//
////		camera.setErrorCallback(MainScreen.thiz);
//
//		supportedSceneModes = getSupportedSceneModes();
//		supportedWBModes = getSupportedWhiteBalance();
//		supportedFocusModes = getSupportedFocusModes();
//		supportedFlashModes = getSupportedFlashModes();
//		supportedISOModes = getSupportedISO();
//
//		PluginManager.getInstance().SetCameraPictureSize();
//		PluginManager.getInstance().SetupCameraParameters();
//		//cp = cameraParameters;
//
////		try {
////			//Log.i("CameraTest", Build.MODEL);
////			if (Build.MODEL.contains("Nexus 5"))
////			{
////				cameraParameters.setPreviewFpsRange(7000, 30000);
////				setCameraParameters(cameraParameters);
////			}
////			
////			//Log.i("CameraTest", "fps ranges "+range.size()+" " + range.get(0)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " " + range.get(0)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
////			//cameraParameters.setPreviewFpsRange(range.get(0)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], range.get(0)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
////			//cameraParameters.setPreviewFpsRange(7000, 30000);
////			// an obsolete but much more reliable way of setting preview to a reasonable fps range
////			// Nexus 5 is giving preview which is too dark without this
////			//cameraParameters.setPreviewFrameRate(30);
////		
////			
////		} catch (RuntimeException e) {
////			Log.e("CameraTest", "MainScreen.setupCamera unable setParameters "
////					+ e.getMessage());
////		}
////
////		previewWidth = cameraParameters.getPreviewSize().width;
////		previewHeight = cameraParameters.getPreviewSize().height;
//
////		Util.initialize(mainContext);
////		Util.initializeMeteringMatrix();
////		
////		prepareMeteringAreas();
//
//		guiManager.onCameraCreate();
//		PluginManager.getInstance().onCameraParametersSetup();
//		guiManager.onPluginsInitialized();
//
//		// ----- Start preview and setup frame buffer if needed
//
//		// ToDo: call camera release sequence from onPause somewhere ???
//		new CountDownTimer(10, 10) {
//			@Override
//			public void onFinish() {
////				try // exceptions sometimes happen here when resuming after
////					// processing
////				{
////					camera.startPreview();
////				} catch (RuntimeException e) {
////					Toast.makeText(MainScreen.thiz, "Unable to start camera", Toast.LENGTH_LONG).show();
////					return;
////				}
////
////				camera.setPreviewCallbackWithBuffer(MainScreen.thiz);
////				camera.addCallbackBuffer(pviewBuffer);
//
//				PluginManager.getInstance().onCameraSetup();
//				guiManager.onCameraSetup();
//				MainScreen.mApplicationStarted = true;
//			}
//
//			@Override
//			public void onTick(long millisUntilFinished) {
//			}
//		}.start();
	}
	
	
	protected void openCameraFrontOrRear()
	{
		if (Camera.getNumberOfCameras() > 0)
		{
			CameraController.camera = Camera.open(CameraController.CameraIndex);
		}
	
		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		Camera.getCameraInfo(CameraController.CameraIndex, cameraInfo);
	
		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
			CameraController.CameraMirrored = true;
		else
			CameraController.CameraMirrored = false;
	}
	
	
	public static void PopulateCameraDimensions() {
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();

		////For debug file
//		File saveDir = PluginManager.getInstance().GetSaveDir();
//		File file = new File(
//        		saveDir, 
//        		"!!!ABC_DEBUG_COMMON.txt");
//		if (file.exists())
//		    file.delete();
//		try {
//			String data = "";
//			data = cp.flatten();
//			FileOutputStream out;
//			out = new FileOutputStream(file);
//			OutputStreamWriter outputStreamWriter = new OutputStreamWriter(out);
//			outputStreamWriter.write(data);
//			outputStreamWriter.write(data);
//			outputStreamWriter.flush();
//			outputStreamWriter.close();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
		
		
		//List<Camera.Size> cs;
		//int MinMPIX = MIN_MPIX_SUPPORTED;
		//cs = cp.getSupportedPictureSizes();
		
		int MinMPIX = CameraController.MIN_MPIX_SUPPORTED;
		CameraCharacteristics params = MainScreen.thiz.getCameraParameters2();
    	Size[] cs = params.get(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES);

		CharSequence[] RatioStrings = { " ", "4:3", "3:2", "16:9", "1:1" };

		int iHighestIndex = 0;
		Size sHighest = cs[iHighestIndex];

//		/////////////////////////		
//		try {
//			File saveDir2 = PluginManager.getInstance().GetSaveDir();
//			File file2 = new File(
//	        		saveDir2, 
//	        		"!!!ABC_DEBUG.txt");
//			if (file2.exists())
//			    file2.delete();
//			FileOutputStream out2;
//			out2 = new FileOutputStream(file2);
//			OutputStreamWriter outputStreamWriter2 = new OutputStreamWriter(out2);
//
//			for (int ii = 0; ii < cs.size(); ++ii) {
//				Size s = cs.get(ii);
//					String data = "cs.size() = "+cs.size()+ " " +"size "+ ii + " = " + s.width + "x" +s.height;
//					outputStreamWriter2.write(data);
//					outputStreamWriter2.flush();
//			}
//			outputStreamWriter2.close();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		/////////////////////////
		
		int ii = 0;
		for(Size s : cs)
		{
//		for (int ii = 0; ii < cs.size(); ++ii) {
//			Size s = cs.get(ii);

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
			
			ii++;
		}

		if (CameraController.ResolutionsNamesList.size() == 0) {
			Size s = cs[iHighestIndex];

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
		}

		return;
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
