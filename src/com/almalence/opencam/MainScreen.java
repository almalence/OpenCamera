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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.OrientationEventListener;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.cameracontroller.HALv3;
import com.almalence.opencam.ui.AlmalenceGUI;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;
import com.almalence.plugins.capture.panoramaaugmented.PanoramaAugmentedCapturePlugin;
import com.almalence.plugins.capture.video.VideoCapturePlugin;
import com.almalence.util.AppRater;
import com.almalence.util.AppWidgetNotifier;
import com.almalence.util.Util;
//<!-- -+-

//-+- -->
/* <!-- +++
 import com.almalence.opencam_plus.ui.AlmalenceGUI;
 import com.almalence.opencam_plus.ui.GLLayer;
 import com.almalence.opencam_plus.ui.GUI;
 +++ --> */

/***
 * MainScreen - main activity screen with camera functionality
 * 
 * Passes all main events to PluginManager
 ***/

public class MainScreen extends Activity implements ApplicationInterface, View.OnClickListener, View.OnTouchListener,
		SurfaceHolder.Callback, Handler.Callback, Camera.ShutterCallback
{
	// >>Description
	// section with different global parameters available for everyone
	//
	// Camera parameters and possibly access to camera instance
	//
	// Global defines and others
	//
	// Description<<

	private static final int			MSG_RETURN_CAPTURED				= -1;

	private static final int			MODE_GENERAL					= 0;
	private static final int			MODE_SMART_MULTISHOT_AND_NIGHT	= 1;
	private static final int			MODE_PANORAMA					= 2;
	private static final int			MODE_VIDEO						= 3;

	private static final int			MIN_MPIX_SUPPORTED				= 1280 * 960;
	private static final int			MIN_MPIX_PREVIEW				= 600 * 400;
	private static final long			MPIX_8							= 3504 * 2336;

	public static MainScreen			thiz;
	public Context						mainContext;
	private Handler						messageHandler;

	// Interface to HALv3 camera and Old style camera
	private CameraController			cameraController				= null;

	// HALv3 camera's objects
	private ImageReader					mImageReaderPreviewYUV;
	private ImageReader					mImageReaderYUV;
	private ImageReader					mImageReaderJPEG;

	private boolean						captureYUVFrames				= false;

	public GUI							guiManager						= null;

	// OpenGL layer. May be used to allow capture plugins to draw overlaying
	// preview, such as night vision or panorama frames.
	private GLLayer						glView;

	private boolean						mPausing						= false;

	private File						forceFilename					= null;
	private Uri							forceFilenameUri;

	private SurfaceHolder				surfaceHolder;
	private SurfaceView					preview;
	private Surface						mCameraSurface					= null;
	private OrientationEventListener	orientListener;
	private boolean						landscapeIsNormal				= false;
	private boolean						surfaceCreated					= false;

	// shared between activities
	private int							imageWidth, imageHeight;
	private int							previewWidth, previewHeight;
	private int							saveImageWidth, saveImageHeight;

	private CountDownTimer				screenTimer						= null;
	private boolean						isScreenTimerRunning			= false;

	private static boolean				wantLandscapePhoto				= false;
	private int							orientationMain					= 0;
	private int							orientationMainPrevious			= 0;

	private SoundPlayer					shutterPlayer					= null;

	// Common preferences
	private String						imageSizeIdxPreference;
	private String						multishotImageSizeIdxPreference;
	private boolean						shutterPreference				= true;
	private boolean						shotOnTapPreference				= false;

	private boolean						showHelp						= false;

	private boolean						keepScreenOn					= false;

	private String						saveToPath;
	private String						saveToPreference;
	private boolean						sortByDataPreference;

	private static boolean				maxScreenBrightnessPreference;

	private static boolean				mAFLocked						= false;

	// >>Description
	// section with initialize, resume, start, stop procedures, preferences
	// access
	//
	// Initialize, stop etc depends on plugin type.
	//
	// Create main GUI controls and plugin specific controls.
	//
	// Description<<

	private static boolean				isCreating						= false;
	private static boolean				mApplicationStarted				= false;
	private static boolean				mCameraStarted					= false;

	// Clicked mode id from widget.
	public static final String			EXTRA_ITEM						= "WidgetModeID";

	public static final String			EXTRA_TORCH						= "WidgetTorchMode";
	public static final String			EXTRA_BARCODE					= "WidgetBarcodeMode";
	public static final String			EXTRA_SHOP						= "WidgetGoShopping";

	private static boolean				launchTorch						= false;
	private static boolean				launchBarcode					= false;
	private static boolean				goShopping						= false;

	private static int					prefFlash						= -1;
	private static boolean				prefBarcode						= false;

	private static final int			VOLUME_FUNC_SHUTTER				= 0;
	private static final int			VOLUME_FUNC_EXPO				= 2;
	private static final int			VOLUME_FUNC_NONE				= 3;

	private static List<Area>			mMeteringAreaMatrix5			= new ArrayList<Area>();
	private static List<Area>			mMeteringAreaMatrix4			= new ArrayList<Area>();
	private static List<Area>			mMeteringAreaMatrix1			= new ArrayList<Area>();
	private static List<Area>			mMeteringAreaCenter				= new ArrayList<Area>();
	private static List<Area>			mMeteringAreaSpot				= new ArrayList<Area>();

	private int							currentMeteringMode				= -1;

	public static String				sEvPref;
	public static String				sSceneModePref;
	public static String				sWBModePref;
	public static String				sFrontFocusModePref;
	public static String				sRearFocusModePref;
	public static String				sFlashModePref;
	public static String				sISOPref;
	public static String				sMeteringModePref;

	public static String				sDelayedCapturePref;
	public static String				sShowDelayedCapturePref;
	public static String				sDelayedSoundPref;
	public static String				sDelayedFlashPref;
	public static String				sDelayedCaptureIntervalPref;

	private static String				sUseFrontCameraPref;
	private static String				sShutterPref;
	private static String				sShotOnTapPref;
	private static String				sVolumeButtonPref;

	private static String				sImageSizeRearPref;
	private static String				sImageSizeFrontPref;

	public static String				sJPEGQualityPref;

	public static String				sDefaultInfoSetPref;
	public static String				sSWCheckedPref;
	public static String				sSavePathPref;
	public static String				sSaveToPref;
	public static String				sSortByDataPref;

	public static String				sDefaultModeName;

	public static int					sDefaultValue					= CameraParameters.SCENE_MODE_AUTO;
	public static int					sDefaultFocusValue				= CameraParameters.AF_MODE_CONTINUOUS_PICTURE;
	public static int					sDefaultFlashValue				= CameraParameters.FLASH_MODE_OFF;
	public static int					sDefaultMeteringValue			= CameraParameters.meteringModeAuto;

	@Override
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		sEvPref = getResources().getString(R.string.Preference_EvCompensationValue);
		sSceneModePref = getResources().getString(R.string.Preference_SceneModeValue);
		sWBModePref = getResources().getString(R.string.Preference_WBModeValue);
		sFrontFocusModePref = getResources().getString(R.string.Preference_FrontFocusModeValue);
		sRearFocusModePref = getResources().getString(R.string.Preference_RearFocusModeValue);
		sFlashModePref = getResources().getString(R.string.Preference_FlashModeValue);
		sISOPref = getResources().getString(R.string.Preference_ISOValue);
		sMeteringModePref = getResources().getString(R.string.Preference_MeteringModeValue);

		sDelayedCapturePref = getResources().getString(R.string.Preference_DelayedCaptureValue);
		sShowDelayedCapturePref = getResources().getString(R.string.Preference_ShowDelayedCaptureValue);
		sDelayedSoundPref = getResources().getString(R.string.Preference_DelayedSoundValue);
		sDelayedFlashPref = getResources().getString(R.string.Preference_DelayedFlashValue);
		sDelayedCaptureIntervalPref = getResources().getString(R.string.Preference_DelayedCaptureIntervalValue);

		sUseFrontCameraPref = getResources().getString(R.string.Preference_UseFrontCameraValue);
		sShutterPref = getResources().getString(R.string.Preference_ShutterCommonValue);
		sShotOnTapPref = getResources().getString(R.string.Preference_ShotOnTapValue);
		sVolumeButtonPref = getResources().getString(R.string.Preference_VolumeButtonValue);

		sImageSizeRearPref = getResources().getString(R.string.Preference_ImageSizeRearValue);
		sImageSizeFrontPref = getResources().getString(R.string.Preference_ImageSizeFrontValue);

		sJPEGQualityPref = getResources().getString(R.string.Preference_JPEGQualityCommonValue);

		sDefaultInfoSetPref = getResources().getString(R.string.Preference_DefaultInfoSetValue);
		sSWCheckedPref = getResources().getString(R.string.Preference_SWCheckedValue);
		sSavePathPref = getResources().getString(R.string.Preference_SavePathValue);
		sSaveToPref = getResources().getString(R.string.Preference_SaveToValue);
		sSortByDataPref = getResources().getString(R.string.Preference_SortByDataValue);

		sDefaultModeName = getResources().getString(R.string.Preference_DefaultModeName);

		Intent intent = this.getIntent();
		String mode = intent.getStringExtra(EXTRA_ITEM);
		launchTorch = intent.getBooleanExtra(EXTRA_TORCH, false);
		launchBarcode = intent.getBooleanExtra(EXTRA_BARCODE, false);
		goShopping = intent.getBooleanExtra(EXTRA_SHOP, false);

		mainContext = this.getBaseContext();
		messageHandler = new Handler(this);
		thiz = this;

		mApplicationStarted = false;

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// ensure landscape orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// set to fullscreen
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);

		// set some common view here
		setContentView(R.layout.opencamera_main_layout);

		// reset or save settings
		resetOrSaveSettings();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		if (null != mode)
			prefs.edit().putString("defaultModeName", mode).commit();

		if (launchTorch)
		{
			prefFlash = prefs.getInt(sFlashModePref, CameraParameters.FLASH_MODE_AUTO);
			prefs.edit().putInt(sFlashModePref, CameraParameters.FLASH_MODE_TORCH).commit();
		}

		if (launchBarcode)
		{
			prefBarcode = prefs.getBoolean("PrefBarcodescannerVF", false);
			prefs.edit().putBoolean("PrefBarcodescannerVF", true).commit();
		}

		// <!-- -+-

		/**** Billing *****/
		if (true == prefs.contains("unlock_all_forever"))
		{
			unlockAllPurchased = prefs.getBoolean("unlock_all_forever", false);
		}
		if (true == prefs.contains("plugin_almalence_hdr"))
		{
			hdrPurchased = prefs.getBoolean("plugin_almalence_hdr", false);
		}
		if (true == prefs.contains("plugin_almalence_panorama"))
		{
			panoramaPurchased = prefs.getBoolean("plugin_almalence_panorama", false);
		}
		if (true == prefs.contains("plugin_almalence_moving_burst"))
		{
			objectRemovalBurstPurchased = prefs.getBoolean("plugin_almalence_moving_burst", false);
		}
		if (true == prefs.contains("plugin_almalence_groupshot"))
		{
			groupShotPurchased = prefs.getBoolean("plugin_almalence_groupshot", false);
		}
		if (true == prefs.contains("subscription_unlock_all_month"))
		{
			unlockAllSubscriptionMonth = prefs.getBoolean("subscription_unlock_all_month", false);
		}
		if (true == prefs.contains("subscription_unlock_all_year"))
		{
			unlockAllSubscriptionYear = prefs.getBoolean("subscription_unlock_all_year", false);
		}

		createBillingHandler();
		
		/**** Billing *****/

		// application rating helper
		AppRater.app_launched(this);
		// -+- -->

		AppWidgetNotifier.app_launched(this);

		try
		{
			cameraController = CameraController.getInstance();
		} catch (VerifyError exp)
		{
			Log.e("MainScreen", exp.getMessage());
		}
		cameraController.onCreate(MainScreen.thiz, MainScreen.thiz, PluginManager.getInstance());

		// set preview, on click listener and surface buffers
		preview = (SurfaceView) this.findViewById(R.id.SurfaceView01);
		preview.setOnClickListener(this);
		preview.setOnTouchListener(this);
		preview.setKeepScreenOn(true);

		surfaceHolder = preview.getHolder();
		surfaceHolder.addCallback(this);
		// surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		// if(!CameraController.isUseHALv3())
		// {
		// surfaceHolder.addCallback(this);
		// surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
		// }

		orientListener = new OrientationEventListener(this)
		{
			@Override
			public void onOrientationChanged(int orientation)
			{
				// figure landscape or portrait
				if (MainScreen.thiz.landscapeIsNormal)
				{
					orientation += 90;
				}

				if ((orientation < 45) || (orientation > 315 && orientation < 405)
						|| ((orientation > 135) && (orientation < 225)))
				{
					if (MainScreen.wantLandscapePhoto)
					{
						MainScreen.wantLandscapePhoto = false;
					}
				} else
				{
					if (!MainScreen.wantLandscapePhoto)
					{
						MainScreen.wantLandscapePhoto = true;
					}
				}

				// orient properly for video
				if ((orientation > 135) && (orientation < 225))
					orientationMain = 270;
				else if ((orientation < 45) || (orientation > 315))
					orientationMain = 90;
				else if ((orientation < 325) && (orientation > 225))
					orientationMain = 0;
				else if ((orientation < 135) && (orientation > 45))
					orientationMain = 180;

				if (orientationMain != orientationMainPrevious)
				{
					orientationMainPrevious = orientationMain;
				}
			}
		};

		keepScreenOn = prefs.getBoolean("keepScreenOn", false);
		// prevent power drain
		screenTimer = new CountDownTimer(180000, 180000)
		{
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}

			public void onFinish()
			{
				boolean isVideoRecording = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext())
						.getBoolean("videorecording", false);
				if (isVideoRecording || keepScreenOn)
				{
					// restart timer
					screenTimer.start();
					isScreenTimerRunning = true;
					preview.setKeepScreenOn(true);
					return;
				}
				preview.setKeepScreenOn(false);
				isScreenTimerRunning = false;
			}
		};
		screenTimer.start();
		isScreenTimerRunning = true;

		PluginManager.getInstance().setupDefaultMode();
		// init gui manager
		guiManager = new AlmalenceGUI();
		guiManager.createInitialGUI();
		this.findViewById(R.id.mainLayout1).invalidate();
		this.findViewById(R.id.mainLayout1).requestLayout();
		guiManager.onCreate();

		// init plugin manager
		PluginManager.getInstance().onCreate();

		if (this.getIntent().getAction() != null)
		{
			if (this.getIntent().getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE))
			{
				try
				{
					forceFilenameUri = this.getIntent().getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
					MainScreen.setForceFilename(new File(((Uri) forceFilenameUri).getPath()));
					if (MainScreen.getForceFilename().getAbsolutePath().equals("/scrapSpace"))
					{
						MainScreen.setForceFilename(new File(Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/mms/scrapSpace/.temp.jpg"));
						new File(MainScreen.getForceFilename().getParent()).mkdirs();
					}
				} catch (Exception e)
				{
					MainScreen.setForceFilename(null);
				}
			} else
			{
				MainScreen.setForceFilename(null);
			}
		} else
		{
			MainScreen.setForceFilename(null);
		}

		// <!-- -+-
		if (goShopping)
		{
			if (MainScreen.thiz.titleUnlockAll == null || MainScreen.thiz.titleUnlockAll.endsWith("check for sale"))
			{
				Toast.makeText(MainScreen.getMainContext(),
						"Error connecting to Google Play. Check internet connection.", Toast.LENGTH_LONG).show();
				return;
			}
			guiManager.showStore();
		}
		// -+- -->
	}

	/*
	 * Get/Set method for private variables
	 */
	public static MainScreen getInstance()
	{
		return thiz;
	}

	public static Context getMainContext()
	{
		return thiz.mainContext;
	}

	public static Handler getMessageHandler()
	{
		return thiz.messageHandler;
	}

	public static CameraController getCameraController()
	{
		return thiz.cameraController;
	}

	public static GUI getGUIManager()
	{
		return thiz.guiManager;
	}

	@TargetApi(21)
	public static void createImageReaders()
	{
		// ImageReader for preview frames in YUV format
		thiz.mImageReaderPreviewYUV = ImageReader.newInstance(thiz.previewWidth, thiz.previewHeight,
				ImageFormat.YUV_420_888, 1);
		// thiz.mImageReaderPreviewYUV = ImageReader.newInstance(1280, 720,
		// ImageFormat.YUV_420_888, 1);

		// ImageReader for YUV still images
		thiz.mImageReaderYUV = ImageReader.newInstance(thiz.imageWidth, thiz.imageHeight, ImageFormat.YUV_420_888, 1);

		// ImageReader for JPEG still images
		thiz.mImageReaderJPEG = ImageReader.newInstance(thiz.imageWidth, thiz.imageHeight, ImageFormat.JPEG, 1);
	}

	public static ImageReader getPreviewYUVImageReader()
	{
		return thiz.mImageReaderPreviewYUV;
	}

	public static ImageReader getYUVImageReader()
	{
		return thiz.mImageReaderYUV;
	}

	public static ImageReader getJPEGImageReader()
	{
		return thiz.mImageReaderJPEG;
	}

	public static boolean isCaptureYUVFrames()
	{
		return thiz.captureYUVFrames;
	}

	public static void setCaptureYUVFrames(boolean captureYUV)
	{
		thiz.captureYUVFrames = captureYUV;
	}

	public static File getForceFilename()
	{
		return thiz.forceFilename;
	}

	public static void setForceFilename(File fileName)
	{
		thiz.forceFilename = fileName;
	}

	public static Uri getForceFilenameURI()
	{
		return thiz.forceFilenameUri;
	}

	public static SurfaceHolder getPreviewSurfaceHolder()
	{
		return thiz.surfaceHolder;
	}

	public static SurfaceView getPreviewSurfaceView()
	{
		return thiz.preview;
	}

	public static int getOrientation()
	{
		return thiz.orientationMain;
	}

	public static String getImageSizeIndex()
	{
		return thiz.imageSizeIdxPreference;
	}

	public static String getMultishotImageSizeIndex()
	{
		return thiz.multishotImageSizeIdxPreference;
	}

	public static boolean isShutterSoundEnabled()
	{
		return thiz.shutterPreference;
	}

	public static boolean isShotOnTap()
	{
		return thiz.shotOnTapPreference;
	}

	public static boolean isShowHelp()
	{
		return thiz.showHelp;
	}

	public static void setShowHelp(boolean show)
	{
		thiz.showHelp = show;
	}

	public static String getSaveToPath()
	{
		return thiz.saveToPath;
	}

	public static String getSaveTo()
	{
		return thiz.saveToPreference;
	}

	public static boolean isSortByData()
	{
		return thiz.sortByDataPreference;
	}

	public static int getMeteringMode()
	{
		return thiz.currentMeteringMode;
	}

	/*
	 * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ Get/Set method for private variables
	 */

	public void onPreferenceCreate(PreferenceFragment prefActivity)
	{
		setImageSizeOptions(prefActivity, MODE_GENERAL);
		setImageSizeOptions(prefActivity, MODE_SMART_MULTISHOT_AND_NIGHT);
		setImageSizeOptions(prefActivity, MODE_PANORAMA);
		setImageSizeOptions(prefActivity, MODE_VIDEO);
	}

	private void setImageSizeOptions(PreferenceFragment prefActivity, int mode)
	{
		CharSequence[] entries = null;
		CharSequence[] entryValues = null;

		int idx = 0;
		int currentIdx = -1;
		String opt1 = "";
		String opt2 = "";

		if (mode == MODE_GENERAL)
		{
			opt1 = "imageSizePrefCommonBack";
			opt2 = "imageSizePrefCommonFront";
			currentIdx = Integer.parseInt(MainScreen.getImageSizeIndex());
			entries = CameraController.getResolutionsNamesList().toArray(
					new CharSequence[CameraController.getResolutionsNamesList().size()]);
			entryValues = CameraController.getResolutionsIdxesList().toArray(
					new CharSequence[CameraController.getResolutionsIdxesList().size()]);
		} else if (mode == MODE_SMART_MULTISHOT_AND_NIGHT)
		{
			opt1 = "imageSizePrefSmartMultishotBack";
			opt2 = "imageSizePrefSmartMultishotFront";
			currentIdx = Integer.parseInt(CameraController.MultishotResolutionsIdxesList.get(MainScreen
					.selectImageDimensionMultishot()));
			entries = CameraController.MultishotResolutionsNamesList
					.toArray(new CharSequence[CameraController.MultishotResolutionsNamesList.size()]);
			entryValues = CameraController.MultishotResolutionsIdxesList
					.toArray(new CharSequence[CameraController.MultishotResolutionsIdxesList.size()]);
		} else if (mode == MODE_PANORAMA)
		{
			opt1 = "imageSizePrefPanoramaBack";
			opt2 = "imageSizePrefPanoramaFront";
			PanoramaAugmentedCapturePlugin.onDefaultSelectResolutons();
			currentIdx = PanoramaAugmentedCapturePlugin.prefResolution;
			entries = PanoramaAugmentedCapturePlugin.getResolutionspicturenameslist().toArray(
					new CharSequence[PanoramaAugmentedCapturePlugin.getResolutionspicturenameslist().size()]);
			entryValues = PanoramaAugmentedCapturePlugin.getResolutionspictureidxeslist().toArray(
					new CharSequence[PanoramaAugmentedCapturePlugin.getResolutionspictureidxeslist().size()]);
		} else if (mode == MODE_VIDEO)
		{
			opt1 = "imageSizePrefVideoBack";
			opt2 = "imageSizePrefVideoFront";

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			currentIdx = Integer.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? opt1 : opt2, "2"));

			CharSequence[] entriesTmp = new CharSequence[6];
			CharSequence[] entryValuesTmp = new CharSequence[6];
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), VideoCapturePlugin.QUALITY_4K))
			{
				entriesTmp[idx] = "4K";
				entryValuesTmp[idx] = "5";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_1080P))
			{
				entriesTmp[idx] = "1080p";
				entryValuesTmp[idx] = "2";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_720P))
			{
				entriesTmp[idx] = "720p";
				entryValuesTmp[idx] = "3";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_480P))
			{
				entriesTmp[idx] = "480p";
				entryValuesTmp[idx] = "4";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_CIF))
			{
				entriesTmp[idx] = "352 x 288";
				entryValuesTmp[idx] = "1";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_QCIF))
			{
				entriesTmp[idx] = "176 x 144";
				entryValuesTmp[idx] = "0";
				idx++;
			}

			entries = new CharSequence[idx];
			entryValues = new CharSequence[idx];

			for (int i = 0; i < idx; i++)
			{
				entries[i] = entriesTmp[i];
				entryValues[i] = entryValuesTmp[i];
			}
		}

		if (CameraController.getResolutionsIdxesList() != null)
		{
			ListPreference lp = (ListPreference) prefActivity.findPreference(opt1);
			ListPreference lp2 = (ListPreference) prefActivity.findPreference(opt2);

			if (CameraController.getCameraIndex() == 0 && lp2 != null)
				prefActivity.getPreferenceScreen().removePreference(lp2);
			else if (lp != null && lp2 != null)
			{
				prefActivity.getPreferenceScreen().removePreference(lp);
				lp = lp2;
			}
			if (lp != null)
			{
				lp.setEntries(entries);
				lp.setEntryValues(entryValues);

				// set currently selected image size
				for (idx = 0; idx < entryValues.length; ++idx)
				{
					if (Integer.valueOf(entryValues[idx].toString()) == currentIdx)
					{
						lp.setValueIndex(idx);
						break;
					}
				}

				if (mode == MODE_GENERAL)
				{
					lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
					{
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							int value = Integer.parseInt(newValue.toString());
							CameraController.setCameraImageSizeIndex(value);
							return true;
						}
					});
				}

				if (mode == MODE_PANORAMA)
				{
					lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
					{
						// @Override
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							int value = Integer.parseInt(newValue.toString());
							PanoramaAugmentedCapturePlugin.prefResolution = value;

							for (int i = 0; i < PanoramaAugmentedCapturePlugin.getResolutionspictureidxeslist().size(); i++)
							{
								if (PanoramaAugmentedCapturePlugin.getResolutionspictureidxeslist().get(i).equals(newValue))
								{
									final int idx = i;
									final Point point = PanoramaAugmentedCapturePlugin.getResolutionspicturesizeslist().get(idx);

									// frames_fit_count may decrease when
									// returning to main view due to slightly
									// more memory used, so in text messages we
									// report both exact and decreased count to
									// the user
									final int frames_fit_count = (int) (PanoramaAugmentedCapturePlugin.getAmountOfMemoryToFitFrames() / PanoramaAugmentedCapturePlugin.getFrameSizeInBytes(
											point.x, point.y));

									{
										Toast.makeText(
												MainScreen.getInstance(),
												String.format(
														MainScreen
																.getInstance()
																.getString(
																		R.string.pref_plugin_capture_panoramaaugmented_imageheight_warning),
														frames_fit_count), Toast.LENGTH_SHORT).show();

										return true;
									}
								}
							}

							return true;
						}
					});
				}
			}
		}

	}

	public void onAdvancePreferenceCreate(PreferenceFragment prefActivity)
	{
		CheckBoxPreference cp = (CheckBoxPreference) prefActivity.findPreference(getResources().getString(
				R.string.Preference_UseHALv3Key));
		if (cp != null)
		{
			if (!CameraController.isHALv3Supported())
				cp.setEnabled(false);
			else
				cp.setEnabled(true);
		}
	}

	public void glSetRenderingMode(final int renderMode)
	{
		if (renderMode != GLSurfaceView.RENDERMODE_WHEN_DIRTY && renderMode != GLSurfaceView.RENDERMODE_CONTINUOUSLY)
		{
			throw new IllegalArgumentException();
		}

		final GLSurfaceView surfaceView = glView;
		if (surfaceView != null)
		{
			surfaceView.setRenderMode(renderMode);
		}
	}

	public void glRequestRender()
	{
		final GLSurfaceView surfaceView = glView;
		if (surfaceView != null)
		{
			surfaceView.requestRender();
		}
	}

	public void queueGLEvent(final Runnable runnable)
	{
		final GLSurfaceView surfaceView = glView;

		if (surfaceView != null && runnable != null)
		{
			surfaceView.queueEvent(runnable);
		}
	}

	public int glGetPreviewTexture()
	{
		return glView.getPreviewTexture();
	}

	public SurfaceTexture glGetSurfaceTexture()
	{
		return glView.getSurfaceTexture();
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		MainScreen.getCameraController().onStart();
		MainScreen.getGUIManager().onStart();
		PluginManager.getInstance().onStart();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		mApplicationStarted = false;
		orientationMain = 0;
		orientationMainPrevious = 0;
		MainScreen.getGUIManager().onStop();
		PluginManager.getInstance().onStop();
		MainScreen.getCameraController().onStop();

		if (CameraController.isUseHALv3())
			stopImageReaders();
	}

	@TargetApi(21)
	private void stopImageReaders()
	{
		// IamgeReader should be closed
		if (mImageReaderPreviewYUV != null)
		{
			mImageReaderPreviewYUV.close();
			mImageReaderPreviewYUV = null;
		}
		if (mImageReaderYUV != null)
		{
			mImageReaderYUV.close();
			mImageReaderYUV = null;
		}
		if (mImageReaderJPEG != null)
		{
			mImageReaderJPEG.close();
			mImageReaderJPEG = null;
		}
	}

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		if (launchTorch && prefs.getInt(sFlashModePref, -1) == CameraParameters.FLASH_MODE_TORCH)
		{
			prefs.edit().putInt(sFlashModePref, prefFlash).commit();
		}
		if (launchBarcode && prefs.getBoolean("PrefBarcodescannerVF", false))
		{
			prefs.edit().putBoolean("PrefBarcodescannerVF", prefBarcode).commit();
		}
		MainScreen.getGUIManager().onDestroy();
		PluginManager.getInstance().onDestroy();
		MainScreen.getCameraController().onDestroy();

		// <!-- -+-
		/**** Billing *****/
		destroyBillingHandler();
		/**** Billing *****/
		// -+- -->

		this.hideOpenGLLayer();
	}

	@Override
	protected void onResume()
	{
		super.onResume();

		if (!isCreating)
			new CountDownTimer(50, 50)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(MainScreen.getMainContext());

					updatePreferences();

					captureYUVFrames = false;

					saveToPath = prefs.getString(sSavePathPref, Environment.getExternalStorageDirectory()
							.getAbsolutePath());
					saveToPreference = prefs.getString(MainScreen.sSaveToPref, "0");
					sortByDataPreference = prefs.getBoolean(MainScreen.sSortByDataPref, false);

					maxScreenBrightnessPreference = prefs.getBoolean("maxScreenBrightnessPref", false);
					setScreenBrightness(maxScreenBrightnessPreference);

					CameraController.useHALv3(prefs.getBoolean(getResources()
							.getString(R.string.Preference_UseHALv3Key), false));

					MainScreen.getGUIManager().onResume();
					PluginManager.getInstance().onResume();
					MainScreen.thiz.mPausing = false;

					if (CameraController.isUseHALv3())
					{
						MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						Log.e("MainScreen", "onResume: cameraController.setupCamera(null)");
						cameraController.setupCamera(null);

						if (glView != null)
						{
							glView.onResume();
							Log.e("GL", "glView onResume");
						}
					} else if (surfaceCreated && (!CameraController.isCameraCreated()))
					{
						MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						cameraController.setupCamera(surfaceHolder);

						if (glView != null)
						{
							glView.onResume();
							Log.e("GL", "glView onResume");
						}
					}
					orientListener.enable();
				}
			}.start();

		shutterPlayer = new SoundPlayer(this.getBaseContext(), getResources().openRawResourceFd(
				R.raw.plugin_capture_tick));

		if (screenTimer != null)
		{
			if (isScreenTimerRunning)
				screenTimer.cancel();
			screenTimer.start();
			isScreenTimerRunning = true;
		}

		// Log.e("Density", "" + getResources().getDisplayMetrics().toString());

		long memoryFree = getAvailableInternalMemory();
		if (memoryFree < 30)
			Toast.makeText(MainScreen.getMainContext(), "Almost no free space left on internal storage.",
					Toast.LENGTH_LONG).show();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean dismissKeyguard = prefs.getBoolean("dismissKeyguard", true);
		if (dismissKeyguard)
			getWindow()
					.addFlags(
							WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
									| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		else
			getWindow()
					.clearFlags(
							WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
									| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		
		// <!-- -+-
		if(isABCUnlockedInstalled(this))
		{
			unlockAllPurchased = true;
			prefs.edit().putBoolean("unlock_all_forever", true).commit();
		}
		// -+- -->
	}

	public void relaunchCamera()
	{
		if (CameraController.isUseHALv3())
		{
			new CountDownTimer(100, 100)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					PluginManager.getInstance().switchMode(
							ConfigParser.getInstance().getMode(PluginManager.getInstance().getActiveModeID()));
				}
			}.start();
		}
	}

	private long getAvailableInternalMemory()
	{
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize / 1048576;
	}

	private void updatePreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		CameraController.setCameraIndex(!prefs.getBoolean(MainScreen.sUseFrontCameraPref, false) ? 0 : 1);
		shutterPreference = prefs.getBoolean(MainScreen.sShutterPref, false);
		shotOnTapPreference = prefs.getBoolean(MainScreen.sShotOnTapPref, false);
		imageSizeIdxPreference = prefs.getString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeRearPref
				: MainScreen.sImageSizeFrontPref, "-1");

		multishotImageSizeIdxPreference = prefs.getString(
				CameraController.getCameraIndex() == 0 ? "imageSizePrefSmartMultishotBack"
						: "imageSizePrefSmartMultishotFront", "-1");
	}

	@Override
	protected void onPause()
	{
		super.onPause();
		mApplicationStarted = false;

		MainScreen.getGUIManager().onPause();
		PluginManager.getInstance().onPause(true);

		orientListener.disable();

		if (shutterPreference)
		{
			AudioManager mgr = (AudioManager) MainScreen.thiz
					.getSystemService(MainScreen.getMainContext().AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
		}

		this.mPausing = true;

		this.hideOpenGLLayer();

		if (screenTimer != null)
		{
			if (isScreenTimerRunning)
				screenTimer.cancel();
			isScreenTimerRunning = false;
		}

		cameraController.onPause();

		this.findViewById(R.id.mainLayout2).setVisibility(View.INVISIBLE);

		if (shutterPlayer != null)
		{
			shutterPlayer.release();
			shutterPlayer = null;
		}
	}

	public void pauseMain()
	{
		onPause();
	}

	public void stopMain()
	{
		onStop();
	}

	public void startMain()
	{
		onStart();
	}

	public void resumeMain()
	{
		onResume();
	}

	@Override
	public void surfaceChanged(final SurfaceHolder holder, final int format, final int width, final int height)
	{

		if (!isCreating)
			new CountDownTimer(50, 50)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					updatePreferences();

					if (!MainScreen.thiz.mPausing && surfaceCreated && (!CameraController.isCameraCreated()))
					{
						MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						Log.e("MainScreen", "surfaceChanged: cameraController.setupCamera(null)");
						if (!CameraController.isUseHALv3())
						{
							cameraController.setupCamera(holder);
						} else
						{
							messageHandler.sendEmptyMessage(PluginManager.MSG_SURFACE_READY);
						}
					}
				}
			}.start();
		else
		{
			updatePreferences();
		}
	}

	public static int selectImageDimensionMultishot()
	{
		long maxMem = Runtime.getRuntime().maxMemory() - Debug.getNativeHeapAllocatedSize();
		long maxMpix = (maxMem - 1000000) / 3; // 2 x Mpix - result, 1/4 x Mpix
												// x 4 - compressed input jpegs,
												// 1Mb - safe reserve

		if (maxMpix < MIN_MPIX_SUPPORTED)
		{
			String msg;
			msg = "MainScreen.selectImageDimension maxMem = " + maxMem;
			Log.e("MultishotCapturePlugin", "MainScreen.selectImageDimension maxMpix < MIN_MPIX_SUPPORTED");
			Log.e("MultishotCapturePlugin", msg);
		}

		// find index selected in preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int prefIdx = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? "imageSizePrefSmartMultishotBack"
						: "imageSizePrefSmartMultishotFront", "-1"));

		// ----- Find max-resolution capture dimensions
		int minMPIX = MIN_MPIX_PREVIEW;

		int defaultCaptureIdx = -1;
		long defaultCaptureMpix = 0;
		long captureMpix = 0;
		int captureIdx = -1;
		boolean prefFound = false;

		// figure default resolution
		for (int ii = 0; ii < CameraController.MultishotResolutionsSizeList.size(); ++ii)
		{
			CameraController.Size s = CameraController.MultishotResolutionsSizeList.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((mpix >= minMPIX) && (mpix < maxMpix) && (mpix > defaultCaptureMpix))
			{
				defaultCaptureIdx = ii;
				defaultCaptureMpix = mpix;
			}
		}

		for (int ii = 0; ii < CameraController.MultishotResolutionsSizeList.size(); ++ii)
		{
			CameraController.Size s = CameraController.MultishotResolutionsSizeList.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((ii == prefIdx) && (mpix >= minMPIX))
			{
				prefFound = true;
				captureIdx = ii;
				captureMpix = mpix;
				break;
			}

			if (mpix > captureMpix)
			{
				captureIdx = ii;
				captureMpix = mpix;
			}
		}

		if (defaultCaptureMpix > 0 && !prefFound)
		{
			captureIdx = defaultCaptureIdx;
			captureMpix = defaultCaptureMpix;
		}

		return captureIdx;
	}

	public void onSurfaceChangedMain(final SurfaceHolder holder, final int width, final int height)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		CameraController.setCameraImageSizeIndex(!prefs.getBoolean("useFrontCamera", false) ? 0 : 1);
		shutterPreference = prefs.getBoolean("shutterPrefCommon", false);
		shotOnTapPreference = prefs.getBoolean("shotontapPrefCommon", false);
		imageSizeIdxPreference = prefs.getString(CameraController.getCameraIndex() == 0 ? "imageSizePrefCommonBack"
				: "imageSizePrefCommonFront", "-1");

		if (!MainScreen.thiz.mPausing && surfaceCreated && (!CameraController.isCameraCreated()))
		{
			MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);

			if (CameraController.isUseHALv3())
				messageHandler.sendEmptyMessage(PluginManager.MSG_SURFACE_READY);
			else
			{
				Log.e("MainScreen", "surfaceChangedMain: cameraController.setupCamera(null)");
				cameraController.setupCamera(holder);
			}
		}
	}

	@Override
	public void addSurfaceCallback()
	{
		thiz.surfaceHolder.addCallback(thiz);
	}

	@Override
	public void configureCamera()
	{
		Log.e("MainScreen", "configureCamera()");

		CameraController.getInstance().updateCameraFeatures();

		// ----- Select preview dimensions with ratio correspondent to
		// full-size image
		PluginManager.getInstance().setCameraPreviewSize();
		// prepare list of surfaces to be used in capture requests
		if (CameraController.isUseHALv3())
			configureHALv3Camera(captureYUVFrames);
		else
		{
			Camera.Size sz = CameraController.getInstance().getCameraParameters().getPreviewSize();

			guiManager.setupViewfinderPreviewSize(cameraController.new Size(sz.width, sz.height));
			CameraController.getInstance().allocatePreviewBuffer(
					sz.width
							* sz.height
							* ImageFormat.getBitsPerPixel(CameraController.getInstance().getCameraParameters()
									.getPreviewFormat()) / 8);

			CameraController.getCamera().setErrorCallback(CameraController.getInstance());

			onCameraConfigured();
		}

	}

	private void onCameraConfigured()
	{
		PluginManager.getInstance().setCameraPictureSize();
		PluginManager.getInstance().setupCameraParameters();

		Camera.Parameters cp = CameraController.getInstance().getCameraParameters();

		if (!CameraController.isUseHALv3())
		{
			try
			{
				// Nexus 5 is giving preview which is too dark without this
				if (Build.MODEL.contains("Nexus 5"))
				{
					cp.setPreviewFpsRange(7000, 30000);
					cameraController.setCameraParameters(cp);
					cp = CameraController.getInstance().getCameraParameters();
				}
			} catch (RuntimeException e)
			{
				Log.e("CameraTest", "MainScreen.setupCamera unable setParameters " + e.getMessage());
			}

			previewWidth = cp.getPreviewSize().width;
			previewHeight = cp.getPreviewSize().height;
		}

		try
		{
			Util.initialize(mainContext);
			Util.initializeMeteringMatrix();
		} catch (Exception e)
		{
			Log.e("Main setup camera", "Util.initialize failed!");
		}

		prepareMeteringAreas();

		if (!CameraController.isUseHALv3())
		{
			guiManager.onCameraCreate();
			PluginManager.getInstance().onCameraParametersSetup();
			guiManager.onPluginsInitialized();
		}

		// ----- Start preview and setup frame buffer if needed

		// call camera release sequence from onPause somewhere ???
		new CountDownTimer(10, 10)
		{
			@Override
			public void onFinish()
			{
				if (!CameraController.isUseHALv3())
				{
					if (!CameraController.isCameraCreated())
						return;
					// exceptions sometimes happen here when resuming after
					// processing
					try
					{
						CameraController.startCameraPreview();
					} catch (RuntimeException e)
					{
						Toast.makeText(MainScreen.thiz, "Unable to start camera", Toast.LENGTH_LONG).show();
						return;
					}

					CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
					CameraController.getCamera().addCallbackBuffer(CameraController.getInstance().getPreviewBuffer());
				} else
				{
					guiManager.onCameraCreate();
					PluginManager.getInstance().onCameraParametersSetup();
					guiManager.onPluginsInitialized();
				}

				PluginManager.getInstance().onCameraSetup();
				guiManager.onCameraSetup();
				MainScreen.mApplicationStarted = true;
			}

			@Override
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}
		}.start();
	}

	@SuppressLint("NewApi")
	@TargetApi(21)
	private void configureHALv3Camera(boolean captureYUVFrames)
	{
		List<Surface> sfl = new ArrayList<Surface>();

		sfl.add(mCameraSurface); // surface for viewfinder preview
		sfl.add(mImageReaderPreviewYUV.getSurface()); // surface for preview yuv
														// images
		if (captureYUVFrames)
		{
			Log.e("MainScreen", "add mImageReaderYUV");
			sfl.add(mImageReaderYUV.getSurface()); // surface for yuv image
													// capture
		} else
		{
			Log.e("MainScreen", "add mImageReaderJPEG");
			sfl.add(mImageReaderJPEG.getSurface()); // surface for jpeg image
													// capture
		}

		cameraController.setPreviewSurface(mImageReaderPreviewYUV.getSurface());

		guiManager.setupViewfinderPreviewSize(cameraController.new Size(this.previewWidth, this.previewHeight));
		// guiManager.setupViewfinderPreviewSize(cameraController.new Size(1280,
		// 720));

		// configure camera with all the surfaces to be ever used
		try
		{
			Log.e("MainScreen", "HALv3.getCamera2().configureOutputs(sfl);");
			HALv3.getCamera2().createCaptureSession(sfl, HALv3.captureSessionStateListener, null);
		} catch (Exception e)
		{
			Log.e("MainScreen", "configureOutputs failed. " + e.getMessage());
			e.printStackTrace();
		}

		// ^^ HALv3 code
		// -------------------------------------------------------------------
	}

	private void prepareMeteringAreas()
	{
		Rect centerRect = Util.convertToDriverCoordinates(new Rect(previewWidth / 4, previewHeight / 4, previewWidth
				- previewWidth / 4, previewHeight - previewHeight / 4));
		Rect topLeftRect = Util.convertToDriverCoordinates(new Rect(0, 0, previewWidth / 2, previewHeight / 2));
		Rect topRightRect = Util.convertToDriverCoordinates(new Rect(previewWidth / 2, 0, previewWidth,
				previewHeight / 2));
		Rect bottomRightRect = Util.convertToDriverCoordinates(new Rect(previewWidth / 2, previewHeight / 2,
				previewWidth, previewHeight));
		Rect bottomLeftRect = Util.convertToDriverCoordinates(new Rect(0, previewHeight / 2, previewWidth / 2,
				previewHeight));
		Rect spotRect = Util.convertToDriverCoordinates(new Rect(previewWidth / 2 - 10, previewHeight / 2 - 10,
				previewWidth / 2 + 10, previewHeight / 2 + 10));

		mMeteringAreaMatrix5.clear();
		mMeteringAreaMatrix5.add(new Area(centerRect, 600));
		mMeteringAreaMatrix5.add(new Area(topLeftRect, 200));
		mMeteringAreaMatrix5.add(new Area(topRightRect, 200));
		mMeteringAreaMatrix5.add(new Area(bottomRightRect, 200));
		mMeteringAreaMatrix5.add(new Area(bottomLeftRect, 200));

		mMeteringAreaMatrix4.clear();
		mMeteringAreaMatrix4.add(new Area(topLeftRect, 250));
		mMeteringAreaMatrix4.add(new Area(topRightRect, 250));
		mMeteringAreaMatrix4.add(new Area(bottomRightRect, 250));
		mMeteringAreaMatrix4.add(new Area(bottomLeftRect, 250));

		mMeteringAreaMatrix1.clear();
		mMeteringAreaMatrix1.add(new Area(centerRect, 1000));

		mMeteringAreaCenter.clear();
		mMeteringAreaCenter.add(new Area(centerRect, 1000));

		mMeteringAreaSpot.clear();
		mMeteringAreaSpot.add(new Area(spotRect, 1000));
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		// ----- Find 'normal' orientation of the device

		Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		if ((rotation == Surface.ROTATION_90) || (rotation == Surface.ROTATION_270))
			landscapeIsNormal = true; // false; - if landscape view orientation
										// set for MainScreen
		else
			landscapeIsNormal = false;

		surfaceCreated = true;

		mCameraSurface = surfaceHolder.getSurface();

		Log.e("MainScreen", "SURFACE CREATED");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		surfaceCreated = false;
	}

	// SURFACES (preview, image readers)
	public Surface getCameraSurface()
	{
		return mCameraSurface;
	}

	@TargetApi(21)
	public Surface getPreviewYUVSurface()
	{
		return mImageReaderPreviewYUV.getSurface();
	}

	@TargetApi(14)
	public boolean isFaceDetectionAvailable(Camera.Parameters params)
	{
		return params.getMaxNumDetectedFaces() > 0;
	}

	public CameraController.Size getPreviewSize()
	{
		LayoutParams lp = preview.getLayoutParams();
		if (lp == null)
			return null;

		return cameraController.new Size(lp.width, lp.height);
	}

	/*
	 * CAMERA PARAMETERS SECTION Supplementary methods for those plugins that
	 * need an icons of supported camera parameters (scene, iso, wb, flash,
	 * focus) Methods return id of drawable icon
	 */
	public int getSceneIcon(int sceneMode)
	{
		return guiManager.getSceneIcon(sceneMode);
	}

	public int getWBIcon(int wb)
	{
		return guiManager.getWBIcon(wb);
	}

	public int getFocusIcon(int focusMode)
	{
		return guiManager.getFocusIcon(focusMode);
	}

	public int getFlashIcon(int flashMode)
	{
		return guiManager.getFlashIcon(flashMode);
	}

	public int getISOIcon(int isoMode)
	{
		return guiManager.getISOIcon(isoMode);
	}

	public void setCameraMeteringMode(int mode)
	{
		if (CameraParameters.meteringModeAuto == mode)
			cameraController.setCameraMeteringAreas(null);
		else if (CameraParameters.meteringModeMatrix == mode)
		{
			int maxAreasCount = CameraController.getInstance().getMaxNumMeteringAreas();
			if (maxAreasCount > 4)
				cameraController.setCameraMeteringAreas(mMeteringAreaMatrix5);
			else if (maxAreasCount > 3)
				cameraController.setCameraMeteringAreas(mMeteringAreaMatrix4);
			else if (maxAreasCount > 0)
				cameraController.setCameraMeteringAreas(mMeteringAreaMatrix1);
			else
				cameraController.setCameraMeteringAreas(null);
		} else if (CameraParameters.meteringModeCenter == mode)
			cameraController.setCameraMeteringAreas(mMeteringAreaCenter);
		else if (CameraParameters.meteringModeSpot == mode)
			cameraController.setCameraMeteringAreas(mMeteringAreaSpot);

		currentMeteringMode = mode;
	}

	/*
	 * 
	 * CAMERA parameters access function ended
	 */

	// >>Description
	// section with user control procedures and main capture functions
	//
	// all events translated to PluginManager
	// Description<<

	public static void setAutoFocusLock(boolean locked)
	{
		mAFLocked = locked;
	}

	public static boolean getAutoFocusLock()
	{
		return mAFLocked;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (!mApplicationStarted)
			return true;

		// menu button processing
		if (keyCode == KeyEvent.KEYCODE_MENU)
		{
			menuButtonPressed();
			return true;
		}
		// shutter/camera button processing
		if (keyCode == KeyEvent.KEYCODE_CAMERA || keyCode == KeyEvent.KEYCODE_DPAD_CENTER)
		{
			MainScreen.getGUIManager().onHardwareShutterButtonPressed();
			return true;
		}
		// focus/half-press button processing
		if (keyCode == KeyEvent.KEYCODE_FOCUS)
		{
			MainScreen.getGUIManager().onHardwareFocusButtonPressed();
			return true;
		}

		// check if Headset Hook button has some functions except standard
		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			boolean headsetFunc = prefs.getBoolean("headsetPrefCommon", false);
			if (headsetFunc)
			{
				MainScreen.getGUIManager().onHardwareFocusButtonPressed();
				MainScreen.getGUIManager().onHardwareShutterButtonPressed();
				return true;
			}
		}

		// check if volume button has some functions except Zoom-ing
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			int buttonFunc = Integer.parseInt(prefs.getString(MainScreen.sVolumeButtonPref, "0"));
			if (buttonFunc == VOLUME_FUNC_SHUTTER)
			{
				MainScreen.getGUIManager().onHardwareFocusButtonPressed();
				MainScreen.getGUIManager().onHardwareShutterButtonPressed();
				return true;
			} else if (buttonFunc == VOLUME_FUNC_EXPO)
			{
				MainScreen.getGUIManager().onVolumeBtnExpo(keyCode);
				return true;
			} else if (buttonFunc == VOLUME_FUNC_NONE)
				return true;
		}

		if (PluginManager.getInstance().onKeyDown(true, keyCode, event))
			return true;
		if (guiManager.onKeyDown(true, keyCode, event))
			return true;

		// <!-- -+-
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (AppRater.showRateDialogIfNeeded(this))
			{
				return true;
			}
			if (AppWidgetNotifier.showNotifierDialogIfNeeded(this))
			{
				return true;
			}
		}
		// -+- -->

		if (super.onKeyDown(keyCode, event))
			return true;
		return false;
	}

	@Override
	public void onClick(View v)
	{
		if (mApplicationStarted)
			MainScreen.getGUIManager().onClick(v);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event)
	{
		if (mApplicationStarted)
			return MainScreen.getGUIManager().onTouch(view, event);
		return true;
	}

	public boolean onTouchSuper(View view, MotionEvent event)
	{
		return super.onTouchEvent(event);
	}

	public void onButtonClick(View v)
	{
		MainScreen.getGUIManager().onButtonClick(v);
	}

	@Override
	public void onShutter()
	{
		PluginManager.getInstance().onShutter();
	}

	// >>Description
	// message processor
	//
	// processing main events and calling active plugin procedures
	//
	// possible some additional plugin dependent events.
	//
	// Description<<
	@Override
	public boolean handleMessage(Message msg)
	{

		switch (msg.what)
		{
		case MSG_RETURN_CAPTURED:
			this.setResult(RESULT_OK);
			this.finish();
			break;
		case PluginManager.MSG_CAMERA_CONFIGURED:
			// Log.e("MainScreen", "case PluginManager.MSG_CAMERA_CONFIGURED");
			onCameraConfigured();
			break;
		case PluginManager.MSG_CAMERA_READY:
			{
				if (CameraController.isCameraCreated())
				{
					configureCamera();
					PluginManager.getInstance().onGUICreate();
					MainScreen.getGUIManager().onGUICreate();
				}
			}
			break;
		case PluginManager.MSG_CAMERA_OPENED:
			// Log.e("MainScreen", "case PluginManager.MSG_CAMERA_OPENED");
			if (mCameraStarted)
				break;
		case PluginManager.MSG_SURFACE_READY:
			{
				// if both surface is created and camera device is opened
				// - ready to set up preview and other things
				if (surfaceCreated && (HALv3.getCamera2() != null))
				{
					// Log.e("MainScreen",
					// "case PluginManager.MSG_SURFACE_READY");
					configureCamera();
					PluginManager.getInstance().onGUICreate();
					MainScreen.getGUIManager().onGUICreate();
					mCameraStarted = true;
				}
			}
			break;
		case PluginManager.MSG_CAMERA_STOPED:
			// Log.e("MainScreen", "case PluginManager.MSG_CAMERA_STOPED");
			mCameraStarted = false;
			break;
		default:
			PluginManager.getInstance().handleMessage(msg);
			break;
		}

		return true;
	}

	public void menuButtonPressed()
	{
		PluginManager.getInstance().menuButtonPressed();
	}

	public void disableCameraParameter(GUI.CameraParameter iParam, boolean bDisable, boolean bInitMenu)
	{
		guiManager.disableCameraParameter(iParam, bDisable, bInitMenu);
	}

	public void showOpenGLLayer(final int version)
	{
		if (glView == null)
		{
			glView = new GLLayer(MainScreen.getMainContext(), version);
			glView.setLayoutParams(new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			((RelativeLayout) this.findViewById(R.id.mainLayout2)).addView(glView, 0);
			preview.bringToFront();
			glView.setZOrderMediaOverlay(true);
			glView.onResume();
		}
	}

	public void hideOpenGLLayer()
	{
		if (glView != null)
		{
			// preview.getHolder().getSurface().lockCanvas(null).drawColor(Color.BLACK);
			glView.onPause();
			((RelativeLayout) this.findViewById(R.id.mainLayout2)).removeView(glView);
			glView = null;
		}
	}

	public void playShutter(int sound)
	{
		if (!MainScreen.isShutterSoundEnabled())
		{
			MediaPlayer mediaPlayer = MediaPlayer.create(MainScreen.thiz, sound);
			mediaPlayer.start();
		}
	}

	public void playShutter()
	{
		if (!MainScreen.isShutterSoundEnabled())
		{
			if (shutterPlayer != null)
				shutterPlayer.play();
		}
	}

	// set TRUE to mute and FALSE to unmute
	public void muteShutter(boolean mute)
	{
		if (MainScreen.isShutterSoundEnabled())
		{
			AudioManager mgr = (AudioManager) MainScreen.thiz
					.getSystemService(MainScreen.getMainContext().AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, mute);
		}
	}

	public static int getImageWidth()
	{
		return thiz.imageWidth;
	}

	public static void setImageWidth(int setImageWidth)
	{
		thiz.imageWidth = setImageWidth;
	}

	public static int getImageHeight()
	{
		return thiz.imageHeight;
	}

	public static void setImageHeight(int setImageHeight)
	{
		thiz.imageHeight = setImageHeight;
	}

	public static int getSaveImageWidth()
	{
		return thiz.saveImageWidth;
	}

	public static void setSaveImageWidth(int setSaveImageWidth)
	{
		thiz.saveImageWidth = setSaveImageWidth;
	}

	public static int getSaveImageHeight()
	{
		return thiz.saveImageHeight;
	}

	public static void setSaveImageHeight(int setSaveImageHeight)
	{
		thiz.saveImageHeight = setSaveImageHeight;
	}

	public static int getPreviewWidth()
	{
		return thiz.previewWidth;
	}

	public static void setPreviewWidth(int iWidth)
	{
		Log.e("MainScreen", "setPreviewWidth = " + iWidth);
		thiz.previewWidth = iWidth;
	}

	public static int getPreviewHeight()
	{
		return thiz.previewHeight;
	}

	public static void setPreviewHeight(int iHeight)
	{
		Log.e("MainScreen", "setPreviewHeight = " + iHeight);
		thiz.previewHeight = iHeight;
	}

	public static boolean getWantLandscapePhoto()
	{
		return wantLandscapePhoto;
	}

	public static void setWantLandscapePhoto(boolean setWantLandscapePhoto)
	{
		wantLandscapePhoto = setWantLandscapePhoto;
	}

	public void setScreenBrightness(boolean setMax)
	{
		Window window = getWindow();
		WindowManager.LayoutParams layoutpars = window.getAttributes();

		// Set the brightness of this window
		if (setMax)
			layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
		else
			layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

		// Apply attribute changes to this window
		window.setAttributes(layoutpars);
	}

	/*******************************************************/
	/************************ Billing ************************/

	private boolean		showStore					= false;
	// <!-- -+-
	OpenIabHelper		mHelper;

	private boolean		bOnSale						= false;
	private boolean		couponSale					= false;

	private boolean		unlockAllPurchased			= false;
	private boolean		hdrPurchased				= false;
	private boolean		panoramaPurchased			= false;
	private boolean		objectRemovalBurstPurchased	= false;
	private boolean		groupShotPurchased			= false;

	private boolean		unlockAllSubscriptionMonth	= false;
	private boolean		unlockAllSubscriptionYear	= false;

	static final String	SKU_HDR						= "plugin_almalence_hdr";
	static final String	SKU_PANORAMA				= "plugin_almalence_panorama";
	static final String	SKU_UNLOCK_ALL				= "unlock_all_forever";
	static final String	SKU_UNLOCK_ALL_COUPON		= "unlock_all_forever_coupon";
	static final String	SKU_MOVING_SEQ				= "plugin_almalence_moving_burst";
	static final String	SKU_GROUPSHOT				= "plugin_almalence_groupshot";
	// subscription
	static final String	SKU_SUBSCRIPTION_YEAR		= "subscription_unlock_all_year";
	static final String	SKU_SUBSCRIPTION_YEAR_CTRL		= "subscription_unlock_all_year_controller";

	static final String	SKU_SALE1					= "abc_sale_controller1";
	static final String	SKU_SALE2					= "abc_sale_controller2";
	
	static final String	SKU_PROMO					= "abc_promo";

	static
	{
		// Yandex store
		OpenIabHelper.mapSku(SKU_HDR, "com.yandex.store", "plugin_almalence_hdr");
		OpenIabHelper.mapSku(SKU_PANORAMA, "com.yandex.store", "plugin_almalence_panorama");
		OpenIabHelper.mapSku(SKU_UNLOCK_ALL, "com.yandex.store", "unlock_all_forever");
		OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON, "com.yandex.store", "unlock_all_forever_coupon");
		OpenIabHelper.mapSku(SKU_MOVING_SEQ, "com.yandex.store", "plugin_almalence_moving_burst");
		OpenIabHelper.mapSku(SKU_GROUPSHOT, "com.yandex.store", "plugin_almalence_groupshot");
		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR, "com.yandex.store", "subscription_unlock_all_year");
		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR_CTRL, "com.yandex.store", "subscription_unlock_all_year_controller");

		OpenIabHelper.mapSku(SKU_SALE1, "com.yandex.store", "abc_sale_controller1");
		OpenIabHelper.mapSku(SKU_SALE2, "com.yandex.store", "abc_sale_controller2");
		OpenIabHelper.mapSku(SKU_PROMO, "com.yandex.store", "abc_promo");

		// Amazon store
		OpenIabHelper.mapSku(SKU_HDR, OpenIabHelper.NAME_AMAZON, "plugin_almalence_hdr_amazon");
		OpenIabHelper.mapSku(SKU_PANORAMA, OpenIabHelper.NAME_AMAZON, "plugin_almalence_panorama_amazon");
		OpenIabHelper.mapSku(SKU_UNLOCK_ALL, OpenIabHelper.NAME_AMAZON, "unlock_all_forever_amazon");
		OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON, OpenIabHelper.NAME_AMAZON, "unlock_all_forever_coupon_amazon");
		OpenIabHelper.mapSku(SKU_MOVING_SEQ, OpenIabHelper.NAME_AMAZON, "plugin_almalence_moving_burst_amazon");
		OpenIabHelper.mapSku(SKU_GROUPSHOT, OpenIabHelper.NAME_AMAZON, "plugin_almalence_groupshot_amazon");
		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR, OpenIabHelper.NAME_AMAZON, "subscription_unlock_all_year");
		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR_CTRL, OpenIabHelper.NAME_AMAZON, "subscription_unlock_all_year_controller");

		OpenIabHelper.mapSku(SKU_SALE1, OpenIabHelper.NAME_AMAZON, "abc_sale_controller1_amazon");
		OpenIabHelper.mapSku(SKU_SALE2, OpenIabHelper.NAME_AMAZON, "abc_sale_controller2_amazon");
		OpenIabHelper.mapSku(SKU_PROMO, OpenIabHelper.NAME_AMAZON, "abc_promo_amazon");

		// Samsung store
		// OpenIabHelper.mapSku(SKU_HDR, OpenIabHelper.NAME_SAMSUNG,
		// "100000103369/000001018387");
		// OpenIabHelper.mapSku(SKU_PANORAMA, OpenIabHelper.NAME_SAMSUNG,
		// "100000103369/000001018389");
		// OpenIabHelper.mapSku(SKU_UNLOCK_ALL, OpenIabHelper.NAME_SAMSUNG,
		// "100000103369/000001017613");
		// OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON,
		// OpenIabHelper.NAME_SAMSUNG, "100000103369/000001018392");
		// OpenIabHelper.mapSku(SKU_MOVING_SEQ, OpenIabHelper.NAME_SAMSUNG,
		// "100000103369/000001018391");
		// OpenIabHelper.mapSku(SKU_GROUPSHOT, OpenIabHelper.NAME_SAMSUNG,
		// "100000103369/000001018384");
		//
		// OpenIabHelper.mapSku(SKU_SALE1, OpenIabHelper.NAME_SAMSUNG,
		// "100000103369/000001018393");
		// OpenIabHelper.mapSku(SKU_SALE2, OpenIabHelper.NAME_SAMSUNG,
		// "100000103369/000001018394");
	}

	public void setShowStore(boolean show)
	{
		showStore = show;
	}

	public boolean isShowStore()
	{
		return showStore;
	}

	public void activateCouponSale()
	{
		couponSale = true;
	}

	public boolean isCouponSale()
	{
		return couponSale;
	}

	public boolean isUnlockedAll()
	{
		return unlockAllPurchased;
	}

	// controls subscription status request
	private boolean	subscriptionStatusRequest	= false;
	private long	timeLastSubscriptionCheck	= 0;							// should
																				// check
																				// each
																				// 32
																				// days
																				// 32*24*60*60*1000
	private long	days32						= 32 * 24 * 60 * 60 * 1000L;

	private void createBillingHandler()
	{
		try
		{
			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

			timeLastSubscriptionCheck = prefs.getLong("timeLastSubscriptionCheck", 0);
			if ((System.currentTimeMillis() - timeLastSubscriptionCheck) > days32)
				subscriptionStatusRequest = true;
			else
				subscriptionStatusRequest = false;

			// Log.e("Main billing!!!!!!!!!!!!!!",
			// "Subscription check timeLastSubscriptionCheck= " +
			// timeLastSubscriptionCheck + " diff is " +
			// (System.currentTimeMillis() - timeLastSubscriptionCheck) +
			// " days32 =" + Long.toString(days32));

			if ((isInstalled("com.almalence.hdr_plus")) || (isInstalled("com.almalence.pixfix")))
			{
				hdrPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_hdr", true).commit();
			}
			if (isInstalled("com.almalence.panorama.smoothpanorama"))
			{
				panoramaPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_panorama", true).commit();
			}

			String base64EncodedPublicKeyGoogle = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnztuXLNughHjGW55Zlgicr9r5bFP/K5DBc3jYhnOOo1GKX8M2grd7+SWeUHWwQk9lgQKat/ITESoNPE7ma0ZS1Qb/VfoY87uj9PhsRdkq3fg+31Q/tv5jUibSFrJqTf3Vmk1l/5K0ljnzX4bXI0p1gUoGd/DbQ0RJ3p4Dihl1p9pJWgfI9zUzYfvk2H+OQYe5GAKBYQuLORrVBbrF/iunmPkOFN8OcNjrTpLwWWAcxV5k0l5zFPrPVtkMZzKavTVWZhmzKNhCvs1d8NRwMM7XMejzDpI9A7T9egl6FAN4rRNWqlcZuGIMVizJJhvOfpCLtY971kQkYNXyilD40fefwIDAQAB";
			String base64EncodedPublicKeyYandex = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6KzaraKmv48Y+Oay2ZpWu4BHtSKYZidyCxbaYZmmOH4zlRNic/PDze7OA4a1buwdrBg3AAHwfVbHFzd9o91yinnHIWYQqyPg7L1Swh5W70xguL4jlF2N/xI9VoL4vMRv3Bf/79VfQ11utcPLHEXPR8nPEp9PT0wN2Hqp4yCWFbfvhVVmy7sQjywnfLqcWTcFCT6N/Xdxs1quq0hTE345MiCgkbh1xVULmkmZrL0rWDVCaxfK4iZWSRgQJUywJ6GMtUh+FU6/7nXDenC/vPHqnDR0R6BRi+QsES0ZnEfQLqNJoL+rqJDr/sDIlBQQDMQDxVOx0rBihy/FlHY34UF+bwIDAQAB";
			// Create the helper, passing it our context and the public key to
			// verify signatures with
			// Log.v("Main billing", "Creating IAB helper.");
			Map<String, String> storeKeys = new HashMap<String, String>();
			storeKeys.put(OpenIabHelper.NAME_GOOGLE, base64EncodedPublicKeyGoogle);
			storeKeys.put("com.yandex.store", base64EncodedPublicKeyYandex);
			mHelper = new OpenIabHelper(this, storeKeys);

			mHelper.enableDebugLogging(true);

			// Log.v("Main billing", "Starting setup.");
			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener()
			{
				public void onIabSetupFinished(IabResult result)
				{
					try
					{
						Log.v("Main billing", "Setup finished.");

						if (!result.isSuccess())
						{
							Log.v("Main billing", "Problem setting up in-app billing: " + result);
							return;
						}

						List<String> additionalSkuList = new ArrayList<String>();
						additionalSkuList.add(SKU_HDR);
						additionalSkuList.add(SKU_PANORAMA);
						additionalSkuList.add(SKU_UNLOCK_ALL);
						additionalSkuList.add(SKU_UNLOCK_ALL_COUPON);
						additionalSkuList.add(SKU_MOVING_SEQ);
						additionalSkuList.add(SKU_GROUPSHOT);
						additionalSkuList.add(SKU_SUBSCRIPTION_YEAR_CTRL);
						additionalSkuList.add(SKU_PROMO);

						if (subscriptionStatusRequest)
						{
							// Log.e("Main billing!!!!!!!!!!!!!!",
							// "Subscription check.");

							// subscription year
							additionalSkuList.add(SKU_SUBSCRIPTION_YEAR);
							// reset subscription status
							unlockAllSubscriptionYear = false;
							prefs.edit().putBoolean("subscription_unlock_all_year", false).commit();

							timeLastSubscriptionCheck = System.currentTimeMillis();
							prefs.edit().putLong("timeLastSubscriptionCheck", timeLastSubscriptionCheck).commit();
						}

						// for sale
						additionalSkuList.add(SKU_SALE1);
						additionalSkuList.add(SKU_SALE2);

						Log.v("Main billing", "Setup successful. Querying inventory.");
						mHelper.queryInventoryAsync(true, additionalSkuList, mGotInventoryListener);
					} catch (Exception e)
					{
						e.printStackTrace();
						Log.e("Main billing", "onIabSetupFinished exception: " + e.getMessage());
					}
				}
			});
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Main billing", "createBillingHandler exception: " + e.getMessage());
		}
	}

	private void destroyBillingHandler()
	{
		try
		{
			if (mHelper != null)
				mHelper.dispose();
			mHelper = null;
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Main billing", "destroyBillingHandler exception: " + e.getMessage());
		}
	}

	public String								titleUnlockAll				= "$6.95";
	public String								titleUnlockAllCoupon		= "$3.95";
	public String								titleUnlockHDR				= "$2.99";
	public String								titleUnlockPano				= "$2.99";
	public String								titleUnlockMoving			= "$3.99";
	public String								titleUnlockGroup			= "$2.99";
	public String								titleSubscriptionYear		= "$4.99";

	public String								summary_SKU_PROMO			= "";
//	public String								summaryUnlockAll			= "";
//	public String								summaryUnlockHDR			= "";
//	public String								summaryUnlockPano			= "";
//	public String								summaryUnlockMoving			= "";
//	public String								summaryUnlockGroup			= "";
//
//	public String								summarySubscriptionMonth	= "";
//	public String								summarySubscriptionYear		= "";

	IabHelper.QueryInventoryFinishedListener	mGotInventoryListener		= 
			new IabHelper.QueryInventoryFinishedListener()
			{
				public void onQueryInventoryFinished(
						IabResult result,
						Inventory inventory)
				{
					if (inventory == null)
					{
						Log.e("Main billing",
								"mGotInventoryListener inventory null ");
						return;
					}

																					SharedPreferences prefs = PreferenceManager
																							.getDefaultSharedPreferences(MainScreen
																									.getMainContext());

																					Editor prefsEditor = prefs.edit();
																					if (inventory.hasPurchase(SKU_HDR))
																					{
																						hdrPurchased = true;
																						prefsEditor.putBoolean(
																								"plugin_almalence_hdr",
																								true).commit();
																					}
																					if (inventory
																							.hasPurchase(SKU_PANORAMA))
																					{
																						panoramaPurchased = true;
																						prefsEditor
																								.putBoolean(
																										"plugin_almalence_panorama",
																										true).commit();
																					}
																					if (inventory
																							.hasPurchase(SKU_UNLOCK_ALL))
																					{
																						unlockAllPurchased = true;
																						prefsEditor.putBoolean(
																								"unlock_all_forever",
																								true).commit();
																					}
																					if (inventory
																							.hasPurchase(SKU_UNLOCK_ALL_COUPON))
																					{
																						unlockAllPurchased = true;
																						prefsEditor.putBoolean(
																								"unlock_all_forever",
																								true).commit();
																					}
																					if (inventory
																							.hasPurchase(SKU_MOVING_SEQ))
																					{
																						objectRemovalBurstPurchased = true;
																						prefsEditor
																								.putBoolean(
																										"plugin_almalence_moving_burst",
																										true).commit();
																					}
																					if (inventory
																							.hasPurchase(SKU_GROUPSHOT))
																					{
																						groupShotPurchased = true;
																						prefsEditor
																								.putBoolean(
																										"plugin_almalence_groupshot",
																										true).commit();
																					}
																					if (inventory
																							.hasPurchase(SKU_SUBSCRIPTION_YEAR))
																					{
																						unlockAllSubscriptionYear = true;
																						prefsEditor
																								.putBoolean(
																										"subscription_unlock_all_year",
																										true).commit();
																					}

																					try
																					{
																						String[] separated = inventory
																								.getSkuDetails(
																										SKU_SALE1)
																								.getPrice().split(",");
																						int price1 = Integer
																								.valueOf(separated[0]);
																						String[] separated2 = inventory
																								.getSkuDetails(
																										SKU_SALE2)
																								.getPrice().split(",");
																						int price2 = Integer
																								.valueOf(separated2[0]);

																						if (price1 < price2)
																							bOnSale = true;
																						else
																							bOnSale = false;

																						prefsEditor.putBoolean(
																								"bOnSale", bOnSale)
																								.commit();

																						// Log.e("Main billing SALE",
																						// "Sale status is "
																						// +
																						// bOnSale);
																					} catch (Exception e)
																					{
																						Log.e("Main billing SALE",
																								"No sale data available");
																						bOnSale = false;
																					}

																					try
																					{
																						titleUnlockAll = inventory
																								.getSkuDetails(
																										SKU_UNLOCK_ALL)
																								.getPrice();
																						titleUnlockAllCoupon = inventory
																								.getSkuDetails(
																										SKU_UNLOCK_ALL_COUPON)
																								.getPrice();
																						titleUnlockHDR = inventory
																								.getSkuDetails(SKU_HDR)
																								.getPrice();
																						titleUnlockPano = inventory
																								.getSkuDetails(
																										SKU_PANORAMA)
																								.getPrice();
																						titleUnlockMoving = inventory
																								.getSkuDetails(
																										SKU_MOVING_SEQ)
																								.getPrice();
																						titleUnlockGroup = inventory
																								.getSkuDetails(
																										SKU_GROUPSHOT)
																								.getPrice();

																						summary_SKU_PROMO = inventory
																						.getSkuDetails(
																								SKU_PROMO)
																						.getDescription();
																						
//																						summaryUnlockAll = inventory
//																								.getSkuDetails(
//																										SKU_UNLOCK_ALL)
//																								.getDescription();
//																						summaryUnlockHDR = inventory
//																								.getSkuDetails(SKU_HDR)
//																								.getDescription();
//																						summaryUnlockPano = inventory
//																								.getSkuDetails(
//																										SKU_PANORAMA)
//																								.getDescription();
//																						summaryUnlockMoving = inventory
//																								.getSkuDetails(
//																										SKU_MOVING_SEQ)
//																								.getDescription();
//																						summaryUnlockGroup = inventory
//																								.getSkuDetails(
//																										SKU_GROUPSHOT)
//																								.getDescription();
																					} catch (Exception e)
																					{
																						Log.e("Market!!!!!!!!!!!!!!!!!!!!!!!",
																								"Error Getting data for store!!!!!!!!");
																					}

//																					try
//																					{
//																						titleSubscriptionMonth = inventory
//																								.getSkuDetails(
//																										SKU_SUBSCRIPTION_MONTH)
//																								.getPrice();
//																						summarySubscriptionMonth = inventory
//																								.getSkuDetails(
//																										SKU_SUBSCRIPTION_MONTH)
//																								.getDescription();
//																					} catch (Exception e)
//																					{
//																						Log.e("Market!!!!!!!!!!!!!!!!!!!!!!!",
//																								"Error Getting data for store SubscriptionMonth!!!!!!!!");
//																					}
//																					try
//																					{
//																						titleSubscriptionYear = inventory
//																								.getSkuDetails(
//																										SKU_SUBSCRIPTION_YEAR)
//																								.getPrice();
//																						summarySubscriptionYear = inventory
//																								.getSkuDetails(
//																										SKU_SUBSCRIPTION_YEAR)
//																								.getDescription();
//																					} catch (Exception e)
//																					{
//																						Log.e("Market!!!!!!!!!!!!!!!!!!!!!!!",
//																								"Error Getting data for store SubscriptionYear!!!!!!!!");
//																					}
																				}
																			};

	private int									HDR_REQUEST					= 100;
	private int									PANORAMA_REQUEST			= 101;
	private int									ALL_REQUEST					= 102;
	private int									OBJECTREM_BURST_REQUEST		= 103;
	private int									GROUPSHOT_REQUEST			= 104;
	private int									SUBSCRIPTION_YEAR_REQUEST	= 106;

	public boolean isPurchasedAll()
	{
		return unlockAllPurchased;
	}

	public boolean isPurchasedHDR()
	{
		return hdrPurchased;
	}

	public boolean isPurchasedPanorama()
	{
		return panoramaPurchased;
	}

	public boolean isPurchasedMoving()
	{
		return objectRemovalBurstPurchased;
	}

	public boolean isPurchasedGroupshot()
	{
		return groupShotPurchased;
	}

	public boolean isPurchasedUnlockAllSubscriptionMonth()
	{
		return unlockAllSubscriptionMonth;
	}

	public boolean isPurchasedUnlockAllSubscriptionYear()
	{
		return unlockAllSubscriptionYear;
	}

	public void purchaseAll()
	{
		if (isPurchasedAll())
			return;
		
		//now will call store with abc unlocked
		callStoreForUnlocked(this);
		//iap functionality
//		String payload = "";
//		try
//		{
//			mHelper.launchPurchaseFlow(MainScreen.thiz, isCouponSale() ? SKU_UNLOCK_ALL_COUPON : SKU_UNLOCK_ALL,
//					ALL_REQUEST, mPreferencePurchaseFinishedListener, payload);
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			Log.e("Main billing", "Purchase result " + e.getMessage());
//			Toast.makeText(MainScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
//		}
	}

	public void purchaseHDR()
	{
		if (isPurchasedHDR() || isPurchasedAll())
			return;
		String payload = "";
		try
		{
			mHelper.launchPurchaseFlow(MainScreen.thiz, SKU_HDR, HDR_REQUEST, mPreferencePurchaseFinishedListener,
					payload);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	public void purchasePanorama()
	{
		if (isPurchasedPanorama() || isPurchasedAll())
			return;
		String payload = "";
		try
		{
			mHelper.launchPurchaseFlow(MainScreen.thiz, SKU_PANORAMA, PANORAMA_REQUEST,
					mPreferencePurchaseFinishedListener, payload);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	public void purchaseMultishot()
	{
		if (isPurchasedMoving() || isPurchasedAll())
			return;
		String payload = "";
		try
		{
			mHelper.launchPurchaseFlow(MainScreen.thiz, SKU_MOVING_SEQ, OBJECTREM_BURST_REQUEST,
					mPreferencePurchaseFinishedListener, payload);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	public void purchasedUnlockAllSubscriptionYear()
	{
		if (isPurchasedUnlockAllSubscriptionYear() || isPurchasedAll())
			return;
		String payload = "";
		try
		{
			mHelper.launchPurchaseFlow(MainScreen.thiz, SKU_SUBSCRIPTION_YEAR, SUBSCRIPTION_YEAR_REQUEST,
					mPreferencePurchaseFinishedListener, payload);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	// public void purchaseGroupshot()
	// {
	// if (isPurchasedGroupshot() || isPurchasedAll())
	// return;
	// String payload = "";
	// try
	// {
	// mHelper.launchPurchaseFlow(MainScreen.thiz, SKU_GROUPSHOT,
	// GROUPSHOT_REQUEST,
	// mPreferencePurchaseFinishedListener, payload);
	// } catch (Exception e)
	// {
	// e.printStackTrace();
	// Log.e("Main billing", "Purchase result " + e.getMessage());
	// Toast.makeText(MainScreen.thiz, "Error during purchase " +
	// e.getMessage(), Toast.LENGTH_LONG).show();
	// }
	// }

	// Callback for when purchase from preferences is finished
	IabHelper.OnIabPurchaseFinishedListener	mPreferencePurchaseFinishedListener	= new IabHelper.OnIabPurchaseFinishedListener()
																				{
																					public void onIabPurchaseFinished(
																							IabResult result,
																							Purchase purchase)
																					{
																						showStore = true;
																						purchaseFinished(result,
																								purchase);
																					}

																				};

	private void purchaseFinished(IabResult result, Purchase purchase)
	{
		Log.v("Main billing", "Purchase finished: " + result + ", purchase: " + purchase);
		if (result.isFailure())
		{
			Log.v("Main billing", "Error purchasing: " + result);
			return;
		}

		Log.v("Main billing", "Purchase successful.");

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		if (purchase.getSku().equals(SKU_HDR))
		{
			Log.v("Main billing", "Purchase HDR.");
			hdrPurchased = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("plugin_almalence_hdr", true).commit();
		}
		if (purchase.getSku().equals(SKU_PANORAMA))
		{
			Log.v("Main billing", "Purchase Panorama.");
			panoramaPurchased = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("plugin_almalence_panorama", true).commit();
		}
		if (purchase.getSku().equals(SKU_UNLOCK_ALL))
		{
			Log.v("Main billing", "Purchase unlock_all_forever.");
			unlockAllPurchased = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("unlock_all_forever", true).commit();
		}
		if (purchase.getSku().equals(SKU_UNLOCK_ALL_COUPON))
		{
			Log.v("Main billing", "Purchase unlock_all_forever_coupon.");
			unlockAllPurchased = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("unlock_all_forever", true).commit();
		}
		if (purchase.getSku().equals(SKU_MOVING_SEQ))
		{
			Log.v("Main billing", "Purchase plugin_almalence_moving_burst.");
			objectRemovalBurstPurchased = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("plugin_almalence_moving_burst", true).commit();
		}
		if (purchase.getSku().equals(SKU_GROUPSHOT))
		{
			Log.v("Main billing", "Purchase plugin_almalence_groupshot.");
			groupShotPurchased = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("plugin_almalence_groupshot", true).commit();
		}
		if (purchase.getSku().equals(SKU_SUBSCRIPTION_YEAR))
		{
			Log.v("Main billing", "Purchase year subscription.");
			unlockAllSubscriptionYear = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("subscription_unlock_all_year", true).commit();

			timeLastSubscriptionCheck = System.currentTimeMillis();
			prefs.edit().putLong("timeLastSubscriptionCheck", timeLastSubscriptionCheck).commit();
		}
	}

	public void launchPurchase(int requestID)
	{
		try
		{
			guiManager.showStore();
		} catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(this, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	IabHelper.OnIabPurchaseFinishedListener	mPurchaseFinishedListener	= new IabHelper.OnIabPurchaseFinishedListener()
																		{
																			public void onIabPurchaseFinished(
																					IabResult result, Purchase purchase)
																			{

																				guiManager.showStore();
																				purchaseFinished(result, purchase);
																			}
																		};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		Log.v("Main billing", "onActivityResult(" + requestCode + "," + resultCode + "," + data);

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data))
		{
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		} else
		{
			Log.v("Main billing", "onActivityResult handled by IABUtil.");
		}
	}

	public boolean	showPromoRedeemed	= false;

	// enter promo code to get smth
	public void enterPromo()
	{
		final float density = getResources().getDisplayMetrics().density;

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));

		// rating bar
		final EditText editText = new EditText(this);
		editText.setHint(R.string.Pref_Upgrde_PromoCode_Text);
		editText.setHintTextColor(Color.WHITE);

		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.gravity = Gravity.CENTER_HORIZONTAL;
		params.setMargins(0, 20, 0, 30);
		editText.setLayoutParams(params);
		ll.addView(editText);

		Button b3 = new Button(this);
		b3.setText(getResources().getString(R.string.Pref_Upgrde_PromoCode_DoneText));
		ll.addView(b3);

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(ll);
		final AlertDialog dialog = builder.create();

		b3.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				String[] sep = MainScreen.getInstance().summary_SKU_PROMO.split(";");
				String promo = editText.getText().toString();
				boolean matchPromo = false;
				
				for (int i = 0; i<sep.length;i++)
				{
					if (promo.equalsIgnoreCase(sep[i]))
						matchPromo = true;
				}
				
				//if (promo.equalsIgnoreCase("appoftheday") || promo.equalsIgnoreCase("stelapps"))
				if (matchPromo)
				{
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
					unlockAllPurchased = true;

					Editor prefsEditor = prefs.edit();
					prefsEditor.putBoolean("unlock_all_forever", true);
					prefsEditor.commit();
					dialog.dismiss();
					guiManager.hideStore();
					showPromoRedeemed = true;
					guiManager.showStore();
				} else
				{
					editText.setText("");
					editText.setHint(R.string.Pref_Upgrde_PromoCode_IncorrectText);
				}
			}
		});

		dialog.show();
	}

	// next methods used to store number of free launches.
	// using files to store this info

	// returns number of launches left
	public int getLeftLaunches(String modeID)
	{
		String dirPath = getFilesDir().getAbsolutePath() + File.separator + modeID;
		File projDir = new File(dirPath);
		if (!projDir.exists())
		{
			projDir.mkdirs();
			WriteLaunches(projDir, 30);
		}
		int left = ReadLaunches(projDir);
		return left;
	}

	// decrements number of launches left
	public void decrementLeftLaunches(String modeID)
	{
		String dirPath = getFilesDir().getAbsolutePath() + File.separator + modeID;
		File projDir = new File(dirPath);
		if (!projDir.exists())
		{
			projDir.mkdirs();
			WriteLaunches(projDir, 30);
		}

		int left = ReadLaunches(projDir);
		if (left > 0)
			WriteLaunches(projDir, left - 1);

		if (left == 5)
		{
			// show subscription dialog
			showSubscriptionDialog();
			return;
		}
	}

	// writes number of launches left into memory
	private void WriteLaunches(File projDir, int left)
	{
		FileOutputStream fos = null;
		try
		{
			fos = new FileOutputStream(projDir + "/left");
			fos.write(left);
			fos.close();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	// reads number of launches left from memory
	private int ReadLaunches(File projDir)
	{
		int left = 0;
		FileInputStream fis = null;
		try
		{
			fis = new FileInputStream(projDir + "/left");
			left = fis.read();
			fis.close();
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
		return left;
	}

	public boolean checkLaunches(Mode mode)
	{
		// if mode free
		if (mode.SKU == null)
			return true;
		if (mode.SKU.isEmpty())
			return true;

		// if all unlocked
		if (unlockAllPurchased)
			return true;

		// if current mode unlocked
		if (mode.SKU.equals("plugin_almalence_hdr"))
		{
			if (hdrPurchased)
				return true;
		}
		if (mode.SKU.equals("plugin_almalence_video"))
		{
			if (hdrPurchased)
				return true;
		} else if (mode.SKU.equals("plugin_almalence_panorama_augmented"))
		{
			if (panoramaPurchased)
				return true;
		} else if (mode.SKU.equals("plugin_almalence_moving_burst"))
		{
			if (objectRemovalBurstPurchased)
				return true;
		} else if (mode.SKU.equals("plugin_almalence_groupshot"))
		{
			if (groupShotPurchased)
				return true;
		}

		int launchesLeft = MainScreen.thiz.getLeftLaunches(mode.modeID);
		int id = MainScreen.thiz.getResources()
				.getIdentifier(mode.modeName, "string", MainScreen.thiz.getPackageName());
		String modename = MainScreen.thiz.getResources().getString(id);

		if (0 == launchesLeft)// no more launches left
		{
			String left = String.format(getResources().getString(R.string.trial_finished), modename);
			Toast toast = Toast.makeText(this, left, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

			// show appstore for this mode
			launchPurchase(100);
			return false;
		} else if ((10 == launchesLeft) || (20 == launchesLeft) || (5 >= launchesLeft))
		{
			// show appstore button and say that it cost money
			String left = String.format(getResources().getString(R.string.trial_left), modename, launchesLeft);
			Toast toast = Toast.makeText(this, left, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();
		}
		return true;
	}

	private boolean isInstalled(String packageName)
	{
		PackageManager pm = getPackageManager();
		boolean installed = false;
		try
		{
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			installed = true;
		} catch (PackageManager.NameNotFoundException e)
		{
			installed = false;
		}
		return installed;
	}

	private void showSubscriptionDialog()
	{
		final float density = getResources().getDisplayMetrics().density;

		LinearLayout ll = new LinearLayout(this);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));

		ImageView img = new ImageView(this);
		img.setImageResource(R.drawable.store_subscription);
		ll.addView(img);

		TextView tv = new TextView(this);
		tv.setText(MainScreen.getInstance().getResources().getString(R.string.subscriptionText));
		tv.setWidth((int) (250 * density));
		tv.setPadding((int) (4 * density), 0, (int) (4 * density), (int) (24 * density));
		ll.addView(tv);

		Button bNo = new Button(this);
		bNo.setText(MainScreen.getInstance().getResources().getString(R.string.subscriptionNoText));
		ll.addView(bNo);

		Button bSubscribe = new Button(this);
		bSubscribe.setText(MainScreen.getInstance().getResources().getString(R.string.subscriptionYesText));
		ll.addView(bSubscribe);

		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
		builder.setView(ll);
		final AlertDialog dialog = builder.create();

		bSubscribe.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				purchasedUnlockAllSubscriptionYear();
				dialog.dismiss();
			}
		});

		bNo.setOnClickListener(new OnClickListener()
		{
			public void onClick(View v)
			{
				dialog.dismiss();
			}
		});

		dialog.show();
	}
	
	private boolean isABCUnlockedInstalled(Activity activity)
	{
		try
		{
			activity.getPackageManager().getInstallerPackageName("com.almalence.opencam_plus");
		} catch (IllegalArgumentException e)
		{
			return false;
		}

		return true;
	}

	private void callStoreForUnlocked(Activity activity)
	{
		try
		{
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("market://details?id=com.almalence.opencam_plus"));
			activity.startActivity(intent);
		} catch (ActivityNotFoundException e)
		{
			return;
		}
	}
	
	// -+- -->

	/************************ Billing ************************/
	/*******************************************************/

	// <!-- -+-

	// Application rater code
	public static void callStoreFree(Activity act)
	{
		try
		{
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("market://details?id=com.almalence.opencam"));
			act.startActivity(intent);
		} catch (ActivityNotFoundException e)
		{
			return;
		}
	}

	// -+- -->

	// installing packages from play store
	public static void callStoreInstall(Activity act, String id)
	{
		try
		{
			Intent intent = new Intent(Intent.ACTION_VIEW);
			intent.setData(Uri.parse("market://details?id=" + id));
			act.startActivity(intent);
		} catch (ActivityNotFoundException e)
		{
			return;
		}
	}

	private void resetOrSaveSettings()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		Editor prefsEditor = prefs.edit();
		boolean isSaving = prefs.getBoolean("SaveConfiguration_Mode", true);
		if (!isSaving)
		{
			prefsEditor.putString("defaultModeName", "single");
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_ImageSize", true);
		if (!isSaving)
		{
			// general settings - image size
			prefsEditor.putString("imageSizePrefCommonBack", "-1");
			prefsEditor.putString("imageSizePrefCommonFront", "-1");
			// night hi sped image size
			prefsEditor.putString("imageSizePrefNightBack", "-1");
			prefsEditor.putString("pref_plugin_capture_panoramaaugmented_imageheight", "0");
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_SceneMode", false);
		if (!isSaving)
		{
			prefsEditor.putInt(sSceneModePref, sDefaultValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_FocusMode", true);
		if (!isSaving)
		{
			prefsEditor.putInt("sRearFocusModePref", sDefaultFocusValue);
			prefsEditor.putInt(sFrontFocusModePref, sDefaultFocusValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_WBMode", false);
		if (!isSaving)
		{
			prefsEditor.putInt(sWBModePref, sDefaultValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_ISOMode", false);
		if (!isSaving)
		{
			prefsEditor.putInt(sISOPref, sDefaultValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_FlashMode", true);
		if (!isSaving)
		{
			prefsEditor.putInt(sFlashModePref, sDefaultValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_FrontRearCamera", true);
		if (!isSaving)
		{
			prefsEditor.putBoolean("useFrontCamera", false);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_ExpoCompensation", false);
		if (!isSaving)
		{
			prefsEditor.putInt("EvCompensationValue", 0);
			prefsEditor.commit();
		}
	}
}
