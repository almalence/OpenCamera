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
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.AutoFocusCallback;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
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
import com.almalence.sony.cameraremote.PictureCallbackSonyRemote;
import com.almalence.sony.cameraremote.ServerDevice;
import com.almalence.sony.cameraremote.ZoomCallbackSonyRemote;
import com.almalence.util.ImageConversion;
//<!-- -+-
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.PluginManagerInterface;
import com.almalence.opencam.R;
//-+- -->
/* <!-- +++
 import com.almalence.opencam_plus.ApplicationInterface;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.PluginManagerInterface;
 import com.almalence.opencam_plus.R;
 +++ --> */

public class CameraController implements Camera.PictureCallback, Camera.AutoFocusCallback, Camera.ErrorCallback,
		Camera.PreviewCallback, Camera.ShutterCallback, Handler.Callback, PictureCallbackSonyRemote
{
	private static final String						TAG								= "CameraController";

	// YUV_RAW is the same as YUV (ie NV21) except that
	// noise filtering, edge enhancements and scaler
	// are disabled if possible
	public static final int							RAW								= 0x20;
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

	private static String							wbAuto;
	private static String							wbIncandescent;
	private static String							wbFluorescent;
	private static String							wbWarmFluorescent;
	private static String							wbDaylight;
	private static String							wbCloudyDaylight;
	private static String							wbTwilight;
	private static String							wbShade;

	private static String							wbAutoSonyRemote;
	private static String							wbIncandescentSonyRemote;
	private static String							wbWarmFluorescentSonyRemote;
	private static String							wbDaylightSonyRemote;
	private static String							wbCloudyDaylightSonyRemote;
	private static String							wbShadeSonyRemote;

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
	private static String							iso6400;
	private static String							iso10000;

	private static String							isoAuto_2;
	private static String							iso50_2;
	private static String							iso100_2;
	private static String							iso200_2;
	private static String							iso400_2;
	private static String							iso800_2;
	private static String							iso1600_2;
	private static String							iso3200_2;
	private static String							iso6400_2;
	private static String							iso10000_2;

	private static String							meteringAuto;
	private static String							meteringMatrix;
	private static String							meteringCenter;
	private static String							meteringSpot;

	private static String							colorEffectOffSystem;
	private static String							colorEffectAquaSystem;
	private static String							colorEffectBlackboardSystem;
	private static String							colorEffectMonoSystem;
	private static String							colorEffectNegativeSystem;
	private static String							colorEffectPosterizeSystem;
	private static String							colorEffectSepiaSystem;
	private static String							colorEffectSolarizeSystem;
	private static String							colorEffectWhiteboardSystem;

	// List of localized names for camera parameters values
	private static Map<Integer, String>				mode_scene;
	private static Map<String, Integer>				key_scene;

	private static Map<Integer, String>				mode_wb;
	private static Map<Integer, String>				mode_wb_sony_remote;
	private static Map<String, Integer>				key_wb;
	private static Map<String, Integer>				key_wb_sony_remote;

	private static Map<Integer, String>				mode_focus;
	private static Map<String, Integer>				key_focus;

	private static Map<Integer, String>				mode_flash;
	private static Map<String, Integer>				key_flash;

	private static Map<Integer, String>				mode_color_effect;
	private static Map<String, Integer>				key_color_effect;

	private static List<Integer>					iso_values;
	private static List<String>						iso_default;
	private static Map<String, String>				iso_default_values;
	private static Map<Integer, String>				mode_iso;
	private static Map<Integer, String>				mode_iso2;
	private static Map<Integer, Integer>			mode_iso_HALv3;
	private static Map<String, Integer>				key_iso;
	private static Map<String, Integer>				key_iso2;
	private static boolean							isUseISO2Keys					= true;

	private static CameraController					cameraController				= null;

	private static PluginManagerInterface			pluginManager					= null;
	private static ApplicationInterface				appInterface					= null;
	protected static Context						mainContext						= null;
	protected static Handler						messageHandler					= null;

	// Old camera interface
	private static Camera							camera							= null;
	private static Camera.Parameters				cameraParameters				= null;

	private static byte[]							pviewBuffer;

	// Message handler for multishot capturing with pause between shots
	// and different exposure compensations
	private static Handler							pauseHandler;

	private static boolean							needRelaunch					= false;
	public static boolean							isOldCameraOneModeLaunched		= false;

	private static boolean							isHALv3							= false;
	private static boolean							isHALv3Supported				= false;
	protected static boolean						isRAWCaptureSupported			= false;
	protected static boolean						isManualSensorSupported			= false;

	protected static String[]						cameraIdList					= { "" };

	// Flags to know which camera feature supported at current device
	private static boolean							mEVSupported					= false;
	private static boolean							mSceneModeSupported				= false;
	private static boolean							mWBSupported					= false;
	private static boolean							mFocusModeSupported				= false;
	private static boolean							mFlashModeSupported				= false;
	private static boolean							mISOSupported					= false;
	private static boolean							mCollorEffectSupported			= false;

	private static int								minExpoCompensation				= 0;
	private static int								maxExpoCompensation				= 0;
	private static float							expoCompensationStep			= 0;

	protected static boolean						mVideoStabilizationSupported	= false;

	private static int[]							supportedSceneModes;
	private static int[]							supportedWBModes;
	private static int[]							supportedFocusModes;
	private static int[]							supportedFlashModes;
	private static int[]							supportedCollorEffects;
	private static int[]							supportedISOModes;

	private static int								maxFocusRegionsSupported;
	private static int								maxMeteringRegionsSupported;

	protected static int							CameraIndex						= 0;
	protected static boolean						CameraMirrored					= false;

	protected static int							mDisplayOrientation				= 0;

	// Image size index for capturing
	private static int								CapIdx;

	private static Size								imageSize;

	private static int								iPreviewWidth;
	private static int								iPreviewHeight;

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

	public static List<String>						ColorEffectsNamesList;

	public static List<Integer>						FastIdxelist;

	protected static List<CameraController.Size>	SupportedPreviewSizesList;
	protected static List<CameraController.Size>	SupportedPictureSizesList;
	protected static List<CameraController.Size>	SupportedVideoSizesList;

	protected static final CharSequence[]			RATIO_STRINGS					= { " ", "4:3", "3:2", "16:9",
			"1:1"																	};

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

	protected static boolean						appStarted						= false;

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

	public static void onCreate(Context context, ApplicationInterface app, PluginManagerInterface pluginManagerBase,
			Handler msgHandler)
	{
		pluginManager = pluginManagerBase;
		appInterface = app;
		mainContext = context;

		messageHandler = msgHandler;

		pauseHandler = new Handler(CameraController.getInstance());

		appStarted = false;

		isOldCameraOneModeLaunched = false;

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

		wbAuto = mainContext.getResources().getString(R.string.wbAutoSystem);
		wbIncandescent = mainContext.getResources().getString(R.string.wbIncandescentSystem);
		wbFluorescent = mainContext.getResources().getString(R.string.wbFluorescentSystem);
		wbWarmFluorescent = mainContext.getResources().getString(R.string.wbWarmFluorescentSystem);
		wbDaylight = mainContext.getResources().getString(R.string.wbDaylightSystem);
		wbCloudyDaylight = mainContext.getResources().getString(R.string.wbCloudyDaylightSystem);
		wbTwilight = mainContext.getResources().getString(R.string.wbTwilightSystem);
		wbShade = mainContext.getResources().getString(R.string.wbShadeSystem);

		wbAutoSonyRemote = mainContext.getResources().getString(R.string.wbAutoSonyRemote);
		wbIncandescentSonyRemote = mainContext.getResources().getString(R.string.wbIncandescentSonyRemote);
		wbWarmFluorescentSonyRemote = mainContext.getResources().getString(R.string.wbWarmFluorescentSonyRemote);
		wbDaylightSonyRemote = mainContext.getResources().getString(R.string.wbDaylightSonyRemote);
		wbCloudyDaylightSonyRemote = mainContext.getResources().getString(R.string.wbCloudyDaylightSonyRemote);
		wbShadeSonyRemote = mainContext.getResources().getString(R.string.wbShadeSonyRemote);

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
		iso6400 = mainContext.getResources().getString(R.string.iso6400System);
		iso10000 = mainContext.getResources().getString(R.string.iso10000System);

		isoAuto_2 = mainContext.getResources().getString(R.string.isoAutoDefaultSystem);
		iso50_2 = mainContext.getResources().getString(R.string.iso50DefaultSystem);
		iso100_2 = mainContext.getResources().getString(R.string.iso100DefaultSystem);
		iso200_2 = mainContext.getResources().getString(R.string.iso200DefaultSystem);
		iso400_2 = mainContext.getResources().getString(R.string.iso400DefaultSystem);
		iso800_2 = mainContext.getResources().getString(R.string.iso800DefaultSystem);
		iso1600_2 = mainContext.getResources().getString(R.string.iso1600DefaultSystem);
		iso3200_2 = mainContext.getResources().getString(R.string.iso3200DefaultSystem);
		iso6400_2 = mainContext.getResources().getString(R.string.iso6400DefaultSystem);
		iso10000_2 = mainContext.getResources().getString(R.string.iso10000DefaultSystem);

		meteringAuto = mainContext.getResources().getString(R.string.meteringAutoSystem);
		meteringMatrix = mainContext.getResources().getString(R.string.meteringMatrixSystem);
		meteringCenter = mainContext.getResources().getString(R.string.meteringCenterSystem);
		meteringSpot = mainContext.getResources().getString(R.string.meteringSpotSystem);

		colorEffectOffSystem = mainContext.getResources().getString(R.string.colorEffectOffSystem);
		colorEffectAquaSystem = mainContext.getResources().getString(R.string.colorEffectAquaSystem);
		colorEffectBlackboardSystem = mainContext.getResources().getString(R.string.colorEffectBlackboardSystem);
		colorEffectMonoSystem = mainContext.getResources().getString(R.string.colorEffectMonoSystem);
		colorEffectNegativeSystem = mainContext.getResources().getString(R.string.colorEffectNegativeSystem);
		colorEffectPosterizeSystem = mainContext.getResources().getString(R.string.colorEffectPosterizeSystem);
		colorEffectSepiaSystem = mainContext.getResources().getString(R.string.colorEffectSepiaSystem);
		colorEffectSolarizeSystem = mainContext.getResources().getString(R.string.colorEffectSolarizeSystem);
		colorEffectWhiteboardSystem = mainContext.getResources().getString(R.string.colorEffectWhiteboardSystem);

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

		mode_wb_sony_remote = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.WB_MODE_AUTO, wbAutoSonyRemote);
				put(CameraParameters.WB_MODE_INCANDESCENT, wbIncandescentSonyRemote);
				put(CameraParameters.WB_MODE_WARM_FLUORESCENT, wbWarmFluorescentSonyRemote);
				put(CameraParameters.WB_MODE_DAYLIGHT, wbDaylightSonyRemote);
				put(CameraParameters.WB_MODE_CLOUDY_DAYLIGHT, wbCloudyDaylightSonyRemote);
				put(CameraParameters.WB_MODE_SHADE, wbShadeSonyRemote);
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

		key_wb_sony_remote = new HashMap<String, Integer>()
		{
			{
				put(wbAutoSonyRemote, CameraParameters.WB_MODE_AUTO);
				put(wbIncandescentSonyRemote, CameraParameters.WB_MODE_INCANDESCENT);
				put(wbWarmFluorescentSonyRemote, CameraParameters.WB_MODE_WARM_FLUORESCENT);
				put(wbDaylightSonyRemote, CameraParameters.WB_MODE_DAYLIGHT);
				put(wbCloudyDaylightSonyRemote, CameraParameters.WB_MODE_CLOUDY_DAYLIGHT);
				put(wbShadeSonyRemote, CameraParameters.WB_MODE_SHADE);
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

		mode_color_effect = new HashMap<Integer, String>()
		{
			{
				put(CameraParameters.COLOR_EFFECT_MODE_OFF, colorEffectOffSystem);
				put(CameraParameters.COLOR_EFFECT_MODE_AQUA, colorEffectAquaSystem);
				put(CameraParameters.COLOR_EFFECT_MODE_BLACKBOARD, colorEffectBlackboardSystem);
				put(CameraParameters.COLOR_EFFECT_MODE_MONO, colorEffectMonoSystem);
				put(CameraParameters.COLOR_EFFECT_MODE_NEGATIVE, colorEffectNegativeSystem);
				put(CameraParameters.COLOR_EFFECT_MODE_POSTERIZE, colorEffectPosterizeSystem);
				put(CameraParameters.COLOR_EFFECT_MODE_SEPIA, colorEffectSepiaSystem);
				put(CameraParameters.COLOR_EFFECT_MODE_SOLARIZE, colorEffectSolarizeSystem);
				put(CameraParameters.COLOR_EFFECT_MODE_WHITEBOARD, colorEffectWhiteboardSystem);
			}
		};

		key_color_effect = new HashMap<String, Integer>()
		{
			{
				put(colorEffectOffSystem, CameraParameters.COLOR_EFFECT_MODE_OFF);
				put(colorEffectAquaSystem, CameraParameters.COLOR_EFFECT_MODE_AQUA);
				put(colorEffectBlackboardSystem, CameraParameters.COLOR_EFFECT_MODE_BLACKBOARD);
				put(colorEffectMonoSystem, CameraParameters.COLOR_EFFECT_MODE_MONO);
				put(colorEffectNegativeSystem, CameraParameters.COLOR_EFFECT_MODE_NEGATIVE);
				put(colorEffectPosterizeSystem, CameraParameters.COLOR_EFFECT_MODE_POSTERIZE);
				put(colorEffectSepiaSystem, CameraParameters.COLOR_EFFECT_MODE_SEPIA);
				put(colorEffectSolarizeSystem, CameraParameters.COLOR_EFFECT_MODE_SOLARIZE);
				put(colorEffectWhiteboardSystem, CameraParameters.COLOR_EFFECT_MODE_WHITEBOARD);
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
				add(CameraParameters.ISO_6400);
				add(CameraParameters.ISO_10000);
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
				put(CameraParameters.ISO_6400, iso6400);
				put(CameraParameters.ISO_10000, iso10000);
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
				put(CameraParameters.ISO_6400, iso6400_2);
				put(CameraParameters.ISO_10000, iso10000_2);
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
				put(CameraParameters.ISO_6400, 6400);
				put(CameraParameters.ISO_10000, 10000);
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
				put(iso6400, CameraParameters.ISO_6400);
				put(iso10000, CameraParameters.ISO_10000);
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
				put(iso6400_2, CameraParameters.ISO_6400);
				put(iso10000_2, CameraParameters.ISO_10000);
			}
		};

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);

		isHALv3 = prefs.getBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), false);
		if (!pluginManager.isCamera2InterfaceAllowed())
			isHALv3 = false;

		try
		{
			if (!(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT && mainContext.getSystemService("camera") != null)
					|| (!isFlex2() && !isNexus() && !isAndroidOne()))
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
			HALv3.onCreateHALv3(mainContext, appInterface, pluginManager, messageHandler);
			if (!HALv3.checkHardwareLevel())
			{
				isHALv3 = false;
				isHALv3Supported = false;
				prefs.edit().putBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), false)
						.commit();
			}
		}

		SonyRemoteCamera.onCreateSonyRemoteCamera(mainContext, appInterface, pluginManager, messageHandler);
	}

	public static void createHALv3Manager()
	{
		if (CameraController.isHALv3Supported)
			HALv3.onCreateHALv3(mainContext, appInterface, pluginManager, messageHandler);
	}

	public static void onStart()
	{
		// Does nothing yet
	}

	public static void onResume()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (pluginManager.isPreviewDependentMode())
			{
				previewMode = appInterface.getExpoPreviewPref();

				evLatency = 0;
				previewWorking = false;
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
	}

	public static void onPause(boolean isModeSwitching)
	{
		if (pluginManager.isPreviewDependentMode())
		{
			evLatency = 0;
			previewWorking = false;
			if (cdt != null)
			{
				cdt.cancel();
				cdt = null;
			}
		}

		total_frames = 0;

		if (!CameraController.isRemoteCamera())
		{
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
					camera.stopPreview();
					if (!isModeSwitching)
					{
						camera.release();
						camera = null;
					}
				}
			} else
				HALv3.onPauseHALv3();
		} else
		{
			SonyRemoteCamera.onPauseSonyRemoteCamera();
		}

		CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;
	}

	public static void onStop()
	{
		if (needRelaunch)
		{
			SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mainContext).edit();
			prefEditor.putBoolean(mainContext.getResources().getString(R.string.Preference_UseHALv3Key), true).commit();
		}
	}

	public static void onDestroy()
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

	public static boolean isRemoteCamera()
	{
		return (CameraIndex == getNumberOfCameras() - 1);
	}

	public static void useHALv3(boolean useHALv3)
	{
		Log.e(TAG, "useHALv3 " + useHALv3);
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
			 * boolean nroffAvailable = false; int nrmodes[] =
			 * camCharacter.get(CameraCharacteristics
			 * .NOISE_REDUCTION_AVAILABLE_NOISE_REDUCTION_MODES); if (nrmodes !=
			 * null) for (int i=0; i<nrmodes.length; ++i) if (nrmodes[i] ==
			 * CameraMetadata.NOISE_REDUCTION_MODE_OFF) nroffAvailable = true;
			 * 
			 * if ( (
			 * (camCharacter.get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL
			 * ) == CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_FULL) ||
			 * ((camCharacter
			 * .get(CameraCharacteristics.INFO_SUPPORTED_HARDWARE_LEVEL) ==
			 * CameraMetadata.INFO_SUPPORTED_HARDWARE_LEVEL_LIMITED) &&
			 * (camCharacter.get(CameraCharacteristics.SYNC_MAX_LATENCY) ==
			 * CameraMetadata.SYNC_MAX_LATENCY_PER_FRAME_CONTROL)) ) &&
			 * nroffAvailable ) SuperModeOk = true;
			 */

			// hard-code to enable these only, as we have no profiles for
			// other models at the moment
			if (CameraController.isNexus() || CameraController.isFlex2())
				SuperModeOk = true;
		}

		return SuperModeOk;
	}

	public static boolean isUseSuperMode()
	{
		return (isSuperModePossible() && isHALv3) || (isSuperModePossible() && isOldCameraOneModeLaunched);
	}

	public static boolean isNexus()
	{
		return Build.MODEL.contains("Nexus 5") || Build.MODEL.contains("Nexus 6");
	}

	public static boolean isFlex2()
	{
		return Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h959")
				|| Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-f510");
	}
	
	public static boolean isAndroidOne()
	{
		return Build.MODEL.contains("Micromax AQ4501");
	}

	public static boolean isHALv3Supported()
	{
		return isHALv3Supported;
	}

	public static boolean isRAWCaptureSupported()
	{
		return isRAWCaptureSupported;
	}

	public static boolean isManualSensorSupported()
	{
		return isManualSensorSupported;
	}

	// Google doc's method to determine camera's display orientation
	public static void setCameraDisplayOrientation(Activity activity, int cameraId, android.hardware.Camera camera)
	{
		android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
		android.hardware.Camera.getCameraInfo(cameraId, info);
		int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
		int degrees = 0;
		switch (rotation)
		{
		case Surface.ROTATION_0:
			degrees = 0;
			break;
		case Surface.ROTATION_90:
			degrees = 90;
			break;
		case Surface.ROTATION_180:
			degrees = 180;
			break;
		case Surface.ROTATION_270:
			degrees = 270;
			break;
		}

		int result;
		if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
		{
			result = (info.orientation + degrees) % 360;
			result = (360 - result) % 360; // compensate the mirror
		} else
		{ // back-facing
			result = (info.orientation - degrees + 360) % 360;
		}

		mDisplayOrientation = result;
		camera.setDisplayOrientation(mDisplayOrientation);
	}

	public static int getDisplayOrientation()
	{
		return 0;
	}

	public static void setupCamera(SurfaceHolder holder, boolean openCamera)
	{
		if (!CameraController.isRemoteCamera())
		{
			// Devices camera setup
			if (!CameraController.isHALv3)
			{
				if (camera == null || !openCamera)
				{
					try
					{
						if (openCamera)
						{
							if (Camera.getNumberOfCameras() > 0)
								camera = Camera.open(CameraIndex);
							else
								camera = Camera.open();
						}

						Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
						Camera.getCameraInfo(CameraIndex, cameraInfo);
						if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
							CameraMirrored = true;
						else
							CameraMirrored = false;

						if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN)
							setAutoFocusMoveCallback(camera);
					} catch (RuntimeException e)
					{
						Log.e(TAG, "Unable to open camera");
						e.printStackTrace();
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
						setCameraDisplayOrientation(appInterface.getMainActivity(), CameraIndex, camera);
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

			if (!CameraController.isHALv3)
			{
				if (CameraController.isCameraCreated())
					appInterface.configureCamera(true);
			}
		} else
		{
			if (SonyRemoteCamera.mTargetDevice != null)
			{
				SonyRemoteCamera.openCameraSonyRemote();
			}
		}

	}

	@TargetApi(16)
	protected static void setAutoFocusMoveCallback(Camera camera)
	{
		try
		{
			camera.setAutoFocusMoveCallback(new Camera.AutoFocusMoveCallback()
			{
				@Override
				public void onAutoFocusMoving(boolean start, Camera camera)
				{
					CameraController.onAutoFocusMoving(start);
				}
			});
		} catch (Exception e)
		{
			Log.e(TAG, "setAutoFocusModeCallback failed");
		}
	}

	public static boolean isCameraCreated()
	{
		if (!CameraController.isHALv3)
			return camera != null;
		else
			return isCameraCreatedHALv3();

	}
	
	public static boolean isCaptureFormatSupported(int captureFormat)
	{
		if(isUseHALv3())
			return HALv3.isCaptureFormatSupported(captureFormat);
		else
			return true;
	}

	@TargetApi(21)
	public static void setCaptureFormat(int captureFormat)
	{
		HALv3.setCaptureFormat(captureFormat);
	}

	@TargetApi(21)
	public static boolean createCaptureSession(List<Surface> sfl)
	{
		return HALv3.createCaptureSession(sfl);
	}

	@TargetApi(21)
	public static boolean isCameraCreatedHALv3()
	{
		return HALv3.getInstance().camDevice != null;
	}

	private static void fillPreviewSizeList()
	{
		if (!CameraController.isRemoteCamera())
		{
			CameraController.SupportedPreviewSizesList = new ArrayList<CameraController.Size>();
			if (!isHALv3)
			{
				if (camera != null && camera.getParameters() != null)
				{
					List<Camera.Size> list = camera.getParameters().getSupportedPreviewSizes();
					if (list != null) {
						for (Camera.Size sz : list)
							CameraController.SupportedPreviewSizesList.add(new CameraController.Size(sz.width, sz.height));
					}
				}
			} else
				CameraController.SupportedPreviewSizesList = HALv3.fillPreviewSizeList();
		} else
		{
			CameraController.SupportedPreviewSizesList = SonyRemoteCamera.getPreviewSizeListRemote();
		}
	}

	private static void fillPictureSizeList()
	{
		CameraController.SupportedPictureSizesList = new ArrayList<CameraController.Size>();
		if (!isHALv3)
		{
			if (camera != null && camera.getParameters() != null)
			{
				List<Camera.Size> list = camera.getParameters().getSupportedPictureSizes();
				for (Camera.Size sz : list)
					CameraController.SupportedPictureSizesList.add(new CameraController.Size(sz.width, sz.height));
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

	public static void populateCameraDimensionsSonyRemote()
	{
		CameraController.fillPreviewSizeList();
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();
		CameraController.FastIdxelist = new ArrayList<Integer>();

		int minMPIX = CameraController.MIN_MPIX_SUPPORTED;

		int iHighestIndex = 0;
		CameraController.Size sHighest = SonyRemoteCamera.getPictureSizeListRemote().get(0);

		for (int ii = 0; ii < SonyRemoteCamera.getPictureSizeListRemote().size(); ++ii)
		{
			CameraController.Size s = SonyRemoteCamera.getPictureSizeListRemote().get(ii);

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

			fillResolutionsList(ii, currSizeWidth, currSizeHeight);
		}

		if (CameraController.ResolutionsNamesList.isEmpty())
		{
			CameraController.Size s = SonyRemoteCamera.getPictureSizeListRemote().get(iHighestIndex);

			int currSizeWidth = s.getWidth();
			int currSizeHeight = s.getHeight();

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
		float ratio = (float) ((float) currSizeWidth / (float) currSizeHeight);

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
			CameraController.ResolutionsSizeList.add(loc, new CameraController.Size(currSizeWidth, currSizeHeight));
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
						maxFastIdx = CameraController.FastIdxelist.get(i);
					}
				}
			}
			if (CameraController.SupportedPreviewSizesList != null
					&& CameraController.SupportedPreviewSizesList.size() > 0 && maxMpx >= MPIX_1080)
				appInterface.setSpecialImageSizeIndexPref(maxFastIdx);
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

		if (mpix < 0.1f)
		{
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
			CameraController.MultishotResolutionsSizeList.add(loc, new CameraController.Size(currSizeWidth,
					currSizeHeight));
		}
	}

	public static List<CameraController.Size> getSupportedPreviewSizes()
	{
		if (!CameraController.isRemoteCamera())
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
		} else
		{
			return SonyRemoteCamera.getPreviewSizeListRemote();
		}
	}

	public static void setCameraPreviewSize(CameraController.Size sz)
	{
		iPreviewWidth = sz.mWidth;
		iPreviewHeight = sz.mHeight;

		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				Camera.Parameters params = getCameraParameters();
				if (params != null)
				{
					params.setPreviewSize(iPreviewWidth, iPreviewHeight);
					setCameraParameters(params);
				}
			} else
			{
				HALv3.setupImageReadersHALv3(sz);
			}
		}
	}

	public static void setSurfaceHolderFixedSize(int width, int height)
	{
		if (CameraController.isHALv3)
		{
			ApplicationScreen.setSurfaceHolderSize(width, height);
		}
	}

	public static List<CameraController.Size> getSupportedPictureSizes()
	{
		List<CameraController.Size> pictureSizes = new ArrayList<CameraController.Size>();
		if (!CameraController.isRemoteCamera())
		{
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
		} else
		{
			SonyRemoteCamera.fillPictureSizeListRemote(pictureSizes);
		}

		return pictureSizes;
	}

	public static List<CameraController.Size> getSupportedVideoSizes()
	{
		List<CameraController.Size> videoSizes = new ArrayList<CameraController.Size>();
		if (!CameraController.isHALv3)
		{
			if (CameraController.SupportedVideoSizesList != null)
			{
				videoSizes = new ArrayList<CameraController.Size>(CameraController.SupportedVideoSizesList);
			} else if (camera != null && camera.getParameters() != null)
			{
				List<Camera.Size> sizes = camera.getParameters().getSupportedVideoSizes();
				for (Camera.Size sz : sizes)
					videoSizes.add(new CameraController.Size(sz.width, sz.height));
			} else
			{
				Log.d(TAG, "camera == null");
			}
		} else
			videoSizes = null;

		return videoSizes;
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

	public static List<CameraController.Size> getMultishotResolutionsSizeList()
	{
		return CameraController.MultishotResolutionsSizeList;
	}

	public static List<String> getMultishotResolutionsIdxesList()
	{
		return CameraController.MultishotResolutionsIdxesList;
	}

	public static List<String> getMultishotResolutionsNamesList()
	{
		return CameraController.MultishotResolutionsNamesList;
	}

	public static int getNumberOfCameras()
	{
		if (!CameraController.isHALv3)
			return Camera.getNumberOfCameras() + 1;
		else
			return CameraController.cameraIdList.length + 1;
	}

	public static void updateCameraFeatures()
	{
		try
		{
			if (camera != null)
				cameraParameters = camera.getParameters();

			mEVSupported = getExposureCompensationSupported();
			mSceneModeSupported = getSceneModeSupported();
			mWBSupported = getWhiteBalanceSupported();
			mFocusModeSupported = getFocusModeSupported();
			mCollorEffectSupported = getCollorEffectSupported();
			mFlashModeSupported = getFlashModeSupported();
			mISOSupported = getISOSupported();

			if (!CameraController.isRemoteCamera())
			{
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
			} else
			{
				minExpoCompensation = SonyRemoteCamera.getMinExposureCompensationRemote();
				maxExpoCompensation = SonyRemoteCamera.getMaxExposureCompensationRemote();
				expoCompensationStep = SonyRemoteCamera.getExposureCompensationStepRemote();
			}

			supportedSceneModes = getSupportedSceneModesInternal();
			supportedWBModes = getSupportedWhiteBalanceInternal();
			supportedFocusModes = getSupportedFocusModesInternal();
			supportedFlashModes = getSupportedFlashModesInternal();
			supportedCollorEffects = getSupportedCollorEffectsInternal();
			fillCollorEffectNames();

			supportedISOModes = getSupportedISOInternal();

			maxFocusRegionsSupported = CameraController.getMaxNumFocusAreas();
			maxMeteringRegionsSupported = CameraController.getMaxNumMeteringAreas();

			cameraParameters = null;
		} catch (NullPointerException exp)
		{
			exp.printStackTrace();
		}
	}

	@Override
	public void onError(int arg0, Camera arg1)
	{
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
	
	public static CameraDevice getCamera2()
	{
		return HALv3.getCamera2();
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
		} catch (Exception e)
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
		if (camera != null)
		{
			camera.startPreview();

			if (Build.MODEL.equals("Nexus 4"))
			{
				int initValue = appInterface.getEVPref();
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
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera != null && camera.getParameters() != null
						&& camera.getParameters().isVideoStabilizationSupported())
				{
					camera.getParameters().setVideoStabilization(stabilization);
					setCameraParameters(camera.getParameters());
				}
			}
		}
	}

	@TargetApi(15)
	public static boolean getVideoStabilizationSupported()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera == null || (camera != null && camera.getParameters() == null))
					return false;

				return camera.getParameters().isVideoStabilizationSupported();
			} else
				return false;
		} else
		{
			return false;
		}
	}

	public static boolean isVideoStabilizationSupported()
	{
		return mVideoStabilizationSupported;
	}

	public static boolean isExposureLockSupported()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera == null || (camera != null && camera.getParameters() == null))
					return false;

				return camera.getParameters().isAutoExposureLockSupported();
			} else
				return true;
		} else
		{
			return false;
		}
	}

	public static boolean isExposureLock()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera == null || (camera != null && camera.getParameters() == null))
					return false;

				return camera.getParameters().getAutoExposureLock();
			} else
				return true;
		} else
		{
			return false;
		}
	}

	public static void setAutoExposureLock(boolean lock)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera == null || (camera != null && camera.getParameters() == null))
					return;

				Camera.Parameters params = camera.getParameters();
				params.setAutoExposureLock(lock);
				camera.setParameters(params);
			} else
				HALv3.setAutoExposureLock(lock);
		}
	}

	public static boolean isWhiteBalanceLockSupported()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera == null || (camera != null && camera.getParameters() == null))
					return false;

				return camera.getParameters().isAutoWhiteBalanceLockSupported();
			} else
				return true;
		} else
		{
			return false;
		}
	}

	public static boolean isWhiteBalanceLock()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera == null || (camera != null && camera.getParameters() == null))
					return false;

				return camera.getParameters().getAutoWhiteBalanceLock();
			} else
				return PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext()).getBoolean(
						ApplicationScreen.sAWBLockPref, false);
		} else
		{
			return false;
		}
	}

	public static void setAutoWhiteBalanceLock(boolean lock)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera == null || (camera != null && camera.getParameters() == null))
					return;

				Camera.Parameters params = camera.getParameters();
				params.setAutoWhiteBalanceLock(lock);
				camera.setParameters(params);
			} else
				HALv3.setAutoWhiteBalanceLock(lock);
		}
	}

	public static boolean isZoomSupported()
	{
		if (!CameraController.isRemoteCamera())
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
		} else
		{
			return SonyRemoteCamera.isZoomAvailable();
		}
	}

	public static int getMaxZoom()
	{
		if (!CameraController.isRemoteCamera())
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
		} else
		{
			return 0;
		}
	}

	public static void setZoom(int value)
	{
		if (!CameraController.isRemoteCamera())
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
	}

	public static void setZoomCallbackSonyRemote(ZoomCallbackSonyRemote callback)
	{
		SonyRemoteCamera.setZoomCallbackSonyRemote(callback);
	}

	public static void actZoomSonyRemote(final String direction, final String movement)
	{
		SonyRemoteCamera.actZoom(direction, movement);
	}

	// Note: getZoom returns zoom in floating point,
	// unlike old android camera API which returns it multiplied by 10
	public static float getZoom()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				Camera.Parameters cp = getCameraParameters();
				return (cp.getZoom() / 10.0f + 1f);
			} else
				return HALv3.getZoom();
		} else
		{
			return 0.0f;
		}
	}

	public static boolean isLumaAdaptationSupported()
	{
		if (!CameraController.isRemoteCamera())
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
		} else
		{
			return false;
		}
	}

	// Used to initialize internal variable
	private static boolean getExposureCompensationSupported()
	{
		if (!CameraController.isRemoteCamera())
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
		} else
		{
			return SonyRemoteCamera.isExposureCompensationAvailable();
		}
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
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera != null && camera.getParameters() != null)
				{
					Camera.Parameters cameraParameters = CameraController.getCamera().getParameters();

					return cameraParameters.getExposureCompensation() * cameraParameters.getExposureCompensationStep();
				} else
					return 0;
			} else
				return appInterface.getEVPref() * HALv3.getExposureCompensationStepHALv3();
		} else
		{
			return SonyRemoteCamera.getExposureCompensationRemote()
					* SonyRemoteCamera.getExposureCompensationStepRemote();
		}
	}

	public static void resetExposureCompensation()
	{
		if (!CameraController.isRemoteCamera())
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
	}

	private static boolean getSceneModeSupported()
	{
		int[] supported_scene = getSupportedSceneModesInternal();

		if (supported_scene != null && supported_scene.length == 1)
			return supported_scene[0] != CameraParameters.SCENE_MODE_AUTO;

		return supported_scene != null && supported_scene.length > 0;
	}

	public static boolean isSceneModeSupported()
	{
		return mSceneModeSupported;
	}

	private static int[] getSupportedSceneModesInternal()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				List<String> sceneModes = null;
				if (cameraParameters != null)
				{
					sceneModes = cameraParameters.getSupportedSceneModes();
				} else if (camera != null)
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
		} else
		{
			return null;
		}
	}

	public static int[] getSupportedSceneModes()
	{
		return supportedSceneModes;
	}

	public static List<String> getSupportedSceneModesNames()
	{
		List<String> sceneModeNames = new ArrayList<String>();
		for (int i : supportedSceneModes)
		{
			sceneModeNames.add(mode_scene.get(i));
		}
		return sceneModeNames;
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
		if (!CameraController.isRemoteCamera())
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
		} else
		{
			// Sony camera modes
			List<String> wbModes;
			wbModes = SonyRemoteCamera.getAvailableWhiteBalanceRemote();

			if (wbModes != null)
			{
				Set<String> known_wb = CameraController.key_wb_sony_remote.keySet();
				wbModes.retainAll(known_wb);
				int[] wb = new int[wbModes.size()];
				for (int i = 0; i < wbModes.size(); i++)
				{
					String mode = wbModes.get(i);
					if (CameraController.key_wb_sony_remote.containsKey(mode))
						wb[i] = CameraController.key_wb_sony_remote.get(mode).byteValue();
				}
				return wb;
			}

			return new int[0];
		}
	}

	public static int[] getSupportedWhiteBalance()
	{
		return supportedWBModes;
	}

	public static List<String> getSupportedWhiteBalanceNames()
	{
		List<String> wbNames = new ArrayList<String>();
		for (int i : supportedWBModes)
		{
			wbNames.add(mode_wb.get(i));
		}
		return wbNames;
	}

	private static boolean getFocusModeSupported()
	{
		int[] supported_focus = getSupportedFocusModesInternal();
		return (supported_focus != null && (supported_focus.length > 1 || (supported_focus.length == 1 && supported_focus[0] != CameraParameters.AF_MODE_OFF)));
	}

	public static boolean isFocusModeSupported()
	{
		return mFocusModeSupported;
	}

	private static int[] getSupportedFocusModesInternal()
	{
		if (!CameraController.isRemoteCamera())
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
		} else
		{
			List<String> focusModes;
			focusModes = SonyRemoteCamera.getAvailableFocusModeRemote();

			if (focusModes != null)
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
		}
	}

	public static int[] getSupportedFocusModes()
	{
		return supportedFocusModes;
	}

	public static List<String> getSupportedFocusModesNames()
	{
		ArrayList<String> focusModes = new ArrayList<String>();
		int[] modes = getSupportedFocusModesInternal();
		for (int i : modes)
		{
			focusModes.add(mode_focus.get(i));
		}
		return focusModes;
	}

	private static boolean getFlashModeSupported()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (CameraController.isHALv3)
				return HALv3.isFlashModeSupportedHALv3();
			else
			{
				int[] supported_flash = getSupportedFlashModesInternal();
				return supported_flash != null && supported_flash.length > 0;
			}
		} else
		{
			return SonyRemoteCamera.isFlashAvailableRemote();
		}
	}

	public static boolean isFlashModeSupported()
	{
		return mFlashModeSupported;
	}

	private static int[] getSupportedFlashModesInternal()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (CameraController.isHALv3)
			{
				if (isFlashModeSupported())
				{
					int[] flash = new int[5];
					flash[0] = CameraParameters.FLASH_MODE_AUTO;
					flash[1] = CameraParameters.FLASH_MODE_OFF;
					flash[2] = CameraParameters.FLASH_MODE_SINGLE;
					flash[3] = CameraParameters.FLASH_MODE_REDEYE;
					flash[4] = CameraParameters.FLASH_MODE_TORCH;
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
		} else
		{
			SonyRemoteCamera.getAvailableFlashModeRemote();
		}

		return new int[0];
	}

	public static int[] getSupportedFlashModes()
	{
		return supportedFlashModes;
	}

	public static List<String> getSupportedFlashModesNames()
	{
		ArrayList<String> flashModes = new ArrayList<String>();
		int[] modes = getSupportedFlashModesInternal();
		for (int i : modes)
		{
			flashModes.add(mode_flash.get(i));
		}
		return flashModes;
	}

	private static int[] getSupportedCollorEffectsInternal()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				List<String> collorEffects;
				if (cameraParameters != null)
				{
					collorEffects = cameraParameters.getSupportedColorEffects();
				} else
				{
					collorEffects = camera.getParameters().getSupportedColorEffects();
				}

				if (camera != null && collorEffects != null)
				{
					Set<String> known_collor_effects = CameraController.key_color_effect.keySet();
					collorEffects.retainAll(known_collor_effects);
					int[] collorEffect = new int[collorEffects.size()];
					for (int i = 0; i < collorEffects.size(); i++)
					{
						String mode = collorEffects.get(i);
						if (CameraController.key_color_effect.containsKey(mode))
							collorEffect[i] = CameraController.key_color_effect.get(mode).byteValue();
					}

					return collorEffect;
				}

				return new int[] { 0 };
			} else
				return HALv3.getSupportedCollorEffectsHALv3();
		} else
		{
			return new int[] { 0 };
		}
	}

	private static void fillCollorEffectNames()
	{
		ColorEffectsNamesList = new ArrayList<String>();
		for (int mode : supportedCollorEffects)
		{
			switch (mode)
			{
			case CameraParameters.COLOR_EFFECT_MODE_OFF:
				ColorEffectsNamesList.add(mainContext.getResources().getString(R.string.colorEffectOff));
				break;
			case CameraParameters.COLOR_EFFECT_MODE_AQUA:
				ColorEffectsNamesList.add(mainContext.getResources().getString(R.string.colorEffectAqua));
				break;
			case CameraParameters.COLOR_EFFECT_MODE_BLACKBOARD:
				ColorEffectsNamesList.add(mainContext.getResources().getString(R.string.colorEffectBlackboard));
				break;
			case CameraParameters.COLOR_EFFECT_MODE_MONO:
				ColorEffectsNamesList.add(mainContext.getResources().getString(R.string.colorEffectMono));
				break;
			case CameraParameters.COLOR_EFFECT_MODE_NEGATIVE:
				ColorEffectsNamesList.add(mainContext.getResources().getString(R.string.colorEffectNegative));
				break;
			case CameraParameters.COLOR_EFFECT_MODE_POSTERIZE:
				ColorEffectsNamesList.add(mainContext.getResources().getString(R.string.colorEffectPosterize));
				break;
			case CameraParameters.COLOR_EFFECT_MODE_SEPIA:
				ColorEffectsNamesList.add(mainContext.getResources().getString(R.string.colorEffectSepia));
				break;
			case CameraParameters.COLOR_EFFECT_MODE_SOLARIZE:
				ColorEffectsNamesList.add(mainContext.getResources().getString(R.string.colorEffectSolarize));
				break;
			case CameraParameters.COLOR_EFFECT_MODE_WHITEBOARD:
				ColorEffectsNamesList.add(mainContext.getResources().getString(R.string.colorEffectWhiteboard));
				break;
			}
		}
	}

	private static boolean getCollorEffectSupported()
	{
		int[] supported_collor_effect = getSupportedCollorEffectsInternal();
		return (supported_collor_effect != null && supported_collor_effect.length > 1);
	}

	public static boolean isColorEffectSupported()
	{
		return mCollorEffectSupported;
	}

	public static int[] getSupportedColorEffects()
	{
		return supportedCollorEffects;
	}

	public static List<String> getSupportedColorEffectsNames()
	{
		ArrayList<String> collorEffects = new ArrayList<String>();
		int[] modes = supportedCollorEffects;
		for (int i : modes)
		{
			collorEffects.add(mode_color_effect.get(i));
		}
		return collorEffects;
	}

	private static boolean getISOSupported()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				int[] supported_iso = getSupportedISOInternal();
				String isoSystem = CameraController.getCameraParameters().get("iso");
				String isoSystem2 = CameraController.getCameraParameters().get("iso-speed");
				return supported_iso.length > 0 || isoSystem != null || isoSystem2 != null;
			} else
				return HALv3.isISOModeSupportedHALv3();
		} else
		{
			return SonyRemoteCamera.isISOModeAvailableRemote();
		}
	}

	public static boolean isISOSupported()
	{
		return mISOSupported;
	}

	private static int[] getSupportedISOInternal()
	{
		if (!CameraController.isRemoteCamera())
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
						{
							iso[index++] = CameraController.key_iso.get(isoModes.get(i)).byteValue();
							isUseISO2Keys = false;
						} else if (CameraController.key_iso2.containsKey(mode))
						{
							iso[index++] = CameraController.key_iso2.get(isoModes.get(i)).byteValue();
							isUseISO2Keys = true;
						}

					}

					return iso;
				}

				return new int[0];
			} else
				return HALv3.getSupportedISOModesHALv3();
		} else
		{
			List<String> isoModes = SonyRemoteCamera.getAvailableIsoModeRemote();

			int supportedISOCount = 0;
			for (int i = 0; i < isoModes.size(); i++)
			{
				String mode = isoModes.get(i).toLowerCase();
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
				{
					iso[index++] = CameraController.key_iso.get(isoModes.get(i)).byteValue();
					isUseISO2Keys = false;
				} else if (CameraController.key_iso2.containsKey(mode))
				{
					iso[index++] = CameraController.key_iso2.get(isoModes.get(i)).byteValue();
					isUseISO2Keys = true;
				}

			}

			return iso;
		}
	}

	public static int[] getSupportedISO()
	{
		return supportedISOModes;
	}

	public static List<String> getSupportedISONames()
	{
		List<String> isoNames = new ArrayList<String>();
		for (int i : supportedISOModes)
		{
			isoNames.add(mode_iso.get(i));
		}
		return isoNames;
	}

	/*
	 * Manual sensor parameters: focus distance and exposure time + manual white balance. Available
	 * only in Camera2 mode.
	 */
	public static boolean isManualWhiteBalanceSupported()
	{
		if (CameraController.isHALv3)
			return HALv3.isManualWhiteBalanceSupportedHALv3();
		else
			return false;
	}
	
	public static boolean isManualFocusDistanceSupported()
	{
		if (CameraController.isHALv3)
			return isManualSensorSupported && HALv3.isManualFocusDistanceSupportedHALv3();
		else
			return false;
	}

	public static float getMinimumFocusDistance()
	{
		if (CameraController.isHALv3)
			return HALv3.getCameraMinimumFocusDistance();
		else
			return 0;
	}

	public static boolean isManualExposureTimeSupported()
	{
		if (CameraController.isHALv3)
		{
			if (isManualSensorSupported && (getMinimumExposureTime() != getMaximumExposureTime()))
				return true;

			return false;
		} else
			return false;
	}

	public static long getMinimumExposureTime()
	{
		if (CameraController.isHALv3)
			return HALv3.getCameraMinimumExposureTime();
		else
			return 0;
	}

	public static long getMaximumExposureTime()
	{
		if (CameraController.isHALv3)
			return HALv3.getCameraMaximumExposureTime();
		else
			return 0;
	}

	public static int getMaxNumMeteringAreas()
	{
		try
		{
			if (CameraController.isHALv3)
				return HALv3.getMaxNumMeteringAreasHALv3();
			else if (camera != null)
			{
				Camera.Parameters camParams = camera.getParameters();
				return camParams.getMaxNumMeteringAreas();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
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

	public static int getMaxFocusAreasSupported()
	{
		return maxFocusRegionsSupported;
	}

	public static int getMaxMeteringAreasSupported()
	{
		return maxMeteringRegionsSupported;
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

	public static void setCameraImageSizeIndex(int captureIndex)
	{
		CapIdx = captureIndex;
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
		if (!CameraController.isHALv3)
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
		if (!CameraController.isRemoteCamera())
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
				return appInterface.getSceneModePref();
		} else
		{
			return -1;
		}

		return -1;
	}

	public static int getWBMode()
	{
		if (!isRemoteCamera())
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
				return appInterface.getWBModePref();
		} else
		{
			return CameraController.key_wb_sony_remote.get(SonyRemoteCamera.getWhiteBalanceRemote());
		}

		return -1;
	}

	public static int getFocusMode()
	{

		if (!CameraController.isRemoteCamera())
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
				return appInterface.getFocusModePref(-1);
		} else
		{
			return -1;
		}

		return -1;
	}

	public static int getFlashMode()
	{
		if (!CameraController.isRemoteCamera())
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
				return appInterface.getFlashModePref(-1);
		} else
		{
			return CameraController.key_flash.get(SonyRemoteCamera.getFlashModeRemote());
		}

		return -1;
	}

	public static int getISOMode()
	{
		if (!CameraController.isRemoteCamera())
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
				return appInterface.getISOModePref(-1);
		} else
		{
			return CameraController.key_iso2.get(SonyRemoteCamera.getIsoModeRemote());
		}

		return -1;
	}

	public static int getCurrentSensitivity()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				return -1;
			} else
			{
				return HALv3.getCameraCurrentSensitivityHALv3();
			}
		} else
		{
			return -1;
		}
	}

	public static void setCameraSceneMode(int mode)
	{
		if (!CameraController.isRemoteCamera())
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
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} else
				HALv3.setCameraSceneModeHALv3(mode);
		}
	}

	public static void setCameraWhiteBalance(int mode)
	{
		if (!isRemoteCamera())
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
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} else
				HALv3.setCameraWhiteBalanceHALv3(mode);
		} else
		{
			SonyRemoteCamera.setWhiteBalanceRemote(CameraController.mode_wb_sony_remote.get(mode));
		}
	}
	
	@TargetApi(21)
	public static void setCameraColorTemperature(int iTemp)
	{
		if (CameraController.isHALv3)
			HALv3.setCameraColorTemperatureHALv3(iTemp);
	}

	public static void setCameraFocusMode(int mode)
	{
		if (!CameraController.isRemoteCamera())
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
							appInterface.setAutoFocusLock(false);
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} else
				HALv3.setCameraFocusModeHALv3(mode);
		} else
		{
			// sony
		}
	}

	public static void setCameraFlashMode(final int mode)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera != null)
				{
					try
					{
						final Camera.Parameters params = camera.getParameters();
						if (params != null)
						{
							String currentFlash = params.getFlashMode();
							if (currentFlash.equals(flashTorch))
							{
								params.setFlashMode(flashOff);
								setCameraParameters(params);
								Handler handler = new Handler();
								handler.postDelayed(new Runnable()
								{
									@Override
									public void run()
									{
										String flashmode = CameraController.mode_flash.get(mode);
										params.setFlashMode(flashmode);
										setCameraParameters(params);
									}
								}, 50);
							} else
							{
								String flashmode = CameraController.mode_flash.get(mode);
								if (flashmode == null)
									return;
								params.setFlashMode(flashmode);
								setCameraParameters(params);
							}
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} else
				HALv3.setCameraFlashModeHALv3(mode);
		} else
		{
			SonyRemoteCamera.setFlashModeRemote(CameraController.mode_flash.get(mode));
		}
	}

	public static void setCameraISO(int mode)
	{
		if (!isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				if (camera != null)
				{
					// boolean isSpecialDevice = Build.MODEL.contains("SM-N910")
					// || Build.MODEL.contains("ALCATEL ONE TOUCH");
					Camera.Parameters params = camera.getParameters();
					if (params != null)
					{
						String iso = isUseISO2Keys ? CameraController.mode_iso2.get(mode) : CameraController.mode_iso
								.get(mode);
						if (params.get(CameraParameters.isoParam) != null)
							params.set(CameraParameters.isoParam, iso);
						else if (params.get(CameraParameters.isoParam2) != null)
							params.set(CameraParameters.isoParam2, iso);
						else if (params.get(CameraParameters.isoParam3) != null)
							params.set(CameraParameters.isoParam3, iso);
						else
							params.set(CameraParameters.isoParam, iso);
						if (!setCameraParameters(params))
						{
							iso = isUseISO2Keys ? CameraController.mode_iso.get(mode) : CameraController.mode_iso2
									.get(mode);
							if (params.get(CameraParameters.isoParam) != null)
								params.set(CameraParameters.isoParam, iso);
							else if (params.get(CameraParameters.isoParam2) != null)
								params.set(CameraParameters.isoParam2, iso);
							else if (params.get(CameraParameters.isoParam3) != null)
								params.set(CameraParameters.isoParam3, iso);
							else
								params.set(CameraParameters.isoParam, iso);

							setCameraParameters(params);
						}
					}
				}
			} else
				HALv3.setCameraISOModeHALv3(mode);
		} else
		{
			SonyRemoteCamera.setIsoSpeedRateRemote(CameraController.mode_iso2.get(mode));
		}
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
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static void setCameraExposureCompensation(int iEV)
	{
		if (!isRemoteCamera())
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
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} else
				HALv3.setCameraExposureCompensationHALv3(iEV);
		} else
		{
			SonyRemoteCamera.setExposureCompensationRemote(iEV);
		}

		sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_EV_CHANGED);
	}

	public static void setCameraExposureTime(long iTime)
	{
		if (CameraController.isHALv3)
		{
			HALv3.setCameraExposureTimeHALv3(iTime);
		}
	}

	public static long getCameraExposureTime()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				return -1;
			} else
			{
				return HALv3.getCameraCurrentExposureHALv3();
			}
		} else
		{
			return -1;
		}
	}

	public static void resetCameraAEMode()
	{
		if (CameraController.isHALv3)
		{
			HALv3.resetCameraAEModeHALv3();
		}
	}

	public static void setCameraFocusDistance(float fDistance)
	{
		if (CameraController.isHALv3)
		{
			HALv3.setCameraFocusDistanceHALv3(fDistance);
		}
	}

	public static void setCameraFocusAreas(List<Area> focusAreas)
	{
		if (!CameraController.isRemoteCamera())
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
		} else
		{
			SonyRemoteCamera.setCameraFocusAreasSonyRemote(focusAreas);
		}
	}

	public static void setCameraMeteringAreas(List<Area> meteringAreas)
	{
		if (!CameraController.isRemoteCamera())
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
							params.setMeteringAreas(meteringAreas);
							cameraController.setCameraParameters(params);
						}
					} catch (RuntimeException e)
					{
						Log.e(TAG, e.getMessage());
					}
				}
			} else
				HALv3.setCameraMeteringAreasHALv3(meteringAreas);
		}
	}

	public static void setFocusState(int state)
	{
		if (state != CameraController.FOCUS_STATE_IDLE && state != CameraController.FOCUS_STATE_FOCUSED
				&& state != CameraController.FOCUS_STATE_FAIL)
			return;

		mFocusState = state;

		sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_FOCUS_STATE_CHANGED);
	}

	public static void setCameraColorEffect(int effect)
	{
		if (!CameraController.isRemoteCamera())
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
							String collorEffect = CameraController.mode_color_effect.get(effect);
							params.setColorEffect(collorEffect);
							setCameraParameters(params);
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
				}
			} else
				HALv3.setCameraCollorEffectHALv3(effect);
		} else
		{
			// Nothing to do for Sony
		}
	}

	public static int getFocusState()
	{
		return mFocusState;
	}

	public static boolean isAutoFocusPerform()
	{
		int focusMode = CameraController.getFocusMode();
		if (CameraController.isFocusModeSupported()
				&& focusMode != -1
				&& (CameraController.getFocusState() == CameraController.FOCUS_STATE_IDLE || CameraController
						.getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
				&& !(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
						|| focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
						|| focusMode == CameraParameters.AF_MODE_INFINITY
						|| focusMode == CameraParameters.AF_MODE_FIXED || focusMode == CameraParameters.AF_MODE_EDOF || focusMode == CameraParameters.MF_MODE)
				&& !ApplicationScreen.instance.getAutoFocusLock())
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
		if (!CameraController.isRemoteCamera())
		{
			final Camera.Parameters cp = getCameraParameters();
			if (cp == null)
			{
				return;
			}

			cp.setPictureSize(width, height);
			setCameraParameters(cp);
		} else
		{
			SonyRemoteCamera.setPictureSizeRemote(width, height);
		}
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
			// LG G Flex 2.
			if (Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h959")
					|| Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h510")
					|| Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-f510k"))
			{
				return 60.808907f;
			}

			if (camera != null)
				return camera.getParameters().getHorizontalViewAngle();
		} else
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
			// LG G Flex 2.
			if (Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h959")
					|| Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h510")
					|| Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-f510k"))
			{
				return 47.50866f;
			}

			if (camera != null)
				return camera.getParameters().getVerticalViewAngle();
		} else
		{
			return HALv3.getVerticalViewAngle();
		}

		if (Build.MODEL.contains("Nexus"))
			return 46.66f;

		return 42.7f;
	}

	public static int getSensorOrientation()
	{
		if (!isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
				Camera.getCameraInfo(CameraIndex, cameraInfo);
				return cameraInfo.orientation;
			} else
				return HALv3.getInstance().getSensorOrientation();
		} else
		{
			return -1;
		}
	}

	// CAMERA PARAMETERS AND CAPABILITIES
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

	protected static boolean	playShutterSound	= false;

	public static void startVideoRecordingSonyRemote()
	{
		SonyRemoteCamera.startMovieRec();
	}

	public static void stopVideoRecordingSonyRemote()
	{
		SonyRemoteCamera.stopMovieRec();
	}

	// Note: per-frame 'gain' and 'exposure' parameters are only effective for
	// Camera2 API at the moment
	public static int captureImagesWithParams(int nFrames, int format, int[] pause, int[] evRequested, int[] gain,
			long[] exposure, boolean resInHeap, boolean playSound)
	{
		pauseBetweenShots = pause;
		evValues = evRequested;

		total_frames = nFrames;
		frame_num = 0;
		frameFormat = format;

		resultInHeap = resInHeap;

		previewWorking = false;
		cdt = null;

		playShutterSound = playSound;

		if (!isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				takeYUVFrame = (format == CameraController.YUV) || (format == CameraController.YUV_RAW);
				if (evRequested != null && evRequested.length >= total_frames)
					CameraController.setExposure();
				else
				{
					if (CameraController.getFocusMode() == CameraParameters.AF_MODE_CONTINUOUS_PICTURE)
					{
						camera.autoFocus(new AutoFocusCallback()
						{
							@Override
							public void onAutoFocus(boolean success, Camera camera)
							{
								CameraController.takeImage();
							}
						});
					} else
					{
						CameraController.takeImage();
					}
				}
				return 0;
			} else
				return HALv3.captureImageWithParamsHALv3(nFrames, format, pause, evRequested, gain, exposure,
						resultInHeap, playShutterSound);
		} else
		{
			takeYUVFrame = (format == CameraController.YUV) || (format == CameraController.YUV_RAW);
			CameraController.takeImageSonyRemote();
			return 0;
		}
	}

	public static void forceFocus()
	{
		if (CameraController.isHALv3)
		{
			HALv3.forceFocusHALv3();
		}
	}

	public static boolean autoFocus(Camera.AutoFocusCallback listener)
	{
		synchronized (SYNC_OBJECT)
		{
			if (!CameraController.isRemoteCamera())
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
			} else
			{
				if (CameraController.getFocusState() == CameraController.FOCUS_STATE_IDLE)
				{
					CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
					return SonyRemoteCamera.autoFocusSonyRemote();
				}
			}

			return false;
		}
	}

	public static boolean autoFocus()
	{
		synchronized (SYNC_OBJECT)
		{
			if (!CameraController.isRemoteCamera())
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
								Camera.Parameters params = CameraController.getCameraParameters();
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
			} else
			{
				if (CameraController.getFocusState() == CameraController.FOCUS_STATE_IDLE
						|| CameraController.getFocusState() == CameraController.FOCUS_STATE_FOCUSED
						|| CameraController.getFocusState() == CameraController.FOCUS_STATE_FAIL)
				{
					CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
					return SonyRemoteCamera.autoFocusSonyRemote();
				}
			}

			return false;
		}
	}

	public static void cancelAutoFocus()
	{
		CameraController.setFocusState(CameraController.FOCUS_STATE_IDLE);
		if (!CameraController.isRemoteCamera())
		{
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
		} else
		{
			SonyRemoteCamera.cancelAutoFocusSonyRemote();
		}
	}

	// Callback always contains JPEG frame.
	// So, we have to convert JPEG to YUV if capture plugin has requested YUV
	// frame.
	@Override
	public void onPictureTakenSonyRemote(byte[] paramArrayOfByte, boolean fromRequest)
	{
		Log.d(TAG, "onPictureTaken Sony remote");

		if (fromRequest)
		{
			pluginManager.collectExifData(paramArrayOfByte);
			if (!CameraController.takeYUVFrame) // if JPEG frame requested
			{
				int frame = 0;
				if (resultInHeap)
					frame = SwapHeap.SwapToHeap(paramArrayOfByte);
				pluginManager.onImageTaken(frame, paramArrayOfByte, paramArrayOfByte.length, CameraController.JPEG);
			} else
			// is YUV frame requested
			{
				int yuvFrame = ImageConversion.JpegConvert(paramArrayOfByte, imageSize.getWidth(),
						imageSize.getHeight(), false, false, 0);
				int frameLen = imageSize.getWidth() * imageSize.getHeight() + 2 * ((imageSize.getWidth() + 1) / 2)
						* ((imageSize.getHeight() + 1) / 2);

				byte[] frameData = null;
				if (!resultInHeap)
				{
					frameData = SwapHeap.SwapFromHeap(yuvFrame, frameLen);
					yuvFrame = 0;
				}

				pluginManager.onImageTaken(yuvFrame, frameData, frameLen, CameraController.YUV);
			}
		} else
		{
			pluginManager.collectExifData(paramArrayOfByte);
			int frame = SwapHeap.SwapToHeap(paramArrayOfByte);
			pluginManager.onImageTaken(frame, paramArrayOfByte, paramArrayOfByte.length, CameraController.JPEG);
		}

		CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;
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

		pluginManager.collectExifData(paramArrayOfByte);
		if (!CameraController.takeYUVFrame) // if JPEG frame requested
		{

			int frame = 0;
			if (resultInHeap)
				frame = SwapHeap.SwapToHeap(paramArrayOfByte);
			pluginManager.onImageTaken(frame, paramArrayOfByte, paramArrayOfByte.length, CameraController.JPEG);
		} else
		// is YUV frame requested
		{
			int yuvFrame = ImageConversion.JpegConvert(paramArrayOfByte, imageSize.getWidth(), imageSize.getHeight(),
					false, false, 0);
			int frameLen = imageSize.getWidth() * imageSize.getHeight() + 2 * ((imageSize.getWidth() + 1) / 2)
					* ((imageSize.getHeight() + 1) / 2);

			byte[] frameData = null;
			if (!resultInHeap)
			{
				frameData = SwapHeap.SwapFromHeap(yuvFrame, frameLen);
				yuvFrame = 0;
			}

			pluginManager.onImageTaken(yuvFrame, frameData, frameLen, CameraController.YUV);
		}

		try
		{
			CameraController.startCameraPreview();
		} catch (RuntimeException e)
		{
			appInterface.captureFailed();
			CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;
			return;
		}
		CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;

		nextFrame();

		if (pluginManager.isPreviewDependentMode() && (frame_num < total_frames))
		{
			// if preview not working
			if (previewMode == false)
				return;
			previewWorking = false;
			// start timer to check if onpreviewframe working
			cdt = new CountDownTimer(5000, 5000)
			{
				public void onTick(long millisUntilFinished)
				{
				}

				public void onFinish()
				{
					if (!previewWorking)
					{
						Log.d(TAG, "previewMode DISABLED!");
						previewMode = false;
						appInterface.setExpoPreviewPref(previewMode);
						evLatency = 0;
						CameraController.takeImage();
					}
				}
			};
			cdt.start();
		}
	}

	private class DecodeToYUVFrameTask extends AsyncTask<byte[], Void, Void>
	{
		int		yuvFrame	= 0;
		int		frameLen	= 0;
		byte[]	frameData	= null;

		@Override
		protected Void doInBackground(byte[]... params)
		{
			byte[] paramArrayOfByte = params[0];
			yuvFrame = ImageConversion.JpegConvert(paramArrayOfByte, imageSize.getWidth(), imageSize.getHeight(),
					false, false, 0);
			frameLen = imageSize.getWidth() * imageSize.getHeight() + 2 * ((imageSize.getWidth() + 1) / 2)
					* ((imageSize.getHeight() + 1) / 2);

			frameData = null;
			if (!resultInHeap)
			{
				frameData = SwapHeap.SwapFromHeap(yuvFrame, frameLen);
				yuvFrame = 0;
			}
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

	public static void onAutoFocusMoving(boolean start)
	{
		pluginManager.onAutoFocusMoving(start);
		if (start)
			CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSING);
		else
			CameraController.setFocusState(CameraController.FOCUS_STATE_FOCUSED);
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

				pluginManager.collectExifData(null);
				pluginManager.onImageTaken(frame, data, dataLenght, CameraController.YUV);
			} else
			{
				int jpegData = 0;
			}
			nextFrame();
			return;
		}

		if (pluginManager.isPreviewDependentMode() && evLatency > 0)
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
				CameraController.takeImage();
			}
			return;
		}
	}

	public static void setPreviewCallbackWithBuffer()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isHALv3)
			{
				CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
				CameraController.getCamera().addCallbackBuffer(CameraController.pviewBuffer);
			}
		}
	}

	//plugin has to set it to TRUE if need preview frames
	public static void setNeedPreviewFrame(boolean needPreviewFrame)
	{
		if(CameraController.isUseHALv3())
			HALv3.setNeedPreviewFrame(needPreviewFrame);
	}
	
	//should be reset on each changemode and on resume (call)
	public static void resetNeedPreviewFrame()
	{
		if(CameraController.isUseHALv3())
			HALv3.resetNeedPreviewFrame();
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

	// Handle messages only for old camera interface logic
	@Override
	public boolean handleMessage(Message msg)
	{
		return true;
	}

	private static void takeImage()
	{
		Log.e(TAG, "takeImage called");
		synchronized (SYNC_OBJECT)
		{
			if (imageSize == null)
			{
				sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED_NORESULT, null);
				return;
			}

			int imageWidth = imageSize.getWidth();
			int imageHeight = imageSize.getHeight();

			// play tick sound
			appInterface.showCaptureIndication(playShutterSound);

			lastCaptureStarted = SystemClock.uptimeMillis();
			if (imageWidth == iPreviewWidth && imageHeight == iPreviewHeight
					&& ((frameFormat == CameraController.YUV) || (frameFormat == CameraController.YUV_RAW)))
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
				} catch (Exception exp)
				{
					previewWorking = true;
					if (cdt != null)
					{
						cdt.cancel();
						cdt = null;
					}

					Log.e(TAG, "takePicture exception. Message: " + exp.getMessage());
					exp.printStackTrace();
				}
			}
		}
	}

	private static void nextFrame()
	{
		Log.d(TAG, "MSG_NEXT_FRAME");
		if (++frame_num < total_frames)
		{
			if (pauseBetweenShots == null || Array.getLength(pauseBetweenShots) < frame_num)
			{
				if (evValues != null && evValues.length >= total_frames)
					CameraController.setExposure();
				else
					CameraController.takeImage();
			} else
			{
				pauseHandler.postDelayed(new Runnable()
				{
					public void run()
					{
						if (evValues != null && evValues.length >= total_frames)
							CameraController.setExposure();
						else
							CameraController.takeImage();
					}
				}, pauseBetweenShots[frame_num] - (SystemClock.uptimeMillis() - lastCaptureStarted));
			}
		} else if (pluginManager.isPreviewDependentMode())
		{
			previewWorking = true;
			if (cdt != null)
			{
				cdt.cancel();
				cdt = null;
			}
		}
	}

	private static void setExposure()
	{
		try
		{
			if (evValues != null && evValues.length > frame_num)
				CameraController.setCameraExposureCompensation(evValues[frame_num]);
		} catch (RuntimeException e)
		{
			Log.e(TAG, "setExpo fail in MSG_SET_EXPOSURE");
		}

		if (pluginManager.isPreviewDependentMode() && previewMode)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
			// if true - evLatency will be doubled.
			boolean isSlow = prefs.getBoolean("PreferenceExpoSlow", false);

			// Note 3 & LG G3 need more time to change exposure.
			if (Build.MODEL.contains("SM-N900") || Build.MODEL.contains("SM-N910"))
				evLatency = 20 * (isSlow ? 2 : 1);
			else if (Build.MODEL.contains("LG-D855"))
				evLatency = 30 * (isSlow ? 2 : 1);
			else
			{
				// message to capture image will be emitted a few frames after
				// setExposure
				evLatency = 10 * (isSlow ? 2 : 1);// the minimum value at which
													// Galaxy Nexus is
													// changing exposure in a
													// stable way
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
					CameraController.takeImage();
				}
			}.start();
		}
	}

	private static void takeImageSonyRemote()
	{
		Log.e(TAG, "takeImage called");
		synchronized (SYNC_OBJECT)
		{
			if (imageSize == null)
			{
				sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED_NORESULT, null);
				return;
			}

			int imageWidth = imageSize.getWidth();
			int imageHeight = imageSize.getHeight();

			// play tick sound
			appInterface.showCaptureIndication(playShutterSound);

			lastCaptureStarted = SystemClock.uptimeMillis();

			mCaptureState = CameraController.CAPTURE_STATE_CAPTURING;
			// camera.setPreviewCallback(null);
			SonyRemoteCamera.takePicture(CameraController.getInstance());
		}
	}

	public static void sendMessage(int what, int arg1)
	{
		Message message = new Message();
		message.arg1 = arg1;
		message.what = what;
		messageHandler.sendMessage(message);
	}

	public static void sendMessage(int what, String obj)
	{
		Message message = new Message();
		message.obj = String.valueOf(obj);
		message.what = what;
		messageHandler.sendMessage(message);
	}

	/**
	 * Sets a target ServerDevice object.
	 * 
	 * @param device
	 */
	public static void setTargetServerDevice(ServerDevice device)
	{
		SonyRemoteCamera.setTargetServerDevice(device);
	}

	/**
	 * Returns a target ServerDevice object.
	 * 
	 * @return return ServiceDevice
	 */
	public static ServerDevice getTargetServerDevice()
	{
		return SonyRemoteCamera.getTargetServerDevice();
	}
}
