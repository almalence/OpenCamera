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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;

import android.app.Dialog;
import android.content.ContentValues;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.hardware.Camera.Size;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;
import android.media.MediaScannerConnection;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Message;
import android.os.SystemClock;
import android.preference.CheckBoxPreference;
import android.preference.ListPreference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.provider.MediaStore.Images;
import android.provider.MediaStore.Images.ImageColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.NumberPicker;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.almalence.SwapHeap;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginCapture;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
import com.almalence.opencam_plus.ui.AlmalenceGUI.ShutterButton;
import com.almalence.opencam_plus.ui.GUI;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.ui.AlmalenceGUI.ShutterButton;
import com.almalence.opencam.ui.GUI;
//-+- -->

import com.almalence.ui.RotateImageView;

/***
Implements basic functionality of Video capture.
***/

public class VideoCapturePlugin extends PluginCapture
{
	private boolean takingAlready=false;
	
    private boolean isRecording;
    
    
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
    
    private RotateImageView timeLapseButton;
    private RotateImageView takePictureButton;

    public boolean showRecording = false;
    
    private View buttonsLayout;    

    private boolean snapshotSupported = false;
    
    private boolean videoStabilization = false;
    
    
    private static Hashtable<Integer, Boolean> previewSizes = new Hashtable<Integer, Boolean>()
	{
		{
			put(CamcorderProfile.QUALITY_QCIF, false);
			put(CamcorderProfile.QUALITY_CIF, false);
			put(CamcorderProfile.QUALITY_480P, false);
			put(CamcorderProfile.QUALITY_720P, false);
			put(CamcorderProfile.QUALITY_1080P, false);
		}
	};
			
	private boolean qualityCIFSupported = false;
    private boolean qualityQCIFSupported = false;
    private boolean quality480Supported = false;
    private boolean quality720Supported = false;
    private boolean quality1080Supported = false;
    
	public VideoCapturePlugin()
	{
		super("com.almalence.plugins.videocapture",
			  R.xml.preferences_capture_video,
			  0,
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
		//Drawable img = MainScreen.mainContext.getResources().getDrawable( R.drawable.ic_recording_indicator );
		//mRecordingTimeView.setCompoundDrawablesWithIntrinsicBounds( img, null, null, null );		
		
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
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		
		//change shutter icon
		isRecording = false;
		prefs.edit().putBoolean("videorecording", false).commit();
		
		MainScreen.guiManager.setShutterIcon(ShutterButton.RECORDER_START);
		
		onPreferenceCreate((PreferenceFragment)null);
		
		int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "2"));
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
	    
//	    if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
//	    {
//	    	ImageSizeIdxPreference=3;
//	    	quality = CamcorderProfile.QUALITY_720P;
//	    	quickControlIconID = R.drawable.gui_almalence_video_720;
//	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
//	    	{
//	    		ImageSizeIdxPreference=4;
//    	    	quality = CamcorderProfile.QUALITY_480P;
//    	    	quickControlIconID = R.drawable.gui_almalence_video_480;    	    	
//	    	}
//	    }
	    if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
	    {
	    	ImageSizeIdxPreference=3;
	    	quality = CamcorderProfile.QUALITY_720P;
	    	quickControlIconID = R.drawable.gui_almalence_video_720;
	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
	    	{
	    		ImageSizeIdxPreference=4;
    	    	quality = CamcorderProfile.QUALITY_480P;
    	    	quickControlIconID = R.drawable.gui_almalence_video_480;    	    	
	    	}	    	
	    }	    
	    
	    Editor editor = prefs.edit();
	    editor.putString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", String.valueOf(ImageSizeIdxPreference));
	    editor.commit();
	    
	    
	    List<View> specialView = new ArrayList<View>();
		RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2);
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
		
//    	mDisplayOrientationCurrent = MainScreen.guiManager.getDisplayOrientation();
//    	int orientation = MainScreen.guiManager.getLayoutOrientation();
//    	mLayoutOrientationCurrent = orientation == 0 || orientation == 180? orientation: (orientation + 180)%360;
		
    	// Calculate right sizes for plugin's controls
		DisplayMetrics metrics = new DisplayMetrics();
		MainScreen.thiz.getWindowManager().getDefaultDisplay()
				.getMetrics(metrics);
		float fScreenDensity = metrics.density;
    			
    	int iIndicatorSize = (int) (MainScreen.mainContext.getResources()
				.getInteger(R.integer.infoControlHeight) * fScreenDensity);
//		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(((RelativeLayout) MainScreen.thiz.findViewById(R.id.pluginsLayout))
//				.getWidth() / 7, ((RelativeLayout) MainScreen.thiz.findViewById(R.id.pluginsLayout))
//				.getWidth() / 7);
    	RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(iIndicatorSize, iIndicatorSize);
		int topMargin = MainScreen.thiz.findViewById(R.id.paramsLayout).getHeight() + (int)MainScreen.thiz.getResources().getDimension(R.dimen.viewfinderViewsMarginTop);
		params.setMargins((int)(2*MainScreen.guiManager.getScreenDensity()), topMargin, 0, 0);
//		params.height = mainLayoutHeight/2;
		
		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_LEFT);
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2)).addView(this.mRecordingTimeView, params);
		
		this.mRecordingTimeView.setLayoutParams(params);
		this.mRecordingTimeView.requestLayout();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2)).requestLayout();		
		
		LayoutInflater inflator = MainScreen.thiz.getLayoutInflater();		
		buttonsLayout = inflator.inflate(R.layout.plugin_capture_video_layout, null, false);
		buttonsLayout.setVisibility(View.VISIBLE);
		
		timeLapseButton = (RotateImageView)buttonsLayout.findViewById(R.id.buttonPauseVideo);
		Camera camera = MainScreen.thiz.getCamera();
	    if (camera != null)
	    {
	    	Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
        	if (cp!=null)
        	{
        		if  (cp.isVideoSnapshotSupported())
        			snapshotSupported = true;
        	}
	    }
    	takePictureButton = (RotateImageView)buttonsLayout.findViewById(R.id.buttonCaptureImage);
	    
		timeLapseButton.setOnClickListener(new OnClickListener(){

			@Override
			public void onClick(View v) {			
				TimeLapseDialog();
			}
			
		});
		
		if (snapshotSupported)
		{
			takePictureButton.setOnClickListener(new OnClickListener(){
	
				@Override
				public void onClick(View v) {				
					takePicture();
				}
				
			});
		}
		
		List<View> specialView2 = new ArrayList<View>();
		RelativeLayout specialLayout2 = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2);
		for(int i = 0; i < specialLayout2.getChildCount(); i++)
			specialView2.add(specialLayout2.getChildAt(i));
		
		params = new RelativeLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
		params.height = (int)MainScreen.thiz.getResources().getDimension(R.dimen.videobuttons_size);
		
		params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);		
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2)).addView(this.buttonsLayout, params);
		
		this.buttonsLayout.setLayoutParams(params);
		this.buttonsLayout.requestLayout();
		
		((RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2)).requestLayout();
		
		if (snapshotSupported)
		{
			takePictureButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
			takePictureButton.invalidate();
			takePictureButton.requestLayout();
		}
		else
			takePictureButton.setVisibility(View.INVISIBLE);
		
		
		timeLapseButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
		timeLapseButton.invalidate();
		timeLapseButton.requestLayout();
	
		//List<Camera.Size> mResolutions = MainScreen.thiz.getCameraParameters().getSupportedVideoSizes();
		
		if(Build.MODEL.contains(MainScreen.deviceSS3_01) || Build.MODEL.contains(MainScreen.deviceSS3_02) ||
				Build.MODEL.contains(MainScreen.deviceSS3_03) || Build.MODEL.contains(MainScreen.deviceSS3_04) ||
				Build.MODEL.contains(MainScreen.deviceSS3_05) || Build.MODEL.contains(MainScreen.deviceSS3_06) ||
				Build.MODEL.contains(MainScreen.deviceSS3_07) || Build.MODEL.contains(MainScreen.deviceSS3_08) ||
				Build.MODEL.contains(MainScreen.deviceSS3_09) || Build.MODEL.contains(MainScreen.deviceSS3_10) ||
				Build.MODEL.contains(MainScreen.deviceSS3_11) || Build.MODEL.contains(MainScreen.deviceSS3_12) ||	Build.MODEL.contains(MainScreen.deviceSS3_13))
			takePictureButton.setVisibility(View.INVISIBLE);
	}
	
	@Override
	public void onQuickControlClick()
	{
		if (isRecording)
			return;
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		Editor editor = prefs.edit();
		
        int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "2"));
	    	
	    int quality = 0;
	    switch (ImageSizeIdxPreference)
	    {
	    case 0:
	    	quality = CamcorderProfile.QUALITY_CIF;
	    	quickControlIconID = R.drawable.gui_almalence_video_cif;
	    	editor.putString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "1");
	    	break;
	    case 1:
	    	quality = CamcorderProfile.QUALITY_1080P;
	    	quickControlIconID = R.drawable.gui_almalence_video_1080;
	    	editor.putString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "2");
	    	break;
	    case 2:
	    	quality = CamcorderProfile.QUALITY_720P;
	    	quickControlIconID = R.drawable.gui_almalence_video_720;
	    	editor.putString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "3");
	    	break;
	    case 3:
	    	quality = CamcorderProfile.QUALITY_480P;
	    	quickControlIconID = R.drawable.gui_almalence_video_480;
	    	editor.putString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "4");
	    	break;
	    case 4:
	    	quality = CamcorderProfile.QUALITY_QCIF;
	    	quickControlIconID = R.drawable.gui_almalence_video_qcif;
	    	editor.putString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "0");
	    	break;
	    }
	    
	    editor.commit();
	    
	    if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
	    {
	    	ImageSizeIdxPreference = (Integer.parseInt(prefs.getString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "2")) + 1)%5;
	    	editor.putString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", String.valueOf(ImageSizeIdxPreference));
	    	onQuickControlClick();
	    }
	    
	    Camera camera = MainScreen.thiz.getCamera();
	    if (camera != null)
	    {
	    	camera.stopPreview();
	        Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
	        if (cp!=null)
	        {
	        	SetCameraPreviewSize(cp);
	        	MainScreen.guiManager.setupViewfinderPreviewSize(cp);
	        }
	        camera.startPreview();
	        
	        Message msg = new Message();
			msg.arg1 = PluginManager.MSG_PREVIEW_CHANGED;
			msg.what = PluginManager.MSG_BROADCAST;
			MainScreen.H.sendMessage(msg);
	    }
        
//	    Message msg = new Message();
//		msg.what = PluginManager.MSG_RESTART_MAIN_SCREEN;				
//		MainScreen.H.sendMessage(msg);
	}
	
	@Override
    public void onOrientationChanged(int orientation)
    {
		if(mRecordingTimeView != null)
		{
			mRecordingTimeView.setRotation(MainScreen.guiManager.getDisplayRotation()); 
			mRecordingTimeView.invalidate();
		}
		if (snapshotSupported)
		{
			if (takePictureButton!=null)
			{
				takePictureButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
				takePictureButton.invalidate();
				takePictureButton.requestLayout();
			}
		}
		if (timeLapseButton!=null)
		{
			timeLapseButton.setOrientation(MainScreen.guiManager.getLayoutOrientation());
			timeLapseButton.invalidate();
			timeLapseButton.requestLayout();
		}
    }
	
	private static File getOutputMediaFile(){
		File saveDir = PluginManager.getInstance().GetSaveDir();

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
//        
//	    int ImageSizeIdxPreference = Integer.parseInt(prefs.getString("imageSizePrefVideo", "2"));
//	    int quality = 0;
//	    switch (ImageSizeIdxPreference)
//	    {
//	    case 0:
//	    	quality = CamcorderProfile.QUALITY_QCIF;
//	    	quickControlIconID = R.drawable.gui_almalence_video_qcif;
//	    	break;
//	    case 1:
//	    	quality = CamcorderProfile.QUALITY_CIF;
//	    	quickControlIconID = R.drawable.gui_almalence_video_cif;
//	    	break;
//	    case 2:
//	    	quality = CamcorderProfile.QUALITY_1080P;
//	    	quickControlIconID = R.drawable.gui_almalence_video_1080;
//	    	break;
//	    case 3:
//	    	quality = CamcorderProfile.QUALITY_720P;
//	    	quickControlIconID = R.drawable.gui_almalence_video_720;
//	    	break;
//	    case 4:
//	    	quality = CamcorderProfile.QUALITY_480P;
//	    	quickControlIconID = R.drawable.gui_almalence_video_480;
//	    	break;
//	    }
//	    
//	    if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
//	    {
//	    	ImageSizeIdxPreference=3;
//	    	quality = CamcorderProfile.QUALITY_720P;
//	    	quickControlIconID = R.drawable.gui_almalence_video_720;
//	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
//	    	{
//	    		ImageSizeIdxPreference=4;
//    	    	quality = CamcorderProfile.QUALITY_480P;
//    	    	quickControlIconID = R.drawable.gui_almalence_video_480;    	    	
//	    	}
//	    }
//	    
//	    Editor editor = prefs.edit();
//	    editor.putString("imageSizePrefVideo", String.valueOf(ImageSizeIdxPreference));
//	    editor.commit();
	    
	    PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putBoolean("ContinuousCapturing", true).commit();
	    
	    shutterOff = false;
	    showRecording=false;
	    
	    swChecked = false;
	    interval = 0;
		measurementVal = 0;
	}
	
	@Override
	public void onPause()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        prefs.edit().putString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, preferenceFocusMode).commit();
        
        Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
		if (isRecording) {
            // stop recording and release camera
            mMediaRecorder.stop();  // stop the recording
            releaseMediaRecorder(); // release the MediaRecorder object
            camera.lock();         // take camera access back from MediaRecorder

            MainScreen.guiManager.lockControls = false;
            
            Message msg = new Message();
	  		msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
	  		msg.what = PluginManager.MSG_BROADCAST;
	  		MainScreen.H.sendMessage(msg);
		  		
            // inform the user that recording has stopped
            isRecording = false;
            showRecordingUI(isRecording);
            prefs.edit().putBoolean("videorecording", false).commit();
            
            //change shutter icon
            MainScreen.guiManager.setShutterIcon(ShutterButton.RECORDER_START);
        }
		else
			releaseMediaRecorder();
		
		if(camera != null)
		{
			Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
			cp.setRecordingHint(false);
			MainScreen.thiz.setCameraParameters(cp);
		}
		
		if(this.buttonsLayout != null)
		{
			List<View> specialView = new ArrayList<View>();
			RelativeLayout specialLayout = (RelativeLayout)MainScreen.thiz.findViewById(R.id.specialPluginsLayout2);
			for(int i = 0; i < specialLayout.getChildCount(); i++)
				specialView.add(specialLayout.getChildAt(i));
	
			for(int j = 0; j < specialView.size(); j++)
			{
				View view = specialView.get(j);
				int view_id = view.getId();
				int layout_id = this.buttonsLayout.getId();
				if(view_id == layout_id)
				{
					if(view.getParent() != null)
						((ViewGroup)view.getParent()).removeView(view);
					
					specialLayout.removeView(view);
				}
			}
		}
		
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putBoolean("ContinuousCapturing", false).commit();
	}
	
	@Override
	public void onCameraParametersSetup()
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
    	
    	this.qualityQCIFSupported = false;
    	this.qualityCIFSupported = false;
    	this.quality480Supported = false;
    	this.quality720Supported = false;
    	this.quality1080Supported = false;
    	previewSizes.put(CamcorderProfile.QUALITY_QCIF, false);
    	previewSizes.put(CamcorderProfile.QUALITY_CIF, false);
    	previewSizes.put(CamcorderProfile.QUALITY_480P, false);
    	previewSizes.put(CamcorderProfile.QUALITY_720P, false);
    	previewSizes.put(CamcorderProfile.QUALITY_1080P, false);
    	
		Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
		List<Size> psz = cp.getSupportedPreviewSizes();
		if(psz.contains(camera.new Size(176,144)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_QCIF, true);
			this.qualityQCIFSupported = true;
		}
		if(psz.contains(camera.new Size(352,288)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_CIF, true);
			this.qualityCIFSupported = true;
		}
		if(psz.contains(camera.new Size(640,480)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_480P, true);
			this.quality480Supported = true;
		}
		if(psz.contains(camera.new Size(1280,720)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_720P, true);
			this.quality720Supported = true;
		}
		if(psz.contains(camera.new Size(1920,1080)) || psz.contains(camera.new Size(1920,1088)))
		{
			previewSizes.put(CamcorderProfile.QUALITY_1080P, true);
			this.quality1080Supported = true;
		}
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        //preferenceFocusMode = prefs.getString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, Camera.Parameters.FOCUS_MODE_AUTO);
        
	    int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "2"));
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
	    
	    if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
	    {
	    	ImageSizeIdxPreference=3;
	    	quality = CamcorderProfile.QUALITY_720P;
	    	quickControlIconID = R.drawable.gui_almalence_video_720;
	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
	    	{
	    		ImageSizeIdxPreference=4;
    	    	quality = CamcorderProfile.QUALITY_480P;
    	    	quickControlIconID = R.drawable.gui_almalence_video_480;    	    	
	    	}
	    }
	    
	    Editor editor = prefs.edit();
	    editor.putString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", String.valueOf(ImageSizeIdxPreference));
	    editor.commit();
	    
		cp.setRecordingHint(true);
		MainScreen.thiz.setCameraParameters(cp);
	}
	
	@Override
	public void SetCameraPreviewSize(Camera.Parameters cp)
	{
		Camera camera = MainScreen.thiz.getCamera();
    	if (null==camera)
    		return;
    	if(cp == null)
        	Log.e("VideoCapturePlugin", "MainScreen.SetCameraPreviewSize MainScreen.thiz.getCameraParameters returned null!");    	
    	
    	SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
	    int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "2"));
	    	
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
    	
    	MainScreen.thiz.setCameraParameters(cp);
	}
	
	//Get optimal supported preview size with aspect ration 16:9 or 4:3
	private Camera.Size getBestPreviewSize(boolean aspect169)
	{
		Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
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
		Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
		
		cp.setPictureSize(MainScreen.getImageWidth(), MainScreen.getImageHeight());
		cp.setJpegQuality(95);
		
		if (cp.getSupportedFocusModes().contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO))
		{
			cp.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
			MainScreen.thiz.setCameraParameters(cp);
		}
		PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putString(MainScreen.getCameraMirrored()? GUI.sRearFocusModePref : GUI.sFrontFocusModePref, Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO).commit();
	}
	
	private void releaseMediaRecorder()
	{
		captureRate = 24;
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

	//*captureRate/24 - to get correct recording time
	double captureRate = 24;
	
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
			try
			{
				mMediaRecorder.stop();  // stop the recording
			}
			catch (Exception e) {
				e.printStackTrace();
				Log.e("video OnShutterClick", "mMediaRecorder.stop() exception: " + e.getMessage());
			}
            releaseMediaRecorder(); // release the MediaRecorder object
            camera.lock();         // take camera access back from MediaRecorder

            camera.stopPreview();
	        Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
	        if (cp!=null)
	        {
	        	SetCameraPreviewSize(cp);
	        	MainScreen.guiManager.setupViewfinderPreviewSize(cp);	        	
	   	    	if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH && videoStabilization)
	   	    		MainScreen.thiz.setVideoStabilization(false);
	        }
	        camera.startPreview();
            
            MainScreen.guiManager.lockControls = false;
            // inform the user that recording has stopped
            isRecording = false;
            showRecordingUI(isRecording);
            PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putBoolean("videorecording", false).commit();
            
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
	        MediaScannerConnection.scanFile(MainScreen.thiz, filesSavedNames, null, null);
            
            new CountDownTimer(500, 500) {			 
   		     	public void onTick(long millisUntilFinished) {}

	   		     public void onFinish() {
	   		    	MainScreen.H.sendEmptyMessage(PluginManager.MSG_EXPORT_FINISHED);
	   		    	shutterOff = false;
	   				showRecording=false;
	   		     }
   		  	}.start();  		  	


//   		 if(Build.MODEL.contains(MainScreen.deviceSS3_01) || Build.MODEL.contains(MainScreen.deviceSS3_02) ||
// 				Build.MODEL.contains(MainScreen.deviceSS3_03) || Build.MODEL.contains(MainScreen.deviceSS3_04) ||
// 				Build.MODEL.contains(MainScreen.deviceSS3_05) || Build.MODEL.contains(MainScreen.deviceSS3_06) ||
// 				Build.MODEL.contains(MainScreen.deviceSS3_07) || Build.MODEL.contains(MainScreen.deviceSS3_08) ||
// 				Build.MODEL.contains(MainScreen.deviceSS3_09) || Build.MODEL.contains(MainScreen.deviceSS3_10) ||
// 				Build.MODEL.contains(MainScreen.deviceSS3_11) || Build.MODEL.contains(MainScreen.deviceSS3_12) ||	Build.MODEL.contains(MainScreen.deviceSS3_13))
//   		  	{
//   		  		MainScreen.guiManager.lockControls = false;
//   		  		
//   		  		Message msg = new Message();
//   		  		msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
//   		  		msg.what = PluginManager.MSG_BROADCAST;
//   		  		MainScreen.H.sendMessage(msg);
//   		  	}
			
        } else 
        {
//        	if(Build.MODEL.contains(MainScreen.deviceSS3_01) || Build.MODEL.contains(MainScreen.deviceSS3_02) ||
//    				Build.MODEL.contains(MainScreen.deviceSS3_03) || Build.MODEL.contains(MainScreen.deviceSS3_04) ||
//    				Build.MODEL.contains(MainScreen.deviceSS3_05) || Build.MODEL.contains(MainScreen.deviceSS3_06) ||
//    				Build.MODEL.contains(MainScreen.deviceSS3_07) || Build.MODEL.contains(MainScreen.deviceSS3_08) ||
//    				Build.MODEL.contains(MainScreen.deviceSS3_09) || Build.MODEL.contains(MainScreen.deviceSS3_10) ||
//    				Build.MODEL.contains(MainScreen.deviceSS3_11) || Build.MODEL.contains(MainScreen.deviceSS3_12) ||	Build.MODEL.contains(MainScreen.deviceSS3_13))
//   		  	{
//   		  		MainScreen.guiManager.lockControls = true;
//   		  		
//   		  		Message msg = new Message();
//   		  		msg.arg1 = PluginManager.MSG_CONTROL_LOCKED;
//   		  		msg.what = PluginManager.MSG_BROADCAST;
//   		  		MainScreen.H.sendMessage(msg);
//   		  	}
        
        	Date curDate = new Date();
        	SessionID = curDate.getTime();
        	
   	    	if(Build.VERSION.SDK_INT > Build.VERSION_CODES.ICE_CREAM_SANDWICH && videoStabilization)
   	    		MainScreen.thiz.setVideoStabilization(true);
        	
        	shutterOff=true;
        	mRecordingStartTime = SystemClock.uptimeMillis();
        	
        	mMediaRecorder = new MediaRecorder();
        	camera.stopPreview();
    		camera.unlock();
    	    mMediaRecorder.setCamera(camera);

    	    // Step 2: Set sources
    	    mMediaRecorder.setAudioSource(MediaRecorder.AudioSource.CAMCORDER);
    	    mMediaRecorder.setVideoSource(MediaRecorder.VideoSource.CAMERA);
    	    
    	    SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
    	    int ImageSizeIdxPreference = Integer.parseInt(prefs.getString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "2"));
   	    	
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
    	    
//    	    if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
//   	    	{
//    	    	ImageSizeIdxPreference=3;
//    	    	quality = CamcorderProfile.QUALITY_720P;
//    	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
//    	    	{
//    	    		ImageSizeIdxPreference=4;
//        	    	quality = CamcorderProfile.QUALITY_480P;
//        	    	
//        	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
//        	    	{
//        	    		ImageSizeIdxPreference=0;
//            	    	quality = CamcorderProfile.QUALITY_QCIF;
//            	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
//            	    	{
//            	    		ImageSizeIdxPreference=1;
//                	    	quality = CamcorderProfile.QUALITY_CIF;
//                	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
//                	    	{
//                	    		return;
//                	    	}
//            	    	}
//        	    	}
//    	    	}
//   	    	}
    	    
    	    boolean useProfile = true;
    	    if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
   	    	{
    	    	ImageSizeIdxPreference=3;
    	    	quality = CamcorderProfile.QUALITY_720P;
    	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
    	    	{
    	    		ImageSizeIdxPreference=4;
        	    	quality = CamcorderProfile.QUALITY_480P;
        	    	
        	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
        	    	{
        	    		ImageSizeIdxPreference=0;
            	    	quality = CamcorderProfile.QUALITY_QCIF;
            	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality) && !previewSizes.get(quality))
            	    	{
            	    		ImageSizeIdxPreference=1;
                	    	quality = CamcorderProfile.QUALITY_CIF;
                	    	if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
                	    	{
                	    		return;
                	    	}
            	    	}
            	    	else if(!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
            	    		useProfile = false;
            	    	else
            	    		return;
        	    	}
        	    	else if(!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
        	    		useProfile = false;
    	    	}
    	    	else if(!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
    	    		useProfile = false;
   	    	}
    	    else if(!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
    	    	useProfile = false;

    	    Editor editor = prefs.edit();
    	    editor.putString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", String.valueOf(ImageSizeIdxPreference));
    	    editor.commit();

    	    // Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
    	        	    
    	    try
    		{
    	    	
    	    	try
        		{
	    	    	if (swChecked)
	    	    	{
	    	    		int qualityTimeLapse = quality;
	    	    		//if time lapse activated
	    	    		switch(quality)
	    	    		{
	    	    		 case CamcorderProfile.QUALITY_QCIF:
	    	     	    	quality = CamcorderProfile.QUALITY_TIME_LAPSE_QCIF;
	    	     	    	break;
	    	     	    case CamcorderProfile.QUALITY_CIF:
	    	     	    	quality = CamcorderProfile.QUALITY_TIME_LAPSE_CIF;
	    	     	    	break;
	    	     	    case CamcorderProfile.QUALITY_1080P:
	    	     	    	quality = CamcorderProfile.QUALITY_TIME_LAPSE_1080P;
	    	     	    	break;
	    	     	    case CamcorderProfile.QUALITY_720P:
	    	     	    	quality = CamcorderProfile.QUALITY_TIME_LAPSE_720P;
	    	     	    	break;
	    	     	    case CamcorderProfile.QUALITY_480P:
	    	     	    	quality = CamcorderProfile.QUALITY_TIME_LAPSE_480P;
	    	     	    	break;
	    	    		}
	    	    		if (!CamcorderProfile.hasProfile(MainScreen.CameraIndex, quality))
	    	   	    	{
	    	    			Toast.makeText(MainScreen.thiz, "Time lapse not supported", Toast.LENGTH_LONG).show();
	    	   	    	}
	    	    		else
	    	    			quality = qualityTimeLapse;
	    	    	}
        		} catch (Exception e) {
    				e.printStackTrace();
    				Log.e("Video", "Time lapse error catched" + e.getMessage());
    				swChecked = false;
    				
    				MainScreen.guiManager.lockControls = false;
       		  		
       		  		Message msg = new Message();
       		  		msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
       		  		msg.what = PluginManager.MSG_BROADCAST;
       		  		MainScreen.H.sendMessage(msg);
    			}
    	    	
    	    	if(useProfile)
    	    	{
	    	    	CamcorderProfile pr = CamcorderProfile.get(MainScreen.CameraIndex, quality);
	    	    	mMediaRecorder.setProfile(pr);
    	    	}
    	    	else
    	    	{
	    	    	mMediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
	    	    	mMediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
	    	    	mMediaRecorder.setVideoEncoder(MediaRecorder.VideoEncoder.H264);
	    	    	
	    	    	//mMediaRecorder.setVideoEncodingBitRate(5246000);
	    	    	
	    	    	Size sz = null;
    	    		//if time lapse activated
    	    		switch(quality)
    	    		{
    	    		 case CamcorderProfile.QUALITY_QCIF:
    	     	    	sz = camera.new Size(176,144);
    	     	    	break;
    	     	    case CamcorderProfile.QUALITY_CIF:
    	     	    	sz = camera.new Size(352,288);
    	     	    	break;
    	     	    case CamcorderProfile.QUALITY_1080P:
    	     	    {
    	     	    	Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
    	     			List<Size> psz = cp.getSupportedPreviewSizes();    	     			
    	     	    	sz = camera.new Size(1920,1080);
    	     	    	if(!psz.contains(sz))
    	     	    		sz = camera.new Size(1920,1088);
    	     	    } break;
    	     	    case CamcorderProfile.QUALITY_720P:
    	     	    	sz = camera.new Size(1280,720);
    	     	    	break;
    	     	    case CamcorderProfile.QUALITY_480P:
    	     	    	sz = camera.new Size(640,480);
    	     	    	break;
    	    		}
	    	    	mMediaRecorder.setVideoSize(sz.width, sz.height);	    	    	
    	    	}
    	    	
//    	    	Camera.Parameters params = MainScreen.thiz.getCameraParameters();
//    	    	params.set("cam_mode",1);
//    	    	MainScreen.thiz.setCameraParameters(params);
    	    	
    	    	if (swChecked)
    	    	{
    	    		double val1 = Double.valueOf(stringInterval[interval]);
    	    		int val2 = measurementVal;
    	    		switch (val2)
    	    		{
    	    		case 0:
    	    			val2 = 1;
    	    			break;
    	    		case 1:
    	    			val2 = 60;
    	    			break;
    	    		case 2:
    	    			val2 = 3600;
    	    			break;
    	    		}
    	    		captureRate = 1/(val1 * val2);
    	    		mMediaRecorder.setCaptureRate(captureRate);
    	    	}
//    	    	Camera.Parameters cp = MainScreen.thiz.getCameraParameters();
//    	        if (cp!=null)
//    	        {
//    	        	Log.e("Video", "cp null");
//    	        }
//    	    	List<int[]> frame = cp.getSupportedPreviewFpsRange();
    	    	//mMediaRecorder.setCaptureRate(0.1);
    	    	
	        } catch (Exception e) {
				e.printStackTrace();
				Log.e("Video", "On shutter pressed " + e.getMessage());
				
				MainScreen.guiManager.lockControls = false;
   		  		Message msg = new Message();
   		  		msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
   		  		msg.what = PluginManager.MSG_BROADCAST;
   		  		MainScreen.H.sendMessage(msg);
	   		  	releaseMediaRecorder(); // release the MediaRecorder object
	            camera.lock();         // take camera access back from MediaRecorder
	            camera.stopPreview();
		        camera.startPreview();
		        
				return;
				//Toast.makeText(this, "Error during purchase " +e.getMessage(), Toast.LENGTH_LONG).show();
			}

    	    //mMediaRecorder.setMaxDuration(mMaxVideoDurationInMs);
    	    
    	    // Step 4: Set output file
    	    mMediaRecorder.setOutputFile(getOutputMediaFile().toString());

    	    // Step 5: Set the preview output
    	    mMediaRecorder.setPreviewDisplay(MainScreen.thiz.surfaceHolder.getSurface());

   	    	mMediaRecorder.setOrientationHint(
   	    			MainScreen.getCameraMirrored()?
   	    			(MainScreen.getWantLandscapePhoto()?MainScreen.orientationMain:(MainScreen.orientationMain+180)%360)
   	    			:MainScreen.orientationMain);  	    	
   	    	
    	    // Step 6: Prepare configured MediaRecorder
    	    try {
    	        mMediaRecorder.prepare();
    	        
                // Camera is available and unlocked, MediaRecorder is prepared,
                // now you can start recording
                mMediaRecorder.start();

    	    } catch (Exception e) 
    	    {
    	        Log.d("Video", "Exception preparing MediaRecorder: " + e.getMessage());
    	        releaseMediaRecorder();
    	        Toast.makeText(MainScreen.thiz, "Failed to start video recording", Toast.LENGTH_LONG).show();
    	        
    	        MainScreen.guiManager.lockControls = false;
   		  		Message msg = new Message();
   		  		msg.arg1 = PluginManager.MSG_CONTROL_UNLOCKED;
   		  		msg.what = PluginManager.MSG_BROADCAST;
   		  		MainScreen.H.sendMessage(msg);
	            camera.lock();         // take camera access back from MediaRecorder
	            camera.stopPreview();
		        camera.startPreview();
		        
    	        return;
    	    }

            //change shutter icon
            MainScreen.guiManager.setShutterIcon(ShutterButton.RECORDER_STOP);
            
            // inform the user that recording has started
            isRecording = true;
            showRecordingUI(isRecording);
            prefs.edit().putBoolean("videorecording", true).commit();
            
            //PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext).edit().putBoolean("ContinuousCapturing", true).commit();
            
            new CountDownTimer(1000, 1000) {			 
   		     	public void onTick(long millisUntilFinished) {}

	   		     public void onFinish() {
	   		    	 shutterOff=false;
	   		    	if(!(Build.MODEL.contains(MainScreen.deviceSS3_01) || Build.MODEL.contains(MainScreen.deviceSS3_02) ||
	   						Build.MODEL.contains(MainScreen.deviceSS3_03) || Build.MODEL.contains(MainScreen.deviceSS3_04) ||
	   						Build.MODEL.contains(MainScreen.deviceSS3_05) || Build.MODEL.contains(MainScreen.deviceSS3_06) ||
	   						Build.MODEL.contains(MainScreen.deviceSS3_07) || Build.MODEL.contains(MainScreen.deviceSS3_08) ||
	   						Build.MODEL.contains(MainScreen.deviceSS3_09) || Build.MODEL.contains(MainScreen.deviceSS3_10) ||
	   						Build.MODEL.contains(MainScreen.deviceSS3_11) || Build.MODEL.contains(MainScreen.deviceSS3_12) ||	Build.MODEL.contains(MainScreen.deviceSS3_13)))
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
		if (CamcorderProfile.hasProfile(MainScreen.CameraIndex, CamcorderProfile.QUALITY_QCIF) || this.qualityQCIFSupported)
		{
			entries[idx]="176 x 144";
			entryValues[idx]="0";
			idx++;
		}
		if (CamcorderProfile.hasProfile(MainScreen.CameraIndex, CamcorderProfile.QUALITY_CIF) || this.qualityCIFSupported)
		{
			entries[idx]="352 x 288";
			entryValues[idx]="1";
			idx++;
		}
		if (CamcorderProfile.hasProfile(MainScreen.CameraIndex, CamcorderProfile.QUALITY_1080P) || this.quality1080Supported)
		{
			entries[idx]="1080p";
			entryValues[idx]="2";
			idx++;
		}
		if (CamcorderProfile.hasProfile(MainScreen.CameraIndex, CamcorderProfile.QUALITY_720P) || this.quality720Supported)
		{
			entries[idx]="720p";
			entryValues[idx]="3";
			idx++;
		}
		if (CamcorderProfile.hasProfile(MainScreen.CameraIndex, CamcorderProfile.QUALITY_480P) || this.quality480Supported)
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
		int imageSizePrefVideo = Integer.parseInt(prefs.getString(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront", "2"));
		if (pf!=null)
    	{
			ListPreference lp = (ListPreference)pf.findPreference("imageSizePrefVideoBack");
	        ListPreference lp2 = (ListPreference)pf.findPreference("imageSizePrefVideoFront");
	        
	        PreferenceCategory cat = (PreferenceCategory)pf.findPreference("Pref_VideoCapture_Category");
	        if(MainScreen.CameraIndex == 0 && lp2 != null && cat != null)
	        {
	        	cat.removePreference(lp2);
	        }
	        else if(lp != null && lp2 != null && cat != null)
	        {
	        	cat.removePreference(lp);
	        	lp = lp2;
	        }
	        else if(lp == null)
	        	lp = lp2;
	        
//			ListPreference lp = (ListPreference)pf.findPreference(MainScreen.CameraIndex == 0? "imageSizePrefVideoBack" : "imageSizePrefVideoFront");
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
		
		if (pf!=null && !MainScreen.thiz.mVideoStabilizationSupported)
    	{
			PreferenceCategory cat = (PreferenceCategory)pf.findPreference("Pref_VideoCapture_Category");
			CheckBoxPreference cp = (CheckBoxPreference) pf.findPreference("videoStabilizationPref");
			if(cp != null && cat != null)
				cat.removePreference(cp);
    	}
		
	}
	
	private void getPrefs()
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);

        CameraIDPreference = 0;
        
        videoStabilization = prefs.getBoolean("videoStabilizationPref", false);
       
        readVideoPreferences(prefs);
    }
	
	private void showRecordingUI(boolean recording) {
        if (recording) {
    		mRecordingTimeView.setRotation(MainScreen.guiManager.getDisplayRotation()); 
    		mRecordingTimeView.invalidate();
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

        long deltaAdjusted = (long)(delta*captureRate/24);
        //*captureRate/24 needed for time lapse
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
		  
		  //show recording shutter
		  if (showRecording)
		  {
			  MainScreen.guiManager.setShutterIcon(ShutterButton.RECORDER_STOP);
			  showRecording=false;
		  }
		  else
		  {
			  MainScreen.guiManager.setShutterIcon(ShutterButton.RECORDER_RECORDING);
			  showRecording=true;
		  }
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

	public void takePicture()
	{
		if(takingAlready)
			return;
		
		takingAlready = true;		
		
		Camera camera = MainScreen.thiz.getCamera();
		if (camera != null)		// paranoia
		{
			//MainScreen.thiz.PlayShutter();
			
	    	try {
	    		camera.setPreviewCallback(null);
	    		camera.takePicture(null, null, null, MainScreen.thiz);
			} catch (Exception e) {
				e.printStackTrace();
				Log.e("Video capture still image", "takePicture exception: " + e.getMessage());
				takingAlready = false;				
			}
		}
		else
		{
			takingAlready = false;			
		}
	}

	//timelapse values
	public int interval = 0;
	public int measurementVal = 0;
	public boolean swChecked = false;
	
	String[] stringInterval = { "0.1", "0.2", "0.3", "0.4", "0.5", "1", "1.5", "2", "2.5", "3", "4", "5", "6", "10", "12", "15", "24"};
	String[] stringMeasurement = { "seconds", "minutes", "hours"};
	public void TimeLapseDialog()
	{
		if (isRecording)
			return;

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
		interval = Integer.valueOf(prefs.getString("timelapseInterval", "0"));
		measurementVal = Integer.valueOf(prefs.getString("timelapseMeasurementVal", "0"));
		
		//show time lapse settings
		final Dialog d = new Dialog(MainScreen.thiz);
        d.setTitle("Time lapse");
        d.setContentView(R.layout.plugin_capture_video_timelapse_dialog);
        final Button bSet = (Button) d.findViewById(R.id.button1);
        final NumberPicker np = (NumberPicker) d.findViewById(R.id.numberPicker1);
        np.setMaxValue(16);
        np.setMinValue(0);
        np.setValue(interval);
        np.setDisplayedValues(stringInterval);
        np.setWrapSelectorWheel(false);
        np.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        
        final NumberPicker np2 = (NumberPicker) d.findViewById(R.id.numberPicker2);
        np2.setMaxValue(2);
        np2.setMinValue(0);
        np2.setValue(measurementVal);
        np2.setWrapSelectorWheel(false);
        np2.setDisplayedValues(stringMeasurement);
        np2.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        
        final Switch sw = (Switch) d.findViewById(R.id.timelapse_switcher);
        
        //disable/enable controls in dialog
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) 
			{
				if (false == sw.isChecked())
		        {
		        	np2.setEnabled(false);
		        	np.setEnabled(false);
		        	swChecked = false;
		        }
				else
				{
					np2.setEnabled(true);
		        	np.setEnabled(true);
		        	swChecked = true;
		        	bSet.setEnabled(true);
				}
			}
		});
        
        //disable control in dialog by default
        if (false == swChecked)
        {
        	sw.setChecked(false);
        	np2.setEnabled(false);
        	np.setEnabled(false);
        	bSet.setEnabled(false);
        }
        else
        {
        	np2.setEnabled(true);
        	np.setEnabled(true);
        	bSet.setEnabled(true);
        	sw.setChecked(true);
        }
        
        //set button in dialog pressed
        bSet.setOnClickListener(new OnClickListener()
        {
         @Override
         public void onClick(View v) {
             d.dismiss();
             if (swChecked == true)
             {
            	 measurementVal = np2.getValue();
            	 interval  		= np.getValue();
            	 
            	 SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
            	 Editor editor = prefs.edit();
         	     editor.putString("timelapseMeasurementVal", String.valueOf(measurementVal));
         	     editor.putString("timelapseInterval", String.valueOf(interval));
         	     editor.commit();
         	    
            	 timeLapseButton.setImageResource(R.drawable.plugin_capture_video_timelapse_active);
             }
             else
             {
            	 timeLapseButton.setImageResource(R.drawable.plugin_capture_video_timelapse_inactive);
             }
             
          }    
         });
      d.show();
	}

	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
		if (paramArrayOfByte == null)
			return;
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

//    	if (!isRecording)
    	{
			try
			{
				paramCamera.startPreview();
			}
			catch (RuntimeException e)
			{
				Log.i("View capture still image", "StartPreview fail");
			}
		}
		
		Message message = new Message();
		message.obj = String.valueOf(SessionID);
		message.what = PluginManager.MSG_CAPTURE_FINISHED;
		MainScreen.H.sendMessage(message);

		takingAlready = false;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera){}
	
}
