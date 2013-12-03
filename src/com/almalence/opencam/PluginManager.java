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

package com.almalence.opencam;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.os.AsyncTask;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.almalence.asynctaskmanager.Task;
import com.almalence.opencam.util.exifreader.imaging.jpeg.JpegMetadataReader;
import com.almalence.opencam.util.exifreader.imaging.jpeg.JpegProcessingException;
import com.almalence.opencam.util.exifreader.metadata.Directory;
import com.almalence.opencam.util.exifreader.metadata.Metadata;
import com.almalence.opencam.util.exifreader.metadata.exif.ExifIFD0Directory;
import com.almalence.opencam.util.exifreader.metadata.exif.ExifSubIFDDirectory;
import com.almalence.plugins.capture.burst.BurstCapturePlugin;
import com.almalence.plugins.capture.expobracketing.ExpoBracketingCapturePlugin;
import com.almalence.plugins.capture.groupshot.GroupShotCapturePlugin;
import com.almalence.plugins.capture.night.NightCapturePlugin;
import com.almalence.plugins.capture.objectremoval.ObjectRemovalCapturePlugin;
import com.almalence.plugins.capture.panoramaaugmented.PanoramaAugmentedCapturePlugin;
import com.almalence.plugins.capture.preshot.PreshotCapturePlugin;
import com.almalence.plugins.capture.selftimer.SelfTimerCapturePlugin;
import com.almalence.plugins.capture.sequence.SequenceCapturePlugin;
import com.almalence.plugins.capture.standard.CapturePlugin;
import com.almalence.plugins.capture.video.VideoCapturePlugin;
import com.almalence.plugins.export.standard.ExportPlugin;
import com.almalence.plugins.processing.groupshot.GroupShotProcessingPlugin;
import com.almalence.plugins.processing.hdr.HDRProcessingPlugin;
import com.almalence.plugins.processing.night.NightProcessingPlugin;
import com.almalence.plugins.processing.objectremoval.ObjectRemovalProcessingPlugin;
import com.almalence.plugins.processing.panorama.PanoramaProcessingPlugin;
import com.almalence.plugins.processing.preshot.PreshotProcessingPlugin;
import com.almalence.plugins.processing.sequence.SequenceProcessingPlugin;
import com.almalence.plugins.processing.simple.SimpleProcessingPlugin;
import com.almalence.plugins.vf.aeawlock.AeAwLockVFPlugin;
import com.almalence.plugins.vf.focus.FocusVFPlugin;
import com.almalence.plugins.vf.grid.GridVFPlugin;
import com.almalence.plugins.vf.histogram.HistogramVFPlugin;
import com.almalence.plugins.vf.infoset.InfosetVFPlugin;
import com.almalence.plugins.vf.zoom.ZoomVFPlugin;

/***
 * Plugins managing class.
 * 
 * Controls plugins interaction with mainScreen and processing, controls
 * different stages of activity workflow
 * 
 * may be used by other plugins to retrieve some parameters/settings from other
 * plugins
 ***/

public class PluginManager {
	private static final String PREFERENCE_KEY_DEFAULTS_SELECTED  = "DEFAULTS_SELECTED_";

	private static PluginManager pluginManager;

	// we need some selection of active plugins by type.
	// probably different lists for different plugin's types
	// + probably it's more useful to have map instead of list (Map<Integer,
	// Plugin> pluginList)
	// in map case we'll have all plugins in one map and keys of active plugins
	// of each type (as we have limited amount
	// of types we can have just simple int variables or create a list, but it's
	// more complicated)
	Map<String, Plugin> pluginList;

	// active plugins IDs
	List<String> activeVF;
	String activeCapture;
	String activeProcessing;
	List<String> activeFilter;
	String activeExport;

	// list of plugins by type
	List<Plugin> listVF;
	List<Plugin> listCapture;
	List<Plugin> listProcessing;
	List<Plugin> listFilter;
	List<Plugin> listExport;

	// counter indicating amout of processing tasks running
	private int cntProcessing = 0;

	// session ID used to identify capture session. Created on start and
	// updated each time when capture finished and processing/filter/export
	// sequence started
	private long SessionID = 0;

	// table for sharing plugin's data
	// hashtable for storing shared data - assoc massive for string key and
	// string value
	// file SharedMemory.txt contains data keys and formats for currently used
	// data
	// TODO!!! release data!!!
	// synchronize data??
	private Hashtable<String, String> sharedMemory;

	// message codes
	public static final int MSG_NO_CAMERA = 1;
	public static final int MSG_TAKE_PICTURE = 2;
	public static final int MSG_CAPTURE_FINISHED = 3;
	public static final int MSG_PROCESSING_FINISHED = 4;
	public static final int MSG_START_POSTPROCESSING = 5;
	public static final int MSG_POSTPROCESSING_FINISHED = 6;
	public static final int MSG_FILTER_FINISHED = 7;
	public static final int MSG_EXPORT_FINISHED = 8;
	public static final int MSG_START_FX = 9;
	public static final int MSG_FX_FINISHED = 10;
	public static final int MSG_DELAYED_CAPTURE = 11;	
	public static final int MSG_FORCE_FINISH_CAPTURE = 12;

	public static final int MSG_SET_EXPOSURE = 22;
	public static final int MSG_NEXT_FRAME = 23;
	
	public static final int MSG_BAD_FRAME = 24;

	public static final int MSG_FOCUS_STATE_CHANGED = 28;

	public static final int MSG_START_FULLSIZE_PROCESSING = 29;

	public static final int MSG_RESTART_MAIN_SCREEN = 30;

	public static final int MSG_RETURN_CAPTURED = 222;

	public static final int MSG_RESULT_OK = 40;
	public static final int MSG_RESULT_UNSAVED = 41;

	public static final int MSG_CONTROL_LOCKED = 50;
	public static final int MSG_CONTROL_UNLOCKED = 51;
	public static final int MSG_PROCESSING_BLOCK_UI = 52;
	public static final int MSG_PREVIEW_CHANGED = 53;

	public static final int MSG_EV_CHANGED = 60;
	public static final int MSG_SCENE_CHANGED = 61;
	public static final int MSG_WB_CHANGED = 62;
	public static final int MSG_FOCUS_CHANGED = 63;
	public static final int MSG_FLASH_CHANGED = 64;
	public static final int MSG_ISO_CHANGED = 65;
	
	// OpenGL layer messages
	public static final int MSG_OPENGL_LAYER_SHOW = 70;
	public static final int MSG_OPENGL_LAYER_HIDE = 71;

	//events to pause/resume capture. for example to stop capturing in preshot when popup share opened
	public static final int MSG_STOP_CAPTURE = 80;
	public static final int MSG_START_CAPTURE = 81;
	
	// broadcast will be resent to every active plugin
	public static final int MSG_BROADCAST = 9999;

	// Support flag to avoid plugin's view disappearance issue
	static boolean isRestarting = false;
	
	private static boolean isDefaultsSelected = false;

	public static PluginManager getInstance() {
		if (pluginManager == null) {
			pluginManager = new PluginManager();
		}
		return pluginManager;
	}

	// plugin manager ctor. plugins initialization and filling plugin list
	private PluginManager() {
		pluginList = new Hashtable<String, Plugin>();

		sharedMemory = new Hashtable<String, String>();

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
		HistogramVFPlugin histgramVFPlugin = new HistogramVFPlugin();
		pluginList.put(histgramVFPlugin.getID(), histgramVFPlugin);
		listVF.add(histgramVFPlugin);

		ZoomVFPlugin zoomVFPlugin = new ZoomVFPlugin();
		pluginList.put(zoomVFPlugin.getID(), zoomVFPlugin);
		listVF.add(zoomVFPlugin);

		GridVFPlugin gridVFPlugin = new GridVFPlugin();
		pluginList.put(gridVFPlugin.getID(), gridVFPlugin);
		listVF.add(gridVFPlugin);

		InfosetVFPlugin infosetVFPlugin = new InfosetVFPlugin();
		pluginList.put(infosetVFPlugin.getID(), infosetVFPlugin);
		listVF.add(infosetVFPlugin);

		FocusVFPlugin focusVFPlugin = new FocusVFPlugin();
		pluginList.put(focusVFPlugin.getID(), focusVFPlugin);
		listVF.add(focusVFPlugin);
		
		AeAwLockVFPlugin aeawlockVFPlugin = new AeAwLockVFPlugin();
		pluginList.put(aeawlockVFPlugin.getID(), aeawlockVFPlugin);
		listVF.add(aeawlockVFPlugin);

		// Capture
		CapturePlugin testCapturePlugin = new CapturePlugin();
		pluginList.put(testCapturePlugin.getID(), testCapturePlugin);
		listCapture.add(testCapturePlugin);

		ExpoBracketingCapturePlugin expoBracketingCapturePlugin = new ExpoBracketingCapturePlugin();
		pluginList.put(expoBracketingCapturePlugin.getID(),
				expoBracketingCapturePlugin);
		listCapture.add(expoBracketingCapturePlugin);

		NightCapturePlugin nightCapturePlugin = new NightCapturePlugin();
		pluginList.put(nightCapturePlugin.getID(), nightCapturePlugin);
		listCapture.add(nightCapturePlugin);

		SelfTimerCapturePlugin testTimerCapturePlugin = new SelfTimerCapturePlugin();
		pluginList.put(testTimerCapturePlugin.getID(), testTimerCapturePlugin);
		listCapture.add(testTimerCapturePlugin);

		BurstCapturePlugin burstCapturePlugin = new BurstCapturePlugin();
		pluginList.put(burstCapturePlugin.getID(), burstCapturePlugin);
		listCapture.add(burstCapturePlugin);
		
		ObjectRemovalCapturePlugin objectRemovalCapturePlugin = new ObjectRemovalCapturePlugin();
		pluginList.put(objectRemovalCapturePlugin.getID(), objectRemovalCapturePlugin);
		listCapture.add(objectRemovalCapturePlugin);
		
		SequenceCapturePlugin sequenceCapturePlugin = new SequenceCapturePlugin();
		pluginList.put(sequenceCapturePlugin.getID(), sequenceCapturePlugin);
		listCapture.add(sequenceCapturePlugin);
		
		GroupShotCapturePlugin groupShotCapturePlugin = new GroupShotCapturePlugin();
		pluginList.put(groupShotCapturePlugin.getID(), groupShotCapturePlugin);
		listCapture.add(groupShotCapturePlugin);

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
		pluginList.put(backInTimeProcessingPlugin.getID(),
				backInTimeProcessingPlugin);
		listCapture.add(backInTimeProcessingPlugin);
		
//		HiresPortraitCapturePlugin hiresPortraitCapturePlugin = new HiresPortraitCapturePlugin();
//		pluginList.put(hiresPortraitCapturePlugin.getID(),hiresPortraitCapturePlugin);
//		listCapture.add(hiresPortraitCapturePlugin);

		// Processing
		// or move this to onCreate from processing??!??!!??!
		
		SimpleProcessingPlugin simpleProcessingPlugin = new SimpleProcessingPlugin();
		pluginList.put(simpleProcessingPlugin.getID(), simpleProcessingPlugin);
		listProcessing.add(simpleProcessingPlugin);

		NightProcessingPlugin nightProcessingPlugin = new NightProcessingPlugin();
		pluginList.put(nightProcessingPlugin.getID(), nightProcessingPlugin);
		listProcessing.add(nightProcessingPlugin);

		HDRProcessingPlugin hdrProcessingPlugin = new HDRProcessingPlugin();
		pluginList.put(hdrProcessingPlugin.getID(), hdrProcessingPlugin);
		listProcessing.add(hdrProcessingPlugin);
		
		ObjectRemovalProcessingPlugin movingObjectsProcessingPlugin = new ObjectRemovalProcessingPlugin();
		pluginList.put(movingObjectsProcessingPlugin.getID(), movingObjectsProcessingPlugin);
		listProcessing.add(movingObjectsProcessingPlugin);
		
		SequenceProcessingPlugin sequenceProcessingPlugin = new SequenceProcessingPlugin();
		pluginList.put(sequenceProcessingPlugin.getID(), sequenceProcessingPlugin);
		listProcessing.add(sequenceProcessingPlugin);
		
		GroupShotProcessingPlugin groupShotProcessingPlugin = new GroupShotProcessingPlugin();
		pluginList.put(groupShotProcessingPlugin.getID(), groupShotProcessingPlugin);
		listProcessing.add(groupShotProcessingPlugin);
		
		PanoramaProcessingPlugin panoramaProcessingPlugin = new PanoramaProcessingPlugin();
		pluginList.put(panoramaProcessingPlugin.getID(), panoramaProcessingPlugin);
		listProcessing.add(panoramaProcessingPlugin);
		
//		HiresPortraitProcessingPlugin hiresPortraitProcessingPlugin = new HiresPortraitProcessingPlugin();
//		pluginList.put(hiresPortraitProcessingPlugin.getID(), hiresPortraitProcessingPlugin);
//		listProcessing.add(hiresPortraitProcessingPlugin);
		// Filter

		// Export
		ExportPlugin testExportPlugin = new ExportPlugin();
		pluginList.put(testExportPlugin.getID(), testExportPlugin);
		listExport.add(testExportPlugin);

		// parsing configuration file to setup modes
		ParseConfig();
	}


	public void setupDefaultMode()
	{
		// select default mode - selection from preferences if exists. or from
		// config if first start
		Mode mode = null;

		// checks preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		if (true == prefs.contains("defaultModeName")) 
		{
			String defaultModeName = prefs.getString("defaultModeName", "");
			mode = ConfigParser.getInstance().getMode(defaultModeName);
		} else 
		{
			// set default mode - get this val from mode.xml and later control
			// in preerences
			mode = ConfigParser.getInstance().getDefaultMode();

			Editor prefsEditor = prefs.edit();
			prefsEditor.putString("defaultModeName", mode.modeID);
			prefsEditor.commit();
		}
		
		//when old mode removed for example
		if (mode == null)
		{
			// set default mode - get this val from mode.xml and later control
			// in preerences
			mode = ConfigParser.getInstance().getDefaultMode();

			Editor prefsEditor = prefs.edit();
			prefsEditor.putString("defaultModeName", mode.modeID);
			prefsEditor.commit();
		}

		// set active plugins for default mode
		activeVF.clear();
		for (int i = 0; i < mode.VF.size(); i++)
			activeVF.add(mode.VF.get(i));
		activeCapture = mode.Capture;
		activeProcessing = mode.Processing;
		for (int i = 0; i < mode.Filter.size(); i++)
			activeFilter.add(mode.Filter.get(i));
		activeFilter.clear();
		activeExport = mode.Export;
	}


	public String getActiveModeID() {
		return getActiveMode().modeID;
	}

	public Mode getActiveMode() {
		// select default mode - selection from preferences if exists. or from
		// config if first start
		Mode mode = null;

		// checks preferences
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		if (true == prefs.contains("defaultModeName")) {
			String defaultModeName = prefs.getString("defaultModeName", "");
			mode = ConfigParser.getInstance().getMode(defaultModeName);
		} else {
			// set default mode - get this val from mode.xml and later control
			// in preerences
			mode = ConfigParser.getInstance().getDefaultMode();

			Editor prefsEditor = prefs.edit();
			prefsEditor.putString("defaultModeName", mode.modeID);
			prefsEditor.commit();
		}

		return mode;
	}

	// isFromMain - indicates event originator - mainscreen or processing screen
	public void onCreate()
	{
		isDefaultsSelected = false;
		
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onCreate();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onCreate();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onCreate();
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onCreate();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onCreate();
		
		countdownAnimation = AnimationUtils.loadAnimation(MainScreen.thiz, R.anim.plugin_capture_selftimer_countdown);
		countdownAnimation.setFillAfter(true);
		
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();		
		countdownLayout = (RelativeLayout)inflator.inflate(R.layout.plugin_capture_selftimer_layout, null, false);
		countdownView = (TextView)countdownLayout.findViewById(R.id.countdown_text);
	}

	private void restartMainScreen() {
		// disable old plugins
		MainScreen.guiManager.onStop();
		// onPause(true);
		MainScreen.thiz.PauseMain();
		onStop();

		// create correct workflow for plugins
		onCreate();
		onStart();
		MainScreen.thiz.ResumeMain();

		countdownView.clearAnimation();
        countdownLayout.setVisibility(View.GONE);
        
		// onGUICreate();
		// MainScreen.guiManager.onGUICreate();
	}

	// parse config to get camera and modes configurations
	void ParseConfig() {
		try {
			ConfigParser.getInstance().parse();
		} catch (XmlPullParserException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void switchMode(Mode mode) {
		// disable old plugins
		MainScreen.guiManager.onStop();
		MainScreen.thiz.PauseMain();
		onStop();
		onDestroy();

		// clear lists and fill with new active plugins
		activeVF.clear();
		for (int i = 0; i < mode.VF.size(); i++)
			activeVF.add(mode.VF.get(i));
		activeCapture = mode.Capture;
		activeProcessing = mode.Processing;
		activeFilter.clear();
		for (int i = 0; i < mode.Filter.size(); i++)
			activeFilter.add(mode.Filter.get(i));
		activeExport = mode.Export;

		// set mode as default for future starts
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(MainScreen.mainContext);
		Editor prefsEditor = prefs.edit();
		prefsEditor.putString("defaultModeName", mode.modeID);
		prefsEditor.commit();

		onCreate();
		onStart();
		MainScreen.thiz.ResumeMain();
	}

	public List<Plugin> getActivePlugins(PluginType type) {
		List<Plugin> activePlugins = new ArrayList<Plugin>();
		switch (type) {
		case ViewFinder: {
			for (int i = 0; i < activeVF.size(); i++)
				activePlugins.add(pluginList.get(activeVF.get(i)));
		}
			break;
		case Capture:
			activePlugins.add(pluginList.get(activeCapture));
			break;
		case Processing:
			activePlugins.add(pluginList.get(activeProcessing));
			break;
		case Filter: {
			for (int i = 0; i < activeFilter.size(); i++)
				activePlugins.add(pluginList.get(i));
		}
			break;
		case Export:
			activePlugins.add(pluginList.get(activeExport));
			break;
		default:
			break;
		}

		return activePlugins;
	}

	// base onStart stage
	public void onStart() {
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onStart();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onStart();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onStart();
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onStart();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onStart();
	}

	// base onStop stage;
	public void onStop() {
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onStop();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onStop();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onStop();
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onStop();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onStop();
	}
	
	// base onDestroy stage
	public void onDestroy() {
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onDestroy();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onDestroy();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onDestroy();
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onDestroy();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onDestroy();
	}

	// base onResume stage
	public void onResume() {
		shutterRelease = true;
		Date curDate = new Date();
		SessionID = curDate.getTime();

		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onResume();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onResume();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onResume();
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onResume();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onResume();
	}

	// base onPause stage
	public void onPause(boolean isFromMain) {
		
		//stops delayed interval timer if it's working
		if (delayedCaptureFlashPrefCommon || delayedCaptureSoundPrefCommon)
		{
			releaseSoundPlayers();
	        countdownHandler.removeCallbacks(FlashOff);
		    finalcountdownHandler.removeCallbacks(FlashBlink);
			//stops timer befor exit to be sure it canceled
			if (timer!=null)
			{
				timer.cancel();
				timer=null;
			}
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

	public void menuButtonPressed() {
		onShowPreferences();
		Intent settingsActivity = new Intent(MainScreen.mainContext,
				Preferences.class);
		MainScreen.thiz.startActivity(settingsActivity);
	}

	// Called before preferences activity started
	public void onShowPreferences() {
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onShowPreferences();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onShowPreferences();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onShowPreferences();
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onShowPreferences();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onShowPreferences();
	}

	public void onGUICreate() {
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
		
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout);
		for(int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for(int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int layout_id = this.countdownLayout.getId();
			if(view_id == layout_id)
			{
				if(view.getParent() != null)
					((ViewGroup)view.getParent()).removeView(view);
				
				specialLayout.removeView(view);
			}
		}
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT, LayoutParams.FILL_PARENT);		
		
		params.addRule(RelativeLayout.CENTER_IN_PARENT);		
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).addView(this.countdownLayout, params);
		
		this.countdownLayout.setLayoutParams(params);
		this.countdownLayout.requestLayout();
		this.countdownLayout.setVisibility(View.INVISIBLE);
	}

	public void OnShutterClick() {
		// check if plugin payed
		if (!MainScreen.thiz.checkLaunches(getActiveMode()))
		{
			MainScreen.guiManager.lockControls = false;
			return;
		}
		
		if (shutterRelease == false)
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		delayedCaptureFlashPrefCommon = prefs.getBoolean("delayedCaptureFlashPrefCommon", false);
		delayedCaptureSoundPrefCommon = prefs.getBoolean("delayedCaptureSoundPrefCommon", false);
		int delayInterval = prefs.getInt("delayedCapturePrefCommon", 0);
		boolean showDelayedCapturePrefCommon = prefs.getBoolean("showDelayedCapturePrefCommon", false);
		if (showDelayedCapturePrefCommon == false ||delayInterval==0 || pluginList.get(activeCapture).delayedCaptureSupported()==false)
		{	
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).OnShutterClick();
			if (null != pluginList.get(activeCapture) && MainScreen.thiz.findViewById(R.id.postprocessingLayout).getVisibility() == View.GONE)
				pluginList.get(activeCapture).OnShutterClick();
		}
		else
		{
			shutterRelease = false;
			delayedCapture(delayInterval);	
		}
	}
	
	public void OnFocusButtonClick() {
		// check if plugin payed
		if (!MainScreen.thiz.checkLaunches(getActiveMode()))
		{
			MainScreen.guiManager.lockControls = false;
			return;
		}

		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).OnFocusButtonClick();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).OnFocusButtonClick();
	}

	public boolean onTouch(View view, MotionEvent e) {
		int res = 0;
		for (int i = 0; i < activeVF.size(); i++)
			res += (pluginList.get(activeVF.get(i)).onTouch(view, e) ? 1 : 0);
		if (null != pluginList.get(activeCapture))
			res += (pluginList.get(activeCapture).onTouch(view, e) ? 1 : 0);
		return (res > 0 ? true : false);
	}

	public Plugin getPlugin(String id) {
		return pluginList.get(id);
	}

	// loads preferences to main preferences activity. Big preference screen
	// with all active plugin preferences
	public void loadPreferences() {
	}

	private void AddModeSettings(String modeName, PreferenceFragment pf)
	{
		Mode mode = ConfigParser.getInstance().getMode(modeName);
		for (int j = 0; j < listCapture.size(); j++) {
			Plugin pg = listCapture.get(j);
			if (mode.Capture.equals(pg.getID()))
			{
				addHeadersContent(pf, pg, false);
				//addHeadersContent(pf, pg, true);
			}
		}
		for (int j = 0; j < listProcessing.size(); j++) {
			Plugin pg = listProcessing.get(j);
			if (mode.Processing.equals(pg.getID()))
			{
				addHeadersContent(pf, pg, false);
				//addHeadersContent(pf, pg, true);
			}
		}
	}
	
	public void loadHeaderContent(String settings, PreferenceFragment pf) {
		List<Plugin> activePlugins = new ArrayList<Plugin>();
		List<Plugin> inactivePlugins = new ArrayList<Plugin>();

		boolean hasInactive = false;

		loadStandardSettingsBefore(pf, settings);
		if ("general_settings".equals(settings)) {
		} 
		else if ("vf_settings".equals(settings)) {
			for (int i = 0; i < listVF.size(); i++) {
				Plugin pg = listVF.get(i);
				if (activeVF.contains(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, activePlugins, false);

			if (activePlugins.size() != listVF.size()
					&& isPreferenecesAvailable(inactivePlugins, false))
				pf.addPreferencesFromResource(R.xml.preferences_vf_inactive);
			
			pf.addPreferencesFromResource(R.xml.preferences_vf_common);
		} 
		else if ("vf_inactive_settings".equals(settings)) {
			for (int i = 0; i < listVF.size(); i++) {
				Plugin pg = listVF.get(i);
				if (!activeVF.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, false);
		}
		else if ("save_configuration".equals(settings)) 
		{
			pf.addPreferencesFromResource(R.xml.preferences_general_saveconfiguration);
		}
		else if ("shooting_settings".equals(settings)) 
		{
			pf.addPreferencesFromResource(R.xml.preferences_modes);
		}
		else if ("selftimer".equals(settings))
		{
			AddModeSettings("selftimer", pf);
		}
		else if ("burst".equals(settings))
		{
			AddModeSettings("burstmode", pf);
		}
		else if ("expobracketing".equals(settings))
		{
			AddModeSettings("expobracketing", pf);
		}
		else if ("hdr".equals(settings))
		{
			AddModeSettings("hdrmode", pf);
		}
		else if ("night".equals(settings))
		{
			AddModeSettings("nightmode", pf);
		}
		else if ("video".equals(settings))
		{
			AddModeSettings("video", pf);
		}
		else if ("preshot".equals(settings))
		{
			AddModeSettings("pixfix", pf);
		}
		else if ("objectremoval".equals(settings))
		{
			AddModeSettings("movingobjects", pf);
		}
		else if ("groupshot".equals(settings))
		{
			AddModeSettings("groupshot", pf);
		}
		else if ("sequence".equals(settings))
		{
			AddModeSettings("sequence", pf);
		}
		else if ("panorama_augmented".equals(settings))
		{
			AddModeSettings("panorama_augmented", pf);
		}
//			List<Mode> modes = ConfigParser.getInstance().getList();
//			for (int i = 0; i < modes.size(); i++)
//			{
//				pf.addPreferencesFromResource(R.xml.preferences_modes);
//				Mode mode = modes.get(i);
//				for (int j = 0; j < listCapture.size(); j++) {
//					Plugin pg = listCapture.get(j);
//					if (mode.Capture.equals(pg.getID()))
//						addHeadersContent(pf, pg, false);
//				}
//				
//			}
/*			
			for (int i = 0; i < listCapture.size(); i++) {
				Plugin pg = listCapture.get(i);
				if (activeCapture.equals(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (activePlugins.size() != listCapture.size()
					&& isPreferenecesAvailable(inactivePlugins, false))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, false);

			activePlugins.clear();
			inactivePlugins.clear();
			for (int i = 0; i < listProcessing.size(); i++) {
				Plugin pg = listProcessing.get(i);
				if (activeProcessing.equals(pg.getID()))
					activePlugins.add(pg);
			}
			if (activePlugins.size() != listProcessing.size()
					&& isPreferenecesAvailable(inactivePlugins, false))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, false);

			if (hasInactive)
				pf.addPreferencesFromResource(R.xml.preferences_shooting_inactive);
				*/
//		}
		else if ("saving_settings".equals(settings)) {
			for (int i = 0; i < listFilter.size(); i++) {
				Plugin pg = listFilter.get(i);
				if (activeFilter.contains(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (activePlugins.size() != listFilter.size()
					&& isPreferenecesAvailable(inactivePlugins, false))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, false);

			activePlugins.clear();
			inactivePlugins.clear();
			for (int i = 0; i < listExport.size(); i++) {
				Plugin pg = listExport.get(i);
				if (activeExport.contains(pg.getID()))
					activePlugins.add(pg);
				else
					inactivePlugins.add(pg);
			}
			if (activePlugins.size() != listExport.size()
					&& isPreferenecesAvailable(inactivePlugins, false))
				hasInactive = true;
			addHeadersContent(pf, activePlugins, false);

			if (hasInactive)
				pf.addPreferencesFromResource(R.xml.preferences_saving_inactive);
		} else if ("saving_inactive_settings".equals(settings)) {
			for (int i = 0; i < listFilter.size(); i++) {
				Plugin pg = listFilter.get(i);
				if (!activeFilter.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, false);

			activePlugins.clear();
			for (int i = 0; i < listExport.size(); i++) {
				Plugin pg = listExport.get(i);
				if (!activeExport.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, false);
		} else if ("advanced".equals(settings)) {
			loadCommonAdvancedSettings(pf);

			for (int i = 0; i < listVF.size(); i++) {
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
			for (int i = 0; i < listCapture.size(); i++) {
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
			for (int i = 0; i < listProcessing.size(); i++) {
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
			for (int i = 0; i < listFilter.size(); i++) {
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
			for (int i = 0; i < listExport.size(); i++) {
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
		} else if ("advanced_inactive".equals(settings)) {
			for (int i = 0; i < listVF.size(); i++) {
				Plugin pg = listVF.get(i);
				if (!activeVF.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);

			inactivePlugins.clear();
			for (int i = 0; i < listCapture.size(); i++) {
				Plugin pg = listCapture.get(i);
				if (!activeCapture.equals(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);

			inactivePlugins.clear();
			for (int i = 0; i < listProcessing.size(); i++) {
				Plugin pg = listProcessing.get(i);
				if (!activeProcessing.equals(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);

			inactivePlugins.clear();
			for (int i = 0; i < listFilter.size(); i++) {
				Plugin pg = listFilter.get(i);
				if (!activeFilter.contains(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);

			inactivePlugins.clear();
			for (int i = 0; i < listExport.size(); i++) {
				Plugin pg = listExport.get(i);
				if (!activeExport.equals(pg.getID()))
					inactivePlugins.add(pg);
			}
			addHeadersContent(pf, inactivePlugins, true);
		} else if ("plugins_settings".equals(settings)) {
			pf.addPreferencesFromResource(R.xml.preferences_plugins_upgrade);
			MainScreen.thiz.onBillingPreferenceCreate(pf);
		}

		loadStandardSettingsAfter(pf, settings);
	}

	private void addHeadersContent(PreferenceFragment pf, List<Plugin> list,
			boolean isAdvanced) {
		int size = list.size(), i = 0;
		while (i < size) {
			addHeadersContent(pf, list.get(i), isAdvanced);
			i++;
		}
	}

	private void addHeadersContent(PreferenceFragment pf, Plugin plugin,
			boolean isAdvanced) {
		if (null == plugin)
			return;
		if (!plugin.isShowPreferences)
			return;
		if (!isAdvanced) {
			if (plugin.getPreferenceName() == 0)
				return;
			pf.addPreferencesFromResource(plugin.getPreferenceName());
			plugin.onPreferenceCreate(pf);
		} else {
			if (plugin.getAdvancedPreferenceName() == 0)
				return;
			pf.addPreferencesFromResource(plugin.getAdvancedPreferenceName());
		}

		plugin.showInitialSummary(pf);
	}

	private void loadStandardSettingsBefore(PreferenceFragment pf,
			String settings) {
		if ("general_settings".equals(settings)) {
			pf.addPreferencesFromResource(R.xml.preferences);
			MainScreen.thiz.onPreferenceCreate(pf);
		} else if ("saving_settings".equals(settings)) {
			pf.addPreferencesFromResource(R.xml.preferences_export_common);
		}
	}

	private void loadStandardSettingsAfter(PreferenceFragment pf,
			String settings) {
	}

	private void loadCommonAdvancedSettings(PreferenceFragment pf) {
		pf.addPreferencesFromResource(R.xml.preferences_advanced_common);
	}

	// show preference's value in summary on start
	public void showInitialSummary(PreferenceActivity prefActivity) {
		Plugin tmp;
		for (int i = 0; i < activeVF.size(); i++) {
			tmp = pluginList.get(activeVF.get(i));
			tmp.showInitialSummary(prefActivity);
		}

		tmp = pluginList.get(activeCapture);
		if (null != tmp)
			tmp.showInitialSummary(prefActivity);

		tmp = pluginList.get(activeProcessing);
		if (null != tmp)
			tmp.showInitialSummary(prefActivity);

		for (int i = 0; i < activeFilter.size(); i++) {
			tmp = pluginList.get(activeFilter.get(i));
			tmp.showInitialSummary(prefActivity);
		}

		tmp = pluginList.get(activeExport);
		if (null != tmp)
			tmp.showInitialSummary(prefActivity);
	}

	protected boolean isPreferenecesAvailable(List<Plugin> plugins,
			boolean isAdvanced) {
		boolean isAvailable = false;
		for (int i = 0; i < plugins.size(); i++) {
			Plugin plugin = plugins.get(i);
			if (plugin.isShowPreferences && !isAdvanced
					&& plugin.getPreferenceName() != 0)
				isAvailable = true;
			else if (plugin.isShowPreferences && isAdvanced
					&& plugin.getAdvancedPreferenceName() != 0)
				isAvailable = true;
		}
		return isAvailable;
	}

	public void onOrientationChanged(int orientation) {
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onOrientationChanged(
					orientation);
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onOrientationChanged(
					orientation);
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onOrientationChanged(
					orientation);
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onOrientationChanged(orientation);
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onOrientationChanged(
					orientation);
	}

	// can be situation when 2 plugins will handle this interface and it's not
	// write.
	// probably we should close it.
	public boolean onKeyDown(boolean isFromMain, int keyCode, KeyEvent event) {
		int res = 0;
		for (int i = 0; i < activeVF.size(); i++)
			res += pluginList.get(activeVF.get(i)).onKeyDown(keyCode, event) == true ? 1
					: 0;
		if (null != pluginList.get(activeCapture))
			res += pluginList.get(activeCapture).onKeyDown(keyCode, event) == true ? 1
					: 0;
		if (null != pluginList.get(activeProcessing))
			res += pluginList.get(activeProcessing).onKeyDown(keyCode, event) == true ? 1
					: 0;
		for (int i = 0; i < activeFilter.size(); i++)
			res += pluginList.get(i).onKeyDown(keyCode, event) == true ? 1 : 0;
		if (null != pluginList.get(activeExport))
			res += pluginList.get(activeExport).onKeyDown(keyCode, event) == true ? 1
					: 0;
		return (res > 0 ? true : false);
	}

	/******************************************************************************************************
	 * VF/Capture Interfaces
	 ******************************************************************************************************/

	public void SelectDefaults()
	{
//		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.thiz);
		
		if(!isDefaultsSelected)
			for (final Entry<String, Plugin> entry : this.pluginList.entrySet())
			{
				final Plugin plugin = entry.getValue();
				
//				final String plugin_key = PREFERENCE_KEY_DEFAULTS_SELECTED + plugin.ID;
//				
//				if (!prefs.getBoolean(plugin_key, false))
//				{
//					plugin.onDefaultsSelect();
//				
//					prefs.edit().putBoolean(plugin_key, true).commit();
//				}
				
				plugin.onDefaultsSelect();
			}
		isDefaultsSelected = true;	
	}
	
	public void SelectImageDimension() {
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).SelectImageDimension();
	}

	public void SetCameraPreviewSize(Camera.Parameters cp) {
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).SetCameraPreviewSize(cp);
	}

	public void SetCameraPictureSize() {
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).SetCameraPictureSize();
	}

	public void onAutoFocus(boolean paramBoolean, Camera paramCamera) {
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onAutoFocus(paramBoolean,
					paramCamera);

		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture)
					.onAutoFocus(paramBoolean, paramCamera);
	}

	void takePicture() {
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).takePicture();
	}

	void onShutter() {
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onShutter();
	}

	void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera) {
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onPictureTaken(paramArrayOfByte,
					paramCamera);
	}

	void onPreviewFrame(byte[] data, Camera paramCamera) {
		// prevents plugin's views to disappear
		if (isRestarting) {
			RelativeLayout pluginsLayout = (RelativeLayout) MainScreen.thiz
					.findViewById(R.id.mainLayout1);
			pluginsLayout.requestLayout();
			isRestarting = false;
		}

		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onPreviewFrame(data, paramCamera);

		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onPreviewFrame(data, paramCamera);
	}
	
	public void SetupCameraParameters() {
		MainScreen.thiz.updateCameraFeatures();
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).SetupCameraParameters();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).SetupCameraParameters();
	}

	public void onCameraParametersSetup() {
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onCameraParametersSetup();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onCameraParametersSetup();
	}

	public void onCameraSetup() {
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onCameraSetup();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onCameraSetup();
	}

	/******************************************************************************************************
	 * Processing Interfaces
	 ******************************************************************************************************/
	void startProcessingActivity() {
	}

	void StartProcessing() {
	}

	public void processingOnClick(View v) {
	}

	public void onPreviewComplete(Task task) {
	}

	public Bitmap getMultishotBitmap(int index) {
		return null;
	}

	public Bitmap getScaledMultishotBitmap(int index, int scaled_width,
			int scaled_height) {
		return null;
	}

	public int getResultYUV(int index) {
		if (null != pluginList.get(activeProcessing))
			return pluginList.get(activeProcessing).getResultYUV(index);
		else
			return -1;
	}

	public int getMultishotImageCount() {
		return 0;
	}

	/******************************************************************************************************
	 * Filter Interfaces
	 ******************************************************************************************************/

	public void callFilterPlugin() {
	}

	/******************************************************************************************************
	 * Export Interfaces
	 ******************************************************************************************************/
	// broadcast message handler
	// handler will notify all active plugins where data is located and length
	// of data.
	// First idea - is data array and data length in main. Can be accessed by
	// any interested plugin.
	// message can pass 2 int parameters. can be used as message id or short
	// data pass
	void onBroadcast(int arg1, int arg2) {
		boolean res;
		for (int i = 0; i < activeVF.size(); i++) {
			res = pluginList.get(activeVF.get(i)).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
		if (null != pluginList.get(activeCapture)) {
			res = pluginList.get(activeCapture).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
		if (null != pluginList.get(activeProcessing)) {
			res = pluginList.get(activeProcessing).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
		for (int i = 0; i < activeFilter.size(); i++) {
			res = pluginList.get(i).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
		if (null != pluginList.get(activeExport)) {
			res = pluginList.get(activeExport).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
	}

	/******************************************************************************************************
	 * Message handler
	 ******************************************************************************************************/
	public boolean handleMessage(Message msg) {
		switch (msg.what) {
		case MSG_NO_CAMERA:
			// gracefully exit if camera is not available
			// AlertDialog ad = new AlertDialog.Builder(this)
			// .setIcon(R.drawable.alert_dialog_icon)
			// .setTitle(R.string.no_camera_title)
			// .setMessage(R.string.no_camera_msg)
			// .setPositiveButton(android.R.string.ok, new
			// DialogInterface.OnClickListener()
			// {
			// public void onClick(DialogInterface dialog, int whichButton)
			// {
			// finish();
			// }
			// })
			// .create();
			//
			// ad.show();
			break;

		case MSG_TAKE_PICTURE:
			pluginManager.takePicture();
			break;

		case MSG_CAPTURE_FINISHED:
			shutterRelease = true;
			
			MainScreen.guiManager.lockControls = false;
			Message message = new Message();
			message.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
			message.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(message);
			
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onCaptureFinished();

			MainScreen.guiManager.onCaptureFinished();
			MainScreen.guiManager.startProcessingAnimation();

			// start async task for further processing
			cntProcessing++;
			ProcessingTask task = new ProcessingTask(MainScreen.thiz);
			task.SessionID = SessionID;
			task.processing = pluginList.get(activeProcessing);
			task.export = pluginList.get(activeExport);
			task.execute();
			Date curDate = new Date();
			SessionID = curDate.getTime();

			MainScreen.thiz.MuteShutter(false);
			
			//if mode free
			Mode mode = getActiveMode();
	    	if (mode.SKU != null)
	    		if (!mode.SKU.isEmpty())
	    			MainScreen.thiz.decrementLeftLaunches(mode.modeID);
			break;
			
		case MSG_START_POSTPROCESSING:
			if (null != pluginList.get(activeProcessing))
			{
				MainScreen.guiManager.lockControls = true;
				Message message2 = new Message();
				message2.arg1 = PluginManager.MSG_CONTROL_LOCKED;
				message2.what = PluginManager.MSG_BROADCAST;
				MainScreen.H.sendMessage(message2);
				
				pluginList.get(activeProcessing).onStartPostProcessing();
				MainScreen.guiManager.onPostProcessingStarted();
			}
			break;

		case MSG_PROCESSING_FINISHED:
			pluginManager.callFilterPlugin();
			break;

		case MSG_POSTPROCESSING_FINISHED:
			long sessionID = 0;
			String sSessionID = PluginManager.getInstance().getFromSharedMem("sessionID");
			if(sSessionID != null)
				sessionID = Long.parseLong(PluginManager.getInstance().getFromSharedMem("sessionID"));
			
			// notify GUI about saved images			
			MainScreen.guiManager.lockControls = false;
			Message message3 = new Message();
			message3.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
			message3.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(message3);
			
			MainScreen.guiManager.onPostProcessingFinished();
			if (null != pluginList.get(activeExport) && 0 != sessionID)
				pluginList.get(activeExport).onExportActive(sessionID);
			else
				MainScreen.H.sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);
			
			clearSharedMemory(sessionID);
			break;
		case MSG_EXPORT_FINISHED:
			// event from plugin that saving finished and memory can be freed
			if (cntProcessing > 0)
				cntProcessing--;
			// free memory in processing
			if (null != pluginList.get(activeProcessing))
				pluginList.get(activeProcessing).FreeMemory();

			// notify GUI about saved images
			MainScreen.guiManager.onExportFinished();

			// notify capture plugins that saving finished
			if (null != pluginList.get(activeCapture))
				pluginList.get(activeCapture).onExportFinished();
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onExportFinished();
			
			if (MainScreen.thiz.getIntent().getAction() != null)
	        {
		    	if (MainScreen.thiz.getIntent().getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE)
		    			&& MainScreen.ForceFilename == null)
		    	{
		    		MainScreen.thiz.H.sendEmptyMessage(MSG_RETURN_CAPTURED);
		    	}
	        }
			break;

		case MSG_DELAYED_CAPTURE: 
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).OnShutterClick();
			if (null != pluginList.get(activeCapture) && MainScreen.thiz.findViewById(R.id.postprocessingLayout).getVisibility() == View.GONE)
				pluginList.get(activeCapture).OnShutterClick();
			break;
			
		case MSG_RETURN_CAPTURED:
			MainScreen.thiz.setResult(Activity.RESULT_OK);
			MainScreen.thiz.finish();
			break;

		case MSG_START_FULLSIZE_PROCESSING:
			PluginManager.getInstance().StartProcessing();
			break;

		case MSG_RESTART_MAIN_SCREEN:
			PluginManager.getInstance().restartMainScreen();
			break;

		case MSG_OPENGL_LAYER_SHOW:
			MainScreen.thiz.showOpenGLLayer();
			break;

		case MSG_OPENGL_LAYER_HIDE:
			MainScreen.thiz.hideOpenGLLayer();
			break;
			
		case MSG_PROCESSING_BLOCK_UI:
			MainScreen.guiManager.processingBlockUI();
			break;

		case MSG_BROADCAST:
			pluginManager.onBroadcast(msg.arg1, msg.arg2);
			break;
		}

		return true;
	}

	/******************************************************************************************************
	 * Work with hash table
	 ******************************************************************************************************/
	public boolean addToSharedMem(String key, String value) {
		sharedMemory.put(key, value);

		return true;
	}
	
	public boolean addToSharedMem_ExifTagsFromJPEG(final byte[] paramArrayOfByte) {		
		try
    	{
    		InputStream is = new ByteArrayInputStream(paramArrayOfByte);
			Metadata metadata = JpegMetadataReader.readMetadata(is);
			Directory exifDirectory = metadata.getDirectory(ExifSubIFDDirectory.class);			
			String s1 = exifDirectory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME); //ExifInterface.TAG_EXPOSURE_TIME (String)
			String s2 = exifDirectory.getString(ExifSubIFDDirectory.TAG_FNUMBER); //ExifInterface.TAG_APERTURE (String)
			String s3 = exifDirectory.getString(ExifSubIFDDirectory.TAG_FLASH); //ExifInterface.TAG_FLASH (int)
			String s4 = exifDirectory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH); //ExifInterface.TAG_FOCAL_LENGTH (rational)
			String s5 = exifDirectory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT); //ExifInterface.TAG_ISO (String)			
			String s6 = exifDirectory.getString(ExifSubIFDDirectory.TAG_WHITE_BALANCE_MODE); //ExifInterface.TAG_WHITE_BALANCE (String)
			String s9 = exifDirectory.getString(ExifSubIFDDirectory.TAG_SPECTRAL_SENSITIVITY);
			String s10 = exifDirectory.getString(ExifSubIFDDirectory.TAG_EXIF_VERSION);

			Directory exif2Directory = metadata.getDirectory(ExifIFD0Directory.class);
			String s7 = exif2Directory.getString(ExifIFD0Directory.TAG_MAKE); //ExifInterface.TAG_MAKE (String)
			String s8 = exif2Directory.getString(ExifIFD0Directory.TAG_MODEL); //ExifInterface.TAG_MODEL (String)
			
			if(s1 != null) PluginManager.getInstance().addToSharedMem("exiftag_exposure_time"+String.valueOf(PluginManager.getInstance().getSessionID()), s1);
			if(s2 != null) PluginManager.getInstance().addToSharedMem("exiftag_aperture"+String.valueOf(PluginManager.getInstance().getSessionID()), s2);
			if(s3 != null) PluginManager.getInstance().addToSharedMem("exiftag_flash"+String.valueOf(PluginManager.getInstance().getSessionID()), s3);
			if(s4 != null) PluginManager.getInstance().addToSharedMem("exiftag_focal_lenght"+String.valueOf(PluginManager.getInstance().getSessionID()), s4);
			if(s5 != null) PluginManager.getInstance().addToSharedMem("exiftag_iso"+String.valueOf(PluginManager.getInstance().getSessionID()), s5);
			if(s6 != null) PluginManager.getInstance().addToSharedMem("exiftag_white_balance"+String.valueOf(PluginManager.getInstance().getSessionID()), s6);
			if(s7 != null) PluginManager.getInstance().addToSharedMem("exiftag_make"+String.valueOf(PluginManager.getInstance().getSessionID()), s7);
			if(s8 != null) PluginManager.getInstance().addToSharedMem("exiftag_model"+String.valueOf(PluginManager.getInstance().getSessionID()), s8);
			if(s9 != null) PluginManager.getInstance().addToSharedMem("exiftag_spectral_ensitivity"+String.valueOf(PluginManager.getInstance().getSessionID()), s9);
			if(s10 != null) PluginManager.getInstance().addToSharedMem("exiftag_version"+String.valueOf(PluginManager.getInstance().getSessionID()), s10);
			
		} catch (JpegProcessingException e1)
		{
			e1.printStackTrace();
			return false;
		}
		return true;
	}
	
	public boolean addToSharedMem_ExifTagsFromCamera() {		
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if (params==null)
			return false;
		
		String s1 = null;
		if(params.getSupportedWhiteBalance().size() > 0)
			s1 = params.getWhiteBalance().compareTo(MainScreen.thiz.getResources().getString(R.string.wbAutoSystem)) == 0 ? String.valueOf(0) : String.valueOf(1);
		String s2 = Build.MANUFACTURER;
		String s3 = Build.MODEL;
		
		String s4 = null;
		if(MainScreen.guiManager.mISOSupported)
			s4 = MainScreen.thiz.getISOMode();
		
		if(s1 != null) PluginManager.getInstance().addToSharedMem("exiftag_white_balance"+String.valueOf(PluginManager.getInstance().getSessionID()), s1);
		if(s2 != null) PluginManager.getInstance().addToSharedMem("exiftag_make"+String.valueOf(PluginManager.getInstance().getSessionID()), s2);
		if(s3 != null) PluginManager.getInstance().addToSharedMem("exiftag_model"+String.valueOf(PluginManager.getInstance().getSessionID()), s3);
		if(s4 != null && (s4.compareTo("auto") != 0)) PluginManager.getInstance().addToSharedMem("exiftag_iso"+String.valueOf(PluginManager.getInstance().getSessionID()), s4);
		return true;
	}

	public String getFromSharedMem(String key) {
		return sharedMemory.get(key);
	}

	public boolean containsSharedMem(String key) {
		if (!sharedMemory.containsKey(key))
			return false;
		return true;
	}

	public void clearSharedMemory(long sessionID) {
		String partKey = String.valueOf(sessionID);
		Enumeration<String> e = sharedMemory.keys();
		while (e.hasMoreElements()) {
			String i = (String) e.nextElement();
			if (i.contains(partKey))
				sharedMemory.remove(i);
		}
	}

	public void removeFromSharedMemory(String key) {
		Enumeration<String> e = sharedMemory.keys();
		while (e.hasMoreElements()) {
			String i = (String) e.nextElement();
			if (i.equals(key))
				sharedMemory.remove(i);
		}
	}
	
	/******************************************************************************************************
	 * OpenGL layer functions
	 ******************************************************************************************************/
	public boolean isGLSurfaceNeeded() {
		boolean ret = false;
		if (null != pluginList.get(activeCapture))
		{
			if (pluginList.get(activeCapture).isGLSurfaceNeeded())
				ret = true;
		}
		return ret;
	}
	
	public void onGLSurfaceCreated(GL10 gl, EGLConfig config) {
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onGLSurfaceCreated(gl, config);
	}

	public void onGLSurfaceChanged(GL10 gl, int width, int height) {
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onGLSurfaceChanged(gl, width, height);
	}

	public void onGLDrawFrame(GL10 gl) {
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onGLDrawFrame(gl);
	}

	// ////////////////////////////
	// processing&Filter&Export task
	// ////////////////////////////
	private class ProcessingTask extends AsyncTask<Void, Void, Void> {
		public long SessionID = 0;// id to identify data flow

		Plugin processing=null;
		Plugin export = null;
		
		public ProcessingTask(Context context) {
		}

		@Override
		protected void onPreExecute() {
		}

		@Override
		protected Void doInBackground(Void... params) 
		{
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			//if post rpocessing not needed - save few values 
			//from main screen to shared memory for current session
//			Plugin processing = pluginList.get(activeProcessing);
//			Plugin export = pluginList.get(activeExport);
			if (null != processing)
				if(!processing.isPostProcessingNeeded())
				{
					addToSharedMem("imageHeight"+SessionID, String.valueOf(MainScreen.getImageHeight()));
					addToSharedMem("imageWidth"+SessionID, String.valueOf(MainScreen.getImageWidth()));
//					addToSharedMem("saveImageHeight"+SessionID, String.valueOf(MainScreen.getSaveImageHeight()));
//					addToSharedMem("saveImageWidth"+SessionID, String.valueOf(MainScreen.getSaveImageWidth()));
					addToSharedMem("wantLandscapePhoto"+SessionID, String.valueOf(MainScreen.getWantLandscapePhoto()));
					addToSharedMem("CameraMirrored"+SessionID, String.valueOf(MainScreen.getCameraMirrored()));
				}
			
			
			if (null != processing)
			{
				processing.onStartProcessing(SessionID);
				if(processing.isPostProcessingNeeded())
				{
					MainScreen.H.sendEmptyMessage(PluginManager.MSG_START_POSTPROCESSING);					
					return null;
				}
			}
			
			if (null != export)
				export.onExportActive(SessionID);
			else
				MainScreen.H.sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);

			clearSharedMemory(SessionID);
			return null;
		}

		@Override
		protected void onPostExecute(Void v) {
		}
	}

	// /////////////////////////////////////
	// Utils
	// /////////////////////////////////////

	public int getProcessingCounter() {
		return cntProcessing;
	}

	//return current session id!!!
	public long getSessionID() {
		return SessionID;
	}
	
	//get file saving directory
	public File GetSaveDir()
    {
        File dcimDir, saveDir = null, memcardDir;
        boolean usePhoneMem = true;
        
        if ((Integer.parseInt(MainScreen.SaveToPreference) == 1))
        {
			dcimDir = Environment.getExternalStorageDirectory();
			
			// there are variations in sd-card directory namings
			
			memcardDir = new File("/storage", "sdcard1");		// Jelly Bean fix
            if (memcardDir.exists())
            {
	            saveDir = new File("/storage", "sdcard1/DCIM/Camera");
            	usePhoneMem = false;
            }
            else
            {
            	memcardDir = new File("/mnt", "extSdCard");		// SGSIII
	            if (memcardDir.exists())
	            {
		            saveDir = new File("/mnt", "extSdCard/DCIM/Camera");
	            	usePhoneMem = false;
	            }
	            else
	            {
					memcardDir = new File("/storage", "sdcard0");		// Jelly Bean fix
		            if (memcardDir.exists())
		            {
			            saveDir = new File("/storage", "sdcard0/DCIM/Camera");
		            	usePhoneMem = false;
		            }
		            else
		            {
		    			memcardDir = new File(dcimDir, "external_sd");		// Samsung
		                if (memcardDir.exists())
		                {
		    	            saveDir = new File(dcimDir, "external_sd/DCIM/Camera");
		                	usePhoneMem = false;
		                }
		                else
		                {
							memcardDir = new File(dcimDir, "sdcard-ext");		// HTC 4G (?)
				            if (memcardDir.exists())
				            {
					            saveDir = new File(dcimDir, "sdcard-ext/DCIM/Camera"); 
				            	usePhoneMem = false;
				            }
				            else
				            {
								memcardDir = new File("/mnt", "sdcard-ext");		// Motorola Atrix 4G (?)
					            if (memcardDir.exists())
					            {
						            saveDir = new File("/mnt", "sdcard-ext/DCIM/Camera"); 
					            	usePhoneMem = false;
					            }
					            else
					            {
									memcardDir = new File("/", "sdcard");		// Motorola Droid X (?) - an internal sd card location on normal phones
						            if (memcardDir.exists())
						            {
							            saveDir = new File("/", "sdcard/DCIM/Camera");
						            	usePhoneMem = false;
						            }
					            }
				            }
		                }
		            }
	            }
            }
        }
        else if ((Integer.parseInt(MainScreen.SaveToPreference) == 2))
        {
        	saveDir = new File(MainScreen.SaveToPath);
        	usePhoneMem = false;
        }
        
        
        if (usePhoneMem)		// phone memory (internal sd card)
		{
        	
        	dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            saveDir = new File(dcimDir, "Camera"); // "HDR");
		}
        if (!saveDir.exists())
            saveDir.mkdirs();

        // if creation failed - try to switch to phone mem
        if (!saveDir.exists())
        {
        	dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
            saveDir = new File(dcimDir, "Camera"); // "HDR");
            
            if (!saveDir.exists())
                saveDir.mkdirs();
        }
        return saveDir;
    }
	
	//delayed capture feature
	
	private SoundPlayer countdownPlayer = null;
    private SoundPlayer finalcountdownPlayer = null;

    private CountDownTimer timer=null;
    
    public String flashModeBackUp = "";
	
	final Handler countdownHandler = new Handler();
	final Handler finalcountdownHandler = new Handler();
	
	private RelativeLayout countdownLayout = null;
    private TextView countdownView = null;
    
    private Animation countdownAnimation = null;
    
	private boolean delayedCaptureFlashPrefCommon = false; 
	private boolean delayedCaptureSoundPrefCommon = false;
			
	private boolean shutterRelease = true;
	private void delayedCapture(int delayInterval)
	{
		initializeSoundPlayers(MainScreen.thiz.getResources().openRawResourceFd(R.raw.plugin_capture_selftimer_countdown),
		MainScreen.thiz.getResources().openRawResourceFd(R.raw.plugin_capture_selftimer_finalcountdown));
		countdownHandler.removeCallbacks(FlashOff);	 
		finalcountdownHandler.removeCallbacks(FlashBlink);		
		
		timer = new CountDownTimer(delayInterval*1000+500, 1000) 
		{			 
			 public void onTick(long millisUntilFinished) 
		     {		    	 
		    	 countdownView.setRotation(90 - MainScreen.orientationMain);
		         countdownView.setText(String.valueOf(millisUntilFinished/1000));
		         countdownView.clearAnimation();
		         countdownLayout.setVisibility(View.VISIBLE);
		         countdownView.startAnimation(countdownAnimation);
	    		 
		         if (!delayedCaptureFlashPrefCommon && !delayedCaptureSoundPrefCommon)
		    		 return;
		    	 
	    		 TickEverySecond((millisUntilFinished/1000 <= 1)? true : false);
	    		 
		         Camera camera = MainScreen.thiz.getCamera();
		     	 if (null==camera)
		     		return;
		         
		         if(delayedCaptureFlashPrefCommon)
		         {
			         if(millisUntilFinished > 1000)// || (imagesTaken != 0 && isFirstTick))
			         {
			        	try 
			        	{
			        		 Camera.Parameters p = MainScreen.thiz.getCameraParameters();
				        	 p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
				        	 MainScreen.thiz.setCameraParameters(p);
						} catch (Exception e) {
							e.printStackTrace();
							Log.e("Self-timer", "Torch exception: " + e.getMessage());
						}
			        	countdownHandler.postDelayed(FlashOff, 50);
			         }
		         }
		     }

		     public void onFinish() 
		     {
		    	 countdownView.clearAnimation();
		         countdownLayout.setVisibility(View.GONE);
		         
		    	 countdownHandler.removeCallbacks(FlashOff);	 
		 	     finalcountdownHandler.removeCallbacks(FlashBlink);
		         
		 	    Camera camera = MainScreen.thiz.getCamera();
		    	if (camera != null)		// paranoia
				{
					if(MainScreen.thiz.getSupportedFlashModes() != null)
		    			MainScreen.thiz.setCameraFlashMode(flashModeBackUp);
					
					Message msg = new Message();
					msg.what = PluginManager.MSG_DELAYED_CAPTURE;
					MainScreen.H.sendMessage(msg);
				}
		    	timer=null;
		     }
		  };
		  timer.start();
	}
	
	public void TickEverySecond(boolean isLastSecond)
	{
		if (MainScreen.ShutterPreference)
			return;
		if (delayedCaptureSoundPrefCommon)
		{
			if(isLastSecond)
			{
				if (finalcountdownPlayer != null)
					finalcountdownPlayer.play();
			}
			else
			{
				if (countdownPlayer!=null)
					countdownPlayer.play();
			}
		}
 	}
	
	public void initializeSoundPlayers(AssetFileDescriptor fd_countdown, AssetFileDescriptor fd_finalcountdown) {
		countdownPlayer = new SoundPlayer(MainScreen.mainContext, fd_countdown);
		finalcountdownPlayer = new SoundPlayer(MainScreen.mainContext, fd_finalcountdown);
    }

    public void releaseSoundPlayers() {
        if (countdownPlayer != null) {
        	countdownPlayer.release();
        	countdownPlayer = null;
        }
        
        if (finalcountdownPlayer != null) {
        	finalcountdownPlayer.release();
        	finalcountdownPlayer = null;
        }
    }
    
    private Runnable FlashOff = new Runnable() {
        public void run() {
        	Camera camera = MainScreen.thiz.getCamera();
        	if (null==camera)
        		return;
        	Camera.Parameters p = MainScreen.thiz.getCameraParameters();
       	 	p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
       	 	MainScreen.thiz.setCameraParameters(p); 
        }
    };
    
    private Runnable FlashBlink = new Runnable() {
    	boolean isFlashON = false;
        public void run() {
        	Camera camera = MainScreen.thiz.getCamera();
        	if (null==camera)
        		return;
        	
        	try {
	        	Camera.Parameters p = MainScreen.thiz.getCameraParameters();
	        	if(isFlashON)
	        	{
	       	 		p.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
	       	 		isFlashON = false;
	        	}
	        	else
	        	{
	        		p.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
	       	 		isFlashON = true;
	        	}
	        	MainScreen.thiz.setCameraParameters(p);
        	} catch (Exception e) {
				e.printStackTrace();
				Log.e("Self-timer", "finalcountdownHandler exception: " + e.getMessage());
			}
        	finalcountdownHandler.postDelayed(this, 50);
        }
    };
	
}
