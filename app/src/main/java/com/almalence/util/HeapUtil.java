/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the &quot;License&quot;);
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an &quot;AS IS&quot; BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.almalence.util;

/**
 * Collection of utility functions used in this package.
 */
public class HeapUtil
{

	/**
	 * Return information about native heap memory:
	 * 
	 * [0] = megabytes used [1] = megabytes free
	 */
	public static native int[] getMemoryInfo();
	
	//Method based on similar method from AugmentedPanorama plugin.
	public static long getAmountOfMemoryToFitFrames()
	{
		// activityManager returning crap (way less than really available)
		final int[] mi = HeapUtil.getMemoryInfo();

		//Log.e(TAG, "Memory: used: " + mi[0] + "Mb  free: " + mi[1] + "Mb");

		return (long) ((mi[1] - 10.f) * 1000000.f * 0.6f); // use up to 60% and
															// ensure at least
															// 64Mb left free
	}

	public static int getRAWFrameSizeInBytes(int width, int height)
	{
		return (2 * width * height);
	}

	static
	{
		System.loadLibrary("utils-image");
		System.loadLibrary("utils-jni");
	}
}