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
import android.os.Build;
import android.preference.PreferenceManager;
import android.util.Log;

import com.almalence.SwapHeap;
import com.almalence.asynctaskmanager.OnTaskCompleteListener;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginProcessing;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

/***
 * Implements night processing
 ***/

public class NightProcessingPlugin extends PluginProcessing implements OnTaskCompleteListener
{
	// fused result
	private int				yuv;
	private static int[]	crop				= new int[4];

	private long			sessionID			= 0;

	// night preferences
	private String			NoisePreference;
	private String			GhostPreference;
	private Boolean			SaturatedColors;

	private int				mDisplayOrientation	= 0;
	private boolean			mCameraMirrored		= false;

	private int				mImageWidth;
	private int				mImageHeight;

	public NightProcessingPlugin()
	{
		super("com.almalence.plugins.nightprocessing", R.xml.preferences_processing_night,
				R.xml.preferences_processing_night, 0, null);
	}

	@Override
	public void onStart()
	{
		getPrefs();
	}

	@Override
	public void onStartProcessing(long SessionID)
	{
		sessionID = SessionID;

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				PluginManager.getInstance().getActiveMode().modeSaveName);

		mDisplayOrientation = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
				"frameorientation1" + sessionID));
		mCameraMirrored = CameraController.isFrontCamera();

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		mImageWidth = imageSize.getWidth();
		mImageHeight = imageSize.getHeight();

		AlmaShotNight.Initialize();

		// start night processing
		nightProcessing();

		PluginManager.getInstance().addToSharedMem("resultfromshared" + sessionID, "true");
		PluginManager.getInstance().addToSharedMem("resultcrop0" + sessionID,
				String.valueOf(NightProcessingPlugin.crop[0]));
		PluginManager.getInstance().addToSharedMem("resultcrop1" + sessionID,
				String.valueOf(NightProcessingPlugin.crop[1]));
		PluginManager.getInstance().addToSharedMem("resultcrop2" + sessionID,
				String.valueOf(NightProcessingPlugin.crop[2]));
		PluginManager.getInstance().addToSharedMem("resultcrop3" + sessionID,
				String.valueOf(NightProcessingPlugin.crop[3]));

		PluginManager.getInstance().addToSharedMem("writeorientationtag" + sessionID, "false");
		PluginManager.getInstance().addToSharedMem("resultframeorientation1" + sessionID,
				String.valueOf(mDisplayOrientation));
		PluginManager.getInstance().addToSharedMem("resultframemirrored1" + sessionID, String.valueOf(mCameraMirrored));
		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, "1");
		PluginManager.getInstance().addToSharedMem("resultframe1" + sessionID, String.valueOf(yuv));

		PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(mImageWidth));
		PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(mImageHeight));
	}

	private void nightProcessing()
	{
		int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
				"amountofcapturedframes" + sessionID));

		int[] frames = new int[imagesAmount];

		for (int i = 0; i < imagesAmount; i++)
		{
			frames[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"frame" + (i + 1) + sessionID));
		}

		AlmaShotNight.NightAddYUVFrames(frames, imagesAmount, mImageWidth, mImageHeight);

//		Log.d("Night", "PreviewTask.doInBackground AlmaShotNight.Process start");

		float zoom = Float.parseFloat(PluginManager.getInstance().getFromSharedMem(
				"zoom" + sessionID));
		boolean isSuperMode = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
				"isSuperMode" + sessionID));
		int sensorGain = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
				"sensorGain" + sessionID));
		
		yuv = AlmaShotNight.Process(mImageWidth, mImageHeight, mImageWidth, mImageHeight,
				sensorGain, Integer.parseInt(NoisePreference), Integer.parseInt(GhostPreference),
				9, SaturatedColors ? 9 : 0, imagesAmount,
				NightProcessingPlugin.crop,
				mDisplayOrientation,
				mCameraMirrored,
				zoom, isSuperMode);

		AlmaShotNight.Release();
	}

	private void getPrefs()
	{
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getInstance()
				.getBaseContext());
		NoisePreference = prefs.getString("noisePrefNight", "0");
		GhostPreference = prefs.getString("ghostPrefNight", "1");
		SaturatedColors = prefs.getBoolean("keepcolorsPref", true);
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
