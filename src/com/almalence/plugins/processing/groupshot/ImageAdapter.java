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

import java.io.File;
import java.io.FilenameFilter;
import java.util.List;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Bitmap.Config;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import com.almalence.opencamunderground.R;
import com.almalence.opencamunderground.cameracontroller.CameraController;
import com.almalence.util.MemoryImageCache;
import com.almalence.util.Size;
/* <!-- +++
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.cameracontroller.CameraController;

 +++ --> */
// <!-- -+-
//-+- -->

public class ImageAdapter extends BaseAdapter
{
	static final int			THUMBNAIL_WIDTH		= 150;
	static final int			THUMBNAIL_HEIGHT	= 180;
	static final int			IMAGEVIEW_PADDING	= 4;
	int							mGalleryItemBackground;
	private Context				mContext			= null;
	private List<Integer>		mYUVList;
	private boolean				mCameraMirrored;
	private int					mImageDataOrientation;
	private MemoryImageCache	cache				= null;
	private int					mSelectedItem;

	public ImageAdapter(Context context, List<Integer> list, int imageDataOrientation, boolean isMirrored)
	{
		mContext = context;
		mYUVList = list;
		mCameraMirrored = isMirrored;
		mImageDataOrientation = imageDataOrientation;
		TypedArray a = context.obtainStyledAttributes(R.styleable.GalleryTheme);
		mGalleryItemBackground = a.getResourceId(R.styleable.GalleryTheme_android_galleryItemBackground, 0);
		a.recycle();

		cache = new MemoryImageCache(mYUVList.size());

		for (int i = 0; i < mYUVList.size(); i++)
		{
			final int id = i;
			new Thread(new Runnable()
			{
				@Override
				public void run()
				{
					final String Key = String.valueOf(id);
					cache.addBitmap(Key, decodeYUVfromData(id));
				}
			}).start();
		}
	}

	public void finalize()
	{
		try
		{
			super.finalize();
		} catch (Throwable e)
		{
			e.printStackTrace();
		}
		cache.clear();
	}

	private Bitmap decodeYUVfromData(int position)
	{
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		int width = imageSize.getWidth();
		int height = imageSize.getHeight();

		int scaledWidth = 0;
		int scaledHeight = 0;

		float imageRatio = (float) width / (float) height;
		float displayRatio = (float) THUMBNAIL_WIDTH / (float) THUMBNAIL_HEIGHT;

		if (imageRatio > displayRatio)
		{
			scaledWidth = THUMBNAIL_WIDTH;
			scaledHeight = (int) (THUMBNAIL_WIDTH / displayRatio);
		} else
		{
			scaledWidth = (int) (THUMBNAIL_HEIGHT * imageRatio);
			scaledHeight = THUMBNAIL_HEIGHT;
		}

		Rect rect = new Rect(0, 0, width, height);
		Bitmap bitmap = Bitmap.createBitmap(
				AlmaShotGroupShot.NV21toARGB(mYUVList.get(position), width, height, rect, scaledWidth, scaledHeight),
				scaledWidth, scaledHeight, Config.RGB_565);

		Matrix matrix = new Matrix();
		// Workaround for Nexus5x, image is flipped because of sensor
		// orientation
//		if (CameraController.isNexus5x)
//			matrix.postRotate(mCameraMirrored ? (mIsLandscape ? 90 : 270) : 270);
//		else
//			matrix.postRotate(mCameraMirrored ? (mIsLandscape ? 90 : -90) : 90);
		
		if (mImageDataOrientation != 0)
		{
			matrix.postRotate(mImageDataOrientation);
		}

		Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
		bitmap.recycle();
		bitmap = null;
		return rotatedBitmap;
	}

	public int getCount()
	{
		return mYUVList.size();
	}

	public void setCurrentSeleted(int position)
	{
		mSelectedItem = position;
	}

	public Object getItem(int position)
	{
		return position;
	}

	public long getItemId(int position)
	{
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent)
	{
		ImageView imageView;
		if (convertView == null)
		{ // if it's not recycled, initialize some attributes
			imageView = new ImageView(mContext);
			imageView.setLayoutParams(new Gallery.LayoutParams(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
			imageView.setScaleType(ImageView.ScaleType.FIT_XY);
			imageView.setPadding(0, 0, 0, 0);
		} else
		{
			imageView = (ImageView) convertView;
		}

		final String Key = String.valueOf(position);
		Bitmap b = cache.getBitmap(Key);

		if (b != null)
		{
			imageView.setImageBitmap(b);
		} else
		{
			imageView.setImageBitmap(decodeYUVfromData(position));
		}

		if (position == mSelectedItem)
		{
			imageView.setPadding(IMAGEVIEW_PADDING, IMAGEVIEW_PADDING, IMAGEVIEW_PADDING, IMAGEVIEW_PADDING);
			imageView.setBackgroundColor(0xFF00AAEA);
		} else
		{
			imageView.setPadding(IMAGEVIEW_PADDING, IMAGEVIEW_PADDING, IMAGEVIEW_PADDING, IMAGEVIEW_PADDING);
			imageView.setBackgroundColor(Color.WHITE);
		}
		return imageView;
	}
}
