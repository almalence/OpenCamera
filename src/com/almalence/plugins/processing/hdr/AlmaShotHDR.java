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

package com.almalence.plugins.processing.hdr;

public final class AlmaShotHDR
{
	public static synchronized native String Initialize();

	public static synchronized native int Release();

	public static synchronized native String HDRConvertFromJpeg(int[] frame, int[] frame_len, int nFrames, int sx,
			int sy);

	public static synchronized native String HDRAddYUVFrames(int[] frame, int nFrames, int sx, int sy);

	public static synchronized native String HDRPreview(int nFrames, int sx, int sy, int[] pview, int expoPref,
			int colorPref, int ctrstPref, int microPref, int noSegmPref, int noisePref, boolean mirrored);

	public static synchronized native String HDRPreview2(int sx, int sy, int[] pview, boolean mirrored);

	public static synchronized native String HDRPreview2a(int sx, int sy, int[] pview, boolean rotate, int exposure,
			int vividness, int contrast, int microcontrast, boolean mirrored);

	public static synchronized native byte[] HDRProcess(int sx, int sy, int[] crop, int rotate, boolean mirrored);

	public static synchronized native void HDRStopProcessing();

	public static synchronized native void HDRFreeInstance();

	static
	{
		System.loadLibrary("utils-image");
		System.loadLibrary("almalib");
		System.loadLibrary("almashot-hdr");
	}
}
