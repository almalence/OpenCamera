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
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginProcessing;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
import com.almalence.opencam.cameracontroller.CameraController;

//-+- -->

/***
 * Implements simple processing plugin - just translate shared memory values
 * from captured to result.
 ***/

public class SimpleProcessingPlugin extends PluginProcessing
{
	private long			sessionID				= 0;

	private static boolean	DROLocalTMPreference	= true;
	private static int		prefPullYUV				= 7; // 9;

	public SimpleProcessingPlugin()
	{
		super("com.almalence.plugins.simpleprocessing", 0, 0, 0, null);
	}

	@Override
	public void onStartProcessing(long SessionID)
	{
		sessionID = SessionID;

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				PluginManager.getInstance().getActiveMode().modeSaveName);

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		int mImageWidth = imageSize.getWidth();
		int mImageHeight = imageSize.getHeight();

		String num = PluginManager.getInstance().getFromSharedMem("amountofcapturedframes" + sessionID);
		if (num == null)
			return;
		int imagesAmount = Integer.parseInt(num);

		if (imagesAmount == 0)
			imagesAmount = 1;

		for (int i = 1; i <= imagesAmount; i++)
		{
			int orientation = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"frameorientation" + i + sessionID));
			String isDRO = PluginManager.getInstance().getFromSharedMem("isdroprocessing" + sessionID);

			if (isDRO != null && isDRO.equals("0"))
			{
				AlmaShotDRO.Initialize();

				int inputYUV = 0;
				inputYUV = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + i + sessionID));

				int yuv = AlmaShotDRO.DroProcess(inputYUV, mImageWidth, mImageHeight, 1.5f, DROLocalTMPreference, 0,
						prefPullYUV, 0.35f, 0.6f);

				AlmaShotDRO.Release();

				if (orientation == 90 || orientation == 270)
				{
					PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID,
							String.valueOf(mImageHeight));
					PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID,
							String.valueOf(mImageWidth));
				} else
				{
					PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID,
							String.valueOf(mImageWidth));
					PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID,
							String.valueOf(mImageHeight));
				}

				PluginManager.getInstance().addToSharedMem("resultframe" + i + sessionID, String.valueOf(yuv));
			} else
			{
				int frame = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("frame" + i + sessionID));
				int len = Integer.parseInt(PluginManager.getInstance().getFromSharedMem("framelen" + i + sessionID));

				PluginManager.getInstance().addToSharedMem("resultframeformat" + i + sessionID, "jpeg");
				PluginManager.getInstance().addToSharedMem("resultframe" + i + sessionID, String.valueOf(frame));
				PluginManager.getInstance().addToSharedMem("resultframelen" + i + sessionID, String.valueOf(len));

				PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID,
						String.valueOf(mImageWidth));
				PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID,
						String.valueOf(mImageHeight));
			}

			boolean cameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
					"framemirrored" + i + sessionID));
			PluginManager.getInstance().addToSharedMem("resultframeorientation" + i + sessionID,
					String.valueOf(orientation));
			PluginManager.getInstance().addToSharedMem("resultframemirrored" + i + sessionID,
					String.valueOf(cameraMirrored));
		}

		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(imagesAmount));
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
