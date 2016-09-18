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

package com.almalence.plugins.capture.focusbracketing;

import java.util.Date;
import java.util.List;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.hardware.camera2.CaptureResult;
import android.preference.PreferenceManager;
import android.util.Log;

/* <!-- +++
 import com.almalence.focuscam_plus.ApplicationScreen;
 import com.almalence.focuscam_plus.PluginCapture;
 import com.almalence.focuscam_plus.PluginManager;
 import com.almalence.focuscam_plus.R;
 import com.almalence.focuscam_plus.ui.GUI.CameraParameter;
 import com.almalence.focuscam_plus.cameracontroller.CameraController;
 import com.almalence.focuscam_plus.ApplicationInterface;
 import com.almalence.focuscam_plus.CameraParameters;
 +++ --> */
// <!-- -+-
import com.almalence.focuscam.ApplicationInterface;
import com.almalence.focuscam.ApplicationScreen;
import com.almalence.focuscam.CameraParameters;
import com.almalence.focuscam.PluginCapture;
import com.almalence.focuscam.PluginManager;
import com.almalence.focuscam.cameracontroller.CameraController;
import com.almalence.focuscam.ui.GUI.CameraParameter;
import com.almalence.focuscam.R;

//-+- -->

/***
 * Implements capture plugin with focus bracketing. 
 ***/

public class FocusBracketingCapturePlugin extends PluginCapture
{
//	private static final int	MAX_HDR_FRAMES			= 4;
//	private int					preferenceEVCompensationValue;

	// almashot - related
//	public static int[]			evValues				= new int[MAX_HDR_FRAMES];
//	public static int[]			evIdx					= new int[MAX_HDR_FRAMES];
	private int					frame_num;
//	public static float			ev_step;
//	private boolean				cm7_crap;

	// shared between activities
	public static int			CapIdx;
	public static int			total_frames;
//	public static boolean		LumaAdaptationAvailable	= false;

	// preferences
//	public static boolean		RefocusPreference;
//	public static boolean		UseLumaAdaptation;
//	private int					preferenceSceneMode;
	private int					preferenceFocusMode;
	private float				preferenceFocusDistanceValue;

	private static String		sModePref;
	
	boolean						aeLocked		= false;
	boolean						awLocked		= false;
	
//	private int[]				pauseBetweenShots		= { 1000, 1000, 1000, 1000, 1000, 1000, 1000, 1000 };

	public FocusBracketingCapturePlugin()
	{
		super("com.almalence.plugins.focusbracketingcapture", R.xml.preferences_capture_focusbracketing,
				R.xml.preferences_capture_focusbracketing, 0, null);
	}

	private static String	ModePreference;
	
	private float[]			focusDistances;

	@Override
	public void onCreate()
	{
		sModePref = ApplicationScreen.getAppResources().getString(R.string.Preference_FocusBracketingPref);
	}

	@Override
	public void onStart()
	{
		getPrefs();
	}

	@Override
	public void onResume()
	{
		inCapture = false;
		aboutToTakePicture = false;
		
		isAllImagesTaken = false;
		isAllCaptureResultsCompleted = true;
		
		aeLocked = false;
		awLocked = false;
		
		AeUnlock();
		AwUnlock();

		ApplicationScreen.instance.muteShutter(false);

		preferenceFocusMode = ApplicationScreen.instance.getFocusModePref(ApplicationScreen.sDefaultFocusValue);
		
		ApplicationScreen.setCaptureFormat(CameraController.YUV);
	}

	@Override
	public void onPause()
	{
		CameraController.setCameraFocusMode(preferenceFocusMode);
	}
	
	
	@Override
	public void onStop()
	{
	}

	@Override
	public void onGUICreate()
	{
//		ApplicationScreen.instance.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FOCUS, true, false, true);
	}

	public boolean delayedCaptureSupported()
	{
		return true;
	}

	@Override
	public void setupCameraParameters()
	{
		CameraController.Size imageSize = CameraController.getCameraImageSize();
		CameraController.setPictureSize(imageSize.getWidth(), imageSize.getHeight());

		//TODO: set Focus mode to MANUAL
//		try
//		{
//			int[] focusModes = CameraController.getSupportedFocusModes();
//			if (focusModes != null && focusModes.length > 0 && CameraController.isManualFocusDistanceSupported())
//			{
//				ApplicationScreen.instance.setFocusModePref(CameraParameters.MF_MODE);
//				ApplicationScreen.getPluginManager().sendMessage(ApplicationInterface.MSG_BROADCAST,
//																 ApplicationInterface.MSG_FOCUS_LOCKED);
//			}
//
//		} catch (RuntimeException e)
//		{
//			Log.e("FocusBracketing", "ApplicationScreen.setupCamera unable to set manual focus distance");
//		}

//		CameraController.resetExposureCompensation();
//		ApplicationScreen.instance.setEVPref(0);
	}
	
//	@Override
//	public void selectImageDimension()
//	{
//		//max size will be used for best performance of focus stacking API
//		int captureIndex = 0;
//		
//		int imgCaptureWidth = CameraController.ResolutionsSizeList.get(captureIndex).getWidth();
//		int imgCaptureHeight = CameraController.ResolutionsSizeList.get(captureIndex).getHeight();
//		
//		CameraController.setCameraImageSize(new CameraController.Size(imgCaptureWidth, imgCaptureHeight));
//
//	}
	
	@Override
	public void setCameraPreviewSize()
	{
		List<CameraController.Size> cs = CameraController.getSupportedPreviewSizes();

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		CameraController.Size os = getOptimalPreviewSize(cs, imageSize.getWidth(), imageSize.getHeight());
		ApplicationScreen.instance.setCameraPreviewSize(os.getWidth(), os.getHeight());
	}


	public void onShutterClick()
	{
		if (!inCapture)
		{
			Date curDate = new Date();
			SessionID = curDate.getTime();

			startCaptureSequence();
		}
	}


	private void startCaptureSequence()
	{
		ApplicationScreen.instance.muteShutter(true);

		if (!inCapture)
		{
			inCapture = true;

			// reiniting for every shutter press
			frame_num = 0;
			resultCompleted = 0;

//			if (CameraController.isAutoFocusPerform())
//				aboutToTakePicture = true;
//			else
			
			if(CameraController.getFocusMode() == CameraParameters.MF_MODE)
				preferenceFocusDistanceValue = CameraController.getCameraFocusDistance();
			
			CaptureFrame();
		}
	}
	

	public void CaptureFrame()
	{
		Log.e("FBCapture", "start focus bracketing capture");
//		float hyperFocalDistance = CameraController.getHyperfocalFocusDistance();
//		CameraController.setCameraFocusDistance(hyperFocalDistance);
		int[] pauseBetweenShots = null;
		switch (Integer.parseInt(ModePreference))
		{
		case 0: // Standard (3 shots: 2m, 66sm, 40sm)
			total_frames = 3;
			focusDistances = new float[3];
			focusDistances[0] = 1.0f/2.0f;
			focusDistances[1] = 1.0f/0.66f;
			focusDistances[2] = 1.0f/0.40f;
			break;
		case 1: // Macro (5 shots: 22sm, 18sm, 15sm, 13sm, 11sm)
			total_frames = 5;
			focusDistances = new float[5];
			focusDistances[0] = 1.0f/0.22f;//hyperFocalDistance;
			focusDistances[1] = 1.0f/0.18f;
			focusDistances[2] = 1.0f/0.15f;
			focusDistances[3] = 1.0f/0.13f;
			focusDistances[4] = 1.0f/0.11f;
//			pauseBetweenShots		= new int[]{ 1000, 0, 0, 0, 0};
			break;
		case 2: // Full-range (8 shots: 2m, 66sm, 40sm, 22sm, 18sm, 15sm, 13sm, 11sm)
			total_frames = 8;
			focusDistances = new float[8];
			focusDistances[0] = 1.0f/2.0f;//hyperFocalDistance;
			focusDistances[1] = 1.0f/0.66f;
			focusDistances[2] = 1.0f/0.40f;
			focusDistances[3] = 1.0f/0.22f;
			focusDistances[4] = 1.0f/0.18f;
			focusDistances[5] = 1.0f/0.15f;
			focusDistances[6] = 1.0f/0.13f;
			focusDistances[7] = 1.0f/0.11f;
//			pauseBetweenShots		= new int[]{ 1000, 0, 0, 0, 0, 0, 0, 0 };
			break;
		default: //Use standard mode
			focusDistances = new float[3];
			focusDistances[0] = 1.0f/2.0f;//hyperFocalDistance;
			focusDistances[1] = 1.0f/0.66f;
			focusDistances[2] = 1.0f/0.40f;
			break;
		}
		
		AeLock();
		AwLock();
		
		createRequestIDList(total_frames);
		CameraController.captureImagesWithParams(total_frames, CameraController.YUV, pauseBetweenShots, null, null, null, focusDistances, false, true, true);
	}
	
	private void AeLock()
	{
		if (CameraController.isExposureLockSupported())
		{
			CameraController.setAutoExposureLock(true);
	
			aeLocked = true;
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			prefs.edit().putBoolean(ApplicationScreen.sAELockPref, aeLocked).commit();
		}
	}

	private void AwLock()
	{
		if (CameraController.isWhiteBalanceLockSupported())
		{
			CameraController.setAutoWhiteBalanceLock(true);
	
			awLocked = true;
			
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
			prefs.edit().putBoolean(ApplicationScreen.sAWBLockPref, awLocked).commit();
		}
	}

	private void AeUnlock()
	{
		if (CameraController.isExposureLockSupported())
			CameraController.setAutoExposureLock(false);
		
		aeLocked = false;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		prefs.edit().putBoolean(ApplicationScreen.sAELockPref, aeLocked).commit();
	}
	

	private void AwUnlock()
	{
		if (CameraController.isWhiteBalanceLockSupported())
			CameraController.setAutoWhiteBalanceLock(false);
	
		awLocked = false;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		prefs.edit().putBoolean(ApplicationScreen.sAWBLockPref, awLocked).commit();
	}
	

	public void onAutoFocus(boolean paramBoolean)
	{
		if (inCapture) // disregard autofocus success (paramBoolean)
		{
			// Log.d("HDR", "onAutoFocus inCapture == true");
			// on motorola xt5 cm7 this function is called twice!
			// on motorola droid's onAutoFocus seem to be called at every
			// startPreview,
			// causing additional frame(s) taken after sequence is finished
			if (aboutToTakePicture)
				CaptureFrame();

			aboutToTakePicture = false;
		}
	}

	@Override
	public void addToSharedMemExifTags(byte[] frameData)
	{
		if (frameData != null)
		{
			if (PluginManager.getInstance().getActiveModeID().equals("focusstackingmode"))
			{
				PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(frameData, SessionID, -1);
			} else
			{
				PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(frameData, SessionID, frame_num + 1);
			}
		} else if (frame_num == 0)
		{
			PluginManager.getInstance().addToSharedMemExifTagsFromCamera(SessionID);
		}
	}

	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format)
	{
//		int n = evIdx[frame_num];
//		if (cm7_crap && (total_frames == 3))
//		{
//			if (frame_num == 0)
//				n = evIdx[0];
//			else if (frame_num == 1)
//				n = evIdx[2];
//			else
//				n = evIdx[1];
//		}
		
		float focusDistance = focusDistances[frame_num];

		++frame_num;
		
		Log.e("FBCapture", "------- frame " + frame_num + " taken");
		PluginManager.getInstance().addToSharedMem("frame" + frame_num + SessionID, String.valueOf(frame));
		PluginManager.getInstance().addToSharedMem("framelen" + frame_num + SessionID, String.valueOf(frame_len));
		PluginManager.getInstance().addToSharedMem("frameorientation" + frame_num + SessionID,
				String.valueOf(ApplicationScreen.getGUIManager().getImageDataOrientation()));
		PluginManager.getInstance().addToSharedMem("framemirrored" + frame_num + SessionID,
				String.valueOf(CameraController.isFrontCamera()));
//		PluginManager.getInstance().addToSharedMem("focusdistance" + frame_num + SessionID, String.valueOf(focusDistance));

		PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID, String.valueOf(frame_num));
		

		if (frame_num >= total_frames)
		{
			if(isAllCaptureResultsCompleted)
			{
				PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
						String.valueOf(frame_num));
	
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
	
//				CameraController.resetExposureCompensation();
	
				frame_num = 0;
				resultCompleted = 0;
				inCapture = false;
				
				isAllImagesTaken = false;
				
				AeUnlock();
				AwUnlock();
				
				if(CameraController.getFocusMode() == CameraParameters.MF_MODE)
					CameraController.setCameraFocusDistance(preferenceFocusDistanceValue);
			}
			else
				isAllImagesTaken = true;
		}
	}

	@TargetApi(21)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		isAllCaptureResultsCompleted = false;

		int requestID = requestIDArray[resultCompleted];
		resultCompleted++;
		if (result.getSequenceId() == requestID)
		{
			PluginManager.getInstance().addToSharedMemExifTagsFromCaptureResult(result, SessionID, resultCompleted);
		}

		float focusDistance = focusDistances[resultCompleted-1];
//		float resFocusDistance = result.get(CaptureResult.LENS_FOCUS_DISTANCE);
//		Log.e("FocusBracketingCapture", "Init focus distance = " + focusDistance + " result focus distance = " + resFocusDistance + " frame num = " + resultCompleted);
		PluginManager.getInstance().addToSharedMem("focusdistance" + resultCompleted + SessionID, String.valueOf(focusDistance));
		
		if (resultCompleted >= total_frames)
		{
			isAllCaptureResultsCompleted = true;
			resultCompleted = 0;
			
			if(isAllImagesTaken)
			{
				PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
						String.valueOf(frame_num + imagesTakenRAW));
				
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
	
				CameraController.resetExposureCompensation();
				
				frame_num = 0;
				resultCompleted = 0;
				inCapture = false;
				
				isAllImagesTaken = false;
				
				AeUnlock();
				AwUnlock();
				
				if(CameraController.getFocusMode() == CameraParameters.MF_MODE)
					CameraController.setCameraFocusDistance(preferenceFocusDistanceValue);
			}
		}
	}

	@Override
	public void onExportFinished()
	{

	}

	private void getPrefs()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

		ModePreference = prefs.getString(sModePref, "0");
	}

	@Override
	public void onCameraSetup()
	{
		// ----- Figure expo correction parameters
		FindFocusDistanceParameters();
	}

	void FindFocusDistanceParameters()
	{
		//TODO: According to capture mode (standard, macro, full-range) populate focus distances for capturing.
	}

	// onPreviewFrame is used only to provide an exact delay between setExposure
	// and takePicture
	@Override
	public void onPreviewFrame(byte[] data)
	{
	}

	public boolean photoTimeLapseCaptureSupported()
	{
		return true;
	}
}
