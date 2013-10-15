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

package com.almalence.plugins.capture.video;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.drawable.Drawable;
import android.hardware.Camera;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.CountDownTimer;
import android.os.Message;
import android.os.SystemClock;
import android.preference.ListPreference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.ui.GUI;
import com.almalence.opencam.ui.AlmalenceGUI.ShutterButton;

/***
Implements basic functionality of Video capture.
***/

public class VideoCapturePlugin extends PluginCapture
{
    private boolean isRecording;
    
    public static int CapIdx;
    
    public static int CameraIDPreference;

    private MediaRecorder mMediaRecorder;
    
    private long mRecordingStartTime;
    
    // The video duration limit. 0 means no limit.
    private int mMaxVideoDurationInMs;
    
    //video duration text view
    private TextView mRecordingTimeView;
    
    private boolean mRecordingTimeCountsDown = false;

    private boolean shutterOff=false;
    
    private static File fileSaved=null;

    private String preferenceFocusMode;
    
	public VideoCapturePlugin()
	{
		super("com.almalence.plugins.videocapture",
			  R.xml.preferences_capture_video,
			  0,
			  MainScreen.thiz.getResources().getString(R.string.Pref_Video_Preference_Title),
			  MainScreen.thiz.getResources().getString(R.string.Pref_Video_Preference_Summary),
			  R.drawable.gui_almalence_video_1080,
			  "Video quality");
	}

	@Override
	public void onCreate()
	{
		mRecordingTimeView = new TextView(MainScreen.mainContext);
		mRecordingTimeView.setTextSize(12);
		mRecordingTimeView.setBackgroundResource(R.drawable.thumbnail_background);
		mRecordingTimeView.setVisibility(View.GONE);
		mRecordingTimeView.setGravity(Gravity.CENTER);
		mRecordingTimeView.setText("00:00");
		Drawable img = MainScreen.mainContext.getResources().getDrawable( R.drawable.ic_recording_indicator );
		mRecordingTimeView.setCompoundDrawablesWithIntrinsicBounds( img, null, null, null );
		
//		clearViews();
//		addView(mRecordingTimeView, ViewfinderZone.VIEWFINDER_ZONE_TOP_LEFT);
	}
	
	@Override
	public void onStart()
	{
		getPrefs();
	}
	
	@Override
	public void onGUICreate()
	{
		//change shutter icon
		isRecording = false;
		
		MainScreen.guiManager.setShutterIcon(ShutterButton.RECORDER_START);
		
		onPreferenceCreate((PreferenceFragment)null);
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
	    int ImageSizeIdxPreference = Integer.parseInt(prefs.getString("imageSizePrefVideo", "2"));
	    int quality = 0;
	    switch (ImageSizeIdxPreference)
	    {
	    case 0:
	    	quality = CamcorderProfile.QUALITY_QCIF;
	    	quickControlIconID = R.drawable.gui_almalence_video_qcif;
	    	break;
	    case 1:
	    	quality = CamcorderProfile.QUALITY_CIF;
	    	quickControlIconID = R.drawable.gui_almalence_video_cif;
	    	break;
	    case 2:
	    	quality = CamcorderProfile.QUALITY_1080P;
	    	quickControlIconID = R.drawable.gui_almalence_video_1080;
	    	break;
	    case 3:
	    	quality = CamcorderProfile.QUALITY_720P;
	    	quickControlIconID = R.drawable.gui_almalence_video_720;
	    	break;
	    case 4:
	    	quality = CamcorderProfile.QUALITY_480P;
	    	quickControlIconID = R.drawable.gui_almalence_video_480;
	    	break;
	    }
	    
	    if ((!CamcorderProfile.hasProfile(quality)) || (MainScreen.getCameraMirrored()==true))
	    {
	    	ImageSizeIdxPreference=3;
	    	quality = CamcorderProfile.QUALITY_720P;
	    	quickControlIconID = R.drawable.gui_almalence_video_720;
	    	if (!CamcorderProfile.hasProfile(quality))
	    	{
	    		ImageSizeIdxPreference=4;
    	    	quality = CamcorderProfile.QUALITY_480P;
    	    	quickControlIconID = R.drawable.gui_almalence_video_480;    	    	
	    	}
	    }
	    
	    Editor editor = prefs.edit();
	    editor.putString("imageSizePrefVideo", String.valueOf(ImageSizeIdxPreference));
	    editor.commit();
	    
	    
	    List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout);
		for(int i = 0; i < specialLayout.getChildCount(); i++)
			specialView.add(specialLayout.getChildAt(i));

		for(int j = 0; j < specialView.size(); j++)
		{
			View view = specialView.get(j);
			int view_id = view.getId();
			int zoom_id = this.mRecordingTimeView.getId();
			if(view_id == zoom_id)
			{
				if(view.getParent() != null)
					((ViewGroup)view.getParent()).removeView(view);
				
				specialLayout.removeView(view);
			}
		}
		
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(((RelativeLayout) MainScreen.thiz.findViewById(R.id.pluginsLayout))
				.getWidth() / 4, ((RelativeLayout) MainScreen.thiz.findViewById(R.id.pluginsLayout))
				.getHeight() / 12);
		int topMargin = MainScreen.thiz.findViewById(R.id.paramsLayout).getHeight() + (int)MainScreen.thiz.getResources().getDimension(R.dimen.viewfinderViewsMarginTop);
		params.setMargins((int)(2*MainScreen.guiManager.getScreenDensity()), topMargin, 0, 0);
//		params.height = mainLayoutHeight/2;
		
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).addView(this.mRecordingTimeView, params);
		
		this.mRecordingTimeView.setLayoutParams(params);
		this.mRecordingTimeView.requestLayout();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout)).requestLayout();
	}
	
	@Override
	public void onQuickControlClick()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		Editor editor = prefs.edit();
		
        int ImageSizeIdxPreference = Integer.parseInt(prefs.getString("imageSizePrefVideo", "2"));
	    	
	    int quality = 0;
	    switch (ImageSizeIdxPreference)
	    {
	    case 0:
	    	quality = CamcorderProfile.QUALITY_CIF;
	    	quickControlIconID = R.drawable.gui_almalence_video_cif;
	    	editor.putString("imageSizePrefVideo", "1");
	    	break;
	    case 1:
	    	quality = CamcorderProfile.QUALITY_1080P;
	    	quickControlIconID = R.drawable.gui_almalence_video_1080;
	    	editor.putString("imageSizePrefVideo", "2");
	    	break;
	    case 2:
	    	quality = CamcorderProfile.QUALITY_720P;
	    	quickControlIconID = R.drawable.gui_almalence_video_720;
	    	editor.putString("imageSizePrefVideo", "3");
	    	break;
	    case 3:
	    	quality = CamcorderProfile.QUALITY_480P;
	    	quickControlIconID = R.drawable.gui_almalence_video_480;
	    	editor.putString("imageSizePrefVideo", "4");
	    	break;
	    case 4:
	    	quality = CamcorderProfile.QUALITY_QCIF;
	    	quickControlIconID = R.drawable.gui_almalence_video_qcif;
	    	editor.putString("imageSizePrefVideo", "0");
	    	break;
	    }
	    
	    editor.commit();
	    
	    if ((!CamcorderProfile.hasProfile(quality)) || ((MainScreen.getCameraMirrored()==true) && (quality == CamcorderProfile.QUALITY_1080P)) )
	    {
	    	ImageSizeIdxPreference = (Integer.parseInt(prefs.getString("imageSizePrefVideo", "2")) + 1)%5;
	    	editor.putString("imageSizePrefVideo", String.valueOf(ImageSizeIdxPreference));
	    	onQuickControlClick();
	    }
	    
	    Message msg = new Message();
		msg.what = PluginManager.MSG_RESTART_MAIN_SCREEN;				
		MainScreen.H.sendMessage(msg);
	}
	
	private static File getOutputMediaFile(){
		File saveDir = PluginManager.getInstance().GetSaveDir();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		int saveOption = Integer.parseInt(prefs.getString("exportName", "3"));
    	Calendar d = Calendar.getInstance();
    	String fileFormat = String.format("%04d%02d%02d_%02d%02d%02d",
        		d.get(Calendar.YEAR),
        		d.get(Calendar.MONTH)+1,
        		d.get(Calendar.DAY_OF_MONTH),
        		d.get(Calendar.HOUR_OF_DAY),
        		d.get(Calendar.MINUTE),
        		d.get(Calendar.SECOND));
    	fileFormat +=".mp4";
    		
        fileSaved = new File(saveDir, fileFormat);
        return fileSaved;
	}
	
	public void onResume()
	{
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        preferenceFocusMode = prefs.getString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, Camera.Parameters.FOCUS_MODE_AUTO);
        
	    int ImageSizeIdxPreference = Integer.parseInt(prefs.getString("imageSizePrefVideo", "2"));
	    switch (ImageSizeIdxPreference)
	    {
	    case 0:
	    	quickControlIconID = R.drawable.gui_almalence_video_qcif;
	    	break;
	    case 1:
	    	quickControlIconID = R.drawable.gui_almalence_video_cif;
	    	break;
	    case 2:
	    	quickControlIconID = R.drawable.gui_almalence_video_1080;
	    	break;
	    case 3:
	    	quickControlIconID = R.drawable.gui_almalence_video_720;
	    	break;
	    case 4:
	    	quickControlIconID = R.drawable.gui_almalence_video_480;
	    	break;
	    }
	    
	    shutterOff = false;
	}
	
	@Override
	public void onPause()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        prefs.edit().putString("FocusModeValue", preferenceFocusMode).commit();
        
        Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		if (isRecording) {
            // stop recording and release camera
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            camera.lock();         // take camera access back from MediaRecorder

            MainScreen.guiManager.lockControls = false;
            // inform the user that recording has stopped
            isRecording = false;
            showRecordingUI(isRecording);
            
            //change shutter icon
            MainScreen.guiManager.setShutterIcon(ShutterButton.RECORDER_START);
        }
		else
			releaseMediaRecorder();
		
		if(camera != null)
		{
			Camera.Parameters cp = camera.getParameters();
			cp.setRecordingHint(false);
			camera.setParameters(cp);
		}
	}
	
	@Override
	public void onCameraParametersSetup()
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		Camera.Parameters cp = camera.getParameters();
		cp.setRecordingHint(true);
		camera.setParameters(cp);
	}
	
	@Override
	public void SetCameraPreviewSize()
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		Camera.Parameters cp = camera.getParameters();
    	if(cp == null)
        	Log.e("VideoCapturePlugin", "MainScreen.SetCameraPreviewSize camera.getParameters returned null!");    	
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
	    int ImageSizeIdxPreference = Integer.parseInt(prefs.getString("imageSizePrefVideo", "2"));
	    	
	    boolean aspect169 = true;
	    switch (ImageSizeIdxPreference)
	    {
	    case 0:	    	
	    case 1:
	    case 4:
	    	aspect169 = false;
	    	break;
	    case 2:	    	
	    case 3:
	    	aspect169 = true;
	    	break;
	    }
	    
	    Camera.Size sz = getBestPreviewSize(aspect169);
	    cp.setPreviewSize(sz.width, sz.height);    	
    	
    	camera.setParameters(cp);
	}
	
	//Get optimal supported preview size with aspect ration 16:9 or 4:3
	private Camera.Size getBestPreviewSize(boolean aspect169)
	{
		Camera camera = MainScreen.thiz.getCamera();
		Camera.Parameters cp = camera.getParameters();
    	List<Camera.Size> cs = cp.getSupportedPreviewSizes();

    	Camera.Size sz = cs.get(0);
    	Long max_mpix = (long)sz.width*sz.height;
    	for (int i=0; i<cs.size(); ++i)
    	{
            Size s = cs.get(i); 
        	
        	Long lmpix = (long)s.width*s.height;
        	float ratio = (float)s.width/s.height;

        	
            if (Math.abs(ratio - 4/3.f)  < 0.1f && !aspect169)
            {
            	if(lmpix > max_mpix)
            	{
            		max_mpix = lmpix;
            		sz = s;
            	}
            }            
            else if (Math.abs(ratio - 16/9.f) < 0.15f && aspect169)
            {
            	if(lmpix > max_mpix)
            	{
            		max_mpix = lmpix;
            		sz = s;
            	}
            }
    	}
    	
    	return sz;
	}
	
	@Override
	public void SetCameraPictureSize() 
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		Camera.Parameters cp = camera.getParameters();
		
		if (cp.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
		{
			cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			camera.setParameters(cp);
		}
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO).commit();
	}
	
	private void releaseMediaRecorder()
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
        if (mMediaRecorder != null) {
            mMediaRecorder.reset();   // clear recorder configuration
            mMediaRecorder.release(); // release the recorder object
            mMediaRecorder = null;
            camera.lock();           // lock camera for later use
        }
    }

	@Override
	public void OnShutterClick()
	{
		if (shutterOff)
			return;
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		if (isRecording) 
		{
            // stop recording and release camera
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            camera.lock();         // take camera access back from MediaRecorder

            camera.startPreview();
            
            MainScreen.guiManager.lockControls = false;
            // inform the user that recording has stopped
            isRecording = false;
            showRecordingUI(isRecording);         
            
            //change shutter icon
            MainScreen.guiManager.setShutterIcon(ShutterButton.RECORDER_START);
            
	        ContentValues values=null;
	        values = new ContentValues(7);
	        values.put(ImageColumns.TITLE, fileSaved.getName().substring(0, fileSaved.getName().lastIndexOf(".")));
	        values.put(ImageColumns.DISPLAY_NAME, fileSaved.getName());
	        values.put(ImageColumns.DATE_TAKEN, System.currentTimeMillis());
	        values.put(ImageColumns.MIME_TYPE, "video/mp4");
	        values.put(ImageColumns.DATA, fileSaved.getAbsolutePath());
	        
	        String[] filesSavedNames= new String[1];
	        filesSavedNames[0] = fileSaved.toString();
	           
			MainScreen.thiz.getContentResolver().insert(Images.Media.EXTERNAL_CONTENT_URI, values);
//			MainScreen.FramesShot = true;
	        MediaScannerConnection.scanFile(MainScreen.thiz, filesSavedNames, null, null);
            PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putBoolean("ContinuousCapturing", false).commit();
            MainScreen.H.sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);
            
            Message msg = new Message();
			msg.what = PluginManager.MSG_RESTART_MAIN_SCREEN;				
			MainScreen.H.sendMessage(msg);
			
			shutterOff = true;
            
//	        new CountDownTimer(100, 100) {			 
//		     	public void onTick(long millisUntilFinished) {}
//
//			     public void onFinish() 
//			     {
//			    	 MainScreen.thiz.PauseMain();
//					 MainScreen.thiz.ResumeMain();			    	 
//			     }
//		  	}.start();
        } else 
        {
        	shutterOff=true;
        	mRecordingStartTime = SystemClock.uptimeMillis();
        	
        	mMediaRecorder = new MediaRecorder();
        	camera.stopPreview();
    		camera.unlock();
    	    mMediaRecorder.setCamera(camera);

    	    // Step 2: Set sources
    	    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    	    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);

    	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
    	    int ImageSizeIdxPreference = Integer.parseInt(prefs.getString("imageSizePrefVideo", "2"));
   	    	
    	    int quality = 0;
    	    switch (ImageSizeIdxPreference)
    	    {
    	    case 0:
    	    	quality = CamcorderProfile.QUALITY_QCIF;
    	    	break;
    	    case 1:
    	    	quality = CamcorderProfile.QUALITY_CIF;
    	    	break;
    	    case 2:
    	    	quality = CamcorderProfile.QUALITY_1080P;
    	    	break;
    	    case 3:
    	    	quality = CamcorderProfile.QUALITY_720P;
    	    	break;
    	    case 4:
    	    	quality = CamcorderProfile.QUALITY_480P;
    	    	break;
    	    }
    	    
    	    if ((!CamcorderProfile.hasProfile(quality)) || ((MainScreen.getCameraMirrored()==true) && (quality == CamcorderProfile.QUALITY_1080P)) )
   	    	{
    	    	ImageSizeIdxPreference=3;
    	    	quality = CamcorderProfile.QUALITY_720P;
    	    	if (!CamcorderProfile.hasProfile(quality))
    	    	{
    	    		ImageSizeIdxPreference=4;
        	    	quality = CamcorderProfile.QUALITY_480P;
        	    	
        	    	if (!CamcorderProfile.hasProfile(quality))
        	    	{
        	    		ImageSizeIdxPreference=0;
            	    	quality = CamcorderProfile.QUALITY_QCIF;
            	    	if (!CamcorderProfile.hasProfile(quality))
            	    	{
            	    		ImageSizeIdxPreference=1;
                	    	quality = CamcorderProfile.QUALITY_CIF;
                	    	if (!CamcorderProfile.hasProfile(quality))
                	    	{
                	    		return;
                	    	}
            	    	}
        	    	}
    	    	}
   	    	}
    	    Editor editor = prefs.edit();
    	    editor.putString("imageSizePrefVideo", String.valueOf(ImageSizeIdxPreference));
    	    editor.commit();

    	    // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
    	    
    	    try
    		{
    	    	CamcorderProfile pr = CamcorderProfile.get(quality);
    	    	mMediaRecorder.setProfile(pr);
	        } catch (Exception e) {
				e.printStackTrace();
				Log.e("Video", "On shutter pressed " + e.getMessage());
				return;
				//Toast.makeText(this, "Error during purchase " +e.getMessage(), Toast.LENGTH_LONG).show();
			}

    	    mMediaRecorder.setMaxDuration(mMaxVideoDurationInMs);
    	    
    	    // Step 4: Set output file
    	    mMediaRecorder.setOutputFile(getOutputMediaFile().toString());

    	    // Step 5: Set the preview output
    	    mMediaRecorder.setPreviewDisplay(MainScreen.thiz.surfaceHolder.getSurface());

   	    	mMediaRecorder.setOrientationHint(MainScreen.getCameraMirrored()?
   	    			(MainScreen.getWantLandscapePhoto()?MainScreen.orientationMain:MainScreen.orientationMain+180)
   	    			:MainScreen.orientationMain); 
    	    
    	    // Step 6: Prepare configured MediaRecorder
    	    try {
    	        mMediaRecorder.prepare();
    	    } catch (IllegalStateException e) {
    	        Log.d("Video", "IllegalStateException preparing MediaRecorder: " + e.getMessage());
    	        releaseMediaRecorder();
    	        return;
    	    } catch (IOException e) {
    	        Log.d("Video", "IOException preparing MediaRecorder: " + e.getMessage());
    	        releaseMediaRecorder();
    	        return;
    	    }
            // Camera is available and unlocked, MediaRecorder is prepared,
            // now you can start recording
            mMediaRecorder.start();

            //change shutter icon
            MainScreen.guiManager.setShutterIcon(ShutterButton.RECORDER_STOP);
            
            // inform the user that recording has started
            isRecording = true;
            showRecordingUI(isRecording);
            
            PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putBoolean("ContinuousCapturing", true).commit();
            
            new CountDownTimer(1000, 1000) {			 
   		     	public void onTick(long millisUntilFinished) {}

	   		     public void onFinish() {
	   		    	 shutterOff=false;
	   		    	 MainScreen.guiManager.lockControls = false;
	   		     }
   		  	}.start();
        }
	}
  
	@Override
	public void onPreferenceCreate(PreferenceFragment pf)
	{
    	CharSequence[] entries=new CharSequence[5];
		CharSequence[] entryValues=new CharSequence[5];

		int idx =0;
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_QCIF))
		{
			entries[idx]="176 x 144";
			entryValues[idx]="0";
			idx++;
		}
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_CIF))
		{
			entries[idx]="352 x 288";
			entryValues[idx]="1";
			idx++;
		}
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_1080P))
		{
			if (MainScreen.getCameraMirrored()==false)
			{
				entries[idx]="1080p";
				entryValues[idx]="2";
				idx++;
			}
		}
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P))
		{
			entries[idx]="720p";
			entryValues[idx]="3";
			idx++;
		}
		if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P))
		{
			entries[idx]="480p";
			entryValues[idx]="4";
			idx++;
		}
		
		CharSequence[] entriesFin=new CharSequence[idx];
		CharSequence[] entryValuesFin=new CharSequence[idx];
		
		for (int i=0; i<idx; i++)
		{
			entriesFin[i] = entries[i];
			entryValuesFin[i] = entryValues[i];
		}

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		int imageSizePrefVideo = Integer.parseInt(prefs.getString("imageSizePrefVideo", "2"));
		if (pf!=null)
    	{
			ListPreference lp = (ListPreference)pf.findPreference("imageSizePrefVideo");
			lp.setEntries(entriesFin);
			lp.setEntryValues(entryValuesFin);
			
			for (idx = 0; idx < entryValuesFin.length; ++idx)
			{
				if (Integer.valueOf(entryValuesFin[idx].toString()) == imageSizePrefVideo)
				{
					lp.setValueIndex(idx);
					break;
				}
			}
    	}
		else
    	{
			for (idx = 0; idx < entryValuesFin.length; ++idx)
			{
				if (Integer.valueOf(entryValuesFin[idx].toString()) == imageSizePrefVideo)
					break;
			}
    	}
	}
	
	private void getPrefs()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);

        CameraIDPreference = 0;
       
        readVideoPreferences(prefs);
    }
	
	private void showRecordingUI(boolean recording) {
        if (recording) {
        	mRecordingTimeView.setText("");
            mRecordingTimeView.setVisibility(View.VISIBLE);
            updateRecordingTime();
        } else {
        	mRecordingTimeView.setVisibility(View.GONE);
        }
    }
	
	//update recording time indicator.
	private void updateRecordingTime() {
        if (!isRecording) {
        	mRecordingTimeView.setText("00:00");
            return;
        }
        long now = SystemClock.uptimeMillis();
        long delta = now - mRecordingStartTime;

        // Starting a minute before reaching the max duration
        // limit, we'll countdown the remaining time instead.
        boolean countdownRemainingTime = (mMaxVideoDurationInMs != 0 && delta >= mMaxVideoDurationInMs - 60000);

        long deltaAdjusted = delta;
        if (countdownRemainingTime) {
            deltaAdjusted = Math.max(0, mMaxVideoDurationInMs - deltaAdjusted) + 999;
        }
        String text;

        long targetNextUpdateDelay;
        text = millisecondToTimeString(deltaAdjusted, false);
        targetNextUpdateDelay = 1000;

        mRecordingTimeView.setText(text);

        if (mRecordingTimeCountsDown != countdownRemainingTime) 
        {
            // Avoid setting the color on every update, do it only
            // when it needs changing.
            mRecordingTimeCountsDown = countdownRemainingTime;
            
            int color = MainScreen.thiz.getResources().getColor(R.color.recording_time_remaining_text);

            mRecordingTimeView.setTextColor(color);
        }

        long actualNextUpdateDelay = targetNextUpdateDelay - (delta % targetNextUpdateDelay);
        
        new CountDownTimer(actualNextUpdateDelay, actualNextUpdateDelay) {
		     public void onTick(long millisUntilFinished) {
		     }

		     public void onFinish() {
		    	 updateRecordingTime();
		     }
		  }.start();
    }
	
	private void readVideoPreferences(SharedPreferences prefs) 
	{
        Intent intent = MainScreen.thiz.getIntent();
        // Set video duration limit. The limit is read from the preference,
        // unless it is specified in the intent.
        if (intent.hasExtra(MediaStore.EXTRA_DURATION_LIMIT)) {
            int seconds =
                    intent.getIntExtra(MediaStore.EXTRA_DURATION_LIMIT, 0);
            mMaxVideoDurationInMs = 1000 * seconds;
        } else
            mMaxVideoDurationInMs = 0;
    }
	
	private static String millisecondToTimeString(long milliSeconds, boolean displayCentiSeconds) {
        long seconds = milliSeconds / 1000; // round down to compute seconds
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long remainderMinutes = minutes - (hours * 60);
        long remainderSeconds = seconds - (minutes * 60);

        StringBuilder timeStringBuilder = new StringBuilder();

        // Hours
        if (hours > 0) {
            if (hours < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(hours);

            timeStringBuilder.append(':');
        }

        // Minutes
        if (remainderMinutes < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderMinutes);
        timeStringBuilder.append(':');

        // Seconds
        if (remainderSeconds < 10) {
            timeStringBuilder.append('0');
        }
        timeStringBuilder.append(remainderSeconds);

        // Centi seconds
        if (displayCentiSeconds) {
            timeStringBuilder.append('.');
            long remainderCentiSeconds = (milliSeconds - seconds * 1000) / 10;
            if (remainderCentiSeconds < 10) {
                timeStringBuilder.append('0');
            }
            timeStringBuilder.append(remainderCentiSeconds);
        }

        return timeStringBuilder.toString();
    }

	@Override
	public void onAutoFocus(boolean paramBoolean, Camera paramCamera){}

	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera){}

	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera){}
	
}
