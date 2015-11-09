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

package com.almalence.plugins.capture.burst;

import java.util.Arrays;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.camera2.CaptureResult;
import android.preference.PreferenceManager;
import android.util.Log;

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
// <!-- -+-
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

public class BurstCapturePlugin extends PluginCapture
{
	// defaul val. value should come from config
	private int				imageAmount			= 3;
	private int				pauseBetweenShots	= 0;
	private int				preferenceFlashMode;

	private static String	sImagesAmountPref;
	private static String	sPauseBetweenShotsPref;

	public BurstCapturePlugin()
	{
		super("com.almalence.plugins.burstcapture", R.xml.preferences_capture_burst, 0,
				R.drawable.gui_almalence_mode_burst, "Burst images");
	}

	@Override
	public void onCreate()
	{
		sImagesAmountPref = ApplicationScreen.getAppResources().getString(R.string.Preference_BurstImagesAmount);
		sPauseBetweenShotsPref = ApplicationScreen.getAppResources().getString(R.string.Preference_BurstPauseBetweenShots);
	}

	@Override
	public void onStart()
	{
		refreshPreferences();
	}

	@Override
	public void onResume()
	{
		imagesTaken = 0;
		imagesTakenRAW = 0;
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

		// refreshPreferences();
		if(captureRAW && CameraController.isRAWCaptureSupported())
			ApplicationScreen.setCaptureFormat(CameraController.RAW);
		else
		{
			captureRAW = false;
			ApplicationScreen.setCaptureFormat(CameraController.JPEG);
		}
	}

	@Override
	public void onGUICreate()
	{
		if (CameraController.isUseCamera2() && CameraController.isNexus5or6)
			ApplicationScreen.instance.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FLASH, true, false, true);
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
	public void onPause()
	{
		if (CameraController.isUseCamera2() && CameraController.isNexus5or6) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			prefs.edit().putInt(ApplicationScreen.sFlashModePref, preferenceFlashMode).commit();
			CameraController.setCameraFlashMode(preferenceFlashMode);
		}
	}

	private void refreshPreferences()
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			imageAmount = Integer.parseInt(prefs.getString(sImagesAmountPref, "3"));
			pauseBetweenShots = Integer.parseInt(prefs.getString(sPauseBetweenShotsPref, "0"));
			captureRAW = prefs.getBoolean(ApplicationScreen.sCaptureRAWPref, false);
		} catch (Exception e)
		{
			Log.e("Burst capture", "Cought exception " + e.getMessage());
		}

		switch (imageAmount)
		{
		case 3:
			quickControlIconID = R.drawable.gui_almalence_mode_burst3;
			break;
		case 5:
			quickControlIconID = R.drawable.gui_almalence_mode_burst5;
			break;
		case 10:
			quickControlIconID = R.drawable.gui_almalence_mode_burst10;
			break;
		case 15:
			quickControlIconID = R.drawable.gui_almalence_mode_burst15;
			break;
		case 20:
			quickControlIconID = R.drawable.gui_almalence_mode_burst20;
			break;
		default:
			break;
		}
	}

	@Override
	public void onQuickControlClick()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		int val = Integer.parseInt(prefs.getString(sImagesAmountPref, "1"));
		int selected = 0;
		switch (val)
		{
		case 3:
			selected = 0;
			break;
		case 5:
			selected = 1;
			break;
		case 10:
			selected = 2;
			break;
		case 15:
			selected = 3;
			break;
		case 20:
			selected = 4;
			break;
		default:
			break;
		}
		selected = (selected + 1) % 5;

		Editor editor = prefs.edit();
		switch (selected)
		{
		case 0:
			quickControlIconID = R.drawable.gui_almalence_mode_burst3;
			editor.putString("burstImagesAmount", "3");
			break;
		case 1:
			quickControlIconID = R.drawable.gui_almalence_mode_burst5;
			editor.putString("burstImagesAmount", "5");
			break;
		case 2:
			quickControlIconID = R.drawable.gui_almalence_mode_burst10;
			editor.putString("burstImagesAmount", "10");
			break;
		case 3:
			quickControlIconID = R.drawable.gui_almalence_mode_burst15;
			editor.putString("burstImagesAmount", "15");
			break;
		case 4:
			quickControlIconID = R.drawable.gui_almalence_mode_burst20;
			editor.putString("burstImagesAmount", "20");
			break;
		default:
			break;
		}
		editor.commit();
	}

	public boolean delayedCaptureSupported()
	{
		return true;
	}

	public void takePicture()
	{
		refreshPreferences();
		inCapture = true;
		resultCompleted = 0;

		int[] pause = new int[imageAmount];
		Arrays.fill(pause, pauseBetweenShots);
		createRequestIDList(captureRAW? imageAmount * 2 : imageAmount);
		if (captureRAW)
		{
			CameraController.captureImagesWithParams(imageAmount, CameraController.RAW, pause, null, null, null, false, true,
					true);
		} else
			CameraController.captureImagesWithParams(imageAmount, CameraController.JPEG, pause, null, null, null, false, true,
					true);
	}

	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format)
	{
		if (frame == 0)
		{
			Log.d("Burst", "Load to heap failed");
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

			imagesTaken = 0;
			imagesTakenRAW = 0;
			resultCompleted = 0;
			ApplicationScreen.instance.muteShutter(false);
			return;
		}

		boolean isRAW = (format == CameraController.RAW);
		if (isRAW)
			imagesTakenRAW++;

		imagesTaken++;
		PluginManager.getInstance().addToSharedMem("frame" + imagesTaken + SessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem("framelen" + imagesTaken + SessionID, String.valueOf(frame_len));

		PluginManager.getInstance().addToSharedMem("frameisraw" + imagesTaken + SessionID, String.valueOf(isRAW));

		PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken + SessionID,
				String.valueOf(ApplicationScreen.getGUIManager().getImageDataOrientation()));
		PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + SessionID,
				String.valueOf(CameraController.isFrontCamera()));

		try
		{
			CameraController.startCameraPreview();
		} catch (RuntimeException e)
		{
			Log.e("Burst", "StartPreview fail");
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

			imagesTaken = 0;
			imagesTakenRAW = 0;
			resultCompleted = 0;
			ApplicationScreen.instance.muteShutter(false);
			return;
		}

		if ((captureRAW && imagesTaken >= (imageAmount * 2)) || (!captureRAW && imagesTaken >= imageAmount))
		{
			if(isAllCaptureResultsCompleted)
			{
				PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
						String.valueOf(imagesTaken));
				PluginManager.getInstance().addToSharedMem("amountofcapturedrawframes" + SessionID,
						String.valueOf(imagesTakenRAW));
	
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
	
				imagesTaken = 0;
				imagesTakenRAW = 0;
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
		
		int requestID = requestIDArray[resultCompleted];
		resultCompleted++;
		if (result.getSequenceId() == requestID)
		{
			PluginManager.getInstance().addToSharedMemExifTagsFromCaptureResult(result, SessionID, resultCompleted);
		}

		if (captureRAW)
			PluginManager.getInstance().addRAWCaptureResultToSharedMem("captureResult" + resultCompleted + SessionID,
					result);
		
		if ((captureRAW && resultCompleted >= (imageAmount * 2)) || (!captureRAW && resultCompleted >= imageAmount))
		{
			isAllCaptureResultsCompleted = true;
			
			if(isAllImagesTaken)
			{
				PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
						String.valueOf(imagesTaken));
				PluginManager.getInstance().addToSharedMem("amountofcapturedrawframes" + SessionID,
						String.valueOf(imagesTakenRAW));
				
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
