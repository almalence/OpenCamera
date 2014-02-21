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
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.Matrix;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Gallery;
import android.widget.ImageView;


/* <!-- +++
import com.almalence.opencam_plus.R;
+++ --> */
// <!-- -+-
import com.almalence.opencam.R;
//-+- -->
import com.almalence.util.MemoryImageCache;

public class ImageAdapter extends BaseAdapter {
	final static int THUMBNAIL_WIDTH = 150;
	final static int THUMBNAIL_HEIGHT = 180;
	final static int IMAGEVIEW_PADDING = 4;
	int mGalleryItemBackground;
	private Context mContext = null;
	private String[] imagePath = null;
	private List<byte[]> mList;
	private boolean mCameraMirrored;
	private MemoryImageCache cache = null;
	private int mSelectedItem;

	public ImageAdapter(Context context, List<byte[]> list, boolean isLandscape, boolean isMirrored) {
		mContext = context;
		mList = list;
		mCameraMirrored = isMirrored;
		TypedArray a = context.obtainStyledAttributes(R.styleable.GalleryTheme);
		mGalleryItemBackground = a.getResourceId(
				R.styleable.GalleryTheme_android_galleryItemBackground, 0);
		a.recycle();
		
		cache = new MemoryImageCache(mList.size());
		
		for (int i = 0; i < mList.size(); i++) {
	    	final int id = i;
		    new Thread(new Runnable() {
		        @Override
		        public void run() {
		        	final String Key = String.valueOf(id);
		        	cache.addBitmap(Key, decodeJPEGfromData(id));
		        }
		    }).start();
		}
	}
	
	public ImageAdapter(Context context, String path) {
		mContext = context;
		TypedArray a = context.obtainStyledAttributes(R.styleable.GalleryTheme);
		mGalleryItemBackground = a.getResourceId(
				R.styleable.GalleryTheme_android_galleryItemBackground, 0);
		a.recycle();
		setDirContainThumbnails(path);
	}
	
	public void finalize() {
		cache.clear();
	}
	
	private Bitmap decodeJPEGfromData(int position) {
		BitmapFactory.Options options = new BitmapFactory.Options();
		options.inPreferredConfig = Config.RGB_565;
		options.inJustDecodeBounds = true;
		
		if (mList == null) {
			BitmapFactory.decodeFile(imagePath[position], options);
		} else {
			BitmapFactory.decodeByteArray(mList.get(position), 0, mList.get(position).length, options);
		}

		float widthScale = (float)options.outWidth / (float)THUMBNAIL_WIDTH;
		float heightScale = (float)options.outHeight / (float)THUMBNAIL_HEIGHT;
		float scale = widthScale > heightScale ? widthScale : heightScale;
		float imageRatio = (float)options.outWidth / (float)options.outHeight;
		float displayRatio = (float)THUMBNAIL_WIDTH / (float)THUMBNAIL_HEIGHT;

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
		
		Bitmap bm = null;
		Bitmap bitmap = null;
		
		if (mList == null) {
			bm = BitmapFactory.decodeFile(imagePath[position], options);		
		} else {
			bm = BitmapFactory.decodeByteArray(mList.get(position), 0, mList.get(position).length, options);
		}
		
		if (imageRatio > displayRatio) {
			bitmap = Bitmap.createScaledBitmap(bm, THUMBNAIL_WIDTH, (int)(THUMBNAIL_WIDTH / displayRatio), true);
		} else {
			bitmap = Bitmap.createScaledBitmap(bm, (int)(THUMBNAIL_HEIGHT * imageRatio), THUMBNAIL_HEIGHT, true);
		}

		if(bitmap != bm)
			bm.recycle();
		
		Matrix matrix = new Matrix();
		matrix.postRotate(mCameraMirrored? -90 : 90);
		return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
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
        	imagePath[numOfFrame] = f.getAbsolutePath();
        	final int id = numOfFrame;
		    new Thread(new Runnable() {
		        @Override
		        public void run() {
		        	final String Key = String.valueOf(id);
		        	cache.addBitmap(Key, decodeJPEGfromData(id));
		        }
		    }).start();
		    numOfFrame++;
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
	
	public void setCurrentSeleted(int position) {
		mSelectedItem = position;
	}

	public Object getItem(int position) {
		return position;
	}

	public long getItemId(int position) {
		return position;
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		ImageView imageView;
        if (convertView == null) {  // if it's not recycled, initialize some attributes
            imageView = new ImageView(mContext);
            imageView.setLayoutParams(new Gallery.LayoutParams(THUMBNAIL_WIDTH, THUMBNAIL_HEIGHT));
            imageView.setScaleType(ImageView.ScaleType.FIT_XY);
            imageView.setPadding(0, 0, 0, 0);
        } else {
            imageView = (ImageView) convertView;
        }
        
    	final String Key = String.valueOf(position);
    	Bitmap b = cache.getBitmap(Key);
    	
    	if (b != null) {
    		imageView.setImageBitmap(b);
    	} else {
    		imageView.setImageBitmap(decodeJPEGfromData(position));
    	}

    	if (position == mSelectedItem) {
    		imageView.setPadding(IMAGEVIEW_PADDING, IMAGEVIEW_PADDING, IMAGEVIEW_PADDING, IMAGEVIEW_PADDING);
    		imageView.setBackgroundColor(0xFF00AAEA);
        } else {
        	imageView.setPadding(IMAGEVIEW_PADDING, IMAGEVIEW_PADDING, IMAGEVIEW_PADDING, IMAGEVIEW_PADDING);
        	imageView.setBackgroundColor(Color.WHITE);
        }
		return imageView;
	}
}