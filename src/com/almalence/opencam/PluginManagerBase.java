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
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.hardware.Camera;
import android.hardware.camera2.CaptureResult;
import android.location.Location;
import android.media.ExifInterface;
import android.opengl.GLSurfaceView;
import android.os.Build;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.support.v4.provider.DocumentFile;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.almalence.plugins.export.standard.GPSTagsConverter;
import com.almalence.util.MLocation;
import com.almalence.util.Util;
import com.almalence.util.exifreader.imaging.jpeg.JpegMetadataReader;
import com.almalence.util.exifreader.imaging.jpeg.JpegProcessingException;
import com.almalence.util.exifreader.metadata.Directory;
import com.almalence.util.exifreader.metadata.Metadata;
import com.almalence.util.exifreader.metadata.exif.ExifIFD0Directory;
import com.almalence.util.exifreader.metadata.exif.ExifSubIFDDirectory;

/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;

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

abstract public class PluginManagerBase implements PluginManagerInterface
{

	// we need some selection of active plugins by type.
	// probably different lists for different plugin's types
	// + probably it's more useful to have map instead of list (Map<Integer,
	// Plugin> pluginList)
	// in map case we'll have all plugins in one map and keys of active plugins
	// of each type (as we have limited amount
	// of types we can have just simple int variables or create a list, but it's
	// more complicated)
	protected Map<String, Plugin>				pluginList;
	protected Map<Long, Plugin>					processingPluginList;

	// active plugins IDs
	protected List<String>						activeVF;
	protected String							activeCapture;
	protected String							activeProcessing;
	// protected List<String> activeFilter;
	protected String							activeExport;

	// list of plugins by type
	protected List<Plugin>						listVF;
	protected List<Plugin>						listCapture;
	protected List<Plugin>						listProcessing;
	// protected List<Plugin> listFilter;
	protected List<Plugin>						listExport;

	// counter indicating amout of processing tasks running
	protected int								cntProcessing			= 0;

	// table for sharing plugin's data
	// hashtable for storing shared data - assoc massive for string key and
	// string value
	// file SharedMemory.txt contains data keys and formats for currently used
	// data
	protected Hashtable<String, String>			sharedMemory;
	protected Hashtable<String, CaptureResult>	rawCaptureResults;

	// Support flag to avoid plugin's view disappearance issue
	protected static boolean					isRestarting			= false;

	static int									jpegQuality				= 95;

	protected static boolean					isDefaultsSelected		= false;

	protected static Map<Integer, Integer>		exifOrientationMap;

	protected int								saveOption;
	private boolean								useGeoTaggingPrefExport;
	private boolean								enableExifTagOrientation;
	private int									additionalRotation;
	private int									additionalRotationValue	= 0;

	// plugin manager ctor. plugins initialization and filling plugin list
	protected PluginManagerBase()
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

		createPlugins();

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

	abstract protected void createPlugins();

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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		if (mode == null)
		{
			// set default mode - get this val from mode.xml and later control
			// in preerences
			mode = ConfigParser.getInstance().getDefaultMode();

			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(ApplicationScreen.sDefaultModeName, mode.modeID);
			prefsEditor.commit();
		}

		// set active plugins for default mode
		activeVF.clear();
		for (int i = 0; i < mode.VF.size(); i++)
			activeVF.add(mode.VF.get(i));
		activeCapture = mode.Capture;
		activeProcessing = mode.Processing;
		// for (int i = 0; i < mode.Filter.size(); i++)
		// activeFilter.add(mode.Filter.get(i));
		// activeFilter.clear();
		activeExport = mode.Export;
	}

	public String getActiveModeID()
	{
		Mode mode = getActiveMode();
		if (mode != null)
			return mode.modeID;
		return "";
	}

	public Mode getActiveMode()
	{
		// select default mode - selection from preferences if exists. or from
		// config if first start
		return getMode();
	}

	protected Mode getMode()
	{
		Mode mode = null;

		// checks preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		if (prefs.contains(ApplicationScreen.sDefaultModeName))
		{
			String defaultModeName = prefs.getString(ApplicationScreen.sDefaultModeName, "");
			mode = ConfigParser.getInstance().getMode(defaultModeName);
		} else
		{
			// set default mode - get this val from mode.xml and later control
			// in preerences
			mode = ConfigParser.getInstance().getDefaultMode();

			Editor prefsEditor = prefs.edit();
			prefsEditor.putString(ApplicationScreen.sDefaultModeName, mode.modeID);
			prefsEditor.commit();
		}

		return mode;
	}

	// isFromMain - indicates event originator - applicationscreen or processing
	// screen
	public void onCreate()
	{
		isDefaultsSelected = false;

		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onCreate();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onCreate();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onCreate();
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onCreate();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onCreate();

		onManagerCreate();
	}

	abstract void onManagerCreate();

	// parse config to get camera and modes configurations
	void parseConfig()
	{
		try
		{
			ConfigParser.getInstance().parse(ApplicationScreen.getMainContext());
		} catch (XmlPullParserException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}
	}

	protected boolean	isRestart					= false;
	protected boolean	switchToOldCameraInterface	= false;

	public void setSwitchModeType(boolean restart)
	{
		isRestart = restart;
	}

	public boolean isRestart()
	{
		return isRestart;
	}

	public boolean isSwitchToOldCameraInterface()
	{
		return switchToOldCameraInterface;
	}

	public void switchMode(final Mode mode)
	{
		String modeName = mode.modeID;
		if (modeName.equals("video")
			|| (CameraController.isNexus6 && (modeName.equals("panorama_augmented") || modeName.equals("preshot")))
			|| (CameraController.isFlex2 && (modeName.equals("hdrmode") || modeName.equals("expobracketing"))))
			switchToOldCameraInterface = true;
		else
			switchToOldCameraInterface = false;

		// disable old plugins
		ApplicationScreen.getGUIManager().onStop();
		ApplicationScreen.instance.switchingMode(isRestart ? false : true);
		ApplicationScreen.instance.pauseMain();
		onStop();
		onDestroy();

		CameraController.resetNeedPreviewFrame();

		// clear lists and fill with new active plugins
		activeVF.clear();
		for (int i = 0; i < mode.VF.size(); i++)
			activeVF.add(mode.VF.get(i));
		activeCapture = mode.Capture;
		activeProcessing = mode.Processing;
		// activeFilter.clear();
		// for (int i = 0; i < mode.Filter.size(); i++)
		// activeFilter.add(mode.Filter.get(i));
		activeExport = mode.Export;

		// set mode as default for future starts
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		Editor prefsEditor = prefs.edit();
		prefsEditor.putString(ApplicationScreen.sDefaultModeName, mode.modeID);
		prefsEditor.commit();

		onCreate();
		onStart();
		ApplicationScreen.instance.switchingMode(isRestart ? false : true);
		ApplicationScreen.instance.resumeMain();
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
		// case Filter:
		// {
		// for (int i = 0; i < activeFilter.size(); i++)
		// activePlugins.add(pluginList.get(i));
		// }
		// break;
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onStart();
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onStop();
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onDestroy();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onDestroy();
	}

	// base onResume stage
	public void onResume()
	{
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onResume();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onResume();
		if (null != pluginList.get(activeProcessing))
			pluginList.get(activeProcessing).onResume();
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onResume();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onResume();
	}

	// base onPause stage
	public void onPause(boolean isFromMain)
	{
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

	public void menuButtonPressed()
	{
		onShowPreferences();
		Intent settingsActivity = new Intent(ApplicationScreen.getMainContext(), Preferences.class);
		ApplicationScreen.instance.startActivity(settingsActivity);
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onShowPreferences();
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onGUICreate();
		if (null != pluginList.get(activeExport))
			pluginList.get(activeExport).onGUICreate();

		isRestarting = true;
	}

	protected boolean	isUserClicked	= true;

	public void onShutterClickNotUser()
	{
		isUserClicked = false;
		onShutterClick();
	}

	public void onShutterClick()
	{
		for (int i = 0; i < activeVF.size(); i++)
			pluginList.get(activeVF.get(i)).onShutterClick();
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).onShutterClick();

		isUserClicked = true;
	}

	public void onFocusButtonClick()
	{
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

	public boolean onMultiTouch(View view, MotionEvent e)
	{
		int res = 0;
		for (int i = 0; i < activeVF.size(); i++)
			res += (pluginList.get(activeVF.get(i)).onMultiTouch(view, e) ? 1 : 0);
		if (null != pluginList.get(activeCapture))
			res += (pluginList.get(activeCapture).onMultiTouch(view, e) ? 1 : 0);
		return (res > 0 ? true : false);
	}
	
	public Plugin getPlugin(String id)
	{
		return pluginList.get(id);
	}

	protected void AddModeSettings(String modeName, PreferenceFragment pf)
	{
		if (modeName.equals("super") && CameraController.isUseCamera2())
		{
			pf.addPreferencesFromResource(R.xml.preferences_processing_super);
			return;
		}

		Mode mode = ConfigParser.getInstance().getMode(modeName);
		if (mode == null)
			return;
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
			// all modes below use simple processing and we should avoid
			// duplicating DRO settings here.
			if (modeName.equals("video") || modeName.equals("burstmode") || modeName.equals("expobracketing")
					|| modeName.equals("burstmode"))
				return;
			Plugin pg = listProcessing.get(j);
			if (mode.Processing.equals(pg.getID()))
			{
				addHeadersContent(pf, pg, false);
			}
		}
	}

	abstract public void loadHeaderContent(String settings, PreferenceFragment pf);

	protected void addHeadersContent(PreferenceFragment pf, List<Plugin> list, boolean isAdvanced)
	{
		int size = list.size(), i = 0;
		while (i < size)
		{
			addHeadersContent(pf, list.get(i), isAdvanced);
			i++;
		}
	}

	protected void addHeadersContent(PreferenceFragment pf, Plugin plugin, boolean isAdvanced)
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// pluginList.get(i).onOrientationChanged(orientation);
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// res += pluginList.get(i).onKeyDown(keyCode, event) ? 1 : 0;
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
	public void addRequestID(int nFrame, int requestID)
	{
		if (null != pluginList.get(activeCapture))
			pluginList.get(activeCapture).addRequestID(nFrame, requestID);
	}

	@Override
	public void collectExifData(byte[] frameData)
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
			RelativeLayout pluginsLayout = (RelativeLayout) ApplicationScreen.instance.findViewById(R.id.mainLayout1);
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
		// for (int i = 0; i < activeFilter.size(); i++)
		// {
		// res = pluginList.get(i).onBroadcast(arg1, arg2);
		// if (res)
		// return;
		// }
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
		return handleApplicationMessage(msg);
	}

	public boolean handleApplicationMessage(Message msg)
	{
		long sessionID = 0;
		
		switch (msg.what)
		{
		case ApplicationInterface.MSG_NO_CAMERA:
			break;

		case ApplicationInterface.MSG_CAPTURE_FINISHED:

			/*
			 * Debug code for Galaxy S6 in Super mode. Look at Camera2 for more
			 * details
			 */
			// CameraController.onCaptureFinished();

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

			int id = ApplicationScreen.getAppResources().getIdentifier(getActiveMode().modeName, "string",
					ApplicationScreen.instance.getPackageName());
			String modeName = ApplicationScreen.getAppResources().getString(id);
			addToSharedMem("mode_name" + (String) msg.obj, modeName);
			// start async task for further processing
			cntProcessing++;

			sessionID = Long.valueOf((String) msg.obj);
			
			// Map sessionID and processing plugin, because active plugin may be
			// changed before image processing will start.
			// We don't map export plugin, because it's the same for all modes.
			processingPluginList.put(sessionID, pluginList.get(activeProcessing));

			Intent mServiceIntent = new Intent(ApplicationScreen.instance, ProcessingService.class);

			// Pass to Service sessionID and some other parameters, that may be
			// required.
			mServiceIntent.putExtra("sessionID", sessionID);
			CameraController.Size imageSize = CameraController.getCameraImageSize();
			PluginManager.getInstance().addToSharedMem("imageWidth" + sessionID, String.valueOf(imageSize.getWidth()));
			PluginManager.getInstance().addToSharedMem("imageHeight" + sessionID, String.valueOf(imageSize.getHeight()));
			PluginManager.getInstance().addToSharedMem("wantLandscapePhoto" + sessionID, String.valueOf(ApplicationScreen.getWantLandscapePhoto()));
			PluginManager.getInstance().addToSharedMem("cameraMirrored" + sessionID, String.valueOf(CameraController.isFrontCamera()));

			// Start processing service with current sessionID.
			ApplicationScreen.instance.startService(mServiceIntent);

			ApplicationScreen.instance.muteShutter(false);

			break;

		case ApplicationInterface.MSG_CAPTURE_FINISHED_NORESULT:

			/*
			 * Debug code for Galaxy S6 in Super mode. Look at Camera2 for more
			 * details
			 */
			// CameraController.onCaptureFinished();

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

			sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);

			ApplicationScreen.getGUIManager().onExportFinished();

			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onExportFinished();

			break;

		case ApplicationInterface.MSG_START_POSTPROCESSING:
			if (null != pluginList.get(activeProcessing))
			{
				ApplicationScreen.getGUIManager().lockControls = true;
				sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_LOCKED);

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
			sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);

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
				if (ApplicationScreen.instance.getIntent().getAction().equals(MediaStore.ACTION_IMAGE_CAPTURE))
				{
					ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_RETURN_CAPTURED);
				}
			}

			ApplicationScreen.getGUIManager().lockControls = false;
			sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);

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

			Toast.makeText(ApplicationScreen.getMainContext(), "Can't save data - no free space left or problems while saving occurred.",
					Toast.LENGTH_LONG).show();

			ApplicationScreen.getGUIManager().lockControls = false;
			sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);

			break;

		case ApplicationInterface.MSG_DELAYED_CAPTURE:
			for (int i = 0; i < activeVF.size(); i++)
				pluginList.get(activeVF.get(i)).onShutterClick();
			if (null != pluginList.get(activeCapture))
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
			onBroadcast(msg.arg1, msg.arg2);
			break;
		default:
			break;
		}

		return true;
	}

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
	public boolean addToSharedMemExifTagsFromCaptureResult(final CaptureResult result, final long SessionID,
			final int num)
	{
		String exposure_time = String.valueOf(result.get(CaptureResult.SENSOR_EXPOSURE_TIME));
		String sensitivity = String.valueOf(result.get(CaptureResult.SENSOR_SENSITIVITY));
		String aperture = String.valueOf(result.get(CaptureResult.LENS_APERTURE));
		String focal_lenght = String.valueOf(result.get(CaptureResult.LENS_FOCAL_LENGTH));
		String flash_mode = String.valueOf(result.get(CaptureResult.FLASH_MODE));
		String awb_mode = String.valueOf(result.get(CaptureResult.CONTROL_AWB_MODE));

		if (num != -1 && exposure_time != null && !exposure_time.equals("null"))
			addToSharedMem("exiftag_exposure_time" + num + SessionID, exposure_time);
		else if (exposure_time != null && !exposure_time.equals("null"))
			addToSharedMem("exiftag_exposure_time" + SessionID, exposure_time);
		if (sensitivity != null && !sensitivity.equals("null"))
			addToSharedMem("exiftag_iso" + SessionID, sensitivity);
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
			s1 = params.getWhiteBalance().compareTo(
					ApplicationScreen.getAppResources().getString(R.string.wbAutoSystem)) == 0 ? String.valueOf(0)
					: String.valueOf(1);
		String s2 = Build.MANUFACTURER;
		String s3 = Build.MODEL;

		String s4 = null;
		if (ApplicationScreen.getGUIManager().mISOSupported)
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
	private boolean containsRAWCaptureResults(String key)
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

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
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

	// /////////////////////////////////////
	// Utils
	// /////////////////////////////////////

	public int getProcessingCounter()
	{
		return cntProcessing;
	}

	public String getFileFormat()
	{
		if (CameraController.isUseCamera2())
		{
			return SavingService.getExportFileName(getActiveMode().modeSaveNameHAL);
		} else
		{
			return SavingService.getExportFileName(getActiveMode().modeSaveName);
		}
	}

	public static File getSaveDir(boolean forceSaveToInternalMemory) {
		return SavingService.getSaveDir(forceSaveToInternalMemory);
	}
	
	public static DocumentFile getSaveDirNew(boolean forceSaveToInternalMemory) {
		return SavingService.getSaveDirNew(forceSaveToInternalMemory);
	}
	
	public static String getExportFileName(String modeName) {
		return SavingService.getExportFileName(modeName);
	}
	
	// Save result pictures method.
	public void saveResultPicture(long sessionID)
	{
		Intent mServiceIntent = new Intent(ApplicationScreen.instance, SavingService.class);

		// Pass to Service sessionID.
		mServiceIntent.putExtra("sessionID", sessionID);

		// Start processing service with current sessionID.
		ApplicationScreen.instance.startService(mServiceIntent);
	}

	public void saveInputFile(boolean isYUV, Long SessionID, int i, byte[] buffer, int yuvBuffer, String fileFormat)
	{
		// if Android 5+ use new saving method.
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP)
		{
			saveInputFileNew(isYUV, SessionID, i, buffer, yuvBuffer, fileFormat);
			return;
		}

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		ContentValues values = null;
		String resultOrientation = getFromSharedMem("frameorientation" + (i + 1) + Long.toString(SessionID));
		if (resultOrientation == null)
		{
			resultOrientation = getFromSharedMem("frameorientation" + i + Long.toString(SessionID));
		}

		String resultMirrored = getFromSharedMem("framemirrored" + (i + 1) + Long.toString(SessionID));
		if (resultMirrored == null)
		{
			resultMirrored = getFromSharedMem("framemirrored" + i + Long.toString(SessionID));
		}
		Boolean cameraMirrored = false;
		if (resultMirrored != null)
			cameraMirrored = Boolean.parseBoolean(resultMirrored);

		int mDisplayOrientation = 0;
		if (resultOrientation != null)
		{
			mDisplayOrientation = Integer.parseInt(resultOrientation);
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		boolean saveGeoInfo = prefs.getBoolean("useGeoTaggingPrefExport", false);

		File saveDir = getSaveDir(false);
		File file = new File(saveDir, fileFormat + ".jpg");
		FileOutputStream os = null;

		try
		{
			try
			{
				os = new FileOutputStream(file);
			} catch (Exception e)
			{
				// save always if not working saving to sdcard
				e.printStackTrace();
				saveDir = getSaveDir(true);
				file = new File(saveDir, fileFormat + ".jpg");

				os = new FileOutputStream(file);
			}
		} catch (FileNotFoundException e1)
		{
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		try
		{
			if (os != null)
			{
				if (!isYUV)
				{
					os.write(buffer);
				} else
				{
					jpegQuality = Integer.parseInt(prefs.getString(MainScreen.sJPEGQualityPref, "95"));

					com.almalence.YuvImage image = new com.almalence.YuvImage(yuvBuffer, ImageFormat.NV21,
							imageSize.getWidth(), imageSize.getHeight(), null);
					// to avoid problems with SKIA
					int cropHeight = image.getHeight() - image.getHeight() % 16;
					image.compressToJpeg(new Rect(0, 0, image.getWidth(), cropHeight), jpegQuality, os);

					mDisplayOrientation = saveExifToInput(file, mDisplayOrientation, cameraMirrored, saveGeoInfo);
				}
				os.close();
			}
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		values = new ContentValues();
		values.put(ImageColumns.TITLE, file.getName().substring(0, file.getName().lastIndexOf(".")));
		values.put(ImageColumns.DISPLAY_NAME, file.getName());
		values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
		values.put(ImageColumns.MIME_TYPE, "image/jpeg");
		values.put(ImageColumns.ORIENTATION, mDisplayOrientation);
		values.put(ImageColumns.DATA, file.getAbsolutePath());

		if (saveGeoInfo)
		{
			Location l = MLocation.getLocation(ApplicationScreen.getMainContext());

			if (l != null)
			{
				values.put(ImageColumns.LATITUDE, l.getLatitude());
				values.put(ImageColumns.LONGITUDE, l.getLongitude());
			}
		}

		ApplicationScreen.instance.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
	}

	private void saveInputFileNew(boolean isYUV, Long SessionID, int i, byte[] buffer, int yuvBuffer, String fileFormat)
	{
		
		int mImageWidth = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageWidth" + SessionID));
		int mImageHeight = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageHeight" + SessionID));
		ContentValues values = null;
		String resultOrientation = getFromSharedMem("frameorientation" + (i + 1) + Long.toString(SessionID));
		if (resultOrientation == null)
		{
			resultOrientation = getFromSharedMem("frameorientation" + i + Long.toString(SessionID));
		}

		String resultMirrored = getFromSharedMem("framemirrored" + (i + 1) + Long.toString(SessionID));
		if (resultMirrored == null)
		{
			resultMirrored = getFromSharedMem("framemirrored" + i + Long.toString(SessionID));
		}

		Boolean cameraMirrored = false;
		if (resultMirrored != null)
			cameraMirrored = Boolean.parseBoolean(resultMirrored);

		int mDisplayOrientation = 0;
		if (resultOrientation != null)
		{
			mDisplayOrientation = Integer.parseInt(resultOrientation);
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		boolean saveGeoInfo = prefs.getBoolean("useGeoTaggingPrefExport", false);

		DocumentFile file;
		DocumentFile saveDir = getSaveDirNew(false);
		if (saveDir == null || !saveDir.exists())
		{
			return;
		}

		file = saveDir.createFile("image/jpeg", fileFormat);
		if (file == null || !file.canWrite())
		{
			return;
		}

		OutputStream os = null;
		File bufFile = new File(ApplicationScreen.instance.getFilesDir(), "buffer.jpeg");
		try
		{
			os = new FileOutputStream(bufFile);
		} catch (Exception e)
		{
			e.printStackTrace();
		}

		if (os != null)
		{
			try
			{
				if (!isYUV)
				{
					os.write(buffer);
				} else
				{
					jpegQuality = Integer.parseInt(prefs.getString(MainScreen.sJPEGQualityPref, "95"));

					com.almalence.YuvImage image = new com.almalence.YuvImage(yuvBuffer, ImageFormat.NV21,
							mImageWidth, mImageHeight, null);
					// to avoid problems with SKIA
					int cropHeight = image.getHeight() - image.getHeight() % 16;
					image.compressToJpeg(new Rect(0, 0, image.getWidth(), cropHeight), jpegQuality, os);
				}
				os.close();
			} catch (IOException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			mDisplayOrientation = saveExifToInput(bufFile, mDisplayOrientation, cameraMirrored, saveGeoInfo);

			// Copy buffer image with exif tags into result file.
			InputStream is = null;
			int len;
			byte[] buf = new byte[1024];
			try
			{
				os = ApplicationScreen.instance.getContentResolver().openOutputStream(file.getUri());
				is = new FileInputStream(bufFile);
				while ((len = is.read(buf)) > 0)
				{
					os.write(buf, 0, len);
				}
				is.close();
				os.close();
			} catch (Exception e)
			{
				e.printStackTrace();
			}
		}

		bufFile.delete();

		values = new ContentValues();
		values.put(ImageColumns.TITLE, file.getName().substring(0, file.getName().lastIndexOf(".")));
		values.put(ImageColumns.DISPLAY_NAME, file.getName());
		values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
		values.put(ImageColumns.MIME_TYPE, "image/jpeg");
		values.put(ImageColumns.ORIENTATION, mDisplayOrientation);
		
		String filePath = file.getName();
		// If we able to get File object, than get path from it.
		// fileObject should not be null for files on phone memory.
		File fileObject = Util.getFileFromDocumentFile(file);
		if (fileObject != null)
		{
			filePath = fileObject.getAbsolutePath();
			values.put(ImageColumns.DATA, filePath);
		} else
		{
			// This case should typically happen for files saved to SD
			// card.
			String documentPath = Util.getAbsolutePathFromDocumentFile(file);
			values.put(ImageColumns.DATA, documentPath);
		}

		if (saveGeoInfo)
		{
			Location l = MLocation.getLocation(ApplicationScreen.getMainContext());
			if (l != null)
			{
				values.put(ImageColumns.LATITUDE, l.getLatitude());
				values.put(ImageColumns.LONGITUDE, l.getLongitude());
			}
		}

		ApplicationScreen.instance.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
	}

	public static int saveExifToInput(File file, int displayOrientation, boolean cameraMirrored, boolean saveGeo)
	{
		try
		{
			ExifInterface ei = new ExifInterface(file.getAbsolutePath());
			int exif_orientation = ExifInterface.ORIENTATION_NORMAL;
			switch (displayOrientation)
			{
			default:
			case 0:
				exif_orientation = ExifInterface.ORIENTATION_NORMAL;
				break;
			case 90:
				if (cameraMirrored)
				{
					displayOrientation = 270;
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
					displayOrientation = 90;
					exif_orientation = ExifInterface.ORIENTATION_ROTATE_90;
				} else
				{
					exif_orientation = ExifInterface.ORIENTATION_ROTATE_270;
				}
				break;
			}
			ei.setAttribute(ExifInterface.TAG_ORIENTATION, "" + exif_orientation);

			if (saveGeo)
			{
				Location l = MLocation.getLocation(ApplicationScreen.getMainContext());

				if (l != null)
				{
					ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE, GPSTagsConverter.convert(l.getLatitude()));
					ei.setAttribute(ExifInterface.TAG_GPS_LATITUDE_REF, GPSTagsConverter.latitudeRef(l.getLatitude()));
					ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE, GPSTagsConverter.convert(l.getLongitude()));
					ei.setAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF,
							GPSTagsConverter.longitudeRef(l.getLongitude()));

				}
			}

			ei.saveAttributes();
		} catch (IOException e)
		{
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return displayOrientation;
	}

	public void sendMessage(int what, String obj, int arg1, int arg2)
	{
		Message message = new Message();
		message.obj = String.valueOf(obj);
		message.arg1 = arg1;
		message.arg2 = arg2;
		message.what = what;
		ApplicationScreen.getMessageHandler().sendMessage(message);
	}

	public void sendMessage(int what, int arg1)
	{
		Message message = new Message();
		message.arg1 = arg1;
		message.what = what;
		ApplicationScreen.getMessageHandler().sendMessage(message);
	}

	public void sendMessage(int what, String obj)
	{
		Message message = new Message();
		message.obj = String.valueOf(obj);
		message.what = what;
		ApplicationScreen.getMessageHandler().sendMessage(message);
	}
}
