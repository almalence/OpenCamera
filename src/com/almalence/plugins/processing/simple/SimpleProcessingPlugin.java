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

package com.almalence.plugins.processing.simple;

/* <!-- +++
import com.almalence.opencam_plus.ApplicationScreen;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.PluginProcessing;
import com.almalence.opencam_plus.R;
import com.almalence.opencam_plus.cameracontroller.CameraController;
+++ --> */
//<!-- -+-
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.R;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

/***
 * Implements simple processing plugin - just translate shared memory values
 * from captured to result.
 ***/

public class SimpleProcessingPlugin extends PluginProcessing
{
	private long				sessionID					= 0;

	public SimpleProcessingPlugin()
	{
		super("com.almalence.plugins.simpleprocessing", R.xml.preferences_capture_dro, 0, 0, null);
	}

	@Override
	public void onResume()
	{
	}
	
	@Override
	public void onStartProcessing(long SessionID)
	{
		sessionID = SessionID;

		ApplicationScreen.getPluginManager().addToSharedMem("modeSaveName" + sessionID,
				ApplicationScreen.getPluginManager().getActiveMode().modeSaveName);

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		int mImageWidth = imageSize.getWidth();
		int mImageHeight = imageSize.getHeight();

		String num = ApplicationScreen.getPluginManager().getFromSharedMem("amountofcapturedframes" + sessionID);

		if (num == null)
			return;
		int imagesAmount = Integer.parseInt(num);

		String numRAW = ApplicationScreen.getPluginManager().getFromSharedMem("amountofcapturedrawframes" + sessionID);

		int imagesAmountRAW = 0;
		if (numRAW != null)
			imagesAmountRAW = Integer.parseInt(numRAW);

		int frameNumRAW = 0;
		int frameNum = 0;

		for (int i = 1; i <= imagesAmount; i++)
		{
			int orientation = Integer.parseInt(ApplicationScreen.getPluginManager().getFromSharedMem(
					"frameorientation" + i + sessionID));

			int frame = Integer.parseInt(ApplicationScreen.getPluginManager().getFromSharedMem("frame" + i + sessionID));
			int len = Integer.parseInt(ApplicationScreen.getPluginManager().getFromSharedMem("framelen" + i + sessionID));

			boolean isRAW = Boolean.parseBoolean(ApplicationScreen.getPluginManager().getFromSharedMem(
					"frameisraw" + i + sessionID));

			int frameIndex = isRAW ? (++frameNumRAW) : (imagesAmountRAW + (++frameNum));

			ApplicationScreen.getPluginManager().addToSharedMem("resultframeformat" + frameIndex + sessionID,
					isRAW ? "dng" : "jpeg");
			ApplicationScreen.getPluginManager().addToSharedMem("resultframe" + frameIndex + sessionID,
					String.valueOf(frame));
			ApplicationScreen.getPluginManager().addToSharedMem("resultframelen" + frameIndex + sessionID,
					String.valueOf(len));

			ApplicationScreen.getPluginManager().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(mImageWidth));
			ApplicationScreen.getPluginManager().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(mImageHeight));

			boolean cameraMirrored = Boolean.parseBoolean(ApplicationScreen.getPluginManager().getFromSharedMem(
					"framemirrored" + i + sessionID));
			ApplicationScreen.getPluginManager().addToSharedMem("resultframeorientation" + i + sessionID,
					String.valueOf(orientation));
			ApplicationScreen.getPluginManager().addToSharedMem("resultframemirrored" + i + sessionID,
					String.valueOf(cameraMirrored));
		}

		ApplicationScreen.getPluginManager().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(imagesAmount));
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
