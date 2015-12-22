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

package com.almalence.plugins.processing.bestshot;

public final class AlmaShotBestShot
{
	public static synchronized native String Initialize();

	public static synchronized native int Release();

	public static synchronized native String ConvertFromJpeg(int[] frame, int[] frame_len, int nFrames, int sx, int sy);

	public static synchronized native String AddYUVFrames(int[] frame, int nFrames, int sx, int sy);

	public static synchronized native int BestShotProcess(int nFrames, int sx, int sy);

	static
	{
		System.loadLibrary("utils-image");
		System.loadLibrary("almalib");
		System.loadLibrary("bestshot");
	}
}
