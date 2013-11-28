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

package com.almalence.plugins.capture.objectremoval;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.CountDownTimer;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

import com.almalence.SwapHeap;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;

/***
Implements object removal capture plugin - captures predefined number of images
***/

public class ObjectRemovalCapturePlugin extends PluginCapture
{
	private boolean takingAlready=false;
		
    //defaul val. value should come from config
	private int imageAmount = 1;
    private int pauseBetweenShots = 0;
    private boolean inCapture;
    private int imagesTaken=0;
	
	public ObjectRemovalCapturePlugin()
	{
		super("com.almalence.plugins.objectremovalcapture",
			  R.xml.preferences_capture_objectremoval,
			  0,
			  MainScreen.thiz.getResources().getString(R.string.Pref_ObjectRemoval_Capture_Preference_Title),
			  MainScreen.thiz.getResources().getString(R.string.Pref_ObjectRemoval_Capture_Preference_Summary),
			  0,
			  null);
		refreshPreferences();
	}
	
	@Override
	public void onResume()
	{
		takingAlready = false;
		imagesTaken=0;
		inCapture = false;
		
		refreshPreferences();
	}
	
	@Override
	public void onGUICreate()
	{
		String text = "1. Hold device still and don't move\n2. Press shutter to start\n3. Wait until series of images taken and processed\n4. Tap detected moving objects to delete";
		MainScreen.guiManager.showHelp("Object removal help", text, R.drawable.plugin_help_object, "objectRemovalShowHelp");
	}
	
	private void refreshPreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		imageAmount = Integer.parseInt(prefs.getString("objectRemovalImagesAmount", "7"));
		pauseBetweenShots = Integer.parseInt(prefs.getString("objectRemovalPauseBetweenShots", "750"));
	}
	
	public boolean delayedCaptureSupported(){return true;}
	
	@Override
	public void OnShutterClick()
	{
		if (inCapture == false)
        {
			MainScreen.thiz.MuteShutter(true);
			
			String fm = MainScreen.thiz.getFocusMode();
	
			if(fm != null && takingAlready == false && (MainScreen.getFocusState() == MainScreen.FOCUS_STATE_IDLE ||
					MainScreen.getFocusState() == MainScreen.FOCUS_STATE_FOCUSING)
					&& !(fm.equals(Parameters.FOCUS_MODE_INFINITY)
	        				|| fm.equals(Parameters.FOCUS_MODE_FIXED)
	        				|| fm.equals(Parameters.FOCUS_MODE_EDOF)
	        				|| fm.equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
	        				|| fm.equals(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
	        				&& !MainScreen.getAutoFocusLock())
				takingAlready = true;			
			else if(takingAlready == false)
				takePicture();
        }
	}
	
	public void takePicture()
	{
		inCapture = true;
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    	{
    		Message msg = new Message();
			msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
			
			MainScreen.guiManager.lockControls = false;
			inCapture = false;
			takingAlready = false;
    		return;
    	}
		refreshPreferences();
		takingAlready = true;
		if (imagesTaken==0 || pauseBetweenShots==0)
		{
			if (camera != null)
	 		{
				// play tick sound
				MainScreen.guiManager.showCaptureIndication();
        		MainScreen.thiz.PlayShutter();
        		
	 	    	camera.takePicture(null, null, null, MainScreen.thiz);
	 		}
		}
		else
		{
			new CountDownTimer(pauseBetweenShots, pauseBetweenShots) {
			     public void onTick(long millisUntilFinished) {}
			     public void onFinish() 
			     {
			    	Message msg = new Message();
					msg.arg1 = PluginManager.MSG_NEXT_FRAME;
					msg.what = PluginManager.MSG_BROADCAST;
					MainScreen.H.sendMessage(msg);
			     }
			  }.start();
		}
	}

	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
		imagesTaken++;
		int frame_len = paramArrayOfByte.length;
		int frame = SwapHeap.SwapToHeap(paramArrayOfByte);
    	
    	if (frame == 0)
    	{
    		Log.i("Object Removal", "Load to heap failed");
    		MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAPTURE_FINISHED);
			imagesTaken=0;
			MainScreen.thiz.MuteShutter(false);
			inCapture = false;
			return;
    		//NotEnoughMemory();
    	}
    	String frameName = "frame" + imagesTaken;
    	String frameLengthName = "framelen" + imagesTaken;
    	
    	PluginManager.getInstance().addToSharedMem(frameName+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem(frameLengthName+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(frame_len));
    	PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken +String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
    	PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(MainScreen.getCameraMirrored()));
    	
    	if(imagesTaken == 1)
    		PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte);
		
		try
		{
			paramCamera.startPreview();
		}
		catch (RuntimeException e)
		{
			Log.i("Object Removal", "StartPreview fail");
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAPTURE_FINISHED);
			imagesTaken=0;
			MainScreen.thiz.MuteShutter(false);
			inCapture = false;
			return;
		}
		if (imagesTaken < imageAmount)
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
		else
		{
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(imagesTaken));
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAPTURE_FINISHED);
			imagesTaken=0;
			new CountDownTimer(5000, 5000) {
			     public void onTick(long millisUntilFinished) {}
			     public void onFinish() 
			     {
			    	 inCapture = false;
			     }
			  }.start();
		}
		takingAlready = false;
	}
	
	@Override
	public void onAutoFocus(boolean paramBoolean, Camera paramCamera)
	{
		if(takingAlready == true)
			takePicture();
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
        			camera.takePicture(null, null, null, MainScreen.thiz);
				}catch (Exception e) {
					e.printStackTrace();
					Log.e("MainScreen takePicture() failed", "takePicture: " + e.getMessage());
					inCapture = false;
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
				inCapture = false;
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
	public void onPreviewFrame(byte[] data, Camera paramCamera){}	
}
