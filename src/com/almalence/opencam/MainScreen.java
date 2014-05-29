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
//import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.hardware.Camera.Size;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
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
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.almalence.ui.RotateImageView;
import com.almalence.util.AppWidgetNotifier;
import com.almalence.util.HeapUtil;
import com.almalence.util.Util;

//<!-- -+-
import com.almalence.opencam.ui.AlmalenceGUI;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;
import com.almalence.util.AppRater;

import org.onepf.oms.OpenIabHelper;
import org.onepf.oms.appstore.googleUtils.IabHelper;
import org.onepf.oms.appstore.googleUtils.IabResult;
import org.onepf.oms.appstore.googleUtils.Inventory;
import org.onepf.oms.appstore.googleUtils.Purchase;
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

public class MainScreen extends Activity implements View.OnClickListener,
		View.OnTouchListener, SurfaceHolder.Callback, Camera.PictureCallback,
		Camera.AutoFocusCallback, Handler.Callback, Camera.ErrorCallback,
		Camera.PreviewCallback, Camera.ShutterCallback {
	// >>Description
	// section with different global parameters available for everyone
	//
	// Camera parameters and possibly access to camera instance
	//
	// Global defines and others
	//
	// Description<<
	public static MainScreen thiz;
	public static Context mainContext;
	public static Handler H;
	

	private Object syncObject = new Object();

	private static final int MSG_RETURN_CAPTURED = -1;

	// public static boolean FramesShot = false;

	public static File ForceFilename = null;

	private static Camera camera = null;
	private static Camera.Parameters cameraParameters = null;

	public static GUI guiManager = null;

	// OpenGL layer. May be used to allow capture plugins to draw overlaying
	// preview, such as night vision or panorama frames.
	private static GLLayer glView;

	public boolean mPausing = false;

	Bundle msavedInstanceState;
	// private. if necessary?!?!?
	public SurfaceHolder surfaceHolder;
	public SurfaceView preview;
	private OrientationEventListener orientListener;
	private boolean landscapeIsNormal = false;
	private boolean surfaceJustCreated = false;
	private boolean surfaceCreated = false;
	public byte[] pviewBuffer;

	// shared between activities
	public static int surfaceWidth, surfaceHeight;
	private static int imageWidth, imageHeight;
	public static int previewWidth, previewHeight;
	private static int saveImageWidth, saveImageHeight;
//	public static PowerManager pm = null;

	private CountDownTimer ScreenTimer = null;
	private boolean isScreenTimerRunning = false;

	public static int CameraIndex = 0;
	private static boolean CameraMirrored = false;
	private static boolean wantLandscapePhoto = false;
	public static int orientationMain = 0;
	public static int orientationMainPrevious = 0;

	private SoundPlayer shutterPlayer = null;

	// Flags to know which camera feature supported at current device
	public boolean mEVSupported = false;
	public boolean mSceneModeSupported = false;
	public boolean mWBSupported = false;
	public boolean mFocusModeSupported = false;
	public boolean mFlashModeSupported = false;
	public boolean mISOSupported = false;
	public boolean mCameraChangeSupported = false;
	
	public boolean mVideoStabilizationSupported = false;

	public static List<String> supportedSceneModes;
	public static List<String> supportedWBModes;
	public static List<String> supportedFocusModes;
	public static List<String> supportedFlashModes;
	public static List<String> supportedISOModes;

	// Common preferences
	public static String ImageSizeIdxPreference;
	public static boolean ShutterPreference = true;
	public static boolean ShotOnTapPreference = false;
	
	public static boolean showHelp = false;
	// public static boolean FullMediaRescan;
	public static final String SavePathPref = "savePathPref";

	private boolean keepScreenOn = false;
	
	public static String SaveToPath;
	public static boolean SaveInputPreference;
	public static String SaveToPreference;
	public static boolean SortByDataPreference;
	
	public static boolean MaxScreenBrightnessPreference;

	// Camera resolution variables and lists
	public static final int MIN_MPIX_SUPPORTED = 1280 * 960;
	// public static final int MIN_MPIX_PREVIEW = 600*400;

	public static int CapIdx;

	public static List<Long> ResolutionsMPixList;
	public static List<String> ResolutionsIdxesList;
	public static List<String> ResolutionsNamesList;

	public static List<Long> ResolutionsMPixListIC;
	public static List<String> ResolutionsIdxesListIC;
	public static List<String> ResolutionsNamesListIC;

	public static List<Long> ResolutionsMPixListVF;
	public static List<String> ResolutionsIdxesListVF;
	public static List<String> ResolutionsNamesListVF;

	public static final int FOCUS_STATE_IDLE = 0;
	public static final int FOCUS_STATE_FOCUSED = 1;
	public static final int FOCUS_STATE_FAIL = 3;
	public static final int FOCUS_STATE_FOCUSING = 4;

	public static final int CAPTURE_STATE_IDLE = 0;
	public static final int CAPTURE_STATE_CAPTURING = 1;

	private static int mFocusState = FOCUS_STATE_IDLE;
	private static int mCaptureState = CAPTURE_STATE_IDLE;
	
	private static boolean mAFLocked = false;

	// >>Description
	// section with initialize, resume, start, stop procedures, preferences
	// access
	//
	// Initialize, stop etc depends on plugin type.
	//
	// Create main GUI controls and plugin specific controls.
	//
	// Description<<

	public static boolean isCreating = false;
	public static boolean mApplicationStarted = false;
	public static long startTime = 0;
	
	public static final String EXTRA_ITEM = "WidgetModeID"; //Clicked mode id from widget.
	public static final String EXTRA_TORCH = "WidgetTorchMode";
	public static final String EXTRA_BARCODE = "WidgetBarcodeMode";	
	public static final String EXTRA_SHOP = "WidgetGoShopping";
	
	public static boolean launchTorch = false;
	public static boolean launchBarcode = false;
	public static boolean goShopping = false;
	
	public static String  prefFlash = "";
	public static boolean prefBarcode = false;

	public static final int VOLUME_FUNC_SHUTTER = 0;
	public static final int VOLUME_FUNC_ZOOM 	= 1;
	public static final int VOLUME_FUNC_EXPO 	= 2;
	public static final int VOLUME_FUNC_NONE	= 3;
	
	public static String deviceSS3_01;
	public static String deviceSS3_02;
	public static String deviceSS3_03;
	public static String deviceSS3_04;
	public static String deviceSS3_05;
	public static String deviceSS3_06;
	public static String deviceSS3_07;
	public static String deviceSS3_08;
	public static String deviceSS3_09;
	public static String deviceSS3_10;
	public static String deviceSS3_11;
	public static String deviceSS3_12;
	public static String deviceSS3_13;
	
	public static List<Area> mMeteringAreaMatrix5 = new ArrayList<Area>();	
	public static List<Area> mMeteringAreaMatrix4 = new ArrayList<Area>();	
	public static List<Area> mMeteringAreaMatrix1 = new ArrayList<Area>();	
	public static List<Area> mMeteringAreaCenter = new ArrayList<Area>();	
	public static List<Area> mMeteringAreaSpot = new ArrayList<Area>();
	
	public static String meteringModeMatrix = "Matrix";
	public static String meteringModeCenter = "Center-weighted";
	public static String meteringModeSpot = "Spot";
	public static String meteringModeAuto = "Auto";
	
	public static String currentMeteringMode = null;

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		deviceSS3_01 = getResources().getString(R.string.device_name_ss3_01);
		deviceSS3_02 = getResources().getString(R.string.device_name_ss3_02);
		deviceSS3_03 = getResources().getString(R.string.device_name_ss3_03);
		deviceSS3_04 = getResources().getString(R.string.device_name_ss3_04);
		deviceSS3_05 = getResources().getString(R.string.device_name_ss3_05);
		deviceSS3_06 = getResources().getString(R.string.device_name_ss3_06);
		deviceSS3_07 = getResources().getString(R.string.device_name_ss3_07);
		deviceSS3_08 = getResources().getString(R.string.device_name_ss3_08);
		deviceSS3_09 = getResources().getString(R.string.device_name_ss3_09);
		deviceSS3_10 = getResources().getString(R.string.device_name_ss3_10);
		deviceSS3_11 = getResources().getString(R.string.device_name_ss3_11);
		deviceSS3_12 = getResources().getString(R.string.device_name_ss3_12);
		deviceSS3_13 = getResources().getString(R.string.device_name_ss3_13);
		
		Intent intent = this.getIntent();
		String mode = intent.getStringExtra(EXTRA_ITEM);
		launchTorch = intent.getBooleanExtra(EXTRA_TORCH, false);
		launchBarcode = intent.getBooleanExtra(EXTRA_BARCODE, false);
		goShopping = intent.getBooleanExtra(EXTRA_SHOP, false);
		
		startTime = System.currentTimeMillis();
		msavedInstanceState = savedInstanceState;
		mainContext = this.getBaseContext();
		H = new Handler(this);
		thiz = this;

		mApplicationStarted = false;

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// ensure landscape orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// set to fullscreen
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN|WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

		// set some common view here
		setContentView(R.layout.opencamera_main_layout);
		
		//reset or save settings
		ResetOrSaveSettings();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		
		if(null != mode)
			prefs.edit().putString("defaultModeName", mode).commit();
		
		if(launchTorch)
		{
			prefFlash = prefs.getString(GUI.sFlashModePref, Camera.Parameters.FLASH_MODE_AUTO);
			prefs.edit().putString(GUI.sFlashModePref, Camera.Parameters.FLASH_MODE_TORCH).commit();
		}
		
		if(launchBarcode)
		{
			prefBarcode = prefs.getBoolean("PrefBarcodescannerVF", false);
			prefs.edit().putBoolean("PrefBarcodescannerVF", true).commit();
		}
		
		// <!-- -+-
		
		/**** Billing *****/
		if (true == prefs.contains("unlock_all_forever")) {
			unlockAllPurchased = prefs.getBoolean("unlock_all_forever", false);
		}
		if (true == prefs.contains("plugin_almalence_hdr")) {
			hdrPurchased = prefs.getBoolean("plugin_almalence_hdr", false);
		}
		if (true == prefs.contains("plugin_almalence_panorama")) {
			panoramaPurchased = prefs.getBoolean("plugin_almalence_panorama", false);
		}
		if (true == prefs.contains("plugin_almalence_moving_burst")) {
			objectRemovalBurstPurchased = prefs.getBoolean("plugin_almalence_moving_burst", false);
		}
		if (true == prefs.contains("plugin_almalence_groupshot")) {
			groupShotPurchased = prefs.getBoolean("plugin_almalence_groupshot", false);
		}
		
		createBillingHandler();
		/**** Billing *****/
		
		//application rating helper
		AppRater.app_launched(this);
		//-+- -->
		
		AppWidgetNotifier.app_launched(this);
		
		// set preview, on click listener and surface buffers
		preview = (SurfaceView) this.findViewById(R.id.SurfaceView01);
		preview.setOnClickListener(this);
		preview.setOnTouchListener(this);
		preview.setKeepScreenOn(true);

		surfaceHolder = preview.getHolder();
		surfaceHolder.addCallback(this);
		surfaceHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

		orientListener = new OrientationEventListener(this) {
			@Override
			public void onOrientationChanged(int orientation) {
				// figure landscape or portrait
				if (MainScreen.thiz.landscapeIsNormal) {
					// Log.e("MainScreen",
					// "landscapeIsNormal = true. Orientation " + orientation +
					// "+90");
					orientation += 90;
				}
				// else
				// Log.e("MainScreen", "landscapeIsNormal = false. Orientation "
				// + orientation);

				if ((orientation < 45)
						|| (orientation > 315 && orientation < 405)
						|| ((orientation > 135) && (orientation < 225))) {
					if (MainScreen.wantLandscapePhoto == true) {
						MainScreen.wantLandscapePhoto = false;
						// Log.e("MainScreen", "Orientation = " + orientation);
						// Log.e("MainScreen","Orientation Changed. wantLandscapePhoto = "
						// + String.valueOf(MainScreen.wantLandscapePhoto));
						//PluginManager.getInstance().onOrientationChanged(false);
					}
				} else {
					if (MainScreen.wantLandscapePhoto == false) {
						MainScreen.wantLandscapePhoto = true;
						// Log.e("MainScreen", "Orientation = " + orientation);
						// Log.e("MainScreen","Orientation Changed. wantLandscapePhoto = "
						// + String.valueOf(MainScreen.wantLandscapePhoto));
						//PluginManager.getInstance().onOrientationChanged(true);
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
				
				if(orientationMain != orientationMainPrevious)
				{
					orientationMainPrevious = orientationMain;
					//PluginManager.getInstance().onOrientationChanged(orientationMain);
				}
			}
		};

		//pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

		keepScreenOn = prefs.getBoolean("keepScreenOn", false);
		// prevent power drain
//		if (!keepScreenOn)
		{
			ScreenTimer = new CountDownTimer(180000, 180000) {
				public void onTick(long millisUntilFinished) {
				}
	
				public void onFinish() {
					boolean isVideoRecording = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getBoolean("videorecording", false);
					if (isVideoRecording || keepScreenOn)
					{
						//restart timer
						ScreenTimer.start();
						isScreenTimerRunning = true;
						preview.setKeepScreenOn(true);
						return;
					}
					preview.setKeepScreenOn(false);
					isScreenTimerRunning = false;
				}
			};
			ScreenTimer.start();
			isScreenTimerRunning = true;
		}

		PluginManager.getInstance().setupDefaultMode();
		// Description
		// init gui manager
		guiManager = new AlmalenceGUI();
		guiManager.createInitialGUI();
		this.findViewById(R.id.mainLayout1).invalidate();
		this.findViewById(R.id.mainLayout1).requestLayout();
		guiManager.onCreate();

		// init plugin manager
		PluginManager.getInstance().onCreate();

		if (this.getIntent().getAction() != null) {
			if (this.getIntent().getAction()
					.equals(MediaStore.ACTION_IMAGE_CAPTURE)) {
				try {
					MainScreen.ForceFilename = new File(
							((Uri) this.getIntent().getExtras()
									.getParcelable(MediaStore.EXTRA_OUTPUT))
									.getPath());
					if (MainScreen.ForceFilename.getAbsolutePath().equals(
							"/scrapSpace")) {
						MainScreen.ForceFilename = new File(Environment
								.getExternalStorageDirectory()
								.getAbsolutePath()
								+ "/mms/scrapSpace/.temp.jpg");
						new File(MainScreen.ForceFilename.getParent()).mkdirs();
					}
				} catch (Exception e) {
					MainScreen.ForceFilename = null;
				}
			} else {
				MainScreen.ForceFilename = null;
			}
		} else {
			MainScreen.ForceFilename = null;
		}
		
		// <!-- -+-
		if(goShopping)
		{
//			MainScreen.thiz.showUnlock = true;
			if (MainScreen.thiz.titleUnlockAll == null || MainScreen.thiz.titleUnlockAll.endsWith("check for sale"))
			{
				Toast.makeText(MainScreen.mainContext, "Error connecting to Google Play. Check internet connection.", Toast.LENGTH_LONG).show();
				return;
			}
//			Intent shopintent = new Intent(MainScreen.thiz, Preferences.class);
//			MainScreen.thiz.startActivity(shopintent);
			guiManager.showStore();
		}
		//-+- -->
	}

	public void onPreferenceCreate(PreferenceFragment prefActivity)
	{
		CharSequence[] entries;
		CharSequence[] entryValues;

		if (ResolutionsIdxesList != null) {
			entries = ResolutionsNamesList
					.toArray(new CharSequence[ResolutionsNamesList.size()]);
			entryValues = ResolutionsIdxesList
					.toArray(new CharSequence[ResolutionsIdxesList.size()]);

			
			ListPreference lp = (ListPreference) prefActivity
					.findPreference("imageSizePrefCommonBack");
			ListPreference lp2 = (ListPreference) prefActivity
					.findPreference("imageSizePrefCommonFront");
			
			if(CameraIndex == 0 && lp2 != null && lp != null)
			{
				prefActivity.getPreferenceScreen().removePreference(lp2);
				lp.setEntries(entries);
				lp.setEntryValues(entryValues);
			}
			else if(lp2 != null && lp != null)
			{
				prefActivity.getPreferenceScreen().removePreference(lp);
				lp2.setEntries(entries);
				lp2.setEntryValues(entryValues);
			}
			else
				return;

			// set currently selected image size
			int idx;
			for (idx = 0; idx < ResolutionsIdxesList.size(); ++idx) {
				if (Integer.parseInt(ResolutionsIdxesList.get(idx)) == CapIdx) {
					break;
				}
			}
			if (idx < ResolutionsIdxesList.size()) {
				if(CameraIndex == 0)
					lp.setValueIndex(idx);
				else
					lp2.setValueIndex(idx);
			}
			if(CameraIndex == 0)
			lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
				// @Override
				public boolean onPreferenceChange(Preference preference,
						Object newValue) {
					int value = Integer.parseInt(newValue.toString());
					CapIdx = value;
					return true;
				}
			});
			else
				lp2.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					// @Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						int value = Integer.parseInt(newValue.toString());
						CapIdx = value;
						return true;
					}
				});
				
		}
	}
	
	public void glSetRenderingMode(final int renderMode)
 	{
 		if (renderMode != GLSurfaceView.RENDERMODE_WHEN_DIRTY
 				&& renderMode != GLSurfaceView.RENDERMODE_CONTINUOUSLY)
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

		if (surfaceView != null && runnable != null) {
			surfaceView.queueEvent(runnable);
		}
	}

	@Override
	protected void onStart()
	{
		super.onStart();
		MainScreen.guiManager.onStart();
		PluginManager.getInstance().onStart();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		mApplicationStarted = false;
		orientationMain = 0;
		orientationMainPrevious = 0;
		MainScreen.guiManager.onStop();
		PluginManager.getInstance().onStop();
	}

	@Override
	protected void onDestroy()
	{	
		super.onDestroy();
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		if(launchTorch && prefs.getString(GUI.sFlashModePref, "").contains(Camera.Parameters.FLASH_MODE_TORCH))
		{
			prefs.edit().putString(GUI.sFlashModePref, prefFlash).commit();
		}
		if(launchBarcode && prefs.getBoolean("PrefBarcodescannerVF", false))
		{
			prefs.edit().putBoolean("PrefBarcodescannerVF", prefBarcode).commit();
		}
		MainScreen.guiManager.onDestroy();
		PluginManager.getInstance().onDestroy();

		// <!-- -+-
		/**** Billing *****/
		destroyBillingHandler();
		/**** Billing *****/
		//-+- -->
		
		glView = null;
	}

	@Override
	protected void onResume()
	{		
		super.onResume();
		
		if (!isCreating)
			new CountDownTimer(50, 50) {
				public void onTick(long millisUntilFinished) {
				}

				public void onFinish() {
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(MainScreen.mainContext);
					CameraIndex = prefs.getBoolean("useFrontCamera", false) == false ? 0
							: 1;
					ShutterPreference = prefs.getBoolean("shutterPrefCommon",
							false);
					ShotOnTapPreference = prefs.getBoolean("shotontapPrefCommon",
							false);
					ImageSizeIdxPreference = prefs.getString(CameraIndex == 0 ?
							"imageSizePrefCommonBack" : "imageSizePrefCommonFront", "-1");
					Log.e("MainScreen", "ImageSizeIdxPreference = " + ImageSizeIdxPreference);
					// FullMediaRescan = prefs.getBoolean("mediaPref", true);
					SaveToPath = prefs.getString(SavePathPref, Environment
							.getExternalStorageDirectory().getAbsolutePath());
					SaveInputPreference = prefs.getBoolean("saveInputPref",
							false);
					SaveToPreference = prefs.getString("saveToPref", "0");
					SortByDataPreference = prefs.getBoolean("sortByDataPref",
							false);
					
					MaxScreenBrightnessPreference = prefs.getBoolean("maxScreenBrightnessPref", false);
					setScreenBrightness(MaxScreenBrightnessPreference);

					MainScreen.guiManager.onResume();
					PluginManager.getInstance().onResume();
					MainScreen.thiz.mPausing = false;

					if (surfaceCreated && (camera == null)) {
						MainScreen.thiz.findViewById(R.id.mainLayout2)
								.setVisibility(View.VISIBLE);
						setupCamera(surfaceHolder);

						PluginManager.getInstance().onGUICreate();
						MainScreen.guiManager.onGUICreate();
						
						if (showStore)
						{
							guiManager.showStore();
							showStore = false;
						}
					}
					orientListener.enable();
				}
			}.start();

		shutterPlayer = new SoundPlayer(this.getBaseContext(), getResources()
				.openRawResourceFd(R.raw.plugin_capture_tick));

		if (ScreenTimer != null) {
			if (isScreenTimerRunning)
				ScreenTimer.cancel();
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
			ScreenTimer.start();
			isScreenTimerRunning = true;
		}

		Log.e("Density", "" + getResources().getDisplayMetrics().toString());
		
		long Free = getAvailableInternalMemory();
		//Log.e("vdsfs", "Memory: free: "+Free+"Mb");
		if (Free<30)
			Toast.makeText(MainScreen.mainContext, "Almost no free space left on internal storage.", Toast.LENGTH_LONG).show();
	}

	private long getAvailableInternalMemory()
	{
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize / 1048576;
	}
	
	@Override
	protected void onPause() {
		super.onPause();
		mApplicationStarted = false;

		MainScreen.guiManager.onPause();
		PluginManager.getInstance().onPause(true);

		orientListener.disable();

		// initiate full media rescan
		// if (FramesShot && FullMediaRescan)
		// {
		// // using MediaScannerConnection.scanFile(this, paths, null, null);
		// instead
		// sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
		// Uri.parse("file://"+ Environment.getExternalStorageDirectory())));
		// FramesShot = false;
		// }

		if (ShutterPreference) {
			AudioManager mgr = (AudioManager) MainScreen.thiz
					.getSystemService(MainScreen.mainContext.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, false);
		}

		this.mPausing = true;

		this.hideOpenGLLayer();

		if (ScreenTimer != null) {
			if (isScreenTimerRunning)
				ScreenTimer.cancel();
			isScreenTimerRunning = false;
		}

		//reset tourch
		try 
    	{
    		 Camera.Parameters p = getCameraParameters();
    		 if (isFlashModeSupported())
        	 {	
    			 p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
    			 setCameraParameters(p);
        	 }
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Main", "Torch exception: " + e.getMessage());
		}
		
		if (camera != null) {
			camera.setPreviewCallback(null);
			camera.stopPreview();
			camera.release();
			camera = null;
			cameraParameters = null;
		}

		this.findViewById(R.id.mainLayout2).setVisibility(View.INVISIBLE);

		if (shutterPlayer != null) {
			shutterPlayer.release();
			shutterPlayer = null;
		}
	}

	public void PauseMain() {
		onPause();
	}

	public void StopMain() {
		onStop();
	}

	public void StartMain() {
		onStart();
	}

	public void ResumeMain() {
		onResume();
	}

	@Override
	public void surfaceChanged(final SurfaceHolder holder, final int format,
			final int width, final int height) {

		if (!isCreating)
			new CountDownTimer(50, 50) {
				public void onTick(long millisUntilFinished) {

				}

				public void onFinish() {
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(MainScreen.mainContext);
					CameraIndex = prefs.getBoolean("useFrontCamera", false) == false ? 0
							: 1;
					ShutterPreference = prefs.getBoolean("shutterPrefCommon",
							false);
					ShotOnTapPreference = prefs.getBoolean("shotontapPrefCommon",
							false);
					ImageSizeIdxPreference = prefs.getString(CameraIndex == 0 ?
							"imageSizePrefCommonBack" : "imageSizePrefCommonFront", "-1");
					// FullMediaRescan = prefs.getBoolean("mediaPref", true);

					if (!MainScreen.thiz.mPausing && surfaceCreated
							&& (camera == null)) {
						surfaceWidth = width;
						surfaceHeight = height;
						MainScreen.thiz.findViewById(R.id.mainLayout2)
								.setVisibility(View.VISIBLE);
						setupCamera(holder);
						PluginManager.getInstance().onGUICreate();
						MainScreen.guiManager.onGUICreate();
					}
				}
			}.start();
		else {
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(MainScreen.mainContext);
			CameraIndex = prefs.getBoolean("useFrontCamera", false) == false ? 0
					: 1;
			ShutterPreference = prefs.getBoolean("shutterPrefCommon", false);
			ShotOnTapPreference = prefs.getBoolean("shotontapPrefCommon",false);
			ImageSizeIdxPreference = prefs.getString(CameraIndex == 0 ?
					"imageSizePrefCommonBack" : "imageSizePrefCommonFront",
					"-1");
			// FullMediaRescan = prefs.getBoolean("mediaPref", true);

			if (!MainScreen.thiz.mPausing && surfaceCreated && (camera == null)) {
				surfaceWidth = width;
				surfaceHeight = height;
			}
		}
	}


	@TargetApi(9)
	protected void openCameraFrontOrRear() {
		if (Camera.getNumberOfCameras() > 0) {
			camera = Camera.open(MainScreen.CameraIndex);
		}

		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
		Camera.getCameraInfo(CameraIndex, cameraInfo);

		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
			CameraMirrored = true;
		else
			CameraMirrored = false;
	}

	public void setupCamera(SurfaceHolder holder) {
		if (camera == null) {
			try {
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.GINGERBREAD) {
					openCameraFrontOrRear();
				} else {
					camera = Camera.open();
				}
			} catch (RuntimeException e) {
				camera = null;
			}

			if (camera == null) {
				Toast.makeText(MainScreen.thiz, "Unable to start camera", Toast.LENGTH_LONG).show();
				return;
			}
		}
		
		cameraParameters = camera.getParameters(); //Initialize of camera parameters variable
		
		if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	    	mVideoStabilizationSupported = isVideoStabilizationSupported();

		PluginManager.getInstance().SelectDefaults();

		// screen rotation
		if (!PluginManager.getInstance().shouldPreviewToGPU())
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

		if (MainScreen.camera == null)
			return;
		//Camera.Parameters cp = MainScreen.cameraParameters;
		
		PopulateCameraDimensions(cameraParameters);
		ResolutionsMPixListIC = ResolutionsMPixList;
		ResolutionsIdxesListIC = ResolutionsIdxesList;
		ResolutionsNamesListIC = ResolutionsNamesList;

		PluginManager.getInstance().SelectImageDimension(); // updates SX, SY
															// values

		// ----- Select preview dimensions with ratio correspondent to full-size
		// image
		PluginManager.getInstance().SetCameraPreviewSize(cameraParameters);

		guiManager.setupViewfinderPreviewSize(cameraParameters);

		Size previewSize = cameraParameters.getPreviewSize();

		pviewBuffer = new byte[previewSize.width
				* previewSize.height
				* ImageFormat.getBitsPerPixel(cameraParameters
						.getPreviewFormat()) / 8];

		camera.setErrorCallback(MainScreen.thiz);

		supportedSceneModes = getSupportedSceneModes();
		supportedWBModes = getSupportedWhiteBalance();
		supportedFocusModes = getSupportedFocusModes();
		supportedFlashModes = getSupportedFlashModes();
		supportedISOModes = getSupportedISO();

		PluginManager.getInstance().SetCameraPictureSize();
		PluginManager.getInstance().SetupCameraParameters();
		//cp = cameraParameters;

		try {
			//Log.i("CameraTest", Build.MODEL);
			if (Build.MODEL.contains("Nexus 5"))
			{
				cameraParameters.setPreviewFpsRange(7000, 30000);
				setCameraParameters(cameraParameters);
			}
			
			//Log.i("CameraTest", "fps ranges "+range.size()+" " + range.get(0)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX] + " " + range.get(0)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
			//cameraParameters.setPreviewFpsRange(range.get(0)[Camera.Parameters.PREVIEW_FPS_MIN_INDEX], range.get(0)[Camera.Parameters.PREVIEW_FPS_MAX_INDEX]);
			//cameraParameters.setPreviewFpsRange(7000, 30000);
			// an obsolete but much more reliable way of setting preview to a reasonable fps range
			// Nexus 5 is giving preview which is too dark without this
			//cameraParameters.setPreviewFrameRate(30);
		
			
		} catch (RuntimeException e) {
			Log.e("CameraTest", "MainScreen.setupCamera unable setParameters "
					+ e.getMessage());
		}

//		previewWidth = cameraParameters.getPreviewSize().width;
//		previewHeight = cameraParameters.getPreviewSize().height;
		previewWidth = cameraParameters.getPreviewSize().width;
		previewHeight = cameraParameters.getPreviewSize().height;

		try
		{
			Util.initialize(mainContext);
			Util.initializeMeteringMatrix();
		}
		catch(Exception e)
		{
			Log.e("Main setup camera", "Util.initialize failed!");
		}
		
		prepareMeteringAreas();

		guiManager.onCameraCreate();
		PluginManager.getInstance().onCameraParametersSetup();
		guiManager.onPluginsInitialized();

		// ----- Start preview and setup frame buffer if needed

		// ToDo: call camera release sequence from onPause somewhere ???
		new CountDownTimer(10, 10) {
			@Override
			public void onFinish() {
				if (!PluginManager.getInstance().shouldPreviewToGPU())
				{
					try // exceptions sometimes happen here when resuming after
						// processing
					{
						camera.startPreview();
					} catch (RuntimeException e) {
						Toast.makeText(MainScreen.thiz, "Unable to start camera", Toast.LENGTH_LONG).show();
						return;
					}
				}

				camera.setPreviewCallbackWithBuffer(MainScreen.thiz);
				camera.addCallbackBuffer(pviewBuffer);

				PluginManager.getInstance().onCameraSetup();
				guiManager.onCameraSetup();
				MainScreen.mApplicationStarted = true;
			}

			@Override
			public void onTick(long millisUntilFinished) {
			}
		}.start();
	}
	
	private void prepareMeteringAreas()
	{
		Rect centerRect = Util.convertToDriverCoordinates(new Rect(previewWidth/4, previewHeight/4, previewWidth - previewWidth/4, previewHeight - previewHeight/4));
		Rect topLeftRect = Util.convertToDriverCoordinates(new Rect(0, 0, previewWidth/2, previewHeight/2));
		Rect topRightRect = Util.convertToDriverCoordinates(new Rect(previewWidth/2, 0, previewWidth, previewHeight/2));
		Rect bottomRightRect = Util.convertToDriverCoordinates(new Rect(previewWidth/2, previewHeight/2, previewWidth, previewHeight));
		Rect bottomLeftRect = Util.convertToDriverCoordinates(new Rect(0, previewHeight/2, previewWidth/2, previewHeight));
		Rect spotRect = Util.convertToDriverCoordinates(new Rect(previewWidth/2 - 10, previewHeight/2 - 10, previewWidth/2 + 10, previewHeight/2 + 10));
		
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

	public static void PopulateCameraDimensions(Camera.Parameters cp) {
		ResolutionsMPixList = new ArrayList<Long>();
		ResolutionsIdxesList = new ArrayList<String>();
		ResolutionsNamesList = new ArrayList<String>();

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
		
		
		List<Camera.Size> cs;
		int MinMPIX = MIN_MPIX_SUPPORTED;
		cs = cp.getSupportedPictureSizes();
		
		//add 8 Mpix for rear camera for HTC One X
		if(Build.MODEL.contains("HTC One X"))
		{
			if (getCameraMirrored() == false)
			{
				Camera.Size additional= null;
				additional= MainScreen.camera.new Size(3264, 2448);
				additional.width = 3264;
				additional.height = 2448;
				cs.add(additional);
			}
		}

		CharSequence[] RatioStrings = { " ", "4:3", "3:2", "16:9", "1:1" };

		int iHighestIndex = 0;
		Size sHighest = cs.get(iHighestIndex);

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
		
		for (int ii = 0; ii < cs.size(); ++ii) {
			Size s = cs.get(ii);

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
			for (loc = 0; loc < ResolutionsMPixList.size(); ++loc)
				if (ResolutionsMPixList.get(loc) < lmpix)
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

			ResolutionsNamesList.add(loc,
					String.format("%3.1f Mpix  " + RatioStrings[ri], mpix));
			ResolutionsIdxesList.add(loc, String.format("%d", ii));
			ResolutionsMPixList.add(loc, lmpix);
		}

		if (ResolutionsNamesList.size() == 0) {
			Size s = cs.get(iHighestIndex);

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

			ResolutionsNamesList.add(0,
					String.format("%3.1f Mpix  " + RatioStrings[ri], mpix));
			ResolutionsIdxesList.add(0, String.format("%d", 0));
			ResolutionsMPixList.add(0, lmpix);
		}

		return;
	}

	@Override
	public void surfaceCreated(SurfaceHolder holder) {
		// ----- Find 'normal' orientation of the device

		Display display = ((WindowManager) this
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		if ((rotation == Surface.ROTATION_90)
				|| (rotation == Surface.ROTATION_270))
			landscapeIsNormal = true; // false; - if landscape view orientation
										// set for MainScreen
		else
			landscapeIsNormal = false;

		surfaceCreated = true;
		surfaceJustCreated = true;
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder) {
		surfaceCreated = false;
		surfaceJustCreated = false;
	}

	@TargetApi(14)
	public boolean isFaceDetectionAvailable(Camera.Parameters params) {
		if (params.getMaxNumDetectedFaces() > 0)
			return true;
		else
			return false;
	}

	public Size getPreviewSize() {
		LayoutParams lp = preview.getLayoutParams();
		if (lp == null)
			return null;

		if (camera != null)
			return camera.new Size(lp.width, lp.height);
		else
			return null;
	}

	public int getPreviewWidth() {
		LayoutParams lp = preview.getLayoutParams();
		if (lp == null)
			return 0;

		return lp.width;

	}

	public int getPreviewHeight() {
		LayoutParams lp = preview.getLayoutParams();
		if (lp == null)
			return 0;

		return lp.height;
	}

	@Override
	public void onError(int error, Camera camera) {
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
	public boolean isFrontCamera() {
		return CameraMirrored;
	}

	public Camera getCamera() {
		return camera;
	}

	public void setCamera(Camera cam) {
		camera = cam;
	}

	public Camera.Parameters getCameraParameters() {
		if (camera != null && cameraParameters != null)
			return cameraParameters;

		return null;
	}

	public boolean setCameraParameters(Camera.Parameters params) {
		if (params != null && camera != null)
		{			
			try
			{
				camera.setParameters(params);
				cameraParameters = params;
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
	
	@TargetApi(15)
	public void setVideoStabilization(boolean stabilization)
	{
		if(cameraParameters != null && cameraParameters.isVideoStabilizationSupported())
		{
			cameraParameters.setVideoStabilization(stabilization);
			this.setCameraParameters(cameraParameters);
		}
	}
	
	@TargetApi(15)
	public boolean isVideoStabilizationSupported()
	{
		if(cameraParameters != null)
			return cameraParameters.isVideoStabilizationSupported();
		
		return false;
	}
	
	public boolean isExposureLockSupported() {
		if (camera != null && cameraParameters != null) {
			if (cameraParameters.isAutoExposureLockSupported())
				return true;
			else
				return false;
		} else
			return false;
	}
	
	public boolean isWhiteBalanceLockSupported() {
		if (camera != null && cameraParameters != null) {
			if (cameraParameters.isAutoWhiteBalanceLockSupported())
				return true;
			else
				return false;
		} else
			return false;
	}

	public boolean isExposureCompensationSupported() {
		if (camera != null && cameraParameters != null) {
			if (cameraParameters.getMinExposureCompensation() == 0
					&& cameraParameters.getMaxExposureCompensation() == 0)
				return false;
			else
				return true;
		} else
			return false;
	}

	public int getMinExposureCompensation() {
		if (camera != null && cameraParameters != null)
			return cameraParameters.getMinExposureCompensation();
		else
			return 0;
	}

	public int getMaxExposureCompensation() {
		if (camera != null && cameraParameters != null)
			return cameraParameters.getMaxExposureCompensation();
		else
			return 0;
	}

	public float getExposureCompensationStep() {
		if (camera != null && cameraParameters != null)
			return cameraParameters.getExposureCompensationStep();
		else
			return 0;
	}

	public float getExposureCompensation() {
		if (camera != null && cameraParameters != null)
			return cameraParameters.getExposureCompensation()
					* cameraParameters.getExposureCompensationStep();
		else
			return 0;
	}

	public void resetExposureCompensation() {
		if (camera != null) {
			if (!isExposureCompensationSupported())
				return;
			Camera.Parameters params = cameraParameters;
			params.setExposureCompensation(0);
			setCameraParameters(params);
		}
	}

	public boolean isSceneModeSupported() {
		List<String> supported_scene = getSupportedSceneModes();
		if (supported_scene != null && supported_scene.size() > 0)
			return true;
		else
			return false;
	}

	public List<String> getSupportedSceneModes() {
		if (camera != null)
			return cameraParameters.getSupportedSceneModes();

		return null;
	}

	public boolean isWhiteBalanceSupported() {
		List<String> supported_wb = getSupportedWhiteBalance();
		if (supported_wb != null && supported_wb.size() > 0)
			return true;
		else
			return false;
	}

	public List<String> getSupportedWhiteBalance() {
		if (camera != null)
			return cameraParameters.getSupportedWhiteBalance();

		return null;
	}

	public boolean isFocusModeSupported() {
		List<String> supported_focus = getSupportedFocusModes();
		if (supported_focus != null && supported_focus.size() > 0)
			return true;
		else
			return false;
	}

	public List<String> getSupportedFocusModes() {
		if (camera != null)
			return cameraParameters.getSupportedFocusModes();

		return null;
	}

	public boolean isFlashModeSupported() {
		List<String> supported_flash = getSupportedFlashModes();
		if (supported_flash != null && supported_flash.size() > 0)
			return true;
		else
			return false;
	}

	public List<String> getSupportedFlashModes() {		
		if (camera != null)
			return cameraParameters.getSupportedFlashModes();

		return null;
	}

	public boolean isISOSupported() {
		List<String> supported_iso = getSupportedISO();
		String isoSystem = MainScreen.thiz.getCameraParameters().get("iso");
		String isoSystem2 = MainScreen.thiz.getCameraParameters().get("iso-speed");
		if ((supported_iso != null && supported_iso.size() > 0) || isoSystem != null || isoSystem2 != null)
			return true;
		else
			return false;
	}

	public List<String> getSupportedISO()
	{
		if (camera != null)
		{
			Camera.Parameters camParams = MainScreen.cameraParameters;
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
				List<String> isoList = new ArrayList<String>();				
				for (int i = 0; i < ISOs.length; i++)
					isoList.add(ISOs[i]);

				return isoList;
			}
		}

		return null;
	}
	
	public int getMaxNumMeteringAreas()
	{
		if(camera != null)
		{
			Camera.Parameters camParams = MainScreen.cameraParameters;
			return camParams.getMaxNumMeteringAreas();
		}
		
		return 0;
	}

	public String getSceneMode() {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;
			if (params != null)
				return params.getSceneMode();
		}

		return null;
	}

	public String getWBMode() {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;
			if (params != null)
				return params.getWhiteBalance();
		}

		return null;
	}

	public String getFocusMode() {
		
		try {
			if (camera != null) {
				Camera.Parameters params = cameraParameters;
				if (params != null)
					return params.getFocusMode();
			}
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.e("MainScreen", "getFocusMode exception: " + e.getMessage());
		}

		return null;
	}

	public String getFlashMode() {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;
			if (params != null)
				return params.getFlashMode();
		}

		return null;
	}

	public String getISOMode() {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;			
			if (params != null)
			{
				String iso = null;
				iso = params.get("iso");
				if(iso == null)
					iso = params.get("iso-speed");
				
				return iso;
			}
		}

		return null;
	}

	public void setCameraSceneMode(String mode) {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;
			if (params != null) {
				params.setSceneMode(mode);
				setCameraParameters(params);
			}
		}
	}

	public void setCameraWhiteBalance(String mode) {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;
			if (params != null) {
				params.setWhiteBalance(mode);
				setCameraParameters(params);
			}
		}
	}

	public void setCameraFocusMode(String mode) {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;
			if (params != null) {
				params.setFocusMode(mode);
				setCameraParameters(params);
				mAFLocked = false;
			}
		}
	}

	public void setCameraFlashMode(String mode) {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;
			if (params != null && mode != "") {
				params.setFlashMode(mode);
				setCameraParameters(params);
			}
		}
	}

	public void setCameraISO(String mode) {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;
			if (params != null) {
				if(params.get("iso") != null)
					params.set("iso", mode);
				else if(params.get("iso-speed") != null)
					params.set("iso-speed", mode);
				
				setCameraParameters(params);
			}
		}
	}
	
	public void setCameraMeteringMode(String mode)
	{
		if (camera != null)
		{
			Camera.Parameters params = cameraParameters;
			if(meteringModeAuto.contains(mode))
				setCameraMeteringAreas(null);
			else if(meteringModeMatrix.contains(mode))
			{				
				int maxAreasCount = params.getMaxNumMeteringAreas();
				if(maxAreasCount > 4)
					setCameraMeteringAreas(mMeteringAreaMatrix5);
				else if(maxAreasCount > 3)
					setCameraMeteringAreas(mMeteringAreaMatrix4);
				else if(maxAreasCount > 0)
					setCameraMeteringAreas(mMeteringAreaMatrix1);
				else
					setCameraMeteringAreas(null);					
			}
			else if(meteringModeCenter.contains(mode))
				setCameraMeteringAreas(mMeteringAreaCenter);
			else if(meteringModeSpot.contains(mode))
				setCameraMeteringAreas(mMeteringAreaSpot);
			
			currentMeteringMode = mode;
		}
	}

	public void setCameraExposureCompensation(int iEV) {
		if (camera != null) {
			Camera.Parameters params = cameraParameters;
			if (params != null) {
				params.setExposureCompensation(iEV);
				setCameraParameters(params);
			}
		}
	}

	/*
	 * CAMERA PARAMETERS SECTION Supplementary methods for those plugins that
	 * need an icons of supported camera parameters (scene, iso, wb, flash,
	 * focus) Methods return id of drawable icon
	 */
	public int getSceneIcon(String sceneMode) {
		return guiManager.getSceneIcon(sceneMode);
	}

	public int getWBIcon(String wb) {
		return guiManager.getWBIcon(wb);
	}

	public int getFocusIcon(String focusMode) {
		return guiManager.getFocusIcon(focusMode);
	}

	public int getFlashIcon(String flashMode) {
		return guiManager.getFlashIcon(flashMode);
	}

	public int getISOIcon(String isoMode) {
		return guiManager.getISOIcon(isoMode);
	}

	public void updateCameraFeatures() {
		mEVSupported = isExposureCompensationSupported();
		mSceneModeSupported = isSceneModeSupported();
		mWBSupported = isWhiteBalanceSupported();
		mFocusModeSupported = isFocusModeSupported();
		mFlashModeSupported = isFlashModeSupported();
		mISOSupported = isISOSupported();
	}

	public void setCameraFocusAreas(List<Area> focusAreas) {
		if (camera != null) {
			try {
				Camera.Parameters params = cameraParameters;
				if (params != null) {
					params.setFocusAreas(focusAreas);
					setCameraParameters(params);
				}
			} catch (RuntimeException e) {
				Log.e("SetFocusArea", e.getMessage());
			}
		}
	}

	public void setCameraMeteringAreas(List<Area> meteringAreas) {
		if (camera != null) {
			try {
				Camera.Parameters params = cameraParameters;
				if (params != null) {
//					Rect rect = meteringAreas.get(0).rect;
//					Log.e("MainScreen", "Metering area: " + rect.left + ", " + rect.top + " - " + rect.right + ", " + rect.bottom);
					if(meteringAreas != null)
					{
						params.setMeteringAreas(null);
						setCameraParameters(params);
					}
					params.setMeteringAreas(meteringAreas);
					setCameraParameters(params);
				}
			} catch (RuntimeException e) {
				Log.e("SetMeteringArea", e.getMessage());
			}
		}
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

	public static boolean takePicture() {
		synchronized (MainScreen.thiz.syncObject) {
			if (camera != null && getFocusState() != MainScreen.FOCUS_STATE_FOCUSING) 
			{
				MainScreen.mCaptureState = MainScreen.CAPTURE_STATE_CAPTURING;
				// Log.e("", "mFocusState = " + getFocusState());
				camera.setPreviewCallback(null);
				camera.takePicture(null, null, null, MainScreen.thiz);
				return true;
			}

			// Log.e("", "takePicture(). FocusState = FOCUS_STATE_FOCUSING ");
			return false;
		}
	}

	public static boolean autoFocus(Camera.AutoFocusCallback listener) {
		synchronized (MainScreen.thiz.syncObject) {
			if (camera != null) {
				if (mCaptureState != CAPTURE_STATE_CAPTURING) {
					setFocusState(MainScreen.FOCUS_STATE_FOCUSING);
					try {
						camera.autoFocus(listener);
					}catch (Exception e) {
						e.printStackTrace();
						Log.e("MainScreen autoFocus(listener) failed", "autoFocus: " + e.getMessage());
						return false;
					}
					return true;
				}
			}
			return false;
		}
	}

	public static boolean autoFocus() {
		synchronized (MainScreen.thiz.syncObject) {
			if (camera != null) {
				if (mCaptureState != CAPTURE_STATE_CAPTURING) {
					String fm = thiz.getFocusMode();
					// Log.e("", "mCaptureState = " + mCaptureState);
					setFocusState(MainScreen.FOCUS_STATE_FOCUSING);
					try {
						camera.autoFocus(MainScreen.thiz);
					}catch (Exception e) {
						e.printStackTrace();
						Log.e("MainScreen autoFocus() failed", "autoFocus: " + e.getMessage());
						return false;
					}					
					return true;
				}
			}
			return false;
		}
	}

	public static void cancelAutoFocus() {
		if (camera != null) {
			setFocusState(MainScreen.FOCUS_STATE_IDLE);
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
	
	public static void setAutoFocusLock(boolean locked)
	{
		mAFLocked = locked;
	}
	
	public static boolean getAutoFocusLock()
	{
		return mAFLocked;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (!mApplicationStarted)
			return true;

		//menu button processing
		if (keyCode == KeyEvent.KEYCODE_MENU) {
			menuButtonPressed();
			return true;
		}
		//shutter/camera button processing
		if (keyCode == KeyEvent.KEYCODE_CAMERA
				|| keyCode == KeyEvent.KEYCODE_DPAD_CENTER) {
			MainScreen.guiManager.onHardwareShutterButtonPressed();
			return true;
		}
		//focus/half-press button processing
		if (keyCode == KeyEvent.KEYCODE_FOCUS) {
			MainScreen.guiManager.onHardwareFocusButtonPressed();
			return true;
		}
		
		//check if Headset Hook button has some functions except standard
		if ( keyCode == KeyEvent.KEYCODE_HEADSETHOOK )
		{
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(MainScreen.mainContext);
			boolean headsetFunc = prefs.getBoolean("headsetPrefCommon", false);
			if (headsetFunc)
			{
				MainScreen.guiManager.onHardwareFocusButtonPressed();
				MainScreen.guiManager.onHardwareShutterButtonPressed();
				return true;
			}
		}
		
		//check if volume button has some functions except Zoom-ing
		if ( keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP )
		{
			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(MainScreen.mainContext);
			int buttonFunc = Integer.parseInt(prefs.getString("volumeButtonPrefCommon", "0"));
			if (buttonFunc == VOLUME_FUNC_SHUTTER)
			{
				MainScreen.guiManager.onHardwareFocusButtonPressed();
				MainScreen.guiManager.onHardwareShutterButtonPressed();
				return true;
			}
			else if (buttonFunc == VOLUME_FUNC_EXPO)
			{
				MainScreen.guiManager.onVolumeBtnExpo(keyCode);
				return true;
			}
			else if (buttonFunc == VOLUME_FUNC_NONE)
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
		//-+- -->
		
		if (super.onKeyDown(keyCode, event))
			return true;
		return false;
	}

	@Override
	public void onClick(View v) {
		if (mApplicationStarted)
			MainScreen.guiManager.onClick(v);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event) {
		if (mApplicationStarted)
			return MainScreen.guiManager.onTouch(view, event);
		return true;
	}

	public boolean onTouchSuper(View view, MotionEvent event) {
		return super.onTouchEvent(event);
	}

	public void onButtonClick(View v) {
		MainScreen.guiManager.onButtonClick(v);
	}
	
	@Override
	public void onShutter()
	{
		PluginManager.getInstance().onShutter();
	}

	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera) 
	{
		
		camera.setPreviewCallbackWithBuffer(MainScreen.thiz);
		camera.addCallbackBuffer(pviewBuffer);
		
		PluginManager.getInstance().onPictureTaken(paramArrayOfByte,
				paramCamera);
		MainScreen.mCaptureState = MainScreen.CAPTURE_STATE_IDLE;
	}

	@Override
	public void onAutoFocus(boolean focused, Camera paramCamera) {
		Log.e("", "onAutoFocus call");
		PluginManager.getInstance().onAutoFocus(focused, paramCamera);
		if (focused)
			setFocusState(MainScreen.FOCUS_STATE_FOCUSED);
		else
			setFocusState(MainScreen.FOCUS_STATE_FAIL);
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera) {
		PluginManager.getInstance().onPreviewFrame(data, paramCamera);
		camera.addCallbackBuffer(pviewBuffer);
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
	public boolean handleMessage(Message msg) {

		if (msg.what == MSG_RETURN_CAPTURED) {
			this.setResult(RESULT_OK);
			this.finish();
			return true;
		}
		PluginManager.getInstance().handleMessage(msg);

		return true;
	}

	public void menuButtonPressed() {
		PluginManager.getInstance().menuButtonPressed();
	}

	public void disableCameraParameter(GUI.CameraParameter iParam,
			boolean bDisable, boolean bInitMenu) {
		guiManager.disableCameraParameter(iParam, bDisable, bInitMenu);
	}

	public void showOpenGLLayer(final int version)
	{
		if (glView == null)
		{
			glView = new GLLayer(MainScreen.mainContext, version);// (GLLayer)findViewById(R.id.SurfaceView02);
			glView.setLayoutParams(new LayoutParams(
					LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT));
			((RelativeLayout)this.findViewById(R.id.mainLayout2)).addView(glView, 1);
			glView.setZOrderMediaOverlay(true);
		}
	}

	public void hideOpenGLLayer()
	{
		if (glView != null)
		{
			glView.onPause();
			((RelativeLayout)this.findViewById(R.id.mainLayout2)).removeView(glView);
			glView = null;
		}
	}

	public void PlayShutter(int sound) {
		if (!MainScreen.ShutterPreference) {
			MediaPlayer mediaPlayer = MediaPlayer
					.create(MainScreen.thiz, sound);
			mediaPlayer.start();
		}
	}

	public void PlayShutter() {
		if (!MainScreen.ShutterPreference) {
			if (shutterPlayer != null)
				shutterPlayer.play();
		}
	}

	// set TRUE to mute and FALSE to unmute
	public void MuteShutter(boolean mute) {
		if (MainScreen.ShutterPreference) {
			AudioManager mgr = (AudioManager) MainScreen.thiz
					.getSystemService(MainScreen.mainContext.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, mute);
		}
	}

	public static int getImageWidth() {
		return imageWidth;
	}

	public static void setImageWidth(int setImageWidth) {
		imageWidth = setImageWidth;
	}

	public static int getImageHeight() {
		return imageHeight;
	}

	public static void setImageHeight(int setImageHeight) {
		imageHeight = setImageHeight;
	}

	public static int getSaveImageWidth() {
		return saveImageWidth;
	}

	public static void setSaveImageWidth(int setSaveImageWidth) {
		saveImageWidth = setSaveImageWidth;
	}

	public static int getSaveImageHeight() {
		return saveImageHeight;
	}

	public static void setSaveImageHeight(int setSaveImageHeight) {
		saveImageHeight = setSaveImageHeight;
	}

	public static boolean getCameraMirrored() {
		return CameraMirrored;
	}

	public static void setCameraMirrored(boolean setCameraMirrored) {
		CameraMirrored = setCameraMirrored;
	}

	public static boolean getWantLandscapePhoto() {
		return wantLandscapePhoto;
	}

	public static void setWantLandscapePhoto(boolean setWantLandscapePhoto) {
		wantLandscapePhoto = setWantLandscapePhoto;
	}

	public static void setFocusState(int state) {
		if (state != MainScreen.FOCUS_STATE_IDLE
				&& state != MainScreen.FOCUS_STATE_FOCUSED
				&& state != MainScreen.FOCUS_STATE_FAIL)
			return;

		MainScreen.mFocusState = state;

		Message msg = new Message();
		msg.what = PluginManager.MSG_BROADCAST;
		msg.arg1 = PluginManager.MSG_FOCUS_STATE_CHANGED;
		H.sendMessage(msg);
	}

	public static int getFocusState() {
		return MainScreen.mFocusState;
	}
	
	public void setScreenBrightness(boolean setMax)
	{
		//ContentResolver cResolver = getContentResolver();
		Window window = getWindow();
		
		WindowManager.LayoutParams layoutpars = window.getAttributes();
		
        //Set the brightness of this window	
		if(setMax)
			layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_FULL;
		else
			layoutpars.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;

        //Apply attribute changes to this window
        window.setAttributes(layoutpars);
	}

	/*******************************************************/
	/************************ Billing ************************/

	private boolean showStore = false;
// <!-- -+-
	OpenIabHelper mHelper;
	
	private boolean bOnSale = false;
	private boolean couponSale = false;
	
	private boolean unlockAllPurchased = false;
	private boolean hdrPurchased = false;
	private boolean panoramaPurchased = false;
	private boolean objectRemovalBurstPurchased = false;
	private boolean groupShotPurchased = false;

	
	static final String SKU_HDR = "plugin_almalence_hdr";
	static final String SKU_PANORAMA = "plugin_almalence_panorama";
	static final String SKU_UNLOCK_ALL = "unlock_all_forever";
	static final String SKU_UNLOCK_ALL_COUPON = "unlock_all_forever_coupon";
	static final String SKU_MOVING_SEQ = "plugin_almalence_moving_burst";
	static final String SKU_GROUPSHOT = "plugin_almalence_groupshot";
	
	static final String SKU_SALE1 = "abc_sale_controller1";
	static final String SKU_SALE2 = "abc_sale_controller2";
	
	static {
		//Yandex store
        OpenIabHelper.mapSku(SKU_HDR, "com.yandex.store", "plugin_almalence_hdr");
        OpenIabHelper.mapSku(SKU_PANORAMA, "com.yandex.store", "plugin_almalence_panorama");
        OpenIabHelper.mapSku(SKU_UNLOCK_ALL, "com.yandex.store", "unlock_all_forever");
        OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON, "com.yandex.store", "unlock_all_forever_coupon");
        OpenIabHelper.mapSku(SKU_MOVING_SEQ, "com.yandex.store", "plugin_almalence_moving_burst");
        OpenIabHelper.mapSku(SKU_GROUPSHOT, "com.yandex.store", "plugin_almalence_groupshot");
        
        OpenIabHelper.mapSku(SKU_SALE1, "com.yandex.store", "abc_sale_controller1");
        OpenIabHelper.mapSku(SKU_SALE2, "com.yandex.store", "abc_sale_controller2");
        
        //Amazon store
        OpenIabHelper.mapSku(SKU_HDR, OpenIabHelper.NAME_AMAZON, "plugin_almalence_hdr_amazon");
        OpenIabHelper.mapSku(SKU_PANORAMA, OpenIabHelper.NAME_AMAZON, "plugin_almalence_panorama_amazon");
        OpenIabHelper.mapSku(SKU_UNLOCK_ALL, OpenIabHelper.NAME_AMAZON, "unlock_all_forever_amazon");
        OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON, OpenIabHelper.NAME_AMAZON, "unlock_all_forever_coupon_amazon");
        OpenIabHelper.mapSku(SKU_MOVING_SEQ, OpenIabHelper.NAME_AMAZON, "plugin_almalence_moving_burst_amazon");
        OpenIabHelper.mapSku(SKU_GROUPSHOT, OpenIabHelper.NAME_AMAZON, "plugin_almalence_groupshot_amazon");
        
        OpenIabHelper.mapSku(SKU_SALE1, OpenIabHelper.NAME_AMAZON, "abc_sale_controller1_amazon");
        OpenIabHelper.mapSku(SKU_SALE2, OpenIabHelper.NAME_AMAZON, "abc_sale_controller2_amazon");
        
        
        //Samsung store
//        OpenIabHelper.mapSku(SKU_HDR, OpenIabHelper.NAME_SAMSUNG, "100000103369/000001018387");
//        OpenIabHelper.mapSku(SKU_PANORAMA, OpenIabHelper.NAME_SAMSUNG, "100000103369/000001018389");
//        OpenIabHelper.mapSku(SKU_UNLOCK_ALL, OpenIabHelper.NAME_SAMSUNG, "100000103369/000001017613");
//        OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON, OpenIabHelper.NAME_SAMSUNG, "100000103369/000001018392");
//        OpenIabHelper.mapSku(SKU_MOVING_SEQ, OpenIabHelper.NAME_SAMSUNG, "100000103369/000001018391");
//        OpenIabHelper.mapSku(SKU_GROUPSHOT, OpenIabHelper.NAME_SAMSUNG, "100000103369/000001018384");
//        
//        OpenIabHelper.mapSku(SKU_SALE1, OpenIabHelper.NAME_SAMSUNG, "100000103369/000001018393");
//        OpenIabHelper.mapSku(SKU_SALE2, OpenIabHelper.NAME_SAMSUNG, "100000103369/000001018394");
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
	
	private void createBillingHandler() 
	{
		try {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
			if ((isInstalled("com.almalence.hdr_plus")) || (isInstalled("com.almalence.pixfix")))
			{
				hdrPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_hdr", true);
				prefsEditor.commit();
			}
			if (isInstalled("com.almalence.panorama.smoothpanorama"))
			{
				panoramaPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_panorama", true);
				prefsEditor.commit();
			}
	
			String base64EncodedPublicKeyGoogle = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnztuXLNughHjGW55Zlgicr9r5bFP/K5DBc3jYhnOOo1GKX8M2grd7+SWeUHWwQk9lgQKat/ITESoNPE7ma0ZS1Qb/VfoY87uj9PhsRdkq3fg+31Q/tv5jUibSFrJqTf3Vmk1l/5K0ljnzX4bXI0p1gUoGd/DbQ0RJ3p4Dihl1p9pJWgfI9zUzYfvk2H+OQYe5GAKBYQuLORrVBbrF/iunmPkOFN8OcNjrTpLwWWAcxV5k0l5zFPrPVtkMZzKavTVWZhmzKNhCvs1d8NRwMM7XMejzDpI9A7T9egl6FAN4rRNWqlcZuGIMVizJJhvOfpCLtY971kQkYNXyilD40fefwIDAQAB";
			String base64EncodedPublicKeyYandex = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6KzaraKmv48Y+Oay2ZpWu4BHtSKYZidyCxbaYZmmOH4zlRNic/PDze7OA4a1buwdrBg3AAHwfVbHFzd9o91yinnHIWYQqyPg7L1Swh5W70xguL4jlF2N/xI9VoL4vMRv3Bf/79VfQ11utcPLHEXPR8nPEp9PT0wN2Hqp4yCWFbfvhVVmy7sQjywnfLqcWTcFCT6N/Xdxs1quq0hTE345MiCgkbh1xVULmkmZrL0rWDVCaxfK4iZWSRgQJUywJ6GMtUh+FU6/7nXDenC/vPHqnDR0R6BRi+QsES0ZnEfQLqNJoL+rqJDr/sDIlBQQDMQDxVOx0rBihy/FlHY34UF+bwIDAQAB";
			// Create the helper, passing it our context and the public key to
			// verify signatures with
			Log.v("Main billing", "Creating IAB helper.");
			Map<String, String> storeKeys = new HashMap<String, String>();
	        storeKeys.put(OpenIabHelper.NAME_GOOGLE, base64EncodedPublicKeyGoogle);
	        storeKeys.put("com.yandex.store", base64EncodedPublicKeyYandex);
			mHelper = new OpenIabHelper(this, storeKeys);
	
			mHelper.enableDebugLogging(true);
	
			Log.v("Main billing", "Starting setup.");
			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener() {
				public void onIabSetupFinished(IabResult result) 
				{
					try {
						Log.v("Main billing", "Setup finished.");
		
						if (!result.isSuccess()) {
							Log.v("Main billing", "Problem setting up in-app billing: "
									+ result);
							return;
						}
		
						List<String> additionalSkuList = new ArrayList<String>();
						additionalSkuList.add(SKU_HDR);
						additionalSkuList.add(SKU_PANORAMA);
						additionalSkuList.add(SKU_UNLOCK_ALL);
						additionalSkuList.add(SKU_UNLOCK_ALL_COUPON);
						additionalSkuList.add(SKU_MOVING_SEQ);
						additionalSkuList.add(SKU_GROUPSHOT);
						
						//for sale
						additionalSkuList.add(SKU_SALE1);
						additionalSkuList.add(SKU_SALE2);
		
						Log.v("Main billing", "Setup successful. Querying inventory.");
						mHelper.queryInventoryAsync(true, additionalSkuList,
								mGotInventoryListener);
					} catch (Exception e) {
						e.printStackTrace();
						Log.e("Main billing",
								"onIabSetupFinished exception: " + e.getMessage());
					}
				}
			});
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Main billing",
					"createBillingHandler exception: " + e.getMessage());
		}
	}

	private void destroyBillingHandler() {
		try {
			if (mHelper != null)
				mHelper.dispose();
			mHelper = null;
		} catch (Exception e) {
			e.printStackTrace();
			Log.e("Main billing",
					"destroyBillingHandler exception: " + e.getMessage());
		}
	}

	public String titleUnlockAll = "$6.95";
	public String titleUnlockAllCoupon = "$3.95";
	public String titleUnlockHDR = "$2.99";
	public String titleUnlockPano = "$2.99";
	public String titleUnlockMoving = "$2.99";
	public String titleUnlockGroup = "$2.99";
	
	public String summaryUnlockAll = "";
	public String summaryUnlockHDR = "";
	public String summaryUnlockPano = "";
	public String summaryUnlockMoving = "";
	public String summaryUnlockGroup = "";
	
	IabHelper.QueryInventoryFinishedListener mGotInventoryListener = new IabHelper.QueryInventoryFinishedListener() {
		public void onQueryInventoryFinished(IabResult result,
				Inventory inventory) {
			if (inventory == null)
			{
				Log.e("Main billing", "mGotInventoryListener inventory null ");
				return;
			}

			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(MainScreen.mainContext);
			
			if (inventory.hasPurchase(SKU_HDR)) {
				hdrPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_hdr", true);
				prefsEditor.commit();
			}
			if (inventory.hasPurchase(SKU_PANORAMA)) {
				panoramaPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_panorama", true);
				prefsEditor.commit();
			}
			if (inventory.hasPurchase(SKU_UNLOCK_ALL)) {
				unlockAllPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("unlock_all_forever", true);
				prefsEditor.commit();
			}
			if (inventory.hasPurchase(SKU_UNLOCK_ALL_COUPON)) {
				unlockAllPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("unlock_all_forever", true);
				prefsEditor.commit();
			}
			if (inventory.hasPurchase(SKU_MOVING_SEQ)) {
				objectRemovalBurstPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_moving_burst", true);
				prefsEditor.commit();
			}
			if (inventory.hasPurchase(SKU_GROUPSHOT)) {
				groupShotPurchased = true;
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_groupshot", true);
				prefsEditor.commit();
			}
			
			try{
				
				String[] separated = inventory.getSkuDetails(SKU_SALE1).getPrice().split(",");
				int price1 = Integer.valueOf(separated[0]);
				String[] separated2 = inventory.getSkuDetails(SKU_SALE2).getPrice().split(",");
				int price2 = Integer.valueOf(separated2[0]);
				
				if(price1<price2)
					bOnSale = true;
				else
					bOnSale = false;
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("bOnSale", bOnSale);
				prefsEditor.commit();
				
				Log.e("Main billing SALE", "Sale status is " + bOnSale);
			}
			catch(Exception e)
			{
				Log.e("Main billing SALE", "No sale data available");
				bOnSale = false;
			}
			
			try{
				titleUnlockAll = inventory.getSkuDetails(SKU_UNLOCK_ALL).getPrice();
				titleUnlockAllCoupon = inventory.getSkuDetails(SKU_UNLOCK_ALL_COUPON).getPrice();
				titleUnlockHDR = inventory.getSkuDetails(SKU_HDR).getPrice();
				titleUnlockPano = inventory.getSkuDetails(SKU_PANORAMA).getPrice();
				titleUnlockMoving = inventory.getSkuDetails(SKU_MOVING_SEQ).getPrice();
				titleUnlockGroup = inventory.getSkuDetails(SKU_GROUPSHOT).getPrice();
				
				summaryUnlockAll = inventory.getSkuDetails(SKU_UNLOCK_ALL).getDescription();
				summaryUnlockHDR = inventory.getSkuDetails(SKU_HDR).getDescription();
				summaryUnlockPano = inventory.getSkuDetails(SKU_PANORAMA).getDescription();
				summaryUnlockMoving = inventory.getSkuDetails(SKU_MOVING_SEQ).getDescription();
				summaryUnlockGroup = inventory.getSkuDetails(SKU_GROUPSHOT).getDescription();
			}catch(Exception e)
			{
				Log.e("Market!!!!!!!!!!!!!!!!!!!!!!!", "Error Getting data for store!!!!!!!!");
			}
		}
	};

	private int HDR_REQUEST = 100;
	private int PANORAMA_REQUEST = 101;
	private int ALL_REQUEST = 102;
	private int OBJECTREM_BURST_REQUEST = 103;
	private int GROUPSHOT_REQUEST = 104;
//	Preference hdrPref, panoramaPref, allPref, objectremovalPref,
//			groupshotPref;

//	public void onBillingPreferenceCreate(final PreferenceFragment prefActivity) {
//		allPref = prefActivity.findPreference("purchaseAll");
//		
//		if (titleUnlockAll!=null && titleUnlockAll != "")
//		{
//			String title = getResources().getString(R.string.Pref_Upgrde_All_Preference_Title) + ": " + titleUnlockAll;
//			allPref.setTitle(title);
//		}
//		if (summaryUnlockAll!=null && summaryUnlockAll != "")
//		{
//			String summary = summaryUnlockAll + " " + getResources().getString(R.string.Pref_Upgrde_All_Preference_Summary);
//			allPref.setSummary(summary);
//		}
//		
//		allPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			public boolean onPreferenceClick(Preference preference) {
//				// generate payload to identify user....????
//				String payload = "";
//				try {
//					mHelper.launchPurchaseFlow(MainScreen.thiz,
//							SKU_UNLOCK_ALL, ALL_REQUEST,
//							mPreferencePurchaseFinishedListener, payload);
//				} catch (Exception e) {
//					e.printStackTrace();
//					Log.e("Main billing", "Purchase result " + e.getMessage());
//					Toast.makeText(MainScreen.thiz,
//							"Error during purchase " + e.getMessage(),
//							Toast.LENGTH_LONG).show();
//				}
//
//				prefActivity.getActivity().finish();
//				Preferences.closePrefs();
//				return true;
//			}
//		});
//		if (unlockAllPurchased) {
//			allPref.setEnabled(false);
//			allPref.setSummary(R.string.already_unlocked);
//
//			hdrPurchased = true;
//			panoramaPurchased = true;
//			objectRemovalBurstPurchased = true;
//			groupShotPurchased = true;
//		}
//
//		hdrPref = prefActivity.findPreference("hdrPurchase");
//		
//		if (titleUnlockHDR!=null && titleUnlockHDR != "")
//		{
//			String title = getResources().getString(R.string.Pref_Upgrde_HDR_Preference_Title) + ": " + titleUnlockHDR;
//			hdrPref.setTitle(title);
//		}
//		if (summaryUnlockHDR!=null && summaryUnlockHDR != "")
//		{
//			String summary = summaryUnlockHDR + " " + getResources().getString(R.string.Pref_Upgrde_HDR_Preference_Summary);
//			hdrPref.setSummary(summary);
//		}
//		
//		hdrPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//			public boolean onPreferenceClick(Preference preference) {
//				// generate payload to identify user....????
//				String payload = "";
//				try {
//					mHelper.launchPurchaseFlow(MainScreen.thiz,
//							SKU_HDR, HDR_REQUEST,
//							mPreferencePurchaseFinishedListener, payload);
//				} catch (Exception e) {
//					e.printStackTrace();
//					Log.e("Main billing", "Purchase result " + e.getMessage());
//					Toast.makeText(MainScreen.thiz,
//							"Error during purchase " + e.getMessage(),
//							Toast.LENGTH_LONG).show();
//				}
//
//				prefActivity.getActivity().finish();
//				Preferences.closePrefs();
//				return true;
//			}
//		});
//
//		if (hdrPurchased) {
//			hdrPref.setEnabled(false);
//			hdrPref.setSummary(R.string.already_unlocked);
//		}
//
//		panoramaPref = prefActivity.findPreference("panoramaPurchase");
//		
//		if (titleUnlockPano!=null && titleUnlockPano != "")
//		{
//			String title = getResources().getString(R.string.Pref_Upgrde_Panorama_Preference_Title) + ": " + titleUnlockPano;
//			panoramaPref.setTitle(title);
//		}
//		if (summaryUnlockPano!=null && summaryUnlockPano != "")
//		{
//			String summary = summaryUnlockPano + " " + getResources().getString(R.string.Pref_Upgrde_Panorama_Preference_Summary) ;
//			panoramaPref.setSummary(summary);
//		}
//		
//		panoramaPref
//				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//					public boolean onPreferenceClick(Preference preference) {
//						// generate payload to identify user....????
//						String payload = "";
//						try {
//							mHelper.launchPurchaseFlow(MainScreen.thiz,
//									SKU_PANORAMA,
//									PANORAMA_REQUEST,
//									mPreferencePurchaseFinishedListener,
//									payload);
//						} catch (Exception e) {
//							e.printStackTrace();
//							Log.e("Main billing",
//									"Purchase result " + e.getMessage());
//							Toast.makeText(MainScreen.thiz,
//									"Error during purchase " + e.getMessage(),
//									Toast.LENGTH_LONG).show();
//						}
//
//						prefActivity.getActivity().finish();
//						Preferences.closePrefs();
//						return true;
//					}
//				});
//		if (panoramaPurchased) {
//			panoramaPref.setEnabled(false);
//			panoramaPref.setSummary(R.string.already_unlocked);
//		}
//
//		objectremovalPref = prefActivity.findPreference("movingPurchase");
//		
//		if (titleUnlockMoving!=null && titleUnlockMoving != "")
//		{
//			String title = getResources().getString(R.string.Pref_Upgrde_Moving_Preference_Title) + ": " + titleUnlockMoving;
//			objectremovalPref.setTitle(title);
//		}
//		if (summaryUnlockMoving!=null && summaryUnlockMoving != "")
//		{
//			String summary = summaryUnlockMoving + " " + getResources().getString(R.string.Pref_Upgrde_Moving_Preference_Summary);
//			objectremovalPref.setSummary(summary);
//		}
//		
//		objectremovalPref
//				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//					public boolean onPreferenceClick(Preference preference) {
//						// generate payload to identify user....????
//						String payload = "";
//						try {
//							mHelper.launchPurchaseFlow(MainScreen.thiz,
//									SKU_MOVING_SEQ,
//									OBJECTREM_BURST_REQUEST,
//									mPreferencePurchaseFinishedListener,
//									payload);
//						} catch (Exception e) {
//							e.printStackTrace();
//							Log.e("Main billing",
//									"Purchase result " + e.getMessage());
//							Toast.makeText(MainScreen.thiz,
//									"Error during purchase " + e.getMessage(),
//									Toast.LENGTH_LONG).show();
//						}
//
//						prefActivity.getActivity().finish();
//						Preferences.closePrefs();
//						return true;
//					}
//				});
//		if (objectRemovalBurstPurchased) {
//			objectremovalPref.setEnabled(false);
//			objectremovalPref.setSummary(R.string.already_unlocked);
//		}
//
//		groupshotPref = prefActivity.findPreference("groupPurchase");
//		
//		if (titleUnlockGroup!=null && titleUnlockGroup != "")
//		{
//			String title = getResources().getString(R.string.Pref_Upgrde_Groupshot_Preference_Title) + ": " + titleUnlockGroup;
//			groupshotPref.setTitle(title);
//		}
//		if (summaryUnlockGroup!=null && summaryUnlockGroup != "")
//		{
//			String summary = summaryUnlockGroup + " " + getResources().getString(R.string.Pref_Upgrde_Groupshot_Preference_Summary);
//			groupshotPref.setSummary(summary);
//		}
//		
//		groupshotPref
//				.setOnPreferenceClickListener(new OnPreferenceClickListener() {
//					public boolean onPreferenceClick(Preference preference) {
//						String payload = "";
//						try {
//							mHelper.launchPurchaseFlow(MainScreen.thiz,
//									SKU_GROUPSHOT,
//									GROUPSHOT_REQUEST,
//									mPreferencePurchaseFinishedListener,
//									payload);
//						} catch (Exception e) {
//							e.printStackTrace();
//							Log.e("Main billing",
//									"Purchase result " + e.getMessage());
//							Toast.makeText(MainScreen.thiz,
//									"Error during purchase " + e.getMessage(),
//									Toast.LENGTH_LONG).show();
//						}
//
//						prefActivity.getActivity().finish();
//						Preferences.closePrefs();
//						return true;
//					}
//				});
//		if (groupShotPurchased) {
//			groupshotPref.setEnabled(false);
//			groupshotPref.setSummary(R.string.already_unlocked);
//		}
//	}

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
	
	public void purchaseAll()
	{
		if(MainScreen.thiz.isPurchasedAll())
			return;
		String payload = "";
		try 
		{
			//guiManager.hideStore();
			mHelper.launchPurchaseFlow(MainScreen.thiz,
					isCouponSale()?SKU_UNLOCK_ALL_COUPON:SKU_UNLOCK_ALL, ALL_REQUEST,
					mPreferencePurchaseFinishedListener, payload);
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz,
					"Error during purchase " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	
	public void purchaseHDR()
	{
		if(MainScreen.thiz.isPurchasedHDR() || MainScreen.thiz.isPurchasedAll())
			return;
		String payload = "";
		try 
		{
			//guiManager.hideStore();
			mHelper.launchPurchaseFlow(MainScreen.thiz,
					SKU_HDR, HDR_REQUEST,
					mPreferencePurchaseFinishedListener, payload);
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz,
					"Error during purchase " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	
	public void purchasePanorama()
	{
		if(MainScreen.thiz.isPurchasedPanorama() || MainScreen.thiz.isPurchasedAll())
			return;
		String payload = "";
		try 
		{
			//guiManager.hideStore();
			mHelper.launchPurchaseFlow(MainScreen.thiz,
					SKU_PANORAMA,
					PANORAMA_REQUEST,
					mPreferencePurchaseFinishedListener,
					payload);
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz,
					"Error during purchase " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	
	public void purchaseMoving()
	{
		if(MainScreen.thiz.isPurchasedMoving() || MainScreen.thiz.isPurchasedAll())
			return;
		String payload = "";
		try 
		{
			//guiManager.hideStore();
			mHelper.launchPurchaseFlow(MainScreen.thiz,
					SKU_MOVING_SEQ,
					OBJECTREM_BURST_REQUEST,
					mPreferencePurchaseFinishedListener,
					payload);
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz,
					"Error during purchase " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	
	public void purchaseGroupshot()
	{
		if(MainScreen.thiz.isPurchasedGroupshot() || MainScreen.thiz.isPurchasedAll())
			return;
		String payload = "";
		try 
		{
			//guiManager.hideStore();
			mHelper.launchPurchaseFlow(MainScreen.thiz,
					SKU_GROUPSHOT,
					GROUPSHOT_REQUEST,
					mPreferencePurchaseFinishedListener,
					payload);
		}
		catch (Exception e) {
			e.printStackTrace();
			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(MainScreen.thiz,
					"Error during purchase " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}
	
//	public boolean showUnlock = false;
	// Callback for when purchase from preferences is finished
	IabHelper.OnIabPurchaseFinishedListener mPreferencePurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			showStore = true;
			Log.v("Main billing", "Purchase finished: " + result
					+ ", purchase: " + purchase);
			if (result.isFailure()) {
				Log.v("Main billing", "Error purchasing: " + result);
//				new CountDownTimer(100, 100) {
//					public void onTick(long millisUntilFinished) {
//					}
//
//					public void onFinish() {
//						showUnlock = true;
//						Intent intent = new Intent(MainScreen.thiz,
//								Preferences.class);
//						startActivity(intent);
//					}
//				}.start();
				return;
			}

			SharedPreferences prefs = PreferenceManager
					.getDefaultSharedPreferences(MainScreen.mainContext);
			
			Log.v("Main billing", "Purchase successful.");

			if (purchase.getSku().equals(SKU_HDR)) {
				Log.v("Main billing", "Purchase HDR.");

				hdrPurchased = true;
//				hdrPref.setEnabled(false);
//				hdrPref.setSummary(R.string.already_unlocked);
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_hdr", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_PANORAMA)) {
				Log.v("Main billing", "Purchase Panorama.");

				panoramaPurchased = true;
//				panoramaPref.setEnabled(false);
//				panoramaPref.setSummary(R.string.already_unlocked);
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_panorama", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_UNLOCK_ALL)) {
				Log.v("Main billing", "Purchase all.");

				unlockAllPurchased = true;
//				allPref.setEnabled(false);
//				allPref.setSummary(R.string.already_unlocked);
//
//				groupshotPref.setEnabled(false);
//				groupshotPref.setSummary(R.string.already_unlocked);
//
//				objectremovalPref.setEnabled(false);
//				objectremovalPref.setSummary(R.string.already_unlocked);
//
//				panoramaPref.setEnabled(false);
//				panoramaPref.setSummary(R.string.already_unlocked);
//
//				hdrPref.setEnabled(false);
//				hdrPref.setSummary(R.string.already_unlocked);
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("unlock_all_forever", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_UNLOCK_ALL_COUPON)) {
				Log.v("Main billing", "Purchase all coupon.");

				unlockAllPurchased = true;
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("unlock_all_forever", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_MOVING_SEQ)) {
				Log.v("Main billing", "Purchase object removal.");

				objectRemovalBurstPurchased = true;
//				objectremovalPref.setEnabled(false);
//				objectremovalPref.setSummary(R.string.already_unlocked);
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_moving_burst", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_GROUPSHOT)) {
				Log.v("Main billing", "Purchase groupshot.");

				groupShotPurchased = true;
//				groupshotPref.setEnabled(false);
//				groupshotPref.setSummary(R.string.already_unlocked);
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_groupshot", true);
				prefsEditor.commit();
			}
		}
		
	};

	public void launchPurchase(String SKU, int requestID) {
		String payload = "";
		try {
			guiManager.showStore();
//			mHelper.launchPurchaseFlow(MainScreen.thiz, SKU, requestID,
//					mPurchaseFinishedListener, payload);
		} catch (Exception e) {
			e.printStackTrace();
//			Log.e("Main billing", "Purchase result " + e.getMessage());
			Toast.makeText(this, "Error during purchase " + e.getMessage(),
					Toast.LENGTH_LONG).show();
		}
	}

	IabHelper.OnIabPurchaseFinishedListener mPurchaseFinishedListener = new IabHelper.OnIabPurchaseFinishedListener() {
		public void onIabPurchaseFinished(IabResult result, Purchase purchase) {
			
			guiManager.showStore();
			Log.v("Main billing", "Purchase finished: " + result
					+ ", purchase: " + purchase);
			if (result.isFailure()) {
				Log.v("Main billing", "Error purchasing: " + result);
				return;
			}

			Log.v("Main billing", "Purchase successful.");

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
			
			if (purchase.getSku().equals(SKU_HDR)) {
				Log.v("Main billing", "Purchase HDR.");
				hdrPurchased = true;
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_hdr", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_PANORAMA)) {
				Log.v("Main billing", "Purchase Panorama.");
				panoramaPurchased = true;
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_panorama", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_UNLOCK_ALL)) {
				Log.v("Main billing", "Purchase unlock_all_forever.");
				unlockAllPurchased = true;
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("unlock_all_forever", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_UNLOCK_ALL_COUPON)) {
				Log.v("Main billing", "Purchase unlock_all_forever_coupon.");
				unlockAllPurchased = true;
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("unlock_all_forever", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_MOVING_SEQ)) {
				Log.v("Main billing", "Purchase plugin_almalence_moving_burst.");
				objectRemovalBurstPurchased = true;
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_moving_burst", true);
				prefsEditor.commit();
			}
			if (purchase.getSku().equals(SKU_GROUPSHOT)) {
				Log.v("Main billing", "Purchase plugin_almalence_groupshot.");
				groupShotPurchased = true;
				
				Editor prefsEditor = prefs.edit();
				prefsEditor.putBoolean("plugin_almalence_groupshot", true);
				prefsEditor.commit();
			}

			
		}
	};

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.v("Main billing", "onActivityResult(" + requestCode + ","
				+ resultCode + "," + data);

		// Pass on the activity result to the helper for handling
		if (!mHelper.handleActivityResult(requestCode, resultCode, data)) {
			// not handled, so handle it ourselves (here's where you'd
			// perform any handling of activity results not related to in-app
			// billing...
			super.onActivityResult(requestCode, resultCode, data);
		} else {
			Log.v("Main billing", "onActivityResult handled by IABUtil.");
		}
	}
	
		// next methods used to store number of free launches.
	// using files to store this info

	// returns number of launches left
	public int getLeftLaunches(String modeID) {
		String dirPath = getFilesDir().getAbsolutePath() + File.separator
				+ modeID;
		File projDir = new File(dirPath);
		if (!projDir.exists()) {
			projDir.mkdirs();
			WriteLaunches(projDir, 30);
		}
		int left = ReadLaunches(projDir);
		return left;
	}

	// decrements number of launches left
	public void decrementLeftLaunches(String modeID) {
		String dirPath = getFilesDir().getAbsolutePath() + File.separator
				+ modeID;
		File projDir = new File(dirPath);
		if (!projDir.exists()) {
			projDir.mkdirs();
			WriteLaunches(projDir, 30);
		}

		int left = ReadLaunches(projDir);
		if (left > 0)
			WriteLaunches(projDir, left - 1);
	}

	// writes number of launches left into memory
	private void WriteLaunches(File projDir, int left) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(projDir + "/left");
			fos.write(left);
			fos.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	// reads number of launches left from memory
	private int ReadLaunches(File projDir) {
		int left = 0;
		FileInputStream fis = null;
		try {
			fis = new FileInputStream(projDir + "/left");
			left = fis.read();
			fis.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return left;
	}

	public boolean checkLaunches(Mode mode) {
		// if mode free
		if (mode.SKU == null)
			return true;
		if (mode.SKU.isEmpty())
			return true;

		// if all unlocked
		if (unlockAllPurchased == true)
			return true;

		// if current mode unlocked
		if (mode.SKU.equals("plugin_almalence_hdr")) {
			if (hdrPurchased == true)
				return true;
		} else if (mode.SKU.equals("plugin_almalence_panorama_augmented")) {
			if (panoramaPurchased == true)
				return true;
		} else if (mode.SKU.equals("plugin_almalence_moving_burst")) {
			if (objectRemovalBurstPurchased == true)
				return true;
		} else if (mode.SKU.equals("plugin_almalence_groupshot")) {
			if (groupShotPurchased == true)
				return true;
		}

		// if (!mode.purchased)
		{
			int launchesLeft = MainScreen.thiz.getLeftLaunches(mode.modeID);
			if (0 == launchesLeft)// no more launches left
			{
				// show appstore for this mode
				launchPurchase(mode.SKU, 100);
				return false;
			} else if ((10 == launchesLeft) || (20 == launchesLeft)
					|| (5 >= launchesLeft)) {
				// show appstore button and say that it cost money
				int id = MainScreen.thiz.getResources().getIdentifier(
						mode.modeName, "string",
						MainScreen.thiz.getPackageName());
				String modename = MainScreen.thiz.getResources().getString(id);

				String left = String.format(getResources().getString(R.string.Pref_Billing_Left),
						modename, launchesLeft);
				Toast toast = Toast.makeText(this, left, Toast.LENGTH_LONG);
				toast.setGravity(Gravity.CENTER, 0, 0);
				toast.show();
			}
		}
		return true;
	}

	private boolean isInstalled(String packageName) {
		PackageManager pm = getPackageManager();
		boolean installed = false;
		try {
			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
			installed = true;
		} catch (PackageManager.NameNotFoundException e) {
			installed = false;
		}
		return installed;
	}
	
// -+- -->
	
	/************************ Billing ************************/
	/*******************************************************/
	
// <!-- -+-
	
	//Application rater code
	public static void CallStoreFree(Activity act)
    {
    	try
    	{
        	Intent intent = new Intent(Intent.ACTION_VIEW);
       		intent.setData(Uri.parse("market://details?id=com.almalence.opencam"));
	        act.startActivity(intent);
    	}
    	catch(ActivityNotFoundException e)
    	{
    		return;
    	}
    }
// -+- -->
	
	//widget ad code
	public static void CallStoreWidgetInstall(Activity act)
    {
    	try
    	{
        	Intent intent = new Intent(Intent.ACTION_VIEW);
       		intent.setData(Uri.parse("market://details?id=com.almalence.opencamwidget"));
	        act.startActivity(intent);
    	}
    	catch(ActivityNotFoundException e)
    	{
    		return;
    	}
    }
	
	private void ResetOrSaveSettings()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		Editor prefsEditor = prefs.edit();
		boolean isSaving = prefs.getBoolean("SaveConfiguration_Mode", true);
		if (false == isSaving)
		{
			prefsEditor.putString("defaultModeName", "single");
			prefsEditor.commit();
		}
		
		isSaving = prefs.getBoolean("SaveConfiguration_ImageSize", true);
		if (false == isSaving)
		{			
			//general settings - image size
			prefsEditor.putString("imageSizePrefCommonBack", "-1");
			prefsEditor.putString("imageSizePrefCommonFront", "-1");
			//night hi sped image size
			prefsEditor.putString("imageSizePrefNightBack", "-1");
			prefsEditor.putString("pref_plugin_capture_panoramaaugmented_imageheight", "0");
			prefsEditor.commit();
		}
		
		isSaving = prefs.getBoolean("SaveConfiguration_SceneMode", true);
		if (false == isSaving)
		{			
			prefsEditor.putString(GUI.sSceneModePref, GUI.sDefaultValue);
			prefsEditor.commit();
		}
		
		isSaving = prefs.getBoolean("SaveConfiguration_FocusMode", true);
		if (false == isSaving)
		{			
			prefsEditor.putString("sRearFocusModePref", GUI.sDefaultFocusValue);
			prefsEditor.putString(GUI.sFrontFocusModePref, GUI.sDefaultFocusValue);
			prefsEditor.commit();
		}
		
		isSaving = prefs.getBoolean("SaveConfiguration_WBMode", true);
		if (false == isSaving)
		{			
			prefsEditor.putString(GUI.sWBModePref, GUI.sDefaultValue);
			prefsEditor.commit();
		}
		
		isSaving = prefs.getBoolean("SaveConfiguration_ISOMode", true);
		if (false == isSaving)
		{			
			prefsEditor.putString(GUI.sISOPref, GUI.sDefaultValue);
			prefsEditor.commit();
		}
		
		isSaving = prefs.getBoolean("SaveConfiguration_FlashMode", true);
		if (false == isSaving)
		{			
			prefsEditor.putString(GUI.sFlashModePref, GUI.sDefaultValue);
			prefsEditor.commit();
		}
		
		isSaving = prefs.getBoolean("SaveConfiguration_FrontRearCamera", true);
		if (false == isSaving)
		{			
			prefsEditor.putBoolean("useFrontCamera", false);
			prefsEditor.commit();
		}
		
		isSaving = prefs.getBoolean("SaveConfiguration_ExpoCompensation", true);
		if (false == isSaving)
		{			
			prefsEditor.putInt("EvCompensationValue", 0);
			prefsEditor.commit();
		}
	}
}
