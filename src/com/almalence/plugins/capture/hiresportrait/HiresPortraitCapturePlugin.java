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

package com.almalence.plugins.capture.hiresportrait;


/***
Implements hires portrait capture plugin - capture self portrait with rear camera, 
based on portrait captured with front camera
***/

public class HiresPortraitCapturePlugin// extends PluginCapture
{
//	private boolean takingAlready=false;
//	private boolean aboutToTakePicture=false;
//	
//	public HiresPortraitCapturePlugin()
//	{
//		super("com.almalence.plugins.hiresportraitcapture", 0, 0, null, null, 0, null);
//	}
//	
//	private boolean isModeAvailable = false;
//	
//	private boolean firstCaptured = false;
//	private boolean secondCaptured = false;
//	
//	private boolean captureFront = false;
//	private boolean captureRear = false;
//	private boolean stillPlaying = false;
//	
//	//sound generator/player part
//	private int duration = 30; // mseconds
//	private int sampleRate = 16000;
//	private int numSamples = duration * sampleRate / 1000;
//	private short generatedSnd[] = new short[numSamples];
//	Handler handler = new Handler();
//	
//	@Override
//	public void onResume()
//	{
//		new CountDownTimer(5, 5)
//		{
//			public void onTick(long millisUntilFinished) {
//		         
//		     }
//		
//		     public void onFinish() 
//		     {			
//		    	 changeCamera();
//		     }
//		}.start();
//	}
//
//	private void changeCamera() 
//    {			
//		if (Camera.getNumberOfCameras() < 2)
//		{
//			isModeAvailable=false;
//			Toast.makeText(MainScreen.thiz, 
//					MainScreen.thiz.getResources().getString(R.string.HiresPortrait_Not_Available),
//					Toast.LENGTH_LONG).show();
//			return;
//		}
//		isModeAvailable=true;
//
//		if (secondCaptured && firstCaptured)
//		{
//			//start processing
//			MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAPTURE_FINISHED);
//			
//			firstCaptured = false;
//			secondCaptured = false;
//			captureFront = false;
//			captureRear = false;
//			return;
//		}
//		//capture first
//		else if (secondCaptured || !firstCaptured)
//		{
//			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//			Camera.getCameraInfo(MainScreen.CameraIndex,cameraInfo);
//			if (cameraInfo.facing != Camera.CameraInfo.CAMERA_FACING_FRONT)
//			{
//				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
//				Editor prefsEditor = prefs.edit();
//				prefsEditor.putBoolean("useFrontCamera", true);
//				prefsEditor.commit();
//				
//				MainScreen.thiz.PauseMain();
//				MainScreen.thiz.ResumeMain();
//				
//				Toast.makeText(MainScreen.thiz, 
//						MainScreen.thiz.getResources().getString(R.string.HiresPortrait_Capture_Front),
//						Toast.LENGTH_LONG).show();
//			}
//			else
//				Initialize();
//		}
//		//first captured - switch to rear
//		else if (firstCaptured)
//		{
//			Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
//			Camera.getCameraInfo(MainScreen.CameraIndex,cameraInfo);
//			if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT)
//			{
//				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
//				Editor prefsEditor = prefs.edit();
//				prefsEditor.putBoolean("useFrontCamera", false);
//				prefsEditor.commit();
//				
//				MainScreen.thiz.PauseMain();
//				MainScreen.thiz.ResumeMain();
//				
//				Toast.makeText(MainScreen.thiz, 
//						MainScreen.thiz.getResources().getString(R.string.HiresPortrait_Capture_Rear),
//						Toast.LENGTH_LONG).show();
//			}
//		}
//    }
//	
//	@Override
//	public void onDestroy()
//	{
//		firstCaptured = false;
//		secondCaptured = false;
//		captureFront = false;
//		captureRear = false;
//		
//		PluginManager.getInstance().clearSharedMemory(PluginManager.getInstance().getSessionID());
//	}
//	
//	@Override
//	public void OnShutterClick()
//	{
//		if (Camera.getNumberOfCameras() < 2)
//		{
//			isModeAvailable=false;
//			Toast.makeText(MainScreen.thiz, 
//					MainScreen.thiz.getResources().getString(R.string.HiresPortrait_Not_Available),
//					Toast.LENGTH_LONG).show();
//			MainScreen.guiManager.lockControls = false;
//			return;
//		}
//		
//		MainScreen.thiz.MuteShutter(false);
//		
//		String fm = MainScreen.thiz.getFocusMode();
//		if(takingAlready == false && MainScreen.getFocusState() == MainScreen.FOCUS_STATE_IDLE
//				&& fm != null
//				&& !(fm.equals(Parameters.FOCUS_MODE_INFINITY)
//				|| fm.equals(Parameters.FOCUS_MODE_FIXED)
//				|| fm.equals(Parameters.FOCUS_MODE_EDOF)
//				|| fm.equals(Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
//				|| fm.equals(Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
//				&& !MainScreen.getAutoFocusLock())			
//				aboutToTakePicture = true;			
//		else if(takingAlready == false)
//			takePicture();
//	}
//	
//		
//	public void takePicture()
//	{
////		if(takingAlready)
////		{
////			aboutToTakePicture = false;
////			return;
////		}
////		
////		takingAlready = true;		
////		
//		Camera camera = MainScreen.thiz.getCamera();
//		if (camera != null)		// paranoia
//		{
//			
//			if(!firstCaptured && !secondCaptured)
//			{
//				captureFront=true;
//				MainScreen.guiManager.showCaptureIndication();
//				MainScreen.thiz.PlayShutter();
//			}
//			
//			
//	    	//MainScreen.camera.takePicture(null, null, null, MainScreen.thiz);			
//		}
//		else
//		{
//			Message msg = new Message();
//			msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
//			msg.what = PluginManager.MSG_BROADCAST;
//			MainScreen.H.sendMessage(msg);
//			
//			MainScreen.guiManager.lockControls = false;
//		}
//	}
//	
//	private int frmcnt=0;
//	@Override
//	public void onPreviewFrame(byte[] data, Camera camera)
//	{
//		if (captureFront)
//		{//save frame from front camera
//			Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
//			int fov = (int)cp.getHorizontalViewAngle();
//			// some devices report incorrect FOV values, use typical view angles then
//			if (fov >= 150)
//				fov = 55;
//				
//			int imageWidth = cp.getPreviewSize().width;
//			int imageHeight = cp.getPreviewSize().height;
//			
//			if(MainScreen.getCameraMirrored())
//				ImageConversion.TransformNV21(data, data, imageWidth, imageHeight, 1, 1, 0); // 1);
//			
//			SetFrontFrame(data, imageWidth, imageHeight, fov);
//			captureFront=false;
//			
////			/******/
////			Rect rect = new Rect(0, 0, MainScreen.previewHeight, MainScreen.previewWidth); 
////	        YuvImage img = new YuvImage(data, ImageFormat.NV21, MainScreen.previewHeight, MainScreen.previewWidth, null);
////	        OutputStream outStream = null;
////	        
////	        File file = new File(PluginManager.getInstance().GetSaveDir(), "front.jpg");
////	        try 
////	        {
////	            outStream = new FileOutputStream(file);
////	            img.compressToJpeg(rect, 100, outStream);
////	            outStream.flush();
////	            outStream.close();
////	        } 
////	        catch (FileNotFoundException e) 
////	        {
////	            e.printStackTrace();
////	        }
////	        catch (IOException e) 
////	        {
////	            e.printStackTrace();
////	        }
////        	/***************/
//			
//			firstCaptured=true;
//			secondCaptured=false;
//			new CountDownTimer(50, 50)
//			{
//				public void onTick(long millisUntilFinished) {
//				}
//				
//				public void onFinish() 
//				{			
//					captureRear=true;
//					stillPlaying = false;
//					changeCamera();
//				}
//			}.start();
//		}
//		else if (captureRear)
//		{
//			if (frmcnt<3) // 10)
//			{
//				frmcnt++;
//				return;
//			}
//			else
//				frmcnt=0;
//			
//			Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
//	        
//			int fov = (int)cp.getHorizontalViewAngle();
//			// some devices report incorrect FOV values, use typical view angles then
//			if (fov >= 150)
//				fov = 55;
//			
//			//rotate correctly
//			int imageWidth = cp.getPreviewSize().width;
//			int imageHeight = cp.getPreviewSize().height;
//			final int res = CheckRearAlignment(data, imageWidth, imageHeight, fov);
//			Log.v("Hires portrait", "Res is " + res);
//			
//            handler.post(new Runnable() {
//                public void run() {
//                    playSound(3500 - FloatMath.sqrt((float)res)*320);
//                }
//            });
//			
//			if (res<3)
//			{
////				/******/
////				Rect rect = new Rect(0, 0, MainScreen.previewHeight, MainScreen.previewWidth); 
////		        YuvImage img = new YuvImage(data, ImageFormat.NV21, MainScreen.previewHeight, MainScreen.previewWidth, null);
////		        OutputStream outStream = null;
////		        
////		        File file = new File(PluginManager.getInstance().GetSaveDir(), "rear.jpg");
////		        try 
////		        {
////		            outStream = new FileOutputStream(file);
////		            img.compressToJpeg(rect, 100, outStream);
////		            outStream.flush();
////		            outStream.close();
////		        } 
////		        catch (FileNotFoundException e) 
////		        {
////		            e.printStackTrace();
////		        }
////		        catch (IOException e) 
////		        {
////		            e.printStackTrace();
////		        }
////	        	/***************/
//	        	
//	        	
//				captureRear=false;
//				MainScreen.guiManager.showCaptureIndication();
//				MainScreen.thiz.PlayShutter();
//				
//				camera.takePicture(null, null, null, MainScreen.thiz);
//			}
//		}
//		
//	}
//	
//	@Override
//	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
//	{
//		int frame_len = paramArrayOfByte.length;
//		int frame = SwapHeap.SwapToHeap(paramArrayOfByte);
//    	
//    	if (frame == 0)
//    	{
//    		//NotEnoughMemory();
//    	}
//		
//		try
//		{
//			paramCamera.startPreview();
//		}
//		catch (RuntimeException e)
//		{
//			Log.i("hires portrait Capture", "StartPreview fail");
//		}
//		
//		PluginManager.getInstance().addToSharedMem("frame"/*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(frame));
//    	PluginManager.getInstance().addToSharedMem("framelen"/*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(frame_len));
//    	PluginManager.getInstance().addToSharedMem("frameorientation"/*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(MainScreen.guiManager.getDisplayOrientation()));
//    	PluginManager.getInstance().addToSharedMem("framemirrored" /*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(MainScreen.getCameraMirrored()));
//    	
//    	if(captureFront)
//    		PluginManager.getInstance().addToSharedMem_ExifTagsFromJPEG(paramArrayOfByte);
//    	
//		secondCaptured=true;
//		changeCamera();
//		
////		MainScreen.H.sendEmptyMessage(PluginManager.MSG_CAPTURE_FINISHED);
//
////		takingAlready = false;
////		aboutToTakePicture = false;
////		
////		if (!firstCaptured)
////		{
////			firstCaptured=true;
////			secondCaptured=false;
////			
////			PluginManager.getInstance().addToSharedMem("frame1"/*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(frame));
////	    	PluginManager.getInstance().addToSharedMem("framelen1"/*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(frame_len));
////	    	PluginManager.getInstance().addToSharedMem("frameorientation1"/*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(MainScreen.getWantLandscapePhoto()));
////	    	PluginManager.getInstance().addToSharedMem("framemirrored1" /*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(MainScreen.getCameraMirrored()));
////			
////			changeCamera();
////		}
////		else if (!secondCaptured)
////		{
////			secondCaptured=true;
////			
////			PluginManager.getInstance().addToSharedMem("frame2"/*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(frame));
////	    	PluginManager.getInstance().addToSharedMem("framelen2"/*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(frame_len));
////	    	PluginManager.getInstance().addToSharedMem("frameorientation2"/*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(MainScreen.getWantLandscapePhoto()));
////	    	PluginManager.getInstance().addToSharedMem("framemirrored2" /*+String.valueOf(PluginManager.getInstance().getSessionID())*/, String.valueOf(MainScreen.getCameraMirrored()));
////	    	
////			changeCamera();
////		}
//	}
//	
//	@Override
//	public void onAutoFocus(boolean paramBoolean, Camera paramCamera)
//	{
//		if(aboutToTakePicture == true)
//			takePicture();
//		
////		if(aboutToTakePicture == true && paramBoolean == true)
////			takePicture();
////		else if(aboutToTakePicture == true)
////		{
////			aboutToTakePicture = false;
////			MainScreen.guiManager.lockControls = false;
////		}
//	}
//
//	
//	void playSound(float freqOfTone)
//	{
//		if (!stillPlaying)	// poor man synchronization
//		{
//			stillPlaying = true;
//			
//			// fill out the array
//			for (int i = 0; i < numSamples; ++i)
//			{
//				float amp = 32767*FloatMath.sin(2 * (float)Math.PI * i / (sampleRate/freqOfTone));
//				if (i<numSamples/4) amp *= (float)i / (numSamples/4); 
//				if (i>3*numSamples/4) amp *= (float)(numSamples-i) / (numSamples/4); 
//				generatedSnd[i] = (short)amp;
//			}
//			
//			AudioTrack audioTrack = new AudioTrack(AudioManager.STREAM_MUSIC,
//					sampleRate, AudioFormat.CHANNEL_OUT_MONO,
//					AudioFormat.ENCODING_PCM_16BIT, numSamples*4, // need some spare samples to prevent click at the end
//				    AudioTrack.MODE_STATIC);
//
//			if (audioTrack.getState() == AudioTrack.STATE_NO_STATIC_DATA)
//			{
//				audioTrack.write(generatedSnd, 0, numSamples);
//				audioTrack.play();
//			}
//			
//			try {
//				Thread.sleep(duration);
//			} catch (InterruptedException e) { }
//
//			audioTrack.stop();
//			audioTrack.release();
//			
//			stillPlaying = false;
//		}
//	}
//	
//	
//	public static synchronized native void Initialize();
//	public static synchronized native void Release();
//	public static synchronized native int SetFrontFrame(byte[] Front_VF_Frame, int width, int height, int frontHorizontalFOV);
//	public static synchronized native int CheckRearAlignment(byte[] Rear_VF_Frame, int width, int height, int rearHorizontalFOV);
//	
//    static 
//    {
//    	System.loadLibrary("almalib");
//        System.loadLibrary("hiresportrait");
//    }
}
