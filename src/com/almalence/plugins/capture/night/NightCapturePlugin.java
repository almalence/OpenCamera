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

package com.almalence.plugins.capture.night;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android2.hardware.camera2.CaptureResult;
import android.media.Image;
import android.opengl.GLES10;
import android.opengl.GLU;
import android.os.Build;
import android.os.Debug;
import android.os.Message;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

/* <!-- +++
 import com.almalence.opencam_plus.CameraController;
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.ui.GUI.CameraParameter;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.GUI.CameraParameter;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.util.ImageConversion;

import com.almalence.ui.Switch.Switch;
import com.almalence.SwapHeap;
import com.almalence.YuvImage;

/***
 * Implements night capture plugin - different capture logics available
 ***/

public class NightCapturePlugin extends PluginCapture
{
	private static final int	HI_SPEED_FRAMES			= 12;
	private static final int	HI_RES_FRAMES			= 8;
	private static final int	MIN_MPIX_SUPPORTED		= 1280 * 960;
	private static final int	MIN_MPIX_PREVIEW		= 600 * 400;
	private static final long	MPIX_8					= 3504 * 2336;				// Actually
																					// 8.2
																					// mpix,
																					// some
																					// reserve
																					// for
																					// unusual
																					// cameras;

	private static Toast		capturingDialog;

	// almashot - related
	private int					frameNumber;
	private boolean				aboutToTakePicture		= false;
	private int					nVFframesToBuffer;

	// shared between activities
	public static int			CapIdx;
	private static int			total_frames;
	private static int[]		compressed_frame		= new int[HI_SPEED_FRAMES];
	private static int[]		compressed_frame_len	= new int[HI_SPEED_FRAMES];

	// Night vision variables
	private GLCameraPreview		cameraPreview;
	private byte[]				data1;
	private byte[]				data2;
	private byte[]				dataS;
	private byte[]				dataRotated;

	int							onDrawFrameCounter		= 1;
	int[]						cameraTexture;
	byte[]						glCameraFrame			= new byte[256 * 256];		// size
																					// of
																					// a
																					// texture
																					// must
																					// be
																					// a
																					// power
																					// of
																					// 2
	FloatBuffer					cubeBuff;
	FloatBuffer					texBuff;

	byte[]						yuvData;
	byte[]						rgbData;

	float						currHalfWidth;
	float						currHalfHeight;

	static int					captureIndex			= -1;
	static int					imgCaptureWidth			= 0;
	static int					imgCaptureHeight		= 0;

	float						cameraDist;

	// preferences
	private static String		ModePreference;									// 0=hi-res
																					// 1=hi-speed
	private static String		FocusPreference;
	private static boolean		OpenGLPreference;
	private static String		ImageSizeIdxPreference;

	private static List<Long>	ResolutionsMPixList;
	public static List<Long> getResolutionsMPixList()
	{
		return ResolutionsMPixList;
	}

	public static List<String> getResolutionsIdxesList()
	{
		return ResolutionsIdxesList;
	}

	public static List<String> getResolutionsNamesList()
	{
		return ResolutionsNamesList;
	}

	private static List<String>	ResolutionsIdxesList;
	private static List<String>	ResolutionsNamesList;

	private int					preferenceSceneMode;
	private int					preferenceFocusMode;
	private int					preferenceFlashMode;

	private static String		nightCaptureModePref;
	private static String		hiResModeTitle;
	private static String		hiSpeedModeTitle;
	private static String		nightVisionLayerShowPref;
	private static String		nightCaptureFocusPref;

	private Switch				modeSwitcher;

	public NightCapturePlugin()
	{
		super("com.almalence.plugins.nightcapture", R.xml.preferences_capture_night,
				R.xml.preferences_capture_night_more, R.drawable.plugin_capture_night_nightvision_on, MainScreen
						.getInstance().getResources().getString(R.string.NightVisionOn));
	}

	@Override
	public void onCreate()
	{
		cameraPreview = new GLCameraPreview(MainScreen.getMainContext());

		nightCaptureModePref = MainScreen.getInstance().getResources().getString(R.string.NightCaptureMode);
		hiResModeTitle = MainScreen.getInstance().getResources().getString(R.string.NightCaptureModeHiRes);
		hiSpeedModeTitle = MainScreen.getInstance().getResources().getString(R.string.NightCaptureModeHiSpeed);
		nightVisionLayerShowPref = MainScreen.getInstance().getResources().getString(R.string.NightVisionLayerShow);
		nightCaptureFocusPref = MainScreen.getInstance().getResources().getString(R.string.NightCaptureFocusPref);

		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		modeSwitcher = (Switch) inflator.inflate(R.layout.plugin_capture_night_modeswitcher, null, false);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		ModePreference = prefs.getString(nightCaptureModePref, "1");
		modeSwitcher.setTextOn(hiResModeTitle);
		modeSwitcher.setTextOff(hiSpeedModeTitle);
		modeSwitcher.setChecked(ModePreference.compareTo("0") == 0 ? true : false);
		modeSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
				if (isChecked)
					ModePreference = "0";
				else
					ModePreference = "1";

				SharedPreferences.Editor editor = prefs.edit();
				editor.putString(nightCaptureModePref, ModePreference);
				editor.commit();

				Message msg = new Message();
				msg.what = PluginManager.MSG_RESTART_MAIN_SCREEN;
				MainScreen.getMessageHandler().sendMessage(msg);
			}
		});
		if (PluginManager.getInstance().getProcessingCounter() == 0)
			modeSwitcher.setEnabled(true);
	}

	@Override
	public void onStart()
	{
		getPrefs();

		if (OpenGLPreference)
		{
			quickControlIconID = R.drawable.plugin_capture_night_nightvision_on;
			quickControlTitle = MainScreen.getInstance().getResources().getString(R.string.NightVisionOn);
		} else
		{
			quickControlIconID = R.drawable.plugin_capture_night_nightvision_off;
			quickControlTitle = MainScreen.getInstance().getResources().getString(R.string.NightVisionOff);
		}
	}

	@Override
	public void onResume()
	{
		takingAlready = false;
		inCapture = false;

		MainScreen.getInstance().muteShutter(false);

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		preferenceSceneMode = prefs.getInt(MainScreen.sSceneModePref, CameraParameters.SCENE_MODE_AUTO);
		preferenceFocusMode = prefs.getInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
				: MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_AUTO);
		preferenceFlashMode = prefs.getInt(MainScreen.sFlashModePref, CameraParameters.FLASH_MODE_SINGLE);

		MainScreen.setCaptureYUVFrames(true);
	}

	@Override
	public void onPause()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		prefs.edit().putInt(MainScreen.sSceneModePref, preferenceSceneMode).commit();
		prefs.edit()
				.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
						: MainScreen.sFrontFocusModePref, preferenceFocusMode).commit();
		prefs.edit().putInt(MainScreen.sFlashModePref, preferenceFlashMode).commit();
	}

	@Override
	public void onStop()
	{
		MainScreen.getGUIManager().removeViews(modeSwitcher, R.id.specialPluginsLayout3);
	}

	@Override
	public void onExportFinished()
	{
		if (modeSwitcher != null && PluginManager.getInstance().getProcessingCounter() == 0 && !inCapture)
			modeSwitcher.setEnabled(true);
	}

	@Override
	public void onGUICreate()
	{
		MainScreen.getGUIManager().removeViews(modeSwitcher, R.id.specialPluginsLayout3);

		MainScreen.getInstance().disableCameraParameter(CameraParameter.CAMERA_PARAMETER_SCENE, true, false);
		MainScreen.getInstance().disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FOCUS, true, false);
		MainScreen.getInstance().disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FLASH, true, true);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout3)).addView(this.modeSwitcher,
				params);

		this.modeSwitcher.setLayoutParams(params);
		this.modeSwitcher.requestLayout();

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout3)).requestLayout();
	}

	@Override
	public boolean isGLSurfaceNeeded()
	{
		return true;
	}

	@Override
	public void onQuickControlClick()
	{
		Message msg = new Message();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		SharedPreferences.Editor editor = prefs.edit();

		if (quickControlIconID == R.drawable.plugin_capture_night_nightvision_on)
		{
			quickControlIconID = R.drawable.plugin_capture_night_nightvision_off;
			quickControlTitle = MainScreen.getInstance().getResources().getString(R.string.NightVisionOff);

			editor.putBoolean(nightVisionLayerShowPref, false);
			editor.commit();

			OpenGLPreference = false;

			data1 = null;
			data2 = null;
			dataS = null;
			dataRotated = null;
			yuvData = null;
			rgbData = null;

			msg.what = PluginManager.MSG_OPENGL_LAYER_HIDE;
		} else if (quickControlIconID == R.drawable.plugin_capture_night_nightvision_off)
		{
			quickControlIconID = R.drawable.plugin_capture_night_nightvision_on;
			quickControlTitle = MainScreen.getInstance().getResources().getString(R.string.NightVisionOn);

			editor.putBoolean(nightVisionLayerShowPref, true);
			editor.commit();

			OpenGLPreference = true;

			msg.what = PluginManager.MSG_OPENGL_LAYER_SHOW;
		}

		MainScreen.getMessageHandler().sendMessage(msg);
	}

	@SuppressLint("CommitPrefEdits")
	private void getPrefs()
	{
		String defaultMode = "1";
		String defaultFocus = "0";

		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		ModePreference = prefs.getString(nightCaptureModePref, defaultMode);
		ImageSizeIdxPreference = prefs.getString(CameraController.getCameraIndex() == 0 ? "imageSizePrefNightBack"
				: "imageSizePrefNightFront", "-1");
		FocusPreference = prefs.getString(nightCaptureFocusPref, defaultFocus);
		OpenGLPreference = prefs.getBoolean(nightVisionLayerShowPref, true);
	}

	@Override
	public void onDefaultsSelect()
	{
		String defaultMode = "1";
		String defaultFocus = "0";

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		ModePreference = prefs.getString(nightCaptureModePref, defaultMode);
		ImageSizeIdxPreference = prefs.getString(CameraController.getCameraIndex() == 0 ? "imageSizePrefNightBack"
				: "imageSizePrefNightFront", "-1");
		FocusPreference = prefs.getString(nightCaptureFocusPref, defaultFocus);
		selectImageDimensionNight();
	}

	@Override
	public void onShowPreferences()
	{
		String defaultMode = "1";
		String defaultFocus = "0";

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		ModePreference = prefs.getString(nightCaptureModePref, defaultMode);
		ImageSizeIdxPreference = prefs.getString(CameraController.getCameraIndex() == 0 ? "imageSizePrefNightBack"
				: "imageSizePrefNightFront", "-1");
		FocusPreference = prefs.getString(nightCaptureFocusPref, defaultFocus);
		selectImageDimension();
	}

	public static void selectImageDimensionNight()
	{
		int mode = Integer.parseInt(ModePreference);

		populateCameraDimensions(1);

		long maxMem = Runtime.getRuntime().maxMemory() - Debug.getNativeHeapAllocatedSize();
		long maxMpix = (maxMem - 1000000) / 3; // 2 x Mpix - result, 1/4 x Mpix
												// x 4 - compressed input jpegs,
												// 1Mb - safe reserve

		if (maxMpix < MIN_MPIX_SUPPORTED)
		{
			String msg;
			msg = "MainScreen.selectImageDimension maxMem = " + maxMem;
			Log.e("NightCapturePlugin", "MainScreen.selectImageDimension maxMpix < MIN_MPIX_SUPPORTED");
			Log.e("NightCapturePlugin", msg);
		}

		// find index selected in preferences
		int prefIdx = -1;
		try
		{
			if (mode == 1)
				prefIdx = Integer.parseInt(NightCapturePlugin.ImageSizeIdxPreference);
			else
				prefIdx = Integer.parseInt(MainScreen.getImageSizeIndex());
		} catch (IndexOutOfBoundsException e)
		{
			prefIdx = -1;
		}

		// ----- Find max-resolution capture dimensions
		List<CameraController.Size> cs;
		int minMPIX = MIN_MPIX_SUPPORTED;
		if (mode == 1) // super mode
		{
			cs = removeDuplicateResolutions(CameraController.getInstance().getSupportedPreviewSizes());
			minMPIX = MIN_MPIX_PREVIEW;
		} else
		{
			cs = CameraController.getInstance().getSupportedPictureSizes();
			if (Build.MODEL.contains("HTC One X") && !CameraController.isFrontCamera())
			{
				CameraController.Size additional = null;
				additional = CameraController.getInstance().new Size(3264, 2448);
				additional.setWidth(3264);
				additional.setHeight(2448);
				cs.add(additional);
			}
		}

		int defaultCaptureIdx = -1;
		long defaultCaptureMpix = 0;
		int defaultCaptureWidth = 0;
		int defaultCaptureHeight = 0;
		long captureMpix = 0;
		int captureWidth = 0;
		int captureHeight = 0;
		int captureIdx = -1;
		boolean prefFound = false;

		// figure default resolution
		for (int ii = 0; ii < cs.size(); ++ii)
		{
			CameraController.Size s = cs.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((mpix >= minMPIX) && (mpix < maxMpix) && (mpix > defaultCaptureMpix) && (mpix <= MPIX_8))
			{
				defaultCaptureIdx = ii;
				defaultCaptureMpix = mpix;
				defaultCaptureWidth = s.getWidth();
				defaultCaptureHeight = s.getHeight();
			}
		}

		for (int ii = 0; ii < cs.size(); ++ii)
		{
			CameraController.Size s = cs.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((ii == prefIdx) && (mpix >= minMPIX))
			{
				prefFound = true;
				captureIdx = ii;
				captureMpix = mpix;
				captureWidth = s.getWidth();
				captureHeight = s.getHeight();
				break;
			}

			if (mpix > captureMpix)
			{
				captureIdx = ii;
				captureMpix = mpix;
				captureWidth = s.getWidth();
				captureHeight = s.getHeight();
			}
		}

		// default to about 8Mpix if nothing is set in preferences or maximum
		// resolution is above memory limits
		if (defaultCaptureMpix > 0 && !prefFound)
		{
			captureIdx = defaultCaptureIdx;
			captureMpix = defaultCaptureMpix;
			captureWidth = defaultCaptureWidth;
			captureHeight = defaultCaptureHeight;
		}

		captureIndex = captureIdx;
		CapIdx = captureIdx;
		imgCaptureWidth = captureWidth;
		imgCaptureHeight = captureHeight;
	}

	@Override
	public void selectImageDimension()
	{
		selectImageDimensionNight();
		setCameraImageSize();
	}

	private void setCameraImageSize()
	{
		if (imgCaptureWidth > 0 && imgCaptureHeight > 0)
		{
			int mode = Integer.parseInt(ModePreference);
			if (mode == 1)
			{
				CapIdx = captureIndex;
				MainScreen.setSaveImageWidth(imgCaptureWidth * 2);
				MainScreen.setSaveImageHeight(imgCaptureHeight * 2);
			} else
			{
				MainScreen.setSaveImageWidth(imgCaptureWidth);
				MainScreen.setSaveImageHeight(imgCaptureHeight);
			}

			MainScreen.setImageWidth(imgCaptureWidth);
			MainScreen.setImageHeight(imgCaptureHeight);

			String msg = "NightCapturePlugin.setCameraImageSize SX = " + MainScreen.getImageWidth() + " SY = "
					+ MainScreen.getImageHeight();
			Log.e("NightCapturePlugin", msg);
		}
	}

	@Override
	public void setCameraPreviewSize(Camera.Parameters cp)
	{
		// for super mode
		nVFframesToBuffer = 0;

		int mode = Integer.parseInt(ModePreference);

		List<CameraController.Size> cs = CameraController.getInstance().getSupportedPreviewSizes();

		if (mode == 1) // hi-speed mode - set exact preview size as selected by
						// user
		{
			CameraController.Size s = CameraController.getInstance().new Size(-1, -1);
			s.setWidth(MainScreen.getImageWidth());
			s.setHeight(MainScreen.getImageHeight());
			if (cs.contains(s))
				cp.setPreviewSize(MainScreen.getImageWidth(), MainScreen.getImageHeight());
			else
			{
				CameraController.Size os = getOptimalPreviewSize(cs, MainScreen.getImageWidth(),
						MainScreen.getImageHeight());
				cp.setPreviewSize(os.getWidth(), os.getHeight());
			}
		} else
		{
			CameraController.Size os = getOptimalPreviewSize(cs, MainScreen.getImageWidth(),
					MainScreen.getImageHeight());
			cp.setPreviewSize(os.getWidth(), os.getHeight());
		}

		if (FocusPreference.compareTo("0") == 0
				&& !CameraController.isModeAvailable(CameraController.getInstance().getSupportedFocusModes(),
						CameraParameters.AF_MODE_FIXED))
		{
			FocusPreference = "1";

			// Get the xml/preferences.xml preferences
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			SharedPreferences.Editor editor = prefs.edit();
			editor.putString(nightCaptureFocusPref, "1");
			editor.commit();
		}

		CameraController.getInstance().setCameraParameters(cp);
	}

	@Override
	public void setCameraPictureSize()
	{
		if (Integer.parseInt(ModePreference) != 1)
		{
			CameraController.getInstance().setPictureSize(MainScreen.getImageWidth(), MainScreen.getImageHeight());
			CameraController.getInstance().setJpegQuality(100);
		}

		int[] sceneModes = CameraController.getInstance().getSupportedSceneModes();
		if (sceneModes != null && CameraController.isModeAvailable(sceneModes, CameraParameters.SCENE_MODE_NIGHT)
				&& (!Build.MODEL.contains("Nexus")))
		{
			CameraController.getInstance().setCameraSceneMode(CameraParameters.SCENE_MODE_NIGHT);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(MainScreen.sSceneModePref, CameraParameters.SCENE_MODE_NIGHT);
			editor.commit();
		}

		try
		{
			int[] focusModes = CameraController.getInstance().getSupportedFocusModes();
			if (focusModes != null)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
				SharedPreferences.Editor editor = prefs.edit();

				if (FocusPreference.compareTo("0") == 0)
				{
					if (CameraController.isModeAvailable(focusModes, CameraParameters.AF_MODE_FIXED))
					{
						CameraController.getInstance().setCameraFocusMode(CameraParameters.AF_MODE_FIXED); // should
																											// set
																											// to
																											// hyperfocal
																											// distance
																											// as
																											// per
																											// android
																											// doc
						editor.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
								: MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_FIXED);
					} else if (CameraController.isModeAvailable(focusModes, CameraParameters.AF_MODE_AUTO))
					{
						CameraController.getInstance().setCameraFocusMode(CameraParameters.AF_MODE_AUTO);
						editor.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
								: MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_AUTO);
					}
				} else if (CameraController.isModeAvailable(focusModes, CameraParameters.AF_MODE_AUTO))
				{
					CameraController.getInstance().setCameraFocusMode(CameraParameters.AF_MODE_AUTO);
					editor.putInt(CameraController.isFrontCamera() ? MainScreen.sRearFocusModePref
							: MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_AUTO);
				}

				PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).edit()
						.putInt(MainScreen.sSceneModePref, CameraController.getInstance().getSceneMode()).commit();
			}

			Log.i("NightCapturePlugin", "MainScreen.setupCamera setFocusMode success");
		} catch (RuntimeException e)
		{
			Log.e("NightCapturePlugin", "MainScreen.setupCamera unable to setFocusMode");
		}

		try
		{
			int[] flashModes = CameraController.getInstance().getSupportedFlashModes();
			if (flashModes != null)
			{
				CameraController.getInstance().setCameraSceneMode(CameraParameters.SCENE_MODE_AUTO);
				CameraController.getInstance().setCameraFlashMode(CameraParameters.FLASH_MODE_OFF);

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt("FlashModeValue", CameraParameters.FLASH_MODE_OFF);
				editor.commit();
			}
		} catch (RuntimeException e)
		{
			Log.e("CameraTest", "MainScreen.setupCamera unable to setFlashMode");
		}

		Message msg = new Message();
		if (OpenGLPreference)
			msg.what = PluginManager.MSG_OPENGL_LAYER_SHOW;
		else
			msg.what = PluginManager.MSG_OPENGL_LAYER_HIDE;
		MainScreen.getMessageHandler().sendMessage(msg);
	}

	// leave only top-most resolution for each aspect ratio for super mode
	private static List<CameraController.Size> removeDuplicateResolutions(List<CameraController.Size> cs)
	{
		List<Long> mpix = new ArrayList<Long>();
		List<Integer> ratIdx = new ArrayList<Integer>();
		long[] riMaxMpix = new long[4];

		for (int i = 0; i < 4; ++i)
			riMaxMpix[i] = 0;

		for (int ii = 0; ii < cs.size(); ++ii)
		{
			CameraController.Size s = cs.get(ii);

			Long lmpix = (long) s.getWidth() * s.getHeight();
			float ratio = (float) s.getWidth() / s.getHeight();

			int ri = 0;
			if (Math.abs(ratio - 4 / 3.f) < 0.1f)
				ri = 1;
			if (Math.abs(ratio - 3 / 2.f) < 0.12f)
				ri = 2;
			if (Math.abs(ratio - 16 / 9.f) < 0.15f)
				ri = 3;

			if (lmpix > riMaxMpix[ri])
				riMaxMpix[ri] = lmpix;

			mpix.add(lmpix);
			ratIdx.add(ri);
		}

		// remove lower-than-maximum resolutions
		Iterator<CameraController.Size> it = cs.iterator();
		while (it.hasNext())
		{
			CameraController.Size s = it.next();

			Long lmpix = (long) s.getWidth() * s.getHeight();
			float ratio = (float) s.getWidth() / s.getHeight();

			int ri = 0;
			if (Math.abs(ratio - 4 / 3.f) < 0.1f)
				ri = 1;
			if (Math.abs(ratio - 3 / 2.f) < 0.12f)
				ri = 2;
			if (Math.abs(ratio - 16 / 9.f) < 0.15f)
				ri = 3;

			if (lmpix < riMaxMpix[ri])
				it.remove();
		}

		return cs;
	}

	public static void populateCameraDimensions(int mode)
	{
		ResolutionsMPixList = new ArrayList<Long>();
		ResolutionsIdxesList = new ArrayList<String>();
		ResolutionsNamesList = new ArrayList<String>();

		List<CameraController.Size> cs;
		int minMPIX = MIN_MPIX_SUPPORTED;
		if (mode == 1) // hi-speed mode
		{
			// hi-speed mode: leave only single top resolution for each aspect
			// ratio
			cs = removeDuplicateResolutions(CameraController.getInstance().getSupportedPreviewSizes());
			minMPIX = MIN_MPIX_PREVIEW;
		} else
		{
			cs = CameraController.getInstance().getSupportedPictureSizes();
			if (Build.MODEL.contains("HTC One X") && !CameraController.isFrontCamera())
			{
				CameraController.Size additional = null;
				additional = CameraController.getInstance().new Size(3264, 2448);
				additional.setWidth(3264);
				additional.setHeight(2448);
				cs.add(additional);
			}
		}

		CharSequence[] ratioStrings = { " ", "4:3", "3:2", "16:9", "1:1" };

		for (int ii = 0; ii < cs.size(); ++ii)
		{
			CameraController.Size s = cs.get(ii);

			if ((long) s.getWidth() * s.getHeight() < minMPIX)
				continue;

			// superzoom supports 12mpix output at most
			if ((mode == 1) && ((s.getWidth() > 4096 / 2) || (s.getHeight() > 3072 / 2)))
				continue;

			Long lmpix = (long) s.getWidth() * s.getHeight();
			float mpix = (float) lmpix / 1000000.f;
			float ratio = (float) s.getWidth() / s.getHeight();

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

			if (mode == 1) // hi-speed mode
				mpix *= 4;

			ResolutionsNamesList.add(loc, String.format("%3.1f Mpix  " + ratioStrings[ri], mpix));
			ResolutionsIdxesList.add(loc, String.format("%d", ii));
			ResolutionsMPixList.add(loc, lmpix);
		}

		return;
	}

	@Override
	public void onCameraParametersSetup()
	{
		populateCameraDimensions(1);
	}

	@Override
	public void onPreferenceCreate(PreferenceActivity prefActivity)
	{
		final PreferenceActivity mPref = prefActivity;

		Preference fp = prefActivity.findPreference(nightCaptureFocusPref);
		if (fp != null)
		{
			fp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				public boolean onPreferenceChange(Preference preference, Object focus_new)
				{
					int new_value = Integer.parseInt(focus_new.toString());
					if ((new_value == 0)
							&& CameraController.getInstance().getSupportedFocusModes() != null
							&& !CameraController.isModeAvailable(CameraController.getInstance()
									.getSupportedFocusModes(), CameraParameters.AF_MODE_FIXED))
					{
						new AlertDialog.Builder(mPref).setIcon(R.drawable.gui_almalence_alert_dialog_icon)
								.setTitle(R.string.Pref_NightCapture_FocusModeAlert_Title)
								.setMessage(R.string.Pref_NightCapture_FocusModeAlert_Msg)
								.setPositiveButton(android.R.string.ok, null).create().show();

						((ListPreference) preference).setValue("1");
						return false;
					}
					return true;
				}
			});
		}
	}

	@Override
	public void onPreferenceCreate(PreferenceFragment prefActivity)
	{
		final PreferenceFragment mPref = prefActivity;

		Preference fp = prefActivity.findPreference(nightCaptureFocusPref);
		if (fp != null)
		{
			fp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			{
				public boolean onPreferenceChange(Preference preference, Object focus_new)
				{
					int new_value = Integer.parseInt(focus_new.toString());
					if ((new_value == 0)
							&& CameraController.getInstance().getSupportedFocusModes() != null
							&& !CameraController.isModeAvailable(CameraController.getInstance()
									.getSupportedFocusModes(), CameraParameters.AF_MODE_FIXED))
					{
						new AlertDialog.Builder(mPref.getActivity())
								.setIcon(R.drawable.gui_almalence_alert_dialog_icon)
								.setTitle(R.string.Pref_NightCapture_FocusModeAlert_Title)
								.setMessage(R.string.Pref_NightCapture_FocusModeAlert_Msg)
								.setPositiveButton(android.R.string.ok, null).create().show();

						((ListPreference) preference).setValue("1");
						return false;
					}
					return true;
				}
			});
		}
	}

	public boolean delayedCaptureSupported()
	{
		return true;
	}

	@Override
	public void onShutterClick()
	{
		if (!takingAlready)
			startCaptureSequence();
	}

	private void startCaptureSequence()
	{
		MainScreen.getInstance().muteShutter(true);

		if (!inCapture)
		{
			Date curDate = new Date();
			SessionID = curDate.getTime();

			int mode = Integer.parseInt(ModePreference);
			if (mode == 1)
			{
				MainScreen.setSaveImageWidth(imgCaptureWidth * 2);
				MainScreen.setSaveImageHeight(imgCaptureHeight * 2);
			} else
			{
				MainScreen.setSaveImageWidth(imgCaptureWidth);
				MainScreen.setSaveImageHeight(imgCaptureHeight);
			}

			inCapture = true;
			takingAlready = false;
			this.modeSwitcher.setEnabled(false);

			LinearLayout bottom_layout = (LinearLayout) MainScreen.getInstance().findViewById(R.id.mainButtons);

			capturingDialog = Toast.makeText(MainScreen.getInstance(), R.string.hold_still, Toast.LENGTH_SHORT);
			capturingDialog.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, bottom_layout.getHeight());
			capturingDialog.show();

			// reiniting for every shutter press
			frameNumber = 0;
			total_frames = HI_RES_FRAMES;

			PluginManager.getInstance().addToSharedMem("nightmode" + SessionID, ModePreference);

			if (FocusPreference.compareTo("0") == 0)
			{
				if (!takingAlready)
				{
					captureFrame();
					takingAlready = true;
				}
			} else
			{
				int focusMode = CameraController.getInstance().getFocusMode();
				if (!takingAlready
						&& (CameraController.getFocusState() == CameraController.FOCUS_STATE_IDLE || CameraController
								.getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
						&& focusMode != -1
						&& !(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
								|| focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
								|| focusMode == CameraParameters.AF_MODE_INFINITY
								|| focusMode == CameraParameters.AF_MODE_FIXED || focusMode == CameraParameters.AF_MODE_EDOF)
						&& !MainScreen.getAutoFocusLock())
					aboutToTakePicture = true;
				else if (!takingAlready
						|| (focusMode != -1 && (focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE || focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO)))
				{
					captureFrame();
					takingAlready = true;
				} else
				{
					inCapture = false;

					PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST,
							PluginManager.MSG_CONTROL_UNLOCKED);

					MainScreen.getGUIManager().lockControls = false;
				}
			}
		}
	}

	
	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, boolean isYUV)
	{
		Log.e("Night", "onImageTaken");
		compressed_frame[frameNumber] = frame;
		compressed_frame_len[frameNumber] = frame_len;

		PluginManager.getInstance().addToSharedMem("frame" + (frameNumber + 1) + SessionID,
				String.valueOf(compressed_frame[frameNumber]));
		PluginManager.getInstance().addToSharedMem("framelen" + (frameNumber + 1) + SessionID,
				String.valueOf(compressed_frame_len[frameNumber]));

		PluginManager.getInstance().addToSharedMem("frameorientation" + (frameNumber + 1) + SessionID,
				String.valueOf(MainScreen.getGUIManager().getDisplayOrientation()));
		PluginManager.getInstance().addToSharedMem("framemirrored" + (frameNumber + 1) + SessionID,
				String.valueOf(CameraController.isFrontCamera()));
		PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
				String.valueOf(frameNumber + 1));
		
		PluginManager.getInstance().addToSharedMem("isyuv" + SessionID, String.valueOf(isYUV));

		if (frameNumber == 0 && !isYUV)
			PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(frameData, SessionID, -1);
		
		
		try
		{
			CameraController.startCameraPreview();
		} catch (RuntimeException e)
		{
			Log.e("Night", "StartPreview fail");
			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, 
					String.valueOf(SessionID));

			frameNumber = 0;
			MainScreen.getInstance().muteShutter(false);
			takingAlready = false;
			inCapture = false;
			return;
		}
		
		if (++frameNumber == total_frames)
		{			
			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

			takingAlready = false;
			inCapture = false;
		}

	}

	@TargetApi(21)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		if (result.getSequenceId() == requestID && frameNumber == 0)
			PluginManager.getInstance().addToSharedMemExifTagsFromCaptureResult(result, SessionID);
	}

	public void captureFrame()
	{
		if (Integer.parseInt(ModePreference) == 1) // hi-speed mode
		{
			nVFframesToBuffer = HI_SPEED_FRAMES;
			// play tick sound
			MainScreen.getGUIManager().startContinuousCaptureIndication();
			MainScreen.getInstance().playShutter();
			return;
		}

		if (Integer.parseInt(ModePreference) == 0) // hi-res mode
		{
			try
			{
				// play tick sound
				MainScreen.getGUIManager().showCaptureIndication();
				MainScreen.getInstance().playShutter();
				
				requestID = CameraController.captureImagesWithParams(total_frames, CameraController.YUV, new int[0], null);
			} catch (RuntimeException e)
			{
				Log.e("CameraTest", "takePicture fail in CaptureFrame (called after release?)");
			}
			return;
		}
	}

	// onPreviewFrame is used only to provide an exact delay between setExposure
	// and takePicture
	// or to collect frames in super mode
	@Override
	public void onPreviewFrame(byte[] data)
	{
		if (OpenGLPreference && !inCapture)
		{
			if (data1 == null)
				data1 = data;
			else if (data2 == null)
			{
				data2 = data;

				if (dataS == null)
					dataS = new byte[data2.length];
				else if (dataS.length < data2.length)
					dataS = new byte[data2.length];

//				Camera.Parameters params = CameraController.getInstance().getCameraParameters();
//				int imageWidth = params.getPreviewSize().width;
//				int imageHeight = params.getPreviewSize().height;
				
				int imageWidth = MainScreen.getPreviewWidth();
				int imageHeight = MainScreen.getPreviewHeight();

				ImageConversion.sumByteArraysNV21(data1, data2, dataS, imageWidth, imageHeight);
				if (CameraController.isFrontCamera())
				{
					dataRotated = new byte[dataS.length];
					ImageConversion.TransformNV21(dataS, dataRotated, imageWidth, imageHeight, 1, 0, 0);

					yuvData = dataRotated;
				} else
					yuvData = dataS;

				data1 = data2;
				data2 = null;
			}
		}
		else if(inCapture && data1 != null)
		{
			data1 = null;
			data2 = null;
			dataS = null;
		}

		if (Integer.parseInt(ModePreference) == 1 && nVFframesToBuffer != 0)
		{
			if (CameraController.isFrontCamera())
			{
				Camera.Parameters params = CameraController.getInstance().getCameraParameters();
				int imageWidth = params.getPreviewSize().width;
				int imageHeight = params.getPreviewSize().height;

				byte[] rotatedFrame = new byte[data.length];
				ImageConversion.TransformNV21(data, rotatedFrame, imageWidth, imageHeight, 1, 0, 0);

				data = rotatedFrame;
			}
			System.gc();

			// swap-out frame data to the heap
			compressed_frame[HI_SPEED_FRAMES - nVFframesToBuffer] = SwapHeap.SwapToHeap(data);
			compressed_frame_len[HI_SPEED_FRAMES - nVFframesToBuffer] = data.length;

			PluginManager.getInstance().addToSharedMem("frame" + (frameNumber + 1) + SessionID,
					String.valueOf(compressed_frame[frameNumber]));
			PluginManager.getInstance().addToSharedMem("framelen" + (frameNumber + 1) + SessionID,
					String.valueOf(compressed_frame_len[frameNumber]));

			PluginManager.getInstance().addToSharedMem("frameorientation" + (frameNumber + 1) + SessionID,
					String.valueOf(MainScreen.getGUIManager().getDisplayOrientation()));
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
					String.valueOf(frameNumber + 1));

			if (frameNumber == 0)
				PluginManager.getInstance().addToSharedMemExifTagsFromCamera(SessionID);

			++frameNumber;
			--nVFframesToBuffer;

			// all frames captured - initiate processing
			if (nVFframesToBuffer == 0)
			{
				// play tick sound
				MainScreen.getInstance().playShutter();

				PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

				MainScreen.getGUIManager().stopCaptureIndication();

				takingAlready = false;
				inCapture = false;
			}
		}
	}


	/******************************************************************************************************
	 * OpenGL layer functions
	 ******************************************************************************************************/
	@Override
	public void onGLSurfaceCreated(GL10 gl, EGLConfig config)
	{
		cameraPreview.generateGLTexture(gl);
		gl.glEnable(GL10.GL_TEXTURE_2D); // Enable Texture Mapping ( NEW )
		gl.glShadeModel(GL10.GL_SMOOTH); // Enable Smooth Shading
		gl.glLineWidth(4.0f);
		gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f); // Black Background
		gl.glClearDepthf(1.0f); // Depth Buffer Setup
		gl.glEnable(GL10.GL_DEPTH_TEST); // Enables Depth Testing
		gl.glDepthFunc(GL10.GL_LEQUAL); // The Type Of Depth Testing To Do

		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GLES10.GL_ONE);

		// Really Nice Perspective Calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}

	@Override
	public void onGLSurfaceChanged(GL10 gl, int width, int height)
	{
		if (height == 0)
		{ // Prevent A Divide By Zero By
			height = 1; // Making Height Equal One
		}

		currHalfWidth = width / 2;
		currHalfHeight = height / 2;

		cameraDist = (float) (currHalfHeight / Math.tan(Math.toRadians(45.0f / 2.0f)));

		gl.glViewport(0, 0, width, height); // Reset The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); // Select The Projection Matrix
		gl.glLoadIdentity(); // Reset The Projection Matrix

		// Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(gl, 45.0f, (float) width / (float) height, cameraDist / 10.0f, cameraDist * 10.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW); // Select The Modelview Matrix
		gl.glLoadIdentity(); // Reset The Modelview Matrix

		cameraPreview.setSurfaceSize(width, height);
	}

	@Override
	public void onGLDrawFrame(GL10 gl)
	{
		// Clear Screen And Depth Buffer
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glLoadIdentity();

		// Drawing
		gl.glTranslatef(0.0f, 0.0f, -(cameraDist)); // Move 5 units into the
													// screen
		gl.glRotatef(-90, 0.0f, 0.0f, 1.0f); // Z

		if (OpenGLPreference && !inCapture && yuvData != null)
			synchronized (this)
			{
				try
				{
					cameraPreview.draw(gl, yuvData, MainScreen.getMainContext()); // Draw
																					// the
																					// square
				} catch (RuntimeException e)
				{
					Log.e("onGLDrawFrame", "onGLDrawFrame in Night some exception" + e.getMessage());
				}
			}
	}

	/******************************************************************************************************
	 * End of OpenGL layer functions
	 ******************************************************************************************************/

	@Override
	public void onAutoFocus(boolean paramBoolean)
	{
		if (inCapture) // disregard autofocus success (paramBoolean)
		{
			if (aboutToTakePicture)
			{
				captureFrame();
				takingAlready = true;
			}

			aboutToTakePicture = false;
		}
	}

	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_NEXT_FRAME)
		{
			CameraController.startCameraPreview();
			if (++frameNumber < total_frames)
			{
				// re-open preview (closed once frame is captured)
				try
				{
					// remaining frames
					if (FocusPreference.compareTo("2") == 0 && !MainScreen.getAutoFocusLock())
					{
						takingAlready = false;
						aboutToTakePicture = true;
						CameraController.autoFocus(CameraController.getInstance());
					} else
					{
						captureFrame();
					}
				} catch (RuntimeException e)
				{
					Log.i("NightCapture plugin", "RuntimeException in MSG_NEXT_FRAME");
					// motorola's sometimes fail to restart preview after
					// onPictureTaken (fixed),
					// especially on night scene
					// just repost our request and try once more (takePicture
					// latency issues?)
					--frameNumber;
					PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_NEXT_FRAME);
				}
			} else
			{
				PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

				takingAlready = false;
				inCapture = false;
			}
			return true;
		}
		return false;
	}
}
