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

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.hardware.Camera;
import android.hardware.camera2.CaptureResult;
import android.media.Image;
import android.os.CountDownTimer;
import android.os.Debug;
import android.preference.PreferenceManager;
import android.util.Log;

//-+- -->
import com.almalence.SwapHeap;
import com.almalence.YuvImage;
import com.almalence.opencam.MainScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
/* <!-- +++
 import com.almalence.opencam_plus.CameraController;
 import com.almalence.opencam_plus.MainScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.cameracontroller.CameraController;

/***
 * Implements group shot capture plugin - captures predefined number of images
 ***/

public class MultiShotCapturePlugin extends PluginCapture
{

	private static final String					TAG					= "MultiShotCapturePlugin";

	private static final int					MIN_MPIX_SUPPORTED	= 1280 * 960;
	private static final int					MIN_MPIX_PREVIEW	= 600 * 400;
	private static final long					MPIX_8				= 3504 * 2336;
	private static final long					MPIX_1080			= 1920 * 1088;

	private static List<CameraController.Size>	ResolutionsSizesList;							;
	private static List<Long>					ResolutionsMPixList;
	private static List<String>					ResolutionsIdxesList;
	private static List<String>					ResolutionsNamesList;

	private static int							captureIndex		= -1;

	public static int getCaptureIndex()
	{
		return captureIndex;
	}

	private static int	imgCaptureWidth		= 0;
	private static int	imgCaptureHeight	= 0;

	public static List<Long> getResolutionsMPixList()
	{
		return ResolutionsMPixList;
	}

	public static List<String> getResolutionsIdxesList()
	{
		return ResolutionsIdxesList;
	}

	public static List<String> getResolutionsNamesList()
	{
		return ResolutionsNamesList;
	}

	// defaul val. value should come from config
	private int		imageAmount			= 8;
	private int[]	pauseBetweenShots	= { 0, 0, 250, 250, 500, 750, 1000, 1250 };
	private int		imagesTaken			= 0;

	public MultiShotCapturePlugin()
	{
		super("com.almalence.plugins.multishotcapture", 0, 0, 0, null);
	}

	@Override
	public void onResume()
	{
		takingAlready = false;
		imagesTaken = 0;
		inCapture = false;

		MainScreen.setCaptureYUVFrames(true);
	}

	@Override
	public void onGUICreate()
	{
		MainScreen.getGUIManager().showHelp(MainScreen.getInstance().getString(R.string.MultiShot_Help_Header),
				MainScreen.getInstance().getResources().getString(R.string.MultiShot_Help),
				R.drawable.plugin_help_object, "multiShotShowHelp");
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
			takingAlready = true;

			if (imagesTaken == 0 || imagesTaken == 1)
			{
				PluginManager.getInstance().sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_NEXT_FRAME);
			} else
			{
				new CountDownTimer(pauseBetweenShots[imagesTaken], pauseBetweenShots[imagesTaken])
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
		imagesTaken++;
		int frame_len = paramArrayOfByte.length;
		int frame = SwapHeap.SwapToHeap(paramArrayOfByte);

		if (frame == 0)
		{
			Log.i(TAG, "Load to heap failed");

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

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

		PluginManager.getInstance().addToSharedMem("isyuv" + SessionID, String.valueOf(false));

		if (imagesTaken == 1)
			PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(paramArrayOfByte, SessionID, -1);
		try
		{
			paramCamera.startPreview();
		} catch (RuntimeException e)
		{
			Log.i(TAG, "StartPreview fail");

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

			imagesTaken = 0;
			MainScreen.getInstance().muteShutter(false);
			inCapture = false;
			return;
		}
		if (imagesTaken < imageAmount)
		{
			inCapture = false;
			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
		} else
		{
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
					String.valueOf(imagesTaken));

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

			imagesTaken = 0;
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
		takingAlready = false;
	}

	@TargetApi(19)
	@Override
	public void onImageAvailable(Image im)
	{
		imagesTaken++;

		ByteBuffer Y = im.getPlanes()[0].getBuffer();
		ByteBuffer U = im.getPlanes()[1].getBuffer();
		ByteBuffer V = im.getPlanes()[2].getBuffer();

		if ((!Y.isDirect()) || (!U.isDirect()) || (!V.isDirect()))
		{
			Log.e("CapturePlugin", "Oops, YUV ByteBuffers isDirect failed");
			return;
		}

		// Note: android documentation guarantee that:
		// - Y pixel stride is always 1
		// - U and V strides are the same
		// So, passing all these parameters is a bit overkill
		int status = YuvImage.CreateYUVImage(Y, U, V, im.getPlanes()[0].getPixelStride(),
				im.getPlanes()[0].getRowStride(), im.getPlanes()[1].getPixelStride(), im.getPlanes()[1].getRowStride(),
				im.getPlanes()[2].getPixelStride(), im.getPlanes()[2].getRowStride(), MainScreen.getImageWidth(),
				MainScreen.getImageHeight(), 0);

		if (status != 0)
			Log.e("CapturePlugin", "Error while cropping: " + status);

		int frame = YuvImage.GetFrame(0);
		int frame_len = MainScreen.getImageWidth() * MainScreen.getImageHeight() + MainScreen.getImageWidth()
				* ((MainScreen.getImageHeight() + 1) / 2);

		if (frame == 0)
		{
			Log.e(TAG, "Load to heap failed");

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

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

		PluginManager.getInstance().addToSharedMem("isyuv" + SessionID, String.valueOf(true));

		try
		{
			CameraController.startCameraPreview();
		} catch (RuntimeException e)
		{
			Log.e(TAG, "StartPreview fail");

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

			imagesTaken = 0;
			MainScreen.getInstance().muteShutter(false);
			inCapture = false;
			return;
		}
		if (imagesTaken < imageAmount)
		{
			inCapture = false;
			MainScreen.getMessageHandler().sendEmptyMessage(PluginManager.MSG_TAKE_PICTURE);
		} else
		{
			PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
					String.valueOf(imagesTaken));

			PluginManager.getInstance().sendMessage(PluginManager.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));

			imagesTaken = 0;
			inCapture = false;
		}

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
				requestID = CameraController.captureImage(1, CameraController.YUV);
			} catch (Exception e)
			{
				e.printStackTrace();
				Log.e(TAG, "CameraController.captureImage failed: " + e.getMessage());
				inCapture = false;
				takingAlready = false;
				PluginManager.getInstance()
						.sendMessage(PluginManager.MSG_BROADCAST, PluginManager.MSG_CONTROL_UNLOCKED);
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
			MainScreen.setSaveImageWidth(imgCaptureWidth);
			MainScreen.setSaveImageHeight(imgCaptureHeight);

			MainScreen.setImageWidth(imgCaptureWidth);
			MainScreen.setImageHeight(imgCaptureHeight);
		}
	}

	public static void selectImageDimensionMultishot()
	{
		populateCameraDimensions();

		long maxMem = Runtime.getRuntime().maxMemory() - Debug.getNativeHeapAllocatedSize();
		long maxMpix = (maxMem - 1000000) / 3; // 2 x Mpix - result, 1/4 x Mpix
												// x 4 - compressed input jpegs,
												// 1Mb - safe reserve

		if (maxMpix < MIN_MPIX_SUPPORTED)
		{
			String msg;
			msg = "MainScreen.selectImageDimension maxMem = " + maxMem;
			Log.e("NightCapturePlugin", "MainScreen.selectImageDimension maxMpix < MIN_MPIX_SUPPORTED");
			Log.e("NightCapturePlugin", msg);
		}

		// find index selected in preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int prefIdx = Integer.parseInt(prefs.getString(
				CameraController.getCameraIndex() == 0 ? "imageSizePrefSmartMultishotBack"
						: "imageSizePrefSmartMultishotFront", "-1"));

		// ----- Find max-resolution capture dimensions
		int minMPIX = MIN_MPIX_PREVIEW;

		int defaultCaptureIdx = -1;
		long defaultCaptureMpix = 0;
		int defaultCaptureWidth = 0;
		int defaultCaptureHeight = 0;
		long captureMpix = 0;
		int captureWidth = 0;
		int captureHeight = 0;
		int captureIdx = -1;
		boolean prefFound = false;

		// figure default resolution
		for (int ii = 0; ii < ResolutionsSizesList.size(); ++ii)
		{
			CameraController.Size s = ResolutionsSizesList.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((mpix >= minMPIX) && (mpix < maxMpix) && (mpix > defaultCaptureMpix) && (mpix <= MPIX_8))
			{
				defaultCaptureIdx = ii;
				defaultCaptureMpix = mpix;
				defaultCaptureWidth = s.getWidth();
				defaultCaptureHeight = s.getHeight();
			}
		}

		for (int ii = 0; ii < ResolutionsSizesList.size(); ++ii)
		{
			CameraController.Size s = ResolutionsSizesList.get(ii);
			long mpix = (long) s.getWidth() * s.getHeight();

			if ((ii == prefIdx) && (mpix >= minMPIX))
			{
				prefFound = true;
				captureIdx = ii;
				captureMpix = mpix;
				captureWidth = s.getWidth();
				captureHeight = s.getHeight();
				break;
			}

			if (mpix > captureMpix)
			{
				captureIdx = ii;
				captureMpix = mpix;
				captureWidth = s.getWidth();
				captureHeight = s.getHeight();
			}
		}

		// default to about 8Mpix if nothing is set in preferences or maximum
		// resolution is above memory limits
		if (defaultCaptureMpix > 0 && !prefFound)
		{
			captureIdx = defaultCaptureIdx;
			captureMpix = defaultCaptureMpix;
			captureWidth = defaultCaptureWidth;
			captureHeight = defaultCaptureHeight;
		}

		captureIndex = captureIdx;
		imgCaptureWidth = captureWidth;
		imgCaptureHeight = captureHeight;
	}

	public static void populateCameraDimensions()
	{
		ResolutionsMPixList = new ArrayList<Long>();
		ResolutionsIdxesList = new ArrayList<String>();
		ResolutionsNamesList = new ArrayList<String>();

		List<CameraController.Size> cs;
		int minMPIX = MIN_MPIX_PREVIEW;
		cs = CameraController.getInstance().getResolutionsSizeList();
		ResolutionsSizesList = new ArrayList<CameraController.Size>(cs);

		List<CameraController.Size> csPreview = CameraController.getInstance().getSupportedPreviewSizes();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
		int prefIdx = Integer.parseInt(prefs.getString("imageSizePrefSmartMultishotBack", "-1"));
		for (int i = 0; i < ResolutionsSizesList.size(); i++)
		{
			CameraController.Size s = ResolutionsSizesList.get(i);
			CameraController.Size sPreview = csPreview.get(0);

			if (s.getHeight() * s.getWidth() < sPreview.getHeight() * sPreview.getWidth())
			{
				ResolutionsSizesList.add(i, sPreview);
				if (sPreview.getHeight() * sPreview.getWidth() > MPIX_1080) {
					if (prefIdx == -1) {
						SharedPreferences.Editor prefEditor = prefs.edit();
						prefEditor.putString("imageSizePrefSmartMultishotBack", String.valueOf(i));
						prefEditor.commit();
					}
				}
				break;
			}

			if ((ResolutionsSizesList.size() - 1 == i)
					&& (s.getHeight() * s.getWidth() != sPreview.getHeight() * sPreview.getWidth()))
			{
				ResolutionsSizesList.add(sPreview);
				if (sPreview.getHeight() * sPreview.getWidth() > MPIX_1080) {
					if (prefIdx == -1) {
						SharedPreferences.Editor prefEditor = prefs.edit();
						prefEditor.putString("imageSizePrefSmartMultishotBack", String.valueOf(i + 1));
						prefEditor.commit();
					}
				}
				break;
			}
		}

		for (int i = 0; i < ResolutionsSizesList.size(); i++)
		{
			CameraController.Size s = ResolutionsSizesList.get(i);
			CameraController.Size sPreview = csPreview.get(1);

			if (s.getHeight() * s.getWidth() < sPreview.getHeight() * sPreview.getWidth())
			{
				ResolutionsSizesList.add(i, sPreview);
				break;
			}

			if ((ResolutionsSizesList.size() - 1 == i)
					&& (s.getHeight() * s.getWidth() != sPreview.getHeight() * sPreview.getWidth()))
			{
				ResolutionsSizesList.add(sPreview);
				break;
			}
		}

		CharSequence[] ratioStrings = { " ", "4:3", "3:2", "16:9", "1:1" };

		for (int ii = 0; ii < ResolutionsSizesList.size(); ++ii)
		{
			CameraController.Size s = ResolutionsSizesList.get(ii);

			if ((long) s.getWidth() * s.getHeight() < minMPIX)
				continue;

			Long lmpix = (long) s.getWidth() * s.getHeight();
			float mpix = (float) lmpix / 1000000.f;
			float ratio = (float) s.getWidth() / s.getHeight();

			// find good location in a list
			int loc;
			for (loc = 0; loc < ResolutionsMPixList.size(); ++loc)
				if (ResolutionsMPixList.get(loc) < lmpix)
					break;

			int ri = 0;
			if (Math.abs(ratio - 4 / 3.f) < 0.1f)
				ri = 1;
			if (Math.abs(ratio - 3 / 2.f) < 0.12f)
				ri = 2;
			if (Math.abs(ratio - 16 / 9.f) < 0.15f)
				ri = 3;
			if (Math.abs(ratio - 1) == 0)
				ri = 4;

			ResolutionsNamesList.add(loc, String.format("%3.1f Mpix  " + ratioStrings[ri], mpix));
			ResolutionsIdxesList.add(loc, String.format("%d", ii));
			ResolutionsMPixList.add(loc, lmpix);
		}
	}
}