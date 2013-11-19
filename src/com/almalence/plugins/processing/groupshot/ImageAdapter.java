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
import android.graphics.Matrix;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;

import com.almalence.opencam.R;

public class ImageAdapter extends BaseAdapter {
	int mGalleryItemBackground;
	private Context mContext = null;
	private String[] imagePath = null;
	private List<byte[]> mList;
	private boolean mDisplayLandscape;
	private boolean mCameraMirrored;

	public ImageAdapter(Context context, List<byte[]> list, boolean isLandscape, boolean isMirrored) {
		mContext = context;
		mList = list;
		mDisplayLandscape = isLandscape;
		mCameraMirrored = isMirrored;
		TypedArray a = context.obtainStyledAttributes(R.styleable.GalleryTheme);
		mGalleryItemBackground = a.getResourceId(
				R.styleable.GalleryTheme_android_galleryItemBackground, 0);
		a.recycle();
	}
	
	public ImageAdapter(Context context, String path) {
		mContext = context;
		TypedArray a = context.obtainStyledAttributes(R.styleable.GalleryTheme);
		mGalleryItemBackground = a.getResourceId(
				R.styleable.GalleryTheme_android_galleryItemBackground, 0);
		a.recycle();
		setDirContainThumbnails(path);
	}
	
	private int setDirContainThumbnails(String path) {
		int numOfFrame = 0;
        File file = new File(path);
        File[] list = file.listFiles(new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name) 
            {
                return name.endsWith(".jpg");
            }
        });
        
        if (list.length == numOfFrame ) {
        	return numOfFrame;
        }
        imagePath = new String[list.length];
        for (File f : list) {
        	imagePath[numOfFrame++] = f.getAbsolutePath();
        }
        
		return numOfFrame;
	}

	public int getCount() {
		if (imagePath != null) {
			return imagePath.length;
		} else {
			return mList.size();
		}
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView i = new ImageView(mContext);
		Display display = ((WindowManager) mContext
				.getSystemService(Context.WINDOW_SERVICE)).getDefaultDisplay();
		int displayWidth = display.getWidth();
		int displayHeight = display.getHeight();
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Config.RGB_565;
		options.inJustDecodeBounds = true;
		
		if (mList == null) {
			BitmapFactory.decodeFile(imagePath[position], options);
		} else {
			BitmapFactory.decodeByteArray(mList.get(position), 0, mList.get(position).length, options);
		}

		float widthScale = options.outWidth / displayWidth;
		float heightScale = options.outHeight / displayHeight;
		float scale = widthScale > heightScale ? widthScale : heightScale;

		if (scale >= 8) {
			options.inSampleSize = 8;
		} else if (scale >= 6) {
			options.inSampleSize = 6;
		} else if (scale >= 4) {
			options.inSampleSize = 4;
		} else if (scale >= 2) {
			options.inSampleSize = 2;
		} else {
			options.inSampleSize = 1;
		}

		options.inJustDecodeBounds = false;
		if (mList == null) {
			Bitmap bm = BitmapFactory.decodeFile(imagePath[position], options);
//			if(!mDisplayLandscape)
//    		{
//	    		Matrix matrix = new Matrix();
//	    		matrix.postRotate(mCameraMirrored? -90 : 90);
//	    		bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
//    		}
			Matrix matrix = new Matrix();
    		matrix.postRotate(mCameraMirrored? -90 : 90);
    		bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);			
			i.setImageBitmap(bm);
		} else {
			Bitmap bm = BitmapFactory.decodeByteArray(mList.get(position), 0, mList.get(position).length, options);
//			if(!mDisplayLandscape)
//    		{
//	    		Matrix matrix = new Matrix();
//	    		matrix.postRotate(mCameraMirrored? -90 : 90);
//	    		bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
//    		}
			Matrix matrix = new Matrix();
    		matrix.postRotate(mCameraMirrored? -90 : 90);
    		bm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
			i.setImageBitmap(bm);
		}
		i.setLayoutParams(new Gallery.LayoutParams(200, 250));
//		if(mDisplayLandscape)
//			i.setLayoutParams(new Gallery.LayoutParams(250, 200));
//		else
//			i.setLayoutParams(new Gallery.LayoutParams(200, 250));
		i.setScaleType(ImageView.ScaleType.FIT_XY);
		i.setBackgroundResource(mGalleryItemBackground);
		return i;
	}
}
