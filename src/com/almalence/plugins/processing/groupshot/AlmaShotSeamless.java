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

package com.almalence.plugins.processing.groupshot;

import com.almalence.util.Size;

import android.graphics.Rect;

public final class AlmaShotSeamless
{

	/**
	 * Initialize Almalence engine, it should be called first.
	 * 
	 * @return status string such as "init status: "
	 */
	public static synchronized native String Initialize();

	/**
	 * Finalize Almalence engine, it should be called at the end.
	 */
	public static synchronized native int Release(int nFrames);

	/**
	 * Prepare JPEG buffer in native for Seamless engine
	 * 
	 * @param frame
	 *            array of JPEG frame buffers
	 * @param frame_len
	 *            array of length of JPEG frame buffers
	 * @param nFrames
	 *            number of frames
	 * @param sx
	 *            width of input frame
	 * @param sy
	 *            height of input frame
	 * @return status string such as "frames total: "
	 */
	public static synchronized native int ConvertAndDetectFacesFromJpegs(int[] frame, int[] frame_len, int nFrames,
			int sx, int sy, int fd_sx, int fd_sy, boolean needRotation, boolean cameraMirrored, int rotationDegree);

	/**
	 * Prepare YUV buffer in native for Seamless engine
	 * 
	 * @param frame
	 *            array of YUV frame buffers
	 * @param frame_len
	 *            array of length of YUV frame buffers
	 * @param nFrames
	 *            number of frames
	 * @param sx
	 *            width of input frame
	 * @param sy
	 *            height of input frame
	 * @return status string such as "frames total: "
	 */
	public static synchronized native int DetectFacesFromYUVs(int[] frame, int[] frame_len, int nFrames, int sx,
			int sy, int fd_sx, int fd_sy, boolean needRotation, boolean cameraMirrored, int rotationDegree);

	public static synchronized native int GetFaces(int index, Face[] faces);

	public static synchronized native int[] NV21toARGB(int inptr, Size src, Rect rect, Size dst);

	public static synchronized native int getInputFrame(int index);

	/**
	 * Initialize Seamless engine
	 * 
	 * @param sx
	 *            width of input frame
	 * @param sy
	 *            height of input frame
	 * @param baseFrame
	 *            to set as background image
	 * @param numOfImg
	 *            number of JPEG frames used for Seamless
	 * @return 1 if all is ok, otherwise false
	 */
	public static synchronized native int Align(int sx, int sy, int baseFrame, int numOfImg);

	/**
	 * Get ARGB8888 pixel data for preview
	 * 
	 * @param baseFrame
	 *            to set as background image
	 * @param inWidth
	 *            input frame width size
	 * @param inHeight
	 *            input frame Height size
	 * @param outWidth
	 *            output frame width size
	 * @param outHeight
	 *            output frame Height size
	 * @param layout
	 *            filled with index of frame.
	 * @return ARGB888 pixel data
	 */
	public static synchronized native int[] Preview(int baseFrame, int inWidth, int inHeight, int outWidth,
			int outHeight, byte[] layout);

	/**
	 * Get NV21(YVU420 planer) as Seamless-processed result
	 * 
	 * @param baseFrame
	 *            to set as background image
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
	public static synchronized native int RealView(int width, int height, int[] crop, byte[] layout);

	/**
	 * Free seamless processing instance
	 */

	static
	{
		System.loadLibrary("utils-image");
		System.loadLibrary("almalib");
		System.loadLibrary("almashot-seamless");
	}
}
