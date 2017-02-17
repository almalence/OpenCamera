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

public final class AlmaShotMPOWriter
{
	private static int[]							mInputFrames;
    private static int[]							mInputFramesSizes;
	private static int								mFramesAmount;
	
	private static float[]						    mFocusDistances; //Focus distances for each input frame
	
	/*
	 * PUBLIC API =====================================>
	 */
	
	/**
	 * Initialize Focus stacking engine, it should be called once for current focus bracketed frames set
	 **/
	public static void Initialize(int[] frames, int[] sizes, List<Float> focusDistances, int nFrames)
	{
		mInputFrames = frames;
        mInputFramesSizes = sizes;
		mFramesAmount = nFrames;

        mFocusDistances = new float[nFrames];

        for(int i = 0; i < nFrames; i++)
            mFocusDistances[i] = focusDistances.get(i);

		MPOWriterInitialize(mInputFrames, mInputFramesSizes, mFocusDistances, mFramesAmount );
	}
	
	/**
	 * Stops focus stacking instance and finalize Almashot engine
	 * @return 0
	 */
	public static int Release()
	{
		return MPOWriterRelease();
	}

    public static byte[] ConstructMPOData()
    {
        byte[] result = MPOWriterProcess();
        return result;
    }
	

	/*
	 * PRIVATE API ======================================>
	 */
	

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
	private static synchronized native int MPOWriterInitialize(int[] frames, int[] sizes, float[] focusDistances, int nFrames);
	
	
	/**
	 * Finalize Almalence engine.
	 * Used in public method Release()
	 */
	private static synchronized native int MPOWriterRelease();

    private static synchronized  native byte[] MPOWriterProcess();
	

	static
	{
		System.loadLibrary("mpo-writer");
	}
}

