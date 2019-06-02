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

package com.almalence.plugins.capture.video;

public final class Mp4Editor
{
//	public static synchronized native String append(String[] inputFiles, String newFile);
	public static synchronized native String appendFds(int[] inputFilesDescriptors, int newFileDescriptor);

	
	static
	{
		System.loadLibrary("almalence-mp4editor");
	}
}
