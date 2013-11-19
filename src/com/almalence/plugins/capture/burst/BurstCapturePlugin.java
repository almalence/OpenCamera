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

package com.almalence.plugins.capture.burst;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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
Implements burst capture plugin - captures predefined number of images
***/

public class BurstCapturePlugin extends PluginCapture
{
	private boolean takingAlready=false;
		
    //defaul val. value should come from config
	private int imageAmount = 3;
    private int pauseBetweenShots = 0;

    private boolean inCapture;
    private int imagesTaken=0;
	
	public BurstCapturePlugin()
	{
		super("com.almalence.plugins.burstcapture",
			  R.xml.preferences_capture_burst,
			  0,
			  MainScreen.thiz.getResources().getString(R.string.Pref_Burst_Preference_Title),
			  MainScreen.thiz.getResources().getString(R.string.Pref_Burst_Preference_Summary),
			  R.drawable.gui_almalence_mode_burst,
			  "Burst images");

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
	
	private void refreshPreferences()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		imageAmount = Integer.parseInt(prefs.getString("burstImagesAmount", "3"));
		pauseBetweenShots = Integer.parseInt(prefs.getString("burstPauseBetweenShots", "0"));
		
        switch (imageAmount)
        {
        case 3:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst3;
        	break;
        case 5:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst5;
        	break;
        case 10:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst10;
        	break;
        }       
	}
	
	@Override
	public void onQuickControlClick()
	{        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        int val = Integer.parseInt(prefs.getString("burstImagesAmount", "1"));
        int selected = 0;
        switch (val)
        {
        case 3:
        	selected=0;
        	break;
        case 5:
        	selected=1;
        	break;
        case 10:
        	selected=2;
        	break;
        }
        selected= (selected+1)%3;
        
    	Editor editor = prefs.edit();
    	switch (selected)
        {
        case 0:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst3;
        	editor.putString("burstImagesAmount", "3");
        	break;
        case 1:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst5;
        	editor.putString("burstImagesAmount", "5");
        	break;
        case 2:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst10;
        	editor.putString("burstImagesAmount", "10");
        	break;
        }
    	editor.commit();
	}

	@Override
	public void OnShutterClick()
	{
		if (inCapture == false)
        {
			MainScreen.thiz.MuteShutter(true);
			
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
				takingAlready = true;			
			else if(takingAlready == false)
				takePicture();
        }
	}
	
	
	public void takePicture()
	{
		refreshPreferences();
		inCapture = true;
		takingAlready = true;
		if (imagesTaken==0 || pauseBetweenShots==0)
		{
			new CountDownTimer(50, 50) {
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
    		Log.i("Burst", "Load to heap failed");
    		MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAPTURE_FINISHED);
			imagesTaken=0;
			MainScreen.thiz.MuteShutter(false);
			inCapture = false;
			return;
    	}
    	String frameName = "frame" + imagesTaken;
    	String frameLengthName = "framelen" + imagesTaken;
    	
    	PluginManager.getInstance().addToSharedMem(frameName+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem(frameLengthName+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(frame_len));
    	PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken + String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
    	PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(MainScreen.getCameraMirrored()));
    	
    	if(imagesTaken == 1)
    		PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte);
		
		try
		{
			paramCamera.startPreview();
		}
		catch (RuntimeException e)
		{
			Log.i("Burst", "StartPreview fail");
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
			inCapture = false;
		}
		takingAlready = false;
	}
	
	@Override
	public void onAutoFocus(boolean paramBoolean, Camera paramCamera)
	{
		if(takingAlready == true)
			takePicture();
		
//		if(takingAlready == true && paramBoolean == true)
//			takePicture();
//		else if(takingAlready == true)
//		{
//			takingAlready = false;
//			MainScreen.guiManager.lockControls = false;
//		}
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
        		
		    	camera.takePicture(null, null, null, MainScreen.thiz);
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
