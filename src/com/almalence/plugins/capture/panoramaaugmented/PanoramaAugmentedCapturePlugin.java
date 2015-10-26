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

package com.almalence.plugins.capture.panoramaaugmented;

import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Point;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.RelativeLayout;
import android.widget.Toast;
import android.hardware.camera2.CaptureResult;

import com.almalence.SwapHeap;
import com.almalence.plugins.capture.panoramaaugmented.AugmentedPanoramaEngine.AugmentedFrameTaken;
import com.almalence.ui.RotateImageView;
import com.almalence.ui.Switch.Switch;
import com.almalence.util.HeapUtil;

/* <!-- +++
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.ui.GUI.CameraParameter;
 import com.almalence.opencam_plus.ConfigParser;
 import com.almalence.opencam_plus.ApplicationInterface;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.ConfigParser;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.GUI.CameraParameter;

//-+- -->

public class PanoramaAugmentedCapturePlugin extends PluginCapture
{
	private static final String			TAG								= "Almalence";

	private static final String			PREFERENCES_KEY_USE_DEVICE_GYRO	= "pref_plugin_capture_panoramaaugmented_usehardwaregyro";

	private AugmentedPanoramaEngine		engine;

	private final static int			MIN_HEIGHT_SUPPORTED			= 640;
	private final static List<Point>	ResolutionsPictureSizesList		= new ArrayList<Point>();
	
	private static boolean 				takingAlready = false;
	
	private boolean	camera2Preference;
	
	public static List<Point> getResolutionsPictureSizeslist()
	{
		return ResolutionsPictureSizesList;
	}

	public static List<String> getResolutionsPictureIndexesList()
	{
		return ResolutionsPictureIdxesList;
	}

	public static List<String> getResolutionsPictureNamesList()
	{
		return ResolutionsPictureNamesList;
	}

	private final static List<String>	ResolutionsPictureIdxesList	= new ArrayList<String>();
	private final static List<String>	ResolutionsPictureNamesList	= new ArrayList<String>();

	public static int					prefResolution;
	private boolean						prefHardwareGyroscope;

	private boolean						prefMemoryRelax				= false;

	private float						viewAngleX					= 55.4f;
	private float						viewAngleY					= 42.7f;

	private SensorManager				sensorManager;
	private Sensor						sensorGravity;
	private Sensor						sensorAccelerometer;
	private static Sensor				sensorGyroscope;
	private VfGyroSensor				sensorSoftGyroscope			= null;
	private boolean						remapOrientation;

	private AugmentedRotationListener	rotationListener;

	private int							pictureWidth;
	private int							pictureHeight;
	private int							previewWidth				= -1;
	private int							previewHeight				= -1;

	private volatile boolean			isFirstFrame				= false;
	private volatile boolean 			glContextCreated 			= false;

	private volatile boolean			coordsRecorded;
	private volatile boolean			previewRestartFlag;

	private boolean						showGyroWarnOnce			= false;

	private int							aewblock					= 1;
	private boolean						aeLockedByPanorama			= false;
	private boolean						wbLockedByPanorama			= false;

	private static String				sModePref					= "panoramaMode";
	private static String				sMemoryPref;
	private static String				sFrameOverlapPref;
	private static String				sAELockPref;

	private Switch						modeSwitcher;
	private boolean						modeSweep;
	private boolean						focused						= false;
	
	private RotateImageView				stopPanoramaButton;

	public PanoramaAugmentedCapturePlugin()
	{
		super("com.almalence.plugins.panoramacapture_augmented", R.xml.preferences_capture_panoramaaugmented, 0, 0,
				null);

		this.inCapture = false;

		this.sensorManager = (SensorManager) ApplicationScreen.getMainContext().getSystemService(Context.SENSOR_SERVICE);
		this.sensorGravity = this.sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
		this.sensorAccelerometer = this.sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorGyroscope = this.sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
	}

	private void init()
	{
		PanoramaAugmentedCapturePlugin.scanResolutions();

		this.getPrefs();

		this.checkCoordinatesRemapRequired();

		this.rotationListener = new AugmentedRotationListener(this.remapOrientation, !this.prefHardwareGyroscope);

		initSensors();
	}

	private void deinit()
	{
		deinitSensors();
	}

	private void initSensors()
	{
		if (this.prefHardwareGyroscope)
		{
			this.sensorManager.registerListener(this.rotationListener, sensorGyroscope,
					SensorManager.SENSOR_DELAY_GAME);
		} else
		{
			if (this.sensorSoftGyroscope == null)
			{
				this.sensorSoftGyroscope = new VfGyroSensor(null);
			}
			this.sensorSoftGyroscope.open();
			this.sensorSoftGyroscope.SetListener(this.rotationListener);
		}

		// on some devices accelerometer events are more frequent
		// on some devices gravity events are more frequent
		// we'll use both the same way to get updates as fast as possible
		this.sensorManager.registerListener(this.rotationListener, this.sensorAccelerometer,
				SensorManager.SENSOR_DELAY_GAME);

		// some devices do not like gravity sensor
		// e.g. on Ascend P2/P6 the GL layer will disappear if gravity sensor is
		// registered
		// (and no data will arrive from it either)
		// so, use gravity sensor only on models where accelerometer sensor is
		// no good
		
		// Accelerometer on LG G2 is very
		// slow (D80x models)
		if (CameraController.isG2) 
		{
			this.sensorManager.registerListener(this.rotationListener, this.sensorGravity,
					SensorManager.SENSOR_DELAY_GAME);
		}
		this.rotationListener.setReceiver(this.engine);
		this.rotationListener.setUpdateDrift(true);
	}

	private void deinitSensors()
	{
		if (this.prefHardwareGyroscope)
		{
			this.sensorManager.unregisterListener(this.rotationListener, sensorGyroscope);
		} else
		{
			if (null != this.sensorSoftGyroscope)
				this.sensorSoftGyroscope.SetListener(null);
		}

		this.sensorManager.unregisterListener(this.rotationListener, this.sensorAccelerometer);
		this.sensorManager.unregisterListener(this.rotationListener, this.sensorGravity);
	}

	public static void onDefaultSelectResolutons()
	{
		PanoramaAugmentedCapturePlugin.scanResolutions();

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.instance);

		final String sizeKey = CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizePanoramaBackPref
				: ApplicationScreen.sImageSizePanoramaFrontPref;

		if (!prefs.contains(sizeKey))
		{
			if (ResolutionsPictureIdxesList.size() > 0)
			{
				boolean found = false;
				int idx = 0;

				// first - try to select resolution <= 5mpix and with at least
				// 10 frames available
				for (int i = 0; i < ResolutionsPictureSizesList.size(); i++)
				{
					final Point point = ResolutionsPictureSizesList.get(i);
					final int frames_fit_count = (int) (getAmountOfMemoryToFitFrames() / getFrameSizeInBytes(point.x,
							point.y));

					if ((frames_fit_count >= 10) && (point.x * point.y < 5200000))
					{
						idx = i;
						found = true;
						break;
					}
				}

				if (!found)
				{
					int best_fit_count = 1;

					// try to select resolution with highest number of frames
					// available (the lowest resolution really)
					for (int i = 0; i < ResolutionsPictureSizesList.size(); i++)
					{
						final Point point = ResolutionsPictureSizesList.get(i);
						final int frames_fit_count = (int) (getAmountOfMemoryToFitFrames() / getFrameSizeInBytes(
								point.x, point.y));

						if (frames_fit_count > best_fit_count)
						{
							best_fit_count = frames_fit_count;
							idx = i;
							found = true;
						}
					}
				}

				prefs.edit().putString(sizeKey, ResolutionsPictureIdxesList.get(idx)).commit();
			}
		}

		PanoramaAugmentedCapturePlugin.prefResolution = Integer.parseInt(prefs.getString(sizeKey, "0"));

	}

	public static void onDefaultSelectGyroscope()
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.instance);

		if (!prefs.contains(PREFERENCES_KEY_USE_DEVICE_GYRO))
		{
			prefs.edit()
					.putBoolean(PREFERENCES_KEY_USE_DEVICE_GYRO, PanoramaAugmentedCapturePlugin.sensorGyroscope != null)
					.commit();
		}
	}

	public static void selectDefaults()
	{
		PanoramaAugmentedCapturePlugin.onDefaultSelectResolutons();
		PanoramaAugmentedCapturePlugin.onDefaultSelectGyroscope();

	}

	@Override
	public void onDefaultsSelect()
	{
		PanoramaAugmentedCapturePlugin.selectDefaults();
	}

	@SuppressLint("InflateParams")
	@Override
	public void onCreate()
	{
		getPrefs();
		sMemoryPref = ApplicationScreen.getAppResources().getString(R.string.Preference_PanoramaMemory);
		sFrameOverlapPref = ApplicationScreen.getAppResources().getString(R.string.Preference_PanoramaFrameOverlap);
		sAELockPref = ApplicationScreen.getAppResources().getString(R.string.Preference_PanoramaAELock);

		final LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();

		this.modeSwitcher = (Switch) inflator.inflate(R.layout.plugin_capture_night_modeswitcher, null, false);
		final Resources resources = ApplicationScreen.getAppResources();
		this.modeSwitcher.setTextOn(resources.getString(R.string.plugin_capture_panoramaaugmented_modeswitch_sweep));
		this.modeSwitcher.setTextOff(resources
				.getString(R.string.plugin_capture_panoramaaugmented_modeswitch_augmented));
		this.modeSwitcher.setChecked(this.modeSweep);
		this.modeSwitcher.setOnCheckedChangeListener(new OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(final CompoundButton buttonView, final boolean isChecked)
			{
				PanoramaAugmentedCapturePlugin.this.modeSweep = isChecked;
				PanoramaAugmentedCapturePlugin.this.setMode();
				final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen
						.getMainContext());
				prefs.edit().putBoolean(sModePref, modeSweep).commit();

				new CountDownTimer(100, 100)
				{
					public void onTick(long millisUntilFinished){}

					public void onFinish()
					{
						PluginManager.getInstance().switchMode(
								ConfigParser.getInstance().getMode(PluginManager.getInstance().getActiveModeID()));
					}
				}.start();
			}
		});

		this.engine = new AugmentedPanoramaEngine();
	}

	private void setMode()
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		final int overlap = Integer.parseInt(prefs.getString(sFrameOverlapPref, "1"));
		final float intersection;
		switch (overlap)
		{
		case 0:
			intersection = 0.70f;
			break;
		case 1:
			intersection = 0.50f;
			break;
		case 2:
			intersection = 0.30f;
			break;
		default:
			intersection = 0.50f;
			break;
		}

		if (this.modeSweep)
		{
			float sweep_intersection;
			sweep_intersection = 1.5f * intersection;
			if (sweep_intersection > 0.9f)
				sweep_intersection = 0.9f;
			this.engine.setFrameIntersection(sweep_intersection);
			this.engine.reset(this.previewHeight, this.previewWidth, this.viewAngleY);

			final int frames_fit_count = (int) (getAmountOfMemoryToFitFrames() / getFrameSizeInBytes(this.previewWidth,
					this.previewHeight) * 0.9);
			this.engine.setMaxFrames(prefMemoryRelax ? frames_fit_count * 2 : frames_fit_count);
			this.engine.setDistanceLimit(0.1f);
			this.engine.setMiniDisplayMode(true);

			//SM: removed 28.10.14 as causing problems on S4 (5905). Decided not useful on other devices. cp.setRecordingHint(true);
		} else
		{
			this.engine.setFrameIntersection(intersection);
			this.engine.reset(this.pictureHeight, this.pictureWidth, this.viewAngleY);

			final int frames_fit_count = (int) (getAmountOfMemoryToFitFrames() / getFrameSizeInBytes(this.pictureWidth,
					this.pictureHeight));
			this.engine.setMaxFrames(prefMemoryRelax ? frames_fit_count * 2 : frames_fit_count);
			this.engine.setDistanceLimit(0.1f);
			this.engine.setMiniDisplayMode(false);
		}
	}

	@Override
	public void onCameraSetup()
	{
		setMode();
	}
	
	@Override
	public void onStart()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		camera2Preference = prefs.getBoolean(ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), false);
		
		if(CameraController.isNexus6 && camera2Preference)
		{
			prefs.edit().putBoolean(ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), false).commit();
			CameraController.setUseCamera2(false);
			
			CameraController.isOldCameraOneModeLaunched = true;
			PluginManager.getInstance().setSwitchModeType(true);
		}
	}

	@Override
	public void onResume()
	{
		CameraController.setNeedPreviewFrame(true);
		ApplicationScreen.instance.muteShutter(false);

		takingAlready = false;
		showGyroWarnOnce = false;
		aeLockedByPanorama = false;
		wbLockedByPanorama = false;
		this.getPrefs();

		ApplicationScreen.setCaptureFormat(CameraController.YUV);
	}

	@Override
	public void onPause()
	{
		this.deinit();

		synchronized (this.engine)
		{
			if (this.inCapture)
				this.stopCapture();
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

		prefs.edit().putBoolean(ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), camera2Preference).commit();
	}

	@Override
	public boolean onKeyDown(final int keyCode, final KeyEvent event)
	{
		if (keyCode == KeyEvent.KEYCODE_BACK)
		{
			if (this.inCapture && this.engine != null)
			{
				synchronized (this.engine)
				{
					final int result = this.engine.cancelFrame();

					if (result <= 0)
					{
						this.stopCapture();
						PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED_NORESULT,
								String.valueOf(SessionID));
						if (modeSwitcher != null)
							modeSwitcher.setEnabled(true);
					}
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public void onStop()
	{
		ApplicationScreen.getGUIManager().removeViews(modeSwitcher, R.id.specialPluginsLayout3);
		
		if(CameraController.isNexus6 && camera2Preference)
		{
			CameraController.useCamera2OnRelaunch(true);
			CameraController.setUseCamera2(camera2Preference);
		}
	}

	@Override
	public void onExportFinished()
	{
		if (modeSwitcher != null && !inCapture)
			modeSwitcher.setEnabled(true);
	}

	@Override
	public void onGUICreate()
	{
		ApplicationScreen.instance.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_SCENE, true, false, true);

		this.clearViews();

		ApplicationScreen.getGUIManager().showHelp(ApplicationScreen.instance.getString(R.string.Panorama_Help_Header),
				ApplicationScreen.getAppResources().getString(R.string.Panorama_Help),
				R.drawable.plugin_help_panorama, "panoramaShowHelp");

		ApplicationScreen.getGUIManager().removeViews(this.modeSwitcher, R.id.specialPluginsLayout3);
		final RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout3)).addView(this.modeSwitcher,
				params);
		this.modeSwitcher.setLayoutParams(params);
		
		stopPanoramaButton = (RotateImageView) ApplicationScreen.instance.findViewById(R.id.buttonPanoramaStop);
		stopPanoramaButton.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				onShutterClick();
			}
		});
	}

	@Override
	public void selectImageDimension()
	{
		this.init();

		final List<CameraController.Size> cs = CameraController.getSupportedPictureSizes();
		final CameraController.Size size;
		
		// Paranoia check. In some unusual cases prefResolution value may be more then cs.size.
		if (PanoramaAugmentedCapturePlugin.prefResolution >= cs.size()) {
			int iPrefResolution = PanoramaAugmentedCapturePlugin.prefResolution;
			while (iPrefResolution >= cs.size()) {
				iPrefResolution--;
			}
			
			size = cs.get(iPrefResolution);
		} else {
			size = cs.get(PanoramaAugmentedCapturePlugin.prefResolution);
		}
		
		if (CameraController.isHTCOneX)
		{
			if (!CameraController.isFrontCamera())
			{
				CameraController.Size additional = null;
				additional = new CameraController.Size(3264, 2448);
				additional.setWidth(3264);
				additional.setHeight(2448);
				cs.add(additional);
			}
		}
		
		this.pictureWidth = size.getWidth();
		this.pictureHeight = size.getHeight();

		CameraController.setCameraImageSize(new CameraController.Size(this.pictureWidth, this.pictureHeight));
	}

	@Override
	public void setCameraPreviewSize()
	{
		final CameraController.Size previewSize;
		if (this.modeSweep)
		{
			previewSize = getOptimalSweepPreviewSize(CameraController.getSupportedPreviewSizes());
		}
		else
		{
			previewSize = this.getOptimalPreviewSize(CameraController.getSupportedPreviewSizes(),
					this.pictureWidth, this.pictureHeight);
		}

		this.previewWidth = previewSize.getWidth();
		this.previewHeight = previewSize.getHeight();

		ApplicationScreen.instance.setCameraPreviewSize(previewSize.getWidth(), previewSize.getHeight());;
	}

	@Override
	public void setupCameraParameters()
	{
		final List<CameraController.Size> picture_sizes = CameraController.getSupportedPictureSizes();
		final CameraController.Size size;
		
		if (picture_sizes.size() == 0)
		{
			Log.e(TAG, "Picture sizes list is empty");
			return;
		}
		
		if (CameraController.isHTCOneX)
		{
			if (!CameraController.isFrontCamera())
			{
				CameraController.Size additional = null;
				additional = new CameraController.Size(3264, 2448);
				additional.setWidth(3264);
				additional.setHeight(2448);
				picture_sizes.add(additional);
			}
		}

		// Paranoia check. In some unusual cases prefResolution value may be more then cs.size.
		if (PanoramaAugmentedCapturePlugin.prefResolution >= picture_sizes.size()) {
			int iPrefResolution = PanoramaAugmentedCapturePlugin.prefResolution;
			while (iPrefResolution >= picture_sizes.size())
				iPrefResolution--;
			
			size = picture_sizes.get(iPrefResolution);
		} else
			size = picture_sizes.get(PanoramaAugmentedCapturePlugin.prefResolution);
		
		this.pictureWidth = size.getWidth();
		this.pictureHeight = size.getHeight();

		CameraController.setCameraImageSize(new CameraController.Size(this.pictureWidth, this.pictureHeight));

		CameraController.setPictureSize(this.pictureWidth, this.pictureHeight);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		int jpegQuality = Integer.parseInt(prefs.getString(ApplicationScreen.sJPEGQualityPref, "95"));
		CameraController.setJpegQuality(jpegQuality);

		try
		{
			try
			{
				this.viewAngleX = CameraController.getHorizontalViewAngle();
				this.viewAngleY = CameraController.getVerticalViewAngle();
			} catch (final Throwable e)
			{
				// Some bugged camera drivers pop ridiculous exception here, use
				// typical view angles then
				this.viewAngleX = 55.4f;
				this.viewAngleY = 42.7f;
			}

			// some devices report incorrect FOV values, use typical view angles
			// then
			if (this.viewAngleX >= 150)
			{
				this.viewAngleX = 55.4f;
				this.viewAngleY = 42.7f;
			}

			// Some devices report incorrect value for vertical view angle
			if (this.viewAngleY == this.viewAngleX)
				this.viewAngleY = this.viewAngleX * 3 / 4;
		} catch (final Exception e)
		{
			// Some bugged camera drivers pop ridiculous exception here, use
			// typical view angles then
			this.viewAngleX = 55.4f;
			this.viewAngleY = 42.7f;
		}

		// some devices report incorrect FOV values, use typical view angles
		// then
		if (this.viewAngleX >= 150)
		{
			this.viewAngleX = 55.4f;
			this.viewAngleY = 42.7f;
		}

		// Some cameras report incorrect view angles
		// Usually vertical view angle is incorrect, but eg Htc One report
		// incorrect horizontal view angle
		// If aspect ratio from FOV differs by more than 10% from aspect ratio
		// from W/H
		// - re-compute view angle
		float HorizontalViewFromAspect = 2
				* 180
				* (float) Math.atan((float) this.pictureWidth / (float) this.pictureHeight
						* (float) Math.tan((float) Math.PI * this.viewAngleY / (2 * 180))) / (float) Math.PI;
		float VerticalViewFromAspect = 2
				* 180
				* (float) Math.atan((float) this.pictureHeight / (float) this.pictureWidth
						* (float) Math.tan((float) Math.PI * this.viewAngleX / (2 * 180))) / (float) Math.PI;
		// not expecting very narrow field of view
		if ((VerticalViewFromAspect > 40.f) && (VerticalViewFromAspect < 0.9f * this.viewAngleY))
			this.viewAngleY = VerticalViewFromAspect;
		else if ((HorizontalViewFromAspect < 0.9f * this.viewAngleX)
				|| (HorizontalViewFromAspect > 1.1f * this.viewAngleX))
			this.viewAngleX = HorizontalViewFromAspect;

		if (!this.prefHardwareGyroscope)
		{
			this.sensorSoftGyroscope.SetFrameParameters(this.previewWidth, this.previewHeight, this.viewAngleX,
					this.viewAngleY);
		}
	}

	@Override
	public boolean isGLSurfaceNeeded()
	{
		return true;
	}

	@Override
	public void onGLSurfaceCreated(final GL10 gl, final EGLConfig config)
	{
		this.engine.onSurfaceCreated(gl, config);
		this.glContextCreated = true;
	}

	@Override
	public void onGLSurfaceChanged(final GL10 gl, final int width, final int height)
	{
		this.engine.onSurfaceChanged(gl, width, height);
	}

	@Override
	public void onGLDrawFrame(final GL10 gl)
	{
		this.engine.onDrawFrame(gl);
	}

	private void setFocused()
	{
		int fm = CameraController.getFocusMode();
		int fs = CameraController.getFocusState();
		if (!takingAlready
				&& (fs == CameraController.FOCUS_STATE_IDLE || fs == CameraController.FOCUS_STATE_FOCUSING)
				&& !(fm == CameraParameters.AF_MODE_INFINITY || fm == CameraParameters.AF_MODE_FIXED
						|| fm == CameraParameters.AF_MODE_EDOF || fm == CameraParameters.AF_MODE_CONTINUOUS_PICTURE || fm == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
						|| fm == CameraParameters.MF_MODE || fm == CameraParameters.AF_MODE_UNSUPPORTED)
				&& !ApplicationScreen.instance.getAutoFocusLock())
		{
			this.focused = false;
		} else if (!takingAlready)
		{
			this.focused = true;
		}
	}

	@Override
	public void onShutterClick()
	{
		synchronized (this.engine)
		{
			if (!takingAlready)
			{
				if (this.inCapture)
				{
					ApplicationScreen.instance.setKeepScreenOn(false);
					this.stopCapture();
				} else
				{
					ApplicationScreen.instance.setKeepScreenOn(true);
					this.startCapture();
				}
			}
		}
	}

	@Override
	public boolean onBroadcast(final int command, final int arg)
	{
		if (command == ApplicationInterface.MSG_FORCE_FINISH_CAPTURE)
		{
			this.stopCapture();

			return true;
		} else if (command == ApplicationInterface.MSG_BAD_FRAME)
		{
			Toast.makeText(
					ApplicationScreen.instance,
					ApplicationScreen.getAppResources()
							.getString(R.string.plugin_capture_panoramaaugmented_badframe), Toast.LENGTH_SHORT).show();
			return true;
		} else if (command == ApplicationInterface.MSG_OUT_OF_MEMORY)
		{
			Toast.makeText(
					ApplicationScreen.instance,
					ApplicationScreen.getAppResources()
							.getString(R.string.plugin_capture_panoramaaugmented_outofmemory), Toast.LENGTH_LONG)
					.show();
			return true;
		} else if (command == ApplicationInterface.MSG_NOTIFY_LIMIT_REACHED)
		{
			Toast.makeText(
					ApplicationScreen.instance,
					ApplicationScreen.getAppResources()
							.getString(R.string.plugin_capture_panoramaaugmented_stopcapture), Toast.LENGTH_LONG)
					.show();
			return true;
		}

		return false;
	}

	private void getPrefs()
	{
		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		this.modeSweep = prefs.getBoolean(sModePref, false);

		try
		{
			prefResolution = Integer
					.parseInt(prefs.getString(CameraController.getCameraIndex() == 0 ? ApplicationScreen.sImageSizePanoramaBackPref
							: ApplicationScreen.sImageSizePanoramaFrontPref, "0"));
		} catch (final Exception e)
		{
			e.printStackTrace();
			Log.e("Panorama", "getPrefs exception: " + e.getMessage());
			prefResolution = 0;
		}
		this.prefHardwareGyroscope = prefs.getBoolean(PREFERENCES_KEY_USE_DEVICE_GYRO, sensorGyroscope != null);

		this.prefMemoryRelax = prefs.getBoolean(sMemoryPref, false);

		aewblock = Integer.parseInt(prefs.getString(sAELockPref, "1"));
	}

	private void createPrefs(final Preference ud_pref)
	{
		if (ud_pref != null)
		{
			ud_pref.setEnabled(PanoramaAugmentedCapturePlugin.sensorGyroscope != null);
			ud_pref.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				@Override
				public boolean onPreferenceChange(final Preference preference, final Object newValue)
				{
					getPrefs();
					if (!PanoramaAugmentedCapturePlugin.this.prefHardwareGyroscope && !((Boolean) newValue))
					{
						final AlertDialog ad = new AlertDialog.Builder(ApplicationScreen.instance)
								.setIcon(R.drawable.alert_dialog_icon)
								.setTitle(R.string.pref_plugin_capture_panoramaaugmented_nogyro_dialog_title)
								.setMessage(R.string.pref_plugin_capture_panoramaaugmented_nogyro_dialog_text)
								.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener()
								{
									public void onClick(final DialogInterface dialog, final int whichButton)
									{
										dialog.dismiss();
									}
								}).create();

						if (showGyroWarnOnce)
							return false;
						showGyroWarnOnce = true;
						ad.show();
						return false;
					} else
						return true;
				}
			});
		}
	}

	@Override
	public void onPreferenceCreate(final PreferenceFragment prefActivity)
	{
		this.createPrefs(prefActivity.findPreference(PREFERENCES_KEY_USE_DEVICE_GYRO));
	}

	private static void scanResolutions()
	{
		ResolutionsPictureNamesList.clear();
		ResolutionsPictureIdxesList.clear();
		ResolutionsPictureSizesList.clear();

		final List<CameraController.Size> cs = CameraController.getSupportedPictureSizes();
		if (CameraController.isHTCOneX)
		{
			if (!CameraController.isFrontCamera())
			{
				CameraController.Size additional = null;
				additional = new CameraController.Size(3264, 2448);
				additional.setWidth(3264);
				additional.setHeight(2448);
				cs.add(additional);
			}
		}
		
		int maxIndex = 0;

		if (cs != null)
		{
			for (int ii = 0; ii < cs.size(); ++ii)
			{
				final CameraController.Size s = cs.get(ii);

				if (s.getWidth() > cs.get(maxIndex).getWidth())
				{
					maxIndex = ii;
				}

				if ((long) s.getWidth() >= MIN_HEIGHT_SUPPORTED)
				{
					// find good location in a list
					int loc;
					boolean shouldInsert = true;
					for (loc = 0; loc < ResolutionsPictureSizesList.size(); ++loc)
					{
						final Point psize = ResolutionsPictureSizesList.get(loc);
						if (psize.x == s.getWidth())
						{
							if (s.getHeight() > psize.y)
							{
								ResolutionsPictureNamesList.remove(loc);
								ResolutionsPictureIdxesList.remove(loc);
								ResolutionsPictureSizesList.remove(loc);
								break;
							} else
							{
								shouldInsert = false;
								break;
							}
						} else if (psize.x < s.getWidth())
						{
							break;
						}
					}

					if (shouldInsert)
					{
						ResolutionsPictureNamesList.add(loc, String.format("%dpx", s.getWidth()));
						ResolutionsPictureIdxesList.add(loc, String.format("%d", ii));
						ResolutionsPictureSizesList.add(loc, new Point(s.getWidth(), s.getHeight()));
					}
				}
			}

			if (ResolutionsPictureIdxesList.size() == 0)
			{
				final CameraController.Size s = cs.get(maxIndex);

				ResolutionsPictureNamesList.add(String.format("%dpx", s.getWidth()));
				ResolutionsPictureIdxesList.add(String.format("%d", maxIndex));
				ResolutionsPictureSizesList.add(new Point(s.getWidth(), s.getHeight()));
			}
		}
	}

	public static long getAmountOfMemoryToFitFrames()
	{
		// activityManager returning crap (way less than really available)
		final int[] mi = HeapUtil.getMemoryInfo();

		//Log.e(TAG, "Memory: used: " + mi[0] + "Mb  free: " + mi[1] + "Mb");

		// memory required for stitched panorama about equals to memory required
		// for input frames
		// (total height can be more, but frames are overlapped by 1/3
		// horizontally)
		// in sweep mode: approx. one out of three frames used,
		// therefore increase in stitched panorama height due to non-straight
		// frames is compensated

		// augmented mode: for output panorama: ~nFrames*bytesPerFrame
		// for intermediate pre-rendered frames: just a bit more than
		// nFrames*bytesPerFrame (20% is possible)
		// also, there is a possibility of memory fragmentation

		return (long) ((mi[1] - 10.f) * 1000000.f * 0.8f); // use up to 80% and
															// ensure at least
															// 64Mb left free
	}

	public static int getFrameSizeInBytes(int width, int height)
	{
		return (5 * width * height + width + 256);
	}

	private void checkCoordinatesRemapRequired()
	{
		final Display display = ((WindowManager) ApplicationScreen.instance.getSystemService(Context.WINDOW_SERVICE))
				.getDefaultDisplay();
		// This is proved way of checking it so we better use deprecated
		// methods.
		@SuppressWarnings("deprecation")
		final int orientation = (display.getWidth() <= display.getHeight()) ? Configuration.ORIENTATION_PORTRAIT
				: Configuration.ORIENTATION_LANDSCAPE;
		final int rotation = display.getRotation();
		this.remapOrientation = (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_0)
				|| (orientation == Configuration.ORIENTATION_LANDSCAPE && rotation == Surface.ROTATION_180)
				|| (orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_90)
				|| (orientation == Configuration.ORIENTATION_PORTRAIT && rotation == Surface.ROTATION_270);
	}

	private void startCapture()
	{
		this.glContextCreated = false;
		final Message msg = new Message();
		msg.what = ApplicationInterface.MSG_OPENGL_LAYER_SHOW;
		ApplicationScreen.getMessageHandler().sendMessage(msg);
		
		this.isFirstFrame = true;

		this.setFocused();

		if (this.modeSwitcher != null)
			this.modeSwitcher.setEnabled(false);

		this.rotationListener.setUpdateDrift(false);

		lockAEWB();

		Date curDate = new Date();
		SessionID = curDate.getTime();

		this.inCapture = true;
		
		
		//show stop button and hide standard bottom gui
		View mainButtonsPanorama = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(R.id.mainButtonsPanorama);
		mainButtonsPanorama.setVisibility(View.VISIBLE);

		View mainButtons = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(R.id.mainButtons);
		mainButtons.setVisibility(View.INVISIBLE);
	}

	private void takePictureUnimode(final int image)
	{
		if (this.focused)
		{
			if (this.modeSweep)
			{
				this.engine.recordCoordinates();
				this.engine.onFrameAdded(image);
				this.isFirstFrame = false;
				
				final boolean done = this.engine.isCircular();
				final boolean oom = this.engine.isMax();

				if (oom && !done)
					PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_OUT_OF_MEMORY);
				else if (done)
					PluginManager.getInstance()
							.sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_NOTIFY_LIMIT_REACHED);

				if (done || oom)
					PluginManager.getInstance()
							.sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_FORCE_FINISH_CAPTURE);
			} else
			{
				takingAlready = true;
				ApplicationScreen.takePicture();
			}
		}
	}

	@Override
	public void onPreviewFrame(final byte[] dataOrig)
	{
		this.previewRestartFlag = false;

		if (!this.prefHardwareGyroscope && this.sensorSoftGyroscope != null)
		{
			this.sensorSoftGyroscope.NewData(dataOrig);
		}

		synchronized (this.engine)
		{
			if (!takingAlready && this.glContextCreated)
			{
				final int state = this.engine.getPictureTakingState(this.modeSweep ? true : 
									CameraController.getFocusMode() == CameraParameters.AF_MODE_AUTO);

				if (state == AugmentedPanoramaEngine.STATE_TAKINGPICTURE || this.isFirstFrame)
				{
					if (this.modeSweep)
						this.takePictureUnimode(SwapHeap.SwapToHeap(dataOrig));
					else
						this.takePictureUnimode(0);
				}
			}
		}
	}

	@Override
	public void onAutoFocus(final boolean success)
	{
		this.focused = true;
	}

	private Handler captureDelayHandler = new Handler();
	private Runnable captureDelayRunnable = new Runnable()
	{
		@Override
		public void run()
		{
			createRequestIDList(1);
			CameraController.captureImagesWithParams(1, CameraController.YUV, null, null, null, null, false, false, true);
			
		}
	};
	@Override
	public void takePicture()
	{
		synchronized (this.engine)
		{
			if (!this.inCapture)
			{
				takingAlready = false;
				return;
			}
		}

		takingAlready = true;

		this.coordsRecorded = false;
		
		captureDelayHandler.postDelayed(captureDelayRunnable, 500);
	}

	private void lockAEWB()
	{
		boolean lock = false;
		switch (aewblock)
		{
		case 0:
			lock = false;
			break;
		case 1:
			if (ApplicationScreen.getGUIManager().getDisplayOrientation() == 90
					|| ApplicationScreen.getGUIManager().getDisplayOrientation() == 180)
				lock = true;
			break;
		case 2:
			lock = true;
			break;
		default:
			break;
		}

		if (lock)
		{
			if (CameraController.isWhiteBalanceLockSupported() && !CameraController.isWhiteBalanceLocked())
				wbLockedByPanorama = CameraController.setAutoWhiteBalanceLock(true);
			if (CameraController.isExposureLockSupported() && !CameraController.isExposureLocked())
				aeLockedByPanorama = CameraController.setAutoExposureLock(true);
		}
		lock = false;
		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_AEWB_CHANGED);
	}

	private void unlockAEWB()
	{
		if (wbLockedByPanorama && CameraController.isWhiteBalanceLockSupported() && CameraController.isWhiteBalanceLocked())
			wbLockedByPanorama = CameraController.setAutoWhiteBalanceLock(false);
		if (aeLockedByPanorama && CameraController.isExposureLockSupported() && CameraController.isExposureLocked())
			aeLockedByPanorama = CameraController.setAutoExposureLock(false);
		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_AEWB_CHANGED);
	}

	@Override
	public void onShutter()
	{
		this.coordsRecorded = true;
		this.engine.recordCoordinates();
	}

	@Override
	public void addToSharedMemExifTags(byte[] frameData)
	{
		if (this.isFirstFrame)
		{
			if (frameData != null)
				PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(frameData, SessionID, -1);
			else
				PluginManager.getInstance().addToSharedMemExifTagsFromCamera(SessionID);
		}
	}

	@Override
	public void onImageTaken(int frame, byte[] frameDataOrig, int frame_len, int format)
	{
		final boolean goodPlace;
		
		synchronized (this.engine)
		{
			takingAlready = false;
			this.engine.notifyAll();

			if (!this.coordsRecorded)
				this.engine.recordCoordinates();

			if (frame == 0)
			{
				frame = SwapHeap.SwapToHeap(frameDataOrig);
				frame_len = frameDataOrig.length;
			}

			goodPlace = this.engine.onFrameAdded(frame);
		}

		this.isFirstFrame = false;

		final boolean done = this.engine.isCircular();
		final boolean oom = this.engine.isMax();

		if (oom && !done)
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_OUT_OF_MEMORY);
		else if (done)
			PluginManager.getInstance()
					.sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_NOTIFY_LIMIT_REACHED);

		nextFrame();

		if (!goodPlace)
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_BAD_FRAME);

		if (done || oom)
			PluginManager.getInstance()
					.sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_FORCE_FINISH_CAPTURE);
	}

	@TargetApi(21)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		int requestID = requestIDArray[0];
		if (result.getSequenceId() == requestID)
		{
			if (this.isFirstFrame)
			{
				PluginManager.getInstance().addToSharedMemExifTagsFromCaptureResult(result, SessionID, -1);
				this.isFirstFrame = false;
			}
		}
	}
	
	
	private void nextFrame()
	{
		this.previewRestartFlag = true;

		CameraController.startCameraPreview();

		new CountDownTimer(1000, 330)
		{
			private boolean	first	= true;

			public void onTick(final long millisUntilFinished)
			{
				if (this.first)
				{
					this.first = false;
					return;
				}

				if (PanoramaAugmentedCapturePlugin.this.previewRestartFlag)
				{
					Log.d("Almalence", String.format("Emergency preview restart"));
					CameraController.setPreviewCallbackWithBuffer();
				} else
				{
					this.cancel();
				}
			}

			public void onFinish()
			{

			}
		}.start();
	}

	private void stopCapture()
	{
		//show standard bottom gui and hide stop button  
		View mainButtonsPanorama = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(R.id.mainButtonsPanorama);
		mainButtonsPanorama.setVisibility(View.GONE);
		View mainButtons = (View) ApplicationScreen.instance.guiManager.getMainView().findViewById(R.id.mainButtons);
		mainButtons.setVisibility(View.VISIBLE);
		mainButtons.findViewById(R.id.buttonSelectMode).setVisibility(View.VISIBLE);
		
		this.inCapture = false;
		this.focused = false;

		unlockAEWB();

		this.rotationListener.setUpdateDrift(true);

		final LinkedList<AugmentedFrameTaken> frames = this.engine.retrieveFrames();

		if (frames.size() > 0)
		{
			PluginManager.getInstance().addToSharedMem("frameorientation" + SessionID,
					String.valueOf(ApplicationScreen.getGUIManager().getDisplayOrientation()));
			PluginManager.getInstance().addToSharedMem("pano_mirror" + SessionID,
					String.valueOf(CameraController.isFrontCamera()));
			if (this.modeSweep)
			{
				PluginManager.getInstance()
						.addToSharedMem("pano_width" + SessionID, String.valueOf(this.previewHeight));
				PluginManager.getInstance()
						.addToSharedMem("pano_height" + SessionID, String.valueOf(this.previewWidth));
			} else
			{
				PluginManager.getInstance()
						.addToSharedMem("pano_width" + SessionID, String.valueOf(this.pictureHeight));
				PluginManager.getInstance()
						.addToSharedMem("pano_height" + SessionID, String.valueOf(this.pictureWidth));
			}
			PluginManager.getInstance().addToSharedMem("pano_frames_count" + SessionID, String.valueOf(frames.size()));
			PluginManager.getInstance().addToSharedMem("pano_camera_fov" + SessionID,
					String.valueOf((int) (this.viewAngleY + 0.5f)));
			PluginManager.getInstance().addToSharedMem("pano_useall" + SessionID, "1");
			PluginManager.getInstance().addToSharedMem("pano_freeinput" + SessionID, "0");

			Vector3d normalLast = new Vector3d();
			Vector3d normalCurrent = new Vector3d();
			Vector3d vTop = new Vector3d();

			float R = this.engine.getRadiusToEdge();
			float PixelsShiftX = 0.0f;
			float PixelsShiftY = 0.0f;
			float angleXprev = 0;
			float angleX = 0;
			float angleY = 0;
			float angleR = 0;
			final Iterator<AugmentedFrameTaken> iterator = frames.iterator();
			AugmentedFrameTaken frame = iterator.next();

			// baseTransform matrix converts from world coordinate system into
			// camera coordinate system (as it was during taking first frame):
			// x,y - is screen surface (in portrait mode x pointing right, y
			// pointing down)
			// z - axis is perpendicular to screen surface, pointing in
			// direction opposite to where camera is pointing
			float[] baseTransform16 = this.engine.initialTransform;
			float[] baseTransform = new float[9];
			baseTransform[0] = baseTransform16[0];
			baseTransform[1] = baseTransform16[1];
			baseTransform[2] = baseTransform16[2];
			baseTransform[3] = baseTransform16[4];
			baseTransform[4] = baseTransform16[5];
			baseTransform[5] = baseTransform16[6];
			baseTransform[6] = baseTransform16[8];
			baseTransform[7] = baseTransform16[9];
			baseTransform[8] = baseTransform16[10];

			frame.getPosition(normalLast);
			normalLast = transformVector(normalLast, baseTransform);

			int frame_cursor = 0;

			while (true)
			{
				frame.getPosition(normalCurrent);
				frame.getTop(vTop);

				normalCurrent = transformVector(normalCurrent, baseTransform);
				vTop = transformVector(vTop, baseTransform);
				angleR = getAngle(normalCurrent, vTop, R);

				angleX = (float) Math.atan2(-normalLast.z, normalLast.x)
						- (float) Math.atan2(-normalCurrent.z, normalCurrent.x);
				angleY = (float) Math.asin(-normalCurrent.y / R);

				// make sure angle difference is within bounds
				// along X axis the angle difference is always positive
				// measuring from the previous frame
				while (angleX - angleXprev > 2 * Math.PI)
					angleX -= 2 * Math.PI;
				while (angleX - angleXprev < 0)
					angleX += 2 * Math.PI;
				while (angleY > Math.PI)
					angleY -= 2 * Math.PI;
				while (angleY < -Math.PI)
					angleY += 2 * Math.PI;

				angleXprev = angleX;

				PixelsShiftX = angleX * R;
				PixelsShiftY = angleY * R;

				CameraController.Size imageSize = CameraController.getCameraImageSize();
				// convert rotation around center into rotation around top-left corner
				PixelsShiftX += imageSize.getWidth() / 2 * (1 - (float)Math.cos(angleR))
						+ imageSize.getHeight() / 2 * (float)Math.sin(angleR);
				PixelsShiftY += -imageSize.getWidth() / 2 * (float)Math.sin(angleR) + imageSize.getHeight()
						/ 2 * (1 - (float)Math.cos(angleR));

				PluginManager.getInstance().addToSharedMem("pano_frame" + (frame_cursor + 1) + "." + SessionID,
						String.valueOf(frame.getNV21address()));

				PluginManager.getInstance().addToSharedMem("pano_frametrs" + (frame_cursor + 1) + ".00." + SessionID,
						String.valueOf((float)Math.cos(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs" + (frame_cursor + 1) + ".01." + SessionID,
						String.valueOf(-(float)Math.sin(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs" + (frame_cursor + 1) + ".02." + SessionID,
						String.valueOf(PixelsShiftX));
				PluginManager.getInstance().addToSharedMem("pano_frametrs" + (frame_cursor + 1) + ".10." + SessionID,
						String.valueOf((float)Math.sin(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs" + (frame_cursor + 1) + ".11." + SessionID,
						String.valueOf((float)Math.cos(angleR)));
				PluginManager.getInstance().addToSharedMem("pano_frametrs" + (frame_cursor + 1) + ".12." + SessionID,
						String.valueOf(PixelsShiftY));
				PluginManager.getInstance().addToSharedMem("pano_frametrs" + (frame_cursor + 1) + ".20." + SessionID,
						String.valueOf(0.0f));
				PluginManager.getInstance().addToSharedMem("pano_frametrs" + (frame_cursor + 1) + ".21." + SessionID,
						String.valueOf(0.0f));
				PluginManager.getInstance().addToSharedMem("pano_frametrs" + (frame_cursor + 1) + ".22." + SessionID,
						String.valueOf(1.0f));

				if (!iterator.hasNext())
					break;

				frame = iterator.next();

				frame_cursor++;
			}

			Message message = new Message();
			message.obj = String.valueOf(SessionID);
			message.what = ApplicationInterface.MSG_CAPTURE_FINISHED;
			ApplicationScreen.getMessageHandler().sendMessage(message);
		}
		
		final Message msg = new Message();
		msg.what = ApplicationInterface.MSG_OPENGL_LAYER_HIDE;
		ApplicationScreen.getMessageHandler().sendMessage(msg);
	}

	private CameraController.Size getOptimalSweepPreviewSize(final List<CameraController.Size> sizes)
	{
		CameraController.Size best_size = sizes.get(0);

		for (CameraController.Size size : sizes)
		{
			if (size.getWidth() > best_size.getWidth())
			{
				best_size = size;
			}
		}

		return best_size;
	}

	private static Vector3d transformVector(final Vector3d vec, final float[] mat)
	{
		final Vector3d vo = new Vector3d();

		vo.x = vec.x * mat[0] + vec.y * mat[1] + vec.z * mat[2];
		vo.y = vec.x * mat[3] + vec.y * mat[4] + vec.z * mat[5];
		vo.z = vec.x * mat[6] + vec.y * mat[7] + vec.z * mat[8];

		return vo;
	}

	// as in: http://stackoverflow.com/questions/5188561/signed-angle-between-two-3d-vectors-with-same-origin-within-the-same-plane-reci
	private static float signedAngle(final Vector3d Va, final Vector3d Vb, final Vector3d Vn)
	{
		try
		{
			Vector3d Vx = new Vector3d();

			Vx.x = Va.y * Vb.z - Va.z * Vb.y;
			Vx.y = Va.z * Vb.x - Va.x * Vb.z;
			Vx.z = Va.x * Vb.y - Va.y * Vb.x;

			float sina = Vx.length() / (Va.length() * Vb.length());
			float cosa = (Va.x * Vb.x + Va.y * Vb.y + Va.z * Vb.z) / (Va.length() * Vb.length());

			float angle = (float) Math.atan2(sina, cosa);

			float sign = Vn.x * Vx.x + Vn.y * Vx.y + Vn.z * Vx.z;
			if (sign < 0)
				return -angle;
			else
				return angle;
		} catch (final Exception e) // if div0 happens
		{
			return 0;
		}
	}

	// compute rotation angle (around normal vector, relative to Y axis)
	private static float getAngle(final Vector3d normal, final Vector3d vTop, final float radius)
	{
		final float angle;

		Vector3d vA = new Vector3d();

		// assuming y axis pointing upwards (ideal top vector = (0,1,0))
		if (normal.y > 0)
		{
			vA.x = -normal.x;
			vA.z = -normal.z;
			vA.y = radius * radius / normal.y - normal.y;
		} else if (normal.y < 0)
		{
			vA.x = normal.x;
			vA.z = normal.z;
			vA.y = -radius * radius / normal.y + normal.y;
		} else
		{
			vA.x = 0;
			vA.y = 1;
			vA.z = 0;
		}

		if (vA.length() > 0)
		{
			angle = signedAngle(vA, vTop, normal);
		} else
			angle = 0; // angle is unknown at pole locations

		return angle;
	}
}
