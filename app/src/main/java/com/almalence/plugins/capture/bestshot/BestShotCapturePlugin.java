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

package com.almalence.plugins.capture.bestshot;

import java.util.Arrays;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.hardware.camera2.CaptureResult;

/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.ui.GUI.CameraParameter;
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.ApplicationInterface;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 +++ --> */
//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.GUI.CameraParameter;
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;

//-+- -->

/***
 * Implements burst capture plugin - captures predefined number of images
 ***/

public class BestShotCapturePlugin extends PluginCapture
{
	// defaul val. value should come from config
	private int	imageAmount	= 5;
	private int	preferenceFlashMode;

	public BestShotCapturePlugin()
	{
		super("com.almalence.plugins.bestshotcapture", 0, 0, 0, null);
	}

	@Override
	public void onCreate() {}

	@Override
	public void onResume()
	{
		imagesTaken = 0;
		inCapture = false;
		aboutToTakePicture = false;
		
		isAllImagesTaken = false;
		isAllCaptureResultsCompleted = true;

		if (CameraController.isUseCamera2() && CameraController.isNexus5or6)
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			preferenceFlashMode = prefs.getInt(ApplicationScreen.sFlashModePref, ApplicationScreen.sDefaultFlashValue);
			
			SharedPreferences.Editor editor = prefs.edit();
			editor.putInt(ApplicationScreen.sFlashModePref, CameraParameters.FLASH_MODE_OFF);
			editor.commit();
		}

		ApplicationScreen.setCaptureFormat(CameraController.YUV);
	}

	@Override
	public void setupCameraParameters()
	{
		try
		{
			int[] flashModes = CameraController.getSupportedFlashModes();
			if (flashModes != null && flashModes.length > 0 && CameraController.isUseCamera2()
					&& CameraController.isNexus5or6)
			{
				CameraController.setCameraFlashMode(CameraParameters.FLASH_MODE_OFF);

				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt(ApplicationScreen.sFlashModePref, CameraParameters.FLASH_MODE_OFF);
				editor.commit();
			}
		} catch (RuntimeException e)
		{
			Log.e("CameraTest", "ApplicationScreen.setupCamera unable to setFlashMode");
		}
	}

	@Override
	public void onGUICreate()
	{
		ApplicationScreen.getGUIManager().showHelp(ApplicationScreen.instance.getString(R.string.Bestshot_Help_Header),
				ApplicationScreen.getAppResources().getString(R.string.Bestshot_Help), R.drawable.plugin_help_bestshot,
				"bestShotShowHelp");

		if (CameraController.isUseCamera2() && CameraController.isNexus5or6)
		{
			ApplicationScreen.instance.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FLASH, true, false, true);
		}
	}

	public boolean delayedCaptureSupported()
	{
		return true;
	}

	@Override
	public void onPause()
	{
		if (CameraController.isUseCamera2() && CameraController.isNexus5or6) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			prefs.edit().putInt(ApplicationScreen.sFlashModePref, preferenceFlashMode).commit();
			CameraController.setCameraFlashMode(preferenceFlashMode);
		}
	}

	public void takePicture()
	{
		imagesTaken = 0;
		resultCompleted = 0;
		createRequestIDList(imageAmount);
		CameraController.captureImagesWithParams(imageAmount, CameraController.YUV, null, null, null, null, false, true, true);
	}

	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format)
	{
		imagesTaken++;

		if (frame == 0)
		{
			Log.d("Bestshot", "Load to heap failed");
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

			imagesTaken = 0;
			ApplicationScreen.instance.muteShutter(false);
			return;
		}
		String frameName = "frame" + imagesTaken;
		String frameLengthName = "framelen" + imagesTaken;

		PluginManager.getInstance().addToSharedMem(frameName + SessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem(frameLengthName + SessionID, String.valueOf(frame_len));
		PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken + SessionID,
				String.valueOf(ApplicationScreen.getGUIManager().getImageDataOrientation()));
		PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + SessionID,
				String.valueOf(CameraController.isFrontCamera()));

		if (imagesTaken >= imageAmount)
		{
			if(isAllCaptureResultsCompleted)
			{
				PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
						String.valueOf(imagesTaken));
	
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
	
				imagesTaken = 0;
				resultCompleted = 0;
				inCapture = false;
				
				isAllImagesTaken = false;
			}
			else
				isAllImagesTaken = true;
		}
	}

	@TargetApi(21)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		isAllCaptureResultsCompleted = false;
		
		resultCompleted++;
		
		if (resultCompleted == 1)
			PluginManager.getInstance().addToSharedMemExifTagsFromCaptureResult(result, SessionID, -1);
		
		if (resultCompleted == imageAmount)
		{
			isAllCaptureResultsCompleted = true;
			
			if(isAllImagesTaken)
			{
				PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
						String.valueOf(imagesTaken));
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
				
				inCapture = false;
				resultCompleted = 0;
				imagesTaken = 0;
				isAllImagesTaken = false;
			}
		}
	}

	@Override
	public void onPreviewFrame(byte[] data)
	{
	}
}
