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
import java.util.Locale;
import java.util.Map;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.ImageReader;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Debug;
import android.os.Environment;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.util.Pair;
import android.view.Display;
import android.view.Gravity;
import android.view.KeyEvent;
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

import com.almalence.plugins.capture.panoramaaugmented.PanoramaAugmentedCapturePlugin;
import com.almalence.plugins.capture.video.VideoCapturePlugin;
import com.almalence.sony.cameraremote.SimpleStreamSurfaceView;
import com.almalence.sony.cameraremote.utils.NFCHandler;
import com.almalence.sony.cameraremote.utils.WifiHandler;
import com.almalence.util.AppWidgetNotifier;

//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.AlmalenceGUI;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;
import com.almalence.util.AppRater;
//-+- -->
/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.ui.AlmalenceGUI;
 import com.almalence.opencam_plus.ui.GLLayer;
 import com.almalence.opencam_plus.ui.GUI;
 +++ --> */

/***
 * MainScreen - main activity screen with camera functionality
 * 
 * Passes all main events to PluginManager
 ***/

@SuppressWarnings("deprecation")
public class MainScreen extends ApplicationScreen
{
	// >>Description
	// section with different global parameters available for everyone
	//
	// Camera parameters and possibly access to camera instance
	//
	// Global defines and others
	//
	// Description<<

	private static final int	MODE_GENERAL					= 0;
	private static final int	MODE_SMART_MULTISHOT_AND_NIGHT	= 1;
	private static final int	MODE_PANORAMA					= 2;
	private static final int	MODE_VIDEO						= 3;

	private static final int	MIN_MPIX_SUPPORTED				= 1280 * 960;
	private static final int	MIN_MPIX_PREVIEW				= 600 * 400;

	public static MainScreen	thiz;

	// Interface to Camera2 camera and Old style camera

	// Camera2 camera's objects
	private ImageReader			mImageReaderPreviewYUV;
	private ImageReader			mImageReaderYUV;
	private ImageReader			mImageReaderJPEG;
	private ImageReader			mImageReaderRAW;
	private MediaRecorder		mMediaRecorder;

	// Common preferences
	private int					imageSizeIdxPreference;
	private int					multishotImageSizeIdxPreference;
	private boolean				shutterPreference				= true;
	private int					shotOnTapPreference				= 0;

	private boolean				showHelp						= false;

	// private boolean keepScreenOn = false;

	private static boolean		maxScreenBrightnessPreference;

	// >>Description
	// section with initialize, resume, start, stop procedures, preferences
	// access
	//
	// Initialize, stop etc depends on plugin type.
	//
	// Create main GUI controls and plugin specific controls.
	//
	// Description<<

	// Clicked mode id from widget.
	public static final String	EXTRA_ITEM						= "WidgetModeID";

	public static final String	EXTRA_TORCH						= "WidgetTorchMode";
	public static final String	EXTRA_BARCODE					= "WidgetBarcodeMode";
	public static final String	EXTRA_SHOP						= "WidgetGoShopping";

	private static boolean		launchTorch						= false;
	private static boolean		launchBarcode					= false;
	private static boolean		goShopping						= false;

	private static int			prefFlash						= -1;
	private static boolean		prefBarcode						= false;

	private static final int	VOLUME_FUNC_SHUTTER				= 0;
	private static final int	VOLUME_FUNC_EXPO				= 2;
	private static final int	VOLUME_FUNC_NONE				= 3;

	public static String		sKeepScreenOn;
	public static String		sFastSwitchShutterOn;

	public static String		sDelayedCapturePref;
	public static String		sShowDelayedCapturePref;
	public static String		sDelayedSoundPref;
	public static String		sDelayedFlashPref;
	public static String		sDelayedCaptureIntervalPref;

	public static String		sPhotoTimeLapseCaptureIntervalPref;
	public static String		sPhotoTimeLapseCaptureIntervalMeasurmentPref;
	public static String		sPhotoTimeLapseActivePref;
	public static String		sPhotoTimeLapseIsRunningPref;
	public static String		sPhotoTimeLapseCount;

	private static String		sShutterPref;
	private static String		sShotOnTapPref;
	private static String		sVolumeButtonPref;

	public static String		sSonyCamerasPref;
	public static String		sDefaultInfoSetPref;
	public static String		sSWCheckedPref;
	public static String		sSavePathPref;
	public static String		sExportNamePref;
	public static String		sExportNamePrefixPref;
	public static String		sExportNamePostfixPref;
	public static String		sSaveToPref;
	
	public static String		sLastPhotoModePref;

	// Camera parameters info
	int							cameraId;
	List<CameraController.Size>	preview_sizes;
	List<CameraController.Size>	video_sizes;
	List<CameraController.Size>	picture_sizes;
	boolean						supports_video_stabilization;
	List<String>				flash_values;
	List<String>				focus_values;
	List<String>				scene_modes_values;
	List<String>				white_balances_values;
	List<String>				isos;
	String						flattenParamteters;

	private NfcAdapter			mNfcAdapter;
	private WifiHandler			mWifiHandler;
	
	
	public static MainScreen getInstance()
	{
		return thiz;
	}

	protected void createPluginManager()
	{
		pluginManager = PluginManager.getInstance();
	}

	/*
	 * Try to catch NFC intent
	 */
	@Override
	protected void onNewIntent(Intent intent)
	{
		try
		{
			Pair<String, String> cameraWifiSettings = NFCHandler.parseIntent(intent);
			mWifiHandler.createIfNeededThenConnectToWifi(cameraWifiSettings.first, cameraWifiSettings.second);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	protected void duringOnCreate()
	{
		thiz = this;

		mApplicationStarted = false;
		isForceClose = false;

		sDelayedCapturePref = getResources().getString(R.string.Preference_DelayedCaptureValue);
		sShowDelayedCapturePref = getResources().getString(R.string.Preference_ShowDelayedCaptureValue);
		sDelayedSoundPref = getResources().getString(R.string.Preference_DelayedSoundValue);
		sDelayedFlashPref = getResources().getString(R.string.Preference_DelayedFlashValue);
		sDelayedCaptureIntervalPref = getResources().getString(R.string.Preference_DelayedCaptureIntervalValue);

		sDelayedCapturePref = getResources().getString(R.string.Preference_DelayedCaptureValue);
		sShowDelayedCapturePref = getResources().getString(R.string.Preference_ShowDelayedCaptureValue);
		sDelayedSoundPref = getResources().getString(R.string.Preference_DelayedSoundValue);
		sDelayedFlashPref = getResources().getString(R.string.Preference_DelayedFlashValue);
		sDelayedCaptureIntervalPref = getResources().getString(R.string.Preference_DelayedCaptureIntervalValue);

		sPhotoTimeLapseCaptureIntervalPref = getResources()
				.getString(R.string.Preference_PhotoTimeLapseCaptureInterval);
		sPhotoTimeLapseCaptureIntervalMeasurmentPref = getResources().getString(
				R.string.Preference_PhotoTimeLapseCaptureIntervalMeasurment);
		sPhotoTimeLapseActivePref = getResources().getString(R.string.Preference_PhotoTimeLapseSWChecked);
		sPhotoTimeLapseIsRunningPref = getResources().getString(R.string.Preference_PhotoTimeLapseIsRunning);
		sPhotoTimeLapseCount = getResources().getString(R.string.Preference_PhotoTimeLapseCount);

		sShutterPref = getResources().getString(R.string.Preference_ShutterCommonValue);
		sSonyCamerasPref = getResources().getString(R.string.Preference_ConnectToSonyCameras);
		sShotOnTapPref = getResources().getString(R.string.Preference_ShotOnTapValue);
		sVolumeButtonPref = getResources().getString(R.string.Preference_VolumeButtonValue);

		sDefaultInfoSetPref = getResources().getString(R.string.Preference_DefaultInfoSetValue);
		sSWCheckedPref = getResources().getString(R.string.Preference_SWCheckedValue);

		sExportNamePref = getResources().getString(R.string.Preference_ExportNameValue);
		sExportNamePrefixPref = getResources().getString(R.string.Preference_SavePathPrefixValue);
		sExportNamePostfixPref = getResources().getString(R.string.Preference_SavePathPostfixValue);
		sSortByDataPref = getResources().getString(R.string.Preference_SortByDataValue);
		sEnableExifOrientationTagPref = getResources().getString(R.string.Preference_EnableExifTagOrientationValue);
		sAdditionalRotationPref = getResources().getString(R.string.Preference_AdditionalRotationValue);

		sKeepScreenOn = getResources().getString(R.string.Preference_KeepScreenOnValue);
		sFastSwitchShutterOn = getResources().getString(R.string.Preference_ShowFastSwitchShutterValue);
		
		sSavePathPref = getResources().getString(R.string.Preference_SavePathValue);
		sSaveToPref = getResources().getString(R.string.Preference_SaveToValue);
		
		sLastPhotoModePref = getResources().getString(R.string.Preference_LastPhotoModeValue);

		Intent intent = this.getIntent();
		String mode = intent.getStringExtra(EXTRA_ITEM);
		launchTorch = intent.getBooleanExtra(EXTRA_TORCH, false);
		launchBarcode = intent.getBooleanExtra(EXTRA_BARCODE, false);

		// reset or save settings
		resetOrSaveSettings();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		SavingService.initSavingPrefs(getApplicationContext());

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
		if (true == prefs.contains("subscription_unlock_all_year"))
		{
			unlockAllSubscriptionYear = prefs.getBoolean("subscription_unlock_all_year", false);
		}

		if (!unlockAllPurchased)
			createBillingHandler();

		/**** Billing *****/

		// application rating helper
		AppRater.app_launched(this);
		// -+- -->

		AppWidgetNotifier.app_launched(this);

		keepScreenOn = prefs.getBoolean(sKeepScreenOn, false);

		mNfcAdapter = NfcAdapter.getDefaultAdapter(this);
		mWifiHandler = new WifiHandler(this);
	}

	@Override
	protected void afterOnCreate()
	{
		PluginManager.getInstance().setupDefaultMode();
		// init gui manager
		guiManager = new AlmalenceGUI();
//		guiManager.createInitialGUI();
//		this.findViewById(R.id.mainLayout1).invalidate();
//		this.findViewById(R.id.mainLayout1).requestLayout();
//		guiManager.onCreate();

		// init plugin manager
//		PluginManager.getInstance().onCreate();

		Intent intent = this.getIntent();
		goShopping = intent.getBooleanExtra(EXTRA_SHOP, false);

		// <!-- -+-
		if (goShopping)
		{
			if (titleUnlockAll == null || titleUnlockAll.endsWith("check for sale"))
			{
				Toast.makeText(MainScreen.getMainContext(),
						"Error connecting to Google Play. Check internet connection.", Toast.LENGTH_LONG).show();
				return;
			}
			guiManager.showStore();
		}
		// -+- -->
	}
	
	@Override
	protected void onApplicationStart()
	{
		setContentView(R.layout.opencamera_main_layout);
		
		findViewById(R.id.SurfaceView02).setVisibility(View.GONE);
		preview = (SurfaceView) this.findViewById(R.id.SurfaceView01);
		preview.setOnClickListener(this);
		preview.setOnTouchListener(this);
		preview.setKeepScreenOn(true);

		surfaceHolder = preview.getHolder();
		surfaceHolder.addCallback(this);
		
		mWifiHandler.register();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

		boolean isCamera2 = prefs.getBoolean(getResources().getString(R.string.Preference_UseCamera2Key),
				(CameraController.isNexus5or6 || CameraController.isFlex2 || CameraController.isAndroidOne || CameraController.isGalaxyS6 /*|| CameraController.isG4*/) ? true : false);
		CameraController.setUseCamera2(isCamera2);
		prefs.edit()
				.putBoolean(getResources().getString(R.string.Preference_UseCamera2Key), CameraController.isUseCamera2())
				.commit();
		int cameraSelected = prefs.getInt(MainScreen.sCameraModePref, 0);
		if (cameraSelected == CameraController.getNumberOfCameras() - 1)
		{
			prefs.edit().putInt(ApplicationScreen.sCameraModePref, 0).commit();
			MainScreen.getGUIManager().setCameraModeGUI(0);
		}

		CameraController.onStart();
		MainScreen.getGUIManager().onStart();
		PluginManager.getInstance().onStart();
	}
	
	@Override
	protected void onApplicationResume()
	{
		//resets all requests for preview frames on restart.
		CameraController.resetNeedPreviewFrame();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
// <!-- -+-
        //check appturbo app of the month conditions
        if (!unlockAllPurchased)
        {
        	if (isAppturboUnlockable(this))
        	{
        		unlockAllPurchased = true;
    			Editor prefsEditor = prefs.edit();
    			prefsEditor.putBoolean("unlock_all_forever", true).commit();
        		Toast.makeText(MainScreen.getMainContext(), this.getResources().getString(R.string.string_appoftheday), Toast.LENGTH_LONG).show();
        	}
        }
// -+- -->
        
		isCameraConfiguring = false;

		if (PluginManager.getInstance().getActiveModeID().contains("video"))
		{
			mMediaRecorder = new MediaRecorder();
		}

		mWifiHandler.register();
		if (mNfcAdapter != null) {
			mNfcAdapter.enableForegroundDispatch(this, NFCHandler.getPendingIntent(this),
					NFCHandler.getIntentFilterArray(), NFCHandler.getTechListArray());
		}

		if (!isCreating)
		{
			//Such separation is needed due to Android 6 bug with half-visible preview on Nexus 5
			//At this moment we only found that CountDownTimer somehow affect on it.
			//Corrupted preview still occurs but less often
			//TODO: investigate deeper that problem
			if(CameraController.isUseCamera2())
				onResumeCamera();
			else
				onResumeTimer = new CountDownTimer(50, 50)
				{
					public void onTick(long millisUntilFinished){}
	
					public void onFinish()
					{
						onResumeCamera();
					}
				}.start();
		}

		shutterPlayer = new SoundPlayer(this.getBaseContext(), getResources().openRawResourceFd(
				R.raw.plugin_capture_tick));

		if (screenTimer != null)
		{
			if (isScreenTimerRunning)
				screenTimer.cancel();
			screenTimer.start();
			isScreenTimerRunning = true;
		}

		//checking for available memory
		long memoryFree = getAvailableInternalMemory();
		if (memoryFree < 30)
			Toast.makeText(MainScreen.getMainContext(), "Almost no free space left on internal storage.",
					Toast.LENGTH_LONG).show();

		boolean dismissKeyguard = prefs.getBoolean("dismissKeyguard", true);
		if (dismissKeyguard)
			getWindow()
					.addFlags(
							WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
									| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		else
		{
			getWindow()
					.clearFlags(
							WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
									| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		}

		// <!-- -+-
		if (isABCUnlockedInstalled(this))
		{
			unlockAllPurchased = true;
			prefs.edit().putBoolean("unlock_all_forever", true).commit();
		}
		// -+- -->
	}
	
	protected void onResumeCamera()
	{
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.getMainContext());

		updatePreferences();

		captureFormat = CameraController.JPEG;

		maxScreenBrightnessPreference = prefs.getBoolean("maxScreenBrightnessPref", false);
		setScreenBrightness(maxScreenBrightnessPreference);

		MainScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);

		boolean openCamera = false;
		String modeId = PluginManager.getInstance().getActiveModeID();
		if (CameraController.isRemoteCamera() && !(modeId.contains("single") || modeId.contains("video")))
		{
			openCamera = true;
			prefs.edit().putInt(MainScreen.sCameraModePref, 0).commit();
			CameraController.setCameraIndex(0);
			guiManager.setCameraModeGUI(0);
		}

		CameraController.onResume();
		MainScreen.getGUIManager().onResume();
		PluginManager.getInstance().onResume();
		
		MainScreen.thiz.mPausing = false;

		if (!CameraController.isRemoteCamera())
		{
			// set preview, on click listener and surface buffers
			findViewById(R.id.SurfaceView02).setVisibility(View.GONE);
			preview = (SurfaceView) findViewById(R.id.SurfaceView01);

			surfaceHolder = preview.getHolder();
			surfaceHolder.addCallback(MainScreen.this);

			preview.setVisibility(View.VISIBLE);
			preview.setOnClickListener(MainScreen.this);
			preview.setOnTouchListener(MainScreen.this);
			preview.setKeepScreenOn(true);

			//Due to google's advice how to prevent surfaceView bug in Android 6
			//we have to force reconfigure of surfaceView (surfaceChanged callback will be call)
			//and only after it we may configure surfaceView with desired size
			if (CameraController.isUseCamera2())
			{
				isSurfaceConfiguring = true;
				//If call method only once surfaceChanged will not be call, so we have to set fake size twice
				MainScreen.setSurfaceHolderSize(0, 0);
				MainScreen.setSurfaceHolderSize(1, 1);
			}
			else
				isSurfaceConfiguring = false; //In camera1 mode logic isn't changed

			if (CameraController.isUseCamera2())
			{
				Log.d("MainScreen", "onResume: CameraController.setupCamera(null)");
				CameraController.setupCamera(null, !switchingMode || openCamera);

				if (glView != null)
				{
					glView.onResume();
					Log.d("GL", "glView onResume");
				}
			} else if ((surfaceCreated && (!CameraController.isCameraCreated())) ||
			// this is for change mode without camera restart!
					(surfaceCreated && MainScreen.getInstance().getSwitchingMode()))
			{
				CameraController.setupCamera(surfaceHolder, !switchingMode || openCamera);

				if (glView != null)
				{
					glView.onResume();
					Log.d("GL", "glView onResume");
				}
			}
		} else
		{
			sonyCameraSelected();
		}

		if (preview != null)
		{
			preview.setKeepScreenOn(keepScreenOn);
		}
		orientListener.enable();		
	}
	
	@Override
	protected void onApplicationPause()
	{
		if (mNfcAdapter != null) {
			mNfcAdapter.disableForegroundDispatch(this);
		}

		if (onResumeTimer != null)
		{
			onResumeTimer.cancel();
		}

		if (mMediaRecorder != null) 
		{
			mMediaRecorder.release();
			mMediaRecorder = null;
		}
		
		mApplicationStarted = false;

		MainScreen.getGUIManager().onPause();
		PluginManager.getInstance().onPause(true);

		orientListener.disable();

		if (shutterPreference)
		{
			AudioManager mgr = (AudioManager) MainScreen.thiz.getSystemService(MainScreen.AUDIO_SERVICE);
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

		CameraController.onPause(switchingMode);
		switchingMode = false;

		if (!CameraController.isRemoteCamera())
		{
			if (CameraController.isUseCamera2())
				stopImageReaders();
		} else
		{
			stopRemotePreview();
		}

		this.findViewById(R.id.mainLayout2).setVisibility(View.INVISIBLE);

		if (shutterPlayer != null)
		{
			shutterPlayer.release();
			shutterPlayer = null;
		}
	}	

	@Override
	protected void onApplicationStop()
	{
		switchingMode = false;
		mApplicationStarted = false;
		orientationMain = 0;
		orientationMainPrevious = 0;
		ApplicationScreen.getGUIManager().onStop();
		ApplicationScreen.getPluginManager().onStop();
		CameraController.onStop();

		if (!CameraController.isRemoteCamera())
		{
			if (CameraController.isUseCamera2())
				stopImageReaders();
		}

		mWifiHandler.reconnectToLastWifi();
		mWifiHandler.unregister();
	}
	
	
	@Override
	protected void onApplicationDestroy()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		if (launchTorch && prefs.getInt(sFlashModePref, -1) == CameraParameters.FLASH_MODE_TORCH)
		{
			prefs.edit().putInt(sFlashModePref, prefFlash).commit();
		}
		if (launchBarcode && prefs.getBoolean("PrefBarcodescannerVF", false))
		{
			prefs.edit().putBoolean("PrefBarcodescannerVF", prefBarcode).commit();
		}

		prefs.edit().putBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);
		prefs.edit().putBoolean(MainScreen.sPhotoTimeLapseActivePref, false);

		MainScreen.getGUIManager().onDestroy();
		PluginManager.getInstance().onDestroy();
		CameraController.onDestroy();

		// <!-- -+-
		/**** Billing *****/
		destroyBillingHandler();
		/**** Billing *****/
		// -+- -->

//		this.hideOpenGLLayer();
	}



	private void updatePreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		CameraController.setCameraIndex(prefs.getInt(MainScreen.sCameraModePref, 0));
		shutterPreference = prefs.getBoolean(MainScreen.sShutterPref, false);
		shotOnTapPreference = Integer.parseInt(prefs.getString(MainScreen.sShotOnTapPref, "0"));

		if (!CameraController.isRemoteCamera())
		{
			imageSizeIdxPreference = Integer.parseInt(prefs.getString(
					CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeRearPref
							: MainScreen.sImageSizeFrontPref, "-1"));

			multishotImageSizeIdxPreference = Integer
					.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? sImageSizeMultishotBackPref
							: sImageSizeMultishotFrontPref, "-1"));
		} else
		{
			imageSizeIdxPreference = Integer.parseInt(prefs.getString(MainScreen.sImageSizeSonyRemotePref, "-1"));
			multishotImageSizeIdxPreference = Integer.parseInt(prefs.getString(
					MainScreen.sImageSizeMultishotSonyRemotePref, "-1"));
		}

		multishotImageSizeIdxPreference = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? sImageSizeMultishotBackPref : sImageSizeMultishotFrontPref,
				"-1"));

		keepScreenOn = prefs.getBoolean(sKeepScreenOn, false);
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
		mCameraSurface = holder.getSurface();

		//In camera2 mode we have to wait a second call of surfaceChanged to continue configuring of camera
		//First call of this function occurs after setSurfaceFixedSize(1, 1) call in onResumeCamera method.
		//Variable isSurfaceConfiguring is used to separate first 'fake' call on surfaceChanged from second 'real' call
		//when we set desired surfaceView size
		//More info read from: https://code.google.com/p/android/issues/detail?id=191251
		if (isCameraConfiguring && !isSurfaceConfiguring)
		{
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_SURFACE_CONFIGURED, 0);
			isCameraConfiguring = false;
		} else if (!isCreating && !isSurfaceConfiguring)
		{
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
						Log.d("MainScreen", "surfaceChanged: CameraController.setupCamera(null). SurfaceSize = "
								+ width + "x" + height);
						if (!CameraController.isRemoteCamera())
						{
							if (!CameraController.isUseCamera2())
							{
								CameraController.setupCamera(holder, !switchingMode);
							} else
							{
								Log.e("MainScreen",
										"surfaceChanged: sendEmptyMessage(ApplicationInterface.MSG_SURFACE_READY)");
								messageHandler.sendEmptyMessage(ApplicationInterface.MSG_SURFACE_READY);
							}
						}
					}
				}
			}.start();
		} else if(isSurfaceConfiguring)
		{
			//Section for first 'fake' call (setSurfaceFixedSize(1, 1))
			//We change flag to allow next surfaceChanged call to continue configuring of camera
			isSurfaceConfiguring = false;
		}
		else
			updatePreferences();
	}

	public void getCameraParametersBundle()
	{
		try
		{
			cameraId = CameraController.getCameraIndex();
			supports_video_stabilization = CameraController.getVideoStabilizationSupported();

			scene_modes_values = CameraController.getSupportedSceneModesNames();
			white_balances_values = CameraController.getSupportedWhiteBalanceNames();
			isos = CameraController.getSupportedISONames();

			preview_sizes = CameraController.getSupportedPreviewSizes();

			picture_sizes = CameraController.getSupportedPictureSizes();

			video_sizes = CameraController.getSupportedVideoSizes();

			flash_values = CameraController.getSupportedFlashModesNames();
			focus_values = CameraController.getSupportedFocusModesNames();

			Camera.Parameters params = CameraController.getCameraParameters();
			if (params != null)
			{
				flattenParamteters = params.flatten();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@TargetApi(21)
	@Override
	public void createImageReaders(ImageReader.OnImageAvailableListener imageAvailableListener)
	{
		// ImageReader for preview frames in YUV format
		mImageReaderPreviewYUV = ImageReader.newInstance(thiz.previewWidth, thiz.previewHeight,
				ImageFormat.YUV_420_888, 2);
		
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		// ImageReader for YUV still images
		mImageReaderYUV = ImageReader.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.YUV_420_888,
				2);

		// ImageReader for JPEG still images
		if (getCaptureFormat() == CameraController.RAW)
		{
			CameraController.Size imageSizeJPEG = CameraController.getMaxCameraImageSize(CameraController.JPEG);
			mImageReaderJPEG = ImageReader.newInstance(imageSizeJPEG.getWidth(), imageSizeJPEG.getHeight(),
					ImageFormat.JPEG, 2);
		} else
			mImageReaderJPEG = ImageReader
					.newInstance(imageSize.getWidth(), imageSize.getHeight(), ImageFormat.JPEG, 2);

		
		if(CameraController.isCaptureFormatSupported(CameraController.RAW))
		{
			CameraController.Size rawImageSize = CameraController.getMaxCameraImageSize(CameraController.RAW);
			// ImageReader for RAW still images
			mImageReaderRAW = ImageReader.newInstance(rawImageSize.getWidth(), rawImageSize.getHeight(), ImageFormat.RAW_SENSOR,
					3);
		}

		guiManager.setupViewfinderPreviewSize(new CameraController.Size(thiz.previewWidth, thiz.previewHeight));

		mImageReaderPreviewYUV.setOnImageAvailableListener(imageAvailableListener, null);
		mImageReaderYUV.setOnImageAvailableListener(imageAvailableListener, null);
		mImageReaderJPEG.setOnImageAvailableListener(imageAvailableListener, null);
		if(mImageReaderRAW != null)
			mImageReaderRAW.setOnImageAvailableListener(imageAvailableListener, null);
	}

	@TargetApi(19)
	@Override
	public Surface getPreviewYUVImageSurface()
	{
		if(mImageReaderPreviewYUV != null)
			return mImageReaderPreviewYUV.getSurface();
		else
		{
			return null;
		}
	}

	@TargetApi(19)
	@Override
	public Surface getYUVImageSurface()
	{
		return mImageReaderYUV.getSurface();
	}

	@TargetApi(19)
	@Override
	public Surface getJPEGImageSurface()
	{
		return mImageReaderJPEG.getSurface();
	}

	@TargetApi(19)
	@Override
	public Surface getRAWImageSurface()
	{
		return mImageReaderRAW.getSurface();
	}
	
	@Override
	public MediaRecorder getMediaRecorder()
	{
		return mMediaRecorder;
	}

	@Override
	public SimpleStreamSurfaceView getSimpleStreamSurfaceView()
	{
		return (SimpleStreamSurfaceView) preview;
	}

	@Override
	public int getImageSizeIndex()
	{
		return MainScreen.getInstance().imageSizeIdxPreference;
	}

	@Override
	public int getMultishotImageSizeIndex()
	{
		return MainScreen.getInstance().multishotImageSizeIdxPreference;
	}

	@Override
	public boolean isShutterSoundEnabled()
	{
		return shutterPreference;
	}

	@Override
	public int isShotOnTap()
	{
		return MainScreen.getInstance().shotOnTapPreference;
	}

	public static boolean isShowHelp()
	{
		return MainScreen.getInstance().showHelp;
	}

	public static void setShowHelp(boolean show)
	{
		MainScreen.getInstance().showHelp = show;
	}

	/*
	 * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ Get/Set method for private variables
	 */

	@Override
	public void onPreferenceCreate(PreferenceFragment prefActivity)
	{
		setImageSizeOptions(prefActivity, MODE_GENERAL);
		setImageSizeOptions(prefActivity, MODE_SMART_MULTISHOT_AND_NIGHT);
		setImageSizeOptions(prefActivity, MODE_PANORAMA);
		setImageSizeOptions(prefActivity, MODE_VIDEO);
	}

	private void setColorEffectOptions(PreferenceFragment prefActivity)
	{
		CharSequence[] entries = null;
		CharSequence[] entryValues = null;

		String opt1 = sRearColorEffectPref;
		String opt2 = sFrontColorEffectPref;
		
		ListPreference lp = (ListPreference) prefActivity.findPreference(opt1);
		ListPreference lp2 = (ListPreference) prefActivity.findPreference(opt2);

		int[] colorEfects = CameraController.getSupportedColorEffects();
		
		if (colorEfects == null || CameraController.ColorEffectsNamesList == null 
				|| !CameraController.isColorEffectSupported()) {
			if (lp != null) {
				prefActivity.getPreferenceScreen().removePreference(lp);
			}
			
			if (lp2 != null) {
				prefActivity.getPreferenceScreen().removePreference(lp2);
			}
			
			return;
		}
		
		entries = CameraController.ColorEffectsNamesList
				.toArray(new CharSequence[CameraController.ColorEffectsNamesList.size()]);
		entryValues = new CharSequence[colorEfects.length];
		for (int i = 0; i < colorEfects.length; i++)
		{
			entryValues[i] = Integer.toString(colorEfects[i]);
		}

		if (CameraController.isFrontCamera() && lp2 != null)
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
		}
	}

	private void setImageSizeOptions(PreferenceFragment prefActivity, int mode)
	{
		CharSequence[] entries = null;
		CharSequence[] entryValues = null;

		int idx = 0;
		int currentIdx = -1;
		String opt1 = "";
		String opt2 = "";
		String opt3 = "";

		if (mode == MODE_GENERAL)
		{
			opt1 = sImageSizeRearPref;
			opt2 = sImageSizeFrontPref;
			opt3 = sImageSizeSonyRemotePref;
			currentIdx = MainScreen.thiz.getImageSizeIndex();

			if (currentIdx == -1)
			{
				currentIdx = 0;
			}

			entries = CameraController.getResolutionsNamesList().toArray(
					new CharSequence[CameraController.getResolutionsNamesList().size()]);
			entryValues = CameraController.getResolutionsIdxesList().toArray(
					new CharSequence[CameraController.getResolutionsIdxesList().size()]);
		} else if (mode == MODE_SMART_MULTISHOT_AND_NIGHT)
		{
			opt1 = sImageSizeMultishotBackPref;
			opt2 = sImageSizeMultishotFrontPref;
			if (!CameraController.isRemoteCamera())
			{
				currentIdx = Integer.parseInt(CameraController.MultishotResolutionsIdxesList.get(selectImageDimensionMultishot()));
				entries = CameraController.MultishotResolutionsNamesList
						.toArray(new CharSequence[CameraController.MultishotResolutionsNamesList.size()]);
				entryValues = CameraController.MultishotResolutionsIdxesList
						.toArray(new CharSequence[CameraController.MultishotResolutionsIdxesList.size()]);
			}
		} else if (mode == MODE_PANORAMA)
		{
			opt1 = sImageSizePanoramaBackPref;
			opt2 = sImageSizePanoramaFrontPref;
			if (!CameraController.isRemoteCamera())
			{
				PanoramaAugmentedCapturePlugin.onDefaultSelectResolutons();
				currentIdx = PanoramaAugmentedCapturePlugin.prefResolution;
				entries = PanoramaAugmentedCapturePlugin.getResolutionsPictureNamesList().toArray(
						new CharSequence[PanoramaAugmentedCapturePlugin.getResolutionsPictureNamesList().size()]);
				entryValues = PanoramaAugmentedCapturePlugin.getResolutionsPictureIndexesList().toArray(
						new CharSequence[PanoramaAugmentedCapturePlugin.getResolutionsPictureIndexesList().size()]);
			}
		} else if (mode == MODE_VIDEO)
		{
			opt1 = sImageSizeVideoBackPref;
			opt2 = sImageSizeVideoFrontPref;

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			currentIdx = Integer.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? opt1 : opt2, "2"));

			CharSequence[] entriesTmp = new CharSequence[6];
			CharSequence[] entryValuesTmp = new CharSequence[6];
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), VideoCapturePlugin.QUALITY_4K))
			{
				entriesTmp[idx] = "4K";
				entryValuesTmp[idx] = "6";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_2160P))
			{
				entriesTmp[idx] = "2160p";
				entryValuesTmp[idx] = "2";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_1080P))
			{
				entriesTmp[idx] = "1080p";
				entryValuesTmp[idx] = "3";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_720P))
			{
				entriesTmp[idx] = "720p";
				entryValuesTmp[idx] = "4";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_480P))
			{
				entriesTmp[idx] = "480p";
				entryValuesTmp[idx] = "5";
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
			ListPreference lp3 = (ListPreference) prefActivity.findPreference(opt3);

			if (!CameraController.isRemoteCamera())
			{
				if (lp3 != null)
				{
					prefActivity.getPreferenceScreen().removePreference(lp3);
				}

				if (CameraController.getCameraIndex() == 0 && lp2 != null)
					prefActivity.getPreferenceScreen().removePreference(lp2);
				else if (lp != null && lp2 != null)
				{
					prefActivity.getPreferenceScreen().removePreference(lp);
					lp = lp2;
				}
			} else
			{
				prefActivity.getPreferenceScreen().removePreference(lp);
				prefActivity.getPreferenceScreen().removePreference(lp2);
				lp = lp3;
			}

			if (lp != null)
			{
				lp.setEntries(entries);
				lp.setEntryValues(entryValues);

				if (currentIdx != -1)
				{
					// set currently selected image size
					for (idx = 0; idx < entryValues.length; ++idx)
					{
						if (Integer.valueOf(entryValues[idx].toString()) == currentIdx)
						{
							lp.setValueIndex(idx);
							break;
						}
					}
				} else
				{
					lp.setValueIndex(0);
				}

				if (mode == MODE_GENERAL)
				{
					lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
					{
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							thiz.imageSizeIdxPreference = Integer.parseInt(newValue.toString());
							setCameraImageSizeIndex(Integer.parseInt(newValue.toString()), false);
							return true;
						}
					});
				}

				if (mode == MODE_PANORAMA)
				{
					lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
					{
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							int value = Integer.parseInt(newValue.toString());
							PanoramaAugmentedCapturePlugin.prefResolution = value;

							for (int i = 0; i < PanoramaAugmentedCapturePlugin.getResolutionsPictureIndexesList().size(); i++)
							{
								if (PanoramaAugmentedCapturePlugin.getResolutionsPictureIndexesList().get(i)
										.equals(newValue))
								{
									final int idx = i;
									final Point point = PanoramaAugmentedCapturePlugin.getResolutionsPictureSizeslist()
											.get(idx);

									// frames_fit_count may decrease when
									// returning to main view due to slightly
									// more memory used, so in text messages we
									// report both exact and decreased count to
									// the user
									final int frames_fit_count = (int) (PanoramaAugmentedCapturePlugin
											.getAmountOfMemoryToFitFrames() / PanoramaAugmentedCapturePlugin
											.getFrameSizeInBytes(point.x, point.y));

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

							return true;
						}
					});
				}
			}
		}
	}

	@Override
	public void onAdvancePreferenceCreate(PreferenceFragment prefActivity)
	{
		CheckBoxPreference cp = (CheckBoxPreference) prefActivity.findPreference(getResources().getString(
				R.string.Preference_UseCamera2Key));
		final CheckBoxPreference fp = (CheckBoxPreference) prefActivity.findPreference(MainScreen.sCaptureRAWPref);

		if (cp != null)
		{
			if (!CameraController.isCamera2Allowed())
				cp.setEnabled(false);
			else
				cp.setEnabled(true);

			cp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				public boolean onPreferenceChange(Preference preference, Object useCamera2)
				{
					PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
							.putBoolean(ApplicationScreen.sInitModeListPref, true).commit();

					boolean new_value = Boolean.parseBoolean(useCamera2.toString());
					if (new_value)
					{
						if (fp != null && CameraController.isRAWCaptureSupported())
							fp.setEnabled(true);
						else
							fp.setEnabled(false);
					} else if (fp != null)
					{
						PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
								.putBoolean(MainScreen.sCaptureRAWPref, false).commit();
						fp.setEnabled(false);
					}

					return true;
				}
			});
		}

		final PreferenceFragment mPref = prefActivity;

		if (fp != null)
		{
			fp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				public boolean onPreferenceChange(Preference preference, Object captureRAW)
				{
					boolean new_value = Boolean.parseBoolean(captureRAW.toString());
					if (new_value)
					{
						new AlertDialog.Builder(mPref.getActivity())
								.setIcon(R.drawable.gui_almalence_alert_dialog_icon)
								.setTitle(R.string.Pref_Common_CaptureRAW_Title)
								.setMessage(R.string.Pref_Common_CaptureRAW_Description)
								.setPositiveButton(android.R.string.ok, null).create().show();
					}
					return true;
				}
			});

			if (CameraController.isRAWCaptureSupported() && CameraController.isUseCamera2())
				fp.setEnabled(true);
			else
				fp.setEnabled(false);
		}
		
		//Real exposure preference should be available only in Camera2 mode
		CheckBoxPreference realExposurePf = (CheckBoxPreference) prefActivity.findPreference(MainScreen.sRealExposureTimeOnPreviewPref);
		if(realExposurePf != null)
		{
			boolean isCamera2 = PreferenceManager.getDefaultSharedPreferences(
					MainScreen.getMainContext()).getBoolean(getResources().getString(R.string.Preference_UseCamera2Key), false);
			if (!isCamera2)
			{
				realExposurePf.setEnabled(false);
			}
			else
				realExposurePf.setEnabled(true);
		}

		setColorEffectOptions(prefActivity);
	}


	@TargetApi(21)
	protected void stopImageReaders()
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
		if (mImageReaderRAW != null)
		{
			mImageReaderRAW.close();
			mImageReaderRAW = null;
		}
	}

	@Override
	protected void stopRemotePreview()
	{
		if (preview != null && SimpleStreamSurfaceView.class.isInstance(preview))
		{
			((SimpleStreamSurfaceView) preview).stop();
		}
	}



	public void setCameraImageSizeIndex(int captureIndex, boolean init)
	{
		CameraController.setCameraImageSizeIndex(captureIndex);
		if (init)
		{
			if (!CameraController.isRemoteCamera())
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
				prefs.edit()
						.putString(
								CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeRearPref
										: MainScreen.sImageSizeFrontPref, String.valueOf(captureIndex)).commit();
			} else
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
				prefs.edit().putString(MainScreen.sImageSizeSonyRemotePref, String.valueOf(captureIndex)).commit();
			}
		}
	}

	@Override
	public void setSpecialImageSizeIndexPref(int iIndex)
	{
		SharedPreferences.Editor prefEditor = PreferenceManager.getDefaultSharedPreferences(mainContext).edit();
		prefEditor.putString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeMultishotBackPref
																	: MainScreen.sImageSizeMultishotFrontPref, String.valueOf(iIndex));
		prefEditor.commit();
	}

	@Override
	public String getSpecialImageSizeIndexPref()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
		return prefs.getString(CameraController.getCameraIndex() == 0 ? MainScreen.sImageSizeMultishotBackPref
																	  : MainScreen.sImageSizeMultishotFrontPref, "-1");
	}

	@Override
	public int selectImageDimensionMultishot()
	{
		String modeName = PluginManager.getInstance().getActiveModeID();
		if (CameraController.isUseCamera2() && modeName.contains("night"))
		{
			return 0;
		}

		long maxMem = Runtime.getRuntime().maxMemory() - Debug.getNativeHeapAllocatedSize();
		long maxMpix = (maxMem - 1000000) / 3; // 2 x Mpix - result, 1/4 x Mpix
												// x 4 - compressed input jpegs,
												// 1Mb - safe reserve

		// find index selected in preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int prefIdx = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? sImageSizeMultishotBackPref : sImageSizeMultishotFrontPref,
				"-1"));

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

			if ((Integer.valueOf(CameraController.MultishotResolutionsIdxesList.get(ii)) == prefIdx)
					&& (mpix >= minMPIX))
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

	@Override
	public void addSurfaceCallback()
	{
		thiz.surfaceHolder.addCallback(thiz);
	}

	@Override
	public void configureCamera(boolean createGUI)
	{
		switchingMode = false;

		CameraController.updateCameraFeatures();

		// ----- Select preview dimensions with ratio correspondent to
		// full-size image
		PluginManager.getInstance().setCameraPreviewSize();
		// prepare list of surfaces to be used in capture requests
		if (!CameraController.isRemoteCamera())
		{
			if (CameraController.isUseCamera2())
				configureCamera2Camera(captureFormat);
			else
			{
				Camera.Size sz = CameraController.getCameraParameters().getPreviewSize();

				Log.e("MainScreen", "Viewfinder preview size: " + sz.width + "x" + sz.height);
				guiManager.setupViewfinderPreviewSize(new CameraController.Size(sz.width, sz.height));
				double bufferSize = sz.width * sz.height
						* ImageFormat.getBitsPerPixel(CameraController.getCameraParameters().getPreviewFormat()) / 8.0d;
				CameraController.allocatePreviewBuffer(bufferSize);

				CameraController.getCamera().setErrorCallback(CameraController.getInstance());

				onCameraConfigured();
			}
		} else
		{
			guiManager.setupViewfinderPreviewSize(new CameraController.Size(((SimpleStreamSurfaceView) preview)
					.getSurfaceWidth(), ((SimpleStreamSurfaceView) preview).getSurfaceHeight()));
			onCameraConfigured();
		}

		Log.e("MainScreen", "createGUI is " + createGUI);
		if (createGUI)
		{
			MainScreen.getGUIManager().onGUICreate();
			PluginManager.getInstance().onGUICreate();
		}
	}

	@TargetApi(21)
	@Override
	public void createCaptureSession()
	{
//		CameraController.setupImageReadersCamera2();
		mCameraSurface = surfaceHolder.getSurface();
		surfaceList.add(mCameraSurface); // surface for viewfinder preview

		String modeId = PluginManager.getInstance().getActiveModeID();
		if (modeId.contains("video"))
		{
			surfaceList.add(mMediaRecorder.getSurface()); // surface for MediaRecorder
		}
		
		// if (captureFormat != CameraController.RAW) // when capture RAW
		// preview frames is not available
		if(mImageReaderPreviewYUV != null)
			surfaceList.add(mImageReaderPreviewYUV.getSurface()); // surface for
																	// preview yuv
		// images
		if (captureFormat == CameraController.YUV && mImageReaderYUV != null)
		{
			Log.d("MainScreen",
					"add mImageReaderYUV " + mImageReaderYUV.getWidth() + " x " + mImageReaderYUV.getHeight());
			surfaceList.add(mImageReaderYUV.getSurface());
			
			//Temporary disable RAW capturing on Galaxy S6 in all modes, including Super mode
//			String modeName = PluginManager.getInstance().getActiveModeID();
//			if (CameraController.isGalaxyS6 && modeName.contains("nightmode"))
//				surfaceList.add(mImageReaderRAW.getSurface());
//			else
//			{
//				Log.d("MainScreen",
//						"add mImageReaderYUV " + mImageReaderYUV.getWidth() + " x " + mImageReaderYUV.getHeight());
//				surfaceList.add(mImageReaderYUV.getSurface());
//			}
			// capture
		} else if (captureFormat == CameraController.JPEG && mImageReaderJPEG != null)
		{
			Log.d("MainScreen",
					"add mImageReaderJPEG " + mImageReaderJPEG.getWidth() + " x " + mImageReaderJPEG.getHeight());
			surfaceList.add(mImageReaderJPEG.getSurface()); // surface for jpeg
															// image
			// capture
		} else if (captureFormat == CameraController.RAW && mImageReaderJPEG != null && mImageReaderRAW != null)
		{
			Log.d("MainScreen", "add mImageReaderRAW + mImageReaderJPEG " + mImageReaderRAW.getWidth() + " x "
					+ mImageReaderRAW.getHeight());
			surfaceList.add(mImageReaderJPEG.getSurface()); // surface for jpeg
															// image
			// capture
			if (CameraController.isRAWCaptureSupported())
				surfaceList.add(mImageReaderRAW.getSurface());
			else
				captureFormat = CameraController.JPEG;
		}

		CameraController.setPreviewSurface(mImageReaderPreviewYUV.getSurface());

		CameraController.setCaptureFormat(captureFormat);
		// configure camera with all the surfaces to be ever used

		// If camera device isn't initialized (equals null) just force stop
		// application.
		if (!CameraController.createCaptureSession(surfaceList))
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_APPLICATION_STOP, 0);
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder)
	{
		Log.e("MainScreen", "SURFACE CREATED");
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
	}

	// Probably used only by Panorama plugin. Added to avoid non direct
	// interface (message/handler)
	public static void takePicture()
	{
		PluginManager.getInstance().takePicture();
	}

	@Override
	public void captureFailed()
	{
		MainScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION);
		MainScreen.getInstance().muteShutter(false);
	}

	@TargetApi(14)
	public boolean isFaceDetectionAvailable(Camera.Parameters params)
	{
		return params.getMaxNumDetectedFaces() > 0;
	}

	public CameraController.Size getPreviewSize()
	{
		if (SimpleStreamSurfaceView.class.isInstance(preview))
		{
			return new CameraController.Size(((SimpleStreamSurfaceView) preview).getSurfaceWidth(),
					((SimpleStreamSurfaceView) preview).getSurfaceHeight());
		} else
		{
			LayoutParams lp = preview.getLayoutParams();
			if (lp == null)
				return null;

			return new CameraController.Size(lp.width, lp.height);
		}
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
			CameraController.setCameraMeteringAreas(null);
		else if (CameraParameters.meteringModeMatrix == mode)
		{
			int maxAreasCount = CameraController.getMaxNumMeteringAreas();
			if (maxAreasCount > 4)
				CameraController.setCameraMeteringAreas(mMeteringAreaMatrix5);
			else if (maxAreasCount > 3)
				CameraController.setCameraMeteringAreas(mMeteringAreaMatrix4);
			else if (maxAreasCount > 0)
				CameraController.setCameraMeteringAreas(mMeteringAreaMatrix1);
			else
				CameraController.setCameraMeteringAreas(null);
		} else if (CameraParameters.meteringModeCenter == mode)
			CameraController.setCameraMeteringAreas(mMeteringAreaCenter);
		else if (CameraParameters.meteringModeSpot == mode)
			CameraController.setCameraMeteringAreas(mMeteringAreaSpot);

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

	@Override
	public void setAutoFocusLock(boolean locked)
	{
		mAFLocked = locked;
	}

	@Override
	public boolean getAutoFocusLock()
	{
		return mAFLocked;
	}

	@Override
	public boolean onKeyUpEvent(int keyCode, KeyEvent event)
	{
		// Prevent system sounds, for volume buttons.
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP)
		{
			return true;
		}

		return false;
	}

	@Override
	public boolean onKeyDownEvent(int keyCode, KeyEvent event)
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
			if (event.getDownTime() == event.getEventTime())
			{
				MainScreen.getGUIManager().onHardwareFocusButtonPressed();
			}
			return true;
		}

		// check if Headset Hook button has some functions except standard
		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			boolean headsetFunc = prefs.getBoolean("headsetPrefCommon", false);
			if (headsetFunc)
			{
				//removed as not needed??? SM 21.08.15 was focusing on HW button pressed when AFL was enabled
				//MainScreen.getGUIManager().onHardwareFocusButtonPressed();
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
				//removed as not needed??? SM 21.08.15 was focusing on HW button pressed when AFL was enabled
				//MainScreen.getGUIManager().onHardwareFocusButtonPressed();
				MainScreen.getGUIManager().onHardwareShutterButtonPressed();
				return true;
			} else if (buttonFunc == VOLUME_FUNC_EXPO)
			{
				MainScreen.getGUIManager().onVolumeBtnExpo(keyCode);
				return true;
			} else if (buttonFunc == VOLUME_FUNC_NONE)
				return true;
		}

		// <!-- -+-
		if (((RelativeLayout) guiManager.getMainView().findViewById(R.id.viewPagerLayoutMain)).getVisibility() == View.VISIBLE)
		{
			guiManager.hideStore();
			return true;
		}
		// -+- -->

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

		return false;
	}

	public void menuButtonPressed()
	{
		PluginManager.getInstance().menuButtonPressed();
	}

	public void disableCameraParameter(GUI.CameraParameter iParam, boolean bDisable, boolean bInitMenu,
			boolean bModeInit)
	{
		guiManager.disableCameraParameter(iParam, bDisable, bInitMenu, bModeInit);
	}

	public void showOpenGLLayer(final int version)
	{
		if (glView == null)
		{
			glView = new GLLayer(MainScreen.getMainContext(), version);
			LayoutParams params = MainScreen.getPreviewSurfaceView().getLayoutParams();
			glView.setLayoutParams(params);
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
			glView.onPause();
			glView.destroyDrawingCache();
			((RelativeLayout) this.findViewById(R.id.mainLayout2)).removeView(glView);
			glView = null;
		}
	}

	@Override
	public void showCaptureIndication(boolean playShutter)
	{
		// play tick sound
		MainScreen.getGUIManager().showCaptureIndication();
		if (playShutter)
			MainScreen.playShutter();
	}

	public void playShutter(int sound)
	{
		if (!MainScreen.getInstance().isShutterSoundEnabled())
		{
			MediaPlayer mediaPlayer = MediaPlayer.create(MainScreen.thiz, sound);
			mediaPlayer.start();
		}
	}

	public static void playShutter()
	{
		if (!MainScreen.getInstance().isShutterSoundEnabled())
		{
			if (thiz.shutterPlayer != null)
				thiz.shutterPlayer.play();
		}
	}

	// set TRUE to mute and FALSE to unmute
	public void muteShutter(boolean mute)
	{
		if (MainScreen.getInstance().isShutterSoundEnabled())
		{
			AudioManager mgr = (AudioManager) MainScreen.thiz.getSystemService(MainScreen.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, mute);
		}
	}
	@Override
	public void setExpoPreviewPref(boolean previewMode)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
		Editor prefsEditor = prefs.edit();
		prefsEditor.putBoolean(MainScreen.sExpoPreviewModePref, previewMode);
		prefsEditor.commit();
	}

	@Override
	public boolean getExpoPreviewPref()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
		if (true == prefs.contains(MainScreen.sExpoPreviewModePref))
		{
			return prefs.getBoolean(MainScreen.sExpoPreviewModePref, true);
		} else
			return true;
	}

	public void setLastPhotoModePref(String mode)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
		Editor prefsEditor = prefs.edit();
		prefsEditor.putString(MainScreen.sLastPhotoModePref, mode);
		prefsEditor.commit();
	}

	public String getLastPhotoModePref()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
		if (true == prefs.contains(MainScreen.sLastPhotoModePref))
		{
			return prefs.getString(MainScreen.sLastPhotoModePref, "single");
		} else
			return "single";
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

	public void setKeepScreenOn(boolean keepScreenOn)
	{
		if (keepScreenOn)
		{
			preview.setKeepScreenOn(keepScreenOn);
		} else
		{
			preview.setKeepScreenOn(this.keepScreenOn);
		}
	}

	
	private static boolean			showStore					= false;
	
	public static Resources getAppResources()
	{
		return MainScreen.thiz.getResources();
	}

	public static void setShowStore(boolean show)
	{
		showStore = show;
	}

	public static boolean isShowStore()
	{
		return showStore;
	}
	
	/*******************************************************/
	/************************ Billing ************************/
	// <!-- -+-
	protected static OpenIabHelper	mHelper;

	private static boolean			bOnSale						= false;
	private static boolean			couponSale					= false;

	private static boolean			unlockAllPurchased			= false;
	private static boolean			superPurchased				= false;
	private static boolean			hdrPurchased				= false;
	private static boolean			panoramaPurchased			= false;
	private static boolean			objectRemovalBurstPurchased	= false;
	private static boolean			groupShotPurchased			= false;

	private static boolean			unlockAllSubscriptionMonth	= false;
	private static boolean			unlockAllSubscriptionYear	= false;

	static final String				SKU_SUPER					= "plugin_almalence_super";
	static final String				SKU_HDR						= "plugin_almalence_hdr";
	static final String				SKU_PANORAMA				= "plugin_almalence_panorama";
	static final String				SKU_UNLOCK_ALL				= "unlock_all_forever";

	// barcode coupon
	static final String				SKU_UNLOCK_ALL_COUPON		= "unlock_all_forever_coupon";

	// multishot currently
	static final String				SKU_MOVING_SEQ				= "plugin_almalence_moving_burst";

	// unused. but if someone payed - will be unlocked multishot
	static final String				SKU_GROUPSHOT				= "plugin_almalence_groupshot";
	// subscription
	static final String				SKU_SUBSCRIPTION_YEAR		= "subscription_unlock_all_year";
	static final String				SKU_SUBSCRIPTION_YEAR_NEW	= "subscription_unlock_all_year_3free";
	static final String				SKU_SUBSCRIPTION_YEAR_CTRL	= "subscription_unlock_all_year_controller";

	static final String				SKU_SALE1					= "abc_sale_controller1";
	static final String				SKU_SALE2					= "abc_sale_controller2";

	static final String				SKU_PROMO					= "abc_promo";

	static
	{
		// Yandex store
		OpenIabHelper.mapSku(SKU_SUPER, "com.yandex.store", "plugin_almalence_super");
		OpenIabHelper.mapSku(SKU_HDR, "com.yandex.store", "plugin_almalence_hdr");
		OpenIabHelper.mapSku(SKU_PANORAMA, "com.yandex.store", "plugin_almalence_panorama");
		OpenIabHelper.mapSku(SKU_UNLOCK_ALL, "com.yandex.store", "unlock_all_forever");
		OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON, "com.yandex.store", "unlock_all_forever_coupon");
		OpenIabHelper.mapSku(SKU_MOVING_SEQ, "com.yandex.store", "plugin_almalence_moving_burst");
		OpenIabHelper.mapSku(SKU_GROUPSHOT, "com.yandex.store", "plugin_almalence_groupshot");
		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR, "com.yandex.store", "subscription_unlock_all_year");
		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR_NEW, "com.yandex.store", "subscription_unlock_all_year_3free");
		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR_CTRL, "com.yandex.store", "subscription_unlock_all_year_controller");

		OpenIabHelper.mapSku(SKU_SALE1, "com.yandex.store", "abc_sale_controller1");
		OpenIabHelper.mapSku(SKU_SALE2, "com.yandex.store", "abc_sale_controller2");
		OpenIabHelper.mapSku(SKU_PROMO, "com.yandex.store", "abc_promo");

		// Amazon store
		OpenIabHelper.mapSku(SKU_SUPER, OpenIabHelper.NAME_AMAZON, "plugin_almalence_super_amazon");
		OpenIabHelper.mapSku(SKU_HDR, OpenIabHelper.NAME_AMAZON, "plugin_almalence_hdr_amazon");
		OpenIabHelper.mapSku(SKU_PANORAMA, OpenIabHelper.NAME_AMAZON, "plugin_almalence_panorama_amazon");
		OpenIabHelper.mapSku(SKU_UNLOCK_ALL, OpenIabHelper.NAME_AMAZON, "unlock_all_forever_amazon");
		OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON, OpenIabHelper.NAME_AMAZON, "unlock_all_forever_coupon_amazon");
		OpenIabHelper.mapSku(SKU_MOVING_SEQ, OpenIabHelper.NAME_AMAZON, "plugin_almalence_moving_burst_amazon");
		OpenIabHelper.mapSku(SKU_GROUPSHOT, OpenIabHelper.NAME_AMAZON, "plugin_almalence_groupshot_amazon");
		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR, OpenIabHelper.NAME_AMAZON, "subscription_unlock_all_year");
		OpenIabHelper
				.mapSku(SKU_SUBSCRIPTION_YEAR_NEW, OpenIabHelper.NAME_AMAZON, "subscription_unlock_all_year_3free");
		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR_CTRL, OpenIabHelper.NAME_AMAZON,
				"subscription_unlock_all_year_controller");

		OpenIabHelper.mapSku(SKU_SALE1, OpenIabHelper.NAME_AMAZON, "abc_sale_controller1_amazon");
		OpenIabHelper.mapSku(SKU_SALE2, OpenIabHelper.NAME_AMAZON, "abc_sale_controller2_amazon");
		OpenIabHelper.mapSku(SKU_PROMO, OpenIabHelper.NAME_AMAZON, "abc_promo_amazon");

		// Samsung store
		// OpenIabHelper.mapSku(SKU_SUPER, OpenIabHelper.NAME_SAMSUNG,
		// "100000103369/000001018387");
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
	private static boolean	subscriptionStatusRequest	= false;
	private static long		timeLastSubscriptionCheck	= 0;// should check each 32 days - 32*24*60*60*1000
	private long			days32						= 32 * 24 * 60 * 60 * 1000L;

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
			Map<String, String> storeKeys = new HashMap<String, String>();
			storeKeys.put(OpenIabHelper.NAME_GOOGLE, base64EncodedPublicKeyGoogle);
			storeKeys.put("com.yandex.store", base64EncodedPublicKeyYandex);

			OpenIabHelper.Options.Builder builder = new OpenIabHelper.Options.Builder()
					.setStoreSearchStrategy(OpenIabHelper.Options.SEARCH_STRATEGY_INSTALLER_THEN_BEST_FIT)
					.setVerifyMode(OpenIabHelper.Options.VERIFY_EVERYTHING).addStoreKeys(storeKeys);

			mHelper = new OpenIabHelper(this, builder.build());

			OpenIabHelper.enableDebugLogging(true);

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
						additionalSkuList.add(SKU_SUPER);
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
							// subscription year
							additionalSkuList.add(SKU_SUBSCRIPTION_YEAR);
							additionalSkuList.add(SKU_SUBSCRIPTION_YEAR_NEW);
							// reset subscription status
							unlockAllSubscriptionYear = false;
							prefs.edit().putBoolean("subscription_unlock_all_year", false).commit();

							timeLastSubscriptionCheck = System.currentTimeMillis();
							prefs.edit().putLong("timeLastSubscriptionCheck", timeLastSubscriptionCheck).commit();
						}

						// for sale
						additionalSkuList.add(SKU_SALE1);
						additionalSkuList.add(SKU_SALE2);

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

	public static String						titleUnlockAll				= "$6.95";
	public static String						titleUnlockAllCoupon		= "$3.95";
	public static String						titleUnlockHDR				= "$2.99";
	public static String						titleUnlockSuper			= "$2.99";
	public static String						titleUnlockPano				= "$2.99";
	public static String						titleUnlockMoving			= "$3.99";
	public static String						titleUnlockGroup			= "$2.99";
	public static String						titleSubscriptionYear		= "$4.99";

	public static String						summary_SKU_PROMO			= "alyrom0nap";
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
					if (inventory
							.hasPurchase(SKU_SUPER))
					{
						superPurchased = true;
						prefsEditor
								.putBoolean(
										"plugin_almalence_super",
										true).commit();
					}
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
										"plugin_almalence_moving_burst",
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
						unlockAllPurchased = true;
						prefsEditor.putBoolean(
								"unlock_all_forever",
								true).commit();
					}
					if (inventory
							.hasPurchase(SKU_SUBSCRIPTION_YEAR_NEW))
					{
						unlockAllSubscriptionYear = true;
						prefsEditor
								.putBoolean(
										"subscription_unlock_all_year",
										true).commit();
						unlockAllPurchased = true;
						prefsEditor.putBoolean(
								"unlock_all_forever",
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
						titleUnlockSuper = inventory
								.getSkuDetails(
										SKU_SUPER)
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

						titleSubscriptionYear = inventory
								.getSkuDetails(
										SKU_SUBSCRIPTION_YEAR_CTRL)
								.getPrice();

						summary_SKU_PROMO = inventory
								.getSkuDetails(
										SKU_PROMO)
								.getDescription();
					} catch (Exception e)
					{
						Log.e("Market",
								"Error Getting data for store!!!!!!!!");
					}
				}
			};

	private static int							HDR_REQUEST					= 100;
	private static int							SUPER_REQUEST				= 107;
	private static int							PANORAMA_REQUEST			= 101;
	private static int							ALL_REQUEST					= 102;
	private static int							OBJECTREM_BURST_REQUEST		= 103;
	private static int							GROUPSHOT_REQUEST			= 104;
	private static int							SUBSCRIPTION_YEAR_REQUEST	= 106;

	public static boolean isPurchasedAll()
	{
		return unlockAllPurchased;
	}

	public static boolean isPurchasedSuper()
	{
		return superPurchased;
	}

	public static boolean isPurchasedHDR()
	{
		return hdrPurchased;
	}

	public static boolean isPurchasedPanorama()
	{
		return panoramaPurchased;
	}

	public static boolean isPurchasedMoving()
	{
		return objectRemovalBurstPurchased;
	}

	public static boolean isPurchasedGroupshot()
	{
		return groupShotPurchased;
	}

	public static boolean isPurchasedUnlockAllSubscriptionMonth()
	{
		return unlockAllSubscriptionMonth;
	}

	public static boolean isPurchasedUnlockAllSubscriptionYear()
	{
		return unlockAllSubscriptionYear;
	}

	public static void purchaseAll()
	{
		if (isPurchasedAll())
			return;

		// now will call store with abc unlocked
		callStoreForUnlocked(thiz);

		// TODO: this is for all other markets!!!!! Do not call store!!!
		// String payload = "";
		// try
		// {
		// mHelper.launchPurchaseFlow(MainScreen.thiz,
		// isCouponSale()?SKU_UNLOCK_ALL_COUPON:SKU_UNLOCK_ALL, ALL_REQUEST,
		// mPreferencePurchaseFinishedListener, payload);
		// }
		// catch (Exception e) {
		// e.printStackTrace();
		// Log.e("Main billing", "Purchase result " + e.getMessage());
		// Toast.makeText(MainScreen.thiz,
		// "Error during purchase " + e.getMessage(),
		// Toast.LENGTH_LONG).show();
		// }
	}

	public void purchaseSuper()
	{
		if (isPurchasedSuper() || isPurchasedAll())
			return;
		String payload = "";
		try
		{
			mHelper.launchPurchaseFlow(MainScreen.thiz, SKU_SUPER, SUPER_REQUEST, mPreferencePurchaseFinishedListener,
					payload);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
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

	public static void purchasedUnlockAllSubscriptionYear()
	{
		if (isPurchasedUnlockAllSubscriptionYear() || isPurchasedAll())
			return;
		String payload = "";
		try
		{
			mHelper.launchPurchaseFlow(MainScreen.thiz, SKU_SUBSCRIPTION_YEAR_NEW, SUBSCRIPTION_YEAR_REQUEST,
					mPreferencePurchaseFinishedListener, payload);
		} catch (Exception e)
		{
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
		}
	}

	// Callback for when purchase from preferences is finished
	protected static IabHelper.OnIabPurchaseFinishedListener	mPreferencePurchaseFinishedListener	= new IabHelper.OnIabPurchaseFinishedListener()
																									{
																										public void onIabPurchaseFinished(
																												IabResult result,
																												Purchase purchase)
																										{
																											showStore = true;
																											purchaseFinished(
																													result,
																													purchase);
																										}
																									};

	private static void purchaseFinished(IabResult result, Purchase purchase)
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
		if (purchase.getSku().equals(SKU_SUPER))
		{
			Log.v("Main billing", "Purchase SUPER.");
			superPurchased = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("plugin_almalence_super", true).commit();
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
			Log.v("Main billing", "Purchase plugin_almalence_moving_burst.");
			objectRemovalBurstPurchased = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("plugin_almalence_moving_burst", true).commit();
		}
		if (purchase.getSku().equals(SKU_SUBSCRIPTION_YEAR))
		{
			Log.v("Main billing", "Purchase year subscription.");
			unlockAllSubscriptionYear = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("subscription_unlock_all_year", true).commit();

			timeLastSubscriptionCheck = System.currentTimeMillis();
			prefs.edit().putLong("timeLastSubscriptionCheck", timeLastSubscriptionCheck).commit();

			unlockAllPurchased = true;
			prefsEditor.putBoolean("unlock_all_forever", true).commit();
		}
		if (purchase.getSku().equals(SKU_SUBSCRIPTION_YEAR_NEW))
		{
			Log.v("Main billing", "Purchase year subscription.");
			unlockAllSubscriptionYear = true;

			Editor prefsEditor = prefs.edit();
			prefsEditor.putBoolean("subscription_unlock_all_year", true).commit();

			timeLastSubscriptionCheck = System.currentTimeMillis();
			prefs.edit().putLong("timeLastSubscriptionCheck", timeLastSubscriptionCheck).commit();

			unlockAllPurchased = true;
			prefsEditor.putBoolean("unlock_all_forever", true).commit();
		}
	}

	public static void launchPurchase(int requestID)
	{
		try
		{
			thiz.guiManager.showStore();
		} catch (Exception e)
		{
			e.printStackTrace();
			Toast.makeText(thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
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

	public boolean	showPromoRedeemed		= false;
	public boolean	showPromoRedeemedJulius	= false;

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
				String[] sep = MainScreen.summary_SKU_PROMO.split(";");
				String promo = editText.getText().toString();
				boolean matchPromo = false;

				// /////////////////////////////////////////////////////
				// juliusapp promotion
				if (promo.equalsIgnoreCase("promo2015"))
				{
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
					panoramaPurchased = true;
					objectRemovalBurstPurchased = true;

					Editor prefsEditor = prefs.edit();
					prefsEditor.putBoolean("plugin_almalence_panorama", true);
					prefsEditor.putBoolean("plugin_almalence_moving_burst", true);
					prefsEditor.commit();
					dialog.dismiss();
					guiManager.hideStore();
					showPromoRedeemedJulius = true;
					guiManager.showStore();
					return;
				}
				// /////////////////////////////////////////////////////

				for (int i = 0; i < sep.length; i++)
				{
					if (promo.equalsIgnoreCase(sep[i]))
						matchPromo = true;
				}

				if (matchPromo)
				{
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
					unlockAllPurchased = true;

					Editor prefsEditor = prefs.edit();
					prefsEditor.putBoolean("unlock_all_forever", true).commit();
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
	public static int getLeftLaunches(String modeID)
	{
		String dirPath = thiz.getFilesDir().getAbsolutePath() + File.separator + modeID;
		File projDir = new File(dirPath);
		if (!projDir.exists())
		{
			projDir.mkdirs();
			WriteLaunches(projDir, 6);
		}
		int left = ReadLaunches(projDir);
		return left;
	}

	// decrements number of launches left
	public static void decrementLeftLaunches(String modeID)
	{
		String dirPath = thiz.getFilesDir().getAbsolutePath() + File.separator + modeID;
		File projDir = new File(dirPath);
		if (!projDir.exists())
		{
			projDir.mkdirs();
			WriteLaunches(projDir, 6);
		}

		int left = ReadLaunches(projDir);
		if (left > 0)
			WriteLaunches(projDir, left - 1);
	}

	// writes number of launches left into memory
	private static void WriteLaunches(File projDir, int left)
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
	private static int ReadLaunches(File projDir)
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

	public static boolean checkLaunches(Mode mode)
	{
		// if all unlocked
		if (unlockAllPurchased)
			return true;

		// if mode free
		if (mode.SKU == null)
			return true;
		if (mode.SKU.isEmpty())
		{
			int launchesLeft = MainScreen.getLeftLaunches(mode.modeID);

			if ((1 == launchesLeft) || (3 == launchesLeft))
			{
				// show internal store
				launchPurchase(100);
			}
			return true;
		}

		// if current mode unlocked
		if (mode.SKU.equals("plugin_almalence_super"))
		{
			if (superPurchased || !CameraController.isUseSuperMode())
				return true;
		}
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

		int launchesLeft = MainScreen.getLeftLaunches(mode.modeID);
		int id = MainScreen.getAppResources().getIdentifier(
				(CameraController.isUseCamera2() ? mode.modeNameHAL : mode.modeName), "string",
				MainScreen.thiz.getPackageName());
		String modename = MainScreen.getAppResources().getString(id);

		if (0 == launchesLeft)// no more launches left
		{
			String left = String.format(thiz.getResources().getString(R.string.trial_finished), modename);
			Toast toast = Toast.makeText(thiz, left, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

			// show google store with paid version
			callStoreForUnlocked(MainScreen.thiz);

			return false;
		} else if (5 >= launchesLeft)
		{
			// show appstore button and say that it cost money
			String left = String.format(thiz.getResources().getString(R.string.trial_left), modename, launchesLeft);
			Toast toast = Toast.makeText(thiz, left, Toast.LENGTH_LONG);
			toast.setGravity(Gravity.CENTER, 0, 0);
			toast.show();

			if ((1 == launchesLeft) || (2 == launchesLeft) || (3 == launchesLeft))
				// show internal store
				launchPurchase(100);
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

	private static void showSubscriptionDialog()
	{
		final float density = thiz.getResources().getDisplayMetrics().density;

		LinearLayout ll = new LinearLayout(thiz);
		ll.setOrientation(LinearLayout.VERTICAL);
		ll.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));

		ImageView img = new ImageView(thiz);
		img.setImageResource(R.drawable.store_subscription);
		img.setAdjustViewBounds(true);
		ll.addView(img);

		TextView tv = new TextView(thiz);
		tv.setText(MainScreen.getAppResources().getString(R.string.subscriptionText));
		tv.setWidth((int) (250 * density));
		tv.setPadding((int) (4 * density), 0, (int) (4 * density), (int) (24 * density));
		ll.addView(tv);

		Button bNo = new Button(thiz);
		bNo.setText(MainScreen.getAppResources().getString(R.string.subscriptionNoText));
		ll.addView(bNo);

		Button bSubscribe = new Button(thiz);
		bSubscribe.setText(MainScreen.getAppResources().getString(R.string.subscriptionYesText));
		ll.addView(bSubscribe);

		final AlertDialog.Builder builder = new AlertDialog.Builder(thiz);
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

	private static void callStoreForUnlocked(Activity activity)
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

	public static boolean isAppturboUnlockable(Context context) {
//    	try
//    	{
//	        List<PackageInfo> packages = context.getPackageManager().getInstalledPackages(0); 
//	        for(PackageInfo pi : packages){ 
//	            if (pi.packageName.equalsIgnoreCase("com.appturbo.appturboCA2015") 
//	                    || pi.packageName.equalsIgnoreCase("com.appturbo.appoftheday2015") ){ 
//	                return true; 
//	            } 
//	        } 
//    	}
//    	catch (Exception e)
//    	{
//    		e.printStackTrace();
//    	}
    	return false;
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

	protected void resetOrSaveSettings()
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
			prefsEditor.putString(sImageSizeRearPref, "-1");
			prefsEditor.putString(sImageSizeFrontPref, "-1");

			// multishot and night
			prefsEditor.putString(sImageSizeMultishotBackPref, "-1");
			prefsEditor.putString(sImageSizeMultishotFrontPref, "-1");

			// panorama
			prefsEditor.remove(sImageSizePanoramaBackPref);
			prefsEditor.remove(sImageSizePanoramaFrontPref);

			// video
			prefsEditor.putString(sImageSizeVideoBackPref, "-1");
			prefsEditor.putString(sImageSizeVideoFrontPref, "-1");

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
			prefsEditor.putInt(sRearFocusModePref, sDefaultFocusValue);
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
			prefsEditor.putInt(sISOPref, sDefaultISOValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_FlashMode", true);
		if (!isSaving)
		{
			prefsEditor.putInt(sFlashModePref, sDefaultFlashValue);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_FrontRearCamera", true);
		if (!isSaving)
		{
			prefsEditor.putInt(sCameraModePref, 0);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_ExpoCompensation", false);
		if (!isSaving)
		{
			prefsEditor.putInt(MainScreen.sEvPref, 0);
			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_DelayedCapture", false);
		if (!isSaving)
		{
			prefsEditor.putInt(MainScreen.sDelayedCapturePref, 0);
			prefsEditor.putBoolean(MainScreen.sSWCheckedPref, false);
			prefsEditor.putBoolean(MainScreen.sDelayedFlashPref, false);
			prefsEditor.putBoolean(MainScreen.sDelayedSoundPref, false);
			prefsEditor.putInt(MainScreen.sDelayedCaptureIntervalPref, 0);

			prefsEditor.commit();
		}

		isSaving = prefs.getBoolean("SaveConfiguration_TimelapseCapture", false);
		if (!isSaving && !prefs.getBoolean(sPhotoTimeLapseIsRunningPref, false))
		{
			prefsEditor.putInt(MainScreen.sPhotoTimeLapseCaptureIntervalPref, 5);
			prefsEditor.putInt(MainScreen.sPhotoTimeLapseCaptureIntervalMeasurmentPref, 0);
			prefsEditor.putBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);
			prefsEditor.putBoolean(MainScreen.sPhotoTimeLapseActivePref, false);

			prefsEditor.commit();
		}
	}

	@Override
	public Activity getMainActivity()
	{
		return thiz;
	}

	public WifiHandler getWifiHandler() {
		return mWifiHandler;
	}
}
