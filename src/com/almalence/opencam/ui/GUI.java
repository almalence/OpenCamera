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
// <!-- -+-
package com.almalence.opencam.ui;
//-+- -->


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import android.annotation.TargetApi;
import android.hardware.Camera;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.View;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.Plugin;
import com.almalence.opencam_plus.R;
import com.almalence.opencam_plus.ui.AlmalenceGUI.ShutterButton;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.Plugin;
import com.almalence.opencam.R;
import com.almalence.opencam.ui.AlmalenceGUI.ShutterButton;
//-+- -->


/***
GUI implements basic functionality for GUI. 

Extend GUI class to implement new GUI.
***/

public abstract class GUI
{
	public final static String sEvPref = "EvCompensationValue";
	public final static String sSceneModePref = "SceneModeValue";
	public final static String sWBModePref = "WBModeValue";
	public final static String sFrontFocusModePref = "FrontFocusModeValue";
	public final static String sRearFocusModePref = "RearFocusModeValue";
	public final static String sFlashModePref = "FlashModeValue";
	public final static String sISOPref = "ISOValue";
	public final static String sDefaultValue = MainScreen.thiz.getResources().getString(R.string.sceneAutoSystem);
	public final static String sDefaultFocusValue = MainScreen.thiz.getResources().getString(R.string.focusContinuousPictureSystem);

	public final static String isoParam = "iso";
	public final static String isoParam2 = "iso-speed";
	
	// Flags to know which camera feature supported at current device
	public boolean mEVSupported = false;	
	public boolean mSceneModeSupported = false;
	public boolean mWBSupported = false;
	public boolean mFocusModeSupported = false;
	public boolean mFlashModeSupported = false;
	public boolean mISOSupported = false;
	public boolean mCameraChangeSupported = false;
	
	public boolean mEVLockSupported = false;
	public boolean mWBLockSupported = false;
	
	//Lists of added plugin's controls
	List<View> VFViews;
	List<View> FullScreenVFViews;
	List<View> CaptureViews;
	List<View> ProcessingViews;
	List<View> FilterViews;
	List<View> ExportViews;
	
	//List of Open Camera modes
	List<View> ModeViews;	
	
	//flag to know if controls should be locked
	public boolean lockControls = false;
	
	static protected int mDeviceOrientation = 0;
	static protected int mPreviousDeviceOrientation = 0;
	
	public enum CameraParameter
	{
		CAMERA_PARAMETER_EV,
		CAMERA_PARAMETER_SCENE,
		CAMERA_PARAMETER_WB,
		CAMERA_PARAMETER_FOCUS,
		CAMERA_PARAMETER_FLASH,
		CAMERA_PARAMETER_ISO,
		CAMERA_PARAMETER_CAMERACHANGE
	}
	
	
	public GUI()
	{
		//Plugin's views lists
		VFViews = new ArrayList<View>();
		FullScreenVFViews = new ArrayList<View>();
		CaptureViews = new ArrayList<View>();
		ProcessingViews = new ArrayList<View>();
		FilterViews = new ArrayList<View>();
		ExportViews = new ArrayList<View>();
		
		ModeViews = new ArrayList<View>();
	}
	
	abstract public void onStart();
	
	abstract public void onStop();
	
	abstract public void onPause();
	
	abstract public void onResume();
	
	abstract public void onDestroy();
	
	abstract public void createInitialGUI();
	
	//Create standard OpenCamera's buttons and theirs OnClickListener
	abstract public void onCreate();
	
	//onGUICreate called when main layout is rendered and size's variables is available
	abstract public void onGUICreate();
	
	abstract public void onCaptureFinished();
	
	//called to set any indication when export plugin work finished.
	abstract public void onPostProcessingStarted();

	abstract public void onPostProcessingFinished();
	
	//called to set any indication when export plugin work finished.
	abstract public void onExportFinished();
	
	//Called when camera object created in MainScreen.
	//After camera creation it is possibly to obtain
	//all camera possibilities such as supported scene mode, flash mode and etc.
	abstract public void onCameraCreate();
	
	abstract public void onCameraSetup();
	
	abstract public void setupViewfinderPreviewSize(Camera.Parameters cp);

	abstract public void menuButtonPressed();
	
	abstract public void onButtonClick(View button);
	
	//Hide all pop-up layouts
	abstract public void hideSecondaryMenus();

	abstract protected void addPluginViews(Map<View, Plugin.ViewfinderZone> views_map);
	
	abstract public void addViewQuick(View view, Plugin.ViewfinderZone zone);
	
	abstract protected void removePluginViews(Map<View, Plugin.ViewfinderZone> views_map);
	
	abstract public void removeViewQuick(View view);

	/* Private section for adding plugin's views */
	
	//INFO view
	abstract protected void addInfoView(View view, android.widget.LinearLayout.LayoutParams viewLayoutParams);
	
	abstract protected void removeInfoView(View view);
	
	// MODE SECTION
	// AddMode
	// RemoveMode
	// GetLayoutWidth
	// GetLayoutHeight
	// methods
	//Adds new mode to the list. OnClickListerner must be created by PluginManager at application start
	abstract public void addMode(View mode);
	
	//reset selection for all views and set or selected one
	abstract public void SetModeSelected(View v);
	
	//hide mode selector
	abstract public void hideModes();
	
	//Get fixed maximum size for mode buttons
	abstract public int getMaxModeViewWidth();
	
	abstract public int getMaxModeViewHeight();
	
	abstract public int getMinPluginViewHeight();

	abstract public int getMinPluginViewWidth();
	
	abstract public int getMaxPluginViewHeight();
	
	abstract public int getMaxPluginViewWidth();
	

	/*CAMERA PARAMETERS SECTION
	 * Supplementary methods for those plugins that need an icons of supported camera parameters (scene, iso, wb, flash, focus)
	 * Methods return id of drawable icon 
	*/
	abstract public int getSceneIcon(String sceneMode);
	
	abstract public int getWBIcon(String wb);
	
	abstract public int getFocusIcon(String focusMode);
	
	abstract public int getFlashIcon(String flashMode);
	
	abstract public int getISOIcon(String isoMode);

	
	/*FOCUS MANAGER SECTION*/
	/*Code for focus zones taken from open source Android Camera
	 * ANDROID CAMERA CODE begin
	*/	
	abstract public boolean onTouch(View view, MotionEvent e);
	
	abstract public void onClick(View view);
	
	abstract public void onHardwareShutterButtonPressed();
	
	abstract public void onHardwareFocusButtonPressed();
	
//	abstract public void autoFocus();
//	
//	abstract public void onAutoFocus(boolean focused, Camera paramCamera);
	
	@TargetApi(14)	
	abstract public void setFocusParameters();
	
	abstract public void setShutterIcon(ShutterButton id);

	abstract public boolean onKeyDown(boolean isFromMain, int keyCode, KeyEvent event);
	
	abstract public void disableCameraParameter(CameraParameter iParam, boolean bDisable, boolean bInitMenu);
	
	abstract public void startProcessingAnimation();
	
	abstract public void processingBlockUI();
	
	//continuous capture indication. Shows some indication until stop called
	abstract public void startContinuousCaptureIndication();
	abstract public void stopCaptureIndication();
	
	//one time capture indication
	abstract public void showCaptureIndication();

	abstract public float getScreenDensity();
	
	public int getDisplayOrientation(){return (mDeviceOrientation + 90)%360;} // used to operate with image's data
	public int getLayoutOrientation(){return (mDeviceOrientation)%360;} //used to operate with ui controls
	public int getDisplayRotation()
	{
		int orientation = getLayoutOrientation();
		int displayRotationCurrent = orientation == 0 || orientation == 180? orientation: (orientation + 180)%360;
		return displayRotationCurrent;
	} //used to operate with plugin's views
	
	//mode help procedure
	abstract public void showHelp(String modeName, String text, int imageID, String Prefs);
}