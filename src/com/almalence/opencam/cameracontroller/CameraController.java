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
import android.media.MediaRecorder;
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
	private static final String						TAG				= "CameraController";

	// YUV_RAW is the same as YUV (ie NV21) except that
	// noise filtering, edge enhancements and scaler
	// are disabled if possible
	public static final int							RAW				= 0x20;
	public static final int							YUV_RAW			= 0x22;
	public static final int							YUV				= 0x23;
	public static final int							JPEG			= 0x100;

	protected static final long						MPIX_1080		= 1920 * 1080;

	//Device models markers. Used to separate device dependent program's logic
	public static boolean							isNexus5x		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").equals("nexus5x");
	public static boolean							isNexus6p		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").equals("nexus6p");
	public static boolean							isNexus5		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").equals("nexus5");
	public static boolean							isNexus6		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").equals("nexus6");
	public static boolean							isNexus7		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("nexus7");
	public static boolean							isNexus9		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("nexus9");
	
	public static boolean							isNexus5or6		= CameraController.isNexus5  ||
																	  CameraController.isNexus5x ||
																	  CameraController.isNexus6  ||
			  														  CameraController.isNexus6p;
	
	public static boolean							isNexus			= CameraController.isNexus5or6 ||
																	  CameraController.isNexus7    ||
			  														  CameraController.isNexus9;

	public static boolean							isFlex2			= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h959") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-f510") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h955") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-as995")||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h950") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-us995")||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-ls996");

	public static boolean							isG4			= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h818")  ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h815")  ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h812")  ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h810")  ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h811")  ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-ls991") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-vs986") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-us991");
	
	public static boolean							isG3			= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-d85")   ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-d72")   ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-d69")   ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-vs985") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-ls990");
	
	public static boolean							isG2			= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-d80") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-vs980") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-ls980");

	public static boolean							isAndroidOne	= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("micromaxaq4501");

	public static boolean							isGalaxyS6		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("sm-g920") ||
														  			  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("sm-g925");
	
	public static boolean							isGalaxyS5		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("sm-g900");
	
	public static boolean							isGalaxyS4		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("gt-i95");
	
	public static boolean							isGalaxyS4Mini	= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("gt-i9190");
	
	public static boolean							isGalaxyNote3	= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("sm-n900") ||
																	  Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("sm-g900");
	
	public static boolean							isGalaxyNote4	= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("sm-n910");
	
	public static boolean							isHTCOne		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("htcone");
	public static boolean							isHTCOneX		= Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("htconex");
	
	public static boolean							isSony			= Build.BRAND.toLowerCase(Locale.US).replace(" ", "").contains("sony");
	
	public static boolean							isHuawei		= Build.BRAND.toLowerCase(Locale.US).replace(" ", "").contains("huawei");
	
	public static boolean							isGionee		= Build.BRAND.toLowerCase(Locale.US).replace(" ", "").contains("gionee");
	
	

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

	private static String							flashAuto;
	private static String							flashOn;
	private static String							flashOff;
	private static String							flashRedEye;
	private static String							flashTorch;
	private static String							flashCaptureTorch;

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
	private static Map<Integer, String>				mode_iso;
	private static Map<Integer, String>				mode_iso2;
	private static Map<Integer, Integer>			mode_iso_Camera2;
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

	//Buffer for preview frames. It used to increase preview fps.
	private static byte[]							pviewBuffer;

	// Message handler for multishot capturing with pause between shots
	// and different exposure compensations
	private static Handler							pauseHandler;

	//Variables used to force start application in Camera2 mode after working in Camera1 mode.
	//Non trivial logic of working in Camera1/2 modes used in several capture modes on some devices
	private static boolean							useCamera2OnRelaunch			= false;
	public static boolean							isOldCameraOneModeLaunched		= false;

	private static boolean							isCamera2						= false; //Flag of using camera2 interface
	private static boolean							isCamera2Allowed				= false; //Flag to show whether OpenCamera support camera2 mode on current device
	private static boolean							isCamera2Supported				= false; //Flag to show whether camera2 is available on current device
	protected static boolean						isRAWCaptureSupported			= false; //Only HARDWARE_LEVEL_FULL devices may capture RAW frames
	protected static boolean						isManualSensorSupported			= false; //Manual sensor means user's control of white balance and exposure time
	
	public static boolean							openCameraWaiting				= false; //TODO: Requesting Permissions at runtime

	protected static String[]						cameraIdList					= { "" }; //Id's of front and back cameras

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
	protected static boolean						mVideoSnapshotSupported	= false;

	private static int[]							supportedSceneModes;
	private static int[]							supportedWBModes;
	private static int[]							supportedFocusModes;
	private static int[]							supportedFlashModes;
	private static int[]							supportedCollorEffects;
	private static int[]							supportedISOModes;

	private static int								maxFocusRegionsSupported;
	private static int								maxMeteringRegionsSupported;

	protected static int							CameraIndex						= 0;
	protected static boolean						CameraMirrored					= false; //Front or back camera

	protected static int							mDisplayOrientation				= 0;

	private static int								CapIdx; // Index of image size in whole image sizes list

	private static Size								imageSize; // Current image size of frame to be captured

	private static int								iPreviewWidth;  //Size of preview frames
	private static int								iPreviewHeight;

	public static final int							MIN_MPIX_SUPPORTED				= 1280 * 720; //Image sizes less than this will be discarded from settings

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

	protected static final CharSequence[]			RATIO_STRINGS					= { " ", "4:3", "3:2", "16:9", "1:1"};

	// States of focus and capture
	public static final int							FOCUS_STATE_IDLE				= 0;
	public static final int							FOCUS_STATE_FOCUSED				= 1;
	public static final int							FOCUS_STATE_FAIL				= 3;
	public static final int							FOCUS_STATE_FOCUSING			= 4;

	public static final int							CAPTURE_STATE_IDLE				= 0;
	public static final int							CAPTURE_STATE_CAPTURING			= 1;

	private static int								mFocusState						= FOCUS_STATE_IDLE;
	private static int								mCaptureState					= CAPTURE_STATE_IDLE;

	//Used in onImageAvailable to separate frames for preview and still capture frames
	protected static Surface						mPreviewSurface					= null;

	//Synchronization object for takeImage and autoFocus methods
	private static final Object						SYNC_OBJECT						= new Object();

	// Singleton access function
	public static CameraController getInstance()
	{
		if (cameraController == null)
		{
			cameraController = new CameraController();
		}
		return cameraController;
	}

	private CameraController(){}

	public static void onCreate(Context context, ApplicationInterface app,
								PluginManagerInterface pluginManagerBase, Handler msgHandler)
	{
		pluginManager = pluginManagerBase;
		appInterface = app;
		mainContext = context;

		messageHandler = msgHandler;

		pauseHandler = new Handler(CameraController.getInstance());

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

		flashAuto = mainContext.getResources().getString(R.string.flashAutoSystem);
		flashOn = mainContext.getResources().getString(R.string.flashOnSystem);
		flashOff = mainContext.getResources().getString(R.string.flashOffSystem);
		flashRedEye = mainContext.getResources().getString(R.string.flashRedEyeSystem);
		flashTorch = mainContext.getResources().getString(R.string.flashTorchSystem);
		flashCaptureTorch = mainContext.getResources().getString(R.string.flashCaptureTorchSystem);

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
				put(CameraParameters.FLASH_MODE_CAPTURE_TORCH, flashCaptureTorch);
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
				put(flashCaptureTorch, CameraParameters.FLASH_MODE_CAPTURE_TORCH);
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

		mode_iso_Camera2 = new HashMap<Integer, Integer>()
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

		isCamera2 = prefs.getBoolean(mainContext.getResources().getString(R.string.Preference_UseCamera2Key), false);
		//At that time limited number of devices supports camera2 interface and not all capture modes may support it
		if (!pluginManager.isCamera2InterfaceAllowed())
			isCamera2 = false;

		try
		{
			//General support of camera2 interface. OpenCamera allows to use camera2 not on all devices which implements camera2
			if (Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT && mainContext.getSystemService(Context.CAMERA_SERVICE) != null)
				isCamera2Supported = true;
			
			//Now only LG Flex2, G4, Nexus 5\6 and Andoid One devices support camera2 without critical problems
			//We have to test Samsung Galaxy S6 to include in this list of allowed devices
			if (!(Build.VERSION.SDK_INT > Build.VERSION_CODES.KITKAT && mainContext.getSystemService(Context.CAMERA_SERVICE) != null)
					|| (!isFlex2 && !isNexus5or6 && !isAndroidOne  /*&& !isGalaxyS6 &&*/ /* && !isG4*/))
			{
				isCamera2 = false;
				isCamera2Allowed = false;
				prefs.edit().putBoolean(mainContext.getResources().getString(R.string.Preference_UseCamera2Key), false)
						.commit();
			} else
				isCamera2Allowed = true;
		} catch (Exception e)
		{
			e.printStackTrace();
			isCamera2 = false;
			isCamera2Allowed = false;
			isCamera2Supported = false;
			prefs.edit().putBoolean(mainContext.getResources().getString(R.string.Preference_UseCamera2Key), false)
					.commit();
		}

		if (CameraController.isCamera2Supported)
		{
			Camera2Controller.onCreateCamera2(mainContext, appInterface, pluginManager, messageHandler);
			//We supports only devices with hardware level FULL and LIMITED
			if (!Camera2Controller.checkHardwareLevel())
			{
				isCamera2 = false;
				isCamera2Allowed = false;
				prefs.edit().putBoolean(mainContext.getResources().getString(R.string.Preference_UseCamera2Key), false)
						.commit();
			}
		}

		SonyRemoteCamera.onCreateSonyRemoteCamera(mainContext, appInterface, pluginManager, messageHandler);
	}


	public static void onStart()
	{
		// Does nothing yet
	}

	public static void onResume()
	{
		if (!CameraController.isRemoteCamera())
		{
			//Some capture modes may use preview frames to choose good time for capturing.
			//Such modes is HDR - it count preview frames to let device complete exposure metering for next frame
			//If preview frames is not receiving, capture plugin use countdown timer to let exposure metering be completed
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

			if (CameraController.isCamera2Supported)
				Camera2Controller.onResumeCamera2();
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
			if (!CameraController.isCamera2)
			{
				//In case when flash mode was TORCH we have to manually switch it off
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
					//Sony devices, some Huawei devices and Samsung Galaxy S5 has unexpected behavior if preview isn't stopped until mode changes
					if (isSony || isGalaxyS5 || isHuawei || isGionee)
						camera.stopPreview();
					if (!isModeSwitching)
					{
						if (!isSony && !isGalaxyS5 && !isHuawei && !isGionee)
							camera.stopPreview();
						camera.release();
						camera = null;
					}
				}
			} else
				Camera2Controller.onPauseCamera2();
		} else
		{
			SonyRemoteCamera.onPauseSonyRemoteCamera();
		}

		CameraController.mCaptureState = CameraController.CAPTURE_STATE_IDLE;
	}

	public static void onStop()
	{
		//Specific case when application will re-start in camera2 mode
		//In future, probably, will be useless when all capture modes will support camera2 interface
		if (useCamera2OnRelaunch)
		{
			SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mainContext).edit();
			prefEditor.putBoolean(mainContext.getResources().getString(R.string.Preference_UseCamera2Key), true).commit();
		}

		if (!CameraController.isRemoteCamera() && CameraController.isCamera2)
			Camera2Controller.onStopCamera2();
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

	//List of supported ISO for camera2 interface
	public static Map<Integer, Integer> getIsoModeCamera2()
	{
		return mode_iso_Camera2;
	}

	/* ^^^ Get different list and maps of camera parameters */

	public static void setPreviewSurface(Surface srf)
	{
		mPreviewSurface = srf;
	}

	/* Preview buffer methods */
	public static void allocatePreviewBuffer(double size)
	{
		try
		{
			pviewBuffer = new byte[(int) Math.ceil(size)];
		}
		catch(OutOfMemoryError e)
		{
			e.printStackTrace();

		    System.gc();

		    try
		    {
		    	pviewBuffer = new byte[(int) Math.ceil(size)];
		    }
		    catch (OutOfMemoryError e2)
		    {
		      e2.printStackTrace();
		    }
		}
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

	public static void setUseCamera2(boolean useCamera2)
	{
		isCamera2 = useCamera2;
	}

	public static boolean isUseCamera2()
	{
		return isCamera2;
	}
	
	public static boolean isCamera2Allowed()
	{
		return isCamera2Allowed;
	}
	
	//returns camera2 supported level. -1 if not suppoerted.
	public static int getCamera2Level()
	{
		if (isCamera2Supported)
			return Camera2Controller.getHardwareLevel();
		return -1;
	}

	public static boolean isRAWCaptureSupported()
	{
		return isRAWCaptureSupported;
	}

	public static boolean isManualSensorSupported()
	{
		return isManualSensorSupported;
	}

	public static void useCamera2OnRelaunch(boolean useCamera2)
	{
		useCamera2OnRelaunch = useCamera2;
	}

	public static boolean isCamera2OnRelaunchUsed()
	{
		return useCamera2OnRelaunch;
	}

	public static boolean isSuperModePossible()
	{
		boolean SuperModeOk = false;

		if (isCamera2Allowed)
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
			if ((CameraController.isNexus5or6 || CameraController.isFlex2)
					&& (!CameraController.isNexus5x && !CameraController.isNexus6p)
					
					/*
																	 * ||
																	 * CameraController
																	 * .
																	 * isGalaxyS6
																	 */
										 /*|| CameraController.isG4*/)
				SuperModeOk = true;
		}

		return SuperModeOk;
	}

	//This code affects only Almalence's implementation of OpenCamera. It used to show Night or Super mode
	public static boolean isUseSuperMode()
	{
		return (isSuperModePossible() && isCamera2) || (isSuperModePossible() && isOldCameraOneModeLaunched);
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

	//Initial setup of camera device - open, configure orientation, fill and select capture frame size
	public static void setupCamera(SurfaceHolder holder, boolean openCamera)
	{
		if (!CameraController.isRemoteCamera())
		{
			// Devices camera setup
			if (!CameraController.isCamera2)
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
				
				cameraController.mVideoSnapshotSupported = getVideoSnapshotSupported();

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
			}
			else
			{
				 if(ApplicationScreen.isCameraPermissionGranted())
					 Camera2Controller.openCameraCamera2();
				 else
					openCameraWaiting = true; 
			}

			pluginManager.selectDefaults();

			CameraController.fillPreviewSizeList();
			CameraController.fillPictureSizeList();

			if (CameraController.isCamera2)
			{
				Camera2Controller.populateCameraDimensionsCamera2();
				Camera2Controller.populateCameraDimensionsForMultishotsCamera2();
			} else
			{
				populateCameraDimensions();
				populateCameraDimensionsForMultishots();
			}

			pluginManager.selectImageDimension(); // updates SX, SY values

			if (!CameraController.isCamera2)
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
		if (!CameraController.isCamera2)
			return camera != null;
		else
			return isCameraCreatedCamera2();

	}
	
	@TargetApi(21)
	public static boolean isCameraCreatedCamera2()
	{
		return Camera2Controller.getInstance().camDevice != null;
	}

	//In camera2 interface we may request several image formats to be captured.
	//CameraController is only provider of such info from camera2 management class
	public static boolean isCaptureFormatSupported(int captureFormat)
	{
		if (isUseCamera2())
			return Camera2Controller.isCaptureFormatSupported(captureFormat);
		else
			return true;
	}

	@TargetApi(21)
	public static void setCaptureFormat(int captureFormat)
	{
		Camera2Controller.setCaptureFormat(captureFormat);
	}

	@TargetApi(21)
	public static boolean createCaptureSession(List<Surface> sfl)
	{
		return Camera2Controller.createCaptureSession(sfl);
	}


	//Image sizes for preview
	private static void fillPreviewSizeList()
	{
		if (!CameraController.isRemoteCamera())
		{
			CameraController.SupportedPreviewSizesList = new ArrayList<CameraController.Size>();
			if (!isCamera2)
			{
				try
				{
					if (camera != null && camera.getParameters() != null)
					{
						List<Camera.Size> list = camera.getParameters().getSupportedPreviewSizes();
						if (list != null)
						{
							for (Camera.Size sz : list)
								CameraController.SupportedPreviewSizesList.add(new CameraController.Size(sz.width,
										sz.height));
						}
					}
				} catch (Exception e)
				{
					e.printStackTrace();
					return;
				}
				;
			} else
				CameraController.SupportedPreviewSizesList = Camera2Controller.getPreviewSizeList();
		} else
		{
			CameraController.SupportedPreviewSizesList = SonyRemoteCamera.getPreviewSizeListRemote();
		}
	}

	
	//Image sizes for still capturing
	private static void fillPictureSizeList()
	{
		CameraController.SupportedPictureSizesList = new ArrayList<CameraController.Size>();
		if (!isCamera2)
		{
			if (camera != null)
			{
				try
				{
					Camera.Parameters params = camera.getParameters();
					if(params != null)
					{
						List<Camera.Size> list = camera.getParameters().getSupportedPictureSizes();
						for (Camera.Size sz : list)
							CameraController.SupportedPictureSizesList.add(new CameraController.Size(sz.width, sz.height));
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					camera.release();
					camera = null;
					ApplicationScreen.instance.finish();
				}
			}
		} else
			Camera2Controller.fillPictureSizeList(CameraController.SupportedPictureSizesList);
	}

	//Fills not only supported Sizes list but also its Mega-pixel representation, indexes in list, user-friendly names
	//and indexes for 'fast' sizes - sizes of preview frames which may used as still capture images
	public static void populateCameraDimensions()
	{
		CameraController.ResolutionsMPixList = new ArrayList<Long>();
		CameraController.ResolutionsSizeList = new ArrayList<Size>();
		CameraController.ResolutionsIdxesList = new ArrayList<String>();
		CameraController.ResolutionsNamesList = new ArrayList<String>();
		CameraController.FastIdxelist = new ArrayList<Integer>();

		Camera.Parameters cp = getCameraParameters();
		if(cp == null)
			return;
		List<Camera.Size> cs = cp.getSupportedPictureSizes();

		if (cs == null)
			return;

		if (CameraController.isHTCOneX)
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

			//Drop buggy image size on some Samsung device
			if (CameraController.isGalaxyS4Mini && isFrontCamera() && (currSizeWidth * currSizeHeight == 1920 * 1080))
				continue;

			if ((long) currSizeWidth * currSizeHeight > (long) highestSizeWidth * highestSizeHeight)
			{
				sHighest = s;
				iHighestIndex = ii;
			}

			if ((long) currSizeWidth * currSizeHeight < CameraController.MIN_MPIX_SUPPORTED)
				continue;

			addResolution(ii, currSizeWidth, currSizeHeight);
		}

		if (CameraController.ResolutionsNamesList.isEmpty())
		{
			Camera.Size s = cs.get(iHighestIndex);

			int currSizeWidth = s.width;
			int currSizeHeight = s.height;

			addResolution(0, currSizeWidth, currSizeHeight);
		}

		return;
	}

	//Fill image sizes lists for Sony cameras
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

			addResolution(ii, currSizeWidth, currSizeHeight);
		}

		if (CameraController.ResolutionsNamesList.isEmpty())
		{
			CameraController.Size s = SonyRemoteCamera.getPictureSizeListRemote().get(iHighestIndex);

			int currSizeWidth = s.getWidth();
			int currSizeHeight = s.getHeight();

			addResolution(0, currSizeWidth, currSizeHeight);
		}

		return;
	}

	protected static void addResolution(int ii, int currSizeWidth, int currSizeHeight)
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
			if (!CameraController.isCamera2)
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
				return Camera2Controller.getPreviewSizeList();
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
			if (!CameraController.isCamera2)
			{
				Camera.Parameters params = getCameraParameters();
				if (params != null)
				{
					params.setPreviewSize(iPreviewWidth, iPreviewHeight);
					setCameraParameters(params);
				}
			} else
			{
				//Camera2 interface doesn't have direct settings of camera preview size
				//Instead of this in camera2 interface we have to create ImageReaders of desired sizes
				Camera2Controller.setupImageReadersCamera2();
			}
		}
	}
	
//	public static void setupImageReadersCamera2()
//	{
//		if (!CameraController.isRemoteCamera() && CameraController.isCamera2)
//				Camera2Controller.setupImageReadersCamera2();
//	}

	//Setup camera logic in camera2 interface
	public static void setSurfaceHolderFixedSize(int width, int height)
	{
		if (CameraController.isCamera2)
		{
			ApplicationScreen.setSurfaceHolderSize(width, height);
		}
	}

	public static List<CameraController.Size> getSupportedPictureSizes()
	{
		List<CameraController.Size> pictureSizes = new ArrayList<CameraController.Size>();
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
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
				Camera2Controller.fillPictureSizeList(pictureSizes);
		} else
		{
			SonyRemoteCamera.fillPictureSizeListRemote(pictureSizes);
		}

		return pictureSizes;
	}

	public static List<CameraController.Size> getSupportedVideoSizes()
	{
		List<CameraController.Size> videoSizes = new ArrayList<CameraController.Size>();
		if (!CameraController.isCamera2)
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
		if (!CameraController.isCamera2)
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
				if (!CameraController.isCamera2)
				{
					if (camera != null && cameraParameters != null)
					{
						minExpoCompensation = cameraParameters.getMinExposureCompensation();
						maxExpoCompensation = cameraParameters.getMaxExposureCompensation();
						expoCompensationStep = cameraParameters.getExposureCompensationStep();
					}
				} else
				{
					minExpoCompensation = Camera2Controller.getMinExposureCompensationCamera2();
					maxExpoCompensation = Camera2Controller.getMaxExposureCompensationCamera2();
					expoCompensationStep = Camera2Controller.getExposureCompensationStepCamera2();
				}
			} else
			{
				minExpoCompensation = SonyRemoteCamera.getMinExposureCompensationRemote();
				maxExpoCompensation = SonyRemoteCamera.getMaxExposureCompensationRemote();
				expoCompensationStep = SonyRemoteCamera.getExposureCompensationStepRemote();
			}

			
			//There may be not all supported modes by devices but only those which supported by OpenCamera
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
	public void onError(int arg0, Camera arg1){}

	
	
	// ------------ CAMERA PARAMETERS AND CAPABILITIES SECTION----------------
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
		return Camera2Controller.getCamera2();
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

	//In camera2 interface CameraCharacteristics is quit the same as Camera.Parameters in camera interface
	@TargetApi(21)
	public static CameraCharacteristics getCameraCharacteristics()
	{
		return Camera2Controller.getCameraCharacteristics();
	}

	public static void startCameraPreview()
	{
		if (camera != null)
		{
			camera.startPreview();

			//Nexus 4 has a buggy behavior
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

	public static void setRecordingHint(boolean hint)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				if (camera != null && camera.getParameters() != null)
				{
					Camera.Parameters cp = CameraController.getCameraParameters();
					cp.setRecordingHint(hint);
					CameraController.setCameraParameters(cp);
				}
			}
		}
	}
	
	@TargetApi(15)
	public static void setVideoStabilization(boolean stabilization)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
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
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
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
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}
	
	public static boolean getVideoSnapshotSupported()
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
				{
					if (camera == null || (camera != null && camera.getParameters() == null))
						return false;

					return camera.getParameters().isVideoSnapshotSupported();
				} else
					return true;
			} else
			{
				return false;
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean isVideoStabilizationSupported()
	{
		return mVideoStabilizationSupported;
	}
	
	public static boolean isVideoSnapshotSupported()
	{
		return mVideoStabilizationSupported;
	}

	public static boolean isExposureLockSupported()
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
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
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean isExposureLocked()
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
				{
					if (camera == null || (camera != null && camera.getParameters() == null))
						return false;

					return camera.getParameters().getAutoExposureLock();
				} else
					return Camera2Controller.isExposureLocked();
			} else
			{
				return false;
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean setAutoExposureLock(boolean lock)
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
				{
					if (camera == null || (camera != null && camera.getParameters() == null))
						return false;

					Camera.Parameters params = camera.getParameters();
					params.setAutoExposureLock(lock);
					camera.setParameters(params);
				} else
					return Camera2Controller.setAutoExposureLock(lock);
			}
			else
				return false;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			return true;
		}
	}

	public static boolean isWhiteBalanceLockSupported()
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
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
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean isWhiteBalanceLocked()
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
				{
					if (camera == null || (camera != null && camera.getParameters() == null))
						return false;

					return camera.getParameters().getAutoWhiteBalanceLock();
				} else
					return Camera2Controller.isWhiteBalanceLocked();
			} else
			{
				return false;
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static boolean setAutoWhiteBalanceLock(boolean lock)
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
				{
					if (camera == null || (camera != null && camera.getParameters() == null))
						return false;

					Camera.Parameters params = camera.getParameters();
					params.setAutoWhiteBalanceLock(lock);
					camera.setParameters(params);
				} else
					return Camera2Controller.setAutoWhiteBalanceLock(lock);
			}
			else
				return false;
		} catch (Exception e)
		{
			e.printStackTrace();
		}
		finally
		{
			return true;
		}
	}

	public static boolean isZoomSupported()
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
				{
					if (null == camera || camera.getParameters() == null)
						return false;

					return camera.getParameters().isZoomSupported();
				} else
				{
					return Camera2Controller.isZoomSupportedCamera2();
				}
			} else
			{
				return SonyRemoteCamera.isZoomAvailable();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	public static float getMaxZoom()
	{
		if (!CameraController.isRemoteCamera())
		{
			try
			{
				if (!CameraController.isCamera2)
				{
					if (null == camera || camera.getParameters() == null)
						return 1;

					//In camera interface zoom range lay between 0 and maxZoom (not a scale factor!)
					return camera.getParameters().getMaxZoom();
				} else
				{
					//In camera2 interface zoom range lay between 1 and max scale factor
					return Camera2Controller.getMaxZoomCamera2();
				}
			} catch (Exception e)
			{
				e.printStackTrace();
				return 0;
			}
		} else
		{
			return 0;
		}
	}

	public static void setZoom(float value)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				Camera.Parameters cp = getCameraParameters();
				if (cp != null)
				{
					cp.setZoom((int)value);
					setCameraParameters(cp);
				}
			} else
				Camera2Controller.setZoom(value);
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
			if (!CameraController.isCamera2)
			{
				Camera.Parameters cp = getCameraParameters();
				return (cp.getZoom() / 10.0f + 1f);
			} else
				return Camera2Controller.getZoom();
		} else
		{
			return 0.0f;
		}
	}

	//Luma adoptation used in Expo-bracketing modes and on Qualcomm chipsets
	public static boolean isLumaAdaptationSupported()
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
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
		} catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
	}

	// Used to initialize internal variable
	private static boolean getExposureCompensationSupported()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
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
				return Camera2Controller.isExposureCompensationSupportedCamera2();
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

	//Look at google docs for details about exposure compensation logic
	public static float getExposureCompensation()
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
				{
					if (camera != null && camera.getParameters() != null)
					{
						Camera.Parameters cameraParameters = CameraController.getCamera().getParameters();

						return cameraParameters.getExposureCompensation()
								* cameraParameters.getExposureCompensationStep();
					} else
						return 0;
				} else
					return appInterface.getEVPref() * Camera2Controller.getExposureCompensationStepCamera2();
			} else
			{
				return SonyRemoteCamera.getExposureCompensationRemote()
						* SonyRemoteCamera.getExposureCompensationStepRemote();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
			return 0;
		}
	}

	public static void resetExposureCompensation()
	{
		try
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
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
					Camera2Controller.resetExposureCompensationCamera2();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private static boolean getSceneModeSupported()
	{
		int[] supported_scene = getSupportedSceneModesInternal();

		//If device supports only AUTO scene, we interpret it as scene mode not supported
		if (supported_scene != null && supported_scene.length == 1)
			return supported_scene[0] != CameraParameters.SCENE_MODE_AUTO;

		return supported_scene != null && supported_scene.length > 0;
	}

	public static boolean isSceneModeSupported()
	{
		return mSceneModeSupported;
	}

	//Returns not all supported scene modes but only those which is known by OpenCamera
	//Some device may have exotic scene modes which is not supported by GUI (no icons and names)
	//All of this is true and for other kinds of methods getSupported*****Internal()
	private static int[] getSupportedSceneModesInternal()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
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
				return Camera2Controller.getSupportedSceneModesCamera2();
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
			if (!CameraController.isCamera2)
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
				return Camera2Controller.getSupportedWhiteBalanceCamera2();
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
		//Some devices has only AF_MODE_OFF. That mode is useless for OpenCamera, so we ignore it
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
			if (!CameraController.isCamera2)
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
				return Camera2Controller.getSupportedFocusModesCamera2();
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
			if (CameraController.isCamera2)
				return Camera2Controller.isFlashModeSupportedCamera2();
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
			if (CameraController.isCamera2)
			{
				//In camera2 flash is managed not as other parameters. There is no method to get all supported flash modes
				if (isFlashModeSupported())
				{
					int[] flash = new int[6];
					flash[0] = CameraParameters.FLASH_MODE_AUTO;
					flash[1] = CameraParameters.FLASH_MODE_OFF;
					flash[2] = CameraParameters.FLASH_MODE_SINGLE;
					flash[3] = CameraParameters.FLASH_MODE_REDEYE;
					flash[4] = CameraParameters.FLASH_MODE_TORCH;
					flash[5] = CameraParameters.FLASH_MODE_CAPTURE_TORCH; //Artificial mode
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

				if (flashModes != null && flashModes.contains(CameraController.mode_flash.get(CameraParameters.FLASH_MODE_TORCH)))
				{
					// If TORCH mode is available, then also enable CAPTURE_TORCH.
					flashModes.add(CameraController.mode_flash.get(CameraParameters.FLASH_MODE_CAPTURE_TORCH));
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
			if (!CameraController.isCamera2)
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
				return Camera2Controller.getSupportedCollorEffectsCamera2();
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
			if (!CameraController.isCamera2)
			{
				int[] supported_iso = getSupportedISOInternal();
				//ISO is not documented by google, so different devices has in most cases 2 kind of parameter's name for current value of ISO
				String isoSystem = CameraController.getCameraParameters().get("iso");
				String isoSystem2 = CameraController.getCameraParameters().get("iso-speed");
				return supported_iso.length > 0 || isoSystem != null || isoSystem2 != null;
			} else
				return Camera2Controller.isISOModeSupportedCamera2();
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
			if (!CameraController.isCamera2)
			{
				if (camera != null)
				{
					//ISO is not documented by google,
					//so different devices has many parameter names for list of supported iso
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
				return Camera2Controller.getSupportedISOModesCamera2();
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
	 * Manual sensor parameters: focus distance and exposure time + manual white
	 * balance. Available only in Camera2 mode.
	 */
	public static boolean isManualWhiteBalanceSupported()
	{
		if (CameraController.isCamera2)
			return Camera2Controller.isManualWhiteBalanceSupportedCamera2();
		else
			return false;
	}

	public static boolean isManualFocusDistanceSupported()
	{
		if (CameraController.isCamera2)
			return isManualSensorSupported && Camera2Controller.isManualFocusDistanceSupportedCamera2();
		else
			return false;
	}

	public static float getMinimumFocusDistance()
	{
		if (CameraController.isCamera2)
			return Camera2Controller.getCameraMinimumFocusDistance();
		else
			return 0;
	}

	public static boolean isManualExposureTimeSupported()
	{
		if (CameraController.isCamera2)
		{
			if (isManualSensorSupported && (getMinimumExposureTime() != getMaximumExposureTime()))
				return true;

			return false;
		} else
			return false;
	}

	public static long getMinimumExposureTime()
	{
		if (CameraController.isCamera2)
			return Camera2Controller.getCameraMinimumExposureTime();
		else
			return 0;
	}

	public static long getMaximumExposureTime()
	{
		if (CameraController.isCamera2)
			return Camera2Controller.getCameraMaximumExposureTime();
		else
			return 0;
	}

	public static int getMaxNumMeteringAreas()
	{
		try
		{
			if (CameraController.isCamera2)
				return Camera2Controller.getMaxNumMeteringAreasCamera2();
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
		if (CameraController.isCamera2)
			return Camera2Controller.getMaxNumFocusAreasCamera2();
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
		if (CameraController.isUseCamera2())
			Camera2Controller.checkImageSize(imageSize);
	}

	public static Size getCameraImageSize()
	{
		return imageSize;
	}

	public static Size getMaxCameraImageSize(int captureFormat)
	{
		if (!CameraController.isCamera2)
			return imageSize;
		else
			return Camera2Controller.getMaxCameraImageSizeCamera2(captureFormat);
	}

	
	//Universal utility method for all parameters: scene mode, white balance, focus modes and etc.
	public static boolean isModeAvailable(int[] modeList, int mode)
	{
		boolean isAvailable = false;
		if(modeList != null && modeList.length > 0)
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

	
	/*
	 * get*****Mode - bunch of methods to get some current camera parameter
	 * in camera1 interface we may get parameter directly from camera object
	 * in camera2 interface at current time such information we can get only from SharedPreference
	 * in future maybe will be possible to get it directly from captureSession
	 */
	public static int getSceneMode()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
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
			if (!CameraController.isCamera2)
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
			if (!CameraController.isCamera2)
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
			if (!CameraController.isCamera2)
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
			if (!CameraController.isCamera2)
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

	
	//Method useful only in camera2 mode
	//This is not just an ISO setting but current real-time ISO value of received preview frame
	public static int getCurrentSensitivity()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				return -1;
			} else
			{
				return Camera2Controller.getCameraCurrentSensitivityCamera2();
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
			if (!CameraController.isCamera2)
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
				Camera2Controller.setCameraSceneModeCamera2(mode);
		}
	}

	public static void setCameraWhiteBalance(int mode)
	{
		if (!isRemoteCamera())
		{
			if (!CameraController.isCamera2)
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
				Camera2Controller.setCameraWhiteBalanceCamera2(mode);
		} else
		{
			SonyRemoteCamera.setWhiteBalanceRemote(CameraController.mode_wb_sony_remote.get(mode));
		}
	}

	@TargetApi(21)
	public static void setCameraColorTemperature(int iTemp)
	{
		if (CameraController.isCamera2)
			Camera2Controller.setCameraColorTemperatureCamera2(iTemp);
	}

	public static void setCameraFocusMode(int mode)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
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
				Camera2Controller.setCameraFocusModeCamera2(mode);
		} else
		{
			// sony
		}
	}

	public static void setCameraFlashMode(final int mode)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				if (camera != null)
				{
					try
					{
						final Camera.Parameters params = camera.getParameters();
						if (params != null)
						{
							String currentFlash = params.getFlashMode();

							// Nothing to do, if newMode and currentMode are
							// equals.
							if (currentFlash.equals(CameraController.mode_flash.get(mode)))
							{
								return;
							}

							//Sometimes torch mode isn't canceled without switching to MODE_OFF.
							//And that switching may take some time, so we used delayed setting of new mode
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
				Camera2Controller.setCameraFlashModeCamera2(mode);
		} else
		{
			SonyRemoteCamera.setFlashModeRemote(CameraController.mode_flash.get(mode));
		}
	}

	public static void setCameraISO(int mode)
	{
		if (!isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				if (camera != null)
				{
					//ISO isn't documented by google, so quite all devices has different parameter names and values for ISO
					//There we iterate all known options for ISO
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
				Camera2Controller.setCameraISOModeCamera2(mode);
		} else
		{
			SonyRemoteCamera.setIsoSpeedRateRemote(CameraController.mode_iso2.get(mode));
		}
	}

	
	//May not work on all devices, coz it not documented by google
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
			if (!CameraController.isCamera2)
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
				Camera2Controller.setCameraExposureCompensationCamera2(iEV);
		} else
		{
			SonyRemoteCamera.setExposureCompensationRemote(iEV);
		}

		sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_EV_CHANGED);
	}

	
	//Manual exposure time available only in camera2 mode
	public static void setCameraExposureTime(long iTime)
	{
		if (CameraController.isCamera2)
		{
			Camera2Controller.setCameraExposureTimeCamera2(iTime);
		}
	}

	public static long getCameraExposureTime()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				return -1;
			} else
			{
				return Camera2Controller.getCameraCurrentExposureCamera2();
			}
		} else
		{
			return -1;
		}
	}

	//Actually creates new preview request without metering areas
	public static void resetCameraAEMode()
	{
		if (CameraController.isCamera2)
		{
			Camera2Controller.resetCameraAEModeCamera2();
		}
	}

	//Manual focus distance available only in camera2 mode
	public static void setCameraFocusDistance(float fDistance)
	{
		if (CameraController.isCamera2)
		{
			Camera2Controller.setCameraFocusDistanceCamera2(fDistance);
		}
	}

	public static void setCameraFocusAreas(List<Area> focusAreas)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
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
				Camera2Controller.setCameraFocusAreasCamera2(focusAreas);
		} else
		{
			SonyRemoteCamera.setCameraFocusAreasSonyRemote(focusAreas);
		}
	}

	public static void setCameraMeteringAreas(List<Area> meteringAreas)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
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
				Camera2Controller.setCameraMeteringAreasCamera2(meteringAreas);
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
			if (!CameraController.isCamera2)
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
				Camera2Controller.setCameraCollorEffectCamera2(effect);
		} else
		{
			// Nothing to do for Sony
		}
	}

	public static int getFocusState()
	{
		return mFocusState;
	}

	
	//Method used by capture plugins after shutter button click performed to decide
	//to start image capturing or to wait onAutoFocus callback will be called
	//Logic is not reliable in general but works in current OpenCamera code
	public static boolean isAutoFocusPerform()
	{
		int focusMode = CameraController.getFocusMode();
		if (CameraController.isFocusModeSupported()
			&& (CameraController.getFocusState() == CameraController.FOCUS_STATE_IDLE
				|| CameraController.getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
			&& !(focusMode == CameraParameters.AF_MODE_UNSUPPORTED
				 || focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
				 || focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
				 || focusMode == CameraParameters.AF_MODE_INFINITY
				 || focusMode == CameraParameters.AF_MODE_FIXED
				 || focusMode == CameraParameters.AF_MODE_EDOF
				 || focusMode == CameraParameters.MF_MODE)
				&& !ApplicationScreen.instance.getAutoFocusLock())
			return true;
		else
			return false;
	}

	public static void setPreviewFrameRate(int frameRate)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				if (camera != null && CameraController.getCameraParameters() != null)
				{
					Camera.Parameters cp = CameraController.getCameraParameters();
					cp.setPreviewFrameRate(frameRate);
					CameraController.setCameraParameters(cp);
				}
			}
		}
	}
	
	public static int getPreviewFrameRate()
	{
		if (!CameraController.isCamera2)
		{
			int[] range = { 0, 0 };
			camera.getParameters().getPreviewFpsRange(range);
			return range[1] / 1000;
		} else
			return Camera2Controller.getPreviewFrameRateCamera2();
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

	
	//All constant values collected experimentally
	public static float getHorizontalViewAngle()
	{
		if (!CameraController.isCamera2)
		{
			// LG G Flex 2.
			if (CameraController.isFlex2 /*|| CameraController.isG4*/)
			{
				return 60.808907f;
			}

			if (camera != null)
				return camera.getParameters().getHorizontalViewAngle();
		} else
		{
			return Camera2Controller.getHorizontalViewAngle();
		}

		if (CameraController.isNexus)
			return 59.63f;

		//Default value
		return 55.4f;
	}

	//All constant values collected experimentally
	public static float getVerticalViewAngle()
	{
		if (!CameraController.isCamera2)
		{
			// LG G Flex 2.
			if (CameraController.isFlex2 /*|| CameraController.isG4*/)
			{
				return 47.50866f;
			}

			if (camera != null)
				return camera.getParameters().getVerticalViewAngle();
		} else
		{
			return Camera2Controller.getVerticalViewAngle();
		}

		if (CameraController.isNexus)
			return 46.66f;

		//Default value
		return 42.7f;
	}

	public static int getSensorOrientation(int cameraIndex)
	{
		if (!isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
				Camera.getCameraInfo(CameraIndex, cameraInfo);
				return cameraInfo.orientation;
			} else
				return Camera2Controller.getInstance().getSensorOrientation(cameraIndex);
		} else
		{
			return -1;
		}
	}

	// CAMERA PARAMETERS AND CAPABILITIES
	// SECTION---------------------------------------------

	// ------------ CAPTURE AND FOCUS FUNCTION ----------------------------

	// Experimental code to take multiple images. Works only with camera2
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

	protected static boolean	indicateCapturing	= false;

	public static void startVideoRecordingSonyRemote()
	{
		SonyRemoteCamera.startMovieRec();
	}

	public static void stopVideoRecordingSonyRemote()
	{
		SonyRemoteCamera.stopMovieRec();
	}

	public static void configureMediaRecorder(MediaRecorder mediaRecorder)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				mediaRecorder.setCamera(camera);		
			}
		}
	}
	
	// Note: per-frame 'gain' and 'exposure' parameters are only effective for
	// Camera2 API at the moment
	public static int captureImagesWithParams(int nFrames, int format, int[] pause, int[] evRequested, int[] gain,
			long[] exposure, boolean setPowerGamma, boolean resInHeap, boolean indicate)
	{
		pauseBetweenShots = pause;
		evValues = evRequested;

		total_frames = nFrames;
		frame_num = 0;
		frameFormat = format;

		resultInHeap = resInHeap;

		previewWorking = false;
		cdt = null;

		indicateCapturing = indicate;

		if (!isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				if (appInterface.getFlashModePref(ApplicationScreen.sDefaultFlashValue) == CameraParameters.FLASH_MODE_CAPTURE_TORCH)
				{
					// If current flash mode is FLASH_MODE_CAPTURE_TORCH, then turn on torch before capturing.
					CameraController.setCameraFlashMode(CameraParameters.FLASH_MODE_TORCH);
				}
				
				takeYUVFrame = (format == CameraController.YUV) || (format == CameraController.YUV_RAW);
				if (evRequested != null && evRequested.length >= total_frames)
					CameraController.setExposure();
				else
				{
					if (CameraController.getFocusMode() == CameraParameters.AF_MODE_CONTINUOUS_PICTURE)
					{
						//In that way we avoid blurred image if continuous focus will perform during frame capturing
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
				return Camera2Controller.captureImageWithParamsCamera2(nFrames, format, pause, evRequested, gain, exposure, setPowerGamma,
						resultInHeap, indicateCapturing);
		} else
		{
			takeYUVFrame = (format == CameraController.YUV) || (format == CameraController.YUV_RAW);
			CameraController.takeImageSonyRemote();
			return 0;
		}
	}

	//Used to 'lock' focus for next plugin's internal logic
	public static void forceFocus()
	{
		if (CameraController.isCamera2)
		{
			Camera2Controller.forceFocusCamera2();
		}
	}

	public static boolean autoFocus()
	{
		synchronized (SYNC_OBJECT)
		{
			if (!CameraController.isRemoteCamera())
			{
				if (!CameraController.isCamera2)
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
					return Camera2Controller.autoFocusCamera2();
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
			if (!CameraController.isCamera2)
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
				Camera2Controller.cancelAutoFocusCamera2();
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
		CameraController.setPreviewCallbackWithBuffer();

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

	//Callback for preview frame in camera1 mode
	@Override
	public void onPreviewFrame(byte[] data, Camera camera)
	{
		pluginManager.onPreviewFrame(data);
		if(pviewBuffer != null)
			CameraController.getCamera().addCallbackBuffer(pviewBuffer);

		//If capture plugin request image with size equals size of preview frame
		//These tricky logic isn't affects capture plugin. It still request and received image as usual
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

		//Actually this part of code is used by Expo-bracketing plugin
		//to decide when exposure is changed and plugin may capture next
		//frame with adjusted exposure - decision depends on number of preview frames
		//have elapsed since setting new exposure value for camera
		if (pluginManager.isPreviewDependentMode() && evLatency > 0)
		{
			//Log.d(TAG, "evLatency = " + evLatency);
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

	//The purpose of this method is to improve preview efficiency and frame rate by allowing preview 
	//frame memory reuse. Valid only for camera1 mode
	public static void setPreviewCallbackWithBuffer()
	{
		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isCamera2)
			{
				//If preview buffer are allocated use preview callback with buffer
				//instead use preview callback without buffer - it may to slow preview frame rate
				if(CameraController.pviewBuffer != null)
				{
					CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
					CameraController.getCamera().addCallbackBuffer(CameraController.pviewBuffer);
				}
				else
					CameraController.getCamera().setPreviewCallback(CameraController.getInstance());
			}
		}
	}

	// plugin has to set it to TRUE if need preview frames
	//Actually method relate to capture mode instead of some plugin
	//All plugins which included to capture mode will receive or not receive preview frames
	public static void setNeedPreviewFrame(boolean needPreviewFrame)
	{
		if (CameraController.isUseCamera2())
			Camera2Controller.setNeedPreviewFrame(needPreviewFrame);
	}

	// should be reset on each changemode and on resume (call)
	public static void resetNeedPreviewFrame()
	{
		if (CameraController.isUseCamera2())
			Camera2Controller.resetNeedPreviewFrame();
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

	//Internal CC class for Size. Used instead of camera.Size class to support both camera1 and camera2 modes.
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
			appInterface.showCaptureIndication(indicateCapturing);

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

	
	//Used by Expo-bracketing mode to allow device complete exposure changing before capturing starts
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

			// Note 3 (and probably 4) & LG G3 need more time to change exposure.
			if (CameraController.isGalaxyNote3 || CameraController.isGalaxyNote4)
				evLatency = 20 * (isSlow ? 2 : 1);
			else if (CameraController.isG3)
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
			appInterface.showCaptureIndication(indicateCapturing);

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
