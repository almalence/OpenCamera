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
Portions created by Initial Developer are Copyright (C) 2016 
by Almalence Inc. All Rights Reserved.
 */

package com.almalence.focusstacking;

import java.util.List;

public final class AlmaShotFocusStacking
{
	private static int[]							mInputFrames;
	private static int								mFramesAmount;
	
	private static List<Float>						mFocusDistances; //Focus distances for each input frame
	
	private static int								mBaseFrameIndex; //Frame around which focus depth will be changed
	private static int								mCurrentFocusDepth; //How much frames to take for focus stacking process
	
	private static byte[]							mFocusAreasMap; //Shows on which input frame desired area is better focused
	
	private static int								mImageWidth;
	private static int								mImageHeight;
	
	private static boolean							mFirstProcessing = true;

	
	/*
	 * PUBLIC API =====================================>
	 */
	
	/**
	 * Initialize Focus stacking engine, it should be called once for current focus bracketed frames set
	 **/
	public static void Initialize(int[] frames, List<Float> focusDistances, int nFrames, int sx, int sy)
	{
		AlmaShotInitialize();
		
		mInputFrames = frames;
		mFramesAmount = nFrames;
		mFocusDistances = focusDistances;
		
		mImageWidth = sx;
		mImageHeight = sy;
		
		mFocusAreasMap = new byte[mImageWidth/16 * mImageHeight/16];
		
		mFirstProcessing = true;
	}
	
	/**
	 * Stops focus stacking instance and finalize Almashot engine
	 * @return 0
	 */
	public static int Release()
	{
		FStackingFreeInstance();
		return AlmaShotRelease();
	}
	
	/**
	 * Get input frame
	 * 
	 * @param index
	 * 				index of desired input frame
	 * @return pointer to input frame in NV21 format
	 */
	public static int GetInputFrame(int index)
	{
		return mInputFrames[index];
	}
	
	
	/**
	 * Get input frame as byte array
	 * @param index
	 * 				index of desired input frame
	 * @param outWidth
	 * 				width of input frame
	 * @param outHeight
	 * 				height of input frame
	 * @param rotate
	 * 				orientation of input frame
	 * @param mirrored
	 * 				flag to show whether frame was shot to front camera or rear camera
	 * @return byte array
	 * 				rotated input frame in NV21 format
	 */
	public static byte[] GetInputByteFrame(int index, int rotate, boolean mirrored)
	{
		return GetInputByteFrameNative(mInputFrames, index, rotate, mirrored);
	}
	
	/**
	 * Process function. Call only after Initialize().	 * 
	 * @param focusDistance - Base focus distance
	 * @param depthOfFocus - desired depth of focus from base focus distance. Depth calculated in both directions: closer and further
	 * @param rotate - desired rotation of result
	 * @param mirrored - is result needs to be mirrored
	 * @param transformResult - if true, engine will transform result according rotate and mirrored arguments
	 * @return byte array of result 'all in focus' frame in NV21 format
	 */
	public static byte[] Process(float focusDistance, int depthOfFocus, int rotate, boolean mirrored, boolean transformResult)
	{
	
		int baseFrameIndex = mFocusDistances.indexOf(focusDistance);
		if(baseFrameIndex == -1)
			return null;
		
		int frameAmount = mFramesAmount;
		int firstFrameIndex = 0;
		int lastFrameIndex = mFramesAmount - 1;
		
		firstFrameIndex = baseFrameIndex - (depthOfFocus - 1);
		lastFrameIndex = baseFrameIndex + (depthOfFocus - 1);
		
		if(firstFrameIndex < 0) firstFrameIndex = 0;
		if(lastFrameIndex > (mFramesAmount - 1)) lastFrameIndex = mFramesAmount - 1;
		
		frameAmount = lastFrameIndex - firstFrameIndex + 1;
		int[] compressed_frame = new int[frameAmount];
		float[] focusDist = new float[frameAmount];
		
		int index = 0;
		for(int i = firstFrameIndex; i <= lastFrameIndex; i++)
		{
			compressed_frame[index] = mInputFrames[i];
			focusDist[index] = mFocusDistances.get(i);
			index++;
		}
		
		byte[] fmap = null;
		if(frameAmount == mFramesAmount && mFirstProcessing)
		{
			fmap = mFocusAreasMap;
			mFirstProcessing = false;
		}
		
		FStackingFreeInstance();
		int initStatus = AlmaShotFocusStacking.FStackingInitialize(compressed_frame, focusDist, frameAmount, mImageWidth, mImageHeight, fmap);
		if(initStatus == -1) //Error. Basically too many input frames (Maximum allowed is 8)
            return null;

		byte[] result = FStackingProcess(rotate, mirrored, transformResult);
		return result;
	}
	

	/**
	 * Get focus areas map
	 * @return Byte array of frame indexes. Each element of array represents 16x16 pixels area on result frame.
	 */
	public static byte[] GetFocusAreasMap()
	{
		return mFocusAreasMap;
	}
	
	/**
	 * Get index of best focused frame in desired coordinates
	 * @param sx - x coordinate
	 * @param sy - y coordinate
	 * @return Index of input frame which best focused on desired point
	 */
	public static int GetFocusedFrameIndex(int sx, int sy)
	{
		int x_lenght = mImageWidth/16;
		
		int index_x = Math.round(sx/16 - 1);
		int index_y = Math.round(sy/16 - 1);
		
		if(index_x < 0) index_x = 0;
		if(index_y < 0) index_y = 0;
		
		int map_index = index_y * x_lenght + index_x;
//		Log.e("FStacking", "map index = " + map_index);
		
		if(map_index > mFocusAreasMap.length - 1)
			return -1;
		
		return mFocusAreasMap[map_index];
	}
	
	/**
	 * Get all aligned frames. JNI's method.
	 * @return array of pointers to aligned frames in NV21 format
	 */
	public static synchronized native int[] GetAlignedFrames();
	/**
	 * Get one aligned frame. JNI's method.
	 * @param index - index of input frame which aligned version is requested
	 * @return pointer to aligned frame in NV21 format
	 */
	public static synchronized native int GetAlignedFrame(int index);
	
	
	/*
	 * PRIVATE API ======================================>
	 */
	
	/**
	 * Initialize Almalence engine.
	 * Private method used in public Initialize method.
	 * 
	 * @return status string such as "init status: "
	 */
	private static synchronized native String AlmaShotInitialize();
	
	
	/**
	 * Initialize Focus stacking jni-engine.
	 * This is a private method. Used in public method Process
	 * 
	 * @param frames
	 * 					input frames in NV21 format
	 * @param focusDistances
	 * 					array of focus distances for each frame in diopter 1\m
	 * @param nFrames
	 * 					number of input frames
	 * @param sx
	 * 					width of input frames
	 * @param sy
	 * 					height of input frames
	 * @param fmap
	 * 					array of frame indexes for each AoI (Area of Interest) 
	 * @return status string such as "init status: "
	 */
	private static synchronized native int FStackingInitialize(int[] frames, float[] focusDistances, int nFrames, int sx, int sy, byte[] fmap);
	
	
	/**
	 * Finalize Focus stacking instance.
	 * Used in public method Release()
	 */
	private static synchronized native int FStackingFreeInstance();
	
	
	/**
	 * Finalize Almalence engine.
	 * Used in public method Release()
	 */
	private static synchronized native int AlmaShotRelease();
	
	/**
	 * Get NV21(YVU420 planer) as Seamless-processed result
	 * Private method. Used in public method Process
	 * 
	 * @param width
	 *            input frame width size
	 * @param height
	 *            input frame Height size
	 * @param crop
	 *            array of 4 integer that contains x, y, width, height for
	 *            cropping
	 * @param layout
	 *            filled with index of frame.
	 * @return NV21(YVU420 planer)
	 */
	private static synchronized native byte[] FStackingProcess(int rotate, boolean mirrored, boolean transformResult);
	
	/**
	 * Get input frame by index as byte array. JNI's method
	 * 
	 * @param frames - array of input frames
	 * @param index - desired input frame
	 * @param rotate - desired rotation of frame
	 * @param mirrored - is need to mirror frame
	 * @return input frame as byte array in NV21 format
	 */
	private static synchronized native byte[] GetInputByteFrameNative(int[] frames, int index, int rotate, boolean mirrored);
	
	static
	{
		System.loadLibrary("almalib");
		System.loadLibrary("almashot-fstacking");
	}
}

