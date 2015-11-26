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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.media.ExifInterface;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

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

	private static PluginManager	pluginManager;

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
		processingPluginList = new HashMap<Long, Plugin>();

		sharedMemory = new Hashtable<String, String>();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			createRAWCaptureResultHashtable();

		activeVF = new ArrayList<String>();
		// activeFilter = new ArrayList<String>();

		listVF = new ArrayList<Plugin>();
		listCapture = new ArrayList<Plugin>();
		listProcessing = new ArrayList<Plugin>();
		// listFilter = new ArrayList<Plugin>();
		listExport = new ArrayList<Plugin>();

		// init plugins and add to pluginList
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onPause();
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onGUICreate();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onGUICreate();

		isRestarting = true;

//		if(countdownLayout.getParent() != null)
//			((ViewGroup) countdownLayout.getParent()).removeView(countdownLayout);
//		ApplicationScreen.getGUIManager().removeViews(countdownLayout, R.id.specialPluginsLayout);
//		ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout).invalidate();
//		ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout).requestLayout();

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);

		params.addRule(RelativeLayout.CENTER_IN_PARENT);

		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout)).addView(
				this.countdownLayout, params);

		this.countdownLayout.setLayoutParams(params);
		this.countdownLayout.requestLayout();
		this.countdownLayout.setVisibility(View.INVISIBLE);

//		if(photoTimeLapseLayout.getParent() != null)
//			((ViewGroup) photoTimeLapseLayout.getParent()).removeView(photoTimeLapseLayout);
//		ApplicationScreen.getGUIManager().removeViews(photoTimeLapseLayout, R.id.specialPluginsLayout);

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
					PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
							ApplicationInterface.MSG_CONTROL_UNLOCKED);
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
		} else
		{
			if (!showDelayedCapturePrefCommon || delayInterval == 0 || !pluginList.get(activeCapture).delayedCaptureSupported())
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
				if (!keepScreenOn)
				{
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
			if (CameraController.isUseCamera2())
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
			
			ApplicationScreen.instance.onAdvancePreferenceCreate(pf); //Some vf advance preferences may be related to entire application instead of some special vf plugin
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
			if (CameraController.isUseCamera2())
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
			Plugin panoramaPlugin = pluginList.get(activeCapture);
			panoramaPlugin.onPreferenceCreate(pf);
		} else if ("dro".equals(settings))
		{
			AddModeSettings("single", pf);
		} else if ("burst".equals(settings))
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
			AddModeSettings("preshot", pf);
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
			// for (int i = 0; i < listFilter.size(); i++)
			// {
			// Plugin pg = listFilter.get(i);
			// if (activeFilter.contains(pg.getID()))
			// activePlugins.add(pg);
			// else
			// inactivePlugins.add(pg);
			// }
			// if (activePlugins.size() != listFilter.size() &&
			// isPreferenecesAvailable(inactivePlugins, false))
			// hasInactive = true;
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
			// for (int i = 0; i < listFilter.size(); i++)
			// {
			// Plugin pg = listFilter.get(i);
			// if (!activeFilter.contains(pg.getID()))
			// inactivePlugins.add(pg);
			// }
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
			// for (int i = 0; i < listFilter.size(); i++)
			// {
			// Plugin pg = listFilter.get(i);
			// if (activeFilter.contains(pg.getID()))
			// activePlugins.add(pg);
			// else
			// inactivePlugins.add(pg);
			// }
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
			// for (int i = 0; i < listFilter.size(); i++)
			// {
			// Plugin pg = listFilter.get(i);
			// if (!activeFilter.contains(pg.getID()))
			// inactivePlugins.add(pg);
			// }
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
		boolean bPhotoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		boolean bPhotoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		if (bPhotoTimeLapseActive && bPhotoTimeLapseIsRunning)
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
		long sessionID = 0;
		
		switch (msg.what)
		{
		case ApplicationInterface.MSG_NO_CAMERA:
			break;

		case ApplicationInterface.MSG_CAPTURE_FINISHED:
			shutterRelease = true;

			/*
			 * Debug code for Galaxy S6 in Super mode. Look at Camera2 for more
			 * details
			 */
			// CameraController.onCaptureFinished();

			if (CameraController.getFocusMode() == CameraParameters.AF_MODE_CONTINUOUS_PICTURE)
			{
				CameraController.cancelAutoFocus();
			}

			if (ApplicationScreen.instance.getFlashModePref(ApplicationScreen.sDefaultFlashValue) == CameraParameters.FLASH_MODE_CAPTURE_TORCH)
			{
				// If flashMode == FLASH_MODE_CAPTURE_TORCH, then turn off torch
				// after capturing completed.
				CameraController.setCameraFlashMode(CameraParameters.FLASH_MODE_OFF);
				CameraController.setCameraFlashMode(CameraParameters.FLASH_MODE_CAPTURE_TORCH);
			}

			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onCaptureFinished();

			ApplicationScreen.getGUIManager().onCaptureFinished();
			ApplicationScreen.getGUIManager().startProcessingAnimation();

			// Returns actual flash mode if it was changed during capturing.
			if (!CameraController.isUseCamera2())
			{
				int flashMode = ApplicationScreen.instance.getFlashModePref(ApplicationScreen.sDefaultFlashValue);
				if (flashMode != CameraParameters.FLASH_MODE_CAPTURE_TORCH)
				{
					CameraController.setCameraFlashMode(flashMode);
				}
			}

			int id = ApplicationScreen.getAppResources().getIdentifier(getActiveMode().modeName, "string",
					ApplicationScreen.instance.getPackageName());
			String modeName = ApplicationScreen.getAppResources().getString(id);
			addToSharedMem("mode_name" + (String) msg.obj, modeName);
			// start async task for further processing
			cntProcessing++;

			sessionID = Long.valueOf((String) msg.obj);
			
			// Map sessionID and processing plugin, because active plugin may be
			// changed before image processing will start (Mode was switched).
			// We don't map export plugin, because it's the same for all modes.
			processingPluginList.put(sessionID, pluginList.get(activeProcessing));

			Intent mServiceIntent = new Intent(ApplicationScreen.instance, ProcessingService.class);

//			// Pass to Service sessionID and some other parameters, that may be required.
			mServiceIntent.putExtra("sessionID", sessionID);
			CameraController.Size imageSize = CameraController.getCameraImageSize();
			PluginManager.getInstance().addToSharedMem("imageWidth" + sessionID, String.valueOf(imageSize.getWidth()));
			PluginManager.getInstance().addToSharedMem("imageHeight" + sessionID, String.valueOf(imageSize.getHeight()));
			PluginManager.getInstance().addToSharedMem("wantLandscapePhoto" + sessionID, String.valueOf(ApplicationScreen.getWantLandscapePhoto()));
			PluginManager.getInstance().addToSharedMem("cameraMirrored" + sessionID, String.valueOf(CameraController.isFrontCamera()));
			
			// Start processing service with current sessionID.
			ApplicationScreen.instance.startService(mServiceIntent);
//			new Thread() {
//				@Override
//				public void run()
//				{
//					android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
//					AlmaShotHDR.getAffinity();
//				}
//			}.start();
//			
////			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
//			ProcessingTask task = new ProcessingTask();
//			task.sessionID = sessionID;
//			task.start();

			ApplicationScreen.instance.muteShutter(false);

			// <!-- -+-
			// if mode free
			controlPremiumContent();
			// -+- -->

			if (!PluginManager.getInstance().getActiveModeID().equals("video"))
			{
				ApplicationScreen.getGUIManager().lockControls = false;
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
						ApplicationInterface.MSG_CONTROL_UNLOCKED);
			}

			break;

		case ApplicationInterface.MSG_CAPTURE_FINISHED_NORESULT:
			shutterRelease = true;

			/*
			 * Debug code for Galaxy S6 in Super mode. Look at Camera2 for more
			 * details
			 */
			// CameraController.onCaptureFinished();

			if (CameraController.getFocusMode() == CameraParameters.AF_MODE_CONTINUOUS_PICTURE)
			{
				CameraController.cancelAutoFocus();
			}

			if (ApplicationScreen.instance.getFlashModePref(ApplicationScreen.sDefaultFlashValue) == CameraParameters.FLASH_MODE_CAPTURE_TORCH)
			{
				// If flashMode == FLASH_MODE_CAPTURE_TORCH, then turn off torch
				// after capturing completed.
				CameraController.setCameraFlashMode(CameraParameters.FLASH_MODE_OFF);
			}

			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onCaptureFinished();

			ApplicationScreen.getGUIManager().onCaptureFinished();
			ApplicationScreen.getGUIManager().startProcessingAnimation();

			ApplicationScreen.instance.muteShutter(false);

			ApplicationScreen.getGUIManager().lockControls = false;

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
					ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().onExportFinished();

			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onExportFinished();

			break;

		case ApplicationInterface.MSG_START_POSTPROCESSING:
			if (null != pluginList.get(activeProcessing))
			{
				ApplicationScreen.getGUIManager().lockControls = true;
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
						ApplicationInterface.MSG_CONTROL_LOCKED);

				pluginList.get(activeProcessing).onStartPostProcessing();
				ApplicationScreen.getGUIManager().onPostProcessingStarted();
			}
			break;

		case ApplicationInterface.MSG_POSTPROCESSING_FINISHED:
			sessionID = 0;
			String sSessionID = getFromSharedMem("sessionID");
			if (sSessionID != null)
				sessionID = Long.parseLong(getFromSharedMem("sessionID"));

			// notify GUI about saved images
			ApplicationScreen.getGUIManager().lockControls = false;
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST,
					ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().onPostProcessingFinished();
			if (null != pluginList.get(activeExport) && 0 != sessionID) 
			{
				pluginList.get(activeExport).onExportActive(sessionID);
			}
			else
			{
				ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);
				clearSharedMemory(sessionID);
			}
			break;
		case ApplicationInterface.MSG_EXPORT_FINISHED:
			getPrefs();

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
						&& ApplicationScreen.getForceFilename() == null)
				{
					ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_RETURN_CAPTURED);
				}
			}

			if (photoTimeLapseActive && photoTimeLapseIsRunning)
			{
				AlarmReceiver.getInstance().setNextAlarm(ApplicationScreen.instance.getApplicationContext());
				ApplicationScreen.instance.guiManager.showCaptureIndication();
			}
			break;

		case ApplicationInterface.MSG_EXPORT_FINISHED_IOEXCEPTION:
			getPrefs();

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

			if (photoTimeLapseActive && photoTimeLapseIsRunning)
			{
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
				ApplicationScreen.getAppResources().openRawResourceFd(R.raw.plugin_capture_selftimer_countdown),
				ApplicationScreen.getAppResources().openRawResourceFd(R.raw.plugin_capture_selftimer_finalcountdown));
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

	private boolean		photoTimeLapseActive;
	private boolean		photoTimeLapseIsRunning;

	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);
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

		if (modeID.equals("video")
				|| (CameraController.isNexus6 && (modeID.equals("preshot") || modeID.equals("panorama_augmented")))
				|| ((CameraController.isFlex2 /*|| CameraController.isG4*/) && (modeID.equals("hdrmode") || modeID.equals("expobracketing"))))
			return false;
		else
			return true;
	}

	@Override
	public void onAutoFocusMoving(boolean start)
	{
	}
}
