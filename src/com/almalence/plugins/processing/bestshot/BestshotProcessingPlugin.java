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

package com.almalence.plugins.processing.bestshot;

/* <!-- +++
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.PluginProcessing;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.PluginProcessing;
//-+- -->
import com.almalence.plugins.processing.bestshot.AlmaShotBestShot;

/***
 * Implements simple processing plugin - just translate shared memory values
 * from captured to result.
 ***/

public class BestshotProcessingPlugin extends PluginProcessing
{
	private long	sessionID	= 0;

	public BestshotProcessingPlugin()
	{
		super("com.almalence.plugins.bestshotprocessing", 0, 0, 0, null);
	}

	@Override
	public void onStartProcessing(long SessionID)
	{
		sessionID = SessionID;

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				PluginManager.getInstance().getActiveMode().modeSaveName);

		PluginManager.getInstance().addToSharedMem("modeSaveName" + sessionID,
				PluginManager.getInstance().getActiveMode().modeSaveName);

		int iSaveImageWidth = MainScreen.getSaveImageWidth();
		int iSaveImageHeight = MainScreen.getSaveImageHeight();

		int mImageWidth = MainScreen.getImageWidth();
		int mImageHeight = MainScreen.getImageHeight();

		String num = PluginManager.getInstance().getFromSharedMem("amountofcapturedframes" + sessionID);
		if (num == null)
			return;
		int imagesAmount = Integer.parseInt(num);

		if (imagesAmount == 0)
			imagesAmount = 1;

		int orientation = Integer.parseInt(PluginManager.getInstance()
				.getFromSharedMem("frameorientation1" + sessionID));
		AlmaShotBestShot.Initialize();

		int[] compressed_frame = new int[imagesAmount];
		int[] compressed_frame_len = new int[imagesAmount];

		for (int i = 0; i < imagesAmount; i++)
		{
			compressed_frame[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"frame" + (i + 1) + sessionID));
			compressed_frame_len[i] = Integer.parseInt(PluginManager.getInstance().getFromSharedMem(
					"framelen" + (i + 1) + sessionID));
		}

		boolean isYUV = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem("isyuv" + sessionID));

		if (!isYUV)
		{
			AlmaShotBestShot.ConvertFromJpeg(compressed_frame, compressed_frame_len, imagesAmount, mImageWidth,
					mImageHeight);
		} else
		{
			AlmaShotBestShot.AddYUVFrames(compressed_frame, imagesAmount, mImageWidth, mImageHeight);
		}

		int idxResult = AlmaShotBestShot.BestShotProcess(imagesAmount, mImageWidth, mImageHeight);

		AlmaShotBestShot.Release();

		if (orientation == 90 || orientation == 270)
		{
			PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(iSaveImageHeight));
			PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(iSaveImageWidth));
		} else
		{
			PluginManager.getInstance().addToSharedMem("saveImageWidth" + sessionID, String.valueOf(iSaveImageWidth));
			PluginManager.getInstance().addToSharedMem("saveImageHeight" + sessionID, String.valueOf(iSaveImageHeight));
		}

		int frame = compressed_frame[idxResult];
		int len = compressed_frame_len[idxResult];

		if (!isYUV)
			PluginManager.getInstance().addToSharedMem("resultframeformat1" + sessionID, "jpeg");
		PluginManager.getInstance().addToSharedMem("resultframe1" + sessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem("resultframelen1" + sessionID, String.valueOf(len));

		boolean cameraMirrored = Boolean.parseBoolean(PluginManager.getInstance().getFromSharedMem(
				"framemirrored1" + sessionID));
		PluginManager.getInstance().addToSharedMem("resultframeorientation1" + sessionID, String.valueOf(orientation));
		PluginManager.getInstance().addToSharedMem("resultframemirrored1" + sessionID, String.valueOf(cameraMirrored));

		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(1));
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
