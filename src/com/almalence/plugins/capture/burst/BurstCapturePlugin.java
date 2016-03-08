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
import java.util.LinkedHashMap;
import java.util.Map.Entry;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.hardware.camera2.CaptureResult;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.almalence.opencamunderground.ApplicationInterface;
import com.almalence.opencamunderground.ApplicationScreen;
import com.almalence.opencamunderground.CameraParameters;
import com.almalence.opencamunderground.PluginCapture;
import com.almalence.opencamunderground.PluginManager;
import com.almalence.opencamunderground.R;
import com.almalence.opencamunderground.cameracontroller.CameraController;
import com.almalence.opencamunderground.ui.GUI.CameraParameter;
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
//-+- -->
import com.almalence.util.HeapUtil;


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
	
	private static Toast	capturingDialog;
	
	//That map helps to find suitable amount of RAW frames to be captured in case of low memory
	protected static final LinkedHashMap<Integer, Integer> IMAGE_AMOUNT_VALUES		= new LinkedHashMap<Integer, Integer>()
	{
		{
			put(0, 20);
			put(1, 15);
			put(2, 10);
			put(3, 5);
			put(4, 3);
		}
	};

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
		//Warn user if current free memory level is not enough to capture all RAW frames
		//in case of RAW capturing enabled, for other capture formats amount of memory is enough by default
		if(!checkFreeMemory(imageAmount))
		{
			LinearLayout bottom_layout = (LinearLayout) ApplicationScreen.instance.findViewById(R.id.mainButtons);

			capturingDialog = Toast.makeText(ApplicationScreen.instance, R.string.not_enough_memory_for_capture, Toast.LENGTH_LONG);
			capturingDialog.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, bottom_layout.getHeight());
			capturingDialog.show();
		}
		
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
			imageAmount = 3;
			break;
		case 1:
			quickControlIconID = R.drawable.gui_almalence_mode_burst5;
			editor.putString("burstImagesAmount", "5");
			imageAmount = 5;
			break;
		case 2:
			quickControlIconID = R.drawable.gui_almalence_mode_burst10;
			editor.putString("burstImagesAmount", "10");
			imageAmount = 10;
			break;
		case 3:
			quickControlIconID = R.drawable.gui_almalence_mode_burst15;
			editor.putString("burstImagesAmount", "15");
			imageAmount = 15;
			break;
		case 4:
			quickControlIconID = R.drawable.gui_almalence_mode_burst20;
			editor.putString("burstImagesAmount", "20");
			imageAmount = 20;
			break;
		default:
			break;
		}
		editor.commit();
		
		//Warn user if current free memory level is not enough to capture all RAW frames
		//in case of RAW capturing enabled, for other capture formats amount of memory is enough by default
		if(!checkFreeMemory(imageAmount))
		{
			LinearLayout bottom_layout = (LinearLayout) ApplicationScreen.instance.findViewById(R.id.mainButtons);

			capturingDialog = Toast.makeText(ApplicationScreen.instance, R.string.not_enough_memory_for_capture, Toast.LENGTH_LONG);
			capturingDialog.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, bottom_layout.getHeight());
			capturingDialog.show();
		}
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

		if (captureRAW)
		{
			//Some device (such as LG G Flex 2 has a bad memory management.)
			//As a result on such devices is impossible to capture all set of amount of RAW frames.
			//To prevent crash we used 'reduced amount of frames' logic:
			//Checking one by one RAW frames amount starting from initial imageAmount to suit current free memory
			//If current RAW amount can't be captured we try to capture less images.
			//If we can't capture even 3 RAW picture we capture only JPEG frames but initial amount.
			
			//Find index of current imageAmount in helper's map
			int idx = IMAGE_AMOUNT_VALUES.size();
			for (Entry<Integer, Integer> entry : IMAGE_AMOUNT_VALUES.entrySet())
			{
		        if (imageAmount == entry.getValue())
		        {
		            idx =  entry.getKey();
		            break;
		        }
			}
			
			//Iterate trough image amount map. Values is reduced on each step
			for(int i = idx; i < IMAGE_AMOUNT_VALUES.size(); i++)
			{
				int imageAmountChecked = IMAGE_AMOUNT_VALUES.get(i);
				if(checkFreeMemory(imageAmountChecked))
				{
					//Checked image amount is suitable for current memory level
					
					//If checked image amount is less than initial then warn user about that and change initial image amount to suitable amount
					if(imageAmount > imageAmountChecked)
					{
						LinearLayout bottom_layout = (LinearLayout) ApplicationScreen.instance.findViewById(R.id.mainButtons);

						capturingDialog = Toast.makeText(ApplicationScreen.instance, R.string.capture_less_raw, Toast.LENGTH_LONG);
						capturingDialog.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, bottom_layout.getHeight());
						capturingDialog.show();
						
						imageAmount = imageAmountChecked;
					}
					
					final int[] pause = new int[imageAmount];
					Arrays.fill(pause, pauseBetweenShots);
					createRequestIDList(imageAmount * 2);
					CameraController.captureImagesWithParams(imageAmount, CameraController.RAW, pause, null, null, null, false, true,
							true);
					return;
				}
			}
			//If no one RAW frame can be captured, capture JPEG frames.
			LinearLayout bottom_layout = (LinearLayout) ApplicationScreen.instance.findViewById(R.id.mainButtons);

			capturingDialog = Toast.makeText(ApplicationScreen.instance, R.string.capture_only_jpeg, Toast.LENGTH_LONG);
			capturingDialog.setGravity(Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL, 0, bottom_layout.getHeight());
			capturingDialog.show();
		}
		
		final int[] pause = new int[imageAmount];
		Arrays.fill(pause, pauseBetweenShots);
		createRequestIDList(imageAmount);
		CameraController.captureImagesWithParams(imageAmount, CameraController.JPEG, pause, null, null, null, false, true,
				true);
	}

	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format)
	{
		if (frame == 0)
		{
			Log.d("Burst", "Load to heap failed");

			imagesTaken = 0;
			imagesTakenRAW = 0;
			resultCompleted = 0;
			isAllImagesTaken = false;
			inCapture = false;
			ApplicationScreen.instance.muteShutter(false);
			
			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED_NORESULT, String.valueOf(SessionID));
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
	
	//On some devices which supports RAW capturing may be not enough free memory to capture several RAW
	//So we need to check available size of memory and compare it to approximate size of all RAWs to be captured 
	private boolean checkFreeMemory(int imgAmount)
	{
		if(captureRAW)
		{
			CameraController.Size imageSize = CameraController.getCameraImageSize();
			int imageWidth = imageSize.getWidth();
			int imageHeight = imageSize.getHeight();
			
			final int freeMemoryAprox = (int) (HeapUtil.getAmountOfMemoryToFitFrames() - (HeapUtil.getRAWFrameSizeInBytes(imageWidth, imageHeight)*imgAmount));
			if(freeMemoryAprox < 20000000.f) //Left 20 Mb for safety reason - to prevent unexpected system's behavior
				return false;
		}
		
		return true;
	}
}
