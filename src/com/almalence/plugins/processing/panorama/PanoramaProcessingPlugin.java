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

package com.almalence.plugins.processing.panorama;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Calendar;

import android.annotation.SuppressLint;
import android.content.SharedPreferences;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.media.ExifInterface;
import android.preference.PreferenceManager;
import android.util.Log;

import com.almalence.SwapHeap;
import com.almalence.YuvImage;

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
//-+- -->

import com.almalence.util.ImageConversion;

public class PanoramaProcessingPlugin extends PluginProcessing
{
	private static final String TAG = "PanoramaProcessingPlugin";

	private static final String PREFERENCES_KEY_SAVEINPUT = "pref_plugin_processing_panorama_saveinput";
	
	private boolean prefSaveInput;
	private boolean prefLandscape;
	private int mOrientation;
	private int out_ptr = 0;
	
	public PanoramaProcessingPlugin()
	{
		super("com.almalence.plugins.panoramaprocessing",
				R.xml.preferences_processing_panorama, 
				0, 
				0,
				null);
	}

	@SuppressLint("DefaultLocale")
	@Override
	public void onStartProcessing(final long sessionID)
    {
		Log.e(TAG, "onStartProcessing");
		
		this.prefSaveInput = PreferenceManager.getDefaultSharedPreferences(
				MainScreen.thiz).getBoolean(PREFERENCES_KEY_SAVEINPUT, false);
		
		PluginManager.getInstance().addToSharedMem("modeSaveName"+Long.toString(sessionID), PluginManager.getInstance().getActiveMode().modeSaveName);
		
		//this.prefLandscape = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("frameorientation" + Long.toString(sessionID)));
		int orient = Integer.valueOf(PluginManager.getInstance().getFromSharedMem("frameorientation" + Long.toString(sessionID)));		
		this.prefLandscape = orient == 0 || orient == 180? true : false;
		mOrientation = this.prefLandscape? (orient == 180? 90 : 270) : (orient == 270? 180 : 0);
		//mOrientation = 0;
		
		try
		{
			final int input_width = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("pano_width"+String.valueOf(sessionID)));
			final int input_height = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("pano_height"+String.valueOf(sessionID)));
			final int frames_count = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("pano_frames_count"+String.valueOf(sessionID)));
			final int camera_fov = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("pano_camera_fov"+String.valueOf(sessionID)));
			final boolean use_all = PluginManager.getInstance().getFromSharedMem("pano_useall"+String.valueOf(sessionID)).equals("1");
			final boolean free_input = use_all ? true : PluginManager.getInstance().getFromSharedMem("pano_freeinput"+String.valueOf(sessionID)).equals("1");
			final boolean mirror = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("pano_mirror" + String.valueOf(sessionID)));
			
			final int[] frames_ptrs = new int[frames_count];
			final float[][][] frame_trs = new float[frames_count][3][3];
			
			for (int i = 0; i < frames_count; i++)
			{
				frames_ptrs[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("pano_frame"+(i+1)+"."+String.valueOf(sessionID)));
				
				for (int y = 0; y < 3; y++)
				{
					for (int x = 0; x < 3; x++)
					{
						frame_trs[i][y][x] = Float.parseFloat(PluginManager.getInstance().getFromSharedMem("pano_frametrs"+(i+1)+"." + y + x + "."+String.valueOf(sessionID)));
					}
				}
			}
			
			// If images are going to be freed during processing we need to save original frames now.
			if (this.prefSaveInput && free_input)
			{
				this.saveFrames(frames_ptrs, 0, frames_ptrs.length, input_width, input_height);
			}		
			
			AlmashotPanorama.initialize();
			final int[] result = AlmashotPanorama.process(input_width, input_height, frames_ptrs, frame_trs, camera_fov, use_all, free_input);
			this.out_ptr = result[0];
			final int output_width = result[1];
			final int output_height = result[2];
			final int crop_x = result[3];
			final int crop_y = result[4];
			final int crop_w = result[5];
			final int crop_h = result[6];
			
			if (mirror)
			{
				ImageConversion.TransformNV21N(this.out_ptr, this.out_ptr, output_width, output_height, 1, 1, 0);
			}
			
			if (!free_input)
			{
				if (this.prefSaveInput)
				{
					final int inputImagesCount = result[7];

					this.saveFrames(result, 8, inputImagesCount, input_width, input_height);
				}
				
				this.freeFrames(frames_ptrs, 0, frames_ptrs.length);
			}
			
			
			MainScreen.setSaveImageWidth(output_width);
			MainScreen.setSaveImageHeight(output_height);
			if (this.prefLandscape)
			{
				PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(output_height));
	        	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(output_width));
			}
			else
			{
				PluginManager.getInstance().addToSharedMem("saveImageWidth"+String.valueOf(sessionID), String.valueOf(output_width));
	        	PluginManager.getInstance().addToSharedMem("saveImageHeight"+String.valueOf(sessionID), String.valueOf(output_height));
			}
			PluginManager.getInstance().addToSharedMem("resultfromshared"+Long.toString(sessionID), "false");
			//PluginManager.getInstance().addToSharedMem("writeorientationtag"+Long.toString(sessionID), this.prefLandscape ? "true" : "false");
	    	PluginManager.getInstance().addToSharedMem("resultframeorientation1" + String.valueOf(sessionID), String.valueOf(mOrientation));			
	    	//PluginManager.getInstance().addToSharedMem("resultframemirrored1" + String.valueOf(sessionID), Boolean.toString(this.prefLandscape));
			PluginManager.getInstance().addToSharedMem("amountofresultframes"+Long.toString(sessionID), "1");
	    	PluginManager.getInstance().addToSharedMem("resultframe1"+Long.toString(sessionID), String.valueOf(this.out_ptr));
	    	PluginManager.getInstance().addToSharedMem("resultcrop0"+Long.toString(sessionID), String.valueOf(crop_x));
	     	PluginManager.getInstance().addToSharedMem("resultcrop1"+Long.toString(sessionID), String.valueOf(crop_y));
	     	PluginManager.getInstance().addToSharedMem("resultcrop2"+Long.toString(sessionID), String.valueOf(crop_w));
	     	PluginManager.getInstance().addToSharedMem("resultcrop3"+Long.toString(sessionID), String.valueOf(crop_h));
	     	AlmashotPanorama.release();
		}
		catch (final NumberFormatException e)
		{
			Log.e(TAG, "Could not parse shared memory data.");
			throw e;
		}
    }

	@SuppressLint("DefaultLocale")
	private void saveFrames(final int[] images, final int offset, final int count, final int input_width, final int input_height)
	{
		File saveDir = PluginManager.getInstance().GetSaveDir(false);
		
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.mainContext);
        final int saveOption = Integer.parseInt(prefs.getString("exportName", "3"));
        final Calendar d = Calendar.getInstance();
        String fileFormat = String.format("%04d%02d%02d_%02d%02d%02d",
        		d.get(Calendar.YEAR),
        		d.get(Calendar.MONTH)+1,
        		d.get(Calendar.DAY_OF_MONTH),
        		d.get(Calendar.HOUR_OF_DAY),
        		d.get(Calendar.MINUTE),
        		d.get(Calendar.SECOND));
        
        final String modeName = PluginManager.getInstance().getActiveMode().modeSaveName;
        
    	switch (saveOption)
    	{
    	case 1://YEARMMDD_HHMMSS
    		break;
    		
    	case 2://YEARMMDD_HHMMSS_MODE
    		fileFormat += "_" + modeName;
    		break;
    		
    	case 3://IMG_YEARMMDD_HHMMSS
    		fileFormat = "IMG_" + fileFormat;
    		break;
    		
    	case 4://IMG_YEARMMDD_HHMMSS_MODE
    		fileFormat = "IMG_" + fileFormat + "_" + modeName;
    		break;
    	}

        final Rect crop = new Rect(0, 0, input_width, input_height);
        for (int i = 0; i < count; ++i)
        {			
        	final int optr = images[offset + i];
        	String index = String.format("_%02d", i);
            File file = new File(saveDir, fileFormat+index+".jpg");
            
            FileOutputStream os = null;
            try
	    	{
	            try
		    	{
	            	os = new FileOutputStream(file);
		    	}
		    	catch (Exception e)
		        {
		    		//save always if not working saving to sdcard
		        	e.printStackTrace();
		        	saveDir = PluginManager.getInstance().GetSaveDir(true);
		        	file = new File(saveDir, fileFormat+index+".jpg");
		        	os = new FileOutputStream(file);
		        }
	    	}
            catch (Exception e)
	        {
            	
	        }
            
            final YuvImage out = new com.almalence.YuvImage(
            		optr, ImageFormat.NV21, input_width, input_height, null);
            
            out.compressToJpeg(crop, 100, os);
        
            try
			{
            	if (os != null)
            	{
            		os.close();
            	}
			} 
            catch (final IOException e)
			{
            	e.printStackTrace();
			}
            
            try
            {
            	final ExifInterface ei = new ExifInterface(file.getAbsolutePath());
            	ei.saveAttributes();
            }
            catch (final IOException e)
            {
            	e.printStackTrace();
            }
        }
	}
	
	private void freeFrames(final int[] images, final int offset, final int count)
	{
		for (int i = 0; i < count; ++i)
        {			
        	SwapHeap.FreeFromHeap(images[offset + i]);
        }
	}
	
	@Override
	public boolean isPostProcessingNeeded()
	{
		return false;
	}

	@Override
	public void onStartPostProcessing()
	{
		
	}
}