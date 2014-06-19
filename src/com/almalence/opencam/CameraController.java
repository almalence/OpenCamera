/*
The contents of this file are subject to the Mozilla Public License
Version 1.1 (the "License"); you may not use this file except in
compliance with the License. You may obtain a copy of the License at
http://www.mozilla.org/MPL/

Software distributed under the License is distributed on an "AS IS"
basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
License for the specific language governing rights and limitations
under the License.

The Original Code is collection of files collectively known as Open Camera.

The Initial Developer of the Original Code is Almalence Inc.
Portions created by Initial Developer are Copyright (C) 2013 
by Almalence Inc. All Rights Reserved.
*/

/* <!-- +++
package com.almalence.opencam_plus;
+++ --> */
// <!-- -+-
package com.almalence.opencam;
//-+- -->

import java.io.IOException;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.os.Build;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

public class CameraController implements Camera.PictureCallback, Camera.AutoFocusCallback, Camera.ErrorCallback, Camera.PreviewCallback, Camera.ShutterCallback
{
	private final static String TAG = "CameraController";
	
	public static final int YUV = 1;
	public static final int JPEG = 0;
	// Android camera parameters constants
	private static String sceneAuto;
	private static String sceneAction;
	private static String scenePortrait;
	private static String sceneLandscape;
	private static String sceneNight;
	private static String sceneNightPortrait;
	private static String sceneTheatre;
	private static String sceneBeach;
	private static String sceneSnow;
	private static String sceneSunset;
	private static String sceneSteadyPhoto;
	private static String sceneFireworks;
	private static String sceneSports;
	private static String sceneParty;
	private static String sceneCandlelight;
	private static String sceneBarcode;
	private static String sceneHDR;
	private static String sceneAR;

	private static String wbAuto;
	private static String wbIncandescent;
	private static String wbFluorescent;
	private static String wbWarmFluorescent;
	private static String wbDaylight;
	private static String wbCloudyDaylight;
	private static String wbTwilight;
	private static String wbShade;

	private static String focusAuto;
	private static String focusInfinity;
	private static String focusNormal;
	private static String focusMacro;
	private static String focusFixed;
	private static String focusEdof;
	private static String focusContinuousVideo;
	private static String focusContinuousPicture;
	private static String focusAfLock;


	private static String flashAuto;
	private static String flashOn;
	private static String flashOff;
	private static String flashRedEye;
	private static String flashTorch;
	
	private static String isoAuto;
	private static String iso50;
	private static String iso100;
	private static String iso200;
	private static String iso400;
	private static String iso800;
	private static String iso1600;
	private static String iso3200;
	
	private static String isoAuto_2;
	private static String iso50_2;
	private static String iso100_2;
	private static String iso200_2;
	private static String iso400_2;
	private static String iso800_2;
	private static String iso1600_2;
	private static String iso3200_2;
	
	private static String meteringAuto;
	private static String meteringMatrix;
	private static String meteringCenter;
	private static String meteringSpot;
	
	
	// List of localized names for camera parameters values	
	private static Map<Integer, String> mode_scene;	
	private static Map<String, Integer> key_scene;	

	private static Map<Integer, String> mode_wb;	
	private static Map<String, Integer> key_wb;

	private static Map<Integer, String> mode_focus;	
	private static Map<String, Integer> key_focus;
	
	private static Map<Integer, String> mode_flash;	
	private static Map<String, Integer> key_flash;	
	
	private static List<Integer> iso_values;	
	private static List<String> iso_default;		
	private static Map<String, String> iso_default_values;	
	private static Map<Integer, String> mode_iso;	
	private static Map<Integer, String> mode_iso2;	
	private static Map<Integer, Integer> mode_iso_HALv3;	
	private static Map<String, Integer> key_iso;	
	private static Map<String, Integer> key_iso2;	
	
	private static CameraController cameraController = null;
	
	private PluginManagerInterface pluginManager = null;
	private ApplicationInterface appInterface= null;
	private Context mainContext = null;
	
	//Old camera interface
	private static Camera camera = null;
	private static Camera.Parameters cameraParameters = null;
	private byte[] pviewBuffer;
	
	private static boolean isHALv3 = false;
	private static boolean isHALv3Supported = false;
	
	public String[] cameraIdList={""};
	
	public static boolean cameraConfigured = false;
	
	
	// Flags to know which camera feature supported at current device
	private boolean mEVSupported = false;
	private boolean mSceneModeSupported = false;
	private boolean mWBSupported = false;
	private boolean mFocusModeSupported = false;
	private boolean mFlashModeSupported = false;
	private boolean mISOSupported = false;
	private boolean mCameraChangeSupported = false;
	
	private int minExpoCompensation = 0;
	private int maxExpoCompensation = 0;
	private float expoCompensationStep = 0;
	
	public boolean mVideoStabilizationSupported = false;

	public static byte[] supportedSceneModes;
	public static byte[] supportedWBModes;
	public static byte[] supportedFocusModes;
	public static byte[] supportedFlashModes;
	public static byte[] supportedISOModes;
	
	
	public static int maxRegionsSupported;
	
	public static List<Area> mMeteringAreaMatrix5 = new ArrayList<Area>();	
	public static List<Area> mMeteringAreaMatrix4 = new ArrayList<Area>();	
	public static List<Area> mMeteringAreaMatrix1 = new ArrayList<Area>();	
	public static List<Area> mMeteringAreaCenter = new ArrayList<Area>();	
	public static List<Area> mMeteringAreaSpot = new ArrayList<Area>();
	
	public static int currentMeteringMode = -1;
	
	
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
	
	
	
	public void onCreate(Context context, ApplicationInterface app, PluginManagerInterface pluginManagerBase)
	{
		pluginManager = pluginManagerBase;
		appInterface = app;
		mainContext = context;
		
		sceneAuto = mainContext.getResources().getString(R.string.sceneAutoSystem);
		sceneAction = mainContext.getResources().getString(R.string.sceneActionSystem);
		scenePortrait = mainContext.getResources().getString(R.string.scenePortraitSystem);
		sceneLandscape = mainContext.getResources().getString(R.string.sceneLandscapeSystem);
		sceneNight = mainContext.getResources().getString(R.string.sceneNightSystem);
		sceneNightPortrait = mainContext.getResources().getString(R.string.sceneNightPortraitSystem);
		sceneTheatre = mainContext.getResources().getString(R.string.sceneTheatreSystem);
		sceneBeach = mainContext.getResources().getString(R.string.sceneBeachSystem);
		sceneSnow = mainContext.getResources().getString(R.string.sceneSnowSystem);
		sceneSunset = mainContext.getResources().getString(R.string.sceneSunsetSystem);
		sceneSteadyPhoto = mainContext.getResources().getString(R.string.sceneSteadyPhotoSystem);
		sceneFireworks = mainContext.getResources().getString(R.string.sceneFireworksSystem);
		sceneSports = mainContext.getResources().getString(R.string.sceneSportsSystem);
		sceneParty = mainContext.getResources().getString(R.string.scenePartySystem);
		sceneCandlelight = mainContext.getResources().getString(R.string.sceneCandlelightSystem);
		sceneBarcode = mainContext.getResources().getString(R.string.sceneBarcodeSystem);
		sceneHDR = mainContext.getResources().getString(R.string.sceneHDRSystem);
		sceneAR = mainContext.getResources().getString(R.string.sceneARSystem);

		wbAuto = mainContext.getResources().getString(R.string.wbAutoSystem);
		wbIncandescent = mainContext.getResources().getString(R.string.wbIncandescentSystem);
		wbFluorescent = mainContext.getResources().getString(R.string.wbFluorescentSystem);
		wbWarmFluorescent = mainContext.getResources().getString(R.string.wbWarmFluorescentSystem);
		wbDaylight = mainContext.getResources().getString(R.string.wbDaylightSystem);
		wbCloudyDaylight = mainContext.getResources().getString(R.string.wbCloudyDaylightSystem);
		wbTwilight = mainContext.getResources().getString(R.string.wbTwilightSystem);
		wbShade = mainContext.getResources().getString(R.string.wbShadeSystem);

		focusAuto = mainContext.getResources().getString(R.string.focusAutoSystem);
		focusInfinity = mainContext.getResources().getString(R.string.focusInfinitySystem);
		focusNormal = mainContext.getResources().getString(R.string.focusNormalSystem);
		focusMacro = mainContext.getResources().getString(R.string.focusMacroSystem);
		focusFixed = mainContext.getResources().getString(R.string.focusFixedSystem);
		focusEdof = mainContext.getResources().getString(R.string.focusEdofSystem);
		focusContinuousVideo = mainContext.getResources().getString(R.string.focusContinuousVideoSystem);
		focusContinuousPicture = mainContext.getResources().getString(R.string.focusContinuousPictureSystem);
		focusAfLock = mainContext.getResources().getString(R.string.focusAfLockSystem);


		flashAuto = mainContext.getResources().getString(R.string.flashAutoSystem);
		flashOn = mainContext.getResources().getString(R.string.flashOnSystem);
		flashOff = mainContext.getResources().getString(R.string.flashOffSystem);
		flashRedEye = mainContext.getResources().getString(R.string.flashRedEyeSystem);
		flashTorch = mainContext.getResources().getString(R.string.flashTorchSystem);
		
		isoAuto = mainContext.getResources().getString(R.string.isoAutoSystem);
		iso50 = mainContext.getResources().getString(R.string.iso50System);
		iso100 = mainContext.getResources().getString(R.string.iso100System);
		iso200 = mainContext.getResources().getString(R.string.iso200System);
		iso400 = mainContext.getResources().getString(R.string.iso400System);
		iso800 = mainContext.getResources().getString(R.string.iso800System);
		iso1600 = mainContext.getResources().getString(R.string.iso1600System);
		iso3200 = mainContext.getResources().getString(R.string.iso3200System);
		
		isoAuto_2 = mainContext.getResources().getString(R.string.isoAutoDefaultSystem);
		iso50_2 = mainContext.getResources().getString(R.string.iso50DefaultSystem);
		iso100_2 = mainContext.getResources().getString(R.string.iso100DefaultSystem);
		iso200_2 = mainContext.getResources().getString(R.string.iso200DefaultSystem);
		iso400_2 = mainContext.getResources().getString(R.string.iso400DefaultSystem);
		iso800_2 = mainContext.getResources().getString(R.string.iso800DefaultSystem);
		iso1600_2 = mainContext.getResources().getString(R.string.iso1600DefaultSystem);
		iso3200_2 = mainContext.getResources().getString(R.string.iso3200DefaultSystem);
		
		meteringAuto = mainContext.getResources().getString(R.string.meteringAutoSystem);
		meteringMatrix = mainContext.getResources().getString(R.string.meteringMatrixSystem);
		meteringCenter = mainContext.getResources().getString(R.string.meteringCenterSystem);
		meteringSpot = mainContext.getResources().getString(R.string.meteringSpotSystem);
		
		// List of localized names for camera parameters values	
		mode_scene = new Hashtable<Integer, String>() {
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
		
		key_scene = new Hashtable<String, Integer>() {
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
		
		

		mode_wb = new Hashtable<Integer, String>() {
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
		
		key_wb = new Hashtable<String, Integer>() {
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


		mode_focus = new Hashtable<Integer, String>() {
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
		
		key_focus = new Hashtable<String, Integer>() {
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

		
		mode_flash = new Hashtable<Integer, String>() {
			{
				put(CameraParameters.FLASH_MODE_OFF, flashOff);
				put(CameraParameters.FLASH_MODE_AUTO, flashAuto);
				put(CameraParameters.FLASH_MODE_SINGLE, flashOn);
				put(CameraParameters.FLASH_MODE_REDEYE, flashRedEye);
				put(CameraParameters.FLASH_MODE_TORCH, flashTorch);
			}
		};
		
		key_flash = new Hashtable<String, Integer>() {
			{
				put(flashOff, CameraParameters.FLASH_MODE_OFF);
				put(flashAuto, CameraParameters.FLASH_MODE_AUTO);
				put(flashOn, CameraParameters.FLASH_MODE_SINGLE);
				put(flashRedEye, CameraParameters.FLASH_MODE_REDEYE);
				put(flashTorch, CameraParameters.FLASH_MODE_TORCH);
			}
		};
		
		
		iso_values = new ArrayList<Integer>() {
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
		
		
		iso_default = new ArrayList<String>() {
			{			
				add(isoAuto);			
				add(iso100);
				add(iso200);
				add(iso400);
				add(iso800);
				add(iso1600);
			}
		};
		
			
		iso_default_values = new Hashtable<String, String>() {
			{			
				put(isoAuto, mainContext.getResources().getString(R.string.isoAutoDefaultSystem));			
				put(iso100, mainContext.getResources().getString(R.string.iso100DefaultSystem));
				put(iso200, mainContext.getResources().getString(R.string.iso200DefaultSystem));
				put(iso400, mainContext.getResources().getString(R.string.iso400DefaultSystem));
				put(iso800, mainContext.getResources().getString(R.string.iso800DefaultSystem));
				put(iso1600, mainContext.getResources().getString(R.string.iso1600DefaultSystem));
			}
		};
		
		mode_iso = new Hashtable<Integer, String>() {
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
		
		mode_iso2 = new Hashtable<Integer, String>() {
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
		
		
		mode_iso_HALv3 = new Hashtable<Integer, Integer>() {
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
		
		key_iso = new Hashtable<String, Integer>() {
			{
				put(isoAuto, CameraParameters.ISO_AUTO);
				put(iso50, CameraParameters.ISO_50);
				put(iso100, CameraParameters.ISO_100);
				put(iso200, CameraParameters.ISO_200);
				put(iso400, CameraParameters.ISO_400);
				put(iso800, CameraParameters.ISO_800);
				put(iso1600, CameraParameters.ISO_1600);
				put(iso3200, CameraParameters.ISO_3200);
			}
		};
		
		key_iso2 = new Hashtable<String, Integer>() {
			{
				put(isoAuto_2, CameraParameters.ISO_AUTO);
				put(iso50_2, CameraParameters.ISO_50);
				put(iso100_2, CameraParameters.ISO_100);
				put(iso200_2, CameraParameters.ISO_200);
				put(iso400_2, CameraParameters.ISO_400);
				put(iso800_2, CameraParameters.ISO_800);
				put(iso1600_2, CameraParameters.ISO_1600);
				put(iso3200_2, CameraParameters.ISO_3200);
			}
		};
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
		
		isHALv3 = prefs.getBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), false);
		if(null == mainContext.getSystemService("camera"))
		{
			isHALv3 = false;
			isHALv3Supported = false;
			prefs.edit().putBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), false).commit();
		}
		else
			isHALv3Supported = true;
		
		if(CameraController.isHALv3Supported)
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
		if(!CameraController.isHALv3)
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
	
	
	/* Get different list and maps of camera parameters */
	public static List<Integer> getIsoValuesList()
	{
		return iso_values;
	}
	
	public static List<String> getIsoDefaultList()
	{
		return iso_default;
	}
	
	public static Map<String, Integer> getIsoKey()
	{
		return key_iso;
	}
	
	public static Map<Integer, Integer> getIsoModeHALv3()
	{
		return mode_iso_HALv3;
	}
	/* ^^^ Get different list and maps of camera parameters */
	
	
	public void setPreviewSurface(Surface srf)
	{
		mPreviewSurface = srf;
	}
	
	
	/* Preview buffer methods */
	public void allocatePreviewBuffer(int size)
	{
		pviewBuffer = new byte[size];
	}
	
	public byte[] getPreviewBuffer()
	{
		return pviewBuffer;
	}
	/* ^^^ Preview buffer methods */
	
	
	public static void useHALv3(boolean useHALv3)
	{
		isHALv3 = useHALv3;
	}
	
	public static boolean isUseHALv3()
	{
		return isHALv3;
	}
	
	public static boolean isHALv3Supported()
	{
		return isHALv3Supported;
	}
	
	
	public void setupCamera(SurfaceHolder holder)
	{
		if(!CameraController.isHALv3)
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
					Toast.makeText(mainContext, "Unable to start camera", Toast.LENGTH_LONG).show();
					return;
				}
			}
			
			CameraController.cameraParameters = CameraController.camera.getParameters(); //Initialize of camera parameters variable
			
			if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				cameraController.mVideoStabilizationSupported = isVideoStabilizationSupported();
			
			pluginManager.SelectDefaults();

			// screen rotation
			if (!pluginManager.shouldPreviewToGPU())
			{
				try {
					camera.setDisplayOrientation(90);
				} catch (RuntimeException e) {
					e.printStackTrace();
				}
		
				try {
					camera.setPreviewDisplay(holder);
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		else
			HALv3.openCameraHALv3();
		


		pluginManager.SelectDefaults();

		if(!CameraController.isHALv3)
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
		}
		
		if(CameraController.isHALv3)
			HALv3.PopulateCameraDimensionsHALv3();
		else
			PopulateCameraDimensions();
		
		CameraController.ResolutionsMPixListIC = CameraController.ResolutionsMPixList;
		CameraController.ResolutionsIdxesListIC = CameraController.ResolutionsIdxesList;
		CameraController.ResolutionsNamesListIC = CameraController.ResolutionsNamesList;

		pluginManager.SelectImageDimension(); // updates SX, SY values
		
		if(CameraController.isHALv3)
			HALv3.setupImageReadersHALv3();
			
		appInterface.addSurfaceCallback();
		
		if(!CameraController.isHALv3)
			appInterface.configureCamera();
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
			return Camera.getNumberOfCameras();
		else
			return CameraController.getInstance().cameraIdList.length;
	}

	
	public void updateCameraFeatures()
	{
		mEVSupported = getExposureCompensationSupported();
		mSceneModeSupported = getSceneModeSupported();
		mWBSupported = getWhiteBalanceSupported();
		mFocusModeSupported = getFocusModeSupported();
		mFlashModeSupported = getFlashModeSupported();
		mISOSupported = getISOSupported();
		
		if(!CameraController.isHALv3)
		{
			if (CameraController.camera != null && CameraController.cameraParameters != null)
			{
				minExpoCompensation = CameraController.cameraParameters.getMinExposureCompensation();
				maxExpoCompensation = CameraController.cameraParameters.getMaxExposureCompensation();
				expoCompensationStep = CameraController.cameraParameters.getExposureCompensationStep();
			}
		}
		else
		{
			minExpoCompensation = HALv3.getMinExposureCompensationHALv3();
			maxExpoCompensation = HALv3.getMaxExposureCompensationHALv3();
			expoCompensationStep = HALv3.getExposureCompensationStepHALv3();
		}
		
		supportedSceneModes = getSupportedSceneModesInternal();
		supportedWBModes = getSupportedWhiteBalanceInternal();
		supportedFocusModes = getSupportedFocusModesInternal();
		supportedFlashModes = getSupportedFlashModesInternal();
		supportedISOModes = getSupportedISOInternal();
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
	
	public boolean applyCameraParameters()
	{
		if (CameraController.camera != null)
		{			
			try
			{
				CameraController.camera.setParameters(CameraController.cameraParameters);
			}
			catch (Exception e) {
				e.printStackTrace();
				Log.e("MainScreen", "applyCameraParameters exception: " + e.getMessage());
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
	

	//Used to initialize internal variable
	private boolean getExposureCompensationSupported()
	{
		if(!CameraController.isHALv3)
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
	
	// Used by CameraController class users.
	public boolean isExposureCompensationSupported()
	{
		return mEVSupported;
	}
	

	public int getMinExposureCompensation()
	{
		return minExpoCompensation;
	}
	

	public int getMaxExposureCompensation()
	{
		return maxExpoCompensation;
	}
	

	public float getExposureCompensationStep()
	{
		return expoCompensationStep;
	}
	

	public float getExposureCompensation()
	{
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
	
	

	private boolean getSceneModeSupported()
	{
		byte supported_scene[] = getSupportedSceneModesInternal();
		if (supported_scene != null && supported_scene.length > 0 && supported_scene[0] != CameraParameters.SCENE_MODE_UNSUPPORTED)
			return true;
		else
			return false;
	}
	
	public boolean isSceneModeSupported()
	{
		return mSceneModeSupported;
	}

	private byte[] getSupportedSceneModesInternal()
	{
		if(!CameraController.isHALv3)
		{
			List<String> sceneModes = CameraController.cameraParameters.getSupportedSceneModes();
			if (CameraController.camera != null && sceneModes != null)
			{
				Set<String> known_scenes = CameraController.key_scene.keySet();
				sceneModes.retainAll(known_scenes);
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
	
	public byte[] getSupportedSceneModes()
	{
		return supportedSceneModes;
	}
	

	private boolean getWhiteBalanceSupported()
	{
//		List<String> supported_wb = getSupportedWhiteBalance();
//		if (supported_wb != null && supported_wb.size() > 0)
//			return true;
//		else
//			return false;

		
		byte supported_wb[] = getSupportedWhiteBalanceInternal();
		if (supported_wb != null && supported_wb.length > 0)
			return true;
		else
			return false;
	}
	
	public boolean isWhiteBalanceSupported()
	{
		return mWBSupported;
	}

	private byte[] getSupportedWhiteBalanceInternal()
	{
//		if (camera != null)
//			return cameraParameters.getSupportedWhiteBalance();
		
		if(!CameraController.isHALv3)
		{
			List<String> wbModes = CameraController.cameraParameters.getSupportedWhiteBalance();
			if (CameraController.camera != null && wbModes != null)
			{
				Set<String> known_wb = CameraController.key_wb.keySet();
				wbModes.retainAll(known_wb);
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
	
	public byte[] getSupportedWhiteBalance()
	{
		return supportedWBModes;
	}
	
	

	private boolean getFocusModeSupported()
	{
//		List<String> supported_focus = getSupportedFocusModes();
//		if (supported_focus != null && supported_focus.size() > 0)
//			return true;
//		else
//			return false;
		
		byte supported_focus[] = getSupportedFocusModesInternal();
		if (supported_focus != null && supported_focus.length > 0)
			return true;
		else
			return false;
	}
	
	public boolean isFocusModeSupported()
	{
		return mFocusModeSupported;
	}

	private byte[] getSupportedFocusModesInternal()
	{
//		if (camera != null)
//			return cameraParameters.getSupportedFocusModes();
//
//		return null;
		
		if(!CameraController.isHALv3)
		{
			List<String> focusModes = CameraController.cameraParameters.getSupportedFocusModes();
			if (CameraController.camera != null && focusModes != null)
			{								
				Set<String> known_focus = CameraController.key_focus.keySet();
				focusModes.retainAll(known_focus);
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
	
	public byte[] getSupportedFocusModes()
	{
		return supportedFocusModes;
	}
	
	

	private boolean getFlashModeSupported()
	{
//		List<String> supported_flash = getSupportedFlashModes();
//		if (supported_flash != null && supported_flash.size() > 0)
//			return true;
//		else
//			return false;
		
		if(CameraController.isHALv3)
			return HALv3.isFlashModeSupportedHALv3();
		else
		{
			byte supported_flash[] = getSupportedFlashModesInternal();
			if (supported_flash != null && supported_flash.length > 0)
				return true;
		}
		
		return false;
	}
	
	public boolean isFlashModeSupported()
	{
		return mFlashModeSupported;
	}
	

	private byte[] getSupportedFlashModesInternal()
	{		
//		if (camera != null)
//			return cameraParameters.getSupportedFlashModes();
//
//		return null;
		
		if(CameraController.isHALv3)
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
			List<String> flashModes = CameraController.cameraParameters.getSupportedFlashModes();
			if (CameraController.camera != null && flashModes != null)
			{
				Set<String> known_flash = CameraController.key_flash.keySet();
				flashModes.retainAll(known_flash);
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
	
	public byte[] getSupportedFlashModes()
	{
		return supportedFlashModes;
	}
	

	private boolean getISOSupported()
	{
		if(!CameraController.isHALv3)
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
	
	public boolean isISOSupported()
	{
		return mISOSupported;
	}

	private byte[] getSupportedISOInternal()
	{
		if(!CameraController.isHALv3)
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
	}
	
	public byte[] getSupportedISO()
	{
		return supportedISOModes;
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
		
		if(CameraController.isHALv3)
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
		
		if(CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
	
	public float getHorizontalViewAngle()
	{
		if(!CameraController.isHALv3)
		{
			if(camera != null)
				return cameraParameters.getHorizontalViewAngle();
		}
		else if(Build.MODEL.contains("Nexus"))
			return 59.63f;			
		
		return 55.4f;		
	}
	
	public float getVerticalViewAngle()
	{
		if(!CameraController.isHALv3)
		{
			if(camera != null)
				return cameraParameters.getVerticalViewAngle();
		}
		else if(Build.MODEL.contains("Nexus"))
			return 46.66f;			
		
		return 42.7f;	
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
//				camera.takePicture(null, null, null, mainContext);
//				return true;
//			}
//
//			return false;
//		}
//	}
	
	public static int captureImage(int nFrames, int format)
	{
		//In old camera interface we can capture only JPEG images, so image format parameter will be ignored.
		if(!CameraController.isHALv3)
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
			if(!CameraController.isHALv3)
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
			if(!CameraController.isHALv3)
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
		if(!CameraController.isHALv3)
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
		
		pluginManager.onPictureTaken(paramArrayOfByte,
				paramCamera);
		CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;
	}

	@Override
	public void onAutoFocus(boolean focused, Camera paramCamera)
	{
		pluginManager.onAutoFocus(focused);
		if (focused)
			CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSED);
		else
			CameraController.setFocusState(CameraController.FOCUS_STATE_FAIL);
	}
	
	
	public void onAutoFocus(boolean focused)
	{
		pluginManager.onAutoFocus(focused);
		if (focused)
			CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSED);
		else
			CameraController.setFocusState(CameraController.FOCUS_STATE_FAIL);
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera)
	{
		pluginManager.onPreviewFrame(data, paramCamera);
		CameraController.getCamera().addCallbackBuffer(pviewBuffer);
	}
	
	
	public static void setPreviewCallbackWithBuffer()
	{
		if(!CameraController.isHALv3)
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
