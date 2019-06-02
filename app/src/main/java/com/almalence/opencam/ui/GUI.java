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
package com.almalence.opencam_plus.ui;
+++ --> */
//<!-- -+-
package com.almalence.opencam.ui;
//-+- -->


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.content.Context;
import android.util.Pair;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.RelativeLayout;

/* <!-- +++
import com.almalence.opencam_plus.ApplicationScreen;
import com.almalence.opencam_plus.Plugin;
import com.almalence.opencam_plus.cameracontroller.CameraController;
+++ --> */
//<!-- -+-
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.Plugin;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

/***
 * GUI implements basic functionality for GUI.
 * 
 * Extend GUI class to implement new GUI.
 ***/

public abstract class GUI
{
	// Flags to know which camera feature supported at current device
	public boolean			mEVSupported					= false;
	public boolean			mSceneModeSupported				= false;
	public boolean			mWBSupported					= false;
	public boolean			mFocusModeSupported				= false;
	public boolean			mFlashModeSupported				= false;
	public boolean			mISOSupported					= false;
	public boolean			mCameraChangeSupported			= false;
	public boolean			mCollorEffectsSupported			= false;

	public boolean			mEVLockSupported				= false;
	public boolean			mWBLockSupported				= false;

	public boolean			mMeteringAreasSupported			= false;
	
	public boolean			mManualExposureTimeSupported  	= false;
	public boolean			mManualFocusDistanceSupported	= false;
	public boolean			mManualWhiteBalanceSupported	= false;
	
	public boolean			isAutoFocusDistance				= true;

	// Lists of added plugin's controls
	List<View>				VFViews;
	List<View>				FullScreenVFViews;
	List<View>				CaptureViews;
	List<View>				ProcessingViews;
	List<View>				FilterViews;
	List<View>				ExportViews;

	// List of Open Camera modes
	List<View>				ModeViews;

	// flag to know if controls should be locked
	public boolean			lockControls				= false;

	static protected int	mDeviceOrientation			= 0;
	static protected int	mPreviousDeviceOrientation	= 0;
	
	//Rotations values for bitmaps according to Pair<imageDataOrientation, deviceLayoutOrientation>
	static protected 		Map<Pair<Integer,Integer>, Integer>			mMatrixRotationMap;
	
	public enum ShutterButton
	{
		DEFAULT, RECORDER_START_WITH_PAUSE, RECORDER_START, RECORDER_STOP_WITH_PAUSE, RECORDER_STOP, RECORDER_RECORDING_WITH_PAUSE, RECORDER_RECORDING, RECORDER_PAUSED, TIMELAPSE_ACTIVE
	}

	public enum CameraParameter
	{
		CAMERA_PARAMETER_EV,
		CAMERA_PARAMETER_SCENE,
		CAMERA_PARAMETER_WB,
		CAMERA_PARAMETER_FOCUS,
		CAMERA_PARAMETER_FLASH,
		CAMERA_PARAMETER_ISO,
		CAMERA_PARAMETER_METERING,
		CAMERA_PARAMETER_EXPTIME,
		CAMERA_PARAMETER_FDISTANCE,
		CAMERA_PARAMETER_CAMERACHANGE
	}

	public GUI()
	{
		// Plugin's views lists
		VFViews = new ArrayList<View>();
		FullScreenVFViews = new ArrayList<View>();
		CaptureViews = new ArrayList<View>();
		ProcessingViews = new ArrayList<View>();
		FilterViews = new ArrayList<View>();
		ExportViews = new ArrayList<View>();

		ModeViews = new ArrayList<View>();
		
		//All possible rotations for bitmap according of combination of image data orientation
		//and gui's layout orientation
		mMatrixRotationMap = new HashMap<Pair<Integer, Integer>, Integer>()
		{
			{
				put(new Pair<Integer, Integer>(0, 0), 0);
				put(new Pair<Integer, Integer>(0, 90), 270);
				put(new Pair<Integer, Integer>(0, 180), 180);
				put(new Pair<Integer, Integer>(0, 270), 90);
				put(new Pair<Integer, Integer>(90, 0), 90);
				put(new Pair<Integer, Integer>(90, 90), 0);
				put(new Pair<Integer, Integer>(90, 180), 270);
				put(new Pair<Integer, Integer>(90, 270), 180);
				put(new Pair<Integer, Integer>(180, 0), 180);
				put(new Pair<Integer, Integer>(180, 90), 90);
				put(new Pair<Integer, Integer>(180, 180), 0);
				put(new Pair<Integer, Integer>(180, 270), 270);
				put(new Pair<Integer, Integer>(270, 0), 270);
				put(new Pair<Integer, Integer>(270, 90), 180);
				put(new Pair<Integer, Integer>(270, 180), 90);
				put(new Pair<Integer, Integer>(270, 270), 0);
			}
		};
	}

	abstract public void onStart();

	abstract public void onStop();

	abstract public void onPause();

	abstract public void onResume();

	abstract public void onDestroy();

	abstract public void createInitialGUI();

	// Create standard OpenCamera's buttons and theirs OnClickListener
	abstract public void onCreate();

	// onGUICreate called when main layout is rendered and size's variables is
	// available
	abstract public void onGUICreate();

	public void removeViews(View viewElement, int layoutId)
	{
		List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout) ApplicationScreen.instance.findViewById(layoutId);
		for (int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for (int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int viewElement_id = viewElement.getId();
			if (view_id == viewElement_id)
			{
				if (view.getParent() != null)
					((ViewGroup) view.getParent()).removeView(view);
				specialLayout.removeView(view);
			}
		}
	}

	abstract public void onCaptureFinished();

	// called to set any indication when export plugin work finished.
	abstract public void onPostProcessingStarted();

	abstract public void onPostProcessingFinished();

	// called to set any indication when export plugin work finished.
	abstract public void onExportFinished();

	// Called when camera object created in ApplicationScreen.
	// After camera creation it is possibly to obtain
	// all camera possibilities such as supported scene mode, flash mode and
	// etc.
	abstract public void onCameraCreate();

	abstract public void onPluginsInitialized();

	abstract public void onCameraSetup();

	abstract public void setupViewfinderPreviewSize(CameraController.Size previewSize);

	abstract public void menuButtonPressed();

	abstract public void onButtonClick(View button);

	// Hide all pop-up layouts
	abstract public void hideSecondaryMenus();

	abstract protected void addPluginViews(Map<View, Plugin.ViewfinderZone> views_map);

	abstract public void addViewQuick(View view, Plugin.ViewfinderZone zone);

	abstract protected void removePluginViews(Map<View, Plugin.ViewfinderZone> views_map);

	abstract public void removeViewQuick(View view);

	/* Private section for adding plugin's views */

	// INFO view
	abstract protected void addInfoView(View view, android.widget.LinearLayout.LayoutParams viewLayoutParams);
	abstract public void addInfoView(View info_control);

	abstract public void removeInfoView(View view);

	// MODE SECTION
	// AddMode
	// RemoveMode
	// GetLayoutWidth
	// GetLayoutHeight
	// methods
	// Adds new mode to the list. OnClickListerner must be created by
	// PluginManager at application start
	abstract public void addMode(View mode);

	// reset selection for all views and set or selected one
	abstract public void SetModeSelected(View v);

	// hide mode selector
	abstract public void hideModes();

	// Get fixed maximum size for mode buttons
	abstract public int getMaxModeViewWidth();

	abstract public int getMaxModeViewHeight();

	abstract public int getMinPluginViewHeight();

	abstract public int getMinPluginViewWidth();

	abstract public int getMaxPluginViewHeight();

	abstract public int getMaxPluginViewWidth();

	/*
	 * CAMERA PARAMETERS SECTION Supplementary methods for those plugins that
	 * need an icons of supported camera parameters (scene, iso, wb, flash,
	 * focus) Methods return id of drawable icon
	 */
	abstract public int getSceneIcon(int sceneMode);

	abstract public int getWBIcon(int wb);

	abstract public int getFocusIcon(int focusMode);

	abstract public int getFlashIcon(int flashMode);

	abstract public int getISOIcon(int isoMode);
	
	// Methods returns readable camera parameters name
	abstract public String getSceneName(int sceneMode);

	abstract public String getWBName(int wb);

	abstract public String getFocusName(int focusMode);

	abstract public String getFlashName(int flashMode);

	abstract public String getISOName(int isoMode);

	/* FOCUS MANAGER SECTION */
	/*
	 * Code for focus zones taken from open source Android Camera ANDROID CAMERA
	 * CODE begin
	 */
	abstract public boolean onTouch(View view, MotionEvent e);

	abstract public void onClick(View view);

	abstract public void onHardwareShutterButtonPressed();

	abstract public void onHardwareFocusButtonPressed();

	abstract public void onVolumeBtnExpo(int keyCode);

	@TargetApi(14)
	abstract public void setFocusParameters();

	abstract public void setShutterIcon(ShutterButton id);

	abstract public boolean onKeyDown(boolean isFromMain, int keyCode, KeyEvent event);

	abstract public void disableCameraParameter(CameraParameter iParam, boolean bDisable, boolean bInitMenu, boolean bModeInit);
	
	abstract public void filterCameraParameter(CameraParameter iParam, int[] allowedParams);

	abstract public void startProcessingAnimation();

	abstract public void processingBlockUI();

	// continuous capture indication. Shows some indication until stop called
	abstract public void startContinuousCaptureIndication();

	abstract public void stopCaptureIndication();

	// one time capture indication
	abstract public void showCaptureIndication();

	abstract public float getScreenDensity();

	public int getDisplayOrientation()
	{
		return (mDeviceOrientation + 90) % 360;
	} //Real device orientation. Landscape is 0
	
	public int getImageDataOrientation()
	{
		int sensorOrientation = CameraController.getSensorOrientation(CameraController.isFrontCamera()? 1 : 0);
		
		int imageOrientation = (mDeviceOrientation + (sensorOrientation + (CameraController.isFrontCamera()? 180 : 0))%360) % 360;
		return imageOrientation;
	} // used to operate with image's data
	//Universal logic to calculate image data orientation based on camera sensor orientation, device orientation and front\back camera mode

	public int getLayoutOrientation()
	{
		return (mDeviceOrientation) % 360;
	} // used to operate with ui controls
	//Portrait mode is 0 because we locked app's orientation to portrait mode

	public int getDisplayRotation()
	{
		int orientation = getLayoutOrientation();
		int displayRotationCurrent = orientation == 0 || orientation == 180 ? orientation : (orientation + 180) % 360;
		return displayRotationCurrent;
	} // used to operate with plugin's views
	
	//Post-processing plugins used that method to get right rotation of preview Bitmap
	public int getMatrixRotationForBitmap(int iImageDataOrientation, int iLayoutOrientation, boolean isCameraMirrored)
	{
//		int compensateRotation = iLayoutOrientation + (iLayoutOrientation == 90 || iLayoutOrientation == 270 ? 180 : 0)%360;
//		
//		int rotation = (iImageDataOrientation + compensateRotation + ((isCameraMirrored && (iImageDataOrientation == 90 || iImageDataOrientation == 270)) ? 180 : 0))%360;
//		
//		return rotation;
		
		//For front camera in portrait mode value of image data orientation isn't mirrored
		//so we have to correct it to get right value of matrix orientation
		boolean isPortrait = (iImageDataOrientation == 90 || iImageDataOrientation == 270);
		if(isCameraMirrored && isPortrait)
			iImageDataOrientation = (iImageDataOrientation + 180)%360;
		
		if((iImageDataOrientation != 0 && iImageDataOrientation != 90 && iImageDataOrientation != 180 && iImageDataOrientation != 270) ||
		   (iLayoutOrientation != 0 && iLayoutOrientation != 90 && iLayoutOrientation != 180 && iLayoutOrientation != 270))
			return 0;
		else
		{
			int rotation = mMatrixRotationMap.get(new Pair<Integer, Integer>(iImageDataOrientation, iLayoutOrientation));
			return rotation;
		}
	}

	// mode help procedure
	abstract public void showHelp(String modeName, String text, int imageID, String Prefs);

	abstract public void setCameraModeGUI(int mode);

	public void showStore()
	{
	}

	public void hideStore()
	{
	}
	
	public void showSonyCameraDeviceExplorer()
	{
	}

	public void hideSonyCameraDeviceExplorer()
	{
	}
	
	public void openGallery(boolean isOpenExternal)
	{}
	
	public View getMainView(){return (View)null;}
}
