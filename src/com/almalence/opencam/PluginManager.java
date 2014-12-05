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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
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
import com.almalence.plugins.export.standard.ExifDriver.ExifDriver;
import com.almalence.plugins.export.standard.ExifDriver.ExifManager;
import com.almalence.plugins.export.standard.ExifDriver.Values.ValueByteArray;
import com.almalence.plugins.export.standard.ExifDriver.Values.ValueNumber;
import com.almalence.plugins.export.standard.ExifDriver.Values.ValueRationals;
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
import com.almalence.util.exifreader.imaging.jpeg.JpegMetadataReader;
import com.almalence.util.exifreader.imaging.jpeg.JpegProcessingException;
import com.almalence.util.exifreader.metadata.Directory;
import com.almalence.util.exifreader.metadata.Metadata;
import com.almalence.util.exifreader.metadata.exif.ExifIFD0Directory;
import com.almalence.util.exifreader.metadata.exif.ExifSubIFDDirectory;
/* <!-- +++
import com.almalence.opencam_plus.cameracontroller.CameraController;
import com.almalence.opencam_plus.ui.AlmalenceGUI.ShutterButton;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.AlmalenceGUI.ShutterButton;
//-+- -->

/***
 * Plugins managing class.
 * 
 * Controls plugins interaction with mainScreen and processing, controls
 * different stages of activity workflow
 * 
 * may be used by other plugins to retrieve some parameters/settings from other
 * plugins
 ***/

public class PluginManager implements PluginManagerInterface
{
	
	private static PluginManager		pluginManager;

	// we need some selection of active plugins by type.
	// probably different lists for different plugin's types
	// + probably it's more useful to have map instead of list (Map<Integer,
	// Plugin> pluginList)
	// in map case we'll have all plugins in one map and keys of active plugins
	// of each type (as we have limited amount
	// of types we can have just simple int variables or create a list, but it's
	// more complicated)
	Map<String, Plugin>					pluginList;

	// active plugins IDs
	List<String>						activeVF;
	String								activeCapture;
	String								activeProcessing;
	List<String>						activeFilter;
	String								activeExport;

	// list of plugins by type
	List<Plugin>						listVF;
	List<Plugin>						listCapture;
	List<Plugin>						listProcessing;
	List<Plugin>						listFilter;
	List<Plugin>						listExport;

	// counter indicating amout of processing tasks running
	private int							cntProcessing							= 0;

	// table for sharing plugin's data
	// hashtable for storing shared data - assoc massive for string key and
	// string value
	// file SharedMemory.txt contains data keys and formats for currently used
	// data
	private Hashtable<String, String>				sharedMemory;
	private Hashtable<String, CaptureResult>		rawCaptureResults;

	// message codes
	public static final int				MSG_NO_CAMERA							= 1;
	public static final int				MSG_TAKE_PICTURE						= 2;
	public static final int				MSG_CAPTURE_FINISHED					= 3;
	public static final int				MSG_PROCESSING_FINISHED					= 4;
	public static final int				MSG_START_POSTPROCESSING				= 5;
	public static final int				MSG_POSTPROCESSING_FINISHED				= 6;
	public static final int				MSG_FILTER_FINISHED						= 7;
	public static final int				MSG_EXPORT_FINISHED						= 8;
	public static final int				MSG_EXPORT_FINISHED_IOEXCEPTION			= 9;
	public static final int				MSG_START_FX							= 10;
	public static final int				MSG_FX_FINISHED							= 11;
	public static final int				MSG_DELAYED_CAPTURE						= 12;
	public static final int				MSG_FORCE_FINISH_CAPTURE				= 13;
	public static final int				MSG_NOTIFY_LIMIT_REACHED				= 14;
	public static final int				MSG_CAPTURE_FINISHED_NORESULT			= 15;

	public static final int				MSG_CAMERA_CONFIGURED					= 160;
	public static final int				MSG_CAMERA_READY						= 161;
	public static final int				MSG_CAMERA_STOPED						= 162;
	
	public static final int				MSG_APPLICATION_STOP					= 163;

	// For HALv3 code version
	public static final int				MSG_CAMERA_OPENED						= 16;
	public static final int				MSG_SURFACE_READY						= 17;
	public static final int				MSG_SURFACE_CONFIGURED					= 170;
	public static final int				MSG_NOT_LEVEL_FULL						= 18;
	public static final int				MSG_PROCESS								= 19;
	public static final int				MSG_PROCESS_FINISHED					= 20;
	public static final int				MSG_VOLUME_ZOOM							= 21;
	// ^^ For HALv3 code version

	public static final int				MSG_NEXT_FRAME							= 23;

	public static final int				MSG_BAD_FRAME							= 24;
	public static final int				MSG_OUT_OF_MEMORY						= 25;

	public static final int				MSG_FOCUS_STATE_CHANGED					= 28;

	public static final int				MSG_RESTART_MAIN_SCREEN					= 30;

	public static final int				MSG_RETURN_CAPTURED						= 31;

	public static final int				MSG_RESULT_OK							= 40;
	public static final int				MSG_RESULT_UNSAVED						= 41;

	public static final int				MSG_CONTROL_LOCKED						= 50;
	public static final int				MSG_CONTROL_UNLOCKED					= 51;
	public static final int				MSG_PROCESSING_BLOCK_UI					= 52;
	public static final int				MSG_PREVIEW_CHANGED						= 53;

	public static final int				MSG_EV_CHANGED							= 60;
	public static final int				MSG_SCENE_CHANGED						= 61;
	public static final int				MSG_WB_CHANGED							= 62;
	public static final int				MSG_FOCUS_CHANGED						= 63;
	public static final int				MSG_FLASH_CHANGED						= 64;
	public static final int				MSG_ISO_CHANGED							= 65;
	public static final int				MSG_AEWB_CHANGED						= 66;

	// OpenGL layer messages
	public static final int				MSG_OPENGL_LAYER_SHOW					= 70;
	public static final int				MSG_OPENGL_LAYER_HIDE					= 71;
	public static final int				MSG_OPENGL_LAYER_SHOW_V2				= 72;
	public static final int				MSG_OPENGL_LAYER_RENDERMODE_CONTINIOUS	= 73;
	public static final int				MSG_OPENGL_LAYER_RENDERMODE_WHEN_DIRTY	= 74;

	// events to pause/resume capture. for example to stop capturing in preshot
	// when popup share opened
	public static final int				MSG_STOP_CAPTURE						= 80;
	public static final int				MSG_START_CAPTURE						= 81;

	// broadcast will be resent to every active plugin
	public static final int				MSG_BROADCAST							= 9999;

	// Support flag to avoid plugin's view disappearance issue
	static boolean						isRestarting							= false;

	static int							jpegQuality								= 95;

	private static boolean				isDefaultsSelected						= false;

	public static PluginManager getInstance()
	{
		if (pluginManager == null)
		{
			pluginManager = new PluginManager();
		}
		return pluginManager;
	}

	// plugin manager ctor. plugins initialization and filling plugin list
	private PluginManager()
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
	}
	
	@TargetApi(21)
	public void createRAWCaptureResultHashtable()
	{
		rawCaptureResults = new Hashtable<String, CaptureResult>();
	}

	public void setupDefaultMode()
	{
		// select default mode - selection from preferences if exists. or from
		// config if first start
		Mode mode = getMode();

		// when old mode removed for example
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		if (mode == null)
		{
			// set default mode - get this val from mode.xml and later control
			// in preerences
			mode = ConfigParser.getInstance().getDefaultMode();

			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(MainScreen.sDefaultModeName, mode.modeID);
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

	public String getActiveModeID()
	{
		return getActiveMode().modeID;
	}

	public Mode getActiveMode()
	{
		// select default mode - selection from preferences if exists. or from
		// config if first start
		return getMode();
	}

	private Mode getMode()
	{
		Mode mode = null;

		// checks preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		if (prefs.contains(MainScreen.sDefaultModeName))
		{
			String defaultModeName = prefs.getString(MainScreen.sDefaultModeName, "");
			mode = ConfigParser.getInstance().getMode(defaultModeName);
		} else
		{
			// set default mode - get this val from mode.xml and later control
			// in preerences
			mode = ConfigParser.getInstance().getDefaultMode();

			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(MainScreen.sDefaultModeName, mode.modeID);
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

		countdownAnimation = AnimationUtils.loadAnimation(MainScreen.getInstance(),
				R.anim.plugin_capture_selftimer_countdown);
		countdownAnimation.setFillAfter(true);

		LayoutInflater inflator = MainScreen.getInstance().getLayoutInflater();
		countdownLayout = (RelativeLayout) inflator.inflate(R.layout.plugin_capture_selftimer_layout, null, false);
		countdownView = (TextView) countdownLayout.findViewById(R.id.countdown_text);

		photoTimeLapseLayout = (RelativeLayout) inflator.inflate(R.layout.plugin_capture_photo_timelapse_layout, null,
				false);
		photoTimeLapseView = (TextView) photoTimeLapseLayout.findViewById(R.id.photo_timelapse_text);
	}

	// parse config to get camera and modes configurations
	void parseConfig()
	{
		try
		{
			ConfigParser.getInstance().parse(MainScreen.getMainContext());
		} catch (XmlPullParserException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}
	
	protected boolean isRestart = false;
	public void setSwitchModeType(boolean restart)
	{
		isRestart = restart;
	}

	public void switchMode(Mode mode)
	{
		Log.e("PluginManager", "switchMode: " + mode.modeName);
		
//		boolean isHALv3 = CameraController.isUseHALv3();
		// disable old plugins
		MainScreen.getGUIManager().onStop();
		MainScreen.getInstance().switchingMode(isRestart? false: true);
//		MainScreen.getInstance().switchingMode(true);
		MainScreen.getInstance().pauseMain();
//		MainScreen.getInstance().onStop();
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		Editor prefsEditor = prefs.edit();
		prefsEditor.putString(MainScreen.sDefaultModeName, mode.modeID);
		//prefsEditor.commit();
		
//		if(mode.modeID.equals("video"))
//		{
//			prefsEditor.putBoolean(MainScreen.getMainContext().getResources().getString(R.string.Preference_UseHALv3Key), false);
//			CameraController.useHALv3(false);
//		}
		prefsEditor.commit();
		

		onCreate();
//		MainScreen.getInstance().onStart();
		onStart();
		MainScreen.getInstance().switchingMode(isRestart? false: true);
//		MainScreen.getInstance().switchingMode(true);
		MainScreen.getInstance().resumeMain();
	}

	public List<Plugin> getActivePlugins(PluginType type)
	{
		List<Plugin> activePlugins = new ArrayList<Plugin>();
		switch (type)
		{
		case ViewFinder:
			{
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
		case Filter:
			{
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
	public void onStart()
	{
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
	public void onStop()
	{
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
	public void onDestroy()
	{
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
	public void onResume()
	{
		shutterRelease = true;
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

	public void menuButtonPressed()
	{
		onShowPreferences();
		Intent settingsActivity = new Intent(MainScreen.getMainContext(), Preferences.class);
		MainScreen.getInstance().startActivity(settingsActivity);
	}

	// Called before preferences activity started
	public void onShowPreferences()
	{
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

		MainScreen.getGUIManager().removeViews(countdownLayout, R.id.specialPluginsLayout);

		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.FILL_PARENT,
				LayoutParams.FILL_PARENT);

		params.addRule(RelativeLayout.CENTER_IN_PARENT);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).addView(
				this.countdownLayout, params);

		this.countdownLayout.setLayoutParams(params);
		this.countdownLayout.requestLayout();
		this.countdownLayout.setVisibility(View.INVISIBLE);

		MainScreen.getGUIManager().removeViews(photoTimeLapseLayout, R.id.specialPluginsLayout);

		params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);

		((RelativeLayout) MainScreen.getInstance().findViewById(R.id.specialPluginsLayout)).addView(
				this.photoTimeLapseLayout, params);

		this.photoTimeLapseLayout.setLayoutParams(params);
		this.photoTimeLapseLayout.requestLayout();
		this.photoTimeLapseLayout.setVisibility(View.INVISIBLE);
	}

	private boolean	isUserClicked	= true;

	public void onShutterClickNotUser()
	{
		isUserClicked = false;
		onShutterClick();
	}

	public void onShutterClick()
	{
		// <!-- -+-
		// check if plugin payed
		if (null != pluginList.get(activeCapture) && !((PluginCapture) pluginList.get(activeCapture)).getInCapture())
		{
			if (!MainScreen.getInstance().checkLaunches(getActiveMode()))
			{
				MainScreen.getGUIManager().lockControls = false;
				return;
			}
		}
		// -+- -->
		if (!shutterRelease)
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
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
					AlarmReceiver.cancelAlarm(MainScreen.getInstance());

					MainScreen.getInstance().guiManager.stopCaptureIndication();
					
					MainScreen.getGUIManager().lockControls = false;
					PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
				} else
				{
					e.putInt(MainScreen.sPhotoTimeLapseCount, 0);
					e.putBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, true);
					e.commit();
					
					for (int i = 0; i < activeVF.size(); i++)
						pluginList.get(activeVF.get(i)).onShutterClick();
					pluginList.get(activeCapture).onShutterClick();
					MainScreen.getInstance().guiManager.showCaptureIndication();
				}
				
			} else
			{
				MainScreen.getInstance().guiManager.setShutterIcon(ShutterButton.TIMELAPSE_ACTIVE);
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
						&& MainScreen.getInstance().findViewById(R.id.postprocessingLayout).getVisibility() == View.GONE
						&& MainScreen.getInstance().findViewById(R.id.blockingLayout).getVisibility() == View.GONE)
					pluginList.get(activeCapture).onShutterClick();
			} else
			{
				shutterRelease = false;
				delayedCapture(delayInterval);
			}
		}

		isUserClicked = true;
	}

	public void onFocusButtonClick()
	{
		// <!-- -+-
		// check if plugin payed
		if (null != pluginList.get(activeCapture) && !((PluginCapture) pluginList.get(activeCapture)).getInCapture())
		{
			if (!MainScreen.getInstance().checkLaunches(getActiveMode()))
			{
				MainScreen.getGUIManager().lockControls = false;
				return;
			}
		}
		// -+- -->
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onFocusButtonClick();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onFocusButtonClick();
	}

	public boolean onTouch(View view, MotionEvent e)
	{
		int res = 0;
		for (int i = 0; i < activeVF.size(); i++)
			res += (pluginList.get(activeVF.get(i)).onTouch(view, e) ? 1 : 0);
		if (null != pluginList.get(activeCapture))
			res += (pluginList.get(activeCapture).onTouch(view, e) ? 1 : 0);
		return (res > 0 ? true : false);
	}

	public Plugin getPlugin(String id)
	{
		return pluginList.get(id);
	}

	private void AddModeSettings(String modeName, PreferenceFragment pf)
	{
		Mode mode = ConfigParser.getInstance().getMode(modeName);
		for (int j = 0; j < listCapture.size(); j++)
		{
			Plugin pg = listCapture.get(j);
			if (mode.Capture.equals(pg.getID()))
			{
				addHeadersContent(pf, pg, false);
			}
		}
		for (int j = 0; j < listProcessing.size(); j++)
		{
			Plugin pg = listProcessing.get(j);
			if (mode.Processing.equals(pg.getID()))
			{
				addHeadersContent(pf, pg, false);
			}
		}
	}

	public void loadHeaderContent(String settings, PreferenceFragment pf)
	{
		List<Plugin> activePlugins = new ArrayList<Plugin>();
		List<Plugin> inactivePlugins = new ArrayList<Plugin>();

		boolean hasInactive = false;

		loadStandardSettingsBefore(pf, settings);
		if ("general_more".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_general_more);
			MainScreen.getInstance().onAdvancePreferenceCreate(pf);
		} else if ("general_image_size".equals(settings))
		{
			pf.addPreferencesFromResource(R.xml.preferences_general_image_size);
			if (CameraController.isUseHALv3())
			{
				Preference pref;
				if (null != (pref = pf.findPreference(MainScreen.sImageSizeMultishotBackPref))
						|| null != (pref = pf.findPreference(MainScreen.sImageSizeMultishotFrontPref)))
				{
					pref.setTitle(MainScreen.getAppResources().getString(
							R.string.Pref_Comon_SmartMultishot_And_Super_ImageSize_Title));
				}
			}
			MainScreen.getInstance().onPreferenceCreate(pf);
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
			// <!-- -+-
			pf.getActivity().finish();
			Preferences.closePrefs();
			MainScreen.getInstance().setShowStore(true);
			// -+- -->
		}
	}

	private void addHeadersContent(PreferenceFragment pf, List<Plugin> list, boolean isAdvanced)
	{
		int size = list.size(), i = 0;
		while (i < size)
		{
			addHeadersContent(pf, list.get(i), isAdvanced);
			i++;
		}
	}

	private void addHeadersContent(PreferenceFragment pf, Plugin plugin, boolean isAdvanced)
	{
		if (null == plugin)
			return;
		if (!plugin.isShowPreferences)
			return;
		if (!isAdvanced)
		{
			if (plugin.getPreferenceName() == 0)
				return;
			pf.addPreferencesFromResource(plugin.getPreferenceName());
			plugin.onPreferenceCreate(pf);
		} else
		{
			if (plugin.getAdvancedPreferenceName() == 0)
				return;
			pf.addPreferencesFromResource(plugin.getAdvancedPreferenceName());
		}

		plugin.showInitialSummary(pf);
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

	protected boolean isPreferenecesAvailable(List<Plugin> plugins, boolean isAdvanced)
	{
		boolean isAvailable = false;
		for (int i = 0; i < plugins.size(); i++)
		{
			Plugin plugin = plugins.get(i);
			if (plugin.isShowPreferences && !isAdvanced && plugin.getPreferenceName() != 0)
				isAvailable = true;
			else if (plugin.isShowPreferences && isAdvanced && plugin.getAdvancedPreferenceName() != 0)
				isAvailable = true;
		}
		return isAvailable;
	}

	public void onOrientationChanged(int orientation)
	{
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onOrientationChanged(orientation);
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onOrientationChanged(orientation);
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onOrientationChanged(orientation);
		for (int i = 0; i < activeFilter.size(); i++)
			pluginList.get(i).onOrientationChanged(orientation);
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onOrientationChanged(orientation);
	}

	// can be situation when 2 plugins will handle this interface and it's not
	// write.
	// probably we should close it.
	public boolean onKeyDown(boolean isFromMain, int keyCode, KeyEvent event)
	{
		int res = 0;
		for (int i = 0; i < activeVF.size(); i++)
			res += pluginList.get(activeVF.get(i)).onKeyDown(keyCode, event) ? 1 : 0;
		if (null != pluginList.get(activeCapture))
			res += pluginList.get(activeCapture).onKeyDown(keyCode, event) ? 1 : 0;
		if (null != pluginList.get(activeProcessing))
			res += pluginList.get(activeProcessing).onKeyDown(keyCode, event) ? 1 : 0;
		for (int i = 0; i < activeFilter.size(); i++)
			res += pluginList.get(i).onKeyDown(keyCode, event) ? 1 : 0;
		if (null != pluginList.get(activeExport))
			res += pluginList.get(activeExport).onKeyDown(keyCode, event) ? 1 : 0;
		return (res > 0 ? true : false);
	}

	/******************************************************************************************************
	 * VF/Capture Interfaces
	 ******************************************************************************************************/

	@Override
	public void selectDefaults()
	{
		if (!isDefaultsSelected)
			for (final Entry<String, Plugin> entry : this.pluginList.entrySet())
			{
				final Plugin plugin = entry.getValue();
				plugin.onDefaultsSelect();
			}
		isDefaultsSelected = true;
	}

	@Override
	public void selectImageDimension()
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).selectImageDimension();
	}

	public void setCameraPreviewSize()
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).setCameraPreviewSize();
	}

	@Override
	public void onAutoFocus(boolean paramBoolean)
	{
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onAutoFocus(paramBoolean);

		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onAutoFocus(paramBoolean);
	}

	void takePicture()
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).takePicture();
	}

	void onShutter()
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onShutter();
	}

	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format)
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onImageTaken(frame, frameData, frame_len, format);
	}

	@TargetApi(21)
	public void onCaptureCompleted(CaptureResult result)
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onCaptureCompleted(result);
	}
	
	@Override
	public void addToSharedMemExifTags(byte[] frameData)
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).addToSharedMemExifTags(frameData);
	}

	@Override
	public void onPreviewFrame(byte[] data)
	{
		// prevents plugin's views to disappear
		if (isRestarting)
		{
			RelativeLayout pluginsLayout = (RelativeLayout) MainScreen.getInstance().findViewById(R.id.mainLayout1);
			pluginsLayout.requestLayout();
			isRestarting = false;
		}

		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onPreviewFrame(data);

		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onPreviewFrame(data);
	}

	public void onFrameAvailable()
	{
		final Plugin plugin = pluginList.get(activeCapture);

		if (plugin != null && plugin instanceof PluginCapture)
		{
			((PluginCapture) plugin).onFrameAvailable();
		}
	}

	public void setupCameraParameters()
	{
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).setupCameraParameters();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).setupCameraParameters();
	}

	public void onCameraParametersSetup()
	{
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onCameraParametersSetup();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onCameraParametersSetup();
	}

	public void onCameraSetup()
	{
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onCameraSetup();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onCameraSetup();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		boolean photoTimeLapseActive = prefs.getBoolean(MainScreen.sPhotoTimeLapseActivePref, false);
		boolean photoTimeLapseIsRunning = prefs.getBoolean(MainScreen.sPhotoTimeLapseIsRunningPref, false);

		if (photoTimeLapseActive && photoTimeLapseIsRunning)
		{
			AlarmReceiver.getInstance().takePicture();
		}

	}

	/******************************************************************************************************
	 * Processing Interfaces
	 ******************************************************************************************************/

	public int getResultYUV(int index)
	{
		if (null != pluginList.get(activeProcessing))
			return pluginList.get(activeProcessing).getResultYUV(index);
		else
			return -1;
	}

	/******************************************************************************************************
	 * Filter Interfaces
	 ******************************************************************************************************/

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
	void onBroadcast(int arg1, int arg2)
	{
		boolean res;
		for (int i = 0; i < activeVF.size(); i++)
		{
			res = pluginList.get(activeVF.get(i)).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
		if (null != pluginList.get(activeCapture))
		{
			res = pluginList.get(activeCapture).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
		if (null != pluginList.get(activeProcessing))
		{
			res = pluginList.get(activeProcessing).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
		for (int i = 0; i < activeFilter.size(); i++)
		{
			res = pluginList.get(i).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
		if (null != pluginList.get(activeExport))
		{
			res = pluginList.get(activeExport).onBroadcast(arg1, arg2);
			if (res)
				return;
		}
	}

	/******************************************************************************************************
	 * Message handler
	 ******************************************************************************************************/
	public boolean handleMessage(Message msg)
	{
		switch (msg.what)
		{
		case MSG_NO_CAMERA:
			break;

		case MSG_TAKE_PICTURE:
			pluginManager.takePicture();
			break;

		case MSG_CAPTURE_FINISHED:
			shutterRelease = true;

			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onCaptureFinished();

			MainScreen.getGUIManager().onCaptureFinished();
			MainScreen.getGUIManager().startProcessingAnimation();

			int id = MainScreen.getAppResources().getIdentifier(getActiveMode().modeName, "string",
					MainScreen.getInstance().getPackageName());
			String modeName = MainScreen.getAppResources().getString(id);
			addToSharedMem("mode_name" + (String) msg.obj, modeName);
			// start async task for further processing
			cntProcessing++;
			ProcessingTask task = new ProcessingTask(MainScreen.getInstance());
			task.SessionID = Long.valueOf((String) msg.obj);
			task.processing = pluginList.get(activeProcessing);
			task.export = pluginList.get(activeExport);
			task.execute();
			MainScreen.getInstance().muteShutter(false);

			// <!-- -+-
			// if mode free
			controlPremiumContent();
			// -+- -->

			if (!PluginManager.getInstance().getActiveModeID().equals("video"))
			{
				MainScreen.getGUIManager().lockControls = false;
				PluginManager.getInstance()
						.sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
			}
			
			break;

		case MSG_CAPTURE_FINISHED_NORESULT:
			shutterRelease = true;

			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onCaptureFinished();

			MainScreen.getGUIManager().onCaptureFinished();
			MainScreen.getGUIManager().startProcessingAnimation();

			MainScreen.getInstance().muteShutter(false);

			MainScreen.getGUIManager().lockControls = false;

			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);

			MainScreen.getGUIManager().onExportFinished();

			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onExportFinished();

			break;

		case MSG_START_POSTPROCESSING:
			if (null != pluginList.get(activeProcessing))
			{
				MainScreen.getGUIManager().lockControls = true;
				PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_LOCKED);

				pluginList.get(activeProcessing).onStartPostProcessing();
				MainScreen.getGUIManager().onPostProcessingStarted();
			}
			break;

		case MSG_POSTPROCESSING_FINISHED:
			long sessionID = 0;
			String sSessionID = getFromSharedMem("sessionID");
			if (sSessionID != null)
				sessionID = Long.parseLong(getFromSharedMem("sessionID"));

			// notify GUI about saved images
			MainScreen.getGUIManager().lockControls = false;
			PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);

			MainScreen.getGUIManager().onPostProcessingFinished();
			if (null != pluginList.get(activeExport) && 0 != sessionID)
				pluginList.get(activeExport).onExportActive(sessionID);
			else
				MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);

			clearSharedMemory(sessionID);
			break;
		case MSG_EXPORT_FINISHED:
			// event from plugin that saving finished and memory can be freed
			if (cntProcessing > 0)
				cntProcessing--;
			// free memory in processing
			if (null != pluginList.get(activeProcessing))
				pluginList.get(activeProcessing).freeMemory();

			// notify GUI about saved images
			MainScreen.getGUIManager().onExportFinished();

			// notify capture plugins that saving finished
			if (null != pluginList.get(activeCapture))
				pluginList.get(activeCapture).onExportFinished();
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onExportFinished();

			if (MainScreen.getInstance().getIntent().getAction() != null)
			{
				if (MainScreen.getInstance().getIntent().getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE)
						&& MainScreen.getForceFilename() == null)
				{
					MainScreen.getMessageHandler().sendEmptyMessage(MSG_RETURN_CAPTURED);
				}
			}
			
			if (photoTimeLapseActive && photoTimeLapseIsRunning) {
				AlarmReceiver.getInstance().setNextAlarm(MainScreen.getInstance().getApplicationContext());
				MainScreen.getInstance().guiManager.showCaptureIndication();
			}
			break;

		case MSG_EXPORT_FINISHED_IOEXCEPTION:
			// event from plugin that saving finished and memory can be freed
			if (cntProcessing > 0)
				cntProcessing--;
			// free memory in processing
			if (null != pluginList.get(activeProcessing))
				pluginList.get(activeProcessing).freeMemory();

			// notify GUI about saved images
			MainScreen.getGUIManager().onExportFinished();

			// notify capture plugins that saving finished
			if (null != pluginList.get(activeCapture))
				pluginList.get(activeCapture).onExportFinished();
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onExportFinished();

			Toast.makeText(MainScreen.getMainContext(), "Can't save data - seems no free space left.",
					Toast.LENGTH_LONG).show();
			
			if (photoTimeLapseActive && photoTimeLapseIsRunning) {
				AlarmReceiver.getInstance().setNextAlarm(MainScreen.getInstance().getApplicationContext());
				MainScreen.getInstance().guiManager.showCaptureIndication();
			}
			break;

		case MSG_DELAYED_CAPTURE:
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onShutterClick();
			if (null != pluginList.get(activeCapture)
					&& MainScreen.getInstance().findViewById(R.id.postprocessingLayout).getVisibility() == View.GONE)
				pluginList.get(activeCapture).onShutterClick();
			break;

		case MSG_RETURN_CAPTURED:
			MainScreen.getInstance().setResult(Activity.RESULT_OK);
			MainScreen.getInstance().finish();
			break;

		case MSG_OPENGL_LAYER_SHOW:
			MainScreen.getInstance().showOpenGLLayer(1);
			break;

		case MSG_OPENGL_LAYER_SHOW_V2:
			MainScreen.getInstance().showOpenGLLayer(2);
			break;

		case MSG_OPENGL_LAYER_HIDE:
			MainScreen.getInstance().hideOpenGLLayer();
			break;

		case MSG_OPENGL_LAYER_RENDERMODE_CONTINIOUS:
			MainScreen.getInstance().glSetRenderingMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
			break;

		case MSG_OPENGL_LAYER_RENDERMODE_WHEN_DIRTY:
			MainScreen.getInstance().glSetRenderingMode(GLSurfaceView.RENDERMODE_WHEN_DIRTY);
			break;

		case MSG_PROCESSING_BLOCK_UI:
			MainScreen.getGUIManager().processingBlockUI();
			break;

		case MSG_BROADCAST:
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
			if (!mode.SKU.isEmpty())
				MainScreen.getInstance().decrementLeftLaunches(mode.modeID);
	}

	// -+- -->

	/******************************************************************************************************
	 * Work with hash table
	 ******************************************************************************************************/
	public boolean addToSharedMem(String key, String value)
	{
		sharedMemory.put(key, value);

		return true;
	}
	
	@TargetApi(21)
	public boolean addRAWCaptureResultToSharedMem(String key, CaptureResult value)
	{
		rawCaptureResults.put(key, value);

		return true;
	}

	public boolean addToSharedMemExifTagsFromJPEG(final byte[] paramArrayOfByte, final long SessionID, final int num)
	{
		try
		{
			InputStream is = new ByteArrayInputStream(paramArrayOfByte);
			Metadata metadata = JpegMetadataReader.readMetadata(is);
			Directory exifDirectory = metadata.getDirectory(ExifSubIFDDirectory.class);
			String s1 = exifDirectory.getString(ExifSubIFDDirectory.TAG_EXPOSURE_TIME);
			String s2 = exifDirectory.getString(ExifSubIFDDirectory.TAG_FNUMBER);
			String s3 = exifDirectory.getString(ExifSubIFDDirectory.TAG_FLASH);
			String s4 = exifDirectory.getString(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
			String s5 = exifDirectory.getString(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
			String s6 = exifDirectory.getString(ExifSubIFDDirectory.TAG_WHITE_BALANCE_MODE);
			String s9 = exifDirectory.getString(ExifSubIFDDirectory.TAG_SPECTRAL_SENSITIVITY);
			String s10 = exifDirectory.getString(ExifSubIFDDirectory.TAG_EXIF_VERSION);
			String s11 = exifDirectory.getString(ExifSubIFDDirectory.TAG_SCENE_CAPTURE_TYPE);
			String s12 = exifDirectory.getString(ExifSubIFDDirectory.TAG_METERING_MODE);

			Directory exif2Directory = metadata.getDirectory(ExifIFD0Directory.class);
			String s7 = exif2Directory.getString(ExifIFD0Directory.TAG_MAKE);
			String s8 = exif2Directory.getString(ExifIFD0Directory.TAG_MODEL);

			if (num != -1 && s1 != null)
				addToSharedMem("exiftag_exposure_time" + num + SessionID, s1);
			else if (s1 != null)
				addToSharedMem("exiftag_exposure_time" + SessionID, s1);
			if (s2 != null)
				addToSharedMem("exiftag_aperture" + SessionID, s2);
			if (s3 != null)
				addToSharedMem("exiftag_flash" + SessionID, s3);
			if (s4 != null)
				addToSharedMem("exiftag_focal_lenght" + SessionID, s4);
			if (s5 != null)
				addToSharedMem("exiftag_iso" + SessionID, s5);
			if (s6 != null)
				addToSharedMem("exiftag_white_balance" + SessionID, s6);
			if (s7 != null)
				addToSharedMem("exiftag_make" + SessionID, s7);
			if (s8 != null)
				addToSharedMem("exiftag_model" + SessionID, s8);
			if (s9 != null)
				addToSharedMem("exiftag_spectral_sensitivity" + SessionID, s9);
			if (s10 != null)
				addToSharedMem("exiftag_version" + SessionID, s10);
			if (s11 != null)
				addToSharedMem("exiftag_scene_capture_type" + SessionID, s11);
			if (s12 != null)
				addToSharedMem("exiftag_metering_mode" + SessionID, s12);

		} catch (JpegProcessingException e1)
		{
			e1.printStackTrace();
			return false;
		}
		return true;
	}

	@TargetApi(21)
	public boolean addToSharedMemExifTagsFromCaptureResult(final CaptureResult result, final long SessionID, final int num)
	{
		String exposure_time = String.valueOf(result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
		String sensitivity = String.valueOf(result.get(CaptureResult.SENSOR_SENSITIVITY));
		String aperture = String.valueOf(result.get(CaptureResult.LENS_APERTURE));
		String focal_lenght = String.valueOf(result.get(CaptureResult.LENS_FOCAL_LENGTH));
		String flash_mode = String.valueOf(result.get(CaptureResult.FLASH_MODE));
		String awb_mode = String.valueOf(result.get(CaptureResult.CONTROL_AWB_MODE));

		if (num != -1 && exposure_time != null && !exposure_time.equals("null"))
			addToSharedMem("exiftag_exposure_time" + num + SessionID, exposure_time);
		else if(exposure_time != null && !exposure_time.equals("null"))
			addToSharedMem("exiftag_exposure_time" + SessionID, exposure_time);
		if (sensitivity != null && !sensitivity.equals("null"))
			addToSharedMem("exiftag_spectral_sensitivity" + SessionID, sensitivity);
		if (aperture != null && !aperture.equals("null"))
			addToSharedMem("exiftag_aperture" + SessionID, aperture);
		if (focal_lenght != null && !focal_lenght.equals("null"))
			addToSharedMem("exiftag_focal_lenght" + SessionID, focal_lenght);
		if (flash_mode != null && !flash_mode.equals("null"))
			addToSharedMem("exiftag_flash" + SessionID, flash_mode);
		if (awb_mode != null && !awb_mode.equals("null"))
			addToSharedMem("exiftag_white_balance" + SessionID, awb_mode);

		return true;
	}

	public boolean addToSharedMemExifTagsFromCamera(final long SessionID)
	{
		Camera.Parameters params = CameraController.getCameraParameters();
		if (params == null)
			return false;

		String s1 = null;
		if (params.getSupportedWhiteBalance() != null)
			s1 = params.getWhiteBalance().compareTo(MainScreen.getAppResources().getString(R.string.wbAutoSystem)) == 0 ? String
					.valueOf(0) : String.valueOf(1);
		String s2 = Build.MANUFACTURER;
		String s3 = Build.MODEL;

		String s4 = null;
		if (MainScreen.getGUIManager().mISOSupported)
			s4 = String.valueOf(CameraController.getISOMode());

		if (s1 != null)
			addToSharedMem("exiftag_white_balance" + SessionID, s1);
		if (s2 != null)
			addToSharedMem("exiftag_make" + SessionID, s2);
		if (s3 != null)
			addToSharedMem("exiftag_model" + SessionID, s3);
		if (s4 != null && (s4.compareTo("auto") != 0))
			addToSharedMem("exiftag_iso" + SessionID, s4);
		return true;
	}

	public String getFromSharedMem(String key)
	{
		return sharedMemory.get(key);
	}

	public boolean containsSharedMem(String key)
	{
		if (!sharedMemory.containsKey(key))
			return false;
		return true;
	}
	
	@TargetApi(21)
	public CaptureResult getFromRAWCaptureResults(String key)
	{
		return rawCaptureResults.get(key);
	}

	@TargetApi(21)
	public boolean containsRAWCaptureResults(String key)
	{
		if (rawCaptureResults.containsKey(key))
			return true;
		else
			return false;
	}

	public void clearSharedMemory(long sessionID)
	{
		String partKey = String.valueOf(sessionID);
		Enumeration<String> e = sharedMemory.keys();
		while (e.hasMoreElements())
		{
			String i = (String) e.nextElement();
			if (i.contains(partKey))
				sharedMemory.remove(i);
		}
		
		if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
			clearRAWCaptureResults(sessionID);
	}
	
	@TargetApi(21)
	public void clearRAWCaptureResults(long sessionID)
	{
		String partKey = String.valueOf(sessionID);
		Enumeration<String> e2 = rawCaptureResults.keys();
		while (e2.hasMoreElements())
		{
			String i = (String) e2.nextElement();
			if (i.contains(partKey))
				rawCaptureResults.remove(i);
		}
	}

	public int sizeOfSharedMemory()
	{
		return sharedMemory.size();
	}

	public void removeFromSharedMemory(String key)
	{
		Enumeration<String> e = sharedMemory.keys();
		while (e.hasMoreElements())
		{
			String i = (String) e.nextElement();
			if (i.equals(key))
				sharedMemory.remove(i);
		}
	}

	public boolean muteSounds()
	{
		final Plugin plugin = pluginList.get(activeCapture);
		if (plugin != null && plugin instanceof PluginCapture)
		{
			return ((PluginCapture) plugin).muteSound();
		} else
		{
			return false;
		}
	}

	/******************************************************************************************************
	 * OpenGL layer functions
	 ******************************************************************************************************/
	@Override
	public boolean shouldPreviewToGPU()
	{
		final Plugin plugin = pluginList.get(activeCapture);

		if (plugin != null && (plugin instanceof PluginCapture))
		{
			return ((PluginCapture) plugin).shouldPreviewToGPU();
		} else
		{
			return false;
		}
	}

	public boolean isGLSurfaceNeeded()
	{
		boolean ret = false;
		if (null != pluginList.get(activeCapture))
		{
			if (pluginList.get(activeCapture).isGLSurfaceNeeded())
				ret = true;
		}
		return ret;
	}

	public void onGLSurfaceCreated(GL10 gl, EGLConfig config)
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onGLSurfaceCreated(gl, config);
	}

	public void onGLSurfaceChanged(GL10 gl, int width, int height)
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onGLSurfaceChanged(gl, width, height);
	}

	public void onGLDrawFrame(GL10 gl)
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onGLDrawFrame(gl);
	}

	// ////////////////////////////
	// processing&Filter&Export task
	// ////////////////////////////
	private class ProcessingTask extends AsyncTask<Void, Void, Void>
	{
		public long	SessionID	= 0;	// id to identify data flow

		Plugin		processing	= null;
		Plugin		export		= null;

		public ProcessingTask(Context context)
		{
		}

		@Override
		protected Void doInBackground(Void... params)
		{
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			// if post rpocessing not needed - save few values
			// from main screen to shared memory for current session
			if (null != processing)
				if (!processing.isPostProcessingNeeded())
				{
					CameraController.Size imageSize = CameraController.getCameraImageSize();
					addToSharedMem("imageHeight" + SessionID, String.valueOf(imageSize.getHeight()));
					addToSharedMem("imageWidth" + SessionID, String.valueOf(imageSize.getWidth()));
					addToSharedMem("wantLandscapePhoto" + SessionID, String.valueOf(MainScreen.getWantLandscapePhoto()));
					addToSharedMem("CameraMirrored" + SessionID, String.valueOf(CameraController.isFrontCamera()));
				}

			if (null != processing)
			{
				processing.onStartProcessing(SessionID);
				if (processing.isPostProcessingNeeded())
				{
					MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_START_POSTPROCESSING);
					return null;
				}
			}

			if (null != export)
				export.onExportActive(SessionID);
			else
				MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);

			clearSharedMemory(SessionID);
			return null;
		}

	}

	// /////////////////////////////////////
	// Utils
	// /////////////////////////////////////

	public int getProcessingCounter()
	{
		return cntProcessing;
	}

	private static final String[]	MEMCARD_DIR_PATH		= new String[] { "/storage", "/mnt", "", "/storage",
			"/Removable", "/storage", "/storage", "", "/mnt", "/" };

	private static final String[]	MEMCARD_DIR_PATH_NAMES	= new String[] { "sdcard1", "extSdCard", "external_sd",
			"external_SD", "MicroSD", "emulated", "sdcard0", "sdcard-ext", "sdcard-ext", "sdcard" };

	private static final String[]	SAVEDIR_DIR_PATH_NAMES	= new String[] { "sdcard1/DCIM/", "extSdCard/DCIM/",
			"external_sd/DCIM/", "external_SD/DCIM/", "MicroSD/DCIM/", "emulated/0/DCIM/", "sdcard0/DCIM/",
			"sdcard-ext/DCIM/", "sdcard-ext/DCIM/", "sdcard/DCIM/" };

	// get file saving directory
	// toInternalMemory - should be true only if force save to internal
	public static File getSaveDir(boolean forceSaveToInternalMemory)
	{
		File dcimDir, saveDir = null, memcardDir;
		boolean usePhoneMem = true;

		String abcDir = "Camera";
		if (MainScreen.isSortByData())
		{
			Calendar rightNow = Calendar.getInstance();
			abcDir = String.format("%tF", rightNow);
		}

		if (Integer.parseInt(MainScreen.getSaveTo()) == 1)
		{
			dcimDir = Environment.getExternalStorageDirectory();

			for (int i = 0; i < SAVEDIR_DIR_PATH_NAMES.length; i++)
			{
				if (MEMCARD_DIR_PATH[i].isEmpty())
				{
					memcardDir = new File(dcimDir, MEMCARD_DIR_PATH_NAMES[i]);
					if (memcardDir.exists())
					{
						saveDir = new File(dcimDir, SAVEDIR_DIR_PATH_NAMES[i] + abcDir);
						usePhoneMem = false;
						break;
					}
				} else
				{
					memcardDir = new File(MEMCARD_DIR_PATH[i], MEMCARD_DIR_PATH_NAMES[i]);
					if (memcardDir.exists())
					{
						saveDir = new File(MEMCARD_DIR_PATH[i], SAVEDIR_DIR_PATH_NAMES[i] + abcDir);
						usePhoneMem = false;
						break;
					}
				}
			}
		} else if ((Integer.parseInt(MainScreen.getSaveTo()) == 2))
		{
			if (MainScreen.isSortByData())
			{
				saveDir = new File(MainScreen.getSaveToPath(), abcDir);
			} else {
				saveDir = new File(MainScreen.getSaveToPath());
			}
			usePhoneMem = false;
		}

		if (usePhoneMem || forceSaveToInternalMemory) // phone memory (internal
														// sd card)
		{
			dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
			saveDir = new File(dcimDir, abcDir);
		}
		if (!saveDir.exists())
			saveDir.mkdirs();

		// if creation failed - try to switch to phone mem
		if (!saveDir.exists())
		{
			dcimDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM);
			saveDir = new File(dcimDir, abcDir);

			if (!saveDir.exists())
				saveDir.mkdirs();
		}
		return saveDir;
	}

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
				MainScreen.getAppResources().openRawResourceFd(R.raw.plugin_capture_selftimer_countdown), MainScreen
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
				msg.what = PluginManager.MSG_DELAYED_CAPTURE;
				MainScreen.getMessageHandler().sendMessage(msg);

				timer = null;
			}
		};
		timer.start();
	}

	public void TickEverySecond(boolean isLastSecond)
	{
		if (MainScreen.isShutterSoundEnabled())
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
		countdownPlayer = new SoundPlayer(MainScreen.getMainContext(), fd_countdown);
		finalcountdownPlayer = new SoundPlayer(MainScreen.getMainContext(), fd_finalcountdown);
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
		InputStream in = MainScreen.getInstance().getContentResolver()
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
		OutputStream out = MainScreen.getInstance().getContentResolver()
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
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

			for (int i = 1; i <= imagesAmount; i++)
			{
				String format = getFromSharedMem("resultframeformat" + i + Long.toString(sessionID));
				
				String idx = "";

				if (imagesAmount != 1)
					idx += "_" + i;

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
					os = MainScreen.getInstance().getContentResolver()
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

				// Take only one result frame from several results
				// Used for PreShot plugin that may decide which result to save
				if (imagesAmount == 1 && imageIndex != 0)
					i = imageIndex;

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
							.getDefaultSharedPreferences(MainScreen.getMainContext());
					jpegQuality = Integer.parseInt(prefs.getString(MainScreen.sJPEGQualityPref, "95"));
					if (!out.compressToJpeg(r, jpegQuality, os))
					{
						MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED_IOEXCEPTION);
						return;
					}
					SwapHeap.FreeFromHeap(yuv);
				}

				String orientation_tag = String.valueOf(0);
				switch (orientation)
				{
				default:
				case 0:
					orientation_tag = String.valueOf(0);
					break;
				case 90:
					orientation_tag = cameraMirrored ? String.valueOf(270) : String.valueOf(90);
					break;
				case 180:
					orientation_tag = String.valueOf(180);
					break;
				case 270:
					orientation_tag = cameraMirrored ? String.valueOf(90) : String.valueOf(270);
					break;
				}

				int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
				if (writeOrientationTag)
				{
					switch ((orientation + additionalRotationValue + 360) % 360)
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
					exifManager = new ExifManager(exifDriver, MainScreen.getInstance());
				}

				if (useGeoTaggingPrefExport)
				{
					Location l = MLocation.getLocation(MainScreen.getMainContext());

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
					String softwareString = MainScreen.getAppResources().getString(R.string.app_name) + ", "
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

				MainScreen.getInstance().getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
			}

			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);
		} catch (IOException e)
		{
			e.printStackTrace();
			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED_IOEXCEPTION);
			return;
		} catch (Exception e)
		{
			e.printStackTrace();
			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);
		} finally
		{
			MainScreen.setForceFilename(null);
		}
	}

	private void addTimestamp(File file)
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());

			int dateFormat = Integer.parseInt(prefs.getString(MainScreen.sTimestampDate, "0"));
			boolean abbreviation = prefs.getBoolean(MainScreen.sTimestampAbbreviation, false);
			int timeFormat = Integer.parseInt(prefs.getString(MainScreen.sTimestampTime, "0"));
			int separator = Integer.parseInt(prefs.getString(MainScreen.sTimestampSeparator, "0"));
			String customText = prefs.getString(MainScreen.sTimestampCustomText, "");
			int color = Integer.parseInt(prefs.getString(MainScreen.sTimestampColor, "1"));
			int fontSizeC = Integer.parseInt(prefs.getString(MainScreen.sTimestampFontSize, "80"));

			if (dateFormat==0 && timeFormat ==0 && customText.equals(""))
				return;
			
			String dateFormatString = "";
			String timeFormatString = "";
			String separatorString = ".";
			String monthString = abbreviation ? "MMMM" : "MM";

			switch (separator)
			{
			case 0:
				separatorString = "/";
				break;
			case 1:
				separatorString = ".";
				break;
			case 2:
				separatorString = "-";
				break;
			case 3:
				separatorString = " ";
				break;
			default:
			}

			switch (dateFormat)
			{
			case 1:
				dateFormatString = "yyyy" + separatorString + monthString + separatorString + "dd";
				break;
			case 2:
				dateFormatString = "dd" + separatorString + monthString + separatorString + "yyyy";
				break;
			case 3:
				dateFormatString = monthString + separatorString + "dd" + separatorString + "yyyy";
				break;
			default:
			}

			switch (timeFormat)
			{
			case 1:
				timeFormatString = " hh:mm:ss a";
				break;
			case 2:
				timeFormatString = " HH:mm:ss";
				break;
			default:
			}

			Date currentDate = Calendar.getInstance().getTime();
			java.text.SimpleDateFormat simpleDateFormat = new java.text.SimpleDateFormat(dateFormatString
					+ timeFormatString);
			String formattedCurrentDate = simpleDateFormat.format(currentDate);

			formattedCurrentDate = formattedCurrentDate + "\n" + customText;

			if (formattedCurrentDate.equals(""))
				return;

			Bitmap sourceBitmap;
			Bitmap bitmap;

			ExifInterface exifInterface = new ExifInterface(file.getAbsolutePath());
			int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
					ExifInterface.ORIENTATION_NORMAL);
			int rotation = 0;
			Matrix matrix = new Matrix();
			if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
			{
				rotation = 90;
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
			{
				rotation = 180;
			} else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
			{
				rotation = 270;
			}
			matrix.postRotate(rotation);

			BitmapFactory.Options options = new BitmapFactory.Options();
			options.inMutable = true;

			sourceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath(), options);
			bitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(), sourceBitmap.getHeight(), matrix,
					false);

			sourceBitmap.recycle();

			int width = bitmap.getWidth();
			int height = bitmap.getHeight();

			Paint p = new Paint();

			Canvas canvas = new Canvas(bitmap);

			final float scale = MainScreen.getInstance().getResources().getDisplayMetrics().density;

			p.setColor(Color.WHITE);
			switch (color)
			{
			case 0:
				color = Color.BLACK;
				p.setColor(Color.BLACK);
				break;
			case 1:
				color = Color.WHITE;
				p.setColor(Color.WHITE);
				break;
			case 2:
				color = Color.YELLOW;
				p.setColor(Color.YELLOW);
				break;

			}

			if (width > height)
			{
				p.setTextSize(height / fontSizeC * scale + 0.5f); // convert dps
																	// to
				// pixels
			} else
			{
				p.setTextSize(width / fontSizeC * scale + 0.5f); // convert dps
																	// to
				// pixels
			}
			p.setTextAlign(Align.RIGHT);
			drawTextWithBackground(canvas, p, formattedCurrentDate, color, Color.BLACK, width, height);

			Matrix matrix2 = new Matrix();
			matrix2.postRotate(360 - rotation);
			sourceBitmap = Bitmap.createBitmap(bitmap, 0, 0, width, height, matrix2, false);

			bitmap.recycle();

			FileOutputStream outStream;
			outStream = new FileOutputStream(file);
			sourceBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outStream);
			sourceBitmap.recycle();
			outStream.flush();
			outStream.close();
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e)
		{
			e.printStackTrace();
		} catch (OutOfMemoryError e)
		{
			e.printStackTrace();
		}
	}
	
	@TargetApi(21)
	public void saveDNGPicture(int frameNum, long sessionID, OutputStream os, int width, int height, int orientation, boolean cameraMirrored)
	{
		DngCreator creator = new DngCreator(CameraController.getCameraCharacteristics(),
											this.getFromRAWCaptureResults("captureResult" + frameNum + sessionID));
		byte[] frame = SwapHeap.SwapFromHeap(
				Integer.parseInt(getFromSharedMem("resultframe" + frameNum + Long.toString(sessionID))),
				Integer.parseInt(getFromSharedMem("resultframelen" + frameNum + Long.toString(sessionID))));
		
		ByteBuffer buff = ByteBuffer.allocateDirect(frame.length);
		buff.put(frame);
		
		int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
		switch ((orientation + additionalRotationValue + 360) % 360)
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
		
		try
		{
			creator.setOrientation(exif_orientation);
			creator.writeByteBuffer(os, new Size(width, height), buff , 0);
		}
		catch (IOException e)
		{
			creator.close();
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		creator.close();
	}

	private void rotateImage(File file, Matrix matrix)
	{
		try
		{
			Bitmap sourceBitmap = BitmapFactory.decodeFile(file.getAbsolutePath());
			Bitmap rotatedBitmap = Bitmap.createBitmap(sourceBitmap, 0, 0, sourceBitmap.getWidth(),
					sourceBitmap.getHeight(), matrix, true);

			FileOutputStream outStream;
			outStream = new FileOutputStream(file);
			rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, jpegQuality, outStream);
			outStream.flush();
			outStream.close();
		} catch (FileNotFoundException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background,
			int imageWidth, int imageHeight)
	{
		Rect text_bounds = new Rect();
		paint.setColor(foreground);
		String[] resText = text.split("\n");
		String maxLengthText = "";

		if (resText.length > 1)
		{
			maxLengthText = resText[0].length() > resText[1].length() ? resText[0] : resText[1];
		} else if (resText.length > 0)
		{
			maxLengthText = resText[0];
		}

		final float scale = MainScreen.getInstance().getResources().getDisplayMetrics().density;
		paint.setStyle(Paint.Style.FILL);
		paint.setColor(background);
		paint.setAlpha(64);
		paint.getTextBounds(text, 0, maxLengthText.length(), text_bounds);
		final int padding = (int) (2 * scale + 0.5f); // convert dps to pixels

		int textWidth = 0;
		int textHeight = text_bounds.bottom - text_bounds.top;
		if (paint.getTextAlign() == Paint.Align.RIGHT || paint.getTextAlign() == Paint.Align.CENTER)
		{
			float width = paint.measureText(maxLengthText); // n.b., need to use
			// measureText rather than
			// getTextBounds here
			textWidth = (int) width;
		}

		text_bounds.left = imageWidth - textWidth - 2 * padding;
		text_bounds.right = imageWidth - padding;
		if (resText.length > 1)
		{
			text_bounds.top = imageHeight - 2 * padding - 2 * textHeight - textHeight;
		} else
		{
			text_bounds.top = imageHeight - 2 * padding - textHeight;
			textHeight /= 3;
		}
		text_bounds.bottom = imageHeight - padding;

		// canvas.drawRect(text_bounds, paint);

		paint.setColor(foreground);
		if (resText.length > 0)
		{
			canvas.drawText(resText[0], imageWidth, imageHeight - 2 * textHeight, paint);
		}
		if (resText.length > 1)
		{
			canvas.drawText(resText[1], imageWidth - padding, imageHeight - textHeight / 2, paint);
		}
	}

	public String getExportFileName(String modeName)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		saveOption = Integer.parseInt(prefs.getString(MainScreen.sExportNamePref, "2"));

		String prefix = prefs.getString(MainScreen.sExportNamePrefixPref, "");
		if (!prefix.equals(""))
			prefix = prefix + "_";

		String postfix = prefs.getString(MainScreen.sExportNamePostfixPref, "");
		if (!postfix.equals(""))
			postfix = "_" + postfix;

		Calendar d = Calendar.getInstance();
		String fileFormat = String.format("%04d%02d%02d_%02d%02d%02d", d.get(Calendar.YEAR), d.get(Calendar.MONTH) + 1,
				d.get(Calendar.DAY_OF_MONTH), d.get(Calendar.HOUR_OF_DAY), d.get(Calendar.MINUTE),
				d.get(Calendar.SECOND));
		switch (saveOption)
		{
		case 1:// YEARMMDD_HHMMSS
			fileFormat = prefix + fileFormat + postfix;
			break;

		case 2:// YEARMMDD_HHMMSS_MODE
			fileFormat = prefix + fileFormat + (modeName.equals("") ? "" : ("_" + modeName)) + postfix;
			break;

		case 3:// IMG_YEARMMDD_HHMMSS
			fileFormat = prefix + "IMG_" + fileFormat + postfix;
			break;

		case 4:// IMG_YEARMMDD_HHMMSS_MODE
			fileFormat = prefix + "IMG_" + fileFormat + (modeName.equals("") ? "" : ("_" + modeName)) + postfix;
			break;
		default:
			break;
		}

		return fileFormat;
	}

	public String getFileFormat()
	{
		return getExportFileName(getActiveMode().modeSaveName);
	}

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
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
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

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		if (prefs.getBoolean("useGeoTaggingPrefExport", false))
		{
			Location l = MLocation.getLocation(MainScreen.getMainContext());

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

		MainScreen.getInstance().getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
	}

	public void sendMessage(int what, String obj, int arg1, int arg2)
	{
		Message message = new Message();
		message.obj = String.valueOf(obj);
		message.arg1 = arg1;
		message.arg2 = arg2;
		message.what = what;
		MainScreen.getMessageHandler().sendMessage(message);
	}

	public void sendMessage(int what, int arg1)
	{
		Message message = new Message();
		message.arg1 = arg1;
		message.what = what;
		MainScreen.getMessageHandler().sendMessage(message);
	}

	public void sendMessage(int what, String obj)
	{
		Message message = new Message();
		message.obj = String.valueOf(obj);
		message.what = what;
		MainScreen.getMessageHandler().sendMessage(message);
	}
}
