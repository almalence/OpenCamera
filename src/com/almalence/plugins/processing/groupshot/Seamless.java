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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.ImageFormat;
import android.graphics.Rect;
import android.preference.PreferenceManager;
import android.util.Log;

import com.almalence.SwapHeap;
import com.almalence.util.Size;

/* <!-- +++
import com.almalence.opencam_plus.MainScreen;
+++ --> */
//<!-- -+-
import com.almalence.opencam.MainScreen;
//-+- -->

public class Seamless
{
	private final String				TAG							= this.getClass()
																			.getName()
																			.substring(
																					this.getClass().getName()
																							.lastIndexOf(".") + 1);

	private static final int			IMAGE_TO_LAYOUT				= 8;
	private static final int			MAX_INPUT_FRAME				= 8;
	private static final int			MAX_WIDTH_FOR_FACEDETECTION	= 1280;

	private int							mNumOfFrame					= 0;
	private int							mBasebaseFrameIndex;
	private Size						mPreviewSize;
	private Size						mInputFrameSize;
	private Bitmap						mBitmap;
	private int[]						ARGBBuffer					= null;
	private int							mOutNV21;
	private int[]						mCrop;
	private ArrayList<ArrayList<Rect>>	mRectList					= null;

	private byte[]						mLayout;
	private byte[]						mClonedLayout;
	private Size						mLayoutSize;
	private int[][]						mChosenFace;
	private boolean						mIsBaseFrameChanged			= false;

	private Seamless()
	{
		super();
	}

	private static final Seamless	mInstance	= new Seamless();

	public static Seamless getInstance()
	{
		return mInstance;
	}

	public enum ImageType
	{
		JPEG, YUV420SP, YVU420SP
	}

	public class FaceThumb
	{
		public Rect		mRect;
		public Bitmap	mBitmap;

		public FaceThumb(Rect rect, Bitmap bitmap)
		{
			mRect = rect;
			mBitmap = bitmap;
		}
	}	

	public void addYUVInputFrames(List<Integer> inputFrame, Size size, Size fd_size, boolean needRotation,
			boolean cameraMirrored, int rotationDegree) throws Exception
	{
		mNumOfFrame = inputFrame.size();
		if (rotationDegree != 0 && rotationDegree != 180)
			mInputFrameSize = new Size(size.getHeight(), size.getWidth());
		else
			mInputFrameSize = size;
//		Log.d("Seamless", "mInputFrameSize WxH = " + mInputFrameSize.getWidth() + " x " + mInputFrameSize.getHeight());

		if (mNumOfFrame < 1 && mNumOfFrame > 8)
		{
			throw new Exception("Number of input frame is wrong");
		}

		AlmaShotSeamless.Initialize();

		int[] PointOfYUVData = new int[mNumOfFrame];
		int[] LengthOfYUVData = new int[mNumOfFrame];

		int data_lenght = mInputFrameSize.getWidth() * mInputFrameSize.getHeight() + 2
				* ((mInputFrameSize.getWidth() + 1) / 2) * ((mInputFrameSize.getHeight() + 1) / 2);
		for (int i = 0; i < mNumOfFrame; i++)
		{
			PointOfYUVData[i] = inputFrame.get(i);
			LengthOfYUVData[i] = data_lenght;
			if (PointOfYUVData[i] == 0)
			{
				Log.d(TAG, "Out of Memory in Native");
				throw new Exception("Out of Memory in Native");
			}
		}

//		long start = System.currentTimeMillis();
		int error = 0;
		error = AlmaShotSeamless
				.DetectFacesFromYUVs(PointOfYUVData, LengthOfYUVData, mNumOfFrame, size.getWidth(), size.getHeight(),
						fd_size.getWidth(), fd_size.getHeight(), needRotation, cameraMirrored, rotationDegree);
//		Log.d(TAG, "DetectFracesFromYUVs() elapsed time = " + (System.currentTimeMillis() - start));
		if (error < 0)
		{
			Log.d(TAG, "Out Of Memory");
			throw new Exception("Out Of Memory");
		} else if (error < MAX_INPUT_FRAME)
		{
			Log.d(TAG, "JPEG buffer is wrong in " + error + " frame");
			throw new Exception("Out Of Memory");
		}

		return;
	}

	public boolean initialize(int baseFrameIndex, ArrayList<ArrayList<Rect>> faceInfoList, Size preview)
			throws Exception
	{
		if (mNumOfFrame == 0)
		{
			throw new Exception("Input frames not added");
		}
		if (baseFrameIndex >= mNumOfFrame && baseFrameIndex < -1)
		{
			throw new Exception("baseFrameIndex is wrong : baseFrameIndex = " + baseFrameIndex);
		}

		mBasebaseFrameIndex = baseFrameIndex;
		mPreviewSize = preview;
		mLayoutSize = new Size(mInputFrameSize.getWidth() / IMAGE_TO_LAYOUT, mInputFrameSize.getHeight()
				/ IMAGE_TO_LAYOUT);
		mBitmap = Bitmap.createBitmap(mPreviewSize.getWidth(), mPreviewSize.getHeight(), Config.ARGB_8888);

		int layoutLength = mLayoutSize.getWidth() * mLayoutSize.getHeight();
		mLayout = new byte[layoutLength];

		if (AlmaShotSeamless.Align(mInputFrameSize.getWidth(), mInputFrameSize.getHeight(), mBasebaseFrameIndex,
				mNumOfFrame) != 0)
		{
			throw new Exception("Align : error");
		}

		mRectList = faceInfoList;
		int max = 0;
		for (int i = 0; i < mRectList.size(); i++)
		{
			if (max < mRectList.get(i).size())
			{
				max = mRectList.get(i).size();
			}
		}

		mChosenFace = new int[mRectList.size()][max];
		for (int i = 0; i < mRectList.size(); i++)
		{
			Arrays.fill(mChosenFace[i], i);
		}

		return true;
	}

	public Bitmap getPreviewBitmap()
	{
		if (ARGBBuffer == null || mIsBaseFrameChanged)
		{
			ARGBBuffer = null;
			System.gc();
			makePreview(mRectList.get(mBasebaseFrameIndex));
			mIsBaseFrameChanged = false;
		}
		mBitmap.setPixels(ARGBBuffer, 0, mPreviewSize.getWidth(), 0, 0, mPreviewSize.getWidth(),
				mPreviewSize.getHeight());
		return mBitmap;
	}

	private boolean isFarDistance(Rect src, Rect dst)
	{
		if (dst.centerX() > src.left && dst.centerX() < src.right)
		{
			return false;
		}

		if (dst.centerY() > src.top && dst.centerY() < src.bottom)
		{
			return false;
		}
		return true;

	}

	public boolean changeFace(int faceIndex, int frameIndex)
	{
		mChosenFace[mBasebaseFrameIndex][faceIndex] = frameIndex;
		makePreview(mRectList.get(mBasebaseFrameIndex));
		return true;
	}

	public boolean setBaseFrame(int base)
	{
		mBasebaseFrameIndex = base;
		mIsBaseFrameChanged = true;
		return true;
	}

	public byte[] processingSaveData()
	{
		byte[] jpegBuffer = null;

		try
		{
			mCrop = new int[5];
			mOutNV21 = AlmaShotSeamless.RealView(mInputFrameSize.getWidth(), mInputFrameSize.getHeight(), mCrop,
					mLayout);

			android.graphics.YuvImage out = new android.graphics.YuvImage(SwapHeap.SwapFromHeap(mOutNV21,
					mInputFrameSize.getWidth() * mInputFrameSize.getHeight() * 3 / 2), ImageFormat.NV21,
					mInputFrameSize.getWidth(), mInputFrameSize.getHeight(), null);
			mOutNV21 = 0;

			ByteArrayOutputStream os = new ByteArrayOutputStream();

			Rect r = new Rect(mCrop[0], mCrop[1], mCrop[0] + mCrop[2], mCrop[1] + mCrop[3]);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(MainScreen.getMainContext());
			int jpegQuality = Integer.parseInt(prefs.getString(MainScreen.sJPEGQualityPref, "95"));
			if (!out.compressToJpeg(r, jpegQuality, os))
			{
				Log.d(TAG, "the compression is not successful");
			}
			jpegBuffer = os.toByteArray();
			os.close();

		} catch (Exception e)
		{
			Log.d(TAG, "Exception occured");
			e.printStackTrace();
		}

		return jpegBuffer;
	}

	public List<FaceThumb> getFacePreview(int indexOfSelectedFace) throws Exception
	{
		int i = 0;
		ArrayList<FaceThumb> list = new ArrayList<FaceThumb>();

		for (ArrayList<Rect> rect : mRectList)
		{
			if (rect.size() < indexOfSelectedFace)
			{
				throw new Exception("length of face rect array is less than indexOfSelectedFace");
			}
			Rect faceRect = rect.get(indexOfSelectedFace);

			float radius = getRadius(faceRect);
			int left = faceRect.centerX() - (int) radius;
			int top = faceRect.centerY() - (int) radius;
			int right = faceRect.centerX() + (int) radius;
			int bottom = faceRect.centerY() + (int) radius;
			if (left < 0)
				left = 0;
			if (right > mInputFrameSize.getWidth())
				right = mInputFrameSize.getWidth();
			if (top < 0)
				top = 0;
			if (bottom > mInputFrameSize.getHeight())
				bottom = mInputFrameSize.getHeight();

			if ((right - left) > (bottom - top))
			{
				left = ((right + left) / 2) - ((bottom - top) / 2);
				right = ((right + left) / 2) + ((bottom - top) / 2);
			} else if ((right - left) < (bottom - top))
			{
				top = ((bottom + top) / 2) - ((right - left) / 2);
				bottom = ((bottom + top) / 2) + ((right - left) / 2);
			}

			Rect newRect = new Rect(left, top, right, bottom);

			Bitmap bitmap = Bitmap.createBitmap(newRect.width(), newRect.height(), Config.ARGB_8888);
			Size size = new Size(newRect.width(), newRect.height());
			int[] buffer = null;
			buffer = AlmaShotSeamless.NV21toARGB(AlmaShotSeamless.getInputFrame(i), mInputFrameSize, newRect, size);
			bitmap.setPixels(buffer, 0, newRect.width(), 0, 0, newRect.width(), newRect.height());
			buffer = null;
			list.add(new FaceThumb(newRect, bitmap));
			i++;
		}
		return list;
	}

	public int getWidthForFaceDetection(int w, int h)
	{
		// compute
		int ds = (Math.max(w, h) + MAX_WIDTH_FOR_FACEDETECTION - 1) / MAX_WIDTH_FOR_FACEDETECTION;
		return w / ds;
	}

	public int getHeightForFaceDetection(int w, int h)
	{
		// compute
		int ds = (Math.max(w, h) + MAX_WIDTH_FOR_FACEDETECTION - 1) / MAX_WIDTH_FOR_FACEDETECTION;
		return h / ds;
	}

	public void release()
	{
		AlmaShotSeamless.Release(mNumOfFrame);
		mPreviewSize = null;
		mInputFrameSize = null;
		if (mBitmap != null)
		{
			mBitmap = null;
		}
		ARGBBuffer = null;
		mCrop = null;
		mRectList = null;

		mLayout = null;
		mClonedLayout = null;
		mLayoutSize = null;
		mChosenFace = null;

		if (mOutNV21 != 0)
		{
			SwapHeap.FreeFromHeap(mOutNV21);
			mOutNV21 = 0;
		}

		try
		{
			this.finalize();
		} catch (Throwable e)
		{
			e.printStackTrace();
		}
		return;
	}

	private void prepareLayout(List<Rect> faceRect, int baseFrame)
	{
		fillLayoutwithBaseFrame(baseFrame);
		fillLayoutwithStitchingflag(faceRect);
		fillLayoutwithFaceindex(faceRect);
	}

	private void makePreview(List<Rect> faceRect)
	{
		prepareLayout(faceRect, mBasebaseFrameIndex);

		mClonedLayout = mLayout.clone();
		ARGBBuffer = AlmaShotSeamless.Preview(mBasebaseFrameIndex, mInputFrameSize.getWidth(),
				mInputFrameSize.getHeight(), mPreviewSize.getWidth(), mPreviewSize.getHeight(), mClonedLayout);
		mClonedLayout = null;
		return;
	}

	private boolean checkDistance(float radius, float x, float y, int centerX, int centerY)
	{
		float distance = getSquareOfDistance((int) x, (int) y, centerX, centerY);
		if (distance < (radius * radius))
		{
			return true;
		}
		return false;
	}

	private void fillLayoutwithBaseFrame(int baseFrame)
	{
		int width = mLayoutSize.getWidth();
		int Height = mLayoutSize.getHeight();
		for (int yy = 0; yy < Height; ++yy)
		{
			for (int xx = 0; xx < width; ++xx)
			{
				mLayout[xx + yy * width] = (byte) baseFrame;
			}
		}
	}

	private void fillLayoutwithStitchingflag(List<Rect> faceRect)
	{
		int i = 0;
		int width = mLayoutSize.getWidth();
		int Height = mLayoutSize.getHeight();

		for (Rect rect : faceRect)
		{
			int frameIndex = mChosenFace[mBasebaseFrameIndex][i];
			Rect dst = mRectList.get(frameIndex).get(i);
			Rect newRect = null;
			if (isFarDistance(rect, dst))
			{
				newRect = new Rect((int) (dst.left / IMAGE_TO_LAYOUT), (int) (dst.top / IMAGE_TO_LAYOUT),
						(int) (dst.right / IMAGE_TO_LAYOUT), (int) (dst.bottom / IMAGE_TO_LAYOUT));
			} else
			{
				newRect = new Rect((int) (rect.left / IMAGE_TO_LAYOUT), (int) (rect.top / IMAGE_TO_LAYOUT),
						(int) (rect.right / IMAGE_TO_LAYOUT), (int) (rect.bottom / IMAGE_TO_LAYOUT));
			}

			float radius = getRadius(newRect);
			radius += radius;
			int left, top, right, bottom;

			left = newRect.centerX() - (int) radius;
			top = newRect.centerY() - (int) radius;
			right = newRect.centerX() + (int) radius;
			bottom = newRect.centerY() + (int) radius;

			if (left < 0)
				left = 0;
			if (right > width)
				right = width;
			if (top < 0)
				top = 0;
			if (bottom > Height)
				bottom = Height;

			for (int yy = top; yy < bottom; ++yy)
			{
				for (int xx = left; xx < right; ++xx)
				{
					int xy = xx + yy * width;
					if (checkDistance(radius, xx, yy, newRect.centerX(), newRect.centerY()))
					{
						mLayout[xy] = (byte) 0xFF;
					}
				}
			}
			i++;
		}
		return;
	}

	private void fillLayoutwithFaceindex(List<Rect> faceRect)
	{
		int i = 0;
		int width = mLayoutSize.getWidth();
		int Height = mLayoutSize.getHeight();
		for (Rect rect : faceRect)
		{
			int frameIndex = mChosenFace[mBasebaseFrameIndex][i];
			Rect dst = mRectList.get(frameIndex).get(i);
			Rect newRect = null;
			if (isFarDistance(rect, dst))
			{
				newRect = new Rect((int) (dst.left / IMAGE_TO_LAYOUT), (int) (dst.top / IMAGE_TO_LAYOUT),
						(int) (dst.right / IMAGE_TO_LAYOUT), (int) (dst.bottom / IMAGE_TO_LAYOUT));

			} else
			{
				newRect = new Rect((int) (rect.left / IMAGE_TO_LAYOUT), (int) (rect.top / IMAGE_TO_LAYOUT),
						(int) (rect.right / IMAGE_TO_LAYOUT), (int) (rect.bottom / IMAGE_TO_LAYOUT));
			}

			float radius = getRadius(newRect);
			int left, top, right, bottom;

			left = newRect.centerX() - (int) radius;
			top = newRect.centerY() - (int) radius;
			right = newRect.centerX() + (int) radius;
			bottom = newRect.centerY() + (int) radius;

			if (left < 0)
				left = 0;
			if (right > width)
				right = width;
			if (top < 0)
				top = 0;
			if (bottom > Height)
				bottom = Height;

			for (int yy = top; yy < bottom; ++yy)
			{
				for (int xx = left; xx < right; ++xx)
				{
					int xy = xx + yy * width;
					if (checkDistance(radius, xx, yy, newRect.centerX(), newRect.centerY()))
					{
						mLayout[xy] = (byte) frameIndex;
					}
				}
			}
			i++;
		}
		return;
	}

	private float getRadius(Rect rect)
	{
		return (rect.width() + rect.height()) / 2;
	}

	private int getSquareOfDistance(int x, int y, int x0, int y0)
	{
		return (x - x0) * (x - x0) + (y - y0) * (y - y0);
	}
}
