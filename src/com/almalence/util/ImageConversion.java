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

public class ImageConversion 
{
    public static native int JpegConvert(byte[] in, int sx, int sy, boolean rotate, boolean mirrored, int rotationDegree);
    public static native void sumByteArraysNV21(byte[] data1, byte[] data2, byte[] out, int width, int height);
    public static native void TransformNV21(byte[] InPic, byte[] OutPic, int sx, int sy, int flipLR, int flipUD, int rotate90);
    public static native void TransformNV21N(int InPic, int OutPic, int sx, int sy, int flipLR, int flipUD, int rotate90);
    public static native void convertNV21toGL(byte[] ain, byte[] aout, int width, int height, int outWidth, int outHeight);

    static 
    {
        System.loadLibrary("utils-image");
        System.loadLibrary("utils-jni");
    }
    
    public static void resizeJpeg2RGBA(final byte[] jpeg, final byte[] rgb_out, final int inWidth, final int inHeight, final int outWidth, int outHeight, boolean mirror)
    {
    	if (jpeg == null || rgb_out == null)
    	{
    		throw new IllegalArgumentException("Input and output buffers must not be null.");
    	}
    	
    	nativeresizeJpeg2RGBA(jpeg, rgb_out, inWidth, inHeight, outWidth, outHeight, mirror);
    }
    private static native void nativeresizeJpeg2RGBA(byte[] jpeg, byte[] rgb_out, int inHeight, int inWidth, int outWidth, int outHeight, boolean mirror);
    
    /**
     * Lets use this method to check pointers for NULL. Maybe move it to somewhere else?
     */
    private static int checkPtr(final int ptr)
    {
    	if (ptr == 0)
    	{
    		throw new OutOfMemoryError();
    	}
    	
    	return ptr;
    }
}
