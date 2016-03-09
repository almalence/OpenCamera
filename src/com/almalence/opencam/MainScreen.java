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

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Point;
import android.hardware.Camera;
import android.media.AudioManager;
import android.media.CamcorderProfile;
import android.media.MediaPlayer;
import android.net.Uri;
import android.nfc.NfcAdapter;
import android.os.CountDownTimer;
import android.os.Debug;
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
import com.almalence.util.Util;

//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.AlmalenceGUI;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;
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

	// Common preferences
	private int					imageSizeIdxPreference;
	private int					multishotImageSizeIdxPreference;
	private boolean				shutterPreference				= true;
	private int					shotOnTapPreference				= 0;

	private boolean				showHelp						= false;

	private boolean				keepScreenOn					= true;

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

	public static String				sKeepScreenOn;

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

		sKeepScreenOn = getResources().getString(R.string.Preference_KeepScreenOnValue);
		
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
		// -+- -->

		try
		{
			cameraController = CameraController.getInstance();
		} catch (VerifyError exp)
		{
			Log.e("MainScreen", exp.getMessage());
		}

		keepScreenOn = prefs.getBoolean(sKeepScreenOn, true);

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
				CameraController.checkHardwareLevel());
//						(CameraController.isMotoXPure || CameraController.isNexus5or6 || CameraController.isFlex2 || CameraController.isAndroidOne || CameraController.isGalaxyS6 || CameraController.isOnePlusTwo/*|| CameraController.isG4*/) ? true : false);
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        
		isCameraConfiguring = false;

		mWifiHandler.register();
		if (mNfcAdapter != null) {
			mNfcAdapter.enableForegroundDispatch(this, NFCHandler.getPendingIntent(this),
					NFCHandler.getIntentFilterArray(), NFCHandler.getTechListArray());
		}

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

		if (CameraController.isRemoteCamera())
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

		keepScreenOn = prefs.getBoolean(sKeepScreenOn, true);
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
		if (!cameraPermissionGranted || !storagePermissionGranted)
			return;
		
		mCameraSurface = holder.getSurface();

		//In camera2 mode we have to wait a second call of surfaceChanged to continue configuring of camera
		//First call of this function occurs after setSurfaceFixedSize(1, 1) call in onResumeCamera method.
		//Variable isSurfaceConfiguring is used to separate first 'fake' call on surfaceChanged from second 'real' call
		//when we set desired surfaceView size
		//More info read from: https://code.google.com/p/android/issues/detail?id=191251
		if (isCameraConfiguring)
		{
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_SURFACE_CONFIGURED, 0);
			isCameraConfiguring = false;
		} else
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
		}
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
			currentIdx = Integer.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? opt1 : opt2, "6"));
			
			List<CameraController.Size> vsz = CameraController.SupportedVideoSizesList;

			CharSequence[] entriesTmp = new CharSequence[6];
			CharSequence[] entryValuesTmp = new CharSequence[6];
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), VideoCapturePlugin.QUALITY_4K) || Util.listContainsSize(vsz, new CameraController.Size(4096, 2160)))
			{
				entriesTmp[idx] = "4K";
				entryValuesTmp[idx] = "9";
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_2160P) || Util.listContainsSize(vsz, new CameraController.Size(3840, 2160)))
			{
				entriesTmp[idx] = "2160p";
				entryValuesTmp[idx] = String.valueOf(CamcorderProfile.QUALITY_2160P);
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_1080P))
			{
				entriesTmp[idx] = "1080p";
				entryValuesTmp[idx] = String.valueOf(CamcorderProfile.QUALITY_1080P);
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_720P))
			{
				entriesTmp[idx] = "720p";
				entryValuesTmp[idx] = String.valueOf(CamcorderProfile.QUALITY_720P);
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_480P))
			{
				entriesTmp[idx] = "480p";
				entryValuesTmp[idx] = String.valueOf(CamcorderProfile.QUALITY_480P);
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_CIF))
			{
				entriesTmp[idx] = "352 x 288";
				entryValuesTmp[idx] = String.valueOf(CamcorderProfile.QUALITY_CIF);
				idx++;
			}
			if (CamcorderProfile.hasProfile(CameraController.getCameraIndex(), CamcorderProfile.QUALITY_QCIF))
			{
				entriesTmp[idx] = "176 x 144";
				entryValuesTmp[idx] = String.valueOf(CamcorderProfile.QUALITY_QCIF);
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

		if (!CameraController.isRemoteCamera())
		{
			if (CameraController.isUseCamera2())
			{
				configureCamera2Camera(captureFormat);
				guiManager.setupViewfinderPreviewSize(new CameraController.Size(previewWidth, previewHeight));
			}
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
//		if (keyCode == KeyEvent.KEYCODE_BACK)
//		{
//			if (AppRater.showRateDialogIfNeeded(this))
//			{
//				return true;
//			}
//			if (AppWidgetNotifier.showNotifierDialogIfNeeded(this))
//			{
//				return true;
//			}
//		}
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
	
	// <!-- -+-
	private static boolean			unlockAllPurchased			= false;

	public boolean isUnlockedAll()
	{
		return unlockAllPurchased;
	}

	public String								titleUnlockAll				= "";

	public static boolean isPurchasedAll()
	{
		return unlockAllPurchased;
	}


	public static void purchaseAll()
	{
		if (isPurchasedAll())
			return;

		// now will call store with abc unlocked
		//UNCOMMENT for samsung!
		callStoreForUnlocked(thiz);
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
			//SAMSUNG ONLY!
			Intent intent = new Intent();
			intent.setData(Uri.parse("samsungapps://ProductDetail/com.almalence.opencam_plus")); // The string_of_uri is an 
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | 
			Intent.FLAG_ACTIVITY_CLEAR_TOP | 
			Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
			activity.startActivity(intent);
		} catch (ActivityNotFoundException e)
		{
			return;
		}
	}

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
				callStoreForUnlocked(MainScreen.thiz);
			}
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
				callStoreForUnlocked(MainScreen.thiz);
		}
		return true;
	}
	
	// -+- -->

	/************************ Billing ************************/
	/*******************************************************/


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
