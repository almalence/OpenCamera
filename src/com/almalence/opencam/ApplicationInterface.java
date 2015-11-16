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
//<!-- -+-
package com.almalence.opencam;
//-+- -->

import com.almalence.sony.cameraremote.SimpleStreamSurfaceView;

import android.annotation.TargetApi;
import android.app.Activity;
import android.media.ImageReader;
import android.media.MediaRecorder;
import android.view.Surface;

/*
 * Application interface used by CameraController and secondary classes as Camera2
 * to communicate with implementation of OpenCameraApplication class.
 * Commonly provides methods related to camera functionality
 */
public interface ApplicationInterface
{
	// message codes
	public static final int				MSG_NO_CAMERA							= 1;
	public static final int				MSG_TAKE_PICTURE						= 2;
	public static final int				MSG_CAPTURE_FINISHED					= 3;
	public static final int				MSG_PROCESSING_FINISHED					= 4;
	public static final int				MSG_START_POSTPROCESSING				= 5;
	public static final int				MSG_POSTPROCESSING_FINISHED				= 6;
	public static final int				MSG_FILTER_FINISHED						= 7;
	public static final int				MSG_EXPORT_FINISHED						= 8;
	public static final int				MSG_EXPORT_FINISHED_IOEXCEPTION			= 9;
	public static final int				MSG_START_FX							= 10;
	public static final int				MSG_FX_FINISHED							= 11;
	public static final int				MSG_DELAYED_CAPTURE						= 12;
	public static final int				MSG_FORCE_FINISH_CAPTURE				= 13;
	public static final int				MSG_NOTIFY_LIMIT_REACHED				= 14;
	public static final int				MSG_CAPTURE_FINISHED_NORESULT			= 15;

	public static final int				MSG_CAMERA_CONFIGURED					= 160;
	public static final int				MSG_CAMERA_STOPED						= 162;
	
	public static final int				MSG_APPLICATION_STOP					= 163;

	// For Camera2 code version
	public static final int				MSG_CAMERA_OPENED						= 16;
	public static final int				MSG_SURFACE_READY						= 17;
	public static final int				MSG_SURFACE_CONFIGURED					= 170;
	public static final int				MSG_NOT_LEVEL_FULL						= 18;
	public static final int				MSG_PROCESS								= 19;
	public static final int				MSG_PROCESS_FINISHED					= 20;
	public static final int				MSG_VOLUME_ZOOM							= 21;
	// ^^ For Camera2 code version

	public static final int				MSG_BAD_FRAME							= 24;
	public static final int				MSG_OUT_OF_MEMORY						= 25;

	public static final int				MSG_FOCUS_STATE_CHANGED					= 28;

	public static final int				MSG_RESTART_MAIN_SCREEN					= 30;

	public static final int				MSG_RETURN_CAPTURED						= 31;

	public static final int				MSG_RESULT_OK							= 40;
	public static final int				MSG_RESULT_UNSAVED						= 41;

	public static final int				MSG_CONTROL_LOCKED						= 50;
	public static final int				MSG_CONTROL_UNLOCKED					= 51;
	public static final int				MSG_PROCESSING_BLOCK_UI					= 52;
	public static final int				MSG_PREVIEW_CHANGED						= 53;

	public static final int				MSG_EV_CHANGED							= 60;
	public static final int				MSG_SCENE_CHANGED						= 61;
	public static final int				MSG_WB_CHANGED							= 62;
	public static final int				MSG_FOCUS_CHANGED						= 63;
	public static final int				MSG_FLASH_CHANGED						= 64;
	public static final int				MSG_ISO_CHANGED							= 65;
	public static final int				MSG_AEWB_CHANGED						= 66;
	public static final int				MSG_REMOTE_CAMERA_PARAMETR_CHANGED		= 67;
	public static final int				MSG_EXPOSURE_CHANGED					= 68;
	
	public static final int				MSG_FOCUS_LOCKED						= 663;
	public static final int				MSG_FOCUS_UNLOCKED						= 664;

	// OpenGL layer messages
	public static final int				MSG_OPENGL_LAYER_SHOW					= 70;
	public static final int				MSG_OPENGL_LAYER_HIDE					= 71;
	public static final int				MSG_OPENGL_LAYER_SHOW_V2				= 72;
	public static final int				MSG_OPENGL_LAYER_RENDERMODE_CONTINIOUS	= 73;
	public static final int				MSG_OPENGL_LAYER_RENDERMODE_WHEN_DIRTY	= 74;

	// events to pause/resume capture. for example to stop capturing in preshot
	// when popup share opened
	public static final int				MSG_STOP_CAPTURE						= 80;
	public static final int				MSG_START_CAPTURE						= 81;

	// broadcast will be resent to every active plugin
	public static final int				MSG_BROADCAST							= 9999;
	
	
	
	//Methods to be implemented	
	public void configureCamera(boolean createGUI);
	public void addSurfaceCallback();
	
	public Activity getMainActivity();
	
	//Method to force close application (some exception or unpredictable situation)
	public void stopApplication();
	
	//Used to re-initialize camera object if needed.
	public void relaunchCamera();
	
	//Inform Application that current capturing is failed.
	public void captureFailed();
	
	//ImageReaders is used in camera2 mode. Pass created image available listener
	@TargetApi(21)
	public void createImageReaders(ImageReader.OnImageAvailableListener imageAvailableListener);
	
	//Commonly camera2 applications uses 4 types of surfaces - JPEG, YUV, RAW and Preview (YUV)
	@TargetApi(19)
	public Surface getPreviewYUVImageSurface();
	
	@TargetApi(19)
	public Surface getYUVImageSurface();

	@TargetApi(19)
	public Surface getJPEGImageSurface();

	@TargetApi(19)
	public Surface getRAWImageSurface();
	
	public MediaRecorder getMediaRecorder();
	
	public SimpleStreamSurfaceView getSimpleStreamSurfaceView();
	
	//Surface of camera viewfinder
	public Surface getCameraSurface();
	
	//Method to initialize appropriate objects and variables with size of camera's preview
	public void setCameraPreviewSize(int width, int height);
	
	
	//Specific prefernce to check that onPreviewFrame is working
	public void setExpoPreviewPref(boolean previewMode);
	public boolean getExpoPreviewPref();	
	
	//Set/Get camera parameters preference
	public void setEVPref(int iEv);
	public int  getEVPref();
	
	public void setSceneModePref(int iSceneMode);
	public int  getSceneModePref();
	
	public void setWBModePref(int iWB);
	public int  getWBModePref();
	
	public void setColorTemperature(int iTemp);
	public int  getColorTemperature();
	
	public void setFocusModePref(int iFocusMode);
	public int  getFocusModePref(int defaultMode);
	
	public void setFlashModePref(int iFlashMode);
	public int  getFlashModePref(int defaultMode);
	
	public void setISOModePref(int iISOMode);
	public int  getISOModePref(int defaultMode);
	
	public int getAntibandingModePref();
	
	public int getColorEffectPref();
	
	public boolean getAELockPref();
	public boolean getAWBLockPref();
	
	//Some application modes are uses separate image sizes
	public void    setSpecialImageSizeIndexPref(int iIndex);
	public String  getSpecialImageSizeIndexPref();
	
	//Indicate on GUI capturing and play sound if needed
	public void showCaptureIndication(boolean playShutter);
	
	//Auto focus lock
	public void setAutoFocusLock(boolean locked);
	public boolean getAutoFocusLock();
}
