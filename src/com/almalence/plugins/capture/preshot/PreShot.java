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

package com.almalence.plugins.capture.preshot;

public final class PreShot
{
	public static native int AvailableMemory();

	public static native int AllocateBuffer(int imgW, int imgH, int fps, int secondsToAllocate, int isJPG);

	public static native int[] GetFromBufferRGBA(int idx, boolean manualOrientation, boolean orientation);

	public static native byte[] GetFromBufferToShowInSlow(int idx, int previewW, int previewH, boolean cameraMirrored);

	public static native int getOrientation(int idx);

	//public static native int getOrientationReserved(int idx);

	public static native int GetImageCount();

	public static native byte[] GetFromBufferNV21(int idx, int W, int H, int mirrored);

	public static native byte[] GetFromBufferSimpleNV21(int idx, int W, int H);
	
	// /reserved
//	public static native int MakeCopy();
//
//	public static native byte[] GetFromBufferReservedNV21(int idx, int W, int H, int mirrored);
//
//	public static native byte[] GetFromBufferSimpleReservedNV21(int idx, int W, int H);
//
//	public static native boolean FreeBufferReserved();

	public static native boolean FreeBuffer();

	public static native int InsertToBuffer(byte[] data, int isPortrait);

	static
	{
		System.loadLibrary("utils-image");
		System.loadLibrary("preshot");
	}
}
