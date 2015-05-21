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

package com.almalence.plugins.capture.multishot;

import android.annotation.TargetApi;
import android.os.CountDownTimer;
import android.os.Message;
import android.util.Log;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.hardware.camera2.CaptureResult;

/* <!-- +++
import com.almalence.opencam_plus.cameracontroller.CameraController;
import com.almalence.opencam_plus.ApplicationScreen;
import com.almalence.opencam_plus.PluginCapture;
import com.almalence.opencam_plus.PluginManager;
import com.almalence.opencam_plus.R;
+++ --> */
//<!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
//-+- -->

/***
 * Implements group shot capture plugin - captures predefined number of images
 ***/

public class MultiShotCapturePlugin extends PluginCapture
{

	private static final String					TAG					= "MultiShotCapturePlugin";

	private static final int					MIN_MPIX_SUPPORTED	= 1280 * 960;
	private static final int					MIN_MPIX_PREVIEW	= 600 * 400;

	private static int							captureIndex		= -1;

	public static int getCaptureIndex()
	{
		return captureIndex;
	}

	private static int	imgCaptureWidth		= 0;
	private static int	imgCaptureHeight	= 0;

	// defaul val. value should come from config
	private int		imageAmount			= 8;
	private int[]	pauseBetweenShots			= { 0, 0, 250, 250, 500, 750, 1000, 1250 };
	private int[]	pauseBetweenShotsCamera2	= { 100, 200, 250, 250, 500, 750, 1000, 1250 };

	public MultiShotCapturePlugin()
	{
		super("com.almalence.plugins.multishotcapture", 0, 0, 0, null);
	}

	@Override
	public void onResume()
	{
		imagesTaken = 0;
		inCapture = false;
		aboutToTakePicture = false;

		ApplicationScreen.setCaptureFormat(CameraController.YUV);
	}

	@Override
	public void onGUICreate()
	{
		ApplicationScreen.getGUIManager().showHelp(ApplicationScreen.instance.getString(R.string.MultiShot_Help_Header),
				ApplicationScreen.getAppResources().getString(R.string.MultiShot_Help),
				R.drawable.plugin_help_multishot, "multiShotShowHelp");
	}

	public boolean delayedCaptureSupported()
	{
		return true;
	}

	public void takePicture()
	{
		resultCompleted = 0;
		createRequestIDList(imageAmount);
		CameraController.captureImagesWithParams(imageAmount, CameraController.YUV,
				CameraController.isHALv3Supported()?pauseBetweenShotsCamera2:pauseBetweenShots, null, null, null, true, true);
	}

	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format)
	{
		imagesTaken++;

		//show indication
		ApplicationScreen.instance.findViewById(R.id.captureIndicationText).setVisibility(View.VISIBLE);
		((TextView)ApplicationScreen.instance.findViewById(R.id.captureIndicationText)).setText(imagesTaken+" of " + imageAmount);
		
		if (frame == 0)
		{
			Log.i(TAG, "Load to heap failed");

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED_NORESULT, String.valueOf(SessionID));

			imagesTaken = 0;
			resultCompleted = 0;
			ApplicationScreen.instance.muteShutter(false);
			inCapture = false;
			return;
		}
		String frameName = "frame" + imagesTaken;
		String frameLengthName = "framelen" + imagesTaken;

		PluginManager.getInstance().addToSharedMem(frameName + SessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem(frameLengthName + SessionID, String.valueOf(frame_len));
		PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken + SessionID,
				String.valueOf(ApplicationScreen.getGUIManager().getDisplayOrientation()));
		PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + SessionID,
				String.valueOf(CameraController.isFrontCamera()));

		if (imagesTaken >= imageAmount)
		{
			//hide capture indication
			ApplicationScreen.instance.findViewById(R.id.captureIndicationText).setVisibility(View.GONE);
			
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
					String.valueOf(imagesTaken));

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

			imagesTaken = 0;
			resultCompleted = 0;
			new CountDownTimer(5000, 5000)
			{
				public void onTick(long millisUntilFinished)
				{
				}

				public void onFinish()
				{
					inCapture = false;
				}
			}.start();
		}
	}

	@TargetApi(21)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		int requestID = requestIDArray[resultCompleted];
		resultCompleted++;
		if (result.getSequenceId() == requestID)
		{
			if (imagesTaken == 1)
				PluginManager.getInstance().addToSharedMemExifTagsFromCaptureResult(result, SessionID, resultCompleted);
		}
	}

	@Override
	public void onPreviewFrame(byte[] data)
	{
	}

	@Override
	public void selectImageDimension()
	{
		selectImageDimensionMultishot();
		setCameraImageSize();
	}

	private void setCameraImageSize()
	{
		if (imgCaptureWidth > 0 && imgCaptureHeight > 0)
		{
			CameraController.setCameraImageSize(new CameraController.Size(imgCaptureWidth, imgCaptureHeight));
//			ApplicationScreen.setSaveImageWidth(imgCaptureWidth);
//			ApplicationScreen.setSaveImageHeight(imgCaptureHeight);
//
//			ApplicationScreen.setImageWidth(imgCaptureWidth);
//			ApplicationScreen.setImageHeight(imgCaptureHeight);
		}
	}

	public static void selectImageDimensionMultishot()
	{
		captureIndex = ApplicationScreen.selectImageDimensionMultishot();
		imgCaptureWidth = CameraController.MultishotResolutionsSizeList.get(captureIndex).getWidth();
		imgCaptureHeight = CameraController.MultishotResolutionsSizeList.get(captureIndex).getHeight();
	}
}
