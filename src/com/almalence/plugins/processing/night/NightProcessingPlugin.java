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
import com.almalence.asynctaskmanager.OnTaskCompleteListener;

/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.ConfigParser;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginProcessing;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.ConfigParser;
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

	// super preferences
	private float			fGamma;
	private boolean			upscaleResult;

	private int				mDisplayOrientation	= 0;
	private boolean			mCameraMirrored		= false;
	private int				cameraIndex			= 0;

	private int				mImageWidth;
	private int				mImageHeight;

	private int				mOutImageWidth;
	private int				mOutImageHeight;

	public NightProcessingPlugin()
	{
		super("com.almalence.plugins.nightprocessing", "nightmode", R.xml.preferences_processing_night,
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

		if (CameraController.isUseCamera2())
		{
			PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
					ConfigParser.getInstance().getMode(mode).modeSaveNameHAL);
		} else
		{
			PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
					ConfigParser.getInstance().getMode(mode).modeSaveName);
		}

		mDisplayOrientation = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
				"frameorientation1" + sessionID));
		mCameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
				"cameraMirrored" + sessionID));

		mImageWidth = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageWidth" + sessionID));
		mImageHeight = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("imageHeight" + sessionID));

		mOutImageWidth = mImageWidth;
		mOutImageHeight = mImageHeight;

		if (CameraController.isUseCamera2())
		{
			if (upscaleResult)
			{
				mOutImageWidth = mOutImageWidth * 3 / 2;
				mOutImageWidth -= mOutImageWidth & 3;

				mOutImageHeight = mOutImageHeight * 3 / 2;
				mOutImageHeight -= mOutImageHeight & 3;
			}
		}

		cameraIndex = 100;
		// camera indexes in libalmalib corresponding to models
		if (CameraController.isNexus5)
			cameraIndex = 100;
		if (CameraController.isNexus6)
			cameraIndex = 103;
		if (CameraController.isFlex2)
			cameraIndex = 507;
//		if (CameraController.isG4)
//			cameraIndex = 506;

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

		PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(mOutImageWidth));
		PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(mOutImageHeight));
	}

	private void nightProcessing()
	{
		int imagesAmount = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
				"amountofcapturedframes" + sessionID));

		int[] frames = new int[imagesAmount];

		for (int i = 0; i < imagesAmount; i++)
		{
			frames[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + (i + 1) + sessionID));
		}

		AlmaShotNight.NightAddYUVFrames(frames, imagesAmount, mImageWidth, mImageHeight);

		float zoom = Float.parseFloat(PluginManager.getInstance().getFromSharedMem("zoom" + sessionID));
		boolean isSuperMode = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
				"isSuperMode" + sessionID));
		int sensorGain = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("burstGain" + sessionID));

		if (CameraController.isNexus6 && CameraController.isFrontCamera())
		{
			if (mDisplayOrientation == 0 || mDisplayOrientation == 90)
				mDisplayOrientation += 180;
			else if (mDisplayOrientation == 180 || mDisplayOrientation == 270)
				mDisplayOrientation -= 180;
		}

		yuv = AlmaShotNight.Process(mImageWidth, mImageHeight, mOutImageWidth, mOutImageHeight, sensorGain,
				Integer.parseInt(NoisePreference), Integer.parseInt(GhostPreference), 9, SaturatedColors ? 9 : 0,
				fGamma, imagesAmount, NightProcessingPlugin.crop, mDisplayOrientation, mCameraMirrored, zoom,
				cameraIndex, isSuperMode);

		AlmaShotNight.Release();
	}

	private void getPrefs()
	{
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.instance
				.getBaseContext());
		NoisePreference = prefs.getString("noisePrefNight", "0");
		GhostPreference = prefs.getString("ghostPrefNight", "1");
		SaturatedColors = prefs.getBoolean("keepcolorsPref", true);

		fGamma = prefs.getFloat("gammaPref", 0.5f);
		upscaleResult = prefs.getBoolean("upscaleResult", false);
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
