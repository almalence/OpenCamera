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

import java.util.Date;

import android.annotation.TargetApi;
import android.content.SharedPreferences;
import android.hardware.camera2.CaptureResult;
import android.os.Build;
import android.os.CountDownTimer;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.ViewGroup.LayoutParams;
import android.widget.CompoundButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

/* <!-- +++
 import com.almalence.opencam_plus.cameracontroller.CameraController;
 import com.almalence.opencam_plus.CameraParameters;
 import com.almalence.opencam_plus.ApplicationScreen;
 import com.almalence.opencam_plus.PluginCapture;
 import com.almalence.opencam_plus.PluginManager;
 import com.almalence.opencam_plus.R;
 import com.almalence.opencam_plus.ApplicationInterface;
 +++ --> */
// <!-- -+-
import com.almalence.opencam.CameraParameters;
import com.almalence.opencam.ApplicationScreen;
import com.almalence.opencam.PluginCapture;
import com.almalence.opencam.PluginManager;
import com.almalence.opencam.R;
import com.almalence.opencam.ApplicationInterface;
import com.almalence.opencam.cameracontroller.CameraController;
//-+- -->

import com.almalence.ui.Switch.Switch;

/***
 * Implements back in time capture plugin
 * 
 * Starts capturing images immediately after start. Stops capturing when shutter
 * button pressed.
 ***/

public class PreshotCapturePlugin extends PluginCapture
{
	// preferences
	private static String		PreShotInterval;
	private static String		FPS;
	private static boolean		RefocusPreference;
	private static boolean		AutostartPreference;
	private static String		PauseBetweenShots;
	private int					preferenceFocusMode;

	private static boolean		isSlowMode			= false;

	private static boolean		isBuffering			= false;

	private static int			counter				= 0;
	private static final int	REFOCUS_INTERVAL	= 3;

	private Switch				modeSwitcher;

	private boolean				captureStarted		= false;
	
	private boolean				camera2Preference;

	public PreshotCapturePlugin()
	{
		super("com.almalence.plugins.preshotcapture", R.xml.preferences_capture_preshot,
				R.xml.preferences_capture_preshot, 0, null);
	}

	@Override
	public void onStart()
	{
		getPrefs();
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		camera2Preference = prefs.getBoolean(ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), false);
		
		if(CameraController.isNexus6 && camera2Preference)
		{
			prefs.edit().putBoolean(ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), false).commit();
			CameraController.setUseCamera2(false);
			
			CameraController.isOldCameraOneModeLaunched = true;
			PluginManager.getInstance().setSwitchModeType(true);
		}
	}

	@Override
	public void onResume()
	{
		CameraController.setNeedPreviewFrame(true);
		preferenceFocusMode = ApplicationScreen.instance.getFocusModePref(CameraParameters.AF_MODE_AUTO);
		ApplicationScreen.instance.muteShutter(false);
		captureStarted = false;
	}

	@Override
	public void onPause()
	{
		StopBuffering();
		inCapture = false;
		
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		
		ApplicationScreen.instance.setFocusModePref(preferenceFocusMode);
		
		prefs.edit().putBoolean(ApplicationScreen.getMainContext().getResources().getString(R.string.Preference_UseCamera2Key), camera2Preference).commit();

	}

	@Override
	public void onStop()
	{
		ApplicationScreen.getGUIManager().removeViews(modeSwitcher, R.id.specialPluginsLayout3);
		
		if(CameraController.isNexus6 && camera2Preference)
		{
			CameraController.useCamera2OnRelaunch(true);
			CameraController.setUseCamera2(camera2Preference);
		}
	}

	@Override
	public void onDestroy()
	{
		PreShot.FreeBuffer();
	}

	@Override
	public void onGUICreate()
	{
		getPrefs();

		LayoutInflater inflator = ApplicationScreen.instance.getLayoutInflater();
		modeSwitcher = (Switch) inflator.inflate(R.layout.plugin_capture_preshot_modeswitcher, null, false);

		modeSwitcher.setTextOn("Hi-Res");
		modeSwitcher.setTextOff("Hi-Speed");
		modeSwitcher.setChecked(isSlowMode);
		modeSwitcher.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener()
		{
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked)
			{
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());

				SharedPreferences.Editor editor = prefs.edit();
				editor.putString("modePrefPreShot", isChecked ? "1" : "0");
				editor.commit();

				getPrefs();
			}
		});

		ApplicationScreen.getGUIManager().removeViews(modeSwitcher, R.id.specialPluginsLayout3);
		RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(LayoutParams.WRAP_CONTENT,
				LayoutParams.WRAP_CONTENT);

		params.addRule(RelativeLayout.ALIGN_PARENT_TOP);
		params.addRule(RelativeLayout.ALIGN_PARENT_RIGHT);

		((RelativeLayout) ApplicationScreen.instance.findViewById(R.id.specialPluginsLayout3)).addView(this.modeSwitcher,
				params);

		this.modeSwitcher.setLayoutParams(params);
	}

	private void getPrefs()
	{
		// Get the xml/preferences.xml preferences
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ApplicationScreen.getMainContext());
		RefocusPreference = prefs.getBoolean("refocusPrefPreShot", false);
		AutostartPreference = prefs.getBoolean("autostartPrefPreShot", false);
		PauseBetweenShots = prefs.getString("pauseBetweenShotsPrefPreShot", "500");
		PreShotInterval = prefs.getString("backInTimePrefPreShot", "5");

		if (1 == Integer.parseInt(prefs.getString("modePrefPreShot", "0")))
		{
			isSlowMode = true;
			FPS = "2";
		} else
		{
			FPS = prefs.getString("fpsPrefPreShot", "4");
			isSlowMode = false;
		}
	}

	@Override
	public void onCameraSetup()
	{
		if (AutostartPreference)
			if (PluginManager.getInstance().getProcessingCounter() == 0)
				StartBuffering();
	}

	@Override
	public void setupCameraParameters()
	{
		if (!isSlowMode)// fast mode
		{
			try
			{
				if (CameraController.isModeAvailable(CameraController.getSupportedFocusModes(),
						CameraParameters.AF_MODE_CONTINUOUS_VIDEO))
				{
					CameraController.setCameraFocusMode(CameraParameters.AF_MODE_CONTINUOUS_VIDEO);
					ApplicationScreen.instance.setFocusModePref(CameraParameters.AF_MODE_CONTINUOUS_VIDEO);
				}
			} catch (Exception e)
			{
				Log.i("Preshot capture", "Exception fast:" + e.getMessage());
			}
		} else
		// slow mode
		{
			try
			{
				if (CameraController.isModeAvailable(CameraController.getSupportedFocusModes(),
						CameraParameters.AF_MODE_CONTINUOUS_PICTURE))
				{
					CameraController.setCameraFocusMode(CameraParameters.AF_MODE_CONTINUOUS_PICTURE);
					ApplicationScreen.instance.setFocusModePref(CameraParameters.AF_MODE_CONTINUOUS_PICTURE);
				}
			} catch (Exception e)
			{
				Log.i("Preshot capture", "Exception slow:" + e.getMessage());

			}
		}

		PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_BROADCAST, 
				ApplicationInterface.MSG_FOCUS_CHANGED);
	}

	@Override
	public void onExportFinished()
	{
		inCapture = false;
		if (modeSwitcher != null && isBuffering == false)
			modeSwitcher.setEnabled(true);
		if (AutostartPreference)
			StartBuffering();
	}

	@Override
	public void onShutterClick()
	{
		if (captureStarted || AutostartPreference)
		{
			if (0 == PreShot.GetImageCount())
			{
				Toast.makeText(ApplicationScreen.instance, "No images yet", Toast.LENGTH_SHORT).show();
				return;
			}
			captureStarted = false;
			StopBuffering();

			PluginManager.getInstance().sendMessage(ApplicationInterface.MSG_CAPTURE_FINISHED, 
					String.valueOf(SessionID));
		} else if (!inCapture)
		{
			if (!AutostartPreference && modeSwitcher != null)
				modeSwitcher.setEnabled(false);
			captureStarted = true;
			StartBuffering();
		}
	}

	private static int	frmCnt		= 1;
	private static int	preview_fps	= 0;

	public static int	imW			= 0;
	public static int	imH			= 0;

	private long t1 = 0;
	private int cnt = 0;
	private double fpsInterval = 0;
	
	// starts buffering to native buffer
	void StartBuffering()
	{

		Date curDate = new Date();
		SessionID = curDate.getTime();

		ApplicationScreen.instance.muteShutter(true);
		
		resultCompleted = 0;
		
		isBuffering = true;
		if (!isSlowMode)
		{
			PreShot.FreeBuffer();
			ApplicationScreen.getGUIManager().startContinuousCaptureIndication();
			preview_fps = CameraController.getPreviewFrameRate();
			if (CameraController.isHTCOne)
				preview_fps = 30;

			imW = ApplicationScreen.getPreviewWidth();
			imH = ApplicationScreen.getPreviewHeight();

			Log.i("Preshot capture", "StartBuffering trying to allocate!");

			int secondsAllocated = PreShot.AllocateBuffer(imW, imH, Integer.parseInt(FPS),
					Integer.parseInt(PreShotInterval), 0);
			if (secondsAllocated == 0)
			{
				Log.i("Preshot capture", "StartBuffering failed, can't allocate native buffer!");
				return;
			}
			PluginManager.getInstance().addToSharedMem("IsSlowMode" + SessionID, "false");
			cnt = frmCnt % (preview_fps / Integer.parseInt(FPS))*10;
			fpsInterval = 1000.0/Integer.parseInt(FPS);
			t1 = System.currentTimeMillis();
			
			inCapture = true;
		} else
		{
			// full size code
			PreShot.FreeBuffer();
			CameraController.Size imageSize = CameraController.getCameraImageSize();
			imW = imageSize.getWidth();
			imH = imageSize.getHeight();

			int secondsAllocated = PreShot.AllocateBuffer(imW, imH, Integer.parseInt(FPS),
					Integer.parseInt(PreShotInterval), 1);
			if (secondsAllocated == 0)
			{
				Log.i("Preshot capture", "StartBuffering failed, can't allocate native buffer!");
				return;
			}
			PluginManager.getInstance().addToSharedMem("cameraMirrored" + SessionID, String.valueOf(CameraController.isFrontCamera()));
			PluginManager.getInstance().addToSharedMem("IsSlowMode" + SessionID, "true");

			StartCaptureSequence();
		}
	}

	void StopBuffering()
	{
		ApplicationScreen.getGUIManager().stopCaptureIndication();

		ApplicationScreen.instance.muteShutter(false);

		if (modeSwitcher != null)
			modeSwitcher.setEnabled(false);

		if (!isBuffering)
			return;
		else
			isBuffering = false;

		counter = 0;
		resultCompleted = 0;

		PluginManager.getInstance().addToSharedMem("amountofcapturedframes" + SessionID,
				String.valueOf(PreShot.GetImageCount()));

		if (!isSlowMode)
		{
			frmCnt = 1;
		}
	}

	@Override
	public void onPreviewFrame(byte[] data)
	{
		if (isSlowMode || !isBuffering)
			return;

		long t2 = System.currentTimeMillis();
		long timelapse = t2-t1;
		
		if (0 == cnt || timelapse>fpsInterval)
		{
			t1 = System.currentTimeMillis();
			System.gc();

			if (frmCnt == 1)
				PluginManager.getInstance().addToSharedMemExifTagsFromCamera(SessionID);

			PreShot.InsertToBuffer(data, ApplicationScreen.getGUIManager().getImageDataOrientation());
		}
		frmCnt++;
	}

	void StartCaptureSequence()
	{
		if (!inCapture)
		{
			inCapture = true;
			
			if(CameraController.isAutoFocusPerform())
				aboutToTakePicture = true;
			else
				CaptureFrame();
		}
	}

	public void NotEnoughMemory()
	{
		Log.i("Preshot capture", "NotEnoughMemory!");
	}

	@Override
	public void addToSharedMemExifTags(byte[] frameData) {
		if (0 == PreShot.GetImageCount()) {
			if (frameData != null)
				PluginManager.getInstance().addToSharedMemExifTagsFromJPEG(frameData, SessionID, -1);
			else
				PluginManager.getInstance().addToSharedMemExifTagsFromCamera(SessionID);
		}
	}
	
	@Override
	public void onImageTaken(int frame, byte[] frameData, int frame_len, int format)
	{
		PreShot.InsertToBuffer(frameData, ApplicationScreen.getGUIManager().getImageDataOrientation());

		try
		{
			CameraController.startCameraPreview();
			if (isBuffering)
			{
				ProcessPauseBetweenShots();
			}
		} catch (RuntimeException e)
		{
			Log.i("Preshot capture", "StartPreview fail");
			StopBuffering();
		}
	}

	void ProcessPauseBetweenShots()
	{
		int interval = Integer.parseInt(PauseBetweenShots);
		if (interval == 0)
		{
			afterPause();
			return;
		}

		new CountDownTimer(interval, interval)
		{
			public void onFinish()
			{
				afterPause();
			}

			@Override
			public void onTick(long millisUntilFinished)
			{
			}
		}.start();
	}

	void afterPause()
	{
		if (isBuffering)
		{
			int focusMode = ApplicationScreen.instance.getFocusModePref(-1);
			if (RefocusPreference
					|| (counter >= REFOCUS_INTERVAL)
					&& !(focusMode == CameraParameters.AF_MODE_CONTINUOUS_PICTURE
							|| focusMode == CameraParameters.AF_MODE_CONTINUOUS_VIDEO
							|| focusMode == CameraParameters.AF_MODE_INFINITY
							|| focusMode == CameraParameters.AF_MODE_FIXED || focusMode == CameraParameters.AF_MODE_EDOF)
					&& !ApplicationScreen.instance.getAutoFocusLock())
			{
				counter = 0;
				aboutToTakePicture = true;
				if (!CameraController.autoFocus())
				{
					aboutToTakePicture = false;
					CaptureFrame();
				}
			} else
			{
				CaptureFrame();
			}
		}
	}

	public void CaptureFrame()
	{
		if (isBuffering)
		{
			if (CameraController.getFocusState() == CameraController.FOCUS_STATE_FOCUSING)
				return;
	
			createRequestIDList(1);
			CameraController.captureImagesWithParams(1, CameraController.JPEG, null, null, null, null, false, false, true);
			counter++;
		}
	}
	
	@TargetApi(21)
	@Override
	public void onCaptureCompleted(CaptureResult result)
	{
		int requestID = requestIDArray[0];
		resultCompleted++;
		Log.e("PreShotCapturePlugin", "onCaptureCompleted. resultCompleted = " +resultCompleted);
		if (result.getSequenceId() == requestID)
		{
			PluginManager.getInstance().addToSharedMemExifTagsFromCaptureResult(result, SessionID, resultCompleted);
		}
	}

	@Override
	public void onAutoFocus(boolean paramBoolean)
	{
		// on motorola xt5 cm7 this function is called twice!
		// on motorola droid's onAutoFocus seem to be called at every
		// startPreview,
		// causing additional frame(s) taken after sequence is finished
		if (aboutToTakePicture && isSlowMode)
		{
			CaptureFrame();
		}
		
		aboutToTakePicture = false;
	}

	@Override
	public boolean onBroadcast(int arg1, int arg2)
	{
		if (arg1 == ApplicationInterface.MSG_STOP_CAPTURE)
		{
			StopBuffering();
			return true;
		} else if (arg1 == ApplicationInterface.MSG_START_CAPTURE)
		{
			if (PluginManager.getInstance().getProcessingCounter() == 0)
				StartBuffering();
			return true;
		}
		return false;
	}
}
