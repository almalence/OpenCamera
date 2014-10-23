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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.Canvas;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Point;
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

public class AlmaCLRShot
{
	private final String			TAG				= this.getClass().getName()
															.substring(this.getClass().getName().lastIndexOf(".") + 1);

	private int						IMAGE_TO_LAYOUT	= 8;
	private static final int		MAX_INPUT_FRAME	= 8;

	private List<byte[]>			mJpegData;
	private int						mBaseFrameIndex;
	private Size					mPreviewSize;
	private Size					mInputFrameSize;
	private Size					mLayoutSize;
	private int						mNumOfFrame;
	private int[]					ARGBBuffer		= null;
	private int[]					mBaseArea		= null;
	private int[]					mCrop			= null;
	private int						mSensitivity;	// sensitivity:
													// default
													// is
													// 0,
													// useful
													// range
													// is
													// [-15..+15]
	private int						mMinSize;		// minSize:
													// the
													// smallest
													// object
													// size
													// which
													// will
													// be
													// detected
													// (object
													// area,
													// in
													// pixels).
	private int						mGhosting;
	private int						mAngle;

	private int						mOutNV21		= 0;
	private ObjectInfo[]			mObjInfo		= null;
	private ObjBorderInfo[]			mObjBorderInfo	= null;
	private Rect[]					mBoarderRect	= null;
	private int						mTotalObj		= 0;

	private byte[]					mAutoLayout;
	private byte[]					mManualLayout;
	private byte[]					mEnumObj;

	private static final Object		syncObject		= new Object();

	private OnProcessingListener	mOnProcessingListener;

	private AlmaCLRShot()
	{
		super();
	}

	private static final AlmaCLRShot	mInstance	= new AlmaCLRShot();

	public static AlmaCLRShot getInstance()
	{
		return mInstance;
	}

	public class ObjectInfo
	{
		public ObjectInfo()
		{
			super();
		}

		Rect	objectRect	= null;
		Bitmap	thumbnail	= null;

		public Rect getRect()
		{
			return objectRect;
		}

		public Bitmap getThumbnail()
		{
			return thumbnail;
		}
	}

	public class ObjBorderInfo
	{
		public ObjBorderInfo()
		{
			super();
		}

		Rect	mRect	= null;
		Bitmap	mThumb	= null;

		public Rect getRect()
		{
			return mRect;
		}

		public Bitmap getThumbnail()
		{
			return mThumb;
		}
	}

	public interface OnProcessingListener
	{
		// Call back when each object created.
		void onObjectCreated(ObjectInfo objInfo);

		// Call back when processing complete.
		void onProcessingComplete(ObjectInfo[] objInfoList);
	}

	public void addInputFrame(List<Integer> inputFrame, Size size) throws Exception
	{
		mNumOfFrame = inputFrame.size();
		mInputFrameSize = size;

		if (mNumOfFrame > MAX_INPUT_FRAME)
		{
			Log.d(TAG, "Number of Input Frame = " + mNumOfFrame);
			throw new Exception("Too Many Input Frame");
		}

		if (!mInputFrameSize.isValid())
		{
			Log.d(TAG, "Input frame size is wrong ");
			throw new Exception("Too Many Input Frame");
		}

		long pixels = mInputFrameSize.getWidth() * mInputFrameSize.getHeight();

		if (pixels >= 7680000)
		{
			IMAGE_TO_LAYOUT = 16;
		} else
		{
			IMAGE_TO_LAYOUT = 8;
		}

		Initialize();

		synchronized (syncObject)
		{
			int[] PointOfData = new int[mNumOfFrame];
			int[] LengthOfData = new int[mNumOfFrame];

			int data_lenght = mInputFrameSize.getWidth() * mInputFrameSize.getHeight() + 2
					* ((mInputFrameSize.getWidth() + 1) / 2) * ((mInputFrameSize.getHeight() + 1) / 2);
			for (int i = 0; i < mNumOfFrame; i++)
			{
				PointOfData[i] = inputFrame.get(i);
				LengthOfData[i] = data_lenght;
				if (PointOfData[i] == 0)
				{
					Log.d(TAG, "Out of Memory in Native");
					throw new Exception("Out of Memory in Native");
				}
			}

			int error = -1;
			error = AddYUVInputFrame(PointOfData, LengthOfData, mNumOfFrame, size.getWidth(), size.getHeight());
			if (error < 0)
			{
				Log.d(TAG, "Out Of Memory");
				throw new Exception("Out Of Memory");
			} else if (error < mNumOfFrame)
			{
				Log.d(TAG, "JPEG buffer is wrong in " + error + " frame");
				throw new Exception("Out Of Memory");
			}
		}
		return;
	}

	public boolean initialize(Size previewSize, int angle, int baseFrame, int sensitivity, int minSize, int ghosting,
			OnProcessingListener listener) throws Exception
	{
//		Log.d(TAG, "initialize() -- start");
		mGhosting = ghosting;
		mPreviewSize = previewSize;
		mBaseFrameIndex = baseFrame;
		mSensitivity = sensitivity;
		mMinSize = minSize;
		mOnProcessingListener = listener;
		mAngle = angle;
		mLayoutSize = new Size(mInputFrameSize.getWidth() / IMAGE_TO_LAYOUT, mInputFrameSize.getHeight()
				/ IMAGE_TO_LAYOUT);

		if (mAngle != 0 && mAngle != 90 && mAngle != 180 && mAngle != 270)
		{
			Log.d(TAG, "Angle is invalid");
			throw new Exception("Angle is invalid");
		}

		if (!mPreviewSize.isValid())
		{
			Log.d(TAG, "Preview size is wrong");
			throw new Exception("Too Many Input Frame");
		}

		if (mSensitivity < -15 || mSensitivity > 15)
		{
			Log.d(TAG, "Sensitivity value is wrong");
			throw new Exception("Sensitivity value is wrong");
		}

		if (mMinSize < 0 || mMinSize > mInputFrameSize.getWidth() * mInputFrameSize.getHeight())
		{
			Log.d(TAG, "MinSize value is wrong");
			throw new Exception("Sensitivity value is wrong");
		}

		int length = mLayoutSize.getWidth() * mLayoutSize.getHeight();

		mAutoLayout = new byte[length];
		mManualLayout = new byte[length];
		mEnumObj = new byte[length];
		mCrop = new int[5];
		mBaseArea = new int[4];
		mCrop[4] = mBaseFrameIndex;

		Arrays.fill(mAutoLayout, (byte) mBaseFrameIndex);

		mAutoLayout[0] = -1;

		removeProcessing(mAutoLayout);
		updateLayout();

//		Log.d(TAG, "initialize() -- end");
		return true;
	}

	private Rect rotateObjRect(Rect rect)
	{
		if (mAngle == 0)
		{
			return rect;
		}
		Rect newRect = null;
		switch (mAngle)
		{
		case 90:
			newRect = new Rect((mInputFrameSize.getHeight() - rect.bottom), rect.left,
					(mInputFrameSize.getHeight() - rect.top), rect.right);
			break;
		case 180:
			newRect = new Rect(mInputFrameSize.getWidth() - rect.right, mInputFrameSize.getHeight() - rect.bottom,
					mInputFrameSize.getWidth() - rect.left, mInputFrameSize.getHeight() - rect.top);
			break;
		case 270:
			newRect = new Rect(rect.top, mInputFrameSize.getWidth() - rect.left, rect.bottom,
					mInputFrameSize.getWidth() - rect.left);
			break;
		default:
			break;
		}
		return newRect;
	}

	private Rect rotateRect(Rect rect)
	{
		if (mAngle == 0)
		{
			return rect;
		}
		Rect newRect = null;
		switch (mAngle)
		{
		case 90:
			newRect = new Rect((mPreviewSize.getHeight() - rect.bottom), rect.left,
					(mPreviewSize.getHeight() - rect.top), rect.right);
			break;
		case 180:
			newRect = new Rect(mPreviewSize.getWidth() - rect.right, mPreviewSize.getHeight() - rect.bottom,
					mPreviewSize.getWidth() - rect.left, mPreviewSize.getHeight() - rect.top);
			break;
		case 270:
			newRect = new Rect(rect.top, mPreviewSize.getWidth() - rect.left, rect.bottom, mPreviewSize.getWidth()
					- rect.left);
			break;
		default:
			break;
		}
		return newRect;
	}

	public synchronized ObjectInfo[] getObjectInfoList()
	{
//		Log.d(TAG, "getObjectInfoList() -- start");
		long start = System.currentTimeMillis();

		if (mTotalObj == 0)
		{
			return new ObjectInfo[0];
		}

		if (mObjInfo != null)
		{
			return mObjInfo;
		}
		mObjInfo = new ObjectInfo[mTotalObj];

		if (mBoarderRect == null)
		{
			mBoarderRect = scanLayoutforRect();
		}

		float ratio = (float) mPreviewSize.getWidth() / ((float) mInputFrameSize.getWidth() / IMAGE_TO_LAYOUT);

		for (int i = 0; i < mTotalObj; i++)
		{
//			Log.d(TAG, "mObjInfo" + "[" + i + "]");
			Rect orgRect = new Rect(mBoarderRect[i].left * IMAGE_TO_LAYOUT, mBoarderRect[i].top * IMAGE_TO_LAYOUT,
					mBoarderRect[i].right * IMAGE_TO_LAYOUT, mBoarderRect[i].bottom * IMAGE_TO_LAYOUT);
			Rect PreviewRect = new Rect(Math.round(mBoarderRect[i].left * ratio), Math.round(mBoarderRect[i].top
					* ratio), Math.round(mBoarderRect[i].right * ratio), Math.round(mBoarderRect[i].bottom * ratio));
			mObjInfo[i] = new ObjectInfo();
			mObjInfo[i].thumbnail = getObjectBitmap(i, orgRect);
			mObjInfo[i].objectRect = rotateRect(PreviewRect);
			if (mOnProcessingListener != null)
			{
				mOnProcessingListener.onObjectCreated(mObjInfo[i]);
			}
		}

		if (mOnProcessingListener != null)
		{
			mOnProcessingListener.onProcessingComplete(mObjInfo);
		}

//		Log.d(TAG, "getObjectInfoList() elapsed time = " + (System.currentTimeMillis() - start));
//		Log.d(TAG, "getObjectInfoList() -- end");
		return mObjInfo;
	}

	private Bitmap rotateBitmap(Bitmap b, int w, int h, float angle)
	{
		if (b == null)
		{
			return b;
		}
		if (angle == 0)
		{
			return b;
		}
		Matrix matrix = new Matrix();
		matrix.preRotate(angle);
		Bitmap rotImage = Bitmap.createBitmap(b, 0, 0, w, h, matrix, true);
		b.recycle();
		b = null;
		return rotImage;
	}

	public Bitmap getPreviewBitmap()
	{
//		Log.d(TAG, "getPreviewBitmap() -- start");

		Bitmap bitmap = Bitmap.createBitmap(mPreviewSize.getWidth(), mPreviewSize.getHeight(), Config.ARGB_8888);

		Rect rect = new Rect(0, 0, mInputFrameSize.getWidth(), mInputFrameSize.getHeight());
		ARGBBuffer = NV21toARGB(mOutNV21, mInputFrameSize, rect, mPreviewSize);
		bitmap.setPixels(ARGBBuffer, 0, mPreviewSize.getWidth(), 0, 0, mPreviewSize.getWidth(),
				mPreviewSize.getHeight());
		ARGBBuffer = null;

//		Log.d(TAG, "getPreviewBitmap() -- end");
		return rotateBitmap(bitmap, bitmap.getWidth(), bitmap.getHeight(), mAngle);
	}

	public int getTotalObjNum()
	{
		return mTotalObj;
	}

	private int getLayoutPos(float xx, float yy)
	{
		float xw = mPreviewSize.getWidth();
		float xh = mPreviewSize.getHeight();
		float w = mLayoutSize.getWidth();
		float h = mLayoutSize.getHeight();
		int x, y;
		x = (int) (xx * w / xw);
		y = (int) (yy * h / xh);
		return (x + y * (int) w);
	}

	private int getLayoutXPos(float xx)
	{
		float xw = mPreviewSize.getWidth();
		float w = mLayoutSize.getWidth();
		return (int) (xx * w / xw);
	}

	private int getLayoutYPos(float yy)
	{
		float xh = mPreviewSize.getHeight();
		float h = mLayoutSize.getHeight();
		return (int) (yy * h / xh);
	}

	private Rect[] scanLayoutforRect()
	{
		int width = mLayoutSize.getWidth();
		int height = mLayoutSize.getHeight();

		Rect[] rect = new Rect[mTotalObj];

		for (int i = 0; i < mTotalObj; i++)
		{
			rect[i] = new Rect(-1, -1, -1, -1);
		}

		for (int y = 0; y < height; y++)
		{
			for (int x = 0; x < width; x++)
			{
				int obj = mEnumObj[x + y * width];
				if (obj > 0)
				{
					if (rect[obj - 1].left > x || rect[obj - 1].left == -1)
					{
						rect[obj - 1].left = x;
					}
					if (rect[obj - 1].right < x || rect[obj - 1].right == -1)
					{
						rect[obj - 1].right = x;
					}

					if (rect[obj - 1].top > y || rect[obj - 1].top == -1)
					{
						rect[obj - 1].top = y;
					}

					if (rect[obj - 1].bottom < y || rect[obj - 1].bottom == -1)
					{
						rect[obj - 1].bottom = y;
					}
				}
			}
		}
		return rect;
	}

	private static final int	UP_DIRECTION	= 0;
	private static final int	RIGHT_DIRECTION	= 1;
	private static final int	DOWN_DIRECTION	= 2;
	private static final int	LEFT_DIRECTION	= 3;

	private Bitmap getObjBorderSource(int index, Paint paint, Rect rect)
	{
		int i = 0;
		int x = 0;
		int y = 0;
		int w = rect.width();
		int h = rect.height();
		int x_starting = -1;
		int y_starting = -1;
		int last_direction = RIGHT_DIRECTION;
		byte[] tmpBuffer = new byte[rect.width() * rect.height()];
		Bitmap bitmap = Bitmap.createBitmap(rect.width(), rect.height(), Config.ARGB_8888);

		for (int yy = rect.bottom - 1; yy >= rect.top; --yy)
		{
			for (int xx = rect.right - 1; xx >= rect.left; --xx)
			{
				int pos = getLayoutPos(xx, yy);
				int new_pos = (xx - rect.left) + rect.width() * (yy - rect.top);

				if (mBaseFrameIndex == mAutoLayout[pos])
				{
					continue;
				}

				tmpBuffer[new_pos] = mEnumObj[pos];

				if (tmpBuffer[new_pos] == index)
				{
					x = xx - rect.left;
					y = yy - rect.top;
				}
			}
		}

		// list of list (removed objects) of points
		List<Point> ListArray = new ArrayList<Point>();

		x_starting = x;
		y_starting = y;

		ListArray.add(new Point(x, y));

		do
		{
			switch (last_direction)
			{
			case LEFT_DIRECTION:
				if (x < w - 1 && y < h - 1)
				{
					if (tmpBuffer[(x + 1) + w * (y + 1)] == index)
					{
						x += 1;
						y += 1;
						last_direction = DOWN_DIRECTION;
						break;
					}
				}
				if (y < h - 1)
				{
					if (tmpBuffer[x + w * (y + 1)] == index)
					{
						y += 1;
						last_direction = LEFT_DIRECTION;
						break;
					}
				}
				if (y < h - 1 && x >= 1)
				{
					if (tmpBuffer[(x - 1) + w * (y + 1)] == index)
					{
						x -= 1;
						y += 1;
						last_direction = LEFT_DIRECTION;
						break;
					}
				}
				if (x >= 1)
				{
					if (tmpBuffer[(x - 1) + w * y] == index)
					{
						last_direction = UP_DIRECTION;
						x -= 1;
						break;
					}
				}
				if (x >= 1 && y >= 1)
				{
					if (tmpBuffer[(x - 1) + w * (y - 1)] == index)
					{
						x -= 1;
						y -= 1;
						last_direction = UP_DIRECTION;
						break;
					}
				}
				if (y >= 1)
				{
					if (tmpBuffer[x + w * (y - 1)] == index)
					{
						y -= 1;
						last_direction = RIGHT_DIRECTION;
						break;
					}
				}
				if (x < w - 1 && y >= 1)
				{
					if (tmpBuffer[(x + 1) + w * (y - 1)] == index)
					{
						x += 1;
						y -= 1;
						last_direction = RIGHT_DIRECTION;
						break;
					}
				}
				break;
			case UP_DIRECTION:
				if (y < h - 1 && x >= 1)
				{
					if (tmpBuffer[(x - 1) + w * (y + 1)] == index)
					{
						x -= 1;
						y += 1;
						last_direction = LEFT_DIRECTION;
						break;
					}
				}
				if (x >= 1)
				{
					if (tmpBuffer[(x - 1) + w * y] == index)
					{
						last_direction = UP_DIRECTION;
						x -= 1;
						break;
					}
				}
				if (x >= 1 && y >= 1)
				{
					if (tmpBuffer[(x - 1) + w * (y - 1)] == index)
					{
						x -= 1;
						y -= 1;
						last_direction = UP_DIRECTION;
						break;
					}
				}
				if (y >= 1)
				{
					if (tmpBuffer[x + w * (y - 1)] == index)
					{
						y -= 1;
						last_direction = RIGHT_DIRECTION;
						break;
					}
				}
				if (x < w - 1 && y >= 1)
				{
					if (tmpBuffer[(x + 1) + w * (y - 1)] == index)
					{
						x += 1;
						y -= 1;
						last_direction = RIGHT_DIRECTION;
						break;
					}
				}
				if (x < w - 1)
				{
					if (tmpBuffer[x + 1 + w * y] == index)
					{
						x += 1;
						last_direction = DOWN_DIRECTION;
						break;
					}
				}
				if (x < w - 1 && y < h - 1)
				{
					if (tmpBuffer[(x + 1) + w * (y + 1)] == index)
					{
						x += 1;
						y += 1;
						last_direction = DOWN_DIRECTION;
						break;
					}
				}
				break;
			case RIGHT_DIRECTION:
				if (x >= 1 && y >= 1)
				{
					if (tmpBuffer[(x - 1) + w * (y - 1)] == index)
					{
						x -= 1;
						y -= 1;
						last_direction = UP_DIRECTION;
						break;
					}
				}
				if (y >= 1)
				{
					if (tmpBuffer[x + w * (y - 1)] == index)
					{
						y -= 1;
						last_direction = RIGHT_DIRECTION;
						break;
					}
				}
				if (x < w - 1 && y >= 1)
				{
					if (tmpBuffer[(x + 1) + w * (y - 1)] == index)
					{
						x += 1;
						y -= 1;
						last_direction = RIGHT_DIRECTION;
						break;
					}
				}
				if (x < w - 1)
				{
					if (tmpBuffer[x + 1 + w * y] == index)
					{
						x += 1;
						last_direction = DOWN_DIRECTION;
						break;
					}
				}
				if (x < w - 1 && y < h - 1)
				{
					if (tmpBuffer[(x + 1) + w * (y + 1)] == index)
					{
						x += 1;
						y += 1;
						last_direction = DOWN_DIRECTION;
						break;
					}
				}
				if (y < h - 1)
				{
					if (tmpBuffer[x + w * (y + 1)] == index)
					{
						y += 1;
						last_direction = LEFT_DIRECTION;
						break;
					}
				}
				if (y < h - 1 && x >= 1)
				{
					if (tmpBuffer[(x - 1) + w * (y + 1)] == index)
					{
						x -= 1;
						y += 1;
						last_direction = LEFT_DIRECTION;
						break;
					}
				}
				break;
			case DOWN_DIRECTION:
				if (x < w - 1 && y >= 1)
				{
					if (tmpBuffer[(x + 1) + w * (y - 1)] == index)
					{
						x += 1;
						y -= 1;
						last_direction = RIGHT_DIRECTION;
						break;
					}
				}
				if (x < w - 1)
				{
					if (tmpBuffer[x + 1 + w * y] == index)
					{
						x += 1;
						last_direction = DOWN_DIRECTION;
						break;
					}
				}
				if (x < w - 1 && y < h - 1)
				{
					if (tmpBuffer[(x + 1) + w * (y + 1)] == index)
					{
						x += 1;
						y += 1;
						last_direction = DOWN_DIRECTION;
						break;
					}
				}
				if (y < h - 1)
				{
					if (tmpBuffer[x + w * (y + 1)] == index)
					{
						y += 1;
						last_direction = LEFT_DIRECTION;
						break;
					}
				}

				if (y < h - 1 && x >= 1)
				{
					if (tmpBuffer[(x - 1) + w * (y + 1)] == index)
					{
						x -= 1;
						y += 1;
						last_direction = LEFT_DIRECTION;
						break;
					}
				}
				if (x >= 1)
				{
					if (tmpBuffer[x - 1 + w * y] == index)
					{
						x -= 1;
						last_direction = UP_DIRECTION;
						break;
					}
				}
				if (x >= 1 && y >= 1)
				{
					if (tmpBuffer[(x - 1) + w * (y - 1)] == index)
					{
						x -= 1;
						y -= 1;
						last_direction = UP_DIRECTION;
						break;
					}
				}
				break;
			default:
				break;
			}

			if (++i % 20 == 19)
			{
				ListArray.add(new Point(x, y));
			}
		} while (x != x_starting || y != y_starting);

		Path path = new Path();
		path.setFillType(Path.FillType.EVEN_ODD);
		path.reset();

		// set first point
		Point p = ListArray.get(0);
		ListArray.remove(0);
		path.moveTo(p.x, p.y);
		for (Point point : ListArray)
		{
			path.lineTo(point.x, point.y);
		}

		path.lineTo(x_starting, y_starting);

		Canvas canvas = new Canvas(bitmap);
		paint.setStyle(Paint.Style.STROKE);
		canvas.drawPath(path, paint);

		paint.setColor(paint.getColor() & 0x4CFFFFFF);
		paint.setStyle(Paint.Style.FILL);
		canvas.drawPath(path, paint);

		paint.setColor(paint.getColor() | 0xFF000000);

		return bitmap;
	}

	public synchronized ObjBorderInfo[] getObjBorderBitmap(Paint paint)
	{
//		Log.d(TAG, "getObjBoundaryBitmap() -- start");

		if (mObjBorderInfo != null)
		{
			return mObjBorderInfo;
		}

		mObjBorderInfo = new ObjBorderInfo[mTotalObj];
		if (mBoarderRect == null)
		{
			mBoarderRect = scanLayoutforRect();
		}

		float ratio = (float) mPreviewSize.getWidth() / ((float) mInputFrameSize.getWidth() / IMAGE_TO_LAYOUT);

		for (int i = 0; i < mTotalObj; i++)
		{
//			Log.d(TAG, "mObjBorderInfo" + "[" + i + "]");
			if (mObjInfo[i].thumbnail == null)
			{
				continue;
			}
			Rect PreviewRect = new Rect(Math.round(mBoarderRect[i].left * ratio - 5), Math.round(mBoarderRect[i].top
					* ratio - 5), Math.round(mBoarderRect[i].right * ratio + 5), Math.round(mBoarderRect[i].bottom
					* ratio + 5));

			Rect LayoutRect = new Rect(Math.round(mBoarderRect[i].left * ratio - 5), Math.round(mBoarderRect[i].top
					* ratio - 5), Math.round(mBoarderRect[i].right * ratio + 5), Math.round(mBoarderRect[i].bottom
					* ratio + 5));

			PreviewRect.setIntersect(PreviewRect, new Rect(0, 0, mPreviewSize.getWidth() - 1,
					mPreviewSize.getHeight() - 1));
			LayoutRect.setIntersect(LayoutRect, new Rect(0, 0, mPreviewSize.getWidth() - 1,
					mPreviewSize.getHeight() - 1));

			mObjBorderInfo[i] = new ObjBorderInfo();
			mObjBorderInfo[i].mRect = rotateRect(PreviewRect);
			if (mObjInfo[i].thumbnail == null)
			{
				mObjBorderInfo[i].mThumb = null;
				continue;
			}
			mObjBorderInfo[i].mThumb = rotateBitmap(getObjBorderSource(i + 1, paint, LayoutRect), LayoutRect.width(),
					LayoutRect.height(), mAngle);
		}
		return mObjBorderInfo;
	}

	public void setObjectList(boolean[] objectIndex) throws Exception
	{
//		Log.d(TAG, "setObjectList() -- start");
		long start = System.currentTimeMillis();

		if (objectIndex.length > mTotalObj)
		{
			Log.d(TAG, "object index is greater than total number of object");
			throw new Exception("object index is greater than total number of object");
		}

		int width = mLayoutSize.getWidth();
		int height = mLayoutSize.getHeight();
		int obj = 0;
		for (boolean isRemoved : objectIndex)
		{
			for (int yy = 0; yy < height; ++yy)
			{
				for (int xx = 0; xx < width; ++xx)
				{
					int xy = xx + yy * width;
					if (mEnumObj[xy] == (obj + 1))
					{
						if (!isRemoved)
						{
							mManualLayout[xy] = (byte) mBaseFrameIndex;
						} else
						{
							mManualLayout[xy] = mAutoLayout[xy];
						}
					}
				}
			}
			obj++;
		}

		removeProcessing(mManualLayout);

//		Log.d(TAG, "setObjectList() elapsed time = " + (System.currentTimeMillis() - start));
//		Log.d(TAG, "setObjectList() -- end");
		return;
	}

	public void setObject(int objectIndex, boolean removed) throws Exception
	{
//		Log.d(TAG, "setObject() -- start");
		long start = System.currentTimeMillis();

		if (objectIndex > mTotalObj)
		{
			Log.d(TAG, "object index is greater than total number of object");
			throw new Exception("object index is greater than total number of object");
		}

		int width = mLayoutSize.getWidth();
		int height = mLayoutSize.getHeight();

		for (int yy = 0; yy < height; ++yy)
		{
			for (int xx = 0; xx < width; ++xx)
			{
				int xy = xx + yy * width;
				if (mEnumObj[xy] == objectIndex)
				{
					if (!removed)
					{
						mManualLayout[xy] = (byte) mBaseFrameIndex;
					} else
					{
						mManualLayout[xy] = mAutoLayout[xy];
					}
				}
			}
		}

		removeProcessing(mManualLayout);

//		Log.d(TAG, "setObject() elapsed time = " + (System.currentTimeMillis() - start));
//		Log.d(TAG, "setObject() -- end");
		return;
	}

	public void reverseObject(float lx, float ly)
	{
		int layoutW = mLayoutSize.getWidth();
		int layoutH = mLayoutSize.getHeight();

		byte obj = mEnumObj[getLayoutPos(lx, ly)];

		if (obj == 0)
		{
			return;
		}

		for (int yy = 0; yy < layoutH; ++yy)
		{
			for (int xx = 0; xx < layoutW; ++xx)
			{
				int xy = xx + yy * layoutW;

				if (mEnumObj[xy] == obj)
				{
					if (mManualLayout[xy] == mAutoLayout[xy])
					{
						mManualLayout[xy] = (byte) mBaseFrameIndex;
					} else
					{
						mManualLayout[xy] = mAutoLayout[xy];
					}
				}
			}
		}
		removeProcessing(mManualLayout);
		return;
	}

	public int getOccupiedObject(float lx, float ly) throws Exception
	{
		float x = 0;
		float y = 0;

		switch (mAngle)
		{
		case 0:
			x = lx;
			y = ly;
			break;
		case 90:
			x = ly;
			y = mPreviewSize.getHeight() - lx;
			break;
		case 180:
			x = mPreviewSize.getWidth() - lx;
			y = mPreviewSize.getHeight() - ly;
			break;
		case 270:
			x = mPreviewSize.getHeight() - ly;
			y = lx;
			break;
		default:
			break;
		}

		if (x > mPreviewSize.getWidth() || y > mPreviewSize.getHeight())
		{
			Log.d(TAG, "Coordiation is invalid x = " + x + " y = " + y);
			throw new Exception("Invalid touch position");
		}

		return (int) mEnumObj[getLayoutPos(x, y)];
	}

	public boolean isRemoved(int x, int y) throws Exception
	{
		int previewW = mPreviewSize.getWidth();
		int previewH = mPreviewSize.getHeight();

		if (x >= 0 && x <= previewW && y >= 0 && y <= previewH)
		{
			Log.d(TAG, "Invalid touch position");
			throw new Exception("Invalid touch position");
		}

		int pos = getLayoutPos(x, y);

		return mManualLayout[pos] != mAutoLayout[pos];
	}

	public void setBaseFrame(int base) throws Exception
	{

		if (base >= mNumOfFrame)
		{
			Log.d(TAG, "Invalid Base Frame");
			throw new Exception("Invalid Base Frame");
		}

		mCrop[4] = mBaseFrameIndex = base;

		for (int i = 0; i < mAutoLayout.length; ++i)
		{
			mAutoLayout[i] = (byte) mBaseFrameIndex;
		}

		mAutoLayout[0] = -1;

		removeProcessing(mAutoLayout);
		updateLayout();

		mObjInfo = null;
		mObjBorderInfo = null;
		mBoarderRect = null;
		return;
	}

	public boolean isRemoved(int ObjIndex)
	{
		int width = mLayoutSize.getWidth();
		int height = mLayoutSize.getHeight();

		for (int yy = 0; yy < height; ++yy)
		{
			for (int xx = 0; xx < width; ++xx)
			{
				int xy = xx + yy * width;
				if (mEnumObj[xy] == ObjIndex)
				{
					return mManualLayout[xy] != mAutoLayout[xy];
				}
			}
		}
		return true;
	}

	public byte[] processingSaveData()
	{
		byte[] jpegBuffer = null;

		android.graphics.YuvImage out = new android.graphics.YuvImage(SwapHeap.SwapFromHeap(mOutNV21,
				mInputFrameSize.getWidth() * mInputFrameSize.getHeight() * 3 / 2), ImageFormat.NV21,
				mInputFrameSize.getWidth(), mInputFrameSize.getHeight(), null);
		mOutNV21 = 0;
		try
		{
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

	public void release()
	{
//		Log.d(TAG, "release() start");
		synchronized (syncObject)
		{
			Release(mNumOfFrame);

			mPreviewSize = null;
			mInputFrameSize = null;
			mLayoutSize = null;

			ARGBBuffer = null;
			mCrop = null;

			mObjInfo = null;
			mObjBorderInfo = null;
			mBoarderRect = null;

			mAutoLayout = null;
			mManualLayout = null;
			mEnumObj = null;

			IMAGE_TO_LAYOUT = 8;

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
				Log.d(TAG, "Instance is not finalized correctly");
				e.printStackTrace();
			}
		}
//		Log.d(TAG, "release() end");
		return;
	}

	private synchronized void removeProcessing(byte[] layout)
	{
		if (mOutNV21 != 0)
		{
			SwapHeap.FreeFromHeap(mOutNV21);
			mOutNV21 = 0;
		}

		long start = System.currentTimeMillis();
		mOutNV21 = MovObjProcess(mNumOfFrame, mInputFrameSize, mSensitivity, mMinSize, mBaseArea, mCrop, layout,
				mGhosting, IMAGE_TO_LAYOUT);
//		Log.d(TAG, "MovObjProcess() elapsed time = " + (System.currentTimeMillis() - start));
//		Log.d(TAG, "mCrop[0] = " + mCrop[0] + " mCrop[1] = " + mCrop[1] + " mCrop[2] = " + mCrop[2] + " mCrop[3] = "
//				+ mCrop[3]);

		mBaseFrameIndex = mCrop[4];
		return;
	}

	private synchronized void updateLayout()
	{
		System.arraycopy(mAutoLayout, 0, mManualLayout, 0, mAutoLayout.length);
		mTotalObj = MovObjEnumerate(mNumOfFrame, mLayoutSize, mManualLayout, mEnumObj, mBaseFrameIndex);
		MovObjFixHoles(mLayoutSize, mEnumObj, 0);
		MovObjFixHoles(mLayoutSize, mAutoLayout, mBaseFrameIndex);
		return;
	}

	private Bitmap getObjectBitmap(int objectIndex, Rect rect)
	{
		float ratio = (float) mInputFrameSize.getWidth() / (float) mPreviewSize.getWidth();
		int[] buffer = null;
		if ((rect.width() / ratio) < 1 || (rect.height() / ratio) < 1 || rect.left < 0 || rect.right < 0
				|| rect.top < 0 || rect.bottom < 0)
		{
			return null;
		}

		// adjust object rectangle to match covered area in base frame
		Rect rect_adj = new Rect(rect.left * mBaseArea[2] / mInputFrameSize.getWidth() + mBaseArea[0], rect.top
				* mBaseArea[3] / mInputFrameSize.getHeight() + mBaseArea[1], rect.left * mBaseArea[2]
				/ mInputFrameSize.getWidth() + mBaseArea[0] + rect.width() * mBaseArea[2] / mInputFrameSize.getWidth(),
				rect.top * mBaseArea[3] / mInputFrameSize.getHeight() + mBaseArea[1] + rect.height() * mBaseArea[3]
						/ mInputFrameSize.getHeight());

		Size size = new Size((int) ((float) rect.width() / (float) ratio),
				(int) ((float) rect.height() / (float) ratio));
		buffer = NV21toARGB(getInputFrame(mBaseFrameIndex), mInputFrameSize, rect_adj, size);

		for (int yy = 0; yy < size.getHeight(); ++yy)
		{
			for (int xx = 0; xx < size.getWidth(); ++xx)
			{
				if (xx + (rect.left / ratio) > mPreviewSize.getWidth())
				{
					Log.d(TAG, "x = " + xx);
					Log.d(TAG, "width out of range");
				}

				if (yy + (rect.top / ratio) > mPreviewSize.getHeight())
				{
					Log.d(TAG, "y = " + yy);
					Log.d(TAG, "height out of range");
				}
				int pos = getLayoutPos(xx + (rect.left / ratio), yy + (rect.top / ratio));
				int obj = mEnumObj[pos];
				int xy = xx + yy * size.getWidth();

				if (obj == objectIndex + 1)
				{

					int r = (buffer[xy] >> 16) & 0xFF;
					int g = (buffer[xy] >> 8) & 0xFF;
					int b = (buffer[xy] >> 0) & 0xFF;

					buffer[xy] = (255 << 24) | (r << 16) | (g << 8) | b;
				} else
				{
					buffer[xy] = (0x00 << 24) | (0x00 << 16) | (0x00 << 8) | 0x00;
				}
			}
		}

		Bitmap bitmap = Bitmap.createBitmap(size.getWidth(), size.getHeight(), Config.ARGB_8888);
		bitmap.setPixels(buffer, 0, size.getWidth(), 0, 0, size.getWidth(), size.getHeight());
		buffer = null;
		return rotateBitmap(bitmap, size.getWidth(), size.getHeight(), mAngle);
	}

	private static native String Initialize();

	private static native int Release(int nFrames);

	private static native int ConvertFromJpeg(int[] frame, int[] frame_len, int nFrames, int sx, int sy);

	private static native int AddYUVInputFrame(int[] frame, int[] frame_len, int nFrames, int sx, int sy);

	private static native int[] NV21toARGB(int inptr, Size src, Rect rect, Size dst);

	private static native int getInputFrame(int index);

	private static native int MovObjProcess(int nFrames, Size size, int sensitivity, int minSize, int[] base_area,
			int[] crop, byte[] layout, int ghosting, int ratio);

	private static native int MovObjEnumerate(int nFrames, Size size, byte[] layout, byte[] enumObjects, int baseFrame);

	private static native int MovObjFixHoles(Size size, byte[] enumObjects, int baseFrame);

	static
	{
		System.loadLibrary("utils-image");
		System.loadLibrary("almalib");
		System.loadLibrary("almashot-clr");
	}
}
