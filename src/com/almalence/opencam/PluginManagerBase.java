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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
import android.media.ExifInterface;
import android.opengl.GLSurfaceView;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.os.Message;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Size;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.almalence.SwapHeap;

import com.almalence.plugins.export.ExifDriver.ExifDriver;
import com.almalence.plugins.export.ExifDriver.ExifManager;
import com.almalence.plugins.export.ExifDriver.Values.ValueByteArray;
import com.almalence.plugins.export.ExifDriver.Values.ValueNumber;
import com.almalence.plugins.export.ExifDriver.Values.ValueRationals;
import com.almalence.templatecamera.R;
import com.almalence.util.exifreader.imaging.jpeg.JpegMetadataReader;
import com.almalence.util.exifreader.imaging.jpeg.JpegProcessingException;
import com.almalence.util.exifreader.metadata.Directory;
import com.almalence.util.exifreader.metadata.Metadata;
import com.almalence.util.exifreader.metadata.exif.ExifIFD0Directory;
import com.almalence.util.exifreader.metadata.exif.ExifSubIFDDirectory;
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
	protected Map<String, Plugin>	pluginList;

	// active plugins IDs
	protected List<String>			activeVF;
	protected String				activeCapture;
	protected String				activeProcessing;
	protected List<String>			activeFilter;
	protected String				activeExport;

	// list of plugins by type
	protected List<Plugin>			listVF;
	protected List<Plugin>			listCapture;
	protected List<Plugin>			listProcessing;
	protected List<Plugin>			listFilter;
	protected List<Plugin>			listExport;

	// counter indicating amout of processing tasks running
	protected int					cntProcessing							= 0;

	// table for sharing plugin's data
	// hashtable for storing shared data - assoc massive for string key and
	// string value
	// file SharedMemory.txt contains data keys and formats for currently used
	// data
	protected Hashtable<String, String>				sharedMemory;
	protected Hashtable<String, CaptureResult>		rawCaptureResults;

	// Support flag to avoid plugin's view disappearance issue
	protected static boolean				isRestarting							= false;

	static int								jpegQuality								= 95;

	protected static boolean				isDefaultsSelected						= false;
	
	protected static Map<Integer, Integer>	exifOrientationMap;
	
	
	protected int		saveOption;

	// plugin manager ctor. plugins initialization and filling plugin list
	protected PluginManagerBase()
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
		for (int i = 0; i < mode.Filter.size(); i++)
			activeFilter.add(mode.Filter.get(i));
		activeFilter.clear();
		activeExport = mode.Export;
	}

	public String getActiveModeID()
	{
		Mode mode = getActiveMode();
		if(mode != null)
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

	// isFromMain - indicates event originator - applicationscreen or processing screen
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
	
	protected boolean isRestart = false;
	public void setSwitchModeType(boolean restart)
	{
		isRestart = restart;
	}
	
	public boolean isRestart()
	{
		return isRestart;
	}

	public void switchMode(Mode mode)
	{
		// disable old plugins
		ApplicationScreen.getGUIManager().onStop();
		ApplicationScreen.instance.switchingMode(isRestart? false: true);
		ApplicationScreen.instance.pauseMain();
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		Editor prefsEditor = prefs.edit();
		prefsEditor.putString(ApplicationScreen.sDefaultModeName, mode.modeID);
		prefsEditor.commit();
		
		onCreate();
		onStart();
		ApplicationScreen.instance.switchingMode(isRestart? false: true);
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

	public Plugin getPlugin(String id)
	{
		return pluginList.get(id);
	}

	protected void AddModeSettings(String modeName, PreferenceFragment pf)
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
			if (modeName.equals("video"))
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
	
//	@Override
//	public void createRequestIDList(int nFrames)
//	{
//		if (null != pluginList.get(activeCapture))
//			pluginList.get(activeCapture).createRequestIDList(nFrames);
//	}
	
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
		return handleApplicationMessage(msg);
	}
	
	public boolean handleApplicationMessage(Message msg)
	{
		switch (msg.what)
		{
		case ApplicationInterface.MSG_NO_CAMERA:
			break;

		case ApplicationInterface.MSG_CAPTURE_FINISHED:

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
			ProcessingTask task = new ProcessingTask(ApplicationScreen.instance);
			task.SessionID = Long.valueOf((String) msg.obj);
			task.processing = pluginList.get(activeProcessing);
			task.export = pluginList.get(activeExport);
			task.execute();
			ApplicationScreen.instance.muteShutter(false);

			break;

		case ApplicationInterface.MSG_CAPTURE_FINISHED_NORESULT:

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
			long sessionID = 0;
			String sSessionID = getFromSharedMem("sessionID");
			if (sSessionID != null)
				sessionID = Long.parseLong(getFromSharedMem("sessionID"));

			// notify GUI about saved images
			ApplicationScreen.getGUIManager().lockControls = false;
			sendMessage(ApplicationInterface.MSG_BROADCAST, ApplicationInterface.MSG_CONTROL_UNLOCKED);

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

			Toast.makeText(ApplicationScreen.getMainContext(), "Can't save data - seems no free space left.",
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
			s1 = params.getWhiteBalance().compareTo(ApplicationScreen.getAppResources().getString(R.string.wbAutoSystem)) == 0 ? String
					.valueOf(0) : String.valueOf(1);
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
	protected class ProcessingTask extends AsyncTask<Void, Void, Void>
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
					addToSharedMem("wantLandscapePhoto" + SessionID, String.valueOf(ApplicationScreen.getWantLandscapePhoto()));
					addToSharedMem("CameraMirrored" + SessionID, String.valueOf(CameraController.isFrontCamera()));
				}

			if (null != processing)
			{
				processing.onStartProcessing(SessionID);
				if (processing.isPostProcessingNeeded())
				{
					ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_START_POSTPROCESSING);
					return null;
				}
			}

			if (null != export)
				export.onExportActive(SessionID);
			else
				ApplicationScreen.getMessageHandler().sendEmptyMessage(ApplicationInterface.MSG_EXPORT_FINISHED);

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

	protected static final String[]	MEMCARD_DIR_PATH		= new String[] { "/storage", "/mnt", "", "/storage",
			"/Removable", "/storage", "/storage", "", "/mnt", "/" };

	protected static final String[]	MEMCARD_DIR_PATH_NAMES	= new String[] { "sdcard1", "extSdCard", "external_sd",
			"external_SD", "MicroSD", "emulated", "sdcard0", "sdcard-ext", "sdcard-ext", "sdcard" };

	protected static final String[]	SAVEDIR_DIR_PATH_NAMES	= new String[] { "sdcard1/DCIM/", "extSdCard/DCIM/",
			"external_sd/DCIM/", "external_SD/DCIM/", "MicroSD/DCIM/", "emulated/0/DCIM/", "sdcard0/DCIM/",
			"sdcard-ext/DCIM/", "sdcard-ext/DCIM/", "sdcard/DCIM/" };

	// get file saving directory
	// toInternalMemory - should be true only if force save to internal
	public static File getSaveDir(boolean forceSaveToInternalMemory)
	{
		File dcimDir, saveDir = null, memcardDir;
		boolean usePhoneMem = true;

		String abcDir = "Camera";
		if (ApplicationScreen.instance.isSortByData())
		{
			Calendar rightNow = Calendar.getInstance();
			abcDir = String.format("%tF", rightNow);
		}

		if (Integer.parseInt(ApplicationScreen.instance.getSaveTo()) == 1)
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
		} else if ((Integer.parseInt(ApplicationScreen.instance.getSaveTo()) == 2))
		{
			if (ApplicationScreen.instance.isSortByData())
			{
				saveDir = new File(ApplicationScreen.instance.getSaveToPath(), abcDir);
			} else {
				saveDir = new File(ApplicationScreen.instance.getSaveToPath());
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


	public void saveResultPicture(long sessionID)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
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

				File file = new File(saveDir, fileFormat);
				

				OutputStream os = null;
				
				try
				{
					os = new FileOutputStream(file);
				} catch (Exception e)
				{
					// save always if not working saving to sdcard
					e.printStackTrace();
					saveDir = getSaveDir(true);
					file = new File(saveDir, fileFormat);
					os = new FileOutputStream(file);
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
					switch ((orientation + 360) % 360)
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
				else
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

				if (writeOrientationTag)
				{
					values.put(ImageColumns.ORIENTATION, String.valueOf((Integer.parseInt(orientation_tag) + 360) % 360));
				} else
				{
					values.put(ImageColumns.ORIENTATION, String.valueOf(0));
				}

				values.put(ImageColumns.BUCKET_ID, path.hashCode());
				values.put(ImageColumns.BUCKET_DISPLAY_NAME, name);
				values.put(ImageColumns.DATA, file.getAbsolutePath());

				File tmpFile = file;
				tmpFile = file;
				

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
					String softwareString = ApplicationScreen.getAppResources().getString(R.string.app_name) + ", "
							+ tag_modename;
					ValueByteArray softwareValue = new ValueByteArray(ExifDriver.FORMAT_ASCII_STRINGS);
					softwareValue.setBytes(softwareString.getBytes());
					exifDriver.getIfd0().put(ExifDriver.TAG_SOFTWARE, softwareValue);

					ValueNumber value = new ValueNumber(ExifDriver.FORMAT_UNSIGNED_SHORT, exif_orientation);
					exifDriver.getIfd0().put(ExifDriver.TAG_ORIENTATION, value);

					// Save exif info to new file, and replace old file with new
					// one.
					File modifiedFile = new File(tmpFile.getAbsolutePath() + ".tmp");
					exifDriver.save(modifiedFile.getAbsolutePath());
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
					File rawFile = new File(Plugin.CAMERA_IMAGE_BUCKET_NAME);
				}

				ApplicationScreen.instance.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
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
		}
	}

	protected void addTimestamp(File file)
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

			int dateFormat = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampDate, "0"));
			boolean abbreviation = prefs.getBoolean(ApplicationScreen.sTimestampAbbreviation, false);
			int timeFormat = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampTime, "0"));
			int separator = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampSeparator, "0"));
			String customText = prefs.getString(ApplicationScreen.sTimestampCustomText, "");
			int color = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampColor, "1"));
			int fontSizeC = Integer.parseInt(prefs.getString(ApplicationScreen.sTimestampFontSize, "80"));

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

			final float scale = ApplicationScreen.instance.getResources().getDisplayMetrics().density;

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
		switch ((orientation + 360) % 360)
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

	protected void rotateImage(File file, Matrix matrix)
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

	protected void drawTextWithBackground(Canvas canvas, Paint paint, String text, int foreground, int background,
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

		final float scale = ApplicationScreen.instance.getResources().getDisplayMetrics().density;
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
			canvas.drawText(resText[0], imageWidth - 5*padding, imageHeight - 2 * textHeight, paint);
		}
		if (resText.length > 1)
		{
			canvas.drawText(resText[1], imageWidth - 5*padding, imageHeight - textHeight / 2, paint);
		}
	}

	public String getExportFileName(String modeName)
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		saveOption = Integer.parseInt(prefs.getString(ApplicationScreen.sExportNamePref, "2"));

		String prefix = prefs.getString(ApplicationScreen.sExportNamePrefixPref, "");
		if (!prefix.equals(""))
			prefix = prefix + "_";

		String postfix = prefs.getString(ApplicationScreen.sExportNamePostfixPref, "");
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
		if (CameraController.isUseHALv3()) {
			return getExportFileName(getActiveMode().modeSaveNameHAL);
		} else {
			return getExportFileName(getActiveMode().modeSaveName);
		}
	}

	protected void writeData(FileOutputStream os, boolean isYUV, Long SessionID, int i, byte[] buffer, int yuvBuffer,
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
				jpegQuality = Integer.parseInt(prefs.getString(ApplicationScreen.sJPEGQualityPref, "95"));

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

		ApplicationScreen.instance.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
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
