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

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.opengl.GLES10;
import android.opengl.GLU;
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
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Toast;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginCapture;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
import com.almalence.opencam_plus.ui.GUI;
import com.almalence.opencam_plus.ui.GUI.CameraParameter;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.ui.GUI;
import com.almalence.opencam.ui.GUI.CameraParameter;
//-+- -->

import com.almalence.util.ImageConversion;

import com.almalence.ui.Switch.Switch;
import com.almalence.SwapHeap;

/***
Implements night capture plugin - different capture logics available
***/

public class NightCapturePlugin extends PluginCapture
{
	public static final int HI_SPEED_FRAMES = 12;
	public static final int HI_RES_FRAMES = 8;
	public static final int MIN_MPIX_SUPPORTED = 1280*960;
	public static final int MIN_MPIX_PREVIEW = 600*400;
	
    private static Toast capturingDialog;
    public static boolean inCapture = false;

    //almashot - related
    public static int evValues[] = new int[HI_SPEED_FRAMES];
    public static int evIdx[] = new int[HI_SPEED_FRAMES];
    private int frame_num;
    public static float ev_step;
	private boolean takingAlready;
	private boolean aboutToTakePicture=false;
	private int nVFframesToBuffer;

	// shared between activities 
    public static int CapIdx;
    public static int total_frames;
    public static boolean wantLandscapePhoto = false;
    public static int compressed_frame[] = new int[HI_SPEED_FRAMES];
    public static int compressed_frame_len[] = new int[HI_SPEED_FRAMES];
    
    //Night vision variables
    private GLCameraPreview cameraPreview;
    public static byte[] currByte;
    private byte[] data1;
	private byte[] data2;
	private byte[] dataS;
	private byte[] dataRotated;
	
	int onDrawFrameCounter=1;
	int[] cameraTexture;
	byte[] glCameraFrame=new byte[256*256]; //size of a texture must be a power of 2
	FloatBuffer cubeBuff;
	FloatBuffer texBuff;
	
	byte[] yuv_data;
	byte[] rgb_data;
	
	float currHalfWidth;
	float currHalfHeight;
	
	int captureIndex = -1;
	int imgCaptureWidth = 0;
    int imgCaptureHeight = 0;
	
	float cameraDist;
    
    public static Bitmap sweet;
    public static int currLen;
    
    public static boolean FramesShot = false;
    
    //preferences
 	public static String ModePreference;	// 0=hi-res 1=hi-speed  
 	public static String FocusPreference;
 	public static boolean OpenGLPreference;
 	public static String ImageSizeIdxPreference;
 	
 	public static List<Long> ResolutionsMPixList;
	public static List<String> ResolutionsIdxesList;
	public static List<String> ResolutionsNamesList;
	
	public static List<Long> ResolutionsMPixListIC;
	public static List<String> ResolutionsIdxesListIC;
	public static List<String> ResolutionsNamesListIC;
	
	public static List<Long> ResolutionsMPixListVF;
	public static List<String> ResolutionsIdxesListVF;
	public static List<String> ResolutionsNamesListVF;
	
	private String preferenceSceneMode;
	private String preferenceFocusMode;
	private String preferenceFlashMode;
	
	private Switch modeSwitcher;

	public NightCapturePlugin()
	{
		super("com.almalence.plugins.nightcapture",
			  R.xml.preferences_capture_night,
			  R.xml.preferences_capture_night,
			  R.drawable.plugin_capture_night_nightvision_on,
			  "Night vision ON");
	}
		
	@Override
	public void onCreate()
	{
		cameraPreview = new GLCameraPreview(MainScreen.mainContext);
		
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();		
		modeSwitcher = (Switch)inflator.inflate(R.layout.plugin_capture_night_modeswitcher, null, false);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        ModePreference = prefs.getString("modePref", "1");
        modeSwitcher.setTextOn("Hi-Res");
        modeSwitcher.setTextOff("Hi-Speed");
        modeSwitcher.setChecked(ModePreference.compareTo("0") == 0 ? true : false);
		modeSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
				if (isChecked)				
					ModePreference = "0";		        	
				else			
					ModePreference = "1";
				
				SharedPreferences.Editor editor = prefs.edit();		        	
	        	editor.putString("modePref", ModePreference);
	        	editor.commit();
				
				Message msg = new Message();
				msg.what = PluginManager.MSG_RESTART_MAIN_SCREEN;				
				MainScreen.H.sendMessage(msg);
			}
		});
		
//		android.widget.RelativeLayout.LayoutParams lp = new android.widget.RelativeLayout.LayoutParams(
//				LayoutParams.WRAP_CONTENT,
//				LayoutParams.WRAP_CONTENT);
//		
//		modeSwitcher.setLayoutParams(lp);
		if(PluginManager.getInstance().getProcessingCounter() == 0)
			modeSwitcher.setEnabled(true);
	}
	
	@Override
	public void onStart()
	{
		getPrefs();
		
		if(OpenGLPreference)
		{
			quickControlIconID = R.drawable.plugin_capture_night_nightvision_on;
			quickControlTitle = "Night vision ON";
		}
		else
		{
			quickControlIconID = R.drawable.plugin_capture_night_nightvision_off;
			quickControlTitle = "Night vision OFF";
		}
	}
	
	@Override
	public void onResume()
	{
		takingAlready = false;
		inCapture = false;
        
        MainScreen.thiz.MuteShutter(false);
        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        preferenceSceneMode = prefs.getString("SceneModeValue", Camera.Parameters.SCENE_MODE_AUTO);
        preferenceFocusMode = prefs.getString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, Camera.Parameters.FOCUS_MODE_AUTO);
        preferenceFlashMode = prefs.getString("FlashModeValue", Camera.Parameters.FLASH_MODE_AUTO);
	}
	
	@Override
	public void onPause()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        prefs.edit().putString("SceneModeValue", preferenceSceneMode).commit();
        prefs.edit().putString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, preferenceFocusMode).commit();
        prefs.edit().putString("FlashModeValue", preferenceFlashMode).commit();
	}
	
	@Override
	public void onStop()
	{
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout3);
		for(int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for(int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int zoom_id = this.modeSwitcher.getId();
			if(view_id == zoom_id)
			{
				if(view.getParent() != null)
					((ViewGroup)view.getParent()).removeView(view);
				specialLayout.removeView(view);
			}
		}
	}

	@Override
	public void onExportFinished()
	{
		if(modeSwitcher != null && PluginManager.getInstance().getProcessingCounter() == 0 && inCapture == false)
			modeSwitcher.setEnabled(true);
	}
	
	@Override
	public void onGUICreate()
	{
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout3);
		for(int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for(int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int zoom_id = this.modeSwitcher.getId();
			if(view_id == zoom_id)
			{
				if(view.getParent() != null)
					((ViewGroup)view.getParent()).removeView(view);
				
				specialLayout.removeView(view);
			}
		}		
		
		MainScreen.thiz.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_SCENE, true, false);
		MainScreen.thiz.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FOCUS, true, false);
		MainScreen.thiz.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FLASH, true, true);
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout3)).addView(this.modeSwitcher, params);
		
		this.modeSwitcher.setLayoutParams(params);
		this.modeSwitcher.requestLayout();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout3)).requestLayout();
		
//		clearViews();
//		addView(modeSwitcher, ViewfinderZone.VIEWFINDER_ZONE_TOP_RIGHT);
	}
	
	@Override
	public boolean isGLSurfaceNeeded(){return true;}
	
	@Override
	public void onQuickControlClick()
	{
		Message msg = new Message();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		SharedPreferences.Editor editor = prefs.edit();		
    	
		if(quickControlIconID == R.drawable.plugin_capture_night_nightvision_on)
		{
			quickControlIconID = R.drawable.plugin_capture_night_nightvision_off;
			quickControlTitle = "Night vision OFF";
			
			editor.putBoolean("openglPref", false);
	    	editor.commit();
	    	
	    	OpenGLPreference = false;
	    	
	    	data1 = null;
	    	data2 = null;
	    	dataS = null;
	    	dataRotated = null;
	    	yuv_data = null;
	    	rgb_data = null;
	    	
	    	msg.what = PluginManager.MSG_OPENGL_LAYER_HIDE;
		}
		else if(quickControlIconID == R.drawable.plugin_capture_night_nightvision_off)
		{
			quickControlIconID = R.drawable.plugin_capture_night_nightvision_on;
			quickControlTitle = "Night vision ON";
			
			editor.putBoolean("openglPref", true);
	    	editor.commit();
	    	
	    	OpenGLPreference = true;
	    	
	    	msg.what = PluginManager.MSG_OPENGL_LAYER_SHOW;
		}
		
		MainScreen.H.sendMessage(msg);
	}
	
	@SuppressLint("CommitPrefEdits")
	private void getPrefs()
    {		
		String defaultMode = "1";		
		String defaultFocus = "0";
    	
        // Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);        
        ModePreference = prefs.getString("modePref", defaultMode);
        ImageSizeIdxPreference = prefs.getString(MainScreen.CameraIndex == 0? "imageSizePrefNightBack" : "imageSizePrefNightFront", "-1");
        FocusPreference = prefs.getString("focusPref", defaultFocus);
        OpenGLPreference = prefs.getBoolean("openglPref", true);
    }
	
	@Override
	public void onDefaultsSelect()
	{
		String defaultMode = "1";
		String defaultFocus = "0";
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);        
        ModePreference = prefs.getString("modePref", defaultMode);
		ImageSizeIdxPreference = prefs.getString(MainScreen.CameraIndex == 0? "imageSizePrefNightBack" : "imageSizePrefNightFront", "-1");
        FocusPreference = prefs.getString("focusPref", defaultFocus);
		SelectImageDimensionNight();
	}
	
	@Override
	public void onShowPreferences()
	{
		String defaultMode = "1";
		String defaultFocus = "0";
		
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);        
        ModePreference = prefs.getString("modePref", defaultMode);
		ImageSizeIdxPreference = prefs.getString("imageSizePrefNight", "-1");
        FocusPreference = prefs.getString("focusPref", defaultFocus);
		SelectImageDimension();		
	}
	
	private void SelectImageDimensionNight()
	{
		int mode = Integer.parseInt(ModePreference);

        PopulateCameraDimensions(1);
        ResolutionsMPixListVF = ResolutionsMPixList;
        ResolutionsIdxesListVF = ResolutionsIdxesList;
        ResolutionsNamesListVF = ResolutionsNamesList;
        
    	
    	long maxMem = Runtime.getRuntime().maxMemory() - Debug.getNativeHeapAllocatedSize();
    	long maxMpix = (maxMem - 1000000) / 3;	// 2 x Mpix - result, 1/4 x Mpix x 4 - compressed input jpegs, 1Mb - safe reserve

    	if (maxMpix < MIN_MPIX_SUPPORTED)
    	{
    		String msg;
    		msg = "MainScreen.SelectImageDimension maxMem = " + maxMem;
    		Log.e("NightCapturePlugin", "MainScreen.SelectImageDimension maxMpix < MIN_MPIX_SUPPORTED");
    		Log.e("NightCapturePlugin", msg);
    	}
    	
    	// find index selected in preferences
    	int prefIdx = -1;
    	try
    	{
    		if(mode == 1)
    			prefIdx = Integer.parseInt(NightCapturePlugin.ImageSizeIdxPreference);
    		else
    			prefIdx = Integer.parseInt(MainScreen.ImageSizeIdxPreference);
    	}
    	catch (IndexOutOfBoundsException e)
    	{
    		prefIdx = -1;
    	}

        // ----- Find max-resolution capture dimensions
    	Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
        Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
        if(cp == null)
        	Log.e("NightCapturePlugin", "MainScreen.SelectImageDimension MainScreen.thiz.getCameraParameters == null");
        List<Camera.Size> cs;
    	int MinMPIX = MIN_MPIX_SUPPORTED;
        if (mode == 1)	// super mode
        {
        	cs = RemoveDuplicateResolutions(cp.getSupportedPreviewSizes());
        	MinMPIX = MIN_MPIX_PREVIEW;
        }
        else
        	cs = cp.getSupportedPictureSizes();

        int Capture5mIdx = -1;
        long Capture5mMpix = 0;
        int Capture5mWidth = 0;
        int Capture5mHeight = 0;
        int CaptureIdx = -1;
        long CaptureMpix = 0;
        int CaptureWidth = 0;
        int CaptureHeight = 0;
		boolean prefFound = false;
        
		// figure default resolution
    	for (int ii=0; ii<cs.size(); ++ii)
    	{
            Size s = cs.get(ii); 
            long mpix = (long)s.width*s.height;
            
    		if ((mpix >= MinMPIX) && (mpix < maxMpix))
    		{
            	if (mpix > Capture5mMpix)
            	{
                    Capture5mIdx = ii;
            		Capture5mMpix = mpix;
            		Capture5mWidth = s.width;
            		Capture5mHeight = s.height;
            	}
    		}
    	}
    	
    	for (int ii=0; ii<cs.size(); ++ii)
    	{
            Size s = cs.get(ii); 
            long mpix = (long)s.width*s.height;
            
        	if ((ii==prefIdx) && (mpix >= MinMPIX))
        	{
        		prefFound = true;
                CaptureIdx = ii;
        		CaptureMpix = mpix;
        		CaptureWidth = s.width;
        		CaptureHeight = s.height;
        		break;
        	}
        	
        	if (mpix > CaptureMpix)
        	{
                CaptureIdx = ii;
        		CaptureMpix = mpix;
        		CaptureWidth = s.width;
        		CaptureHeight = s.height;
        	}
        }

    	// default to about 5Mpix if nothing is set in preferences or maximum resolution is above memory limits
    	if (Capture5mMpix>0)
    	{
	    	if (!prefFound)
	    	{
                CaptureIdx = Capture5mIdx;
	    		CaptureMpix = Capture5mMpix;
	    		CaptureWidth = Capture5mWidth;
	    		CaptureHeight = Capture5mHeight;
	    	}
    	}
    	
    	captureIndex = CaptureIdx;
    	imgCaptureWidth = CaptureWidth;
        imgCaptureHeight = CaptureHeight;
	}
	
	@Override
	public void SelectImageDimension()
    {
		SelectImageDimensionNight();
		setCameraImageSize();
    }
	
	private void setCameraImageSize()
	{
		if(imgCaptureWidth > 0 && imgCaptureHeight > 0)
		{
			int mode = Integer.parseInt(ModePreference);
			if(mode == 1)
	    	{
	    		CapIdx = captureIndex;
	    		MainScreen.setSaveImageWidth(imgCaptureWidth*2);
	        	MainScreen.setSaveImageHeight(imgCaptureHeight*2);
	    	}
	    	else
	    	{
	    	  	MainScreen.setSaveImageWidth(imgCaptureWidth);
	        	MainScreen.setSaveImageHeight(imgCaptureHeight);
	    	}
	    	
	    	MainScreen.setImageWidth(imgCaptureWidth);
	    	MainScreen.setImageHeight(imgCaptureHeight);
	        
	        String msg = "NightCapturePlugin.setCameraImageSize SX = " + MainScreen.getImageWidth() + " SY = " + MainScreen.getImageHeight();
	        Log.e("NightCapturePlugin", msg);
		}
	}
	
	@Override
	public void SetCameraPreviewSize(Camera.Parameters cp)
	{		
		// for super mode
        nVFframesToBuffer = 0;
        
		int mode = Integer.parseInt(ModePreference);
		
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
    	if(cp == null)
        	Log.e("NightCapturePlugin", "MainScreen.setupCamera MainScreen.thiz.getCameraParameters returned null!");
    	List<Camera.Size> cs = cp.getSupportedPreviewSizes();
    	
    	if (mode == 1)		// hi-speed mode - set exact preview size as selected by user
    	{    		
    		Size s = camera.new Size(-1, -1);
    		s.width = MainScreen.getImageWidth();
    		s.height = MainScreen.getImageHeight();
    		if(cs.contains(s))
    			cp.setPreviewSize(MainScreen.getImageWidth(), MainScreen.getImageHeight());
    		else
    		{
    			Size os = getOptimalPreviewSize(cs, MainScreen.getImageWidth(), MainScreen.getImageHeight());
    	    	cp.setPreviewSize(os.width, os.height);
    		}
    	}
    	else
    	{
	    	Size os = getOptimalPreviewSize(cs, MainScreen.getImageWidth(), MainScreen.getImageHeight());
	    	cp.setPreviewSize(os.width, os.height);
    	}
    	
    	if(FocusPreference.compareTo("0") == 0 && !MainScreen.thiz.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED))
        {
        	FocusPreference = "1";
        	
        	// Get the xml/preferences.xml preferences
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        	SharedPreferences.Editor editor = prefs.edit();        	
        	editor.putString("focusPref", "1");
        	editor.commit();
        }
    	
    	MainScreen.thiz.setCameraParameters(cp);
	}
    
	@Override
	public void SetCameraPictureSize()
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
		if (Integer.parseInt(ModePreference) != 1)
		{
			cp.setPictureSize(MainScreen.getImageWidth(), MainScreen.getImageHeight());
			cp.setJpegQuality(100);
		}
        
		try
        {
			MainScreen.thiz.setCameraParameters(cp);
		}
		catch(RuntimeException e)
	    {
	    	Log.e("NightCapturePlugin", "MainScreen.setupCamera unable setParameters "+e.getMessage());	
	    }
		
		cp = MainScreen.thiz.getCameraParameters();
		List<String> sceneModes = cp.getSupportedSceneModes();
		if(sceneModes != null && sceneModes.contains(Camera.Parameters.SCENE_MODE_NIGHT))
		{
			cp.setSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
			MainScreen.thiz.setCameraSceneMode(Camera.Parameters.SCENE_MODE_NIGHT);
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
	    	SharedPreferences.Editor editor = prefs.edit();        	
	    	editor.putString("SceneModeValue", Camera.Parameters.SCENE_MODE_NIGHT);
	    	editor.commit();
		}
        
        try
        {        	
	        if (FocusPreference.compareTo("0") == 0)
	        {
	        	if (cp.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_FIXED))
	        		cp.setFocusMode(Camera.Parameters.FOCUS_MODE_FIXED);	// should set to hyperfocal distance as per android doc
		        else if(cp.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO))
		        	cp.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
	        }
	        else if(cp.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_AUTO))
	        	cp.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO);
	        
	        String sUserFocusMode = cp.getFocusMode();
			//MainScreen.thiz.setCameraParameters(cp);
	        MainScreen.thiz.setCameraFocusMode(sUserFocusMode);
			cp = MainScreen.thiz.getCameraParameters();			
			String sSystemFocusMode = cp.getFocusMode();			
			
			if(sUserFocusMode.compareTo(sSystemFocusMode) != 0)
			{
		    	Log.i("NightCapturePlugin", "setFocusMode didn't worked in Night Scene, reverting to Auto Scene");
		    	if(cp.getSupportedSceneModes().contains(Camera.Parameters.SCENE_MODE_AUTO))
		    		MainScreen.thiz.setCameraSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
				MainScreen.thiz.setCameraFocusMode(sUserFocusMode);
			}
			
			PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, sUserFocusMode).commit();
			PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putString("SceneModeValue", cp.getSceneMode()).commit();
	    	
	    	Log.i("NightCapturePlugin", "MainScreen.setupCamera setFocusMode success");
	    }
	    catch(RuntimeException e)
	    {
	    	Log.e("NightCapturePlugin", "MainScreen.setupCamera unable to setFocusMode");
	    }
        
        try
        {
			if (cp.getSupportedFlashModes() != null)
	        {				
				//On some models, as Nexus 4, flash mode can't be controlled by user in scene mode NIGHT
				//Device automatically set flash mode to 'auto'
				//For such cases we change scene mode to AUTO and set flash mode to FLASH_MODE_OFF in free version				
				cp = MainScreen.thiz.getCameraParameters();
				String sSystemFlashMode = cp.getFlashMode();
				
				
				if(Camera.Parameters.FLASH_MODE_OFF.compareTo(sSystemFlashMode) != 0)
				{
					cp.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
					cp.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
					MainScreen.thiz.setCameraParameters(cp);
				}
				
				PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putString("FlashModeValue", Camera.Parameters.FLASH_MODE_OFF).commit();
	        }
        }
        catch(RuntimeException e)
        {
        	Log.e("CameraTest", "MainScreen.setupCamera unable to setFlashMode");	
        }
        
        Message msg = new Message();
		if(OpenGLPreference)
			msg.what = PluginManager.MSG_OPENGL_LAYER_SHOW;
		else
			msg.what = PluginManager.MSG_OPENGL_LAYER_HIDE;
		MainScreen.H.sendMessage(msg);
	}
	
	// leave only top-most resolution for each aspect ratio for super mode
    private static List<Camera.Size> RemoveDuplicateResolutions(List<Camera.Size> cs)
    {
    	List<Long> MPix = new ArrayList<Long>();
    	List<Integer> RatIdx = new ArrayList<Integer>();
    	long ri_max_mpix[] = new long [4];
    	
    	for (int i=0; i<4; ++i)
    		ri_max_mpix[i] = 0;
    	
    	for (int ii=0; ii<cs.size(); ++ii)
    	{
            Size s = cs.get(ii); 
    	
        	Long lmpix = (long)s.width*s.height;
        	float ratio = (float)s.width/s.height;

        	int ri = 0;
            if (Math.abs(ratio - 4/3.f)  < 0.1f) ri = 1;
            if (Math.abs(ratio - 3/2.f)  < 0.12f) ri = 2;
            if (Math.abs(ratio - 16/9.f) < 0.15f) ri = 3;

            if (lmpix > ri_max_mpix[ri])
            	ri_max_mpix[ri] = lmpix;
            
        	MPix.add(lmpix);
        	RatIdx.add(ri);
    	}
    	
    	// remove lower-than-maximum resolutions
    	for (int ii=0; ii<cs.size();)
    	{
            Size s = cs.get(ii); 
        	
        	Long lmpix = (long)s.width*s.height;
        	float ratio = (float)s.width/s.height;

        	int ri = 0;
            if (Math.abs(ratio - 4/3.f)  < 0.1f) ri = 1;
            if (Math.abs(ratio - 3/2.f)  < 0.12f) ri = 2;
            if (Math.abs(ratio - 16/9.f) < 0.15f) ri = 3;

            if (lmpix < ri_max_mpix[ri])
            	cs.remove(ii);
            else
            	++ii;
    	}
    	
    	return cs;
    }
	
	public static void PopulateCameraDimensions(int mode)
    {
		ResolutionsMPixList = new ArrayList<Long>();
    	ResolutionsIdxesList = new ArrayList<String>();
    	ResolutionsNamesList = new ArrayList<String>();

    	Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
    	Camera.Parameters cp = MainScreen.thiz.getCameraParameters();        

        List<Camera.Size> cs;
    	int MinMPIX = MIN_MPIX_SUPPORTED;
        if (mode == 1)	// hi-speed mode
        {
        	// hi-speed mode: leave only single top resolution for each aspect ratio
        	cs = RemoveDuplicateResolutions(cp.getSupportedPreviewSizes());
        	MinMPIX = MIN_MPIX_PREVIEW;
        }
        else
        {
        	cs = cp.getSupportedPictureSizes();
        }

        CharSequence[] RatioStrings = {" ", "4:3", "3:2", "16:9", "1:1"};
        
    	for (int ii=0; ii<cs.size(); ++ii)
    	{
            Size s = cs.get(ii); 

            if ((long)s.width*s.height < MinMPIX)
            	continue;

            // superzoom supports 12mpix output at most
            if ((mode == 1) && ((s.width > 4096/2) || (s.height > 3072/2)))
            	continue;

        	Long lmpix = (long)s.width*s.height;
        	float mpix = (float)lmpix/1000000.f;
        	float ratio = (float)s.width/s.height;

        	// find good location in a list
        	int loc;
        	for (loc=0; loc<ResolutionsMPixList.size(); ++loc)
        		if (ResolutionsMPixList.get(loc) < lmpix)
        			break;
        	
        	int ri = 0;
            if (Math.abs(ratio - 4/3.f)  < 0.1f) ri = 1;
            if (Math.abs(ratio - 3/2.f)  < 0.12f) ri = 2;
            if (Math.abs(ratio - 16/9.f) < 0.15f) ri = 3;
            if (Math.abs(ratio - 1)  == 0) ri = 4;

            if (mode == 1)	// hi-speed mode
            	mpix *= 4;
            
        	ResolutionsNamesList.add(loc, String.format("%3.1f Mpix  " + RatioStrings[ri], mpix));
        	ResolutionsIdxesList.add(loc, String.format("%d", ii));
        	ResolutionsMPixList.add(loc, lmpix);
        }
    	
		return;
    }
	
	@Override
	public void onCameraParametersSetup()
	{
        //int mode = Integer.parseInt(ModePreference);
        PopulateCameraDimensions(1);
        ResolutionsMPixListVF = ResolutionsMPixList;
        ResolutionsIdxesListVF = ResolutionsIdxesList;
        ResolutionsNamesListVF = ResolutionsNamesList;
	}
	
	@Override
	public void onPreferenceCreate(PreferenceActivity prefActivity)
	{
		final PreferenceActivity mPref = prefActivity;
    	
		CharSequence[] entries;
		CharSequence[] entryValues;
		
		if (ResolutionsIdxesList != null)
        {
	        entries = ResolutionsNamesList.toArray(new CharSequence[ResolutionsNamesList.size()]);
	        entryValues = ResolutionsIdxesList.toArray(new CharSequence[ResolutionsIdxesList.size()]);

	        PreferenceCategory cat = (PreferenceCategory)prefActivity.findPreference("Pref_NightCapture_Category");
	        ListPreference lp = (ListPreference)prefActivity.findPreference("imageSizePrefNightBack");
	        ListPreference lp2 = (ListPreference)prefActivity.findPreference("imageSizePrefNightFront");
	        
	        if(MainScreen.CameraIndex == 0 && lp2 != null)
	        	cat.removePreference(lp2);
	        else if(lp != null && lp2 != null)
	        {
	        	cat.removePreference(lp);
	        	lp = lp2;
	        }
	        if(lp != null)
	        {
		        lp.setEntries(entries);
		        lp.setEntryValues(entryValues);
		        
		        // set currently selected image size
				int idx;
				for (idx = 0; idx < ResolutionsIdxesList.size(); ++idx)
				{
					if (Integer.parseInt(ResolutionsIdxesList.get(idx)) == CapIdx)
					{
						break;
					}
				}
				if (idx < ResolutionsIdxesList.size())
				{
					lp.setValueIndex(idx);
//				}
//				else 
//				{
//					idx=0;
//					lp.setValueIndex(idx);
//				}
					lp.setSummary(entries[idx]);
			        lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			        {
						//@Override
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							int value = Integer.parseInt(newValue.toString());
							CapIdx = value;
			                return true;
						}
			        });
				}
	        }
        }
		
		ListPreference fp = (ListPreference) (ListPreference)prefActivity.findPreference("focusPref");
		if(fp != null)
		{
	        fp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
	        {
	            public boolean onPreferenceChange(Preference preference, Object focus_new)
	            {
	            	int new_value = Integer.parseInt(focus_new.toString());
	          
	            	if ((new_value == 0) && MainScreen.supportedFocusModes != null && !MainScreen.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED))
	            	{
	            		new AlertDialog.Builder(mPref)
	        			.setIcon(R.drawable.gui_almalence_alert_dialog_icon)
	        			.setTitle(R.string.Pref_NightCapture_FocusModeAlert_Title)
	        			.setMessage(R.string.Pref_NightCapture_FocusModeAlert_Msg)
	        			.setPositiveButton(android.R.string.ok, null)
	        			.create().show();
	
		                ((ListPreference)preference).setValue("1");
		                
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
    	
		CharSequence[] entries;
		CharSequence[] entryValues;
		
		if (ResolutionsIdxesList != null)
        {
	        entries = ResolutionsNamesList.toArray(new CharSequence[ResolutionsNamesList.size()]);
	        entryValues = ResolutionsIdxesList.toArray(new CharSequence[ResolutionsIdxesList.size()]);

	        PreferenceCategory cat = (PreferenceCategory)prefActivity.findPreference("Pref_NightCapture_Category");
	        ListPreference lp = (ListPreference)prefActivity.findPreference("imageSizePrefNightBack");
	        ListPreference lp2 = (ListPreference)prefActivity.findPreference("imageSizePrefNightFront");
	        
	        if(MainScreen.CameraIndex == 0 && lp2 != null)
	        	cat.removePreference(lp2);
	        else if(lp != null && lp2 != null)
	        {
	        	cat.removePreference(lp);
	        	lp = lp2;
	        }
	        if(lp != null)
	        {
		        lp.setEntries(entries);
		        lp.setEntryValues(entryValues);
		        
		        // set currently selected image size
				int idx;
				for (idx = 0; idx < ResolutionsIdxesList.size(); ++idx)
				{
					if (Integer.parseInt(ResolutionsIdxesList.get(idx)) == CapIdx)
					{
						break;
					}
				}
				if (idx < ResolutionsIdxesList.size())
				{
					lp.setValueIndex(idx);
//				}
//				else 
//				{
//					idx=0;
//					lp.setValueIndex(idx);
//				}
					lp.setSummary(entries[idx]);
			        lp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
			        {
						//@Override
						public boolean onPreferenceChange(Preference preference, Object newValue)
						{
							int value = Integer.parseInt(newValue.toString());
							CapIdx = value;
			                return true;
						}
			        });
				}
	        }
        }
		
		ListPreference fp = (ListPreference) (ListPreference)prefActivity.findPreference("focusPref");
		if(fp != null)
		{
	        fp.setOnPreferenceChangeListener(new OnPreferenceChangeListener()
	        {
	            public boolean onPreferenceChange(Preference preference, Object focus_new)
	            {
	            	int new_value = Integer.parseInt(focus_new.toString());
	          
	            	if ((new_value == 0) && MainScreen.supportedFocusModes != null && !MainScreen.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_FIXED))
	            	{
	            		new AlertDialog.Builder(mPref.getActivity())
	        			.setIcon(R.drawable.gui_almalence_alert_dialog_icon)
	        			.setTitle(R.string.Pref_NightCapture_FocusModeAlert_Title)
	        			.setMessage(R.string.Pref_NightCapture_FocusModeAlert_Msg)
	        			.setPositiveButton(android.R.string.ok, null)
	        			.create().show();
	
		                ((ListPreference)preference).setValue("1");
		                
		                return false;
	            	}
	
	                return true;
	            }
	        });
		}
	}
	
	public boolean delayedCaptureSupported(){return true;}
	
	@Override
	public void OnShutterClick()
	{
		if (takingAlready == false)
			startCaptureSequence();
	}
	
	private void startCaptureSequence()
	{
		MainScreen.thiz.MuteShutter(true);
		
		if (inCapture == false)
        {
			Date curDate = new Date();
			SessionID = curDate.getTime();
			
			int mode = Integer.parseInt(ModePreference);
			if(mode == 1)
	    	{
				MainScreen.setSaveImageWidth(imgCaptureWidth*2);
				MainScreen.setSaveImageHeight(imgCaptureHeight*2);
//	        	PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(SessionID), String.valueOf(imgCaptureWidth*2));
//	        	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(SessionID), String.valueOf(imgCaptureHeight*2));
	    	}
	    	else
	    	{
	    		MainScreen.setSaveImageWidth(imgCaptureWidth);
				MainScreen.setSaveImageHeight(imgCaptureHeight);
//	        	PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(SessionID), String.valueOf(imgCaptureWidth));
//	        	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(SessionID), String.valueOf(imgCaptureHeight));
	    	}

			
    		inCapture = true;
    		takingAlready = false;
    		this.modeSwitcher.setEnabled(false);
    		
    		LinearLayout bottom_layout = (LinearLayout)MainScreen.thiz.findViewById(R.id.mainButtons);
    		
        	capturingDialog = Toast.makeText(MainScreen.thiz, R.string.hold_still, Toast.LENGTH_SHORT);
        	capturingDialog.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, bottom_layout.getHeight());
        	capturingDialog.show();
    		
            // reiniting for every shutter press 
            frame_num = 0;
    		total_frames = HI_RES_FRAMES;
    		
    		//Log.e("NIGHT CAMERA DEBUG", "startCaptureSequence session = " + PluginManager.getInstance().getSessionID());
            PluginManager.getInstance().addToSharedMem("nightmode"+String.valueOf(SessionID), ModePreference);

            Camera camera = MainScreen.thiz.getCamera();
        	if (null==camera)
        		return;
            if (FocusPreference.compareTo("0") == 0)
            {	// if FOCUS_MODE_FIXED
	        	if (!takingAlready)
	        	{
		        	CaptureFrame(camera);
		    		takingAlready = true;
	        	}
            }
            else
            {
            	String fm = MainScreen.thiz.getFocusMode();
        		if(takingAlready == false && (MainScreen.getFocusState() == MainScreen.FOCUS_STATE_IDLE ||
        				MainScreen.getFocusState() == MainScreen.FOCUS_STATE_FOCUSING)
        				&& fm != null
        				&& !(fm.equals(Parameters.FOCUS_MODE_INFINITY)
           				|| fm.equals(Parameters.FOCUS_MODE_FIXED)
           				|| fm.equals(Parameters.FOCUS_MODE_EDOF)
           				|| fm.equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
       	    			|| fm.equals(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
       	    			&& !MainScreen.getAutoFocusLock())
        			aboutToTakePicture = true;
        		else if(takingAlready == false || (fm != null && (fm.equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
        				|| fm.equals(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))))
        		{
        			CaptureFrame(camera);
                	takingAlready = true;
        		}
        		else
        		{
        			inCapture = false;
        			
        			Message msg = new Message();
        			msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
        			msg.what = PluginManager.MSG_BROADCAST;
        			MainScreen.H.sendMessage(msg);
        			
        			MainScreen.guiManager.lockControls = false;
        		}
            }          
        }
	}
	
	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
    	compressed_frame[frame_num] = SwapHeap.SwapToHeap(paramArrayOfByte);
    	compressed_frame_len[frame_num] = paramArrayOfByte.length;
    	
    	PluginManager.getInstance().addToSharedMem("frame"+(frame_num+1)+String.valueOf(SessionID), String.valueOf(compressed_frame[frame_num]));
    	PluginManager.getInstance().addToSharedMem("framelen"+(frame_num+1)+String.valueOf(SessionID), String.valueOf(compressed_frame_len[frame_num]));
    	
    	PluginManager.getInstance().addToSharedMem("frameorientation"+ (frame_num+1) + String.valueOf(SessionID), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
    	PluginManager.getInstance().addToSharedMem("framemirrored" + (frame_num+1) + String.valueOf(SessionID), String.valueOf(MainScreen.getCameraMirrored()));
    	PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(SessionID), String.valueOf(frame_num+1));
    	
    	if(frame_num == 0)
    		PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte, SessionID);
    	
    	String message = MainScreen.thiz.getResources().getString(R.string.capturing);
		message += " ";
		message += frame_num+1 + "/";
		message +=  total_frames;
		capturingDialog.setText(message);
		capturingDialog.show();
		
		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_NEXT_FRAME;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}
	
	public void CaptureFrame(Camera paramCamera)
    {
		if (Integer.parseInt(ModePreference) == 1)	// hi-speed mode
    	{
    		nVFframesToBuffer = HI_SPEED_FRAMES;
    		// play tick sound
    		MainScreen.guiManager.startContinuousCaptureIndication();
    		MainScreen.thiz.PlayShutter();
    		return;
    	}

    	if (Integer.parseInt(ModePreference) == 0)	// hi-res mode
    	{
	    	try
	    	{
	    		// play tick sound
	    		MainScreen.guiManager.showCaptureIndication();
        		MainScreen.thiz.PlayShutter();
        		paramCamera.setPreviewCallback(null);
	    		paramCamera.takePicture(null, null, null, MainScreen.thiz);
	    	}
	    	catch (RuntimeException e)
	    	{
	    		Log.e("CameraTest", "takePicture fail in CaptureFrame (called after release?)");
	    	}
    		return;
    	}
    }
	
	// onPreviewFrame is used only to provide an exact delay between setExposure and takePicture
    // or to collect frames in super mode
	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera)
	{
		if(OpenGLPreference)
		{			
			if(data1 == null)		
				data1 = data;
			else if(data2 == null)
			{			
				data2 = data;
				
				if (dataS == null)
					dataS = new byte[data2.length];
				else if (dataS.length<data2.length)
					dataS = new byte[data2.length];
				
				Camera.Parameters params = MainScreen.thiz.getCameraParameters();			
				int imageWidth = params.getPreviewSize().width;
				int imageHeight = params.getPreviewSize().height;
				
				ImageConversion.sumByteArraysNV21(data1,data2,dataS,imageWidth,imageHeight);
				if(MainScreen.getCameraMirrored())
				{
					dataRotated = new byte[dataS.length];
					ImageConversion.TransformNV21(dataS, dataRotated, imageWidth, imageHeight, 1, 0, 0);
					
					yuv_data = dataRotated;
				}
				else
				 yuv_data = dataS;
				
				data1 = data2;
				data2 = null;
			}
		}
				
		if (Integer.parseInt(ModePreference) == 1)
		{
			if (nVFframesToBuffer != 0)
			{				
				if(MainScreen.getCameraMirrored())
				{
					Camera.Parameters params = MainScreen.thiz.getCameraParameters();			
					int imageWidth = params.getPreviewSize().width;
					int imageHeight = params.getPreviewSize().height;
					
					byte[] dataRotated = new byte[data.length];
					ImageConversion.TransformNV21(data, dataRotated, imageWidth, imageHeight, 1, 0, 0);
					
					data = dataRotated;					
				}	
				System.gc();
				
				// swap-out frame data to the heap				
		    	compressed_frame[HI_SPEED_FRAMES-nVFframesToBuffer] = SwapHeap.SwapToHeap(data);
		    	compressed_frame_len[HI_SPEED_FRAMES-nVFframesToBuffer] = data.length;
		    	
		    	PluginManager.getInstance().addToSharedMem("frame"+(frame_num+1)+String.valueOf(SessionID), String.valueOf(compressed_frame[frame_num]));
		    	PluginManager.getInstance().addToSharedMem("framelen"+(frame_num+1)+String.valueOf(SessionID), String.valueOf(compressed_frame_len[frame_num]));
		    	
		    	PluginManager.getInstance().addToSharedMem("frameorientation"+ (frame_num+1) + String.valueOf(SessionID), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
		    	PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(SessionID), String.valueOf(frame_num+1));
				
		    	if(frame_num == 0)
		    		PluginManager.getInstance().addToSharedMem_ExifTagsFromCamera(SessionID);
		    	
		    	++frame_num;
				--nVFframesToBuffer;
				
				// all frames captured - initiate processing
				if (nVFframesToBuffer == 0)
				{
					// play tick sound
	        		MainScreen.thiz.PlayShutter();
										
	        		Message message = new Message();
	        		message.obj = String.valueOf(SessionID);
	    			message.what = PluginManager.MSG_CAPTURE_FINISHED;
	    			MainScreen.H.sendMessage(message);
	    			
					MainScreen.guiManager.stopCaptureIndication();
					
					takingAlready = false;
					inCapture = false;
					//modeSwitcher.setEnabled(true);
				}
			}
			
		}		
	}
	
/******************************************************************************************************
	OpenGL layer functions
******************************************************************************************************/
	@Override
	public void onGLSurfaceCreated(GL10 gl, EGLConfig config)
	{		
		cameraPreview.generateGLTexture(gl);
		gl.glEnable(GL10.GL_TEXTURE_2D);            //Enable Texture Mapping ( NEW )		
	    gl.glShadeModel(GL10.GL_SMOOTH);            //Enable Smooth Shading
	    gl.glLineWidth(4.0f);
	    gl.glClearColor(0.0f, 0.0f, 0.0f, 0.5f);    //Black Background	
	    gl.glClearDepthf(1.0f);                     //Depth Buffer Setup	
	    gl.glEnable(GL10.GL_DEPTH_TEST);            //Enables Depth Testing	
	    gl.glDepthFunc(GL10.GL_LEQUAL);             //The Type Of Depth Testing To Do
	    
	    gl.glBlendFunc(GL10.GL_SRC_ALPHA, GLES10.GL_ONE);
		
		//Really Nice Perspective Calculations
		gl.glHint(GL10.GL_PERSPECTIVE_CORRECTION_HINT, GL10.GL_NICEST);
	}
	
	@Override
	public void onGLSurfaceChanged(GL10 gl, int width, int height)
	{
		if(height == 0) { 						//Prevent A Divide By Zero By
			height = 1; 						//Making Height Equal One
		}
		
		currHalfWidth = width/2;
		currHalfHeight = height/2;

		cameraDist = (float)(currHalfHeight / Math.tan(Math.toRadians(45.0f / 2.0f)));

		gl.glViewport(0, 0, width, height); 	//Reset The Current Viewport
		gl.glMatrixMode(GL10.GL_PROJECTION); 	//Select The Projection Matrix
		gl.glLoadIdentity(); 					//Reset The Projection Matrix

		//Calculate The Aspect Ratio Of The Window
		GLU.gluPerspective(gl, 45.0f, (float)width / (float)height, cameraDist / 10.0f, cameraDist * 10.0f);

		gl.glMatrixMode(GL10.GL_MODELVIEW); 	//Select The Modelview Matrix
		gl.glLoadIdentity(); 					//Reset The Modelview Matrix
		
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		Camera.Parameters params = MainScreen.thiz.getCameraParameters();
		if(params == null)
		{
			Log.e("NIGHT CAMERA DEBUG", "GLLayer.onSurfaceChanged params = null");
			return;
		}
		
		cameraPreview.setSurfaceSize(width, height);
	}
	
	@Override
	public void onGLDrawFrame(GL10 gl)
	{
		//Clear Screen And Depth Buffer
		gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);	
		gl.glLoadIdentity();
		
		//Drawing
		gl.glTranslatef(0.0f, 0.0f, -(cameraDist));		//Move 5 units into the screen
		gl.glRotatef(-90, 0.0f, 0.0f, 1.0f);	//Z
		
		if(OpenGLPreference && !inCapture)
			synchronized(this)
			{
				try {
					cameraPreview.draw(gl, yuv_data, MainScreen.mainContext);		//Draw the square
				} catch (RuntimeException e) {
					Log.e("onGLDrawFrame", "onGLDrawFrame in Night some exception" + e.getMessage());
				}
			}
	}
/******************************************************************************************************
	End of OpenGL layer functions
******************************************************************************************************/

	@Override
	public void onAutoFocus(boolean paramBoolean, Camera paramCamera)
    {
        if (inCapture) // disregard autofocus success (paramBoolean)
        {
        	if(aboutToTakePicture == true)
        	{
    			CaptureFrame(paramCamera);
    			takingAlready = true;
        	}
        	
//        	if(aboutToTakePicture == true && paramBoolean == true)
//        	{
//    			CaptureFrame(paramCamera);
//    			takingAlready = true;
//        	}
//    		else if(aboutToTakePicture == true)
//    		{
//    			modeSwitcher.setEnabled(true);
//        		inCapture = false;
//        		takingAlready = false;
//    			MainScreen.guiManager.lockControls = false;
//    		}
        	
        	aboutToTakePicture = false;
        }
    }
	
	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_NEXT_FRAME)
		{
			Camera camera = MainScreen.thiz.getCamera();
    		if (camera != null)
    		{
    			camera.startPreview();
	            if (++frame_num < total_frames)
	            {
	            	// re-open preview (closed once frame is captured)
					try
					{
						//MainScreen.camera.startPreview();
						// remaining frames
		            	if (FocusPreference.compareTo("2") == 0 && !MainScreen.getAutoFocusLock())
		            	{
		            		takingAlready = false;
		            		aboutToTakePicture = true;
		                    camera.autoFocus(MainScreen.thiz);
		            	}
		            	else
		            	{
		            		CaptureFrame(camera);
		            	}
					}
					catch (RuntimeException e)
					{
			    		Log.i("NightCapture plugin", "RuntimeException in MSG_NEXT_FRAME");
						// motorola's sometimes fail to restart preview after onPictureTaken (fixed),
						// especially on night scene
						// just repost our request and try once more (takePicture latency issues?)
						--frame_num;
						Message msg = new Message();
						msg.arg1 = PluginManager.MSG_NEXT_FRAME;
						msg.what = PluginManager.MSG_BROADCAST;
						MainScreen.H.sendMessage(msg);
					}
	            }
	            else
	            {
	            	//modeSwitcher.setEnabled(true);
	            		            	
	            	Message message = new Message();
	            	message.obj = String.valueOf(SessionID);
	    			message.what = PluginManager.MSG_CAPTURE_FINISHED;
	    			MainScreen.H.sendMessage(message);
	            	
	            	takingAlready = false;
	            	inCapture = false;
	            }
    		}
    		return true;
		}
		return false;
	}
}
