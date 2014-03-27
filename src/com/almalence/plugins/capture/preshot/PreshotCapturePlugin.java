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

package com.almalence.plugins.capture.preshot;

import java.util.Date;

import android.content.SharedPreferences;
import android.hardware.Camera;
import android.media.Image;
import android.os.CountDownTimer;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.almalence.opencam.CameraController;
import com.almalence.opencam.CameraParameters;
/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginCapture;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
import com.almalence.opencam_plus.ui.GUI;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.ui.Switch.Switch;

/***
Implements back in time capture plugin

Starts capturing images immediately after start. 
Stops capturing when shutter button pressed.
***/

public class PreshotCapturePlugin extends PluginCapture
{
	public static final int MIN_MPIX_SUPPORTED = 1280*960;
	public static final int MIN_MPIX_PREVIEW = 600*400;
	
    public static boolean inCapture = false;

	private boolean takingAlready = false;

	public static int CapIdx;
	
	public static boolean wantLandscapePhoto = false;
	
    // preferences
    public static String PreShotInterval;
	public static String FPS;
	public static boolean IsRecordSound;
	public static boolean SaveInputPreference;
	public static boolean RefocusPreference;
	public static boolean AutostartPreference;
	public static String PauseBetweenShots;
	private int preferenceFocusMode;
	
	public static boolean isSlowMode = false;
	
	private static boolean isBuffering = false;
	
	private static int counter=0;
    private static final int REFOCUS_INTERVAL = 3;
    
    private Switch modeSwitcher;
    
    private boolean captureStarted=false;
    
	public PreshotCapturePlugin()
	{
		super("com.almalence.plugins.preshotcapture",
			  R.xml.preferences_capture_preshot,
			  R.xml.preferences_capture_preshot,
			  0,
			  null);
	}
	
	@Override
	public void onStart()
	{
		getPrefs();
	}
	
	@Override
	public void onResume()
	{
		//PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putBoolean("UseFocus", false).commit();
		preferenceFocusMode = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(MainScreen.getCameraMirrored()? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_AUTO);        

		MainScreen.thiz.MuteShutter(false);
		
		captureStarted=false;
	}

	@Override
	public void onPause()
	{
		StopBuffering();
		inCapture = false;
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putBoolean("UseFocus", true).commit();		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.getCameraMirrored()? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, preferenceFocusMode).commit();
		
	}
	
	@Override
	public void onDestroy()
	{
		PreShot.FreeBuffer();
	}
	
	@Override
	public void onGUICreate()
	{
		getPrefs();
		
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();		
		modeSwitcher = (Switch)inflator.inflate(R.layout.plugin_capture_preshot_modeswitcher, null, false);
		
        modeSwitcher.setTextOn("Hi-Res");
        modeSwitcher.setTextOff("Hi-Speed");
        modeSwitcher.setChecked(isSlowMode);
		modeSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				StopBuffering();
				inCapture=false;
				
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
				
				SharedPreferences.Editor editor = prefs.edit();		        	
	        	editor.putString("modePrefPreShot", isChecked?"1":"0");
	        	editor.commit();
	        					
				Message msg = new Message();
				msg.what = PluginManager.MSG_RESTART_MAIN_SCREEN;				
				MainScreen.H.sendMessage(msg);
			}
		});
		
		android.widget.RelativeLayout.LayoutParams lp = new android.widget.RelativeLayout.LayoutParams(
				LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);
		lp.topMargin = 17;
		
		modeSwitcher.setLayoutParams(lp);
		if (PluginManager.getInstance().getProcessingCounter()==0)
			modeSwitcher.setEnabled(true);
		else
			modeSwitcher.setEnabled(false);
		
		clearViews();
		addView(modeSwitcher, ViewfinderZone.VIEWFINDER_ZONE_TOP_RIGHT);
	}
	
	private void getPrefs()
    {
		// Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        SaveInputPreference = prefs.getBoolean("saveInputPrefPreShot", false);
		RefocusPreference = prefs.getBoolean("refocusPrefPreShot", false);
		AutostartPreference = prefs.getBoolean("autostartPrefPreShot", false);
		PauseBetweenShots = prefs.getString("pauseBetweenShotsPrefPreShot", "500");
		PreShotInterval = prefs.getString("backInTimePrefPreShot", "5");
		
		if (1 == Integer.parseInt(prefs.getString("modePrefPreShot", "0")))
		{
			isSlowMode = true;
			FPS = "2";
		}
		else
		{
			FPS = prefs.getString("fpsPrefPreShot", "4");
			isSlowMode = false;
		}
    }

	@Override
	public void onCameraSetup()
	{
		if (AutostartPreference)
			if (PluginManager.getInstance().getProcessingCounter()==0)
				StartBuffering();
	}
	
	@Override
	public void SetupCameraParameters()
	{
		Camera camera = CameraController.getInstance().getCamera();
    	if (null==camera)
    		return;
		Camera.Parameters cp = CameraController.getInstance().getCameraParameters();
		if (isSlowMode==false)//fast mode
		{
			try 
			{				 
//				if(MainScreen.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
				if (CameraController.isModeAvailable(CameraController.supportedFocusModes,CameraParameters.AF_MODE_CONTINUOUS_VIDEO))
				{
					cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);			
					CameraController.getInstance().setCameraParameters(cp);
					
					PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putInt(MainScreen.getCameraMirrored()? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, CameraParameters.AF_MODE_CONTINUOUS_VIDEO).commit();
				}				
			}
			catch (Exception e)
			{
				Log.i("Preshot capture", "Exception fast:" + e.getMessage());
			}
		}
		else//slow mode
		{
			try 
			{
//				if(MainScreen.supportedFocusModes.contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE))
				if (CameraController.isModeAvailable(CameraController.supportedFocusModes,CameraParameters.AF_MODE_CONTINUOUS_PICTURE))
				{
					cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
					CameraController.getInstance().setCameraParameters(cp);
					
					PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putString(MainScreen.getCameraMirrored()? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE).commit();
				}				
			}
			catch(Exception e)
			{
				Log.i("Preshot capture", "Exception slow:" + e.getMessage());
				
			}
		}
		
		Message msg = new Message();
		msg.arg1 = PluginManager.MSG_FOCUS_CHANGED;
		msg.what = PluginManager.MSG_BROADCAST;
		MainScreen.H.sendMessage(msg);
	}

	@Override
	public void onExportFinished()
	{
		if (modeSwitcher!=null)
			modeSwitcher.setEnabled(true);
		if(AutostartPreference)
		{
			Camera camera = CameraController.getInstance().getCamera();
	    	if (null==camera)
	    		return;
	    	StartBuffering();
		}
	}

	@Override
	public void OnShutterClick()
	{
//		if (PluginManager.getInstance().getProcessingCounter()!=0)
//		{
//			Toast.makeText(MainScreen.thiz, "Processing in progress. Please wait.", Toast.LENGTH_SHORT).show();
//			return;
//		}
		if (captureStarted || AutostartPreference)
		{
			if (0 == PreShot.GetImageCount())
			{
				Toast.makeText(MainScreen.thiz, "No images yet", Toast.LENGTH_SHORT).show();
				return;
			}
			captureStarted=false;
			StopBuffering();
			
			Message message = new Message();
			message.obj = String.valueOf(SessionID);
			message.what = PluginManager.MSG_CAPTURE_FINISHED;
			MainScreen.H.sendMessage(message);
		}
		else
		{
			Camera camera = CameraController.getInstance().getCamera();
	    	if (null==camera)
	    		return;
	    	if (!AutostartPreference)
	    		modeSwitcher.setEnabled(false);
			captureStarted=true;
	    	StartBuffering();
		}
	}
	
	private static int frmCnt = 1;
	private static int preview_fps = 0; 
		
	public static int imW = 0;
	public static int imH = 0;
	public static int format = 0;
	
	// starts buffering to native buffer
	void StartBuffering() {
		
		Date curDate = new Date();
		SessionID = curDate.getTime();

		MainScreen.thiz.MuteShutter(true);
		
		isBuffering = true;
		if (isSlowMode==false)
		{
			Camera camera = CameraController.getInstance().getCamera();
	    	if (null==camera)
	    		return;
			MainScreen.guiManager.startContinuousCaptureIndication();
			preview_fps = CameraController.getInstance().getCameraParameters().getPreviewFrameRate();
			
			imW = CameraController.getInstance().getCameraParameters().getPreviewSize().width;
			imH = CameraController.getInstance().getCameraParameters().getPreviewSize().height;
			format = CameraController.getInstance().getCameraParameters().getPreviewFormat();
			
			MainScreen.setSaveImageWidth(imW);
			MainScreen.setSaveImageHeight(imH);
//			PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(SessionID), String.valueOf(imW));
//        	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(SessionID), String.valueOf(imH));
	
			Log.i("Preshot capture", "StartBuffering trying to allocate!");
			
			int secondsAllocated = PreShot.AllocateBuffer(imW, imH, Integer.parseInt(FPS), Integer.parseInt(PreShotInterval), 0);
			if (secondsAllocated==0) {
				Log.i("Preshot capture", "StartBuffering failed, can't allocate native buffer!");
				return;
			}
			PluginManager.getInstance().addToSharedMem("IsSlowMode"+String.valueOf(SessionID), "false");
		}
		else
		{
			//full size code
			PreShot.FreeBuffer();
			imW = MainScreen.getImageWidth();
			imH = MainScreen.getImageHeight();
			
			MainScreen.setSaveImageWidth(imW);
			MainScreen.setSaveImageHeight(imH);
//			PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(SessionID), String.valueOf(imW));
//        	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(SessionID), String.valueOf(imH));
			
			//Log.i("StartBuffering", "SX "+ SX +" SY "+ SY);
			int secondsAllocated = PreShot.AllocateBuffer(imW, imH, Integer.parseInt(FPS), Integer.parseInt(PreShotInterval), 1);
			if (secondsAllocated==0) {
				Log.i("Preshot capture","StartBuffering failed, can't allocate native buffer!");
				return;
			}
			PluginManager.getInstance().addToSharedMem("IsSlowMode"+String.valueOf(SessionID), "true");
			
			StartCaptureSequence();
		}
	}

	void StopBuffering() {
		
		MainScreen.guiManager.stopCaptureIndication();
		
		MainScreen.thiz.MuteShutter(false);
		
		if(modeSwitcher != null)
			modeSwitcher.setEnabled(false);
		
		if (!isBuffering)
			return;
		else
			isBuffering = false;
		
		counter = 0;
		
		PluginManager.getInstance().addToSharedMem("amountofcapturedframes"+String.valueOf(SessionID), String.valueOf(PreShot.GetImageCount()));
		
		if (isSlowMode==false)
		{
			frmCnt = 1;
		}
	}

	@Override
	public void onPreviewFrame(byte[] _data, Camera _camera) {
		if (isSlowMode==true || !isBuffering)
			return;
		
		if (0==frmCnt%(preview_fps/Integer.parseInt(FPS)))
		{
//			if(MainScreen.getCameraMirrored())
//			{
//				Camera.Parameters params = _CameraController.getInstance().getCameraParameters();			
//				int imageWidth = params.getPreviewSize().width;
//				int imageHeight = params.getPreviewSize().height;
//				
//				byte[] dataRotated = new byte[_data.length];
//				ImageConversion.TransformNV21(_data, dataRotated, imageWidth, imageHeight, 1, 1, 1);
//				
//				_data = dataRotated;			
				
	//			/******/
	//			Rect rect = new Rect(0, 0, MainScreen.previewHeight, MainScreen.previewWidth); 
	//	        YuvImage img = new YuvImage(_data, ImageFormat.NV21, MainScreen.previewHeight, MainScreen.previewWidth, null);
	//	        OutputStream outStream = null;
	//	        
	//	        File file = new File(PluginManager.getInstance().GetSaveDir(), "front.jpg");
	//	        try 
	//	        {
	//	            outStream = new FileOutputStream(file);
	//	            img.compressToJpeg(rect, 100, outStream);
	//	            outStream.flush();
	//	            outStream.close();
	//	        } 
	//	        catch (FileNotFoundException e) 
	//	        {
	//	            e.printStackTrace();
	//	        }
	//	        catch (IOException e) 
	//	        {
	//	            e.printStackTrace();
	//	        }
	//        	/***************/
//			}	
			System.gc();
		
		
			if(frmCnt == 0)
	    		PluginManager.getInstance().addToSharedMem_ExifTagsFromCamera(SessionID);
			//PreShot.InsertToBuffer(_data, MainScreen.getWantLandscapePhoto()?0:1);

			PreShot.InsertToBuffer(_data, MainScreen.guiManager.getDisplayOrientation());
		}
		frmCnt++;
	}
	
	void StartCaptureSequence()
    {
        if (inCapture == false)
        {
    		inCapture = true;
    		takingAlready = false;

    		Camera camera = CameraController.getInstance().getCamera();
        	if (null==camera)
        		return;
    		// start series
            try		// some crappy models have autoFocus() = null
            {
            	int focusMode = CameraController.getInstance().getFocusMode();
            	if(!(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
      				  focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO ||
    				  focusMode == CameraParameters.AF_MODE_INFINITY ||
    				  focusMode == CameraParameters.AF_MODE_FIXED ||
    				  focusMode == CameraParameters.AF_MODE_EDOF)
					 && !MainScreen.getAutoFocusLock())
            	{
        			if(!MainScreen.autoFocus())
        			{
        				this.CaptureFrame(camera);
    		    		takingAlready = true;
        			}
            	}
            	else if (!takingAlready)
            	{
		        	this.CaptureFrame(camera);
		    		takingAlready = true;
            	}
            }
            catch (NullPointerException e)
            {
            	if (!takingAlready)
            	{
    	        	this.CaptureFrame(camera);
    	    		takingAlready = true;
            	}
            }
        }
    }
    
    public void NotEnoughMemory()
    {
    	Log.i("Preshot capture", "NotEnoughMemory!");
    }
	
    @Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
    	inCapture = false;
    	
    	if (0 == PreShot.GetImageCount())
    		PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte, SessionID);
			
    	//PreShot.InsertToBuffer(paramArrayOfByte, MainScreen.getWantLandscapePhoto()?0:1);
    	PreShot.InsertToBuffer(paramArrayOfByte, MainScreen.guiManager.getDisplayOrientation());
    	
		takingAlready = false;
		paramCamera.startPreview();

		try
		{
			if (isBuffering)
			{
				ProcessPauseBetweenShots();
			}
		}
		catch (RuntimeException e)
		{
			Log.i("Preshot capture", "StartPreview fail");
			Message msg = new Message();
			msg.arg1 = PluginManager.MSG_NEXT_FRAME;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
		}
	}
    
    
    @Override
	public void onImageAvailable(Image im)
	{
		
	}
    
    
	
	void ProcessPauseBetweenShots()
    {
    	int interval = Integer.parseInt(PauseBetweenShots);
    	if (interval == 0)
    	{
    		afterPause();
    		return;
    	}
    	
    	new CountDownTimer(interval, interval) {
			public void onFinish() {
				afterPause();
			}
			
			@Override
			public void onTick(long millisUntilFinished) {}
    	}.start();    	
    }
    
    void afterPause()
    {
    	if (isBuffering)
		{
    		Camera camera = CameraController.getInstance().getCamera();
        	if (null==camera)
        		return;
    		int focusMode = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).getInt(MainScreen.getCameraMirrored()? MainScreen.sRearFocusModePref : MainScreen.sFrontFocusModePref, -1);
			if (RefocusPreference||(counter>=REFOCUS_INTERVAL) &&
				!(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE ||
				  focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO ||
				  focusMode == CameraParameters.AF_MODE_INFINITY ||
				  focusMode == CameraParameters.AF_MODE_FIXED ||
				  focusMode == CameraParameters.AF_MODE_EDOF)
				  && !MainScreen.getAutoFocusLock())
			{
				Log.v("", "1");
				counter=0;
				if(!MainScreen.autoFocus())
					CaptureFrame(camera);
			}
			else
			{
				Log.v("", "2");
				CaptureFrame(camera);
			}
		}
    }
    
	public void CaptureFrame(Camera paramCamera)
    {
    	// only requesting exposure change here
		try
    	{
    		if (isBuffering)
    		{
    			if(MainScreen.getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
    				return;
	    		inCapture = true;
	    		
	    		MainScreen.guiManager.showCaptureIndication();
	    		MainScreen.thiz.PlayShutter();
	    		
	    		//paramCamera.takePicture(null, null, null, MainScreen.thiz);
	    		MainScreen.takePicture();
	    		counter++;
    		}
    	}    	
    	catch (RuntimeException e)
    	{
    		Log.i("Preshot capture", "takePicture fail in CaptureFrame (called after release?)" + e.getMessage());
			paramCamera.startPreview();
    	}
    }

	@Override
	public void onAutoFocus(boolean paramBoolean, Camera paramCamera)
    {
    	// on motorola xt5 cm7 this function is called twice!
		// on motorola droid's onAutoFocus seem to be called at every startPreview,
		// causing additional frame(s) taken after sequence is finished 
    	if (!takingAlready && isSlowMode)
    	{
        	CaptureFrame(paramCamera);
    		takingAlready = true;
    	}
    }
	
	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		Camera camera = CameraController.getInstance().getCamera();
    	if (null==camera)
    		return false;
		if (arg1 == PluginManager.MSG_NEXT_FRAME)
		{
    		if (camera != null)
    		{
				try
				{
					camera.startPreview();
					CaptureFrame(camera);
				}
				catch (RuntimeException e)
				{
		    		Log.i("CameraTest", "RuntimeException in MSG_NEXT_FRAME");
					Message msg = new Message();
					msg.arg1 = PluginManager.MSG_NEXT_FRAME;
					msg.what = PluginManager.MSG_BROADCAST;
					MainScreen.H.sendMessage(msg);
				}
    		}
    		return true;
		}
		else if (arg1 == PluginManager.MSG_STOP_CAPTURE)
		{
    		if (camera != null)
    		{
				StopBuffering();
    		}
    		return true;
		}
		else if (arg1 == PluginManager.MSG_START_CAPTURE)
		{
    		if (camera != null)
    		{
    			if (PluginManager.getInstance().getProcessingCounter()==0)
    				StartBuffering();
    		}
    		return true;
		}
		return false;
	}
}
