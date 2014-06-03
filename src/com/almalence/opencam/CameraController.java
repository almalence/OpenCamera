package com.almalence.opencam;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import com.almalence.opencam.R;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.camera2.CameraManager;

import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

public class CameraController implements Camera.PictureCallback, Camera.AutoFocusCallback, Camera.ErrorCallback, Camera.PreviewCallback, Camera.ShutterCallback
{
	private final String TAG = "CameraController";
	
	public static final int YUV = 1;
	public static final int JPEG = 0;
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
	
	private final static String isoAuto = MainScreen.thiz.getResources()
			.getString(R.string.isoAutoSystem);
	private final static String iso50 = MainScreen.thiz.getResources()
			.getString(R.string.iso50System);
	private final static String iso100 = MainScreen.thiz.getResources()
			.getString(R.string.iso100System);
	private final static String iso200 = MainScreen.thiz.getResources()
			.getString(R.string.iso200System);
	private final static String iso400 = MainScreen.thiz.getResources()
			.getString(R.string.iso400System);
	private final static String iso800 = MainScreen.thiz.getResources()
			.getString(R.string.iso800System);
	private final static String iso1600 = MainScreen.thiz.getResources()
			.getString(R.string.iso1600System);
	private final static String iso3200 = MainScreen.thiz.getResources()
			.getString(R.string.iso3200System);
	
	private final static String isoAuto_2 = MainScreen.thiz.getResources()
			.getString(R.string.isoAutoDefaultSystem);
	private final static String iso50_2 = MainScreen.thiz.getResources()
			.getString(R.string.iso50DefaultSystem);
	private final static String iso100_2 = MainScreen.thiz.getResources()
			.getString(R.string.iso100DefaultSystem);
	private final static String iso200_2 = MainScreen.thiz.getResources()
			.getString(R.string.iso200DefaultSystem);
	private final static String iso400_2 = MainScreen.thiz.getResources()
			.getString(R.string.iso400DefaultSystem);
	private final static String iso800_2 = MainScreen.thiz.getResources()
			.getString(R.string.iso800DefaultSystem);
	private final static String iso1600_2 = MainScreen.thiz.getResources()
			.getString(R.string.iso1600DefaultSystem);
	private final static String iso3200_2 = MainScreen.thiz.getResources()
			.getString(R.string.iso3200DefaultSystem);
	
	private final static String meteringAuto = MainScreen.thiz.getResources()
			.getString(R.string.meteringAutoSystem);
	private final static String meteringMatrix = MainScreen.thiz.getResources()
			.getString(R.string.meteringMatrixSystem);
	private final static String meteringCenter = MainScreen.thiz.getResources()
			.getString(R.string.meteringCenterSystem);
	private final static String meteringSpot = MainScreen.thiz.getResources()
			.getString(R.string.meteringSpotSystem);
	
	
	// List of localized names for camera parameters values	
	public final static Map<Integer, String> mode_scene = new Hashtable<Integer, String>() {
		{
			put(CameraParameters.SCENE_MODE_AUTO, sceneAuto);
			put(CameraParameters.SCENE_MODE_ACTION, sceneAction);
			put(CameraParameters.SCENE_MODE_PORTRAIT, scenePortrait);
			put(CameraParameters.SCENE_MODE_LANDSCAPE,	sceneLandscape);
			put(CameraParameters.SCENE_MODE_NIGHT,	sceneNight);
			put(CameraParameters.SCENE_MODE_NIGHT_PORTRAIT, sceneNightPortrait);
			put(CameraParameters.SCENE_MODE_THEATRE, sceneTheatre);
			put(CameraParameters.SCENE_MODE_BEACH,	sceneBeach);
			put(CameraParameters.SCENE_MODE_SNOW, sceneSnow);
			put(CameraParameters.SCENE_MODE_SUNSET, sceneSunset);
			put(CameraParameters.SCENE_MODE_STEADYPHOTO, sceneSteadyPhoto);
			put(CameraParameters.SCENE_MODE_FIREWORKS, sceneFireworks);
			put(CameraParameters.SCENE_MODE_SPORTS, sceneSports);
			put(CameraParameters.SCENE_MODE_PARTY, sceneParty);
			put(CameraParameters.SCENE_MODE_CANDLELIGHT, sceneCandlelight);
			put(CameraParameters.SCENE_MODE_BARCODE, sceneBarcode);
		}
	};
	
	public final static Map<String, Integer> key_scene = new Hashtable<String, Integer>() {
		{
			put(sceneAuto, CameraParameters.SCENE_MODE_AUTO);
			put(sceneAction, CameraParameters.SCENE_MODE_ACTION);
			put(scenePortrait, CameraParameters.SCENE_MODE_PORTRAIT);
			put(sceneLandscape, CameraParameters.SCENE_MODE_LANDSCAPE);
			put(sceneNight, CameraParameters.SCENE_MODE_NIGHT);
			put(sceneNightPortrait, CameraParameters.SCENE_MODE_NIGHT_PORTRAIT);
			put(sceneTheatre, CameraParameters.SCENE_MODE_THEATRE);
			put(sceneBeach, CameraParameters.SCENE_MODE_BEACH);
			put(sceneSnow, CameraParameters.SCENE_MODE_SNOW);
			put(sceneSunset, CameraParameters.SCENE_MODE_SUNSET);
			put(sceneSteadyPhoto, CameraParameters.SCENE_MODE_STEADYPHOTO);
			put(sceneFireworks, CameraParameters.SCENE_MODE_FIREWORKS);
			put(sceneSports, CameraParameters.SCENE_MODE_SPORTS);
			put(sceneParty, CameraParameters.SCENE_MODE_PARTY);
			put(sceneCandlelight, CameraParameters.SCENE_MODE_CANDLELIGHT);
			put(sceneBarcode, CameraParameters.SCENE_MODE_BARCODE);
		}
	};
	
	

	public final static Map<Integer, String> mode_wb = new Hashtable<Integer, String>() {
		{
			put(CameraParameters.WB_MODE_AUTO, wbAuto);
			put(CameraParameters.WB_MODE_INCANDESCENT, wbIncandescent);
			put(CameraParameters.WB_MODE_FLUORESCENT, wbFluorescent);
			put(CameraParameters.WB_MODE_WARM_FLUORESCENT, wbWarmFluorescent);
			put(CameraParameters.WB_MODE_DAYLIGHT, wbDaylight);
			put(CameraParameters.WB_MODE_CLOUDY_DAYLIGHT, wbCloudyDaylight);
			put(CameraParameters.WB_MODE_TWILIGHT, wbTwilight);
			put(CameraParameters.WB_MODE_SHADE, wbShade);
		}
	};
	
	public final static Map<String, Integer> key_wb = new Hashtable<String, Integer>() {
		{
			put(wbAuto, CameraParameters.WB_MODE_AUTO);
			put(wbIncandescent, CameraParameters.WB_MODE_INCANDESCENT);
			put(wbFluorescent, CameraParameters.WB_MODE_FLUORESCENT);
			put(wbWarmFluorescent, CameraParameters.WB_MODE_WARM_FLUORESCENT);
			put(wbDaylight, CameraParameters.WB_MODE_DAYLIGHT);
			put(wbCloudyDaylight, CameraParameters.WB_MODE_CLOUDY_DAYLIGHT);
			put(wbTwilight, CameraParameters.WB_MODE_TWILIGHT);
			put(wbShade, CameraParameters.WB_MODE_SHADE);
		}
	};


//	public final static int CONTROL_AF_MODE_INFINITY = 6;
//	public final static int CONTROL_AF_MODE_NORMAL = 7;
//	public final static int CONTROL_AF_MODE_FIXED = 8;
	public final static Map<Integer, String> mode_focus = new Hashtable<Integer, String>() {
		{
			put(CameraParameters.AF_MODE_AUTO, focusAuto);
			put(CameraParameters.AF_MODE_INFINITY, focusInfinity);
			put(CameraParameters.AF_MODE_NORMAL, focusNormal);
			put(CameraParameters.AF_MODE_MACRO, focusMacro);
			put(CameraParameters.AF_MODE_FIXED, focusFixed);
			put(CameraParameters.AF_MODE_EDOF, focusEdof);
			put(CameraParameters.AF_MODE_CONTINUOUS_VIDEO, focusContinuousVideo);
			put(CameraParameters.AF_MODE_CONTINUOUS_PICTURE, focusContinuousPicture);
		}
	};
	
	public final static Map<String, Integer> key_focus = new Hashtable<String, Integer>() {
		{
			put(focusAuto, CameraParameters.AF_MODE_AUTO);
			put(focusInfinity, CameraParameters.AF_MODE_INFINITY);
			put(focusNormal, CameraParameters.AF_MODE_NORMAL);
			put(focusMacro, CameraParameters.AF_MODE_MACRO);
			put(focusFixed, CameraParameters.AF_MODE_FIXED);
			put(focusEdof, CameraParameters.AF_MODE_EDOF);
			put(focusContinuousVideo, CameraParameters.AF_MODE_CONTINUOUS_VIDEO);
			put(focusContinuousPicture, CameraParameters.AF_MODE_CONTINUOUS_PICTURE);
		}
	};

	
//	public final static int FLASH_MODE_AUTO = 3;
//	public final static int FLASH_MODE_REDEYE = 4;
	public final static Map<Integer, String> mode_flash = new Hashtable<Integer, String>() {
		{
			put(CameraParameters.FLASH_MODE_OFF, flashOff);
			put(CameraParameters.FLASH_MODE_AUTO, flashAuto);
			put(CameraParameters.FLASH_MODE_SINGLE, flashOn);
			put(CameraParameters.FLASH_MODE_REDEYE, flashRedEye);
			put(CameraParameters.FLASH_MODE_TORCH, flashTorch);
		}
	};
	
	public final static Map<String, Integer> key_flash = new Hashtable<String, Integer>() {
		{
			put(flashOff, CameraParameters.FLASH_MODE_OFF);
			put(flashAuto, CameraParameters.FLASH_MODE_AUTO);
			put(flashOn, CameraParameters.FLASH_MODE_SINGLE);
			put(flashRedEye, CameraParameters.FLASH_MODE_REDEYE);
			put(flashTorch, CameraParameters.FLASH_MODE_TORCH);
		}
	};
	
	
	public final static List<Integer> iso_values = new ArrayList<Integer>() {
		{			
			add(CameraParameters.ISO_AUTO);			
			add(CameraParameters.ISO_50);
			add(CameraParameters.ISO_100);
			add(CameraParameters.ISO_200);
			add(CameraParameters.ISO_400);
			add(CameraParameters.ISO_800);
			add(CameraParameters.ISO_1600);
			add(CameraParameters.ISO_3200);
		}
	};
	
	
	public final static List<String> iso_default = new ArrayList<String>() {
		{			
			add(isoAuto);			
			add(iso100);
			add(iso200);
			add(iso400);
			add(iso800);
			add(iso1600);
		}
	};
	
		
	public final static Map<String, String> iso_default_values = new Hashtable<String, String>() {
	{			
			put(isoAuto, MainScreen.thiz.getResources().getString(R.string.isoAutoDefaultSystem));			
			put(iso100, MainScreen.thiz.getResources().getString(R.string.iso100DefaultSystem));
			put(iso200, MainScreen.thiz.getResources().getString(R.string.iso200DefaultSystem));
			put(iso400, MainScreen.thiz.getResources().getString(R.string.iso400DefaultSystem));
			put(iso800, MainScreen.thiz.getResources().getString(R.string.iso800DefaultSystem));
			put(iso1600, MainScreen.thiz.getResources().getString(R.string.iso1600DefaultSystem));
		}
	};
	
	public final static Map<Integer, String> mode_iso = new Hashtable<Integer, String>() {
		{
			put(CameraParameters.ISO_AUTO, isoAuto);
			put(CameraParameters.ISO_50, iso50);
			put(CameraParameters.ISO_100, iso100);
			put(CameraParameters.ISO_200, iso200);
			put(CameraParameters.ISO_400, iso400);
			put(CameraParameters.ISO_800, iso800);
			put(CameraParameters.ISO_1600, iso1600);
			put(CameraParameters.ISO_3200, iso3200);
		}
	};
	
	public final static Map<Integer, String> mode_iso2 = new Hashtable<Integer, String>() {
		{
			put(CameraParameters.ISO_AUTO, isoAuto_2);
			put(CameraParameters.ISO_50, iso50_2);
			put(CameraParameters.ISO_100, iso100_2);
			put(CameraParameters.ISO_200, iso200_2);
			put(CameraParameters.ISO_400, iso400_2);
			put(CameraParameters.ISO_800, iso800_2);
			put(CameraParameters.ISO_1600, iso1600_2);
			put(CameraParameters.ISO_3200, iso3200_2);
		}
	};
	
	
	public final static Map<Integer, Integer> mode_iso_HALv3 = new Hashtable<Integer, Integer>() {
		{
			put(CameraParameters.ISO_AUTO, 1);
			put(CameraParameters.ISO_50, 50);
			put(CameraParameters.ISO_100, 100);
			put(CameraParameters.ISO_200, 200);
			put(CameraParameters.ISO_400, 400);
			put(CameraParameters.ISO_800, 800);
			put(CameraParameters.ISO_1600, 1600);
			put(CameraParameters.ISO_3200, 3200);
		}
	};
	
	public final static Map<String, Integer> key_iso = new Hashtable<String, Integer>() {
		{
			put(isoAuto, CameraParameters.ISO_AUTO);
			put(iso50, CameraParameters.ISO_AUTO);
			put(iso100, CameraParameters.ISO_AUTO);
			put(iso200, CameraParameters.ISO_AUTO);
			put(iso400, CameraParameters.ISO_AUTO);
			put(iso800, CameraParameters.ISO_AUTO);
			put(iso1600, CameraParameters.ISO_AUTO);
			put(iso3200, CameraParameters.ISO_AUTO);
		}
	};
	
	public final static Map<String, Integer> key_iso2 = new Hashtable<String, Integer>() {
		{
			put(isoAuto_2, CameraParameters.ISO_AUTO);
			put(iso50_2, CameraParameters.ISO_AUTO);
			put(iso100_2, CameraParameters.ISO_AUTO);
			put(iso200_2, CameraParameters.ISO_AUTO);
			put(iso400_2, CameraParameters.ISO_AUTO);
			put(iso800_2, CameraParameters.ISO_AUTO);
			put(iso1600_2, CameraParameters.ISO_AUTO);
			put(iso3200_2, CameraParameters.ISO_AUTO);
		}
	};
	
	
	private static CameraController cameraController = null;
	
	//Old camera interface
	private static Camera camera = null;
	private static Camera.Parameters cameraParameters = null;
	public byte[] pviewBuffer;
	
//	//HALv3 camera's objects
//	@TargetApi(19)
//	@SuppressLint("NewApi")
//	public static class HALv3
//	{
//		private static HALv3 instance = null;
//		
//		public static HALv3 getInstance()
//		{
//			if (instance == null)
//			{
//				instance = new HALv3();
//			}
//			return instance;
//		}
//		
//		public CameraManager manager = null;
//		public CameraCharacteristics camCharacter=null;
//		public cameraAvailableListener availListener = null;
//		public CameraDevice camDevice = null;
//		public CaptureRequest.Builder previewRequestBuilder = null;
//	}
	
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
	
	
	public static int maxRegionsSupported;
	
	
	public static int CameraIndex = 0;
	public static boolean CameraMirrored = false;
	
	//Image size index for capturing
	public static int CapIdx;
	
	public static final int MIN_MPIX_SUPPORTED = 1280 * 960;

	//Lists of resolutions, their indexes and names (for capturing and preview)
	public static List<Long> ResolutionsMPixList;
	public static List<CameraController.Size> ResolutionsSizeList;
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
	
	
	public static int iCaptureID = -1;
	public static Surface mPreviewSurface = null;
	
	
	private Object syncObject = new Object();
	
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
		if(MainScreen.isHALv3Supported)
			HALv3.onCreateHALv3();
	}
	
	
	
	public void onStart()
	{
		
	}
	
	public void onResume()
	{
		
	}
	
	public void onPause()
	{
		//reset torch
		if(!MainScreen.isHALv3)
		{
			try 
	    	{
	    		 Camera.Parameters p = cameraController.getCameraParameters();
	    		 if (p != null && cameraController.isFlashModeSupported())
	        	 {	
	    			 p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
	    			 cameraController.setCameraParameters(p);
	        	 }
			} catch (Exception e) {
				e.printStackTrace();
			}		
		
			if (CameraController.camera != null)
			{
				CameraController.camera.setPreviewCallback(null);
				CameraController.camera.stopPreview();
				CameraController.camera.release();
				CameraController.camera = null;
				CameraController.cameraParameters = null;
			}
		}
		else
			HALv3.onPauseHALv3();
	}
	
	
	
	public void onStop()
	{
		
	}
	
	public void onDestroy()
	{
		
	}
	
	public void setPreviewSurface(Surface srf)
	{
		mPreviewSurface = srf;
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
				cameraController.mVideoStabilizationSupported = isVideoStabilizationSupported();
		}
		else
			HALv3.openCameraHALv3();
		


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
			HALv3.PopulateCameraDimensionsHALv3();
		else
			PopulateCameraDimensions();
		
		CameraController.ResolutionsMPixListIC = CameraController.ResolutionsMPixList;
		CameraController.ResolutionsIdxesListIC = CameraController.ResolutionsIdxesList;
		CameraController.ResolutionsNamesListIC = CameraController.ResolutionsNamesList;

		PluginManager.getInstance().SelectImageDimension(); // updates SX, SY
															// values

		
		if(MainScreen.isHALv3)
			HALv3.setupImageReadersHALv3();
			
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
////		Util.initialize(MainScreen.mainContext);
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
	
	public static boolean isCameraCreated()
	{
		if(!MainScreen.isHALv3)
			return camera != null;
		else
			return isCameraCreatedHALv3();
				
	}
	
	@TargetApi(19)
	public static boolean isCameraCreatedHALv3()
	{
		return HALv3.getInstance().camDevice != null;
	}
	
	
	
	
	
	public void PopulateCameraDimensions()
	{
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();	
		
		
		int MinMPIX = CameraController.MIN_MPIX_SUPPORTED;
		Camera.Parameters cp = getCameraParameters();
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
			CameraController.ResolutionsSizeList.add(loc, CameraController.getInstance().new Size(s.width, s.height));
			
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
			CameraController.ResolutionsSizeList.add(0, CameraController.getInstance().new Size(s.width, s.height));
		}

		return;
	}
	
	public List<CameraController.Size> getSupportedPreviewSizes()
	{
		List<CameraController.Size> previewSizes = new ArrayList<CameraController.Size>();
		if(!MainScreen.isHALv3)
		{
			List<Camera.Size> sizes = cameraParameters.getSupportedPreviewSizes();
			for(Camera.Size sz : sizes)
				previewSizes.add(this.new Size(sz.width, sz.height));
		}
		else
			HALv3.fillPreviewSizeList(previewSizes);
			
		return previewSizes;
	}
	
	
	
	public List<CameraController.Size> getSupportedPictureSizes()
	{
		List<CameraController.Size> pictureSizes = new ArrayList<CameraController.Size>();
		if(!MainScreen.isHALv3)
		{
			List<Camera.Size> sizes = cameraParameters.getSupportedPictureSizes();
			for(Camera.Size sz : sizes)
				pictureSizes.add(this.new Size(sz.width, sz.height));
		}
		else
			HALv3.fillPictureSizeList(pictureSizes);
			
		return pictureSizes;
	}
	
	
	public static int getNumberOfCameras()
	{
		if(!MainScreen.isHALv3)
			return Camera.getNumberOfCameras();
		else
			return CameraController.getInstance().cameraIdList.length;
	}

	
	public void updateCameraFeatures()
	{
		mEVSupported = isExposureCompensationSupported();
		mSceneModeSupported = isSceneModeSupported();
		mWBSupported = isWhiteBalanceSupported();
		mFocusModeSupported = isFocusModeSupported();
		mFlashModeSupported = isFlashModeSupported();
		mISOSupported = isISOSupported();
	}

	@Override
	public void onError(int arg0, Camera arg1)
	{
		// TODO Auto-generated method stub
		
	}	
	
	//------------ CAMERA PARAMETERS AND CAPABILITIES SECTION-------------------------------------------
	public static boolean isAutoExposureLockSupported()
	{
		return false;
	}
	
	public static boolean isAutoWhiteBalanceLockSupported()
	{
		return false;
	}
	
	/*
	 * CAMERA parameters access functions
	 * 
	 * Camera.Parameters get/set Camera scene modes getSupported/set Camera
	 * white balance getSupported/set Camera focus modes getSupported/set Camera
	 * flash modes getSupported/set
	 * 
	 * For API14 Camera focus areas get/set Camera metering areas get/set
	 */
	public boolean isFrontCamera()
	{
		return CameraMirrored;
	}

	public static Camera getCamera()
	{
		return camera;
	}
	

	public static void setCamera(Camera cam) {
		CameraController.camera = cam;
	}

	public Camera.Parameters getCameraParameters() {
		if (CameraController.camera != null && CameraController.cameraParameters != null)
			return CameraController.cameraParameters;

		return null;
	}
	

	public boolean setCameraParameters(Camera.Parameters params) {
		if (params != null && CameraController.camera != null)
		{			
			try
			{
				CameraController.camera.setParameters(params);
				CameraController.cameraParameters = params;
				//cameraParameters = camera.getParameters();
			}
			catch (Exception e) {
				e.printStackTrace();
				Log.e("MainScreen", "setCameraParameters exception: " + e.getMessage());
				return false;
			}
			
			return true;
		}
		
		return false;		
	}
	
	public static void startCameraPreview()
	{
		if(camera != null)
			camera.startPreview();
	}
	
	public static void stopCameraPreview()
	{
		if(camera != null)
			camera.stopPreview();
	}
	
	public static void lockCamera()
	{
		if(camera != null)
			camera.lock();
	}
	
	public static void unlockCamera()
	{
		if(camera != null)
			camera.unlock();
	}
	
	@TargetApi(15)
	public void setVideoStabilization(boolean stabilization)
	{
		if(CameraController.cameraParameters != null && CameraController.cameraParameters.isVideoStabilizationSupported())
		{
			CameraController.cameraParameters.setVideoStabilization(stabilization);
			this.setCameraParameters(CameraController.cameraParameters);
		}
	}
	
	@TargetApi(15)
	public boolean isVideoStabilizationSupported()
	{
		if(CameraController.cameraParameters != null)
			return CameraController.cameraParameters.isVideoStabilizationSupported();
		
		return false;
	}
	
	public boolean isExposureLockSupported()
	{
//		if (camera != null && cameraParameters != null) {
//			if (cameraParameters.isAutoExposureLockSupported())
//				return true;
//			else
//				return false;
//		} else
//			return false;
		return true;
	}
	
	public boolean isWhiteBalanceLockSupported()
	{
//		if (camera != null && cameraParameters != null) {
//			if (cameraParameters.isAutoWhiteBalanceLockSupported())
//				return true;
//			else
//				return false;
//		} else
//			return false;
		return true;
	}
	
	public boolean isZoomSupported()
	{
		if(!MainScreen.isHALv3)
		{
			if (null==camera)
	    		return false;
	        Camera.Parameters cp = CameraController.getInstance().getCameraParameters();
	        
	        return cp.isZoomSupported();
		}
		else
		{
			return HALv3.isZoomSupportedHALv3();
		}
	}
	
	public int getMaxZoom()
	{
		if(!MainScreen.isHALv3)
		{
			if (null==camera)
	    		return 1;
	        Camera.Parameters cp = CameraController.getInstance().getCameraParameters();
	        
	        int maxZoom = cp.getMaxZoom();
	        return maxZoom;
		}
		else
		{
			float maxZoom = HALv3.getMaxZoomHALv3();
			return (int)(maxZoom - 10.0f);
		}
	}
	
	public void setZoom(int value)
	{
		if(!MainScreen.isHALv3)
		{
			Camera.Parameters cp = this.getCameraParameters();
			if(cp != null)
			{
				cp.setZoom(value);
				this.setCameraParameters(cp);
			}
		}
		else
			HALv3.setZoom(value/10.0f + 1f);
		
	}
	
	public boolean isLumaAdaptationSupported()
	{
		if(!MainScreen.isHALv3)
		{			
	    	if (null==camera)
	    		return false;
	        Camera.Parameters cp = CameraController.getInstance().getCameraParameters();
	        
	        String luma = cp.get("luma-adaptation");
	        if (luma == null)
	        	return false;
	        else
	        	return true;
		}
		else
		{
			return false;
		}
	}
	

	public boolean isExposureCompensationSupported()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null && CameraController.cameraParameters != null) {
				if (CameraController.cameraParameters.getMinExposureCompensation() == 0
						&& CameraController.cameraParameters.getMaxExposureCompensation() == 0)
					return false;
				else
					return true;
			} else
				return false;
		}
		else
			return HALv3.isExposureCompensationSupportedHALv3();
	}
	

	public int getMinExposureCompensation()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null && CameraController.cameraParameters != null)
				return CameraController.cameraParameters.getMinExposureCompensation();
			else
				return 0;
		}
		else
			return HALv3.getMinExposureCompensationHALv3();
	}
	

	public int getMaxExposureCompensation()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null && CameraController.cameraParameters != null)
				return CameraController.cameraParameters.getMaxExposureCompensation();
			else
				return 0;
		}
		else
			return HALv3.getMaxExposureCompensationHALv3();
	}
	

	public float getExposureCompensationStep()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null && CameraController.cameraParameters != null)
				return CameraController.cameraParameters.getExposureCompensationStep();
			else
				return 0;
		}
		else
			return HALv3.getExposureCompensationStepHALv3();
	}
	

	public float getExposureCompensation()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null && CameraController.cameraParameters != null)
				return CameraController.cameraParameters.getExposureCompensation()
						* CameraController.cameraParameters.getExposureCompensationStep();
			else
				return 0;
		}
		else
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
			int currEv = prefs.getInt(MainScreen.sEvPref, 0);
			
			return currEv;
		}
	}

	public void resetExposureCompensation()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null) {
				if (!isExposureCompensationSupported())
					return;
				Camera.Parameters params = CameraController.cameraParameters;
				params.setExposureCompensation(0);
				setCameraParameters(params);
			}
		}
		else
			HALv3.resetExposureCompensationHALv3();
	}
	
	

	public boolean isSceneModeSupported()
	{
//		if(!MainScreen.isHALv3)
//		{
//		List<String> supported_scene = getSupportedSceneModes();
//		if (supported_scene != null && supported_scene.size() > 0)
//			return true;
//		else
//			return false;
//		}
//		else
//		{
//			if(HALv3.getInstance().camCharacter != null)
//			{
//				byte scenes[]  = HALv3.getInstance().camCharacter.get(CameraCharacteristics.CONTROL_AVAILABLE_SCENE_MODES);
//				if(scenes.length > 0 && scenes[0] != CameraCharacteristics.CONTROL_SCENE_MODE_UNSUPPORTED)
//					return true;				
//			}
//			
//			return false;
//		}
		
		
		byte supported_scene[] = getSupportedSceneModes();
		if (supported_scene != null && supported_scene.length > 0 && supported_scene[0] != CameraParameters.SCENE_MODE_UNSUPPORTED)
			return true;
		else
			return false;
	}

	public byte[] getSupportedSceneModes()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				List<String> sceneModes = CameraController.cameraParameters.getSupportedSceneModes();
				byte scenes[] = new byte[sceneModes.size()];
				for(int i = 0; i < sceneModes.size(); i++)
				{
					String mode = sceneModes.get(i);
					if(CameraController.key_scene.containsKey(mode))
						scenes[i] = CameraController.key_scene.get(mode).byteValue();
				}
				
				return scenes;
			}
	
			return null;
		}
		else
			return HALv3.getSupportedSceneModesHALv3();
	}
	

	public boolean isWhiteBalanceSupported()
	{
//		List<String> supported_wb = getSupportedWhiteBalance();
//		if (supported_wb != null && supported_wb.size() > 0)
//			return true;
//		else
//			return false;

		
		byte supported_wb[] = getSupportedWhiteBalance();
		if (supported_wb != null && supported_wb.length > 0)
			return true;
		else
			return false;
	}

	public byte[] getSupportedWhiteBalance()
	{
//		if (camera != null)
//			return cameraParameters.getSupportedWhiteBalance();
		
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				List<String> wbModes = CameraController.cameraParameters.getSupportedWhiteBalance();
				byte wb[] = new byte[wbModes.size()];
				for(int i = 0; i < wbModes.size(); i++)
				{
					String mode = wbModes.get(i);
					if(CameraController.key_wb.containsKey(mode))
						wb[i] = CameraController.key_wb.get(mode).byteValue();
				}
				return wb;
			}
	
			return null;
		}
		else
			return HALv3.getSupportedWhiteBalanceHALv3();
	}
	
	

	public boolean isFocusModeSupported()
	{
//		List<String> supported_focus = getSupportedFocusModes();
//		if (supported_focus != null && supported_focus.size() > 0)
//			return true;
//		else
//			return false;
		
		byte supported_focus[] = getSupportedFocusModes();
		if (supported_focus != null && supported_focus.length > 0)
			return true;
		else
			return false;
	}

	public byte[] getSupportedFocusModes() {
//		if (camera != null)
//			return cameraParameters.getSupportedFocusModes();
//
//		return null;
		
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				List<String> focusModes = CameraController.cameraParameters.getSupportedFocusModes();
				byte focus[] = new byte[focusModes.size()];
				for(int i = 0; i < focusModes.size(); i++)
				{
					String mode = focusModes.get(i);
					if(CameraController.key_focus.containsKey(mode))
						focus[i] = CameraController.key_focus.get(mode).byteValue();
				}
				
				return focus;
			}
	
			return null;
		}
		else
			return HALv3.getSupportedFocusModesHALv3();
	}
	
	

	public boolean isFlashModeSupported() {
//		List<String> supported_flash = getSupportedFlashModes();
//		if (supported_flash != null && supported_flash.size() > 0)
//			return true;
//		else
//			return false;
		
		if(MainScreen.isHALv3)
			return HALv3.isFlashModeSupportedHALv3();
		else
		{
			byte supported_flash[] = getSupportedFlashModes();
			if (supported_flash != null && supported_flash.length > 0)
				return true;
		}
		
		return false;
	}
	
	

	public byte[] getSupportedFlashModes()
	{		
//		if (camera != null)
//			return cameraParameters.getSupportedFlashModes();
//
//		return null;
		
		if(MainScreen.isHALv3)
		{
			if(isFlashModeSupported())
			{
				byte flash[] = new byte[3];
				flash[0] = CameraParameters.FLASH_MODE_OFF;
				flash[1] = CameraParameters.FLASH_MODE_SINGLE;
				flash[2] = CameraParameters.FLASH_MODE_TORCH;
				return flash;
			}
		}
		else
		{
			if (CameraController.camera != null)
			{
				List<String> flashModes = CameraController.cameraParameters.getSupportedFlashModes();
				byte flash[] = new byte[flashModes.size()];
				for(int i = 0; i < flashModes.size(); i++)
				{
					String mode = flashModes.get(i);
					if(CameraController.key_flash.containsKey(mode))
						flash[i] = CameraController.key_flash.get(flashModes.get(i)).byteValue();
				}
				
				return flash;
			}
		}

		return null;		
	}

	public boolean isISOSupported() {
		if(!MainScreen.isHALv3)
		{
			byte supported_iso[] = getSupportedISO();
			String isoSystem = CameraController.getInstance().getCameraParameters().get("iso");
			String isoSystem2 = CameraController.getInstance().getCameraParameters().get("iso-speed");
			if (supported_iso != null || isoSystem != null || isoSystem2 != null)
				return true;
			else
				return false;
		}
		else
			return HALv3.isISOModeSupportedHALv3();
	}

	public byte[] getSupportedISO()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				List<String> isoModes = null;
				Camera.Parameters camParams = CameraController.getInstance().getCameraParameters();
				String supportedIsoValues = camParams.get("iso-values");
				String supportedIsoValues2 = camParams.get("iso-speed-values");
				String supportedIsoValues3 = camParams.get("iso-mode-values");
				//String iso = camParams.get("iso");
				
				String delims = "[,]+";
				String[] ISOs = null;
				
				if (supportedIsoValues != "" && supportedIsoValues != null)
					ISOs = supportedIsoValues.split(delims);
				else if(supportedIsoValues2 != "" && supportedIsoValues2 != null)
					ISOs = supportedIsoValues2.split(delims);
				else if(supportedIsoValues3 != "" && supportedIsoValues3 != null)
					ISOs = supportedIsoValues3.split(delims);
				
				if(ISOs != null)
				{
					isoModes = new ArrayList<String>();				
					for (int i = 0; i < ISOs.length; i++)
						isoModes.add(ISOs[i]);
				}
				else
					return null;
				
				byte iso[] = new byte[isoModes.size()];
				for(int i = 0; i < isoModes.size(); i++)
				{
					String mode = isoModes.get(i);
					if(CameraController.key_iso.containsKey(mode))
					{
						if(CameraController.key_iso.containsKey(mode))
							iso[i] = CameraController.key_iso.get(isoModes.get(i)).byteValue();
						else if(CameraController.key_iso2.containsKey(mode))
							iso[i] = CameraController.key_iso2.get(isoModes.get(i)).byteValue();
					}
				}
				
				return iso;
			}
	
			return null;
		}
		else
			return HALv3.getSupportedISOModesHALv3();
		
//		if (camera != null)
//		{
//			Camera.Parameters camParams = MainScreen.cameraParameters;
//			String supportedIsoValues = camParams.get("iso-values");
//			String supportedIsoValues2 = camParams.get("iso-speed-values");
//			String supportedIsoValues3 = camParams.get("iso-mode-values");
//			//String iso = camParams.get("iso");
//			
//			String delims = "[,]+";
//			String[] ISOs = null;
//			
//			if (supportedIsoValues != "" && supportedIsoValues != null)
//				ISOs = supportedIsoValues.split(delims);
//			else if(supportedIsoValues2 != "" && supportedIsoValues2 != null)
//				ISOs = supportedIsoValues2.split(delims);
//			else if(supportedIsoValues3 != "" && supportedIsoValues3 != null)
//				ISOs = supportedIsoValues3.split(delims);
//			
//			if(ISOs != null)
//			{
//				List<String> isoList = new ArrayList<String>();				
//				for (int i = 0; i < ISOs.length; i++)
//					isoList.add(ISOs[i]);
//
//				return isoList;
//			}
//		}
//
//		return null;
	}	
	
	
	public int getMaxNumMeteringAreas()
	{
//		if(camera != null)
//		{
//			Camera.Parameters camParams = MainScreen.cameraParameters;
//			return camParams.getMaxNumMeteringAreas();
//		}
//		
//		return 0;
		
		if(MainScreen.isHALv3)
			return HALv3.getMaxNumMeteringAreasHALv3();
		else if(CameraController.camera != null)
		{
			Camera.Parameters camParams = CameraController.cameraParameters;
			return camParams.getMaxNumMeteringAreas();
		}
		
		return 0;
	}
	
	
	public int getMaxNumFocusAreas()
	{
//		if(camera != null)
//		{
//			Camera.Parameters camParams = MainScreen.cameraParameters;
//			return camParams.getMaxNumMeteringAreas();
//		}
//		
//		return 0;
		
		if(MainScreen.isHALv3)
			return HALv3.getMaxNumFocusAreasHALv3();
		else if(CameraController.camera != null)
		{
			Camera.Parameters camParams = CameraController.cameraParameters;
			return camParams.getMaxNumFocusAreas();
		}
		
		return 0;
	}
	
	
	
	
	public static boolean isModeAvailable(byte[] modeList, int mode)
	{
		boolean isAvailable = false;
		for(int currMode : modeList)
		{
			if(currMode == mode)
			{
				isAvailable = true;
				break;
			}
		}
		return isAvailable;
	}
	
	

	public int getSceneMode()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				try
				{
					Camera.Parameters params = CameraController.cameraParameters;
					if (params != null)
						return CameraController.key_scene.get(params.getSceneMode());
				}
				catch(Exception e)
				{
					e.printStackTrace();
					Log.e("MainScreen", "getSceneMode exception: " + e.getMessage());
				}
			}
		}
		else
			return PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(MainScreen.sSceneModePref, -1);

		return -1;
	}

	public int getWBMode()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				try
				{
					Camera.Parameters params = CameraController.cameraParameters;
					if (params != null)
						return CameraController.key_wb.get(params.getWhiteBalance());
				}
				catch(Exception e)
				{
					e.printStackTrace();
					Log.e("MainScreen", "getWBMode exception: " + e.getMessage());
				}
			}
		}
		else
			return PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(MainScreen.sWBModePref, -1);

		return -1;
	}

	public int getFocusMode()
	{
		
		if(!MainScreen.isHALv3)
		{
			try
			{
				if (CameraController.camera != null)
				{
					Camera.Parameters params = CameraController.cameraParameters;
					if (params != null)
						return CameraController.key_focus.get(params.getFocusMode());
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
				Log.e("MainScreen", "getFocusMode exception: " + e.getMessage());
			}
		}
		else
			return PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(CameraMirrored? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, -1);

		return -1;
	}

	public int getFlashMode()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				try
				{
					Camera.Parameters params = CameraController.cameraParameters;
					if (params != null)
						return CameraController.key_flash.get(params.getFlashMode());
				}
				catch (Exception e)
				{
					e.printStackTrace();
					Log.e("MainScreen", "getFlashMode exception: " + e.getMessage());
				}
			}
		}
		else
			return PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(MainScreen.sFlashModePref, -1);

		return -1;
	}

	public int getISOMode()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.cameraParameters;			
				if (params != null)
				{
					String iso = null;
					iso = params.get("iso");
					if(iso == null)
						iso = params.get("iso-speed");
					
					return CameraController.key_iso.get(iso);
				}
			}
		}
		else
			return PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(MainScreen.sISOPref, -1);

		return -1;
	}

	public void setCameraSceneMode(int mode)
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.cameraParameters;
				if (params != null)
				{
					params.setSceneMode(CameraController.mode_scene.get(mode));
					setCameraParameters(params);
				}
			}
		}
		else
			HALv3.setCameraSceneModeHALv3(mode);
	}
	
	

	public void setCameraWhiteBalance(int mode)
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.cameraParameters;
				if (params != null)
				{
					params.setWhiteBalance(CameraController.mode_wb.get(mode));
					setCameraParameters(params);
				}
			}
		}
		else
			HALv3.setCameraWhiteBalanceHALv3(mode);		
	}
	
	

	public void setCameraFocusMode(int mode)
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.cameraParameters;
				if (params != null)
				{
					params.setFocusMode(CameraController.mode_focus.get(mode));
					setCameraParameters(params);
					MainScreen.mAFLocked = false;
				}
			}
		}
		else
			HALv3.setCameraFocusModeHALv3(mode);
	}
	
	

	public void setCameraFlashMode(int mode)
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null)
			{
				Camera.Parameters params = CameraController.cameraParameters;
				if (params != null)
				{
					params.setFlashMode(CameraController.mode_flash.get(mode));
					setCameraParameters(params);
				}
			}
		}
		else
			HALv3.setCameraFlashModeHALv3(mode);
	}
	
	

	public void setCameraISO(int mode)
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null) 
			{
				Camera.Parameters params = CameraController.cameraParameters;
				if(params != null)
				{
					if(params.get(CameraParameters.isoParam) != null)
						params.set(CameraParameters.isoParam, CameraController.mode_iso.get(mode));
					else if(params.get(CameraParameters.isoParam2) != null)
						params.set(CameraParameters.isoParam2, CameraController.mode_iso.get(mode));
					if(false == this.setCameraParameters(params))
					{
						if(params.get(CameraParameters.isoParam) != null)
							params.set(CameraParameters.isoParam, CameraController.mode_iso2.get(mode));
						else if(params.get(CameraParameters.isoParam2) != null)
							params.set(CameraParameters.isoParam2, CameraController.mode_iso2.get(mode));
						this.setCameraParameters(params);
					}
				}
			}
		}
		else
			HALv3.setCameraISOModeHALv3(mode);
	}
	
	
	public void setLumaAdaptation(int iEv)
	{
		Camera.Parameters params = CameraController.getInstance().getCameraParameters();
		if(params != null)
		{
			params.set("luma-adaptation", iEv);
			setCameraParameters(params);
		}
	}

	public void setCameraExposureCompensation(int iEV)
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.camera != null) {
				Camera.Parameters params = CameraController.cameraParameters;
				if (params != null) {
					params.setExposureCompensation(iEV);
					setCameraParameters(params);
				}
			}
		}
		else
			HALv3.setCameraExposureCompensationHALv3(iEV);
	}
	
	
	
	public void setCameraFocusAreas(List<Area> focusAreas)
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.getCamera() != null)
			{
				try
				{
					Camera.Parameters params = CameraController.getInstance().getCameraParameters();
					if (params != null)
					{
						params.setFocusAreas(focusAreas);
						cameraController.setCameraParameters(params);
					}
				}
				catch (RuntimeException e)
				{
					Log.e("SetFocusArea", e.getMessage());
				}
			}
		}
		else
			HALv3.setCameraFocusAreasHALv3(focusAreas);
	}
	
	public void setCameraMeteringAreas(List<Area> meteringAreas)
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.getCamera() != null)
			{
				try
				{
					Camera.Parameters params = CameraController.getInstance().getCameraParameters();
					if (params != null)
					{
						if(meteringAreas != null)
						{
							params.setMeteringAreas(null);
							cameraController.setCameraParameters(params);
						}
						params.setMeteringAreas(meteringAreas);
						cameraController.setCameraParameters(params);
					}
				} 
				catch (RuntimeException e)
				{
					Log.e("SetMeteringArea", e.getMessage());
				}
			}
		}
		else
			HALv3.setCameraMeteringAreasHALv3(meteringAreas);
	}
	
	
	public static void setFocusState(int state)
	{
		if (state != CameraController.FOCUS_STATE_IDLE
				&& state != CameraController.FOCUS_STATE_FOCUSED
				&& state != CameraController.FOCUS_STATE_FAIL)
			return;

		mFocusState = state;

		Message msg = new Message();
		msg.what = PluginManager.MSG_BROADCAST;
		msg.arg1 = PluginManager.MSG_FOCUS_STATE_CHANGED;
		MainScreen.H.sendMessage(msg);
	}

	public static int getFocusState()
	{
		return mFocusState;
	}
	
	
	public int getPreviewFrameRate()
	{		
		if(!MainScreen.isHALv3)
		{
			int range[] = {0 , 0};
			cameraParameters.getPreviewFpsRange(range);
			return range[1]/1000;
		}
		else
			return HALv3.getPreviewFrameRateHALv3();
	}
	
	
	public void setPictureSize(int width, int height)
	{
		final Camera.Parameters cp = getCameraParameters();
		if (cp == null)
		{
			return;
		}
		
		cp.setPictureSize(width, height);
	}
	
	public void setJpegQuality(int quality)
	{
		final Camera.Parameters cp = getCameraParameters();
		if (cp == null)
		{
			return;
		}
		
		cp.setJpegQuality(quality);
	}
	
	
	//^^^^^^^^^^^ CAMERA PARAMETERS AND CAPABILITIES SECTION---------------------------------------------
	
	
	
	//------------ CAPTURE AND FOCUS FUNCTION ----------------------------
	
//	public static boolean takePicture()
//	{
//		synchronized (CameraController.getInstance().syncObject)
//		{
//			if (camera != null && CameraController.getFocusState() != CameraController.FOCUS_STATE_FOCUSING) 
//			{
//				mCaptureState = CameraController.CAPTURE_STATE_CAPTURING;
//				// Log.e("", "mFocusState = " + getFocusState());
//				camera.setPreviewCallback(null);
//				camera.takePicture(null, null, null, MainScreen.thiz);
//				return true;
//			}
//
//			return false;
//		}
//	}
	
	public static int captureImage(int nFrames, int format)
	{
		//In old camera interface we can capture only JPEG images, so image format parameter will be ignored.
		if(!MainScreen.isHALv3)
		{
			synchronized (CameraController.getInstance().syncObject)
			{
				if (camera != null && CameraController.getFocusState() != CameraController.FOCUS_STATE_FOCUSING) 
				{
					mCaptureState = CameraController.CAPTURE_STATE_CAPTURING;
					// Log.e("", "mFocusState = " + getFocusState());
					camera.setPreviewCallback(null);
					camera.takePicture(CameraController.getInstance(), null, null, CameraController.getInstance());
					return 0;
				}

				return -1;
			}
		}
		else
			return HALv3.captureImageHALv3(nFrames, format);
	}
	
	

	public static boolean autoFocus(Camera.AutoFocusCallback listener)
	{
		synchronized (CameraController.getInstance().syncObject)
		{
			if(!MainScreen.isHALv3)
			{
			if (CameraController.getCamera() != null)
			{
				if (CameraController.mCaptureState != CameraController.CAPTURE_STATE_CAPTURING)
				{
					CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
					try {
						CameraController.getCamera().autoFocus(listener);
					}catch (Exception e) {
						e.printStackTrace();
						Log.e("MainScreen autoFocus(listener) failed", "autoFocus: " + e.getMessage());
						return false;
					}
					return true;
				}
			}
			}
			else
				return HALv3.autoFocusHALv3();
				
			return false;
		}
	}
	
	

	public static boolean autoFocus()
	{
		synchronized (CameraController.getInstance().syncObject)
		{
			if(!MainScreen.isHALv3)
			{
				if (CameraController.getCamera() != null)
				{
					if (CameraController.mCaptureState != CameraController.CAPTURE_STATE_CAPTURING)
					{
						//int fm = thiz.getFocusMode();
						// Log.e("", "mCaptureState = " + mCaptureState);
						CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
						try {
							CameraController.getCamera().autoFocus(CameraController.getInstance());
						}catch (Exception e) {
							e.printStackTrace();
							Log.e("MainScreen autoFocus() failed", "autoFocus: " + e.getMessage());
							return false;
						}					
						return true;
					}
				}
			}
			else
				return HALv3.autoFocusHALv3();
			
			return false;
		}
	}
	
	

	public static void cancelAutoFocus()
	{
		if(!MainScreen.isHALv3)
		{
			if (CameraController.getCamera() != null) {
				CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);
				try
				{
					camera.cancelAutoFocus();
				}
				catch(RuntimeException exp)
				{
					Log.e("MainScreen", "cancelAutoFocus failed. Message: " + exp.getMessage());
				}
			}
		}
		else
			HALv3.cancelAutoFocusHALv3();
	}
	
	
	
	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera) 
	{
		CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
		CameraController.getCamera().addCallbackBuffer(pviewBuffer);
		
		PluginManager.getInstance().onPictureTaken(paramArrayOfByte,
				paramCamera);
		CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;
	}

	@Override
	public void onAutoFocus(boolean focused, Camera paramCamera)
	{
		PluginManager.getInstance().onAutoFocus(focused);
		if (focused)
			CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSED);
		else
			CameraController.setFocusState(CameraController.FOCUS_STATE_FAIL);
	}
	
	
	public void onAutoFocus(boolean focused)
	{
		PluginManager.getInstance().onAutoFocus(focused);
		if (focused)
			CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSED);
		else
			CameraController.setFocusState(CameraController.FOCUS_STATE_FAIL);
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera)
	{
		PluginManager.getInstance().onPreviewFrame(data, paramCamera);
		CameraController.getCamera().addCallbackBuffer(pviewBuffer);
	}
	
	
	public static void setPreviewCallbackWithBuffer()
	{
		if(!MainScreen.isHALv3)
		{
			CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
			CameraController.getCamera().addCallbackBuffer(CameraController.getInstance().pviewBuffer);
		}
	}
	//^^^^^^^^^^^^^ CAPTURE AND FOCUS FUNCTION ----------------------------
	
	
	
	
	
	
	public class Size
	{
		private int mWidth;
		private int mHeight;
		
		public Size(int w, int h)
		{
			mWidth = w;
			mHeight = h;
		}
		
		public int getWidth()
		{
			return mWidth;
		}
		
		public int getHeight()
		{
			return mHeight;
		}
		
		public void setWidth(int width)
		{
			mWidth = width;
		}
		
		public void setHeight(int height)
		{
			mHeight = height;
		}
	}






	@Override
	public void onShutter() {
		// TODO Auto-generated method stub
		
	}
}
