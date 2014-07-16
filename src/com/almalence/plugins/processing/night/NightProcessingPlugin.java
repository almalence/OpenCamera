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

package com.almalence.plugins.processing.night;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.almalence.SwapHeap;
import com.almalence.asynctaskmanager.OnTaskCompleteListener;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.PluginProcessing;
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import com.almalence.util.ImageConversion;

/***
Implements night processing
***/

public class NightProcessingPlugin extends PluginProcessing implements OnTaskCompleteListener
{
	// fused result
	private int yuv;
	private static int[] crop = new int[4];
	
	private static int HI_SPEED_FRAMES = 12;

    private long sessionID=0;
    
    //night preferences
    private String NoisePreference;
    private String GhostPreference;
    private Boolean SaturatedColors;
    
    private int mDisplayOrientation = 0;
	private boolean mCameraMirrored = false;
	
	private int mImageWidth;
	private int mImageHeight;
    
	public NightProcessingPlugin()
	{
		super("com.almalence.plugins.nightprocessing",
			  R.xml.preferences_processing_night,
			  R.xml.preferences_processing_night,
			  0,
			  null);
	}
	
	@Override
	public void onStart()
	{
		getPrefs();
	}

	@Override
	public void onStartProcessing(long SessionID) 
	{
		sessionID=SessionID;
		
		PluginManager.getInstance().addToSharedMem("modeSaveName"+Long.toString(sessionID), PluginManager.getInstance().getActiveMode().modeSaveName);
		
		mDisplayOrientation = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frameorientation1" + Long.toString(sessionID)));
		mCameraMirrored = CameraController.isFrontCamera();
		
		mImageWidth = MainScreen.getImageWidth();
		mImageHeight = MainScreen.getImageHeight();
		
		int iSaveImageWidth = MainScreen.getSaveImageWidth();
		int iSaveImageHeight = MainScreen.getSaveImageHeight();
		
		AlmaShotNight.Initialize();

		//start night processing
		nightPreview();
		
		nightProcessing();
		
		if(mDisplayOrientation == 180 || mDisplayOrientation == 270)
		{
			int yuv_lenght = mImageWidth*mImageHeight+2*((mImageWidth+1)/2)*((mImageHeight+1)/2);
			int mode = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("nightmode"+Long.toString(sessionID)));	    	
	    	if (mode == 1)
	    		yuv_lenght = mImageWidth*2*mImageHeight*2+2*((mImageWidth*2+1)/2)*((mImageHeight*2+1)/2);
			
			byte[] yuv_data = SwapHeap.SwapFromHeap(yuv, yuv_lenght);
			
			byte[] dataRotated = new byte[yuv_data.length];
			if (mode == 1)
				ImageConversion.TransformNV21(yuv_data, dataRotated, mImageWidth*2, mImageHeight*2, 1, 1, 0);
			else
				ImageConversion.TransformNV21(yuv_data, dataRotated, mImageWidth, mImageHeight, 1, 1, 0);
			
			yuv = SwapHeap.SwapToHeap(dataRotated);
		}
		
		PluginManager.getInstance().addToSharedMem("resultfromshared"+Long.toString(sessionID), "false");
		PluginManager.getInstance().addToSharedMem("resultcrop0"+Long.toString(sessionID), String.valueOf(NightProcessingPlugin.crop[0]));
		PluginManager.getInstance().addToSharedMem("resultcrop1"+Long.toString(sessionID), String.valueOf(NightProcessingPlugin.crop[1]));
		PluginManager.getInstance().addToSharedMem("resultcrop2"+Long.toString(sessionID), String.valueOf(NightProcessingPlugin.crop[2]));
		PluginManager.getInstance().addToSharedMem("resultcrop3"+Long.toString(sessionID), String.valueOf(NightProcessingPlugin.crop[3]));
	
		PluginManager.getInstance().addToSharedMem("writeorientationtag"+Long.toString(sessionID), "false");
    	PluginManager.getInstance().addToSharedMem("resultframeorientation1" + String.valueOf(sessionID), String.valueOf(mDisplayOrientation));
    	PluginManager.getInstance().addToSharedMem("resultframemirrored1" + String.valueOf(sessionID), String.valueOf(mCameraMirrored));
		PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), "1");
    	PluginManager.getInstance().addToSharedMem("resultframe1"+Long.toString(sessionID), String.valueOf(yuv));
    	
		PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(iSaveImageWidth));
    	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(iSaveImageHeight));
	}
			
	
	private void nightPreview()
	{
    	int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("amountofcapturedframes"+Long.toString(sessionID)));
    	
    	int[] compressed_frame = new int[imagesAmount];
        int[] compressed_frame_len = new int[imagesAmount];

		for (int i=0; i<imagesAmount; i++)
		{
			compressed_frame[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + (i+1)+Long.toString(sessionID)));
			compressed_frame_len[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen" + (i+1)+Long.toString(sessionID)));
		}
		
		int mode = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("nightmode"+Long.toString(sessionID)));
    	
    	if(mode == 1)
    	{
    		AlmaShotNight.SuperZoomPreview(compressed_frame, NightProcessingPlugin.HI_SPEED_FRAMES, mImageWidth, mImageHeight, 
    				mImageWidth*2, mImageHeight*2,
    				Integer.parseInt(NoisePreference), Integer.parseInt(GhostPreference), SaturatedColors? 1 : 0,
    				1);
    	}
    	else
    	{
            boolean isYUV = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("isyuv"+Long.toString(sessionID)));
    		if(!isYUV)
    		{
    	    	AlmaShotNight.ConvertFromJpeg(
    	    			compressed_frame,
    	    			compressed_frame_len,
    	    			imagesAmount,
    	    			mImageWidth, mImageHeight);
    	    	Log.e("Night", "PreviewTask.doInBackground AlmaShot.ConvertFromJpeg success");
    		}
    		else
    		{
    			AlmaShotNight.NightAddYUVFrames(
    	    			compressed_frame,
    	    			imagesAmount,
    	    			mImageWidth, mImageHeight);
    	    	Log.e("Night", "PreviewTask.doInBackground AlmaShot.AddYUVFrames success");
    		}
        	
    		
    		AlmaShotNight.BlurLessPreview(mImageWidth, mImageHeight,
    			Integer.parseInt(NoisePreference), Integer.parseInt(GhostPreference),
    			9, SaturatedColors? 9 : 0,
    			imagesAmount);
    	}
    	System.gc();
	}
	
	private void nightProcessing()
	{
    	int mode = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("nightmode"+Long.toString(sessionID)));
    	
    	if (mode == 1)
    	{
    		yuv = AlmaShotNight.SuperZoomProcess(mImageWidth*2, mImageHeight*2, NightProcessingPlugin.crop, mDisplayOrientation == 90 || mDisplayOrientation == 270, mCameraMirrored);
    	}
    	else
    	{
			yuv = AlmaShotNight.BlurLessProcess(mImageWidth, mImageHeight, NightProcessingPlugin.crop, mDisplayOrientation == 90 || mDisplayOrientation == 270, mCameraMirrored);
    	}
    	
    	AlmaShotNight.Release();
	}

	private void getPrefs()
    {
        // Get the xml/preferences.xml preferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getInstance().getBaseContext());
        NoisePreference = prefs.getString("noisePrefNight", "0");
        GhostPreference = prefs.getString("ghostPrefNight", "1");
        SaturatedColors = prefs.getBoolean("keepcolorsPref", true);
    }

	@Override
	public boolean isPostProcessingNeeded(){return false;}

	@Override
	public void onStartPostProcessing(){}
}
