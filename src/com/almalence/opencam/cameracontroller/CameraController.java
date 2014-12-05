/*
	CameraController for OpenCamera project - interface to camera device
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

import java.io.IOException;
import java.lang.reflect.Array;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Image;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.widget.Toast;

import com.almalence.SwapHeap;
import com.almalence.util.ImageConversion;
//<!-- -+-
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginManagerInterface;
import com.almalence.opencam.R;
//-+- -->
/* <!-- +++
import com.almalence.opencam_plus.ApplicationInterface;
import com.almalence.opencam_plus.CameraParameters;
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.PluginManagerInterface;
import com.almalence.opencam_plus.R;
+++ --> */

public class CameraController implements Camera.PictureCallback, Camera.AutoFocusCallback, Camera.ErrorCallback,
		Camera.PreviewCallback, Camera.ShutterCallback, Handler.Callback
{
	private static final String						TAG								= "CameraController";

	// YUV_RAW is the same as YUV (ie NV21) except that
	// noise filtering, edge enhancements and scaler
	// are disabled if possible
	public static final int							RAW							    = 0x20;
	public static final int							YUV_RAW							= 0x22;
	public static final int							YUV								= 0x23;
	public static final int							JPEG							= 0x100;

	protected static final long						MPIX_1080						= 1920 * 1080;

	// Android camera parameters constants
	private static String							sceneAuto;
	private static String							sceneAction;
	private static String							scenePortrait;
	private static String							sceneLandscape;
	private static String							sceneNight;
	private static String							sceneNightPortrait;
	private static String							sceneTheatre;
	private static String							sceneBeach;
	private static String							sceneSnow;
	private static String							sceneSunset;
	private static String							sceneSteadyPhoto;
	private static String							sceneFireworks;
	private static String							sceneSports;
	private static String							sceneParty;
	private static String							sceneCandlelight;
	private static String							sceneBarcode;
	private static String							sceneHDR;
	private static String							sceneAR;

	private static String							wbAuto;
	private static String							wbIncandescent;
	private static String							wbFluorescent;
	private static String							wbWarmFluorescent;
	private static String							wbDaylight;
	private static String							wbCloudyDaylight;
	private static String							wbTwilight;
	private static String							wbShade;

	private static String							focusAuto;
	private static String							focusInfinity;
	private static String							focusNormal;
	private static String							focusMacro;
	private static String							focusFixed;
	private static String							focusEdof;
	private static String							focusContinuousVideo;
	private static String							focusContinuousPicture;
	private static String							focusAfLock;

	private static String							flashAuto;
	private static String							flashOn;
	private static String							flashOff;
	private static String							flashRedEye;
	private static String							flashTorch;

	private static String							isoAuto;
	private static String							iso50;
	private static String							iso100;
	private static String							iso200;
	private static String							iso400;
	private static String							iso800;
	private static String							iso1600;
	private static String							iso3200;

	private static String							isoAuto_2;
	private static String							iso50_2;
	private static String							iso100_2;
	private static String							iso200_2;
	private static String							iso400_2;
	private static String							iso800_2;
	private static String							iso1600_2;
	private static String							iso3200_2;

	private static String							meteringAuto;
	private static String							meteringMatrix;
	private static String							meteringCenter;
	private static String							meteringSpot;

	// List of localized names for camera parameters values
	private static Map<Integer, String>				mode_scene;
	private static Map<String, Integer>				key_scene;

	private static Map<Integer, String>				mode_wb;
	private static Map<String, Integer>				key_wb;

	private static Map<Integer, String>				mode_focus;
	private static Map<String, Integer>				key_focus;

	private static Map<Integer, String>				mode_flash;
	private static Map<String, Integer>				key_flash;

	private static List<Integer>					iso_values;
	private static List<String>						iso_default;
	private static Map<String, String>				iso_default_values;
	private static Map<Integer, String>				mode_iso;
	private static Map<Integer, String>				mode_iso2;
	private static Map<Integer, Integer>			mode_iso_HALv3;
	private static Map<String, Integer>				key_iso;
	private static Map<String, Integer>				key_iso2;

	private static CameraController					cameraController				= null;

	private static PluginManagerInterface			pluginManager					= null;
	private static ApplicationInterface				appInterface					= null;
	protected static Context						mainContext						= null;

	// Old camera interface
	private static Camera							camera							= null;
	private static Camera.Parameters				cameraParameters				= null;

	private static byte[]							pviewBuffer;

	// Message handler for multishot capturing with pause between shots
	// and different exposure compensations
	private static Handler							messageHandler;
	private static Handler							pauseHandler;

	private static boolean							needRelaunch					= false;
	public static boolean							isVideoModeLaunched				= false;
	
	private static boolean							isHALv3							= false;
	private static boolean							isHALv3Supported				= false;
	protected static boolean						isRAWCaptureSupported			= false;

	protected static String[]								cameraIdList					= { "" };

	// Flags to know which camera feature supported at current device
	private static boolean							mEVSupported					= false;
	private static boolean							mSceneModeSupported				= false;
	private static boolean							mWBSupported					= false;
	private static boolean							mFocusModeSupported				= false;
	private static boolean							mFlashModeSupported				= false;
	private static boolean							mISOSupported					= false;

	private static int								minExpoCompensation				= 0;
	private static int								maxExpoCompensation				= 0;
	private static float							expoCompensationStep			= 0;

	protected static boolean						mVideoStabilizationSupported	= false;

	private static int[]							supportedSceneModes;
	private static int[]							supportedWBModes;
	private static int[]							supportedFocusModes;
	private static int[]							supportedFlashModes;
	private static int[]							supportedISOModes;

	private static int								maxRegionsSupported;

	protected static int							CameraIndex						= 0;
	protected static boolean						CameraMirrored					= false;

	// Image size index for capturing
	private static int								CapIdx;
	
	private static Size								imageSize;

	public static final int							MIN_MPIX_SUPPORTED				= 1280 * 720;

	// Lists of resolutions, their indexes and names (for capturing and preview)
	protected static List<Long>						ResolutionsMPixList;
	protected static List<CameraController.Size>	ResolutionsSizeList;
	protected static List<String>					ResolutionsIdxesList;
	protected static List<String>					ResolutionsNamesList;

	public static List<Long>						MultishotResolutionsMPixList;
	public static List<CameraController.Size>		MultishotResolutionsSizeList;
	public static List<String>						MultishotResolutionsIdxesList;
	public static List<String>						MultishotResolutionsNamesList;

	public static List<Integer>						FastIdxelist;											

	protected static List<CameraController.Size>	SupportedPreviewSizesList;
	protected static List<CameraController.Size>	SupportedPictureSizesList;

	protected static final CharSequence[]			RATIO_STRINGS
													= { " ", "4:3", "3:2", "16:9", "1:1" };

	// States of focus and capture
	public static final int							FOCUS_STATE_IDLE				= 0;
	public static final int							FOCUS_STATE_FOCUSED				= 1;
	public static final int							FOCUS_STATE_FAIL				= 3;
	public static final int							FOCUS_STATE_FOCUSING			= 4;

	public static final int							CAPTURE_STATE_IDLE				= 0;
	public static final int							CAPTURE_STATE_CAPTURING			= 1;

	private static int								mFocusState						= FOCUS_STATE_IDLE;
	private static int								mCaptureState					= CAPTURE_STATE_IDLE;

	protected static int							iCaptureID						= -1;
	protected static Surface						mPreviewSurface					= null;

	private static final Object						SYNC_OBJECT						= new Object();
	
	protected static boolean 						appStarted							= false;

	// Singleton access function
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

	public static void onCreate(Context context, ApplicationInterface app, PluginManagerInterface pluginManagerBase)
	{
		pluginManager = pluginManagerBase;
		appInterface = app;
		mainContext = context;

		messageHandler = new Handler(CameraController.getInstance());
		pauseHandler = new Handler(CameraController.getInstance());
		
		appStarted = false;
		
		isVideoModeLaunched = false;

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
		mode_scene = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.SCENE_MODE_AUTO, sceneAuto);
				put(CameraParameters.SCENE_MODE_ACTION, sceneAction);
				put(CameraParameters.SCENE_MODE_PORTRAIT, scenePortrait);
				put(CameraParameters.SCENE_MODE_LANDSCAPE, sceneLandscape);
				put(CameraParameters.SCENE_MODE_NIGHT, sceneNight);
				put(CameraParameters.SCENE_MODE_NIGHT_PORTRAIT, sceneNightPortrait);
				put(CameraParameters.SCENE_MODE_THEATRE, sceneTheatre);
				put(CameraParameters.SCENE_MODE_BEACH, sceneBeach);
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

		key_scene = new HashMap<String, Integer>()
		{
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

		mode_wb = new HashMap<Integer, String>()
		{
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

		key_wb = new HashMap<String, Integer>()
		{
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

		mode_focus = new HashMap<Integer, String>()
		{
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

		key_focus = new HashMap<String, Integer>()
		{
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

		mode_flash = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.FLASH_MODE_OFF, flashOff);
				put(CameraParameters.FLASH_MODE_AUTO, flashAuto);
				put(CameraParameters.FLASH_MODE_SINGLE, flashOn);
				put(CameraParameters.FLASH_MODE_REDEYE, flashRedEye);
				put(CameraParameters.FLASH_MODE_TORCH, flashTorch);
			}
		};

		key_flash = new HashMap<String, Integer>()
		{
			{
				put(flashOff, CameraParameters.FLASH_MODE_OFF);
				put(flashAuto, CameraParameters.FLASH_MODE_AUTO);
				put(flashOn, CameraParameters.FLASH_MODE_SINGLE);
				put(flashRedEye, CameraParameters.FLASH_MODE_REDEYE);
				put(flashTorch, CameraParameters.FLASH_MODE_TORCH);
			}
		};

		iso_values = new ArrayList<Integer>()
		{
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

		iso_default = new ArrayList<String>()
		{
			{
				add(isoAuto);
				add(iso100);
				add(iso200);
				add(iso400);
				add(iso800);
				add(iso1600);
			}
		};

		iso_default_values = new HashMap<String, String>()
		{
			{
				put(isoAuto, mainContext.getResources().getString(R.string.isoAutoDefaultSystem));
				put(iso100, mainContext.getResources().getString(R.string.iso100DefaultSystem));
				put(iso200, mainContext.getResources().getString(R.string.iso200DefaultSystem));
				put(iso400, mainContext.getResources().getString(R.string.iso400DefaultSystem));
				put(iso800, mainContext.getResources().getString(R.string.iso800DefaultSystem));
				put(iso1600, mainContext.getResources().getString(R.string.iso1600DefaultSystem));
			}
		};

		mode_iso = new HashMap<Integer, String>()
		{
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

		mode_iso2 = new HashMap<Integer, String>()
		{
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

		mode_iso_HALv3 = new HashMap<Integer, Integer>()
		{
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

		key_iso = new HashMap<String, Integer>()
		{
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

		key_iso2 = new HashMap<String, Integer>()
		{
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
		String modeID = PluginManager.getInstance().getActiveModeID();
		if (modeID.equals("video"))
			isHALv3 = false;
//		Boolean isNexus = (Build.MODEL.contains("Nexus 5") || Build.MODEL.contains("Nexus 7"));
		try
		{
			if (!(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT && mainContext
					.getSystemService("camera") != null))
			{
				isHALv3 = false;
				isHALv3Supported = false;
				prefs.edit().putBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), false)
						.commit();
			} else
				isHALv3Supported = true;
		} catch (Exception e)
		{
			e.printStackTrace();
			isHALv3 = false;
			isHALv3Supported = false;
			prefs.edit().putBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), false)
					.commit();
		}

		if (CameraController.isHALv3Supported)
		{
			HALv3.onCreateHALv3();
			if(!HALv3.checkHardwareLevel())
			{
				isHALv3 = false;
				isHALv3Supported = false;
				prefs.edit().putBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), false)
						.commit();
			}
		}
		
		
	}

	public static void createHALv3Manager()
	{
		if (CameraController.isHALv3Supported)
			HALv3.onCreateHALv3();
	}

	public static void onStart()
	{
		// Does nothing yet
	}

	public static void onResume()
	{
		String modeID = PluginManager.getInstance().getActiveModeID();
		if (modeID.equals("hdrmode") || modeID.equals("expobracketing"))
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
			if (true == prefs.contains(MainScreen.sExpoPreviewModePref)) 
	        {
	        	previewMode = prefs.getBoolean(MainScreen.sExpoPreviewModePref, true);
	        }
	        else
	        	previewMode = true;
	        
			evLatency=0;
	        previewWorking=false;
	        if (cdt != null)
			{
				cdt.cancel();
				cdt = null;
			}
		}
        
        total_frames = 0;
        
        if (CameraController.isHALv3Supported)
			HALv3.onResumeHALv3();
	}

	public static void onPause(boolean isModeSwitching)
	{
		String modeID = PluginManager.getInstance().getActiveModeID();
		if (modeID.equals("hdrmode") || modeID.equals("expobracketing"))
		{
			evLatency=0;
	        previewWorking=false;
	        if (cdt != null)
			{
				cdt.cancel();
				cdt = null;
			}
		}
        
        total_frames = 0;
        
		// reset torch
		if (!CameraController.isHALv3)
		{
			try
			{
				Camera.Parameters p = cameraController.getCameraParameters();
				if (p != null && cameraController.isFlashModeSupported())
				{
					p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					cameraController.setCameraParameters(p);
				}
			} catch (Exception e)
			{
				e.printStackTrace();
			}

			if (camera != null)
			{
				camera.setPreviewCallback(null);
				if (!isModeSwitching)
				{
					camera.stopPreview();
					camera.release();
					camera = null;
				}
			}
		} else
			HALv3.onPauseHALv3();
	}

	public static void onStop()
	{
		if(needRelaunch)
		{
			SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit();
			prefEditor.putBoolean(MainScreen.getMainContext().getResources().getString(R.string.Preference_UseHALv3Key), true).commit();
		}
	}

	public static void onDestroy()
	{
		// Does nothing yet
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

	public static void setPreviewSurface(Surface srf)
	{
		mPreviewSurface = srf;
	}

	/* Preview buffer methods */
	public static void allocatePreviewBuffer(int size)
	{
		pviewBuffer = new byte[size];
	}

	public static byte[] getPreviewBuffer()
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
	
	public static void needCameraRelaunch(boolean relaunch)
	{
		needRelaunch = relaunch;
	}
	
	public static boolean isCameraRelaunch()
	{
		return needRelaunch;
	}
	
	
	public static boolean isSuperModePossible()
	{
		boolean SuperModeOk = false;
		
		if (isHALv3Supported)
		{
			// if we're working via Camera2 API -
			// check if device conform to Super Mode requirements
			
			/*
			boolean nroffAvailable = false;
			int nrmodes[] = camCharacter.get(CameraCharacteristics.NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES);
			if (nrmodes != null)
				for (int i=0; i<nrmodes.length; ++i)
					if (nrmodes[i] == CameraMetadata.NOISE_REDUCTION_MODE_OFF)
						nroffAvailable = true;
			
			if ( ( (camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) ||
				  ((camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) &&
				   (camCharacter.get(CameraCharacteristics.SYNC_MAX_LATENCY) == CameraMetadata.SYNC_MAX_LATENCY_PER_FRAME_CONTROL)) ) &&
				 nroffAvailable
				)
				SuperModeOk = true;
			*/
			
			// hard-code to enable Nexus 5 only, as we have no profiles for other models at the moment
			if (CameraController.isNexus())
				SuperModeOk = true;
		}
		
		return SuperModeOk;
	}

	public static boolean isUseSuperMode()
	{
		return (isSuperModePossible() && isHALv3) || isVideoModeLaunched;
	}
	
	public static boolean isNexus()
	{
		return Build.MODEL.contains("Nexus 5");
	}

	public static boolean isHALv3Supported()
	{
		return isHALv3Supported;
	}
	
	public static boolean isRAWCaptureSupported()
	{
		return isRAWCaptureSupported;
	}

	public static void setupCamera(SurfaceHolder holder)
	{
		if (!CameraController.isHALv3)
		{
			if (camera == null || MainScreen.getInstance().getSwitchingMode())
			{
				try
				{
					if (!MainScreen.getInstance().getSwitchingMode())
					{
						if (Camera.getNumberOfCameras() > 0)
							camera = Camera.open(CameraIndex);
						else
							camera = Camera.open();
					}
					MainScreen.getInstance().switchingMode(false);

					Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
					Camera.getCameraInfo(CameraIndex, cameraInfo);
					if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
						CameraMirrored = true;
					else
						CameraMirrored = false;

				} catch (RuntimeException e)
				{
					Log.e(TAG, "Unable to open camera");
					camera = null;
				}

				if (camera == null)
				{
					Toast.makeText(mainContext, "Unable to start camera", Toast.LENGTH_LONG).show();
					return;
				}
			}

			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH)
				cameraController.mVideoStabilizationSupported = getVideoStabilizationSupported();

			// screen rotation
			if (!pluginManager.shouldPreviewToGPU())
			{
				try
				{
					camera.setDisplayOrientation(90);
				} catch (RuntimeException e)
				{
					Log.e(TAG, "Unable to set display orientation for camera");
					e.printStackTrace();
				}

				try
				{
					camera.setPreviewDisplay(holder);
				} catch (IOException e)
				{
					Log.e(TAG, "Unable to set preview display for camera");
					e.printStackTrace();
				}
			}
		} else
			HALv3.openCameraHALv3();

		pluginManager.selectDefaults();

		if (!CameraController.isHALv3)
		{
			// screen rotation
			try
			{
				camera.setDisplayOrientation(90);
			} catch (RuntimeException e)
			{
				Log.e(TAG, "Unable to set display orientation for camera");
				e.printStackTrace();
			}

			try
			{
				camera.setPreviewDisplay(holder);
			} catch (IOException e)
			{
				Log.e(TAG, "Unable to set preview display for camera");
				e.printStackTrace();
			}
		}

		CameraController.fillPreviewSizeList();
		CameraController.fillPictureSizeList();

		if (CameraController.isHALv3)
		{
			HALv3.populateCameraDimensionsHALv3();
			HALv3.populateCameraDimensionsForMultishotsHALv3();
		} else
		{
			populateCameraDimensions();
			populateCameraDimensionsForMultishots();
		}

		pluginManager.selectImageDimension(); // updates SX, SY values

		// if (CameraController.isHALv3)
		// HALv3.setupImageReadersHALv3();

		if (!CameraController.isHALv3)
		{
			Message msg = new Message();
			msg.what = PluginManager.MSG_CAMERA_READY;
			MainScreen.getMessageHandler().sendMessage(msg);
		}
	}

	public static boolean isCameraCreated()
	{
		if (!CameraController.isHALv3)
			return camera != null;
		else
			return isCameraCreatedHALv3();

	}
	
	@TargetApi(21)
	public static void setCaptureFormat(int captureFormat)
	{
		HALv3.setCaptureFormat(captureFormat);
	}
	
	@TargetApi(21)
	public static void createCaptureSession(List<Surface> sfl)
	{
		HALv3.createCaptureSession(sfl);
	}

	@TargetApi(21)
	public static boolean isCameraCreatedHALv3()
	{
		return HALv3.getInstance().camDevice != null;
	}

	private static void fillPreviewSizeList()
	{
		CameraController.SupportedPreviewSizesList = new ArrayList<CameraController.Size>();
		if (!isHALv3)
		{
			if(camera != null && camera.getParameters() != null)
			{
				List<Camera.Size> list = camera.getParameters().getSupportedPreviewSizes();
				for (Camera.Size sz : list)
					CameraController.SupportedPreviewSizesList.add(new CameraController.Size(sz.width,
							sz.height));
			}
		} else
			CameraController.SupportedPreviewSizesList = HALv3.fillPreviewSizeList();
	}

	private static void fillPictureSizeList()
	{
		CameraController.SupportedPictureSizesList = new ArrayList<CameraController.Size>();
		if (!isHALv3)
		{
			if(camera != null && camera.getParameters() != null)
			{
				List<Camera.Size> list = camera.getParameters().getSupportedPictureSizes();
				for (Camera.Size sz : list)
					CameraController.SupportedPictureSizesList.add(new CameraController.Size(sz.width,
							sz.height));
			}
		} else
			HALv3.fillPictureSizeList(CameraController.SupportedPictureSizesList);
	}

	public static void populateCameraDimensions()
	{
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();
		CameraController.FastIdxelist = new ArrayList<Integer>();

		int minMPIX = CameraController.MIN_MPIX_SUPPORTED;
		Camera.Parameters cp = getCameraParameters();
		List<Camera.Size> cs = cp.getSupportedPictureSizes();

		if (cs == null)
			return;

		if (Build.MODEL.contains("HTC One X"))
		{
			if (!CameraController.isFrontCamera())
			{
				Camera.Size additional = null;
				additional = CameraController.getCamera().new Size(3264, 2448);
				cs.add(additional);
			}
		}
		
		int iHighestIndex = 0;
		Camera.Size sHighest = cs.get(0);

		for (int ii = 0; ii < cs.size(); ++ii)
		{
			Camera.Size s = cs.get(ii);

			int currSizeWidth = s.width;
			int currSizeHeight = s.height;
			int highestSizeWidth = sHighest.width;
			int highestSizeHeight = sHighest.height;

			if (Build.MODEL.contains("GT-I9190") && isFrontCamera() && (currSizeWidth * currSizeHeight == 1920 * 1080))
				continue;

			if ((long) currSizeWidth * currSizeHeight > (long) highestSizeWidth * highestSizeHeight)
			{
				sHighest = s;
				iHighestIndex = ii;
			}

			if ((long) currSizeWidth * currSizeHeight < minMPIX)
				continue;

			fillResolutionsList(ii, currSizeWidth, currSizeHeight);
		}

		if (CameraController.ResolutionsNamesList.isEmpty())
		{
			Camera.Size s = cs.get(iHighestIndex);

			int currSizeWidth = s.width;
			int currSizeHeight = s.height;

			fillResolutionsList(0, currSizeWidth, currSizeHeight);
		}

		return;
	}

	protected static void fillResolutionsList(int ii, int currSizeWidth, int currSizeHeight)
	{
		boolean needAdd = true;
		boolean isFast = false;

		Long lmpix = (long) currSizeWidth * currSizeHeight;
		float mpix = (float) lmpix / 1000000.f;
		float ratio = (float) currSizeWidth / currSizeHeight;

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

		for (int i = 0; i < CameraController.SupportedPreviewSizesList.size(); i++)
		{
			if (currSizeWidth == CameraController.SupportedPreviewSizesList.get(i).getWidth()
					&& currSizeHeight == CameraController.SupportedPreviewSizesList.get(i).getHeight())
			{
				isFast = true;
			}
		}

		String newName;
		if (isFast)
		{
			newName = String.format("%3.1f Mpix  " + RATIO_STRINGS[ri] + " (fast)", mpix);
		} else
		{
			newName = String.format("%3.1f Mpix  " + RATIO_STRINGS[ri], mpix);
		}

		for (int i = 0; i < CameraController.ResolutionsNamesList.size(); i++)
		{
			if (newName.equals(CameraController.ResolutionsNamesList.get(i)))
			{
				Long lmpixInArray = (long) (CameraController.ResolutionsSizeList.get(i).getWidth() * CameraController.ResolutionsSizeList
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
			CameraController.ResolutionsNamesList.add(loc, newName);
			CameraController.ResolutionsIdxesList.add(loc, String.format("%d", ii));
			CameraController.ResolutionsMPixList.add(loc, lmpix);
			CameraController.ResolutionsSizeList.add(loc, new CameraController.Size(currSizeWidth,
					currSizeHeight));
		}
	}

	public static void populateCameraDimensionsForMultishots()
	{
		CameraController.MultishotResolutionsMPixList = new ArrayList<Long>(CameraController.ResolutionsMPixList);
		CameraController.MultishotResolutionsSizeList = new ArrayList<CameraController.Size>(
				CameraController.ResolutionsSizeList);
		CameraController.MultishotResolutionsIdxesList = new ArrayList<String>(CameraController.ResolutionsIdxesList);
		CameraController.MultishotResolutionsNamesList = new ArrayList<String>(CameraController.ResolutionsNamesList);

		if (CameraController.SupportedPreviewSizesList != null && CameraController.SupportedPreviewSizesList.size() > 0)
		{
			fillResolutionsListMultishot(MultishotResolutionsIdxesList.size(),
					CameraController.SupportedPreviewSizesList.get(0).getWidth(),
					CameraController.SupportedPreviewSizesList.get(0).getHeight());
		}

		if (CameraController.SupportedPreviewSizesList != null && CameraController.SupportedPreviewSizesList.size() > 1)
		{
			fillResolutionsListMultishot(MultishotResolutionsIdxesList.size(),
					CameraController.SupportedPreviewSizesList.get(1).getWidth(),
					CameraController.SupportedPreviewSizesList.get(1).getHeight());
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		String prefIdx = prefs.getString(MainScreen.sImageSizeMultishotBackPref, "-1");

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
						maxFastIdx = CameraController.FastIdxelist.get(i);
					}
				}
			}
			if (CameraController.SupportedPreviewSizesList != null
					&& CameraController.SupportedPreviewSizesList.size() > 0 && maxMpx >= MPIX_1080)
			{
				SharedPreferences.Editor prefEditor = prefs.edit();
				prefEditor.putString(MainScreen.sImageSizeMultishotBackPref, String.valueOf(maxFastIdx));
				prefEditor.commit();
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
		if (Math.abs(ratio - 4 / 3.f) < 0.12f)
			ri = 1;
		if (Math.abs(ratio - 3 / 2.f) < 0.12f)
			ri = 2;
		if (Math.abs(ratio - 16 / 9.f) < 0.15f)
			ri = 3;
		if (Math.abs(ratio - 1) == 0)
			ri = 4;

		if (mpix < 0.1f) {
			mpix = 0.1f;
		}
		
		String newName;
		if (isFast)
		{
			newName = String.format("%3.1f Mpix  " + RATIO_STRINGS[ri] + " (fast)", mpix);
		} else
		{
			newName = String.format("%3.1f Mpix  " + RATIO_STRINGS[ri], mpix);
		}

		for (int i = 0; i < CameraController.MultishotResolutionsNamesList.size(); i++)
		{
			if (newName.equals(CameraController.MultishotResolutionsNamesList.get(i)))
			{
				Long lmpixInArray = (long) (CameraController.MultishotResolutionsSizeList.get(i).getWidth() * MultishotResolutionsSizeList
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
			CameraController.MultishotResolutionsSizeList.add(loc, new CameraController.Size(
					currSizeWidth, currSizeHeight));
		}
	}

	public static List<CameraController.Size> getSupportedPreviewSizes()
	{
		List<CameraController.Size> previewSizes = new ArrayList<CameraController.Size>();
		if (!CameraController.isHALv3)
		{
			if (CameraController.SupportedPreviewSizesList != null)
			{
				List<CameraController.Size> sizes = SupportedPreviewSizesList;
				for (CameraController.Size sz : sizes)
					previewSizes.add(new CameraController.Size(sz.getWidth(), sz.getHeight()));
			} else
			{
				Log.d(TAG, "SupportedPreviewSizesList == null");
			}

			return previewSizes;

		} else
			return HALv3.fillPreviewSizeList();
	}

	public static void setCameraPreviewSize(CameraController.Size sz)
	{
		if (!CameraController.isHALv3)
		{
			Camera.Parameters params = getCameraParameters();
			if (params != null)
			{
				params.setPreviewSize(sz.mWidth, sz.mHeight);
				setCameraParameters(params);
			}
		} else
		{
			HALv3.setupImageReadersHALv3(sz);
		}
	}
	
	public static void setSurfaceHolderFixedSize(int width, int height)
	{
		if (CameraController.isHALv3)
		{
			MainScreen.setSurfaceHolderSize(width, height);
		}
	}

	public static List<CameraController.Size> getSupportedPictureSizes()
	{
		List<CameraController.Size> pictureSizes = new ArrayList<CameraController.Size>();
		if (!CameraController.isHALv3)
		{
			if (CameraController.SupportedPictureSizesList != null)
			{
				pictureSizes = new ArrayList<CameraController.Size>(CameraController.SupportedPictureSizesList);
			} else if (camera != null && camera.getParameters() != null)
			{
				List<Camera.Size> sizes = camera.getParameters().getSupportedPictureSizes();
				for (Camera.Size sz : sizes)
					pictureSizes.add(new CameraController.Size(sz.width, sz.height));
			} else
			{
				Log.d(TAG, "camera == null");
			}
		} else
			HALv3.fillPictureSizeList(pictureSizes);

		return pictureSizes;
	}

	public static List<CameraController.Size> getResolutionsSizeList()
	{
		return CameraController.ResolutionsSizeList;
	}

	public static List<String> getResolutionsIdxesList()
	{
		return CameraController.ResolutionsIdxesList;
	}

	public static List<String> getResolutionsNamesList()
	{
		return CameraController.ResolutionsNamesList;
	}

	public static int getNumberOfCameras()
	{
		if (!CameraController.isHALv3)
			return Camera.getNumberOfCameras();
		else
			return CameraController.cameraIdList.length;
	}

	public static void updateCameraFeatures()
	{
		if (camera != null)
			cameraParameters = camera.getParameters();

		mEVSupported = getExposureCompensationSupported();
		mSceneModeSupported = getSceneModeSupported();
		mWBSupported = getWhiteBalanceSupported();
		mFocusModeSupported = getFocusModeSupported();
		mFlashModeSupported = getFlashModeSupported();
		mISOSupported = getISOSupported();

		if (!CameraController.isHALv3)
		{
			if (camera != null && cameraParameters != null)
			{
				minExpoCompensation = cameraParameters.getMinExposureCompensation();
				maxExpoCompensation = cameraParameters.getMaxExposureCompensation();
				expoCompensationStep = cameraParameters.getExposureCompensationStep();
			}
		} else
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

		maxRegionsSupported = CameraController.getMaxNumFocusAreas();

		cameraParameters = null;
	}

	@Override
	public void onError(int arg0, Camera arg1)
	{
		// Not used
	}

	// ------------ CAMERA PARAMETERS AND CAPABILITIES
	// SECTION-------------------------------------------
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
	public static boolean isFrontCamera()
	{
		return CameraMirrored;
	}

	public static Camera getCamera()
	{
		return camera;
	}

	public static void setCamera(Camera cam)
	{
		camera = cam;
	}

	public static Camera.Parameters getCameraParameters()
	{
		try
		{
		if (camera != null)
			return camera.getParameters();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		return null;
	}

	public static boolean setCameraParameters(Camera.Parameters params)
	{
		if (params != null && camera != null)
		{
			try
			{
				camera.setParameters(params);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e(TAG, "setCameraParameters exception: " + e.getMessage());
				return false;
			}

			return true;
		}

		return false;
	}
	
	@TargetApi(21)
	public static CameraCharacteristics getCameraCharacteristics()
	{
		return HALv3.getCameraParameters2();
	}

	public static void startCameraPreview()
	{
		if (camera != null) {
			camera.startPreview();

			if (Build.MODEL.equals("Nexus 4")) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
				int initValue = prefs.getInt(MainScreen.sEvPref, 0);
				CameraController.setCameraExposureCompensation(initValue);
			}
		}
		
	}		

	public static void stopCameraPreview()
	{
		if (camera != null)
			camera.stopPreview();
	}

	public static void lockCamera()
	{
		if (camera != null)
			camera.lock();
	}

	public static void unlockCamera()
	{
		if (camera != null)
			camera.unlock();
	}

	@TargetApi(15)
	public static void setVideoStabilization(boolean stabilization)
	{
		if (camera != null && camera.getParameters() != null
				&& camera.getParameters().isVideoStabilizationSupported())
		{
			camera.getParameters().setVideoStabilization(stabilization);
			setCameraParameters(camera.getParameters());
		}
	}

	@TargetApi(15)
	public static boolean getVideoStabilizationSupported()
	{
		if (camera != null && camera.getParameters() != null)
			return camera.getParameters().isVideoStabilizationSupported();

		return false;
	}

	public static boolean isVideoStabilizationSupported()
	{
		return mVideoStabilizationSupported;
	}

	public static boolean isExposureLockSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (camera == null || ( camera != null && camera.getParameters() == null))
				return false;

			return camera.getParameters().isAutoExposureLockSupported();
		} else
			return true;
	}

	public static boolean isExposureLock()
	{
		if (!CameraController.isHALv3)
		{
			if (camera == null || ( camera != null && camera.getParameters() == null))
				return false;

			return camera.getParameters().getAutoExposureLock();
		} else
			return true;
	}

	public static boolean isWhiteBalanceLockSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (camera == null || ( camera != null && camera.getParameters() == null))
				return false;

			return camera.getParameters().isAutoWhiteBalanceLockSupported();
		} else
			return false;
	}

	public static boolean isWhiteBalanceLock()
	{
		if (!CameraController.isHALv3)
		{
			if (camera == null || ( camera != null && camera.getParameters() == null))
				return false;

			return camera.getParameters().getAutoWhiteBalanceLock();
		} else
			return false;
	}

	public static boolean isZoomSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (null == camera || camera.getParameters() == null)
				return false;
			
			return camera.getParameters().isZoomSupported();
		} else
		{
			return HALv3.isZoomSupportedHALv3();
		}
	}

	public static int getMaxZoom()
	{
		if (!CameraController.isHALv3)
		{
			if (null == camera || camera.getParameters() == null)
				return 1;

			return camera.getParameters().getMaxZoom();
		} else
		{
			float maxZoom = HALv3.getMaxZoomHALv3();
			return (int) (maxZoom - 10.0f);
		}
	}

	public static void setZoom(int value)
	{
		if (!CameraController.isHALv3)
		{
			Camera.Parameters cp = getCameraParameters();
			if (cp != null)
			{
				cp.setZoom(value);
				setCameraParameters(cp);
			}
		} else
			HALv3.setZoom(value / 10.0f + 1f);
	}

	// Note: getZoom returns zoom in floating point,
	// unlike old android camera API which returns it multiplied by 10 
	public static float getZoom()
	{
		if (!CameraController.isHALv3)
		{
			Camera.Parameters cp = getCameraParameters();
			return (cp.getZoom() / 10.0f + 1f);
		} else
			return HALv3.getZoom();
	}
	
	public static boolean isLumaAdaptationSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (null == camera || camera.getParameters() == null)
				return false;
			Camera.Parameters cp = CameraController.getCameraParameters();

			String luma = cp.get("luma-adaptation");
			return luma != null;
		} else
		{
			return false;
		}
	}

	// Used to initialize internal variable
	private static boolean getExposureCompensationSupported()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				if (cameraParameters != null)
				{
					return cameraParameters.getMinExposureCompensation() != 0
							&& cameraParameters.getMaxExposureCompensation() != 0;
				} else
				{
					return camera.getParameters().getMinExposureCompensation() != 0
							&& camera.getParameters().getMaxExposureCompensation() != 0;
				}

			} else
				return false;
		} else
			return HALv3.isExposureCompensationSupportedHALv3();
	}

	// Used by CameraController class users.
	public static boolean isExposureCompensationSupported()
	{
		return mEVSupported;
	}

	public static int getMinExposureCompensation()
	{
		return minExpoCompensation;
	}

	public static int getMaxExposureCompensation()
	{
		return maxExpoCompensation;
	}

	public static float getExposureCompensationStep()
	{
		return expoCompensationStep;
	}

	public static float getExposureCompensation()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null && camera.getParameters() != null)
			{
				Camera.Parameters cameraParameters = CameraController.getCamera().getParameters();

				return cameraParameters.getExposureCompensation()
						* cameraParameters.getExposureCompensationStep();
			}
			else
				return 0;
		}
		else
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
			return prefs.getInt(MainScreen.sEvPref, 0);
		}
	}

	public static void resetExposureCompensation()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null && camera.getParameters() != null)
			{
				if (!isExposureCompensationSupported())
					return;
				Camera.Parameters params = camera.getParameters();
				params.setExposureCompensation(0);
				setCameraParameters(params);
			}
		} else
			HALv3.resetExposureCompensationHALv3();
	}

	private static boolean getSceneModeSupported()
	{
		int[] supported_scene = getSupportedSceneModesInternal();
		return supported_scene != null && supported_scene.length > 0
				&& supported_scene[0] != CameraParameters.SCENE_MODE_AUTO;
	}

	public static boolean isSceneModeSupported()
	{
		return mSceneModeSupported;
	}

	private static int[] getSupportedSceneModesInternal()
	{
		if (!CameraController.isHALv3)
		{
			List<String> sceneModes = null;
			if (cameraParameters != null)
			{
				sceneModes = cameraParameters.getSupportedSceneModes();
			} else if(camera != null)
			{
				sceneModes = camera.getParameters().getSupportedSceneModes();
			}

			if (camera != null && sceneModes != null)
			{
				Set<String> known_scenes = CameraController.key_scene.keySet();
				sceneModes.retainAll(known_scenes);
				int[] scenes = new int[sceneModes.size()];
				for (int i = 0; i < sceneModes.size(); i++)
				{
					String mode = sceneModes.get(i);
					if (CameraController.key_scene.containsKey(mode))
						scenes[i] = CameraController.key_scene.get(mode).byteValue();
				}

				return scenes;
			}

			return new int[0];
		} else
			return HALv3.getSupportedSceneModesHALv3();
	}

	public static int[] getSupportedSceneModes()
	{
		return supportedSceneModes;
	}

	private static boolean getWhiteBalanceSupported()
	{
		int[] supported_wb = getSupportedWhiteBalanceInternal();
		return supported_wb != null && supported_wb.length > 0;
	}

	public static boolean isWhiteBalanceSupported()
	{
		return mWBSupported;
	}

	private static int[] getSupportedWhiteBalanceInternal()
	{
		if (!CameraController.isHALv3)
		{
			List<String> wbModes;
			if (cameraParameters != null)
			{
				wbModes = cameraParameters.getSupportedWhiteBalance();
			} else
			{
				wbModes = camera.getParameters().getSupportedWhiteBalance();
			}
			
			if (camera != null && wbModes != null)
			{
				Set<String> known_wb = CameraController.key_wb.keySet();
				wbModes.retainAll(known_wb);
				int[] wb = new int[wbModes.size()];
				for (int i = 0; i < wbModes.size(); i++)
				{
					String mode = wbModes.get(i);
					if (CameraController.key_wb.containsKey(mode))
						wb[i] = CameraController.key_wb.get(mode).byteValue();
				}
				return wb;
			}

			return new int[0];
		} else
			return HALv3.getSupportedWhiteBalanceHALv3();
	}

	public static int[] getSupportedWhiteBalance()
	{
		return supportedWBModes;
	}

	private static boolean getFocusModeSupported()
	{
		int[] supported_focus = getSupportedFocusModesInternal();
		return supported_focus != null && supported_focus.length > 0;
	}

	public static boolean isFocusModeSupported()
	{
		return mFocusModeSupported;
	}

	private static int[] getSupportedFocusModesInternal()
	{
		if (!CameraController.isHALv3)
		{
			List<String> focusModes;
			if (cameraParameters != null)
			{
				focusModes = cameraParameters.getSupportedFocusModes();
			} else
			{
				focusModes = camera.getParameters().getSupportedFocusModes();
			}
			
			if (camera != null && focusModes != null)
			{
				Set<String> known_focus = CameraController.key_focus.keySet();
				focusModes.retainAll(known_focus);
				int[] focus = new int[focusModes.size()];
				for (int i = 0; i < focusModes.size(); i++)
				{
					String mode = focusModes.get(i);
					if (CameraController.key_focus.containsKey(mode))
						focus[i] = CameraController.key_focus.get(mode).byteValue();
				}

				return focus;
			}

			return new int[0];
		} else
			return HALv3.getSupportedFocusModesHALv3();
	}

	public static int[] getSupportedFocusModes()
	{
		return supportedFocusModes;
	}

	private static boolean getFlashModeSupported()
	{
		if (CameraController.isHALv3)
			return HALv3.isFlashModeSupportedHALv3();
		else
		{
			int[] supported_flash = getSupportedFlashModesInternal();
			return supported_flash != null && supported_flash.length > 0;
		}
	}

	public static boolean isFlashModeSupported()
	{
		return mFlashModeSupported;
	}

	private static int[] getSupportedFlashModesInternal()
	{
		if (CameraController.isHALv3)
		{
			if (isFlashModeSupported())
			{
				int[] flash = new int[3];
				flash[0] = CameraParameters.FLASH_MODE_OFF;
				flash[1] = CameraParameters.FLASH_MODE_SINGLE;
				flash[2] = CameraParameters.FLASH_MODE_TORCH;
				return flash;
			}
		} else
		{
			
			List<String> flashModes = null;
			if (cameraParameters != null)
			{
				flashModes = cameraParameters.getSupportedFlashModes();
			} else
			{
				flashModes = camera.getParameters().getSupportedFlashModes();
			}
			
			if (camera != null && flashModes != null)
			{
				Set<String> known_flash = CameraController.key_flash.keySet();
				flashModes.retainAll(known_flash);
				int[] flash = new int[flashModes.size()];
				for (int i = 0; i < flashModes.size(); i++)
				{
					String mode = flashModes.get(i);
					if (CameraController.key_flash.containsKey(mode))
						flash[i] = CameraController.key_flash.get(flashModes.get(i)).byteValue();
				}

				return flash;
			}
		}

		return new int[0];
	}

	public static int[] getSupportedFlashModes()
	{
		return supportedFlashModes;
	}

	private static boolean getISOSupported()
	{
		if (!CameraController.isHALv3)
		{
			int[] supported_iso = getSupportedISOInternal();
			String isoSystem = CameraController.getCameraParameters().get("iso");
			String isoSystem2 = CameraController.getCameraParameters().get("iso-speed");
			return supported_iso.length > 0 || isoSystem != null || isoSystem2 != null;
		} else
			return HALv3.isISOModeSupportedHALv3();
	}

	public static boolean isISOSupported()
	{
		return mISOSupported;
	}

	private static int[] getSupportedISOInternal()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				List<String> isoModes = null;
				Camera.Parameters camParams = CameraController.getCameraParameters();
				String supportedIsoValues = camParams.get("iso-values");
				String supportedIsoValues2 = camParams.get("iso-speed-values");
				String supportedIsoValues3 = camParams.get("iso-mode-values");
				String supportedIsoValues4 = camParams.get("nv-picture-iso-values");

				String delims = "[,]+";
				String[] isoList = null;

				if (supportedIsoValues != null && !supportedIsoValues.equals(""))
					isoList = supportedIsoValues.split(delims);
				else if (supportedIsoValues2 != null && !supportedIsoValues2.equals(""))
					isoList = supportedIsoValues2.split(delims);
				else if (supportedIsoValues3 != null && !supportedIsoValues3.equals(""))
					isoList = supportedIsoValues3.split(delims);
				else if (supportedIsoValues4 != null && !supportedIsoValues4.equals(""))
					isoList = supportedIsoValues4.split(delims);

				if (isoList != null)
				{
					isoModes = new ArrayList<String>();
					for (int i = 0; i < isoList.length; i++)
						isoModes.add(isoList[i]);
				} else
					return new int[0];

				int supportedISOCount = 0;
				for (int i = 0; i < isoModes.size(); i++)
				{
					String mode = isoModes.get(i);
					if (CameraController.key_iso.containsKey(mode))
						supportedISOCount++;
					else if (CameraController.key_iso2.containsKey(mode))
						supportedISOCount++;
				}

				int[] iso = new int[supportedISOCount];
				for (int i = 0, index = 0; i < isoModes.size(); i++)
				{
					String mode = isoModes.get(i);
					if (CameraController.key_iso.containsKey(mode))
						iso[index++] = CameraController.key_iso.get(isoModes.get(i)).byteValue();
					else if (CameraController.key_iso2.containsKey(mode))
						iso[index++] = CameraController.key_iso2.get(isoModes.get(i)).byteValue();
				}

				return iso;
			}

			return new int[0];
		} else
			return HALv3.getSupportedISOModesHALv3();
	}

	public static int[] getSupportedISO()
	{
		return supportedISOModes;
	}

	public static int getMaxNumMeteringAreas()
	{
		if (CameraController.isHALv3)
			return HALv3.getMaxNumMeteringAreasHALv3();
		else if (camera != null)
		{
			Camera.Parameters camParams = camera.getParameters();
			return camParams.getMaxNumMeteringAreas();
		}

		return 0;
	}

	private static int getMaxNumFocusAreas()
	{
		if (CameraController.isHALv3)
			return HALv3.getMaxNumFocusAreasHALv3();
		else if (camera != null)
		{
			Camera.Parameters camParams = camera.getParameters();
			return camParams.getMaxNumFocusAreas();
		}

		return 0;
	}

	public static int getMaxAreasSupported()
	{
		return maxRegionsSupported;
	}

	public static int getCameraIndex()
	{
		return CameraIndex;
	}

	public static void setCameraIndex(int index)
	{
		CameraIndex = index;
	}

	public static int getCameraImageSizeIndex()
	{
		return CapIdx;
	}

	public static void setCameraImageSizeIndex(int captureIndex, boolean init)
	{
		CapIdx = captureIndex;
		if(init)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			prefs.edit().putString(CameraIndex == 0 ? MainScreen.sImageSizeRearPref
					: MainScreen.sImageSizeFrontPref, String.valueOf(captureIndex)).commit();
		}
	}
	
	public static void setCameraImageSize(Size imgSize)
	{
		imageSize = imgSize;
	}
	
	public static Size getCameraImageSize()
	{
		return imageSize;
	}
	
	public static Size getMaxCameraImageSize(int captureFormat)
	{
		if(!CameraController.isHALv3)
			return imageSize;
		else
			return HALv3.getMaxCameraImageSizeHALv3(captureFormat);
	}

	public static boolean isModeAvailable(int[] modeList, int mode)
	{
		boolean isAvailable = false;
		for (int currMode : modeList)
		{
			if (currMode == mode)
			{
				isAvailable = true;
				break;
			}
		}
		return isAvailable;
	}

	public static int getSceneMode()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
						return CameraController.key_scene.get(params.getSceneMode());
				} catch (Exception e)
				{
					e.printStackTrace();
					Log.e(TAG, "getSceneMode exception: " + e.getMessage());
				}
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(MainScreen.sSceneModePref, -1);

		return -1;
	}

	public static int getWBMode()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
						return CameraController.key_wb.get(params.getWhiteBalance());
				} catch (Exception e)
				{
					e.printStackTrace();
					Log.e(TAG, "getWBMode exception: " + e.getMessage());
				}
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(MainScreen.sWBModePref, -1);

		return -1;
	}

	public static int getFocusMode()
	{

		if (!CameraController.isHALv3)
		{
			try
			{
				if (camera != null)
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
						return CameraController.key_focus.get(params.getFocusMode());
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e(TAG, "getFocusMode exception: " + e.getMessage());
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(
					CameraMirrored ? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, -1);

		return -1;
	}

	public static int getFlashMode()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
						return CameraController.key_flash.get(params.getFlashMode());
				} catch (Exception e)
				{
					e.printStackTrace();
					Log.e(TAG, "getFlashMode exception: " + e.getMessage());
				}
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(MainScreen.sFlashModePref, -1);

		return -1;
	}

	public static int getISOMode()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				Camera.Parameters params = camera.getParameters();
				if (params != null)
				{
					String iso = null;
					iso = params.get("iso");
					if (iso == null)
						iso = params.get("iso-speed");

					if (CameraController.key_iso.containsKey(iso))
						return CameraController.key_iso.get(iso);
					else if (CameraController.key_iso2.containsKey(iso))
						return CameraController.key_iso2.get(iso);
				}
			}
		} else
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(MainScreen.sISOPref, -1);

		return -1;
	}

	public static void setCameraSceneMode(int mode)
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
					{
						params.setSceneMode(CameraController.mode_scene.get(mode));
						setCameraParameters(params);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		} else
			HALv3.setCameraSceneModeHALv3(mode);
	}

	public static void setCameraWhiteBalance(int mode)
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
					{
						params.setWhiteBalance(CameraController.mode_wb.get(mode));
						setCameraParameters(params);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		} else
			HALv3.setCameraWhiteBalanceHALv3(mode);
	}

	public static void setCameraFocusMode(int mode)
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
					{
						String focusmode = CameraController.mode_focus.get(mode);
						params.setFocusMode(focusmode);
						setCameraParameters(params);
						MainScreen.setAutoFocusLock(false);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		} else
			HALv3.setCameraFocusModeHALv3(mode);
	}

	public static void setCameraFlashMode(int mode)
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
					{
						String flashmode = CameraController.mode_flash.get(mode);
						params.setFlashMode(flashmode);
						setCameraParameters(params);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		} else
			HALv3.setCameraFlashModeHALv3(mode);
	}

	public static void setCameraISO(int mode)
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
					{
						if (params.get(CameraParameters.isoParam) != null)
							params.set(CameraParameters.isoParam, CameraController.mode_iso.get(mode));
						else if (params.get(CameraParameters.isoParam2) != null)
							params.set(CameraParameters.isoParam2, CameraController.mode_iso.get(mode));
						else if (params.get(CameraParameters.isoParam3) != null)
							params.set(CameraParameters.isoParam3, CameraController.mode_iso.get(mode));
						if (!setCameraParameters(params))
						{
							if (params.get(CameraParameters.isoParam) != null)
								params.set(CameraParameters.isoParam, CameraController.mode_iso2.get(mode));
							else if (params.get(CameraParameters.isoParam2) != null)
								params.set(CameraParameters.isoParam2, CameraController.mode_iso2.get(mode));
							else if (params.get(CameraParameters.isoParam3) != null)
								params.set(CameraParameters.isoParam3, CameraController.mode_iso2.get(mode));
							
							setCameraParameters(params);
						}
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		} else
			HALv3.setCameraISOModeHALv3(mode);
	}

	public static void setLumaAdaptation(int iEv)
	{
		try
		{
			Camera.Parameters params = CameraController.getCameraParameters();
			if (params != null)
			{
				params.set("luma-adaptation", iEv);
				setCameraParameters(params);
			}
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void setCameraExposureCompensation(int iEV)
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
					{
						params.setExposureCompensation(iEV);
						setCameraParameters(params);
					}
				}
				catch(Exception e)
				{
					e.printStackTrace();
				}
			}
		} else
			HALv3.setCameraExposureCompensationHALv3(iEV);
	}

	public static void setCameraFocusAreas(List<Area> focusAreas)
	{
		if (!CameraController.isHALv3)
		{
			if (CameraController.getCamera() != null)
			{
				try
				{
					Camera.Parameters params = CameraController.getCameraParameters();
					if (params != null)
					{
						params.setFocusAreas(focusAreas);
						cameraController.setCameraParameters(params);
					}
				} catch (RuntimeException e)
				{
					Log.e(TAG, e.getMessage());
				}
			}
		} else
			HALv3.setCameraFocusAreasHALv3(focusAreas);
	}

	public static void setCameraMeteringAreas(List<Area> meteringAreas)
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if (params != null)
					{
						if (meteringAreas != null)
						{
							params.setMeteringAreas(meteringAreas);
							cameraController.setCameraParameters(params);
						}
					}
				} catch (RuntimeException e)
				{
					Log.e(TAG, e.getMessage());
				}
			}
		} else
			HALv3.setCameraMeteringAreasHALv3(meteringAreas);
	}

	public static void setFocusState(int state)
	{
		if (state != CameraController.FOCUS_STATE_IDLE && state != CameraController.FOCUS_STATE_FOCUSED
				&& state != CameraController.FOCUS_STATE_FAIL)
			return;

		mFocusState = state;

		PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_FOCUS_STATE_CHANGED);
	}

	public static int getFocusState()
	{
		return mFocusState;
	}
	
	public static boolean isAutoFocusPerform()
	{
		int focusMode = CameraController.getFocusMode();
		if (focusMode != -1
				&& (CameraController.getFocusState() == CameraController.FOCUS_STATE_IDLE || CameraController
						.getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
				&& !(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
						|| focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
						|| focusMode == CameraParameters.AF_MODE_INFINITY
						|| focusMode == CameraParameters.AF_MODE_FIXED || focusMode == CameraParameters.AF_MODE_EDOF)
				&& !MainScreen.getAutoFocusLock())
			return true;
		else
			return false;
	}

	public static int getPreviewFrameRate()
	{
		if (!CameraController.isHALv3)
		{
			int[] range = { 0, 0 };
			camera.getParameters().getPreviewFpsRange(range);
			return range[1] / 1000;
		} else
			return HALv3.getPreviewFrameRateHALv3();
	}

	public static void setPictureSize(int width, int height)
	{
		final Camera.Parameters cp = getCameraParameters();
		if (cp == null)
		{
			return;
		}

		cp.setPictureSize(width, height);
		setCameraParameters(cp);
	}

	public static void setJpegQuality(int quality)
	{
		final Camera.Parameters cp = getCameraParameters();
		if (cp == null)
		{
			return;
		}

		cp.setJpegQuality(quality);
		setCameraParameters(cp);
	}

	public static float getHorizontalViewAngle()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
				return camera.getParameters().getHorizontalViewAngle();
		}
		else
		{
			return HALv3.getHorizontalViewAngle();
		}
		
		if (Build.MODEL.contains("Nexus"))
			return 59.63f;

		return 55.4f;
	}

	public static float getVerticalViewAngle()
	{
		if (!CameraController.isHALv3)
		{
			if (camera != null)
				return camera.getParameters().getVerticalViewAngle();
		}
		else
		{
			return HALv3.getVerticalViewAngle();
		}
			
		if (Build.MODEL.contains("Nexus"))
			return 46.66f;

		return 42.7f;
	}

	// ^^^^^^^^^^^ CAMERA PARAMETERS AND CAPABILITIES
	// SECTION---------------------------------------------

	// ------------ CAPTURE AND FOCUS FUNCTION ----------------------------

	// Experimental code to take multiple images. Works only with HALv3
	// interface in API 19(currently minimum API version for Android L increased
	// to 21)
	protected static int[]		pauseBetweenShots	= null;
	protected static int[]		evValues			= null;

	protected static int		total_frames;
	protected static int		frame_num;
	protected static int		frameFormat			= CameraController.JPEG;

	protected static boolean	takePreviewFrame	= false;

	protected static boolean	takeYUVFrame		= false;

	protected static boolean	resultInHeap		= false;

	// Note: per-frame 'gain' and 'exposure' parameters are only effective for Camera2 API at the moment
	public static int captureImagesWithParams(int nFrames, int format, int[] pause, int[] evRequested, int[] gain, long[] exposure, boolean resInHeap)
	{
		pauseBetweenShots = pause;
		evValues = evRequested;

		total_frames = nFrames;
		frame_num = 0;
		frameFormat = format;

		resultInHeap = resInHeap;
		
		previewWorking=false;
		cdt = null;

		if (!CameraController.isHALv3)
		{
			takeYUVFrame = (format == CameraController.YUV) || (format == CameraController.YUV_RAW);
			if (evRequested != null && evRequested.length >= total_frames)
				CameraController.sendMessage(MSG_SET_EXPOSURE);
			else
				CameraController.sendMessage(MSG_TAKE_IMAGE);
			return 0;
		} else
			return HALv3.captureImageWithParamsHALv3(nFrames, format, pause, evRequested, gain, exposure, resultInHeap);
	}

	public static boolean autoFocus(Camera.AutoFocusCallback listener)
	{
		synchronized (SYNC_OBJECT)
		{
			if (!CameraController.isHALv3)
			{
				if (CameraController.getCamera() != null
						&& CameraController.mCaptureState != CameraController.CAPTURE_STATE_CAPTURING)
				{
					CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
					try
					{
						CameraController.getCamera().autoFocus(listener);
					} catch (Exception e)
					{
						e.printStackTrace();
						Log.e(TAG, "autoFocus: " + e.getMessage());
						return false;
					}
					return true;
				}
			} else
				return HALv3.autoFocusHALv3();

			return false;
		}
	}

	public static boolean autoFocus()
	{
		synchronized (SYNC_OBJECT)
		{
			if (!CameraController.isHALv3)
			{
				if (CameraController.getCamera() != null)
				{
					if (CameraController.mCaptureState != CameraController.CAPTURE_STATE_CAPTURING)
					{
						CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
						try
						{
							CameraController.getCamera().autoFocus(CameraController.getInstance());
						} catch (Exception e)
						{
							e.printStackTrace();
							Log.e(TAG, "autoFocus: " + e.getMessage());
							return false;
						}
						return true;
					}
				}
			} else
				return HALv3.autoFocusHALv3();

			return false;
		}
	}

	public static void cancelAutoFocus()
	{
		CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);
		if (!CameraController.isHALv3)
		{
			if (CameraController.getCamera() != null)
			{
				try
				{
					camera.cancelAutoFocus();
				} catch (RuntimeException exp)
				{
					Log.e(TAG, "cancelAutoFocus failed. Message: " + exp.getMessage());
				}
			}
		} else
			HALv3.cancelAutoFocusHALv3();
	}

	// Callback always contains JPEG frame.
	// So, we have to convert JPEG to YUV if capture plugin has requested YUV
	// frame.
	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
		Log.d(TAG, "onPictureTaken");
		CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
		CameraController.getCamera().addCallbackBuffer(pviewBuffer);

		pluginManager.addToSharedMemExifTags(paramArrayOfByte);
		if (!CameraController.takeYUVFrame) // if JPEG frame requested
		{

			int frame = 0;
			if (resultInHeap)
				frame = SwapHeap.SwapToHeap(paramArrayOfByte);
			pluginManager.onImageTaken(frame, paramArrayOfByte, paramArrayOfByte.length, CameraController.JPEG);
		} else
		// is YUV frame requested
		{
//			new DecodeToYUVFrameTask().execute(paramArrayOfByte);
			
			int yuvFrame = ImageConversion.JpegConvert(paramArrayOfByte, imageSize.getWidth(),
	    			imageSize.getHeight(), false, false, 0);
			int frameLen = imageSize.getWidth() * imageSize.getHeight() + 2
					* ((imageSize.getWidth() + 1) / 2) * ((imageSize.getHeight() + 1) / 2);

			byte[] frameData = null;
			if (!resultInHeap)
			{
				frameData = SwapHeap.SwapFromHeap(yuvFrame, frameLen);
				yuvFrame = 0;
			}			
			
			pluginManager.onImageTaken(yuvFrame, frameData, frameLen, CameraController.YUV);
			
//			int yuvFrame = ImageConversion.JpegConvert(paramArrayOfByte, imageSize.getWidth(),
//					imageSize.getHeight(), false, false, 0);
//			int frameLen = imageSize.getWidth() * imageSize.getHeight() + 2
//					* ((imageSize.getWidth() + 1) / 2) * ((imageSize.getHeight() + 1) / 2);
//
//			byte[] frameData = null;
//			if (!resultInHeap)
//			{
//				frameData = SwapHeap.SwapFromHeap(yuvFrame, frameLen);
//				yuvFrame = 0;
//			}
//			pluginManager.onImageTaken(yuvFrame, frameData, frameLen, true);
		}

		try
		{
			CameraController.startCameraPreview();
		} catch (RuntimeException e)
		{
			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED_IOEXCEPTION);
			MainScreen.getInstance().muteShutter(false);
			CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;
			return;
		}
		CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;

		CameraController.sendMessage(MSG_NEXT_FRAME);
		
		String modeID = PluginManager.getInstance().getActiveModeID();
		if (modeID.equals("hdrmode") || modeID.equals("expobracketing"))
		{
			//if preview not working
			if (previewMode==false)
				return;
			previewWorking = false;
			//start timer to check if onpreviewframe working
			cdt = new CountDownTimer(5000, 5000) {
				public void onTick(long millisUntilFinished) {
				}
	
				public void onFinish() {
					if (!previewWorking)
					{
						Log.d(TAG, "previewMode DISABLED!");
						previewMode=false;
						SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
						Editor prefsEditor = prefs.edit();
						prefsEditor.putBoolean(MainScreen.sExpoPreviewModePref, false);
						prefsEditor.commit();
						evLatency=0;
						CameraController.sendMessage(MSG_TAKE_IMAGE);
					}
				}
			};
			cdt.start();
		}
	}
	
	private class DecodeToYUVFrameTask extends AsyncTask<byte[], Void, Void> {
		int yuvFrame = 0;
		int frameLen = 0;
		byte[] frameData = null;
		
		@Override
	     protected Void doInBackground(byte[]...params)
	     {
	    	byte[] paramArrayOfByte = params[0];	    	
	    	yuvFrame = ImageConversion.JpegConvert(paramArrayOfByte, imageSize.getWidth(),
	    			imageSize.getHeight(), false, false, 0);
			frameLen = imageSize.getWidth() * imageSize.getHeight() + 2
					* ((imageSize.getWidth() + 1) / 2) * ((imageSize.getHeight() + 1) / 2);

			frameData = null;
			if (!resultInHeap)
			{
				frameData = SwapHeap.SwapFromHeap(yuvFrame, frameLen);
				yuvFrame = 0;
			}			
			
//			pluginManager.onImageTaken(yuvFrame, frameData, frameLen, true);
			return null;	         
	     }

		@Override
	     protected void onPostExecute(Void result)
	     {
	    	 pluginManager.onImageTaken(yuvFrame, frameData, frameLen, CameraController.YUV);
	     }
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

	public static void onAutoFocus(boolean focused)
	{
		pluginManager.onAutoFocus(focused);
		if (focused)
			CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSED);
		else
			CameraController.setFocusState(CameraController.FOCUS_STATE_FAIL);
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera camera)
	{
		pluginManager.onPreviewFrame(data);
		CameraController.getCamera().addCallbackBuffer(pviewBuffer);

		if (takePreviewFrame)
		{
			takePreviewFrame = false;
			if (CameraController.takeYUVFrame)
			{
				int frame = 0;
				int dataLenght = data.length;
				if (resultInHeap)
				{
					frame = SwapHeap.SwapToHeap(data);
					data = null;
				}
				
				pluginManager.addToSharedMemExifTags(null);
				pluginManager.onImageTaken(frame, data, dataLenght, CameraController.YUV);
			} else
			{
				int jpegData = 0;
				// int yuvFrame = ImageConversion.JpegConvert(paramArrayOfByte,
				// imageSize.getWidth(), imageSize.getHeight(),
				// false, false, 0);
				// pluginManager.onImageTaken(yuvFrame, null, 0, true);
			}

			// pluginManager.onPictureTaken(data, true);
			CameraController.sendMessage(MSG_NEXT_FRAME);
			return;
		}

		String modeID = PluginManager.getInstance().getActiveModeID();
		if ((modeID.equals("hdrmode") || modeID.equals("expobracketing")) && evLatency > 0)
		{
			Log.d(TAG, "evLatency = " + evLatency);
			previewWorking = true;
			
			if (--evLatency == 0)
			{
				if (cdt != null)
				{
					cdt.cancel();
					cdt = null;
				}
				CameraController.sendMessage(MSG_TAKE_IMAGE);
			}
			return;
		}
	}

	public static void setPreviewCallbackWithBuffer()
	{
		if (!CameraController.isHALv3)
		{
			CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
			CameraController.getCamera().addCallbackBuffer(CameraController.pviewBuffer);
		}
	}

	// ^^^^^^^^^^^^^ CAPTURE AND FOCUS FUNCTION ----------------------------


	// =============== Captured Image data manipulation ======================

	@TargetApi(19)
	public static boolean isYUVImage(Image im)
	{
		return im.getFormat() == ImageFormat.YUV_420_888;
	}

	@TargetApi(19)
	public static int getImageLenght(Image im)
	{
		if (im.getFormat() == ImageFormat.YUV_420_888)
		{
			return imageSize.getWidth() * imageSize.getHeight() + imageSize.getWidth()
					* ((imageSize.getHeight() + 1) / 2);
		} else if (im.getFormat() == ImageFormat.JPEG)
		{
			ByteBuffer jpeg = im.getPlanes()[0].getBuffer();

			return jpeg.limit();
		}

		return 0;
	}

	// ^^^^^^^^^^^^^^^^^^^^^ Image data manipulation ^^^^^^^^^^^^^^^^^^^^^^^^^^^

	public static class Size
	{
		private int	mWidth;
		private int	mHeight;

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
	public void onShutter()
	{
		// Not used
	}

	// set exposure based on onpreviewframe
	private static int				evLatency;
	private static boolean			previewMode			= true;
	private static boolean			previewWorking		= false;
	private static CountDownTimer	cdt					= null;
	private static long				lastCaptureStarted	= 0;

	public static final int	MSG_SET_EXPOSURE	= 01;
	public static final int	MSG_NEXT_FRAME		= 02;
	public static final int	MSG_TAKE_IMAGE		= 03;

	public static void sendMessage(int what)
	{
		Message message = new Message();
		message.what = what;
		messageHandler.sendMessage(message);
	}

	// Handle messages only for old camera interface logic
	@Override
	public boolean handleMessage(Message msg)
	{

		switch (msg.what)
		{
		case MSG_SET_EXPOSURE:
			try
			{
				// Note: LumaAdaptation is obsolete and unlikely to be relevant for Android >= 4.0
				// if (UseLumaAdaptation && LumaAdaptationAvailable)
				// CameraController.setLumaAdaptation(evValues[frame_num]);
				// else
				if (evValues != null && evValues.length > frame_num)
					CameraController.setCameraExposureCompensation(evValues[frame_num]);
			} catch (RuntimeException e)
			{
				Log.e(TAG, "setExpo fail in MSG_SET_EXPOSURE");
			}

			String modeID = PluginManager.getInstance().getActiveModeID();
			if ((modeID.equals("hdrmode") || modeID.equals("expobracketing")) && previewMode)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
				//if true - evLatency will be doubled. 
				boolean isSlow = prefs.getBoolean("PreferenceExpoSlow", false);
				
				// Note 3 & LG G3 need more time to change exposure.
				if (Build.MODEL.contains("SM-N900"))
					evLatency = 20*(isSlow?2:1);
				else if (Build.MODEL.contains("LG-D855"))
					evLatency = 30*(isSlow?2:1);
				else
				{
					// message to capture image will be emitted a few frames after
					// setExposure
					evLatency = 10*(isSlow?2:1);// the minimum value at which Galaxy Nexus is
												// changing exposure in a stable way
				}
			} else
			{
				new CountDownTimer(500, 500)
				{
					public void onTick(long millisUntilFinished)
					{
					}

					public void onFinish()
					{
						CameraController.sendMessage(MSG_TAKE_IMAGE);
					}
				}.start();
			}

			return true;

		case MSG_NEXT_FRAME:
			Log.d(TAG, "MSG_NEXT_FRAME");
			String modeID2 = PluginManager.getInstance().getActiveModeID();
			if (++frame_num < total_frames)
			{
				if (pauseBetweenShots == null || Array.getLength(pauseBetweenShots) < frame_num)
				{
					if (evValues != null && evValues.length >= total_frames)
						CameraController.sendMessage(MSG_SET_EXPOSURE);
					else
						CameraController.sendMessage(MSG_TAKE_IMAGE);
				} else
				{
					pauseHandler.postDelayed(new Runnable()
					{
						public void run()
						{
							if (evValues != null && evValues.length >= total_frames)
								CameraController.sendMessage(MSG_SET_EXPOSURE);
							else
								CameraController.sendMessage(MSG_TAKE_IMAGE);
						}
					},
					pauseBetweenShots[frame_num] - (SystemClock.uptimeMillis() - lastCaptureStarted));
				}
			}
			else if (modeID2.equals("hdrmode") || modeID2.equals("expobracketing"))
			{
				previewWorking = true;
            	if (cdt!=null)
            	{
            		cdt.cancel();
            		cdt = null;
            	}
			}
			break;
		case MSG_TAKE_IMAGE:
			synchronized (SYNC_OBJECT)
			{
				int imageWidth = imageSize.getWidth();
				int imageHeight = imageSize.getHeight();
				int previewWidth = MainScreen.getPreviewWidth();
				int previewHeight = MainScreen.getPreviewHeight();

				// play tick sound
				MainScreen.getGUIManager().showCaptureIndication();
				MainScreen.getInstance().playShutter();

				lastCaptureStarted = SystemClock.uptimeMillis();
				if (imageWidth == previewWidth && imageHeight == previewHeight &&
						((frameFormat == CameraController.YUV) || (frameFormat == CameraController.YUV_RAW)))
					takePreviewFrame = true; // Temporary make capture by
												// preview frames only for YUV
												// requests to avoid slow YUV to
												// JPEG conversion
				else if (camera != null && CameraController.getFocusState() != CameraController.FOCUS_STATE_FOCUSING)
				{
					try
					{
						mCaptureState = CameraController.CAPTURE_STATE_CAPTURING;
						camera.setPreviewCallback(null);
						camera.takePicture(null, null, null, CameraController.getInstance());
					}
					catch(Exception exp)
					{
						previewWorking = true;
		            	if (cdt!=null)
		            	{
		            		cdt.cancel();
		            		cdt = null;
		            	}
		            	
						Log.e(TAG, "takePicture exception. Message: " + exp.getMessage());
						exp.printStackTrace();
						
//						PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED_NORESULT, 0);
					}

				}
			}
			break;
		default:
			break;
		}

		return true;
	}
}
