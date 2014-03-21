package com.almalence.opencam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

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
	
	
	// Android camera parameters constants
	private final static String sceneAuto = MainScreen.thiz.getResources()
			.getString(R.string.sceneAutoSystem);
	private final static String sceneAction = MainScreen.thiz.getResources()
			.getString(R.string.sceneActionSystem);
	private final static String scenePortrait = MainScreen.thiz.getResources()
			.getString(R.string.scenePortraitSystem);
	private final static String sceneLandscape = MainScreen.thiz.getResources()
			.getString(R.string.sceneLandscapeSystem);
	private final static String sceneNight = MainScreen.thiz.getResources()
			.getString(R.string.sceneNightSystem);
	private final static String sceneNightPortrait = MainScreen.thiz
			.getResources().getString(R.string.sceneNightPortraitSystem);
	private final static String sceneTheatre = MainScreen.thiz.getResources()
			.getString(R.string.sceneTheatreSystem);
	private final static String sceneBeach = MainScreen.thiz.getResources()
			.getString(R.string.sceneBeachSystem);
	private final static String sceneSnow = MainScreen.thiz.getResources()
			.getString(R.string.sceneSnowSystem);
	private final static String sceneSunset = MainScreen.thiz.getResources()
			.getString(R.string.sceneSunsetSystem);
	private final static String sceneSteadyPhoto = MainScreen.thiz
			.getResources().getString(R.string.sceneSteadyPhotoSystem);
	private final static String sceneFireworks = MainScreen.thiz.getResources()
			.getString(R.string.sceneFireworksSystem);
	private final static String sceneSports = MainScreen.thiz.getResources()
			.getString(R.string.sceneSportsSystem);
	private final static String sceneParty = MainScreen.thiz.getResources()
			.getString(R.string.scenePartySystem);
	private final static String sceneCandlelight = MainScreen.thiz
			.getResources().getString(R.string.sceneCandlelightSystem);
	private final static String sceneBarcode = MainScreen.thiz.getResources()
			.getString(R.string.sceneBarcodeSystem);
	private final static String sceneHDR = MainScreen.thiz.getResources()
			.getString(R.string.sceneHDRSystem);
	private final static String sceneAR = MainScreen.thiz.getResources()
			.getString(R.string.sceneARSystem);

	private final static String wbAuto = MainScreen.thiz.getResources()
			.getString(R.string.wbAutoSystem);
	private final static String wbIncandescent = MainScreen.thiz.getResources()
			.getString(R.string.wbIncandescentSystem);
	private final static String wbFluorescent = MainScreen.thiz.getResources()
			.getString(R.string.wbFluorescentSystem);
	private final static String wbWarmFluorescent = MainScreen.thiz
			.getResources().getString(R.string.wbWarmFluorescentSystem);
	private final static String wbDaylight = MainScreen.thiz.getResources()
			.getString(R.string.wbDaylightSystem);
	private final static String wbCloudyDaylight = MainScreen.thiz
			.getResources().getString(R.string.wbCloudyDaylightSystem);
	private final static String wbTwilight = MainScreen.thiz.getResources()
			.getString(R.string.wbTwilightSystem);
	private final static String wbShade = MainScreen.thiz.getResources()
			.getString(R.string.wbShadeSystem);

	private final static String focusAuto = MainScreen.thiz.getResources()
			.getString(R.string.focusAutoSystem);
	private final static String focusInfinity = MainScreen.thiz.getResources()
			.getString(R.string.focusInfinitySystem);
	private final static String focusNormal = MainScreen.thiz.getResources()
			.getString(R.string.focusNormalSystem);
	private final static String focusMacro = MainScreen.thiz.getResources()
			.getString(R.string.focusMacroSystem);
	private final static String focusFixed = MainScreen.thiz.getResources()
			.getString(R.string.focusFixedSystem);
	private final static String focusEdof = MainScreen.thiz.getResources()
			.getString(R.string.focusEdofSystem);
	private final static String focusContinuousVideo = MainScreen.thiz
			.getResources().getString(R.string.focusContinuousVideoSystem);
	private final static String focusContinuousPicture = MainScreen.thiz
			.getResources().getString(R.string.focusContinuousPictureSystem);
	private final static String focusAfLock = MainScreen.thiz
			.getResources().getString(R.string.focusAfLockSystem);


	private final static String flashAuto = MainScreen.thiz.getResources()
			.getString(R.string.flashAutoSystem);
	private final static String flashOn = MainScreen.thiz.getResources()
			.getString(R.string.flashOnSystem);
	private final static String flashOff = MainScreen.thiz.getResources()
			.getString(R.string.flashOffSystem);
	private final static String flashRedEye = MainScreen.thiz.getResources()
			.getString(R.string.flashRedEyeSystem);
	private final static String flashTorch = MainScreen.thiz.getResources()
			.getString(R.string.flashTorchSystem);
	
	
	// List of localized names for camera parameters values	
	public final static Map<Integer, String> mode_scene = new Hashtable<Integer, String>() {
		{
			put(CameraCharacteristics.CONTROL_MODE_AUTO, sceneAuto);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_ACTION, sceneAction);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_PORTRAIT, scenePortrait);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_LANDSCAPE,	sceneLandscape);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT,	sceneNight);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT_PORTRAIT, sceneNightPortrait);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_THEATRE, sceneTheatre);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_BEACH,	sceneBeach);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_SNOW, sceneSnow);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_SUNSET, sceneSunset);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_STEADYPHOTO, sceneSteadyPhoto);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_FIREWORKS, sceneFireworks);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_SPORTS, sceneSports);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_PARTY, sceneParty);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_CANDLELIGHT, sceneCandlelight);
			put(CameraCharacteristics.CONTROL_SCENE_MODE_BARCODE, sceneBarcode);
		}
	};
	
	public final static Map<String, Integer> key_scene = new Hashtable<String, Integer>() {
		{
			put(sceneAuto, CameraCharacteristics.CONTROL_MODE_AUTO);
			put(sceneAction, CameraCharacteristics.CONTROL_SCENE_MODE_ACTION);
			put(scenePortrait, CameraCharacteristics.CONTROL_SCENE_MODE_PORTRAIT);
			put(sceneLandscape, CameraCharacteristics.CONTROL_SCENE_MODE_LANDSCAPE);
			put(sceneNight, CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT);
			put(sceneNightPortrait, CameraCharacteristics.CONTROL_SCENE_MODE_NIGHT_PORTRAIT);
			put(sceneTheatre, CameraCharacteristics.CONTROL_SCENE_MODE_THEATRE);
			put(sceneBeach, CameraCharacteristics.CONTROL_SCENE_MODE_BEACH);
			put(sceneSnow, CameraCharacteristics.CONTROL_SCENE_MODE_SNOW);
			put(sceneSunset, CameraCharacteristics.CONTROL_SCENE_MODE_SUNSET);
			put(sceneSteadyPhoto, CameraCharacteristics.CONTROL_SCENE_MODE_STEADYPHOTO);
			put(sceneFireworks, CameraCharacteristics.CONTROL_SCENE_MODE_FIREWORKS);
			put(sceneSports, CameraCharacteristics.CONTROL_SCENE_MODE_SPORTS);
			put(sceneParty, CameraCharacteristics.CONTROL_SCENE_MODE_PARTY);
			put(sceneCandlelight, CameraCharacteristics.CONTROL_SCENE_MODE_CANDLELIGHT);
			put(sceneBarcode, CameraCharacteristics.CONTROL_SCENE_MODE_BARCODE);
		}
	};
	
	

	public final static Map<Integer, String> mode_wb = new Hashtable<Integer, String>() {
		{
			put(CameraCharacteristics.CONTROL_AWB_MODE_AUTO, wbAuto);
			put(CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT, wbIncandescent);
			put(CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT, wbFluorescent);
			put(CameraCharacteristics.CONTROL_AWB_MODE_WARM_FLUORESCENT, wbWarmFluorescent);
			put(CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT, wbDaylight);
			put(CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT, wbCloudyDaylight);
			put(CameraCharacteristics.CONTROL_AWB_MODE_TWILIGHT, wbTwilight);
			put(CameraCharacteristics.CONTROL_AWB_MODE_SHADE, wbShade);
		}
	};
	
	public final static Map<String, Integer> key_wb = new Hashtable<String, Integer>() {
		{
			put(wbAuto, CameraCharacteristics.CONTROL_AWB_MODE_AUTO);
			put(wbIncandescent, CameraCharacteristics.CONTROL_AWB_MODE_INCANDESCENT);
			put(wbFluorescent, CameraCharacteristics.CONTROL_AWB_MODE_FLUORESCENT);
			put(wbWarmFluorescent, CameraCharacteristics.CONTROL_AWB_MODE_WARM_FLUORESCENT);
			put(wbDaylight, CameraCharacteristics.CONTROL_AWB_MODE_DAYLIGHT);
			put(wbCloudyDaylight, CameraCharacteristics.CONTROL_AWB_MODE_CLOUDY_DAYLIGHT);
			put(wbTwilight, CameraCharacteristics.CONTROL_AWB_MODE_TWILIGHT);
			put(wbShade, CameraCharacteristics.CONTROL_AWB_MODE_SHADE);
		}
	};


	public final static int CONTROL_AF_MODE_INFINITY = 6;
	public final static int CONTROL_AF_MODE_NORMAL = 7;
	public final static int CONTROL_AF_MODE_FIXED = 8;
	public final static Map<Integer, String> mode_focus = new Hashtable<Integer, String>() {
		{
			put(CameraCharacteristics.CONTROL_AF_MODE_AUTO, focusAuto);
			put(CameraController.CONTROL_AF_MODE_INFINITY, focusInfinity);
			put(CameraController.CONTROL_AF_MODE_NORMAL, focusNormal);
			put(CameraCharacteristics.CONTROL_AF_MODE_MACRO, focusMacro);
			put(CameraController.CONTROL_AF_MODE_FIXED, focusFixed);
			put(CameraCharacteristics.CONTROL_AF_MODE_EDOF, focusEdof);
			put(CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO, focusContinuousVideo);
			put(CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE, focusContinuousPicture);
		}
	};
	
	public final static Map<String, Integer> key_focus = new Hashtable<String, Integer>() {
		{
			put(focusAuto, CameraCharacteristics.CONTROL_AF_MODE_AUTO);
			put(focusInfinity, CameraController.CONTROL_AF_MODE_INFINITY);
			put(focusNormal, CameraController.CONTROL_AF_MODE_NORMAL);
			put(focusMacro, CameraCharacteristics.CONTROL_AF_MODE_MACRO);
			put(focusFixed, CameraController.CONTROL_AF_MODE_FIXED);
			put(focusEdof, CameraCharacteristics.CONTROL_AF_MODE_EDOF);
			put(focusContinuousVideo, CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_VIDEO);
			put(focusContinuousPicture, CameraCharacteristics.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
		}
	};

	
	public final static int FLASH_MODE_AUTO = 3;
	public final static int FLASH_MODE_REDEYE = 4;
	public final static Map<Integer, String> mode_flash = new Hashtable<Integer, String>() {
		{
			put(CameraCharacteristics.FLASH_MODE_OFF, flashOff);
			put(CameraController.FLASH_MODE_AUTO, flashAuto);
			put(CameraCharacteristics.FLASH_MODE_SINGLE, flashOn);
			put(CameraController.FLASH_MODE_REDEYE, flashRedEye);
			put(CameraCharacteristics.FLASH_MODE_TORCH, flashTorch);
		}
	};
	
	public final static Map<String, Integer> key_flash = new Hashtable<String, Integer>() {
		{
			put(flashOff, CameraCharacteristics.FLASH_MODE_OFF);
			put(flashAuto, CameraController.FLASH_MODE_AUTO);
			put(flashOn, CameraCharacteristics.FLASH_MODE_SINGLE);
			put(flashRedEye, CameraController.FLASH_MODE_REDEYE);
			put(flashTorch, CameraCharacteristics.FLASH_MODE_TORCH);
		}
	};
		
	
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
	public static List<Camera.Size> ResolutionsSizeList;
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
		
		if(MainScreen.isHALv3)
			PopulateCameraDimensionsHALv3();
		else
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
	
	
	public static void PopulateCameraDimensionsHALv3(){
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<Camera.Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();
		
		int MinMPIX = CameraController.MIN_MPIX_SUPPORTED;
		CameraCharacteristics params = MainScreen.thiz.getCameraParameters2();
		Size[] cs = params.get(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES);

		CharSequence[] RatioStrings = { " ", "4:3", "3:2", "16:9", "1:1" };

		int iHighestIndex = 0;		
		Size sHighest = cs[iHighestIndex];
		
		int ii = 0;
		for(Size s : cs)
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
			CameraController.ResolutionsSizeList.add(loc, camera.new Size(s.getWidth(), s.getHeight()));
			
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
			CameraController.ResolutionsSizeList.add(0, camera.new Size(s.getWidth(), s.getHeight()));
		}

		return;
	}
	
	
	public static void PopulateCameraDimensions()
	{
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<Camera.Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();	
		
		
		int MinMPIX = CameraController.MIN_MPIX_SUPPORTED;
		Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
		List<Camera.Size> cs = cp.getSupportedPictureSizes();

		CharSequence[] RatioStrings = { " ", "4:3", "3:2", "16:9", "1:1" };

		int iHighestIndex = 0;
		Camera.Size sHighest = cs.get(0);
		
		
		for (int ii = 0; ii < cs.size(); ++ii)
		{
			Camera.Size s = cs.get(ii);

			if ((long) s.width * s.height > (long) sHighest.width
					* sHighest.height) {
				sHighest = s;
				iHighestIndex = ii;
			}

			if ((long) s.width * s.height < MinMPIX)
				continue;

			Long lmpix = (long) s.width * s.height;
			float mpix = (float) lmpix / 1000000.f;
			float ratio = (float) s.width / s.height;

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
			CameraController.ResolutionsSizeList.add(loc, s);
			
			ii++;
		}

		if (CameraController.ResolutionsNamesList.size() == 0) {
			Camera.Size s = cs.get(iHighestIndex);

			Long lmpix = (long) s.width * s.height;
			float mpix = (float) lmpix / 1000000.f;
			float ratio = (float) s.width / s.height;

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
			CameraController.ResolutionsSizeList.add(0, s);
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
