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

package com.almalence.plugins.vf.histogram;

public final class Histogram
{
	public static synchronized native void createHistogram(byte[] ain, int[] afacts, int width, int height,
			int surfaceWidth, int surfaceHeight);

	public static synchronized native void createRGBHistogram(byte[] ain, int[] afactsR, int[] afactsG, int[] afactsB,
			int width, int height, int surfaceWidth, int surfaceHeight);

	static
	{
		System.loadLibrary("histogram");
	}
}
