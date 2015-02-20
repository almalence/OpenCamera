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
import android.content.res.Resources;
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
import android.util.DisplayMetrics;
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

import com.almalence.plugins.capture.panoramaaugmented.PanoramaAugmentedCapturePlugin;
import com.almalence.plugins.capture.video.VideoCapturePlugin;
import com.almalence.util.AppWidgetNotifier;
import com.almalence.util.Util;

//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.cameracontroller.HALv3;
import com.almalence.opencam.ui.AlmalenceGUI;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;

//-+- -->
/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 //import com.almalence.opencam_plus.cameracontroller.HALv3;
 import com.almalence.opencam_plus.ui.GLLayer;
 import com.almalence.opencam_plus.ui.GUI;
 +++ --> */

/***
 * MainScreen - main activity screen with camera functionality
 * 
 * Passes all main events to PluginManager
 ***/

@SuppressWarnings("deprecation")
abstract public class ApplicationScreen extends Activity implements ApplicationInterface, View.OnClickListener, View.OnTouchListener,
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

	protected static final int			MSG_RETURN_CAPTURED				= -1;
//
//	protected static final int			MODE_GENERAL					= 0;
//	protected static final int			MODE_SMART_MULTISHOT_AND_NIGHT	= 1;
//	protected static final int			MODE_PANORAMA					= 2;
//	protected static final int			MODE_VIDEO						= 3;
//
//	protected static final int			MIN_MPIX_PREVIEW				= 600 * 400;

	public static ApplicationScreen		thiz;
	public Context						mainContext;
	protected Handler					messageHandler;

	// Interface to HALv3 camera and Old style camera
	protected CameraController			cameraController				= null;

	protected int							captureFormat					= CameraController.JPEG;

	public GUI							guiManager						= null;

	// OpenGL layer. May be used to allow capture plugins to draw overlaying
	// preview, such as night vision or panorama frames.
	protected GLLayer						glView;

	protected boolean						mPausing						= false;

	protected SurfaceHolder				surfaceHolder;
	protected SurfaceView					preview;
	protected Surface						mCameraSurface					= null;
	protected OrientationEventListener	orientListener;
	protected boolean						landscapeIsNormal				= false;
	protected boolean						surfaceCreated					= false;

	protected int							surfaceWidth					= 0;
	protected int							surfaceHeight					= 0;
//	
	protected int							surfaceLayoutWidth				= 0;
	protected int							surfaceLayoutHeight				= 0;

	// shared between activities
	// protected int imageWidth, imageHeight;
	protected int							previewWidth, previewHeight;

	protected CountDownTimer				screenTimer						= null;
	protected boolean						isScreenTimerRunning			= false;

	protected static boolean				wantLandscapePhoto				= false;
	protected int							orientationMain					= 0;
	protected int							orientationMainPrevious			= 0;

	protected SoundPlayer					shutterPlayer					= null;

	// Common preferences
//	protected String						imageSizeIdxPreference;
//	protected String						multishotImageSizeIdxPreference;
//	protected boolean						shutterPreference				= true;
//	protected int							shotOnTapPreference				= 0;
//
//	protected boolean						showHelp						= false;
//
	protected boolean						keepScreenOn					= false;
//
//	protected String						saveToPath;
//	protected String						saveToPreference;
//	protected boolean						sortByDataPreference;
//
//	protected boolean						captureRAW;
//
	protected List<Surface>				surfaceList;
//
	protected static boolean				mAFLocked						= false;
//
//	// shows if mode is currently switching
//	protected boolean						switchingMode					= false;

	// >>Description
	// section with initialize, resume, start, stop procedures, preferences
	// access
	//
	// Initialize, stop etc depends on plugin type.
	//
	// Create main GUI controls and plugin specific controls.
	//
	// Description<<

	protected static boolean				isCreating						= false;
	protected static boolean				mApplicationStarted				= false;
	protected static boolean				mCameraStarted					= false;
	protected static boolean				isForceClose					= false;

	protected static final int			VOLUME_FUNC_SHUTTER				= 0;
	protected static final int			VOLUME_FUNC_EXPO				= 2;
	protected static final int			VOLUME_FUNC_NONE				= 3;

	protected static List<Area>			mMeteringAreaMatrix5			= new ArrayList<Area>();
	protected static List<Area>			mMeteringAreaMatrix4			= new ArrayList<Area>();
	protected static List<Area>			mMeteringAreaMatrix1			= new ArrayList<Area>();
	protected static List<Area>			mMeteringAreaCenter				= new ArrayList<Area>();
	protected static List<Area>			mMeteringAreaSpot				= new ArrayList<Area>();

	protected int						currentMeteringMode				= -1;

	public static String				sEvPref;
	public static String				sSceneModePref;
	public static String				sWBModePref;
	public static String				sFrontFocusModePref;
	public static String				sFrontFocusModeVideoPref;
	public static String				sRearFocusModePref;
	public static String				sRearFocusModeVideoPref;
	public static String				sFlashModePref;
	public static String				sISOPref;
	public static String				sMeteringModePref;

//	public static String				sDelayedCapturePref;
//	public static String				sShowDelayedCapturePref;
//	public static String				sDelayedSoundPref;
//	public static String				sDelayedFlashPref;
//	public static String				sDelayedCaptureIntervalPref;

//	public static String				sPhotoTimeLapseCaptureIntervalPref;
//	public static String				sPhotoTimeLapseCaptureIntervalMeasurmentPref;
//	public static String				sPhotoTimeLapseActivePref;
//	public static String				sPhotoTimeLapseIsRunningPref;
//	public static String				sPhotoTimeLapseCount;

	public static String				sUseFrontCameraPref;
//	protected static String				sShutterPref;
//	protected static String				sShotOnTapPref;
//	protected static String				sVolumeButtonPref;

	public static String				sImageSizeRearPref;
	public static String				sImageSizeFrontPref;

	public static String				sImageSizeMultishotBackPref;
	public static String				sImageSizeMultishotFrontPref;
//
//	public static String				sImageSizePanoramaBackPref;
//	public static String				sImageSizePanoramaFrontPref;
//
//	public static String				sImageSizeVideoBackPref;
//	public static String				sImageSizeVideoFrontPref;
//
//	public static String				sCaptureRAWPref;
//
//	public static String				sInitModeListPref				= "initModeListPref";
//
//	public static String				sJPEGQualityPref;
//	
	public static String				sAntibandingPref;
//	
	public static String				sAELockPref;
	public static String				sAWBLockPref;
//
//	public static String				sDefaultInfoSetPref;
//	public static String				sSWCheckedPref;
//	public static String				sSavePathPref;
//	public static String				sExportNamePref;
//	public static String				sExportNamePrefixPref;
//	public static String				sExportNamePostfixPref;
//	public static String				sSaveToPref;
//	public static String				sSortByDataPref;
//	public static String				sEnableExifOrientationTagPref;
//	public static String				sAdditionalRotationPref;
//
	public static String				sExpoPreviewModePref;
//
//	public static String				sDefaultModeName;
//
	public static int					sDefaultValue					= CameraParameters.SCENE_MODE_AUTO;
	public static int					sDefaultFocusValue				= CameraParameters.AF_MODE_CONTINUOUS_PICTURE;
	public static int					sDefaultFlashValue				= CameraParameters.FLASH_MODE_OFF;
	public static int					sDefaultMeteringValue			= CameraParameters.meteringModeAuto;

	
	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		
		sEvPref = getResources().getString(R.string.Preference_EvCompensationValue);
		sSceneModePref = getResources().getString(R.string.Preference_SceneModeValue);
		sWBModePref = getResources().getString(R.string.Preference_WBModeValue);
		sFrontFocusModePref = getResources().getString(R.string.Preference_FrontFocusModeValue);
		sFrontFocusModeVideoPref = getResources().getString(R.string.Preference_FrontFocusModeVideoValue);
		sRearFocusModePref = getResources().getString(R.string.Preference_RearFocusModeValue);
		sRearFocusModeVideoPref = getResources().getString(R.string.Preference_RearFocusModeVideoValue);
		sFlashModePref = getResources().getString(R.string.Preference_FlashModeValue);
		sISOPref = getResources().getString(R.string.Preference_ISOValue);
		sMeteringModePref = getResources().getString(R.string.Preference_MeteringModeValue);

		sUseFrontCameraPref = getResources().getString(R.string.Preference_UseFrontCameraValue);

		sImageSizeRearPref = getResources().getString(R.string.Preference_ImageSizeRearValue);
		sImageSizeFrontPref = getResources().getString(R.string.Preference_ImageSizeFrontValue);

		sImageSizeMultishotBackPref = getResources()
				.getString(R.string.Preference_ImageSizePrefSmartMultishotBackValue);
		sImageSizeMultishotFrontPref = getResources().getString(
				R.string.Preference_ImageSizePrefSmartMultishotFrontValue);
		
		sAntibandingPref = getResources().getString(R.string.Preference_AntibandingValue);

		sAELockPref = getResources().getString(R.string.Preference_AELockValue);
		sAWBLockPref = getResources().getString(R.string.Preference_AWBLockValue);

		sExpoPreviewModePref = getResources().getString(R.string.Preference_ExpoBracketingPreviewModePref);

		mainContext = this.getBaseContext();
		messageHandler = new Handler(this);
		thiz = this;
		
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// ensure landscape orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// set to fullscreen
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		
		// set some common view here
		setContentView(R.layout.opencamera_main_layout);
		
		duringOnCreate();
		
		try
		{
			cameraController = CameraController.getInstance();
		} catch (VerifyError exp)
		{
			Log.e("MainScreen", exp.getMessage());
		}
		CameraController.onCreate(ApplicationScreen.thiz, ApplicationScreen.thiz, PluginManager.getInstance(), ApplicationScreen.thiz.messageHandler);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		keepScreenOn = prefs.getBoolean("keepScreenOn", false);
		
		// set preview, on click listener and surface buffers
		preview = (SurfaceView) this.findViewById(R.id.SurfaceView01);
		preview.setOnClickListener(this);
		preview.setOnTouchListener(this);
		preview.setKeepScreenOn(true);

		surfaceHolder = preview.getHolder();
		surfaceHolder.addCallback(this);
		
		orientListener = new OrientationEventListener(this)
		{
			@Override
			public void onOrientationChanged(int orientation)
			{
				// figure landscape or portrait
				if (ApplicationScreen.thiz.landscapeIsNormal)
				{
					orientation += 90;
				}

				if ((orientation < 45) || (orientation > 315 && orientation < 405)
						|| ((orientation > 135) && (orientation < 225)))
				{
					if (ApplicationScreen.wantLandscapePhoto)
					{
						ApplicationScreen.wantLandscapePhoto = false;
					}
				} else
				{
					if (!ApplicationScreen.wantLandscapePhoto)
					{
						ApplicationScreen.wantLandscapePhoto = true;
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
				preview.setKeepScreenOn(keepScreenOn);
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
		
		afterOnCreate();
	}
	
	//At this point CameraController, GUIManager are not created yet.
	//Use this method to initialize some shared preferences or do any other
	//logic that isn't depended from OpenCamera core's objects.
	abstract protected void duringOnCreate();
	
	//At this point all OpenCamera main objects are created.
	abstract protected void afterOnCreate();

	/*
	 * Get/Set method for protected variables
	 */
//	public static ApplicationScreen getInstance()
//	{
//		return thiz;
//	}

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
	abstract public void createImageReaders(ImageReader.OnImageAvailableListener imageAvailableListener);
	

	@TargetApi(19)
	@Override
	abstract public Surface getPreviewYUVImageSurface();
	

	@TargetApi(19)
	@Override
	abstract public Surface getYUVImageSurface();
	
	@TargetApi(19)
	@Override
	abstract public Surface getJPEGImageSurface();
	

	@TargetApi(19)
	@Override
	abstract public Surface getRAWImageSurface();
	
	
	public static SurfaceHolder getPreviewSurfaceHolder()
	{
		return thiz.surfaceHolder;
	}

	public static SurfaceView getPreviewSurfaceView()
	{
		return thiz.preview;
	}
	
	
	public static int getCaptureFormat()
	{
		return thiz.captureFormat;
	}

	public static void setCaptureFormat(int capture)
	{
		thiz.captureFormat = capture;
	}
	
	public static int getPreviewSurfaceLayoutWidth()
	{
		return thiz.surfaceLayoutWidth;
	}
	
	public static int getPreviewSurfaceLayoutHeight()
	{
		return thiz.surfaceLayoutHeight;
	}
	
	public static void setPreviewSurfaceLayoutWidth(int width)
	{
		thiz.surfaceLayoutWidth = width;
	}
	
	public static void setPreviewSurfaceLayoutHeight(int height)
	{
		thiz.surfaceLayoutHeight = height;
	}

	public static void setSurfaceHolderSize(int width, int height)
	{
		if (thiz.surfaceHolder != null)
		{
			thiz.surfaceWidth = width;
			thiz.surfaceHeight = height;
			thiz.surfaceHolder.setFixedSize(width, height);
		}
	}
	
	abstract public boolean isShutterSoundEnabled();

	public static int getOrientation()
	{
		return thiz.orientationMain;
	}

	public static int getMeteringMode()
	{
		return thiz.currentMeteringMode;
	}

	/*
	 * ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^ Get/Set method for protected variables
	 */
	
	abstract public void onPreferenceCreate(PreferenceFragment prefActivity);
	abstract public void onAdvancePreferenceCreate(PreferenceFragment prefActivity);

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
		onApplicationStart();
	}
	
	protected void onApplicationStart()
	{
		CameraController.onStart();
		ApplicationScreen.getGUIManager().onStart();
		PluginManager.getInstance().onStart();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		onApplicationStop();
	}
	
	protected void onApplicationStop()
	{
		mApplicationStarted = false;
		orientationMain = 0;
		orientationMainPrevious = 0;
		ApplicationScreen.getGUIManager().onStop();
		PluginManager.getInstance().onStop();
		CameraController.onStop();

		if (CameraController.isUseHALv3())
			stopImageReaders();		
	}

	@TargetApi(21)
	abstract protected void stopImageReaders();

	@Override
	protected void onDestroy()
	{
		super.onDestroy();
		
		onApplicationDestroy();
	}
	
	protected void onApplicationDestroy()
	{
		ApplicationScreen.getGUIManager().onDestroy();
		PluginManager.getInstance().onDestroy();
		CameraController.onDestroy();

		this.hideOpenGLLayer();		
	}

	protected CountDownTimer	onResumeTimer	= null;

	@Override
	protected void onResume()
	{
		super.onResume();

		onApplicationResume();
	}
	
	protected void onApplicationResume()
	{
		isCameraConfiguring = false;

		if (!isCreating)
			onResumeTimer = new CountDownTimer(50, 50)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

					preview.setKeepScreenOn(true);
					
					captureFormat = CameraController.JPEG;

					ApplicationScreen.getGUIManager().onResume();
					PluginManager.getInstance().onResume();
					CameraController.onResume();
					ApplicationScreen.thiz.mPausing = false;

					if (CameraController.isUseHALv3())
					{
						ApplicationScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						CameraController.setupCamera(null, true);

						if (glView != null)
							glView.onResume();
					} else if ((surfaceCreated && (!CameraController.isCameraCreated())))
					{
						ApplicationScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						CameraController.setupCamera(surfaceHolder, true);

						if (glView != null)
						{
							glView.onResume();
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

		long memoryFree = getAvailableInternalMemory();
		if (memoryFree < 30)
			Toast.makeText(ApplicationScreen.getMainContext(), "Almost no free space left on internal storage.",
					Toast.LENGTH_LONG).show();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
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
	}

	@Override
	public void relaunchCamera()
	{
		if (CameraController.isUseHALv3() || PluginManager.getInstance().isRestart())
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
		} else {
			// Need this for correct exposure control state, after switching DRO-on/DRO-off in single mode.
			guiManager.onPluginsInitialized();
		}
	}

	protected long getAvailableInternalMemory()
	{
		File path = Environment.getDataDirectory();
		StatFs stat = new StatFs(path.getPath());
		long blockSize = stat.getBlockSize();
		long availableBlocks = stat.getAvailableBlocks();
		return availableBlocks * blockSize / 1048576;
	}

	@Override
	protected void onPause()
	{
		super.onPause();

		onApplicationPause();
	}
	
	protected void onApplicationPause()
	{
		if (onResumeTimer != null)
		{
			onResumeTimer.cancel();
		}

		mApplicationStarted = false;

		ApplicationScreen.getGUIManager().onPause();
		PluginManager.getInstance().onPause(true);

		orientListener.disable();

		this.mPausing = true;

		this.hideOpenGLLayer();

		if (screenTimer != null)
		{
			if (isScreenTimerRunning)
				screenTimer.cancel();
			isScreenTimerRunning = false;
		}

		// CameraController.onPause(CameraController.isUseHALv3()? false :
		// switchingMode);
		CameraController.onPause(true);

		if (CameraController.isUseHALv3())
			stopImageReaders();

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
		Log.e("ApplicationScreen", "SURFACE CHANGED");
		mCameraSurface = holder.getSurface();

		if (isCameraConfiguring)
		{
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_SURFACE_CONFIGURED, 0);
			isCameraConfiguring = false;
		}
		else if (!isCreating)
		{
			new CountDownTimer(50, 50)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					if (!ApplicationScreen.thiz.mPausing && surfaceCreated && (!CameraController.isCameraCreated()))
					{
						ApplicationScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						if (!CameraController.isUseHALv3())
						{
							CameraController.setupCamera(holder, true);
						}
						else
							messageHandler.sendEmptyMessage(ApplicationInterface.MSG_SURFACE_READY);
					}
				}
			}.start();
		}
	}
	
	public void setCameraImageSizeIndex(int captureIndex, boolean init)
	{
		CameraController.setCameraImageSizeIndex(captureIndex);
	}
	
	//Used if some modes want to set special image size
	@Override
	public void setSpecialImageSizeIndexPref(int iIndex)
	{
	}
	
	@Override
	public String  getSpecialImageSizeIndexPref()
	{
		return "-1";
	}

	//Method used only in Almalence's multishot modes
	public static int selectImageDimensionMultishot()
	{
		return 0;
	}

//	public void onSurfaceChangedMain(final SurfaceHolder holder, final int width, final int height)
//	{
//		if (!ApplicationScreen.thiz.mPausing && surfaceCreated && (!CameraController.isCameraCreated()))
//		{
//			ApplicationScreen.thiz.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
//
//			if (CameraController.isUseHALv3())
//			{
//				// CameraController.setupCamera(null);
//				messageHandler.sendEmptyMessage(ApplicationInterface.MSG_SURFACE_READY);
//			} else
//			{
//				Log.d("MainScreen", "surfaceChangedMain: CameraController.setupCamera(null)");
//				CameraController.setupCamera(holder, true);
//			}
//		}
//	}

	@Override
	public void addSurfaceCallback()
	{
		thiz.surfaceHolder.addCallback(thiz);
	}

	boolean	isCameraConfiguring	= false;

	@Override
	public void configureCamera(boolean createGUI)
	{
		CameraController.updateCameraFeatures();

		// ----- Select preview dimensions with ratio correspondent to
		// full-size image
		PluginManager.getInstance().setCameraPreviewSize();
		// prepare list of surfaces to be used in capture requests
		if (CameraController.isUseHALv3())
			configureHALv3Camera(captureFormat);
		else
		{
			Camera.Size sz = CameraController.getCameraParameters().getPreviewSize();

			Log.e("ApplicationScreen", "Viewfinder preview size: " + sz.width + "x" + sz.height);
			guiManager.setupViewfinderPreviewSize(new CameraController.Size(sz.width, sz.height));
			CameraController.allocatePreviewBuffer(sz.width * sz.height
					* ImageFormat.getBitsPerPixel(CameraController.getCameraParameters().getPreviewFormat()) / 8);

			CameraController.getCamera().setErrorCallback(CameraController.getInstance());

			onCameraConfigured();
		}
		
		if(createGUI)
		{
			PluginManager.getInstance().onGUICreate();
			ApplicationScreen.getGUIManager().onGUICreate();
		}
	}

	protected void onCameraConfigured()
	{
		PluginManager.getInstance().setupCameraParameters();

		Camera.Parameters cp = CameraController.getCameraParameters();

		if (!CameraController.isUseHALv3())
		{
			try
			{
				// Nexus 5 is giving preview which is too dark without this
				if (Build.MODEL.contains("Nexus 5"))
				{
					cp.setPreviewFpsRange(7000, 30000);
					CameraController.setCameraParameters(cp);
					cp = CameraController.getCameraParameters();
				}
			} catch (RuntimeException e)
			{
				Log.d("ApplicationScreen", "MainScreen.setupCamera unable setParameters " + e.getMessage());
			}
			
			if (cp != null)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
				int antibanding = Integer.parseInt(prefs.getString(MainScreen.sAntibandingPref, "3"));
				switch(antibanding)
				{
					case 0:
						cp.setAntibanding("off");
						break;
					case 1:
						cp.setAntibanding("50hz");
						break;
					case 2:
						cp.setAntibanding("60hz");
						break;
					case 3:
						cp.setAntibanding("auto");
						break;
					default:
						cp.setAntibanding("auto");
						break;
				}
				CameraController.setCameraParameters(cp);
				
				previewWidth = cp.getPreviewSize().width;
				previewHeight = cp.getPreviewSize().height;
			}
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
						Toast.makeText(ApplicationScreen.thiz, "Unable to start camera", Toast.LENGTH_LONG).show();
						return;
					}

					CameraController.getCamera().setPreviewCallbackWithBuffer(CameraController.getInstance());
					CameraController.getCamera().addCallbackBuffer(CameraController.getPreviewBuffer());
				} else
				{
					guiManager.onCameraCreate();
					PluginManager.getInstance().onCameraParametersSetup();
					guiManager.onPluginsInitialized();
				}

				PluginManager.getInstance().onCameraSetup();
				guiManager.onCameraSetup();
				ApplicationScreen.mApplicationStarted = true;

				if (ApplicationScreen.isForceClose)
					PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_APPLICATION_STOP, 0);
			}

			@Override
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}
		}.start();
	}

	@TargetApi(21)
	protected void configureHALv3Camera(int captureFormat)
	{
		isCameraConfiguring = true;

		surfaceList = new ArrayList<Surface>();

		setSurfaceHolderSize(surfaceWidth, surfaceHeight);
	}

	@TargetApi(21)
	abstract public void createCaptureSession();	

	protected void prepareMeteringAreas()
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
		Log.e("ApplicationScreen", "SURFACE CREATED");
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

		Log.d("MainScreen", "SURFACE CREATED");
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder holder)
	{
		surfaceCreated = false;
	}

	// SURFACES (preview, image readers)
	@Override
	public Surface getCameraSurface()
	{
		return mCameraSurface;
	}

	//Probably used only by Panorama plugin. Added to avoid non direct interface (message/handler)
	public static void takePicture()
	{
		PluginManager.getInstance().takePicture();
	}
	
	@Override
	public void captureFailed()
	{
		ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION);
		ApplicationScreen.thiz.muteShutter(false);
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

		return new CameraController.Size(lp.width, lp.height);
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
	public boolean onKeyUp(int keyCode, KeyEvent event) {
		// Prevent system sounds, for volume buttons.
		if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN || keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
			return true;
		}
		
		return super.onKeyUp(keyCode, event);
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
			ApplicationScreen.getGUIManager().onHardwareShutterButtonPressed();
			return true;
		}
		// focus/half-press button processing
		if (keyCode == KeyEvent.KEYCODE_FOCUS)
		{
			if (event.getDownTime() == event.getEventTime())
			{
				ApplicationScreen.getGUIManager().onHardwareFocusButtonPressed();
			}
			return true;
		}

		// check if Headset Hook button has some functions except standard
		if (keyCode == KeyEvent.KEYCODE_HEADSETHOOK)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			boolean headsetFunc = prefs.getBoolean("headsetPrefCommon", false);
			if (headsetFunc)
			{
				ApplicationScreen.getGUIManager().onHardwareFocusButtonPressed();
				ApplicationScreen.getGUIManager().onHardwareShutterButtonPressed();
				return true;
			}
		}

		if (PluginManager.getInstance().onKeyDown(true, keyCode, event))
			return true;
		if (guiManager.onKeyDown(true, keyCode, event))
			return true;

		return super.onKeyDown(keyCode, event);
	}
	

	@Override
	public void onClick(View v)
	{
		if (mApplicationStarted)
			ApplicationScreen.getGUIManager().onClick(v);
	}

	@Override
	public boolean onTouch(View view, MotionEvent event)
	{
		if (mApplicationStarted)
			return ApplicationScreen.getGUIManager().onTouch(view, event);
		return true;
	}

	public boolean onTouchSuper(View view, MotionEvent event)
	{
		return super.onTouchEvent(event);
	}

	public void onButtonClick(View v)
	{
		ApplicationScreen.getGUIManager().onButtonClick(v);
	}

	@Override
	public void onShutter()
	{
		PluginManager.getInstance().onShutter();
	}

	public static boolean isForceClose()
	{
		return isForceClose;
	}

	public static boolean isApplicationStarted()
	{
		return mApplicationStarted;
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
		case ApplicationInterface.MSG_APPLICATION_STOP:
			this.setResult(RESULT_OK);
			this.finish();
			break;
		case MSG_RETURN_CAPTURED:
			this.setResult(RESULT_OK);
			this.finish();
			break;
		case ApplicationInterface.MSG_CAMERA_CONFIGURED:
			onCameraConfigured();
			break;
//		case ApplicationInterface.MSG_CAMERA_READY:
//			{
//				if (CameraController.isCameraCreated())
//				{
//					configureCamera();
//					PluginManager.getInstance().onGUICreate();
//					MainScreen.getGUIManager().onGUICreate();
//				}
//			}
//			break;
		case ApplicationInterface.MSG_CAMERA_OPENED:
			if (mCameraStarted)
				break;
		case ApplicationInterface.MSG_SURFACE_READY:
			{
				// if both surface is created and camera device is opened
				// - ready to set up preview and other things
				// if (surfaceCreated && (HALv3.getCamera2() != null))
				if (surfaceCreated)
				{
					configureCamera(!CameraController.isUseHALv3());
					mCameraStarted = true;
				}
			}
			break;
		case ApplicationInterface.MSG_SURFACE_CONFIGURED:
			{
				createCaptureSession();
				PluginManager.getInstance().onGUICreate();
				ApplicationScreen.getGUIManager().onGUICreate();
				mCameraStarted = true;
			}
			break;
		case ApplicationInterface.MSG_CAMERA_STOPED:
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
			glView = new GLLayer(ApplicationScreen.getMainContext(), version);
			LayoutParams params = ApplicationScreen.getPreviewSurfaceView().getLayoutParams();
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
			// preview.getHolder().getSurface().lockCanvas(null).drawColor(Color.BLACK);
			glView.onPause();
			glView.destroyDrawingCache();
			((RelativeLayout) this.findViewById(R.id.mainLayout2)).removeView(glView);
			glView = null;
		}
	}
	
	@Override
	abstract public void showCaptureIndication(boolean playShutter);
	

	// set TRUE to mute and FALSE to unmute
	public void muteShutter(boolean mute)
	{
			AudioManager mgr = (AudioManager) ApplicationScreen.thiz.getSystemService(ApplicationScreen.AUDIO_SERVICE);
			mgr.setStreamMute(AudioManager.STREAM_SYSTEM, mute);
	}

	@Override
	public void setExpoPreviewPref(boolean previewMode)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
		Editor prefsEditor = prefs.edit();
		prefsEditor.putBoolean(ApplicationScreen.sExpoPreviewModePref, previewMode);
		prefsEditor.commit();
	}
	
	@Override
	public boolean getExpoPreviewPref()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mainContext);
		if (true == prefs.contains(ApplicationScreen.sExpoPreviewModePref)) 
        {
        	return prefs.getBoolean(ApplicationScreen.sExpoPreviewModePref, true);
        }
        else
        	return true;
	}
	
	
	@Override
	public void setCameraPreviewSize(int iWidth, int iHeight)
	{
		if(CameraController.isUseHALv3())
		{
			setSurfaceHolderSize(iWidth, iHeight);
			setPreviewWidth(iWidth);
			setPreviewHeight(iHeight);
		}
		
		CameraController.setCameraPreviewSize(new CameraController.Size(iWidth, iHeight));		
	}

	public static int getPreviewWidth()
	{
		return thiz.previewWidth;
	}

	public static void setPreviewWidth(int iWidth)
	{
		thiz.previewWidth = iWidth;
	}

	public static int getPreviewHeight()
	{
		return thiz.previewHeight;
	}

	public static void setPreviewHeight(int iHeight)
	{
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

	public static Resources getAppResources()
	{
		return ApplicationScreen.thiz.getResources();
	}

//	/*******************************************************/
//	/************************ Billing ************************/
//
//	protected boolean		showStore					= false;
//	// <!-- -+-
//	OpenIabHelper		mHelper;
//
//	protected boolean		bOnSale						= false;
//	protected boolean		couponSale					= false;
//
//	protected boolean		unlockAllPurchased			= false;
//	protected boolean		superPurchased				= false;
//	protected boolean		hdrPurchased				= false;
//	protected boolean		panoramaPurchased			= false;
//	protected boolean		objectRemovalBurstPurchased	= false;
//	protected boolean		groupShotPurchased			= false;
//
//	protected boolean		unlockAllSubscriptionMonth	= false;
//	protected boolean		unlockAllSubscriptionYear	= false;
//
//	static final String	SKU_SUPER					= "plugin_almalence_super";
//	static final String	SKU_HDR						= "plugin_almalence_hdr";
//	static final String	SKU_PANORAMA				= "plugin_almalence_panorama";
//	static final String	SKU_UNLOCK_ALL				= "unlock_all_forever";
//
//	// barcode coupon
//	static final String	SKU_UNLOCK_ALL_COUPON		= "unlock_all_forever_coupon";
//
//	// multishot currently
//	static final String	SKU_MOVING_SEQ				= "plugin_almalence_moving_burst";
//
//	// unused. but if someone payed - will be unlocked multishot
//	static final String	SKU_GROUPSHOT				= "plugin_almalence_groupshot";
//	// subscription
//	static final String	SKU_SUBSCRIPTION_YEAR		= "subscription_unlock_all_year";
//	static final String	SKU_SUBSCRIPTION_YEAR_NEW	= "subscription_unlock_all_year_3free";
//	static final String	SKU_SUBSCRIPTION_YEAR_CTRL	= "subscription_unlock_all_year_controller";
//
//	static final String	SKU_SALE1					= "abc_sale_controller1";
//	static final String	SKU_SALE2					= "abc_sale_controller2";
//
//	static final String	SKU_PROMO					= "abc_promo";
//
//	static
//	{
//		// Yandex store
//		OpenIabHelper.mapSku(SKU_SUPER, "com.yandex.store", "plugin_almalence_super");
//		OpenIabHelper.mapSku(SKU_HDR, "com.yandex.store", "plugin_almalence_hdr");
//		OpenIabHelper.mapSku(SKU_PANORAMA, "com.yandex.store", "plugin_almalence_panorama");
//		OpenIabHelper.mapSku(SKU_UNLOCK_ALL, "com.yandex.store", "unlock_all_forever");
//		OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON, "com.yandex.store", "unlock_all_forever_coupon");
//		OpenIabHelper.mapSku(SKU_MOVING_SEQ, "com.yandex.store", "plugin_almalence_moving_burst");
//		OpenIabHelper.mapSku(SKU_GROUPSHOT, "com.yandex.store", "plugin_almalence_groupshot");
//		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR, "com.yandex.store", "subscription_unlock_all_year");
//		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR_NEW, "com.yandex.store", "subscription_unlock_all_year_3free");
//		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR_CTRL, "com.yandex.store", "subscription_unlock_all_year_controller");
//
//		OpenIabHelper.mapSku(SKU_SALE1, "com.yandex.store", "abc_sale_controller1");
//		OpenIabHelper.mapSku(SKU_SALE2, "com.yandex.store", "abc_sale_controller2");
//		OpenIabHelper.mapSku(SKU_PROMO, "com.yandex.store", "abc_promo");
//
//		// Amazon store
//		OpenIabHelper.mapSku(SKU_SUPER, OpenIabHelper.NAME_AMAZON, "plugin_almalence_super_amazon");
//		OpenIabHelper.mapSku(SKU_HDR, OpenIabHelper.NAME_AMAZON, "plugin_almalence_hdr_amazon");
//		OpenIabHelper.mapSku(SKU_PANORAMA, OpenIabHelper.NAME_AMAZON, "plugin_almalence_panorama_amazon");
//		OpenIabHelper.mapSku(SKU_UNLOCK_ALL, OpenIabHelper.NAME_AMAZON, "unlock_all_forever_amazon");
//		OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON, OpenIabHelper.NAME_AMAZON, "unlock_all_forever_coupon_amazon");
//		OpenIabHelper.mapSku(SKU_MOVING_SEQ, OpenIabHelper.NAME_AMAZON, "plugin_almalence_moving_burst_amazon");
//		OpenIabHelper.mapSku(SKU_GROUPSHOT, OpenIabHelper.NAME_AMAZON, "plugin_almalence_groupshot_amazon");
//		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR, OpenIabHelper.NAME_AMAZON, "subscription_unlock_all_year");
//		OpenIabHelper
//				.mapSku(SKU_SUBSCRIPTION_YEAR_NEW, OpenIabHelper.NAME_AMAZON, "subscription_unlock_all_year_3free");
//		OpenIabHelper.mapSku(SKU_SUBSCRIPTION_YEAR_CTRL, OpenIabHelper.NAME_AMAZON,
//				"subscription_unlock_all_year_controller");
//
//		OpenIabHelper.mapSku(SKU_SALE1, OpenIabHelper.NAME_AMAZON, "abc_sale_controller1_amazon");
//		OpenIabHelper.mapSku(SKU_SALE2, OpenIabHelper.NAME_AMAZON, "abc_sale_controller2_amazon");
//		OpenIabHelper.mapSku(SKU_PROMO, OpenIabHelper.NAME_AMAZON, "abc_promo_amazon");
//
//		// Samsung store
//		// OpenIabHelper.mapSku(SKU_SUPER, OpenIabHelper.NAME_SAMSUNG,
//		// "100000103369/000001018387");
//		// OpenIabHelper.mapSku(SKU_HDR, OpenIabHelper.NAME_SAMSUNG,
//		// "100000103369/000001018387");
//		// OpenIabHelper.mapSku(SKU_PANORAMA, OpenIabHelper.NAME_SAMSUNG,
//		// "100000103369/000001018389");
//		// OpenIabHelper.mapSku(SKU_UNLOCK_ALL, OpenIabHelper.NAME_SAMSUNG,
//		// "100000103369/000001017613");
//		// OpenIabHelper.mapSku(SKU_UNLOCK_ALL_COUPON,
//		// OpenIabHelper.NAME_SAMSUNG, "100000103369/000001018392");
//		// OpenIabHelper.mapSku(SKU_MOVING_SEQ, OpenIabHelper.NAME_SAMSUNG,
//		// "100000103369/000001018391");
//		// OpenIabHelper.mapSku(SKU_GROUPSHOT, OpenIabHelper.NAME_SAMSUNG,
//		// "100000103369/000001018384");
//		//
//		// OpenIabHelper.mapSku(SKU_SALE1, OpenIabHelper.NAME_SAMSUNG,
//		// "100000103369/000001018393");
//		// OpenIabHelper.mapSku(SKU_SALE2, OpenIabHelper.NAME_SAMSUNG,
//		// "100000103369/000001018394");
//	}
//
//	public void setShowStore(boolean show)
//	{
//		showStore = show;
//	}
//
//	public boolean isShowStore()
//	{
//		return showStore;
//	}
//
//	public void activateCouponSale()
//	{
//		couponSale = true;
//	}
//
//	public boolean isCouponSale()
//	{
//		return couponSale;
//	}
//
//	public boolean isUnlockedAll()
//	{
//		return unlockAllPurchased;
//	}
//
//	// controls subscription status request
//	protected boolean	subscriptionStatusRequest	= false;
//	protected long	timeLastSubscriptionCheck	= 0;							// should
//																				// check
//																				// each
//																				// 32
//																				// days
//																				// 32*24*60*60*1000
//	protected long	days32						= 32 * 24 * 60 * 60 * 1000L;
//
//	protected void createBillingHandler()
//	{
//		try
//		{
//			final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
//
//			timeLastSubscriptionCheck = prefs.getLong("timeLastSubscriptionCheck", 0);
//			if ((System.currentTimeMillis() - timeLastSubscriptionCheck) > days32)
//				subscriptionStatusRequest = true;
//			else
//				subscriptionStatusRequest = false;
//
//			if ((isInstalled("com.almalence.hdr_plus")) || (isInstalled("com.almalence.pixfix")))
//			{
//				hdrPurchased = true;
//				Editor prefsEditor = prefs.edit();
//				prefsEditor.putBoolean("plugin_almalence_hdr", true).commit();
//			}
//			if (isInstalled("com.almalence.panorama.smoothpanorama"))
//			{
//				panoramaPurchased = true;
//				Editor prefsEditor = prefs.edit();
//				prefsEditor.putBoolean("plugin_almalence_panorama", true).commit();
//			}
//
//			String base64EncodedPublicKeyGoogle = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAnztuXLNughHjGW55Zlgicr9r5bFP/K5DBc3jYhnOOo1GKX8M2grd7+SWeUHWwQk9lgQKat/ITESoNPE7ma0ZS1Qb/VfoY87uj9PhsRdkq3fg+31Q/tv5jUibSFrJqTf3Vmk1l/5K0ljnzX4bXI0p1gUoGd/DbQ0RJ3p4Dihl1p9pJWgfI9zUzYfvk2H+OQYe5GAKBYQuLORrVBbrF/iunmPkOFN8OcNjrTpLwWWAcxV5k0l5zFPrPVtkMZzKavTVWZhmzKNhCvs1d8NRwMM7XMejzDpI9A7T9egl6FAN4rRNWqlcZuGIMVizJJhvOfpCLtY971kQkYNXyilD40fefwIDAQAB";
//			String base64EncodedPublicKeyYandex = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA6KzaraKmv48Y+Oay2ZpWu4BHtSKYZidyCxbaYZmmOH4zlRNic/PDze7OA4a1buwdrBg3AAHwfVbHFzd9o91yinnHIWYQqyPg7L1Swh5W70xguL4jlF2N/xI9VoL4vMRv3Bf/79VfQ11utcPLHEXPR8nPEp9PT0wN2Hqp4yCWFbfvhVVmy7sQjywnfLqcWTcFCT6N/Xdxs1quq0hTE345MiCgkbh1xVULmkmZrL0rWDVCaxfK4iZWSRgQJUywJ6GMtUh+FU6/7nXDenC/vPHqnDR0R6BRi+QsES0ZnEfQLqNJoL+rqJDr/sDIlBQQDMQDxVOx0rBihy/FlHY34UF+bwIDAQAB";
//			// Create the helper, passing it our context and the public key to
//			// verify signatures with
//			// Log.v("Main billing", "Creating IAB helper.");
//			Map<String, String> storeKeys = new HashMap<String, String>();
//			storeKeys.put(OpenIabHelper.NAME_GOOGLE, base64EncodedPublicKeyGoogle);
//			storeKeys.put("com.yandex.store", base64EncodedPublicKeyYandex);
//			mHelper = new OpenIabHelper(this, storeKeys);
//
//			OpenIabHelper.enableDebugLogging(true);
//
//			// Log.v("Main billing", "Starting setup.");
//			mHelper.startSetup(new IabHelper.OnIabSetupFinishedListener()
//			{
//				public void onIabSetupFinished(IabResult result)
//				{
//					try
//					{
//						Log.v("Main billing", "Setup finished.");
//
//						if (!result.isSuccess())
//						{
//							Log.v("Main billing", "Problem setting up in-app billing: " + result);
//							return;
//						}
//
//						List<String> additionalSkuList = new ArrayList<String>();
//						additionalSkuList.add(SKU_SUPER);
//						additionalSkuList.add(SKU_HDR);
//						additionalSkuList.add(SKU_PANORAMA);
//						additionalSkuList.add(SKU_UNLOCK_ALL);
//						additionalSkuList.add(SKU_UNLOCK_ALL_COUPON);
//						additionalSkuList.add(SKU_MOVING_SEQ);
//						additionalSkuList.add(SKU_GROUPSHOT);
//						additionalSkuList.add(SKU_SUBSCRIPTION_YEAR_CTRL);
//						additionalSkuList.add(SKU_PROMO);
//
//						if (subscriptionStatusRequest)
//						{
//							// subscription year
//							additionalSkuList.add(SKU_SUBSCRIPTION_YEAR);
//							additionalSkuList.add(SKU_SUBSCRIPTION_YEAR_NEW);
//							// reset subscription status
//							unlockAllSubscriptionYear = false;
//							prefs.edit().putBoolean("subscription_unlock_all_year", false).commit();
//
//							timeLastSubscriptionCheck = System.currentTimeMillis();
//							prefs.edit().putLong("timeLastSubscriptionCheck", timeLastSubscriptionCheck).commit();
//						}
//
//						// for sale
//						additionalSkuList.add(SKU_SALE1);
//						additionalSkuList.add(SKU_SALE2);
//
//						// Log.v("Main billing",
//						// "Setup successful. Querying inventory.");
//						mHelper.queryInventoryAsync(true, additionalSkuList, mGotInventoryListener);
//					} catch (Exception e)
//					{
//						e.printStackTrace();
//						Log.e("Main billing", "onIabSetupFinished exception: " + e.getMessage());
//					}
//				}
//			});
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			Log.e("Main billing", "createBillingHandler exception: " + e.getMessage());
//		}
//	}
//
//	protected void destroyBillingHandler()
//	{
//		try
//		{
//			if (mHelper != null)
//				mHelper.dispose();
//			mHelper = null;
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			Log.e("Main billing", "destroyBillingHandler exception: " + e.getMessage());
//		}
//	}
//
//	public String								titleUnlockAll				= "$6.95";
//	public String								titleUnlockAllCoupon		= "$3.95";
//	public String								titleUnlockHDR				= "$2.99";
//	public String								titleUnlockSuper			= "$2.99";
//	public String								titleUnlockPano				= "$2.99";
//	public String								titleUnlockMoving			= "$3.99";
//	public String								titleUnlockGroup			= "$2.99";
//	public String								titleSubscriptionYear		= "$4.99";
//
//	public String								summary_SKU_PROMO			= "alyrom0nap";
//	// public String summaryUnlockAll = "";
//	// public String summaryUnlockHDR = "";
//	// public String summaryUnlockPano = "";
//	// public String summaryUnlockMoving = "";
//	// public String summaryUnlockGroup = "";
//	//
//	// public String summarySubscriptionMonth = "";
//	// public String summarySubscriptionYear = "";
//
//	IabHelper.QueryInventoryFinishedListener	mGotInventoryListener		= new IabHelper.QueryInventoryFinishedListener()
//																			{
//																				public void onQueryInventoryFinished(
//																						IabResult result,
//																						Inventory inventory)
//																				{
//																					if (inventory == null)
//																					{
//																						Log.e("Main billing",
//																								"mGotInventoryListener inventory null ");
//																						return;
//																					}
//
//																					SharedPreferences prefs = PreferenceManager
//																							.getDefaultSharedPreferences(ApplicationScreen
//																									.getMainContext());
//
//																					Editor prefsEditor = prefs.edit();
//																					if (inventory
//																							.hasPurchase(SKU_SUPER))
//																					{
//																						superPurchased = true;
//																						prefsEditor
//																								.putBoolean(
//																										"plugin_almalence_super",
//																										true).commit();
//																					}
//																					if (inventory.hasPurchase(SKU_HDR))
//																					{
//																						hdrPurchased = true;
//																						prefsEditor.putBoolean(
//																								"plugin_almalence_hdr",
//																								true).commit();
//																					}
//																					if (inventory
//																							.hasPurchase(SKU_PANORAMA))
//																					{
//																						panoramaPurchased = true;
//																						prefsEditor
//																								.putBoolean(
//																										"plugin_almalence_panorama",
//																										true).commit();
//																					}
//																					if (inventory
//																							.hasPurchase(SKU_UNLOCK_ALL))
//																					{
//																						unlockAllPurchased = true;
//																						prefsEditor.putBoolean(
//																								"unlock_all_forever",
//																								true).commit();
//																					}
//																					if (inventory
//																							.hasPurchase(SKU_UNLOCK_ALL_COUPON))
//																					{
//																						unlockAllPurchased = true;
//																						prefsEditor.putBoolean(
//																								"unlock_all_forever",
//																								true).commit();
//																					}
//																					if (inventory
//																							.hasPurchase(SKU_MOVING_SEQ))
//																					{
//																						objectRemovalBurstPurchased = true;
//																						prefsEditor
//																								.putBoolean(
//																										"plugin_almalence_moving_burst",
//																										true).commit();
//																					}
//																					if (inventory
//																							.hasPurchase(SKU_GROUPSHOT))
//																					{
//																						groupShotPurchased = true;
//																						prefsEditor
//																								.putBoolean(
//																										"plugin_almalence_moving_burst",
//																										true).commit();
//																					}
//																					if (inventory
//																							.hasPurchase(SKU_SUBSCRIPTION_YEAR))
//																					{
//																						unlockAllSubscriptionYear = true;
//																						prefsEditor
//																								.putBoolean(
//																										"subscription_unlock_all_year",
//																										true).commit();
//																						unlockAllPurchased = true;
//																						prefsEditor.putBoolean(
//																								"unlock_all_forever",
//																								true).commit();
//																					}
//																					if (inventory
//																							.hasPurchase(SKU_SUBSCRIPTION_YEAR_NEW))
//																					{
//																						unlockAllSubscriptionYear = true;
//																						prefsEditor
//																								.putBoolean(
//																										"subscription_unlock_all_year",
//																										true).commit();
//																						unlockAllPurchased = true;
//																						prefsEditor.putBoolean(
//																								"unlock_all_forever",
//																								true).commit();
//																					}
//
//																					try
//																					{
//																						String[] separated = inventory
//																								.getSkuDetails(
//																										SKU_SALE1)
//																								.getPrice().split(",");
//																						int price1 = Integer
//																								.valueOf(separated[0]);
//																						String[] separated2 = inventory
//																								.getSkuDetails(
//																										SKU_SALE2)
//																								.getPrice().split(",");
//																						int price2 = Integer
//																								.valueOf(separated2[0]);
//
//																						if (price1 < price2)
//																							bOnSale = true;
//																						else
//																							bOnSale = false;
//
//																						prefsEditor.putBoolean(
//																								"bOnSale", bOnSale)
//																								.commit();
//																					} catch (Exception e)
//																					{
//																						Log.e("Main billing SALE",
//																								"No sale data available");
//																						bOnSale = false;
//																					}
//
//																					try
//																					{
//																						titleUnlockAll = inventory
//																								.getSkuDetails(
//																										SKU_UNLOCK_ALL)
//																								.getPrice();
//																						titleUnlockAllCoupon = inventory
//																								.getSkuDetails(
//																										SKU_UNLOCK_ALL_COUPON)
//																								.getPrice();
//																						titleUnlockSuper = inventory
//																								.getSkuDetails(
//																										SKU_SUPER)
//																								.getPrice();
//																						titleUnlockHDR = inventory
//																								.getSkuDetails(SKU_HDR)
//																								.getPrice();
//																						titleUnlockPano = inventory
//																								.getSkuDetails(
//																										SKU_PANORAMA)
//																								.getPrice();
//																						titleUnlockMoving = inventory
//																								.getSkuDetails(
//																										SKU_MOVING_SEQ)
//																								.getPrice();
//																						titleUnlockGroup = inventory
//																								.getSkuDetails(
//																										SKU_GROUPSHOT)
//																								.getPrice();
//
//																						titleSubscriptionYear = inventory
//																								.getSkuDetails(
//																										SKU_SUBSCRIPTION_YEAR_CTRL)
//																								.getPrice();
//
//																						summary_SKU_PROMO = inventory
//																								.getSkuDetails(
//																										SKU_PROMO)
//																								.getDescription();
//																					} catch (Exception e)
//																					{
//																						Log.e("Market",
//																								"Error Getting data for store!!!!!!!!");
//																					}
//																				}
//																			};
//
//	protected int									HDR_REQUEST					= 100;
//	protected int									SUPER_REQUEST				= 107;
//	protected int									PANORAMA_REQUEST			= 101;
//	protected int									ALL_REQUEST					= 102;
//	protected int									OBJECTREM_BURST_REQUEST		= 103;
//	protected int									GROUPSHOT_REQUEST			= 104;
//	protected int									SUBSCRIPTION_YEAR_REQUEST	= 106;
//
//	public boolean isPurchasedAll()
//	{
//		return unlockAllPurchased;
//	}
//
//	public boolean isPurchasedSuper()
//	{
//		return superPurchased;
//	}
//
//	public boolean isPurchasedHDR()
//	{
//		return hdrPurchased;
//	}
//
//	public boolean isPurchasedPanorama()
//	{
//		return panoramaPurchased;
//	}
//
//	public boolean isPurchasedMoving()
//	{
//		return objectRemovalBurstPurchased;
//	}
//
//	public boolean isPurchasedGroupshot()
//	{
//		return groupShotPurchased;
//	}
//
//	public boolean isPurchasedUnlockAllSubscriptionMonth()
//	{
//		return unlockAllSubscriptionMonth;
//	}
//
//	public boolean isPurchasedUnlockAllSubscriptionYear()
//	{
//		return unlockAllSubscriptionYear;
//	}
//
//	public void purchaseAll()
//	{
//		if (isPurchasedAll())
//			return;
//
//		// now will call store with abc unlocked
//		callStoreForUnlocked(this);
//
//		// TODO: this is for all other markets!!!!! Do not call store!!!
//		// String payload = "";
//		// try
//		// {
//		// mHelper.launchPurchaseFlow(MainScreen.thiz,
//		// isCouponSale()?SKU_UNLOCK_ALL_COUPON:SKU_UNLOCK_ALL, ALL_REQUEST,
//		// mPreferencePurchaseFinishedListener, payload);
//		// }
//		// catch (Exception e) {
//		// e.printStackTrace();
//		// Log.e("Main billing", "Purchase result " + e.getMessage());
//		// Toast.makeText(MainScreen.thiz,
//		// "Error during purchase " + e.getMessage(),
//		// Toast.LENGTH_LONG).show();
//		// }
//	}
//
//	public void purchaseSuper()
//	{
//		if (isPurchasedSuper() || isPurchasedAll())
//			return;
//		String payload = "";
//		try
//		{
//			mHelper.launchPurchaseFlow(ApplicationScreen.thiz, SKU_SUPER, SUPER_REQUEST, mPreferencePurchaseFinishedListener,
//					payload);
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			Log.e("Main billing", "Purchase result " + e.getMessage());
//			Toast.makeText(ApplicationScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
//		}
//	}
//
//	public void purchaseHDR()
//	{
//		if (isPurchasedHDR() || isPurchasedAll())
//			return;
//		String payload = "";
//		try
//		{
//			mHelper.launchPurchaseFlow(ApplicationScreen.thiz, SKU_HDR, HDR_REQUEST, mPreferencePurchaseFinishedListener,
//					payload);
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			Log.e("Main billing", "Purchase result " + e.getMessage());
//			Toast.makeText(ApplicationScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
//		}
//	}
//
//	public void purchasePanorama()
//	{
//		if (isPurchasedPanorama() || isPurchasedAll())
//			return;
//		String payload = "";
//		try
//		{
//			mHelper.launchPurchaseFlow(ApplicationScreen.thiz, SKU_PANORAMA, PANORAMA_REQUEST,
//					mPreferencePurchaseFinishedListener, payload);
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			Log.e("Main billing", "Purchase result " + e.getMessage());
//			Toast.makeText(ApplicationScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
//		}
//	}
//
//	public void purchaseMultishot()
//	{
//		if (isPurchasedMoving() || isPurchasedAll())
//			return;
//		String payload = "";
//		try
//		{
//			mHelper.launchPurchaseFlow(ApplicationScreen.thiz, SKU_MOVING_SEQ, OBJECTREM_BURST_REQUEST,
//					mPreferencePurchaseFinishedListener, payload);
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			Log.e("Main billing", "Purchase result " + e.getMessage());
//			Toast.makeText(ApplicationScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
//		}
//	}
//
//	public void purchasedUnlockAllSubscriptionYear()
//	{
//		if (isPurchasedUnlockAllSubscriptionYear() || isPurchasedAll())
//			return;
//		String payload = "";
//		try
//		{
//			mHelper.launchPurchaseFlow(ApplicationScreen.thiz, SKU_SUBSCRIPTION_YEAR_NEW, SUBSCRIPTION_YEAR_REQUEST,
//					mPreferencePurchaseFinishedListener, payload);
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			Log.e("Main billing", "Purchase result " + e.getMessage());
//			Toast.makeText(ApplicationScreen.thiz, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
//		}
//	}
//
//	// Callback for when purchase from preferences is finished
//	IabHelper.OnIabPurchaseFinishedListener	mPreferencePurchaseFinishedListener	= new IabHelper.OnIabPurchaseFinishedListener()
//																				{
//																					public void onIabPurchaseFinished(
//																							IabResult result,
//																							Purchase purchase)
//																					{
//																						showStore = true;
//																						purchaseFinished(result,
//																								purchase);
//																					}
//																				};
//
//	protected void purchaseFinished(IabResult result, Purchase purchase)
//	{
//		Log.v("Main billing", "Purchase finished: " + result + ", purchase: " + purchase);
//		if (result.isFailure())
//		{
//			Log.v("Main billing", "Error purchasing: " + result);
//			return;
//		}
//
//		Log.v("Main billing", "Purchase successful.");
//
//		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
//
//		if (purchase.getSku().equals(SKU_HDR))
//		{
//			Log.v("Main billing", "Purchase HDR.");
//			hdrPurchased = true;
//
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("plugin_almalence_hdr", true).commit();
//		}
//		if (purchase.getSku().equals(SKU_SUPER))
//		{
//			Log.v("Main billing", "Purchase SUPER.");
//			superPurchased = true;
//
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("plugin_almalence_super", true).commit();
//		}
//		if (purchase.getSku().equals(SKU_PANORAMA))
//		{
//			Log.v("Main billing", "Purchase Panorama.");
//			panoramaPurchased = true;
//
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("plugin_almalence_panorama", true).commit();
//		}
//		if (purchase.getSku().equals(SKU_UNLOCK_ALL))
//		{
//			Log.v("Main billing", "Purchase unlock_all_forever.");
//			unlockAllPurchased = true;
//
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("unlock_all_forever", true).commit();
//		}
//		if (purchase.getSku().equals(SKU_UNLOCK_ALL_COUPON))
//		{
//			Log.v("Main billing", "Purchase unlock_all_forever_coupon.");
//			unlockAllPurchased = true;
//
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("unlock_all_forever", true).commit();
//		}
//		if (purchase.getSku().equals(SKU_MOVING_SEQ))
//		{
//			Log.v("Main billing", "Purchase plugin_almalence_moving_burst.");
//			objectRemovalBurstPurchased = true;
//
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("plugin_almalence_moving_burst", true).commit();
//		}
//		if (purchase.getSku().equals(SKU_GROUPSHOT))
//		{
//			Log.v("Main billing", "Purchase plugin_almalence_moving_burst.");
//			objectRemovalBurstPurchased = true;
//
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("plugin_almalence_moving_burst", true).commit();
//		}
//		if (purchase.getSku().equals(SKU_SUBSCRIPTION_YEAR))
//		{
//			Log.v("Main billing", "Purchase year subscription.");
//			unlockAllSubscriptionYear = true;
//
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("subscription_unlock_all_year", true).commit();
//
//			timeLastSubscriptionCheck = System.currentTimeMillis();
//			prefs.edit().putLong("timeLastSubscriptionCheck", timeLastSubscriptionCheck).commit();
//
//			unlockAllPurchased = true;
//			prefsEditor.putBoolean("unlock_all_forever", true).commit();
//		}
//		if (purchase.getSku().equals(SKU_SUBSCRIPTION_YEAR_NEW))
//		{
//			Log.v("Main billing", "Purchase year subscription.");
//			unlockAllSubscriptionYear = true;
//
//			Editor prefsEditor = prefs.edit();
//			prefsEditor.putBoolean("subscription_unlock_all_year", true).commit();
//
//			timeLastSubscriptionCheck = System.currentTimeMillis();
//			prefs.edit().putLong("timeLastSubscriptionCheck", timeLastSubscriptionCheck).commit();
//
//			unlockAllPurchased = true;
//			prefsEditor.putBoolean("unlock_all_forever", true).commit();
//		}
//	}
//
//	public void launchPurchase(int requestID)
//	{
//		try
//		{
//			guiManager.showStore();
//		} catch (Exception e)
//		{
//			e.printStackTrace();
//			Toast.makeText(this, "Error during purchase " + e.getMessage(), Toast.LENGTH_LONG).show();
//		}
//	}
//
//	IabHelper.OnIabPurchaseFinishedListener	mPurchaseFinishedListener	= new IabHelper.OnIabPurchaseFinishedListener()
//																		{
//																			public void onIabPurchaseFinished(
//																					IabResult result, Purchase purchase)
//																			{
//
//																				guiManager.showStore();
//																				purchaseFinished(result, purchase);
//																			}
//																		};
//
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data)
//	{
//		Log.v("Main billing", "onActivityResult(" + requestCode + "," + resultCode + "," + data);
//
//		// Pass on the activity result to the helper for handling
//		if (!mHelper.handleActivityResult(requestCode, resultCode, data))
//		{
//			// not handled, so handle it ourselves (here's where you'd
//			// perform any handling of activity results not related to in-app
//			// billing...
//			super.onActivityResult(requestCode, resultCode, data);
//		} else
//		{
//			Log.v("Main billing", "onActivityResult handled by IABUtil.");
//		}
//	}
//
//	public boolean	showPromoRedeemed		= false;
//	public boolean	showPromoRedeemedJulius	= false;
//
//	// enter promo code to get smth
//	public void enterPromo()
//	{
//		final float density = getResources().getDisplayMetrics().density;
//
//		LinearLayout ll = new LinearLayout(this);
//		ll.setOrientation(LinearLayout.VERTICAL);
//		ll.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));
//
//		// rating bar
//		final EditText editText = new EditText(this);
//		editText.setHint(R.string.Pref_Upgrde_PromoCode_Text);
//		editText.setHintTextColor(Color.WHITE);
//
//		LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
//				LayoutParams.WRAP_CONTENT);
//		params.gravity = Gravity.CENTER_HORIZONTAL;
//		params.setMargins(0, 20, 0, 30);
//		editText.setLayoutParams(params);
//		ll.addView(editText);
//
//		Button b3 = new Button(this);
//		b3.setText(getResources().getString(R.string.Pref_Upgrde_PromoCode_DoneText));
//		ll.addView(b3);
//
//		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setView(ll);
//		final AlertDialog dialog = builder.create();
//
//		b3.setOnClickListener(new OnClickListener()
//		{
//			public void onClick(View v)
//			{
//				String[] sep = ApplicationScreen.getInstance().summary_SKU_PROMO.split(";");
//				String promo = editText.getText().toString();
//				boolean matchPromo = false;
//
//				// /////////////////////////////////////////////////////
//				// juliusapp promotion
//				if (promo.equalsIgnoreCase("MONOMO") || promo.equalsIgnoreCase("RISPARMI"))
//				{
//					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
//					panoramaPurchased = true;
//					objectRemovalBurstPurchased = true;
//
//					Editor prefsEditor = prefs.edit();
//					prefsEditor.putBoolean("plugin_almalence_panorama", true);
//					prefsEditor.putBoolean("plugin_almalence_moving_burst", true);
//					prefsEditor.commit();
//					dialog.dismiss();
//					guiManager.hideStore();
//					showPromoRedeemedJulius = true;
//					guiManager.showStore();
//					return;
//				}
//				// /////////////////////////////////////////////////////
//
//				for (int i = 0; i < sep.length; i++)
//				{
//					if (promo.equalsIgnoreCase(sep[i]))
//						matchPromo = true;
//				}
//
//				// if (promo.equalsIgnoreCase("appoftheday") ||
//				// promo.equalsIgnoreCase("stelapps"))
//				if (matchPromo)
//				{
//					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
//					unlockAllPurchased = true;
//
//					Editor prefsEditor = prefs.edit();
//					prefsEditor.putBoolean("unlock_all_forever", true);
//					prefsEditor.commit();
//					dialog.dismiss();
//					guiManager.hideStore();
//					showPromoRedeemed = true;
//					guiManager.showStore();
//				} else
//				{
//					editText.setText("");
//					editText.setHint(R.string.Pref_Upgrde_PromoCode_IncorrectText);
//				}
//			}
//		});
//
//		dialog.show();
//	}
//
//	// next methods used to store number of free launches.
//	// using files to store this info
//
//	// returns number of launches left
//	public int getLeftLaunches(String modeID)
//	{
//		String dirPath = getFilesDir().getAbsolutePath() + File.separator + modeID;
//		File projDir = new File(dirPath);
//		if (!projDir.exists())
//		{
//			projDir.mkdirs();
//			WriteLaunches(projDir, 30);
//		}
//		int left = ReadLaunches(projDir);
//		return left;
//	}
//
//	// decrements number of launches left
//	public void decrementLeftLaunches(String modeID)
//	{
//		String dirPath = getFilesDir().getAbsolutePath() + File.separator + modeID;
//		File projDir = new File(dirPath);
//		if (!projDir.exists())
//		{
//			projDir.mkdirs();
//			WriteLaunches(projDir, 30);
//		}
//
//		int left = ReadLaunches(projDir);
//		if (left > 0)
//			WriteLaunches(projDir, left - 1);
//
//		if (left == 5 || left == 3)
//		{
//			// show subscription dialog
//			showSubscriptionDialog();
//			return;
//		}
//	}
//
//	// writes number of launches left into memory
//	protected void WriteLaunches(File projDir, int left)
//	{
//		FileOutputStream fos = null;
//		try
//		{
//			fos = new FileOutputStream(projDir + "/left");
//			fos.write(left);
//			fos.close();
//		} catch (FileNotFoundException e)
//		{
//			e.printStackTrace();
//		} catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//	}
//
//	// reads number of launches left from memory
//	protected int ReadLaunches(File projDir)
//	{
//		int left = 0;
//		FileInputStream fis = null;
//		try
//		{
//			fis = new FileInputStream(projDir + "/left");
//			left = fis.read();
//			fis.close();
//		} catch (FileNotFoundException e)
//		{
//			e.printStackTrace();
//		} catch (IOException e)
//		{
//			e.printStackTrace();
//		}
//		return left;
//	}
//
//	public boolean checkLaunches(Mode mode)
//	{
//		// if mode free
//		if (mode.SKU == null)
//			return true;
//		if (mode.SKU.isEmpty())
//			return true;
//
//		// if all unlocked
//		if (unlockAllPurchased)
//			return true;
//
//		// if current mode unlocked
//		if (mode.SKU.equals("plugin_almalence_super"))
//		{
//			if (superPurchased || !CameraController.isUseSuperMode())
//				return true;
//		}
//		if (mode.SKU.equals("plugin_almalence_hdr"))
//		{
//			if (hdrPurchased)
//				return true;
//		}
//		if (mode.SKU.equals("plugin_almalence_video"))
//		{
//			if (hdrPurchased)
//				return true;
//		} else if (mode.SKU.equals("plugin_almalence_panorama_augmented"))
//		{
//			if (panoramaPurchased)
//				return true;
//		} else if (mode.SKU.equals("plugin_almalence_moving_burst"))
//		{
//			if (objectRemovalBurstPurchased)
//				return true;
//		} else if (mode.SKU.equals("plugin_almalence_groupshot"))
//		{
//			if (groupShotPurchased)
//				return true;
//		}
//
//		int launchesLeft = ApplicationScreen.thiz.getLeftLaunches(mode.modeID);
//		int id = ApplicationScreen.getAppResources().getIdentifier(
//				(CameraController.isUseHALv3() ? mode.modeNameHAL : mode.modeName), "string",
//				ApplicationScreen.thiz.getPackageName());
//		String modename = ApplicationScreen.getAppResources().getString(id);
//
//		if (0 == launchesLeft)// no more launches left
//		{
//			String left = String.format(getResources().getString(R.string.trial_finished), modename);
//			Toast toast = Toast.makeText(this, left, Toast.LENGTH_LONG);
//			toast.setGravity(Gravity.CENTER, 0, 0);
//			toast.show();
//
//			// show appstore for this mode
//			launchPurchase(100);
//			return false;
//		} else if ((10 == launchesLeft) || (20 == launchesLeft) || (5 >= launchesLeft))
//		{
//			// show appstore button and say that it cost money
//			String left = String.format(getResources().getString(R.string.trial_left), modename, launchesLeft);
//			Toast toast = Toast.makeText(this, left, Toast.LENGTH_LONG);
//			toast.setGravity(Gravity.CENTER, 0, 0);
//			toast.show();
//		}
//		return true;
//	}
//
//	protected boolean isInstalled(String packageName)
//	{
//		PackageManager pm = getPackageManager();
//		boolean installed = false;
//		try
//		{
//			pm.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES);
//			installed = true;
//		} catch (PackageManager.NameNotFoundException e)
//		{
//			installed = false;
//		}
//		return installed;
//	}
//
//	protected void showSubscriptionDialog()
//	{
//		final float density = getResources().getDisplayMetrics().density;
//
//		LinearLayout ll = new LinearLayout(this);
//		ll.setOrientation(LinearLayout.VERTICAL);
//		ll.setPadding((int) (10 * density), (int) (10 * density), (int) (10 * density), (int) (10 * density));
//
//		ImageView img = new ImageView(this);
//		img.setImageResource(R.drawable.store_subscription);
//		img.setAdjustViewBounds(true);
//		ll.addView(img);
//
//		TextView tv = new TextView(this);
//		tv.setText(ApplicationScreen.getAppResources().getString(R.string.subscriptionText));
//		tv.setWidth((int) (250 * density));
//		tv.setPadding((int) (4 * density), 0, (int) (4 * density), (int) (24 * density));
//		ll.addView(tv);
//
//		Button bNo = new Button(this);
//		bNo.setText(ApplicationScreen.getAppResources().getString(R.string.subscriptionNoText));
//		ll.addView(bNo);
//
//		Button bSubscribe = new Button(this);
//		bSubscribe.setText(ApplicationScreen.getAppResources().getString(R.string.subscriptionYesText));
//		ll.addView(bSubscribe);
//
//		final AlertDialog.Builder builder = new AlertDialog.Builder(this);
//		builder.setView(ll);
//		final AlertDialog dialog = builder.create();
//
//		bSubscribe.setOnClickListener(new OnClickListener()
//		{
//			public void onClick(View v)
//			{
//				purchasedUnlockAllSubscriptionYear();
//				dialog.dismiss();
//			}
//		});
//
//		bNo.setOnClickListener(new OnClickListener()
//		{
//			public void onClick(View v)
//			{
//				dialog.dismiss();
//			}
//		});
//
//		dialog.show();
//	}
//
//	protected boolean isABCUnlockedInstalled(Activity activity)
//	{
//		try
//		{
//			activity.getPackageManager().getInstallerPackageName("com.almalence.opencam_plus");
//		} catch (IllegalArgumentException e)
//		{
//			return false;
//		}
//
//		return true;
//	}
//
//	protected void callStoreForUnlocked(Activity activity)
//	{
//		try
//		{
//			Intent intent = new Intent(Intent.ACTION_VIEW);
//			intent.setData(Uri.parse("market://details?id=com.almalence.opencam_plus"));
//			activity.startActivity(intent);
//		} catch (ActivityNotFoundException e)
//		{
//			return;
//		}
//	}
//
//	// -+- -->
//
//	/************************ Billing ************************/
//	/*******************************************************/

	abstract protected void resetOrSaveSettings();	

//	public void switchingMode(boolean isModeSwitching)
//	{
//		switchingMode = isModeSwitching;
//	}
//
//	public boolean getSwitchingMode()
//	{
//		return switchingMode;
//	}
	
	@Override
	public Activity getMainActivity()
	{
		return thiz;
	}
	
	@Override
	public void stopApplication()
	{
		finish();
	}
	
	
	//Set/Get camera parameters preference
	
	//EXPOSURE COMPENSATION PREFERENCE
	@Override
	public void setEVPref(int iEv)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
		.putInt(ApplicationScreen.sEvPref, iEv).commit();
	}
	
	@Override
	public int  getEVPref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sEvPref, 0);
	}
	
	
	//SCENE MODE PREFERENCE
	@Override
	public void setSceneModePref(int iSceneMode)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
		.putInt(ApplicationScreen.sSceneModePref, iSceneMode).commit();
	}
	
	@Override
	public int  getSceneModePref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sSceneModePref, ApplicationScreen.sDefaultValue);
	}
	
	
	//WHITE BALANCE MODE PREFERENCE
	@Override
	public void setWBModePref(int iWB)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
		.putInt(ApplicationScreen.sWBModePref, iWB).commit();
	}
	
	@Override
	public int  getWBModePref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sWBModePref, ApplicationScreen.sDefaultValue);
	}
	
	
	//FOCUS MODE PREFERENCE
	@Override
	public void setFocusModePref(int iFocusMode)
	{
		String modeName = PluginManager.getInstance().getActiveModeID();
		String frontFocusMode = null;
		String backFocusMode = null;
		
		if(modeName.contains("video"))
		{
			frontFocusMode = ApplicationScreen.sFrontFocusModeVideoPref;
			backFocusMode = ApplicationScreen.sRearFocusModeVideoPref;
		}
		else
		{
			frontFocusMode = ApplicationScreen.sFrontFocusModePref;
			backFocusMode = ApplicationScreen.sRearFocusModePref;
		}
		
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
		.putInt(CameraController.isFrontCamera() ? frontFocusMode : backFocusMode, iFocusMode).commit();
	}
	
	@Override
	public int  getFocusModePref(int defaultMode)
	{
		String modeName = PluginManager.getInstance().getActiveModeID();
		String frontFocusMode = null;
		String backFocusMode = null;
		
		if(modeName.contains("video"))
		{
			frontFocusMode = ApplicationScreen.sFrontFocusModeVideoPref;
			backFocusMode = ApplicationScreen.sRearFocusModeVideoPref;
		}
		else
		{
			frontFocusMode = ApplicationScreen.sFrontFocusModePref;
			backFocusMode = ApplicationScreen.sRearFocusModePref;
		}
		
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(CameraController.isFrontCamera() ? frontFocusMode : backFocusMode, defaultMode);
	}
	
	//FLASH MODE PREFERENCE
	@Override
	public void setFlashModePref(int iFlashMode)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
		.putInt(ApplicationScreen.sFlashModePref, iFlashMode).commit();
	}
	
	@Override
	public int  getFlashModePref(int defaultMode)
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sFlashModePref, defaultMode);
	}
	
	
	//ISO MODE PREFERENCE
	@Override
	public void setISOModePref(int iISOMode)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putInt(ApplicationScreen.sISOPref, iISOMode).commit();
	}
	
	@Override
	public int  getISOModePref(int defaultMode)
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sISOPref, defaultMode);
	}
	
	
	@Override
	public int getAntibandingModePref()
	{
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString(ApplicationScreen.sAntibandingPref,
				"3"));
	}
	
	@Override
	public boolean getAELockPref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean(ApplicationScreen.sAELockPref, false);
	}
	
	@Override
	public boolean getAWBLockPref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean(ApplicationScreen.sAWBLockPref, false);
	}
}
