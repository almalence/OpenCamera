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
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 +++ --> */
// <!-- -+-
package com.almalence.opencam;

import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import java.util.Date;

public abstract class PluginCapture extends Plugin
{
	protected boolean	inCapture;
	protected boolean	aboutToTakePicture	= false;
	protected int		imagesTaken			= 0;
	protected int		imagesTakenRAW			= 0;

	public boolean getInCapture()
	{
		return inCapture;
	}

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
	public void addToSharedMemExifTags(byte[] frameData)
	{
		// frameData is jpeg array or null.
		if (imagesTaken == 0)
		{
			if (frameData != null)
				PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(frameData, SessionID, -1);
			else
				PluginManager.getInstance().addToSharedMemExifTagsFromCamera(SessionID);
		}
	}

	@Override
	public void onResume()
	{
		inCapture = false;
		aboutToTakePicture = false;
	}

	@Override
	public void onShutterClick()
	{
		if (!inCapture)
		{
			inCapture = true;

			MainScreen.getGUIManager().lockControls = true;
			Date curDate = new Date();
			SessionID = curDate.getTime();

			MainScreen.getInstance().muteShutter(true);

			if (CameraController.isAutoFocusPerform())
				aboutToTakePicture = true;
			else
				takePicture();
		}
	}

	@Override
	public void onExportFinished()
	{
	}

	public void takePicture()
	{
	}

	@Override
	public void onAutoFocus(boolean paramBoolean)
	{
		if (inCapture)
		{
			if (aboutToTakePicture)
				takePicture();

			aboutToTakePicture = false;
		}
	}

	@Override
	public abstract void onImageTaken(int frame, byte[] frameData, int frame_len, int format);

	@Override
	public abstract void onPreviewFrame(byte[] data);

	public boolean shouldPreviewToGPU()
	{
		return false;
	}

	public void onFrameAvailable()
	{

	}
}
