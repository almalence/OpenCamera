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

import java.nio.ByteBuffer;
import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.os.CountDownTimer;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

/* <!-- +++
 import com.almalence.opencam_plus.CameraController;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
//-+- -->

import com.almalence.SwapHeap;
import com.almalence.YuvImage;

/***
 * Implements burst capture plugin - captures predefined number of images
 ***/

public class BestShotCapturePlugin extends PluginCapture
{
	// defaul val. value should come from config
	private int				imageAmount	= 5;

	private int				imagesTaken	= 0;

	private static String	sImagesAmountPref;

	public BestShotCapturePlugin()
	{
		super("com.almalence.plugins.bestshotcapture", R.xml.preferences_capture_bestshot, 0,
				R.drawable.gui_almalence_mode_bestshot, "Best Shot images");
	}

	@Override
	public void onCreate()
	{
		sImagesAmountPref = MainScreen.getInstance().getResources().getString(R.string.Preference_BestShotImagesAmount);
	}

	@Override
	public void onResume()
	{
		takingAlready = false;
		imagesTaken = 0;
		inCapture = false;
		refreshPreferences();

		MainScreen.setCaptureYUVFrames(true);
	}

	private void refreshPreferences()
	{
		try
		{
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			imageAmount = Integer.parseInt(prefs.getString(sImagesAmountPref, "5"));
		} catch (Exception e)
		{
			Log.v("Bestshot capture", "Cought exception " + e.getMessage());
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
		default:
			break;
		}
	}

	@Override
	public void onQuickControlClick()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int val = Integer.parseInt(prefs.getString(sImagesAmountPref, "5"));
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
		default:
			break;
		}
		selected = (selected + 1) % 3;

		Editor editor = prefs.edit();
		switch (selected)
		{
		case 0:
			quickControlIconID = R.drawable.gui_almalence_mode_burst3;
			editor.putString("BestshotImagesAmount", "3");
			break;
		case 1:
			quickControlIconID = R.drawable.gui_almalence_mode_burst5;
			editor.putString("BestshotImagesAmount", "5");
			break;
		case 2:
			quickControlIconID = R.drawable.gui_almalence_mode_burst10;
			editor.putString("BestshotImagesAmount", "10");
			break;
		default:
			break;
		}
		editor.commit();
	}

	@Override
	public void onGUICreate()
	{
		MainScreen.getGUIManager().showHelp(MainScreen.getInstance().getString(R.string.Bestshot_Help_Header),
				MainScreen.getInstance().getResources().getString(R.string.Bestshot_Help),
				R.drawable.plugin_help_bestshot, "bestShotShowHelp");
	}

	public boolean delayedCaptureSupported()
	{
		return true;
	}

	public void takePicture()
	{
		if (!inCapture)
		{
			inCapture = true;
			refreshPreferences();
			takingAlready = true;
			if (imagesTaken == 0)
			{
				PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
						PluginManager.MSG_NEXT_FRAME);
			} else
			{
				new CountDownTimer(50, 50)
				{
					public void onTick(long millisUntilFinished)
					{
					}

					public void onFinish()
					{
						PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
								PluginManager.MSG_NEXT_FRAME);
					}
				}.start();
			}
		}
	}

	@Override
	public void onPictureTaken(byte[] paramArrayOfByte, Camera paramCamera)
	{
		Log.i("Bestshot", "1");
		imagesTaken++;
		int frame_len = paramArrayOfByte.length;
		int frame = SwapHeap.SwapToHeap(paramArrayOfByte);

		if (frame == 0)
		{
			Log.i("Bestshot", "Load to heap failed");
			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, 
					String.valueOf(SessionID));

			imagesTaken = 0;
			MainScreen.getInstance().muteShutter(false);
			inCapture = false;
			return;
		}
		String frameName = "frame" + imagesTaken;
		String frameLengthName = "framelen" + imagesTaken;

		PluginManager.getInstance().addToSharedMem(frameName + SessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem(frameLengthName + SessionID, String.valueOf(frame_len));
		PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken + SessionID,
				String.valueOf(MainScreen.getGUIManager().getDisplayOrientation()));
		PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + SessionID,
				String.valueOf(CameraController.isFrontCamera()));

		if (imagesTaken == 1)
			PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(paramArrayOfByte, SessionID, -1);

		try
		{
			paramCamera.startPreview();
		} catch (RuntimeException e)
		{
			Log.i("Bestshot", "StartPreview fail");
			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, 
					String.valueOf(SessionID));

			imagesTaken = 0;
			MainScreen.getInstance().muteShutter(false);
			inCapture = false;
			return;
		}
		if (imagesTaken < imageAmount)
			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
		else
		{
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
					String.valueOf(imagesTaken));

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, 
					String.valueOf(SessionID));

			imagesTaken = 0;
			inCapture = false;
		}
		inCapture = false;
		takingAlready = false;
	}

	@TargetApi(19)
	@Override
	public void onImageAvailable(Image im)
	{
		imagesTaken++;
		int frame = 0;
		int frame_len = 0;
		boolean isYUV = false;

		if (im.getFormat() == ImageFormat.YUV_420_888)
		{
			ByteBuffer Y = im.getPlanes()[0].getBuffer();
			ByteBuffer U = im.getPlanes()[1].getBuffer();
			ByteBuffer V = im.getPlanes()[2].getBuffer();

			if ((!Y.isDirect()) || (!U.isDirect()) || (!V.isDirect()))
			{
				Log.e("BestShotCapturePlugin", "Oops, YUV ByteBuffers isDirect failed");
				return;
			}

			// Note: android documentation guarantee that:
			// - Y pixel stride is always 1
			// - U and V strides are the same
			// So, passing all these parameters is a bit overkill
			int status = YuvImage.CreateYUVImage(Y, U, V, im.getPlanes()[0].getPixelStride(),
					im.getPlanes()[0].getRowStride(), im.getPlanes()[1].getPixelStride(),
					im.getPlanes()[1].getRowStride(), im.getPlanes()[2].getPixelStride(),
					im.getPlanes()[2].getRowStride(), MainScreen.getImageWidth(), MainScreen.getImageHeight(), 0);

			if (status != 0)
				Log.e("BestShotCapturePlugin", "Error while cropping: " + status);

			frame = YuvImage.GetFrame(0);
			frame_len = MainScreen.getImageWidth() * MainScreen.getImageHeight() + MainScreen.getImageWidth()
					* ((MainScreen.getImageHeight() + 1) / 2);

			isYUV = true;
		} else if (im.getFormat() == ImageFormat.JPEG)
		{
			Log.e("BestShotCapturePlugin", "JPEG Image received");
			ByteBuffer jpeg = im.getPlanes()[0].getBuffer();

			frame_len = jpeg.limit();
			byte[] jpegByteArray = new byte[frame_len];
			jpeg.get(jpegByteArray, 0, frame_len);

			frame = SwapHeap.SwapToHeap(jpegByteArray);

			if (imagesTaken == 1)
				PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(jpegByteArray, SessionID, -1);
		}

		String frameName = "frame" + imagesTaken;
		String frameLengthName = "framelen" + imagesTaken;

		PluginManager.getInstance().addToSharedMem(frameName + SessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem(frameLengthName + SessionID, String.valueOf(frame_len));
		PluginManager.getInstance().addToSharedMem("frameorientation" + imagesTaken + SessionID,
				String.valueOf(MainScreen.getGUIManager().getDisplayOrientation()));
		PluginManager.getInstance().addToSharedMem("framemirrored" + imagesTaken + SessionID,
				String.valueOf(CameraController.isFrontCamera()));

		PluginManager.getInstance().addToSharedMem("isyuv" + SessionID, String.valueOf(isYUV));

		if (imagesTaken == imageAmount)
		{
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
					String.valueOf(imagesTaken));

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, 
					String.valueOf(SessionID));

			imagesTaken = 0;
			inCapture = false;
		}
		inCapture = false;
		takingAlready = false;
	}

	@TargetApi(19)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		if (result.get(CaptureResult.REQUEST_ID) == requestID)
		{
			if (imagesTaken == 1)
				PluginManager.getInstance().addToSharedMemExifTagsFromCaptureResult(result, SessionID);
		}
	}

	@Override
	public void onAutoFocus(boolean paramBoolean)
	{
		if (takingAlready)
			takePicture();
	}

	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == PluginManager.MSG_NEXT_FRAME)
		{
			// play tick sound
			MainScreen.getGUIManager().showCaptureIndication();
			MainScreen.getInstance().playShutter();

			try
			{
				requestID = CameraController.captureImagesWithParams(imageAmount, CameraController.YUV, 50, new int[0]);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e("Bestshot takePicture() failed", "takePicture: " + e.getMessage());
				inCapture = false;
				takingAlready = false;
				PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, 
						PluginManager.MSG_CONTROL_UNLOCKED);
				MainScreen.getGUIManager().lockControls = false;
			}
			return true;
		}
		return false;
	}

	@Override
	public void onPreviewFrame(byte[] data, Camera paramCamera)
	{
	}
}
