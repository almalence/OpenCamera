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


import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.Size;
import android.media.Image;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

import com.almalence.asynctaskmanager.Task;

/***
Abstract class for plugins
***/

public abstract class Plugin 
{
	//unique plugin id
	public String ID;
	//preferences file name - file distributed with plugin - analog of preference.xml - should be also with unique name.
	private int prefName = 0;
	
	//name of advanced preference file.
	//each plugin can shot it's preference by type + can add advanced preferences for expert users
	private int advancedPrefName = 0;
	
	//indicates if plugin has preferences and want to show it. Default - false (no settings to show)
	// plugin should set this to true if want to show preferences
	protected boolean isShowPreferences=false;
	
	//ID of icon and title for creating quick control button
	//Each plugin may have only one quick control button
	public int quickControlIconID = -1;
	public String quickControlTitle = "";	 
	
	protected static final String TIME_STAMP_NAME = "'IMG'_yyyyMMdd_HHmmss";

	protected long SessionID=0;
	
	public enum ViewfinderZone {
		
		//6 zones of ordinary controls
		VIEWFINDER_ZONE_TOP_LEFT,
		VIEWFINDER_ZONE_TOP_RIGHT,		
		VIEWFINDER_ZONE_CENTER_RIGHT,
		VIEWFINDER_ZONE_BOTTOM_RIGHT,
		VIEWFINDER_ZONE_BOTTOM_LEFT,
		VIEWFINDER_ZONE_CENTER_LEFT,
		
		//Center screen controls
		VIEWFINDER_ZONE_FULLSCREEN,
		VIEWFINDER_ZONE_CENTER;
		
		public static int getInt(ViewfinderZone value)
		{
			switch(value)
			{
			case VIEWFINDER_ZONE_TOP_LEFT:
				return 0;
			case VIEWFINDER_ZONE_TOP_RIGHT:
				return 1;
			case VIEWFINDER_ZONE_CENTER_RIGHT:
				return 2;
			case VIEWFINDER_ZONE_BOTTOM_RIGHT:
				return 3;
			case VIEWFINDER_ZONE_BOTTOM_LEFT:
				return 4;
			case VIEWFINDER_ZONE_CENTER_LEFT:
				return 5;
				
			default: return 7;
			}
			
		}
	}
	
	//Views (generally a Buttons) for GUI
	protected Map<View, ViewfinderZone> pluginViews;
	
	//Informational views
	protected List<View> infoViews;
	
	//Postprocessing view. Actually a layout
	protected View postProcessingView = null;
	
	public Plugin(String sID, int preferenceID, int advancedPreferenceID, int quickControlID, String quickControlInitTitle)
	{
		ID=sID;
		
		if(preferenceID != 0)
		{
			setPreferenceName(preferenceID);		
			isShowPreferences=true;
		}
		
		setAdvancedPreferenceName(advancedPreferenceID);
		//for dynamic preference generation. Header text and preferences xml
		//title = preferenceTitle;
		//summary = preferenceSummary;
		
		quickControlIconID = quickControlID;
		quickControlTitle = quickControlInitTitle;
		
		pluginViews = new Hashtable<View, ViewfinderZone>();
		infoViews = new ArrayList<View>();
	}
	
	//base onCreate stage;
	public void onCreate(){}
		
	//base onStart stage;
	public void onStart(){}
	public void onStartProcessing(long SessionID){}
	public void onStartPostProcessing(){}
	
	//base onStop stage;
	public void onStop(){}
    
    //base onDestroy stage
	public void onDestroy(){}

    //base onResume stage
	public void onResume(){}

    //base onPause stage
	public void onPause(){}
	
	//base onGUIStart stage
	public void onGUICreate(){}

	public void onShowPreferences(){}
	
	public void OnShutterClick(){}
	
	public void OnFocusButtonClick(){}
	
	public boolean onTouch(View view, MotionEvent e){return false;}
	
	public void onOrientationChanged(int orientation){}
	
	public boolean onKeyDown(int keyCode, KeyEvent event){return false;}
	
/******************************************************************************************************
	VF/Capture Interfaces
******************************************************************************************************/
	public void onAutoFocus(boolean paramBoolean, Camera paramCamera){}
	
	public void takePicture(){}
	
	public void onShutter(){};
	
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera){}
	
	public void onImageAvailable(Image im){}

	public void onPreviewFrame(byte[] data, Camera paramCamera){}
	
	private int MIN_MPIX_SUPPORTED = 1280 * 960;
	
//	@TargetApi(9)
//	protected void openCameraFrontOrRear()
//	{
//		if (Camera.getNumberOfCameras() > 0) {
//			MainScreen.thiz.setCamera(Camera.open(CameraController.CameraIndex));
//		}
//
//		Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//		Camera.getCameraInfo(CameraController.CameraIndex,
//				cameraInfo);
//
//		if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
//			MainScreen.setCameraMirrored(true);
//		else
//			MainScreen.setCameraMirrored(false);		
//	}
	
	public void SelectImageDimension()
	{
		// ----- Figure how much memory do we have and possibly limit resolution
		long maxMem = Runtime.getRuntime().maxMemory();// -
														// Debug.getNativeHeapAllocatedSize();
		long maxMpix = (maxMem - 1000000) / 3; // 2 x Mpix - result, 1/4 x Mpix
												// x 4 - compressed input jpegs,
												// 1Mb - safe reserve

		// find index selected in preferences
    	int prefIdx = -1;
    	try
    	{
    		prefIdx = Integer.parseInt(MainScreen.ImageSizeIdxPreference);
    	}
    	catch (IndexOutOfBoundsException e)
    	{
    		prefIdx = -1;
    	}

		// ----- Find max-resolution capture dimensions
//    	Camera camera = MainScreen.thiz.getCamera();
//    	if (null==camera)
//    		return;
//		Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
//		List<Camera.Size> cs = cp.getSupportedPictureSizes();
    	
    	//CameraCharacteristics params = MainScreen.thiz.getCameraParameters2();
    	//Size[] cs = params.get(CameraCharacteristics.SCALER_AVAILABLE_PROCESSED_SIZES);
    	List<CameraController.Size> cs = CameraController.ResolutionsSizeList;
		int Capture5mIdx = -1;
		long Capture5mMpix = 0;
		int Capture5mWidth = 0;
		int Capture5mHeight = 0;
		long CaptureMpix = 0;
		int CaptureWidth = 0;
		int CaptureHeight = 0;
		int CaptureIdx = -1;
		boolean prefFound = false;

		// figure default resolution
		int ii = 0;
		for(CameraController.Size s : cs)
		{
//		for (int ii = 0; ii < cs.size(); ++ii) {
			//Size s = cs.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((mpix >= MIN_MPIX_SUPPORTED)
					&& (mpix < maxMpix)) {
				if (mpix > Capture5mMpix) {
					Capture5mIdx = ii;
					Capture5mMpix = mpix;
					Capture5mWidth = s.getWidth();
					Capture5mHeight = s.getHeight();
				}
			}
			
			ii++;
		}

		ii = 0;
		for(CameraController.Size s : cs)
		{
//		for (int ii = 0; ii < cs.size(); ++ii) {
//			Size s = cs.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((ii == prefIdx) && (mpix >= MIN_MPIX_SUPPORTED)) {
				prefFound = true;
				CaptureIdx = ii;
				CaptureMpix = mpix;
				CaptureWidth = s.getWidth();
				CaptureHeight = s.getHeight();
				break;
			}

			if (mpix > CaptureMpix) {
				CaptureIdx = ii;
				CaptureMpix = mpix;
				CaptureWidth = s.getWidth();
				CaptureHeight = s.getHeight();
			}
			
			ii++;
		}

		// default to about 5Mpix if nothing is set in preferences or maximum
		// resolution is above memory limits
		if (Capture5mMpix > 0) {
			if (!prefFound)
			{
				CaptureIdx = Capture5mIdx;
				CaptureMpix = Capture5mMpix;
				CaptureWidth = Capture5mWidth;
				CaptureHeight = Capture5mHeight;
			}
		}

		CameraController.CapIdx = CaptureIdx;
		MainScreen.setImageWidth(CaptureWidth);
		MainScreen.setImageHeight(CaptureHeight);
		
		MainScreen.setSaveImageWidth(CaptureWidth);
		MainScreen.setSaveImageHeight(CaptureHeight);
//		PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(SessionID), String.valueOf(CaptureWidth));
//    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(SessionID), String.valueOf(CaptureHeight));
	}
	
	public void SetCameraPreviewSize(Camera.Parameters cp) {
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		List<Camera.Size> cs = cp.getSupportedPreviewSizes();

		Camera.Size os = getOptimalPreviewSize(cs, MainScreen.getImageWidth(),
				MainScreen.getImageHeight());
		cp.setPreviewSize(os.width, os.height);
		try
        {
			MainScreen.thiz.setCameraParameters(cp);
		}
		catch(RuntimeException e)
	    {
	    	Log.e("CameraTest", "MainScreen.setupCamera unable setParameters "+e.getMessage());	
	    }
	}
	
	public void SetCameraPictureSize() {
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
    	int jpegQuality = Integer.parseInt(prefs.getString("commonJPEGQuality", "95"));
    	
		Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
		cp.setPictureSize(MainScreen.getImageWidth(), MainScreen.getImageHeight());
		cp.setJpegQuality(jpegQuality);
		try
        {
			MainScreen.thiz.setCameraParameters(cp);
		}
		catch(RuntimeException e)
	    {
	    	Log.e("CameraTest", "MainScreen.setupCamera unable setParameters "+e.getMessage());	
	    }
	}
	
	// from google example
	protected Camera.Size getOptimalPreviewSize(List<Camera.Size> sizes, int w, int h) {
		final double ASPECT_TOLERANCE = 0.05;
		double targetRatio = (double) w / h;
		if (sizes == null)
			return null;

		Camera.Size optimalSize = null;
		double minDiff = Double.MAX_VALUE;

		int targetHeight = h;

		// Try to find an size match aspect ratio and size
		for (Camera.Size size : sizes) {
			double ratio = (double) size.width / size.height;
			if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE)
				continue;
			if (Math.abs(size.height - targetHeight) < minDiff) {
				optimalSize = size;
				minDiff = Math.abs(size.height - targetHeight);
			}
		}

		// Cannot find the one match the aspect ratio, ignore the requirement
		if (optimalSize == null) {
			minDiff = Double.MAX_VALUE;
			for (Camera.Size size : sizes) {
				if (Math.abs(size.height - targetHeight) < minDiff) {
					optimalSize = size;
					minDiff = Math.abs(size.height - targetHeight);
				}
			}
		}
		return optimalSize;
	}
	
	//called after camera parameters setup
	public void onCameraSetup(){}
	
	//called to set specific plugin's camera parameters
	public void SetupCameraParameters(){}	
		
	//called before camera parameters setup - to set plugin specific options
	public void onCameraParametersSetup(){}	
	
	// Called for each plugin on first camera startup. Camera setup is guaranteed when it's called.
	public void onDefaultsSelect() { }
	
	public void onCaptureFinished() { }

	//return true in implementation of plugin if plugin can call OnShutterClick with delay, specified in general settings.
	public boolean delayedCaptureSupported(){return false;}
	
/******************************************************************************************************
	Processing Interfaces
******************************************************************************************************/
	public void StartProcessing(){}
	
	//called on task complete
	public void onTaskComplete(Task task){}
	
	//called on intermediate result ready. For example if during processing some preview available
	public void onPreviewComplete(Task task){}
	
	public void onClick(View v){}
	
	//onBroadcast message - receives broadcast message.
	//if message ignored - return false (continue onBroadcast)
	//if message needed and no need to send it further - return true (stop onBroadcast)
	//if message needed and need to send it further - return false (continue onBroadcast)
	public boolean onBroadcast(int arg1, int arg2)
	{
		return false;
	}
	
	//supplementary methods
	public String getID()
	{
		return ID;
	}
	
	public void setPreferenceName(int id)
	{
		prefName = id;
	}
	
	public void setAdvancedPreferenceName(int id)
	{
		advancedPrefName = id;
	}
	
	public int getPreferenceName()
	{
		return prefName;
	}
	
	public int getAdvancedPreferenceName()
	{
		return advancedPrefName;
	}
	
	//show preference's value in summary on start 
	public void showInitialSummary(PreferenceActivity prefActivity)
	{
		for(int i=0;i<prefActivity.getPreferenceScreen().getPreferenceCount();i++)
		{
		    initSummary(prefActivity.getPreferenceScreen().getPreference(i));
		}
		onPreferenceCreate(prefActivity);
	}
	
	public void showInitialSummary(PreferenceFragment preferenceFragment)
	{
		for(int i=0;i<preferenceFragment.getPreferenceScreen().getPreferenceCount();i++)
		{
		    initSummary(preferenceFragment.getPreferenceScreen().getPreference(i));
		}
		onPreferenceCreate(preferenceFragment);
	}
	
	private void initSummary(Preference p)
	{
		if (p instanceof PreferenceCategory)
		{
		    PreferenceCategory pCat = (PreferenceCategory)p;
		    for(int i=0;i<pCat.getPreferenceCount();i++)
		    {
		    	initSummary(pCat.getPreference(i));
		    }
		}
		else
   		{
   			updatePrefSummary(p);
   		}
	}
		 
	public void updatePrefSummary(Preference p)
	{
		if (p instanceof ListPreference) 
		{
			ListPreference listPref = (ListPreference) p; 
			p.setSummary(listPref.getEntry()); 
		}
		if (p instanceof EditTextPreference) 
		{
			EditTextPreference editTextPref = (EditTextPreference) p; 
			if(p.getKey().equalsIgnoreCase("editKey"))
			{
				p.setSummary("*****"); 
			}
			else 
			{
				p.setSummary(editTextPref.getText()); 
			}
		}
 	}
	
	//method can be used to create some additional preferences programmatically 
	public void onPreferenceCreate(PreferenceActivity prefActivity){}
	
	public void onPreferenceCreate(PreferenceFragment preferenceFragment){}
	
	public boolean ShowPreferences()
	{
		return isShowPreferences;
	}
	
	//called for multishot plugin to obtain bitmap at specific index
	public Bitmap getMultishotBitmap(int index) {return null;}
	public Bitmap getScaledMultishotBitmap(int index, int scaled_width, int scaled_height){return null;}
	public int getResultYUV(int index){return -1;}
	public int getMultishotImageCount(){return 0;}
	public boolean isPostProcessingNeeded(){return false;}


/******************************************************************************************************
Export Interface
******************************************************************************************************/
	public void onExportActive(long SessionID)
	{}
	
//	//called when export finished to clean all allocated memory
	public void FreeMemory()
	{}
	
	public void onExportFinished()
	{}
	
/******************************************************************************************************
	GUI Interface
******************************************************************************************************/
	//method is used by children of class Plugin
	protected void clearViews(){pluginViews.clear();}
	protected void addView(View view, ViewfinderZone position){pluginViews.put(view, position);}
	protected void addView(View view){pluginViews.put(view, ViewfinderZone.VIEWFINDER_ZONE_BOTTOM_LEFT);}
	protected void removeView(View view){pluginViews.remove(view);}
	
	//used by GUIManager to obtain list of view for current plugin
	public Map<View, ViewfinderZone> getPluginViews(){return pluginViews;}
	
	
	//method is used by children of class Plugin
	protected void clearInfoViews(){infoViews.clear();}
	protected void addInfoView(View view){infoViews.add(view);}
	protected void removeInfoView(View view){infoViews.remove(view);}
	
	//used by GUIManager to obtain list of view for current plugin
	public List<View> getInfoViews(){return infoViews;}
	
	public View getPostProcessingView(){return postProcessingView;}
	
	//Quick control interfaces
	public int getQuickControlIconID(){return quickControlIconID;}
	public String getQuickControlTitle(){return quickControlTitle;}
	public void onQuickControlClick(){}
	
/******************************************************************************************************
	OpenGL layer functions
******************************************************************************************************/
	
	public boolean isGLSurfaceNeeded(){return false;}
	public void onGLSurfaceCreated(GL10 gl, EGLConfig config){}	
	public void onGLSurfaceChanged(GL10 gl, int width, int height){}	
	public void onGLDrawFrame(GL10 gl){}
}
