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

/* <!-- +++
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-
package com.almalence.opencam;

//-+- -->

import java.util.Date;

import com.almalence.opencam.cameracontroller.CameraController;

import android.hardware.Camera;
import android.media.Image;
import android.preference.PreferenceFragment;

public abstract class PluginCapture extends Plugin
{
	protected boolean	takingAlready	= false;
	protected boolean	inCapture;

	public PluginCapture(String ID, int preferenceID, int advancedPreferenceID, int quickControlID,
			String quickControlInitTitle)
	{
		super(ID, preferenceID, advancedPreferenceID, quickControlID, quickControlInitTitle);
	}

	public boolean muteSound()
	{
		return false;
	}

	@Override
	public void onShutterClick()
	{
		if (!inCapture)
		{
			Date curDate = new Date();
			SessionID = curDate.getTime();

			MainScreen.getInstance().muteShutter(true);

			int focusMode = CameraController.getInstance().getFocusMode();
			if (focusMode != -1
					&& !takingAlready
					&& (CameraController.getFocusState() == CameraController.FOCUS_STATE_IDLE || CameraController
							.getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
					&& !(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
							|| focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
							|| focusMode == CameraParameters.AF_MODE_INFINITY
							|| focusMode == CameraParameters.AF_MODE_FIXED || focusMode == CameraParameters.AF_MODE_EDOF)
					&& !MainScreen.getAutoFocusLock())
				takingAlready = true;
			else if (!takingAlready)
			{
				takePicture();
			}
		}
	}

	public void takePicture()
	{
	}

	@Override
	public abstract void onAutoFocus(boolean paramBoolean);

	@Override
	public abstract void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera);

	@Override
	public abstract void onImageAvailable(Image im);

	@Override
	public abstract void onPreviewFrame(byte[] data, Camera paramCamera);

	public boolean shouldPreviewToGPU()
	{
		return false;
	}

	public void onFrameAvailable()
	{

	}
}
