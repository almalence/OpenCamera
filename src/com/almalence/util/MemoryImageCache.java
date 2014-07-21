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

package com.almalence.util;

import java.io.File;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.util.LruCache;

public class MemoryImageCache
{
	private LruCache<String, Bitmap>	lruCache;

	private final Object				mCacheLock	= new Object();

	public MemoryImageCache(int maxCount)
	{
		lruCache = new LruCache<String, Bitmap>(maxCount);
	}

	public void addBitmap(String key, Bitmap bitmap)
	{
		if (bitmap == null)
			return;

		if (lruCache != null)
		{
			if (getBitmap(key) == null)
			{
				lruCache.put(key, bitmap);
				return;
			}

			synchronized (mCacheLock)
			{
				lruCache.put(key, bitmap);
			}
		}
	}

	public void addBitmap(String key, File bitmapFile)
	{
		if (bitmapFile == null)
			return;
		if (!bitmapFile.exists())
			return;

		Bitmap bitmap = BitmapFactory.decodeFile(bitmapFile.getAbsolutePath());
		if (lruCache != null)
		{
			if (getBitmap(key) == null)
			{
				lruCache.put(key, bitmap);
				return;
			}

			synchronized (mCacheLock)
			{
				lruCache.put(key, bitmap);
			}
		}
	}

	public Bitmap getBitmap(String key)
	{
		synchronized (mCacheLock)
		{
			if (lruCache != null)
			{
				return lruCache.get(key);
			}
		}
		return null;
	}

	public void clear()
	{
		lruCache.evictAll();
	}
}