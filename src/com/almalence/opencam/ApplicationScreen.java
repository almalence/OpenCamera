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
import java.util.ArrayList;
import java.util.List;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.hardware.Camera.Area;
import android.media.AudioManager;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.StatFs;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Display;
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


/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.cameracontroller.Camera2Controller;
 import com.almalence.opencam_plus.ui.GLLayer;
 import com.almalence.opencam_plus.ui.GUI;
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.cameracontroller.Camera2Controller;
import com.almalence.opencam.ui.GLLayer;
import com.almalence.opencam.ui.GUI;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.sony.cameraremote.SimpleStreamSurfaceView;
import com.almalence.util.Util;

/***
 * ApplicationScreen - main activity screen with camera functionality
 * 
 * Passes all main events to PluginManager
 ***/

@SuppressWarnings("deprecation")
abstract public class ApplicationScreen extends Activity implements ApplicationInterface, View.OnClickListener,
		View.OnTouchListener, SurfaceHolder.Callback, Handler.Callback, Camera.ShutterCallback
{
	// >>Description
	// section with different global parameters available for everyone
	//
	// Camera parameters and possibly access to camera instance
	//
	// Global defines and others
	//
	// Description<<

	public static ApplicationScreen		instance;
	public Context						mainContext;
	protected Handler					messageHandler;

	// Interface to Camera2 camera and Old style camera
	protected CameraController			cameraController			= null;

	protected int						captureFormat				= CameraController.JPEG;

	public GUI							guiManager					= null;

	protected PluginManagerBase			pluginManager				= null;

	// OpenGL layer. May be used to allow capture plugins to draw overlaying
	// preview, such as night vision or panorama frames.
	protected GLLayer					glView;

	protected boolean					mPausing					= false;

	protected SurfaceHolder				surfaceHolder;
	protected SurfaceView				preview;
	protected Surface					mCameraSurface				= null;
	protected OrientationEventListener	orientListener;
	protected boolean					landscapeIsNormal			= false;
	protected boolean					surfaceCreated				= false;

	protected int						surfaceWidth				= 0;
	protected int						surfaceHeight				= 0;
	//
	protected int						surfaceLayoutWidth			= 0;
	protected int						surfaceLayoutHeight			= 0;

	// shared between activities
	// protected int imageWidth, imageHeight;
	protected int						previewWidth, previewHeight;

	protected CountDownTimer			screenTimer					= null;
	protected boolean					isScreenTimerRunning		= false;

	protected static boolean			wantLandscapePhoto			= false;
	protected int						orientationMain				= 0;
	protected int						orientationMainPrevious		= 0;

	protected SoundPlayer				shutterPlayer				= null;

	// Common preferences
	protected boolean					keepScreenOn				= false;

	protected List<Surface>				surfaceList;

	protected static boolean			mAFLocked					= false;
	// // shows if mode is currently switching
	protected boolean					switchingMode				= false;

	// >>Description
	// section with initialize, resume, start, stop procedures, preferences
	// access
	//
	// Initialize, stop etc depends on plugin type.
	//
	// Create main GUI controls and plugin specific controls.
	//
	// Description<<

	protected static boolean			isCreating					= false;
	protected static boolean			mApplicationStarted			= false;
	protected static boolean			mCameraStarted				= false;
	protected static boolean			isForceClose				= false;

	protected static final int			VOLUME_FUNC_SHUTTER			= 0;
	protected static final int			VOLUME_FUNC_EXPO			= 2;
	protected static final int			VOLUME_FUNC_NONE			= 3;

	protected static List<Area>			mMeteringAreaMatrix5		= new ArrayList<Area>();
	protected static List<Area>			mMeteringAreaMatrix4		= new ArrayList<Area>();
	protected static List<Area>			mMeteringAreaMatrix1		= new ArrayList<Area>();
	protected static List<Area>			mMeteringAreaCenter			= new ArrayList<Area>();
	protected static List<Area>			mMeteringAreaSpot			= new ArrayList<Area>();

	protected int						currentMeteringMode			= -1;

	public static String				sInitModeListPref			= "initModeListPref";

	public static String				sEvPref;
	public static String				sExposureTimeModePref;
	public static String				sRealExposureTimeOnPreviewPref;
	public static String				sExposureTimePref;
	public static String				sFocusDistanceModePref;
	public static String				sFocusDistancePref;
	public static String				sSceneModePref;
	public static String				sWBModePref;
	public static String				sColorTemperaturePref;
	public static String				sFrontFocusModePref;
	public static String				sFrontFocusModeVideoPref;
	public static String				sRearFocusModePref;
	public static String				sRearFocusModeVideoPref;
	public static String				sFrontColorEffectPref;
	public static String				sRearColorEffectPref;
	public static String				sFlashModePref;
	public static String				sISOPref;
	public static String				sMeteringModePref;
	public static String				sCameraModePref;

	public static String				sUseFrontCameraPref;

	public static String				sImageSizeRearPref;
	public static String				sImageSizeFrontPref;
	public static String				sImageSizeSonyRemotePref;

	public static String				sImageSizeMultishotBackPref;
	public static String				sImageSizeMultishotFrontPref;
	public static String				sImageSizeMultishotSonyRemotePref;

	public static String				sImageSizePanoramaBackPref;
	public static String				sImageSizePanoramaFrontPref;
	public static String				sImageSizeVideoBackPref;
	public static String				sImageSizeVideoFrontPref;
	public static String				sCaptureRAWPref;
	public static String				sJPEGQualityPref;
	public static String				sAntibandingPref;
	public static String				sAELockPref;
	public static String				sAWBLockPref;
	public static String				sSavePathPref;
	public static String				sExportNamePref;
	public static String				sExportNameSeparatorPref;
	public static String				sExportNamePrefixPref;
	public static String				sExportNamePostfixPref;
	public static String				sSaveToPref;
	public static String				sSortByDataPref;
	public static String				sEnableExifOrientationTagPref;
	public static String				sAdditionalRotationPref;
	public static String				sUseGeotaggingPref;

	public static String				sTimestampDate;
	public static String				sTimestampAbbreviation;
	public static String				sTimestampTime;
	public static String				sTimestampGeo;
	public static String				sTimestampSeparator;
	public static String				sTimestampCustomText;
	public static String				sTimestampColor;
	public static String				sTimestampFontSize;
	public static String				sExpoPreviewModePref;

	public static String				sDefaultModeName;
	public static int					sDefaultValue				= CameraParameters.SCENE_MODE_AUTO;
	public static int					sDefaultFocusValue				= CameraParameters.AF_MODE_CONTINUOUS_PICTURE;
	public static int					sDefaultFlashValue				= CameraParameters.FLASH_MODE_OFF;
	public static int					sDefaultISOValue				= CameraParameters.ISO_AUTO;
	public static int					sDefaultMeteringValue			= CameraParameters.meteringModeAuto;
	public static Long					lDefaultExposureTimeValue		= 33333333L;
	public static int					sDefaultColorEffectValue		= CameraParameters.COLOR_EFFECT_MODE_OFF;
	public static int					iDefaultColorTemperatureValue	= 6500;
	public static int					iMinColorTemperatureValue		= 1000;
	public static int					iMaxColorTemperatureValue		= 10000;

	private static File					forceFilename					= null;
	private static Uri					forceFilenameUri;
	
	private final static int			CAMERA_PERMISSION_CODE			= 0;
	private static boolean				cameraPermissionGranted			= true; //TODO: grand permissions on runtime

	public void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		sEvPref = getResources().getString(R.string.Preference_EvCompensationValue);
		sSceneModePref = getResources().getString(R.string.Preference_SceneModeValue);
		sWBModePref = getResources().getString(R.string.Preference_WBModeValue);
		sColorTemperaturePref = getResources().getString(R.string.Preference_ColorTemperatureValue);
		sFrontFocusModePref = getResources().getString(R.string.Preference_FrontFocusModeValue);
		sFrontFocusModeVideoPref = getResources().getString(R.string.Preference_FrontFocusModeVideoValue);
		sRearFocusModePref = getResources().getString(R.string.Preference_RearFocusModeValue);
		sRearFocusModeVideoPref = getResources().getString(R.string.Preference_RearFocusModeVideoValue);
		sFrontColorEffectPref = getResources().getString(R.string.Preference_FrontColorEffectValue);
		sRearColorEffectPref = getResources().getString(R.string.Preference_RearColorEffectValue);
		sFlashModePref = getResources().getString(R.string.Preference_FlashModeValue);
		sISOPref = getResources().getString(R.string.Preference_ISOValue);
		sMeteringModePref = getResources().getString(R.string.Preference_MeteringModeValue);
		sExposureTimePref = getResources().getString(R.string.Preference_ExposureTimeValue);
		sExposureTimeModePref = getResources().getString(R.string.Preference_ExposureTimeModeValue);
		sRealExposureTimeOnPreviewPref = getResources().getString(R.string.Preference_RealExposureTimeOnPreviewValue);
		sFocusDistancePref = getResources().getString(R.string.Preference_FocusDistanceValue);
		sFocusDistanceModePref = getResources().getString(R.string.Preference_FocusDistanceModeValue);
		sCameraModePref = getResources().getString(R.string.Preference_CameraModeValue);

		sUseFrontCameraPref = getResources().getString(R.string.Preference_UseFrontCameraValue);

		sImageSizeRearPref = getResources().getString(R.string.Preference_ImageSizeRearValue);
		sImageSizeFrontPref = getResources().getString(R.string.Preference_ImageSizeFrontValue);
		sImageSizeSonyRemotePref = getResources().getString(R.string.Preference_ImageSizeSonyRemoteValue);

		sImageSizeMultishotBackPref = getResources()
				.getString(R.string.Preference_ImageSizePrefSmartMultishotBackValue);
		sImageSizeMultishotFrontPref = getResources().getString(
				R.string.Preference_ImageSizePrefSmartMultishotFrontValue);
		sImageSizeMultishotSonyRemotePref = getResources().getString(
				R.string.Preference_ImageSizePrefSmartMultishotSonyRemoteValue);

		sImageSizePanoramaBackPref = getResources().getString(R.string.Preference_ImageSizePrefPanoramaBackValue);
		sImageSizePanoramaFrontPref = getResources().getString(R.string.Preference_ImageSizePrefPanoramaFrontValue);

		sImageSizeVideoBackPref = getResources().getString(R.string.Preference_ImageSizePrefVideoBackValue);
		sImageSizeVideoFrontPref = getResources().getString(R.string.Preference_ImageSizePrefVideoFrontValue);

		sCaptureRAWPref = getResources().getString(R.string.Preference_CaptureRAWValue);

		sJPEGQualityPref = getResources().getString(R.string.Preference_JPEGQualityCommonValue);

		sAntibandingPref = getResources().getString(R.string.Preference_AntibandingValue);

		sExportNamePref = getResources().getString(R.string.Preference_ExportNameValue);
		sExportNameSeparatorPref = getResources().getString(R.string.Preference_ExportNameSeparatorValue);
		sExportNamePrefixPref = getResources().getString(R.string.Preference_SavePathPrefixValue);
		sExportNamePostfixPref = getResources().getString(R.string.Preference_SavePathPostfixValue);
		sSavePathPref = getResources().getString(R.string.Preference_SavePathValue);
		sSaveToPref = getResources().getString(R.string.Preference_SaveToValue);
		sSortByDataPref = getResources().getString(R.string.Preference_SortByDataValue);
		sEnableExifOrientationTagPref = getResources().getString(R.string.Preference_EnableExifTagOrientationValue);
		sAdditionalRotationPref = getResources().getString(R.string.Preference_AdditionalRotationValue);
		sUseGeotaggingPref = getResources().getString(R.string.Preference_UseGeotaggingValue);

		sTimestampDate = getResources().getString(R.string.Preference_TimestampDateValue);
		sTimestampAbbreviation = getResources().getString(R.string.Preference_TimestampAbbreviationValue);
		sTimestampTime = getResources().getString(R.string.Preference_TimestampTimeValue);
		sTimestampGeo = getResources().getString(R.string.Preference_TimestampGeoValue);
		sTimestampSeparator = getResources().getString(R.string.Preference_TimestampSeparatorValue);
		sTimestampCustomText = getResources().getString(R.string.Preference_TimestampCustomTextValue);
		sTimestampColor = getResources().getString(R.string.Preference_TimestampColorValue);
		sTimestampFontSize = getResources().getString(R.string.Preference_TimestampFontSizeValue);

		sAELockPref = getResources().getString(R.string.Preference_AELockValue);
		sAWBLockPref = getResources().getString(R.string.Preference_AWBLockValue);

		sExpoPreviewModePref = getResources().getString(R.string.Preference_ExpoBracketingPreviewModePref);

		sDefaultModeName = getResources().getString(R.string.Preference_DefaultModeName);

		mainContext = this.getBaseContext();
		messageHandler = new Handler(this);
		instance = this;

		requestWindowFeature(Window.FEATURE_NO_TITLE);
		// ensure landscape orientation
		setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
		// set to fullscreen
		getWindow().addFlags(
				WindowManager.LayoutParams.FLAG_FULLSCREEN | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
						| WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
		
		
//		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
//			grandPermissions();

		// set some common view here
//		setContentView(R.layout.opencamera_main_layout);

		createPluginManager();
		duringOnCreate();

		try
		{
			cameraController = CameraController.getInstance();
		} catch (VerifyError exp)
		{
			Log.e("ApplicationScreen", exp.getMessage());
		}
		CameraController.onCreate(ApplicationScreen.instance, ApplicationScreen.instance, pluginManager,
				ApplicationScreen.instance.messageHandler);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		keepScreenOn = prefs.getBoolean("keepScreenOn", false);

		// set preview, on click listener and surface buffers
//		findViewById(R.id.SurfaceView02).setVisibility(View.GONE);
//		preview = (SurfaceView) this.findViewById(R.id.SurfaceView01);
//		preview.setOnClickListener(this);
//		preview.setOnTouchListener(this);
//		preview.setKeepScreenOn(true);
//
//		surfaceHolder = preview.getHolder();
//		surfaceHolder.addCallback(this);

		orientListener = new OrientationEventListener(this)
		{
			@Override
			public void onOrientationChanged(int orientation)
			{
				// figure landscape or portrait
				if (ApplicationScreen.instance.landscapeIsNormal)
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
				boolean isVideoRecording = PreferenceManager.getDefaultSharedPreferences(
						ApplicationScreen.getMainContext()).getBoolean("videorecording", false);
				if (isVideoRecording || keepScreenOn)
				{
					// restart timer
					screenTimer.start();
					isScreenTimerRunning = true;
					if (preview != null)
					{
						preview.setKeepScreenOn(true);
					}
					return;
				}
				if (preview != null)
				{
					preview.setKeepScreenOn(keepScreenOn);
				}
				isScreenTimerRunning = false;
			}
		};
		screenTimer.start();
		isScreenTimerRunning = true;

		if (this.getIntent().getAction() != null)
		{
			if (this.getIntent().getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE))
			{
				try
				{
					forceFilenameUri = this.getIntent().getExtras().getParcelable(MediaStore.EXTRA_OUTPUT);
					ApplicationScreen.setForceFilename(new File(((Uri) forceFilenameUri).getPath()));
					if (ApplicationScreen.getForceFilename().getAbsolutePath().equals("/scrapSpace"))
					{
						ApplicationScreen.setForceFilename(new File(Environment.getExternalStorageDirectory()
								.getAbsolutePath() + "/mms/scrapSpace/.temp.jpg"));
						new File(ApplicationScreen.getForceFilename().getParent()).mkdirs();
					}
				} catch (Exception e)
				{
					ApplicationScreen.setForceFilename(null);
				}
			} else
			{
				ApplicationScreen.setForceFilename(null);
			}
		} else
		{
			ApplicationScreen.setForceFilename(null);
		}

		afterOnCreate();
	}
	
	@TargetApi(23)
	protected void grandPermissions()
	{
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED)
		{
			cameraPermissionGranted = false;
		    // Should we show an explanation?
		    if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA))
		    {
		
		        // Show an explanation to the user *asynchronously* -- don't block
		        // this thread waiting for the user's response! After the user
		        // sees the explanation, try again to request the permission.
		
		    }
		    else 
		    {
		
		        // No explanation needed, we can request the permission.
		
		        ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, ApplicationScreen.CAMERA_PERMISSION_CODE);
		
		        // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
		        // app-defined int constant. The callback method gets the
		        // result of the request.
		    }
		}
		else
			cameraPermissionGranted = true;
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults)
	{
	    switch (requestCode)
	    {
	        case ApplicationScreen.CAMERA_PERMISSION_CODE:
	        {
	            // If request is cancelled, the result arrays are empty.
	            if (grantResults.length > 0
	                && grantResults[0] == PackageManager.PERMISSION_GRANTED)
	            {

	            	cameraPermissionGranted = true;
	                // permission was granted, yay! Do the
	                // contacts-related task you need to do.
	            	if(CameraController.openCameraWaiting)
	            		Camera2Controller.openCameraCamera2();

	            }
	            else {

	                // permission denied, boo! Disable the
	                // functionality that depends on this permission.
	            }
	            return;
	        }

	        // other 'case' lines to check for other
	        // permissions this app might request
	    }
	}
	
	public static boolean isCameraPermissionGranted()
	{
		return cameraPermissionGranted;
	}

	abstract protected void createPluginManager();

	public static PluginManagerBase getPluginManager()
	{
		return ApplicationScreen.instance.pluginManager;
	}

	// At this point CameraController, GUIManager are not created yet.
	// Use this method to initialize some shared preferences or do any other
	// logic that isn't depended from OpenCamera core's objects.
	abstract protected void duringOnCreate();

	// At this point all OpenCamera main objects are created.
	abstract protected void afterOnCreate();

	public static Context getMainContext()
	{
		return instance.mainContext;
	}

	public static Handler getMessageHandler()
	{
		return instance.messageHandler;
	}

	public static CameraController getCameraController()
	{
		return instance.cameraController;
	}

	public static GUI getGUIManager()
	{
		return instance.guiManager;
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
	
	@Override
	abstract public MediaRecorder getMediaRecorder();

	public static SurfaceHolder getPreviewSurfaceHolder()
	{
		return instance.surfaceHolder;
	}

	public static SurfaceView getPreviewSurfaceView()
	{
		return instance.preview;
	}

	public static int getCaptureFormat()
	{
		return instance.captureFormat;
	}

	public static void setCaptureFormat(int capture)
	{
		if (!CameraController.isRemoteCamera()) {			
			if(CameraController.isCaptureFormatSupported(capture))
			{
				instance.captureFormat = capture;
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					CameraController.setCaptureFormat(capture);
			}
			else if(CameraController.isCaptureFormatSupported(CameraController.JPEG))
			{
				instance.captureFormat = CameraController.JPEG;
				
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
					CameraController.setCaptureFormat(CameraController.JPEG);
			}
		} else {
			instance.captureFormat = CameraController.JPEG;
		}
	}

	public static int getPreviewSurfaceLayoutWidth()
	{
		return instance.surfaceLayoutWidth;
	}

	public static int getPreviewSurfaceLayoutHeight()
	{
		return instance.surfaceLayoutHeight;
	}

	public static void setPreviewSurfaceLayoutWidth(int width)
	{
		instance.surfaceLayoutWidth = width;
	}

	public static void setPreviewSurfaceLayoutHeight(int height)
	{
		instance.surfaceLayoutHeight = height;
	}

	public static void setSurfaceHolderSize(int width, int height)
	{
		if (instance.surfaceHolder != null)
		{
			instance.surfaceWidth = width;
			instance.surfaceHeight = height;
			instance.surfaceHolder.setFixedSize(width, height);
		}
	}

	abstract public int getImageSizeIndex();

	abstract public int getMultishotImageSizeIndex();

	abstract public boolean isShutterSoundEnabled();

	abstract public int isShotOnTap();

	public static int getOrientation()
	{
		return instance.orientationMain;
	}

	public static int getMeteringMode()
	{
		return instance.currentMeteringMode;
	}

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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		int cameraSelected = prefs.getInt(ApplicationScreen.sCameraModePref, 0);
		if (cameraSelected == CameraController.getNumberOfCameras() - 1)
		{
			prefs.edit().putInt(ApplicationScreen.sCameraModePref, 0).commit();
			ApplicationScreen.getGUIManager().setCameraModeGUI(0);
		}

		CameraController.onStart();
		ApplicationScreen.getGUIManager().onStart();
		ApplicationScreen.getPluginManager().onStart();
	}

	@Override
	protected void onStop()
	{
		super.onStop();
		onApplicationStop();
	}

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
	}

	@TargetApi(21)
	abstract protected void stopImageReaders();

	abstract protected void stopRemotePreview();

	@Override
	protected void onDestroy()
	{
		super.onDestroy();

		onApplicationDestroy();
	}

	protected void onApplicationDestroy()
	{
		ApplicationScreen.getGUIManager().onDestroy();
		ApplicationScreen.getPluginManager().onDestroy();
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
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen
							.getMainContext());

					preview.setKeepScreenOn(true);

					captureFormat = CameraController.JPEG;

					String modeId = ApplicationScreen.getPluginManager().getActiveModeID();
					if (CameraController.isRemoteCamera() && !(modeId.contains("single") || modeId.contains("video")))
					{
						prefs.edit().putInt(ApplicationScreen.sCameraModePref, 0).commit();
						CameraController.setCameraIndex(0);
						guiManager.setCameraModeGUI(0);
					}

					CameraController.onResume();
					ApplicationScreen.getGUIManager().onResume();
					ApplicationScreen.getPluginManager().onResume();
					
					ApplicationScreen.instance.mPausing = false;

					if (!CameraController.isRemoteCamera())
					{
						// set preview, on click listener and surface buffers
						findViewById(R.id.SurfaceView02).setVisibility(View.GONE);
						preview = (SurfaceView) findViewById(R.id.SurfaceView01);
						preview.setOnClickListener(ApplicationScreen.this);
						preview.setOnTouchListener(ApplicationScreen.this);
						preview.setKeepScreenOn(true);

						surfaceHolder = preview.getHolder();
						surfaceHolder.addCallback(ApplicationScreen.this);

						// One of device camera is selected.
						if (CameraController.isUseCamera2())
						{
							ApplicationScreen.instance.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
							CameraController.setupCamera(null, true);

							if (glView != null)
								glView.onResume();
						} else if ((surfaceCreated && (!CameraController.isCameraCreated())))
						{
							ApplicationScreen.instance.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
							CameraController.setupCamera(surfaceHolder, true);

							if (glView != null)
							{
								glView.onResume();
							}
						}
					} else
					{
						sonyCameraSelected();
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
		if (!CameraController.isRemoteCamera())
		{
			if (CameraController.isUseCamera2() || ApplicationScreen.getPluginManager().isRestart())
			{
				new CountDownTimer(100, 100)
				{
					public void onTick(long millisUntilFinished)
					{
						// Not used
					}

					public void onFinish()
					{
						ApplicationScreen.getPluginManager().switchMode(
								ConfigParser.getInstance().getMode(
										ApplicationScreen.getPluginManager().getActiveModeID()));
					}
				}.start();
			} else
			{
				// Need this for correct exposure control state, after switching
				// DRO-on/DRO-off in single mode.
				guiManager.onPluginsInitialized();
			}
		} else
		{
			ApplicationScreen.getGUIManager().setCameraModeGUI(0);
			ApplicationScreen.instance.pauseMain();
			ApplicationScreen.instance.switchingMode(false);
			ApplicationScreen.instance.resumeMain();
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
		ApplicationScreen.getPluginManager().onPause(true);

		orientListener.disable();

		this.mPausing = true;

		this.hideOpenGLLayer();

		if (screenTimer != null)
		{
			if (isScreenTimerRunning)
				screenTimer.cancel();
			isScreenTimerRunning = false;
		}

		CameraController.onPause(true);

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
			ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_SURFACE_CONFIGURED, 0);
			isCameraConfiguring = false;
		} else if (!isCreating)
		{
			new CountDownTimer(50, 50)
			{
				public void onTick(long millisUntilFinished)
				{
					// Not used
				}

				public void onFinish()
				{
					if (!ApplicationScreen.instance.mPausing && surfaceCreated && (!CameraController.isCameraCreated()))
					{
						ApplicationScreen.instance.findViewById(R.id.mainLayout2).setVisibility(View.VISIBLE);
						if (!CameraController.isRemoteCamera())
						{
							if (!CameraController.isUseCamera2())
							{
								CameraController.setupCamera(holder, true);
							} else
								messageHandler.sendEmptyMessage(ApplicationInterface.MSG_SURFACE_READY);
						} else
						{
							// Sony camera
						}
					}
				}
			}.start();
		}
	}

	public void setCameraImageSizeIndex(int captureIndex, boolean init)
	{
		CameraController.setCameraImageSizeIndex(captureIndex);
	}

	// Used if some modes want to set special image size
	@Override
	public void setSpecialImageSizeIndexPref(int iIndex)
	{
	}

	@Override
	public String getSpecialImageSizeIndexPref()
	{
		return "-1";
	}

	// Method used only in Almalence's multishot modes
	abstract public int selectImageDimensionMultishot();

	@Override
	public void addSurfaceCallback()
	{
		instance.surfaceHolder.addCallback(instance);
	}

	boolean	isCameraConfiguring		= false;
	
	//Is used to implement google's advice how to prevent surfaceView bug in Android 6
	//Will be removed when google will fix the bug
	boolean	isSurfaceConfiguring	= false; 

	@Override
	public void configureCamera(boolean createGUI)
	{
		CameraController.updateCameraFeatures();

		// ----- Select preview dimensions with ratio correspondent to
		// full-size image
		ApplicationScreen.getPluginManager().setCameraPreviewSize();
		// prepare list of surfaces to be used in capture requests
		if (!CameraController.isRemoteCamera())
		{
			if (CameraController.isUseCamera2())
				configureCamera2Camera(captureFormat);
			else
			{
				Camera.Size sz = CameraController.getCameraParameters().getPreviewSize();

				Log.e("ApplicationScreen", "Viewfinder preview size: " + sz.width + "x" + sz.height);
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

		if (createGUI)
		{
			ApplicationScreen.getPluginManager().onGUICreate();
			ApplicationScreen.getGUIManager().onGUICreate();
		}
	}

	protected void onCameraConfigured()
	{
		ApplicationScreen.getPluginManager().setupCameraParameters();

		if (!CameraController.isRemoteCamera())
		{
			if (!CameraController.isUseCamera2())
			{
				Camera.Parameters cp = CameraController.getCameraParameters();
				try
				{
					// Nexus 5 is giving preview which is too dark without this
					if (CameraController.isNexus5)
					{
						cp.setPreviewFpsRange(7000, 30000);
						CameraController.setCameraParameters(cp);
						cp = CameraController.getCameraParameters();
					}
				} catch (RuntimeException e)
				{
					Log.d("ApplicationScreen",
							"ApplicationScreen.onCameraConfigured() unable to setParameters " + e.getMessage());
				}

				if (cp != null)
				{
					SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen
							.getMainContext());
					int antibanding = Integer.parseInt(prefs.getString(ApplicationScreen.sAntibandingPref, "3"));
					switch (antibanding)
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

			if (!CameraController.isUseCamera2())
			{
				guiManager.onCameraCreate();
				ApplicationScreen.getPluginManager().onCameraParametersSetup();
				guiManager.onPluginsInitialized();
			}
		} else
		{
			guiManager.onCameraCreate();
			ApplicationScreen.getPluginManager().onCameraParametersSetup();
			guiManager.onPluginsInitialized();
		}

		// ----- Start preview and setup frame buffer if needed

		// call camera release sequence from onPause somewhere ???
		new CountDownTimer(10, 10)
		{
			@Override
			public void onFinish()
			{
				if (!CameraController.isRemoteCamera())
				{
					if (!CameraController.isUseCamera2())
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
							e.printStackTrace();
							Toast.makeText(ApplicationScreen.instance, "Unable to start camera", Toast.LENGTH_LONG)
									.show();
							return;
						}

						CameraController.setPreviewCallbackWithBuffer();
					} else
					{
						guiManager.onCameraCreate();
						ApplicationScreen.getPluginManager().onCameraParametersSetup();
						guiManager.onPluginsInitialized();
					}
				} else
				{
					guiManager.onCameraCreate();
					ApplicationScreen.getPluginManager().onCameraParametersSetup();
					guiManager.onPluginsInitialized();
				}

				ApplicationScreen.getPluginManager().onCameraSetup();
				guiManager.onCameraSetup();
				ApplicationScreen.mApplicationStarted = true;

				if (ApplicationScreen.isForceClose)
					ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_APPLICATION_STOP, 0);
			}

			@Override
			public void onTick(long millisUntilFinished)
			{
				// Not used
			}
		}.start();
	}

	@TargetApi(21)
	protected void configureCamera2Camera(int captureFormat)
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
		// ----- Find 'normal' orientation of the device

		Display display = ((WindowManager) this.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int rotation = display.getRotation();
		if ((rotation == Surface.ROTATION_90) || (rotation == Surface.ROTATION_270))
			landscapeIsNormal = true; // false; - if landscape view orientation
										// set for ApplicationScreen
		else
			landscapeIsNormal = false;

		surfaceCreated = true;

		mCameraSurface = surfaceHolder.getSurface();
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

	// Probably used only by Panorama plugin. Added to avoid non direct
	// interface (message/handler)
	public static void takePicture()
	{
		ApplicationScreen.getPluginManager().takePicture();
	}

	@Override
	public void captureFailed()
	{
		ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION);
		ApplicationScreen.instance.muteShutter(false);
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
			return new CameraController.Size(preview.getWidth(), preview.getHeight());
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
	public boolean onKeyUp(int keyCode, KeyEvent event)
	{
		if (onKeyUpEvent(keyCode, event))
			return true;

		return super.onKeyUp(keyCode, event);
	}

	abstract boolean onKeyUpEvent(int keyCode, KeyEvent event);

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event)
	{
		if (onKeyDownEvent(keyCode, event))
			return true;

		return super.onKeyDown(keyCode, event);
	}

	abstract boolean onKeyDownEvent(int keyCode, KeyEvent event);

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
		ApplicationScreen.getPluginManager().onShutter();
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
		case ApplicationInterface.MSG_CAMERA_CONFIGURED:
			onCameraConfigured();
			break;
		case ApplicationInterface.MSG_CAMERA_OPENED:
			if (mCameraStarted)
				break;
		case ApplicationInterface.MSG_SURFACE_READY:
			{
				String modeName = ApplicationScreen.getPluginManager().getActiveModeID();
				if (!CameraController.isRemoteCamera())
				{
					// if both surface is created and camera device is opened
					// - ready to set up preview and other things
					// if (surfaceCreated && (Camera2.getCamera2() != null))
					if (surfaceCreated)
					{
						configureCamera(!CameraController.isUseCamera2() || modeName.contains("video")
								|| (CameraController.isNexus6 && modeName.contains("preshot"))
								|| (CameraController.isFlex2 && (modeName.contains("hdrmode") || modeName.contains("expobracketing"))));
						mCameraStarted = true;
					}
				} else
				{
					if (!modeName.contains("video"))
					{
						CameraController.populateCameraDimensionsSonyRemote();
						ApplicationScreen.getPluginManager().selectImageDimension();
					}
					configureCamera(true);
				}
			}
			break;
		case ApplicationInterface.MSG_SURFACE_CONFIGURED:
			{
				createCaptureSession();
				ApplicationScreen.getGUIManager().onGUICreate();
				ApplicationScreen.getPluginManager().onGUICreate();
				mCameraStarted = true;
			}
			break;
		case ApplicationInterface.MSG_CAMERA_STOPED:
			mCameraStarted = false;
			break;
		default:
			ApplicationScreen.getPluginManager().handleMessage(msg);
			break;
		}

		return true;
	}

	public void menuButtonPressed()
	{
		ApplicationScreen.getPluginManager().menuButtonPressed();
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
		AudioManager mgr = (AudioManager) ApplicationScreen.instance.getSystemService(ApplicationScreen.AUDIO_SERVICE);
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
		} else
			return true;
	}

	@Override
	public void setCameraPreviewSize(int iWidth, int iHeight)
	{
		if (!CameraController.isRemoteCamera())
		{
			if (CameraController.isUseCamera2())
			{
				setSurfaceHolderSize(iWidth, iHeight);
				setPreviewWidth(iWidth);
				setPreviewHeight(iHeight);
			}
		} else
		{
			setPreviewWidth(iWidth);
			setPreviewHeight(iHeight);
		}

		CameraController.setCameraPreviewSize(new CameraController.Size(iWidth, iHeight));
	}

	public static int getPreviewWidth()
	{
		return instance.previewWidth;
	}

	public static void setPreviewWidth(int iWidth)
	{
		instance.previewWidth = iWidth;
	}

	public static int getPreviewHeight()
	{
		return instance.previewHeight;
	}

	public static void setPreviewHeight(int iHeight)
	{
		instance.previewHeight = iHeight;
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
		return ApplicationScreen.instance.getResources();
	}

	abstract protected void resetOrSaveSettings();

	public void switchingMode(boolean isModeSwitching)
	{
		switchingMode = isModeSwitching;
	}

	public boolean getSwitchingMode()
	{
		return switchingMode;
	}

	@Override
	abstract public Activity getMainActivity();

	@Override
	public void stopApplication()
	{
		finish();
	}

	// Set/Get camera parameters preference

	// EXPOSURE COMPENSATION PREFERENCE
	@Override
	public void setEVPref(int iEv)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putInt(ApplicationScreen.sEvPref, iEv)
				.commit();
	}

	@Override
	public int getEVPref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sEvPref, 0);
	}

	// SCENE MODE PREFERENCE
	@Override
	public void setSceneModePref(int iSceneMode)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
				.putInt(ApplicationScreen.sSceneModePref, iSceneMode).commit();
	}

	@Override
	public int getSceneModePref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sSceneModePref,
				ApplicationScreen.sDefaultValue);
	}

	// WHITE BALANCE MODE PREFERENCE
	@Override
	public void setWBModePref(int iWB)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putInt(ApplicationScreen.sWBModePref, iWB)
				.commit();
	}

	@Override
	public int getWBModePref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sWBModePref,
				ApplicationScreen.sDefaultValue);
	}
	
	
	@Override
	public void setColorTemperature(int iTemp)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putInt(ApplicationScreen.sColorTemperaturePref, iTemp)
		.commit();
	}
	
	@Override
	public int getColorTemperature()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(MainScreen.sColorTemperaturePref,
				ApplicationScreen.iDefaultColorTemperatureValue);
	}

	// FOCUS MODE PREFERENCE
	@Override
	public void setFocusModePref(int iFocusMode)
	{
		String modeName = ApplicationScreen.getPluginManager().getActiveModeID();
		String frontFocusMode = null;
		String backFocusMode = null;

		if (modeName.contains("video"))
		{
			frontFocusMode = ApplicationScreen.sFrontFocusModeVideoPref;
			backFocusMode = ApplicationScreen.sRearFocusModeVideoPref;
		} else
		{
			frontFocusMode = ApplicationScreen.sFrontFocusModePref;
			backFocusMode = ApplicationScreen.sRearFocusModePref;
		}

		if (!CameraController.isRemoteCamera())
		{
			PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
					.putInt(CameraController.isFrontCamera() ? frontFocusMode : backFocusMode, iFocusMode).commit();
		} else
		{
			// Sony cmaera
		}
	}

	@Override
	public int getFocusModePref(int defaultMode)
	{
		String modeName = ApplicationScreen.getPluginManager().getActiveModeID();
		String frontFocusMode = null;
		String backFocusMode = null;

		if (modeName.contains("video"))
		{
			frontFocusMode = ApplicationScreen.sFrontFocusModeVideoPref;
			backFocusMode = ApplicationScreen.sRearFocusModeVideoPref;
		} else
		{
			frontFocusMode = ApplicationScreen.sFrontFocusModePref;
			backFocusMode = ApplicationScreen.sRearFocusModePref;
		}

		if (!CameraController.isRemoteCamera())
		{
			return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(
					CameraController.isFrontCamera() ? frontFocusMode : backFocusMode, defaultMode);
		} else
		{
			// Sony camera
			return 0;
		}
	}

	// FLASH MODE PREFERENCE
	@Override
	public void setFlashModePref(int iFlashMode)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit()
				.putInt(ApplicationScreen.sFlashModePref, iFlashMode).commit();
	}

	@Override
	public int getFlashModePref(int defaultMode)
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sFlashModePref,
				defaultMode);
	}

	// ISO MODE PREFERENCE
	@Override
	public void setISOModePref(int iISOMode)
	{
		PreferenceManager.getDefaultSharedPreferences(mainContext).edit().putInt(ApplicationScreen.sISOPref, iISOMode)
				.commit();
	}

	@Override
	public int getISOModePref(int defaultMode)
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(ApplicationScreen.sISOPref,
				defaultMode);
	}

	@Override
	public int getAntibandingModePref()
	{
		return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString(
				ApplicationScreen.sAntibandingPref, "3"));
	}
	
	@Override
	public int getColorEffectPref()
	{
		try
		{
			return Integer.parseInt(PreferenceManager.getDefaultSharedPreferences(mainContext).getString(
					CameraController.isFrontCamera() ? ApplicationScreen.sRearColorEffectPref
							: ApplicationScreen.sFrontColorEffectPref, String
							.valueOf(ApplicationScreen.sDefaultColorEffectValue)));
		} catch (Exception e)
		{
			return (PreferenceManager.getDefaultSharedPreferences(mainContext).getInt(CameraController.isFrontCamera() ? ApplicationScreen.sRearColorEffectPref
					: ApplicationScreen.sFrontColorEffectPref, ApplicationScreen.sDefaultColorEffectValue));
		}
	}

	@Override
	public boolean getAELockPref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean(ApplicationScreen.sAELockPref,
				false);
	}

	@Override
	public boolean getAWBLockPref()
	{
		return PreferenceManager.getDefaultSharedPreferences(mainContext).getBoolean(ApplicationScreen.sAWBLockPref,
				false);
	}

	public static Uri getForceFilenameURI()
	{
		return ApplicationScreen.forceFilenameUri;
	}

	public static File getForceFilename()
	{
		return ApplicationScreen.forceFilename;
	}

	public static void setForceFilename(File fileName)
	{
		ApplicationScreen.forceFilename = fileName;
	}

	public void sonyCameraSelected()
	{
		findViewById(R.id.SurfaceView01).setVisibility(View.GONE);
		preview = (SurfaceView) this.findViewById(R.id.SurfaceView02);
		surfaceHolder = preview.getHolder();
		surfaceHolder.addCallback(this);

		preview.setVisibility(View.VISIBLE);
		preview.setOnClickListener(this);
		preview.setOnTouchListener(this);
		preview.setKeepScreenOn(true);

		CameraController.setupCamera(preview.getHolder(), !switchingMode);
	}
}
