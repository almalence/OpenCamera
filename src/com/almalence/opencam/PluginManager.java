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
//<!-- -+-
package com.almalence.opencam;

//-+- -->

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.xmlpull.v1.XmlPullParserException;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Paint.Align;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.DngCreator;
import android.location.Location;
import android.media.ExifInterface;
import android.net.Uri;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.util.Size;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.SwapHeap;
import com.almalence.plugins.capture.bestshot.BestShotCapturePlugin;
import com.almalence.plugins.capture.burst.BurstCapturePlugin;
import com.almalence.plugins.capture.expobracketing.ExpoBracketingCapturePlugin;
import com.almalence.plugins.capture.multishot.MultiShotCapturePlugin;
import com.almalence.plugins.capture.night.NightCapturePlugin;
import com.almalence.plugins.capture.panoramaaugmented.PanoramaAugmentedCapturePlugin;
import com.almalence.plugins.capture.preshot.PreshotCapturePlugin;
import com.almalence.plugins.capture.standard.CapturePlugin;
import com.almalence.plugins.capture.video.VideoCapturePlugin;
import com.almalence.plugins.export.standard.ExportPlugin;
import com.almalence.plugins.export.standard.GPSTagsConverter;
import com.almalence.plugins.export.ExifDriver.ExifDriver;
import com.almalence.plugins.export.ExifDriver.ExifManager;
import com.almalence.plugins.export.ExifDriver.Values.ValueByteArray;
import com.almalence.plugins.export.ExifDriver.Values.ValueNumber;
import com.almalence.plugins.export.ExifDriver.Values.ValueRationals;
import com.almalence.plugins.processing.bestshot.BestshotProcessingPlugin;
import com.almalence.plugins.processing.hdr.HDRProcessingPlugin;
import com.almalence.plugins.processing.multishot.MultiShotProcessingPlugin;
import com.almalence.plugins.processing.night.NightProcessingPlugin;
import com.almalence.plugins.processing.panorama.PanoramaProcessingPlugin;
import com.almalence.plugins.processing.preshot.PreshotProcessingPlugin;
import com.almalence.plugins.processing.simple.SimpleProcessingPlugin;
import com.almalence.plugins.vf.aeawlock.AeAwLockVFPlugin;
import com.almalence.plugins.vf.barcodescanner.BarcodeScannerVFPlugin;
import com.almalence.plugins.vf.focus.FocusVFPlugin;
import com.almalence.plugins.vf.grid.GridVFPlugin;
import com.almalence.plugins.vf.gyro.GyroVFPlugin;
import com.almalence.plugins.vf.histogram.HistogramVFPlugin;
import com.almalence.plugins.vf.infoset.InfosetVFPlugin;
import com.almalence.plugins.vf.zoom.ZoomVFPlugin;
import com.almalence.util.MLocation;
/* <!-- +++
import com.almalence.opencam_plus.cameracontroller.CameraController;
import com.almalence.opencam_plus.ui.GUI.ShutterButton;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.GUI.ShutterButton;
//-+- -->

/***
 * Plugins managing class.
 * 
 * Controls plugins interaction with ApplicationScreen and processing, controls
 * different stages of activity workflow
 * 
 * may be used by other plugins to retrieve some parameters/settings from other
 * plugins
 ***/

public class PluginManager extends PluginManagerBase
{
	
	private static PluginManager		pluginManager;

	public static final String 			ACTION_NEW_PICTURE = "android.hardware.action.NEW_PICTURE";
	
	public static PluginManager getInstance()
	{
		if (pluginManager == null)
		{
			pluginManager = new PluginManager();
		}
		return pluginManager;
	}

	private PluginManager()
	{
		super();
	}
	
	// plugin manager ctor. plugins initialization and filling plugin list
	@Override
	protected void createPlugins()
	{
		pluginList = new Hashtable<String, Plugin>();

		sharedMemory = new Hashtable<String, String>();
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			createRAWCaptureResultHashtable();

		activeVF = new ArrayList<String>();
		activeFilter = new ArrayList<String>();

		listVF = new ArrayList<Plugin>();
		listCapture = new ArrayList<Plugin>();
		listProcessing = new ArrayList<Plugin>();
		listFilter = new ArrayList<Plugin>();
		listExport = new ArrayList<Plugin>();

		// init plugins and add to pluginList
		// probably will be created only active for memory saving purposes.

		/*
		 * Insert any new plugin below (create and add to list of concrete type)
		 */

		// VF
		AeAwLockVFPlugin aeawlockVFPlugin = new AeAwLockVFPlugin();
		pluginList.put(aeawlockVFPlugin.getID(), aeawlockVFPlugin);
		listVF.add(aeawlockVFPlugin);

		HistogramVFPlugin histgramVFPlugin = new HistogramVFPlugin();
		pluginList.put(histgramVFPlugin.getID(), histgramVFPlugin);
		listVF.add(histgramVFPlugin);

		BarcodeScannerVFPlugin barcodeScannerVFPlugin = new BarcodeScannerVFPlugin();
		pluginList.put(barcodeScannerVFPlugin.getID(), barcodeScannerVFPlugin);
		listVF.add(barcodeScannerVFPlugin);

		GyroVFPlugin gyroVFPlugin = new GyroVFPlugin();
		pluginList.put(gyroVFPlugin.getID(), gyroVFPlugin);
		listVF.add(gyroVFPlugin);

		ZoomVFPlugin zoomVFPlugin = new ZoomVFPlugin();
		pluginList.put(zoomVFPlugin.getID(), zoomVFPlugin);
		listVF.add(zoomVFPlugin);

		GridVFPlugin gridVFPlugin = new GridVFPlugin();
		pluginList.put(gridVFPlugin.getID(), gridVFPlugin);
		listVF.add(gridVFPlugin);

		FocusVFPlugin focusVFPlugin = new FocusVFPlugin();
		pluginList.put(focusVFPlugin.getID(), focusVFPlugin);
		listVF.add(focusVFPlugin);

		InfosetVFPlugin infosetVFPlugin = new InfosetVFPlugin();
		pluginList.put(infosetVFPlugin.getID(), infosetVFPlugin);
		listVF.add(infosetVFPlugin);

		// Capture
		CapturePlugin testCapturePlugin = new CapturePlugin();
		pluginList.put(testCapturePlugin.getID(), testCapturePlugin);
		listCapture.add(testCapturePlugin);

		ExpoBracketingCapturePlugin expoBracketingCapturePlugin = new ExpoBracketingCapturePlugin();
		pluginList.put(expoBracketingCapturePlugin.getID(), expoBracketingCapturePlugin);
		listCapture.add(expoBracketingCapturePlugin);

		NightCapturePlugin nightCapturePlugin = new NightCapturePlugin();
		pluginList.put(nightCapturePlugin.getID(), nightCapturePlugin);
		listCapture.add(nightCapturePlugin);

		BurstCapturePlugin burstCapturePlugin = new BurstCapturePlugin();
		pluginList.put(burstCapturePlugin.getID(), burstCapturePlugin);
		listCapture.add(burstCapturePlugin);

		BestShotCapturePlugin bestShotCapturePlugin = new BestShotCapturePlugin();
		pluginList.put(bestShotCapturePlugin.getID(), bestShotCapturePlugin);
		listCapture.add(bestShotCapturePlugin);

		MultiShotCapturePlugin multiShotCapturePlugin = new MultiShotCapturePlugin();
		pluginList.put(multiShotCapturePlugin.getID(), multiShotCapturePlugin);
		listCapture.add(multiShotCapturePlugin);

		VideoCapturePlugin videoCapturePlugin = new VideoCapturePlugin();
		pluginList.put(videoCapturePlugin.getID(), videoCapturePlugin);
		listCapture.add(videoCapturePlugin);

		PreshotCapturePlugin backInTimeCapturePlugin = new PreshotCapturePlugin();
		pluginList.put(backInTimeCapturePlugin.getID(), backInTimeCapturePlugin);
		listCapture.add(backInTimeCapturePlugin);

		PanoramaAugmentedCapturePlugin panoramaAugmentedCapturePlugin = new PanoramaAugmentedCapturePlugin();
		pluginList.put(panoramaAugmentedCapturePlugin.getID(), panoramaAugmentedCapturePlugin);
		listCapture.add(panoramaAugmentedCapturePlugin);

		PreshotProcessingPlugin backInTimeProcessingPlugin = new PreshotProcessingPlugin();
		pluginList.put(backInTimeProcessingPlugin.getID(), backInTimeProcessingPlugin);
		listCapture.add(backInTimeProcessingPlugin);

		// Processing
		SimpleProcessingPlugin simpleProcessingPlugin = new SimpleProcessingPlugin();
		pluginList.put(simpleProcessingPlugin.getID(), simpleProcessingPlugin);
		listProcessing.add(simpleProcessingPlugin);

		NightProcessingPlugin nightProcessingPlugin = new NightProcessingPlugin();
		pluginList.put(nightProcessingPlugin.getID(), nightProcessingPlugin);
		listProcessing.add(nightProcessingPlugin);

		HDRProcessingPlugin hdrProcessingPlugin = new HDRProcessingPlugin();
		pluginList.put(hdrProcessingPlugin.getID(), hdrProcessingPlugin);
		listProcessing.add(hdrProcessingPlugin);

		MultiShotProcessingPlugin multiShotProcessingPlugin = new MultiShotProcessingPlugin();
		pluginList.put(multiShotProcessingPlugin.getID(), multiShotProcessingPlugin);
		listProcessing.add(multiShotProcessingPlugin);

		PanoramaProcessingPlugin panoramaProcessingPlugin = new PanoramaProcessingPlugin();
		pluginList.put(panoramaProcessingPlugin.getID(), panoramaProcessingPlugin);
		listProcessing.add(panoramaProcessingPlugin);

		BestshotProcessingPlugin bestshotProcessingPlugin = new BestshotProcessingPlugin();
		pluginList.put(bestshotProcessingPlugin.getID(), bestshotProcessingPlugin);
		listProcessing.add(bestshotProcessingPlugin);

		// Filter

		// Export
		ExportPlugin testExportPlugin = new ExportPlugin();
		pluginList.put(testExportPlugin.getID(), testExportPlugin);
		listExport.add(testExportPlugin);

		// parsing configuration file to setup modes
		parseConfig();
		
		exifOrientationMap = new HashMap<Integer, Integer>()
		{
			{
				put(0, ExifInterface.ORIENTATION_NORMAL);
				put(90, ExifInterface.ORIENTATION_ROTATE_90);
				put(180, ExifInterface.ORIENTATION_ROTATE_180);
				put(270, ExifInterface.ORIENTATION_ROTATE_270);
			}
		};
	}
	
	@Override
	public void onManagerCreate()
	{
		countdownAnimation = AnimationUtils.loadAnimation(ApplicationScreen.instance,
				R.anim.plugin_capture_selftimer_countdown);
		countdownAnimation.setFillAfter(true);

		LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();
		countdownLayout = (RelativeLayout) inflator.inflate(R.layout.plugin_capture_selftimer_layout, null, false);
		countdownView = (TextView) countdownLayout.findViewById(R.id.countdown_text);

		photoTimeLapseLayout = (RelativeLayout) inflator.inflate(R.layout.plugin_capture_photo_timelapse_layout, null,
				false);
		photoTimeLapseView = (TextView) photoTimeLapseLayout.findViewById(R.id.photo_timelapse_text);		
	}

	// base onPause stage
	@Override
	public void onPause(boolean isFromMain)
	{
		// stops delayed interval timer if it's working
		if (delayedCaptureFlashPrefCommon || delayedCaptureSoundPrefCommon)
		{
			releaseSoundPlayers();
			countdownHandler.removeCallbacks(flashOff);
			finalcountdownHandler.removeCallbacks(flashBlink);
		}
		// stops timer before exit to be sure it's canceled
		if (timer != null)
		{
			timer.cancel();
			timer = null;
		}

		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onPause();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onPause();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onPause();
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onPause();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onPause();
	}

	@Override
	public void menuButtonPressed()
	{
		onShowPreferences();
		Intent settingsActivity = new Intent(MainScreen.getMainContext(), Preferences.class);
		MainScreen.getInstance().getCameraParametersBundle();
		MainScreen.getInstance().startActivity(settingsActivity);
	}

	@Override
	public void onGUICreate()
	{
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onGUICreate();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onGUICreate();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onGUICreate();
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onGUICreate();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onGUICreate();

		isRestarting = true;

		ApplicationScreen.getGUIManager().removeViews(countdownLayout, R.id.specialPluginsLayout);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);

		params.addRule(RelativeLayout.CENTER_IN_PARENT);

		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout)).addView(
				this.countdownLayout, params);

		this.countdownLayout.setLayoutParams(params);
		this.countdownLayout.requestLayout();
		this.countdownLayout.setVisibility(View.INVISIBLE);

		ApplicationScreen.getGUIManager().removeViews(photoTimeLapseLayout, R.id.specialPluginsLayout);

		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout)).addView(
				this.photoTimeLapseLayout, params);

		this.photoTimeLapseLayout.setLayoutParams(params);
		this.photoTimeLapseLayout.requestLayout();
		this.photoTimeLapseLayout.setVisibility(View.INVISIBLE);
	}

	
	@Override
	public void onShutterClick()
	{
		// <!-- -+-
		// check if plugin payed
		if (null != pluginList.get(activeCapture) && !((PluginCapture) pluginList.get(activeCapture)).getInCapture())
		{
			if (!MainScreen.checkLaunches(getActiveMode()))
			{
				ApplicationScreen.getGUIManager().lockControls = false;
				return;
			}
		}
		// -+- -->
		if (!shutterRelease)
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		delayedCaptureFlashPrefCommon = prefs.getBoolean(MainScreen.sDelayedFlashPref, false);
		delayedCaptureSoundPrefCommon = prefs.getBoolean(MainScreen.sDelayedSoundPref, false);
		int delayInterval = prefs.getInt(MainScreen.sDelayedCapturePref, 0);
		boolean showDelayedCapturePrefCommon = prefs.getBoolean(MainScreen.sShowDelayedCapturePref, false);

		photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		if (photoTimeLapseActive && pluginList.get(activeCapture).photoTimeLapseCaptureSupported())
		{
			if (isUserClicked)
			{
				Editor e = prefs.edit();
				if (photoTimeLapseIsRunning)
				{
					e.putBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);
					e.commit();
					AlarmReceiver.cancelAlarm(ApplicationScreen.instance);

					ApplicationScreen.instance.guiManager.stopCaptureIndication();
					
					ApplicationScreen.getGUIManager().lockControls = false;
					PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);
				} else
				{
					e.putInt(MainScreen.sPhotoTimeLapseCount, 0);
					e.putBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, true);
					e.commit();
					
					for (int i = 0; i < activeVF.size(); i++)
						pluginList.get(activeVF.get(i)).onShutterClick();
					pluginList.get(activeCapture).onShutterClick();
					ApplicationScreen.instance.guiManager.showCaptureIndication();
				}
				
			} else
			{
				ApplicationScreen.instance.guiManager.setShutterIcon(ShutterButton.TIMELAPSE_ACTIVE);
				for (int i = 0; i < activeVF.size(); i++)
					pluginList.get(activeVF.get(i)).onShutterClick();
				pluginList.get(activeCapture).onShutterClick();
			}
		} else {
			if (!showDelayedCapturePrefCommon || delayInterval == 0
					|| !pluginList.get(activeCapture).delayedCaptureSupported())
			{
				for (int i = 0; i < activeVF.size(); i++)
					pluginList.get(activeVF.get(i)).onShutterClick();
				if (null != pluginList.get(activeCapture)
						&& ApplicationScreen.instance.findViewById(R.id.postprocessingLayout).getVisibility() == View.GONE
						&& ApplicationScreen.instance.findViewById(R.id.blockingLayout).getVisibility() == View.GONE)
					pluginList.get(activeCapture).onShutterClick();
			} else
			{
				boolean keepScreenOn = prefs.getBoolean(MainScreen.sKeepScreenOn, false);
				if (!keepScreenOn) {
					MainScreen.getInstance().setKeepScreenOn(true);
					Handler handler = new Handler();
					handler.postDelayed(new Runnable()
					{
						@Override
						public void run()
						{
							MainScreen.getInstance().setKeepScreenOn(false);
						}
					}, delayInterval * 1000 + 500);
				}
				
				
				shutterRelease = false;
				delayedCapture(delayInterval);
			}
		}

		isUserClicked = true;
	}

	@Override
	public void onFocusButtonClick()
	{
		// <!-- -+-
		// check if plugin payed
		if (null != pluginList.get(activeCapture) && !((PluginCapture) pluginList.get(activeCapture)).getInCapture())
		{
			if (!MainScreen.checkLaunches(getActiveMode()))
			{
				ApplicationScreen.getGUIManager().lockControls = false;
				return;
			}
		}
		// -+- -->
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onFocusButtonClick();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onFocusButtonClick();
	}

	@Override
	public void loadHeaderContent(String settings, PreferenceFragment pf)
	{
		List<Plugin> activePlugins = new ArrayList<Plugin>();
		List<Plugin> inactivePlugins = new ArrayList<Plugin>();

		boolean hasInactive = false;

		loadStandardSettingsBefore(pf, settings);
		if ("general_more".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_general_more);
			ApplicationScreen.instance.onAdvancePreferenceCreate(pf);
		} else if ("general_image_size".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_general_image_size);
			if (CameraController.isUseHALv3())
			{
				Preference pref;
				if (null != (pref = pf.findPreference(ApplicationScreen.sImageSizeMultishotBackPref))
						|| null != (pref = pf.findPreference(ApplicationScreen.sImageSizeMultishotFrontPref)))
				{
					pref.setTitle(ApplicationScreen.getAppResources().getString(
							R.string.Pref_Comon_SmartMultishot_And_Super_ImageSize_Title));
				}
			}
			ApplicationScreen.instance.onPreferenceCreate(pf);
		} else if ("vf_settings".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_vf_common);
		} else if ("vf_more".equals(settings))
		{
			for (int i = 0; i < listVF.size(); i++)
			{
				Plugin pg = listVF.get(i);
				if (activeVF.contains(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, activePlugins, false);

			pf.addPreferencesFromResource(R.xml.preferences_vf_more);

			if (activePlugins.size() != listVF.size() && isPreferenecesAvailable(inactivePlugins, false))
				pf.addPreferencesFromResource(R.xml.preferences_vf_inactive);
		} else if ("vf_inactive_settings".equals(settings))
		{
			for (int i = 0; i < listVF.size(); i++)
			{
				Plugin pg = listVF.get(i);
				if (!activeVF.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, false);
		} else if ("save_configuration".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_general_saveconfiguration);
		} else if ("export_more".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_export_common);
		} else if ("export_timestamp".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_export_timestamp);
		} else if ("shooting_settings".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_modes);
		} else if ("capture_expobracketing_more".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_capture_expobracketing_more);
		} else if ("processing_expobracketing_more".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_processing_hdr_more);
		} else if ("capture_night_more".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_capture_night_more);
		} else if ("processing_night_more".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_processing_night_more);
			if (CameraController.isUseHALv3())
			{
				PreferenceScreen prefScr;
				if (null != (prefScr = (PreferenceScreen) pf.findPreference("nightProcessingMoreScreen")))
				{
					Preference pref;
					if (null != (pref = pf.findPreference("keepcolorsPref")))
					{
						prefScr.removePreference(pref);
					}
				}
			}
		} else if ("capture_preshot_more".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_capture_preshot_more);
		} else if ("capture_panorama_more".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_capture_panoramaaugmented_more);
		}
		else if ("dro".equals(settings))
		{
			AddModeSettings("single", pf);
		}else if ("burst".equals(settings))
		{
			AddModeSettings("burstmode", pf);
		} else if ("expobracketing".equals(settings))
		{
			AddModeSettings("expobracketing", pf);
		} else if ("hdr".equals(settings))
		{
			AddModeSettings("hdrmode", pf);
		} else if ("night".equals(settings))
		{
			AddModeSettings("nightmode", pf);
		} else if ("super".equals(settings))
		{
			AddModeSettings("super", pf);
		} else if ("video".equals(settings))
		{
			AddModeSettings("video", pf);
		} else if ("preshot".equals(settings))
		{
			AddModeSettings("pixfix", pf);
		} else if ("multishot".equals(settings))
		{
			AddModeSettings("multishot", pf);
		} else if ("panorama_augmented".equals(settings))
		{
			AddModeSettings("panorama_augmented", pf);
		} else if ("bestshotmode".equals(settings))
		{
			AddModeSettings("bestshotmode", pf);
		} else if ("saving_settings".equals(settings))
		{
			for (int i = 0; i < listFilter.size(); i++)
			{
				Plugin pg = listFilter.get(i);
				if (activeFilter.contains(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (activePlugins.size() != listFilter.size() && isPreferenecesAvailable(inactivePlugins, false))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, false);

			activePlugins.clear();
			inactivePlugins.clear();
			for (int i = 0; i < listExport.size(); i++)
			{
				Plugin pg = listExport.get(i);
				if (activeExport.contains(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (activePlugins.size() != listExport.size() && isPreferenecesAvailable(inactivePlugins, false))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, false);

			if (hasInactive)
				pf.addPreferencesFromResource(R.xml.preferences_saving_inactive);
		} else if ("saving_inactive_settings".equals(settings))
		{
			for (int i = 0; i < listFilter.size(); i++)
			{
				Plugin pg = listFilter.get(i);
				if (!activeFilter.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, false);

			activePlugins.clear();
			for (int i = 0; i < listExport.size(); i++)
			{
				Plugin pg = listExport.get(i);
				if (!activeExport.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, false);
		} else if ("advanced".equals(settings))
		{
			loadCommonAdvancedSettings(pf);

			for (int i = 0; i < listVF.size(); i++)
			{
				Plugin pg = listVF.get(i);
				if (activeVF.contains(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (isPreferenecesAvailable(inactivePlugins, true))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, true);

			activePlugins.clear();
			inactivePlugins.clear();
			for (int i = 0; i < listCapture.size(); i++)
			{
				Plugin pg = listCapture.get(i);
				if (activeCapture.equals(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (isPreferenecesAvailable(inactivePlugins, true))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, true);

			activePlugins.clear();
			inactivePlugins.clear();
			for (int i = 0; i < listProcessing.size(); i++)
			{
				Plugin pg = listProcessing.get(i);
				if (activeProcessing.equals(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (isPreferenecesAvailable(inactivePlugins, true))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, true);

			activePlugins.clear();
			inactivePlugins.clear();
			for (int i = 0; i < listFilter.size(); i++)
			{
				Plugin pg = listFilter.get(i);
				if (activeFilter.contains(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (isPreferenecesAvailable(inactivePlugins, true))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, true);

			activePlugins.clear();
			inactivePlugins.clear();
			for (int i = 0; i < listExport.size(); i++)
			{
				Plugin pg = listExport.get(i);
				if (activeExport.equals(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (isPreferenecesAvailable(inactivePlugins, true))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, true);

			if (hasInactive)
				pf.addPreferencesFromResource(R.xml.preferences_advance_inactive);
		} else if ("advanced_inactive".equals(settings))
		{
			for (int i = 0; i < listVF.size(); i++)
			{
				Plugin pg = listVF.get(i);
				if (!activeVF.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);

			inactivePlugins.clear();
			for (int i = 0; i < listCapture.size(); i++)
			{
				Plugin pg = listCapture.get(i);
				if (!activeCapture.equals(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);

			inactivePlugins.clear();
			for (int i = 0; i < listProcessing.size(); i++)
			{
				Plugin pg = listProcessing.get(i);
				if (!activeProcessing.equals(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);

			inactivePlugins.clear();
			for (int i = 0; i < listFilter.size(); i++)
			{
				Plugin pg = listFilter.get(i);
				if (!activeFilter.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);

			inactivePlugins.clear();
			for (int i = 0; i < listExport.size(); i++)
			{
				Plugin pg = listExport.get(i);
				if (!activeExport.equals(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);
		} else if ("plugins_settings".equals(settings))
		{
			pf.getActivity().finish();
			Preferences.closePrefs();
			MainScreen.setShowStore(true);
		}
	}

	private void loadStandardSettingsBefore(PreferenceFragment pf, String settings)
	{
		if ("general_settings".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences);
		}
	}

	private void loadCommonAdvancedSettings(PreferenceFragment pf)
	{
		pf.addPreferencesFromResource(R.xml.preferences_advanced_common);
	}

	@Override
	public void onCameraSetup()
	{
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onCameraSetup();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onCameraSetup();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		boolean photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		boolean photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		if (photoTimeLapseActive && photoTimeLapseIsRunning)
		{
			AlarmReceiver.getInstance().takePicture();
		}

	}

	/******************************************************************************************************
	 * Message handler
	 ******************************************************************************************************/
	@Override
	public boolean handleApplicationMessage(Message msg)
	{
		switch (msg.what)
		{
		case ApplicationInterface.MSG_NO_CAMERA:
			break;

		case ApplicationInterface.MSG_CAPTURE_FINISHED:
			shutterRelease = true;

			if (CameraController.getFocusMode() == CameraParameters.AF_MODE_CONTINUOUS_PICTURE) {
				CameraController.cancelAutoFocus();
			}
			
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onCaptureFinished();

			ApplicationScreen.getGUIManager().onCaptureFinished();
			ApplicationScreen.getGUIManager().startProcessingAnimation();

			// Returns actual flash mode if it was changed during capturing.
			if (!CameraController.isUseHALv3()) {
				CameraController.setCameraFlashMode(PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext()).getInt(MainScreen.sFlashModePref, -1));
			}
			
			int id = ApplicationScreen.getAppResources().getIdentifier(getActiveMode().modeName, "string",
					ApplicationScreen.instance.getPackageName());
			String modeName = ApplicationScreen.getAppResources().getString(id);
			addToSharedMem("mode_name" + (String) msg.obj, modeName);
			// start async task for further processing
			cntProcessing++;
			ProcessingTask task = new ProcessingTask(ApplicationScreen.instance);
			task.SessionID = Long.valueOf((String) msg.obj);
			task.processing = pluginList.get(activeProcessing);
			task.export = pluginList.get(activeExport);
			task.execute();
			ApplicationScreen.instance.muteShutter(false);

			// <!-- -+-
			// if mode free
			controlPremiumContent();
			// -+- -->

			if (!PluginManager.getInstance().getActiveModeID().equals("video"))
			{
				ApplicationScreen.getGUIManager().lockControls = false;
				PluginManager.getInstance()
						.sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);
			}
			
			break;

		case ApplicationInterface.MSG_CAPTURE_FINISHED_NORESULT:
			shutterRelease = true;

			if (CameraController.getFocusMode() == CameraParameters.AF_MODE_CONTINUOUS_PICTURE) {
				CameraController.cancelAutoFocus();
			}
			
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onCaptureFinished();

			ApplicationScreen.getGUIManager().onCaptureFinished();
			ApplicationScreen.getGUIManager().startProcessingAnimation();

			ApplicationScreen.instance.muteShutter(false);

			ApplicationScreen.getGUIManager().lockControls = false;

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().onExportFinished();

			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onExportFinished();

			break;

		case ApplicationInterface.MSG_START_POSTPROCESSING:
			if (null != pluginList.get(activeProcessing))
			{
				ApplicationScreen.getGUIManager().lockControls = true;
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_LOCKED);

				pluginList.get(activeProcessing).onStartPostProcessing();
				ApplicationScreen.getGUIManager().onPostProcessingStarted();
			}
			break;

		case ApplicationInterface.MSG_POSTPROCESSING_FINISHED:
			long sessionID = 0;
			String sSessionID = getFromSharedMem("sessionID");
			if (sSessionID != null)
				sessionID = Long.parseLong(getFromSharedMem("sessionID"));

			// notify GUI about saved images
			ApplicationScreen.getGUIManager().lockControls = false;
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().onPostProcessingFinished();
			if (null != pluginList.get(activeExport) && 0 != sessionID)
				pluginList.get(activeExport).onExportActive(sessionID);
			else
				ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);

			clearSharedMemory(sessionID);
			break;
		case ApplicationInterface.MSG_EXPORT_FINISHED:
			// event from plugin that saving finished and memory can be freed
			if (cntProcessing > 0)
				cntProcessing--;
			// free memory in processing
			if (null != pluginList.get(activeProcessing))
				pluginList.get(activeProcessing).freeMemory();

			// notify GUI about saved images
			ApplicationScreen.getGUIManager().onExportFinished();

			// notify capture plugins that saving finished
			if (null != pluginList.get(activeCapture))
				pluginList.get(activeCapture).onExportFinished();
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onExportFinished();

			if (ApplicationScreen.instance.getIntent().getAction() != null)
			{
				if (ApplicationScreen.instance.getIntent().getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE)
						&& MainScreen.getForceFilename() == null)
				{
					ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_RETURN_CAPTURED);
				}
			}
			
			if (photoTimeLapseActive && photoTimeLapseIsRunning) {
				AlarmReceiver.getInstance().setNextAlarm(ApplicationScreen.instance.getApplicationContext());
				ApplicationScreen.instance.guiManager.showCaptureIndication();
			}
			break;

		case ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION:
			// event from plugin that saving finished and memory can be freed
			if (cntProcessing > 0)
				cntProcessing--;
			// free memory in processing
			if (null != pluginList.get(activeProcessing))
				pluginList.get(activeProcessing).freeMemory();

			// notify GUI about saved images
			ApplicationScreen.getGUIManager().onExportFinished();

			// notify capture plugins that saving finished
			if (null != pluginList.get(activeCapture))
				pluginList.get(activeCapture).onExportFinished();
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onExportFinished();

			Toast.makeText(ApplicationScreen.getMainContext(), "Can't save data - seems no free space left.",
					Toast.LENGTH_LONG).show();
			
			if (photoTimeLapseActive && photoTimeLapseIsRunning) {
				AlarmReceiver.getInstance().setNextAlarm(ApplicationScreen.instance.getApplicationContext());
				ApplicationScreen.instance.guiManager.showCaptureIndication();
			}
			break;

		case ApplicationInterface.MSG_DELAYED_CAPTURE:
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onShutterClick();
			if (null != pluginList.get(activeCapture)
					&& ApplicationScreen.instance.findViewById(R.id.postprocessingLayout).getVisibility() == View.GONE)
				pluginList.get(activeCapture).onShutterClick();
			break;

		case ApplicationInterface.MSG_RETURN_CAPTURED:
			ApplicationScreen.instance.setResult(Activity.RESULT_OK);
			ApplicationScreen.instance.finish();
			break;

		case ApplicationInterface.MSG_OPENGL_LAYER_SHOW:
			ApplicationScreen.instance.showOpenGLLayer(1);
			break;

		case ApplicationInterface.MSG_OPENGL_LAYER_SHOW_V2:
			ApplicationScreen.instance.showOpenGLLayer(2);
			break;

		case ApplicationInterface.MSG_OPENGL_LAYER_HIDE:
			ApplicationScreen.instance.hideOpenGLLayer();
			break;

		case ApplicationInterface.MSG_OPENGL_LAYER_RENDERMODE_CONTINIOUS:
			ApplicationScreen.instance.glSetRenderingMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
			break;

		case ApplicationInterface.MSG_OPENGL_LAYER_RENDERMODE_WHEN_DIRTY:
			ApplicationScreen.instance.glSetRenderingMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			break;

		case ApplicationInterface.MSG_PROCESSING_BLOCK_UI:
			ApplicationScreen.getGUIManager().processingBlockUI();
			break;

		case ApplicationInterface.MSG_BROADCAST:
			pluginManager.onBroadcast(msg.arg1, msg.arg2);
			break;
		default:
			break;
		}

		return true;
	}

	// <!-- -+-
	public void controlPremiumContent()
	{
		Mode mode = getActiveMode();
		if (mode.SKU != null)
			MainScreen.getInstance().decrementLeftLaunches(mode.modeID);
	}

	// -+- -->

	// delayed capture feature
	private SoundPlayer		countdownPlayer					= null;
	private SoundPlayer		finalcountdownPlayer			= null;

	private CountDownTimer	timer							= null;

	public int				flashModeBackUp					= -1;

	final Handler			countdownHandler				= new Handler();
	final Handler			finalcountdownHandler			= new Handler();

	private RelativeLayout	countdownLayout					= null;
	private TextView		countdownView					= null;

	private RelativeLayout	photoTimeLapseLayout			= null;
	private TextView		photoTimeLapseView				= null;

	private Animation		countdownAnimation				= null;

	private boolean			delayedCaptureFlashPrefCommon	= false;
	private boolean			delayedCaptureSoundPrefCommon	= false;

	private boolean			shutterRelease					= true;

	private void delayedCapture(int delayInterval)
	{
		initializeSoundPlayers(
				ApplicationScreen.getAppResources().openRawResourceFd(R.raw.plugin_capture_selftimer_countdown), ApplicationScreen
						.getAppResources().openRawResourceFd(R.raw.plugin_capture_selftimer_finalcountdown));
		countdownHandler.removeCallbacks(flashOff);
		finalcountdownHandler.removeCallbacks(flashBlink);

		timer = new CountDownTimer(delayInterval * 1000 + 500, 1000)
		{
			public void onTick(long millisUntilFinished)
			{
				countdownView.setRotation(90 - MainScreen.getOrientation());
				countdownView.setText(String.valueOf(millisUntilFinished / 1000));
				countdownView.clearAnimation();
				countdownLayout.setVisibility(View.VISIBLE);
				countdownView.startAnimation(countdownAnimation);

				if (!delayedCaptureFlashPrefCommon && !delayedCaptureSoundPrefCommon)
					return;

				TickEverySecond((millisUntilFinished / 1000 <= 1) ? true : false);

				if (delayedCaptureFlashPrefCommon)
				{
					if (millisUntilFinished > 1000)
					{
						try
						{
							flashModeBackUp = CameraController.getFlashMode();
							CameraController.setCameraFlashMode(CameraParameters.FLASH_MODE_TORCH);
						} catch (Exception e)
						{
							e.printStackTrace();
							Log.e("Self-timer", "Torch exception: " + e.getMessage());
						}
						countdownHandler.postDelayed(flashOff, 50);
					}
				}
			}

			public void onFinish()
			{
				countdownView.clearAnimation();
				countdownLayout.setVisibility(View.GONE);

				countdownHandler.removeCallbacks(flashOff);
				finalcountdownHandler.removeCallbacks(flashBlink);
				if (delayedCaptureFlashPrefCommon)
					if (CameraController.getSupportedFlashModes() != null)
						CameraController.setCameraFlashMode(flashModeBackUp);

				Message msg = new Message();
				msg.what = ApplicationInterface.MSG_DELAYED_CAPTURE;
				ApplicationScreen.getMessageHandler().sendMessage(msg);

				timer = null;
			}
		};
		timer.start();
	}

	public void TickEverySecond(boolean isLastSecond)
	{
		if (ApplicationScreen.instance.isShutterSoundEnabled())
			return;
		if (delayedCaptureSoundPrefCommon)
		{
			if (isLastSecond)
			{
				if (finalcountdownPlayer != null)
					finalcountdownPlayer.play();
			} else
			{
				if (countdownPlayer != null)
					countdownPlayer.play();
			}
		}
	}

	public void initializeSoundPlayers(AssetFileDescriptor fd_countdown, AssetFileDescriptor fd_finalcountdown)
	{
		countdownPlayer = new SoundPlayer(ApplicationScreen.getMainContext(), fd_countdown);
		finalcountdownPlayer = new SoundPlayer(ApplicationScreen.getMainContext(), fd_finalcountdown);
	}

	public void releaseSoundPlayers()
	{
		if (countdownPlayer != null)
		{
			countdownPlayer.release();
			countdownPlayer = null;
		}

		if (finalcountdownPlayer != null)
		{
			finalcountdownPlayer.release();
			finalcountdownPlayer = null;
		}
	}

	private Runnable	flashOff	= new Runnable()
									{
										public void run()
										{
											CameraController.setCameraFlashMode(CameraParameters.FLASH_MODE_OFF);
										}
									};

	private Runnable	flashBlink	= new Runnable()
									{
										boolean	isFlashON	= false;

										public void run()
										{
											try
											{
												if (isFlashON)
												{
													CameraController
															.setCameraFlashMode(CameraParameters.FLASH_MODE_OFF);
													isFlashON = false;
												} else
												{
													CameraController
															.setCameraFlashMode(CameraParameters.FLASH_MODE_TORCH);
													isFlashON = true;
												}
											} catch (Exception e)
											{
												e.printStackTrace();
												Log.e("Self-timer",
														"finalcountdownHandler exception: " + e.getMessage());
											}
											finalcountdownHandler.postDelayed(this, 50);
										}
									};

	// Saving
	private void copyFromForceFileName(File dst) throws IOException
	{
		InputStream in = ApplicationScreen.instance.getContentResolver()
				.openInputStream(MainScreen.getForceFilenameURI());
		OutputStream out = new FileOutputStream(dst);

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	private void copyToForceFileName(File src) throws IOException
	{
		InputStream in = new FileInputStream(src);
		OutputStream out = ApplicationScreen.instance.getContentResolver()
				.openOutputStream(MainScreen.getForceFilenameURI());

		// Transfer bytes from in to out
		byte[] buf = new byte[1024];
		int len;
		while ((len = in.read(buf)) > 0)
		{
			out.write(buf, 0, len);
		}
		in.close();
		out.close();
	}

	private int		saveOption;
	private boolean	useGeoTaggingPrefExport;
	private boolean	enableExifTagOrientation;
	private int		additionalRotation;
	private int		additionalRotationValue	= 0;
	private boolean	photoTimeLapseActive;
	private boolean	photoTimeLapseIsRunning;

	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		saveOption = Integer.parseInt(prefs.getString(MainScreen.sExportNamePref, "2"));
		useGeoTaggingPrefExport = prefs.getBoolean("useGeoTaggingPrefExport", false);
		enableExifTagOrientation = prefs.getBoolean(MainScreen.sEnableExifOrientationTagPref, true);
		additionalRotation = Integer.parseInt(prefs.getString(MainScreen.sAdditionalRotationPref, "0"));
		photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		switch (additionalRotation)
		{
		case 0:
			additionalRotationValue = 0;
			break;
		case 1:
			additionalRotationValue = -90;
			break;
		case 2:
			additionalRotationValue = 90;
			break;
		case 3:
			additionalRotationValue = 180;
			break;
		}
	}

	
	@Override
	public void saveResultPicture(long sessionID)
	{
		getPrefs();
		// save fused result
		try
		{
			File saveDir = getSaveDir(false);

			Calendar d = Calendar.getInstance();

			int imagesAmount = Integer.parseInt(getFromSharedMem("amountofresultframes" + Long.toString(sessionID)));
			
			if (imagesAmount == 0)
				imagesAmount = 1;

			int imageIndex = 0;
			String sImageIndex = getFromSharedMem("resultframeindex" + Long.toString(sessionID));
			if (sImageIndex != null)
				imageIndex = Integer.parseInt(getFromSharedMem("resultframeindex" + Long.toString(sessionID)));

			if (imageIndex != 0)
				imagesAmount = 1;

			ContentValues values = null;

			boolean hasDNGResult = false;
			for (int i = 1; i <= imagesAmount; i++)
			{
				// Take only one result frame from several results
				// Used for PreShot plugin that may decide which result to save
				if (imagesAmount == 1 && imageIndex != 0)
					i = imageIndex;
				
				String format = getFromSharedMem("resultframeformat" + i + Long.toString(sessionID));
				
				if(format != null && format.equalsIgnoreCase("dng"))
					hasDNGResult = true;
				
				String idx = "";

				if (imagesAmount != 1)
					idx += "_" + ((format != null && !format.equalsIgnoreCase("dng") && hasDNGResult) ? i - imagesAmount/2 : i);

				String modeName = getFromSharedMem("modeSaveName" + Long.toString(sessionID));
				// define file name format. from settings!
				String fileFormat = getExportFileName(modeName);
				fileFormat += idx + ((format != null && format.equalsIgnoreCase("dng"))? ".dng" : ".jpg");

				File file;
				if (MainScreen.getForceFilename() == null)
				{
					file = new File(saveDir, fileFormat);
				} else
				{
					file = MainScreen.getForceFilename();
				}

				OutputStream os = null;
				if (MainScreen.getForceFilename() != null)
				{
					os = ApplicationScreen.instance.getContentResolver()
							.openOutputStream(MainScreen.getForceFilenameURI());
				} else
				{
					try
					{
						os = new FileOutputStream(file);
					} catch (Exception e)
					{
						// save always if not working saving to sdcard
						e.printStackTrace();
						saveDir = getSaveDir(true);
						if (MainScreen.getForceFilename() == null)
						{
							file = new File(saveDir, fileFormat);
						} else
						{
							file = MainScreen.getForceFilename();
						}
						os = new FileOutputStream(file);
					}
				}

				

				String resultOrientation = getFromSharedMem("resultframeorientation" + i + Long.toString(sessionID));
				int orientation = 0;
				if (resultOrientation != null)
					orientation = Integer.parseInt(resultOrientation);

				String resultMirrored = getFromSharedMem("resultframemirrored" + i + Long.toString(sessionID));
				Boolean cameraMirrored = false;
				if (resultMirrored != null)
					cameraMirrored = Boolean.parseBoolean(resultMirrored);

				int x = Integer.parseInt(getFromSharedMem("saveImageHeight" + Long.toString(sessionID)));
				int y = Integer.parseInt(getFromSharedMem("saveImageWidth" + Long.toString(sessionID)));
				if (orientation == 0 || orientation == 180 || (format != null && format.equalsIgnoreCase("dng")))
				{
					x = Integer.valueOf(getFromSharedMem("saveImageWidth" + Long.toString(sessionID)));
					y = Integer.valueOf(getFromSharedMem("saveImageHeight" + Long.toString(sessionID)));
				}

				Boolean writeOrientationTag = true;
				String writeOrientTag = getFromSharedMem("writeorientationtag" + Long.toString(sessionID));
				if (writeOrientTag != null)
					writeOrientationTag = Boolean.parseBoolean(writeOrientTag);

				if (format != null && format.equalsIgnoreCase("jpeg"))
				{// if result in jpeg format

					if (os != null)
					{
						byte[] frame = SwapHeap.SwapFromHeap(
								Integer.parseInt(getFromSharedMem("resultframe" + i + Long.toString(sessionID))),
								Integer.parseInt(getFromSharedMem("resultframelen" + i + Long.toString(sessionID))));
						os.write(frame);
						try
						{
							os.close();
						} catch (Exception e)
						{
							e.printStackTrace();
						}
					}
				}
				else if(format != null && format.equalsIgnoreCase("dng"))
				{
					saveDNGPicture(i, sessionID, os, x, y, orientation, cameraMirrored);
				}
				else
				{// if result in nv21 format
					int yuv = Integer.parseInt(getFromSharedMem("resultframe" + i + Long.toString(sessionID)));
					com.almalence.YuvImage out = new com.almalence.YuvImage(yuv, ImageFormat.NV21, x, y, null);
					Rect r;

					String res = getFromSharedMem("resultfromshared" + Long.toString(sessionID));
					if ((null == res) || "".equals(res) || "true".equals(res))
					{
						// to avoid problems with SKIA
						int cropHeight = out.getHeight() - out.getHeight() % 16;
						r = new Rect(0, 0, out.getWidth(), cropHeight);
					} else
					{
						if (null == getFromSharedMem("resultcrop0" + Long.toString(sessionID)))
						{
							// to avoid problems with SKIA
							int cropHeight = out.getHeight() - out.getHeight() % 16;
							r = new Rect(0, 0, out.getWidth(), cropHeight);
						} else
						{
							int crop0 = Integer.parseInt(getFromSharedMem("resultcrop0" + Long.toString(sessionID)));
							int crop1 = Integer.parseInt(getFromSharedMem("resultcrop1" + Long.toString(sessionID)));
							int crop2 = Integer.parseInt(getFromSharedMem("resultcrop2" + Long.toString(sessionID)));
							int crop3 = Integer.parseInt(getFromSharedMem("resultcrop3" + Long.toString(sessionID)));

							r = new Rect(crop0, crop1, crop0 + crop2, crop1 + crop3);

						}
					}

					SharedPreferences prefs = PreferenceManager
							.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
					jpegQuality = Integer.parseInt(prefs.getString(ApplicationScreen.sJPEGQualityPref, "95"));
					if (!out.compressToJpeg(r, jpegQuality, os))
					{
						ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION);
						return;
					}
					SwapHeap.FreeFromHeap(yuv);
				}

				String orientation_tag = String.valueOf(0);
				int sensorOrientation = CameraController.getSensorOrientation();
				int displayOrientation = CameraController.getDisplayOrientation();
				sensorOrientation = (360+ sensorOrientation + (cameraMirrored ? -displayOrientation: displayOrientation))%360;
				
				if(Build.MODEL.equals("Nexus 6") && cameraMirrored)
					orientation = (orientation + 180)%360;
					
				switch (orientation)
				{
				default:
				case 0:
					orientation_tag = String.valueOf(0);
//					orientation_tag = cameraMirrored ? String.valueOf((270 - sensorOrientation)%360) : String.valueOf(0);
					break;
				case 90:
					orientation_tag = cameraMirrored ? String.valueOf(270) : String.valueOf(90);
//					orientation_tag = String.valueOf(sensorOrientation);
					break;
				case 180:
					orientation_tag = String.valueOf(180);
//					orientation_tag = cameraMirrored ? String.valueOf(((270 - sensorOrientation)%360 + 180)%360) : String.valueOf(180);
					break;
				case 270:
					orientation_tag = cameraMirrored ? String.valueOf(90) : String.valueOf(270);
//					orientation_tag = cameraMirrored ? String.valueOf((sensorOrientation + 180)%360) : String.valueOf(270);
					break;
				}

				int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
				if (writeOrientationTag)
				{
					switch ((orientation + additionalRotationValue + 360) % 360)
					{
					default:
					case 0:
//						exif_orientation = exifOrientationMap.get(cameraMirrored ? (270 - sensorOrientation)%360 : 0);
						exif_orientation = ExifInterface.ORIENTATION_NORMAL;
						break;
					case 90:
//						exif_orientation = exifOrientationMap.get(sensorOrientation);
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270
								: ExifInterface.ORIENTATION_ROTATE_90;
						break;
					case 180:
//						exif_orientation = exifOrientationMap.get(cameraMirrored ? ((270 - sensorOrientation)%360 + 180)%360 : 180);
						exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
						break;
					case 270:
//						exif_orientation = exifOrientationMap.get(cameraMirrored ? (sensorOrientation + 180)%360 : 270);
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_90
								: ExifInterface.ORIENTATION_ROTATE_270;
						break;
					}
				} else
				{
					switch ((additionalRotationValue + 360) % 360)
					{
					default:
					case 0:
						exif_orientation = ExifInterface.ORIENTATION_NORMAL;
						break;
					case 90:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_270
								: ExifInterface.ORIENTATION_ROTATE_90;
						break;
					case 180:
						exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
						break;
					case 270:
						exif_orientation = cameraMirrored ? ExifInterface.ORIENTATION_ROTATE_90
								: ExifInterface.ORIENTATION_ROTATE_270;
						break;
					}
				}

				if (!enableExifTagOrientation)
					exif_orientation = ExifInterface.ORIENTATION_NORMAL;

				File parent = file.getParentFile();
				String path = parent.toString().toLowerCase();
				String name = parent.getName().toLowerCase();

				values = new ContentValues();
				values.put(
						ImageColumns.TITLE,
						file.getName().substring(
								0,
								file.getName().lastIndexOf(".") >= 0 ? file.getName().lastIndexOf(".") : file.getName()
										.length()));
				values.put(ImageColumns.DISPLAY_NAME, file.getName());
				values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
				values.put(ImageColumns.MIME_TYPE, "image/jpeg");

				if (enableExifTagOrientation)
				{
					if (writeOrientationTag)
					{
						values.put(ImageColumns.ORIENTATION, String.valueOf((Integer.parseInt(orientation_tag)
								+ additionalRotationValue + 360) % 360));
					} else
					{
						values.put(ImageColumns.ORIENTATION, String.valueOf((additionalRotationValue + 360) % 360));
					}
				} else
				{
					values.put(ImageColumns.ORIENTATION, String.valueOf(0));
				}

				values.put(ImageColumns.BUCKET_ID, path.hashCode());
				values.put(ImageColumns.BUCKET_DISPLAY_NAME, name);
				values.put(ImageColumns.DATA, file.getAbsolutePath());

				File tmpFile;
				if (MainScreen.getForceFilename() == null)
				{
					tmpFile = file;
				} else
				{
					tmpFile = new File(saveDir, "external.tmp");
					tmpFile.createNewFile();
					copyFromForceFileName(tmpFile);
				}

				if (!enableExifTagOrientation)
				{
					Matrix matrix = new Matrix();
					if (writeOrientationTag && (orientation + additionalRotationValue) != 0)
					{
						matrix.postRotate((orientation + additionalRotationValue + 360) % 360);
						rotateImage(tmpFile, matrix);
					} else if (!writeOrientationTag && additionalRotationValue != 0)
					{
						matrix.postRotate((additionalRotationValue + 360) % 360);
						rotateImage(tmpFile, matrix);
					}
				}

				// Set tag_model using ExifInterface.
				// If we try set tag_model using ExifDriver, then standard
				// gallery of android (Nexus 4) will crash on this file.
				// Can't figure out why, other Exif tools work fine.
				ExifInterface ei = new ExifInterface(tmpFile.getAbsolutePath());

				String tag_model = getFromSharedMem("exiftag_model" + Long.toString(sessionID));
				String tag_make = getFromSharedMem("exiftag_make" + Long.toString(sessionID));
				if (tag_model != null)
				{
					ei.setAttribute(ExifInterface.TAG_MODEL, tag_model);
					ei.setAttribute(ExifInterface.TAG_MAKE, tag_make);
					ei.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(exif_orientation));
				}
				ei.saveAttributes();
				addTimestamp(tmpFile);

				// Open ExifDriver.
				ExifDriver exifDriver = ExifDriver.getInstance(tmpFile.getAbsolutePath());
				ExifManager exifManager = null;
				if (exifDriver != null)
				{
					exifManager = new ExifManager(exifDriver, ApplicationScreen.instance);
				}

				if (useGeoTaggingPrefExport)
				{
					Location l = MLocation.getLocation(ApplicationScreen.getMainContext());

					if (l != null)
					{
						double lat = l.getLatitude();
						double lon = l.getLongitude();
						boolean hasLatLon = (lat != 0.0d) || (lon != 0.0d);

						if (hasLatLon)
						{
							exifManager.setGPSLocation(l.getLatitude(), l.getLongitude(), l.getAltitude());

							values.put(ImageColumns.LATITUDE, l.getLatitude());
							values.put(ImageColumns.LONGITUDE, l.getLongitude());
						}

						String GPSDateString = new SimpleDateFormat("yyyy:MM:dd").format(new Date(l.getTime()));
						if (GPSDateString != null)
						{
							ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
							value.setBytes(GPSDateString.getBytes());
							exifDriver.getIfdGps().put(ExifDriver.TAG_GPS_DATE_STAMP, value);
						}
					}
				}

				String tag_exposure_time = getFromSharedMem("exiftag_exposure_time" + Long.toString(sessionID));
				String tag_aperture = getFromSharedMem("exiftag_aperture" + Long.toString(sessionID));
				String tag_flash = getFromSharedMem("exiftag_flash" + Long.toString(sessionID));
				String tag_focal_length = getFromSharedMem("exiftag_focal_lenght" + Long.toString(sessionID));
				String tag_iso = getFromSharedMem("exiftag_iso" + Long.toString(sessionID));
				String tag_white_balance = getFromSharedMem("exiftag_white_balance" + Long.toString(sessionID));
				String tag_spectral_sensitivity = getFromSharedMem("exiftag_spectral_sensitivity"
						+ Long.toString(sessionID));
				String tag_version = getFromSharedMem("exiftag_version" + Long.toString(sessionID));
				String tag_scene = getFromSharedMem("exiftag_scene_capture_type" + Long.toString(sessionID));
				String tag_metering_mode = getFromSharedMem("exiftag_metering_mode" + Long.toString(sessionID));

				if (exifDriver != null)
				{
					if (tag_exposure_time != null)
					{
						int[][] ratValue = ExifManager.stringToRational(tag_exposure_time);
						if (ratValue != null)
						{
							ValueRationals value = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
							value.setRationals(ratValue);
							exifDriver.getIfdExif().put(ExifDriver.TAG_EXPOSURE_TIME, value);
						}
					} else
					{ // hack for expo bracketing
						tag_exposure_time = getFromSharedMem("exiftag_exposure_time" + Integer.toString(i)
								+ Long.toString(sessionID));
						if (tag_exposure_time != null)
						{
							int[][] ratValue = ExifManager.stringToRational(tag_exposure_time);
							if (ratValue != null)
							{
								ValueRationals value = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
								value.setRationals(ratValue);
								exifDriver.getIfdExif().put(ExifDriver.TAG_EXPOSURE_TIME, value);
							}
						}
					}
					if (tag_aperture != null)
					{
						int[][] ratValue = ExifManager.stringToRational(tag_aperture);
						if (ratValue != null)
						{
							ValueRationals value = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
							value.setRationals(ratValue);
							exifDriver.getIfdExif().put(ExifDriver.TAG_FNUMBER, value);
						}
						
						// TAG_FNUMBER and TAG_APERTURE_VALUE have same value. But it's stored in different ways.
						// TAG_APERTURE_VALUE is actual aperture value of lens when the image was taken. 
						// To convert this value to ordinary F-number(F-stop), 
						// calculate this value's power of root 2 (=1.4142). 
						// For example, if value is '5', F-number is 1.4142^5 = F5.6.
						// So, to get actual aperture value from F-number we need to take Log. 
						Double aperture = Math.log(Double.valueOf(tag_aperture)) / Math.log(Double.valueOf(Math.sqrt(2.d)));
						int[][] ratValueApp = ExifManager.stringToRational(String.format("%.3f", aperture));
						if (ratValueApp != null)
						{
							ValueRationals value = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
							value.setRationals(ratValueApp);
							exifDriver.getIfdExif().put(ExifDriver.TAG_APERTURE_VALUE, value);
						}
					}
					if (tag_flash != null)
					{
						ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT,
								Integer.parseInt(tag_flash));
						exifDriver.getIfdExif().put(ExifDriver.TAG_FLASH, value);
					}
					if (tag_focal_length != null)
					{
						int[][] ratValue = ExifManager.stringToRational(tag_focal_length);
						if (ratValue != null)
						{
							ValueRationals value = new ValueRationals(ExifDriver.FORMAT_UNSIGNED_RATIONAL);
							value.setRationals(ratValue);
							exifDriver.getIfdExif().put(ExifDriver.TAG_FOCAL_LENGTH, value);
						}
					}
					try
					{
						if (tag_iso != null)
						{
							if (tag_iso.indexOf("ISO") > 0)
							{
								tag_iso = tag_iso.substring(0, 2);
							}
							ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT,
									Integer.parseInt(tag_iso));
							exifDriver.getIfdExif().put(ExifDriver.TAG_ISO_SPEED_RATINGS, value);
						}
					} catch (Exception e)
					{
						e.printStackTrace();
					}
					if (tag_scene != null)
					{
						ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT,
								Integer.parseInt(tag_scene));
						exifDriver.getIfdExif().put(ExifDriver.TAG_SCENE_CAPTURE_TYPE, value);
					} else
					{
						int sceneMode = CameraController.getSceneMode();

						int sceneModeVal = 0;
						if (sceneMode == CameraParameters.SCENE_MODE_LANDSCAPE)
						{
							sceneModeVal = 1;
						} else if (sceneMode == CameraParameters.SCENE_MODE_PORTRAIT)
						{
							sceneModeVal = 2;
						} else if (sceneMode == CameraParameters.SCENE_MODE_NIGHT)
						{
							sceneModeVal = 3;
						}

						ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, sceneModeVal);
						exifDriver.getIfdExif().put(ExifDriver.TAG_SCENE_CAPTURE_TYPE, value);
					}
					if (tag_white_balance != null)
					{
						exifDriver.getIfd0().remove(ExifDriver.TAG_LIGHT_SOURCE);

						ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT,
								Integer.parseInt(tag_white_balance));
						exifDriver.getIfdExif().put(ExifDriver.TAG_WHITE_BALANCE, value);
						exifDriver.getIfdExif().put(ExifDriver.TAG_LIGHT_SOURCE, value);
					} else
					{
						exifDriver.getIfd0().remove(ExifDriver.TAG_LIGHT_SOURCE);

						int whiteBalance = CameraController.getWBMode();
						int whiteBalanceVal = 0;
						int lightSourceVal = 0;
						if (whiteBalance == CameraParameters.WB_MODE_AUTO)
						{
							whiteBalanceVal = 0;
							lightSourceVal = 0;
						} else
						{
							whiteBalanceVal = 1;
							lightSourceVal = 0;
						}

						if (whiteBalance == CameraParameters.WB_MODE_DAYLIGHT)
						{
							lightSourceVal = 1;
						} else if (whiteBalance == CameraParameters.WB_MODE_FLUORESCENT)
						{
							lightSourceVal = 2;
						} else if (whiteBalance == CameraParameters.WB_MODE_WARM_FLUORESCENT)
						{
							lightSourceVal = 2;
						} else if (whiteBalance == CameraParameters.WB_MODE_INCANDESCENT)
						{
							lightSourceVal = 3;
						} else if (whiteBalance == CameraParameters.WB_MODE_TWILIGHT)
						{
							lightSourceVal = 3;
						} else if (whiteBalance == CameraParameters.WB_MODE_CLOUDY_DAYLIGHT)
						{
							lightSourceVal = 10;
						} else if (whiteBalance == CameraParameters.WB_MODE_SHADE)
						{
							lightSourceVal = 11;
						}

						ValueNumber valueWB = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, whiteBalanceVal);
						exifDriver.getIfdExif().put(ExifDriver.TAG_WHITE_BALANCE, valueWB);

						ValueNumber valueLS = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, lightSourceVal);
						exifDriver.getIfdExif().put(ExifDriver.TAG_LIGHT_SOURCE, valueLS);
					}
					if (tag_spectral_sensitivity != null)
					{
						ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
						value.setBytes(tag_spectral_sensitivity.getBytes());
						exifDriver.getIfd0().put(ExifDriver.TAG_SPECTRAL_SENSITIVITY, value);
					}
					if (tag_version != null && !tag_version.equals("48 50 50 48"))
					{
						ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
						value.setBytes(tag_version.getBytes());
						exifDriver.getIfd0().put(ExifDriver.TAG_EXIF_VERSION, value);
					} else
					{
						ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
						byte[] version = { (byte) 48, (byte) 50, (byte) 50, (byte) 48 };
						value.setBytes(version);
						exifDriver.getIfd0().put(ExifDriver.TAG_EXIF_VERSION, value);
					}
					if (tag_metering_mode != null && !tag_metering_mode.equals("")
							&& Integer.parseInt(tag_metering_mode) <= 255)
					{
						ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT,
								Integer.parseInt(tag_metering_mode));
						exifDriver.getIfdExif().put(ExifDriver.TAG_METERING_MODE, value);
						exifDriver.getIfd0().put(ExifDriver.TAG_METERING_MODE, value);
					} else
					{
						ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, 0);
						exifDriver.getIfdExif().put(ExifDriver.TAG_METERING_MODE, value);
						exifDriver.getIfd0().put(ExifDriver.TAG_METERING_MODE, value);
					}

					ValueNumber xValue = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_LONG, x);
					exifDriver.getIfdExif().put(ExifDriver.TAG_IMAGE_WIDTH, xValue);

					ValueNumber yValue = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_LONG, y);
					exifDriver.getIfdExif().put(ExifDriver.TAG_IMAGE_HEIGHT, yValue);

					String dateString = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss").format(new Date());
					if (dateString != null)
					{
						ValueByteArray value = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
						// Date string length is 19 bytes. But exif tag
						// specification length is 20 bytes.
						// That's why we add "empty" byte (0x00) in the end.
						byte[] bytes = dateString.getBytes();
						byte[] res = new byte[20];
						for (int ii = 0; ii < bytes.length; ii++)
						{
							res[ii] = bytes[ii];
						}
						res[19] = 0x00;
						value.setBytes(res);
						exifDriver.getIfd0().put(ExifDriver.TAG_DATETIME, value);
						exifDriver.getIfdExif().put(ExifDriver.TAG_DATETIME_DIGITIZED, value);
						exifDriver.getIfdExif().put(ExifDriver.TAG_DATETIME_ORIGINAL, value);
					}

					// extract mode name
					String tag_modename = getFromSharedMem("mode_name" + Long.toString(sessionID));
					if (tag_modename == null)
						tag_modename = "";
					String softwareString = ApplicationScreen.getAppResources().getString(R.string.app_name) + ", "
							+ tag_modename;
					ValueByteArray softwareValue = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
					softwareValue.setBytes(softwareString.getBytes());
					exifDriver.getIfd0().put(ExifDriver.TAG_SOFTWARE, softwareValue);

					if (enableExifTagOrientation)
					{
						ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, exif_orientation);
						exifDriver.getIfd0().put(ExifDriver.TAG_ORIENTATION, value);
					} else
					{
						ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT,
								ExifInterface.ORIENTATION_NORMAL);
						exifDriver.getIfd0().put(ExifDriver.TAG_ORIENTATION, value);
					}

					// Save exif info to new file, and replace old file with new
					// one.
					File modifiedFile = new File(tmpFile.getAbsolutePath() + ".tmp");
					exifDriver.save(modifiedFile.getAbsolutePath());
					if (MainScreen.getForceFilename() == null)
					{
						file.delete();
						modifiedFile.renameTo(file);

						File[] fList = new File(Environment.getExternalStorageDirectory().toString() + "/DCIM/Camera/")
								.listFiles();
						for (File f : fList)
						{
							if (f.getAbsolutePath().contains("tmp_raw_img"))
							{
								String fName = file.getAbsolutePath().replace("jpg", "raw");

								File resRawFile = new File(fName);

								InputStream in = new FileInputStream(f);
								OutputStream out = new FileOutputStream(resRawFile);

								// Transfer bytes from in to out
								byte[] buf = new byte[1024];
								int len;
								while ((len = in.read(buf)) > 0)
								{
									out.write(buf, 0, len);
								}
								in.close();
								out.close();

								f.delete();
								break;
							}
						}
						File rawFile = new File(CapturePlugin.CAMERA_IMAGE_BUCKET_NAME);
					} else
					{
						copyToForceFileName(modifiedFile);
						tmpFile.delete();
						modifiedFile.delete();
					}
				}

				Uri uri = ApplicationScreen.instance.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);			
				broadcastNewPicture(uri);
			}

			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);
		} catch (IOException e)
		{
			e.printStackTrace();
			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION);
			return;
		} catch (Exception e)
		{
			e.printStackTrace();
			ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);
		} finally
		{
			MainScreen.setForceFilename(null);
		}
	}

	private static void broadcastNewPicture(Uri uri) 
	{
		ApplicationScreen.getMainContext().sendBroadcast(new Intent(ACTION_NEW_PICTURE, uri));
		// Keep compatibility
		ApplicationScreen.getMainContext().sendBroadcast(new Intent("com.android.camera.NEW_PICTURE", uri));
	}
	

	@Override
	public void writeData(FileOutputStream os, boolean isYUV, Long SessionID, int i, byte[] buffer, int yuvBuffer,
			File file) throws IOException
	{
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		ContentValues values = null;
		String resultOrientation = getFromSharedMem("frameorientation" + (i + 1) + Long.toString(SessionID));
		Boolean orientationLandscape = false;
		if (resultOrientation == null)
			orientationLandscape = true;
		else
			orientationLandscape = Boolean.parseBoolean(resultOrientation);

		String resultMirrored = getFromSharedMem("framemirrored" + (i + 1) + Long.toString(SessionID));
		Boolean cameraMirrored = false;
		if (resultMirrored != null)
			cameraMirrored = Boolean.parseBoolean(resultMirrored);

		int mDisplayOrientation = Integer.parseInt(resultOrientation);
		if (os != null)
		{
			if (!isYUV)
			{
				os.write(buffer);
			} else
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
				jpegQuality = Integer.parseInt(prefs.getString(MainScreen.sJPEGQualityPref, "95"));

				com.almalence.YuvImage image = new com.almalence.YuvImage(yuvBuffer, ImageFormat.NV21,
						imageSize.getWidth(), imageSize.getHeight(), null);
				// to avoid problems with SKIA
				int cropHeight = image.getHeight() - image.getHeight() % 16;
				image.compressToJpeg(new Rect(0, 0, image.getWidth(), cropHeight), jpegQuality, os);
			}
			os.close();

			ExifInterface ei = new ExifInterface(file.getAbsolutePath());
			int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
			switch (mDisplayOrientation)
			{
			default:
			case 0:
				exif_orientation = ExifInterface.ORIENTATION_NORMAL;
				break;
			case 90:
				if (cameraMirrored)
				{
					mDisplayOrientation = 270;
					exif_orientation = ExifInterface.ORIENTATION_ROTATE_270;
				} else
				{
					exif_orientation = ExifInterface.ORIENTATION_ROTATE_90;
				}
				break;
			case 180:
				exif_orientation = ExifInterface.ORIENTATION_ROTATE_180;
				break;
			case 270:
				if (cameraMirrored)
				{
					mDisplayOrientation = 90;
					exif_orientation = ExifInterface.ORIENTATION_ROTATE_90;
				} else
				{
					exif_orientation = ExifInterface.ORIENTATION_ROTATE_270;
				}
				break;
			}
			ei.setAttribute(ExifInterface.TAG_ORIENTATION, "" + exif_orientation);
			ei.saveAttributes();
		}

		values = new ContentValues();
		values.put(ImageColumns.TITLE, file.getName().substring(0, file.getName().lastIndexOf(".")));
		values.put(ImageColumns.DISPLAY_NAME, file.getName());
		values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
		values.put(ImageColumns.MIME_TYPE, "image/jpeg");
		values.put(ImageColumns.ORIENTATION, mDisplayOrientation);
		values.put(ImageColumns.DATA, file.getAbsolutePath());

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		if (prefs.getBoolean("useGeoTaggingPrefExport", false))
		{
			Location l = MLocation.getLocation(ApplicationScreen.getMainContext());

			if (l != null)
			{
				ExifInterface ei = new ExifInterface(file.getAbsolutePath());
				ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPSTagsConverter.convert(l.getLatitude()));
				ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPSTagsConverter.latitudeRef(l.getLatitude()));
				ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPSTagsConverter.convert(l.getLongitude()));
				ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF, GPSTagsConverter.longitudeRef(l.getLongitude()));

				ei.saveAttributes();

				values.put(ImageColumns.LATITUDE, l.getLatitude());
				values.put(ImageColumns.LONGITUDE, l.getLongitude());
			}
		}

		ApplicationScreen.instance.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
	}
	
	public boolean isPreviewDependentMode()
	{
		String modeID = getActiveModeID();
		if (modeID.equals("hdrmode") || modeID.equals("expobracketing"))
			return true;
		else
			return false;
	}
	
	public boolean isCamera2InterfaceAllowed()
	{
		String modeID = getActiveModeID();
		// Temp fix HDR modes for LG G Flex 2.
		boolean isLgGFlex2 = Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h959")
				|| Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-h510")
				|| Build.MODEL.toLowerCase(Locale.US).replace(" ", "").contains("lg-f510k");

		if (modeID.equals("video")
				|| (Build.MODEL.contains("Nexus 6") && (modeID.equals("pixfix") || modeID.equals("panorama_augmented")))
				|| (isLgGFlex2 && (modeID.equals("hdrmode") || modeID.equals("expobracketing"))))
			return false;
		else
			return true;
	}

	@Override
	public void onAutoFocusMoving(boolean start)
	{
		// TODO Auto-generated method stub
		
	}
}
