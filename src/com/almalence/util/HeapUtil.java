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

	static
	{
		System.loadLibrary("utils-image");
		System.loadLibrary("utils-jni");
	}
}