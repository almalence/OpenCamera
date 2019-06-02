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

package com.almalence.plugins.capture.expobracketing;

import java.util.Date;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.hardware.camera2.CaptureResult;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;

/* <!-- +++
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.ui.GUI.CameraParameter;
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.ApplicationInterface;
 import com.almalence.opencam_plus.CameraParameters;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.cameracontroller.CameraController;
import com.almalence.opencam.ui.GUI.CameraParameter;
import com.almalence.opencam.R;

//-+- -->

/***
 * Implements capture plugin with exposure bracketing. Used for HDR image
 * processing
 ***/

public class ExpoBracketingCapturePlugin extends PluginCapture
{
	private static final int	MAX_HDR_FRAMES			= 4;

	private int					preferenceEVCompensationValue;

	// almashot - related
	public static int[]			evValues				= new int[MAX_HDR_FRAMES];
	public static int[]			evIdx					= new int[MAX_HDR_FRAMES];
	private int					frame_num;
	public static float			ev_step;
	private boolean				cm7_crap;

	// shared between activities
	public static int			CapIdx;
	public static int			total_frames;
	public static boolean		LumaAdaptationAvailable	= false;

	// preferences
	public static boolean		RefocusPreference;
	public static boolean		UseLumaAdaptation;
	private int					preferenceSceneMode;
	private int					preferenceFlashMode;

	// set exposure based on onpreviewframe
	private CountDownTimer		cdt						= null;

	private static String		sEvPref;
	private static String		sRefocusPref;
	private static String		sUseLumaPref;
	
	private boolean				camera2Preference;

	public ExpoBracketingCapturePlugin()
	{
		super("com.almalence.plugins.expobracketingcapture", R.xml.preferences_capture_expobracketing,
				R.xml.preferences_capture_expobracketing, 0, null);
	}

	private static String	EvPreference;

	@Override
	public void onCreate()
	{
		sEvPref = ApplicationScreen.getAppResources().getString(R.string.Preference_ExpoBracketingPref);
		sRefocusPref = ApplicationScreen.getAppResources().getString(R.string.Preference_ExpoBracketingRefocusPref);
		sUseLumaPref = ApplicationScreen.getAppResources().getString(R.string.Preference_ExpoBracketingUseLumaPref);
	}

	@Override
	public void onStart()
	{
		getPrefs();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		camera2Preference = prefs.getBoolean(ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), false);
		
//		if(CameraController.isFlex2 && camera2Preference)
//		{
//			prefs.edit().putBoolean(ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), false).commit();
//			CameraController.setUseCamera2(false);
//			
//			CameraController.isOldCameraOneModeLaunched = true;
//			PluginManager.getInstance().setSwitchModeType(true);
//		}
	}

	@Override
	public void onResume()
	{
		inCapture = false;
		aboutToTakePicture = false;
		
		isAllImagesTaken = false;
		isAllCaptureResultsCompleted = true;

		ApplicationScreen.instance.muteShutter(false);

		preferenceEVCompensationValue = ApplicationScreen.instance.getEVPref();
		preferenceSceneMode = ApplicationScreen.instance.getSceneModePref();
		preferenceFlashMode = ApplicationScreen.instance.getFlashModePref(ApplicationScreen.sDefaultFlashValue);
		
		if (CameraController.isUseCamera2() && CameraController.isNexus5or6)
			ApplicationScreen.instance.setFlashModePref(CameraParameters.FLASH_MODE_OFF);

		cdt = null;

		if (PluginManager.getInstance().getActiveModeID().equals("hdrmode"))
			ApplicationScreen.setCaptureFormat(CameraController.YUV);
		else if(captureRAW && CameraController.isRAWCaptureSupported())
			ApplicationScreen.setCaptureFormat(CameraController.RAW);
		else
		{
			captureRAW = false;
			ApplicationScreen.setCaptureFormat(CameraController.JPEG);
		}
	}

	@Override
	public void onPause()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		ApplicationScreen.instance.setEVPref(preferenceEVCompensationValue);
		ApplicationScreen.instance.setSceneModePref(preferenceSceneMode);

		if (CameraController.isUseCamera2() && CameraController.isNexus5or6)
		{
			prefs.edit().putInt(ApplicationScreen.sFlashModePref, preferenceFlashMode).commit();
			CameraController.setCameraFlashMode(preferenceFlashMode);
		}
		
		prefs.edit().putBoolean(ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), camera2Preference).commit();
	}
	
	@Override
	public void onStop()
	{
//		if(CameraController.isFlex2 && camera2Preference)
//		{
//			CameraController.useCamera2OnRelaunch(true);
//			CameraController.setUseCamera2(camera2Preference);
//		}
	}

	@Override
	public void onGUICreate()
	{
		ApplicationScreen.instance.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_EV, true, false, true);
		ApplicationScreen.instance.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_SCENE, true, true, true);

		if (CameraController.isUseCamera2() && CameraController.isNexus5or6)
			ApplicationScreen.instance.disableCameraParameter(CameraParameter.CAMERA_PARAMETER_FLASH, true, false, true);
	}

	public boolean delayedCaptureSupported()
	{
		return true;
	}

	@Override
	public void setupCameraParameters()
	{
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		int jpegQuality = Integer.parseInt(prefs.getString(ApplicationScreen.sJPEGQualityPref, "95"));

		CameraController.Size imageSize = CameraController.getCameraImageSize();
		CameraController.setPictureSize(imageSize.getWidth(), imageSize.getHeight());
		CameraController.setJpegQuality(jpegQuality);

		// CameraController.applyCameraParameters();

		try
		{
			int[] flashModes = CameraController.getSupportedFlashModes();
			if (flashModes != null && flashModes.length > 0 && CameraController.isUseCamera2() && CameraController.isNexus5or6)
			{
				SharedPreferences.Editor editor = prefs.edit();
				editor.putInt(ApplicationScreen.sFlashModePref, CameraParameters.FLASH_MODE_OFF);
				editor.commit();
				CameraController.setCameraFlashMode(CameraParameters.FLASH_MODE_OFF);
			}

			int[] sceneModes = CameraController.getSupportedSceneModes();
			if (sceneModes != null && CameraController.isModeAvailable(sceneModes, CameraParameters.SCENE_MODE_AUTO))
			{
				CameraController.setCameraSceneMode(CameraParameters.SCENE_MODE_AUTO);
				ApplicationScreen.instance.setSceneModePref(CameraParameters.SCENE_MODE_AUTO);
			}
		} catch (RuntimeException e)
		{
			Log.e("ExpoBracketing", "ApplicationScreen.setupCamera unable to setSceneMode");
		}

		CameraController.resetExposureCompensation();
		ApplicationScreen.instance.setEVPref(0);
	}

	public void onShutterClick()
	{
		if (!inCapture)
		{
			Date curDate = new Date();
			SessionID = curDate.getTime();

			cdt = null;
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

			if (CameraController.isAutoFocusPerform())
				aboutToTakePicture = true;
			else
				CaptureFrame();
		}
	}

	@Override
	public void addToSharedMemExifTags(byte[] frameData)
	{
		if (frameData != null)
		{
			if (PluginManager.getInstance().getActiveModeID().equals("hdrmode"))
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
		int n = evIdx[frame_num];
		if (cm7_crap && (total_frames == 3))
		{
			if (frame_num == 0)
				n = evIdx[0];
			else if (frame_num == 1)
				n = evIdx[2];
			else
				n = evIdx[1];
		}

		boolean isRAW = (format == CameraController.RAW);

		if (isRAW)
		{
			imagesTakenRAW++;
			PluginManager.getInstance().addToSharedMem("frame" + (imagesTakenRAW + 3) + SessionID,
					String.valueOf(frame));
			PluginManager.getInstance().addToSharedMem("framelen" + (imagesTakenRAW + 3) + SessionID,
					String.valueOf(frame_len));
			PluginManager.getInstance().addToSharedMem("frameorientation" + (imagesTakenRAW + 3) + SessionID,
					String.valueOf(ApplicationScreen.getGUIManager().getImageDataOrientation()));
			PluginManager.getInstance().addToSharedMem("framemirrored" + (imagesTakenRAW + 3) + SessionID,
					String.valueOf(CameraController.isFrontCamera()));

			PluginManager.getInstance().addToSharedMem("frameisraw" + (imagesTakenRAW + 3) + SessionID,
					String.valueOf(isRAW));

			PluginManager.getInstance().addToSharedMem("amountofcapturedrawframes" + SessionID,
					String.valueOf(imagesTakenRAW));
		} else
		{
			++frame_num;
			PluginManager.getInstance().addToSharedMem("frame" + (n + 1) + SessionID, String.valueOf(frame));
			PluginManager.getInstance().addToSharedMem("framelen" + (n + 1) + SessionID, String.valueOf(frame_len));
			PluginManager.getInstance().addToSharedMem("frameorientation" + (n + 1) + SessionID,
					String.valueOf(ApplicationScreen.getGUIManager().getImageDataOrientation()));
			PluginManager.getInstance().addToSharedMem("framemirrored" + (n + 1) + SessionID,
					String.valueOf(CameraController.isFrontCamera()));

			PluginManager.getInstance().addToSharedMem("frameisraw" + (n + 1) + SessionID, String.valueOf(isRAW));

			PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID, String.valueOf(n + 1));
		}

		if ((captureRAW && (frame_num + imagesTakenRAW) >= (total_frames * 2))
				|| (!captureRAW && frame_num >= total_frames))
		{
			if(isAllCaptureResultsCompleted)
			{
				PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
						String.valueOf(frame_num + imagesTakenRAW));
				if (cdt != null)
				{
					cdt.cancel();
					cdt = null;
				}
	
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
	
				CameraController.resetExposureCompensation();
	
				imagesTakenRAW = 0;
				frame_num = 0;
				resultCompleted = 0;
				inCapture = false;
				
				isAllImagesTaken = false;
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
			// Log.e("ExpoBkt", "frame_num = " + frame_num);
			// if (evIdx[frame_num] == 0)
			PluginManager.getInstance().addToSharedMemExifTagsFromCaptureResult(result, SessionID, resultCompleted);
		}

		if (captureRAW)
		{
			Log.e("ExpoBkt", "resultCompleted = " + resultCompleted);
			PluginManager.getInstance().addRAWCaptureResultToSharedMem("captureResult" + resultCompleted + SessionID,
					result);
		}
		
		if ((captureRAW && resultCompleted >= (total_frames * 2))
				|| (!captureRAW && resultCompleted >= total_frames))
		{
			isAllCaptureResultsCompleted = true;
			resultCompleted = 0;
			
			if(isAllImagesTaken)
			{
				PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
						String.valueOf(frame_num + imagesTakenRAW));
				
				if (cdt != null)
				{
					cdt.cancel();
					cdt = null;
				}
	
				PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, String.valueOf(SessionID));
	
				CameraController.resetExposureCompensation();
				
				imagesTakenRAW = 0;
				frame_num = 0;
				resultCompleted = 0;
				inCapture = false;
				
				isAllImagesTaken = false;
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

		RefocusPreference = prefs.getBoolean(sRefocusPref, false);
		UseLumaAdaptation = prefs.getBoolean(sUseLumaPref, false);

		EvPreference = prefs.getString(sEvPref, "0");

		captureRAW = prefs.getBoolean(ApplicationScreen.sCaptureRAWPref, false);
		if (PluginManager.getInstance().getActiveModeID().equals("hdrmode"))
			captureRAW = false;
	}

	@Override
	public void onCameraSetup()
	{
		// ----- Figure expo correction parameters
		FindExpoParameters();
	}

	void FindExpoParameters()
	{
		int ev_inc;
		int min_ev, max_ev;

		LumaAdaptationAvailable = CameraController.isLumaAdaptationSupported();

		if (UseLumaAdaptation && LumaAdaptationAvailable)
		{
			// set up fixed values for luma-adaptation (used on Qualcomm
			// chipsets)
			ev_step = 0.5f;
			total_frames = 3;
			evIdx[0] = 0;
			evIdx[1] = 1;
			evIdx[2] = 2;

			evValues[0] = 8;
			evValues[1] = 4;
			evValues[2] = 0;

			return;
		}

		// figure min and max ev
		min_ev = CameraController.getMinExposureCompensation();
		max_ev = CameraController.getMaxExposureCompensation();
		try
		{
			ev_step = CameraController.getExposureCompensationStep();
		} catch (NullPointerException e)
		{
			// miezu m9 fails to provide exposure correction step,
			// substituting with the most common step
			ev_step = 0.5f;
		}

		// cyanogenmod returns values that are absolutely ridiculous
		// change to a more sensible values, which at least return differing
		// exposures
		cm7_crap = false;
		if (ev_step > 1)
		{
			// debug log
			ev_step = 0.5f;
			cm7_crap = true;
		}

		// motorola droid2 crap (likely other motorola models too) - step is
		// clearly not what is reported
		// signature: <motorola> <DROID2>, ev_step = 0.3333
		if (android.os.Build.MANUFACTURER.toLowerCase().contains("motorola") && (Math.abs(ev_step - 0.333) < 0.01))
			ev_step = 1.5f;

		// xperia cameras seem to give slightly higher step than reported by
		// android
		if (android.os.Build.MANUFACTURER.toLowerCase().contains("sony") && (Math.abs(ev_step - 0.333) < 0.01))
			ev_step = 0.5f;

		// incorrect step in GT-S5830, may be other samsung models
		if (android.os.Build.MANUFACTURER.toLowerCase().contains("samsung") && (Math.abs(ev_step - 0.166) < 0.01))
			ev_step = 0.5f;

		switch (Integer.parseInt(EvPreference))
		{
		case 1: // -1 to +1 Ev compensation
			max_ev = (int) Math.floor(1 / ev_step);
			min_ev = -max_ev;
			break;
		case 2: // -2 to +2 Ev compensation
			max_ev = (int) Math.floor(2 / ev_step);
			min_ev = -max_ev;
			break;
		default:
			break;
		}
 
		// select proper min_ev, ev_inc
		if (ev_step == 0)
		{
			min_ev = 0;
			max_ev = 0;
			ev_inc = 0;
			total_frames = 3;

			for (int i = 0; i < total_frames; ++i)
				evValues[i] = 0;
		} else
		{
			ev_inc = (int) Math.floor(2 / ev_step);

			// we do not need overly wide dynamic range, limit to [-3Ev..+3Ev]
			// some models report range that they can not really handle
			if ((min_ev * ev_step < -3) && (max_ev * ev_step > 3)
					&& PluginManager.getInstance().getActiveModeID().equals("hdrmode"))
			{
				max_ev = (int) Math.floor(3 / ev_step);
				min_ev = -max_ev;
			}

			CameraController.Size imageSize = CameraController.getCameraImageSize();
			// if capturing more than 5mpix images - force no more than 3 frames
			int max_total_frames = MAX_HDR_FRAMES;
			if (imageSize.getWidth() * imageSize.getHeight() > 5200000)
				max_total_frames = 3;

			// motorola likes it a lot when the first shot is at 0Ev
			// (it will not change exposure on consequent shots otherwise)
			// Ev=0
			total_frames = 1;
			int min_range = 0;
			int max_range = 0;

			if ((ev_inc <= max_ev) && (total_frames < max_total_frames))
			{
				max_range = 1;
				++total_frames;
			}
			if ((-ev_inc >= min_ev) && (total_frames < max_total_frames))
			{
				min_range = -1;
				++total_frames;
			}
			if ((2 * ev_inc <= max_ev) && (total_frames < max_total_frames))
			{
				max_range = 2;
				++total_frames;
			}
			if ((-2 * ev_inc >= min_ev) && (total_frames < max_total_frames))
			{
				min_range = -2;
				++total_frames;
			}

			// if the range is too small for reported Ev step - just do two
			// frames - at min Ev and at max Ev
			if (max_range == min_range)
			{
				total_frames = 2;
				evValues[0] = max_ev;
				evValues[1] = min_ev;
			} else
			{
				evValues[0] = 0;

				int frame = 1;
				for (int i = max_range; i >= min_range; --i)
					if (i != 0)
					{
						evValues[frame] = i * ev_inc;
						++frame;
					}
			}
		}

		// sort frame idx'es in descending order of Ev's
		boolean[] skip_idx = new boolean[MAX_HDR_FRAMES];
		for (int i = 0; i < total_frames; ++i)
			skip_idx[i] = false;

		for (int i = 0; i < total_frames; ++i)
		{
			int ev_max = min_ev - 1;
			int max_idx = 0;
			for (int j = 0; j < total_frames; ++j)
				if ((evValues[j] > ev_max) && (!skip_idx[j]))
				{
					ev_max = evValues[j];
					max_idx = j;
				}

			evIdx[max_idx] = i;
			skip_idx[max_idx] = true;
		}
	}

	public void NotEnoughMemory()
	{
		// // warn user of low memory
	}

	public void CaptureFrame()
	{
		boolean isHDRMode = PluginManager.getInstance().getActiveModeID().equals("hdrmode");
		// requestID = CameraController.captureImagesWithParams(total_frames,
		// isHDRMode? CameraController.YUV : CameraController.JPEG, new int[0],
		// evValues, true);

		int gain[] = null;
		long exposure[] = null;
		
		// On Nexus5x and Nexus6p we can't change exposure compensation.
		// That's why we will use manual exposure (shutter speed) settings to capture HDR.
		// Adjust the shutter speed one or two stops faster (i.e. if you're at 1/250 sec, go to 1/500 or 1/1000 sec), take a photo, 
		// then adjust it one or two stops slower than your original shutter speed 
		// (i.e. if you were at 1/250 sec, then set it to 1/125 or 1/60 sec), and take another photo.
		// Also if we use manual exposure settings, we should set ISO manually (get ISO and Exposure original values from preview).
		if (CameraController.isNexus5x || 
			CameraController.isNexus6p || 
			CameraController.isFlex2 || 
			CameraController.isGalaxyS7 || 
			CameraController.isG5 ||
			CameraController.isHTCM10 ||
			CameraController.isMotoZ ||
			CameraController.isGalaxyS8 ||
			CameraController.isVivoXXX ||
			CameraController.isGalaxyNote8)
		{
			gain = new int[3];
			gain[0] = CameraController.getCurrentSensitivity();
			gain[1] = CameraController.getCurrentSensitivity();
			gain[2] = CameraController.getCurrentSensitivity();
			
			switch (Integer.parseInt(EvPreference))
			{
			case 1: // -1 to +1 Ev compensation
				exposure = new long[3];
				exposure[0] = CameraController.getCameraExposureTime();
				exposure[1] = CameraController.getCameraExposureTime() * 2;
				exposure[2] = CameraController.getCameraExposureTime() / 2;
				break;
			case 2: // -2 to +2 Ev compensation
				exposure = new long[3];
				exposure[0] = CameraController.getCameraExposureTime();
				exposure[1] = CameraController.getCameraExposureTime() * 4;
				exposure[2] = CameraController.getCameraExposureTime() / 4;
				break;
			default:
				exposure = new long[3];
				exposure[0] = CameraController.getCameraExposureTime();
				exposure[1] = CameraController.getCameraExposureTime() * 4;
				exposure[2] = CameraController.getCameraExposureTime() / 2;
				break;
			}
		}
		
		createRequestIDList(captureRAW? total_frames*2 : total_frames);
		if (captureRAW)
			CameraController.captureImagesWithParams(total_frames, CameraController.RAW, null, evValues, gain, exposure,
					false, true, true);
		else
			CameraController.captureImagesWithParams(total_frames, isHDRMode ? CameraController.YUV
					: CameraController.JPEG, null, evValues, gain, exposure, false, true, true);
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
