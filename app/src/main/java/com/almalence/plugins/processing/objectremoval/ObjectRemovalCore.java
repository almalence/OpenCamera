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

package com.almalence.plugins.processing.objectremoval;

import java.util.ArrayList;

import android.graphics.Bitmap;
import android.graphics.Paint;

import com.almalence.plugins.processing.multishot.AlmaCLRShot;
import com.almalence.plugins.processing.multishot.AlmaCLRShot.ObjBorderInfo;
import com.almalence.plugins.processing.multishot.AlmaCLRShot.ObjectInfo;
import com.almalence.util.Size;

/* <!-- +++
import com.almalence.focuscam_plus.cameracontroller.CameraController;
+++ --> */
//<!-- -+-
import com.almalence.focuscam.cameracontroller.CameraController;

public class ObjectRemovalCore
{
	private final String				TAG							= this.getClass()
																		   .getName()
																		   .substring(this.getClass().getName().lastIndexOf(".") + 1);
	
	private static int				mSensitivity	= 19;
	private static int				mMinSize		= 1000;
	private static String			mGhosting		= "2";

	private static  AlmaCLRShot		mAlmaCLRShot;
	
	public static int				mDisplayWidth;
	public static int				mDisplayHeight;
	
	
	private ArrayList<Integer>		mYUVBufferList = null;	// List of input images.
	
	private static Bitmap			mPreviewBitmap = null;
	
	public void setYUVBufferList(ArrayList<Integer> YUVBufferList)
	{
		this.mYUVBufferList = YUVBufferList;
	}
	
	public ArrayList<Integer> getYUVBufferList()
	{
		return mYUVBufferList;
	}
	
	public void clear()
	{
		if(mYUVBufferList != null)
			mYUVBufferList.clear();
	}
	
	private ObjectRemovalCore()
	{
		super();
	}
	
	private static ObjectRemovalCore	mInstance;
	
	public static ObjectRemovalCore getInstance()
	{
		if (mInstance == null)
		{
			mInstance = new ObjectRemovalCore();
		}
		return mInstance;
	}
	
	public static void release()
	{
		mAlmaCLRShot.release();
	}
	
	public void initializeParameters(int displayWidth, int displayHeight)
	{
		mDisplayWidth = displayWidth;
		mDisplayHeight = displayHeight;
		
		if(mPreviewBitmap != null)
		{
			mPreviewBitmap.recycle();
			mPreviewBitmap = null;
		}
	}
	
	public void onStartProcessing()
	{
		try
		{
			CameraController.Size imageSize = CameraController.getCameraImageSize();
			Size input = new Size(imageSize.getWidth(), imageSize.getHeight());
			
			Size preview = new Size(mDisplayWidth, mDisplayHeight);
			
			int minSize = 1000;
			if (mMinSize == 0)
			{
				minSize = 0;
			} else
			{
				minSize = input.getWidth() * input.getHeight() / mMinSize;
			}
			
			mAlmaCLRShot = AlmaCLRShot.getInstance();
			// frames!!! should be taken from heap
			mAlmaCLRShot.addInputFrame(mYUVBufferList, input);

			mAlmaCLRShot.initialize(preview, 0,
			/*
			 * -1 : auto mode 0 ~ max number of input frame : manual mode
			 */
			-1,
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
			Integer.parseInt(mGhosting), null, null);
		} catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static byte[] processingSaveData()
	{
		return mAlmaCLRShot.processingSaveData();
	}
	
	public static Bitmap getPreviewBitmap()
	{
		if(mPreviewBitmap == null)
			mPreviewBitmap = mAlmaCLRShot.getPreviewBitmap();
		
		return Bitmap.createBitmap(mPreviewBitmap, 0, 0, mPreviewBitmap.getWidth(), mPreviewBitmap.getHeight(), null, false);
	}
	
	public static int getTotalObjNum()
	{
		return mAlmaCLRShot.getTotalObjNum();
	}
	
	public static synchronized ObjectInfo[] getObjectInfoList()
	{
		return mAlmaCLRShot.getObjectInfoList();
	}
	
	public static synchronized ObjBorderInfo[] getObjBorderBitmap(Paint paint)
	{
		return mAlmaCLRShot.getObjBorderBitmap(paint);
	}
	
	public static int getOccupiedObject(float lx, float ly) throws Exception
	{
		return mAlmaCLRShot.getOccupiedObject(lx, ly);
	}
	
	public static void setObjectList(boolean[] objectIndex) throws Exception
	{
		mAlmaCLRShot.setObjectList(objectIndex);
	}
}
