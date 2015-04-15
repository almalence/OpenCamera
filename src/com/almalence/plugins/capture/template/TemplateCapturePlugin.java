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

package com.almalence.plugins.capture.template;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.hardware.camera2.CaptureResult;

import com.almalence.opencam.ApplicationInterface;
/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.TemplateScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.TemplatePluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

/***
 * Implements standard capture plugin - capture single image and save it in
 * shared memory
 ***/

public class TemplateCapturePlugin extends PluginCapture
{
	public static final String	CAMERA_IMAGE_BUCKET_NAME	= Environment.getExternalStorageDirectory().toString()
																	+ "/DCIM/Camera/tmp_raw_img";

	public TemplateCapturePlugin()
	{
		super("com.almalence.plugins.capture", 0, 0, 0, null);
	}

	@Override
	public void onCreate()
	{
		
	}

	@Override
	public void onCameraParametersSetup()
	{
		
	}

	@Override
	public void onStart()
	{
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		captureRAW = (prefs.getBoolean(ApplicationScreen.sCaptureRAWPref, false) && CameraController.isRAWCaptureSupported());
	}

	@Override
	public void onResume()
	{
		inCapture = false;
		aboutToTakePicture = false;
		
		if(captureRAW)
			ApplicationScreen.setCaptureFormat(CameraController.RAW);
		else
			ApplicationScreen.setCaptureFormat(CameraController.JPEG);
	}

	@Override
	public void onPause()
	{
	}

	@Override
	public void onGUICreate()
	{
		
	}

	@Override
	public void onStop()
	{
	}

	@Override
	public void onDefaultsSelect()
	{
	}

	@Override
	public void onShowPreferences()
	{
	}

	protected int framesCaptured = 0;
	protected int resultCompleted = 0;
	@Override
	public void takePicture()
	{
		framesCaptured = 0;
		resultCompleted = 0;
		createRequestIDList(captureRAW? 2 : 1);
		if(captureRAW)
			CameraController.captureImagesWithParams(1, CameraController.RAW, null, null, null, null, true, true);
		else
			CameraController.captureImagesWithParams(1, CameraController.JPEG, null, null, null, null, true, true);
	}

	
	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format)
	{
		framesCaptured++;
		boolean isRAW = (format == CameraController.RAW);
		
		ApplicationScreen.getPluginManager().addToSharedMem("frame" + framesCaptured + SessionID, String.valueOf(frame));
		ApplicationScreen.getPluginManager().addToSharedMem("framelen" + framesCaptured + SessionID, String.valueOf(frame_len));
		
		ApplicationScreen.getPluginManager().addToSharedMem("frameisraw" + framesCaptured + SessionID, String.valueOf(isRAW));
		
		
		ApplicationScreen.getPluginManager().addToSharedMem("frameorientation" + framesCaptured + SessionID,
				String.valueOf(ApplicationScreen.getGUIManager().getDisplayOrientation()));
		ApplicationScreen.getPluginManager().addToSharedMem("framemirrored" + framesCaptured + SessionID,
				String.valueOf(CameraController.isFrontCamera()));

		ApplicationScreen.getPluginManager().addToSharedMem("amountofcapturedframes" + SessionID, String.valueOf(framesCaptured));
		ApplicationScreen.getPluginManager().addToSharedMem("amountofcapturedrawframes" + SessionID, isRAW? "1" : "0");

		if((captureRAW && framesCaptured == 2) || !captureRAW)
		{
			ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
			inCapture = false;
			framesCaptured = 0;
			resultCompleted = 0;
		}
	}

	@TargetApi(21)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		int requestID = requestIDArray[resultCompleted];
		resultCompleted++;
		if (result.getSequenceId() == requestID)
			ApplicationScreen.getPluginManager().addToSharedMemExifTagsFromCaptureResult(result, SessionID, resultCompleted);
		
		if(captureRAW)
			ApplicationScreen.getPluginManager().addRAWCaptureResultToSharedMem("captureResult" + resultCompleted + SessionID, result);
	}

	@Override
	public void onPreviewFrame(byte[] data)
	{
	}

	public boolean delayedCaptureSupported()
	{
		return true;
	}

	public boolean photoTimeLapseCaptureSupported()
	{
		return true;
	}
}
