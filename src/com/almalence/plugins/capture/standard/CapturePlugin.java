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

import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Message;
import android.util.Log;

import com.almalence.SwapHeap;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;


/***
Implements standard capture plugin - capture single image and save it in shared memory
***/

public class CapturePlugin extends PluginCapture
{
	private boolean takingAlready=false;
	private boolean aboutToTakePicture=false;
	
	public CapturePlugin()
	{
		super("com.almalence.plugins.capture", 0, 0, null, null, 0, null);
	}
	
	@Override
	public void OnShutterClick()
	{
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
			takePicture();
	}
	
	public void takePicture()
	{
		if(takingAlready)
		{
			aboutToTakePicture = false;
			return;
		}
		
		takingAlready = true;		
		
		Camera camera = MainScreen.thiz.getCamera();
		if (camera != null)		// paranoia
		{
			MainScreen.guiManager.showCaptureIndication();
			
			MainScreen.thiz.PlayShutter();
	    	try {
	    		camera.takePicture(null, null, null, MainScreen.thiz);
			} catch (Exception e) {
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
    	PluginManager.getInstance().addToSharedMem("frame1"+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem("framelen1"+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(frame_len));
    	PluginManager.getInstance().addToSharedMem("frameorientation1"+String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
    	PluginManager.getInstance().addToSharedMem("framemirrored1" + String.valueOf(PluginManager.getInstance().getSessionID()), String.valueOf(MainScreen.getCameraMirrored()));
		
    	PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(PluginManager.getInstance().getSessionID()), "1");
    	PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte);
    	
		try
		{
			paramCamera.startPreview();
		}
		catch (RuntimeException e)
		{
			Log.i("Capture", "StartPreview fail");
		}
		MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAPTURE_FINISHED);

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
