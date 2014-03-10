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

package com.almalence.plugins.capture.standard;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import com.almalence.SwapHeap;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginCapture;
import com.almalence.opencam_plus.PluginManager;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.ui.GUI;
import com.almalence.opencam.ui.GUI.CameraParameter;
//-+- -->
import com.almalence.ui.Switch.Switch;


/***
Implements standard capture plugin - capture single image and save it in shared memory
***/

public class CapturePlugin extends PluginCapture
{
	private boolean takingAlready=false;
	private boolean aboutToTakePicture=false;
	
	public static String ModePreference;	// 0=DRO On 1=DRO Off
	private Switch modeSwitcher;
	
	public CapturePlugin()
	{
		super("com.almalence.plugins.capture", 0, 0, 0, null);
	}
	
	@Override
	public void onCreate()
	{
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();		
		modeSwitcher = (Switch)inflator.inflate(R.layout.plugin_capture_standard_modeswitcher, null, false);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        ModePreference = prefs.getString("modeStandardPref", "1");
        modeSwitcher.setTextOn("DRO On");
        modeSwitcher.setTextOff("DRO Off");
        modeSwitcher.setChecked(ModePreference.compareTo("0") == 0 ? true : false);
		modeSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
				int currEv = prefs.getInt(GUI.sEvPref, 0);
				int newEv = currEv;
				int minValue = MainScreen.thiz.getMinExposureCompensation();
				if (isChecked)
				{
					newEv -= 1;
					ModePreference = "0";
				}
				else
				{
					ModePreference = "1";
				}
				
				Camera.Parameters params = MainScreen.thiz.getCameraParameters();
				if (params != null && newEv >= minValue)
				{
					params.setExposureCompensation(newEv);
					MainScreen.thiz.setCameraParameters(params);
				}
				
				SharedPreferences.Editor editor = prefs.edit();		        	
	        	editor.putString("modeStandardPref", ModePreference);
	        	editor.commit();
	        	
	        	if (ModePreference.compareTo("0") == 0)
	    			MainScreen.guiManager.showHelp("Dro help", MainScreen.thiz.getResources().getString(R.string.Dro_Help), R.drawable.plugin_help_dro, "droShowHelp");
			}
		});
		
		if(PluginManager.getInstance().getProcessingCounter() == 0)
			modeSwitcher.setEnabled(true);
	}
	
	@Override
	public void onCameraParametersSetup()
	{		
		if (ModePreference.compareTo("0") == 0)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
			int currEv = prefs.getInt(GUI.sEvPref, 0);
			int newEv = currEv;
			int minValue = MainScreen.thiz.getMinExposureCompensation();
			newEv -= 1;
			
			Camera.Parameters params = MainScreen.thiz.getCameraParameters();
			if (params != null && newEv >= minValue)
			{
				params.setExposureCompensation(newEv);
				MainScreen.thiz.setCameraParameters(params);
			}
		}
	}
	
	
	@Override
	public void onStart()
	{
		// Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);        
        ModePreference = prefs.getString("modeStandardPref", "1");
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
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
		
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout3)).addView(this.modeSwitcher, params);
		
		this.modeSwitcher.setLayoutParams(params);
		this.modeSwitcher.requestLayout();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout3)).requestLayout();
		
		if (ModePreference.compareTo("0") == 0)
			MainScreen.guiManager.showHelp("Dro help", MainScreen.thiz.getResources().getString(R.string.Dro_Help), R.drawable.plugin_help_dro, "droShowHelp");
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
	public void OnShutterClick()
	{
		if(takingAlready == false)
		{
			Date curDate = new Date();
			SessionID = curDate.getTime();
			
			MainScreen.thiz.MuteShutter(false);
			
			String fm = MainScreen.thiz.getFocusMode();
			int fs = MainScreen.getFocusState();
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
			else if(takingAlready == false)
			{
				takePicture();
			}
		}
	}
	
	
	@Override
	public void onDefaultsSelect()
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);        
        ModePreference = prefs.getString("modeStandardPref", "1");
	}
	
	@Override
	public void onShowPreferences()
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);        
        ModePreference = prefs.getString("modeStandardPref", "1");
	}
	
	
	public void takePicture()
	{
		if(takingAlready)
		{
			aboutToTakePicture = false;
			return;
		}
		
		takingAlready = true;
		
		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_NEXT_FRAME;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);

	}
	
	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_NEXT_FRAME)
		{
			Camera camera = MainScreen.thiz.getCamera();
			if (camera != null)
			{
				// play tick sound
				MainScreen.guiManager.showCaptureIndication();
        		MainScreen.thiz.PlayShutter();
        		
        		try {
        			camera.setPreviewCallback(null);
        			camera.takePicture(null, null, null, MainScreen.thiz);
				}
        		catch (Exception e) 
				{
        			e.printStackTrace();
    				Log.e("Standard capture", "takePicture exception: " + e.getMessage());
    				takingAlready = false;
    				Message msg = new Message();
    				msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
    				msg.what = PluginManager.MSG_BROADCAST;
    				MainScreen.H.sendMessage(msg);
    				MainScreen.guiManager.lockControls = false;
				}
			}
			else
			{
				takingAlready = false;
				Message msg = new Message();
				msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
				msg.what = PluginManager.MSG_BROADCAST;
				MainScreen.H.sendMessage(msg);
				
				MainScreen.guiManager.lockControls = false;
			}						
    		return true;
		}
		return false;
	}
	
	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
		int frame_len = paramArrayOfByte.length;
		int frame = SwapHeap.SwapToHeap(paramArrayOfByte);
    	
    	if (frame == 0)
    	{
    		//NotEnoughMemory();
    	}
    	PluginManager.getInstance().addToSharedMem("frame1"+String.valueOf(SessionID), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem("framelen1"+String.valueOf(SessionID), String.valueOf(frame_len));
    	PluginManager.getInstance().addToSharedMem("frameorientation1"+String.valueOf(SessionID), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
    	PluginManager.getInstance().addToSharedMem("framemirrored1" + String.valueOf(SessionID), String.valueOf(MainScreen.getCameraMirrored()));
		
    	PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(SessionID), "1");
    	PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte, SessionID);
    	
    	PluginManager.getInstance().addToSharedMem("isdroprocessing"+String.valueOf(SessionID), ModePreference);
    	
		try
		{
			paramCamera.startPreview();
		}
		catch (RuntimeException e)
		{
			Log.i("Capture", "StartPreview fail");
		}
		
		Message message = new Message();
		message.obj = String.valueOf(SessionID);
		message.what = PluginManager.MSG_CAPTURE_FINISHED;
		MainScreen.H.sendMessage(message);

		takingAlready = false;
		aboutToTakePicture = false;
	}
	
	@Override
	public void onAutoFocus(boolean paramBoolean, Camera paramCamera)
	{
		if(aboutToTakePicture == true)
			takePicture();
		
//		if(aboutToTakePicture == true && paramBoolean == true)
//			takePicture();
//		else if(aboutToTakePicture == true)
//		{
//			aboutToTakePicture = false;
//			MainScreen.guiManager.lockControls = false;
//		}
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera){}
	
	public boolean delayedCaptureSupported(){return true;}
}
