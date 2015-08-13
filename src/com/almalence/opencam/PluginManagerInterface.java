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

/* <!-- +++
 package com.almalence.opencam_plus;
 +++ --> */
// <!-- -+-
package com.almalence.opencam;
//-+- -->

import android.hardware.camera2.CaptureResult;


//PluginManagerInterface used by Camera Controller to communicate with appluication's plugins
public interface PluginManagerInterface
{
	//Ask current capture plugin to select desire image size
	public void selectImageDimension();

	//Call to ask plugins to choose desire default camera parameters values
	public void selectDefaults();

	public boolean shouldPreviewToGPU();

	//Callback of auto focus
	public void onAutoFocus(boolean focused);
	
	public void onAutoFocusMoving(boolean start);

	//Callback for camera's preview frames
	public void onPreviewFrame(byte[] data);

	//Callback for captured still images
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format);
	
	//Callback for CaptureResult (used in camera2 mode)
	public void onCaptureCompleted(CaptureResult result);
	
	//Pass captured frame data to let plugins extract desired exif data
	public void collectExifData(byte[] frameData);
	
	//In case of multishot capturing pass to plugin id of current capture request
	public void addRequestID(int nFrame, int requestID);
	
	public boolean isPreviewDependentMode();
	
	//Check if current device is allowed to use camera2 interface
	//At that time we limit numbers of devices which may use camera2 interface
	public boolean isCamera2InterfaceAllowed();

}
