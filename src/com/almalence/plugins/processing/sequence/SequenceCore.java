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

package com.almalence.plugins.processing.sequence;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.PointF;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.almalence.SwapHeap;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.plugins.capture.expobracketing.ExpoBracketingCapturePlugin;
import com.almalence.plugins.processing.groupshot.GroupShotCore;
import com.almalence.util.ImageConversion;
import com.almalence.util.Size;

/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 +++ --> */
//<!-- -+-

//-+- -->

public class SequenceCore
{
	private final String				TAG							= this.getClass()
																			.getName()
																			.substring(this.getClass().getName().lastIndexOf(".") + 1);
	
	
	private static int					mSensitivity	= 22;
	private static int					mMinSize		= 1000;
	private static String				mGhosting		= "0";

	private Handler						mHandler		= null;
	
	private int							mImageDataOrientation;
	private boolean						mCameraMirrored;
	public static int					mDisplayWidth;
	public static int					mDisplayHeight;
	
	public int							imagesAmount	 = 0;

	private AlmaCLRShot					mAlmaCLRShot;
	private int[]						indexes;
	
	private ArrayList<Integer>			mYUVBufferList;	// List of input images.

	public void setYUVBufferList(ArrayList<Integer> YUVBufferList)
	{
		this.mYUVBufferList = YUVBufferList;
	}
	
	public ArrayList<Integer> getYUVBufferList()
	{
		return mYUVBufferList;
	}
	
	private SequenceCore()
	{
		super();
	}
	
	private static SequenceCore	mInstance;
	
	public static SequenceCore getInstance()
	{
		if (mInstance == null)
		{
			mInstance = new SequenceCore();
		}
		return mInstance;
	}
	
	public void initializeParameters(int imgAmount, boolean mirrored, int imageDataOrientation, Handler handler)
	{
		imagesAmount = imgAmount;
		mCameraMirrored = mirrored;
		mImageDataOrientation = imageDataOrientation;
		mHandler = handler;
	}
	
	public void release()
	{
		mAlmaCLRShot.release();
	}
	
	public void onStartProcessing()
	{
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		Size input = new Size(imageSize.getWidth(), imageSize.getHeight());
		int minSize = 1000;
		if (mMinSize == 0)
		{
			minSize = 0;
		} else
		{
			minSize = input.getWidth() * input.getHeight() / mMinSize;
		}
		
		int iImageWidth = imageSize.getWidth();
		int iImageHeight = imageSize.getHeight();
		
		mAlmaCLRShot = AlmaCLRShot.getInstance();
		
		Display display = ((WindowManager) ApplicationScreen.instance.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		Point dis = new Point();
		display.getSize(dis);

		float imageRatio = (float) iImageWidth / (float) iImageHeight;
		float displayRatio = (float) dis.y / (float) dis.x;

		if (imageRatio > displayRatio)
		{
			mDisplayWidth = dis.y;
			mDisplayHeight = (int) ((float) dis.y / (float) imageRatio);
		} else
		{
			mDisplayWidth = (int) ((float) dis.x * (float) imageRatio);
			mDisplayHeight = dis.x;
		}

		Size preview = new Size(mDisplayWidth, mDisplayHeight);			

		this.indexes = new int[imagesAmount];
		for (int i = 0; i < imagesAmount; i++)
		{
			this.indexes[i] = i;
		}

		try
		{
			// frames!!! should be taken from heap
			mAlmaCLRShot.addYUVInputFrame(mYUVBufferList, input);
	
			mAlmaCLRShot.initialize(preview,
			/*
			 * sensitivity for objection detection
			 */
			mSensitivity - 15,
			/*
			 * Minimum size of object to be able to detect -15 ~ 15 max -> easy
			 * detection dull detection min ->
			 */
			minSize,
			/*
			 * ghosting parameter 0 : normal operation 1 : detect ghosted
			 * objects but not remove them 2 : detect and remove all object
			 */
			Integer.parseInt(mGhosting), indexes);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public Bitmap getPreviewBitmap()
	{
		return mAlmaCLRShot.getPreviewBitmap();
	}
	
	public void runProcessingTask(int[] idx)
	{
		ProcessingTask task = new ProcessingTask();
		task.idxInput = idx;
		task.executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
	}

	private class ProcessingTask extends AsyncTask<Void, Void, Void>
	{
		public int[] idxInput;

		@Override
		protected void onPreExecute()
		{
			SequenceProcessingPlugin.setProgressBarVisibility(true);
		}
		
		@Override
		protected Void doInBackground(Void... params)
		{
			android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_DEFAULT);
			CameraController.Size imageSize = CameraController.getCameraImageSize();
			Size input = new Size(imageSize.getWidth(), imageSize.getHeight());
			int minSize = 1000;
			if (mMinSize == 0)
			{
				minSize = 0;
			} else
			{
				minSize = input.getWidth() * input.getHeight() / mMinSize;
			}

			Size preview = new Size(mDisplayWidth, mDisplayHeight);
			try
			{
				
				
				mAlmaCLRShot.initialize(preview,
				/*
				 * sensitivity for objection detection
				 */
				mSensitivity - 15,
				/*
				 * Minimum size of object to be able to detect -15 ~ 15 max -> easy
				 * detection dull detection min ->
				 */
				minSize,
				/*
				 * ghosting parameter 0 : normal operation 1 : detect ghosted
				 * objects but not remove them 2 : detect and remove all object
				 */
				Integer.parseInt(mGhosting), idxInput);
			} catch (NumberFormatException e)
			{
				e.printStackTrace();
			} catch (Exception e)
			{
				e.printStackTrace();
			}

			return null;
		}
		
		@Override
		protected void onPostExecute(Void result)
		{
			SequenceProcessingPlugin.setProgressBarVisibility(false);
			mHandler.sendEmptyMessage(SequenceProcessingPlugin.MSG_REDRAW);
		}
	}
	
	public void processAndSaveData(long sessionID)
	{
		byte[] result = mAlmaCLRShot.processingSaveData();
		int frame_len = result.length;
		int frame = SwapHeap.SwapToHeap(result);

		PluginManager.getInstance().addToSharedMem("resultframeformat1" + sessionID, "jpeg");
		PluginManager.getInstance().addToSharedMem("resultframe1" + sessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem("resultframelen1" + sessionID, String.valueOf(frame_len));

		//Nexus 6 and 6p has a original front camera sensor orientation, we have to manage it
		PluginManager.getInstance().addToSharedMem("resultframeorientation1" + sessionID, String.valueOf(mImageDataOrientation));
//				String.valueOf((CameraController.isFlippedSensorDevice() && mCameraMirrored)? (mImageDataOrientation + 180) % 360 : mImageDataOrientation));
		PluginManager.getInstance().addToSharedMem("resultframemirrored1" + sessionID, String.valueOf(mCameraMirrored));

		PluginManager.getInstance().addToSharedMem("amountofresultframes" + sessionID, String.valueOf(1));

		PluginManager.getInstance().addToSharedMem("sessionID", String.valueOf(sessionID));
		mAlmaCLRShot.release();
	}
}
