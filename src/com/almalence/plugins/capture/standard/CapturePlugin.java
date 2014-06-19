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

import java.nio.ByteBuffer;
import java.util.Date;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;

import com.almalence.YuvImage;
import com.almalence.SwapHeap;

/* <!-- +++
import com.almalence.opencam_plus.CameraController;
import com.almalence.opencam_plus.CameraParameters;
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginCapture;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.CameraController;
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
//-+- -->
import com.almalence.ui.Switch.Switch;


/***
Implements standard capture plugin - capture single image and save it in shared memory
***/

public class CapturePlugin extends PluginCapture
{
	private static String ModePreference;	// 0=DRO On 1=DRO Off
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
				int currEv = prefs.getInt(MainScreen.sEvPref, 0);
				int newEv = currEv;
				int minValue = CameraController.getInstance().getMinExposureCompensation();
				float expStep = CameraController.getInstance().getExposureCompensationStep();
				if (isChecked)
				{
					int diff = (int)Math.round(0.5/expStep);
					if(diff < 1)
						diff = 1;
					newEv -= diff;
					ModePreference = "0";
				}
				else
				{
					ModePreference = "1";
				}
				
				if(newEv >= minValue)
					CameraController.getInstance().setCameraExposureCompensation(newEv);
				
				
				SharedPreferences.Editor editor = prefs.edit();		        	
	        	editor.putString("modeStandardPref", ModePreference);
	        	editor.commit();
	        	
	        	if (ModePreference.compareTo("0") == 0)
	    			MainScreen.guiManager.showHelp(MainScreen.thiz.getString(R.string.Dro_Help_Header), MainScreen.thiz.getResources().getString(R.string.Dro_Help), R.drawable.plugin_help_dro, "droShowHelp");
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
			int currEv = prefs.getInt(MainScreen.sEvPref, 0);
			int newEv = currEv;
			int minValue = CameraController.getInstance().getMinExposureCompensation();
			newEv -= 1;
			
			if(newEv >= minValue)
				CameraController.getInstance().setCameraExposureCompensation(newEv);
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
		MainScreen.guiManager.removeViews(modeSwitcher, R.id.specialPluginsLayout3);
		
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
		MainScreen.guiManager.removeViews(modeSwitcher, R.id.specialPluginsLayout3);
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
	
	
	@Override
	public void takePicture()
	{
		if(inCapture == false)
		{
			inCapture = true;
			takingAlready = true;
			
			Message msg = new Message();
			msg.arg1 = PluginManager.MSG_NEXT_FRAME;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}

	}
	
	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_NEXT_FRAME)
		{
			// play tick sound
			MainScreen.guiManager.showCaptureIndication();
    		MainScreen.thiz.PlayShutter();
    		
			// play tick sound
			MainScreen.guiManager.showCaptureIndication();
    		MainScreen.thiz.PlayShutter();
    		
    		try {
    			if(ModePreference.compareTo("0") == 0)
					requestID = CameraController.captureImage(1, CameraController.YUV);			
				else
					requestID = CameraController.captureImage(1, CameraController.JPEG);
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
		inCapture = false;
	}
	
	@TargetApi(19)
	@Override
	public void onImageAvailable(Image im)
	{
		int frame = 0;
		int frame_len = 0;
		boolean isYUV = false;
		
		if(im.getFormat() == ImageFormat.YUV_420_888)
		{
			Log.e("CapturePlugin", "YUV Image received");
			ByteBuffer Y = im.getPlanes()[0].getBuffer();
			ByteBuffer U = im.getPlanes()[1].getBuffer();
			ByteBuffer V = im.getPlanes()[2].getBuffer();
	
			if ( (!Y.isDirect()) || (!U.isDirect()) || (!V.isDirect()) )
			{
				Log.e("CapturePlugin", "Oops, YUV ByteBuffers isDirect failed");
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
				Log.e("CapturePlugin", "Error while cropping: "+status);
			
			
			frame = YuvImage.GetFrame(0);			
			frame_len = MainScreen.getImageWidth()*MainScreen.getImageHeight()+MainScreen.getImageWidth()*((MainScreen.getImageHeight()+1)/2);
			isYUV = true;
		}
		else if(im.getFormat() == ImageFormat.JPEG)
		{
			Log.e("CapturePlugin", "JPEG Image received");
			ByteBuffer jpeg = im.getPlanes()[0].getBuffer();
			
			frame_len = jpeg.limit();
			byte[] jpegByteArray = new byte[frame_len];
			jpeg.get(jpegByteArray, 0, frame_len);
//			byte[] jpegByteArray = jpeg.array();			
//			int frame_len = jpegByteArray.length;
			
			frame = SwapHeap.SwapToHeap(jpegByteArray);
			
			PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(jpegByteArray, SessionID);
		}
    	
    	PluginManager.getInstance().addToSharedMem("frame1"+String.valueOf(SessionID), String.valueOf(frame));
    	PluginManager.getInstance().addToSharedMem("framelen1"+String.valueOf(SessionID), String.valueOf(frame_len));
    	PluginManager.getInstance().addToSharedMem("frameorientation1"+String.valueOf(SessionID), String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
    	PluginManager.getInstance().addToSharedMem("framemirrored1" + String.valueOf(SessionID), String.valueOf(MainScreen.getCameraMirrored()));
		
    	PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(SessionID), "1");
    	
    	PluginManager.getInstance().addToSharedMem("isyuv"+String.valueOf(SessionID), String.valueOf(isYUV));
    	PluginManager.getInstance().addToSharedMem("isdroprocessing"+String.valueOf(SessionID), ModePreference);
		
		Message message = new Message();
		message.obj = String.valueOf(SessionID);
		message.what = PluginManager.MSG_CAPTURE_FINISHED;
		MainScreen.H.sendMessage(message);

		takingAlready = false;
	}
	
	@TargetApi(19)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		if(result.get(CaptureResult.REQUEST_ID) == requestID)
		{
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
	public void onPreviewFrame(byte[] data, Camera paramCamera){}
	
	public boolean delayedCaptureSupported(){return true;}
}
