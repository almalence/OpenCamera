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

import java.nio.ByteBuffer;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.os.CountDownTimer;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

/* <!-- +++
import com.almalence.opencam_plus.CameraController;
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginCapture;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.SwapHeap;
import com.almalence.YuvImage;

/***
Implements burst capture plugin - captures predefined number of images
***/

public class BurstCapturePlugin extends PluginCapture
{
    //defaul val. value should come from config
	private int imageAmount = 3;
    private int pauseBetweenShots = 0;

    private int imagesTaken=0;
    
    private static String sImagesAmountPref;
    private static String sPauseBetweenShotsPref;
	
    
	public BurstCapturePlugin()
	{
		super("com.almalence.plugins.burstcapture",
			  R.xml.preferences_capture_burst,
			  0,
			  R.drawable.gui_almalence_mode_burst,
			  "Burst images");
	}
	
	@Override
	public void onCreate()
	{
		sImagesAmountPref = MainScreen.thiz.getResources().getString(R.string.Preference_BurstImagesAmount);
		sPauseBetweenShotsPref = MainScreen.thiz.getResources().getString(R.string.Preference_BurstPauseBetweenShots);
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
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
			imageAmount = Integer.parseInt(prefs.getString(sImagesAmountPref, "3"));
			pauseBetweenShots = Integer.parseInt(prefs.getString(sPauseBetweenShotsPref, "0"));
		}
		catch (Exception e)
		{
			Log.v("Burst capture", "Cought exception " + e.getMessage());
		}
		
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
        case 15:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst15;
        	break;
        case 20:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst20;
        	break;
        }       
	}
	
	@Override
	public void onQuickControlClick()
	{        
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        int val = Integer.parseInt(prefs.getString(sImagesAmountPref, "1"));
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
        case 15:
        	selected=3;
        	break;
        case 20:
        	selected=4;
        	break;
        }
        selected= (selected+1)%5;
        
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
        case 3:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst15;
        	editor.putString("burstImagesAmount", "15");
        	break;
        case 4:
        	quickControlIconID = R.drawable.gui_almalence_mode_burst20;
        	editor.putString("burstImagesAmount", "20");
        	break;
        }
    	editor.commit();
	}

	public boolean delayedCaptureSupported(){return true;}
	
	public void takePicture()
	{
		refreshPreferences();
		inCapture = true;
		takingAlready = true;
		if (imagesTaken==0 || pauseBetweenShots==0)
		{
			Message msg = new Message();
			msg.arg1 = PluginManager.MSG_NEXT_FRAME;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);					
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
    		Message message = new Message();
    		message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
			
			imagesTaken=0;
			MainScreen.thiz.MuteShutter(false);
			inCapture = false;
			return;
    	}
    	String frameName = "frame" + imagesTaken;
    	String frameLengthName = "framelen" + imagesTaken;
    	
    	PluginManager.getInstance().addToSharedMem(frameName+String.valueOf(SessionID), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem(frameLengthName+String.valueOf(SessionID), String.valueOf(frame_len));
    	PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken + String.valueOf(SessionID), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
    	PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + String.valueOf(SessionID), String.valueOf(CameraController.isFrontCamera()));
    	
    	if(imagesTaken == 1)
    		PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte, SessionID, -1);
		
		try
		{
			paramCamera.startPreview();
		}
		catch (RuntimeException e)
		{
			Log.i("Burst", "StartPreview fail");
			Message message = new Message();
			message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
			
			imagesTaken=0;
			MainScreen.thiz.MuteShutter(false);
			inCapture = false;
			return;
		}
		if (imagesTaken < imageAmount)
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
		else
		{
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(SessionID), String.valueOf(imagesTaken));
			
			Message message = new Message();
			message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
			
			imagesTaken=0;
			inCapture = false;
		}
		takingAlready = false;
	}
	
	
	@TargetApi(19)
	@Override
	public void onImageAvailable(Image im)
	{
		imagesTaken++;
		
		String frameName = "frame" + imagesTaken;
    	String frameLengthName = "framelen" + imagesTaken;
		
		int frame = 0;
		int frame_len = 0;
		boolean isYUV = false;
		
		if(im.getFormat() == ImageFormat.YUV_420_888)
		{
			Log.e("BurstCapturePlugin", "YUV Image received");
			ByteBuffer Y = im.getPlanes()[0].getBuffer();
			ByteBuffer U = im.getPlanes()[1].getBuffer();
			ByteBuffer V = im.getPlanes()[2].getBuffer();
	
			if ( (!Y.isDirect()) || (!U.isDirect()) || (!V.isDirect()) )
			{
				Log.e("BurstCapturePlugin", "Oops, YUV ByteBuffers isDirect failed");
				return;
			}
			
			
			// Note: android documentation guarantee that:
			// - Y pixel stride is always 1
			// - U and V strides are the same
			//   So, passing all these parameters is a bit overkill
			int status = YuvImage.CreateYUVImage(Y, U, V,
					im.getPlanes()[0].getPixelStride(),
					im.getPlanes()[0].getRowStride(),
					im.getPlanes()[1].getPixelStride(),
					im.getPlanes()[1].getRowStride(),
					im.getPlanes()[2].getPixelStride(),
					im.getPlanes()[2].getRowStride(),
					MainScreen.getImageWidth(), MainScreen.getImageHeight(), 0);
			
			if (status != 0)
				Log.e("BurstCapturePlugin", "Error while cropping: "+status);
			
			
			frame = YuvImage.GetFrame(0);			
			frame_len = MainScreen.getImageWidth()*MainScreen.getImageHeight()+MainScreen.getImageWidth()*((MainScreen.getImageHeight()+1)/2);
			isYUV = true;
		}
		else if(im.getFormat() == ImageFormat.JPEG)
		{
			Log.e("BurstCapturePlugin", "JPEG Image received");
			ByteBuffer jpeg = im.getPlanes()[0].getBuffer();
			
			frame_len = jpeg.limit();
			byte[] jpegByteArray = new byte[frame_len];
			jpeg.get(jpegByteArray, 0, frame_len);
			
			frame = SwapHeap.SwapToHeap(jpegByteArray);
			
			if(imagesTaken == 1)
	    		PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(jpegByteArray, SessionID, -1);
		}
    	
		PluginManager.getInstance().addToSharedMem(frameName+String.valueOf(SessionID), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem(frameLengthName+String.valueOf(SessionID), String.valueOf(frame_len));
    	PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken + String.valueOf(SessionID), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
    	PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + String.valueOf(SessionID), String.valueOf(CameraController.isFrontCamera()));
		
    	PluginManager.getInstance().addToSharedMem("isyuv"+String.valueOf(SessionID), String.valueOf(isYUV));	
    	
		
		try
		{
			CameraController.startCameraPreview();
		}
		catch (RuntimeException e)
		{
			Log.i("Burst", "StartPreview fail");
			Message message = new Message();
			message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
			
			imagesTaken=0;
			MainScreen.thiz.MuteShutter(false);
			inCapture = false;
			return;
		}
		if (imagesTaken < imageAmount)
			MainScreen.H.sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
		else
		{
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(SessionID), String.valueOf(imagesTaken));
			
			Message message = new Message();
			message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
			
			imagesTaken=0;
			inCapture = false;
		}
		takingAlready = false;
	}
	
	@TargetApi(19)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		if(result.get(CaptureResult.REQUEST_ID) == requestID)
		{
			if(imagesTaken == 1)
	    		PluginManager.getInstance().addToSharedMem_ExifTagsFromCaptureResult(result, SessionID);
		}
	}
	
	
	@Override
	public void onAutoFocus(boolean paramBoolean)
	{
		if(takingAlready == true)
			takePicture();
	}

	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_NEXT_FRAME)
		{
			// play tick sound
			MainScreen.guiManager.showCaptureIndication();
    		MainScreen.thiz.PlayShutter();
    		
    		try
    		{
    			requestID = CameraController.captureImage(1, CameraController.JPEG);
			}catch (Exception e)
			{
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
				
    		return true;
		}
		return false;
	}
	
	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera){}	
}
